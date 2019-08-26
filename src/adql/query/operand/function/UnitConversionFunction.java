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
 * Copyright 2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * It represents the IN_UNIT function of ADQL.
 *
 * <p>This function converts the given value into the given VO-Unit.</p>
 *
 * <p>
 * 	This function should report an error if the specified unit is not valid or
 * 	if the conversion is not possible.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (08/2019)
 * @since 2.0
 */
public class UnitConversionFunction extends ADQLFunction {

	/** Description of this ADQL Feature. */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_UNIT, "IN_UNIT", true, "Convert the given value (1st argument) into the given VO-Unit (2nd argument).");

	/** Constant name of this function. */
	protected final String FCT_NAME = "IN_UNIT";

	/** The value to convert. */
	protected ADQLOperand value;

	/** The VO-Unit into which the value must be converted. */
	protected ADQLOperand targetUnit;

	/**
	 * Create the object representation of the ADQL function IN_UNIT.
	 *
	 * @param value			The value to convert.
	 * @param targetUnit	The VO-Unit into which the value must be converted.
	 *
	 * @throws NullPointerException		If one of the given operands is NULL.
	 * @throws IllegalArgumentException	If the 1st operand is not a numeric,
	 *                                 	or if the 2nd is not a string.
	 */
	public UnitConversionFunction(final ADQLOperand value, final ADQLOperand targetUnit) throws NullPointerException, IllegalArgumentException {
		setValue(value);
		setTargetUnit(targetUnit);
	}

	/**
	 * Get the numeric operand to convert into a different unit.
	 *
	 * @return	The value to convert.
	 */
	public final ADQLOperand getValue() {
		return value;
	}

	/**
	 * Set the numeric operand to convert into a different unit.
	 *
	 * @param value	The value to convert.
	 *
	 * @throws NullPointerException		If the given operand is NULL.
	 * @throws IllegalArgumentException	If the given operand is not a numeric.
	 */
	public final void setValue(final ADQLOperand value) throws NullPointerException, IllegalArgumentException {
		if (value == null)
			throw new NullPointerException("The 1st argument of the ADQL function " + FCT_NAME + " (i.e. the value to convert) must be non-NULL!");

		if (!value.isNumeric())
			throw new IllegalArgumentException("The 1st argument of the ADQL function " + FCT_NAME + " (i.e. the value to convert) must be a numeric!");

		this.value = value;
	}

	/**
	 * Get the VO-Unit into which the value must be converted.
	 *
	 * @return	The target unit.
	 */
	public final ADQLOperand getTargetUnit() {
		return targetUnit;
	}

	/**
	 * Set the VO-Unit into which the value must be converted.
	 *
	 * @param targetUnit	The target unit.
	 *
	 * @throws NullPointerException		If the given operand is NULL.
	 * @throws IllegalArgumentException	If the given operand is not a string.
	 */
	public final void setTargetUnit(final ADQLOperand targetUnit) throws NullPointerException, IllegalArgumentException {
		if (targetUnit == null)
			throw new NullPointerException("The 2nd argument of the ADQL function " + FCT_NAME + " (i.e. target unit) must be non-NULL!");

		if (!targetUnit.isString())
			throw new IllegalArgumentException("The 2nd argument of the ADQL function " + FCT_NAME + " (i.e. target unit) must be of type VARCHAR (i.e. a string)!");

		this.targetUnit = targetUnit;
	}

	@Override
	public final boolean isNumeric() {
		return true;
	}

	@Override
	public final boolean isString() {
		return false;
	}

	@Override
	public final boolean isGeometry() {
		return false;
	}

	@Override
	public String getName() {
		return FCT_NAME;
	}

	@Override
	public LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new UnitConversionFunction((ADQLOperand)value.getCopy(), (ADQLOperand)targetUnit.getCopy());
	}

	@Override
	public int getNbParameters() {
		return 2;
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ value, targetUnit };
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + FCT_NAME + "\" (nb required params = " + getNbParameters() + ")!");

		switch(index) {
			case 0:
				return value;
			case 1:
				return targetUnit;
			default:
				return null;
		}
	}

	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + FCT_NAME + "\" (nb required params = " + getNbParameters() + ")!");
		else if (replacer == null)
			throw new NullPointerException("Impossible to remove any parameter from the " + FCT_NAME + " function! All parameters are required!");
		else {
			ADQLOperand replaced = null;
			switch(index) {
				case 0:
					replaced = value;
					setValue(replacer);
					setPosition(null);
					break;
				case 1:
					replaced = targetUnit;
					setTargetUnit(replacer);
					setPosition(null);
					break;
			}
			return replaced;
		}
	}

}
