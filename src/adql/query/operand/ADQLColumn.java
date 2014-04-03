package adql.query.operand;

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

import adql.db.DBColumn;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.IdentifierField;
import adql.query.NullADQLIterator;
import adql.query.TextPosition;
import adql.query.from.ADQLTable;

/**
 * Represents the complete (literal) reference to a column ({schema(s)}.{table}.{column}).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 07/2011
 */
public class ADQLColumn implements ADQLOperand {

	/** Position in the original ADQL query string. */
	private TextPosition position = null;

	/** The name of the catalog which contains this column. */
	private String catalog = null;

	/** The name of the schema which contains this column. */
	private String schema = null;

	/** The name of the table which contains this column. */
	private String table = null;

	/** Column name (NEVER <i>null</i> but ""). */
	private String column;

	/** Lets specify the case sensitivity of the catalog, schema, table and column parts. */
	private byte caseSensitivity = 0;

	/** The corresponding column in the "database". By default, this field is automatically filled by {@link adql.db.DBChecker}. */
	private DBColumn dbLink = null;

	/** The {@link ADQLTable} which is supposed to contain this column. By default, this field is automatically filled by {@link adql.db.DBChecker}. */
	private ADQLTable adqlTable = null;


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds a Column with the complete reference to a column ({schema(s)}.{table}.{column}).
	 * 
	 * @param columnRef	The complete reference to a column.
	 * 
	 * @see ADQLColumn#setColumn(String)
	 */
	public ADQLColumn(String columnRef){
		setColumn(columnRef);
	}

	/**
	 * Builds a column with the given column name and the given table name.
	 * 
	 * @param tableName		Name of the table.
	 * @param columnName	Name of the column.
	 * 
	 * @see ADQLColumn#setTableName(String)
	 * @see ADQLColumn#setColumnName(String)
	 */
	public ADQLColumn(String tableName, String columnName){
		setTableName(tableName);
		setColumnName(columnName);
	}

	/**
	 * Builds a column with the given column name, table name and schema name.
	 * 
	 * @param schema	Name of the schema.
	 * @param table		Name of the table.
	 * @param column	Name of the column.
	 * 
	 * @see #ADQLColumn(String, String)
	 * @see #setSchemaName(String)
	 */
	public ADQLColumn(String schema, String table, String column){
		this(table, column);
		setSchemaName(schema);
	}

	/**
	 * Builds a column with the given column name, table name, schema name and catalog name.
	 * 
	 * @param catalog 	Name of the catalog.
	 * @param schema	Name of the schema.
	 * @param table		Name of the table.
	 * @param column	Name of the column.
	 * 
	 * @see #ADQLColumn(String, String)
	 * @see #setSchemaName(String)
	 */
	public ADQLColumn(String catalog, String schema, String table, String column){
		this(schema, table, column);
		setCatalogName(catalog);
	}

	/**
	 * Builds a Column by copying the given one.
	 * 
	 * @param toCopy	The Column to copy.
	 */
	public ADQLColumn(ADQLColumn toCopy){
		column = toCopy.column;
		table = toCopy.table;
	}

	/**
	 * Lets normalizing any catalog/schema/table name or alias.
	 * If the name is surrounded by double-quotes, they are removed
	 * and the corresponding field will be declared as case sensitive.
	 * 
	 * @param name	Name to normalize.
	 * 
	 * @return		The normalized name.
	 */
	protected String normalizeName(String name, IdentifierField field){
		if (name == null)
			return null;

		StringBuffer n = new StringBuffer(name);
		n.trimToSize();
		if (n.length() == 0)
			return null;
		else{
			if (n.length() > 1 && n.charAt(0) == '\"' && n.charAt(n.length()-1) == '\"'){
				n.deleteCharAt(0);
				n.deleteCharAt(n.length()-1);
				n.trimToSize();
				if (n.length() == 0)
					return null;
				else
					setCaseSensitive(field, true);
			}
		}
		return n.toString();
	}


	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	/**
	 * Gets the position in the original ADQL query string.
	 * 
	 * @return	The position of this {@link ADQLTable}.
	 */
	public final TextPosition getPosition(){
		return position;
	}

