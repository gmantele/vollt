package adql.query;

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
 * Copyright 2022 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.NoSuchElementException;

import adql.db.DBColumn;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.feature.LanguageFeature;

/**
 * It represents an operation between two rows sets (UNION, EXCEPT, INTERSECT).
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (07/2022)
 * @since 2.0
 *
 * @see SetOperationType
 */
public class SetOperation extends ADQLSet {

	/** Part of the operation at the left of the set operator. */
	private ADQLSet leftSet;

	/**
	 * Set operation type: UNION, INTERSECT, EXCEPT
	 * @see SetOperationType
	 */
	private SetOperationType operation;

	/** Flag to indicate whether the <code>ALL</code> keyword is used.
	 * <code>true</code> if all <code>ALL</code> is used,
	 * <code>false</code> otherwise. */
	private boolean withDuplicates = false;

	/** Part of the operation at the right of the set operator. */
	private ADQLSet rightSet;

	/**
	 * Builds a set operation.
	 *
	 * @param leftSet		Left set.
	 * @param operation		Set operation type.
	 * @param rightSet		Right set.
	 *
	 * @throws NullPointerException	If one of the given parameters is NULL.
	 *
	 * @see SetOperation#setLeftSet(ADQLSet)
	 * @see SetOperation#setRightSet(ADQLSet)
	 */
	public SetOperation(final ADQLSet leftSet, final SetOperationType operation, final ADQLSet rightSet) {
		this(null, leftSet, operation, rightSet);
	}

	/**
	 * Builds a set operation.
	 *
	 * @param version		Followed version of the ADQL grammar.
	 *               		<i>If NULL, the
	 *               		{@link adql.parser.ADQLParser#DEFAULT_VERSION default version}
	 *               		will be set.</i>
	 * @param leftSet		Left set.
	 * @param operation		Set operation type.
	 * @param rightSet		Right set.
	 *
	 * @throws NullPointerException	If one of the three last parameters is NULL.
	 *
	 * @see SetOperation#setLeftSet(ADQLSet)
	 * @see SetOperation#setRightSet(ADQLSet)
	 */
	public SetOperation(final ADQLVersion version, final ADQLSet leftSet, final SetOperationType operation, final ADQLSet rightSet) {
		super(version);
		setLeftSet(leftSet);
		setOperation(operation);
		setRightSet(rightSet);
		setPosition(new TextPosition(leftSet.getPosition(), rightSet.getPosition()));
	}

	/**
	 * Builds a {@link SetOperation} by copying the given one.
	 *
	 * @param toCopy	The {@link SetOperation} to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	public SetOperation(final SetOperation toCopy) throws Exception {
		super(toCopy);
		leftSet = (ADQLSet)toCopy.leftSet.getCopy();
		operation = toCopy.operation;
		rightSet = (ADQLSet)toCopy.rightSet.getCopy();
		position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
	}

	@Override
	public String getName() {
		return operation.toString();
	}

	/**
	 * Gets the left set.
	 *
	 * @return The left set.
	 */
	public ADQLSet getLeftSet() {
		return leftSet;
	}

	/**
	 * Changes the left set of this set operation.
	 *
	 * @param newLeftSet	The new left set of this set operation.
	 *
	 * @throws NullPointerException		If the given set is NULL.
	 */
	public void setLeftSet(final ADQLSet newLeftSet) throws NullPointerException, UnsupportedOperationException {
		if (newLeftSet == null)
			throw new NullPointerException("Impossible to update a SetOperation with a left set equals to NULL!");

		leftSet = newLeftSet;

		position = null;
	}

	/**
	 * Gets the set operation type.
	 *
	 * @return The set operation type.
	 *
	 * @see SetOperationType
	 */
	public SetOperationType getOperation() {
		return operation;
	}

	/**
	 * Changes the type of this set operation (UNION, EXCEPT, INTERSECT).
	 *
	 * @param newOperation	The new type of this set operation.
	 *
	 * @see SetOperationType
	 */
	public void setOperation(SetOperationType newOperation) {
		if (newOperation != null)
			operation = newOperation;
	}

	/**
	 * Tell whether duplicate rows must be kept.
	 *
	 * @return	<code>true</code> if duplicates must be preserved,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean isWithDuplicates() {
		return withDuplicates;
	}

	/**
	 * @param withDuplicates the withDuplicates to set
	 */
	public final void setWithDuplicates(final boolean withDuplicates) {
		this.withDuplicates = withDuplicates;
	}

