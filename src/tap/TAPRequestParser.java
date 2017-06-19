package tap;

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
 * Copyright 2014-2017 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;
import uws.UWSToolBox;
import uws.service.file.UWSFileManager;
import uws.service.request.FormEncodedParser;
import uws.service.request.MultipartParser;
import uws.service.request.RequestParser;
import uws.service.request.UploadFile;

/**
 * This parser adapts the request parser to use in function of the request
 * content-type:
 * 
 * <ul>
 * 	<li><b>application/x-www-form-urlencoded</b>: {@link FormEncodedParser}</li>
 * 	<li><b>multipart/form-data</b>: {@link MultipartParser}</li>
 * 	<li><b>other</b>: no parameter is returned</li>
 * </ul>
 * 
 * <p>
 * 	The request body size is limited for the multipart. If you want to change
 * 	this limit, you MUST do it for each of these parsers, setting the following
 * 	static attributes: {@link MultipartParser#SIZE_LIMIT}.
 * </p>
 * 
 * <p><i>Note:
 * 	If you want to change the support other request parsing, you will have to
 * 	write your own {@link RequestParser} implementation.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.1 (06/2017)
 * @since 2.0
 */
public class TAPRequestParser implements RequestParser {

	/** File manager to use to create {@link UploadFile} instances.
	 * It is required by this new object to execute open, move and delete
	 * operations whenever it could be asked. */
	private final UWSFileManager fileManager;

	/** {@link RequestParser} to use when a application/x-www-form-urlencoded
	 * request must be parsed. This attribute is set by
	 * {@link #parse(HttpServletRequest)} only when needed, by calling the
	 * function {@link #getFormParser()}. */
	private RequestParser formParser = null;

	/** {@link RequestParser} to use when a multipart/form-data request must be
	 * parsed. This attribute is set by {@link #parse(HttpServletRequest)}
	 * only when needed, by calling the function {@link #getMultipartParser()}. */
	private RequestParser multipartParser = null;

	/**
	 * Build a {@link RequestParser} able to choose the most appropriate
	 * {@link RequestParser} in function of the request content-type.
	 * 
	 * @param fileManager	The file manager to use in order to store any
	 *                   	eventual upload. <b>MUST NOT be NULL</b>
	 */
	public TAPRequestParser(final UWSFileManager fileManager){
		if (fileManager == null)
			throw new NullPointerException("Missing file manager => can not create a TAPRequestParser!");
		this.fileManager = fileManager;
	}

	@Override
	public Map<String,Object> parse(final HttpServletRequest req) throws UWSException{
		if (req == null)
			return new HashMap<String,Object>();

		// Get the method:
		String method = (req.getMethod() == null) ? "" : req.getMethod().toLowerCase();

		if (method.equals("post") || method.equals("put")){
			Map<String,Object> params = null;

			// Get the parameters:
			if (FormEncodedParser.isFormEncodedRequest(req))
				params = getFormParser().parse(req);
			else if (MultipartParser.isMultipartContent(req))
				params = getMultipartParser().parse(req);
			else
				params = new HashMap<String,Object>(0);

			// Only for POST requests, the parameters specified in the URL must be added:
			if (method.equals("post"))
				params = UWSToolBox.addGETParameters(req, (params == null) ? new HashMap<String,Object>() : params);

			return params;
		}else
			return UWSToolBox.addGETParameters(req, new HashMap<String,Object>());
	}

	/**
	 * Get the {@link RequestParser} to use for
	 * application/x-www-form-urlencoded HTTP requests.
	 * This parser may be created if not already done.
	 * 
	 * @return	The {@link RequestParser} to use for
	 *        	application/x-www-form-urlencoded requests. <i>Never NULL</i>
	 */
	private synchronized final RequestParser getFormParser(){
		return (formParser != null) ? formParser : (formParser = new FormEncodedParser(){
			@Override
			protected void consumeParameter(String name, Object value, final Map<String,Object> allParams){
				// Modify the value if it is an UPLOAD parameter:
				if (name != null && name.equalsIgnoreCase("upload")){
					// if no value, ignore this parameter:
					if (value == null)
						return;
					// put in lower case the parameter name:
					name = name.toLowerCase();
					// transform the value in a String array:
					value = append((String)value, (allParams.containsKey("upload") ? (String[])allParams.get("upload") : null));
				}

				// Update the map, normally:
				super.consumeParameter(name, value, allParams);
			}
		});
	}

	/**
	 * Get the {@link RequestParser} to use for multipart/form-data HTTP
	 * requests.
	 * This parser may be created if not already done.
	 * 
	 * @return	The {@link RequestParser} to use for multipart/form-data
	 *        	requests. <i>Never NULL</i>
	 */
	private synchronized final RequestParser getMultipartParser(){
		return (multipartParser != null) ? multipartParser : (multipartParser = new MultipartParser(fileManager){
			@Override
			protected void consumeParameter(String name, Object value, final Map<String,Object> allParams){
				// Modify the value if it is an UPLOAD parameter:
				if (name != null && name.equalsIgnoreCase(TAPJob.PARAM_UPLOAD)){
					// if no value, ignore this parameter:
					if (value == null)
						return;
					// ignore also parameter having the same name in the same case and which is a file (only strings can be processed as DALI UPLOAD parameter):
					else if (name.equals(TAPJob.PARAM_UPLOAD) && value instanceof UploadFile){
						try{
							((UploadFile)value).deleteFile();
						}catch(IOException ioe){}
						return;
					}
					// use the same case for the parameter name:
					name = TAPJob.PARAM_UPLOAD;
					// transform the value in a String array:
					value = append((String)value, (allParams.containsKey(TAPJob.PARAM_UPLOAD) ? (String[])allParams.get(TAPJob.PARAM_UPLOAD) : null));
				}

				// Update the map, normally:
				super.consumeParameter(name, value, allParams);
			}
		});
	}

	/**
	 * Create a new array in which the given String is appended at the end of
	 * the given array.
	 * 
	 * @param value		String to append in the array.
	 * @param oldValue	The array after which the given String must be appended.
	 * 
	 * @return	The new array containing the values of the array and then the
	 *        	given String.
	 */
	private final static String[] append(final String value, final String[] oldValue){
		// Create the corresponding array of Strings:
		// ...if the array already exists, extend it:
		String[] newValue;
		if (oldValue != null){
			newValue = new String[oldValue.length + 1];
			for(int i = 0; i < oldValue.length; i++)
				newValue[i] = oldValue[i];
		}
		// ...otherwise, create a new array:
		else
			newValue = new String[1];

		// Add the new value in the array:
		newValue[newValue.length - 1] = value;

		// Update the value to put inside the map:
		return newValue;
	}

}
