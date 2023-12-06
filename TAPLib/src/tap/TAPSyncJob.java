package tap;

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
 * Copyright 2012-2020 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import tap.parameters.TAPExecutionDurationController;
import tap.parameters.TAPParameters;
import uws.UWSException;
import uws.job.JobThread;
import uws.job.UWSJob;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.UploadFile;

/**
 * This class represent a TAP synchronous job. A such job must execute an ADQL
 * query and return immediately its result.
 *
 * <h3>Timeout</h3>
 *
 * <p>
 * 	The execution of a such job is limited to a short time. Once this time is
 * 	elapsed, the job is stopped. For a longer job, an asynchronous job should be
 * 	used.
 * </p>
 *
 * <p>
 * 	The maximum execution duration of a synchronous job is determined by
 * 	{@link #determineMaxExecutionDuration()}.
 * </p>
 *
 * <h3>Error management</h3>
 *
 * <p>
 * 	If an error occurs it must be propagated ; it will be written later in the
 * 	HTTP response on a top level.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (08/2020)
 */
public class TAPSyncJob {

	/** Ultimate execution duration (in milliseconds) to use if not a single
	 * alternative for this duration can be found.
	 * @since 2.4 */
	protected final long MAX_DURATION_FALLBACK = 10000;

	/** The time (in ms) to wait the end of the thread after an interruption. */
	protected long waitForStop = 1000;

	/** Last generated ID of a synchronous job. */
	protected static String lastId = "S" + System.currentTimeMillis() + "A";

	/** Description of the TAP service in charge of this synchronous job. */
	protected final ServiceConnection service;

	/** ID of this job. This ID is also used to identify the thread. */
	protected final String ID;

	/** Parameters of the execution. It mainly contains the ADQL query to
	 * execute. */
	protected final TAPParameters tapParams;

	/** The thread in which the query execution will be done. */
	protected SyncThread thread;

	/** Report of the query execution. It stays NULL until the execution ends. */
	protected TAPExecutionReport execReport = null;

	/** Date at which this synchronous job has really started. It is NULL when
	 * the job has never been started.
	 *
	 * <p><i><b>Note:</b>
	 * 	A synchronous job can be run just once ; so if an attempt of executing
	 * 	it again, the start date will be tested: if NULL, the second starting is
	 * 	not considered and an exception is thrown.
	 * </i></p> */
	private Date startedAt = null;

	/**
	 * Create a synchronous TAP job.
	 *
	 * @param service	Description of the TAP service which is in charge of
	 *               	this synchronous job.
	 * @param params	Parameters of the query to execute. It must mainly
	 *              	contain the ADQL query to execute.
	 *
	 * @throws NullPointerException	If one of the parameters is NULL.
	 */
	public TAPSyncJob(final ServiceConnection service, final TAPParameters params) throws NullPointerException {
		if (params == null)
			throw new NullPointerException("Missing TAP parameters ! => Impossible to create a synchronous TAP job.");
		tapParams = params;
		tapParams.init();

		if (service == null)
			throw new NullPointerException("Missing the service description ! => Impossible to create a synchronous TAP job.");
		this.service = service;

		ID = generateId();
	}

	/**
	 * Create a synchronous TAP job.
	 * The given HTTP request ID will be used as Job ID if not already used by
	 * another job.
	 *
	 * @param service	Description of the TAP service which is in charge of
	 *               	this synchronous job.
	 * @param params	Parameters of the query to execute. It must mainly
	 *              	contain the ADQL query to execute.
	 * @param requestID	ID of the HTTP request which has initiated the creation
	 *                 	of this job.
	 *                 	<i>Note: if NULL, empty or already used, a job ID will
	 *                 	be generated thanks to {@link #generateId()}.</i>
	 *
	 * @throws NullPointerException	If one of the 2 first parameters is NULL.
	 *
	 * @since 2.1
	 */
	public TAPSyncJob(final ServiceConnection service, final TAPParameters params, final String requestID) throws NullPointerException {
		if (params == null)
			throw new NullPointerException("Missing TAP parameters ! => Impossible to create a synchronous TAP job.");
		tapParams = params;
		tapParams.init();

		if (service == null)
			throw new NullPointerException("Missing the service description ! => Impossible to create a synchronous TAP job.");
		this.service = service;

		synchronized (lastId) {
			if (requestID == null || requestID.trim().length() == 0 || lastId.equals(requestID))
				ID = generateId();
			else {
				ID = requestID;
				lastId = requestID;
			}
		}
	}

