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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
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
 * 	<li>T1 [INNER] JOIN T2 ON &lt;condition&gt;</li>
 * 	<li>T1 [INNER] JOIN T2 USING(&lt;columns list&gt;)</li>
 * 	<li>T1 NATURAL [INNER] JOIN T2</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 */
public class InnerJoin extends ADQLJoin {

	/**
	 * Builds a NATURAL INNER JOIN between the two given "tables".
	 * 
	 * @param left	Left "table".
	 * @param right	Right "table".
	 * 
	 * @see ADQLJoin#ADQLJoin(FromContent, FromContent)
	 * @see #setNatural(boolean)
	 */
	public InnerJoin(FromContent left, FromContent right){
		super(left, right);
		setNatural(true);
	}

	/**
	 * Builds an INNER JOIN between the two given "tables" with the given condition.
	 * 
	 * @param left		Left "table".
	 * @param right		Right "table".
	 * @param condition	Join condition.
	 * 
	 * @see ADQLJoin#ADQLJoin(FromContent, FromContent)
	 * @see #setJoinCondition(ClauseConstraints)
	 */
	public InnerJoin(FromContent left, FromContent right, ClauseConstraints condition){
		super(left, right);
		setJoinCondition(condition);
	}

	/**
	 * Builds an INNER JOIN between the two given "tables" with the list of columns to join.
	 * 
	 * @param left			Left "table".
	 * @param right			Right "table".
	 * @param lstColumns	List of columns to join.
	 * 
	 * @see ADQLJoin#ADQLJoin(FromContent, FromContent)
	 * @see #setJoinedColumns(Collection)
	 */
	public InnerJoin(FromContent left, FromContent right, Collection<ADQLColumn> lstColumns){
		super(left, right);
		setJoinedColumns(new ArrayList<ADQLColumn>(lstColumns));
	}

	/**
	 * Builds a copy of the given INNER join.
	 * 
	 * @param toCopy		The INNER join to copy.
	 * 
	 * @throws Exception	If there is an error during the copy.
	 * 
	 * @see ADQLJoin#ADQLJoin(ADQLJoin)
	 */
	public InnerJoin(InnerJoin toCopy) throws Exception{
		super(toCopy);
	}

	@Override
	public String getJoinType(){
		return "INNER JOIN";
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new InnerJoin(this);
	}

}
