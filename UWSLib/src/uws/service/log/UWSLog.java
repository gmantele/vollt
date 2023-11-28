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
 * Copyright 2012-2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import uws.job.ErrorSummary;
import uws.job.JobList;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSUrl;

/**
 * Let log any kind of message about a UWS service.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (07/2018)
 */
public interface UWSLog {

	/**
	 * Indicate the level of the error: debug, info, warning or error.
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 4.1 (09/2014)
	 * @since 4.1
	 */
	public static enum LogLevel {
		DEBUG, INFO, WARNING, ERROR, FATAL;
	}

	/**
	 * Get a string representing the configuration of this logger.
	 *
	 * <p>
	 * 	The result of this function aims to be logged when the logging
	 * 	mechanism is successfully configured and ready to be used.
	 * </p>
	 *
	 * @return	String representing the configuration of this logger.
	 *
	 * @since 4.3.
	 */
	public String getConfigString();

	/* *********************** */
	/* GENERAL LOGGING METHODS */
	/* *********************** */

	/**
	 * Generic way to log a message and/or an exception.
	 *
	 * <p><i>Note:
	 * 	The other functions of this class or extension, MAY be equivalent to a
	 * 	call to this function with some specific parameter values. It should be
	 * 	especially the case for the debug(...), info(...), warning(...) and
	 * 	error(...) functions.
	 * </i></p>
	 *
	 * @param level		Level of the error (info, warning, error, ...).
	 *             		<i>SHOULD NOT be NULL, but if NULL anyway, the level
	 *             		SHOULD be considered as INFO</i>
	 * @param context	Context of the log item (HTTP, Thread, Job, UWS, ...).
	 *               	<i>MAY be NULL</i>
	 * @param message	Message to log. <i>MAY be NULL</i>
	 * @param error		Error/Exception to log. <i>MAY be NULL</i>
	 *
	 * @since 4.1
	 */
	public void log(final LogLevel level, final String context, final String message, final Throwable error);

	/**
	 * Logs a debug message.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.WARNING, null, msg, null)</code>
	 * </i></p>
	 *
	 * @param msg	A DEBUG message.
	 */
	public void debug(final String msg);

	/**
	 * Logs an exception as a debug message.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.WARNING, null, null, t)</code>
	 * </i></p>
	 *
	 * @param t	An exception.
	 */
	public void debug(final Throwable t);

	/**
	 * Logs a full (message+exception) debug message.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.WARNING, null, msg, t)</code>
	 * </i></p>
	 *
	 * @param msg	A DEBUG message.
	 * @param t		An exception.
	 */
	public void debug(final String msg, final Throwable t);

	/**
	 * Logs the given information.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.INFO, null, msg, null)</code>
	 * </i></p>
	 *
	 * @param msg	An INFO message.
	 */
	public void info(final String msg);

	/**
	 * Logs the given warning.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.WARNING, null, msg, null)</code>
	 * </i></p>
	 *
	 * @param msg	A WARNING message.
	 */
	public void warning(final String msg);

	/**
	 * Logs the given error.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.ERROR, null, msg, null)</code>
	 * </i></p>
	 *
	 * @param msg	An ERROR message.
	 */
	public void error(final String msg);

	/**
	 * Logs the given exception as an error.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.ERROR, null, null, t)</code>
	 * </i></p>
	 *
	 * @param t	An exception.
	 */
	public void error(final Throwable t);

	/**
	 * Logs a full (message+exception) error message.
	 *
	 * <p><i>Note:
	 * 	This function should be equals to:
	 * 		<code>log(LogLevel.ERROR, null, msg, t)</code>
	 * </i></p>
	 *
	 * @param msg	An ERROR message.
	 * @param t		An exception.
	 */
	public void error(final String msg, final Throwable t);

	/* ****************** */
	/* SPECIFIC FUNCTIONS */
	/* ****************** */

