package adql.query.operand.function.geometry;

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

import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * <p>It represents the REGION function the ADQL language.</p>
 * 
 * <p>This function provides a generic way of expressing a region represented by a single string input parameter.
 * The format of the string MUST be specified by a service that accepts ADQL by referring to a standard format.
 * Currently STC/s is the only standardized string representation a service can declare.</p>
 * 
 * <p><i><u>Example:</u><br />
 * REGION('Convex ... Position ... Error ... Size')<br />
 * In this example the function embeds a string serialization of an STC region within parenthesis.</i></p>
 * 
 * <p><b><u>Warning:</u><br />
 * Inappropriate geometries for this construct SHOULD throw an error message, to be defined by the service making use of ADQL.</b></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public class RegionFunction extends GeometryFunction {

	/** The only parameter of this function. */
	protected ADQLOperand parameter;

	/**
	 * Builds a REGION function.
	 * 
	 * @param param				The parameter (a string or a column reference or a concatenation or a user function).
	 * @throws ParseException	If the given parameter is <i>null</i>.
	 */
	public RegionFunction(ADQLOperand param) throws NullPointerException, Exception{
		super();
		if (param == null)
			throw new NullPointerException("The ADQL function REGION must have exactly one parameter !");
		else if (!param.isString())
			throw new Exception("The only required parameter of a REGION function must be a string literal !");
		parameter = param;
	}

	/**
	 * Builds a REGION function by copying the given one.
	 * 
	 * @param toCopy		The REGION function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public RegionFunction(RegionFunction toCopy) throws Exception{
		super();
		parameter = (ADQLOperand)(toCopy.parameter.getCopy());
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new RegionFunction(this);
	}

	@Override
	public String getName(){
		return "REGION";
	}

	@Override
	public boolean isNumeric(){
		return false;
	}

	@Override
	public boolean isString(){
		return false;
	}

	@Override
	public boolean isGeometry(){
		return true;
	}

	@Override
	public ADQLOperand[] getParameters(){
		return new ADQLOperand[]{parameter};
	}

	@Override
	public int getNbParameters(){
		return 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index == 0)
			return parameter;
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
	}

	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (index == 0){
			if (replacer == null)
				throw new NullPointerException("Impossible to remove the only required parameter of a " + getName() + " function !");
			else if (replacer instanceof ADQLOperand){
				ADQLOperand replaced = parameter;
				parameter = replacer;
				return replaced;
			}else
				throw new Exception("Impossible to replace an ADQLOperand by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
		}else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
	}

}
