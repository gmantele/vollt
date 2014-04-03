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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;

import uws.job.JobList;
import uws.job.UWSJob;

import uws.job.user.JobOwner;

import uws.service.UWS;

import uws.service.file.UWSFileManager;

/**
 * <p>Default implementation of {@link UWSLog} interface which lets logging any message about a UWS.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public class DefaultUWSLog implements UWSLog {

	/** Format to use to serialize all encountered dates. */
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	protected final UWS uws;
	protected final UWSFileManager fileManager;
	protected final PrintWriter defaultOutput;

	/**
	 * <p>The minimum value of the HTTP status code required to print the stack trace of a HTTP error.</p>
	 * <p><i><u>note:</u> This value is used only by the function {@link #httpRequest(HttpServletRequest, JobOwner, String, int, String, Throwable)}. </i></p>
	 */
	protected int minResponseCodeForStackTrace = 500;

	/**
	 * <p>Builds a {@link UWSLog} which will use the file manager
	 * of the given UWS to get the log output (see {@link UWSFileManager#getLogOutput(UWSLogType)}).</p>
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
	 * manager to get the log output (see {@link UWSFileManager#getLogOutput(UWSLogType)}).</p>
	 * 
	 * <p><i><u>note 1</u>: This constructor is particularly useful if the way of managing log output may change in the given file manager.
	 * Indeed, the output may change in function of the type of message to log ({@link UWSLogType}).</i></p>
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
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link UWSLogType}).</i></p>
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
	 * <p><i><u>note</u>: the given output will be used whatever is the type of message to log ({@link UWSLogType}).</i></p>
	 * 
	 * @param writer	A print writer.
	 */
	public DefaultUWSLog(final PrintWriter writer){
		uws = null;
		fileManager = null;
		defaultOutput = writer;
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
	 * <p>Gets the minimum value of the HTTP status code required to print the stack trace of a HTTP error.</p>
	 * 
	 * <p><i><u>note:</u> This value is used only by the function {@link #httpRequest(HttpServletRequest, JobOwner, String, int, String, Throwable)}. </i></p>
	 * 
	 * @return	A HTTP response status code.
	 */
	public int getMinResponseCodeForStackTrace(){
		return minResponseCodeForStackTrace;
	}

	/**
	 * <p>Sets the minimum value of the HTTP status code required to print the stack trace of a HTTP error.</p>
	 * 
	 * <p><i><u>note:</u> This value is used only by the function {@link #httpRequest(HttpServletRequest, JobOwner, String, int, String, Throwable)}. </i></p>
	 * 
	 * @param httpCode	A HTTP response status code.
	 */
	public void setMinResponseCodeForStackTrace(final int httpCode){
		minResponseCodeForStackTrace = httpCode;
	}

	/**
	 * <p>Gets an output for the given type of message to print.</p>
	 * 
	 * <p>The {@link System#err} output is used if none can be found in the {@link UWS} or the {@link UWSFileManager}
	 * given at the creation, or if the given output stream or writer is NULL.</p>
	 * 
	 * @param logType	Type of the message to print;
	 * @return			A writer.
	 */
	protected PrintWriter getOutput(final UWSLogType logType){
		try{
			if (uws != null){
				if (uws.getFileManager() != null)
					return uws.getFileManager().getLogOutput(logType);
			}else if (fileManager != null)
				return fileManager.getLogOutput(logType);
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
	 * Logs the given message (and exception, if any).
	 * 
	 * @param type	Type of the message to print. <i><u>note:</u> (If NULL, it will be ERROR if an exception is given, INFO otherwise.)</i>
	 * @param msg	Message to print. (may be NULL)
	 * @param t		Exception to print. (may be NULL)
	 */
	public void log(UWSLogType type, final String msg, final Throwable t){
		// If the type is missing:
		if (type == null)
			type = (t != null) ? UWSLogType.ERROR : UWSLogType.INFO;

		PrintWriter out = getOutput(type);
		// Print the date/time:
		out.print(dateFormat.format(new Date()));
		out.print('\t');
		out.print(String.format("%1$-13s", type.toString()));
		out.print('\t');
		// Print the message:
		if (msg != null)
			out.println(msg);
		else if (t != null && t instanceof UWSException){
			UWSException uwsEx = (UWSException)t;
			out.println("EXCEPTION " + uwsEx.getClass().getName() + "\t" + uwsEx.getUWSErrorType() + "\tHTTP-" + uwsEx.getHttpErrorCode() + "\t" + uwsEx.getMessage());
		}else
			out.println();
		// Print the stack trace, if any:
		if (t != null)
			t.printStackTrace(out);
		out.flush();
	}

	@Override
	public void debug(String msg){
		log(UWSLogType.DEBUG, msg, null);
	}

	@Override
	public void debug(Throwable t){
		log(UWSLogType.DEBUG, null, t);
	}

	@Override
	public void debug(String msg, Throwable t){
		log(UWSLogType.DEBUG, msg, t);
	}

	@Override
	public void info(String msg){
		log(UWSLogType.INFO, msg, null);
	}

	@Override
	public void warning(String msg){
		log(UWSLogType.WARNING, msg, null);
	}

	@Override
	public void error(String msg){
		log(UWSLogType.ERROR, msg, null);
	}

	@Override
	public void error(Throwable t){
		log(UWSLogType.ERROR, null, t);
	}

	@Override
	public void error(String msg, Throwable t){
		log(UWSLogType.ERROR, msg, t);
	}

	/* **************************** */
	/* METHODS ABOUT THE UWS STATUS */
	/* **************************** */

	/**
	 * Gets the name of the UWS, if any.
	 * 
	 * @param uws	UWS whose the name must be returned.
	 * 
	 * @return		Name of the given UWS (followed by a space: " ") or an empty string ("").
	 */
	protected final static String getUWSName(final UWS uws){
		return ((uws != null && uws.getName() != null && !uws.getName().trim().isEmpty()) ? (uws.getName() + " ") : "");
	}

	@Override
	public void uwsInitialized(UWS uws){
		if (uws != null){
			String msg = "UWS " + getUWSName(uws) + "INITIALIZED !";
			info(msg);
			log(UWSLogType.HTTP_ACTIVITY, msg, null);
		}
	}

	@Override
	public void ownerJobsSaved(JobOwner owner, int[] report){
		if (owner != null){
			String strReport = (report == null || report.length != 2) ? "???" : (report[0] + "/" + report[1]);
			String ownerPseudo = (owner.getPseudo() != null && !owner.getPseudo().trim().isEmpty() && !owner.getID().equals(owner.getPseudo())) ? (" (alias " + owner.getPseudo() + ")") : "";
			info(strReport + " saved jobs for the user " + owner.getID() + ownerPseudo + " !");
		}
	}

	@Override
	public void uwsRestored(UWS uws, int[] report){
		if (uws != null){
			String strReport = (report == null || report.length != 4) ? "[Unknown report format !]" : (report[0] + "/" + report[1] + " restored jobs and " + report[2] + "/" + report[3] + " restored users");
			info("UWS " + getUWSName(uws) + "RESTORED => " + strReport);
		}
	}

	@Override
	public void uwsSaved(UWS uws, int[] report){
		if (uws != null){
			String strReport = (report == null || report.length != 4) ? "[Unknown report format !]" : (report[0] + "/" + report[1] + " saved jobs and " + report[2] + "/" + report[3] + " saved users");
			info("UWS " + getUWSName(uws) + "SAVED => " + strReport);
		}
	}

	@Override
	public void jobCreated(UWSJob job){
		if (job != null){
			String jlName = (job.getJobList() != null) ? job.getJobList().getName() : null;
			info("JOB " + job.getJobId() + " CREATED" + ((jlName != null) ? (" and added into " + jlName) : "") + " !");
		}
	}

	@Override
	public void jobDestroyed(UWSJob job, JobList jl){
		if (job != null){
			String jlName = (jl != null) ? jl.getName() : null;
			info("JOB " + job.getJobId() + " DESTROYED" + ((jlName != null) ? (" and removed from " + jlName) : "") + " !");
		}
	}

	@Override
	public void jobStarted(UWSJob job){
		if (job != null){
			info("JOB " + job.getJobId() + " STARTED !");
		}
	}

	@Override
	public void jobFinished(UWSJob job){
		if (job != null){
			long endTime = (job.getEndTime() == null) ? -1 : job.getEndTime().getTime();
			long startTime = (job.getStartTime() == null) ? -1 : job.getStartTime().getTime();
			long duration = (endTime > 0 && startTime > 0) ? (endTime - startTime) : -1;
			info("JOB " + job.getJobId() + " FINISHED with the phase " + job.getPhase() + ((duration > 0) ? " after an execution of " + duration + "ms" : "") + " !");
		}
	}

	/* ************* */
	/* HTTP ACTIVITY */
	/* ************* */

	@SuppressWarnings("unchecked")
	public void httpRequest(final HttpServletRequest request, final JobOwner user, final String uwsAction, final int responseStatusCode, final String responseMsg, final Throwable responseError){
		if (request != null){
			StringBuffer str = new StringBuffer();

			// Write the executed UWS action:
			if (uwsAction == null || uwsAction.trim().isEmpty())
				str.append("???");
			else
				str.append(uwsAction);
			str.append('\t');

			// Write the response status code:
			if (responseStatusCode > 0)
				str.append("HTTP-").append(responseStatusCode);
			else
				str.append("HTTP-???");
			str.append('\t');

			// Write the "response" message:
			if (responseMsg != null)
				str.append('[').append(responseMsg).append(']');
			else
				str.append("[]");
			str.append('\t');

			// Write the request type and the URL:
			str.append("[HTTP-").append(request.getMethod()).append("] ").append(request.getRequestURL()).append('\t');

			// Write the posted parameters:
			Enumeration<String> paramNames = request.getParameterNames();
			while(paramNames.hasMoreElements()){
				String param = paramNames.nextElement();
				String paramValue = request.getParameter(param);
				if (paramValue != null)
					paramValue = paramValue.replaceAll("[\t\n\r]", " ");
				else
					paramValue = "";
				str.append(param).append('=').append(paramValue);
				if (paramNames.hasMoreElements())
					str.append('&');
			}
			str.append('\t');

			// Write the IP address and the corresponding user:
			str.append(request.getRemoteAddr()).append('[');
			if (user != null){
				str.append("id:").append(user.getID());
				if (user.getPseudo() != null)
					str.append(";pseudo:").append(user.getPseudo());
			}else
				str.append("???");
			str.append("]\t");

			// Write the user agent:
			str.append(request.getHeader("User-Agent"));

			// Send the log message to the log file:
			log(UWSLogType.HTTP_ACTIVITY, str.toString(), (responseStatusCode >= minResponseCodeForStackTrace) ? responseError : null);
		}
	}

	/* ********************** */
	/* THREAD STATUS MESSAGES */
	/* ********************** */

	@Override
	public void threadStarted(Thread t, String task){
		if (t != null)
			info("THREAD " + t.getId() + " STARTED\t" + t.getName() + "\t" + t.getState() + "\t" + t.getThreadGroup().activeCount() + " active threads");
	}

	@Override
	public void threadFinished(Thread t, String task){
		if (t != null)
			info("THREAD " + t.getId() + " ENDED\t" + t.getName() + "\t" + t.getState() + "\t" + t.getThreadGroup().activeCount() + " active threads");
	}

	@Override
	public void threadInterrupted(Thread t, String task, Throwable error){
		if (t != null){
			if (error == null || error instanceof InterruptedException)
				info("THREAD " + t.getId() + " CANCELLED\t" + t.getName() + "\t" + t.getState() + "\t" + t.getThreadGroup().activeCount() + " active threads");
			else
				error("THREAD " + t.getId() + " INTERRUPTED\t" + t.getName() + "\t" + t.getState() + "\t" + t.getThreadGroup().activeCount() + " active threads", error);
		}
	}

}
