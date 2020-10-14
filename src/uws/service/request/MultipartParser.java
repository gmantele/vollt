package uws.service.request;

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
 * Copyright 2014-2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import uws.UWSException;
import uws.service.UWS;
import uws.service.file.UWSFileManager;

/**
 * Extract parameters encoded using the Content-type
 * <code>multipart/form-data</code> in an {@link HttpServletRequest}.
 *
 * <h4>Uploaded file storage</h4>
 *
 * <p>
 * 	The created file(s) is(are) stored in the temporary upload directory
 * 	({@link UWSFileManager#getTmpDirectory()}. This directory is supposed to be
 * 	emptied regularly in case it is forgotten at any moment by the service
 * 	implementation to delete unused request files.
 * </p>
 *
 * <h4>Upload limits</h4>
 *
 * <p>
 * 	The size of the full request body as well as the size of a single uploaded
 * 	file are both limited. To get and/or change these limits, use the functions:
 * </p>
 * <ul>
 * 	<li><i>single file limit:</i> {@link #getMaxFileSize()} and
 * 	    {@link #setMaxFileSize(long)}</li>
 * 	<li><i>multipart request limit:</i> {@link #getMaxRequestSize()} and
 * 	    {@link #setMaxRequestSize(long)}</li>
 * </ul>
 * <p>
 * 	By default, the limit for an uploaded file is
 * 	{@value #DEFAULT_FILE_SIZE_LIMIT} (i.e. unlimited) and the limit for the
 * 	whole request (i.e. all uploaded files together + HTTP header) is
 * 	{@value #DEFAULT_SIZE_LIMIT} (i.e. unlimited}).
 * </p>
 *
 * <h4>Parameter consumption</h4>
 *
 * <p>
 * 	By default, this {@link RequestParser} overwrite parameter occurrences in
 * 	the map: that's to say if a parameter is provided several times, only the
 * 	last value will be kept. This behaviour can be changed by overwriting the
 * 	function {@link #consumeParameter(String, Object, Map)} of this class.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 4.4 (09/2018)
 * @since 4.1
 */
public class MultipartParser implements RequestParser {

	/** HTTP content-type for HTTP request formated in multipart. */
	public static final String EXPECTED_CONTENT_TYPE = "multipart/form-data";

	/** Default maximum allowed size for a single uploaded file:
	 * -1 (i.e. unlimited).
	 * @since 4.4 */
	public static final int DEFAULT_FILE_SIZE_LIMIT = -1;

	/** Default maximum allowed size for an HTTP request content:
	 * -1 (i.e. unlimited). */
	public static final int DEFAULT_SIZE_LIMIT = -1;

	/** Size threshold (in bytes) for an individual file before being stored on
	 * disk. Below this threshold, the file is only stored in memory.
	 * <p><i><b>Note:</b> By default, set to 10 kiB.</i></p>
	 * @since 4.4 */
	protected final static int SIZE_BEFORE_DISK_STORAGE = 10 * 1024;

	/** Maximum allowed size for an HTTP request content. Over this limit, an
	 * exception is thrown and the request is aborted.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The default value is {@link #DEFAULT_SIZE_LIMIT}
	 * 	(= {@value #DEFAULT_SIZE_LIMIT} MiB).
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	This limit is expressed in bytes and can not be negative.
	 *  Its smallest possible value is 0. If the set value is though negative,
	 *  it will be ignored and {@link #DEFAULT_SIZE_LIMIT} will be used instead.
	 * </i></p>
	 *
	 *  @deprecated Since 4.4 ; barely used and never worked (with COS). */
	@Deprecated
	public static int SIZE_LIMIT = DEFAULT_SIZE_LIMIT;

	/** Indicates whether this parser should allow inline files or not. */
	public final boolean allowUpload;

	/** File manager to use to create {@link UploadFile} instances.
	 * It is required by this new object to execute open, move and delete
	 * operations whenever it could be asked. */
	protected final UWSFileManager fileManager;

	/** Tool to parse Multipart HTTP request and fetch files when necessary.
	 * @since 4.4 */
	protected final ServletFileUpload fileUpload;

	/**
	 * Build a {@link MultipartParser} forbidding uploads (i.e. inline files).
	 *
	 * <p>
	 * 	With this parser, when an upload (i.e. submitted inline files) is
	 * 	detected, an exception is thrown by {@link #parse(HttpServletRequest)}
	 * 	which cancels immediately the request.
	 * </p>
	 */
	public MultipartParser(){
		this(false, null);
	}

