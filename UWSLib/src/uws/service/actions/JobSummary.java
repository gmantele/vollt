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
import java.util.Enumeration;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.job.serializer.UWSSerializer;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.log.UWSLog.LogLevel;
import uws.service.wait.BlockingPolicy;
import uws.service.wait.WaitObserver;

/**
 * The "Get Job" action of a UWS.
 *
 * <p><i>Note:
 * 	The corresponding name is {@link UWSAction#JOB_SUMMARY}.
 * </i></p>
 *
 * <p>
 * 	This action returns the summary of the job specified in the given UWS URL.
 * 	This summary is serialized by the {@link UWSSerializer} chosen in function
 * 	of the HTTP Accept header.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (11/2017)
 */
public class JobSummary extends UWSAction {
	private static final long serialVersionUID = 1L;

	/** Name of the parameter which allows the blocking behavior
	 * (for a specified or unlimited duration) of a {@link JobSummary} request.
	 * @since 4.3 */
	public final static String WAIT_PARAMETER = "WAIT";

	public JobSummary(UWSService u){
		super(u);
	}

	/**
	 * @see UWSAction#JOB_SUMMARY
	 * @see uws.service.actions.UWSAction#getName()
	 */
	@Override
	public String getName(){
		return JOB_SUMMARY;
	}

	@Override
	public String getDescription(){
		return "Lets getting a summary of the specified job. (URL: {baseUWS_URL}/{jobListName}/{job-id}, Method: HTTP-GET, No parameter)";
	}

