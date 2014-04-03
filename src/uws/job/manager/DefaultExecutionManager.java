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

import uws.UWSException;
import uws.UWSExceptionFactory;

import uws.job.ExecutionPhase;
import uws.job.UWSJob;

/**
 * <p>Default implementation of the ExecutionManager interface.</p>
 * 
 * <p>This manager does not have a queue. That is to say that all jobs are always immediately starting.
 * Consequently this manager is just used to gather all running jobs.</p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public class DefaultExecutionManager implements ExecutionManager {
	private static final long serialVersionUID = 1L;

	/** List of running jobs. */
	protected Map<String,UWSJob> runningJobs;

	public DefaultExecutionManager(){
		runningJobs = new LinkedHashMap<String,UWSJob>(10);
	}

	/* ******* */
	/* GETTERS */
	/* ******* */

	public final Iterator<UWSJob> getRunningJobs(){
		return runningJobs.values().iterator();
	}

	public final int getNbRunningJobs(){
		return runningJobs.size();
	}

	/**
	 * Always returns a Null Iterator (iterator whose next() returns <i>null</i> and hasNext() returns <i>false</i>).
	 * 
	 * @see uws.job.manager.ExecutionManager#getQueuedJobs()
	 */
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
	public final int getNbQueuedJobs(){
		return 0;
	}

	/**
	 * Does nothing in its implementation.
	 * 
	 * @see uws.job.manager.ExecutionManager#refresh()
	 */
	public final void refresh() throws UWSException{
		;
	}

	public synchronized ExecutionPhase execute(final UWSJob jobToExecute) throws UWSException{
		if (jobToExecute == null)
			return null;

		// If the job is already running, ensure it is in the list of running jobs:
		if (jobToExecute.isRunning())
			runningJobs.put(jobToExecute.getJobId(), jobToExecute);

		// If the job is already finished, ensure it is not any more in the list of running jobs:
		else if (jobToExecute.isFinished()){
			runningJobs.remove(jobToExecute);
			throw UWSExceptionFactory.incorrectPhaseTransition(jobToExecute.getJobId(), jobToExecute.getPhase(), ExecutionPhase.EXECUTING);

			// Otherwise start it:
		}else{
			jobToExecute.start(false);
			runningJobs.put(jobToExecute.getJobId(), jobToExecute);
		}

		return jobToExecute.getPhase();
	}

	public synchronized void remove(final UWSJob jobToRemove) throws UWSException{
		if (jobToRemove != null)
			runningJobs.remove(jobToRemove.getJobId());
	}
}
