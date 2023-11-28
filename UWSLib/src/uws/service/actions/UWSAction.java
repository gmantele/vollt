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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.job.JobList;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.log.UWSLog;

/**
 * <p>Action of a UWS (i.e. "List Jobs", "Get Job", etc...). An instance of a UWSAction can be added to a given UWS thanks to the method
 * {@link UWSService#addUWSAction(UWSAction)}.</p>
 * 
 * <p><b><u>WARNING:</u> The action of a UWS have, each one, a different name. So be careful about the name of your UWS action !
 * By default the name of a UWS action is the full java name of the class !</b></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (11/2014)
 * 
 * @see UWSService
 */
public abstract class UWSAction implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Name of the UWS action {@link ListJobs}. */
	public final static String LIST_JOBS = "List Jobs";
	/** Name of the UWS action {@link AddJob}. */
	public final static String ADD_JOB = "Add Job";
	/** Name of the UWS action {@link SetUWSParameter}.
	 * @since 4.1 */
	public final static String SET_UWS_PARAMETER = "Set UWS Parameter";
	/** Name of the UWS action {@link DestroyJob}. */
	public final static String DESTROY_JOB = "Destroy Job";
	/** Name of the UWS action {@link JobSummary}. */
	public final static String JOB_SUMMARY = "Get Job";
	/** Name of the UWS action {@link GetJobParam}. */
	public final static String GET_JOB_PARAM = "Get Job Parameter";
	/** Name of the UWS action {@link SetJobParam}. */
	public final static String SET_JOB_PARAM = "Set Job Parameter";
	/** Name of the UWS action {@link ShowHomePage}. */
	public final static String HOME_PAGE = "Show UWS Home Page";

	/** The UWS on which this action must be applied. */
	protected final UWSService uws;

	/* *********** */
	/* CONSTRUCTOR */
	/* *********** */
	/**
	 * Builds a UWSAction.
	 * 
	 * @param u	The UWS which contains this action.
	 */
	protected UWSAction(UWSService u){
		uws = u;
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	/**
	 * Gets the UWS which contains this action.
	 * 
	 * @return	Its UWS.
	 */
	public final UWSService getUWS(){
		return uws;
	}

	/**
	 * Get the logger associated with this UWS service.
	 * 
	 * @return	UWS logger.
	 * 
	 * @since 4.1
	 */
	public final UWSLog getLogger(){
		return uws.getLogger();
	}

	/**
	 * <p>Gets the name of this UWS action. <b>MUST BE UNIQUE !</b></p>
	 * 
	 * <p><i><u>Note:</u> By default the name of the class is returned ({@link Class#getName()}).</i></p>
	 * 
	 * @return	Its name.
	 */
	public String getName(){
		return getClass().getName();
	}

	/**
	 * <p>Gets the description of this UWS action.</p>
	 * 
	 * <p><i><u>Note:</u> By default an empty string is returned.</i></p>
	 * 
	 * @return	Its description.
	 */
	public String getDescription(){
		return "";
	}

	/* ************ */
	/* TOOL METHODS */
	/* ************ */
	/**
	 * Extracts the name of the jobs list from the given UWS URL
	 * and gets the jobs list from the UWS.
	 * 
	 * @param urlInterpreter	The UWS URL which contains the name of the jobs list to get.
	 * 
	 * @return					The corresponding jobs list.
	 * 
	 * @throws UWSException		If there is no jobs list name in the given UWS URL
	 * 							or if no corresponding jobs list can be found in the UWS.
	 * 
	 * @see UWSUrl#getJobListName()
	 * @see UWSService#getJobList(String)
	 */
	protected final JobList getJobsList(UWSUrl urlInterpreter) throws UWSException{
		String jlName = urlInterpreter.getJobListName();
		JobList jobsList = null;

		if (jlName != null){
			jobsList = uws.getJobList(jlName);
			if (jobsList == null)
				throw new UWSException(UWSException.NOT_FOUND, "Incorrect job list name! The jobs list " + jlName + " does not exist.");
		}else
			throw new UWSException(UWSException.BAD_REQUEST, "Missing job list name!");

		return jobsList;
	}

	/**
	 * <p>Extracts the job ID from the given UWS URL
	 * and gets the corresponding job from the UWS.</p>
	 * 
	 * <p><i><u>Note:</u> This function calls {@link #getJob(UWSUrl, JobOwner)} with userId=null and checkUser=false !</i></p>
	 * 
	 * @param urlInterpreter	The UWS URL which contains the ID of the job to get.
	 * 
	 * @return					The corresponding job.
	 * 
	 * @throws UWSException		If no jobs list name or/and job ID can be found in the given UWS URL
	 * 							or if there are no corresponding jobs list and/or job in the UWS
	 * 							or if the specified user has not enough rights to get the specified job.
	 * 
	 * @see #getJob(UWSUrl, JobOwner)
	 */
	protected final UWSJob getJob(UWSUrl urlInterpreter) throws UWSException{
		return getJob(urlInterpreter, (JobOwner)null);
	}

	/**
	 * <p>Extracts the job ID from the given UWS URL and gets the corresponding job from the UWS.
	 * The specified job is returned ONLY IF the specified user has enough rights.</p>
	 * 
	 * @param urlInterpreter	The UWS URL which contains the ID of the job to get.
	 * @param user				The user who asks for the specified job.
	 * 
	 * @return					The corresponding job.
	 * 
	 * @throws UWSException		If no jobs list name or/and job ID can be found in the given UWS URL
	 * 							or if there are no corresponding jobs list and/or job in the UWS
	 * 							or if the specified user has not enough rights to get the specified job.
	 * 
	 * @see UWSUrl#getJobId()
	 * @see #getJobsList(UWSUrl)
	 * @see JobList#getJob(String, JobOwner)
	 * 
	 * @since 3.1
	 */
	protected final UWSJob getJob(UWSUrl urlInterpreter, JobOwner user) throws UWSException{
		String jobId = urlInterpreter.getJobId();
		UWSJob job = null;

		if (jobId != null){
			JobList jobsList = getJobsList(urlInterpreter);
			job = jobsList.getJob(jobId, user);
			if (job == null)
				throw new UWSException(UWSException.NOT_FOUND, "Incorrect job ID! The job \"" + jobId + "\" does not exist in the jobs list \"" + jobsList.getName() + "\".");
		}else
			throw new UWSException(UWSException.BAD_REQUEST, "Missing job ID!");

		return job;
	}

	/**
	 * <p>Extracts the job ID from the given UWS URL and gets the corresponding job from the given jobs list.</p>
	 * 
	 * <p><i><u>Note:</u> This function calls {@link #getJob(UWSUrl, JobList, JobOwner)} with userId=null and checkUser=false !</i></p>
	 * 
	 * @param urlInterpreter	The UWS URL which contains the ID of the job to get.
	 * @param jobsList			The jobs list which is supposed to contain the job to get.
	 * 
	 * @return					The corresponding job.
	 * 
	 * @throws UWSException		If no job ID can be found in the given UWS URL
	 * 							or if there are no corresponding job in the UWS.
	 * 
	 * @see #getJob(UWSUrl, JobList, JobOwner)
	 */
	protected final UWSJob getJob(UWSUrl urlInterpreter, JobList jobsList) throws UWSException{
		return getJob(urlInterpreter, jobsList, null);
	}

	/**
	 * <p>Extracts the job ID from the given UWS URL and gets the corresponding job from the given jobs list.
	 * If checkUser=true, the specified job is returned ONLY IF the specified user has enough rights.</p>
	 * 
	 * @param urlInterpreter	The UWS URL which contains the ID of the job to get.
	 * @param jobsList			The jobs list which is supposed to contain the job to get.
	 * @param user				The user who asks for the specified job.
	 * 
	 * @return					The corresponding job.
	 * 
	 * @throws UWSException		If no job ID can be found in the given UWS URL
	 * 							or if there are no corresponding job in the UWS
	 * 							or if the specified user has not enough rights.
	 * 
	 * @see UWSUrl#getJobId()
	 * @see JobList#getJob(String, JobOwner)
	 * 
	 * @since 3.1
	 */
	protected final UWSJob getJob(UWSUrl urlInterpreter, JobList jobsList, JobOwner user) throws UWSException{
		String jobId = urlInterpreter.getJobId();
		UWSJob job = null;

		if (jobId != null){
			if (jobsList == null)
				throw new UWSException(UWSException.BAD_REQUEST, "Missing job list name!");
			job = jobsList.getJob(jobId, user);
			if (job == null)
				throw new UWSException(UWSException.NOT_FOUND, "Incorrect job ID! The job \"" + jobId + "\" does not exist in the jobs list \"" + jobsList.getName() + "\".");
		}else
			throw new UWSException(UWSException.BAD_REQUEST, "Missing job ID!");

		return job;
	}

	/* ************** */
	/* ACTION METHODS */
	/* ************** */
	/**
	 * Indicates whether the given request corresponds to this UWS action.
	 * 
	 * @param urlInterpreter	The UWS URL of the given request.
	 * @param user				The user who has sent the given request.
	 * @param request			The received request.
	 * 
	 * @return					<i>true</i> if the given request corresponds to this UWS action, <i>false</i> otherwise.
	 * 
	 * @throws UWSException		If any error occurs during the tests.
	 */
	public abstract boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException;

	/**
	 * <p>Applies this UWS action in function of the given request
	 * and writes the result in the given response.</p>
	 * 
	 * <p><i><u>Note:</u> You can use the functions {@link #getJobsList(UWSUrl)}, {@link #getJob(UWSUrl)} and {@link #getJob(UWSUrl, JobList)} to
	 * get more easily the jobs list and/or the job from the given UWS URL !</i></p>
	 * 
	 * @param urlInterpreter	The UWS URL of the given request.
	 * @param user				The user who has sent the given request.
	 * @param request			The received request.
	 * @param response			The response of the given request (MUST BE UPDATED).
	 * 
	 * @return					<i>true</i> if the actions has been successfully applied, <i>false</i> otherwise.
	 * 
	 * @throws UWSException		If any error occurs during the action application.
	 * @throws IOException		If there is an error while the result is written in the given response.
	 */
	public abstract boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException;

	/* ************* */
	/* MISCELLANEOUS */
	/* ************* */
	@Override
	public final boolean equals(Object obj){
		if (obj instanceof UWSAction)
			return getName().equals(((UWSAction)obj).getName());
		else
			return super.equals(obj);
	}

	@Override
	public String toString(){
		return getName();
	}

}
