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
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPJob;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPSyncJob;
import tap.parameters.TAPParameters;
import uws.UWSException;

public class Sync implements TAPResource {

	public static final String RESOURCE_NAME = "sync";

	protected String accessURL = null;

	protected final ServiceConnection<?> service;

	protected final Capabilities capabilities;

	public Sync(ServiceConnection<?> service, Capabilities capabilities){
		this.service = service;
		this.capabilities = capabilities;
	}

	@Override
	public String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public void setTAPBaseURL(String baseURL){
		accessURL = ((baseURL != null) ? (baseURL + "/") : "") + getName();
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
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, TAPException, UWSException{
		TAPParameters params = (TAPParameters)service.getFactory().createUWSParameters(request);
		params.check();

		if (params.getRequest().equalsIgnoreCase(TAPJob.REQUEST_GET_CAPABILITIES))
			return capabilities.executeResource(request, response);

		if (!service.isAvailable()){
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, service.getAvailability());
			return false;
		}

		TAPSyncJob syncJob = new TAPSyncJob(service, params);
		syncJob.start(response);

		return true;
	}

}
