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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.NoSuchElementException;

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.TextPosition;

/**
 * Lets wrapping an operand by parenthesis.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class WrappedOperand implements ADQLOperand {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, "OPERAND_WRAP", false, "An operand wrapped between parenthesis.");

	/** The wrapped operand. */
	private ADQLOperand operand;
	/** Position of this operand.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Wraps the given operand.
	 *
	 * @param operand	Operand to wrap.
	 *
	 * @throws NullPointerException	If the given operand is NULL.
	 */
	public WrappedOperand(ADQLOperand operand) throws NullPointerException {
		if (operand == null)
			throw new NullPointerException("Impossible to wrap a NULL operand: (NULL) has no sense!");
		this.operand = operand;
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Gets the wrapped operand.
	 *
	 * @return Its operand.
	 */
	public final ADQLOperand getOperand() {
		return operand;
	}

	@Override
	public final boolean isNumeric() {
		return operand.isNumeric();
	}

	@Override
	public final boolean isString() {
		return operand.isString();
	}

	@Override
	public final TextPosition getPosition() {
		return this.position;
	}

	/**
	 * Sets the position at which this {@link WrappedOperand} has been found in
	 * the original ADQL query string.
	 *
	 * @param position	Position of this {@link WrappedOperand}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position) {
		this.position = position;
	}

	@Override
	public final boolean isGeometry() {
		return operand.isGeometry();
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new WrappedOperand((ADQLOperand)operand.getCopy());
	}

	@Override
	public String getName() {
		return operand.getName();
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
				else if (replacer instanceof ADQLOperand) {
					operand = (ADQLOperand)replacer;
					position = null;
				} else
					throw new UnsupportedOperationException("Impossible to replace an ADQLOperand (\"" + operand + "\") by a " + replacer.getClass().getName() + " (\"" + replacer.toADQL() + "\")!");
			}

			@Override
			public void remove() {
				if (!operandGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called!");
				else
					throw new UnsupportedOperationException("Impossible to remove the only item of the WrappedOperand \"" + toADQL() + "\": the WrappedOperand would be empty!");
			}
		};
	}

	@Override
	public String toADQL() {
		return "(" + operand.toADQL() + ")";
	}

}
