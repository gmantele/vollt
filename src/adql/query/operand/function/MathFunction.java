package adql.query.operand.function;

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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * It represents any basic mathematical function.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 * 
 * @see MathFunctionType
 */
public class MathFunction extends ADQLFunction {

	/** Type of the mathematical function (which will also be its name). */
	private final MathFunctionType type;

	/** First parameter of this function (may be null). */
	private ADQLOperand param1 = null;

	/** Second parameter of this function (may be null). */
	private ADQLOperand param2 = null;

	/** Number of given parameters.
	 * @since 1.2*/
	private int nbParams;

	/**
	 * Creates a mathematical function without parameter.
	 * 
	 * @param t	The type of the function.
	 * @throws Exception	If the given function parameters are incorrect.
	 * 
	 * @see MathFunction#MathFunction(MathFunctionType, ADQLOperand, ADQLOperand)
	 */
	public MathFunction(MathFunctionType t) throws Exception{
		this(t, null, null);
	}

	/**
	 * Creates a mathematical function with only one parameter.
	 * 
	 * @param t			The type of the function.
	 * @param parameter	Its only parameter.
	 * @throws Exception	If the given function parameters are incorrect.
	 * 
	 * @see MathFunction#MathFunction(MathFunctionType, ADQLOperand, ADQLOperand)
	 */
	public MathFunction(MathFunctionType t, ADQLOperand parameter) throws Exception{
		this(t, parameter, null);
	}

	/**
	 * Creates a mathematical function with two parameters.
	 * @param t				The type of the function.
	 * @param parameter1	Its first parameter.
	 * @param parameter2	Its second parameter.
	 * @throws Exception	If the given function parameters are incorrect.
	 */
	public MathFunction(MathFunctionType t, ADQLOperand parameter1, ADQLOperand parameter2) throws Exception{
		// Set the function type:
		type = t;
		// Only two parameters can be managed inside this class.
		if (type.nbMaxParams() > 2)
			throw new Exception("Impossible for MathFunction object to have " + type.nbMaxParams() + " ! It is limited to 2 parameters !");
		// Compute the number of given parameters:
		nbParams = ((parameter1 != null) ? 1 : 0) + ((parameter2 != null) ? 1 : 0);
		// Check it and throw immediately an exception if incorrect:
		if (nbParams < type.nbMinParams() || nbParams > type.nbMaxParams()){
			if (type.nbMinParams() == type.nbMaxParams())
				throw new Exception("The function " + type.name() + " must have " + ((type.nbMaxParams() == 0) ? "no parameter!" : ("exactly " + type.nbMaxParams() + " parameters!")));
			else{
				switch(type.nbMaxParams()){
					case 0:
						throw new Exception("The function " + type.name() + " must have no parameter !");
					case 1:
						throw new Exception("The function " + type.name() + " must have only one parameter !");
					case 2:
						throw new Exception("The function " + type.name() + " must have two parameters !");
				}
			}
		}
		// Set the function parameters:
		param1 = parameter1;
		param2 = parameter2;
	}

	/**
	 * Builds a mathematical function by copying the given one.
	 * 
	 * @param toCopy		The mathematical function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public MathFunction(MathFunction toCopy) throws Exception{
		type = toCopy.type;
		param1 = (ADQLOperand)toCopy.param1.getCopy();
		param2 = (ADQLOperand)toCopy.param2.getCopy();
	}

	/**
	 * Gets the type of the function (ABS, COS, SIN, ...).
	 * 
	 * @return	Its type.
	 * 
	 * @see MathFunctionType
	 */
	public final MathFunctionType getType(){
		return type;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new MathFunction(this);
	}

	@Override
	public String getName(){
		return type.name();
	}

	@Override
	public final boolean isNumeric(){
		return true;
	}

	@Override
	public final boolean isString(){
		return false;
	}

	@Override
	public final boolean isGeometry(){
		return false;
	}

	@Override
	public ADQLOperand[] getParameters(){
		switch(getNbParameters()){
			case 1:
				return new ADQLOperand[]{param1};
			case 2:
				return new ADQLOperand[]{param1,param2};
			default:
				return new ADQLOperand[0];
		}
	}

	@Override
	public int getNbParameters(){
		return nbParams;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + type.name() + "\" (nb required params = " + type.nbMaxParams() + ") !");

		switch(index){
			case 0:
				return param1;
			case 1:
				return param2;
			default:
				return null;
		}
	}

	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + type.name() + "\" (nb required params = " + type.nbMaxParams() + ") !");
		else if (replacer == null)
			throw new NullPointerException("Impossible to remove any parameter from a mathematical function ! All parameters are required !");
		else{
			ADQLOperand replaced = null;
			switch(index){
				case 0:
					replaced = param1;
					param1 = replacer;
					break;
				case 1:
					replaced = param2;
					param2 = replacer;
					break;
			}
			return replaced;
		}
	}

}
