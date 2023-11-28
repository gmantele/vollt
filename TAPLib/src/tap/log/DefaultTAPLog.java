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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;

import tap.TAPException;
import tap.TAPExecutionReport;
import tap.TAPSyncJob;
import tap.db.DBConnection;
import tap.parameters.TAPParameters;
import uws.UWSException;
import uws.service.file.UWSFileManager;
import uws.service.log.DefaultUWSLog;

/**
 * Default implementation of the {@link TAPLog} interface which lets logging any message about a TAP service.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (04/2015)
 * 
 * @see DefaultUWSLog
 */
public class DefaultTAPLog extends DefaultUWSLog implements TAPLog {

	/**
	 * <p>Builds a {@link TAPLog} which will use the given file
	 * manager to get the log output (see {@link UWSFileManager#getLogOutput(uws.service.log.UWSLog.LogLevel, String)}).</p>
	 * 
	 * <p><i><u>note 1</u>: This constructor is particularly useful if the way of managing log output may change in the given file manager.
	 * Indeed, the output may change in function of the type of message to log ({@link uws.service.log.UWSLog.LogLevel}).</i></p>
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
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link uws.service.log.UWSLog.LogLevel}).</i></p>
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
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link uws.service.log.UWSLog.LogLevel}).</i></p>
	 * 
	 * @param writer	A print writer.
	 * 
	 * @see DefaultUWSLog#DefaultUWSLog(PrintWriter)
	 */
	public DefaultTAPLog(final PrintWriter writer){
		super(writer);
	}

	@Override
	protected void printException(Throwable error, final PrintWriter out){
		if (error != null){
			if (error instanceof UWSException || error instanceof TAPException || error.getClass().getPackage().getName().startsWith("adql.")){
				if (error.getCause() != null)
					printException(error.getCause(), out);
				else{
					out.println("Caused by a " + error.getClass().getName() + " " + getExceptionOrigin(error));
					if (error.getMessage() != null)
						out.println("\t" + error.getMessage());
				}
			}else if (error instanceof SQLException){
				out.println("Caused by a " + error.getClass().getName() + " " + getExceptionOrigin(error));
				out.print("\t");
				do{
					out.println(error.getMessage());
					error = ((SQLException)error).getNextException();
					if (error != null)
						out.print("\t=> ");
				}while(error != null);
			}else{
				out.print("Caused by a ");
				error.printStackTrace(out);
			}
		}
	}

	@Override
	public void logDB(LogLevel level, final DBConnection connection, final String event, final String message, final Throwable error){
		// If the type is missing:
		if (level == null)
			level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

		// Log or not?
		if (!canLog(level))
			return;

		// log the main given error:
		log(level, "DB", event, (connection != null ? connection.getID() : null), message, null, error);

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
	public void logTAP(LogLevel level, final Object obj, final String event, final String message, final Throwable error){
		// If the type is missing:
		if (level == null)
			level = (error != null) ? LogLevel.ERROR : LogLevel.INFO;

		// Log or not?
		if (!canLog(level))
			return;

		// Get more information (when known event and available object):
		String jobId = null, msgAppend = null;
		try{
			if (event != null && obj != null){
				if (event.equals("SYNC_INIT"))
					msgAppend = "QUERY=" + ((TAPParameters)obj).getQuery();
				else if (obj instanceof TAPSyncJob){
					log(level, "JOB", event, ((TAPSyncJob)obj).getID(), message, null, error);
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
		log(level, "TAP", event, jobId, message, msgAppend, error);
	}

}
