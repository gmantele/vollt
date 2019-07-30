package adql.query.operand.function;

import java.util.regex.Matcher;

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
 * Copyright 2012-2020 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.FunctionDef;
import adql.db.FunctionDef.FunctionParam;
import adql.parser.feature.LanguageFeature;
import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ClauseADQL;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;
import adql.translator.ADQLTranslator;
import adql.translator.TranslationException;

/**
 * It represents any function which is not managed by ADQL.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (08/2020)
 */
public final class DefaultUDF extends UserDefinedFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public LanguageFeature languageFeature;

	/** Define/Describe this user defined function.
	 * This object gives the return type and the number and type of all
	 * parameters. */
	protected FunctionDef definition = null;

	/** Its parsed parameters. */
	protected final ADQLList<ADQLOperand> parameters;

	/** Parsed name of this UDF. */
	protected final String functionName;

	/**
	 * Creates a user function.
	 *
	 * @param params	Parameters of the function.
	 */
	public DefaultUDF(final String name, final ADQLOperand[] params) throws NullPointerException {
		functionName = name;
		parameters = new ClauseADQL<ADQLOperand>();
		if (params != null) {
			for(ADQLOperand p : params)
				parameters.add(p);
		}
		generateLanguageFeature();
	}

	/**
	 * Builds a UserFunction by copying the given one.
	 * 
	 * @param toCopy		The UserFunction to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public DefaultUDF(final DefaultUDF toCopy) throws Exception {
		functionName = toCopy.functionName;
		parameters = (ADQLList<ADQLOperand>)(toCopy.parameters.getCopy());
		setPosition((toCopy.getPosition() == null) ? null : new TextPosition(toCopy.getPosition()));
		definition = toCopy.definition;
		languageFeature = toCopy.languageFeature;
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return languageFeature;
	}

	/**
	 * Get the signature/definition/description of this user defined function.
	 * The returned object provides information on the return type and the
	 * number and type of parameters.
	 *
	 * @return	Definition of this function. (MAY be NULL)
	 */
	public final FunctionDef getDefinition() {
		return definition;
	}

	/**
	 * Let set the signature/definition/description of this user defined
	 * function.
	 *
	 * <p><i><b>IMPORTANT:</b>
	 * 	No particular checks are done here except on the function name which
	 * 	MUST be the same (case insensitive) as the name of the given definition.
	 * 	Advanced checks must have been done before calling this setter.
	 * </i></p>
	 * 
	 * @param def	The definition applying to this parsed UDF,
	 *           	or NULL if none has been found.
	 * 
	 * @throws IllegalArgumentException	If the name in the given definition does
	 *                                 	not match the name of this parsed
	 *                                 	function.
	 * 
	 * @since 1.3
	 */
	public final void setDefinition(final FunctionDef def) throws IllegalArgumentException {
		// Ensure the definition is compatible with this ADQL function:
		if (def != null && (def.name == null || !functionName.equalsIgnoreCase(def.name)))
			throw new IllegalArgumentException("The parsed function name (" + functionName + ") does not match to the name of the given UDF definition (" + def.name + ").");

		// Set the new definition (may be NULL):
		this.definition = def;

		// Update the Language Feature of this ADQL function:
		// ...if no definition, generate a default LanguageFeature:
		if (this.definition == null)
			generateLanguageFeature();
		// ...otherwise, use the definition to set the LanguageFeature:
		else
			languageFeature = new LanguageFeature(this.definition);
	}

	/**
	 * Generate and set a default {@link LanguageFeature} for this ADQL
	 * function.
	 *
	 * <p><i><b>Note:</b>
	 * 	Knowing neither the parameters name nor their type, the generated
	 * 	LanguageFeature will just set an unknown type and set a default
	 * 	parameter name (index prefixed with `$`).
	 * </i></p>
	 *
	 * @since 2.0
	 */
	private void generateLanguageFeature() {
		// Create an unknown DBType:
		DBType unknownType = new DBType(DBDatatype.UNKNOWN);
		unknownType.type.setCustomType("type");

		// Create the list of input parameters:
		FunctionParam[] inputParams = new FunctionParam[parameters.size()];
		for(int i = 1; i <= parameters.size(); i++)
			inputParams[i - 1] = new FunctionParam("param" + i, unknownType);

		// Create the Function Definition:
		FunctionDef fctDef = new FunctionDef(functionName, unknownType, inputParams);

		// Finally create the LanguageFeature:
		languageFeature = new LanguageFeature(fctDef);
	}

	@Override
	public final boolean isNumeric() {
		return (definition == null || definition.isNumeric());
	}

	@Override
	public final boolean isString() {
		return (definition == null || definition.isString());
	}

	@Override
	public final boolean isGeometry() {
		return (definition == null || definition.isGeometry());
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		DefaultUDF copy = new DefaultUDF(this);
		copy.setDefinition(definition);
		return copy;
	}

	@Override
	public final String getName() {
		return functionName;
	}

	@Override
	public final ADQLOperand[] getParameters() {
		ADQLOperand[] params = new ADQLOperand[parameters.size()];
		int i = 0;
		for(ADQLOperand op : parameters)
			params[i++] = op;
		return params;
	}

	@Override
	public final int getNbParameters() {
		return parameters.size();
	}

	@Override
	public final ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		return parameters.get(index);
	}

	/**
	 * Function to override if you want to check the parameters of this user
	 * defined function.
	 *
	 * @see adql.query.operand.function.ADQLFunction#setParameter(int, adql.query.operand.ADQLOperand)
	 */
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		ADQLOperand oldParam = parameters.set(index, replacer);
		setPosition(null);
		return oldParam;
	}

	@Override
	public String translate(final ADQLTranslator caller) throws TranslationException{
		// Use the translation pattern if any is specified:
		if (definition != null && definition.getTranslationPattern() != null) {
			StringBuffer sql = new StringBuffer();

			final String escapedDollarRegex = "\\$\\$";
			final String dollarReplacement = Matcher.quoteReplacement("$");
			final String transPattern = definition.getTranslationPattern();

			Matcher m = FunctionDef.argRefPattern.matcher(transPattern);
			int startIndex = 0;
			ADQLOperand arg;

			// Search for all argument references:
			while(m.find()) {
				// append the string before the argument to insert:
				sql.append(transPattern.substring(startIndex, m.start()).replaceAll(escapedDollarRegex, dollarReplacement));
				startIndex = m.end();
				// get the argument:
				arg = getParameter(Integer.parseInt(m.group().substring(1)) - 1);
				// translate and insert the argument:
				sql.append(caller.translate(arg));
			}

			// If no argument found, take the whole string as such
			// (and just replace escaped $):
			if (startIndex == 0)
				sql.append(transPattern.replaceAll(escapedDollarRegex, dollarReplacement));

			// Otherwise, take the whole end of the string as such
			// (and again replace escaped $):
			else if (startIndex < transPattern.length())
				sql.append(transPattern.substring(startIndex).replaceAll(escapedDollarRegex, dollarReplacement));

			return sql.toString();
		}
		// Otherwise, no translation needed (use the ADQL as translation result):
		else {
		StringBuffer sql = new StringBuffer(functionName);
		sql.append('(');
		for(int i = 0; i < parameters.size(); i++){
			if (i > 0)
				sql.append(',').append(' ');
			sql.append(caller.translate(parameters.get(i)));
			}
			sql.append(')');
			return sql.toString();
		}
	}

}
