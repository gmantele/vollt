package uws.service.backup;

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

import uws.job.user.JobOwner;

/**
 * Let's saving and restoring the whole UWS.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public interface UWSBackupManager {

	/**
	 * Enables/Disables the backup of the associated UWS.
	 * 
	 * @param enableBackup	<i>true</i> to enable the backup, <i>false</i> otherwise.
	 */
	public void setEnabled(final boolean enabled);

	/**
	 * Save all jobs lists and all information about the jobs owners.
	 * 
	 * @return An array with the following information: number of saved jobs, number of all jobs, number of saved users and number of all users.
	 * 			<i>null</i> or an empty array is returned if there is a grave error while saving the whole UWS or the backup is disabled.
	 */
	public int[] saveAll();

	/**
	 * Restore all jobs list and all information about the jobs owners.
	 * 
	 * @return An array with the following information: number of restored jobs, number of all jobs, number of restored users and number of all users.
	 * 			<i>null</i> or an empty array is returned if there is a grave error while saving the whole UWS.
	 * 
	 * @throws IllegalStateException If the restoration can not be done now (i.e. if the restoration can be done only one time: at the creation of the UWS).
	 */
	public int[] restoreAll() throws IllegalStateException;

	/**
	 * Save ONLY the jobs of the given user and all information about this user.
	 * 
	 * @return An array with the following information: number of saved jobs, number of all jobs.
	 * 			<i>null</i> or an empty array is returned if there is a grave error while saving the whole UWS or the backup is disabled.
	 * 
	 * @throws IllegalArgumentException	If the given owner or its ID is <i>null</i>.
	 */
	public int[] saveOwner(final JobOwner owner) throws IllegalArgumentException;

}
