package adql.db.exception;

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

import java.util.ArrayList;
import java.util.Iterator;

import adql.db.DBChecker;
import adql.parser.ParseException;

/**
 * This exception is thrown by {@link DBChecker} when several columns, tables,
 * functions or even ADQL features do not exist. It lists several
 * {@link ParseException} (either {@link UnresolvedColumnException},
 * {@link UnresolvedTableException}, {@link UnresolvedFunctionException},
 * {@link UnresolvedJoinException} or {@link UnsupportedFeatureException}).
 *
 * <p>
 * 	Its message only tells the number of unresolved identifiers.
 * 	If you want to have more details about the position and the exact message of each exception, you just have to iterate
 * 	on this {@link UnresolvedIdentifiersException} (method {@link #iterator()}).
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 *
 * @see DBChecker
 */
public class UnresolvedIdentifiersException extends ParseException implements Iterable<ParseException> {
	private static final long serialVersionUID = 1L;

	/**
	 * Type of the unresolved/unsupported items listed in this exception.
	 *
	 * <i>
	 * <p><b>Examples:</b></p>
	 * <ul>
	 * 	<li><code>unresolved identifier</code> (default),</li>
	 * 	<li><code>unsupported expression</code></li>
	 * </ul>
	 * </i>
	 *
	 * @since 2.0 */
	protected final String itemNature;

	/** List of exceptions (one per unresolved identifier). */
	protected ArrayList<ParseException> exceptions;
	private String unresolvedIdentifiers = null;

	/**
	 * Build an empty {@link UnresolvedIdentifiersException} (that's to say:
	 * there is no unresolved identifier).
	 */
	public UnresolvedIdentifiersException() {
		this(null);
	}

	/**
	 * Build an empty {@link UnresolvedIdentifiersException} (that's to say:
	 * there is no unresolved identifier).
	 *
	 * @param itemNature	Type of the unresolved items listed in this
	 *                     	exception. <i>By default:
	 *                     	<code>unresolved identifier</code>.</i>
	 *
	 * @since 2.0
	 */
	public UnresolvedIdentifiersException(final String itemNature) {
		this.itemNature = (itemNature == null || itemNature.trim().isEmpty() ? "unresolved identifier" : itemNature);
		exceptions = new ArrayList<ParseException>();
	}

	/**
	 * Adds a {@link ParseException} (supposed to be either an {@link UnresolvedColumnException} or an {@link UnresolvedTableException}).
	 *
	 * @param pe	An exception.
	 */
	public final void addException(final ParseException pe) {
		if (pe != null) {
			exceptions.add(pe);
			if (pe instanceof UnresolvedColumnException) {
				String colName = ((UnresolvedColumnException)pe).getColumnName();
				if (colName != null && colName.trim().length() > 0)
					addIdentifierName(colName + " " + pe.getPosition());
			} else if (pe instanceof UnresolvedTableException) {
				String tableName = ((UnresolvedTableException)pe).getTableName();
				if (tableName != null && tableName.trim().length() > 0)
					addIdentifierName(tableName + " " + pe.getPosition());
			} else if (pe instanceof UnresolvedFunctionException) {
				String fctName = (((UnresolvedFunctionException)pe).getFunction() == null) ? null : ((UnresolvedFunctionException)pe).getFunction().getName() + "(...)";
				if (fctName != null && fctName.trim().length() > 0)
					addIdentifierName(fctName + " " + pe.getPosition());
			} else if (pe instanceof UnresolvedIdentifiersException)
				addIdentifierName(((UnresolvedIdentifiersException)pe).unresolvedIdentifiers);
		}
	}

	/**
	 * Adds the name (or the description) into the string list of all the unresolved identifiers.
	 *
	 * @param name	Name (or description) of the identifier to add.
	 */
	private final void addIdentifierName(final String name) {
		if (name != null && name.trim().length() > 0) {
			if (unresolvedIdentifiers == null)
				unresolvedIdentifiers = "";
			else
				unresolvedIdentifiers += ", ";
			unresolvedIdentifiers += name;
		}
	}

	/**
	 * Gets the number of unresolved identifiers.
	 *
	 * @return	The number of unresolved identifiers.
	 */
	public final int getNbErrors() {
		return exceptions.size();
	}

	/**
	 * Gets the list of all errors.
	 *
	 * @return	Errors list.
	 */
	public final Iterator<ParseException> getErrors() {
		return exceptions.iterator();
	}

	@Override
	public final Iterator<ParseException> iterator() {
		return getErrors();
	}

	/**
	 * Only tells how many identifiers have not been resolved.
	 *
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		StringBuffer buf = new StringBuffer();
		buf.append(exceptions.size()).append(" " + itemNature + "s").append(((unresolvedIdentifiers != null) ? (": " + unresolvedIdentifiers) : "")).append('!');
		for(ParseException pe : exceptions)
			buf.append("\n  - ").append(pe.getMessage());
		return buf.toString();
	}

}
