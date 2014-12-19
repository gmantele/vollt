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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;
import uws.service.UWS;
import uws.service.file.UWSFileManager;

/**
 * <p>This parser merely copies the whole HTTP request content inside a file.
 * It names this file: "JDL" (Job Description Language).</p>
 * 
 * <p>
 * 	The created file is stored in the temporary upload directory ({@link UWSFileManager#TMP_UPLOAD_DIR} ; this attribute can be modified if needed).
 * 	This directory is supposed to be emptied regularly in case it is forgotten at any moment by the UWS service implementation to delete unused request files.
 * </p>
 * 
 * <p>
 * 	The size of the JDL is limited by the static attribute {@link #SIZE_LIMIT} before the creation of the file.
 * 	Its default value is: {@link #DEFAULT_SIZE_LIMIT}={@value #DEFAULT_SIZE_LIMIT} bytes.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.1 (11/2014)
 * @since 4.1
 */
public class NoEncodingParser implements RequestParser {

	/** Default maximum allowed size for an HTTP request content: 2 MiB. */
	public static final int DEFAULT_SIZE_LIMIT = 2 * 1024 * 1024;

	/** <p>Maximum allowed size for an HTTP request content. Over this limit, an exception is thrown and the request is aborted.</p>
	 * <p><i>Note:
	 * 	The default value is {@link #DEFAULT_SIZE_LIMIT} (= {@value #DEFAULT_SIZE_LIMIT} MiB).
	 * </i></p>
	 * <p><i>Note:
	 * 	This limit is expressed in bytes and can not be negative.
	 *  Its smallest possible value is 0. If the set value is though negative,
	 *  it will be ignored and {@link #DEFAULT_SIZE_LIMIT} will be used instead.
	 * </i></p> */
	public static int SIZE_LIMIT = DEFAULT_SIZE_LIMIT;

	/** File manager to use to create {@link UploadFile} instances.
	 * It is required by this new object to execute open, move and delete operations whenever it could be asked. */
	protected final UWSFileManager fileManager;

	/**
	 * Build the request parser.
	 * 
	 * @param fileManager	A file manager. <b>MUST NOT be NULL</b>
	 */
	public NoEncodingParser(final UWSFileManager fileManager){
		if (fileManager == null)
			throw new NullPointerException("Missing file manager => can not create a SingleDataParser!");
		this.fileManager = fileManager;
	}

	@Override
	public Map<String,Object> parse(final HttpServletRequest request) throws UWSException{
		// Check the request size:
		if (request.getContentLength() <= 0)
			return new HashMap<String,Object>();
		else if (request.getContentLength() > (SIZE_LIMIT < 0 ? DEFAULT_SIZE_LIMIT : SIZE_LIMIT))
			throw new UWSException("JDL too big (>" + SIZE_LIMIT + " bytes) => Request rejected! You should see with the service administrator to extend this limit.");

		// Build the parameter name:
		String paramName;
		if (request.getMethod() != null && request.getMethod().equalsIgnoreCase("put")){
			paramName = request.getRequestURI();
			if (paramName.lastIndexOf('/') + 1 > 0)
				paramName = paramName.substring(paramName.lastIndexOf('/') + 1);
		}else
			paramName = "JDL";

		// Build the file by copy of the whole request body:
		Object reqID = request.getAttribute(UWS.REQ_ATTRIBUTE_ID);
		if (reqID == null || !(reqID instanceof String))
			reqID = (new Date()).getTime();
		File f = new File(UWSFileManager.TMP_UPLOAD_DIR, "REQUESTBODY_" + reqID);
		OutputStream output = null;
		InputStream input = null;
		long totalLength = 0;
		try{
			output = new BufferedOutputStream(new FileOutputStream(f));
			input = new BufferedInputStream(request.getInputStream());

			byte[] buffer = new byte[2049];
			int len = input.read(buffer);
			if (len <= 0){
				output.close();
				f.delete();
				HashMap<String,Object> params = new HashMap<String,Object>(1);
				params.put(paramName, "");
				return params;
			}else if (len <= 2048 && request.getMethod() != null && request.getMethod().equalsIgnoreCase("put") && request.getContentType() != null && request.getContentType().toLowerCase().startsWith("text/plain")){
				output.close();
				f.delete();
				HashMap<String,Object> params = new HashMap<String,Object>(1);
				params.put(paramName, new String(buffer, 0, len));
				return params;
			}else{
				do{
					output.write(buffer, 0, len);
					totalLength += len;
				}while((len = input.read(buffer)) > 0);
				output.flush();
			}
		}catch(IOException ioe){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Internal error => Impossible to get the JDL from the HTTP request!");
		}finally{
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe2){}
			}
			if (output != null){
				try{
					output.close();
				}catch(IOException ioe2){}
			}
		}

		// Build its description:
		UploadFile lob = new UploadFile(paramName, f.toURI().toString(), fileManager);
		lob.mimeType = request.getContentType();
		lob.length = totalLength;

		// Create the parameters map:
		HashMap<String,Object> parameters = new HashMap<String,Object>();
		parameters.put(paramName, lob);

		return parameters;
	}

}
