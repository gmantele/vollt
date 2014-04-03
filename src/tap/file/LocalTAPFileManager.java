package tap.file;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.File;

import tap.db.DBConnection;
import uws.UWSException;

import uws.service.file.DefaultOwnerGroupIdentifier;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.OwnerGroupIdentifier;

/**
 * <p>
 * 	Lets creating and managing all files needed in a TAP service.
 * 	These files are: UWS job results and errors, log files, backup files and the upload directory.
 * </p>
 * <p>
 * 	All files are written in the local machine, into the given directory.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 * 
 * @see LocalUWSFileManager
 */
public class LocalTAPFileManager extends LocalUWSFileManager implements TAPFileManager {

	/** Default name of the upload directory. */
	public final static String DEFAULT_UPLOAD_DIRECTORY_NAME = "Upload";

	/** Default name of the DB activity log file. */
	public final static String DEFAULT_DB_ACTIVITY_LOG_FILE_NAME = "service_db_activity.log";

	/** Local directory in which all uploaded files will be kept until they are read or ignored (in this case, they will be deleted). */
	private final File uploadDirectory;

	/**
	 * <p>Builds a {@link TAPFileManager} which manages all UWS files in the given directory.</p>
	 * <p>
	 * 	There will be one directory for each owner ID and owner directories will be grouped
	 * 	thanks to {@link DefaultOwnerGroupIdentifier}.
	 * </p>
	 * 
	 * @param root				TAP root directory.
	 *
	 * @throws UWSException		If the given root directory is <i>null</i>, is not a directory or has not the READ and WRITE permissions.
	 * 
	 * @see LocalUWSFileManager#LocalUWSFileManager(File)
	 * @see #getUploadDirectoryName()
	 */
	public LocalTAPFileManager(File root) throws UWSException{
		super(root);
		uploadDirectory = new File(rootDirectory, getUploadDirectoryName());
	}

	/**
	 * <p>Builds a {@link TAPFileManager} which manages all UWS files in the given directory.</p>
	 * <p>
	 * 	If, according to the third parameter, the owner directories must be grouped,
	 * 	the {@link DefaultOwnerGroupIdentifier} will be used.
	 * </p>
	 * 
	 * @param root						TAP root directory.
	 * @param oneDirectoryForEachUser	<i>true</i> to create one directory for each owner ID, <i>false</i> otherwise.
	 * @param groupUserDirectories		<i>true</i> to group user directories, <i>false</i> otherwise.
	 * 									<i><u>note:</u> this value is ignored if the previous parameter is false.</i>
	 *
	 * @throws UWSException				If the given root directory is <i>null</i>, is not a directory or has not the READ and WRITE permissions.
	 * 
	 * @see LocalUWSFileManager#LocalUWSFileManager(File, boolean, boolean)
	 * @see #getUploadDirectoryName()
	 */
	public LocalTAPFileManager(File root, boolean oneDirectoryForEachUser, boolean groupUserDirectories) throws UWSException{
		super(root, oneDirectoryForEachUser, groupUserDirectories);
		uploadDirectory = new File(rootDirectory, getUploadDirectoryName());
	}

	/**
	 * Builds a {@link TAPFileManager} which manages all UWS files in the given directory.
	 * 
	 * @param root						TAP root directory.
	 * @param oneDirectoryForEachUser	<i>true</i> to create one directory for each owner ID, <i>false</i> otherwise.
	 * @param groupUserDirectories		<i>true</i> to group user directories, <i>false</i> otherwise.
	 * 									<i><u>note:</u> this value is ignored if the previous parameter is false.</i>
	 * @param ownerGroupIdentifier		The "function" to use to identify the group of a job owner.
	 * 									<i><ul>
	 * 										<li><u>note 1:</u> this value is ignored if one of the two previous parameters is false.</li>
	 * 										<li><u>note 2:</u> if this value is null but the previous parameters are true,
	 * 											{@link DefaultOwnerGroupIdentifier} will be chosen as default group identifier.</li>
	 *									</ul></i>
	 *
	 * @throws UWSException				If the given root directory is <i>null</i>, is not a directory or has not the READ and WRITE permissions.
	 * 
	 * @see LocalUWSFileManager#LocalUWSFileManager(File, boolean, boolean, OwnerGroupIdentifier)
	 * @see #getUploadDirectoryName()
	 */
	public LocalTAPFileManager(File root, boolean oneDirectoryForEachUser, boolean groupUserDirectories, OwnerGroupIdentifier ownerGroupIdentifier) throws UWSException{
		super(root, oneDirectoryForEachUser, groupUserDirectories, ownerGroupIdentifier);
		uploadDirectory = new File(rootDirectory, getUploadDirectoryName());
	}

	@Override
	protected String getLogFileName(final String logTypeGroup){
		if (logTypeGroup != null && logTypeGroup.equals(DBConnection.LOG_TYPE_DB_ACTIVITY.getCustomType()))
			return DEFAULT_DB_ACTIVITY_LOG_FILE_NAME;
		else
			return super.getLogFileName(logTypeGroup);
	}

	/**
	 * <p>Gets the name of the directory in which all uploaded files will be saved.</p>
	 * 
	 * <p><i><u>note 1:</u> this function is called ONLY one time: at the creation.</i></p>
	 * <p><i><u>note 2:</u> by default, this function returns: {@link #DEFAULT_UPLOAD_DIRECTORY_NAME}.</i></p>
	 * 
	 * @return	The name of the upload directory.
	 */
	protected String getUploadDirectoryName(){
		return DEFAULT_UPLOAD_DIRECTORY_NAME;
	}

	@Override
	public final File getUploadDirectory(){
		if (uploadDirectory != null && !uploadDirectory.exists())
			uploadDirectory.mkdirs();
		return uploadDirectory;
	}

}