	/**
	 * Checks whether:
	 * <ul>
	 * 	<li>a job list name is specified in the given UWS URL
	 * 		<i>(<u>note:</u> the existence of the jobs list is not checked)</i>,
	 * 	</li>
	 * 	<li>a job ID is given in the UWS URL
	 * 		<i>(<u>note:</u> the existence of the job is not checked)</i>,</li>
	 * 	<li>there is no job attribute,</li>
	 * 	<li>the HTTP method is HTTP-GET.</li>
	 * </ul>
	 *
	 * @see uws.service.actions.UWSAction#match(UWSUrl, JobOwner, HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException{
		return (urlInterpreter.hasJobList() && urlInterpreter.hasJob() && !urlInterpreter.hasAttribute() && request.getMethod().equalsIgnoreCase("get"));
	}

	/**
	 * Gets the specified job <i>(and throw an error if not found)</i>,
	 * chooses the serializer and write the serialization of the job in the
	 * given response.
	 *
	 * @see #getJob(UWSUrl)
	 * @see UWSService#getSerializer(String)
	 * @see UWSJob#serialize(ServletOutputStream, UWSSerializer)
	 *
	 * @see uws.service.actions.UWSAction#apply(UWSUrl, JobOwner, HttpServletRequest, HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException{
		// Get the job:
		UWSJob job = getJob(urlInterpreter);

		// Block if necessary:
		JobSummary.block(uws.getWaitPolicy(), request, job, user);

		// Write the job summary:
		UWSSerializer serializer = uws.getSerializer(request.getHeader("Accept"));
		response.setContentType(serializer.getMimeType());
		response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);
		try{
			job.serialize(response.getOutputStream(), serializer, user);
		}catch(Exception e){
			if (!(e instanceof UWSException)){
				getLogger().logUWS(LogLevel.ERROR, urlInterpreter, "SERIALIZE", "Can not serialize the job \"" + job.getJobId() + "\"!", e);
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e, "Can not format properly the job \"" + job.getJobId() + "\"!");
			}else
				throw (UWSException)e;
		}

		return true;
	}

	/**
	 * Block the current thread until the specified duration (in seconds) is
	 * elapsed or if the execution phase of the target job changes.
	 *
	 * <p>
	 * 	A blocking is performed only if the given job is in an active phase
	 * 	(i.e. PENDING, QUEUED or EXECUTING).
	 * </p>
	 *
	 * <p>This function expects the 2 following HTTP-GET parameters:</p>
	 * <ul>
	 * 	<li><b>WAIT</b>: <i>[MANDATORY]</i> with a value (in seconds). The value
	 * 		             must be a positive and not null integer expressing a
	 * 		             duration in seconds or -1 (or any other negative value)
	 * 		             for an infinite time. If a not legal value or no value
	 * 		             is provided, the parameter will be merely ignored.
	 * 		             <br/>
	 * 	                 This parameter raises a flag meaning a blocking is
	 * 		             required (except if 0 is provided) and eventually a
	 * 		             time (in seconds) to wait before stop blocking.
	 * 		             <br/>
	 * 	                 If several values are provided, only the one meaning
	 * 		             the smallest blocking waiting time will be kept.
	 * 		             Particularly if both a negative and a positive or null
	 * 		             value are given, only the positive or null value will
	 * 		             be kept.</li>
	 *
	 * 	<li><b>PHASE</b>: <i>[OPTIONAL]</i> A legal execution phase must be
	 * 		              provided, otherwise this parameter will be ignored.
	 * 		              <br/>
	 * 	                  This parameter indicates the phase in which the job
	 * 		              must be at the time the blocking is required. If the
	 * 		              current job phase is different from the specified one,
	 * 		              no blocking will be performed. Note that the allowed
	 * 		              phases are PENDING, QUEUED and EXECUTING, because only
	 * 		              a job in one of these phases can be blocked.
	 * 		              <br/>
	 * 	                  If several values are provided, only the last
	 * 		              occurrence is kept.</li>
	 * </ul>
	 *
	 * <p><i>Note:
	 * 	A waiting time of 0 will be interpreted as "no blocking".
	 * </i></p>
	 *
	 * <p><i>Note:
	 * 	This function will have no effect if the given thread, the given HTTP
	 * 	request or the given job is NULL.
	 * </i></p>
	 *
	 * @param policy		Strategy to adopt for the blocking behavior.
	 *              		<i>If NULL, the standard blocking behavior will be
	 *              		performed: block the duration (eventually unlimited)
	 *              		specified by the user.</i>
	 * @param req			The HTTP request which asked for the blocking.
	 *           			<b>MUST NOT be NULL, otherwise no blocking will be
	 *           			performed.</b>
	 * @param job			The job associate with the HTTP request.
	 *           			<b>MUST NOT be NULL, otherwise no blocking will be
	 *           			performed.</b>
	 * @param user			The user who asked for the blocking behavior.
	 *            			<i>NULL if no user is logged in.</i>
	 *
	 * @since 4.3
	 */
	public static void block(final BlockingPolicy policy, final HttpServletRequest req, final UWSJob job, final JobOwner user){
		if (req == null || job == null)
			return;

		/* No blocking if the job is not in an "active" phase: */
		if (job.getPhase() != ExecutionPhase.PENDING && job.getPhase() != ExecutionPhase.QUEUED && job.getPhase() != ExecutionPhase.EXECUTING)
			return;

		/* Extract the parameters WAIT (only the smallest waiting time is taken
		 * into account) and PHASE (only the last legal occurrence is taken into
		 * account): */
		ExecutionPhase phase = null;
		boolean waitGiven = false;
		long waitingTime = 0;
		String param;
		String[] values;
		Enumeration<String> parameters = req.getParameterNames();
		while(parameters.hasMoreElements()){
			param = parameters.nextElement();
			values = req.getParameterValues(param);
			// CASE: WAIT parameter
			if (param.toUpperCase().equals("WAIT")){
				/* note: a value MUST be given for a WAIT parameter ; if it is
				 *       missing the parameter is ignored */
				if (values != null){
					for(int i = 0; i < values.length; i++){
						try{
							if (values[i] != null && values[i].trim().length() > 0){
								long tmp = Long.parseLong(values[i]);
								if (tmp < 0 && !waitGiven)
									waitingTime = tmp;
								else if (tmp >= 0)
									waitingTime = (waitGiven && waitingTime >= 0) ? Math.min(waitingTime, tmp) : tmp;
								waitGiven = true;
							}
						}catch(NumberFormatException nfe){}
					}
				}
			}
			// CASE: PHASE parameter
			else if (param.toUpperCase().equals("PHASE") && values != null){
				for(int i = values.length - 1; phase == null && i >= 0; i--){
					try{
						if (values[i].trim().length() > 0)
							phase = ExecutionPhase.valueOf(values[i].toUpperCase());
					}catch(IllegalArgumentException iae){}
				}
			}
		}

		/* The HTTP-GET request should block until either the specified time
		 * (or the timeout) is reached or if the job phase changed: */
		if (waitingTime != 0 && (phase == null || job.getPhase() == phase)){
			Thread threadToBlock = Thread.currentThread();
			WaitObserver observer = null;

			/* Eventually limit the waiting time in function of the chosen
			 * policy: */
			if (policy != null)
				waitingTime = policy.block(threadToBlock, waitingTime, job, user, req);

			/* Blocking ONLY IF the duration is NOT NULL (i.e. wait during 0
			 * seconds): */
			if (waitingTime != 0){
				try{
					/* Watch the job in order to detect an execution phase
					 * modification: */
					observer = new WaitObserver(threadToBlock);
					job.addObserver(observer);

					/* If the job is still processing, then wait the specified
					 * time: */
					if (job.getPhase() == ExecutionPhase.PENDING || job.getPhase() == ExecutionPhase.QUEUED || job.getPhase() == ExecutionPhase.EXECUTING){
						synchronized(threadToBlock){
							// Limited duration:
							if (waitingTime > 0)
								threadToBlock.wait(waitingTime * 1000);
							/* "Unlimited" duration (the wait will stop only if
							 * the job phase changes): */
							else
								threadToBlock.wait();
						}
					}

				}catch(InterruptedException ie){
					/* If the WAIT has been interrupted, the blocking
					 * is stopped and nothing special should happen. */
				}
				/* Clear all retained resources. */
				finally{
					// Do not observe any more the job:
					if (observer != null)
						job.removeObserver(observer);

					/* Notify the BlockingPolicy that this Thread is no longer
					 * blocked: */
					if (policy != null)
						policy.unblocked(threadToBlock, job, user, req);
				}
			}
		}
	}

}