	/**
	 * Build a {@link MultipartParser} allowing uploads (i.e. inline files).
	 *
	 * @param fileManager	The file manager to use in order to store any
	 *                   	eventual upload. <b>MUST NOT be NULL</b>
	 */
	public MultipartParser(final UWSFileManager fileManager){
		this(true, fileManager);
	}

	/**
	 * Build a {@link MultipartParser}.
	 *
	 * <p>
	 * 	If the first parameter is <i>false</i>, then when an upload
	 * 	(i.e. submitted inline files) is detected, an exception is thrown
	 * 	by {@link #parse(HttpServletRequest)} which cancels immediately the
	 * 	request.
	 * </p>
	 *
	 * @param uploadEnabled	<i>true</i> to allow uploads (i.e. inline files),
	 *                     	<i>false</i> otherwise. If <i>false</i>, the two
	 *                     	other parameters are useless.
	 * @param fileManager	The file manager to use in order to store any
	 *                   	eventual upload. <b>MUST NOT be NULL</b>
	 */
	protected MultipartParser(final boolean uploadEnabled, final UWSFileManager fileManager){
		this(uploadEnabled, fileManager, DEFAULT_FILE_SIZE_LIMIT, DEFAULT_SIZE_LIMIT);
	}

	/**
	 * Build a {@link MultipartParser}.
	 *
	 * <p>
	 * 	If the first parameter is <i>false</i>, then when an upload
	 * 	(i.e. submitted inline files) is detected, an exception is thrown
	 * 	by {@link #parse(HttpServletRequest)} which cancels immediately the
	 * 	request.
	 * </p>
	 *
	 * @param uploadEnabled		<i>true</i> to allow uploads (i.e. inline files),
	 *                     		<i>false</i> otherwise. If <i>false</i>, the two
	 *                     		other parameters are useless.
	 * @param fileManager		The file manager to use in order to store any
	 *                   		eventual upload. <b>MUST NOT be NULL</b>
	 * @param maxFileSize		Maximum size of a single upload file (in bytes).
	 *                   		<i>A negative value for "unlimited".</i>
	 * @param maxRequestSize	Maximum size of a whole multipart request (in
	 *                      	bytes).
	 *                   		<i>A negative value for "unlimited".</i>
	 *
	 * @since 4.4
	 */
	protected MultipartParser(final boolean uploadEnabled, final UWSFileManager fileManager, final long maxFileSize, final long maxRequestSize){
		if (uploadEnabled && fileManager == null)
			throw new NullPointerException("Missing file manager although the upload capability is enabled => can not create a MultipartParser!");

		this.allowUpload = uploadEnabled;
		this.fileManager = fileManager;

		// Create a factory for disk-based file items:
		DiskFileItemFactory factory = new DiskFileItemFactory();

		// Configure a repository:
		factory.setRepository(fileManager.getTmpDirectory());

		/* Set the maximum size of an in-memory file before being stored on the
		 * disk: */
		factory.setSizeThreshold(SIZE_BEFORE_DISK_STORAGE);

		// Create a new file upload handler:
		fileUpload = new ServletFileUpload(factory);

		// Set the maximum size for each single file:
		fileUpload.setFileSizeMax(maxFileSize);

		/* Set the maximum size for a whole multipart HTTP request
		 * (i.e. all files together): */
		fileUpload.setSizeMax(maxRequestSize);
	}

	/**
	 * Get the maximum size (in bytes) of a single uploaded file.
	 *
	 * @return	Maximum upload file size (in bytes),
	 *        	or -1 if no limit.
	 *
	 * @since 4.4
	 */
	public final long getMaxFileSize(){
		return fileUpload.getFileSizeMax();
	}

	/**
	 * Set the maximum size (in bytes) of a single uploaded file.
	 *
	 * @param maxFileSize	New maximum upload file size (in bytes).
	 *                      If <code>-1</code>, then there will be no limit.
	 *
	 * @since 4.4
	 */
	public void setMaxFileSize(final long maxFileSize){
		fileUpload.setFileSizeMax(maxFileSize);
	}

	/**
	 * Get the maximum size (in bytes) of a whole multipart request.
	 *
	 * @return	Maximum multipart request size (in bytes),
	 *        	or -1 if no limit.
	 *
	 * @since 4.4
	 */
	public final long getMaxRequestSize(){
		return fileUpload.getSizeMax();
	}

	/**
	 * Set the maximum size (in bytes) of a whole multipart request.
	 *
	 * @param maxRequestSize	New maximum multipart request size (in bytes).
	 *                      	If <code>-1</code>, then there will be no limit.
	 *
	 * @since 4.4
	 */
	public void setMaxRequestSize(final long maxRequestSize){
		fileUpload.setSizeMax((maxRequestSize < 0) ? -1 : maxRequestSize);
	}

