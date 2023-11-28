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
 * Copyright 2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uws.UWSToolBox;
import uws.job.UWSJob;
import uws.job.user.JobOwner;

/**
 * This implementation of {@link UWSLog} forwards all log submission to
 * <a href="https://www.slf4j.org">SLF4J</a>.
 *
 * <p>
 * 	Thus, a UWS implementor can choose how to deal with logs. SLF4J already
 * 	provides bridges at least for the following logging frameworks:
 * </p>
 * <ul>
 * 	<li><a href="https://www.slf4j.org/api/org/slf4j/helpers/NOPLogger.html">
 * 		NOPE</a> (all logs are merely discarded),</li>
 * 	<li><a href="https://www.slf4j.org/apidocs/org/slf4j/impl/SimpleLogger.html">
 * 		simple logging mechanism</a> (console, file, ...),</li>
 * 	<li><a href="https://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html">JUL</a> (java.util.logging),</li>
 * 	<li><a href="https://logging.apache.org/log4j">Log4J</a>,</li>
 * 	<li><a href="http://commons.apache.org/proper/commons-logging/">JCL</a>
 * 	    (Jakarta Commons Logging),</li>
 * 	<li>and <a href="https://logback.qos.ch/">LogBack</a>.</li>
 * </ul>
 *
 * <b>SLF4J loggers</b>
 *
 * <p>
 * 	A different logger is used in function of the log entry's CONTEXT.
 * 	Four values are supported: "UWS", "HTTP", "JOB" and "THREAD". If a different
 * 	value is set for the CONTEXT, the general/root logger will be used.
 * </p>
 * <p>The ID of each logger follows this rule:</p>
 * <pre>{@value #DEFAULT_ROOT_LOG_ID}{SUFFIX}</pre>
 * <p>
 * 	The ID of the general/root logger is just
 * 	<code>{@value #DEFAULT_ROOT_LOG_ID}</code>. For all the other specific
 * 	loggers, <code>{SUFFIX}</code> is a dot followed by one among
 * 	{@value #UWS_LOG_ID_SUFFIX}, {@value #HTTP_LOG_ID_SUFFIX},
 * 	{@value #JOB_LOG_ID_SUFFIX} and {@value #THREAD_LOG_ID_SUFFIX}. Thus, the
 * 	full ID of the logger for the HTTP requests is:
 * </p>
 * <pre>uws.service.http</pre>
 *
 * <b>Log entries format</b>
 *
 * <p>All log entries are formatted as follow:</p>
 * <pre>{EVENT} - {ID} - {MESSAGE} - {ADDITIONAL_DATA}</pre>
 * <p>
 * 	{EVENT}, {ID} and {ADDITIONAL_DATA} may not be all provided for
 * 	some messages ; all are optional, and especially ADDITIONAL_DATA. When not
 * 	provided, each is replaced by an empty string. In the special case of
 * 	ADDITIONAL_DATA, if none is provided, the " - " prefixing it is not written.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 4.3 (07/2018)
 * @since 4.3
 */
public class Slf4jUWSLog implements UWSLog {

	/** Default SLF4J identifier for the general/root logger to use. */
	protected static final String DEFAULT_ROOT_LOG_ID = "uws.service";

	/** Suffix to append to the root logger ID for the UWS logger. */
	protected static final String UWS_LOG_ID_SUFFIX = "UWS";

	/** Suffix to append to the root logger ID for the HTTP logger. */
	protected static final String HTTP_LOG_ID_SUFFIX = "HTTP";

	/** Suffix to append to the root logger ID for the JOB logger. */
	protected static final String JOB_LOG_ID_SUFFIX = "JOB";

	/** Suffix to append to the root logger ID for the THREAD logger. */
	protected static final String THREAD_LOG_ID_SUFFIX = "THREAD";

	/** General purpose logger. */
	protected final Logger rootLogger;

	/** Logger for general UWS actions. */
	protected final Logger uwsLogger;

	/** Logger for HTTP requests and responses. */
	protected final Logger httpLogger;

	/** Logger for UWS jobs actions. */
	protected final Logger jobLogger;

	/** Logger for UWS jobs' threads actions. */
	protected final Logger threadLogger;

