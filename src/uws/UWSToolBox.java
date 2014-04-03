package uws;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.job.ErrorSummary;
import uws.job.UWSJob;

import uws.service.UWSUrl;

import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog;

/**
 * Some useful functions for the managing of a UWS service.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public class UWSToolBox {

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
	 * <p><i>NOTE:
	 * 		it converts the Map&lt;String, <b>String[]</b>&gt; returned by {@link HttpServletRequest#getParameterMap()}
	 * 		into a Map&lt;String, <b>String</b>&gt; (the key is put in lower case).
	 * </i></p>
	 * 
	 * @param req	The HTTP request which contains the parameters to extract.
	 * 
	 * @return		The corresponding map of string.
	 */
	@SuppressWarnings("unchecked")
	public static final HashMap<String,String> getParamsMap(HttpServletRequest req){
		HashMap<String,String> params = new HashMap<String,String>(req.getParameterMap().size());

		Enumeration<String> e = req.getParameterNames();
		while(e.hasMoreElements()){
			String name = e.nextElement();
			params.put(name.toLowerCase(), req.getParameter(name));
		}

		return params;
	}

	/**
	 * Converts map of UWS parameters into a string corresponding to the query part of a HTTP-GET URL (i.e. ?EXECUTIONDURATION=60&DESTRUCTION=2010-09-01T13:58:00:000-0200).
	 * 
	 * @param parameters	A Map of parameters.
	 * @return				The corresponding query part of an HTTP-GET URL (all keys have been set in upper case).
	 */
	public final static String getQueryPart(Map<String,String> parameters){
		if (parameters == null || parameters.isEmpty())
			return "";

		StringBuffer queryPart = new StringBuffer();
		for(Map.Entry<String,String> e : parameters.entrySet()){
			String key = e.getKey();
			String val = e.getValue();

			if (key != null)
				key = key.trim().toUpperCase();
			if (val != null)
				val = val.trim();

			if (key != null && !key.isEmpty() && val != null && !val.isEmpty()){
				queryPart.append(e.getKey() + "=" + val);
				queryPart.append("&");
			}
		}

		return queryPart.substring(0, queryPart.length() - 1);
	}

	/**
	 * Converts the given query part of a HTTP-GET URL to a map of parameters.
	 * 
	 * @param queryPart		A query part of a HTTP-GET URL.
	 * @return				The corresponding map of parameters (all keys have been set in lower case).
	 */
	public final static Map<String,String> getParameters(String queryPart){
		HashMap<String,String> parameters = new HashMap<String,String>();

		if (queryPart != null){
			queryPart = queryPart.substring(queryPart.indexOf("?") + 1).trim();
			if (!queryPart.isEmpty()){
				String[] keyValues = queryPart.split("&");
				for(String item : keyValues){
					String[] keyValue = item.split("=");
					if (keyValue.length == 2){
						keyValue[0] = keyValue[0].trim().toLowerCase();
						keyValue[1] = keyValue[1].trim();
						if (!keyValue[0].isEmpty() && !keyValue[1].isEmpty())
							parameters.put(keyValue[0].trim(), keyValue[1].trim());
					}
				}
			}
		}

		return parameters;
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
	 * Copies the content of the given input stream in the given HTTP response.
	 * 
	 * @param input			Data to copy.
	 * @param mimeType		Type of data to copy (may be null).
	 * @param contentSize	Size of the file to write.
	 * @param response		Response in which the data must be copied.
	 * 
	 * @throws IOException	If there is an error while opening the output stream or while copying.
	 */
	public static final void write(final InputStream input, final String mimeType, final long contentSize, final HttpServletResponse response) throws IOException{
		ServletOutputStream output = null;
		try{
			// Set the HTTP content type:
			if (mimeType != null)
				response.setContentType(mimeType);

			// Set the HTTP content length:
			if (contentSize > 0)
				response.setContentLength((int)contentSize);

			// Write the file into the HTTP response:
			output = response.getOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while((length = input.read(buffer)) > 0)
				output.print(new String(buffer, 0, length));
		}finally{
			if (output != null)
				output.flush();
		}
	}

	/**
	 * Writes the stack trace of the given exception in the file whose the name and the parent directory are given in parameters.
	 * If the specified file already exists, it will be overwritten if the parameter <i>overwrite</i> is equal to <i>true</i>, otherwise
	 * no file will not be changed <i>(default behavior of {@link UWSToolBox#writeErrorFile(Exception, String, String)})</i>.
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
	protected static final String[] fileExts = new String[]{"","vot","json","json","csv","tsv","txt","xml","xml","pdf","ai","eps","ps","html","zip","gzip","gz","tar","gif","jpeg","jpg","png","bmp"};

	/** List of known MIME types (see {@link #fileExts}). */
	protected static final String[] mimeTypes = new String[]{"application/octet-stream","application/x-votable+xml","application/json","text/json","text/csv","text/tab-separated-values","text/plain","application/xml","text/xml","application/pdf","application/postscript","application/postscript","application/postscript","text/html","application/zip","application/x-gzip","application/x-tar","image/gif","image/jpeg","image/jpeg","image/png","image/x-portable-bitmap"};

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
	 * @param MIME type		A MIME type (i.e. text/plain, application/json, application/xml, text/xml, application/x-votable+xml, ....)
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
}
