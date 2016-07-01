package uws.service.log;

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
 * Copyright 2012-2016 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.file.UWSFileManager;

/**
 * <p>Default implementation of {@link UWSLog} interface which lets logging any message about a UWS.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.2 (07/2016)
 */
public class DefaultUWSLog implements UWSLog {

	/** Format to use to serialize all encountered dates. */
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	protected final UWS uws;
	protected final UWSFileManager fileManager;
	protected final PrintWriter defaultOutput;

	/** <p>Minimum level that a message must have in order to be logged.</p>
	 * <p>The default behavior is the following:</p>
	 * <ul>
	 * 	<li><b>DEBUG</b>: every messages are logged.</li>
	 * 	<li><b>INFO</b>: every messages EXCEPT DEBUG are logged.</li>
	 * 	<li><b>WARNING</b>: every messages EXCEPT DEBUG and INFO are logged.</li>
	 * 	<li><b>ERROR</b>: only ERROR and FATAL messages are logged.</li>
	 * 	<li><b>FATAL</b>: only FATAL messages are logged.</li>
	 * </ul>
	 * @since 4.1 */
	protected LogLevel minLogLevel = LogLevel.DEBUG;

	/**
	 * <p>Builds a {@link UWSLog} which will use the file manager
	 * of the given UWS to get the log output (see {@link UWSFileManager#getLogOutput(uws.service.log.UWSLog.LogLevel, String)}).</p>
	 * 
	 * <p><i><u>note 1</u>: This constructor is particularly useful if the file manager of the given UWS may change.</i></p>
	 * <p><i><u>note 2</u>: If no output can be found in the file manager (or if there is no file manager),
	 * the standard error output ({@link System#err}) will be chosen automatically for all log messages.</i></p>
	 * 
	 * @param uws	A UWS.
	 */
	public DefaultUWSLog(final UWS uws){
		this.uws = uws;
		fileManager = null;
		defaultOutput = null;
	}

	/**
	 * <p>Builds a {@link UWSLog} which will use the given file
	 * manager to get the log output (see {@link UWSFileManager#getLogOutput(uws.service.log.UWSLog.LogLevel, String)}).</p>
	 * 
	 * <p><i><u>note 1</u>: This constructor is particularly useful if the way of managing log output may change in the given file manager.
	 * Indeed, the output may change in function of the type of message to log ({@link uws.service.log.UWSLog.LogLevel}).</i></p>
	 * 
	 * <p><i><u>note 2</u> If no output can be found in the file manager the standard error output ({@link System#err})
	 * will be chosen automatically for all log messages.</i></p>
	 * 
	 * @param fm	A UWS file manager.
	 */
	public DefaultUWSLog(final UWSFileManager fm){
		uws = null;
		fileManager = fm;
		defaultOutput = null;
	}

	/**
	 * <p>Builds a {@link UWSLog} which will print all its
	 * messages into the given stream.</p>
	 * 
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link uws.service.log.UWSLog.LogLevel}).</i></p>
	 * 
	 * @param output	An output stream.
	 */
	public DefaultUWSLog(final OutputStream output){
		uws = null;
		fileManager = null;
		defaultOutput = new PrintWriter(output);
	}

	/**
	 * <p>Builds a {@link UWSLog} which will print all its
	 * messages into the given stream.</p>
	 * 
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link uws.service.log.UWSLog.LogLevel}).</i></p>
	 * 
	 * @param writer	A print writer.
	 */
	public DefaultUWSLog(final PrintWriter writer){
		uws = null;
		fileManager = null;
		defaultOutput = writer;
	}

	/**
	 * <p>Get the minimum level that a message must have in order to be logged.</p>
	 * 
	 * <p>The default behavior is the following:</p>
	 * <ul>
	 * 	<li><b>DEBUG</b>: every messages are logged.</li>
	 * 	<li><b>INFO</b>: every messages EXCEPT DEBUG are logged.</li>
	 * 	<li><b>WARNING</b>: every messages EXCEPT DEBUG and INFO are logged.</li>
	 * 	<li><b>ERROR</b>: only ERROR and FATAL messages are logged.</li>
	 * 	<li><b>FATAL</b>: only FATAL messages are logged.</li>
	 * </ul>
	 * 
	 * @return	The minimum log level.
	 * 
	 * @since 4.1
	 */
	public final LogLevel getMinLogLevel(){
		return minLogLevel;
	}

