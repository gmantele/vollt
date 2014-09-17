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
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPException;
import uk.ac.starlink.votable.VOSerializer;

/**
 * <p>TAP resource describing the capabilities of a TAP service.</p>
 * 
 * <p>This resource just return an XML document giving a description of the TAP service and list all its VOSI resources.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public class Capabilities implements TAPResource, VOSIResource {

	/** Name of this TAP resource. */
	public static final String RESOURCE_NAME = "capabilities";

	/** Representation of the whole TAP service. This object list all available resources ;
	 * resources that correspond to the capabilities this resource must list. */
	private final TAP tap;

	/** <p>URL toward this TAP resource.
	 * This URL is particularly important for its declaration in the capabilities of the TAP service.</p>
	 * 
	 * <p><i>Note: By default, it is just the name of this resource. It is updated after initialization of the service
	 * when the TAP service base URL is communicated to its resources. Then, it is: baseTAPURL + "/" + RESOURCE_NAME.</i></p> */
	protected String accessURL = getName();

	/**
	 * Build a "/capabilities" resource.
	 * 
	 * @param tap	Object representation of the whole TAP service.
	 */
	public Capabilities(final TAP tap){
		this.tap = tap;
	}

	@Override
	public final void setTAPBaseURL(String baseURL){
		accessURL = ((baseURL == null) ? "" : (baseURL + "/")) + getName();
	}

	@Override
	public final String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public final String getStandardID(){
		return "ivo://ivoa.net/std/VOSI#capabilities";
	}

	@Override
	public final String getAccessURL(){
		return accessURL;
	}

	@Override
	public String getCapability(){
		return getDefaultCapability(this);
	}

	@Override
	public void init(ServletConfig config) throws ServletException{
		;
	}

	@Override
	public void destroy(){
		;
	}

	@Override
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws IOException, TAPException{
		/* "In the REST binding, the support interfaces shall have distinct URLs in the HTTP scheme and shall be accessible by the GET operation in the HTTP protocol.
		 * The response to an HTTP POST, PUT or DELETE to these resources is not defined by this specification. However, if an implementation has no special action
		 * to perform for these requests, the normal response would be a 405 "Method not allowed" error."
		 * (Extract of the VOSI definition: http://www.ivoa.net/documents/VOSI/20100311/PR-VOSI-1.0-20100311.html#sec2) */
		if (!request.getMethod().equalsIgnoreCase("GET"))
			throw new TAPException("The CAPABILITIES resource is only accessible in HTTP-GET! No special action can be perfomed with another HTTP method.", HttpServletResponse.SC_METHOD_NOT_ALLOWED);

		// Set the response MIME type (XML):
		response.setContentType("application/xml");

		// Get the response stream:
		PrintWriter out = response.getWriter();

		// Write the XML document header:
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.print("<vosi:capabilities xmlns:vosi=\"http://www.ivoa.net/xml/VOSICapabilities/v1.0\"");
		out.print(" xmlns:tr=\"http://www.ivoa.net/xml/TAP/v0.1\"");
		out.print(" xmlns:vr=\"http://www.ivoa.net/xml/VOResource/v1.0\"");
		out.print(" xmlns:vs=\"http://www.ivoa.net/xml/VODataService/v1.0\"");
		out.print(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		out.println(" xsi:schemaLocation=\"http://www.ivoa.net/xml/TAP/v0.1 http://www.ivoa.net/xml/TAP/v0.1\">");

		// Write the full list of this TAP capabilities:
		out.print(tap.getCapability());

		// Write the capabilities of all VOSI resources:
		Iterator<TAPResource> it = tap.getTAPResources();
		while(it.hasNext()){
			TAPResource res = it.next();
			if (res instanceof VOSIResource){
				String cap = ((VOSIResource)res).getCapability();
				if (cap != null){
					out.println();
					out.print(cap);
				}
			}
		}

		// Write the end of the XML document:
		out.println("\n</vosi:capabilities>");

		out.flush();

		return true;
	}

	/**
	 * Write the XML description of the given VOSI resource.
	 * 
	 * @param res	Resource to describe in XML.
	 * 
	 * @return	XML description of the given VOSI resource.
	 */
	public static final String getDefaultCapability(final VOSIResource res){
		return "\t<capability " + VOSerializer.formatAttribute("standardID", res.getStandardID()) + ">\n" + "\t\t<interface xsi:type=\"vs:ParamHTTP\" role=\"std\">\n" + "\t\t\t<accessURL use=\"full\"> " + ((res.getAccessURL() == null) ? "" : VOSerializer.formatText(res.getAccessURL())) + " </accessURL>\n" + "\t\t</interface>\n" + "\t</capability>";
	}

}
