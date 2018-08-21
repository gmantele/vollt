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
 * Extract parameters encoded using the Content-type multipart/form-data
 * in an {@link HttpServletRequest}.
 *
 * <p>
 * 	The created file(s) is(are) stored in the temporary upload directory
 * 	({@link UWSFileManager#getTmpDirectory()}. This directory is supposed to be
 * 	emptied regularly in case it is forgotten at any moment by the service
 * 	implementation to delete unused request files.
 * </p>
 *
 * <p>
 * 	The size of the full request body is limited by the static attribute
 * 	{@link #SIZE_LIMIT} before the creation of the file. Its default value is:
 * 	{@link #DEFAULT_SIZE_LIMIT}={@value #DEFAULT_SIZE_LIMIT} bytes.
 * </p>
 *
 * <p>
 * 	By default, this {@link RequestParser} overwrite parameter occurrences in
 * 	the map: that's to say if a parameter is provided several times, only the
 * 	last value will be kept. This behaviour can be changed by overwriting the
 * 	function {@link #consumeParameter(String, Object, Map)} of this class.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 4.4 (08/2018)
 * @since 4.4
 */
public class MultipartParser implements RequestParser {

	/** HTTP content-type for HTTP request formated in multipart. */
	public static final String EXPECTED_CONTENT_TYPE = "multipart/form-data";

	/** Default maximum allowed size for an HTTP request content: 10 MiB. */
	public static final int DEFAULT_SIZE_LIMIT = 10 * 1024 * 1024;

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
	 * </i></p> */
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
		if (uploadEnabled && fileManager == null)
			throw new NullPointerException("Missing file manager although the upload capability is enabled => can not create a MultipartParser!");

		this.allowUpload = uploadEnabled;
		this.fileManager = fileManager;

		// Create a factory for disk-based file items:
		DiskFileItemFactory factory = new DiskFileItemFactory();

		// Configure a repository:
		factory.setRepository(fileManager.getTmpDirectory());

		// Create a new file upload handler
		fileUpload = new ServletFileUpload(factory);
		fileUpload.setFileSizeMax(SIZE_LIMIT);
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
			throw new UWSException(UWSException.BAD_REQUEST, fue, "Incorrect HTTP request: " + fue.getMessage() + ".");
		}catch(IOException ioe){
			throw new UWSException(UWSException.BAD_REQUEST, ioe, "Incorrect HTTP request: " + ioe.getMessage() + ".");
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
