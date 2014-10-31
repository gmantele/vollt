package adql.query.operand.function;

import adql.query.operand.UnknownType;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

/**
 * Function defined by the user (i.e. PSQL functions).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 * 
 * @see DefaultUDF
 */
public abstract class UserDefinedFunction extends ADQLFunction implements UnknownType {

	/** Type expected by the parser.
	 * @since 1.3 */
	private char expectedType = '?';

	@Override
	public char getExpectedType(){
		return expectedType;
	}

	@Override
	public void setExpectedType(final char c){
		expectedType = c;
	}

}
