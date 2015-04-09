package tap.resource;

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
 * Copyright 2015 - Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPException;
import tap.formatter.OutputFormat;
import uws.ClientAbortException;
import uws.UWSToolBox;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Write the content of the TAP service's home page.</p>
 * 
 * <p><i>Note:
 * 	This class is using the two following {@link TAP} attributes in order to display the home page:
 * 	{@link TAP#homePageURI} and {@link TAP#homePageMimeType}. The MIME type is used only for the third case below (local file). 
 * </i></p>
 * 
 * <p>Four cases are taken into account in this class, in function of the {@link TAP#homePageURI} value:</p>
 * <ol>
 * 	<li><b>a default content</b> if no custom home page (URI) has been specified using {@link TAP#setHomePageURI(String)}.
 * 	                             This default home page is hard-coded in this class and displays just an HTML list of
 * 	                             links. There is one link for each resources of this TAP service (excluding the home page).</li>
 * 	<li><b>a file inside WebContent</b> if the given URI has no scheme (e.g. "tapIndex.jsp" or "/myFiles/tapIndex.html").
 * 	                                    The URI is then an absolute (if starting with "/") or a relative path to file inside the WebContent directory.
 * 	                                    In this case the request is forwarded to this file. It is neither a redirection nor a copy,
 * 	                                    but a kind of inclusion of the interpreted file into the response.
 *                                      <i>This method MUST be used if your home page is a JSP.</i></li>
 * <li><b>a local file</b> if a URI starts with "file:". In this case, the content of the local file is copied in the HTTP response. There is no interpretation. So this method should not be used for JSP.</li>	
 * <li><b>a distance document</b> in all other cases. Indeed, if there is a scheme different from "file:" the given URI will be considered as a URL.
 *                                In this case, any request to the TAP home page is redirected to this URL.</li>
 * </ol>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (04/2015)
 * @since 2.0
 */
public class HomePage implements TAPResource {

	/** Name of this TAP resource. */
	public static final String RESOURCE_NAME = "HOME PAGE";

	/** TAP service owning this resource. */
	protected final TAP tap;

	public HomePage(final TAP tap){
		if (tap == null)
			throw new NullPointerException("Missing TAP object! The HOME PAGE resource can not be initialized without a TAP instance.");
		this.tap = tap;
	}

	@Override
	public void init(final ServletConfig config) throws ServletException{}

	@Override
	public void destroy(){}

	@Override
	public void setTAPBaseURL(String baseURL){}

	@Override
	public final String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public boolean executeResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException, TAPException{
		boolean written = false;

		// Display the specified home page, if any is specified:
		if (tap.homePageURI != null){

			URI uri = null;
			try{
				uri = new URI(tap.homePageURI);
				/* CASE: FILE IN WebContent */
				if (uri.getScheme() == null){
					try{
						if (request.getServletContext().getResource(tap.homePageURI) != null){
							request.getRequestDispatcher(tap.homePageURI).forward(request, response);
							written = true;
						}else
							logError("Web application file not found", null);
					}catch(MalformedURLException mue){
						logError("incorrect URL syntax", mue);
					}
				}
				/* CASE: LOCAL FILE */
				else if (uri.getScheme().equalsIgnoreCase("file")){
					// Set the content type:
					response.setContentType(tap.homePageMimeType);

					// Set the character encoding:
					response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

					// Get the character writer:
					PrintWriter writer = response.getWriter();

					// Get an input toward the custom home page:
					BufferedReader input = null;
					try{
						File f = new File(uri.getPath());
						if (f.exists() && !f.isDirectory() && f.canRead()){
							// set the content length:
							response.setContentLength((int)f.length());

							// get the input stream:
							input = new BufferedReader(new FileReader(f));

							// Copy the content of the input into the given writer:
							char[] buffer = new char[2048];
							int nbReads = 0, nbBufferWritten = 0;
							while((nbReads = input.read(buffer)) > 0){
								writer.write(buffer, 0, nbReads);
								if ((++nbBufferWritten) % 4 == 0){ // the minimum and default buffer size of an HttpServletResponse is 8kiB => 4*2048
									UWSToolBox.flush(writer);
									nbBufferWritten = 0;
								}
							}
							UWSToolBox.flush(writer);

							// copy successful:
							written = true;
						}else
							logError("file not found or not readable (" + f.exists() + !f.isDirectory() + f.canRead() + ")", null);

					}catch(ClientAbortException cae){
						/*   This exception is an extension of IOException thrown only by some functions of UWSToolBox.
						 * It aims to notify about an IO error while trying to write the content of an HttpServletResponse.
						 * Such exception just means that the connection with the HTTP client has been closed/aborted.
						 * Consequently, no error nor result can be written any more in the HTTP response.
						 *   This error, is just propagated to the TAP instance, so that stopping any current process
						 * for this request and so that being logged without any attempt of writing the error in the HTTP response.
						 */
						throw cae;

					}catch(IOException ioe){
						/*   This IOException can be thrown only by InputStream.read(...) (because PrintWriter.print(...)
						 * silently fallbacks in case of error).
						 *   So this error must not be propagated but caught and logged right now. Thus the default home page
						 * can be displayed after the error has been logged. */
						logError("the following error occurred while reading the specified local file", ioe);

					}finally{
						if (input != null)
							input.close();
					}

					// Stop trying to write the home page if the HTTP request has been aborted/closed:
					/*if (requestAborted)
						throw new IOException("HTTP request aborted or connection with the HTTP client closed for another reason!");*/
				}
				/* CASE: HTTP/HTTPS/FTP/... */
				else{
					response.sendRedirect(tap.homePageURI);
					written = true;
				}

			}catch(IOException ioe){
				/*   This IOException can be caught here only if caused by a HTTP client abortion or by a closing of the HTTPrequest.
				 * So, it must be propagated until the TAP instance, where it will be merely logged as INFO. No response/error can be 
				 * returned in the HTTP response. */
				throw ioe;

			}catch(IllegalStateException ise){
				/*   This exception is caused by an attempt to reset the HTTP response buffer while a part of its
				 * content has already been submitted to the HTTP client.
				 *   It must be propagated to the TAP instance so that being logged as a FATAL error. */
				throw ise;

			}catch(Exception e){
				/*   The other errors are just logged, but not reported to the HTTP client,
				 * and then the default home page is displayed. */
				if (e instanceof URISyntaxException)
					logError("the given URI has a wrong and unexpected syntax", e);
				else
					logError(null, e);
			}
		}

		// DEFAULT: list all available resources:
		if (!written){
			// Set the content type: HTML document
			response.setContentType("text/html");

			// Set the character encoding:
			response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

			// Get the output stream:
			PrintWriter writer = response.getWriter();

			// HTML header + CSS + Javascript:
			writer.print("<!DOCTYPE html>\n<html>\n\t<head>\n\t\t<meta charset=\"UTF-8\">\n\t\t<title>TAP HOME PAGE</title>\n\t\t<style type=\"text/css\">\n\t\t\th1 { text-align: center; }\n\t\t\t.subtitle { font-size: .8em; }\n\t\t\th2 { border-bottom: 1px solid black; }\n\t\t\t.formField textarea { padding: .5em; display: block; margin: 0; }\n\t\t\t.formField { margin-bottom: .5em; }\n\t\t\t#submit { font-weight: bold; font-size: 1.1em; padding: .4em; color: green; }\n\t\t\t#footer { font-style: .8em; font-style: italic; }\n\t\t</style>\n\t\t<script type=\"text/javascript\">\n\t\t\tfunction toggleTextInput(id){\n\t\t\t\tdocument.getElementById(id).disabled = !document.getElementById(id).disabled;\n\t\t\t\tif (document.getElementById(id).disabled){\n\t\t\t\t\tdocument.getElementById(id).value = \"-1\";\n\t\t\t\t\tdocument.getElementById(id).removeAttribute('name');\n\t\t\t\t}else\n\t\t\t\t\tdocument.getElementById(id).name = id;\n\t\t\t}\n\t\t\tfunction toggleUploadFeature(){\n\t\t\t\tdocument.getElementById('uplTable').disabled = !document.getElementById('uplTable').disabled;\n\t\t\t\tif (document.getElementById('uplTable').disabled){\n\t\t\t\t\tdocument.getElementById('uplTable').removeAttribute('name');\n\t\t\t\t\tdocument.getElementById('queryForm').enctype = \"application/x-www-form-urlencoded\";\n\t\t\t\t\tdocument.getElementById('queryForm').removeChild(document.getElementById('uploadParam'));\n\t\t\t\t}else{\n\t\t\t\t\tdocument.getElementById('queryForm').enctype = \"multipart/form-data\";\n\t\t\t\t\tdocument.getElementById('uplTable').name = 'uplTable';\n\t\t\t\t\tvar newInput = document.createElement('input');\n\t\t\t\t\tnewInput.id = \"uploadParam\";\n\t\t\t\t\tnewInput.type = \"hidden\";\n\t\t\t\t\tnewInput.name = \"UPLOAD\";\n\t\t\t\t\tnewInput.value = \"upload,param:uplTable\";\n\t\t\t\t\tdocument.getElementById('queryForm').appendChild(newInput);\n\t\t\t\t}\n\t\t\t}\n\t\t</script>\n\t</head>\n\t<body>");

			// Page title:
			writer.print("\n\t\t<h1 class=\"centered\">TAP HOME PAGE<br/>");
			if (tap.getServiceConnection().getProviderName() != null)
				writer.print("<span class=\"subtitle\">- " + tap.getServiceConnection().getProviderName() + " -</span>");
			writer.print("</h1>");

			// Service description:
			if (tap.getServiceConnection().getProviderDescription() != null)
				writer.print("\n\n\t\t<h2>Service description</h2>\n\t\t<p>" + tap.getServiceConnection().getProviderDescription() + "</p>");

			// List of all available resources:
			writer.print("\n\n\t\t<h2>Available resources</h2>\n\t\t<ul>");
			for(TAPResource res : tap.resources.values())
				writer.println("\n\t\t\t<li><a href=\"" + tap.tapBaseURL + "/" + res.getName() + "\">" + res.getName() + "</a></li>");
			writer.print("\n\t\t</ul>");

			// ADQL query form:
			writer.print("\n\t\t\n\t\t<h2>ADQL query</h2>\n\t\t<noscript>\n\t\t\t<p><strong><u>WARNING</u>! Javascript is disabled in your browser. Consequently, you can submit queries ONLY in synchronous mode and no limit on result nor execution duration can be specified. Besides, no table can be uploaded.</strong></p>\n\t\t</noscript>");
			writer.print("\n\t\t<form id=\"queryForm\" action=\"" + tap.getAccessURL() + "/sync\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" target=\"_blank\">\n\t\t\t<input type=\"hidden\" name=\"REQUEST\" value=\"doQuery\" />\n\t\t\t<input type=\"hidden\" name=\"VERSION\" value=\"1.0\" />\n\t\t\t<input type=\"hidden\" name=\"LANG\" value=\"ADQL\" />\n\t\t\t<input type=\"hidden\" name=\"PHASE\" value=\"RUN\" />\n\t\t\t<div class=\"formField\">\n\t\t\t\t<strong>Query:</strong>\n\t\t\t\t<textarea name=\"QUERY\" cols=\"80\" rows=\"5\">\nSELECT *\nFROM TAP_SCHEMA.tables;</textarea>\n\t\t\t</div>");
			writer.print("\n\n\t\t\t<div class=\"formField\">\n\t\t\t\t<strong>Execution mode:</strong> <input id=\"asyncOption\" name=\"REQUEST\" value=\"doQuery\" type=\"radio\" onclick=\"document.getElementById('queryForm').action='" + tap.getAccessURL() + "/async';\" /><label for=\"asyncOption\"> Asynchronous/Batch</label>\n\t\t\t\t<input id=\"syncOption\" name=\"REQUEST\" value=\"doQuery\" type=\"radio\" onclick=\"document.getElementById('queryForm').action='" + tap.getAccessURL() + "/sync';\" checked=\"checked\" /><label for=\"syncOption\"> Synchronous</label>\n\t\t\t</div>");
			writer.print("\n\t\t\t<div class=\"formField\"><strong>Format:</strong>\n\t\t\t\t<select name=\"FORMAT\">");
			Iterator<OutputFormat> itFormats = tap.getServiceConnection().getOutputFormats();
			OutputFormat fmt;
			while(itFormats.hasNext()){
				fmt = itFormats.next();
				writer.println("\n\t\t\t<option value=\"" + fmt.getMimeType() + "\"" + (fmt.getShortMimeType() != null && fmt.getShortMimeType().equalsIgnoreCase("votable") ? " selected=\"selected\"" : "") + ">" + fmt.getShortMimeType() + "</option>");
			}
			writer.print("\n\t\t\t\t</select>\n\t\t\t</div>");

			// Result limit:
			writer.print("\n\t\t\t<div class=\"formField\">\n\t\t\t\t<input id=\"toggleMaxRec\" type=\"checkbox\" onclick=\"toggleTextInput('MAXREC');\" /><label for=\"toggleMaxRec\"><strong>Result limit:</strong></label> <input id=\"MAXREC\" type=\"text\" value=\"-1\" list=\"maxrecList\" disabled=\"disabled\" /> rows <em>(0 to get only metadata ; a value &lt; 0 means 'default value')</em>\n\t\t\t\t<datalist id=\"maxrecList\">");
			if (tap.getServiceConnection().getOutputLimit() != null && tap.getServiceConnection().getOutputLimit().length >= 2){
				writer.print("\n\t\t\t\t\t<option value=\"" + tap.getServiceConnection().getOutputLimit()[0] + "\">Default</option>");
				writer.print("\n\t\t\t\t\t<option value=\"" + tap.getServiceConnection().getOutputLimit()[1] + "\">Maximum</option>");
			}
			writer.print("\n\t\t\t\t</datalist>\n\t\t\t</div>");

			// Execution duration limit:
			writer.print("\n\t\t\t<div class=\"formField\">\n\t\t\t\t<input id=\"toggleDuration\" type=\"checkbox\" onclick=\"toggleTextInput('EXECUTIONDURATION');\" /><label for=\"toggleDuration\"><strong>Duration limit:</strong></label> <input id=\"EXECUTIONDURATION\" type=\"text\" value=\"-1\" list=\"durationList\" disabled=\"disabled\" /> seconds <em>(a value &le; 0 means 'default value')</em>\n\t\t\t\t<datalist id=\"durationList\">");
			if (tap.getServiceConnection().getExecutionDuration() != null && tap.getServiceConnection().getExecutionDuration().length >= 2){
				writer.print("\n\t\t\t\t\t<option value=\"" + tap.getServiceConnection().getExecutionDuration()[0] + "\">Default</option>");
				writer.print("\n\t\t\t\t\t<option value=\"" + tap.getServiceConnection().getExecutionDuration()[1] + "\">Maximum</option>");
			}
			writer.print("\n\t\t\t\t</datalist>\n\t\t\t</div>");

			// Upload feature:
			if (tap.getServiceConnection().uploadEnabled())
				writer.print("\n\t\t\t<div class=\"formField\">\n\t\t\t\t<input id=\"toggleUpload\" type=\"checkbox\" onclick=\"toggleUploadFeature();\" /><label for=\"toggleUpload\"><strong>Upload a VOTable:</strong></label> <input id=\"uplTable\" type=\"file\" disabled=\"disabled\" /> <em>(the uploaded table must be referenced in the ADQL query with the following full name: TAP_UPLOAD.upload)</em>\n\t\t\t</div>");

			// Footer:
			writer.print("\n\t\t\t<input id=\"submit\" type=\"submit\" value=\"Execute!\" />\n\t\t</form>\n\t\t<br/>\n\t\t<hr/>\n\t\t<div id=\"footer\">\n\t\t\t<p>Page generated by <a href=\"http://cdsportal.u-strasbg.fr/taptuto/\" target=\"_blank\">TAPLibrary</a> v2.0</p>\n\t\t</div>\n\t</body>\n</html>");

			writer.flush();

			written = true;
		}

		return written;
	}

	/**
	 * <p>Log the given error as a TAP log message with the {@link LogLevel} ERROR, and the event "HOME_PAGE".</p>
	 * 
	 * <p>
	 * 	The logged message starts with: <code>Can not write the specified home page content ({tap.homePageURI})</code>.
	 * 	After the specified error message, the following is appended: <code>! => The default home page will be displayed.</code>.
	 * </p>
	 * 
	 * <p>
	 * 	If the message parameter is missing, the {@link Throwable} message will be taken instead.
	 * 	And if this latter is also missing, none will be written.
	 * </p>
	 *
	 * @param message	Error message to log.
	 * @param error		The exception at the origin of the error.
	 */
	protected void logError(final String message, final Throwable error){
		tap.getLogger().logTAP(LogLevel.ERROR, null, "HOME_PAGE", "Can not write the specified home page content (" + tap.homePageURI + ") " + (message == null ? (error == null ? "" : ": " + error.getMessage()) : ": " + message) + "! => The default home page will be displayed.", error);
	}

}
