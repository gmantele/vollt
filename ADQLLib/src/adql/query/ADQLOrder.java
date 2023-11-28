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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.NoSuchElementException;

import adql.parser.feature.LanguageFeature;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;

/**
 * Represents an item of the ORDER BY list: that's to say a column reference
 * or a value expression, and an optional sorting indication (ASC, DESC).
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (08/2019)
 */
public class ADQLOrder implements ADQLObject {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, "ORDER_BY_ITEM", false, "Column reference or expression on which the query result must be ordered.");

	/** Position in the original ADQL query string.
	 * @since 2.0 */
	private TextPosition position = null;

	/** Reference to the column on which the query result must be ordered.
	 * <p><i><b>Important note:</b>
	 * 	If NULL, this ORDER BY is done on a value expression.
	 * 	In such case, see {@link #expression}.
	 * </i></p>
	 * @since 2.0 */
	protected ColumnReference colRef = null;

	/** Value on which the query result must be ordered.
	 * <p><i><b>Important note:</b>
	 * 	If NULL, this ORDER BY is done on a column reference.
	 * 	In such case, see {@link #colRef}.
	 * </i></p>
	 * @since 2.0 */
	protected ADQLOperand expression = null;

	/** Gives an indication about how to order the results of a query.
	 * (<code>true</code> for DESCending, <code>false</code> for ASCending) */
	private boolean descSorting = false;

	/**
	 * Builds an order indication with the index of the selected column on which
	 * an ASCending ordering will be done.
	 *
	 * @param colIndex	The index of a selected column (from 1).
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the index is less or equal 0.
	 *
	 * @see #ADQLOrder(int, boolean)
	 */
	public ADQLOrder(final int colIndex) throws ArrayIndexOutOfBoundsException {
		this(colIndex, false);
	}

	/**
	 * Builds an order indication with the index of the selected column on which
	 * the specified ordering will be done.
	 *
	 * @param colIndex	The index of a selected column (from 1).
	 * @param desc		<code>true</code> means DESCending order,
	 *            		<code>false</code> means ASCending order.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the index is less or equal 0.
	 *
	 * @see ColumnReference#ColumnReference(int) ColumnReference(int)
	 */
	public ADQLOrder(final int colIndex, final boolean desc) throws ArrayIndexOutOfBoundsException {
		setOrder(colIndex, desc);
	}

	/**
	 * Builds an order indication with the name or the alias of the selected
	 * column on which an ASCending ordering will be done.
	 *
	 * @param colName	The name or the alias of a selected column.
	 *
	 * @throws NullPointerException	If the given name is NULL
	 *                             	or is an empty string.
	 *
	 * @see #ADQLOrder(String, boolean)
	 */
	public ADQLOrder(final String colName) throws NullPointerException {
		this(colName, false);
	}

	/**
	 * Builds an order indication with the name of the alias of the selected
	 * column on which the specified ordering will be done.
	 *
	 * @param colName	The name of the alias of a selected column.
	 * @param desc		<code>true</code> means DESCending order,
	 *            		<code>false</code> means ASCending order.
	 *
	 * @throws NullPointerException	If the given name is NULL
	 *                             	or is an empty string.
	 *
	 * @see ColumnReference#ColumnReference(String) ColumnReference(String)
	 *
	 * @deprecated	Since ADQL-2.1, a column reference can be a qualified
	 *            	column (i.e. an {@link ADQLColumn}). You should use
	 *            	{@link #ADQLOrder(ADQLOperand)} instead.
	 */
	@Deprecated
	public ADQLOrder(final String colName, final boolean desc) throws NullPointerException {
		this(new ADQLColumn(null, colName), desc);
	}

	/**
	 * Builds an order indication with the expression on which an ASCending
	 * ordering will be done.
	 *
	 * @param expr	The expression to order on.
	 *
	 * @throws NullPointerException	If the given expression is NULL.
	 *
	 * @see #ADQLOrder(ADQLOperand)
	 *
	 * @since 2.0
	 */
	public ADQLOrder(final ADQLOperand expr) throws NullPointerException {
		this(expr, false);
	}

	/**
	 * Builds an order indication with the expression on which the specified
	 * ordering will be done.
	 *
	 * @param expr	The expression to order on.
	 * @param desc	<code>true</code> means DESCending order,
	 *            	<code>false</code> means ASCending order.
	 *
	 * @throws NullPointerException	If the given expression is NULL.
	 *
	 * @since 2.0
	 */
	public ADQLOrder(final ADQLOperand expr, final boolean desc) throws NullPointerException {
		setOrder(expr, desc);
	}

	/**
	 * Builds an ORDER BY item by copying the given one.
	 *
	 * @param toCopy	The ORDER BY item to copy.
	 *
	 * @throws Exception	If the copy failed.
	 */
	public ADQLOrder(ADQLOrder toCopy) throws Exception {
		if (toCopy.colRef != null)
			colRef = (ColumnReference)toCopy.colRef.getCopy();
		if (toCopy.expression != null)
			expression = (ADQLOperand)toCopy.expression.getCopy();
		descSorting = toCopy.descSorting;
	}

	/**
	 * Tells how the results will be sorted.
	 *
	 * @return	<code>true</code> DESCending order,
	 *        	<code>false</code> ASCending order.
	 */
	public boolean isDescSorting() {
		return descSorting;
	}

	/**
	 * Get the reference of column on which the query result will be ordered.
	 *
	 * @return	The set column reference. <i>Might be NULL.</i>
	 *
	 * @since 2.0
	 */
	public final ColumnReference getColumnReference() {
		return colRef;
	}

	/**
	 * Get the expression on which the query result will be ordered.
	 *
	 * @return	The set expression. <i>Might be NULL.</i>
	 *
	 * @since 2.0
	 */
	public final ADQLOperand getExpression() {
		return expression;
	}

	/**
	 * Updates the current order indication.
	 *
	 * @param colIndex	The index of a selected column (from 1).
	 * @param desc		<code>true</code> means DESCending order,
	 *            		<code>false</code> means ASCending order.
	 *
	 * @throws IndexOutOfBoundsException	If the given index is less
	 *                                  	or equal 0.
	 */
	public void setOrder(final int colIndex, final boolean desc) throws ArrayIndexOutOfBoundsException {
		if (colIndex <= 0)
			throw new ArrayIndexOutOfBoundsException("Impossible to make a reference to the " + colIndex + "th column: a column index must be greater or equal 1!");

		colRef = new ColumnReference(colIndex);
		expression = null;
		descSorting = desc;
	}

	/**
	 * Updates the current order indication.
	 *
	 * @param colName	The name or the alias of a selected column.
	 * @param desc		<code>true</code> means DESCending order,
	 *            		<code>false</code> means ASCending order.
	 *
	 * @throws NullPointerException	If the given name is NULL
	 *                             	or is an empty string.
	 *
	 * @deprecated	Since ADQL-2.1, a column reference can be a qualified
	 *            	column (i.e. an {@link ADQLColumn}). You should use
	 *            	{@link #setOrder(ADQLOperand, boolean)} instead.
	 */
	@Deprecated
	public void setOrder(final String colName, final boolean desc) throws NullPointerException {
		if (colName == null)
			throw new NullPointerException("Impossible to make a reference: the given name is null or is an empty string!");

		colRef = null;
		expression = new ADQLColumn(null, colName);
		descSorting = desc;
		position = null;
	}

	/**
	 * Updates the current order indication.
	 *
	 * @param expr	The expression to order on.
	 * @param desc	<code>true</code> means DESCending order,
	 *            	<code>false</code> means ASCending order.
	 *
	 * @throws NullPointerException	If the given expression is NULL.
	 *
	 * @since 2.0
	 */
	public void setOrder(final ADQLOperand expr, boolean desc) throws NullPointerException {
		if (expr == null)
			throw new NullPointerException("Impossible to make a reference: the given expression is null!");

		colRef = null;
		expression = expr;
		descSorting = desc;
		position = null;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ADQLOrder(this);
	}

	@Override
	public String getName() {
		return (colRef != null ? colRef.getName() : expression.getName()) + (descSorting ? "_DESC" : "_ASC");
	}

	@Override
	public String toADQL() {
		return (colRef != null ? colRef.toADQL() : expression.toADQL()) + (descSorting ? " DESC" : " ASC");
	}

	/**
	 * Gets the position in the original ADQL query string.
	 *
	 * @return	The position of this {@link ColumnReference}.
	 */
	@Override
	public final TextPosition getPosition() {
		return position;
	}

	/**
	 * Sets the position at which this {@link ColumnReference} has been found in the original ADQL query string.
	 *
	 * @param pos	Position of this {@link ColumnReference}.
	 */
	public void setPosition(final TextPosition pos) {
		position = pos;
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLIterator() {

			private boolean itemDone = false;

			@Override
			public ADQLObject next() {
				if (itemDone)
					throw new NoSuchElementException();
				else
					itemDone = true;
				return (colRef != null ? colRef : expression);
			}

			@Override
			public boolean hasNext() {
				return !itemDone;
			}

			@Override
			public void replace(final ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (!itemDone)
					throw new IllegalStateException("No iteration done yet!");
				else if (replacer == null)
					throw new UnsupportedOperationException("Impossible to delete a column reference or an expression from an ORDER BY item! You must delete the complete ORDER BY item.");

				if (replacer instanceof ColumnReference) {
					colRef = (ColumnReference)replacer;
					expression = null;
				} else if (replacer instanceof ADQLOperand) {
					colRef = null;
					expression = (ADQLOperand)replacer;
				} else
					throw new UnsupportedOperationException("Impossible to replace a column reference or a value expression by a " + replacer.getClass().getName() + "!");

				position = null;
			}
		};
	}

}
