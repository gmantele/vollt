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
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import tap.TAPException;
import uws.AcceptHeader;
import uws.UWSException;
import uws.UWSToolBox;
import uws.job.ErrorSummary;
import uws.job.ErrorType;
import uws.job.UWSJob;
import uws.job.serializer.UWSSerializer;
import uws.job.user.JobOwner;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Default implementation of a {@link ServiceErrorWriter} interface for a UWS service.</p>
 * 
 * <p>
 * 	All errors are written using the function {@link #formatError(String, ErrorType, int, String, String, JobOwner, HttpServletResponse, String)}
 * 	in order to format the error in the most appropriate format. 2 formats are managed by default by this implementation: HTML (default) and JSON.
 * 	This format is chosen thanks to the "Accept" header of the HTTP request. If no request is provided or if there is no known format,
 * 	the HTML format is chosen by default.
 * </p>
 * 
 * <p>
 * 	{@link UWSException}s may precise the HTTP error code to apply,
 * 	which will be used to set the HTTP status of the response. If it is a different kind of exception,
 * 	the HTTP status 500 (INTERNAL SERVER ERROR) will be used.
 * </p>
 * 
 * <p>
 * 	Besides, all exceptions except {@link UWSException} and {@link TAPException} will be logged as FATAL in the TAP context
 * 	(with no event and no object). Thus the full stack trace is available to the administrator so that the error can
 * 	be understood as easily and quickly as possible.
 * 	<i>The stack trace is no longer displayed to the user.</i>
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (09/2014)
 */
public class DefaultUWSErrorWriter implements ServiceErrorWriter {

	/** List of all managed output formats. */
	protected final String[] managedFormats = new String[]{"application/json","json","text/json","text/html","html"};

	/** Logger to use when grave error must be logged or if a JSON error occurs. */
	protected final UWSLog logger;

	/**
	 * Build an error writer which will log any error in response of an HTTP request.
	 * 
	 * @param logger	Object to use to log errors.
	 */
	public DefaultUWSErrorWriter(final UWSLog logger){
		if (logger == null)
			throw new NullPointerException("Missing logger! Can not write a default error writer without.");

		this.logger = logger;
	}

	@Override
	public void writeError(Throwable t, HttpServletResponse response, HttpServletRequest request, String reqID, JobOwner user, String action) throws IOException{
		if (t == null || response == null)
			return;

		// If expected error, just write it:
		if (t instanceof UWSException){
			UWSException ue = (UWSException)t;
			writeError(ue.getMessage(), ue.getUWSErrorType(), ue.getHttpErrorCode(), response, request, reqID, user, action);
		}
		// Otherwise, log it and write a message to the user:
		else{
			// log the error as GRAVE/FATAL (because unexpected/unmanaged):
			logger.logUWS(LogLevel.FATAL, null, null, "[REQUEST N°" + reqID + "] " + t.getMessage(), t);
			// write a message to the user:
			writeError("INTERNAL SERVER ERROR! Sorry, this error is unexpected and no explaination can be provided for the moment. Details about this error have been reported in the service log files ; you should try again your request later or notify the administrator(s) by yourself (with the following 'Request ID').", ErrorType.FATAL, UWSException.INTERNAL_SERVER_ERROR, response, request, reqID, user, action);
		}
	}

	@Override
	public void writeError(String message, ErrorType type, int httpErrorCode, HttpServletResponse response, HttpServletRequest request, String reqID, JobOwner user, String action) throws IOException{
		if (message != null || response != null)
			return;

		// Just format and write the error message:
		formatError(message, type, httpErrorCode, reqID, action, user, response, (request != null) ? request.getHeader("Accept") : null);
	}

	@Override
	public void writeError(Throwable t, ErrorSummary error, UWSJob job, OutputStream output) throws IOException{
		UWSToolBox.writeErrorFile((t instanceof Exception) ? (Exception)t : new UWSException(t), error, job, output);
	}

	@Override
	public String getErrorDetailsMIMEType(){
		return "text/plain";
	}

	/**
	 * Parses the header "Accept", splits it in a list of MIME type and compare each one to each managed formats ({@link #managedFormats}).
	 * If there is a match (not case sensitive), return the corresponding managed format immediately.
	 * 
	 * @param acceptHeader	The header item named "Accept" (which lists all expected response formats).
	 * @return				The first format common to the "Accept" header and the managed formats of this writer.
	 */
	protected final String chooseFormat(final String acceptHeader){
		if (acceptHeader != null && !acceptHeader.trim().isEmpty()){
			// Parse the given MIME types list:
			AcceptHeader accept = new AcceptHeader(acceptHeader);
			ArrayList<String> lstMimeTypes = accept.getOrderedMimeTypes();
			for(String acceptedFormat : lstMimeTypes){
				for(String f : managedFormats){
					if (acceptedFormat.equalsIgnoreCase(f))
						return f;
				}
			}
		}
		return null;
	}

	/**
	 * <p>Formats and writes the given error in the HTTP servlet response.</p>
	 * <p>The format is chosen thanks to the Accept header of the HTTP request.
	 * If unknown, the HTML output is chosen.</p>
	 * 
	 * @param t					Exception to format and to write.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
	 * @param reqID				ID of the request at the origin of the specified error.
	 * @param action			Action which generates the error <i><u>note:</u> displayed only if not NULL and not empty.
	 * @param user				User which is at the origin of the request/action which generates the error.
	 * @param response			Response in which the error must be written.
	 * @param acceptHeader		Value of the header named "Accept" (which lists all allowed response format).
	 * 
	 * @throws IOException		If there is an error while writing the given exception.
	 * 
	 * @see #formatHTMLError(Throwable, boolean, ErrorType, int, String, JobOwner, HttpServletResponse)
	 * @see #formatJSONError(Throwable, boolean, ErrorType, int, String, JobOwner, HttpServletResponse)
	 */
	protected void formatError(final String message, final ErrorType type, final int httpErrorCode, final String reqID, final String action, final JobOwner user, final HttpServletResponse response, final String acceptHeader) throws IOException{
		// Reset the whole response to ensure the output stream is free:
		if (response.isCommitted())
			return;
		response.reset();

		String format = chooseFormat(acceptHeader);
		if (format != null && (format.equalsIgnoreCase("application/json") || format.equalsIgnoreCase("text/json") || format.equalsIgnoreCase("json")))
			formatJSONError(message, type, httpErrorCode, reqID, action, user, response);
		else
			formatHTMLError(message, type, httpErrorCode, reqID, action, user, response);
	}

	/**
	 * <p>Formats and writes the given error in the HTTP servlet response.</p>
	 * <p>A full HTML response is printed with: the HTTP error code, the error type, the name of the exception, the message and the full stack trace.</p>
	 * 
	 * @param t					Exception to format and to write.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
	 * @param reqID				ID of the request at the origin of the specified error.
	 * @param action			Action which generates the error <i><u>note:</u> displayed only if not NULL and not empty.
	 * @param user				User which is at the origin of the request/action which generates the error.
	 * @param response			Response in which the error must be written.
	 * 
	 * @throws IOException		If there is an error while writing the given exception.
	 */
	protected void formatHTMLError(final String message, final ErrorType type, final int httpErrorCode, final String reqID, final String action, final JobOwner user, final HttpServletResponse response) throws IOException{
		// Erase anything written previously in the HTTP response:
		response.reset();

		// Set the HTTP status code and the content type of the response:
		response.setStatus(httpErrorCode);
		response.setContentType(UWSSerializer.MIME_TYPE_HTML);

		PrintWriter out = response.getWriter();

		// Header:
		out.println("<html>\n\t<head>");
		out.println("\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
		out.println("\t\t<style type=\"text/css\">");
		out.println("\t\t\tbody { background-color: white; color: black; }");
		out.println("\t\t\th2 { font-weight: bold; font-variant: small-caps; text-decoration: underline; font-size: 1.5em; color: #4A4A4A; }");
		out.println("\t\t\tul, ol { margin-left: 2em; margin-top: 0.2em; text-align: justify; }");
		out.println("\t\t\tli { margin-bottom: 0.2em; margin-top: 0; }");
		out.println("\t\t\tp, p.listheader { text-align: justify; text-indent: 2%; margin-top: 0; }");
		out.println("\t\t\ttable { border-collapse: collapse; }");
		out.println("\t\t\ttable, th, td { border: 1px solid #FC8813; }");
		out.println("\t\t\tth { background-color: #F29842; color: white; font-size: 1.1em; }");
		out.println("\t\t\ttr.alt { background-color: #FFDAB6; }");
		out.println("\t\t</style>");
		out.println("\t\t<title>SERVICE ERROR</title>");
		out.println("\t</head>\n\t<body>");

		// Title:
		String errorColor = (type == ErrorType.FATAL) ? "red" : "orange";
		out.println("\t\t<h1 style=\"text-align: center; background-color:" + errorColor + "; color: white; font-weight: bold;\">SERVICE ERROR - " + httpErrorCode + "</h1>");

		// Description part:
		out.println("\t\t<h2>Description</h2>");
		out.println("\t\t<ul>");
		out.println("\t\t\t<li><b>Type: </b>" + type + "</li>");
		if (reqID != null)
			out.println("\t\t\t<li><b>Request ID: </b>" + reqID + "</li>");
		if (action != null)
			out.println("\t\t\t<li><b>Action: </b>" + action + "</li>");
		out.println("\t\t\t<li><b>Message:</b><p>" + message + "</p></li>");
		out.println("\t\t</ul>");

		out.println("\t</body>\n</html>");
		out.close();
	}

	/**
	 * <p>Formats and writes the given error in the HTTP servlet response.</p>
	 * <p>A JSON response is printed with: the HTTP error code, the error type, the name of the exception, the message and the list of all causes' message.</p>
	 * 
	 * @param t					Exception to format and to write.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
	 * @param reqID				ID of the request at the origin of the specified error.
	 * @param action			Action which generates the error <i><u>note:</u> displayed only if not NULL and not empty.
	 * @param user				User which is at the origin of the request/action which generates the error.
	 * @param response			Response in which the error must be written.
	 * 
	 * @throws IOException		If there is an error while writing the given exception.
	 */
	protected void formatJSONError(final String message, final ErrorType type, final int httpErrorCode, final String reqID, final String action, final JobOwner user, final HttpServletResponse response) throws IOException{
		// Erase anything written previously in the HTTP response:
		response.reset();

		// Set the HTTP status code and the content type of the response:
		response.setStatus(httpErrorCode);
		response.setContentType(UWSSerializer.MIME_TYPE_JSON);

		PrintWriter out = response.getWriter();
		try{
			JSONWriter json = new JSONWriter(out);

			json.object();
			json.key("errorcode").value(httpErrorCode);
			json.key("errortype").value(type.toString());
			if (reqID != null)
				json.key("requestid").value(reqID);
			if (action != null)
				json.key("action").value(action);
			json.key("message").value(message);

			json.endObject();
		}catch(JSONException je){
			logger.logUWS(LogLevel.ERROR, null, "FORMAT_ERROR", "Impossible to format/write an error in JSON!", je);
			throw new IOException("Error while formatting the error in JSON!", je);
		}finally{
			out.flush();
			out.close();
		}
	}

}
