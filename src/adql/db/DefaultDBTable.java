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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of {@link DBTable}.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2019)
 */
public class DefaultDBTable implements DBTable {

	protected String dbCatalogName = null;
	protected String dbSchemaName = null;
	protected String dbName;

	protected String adqlCatalogName = null;
	protected String adqlSchemaName = null;
	protected String adqlName = null;

	protected boolean tableCaseSensitive = false;

	protected Map<String, DBColumn> columns = new LinkedHashMap<String, DBColumn>();

	/**
	 * Builds a default {@link DBTable} with the given DB name.
	 *
	 * <p>With this constructor: ADQL name = DB name.</p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The ADQL/DB schema and catalog names are set to NULL.
	 * </i></p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	The ADQL table name MUST NOT be qualified (i.e. prefixed by a schema
	 * 	and/or a catalog)! For instance, <code>t1</code> is ok, but not
	 * 	<code>schema1.t1</code> or <code>cat1.schema1.t2</code> which won't be
	 * 	split but instead, considered as the whole ADQL name.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	The ADQL table name can be delimited (i.e. surrounded by double quotes).
	 * 	In such case, the surrounded name would be considered as case-sensitive.
	 * </i></p>
	 *
	 * @param dbName	Database name (it will be also used as ADQL table name).
	 *
	 * @see #DefaultDBTable(String, String)
	 */
	public DefaultDBTable(final String dbName) {
		this(dbName, null);
	}

	/**
	 * Builds a default {@link DBTable} with the given DB and ADQL names.
	 *
	 * <p><i><b>Note:</b>
	 * 	The ADQL/DB schema and catalog names are set to NULL.
	 * </i></p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	The ADQL table name MUST NOT be qualified (i.e. prefixed by a schema
	 * 	and/or a catalog)! For instance, <code>t1</code> is ok, but not
	 * 	<code>schema1.t1</code> or <code>cat1.schema1.t2</code> which won't be
	 * 	split but instead, considered as the whole ADQL name.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	The ADQL table name can be delimited (i.e. surrounded by double quotes).
	 * 	In such case, the surrounded name would be considered as case-sensitive.
	 * </i></p>
	 *
	 * @param dbName	Database name.
	 * @param adqlName	Name used in ADQL queries.
	 *                	<i>If NULL, dbName will be used instead.</i>
	 */
	public DefaultDBTable(final String dbName, final String adqlName) {
		if (dbName == null || dbName.trim().length() == 0)
			throw new NullPointerException("Missing DB name!");
		this.dbName = dbName;
		setADQLName(adqlName);
	}

	/**
	 * Builds default {@link DBTable} with a DB catalog, schema and table names.
	 *
	 * <p><i><b>WARNING:</b>
	 * 	The ADQL table name MUST NOT be qualified (i.e. prefixed by a schema
	 * 	and/or a catalog)! For instance, <code>t1</code> is ok, but not
	 * 	<code>schema1.t1</code> or <code>cat1.schema1.t2</code> which won't be
	 * 	split but instead, considered as the whole ADQL name.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	The ADQL table name can be delimited (i.e. surrounded by double quotes).
	 * 	In such case, the surrounded name would be considered as case-sensitive.
	 * </i></p>
	 *
	 * @param dbCatName		Database catalog name (it will be also used as ADQL
	 *                 		catalog name).
	 * @param dbSchemaName	Database schema name (it will be also used as ADQL
	 *                   	schema name).
	 * @param dbName		Database table name (it will be also used as ADQL
	 *              		table name).
	 *                 		<em>MUST NOT be NULL!</em>
	 *
	 * @see #DefaultDBTable(String, String, String, String, String, String)
	 */
	public DefaultDBTable(final String dbCatName, final String dbSchemaName, final String dbName) {
		this(dbCatName, null, dbSchemaName, null, dbName, null);
	}

