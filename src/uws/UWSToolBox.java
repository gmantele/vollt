package uws;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
 * Copyright 2012-2024 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import uws.job.ErrorSummary;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSUrl;
import uws.service.UserIdentifier;
import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog;
import uws.service.request.RequestParser;
import uws.service.request.UploadFile;

/**
 * Some useful functions for the managing of a UWS service.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.5 (08/2024)
 */
public class UWSToolBox {

	/**
	 * Default character encoding for all HTTP response sent by this library.
	 * @since 4.1 */
	public final static String DEFAULT_CHAR_ENCODING = "UTF-8";

	private static UWSLog defaultLogger = null;

	/** <b>THIS CLASS CAN'T BE INSTANTIATED !</b> */
	private UWSToolBox(){
		;
	}

	/**
	 * <p>Lets building the absolute URL of any resource available in the root server, from a relative URL.</p>
	 * <p>For instance, if the server URL is http://foo.org/uwstuto (and whatever is the current URL):</p>
	 * <ul>
	 * 	<li><i>if you want the URL of the html page "basic.html" of the root directory:</i> servletPath=<b>"/basic.html"</b> => returned URL=<b>http://foo.org/uwstuto/basic.html</b></li>
	 * 	<li><i>if you want the URL of the image "ivoa.png" contained into the directory "images":</i> servletPath=<b>"/images/ivoa.png"</b> => returned URL=<b>"http://foo.org/uwstuto/images/ivoa.png"</b></li>
	 * </ul>
	 *
	 * @param serverPath	The relative path to access a server resource.
	 * @param req			A request of the servlet.
	 *
	 * @return				The absolute URL to access the desired server resource
	 * 						or <i>null</i> if one of the parameter is <i>null</i> or if a well-formed URL can not be built.
	 *
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getContextPath()
	 * @see URL#URL(String)
	 */
	public static final URL getServerResource(String serverPath, HttpServletRequest req){
		if (serverPath == null || req == null)
			return null;

		try{
			if (serverPath.length() > 0 && serverPath.charAt(0) != '/')
				serverPath = "/" + serverPath;

			return new URL(req.getRequestURL().substring(0, req.getRequestURL().lastIndexOf(req.getContextPath()) + req.getContextPath().length()) + serverPath);
		}catch(MalformedURLException e){
			return null;
		}
	}

	/**
	 * Gets the default {@link UWSLog} instance.
	 * Any log message will be print on the standard error output ({@link System#err}).
	 *
	 * @return	The default {@link UWSLog} instance.
	 */
	public static final UWSLog getDefaultLogger(){
		if (defaultLogger == null)
			defaultLogger = new DefaultUWSLog(System.err);
		return defaultLogger;
	}

