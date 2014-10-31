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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Iterator;

import adql.db.exception.UnresolvedJoin;
import adql.query.ADQLQuery;

/**
 * This is a special column which exists only after a NATURAL JOIN or a JOIN ... USING between two tables.
 * It lets unify several columns of the joined tables in a single column.
 * 
 * Thus, the writer of an ADQL query can use the column name without table prefix (since after the join there will be only one)
 * or with a prefix table of the joined tables. The list of all covered tables is stored in this object and can be extended
 * in case of several JOINs.
 * 
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 1.3 (10/2014)
 * @since 1.2
 */
public class DBCommonColumn implements DBColumn {

	protected DBColumn generalColumnDesc;
	protected ArrayList<DBTable> lstCoveredTables = new ArrayList<DBTable>();

	/**
	 * Create a column which merges both of the given columns.
	 * 
	 * This special {@link DBColumn} implementation is not associated with one table,
	 * and can be listed in a {@link DBTable} ONLY IF the latter is the result of a sub-query (see {@link ADQLQuery#getResultingColumns()}).
	 * 
	 * A column resulting from a tables join is common only to the joined tables. That's why a list of all tables covered
	 * by this column is created or update at each merge. It can be accessed thanks to {@link #getCoveredTables()}.
	 * 
	 * Note: In the case one or both of the columns to join are {@link DBCommonColumn}, the list of their covered tables are also merged.
	 * 
	 * @param leftCol	Column of the left join table. May be a {@link DBCommonColumn}.
	 * @param rightCol	Column of the right join table. May be a {@link DBCommonColumn}.
	 * 
	 * @throws UnresolvedJoin	If the type of the two given columns are not roughly (just testing numeric, string or geometry) compatible.
	 */
	public DBCommonColumn(final DBColumn leftCol, final DBColumn rightCol) throws UnresolvedJoin{
		// Test whether type of both columns are compatible:
		if (leftCol.getDatatype() != null && rightCol.getDatatype() != null && !leftCol.getDatatype().isCompatible(rightCol.getDatatype()))
			throw new UnresolvedJoin("JOIN impossible: incompatible column types when trying to join the columns " + leftCol.getADQLName() + " (" + leftCol.getDatatype() + ") and " + rightCol.getADQLName() + " (" + rightCol.getDatatype() + ")!");

		// LEFT COLUMN:
		if (leftCol instanceof DBCommonColumn){
			// set the general column description:
			generalColumnDesc = ((DBCommonColumn)leftCol).generalColumnDesc;

			// add all covered tables of the left common column:
			Iterator<DBTable> it = ((DBCommonColumn)leftCol).getCoveredTables();
			while(it.hasNext())
				addCoveredTable(it.next());
		}else{
			// set the general column description:
			generalColumnDesc = leftCol.copy(leftCol.getDBName(), leftCol.getADQLName(), null);
			// add the table to cover:
			addCoveredTable(leftCol.getTable());
		}

		// RIGHT COLUMN:
		if (rightCol instanceof DBCommonColumn){
			// add all covered tables of the left common column:
			Iterator<DBTable> it = ((DBCommonColumn)rightCol).getCoveredTables();
			while(it.hasNext())
				addCoveredTable(it.next());
		}else{
			// add the table to cover:
			addCoveredTable(rightCol.getTable());
		}
	}

	/**
	 * Constructor by copy.
	 * It returns a copy of this instance of {@link DBCommonColumn}.
	 * 
	 * Note: The list of covered tables is NOT deeply copied!
	 * 
	 * @param toCopy	The {@link DBCommonColumn} to copy.
	 * @param dbName	The new DB name of this {@link DBCommonColumn}.
	 * @param adqlName	The new ADQL name of this {@link DBCommonColumn}.
	 */
	@SuppressWarnings("unchecked")
	public DBCommonColumn(final DBCommonColumn toCopy, final String dbName, final String adqlName){
		generalColumnDesc = toCopy.generalColumnDesc.copy(dbName, adqlName, null);
		lstCoveredTables = (ArrayList<DBTable>)toCopy.lstCoveredTables.clone();
	}

	@Override
	public final String getADQLName(){
		return generalColumnDesc.getADQLName();
	}

	@Override
	public final String getDBName(){
		return generalColumnDesc.getDBName();
	}

	@Override
	public final DBType getDatatype(){
		return generalColumnDesc.getDatatype();
	}

	@Override
	public final DBTable getTable(){
		return null;
	}

	/**
	 * Get an iterator over the list of all tables covered by this common column.
	 * 
	 * @return	Iterator over all covered tables.
	 */
	public final Iterator<DBTable> getCoveredTables(){
		return lstCoveredTables.iterator();
	}

	/**
	 * Add a table that this common column must cover from now.
	 * 
	 * Warning: no unicity check is never done !
	 * 
	 * @param table	Table to add in the covered tables list.
	 */
	protected void addCoveredTable(final DBTable table){
		if (table != null)
			lstCoveredTables.add(table);
	}

	/**
	 * WARNING: This copy function does not make a real copy of this DBCommonColumn !
	 *          It returns a modified copy of the general column description it contains.
	 * 
	 * Note: To make a real copy of this DBCommonColumn use the Constructor by copy {@link #DBCommonColumn(DBCommonColumn, String, String)}.
	 * 
	 * @param dbName	Its new DB name.
	 * @param adqlName	Its new ADQL name.
	 * @param dbTable	Its new DBTable
	 * 
	 * @return			A modified copy of the general column description this common column represents.
	 * 
	 * @see adql.db.DBColumn#copy(java.lang.String, java.lang.String, adql.db.DBTable)
	 */
	@Override
	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable){
		return generalColumnDesc.copy(dbName, adqlName, dbTable);
	}

}
