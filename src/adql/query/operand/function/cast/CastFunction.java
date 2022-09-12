package adql.query.operand.function.cast;

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
 * Copyright 2021-2022 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.db.DBType;
import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.UnknownType;
import adql.query.operand.function.ADQLFunction;
import adql.translator.FunctionTranslator;

/**
 * Object representation of the ADQL CAST function.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (09/2022)
 * @since 2.0
 */
public class CastFunction extends ADQLFunction implements UnknownType {

	/** Description of this ADQL Feature. */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_TYPE, "CAST", true, "Convert the given value into the specified datatype.");

	/** Constant name of this function. */
	protected final String FCT_NAME = "CAST";

	/** The value to cast. */
	protected ADQLOperand value;

	/** The type into which the value must be casted. */
	protected TargetType targetType;

	/** How to translate this CAST function.
	 * <p>
	 * 	This is by default NULL. It is useful when the target type is not a
	 * 	standard one.
	 * </p> */
	protected FunctionTranslator customTranslation = null;

	/** Type expected by the parser. */
	private char expectedType = '?';

	public CastFunction(final ADQLOperand value, final TargetType type) {
		this.value = value;
		this.targetType = type;
	}

	public CastFunction(final CastFunction toCopy) throws Exception {
		this.value = (toCopy.value == null ? null : (ADQLOperand)toCopy.value.getCopy());
		this.targetType = toCopy.targetType.getCopy();
		setPosition((toCopy.getPosition() == null) ? null : new TextPosition(toCopy.getPosition()));
	}

	@Override
	public boolean isNumeric() {
		return targetType.isNumeric();
	}

	@Override
	public boolean isString() {
		return targetType.isString();
	}

	@Override
	public boolean isGeometry() {
		return targetType.isGeometry();
	}

	@Override
	public char getExpectedType() {
		return expectedType;
	}

	@Override
	public void setExpectedType(final char c) {
		expectedType = c;
	}

	@Override
	public String getName() {
		return FCT_NAME;
	}

	public ADQLOperand getValue() {
		return value;
	}

	/**
	 * Get the database type actually returned by this function.
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	This function is just a convenient access to
	 * 	{@link #getTargetType()}{@link TargetType#getReturnType() .getReturnType()}.
	 * </i></p>
	 *
	 * @return	Type returned by this function.
	 *
	 * @see TargetType#getReturnType()
	 */
	public DBType getReturnType() {
		return targetType.getReturnType();
	}

	/**
	 * Get the type into which the given value is going to be casted.
	 *
	 * @return	The target type.
	 */
	public final TargetType getTargetType() {
		return targetType;
	}

	/**
	 * Set the type into which the given value must be casted.
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given target type is custom (i.e. not a
	 * 	{@link StandardTargetType}), it may be useful to provide a specific
	 * 	translation. In such case, {@link #setFunctionTranslator(FunctionTranslator)}
	 * 	should be used.
	 * </i></p>
	 *
	 * @param type	The new target type.
	 *
	 * @throws NullPointerException	If the given type is NULL.
	 */
	protected void setTargetType(final TargetType type) throws NullPointerException {
		if (type == null)
			throw new NullPointerException("Missing target type! It is required in a CAST function.");
		targetType = type;
	}

	/**
	 * Get the specialized translator to translate this CAST function.
	 *
	 * <p><i><b>Note:</b>
	 * 	It is generally useful to give a such translator when the target type is
	 * 	not a standard one.
	 * </i></p>
	 *
	 * @return	The function translator to use,
	 *        	or NULL for the default {@link CastFunction} translation.
	 */
	public final FunctionTranslator getFunctionTranslator() {
		return customTranslation;
	}

	/**
	 * Set a specialized translator to translate this CAST function.
	 *
	 * <p><i><b>Note:</b>
	 * 	It is generally useful to give a such translator when the target type is
	 * 	not a standard one.
	 * </i></p>
	 *
	 * @param customTranslation	The new translator to use,
	 *                         	or NULL for the default translation.
	 */
	public final void setFunctionTranslator(final FunctionTranslator customTranslation) {
		this.customTranslation = customTranslation;
	}

	@Override
	public LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new CastFunction(this);
	}

	@Override
	public int getNbParameters() {
		return 2 + targetType.getNbParameters();
		/*
		 *  [0]  = the value to cast,
		 *  [1]  = the type name,
		 * [...] = the type parameters.
		 */
	}

	@Override
	public ADQLOperand[] getParameters() {
		final ADQLOperand[] params = new ADQLOperand[getNbParameters()];

		// [0] = the value to cast
		params[0] = value;

		// [1] = the type name
		params[1] = new StringConstant(targetType.getName());

		// [...] = the type parameters
		for(int i = 0; i < targetType.getNbParameters(); i++)
			params[i + 2] = targetType.getParameter(i);

		return params;
	}

	@Override
	public ADQLOperand getParameter(final int index) throws ArrayIndexOutOfBoundsException {
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"CAST\" (nb required params = " + getNbParameters() + ")!");

		// [0] = the value to cast
		if (index == 0)
			return value;

		// [1] = the type name
		else if (index == 1)
			return new StringConstant(targetType.getName());

		// [...] = a type parameter
		else
			return targetType.getParameter(index - 2);
	}

	@Override
	public ADQLOperand setParameter(final int index, final ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"CAST\" (nb required params = " + getNbParameters() + ")!");

		if (replacer == null)
			throw new NullPointerException("Impossible to remove any parameter from the \"CAST\" function ! All parameters are required!");

		ADQLOperand replaced = null;
		switch(index) {
			// CASE: [0] = the value to cast
			case 0:
				replaced = value;
				value = replacer;
				setPosition(null);
				break;

			// CASE: [1] = the type name => the entire TargetType has to be replaced (but then, with no parameter if a standard type)
			case 1:
				if (replacer instanceof StringConstant) {
					replaced = getParameter(1);
					try {
						targetType = new StandardTargetType(((StringConstant)replacer).getValue());
					} catch(Exception ex) {
						targetType = new CustomTargetType(((StringConstant)replacer).getValue(), targetType.getParameters());
					}
					setPosition(null);
				} else
					throw new Exception("Impossible to replace a datatype's name by a " + replacer.getClass().getName() + "! A StringConstant with the datatype name was expected.");
				break;

			// CASE: [...] = a type parameter
			default:
				targetType.setParameter(index - 2, replacer);
				setPosition(null);
				break;
		}
		return replaced;
	}

	@Override
	public String toADQL() {
		final StringBuilder adql = new StringBuilder(FCT_NAME);
		adql.append('(').append(value == null ? "NULL" : value.toADQL());
		adql.append(" AS ").append(targetType.toADQL());
		adql.append(')');
		return adql.toString();
	}

}
