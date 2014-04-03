package adql.query.from;

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
import java.util.NoSuchElementException;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DefaultDBTable;
import adql.db.SearchColumnList;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.IdentifierField;
import adql.query.TextPosition;

/**
 * It represents any item of the clause FROM: a table name or a sub-query.<br />
 * A table reference may have an alias (MUST if it is a sub-query).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 */
public class ADQLTable implements ADQLObject, FromContent {

	/** The name of the catalog which contains the table. */
	private String catalog = null;

	/** The name of the schema which contains the table. */
	private String schema = null;

	/** The name of the table. */
	private String table;

	/** A sub-query whose the result will be used as a table. */
	private ADQLQuery subQuery;

	/** Label of the table reference. */
	private String alias = null;

	/** Lets specify the case sensitivity of the catalog, schema, table and alias parts. */
	private byte caseSensitivity = 0;

	/** The corresponding table in the "database". */
	private DBTable dbLink = null;

	/** Position in the original ADQL query string. */
	private TextPosition position = null;

	/**
	 * Builds a reference to a table with its name (simple or full ({catalog}.{schema}.{table})).
	 * 
	 * @param table	Name of the table (simple or full ({catalog}.{schema}.{table})).
	 */
	public ADQLTable(String table){
		setTable(table);
		subQuery = null;
	}

	/**
	 * Builds a reference to a table with its name and the name of its schema.
	 * 
	 * @param schema	Name of its schema.
	 * @param table		Name of the table.
	 */
	public ADQLTable(String schema, String table){
		this(table);
		setSchemaName(schema);
	}

	/**
	 * Builds a reference to a table with its name, the name of its schema and the name of its catalog.
	 * 
	 * @param catalog	Name of its catalog.
	 * @param schema	Name of its schema.
	 * @param table		Name of the table.
	 */
	public ADQLTable(String catalog, String schema, String table){
		this(schema, table);
		setCatalogName(catalog);
	}

	/**
	 * Builds a reference to a sub-query.
	 * 
	 * @param query	Sub-query.
	 * 
	 * @see #setSubQuery(ADQLQuery)
	 */
	public ADQLTable(ADQLQuery query){
		setSubQuery(query);
	}

	/**
	 * Builds an ADQL table by copying the given one.
	 * 
	 * @param toCopy		The ADQLTable to copy.
	 * 
	 * @throws Exception	If there is an error during the copy.
	 */
	public ADQLTable(ADQLTable toCopy) throws Exception{
		catalog = toCopy.catalog;
		schema = toCopy.schema;
		table = toCopy.table;
		subQuery = (toCopy.subQuery == null) ? null : (ADQLQuery)toCopy.subQuery.getCopy();
		alias = toCopy.alias;
		caseSensitivity = toCopy.caseSensitivity;
		position = toCopy.position;
	}

	/**
	 * <p>Lets normalizing any catalog/schema/table name or alias.</p>
	 * <p>If the name is surrounded by double-quotes, they are removed
	 * and the corresponding field will be declared as case sensitive.</p>
	 * 
	 * @param name		Name to normalize.
	 * @param field		The name part to normalize and to get (if normalized, the case sensitivity of the given field will be set).
	 * 
	 * @return		The normalized name.
	 */
	protected String normalizeName(final String name, IdentifierField field){
		if (name == null)
			return null;

		StringBuffer n = new StringBuffer(name);
		n.trimToSize();
		if (n.length() == 0)
			return null;
		else{
			if (n.length() > 1 && n.charAt(0) == '\"' && n.charAt(n.length() - 1) == '\"'){
				n.deleteCharAt(0);
				n.deleteCharAt(n.length() - 1);
				n.trimToSize();
				if (n.length() == 0)
					return null;
				else
					setCaseSensitive(field, true);
			}
		}
		return n.toString();
	}

