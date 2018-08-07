package tap.log;

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
 * Copyright 2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tap.TAPExecutionReport;
import tap.TAPSyncJob;
import tap.db.DBConnection;
import tap.parameters.TAPParameters;
import uws.service.log.Slf4jUWSLog;

/**
 * This implementation of {@link TAPLog} forwards all log submission to
 * <a href="https://www.slf4j.org">SLF4J</a>.
 *
 * <p>
 * 	Thus, a TAP implementor can choose how to deal with logs. SLF4J already
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
 * @version 2.3 (07/2018)
 * @since 2.3
 */
public class Slf4jTAPLog extends Slf4jUWSLog implements TAPLog {

	/** Default SLF4J identifier for the general/root logger to use. */
	protected static final String DEFAULT_ROOT_LOG_ID = "tap.service";

	/** Suffix to append to the root logger ID for the TAP logger. */
	protected static final String TAP_LOG_ID_SUFFIX = "TAP";

	/** Suffix to append to the root logger ID for the DATABASE logger. */
	protected static final String DB_LOG_ID_SUFFIX = "DB";

	/** Logger for TAP jobs actions. */
	protected final Logger tapLogger;

	/** Logger for database actions. */
	protected final Logger dbLogger;

	/**
	 * Initialise this logger.
	 */
	public Slf4jTAPLog(){
		this(DEFAULT_ROOT_LOG_ID);
	}

	@Override
	public String getConfigString(){
		return "type: \"SLF4J\", loggers: [\"" + rootLogger.getName() + "\", \"" + uwsLogger.getName() + "\", \"" + httpLogger.getName() + "\", \"" + jobLogger.getName() + "\", \"" + threadLogger.getName() + "\", \"" + tapLogger.getName() + "\", \"" + dbLogger.getName() + "\"]";
	}

	/**
	 * Initialise this logger with the specified SLF4J logger.
	 */
	protected Slf4jTAPLog(final String logId){
		super((logId == null || logId.trim().length() == 0) ? DEFAULT_ROOT_LOG_ID : logId);

		final String rootLogId = rootLogger.getName();
		tapLogger = LoggerFactory.getLogger(rootLogId + "." + TAP_LOG_ID_SUFFIX);
		dbLogger = LoggerFactory.getLogger(rootLogId + "." + DB_LOG_ID_SUFFIX);
	}

	@Override
	public void logDB(final LogLevel level, final DBConnection connection, final String event, final String message, final Throwable error){
		// log the main given error:
		log(level, dbLogger, event, (connection != null ? connection.getID() : null), message, null, error);

		/* Some SQL exceptions (like BatchUpdateException) have a next exception which provides more information.
		 * Here, the stack trace of the next exception is also logged:
		 */
		if (error != null && error instanceof SQLException && ((SQLException)error).getNextException() != null){
			Throwable nextError;
			int indNextError = 1;
			/* show all next exceptions
			 * (limited to 3 so that not filling the log with a lot of stack
			 *  traces): */
			do{
				nextError = ((SQLException)error).getNextException();
				log(level, dbLogger, event, (connection != null ? connection.getID() : null), "[NEXT EXCEPTION - " + (indNextError++) + "]", null, nextError);
			}while(error != null && indNextError <= 3);
		}
	}

	@Override
	public void logTAP(final LogLevel level, final Object obj, final String event, final String message, final Throwable error){
		// Get more information (when known event and available object):
		String jobId = null, msgAppend = null;
		try{
			if (event != null && obj != null){
				if (event.equals("SYNC_INIT"))
					msgAppend = "QUERY=" + ((TAPParameters)obj).getQuery();
				else if (obj instanceof TAPSyncJob){
					log(level, tapLogger, event, ((TAPSyncJob)obj).getID(), message, null, error);
					return;
				}else if (obj instanceof TAPExecutionReport){
					TAPExecutionReport report = (TAPExecutionReport)obj;
					jobId = report.jobID;
					msgAppend = (report.synchronous ? "SYNC" : "ASYNC") + ",duration=" + report.getTotalDuration() + "ms (upload=" + report.getUploadDuration() + ",parse=" + report.getParsingDuration() + ",exec=" + report.getExecutionDuration() + ",format[" + report.parameters.getFormat() + "]=" + report.getFormattingDuration() + ")";
				}else if (event.equalsIgnoreCase("WRITING_ERROR"))
					jobId = obj.toString();
			}
		}catch(Throwable t){
			error("Error while preparing a log message in logTAP(...)! The message will be logger but without additional information such as the job ID.", t);
		}

		// Log the message:
		log(level, tapLogger, event, jobId, message, msgAppend, error);
	}

}