	/**
	 * Builds default {@link DBTable} with the DB and ADQL names for the
	 * catalog, schema and table.
	 *
	 * <p><i><b>WARNING:</b>
	 * 	The ADQL table name MUST NOT be qualified (i.e. prefixed by a schema
	 * 	and/or a catalog)! For instance, <code>t1</code> is ok, but not
	 * 	<code>schema1.t1</code> or <code>cat1.schema1.t2</code> which won't be
	 * 	split but instead, considered as the whole ADQL name.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	The ADQL table name can be delimited (i.e. surrounded by double quotes).
	 * 	In such case, the surrounded name would be considered as case-sensitive.
	 * </i></p>
	 *
	 * @param dbCatName			Database catalog name.
	 * @param adqlCatName		Catalog name used in ADQL queries.
	 *                   		<em>If NULL, it will be set to dbCatName.</em>
	 * @param dbSchemaName		Database schema name.
	 * @param adqlSchemaName	Schema name used in ADQL queries.
	 *                   		<em>If NULL, it will be set to dbSchemName.</em>
	 * @param dbName			Database table name.
	 *                 			<em>MUST NOT be NULL!</em>
	 * @param adqlName			Table name used in ADQL queries.
	 *                   		<em>If NULL, it will be set to dbName.</em>
	 */
	public DefaultDBTable(final String dbCatName, final String adqlCatName, final String dbSchemaName, final String adqlSchemaName, final String dbName, final String adqlName) {
		if (dbName == null || dbName.trim().length() == 0)
			throw new NullPointerException("Missing DB name!");

		this.dbName = dbName;
		setADQLName(adqlName);

		this.dbSchemaName = dbSchemaName;
		this.adqlSchemaName = (adqlSchemaName == null) ? dbSchemaName : adqlSchemaName;

		this.dbCatalogName = dbCatName;
		this.adqlCatalogName = (adqlCatName == null) ? dbCatName : adqlCatName;
	}

	@Override
	public final String getDBName() {
		return dbName;
	}

	@Override
	public final String getDBSchemaName() {
		return dbSchemaName;
	}

	@Override
	public final String getDBCatalogName() {
		return dbCatalogName;
	}

	@Override
	public final String getADQLName() {
		return adqlName;
	}

	/**
	 * Change the ADQL name of this table.
	 *
	 * <p>
	 * 	The case sensitivity is automatically set. The table name will be
	 * 	considered as case sensitive if the given name is surrounded by double
	 * 	quotes (<code>"</code>). In such case, the table name is stored and then
	 * 	returned WITHOUT these double quotes.
	 * </p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	If the name without the double quotes (and then trimmed) is an empty
	 * 	string, the ADQL name will be set to the {@link #getDBName()} as such.
	 * 	Then the case sensitivity will be set to <code>false</code>.
	 * </i></p>
	 *
	 * @param name	New ADQL name of this table.
	 */
	public void setADQLName(final String name) {
		// Set the new table name (only if not NULL, otherwise use the DB name):
		adqlName = (name != null) ? name : dbName;

		// Detect automatically case sensitivity:
		if ((tableCaseSensitive = isDelimited(adqlName)))
			adqlName = adqlName.substring(1, adqlName.length() - 1).replaceAll("\"\"", "\"");

		// If the final name is empty, no case sensitivity and use the DB name:
		if (adqlName.trim().length() == 0) {
			adqlName = dbName;
			tableCaseSensitive = false;
		}
	}

	/**
	 * Tell whether the given identifier is delimited (i.e. within the same pair
	 * of double quotes - <code>"</code>).
	 *
	 * <i>
	 * <p>The following identifiers ARE delimited:</p>
	 * <ul>
	 * 	<li><code>"a"</code></li>
	 * 	<li><code>" "</code> (string with spaces ; but won't be considered as a
	 * 	                      valid ADQL name)</li>
	 * 	<li><code>"foo.bar"</code></li>
	 * 	<li><code>"foo"".""bar"</code> (with escaped double quotes)</li>
	 * 	<li><code>""""</code> (idem)</li>
	 * </ul>
	 * </i>
	 *
	 * <i>
	 * <p>The following identifiers are NOT considered as delimited:</p>
	 * <ul>
	 * 	<li><code>""</code> (empty string)</li>
	 * 	<li><code>"foo</code> (missing ending double quote)</li>
	 * 	<li><code>foo"</code> (missing leading double quote)</li>
	 * 	<li><code>"foo"."bar"</code> (not the same pair of double quotes)</li>
	 * </ul>
	 * </i>
	 *
	 * @param name	Identifier that may be delimited.
	 *
	 * @return	<code>true</code> if the given identifier is delimited,
	 *        	<code>false</code> otherwise.
	 *
	 * @since 2.0
	 */
	public static final boolean isDelimited(final String name) {
		return name != null && name.matches("\"(\"\"|[^\"])*\"");
	}

	@Override
	public boolean isCaseSensitive() {
		return tableCaseSensitive;
	}

	public void setCaseSensitive(final boolean sensitive) {
		tableCaseSensitive = sensitive;
	}

	@Override
	public final String getADQLSchemaName() {
		return adqlSchemaName;
	}

	public void setADQLSchemaName(final String name) {
		adqlSchemaName = (name != null) ? name : dbSchemaName;
	}

	@Override
	public final String getADQLCatalogName() {
		return adqlCatalogName;
	}

