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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPJob;
import tap.TAPSyncJob;
import tap.parameters.TAPParameters;
import uws.UWSException;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Synchronous resource of a TAP service.</p>
 * 
 * <p>
 * 	Requests sent to this resource can be either to get the capabilities of the TAP service (which should actually be accessed with the resource /capabilities)
 * 	or to execute synchronously an ADQL query. For the second case, "synchronously" means that result or error is returned immediately when the execution ends.
 * 	Besides, generally, the execution time is much more limited than an asynchronous query. 
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public class Sync implements TAPResource {

	/** Name of this TAP resource. */
	public static final String RESOURCE_NAME = "sync";

	/** Description of the TAP service owning this resource. */
	protected final ServiceConnection service;

	/** List of all capabilities of the TAP service. */
	protected final Capabilities capabilities;

	/**
	 * Build a synchronous resource for the TAP service whose the description and
	 * the capabilities are provided in parameters.
	 * 
	 * @param service		Description of the TAP service which will own this resource.
	 * @param capabilities	Capabilities of the TAP service.
	 */
	public Sync(final ServiceConnection service, final Capabilities capabilities){
		this.service = service;
		this.capabilities = capabilities;
	}

	@Override
	public String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public void setTAPBaseURL(final String baseURL){
		;
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
		// Retrieve the execution parameters:
		TAPParameters params = service.getFactory().createTAPParameters(request);
		params.check();

		// CASE 1: GET CAPABILITIES
		/* If the user asks for the capabilities through the TAP parameters, execute the corresponding resource. */
		if (params.getRequest().equalsIgnoreCase(TAPJob.REQUEST_GET_CAPABILITIES))
			return capabilities.executeResource(request, response);

		// CASE 2: EXECUTE SYNCHRONOUSLY AN ADQL QUERY
		// Ensure the service is currently available:
		if (!service.isAvailable())
			throw new TAPException("Can not execute a query: this TAP service is not available! " + service.getAvailability(), UWSException.SERVICE_UNAVAILABLE);

		// Execute synchronously the given job:
		try{
			TAPSyncJob syncJob = new TAPSyncJob(service, params);
			syncJob.start(response);
			return true;
		}catch(TAPException te){
			throw te;
		}catch(Exception t){
			service.getLogger().logTAP(LogLevel.FATAL, params, "SYNC_INIT", "Unexpected error while executing the given ADQL query!", t);
			throw new TAPException("Unexpected error while executing the given ADQL query!");
		}

	}

}
