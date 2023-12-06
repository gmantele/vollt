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
 * <p><i><b>WARNING: constructors signature and behavior changed since v2.0!</b>
 * 	Before v2.0, the constructors expected to have the DB names before the ADQL
 * 	names and thus, they forced to give a DB table name ; the ADQL table name
 * 	being optional (if not provided it was set to the DB name).
 * 	But since v2.0, this logic is inverted: the ADQL name is mandatory (a
 * 	{@link NullPointerException} will be thrown if NULL or empty) while the DB
 * 	name is optional ({@link #getDBName()} will return the same as
 * 	{@link #getADQLName()} if no DB name is specified at initialization).
 * 	Consequently, the ADQL names are expected as first parameters.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2019)
 */
public class DefaultDBTable extends DBIdentifier implements DBTable {

	protected String dbCatalogName = null;
	protected String dbSchemaName = null;

	protected String adqlCatalogName = null;
	protected String adqlSchemaName = null;

	protected Map<String, DBColumn> columns = new LinkedHashMap<String, DBColumn>();

	/**
	 * Builds a default {@link DBTable} with the given <b>ADQL name</b>.
	 *
	 * <p>With this constructor: DB name = ADQL name.</p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The ADQL/DB schema and catalog names are set to NULL.
	 * </i></p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	The ADQL table name MUST be NON-qualified (i.e. not prefixed by a schema
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
	 * @param adqlName	The ADQL name of this table (i.e. name to use in ADQL).
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 *
	 * @since 2.0
	 */
	public DefaultDBTable(final String adqlName) throws NullPointerException {
		super(adqlName);
	}

	/**
	 * Builds a default {@link DBTable} with the given ADQL and DB names.
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
	 * @param adqlName	Name used in ADQL queries.
	 * @param dbName	Database name.
	 *                	<i>If NULL, {@link #getDBName()} will return the same as
	 *                	{@link #getADQLName()}.</i>
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 *
	 * @since 2.0
	 */
	public DefaultDBTable(final String adqlName, final String dbName) throws NullPointerException {
		super(adqlName, dbName);
	}

	/**
	 * Builds default {@link DBTable} with a ADQL catalog, schema and table
	 * names.
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
	 * @param adqlCatName		ADQL catalog name (it will be also used as DB
	 *                 			catalog name).
	 * @param adqlSchemaName	ADQL schema name (it will be also used as DB
	 *                   		schema name).
	 * @param adqlName			ADQL table name (it will be also used as DB
	 *              			table name).
	 *                 			<i>MUST NOT be NULL!</i>
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 *
	 * @since 2.0
	 */
	public DefaultDBTable(final String adqlCatName, final String adqlSchemaName, final String adqlName) throws NullPointerException {
		this(adqlCatName, null, adqlSchemaName, null, adqlName, null);
	}

	/**
	 * Builds default {@link DBTable} with the ADQL and DB names for the
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
	 * @param adqlCatName		Catalog name used in ADQL queries.
	 * @param dbCatName			Database catalog name.
	 *                   		<i>If NULL, it will be set to adqlCatName.</i>
	 * @param adqlSchemaName	Schema name used in ADQL queries.
	 * @param dbSchemaName		Database schema name.
	 *                   		<i>If NULL, it will be set to adqlSchemaName.</i>
	 * @param adqlName			Table name used in ADQL queries.
	 *                 			<i>MUST NOT be NULL!</i>
	 * @param dbName			Database table name.
	 *                   		<i>If NULL, it will be set to adqlName.</i>
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 */
	public DefaultDBTable(final String adqlCatName, final String dbCatName, final String adqlSchemaName, final String dbSchemaName, final String adqlName, final String dbName) throws NullPointerException {
		super(adqlName, dbName);

		setADQLSchemaName(adqlSchemaName);
		setDBSchemaName(dbSchemaName);

		setADQLCatalogName(adqlCatName);
		setDBCatalogName(dbCatName);
	}

	@Override
	public final String getDBSchemaName() {
		return (dbSchemaName == null) ? adqlSchemaName : dbSchemaName;
	}

	public final void setDBSchemaName(final String name) {
		dbSchemaName = normalize(name);
	}

	@Override
	public final String getDBCatalogName() {
		return (dbCatalogName == null) ? adqlCatalogName : dbCatalogName;
	}

	public final void setDBCatalogName(final String name) {
		dbCatalogName = normalize(name);
	}

	@Override
	public final String getADQLSchemaName() {
		return adqlSchemaName;
	}

	public void setADQLSchemaName(final String name) {
		adqlSchemaName = normalize(name);
	}

	@Override
	public final String getADQLCatalogName() {
		return adqlCatalogName;
	}

	public void setADQLCatalogName(final String name) {
		adqlCatalogName = normalize(dbName);
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
		DefaultDBTable copy = new DefaultDBTable(adqlCatalogName, dbCatalogName, adqlSchemaName, dbSchemaName, adqlName, dbName);
		copy.setCaseSensitive(this.isCaseSensitive());
		for(DBColumn col : this) {
			if (col instanceof DBCommonColumn)
				copy.addColumn(new DBCommonColumn((DBCommonColumn)col, col.getDBName(), col.getADQLName()));
			else
				copy.addColumn(col.copy(col.getDBName(), col.getADQLName(), copy));
		}
		return copy;
	}
}