	/**
	 * Initialise this logger.
	 */
	public Slf4jUWSLog(){
		this(DEFAULT_ROOT_LOG_ID);
	}

	/**
	 * Initialise this logger with the specified SLF4J logger.
	 */
	protected Slf4jUWSLog(final String logId){
		final String rootLogId = (logId == null || logId.trim().length() == 0) ? DEFAULT_ROOT_LOG_ID : logId;
		rootLogger = LoggerFactory.getLogger(rootLogId);
		uwsLogger = LoggerFactory.getLogger(rootLogId + "." + UWS_LOG_ID_SUFFIX);
		httpLogger = LoggerFactory.getLogger(rootLogId + "." + HTTP_LOG_ID_SUFFIX);
		jobLogger = LoggerFactory.getLogger(rootLogId + "." + JOB_LOG_ID_SUFFIX);
		threadLogger = LoggerFactory.getLogger(rootLogId + "." + THREAD_LOG_ID_SUFFIX);
	}

	@Override
	public String getConfigString(){
		return "type: \"SLF4J\", loggers: [\"" + rootLogger.getName() + "\", \"" + uwsLogger.getName() + "\", \"" + httpLogger.getName() + "\", \"" + jobLogger.getName() + "\", \"" + threadLogger.getName() + "\"]";
	}

	/* *********************** */
	/* GENERAL LOGGING METHODS */
	/* *********************** */

	@Override
	public void log(LogLevel level, String context, String message, Throwable error){
		Logger logger = rootLogger;
		if (context != null){
			if ("HTTP".equalsIgnoreCase(context))
				logger = httpLogger;
			else if ("UWS".equalsIgnoreCase(context))
				logger = uwsLogger;
			else if ("JOB".equalsIgnoreCase(context))
				logger = jobLogger;
			else if ("THREAD".equalsIgnoreCase(context))
				logger = threadLogger;
		}
		log(level, logger, null, null, message, null, error);
	}

	/**
	 * <p>Logs a full message and/or error.</p>
	 *
	 * <p><i>Note:
	 * 	If no message and error is provided, nothing will be written.
	 * </i></p>
	 *
	 * @param level		Level of the error (DEBUG, INFO, WARNING, ERROR, FATAL).	<i>SHOULD NOT be NULL</i>
	 * @param logger	Logger to use. <i>MUST NOT be NULL</i>
	 * @param event		Context event during which this log is emitted. <i>MAY be NULL</i>
	 * @param ID		ID of the job or HTTP request (it may also be an ID of anything else). <i>MAY BE NULL</i>
	 * @param message	Message of the error. <i>MAY be NULL</i>
	 * @param addColumn	Additional column to append after the message and before the stack trace.
	 * @param error		Error at the origin of the log error/warning/fatal. <i>MAY be NULL</i>
	 */
	protected final void log(LogLevel level, Logger logger, final String event, final String ID, final String message, final String addColumn, final Throwable error){
		// If no message and no error is provided, nothing to log:
		if ((message == null || message.length() <= 0) && error == null)
			return;

		// Set a default log level, if needed:
		if (level == null)
			level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

		// Set the root logger if not is set:
		if (logger == null)
			logger = rootLogger;

		// Call the appropriate function depending on the log level:
		switch(level){
			case DEBUG:
				logger.debug("{} - {} - {}{}", (event == null ? "" : event), (ID == null ? "" : ID), message, (addColumn == null ? "" : " - " + addColumn), error);
				break;
			case INFO:
				logger.info("{} - {} - {}{}", (event == null ? "" : event), (ID == null ? "" : ID), message, (addColumn == null ? "" : " - " + addColumn), error);
				break;
			case WARNING:
				logger.warn("{} - {} - {}{}", (event == null ? "" : event), (ID == null ? "" : ID), message, (addColumn == null ? "" : " - " + addColumn), error);
				break;
			case ERROR:
				logger.error("{} - {} - {}{}", (event == null ? "" : event), (ID == null ? "" : ID), message, (addColumn == null ? "" : " - " + addColumn), error);
				break;
			case FATAL:
				logger.error("{} - {} - GRAVE: {}{}", (event == null ? "" : event), (ID == null ? "" : ID), message, (addColumn == null ? "" : " - " + addColumn), error);
				break;
		}
	}