	public void setADQLCatalogName(final String name) {
		adqlName = (name != null) ? null : dbName;
	}

	/**
	 * <p>Case sensitive !</p>
	 * <p>Research optimized for researches by ADQL name.</p>
	 *
	 * @see adql.db.DBTable#getColumn(java.lang.String, boolean)
	 */
	@Override
	public DBColumn getColumn(String colName, boolean byAdqlName) {
		if (byAdqlName)
			return columns.get(colName);
		else {
			for(DBColumn col : columns.values()) {
				if (col.getDBName().equals(colName))
					return col;
			}
			return null;
		}
	}

	public boolean hasColumn(String colName, boolean byAdqlName) {
		return (getColumn(colName, byAdqlName) != null);
	}

	@Override
	public Iterator<DBColumn> iterator() {
		return columns.values().iterator();
	}

	public void addColumn(DBColumn column) {
		if (column != null)
			columns.put(column.getADQLName(), column);
	}

	public void addAllColumns(Collection<DBColumn> colList) {
		if (colList != null) {
			for(DBColumn column : colList)
				addColumn(column);
		}
	}

	/**
	 * Splits the given table name in 3 parts: catalog, schema, table.
	 *
	 * @param table	The table name to split.
	 *
	 * @return	A String array of 3 items: [0]=catalog, [1]=schema, [0]=table.
	 *
	 * @deprecated	Since v2.0, the table name is not any more split
	 *            	automatically.
	 */
	@Deprecated
	public static final String[] splitTableName(final String table) {
		String[] splitRes = new String[]{ null, null, null };

		if (table == null || table.trim().length() == 0)
			return splitRes;

		String[] names = table.trim().split("\\.");
		switch(names.length) {
			case 1:
				splitRes[2] = table.trim();
				break;
			case 2:
				splitRes[2] = names[1].trim();
				splitRes[1] = names[0].trim();
				break;
			case 3:
				splitRes[2] = names[2].trim();
				splitRes[1] = names[1].trim();
				splitRes[0] = names[0].trim();
				break;
			default:
				splitRes[2] = names[names.length - 1].trim();
				splitRes[1] = names[names.length - 2].trim();
				StringBuffer buff = new StringBuffer(names[0].trim());
				for(int i = 1; i < names.length - 2; i++)
					buff.append('.').append(names[i].trim());
				splitRes[0] = buff.toString();
		}

		return splitRes;
	}

	/**
	 * <p>Join the last 3 items of the given string array with a dot ('.').
	 * These three parts should be: [0]=catalog name, [1]=schema name, [2]=table name.</p>
	 *
	 * <p>
	 * 	If the array contains less than 3 items, all the given items will be though joined.
	 * 	However, if it contains more than 3 items, only the three last items will be.
	 * </p>
	 *
	 * <p>A null item will be written as an empty string (string of length 0 ; "").</p>
	 *
	 * <p>
	 * 	In the case the first and the third items are not null, but the second is null, the final string will contain in the middle two dots.
	 * 	Example: if the array is {"cat", NULL, "table"}, then the joined string will be: "cat..table".
	 * </p>
	 *
	 * @param nameParts	String items to join.
	 *
	 * @return	A string joining the 3 last string items of the given array,
	 *        	or an empty string if the given array is NULL.
	 *
	 * @since 1.3
	 *
	 * @deprecated	Since v2.0, the table name is not any more split
	 *            	automatically. So, it is not any more needed to join all its
	 *            	parts.
	 */
	@Deprecated
	public static final String joinTableName(final String[] nameParts) {
		if (nameParts == null)
			return "";

		StringBuffer str = new StringBuffer();
		boolean empty = true;
		for(int i = (nameParts.length <= 3) ? 0 : (nameParts.length - 3); i < nameParts.length; i++) {
			if (!empty)
				str.append('.');

			String part = (nameParts[i] == null) ? null : nameParts[i].trim();
			if (part != null && part.length() > 0) {
				str.append(part);
				empty = false;
			}
		}
		return str.toString();
	}

	@Override
	public DBTable copy(String dbName, String adqlName) {
		DefaultDBTable copy = new DefaultDBTable(dbCatalogName, adqlCatalogName, dbSchemaName, adqlSchemaName, dbName, adqlName);
		copy.tableCaseSensitive = tableCaseSensitive;
		for(DBColumn col : this) {
			if (col instanceof DBCommonColumn)
				copy.addColumn(new DBCommonColumn((DBCommonColumn)col, col.getDBName(), col.getADQLName()));
			else
				copy.addColumn(col.copy(col.getDBName(), col.getADQLName(), copy));
		}
		return copy;
	}
}
