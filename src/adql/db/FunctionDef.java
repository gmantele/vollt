package adql.db;

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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adql.db.DBType.DBDatatype;
import adql.parser.ParseException;
import adql.query.operand.function.ADQLFunction;

/**
 * <p>Definition of any function that could be used in ADQL queries.</p>
 * 
 * <p>
 * 	A such definition can be built manually thanks to the different constructors of this class,
 * 	or by parsing a string function definition form using the static function {@link #parse(String)}.
 * </p>
 * 
 * <p>
 * 	The syntax of the expression expected by {@link #parse(String)} is the same as the one used to build
 * 	the string returned by {@link #toString()}. Here is this syntax:
 * </p>
 * <pre>{fctName}([{param1Name} {param1Type}, ...])[ -> {returnType}]</pre>
 * 
 * <p>
 * 	A description of this function may be set thanks to the public class attribute {@link #description}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.3 (10/2014)
 * 
 * @since 1.3
 */
public class FunctionDef implements Comparable<FunctionDef> {
	/** Regular expression for what should be a function or parameter name - a regular identifier. */
	protected final static String regularIdentifierRegExp = "[a-zA-Z]+[0-9a-zA-Z_]*";
	/** Rough regular expression for a function return type or a parameter type.
	 * The exact type is not checked here ; just the type name syntax is tested, not its value.
	 * This regular expression allows a type to have exactly one parameter (which is generally the length of a character or binary string. */
	protected final static String typeRegExp = "([a-zA-Z]+[0-9a-zA-Z]*)(\\(\\s*([0-9]+)\\s*\\))?";
	/** Rough regular expression for a function parameters' list. */
	protected final static String fctParamsRegExp = "\\s*[^,]+\\s*(,\\s*[^,]+\\s*)*";
	/** Rough regular expression for a function parameter: a name (see {@link #regularIdentifierRegExp}) and a type (see {@link #typeRegExp}). */
	protected final static String fctParamRegExp = "\\s*(" + regularIdentifierRegExp + ")\\s+" + typeRegExp + "\\s*";
	/** Rough regular expression for a whole function definition. */
	protected final static String fctDefRegExp = "\\s*(" + regularIdentifierRegExp + ")\\s*\\(([a-zA-Z0-9,() \r\n\t]*)\\)(\\s*->\\s*(" + typeRegExp + "))?\\s*";

	/** Pattern of a function definition. This object has been compiled with {@link #fctDefRegExp}. */
	protected final static Pattern fctPattern = Pattern.compile(fctDefRegExp);
	/** Pattern of a single parameter definition. This object has been compiled with {@link #fctParamRegExp}. */
	protected final static Pattern paramPattern = Pattern.compile(fctParamRegExp);

	/** Name of the function. */
	public final String name;

	/** Description of this function. */
	public String description = null;

	/** Type of the result returned by this function. */
	public final DBType returnType;
	/** Indicate whether the return type is a string. */
	protected final boolean isString;
	/** Indicate whether the return type is a numeric. */
	protected final boolean isNumeric;
	/** Indicate whether the return type is a geometry. */
	protected final boolean isGeometry;

	/** Total number of parameters. */
	public final int nbParams;
	/** List of all the parameters of this function. */
	protected final FunctionParam[] params;

	/** <p>String representation of this function.</p>
	 * <p>The syntax of this representation is the following <i>(items between brackets are optional)</i>:</p>
	 * <pre>{fctName}([{param1Name} {param1Type}, ...])[ -> {returnType}]</pre> */
	private final String serializedForm;

	/** <p>String representation of this function dedicated to comparison with any function signature.</p>
	 * <p>This form is different from the serialized form on the following points:</p>
	 * <ul>
	 * 	<li>the function name is always in lower case.</li>
	 * 	<li>each parameter is represented by a string of 3 characters, one for each kind of type (in the order): numeric, string, geometry.
	 * 	    Each character is either a 0 or 1, so that indicating whether the parameter is of that kind of type.</li>
	 * 	<li>no return type.</li>
	 * </ul>
	 * <p>So the syntax of this form is the following <i>(items between brackets are optional ; xxx is a string of 3 characters, each being either 0 or 1)</i>:</p>
	 * <pre>{fctName}([xxx, ...])</pre> */
	private final String compareForm;

	/**
	 * <p>Definition of a function parameter.</p>
	 * 
	 * <p>This definition is composed of two items: the name and the type of the parameter.</p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static final class FunctionParam {
		/** Parameter name. <i>Ensured not null</i> */
		public final String name;
		/** Parameter type. <i>Ensured not null</i> */
		public final DBType type;

