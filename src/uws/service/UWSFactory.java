package uws.service;

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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;

/**
 * Let's creating UWS jobs, their threads and extracting their parameters from {@link HttpServletRequest}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 * 
 * @see UWS#getFactory()
 */
public interface UWSFactory {

	/**
	 * Creates a (PENDING) UWS job from the given HTTP request.
	 * 
	 * @param request		Request which triggers the creation of a UWS job.
	 * @param user			The identified UWS user (see {@link UserIdentifier}).
	 * 
	 * @return				The created job.
	 * 
	 * @throws UWSException	If there is an error while creating the job.
	 */
	public UWSJob createJob(final HttpServletRequest request, final JobOwner user) throws UWSException;

	/**
	 * <p>Creates a UWS job with the following attributes.</p>
	 * 
	 * <i>
	 * 	<ul>
	 * 		<li><u>note1:</u> This function is mainly used to restore a UWS job at the UWS initialization.</li>
	 * 		<li><u>note2:</u> The job phase is chosen automatically from the given job attributes (i.e. no endTime =&gt; PENDING, no result and no error =&gt; ABORTED, ...).</li>
	 * 	</ul>
	 * </i>
	 * 
	 * @param jobID			ID of the job (NOT NULL).
	 * @param owner			Owner of the job.
	 * @param params		List of all input UWS job parameters.
	 * @param quote			Its quote (in seconds).
	 * @param startTime		Date/Time of the start of this job.
	 * @param endTime		Date/Time of the end of this job.
	 * @param results		All results of this job.
	 * @param error			The error which ended the job to create.
	 * 
	 * @return				The created job.
	 * 
	 * @throws UWSException	If there is an error while creating the job.
	 */
	public UWSJob createJob(final String jobID, final JobOwner owner, final UWSParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws UWSException;

	/**
	 * Creates the thread which will executes the task described by the given {@link UWSJob} instance.
	 * 
	 * @param jobDescription	Description of the task to execute.
	 * 
	 * @return					The task to execute.
	 * 
	 * @throws UWSException		If there is an error while creating the job task.
	 */
	public JobThread createJobThread(final UWSJob jobDescription) throws UWSException;

	/**
	 * Lets extracting all parameters from the given request.
	 * 
	 * @param req		The request from which parameters must be extracted.
	 * 
	 * @return			The extracted parameters.
	 * 
	 * @throws UWSException	If an error occurs while extracting parameters.
	 */
	public UWSParameters createUWSParameters(final HttpServletRequest request) throws UWSException;

	/**
	 * Lets extracting all parameters from the given map.
	 * 
	 * @param params	All the parameters to check and to store.
	 * 
	 * @return			The extracted parameters.
	 * 
	 * @throws UWSException	If an error occurs while extracting parameters.
	 */
	public UWSParameters createUWSParameters(final Map<String,Object> params) throws UWSException;

}
