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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import adql.query.IdentifierField;
import adql.query.from.ADQLJoin;
import adql.query.operand.ADQLColumn;
import cds.utils.TextualSearchList;

/**
 * <p>A list of {@link DBColumn} elements ordered by their ADQL name in an ascending manner.</p>
 * 
 * <p>
 * 	In addition to an ADQL name, {@link DBColumn} elements can be searched by specifying their table, schema and catalog.
 * 	These last information will be used only if the ADQL column name is ambiguous, otherwise all matching elements are returned.
 * </p>
 * 
 * <p><i>
 * 	<u>Note:</u>
 * 	Table aliases can be listed here with their corresponding table name. Consequently, a table alias can be given as table name in the search parameters.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (08/2015)
 */
public class SearchColumnList extends TextualSearchList<DBColumn> {
	private static final long serialVersionUID = 1L;

	/** Indicates whether multiple occurrences are allowed. */
	private boolean distinct = false;

	/** Case-sensitive dictionary of table aliases. (tableAlias <-> TableName) */
	private final HashMap<String,String> tableAliases = new HashMap<String,String>();

	/** Case-insensitive dictionary of table aliases. (tablealias <-> List&lt;TableName&gt;) */
	private final HashMap<String,ArrayList<String>> mapAliases = new HashMap<String,ArrayList<String>>();

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Void constructor.
	 */
	public SearchColumnList(){
		super(new DBColumnKeyExtractor());
	}

	/**
	 * Constructor by copy: all the elements of the given collection of {@link DBColumn} are copied ordered into this list.
	 * 
	 * @param collection	Collection of {@link DBColumn} to copy.
	 */
	public SearchColumnList(final Collection<DBColumn> collection){
		super(collection, new DBColumnKeyExtractor());
	}

