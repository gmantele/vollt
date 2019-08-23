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
 * Copyright 2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.NoSuchElementException;

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.TextPosition;

/**
 * Operand that represents the binary complement (bitwise NOT) of a given
 * numeric operand.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (08/2019)
 * @since 2.0
 */
public final class BitNotOperand implements ADQLOperand {

	/** Description of this ADQL Feature. */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_BITWISE, "BIT_NOT", true);

	/** The operand whose bits must be reversed. */
	private ADQLOperand operand;

	/** Position of this operand. */
	private TextPosition position = null;

	/**
	 * Builds an operand which computes the binary complement of the given
	 * operand.
	 *
	 * <p><i><b>Important:</b>
	 * 	The given operand must be numeric ({@link ADQLOperand#isNumeric()} must
	 * 	return <code>true</code>)!
	 * </b></p>
	 *
	 * @param operand	The operand whose binary complement must be computed.
	 *
	 * @throws NullPointerException				If the given operand is NULL.
	 * @throws UnsupportedOperationException	If the given operand is not
	 *                                          numeric (if {@link ADQLOperand#isNumeric()}
	 *                                          does not return
	 *                                          <code>true</code>).
	 */
	public BitNotOperand(ADQLOperand operand) throws NullPointerException, UnsupportedOperationException {
		if (operand == null)
			throw new NullPointerException("Impossible to apply a bitwise NOT on NULL!");

		if (operand.isNumeric())
			this.operand = operand;
		else
			throw new UnsupportedOperationException("Impossible to apply a bitwise NOT on a non-numeric operand (" + operand.toADQL() + ")!");
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Gets the operand whose binary complement must be computed.
	 *
	 * @return	The operand to binary-complement.
	 */
	public final ADQLOperand getOperand() {
		return operand;
	}

	/** Always returns <code>true</code>.
	 * @see adql.query.operand.ADQLOperand#isNumeric()
	 */
	@Override
	public final boolean isNumeric() {
		return true;
	}

	/** Always returns <code>false</code>.
	 * @see adql.query.operand.ADQLOperand#isString()
	 */
	@Override
	public final boolean isString() {
		return false;
	}

	@Override
	public final TextPosition getPosition() {
		return this.position;
	}

	/**
	 * Sets the position at which this {@link BitNotOperand} has been found in
	 * the original ADQL query string.
	 *
	 * @param position	Position of this {@link BitNotOperand}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position) {
		this.position = position;
	}

	/** Always returns <code>false</code>.
	 * @see adql.query.operand.ADQLOperand#isGeometry()
	 */
	@Override
	public final boolean isGeometry() {
		return false;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		BitNotOperand copy = new BitNotOperand((ADQLOperand)operand.getCopy());
		return copy;
	}

	@Override
	public String getName() {
		return "BIT_NOT_" + operand.getName();
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLIterator() {

			private boolean operandGot = (operand == null);

			@Override
			public ADQLObject next() {
				if (operandGot)
					throw new NoSuchElementException();
				operandGot = true;
				return operand;
			}

			@Override
			public boolean hasNext() {
				return !operandGot;
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (!operandGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called!");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLOperand && ((ADQLOperand)replacer).isNumeric())
					operand = (ADQLOperand)replacer;
				else
					throw new UnsupportedOperationException("Impossible to replace the operand \"" + operand.toADQL() + "\" by \"" + replacer.toADQL() + "\" in the BitNotOperand \"" + toADQL() + "\" because the replacer is not an ADQLOperand or is not numeric!");
			}

			@Override
			public void remove() {
				if (!operandGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called!");
				else
					throw new UnsupportedOperationException("Impossible to remove the only operand (" + operand.toADQL() + ") of a BitNotOperand (" + toADQL() + "). However you can remove the whole BitNotOperand.");
			}
		};
	}

	@Override
	public String toADQL() {
		return "~" + operand.toADQL();
	}

}
