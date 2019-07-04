package adql.query.operand.function.string;

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
import adql.query.operand.function.ADQLFunction;

/**
 * It represents the LOWER function of ADQL.
 *
 * <p>This function converts its string parameter to lower case.</p>
 *
 * <p>
 * 	Since case folding is a nontrivial operation in a multi-encoding world,
 * 	ADQL requires standard behaviour for the ASCII characters, and
 * 	recommends following algorithm R2 described in Section 3.13,
 * 	"Default Case Algorithms" of The Unicode Consortium (2012) for characters
 * 	outside the ASCII set.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>LOWER('Francis Albert Augustus Charles Emmanuel')</pre>
 * <p>which should return:</p>
 * <pre>francis albert augustus charles emmanuel</pre>
 * </i>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (07/2019)
 * @since 2.0
 */
public class LowerFunction extends ADQLFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_STRING, "LOWER", true, "Convert all characters of the given string in lower case.");

	/** Constant name of this function. */
	protected final String FCT_NAME = "LOWER";

	/** The only parameter of this function. */
	protected ADQLOperand strParam;

	/**
	 * Builds a LOWER function with its parameter.
	 *
	 * @param param					Parameter of LOWER.
	 * @throws NullPointerException	If the given operand is NULL
	 *                             	or if it's not a string parameter.
	 */
	public LowerFunction(final ADQLOperand strParam) {
		if (strParam == null)
			throw new NullPointerException("The function " + FCT_NAME + " must have one non-NULL parameter!");

		if (!strParam.isString())
			throw new NullPointerException("The ADQL function " + FCT_NAME + " must have one parameter of type VARCHAR (i.e. a String)!");

		this.strParam = strParam;
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public final boolean isNumeric() {
		return false;
	}

	@Override
	public final boolean isString() {
		return true;
	}

	@Override
	public final boolean isGeometry() {
		return false;
	}

	@Override
	public final String getName() {
		return FCT_NAME;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new LowerFunction((ADQLOperand)(strParam.getCopy()));
	}

	@Override
	public int getNbParameters() {
		return 1;
	}

	@Override
	public final ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ strParam };
	}

	@Override
	public ADQLOperand getParameter(final int index) throws ArrayIndexOutOfBoundsException {
		if (index == 0)
			return strParam;
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + FCT_NAME + "\"!");
	}

	@Override
	public ADQLOperand setParameter(final int index, final ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (index == 0) {
			ADQLOperand replaced = strParam;
			if (replacer == null)
				throw new NullPointerException("Missing the new parameter of the function \"" + toADQL() + "\"!");
			else if (replacer.isString())
				strParam = replacer;
			else
				throw new Exception("Impossible to replace a String parameter by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
			setPosition(null);
			return replaced;
		} else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + FCT_NAME + "\"!");
	}

}
