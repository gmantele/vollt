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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * Abstract implementation of the interface {@link ExecutionManager} which lets
 * managing an execution queue.
 *
 * <p>
 * 	When calling {@link #execute(UWSJob)}, ALL jobs are put into the list of
 * 	queued jobs (so their phase is changed to {@link ExecutionPhase#QUEUED}).
 * 	A call to {@link #refresh()}, reads this list and tries to execute the first
 * 	job of the list. The function {@link #isReadyForExecution(UWSJob)} decides
 * 	whether the first job of the queue can be executed NOW or not.
 * </p>
 *
 * <p><i>Note:
 * 	The order of queued jobs is preserved: it is implemented by a FIFO queue.
 * </i></p>
 *
 * <p><i>Note:
 *	After a call to {@link #stopAll()}, this manager is still able to execute
 *	new jobs. Except if it was not possible to stop them properly, stopped jobs
 *	could be executed again by calling afterwards {@link #execute(UWSJob)} with
 *	these jobs in parameter.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (09/2017)
 */
public abstract class AbstractQueuedExecutionManager implements ExecutionManager {

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
	@Override
	public final Iterator<UWSJob> getRunningJobs(){
		return runningJobs.values().iterator();
	}

	@Override
	public final int getNbRunningJobs(){
		return runningJobs.size();
	}

	@Override
	public final Iterator<UWSJob> getQueuedJobs(){
		return queuedJobs.iterator();
	}

	@Override
	public final int getNbQueuedJobs(){
		return queuedJobs.size();
	}

	/**
	 * Tells whether there is a waiting queue.
	 *
	 * @return	<i>true</i> if at least one job is into the queue,
	 *        	<i>false</i> otherwise.
	 */
	public boolean hasQueue(){
		return !queuedJobs.isEmpty();
	}

	/**
	 * Tells whether the given job can be executed NOW. In other words, in
	 * function of the result of this function, the given job will be put in the
	 * queue or it will be executed.
	 *
	 * @param 	jobToExecute
	 *
	 * @return	<i>true</i> if the given job can be executed NOW (=&gt; it will
	 *        	be executed),
	 *        	<i>false</i> otherwise (=&gt; it will be put in the queue).
	 */
	public abstract boolean isReadyForExecution(UWSJob jobToExecute);

	/* **************************** */
	/* EXECUTION MANAGEMENT METHODS */
	/* **************************** */
	/**
	 * Removes the first queued job(s) from the queue and executes it (them)
	 * <b>ONLY IF</b> it (they) can be executed (see
	 * {@link #isReadyForExecution(UWSJob)}).
	 *
	 * <p><i>Note:
	 * 	Nothing is done if there is no queue.
	 * </i></p>
	 *
	 * <p><i>Note:
	 * 	If any error occurs while refreshing this manager, it SHOULD be logged
	 * 	using the service logger.
	 * </i></p>
	 *
	 * @see #hasQueue()
	 * @see #isReadyForExecution(UWSJob)
	 * @see #startJob(UWSJob)
	 *
	 * @see uws.job.manager.ExecutionManager#refresh()
	 */
	@Override
	public synchronized final void refresh(){
		// Return immediately if no queue:
		if (!hasQueue())
			return;

		// Start the first job of the queue while it can be executed:
		UWSJob jobToStart;
		while(!queuedJobs.isEmpty() && isReadyForExecution(queuedJobs.firstElement())){
			jobToStart = queuedJobs.remove(0);
			try{
				startJob(jobToStart);
			}catch(UWSException ue){
				logger.logJob(LogLevel.ERROR, jobToStart, "START", "Can not start the job \"" + jobToStart.getJobId() + "\"! This job is not any more part of its execution manager.", ue);
			}
		}
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
	 * Refreshes this manager and then put the given job into the queue (if
	 * it is not already into it).
	 *
	 * <p><i>Note:
	 * 	If any error occurs while executing the given job, it SHOULD be logged
	 * 	using the service logger.
	 * </i></p>
	 *
	 * @param jobToExecute	The job to execute.
	 *
	 * @return	The resulting execution phase of the given job
	 *        	({@link ExecutionPhase#EXECUTING EXECUTING} or
	 *        	{@link ExecutionPhase#QUEUED QUEUED}
	 *        	or <i>null</i> if the given job is <i>null</i>).
	 *
	 * @see #refresh()
	 * @see UWSJob#isRunning()
	 * @see #isReadyForExecution(UWSJob)
	 * @see UWSJob#setPhase(ExecutionPhase)
	 *
	 * @see uws.job.manager.ExecutionManager#execute(UWSJob)
	 */
	@Override
	public synchronized final ExecutionPhase execute(final UWSJob jobToExecute){
		if (jobToExecute == null)
			return null;

		// Refresh the list of running jobs before all:
		refresh();

		// If the job is already running, ensure it is in the list of running jobs:
		if (jobToExecute.isRunning())
			runningJobs.put(jobToExecute.getJobId(), jobToExecute);

		// If the job is already finished, ensure it is not any more in both list of jobs:
		else if (jobToExecute.isFinished()){
			runningJobs.remove(jobToExecute);
			queuedJobs.remove(jobToExecute);

		}// Otherwise, change the phase to QUEUED, put it into the queue and then refresh the queue:
		else{
			try{
				if (jobToExecute.getPhase() != ExecutionPhase.QUEUED)
					jobToExecute.setPhase(ExecutionPhase.QUEUED);

				if (!queuedJobs.contains(jobToExecute)){
					queuedJobs.add(jobToExecute);
					refresh();
				}
			}catch(UWSException ue){
				// log the error:
				logger.logJob(LogLevel.ERROR, jobToExecute, "QUEUE", "Can not set the job \"" + jobToExecute.getJobId() + "\" in the QUEUED phase!", ue);
				// set the phase HELD (meaning it is impossible to set the job into a QUEUED phase):
				try{
					jobToExecute.setPhase(ExecutionPhase.HELD);
				}catch(UWSException e){}
			}
		}

		return jobToExecute.getPhase();
	}

	/**
	 * Removes the given job from the lists of queued and running jobs and
	 * then refreshes the manager.
	 *
	 * <p><i>Note:
	 * 	If any error occurs while removing a job from this manager, it SHOULD be
	 * 	logged using the service logger.
	 * </i></p>
	 *
	 * @see uws.job.manager.ExecutionManager#remove(uws.job.UWSJob)
	 */
	@Override
	public final synchronized void remove(final UWSJob jobToRemove){
		if (jobToRemove != null){
			runningJobs.remove(jobToRemove.getJobId());
			queuedJobs.remove(jobToRemove);
			refresh();
		}
	}

	@Override
	public final synchronized void stopAll(){
		// Set back all queued jobs to the PENDING phase:
		for(UWSJob qj : queuedJobs){
			try{
				qj.setPhase(ExecutionPhase.PENDING, true);
			}catch(UWSException ue){
				if (logger != null)
					logger.logJob(LogLevel.WARNING, qj, "ABORT", "Can not set back the job to the PENDING phase.", ue);
			}
		}

		// Empty the queue:
		queuedJobs.clear();

		// Stop all running jobs and set them back to the PENDING phase:
		for(UWSJob rj : runningJobs.values()){
			try{
				// Stop the job:
				rj.abort();
				// Set its phase back to PENDING:
				rj.setPhase(ExecutionPhase.PENDING, true);
			}catch(UWSException ue){
				if (logger != null)
					logger.logJob(LogLevel.WARNING, rj, "ABORT", "Can not stop the job nicely. The thread may continue to run until its end.", ue);
			}
		}

		// Empty the list of running jobs:
		runningJobs.clear();
	}
}