	/**
	 * Sets the position at which this {@link ADQLColumn} has been found in the original ADQL query string.
	 * 
	 * @param pos	Position of this {@link ADQLColumn}.
	 */
	public void setPosition(final TextPosition pos) {
		position = pos;
	}

	/**
	 * Gets the name of the catalog which contains this column.
	 * 
	 * @return Catalog name.
	 */
	public final String getCatalogName() {
		return catalog;
	}

	/**
	 * Sets the name of the catalog which contains this column.
	 * 
	 * @param catalog New name of the catalog.
	 */
	public final void setCatalogName(String catalog) {
		final String temp = normalizeName(catalog, IdentifierField.CATALOG);
		if ((this.catalog == null && temp != null) || (this.catalog != null && !this.catalog.equalsIgnoreCase(temp)))
			dbLink = null;
		this.catalog = temp;
	}

	/**
	 * Gets the name of the schema which contains this column.
	 * 
	 * @return Schema name.
	 */
	public final String getSchemaName() {
		return schema;
	}

	/**
	 * Sets the name of the schema which contains this column.
	 * 
	 * @param schema New name of the schema.
	 */
	public final void setSchemaName(String schema) {
		final String temp = normalizeName(schema, IdentifierField.SCHEMA);
		if ((this.schema == null && temp != null) || (this.schema != null && !this.schema.equalsIgnoreCase(temp)))
			dbLink = null;
		this.schema = temp;
	}

	/**
	 * Gets the name of the table which contains this column.
	 * 
	 * @return	Table name.
	 */
	public final String getTableName(){
		return table;
	}

	/**
	 * Sets the name of the table which contains this column.
	 * 
	 * @param tableName		New name of the table.
	 */
	public final void setTableName(String tableName){
		final String temp = normalizeName(tableName, IdentifierField.TABLE);
		if ((this.table == null && temp != null) || (this.table != null && !this.table.equalsIgnoreCase(temp)))
			dbLink = null;
		this.table = temp;
	}

	/**
	 * Gets the name of this column.
	 * 
	 * @return	Its column name.
	 */
	public final String getColumnName(){
		return column;
	}

	/**
	 * Gets the full name of this column (catalogName . schemaName . tableName . columnName)
	 * by respecting the case sensitivity of each field (if case sensitive, double-quotes will surround the concerned fields name).
	 * 
	 * @return	Its full name.
	 * 
	 * @see #getFullColumnPrefix()
	 */
	public final String getFullColumnName(){
		if (column == null)
			return "";

		StringBuffer name = getFullColumnPrefix();
		if (name.length() > 0)
			name.append('.');

		// COLUMN:
		if (isCaseSensitive(IdentifierField.COLUMN))
			name.append('\"').append(column).append('\"');
		else
			name.append(column);

		return name.toString();
	}

	/**
	 * Gets the full column prefix (catalogName . schemaName . tableName)
	 * by respecting the case sensitivity of each field (if case sensitive, double-quotes will surround the concerned fields name).
	 * 
	 * @return	Its full prefix.
	 */
	public final StringBuffer getFullColumnPrefix(){
		StringBuffer name = new StringBuffer();

		// CATALOG:
		if (catalog != null){
			if (isCaseSensitive(IdentifierField.CATALOG))
				name.append('\"').append(catalog).append("\".");
			else
				name.append(catalog).append('.');
		}
		// SCHEMA:
		if (schema != null){
			if (isCaseSensitive(IdentifierField.SCHEMA))
				name.append('\"').append(schema).append("\".");
			else
				name.append(schema).append('.');
		}
		// TABLE:
		if (table != null){
			if (isCaseSensitive(IdentifierField.TABLE))
				name.append('\"').append(table).append("\"");
			else
				name.append(table);
		}

		return name;
	}

	/**
	 * Changes the name of the column ({column} in {schema(s)}.{table}.{column}).
	 * 
	 * @param columnName	The new column name.
	 */
	public final void setColumnName(String columnName){
		final String temp = normalizeName(columnName, IdentifierField.COLUMN);
		if ((this.column == null && temp != null) || (this.column != null && !this.column.equalsIgnoreCase(temp)))
			dbLink = null;
		this.column = temp;
	}

