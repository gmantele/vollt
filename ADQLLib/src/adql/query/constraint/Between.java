package adql.query.constraint;

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
 *                       Astronomisches Rechen Institute (ARI)
 */

import java.util.NoSuchElementException;

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;

/**
 * It represents the BETWEEN predicate of SQL and ADQL.
 *
 * <p>
 * 	This predicate returns <code>true</code> if the value of the left operand is
 * 	between the value of the two other operands, else it returns
 * 	<code>false</code>.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class Between implements ADQLConstraint {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, "BETWEEN", false, "A BETWEEN constraint (i.e. interval comparaison for a numeric value).");

	/** Left part of the BETWEEN constraint. */
	private ADQLOperand leftOperand;

	/** Operand between the BETWEEN predicate and the AND keyword. */
	private ADQLOperand minOperand;

	/** Last part of the BETWEEN constraint (just after the AND keyword). */
	private ADQLOperand maxOperand;

	/** Indicates which predicate must be used: BETWEEN (<code>false</code>) or
	 * NOT BETWEEN (<code>true</code>). */
	private boolean notBetween = false;

	/** Position of this {@link Between} in the given ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Builds a BETWEEN constraints.
	 *
	 * @param operand	The left operand (left part of the BETWEEN predicate).
	 * @param min		The operand which represents the minimum value.
	 * @param max		The operand which represents the maximum value.
	 *
	 * @throws NullPointerException	If one of the given parameters is NULL.
	 */
	public Between(ADQLOperand operand, ADQLOperand min, ADQLOperand max) throws NullPointerException {
		this(operand, min, max, false);
	}

	/**
	 * Builds a BETWEEN constraints.
	 *
	 * @param operand		The left operand (left part of the BETWEEN
	 *               		predicate).
	 * @param min			The operand which represents the minimum value.
	 * @param max			The operand which represents the maximum value.
	 * @param notBetween	<code>true</code> if the predicate NOT BETWEEN must
	 *                  	be used
	 *                  	or <code>false</code> for BETWEEN.
	 *
	 * @throws NullPointerException	If one of the given parameters is NULL.
	 */
	public Between(ADQLOperand operand, ADQLOperand min, ADQLOperand max, boolean notBetween) throws NullPointerException {
		setLeftOperand(operand);
		setMinOperand(min);
		setMaxOperand(max);
		setNotBetween(notBetween);
	}

	/**
	 * Builds a BETWEEN constraint by copying the given one.
	 *
	 * @param toCopy		The BETWEEN constraint to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	public Between(Between toCopy) throws Exception {
		setLeftOperand((ADQLOperand)toCopy.leftOperand.getCopy());
		setMinOperand((ADQLOperand)toCopy.minOperand.getCopy());
		setMaxOperand((ADQLOperand)toCopy.maxOperand.getCopy());
		position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Gets the left operand of this BETWEEN constraint.
	 *
	 * @return	Its left operand.
	 */
	public final ADQLOperand getLeftOperand() {
		return leftOperand;
	}

	/**
	 * Replaces the left operand of this BETWEEN constraint.
	 *
	 * @param leftOperand	Its new left operand.
	 *
	 * @throws NullPointerException	If the given operand is NULL.
	 */
	public void setLeftOperand(ADQLOperand leftOperand) throws NullPointerException {
		this.leftOperand = leftOperand;
		position = null;
	}

	/**
	 * Gets the operand which represents the minimum value.
	 *
	 * @return	Its minimum value.
	 */
	public final ADQLOperand getMinOperand() {
		return minOperand;
	}

	/**
	 * Replaces the operand which represents the minimum value.
	 *
	 * @param minOperand	Its new minimum value.
	 *
	 * @throws NullPointerException	If the given operand is NULL.
	 */
	public void setMinOperand(ADQLOperand minOperand) throws NullPointerException {
		this.minOperand = minOperand;
		position = null;
	}

	/**
	 * Gets the operand which represents the maximum value.
	 *
	 * @return	Its maximum value.
	 */
	public final ADQLOperand getMaxOperand() {
		return maxOperand;
	}

	/**
	 * Replaces the operand which represents the maximum value.
	 *
	 * @param maxOperand	Its new maximum value.
	 *
	 * @throws NullPointerException	If the given operand is NULL.
	 */
	public void setMaxOperand(ADQLOperand maxOperand) throws NullPointerException {
		this.maxOperand = maxOperand;
		position = null;
	}

	/**
	 * Tells whether the predicate is BETWEEN or NOT BETWEEN.
	 *
	 * @return	<code>true</code> for NOT BETWEEN,
	 *        	<code>false</code> for BETWEEN.
	 */
	public final boolean isNotBetween() {
		return notBetween;
	}

	/**
	 * Lets indicating the predicate to use (BETWEEN or NOT BETWEEN).
	 *
	 * @param notBetween	<code>true</code> for NOT BETWEEN,
	 *                  	<code>false</code> for BETWEEN.
	 */
	public void setNotBetween(boolean notBetween) {
		this.notBetween = notBetween;
		position = null;
	}

	@Override
	public final TextPosition getPosition() {
		return position;
	}

	/**
	 * Set the position of this {@link Between} in the given ADQL query string.
	 *
	 * @param position	New position of this {@link Between}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position) {
		this.position = position;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new Between(this);
	}

	@Override
	public String getName() {
		return (isNotBetween() ? "NOT " : "") + "BETWEEN";
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLIterator() {

			private int index = -1;

			@Override
			public ADQLObject next() {
				switch(++index) {
					case 0:
						return leftOperand;
					case 1:
						return minOperand;
					case 2:
						return maxOperand;
					default:
						throw new NoSuchElementException();
				}
			}

			@Override
			public boolean hasNext() {
				return index + 1 < 3;
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called!");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLOperand) {
					switch(index) {
						case 0:
							leftOperand = (ADQLOperand)replacer;
							break;
						case 1:
							minOperand = (ADQLOperand)replacer;
							break;
						case 2:
							maxOperand = (ADQLOperand)replacer;
							break;
					}
					position = null;
				} else
					throw new UnsupportedOperationException("Impossible to replace an ADQLOperand by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
			}

			@Override
			public void remove() {
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called!");
				else
					throw new UnsupportedOperationException("Impossible to remove this operand from the BETWEEN constraint!");
			}
		};
	}

	@Override
	public String toADQL() {
		return leftOperand.toADQL() + " " + getName() + " " + minOperand.toADQL() + " AND " + maxOperand.toADQL();
	}

}