	@Override
	public final Map<String, Object> parse(final HttpServletRequest request) throws UWSException{
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<String, Object>();

		try{
			List<FileItem> fileItems = fileUpload.parseRequest(request);
			for(FileItem item : fileItems){
				String name = item.getFieldName();
				InputStream stream = item.getInputStream();
				if (item.isFormField())
					consumeParameter(name, Streams.asString(stream), parameters);
				else{
					if (!allowUpload)
						throw new UWSException(UWSException.BAD_REQUEST, "Uploads are not allowed by this service!");
					else{
						// keep the file:
						File file = getFileFromParam(request, fileManager.getTmpDirectory().getPath(), FilenameUtils.getName(item.getName()));
						FileUtils.copyInputStreamToFile(stream, file);
						// build its description/pointer:
						UploadFile lob = new UploadFile(name, FilenameUtils.getName(item.getName()), file.toURI().toString(), fileManager);
						lob.mimeType = item.getContentType();
						lob.length = file.length();
						// add it inside the parameters map:
						consumeParameter(name, lob, parameters);
					}
				}
				// finally delete the file item stored by FileUpload:
				item.delete();
			}
		}catch(FileUploadException fue){
			throw new UWSException(UWSException.BAD_REQUEST, fue, "Incorrect HTTP request: " + fue.getMessage() + " (server limits: each file/parameter <= " + (fileUpload.getFileSizeMax() <= 0 ? "unlimited" : fileUpload.getFileSizeMax() + " bytes") + " and the whole request <= " + (fileUpload.getSizeMax() <= 0 ? "unlimited" : fileUpload.getSizeMax()) + " bytes)");
		}catch(IOException ioe){
			throw new UWSException(UWSException.BAD_REQUEST, ioe, "Incorrect HTTP request: " + ioe.getMessage());
		}catch(IllegalArgumentException iae){
			String confError = iae.getMessage();
			if (fileManager.getTmpDirectory() == null)
				confError = "Missing upload directory!";
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, iae, "Internal Error! Incorrect UPLOAD configuration: " + confError);
		}

		return parameters;
	}

	/**
	 * Return the path of a non-existing file inside the given directory and
	 * whose the name is built using the given file name and the HTTP request
	 * ID.
	 *
	 * @param request		The received HTTP request.
	 * @param parentFile	The directory in which the file should be created.
	 * @param inputFileName	The file name provided by the user.
	 *
	 * @return	Path of a non-existing file for the specified input file.
	 *
	 * @since 4.4
	 */
	protected File getFileFromParam(final HttpServletRequest request, final String parentFile, final String inputFileName){
		Object reqID = request.getAttribute(UWS.REQ_ATTRIBUTE_ID);
		if (reqID == null || !(reqID instanceof String))
			reqID = (new Date()).getTime();
		char uniq = 'A';
		File f = new File(parentFile, "UPLOAD_" + reqID + uniq + "_" + inputFileName);
		while(f.exists()){
			uniq++;
			f = new File(parentFile, "UPLOAD_" + reqID + "_" + inputFileName);
		}
		return f;
	}

	/**
	 * Consume the specified parameter: add it inside the given map.
	 *
	 * <p>
	 * 	By default, this function is just putting the given value inside the
	 * 	map. So, if the parameter already exists in the map, its old value will
	 * 	be overwritten by the given one.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If the old value was a file, it will be deleted from the file system
	 * 	before its replacement in the map.
	 * </i></p>
	 *
	 * @param name		Name of the parameter to consume.
	 * @param value		Its value.
	 * @param allParams	The list of all parameters read until now.
	 */
	protected void consumeParameter(final String name, final Object value, final Map<String, Object> allParams){
		// If the old value was a file, delete it before replacing its value:
		if (allParams.containsKey(name) && allParams.get(name) instanceof UploadFile){
			try{
				((UploadFile)allParams.get(name)).deleteFile();
			}catch(IOException ioe){
			}
		}

		// Put the given value in the given map:
		allParams.put(name, value);
	}

	/**
	 * Utility method that determines whether the content of the given request
	 * is a multipart/form-data.
	 *
	 * <p><i>Important:
	 * 	This function just test the content-type of the request. The HTTP method
	 * 	(e.g. GET, POST, ...) is not tested.
	 * </i></p>
	 *
	 * @param request The servlet request to be evaluated. Must be non-null.
	 *
	 * @return	<code>true</code> if the request is multipart,
	 *        	<code>false</code> otherwise.
	 */
	public static final boolean isMultipartContent(final HttpServletRequest request){
		return ServletFileUpload.isMultipartContent(request);
	}

}
