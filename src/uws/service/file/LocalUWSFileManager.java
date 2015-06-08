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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.ErrorSummary;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.UploadFile;

/**
 * <p>All UWS files are stored in the local machine into the specified directory.</p>
 * 
 * <p>
 * 	The name of the log file, the result files and the backup files may be customized by overriding the following functions:
 * 	{@link #getLogFileName(uws.service.log.UWSLog.LogLevel, String)}, {@link #getResultFileName(Result, UWSJob)}, {@link #getBackupFileName(JobOwner)} and {@link #getBackupFileName()}.
 * </p>
 * 
 * <p>
 * 	By default, results and backups are grouped by owner/user and owners/users are grouped thanks to {@link DefaultOwnerGroupIdentifier}.
 * 	By using the appropriate constructor, you can change these default behaviors.
 * </p>
 * 
 * <p>
 * 	A log file rotation is set by default so that avoiding a too big log file after several months/years of use.
 * 	By default the rotation is done every month on the 1st at 6am. This frequency can be changed easily thanks to the function
 * 	{@link #setLogRotationFreq(String)}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (02/2015)
 */
public class LocalUWSFileManager implements UWSFileManager {

	/** Format to use to format dates. */
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/** Default name of the log file. */
	protected static final String DEFAULT_LOG_FILE_NAME = "service.log";
	/** Default name of the general UWS backup file. */
	protected static final String DEFAULT_BACKUP_FILE_NAME = "service.backup";

	/** Directory in which all files managed by this class will be written and read. */
	protected final File rootDirectory;

	/** Output toward the service log file. */
	protected PrintWriter logOutput = null;
	/** Frequency at which the log file must be "rotated" (the file is renamed with the date of its first write and a new log file is created).
	 * Thus, too big log files can be avoided. */
	protected EventFrequency logRotation = new EventFrequency("D 0 0");	// Log file rotation every day at midnight. 

	/** Indicate whether a directory must be used to gather all jobs, results and errors related to one identified user.
	 * If FALSE, all jobs, results and errors will be in only one directory, whoever owns them. */
	protected final boolean oneDirectoryForEachUser;
	/** Gather user directories, set by set. At the end, several user group directories may be created.
	 * This option is considered only if {@link #oneDirectoryForEachUser} is TRUE. */
	protected final boolean groupUserDirectories;
	/** Object giving the policy about how to group user directories. */
	protected final OwnerGroupIdentifier ownerGroupId;

	/**
	 * <p>Builds a {@link UWSFileManager} which manages all UWS files in the given directory.</p>
	 * <p>
	 * 	There will be one directory for each owner ID and owner directories will be grouped
	 * 	thanks to {@link DefaultOwnerGroupIdentifier}.
	 * </p>
	 * 
	 * @param root				UWS root directory.
	 *
	 * @throws NullPointerException	If the given root directory is <i>null</i>.
	 * @throws UWSException			If the given file is not a directory or has not the READ and WRITE permissions.
	 * 
	 * @see #LocalUWSFileManager(File, boolean, boolean, OwnerGroupIdentifier)
	 */
	public LocalUWSFileManager(final File root) throws UWSException{
		this(root, true, true, null);
	}

	/**
	 * <p>Builds a {@link UWSFileManager} which manages all UWS files in the given directory.</p>
	 * <p>
	 * 	If, according to the third parameter, the owner directories must be grouped,
	 * 	the {@link DefaultOwnerGroupIdentifier} will be used.
	 * </p>
	 * 
	 * @param root						UWS root directory.
	 * @param oneDirectoryForEachUser	<i>true</i> to create one directory for each owner ID, <i>false</i> otherwise.
	 * @param groupUserDirectories		<i>true</i> to group user directories, <i>false</i> otherwise.
	 * 									<i><u>note:</u> this value is ignored if the previous parameter is false.</i>
	 *
	 * @throws NullPointerException	If the given root directory is <i>null</i>.
	 * @throws UWSException			If the given file is not a directory or has not the READ and WRITE permissions.
	 * 
	 * @see #LocalUWSFileManager(File, boolean, boolean, OwnerGroupIdentifier)
	 */
	public LocalUWSFileManager(final File root, final boolean oneDirectoryForEachUser, final boolean groupUserDirectories) throws UWSException{
		this(root, oneDirectoryForEachUser, groupUserDirectories, null);
	}

