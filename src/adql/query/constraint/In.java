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
import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.ClauseADQL;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;

/**
 * It represents the IN predicate of SQL and ADQL.
 *
 * <p>
 * 	This predicate returns <code>true</code> if the value of the given operand is
 * 	either in the given values list or in the results of the given sub-query,
 * 	else it returns <code>false</code>.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class In implements ADQLConstraint {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, "IN", false, "An IN constrait (which tests whether a value is part of the given list of value or result of a sub-query).");

	/** The operand whose the value must be in the given list or in the results
	 * of the given sub-query. */
	private ADQLOperand leftOp;

	/** The sub-query which must return a list of values. */
	private ADQLQuery subQuery;

	/** The list of values. */
	private ADQLList<ADQLOperand> list;

	/** IN or NOT IN ? */
	private boolean notIn = false;

	/** Position of this {@link In} in the given ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Builds an IN constraint with a sub-query.
	 *
	 * @param op	The operand whose the value must be in the results of the
	 *          	given sub-query.
	 * @param query	A sub-query.
	 *
	 * @throws NullPointerException If the given operand and/or the given
	 *                             	sub-query is NULL.
	 */
	public In(ADQLOperand op, ADQLQuery query) throws NullPointerException {
		this(op, query, false);
	}

	/**
	 * Builds an IN constraint with a sub-query.
	 *
	 * @param op	The operand whose the value must be in the results of the
	 *          	given sub-query.
	 * @param query	A sub-query.
	 * @param notIn	<code>true</code> for NOT IN,
	 *             	<code>false</code> for IN.
	 *
	 * @throws NullPointerException	If the given operand and/or the given
	 *                             	sub-query is NULL.
	 */
	public In(ADQLOperand op, ADQLQuery query, boolean notIn) throws NullPointerException {
		setOperand(op);
		setSubQuery(query);
		setNotIn(notIn);
	}

	/**
	 * Builds an IN constraint with a values list.
	 *
	 * @param op			The operand whose the value must be in the given
	 *          			values list.
	 * @param valuesList	The values list.
	 *
	 * @throws 	NullPointerException	If the given operand is NULL and/or the
	 *                              	given list is NULL or empty.
	 */
	public In(ADQLOperand op, ADQLOperand[] valuesList) throws NullPointerException {
		this(op, valuesList, false);
	}

	/**
	 * Builds an IN constraint with a values list.
	 *
	 * @param op			The operand whose the value must be in the given
	 *          			values list.
	 * @param valuesList	The values list.
	 * @param notIn			<code>true</code> for NOT IN,
	 *             			<code>false</code> for IN.
	 *
	 * @throws NullPointerException	If the given operand is NULL and/or the
	 *                             	given list is NULL or empty.
	 */
	public In(ADQLOperand op, ADQLOperand[] valuesList, boolean notIn) throws NullPointerException {
		setOperand(op);
		setValuesList(valuesList);
		setNotIn(notIn);
	}

	/**
	 * Builds an IN constraint with a values list.
	 *
	 * @param op					The operand whose the value must be in the given values list.
	 * @param valuesList			The values list.
	 * @throws NullPointerException	If the given operand is NULL and/or the given list is NULL or empty.
	 */
	public In(ADQLOperand op, ADQLList<ADQLOperand> valuesList) throws NullPointerException {
		this(op, valuesList, false);
	}

	/**
	 * Builds an IN constraint with a values list.
	 *
	 * @param op			The operand whose the value must be in the given
	 *          			values list.
	 * @param valuesList	The values list.
	 * @param notIn			<code>true</code> for NOT IN,
	 *             			<code>false</code> for IN.
	 *
	 * @throws NullPointerException	If the given operand is NULL and/or the
	 *                             	given list is NULL or empty.
	 */
	public In(ADQLOperand op, ADQLList<ADQLOperand> valuesList, boolean notIn) throws NullPointerException {
		setOperand(op);
		setValuesList(valuesList);
		setNotIn(notIn);
	}

	/**
	 * Builds a IN constraint by copying the given one.
	 *
	 * @param toCopy	The IN constraint to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public In(In toCopy) throws Exception {
		leftOp = (ADQLOperand)toCopy.leftOp.getCopy();
		if (toCopy.hasSubQuery())
			setSubQuery((ADQLQuery)toCopy.subQuery.getCopy());
		else
			setValuesList((ADQLList<ADQLOperand>)toCopy.list.getCopy());
		notIn = toCopy.notIn;
		position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Gets the left operand of this IN constraint.
	 *
	 * @return	Its left operand.
	 */
	public final ADQLOperand getOperand() {
		return leftOp;
	}

	/**
	 * Replaces the left operand of this IN constraint.
	 *
	 * @param newLeftOp		Its new left operand.
	 *
	 * @throws NullPointerException		If the given operand is NULL.
	 */
	public void setOperand(ADQLOperand newLeftOp) throws NullPointerException {
		if (newLeftOp == null)
			throw new NullPointerException("Impossible to set a left operand NULL in an IN constraint!");
		else {
			leftOp = newLeftOp;
			position = null;
		}
	}

	/**
	 * Gets the sub-query (right operand) of this IN constraint.
	 *
	 * @return	Its sub-query.
	 */
	public final ADQLQuery getSubQuery() {
		return subQuery;
	}

	/**
	 * Tells whether the right operand of this IN constraint is a sub-query or
	 * a values list.
	 *
	 * @return	<code>true</code> if the right operand is a sub-query,
	 *        	<code>false</code> for a values list (even empty).
	 */
	public final boolean hasSubQuery() {
		return subQuery != null;
	}

	/**
	 * Replaces the sub-query (right operand) of this IN constraint.
	 *
	 * @param newSubQuery		Its new sub-query.
	 *
	 * @throws NullPointerException		If the given sub-query is NULL.
	 */
	public void setSubQuery(ADQLQuery newSubQuery) throws NullPointerException {
		if (newSubQuery == null)
			throw new NullPointerException("Impossible to set a sub-query NULL in an IN constraint!");
		else {
			list = null;
			subQuery = newSubQuery;
			position = null;
		}
	}

	/**
	 * Gets the values list (right operand) of this IN constraint.
	 *
	 * @return	Its values list.
	 */
	public final ADQLList<ADQLOperand> getValuesList() {
		return list;
	}

	/**
	 * Replaces the values list (right operand) of this IN constraint.
	 *
	 * @param valuesList	Its new values list.
	 *
	 * @throws NullPointerException		If the given list is NULL.
	 */
	public void setValuesList(ADQLOperand[] valuesList) throws NullPointerException {
		if (valuesList == null)
			throw new NullPointerException("Impossible to set a values list NULL in an IN constraint!");
		else if (valuesList.length > 0) {
			subQuery = null;
			list = new ClauseADQL<ADQLOperand>();
			for(int i = 0; i < valuesList.length; i++)
				list.add(valuesList[i]);
			position = null;
		}
	}

	/**
	 * Replaces the values list (right operand) of this IN constraint.
	 *
	 * @param valuesList	Its new values list.
	 *
	 * @throws NullPointerException		If the given list is NULL.
	 */
	public void setValuesList(ADQLList<ADQLOperand> valuesList) throws NullPointerException {
		if (valuesList == null)
			throw new NullPointerException("Impossible to set a values list NULL in an IN constraint!");
		else {
			subQuery = null;
			list = valuesList;
			position = null;
		}
	}

	/**
	 * Tells whether this predicate is IN or NOT IN.
	 *
	 * @return	<code>true</code> for NOT IN,
	 *        	<code>false</code> for IN.
	 */
	public final boolean isNotIn() {
		return notIn;
	}

	/**
	 * Lets telling whether this predicate must be IN or NOT IN.
	 *
	 * @param notIn	<code>true</code> for NOT IN,
	 *             	<code>false</code> for IN.
	 */
	public void setNotIn(boolean notIn) {
		this.notIn = notIn;
		position = null;
	}

	@Override
	public final TextPosition getPosition() {
		return position;
	}

	/**
	 * Set the position of this {@link In} in the given ADQL query string.
	 *
	 * @param position	New position of this {@link In}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position) {
		this.position = position;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new In(this);
	}

	@Override
	public String getName() {
		return notIn ? "NOT IN" : "IN";
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLIterator() {

			private int index = -1;

			@Override
			public ADQLObject next() {
				index++;
				if (index == 0)
					return leftOp;
				else if (index == 1)
					return hasSubQuery() ? subQuery : list;
				else
					throw new NoSuchElementException();
			}

			@Override
			public boolean hasNext() {
				return index + 1 < 2;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called!");

				if (replacer == null)
					remove();

				if (index == 0) {
					if (replacer instanceof ADQLOperand) {
						leftOp = (ADQLOperand)replacer;
						position = null;
					} else
						throw new UnsupportedOperationException("Impossible to replace an ADQLOperand by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
				} else if (index == 1) {
					if (hasSubQuery() && replacer instanceof ADQLQuery) {
						subQuery = (ADQLQuery)replacer;
						position = null;
					} else if (!hasSubQuery() && replacer instanceof ADQLList) {
						list = (ADQLList<ADQLOperand>)replacer;
						position = null;
					} else
						throw new UnsupportedOperationException("Impossible to replace an " + (hasSubQuery() ? "ADQLQuery" : "ADQLList<ADQLOperand>") + " by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
				}
			}

			@Override
			public void remove() {
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called!");

				if (index == 0)
					throw new UnsupportedOperationException("Impossible to remove the left operand of the IN constraint!");
				else if (index == 1)
					throw new UnsupportedOperationException("Impossible to remove the " + (hasSubQuery() ? "sub-query" : "values list") + " of the IN constraint!");
			}
		};
	}

	@Override
	public String toADQL() {
		return leftOp.toADQL() + " " + getName() + " (" + (hasSubQuery() ? subQuery.toADQL() : list.toADQL()) + ")";
	}

}