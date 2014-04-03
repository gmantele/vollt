package uws.service.error;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.job.ErrorType;
import uws.job.user.JobOwner;

/**
 * Let's writing/formatting any Exception/Throwable in a {@link HttpServletResponse}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public interface ServiceErrorWriter {

	/**
	 * Writes the given exception in the given response.
	 * 
	 * @param t						Exception to write/format.
	 * @param response				Response in which the given exception must be written.
	 * @param request				Request at the origin of the error (MAY BE NULL).
	 * @param user					User which sends the given request (which generates the error) (MAY BE NULL).
	 * @param action				Type/Name of the action which generates the error (MAY BE NULL).
	 * 
	 * @throws IOException			If there is an error while writing the response.
	 */
	public void writeError(final Throwable t, final HttpServletResponse response, final HttpServletRequest request, final JobOwner user, final String action) throws IOException;

	/**
	 * Writes the described error in the given response.
	 * 
	 * @param message			Message to display.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
	 * @param response			Response in which the described error must be written.
	 * @param request			Request which causes this error.
	 * @param user				User which sends the HTTP request.
	 * @param action			Action corresponding to the given request.
	 * 
	 * @throws IOException		If there is an error while writing the response.
	 */
	public void writeError(final String message, final ErrorType type, final int httpErrorCode, final HttpServletResponse response, final HttpServletRequest request, final JobOwner user, final String action) throws IOException;

}
