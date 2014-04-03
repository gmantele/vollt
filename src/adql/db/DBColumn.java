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
 * Copyright 2011 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * <p>Definition of a valid target column.</p>
 * 
 * <p>
 * 	This column can be used in an ADQL query with its ADQL name ({@link #getADQLName()})
 * 	and corresponds to a real column in the "database" with its DB name ({@link #getDBName()}).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
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
