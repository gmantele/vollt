package uws.service.request;

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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;
import uws.job.parameters.InputParamController;
import uws.job.parameters.UWSParameters;

/**
 * <p>This parser lets extract parameters from an {@link HttpServletRequest}.
 * 
 * <p>
 * 	These parameters can be indeed provided in several ways. Among these ways,
 * 	application/x-www-form-urlencoded and multipart/form-data are the most famous.
 * 	Both are already fully supported by the UWS library by default in {@link UWSParameters}.
 * </p>
 * 
 * <p><b>IMPORTANT:
 * 	A {@link RequestParser} extension MUST NOT be used to check the parameters' value.
 * 	It only aims to parse an {@link HttpServletRequest} in order to extract parameters.
 * </b></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.1 (11/2014)
 * @since 4.1
 * 
 * @see UWSParameters
 */
public interface RequestParser {

	/**
	 * <p>Extract parameters from the given HTTP request.</p>
	 * 
	 * <p>
	 * 	These parameters can be fetched from {@link HttpServletRequest#getParameterMap()}
	 * 	or directly from the full request content. In this last case, a parsing is necessary ;
	 * 	hence this function.
	 * </p>
	 * 
	 * <p>
	 * 	In case a parameter is provided several times with the same time and the same case,
	 * 	the request parser can choose to keep only the last occurrence or all occurrences.
	 * 	If all occurrences are kept, this function MUST return an array of {@link Object}s
	 * 	(in which types may be mixed), otherwise a map value MUST be an elementary object.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	A parameter item can be a simple value (e.g. String, integer, ...)
	 * 	or a more complex object (e.g. File, InputStream, ...).
	 * </i></p>
	 * 
	 * <p><b>IMPORTANT:</b>
	 * 	This function MUST NOT be used to check the parameters' value.
	 * 	It only aims to parse the given request in order to extract its embedded parameters.
	 * 	<br/>
	 * 	<b>Consequently, if this function throws an exception, it could be only because the request
	 * 	can not be read, and not because a parameter format or value is incorrect.</b>
	 * 	<br/>
	 * 	Parameter checks should be done in {@link UWSParameters} and more particularly by
	 * 	an {@link InputParamController}.
	 * </b></p>
	 * 
	 * @param request	An HTTP request.
	 * 
	 * @return	A map listing all extracted parameters. Values are either an elementary object (whatever is the type),
	 *        	or an array of {@link Object}s (in which types can be mixed).
	 * 
	 * @throws UWSException	If any error provides this function to read the parameters.
	 */
	public Map<String,Object> parse(final HttpServletRequest request) throws UWSException;

}
