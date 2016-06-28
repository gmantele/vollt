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

import adql.db.DBColumn;
import adql.db.DBCommonColumn;
import adql.db.SearchColumnList;
import adql.db.exception.UnresolvedJoinException;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.query.ClauseConstraints;
import adql.query.operand.ADQLColumn;

/**
 * <p>Special implementation of {@link OuterJoin} for MS SQL Server.</p>
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
 * 	Since this special behavior is also valid for {@link InnerJoin}, a special implementation
 * 	of this class has been also created: {@link SQLServer_InnerJoin}. Both must have exactly the
 * 	same behavior when {@link #getDBColumns()} is called. That's why the static function
 * 	{@link InnerJoin#getDBColumns(ADQLJoin)} has been created. It is called by {@link SQLServer_InnerJoin}
 * 	and {@link SQLServer_OuterJoin}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (03/2016)
 * @since 1.4
 * 
 * @see SQLServer_ADQLQueryFactory
 * @see SQLServer_InnerJoin
 */
public class SQLServer_OuterJoin extends OuterJoin {

	/**
	 * Builds a NATURAL OUTER join between the two given "tables".
	 * 
	 * @param left	Left "table".
	 * @param right	Right "table".
	 * @param type	OUTER join type (left, right or full).
	 * 
	 * @see OuterJoin#OuterJoin(FromContent, FromContent, OuterType)
	 */
	public SQLServer_OuterJoin(FromContent left, FromContent right, OuterType type) {
		super(left, right, type);
	}

	/**
	 * Builds an OUTER join between the two given "tables" with the given condition.
	 * 
	 * @param left		Left "table".
	 * @param right		Right "table".
	 * @param type		Outer join type (left, right or full).
	 * @param condition	Join condition.
	 * 
	 * @see OuterJoin#OuterJoin(FromContent, FromContent, OuterType, ClauseConstraints)
	 */
	public SQLServer_OuterJoin(FromContent left, FromContent right, OuterType type, ClauseConstraints condition) {
		super(left, right, type, condition);
	}

	/**
	 * Builds an OUTER join between the two given "tables" with a list of columns to join.
	 * 
	 * @param left			Left "table".
	 * @param right			Right "table".
	 * @param type			Outer join type.
	 * @param lstColumns	List of columns to join.
	 * 
	 * @see OuterJoin#OuterJoin(FromContent, FromContent, OuterType, Collection)
	 */
	public SQLServer_OuterJoin(FromContent left, FromContent right, OuterType type, Collection<ADQLColumn> lstColumns) {
		super(left, right, type, lstColumns);
	}

	/**
	 * Builds a copy of the given OUTER join.
	 * 
	 * @param toCopy		The OUTER join to copy.
	 * 
	 * @throws Exception	If there is an error during the copy.
	 * 
	 * @see OuterJoin#OuterJoin(OuterJoin)
	 */
	public SQLServer_OuterJoin(OuterJoin toCopy) throws Exception {
		super(toCopy);
	}

	@Override
	public SearchColumnList getDBColumns() throws UnresolvedJoinException{
		return SQLServer_InnerJoin.getDBColumns(this);
	}

}
