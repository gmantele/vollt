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
 * Copyright 2012-2020 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.job.JobList;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>The "Add Job" action of a UWS.</p>
 *
 * <p><i><u>Note:</u> The corresponding name is {@link UWSAction#ADD_JOB}.</i></p>
 *
 * <p>This action creates a new job and adds it to the specified jobs list.
 * The response of this action is a redirection to the new job resource (that is to say: a redirection to the job summary of the new job).</p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.5 (07/2020)
 */
public class AddJob extends UWSAction {
	private static final long serialVersionUID = 1L;

	public AddJob(UWSService u) {
		super(u);
	}

	/**
	 * @see UWSAction#ADD_JOB
	 * @see uws.service.actions.UWSAction#getName()
	 */
	@Override
	public String getName() {
		return ADD_JOB;
	}

	@Override
	public String getDescription() {
		return "Lets adding to the specified jobs list a job whose the parameters are given. (URL: {baseUWS_URL}/{jobListName}, Method: HTTP-POST, Parameters: job parameters)";
	}

	/**
	 * Checks whether:
	 * <ul>
	 * 	<li>a job list name is specified in the given UWS URL <i>(<u>note:</u> by default, the existence of the jobs list is not checked)</i>,</li>
	 * 	<li>the UWS URL does not make a reference to a job (so: no job ID),</li>
	 * 	<li>the HTTP method is HTTP-POST.</li>
	 * </ul>
	 * @see uws.service.actions.UWSAction#match(UWSUrl, JobOwner, HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException {
		return (urlInterpreter.hasJobList() && !urlInterpreter.hasJob() && request.getMethod().equalsIgnoreCase("post"));
	}

	/**
	 * Gets the specified jobs list <i>(throw an error if not found)</i>,
	 * creates a new job, adds it to the jobs list and makes a redirection to the summary of this new job.
	 *
	 * @see #getJobsList(UWSUrl)
	 * @see uws.service.UWSFactory#createJob(HttpServletRequest, JobOwner)
	 * @see JobList#addNewJob(UWSJob)
	 * @see UWSService#redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)
	 *
	 * @see uws.service.actions.UWSAction#apply(UWSUrl, JobOwner, HttpServletRequest, HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException {
		// Get the jobs list:
		JobList jobsList = getJobsList(urlInterpreter);

		// Forbids the job creation if the user has not the WRITE permission for the specified jobs list:
		if (user != null && !user.hasWritePermission(jobsList))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, true, jobsList.getName()));

		// Create the job:
		UWSJob newJob;
		try {
			newJob = uws.getFactory().createJob(request, user);
		} catch(UWSException ue) {
			getLogger().logUWS(LogLevel.ERROR, urlInterpreter, "ADD_JOB", "Can not create a new job!", ue);
			throw ue;
		}

		// Add it to the jobs list:
		if (jobsList.addNewJob(newJob) != null) {

			// Make a redirection to the added job:
			uws.redirect(urlInterpreter.jobSummary(jobsList.getName(), newJob.getJobId()).getRequestURL(), request, user, getName(), response);

			return true;
		} else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Unable to add the new job to the jobs list for an unknown reason. (ID of the new job = \"" + newJob.getJobId() + "\" ; ID already used = " + (jobsList.getJob(newJob.getJobId()) != null) + ")");
	}

}
