package uws.job.manager;

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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import uws.UWSException;
import uws.UWSToolBox;

import uws.job.ErrorType;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.service.log.UWSLog;

/**
 * <p>Abstract implementation of the interface {@link ExecutionManager} which lets managing an execution queue.</p>
 * <p>
 * 	When calling {@link #execute(UWSJob)}, ALL jobs are put into the list of queued jobs (so their phase is changed
 * 	to {@link ExecutionPhase#QUEUED}). A call to {@link #refresh()}, reads this list and tries to execute the first job of the list.
 * 	The function {@link #isReadyForExecution(UWSJob)} decides whether the first job of the queue can be executed NOW or not.
 * </p>
 * </p>
 * 	NOTE: The order of queued jobs is preserved: it is implemented by a FIFO queue.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public abstract class AbstractQueuedExecutionManager implements ExecutionManager {
	private static final long serialVersionUID = 1L;

	/** List of running jobs. */
	protected Map<String,UWSJob> runningJobs;

	/** List of queued jobs. */
	protected Vector<UWSJob> queuedJobs;

	protected final UWSLog logger;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds an execution manager without queue.
	 */
	protected AbstractQueuedExecutionManager(final UWSLog logger){
		runningJobs = new LinkedHashMap<String,UWSJob>();
		queuedJobs = new Vector<UWSJob>(0, 10);
		this.logger = (logger == null) ? UWSToolBox.getDefaultLogger() : logger;
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	public final Iterator<UWSJob> getRunningJobs(){
		return runningJobs.values().iterator();
	}

	public final int getNbRunningJobs(){
		return runningJobs.size();
	}

	public final Iterator<UWSJob> getQueuedJobs(){
		return queuedJobs.iterator();
	}

	public final int getNbQueuedJobs(){
		return queuedJobs.size();
	}

	/**
	 * Tells whether there is a waiting queue.
	 * 
	 * @return	<i>true</i> if at least one job is into the queue, <i>false</i> otherwise.
	 */
	public boolean hasQueue(){
		return !queuedJobs.isEmpty();
	}

	/**
	 * Tells whether the given job can be executed NOW. In other words, in function
	 * of the result of this function, the given job will be put in the queue or it will be executed.
	 * 
	 * @param 	jobToExecute
	 * @return	<i>true</i> if the given job can be executed NOW (=&gt; it will be executed), <i>false</i> otherwise (=&gt; it will be put in the queue).
	 */
	public abstract boolean isReadyForExecution(UWSJob jobToExecute);

	/* **************************** */
	/* EXECUTION MANAGEMENT METHODS */
	/* **************************** */
	/**
	 * <p>Removes the first queued job(s) from the queue and executes it (them)
	 * <b>ONLY IF</b> it (they) can be executed (see {@link #isReadyForExecution(AbstractJob)}).</p>
	 * 
	 * <p><i><u>Note:</u> Nothing is done if there is no queue.</i></p>
	 * 
	 * @throws UWSException	If there is an error during the phase transition of one or more jobs.
	 * 
	 * @see #hasQueue()
	 * @see #isReadyForExecution(UWSJob)
	 * @see #startJob(UWSJob)
	 * 
	 * @see uws.job.manager.ExecutionManager#refresh()
	 */
	public synchronized final void refresh() throws UWSException{
		// Return immediately if no queue:
		if (!hasQueue())
			return;

		String allMsg = null;	// the concatenation of all errors which may occur

		// Start the first job of the queue while it can be executed:
		while(!queuedJobs.isEmpty() && isReadyForExecution(queuedJobs.firstElement())){
			try{
				startJob(queuedJobs.remove(0));
			}catch(UWSException ue){
				allMsg = ((allMsg == null) ? "ERRORS THAT OCCURED WHILE REFRESHING THE EXECUTION MANAGER:" : allMsg) + "\n\t- " + ue.getMessage();
			}
		}

		// Throw one error for all jobs that can not have been executed:
		if (allMsg != null)
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, allMsg, ErrorType.TRANSIENT);
	}

	/**
	 * Starts immediately the given job. This job is removed from the list of
	 * queued jobs and added into the list of running jobs.
	 * 
	 * @param jobToStartNow		The job to start.
	 * 
	 * @throws UWSException		If there is an error while starting the job.
	 * 
	 * @see UWSJob#start(boolean)
	 */
	protected void startJob(final UWSJob jobToStartNow) throws UWSException{
		if (jobToStartNow != null){
			jobToStartNow.start(false);
			queuedJobs.remove(jobToStartNow);
			runningJobs.put(jobToStartNow.getJobId(), jobToStartNow);
		}
	}

	/**
	 * <p>Refreshes this manager and then put the given job into the queue (if it is not already into it).</p>
	 * 
	 * @param jobToExecute	The job to execute.
	 * @return				The resulting execution phase of the given job ({@link ExecutionPhase#EXECUTING EXECUTING} or {@link ExecutionPhase#QUEUED QUEUED} or <i>null</i> if the given job is <i>null</i>).
	 * 
	 * @throws UWSException	If there is an error while changing the execution phase of the given job or if the job is already finished.
	 * 
	 * @see #refresh()
	 * @see AbstractJob#isRunning()
	 * @see #isReadyForExecution(UWSJob)
	 * @see UWSJob#setPhase(ExecutionPhase)
	 * 
	 * @see uws.job.manager.ExecutionManager#execute(AbstractJob)
	 */
	public synchronized final ExecutionPhase execute(final UWSJob jobToExecute) throws UWSException{
		if (jobToExecute == null)
			return null;

		// Refresh the list of running jobs before all:
		try{
			refresh();
		}catch(UWSException ue){
			logger.error("Impossible to refresh the execution manager !", ue);
		}

		// If the job is already running, ensure it is in the list of running jobs:
		if (jobToExecute.isRunning())
			runningJobs.put(jobToExecute.getJobId(), jobToExecute);

		// If the job is already finished, ensure it is not any more in both list of jobs:
		else if (jobToExecute.isFinished()){
			runningJobs.remove(jobToExecute);
			queuedJobs.remove(jobToExecute);

		}// Otherwise, change the phase to QUEUED, put it into the queue and then refresh the queue:
		else{
			if (jobToExecute.getPhase() != ExecutionPhase.QUEUED)
				jobToExecute.setPhase(ExecutionPhase.QUEUED);

			if (!queuedJobs.contains(jobToExecute)){
				queuedJobs.add(jobToExecute);
				refresh();
			}
		}

		return jobToExecute.getPhase();
	}

	/**
	 * Removes the given job from the lists of queued and running jobs and then refreshes the manager.
	 * 
	 * @see uws.job.manager.ExecutionManager#remove(uws.job.UWSJob)
	 */
	public final synchronized void remove(final UWSJob jobToRemove) throws UWSException{
		if (jobToRemove != null){
			runningJobs.remove(jobToRemove.getJobId());
			queuedJobs.remove(jobToRemove);
			refresh();
		}
	}
}
