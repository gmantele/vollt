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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

/**
 * Default implementation of {@link DBColumn}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
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
	public DefaultDBColumn(final String dbName, final DBTable table){
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
	public DefaultDBColumn(final String dbName, final DBType type, final DBTable table){
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
	public DefaultDBColumn(final String dbName, final String adqlName, final DBTable table){
		this(dbName, adqlName, null, table);
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
	 * @param type		Type of the column.
	 *            		<i>Note: there is no default value. Consequently if this parameter is NULL,
	 *            		the type should be considered as unknown. It means that any comparison with
	 *            		any type will always return 'true'.</i>
	 * @param table		DB table which contains this column.
	 * 
	 * @since 1.3
	 */
	public DefaultDBColumn(final String dbName, final String adqlName, final DBType type, final DBTable table){
		this.dbName = dbName;
		this.adqlName = adqlName;
		this.type = type;
		this.table = table;
	}

	@Override
	public final String getADQLName(){
		return adqlName;
	}

	public final void setADQLName(final String adqlName){
		if (adqlName != null)
			this.adqlName = adqlName;
	}

	@Override
	public final DBType getDatatype(){
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
	public final void setDatatype(final DBType type){
		this.type = type;
	}

	@Override
	public final String getDBName(){
		return dbName;
	}

	@Override
	public final DBTable getTable(){
		return table;
	}

	public final void setTable(final DBTable table){
		this.table = table;
	}

	@Override
	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable){
		return new DefaultDBColumn(dbName, adqlName, type, dbTable);
	}

}
