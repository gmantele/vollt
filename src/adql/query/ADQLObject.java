package adql.query;

import adql.search.ISearchHandler;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * <p>This class gathers all main behaviors of any ADQL object (query, clause, columns, condition, etc...):
 * <ul>
 * 	<li>to have a name in ADQL</i>
 * 	<li>to be written in ADQL</li>
 * 	<li>to offer a way to search any ADQL item <i>(included itself)</i>.</li>
 * </ul>
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 */
public interface ADQLObject {

	/**
	 * Gets the name of this object in ADQL.
	 * 
	 * @return	The name of this ADQL object.
	 */
	public String getName();

	/**
	 * Gets the ADQL expression of this object.
	 * 
	 * @return	The corresponding ADQL expression.
	 */
	public String toADQL();

	/**
	 * Gets a (deep) copy of this ADQL object.
	 * 
	 * @return					The copy of this ADQL object.
	 * 
	 * @throws Exception 		If there is any error during the copy.
	 */
	public ADQLObject getCopy() throws Exception;

	/**
	 * <p>Gets an iterator on the intern ADQL objects.</p>
	 * <p><i>
	 * 	<u>Note:</u>The returned iterator is particularly used by a {@link ISearchHandler}
	 * 	extension to browse a whole ADQL tree.
	 * </i></p>
	 * 
	 * @return	An ADQL objects iterator.
	 * 
	 * @see ADQLIterator
	 * @see ISearchHandler
	 */
	public ADQLIterator adqlIterator();

}