	/**
	 * Builds a {@link UWSFileManager} which manages all UWS files in the given directory.
	 * 
	 * @param root						UWS root directory.
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
	 * @throws NullPointerException	If the given root directory is <i>null</i>.
	 * @throws UWSException			If the given file is not a directory or has not the READ and WRITE permissions.
	 */
	public LocalUWSFileManager(final File root, final boolean oneDirectoryForEachUser, final boolean groupUserDirectories, final OwnerGroupIdentifier ownerGroupIdentifier) throws UWSException{
		if (root == null)
			throw new NullPointerException("Missing root directory ! Impossible to create a LocalUWSFileManager.");
		else if (!root.exists()){
			if (!root.mkdirs())
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "The given root directory does not exist and can not be created automatically !");
		}else if (!root.isDirectory())
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "The root directory of a UWSFileManager must be a DIRECTORY !");
		else if (!root.canRead())
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Missing READ permission for the root directory of a UWSFileManager !");
		else if (!root.canWrite())
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Missing WRITE permission for the root directory of a UWSFileManager !");

		rootDirectory = root;

		this.oneDirectoryForEachUser = oneDirectoryForEachUser;
		if (this.oneDirectoryForEachUser){
			this.groupUserDirectories = groupUserDirectories;
			if (this.groupUserDirectories){
				if (ownerGroupIdentifier != null)
					this.ownerGroupId = ownerGroupIdentifier;
				else
					this.ownerGroupId = new DefaultOwnerGroupIdentifier();
			}else
				this.ownerGroupId = null;
		}else{
			this.groupUserDirectories = false;
			this.ownerGroupId = null;
		}
	}

	/**
	 * Gets the directory of the given owner.
	 * 
	 * @param owner	A job owner.
	 * @return		Its directory.
	 */
	public File getOwnerDirectory(final JobOwner owner){
		if (!oneDirectoryForEachUser || owner == null || owner.getID() == null || owner.getID().trim().isEmpty())
			return rootDirectory;

		File ownerDir = rootDirectory;
		if (groupUserDirectories){
			String ownerGroup = ownerGroupId.getOwnerGroup(owner);
			if (ownerGroup != null)
				ownerDir = new File(rootDirectory, ownerGroup);
		}
		ownerDir = new File(ownerDir, owner.getID().replaceAll(File.separator, "_"));

		return ownerDir;
	}

	/**
	 * Removes the owner directory if there is no more file in it (except the backup file which is no more required).
	 * 
	 * @param owner			The user whose the directory must be removed.
	 * 
	 * @throws IOException	If there is an error while removing the owner directory.
	 */
	protected void cleanOwnerDirectory(final JobOwner owner) throws IOException{
		// Remove the owner directory if empty or if only the owner backup file exists:
		if (owner != null && oneDirectoryForEachUser){
			File ownerDir = getOwnerDirectory(owner);
			String[] dirContent = ownerDir.list();
			// if empty...
			if (dirContent.length <= 1){
				// delete the owner backup file it it exists:
				if (dirContent.length == 1 && dirContent[0].equals(getBackupFileName(owner))){
					(new File(rootDirectory, getBackupFileName(owner))).delete();
					dirContent = ownerDir.list();
				}
				// if empty (so, if the owner backup file does not exist any more)...
				if (dirContent.length == 0){
					// delete the owner directory !
					if (ownerDir.delete() && groupUserDirectories){
						// if the user group directory is also empty now, delete it:
						File userGroupDir = ownerDir.getParentFile();
						if (userGroupDir.list().length == 0)
							userGroupDir.delete();
					}
				}
			}
		}
	}

	/* ******************* */
	/* LOG FILE MANAGEMENT */
	/* ******************* */

	/**
	 * Get the frequency of the log file rotation
	 * in a human readable way.
	 * 
	 * @return	A human readable frequency of the log file rotation.
	 */
	public final String getLogRotationFreq(){
		return logRotation.toString();
	}

	/**
	 * <p>Set the frequency at which a rotation of the log file must be done.</p>
	 * 
	 * <p>
	 * 	"rotation" means here, to close the currently used log file, to rename it so that suffixing it
	 * 	with the date at which the first log has been written in it, and to create a new log file.
	 * </p>
	 * 
	 * <p>The frequency string must respect the following syntax:</p>
	 * <ul>
	 * 	<li>'D' hh mm : daily schedule at hh:mm</li>
	 * 	<li>'W' dd hh mm : weekly schedule at the given day of the week (1:sunday, 2:monday, ..., 7:saturday) at hh:mm</li>
	 * 	<li>'M' dd hh mm : monthly schedule at the given day of the month at hh:mm</li>
	 * 	<li>'h' mm : hourly schedule at the given minute</li>
	 * 	<li>'m' : scheduled every minute (for completness :-))</li>
	 * </ul>
	 * <p><i>Where: hh = integer between 0 and 23, mm = integer between 0 and 59, dd (for 'W') = integer between 1 and 7 (1:sunday, 2:monday, ..., 7:saturday),
	 * dd (for 'M') = integer between 1 and 31.</i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	The frequency type is case sensitive! Then you should particularly pay attention at the case
	 * 	when using the frequency types 'M' (monthly) and 'm' (every minute).
	 * </p>
	 * 
	 * <p>
	 * 	Parsing errors are not thrown but "resolved" silently. The "solution" depends of the error.
	 * 	2 cases of errors are considered:
	 * </p>
	 * <ul>
	 * 	<li><b>Frequency type mismatch:</b> It happens when the first character is not one of the expected (D, W, M, h, m).
	 * 	                                    That means: bad case (i.e. 'd' rather than 'D'), another character.
	 * 	                                    In this case, the frequency will be: <b>daily at 00:00</b>.</li>
	 * 
	 * 	<li><b>Parameter(s) missing or incorrect:</b> With the "daily" frequency ('D'), at least 2 parameters must be provided ;
	 * 	                                             3 for "weekly" ('W') and "monthly" ('M') ; only 1 for "hourly" ('h') ; none for "every minute" ('m').
	 * 	                                             This number of parameters is a minimum: only the n first parameters will be considered while
	 * 	                                             the others will be ignored.
	 * 	                                             If this minimum number of parameters is not respected or if a parameter value is incorrect,
	 * 	                                             <b>all parameters will be set to their default value</b>
	 * 	                                             (which is 0 for all parameter except dd for which it is 1).</li>
	 * </ul>
	 * 
	 * <p>Examples:</p>
	 * <ul>
	 * 	<li><i>"" or NULL</i> = every day at 00:00</li>
	 * 	<li><i>"D 06 30" or "D 6 30"</i> = every day at 06:30</li>
	 * 	<li><i>"D 24 30"</i> = every day at 00:00, because hh must respect the rule: 0 &le; hh &le; 23</li>
	 * 	<li><i>"d 06 30" or "T 06 30"</i> = every day at 00:00, because the frequency type "d" (lower case of "D") or "T" do not exist</li>
	 * 	<li><i>"W 2 6 30"</i> = every week on Tuesday at 06:30</li>
	 * 	<li><i>"W 8 06 30"</i> = every week on Sunday at 00:00, because with 'W' dd must respect the rule: 1 &le; dd &le; 7</li>
	 * 	<li><i>"M 2 6 30"</i> = every month on the 2nd at 06:30</li>
	 * 	<li><i>"M 32 6 30"</i> = every month on the 1st at 00:00, because with 'M' dd must respect the rule: 1 &le; dd &le; 31</li>
	 * 	<li><i>"M 5 6 30 12"</i> = every month on the 5th at 06:30, because at least 3 parameters are expected and so considered: "12" and eventual other parameters are ignored</li>
	 * </ul>
	 * 
	 * @param interval	Interval between two log rotations.
	 */
	public final void setLogRotationFreq(final String interval){
		logRotation = new EventFrequency(interval);
	}

	/**
	 * <p>Gets the name of the UWS log file.</p>
	 * 
	 * <p>By default: {@link #DEFAULT_LOG_FILE_NAME}.</p>
	 * 
	 * @param level		Level of the message to log (DEBUG, INFO, WARNING, ERROR, FATAL).
	 * @param context	Context of the message to log (UWS, HTTP, THREAD, JOB, ...).
	 * 
	 * @return	The name of the UWS log file.
	 */
	protected String getLogFileName(final LogLevel level, final String context){
		return DEFAULT_LOG_FILE_NAME;
	}

	/**
	 * Gets the UWS log file.
	 * 
	 * @param level		Level of the message to log (DEBUG, INFO, WARNING, ERROR, FATAL).
	 * @param context	Context of the message to log (UWS, HTTP, THREAD, JOB, ...).
	 * 
	 * @return	The UWS log file.
	 * 
	 * @see #getLogFileName(uws.service.log.UWSLog.LogLevel, String)
	 */
	protected File getLogFile(final LogLevel level, final String context){
		return new File(rootDirectory, getLogFileName(level, context));
	}

	@Override
	public InputStream getLogInput(final LogLevel level, final String context) throws IOException{
		File logFile = getLogFile(level, context);
		if (logFile.exists())
			return new FileInputStream(logFile);
		else
			return null;
	}

	@Override
	public synchronized PrintWriter getLogOutput(final LogLevel level, final String context) throws IOException{
		// If a file rotation is needed...
		if (logOutput != null && logRotation != null && logRotation.isTimeElapsed()){
			// ...Close the output stream:
			logOutput.close();
			logOutput = null;

			// ...Rename this log file:
			// get the file:
			File logFile = getLogFile(level, context);
			// and its name:
			String logFileName = logFile.getName();
			// separate the file name from the extension:
			String fileExt = "";
			int indFileExt = logFileName.lastIndexOf('.');
			if (indFileExt >= 0){
				fileExt = logFileName.substring(indFileExt);
				logFileName = logFileName.substring(0, indFileExt);
			}
			// build the new file name and rename the log file:
			logFile.renameTo(new File(logFile.getParentFile(), logFileName + "_" + logRotation.getEventID() + fileExt));
		}

		// If the log output is not yet set or if a file rotation has been done...
		if (logOutput == null){
			// ...Create the output:
			File logFile = getLogFile(level, context);
			createParentDir(logFile);
			logOutput = new PrintWriter(new FileOutputStream(logFile, true), true);

			// ...Write a log header:
			printLogHeader(logOutput);

			// ...Set the date of the next rotation:
			if (logRotation != null)
				logRotation.nextEvent();
		}

		return logOutput;
	}

	/**
	 * Print a header into the log file so that separating older log messages to the new ones.
	 */
	protected void printLogHeader(final PrintWriter out){
		String msgHeader = "########################################### LOG STARTS " + dateFormat.format(new Date()) + " (file rotation: " + logRotation + ") ###########################################";
		StringBuffer buf = new StringBuffer("");
		for(int i = 0; i < msgHeader.length(); i++)
			buf.append('#');
		String separator = buf.toString();

		out.println(separator);
		out.println(msgHeader);
		out.println(separator);

		out.flush();
	}

	/* ************************* */
	/* UPLOADED FILES MANAGEMENT */
	/* ************************* */

	/**
	 * Create a File instance from the given upload file description.
	 * This function is able to deal with location as URI and as file path. 
	 * 
	 * @param upload	Description of an uploaded file.
	 * 
	 * @return	The corresponding File object.
	 * 
	 * @since 4.1
	 */
	protected final File getFile(final UploadFile upload){
		if (upload.getLocation().startsWith("file:")){
			try{
				return new File(new URI(upload.getLocation()));
			}catch(URISyntaxException use){
				return new File(upload.getLocation());
			}
		}else
			return new File(upload.getLocation());
	}

	@Override
	public InputStream getUploadInput(final UploadFile upload) throws IOException{
		// Check the source file:
		File source = getFile(upload);
		if (!source.exists())
			throw new FileNotFoundException("The uploaded file submitted with the parameter \"" + upload.paramName + "\" can not be found any more on the server!");
		// Return the stream:
		return new FileInputStream(source);
	}

	@Override
	public InputStream openURI(final URI uri) throws UnsupportedURIProtocolException, IOException{
		String scheme = uri.getScheme();
		if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("ftp"))
			return uri.toURL().openStream();
		else
			throw new UnsupportedURIProtocolException(uri);
	}

	@Override
	public void deleteUpload(final UploadFile upload) throws IOException{
		File f = getFile(upload);
		if (!f.exists())
			return;
		else if (f.isDirectory())
			throw new IOException("Incorrect location! An uploaded file must be a regular file, not a directory. (file location: \"" + f.getPath() + "\")");
		else{
			try{
				if (!f.delete())
					throw new IOException("Can not delete the file!");
			}catch(SecurityException se){
				throw new IOException("Unexpected permission restriction on the uploaded file \"" + f.getPath() + "\" => can not delete it!");
			}
		}
	}

	@Override
	public String moveUpload(final UploadFile upload, final UWSJob destination) throws IOException{
		// Check the source file:
		File source = getFile(upload);
		if (!source.exists())
			throw new FileNotFoundException("The uploaded file submitted with the parameter \"" + upload.paramName + "\" can not be found any more on the server!");

		// Build the final location (in the owner directory, under the name "UPLOAD_{job-id}_{param-name}":
		File ownerDir = getOwnerDirectory(destination.getOwner());
		File copy = new File(ownerDir, "UPLOAD_" + destination.getJobId() + "_" + upload.paramName);

		OutputStream output = null;
		InputStream input = null;
		boolean done = false;
		try{
			// open the input and output:
			input = new BufferedInputStream(getUploadInput(upload));
			output = new BufferedOutputStream(new FileOutputStream(copy));
			// proceed to the copy:
			byte[] buffer = new byte[2048];
			int len;
			while((len = input.read(buffer)) > 0)
				output.write(buffer, 0, len);
			output.flush();
			output.close();
			output = null;
			// close the input and delete the source file:
			input.close();
			input = null;
			source.delete();
			// return the new location:
			done = true;
			return copy.toURI().toString();
		}finally{
			if (output != null){
				try{
					output.close();
				}catch(IOException ioe){}
			}
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe){}
			}
			// In case of problem, the copy must be deleted:
			if (!done && copy.exists()){
				try{
					copy.delete();
				}catch(SecurityException ioe){}
			}
		}
	}

	/* *********************** */
	/* RESULT FILES MANAGEMENT */
	/* *********************** */
	/**
	 * <p>Gets the name of the file in which the given result is/must be written.</p>
	 * <p>By default: jobID + "_" + resultID + "." + {@link UWSToolBox#getFileExtension(String) getFileExtension(resultMIMEType)}</p>
	 * <p><i><u>note:</u> there is no file extension if the MIME type of the result is unknown !</i></p>
	 * 
	 * @param result	The result whose the file name is asked.
	 * @param job		The job which owns the given result.
	 * 
	 * @return			Name of the file corresponding to the given result.
	 * 
	 * @see UWSToolBox#getFileExtension(String)
	 */
	protected String getResultFileName(final Result result, final UWSJob job){
		String fileName = job.getJobId() + "_";

		if (result != null && result.getId() != null && !result.getId().trim().isEmpty())
			fileName += result.getId();
		else
			fileName += Result.DEFAULT_RESULT_NAME;

		String fileExt = UWSToolBox.getFileExtension(result.getMimeType());
		fileExt = (fileExt == null) ? "" : ("." + fileExt);
		fileName += fileExt;

		return fileName;
	}

	/**
	 * Gets the file corresponding to the given result.
	 * 
	 * @param result	The result whose the file is asked.
	 * @param job		The job which owns the given result.
	 * 
	 * @return			The file corresponding to the given result.
	 * 
	 * @see #getOwnerDirectory(JobOwner)
	 * @see #getResultFileName(Result, UWSJob)
	 */
	protected File getResultFile(final Result result, final UWSJob job){
		File ownerDir = getOwnerDirectory(job.getOwner());
		return new File(ownerDir, getResultFileName(result, job));
	}

	@Override
	public InputStream getResultInput(Result result, UWSJob job) throws IOException{
		File resultFile = getResultFile(result, job);
		return resultFile.exists() ? new FileInputStream(resultFile) : null;
	}

	@Override
	public OutputStream getResultOutput(Result result, UWSJob job) throws IOException{
		File resultFile = getResultFile(result, job);
		createParentDir(resultFile);
		return new FileOutputStream(resultFile);
	}

	@Override
	public long getResultSize(Result result, UWSJob job) throws IOException{
		File resultFile = getResultFile(result, job);
		if (resultFile == null || !resultFile.exists())
			return -1;
		else
			return resultFile.length();
	}

	@Override
	public boolean deleteResult(Result result, UWSJob job) throws IOException{
		boolean deleted = getResultFile(result, job).delete();

		if (deleted)
			cleanOwnerDirectory(job.getOwner());

		return deleted;
	}

	/* ********************** */
	/* ERROR FILES MANAGEMENT */
	/* ********************** */
	/**
	 * <p>Gets the name of the file in which the described error is/must be written.</p>
	 * <p>By default: jobID + "_ERROR.log"</p>
	 * 
	 * @param error		The description of the error whose the file name is asked.
	 * @param job		The job which owns the given error.
	 * 
	 * @return			Name of the file corresponding to the described error.
	 */
	protected String getErrorFileName(final ErrorSummary error, final UWSJob job){
		return job.getJobId() + "_ERROR.log";
	}

	/**
	 * Gets the file corresponding to the described error.
	 * 
	 * @param error		The error whose the file is asked.
	 * @param job		The job which owns the given error.
	 * 
	 * @return			The file corresponding to the described error.
	 * 
	 * @see #getOwnerDirectory(JobOwner)
	 * @see #getErrorFileName(ErrorSummary, UWSJob)
	 */
	protected File getErrorFile(final ErrorSummary error, final UWSJob job){
		File ownerDir = getOwnerDirectory(job.getOwner());
		return new File(ownerDir, getErrorFileName(error, job));
	}

	@Override
	public InputStream getErrorInput(ErrorSummary error, UWSJob job) throws IOException{
		File errorFile = getErrorFile(error, job);
		return errorFile.exists() ? new FileInputStream(errorFile) : null;
	}

	@Override
	public OutputStream getErrorOutput(ErrorSummary error, UWSJob job) throws IOException{
		File errorFile = getErrorFile(error, job);
		createParentDir(errorFile);
		return new FileOutputStream(errorFile);
	}

	@Override
	public long getErrorSize(ErrorSummary error, UWSJob job) throws IOException{
		File errorFile = getErrorFile(error, job);
		if (errorFile == null || !errorFile.exists())
			return -1;
		else
			return errorFile.length();
	}

	@Override
	public boolean deleteError(ErrorSummary error, UWSJob job) throws IOException{
		boolean deleted = getErrorFile(error, job).delete();

		if (deleted)
			cleanOwnerDirectory(job.getOwner());

		return deleted;
	}

	/* *********************** */
	/* BACKUP FILES MANAGEMENT */
	/* *********************** */
	/**
	 * <p>Gets the name of the backup file of the given job owner (~ UWS user).</p>
	 * <p>By default: ownerID + ".backup"</p>
	 * 
	 * @param owner	The job owner whose the name of the backup file is asked.
	 * 
	 * @return		The name of the backup file of the given owner.
	 * 
	 * @throws IllegalArgumentException	If the given owner is <i>null</i> or an empty string.
	 */
	protected String getBackupFileName(final JobOwner owner) throws IllegalArgumentException{
		if (owner == null || owner.getID() == null || owner.getID().trim().isEmpty())
			throw new IllegalArgumentException("Missing owner! Can not get the backup file of an unknown owner.");
		return owner.getID().replaceAll(File.separator, "_") + ".backup";
	}

	@Override
	public InputStream getBackupInput(JobOwner owner) throws IllegalArgumentException, IOException{
		File backupFile = new File(getOwnerDirectory(owner), getBackupFileName(owner));
		return backupFile.exists() ? new FileInputStream(backupFile) : null;
	}

	@Override
	public Iterator<InputStream> getAllUserBackupInputs(){
		return new LocalAllUserBackupInputs(this);
	}

	@Override
	public OutputStream getBackupOutput(JobOwner owner) throws IllegalArgumentException, IOException{
		File backupFile = new File(getOwnerDirectory(owner), getBackupFileName(owner));
		createParentDir(backupFile);
		return new FileOutputStream(backupFile);
	}

	/**
	 * <p>Gets the name of the UWS general backup file.</p>
	 * <p>By default: {@link #DEFAULT_BACKUP_FILE_NAME}</p>
	 * 
	 * @return		The name of the UWS general backup file.
	 */
	protected String getBackupFileName(){
		return DEFAULT_BACKUP_FILE_NAME;
	}

	@Override
	public InputStream getBackupInput() throws IOException{
		File backupFile = new File(rootDirectory, getBackupFileName());
		return backupFile.exists() ? new FileInputStream(backupFile) : null;
	}

	@Override
	public OutputStream getBackupOutput() throws IOException{
		File backupFile = new File(rootDirectory, getBackupFileName());
		createParentDir(backupFile);
		return new FileOutputStream(backupFile);
	}

	/* ************** */
	/* TOOL FUNCTIONS */
	/* ************** */

	/**
	 * Creates the parent directory(ies) if it(they) does/do not exist.
	 * 
	 * @param f	The file whose the parent directory must exist after the call of this function.
	 * 
	 * @return	<i>true</i> if the parent directory now exists, <i>false</i> otherwise.
	 */
	protected boolean createParentDir(final File f){
		if (!f.getParentFile().exists())
			return f.getParentFile().mkdirs();
		else
			return true;
	}

	/**
	 * Lets iterating on all user backup files.
	 * The {@link #next()} function creates and returns the {@link InputStream} for the next backup file.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 05/2012
	 */
	protected class LocalAllUserBackupInputs implements Iterator<InputStream> {

		private final LocalUWSFileManager fileManager;
		private Iterator<File> itBackupFiles;

		private final FileFilter dirFilter = new DirectoryFilter();
		private final OwnerFileFilter ownerFileFilter = new OwnerFileFilter();

		public LocalAllUserBackupInputs(final LocalUWSFileManager fm){
			fileManager = fm;
			itBackupFiles = loadAllBackupFiles().iterator();
		}

		private ArrayList<File> loadAllBackupFiles(){
			ArrayList<File> backupFiles = new ArrayList<File>();

			// If there must be 1 directory by user:
			if (fileManager.oneDirectoryForEachUser){
				File[] dir0 = fileManager.rootDirectory.listFiles(dirFilter);
				// If user directories must be grouped (so, we have the list of all user groups):
				if (fileManager.groupUserDirectories){
					for(File groupDir : dir0){
						File[] dir1 = groupDir.listFiles(dirFilter);
						for(File userDir : dir1)
							addOwnerBackupFiles(backupFiles, userDir, userDir.getName());
					}
				}// Otherwise: We have already all user directories:
				else{
					for(File userDir : dir0)
						addOwnerBackupFiles(backupFiles, userDir, userDir.getName());
				}

			}// Otherwise: Get all backup files into the root directory:
			else
				addOwnerBackupFiles(backupFiles, fileManager.rootDirectory, null);

			return backupFiles;
		}

		private void addOwnerBackupFiles(final ArrayList<File> files, final File rootDirectory, final String ownerID){
			ownerFileFilter.setOwnerID(ownerID);
			File[] backups = rootDirectory.listFiles(ownerFileFilter);
			for(File f : backups)
				files.add(f);
		}

		@Override
		public boolean hasNext(){
			return itBackupFiles != null && itBackupFiles.hasNext();
		}

		/**
		 * If the file whose the input stream must be created and returned does not exist
		 * or has not the READ permission, <i>null</i> will be returned.
		 * 
		 * @see java.util.Iterator#next()
		 */
		@Override
		public InputStream next() throws NoSuchElementException{
			if (itBackupFiles == null)
				throw new NoSuchElementException();

			try{
				File f = itBackupFiles.next();
				if (!itBackupFiles.hasNext())
					itBackupFiles = null;

				return (f == null || !f.exists()) ? null : new FileInputStream(f);

			}catch(FileNotFoundException e){
				return null;
			}
		}

		@Override
		public void remove(){
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Filter which lets returning only the directories.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 05/2012
	 */
	protected final static class DirectoryFilter implements FileFilter {
		@Override
		public boolean accept(File f){
			return f != null && f.isDirectory();
		}
	}

	/**
	 * Filter which lets returning only the backup file(s) of the specified user/owner.
	 * 
	 * @author Gr&ecaute;gory Mantelet (CDS)
	 * @version 05/2012
	 */
	protected final class OwnerFileFilter implements FileFilter {
		protected String ownerID = null;

		/**
		 * Sets the ID of the user whose the backup file must be returned.
		 * If <i>null</i>, all the found backup files will be returned EXCEPT the backup file for the whole UWS.
		 * 
		 * @param ownerID	ID of the user whose the backup file must be returned. (MAY BE NULL)
		 */
		public void setOwnerID(final String ownerID){
			this.ownerID = ownerID;
		}

		@Override
		public boolean accept(File f){
			if (f == null || f.isDirectory())
				return false;
			else if (ownerID == null || ownerID.trim().isEmpty())
				return f.getName().endsWith(".backup") && !f.getName().equalsIgnoreCase(getBackupFileName());
			else
				return f.getName().equalsIgnoreCase(ownerID + ".backup");
		}
	}

}
