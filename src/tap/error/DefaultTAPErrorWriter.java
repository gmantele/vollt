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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import tap.TAPException;
import tap.formatter.VOTableFormat;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.ErrorType;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.error.DefaultUWSErrorWriter;
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
 * @version 2.0 (09/2014)
 * 
 * @see DefaultUWSErrorWriter
 */
public class DefaultTAPErrorWriter implements ServiceErrorWriter {

	/** Logger to use to report any unexpected error. */
	protected final TAPLog logger;

	/** Object to use to format an error message in VOTable. */
	protected final VOTableFormat formatter;

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

		this.logger = (service.getLogger() == null) ? new DefaultTAPLog(System.err) : service.getLogger();
		this.formatter = new VOTableFormat(service);
	}

	@Override
	public void writeError(final Throwable t, final HttpServletResponse response, final HttpServletRequest request, final String reqID, final JobOwner user, final String action) throws IOException{
		if (t == null || response == null)
			return;

		// If expected error, just write it in VOTable:
		if (t instanceof UWSException || t instanceof TAPException){
			// get the error type:
			ErrorType type = (t instanceof UWSException) ? ((UWSException)t).getUWSErrorType() : ErrorType.FATAL;
			// get the HTTP error code:
			int httpErrorCode = (t instanceof UWSException) ? ((UWSException)t).getHttpErrorCode() : ((TAPException)t).getHttpErrorCode();
			// write the VOTable error:
			writeError(t.getMessage(), type, httpErrorCode, response, request, reqID, user, action);
		}
		// Otherwise, log it and write a message to the user:
		else
			// write a message to the user:
			writeError("INTERNAL SERVER ERROR! Sorry, this error is grave and unexpected. No explanation can be provided for the moment. Details about this error have been reported in the service log files ; you should try again your request later or notify the administrator(s) by yourself (with the following REQ_ID).", ErrorType.FATAL, UWSException.INTERNAL_SERVER_ERROR, response, request, reqID, user, action);
	}

	@Override
	public void writeError(final String message, final ErrorType type, final int httpErrorCode, final HttpServletResponse response, final HttpServletRequest request, final String reqID, final JobOwner user, final String action) throws IOException{
		if (message == null || response == null)
			return;

		// Erase anything written previously in the HTTP response:
		response.reset();

		// Set the HTTP status:
		response.setStatus((httpErrorCode <= 0) ? 500 : httpErrorCode);

		// Set the MIME type of the answer (XML for a VOTable document):
		response.setContentType("application/xml");

		// List any additional information useful to report to the user:
		HashMap<String,String> addInfos = new HashMap<String,String>();
		if (reqID != null)
			addInfos.put("REQ_ID", reqID);
		if (type != null)
			addInfos.put("ERROR_TYPE", type.toString());
		if (user != null)
			addInfos.put("USER", user.getID() + ((user.getPseudo() == null) ? "" : " (" + user.getPseudo() + ")"));
		if (action != null)
			addInfos.put("ACTION", action);

		// Format the error in VOTable and write the document in the given HTTP response:
		formatter.writeError(message, addInfos, response.getWriter());
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
		HashMap<String,String> addInfos = new HashMap<String,String>();
		if (job != null){
			addInfos.put("JOB_ID", job.getJobId());
			if (job.getOwner() != null)
				addInfos.put("USER", job.getOwner().getID() + ((job.getOwner().getPseudo() == null) ? "" : " (" + job.getOwner().getPseudo() + ")"));
		}
		if (error != null && error.getType() != null)
			addInfos.put("ERROR_TYPE", error.getType().toString());
		addInfos.put("ACTION", "EXECUTING");

		// Format the error in VOTable and write the document in the given HTTP response:
		formatter.writeError(message, addInfos, new PrintWriter(output));
	}

	@Override
	public String getErrorDetailsMIMEType(){
		return "application/xml";
	}

}
