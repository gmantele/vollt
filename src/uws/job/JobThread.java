package uws.job;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import uws.UWSException;
import uws.UWSToolBox;
import uws.service.file.UWSFileManager;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * <P>An instance of this class is a thread dedicated to a job execution.</P>
 * 
 * <P>This thread is necessary associated with a {@link UWSJob} instance. Thus the execution of this thread is mainly done in
 * the {@link #jobWork()} method.</P>
 * 
 * <P>However its execution is possible only if the job phase is {@link ExecutionPhase#EXECUTING EXECUTING}. The job phase constraint (=EXECUTING) is
 * already checked so it is useless to check it again in {@link #jobWork()}.</P>
 * 
 * At the end of the execution the job must be correctly filled (cf {@link #jobWork()}) according to the execution conclusion:
 * <ul>
 * 	<li>SUCCESS: the <i>results</i> field of job must be filled (see {@link #publishResult(Result)} and {@link #getResultOutput(Result)}),</li>
 * 	<li>ABORTED: the method {@link UWSJob#abort()} must be called or an {@link InterruptedException} can be thrown</li>
 * 	<li>ERROR: the <i>errors</i> field must be filled by calling {@link #setError(ErrorSummary)} or {@link #setError(UWSException)}.</li>
 * </ul>
 * 
 * <P>In both cases the startTime and the endTime fields are already managed by {@link UWSJob} so it is useless to change them.</P>
 * 
 * <P>Just after the job work the job phase is set to {@link ExecutionPhase#COMPLETED COMPLETED} if no interruption has been detected,
 * {@link ExecutionPhase#ABORTED ABORTED} otherwise.</P>
 * 
 * If {@link #jobWork()} throws:
 * <ul>
 * 	<li>a {@link UWSException}: the method {@link #setError(UWSException)} is called.
 * 		Besides the exception is kept in the {@link JobThread#lastError} and is available thanks to the {@link UWSJob#getWorkError()}.</li>
 * 	<li>an {@link InterruptedException}: the method {@link UWSJob#abort()} is called.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (08/2014)
 * 
 * @see UWSJob#start()
 * @see UWSJob#abort()
 * @see UWSJob#getFileManager()
 * @see UWSJob#getWorkError()
 */
public abstract class JobThread extends Thread {

	/** The job which contains all parameters for its execution and which must be filled at the end of the execution. */
	protected final UWSJob job;

	/** The last error which has occurred during the execution of this thread. */
	protected UWSException lastError = null;

	/** Indicates whether the {@link UWSJob#jobWork()} has been called and finished, or not. */
	protected boolean finished = false;

	/** Description of what is done by this thread. */
	protected final String taskDescription;

	public final static ThreadGroup tg = new ThreadGroup("UWS_GROUP");

	/**
	 * Builds the JobThread instance which will be used by the given job to execute its task.
	 * 
	 * @param j				The associated job.
	 * @param fileManager	An object to get access to UWS files (particularly: error and results file).
	 * 
	 * @throws UWSException	If the given job or the given file manager is null.
	 * 
	 * @see #getDefaultTaskDescription(UWSJob)
	 */
	public JobThread(UWSJob j) throws UWSException{
		this(j, getDefaultTaskDescription(j));
	}

	/**
	 * Builds the JobThread instance which will be used by the given job to execute its task.
	 * 
	 * @param j				The associated job.
	 * @param fileManager	An object to get access to UWS files (particularly: error and results file).
	 * @param task			Description of the task executed by this thread.
	 * 
	 * @throws UWSException	If the given job or the given file manager is null.
	 */
	public JobThread(UWSJob j, String task) throws UWSException{
		super(tg, j.getJobId());

		job = j;
		taskDescription = task;
	}

	/**
	 * Gets a default description of the task executed by a {@link JobThread}.
	 * @param job	A UWS job.
	 * @return		A default description of a JobThread which will execute the given job.
	 */
	protected final static String getDefaultTaskDescription(final UWSJob job){
		// Describe the task of this thread:
		String task = "";
		task = "Executing the job " + job.getJobId();
		if (job.getJobList() != null && job.getJobList().getName() != null && !job.getJobList().getName().trim().isEmpty()){
			JobList jl = job.getJobList();
			task += " (JobList: " + job.getJobList().getName();
			if (jl.getUWS() != null && jl.getUWS().getName() != null && !jl.getUWS().getName().trim().isEmpty())
				task += ", UWS: " + jl.getUWS().getName();
			task += ")";
		}
		return task;
	}

	/**
	 * Gets the job instance associated to this thread.
	 * 
	 * @return	The associated job instance.
	 */
	public final UWSJob getJob(){
		return job;
	}

	/**
	 * Gets the manager of the UWS files (particularly: the error and results file).
	 * 
	 * @return	The used file manager.
	 */
	public final UWSFileManager getFileManager(){
		return job.getFileManager();
	}

	/**
	 * Gets the last error which has occurred during the execution of this thread.
	 * 
	 * @return	The last error.
	 */
	public final UWSException getError(){
		return lastError;
	}

	/**
	 * Indicates whether the {@link UWSJob#jobWork()} method has been called or not.
	 * 
	 * @return	<i>true</i> if the job work is done, <i>false</i> otherwise.
	 */
	public final boolean isFinished(){
		return finished;
	}

	/**
	 * Lets changing the execution phase of the job and setting its endTime.
	 * 
	 * @throws UWSException	If there is an error while changing the execution phase.
	 */
	private final void complete() throws UWSException{
		if (isInterrupted())
			job.abort();
		else{
			job.setPhase(ExecutionPhase.COMPLETED);
			job.setEndTime(new Date());
		}
	}

	/**
	 * <p>Published the given error in the job.</p>
	 * 
	 * <p><i><u>note:</u> This thread will be stopped.</i></p>
	 * 
	 * @param error			The error to publish.
	 * 
	 * @throws UWSException	If there is an error while publishing the error.
	 * 
	 * @see {@link UWSJob#error(ErrorSummary)}
	 */
	public void setError(final ErrorSummary error) throws UWSException{
		job.error(error);
	}

	/**
	 * <p>Publishes the given exception as an error summary.
	 * Doing that also stops the job, sets the phase to {@link ExecutionPhase#ERROR} and sets the end time.</p>
	 * 
	 * <p>
	 * 	By default, this function tries to write the stack trace of the given exception thanks to
	 * 	{@link UWSFileManager#getErrorOutput(ErrorSummary, UWSJob)}. If it fails or if this job is not connected to
	 * 	a job list or the connected job list is not connected to a UWS, {@link #setError(ErrorSummary)} is called and
	 * 	no error file is written.
	 * </p>
	 * 
	 * @param ue				The exception that has interrupted this job.
	 * 
	 * @throws UWSException		If there is an error while publishing the given exception.
	 * 
	 * {@link UWSToolBox#writeErrorFile(Exception, ErrorSummary, UWSJob, OutputStream)}
	 */
	public void setError(final UWSException ue) throws UWSException{
		if (ue == null)
			return;

		try{
			ErrorSummary error = new ErrorSummary(ue, ue.getUWSErrorType(), job.getUrl() + "/" + UWSJob.PARAM_ERROR_SUMMARY + "/details");
			OutputStream output = getFileManager().getErrorOutput(error, job);

			UWSToolBox.writeErrorFile(ue, error, job, output);

			setError(error);

		}catch(IOException ioe){
			job.getLogger().logThread(LogLevel.ERROR, this, "SET_ERROR", "The stack trace of a UWSException had not been written !", ioe);
			setError(new ErrorSummary(ue.getMessage(), ue.getUWSErrorType()));
		}
	}

	/**
	 * Creates a default result description.
	 * 
	 * @param job	The job which will contains this result.
	 * 
	 * @return		The created result.
	 * 
	 * @see #createResult(String)
	 */
	public Result createResult(){
		String resultName = Result.DEFAULT_RESULT_NAME;

		if (job.getResult(resultName) != null){
			int resultCount = 0;
			do{
				resultCount++;
				resultName = Result.DEFAULT_RESULT_NAME + "_" + resultCount;
			}while(job.getResult(resultName) != null);
		}

		return createResult(resultName);
	}

	/**
	 * Creates a default result description but by precising its name/ID.
	 * 
	 * @param job	The job which will contains this result.
	 * @param name	The name/ID of the result to create.
	 * 
	 * @return		The created result.
	 * 
	 * @see Result#Result(UWSJob, String)
	 */
	public Result createResult(final String name){
		return new Result(job, name);
	}

	/**
	 * Publishes the given result in the job.
	 * 
	 * @param result		The result to publish.
	 * 
	 * @throws UWSException	If there is an error while publishing the result.
	 * 
	 * @see UWSJob#addResult(Result)
	 */
	public void publishResult(final Result result) throws UWSException{
		job.addResult(result);
	}

	/**
	 * <p>Gets an output stream for the given result.</p>
	 * 
	 * <p><i><u>note:</u> the result file will be created if needed.</i></p>
	 * 
	 * @param resultToWrite	The description of the result to write.
	 * 
	 * @return				An output stream for the given result.
	 * 
	 * @throws IOException	If there is an error while creating the file or the output stream.
	 * @throws UWSException	If an error occurs in the {@link UWSFileManager#getResultOutput(Result, UWSJob)}.
	 */
	public OutputStream getResultOutput(final Result resultToWrite) throws IOException, UWSException{
		return getFileManager().getResultOutput(resultToWrite, job);
	}

	/**
	 * <b>Gets the size of the corresponding result file.</p>
	 * 
	 * @param result		Description of the Result whose the size is wanted.
	 * 
	 * @return				The size of the corresponding result file.
	 * 
	 * @throws IOException	If there is an error while getting the result file size.
	 * 
	 * @see {@link UWSFileManager#getResultSize(Result, UWSJob)}
	 */
	public long getResultSize(final Result result) throws IOException{
		return getFileManager().getResultSize(result, job);
	}

	/**
	 * <p>Does the job work <i>(i.e. making a long computation or executing a query on a Database)</i>.</p>
	 * 
	 * <p><b><u>Important:</u>
	 * <ul>
	 * 	<li>This method does the job work but it MUST also fill the associated job with the execution results and/or errors.</li>
	 * 	<li>Do not forget to check the interrupted flag of the thread ({@link Thread#isInterrupted()}) and then to send an {@link InterruptedException}.
	 * 		Otherwise the {@link UWSJob#stop()} method will have no effect, as for {@link #abort()} and {@link #error(ErrorSummary)}.</li>
	 * </ul></b></p>
	 * 
	 * <p><i><u>Notes</u>:
	 * <ul>
	 * 	<li>The "setPhase(COMPLETED)" and the "endTime=new Date()" are automatically applied just after the call to jobWork.</li>
	 * 	<li>If an {@link UWSException} is thrown the {@link JobThread} will automatically publish the exception in this job
	 * 		thanks to the {@link UWSJob#error(UWSException)} method or the {@link #setErrorSummary(ErrorSummary)} method,
	 * 		and so it will set its phase to {@link ExecutionPhase#ERROR}.</li>
	 * 	<li>If an {@link InterruptedException} is thrown the {@link JobThread} will automatically set the phase to {@link ExecutionPhase#ABORTED}</li>
	 * </ul></i></p>
	 * 
	 * @throws UWSException			If there is any kind of error which must be propagated.
	 * @throws InterruptedException	If the thread has been interrupted or if any method throws this exception.
	 */
	protected abstract void jobWork() throws UWSException, InterruptedException;

	/**
	 * <ol>
	 * 	<li>Tests the execution phase of the job: if not {@link ExecutionPhase#EXECUTING EXECUTING}, nothing is done...the thread ends immediately.</li>
	 * 	<li>Calls the {@link UWSJob#jobWork()} method.</li>
	 * 	<li>Sets the <i>finished</i> flag to <i>true</i>.</li>
	 * 	<li>Changes the job phase to {@link ExecutionPhase#COMPLETED COMPLETED} if not interrupted, else {@link ExecutionPhase#ABORTED ABORTED}.
	 * </ol>
	 * <P>If any {@link InterruptedException} occurs the job phase is only set to {@link ExecutionPhase#ABORTED ABORTED}.</P>
	 * <P>If any {@link UWSException} occurs while the phase is {@link ExecutionPhase#EXECUTING EXECUTING} the job phase
	 * is set to {@link ExecutionPhase#ERROR ERROR} and an error  summary is created.</P>
	 * <P>Whatever is the exception, it will always be available thanks to the {@link JobThread#getError()} after execution.</P>
	 * 
	 * @see UWSJob#jobWork()
	 * @see UWSJob#setPhase(ExecutionPhase)
	 * @see UWSJob#publishExecutionError(UWSException)
	 * @see UWSToolBox#publishErrorSummary(UWSJob, String, ErrorType)
	 */
	@Override
	public final void run(){
		if (!job.getPhaseManager().isExecuting())
			return;
		else{
			lastError = null;
			finished = false;
		}

		UWSLog logger = job.getLogger();

		// Log the start of this thread:
		logger.logThread(LogLevel.INFO, this, "START", "Thread \"" + getId() + "\" started.", null);

		try{
			try{
				// Execute the task:
				jobWork();

				// Change the phase to COMPLETED:
				finished = true;
				complete();
			}catch(InterruptedException ex){
				// Abort:
				finished = true;
				if (!job.stopping)
					job.abort();
				// Log the abortion:
				logger.logThread(LogLevel.INFO, this, "END", "Thread \"" + getId() + "\" cancelled.", null);
			}
			return;

		}catch(UWSException ue){
			// Save the error:
			lastError = ue;

		}catch(Throwable t){
			// Build the error:
			if (t instanceof Error)
				lastError = new UWSException(UWSException.INTERNAL_SERVER_ERROR, t, "A FATAL DEEP ERROR OCCURED WHILE EXECUTING THIS QUERY! This error is reported in the service logs.", ErrorType.FATAL);
			else if (t.getMessage() == null || t.getMessage().trim().isEmpty())
				lastError = new UWSException(UWSException.INTERNAL_SERVER_ERROR, t.getClass().getName(), ErrorType.FATAL);
			else
				lastError = new UWSException(UWSException.INTERNAL_SERVER_ERROR, t, ErrorType.FATAL);

		}finally{
			finished = true;

			// Publish the error if any has occurred:
			if (lastError != null){
				// Log the error:
				LogLevel logLevel = (lastError.getCause() != null && lastError.getCause() instanceof Error) ? LogLevel.FATAL : LogLevel.ERROR;
				logger.logThread(logLevel, this, "END", "Thread \"" + getId() + "\" ended with an error.", lastError);
				// Set the error into the job:
				try{
					setError(lastError);
				}catch(UWSException ue){
					try{
						logger.logThread(logLevel, this, "SET_ERROR", "[1st Attempt] Problem in JobThread.setError(UWSException), while setting the execution error of the job " + job.getJobId() + ". A last attempt will be done.", ue);
						setError(new ErrorSummary((lastError.getCause() != null) ? lastError.getCause().getMessage() : lastError.getMessage(), lastError.getUWSErrorType()));
					}catch(UWSException ue2){
						logger.logThread(logLevel, this, "SET_ERROR", "[2nd and last Attempt] Problem in JobThread.setError(ErrorSummary), while setting the execution error of the job " + job.getJobId() + ". This error can not be reported to the user, but it will be reported in the log in the JOB context.", ue2);
						// Note: no need of a level 3: if the second attempt fails, it means the job is in a wrong phase and no error summary can never be set ; further attempt won't change anything!
						logger.logJob(logLevel, job, "EXECUTING", "An error has interrupted the execution of the job \"" + job.getJobId() + "\". Here is its summary: " + lastError.getMessage(), lastError);
					}
				}
			}else
				logger.logThread(LogLevel.INFO, this, "END", "Thread \"" + getId() + "\" successfully ended.", null);
		}
	}
}
