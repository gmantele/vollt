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

import adql.query.ADQLList;
import adql.query.ADQLObject;

/**
 * <p>Defines an interface for any kind of search/replace handler.</p>
 * 
 * <p>
 * 	A replace handler is supposed to replace ADQL objects matching a given condition by another ADQL object (which may be generated on the fly if needed).
 * 	In some ADQL objects (i.e. {@link ADQLList}), it is also possible to remove objects. In this case, the replacement object must be <i>NULL</i>.
 * </p>
 * 
 * <p>
 * 	A simple implementation of this interface already exists: {@link SimpleReplaceHandler}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 * 
 * @see SimpleReplaceHandler
 */
public interface IReplaceHandler extends ISearchHandler {

	/**
	 * Searches all matching ADQL objects from the given ADQL object (included)
	 * and replaces them by their corresponding ADQL object.
	 * 
	 * @param startObj	The ADQL object from which the search must start.
	 */
	public void searchAndReplace(ADQLObject startObj);

	/**
	 * Gets the number of matched objects which have been successfully replaced.
	 * 
	 * @return	The number of replaced objects.
	 */
	public int getNbReplacement();

}