	/* ****************** */
	/* PARAMETERS METHODS */
	/* ****************** */
	/**
	 * <p>Builds a map of strings with all parameters of the given HTTP request.</p>
	 *
	 * <p><i>Note:
	 * 	If the request attribute {@link UWS#REQ_ATTRIBUTE_PARAMETERS} has been already set by the UWS library,
	 * 	this map (after conversion into a Map<String,String>) is returned.
	 * 	Otherwise, the parameters identified automatically by the Servlet are returned (just the last occurrence of each parameter is kept).
	 * </i></p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	This function does not extract directly the parameters from the request content. It is just returning those already extracted
	 * 	either by the Servlet or by a {@link RequestParser}.
	 * </i></p>
	 *
	 * @param req	The HTTP request which contains the parameters to extract.
	 *
	 * @return		The corresponding map of string.
	 */
	@SuppressWarnings("unchecked")
	public static final Map<String, String> getParamsMap(final HttpServletRequest req){
		HashMap<String, String> map = new HashMap<String, String>();

		/* If the attribute "PARAMETERS" has been already set by the UWS library,
		 * return it by casting it from Map<String,Object> into Map<String,String>: */
		if (req.getAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS) != null){
			try{
				// Get the extracted parameters:
				Map<String, Object> params = (Map<String, Object>)req.getAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS);

				// Transform the map of Objects into a map of Strings:
				for(Map.Entry<String, Object> e : params.entrySet()){
					if (e.getValue() != null){
						if (e.getValue().getClass().isArray()){
							StringBuffer str = new StringBuffer();
							Object array = e.getValue();
							int length = Array.getLength(array);
							for(int i = 0; i < length; i++){
								if (i > 0)
									str.append(';');
								str.append(Array.get(array, i));
							}
							map.put(e.getKey(), str.toString());
						}else
							map.put(e.getKey(), e.getValue().toString());
					}
				}

				// Return the fetched map:
				return map;

			}catch(Exception ex){
				map.clear();
			}
		}

		/* If there is no "PARAMETERS" attribute or if an error occurs while reading it,
		 * return all the parameters fetched by the Servlet: */
		Enumeration<String> names = req.getParameterNames();
		int i;
		String n;
		String[] values;
		while(names.hasMoreElements()){
			n = names.nextElement();
			values = req.getParameterValues(n);
			// search for the last non-null occurrence:
			i = values.length - 1;
			while(i >= 0 && values[i] == null)
				i--;
			// if there is one, keep it:
			if (i >= 0)
				map.put(n.toLowerCase(), values[i]);
		}
		return map;
	}

	/**
	 * Converts map of UWS parameters into a string corresponding to the query part of a HTTP-GET URL (i.e. ?EXECUTIONDURATION=60&DESTRUCTION=2010-09-01T13:58:00:000-0200).
	 *
	 * @param parameters	A Map of parameters.
	 *
	 * @return				The corresponding query part of an HTTP-GET URL (all keys have been set in upper case).
	 */
	public final static String getQueryPart(final Map<String, String> parameters){
		if (parameters == null || parameters.isEmpty())
			return "";

		StringBuffer queryPart = new StringBuffer();
		for(Map.Entry<String, String> e : parameters.entrySet()){
			String key = e.getKey();
			String val = e.getValue();

			if (key != null)
				key = key.trim().toUpperCase();
			if (val != null)
				val = val.trim();

			if (key != null && !key.isEmpty() && val != null && !val.isEmpty()){
				try{
					queryPart.append(URLEncoder.encode(e.getKey(), "UTF-8") + "=" + URLEncoder.encode(val, "UTF-8"));
					queryPart.append("&");
				}catch(UnsupportedEncodingException uee){
				}
			}
		}

		return queryPart.substring(0, queryPart.length() - 1);
	}

	/**
	 * Converts the given query part of a HTTP-GET URL to a map of parameters.
	 *
	 * @param queryPart		A query part of a HTTP-GET URL.
	 *
	 * @return				The corresponding map of parameters (all keys have been set in lower case).
	 */
	public final static Map<String, String> getParameters(String queryPart){
		HashMap<String, String> parameters = new HashMap<String, String>();

		if (queryPart != null){
			queryPart = queryPart.substring(queryPart.indexOf("?") + 1).trim();
			if (!queryPart.isEmpty()){
				String[] keyValues = queryPart.split("&");
				for(String item : keyValues){
					String[] keyValue = item.split("=");
					if (keyValue.length == 2){
						keyValue[0] = keyValue[0].trim().toLowerCase();
						keyValue[1] = keyValue[1].trim();
						if (!keyValue[0].isEmpty() && !keyValue[1].isEmpty()){
							try{
								parameters.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
							}catch(UnsupportedEncodingException uee){
							}
						}
					}
				}
			}
		}

		return parameters;
	}

	/**
	 * <p>Extract only the GET parameters from the given HTTP request and add them inside the given map.</p>
	 *
	 * <p><b>Warning</b>:
	 * 	If entries with the same key already exist in the map, they will overwritten.
	 * </p>
	 *
	 * @param req			The HTTP request whose the GET parameters must be extracted.
	 * @param parameters	List of parameters to update.
	 *
	 * @return	The same given parameters map (but updated with all found GET parameters).
	 *
	 * @since 4.1
	 */
	public static final Map<String, Object> addGETParameters(final HttpServletRequest req, final Map<String, Object> parameters){
		String queryString = req.getQueryString();
		if (queryString != null){
			String[] params = queryString.split("&");
			int indSep;
			for(String p : params){
				indSep = p.indexOf('=');
				if (indSep >= 0){
					try{
						parameters.put(URLDecoder.decode(p.substring(0, indSep), "UTF-8"), URLDecoder.decode(p.substring(indSep + 1), "UTF-8"));
					}catch(UnsupportedEncodingException uee){
					}
				}
			}
		}
		return parameters;
	}

	/**
	 * Get the number of parameters submitted in the given HTTP request.
	 *
	 * @param request	An HTTP request;
	 *
	 * @return	The number of submitted parameters.
	 *
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static final int getNbParameters(final HttpServletRequest request){
		if (request == null)
			return 0;
		try{
			return ((Map<String, Object>)request.getAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS)).size();
		}catch(Exception ex){
			return request.getParameterMap().size();
		}
	}

	/**
	 * Check whether a parameter has been submitted with the given name.
	 *
	 * @param name				Name of the parameter to search. <b>The case is important!</b>
	 * @param request			HTTP request in which the specified parameter must be searched.
	 * @param caseSensitive		<i>true</i> to perform the research case-sensitively,
	 *                     		<i>false</i> for a case INsensitive research.
	 *
	 * @return	<i>true</i> if the specified parameter has been found, <i>false</i> otherwise.
	 *
	 * @since 4.1
	 */
	public static final boolean hasParameter(final String name, final HttpServletRequest request, final boolean caseSensitive){
		return getParameter(name, request, caseSensitive) != null;
	}

	/**
	 * Check whether the parameter specified with the given pair (name,value) exists in the given HTTP request.
	 *
	 * @param name				Name of the parameter to search.
	 * @param value				Expected value of the parameter.
	 * @param request			HTTP request in which the given pair must be searched.
	 * @param caseSensitive		<i>true</i> to perform the research (on name AND value) case-sensitively,
	 *                     		<i>false</i> for a case INsensitive research.
	 *
	 * @return	<i>true</i> if the specified parameter has been found with the given value in the given HTTP request,
	 *        	<i>false</i> otherwise.
	 *
	 * @since 4.1
	 */
	public static final boolean hasParameter(final String name, final String value, final HttpServletRequest request, final boolean caseSensitive){
		Object found = getParameter(name, request, caseSensitive);
		if (value == null)
			return found != null;
		else{
			if (found == null || !(found instanceof String))
				return false;
			else
				return (caseSensitive && ((String)found).equals(value)) || (!caseSensitive && ((String)found).equalsIgnoreCase(value));
		}
	}

	/**
	 * Get the parameter specified by the given name from the given HTTP request.
	 *
	 * @param name				Name of the parameter to search.
	 * @param request			HTTP request in which the given pair must be searched.
	 * @param caseSensitive		<i>true</i> to perform the research case-sensitively,
	 *                     		<i>false</i> for a case INsensitive research.
	 *
	 * @return	Value of the parameter.
	 *
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static final Object getParameter(final String name, final HttpServletRequest request, final boolean caseSensitive){
		try{
			// Get the extracted parameters:
			Map<String, Object> params = (Map<String, Object>)request.getAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS);

			// Search case IN-sensitively the given pair (name, value):
			for(Map.Entry<String, Object> e : params.entrySet()){
				if ((!caseSensitive && e.getKey().equalsIgnoreCase(name)) || (caseSensitive && e.getKey().equals(name)))
					return (e.getValue() != null) ? e.getValue() : null;
			}
		}catch(Exception ex){
		}
		return null;
	}

	/**
	 * <p>Delete all unused uploaded files of the given request.</p>
	 *
	 * <p>
	 * 	These files have been stored on the file system
	 * 	if there is a request attribute named {@link UWS#REQ_ATTRIBUTE_PARAMETERS}.
	 * </p>
	 *
	 * @param req	Request in which files have been uploaded.
	 *
	 * @return	The number of deleted files.
	 *
	 * @see UploadFile#isUsed()
	 *
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static final int deleteUploads(final HttpServletRequest req){
		int cnt = 0;
		Object attribute = req.getAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS);
		// If there is the request attribute "UWS_PARAMETERS":
		if (attribute != null && attribute instanceof Map){
			Map<String, Object> params = (Map<String, Object>)attribute;
			// For each parameter...
			for(Map.Entry<String, Object> e : params.entrySet()){
				// ...delete physically the uploaded file ONLY IF not used AND IF it is an uploaded file:
				if (e.getValue() != null && e.getValue() instanceof UploadFile && !((UploadFile)e.getValue()).isUsed()){
					try{
						((UploadFile)e.getValue()).deleteFile();
						cnt++;
					}catch(IOException ioe){
					}
				}
			}
		}
		return cnt;
	}

	/* *************** */
	/* USER EXTRACTION */
	/* *************** */
	/**
	 * <p>Extract the user/job owner from the given HTTP request.</p>
	 *
	 * Two cases are supported:
	 * <ol>
	 * 	<li><b>The user has already been identified and is stored in the HTTP attribute {@link UWS#REQ_ATTRIBUTE_USER}</b> => the stored value is returned.</li>
	 * 	<li><b>No HTTP attribute and a {@link UserIdentifier} is provided</b> => the user is identified with the given {@link UserIdentifier} and stored in the HTTP attribute {@link UWS#REQ_ATTRIBUTE_USER} before being returned.</li>
	 * </ol>
	 *
	 * <p>In any other case, NULL is returned.</p>
	 *
	 * @param request			The HTTP request from which the user must be extracted. <i>note: if NULL, NULL will be returned.</i>
	 * @param userIdentifier	The method to use in order to extract a user from the given request. <i>note: if NULL, NULL is returned IF no HTTP attribute {@link UWS#REQ_ATTRIBUTE_USER} can be found.</i>
	 *
	 * @return	The identified user. <i>MAY be NULL</i>
	 *
	 * @throws NullPointerException	If an error occurs while extracting a {@link UWSUrl} from the given {@link HttpServletRequest}.
	 * @throws UWSException			If any error occurs while extracting a user from the given {@link HttpServletRequest}.
	 *
	 * @since 4.1
	 */
	public static final JobOwner getUser(final HttpServletRequest request, final UserIdentifier userIdentifier) throws NullPointerException, UWSException{
		if (request == null)
			return null;
		else if (request.getAttribute(UWS.REQ_ATTRIBUTE_USER) != null)
			return (JobOwner)request.getAttribute(UWS.REQ_ATTRIBUTE_USER);
		else if (userIdentifier != null){
			JobOwner user = userIdentifier.extractUserId(new UWSUrl(request), request);
			request.setAttribute(UWS.REQ_ATTRIBUTE_USER, user);
			return user;
		}else
			return null;
	}

	/* **************************** */
	/* DIRECTORY MANAGEMENT METHODS */
	/* **************************** */
	/**
	 * Empties the specified directory.
	 *
	 * @param directoryPath	The path of the directory to empty.
	 */
	public static final void clearDirectory(String directoryPath){
		clearDirectory(new File(directoryPath));
	}

	/**
	 * <p>Empties the specified directory.</p>
	 *
	 * <p><i><u>Note:</u> The directory is NOT deleted. Just its content is destroyed.</i></p>
	 *
	 * @param directory	The directory which has to be emptied.
	 */
	public static final void clearDirectory(File directory){
		if (directory != null && directory.exists() && directory.isDirectory() && directory.canWrite()){
			File[] files = directory.listFiles();
			for(int i = 0; i < files.length; i++)
				files[i].delete();
		}
	}

	/* *************************** */
	/* RESPONSE MANAGEMENT METHODS */
	/* *************************** */

	/**
	 * <p>Flush the buffer of the given {@link PrintWriter}.</p>
	 *
	 * <p>
	 * 	This function aims to be used if the given {@link PrintWriter} has been provided by an {@link HttpServletResponse}.
	 * 	In such case, a call to its flush() function may generate a silent error which could only mean that
	 * 	the connection with the HTTP client has been closed.
	 * </p>
	 *
	 * @param writer	The writer to flush.
	 *
	 * @throws ClientAbortException		If the connection with the HTTP client is closed.
	 *
	 * @see PrintWriter#flush()
	 *
	 * @since 4.1
	 */
	public static final void flush(final PrintWriter writer) throws ClientAbortException{
		writer.flush();
		if (writer.checkError())
			throw new ClientAbortException();
	}

	/**
	 * Copies the content of the given input stream in the given HTTP response.
	 *
	 * @param input			Data to copy.
	 * @param mimeType		Type of data to copy (may be null).
	 * @param contentSize	Size of the file to write.
	 * @param response		Response in which the data must be copied.
	 *
	 * @throws IOException	If there is an error while opening the output stream or while copying.
	 */
	public static void write(final InputStream input, final String mimeType, final long contentSize, final HttpServletResponse response) throws IOException{
		ServletOutputStream output = null;
		try{
			// Set the HTTP content type:
			if (mimeType != null)
				response.setContentType(mimeType);

			// Set the character encoding:
			response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

			// Set the HTTP content length:
			if (contentSize >= 0)
				setContentLength(response, contentSize);

			// Write the file into the HTTP response:
			output = response.getOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while((length = input.read(buffer)) > 0)
				output.write(buffer, 0, length);
		}finally{
			if (output != null)
				output.flush();
		}
	}

	/**
	 * Writes the stack trace of the given exception in the file whose the name and the parent directory are given in parameters.
	 * If the specified file already exists, it will be overwritten if the parameter <i>overwrite</i> is equal to <i>true</i>, otherwise
	 * no file will not be changed <i>(default behavior of {@link UWSToolBox#writeErrorFile(Exception, ErrorSummary, UWSJob, OutputStream)})</i>.
	 *
	 * @param ex				The exception which has to be used to generate the error file.
	 * @param error				The error description.
	 * @param job				The job which ended with the given error.
	 * @param output			The stream in which the error description and trace must be written.
	 *
	 * @return					<i>true</i> if the file has been successfully created, <i>false</i> otherwise.
	 *
	 * @throws IOException		If there is an error while writing the description and the stack trace of the given error.
	 */
	public static final boolean writeErrorFile(final Exception ex, final ErrorSummary error, final UWSJob job, final OutputStream output) throws IOException{
		if (ex == null)
			return false;

		PrintWriter pw = new PrintWriter(output);
		pw.println("Date: " + (new Date()).toString());
		pw.println("Job: " + job.getJobId());
		pw.println("Type: " + error.getType());
		pw.println("Message: " + error.getMessage());
		pw.println("Stack Trace:");
		printStackTrace(ex, pw);
		pw.close();

		return true;
	}

	/**
	 * Prints the full stack trace of the given exception in the given writer.
	 *
	 * @param ex			The exception whose the stack trace must be printed.
	 * @param pw			The stream in which the stack trace must be written.
	 *
	 * @throws IOException	If there is an error while printing the stack trace.
	 */
	public static final void printStackTrace(final Throwable ex, final PrintWriter pw) throws IOException{
		pw.println(ex.getClass().getName() + ": " + ex.getMessage());

		StackTraceElement[] elements = ex.getStackTrace();
		for(int i = 0; i < elements.length; i++)
			pw.println("\tat " + elements[i].toString());

		if (ex.getCause() != null){
			pw.print("Caused by: ");
			pw.flush();
			printStackTrace(ex.getCause(), pw);
		}

		pw.flush();
	}

	/* *********************** */
	/* UWS_URL DISPLAY METHODS */
	/* *********************** */
	/**
	 * Displays all the fields of the given UWSUrl.
	 *
	 * @param url	The UWSUrl which has to be displayed.
	 *
	 * @see #printURL(UWSUrl, java.io.OutputStream)
	 */
	public static final void printURL(UWSUrl url){
		try{
			printURL(url, System.out);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	/**
	 * Displays all the fields of the given UWSUrl in the given output stream.
	 *
	 * @param url			The UWSUrl which has to be displayed.
	 * @param output		The stream in which the fields of the given UWSUrl has to be displayed.
	 *
	 * @throws IOException	If there is an error while writing in the given stream.
	 */
	public static final void printURL(UWSUrl url, java.io.OutputStream output) throws IOException{
		StringBuffer toPrint = new StringBuffer();
		toPrint.append("***** UWS_URL (").append(url.getBaseURI()).append(") *****");
		toPrint.append("\nRequest URL: ").append(url.getRequestURL());
		toPrint.append("\nRequest URI: ").append(url.getRequestURI());
		toPrint.append("\nUWS URI: ").append(url.getUwsURI());
		toPrint.append("\nJob List: ").append(url.getJobListName());
		toPrint.append("\nJob ID: ").append(url.getJobId());
		toPrint.append("\nAttributes (").append(url.getAttributes().length).append("):");

		for(String att : url.getAttributes())
			toPrint.append(" ").append(att);

		toPrint.append("\n");

		output.write(toPrint.toString().getBytes());
	}

	/* ********* */
	/* MIME TYPE */
	/* ********* */
	/** List of file extensions whose the MIME type is known (see {@link #mimeTypes}). */
	protected static final String[] fileExts = new String[]{ "", "vot", "json", "json", "csv", "tsv", "txt", "xml", "xml", "pdf", "ai", "eps", "ps", "html", "zip", "gzip", "gz", "tar", "gif", "jpeg", "jpg", "png", "bmp" };

	/** List of known MIME types (see {@link #fileExts}). */
	protected static final String[] mimeTypes = new String[]{ "application/octet-stream", "application/x-votable+xml", "application/json", "text/json", "text/csv", "text/tab-separated-values", "text/plain", "application/xml", "text/xml", "application/pdf", "application/postscript", "application/postscript", "application/postscript", "text/html", "application/zip", "application/x-gzip", "application/x-gzip", "application/x-tar", "image/gif", "image/jpeg", "image/jpeg", "image/png", "image/x-portable-bitmap" };

	/**
	 * Gets the MIME type corresponding to the given file extension.
	 *
	 * @param fileExtension	File extension (i.e. .txt, json, .xml, xml, ....)
	 *
	 * @return				The corresponding MIME type or <i>null</i> if not known.
	 */
	public static final String getMimeType(String fileExtension){
		if (fileExtension == null)
			return null;

		fileExtension = fileExtension.trim();
		if (fileExtension.length() > 0 && fileExtension.charAt(0) == '.')
			fileExtension = fileExtension.substring(1).trim();

		for(int i = 0; i < fileExts.length; i++)
			if (fileExtension.equalsIgnoreCase(fileExts[i]))
				return mimeTypes[i];

		return null;
	}

	/**
	 * Gets the file extension corresponding to the given MIME type.
	 *
	 * @param mimeType		A MIME type (i.e. text/plain, application/json, application/xml, text/xml, application/x-votable+xml, ....)
	 *
	 * @return				The corresponding file extension or <i>null</i> if not known.
	 */
	public static final String getFileExtension(String mimeType){
		if (mimeType == null)
			return null;

		mimeType = mimeType.trim();

		for(int i = 0; i < mimeTypes.length; i++)
			if (mimeType.equalsIgnoreCase(mimeTypes[i]))
				return fileExts[i];

		return null;
	}

	/**
	 * Set the content length in the given {@link HttpServletResponse}.
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	This could perfectly be done using
	 * 	{@link HttpServletResponse#setContentLength(int)}, <b>but only if the
	 * 	content size is encoded or fit in an integer value</b>. Otherwise, that
	 * 	function will set no content length.
	 * 	On the contrary, this current function takes a long value and set
	 * 	manually the content type header.
	 * </i></p>
	 *
	 * <p><i><b>Note:</b>
	 * 	This function has no effect if the given {@link HttpServletResponse} is
	 * 	NULL or if the given content size is &le; 0.
	 * </i></p>
	 *
	 * @param response		HTTP response.
	 * @param contentSize	The content size to set.
	 *
	 * @since 4.4
	 */
	public static final void setContentLength(final HttpServletResponse response, final long contentSize){
		if (response != null && contentSize > 0)
			response.setHeader("Content-Length", String.valueOf(contentSize));
	}
}
