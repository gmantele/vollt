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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Default implementation of the ExecutionManager interface.</p>
 * 
 * <p>This manager does not have a queue. That is to say that all jobs are always immediately starting.
 * Consequently this manager is just used to gather all running jobs.</p>
 * 
 * <p><i>Note:
 *	After a call to {@link #stopAll()}, this manager is still able to execute new jobs.
 *	Except if it was not possible to stop them properly, stopped jobs could be executed again by calling
 *	afterwards {@link #execute(UWSJob)} with these jobs in parameter.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (12/2014)
 */
public class DefaultExecutionManager implements ExecutionManager {

	/** List of running jobs. */
	protected Map<String,UWSJob> runningJobs;

	protected final UWSLog logger;

	public DefaultExecutionManager(){
		this(null);
	}

	public DefaultExecutionManager(final UWSLog logger){
		runningJobs = new LinkedHashMap<String,UWSJob>(10);
		this.logger = (logger == null) ? UWSToolBox.getDefaultLogger() : logger;
	}

	/* ******* */
	/* GETTERS */
	/* ******* */

	@Override
	public final Iterator<UWSJob> getRunningJobs(){
		return runningJobs.values().iterator();
	}

	@Override
	public final int getNbRunningJobs(){
		return runningJobs.size();
	}

	/**
	 * Always returns a Null Iterator (iterator whose next() returns <i>null</i> and hasNext() returns <i>false</i>).
	 * 
	 * @see uws.job.manager.ExecutionManager#getQueuedJobs()
	 */
	@Override
	public final Iterator<UWSJob> getQueuedJobs(){
		return new Iterator<UWSJob>(){
			@Override
			public boolean hasNext(){
				return false;
			}

			@Override
			public UWSJob next(){
				return null;
			}

			@Override
			public void remove(){
				;
			}
		};
	}

	/**
	 * Always returns 0.
	 * 
	 * @see uws.job.manager.ExecutionManager#getNbQueuedJobs()
	 */
	@Override
	public final int getNbQueuedJobs(){
		return 0;
	}

	/**
	 * Does nothing in its implementation.
	 * 
	 * @see uws.job.manager.ExecutionManager#refresh()
	 */
	@Override
	public final void refresh(){
		;
	}

	@Override
	public synchronized ExecutionPhase execute(final UWSJob jobToExecute){
		if (jobToExecute == null)
			return null;

		// If the job is already running, ensure it is in the list of running jobs:
		if (jobToExecute.isRunning())
			runningJobs.put(jobToExecute.getJobId(), jobToExecute);

		// If the job is already finished, ensure it is not any more in the list of running jobs:
		else if (jobToExecute.isFinished()){
			runningJobs.remove(jobToExecute);
			logger.logJob(LogLevel.WARNING, jobToExecute, "START", "Job \"" + jobToExecute.getJobId() + "\" already finished!", null);

			// Otherwise start it:
		}else{
			try{
				jobToExecute.start(false);
				runningJobs.put(jobToExecute.getJobId(), jobToExecute);
			}catch(UWSException ue){
				logger.logJob(LogLevel.ERROR, jobToExecute, "START", "Can not start the job \"" + jobToExecute.getJobId() + "\"! This job is not any more part of its execution manager.", ue);
			}
		}

		return jobToExecute.getPhase();
	}

	@Override
	public synchronized void remove(final UWSJob jobToRemove){
		if (jobToRemove != null)
			runningJobs.remove(jobToRemove.getJobId());
	}

	@Override
	public synchronized void stopAll(){
		// Stop all running jobs:
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
