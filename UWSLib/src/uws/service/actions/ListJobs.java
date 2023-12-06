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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.JobList;
import uws.job.serializer.UWSSerializer;
import uws.job.serializer.filter.JobListRefiner;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.log.UWSLog.LogLevel;

/**
 * The "List Jobs" action of a UWS.
 *
 * <p><i>Note:
 * 	The corresponding name is {@link UWSAction#LIST_JOBS}.
 * </i></p>
 *
 * <p>
 * 	This action returns the list of jobs contained in the jobs list specified by
 * 	the URL of the request. This list is serialized by the {@link UWSSerializer}
 * 	chosen in function of the HTTP Accept header.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (10/2017)
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
	 * 	<li>a job list name is specified in the given UWS URL
	 * 	    <i>(<u>note:</u> the existence of the jobs list is not checked)</i>,</li>
	 * 	<li>the UWS URL does not make a reference to a job (so: no job ID),</li>
	 * 	<li>the HTTP method is HTTP-GET.</li>
	 * </ul>
	 *
	 * @see uws.service.actions.UWSAction#match(UWSUrl, JobOwner, HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException{
		return (urlInterpreter.hasJobList() && !urlInterpreter.hasJob() && request.getMethod().equalsIgnoreCase("get"));
	}

	/**
	 * Gets the specified jobs list <i>(and throw an error if not found)</i>,
	 * chooses the serializer and write the serialization of the jobs list in
	 * the given response.
	 *
	 * @see #getJobsList(UWSUrl)
	 * @see JobListRefiner#JobListRefiner(HttpServletRequest)
	 * @see UWSService#getSerializer(String)
	 * @see JobList#serialize(ServletOutputStream, UWSSerializer, JobOwner, JobListRefiner)
	 *
	 * @see uws.service.actions.UWSAction#apply(UWSUrl, JobOwner, HttpServletRequest, HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobsList(urlInterpreter);

		// Write the jobs list:
		UWSSerializer serializer = uws.getSerializer(request.getHeader("Accept"));
		response.setContentType(serializer.getMimeType());
		response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);
		try{
			jobsList.serialize(response.getOutputStream(), serializer, user, new JobListRefiner(request));
		}catch(Exception e){
			if (!(e instanceof UWSException)){
				getLogger().logUWS(LogLevel.ERROR, urlInterpreter, "SERIALIZE", "Can not serialize the jobs list \"" + jobsList.getName() + "\"!", e);
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e, "Can not format properly the jobs list \"" + jobsList.getName() + "\"!");
			}else
				throw (UWSException)e;
		}

		return true;
	}

}