	/**
	 * Log a message and/or an error in the general context of UWS.
	 *
	 * <p>
	 * 	One of the parameter is of type {@link Object}. This object can be used
	 * 	to provide more information to the log function in order to describe as
	 * 	much as possible the state and/or result event.
	 * </p>
	 *
	 * <p>List of all events sent by the library (case sensitive):</p>
	 * <ul>
	 * 	<li>INIT (with "obj" as an instance of {@link UWS} except in case of
	 * 		error where "obj" is NULL)</li>
	 * 	<li>ADD_JOB_LIST (with "obj" as an instance of {@link JobList})</li>
	 * 	<li>DESTROY_JOB_LIST (with "obj" as an instance of {@link JobList})</li>
	 * 	<li>DESTROY_JOB (with "obj" as an instance of {@link UWSUrl})</li>
	 * 	<li>SERIALIZE (with "obj" as an instance of {@link UWSUrl})</li>
	 * 	<li>SET_PARAM (with "obj" as an instance of {@link HttpServletRequest}
	 * 		in case of error)</li>
	 * 	<li>GET_RESULT (with "obj" as an instance of {@link Result})</li>
	 * 	<li>GET_ERROR (with "obj" as an instance of {@link ErrorSummary})</li>
	 * 	<li>RESTORATION (with "obj" the raw object to de-serialize (may be
	 * 		{@link JSONObject} or {@link JSONArray} or NULL))</li>
	 * 	<li>BACKUP (with "obj" the object to backup ; may be {@link JobOwner},
	 * 		a {@link UWSJob}, ...)</li>
	 * 	<li>RESTORED (with "obj" as an integer array of 4 items: nb of restored
	 * 		jobs, total nb of jobs, nb of restored users, total nb of users)</li>
	 * 	<li>BACKUPED (with "obj" as an integer array of 4 items: nb of saved
	 * 		jobs, total nb of jobs, nb of saved users, total nb of users or with
	 * 		just 2 items (the two last ones))</li>
	 * 	<li>FORMAT_ERROR (with a NULL "obj")</li>
	 * 	<li>STOP (with "obj" as an instance of {@link UWS})</li>
	 * </ul>
	 *
	 * @param level		Level of the log (info, warning, error, ...).
	 *             		<i>SHOULD NOT be NULL, but if NULL anyway, the level
	 *             		SHOULD be considered as INFO</i>
	 * @param obj		Object providing more information about the event/object
	 *           		at the origin of this log. <i>MAY be NULL</i>
	 * @param event		Event at the origin of this log or action currently
	 *             		executed by UWS while this log is sent.
	 *             		<i>MAY be NULL</i>
	 * @param message	Message to log. <i>MAY be NULL</i>
	 * @param error		Error/Exception to log. <i>MAY be NULL</i>
	 *
	 * @since 4.1
	 */
	public void logUWS(final LogLevel level, final Object obj, final String event, final String message, final Throwable error);

	/**
	 * Log a message and/or an error in the HTTP context.
	 * This log function is called when a request is received by the service.
	 * Consequently, the event is: REQUEST_RECEIVED.
	 *
	 * <p><i>Note:
	 * 	When a request is received, this function is called, and then, when the
	 * 	response has been written and sent to the client,
	 * 	{@link #logHttp(LogLevel, HttpServletResponse, String, JobOwner, String, Throwable)}
	 * 	should be called. These functions should always work together.
	 * </i></p>
	 *
	 * @param level		Level of the log (info, warning, error, ...).
	 *             		<i>SHOULD NOT be NULL, but if NULL anyway, the level
	 *             		SHOULD be considered as INFO</i>
	 * @param request	HTTP request received by the service.
	 *               	<i>SHOULD NOT be NULL</i>
	 * @param requestId	ID to use to identify this request until its response is
	 *                 	sent.
	 * @param message	Message to log. <i>MAY be NULL</i>
	 * @param error		Error/Exception to log. <i>MAY be NULL</i>
	 *
	 * @see #logHttp(LogLevel, HttpServletResponse, String, JobOwner, String, Throwable)
	 *
	 * @since 4.1
	 */
	public void logHttp(final LogLevel level, final HttpServletRequest request, final String requestId, final String message, final Throwable error);

