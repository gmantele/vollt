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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPException;
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
 * @version 2.0 (02/2015)
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
		PrintWriter writer = response.getWriter();
		boolean written = false;

		// Display the specified home page, if any is specified:
		if (tap.homePageURI != null){

			URI uri = null;
			try{
				uri = new URI(tap.homePageURI);
				/* CASE: FILE IN WebContent */
				if (uri.getScheme() == null){
					if (request.getServletContext().getResource(tap.homePageURI) != null){
						request.getRequestDispatcher(tap.homePageURI).forward(request, response);
						written = true;
					}else
						tap.getLogger().logTAP(LogLevel.ERROR, null, "HOME_PAGE", "Can not write the specified home page content (" + tap.homePageURI + "): Web application file not found!", null);
				}
				/* CASE: LOCAL FILE */
				else if (uri.getScheme().equalsIgnoreCase("file")){
					// Set the content type:
					response.setContentType(tap.homePageMimeType);

					// Get an input toward the custom home page:
					BufferedInputStream input = null;
					try{
						File f = new File(uri.getPath());
						if (f.exists() && !f.isDirectory() && f.canRead()){
							// set the content length:
							response.setContentLength((int)f.length());

							// get the input stream:
							input = new BufferedInputStream(new FileInputStream(f));

							// Copy the content of the input into the given writer:
							byte[] buffer = new byte[2048];
							int nbReads = 0;
							while((nbReads = input.read(buffer)) > 0)
								writer.print(new String(buffer, 0, nbReads));

							// copy successful:
							written = true;
						}else
							tap.getLogger().logTAP(LogLevel.ERROR, null, "HOME_PAGE", "Can not write the specified home page content (" + tap.homePageURI + "): File not found or not readable (" + f.exists() + !f.isDirectory() + f.canRead() + ")!", null);

					}finally{
						if (input != null)
							input.close();
					}
				}
				/* CASE: HTTP/HTTPS/FTP/... */
				else{
					response.sendRedirect(tap.homePageURI);
					written = true;
				}

			}catch(Exception e){
				tap.getLogger().logTAP(LogLevel.ERROR, null, "HOME_PAGE", "Can not write the specified home page content (" + tap.homePageURI + "): " + e.getMessage(), e);
			}
		}

		// DEFAULT: list all available resources:
		if (!written){
			// Set the content type: HTML document
			response.setContentType("text/html");

			// Write the home page:
			writer.println("<html><head><title>TAP HOME PAGE</title></head><body><h1 style=\"text-align: center\">TAP HOME PAGE</h1><h2>Available resources:</h2><ul>");
			for(TAPResource res : tap.resources.values())
				writer.println("<li><a href=\"" + tap.tapBaseURL + "/" + res.getName() + "\">" + res.getName() + "</a></li>");
			writer.println("</ul></body></html>");

			written = true;
		}

		return written;
	}

}
