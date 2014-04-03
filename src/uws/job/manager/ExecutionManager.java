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

import uws.UWSException;

import uws.job.ExecutionPhase;
import uws.job.UWSJob;

/**
 * <p>Lets managing the execution of a set of jobs.</p>
 * 
 * <p>It is used by a job list to decide whether a given job can be executed or whether it must be put in a queue.</p>
 * 
 * <p>
 * 	Besides the job must notify its manager when it is asked to start ({@link #execute(UWSJob)})
 *  and to end ({@link #remove(UWSJob)}).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public interface ExecutionManager {

	/**
	 * Gets the list of running jobs.
	 * 
	 * @return	An iterator on the running jobs.
	 */
	public Iterator<UWSJob> getRunningJobs();

	/**
	 * Gets the total number of running jobs.
	 * 
	 * @return	The number of running jobs.
	 */
	public int getNbRunningJobs();

	/**
	 * Gets the list of queued jobs.
	 * 
	 * @return	An iterator on the queued jobs.
	 */
	public Iterator<UWSJob> getQueuedJobs();

	/**
	 * Gets the total number of queued jobs.
	 * 
	 * @return	The number of queued jobs.
	 */
	public int getNbQueuedJobs();

	/**
	 * Refreshes the lists of running and queued jobs.
	 * 
	 * @throws UWSException		If there is an error while refreshing this manager.
	 */
	public void refresh() throws UWSException;

	/**
	 * <p>Lets deciding whether the given job can start immediately or whether it must be put in the queue.</p>
	 * 
	 * @param job	The job to execute.
	 * @return		The resulting execution phase of the given job.
	 * 
	 * @throws UWSException	If there is an error while changing the execution phase of the given job or if any other error occurs.
	 * 
	 * @see UWSJob#start(boolean)
	 * @see UWSJob#setPhase(ExecutionPhase)
	 */
	public ExecutionPhase execute(final UWSJob job) throws UWSException;

	/**
	 * Removes the job from this manager whatever is its current execution phase.
	 * 
	 * @param jobToRemove		The job to remove.
	 * 
	 * @throws UWSException		If there is an error while refreshing the list of running jobs or if any other error occurs.
	 */
	public void remove(final UWSJob jobToRemove) throws UWSException;
}
