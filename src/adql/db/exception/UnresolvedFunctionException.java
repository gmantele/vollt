package adql.db.exception;

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
 * Copyright 2014-2015 - Astronomisches Rechen Institut (ARI)
 */

import adql.parser.ParseException;
import adql.query.TextPosition;
import adql.query.operand.function.ADQLFunction;

/**
 * Exception thrown when a function can not be resolved by the library.
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (08/2015)
 * @since 1.3
 */
public class UnresolvedFunctionException extends ParseException {
	private static final long serialVersionUID = 1L;

	/** Function which can not be resolved. */
	protected final ADQLFunction functionInError;

	/**
	 * Build the exception with just a message.
	 * 
	 * @param message	Description of the error.
	 */
	public UnresolvedFunctionException(final String message){
		this(message, (TextPosition)null);
	}

	/**
	 * Build the exception with just a message.
	 * 
	 * @param message	Description of the error.
	 * @param pos		Position of the unresolved function inside the ADQL query.
	 * 
	 * @since 1.4
	 */
	public UnresolvedFunctionException(final String message, final TextPosition pos){
		super(message, pos);
		functionInError = null;
	}

	/**
	 * Build the exception with the unresolved function in parameter.
	 * The position of this function in the ADQL query can be retrieved and used afterwards.
	 * 
	 * @param fct	The unresolved function.
	 */
	public UnresolvedFunctionException(final ADQLFunction fct){
		super("Unresolved function: \"" + fct.toADQL() + "\"! No UDF has been defined or found with the signature: " + getFctSignature(fct) + ".", fct.getPosition());
		functionInError = fct;
	}

	/**
	 * Build the exception with a message but also with the unresolved function in parameter.
	 * The position of this function in the ADQL query can be retrieved and used afterwards.
	 * 
	 * @param message	Description of the error.
	 * @param fct		The unresolved function.
	 */
	public UnresolvedFunctionException(final String message, final ADQLFunction fct){
		super(message, (fct == null) ? null : fct.getPosition());
		functionInError = fct;
	}

	/**
	 * Get the unresolved function at the origin of this exception.
	 * 
	 * @return	The unresolved function. <i>Note: MAY be NULL</i>
	 */
	public final ADQLFunction getFunction(){
		return functionInError;
	}

	/**
	 * <p>Get the signature of the function given in parameter.</p>
	 * 
	 * <p>
	 * 	In this signature, just the name and the type of all the parameters are written.
	 * 	The return type is never part of a function signature.
	 * </p>
	 * 
	 * <p><i>Note 1:
	 * 	A parameter type can be either "NUMERIC", "STRING" or "GEOMETRY". In order to be the most generic has possible,
	 * 	no more precision about a type is returned here. If the parameter is none of these type kinds, "param" suffixed
	 * 	by the parameter index (e.g. "param1") is returned.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	If the given object is NULL, an empty string is returned.
	 * </i></p>
	 * 
	 * @param fct	Function whose the signature must be returned.
	 * 
	 * @return	The corresponding signature.
	 */
	public static String getFctSignature(final ADQLFunction fct){
		if (fct == null)
			return "";

		StringBuffer buf = new StringBuffer(fct.getName().toLowerCase());
		buf.append('(');
		for(int i = 0; i < fct.getNbParameters(); i++){
			if (fct.getParameter(i).isNumeric() && fct.getParameter(i).isString() && fct.getParameter(i).isGeometry())
				buf.append("param").append(i + 1);
			else if (fct.getParameter(i).isNumeric())
				buf.append("NUMERIC");
			else if (fct.getParameter(i).isString())
				buf.append("STRING");
			else if (fct.getParameter(i).isGeometry())
				buf.append("GEOMETRY");
			else
				buf.append("param").append(i + 1);

			if ((i + 1) < fct.getNbParameters())
				buf.append(", ");
		}
		buf.append(')');
		return buf.toString();
	}

}
