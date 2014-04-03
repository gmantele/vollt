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

import uws.UWSException;

import uws.job.UWSJob;
import uws.service.log.UWSLog;

/**
 * <p>Implementation of the interface {@link ExecutionManager} which lets managing an execution queue in function of a maximum number of running jobs:
 * if there are more running jobs than a given number, the jobs to execute are put in the queue until a running job stops.
 * The order of queued jobs are preserved: it is implemented by a FIFO queue.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public class QueuedExecutionManager extends AbstractQueuedExecutionManager {
	private static final long serialVersionUID = 1L;

	/** The maximum number of running jobs. */
	protected int nbMaxRunningJobs = NO_QUEUE;

	/** The value of {@link #nbMaxRunningJobs} which indicates that there is no queue. */
	public final static int NO_QUEUE = Integer.MAX_VALUE;


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds an execution manager without queue.
	 * 
	 * @param logger	The object to user to log some messages (error, info, debug).
	 */
	public QueuedExecutionManager(final UWSLog logger){
		super(logger);
	}

	/**
	 * Builds an execution manager with a queue. The number of executing jobs is limited by the given value (if positive and different from 0).
	 * 
	 * @param logger	The object to user to log some messages (error, info, debug).
	 * @param maxRunningJobs	The maximum number of running jobs (must be > 0 to have a queue).
	 */
	public QueuedExecutionManager(final UWSLog logger, int maxRunningJobs) {
		this(logger);
		nbMaxRunningJobs = (maxRunningJobs <= 0)?NO_QUEUE:maxRunningJobs;
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */

	public final void setNoQueue() {
		nbMaxRunningJobs = NO_QUEUE;
		try{
			refresh();
		}catch(UWSException ue){
			logger.error("Impossible to refresh the execution manager !", ue);
		}
	}

	/**
	 * Gets the maximum number of running jobs.
	 * 
	 * @return	The maximum number of running jobs.
	 */
	public final int getMaxRunningJobs(){
		return nbMaxRunningJobs;
	}

	/**
	 * <p>Sets the maximum number of running jobs.</p>
	 * 
	 * <p><i><u>Note:</u> If the new maximum number of running jobs is increasing the list of running jobs is immediately updated
	 * BUT NOT IF it is decreasing (that is to say, running jobs will not be interrupted to be put in the queue, they continue to run) !</i></p>
	 * 
	 * @param maxRunningJobs	The new maximum number of running jobs ({@link #NO_QUEUE} or a negative value means no maximum number of running jobs: there will be no queue any more).
	 * 
	 * @throws UWSException		If there is an error while updating the list of running jobs (in other words if some queued jobs can not be executed).
	 * 
	 * @see #refresh()
	 */
	public void setMaxRunningJobs(int maxRunningJobs) throws UWSException {
		nbMaxRunningJobs = (maxRunningJobs <= 0)?NO_QUEUE:maxRunningJobs;
		refresh();
	}

	@Override
	public final boolean isReadyForExecution(final UWSJob jobToExecute) {
		if (!hasQueue())
			return true;
		else
			return runningJobs.size() < nbMaxRunningJobs;
	}
}
