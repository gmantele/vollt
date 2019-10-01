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

/**
 * Default implementation of {@link DBColumn}.
 *
 * <p><i><b>WARNING: constructors signature and behavior changed since v2.0!</b>
 * 	Before v2.0, the constructors expected to have the DB names before the ADQL
 * 	names and thus, they forced to give a DB column name ; the ADQL column name
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
public class DefaultDBColumn extends DBIdentifier implements DBColumn {

	/** Type of the column in the "database".
	 * <i>Note: This should be one of the types listed by the IVOA in the TAP description.</i>
	 * @since 1.3 */
	protected DBType type;

	/** Table in which this column exists. */
	protected DBTable table;

	/**
	 * Builds a default {@link DBColumn} with the given ADQL name and table.
	 *
	 * <p>With this constructor: DB name = ADQL name.</p>
	 *
	 * @param adqlName	The ADQL name of this column (i.e. name to use in ADQL).
	 * @param table		Table which contains this column.
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 *
	 * @since 2.0
	 */
	public DefaultDBColumn(final String adqlName, final DBTable table) throws NullPointerException {
		this(adqlName, null, null, table);
	}

	/**
	 * Builds a default {@link DBColumn} with the given ADQL name and table.
	 *
	 * @param adqlName	The ADQL name of this column (i.e. name to use in ADQL).
	 * @param type		Type of the column.
	 *            		<i><b>Note:</b> there is no default value. Consequently
	 *            		if this parameter is NULL, the type should be considered
	 *            		as unknown. It means that any comparison with any type
	 *            		will always return <code>true</code>.</i>
	 * @param table		Table which contains this column.
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 *
	 * @since 2.0
	 */
	public DefaultDBColumn(final String adqlName, final DBType type, final DBTable table) throws NullPointerException {
		this(adqlName, null, type, table);
	}

	/**
	 * Builds a default {@link DBColumn} with the given ADQL and DB names and
	 * table.
	 *
	 * @param adqlName	The ADQL name of this column (i.e. name to use in ADQL).
	 * @param dbName	Database name.
	 *                	<i>If NULL, {@link #getDBName()} will return the same as
	 *                	{@link #getADQLName()}.</i>
	 * @param table		Table which contains this column.
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 *
	 * @since 2.0
	 */
	public DefaultDBColumn(final String adqlName, final String dbName, final DBTable table) throws NullPointerException {
		this(adqlName, dbName, null, table);
	}

	/**
	 * Builds a default {@link DBColumn} with the given ADQL and DB names, type
	 * and table
	 *
	 * @param adqlName	The ADQL name of this column (i.e. name to use in ADQL).
	 * @param dbName	Database name.
	 *                	<i>If NULL, {@link #getDBName()} will return the same as
	 *                	{@link #getADQLName()}.</i>
	 * @param type		Type of the column.
	 *            		<i><b>Note:</b> there is no default value. Consequently
	 *            		if this parameter is NULL, the type should be considered
	 *            		as unknown. It means that any comparison with any type
	 *            		will always return <code>true</code>.</i>
	 * @param table		Table which contains this column.
	 *
	 * @throws NullPointerException	If the given ADQL name is NULL or empty.
	 *
	 * @since 2.0
	 */
	public DefaultDBColumn(final String adqlName, final String dbName, final DBType type, final DBTable table) throws NullPointerException {
		super(adqlName, dbName);

		this.type = type;
		this.table = table;
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
	public final DBTable getTable() {
		return table;
	}

	public final void setTable(final DBTable table) {
		this.table = table;
	}

	@Override
	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable) {
		return new DefaultDBColumn(adqlName, dbName, type, dbTable);
	}

}
