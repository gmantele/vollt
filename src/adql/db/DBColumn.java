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
 * Copyright 2011-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

/**
 * Definition of a valid target column.
 *
 * <p>
 * 	This column can be used in an ADQL query with its ADQL name
 * 	({@link #getADQLName()}) and corresponds to a real column in the "database"
 * 	with its DB name ({@link #getDBName()}).
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2019)
 */
public interface DBColumn {

	/**
	 * Gets the name of this column.
	 *
	 * <i>
	 * <p><b>Notes:</b>
	 * 	The returned ADQL name is:
	 * </p>
	 * <ul>
	 * 	<li>non-empty/NULL</li>
	 * 	<li>non-delimited (i.e. not between double quotes),</li>
	 * 	<li>non-prefixed (i.e. no table/schema/catalog name)</li>
	 * 	<li>and in the same case as provided at initialization (even if not case
	 * 		sensitive).</li>
	 * </ul>
	 * </i>
	 *
	 * @return	Its ADQL name.
	 */
	public String getADQLName();

	/**
	 * Tell whether the column name used in ADQL queries must be delimited
	 * (i.e. surrounded by <code>"</code>). In such case, it will be case
	 * sensitive.
	 *
	 * @return	<code>true</code> if the ADQL column name is case sensitive,
	 *        	<code>false</code> otherwise.
	 *
	 * @since 2.0
	 */
	public boolean isCaseSensitive();

	/**
	 * Gets the name of this column in the "database" (e.g. as it should be used
	 * in SQL queries).
	 *
	 * <i>
	 * <p><b>Notes</b>
	 * 	The returned DB name is:
	 * </p>
	 * <ul>
	 * 	<li>non-empty/NULL</li>
	 * 	<li>non-delimited (i.e. not between double quotes),</li>
	 * 	<li>non-prefixed (i.e. no table/schema/catalog name)</li>
	 * 	<li>and in the EXACT case as it MUST be used.</li>
	 * </ul>
	 *
	 * @return	Its DB name.
	 */
	public String getDBName();

	/**
	 * Get the type of this column (as closed as possible from the "database"
	 * type).
	 *
	 * <p><i><b>Note:</b>
	 * 	The returned type should be as closed as possible from a type listed by
	 * 	the IVOA in the TAP protocol description into the section UPLOAD.
	 * </i></p>
	 *
	 * @return	Its type.
	 *
	 * @since 1.3
	 */
	public DBType getDatatype();

	/**
	 * Gets the table which contains this {@link DBColumn}.
	 *
	 * @return	Its table
	 *        	or NULL if no table is specified.
	 */
	public DBTable getTable();

	/**
	 * Makes a copy of this instance of {@link DBColumn}.
	 *
	 * @param dbName	Its new DB name.
	 * @param adqlName	Its new ADQL name.
	 * @param dbTable	Its new table.
	 *
	 * @return			A modified copy of this {@link DBColumn}.
	 */
	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable);

}
