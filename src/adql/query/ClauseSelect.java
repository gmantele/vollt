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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.List;

import adql.query.operand.ADQLOperand;

/**
 * The SELECT clause of an ADQL query.
 *
 * <p>This ADQL clause is not only a list of ADQL items:</p>
 * <ul>
 * 	<li>The user can specify the maximum number of rows the query must
 * 		return.</li>
 * 	<li>He can also ask that all the returned rows are unique according to the
 * 		first returned column.</li>
 * </ul>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (09/2017)
 */
public class ClauseSelect extends ClauseADQL<SelectItem> {

	/** Indicates whether all returned rows are unique regarding the first
	 * returned column. */
	private boolean distinct = false;

	/** The maximum number of returned rows. */
	private int limit = -1;

	/**
	 * Builds an empty SELECT clause.
	 */
	public ClauseSelect() {
		this(false, -1);
	}

	/**
	 * Builds an empty SELECT clause by specifying whether the returned rows are
	 * unique (regarding the first returned columns).
	 *
	 * @param distinctColumns	<code>true</code> means unique rows
	 *                       	(= SELECT DISTINCT),
	 *                       	<code>false</code> otherwise (= SELECT or
	 *                       	= SELECT ALL).
	 */
	public ClauseSelect(boolean distinctColumns) {
		this(distinctColumns, -1);
	}

	/**
	 * Builds an empty SELECT clause whose the returned rows must be limited to
	 * the given number.
	 *
	 * @param limit	Maximum number of returned rows (= SELECT TOP limit).
	 */
	public ClauseSelect(int limit) {
		this(false, limit);
	}

	/**
	 * Builds an empty SELECT clause.
	 *
	 * @param distinctColumns	<code>true</code> means unique rows
	 *                       	(= SELECT DISTINCT),
	 *                       	<code>false</code> otherwise (= SELECT or
	 *                       	= SELECT ALL).
	 * @param limit				Maximum number of returned rows
	 *             				(= SELECT TOP limit).
	 */
	public ClauseSelect(boolean distinctColumns, int limit) {
		super("SELECT");
		distinct = distinctColumns;
		this.limit = limit;
	}

	/**
	 * Builds a SELECT clause by copying the given one.
	 *
	 * @param toCopy		The SELECT clause to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	public ClauseSelect(ClauseSelect toCopy) throws Exception {
		super(toCopy);
		distinct = toCopy.distinct;
		limit = toCopy.limit;
	}

	/**
	 * Tells whether this clause imposes that returned rows are unique
	 * (regarding the first returned column).
	 *
	 * @return	<code>true</code> for SELECT DISTINCT,
	 *        	<code>false</code> for SELECT ALL.
	 */
	public final boolean distinctColumns() {
		return distinct;
	}

	/**
	 * Changes the DISTINCT flag of this SELECT clause.
	 *
	 * @param distinct	<code>true</code> for SELECT DISTINCT,
	 *                	<code>false</code> for SELECT ALL.
	 */
	public final void setDistinctColumns(boolean distinct) {
		this.distinct = distinct;
	}

	/**
	 * Indicates whether this SELECT clause imposes a maximum number of rows.
	 *
	 * @return	<code>true</code> this clause has a TOP flag,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean hasLimit() {
		return limit >= 0;
	}

	/**
	 * Gets the maximum number of rows imposed by this SELECT clause.
	 *
	 * @return	Maximum number of rows the query must return (SELECT TOP limit).
	 */
	public final int getLimit() {
		return limit;
	}

	/**
	 * Sets no maximum number of rows (classic SELECT).
	 */
	public final void setNoLimit() {
		limit = -1;
	}

	/**
	 * Changes the maximum number of rows this clause imposes.
	 *
	 * @param limit	The maximum number of returned rows (SELECT TOP limit).
	 */
	public final void setLimit(int limit) {
		this.limit = limit;
	}

	/**
	 * <p>Adds an operand to this SELECT clause.</p>
	 *
	 * <p><i><b>IMPORTANT:</b>
	 * 	The given operand will not be added directly!
	 * 	It will be encapsulated in a {@link SelectItem} object which will be
	 * 	then added to the SELECT clause.
	 * </i></p>
	 *
	 * @param operand	The operand to add.
	 *
	 * @return	<code>true</code> if the operand has been successfully added,
	 *        	<code>false</code> otherwise.
	 *
	 * @throws NullPointerException	If the given item is NULL.
	 *
	 * @see SelectItem
	 */
	public boolean add(ADQLOperand operand) throws NullPointerException {
		if (operand == null)
			throw new NullPointerException("It is impossible to add NULL items to a SELECT clause!");
		return add(new SelectItem(operand));
	}

