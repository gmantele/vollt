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
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.job.ErrorSummary;
import uws.job.ErrorType;
import uws.job.UWSJob;
import uws.job.user.JobOwner;

/**
 * Let's writing/formatting any Exception/Throwable in an {@link HttpServletResponse} or in an error summary.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (09/2014)
 */
public interface ServiceErrorWriter {

	/**
	 * <p>Write the given exception in the given response.</p>
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
	 * <p>Write the described error in the given response.</p>
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

	/**
	 * <p>Write the given error in the given output stream.</p>
	 * 
	 * <p>
	 * 	This function is used only for the error summary of a job (that's to say to report in the
	 * 	../error/details parameter any error which occurs while executing a job).
	 * </p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	The error details written in the given output MUST always have the same MIME type.
	 * 	This latter MUST be returned by {@link #getErrorDetailsMIMEType()}.
	 * </i></p>
	 * 
	 * @param t				Error to write.	If <i>error</i> is not null, it will be displayed instead of the message of this throwable.
	 * @param error			Summary of the error. It may particularly contain a message different from the one of the given exception. In this case, it will displayed instead of the exception's message.
	 * @param job			The job which fails.
	 * @param output		Stream in which the error must be written.
	 * 
	 * @throws IOException	If there an error while writing the error in the given stream.
	 * 
	 * @see #getErrorDetailsMIMEType()
	 * 
	 * @since 4.1
	 */
	public void writeError(final Throwable t, final ErrorSummary error, final UWSJob job, final OutputStream output) throws IOException;

	/**
	 * <p>Get the MIME type of the error details written by {@link #writeError(UWSJob, Throwable, ErrorSummary, OutputStream)} in the error summary.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If NULL is returned, the MIME type will be considered as <i>text/plain</i>.
	 * </i></p>
	 * 
	 * @return	MIME type of the error details document. If NULL, it will be considered as text/plain.
	 * 
	 * @see #writeError(UWSJob, Throwable, ErrorSummary, OutputStream)
	 * 
	 * @since 4.1
	 */
	public String getErrorDetailsMIMEType();

}
