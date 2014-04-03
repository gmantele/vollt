package adql.query.constraint;

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

import adql.query.ADQLObject;
import adql.query.ClauseConstraints;

/**
 * Represents a parenthesized list of constraints.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 11/2010
 * 
 * @see ClauseConstraints
 */
public class ConstraintsGroup extends ClauseConstraints implements ADQLConstraint  {

	public ConstraintsGroup(){
		super((String)null);
	}

	public ConstraintsGroup(ConstraintsGroup toCopy) throws Exception{
		super(toCopy);
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ConstraintsGroup(this);
	}

	@Override
	public String toADQL() {
		return "("+super.toADQL()+")";
	}



}