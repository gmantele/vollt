package adql.query.operand;

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

import adql.query.ADQLList;
import adql.query.ADQLObject;

/**
 * Represents a concatenation in ADQL (ex: <i>"_s_ra" || ':' || "_s_dec"</i>).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public final class Concatenation extends ADQLList<ADQLOperand> implements ADQLOperand {

	/**
	 * Builds an empty concatenation.
	 * To add operands, use the "add" functions.
	 */
	public Concatenation(){
		super((String)null);
	}

	/**
	 * Builds a copy of the given {@link Concatenation}.
	 * 
	 * @param toCopy		The {@link Concatenation} to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public Concatenation(Concatenation toCopy) throws Exception{
		super(toCopy);
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new Concatenation(this);
	}

	@Override
	public String[] getPossibleSeparators(){
		return new String[]{"||"};
	}

	@Override
	public String getSeparator(int index) throws ArrayIndexOutOfBoundsException{
		if (index <= 0 || index > size())
			throw new ArrayIndexOutOfBoundsException("Impossible to get the concatenation operator between the item " + (index - 1) + " and " + index + " !");
		return "||";
	}

	@Override
	public final boolean isNumeric(){
		return false;
	}

	@Override
	public final boolean isString(){
		return true;
	}

	@Override
	public final boolean isGeometry(){
		return false;
	}

}