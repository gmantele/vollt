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
 * Copyright 2011 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.Collection;
import adql.query.ADQLObject;
import adql.query.ClauseConstraints;
import adql.query.operand.ADQLColumn;

/**
 * <p>Inner join.</p>
 * 
 * <p>Possible syntaxes:</p>
 * <ul>
 * 	<li>T1 (LEFT|RIGHT|FULL) OUTER JOIN T2 ON &lt;condition&gt;</li>
 * 	<li>T1 (LEFT|RIGHT|FULL) OUTER JOIN T2 USING(&lt;columns list&gt;)</li>
 * 	<li>T1 NATURAL (LEFT|RIGHT|FULL) OUTER JOIN T2</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 */
public class OuterJoin extends ADQLJoin {

	/**
	 * All OUTER JOIN type: left, right and full.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 08/2011
	 */
	public static enum OuterType{
		LEFT, RIGHT, FULL;
	}

	/** Type of this OUTER join. */
	private OuterType type = OuterType.LEFT;

	/**
	 * Builds a NATURAL OUTER join between the two given "tables".
	 * 
	 * @param left	Left "table".
	 * @param right	Right "table".
	 * @param type	OUTER join type (left, right or full).
	 * 
	 * @see ADQLJoin#ADQLJoin(FromContent, FromContent)
	 * @see #setNatural(boolean)
	 * @see #setType(OuterType)
	 */
	public OuterJoin(FromContent left, FromContent right, OuterType type){
		super(left, right);
		setNatural(true);
		setType(type);
	}

	/**
	 * Builds an OUTER join between the two given "tables" with the given condition.
	 * 
	 * @param left		Left "table".
	 * @param right		Right "table".
	 * @param type		Outer join type (left, right or full).
	 * @param condition	Join condition.
	 * 
	 * @see ADQLJoin#ADQLJoin(FromContent, FromContent)
	 * @see #setJoinCondition(ClauseConstraints)
	 * @see #setType(OuterType)
	 */
	public OuterJoin(FromContent left, FromContent right, OuterType type, ClauseConstraints condition){
		super(left, right);
		setJoinCondition(condition);
		setType(type);
	}

	/**
	 * Builds an OUTER join between the two given "tables" with a list of columns to join.
	 * 
	 * @param left			Left "table".
	 * @param right			Right "table".
	 * @param type			Outer join type.
	 * @param lstColumns	List of columns to join.
	 * 
	 * @see ADQLJoin#ADQLJoin(FromContent, FromContent)
	 * @see #setJoinedColumns(Collection)
	 * @see #setType(OuterType)
	 */
	public OuterJoin(FromContent left, FromContent right, OuterType type, Collection<ADQLColumn> lstColumns){
		super(left, right);
		setJoinedColumns(new ArrayList<ADQLColumn>(lstColumns));
		setType(type);
	}

	/**
	 * Builds a copy of the given OUTER join.
	 * 
	 * @param toCopy		The OUTER join to copy.
	 * 
	 * @throws Exception	If there is an error during the copy.
	 * 
	 * @see ADQLJoin#ADQLJoin(ADQLJoin)
	 * @see #setType(OuterType)
	 */
	public OuterJoin(OuterJoin toCopy) throws Exception{
		super(toCopy);
		setType(toCopy.type);
	}

	@Override
	public String getJoinType(){
		return type.toString() + " OUTER JOIN";
	}

	/**
	 * Gets the OUTER join type (left, right or full).
	 * 
	 * @return	Its OUTER join type.
	 */
	public final OuterType getType(){
		return type;
	}

	/**
	 * Sets the OUTER join type (left, right or full).
	 * 
	 * @param type	Its new OUTER join type.
	 */
	public void setType(OuterType type){
		if (type != null)
			this.type = type;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new OuterJoin(this);
	}

}
