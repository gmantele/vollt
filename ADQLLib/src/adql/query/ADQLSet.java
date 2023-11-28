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

import java.util.Iterator;

import adql.db.DBColumn;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.search.ISearchHandler;

/**
 * Object representation of an ADQL's rows set.
 *
 * <p>
 * 	The resulting object of the {@link ADQLParser} is an object of this class.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2022)
 * @since 2.0
 */
public abstract class ADQLSet implements ADQLObject {

	/** Version of the ADQL grammar in which this query is written. */
	protected final ADQLVersion adqlVersion;

	/** The ADQL clause WITH. */
	protected ClauseADQL<WithItem> with;

	/** The ADQL clause ORDER BY. */
	protected ClauseADQL<ADQLOrder> orderBy;

	/** The ADQL clause OFFSET. */
	protected ClauseOffset offset;

	/** Position of this Query (or sub-query) inside the whole given ADQL query
	 * string. */
	protected TextPosition position = null;

	/**
	 * Builds an empty ADQL set.
	 */
	public ADQLSet() {
		this(ADQLParser.DEFAULT_VERSION);
	}

	/**
	 * Builds an empty ADQL set following the specified ADQL grammar.
	 *
	 * @param version	Followed version of the ADQL grammar.
	 *               	<i>If NULL, the
	 *               	{@link adql.parser.ADQLParser#DEFAULT_VERSION default version}
	 *               	will be set.</i>
	 */
	public ADQLSet(final ADQLVersion version) {
		this.adqlVersion = (version == null ? ADQLParser.DEFAULT_VERSION : version);
		with = new ClauseADQL<WithItem>("WITH");
		orderBy = new ClauseADQL<ADQLOrder>("ORDER BY");
		offset = null;
	}

	/**
	 * Builds an ADQL set by copying the given one.
	 *
	 * @param toCopy	The ADQL set to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public ADQLSet(final ADQLSet toCopy) throws Exception {
		adqlVersion = toCopy.adqlVersion;
		with = (ClauseADQL<WithItem>)toCopy.with.getCopy();
		orderBy = (ClauseADQL<ADQLOrder>)toCopy.orderBy.getCopy();
		offset = (ClauseOffset)toCopy.offset.getCopy();
		position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
	}

	/**
	 * Get the followed version of the ADQL grammar.
	 *
	 * @return	The followed ADQL grammar version.
	 */
	public final ADQLVersion getADQLVersion() {
		return adqlVersion;
	}

	/**
	 * Clear all the clauses.
	 */
	public void reset() {
		with.clear();
		orderBy.clear();
		offset = null;
		position = null;
	}

	/**
	 * Gets the WITH clause of this query.
	 *
	 * @return	Its WITH clause.
	 */
	public final ClauseADQL<WithItem> getWith() {
		return with;
	}

	/**
	 * Replaces its WITH clause by the given one.
	 *
	 * <p><i><b>Note:</b>
	 * 	The position of the query is erased.
	 * </i></p>
	 *
	 * @param newWith	The new WITH clause.
	 *
	 * @throws NullPointerException	If the given WITH clause is NULL.
	 */
	public void setWith(ClauseADQL<WithItem> newWith) throws NullPointerException {
		if (newWith == null)
			throw new NullPointerException("Impossible to replace the WITH clause of a rows set by NULL!");
		else
			with = newWith;
		position = null;
	}

	/**
	 * Gets the ORDER BY clause of this rows set.
	 *
	 * @return	Its ORDER BY clause.
	 */
	public final ClauseADQL<ADQLOrder> getOrderBy() {
		return orderBy;
	}

	/**
	 * Replaces its ORDER BY clause by the given one.
	 *
	 * <p><i><b>Note:</b>
	 * 	The position inside the query is erased.
	 * </i></p>
	 *
	 * @param newOrderBy	The new ORDER BY clause.
	 *
	 * @throws NullPointerException	If the given ORDER BY clause is NULL.
	 */
	public void setOrderBy(ClauseADQL<ADQLOrder> newOrderBy) throws NullPointerException {
		if (newOrderBy == null)
			orderBy.clear();
		else
			orderBy = newOrderBy;
		position = null;
	}

	/**
	 * Indicates whether this rows set imposes a maximum number of rows.
	 *
	 * @return	<code>true</code> this clause has a limited number of rows,
	 *        	<code>false</code> otherwise.
	 */
	public abstract boolean hasLimit();

	/**
	 * Gets the maximum number of rows imposed in this rows set.
	 *
	 * @return	Maximum number of rows the query must return.
	 */
	public abstract int getLimit();

	/**
	 * Sets no maximum number of rows returned in this rows set.
	 */
	public abstract void setNoLimit();

	/**
	 * Changes the maximum number of rows returned in this rows set.
	 *
	 * @param limit	The maximum number of returned rows.
	 */
	public abstract void setLimit(int limit);

	/**
	 * Gets the OFFSET value of this rows set.
	 *
	 * @return	Its OFFSET value,
	 *        	or NULL if not OFFSET is set.
	 */
	public final ClauseOffset getOffset() {
		return offset;
	}

	/**
	 * Replaces its OFFSET value by the given one.
	 *
	 * <p><i><b>Note:</b>
	 * 	The position inside the query is erased.
	 * </i></p>
	 *
	 * @param newOffset	The new OFFSET value,
	 *                 	or NULL to remove the current OFFSET.
	 */
	public void setOffset(final ClauseOffset newOffset) {
		offset = newOffset;
		position = null;
	}

	@Override
	public TextPosition getPosition() {
		return position;
	}

	/**
	 * Set the position of this {@link ADQLSet} inside the whole given ADQL
	 * query string.
	 *
	 * @param position New position of this {@link ADQLSet}.
	 */
	public final void setPosition(final TextPosition position) {
		this.position = position;
	}

	public abstract DBColumn[] getResultingColumns();

	/**
	 * Lets searching ADQL objects into this rows set thanks to the given
	 * search handler.
	 *
	 * @param sHandler	A search handler.
	 *
	 * @return An iterator on all ADQL objects found.
	 */
	public Iterator<ADQLObject> search(ISearchHandler sHandler) {
		sHandler.search(this);
		return sHandler.iterator();
	}

}
