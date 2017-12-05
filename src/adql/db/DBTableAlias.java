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
 * Copyright 2017 - Astronomisches Rechen Institut (ARI)
 */

/**
 * This {@link DBTable} wraps another {@link DBTable} with a different ADQL and
 * DB name.
 *
 * <p>
 * 	This wrapper aims to represent in the metadata the aliasing of a table.
 * 	This table should not be part of any schema, in ADQL but also in SQL...it is
 * 	just an alias of an existing table.
 * </p>
 *
 * <p>
 * 	All columns of the origin table are completely copied into this
 * 	{@link DBTable} thanks to {@link DBColumn#copy(String, String, DBTable)},
 * 	with the same ADQL and DB name but a different parent table (this one is
 * 	used of the original one).
 * </p>
 *
 * <p><i>Note:
 * 	The origin table is still available thanks to the function
 * 	{@link #getOriginTable()}.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (11/2017)
 * @since 1.4
 */
public class DBTableAlias extends DefaultDBTable {

	/** Wrapped table. */
	protected final DBTable originTable;

	/**
	 * Wrap the given table under the given ADQL/DB name.
	 *
	 * @param originTable	The table to wrap/alias.
	 * @param tableAlias	The alias name.
	 */
	public DBTableAlias(final DBTable originTable, final String tableAlias){
		super(null, null, tableAlias);

		this.originTable = originTable;

		for(DBColumn col : originTable)
			addColumn(col.copy(col.getDBName(), col.getADQLName(), this));
	}

	/**
	 * Get the aliased/wrapped table.
	 *
	 * @return	The aliased table.
	 */
	public DBTable getOriginTable(){
		return originTable;
	}

}
