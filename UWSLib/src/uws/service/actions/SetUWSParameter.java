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
 * Copyright 2014-2020 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;

/**
 * <p>The UWS action which lets set the phase (RUN or ABORT), the execution duration and the destruction time of a job
 * with a POST or PUT request on {job-id}/{uws-param}.</p>
 *
 * <p><i><u>Note:</u> The corresponding name is {@link UWSAction#SET_UWS_PARAMETER}.</i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.5 (07/2020)
 * @since 4.1
 */
public class SetUWSParameter extends UWSAction {
	private static final long serialVersionUID = 1L;

	public SetUWSParameter(final UWSService u) {
		super(u);
	}

	/**
	 * @see UWSAction#SET_UWS_PARAMETER
	 * @see uws.service.actions.UWSAction#getName()
	 */
	@Override
	public String getName() {
		return SET_UWS_PARAMETER;
	}

	@Override
	public String getDescription() {
		return "Let change one of the standard UWS parameters of a job (e.g. phase, executionduration, destruction) (URL: {baseUWS_URL}/{jobListName}/{jobId}/{uws-param}, where {uws-param} = \"phase\" or \"executionduration\" or \"destruction\", Method: HTTP-POST or HTTP-PUT, Parameter: \"{uws-param}={param-value}\" in POST and \"{param-value\" in PUT (content-type:text/plain))";
	}

	/**
	 * Checks whether:
	 * <ul>
	 * 	<li>a job list name is specified in the given UWS URL <i>(<u>note:</u> by default, the existence of the jobs list is not checked)</i>,</li>
	 * 	<li>a job ID is given in the UWS URL <i>(<u>note:</u> by default, the existence of the job is not yet checked)</i>,</li>
	 * 	<li>the job attribute "phase", "runID", "executionduration" or "destruction" is used in the UWS URL,
	 * 	<li>the HTTP method is HTTP-POST or HTTP-PUT.</li>
	 * </ul>
	 * @see uws.service.actions.UWSAction#match(UWSUrl, JobOwner, HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException {
		return (urlInterpreter.hasJobList() && urlInterpreter.hasJob() && urlInterpreter.getAttributes().length == 1 && urlInterpreter.getAttributes()[0].toLowerCase().matches(UWSParameters.UWS_RW_PARAMETERS_REGEXP) && (request.getMethod().equalsIgnoreCase("post") || request.getMethod().equalsIgnoreCase("put")) && UWSToolBox.hasParameter(urlInterpreter.getAttributes()[0], request, false));
	}

	/**
	 * Get the specified job <i>(throw an error if not found)</i>,
	 * and update the specified UWS standard parameter.
	 *
	 * @see #getJob(UWSUrl)
	 * @see UWSJob#addOrUpdateParameter(String, Object)
	 * @see UWSService#redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)
	 *
	 * @see uws.service.actions.UWSAction#apply(UWSUrl, JobOwner, HttpServletRequest, HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException {
		// Get the job:
		UWSJob job = getJob(urlInterpreter);

		// Forbids the action if the user has not the WRITE permission for the specified job:
		if (user != null && !user.hasWritePermission(job))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, true, job.getJobId()));

		String name = urlInterpreter.getAttributes()[0];
		job.addOrUpdateParameter(name, UWSToolBox.getParameter(name, request, false), user);

		// Make a redirection to the job:
		uws.redirect(urlInterpreter.jobSummary(urlInterpreter.getJobListName(), job.getJobId()).getRequestURL(), request, user, getName(), response);

		return true;
	}

}
