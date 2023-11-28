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

import java.io.Serializable;
import java.util.Date;

import uws.job.JobList;
import uws.job.UWSJob;
import uws.service.UWS;

/**
 * <p>Manages the automatic destruction of given jobs.</p>
 * 
 * <p>
 * 	Any job has a field named: destruction. It indicates when the job must be destroyed. Remember that destroying a job means
 * 	clearing all its resources (stopping it, deleting its result, ...) and removing it from its jobs list.
 * </p>
 * 
 * <p>
 * 	Each job must warn its jobs list of any change of its destruction time,
 * 	so that the jobs list can update the destruction manager with the method {@link #update(UWSJob)}.
 * 	Once the destruction time of a job is reached, it must be removed from this manager and
 *  from its jobs list (see {@link JobList#destroyJob(String)}).
 * </p>
 * 
 * <p>
 * 	<i><u>Note:</u>
 * 		{@link DefaultDestructionManager} is a default implementation of this interface.
 * 		It is used by default by any subclass of {@link UWS} and {@link JobList}.
 * 	</i>
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (12/2014)
 * 
 * @see DefaultDestructionManager
 */
public interface DestructionManager extends Serializable {
	/**
	 * Indicates whether a job is currently planned to be destroyed.
	 * 
	 * @return <i>true</i> if a destruction is currently planned, <i>false</i> otherwise.
	 */
	public boolean isRunning();

	/**
	 * Gets the destruction date/time of the job currently planned for destruction.
	 * 
	 * @return The time of the currently planned destruction.
	 */
	public Date getNextDestruction();

	/**
	 * Gets the ID of the job currently planned for destruction.
	 * 
	 * @return	The ID of the job to destroy.
	 */
	public String getNextJobToDestroy();

	/**
	 * Gets the total number of jobs planned to be destroyed.
	 * 
	 * @return	The jobs to destroy.
	 */
	public int getNbJobsToDestroy();

	/**
	 * <p>Refresh the list of jobs to destroy.</p>
	 * 
	 * <p>
	 * 	It may stop if there is not any more job to destroy.
	 * 	It may change the currently planned job if another job must be destroyed before it.
	 * </p>
	 */
	public void refresh();

	/**
	 * <p>Updates the list of jobs to destroy with the given job.</p>
	 * 
	 * <ul>
	 * 	<li>If the given job has no more destruction time or jobs list, it is removed.</li>
	 * 	<li>If the given job is already into this manager, the manager is refreshed so that take into account the possible modification of its destruction time.</li>
	 * 	<li>If the given job is not into this manager, it is added and the manager is refreshed (see {@link #refresh()}).</li>
	 * </ul>
	 * 
	 * @param job	The job whose the destruction time may have changed.
	 */
	public void update(UWSJob job);

	/**
	 * Removes the given job from this manager.
	 * If the given job is the currently planned job to destroy, the manager is then refreshed.
	 * 
	 * @param job	The job to remove.
	 */
	public void remove(UWSJob job);

	/**
	 * <p>Stop watching the destruction of jobs.</p>
	 * 
	 * <p><i>Note:
	 * 	A subsequent call to {@link #update(UWSJob)} may enable again this manager.
	 * </i></p>
	 * 
	 * @since 4.1
	 */
	public void stop();
}
