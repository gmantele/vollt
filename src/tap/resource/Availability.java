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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import tap.TAPException;
import uk.ac.starlink.votable.VOSerializer;

/**
 * <p>TAP resource describing the availability of a TAP service.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public class Availability implements TAPResource, VOSIResource {

	/** Name of this TAP resource. */
	public static final String RESOURCE_NAME = "availability";

	/** Description of the TAP service owning this resource. */
	protected final ServiceConnection service;

	/** <p>URL toward this TAP resource.
	 * This URL is particularly important for its declaration in the capabilities of the TAP service.</p>
	 * 
	 * <p><i>Note: By default, it is just the name of this resource. It is updated after initialization of the service
	 * when the TAP service base URL is communicated to its resources. Then, it is: baseTAPURL + "/" + RESOURCE_NAME.</i></p> */
	protected String accessURL = getName();

	/**
	 * Build a "availability" resource.
	 * 
	 * @param service	Description of the TAP service which will own this resource.
	 */
	protected Availability(final ServiceConnection service){
		this.service = service;
	}

	@Override
	public final void setTAPBaseURL(final String baseURL){
		accessURL = ((baseURL == null) ? "" : (baseURL + "/")) + getName();
	}

	@Override
	public final String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public final String getStandardID(){
		return "ivo://ivoa.net/std/VOSI#availability";
	}

	@Override
	public final String getAccessURL(){
		return accessURL;
	}

	@Override
	public String getCapability(){
		return Capabilities.getDefaultCapability(this);
	}

	@Override
	public void init(final ServletConfig config) throws ServletException{
		;
	}

	@Override
	public void destroy(){
		;
	}

	@Override
	public boolean executeResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException, TAPException{
		/* "In the REST binding, the support interfaces shall have distinct URLs in the HTTP scheme and shall be accessible by the GET operation in the HTTP protocol.
		 * The response to an HTTP POST, PUT or DELETE to these resources is not defined by this specification. However, if an implementation has no special action
		 * to perform for these requests, the normal response would be a 405 "Method not allowed" error."
		 * (Extract of the VOSI definition: http://www.ivoa.net/documents/VOSI/20100311/PR-VOSI-1.0-20100311.html#sec2) */
		if (!request.getMethod().equalsIgnoreCase("GET"))
			throw new TAPException("The AVAILABILITY resource is only accessible in HTTP-GET! No special action can be perfomed with another HTTP method.", HttpServletResponse.SC_METHOD_NOT_ALLOWED);

		// Set the response MIME type (XML):
		response.setContentType("text/xml");

		// Get the output stream:
		PrintWriter pw = response.getWriter();

		// ...And write the XML document describing the availability of the TAP service:
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		pw.println("<availability xmlns=\"http://www.ivoa.net/xml/VOSIAvailability/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VOSIAvailability/v1.0 http://www.ivoa.net/xml/VOSIAvailability/v1.0\">");

		// available ? (true or false)
		pw.print("\t<available>");
		pw.print(service.isAvailable());
		pw.println("</available>");

		// reason/description of the (non-)availability:
		pw.print("\t<note>");
		pw.print(VOSerializer.formatText(service.getAvailability()));
		pw.println("</note>");

		pw.println("</availability>");

		pw.flush();

		return true;
	}

}
