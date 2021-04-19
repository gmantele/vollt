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
 * Copyright 2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;
import adql.query.operand.UnknownType;

public class CastFunction extends ADQLFunction implements UnknownType {

	/** Description of this ADQL Feature. */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_TYPE, "CAST", true, "Convert the given value into the specified datatype.");

	/** Constant name of this function. */
	protected final String FCT_NAME = "CAST";

	protected ADQLOperand value;
	protected DatatypeParam datatype;

	/** Type expected by the parser. */
	private char expectedType = '?';

	public CastFunction(final ADQLOperand value, final DatatypeParam datatype) {
		this.value = value;
		this.datatype = datatype;
	}

	public CastFunction(final CastFunction toCopy) throws Exception {
		this.value = (ADQLOperand)toCopy.value.getCopy();
		this.datatype = (DatatypeParam)toCopy.datatype.getCopy();
		setPosition((toCopy.getPosition() == null) ? null : new TextPosition(toCopy.getPosition()));
	}

	@Override
	public boolean isNumeric() {
		return datatype.isNumeric();
	}

	@Override
	public boolean isString() {
		return datatype.isString();
	}

	@Override
	public boolean isGeometry() {
		return datatype.isGeometry();
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

	public DatatypeParam getTargetType() {
		return datatype;
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
		return 2;
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ value, datatype };
	}

	@Override
	public ADQLOperand getParameter(final int index) throws ArrayIndexOutOfBoundsException {
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"CAST\" (nb required params = " + getNbParameters() + ")!");

		switch(index) {
			case 0:
				return value;
			case 1:
				return datatype;
			default:
				return null;
		}
	}

	@Override
	public ADQLOperand setParameter(final int index, final ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (index < 0 || index >= getNbParameters())
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"CAST\" (nb required params = " + getNbParameters() + ")!");
		else if (replacer == null)
			throw new NullPointerException("Impossible to remove any parameter from the \"CAST\" function ! All parameters are required!");
		else {
			ADQLOperand replaced = null;
			switch(index) {
				case 0:
					replaced = value;
					value = replacer;
					setPosition(null);
					break;
				case 1:
					if (replaced instanceof DatatypeParam) {
						replaced = datatype;
						datatype = (DatatypeParam)replacer;
						setPosition(null);
					} else
						throw new Exception("Impossible to replace a DatatypeParam by a " + replacer.getClass().getName() + "!");
					break;
			}
			return replaced;
		}
	}

	@Override
	public String toADQL() {
		return FCT_NAME + "(" + value.toADQL() + " AS " + datatype.toADQL() + ")";
	}

}