	/**
	 * This function lets generating a unique ID.
	 *
	 * <p><i><b>By default:</b>
	 * 	"S"+System.currentTimeMillis()+UpperCharacter (UpperCharacter:
	 * 	one upper-case character: A, B, C, ....)
	 * </i></p>
	 *
	 * <p><i><b>Note: </b>
	 * 	DO NOT USE in this function any of the following functions:
	 * 	{@link ServiceConnection#getLogger()},
	 * 	{@link ServiceConnection#getFileManager()} and
	 * 	{@link ServiceConnection#getFactory()}. All of them will return NULL,
	 * 	because this job does not yet know its jobs list (which is needed to
	 * 	know the UWS and so, all of the objects returned by these functions).
	 * </i></p>
	 *
	 * @return	A unique job identifier.
	 */
	protected String generateId() {
		synchronized (lastId) {
			String generatedId = "S" + System.currentTimeMillis() + "A";
			if (lastId != null) {
				while(lastId.equals(generatedId))
					generatedId = generatedId.substring(0, generatedId.length() - 1) + (char)(generatedId.charAt(generatedId.length() - 1) + 1);
			}
			lastId = generatedId;
			return generatedId;
		}
	}

	/**
	 * Get the ID of this synchronous job.
	 *
	 * @return	The job ID.
	 */
	public final String getID() {
		return ID;
	}

	/**
	 * Get the TAP parameters provided by the user and which will be used for
	 * the execution of this job.
	 *
	 * @return	Job parameters.
	 */
	public final TAPParameters getTapParams() {
		return tapParams;
	}

	/**
	 * Get the report of the execution of this job.
	 * This report is NULL if the execution has not yet started.
	 *
	 * @return	Report of this job execution.
	 */
	public final TAPExecutionReport getExecReport() {
		return execReport;
	}

