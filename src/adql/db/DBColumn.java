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
 * Copyright 2011,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

/**
 * <p>Definition of a valid target column.</p>
 * 
 * <p>
 * 	This column can be used in an ADQL query with its ADQL name ({@link #getADQLName()})
 * 	and corresponds to a real column in the "database" with its DB name ({@link #getDBName()}).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public interface DBColumn {

	/**
	 * Gets the name of this column which must be used in an ADQL query.
	 * 
	 * @return	Its ADQL name.
	 */
	public String getADQLName();

	/**
	 * Gets the name of this column in the "database".
	 * 
	 * @return	Its DB name.
	 */
	public String getDBName();

	/**
	 * <p>Get the type of this column (as closed as possible from the "database" type).</p>
	 * 
	 * <p><i>Note:
	 * 	The returned type should be as closed as possible from a type listed by the IVOA in the TAP protocol description into the section UPLOAD.
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
	 * @return	Its table or <i>null</i> if no table is specified.
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
