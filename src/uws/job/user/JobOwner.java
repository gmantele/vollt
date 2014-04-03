package uws.job.user;

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

import java.util.Map;

import uws.job.JobList;
import uws.job.UWSJob;

/**
 * Represents a user of a UWS and a potential job owner.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 * 
 * @see uws.service.UserIdentifier
 */
public interface JobOwner {

	/**
	 * Unique ID of a user.
	 * 
	 * @return	User ID.
	 */
	public String getID();

	/**
	 * Pseudo of a user.
	 * It is used only to display the identity of a user.
	 * 
	 * <i><u>note:</u> a same pseudo may be used by several job owner.</i>
	 * 
	 * @return	Pseudo/Name of a user.
	 */
	public String getPseudo();

	/**
	 * Tells whether this user has the right to list the jobs of the given jobs list.
	 * 
	 * @param jl	A jobs list.
	 * 
	 * @return		<i>true</i> if this user can read the given jobs list, <i>false</i> otherwise.
	 */
	public boolean hasReadPermission(final JobList jl);

	/**
	 * Tells whether this user has the right to add/remove jobs to/from the given jobs list.
	 * 
	 * @param jl	A jobs list.
	 * 
	 * @return		<i>true</i> if this user can write the given jobs list, <i>false</i> otherwise.
	 */
	public boolean hasWritePermission(final JobList jl);

	/**
	 * Tells whether this user has the right to read all parameters, errors and results of the given job.
	 * 
	 * @param job	A job.
	 * 
	 * @return		<i>true</i> if this user can read the given job, <i>false</i> otherwise.
	 */
	public boolean hasReadPermission(final UWSJob job);

	/**
	 * Tells whether this user has the right to destroy the given job and to change the value of its parameters.
	 * 
	 * @param job	A job.
	 * 
	 * @return		<i>true</i> if this user can write the given job, <i>false</i> otherwise.
	 */
	public boolean hasWritePermission(final UWSJob job);

	/**
	 * Tells whether this user has the right to execute and to abort the given job.
	 * 
	 * @param job	A job.
	 * 
	 * @return		<i>true</i> if this user can execute/abort the given job, <i>false</i> otherwise.
	 */
	public boolean hasExecutePermission(final UWSJob job);

	/**
	 * Gets all user data to save (including ID and pseudo/name).
	 * 
	 * @return	A map with some key/value pairs, <i>null</i> or an empty map if no additional user data.
	 */
	public Map<String, Object> getDataToSave();

	/**
	 * Restores any additional user data from the given map.
	 * 
	 * @param data	A map with some key/value pairs, <i>null</i> or an empty map if no additional user data.
	 */
	public void restoreData(final Map<String, Object> data);

}