	/**
	 * Gets the position in the original ADQL query string.
	 * 
	 * @return	The position of this {@link ADQLTable}.
	 */
	public final TextPosition getPosition(){
		return position;
	}

	/**
	 * Sets the position at which this {@link ADQLTable} has been found in the original ADQL query string.
	 * 
	 * @param pos	Position of this {@link ADQLTable}.
	 */
	public final void setPosition(final TextPosition pos){
		position = pos;
	}

	/**
	 * Gets the name of the catalog which contains this table.
	 * 
	 * @return Catalog name.
	 */
	public final String getCatalogName(){
		return catalog;
	}

	/**
	 * Sets the name of the catalog which contains this table.
	 * 
	 * @param catalog The new name of its catalog.
	 */
	public final void setCatalogName(String catalog){
		final String temp = normalizeName(catalog, IdentifierField.CATALOG);
		if ((this.catalog == null && temp != null) || (this.catalog != null && !this.catalog.equalsIgnoreCase(temp)))
			dbLink = null;
		this.catalog = temp;
	}

	/**
	 * Gets the name of the schema which contains this table.
	 * 
	 * @return Schema name.
	 */
	public final String getSchemaName(){
		return schema;
	}

	/**
	 * Sets the name of the schema which contains this table.
	 * 
	 * @param schema The new name of its schema.
	 */
	public final void setSchemaName(String schema){
		final String temp = normalizeName(schema, IdentifierField.SCHEMA);
		if ((this.schema == null && temp != null) || (this.schema != null && !this.schema.equalsIgnoreCase(temp)))
			dbLink = null;
		this.schema = temp;
	}

	/**
	 * Gets the name of the table.
	 * 
	 * @return Table name.
	 */
	public final String getTableName(){
		return table;
	}

	/**
	 * Gets the full name of this table (catalogName . schemaName . tableName)
	 * by respecting the case sensitivity of each field (if case sensitive double-quotes will surround the concerned fields name).
	 * 
	 * @return	Its full name.
	 */
	public final String getFullTableName(){
		if (table == null)
			return "";

		StringBuffer name = new StringBuffer();

		// CATALOG:
		if (catalog != null){
			if (isCaseSensitive(IdentifierField.CATALOG))
				name.append('\"').append(catalog).append('\"').append('.');
			else
				name.append(catalog).append('.');
		}
		// SCHEMA:
		if (schema != null){
			if (isCaseSensitive(IdentifierField.SCHEMA))
				name.append('\"').append(schema).append('\"').append('.');
			else
				name.append(schema).append('.');
		}
		// TABLE:
		if (isCaseSensitive(IdentifierField.TABLE))
			name.append('\"').append(table).append('\"');
		else
			name.append(table);

		return name.toString();
	}

	/**
	 * Sets the name of the table.
	 * 
	 * @param newTableName	The new name of the table.
	 */
	public void setTableName(String newTableName){
		final String temp = normalizeName(newTableName, IdentifierField.TABLE);
		if ((this.table == null && temp != null) || (this.table != null && !this.table.equalsIgnoreCase(temp)))
			dbLink = null;
		this.table = temp;
		if (table != null)
			subQuery = null;
	}

	/**
	 * Updates the whole Table according to the given table reference ({catalog}.{schema}.{table}).
	 * 
	 * @param tableRef	The complete table reference ({catalog}.{schema}.{table}).
	 */
	public final void setTable(String tableRef){
		String[] parts = (tableRef == null) ? null : tableRef.split("\\.");
		if (parts != null && parts.length > 3)
			return;
		else{
			int i = (parts == null) ? -1 : (parts.length - 1);
			setTableName((i < 0) ? null : parts[i--]);
			setSchemaName((i < 0) ? null : parts[i--]);
			setCatalogName((i < 0) ? null : parts[i]);
			if (table != null)
				subQuery = null;
		}
	}

	/**
	 * Gets the sub-query used as table.
	 * 
	 * @return	Sub-query.
	 */
	public final ADQLQuery getSubQuery(){
		return subQuery;
	}