	/**
	 * Constructor with the initial capacity.
	 * 
	 * @param initialCapacity	Initial capacity of this list.
	 */
	public SearchColumnList(final int initialCapacity){
		super(initialCapacity, new DBColumnKeyExtractor());
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	/**
	 * Tells whether multiple occurrences are allowed.
	 * 
	 * @return <i>true</i> means that multiple occurrences are allowed, <i>false</i> otherwise.
	 */
	public final boolean isDistinct(){
		return distinct;
	}

	/**
	 * Lets indicating that multiple occurrences are allowed.
	 * 
	 * @param distinct <i>true</i> means that multiple occurrences are allowed, <i>false</i> otherwise.
	 */
	public final void setDistinct(final boolean distinct){
		this.distinct = distinct;
	}

	/* ********************** */
	/* TABLE ALIAS MANAGEMENT */
	/* ********************** */
	/**
	 * Adds the given association between a table name and its alias in a query.
	 * 
	 * @param tableAlias	Table alias.
	 * @param tableName		Table name.
	 */
	public final void putTableAlias(final String tableAlias, final String tableName){
		if (tableAlias != null && tableName != null){
			tableAliases.put(tableAlias, tableName);

			ArrayList<String> aliases = mapAliases.get(tableAlias.toLowerCase());
			if (aliases == null){
				aliases = new ArrayList<String>();
				mapAliases.put(tableAlias.toLowerCase(), aliases);
			}
			aliases.add(tableAlias);
		}
	}

	/**
	 * Removes the given alias from this list.
	 * 
	 * @param tableAlias	The table alias which must be removed.
	 */
	public final void removeTableAlias(final String tableAlias){
		tableAliases.remove(tableAlias);

		ArrayList<String> aliases = mapAliases.get(tableAlias.toLowerCase());
		if (aliases != null){
			aliases.remove(tableAlias);
			if (aliases.isEmpty())
				mapAliases.remove(tableAlias.toLowerCase());
		}
	}

	/**
	 * Removes all table name/alias associations.
	 */
	public final void removeAllTableAliases(){
		tableAliases.clear();
		mapAliases.clear();
	}

	public final int getNbTableAliases(){
		return tableAliases.size();
	}

	/* ************** */
	/* SEARCH METHODS */
	/* ************** */
	/**
	 * Searches all {@link DBColumn} elements which has the given name (case insensitive).
	 * 
	 * @param columnName	ADQL name of {@link DBColumn} to search for.
	 * 
	 * @return				The corresponding {@link DBColumn} elements.
	 * 
	 * @see TextualSearchList#get(String)
	 */
	public ArrayList<DBColumn> search(final String columnName){
		return get(columnName);
	}

	/**
	 * Searches all {@link DBColumn} elements which have the given catalog, schema, table and column name (case insensitive).
	 * 
	 * @param catalog	Catalog name.
	 * @param schema	Schema name.
	 * @param table		Table name.
	 * @param column	Column name.
	 * 
	 * @return			The list of all matching {@link DBColumn} elements.
	 * 
	 * @see #search(String, String, String, String, byte)
	 */
	public final ArrayList<DBColumn> search(final String catalog, final String schema, final String table, final String column){
		return search(catalog, schema, table, column, (byte)0);
	}

	/**
	 * Searches all {@link DBColumn} elements corresponding to the given {@link ADQLColumn} (case insensitive).
	 * 
	 * @param column	An {@link ADQLColumn}.
	 * 
	 * @return			The list of all corresponding {@link DBColumn} elements.
	 * 
	 * @see #search(String, String, String, String, byte)
	 */
	public ArrayList<DBColumn> search(final ADQLColumn column){
		return search(column.getCatalogName(), column.getSchemaName(), column.getTableName(), column.getColumnName(), column.getCaseSensitive());
	}

	/**
	 * Searches all {@link DBColumn} elements which have the given catalog, schema, table and column name, with the specified case sensitivity.
	 * 
	 * @param catalog			Catalog name.
	 * @param schema			Schema name.
	 * @param table				Table name.
	 * @param column			Column name.
	 * @param caseSensitivity	Case sensitivity for each column parts (one bit by part ; 0=sensitive,1=insensitive ; see {@link IdentifierField} for more details).
	 * 
	 * @return					The list of all matching {@link DBColumn} elements.
	 * 
	 * @see IdentifierField
	 */
	public ArrayList<DBColumn> search(final String catalog, final String schema, final String table, final String column, final byte caseSensitivity){

		ArrayList<DBColumn> tmpResult = get(column, IdentifierField.COLUMN.isCaseSensitive(caseSensitivity));

		/* WITH TABLE PREFIX */
		if (table != null){
			/* 1. Figure out the table alias */
			String tableName = null;
			ArrayList<String> aliasMatches = null;

			// Case sensitive => tableName is set , aliasMatches = null
			if (IdentifierField.TABLE.isCaseSensitive(caseSensitivity)){
				tableName = tableAliases.get(table);
				if (tableName == null)
					tableName = table;
			}
			// Case INsensitive
			// a) Alias is found => tableName = null  , aliasMatches contains the list of all tables matching the alias
			// b) No alias       => tableName = table , aliasMatches = null
			else{
				aliasMatches = mapAliases.get(table.toLowerCase());
				if (aliasMatches == null || aliasMatches.isEmpty())
					tableName = table;
			}

			/* 2. For each found column, test whether its table, schema and catalog names match.
			 *    If it matches, keep the column aside. */
			ArrayList<DBColumn> result = new ArrayList<DBColumn>();
			for(DBColumn match : tmpResult){

				// Get the list of all tables covered by this column:
				//   - only 1 if it is a normal column
				//   - several if it is a common column (= result of table join)
				Iterator<DBTable> itMatchTables;
				if (ADQLJoin.isCommonColumn(match))
					itMatchTables = ((DBCommonColumn)match).getCoveredTables();
				else
					itMatchTables = new SingleIterator<DBTable>(match.getTable());

				// Test the matching with every covered tables:
				DBTable matchTable;
				while(itMatchTables.hasNext()){
					// get the table:
					matchTable = itMatchTables.next();

					// test the table name:
					if (aliasMatches == null){	// case table name is (sensitive) or (INsensitive with no alias found)
						if (IdentifierField.TABLE.isCaseSensitive(caseSensitivity)){
							if (!matchTable.getADQLName().equals(tableName))
								continue;
						}else{
							if (!matchTable.getADQLName().equalsIgnoreCase(tableName))
								continue;
						}
					}else{	// case INsensitive with at least one alias found
						boolean foundAlias = false;
						String temp;
						for(int a = 0; !foundAlias && a < aliasMatches.size(); a++){
							temp = tableAliases.get(aliasMatches.get(a));
							if (temp != null)
								foundAlias = matchTable.getADQLName().equalsIgnoreCase(temp);
						}
						if (!foundAlias)
							continue;
					}

					// test the schema name:
					if (schema != null){
						// No schema name (<=> no schema), then this table can not be a good match:
						if (matchTable.getADQLSchemaName() == null)
							continue;
						if (IdentifierField.SCHEMA.isCaseSensitive(caseSensitivity)){
							if (!matchTable.getADQLSchemaName().equals(schema))
								continue;
						}else{
							if (!matchTable.getADQLSchemaName().equalsIgnoreCase(schema))
								continue;
						}

						// test the catalog name:
						if (catalog != null){
							// No catalog name (<=> no catalog), then this table can not be a good match:
							if (matchTable.getADQLCatalogName() == null)
								continue;
							if (IdentifierField.CATALOG.isCaseSensitive(caseSensitivity)){
								if (!matchTable.getADQLCatalogName().equals(catalog))
									continue;
							}else{
								if (!matchTable.getADQLCatalogName().equalsIgnoreCase(catalog))
									continue;
							}
						}
					}

					// if here, all prefixes are matching and so the column is a good match:
					DBColumn goodMatch = matchTable.getColumn(match.getADQLName(), true);
					result.add(goodMatch);
				}
			}
			return result;

		}
		/* NO TABLE PREFIX */
		else{
			// Special case: the columns merged by a NATURAL JOIN or a USING may have no table reference:
			if (tmpResult.size() > 1){
				// List all common columns. If there are several, only the list of matching normal columns must be returned.
				// This list must not contain common columns.
				// Instead, it must contains all normal columns covered by the common columns.
				ArrayList<DBColumn> result = new ArrayList<DBColumn>(tmpResult.size());
				for(int i = 0; i < tmpResult.size(); i++){
					if (ADQLJoin.isCommonColumn(tmpResult.get(i))){
						// this common column is a good match
						// => add it into the list of matching common columns
						//    AND remove it from the normal columns list
						DBCommonColumn commonColumn = (DBCommonColumn)tmpResult.remove(i);
						result.add(commonColumn);
						// then, add all normal columns covered by this common columns:
						Iterator<DBTable> itCoveredTables = commonColumn.getCoveredTables();
						while(itCoveredTables.hasNext())
							tmpResult.add(itCoveredTables.next().getColumn(column, true));
					}
				}

				if (result.size() == 1)
					return result;
			}

			return tmpResult;
		}
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	@Override
	public boolean add(final DBColumn item){
		if (distinct && contains(item))
			return false;
		else
			return super.add(item);
	}

	@Override
	public boolean addAll(final Collection<? extends DBColumn> c){
		boolean changed = super.addAll(c);

		if (changed){
			if (c instanceof SearchColumnList){
				SearchColumnList list = (SearchColumnList)c;
				for(Map.Entry<String,String> entry : list.tableAliases.entrySet())
					putTableAlias(entry.getKey(), entry.getValue());
			}
		}

		return changed;
	}

	@Override
	public boolean removeAll(final Collection<?> c){
		boolean changed = super.removeAll(c);

		if (changed){
			if (c instanceof SearchColumnList){
				SearchColumnList list = (SearchColumnList)c;
				for(String key : list.tableAliases.keySet())
					removeTableAlias(key);
			}
		}

		return changed;
	}

	/**
	 * Lets extracting the key to associate with a given {@link DBColumn} instance.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 09/2011
	 */
	private static class DBColumnKeyExtractor implements KeyExtractor<DBColumn> {
		@Override
		public String getKey(DBColumn obj){
			return obj.getADQLName();
		}
	}

	/**
	 * Iterator that iterates over only one item, given in the constructor.
	 * 
	 * @param <E> Type of the item that this Iterator must return.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 1.2 (11/2013)
	 * @since 1.2
	 */
	private static class SingleIterator< E > implements Iterator<E> {
		private final E item;
		private boolean done = false;

		public SingleIterator(final E singleItem){
			item = singleItem;
		}

		@Override
		public boolean hasNext(){
			return !done;
		}

		@Override
		public E next(){
			if (!done){
				done = true;
				return item;
			}else
				throw new NoSuchElementException();
		}

		@Override
		public void remove(){
			throw new UnsupportedOperationException();
		}
	}

}
