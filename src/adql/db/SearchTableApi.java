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
 * Copyright 2017-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.List;

import adql.query.from.ADQLTable;

/**
 * Simple interface about a class which allows to search for a specified
 * {@link ADQLTable}.
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 2.0 (09/2019)
 * @since 1.4
 *
 * @see SearchTableList
 */
public interface SearchTableApi {

	/**
	 * Searches all {@link DBTable} elements corresponding to the given {@link ADQLTable} (case insensitive).
	 *
	 * @param table	An {@link ADQLTable}.
	 *
	 * @return		The list of all corresponding {@link DBTable} elements.
	 */
	public List<DBTable> search(final ADQLTable table);

	/**
	 * Adds the given object at the end of this list.
	 *
	 * @param obj	Object to add (different from NULL).
	 *
	 * @throws NullPointerException		If the given object or its extracted key is <code>null</code>.
	 * @throws IllegalArgumentException	If the extracted key is already used by another object in this list.
	 *
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	public boolean add(final DBTable item);

	public SearchTableApi getCopy();

}