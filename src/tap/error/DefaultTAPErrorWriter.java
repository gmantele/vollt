package tap.error;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import tap.TAPException;
import tap.formatter.OutputFormat;
import tap.formatter.VOTableFormat;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import uws.UWSException;
import uws.UWSToolBox;
import uws.job.ErrorSummary;
import uws.job.ErrorType;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.error.ServiceErrorWriter;

/**
 * <p>Default implementation of {@link ServiceErrorWriter} for a TAP service.</p>
 * 
 * <p>
 * 	On the contrary to the UWS standard, all errors must be formatted in VOTable.
 * 	So, all errors given to this {@link ServiceErrorWriter} are formatted in VOTable using the structure defined by the IVOA.
 * 	To do that, this class will use the function {@link VOTableFormat#writeError(String, java.util.Map, java.io.PrintWriter)}.
 * </p>
 * 
 * <p>
 * 	The {@link VOTableFormat} will be got from the {@link ServiceConnection} using {@link ServiceConnection#getOutputFormat(String)}
 * 	with "votable" as parameter. If the returned formatter is not a direct instance or an extension of {@link VOTableFormat},
 * 	a default instance of this class will be always used.
 * </p>
 * 
 * <p>
 * 	{@link UWSException}s and {@link TAPException}s may precise the HTTP error code to apply,
 * 	which will be used to set the HTTP status of the response. If it is a different kind of exception,
 * 	the HTTP status 500 (INTERNAL SERVER ERROR) will be used.
 * </p>
 * 
 * <p>
 * 	Besides, all exceptions except {@link UWSException} and {@link TAPException} will be logged as FATAL in the TAP context
 * 	(with no event and no object). Thus the full stack trace is available to the administrator so that the error can
 * 	be understood as easily and quickly as possible.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (04/2015)
 */
public class DefaultTAPErrorWriter implements ServiceErrorWriter {

	/** Description of the TAP service using this {@link ServiceErrorWriter}. */
	protected final ServiceConnection service;

	/** Logger to use to report any unexpected error.
	 * <b>This attribute MUST NEVER be used directly, but only with its getter {@link #getLogger()}.</b> */
	protected TAPLog logger = null;

	/** Object to use to format an error message into VOTable.
	 * <b>This attribute MUST NEVER be used directly, but only with its getter {@link #getFormatter()}.</b> */
	protected VOTableFormat formatter = null;

