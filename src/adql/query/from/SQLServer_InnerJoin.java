package adql.query.from;

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
 * Copyright 2016 - Astronomisches Rechen Institut (ARI)
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import adql.db.DBColumn;
import adql.db.DBCommonColumn;
import adql.db.SearchColumnList;
import adql.db.exception.UnresolvedJoinException;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.query.ClauseConstraints;
import adql.query.IdentifierField;
import adql.query.operand.ADQLColumn;

/**
 * <p>Special implementation of {@link InnerJoin} for MS SQL Server.</p>
 * 
 * <p><b>Important:</b>
 * 	Instances of this class are created only by {@link SQLServer_ADQLQueryFactory}.
 * </p>
 * 
 * <p>
 * 	This implementation just changes the behavior the {@link #getDBColumns()}.
 * 	In MS SQL Server, there is no keyword NATURAL and USING. That's why the {@link DBColumn}s
 * 	returned by {@link DBColumn} can not contain any {@link DBCommonColumn}. Instead,
 * 	the {@link DBColumn} of the first joined table (i.e. the left one) is returned.
 * </p>
 * 
 * <p>
 * 	Since this special behavior is also valid for {@link OuterJoin}, a special implementation
 * 	of this class has been also created: {@link SQLServer_OuterJoin}. Both must have exactly the
 * 	same behavior when {@link #getDBColumns()} is called. That's why the static function
 * 	{@link #getDBColumns(ADQLJoin)} has been created. It is called by {@link SQLServer_InnerJoin}
 * 	and {@link SQLServer_OuterJoin}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (03/2016)
 * @since 1.4
 * 
 * @see SQLServer_ADQLQueryFactory
 */
public class SQLServer_InnerJoin extends InnerJoin {

	/**
	 * Builds a NATURAL INNER JOIN between the two given "tables".
	 * 
	 * @param left	Left "table".
	 * @param right	Right "table".
	 * 
	 * @see InnerJoin#InnerJoin(FromContent, FromContent)
	 */
	public SQLServer_InnerJoin(FromContent left, FromContent right) {
		super(left, right);
	}

	/**
	 * Builds an INNER JOIN between the two given "tables" with the given condition.
	 * 
	 * @param left		Left "table".
	 * @param right		Right "table".
	 * @param condition	Join condition.
	 * 
	 * @see InnerJoin#InnerJoin(FromContent, FromContent, ClauseConstraints)
	 */
	public SQLServer_InnerJoin(FromContent left, FromContent right, ClauseConstraints condition) {
		super(left, right, condition);
	}

	/**
	 * Builds an INNER JOIN between the two given "tables" with the given condition.
	 * 
	 * @param left		Left "table".
	 * @param right		Right "table".
	 * @param condition	Join condition.
	 * 
	 * @see InnerJoin#InnerJoin(FromContent, FromContent, Collection)
	 */
	public SQLServer_InnerJoin(FromContent left, FromContent right, Collection<ADQLColumn> lstColumns) {
		super(left, right, lstColumns);
	}

	/**
	 * Builds a copy of the given INNER join.
	 * 
	 * @param toCopy		The INNER join to copy.
	 * 
	 * @throws Exception	If there is an error during the copy.
	 * 
	 * @see InnerJoin#InnerJoin(InnerJoin)
	 */
	public SQLServer_InnerJoin(InnerJoin toCopy) throws Exception {
		super(toCopy);
	}

	@Override
	public SearchColumnList getDBColumns() throws UnresolvedJoinException {
		return getDBColumns(this);
	}

	/**
	 * <p>Gets the list of all columns (~ database metadata) available in this FROM part.
	 * Columns implied in a NATURAL join or in a USING list, are not returned as a {@link DBCommonColumn} ; 
	 * actually, just the corresponding {@link DBColumn} of the left table is returned.</p>
	 * 
	 * @return	All the available {@link DBColumn}s.
	 * @throws UnresolvedJoinException If a join is not possible.
	 */
	public static SearchColumnList getDBColumns(final ADQLJoin join) throws UnresolvedJoinException{
		try{
			SearchColumnList list = new SearchColumnList();
			SearchColumnList leftList = join.getLeftTable().getDBColumns();
			SearchColumnList rightList = join.getRightTable().getDBColumns();

			/* 1. Figure out duplicated columns */
			HashMap<String,DBColumn> mapDuplicated = new HashMap<String,DBColumn>();
			// CASE: NATURAL
			if (join.isNatural()){
				// Find duplicated items between the two lists and add one common column in mapDuplicated for each
				DBColumn rightCol;
				for(DBColumn leftCol : leftList){
					// search for at most one column with the same name in the RIGHT list
					// and throw an exception is there are several matches:
					rightCol = findAtMostOneColumn(leftCol.getADQLName(), (byte)0, rightList, false);
					// if there is one...
					if (rightCol != null){
						// ...check there is only one column with this name in the LEFT list,
						// and throw an exception if it is not the case:
						findExactlyOneColumn(leftCol.getADQLName(), (byte)0, leftList, true);
						// ...add the left column:
						mapDuplicated.put(leftCol.getADQLName().toLowerCase(), leftCol);
					}
				}

			}
			// CASE: USING
			else if (join.hasJoinedColumns()){
				// For each columns of usingList, check there is in each list exactly one matching column, and then, add it in mapDuplicated
				DBColumn leftCol;
				ADQLColumn usingCol;
				Iterator<ADQLColumn> itCols = join.getJoinedColumns();
				while(itCols.hasNext()){
					usingCol = itCols.next();
					// search for exactly one column with the same name in the LEFT list
					// and throw an exception if there is none, or if there are several matches:
					leftCol = findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), leftList, true);
					// idem in the RIGHT list:
					findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), rightList, false);
					// add the left column:
					mapDuplicated.put((usingCol.isCaseSensitive(IdentifierField.COLUMN) ? ("\"" + usingCol.getColumnName() + "\"") : usingCol.getColumnName().toLowerCase()), leftCol);
				}

			}
			// CASE: NO DUPLICATION TO FIGURE OUT
			else{
				// Return the union of both lists:
				list.addAll(leftList);
				list.addAll(rightList);
				return list;
			}

			/* 2. Add all columns of the left list except the ones identified as duplications */
			addAllExcept2(leftList, list, mapDuplicated);

			/* 3. Add all columns of the right list except the ones identified as duplications */
			addAllExcept2(rightList, list, mapDuplicated);

			/* 4. Add all common columns of mapDuplicated */
			list.addAll(0, mapDuplicated.values());

			return list;
		}catch(UnresolvedJoinException uje){
			uje.setPosition(join.getPosition());
			throw uje;
		}
	}
	
	public final static void addAllExcept2(final SearchColumnList itemsToAdd, final SearchColumnList target, final Map<String,DBColumn> exception){
		for(DBColumn col : itemsToAdd){
			if (!exception.containsKey(col.getADQLName().toLowerCase()) && !exception.containsKey("\"" + col.getADQLName() + "\""))
				target.add(col);
		}
	}

}
