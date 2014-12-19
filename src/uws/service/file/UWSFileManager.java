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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;

import uws.job.ErrorSummary;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.UploadFile;

/**
 * <p>Lets accessing any file managed by a UWS service.</p>
 * 
 * <p>
 * 	It is particularly useful if you want to organize yourself
 * 	the results, log or backup file generated and read by a UWS.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (11/2014)
 * 
 * @see LocalUWSFileManager
 */
public interface UWSFileManager {

	/* ******************* */
	/* LOG FILE MANAGEMENT */
	/* ******************* */

	/**
	 * Gets an input stream on the log file of this UWS.
	 * 
	 * @param level			Level of the message to log (DEBUG, INFO, WARNING, ERROR or FATAL).
	 * @param context		Context of the message to log (UWS, HTTP, JOB, THREAD, ...).
	 * 
	 * @return				An input on the log file or <i>null</i> if there is no log file.
	 * 
	 * @throws IOException	If there is an error while opening an input stream on the log file.
	 */
	public InputStream getLogInput(final LogLevel level, final String context) throws IOException;

	/**
	 * <p>Gets an output stream on the log file of this UWS.</p>
	 * <p><i><u>note:</u> The log file must be automatically created if needed.</i></p>
	 * 
	 * @param level			Level of the message to log (DEBUG, INFO, WARNING, ERROR or FATAL).
	 * @param context		Context of the message to log (UWS, HTTP, JOB, THREAD, ...).
	 * 
	 * @return				An output on the log file.
	 * 
	 * @throws IOException	If there is an error while creating the log file or while opening an output stream on it.
	 */
	public PrintWriter getLogOutput(final LogLevel level, final String context) throws IOException;

	/* ************************* */
	/* UPLOADED FILES MANAGEMENT */
	/* ************************* */

	/** <p>Temporary directory in which uploaded files will be stored when parsing the HTTP request.</p>
	 * <p><i>IMPORTANT 1:
	 * 	Uploaded files should be then moved using {@link UploadFile#move(UWSJob)} when the job creation or update is validated.
	 * </i></p>
	 * <p><i>IMPORTANT 2:
	 * 	As qualified above, this directory is <b>temporary</b>. It means that it should be emptied sometimes.
	 * 	It is particularly important because when a delete or move operation fails on uploaded files, no log or error might
	 * 	be published.
	 * </i></p>
	 * <p><i>Note:
	 * 	The default value is the temporary directory of the system (i.e. \tmp or \var\tmp on Unix/Linux/MacOS, c:\temp on Windows).
	 * </i></p>
	 * @since 4.1 */
	public static File TMP_UPLOAD_DIR = new File(System.getProperty("java.io.tmpdir"));

	/**
	 * Open a stream toward the specified file, submitted inline in an HTTP request.
	 * 
	 * @param upload	Description of the uploaded file.
	 * 
	 * @return	Input to the specified uploaded file.
	 * 
	 * @throws IOException	If any error occurs while opening the stream.
	 * 
	 * @since 4.1
	 */
	public InputStream getUploadInput(final UploadFile upload) throws IOException;

	/**
	 * <p>Open a stream toward the given URI.</p>
	 * 
	 * <p>
	 * 	Most of the time, the given URI uses the protocol http, https or ftp, which makes
	 * 	the URI perfectly understandable by {@link URL} which is then able to open easily
	 * 	a stream (cf {@link URL#openStream()}). However, a different scheme/protocol could
	 * 	be used ; particularly VO ones like "ivo" and "vos". It is for these particular
	 * 	cases that this function has been designed: in order to provide an implementation
	 * 	supporting additional protocols.
	 * </p>
	 * 
	 * @param uri	URI of any resource to read.
	 * 
	 * @return	Input to the specified resource.
	 * 
	 * @throws UnsupporteURIProtocol	If the protocol is not supported by this implementation.
	 * @throws IOException				If another error occurs while opening the stream.
	 * 
	 * @since 4.1
	 */
	public InputStream openURI(final URI uri) throws UnsupportedURIProtocolException, IOException;

	/**
	 * Delete definitely the specified file, submitted inline in an HTTP request.
	 * 
	 * @param upload	Description of the uploaded file.
	 * 
	 * @throws IOException	If any error occurs while deleting the file.
	 * 
	 * @since 4.1
	 */
	public void deleteUpload(final UploadFile upload) throws IOException;

	/**
	 * <p>Move the specified file from its current location to a location related to the given job.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is generally used only once: after the HTTP request parsing, when creating or updating a job and only if the action has been accepted.
	 * </i></p>
	 * 
	 * <p><b>IMPORTANT:
	 * 	This function might not be able to update the location inside the given {@link UploadFile}. For this reason,
	 * 	it is strongly recommended to not call directly this function, but to use {@link UploadFile#move(UWSJob)}.
	 * </b></p>
	 * 
	 * @param upload		Description of the uploaded file to move.
	 * @param destination	Job in which the uploaded file will be used.
	 * 
	 * @return	The new location of the uploaded file.
	 * 
	 * @throws IOException	If any error occurs while moving the file.
	 * 
	 * @since 4.1
	 */
	public String moveUpload(final UploadFile upload, final UWSJob destination) throws IOException;

	/* *********************** */
	/* RESULT FILES MANAGEMENT */
	/* *********************** */

