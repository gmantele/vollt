package uws.service.actions;

/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;

import uws.job.JobList;

import uws.job.serializer.UWSSerializer;

import uws.job.user.JobOwner;

import uws.service.UWSService;
import uws.service.UWSUrl;

/**
 * <p>The "List Jobs" action of a UWS.</p>
 * 
 * <p><i><u>Note:</u> The corresponding name is {@link UWSAction#LIST_JOBS}.</i></p>
 * 
 * <p>This action returns the list of jobs contained in the jobs list specified by the URL of the request.
 * This list is serialized by the {@link UWSSerializer} choosed in function of the HTTP Accept header.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public class ListJobs extends UWSAction {
	private static final long serialVersionUID = 1L;

	public ListJobs(UWSService u){
		super(u);
	}

	/**
	 * @see UWSAction#LIST_JOBS
	 * @see uws.service.actions.UWSAction#getName()
	 */
	@Override
	public String getName(){
		return LIST_JOBS;
	}

	@Override
	public String getDescription(){
		return "Gives a list of all jobs contained into the specified jobs list. (URL: {baseUWS_URL}/{jobListName}, Method: HTTP-GET, No parameters)";
	}

	/**
	 * Checks whether:
	 * <ul>
	 * 	<li>a job list name is specified in the given UWS URL <i>(<u>note:</u> the existence of the jobs list is not checked)</i>,</li>
	 * 	<li>the UWS URL does not make a reference to a job (so: no job ID),</li>
	 * 	<li>the HTTP method is HTTP-GET.</li>
	 * </ul>
	 * 
	 * @see uws.service.actions.UWSAction#match(uws.service.UWSUrl, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException{
		return (urlInterpreter.hasJobList() && !urlInterpreter.hasJob() && request.getMethod().equalsIgnoreCase("get"));
	}

	/**
	 * Gets the specified jobs list <i>(and throw an error if not found)</i>,
	 * chooses the serializer and write the serialization of the jobs list in the given response.
	 * 
	 * @see #getJobsList(UWSUrl)
	 * @see UWSService#getSerializer(String)
	 * @see JobList#serialize(ServletOutputStream, UWSSerializer, String)
	 * 
	 * @see uws.service.actions.UWSAction#apply(uws.service.UWSUrl, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobsList(urlInterpreter);

		// Write the jobs list:
		UWSSerializer serializer = uws.getSerializer(request.getHeader("Accept"));
		response.setContentType(serializer.getMimeType());
		jobsList.serialize(response.getOutputStream(), serializer, user);

		return true;
	}

}