	/**
	 * <p>Set the minimum level that a message must have in order to be logged.</p>
	 * 
	 * <p>The default behavior is the following:</p>
	 * <ul>
	 * 	<li><b>DEBUG</b>: every messages are logged.</li>
	 * 	<li><b>INFO</b>: every messages EXCEPT DEBUG are logged.</li>
	 * 	<li><b>WARNING</b>: every messages EXCEPT DEBUG and INFO are logged.</li>
	 * 	<li><b>ERROR</b>: only ERROR and FATAL messages are logged.</li>
	 * 	<li><b>FATAL</b>: only FATAL messages are logged.</li>
	 * </ul>
	 * 
	 * <p><i>Note:
	 * 	If the given level is NULL, this function has no effect.
	 * </i></p>
	 * 
	 * @param newMinLevel	The new minimum log level.
	 * 
	 * @since 4.1
	 */
	public final void setMinLogLevel(final LogLevel newMinLevel){
		if (newMinLevel != null)
			minLogLevel = newMinLevel;
	}

	/**
	 * Gets the date formatter/parser to use for any date read/write into this logger.
	 * @return A date formatter/parser.
	 */
	public final DateFormat getDateFormat(){
		return dateFormat;
	}

	/**
	 * Sets the date formatter/parser to use for any date read/write into this logger.
	 * @param dateFormat The date formatter/parser to use from now. (MUST BE DIFFERENT FROM NULL)
	 */
	public final void setDateFormat(final DateFormat dateFormat){
		if (dateFormat != null)
			this.dateFormat = dateFormat;
	}

	/**
	 * <p>Gets an output for the given type of message to print.</p>
	 * 
	 * <p>The {@link System#err} output is used if none can be found in the {@link UWS} or the {@link UWSFileManager}
	 * given at the creation, or if the given output stream or writer is NULL.</p>
	 * 
	 * @param level		Level of the message to print (DEBUG, INFO, WARNING, ERROR or FATAL).
	 * @param context	Context of the message to print (UWS, HTTP, JOB, THREAD).
	 * 
	 * @return			A writer.
	 */
	protected PrintWriter getOutput(final LogLevel level, final String context){
		try{
			if (uws != null){
				if (uws.getFileManager() != null)
					return uws.getFileManager().getLogOutput(level, context);
			}else if (fileManager != null)
				return fileManager.getLogOutput(level, context);
			else if (defaultOutput != null)
				return defaultOutput;
		}catch(IOException ioe){
			ioe.printStackTrace(System.err);
		}
		return new PrintWriter(System.err);
	}

	/* *********************** */
	/* GENERAL LOGGING METHODS */
	/* *********************** */

	/**
	 * <p>Normalize a log message.</p>
	 * 
	 * <p>
	 * 	Since a log entry will a tab-separated concatenation of information, additional tabulations or new-lines
	 * 	would corrupt a log entry. This function replaces such characters by one space. Only \r are definitely deleted.
	 * </p>
	 * 
	 * @param message	Log message to normalize.
	 * 
	 * @return	The normalized log message.
	 * 
	 * @since 4.1
	 */
	protected String normalizeMessage(final String message){
		if (message == null)
			return null;
		else
			return message.replaceAll("[\n\t]", " ").replaceAll("\r", "");
	}

	/**
	 * <p>Tells whether a message with the given error level can be logged or not.</p>
	 * 
	 * <p>In function of the minimum log level of this class, the default behavior is the following:</p>
	 * <ul>
	 * 	<li><b>DEBUG</b>: every messages are logged.</li>
	 * 	<li><b>INFO</b>: every messages EXCEPT DEBUG are logged.</li>
	 * 	<li><b>WARNING</b>: every messages EXCEPT DEBUG and INFO are logged.</li>
	 * 	<li><b>ERROR</b>: only ERROR and FATAL messages are logged.</li>
	 * 	<li><b>FATAL</b>: only FATAL messages are logged.</li>
	 * </ul>
	 * 
	 * @param msgLevel	Level of the message which has been asked to log. <i>Note: if NULL, it will be considered as DEBUG.</i>
	 * 
	 * @return	<i>true</i> if the message associated with the given log level can be logged, <i>false</i> otherwise.
	 * 
	 * @since 4.1
	 */
	protected boolean canLog(LogLevel msgLevel){
		// No level specified => DEBUG
		if (msgLevel == null)
			msgLevel = LogLevel.DEBUG;

		// Decide in function of the minimum log level set in this class:
		switch(minLogLevel){
			case INFO:
				return (msgLevel != LogLevel.DEBUG);
			case WARNING:
				return (msgLevel != LogLevel.DEBUG && msgLevel != LogLevel.INFO);
			case ERROR:
				return (msgLevel == LogLevel.ERROR || msgLevel == LogLevel.FATAL);
			case FATAL:
				return (msgLevel == LogLevel.FATAL);
			case DEBUG:
			default:
				return true;
		}
	}