	/**
	 * Gets an input stream on the result file corresponding to the given job result.
	 * @param result		The description of the result file to read.
	 * @param job			The job of the given result.
	 * @return				An input of the corresponding result file or <i>null</i> if there is no result file.
	 * @throws IOException	If there is an error while opening an input stream on the result file.
	 */
	public InputStream getResultInput(final Result result, final UWSJob job) throws IOException;

	/**
	 * <p>Gets an output stream on the result file corresponding to the given job result.</p>
	 * <p><i><u>note:</u> The result file must be automatically created if needed.</i></p>
	 * @param result		The description of the result file to write.
	 * @param job			The job of the given result.
	 * @return				An output of the corresponding result file.
	 * @throws IOException	If there is an error while creating the result file or while opening an output stream on it.
	 */
	public OutputStream getResultOutput(final Result result, final UWSJob job) throws IOException;

	/**
	 * Gets the size of the specified result file.
	 * @param result		Description of the result file whose the size is wanted.
	 * @param job			The job of the given result.
	 * @return				The size (in bytes) of the specified result file or <i>-1</i> if unknown or the file does not exist.
	 * @throws IOException	If there is an error while getting the result file size.
	 */
	public long getResultSize(final Result result, final UWSJob job) throws IOException;

	/**
	 * Deletes the result file corresponding to the given job result's description.
	 * @param result		The description of the result file to delete.
	 * @param job			The job of the given result.
	 * @return				<i>true</i> if the result file has been deleted or if the result file does not exists, <i>false</i> otherwise.
	 * @throws IOException	If there is a grave and unexpected error while deleting the result file.
	 */
	public boolean deleteResult(final Result result, final UWSJob job) throws IOException;

	/* ********************** */
	/* ERROR FILES MANAGEMENT */
	/* ********************** */

	/**
	 * Gets an input stream on the error file corresponding to the given error and job.
	 * @param error			The description of the error file to read.
	 * @param job			The job of the given error.
	 * @return				An input of the corresponding error file or <i>null</i> if there is no error file.
	 * @throws IOException	If there is an error while opening an input stream on the error file.
	 */
	public InputStream getErrorInput(final ErrorSummary error, final UWSJob job) throws IOException;

	/**
	 * <p>Gets an output stream on the error file corresponding to the given error and job.</p>
	 * <p><i><u>note:</u> The error file must be automatically created if needed.</i></p>
	 * @param error			The description of the error file to write.
	 * @param job			The job of the given error.
	 * @return				An output of the corresponding error file.
	 * @throws IOException	If there is an error while creating the error file or while opening an output stream on it.
	 */
	public OutputStream getErrorOutput(final ErrorSummary error, final UWSJob job) throws IOException;

	/**
	 * Gets the size of the specified error summary file.
	 * @param error			Description of the error file whose the size is wanted.
	 * @param job			The job of the given error.
	 * @return				The size (in bytes) of the specified error file or <i>-1</i> if unknown or the file does not exist.
	 * @throws IOException	If there is an error while getting the error file size.
	 */
	public long getErrorSize(final ErrorSummary error, final UWSJob job) throws IOException;

	/**
	 * Deletes the errir file corresponding to the given job error summary.
	 * @param error			The description of the error file to delete.
	 * @param job			The job of the given error.
	 * @return				<i>true</i> if the error file has been deleted or if the error file does not exists, <i>false</i> otherwise.
	 * @throws IOException	If there is a grave and unexpected error while deleting the error file.
	 */
	public boolean deleteError(final ErrorSummary error, final UWSJob job) throws IOException;

	/* *********************** */
	/* BACKUP FILES MANAGEMENT */
	/* *********************** */

	/**
	 * Gets an input stream on the backup file of ONLY the given job owner (~ UWS user).
	 * @param owner						Owner whose the jobs must be fetched from the file on which the input stream is asked.
	 * @return							An input on the backup file of the given owner or <i>null</i> if there is no backup file.
	 * @throws IllegalArgumentException	If the given owner is <i>null</i>.
	 * @throws IOException				If there is an error while opening an input stream on the backup file.
	 */
	public InputStream getBackupInput(final JobOwner owner) throws IllegalArgumentException, IOException;

	/**
	 * Gets an input stream on the backup file of ALL the job owners (~ UWS user).
	 * @return	An iterator on all job owner's backup file.
	 */
	public Iterator<InputStream> getAllUserBackupInputs();

	/**
	 * <p>Gets an output stream on the backup file of ONLY the given job owner (~ UWS user).</p>
	 * <p><i><u>note:</u> The backup file must be automatically created if needed.</i></p>
	 * @param owner						Owner whose the jobs must be saved in the file on which the output stream is asked.
	 * @return							An output on the backup file of the given owner.
	 * @throws IllegalArgumentException	If the given owner is <i>null</i>.
	 * @throws IOException				If there is an error while creating the backup file or while opening an output stream on it.
	 */
	public OutputStream getBackupOutput(final JobOwner owner) throws IllegalArgumentException, IOException;

	/**
	 * Gets an input stream on the backup file of the whole UWS.
	 * @return				An input on the backup file of the whole UWS or <i>null</i> if there is no backup file.
	 * @throws IOException	If there is an error while opening an input stream of the backup file.
	 */
	public InputStream getBackupInput() throws IOException;

	/**
	 * <p>Gets an output stream on the backup file of the whole UWS.</p>
	 * <p><i><u>note:</u> The backup file must be automatically created if needed.</i></p>
	 * @return				An output on the backup file of the whole UWS.
	 * @throws IOException	If there is an error while creating the backup file or while opening an output stream on it.
	 */
	public OutputStream getBackupOutput() throws IOException;

}
