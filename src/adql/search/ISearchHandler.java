package adql.search;

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

import java.util.Iterator;

import adql.query.ADQLObject;

/**
 * <p>Defines an interface for any kind of search handler.</p>
 * 
 * <p>
 * 	A search handler is supposed to search ADQL objects matching a given condition.
 * 	Then, it lets iterate on all matched items.
 * </p>
 * 
 * <p>
 * 	A simple implementation of this interface already exists: {@link SimpleSearchHandler}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 * 
 * @see IReplaceHandler
 * @see SimpleSearchHandler
 */
public interface ISearchHandler extends Iterable<ADQLObject> {

	/**
	 * Searches all matching ADQL objects from the given ADQL object (included).
	 * 
	 * @param startObj	The ADQL object from which the search must start.
	 */
	public void search(ADQLObject startObj);

	/**
	 * Lets to iterate on the list of all the matched ADQL objects.
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<ADQLObject> iterator();

	/**
	 * Indicates how many ADQL objects have matched.
	 * 
	 * @return	The number of all the matched ADQL objects.
	 */
	public int getNbMatch();

}