	@Override
	public void log(LogLevel level, final String context, final String message, final Throwable error){
		log(level, context, null, null, message, null, error);
	}

	/**
	 * <p>Logs a full message and/or error.</p>
	 * 
	 * <p><i>Note:
	 * 	If no message and error is provided, nothing will be written.
	 * </i></p>
	 * 
	 * @param level		Level of the error (DEBUG, INFO, WARNING, ERROR, FATAL).	<i>SHOULD NOT be NULL</i>
	 * @param context	Context of the error (UWS, HTTP, THREAD, JOB). <i>MAY be NULL</i>
	 * @param event		Context event during which this log is emitted. <i>MAY be NULL</i>
	 * @param ID		ID of the job or HTTP request (it may also be an ID of anything else). <i>MAY BE NULL</i>
	 * @param message	Message of the error. <i>MAY be NULL</i>
	 * @param addColumn	Additional column to append after the message and before the stack trace.
	 * @param error		Error at the origin of the log error/warning/fatal. <i>MAY be NULL</i>
	 * 
	 * @since 4.1
	 */
	protected final void log(LogLevel level, final String context, final String event, final String ID, final String message, final String addColumn, final Throwable error){
		// If no message and no error is provided, nothing to log, so nothing to write:
		if ((message == null || message.length() <= 0) && error == null)
			return;

		// If the type is missing:
		if (level == null)
			level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

		// Log or not?
		if (!canLog(level))
			return;

		StringBuffer buf = new StringBuffer();
		// Print the date/time:
		buf.append(dateFormat.format(new Date())).append('\t');
		// Print the level of error (debug, info, warning, error, fatal):
		buf.append(level.toString()).append('\t');
		// Print the context of the error (uws, thread, job, http):
		buf.append((context == null) ? "" : context).append('\t');
		// Print the context event:
		buf.append((event == null) ? "" : event).append('\t');
		// Print an ID (jobID, requestID):
		buf.append((ID == null) ? "" : ID).append('\t');
		// Print the message:
		if (message != null)
			buf.append(normalizeMessage(message));
		else if (error != null)
			buf.append("[EXCEPTION ").append(error.getClass().getName()).append("] ").append(normalizeMessage(error.getMessage()));
		// Print the additional column, if any:
		if (addColumn != null)
			buf.append('\t').append(normalizeMessage(addColumn));

		// Write the whole log line:
		PrintWriter out = getOutput(level, context);
		out.println(buf.toString());

		// Print the stack trace, if any:
		printException(error, out);

		out.flush();
	}

	/**
	 * <p>Format and print the given exception inside the given writer.</p>
	 * 
	 * <p>This function does nothing if the given error is NULL.</p>
	 * 
	 * <p>The full stack trace is printed ONLY for unknown exceptions.</p>
	 * 
	 * <p>The printed text has the following format for known exceptions:</p>
	 * <pre>
	 * Caused by a {ExceptionClassName} {ExceptionOrigin}
	 *     {ExceptionMessage}
	 * </pre>
	 * 
	 * <p>The printed text has the following format for unknown exceptions:</p>
	 * <pre>
	 * Caused by a {ExceptionFullStackTrace}
	 * </pre>
	 * 
	 * @param error	The exception to print.
	 * @param out	The output in which the exception must be written.
	 * 
	 * @see #getExceptionOrigin(Throwable)
	 * 
	 * @since 4.1
	 */
	protected void printException(final Throwable error, final PrintWriter out){
		if (error != null){
			if (error instanceof UWSException){
				if (error.getCause() != null)
					printException(error.getCause(), out);
				else{
					out.println("Caused by a " + error.getClass().getName() + " " + getExceptionOrigin(error));
					if (error.getMessage() != null)
						out.println("\t" + error.getMessage());
				}
			}else{
				out.print("Caused by a ");
				error.printStackTrace(out);
			}
		}
	}

