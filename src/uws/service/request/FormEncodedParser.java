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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;

/**
 * Extract parameters encoded using the HTTP-GET method or the Content-type
 * application/x-www-form-urlencoded with the HTTP-POST or HTTP-PUT method in
 * an {@link HttpServletRequest}.
 *
 * <p>
 * 	By default, this {@link RequestParser} overwrite parameter occurrences in
 * 	the map: that's to say if a parameter is provided several times, only the
 * 	last value will be kept. This behavior can be changed by overwriting the
 * 	function {@link #consumeParameter(String, Object, Map)} of this class.
 * </p>
 *
 * <p><i>Note:
 * 	When HTTP-POST is used, these parameters are actually already extracted by
 * 	the server application (like Apache/Tomcat) and are available with
 * 	{@link HttpServletRequest#getParameterMap()}. However, when using HTTP-PUT,
 * 	the parameters are extracted manually from the request content.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 4.3 (08/2018)
 * @since 4.1
 */
public class FormEncodedParser implements RequestParser {

	/** HTTP content-type for HTTP request formated in url-form-encoded. */
	public final static String EXPECTED_CONTENT_TYPE = "application/x-www-form-urlencoded";

	@Override
	public final Map<String, Object> parse(HttpServletRequest request) throws UWSException{
		if (request == null)
			return new HashMap<String, Object>();

		HashMap<String, Object> params = new HashMap<String, Object>();

		// Normal extraction for HTTP-POST and other HTTP methods:
		if (request.getMethod() == null || !request.getMethod().equalsIgnoreCase("put")){
			Enumeration<String> names = request.getParameterNames();
			String paramName;
			String[] values;
			int i;
			while(names.hasMoreElements()){
				paramName = names.nextElement();
				values = request.getParameterValues(paramName);
				// search for the last non-null occurrence:
				i = values.length - 1;
				while(i >= 0 && values[i] == null)
					i--;
				// if there is one, keep it:
				if (i >= 0)
					consumeParameter(paramName, values[i], params);
			}
		}
		/* Parameters are not extracted when using the HTTP-PUT method.
		 * This block is doing this extraction manually. */
		else{
			InputStream input = null;
			Scanner scanner = null;
			try{

				// Get the character encoding:
				String charEncoding = request.getCharacterEncoding();
				try{
					if (charEncoding == null || charEncoding.trim().length() == 0 || Charset.isSupported(charEncoding))
						charEncoding = "UTF-8";
				}catch(Exception ex){
					charEncoding = "UTF-8";
				}

				// Get a stream on the request content:
				input = new BufferedInputStream(request.getInputStream());
				// Read the stream by iterating on each parameter pairs:
				scanner = new Scanner(input);
				scanner.useDelimiter("&");
				String pair;
				int indSep;
				while(scanner.hasNext()){
					// get the pair:
					pair = scanner.next();
					// split it between the parameter name and value:
					indSep = pair.indexOf('=');
					try{
						if (indSep >= 0)
							consumeParameter(URLDecoder.decode(pair.substring(0, indSep), charEncoding), URLDecoder.decode(pair.substring(indSep + 1), charEncoding), params);
						else
							consumeParameter(URLDecoder.decode(pair, charEncoding), "", params);
					}catch(UnsupportedEncodingException uee){
						if (indSep >= 0)
							consumeParameter(pair.substring(0, indSep), pair.substring(indSep + 1), params);
						else
							consumeParameter(pair, "", params);
					}
				}

			}catch(IOException ioe){
			}finally{
				if (scanner != null)
					scanner.close();
				if (input != null){
					try{
						input.close();
					}catch(IOException ioe2){
					}
				}
			}
		}

		return params;
	}

	/**
	 * <p>Consume the specified parameter: add it inside the given map.</p>
	 *
	 * <p>
	 * 	By default, this function is just putting the given value inside the map. So, if the parameter already exists in the map,
	 * 	its old value will be overwritten by the given one.
	 * </p>
	 *
	 * @param name		Name of the parameter to consume.
	 * @param value		Its value.
	 * @param allParams	The list of all parameters read until now.
	 */
	protected void consumeParameter(final String name, final Object value, final Map<String, Object> allParams){
		allParams.put(name, value);
	}

	/**
	 * <p>Utility method that determines whether the content of the given request is a application/x-www-form-urlencoded.</p>
	 *
	 * <p><i>Important:
	 * 	This function just test the content-type of the request. The HTTP method (e.g. GET, POST, ...) is not tested.
	 * </i></p>
	 *
	 * @param request The servlet request to be evaluated. Must be non-null.
	 *
	 * @return	<i>true</i> if the request is url-form-encoded,
	 *        	<i>false</i> otherwise.
	 */
	public final static boolean isFormEncodedRequest(final HttpServletRequest request){
		// Extract the content type and determine if it is a url-form-encoded request:
		String contentType = request.getContentType();
		if (contentType == null)
			return false;
		else if (contentType.toLowerCase().startsWith(EXPECTED_CONTENT_TYPE))
			return true;
		else
			return false;
	}

}
