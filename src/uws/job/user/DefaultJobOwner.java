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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uws.job.JobList;
import uws.job.UWSJob;

/**
 * <p>Default implementation of {@link JobOwner}.</p>
 * <p>
 * 	In this implementation, a job owner has only an ID and a pseudo (which may be the same).
 * 	However, other informations may be added thanks to {@link #putUserData(String, String)}.
 * 	These additional data will also be saved with the ID and the pseudo/name of the user
 * 	by a backup manager.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public final class DefaultJobOwner implements JobOwner {

	private final String id;
	private String pseudo;
	private HashMap<String,Object> otherData = null;

	/**
	 * Builds a Job Owner which has the given ID.
	 * Its pseudo will also be equal to the given ID.
	 * 
	 * @param name	ID/Pseudo of the Job Owner to create.
	 */
	public DefaultJobOwner(final String name){
		this(name, name);
	}

	public DefaultJobOwner(final String id, final String pseudo){
		this.id = id;
		this.pseudo = pseudo;
	}

	@Override
	public final String getID(){
		return id;
	}

	@Override
	public final String getPseudo(){
		return pseudo;
	}

	public final void setPseudo(final String pseudo){
		this.pseudo = pseudo;
	}

	/**
	 * By default: ALL users have the READ permission for ALL jobs lists.
	 * @see uws.job.user.JobOwner#hasReadPermission(uws.job.JobList)
	 */
	@Override
	public boolean hasReadPermission(JobList jl){
		return true;
	}

	/**
	 * By default: ALL users have the WRITE permission for ALL jobs lists.
	 * @see uws.job.user.JobOwner#hasWritePermission(uws.job.JobList)
	 */
	@Override
	public boolean hasWritePermission(JobList jl){
		return true;
	}

	/**
	 * By default: ONLY owners of the given job have the READ permission.
	 * @see uws.job.user.JobOwner#hasReadPermission(uws.job.UWSJob)
	 */
	@Override
	public boolean hasReadPermission(UWSJob job){
		return (job == null) || (job.getOwner() == null) || (job.getOwner().equals(this));
	}

	/**
	 * By default: ONLY owners of the given job have the WRITE permission.
	 * @see uws.job.user.JobOwner#hasWritePermission(uws.job.UWSJob)
	 */
	@Override
	public boolean hasWritePermission(UWSJob job){
		return (job == null) || (job.getOwner() == null) || (job.getOwner().equals(this));
	}

	/**
	 * By default: ONLY owners of the given job have the EXECUTE permission.
	 * @see uws.job.user.JobOwner#hasExecutePermission(uws.job.UWSJob)
	 */
	@Override
	public boolean hasExecutePermission(UWSJob job){
		return (job == null) || (job.getOwner() == null) || (job.getOwner().equals(this));
	}

	public String putUserData(final String name, final String value){
		if (otherData == null)
			otherData = new HashMap<String,Object>();
		return (String)otherData.put(name, value);
	}

	public String getUserData(final String name){
		return (otherData == null) ? null : (String)otherData.get(name);
	}

	public String removeUserData(final String name){
		return (otherData == null) ? null : (String)otherData.remove(name);
	}

	public Set<String> getAllUserData(){
		return (otherData == null) ? null : otherData.keySet();
	}

	@Override
	public Map<String,Object> getDataToSave(){
		return otherData;
	}

	@Override
	public void restoreData(Map<String,Object> data){
		if (data == null || data.isEmpty())
			return;

		if (otherData == null)
			otherData = new HashMap<String,Object>(data.size());

		otherData.putAll(data);
	}

	/**
	 * By default: the user ID.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		return id;
	}

	/**
	 * By default: a {@link DefaultJobOwner} is equal to any {@link JobOwner} only if their ID are equals.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj){
		if (obj == null || !(obj instanceof JobOwner))
			return false;

		String objId = ((JobOwner)obj).getID();
		return (id == null && objId == null) || (id != null && objId != null && id.equals(objId));
	}

	/**
	 * By default: this function returns the hashCode of the ID.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode(){
		return id.hashCode();
	}

}