	/**
	 * Start the execution of this job in order to execute the given ADQL query.
	 *
	 * <p>
	 * 	The execution itself will be processed by an {@link ADQLExecutor} inside
	 * 	a thread ({@link SyncThread}).
	 * </p>
	 *
	 * <p><i><b>Important:</b>
	 * 	No error should be written in this function. If any error occurs it
	 * 	should be thrown, in order to be manager on a top level.
	 * </i></p>
	 *
	 * @param response	Response in which the result must be written.
	 *
	 * @return	<code>true</code> if the execution was successful,
	 *        	<code>false</code> otherwise.
	 *
	 * @throws IllegalStateException	If this synchronous job has already been
	 *                              	started before.
	 * @throws IOException				If any error occurs while writing the
	 *                    				query result in the given
	 *                    				{@link HttpServletResponse}.
	 * @throws TAPException				If any error occurs while executing the
	 *                     				ADQL query.
	 *
	 * @see SyncThread
	 */
	public synchronized boolean start(final HttpServletResponse response) throws IllegalStateException, IOException, TAPException {
		if (startedAt != null)
			throw new IllegalStateException("Impossible to restart a synchronous TAP query!");

		// Log the start of this sync job:
		service.getLogger().logTAP(LogLevel.INFO, this, "START", "Synchronous job " + ID + " is starting!", null);

		// Create the object having the knowledge about how to execute an ADQL query:
		ADQLExecutor executor = service.getFactory().createADQLExecutor();
		try {
			executor.initDBConnection(ID);
		} catch(TAPException te) {
			service.getLogger().logDB(LogLevel.ERROR, null, "CONNECTION_LACK", "No more database connection available for the moment!", te);
			service.getLogger().logTAP(LogLevel.ERROR, this, "END", "Synchronous job " + ID + " execution aborted: no database connection available!", null);
			throw new TAPException("TAP service too busy! No connection available for the moment. You should try later or create an asynchronous query (which will be executed when enough resources will be available again).", UWSException.SERVICE_UNAVAILABLE);
		}

		// Determine the maximum execution duration (in milliseconds):
		final long timeToStop = determineMaxExecutionDuration();

		// Give to a thread which will execute the query:
		thread = new SyncThread(executor, ID, tapParams, response);
		thread.start();

		// Wait the end of the thread until the maximum execution duration is reached:
		boolean timeout = false;
		try {
			// wait the end:
			thread.join(timeToStop);
			// if still alive after this duration, interrupt it:
			if (thread.isAlive()) {
				timeout = true;
				thread.interrupt();
				thread.join(waitForStop);
				// Log the timeout:
				if (thread.isAlive())
					service.getLogger().logTAP(LogLevel.WARNING, this, "TIME_OUT", "Time out (after " + (timeToStop / 1000) + " seconds) for the synchonous job " + ID + ", but the thread can not be interrupted!", null);
				else
					service.getLogger().logTAP(LogLevel.INFO, this, "TIME_OUT", "Time out (after " + (timeToStop / 1000) + " seconds) for the synchonous job " + ID + ".", null);
			}
		} catch(InterruptedException ie) {
			/* Having a such exception here, is not surprising, because we may have interrupted the thread! */
		} finally {
			// Whatever the way the execution stops (normal, cancel or error), an execution report must be fulfilled:
			execReport = thread.getExecutionReport();

			// Delete uploaded files:
			deleteUploads(tapParams);
		}

		// Report any error that may have occurred while the thread execution:
		Throwable error = thread.getError();
		// CASE: TIMEOUT
		if (timeout && error != null && error instanceof InterruptedException) {
			// Report the timeout to the user:
			throw new TAPException("Time out! The execution of this synchronous TAP query was limited to " + tapParams.getExecutionDuration() + " seconds. You should try again but in asynchronous mode.", UWSException.ACCEPTED_BUT_NOT_COMPLETE);
		}
		// CASE: ERRORS
		else if (!thread.isSuccess()) {
			// INTERRUPTION:
			if (error instanceof InterruptedException) {
				// log the unexpected interruption (unexpected because not caused by a timeout):
				service.getLogger().logTAP(LogLevel.ERROR, this, "END", "The execution of the synchronous job " + ID + " has been unexpectedly interrupted!", error);
				// report the unexpected interruption to the user:
				throw new TAPException("The execution of this synchronous job " + ID + " has been unexpectedly aborted!", UWSException.ACCEPTED_BUT_NOT_COMPLETE);
			}
			// REQUEST ABORTION:
			else if (error instanceof IOException) {
				// log the unexpected interruption (unexpected because not caused by a timeout):
				service.getLogger().logTAP(LogLevel.INFO, this, "END", "Abortion of the synchronous job " + ID + "! Cause: connection with the HTTP client unexpectedly closed.", error);
				// throw the error until the TAP instance to notify it about the abortion:
				throw (IOException)error;
			}
			// TAP EXCEPTION:
			else if (error instanceof TAPException) {
				// log the error:
				service.getLogger().logTAP(LogLevel.ERROR, this, "END", "The following error interrupted the execution of the synchronous job " + ID + ".", error);
				// report the error to the user:
				throw (TAPException)error;
			}
			// ANY OTHER EXCEPTION:
			else {
				// log the error:
				service.getLogger().logTAP(LogLevel.FATAL, this, "END", "The following GRAVE error interrupted the execution of the synchronous job " + ID + ".", error);
				// report the error to the user:
				if (error == null)
					throw new TAPException("This query (" + ID + ") stopped unexpectedly without any result! No error/exception has been caught and no execution report has been registered. You should contact the service administrator to investigate this.", UWSException.INTERNAL_SERVER_ERROR);
				else if (error instanceof Error)
					throw (Error)error;
				else
					throw new TAPException(error);
			}
		} else
			service.getLogger().logTAP(LogLevel.INFO, this, "END", "Success of the synchronous job " + ID + ".", null);

		return thread.isSuccess();
	}

