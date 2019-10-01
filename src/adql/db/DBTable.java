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
 * Copyright 2012-2019- UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

/**
 * Definition of a valid target table.
 *
 * <p>
 * 	This table can be used in an ADQL query with its ADQL name
 * 	({@link #getADQLName()}) and corresponds to a real table in the "database"
 * 	with its DB name ({@link #getDBName()}).
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2019)
 */
public interface DBTable extends Iterable<DBColumn> {

	/**
	 * Gets the name of this table in ADQL queries.
	 *
	 * <i>
	 * <p><b>Notes:</b>
	 * 	The returned ADQL name is:
	 * </p>
	 * <ul>
	 * 	<li>non-empty/NULL</li>
	 * 	<li>non-delimited (i.e. not between double quotes),</li>
	 * 	<li>non-prefixed (i.e. no schema/catalog name)</li>
	 * 	<li>and in the same case as provided at initialization (even if not case
	 * 		sensitive).</li>
	 * </ul>
	 * </i>
	 *
	 * @return	Its ADQL name.
	 */
	public String getADQLName();

	/**
	 * Tell whether the table name used in ADQL queries must be delimited
	 * (i.e. surrounded by <code>"</code>). In such case, it will be case
	 * sensitive.
	 *
	 * @return	<code>true</code> if the ADQL table name is case sensitive,
	 *        	<code>false</code> otherwise.
	 *
	 * @since 2.0
	 */
	public boolean isCaseSensitive();

	/**
	 * Gets the name of this table in the "database" (e.g. as it should be used
	 * in SQL queries).
	 *
	 * <i>
	 * <p><b>Notes</b>
	 * 	The returned DB name is:
	 * </p>
	 * <ul>
	 * 	<li>non-empty/NULL</li>
	 * 	<li>non-delimited (i.e. not between double quotes),</li>
	 * 	<li>non-prefixed (i.e. no schema/catalog name)</li>
	 * 	<li>and in the EXACT case as it MUST be used.</li>
	 * </ul>
	 *
	 * @return	Its DB name.
	 */
	public String getDBName();

	/**
	 * Gets the ADQL name of the schema which contains this table.
	 *
	 * <p><i><b>Warning!</b>
	 * 	Same rules as {@link #getADQLName()}.
	 * </i></p>
	 *
	 * @return	ADQL name of its schema.
	 */
	public String getADQLSchemaName();

	/**
	 * Gets the DB name of the schema which contains this table.
	 *
	 * <p><i><b>Warning!</b>
	 * 	Same rules as {@link #getDBName()}.
	 * </i></p>
	 *
	 * @return	DB name of its schema.
	 */
	public String getDBSchemaName();

	/**
	 * Gets the ADQL name of the catalog which contains this table.
	 *
	 * <p><i><b>Warning!</b>
	 * 	Same rules as {@link #getADQLName()}.
	 * </i></p>
	 *
	 * @return	ADQL name of its catalog.
	 */
	public String getADQLCatalogName();

	/**
	 * Gets the DB name of the catalog which contains this table.
	 *
	 * <p><i><b>Warning!</b>
	 * 	Same rules as {@link #getDBName()}.
	 * </i></p>
	 *
	 * @return	DB name of its catalog.
	 */
	public String getDBCatalogName();

	/**
	 * Gets the definition of the specified column if it exists in this table.
	 *
	 * @param colName	Name of the column <i>(may be the ADQL or DB name
	 *               	depending of the second parameter)</i>.
	 * @param adqlName	<code>true</code> means the given name is the ADQL name
	 *                	of the column and that the research must be done on the
	 *                	ADQL name of columns,
	 * 					<code>false</code> means the same thing but with the DB
	 *                	name.
	 *
	 * @return	The corresponding column,
	 *        	or NULL if the specified column had not been found.
	 */
	public DBColumn getColumn(String colName, boolean adqlName);

	/**
	 * Makes a copy of this instance of {@link DBTable}, with the possibility
	 * to change the DB and ADQL names.
	 *
	 * <p><b>IMPORTANT:</b></p>
	 * <ul>
	 * 	<li><b>The given DB and ADQL name may be NULL.</b> If NULL, the copy
	 * 		will contain exactly the same full name (DB and/or ADQL).</li>
	 * 	<li><b>they may be qualified</b> (that's to say: prefixed by the
	 * 		schema name or by the catalog and schema name). It means that it is
	 * 		possible to change the catalog, schema and table name in the copy.</li>
	 * 	<li><b>they may be delimited</b> (that's to say: written between double
	 * 		quotes to force case sensitivity).</li>
	 * </ul>
	 * <i>
	 * <p><b>For instance:</b></p>
	 * <ul>
	 * 	<li><code>.copy(null, "foo")</code> => a copy with the same full DB
	 * 		name, but with no ADQL catalog and schema name and with an ADQL
	 * 		table name equals to "foo"</li>
	 * 	<li><code>.copy("schema.table", null)</code> => a copy with the same
	 * 		full ADQL name, but with no DB catalog name, with a DB schema name
	 * 		equals to "schema" and with a DB table name equals to "table"</li>
	 * </ul>
	 * </i>
	 *
	 * @param dbName	Its new DB name.
	 *              	It may be qualified and/or delimited.
	 *              	It may also be NULL ; if so, the full DB name won't be
	 *              	different in the copy.
	 * @param adqlName	Its new ADQL name. It may be qualified and/or delimited.
	 *              	It may also be NULL ; if so, the full DB name won't be
	 *              	different in the copy.
	 *
	 * @return	A modified copy of this {@link DBTable}.
	 */
	public DBTable copy(final String dbName, final String adqlName);
}
