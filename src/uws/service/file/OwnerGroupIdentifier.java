package uws.service.file;

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
 * <p>Let's grouping some owner directories.</p>
 * 
 * <p>
 * 	Rather than having the following hierarchy from the UWS root directory:
 * 		- root
 * 		 L owner1_dir
 * 		  L result1.xml
 * 		  L result2.xml
 * 		  L owner1.backup
 * 		 L owner2_dir
 * 		  L ...
 * 		 L owner3_dir
 * 		  L ...
 * 	This class let's you grouping owner directories thanks to the function {@link #getOwnerGroup(JobOwner)}:
 * 		- root
 * 		 L group1
 * 		  L owner1_dir
 * 		   L result1.xml
 * 		   L result2.xml
 * 		   L owner1.backup
 * 		  L owner3_dir
 * 		   L ...
 * 		 L group2
 * 		  L owner2_dir
 * 		   L ...
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 04/2012
 * 
 * @see DefaultOwnerGroupIdentifier
 */
public interface OwnerGroupIdentifier {

	/**
	 * Let's extracting the group name of the given job owner.
	 * @param owner	The job owner whose the group name must be returned.
	 * @return		Group name of the given job owner or <i>null</i> if the given job owner is <i>null</i>.
	 */
	public String getOwnerGroup(final JobOwner owner);

}
