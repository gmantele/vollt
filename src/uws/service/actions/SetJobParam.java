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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>The "Set Job Parameter" action of a UWS.</p>
 * 
 * <p><i><u>Note:</u> The corresponding name is {@link UWSAction#SET_JOB_PARAM}.</i></p>
 * 
 * <p>This action sets the value of the specified job attribute.
 * The response of this action is a redirection to the job summary.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (04/2015)
 */
public class SetJobParam extends UWSAction {
	private static final long serialVersionUID = 1L;

	public SetJobParam(UWSService u){
		super(u);
	}

	/**
	 * @see UWSAction#SET_JOB_PARAM
	 * @see uws.service.actions.UWSAction#getName()
	 */
	@Override
	public String getName(){
		return SET_JOB_PARAM;
	}

	@Override
	public String getDescription(){
		return "Sets the value of a job attribute/parameter of the specified job. (URL: {baseUWS_URL}/{jobListName}/{job-id}/{job-attribute}, Method: HTTP-POST or HTTP-PUT, Parameter: {JOB-ATTRIBUTE}={attribute-value})";
	}

	/**
	 * Checks whether:
	 * <ul>
	 * 	<li>a job list name is specified in the given UWS URL <i>(<u>note:</u> by default, the existence of the jobs list is not checked)</i>,</li>
	 * 	<li>a job ID is given in the UWS URL <i>(<u>note:</u> by default, the existence of the job is not yet checked)</i>,</li>
	 * 	<li>if the HTTP method is HTTP-POST: there is exactly one attribute <b>and</b> at least one parameter</li>
	 * 	<li>if the HTTP method is HTTP-PUT: there are at least two attributes ({@link UWSJob#PARAM_PARAMETERS}/{parameter_name}) <b>and</b> there are at least two parameters</li>
	 * </ul>
	 * 
	 * @see uws.service.actions.UWSAction#match(uws.service.UWSUrl, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException{
		return (urlInterpreter.hasJobList() && urlInterpreter.hasJob() && ((request.getMethod().equalsIgnoreCase("post") && (!urlInterpreter.hasAttribute() || urlInterpreter.getAttributes().length == 1)) || (request.getMethod().equalsIgnoreCase("put") && urlInterpreter.getAttributes().length >= 2 && urlInterpreter.getAttributes()[0].equalsIgnoreCase(UWSJob.PARAM_PARAMETERS) && UWSToolBox.hasParameter(urlInterpreter.getAttributes()[1], request, false))));
	}

	/**
	 * <p>Gets the specified job <i>(and throw an error if not found)</i>,
	 * changes the value of the specified job attribute
	 * and makes a redirection to the job summary.</p>
	 * 
	 * @see #getJob(UWSUrl, String)
	 * @see UWSService#createUWSParameters(HttpServletRequest)
	 * @see UWSJob#addOrUpdateParameters(java.util.Map)
	 * @see UWSService#redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)
	 * 
	 * @see uws.service.actions.UWSAction#apply(uws.service.UWSUrl, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException{
		// Get the job:
		UWSJob job = getJob(urlInterpreter);

		UWSParameters params;
		try{
			params = uws.getFactory().createUWSParameters(request);
		}catch(UWSException ue){
			getLogger().logUWS(LogLevel.ERROR, request, "SET_PARAM", "Can not parse the sent UWS parameters!", ue);
			throw ue;
		}

		// Update the job parameters:
		boolean updated = job.addOrUpdateParameters(params, user);

		// Make a redirection to the job:
		uws.redirect(urlInterpreter.jobSummary(urlInterpreter.getJobListName(), job.getJobId()).getRequestURL(), request, user, getName(), response);

		return updated;
	}

}
