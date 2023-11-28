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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Date;
import java.util.List;

import tap.log.TAPLog;
import tap.parameters.DALIUpload;
import tap.parameters.TAPParameters;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.ExecutionPhase;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.log.UWSLog.LogLevel;

/**
 * Description of a TAP job. This class is used for asynchronous but also
 * synchronous queries.
 *
 * <p>
 * 	On the contrary to {@link UWSJob}, it is loading parameters from
 * 	{@link TAPParameters} instances rather than {@link UWSParameters}. However,
 * 	{@link TAPParameters} is an extension of {@link UWSParameters}. That's what
 * 	allow the UWS library to use both {@link TAPJob} and {@link TAPParameters}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.2 (09/2017)
 */
public class TAPJob extends UWSJob {
	private static final long serialVersionUID = 1L;

	/** Name of the standard TAP parameter which specifies the type of request
	 * to execute: "REQUEST". */
	public static final String PARAM_REQUEST = "request";
	/** REQUEST value meaning an ADQL query must be executed: "doQuery". */
	public static final String REQUEST_DO_QUERY = "doQuery";
	/** REQUEST value meaning VO service capabilities must be returned:
	 * "getCapabilities". */
	public static final String REQUEST_GET_CAPABILITIES = "getCapabilities";

	/** Name of the standard TAP parameter which specifies the query language:
	 * "LANG". <i>(only the ADQL language is supported by default in this
	 * version of the library)</i> */
	public static final String PARAM_LANGUAGE = "lang";
	/** LANG value meaning ADQL language: "ADQL". */
	public static final String LANG_ADQL = "ADQL";
	/** LANG value meaning PQL language: "PQL". <i>(this language is not
	 * supported in this version of the library)</i> */
	public static final String LANG_PQL = "PQL";

	/** Name of the standard TAP parameter which specifies the version of the
	 * TAP protocol that must be used: "VERSION". <i>(only the version 1.0 is
	 * supported in this version of the library)</i> */
	public static final String PARAM_VERSION = "version";
	/** VERSION value meaning the version 1.0 of TAP: "1.0". */
	public static final String VERSION_1_0 = "1.0";

	/** Name of the standard TAP parameter which specifies the output format
	 * (format of a query result): "FORMAT". */
	public static final String PARAM_FORMAT = "format";
	/** FORMAT value meaning the VOTable format: "votable". */
	public static final String FORMAT_VOTABLE = "votable";

	/** Name of the standard TAP parameter which specifies the maximum number of
	 * rows that must be returned in the query result: "MAXREC". */
	public static final String PARAM_MAX_REC = "maxRec";
	/** Special MAXREC value meaning the number of output rows is not limited. */
	public static final int UNLIMITED_MAX_REC = -1;

	/** Name of the standard TAP parameter which specifies the query to execute:
	 * "QUERY". */
	public static final String PARAM_QUERY = "query";

	/** Name of the standard TAP parameter which defines the tables to upload in
	 * the database for the query execution: "UPLOAD". */
	public static final String PARAM_UPLOAD = "upload";

	/** Name of the library parameter which informs about a query execution
	 * progression: "PROGRESSION". <i>(this parameter is removed once the
	 * execution is finished)</i> */
	public static final String PARAM_PROGRESSION = "progression";

	/** Internal query execution report. */
	protected TAPExecutionReport execReport = null;

	/** Parameters of this job for its execution. */
	protected final TAPParameters tapParams;

	/**
	 * Build a pending TAP job with the given parameters.
	 *
	 * <p><i>Note:
	 * 	If the parameter {@link #PARAM_PHASE} (</i>phase<i>) is given with the
	 * 	value {@link #PHASE_RUN} the job execution starts immediately after the
	 * 	job has been added to a job list or after
	 * 	{@link #applyPhaseParam(JobOwner)} is called.
	 * </i></p>
	 *
	 * @param owner		User who owns this job. <i>MAY BE NULL</i>
	 * @param tapParams	Set of parameters.
	 *
	 * @throws TAPException	If one of the given parameters has a forbidden or
	 *                     	wrong value.
	 */
	public TAPJob(final JobOwner owner, final TAPParameters tapParams) throws TAPException{
		super(owner, tapParams);
		this.tapParams = tapParams;
		tapParams.check();
	}

	/**
	 * Build a pending TAP job with the given parameters.
	 * The given HTTP request ID will be used as Job ID if not already used by
	 * another job.
	 *
	 * <p><i>Note:
	 * 	If the parameter {@link #PARAM_PHASE} (</i>phase<i>) is given with the
	 * 	value {@link #PHASE_RUN} the job execution starts immediately after the
	 * 	job has been added to a job list or after
	 * 	{@link #applyPhaseParam(JobOwner)} is called.
	 * </i></p>
	 *
	 * @param owner		User who owns this job. <i>MAY BE NULL</i>
	 * @param tapParams	Set of parameters.
	 * @param requestID	ID of the HTTP request which has initiated the creation
	 *                 	of this job.
	 *                 	<i>Note: if NULL, empty or already used, a job ID will
	 *                 	be generated thanks to {@link #generateJobId()}.</i>
	 *
	 * @throws TAPException	If one of the given parameters has a forbidden or
	 *                     	wrong value.
	 *
	 * @since 2.1
	 */
	public TAPJob(final JobOwner owner, final TAPParameters tapParams, final String requestID) throws TAPException{
		super(owner, tapParams, requestID);
		this.tapParams = tapParams;
		tapParams.check();
	}