	/**
	 * Determine the maximum execution duration of this synchronous query.
	 *
	 * <p>By default, this function use the following strategy:</p>
	 * <ul>
	 * 	<li>if set, use the synchronous duration specified in the TAP configuration
	 * 	    (i.e. {@link ServiceConnection#getExecutionDuration()}[2])</li>
	 * 	<li>if none is specified, then use the default execution duration
	 * 	    (i.e. {@link ServiceConnection#getExecutionDuration()}[0])</li>
	 * 	<li>if none is specified either, use the maximum execution duration
	 * 	    (i.e. {@link ServiceConnection#getExecutionDuration()}[1])</li>
	 * 	<li>if still none is specified, try to see if an execution duration is
	 * 	    provided in the HTTP request (using the corresponding UWS' parameter)
	 * 	    and use it</li>
	 * 	<li>in last chance, the execution is set to 60 seconds.</li>
	 * </ul>
	 * <p><i>
	 * 	This default strategy aims to avoid an unlimited execution duration in
	 * 	synchronous mode.
	 * </i></p>
	 *
	 * @return	The maximum execution duration of this synchronous query
	 *        	(in milliseconds) or {@link UWSJob#UNLIMITED_DURATION} for no
	 *        	limit at all.
	 *
	 * @since 2.4
	 */
	protected long determineMaxExecutionDuration() {

		long timeToStop = TAPJob.UNLIMITED_DURATION;

		// Try to use the durations set in the TAP configuration:
		if (service.getExecutionDuration() != null) {
			// use the synchronous execution duration (if any specified):
			if (service.getExecutionDuration().length >= 3 && service.getExecutionDuration()[2] > 0)
				timeToStop = service.getExecutionDuration()[2];
			// otherwise, just use the default value:
			else
				timeToStop = ((Long)(new TAPExecutionDurationController(service)).getDefault()) * 1000;
		}

		/* If the duration is still unlimited, try to see if a duration is
		 * given in the HTTP request (in the UWS way) and use it: */
		if (timeToStop <= 0)
			timeToStop = tapParams.getExecutionDuration() * 1000;

		/* In order to prevent an unlimited execution duration in synchronous
		 * mode (which should not happen), set a hard coded limit (60 seconds): */
		if (timeToStop <= 0)
			timeToStop = MAX_DURATION_FALLBACK;

		return timeToStop;
	}

	/**
	 * Delete all uploaded files.
	 *
	 * @param tapParams	Input parameters (listing all uploaded files, if any).
	 *
	 * @since 2.3
	 */
	protected void deleteUploads(final TAPParameters tapParams) {
		Iterator<UploadFile> itFiles = tapParams.getFiles();
		while(itFiles.hasNext()) {
			UploadFile uf = itFiles.next();
			try {
				uf.deleteFile();
			} catch(IOException ioe) {
				service.getLogger().logTAP(LogLevel.WARNING, this, "END", "Unable to delete the uploaded file \"" + uf.getLocation() + "\"!", ioe);
			}
		}
	}

	/**
	 * Thread which will process the job execution.
	 *
	 * <p>
	 * 	Actually, it will basically just call
	 * 	{@link ADQLExecutor#start(Thread, String, TAPParameters, HttpServletResponse)}
	 * 	with the given {@link ADQLExecutor} and TAP parameters (containing the
	 * 	ADQL query to execute).
	 * </p>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.1 (03/2017)
	 */
	protected class SyncThread extends Thread {

