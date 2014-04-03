package uws.job.parameters;

/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import uws.UWSException;

/**
 * <p>Lets controlling an input parameter of a UWS job.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 */
public interface InputParamController {

	/**
	 * Returns the default value of the controlled parameter.
	 * 
	 * @return	Default value (<i>null</i> is allowed).
	 */
	public Object getDefault();

	/**
	 * <p>Checks the value of the controlled parameter and builds a new object from this value.</p>
	 * 
	 * @param value				Parameter value to check.
	 * @return					The same value or a new object built from the given value.
	 * @throws UWSException		If the given value is strongly incorrect.
	 */
	public Object check(final Object value) throws UWSException;

	/**
	 * Tells whether the controlled parameter may be modified after initialization.
	 * 
	 * @return	<i>true</i> to allow the modification, <i>false</i> otherwise.
	 */
	public boolean allowModification();

}
