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

import adql.db.FunctionDef;
import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ClauseADQL;
import adql.query.operand.ADQLOperand;

/**
 * It represents any function which is not managed by ADQL.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public final class DefaultUDF extends UserDefinedFunction {

	/** Define/Describe this user defined function.
	 * This object gives the return type and the number and type of all parameters. */
	protected FunctionDef definition = null;

	/** Its parsed parameters. */
	protected final ADQLList<ADQLOperand> parameters;

	/** Parsed name of this UDF. */
	protected final String functionName;

	/**
	 * Creates a user function.
	 * @param params	Parameters of the function.
	 */
	public DefaultUDF(final String name, ADQLOperand[] params) throws NullPointerException{
		functionName = name;
		parameters = new ClauseADQL<ADQLOperand>();
		if (params != null){
			for(ADQLOperand p : params)
				parameters.add(p);
		}
	}

	/**
	 * Builds a UserFunction by copying the given one.
	 * 
	 * @param toCopy		The UserFunction to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public DefaultUDF(DefaultUDF toCopy) throws Exception{
		functionName = toCopy.functionName;
		parameters = (ADQLList<ADQLOperand>)(toCopy.parameters.getCopy());
	}

	/**
	 * Get the signature/definition/description of this user defined function.
	 * The returned object provides information on the return type and the number and type of parameters. 
	 * 
	 * @return	Definition of this function. (MAY be NULL)
	 */
	public final FunctionDef getDefinition(){
		return definition;
	}

	/**
	 * <p>Let set the signature/definition/description of this user defined function.</p>
	 * 
	 * <p><i><b>IMPORTANT:</b>
	 * 	No particular checks are done here except on the function name which MUST
	 * 	be the same (case insensitive) as the name of the given definition.
	 * 	Advanced checks must have been done before calling this setter.
	 * </i></p>
	 * 
	 * @param def	The definition applying to this parsed UDF, or NULL if none has been found.
	 * 
	 * @throws IllegalArgumentException	If the name in the given definition does not match the name of this parsed function.
	 */
	public final void setDefinition(final FunctionDef def) throws IllegalArgumentException{
		if (def != null && (def.name == null || !functionName.equalsIgnoreCase(def.name)))
			throw new IllegalArgumentException("The parsed function name (" + functionName + ") does not match to the name of the given UDF definition (" + def.name + ").");

		this.definition = def;
	}

	@Override
	public final boolean isNumeric(){
		return (definition == null || definition.isNumeric());
	}

	@Override
	public final boolean isString(){
		return (definition == null || definition.isString());
	}

	@Override
	public final boolean isGeometry(){
		return (definition == null || definition.isGeometry());
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		DefaultUDF copy = new DefaultUDF(this);
		copy.setDefinition(definition);
		return copy;
	}

	@Override
	public final String getName(){
		return functionName;
	}

	@Override
	public final ADQLOperand[] getParameters(){
		ADQLOperand[] params = new ADQLOperand[parameters.size()];
		int i = 0;
		for(ADQLOperand op : parameters)
			params[i++] = op;
		return params;
	}

	@Override
	public final int getNbParameters(){
		return parameters.size();
	}

	@Override
	public final ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		return parameters.get(index);
	}

	/**
	 * Function to override if you want to check the parameters of this user defined function.
	 * 
	 * @see adql.query.operand.function.ADQLFunction#setParameter(int, adql.query.operand.ADQLOperand)
	 */
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		return parameters.set(index, replacer);
	}

}
