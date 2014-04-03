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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import adql.query.IdentifierField;

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
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 09/2011
 */
public class SearchColumnList extends TextualSearchList<DBColumn> {
	private static final long serialVersionUID = 1L;

	/** Indicates whether multiple occurrences are allowed. */
	private boolean distinct = false;

	/** Case-sensitive dictionary of table aliases. (tableAlias <-> TableName) */
	private final HashMap<String, String> tableAliases = new HashMap<String, String>();

	/** Case-insensitive dictionary of table aliases. (tablealias <-> List&lt;TableName&gt;) */
	private final HashMap<String, ArrayList<String>> mapAliases = new HashMap<String, ArrayList<String>>();


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Void constructor.
	 */
	public SearchColumnList() {
		super(new DBColumnKeyExtractor());
	}

	/**
	 * Constructor by copy: all the elements of the given collection of {@link DBColumn} are copied ordered into this list.
	 * 
	 * @param collection	Collection of {@link DBColumn} to copy.
	 */
	public SearchColumnList(final Collection<DBColumn> collection) {
		super(collection, new DBColumnKeyExtractor());
	}

	/**
	 * Constructor with the initial capacity.
	 * 
	 * @param initialCapacity	Initial capacity of this list.
	 */
	public SearchColumnList(final int initialCapacity) {
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
	public final boolean isDistinct() {
		return distinct;
	}

	/**
	 * Lets indicating that multiple occurrences are allowed.
	 * 
	 * @param distinct <i>true</i> means that multiple occurrences are allowed, <i>false</i> otherwise.
	 */
	public final void setDistinct(final boolean distinct) {
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

		if (table != null){
			ArrayList<DBColumn> result = new ArrayList<DBColumn>();

			for(DBColumn match : tmpResult){
				DBTable dbTable = match.getTable();
				if (IdentifierField.TABLE.isCaseSensitive(caseSensitivity)){
					String tableName = tableAliases.get(table);
					if (tableName == null) tableName = table;
					if (!dbTable.getADQLName().equals(tableName))
						continue;
				}else{
					ArrayList<String> aliases = mapAliases.get(table.toLowerCase());
					if (aliases == null){
						if (!dbTable.getADQLName().equalsIgnoreCase(table))
							continue;
					}else{
						boolean foundAlias = false;
						String temp;
						for(int a=0; !foundAlias && a<aliases.size(); a++){
							temp = tableAliases.get(aliases.get(a));
							if (temp != null)
								foundAlias = dbTable.getADQLName().equalsIgnoreCase(temp);
						}
						if (!foundAlias)
							continue;
					}
				}

				if (schema != null){
					if (IdentifierField.SCHEMA.isCaseSensitive(caseSensitivity)){
						if (!dbTable.getADQLSchemaName().equals(schema))
							continue;
					}else{
						if (!dbTable.getADQLSchemaName().equalsIgnoreCase(schema))
							continue;
					}

					if (catalog != null){
						if (IdentifierField.CATALOG.isCaseSensitive(caseSensitivity)){
							if (!dbTable.getADQLCatalogName().equals(catalog))
								continue;
						}else{
							if (!dbTable.getADQLCatalogName().equalsIgnoreCase(catalog))
								continue;
						}
					}
				}

				result.add(match);
			}
			return result;

		}else{
			// Special case: the columns merged by a NATURAL JOIN or a USING may have no table reference:
			if (tmpResult.size() > 1){
				ArrayList<DBColumn> result = new ArrayList<DBColumn>(tmpResult.size());
				for(int i=0; i<tmpResult.size(); i++){
					if (tmpResult.get(i).getTable() == null)
						result.add(tmpResult.remove(i));
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
	public boolean add(final DBColumn item) {
		if (distinct && contains(item))
			return false;
		else
			return super.add(item);
	}

	@Override
	public boolean addAll(final Collection<? extends DBColumn> c) {
		boolean changed = super.addAll(c);

		if (changed){
			if (c instanceof SearchColumnList){
				SearchColumnList list = (SearchColumnList)c;
				for(Map.Entry<String, String> entry : list.tableAliases.entrySet())
					putTableAlias(entry.getKey(), entry.getValue());
			}
		}

		return changed;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
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
		public String getKey(DBColumn obj) {
			return obj.getADQLName();
		}
	}

}
