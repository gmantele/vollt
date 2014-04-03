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
 * Default implementation of {@link DBColumn}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 */
public class DefaultDBColumn implements DBColumn {

	protected String dbName;
	protected DBTable table;

	protected String adqlName = null;

	/**
	 * Builds a default {@link DBColumn} with the given DB name and DB table.
	 * 
	 * @param dbName	Database column name (it will be also used for the ADQL name).
	 * 					<b>Only the column name is expected. Contrary to {@link DefaultDBTable},
	 * 					if a whole column reference is given, no split will be done.</b>
	 * @param table		DB table which contains this column.
	 * 
	 * @see #DefaultDBColumn(String, String, DBTable)
	 */
	public DefaultDBColumn(final String dbName, final DBTable table){
		this(dbName, dbName, table);
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
	 */
	public DefaultDBColumn(final String dbName, final String adqlName, final DBTable table){
		this.dbName = dbName;
		this.adqlName = adqlName;
		this.table = table;
	}

	public final String getADQLName() {
		return adqlName;
	}

	public final void setADQLName(final String adqlName){
		if (adqlName != null)
			this.adqlName = adqlName;
	}

	public final String getDBName() {
		return dbName;
	}

	public final DBTable getTable() {
		return table;
	}

	public final void setTable(final DBTable table){
		this.table = table;
	}

	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable){
		return new DefaultDBColumn(dbName, adqlName, dbTable);
	}

}
