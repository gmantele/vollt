package uws.service.error;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import uws.AcceptHeader;
import uws.job.ErrorType;
import uws.job.serializer.UWSSerializer;
import uws.job.user.JobOwner;
import uws.service.log.UWSLog;

/**
 * <p>Abstract implementation of the {@link ServiceErrorWriter} interface.</p>
 * 
 * <p>
 * 	The only abstract method is the function {@link #getLogger()}. It MUST return a NON-NULL logger.
 * 	The other functions ({@link #writeError(Throwable, HttpServletResponse, HttpServletRequest, JobOwner, String)}
 * 	and {@link #writeError(String, ErrorType, int, HttpServletResponse, HttpServletRequest, JobOwner, String)}) have
 * 	already a default implementation but may be overridden if needed. Both of them call the function
 * 	{@link #formatError(Throwable, boolean, ErrorType, int, String, JobOwner, HttpServletResponse)}
 * 	to format and write the error in the given {@link HttpServletResponse} in the HTML format with
 * 	the appropriate HTTP error code. The  (full) stack trace of the error may be printed if asked.
 * </p>
 * 
 * <p>2 formats are managed by this implementation: HTML (default) and JSON. That means the writer will format and
 * write a given error in the best appropriate format. This format is chosen thanks to the "Accept" header of the HTTP request.
 * If no request is provided or if there is no known format, the HTML format is chosen by default.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public abstract class AbstractServiceErrorWriter implements ServiceErrorWriter {

	protected final String[] managedFormats = new String[]{"application/json","json","text/json","text/html","html"};

	/**
	 * Logger to use to display the given errors in the appropriate log files.
	 * @return	A NON-NULL and VALID logger.
	 */
	protected abstract UWSLog getLogger();

	@Override
	public void writeError(Throwable t, HttpServletResponse response, HttpServletRequest request, JobOwner user, String action) throws IOException{
		if (t != null && response != null){
			formatError(t, true, ErrorType.FATAL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, action, user, response, (request != null) ? request.getHeader("Accept") : null);
			getLogger().error(t);
			String errorMsg = t.getMessage();
			if (errorMsg == null || errorMsg.trim().isEmpty())
				errorMsg = t.getClass().getName() + " (no error message)";
			getLogger().httpRequest(request, user, action, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg, t);
		}
	}

	@Override
	public void writeError(String message, ErrorType type, int httpErrorCode, HttpServletResponse response, HttpServletRequest request, JobOwner user, String action) throws IOException{
		if (message != null && response != null){
			formatError(new Exception(message), false, type, httpErrorCode, action, user, response, (request != null) ? request.getHeader("Accept") : null);
			getLogger().httpRequest(request, user, action, httpErrorCode, message, null);
		}
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
	 * @param printStackTrace	<i>true</i> to print the (full) stack trace, <i>false</i> otherwise.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
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
	protected void formatError(final Throwable t, final boolean printStackTrace, final ErrorType type, final int httpErrorCode, final String action, final JobOwner user, final HttpServletResponse response, final String acceptHeader) throws IOException{
		// Reset the whole response to ensure the output stream is free:
		if (response.isCommitted())
			return;
		response.reset();

		String format = chooseFormat(acceptHeader);
		if (format != null && (format.equalsIgnoreCase("application/json") || format.equalsIgnoreCase("text/json") || format.equalsIgnoreCase("json")))
			formatJSONError(t, printStackTrace, type, httpErrorCode, action, user, response);
		else
			formatHTMLError(t, printStackTrace, type, httpErrorCode, action, user, response);
	}

	/**
	 * <p>Formats and writes the given error in the HTTP servlet response.</p>
	 * <p>A full HTML response is printed with: the HTTP error code, the error type, the name of the exception, the message and the full stack trace.</p>
	 * 
	 * @param t					Exception to format and to write.
	 * @param printStackTrace	<i>true</i> to print the (full) stack trace, <i>false</i> otherwise.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
	 * @param action			Action which generates the error <i><u>note:</u> displayed only if not NULL and not empty.
	 * @param user				User which is at the origin of the request/action which generates the error.
	 * @param response			Response in which the error must be written.
	 * 
	 * @throws IOException		If there is an error while writing the given exception.
	 */
	protected void formatHTMLError(final Throwable t, final boolean printStackTrace, final ErrorType type, final int httpErrorCode, final String action, final JobOwner user, final HttpServletResponse response) throws IOException{
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
		if (action != null && !action.trim().isEmpty())
			out.println("\t\t\t<li><b>Action: </b>" + action + "</li>");
		String context = null;
		String msg = t.getMessage();
		if (msg != null && !msg.trim().isEmpty()){
			int start = msg.indexOf("["), end = msg.indexOf("]");
			if (start >= 0 && start < end){
				context = msg.substring(start + 1, end);
				msg = msg.substring(end + 1);
			}
		}else
			msg = "";
		if (context != null)
			out.println("\t\t\t<li><b>Context: </b>" + context + "</li>");
		if (printStackTrace)
			out.println("\t\t\t<li><b>Exception: </b>" + t.getClass().getName() + "</li>");
		out.println("\t\t\t<li><b>Message:</b><p>" + msg + "</p></li>");
		out.println("\t\t</ul>");

		// Stack trace part:
		if (printStackTrace){
			out.println("\t\t<h2>Stack trace</h2>");
			Throwable cause = t;
			do{
				out.println("\t\t<table style=\"width: ihnerit;\">");
				out.println("\t\t\t<tr><th>Class</th><th>Method</th><th>Line</th></tr>");
				StackTraceElement[] trace = cause.getStackTrace();
				for(int i = 0; i < trace.length; i++)
					out.println("\t\t\t<tr" + ((i % 2 != 0) ? " class=\"alt\"" : "") + "><td>" + trace[i].getClassName() + "</td><td>" + trace[i].getMethodName() + "</td><td>" + trace[i].getLineNumber() + "</td></tr>");
				out.println("\t\t</table>");

				// Print the stack trace of the "next" error:
				cause = cause.getCause();
				if (cause != null){
					out.println("\t\t<p><b>Caused by " + cause.getClass().getName() + ":</b></p>");
					out.println("\t\t<p>" + cause.getMessage() + "</p>");
				}
			}while(cause != null);
		}

		out.println("\t</body>\n</html>");
		out.close();
	}

	/**
	 * <p>Formats and writes the given error in the HTTP servlet response.</p>
	 * <p>A JSON response is printed with: the HTTP error code, the error type, the name of the exception, the message and the list of all causes' message.</p>
	 * 
	 * @param t					Exception to format and to write.
	 * @param printStackTrace	<i>true</i> to print the (full) stack trace, <i>false</i> otherwise.
	 * @param type				Type of the error: FATAL or TRANSIENT.
	 * @param httpErrorCode		HTTP error code (i.e. 404, 500).
	 * @param action			Action which generates the error <i><u>note:</u> displayed only if not NULL and not empty.
	 * @param user				User which is at the origin of the request/action which generates the error.
	 * @param response			Response in which the error must be written.
	 * 
	 * @throws IOException		If there is an error while writing the given exception.
	 */
	protected void formatJSONError(final Throwable t, final boolean printStackTrace, final ErrorType type, final int httpErrorCode, final String action, final JobOwner user, final HttpServletResponse response) throws IOException{
		// Set the HTTP status code and the content type of the response:
		response.setStatus(httpErrorCode);
		response.setContentType(UWSSerializer.MIME_TYPE_JSON);

		PrintWriter out = response.getWriter();
		try{
			JSONWriter json = new JSONWriter(out);

			json.object();
			json.key("errorcode").value(httpErrorCode);
			json.key("errortype").value(type.toString());
			json.key("action").value(action);

			String context = null;
			String msg = t.getMessage();
			if (msg != null && !msg.trim().isEmpty()){
				int start = msg.indexOf("["), end = msg.indexOf("]");
				if (start >= 0 && start < end){
					context = msg.substring(start + 1, end);
					msg = msg.substring(end + 1);
				}
			}else
				msg = "";
			if (context != null)
				json.key("context").value(context);
			if (printStackTrace)
				json.key("exception").value(t.getClass().getName());
			json.key("message").value(msg);

			// Stack trace part:
			if (printStackTrace){
				json.key("cause").array();
				Throwable cause = t;
				do{
					json.object();
					json.key("exception").value(cause.getClass().getName());
					json.key("stacktrace").array();
					StackTraceElement[] trace = cause.getStackTrace();
					for(int i = 0; i < trace.length; i++){
						json.object();
						json.key("class").value(trace[i].getClassName());
						json.key("method").value(trace[i].getMethodName());
						json.key("line").value(trace[i].getLineNumber());
						json.endObject();
					}
					json.endArray().endObject();

					// Print the stack trace of the "next" error:
					cause = cause.getCause();
				}while(cause != null);
				json.endArray();
			}

			json.endObject();
		}catch(JSONException je){
			getLogger().error("Impossible to format/write an error in JSON !", je);
			throw new IOException("Error while formatting the error in JSON !", je);
		}finally{
			out.flush();
			out.close();
		}
	}

}
