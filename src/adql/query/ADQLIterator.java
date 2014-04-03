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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.Iterator;

/**
 * <p>Lets iterating on all ADQL objects inside any ADQL object.</p>
 * 
 * <p>
 * 	Any class implementing {@link ADQLObject} has the function {@link ADQLObject#adqlIterator()}.
 * 	This function must return an instance of this class so that being able to iterate on all ADQL object inside itself.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 * 
 * @see ADQLObject#adqlIterator()
 */
public interface ADQLIterator extends Iterator<ADQLObject> {

	/**
	 * Replaces the current ADQL object by the given ADQL object. This method can be called only one time per call to <i>next</i>.
	 * 
	 * @param replacer							The ADQL object which has to replace the current object.
	 * 
	 * @throws UnsupportedOperationException	If the <i>replace</i> operation is not supported by this ADQLIterator.
	 * @throws IllegalStateException			If the <i>next</i> method has not yet been called, or the <i>replace</i> method has already been called after the last call to the <i>next</i> method.
	 */
	public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException;

}