		/** Object knowing how to execute an ADQL query and which will execute
		 * it by calling {@link ADQLExecutor#start(Thread, String, TAPParameters, HttpServletResponse)}. */
		protected final ADQLExecutor executor;
		/** Response in which the query result must be written. No error should
		 * be written in it directly at this level ; the error must be
		 * propagated and it will be written in this HTTP response later on a
		 * top level. */
		protected final HttpServletResponse response;
		/** ID of this thread. It is also the ID of the synchronous job owning
		 * this thread. */
		protected final String ID;
		/** Parameters containing the ADQL query to execute and other execution
		 * parameters/options. */
		protected final TAPParameters tapParams;

		/** Exception that occurs while executing this thread. NULL if the
		 * execution was a success. */
		protected Throwable exception = null;
		/** Query execution report. NULL if the execution has not yet started. */
		protected TAPExecutionReport report = null;

		/**
		 * Create a thread that will run the given executor with the given
		 * parameters.
		 *
		 * @param executor	Object to execute and which knows how to execute an
		 *                	ADQL query.
		 * @param ID		ID of the synchronous job owning this thread.
		 * @param tapParams	TAP parameters to use to get the query to execute
		 *                	and the execution parameters.
		 * @param response	HTTP response in which the ADQL query result must be
		 *                	written.
		 */
		public SyncThread(final ADQLExecutor executor, final String ID, final TAPParameters tapParams, final HttpServletResponse response) {
			super(JobThread.tg, ID);
			this.executor = executor;
			this.ID = ID;
			this.tapParams = tapParams;
			this.response = response;
		}

		/**
		 * Tell whether the execution has ended with success.
		 *
		 * @return	<code>true</code> if the query has been successfully
		 *        	executed,
		 *        	<code>false</code> otherwise (or if this thread is still
		 *        	executed).
		 */
		public final boolean isSuccess() {
			return !isAlive() && report != null && exception == null;
		}

		/**
		 * Get the error that has interrupted/stopped this thread.
		 * This function returns NULL if the query has been successfully
		 * executed.
		 *
		 * @return	Error that occurs while executing the query
		 *        	or NULL if the execution was a success.
		 */
		public final Throwable getError() {
			return exception;
		}

		/**
		 * Get the report of the query execution.
		 *
		 * @return	Query execution report.
		 */
		public final TAPExecutionReport getExecutionReport() {
			return report;
		}

		@Override
		public void interrupt() {
			super.interrupt();
			executor.cancelQuery();
		}

		@Override
		public void run() {
			// Log the start of this thread:
			executor.getLogger().logThread(LogLevel.INFO, thread, "START", "Synchronous thread \"" + ID + "\" started.", null);

			try {
				// Execute the ADQL query:
				report = executor.start(this, ID, tapParams, response);

				// Log the successful end of this thread:
				executor.getLogger().logThread(LogLevel.INFO, thread, "END", "Synchronous thread \"" + ID + "\" successfully ended.", null);

			} catch(Throwable e) {

				// Save the exception for later reporting:
				exception = e;

				// Log the end of the job:
				if (e instanceof InterruptedException || e instanceof IOException)
					// Abortion:
					executor.getLogger().logThread(LogLevel.INFO, this, "END", "Synchronous thread \"" + ID + "\" cancelled.", null);
				else if (e instanceof TAPException)
					// Error:
					executor.getLogger().logThread(LogLevel.ERROR, this, "END", "Synchronous thread \"" + ID + "\" ended with an error.", null);
				else
					// GRAVE error:
					executor.getLogger().logThread(LogLevel.FATAL, this, "END", "Synchronous thread \"" + ID + "\" ended with a FATAL error.", null);
			}
		}

	}

}