	/**
	 * Sets the sub-query to use as table.
	 * 
	 * @param query	Sub-query (MUST NOT BE NULL).
	 * 
	 * @see #refreshDBLink()
	 */
	public final void setSubQuery(final ADQLQuery query){
		if (query != null){
			// set all ADQLTable attributes:
			subQuery = query;
			catalog = null;
			schema = null;
			table = null;
			dbLink = null;
			position = null;

			// set the DB link:
			refreshDBLink();
		}
	}

	/**
	 * (Re-)Builds a default description of this ADQL table <u>ONLY IF it is a sub-query AND there is an alias</u>.
	 * This method has no effect if this table is not a sub-query or has no alias.
	 * 
	 * @see DefaultDBTable
	 * @see ADQLQuery#getResultingColumns()
	 * @see DBColumn#copy(String, String, DBTable)
	 */
	public final void refreshDBLink(){
		if (isSubQuery() && hasAlias()){
			DefaultDBTable dbTable = new DefaultDBTable(alias);
			DBColumn[] columns = subQuery.getResultingColumns();
			for(DBColumn dbCol : columns)
				dbTable.addColumn(dbCol.copy(dbCol.getADQLName(), dbCol.getADQLName(), dbTable));
			dbLink = dbTable;
		}
	}

	/**
	 * Tells whether this table reference is a sub-query or a table name/alias.
	 * 
	 * @return	<i>true</i> if this table is a sub-query, <i>false</i> else.
	 */
	public final boolean isSubQuery(){
		return subQuery != null;
	}

	/**
	 * Gets the label of this table.
	 * 
	 * @return	Table label.
	 */
	public final String getAlias(){
		return alias;
	}

	/**
	 * Tells whether this table has an alias or not.
	 * 
	 * @return	<i>true</i> if this table has an alias, <i>false</i> otherwise.
	 */
	public final boolean hasAlias(){
		return alias != null;
	}

	/**
	 * Sets the label of this table.
	 * 
	 * @param alias	Label to put on this table.
	 */
	public void setAlias(String alias){
		this.alias = normalizeName(alias, IdentifierField.ALIAS);
	}

	/**
	 * Indicates whether the specified field (catalog, schema or table) is case sensitive or not.
	 * 
	 * @param field		A field (catalog, schema or table).
	 * 
	 * @return			<i>true</i> if the specified field is case sensitive, <i>false</i> otherwise.
	 * 
	 * @see IdentifierField
	 * @see IdentifierField#isCaseSensitive(byte)
	 */
	public final boolean isCaseSensitive(IdentifierField field){
		return field.isCaseSensitive(caseSensitivity);
	}

	/**
	 * Sets the case sensitivity of the specified field (catalog, schema or table).
	 * 
	 * @param field			The field for which the case sensitivity must be updated.
	 * 
	 * @param sensitive		<i>true</i> if the specified field must be case sensitive, <i>false</i> otherwise.
	 * 
	 * @see IdentifierField
	 * @see IdentifierField#setCaseSensitive(byte, boolean)
	 */
	public final void setCaseSensitive(IdentifierField field, boolean sensitive){
		caseSensitivity = field.setCaseSensitive(caseSensitivity, sensitive);
	}

	/**
	 * Indicates whether all fields (catalog, schema and table) are case sensitive.
	 * 
	 * @return			<i>true</i> if all fields are case sensitive, <i>false</i> otherwise.
	 * 
	 * @see IdentifierField#isFullCaseSensitive(byte)
	 */
	public final boolean isCaseSensitive(){
		return IdentifierField.isFullCaseSensitive(caseSensitivity);
	}

	/**
	 * Sets the case sensitivity of all fields (catalog, schema and table).
	 * 
	 * @param sensitive		<i>true</i> if all fields must be case sensitive, <i>false</i> otherwise.
	 * 
	 * @see IdentifierField#getFullCaseSensitive(boolean)
	 */
	public final void setCaseSensitive(boolean sensitive){
		caseSensitivity = IdentifierField.getFullCaseSensitive(sensitive);
	}