	/**
	 * Restore a job in a state defined by the given parameters.
	 * The phase must be set separately with
	 * {@link #setPhase(uws.job.ExecutionPhase, boolean)}, where the second
	 * parameter is true.
	 *
	 * @param jobID			ID of the job.
	 * @param creationTime	Creation date/time of the job
	 *                    	(SHOULD NOT BE NEGATIVE OR NULL).
	 * @param owner			User who owns this job.
	 * @param params		Set of not-standard UWS parameters (i.e. what is
	 *              		called by {@link UWSJob} as additional parameters ;
	 *              		they includes all TAP parameters).
	 * @param quote			Quote of this job.
	 * @param startTime		Date/Time at which this job started.
	 *                 		<i>(if not null, it means the job execution was
	 *                 		finished, so a endTime should be provided)</i>
	 * @param endTime		Date/Time at which this job finished.
	 * @param results		List of results.
	 *               		<i>NULL if the job has not been executed, has been
	 *               		aborted or finished with an error.</i>
	 * @param error			Error with which this job ends.
	 *
	 * @throws TAPException	If one of the given parameters has a forbidden or
	 *                     	wrong value.
	 *
	 * @since 2.2
	 */
	public TAPJob(final String jobID, final long creationTime, final JobOwner owner, final TAPParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws TAPException{
		super(jobID, creationTime, owner, params, quote, startTime, endTime, results, error);
		this.tapParams = params;
		this.tapParams.check();
	}

	/**
	 * Get the object storing and managing the set of all (UWS and TAP)
	 * parameters.
	 *
	 * @return The object managing all job parameters.
	 */
	public final TAPParameters getTapParams(){
		return tapParams;
	}

	/**
	 * Get the value of the REQUEST parameter.
	 *
	 * <p>This value must be {@value #REQUEST_DO_QUERY}.</p>
	 *
	 * @return	REQUEST value.
	 */
	public final String getRequest(){
		return tapParams.getRequest();
	}

	/**
	 * Get the value of the FORMAT parameter.
	 *
	 * @return	FORMAT value.
	 */
	public final String getFormat(){
		return tapParams.getFormat();
	}

	/**
	 * Get the value of the LANG parameter.
	 *
	 * <p>
	 * 	This value should always be {@value #LANG_ADQL} in this version of the
	 * 	library
	 * </p>
	 *
	 * @return	LANG value.
	 */
	public final String getLanguage(){
		return tapParams.getLang();
	}

	/**
	 * Get the value of the MAXREC parameter.
	 *
	 * <p>
	 * 	If this value is negative, it means the number of output rows is not
	 * 	limited.
	 * </p>
	 *
	 * @return	MAXREC value.
	 */
	public final int getMaxRec(){
		return tapParams.getMaxRec();
	}

	/**
	 * Get the value of the QUERY parameter (i.e. the query, in the language
	 * returned by {@link #getLanguage()}, to execute).
	 *
	 * @return	QUERY value.
	 */
	public final String getQuery(){
		return tapParams.getQuery();
	}

	/**
	 * Get the value of the VERSION parameter.
	 *
	 * <p>
	 * 	This value should be {@value #VERSION_1_0} in this version of the
	 * 	library.
	 * </p>
	 *
	 * @return	VERSION value.
	 */
	public final String getVersion(){
		return tapParams.getVersion();
	}

	/**
	 * Get the value of the UPLOAD parameter.
	 *
	 * <p>
	 * 	This value must be formatted as specified by the TAP standard (= a
	 * 	semicolon separated list of DALI uploads).
	 * </p>
	 *
	 * @return	UPLOAD value.
	 */
	public final String getUpload(){
		return tapParams.getUpload();
	}

	/**
	 * Get the list of tables to upload in the database for the query execution.
	 *
	 * <p>The returned array is an interpretation of the UPLOAD parameter.</p>
	 *
	 * @return	List of tables to upload.
	 */
	public final DALIUpload[] getTablesToUpload(){
		return tapParams.getUploadedTables();
	}

	/**
	 * Get the execution report.
	 *
	 * <p>
	 * 	This report is available only during or after the job execution.
	 * 	It tells in which step the execution is, and how long was the previous
	 * 	steps. It can also give more information about the number of resulting
	 * 	rows and columns.
	 * </p>
	 *
	 * @return The execReport.
	 */
	public final TAPExecutionReport getExecReport(){
		return execReport;
	}

	/**
	 * Set the execution report.
	 *
	 * <p><b>IMPORTANT:
	 * 	This function can be called only if the job is running or is being
	 * 	restored, otherwise an exception would be thrown. It should not be used
	 * 	by implementors, but only by the internal library processing.
	 * </b></p>
	 *
	 * @param execReport	An execution report.
	 *
	 * @throws UWSException	If this job has never been restored and is not
	 *                     	running.
	 */
	public final void setExecReport(final TAPExecutionReport execReport) throws UWSException{
		if (getRestorationDate() == null && (thread == null || thread.isFinished()))
			throw new UWSException("Impossible to set an execution report if the job is not in the EXECUTING phase ! Here, the job \"" + jobId + "\" is in the phase " + getPhase());
		this.execReport = execReport;
	}

	/**
	 * Create the thread to use for the execution of this job.
	 *
	 * <p><i>Note:
	 * 	If the job already exists, this function does nothing.
	 * </i></p>
	 *
	 * @throws NullPointerException	If the factory returned NULL rather than the
	 *                             	asked {@link JobThread}.
	 * @throws UWSException			If the thread creation fails.
	 *
	 * @see TAPFactory#createJobThread(UWSJob)
	 *
	 * @since 2.0
	 */
	private final void createThread() throws NullPointerException, UWSException{
		if (thread == null){
			thread = getFactory().createJobThread(this);
			if (thread == null)
				throw new NullPointerException("Missing job work! The thread created by the factory is NULL => The job can't be executed!");
		}
	}

	/**
	 * Check whether this job is able to start right now.
	 *
	 * <p>
	 * 	Basically, this function try to get a database connection. If none is
	 * 	available, then this job can not start and this function return FALSE.
	 * 	In all the other cases, TRUE is returned.
	 * </p>
	 *
	 * <p><b>Warning:</b>
	 * 	This function will indirectly open and keep a database connection, so
	 * 	that the job can be started just after its call. If it turns out that
	 * 	the execution won't start just after this call, the DB connection should
	 * 	be closed in some way in order to save database resources.
	 * </p>
	 *
	 * @return	<i>true</i> if this job can start right now,
	 *        	<i>false</i> otherwise.
	 *
	 * @since 2.0
	 */
	public final boolean isReadyForExecution(){
		return thread != null && ((AsyncThread)thread).isReadyForExecution();
	}

	@Override
	public final void start(final boolean useManager) throws UWSException{
		// This job must know its jobs list and this jobs list must know its UWS:
		if (getJobList() == null || getJobList().getUWS() == null)
			throw new IllegalStateException("A TAPJob can not start if it is not linked to a job list or if its job list is not linked to a UWS.");

		// If already running do nothing:
		else if (isRunning())
			return;

		// If asked propagate this request to the execution manager:
		else if (useManager){
			// Create its corresponding thread, if not already existing:
			createThread();
			// Ask to the execution manager to test whether the job is ready for execution, and if, execute it (by calling this function with "false" as parameter):
			getJobList().getExecutionManager().execute(this);

		}// Otherwise start directly the execution:
		else{
			// Create its corresponding thread, if not already existing:
			createThread();
			if (!isReadyForExecution()){
				UWSException ue = new NoDBConnectionAvailableException();
				((TAPLog)getLogger()).logDB(LogLevel.ERROR, null, "CONNECTION_LACK", "No more database connection available for the moment!", ue);
				getLogger().logJob(LogLevel.ERROR, this, "ERROR", "Asynchronous job " + jobId + " execution aborted: no database connection available!", null);
				throw ue;
			}

			// Change the job phase:
			setPhase(ExecutionPhase.QUEUED);
			setPhase(ExecutionPhase.EXECUTING);

			// Set the start time:
			setStartTime(new Date());

			// Run the job:
			thread.start();
			(new JobTimeOut()).start();

			// Log the start of this job:
			getLogger().logJob(LogLevel.INFO, this, "START", "Job \"" + jobId + "\" started.", null);
		}
	}

	/** @since 2.1 */
	@Override
	protected void stop(){
		if (!isStopped()){
			synchronized(thread){
				stopping = true;

				// Interrupts the thread:
				thread.interrupt();

				// Cancel the query execution if any currently running:
				((AsyncThread)thread).executor.cancelQuery();

				// Wait a little for its end:
				if (waitForStop > 0){
					try{
						thread.join(waitForStop);
					}catch(InterruptedException ie){
						getLogger().logJob(LogLevel.WARNING, this, "END", "Unexpected InterruptedException while waiting for the end of the execution of the job \"" + jobId + "\" (thread ID: " + thread.getId() + ")!", ie);
					}
				}
			}
		}
	}

	/**
	 * This exception is thrown by a job execution when no database connection
	 * are available anymore.
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 2.0 (02/2015)
	 * @since 2.0
	 */
	public static class NoDBConnectionAvailableException extends UWSException {
		private static final long serialVersionUID = 1L;

		public NoDBConnectionAvailableException(){
			super("Service momentarily too busy! Please try again later.");
		}

	}

}
