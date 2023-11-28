package adql.db;

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

/**
 * State of the {@link DBChecker} at one recursion level inside an ADQL query.
 *
 * <p>
 * 	An instance of this class aims to list columns and Common Table Expressions
 * 	(i.e. CTE - temporary tables defined in the WITH clause) available inside
 * 	a specific ADQL (sub-)query.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (10/2019)
 * @since 2.0
 */
public class CheckContext {

	/** List of available CTEs at this level. */
	public final SearchTableApi cteTables;

	/** List of available columns (of all tables). */
	public final SearchColumnList availableColumns;

	/**
	 * Create a context with the given list of CTEs and columns.
	 *
	 * @param cteTables	All available CTEs.
	 *                 	<i>Replaced by an empty list, if NULL.</i>
	 * @param columns	All available columns.
	 *                 	<i>Replaced by an empty list, if NULL.</i>
	 */
	public CheckContext(final SearchTableApi cteTables, final SearchColumnList columns) {
		this.cteTables = (cteTables == null ? new SearchTableList() : cteTables);
		this.availableColumns = (columns == null ? new SearchColumnList() : columns);
	}

	/**
	 * Create a deep copy of this context.
	 *
	 * @return	Deep copy.
	 */
	public CheckContext getCopy() {
		return new CheckContext(cteTables.getCopy(), new SearchColumnList(availableColumns));
	}

}
