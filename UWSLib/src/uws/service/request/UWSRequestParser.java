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
 * Copyright 2014-2017 - Astronomisches Rechen Institut (ARI)
 */

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;
import uws.UWSToolBox;
import uws.service.file.UWSFileManager;

/**
 * This parser adapts the request parser to use in function of the request
 * content-type:
 * 
 * <ul>
 * 	<li><b>application/x-www-form-urlencoded</b>: {@link FormEncodedParser}</li>
 * 	<li><b>multipart/form-data</b>: {@link MultipartParser}</li>
 * 	<li><b>(text|application)/(.+-)?xml</b>: {@link XMLRequestParser}
 * 			(the whole request body is an XML document)</li>
 * 	<li><b>other</b>: no parameter is returned</li>
 * </ul>
 * 
 * <p>
 * 	The request body size is limited for the multipart AND the XML-Request
 * 	parsers. If you want to change this limit, you MUST do it for each of these
 * 	parsers, setting the following static attributes: resp.
 * 	{@link MultipartParser#SIZE_LIMIT} and {@link XMLRequestParser#SIZE_LIMIT}
 * 	(and also {@link XMLRequestParser#SMALL_XML_THRESHOLD}).
 * </p>
 * 
 * <p><i>Note:
 * 	If you want to change the support other request parsing, you will have to
 * 	write your own {@link RequestParser} implementation.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.2 (06/2017)
 * @since 4.1
 */
public final class UWSRequestParser implements RequestParser {

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

	/** {@link RequestParser} to use for XML request (i.e. a HTTP request
	 * containing just an XML document). This attribute is set by
	 * {@link #parse(HttpServletRequest)} only when needed, by calling the
	 * function {@link #getXMLRequestParser()}. */
	private RequestParser xmlRequestParser = null;

	/**
	 * Build a {@link RequestParser} able to choose the most appropriate
	 * {@link RequestParser} in function of the request content-type.
	 * 
	 * @param fileManager	The file manager to use in order to store any
	 *                   	eventual upload. <i>Must NOT be NULL.</i>
	 */
	public UWSRequestParser(final UWSFileManager fileManager){
		if (fileManager == null)
			throw new NullPointerException("Missing file manager => can not create a UWSRequestParser!");
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
			else if (XMLRequestParser.isXMLRequest(req))
				params = getXMLRequestParser().parse(req);
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
		return (formParser == null) ? (formParser = new FormEncodedParser()) : formParser;
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
		return (multipartParser == null) ? (multipartParser = new MultipartParser(fileManager)) : multipartParser;
	}

	/**
	 * Get the {@link RequestParser} to use for HTTP requests whose the content
	 * is an XML document.
	 * This parser may be created if not already done.
	 * 
	 * @return	The {@link RequestParser} to use for XML requests.
	 *        	<i>Never NULL</i>
	 * 
	 * @since 4.2
	 */
	private synchronized final RequestParser getXMLRequestParser(){
		return (xmlRequestParser == null) ? (xmlRequestParser = new XMLRequestParser(fileManager)) : xmlRequestParser;
	}

}