	/**
	 * <p>Format and return the origin of the given error.
	 * "Origin" means here: "where the error has been thrown from?" (from which class? method? file? line?).</p>
	 * 
	 * <p>This function does nothing if the given error is NULL or if the origin information is missing.</p>
	 * 
	 * <p>The returned text has the following format:</p>
	 * <pre>
	 * at {OriginClass}.{OriginMethod}({OriginFile}:{OriginLine})
	 * </pre>
	 * 
	 * <p>{OriginFile} and {OriginLine} are written only if provided.</p>
	 * 
	 * @param error	Error whose the origin should be returned.
	 * 
	 * @return	A string which contains formatted information about the origin of the given error.
	 * 
	 * @since 4.1
	 */
	protected String getExceptionOrigin(final Throwable error){
		if (error != null && error.getStackTrace() != null && error.getStackTrace().length > 0){
			StackTraceElement src = error.getStackTrace()[0];
			return "at " + src.getClassName() + "." + src.getMethodName() + ((src.getFileName() != null) ? "(" + src.getFileName() + ((src.getLineNumber() >= 0) ? ":" + src.getLineNumber() : "") + ")" : "");
		}else
			return "";
	}

	@Override
	public void debug(String msg){
		log(LogLevel.DEBUG, null, msg, null);
	}

	@Override
	public void debug(Throwable t){
		log(LogLevel.DEBUG, null, null, t);
	}

	@Override
	public void debug(String msg, Throwable t){
		log(LogLevel.DEBUG, null, msg, t);
	}

	@Override
	public void info(String msg){
		log(LogLevel.INFO, null, msg, null);
	}

	@Override
	public void warning(String msg){
		log(LogLevel.WARNING, null, msg, null);
	}

	@Override
	public void error(String msg){
		log(LogLevel.ERROR, null, msg, null);
	}

	@Override
	public void error(Throwable t){
		log(LogLevel.ERROR, null, null, t);
	}

	@Override
	public void error(String msg, Throwable t){
		log(LogLevel.ERROR, null, msg, t);
	}

	/* ************* */
	/* HTTP ACTIVITY */
	/* ************* */

	/**
	 * <p>A message/error logged with this function will have the following format:</p>
	 * <pre>&lt;TIMESTAMP&gt;	&lt;LEVEL&gt;	HTTP	REQUEST_RECEIVED	&lt;REQUEST_ID&gt;	&lt;MESSAGE&gt;	&lt;HTTP_METHOD&gt; in &lt;CONTENT_TYPE&gt; at &lt;URL&gt; from &lt;IP_ADDR&gt; using &lt;USER_AGENT&gt; with parameters (&lt;PARAM1&gt;=&lt;VAL1&gt;&...)</pre>
	 * 
	 * @see uws.service.log.UWSLog#logHttp(uws.service.log.UWSLog.LogLevel, javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void logHttp(LogLevel level, final HttpServletRequest request, final String requestId, final String message, final Throwable error){
		// IF A REQUEST IS PROVIDED, write its details after the message in a new column:
		if (request != null){
			// If the type is missing:
			if (level == null)
				level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

			// Log or not?
			if (!canLog(level))
				return;

			StringBuffer str = new StringBuffer();

			// Write the request type, content type and the URL:
			str.append(request.getMethod());
			str.append(" as ");
			if (request.getContentType() != null){
				if (request.getContentType().indexOf(';') > 0)
					str.append(request.getContentType().substring(0, request.getContentType().indexOf(';')));
				else
					str.append(request.getContentType());
			}
			str.append(" at ").append(request.getRequestURL());

			// Write the IP address:
			str.append(" from ").append(request.getRemoteAddr());

			// Write the user agent:
			str.append(" using ").append(request.getHeader("User-Agent") == null ? "" : request.getHeader("User-Agent"));

			// Write the posted parameters:
			str.append(" with parameters (");
			Map<String,String> params = UWSToolBox.getParamsMap(request);
			int i = -1;
			for(Entry<String,String> p : params.entrySet()){
				if (++i > 0)
					str.append('&');
				str.append(p.getKey()).append('=').append((p.getValue() != null) ? p.getValue() : "");
			}
			str.append(')');

			// Send the log message to the log file:
			log(level, "HTTP", "REQUEST_RECEIVED", requestId, (message != null ? message : str.toString()), (message != null ? str.toString() : null), error);
		}
		// OTHERWISE, just write the given message:
		else
			log(level, "HTTP", "REQUEST_RECEIVED", requestId, message, null, error);
	}

	/**
	 * <p>A message/error logged with this function will have the following format:</p>
	 * <pre>&lt;TIMESTAMP&gt;	&lt;LEVEL&gt;	HTTP	RESPONSE_SENT	&lt;REQUEST_ID&gt;	&lt;MESSAGE&gt;	HTTP-&lt;STATUS_CODE&gt; to the user &lt;USER&gt; as &lt;CONTENT_TYPE&gt;</pre>
	 * <p>,where &lt;USER&gt; may be either "(id:&lt;USER_ID&gt;;pseudo:&lt;USER_PSEUDO&gt;)" or "ANONYMOUS".</p>
	 * 
	 * @see uws.service.log.UWSLog#logHttp(uws.service.log.UWSLog.LogLevel, javax.servlet.http.HttpServletResponse, java.lang.String, uws.job.user.JobOwner, java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void logHttp(LogLevel level, HttpServletResponse response, String requestId, JobOwner user, String message, Throwable error){
		if (response != null){
			// If the type is missing:
			if (level == null)
				level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

			// Log or not?
			if (!canLog(level))
				return;

			StringBuffer str = new StringBuffer();

			// Write the response status code:
			str.append("HTTP-").append(response.getStatus());

			// Write the user to whom the response is sent:
			str.append(" to the user ");
			if (user != null){
				str.append("(id:").append(user.getID());
				if (user.getPseudo() != null)
					str.append(";pseudo:").append(user.getPseudo());
				str.append(')');
			}else
				str.append("ANONYMOUS");

			// Write the response's MIME type:
			if (response.getContentType() != null)
				str.append(" as ").append(response.getContentType());

			// Send the log message to the log file:
			log(level, "HTTP", "RESPONSE_SENT", requestId, message, str.toString(), error);
		}
		// OTHERWISE, just write the given message:
		else
			log(level, "HTTP", "RESPONSE_SENT", requestId, message, null, error);
	}

	/* ************ */
	/* UWS ACTIVITY */
	/* ************ */

