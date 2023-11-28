package adql.query.operand;

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
 * Copyright 2023 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.NullADQLIterator;
import adql.query.TextPosition;

/**
 * Special value representing the absence of value (i.e. NULL in SQL).
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (02/2023)
 * @since 2.0
 */
public class NullValue implements ADQLOperand {

	/** Operand name. */
	public static final String NAME = "NULL";

	/** Description of this ADQL Feature. */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, NAME, false, "The NULL value.");

	/** Position of this operand. */
	protected TextPosition position = null;

	public NullValue() throws NumberFormatException {}

	/**
	 * Builds a NumericConstant by copying the given one.
	 *
	 * @param toCopy	The NumericConstant to copy.
	 */
	public NullValue(NullValue toCopy) {  }

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/** Always returns <code>true</code>.
	 * @see ADQLOperand#isNumeric()
	 */
	@Override
	public final boolean isNumeric() {
		return true;
	}

	/** Always returns <code>true</code>.
	 * @see ADQLOperand#isString()
	 */
	@Override
	public final boolean isString() {
		return true;
	}

	@Override
	public final TextPosition getPosition() {
		return this.position;
	}

	/**
	 * Sets the position at which this {@link NullValue} has been found in
	 * the original ADQL query string.
	 *
	 * @param position	Position of this {@link NullValue}.
	 */
	public final void setPosition(final TextPosition position) {
		this.position = position;
	}

	/** Always returns <code>true</code>.
	 * @see ADQLOperand#isGeometry()
	 */
	@Override
	public final boolean isGeometry() {
		return true;
	}

	@Override
	public ADQLObject getCopy() {
		return new NullValue(this);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new NullADQLIterator();
	}

	@Override
	public String toADQL() {
		return NAME;
	}

}
