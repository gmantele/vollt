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
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Capabilities implements TAPResource, VOSIResource {

	public static final String RESOURCE_NAME = "capabilities";

	private final TAP<?> tap;
	protected String accessURL = getName();

	public Capabilities(TAP<?> tap){
		this.tap = tap;
	}

	/**
	 */
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

	}

	@Override
	public void destroy(){
		// TODO Auto-generated method stub

	}

	@Override
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		response.setContentType("application/xml");

		StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<vosi:capabilities xmlns:vosi=\"http://www.ivoa.net/xml/VOSICapabilities/v1.0\"");
		xml.append(" xmlns:tr=\"http://www.ivoa.net/xml/TAP/v0.1\"");
		xml.append(" xmlns:vr=\"http://www.ivoa.net/xml/VOResource/v1.0\"");
		xml.append(" xmlns:vs=\"http://www.ivoa.net/xml/VODataService/v1.0\"");
		xml.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		xml.append(" xsi:schemaLocation=\"http://www.ivoa.net/xml/TAP/v0.1 http://www.ivoa.net/xml/TAP/v0.1\">\n");

		xml.append(tap.getCapability());

		// Build the xml document:
		Iterator<TAPResource> it = tap.getTAPResources();
		while(it.hasNext()){
			TAPResource res = it.next();
			if (res instanceof VOSIResource){
				String cap = ((VOSIResource)res).getCapability();
				if (cap != null)
					xml.append('\n').append(cap);
			}
		}

		xml.append("\n</vosi:capabilities>");

		// Write the Capabilities resource into the ServletResponse:
		PrintWriter out = response.getWriter();
		out.print(xml.toString());
		out.flush();

		return true;
	}

	public static final String getDefaultCapability(VOSIResource res){
		return "\t<capability standardID=\"" + res.getStandardID() + "\">\n" + "\t\t<interface xsi:type=\"vs:ParamHTTP\" role=\"std\">\n" + "\t\t\t<accessURL use=\"full\"> " + ((res.getAccessURL() == null) ? "" : res.getAccessURL()) + " </accessURL>\n" + "\t\t</interface>\n" + "\t</capability>";
	}

}