	/**
	 * Gets the whole case sensitivity of this ADQL table.
	 * 
	 * @return	Its new case sensitivity (one bit per fields).
	 * 
	 * @see IdentifierField
	 */
	public final byte getCaseSensitive(){
		return caseSensitivity;
	}

	/**
	 * Sets the whole case sensitivity of this ADQL table.
	 * 
	 * @param sensitivity	Its new case sensitivity (one bit per fields).
	 * 
	 * @see IdentifierField
	 */
	public final void setCaseSensitive(final byte sensitivity){
		caseSensitivity = sensitivity;
	}

	/**
	 * <p>Gets the corresponding {@link DBTable}.</p>
	 * <p><i><u>Note:</u> This information is added automatically by {@link DBChecker} when {@link DBChecker#check(adql.query.ADQLQuery)} is called.</i></p>
	 * 
	 * @return The corresponding {@link DBTable}.
	 */
	public final DBTable getDBLink(){
		return dbLink;
	}

	/**
	 * <p>Sets the {@link DBTable} corresponding to this {@link ADQLTable}.</p>
	 * <p><i>
	 * 	<u>Note:</u> This function will do nothing if this {@link ADQLTable} is a sub query.
	 * </i></p>
	 * 
	 * @param dbLink Its corresponding {@link DBTable}.
	 */
	public final void setDBLink(DBTable dbLink){
		if (!isSubQuery())
			this.dbLink = dbLink;
	}

	public SearchColumnList getDBColumns(){
		SearchColumnList list = new SearchColumnList();
		if (isSubQuery() && dbLink == null)
			refreshDBLink();
		if (dbLink != null){
			for(DBColumn dbCol : dbLink)
				list.add(dbCol);
		}
		return list;
	}

	public ArrayList<ADQLTable> getTables(){
		ArrayList<ADQLTable> tables = new ArrayList<ADQLTable>();
		tables.add(this);
		return tables;
	}

	public ArrayList<ADQLTable> getTablesByAlias(final String alias, final boolean caseSensitive){
		ArrayList<ADQLTable> tables = new ArrayList<ADQLTable>();

		if (hasAlias()){
			if (!caseSensitive){
				if (getAlias().equalsIgnoreCase(alias))
					tables.add(this);
			}else{
				if (IdentifierField.ALIAS.isCaseSensitive(caseSensitivity)){
					if (getAlias().equals(alias))
						tables.add(this);
				}else{
					if (getAlias().toLowerCase().equals(alias))
						tables.add(this);
				}
			}
		}

		return tables;
	}

	public ADQLObject getCopy() throws Exception{
		return new ADQLTable(this);
	}

	public String getName(){
		return hasAlias() ? alias : (isSubQuery() ? "{subquery}" : getTableName());
	}

	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private boolean subQueryGot = !isSubQuery();

			public ADQLObject next(){
				if (!subQueryGot){
					subQueryGot = true;
					return subQuery;
				}else
					throw new NoSuchElementException();
			}

			public boolean hasNext(){
				return !subQueryGot;
			}

			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (!subQueryGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();

				if (replacer instanceof ADQLQuery)
					subQuery = (ADQLQuery)replacer;
				else
					throw new UnsupportedOperationException("Impossible to replace a sub-query (" + subQuery.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
			}

			public void remove(){
				if (!subQueryGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the sub-query of an ADQLTable (" + toADQL() + ") !");
			}
		};
	}

	public String toADQL(){
		return (isSubQuery() ? ("(" + subQuery.toADQL() + ")") : getFullTableName()) + ((alias == null) ? "" : (" AS " + (isCaseSensitive(IdentifierField.ALIAS) ? ("\"" + alias + "\"") : alias)));
	}

}