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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ClauseADQL;

import adql.query.operand.ADQLOperand;

/**
 * It represents any function which is not managed by ADQL.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 */
public final class DefaultUDF extends UserDefinedFunction {

	/** Its parameters. */
	protected final ADQLList<ADQLOperand> parameters;

	protected final String functionName;


	/**
	 * Creates a user function.
	 * @param params	Parameters of the function.
	 */
	public DefaultUDF(final String name, ADQLOperand[] params) throws NullPointerException {
		functionName = name;
		parameters = new ClauseADQL<ADQLOperand>();
		if (params != null){
			for(int i=0; i<params.length; i++)
				parameters.set(i, params[i]);
		}
	}

	/**
	 * Builds a UserFunction by copying the given one.
	 * 
	 * @param toCopy		The UserFunction to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public DefaultUDF(DefaultUDF toCopy) throws Exception {
		functionName = toCopy.functionName;
		parameters = (ADQLList<ADQLOperand>)(toCopy.parameters.getCopy());
	}


	public final boolean isNumeric() {
		return true;
	}

	public final boolean isString() {
		return true;
	}

	public ADQLObject getCopy() throws Exception {
		return new DefaultUDF(this);
	}

	public final String getName() {
		return functionName;
	}

	@Override
	public final ADQLOperand[] getParameters() {
		ADQLOperand[] params = new ADQLOperand[parameters.size()];
		int i=0;
		for(ADQLOperand op : parameters)
			params[i++] = op;
		return params;
	}

	@Override
	public final int getNbParameters(){
		return parameters.size();
	}

	@Override
	public final ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		return parameters.get(index);
	}

	/**
	 * Function to override if you want to check the parameters of this user defined function.
	 * 
	 * @see adql.query.operand.function.ADQLFunction#setParameter(int, adql.query.operand.ADQLOperand)
	 */
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		return parameters.set(index, replacer);
	}

}
