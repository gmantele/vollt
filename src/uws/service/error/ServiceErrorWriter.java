package uws.service.error;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.job.ErrorType;
import uws.job.user.JobOwner;

/**
 * Let's writing/formatting any Exception/Throwable in a {@link HttpServletResponse}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (09/2014)
 */
public interface ServiceErrorWriter {

	/**
	 * <p>Writes the given exception in the given response.</p>
	 * 
	 * <p><i>Note:
	 * 	If this function is called without at least an exception and an HTTP response, nothing should be done.
	 * 	No error may be thrown.
	 * </i></p>
	 * 
	 * @param t					Exception to write/format.
	 * @param response			Response in which the given exception must be written.
	 * @param request			Request at the origin of the error (MAY BE NULL).
	 * @param reqID				ID of the request (which let the user and the administrator identify the failed request). (MAY BE NULL if the request is not provided)
	 * @param user				User which sends the given request (which generates the error) (MAY BE NULL).
	 * @param action			Type/Name of the action which generates the error (MAY BE NULL).
	 * 
	 * @throws IOException		If there is an error while writing the response.
	 */
	public void writeError(final Throwable t, final HttpServletResponse response, final HttpServletRequest request, final String reqID, final JobOwner user, final String action) throws IOException;

	/**
	 * <p>Writes the described error in the given response.</p>
	 * 
	 * <p><i>Note:
	 * 	If this function is called without at least a message and an HTTP response, nothing should be done.
	 * 	No error may be thrown.
	 * </i></p>
	 * 
	 * @param message			Message to display.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
	 * @param response			Response in which the described error must be written.
	 * @param request			Request which causes this error.
	 * @param reqID				ID of the request (which let the user and the administrator identify the failed request).
	 * @param user				User which sends the HTTP request.
	 * @param action			Action corresponding to the given request.
	 * 
	 * @throws IOException		If there is an error while writing the response.
	 */
	public void writeError(final String message, final ErrorType type, final int httpErrorCode, final HttpServletResponse response, final HttpServletRequest request, final String reqID, final JobOwner user, final String action) throws IOException;

}