	@Override
	public void logUWS(LogLevel level, Object obj, String event, String message, Throwable error){
		// If the type is missing:
		if (level == null)
			level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

		// Log or not?
		if (!canLog(level))
			return;

		// CASE "BACKUPED": Append to the message the backup report:
		String report = null;
		if (event != null && event.equalsIgnoreCase("BACKUPED") && obj != null && obj.getClass().getName().equals("[I")){
			int[] backupReport = (int[])obj;
			if (backupReport.length == 2)
				report = "(" + backupReport[0] + "/" + backupReport[1] + " jobs backuped for this user)";
			else
				report = "(" + backupReport[0] + "/" + backupReport[1] + " jobs backuped ; " + backupReport[2] + "/" + backupReport[3] + " users backuped)";
		}else if (event != null && event.equalsIgnoreCase("RESTORED") && obj != null && obj.getClass().getName().equals("[I")){
			int[] restoreReport = (int[])obj;
			report = "(" + restoreReport[0] + "/" + restoreReport[1] + " jobs restored ; " + restoreReport[2] + "/" + restoreReport[3] + " users restored)";
		}

		// Log the message
		log(level, "UWS", event, null, message, report, error);
	}

	/* ************ */
	/* JOB ACTIVITY */
	/* ************ */

	@Override
	public void logJob(LogLevel level, UWSJob job, String event, String message, Throwable error){
		log(level, "JOB", event, (job == null) ? null : job.getJobId(), message, null, error);
	}

	/* ********************** */
	/* THREAD STATUS MESSAGES */
	/* ********************** */

	@Override
	public void logThread(LogLevel level, Thread thread, String event, String message, Throwable error){
		if (thread != null){
			// If the type is missing:
			if (level == null)
				level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

			// Log or not?
			if (!canLog(level))
				return;

			StringBuffer str = new StringBuffer();

			// Write the thread name and ID:
			str.append(thread.getName()).append(" (thread ID: ").append(thread.getId()).append(")");

			// Write the thread state:
			str.append(" is ").append(thread.getState());

			// Write its thread group name:
			str.append(" in the group " + thread.getThreadGroup().getName());

			// Write the number of active threads:
			str.append(" where ").append(thread.getThreadGroup().activeCount()).append(" threads are active");

			log(level, "THREAD", event, thread.getName(), message, str.toString(), error);

		}else
			log(level, "THREAD", event, null, message, null, error);
	}

}