		/**
		 * Create a function parameter.
		 * 
		 * @param paramName	Name of the parameter to create. <i>MUST NOT be NULL</i>
		 * @param paramType	Type of the parameter to create. <i>MUST NOT be NULL</i>
		 */
		public FunctionParam(final String paramName, final DBType paramType){
			if (paramName == null)
				throw new NullPointerException("Missing name! The function parameter can not be created.");
			if (paramType == null)
				throw new NullPointerException("Missing type! The function parameter can not be created.");
			this.name = paramName;
			this.type = paramType;
		}
	}

	/**
	 * <p>Create a function definition.</p>
	 * 
	 * <p>The created function will have <b>no return type</b> and <b>no parameter</b>.</p> 
	 * 
	 * @param fctName	Name of the function.
	 */
	public FunctionDef(final String fctName){
		this(fctName, null, null);
	}

	/**
	 * <p>Create a function definition.</p>
	 * 
	 * <p>The created function will have a return type (if the provided one is not null) and <b>no parameter</b>.</p> 
	 * 
	 * @param fctName		Name of the function.
	 * @param returnType	Return type of the function. <i>If NULL, this function will have no return type</i>
	 */
	public FunctionDef(final String fctName, final DBType returnType){
		this(fctName, returnType, null);
	}

	/**
	 * <p>Create a function definition.</p>
	 * 
	 * <p>The created function will have <b>no return type</b> and some parameters (except if the given array is NULL or empty).</p> 
	 * 
	 * @param fctName		Name of the function.
	 * @param params		Parameters of this function. <i>If NULL or empty, this function will have no parameter.</i>
	 */
	public FunctionDef(final String fctName, final FunctionParam[] params){
		this(fctName, null, params);
	}

	public FunctionDef(final String fctName, final DBType returnType, final FunctionParam[] params){
		// Set the name:
		if (fctName == null)
			throw new NullPointerException("Missing name! Can not create this function definition.");
		this.name = fctName;

		// Set the parameters:
		this.params = (params == null || params.length == 0) ? null : params;
		this.nbParams = (params == null) ? 0 : params.length;

		// Set the return type;
		this.returnType = returnType;
		if (returnType != null){
			isNumeric = returnType.isNumeric();
			isString = returnType.isString();
			isGeometry = returnType.isGeometry();
		}else
			isNumeric = isString = isGeometry = false;

		// Serialize in Strings (serializedForm and compareForm) this function definition:
		StringBuffer bufSer = new StringBuffer(name), bufCmp = new StringBuffer(name.toLowerCase());
		bufSer.append('(');
		for(int i = 0; i < nbParams; i++){
			bufSer.append(params[i].name).append(' ').append(params[i].type);
			bufCmp.append(params[i].type.isNumeric() ? '1' : '0').append(params[i].type.isString() ? '1' : '0').append(params[i].type.isGeometry() ? '1' : '0');
			if (i + 1 < nbParams)
				bufSer.append(", ");
		}
		bufSer.append(')');
		if (returnType != null)
			bufSer.append(" -> ").append(returnType);
		serializedForm = bufSer.toString();
		compareForm = bufCmp.toString();
	}

	/**
	 * Tell whether this function returns a numeric.
	 * 
	 * @return	<i>true</i> if this function returns a numeric, <i>false</i> otherwise.
	 */
	public final boolean isNumeric(){
		return isNumeric;
	}

	/**
	 * Tell whether this function returns a string.
	 * 
	 * @return	<i>true</i> if this function returns a string, <i>false</i> otherwise.
	 */
	public final boolean isString(){
		return isString;
	}

	/**
	 * Tell whether this function returns a geometry.
	 * 
	 * @return	<i>true</i> if this function returns a geometry, <i>false</i> otherwise.
	 */
	public final boolean isGeometry(){
		return isGeometry;
	}

	/**
	 * Get the number of parameters required by this function.
	 * 
	 * @return	Number of required parameters.
	 */
	public final int getNbParams(){
		return nbParams;
	}

	/**
	 * Get the definition of the indParam-th parameter of this function.
	 * 
	 * @param indParam	Index of the parameter whose the definition must be returned.
	 * 
	 * @return	Definition of the specified parameter.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If the given index is negative or bigger than the number of parameters.
	 */
	public final FunctionParam getParam(final int indParam) throws ArrayIndexOutOfBoundsException{
		if (indParam < 0 || indParam >= nbParams)
			throw new ArrayIndexOutOfBoundsException(indParam);
		else
			return params[indParam];
	}

	/**
	 * <p>Let parsing the serialized form of a function definition.</p>
	 * 
	 * <p>The expected syntax is <i>(items between brackets are optional)</i>:</p>
	 * <pre>{fctName}([{param1Name} {param1Type}, ...])[ -> {returnType}]</pre>
	 * 
	 * <p>
	 * 	Allowed parameter types and return types should be one the types listed by the UPLOAD section of the TAP recommendation document.
	 * 	These types are listed in the enumeration object {@link DBType}.
	 * 	However, other types should be accepted like the common database types...but it should be better to not rely on that
	 * 	since the conversion of those types to TAP types should not be exactly what is expected.
	 * </p>
	 * 
	 * @param strDefinition	Serialized function definition to parse.
	 * 
	 * @return	The object representation of the given string definition.
	 * 
	 * @throws ParseException	If the given string has a wrong syntax or uses unknown types.
	 */
	public static FunctionDef parse(final String strDefinition) throws ParseException{
		if (strDefinition == null)
			throw new NullPointerException("Missing string definition to build a FunctionDef!");

		// Check the global syntax of the function definition:
		Matcher m = fctPattern.matcher(strDefinition);
		if (m.matches()){

			// Get the function name:
			String fctName = m.group(1);

			// Parse and get the return type:
			DBType returnType = null;
			if (m.group(3) != null){
				returnType = parseType(m.group(5), (m.group(7) == null) ? DBType.NO_LENGTH : Integer.parseInt(m.group(7)));
				if (returnType == null)
					throw new ParseException("Unknown return type: \"" + m.group(4).trim() + "\"!");
			}

			// Get the parameters, if any:
			String paramsList = m.group(2);
			FunctionParam[] params = null;
			if (paramsList != null && paramsList.trim().length() > 0){

				// Check the syntax of the parameters' list:
				if (!paramsList.matches(fctParamsRegExp))
					throw new ParseException("Wrong parameters syntax! Expected syntax: \"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"()\", \"(param INTEGER)\", \"(param1 INTEGER, param2 DOUBLE)\"");

				// Split all the parameter definitions:
				String[] paramsSplit = paramsList.split(",");
				params = new FunctionParam[paramsSplit.length];
				DBType paramType;

				// For each parameter definition...
				for(int i = 0; i < params.length; i++){
					m = paramPattern.matcher(paramsSplit[i]);
					if (m.matches()){

						// ...parse and get the parameter type:
						paramType = parseType(m.group(2), (m.group(4) == null) ? DBType.NO_LENGTH : Integer.parseInt(m.group(4)));

						// ...build the parameter definition object:
						if (paramType == null)
							throw new ParseException("Unknown type for the parameter \"" + m.group(1) + "\": \"" + m.group(2) + ((m.group(3) == null) ? "" : m.group(3)) + "\"!");
						else
							params[i] = new FunctionParam(m.group(1), paramType);
					}else
						// note: should never happen because we have already check the syntax of the whole parameters list before parsing each individual parameter.
						throw new ParseException("Wrong syntax for the " + (i + 1) + "-th parameter: \"" + paramsSplit[i].trim() + "\"! Expected syntax: \"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"()\", \"(param INTEGER)\", \"(param1 INTEGER, param2 DOUBLE)\"");
				}
			}

			// Build the function definition object:
			return new FunctionDef(fctName, returnType, params);
		}else
			throw new ParseException("Wrong function definition syntax! Expected syntax: \"<regular_identifier>(<parameters>?) <return_type>?\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <return_type>=\" -> <type_name>\", <parameters>=\"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"foo()\", \"foo() -> VARCHAR\", \"foo(param INTEGER)\", \"foo(param1 INTEGER, param2 DOUBLE) -> DOUBLE\"");
	}

	/**
	 * Parse the given string representation of a datatype.
	 * 
	 * @param datatype	String representation of a datatype.
	 *                	<i>Note: This string must not contain the length parameter or any other parameter.
	 *                	These latter should have been separated from the datatype before calling this function.</i>
	 * @param length	Length of this datatype.
	 *              	<i>Note: This length will be used only for binary (BINARY and VARBINARY)
	 *              	and character (CHAR and VARCHAR) types.</i> 
	 * 
	 * @return	The object representation of the specified datatype.
	 */
	private static DBType parseType(String datatype, int length){
		if (datatype == null)
			return null;

		try{
			// Try to find a corresponding DBType item:
			DBDatatype dbDatatype = DBDatatype.valueOf(datatype.toUpperCase());

			// If there's a match, build the type object representation:
			length = (length <= 0) ? DBType.NO_LENGTH : length;
			switch(dbDatatype){
				case CHAR:
				case VARCHAR:
				case BINARY:
				case VARBINARY:
					return new DBType(dbDatatype, length);
				default:
					return new DBType(dbDatatype);
			}
		}catch(IllegalArgumentException iae){
			// If there's no corresponding DBType item, try to find a match among the most used DB types:
			datatype = datatype.toLowerCase();
			if (datatype.equals("bool") || datatype.equals("boolean") || datatype.equals("short"))
				return new DBType(DBDatatype.SMALLINT);
			else if (datatype.equals("int2"))
				return new DBType(DBDatatype.SMALLINT);
			else if (datatype.equals("int") || datatype.equals("int4"))
				return new DBType(DBDatatype.INTEGER);
			else if (datatype.equals("long") || datatype.equals("number") || datatype.equals("bigint") || datatype.equals("int8"))
				return new DBType(DBDatatype.BIGINT);
			else if (datatype.equals("float") || datatype.equals("float4"))
				return new DBType(DBDatatype.REAL);
			else if (datatype.equals("numeric") || datatype.equals("float8"))
				return new DBType(DBDatatype.DOUBLE);
			else if (datatype.equals("byte") || datatype.equals("raw"))
				return new DBType(DBDatatype.BINARY, length);
			else if (datatype.equals("unsignedByte"))
				return new DBType(DBDatatype.VARBINARY, length);
			else if (datatype.equals("character"))
				return new DBType(DBDatatype.CHAR, length);
			else if (datatype.equals("string") || datatype.equals("varchar2"))
				return new DBType(DBDatatype.VARCHAR, length);
			else if (datatype.equals("bytea"))
				return new DBType(DBDatatype.BLOB);
			else if (datatype.equals("text"))
				return new DBType(DBDatatype.CLOB);
			else if (datatype.equals("date") || datatype.equals("time"))
				return new DBType(DBDatatype.TIMESTAMP);
			else if (datatype.equals("position"))
				return new DBType(DBDatatype.POINT);
			else if (datatype.equals("polygon") || datatype.equals("box") || datatype.equals("circle"))
				return new DBType(DBDatatype.REGION);
			else
				return null;
		}
	}

	@Override
	public String toString(){
		return serializedForm;
	}

	@Override
	public int compareTo(final FunctionDef def){
		return compareForm.compareTo(def.compareForm);
	}

	/**
	 * <p>Compare this function definition with the given ADQL function item.</p>
	 * 
	 * <p>
	 * 	The comparison is done only on the function name and on rough type of the parameters.
	 * 	"Rough type" means here that just the kind of type is tested: numeric, string or geometry.
	 * 	Anyway, the return type is never tested by this function, since such information is usually
	 * 	not part of a function signature.
	 * </p>
	 * 
	 * <p>The notion of "greater" and "less" are defined here according to the three following test steps:</p>
	 * <ol>
	 * 	<li><b>Name test:</b> if the name of both function are equals, next steps are evaluated, otherwise the standard string comparison (case insensitive) result is returned.</li>
	 * 	<li><b>Parameters test:</b> parameters are compared individually. Each time parameters (at the same position in both functions) are equals the next parameter can be tested,
	 * 	                            and so on until two parameters are different or the end of the parameters' list is reached.
	 * 	                            Just the kind of type is used for parameter comparison. Each kind of type is tested in the following order: numeric, string and geometry.
	 * 	                            When a kind of type is not equal for both parameters, the function exits with the appropriate value
	 * 	                            (1 if the parameter of this function definition is of the kind of type, -1 otherwise).</li>
	 * 	<li><b>Number of parameters test:</b> in the case where this function definition has N parameters and the given ADQL function has M parameters,
	 * 	                                      and that the L (= min(N,M)) first parameters have the same type in both functions, the value returns by this function
	 * 	                                      will be N-M. Thus, if this function definition has more parameters than the given function, a positive value will be
	 * 	                                      returned. Otherwise a negative value will be returned, or 0 if the number of parameters is the same.</li>
	 * </ol>
	 * 
	 * @param fct	ADQL function item to compare with this function definition.
	 * 
	 * @return	A positive value if this function definition is "greater" than the given {@link ADQLFunction},
	 * 			0 if they are perfectly matching,
	 * 			or a negative value if this function definition is "less" than the given {@link ADQLFunction}.
	 */
	public int compareTo(final ADQLFunction fct){
		if (fct == null)
			throw new NullPointerException("Missing ADQL function with which comparing this function definition!");

		// Names comparison:
		int comp = name.compareToIgnoreCase(fct.getName());

		// If equals, compare the parameters' type:
		if (comp == 0){
			for(int i = 0; comp == 0 && i < nbParams && i < fct.getNbParameters(); i++){
				if (params[i].type.isNumeric() == fct.getParameter(i).isNumeric()){
					if (params[i].type.isString() == fct.getParameter(i).isString()){
						if (params[i].type.isGeometry() == fct.getParameter(i).isGeometry())
							comp = 0;
						else
							comp = params[i].type.isGeometry() ? 1 : -1;
					}else
						comp = params[i].type.isString() ? 1 : -1;
				}else
					comp = params[i].type.isNumeric() ? 1 : -1;
			}

			// If the first min(N,M) parameters are of the same type, do the last comparison on the number of parameters:
			if (comp == 0 && nbParams != fct.getNbParameters())
				comp = nbParams - fct.getNbParameters();
		}

		return comp;
	}
}