	@Override
	public void debug(final String msg){
		log(LogLevel.DEBUG, rootLogger, null, null, msg, null, null);
	}

	@Override
	public void debug(final Throwable t){
		log(LogLevel.DEBUG, rootLogger, null, null, "Debugging stack trace:", null, t);
	}

	@Override
	public void debug(final String msg, final Throwable t){
		log(LogLevel.DEBUG, rootLogger, null, null, msg, null, t);
	}

	@Override
	public void info(final String msg){
		log(LogLevel.INFO, rootLogger, null, null, msg, null, null);
	}

	@Override
	public void warning(final String msg){
		log(LogLevel.WARNING, rootLogger, null, null, msg, null, null);
	}

	@Override
	public void error(final String msg){
		log(LogLevel.ERROR, rootLogger, null, null, msg, null, null);
	}

	@Override
	public void error(final Throwable t){
		log(LogLevel.ERROR, rootLogger, null, null, "Unexpected error:", null, t);
	}

	@Override
	public void error(final String msg, final Throwable t){
		log(LogLevel.ERROR, rootLogger, null, null, msg, null, t);
	}

	/* ************* */
	/* HTTP ACTIVITY */
	/* ************* */

	@Override
	public void logHttp(final LogLevel level, final HttpServletRequest request, final String requestId, final String message, final Throwable error){
		// IF A REQUEST IS PROVIDED, write its details after the message in a new column:
		if (request != null){
			StringBuffer str = new StringBuffer((message != null) ? message + ": " : "");

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
			Map<String, String> params = UWSToolBox.getParamsMap(request);
			int i = -1;
			for(Entry<String, String> p : params.entrySet()){
				if (++i > 0)
					str.append('&');
				str.append(p.getKey()).append('=').append((p.getValue() != null) ? p.getValue() : "");
			}
			str.append(')');

			// Send the log message to the log file:
			if (message == null)
				log(level, httpLogger, "REQUEST_RECEIVED", requestId, str.toString(), null, error);
			else
				log(level, httpLogger, "REQUEST_RECEIVED", requestId, str.toString(), null, error);
		}
		// OTHERWISE, just write the given message:
		else
			log(level, httpLogger, "REQUEST_RECEIVED", requestId, message, null, error);
	}

	@Override
	public void logHttp(final LogLevel level, final HttpServletResponse response, final String requestId, final JobOwner user, final String message, final Throwable error){
		if (response != null){
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
			log(level, httpLogger, "RESPONSE_SENT", requestId, message, str.toString(), error);
		}
		// OTHERWISE, just write the given message:
		else
			log(level, httpLogger, "RESPONSE_SENT", requestId, message, null, error);
	}

	/* ************ */
	/* UWS ACTIVITY */
	/* ************ */

	@Override
	public void logUWS(final LogLevel level, final Object obj, final String event, final String message, final Throwable error){
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
		log(level, uwsLogger, event, null, message, report, error);
	}

	/* ************ */
	/* JOB ACTIVITY */
	/* ************ */

	@Override
	public void logJob(final LogLevel level, final UWSJob job, final String event, final String message, final Throwable error){
		log(level, jobLogger, event, (job == null) ? null : job.getJobId(), message, null, error);
	}

	@Override
	public void logThread(final LogLevel level, final Thread thread, final String event, final String message, final Throwable error){
		if (thread != null){
			StringBuffer str = new StringBuffer();

			// Write the thread name and ID:
			str.append(thread.getName()).append(" (thread ID: ").append(thread.getId()).append(")");

			// Write the thread state:
			str.append(" is ").append(thread.getState());

			// Write its thread group name:
			str.append(" in the group " + thread.getThreadGroup().getName());

			// Write the number of active threads:
			str.append(" where ").append(thread.getThreadGroup().activeCount()).append(" threads are active");

			log(level, threadLogger, event, thread.getName(), message, str.toString(), error);

		}else
			log(level, threadLogger, event, null, message, null, error);
	}

}
