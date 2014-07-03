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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;

public class Availability implements TAPResource, VOSIResource {

	public static final String RESOURCE_NAME = "availability";

	private final ServiceConnection service;
	protected String accessURL = getName();

	protected Availability(ServiceConnection service){
		this.service = service;
	}

	public ServiceConnection getService(){
		return service;
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
	public void init(ServletConfig config) throws ServletException{
		;
	}

	@Override
	public void destroy(){
		;
	}

	@Override
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		if (!request.getMethod().equalsIgnoreCase("GET"))	// ERREUR 405 selon VOSI (cf p.4)
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "The AVAILABILITY resource is only accessible in HTTP-GET !");

		response.setContentType("text/xml");

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		xml += "<availability xmlns=\"http://www.ivoa.net/xml/VOSIAvailability/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VOSIAvailability/v1.0 http://www.ivoa.net/xml/VOSIAvailability/v1.0\">\n";
		xml += "\t<available>" + service.isAvailable() + "</available>\n\t<note>" + service.getAvailability() + "</note>\n";
		xml += "</availability>";

		PrintWriter pw = response.getWriter();
		pw.print(xml);
		pw.flush();

		return true;
	}

}
