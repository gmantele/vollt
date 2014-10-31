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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * It represents any SQL function (COUNT, MAX, MIN, AVG, SUM, etc...).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 * 
 * @see SQLFunctionType
 */
public class SQLFunction extends ADQLFunction {

	/** Type of this SQL function. */
	private final SQLFunctionType type;

	/** The only parameter of this function (may be null). */
	private ADQLOperand param = null;

	/** Distinct values of the parameter ? */
	private boolean distinct = false;

	/**
	 * Creates a SQL function with one parameter.
	 * 
	 * @param t							Type of the function.
	 * @param operand					The only parameter of this function.
	 * @throws NullPointerException	 	If the given is <i>null</i> or if the given operand is <i>null</i> EXCEPT when the given type is {@link SQLFunctionType#COUNT_ALL}.
	 */
	public SQLFunction(SQLFunctionType t, ADQLOperand operand) throws NullPointerException{
		this(t, operand, false);
	}

	/**
	 * Creates a SQL function with one parameter.
	 * 
	 * @param t							Type of the function.
	 * @param operand					The only parameter of this function.
	 * @param distinctValues			<i>true</i> if the quantifier DISTINCT must be used, <i>false</i> otherwise (ALL).
	 * @throws NullPointerException	 	If the given is <i>null</i> or if the given operand is <i>null</i> EXCEPT when the given type is {@link SQLFunctionType#COUNT_ALL}.
	 */
	public SQLFunction(SQLFunctionType t, ADQLOperand operand, boolean distinctValues) throws NullPointerException{
		if (t == null)
			throw new NullPointerException("Impossible to build a SQLFunction without its type (COUNT, SUM, AVG, ...) !");
		else
			type = t;

		if (type == SQLFunctionType.COUNT_ALL)
			param = null;
		else if (operand == null)
			throw new NullPointerException("Impossible to build the SQL function \"" + type.name() + "\" without the operand on which it must apply !");
		else
			param = operand;

		distinct = distinctValues;
	}

	/**
	 * Builds a SQL function by copying the given one.
	 * 
	 * @param toCopy		The SQL function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public SQLFunction(SQLFunction toCopy) throws Exception{
		type = toCopy.type;
		param = (ADQLOperand)toCopy.param.getCopy();
		distinct = toCopy.distinct;
	}

	/**
	 * Indicates whether values of the parameter must be distinct or not.
	 * 
	 * @return	<i>true</i> means distinct values, <i>false</i> else.
	 */
	public final boolean isDistinct(){
		return distinct;
	}

	/**
	 * Tells if distinct values of the given parameter must be taken.
	 * 
	 * @param distinctValues	<i>true</i> means distinct values, <i>false</i> else.
	 */
	public void setDistinct(boolean distinctValues){
		distinct = distinctValues;
	}

	/**
	 * Gets the type (COUNT, SUM, AVG, ...) of this function.
	 * 
	 * @return	Its type.
	 */
	public final SQLFunctionType getType(){
		return type;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new SQLFunction(this);
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
		if (param != null)
			return new ADQLOperand[]{param};
		else
			return new ADQLOperand[0];
	}

	@Override
	public int getNbParameters(){
		return (type == SQLFunctionType.COUNT_ALL) ? 0 : 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + type.name() + "\" !");
		else
			return param;
	}

	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + type.name() + "\" !");

		if (replacer == null)
			throw new NullPointerException("Impossible to remove the only required parameter of the function \"" + type.name() + "\" !");

		ADQLOperand replaced = param;
		param = replacer;

		return replaced;
	}

	@Override
	public String toADQL(){
		if (type == SQLFunctionType.COUNT_ALL)
			return "COUNT(" + (distinct ? "DISTINCT " : "") + "*)";
		else
			return getName() + "(" + (distinct ? "DISTINCT " : "") + param.toADQL() + ")";
	}

}