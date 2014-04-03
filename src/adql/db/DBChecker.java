package adql.db;

/*
 * This file is part of ADQLLibrary.
 * 
 * ADQLLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ADQLLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2011 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


import adql.db.exception.UnresolvedColumnException;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.db.exception.UnresolvedTableException;

import adql.parser.ParseException;
import adql.parser.QueryChecker;

import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.ClauseSelect;
import adql.query.ColumnReference;
import adql.query.IdentifierField;
import adql.query.SelectAllColumns;
import adql.query.SelectItem;
import adql.query.from.ADQLTable;

import adql.query.operand.ADQLColumn;

import adql.search.ISearchHandler;
import adql.search.SearchColumnHandler;
import adql.search.SimpleSearchHandler;

/**
 * <p>
 * 	Checks the existence of tables and columns, but also adds database metadata
 * 	on {@link ADQLTable} and {@link ADQLColumn} instances when they are resolved.
 * </p>
 * 
 * <p>These information are:</p>
 * <ul>
 * 	<li>the corresponding {@link DBTable} or {@link DBColumn} (see getter and setter for DBLink in {@link ADQLTable} and {@link ADQLColumn})</li>
 * 	<li>the link between an {@link ADQLColumn} and its {@link ADQLTable}</li>
 * </ul>
 * 
 * <p><i><u>Note:</u>
 * 	Knowing DB metadata of {@link ADQLTable} and {@link ADQLColumn} is particularly useful for the translation of the ADQL query to SQL, because the ADQL name of columns and tables
 * 	can be replaced in SQL by their DB name, if different. This mapping is done automatically by {@link adql.translator.PostgreSQLTranslator}.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 */
public class DBChecker implements QueryChecker {

	/** List of all available tables ({@link DBTable}). */
	protected SearchTableList lstTables;


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds a {@link DBChecker} with an empty list of tables.
	 */
	public DBChecker(){
		lstTables = new SearchTableList();
	}

	/**
	 * Builds a {@link DBChecker} with the given list of tables.
	 * 
	 * @param tables	List of all available tables.
	 */
	public DBChecker(final Collection<DBTable> tables){
		setTables(tables);
	}


	/* ****** */
	/* SETTER */
	/* ****** */
	/**
	 * <p>Sets the list of all available tables.</p>
	 * 
	 * <p><i><u>Note:</u>
	 * 	Only if the given collection is NOT an instance of {@link SearchTableList},
	 * 	the collection will be copied inside a new {@link SearchTableList}.
	 * </i></p>
	 * 
	 * @param tables	List of {@link DBTable}s.
	 */
	public final void setTables(final Collection<DBTable> tables){
		if (tables == null)
			lstTables = new SearchTableList();
		else if (tables instanceof SearchTableList)
			lstTables = (SearchTableList)tables;
		else
			lstTables = new SearchTableList(tables);
	}