	/**
	 * Adds an operand to this SELECT clause at the given position.
	 *
	 * <p><i><b>IMPORTANT:</b>
	 * 	The given operand will not be added directly!
	 * 	It will be encapsulated in a {@link SelectItem} object which will be
	 * 	then added to the SELECT clause.
	 * </i></p>
	 *
	 * @param index		The position at which the given operand must be added.
	 * @param operand	The operand to add.
	 *
	 * @throws NullPointerException				If the given item is NULL.
	 * @throws ArrayIndexOutOfBoundsException	If the index is out of range
	 *                                       	(index < 0 || index > size()).
	 *
	 * @see SelectItem
	 */
	public void add(int index, ADQLOperand operand) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (operand == null)
			throw new NullPointerException("It is impossible to add NULL items to a SELECT clause!");
		add(index, new SelectItem(operand));
	}

	/**
	 * Replaces the specified operand by the given one.
	 *
	 * <p><i><b>IMPORTANT:</b>
	 * 	The given operand will not be added directly!
	 * 	It will be encapsulated in a {@link SelectItem} object which will be
	 * 	then added to the SELECT clause.
	 * </i></p>
	 *
	 * @param index		The position of the SELECT item to replace.
	 * @param operand	The replacer of the specified SELECT item.
	 *
	 * @return	The replaced SELECT item.
	 *
	 * @throws NullPointerException				If the given item is NULL.
	 * @throws ArrayIndexOutOfBoundsException	If the index is out of range
	 *                                       	(index < 0 || index > size()).
	 */
	public ADQLOperand set(int index, ADQLOperand operand) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (operand == null)
			throw new NullPointerException("It is impossible to replace a SELECT item by a NULL item into a SELECT clause!");
		SelectItem item = set(index, new SelectItem(operand));
		return item.getOperand();
	}

	/**
	 * Gets the specified operand.
	 *
	 * @param index		Index of the operand to retrieve.
	 *
	 * @return	The corresponding operand.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the index is out of range
	 *                                       	(index < 0 || index > size()).
	 */
	public ADQLOperand searchByIndex(int index) throws ArrayIndexOutOfBoundsException {
		return get(index).getOperand();
	}

	/**
	 * Gets the operand which is associated with the given alias
	 * (case sensitive).
	 *
	 * @param alias	Alias of the operand to retrieve.
	 *
	 * @return	The corresponding operand or NULL if none has been found.
	 *
	 * @see #searchByAlias(String, boolean)
	 */
	public ADQLOperand searchByAlias(String alias) {
		List<SelectItem> founds = searchByAlias(alias, true);
		if (founds.isEmpty())
			return null;
		else
			return founds.get(0).getOperand();
	}

	/**
	 * Gets all the select items which are associated with the given alias.
	 *
	 * @param alias	Alias of the operand to retrieve.
	 *
	 * @return	All the corresponding select items.
	 */
	public List<SelectItem> searchByAlias(String alias, boolean caseSensitive) {
		if (alias == null)
			return new ArrayList<SelectItem>(0);

		ArrayList<SelectItem> founds = new ArrayList<SelectItem>();
		for(SelectItem item : this) {
			if (item.hasAlias()) {
				if (!caseSensitive) {
					if (item.isCaseSensitive()) {
						if (item.getAlias().equals(alias.toLowerCase()))
							founds.add(item);
					} else {
						if (item.getAlias().equalsIgnoreCase(alias))
							founds.add(item);
					}
				} else {
					if (item.isCaseSensitive()) {
						if (item.getAlias().equals(alias))
							founds.add(item);
					} else {
						if (item.getAlias().toLowerCase().equals(alias))
							founds.add(item);
					}
				}
			}
		}
		return founds;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ClauseSelect(this);
	}

	@Override
	public String toADQL() {
		String adql = null;

		for(int i = 0; i < size(); i++) {
			if (i == 0) {
				adql = getName() + (distinct ? " DISTINCT" : "") + (hasLimit() ? (" TOP " + limit) : "");
			} else
				adql += " " + getSeparator(i);

			adql += " " + get(i).toADQL();
		}

		return adql;
	}

}