	/**
	 * Log a message and/or an error in the HTTP context.
	 * This log function is called when a response is sent to the client by the
	 * service. Consequently, the event is: RESPONSE_SENT.
	 *
	 * <p><i>Note:
	 * 	When a request is received, {@link #logHttp(LogLevel, HttpServletRequest, String, String, Throwable)}
	 * 	is called, and then, when the response has been written and sent to the
	 * 	client, this function should be called. These functions should always
	 * 	work together.
	 * </i></p>
	 *
	 * @param level		Level of the log (info, warning, error, ...).
	 *             		<i>SHOULD NOT be NULL, but if NULL anyway, the level
	 *             		SHOULD be considered as INFO</i>
	 * @param response	HTTP response sent by the service to the client.
	 *                	<i>MAY be NULL if an error occurs while writing the
	 *                	response</i>
	 * @param requestId	ID to use to identify the request to which the given
	 *                 	response is answering.
	 * @param user		Identified user which has sent the received request.
	 * @param message	Message to log. <i>MAY be NULL</i>
	 * @param error		Error/Exception to log. <i>MAY be NULL</i>
	 *
	 * @see #logHttp(LogLevel, HttpServletRequest, String, String, Throwable)
	 *
	 * @since 4.1
	 */
	public void logHttp(final LogLevel level, final HttpServletResponse response, final String requestId, final JobOwner user, final String message, final Throwable error);

	/**
	 * Log a message and/or an error in the JOB context.
	 *
	 * <p>List of all events sent by the library (case sensitive):</p>
	 * <ul>
	 * 	<li>CREATED</li>
	 * 	<li>QUEUE</li>
	 * 	<li>START</li>
	 * 	<li>ABORT</li>
	 * 	<li>ERROR</li>
	 * 	<li>ARCHIVE</li>
	 * 	<li>EXECUTING</li>
	 * 	<li>CHANGE_PHASE</li>
	 * 	<li>NOTIFY</li>
	 * 	<li>END</li>
	 * 	<li>SERIALIZE</li>
	 * 	<li>MOVE_UPLOAD</li>
	 * 	<li>ADD_RESULT</li>
	 * 	<li>SET_DESTRUCTION</li>
	 * 	<li>SET_ERROR</li>
	 * 	<li>CLEAR_RESOURCES</li>
	 * 	<li>DESTROY</li>
	 * </ul>
	 *
	 * @param level		Level of the log (info, warning, error, ...).
	 *             		<i>SHOULD NOT be NULL, but if NULL anyway, the level
	 *             		SHOULD be considered as INFO</i>
	 * @param job		Job from which this log comes. <i>MAY be NULL</i>
	 * @param event		Event at the origin of this log or action executed by
	 *             		the given job while this log is sent. <i>MAY be NULL</i>
	 * @param message	Message to log. <i>MAY be NULL</i>
	 * @param error		Error/Exception to log. <i>MAY be NULL</i>
	 *
	 * @since 4.1
	 */
	public void logJob(final LogLevel level, final UWSJob job, final String event, final String message, final Throwable error);

	/**
	 * <p>Log a message and/or an error in the THREAD context.</p>
	 *
	 * <p>List of all events sent by the library (case sensitive):</p>
	 * <ul>
	 * 	<li>START</li>
	 * 	<li>SET_ERROR</li>
	 * 	<li>END</li>
	 * </ul>
	 *
	 * @param level		Level of the log (info, warning, error, ...).
	 *             		<i>SHOULD NOT be NULL, but if NULL anyway, the level
	 *             		SHOULD be considered as INFO</i>
	 * @param thread	Thread from which this log comes. <i>MAY be NULL</i>
	 * @param event		Event at the origin of this log or action currently
	 *             		executed by the given thread while this log is sent.
	 *             		<i>MAY be NULL</i>
	 * @param message	Message to log. <i>MAY be NULL</i>
	 * @param error		Error/Exception to log. <i>MAY be NULL</i>
	 *
	 * @since 4.1
	 */
	public void logThread(final LogLevel level, final Thread thread, final String event, final String message, final Throwable error);

}
