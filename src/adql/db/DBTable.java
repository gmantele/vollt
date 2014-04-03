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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * <p>Definition of a valid target table.</p>
 * 
 * <p>
 * 	This table can be used in an ADQL query with its ADQL name ({@link #getADQLName()})
 * 	and corresponds to a real table in the "database" with its DB name ({@link #getDBName()}).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 07/2011
 */
public interface DBTable extends Iterable<DBColumn> {

	/**
	 * Gets the name of this table which must be used in an ADQL query.
	 * 
	 * @return	Its ADQL name.
	 */
	public String getADQLName();

	/**
	 * Gets the name of this table in the "database".
	 * 
	 * @return	Its DB name.
	 */
	public String getDBName();

	/**
	 * Gets the ADQL name of the schema which contains this table.
	 * 
	 * @return	ADQL name of its schema.
	 */
	public String getADQLSchemaName();

	/**
	 * Gets the DB name of the schema which contains this table.
	 * 
	 * @return	DB name of its schema.
	 */
	public String getDBSchemaName();

	/**
	 * Gets the ADQL name of the catalog which contains this table.
	 * 
	 * @return	ADQL name of its catalog.
	 */
	public String getADQLCatalogName();

	/**
	 * Gets the DB name of the catalog which contains this table.
	 * 
	 * @return	DB name of its catalog.
	 */
	public String getDBCatalogName();

	/**
	 * Gets the definition of the specified column if it exists in this table.
	 * 
	 * @param colName		Name of the column <i>(may be the ADQL or DB name depending of the second parameter)</i>.
	 * @param adqlName		<i>true</i> means the given name is the ADQL name of the column and that the research must be done on the ADQL name of columns,
	 * 						<i>false</i> means the same thing but with the DB name.
	 * 
	 * @return				The corresponding column, or <i>null</i> if the specified column had not been found.
	 */
	public DBColumn getColumn(String colName, boolean adqlName);

	/**
	 * Makes a copy of this instance of {@link DBTable}, with the possibility to change the DB and ADQL names.
	 * 
	 * @param dbName	Its new DB name.
	 * @param adqlName	Its new ADQL name.
	 * 
	 * @return			A modified copy of this {@link DBTable}.
	 */
	public DBTable copy(final String dbName, final String adqlName);
}