	/* ************* */
	/* CHECK METHODS */
	/* ************* */
	/**
	 * Followed algorithm:
	 * <pre>
	 * Map&lt;DBTable,ADQLTable&gt; mapTables;
	 * 
	 * For each ADQLTable t
	 * 	if (t.isSubQuery())
	 * 		dbTable = generateDBTable(t.getSubQuery, t.getAlias());
	 * 	else
	 * 		dbTable = resolveTable(t);
	 * 	t.setDBLink(dbTable);
	 * 	dbTables.put(t, dbTable);
	 * End
	 * 
	 * For each SelectAllColumns c
	 * 	table = c.getAdqlTable();
	 * 	if (table != null){
	 * 		dbTable = resolveTable(table);
	 * 		if (dbTable == null)
	 * 			dbTable = query.getFrom().getTablesByAlias(table.getTableName(), table.isCaseSensitive(IdentifierField.TABLE));
	 *		if (dbTable == null)
	 *			throw new UnresolvedTableException(table);
	 * 		table.setDBLink(dbTable);
	 * 	}
	 * End
	 * 
	 * SearchColumnList list = query.getFrom().getDBColumns();
	 * 
	 * For each ADQLColumn c
	 * 	dbColumn = resolveColumn(c, list);
	 * 	c.setDBLink(dbColumn);
	 * 	c.setAdqlTable(mapTables.get(dbColumn.getTable()));
	 * End
	 * 
	 * For each ColumnReference colRef
	 *	checkColumnReference(colRef, query.getSelect(), list);
	 * End
	 * </pre>
	 * 
	 * @param query				The query to check.
	 * 
	 * @throws ParseException	An {@link UnresolvedIdentifiersException} if some tables or columns can not be resolved.
	 * 
	 * @see #resolveTable(ADQLTable)
	 * @see #generateDBTable(ADQLQuery, String)
	 * @see #resolveColumn(ADQLColumn, SearchColumnList)
	 * @see #checkColumnReference(ColumnReference, ClauseSelect, SearchColumnList)
	 */
	public void check(final ADQLQuery query) throws ParseException {
		UnresolvedIdentifiersException errors = new UnresolvedIdentifiersException();
		HashMap<DBTable, ADQLTable> mapTables = new HashMap<DBTable, ADQLTable>();
		ISearchHandler sHandler;

		// Check the existence of all tables:
		sHandler = new SearchTableHandler();
		sHandler.search(query.getFrom());
		for(ADQLObject result : sHandler){
			try{
				ADQLTable table = (ADQLTable)result;
				// resolve the table:
				DBTable dbTable = null;
				if (table.isSubQuery()){
					dbTable = generateDBTable(table.getSubQuery(), table.getAlias());
				}else{
					dbTable = resolveTable(table);
					if (table.hasAlias())
						dbTable = dbTable.copy(dbTable.getDBName(), table.getAlias());
				}
				// link with the matched DBTable:
				table.setDBLink(dbTable);
				mapTables.put(dbTable, table);
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}

		// Attach table information on wildcards with the syntax "{tableName}.*" of the SELECT clause:
		sHandler = new SearchWildCardHandler();
		sHandler.search(query.getSelect());
		for(ADQLObject result : sHandler){
			try{
				SelectAllColumns wildcard = (SelectAllColumns)result;
				ADQLTable table = wildcard.getAdqlTable();
				DBTable dbTable = null;
				// First, try to resolve the table by table alias:
				if (table.getTableName() != null && table.getSchemaName() == null){
					ArrayList<ADQLTable> tables = query.getFrom().getTablesByAlias(table.getTableName(), table.isCaseSensitive(IdentifierField.TABLE));
					if (tables.size() == 1)
						dbTable = tables.get(0).getDBLink();
				}
				// Then try to resolve the table reference by table name:
				if (dbTable == null)
					dbTable = resolveTable(table);

				//			table.setDBLink(dbTable);
				wildcard.setAdqlTable(mapTables.get(dbTable));
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}

		SearchColumnList list = query.getFrom().getDBColumns();

		//		// DEBUG
		//		System.out.println("\n*** FROM COLUMNS ***");
		//		for(DBColumn dbCol : list){
		//			System.out.println("\t- "+dbCol.getADQLName()+" in "+((dbCol.getTable()==null)?"<NULL>":dbCol.getTable().getADQLName())+" (= "+dbCol.getDBName()+" in "+((dbCol.getTable()==null)?"<NULL>":dbCol.getTable().getDBName())+")");
		//		}
		//		System.out.println();

		// Check the existence of all columns:
		sHandler = new SearchColumnHandler();
		sHandler.search(query);
		for(ADQLObject result : sHandler){
			try{
				ADQLColumn adqlColumn = (ADQLColumn)result;
				// resolve the column:
				DBColumn dbColumn = resolveColumn(adqlColumn, list);
				// link with the matched DBColumn:
				adqlColumn.setDBLink(dbColumn);
				adqlColumn.setAdqlTable(mapTables.get(dbColumn.getTable()));
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}

		// Check the correctness of all column references:
		sHandler = new SearchColReferenceHandler();
		sHandler.search(query);
		ClauseSelect select = query.getSelect();
		for(ADQLObject result : sHandler){
			try{
				ColumnReference colRef = (ColumnReference)result;
				// resolve the column reference:
				DBColumn dbColumn = checkColumnReference(colRef, select, list);
				// link with the matched DBColumn:
				colRef.setDBLink(dbColumn);
				if (dbColumn != null)
					colRef.setAdqlTable(mapTables.get(dbColumn.getTable()));
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}

		// Throw all errors if any:
		if (errors.getNbErrors() > 0)
			throw errors;
	}

	/**
	 * Resolves the given table, that's to say searches for the corresponding {@link DBTable}.
	 * 
	 * @param table	The table to resolve.
	 * 
	 * @return		The corresponding {@link DBTable} if found, <i>null</i> otherwise.
	 * 
	 * @throws ParseException	An {@link UnresolvedTableException} if the given table can't be resolved.
	 */
	protected DBTable resolveTable(final ADQLTable table) throws ParseException {
		ArrayList<DBTable> tables = lstTables.search(table);

		// good if only one table has been found:
		if (tables.size() == 1)
			return tables.get(0);
		// but if more than one: ambiguous table name !
		else if (tables.size() > 1)
			throw new UnresolvedTableException(table, tables.get(0).getADQLSchemaName()+"."+tables.get(0).getADQLName(), tables.get(1).getADQLSchemaName()+"."+tables.get(1).getADQLName());
		// otherwise (no match): unknown table !
		else
			throw new UnresolvedTableException(table);
	}

	/**
	 * Resolves the given column, that's to say searches for the corresponding {@link DBColumn}.
	 * 
	 * @param column		The column to resolve.
	 * @param dbColumns		List of all available {@link DBColumn}s.
	 * 
	 * @return				The corresponding {@link DBColumn} if found, <i>null</i> otherwise.
	 * 
	 * @throws ParseException	An {@link UnresolvedColumnException} if the given column can't be resolved
	 * 							or an {@link UnresolvedTableException} if its table reference can't be resolved.
	 */
	protected DBColumn resolveColumn(final ADQLColumn column, final SearchColumnList dbColumns) throws ParseException {
		ArrayList<DBColumn> foundColumns = dbColumns.search(column);

		// good if only one column has been found:
		if (foundColumns.size() == 1)
			return foundColumns.get(0);
		// but if more than one: ambiguous table reference !
		else if (foundColumns.size() > 1){
			if (column.getTableName() == null)
				throw new UnresolvedColumnException(column, (foundColumns.get(0).getTable()==null)?"<NULL>":(foundColumns.get(0).getTable().getADQLName()+"."+foundColumns.get(0).getADQLName()), (foundColumns.get(1).getTable()==null)?"<NULL>":(foundColumns.get(1).getTable().getADQLName()+"."+foundColumns.get(1).getADQLName()));
			else
				throw new UnresolvedTableException(column, (foundColumns.get(0).getTable()==null)?"<NULL>":foundColumns.get(0).getTable().getADQLName(), (foundColumns.get(1).getTable()==null)?"<NULL>":foundColumns.get(1).getTable().getADQLName());
		}// otherwise (no match): unknown column !
		else
			throw new UnresolvedColumnException(column);
	}

	/**
	 * Checks whether the given column reference corresponds to a selected item (column or an expression with an alias)
	 * or to an existing column.
	 * 
	 * @param colRef			The column reference which must be checked.
	 * @param select			The SELECT clause of the ADQL query.
	 * @param dbColumns			The list of all available {@link DBColumn}s.
	 * 
	 * @return 					The corresponding {@link DBColumn} if this reference is actually the name of a column, <i>null</i> otherwise.
	 * 
	 * @throws ParseException	An {@link UnresolvedColumnException} if the given column can't be resolved
	 * 							or an {@link UnresolvedTableException} if its table reference can't be resolved.
	 * 
	 * @see ClauseSelect#searchByAlias(String)
	 * @see #resolveColumn(ADQLColumn, SearchColumnList)
	 */
	protected DBColumn checkColumnReference(final ColumnReference colRef, final ClauseSelect select, final SearchColumnList dbColumns) throws ParseException {
		if (colRef.isIndex()){
			int index = colRef.getColumnIndex();
			if (index > 0 && index <= select.size()){
				SelectItem item = select.get(index-1);
				if (item.getOperand() instanceof ADQLColumn)
					return ((ADQLColumn)item.getOperand()).getDBLink();
				else
					return null;
			}else
				throw new ParseException("Column index out of bounds: "+index+" (must be between 1 and "+select.size()+") !");
		}else{
			ADQLColumn col = new ADQLColumn(colRef.getColumnName());
			col.setCaseSensitive(colRef.isCaseSensitive());

			// search among the select_item aliases:
			if (col.getTableName() == null){
				ArrayList<SelectItem> founds = select.searchByAlias(colRef.getColumnName(), colRef.isCaseSensitive());
				if (founds.size() == 1)
					return null;
				else if (founds.size() > 1)
					throw new UnresolvedColumnException(col, founds.get(0).getAlias(), founds.get(1).getAlias());
			}

			// check the corresponding column:
			return resolveColumn(col, dbColumns);
		}
	}


	/* ************************************* */
	/* DBTABLE & DBCOLUMN GENERATION METHODS */
	/* ************************************* */
	/**
	 * Generates a {@link DBTable} corresponding to the given sub-query with the given table name.
	 * This {@link DBTable} which contains all {@link DBColumn} returned by {@link ADQLQuery#getResultingColumns()}.
	 * 
	 * @param subQuery	Sub-query in which the specified table must be searched.
	 * @param tableName	Name of the table to search.
	 * 
	 * @return			The corresponding {@link DBTable} if the table has been found in the given sub-query, <i>null</i> otherwise.
	 * 
	 * @throws ParseException	Can be used to explain why the table has not been found.
	 */
	public static DBTable generateDBTable(final ADQLQuery subQuery, final String tableName) throws ParseException {
		DefaultDBTable dbTable = new DefaultDBTable(tableName);

		DBColumn[] columns = subQuery.getResultingColumns();
		for(DBColumn dbCol : columns)
			dbTable.addColumn(dbCol.copy(dbCol.getADQLName(), dbCol.getADQLName(), dbTable));

		return dbTable;
	}


	/* *************** */
	/* SEARCH HANDLERS */
	/* *************** */
	/**
	 * Lets searching all tables.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 07/2011
	 */
	private static class SearchTableHandler extends SimpleSearchHandler {
		@Override
		public boolean match(final ADQLObject obj) {
			return obj instanceof ADQLTable;
		}
	}

	/**
	 * Lets searching all wildcards.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 09/2011
	 */
	private static class SearchWildCardHandler extends SimpleSearchHandler {
		@Override
		public boolean match(final ADQLObject obj){
			return (obj instanceof SelectAllColumns) && (((SelectAllColumns)obj).getAdqlTable() != null);
		}
	}

	/**
	 * Lets searching column references.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 11/2011
	 */
	private static class SearchColReferenceHandler extends SimpleSearchHandler {
		@Override
		public boolean match(final ADQLObject obj) {
			return (obj instanceof ColumnReference);
		}
	}

}
