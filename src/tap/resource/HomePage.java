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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPException;
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

					// set character encoding:
					response.setCharacterEncoding("UTF-8");

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
			// Get the output stream:
			PrintWriter writer = response.getWriter();

			// Set the content type: HTML document
			response.setContentType("text/html");

			// Write the home page:
			writer.println("<html><head><title>TAP HOME PAGE</title></head><body><h1 style=\"text-align: center\">TAP HOME PAGE</h1><h2>Available resources:</h2><ul>");
			for(TAPResource res : tap.resources.values())
				writer.println("<li><a href=\"" + tap.tapBaseURL + "/" + res.getName() + "\">" + res.getName() + "</a></li>");
			writer.println("</ul></body></html>");

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
