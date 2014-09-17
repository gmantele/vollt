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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import tap.TAPExecutionReport;
import tap.TAPSyncJob;
import tap.db.DBConnection;
import tap.parameters.TAPParameters;
import uws.service.log.UWSLog;

/**
 * Let log any kind of message about a TAP service.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public interface TAPLog extends UWSLog {

	/**
	 * <p>Log a message and/or an error in the DB (database) context.</p>
	 * 
	 * <p>List of all events sent by the library (case sensitive):</p>
	 * <ul>
	 * 	<li>TRANSLATE</li>
	 * 	<li>EXECUTE</li>
	 * 	<li>RESULT</li>
	 * 	<li>LOAD_TAP_SCHEMA</li>
	 * 	<li>CLEAN_TAP_SCHEMA</li>
	 * 	<li>CREATE_TAP_SCHEMA</li>
	 * 	<li>TABLE_EXIST</li>
	 * 	<li>EXEC_UPDATE</li>
	 * 	<li>ADD_UPLOAD_TABLE</li>
	 * 	<li>DROP_UPLOAD_TABLE</li>
	 * 	<li>START_TRANSACTION</li>
	 * 	<li>COMMIT</li>
	 * 	<li>ROLLBACK</li>
	 * 	<li>END_TRANSACTION</li>
	 * 	<li>CLOSE</li>
	 * </ul>
	 * 
	 * @param level			Level of the log (info, warning, error, ...). <i>SHOULD NOT be NULL, but if NULL anyway, the level SHOULD be considered as INFO</i>
	 * @param connection	DB connection from which this log comes. <i>MAY be NULL</i>
	 * @param event			Event at the origin of this log or action executed by the given database connection while this log is sent. <i>MAY be NULL</i>
	 * @param message		Message to log. <i>MAY be NULL</i>
	 * @param error			Error/Exception to log. <i>MAY be NULL</i>
	 * 
	 * @since 2.0
	 */
	public void logDB(final LogLevel level, final DBConnection connection, final String event, final String message, final Throwable error);

	/**
	 * <p>Log a message and/or an error in the general context of TAP.</p>
	 * 
	 * <p>
	 * 	One of the parameter is of type {@link Object}. This object can be used to provide more information to the log function
	 * 	in order to describe as much as possible the state and/or result event.
	 * </p>
	 * 
	 * <p>List of all events sent by the library (case sensitive):</p>
	 * <ul>
	 * 	<li>SYNC_INIT (with "obj" as an instance of {@link TAPParameters})</li>
	 * 	<li>ASYNC_INIT (with a NULL "obj")</li>
	 * 	<li>SYNC_START (with "obj" as an instance of {@link TAPSyncJob})</li>
	 * 	<li>UPLOAD (with "obj" as an instance of {@link TAPExecutionReport})</li>
	 * 	<li>FORMAT (with "obj" as an instance of {@link TAPExecutionReport})</li>
	 * 	<li>START_STEP (with "obj" as an instance of {@link TAPExecutionReport})</li>
	 * 	<li>END_EXEC (with "obj" as an instance of {@link TAPExecutionReport})</li>
	 * 	<li>END_QUERY (with "obj" as an instance of {@link TAPExecutionReport})</li>
	 * 	<li>DROP_UPLOAD (with "obj" as an instance of {@link TAPExecutionReport})</li>
	 * </ul>
	 * 
	 * @param level		Level of the log (info, warning, error, ...). <i>SHOULD NOT be NULL, but if NULL anyway, the level SHOULD be considered as INFO</i>
	 * @param obj		Object providing more information about the event/object at the origin of this log. <i>MAY be NULL</i>
	 * @param event		Event at the origin of this log or action currently executed by TAP while this log is sent. <i>MAY be NULL</i>
	 * @param message	Message to log. <i>MAY be NULL</i>
	 * @param error		Error/Exception to log. <i>MAY be NULL</i>
	 * 
	 * @since 2.0
	 */
	public void logTAP(final LogLevel level, final Object obj, final String event, final String message, final Throwable error);

}
