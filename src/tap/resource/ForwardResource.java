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
 * Copyright 2015-2017 - Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.log.TAPLog;
import uws.ClientAbortException;
import uws.UWSToolBox;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>A {@link TAPResource} which is able to "forward" an HTTP request toward a specified URI.</p>
 * 
 * <p>
 * 	In function of the URI shape (i.e. what is the scheme? none/file:/other) and the servlet path,
 * 	the HTTP request will be internally forwarded to the Web Application file (using
 * 	{@link RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}),
 * 	the content of the specified file will be copied in the HTTP response or a redirection toward
 * 	the given URL will be performed.
 * </p>
 * 
 * <p><i>See {@link #forward(String, String, HttpServletRequest, HttpServletResponse)} for more details</i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.1 (03/2017)
 * @since 2.1
 */
public abstract class ForwardResource implements TAPResource {

	/** Logger that {@link #forward(String, String, HttpServletRequest, HttpServletResponse)} must use
	 * in case of not grave error (e.g. the specified Web Application file can not be found). */
	protected final TAPLog logger;

	/**
	 * Builds a {@link ForwardResource} with a logger to use in case of "small" errors.
	 * 
	 * @param logger	A TAP logger.
	 */
	protected ForwardResource(final TAPLog logger){
		this.logger = logger;
	}

	/**
	 * <p>Write the content of the specified file in the given HTTP response.</p>
	 * 
	 * <p>Three cases are taken into account in this function, in function of the given URI:</p>
	 * <ol>
	 * 	<li><b>a file inside WebContent</b> if the given URI has no scheme (e.g. "tapIndex.jsp" or "/myFiles/tapIndex.html").
	 * 	                                    The URI is then an absolute (if starting with "/") or a relative path to file inside the WebContent directory.
	 * 	                                    In this case the request is forwarded to this file. It is neither a redirection nor a copy,
	 * 	                                    but a kind of inclusion of the interpreted file into the response.
	 *                                      <i>This method MUST be used if your page/content is a JSP.</i></li>
	 * <li><b>a local file</b> if a URI starts with "file:". In this case, the content of the local file is copied in the HTTP response. There is no interpretation. So this method should not be used for JSP.</li>
	 * <li><b>a distance document</b> in all other cases. Indeed, if there is a scheme different from "file:" the given URI will be considered as a URL.
	 *                                In this case, any request to the TAP home page is redirected to this URL.</li>
	 * </ol>
	 * 
	 * <p><b>Important note:</b>
	 * 	The 1st option is applied ONLY IF the path of the TAP servlet is NOT the root path of the web application:
	 * 	that's to say <code>/*</code>. In the case where a URI without scheme is provided though the servlet path
	 * 	is <code>/*</code>, this function will resolve the full path on the local file system and apply the
	 * 	2nd option: write the file content directly in the response. Note that will work only in cases where the
	 * 	specified file is not a JSP or does not need any kind of interpretation by the function
	 * 	{@link RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}.
	 * </p>
	 * 
	 * @param file		URI/URL/path of the file to write/forward/redirect in the given HTTP response.
	 * @param mimeType	MIME type of the specified file.
	 * @param request	HTTP request which require the specified file.
	 * @param response	HTTP response in which the specified file must be written/forwarded/redirected.
	 * 
	 * @return	<code>true</code> if the forward/redirection was successful, <code>false</code> otherwise.
	 * 
	 * @throws IOException				When an error occur while forwarding toward the specified Web application resource,
	 *                    				or while writing the specified local file
	 *                    				or while redirection toward the specified URL
	 *                    				or when the HTTP connection has been aborted.
	 * @throws IllegalStateException	If an attempt of resetting the buffer fails.
	 */
	public final boolean forward(final String file, final String mimeType, final HttpServletRequest request, final HttpServletResponse response) throws IOException{
		boolean written = false;

		// Display the specified file, if any is specified:
		if (file != null){

			URI uri = null;
			try{
				uri = new URI(file.replaceAll(" ", "%20"));
				/* Note: the space replacement is just a convenient way to fix badly encoding URIs.
				 *       A proper way would be to encode all such incorrect URI characters (e.g. accents), but
				 *       the idea here is to focus on the most common mistake while writing manually 'file:' URIs. */

				/* If the servlet is set on the root Web Application path, a forward toward a WebContent resource won't work.
				 * The file then need to be copied "manually" in the HTTPServletResponse. For that, the trick consists to rewrite
				 * the given file path to a URI with the scheme "file://". */
				if (request.getServletPath().length() == 0 && uri.getScheme() == null)
					uri = new URI("file", null, request.getServletContext().getRealPath(file), null);

				/* CASE: FILE IN WebContent */
				if (uri.getScheme() == null){
					try{
						if (request.getServletContext().getResource(file) != null){
							request.getRequestDispatcher(file).forward(request, response);
							written = true;
						}else
							logError("Web application file not found", null, file);
					}catch(MalformedURLException mue){
						logError("incorrect URL syntax", mue, file);
					}
				}
				/* CASE: LOCAL FILE */
				else if (uri.getScheme().equalsIgnoreCase("file")){
					// Set the content type:
					response.setContentType(mimeType);

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
							logError("file not found or not readable (exists? " + f.exists() + ", file? " + !f.isDirectory() + ", readable? " + f.canRead() + ")", null, file);

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
						 *   So this error must not be propagated but caught and logged right now. Thus a default content
						 * can be displayed after the error has been logged. */
						logError("the following error occurred while reading the specified local file", ioe, file);

					}finally{
						if (input != null)
							input.close();
					}
				}
				/* CASE: HTTP/HTTPS/FTP/... */
				else{
					response.sendRedirect(file);
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
					logError("the given URI has a wrong and unexpected syntax", e, file);
				else
					logError(null, e, file);
			}
		}

		return written;
	}

	/**
	 * <p>Log the given error as a TAP log message with the {@link LogLevel} ERROR, and the event corresponding to the resource name.</p>
	 * 
	 * <p>
	 * 	The logged message starts with: <code>Can not write the specified content ({file})</code>.
	 * 	After the specified error message, the following is appended: <code>! => A default content may be displayed.</code>.
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
	protected void logError(final String message, final Throwable error, final String file){
		if (logger != null)
			logger.logTAP(LogLevel.ERROR, null, getName(), "Can not write the specified content (" + file + ") " + (message == null ? (error == null ? "" : ": " + error.getMessage()) : ": " + message) + "! => A default content may be displayed.", error);
	}

}
