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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;

import tap.TAPExecutionReport;
import tap.TAPSyncJob;
import tap.db.DBConnection;
import tap.parameters.TAPParameters;
import uws.service.file.UWSFileManager;
import uws.service.log.DefaultUWSLog;

/**
 * Default implementation of the {@link TAPLog} interface which lets logging any message about a TAP service.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (12/2014)
 * 
 * @see DefaultUWSLog
 */
public class DefaultTAPLog extends DefaultUWSLog implements TAPLog {

	/**
	 * <p>Builds a {@link TAPLog} which will use the given file
	 * manager to get the log output (see {@link UWSFileManager#getLogOutput(LogLevel, String)}).</p>
	 * 
	 * <p><i><u>note 1</u>: This constructor is particularly useful if the way of managing log output may change in the given file manager.
	 * Indeed, the output may change in function of the type of message to log ({@link LogLevel}).</i></p>
	 * 
	 * <p><i><u>note 2</u> If no output can be found in the file manager the standard error output ({@link System#err})
	 * will be chosen automatically for all log messages.</i></p>
	 * 
	 * @param fm	A TAP file manager.
	 * 
	 * @see DefaultUWSLog#DefaultUWSLog(UWSFileManager)
	 */
	public DefaultTAPLog(final UWSFileManager fm){
		super(fm);
	}

	/**
	 * <p>Builds a {@link TAPLog} which will print all its
	 * messages into the given stream.</p>
	 * 
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link LogLevel}).</i></p>
	 * 
	 * @param output	An output stream.
	 * 
	 * @see DefaultUWSLog#DefaultUWSLog(OutputStream)
	 */
	public DefaultTAPLog(final OutputStream output){
		super(output);
	}

	/**
	 * <p>Builds a {@link TAPLog} which will print all its
	 * messages into the given stream.</p>
	 * 
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link LogLevel}).</i></p>
	 * 
	 * @param writer	A print writer.
	 * 
	 * @see DefaultUWSLog#DefaultUWSLog(PrintWriter)
	 */
	public DefaultTAPLog(final PrintWriter writer){
		super(writer);
	}

	@Override
	public void logDB(final LogLevel level, final DBConnection connection, final String event, final String message, final Throwable error){
		// log the main given error:
		log(level, "DB", event, (connection != null ? connection.getID() : null), message, error);

		/* Some SQL exceptions (like BatchUpdateException) have a next exception which provides more information.
		 * Here, the stack trace of the next exception is also logged:
		 */
		if (error != null && error instanceof SQLException && ((SQLException)error).getNextException() != null){
			PrintWriter out = getOutput(level, "DB");
			out.println("[NEXT EXCEPTION]");
			((SQLException)error).getNextException().printStackTrace(out);
			out.flush();
		}
	}

	@Override
	public void logTAP(final LogLevel level, final Object obj, final String event, final String message, final Throwable error){
		// Get more information (when known event and available object):
		String jobId = null, msgAppend = null;
		try{
			if (event != null && obj != null){
				if (event.equals("SYNC_INIT"))
					msgAppend = "QUERY=" + ((TAPParameters)obj).getQuery().replaceAll("(\t|\r?\n)+", " ");
				else if (event.equals("SYNC_START"))
					jobId = ((TAPSyncJob)obj).getID();
				else if (obj instanceof TAPExecutionReport){
					TAPExecutionReport report = (TAPExecutionReport)obj;
					jobId = report.jobID;
					msgAppend = (report.synchronous ? "SYNC" : "ASYNC") + ",duration=" + report.getTotalDuration() + "ms (upload=" + report.getUploadDuration() + ",parse=" + report.getParsingDuration() + ",exec=" + report.getExecutionDuration() + ",format[" + report.parameters.getFormat() + "]=" + report.getFormattingDuration() + ")";
				}
			}
			if (msgAppend != null)
				msgAppend = "\t" + msgAppend;
		}catch(Throwable t){
			error("Error while preparing a log message in logTAP(...)! The message will be logger but without additional information such as the job ID.", t);
		}

		// Log the message:
		log(level, "TAP", event, jobId, message + (msgAppend != null ? msgAppend : ""), error);
	}

}