	/**
	 * <p>Build an error writer for TAP.</p>
	 * 
	 * <p>
	 * 	On the contrary to the UWS standard, TAP standard defines a format for error reporting.
	 * 	Errors should be reported as VOTable document with a defined structure.  This one is well
	 * 	managed by {@link VOTableFormat} which is actually called by this class when an error must
	 * 	be written.
	 * </p>
	 * 
	 * @param service	Description of the TAP service.
	 * 
	 * @throws NullPointerException	If no service description is provided.
	 */
	public DefaultTAPErrorWriter(final ServiceConnection service) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("Missing description of this TAP service! Can not build a ServiceErrorWriter.");
		this.service = service;
	}

	/**
	 * <p>Get the {@link VOTableFormat} to use in order to format errors.</p>
	 * 
	 * <p><i>Note:
	 * 	If not yet set, the formatter of this {@link ServiceErrorWriter} is set to the formatter of VOTable results returned by the {@link ServiceConnection}.
	 * 	However this formatter should be a {@link VOTableFormat} instance or an extension (because the function {@link VOTableFormat#writeError(String, java.util.Map, PrintWriter)} is needed).
	 * 	Otherwise a default {@link VOTableFormat} instance will be created and always used by this {@link ServiceErrorWriter}.
	 * </i></p>
	 * 
	 * @return	A VOTable formatter.
	 * 
	 * @since 2.0
	 */
	protected VOTableFormat getFormatter(){
		if (formatter == null){
			OutputFormat fmt = service.getOutputFormat("votable");
			if (fmt == null || !(fmt instanceof VOTableFormat))
				formatter = new VOTableFormat(service);
			else
				formatter = (VOTableFormat)fmt;
		}
		return formatter;
	}

	/**
	 * <p>Get the logger to use inside this {@link ServiceErrorWriter}.</p>
	 * 
	 * <p><i>Note:
	 * 	If not yet set, the logger of this {@link ServiceErrorWriter} is set to the logger used by the {@link ServiceConnection}.
	 * 	If none is returned by the {@link ServiceConnection}, a default {@link TAPLog} instance writing logs in System.err
	 * 	will be created and always used by this {@link ServiceErrorWriter}.
	 * </i></p>
	 * 
	 * @return	A logger.
	 * 
	 * @since 2.0
	 */
	protected TAPLog getLogger(){
		if (logger == null){
			logger = service.getLogger();
			if (logger == null)
				logger = new DefaultTAPLog(System.err);
		}
		return logger;
	}

	@Override
	public boolean writeError(final Throwable t, final HttpServletResponse response, final HttpServletRequest request, final String reqID, final JobOwner user, final String action){
		if (t == null || response == null)
			return true;

		boolean written = false;
		// If expected error, just write it in VOTable:
		if (t instanceof UWSException || t instanceof TAPException){
			// get the error type:
			ErrorType type = (t instanceof UWSException) ? ((UWSException)t).getUWSErrorType() : ErrorType.FATAL;
			// get the HTTP error code:
			int httpErrorCode = (t instanceof UWSException) ? ((UWSException)t).getHttpErrorCode() : ((TAPException)t).getHttpErrorCode();
			// write the VOTable error:
			written = writeError(t.getMessage(), type, httpErrorCode, response, request, reqID, user, action);
		}
		// Otherwise, log it and write a message to the user:
		else
			// write a message to the user:
			written = writeError("INTERNAL SERVER ERROR! Sorry, this error is grave and unexpected. No explanation can be provided for the moment. Details about this error have been reported in the service log files ; you should try again your request later or notify the administrator(s) by yourself (with the following REQ_ID).", ErrorType.FATAL, UWSException.INTERNAL_SERVER_ERROR, response, request, reqID, user, action);
		return written;
	}

	@Override
	public boolean writeError(final String message, final ErrorType type, final int httpErrorCode, final HttpServletResponse response, final HttpServletRequest request, final String reqID, final JobOwner user, final String action){
		if (message == null || response == null)
			return true;

		try{
			// Erase anything written previously in the HTTP response:
			response.reset();

			// Set the HTTP status:
			response.setStatus((httpErrorCode <= 0) ? 500 : httpErrorCode);

			// Set the MIME type of the answer (XML for a VOTable document):
			response.setContentType("application/xml");

			// Set the character encoding:
			response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

		}catch(IllegalStateException ise){
			/*   If it is not possible any more to reset the response header and body,
			 * the error is anyway written in order to corrupt the HTTP response.
			 *   Thus, it will be obvious that an error occurred and the result is
			 * incomplete and/or wrong.*/
		}

		try{
			// List any additional information useful to report to the user:
			Map<String,String> addInfos = new LinkedHashMap<String,String>();
			if (reqID != null)
				addInfos.put("REQ_ID", reqID);
			if (type != null)
				addInfos.put("ERROR_TYPE", type.toString());
			if (user != null)
				addInfos.put("USER", user.getID() + ((user.getPseudo() == null) ? "" : " (" + user.getPseudo() + ")"));
			if (action != null)
				addInfos.put("ACTION", action);

			// Format the error in VOTable and write the document in the given HTTP response:
			PrintWriter writer;
			try{
				writer = response.getWriter();
			}catch(IllegalStateException ise){
				/*   This exception may occur just because either the writer or
				 * the output-stream can be used (because already got before).
				 *   So, we just have to get the output-stream if getting the writer
				 * throws an error.*/
				writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(response.getOutputStream())));
			}
			getFormatter().writeError(message, addInfos, writer);

			return true;
		}catch(IllegalStateException ise){
			return false;
		}catch(IOException ioe){
			return false;
		}
	}

	@Override
	public void writeError(Throwable t, ErrorSummary error, UWSJob job, OutputStream output) throws IOException{
		// Get the error message:
		String message;
		if (error != null && error.getMessage() != null)
			message = error.getMessage();
		else if (t != null)
			message = (t.getMessage() == null) ? t.getClass().getName() : t.getMessage();
		else
			message = "{NO MESSAGE}";

		// List any additional information useful to report to the user:
		Map<String,String> addInfos = new LinkedHashMap<String,String>();
		// error type:
		if (error != null && error.getType() != null)
			addInfos.put("ERROR_TYPE", error.getType().toString());
		// infos about the exception:
		putExceptionInfos(t, addInfos);
		// job ID:
		if (job != null){
			addInfos.put("JOB_ID", job.getJobId());
			if (job.getOwner() != null)
				addInfos.put("USER", job.getOwner().getID() + ((job.getOwner().getPseudo() == null) ? "" : " (" + job.getOwner().getPseudo() + ")"));
		}
		// action running while the error occurred (only one is possible here: EXECUTING an ADQL query):
		addInfos.put("ACTION", "EXECUTING");

		// Format the error in VOTable and write the document in the given HTTP response:
		getFormatter().writeError(message, addInfos, new PrintWriter(output));
	}

	/**
	 * Add all interesting additional information about the given exception inside the given map.
	 * 
	 * @param t			Exception whose some details must be added inside the given map.
	 * @param addInfos	Map of all additional information.
	 * 
	 * @since 2.0
	 */
	protected void putExceptionInfos(final Throwable t, final Map<String,String> addInfos){
		if (t != null){
			// Browse the exception stack in order to list all exceptions' messages and to get the last cause of this error:
			StringBuffer causes = new StringBuffer();
			Throwable cause = t.getCause(), lastCause = t;
			int nbCauses = 0, nbStackTraces = 1;
			while(cause != null){
				// new line:
				causes.append('\n');
				// append the message:
				causes.append("\t- ").append(cause.getMessage());
				// SQLException case:
				if (cause instanceof SQLException){
					SQLException se = (SQLException)cause;
					while(se.getNextException() != null){
						se = se.getNextException();
						causes.append("\n\t\t- ").append(se.getMessage());
					}
				}
				// go to the next message:
				lastCause = cause;
				cause = cause.getCause();
				nbCauses++;
				nbStackTraces++;
			}

			// Add the list of all causes' message:
			if (causes.length() > 0)
				addInfos.put("CAUSES", "\n" + nbCauses + "\n<![CDATA[\n" + causes.toString() + "\n]]>\n"); // added CDATA to wrap free text to make error document parsable

			// Add the stack trace of the original exception ONLY IF NOT A TAP NOR A UWS EXCEPTION (only unexpected error should be detailed to the users):
			if (!(lastCause instanceof TAPException && lastCause instanceof UWSException)){
				ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
				lastCause.printStackTrace(new PrintStream(stackTrace));
				addInfos.put("ORIGIN_STACK_TRACE", "\n" + nbStackTraces + "\n" + "\n<![CDATA[\n" + stackTrace.toString() + "\n]]>\n"); // added CDATA to wrap free text to make error document parsable
			}
		}
	}

	@Override
	public String getErrorDetailsMIMEType(){
		return "application/xml";
	}

}
