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

import java.io.File;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uws.job.user.JobOwner;

/**
 * Let's extracting the group name of any given job owner.
 * The name of the extracted name is the first letter of the given owner ID but if none can be found "_" is returned.
 * 
 * @author Gr&ecaute;gory Mantelet (CDS)
 * @version 04/2012
 */
public class DefaultOwnerGroupIdentifier implements OwnerGroupIdentifier {

	/** Pattern to extract the root directory of a user directory from its ID. */
	protected static final Pattern DIR_PREFIX_PATTERN = Pattern.compile(".*([a-zA-Z])[^a-zA-Z]*");

	@Override
	public String getOwnerGroup(JobOwner owner){
		if (owner == null || owner.getID() == null || owner.getID().trim().isEmpty())
			return null;
		else{
			// The user directory name = userID in which each directory separator char are replaced by a _ (=> no confusion with a path):
			String userDir = owner.getID().trim().replaceAll(Pattern.quote(File.separator), "_");

			// The parent directory = the first LETTER of the userID or _ if none can be found:
			String parentDir = "_";
			Matcher m = DIR_PREFIX_PATTERN.matcher(userDir);
			if (m.matches())
				parentDir = m.group(1).toLowerCase();

			return parentDir;
		}
	}

}