	/**
	 * Gets the right set.
	 *
	 * @return The right set.
	 */
	public ADQLSet getRightSet() {
		return rightSet;
	}

	/**
	 * Changes the right set of this set operation.
	 *
	 * @param newRightSet	The new right set of this set operation.
	 *
	 * @throws NullPointerException		If the given set is NULL.
	 */
	public void setRightSet(final ADQLSet newRightSet) throws NullPointerException, UnsupportedOperationException {
		if (newRightSet == null)
			throw new NullPointerException("Impossible to update a SetOperation with a right set equals to NULL!");

		rightSet = newRightSet;

		position = null;
	}

	@Override
	public boolean hasLimit() {
		return false;
	}

	@Override
	public int getLimit() {
		return -1;
	}

	@Override
	public void setNoLimit() {
		// No possibility to set a limit on a SET operation in ADQL!
	}

	@Override
	public void setLimit(int limit) {
		// No possibility to set a limit on a SET operation in ADQL!
	}

	/**
	 * Gets the list of columns (database metadata) selected by this set
	 * operation.
	 *
	 * @return	Selected columns metadata.
	 */
	@Override
	public DBColumn[] getResultingColumns() {
		return leftSet.getResultingColumns();
	}

	@Override
	public LanguageFeature getFeatureDescription() {
		return operation.getFeatureDescription();
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new SetOperation(this);
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLIterator() {

			private int index = -1;
			private ADQLObject currentClause = null;

			@Override
			public ADQLObject next() {
				index++;
				switch(index) {
					case 0:
						currentClause = with;
						break;
					case 1:
						currentClause = leftSet;
						break;
					case 2:
						currentClause = rightSet;
						break;
					case 3:
						currentClause = orderBy;
						break;
					case 4:
						currentClause = null;
						return offset;
					default:
						throw new NoSuchElementException();
				}
				return currentClause;
			}

			@Override
			public boolean hasNext() {
				return index + 1 < 5;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called!");

				if (replacer == null)
					remove();
				else {
					switch(index) {
						case 0:
							if (replacer instanceof ClauseADQL)
								with = (ClauseADQL<WithItem>)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseADQL (" + with.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 1:
							if (replacer instanceof ADQLSet)
								leftSet = (ADQLSet)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace an ADQLSet (" + leftSet.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 2:
							if (replacer instanceof ADQLSet)
								rightSet = (ADQLSet)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ADQLSet (" + rightSet.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 3:
							if (replacer instanceof ClauseADQL)
								orderBy = (ClauseADQL<ADQLOrder>)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseADQL (" + orderBy.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 4:
							if (replacer instanceof ClauseOffset)
								offset = (ClauseOffset)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseOffset (" + offset.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
					}
					position = null;
				}
			}

			@Override
			public void remove() {
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called!");

				if (index == 1 || index == 2)
					throw new UnsupportedOperationException("Impossible to remove the " + ((index == 1) ? "left" : "right") + " set from a set operation!");
				else if (index == 4) {
					offset = null;
					position = null;
				} else {
					if (currentClause != null)
						((ClauseADQL<?>)currentClause).clear();
					position = null;
				}
			}
		};
	}

	@Override
	public String toADQL() {
		StringBuffer adql = new StringBuffer();
		adql.append('(');

		if (!with.isEmpty())
			adql.append(with.toADQL()).append('\n');

		boolean extendedSetExp = (!leftSet.with.isEmpty() || !leftSet.orderBy.isEmpty() || leftSet.offset != null);
		if (extendedSetExp)
			adql.append('(');
		adql.append(leftSet.toADQL());
		if (extendedSetExp)
			adql.append(')');

		adql.append('\n').append(operation.toADQL());
		if (withDuplicates)
			adql.append(" ALL");
		adql.append('\n');

		extendedSetExp = (!rightSet.with.isEmpty() || !rightSet.orderBy.isEmpty() || rightSet.offset != null);
		if (extendedSetExp)
			adql.append('(');
		adql.append(rightSet.toADQL());
		if (extendedSetExp)
			adql.append(')');

		if (!orderBy.isEmpty())
			adql.append('\n').append(orderBy.toADQL());

		if (offset != null)
			adql.append('\n').append(offset.toADQL());

		return adql.append(')').toString();
	}

}