	/**
	 * Updates the whole Column according to the given column reference ({catalog}.{schema}.{table}.{column}).
	 * 
	 * @param columnRef	The complete column reference ({catalog}.{schema}.{table}.{column}).
	 */
	public final void setColumn(String columnRef){
		String[] parts = (columnRef == null)?null:columnRef.split("\\.");
		if (parts != null && parts.length > 4)
			return;
		else{
			int i = (parts==null)?-1:(parts.length-1);
			setColumnName((i<0)?null:parts[i--]);
			setTableName((i<0)?null:parts[i--]);
			setSchemaName((i<0)?null:parts[i--]);
			setCatalogName((i<0)?null:parts[i]);
		}
	}

	/**
	 * Indicates whether the specified field (catalog, schema, table or column) is case sensitive or not.
	 * 
	 * @param field		A field (catalog, schema, table or column).
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
	 * Sets the case sensitivity of the specified field (catalog, schema, table, column).
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
	 * Indicates whether all fields (catalog, schema, table and column) are case sensitive.
	 * 
	 * @return			<i>true</i> if all fields are case sensitive, <i>false</i> otherwise.
	 * 
	 * @see IdentifierField#isFullCaseSensitive(byte)
	 */
	public final boolean isCaseSensitive(){
		return IdentifierField.isFullCaseSensitive(caseSensitivity);
	}

	/**
	 * Sets the case sensitivity of all fields (catalog, schema, table and column).
	 * 
	 * @param sensitive		<i>true</i> if all fields must be case sensitive, <i>false</i> otherwise.
	 * 
	 * @see IdentifierField#getFullCaseSensitive(boolean)
	 */
	public final void setCaseSensitive(boolean sensitive){
		caseSensitivity = IdentifierField.getFullCaseSensitive(sensitive);
	}

	/**
	 * Gets the whole case sensitivity of this ADQL column.
	 * 
	 * @return	Its new case sensitivity (one bit per fields).
	 * 
	 * @see IdentifierField
	 */
	public final byte getCaseSensitive(){
		return caseSensitivity;
	}

	/**
	 * Sets the whole case sensitivity of this ADQL column.
	 * 
	 * @param sensitivity	Its new case sensitivity (one bit per fields).
	 * 
	 * @see IdentifierField
	 */
	public final void setCaseSensitive(final byte sensitivity){
		caseSensitivity = sensitivity;
	}

	/**
	 * Gets the corresponding {@link DBColumn}.
	 * 
	 * @return The corresponding {@link DBColumn}.
	 */
	public final DBColumn getDBLink() {
		return dbLink;
	}

	/**
	 * <p>Sets the {@link DBColumn} corresponding to this {@link ADQLColumn}.</p>
	 * 
	 * <p>By default, this field is automatically filled by {@link adql.db.DBChecker}.</p>
	 * 
	 * @param dbLink Its corresponding {@link DBColumn}.
	 */
	public final void setDBLink(DBColumn dbLink) {
		this.dbLink = dbLink;
	}

	/**
	 * Gets the {@link ADQLTable} from which this column is supposed to come.
	 * 
	 * @return 	Its source table.
	 */
	public final ADQLTable getAdqlTable() {
		return adqlTable;
	}

	/**
	 * <p>Sets the {@link ADQLTable} from which this column is supposed to come.</p>
	 * 
	 * <p>By default, this field is automatically filled by {@link adql.db.DBChecker} when {@link adql.db.DBChecker#check(adql.query.ADQLQuery)} is called.</p>
	 * 
	 * @param adqlTable Its source table.
	 */
	public final void setAdqlTable(ADQLTable adqlTable) {
		this.adqlTable = adqlTable;
	}


	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	public boolean isNumeric(){
		return true;
	}

	public boolean isString() {
		return true;
	}

	public ADQLObject getCopy() throws Exception {
		return new ADQLColumn(this);
	}

	public String getName() {
		return getColumnName();
	}

	public ADQLIterator adqlIterator(){
		return new NullADQLIterator();
	}

	public String toADQL() {
		return getFullColumnName();
	}

	@Override
	public String toString(){
		return toADQL();
	}

}
