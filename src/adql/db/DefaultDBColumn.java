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
 * Copyright 2012,2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

/**
 * Default implementation of {@link DBColumn}.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (08/2015)
 */
public class DefaultDBColumn implements DBColumn {

	/** Name of the column in the "database". */
	protected String dbName;
	/** Type of the column in the "database".
	 * <i>Note: This should be one of the types listed by the IVOA in the TAP description.</i>
	 * @since 1.3 */
	protected DBType type;
	/** Table in which this column exists. */
	protected DBTable table;
	/** Name that this column must have in ADQL queries. */
	protected String adqlName = null;

	/** Indicate whether the ADQL column name should be considered as case
	 * sensitive.
	 * @since 2.0 */
	protected boolean columnCaseSensitive = false;

	/**
	 * Builds a default {@link DBColumn} with the given DB name and DB table.
	 *
	 * @param dbName	Database column name (it will be also used for the ADQL name).
	 * 					<b>Only the column name is expected. Contrary to {@link DefaultDBTable},
	 * 					if a whole column reference is given, no split will be done.</b>
	 * @param table		DB table which contains this column.
	 *
	 * @see #DefaultDBColumn(String, String, DBType, DBTable)
	 */
	public DefaultDBColumn(final String dbName, final DBTable table) {
		this(dbName, dbName, null, table);
	}

	/**
	 * Builds a default {@link DBColumn} with the given DB name and DB table.
	 *
	 * @param dbName	Database column name (it will be also used for the ADQL name).
	 * 					<b>Only the column name is expected. Contrary to {@link DefaultDBTable},
	 * 					if a whole column reference is given, no split will be done.</b>
	 * @param type		Type of the column.
	 *            		<i>Note: there is no default value. Consequently if this parameter is NULL,
	 *            		the type should be considered as unknown. It means that any comparison with
	 *            		any type will always return 'true'.</i>
	 * @param table		DB table which contains this column.
	 *
	 * @see #DefaultDBColumn(String, String, DBType, DBTable)
	 *
	 * @since 1.3
	 */
	public DefaultDBColumn(final String dbName, final DBType type, final DBTable table) {
		this(dbName, dbName, type, table);
	}

	/**
	 * Builds a default {@link DBColumn} with the given DB name, DB table and ADQL name.
	 *
	 * @param dbName	Database column name.
	 * 					<b>Only the column name is expected. Contrary to {@link DefaultDBTable},
	 * 					if a whole column reference is given, no split will be done.</b>
	 * @param adqlName	Column name used in ADQL queries.
	 * 					<b>Only the column name is expected. Contrary to {@link DefaultDBTable},
	 * 					if a whole column reference is given, no split will be done.</b>
	 * @param table		DB table which contains this column.
	 *
	 * @see #DefaultDBColumn(String, String, DBType, DBTable)
	 */
	public DefaultDBColumn(final String dbName, final String adqlName, final DBTable table) {
		this(dbName, adqlName, null, table);
	}

	/**
	 * Builds a default {@link DBColumn} with the given DB name, DB table and ADQL name.
	 *
	 * @param dbName	Database column name.
	 * 					<b>Only the column name is expected. Contrary to {@link DefaultDBTable},
	 * 					if a whole column reference is given, no split will be done.</b>
	 *              	<b>REQUIRED parameter: it must be not NULL.</b>
	 * @param adqlName	Column name used in ADQL queries.
	 * 					<b>Only the column name is expected. Contrary to {@link DefaultDBTable},
	 * 					if a whole column reference is given, no split will be done.</b>
	 *                	<em>If NULL, it will be set to dbName.</em>
	 * @param type		Type of the column.
	 *            		<i>Note: there is no default value. Consequently if this parameter is NULL,
	 *            		the type should be considered as unknown. It means that any comparison with
	 *            		any type will always return 'true'.</i>
	 * @param table		DB table which contains this column.
	 *
	 * @since 1.3
	 */
	public DefaultDBColumn(final String dbName, final String adqlName, final DBType type, final DBTable table) {

		if (dbName == null || dbName.length() == 0)
			throw new NullPointerException("Missing DB name!");

		this.dbName = dbName;
		setADQLName(adqlName);

		this.type = type;
		this.table = table;
	}

	@Override
	public final String getADQLName() {
		return adqlName;
	}

	public final void setADQLName(String name) {
		if (name != null) {

			// Remove leading and trailing space characters:
			name = name.trim();

			// Detect automatically case sensitivity:
			boolean caseSensitive = DefaultDBTable.isDelimited(name);
			if (caseSensitive)
				name = name.substring(1, name.length() - 1).replaceAll("\"\"", "\"");

			// ONLY if the final name is NOT empty:
			if (name.trim().length() > 0) {
				adqlName = name;
				columnCaseSensitive = caseSensitive;
			}
		}
	}

	@Override
	public boolean isCaseSensitive() {
		return columnCaseSensitive;
	}

	/**
	 * Change the case sensitivity of the ADQL column name.
	 *
	 * @param sensitive	<code>true</code> to consider the current ADQL name as
	 *                 	case sensitive,
	 *                 	<code>false</code> otherwise.
	 */
	public void setCaseSensitive(final boolean sensitive) {
		columnCaseSensitive = sensitive;
	}

	@Override
	public final DBType getDatatype() {
		return type;
	}

	/**
	 * <p>Set the type of this column.</p>
	 *
	 * <p><i>Note 1:
	 * 	The given type should be as closed as possible from a type listed by the IVOA in the TAP protocol description into the section UPLOAD.
	 * </i></p>
	 *
	 * <p><i>Note 2:
	 * 	there is no default value. Consequently if this parameter is NULL,
	 * 	the type should be considered as unknown. It means that any comparison with
	 * 	any type will always return 'true'.
	 * </i></p>
	 *
	 * @param type	New type of this column.
	 *
	 * @since 1.3
	 */
	public final void setDatatype(final DBType type) {
		this.type = type;
	}

	@Override
	public final String getDBName() {
		return dbName;
	}

	@Override
	public final DBTable getTable() {
		return table;
	}

	public final void setTable(final DBTable table) {
		this.table = table;
	}

	@Override
	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable) {
		return new DefaultDBColumn(dbName, adqlName, type, dbTable);
	}

}
