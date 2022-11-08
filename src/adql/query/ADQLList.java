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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Iterator;
import java.util.Vector;

import adql.parser.feature.LanguageFeature;

/**
 * Represents a list of ADQL items.
 *
 * <p>
 * 	Since it is a list, it is possible to add, remove, modify and iterate on a
 * 	such object.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 *
 * @see ClauseADQL
 * @see ClauseConstraints
 * @see adql.query.operand.Operation
 * @see adql.query.operand.Concatenation
 */
public abstract class ADQLList<T extends ADQLObject> implements ADQLObject, Iterable<T> {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	protected final LanguageFeature FEATURE;

	/** Label of the list. */
	private final String name;

	/** List of ADQL items. */
	private final Vector<T> list = new Vector<T>();

	/** Position inside an ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Builds an ADQLList with only its name. This name will always prefix the
	 * list.
	 *
	 * @param name	Prefix/Name of this list.
	 */
	protected ADQLList(String name) {
		if (name != null) {
			name = name.trim();
			if (name.length() == 0)
				name = null;
		}

		this.name = name;

		this.FEATURE = new LanguageFeature(null, "CLAUSE" + (this.name == null ? "" : "_" + this.name), false, "An ADQL clause (e.g. SELECT, FROM, ...).");
	}

	/**
	 * Builds an ADQLList by copying the given one.
	 *
	 * <p><i><b>Note:</b> It is a deep copy.</i></p>
	 *
	 * @param toCopy		The ADQLList to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	protected ADQLList(ADQLList<T> toCopy) throws Exception {
		this(toCopy.getName());
		for(T obj : toCopy)
			add((T)obj.getCopy());
		position = (toCopy.position != null) ? new TextPosition(toCopy.position) : null;
	}

	@Override
	public LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Adds the given item (if not NULL) at the end of this clause.
	 *
	 * @param item	The ADQL item to add to this clause.
	 * @return		<code>true</code> if the given item has been successfully
	 *        		added,
	 *        		<code>false</code> otherwise.
	 *
	 * @throws NullPointerException	If the given item is NULL.
	 */
	public boolean add(T item) throws NullPointerException {
		if (item == null)
			throw new NullPointerException("It is impossible to add NULL items to an ADQL clause!");
		else {
			if (list.add(item)) {
				position = null;
				return true;
			} else
				return false;
		}
	}

	/**
	 * Adds the given item (if not NULL) at the given position into this clause.
	 *
	 * @param index	Position at which the given item must be added.
	 * @param item	ADQL item to add to this clause.
	 *
	 * @throws NullPointerException				If the given item is NULL.
	 * @throws ArrayIndexOutOfBoundsException	If the index is out of range
	 *                                       	(index < 0 || index > size()).
	 */
	public void add(int index, T item) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (item != null) {
			list.add(index, item);
			position = null;
		} else
			throw new NullPointerException("It is impossible to add NULL items to an ADQL clause!");
	}

	/**
	 * Replaces the specified ADQL item by the given one.
	 *
	 * @param index	Position of the item to replace.
	 * @param item	Replacer of the specified ADQL item.
	 *
	 * @return	The replaced ADQL item.
	 *
	 * @throws NullPointerException				If the given item is NULL.
	 * @throws ArrayIndexOutOfBoundsException	If the index is out of range
	 *                                       	(index < 0 || index > size()).
	 */
	public T set(int index, T item) throws NullPointerException, ArrayIndexOutOfBoundsException {
		if (item != null) {
			T removed = list.set(index, item);
			position = null;
			return removed;
		} else
			throw new NullPointerException("It is impossible to replace an ADQL item by a NULL item into an ADQL clause!");
	}

	/**
	 * Gets the specified ADQL item.
	 *
	 * @param index	Index of the ADQL item to retrieve.
	 *
	 * @return	The corresponding ADQL item.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the index is out of range
	 *                                       	(index < 0 || index > size()).
	 */
	public T get(int index) throws ArrayIndexOutOfBoundsException {
		return list.get(index);
	}

	/**
	 * Removes the specified ADQL item.
	 *
	 * @param index	Index of the ADQL item to remove.
	 *
	 * @return	The removed ADQL item.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the index is out of range
	 *                                       	(index < 0 || index > size()).
	 */
	public T remove(int index) throws ArrayIndexOutOfBoundsException {
		T removed = list.remove(index);
		if (removed != null)
			position = null;
		return removed;
	}

	/**
	 * Clears this clause.
	 */
	public void clear() {
		list.clear();
		position = null;
	}

	/**
	 * Gets the length of this clause.
	 *
	 * @return	The number of ADQL items contained into this clause.
	 */
	public int size() {
		return list.size();
	}

	/**
	 * Tells whether this clause contains at least one ADQL item.
	 *
	 * @return	<code>true</code> if this clause is empty,
	 *        	<code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public final TextPosition getPosition() {
		return position;
	}

	/**
	 * Sets the position at which this {@link ADQLList} has been found in the
	 * original ADQL query string.
	 *
	 * @param position	Position of this {@link ADQLList}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position) {
		this.position = position;
	}

	@Override
	public String toADQL() {
		StringBuffer adql = new StringBuffer((getName() == null) ? "" : (getName() + " "));

		for(int i = 0; i < size(); i++) {
			if (i > 0)
				adql.append(" " + getSeparator(i) + " ");
			adql.append(get(i).toADQL());
		}

		return adql.toString();
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLListIterator(this);
	}

	/**
	 * Gets the list of all possible separators for this {@link ADQLList}.
	 *
	 * @return	Possible separators.
	 */
	public abstract String[] getPossibleSeparators();

	/**
	 * Gets the separator between the list items index-1 and index.
	 *
	 * @param index	Index of the right list item.
	 *
	 * @return	The corresponding separator.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the index is less or equal
	 *                                       	than 0,
	 *                                       	or is greater or equal than
	 *                                       	{@link ADQLList#size() size()}.
	 */
	public abstract String getSeparator(int index) throws ArrayIndexOutOfBoundsException;

	@Override
	public abstract ADQLObject getCopy() throws Exception;

	/**
	 * Lets iterating on all ADQL objects of the given {@link ADQLList}.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 06/2011
	 */
	public static class ADQLListIterator implements ADQLIterator {
		protected final ADQLList<ADQLObject> list;
		protected int index = -1;
		protected boolean removed = false;

		@SuppressWarnings("unchecked")
		public ADQLListIterator(ADQLList<? extends ADQLObject> lst) {
			list = (ADQLList<ADQLObject>)lst;
		}

		@Override
		public boolean hasNext() {
			return index + 1 < list.size();
		}

		@Override
		public ADQLObject next() {
			removed = false;
			return list.get(++index);
		}

		@Override
		public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
			if (index <= -1)
				throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called!");

			if (removed)
				throw new IllegalStateException("The remove() has already been called!");

			if (replacer == null)
				remove();
			else
				list.set(index, replacer);
		}

		@Override
		public void remove() {
			if (index <= -1)
				throw new IllegalStateException("remove() impossible: next() has not yet been called!");

			if (removed)
				throw new IllegalStateException("The remove() has already been called!");

			list.remove(index--);
			removed = true;
		}
	}

}
