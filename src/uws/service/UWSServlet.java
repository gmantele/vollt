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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.lang.IllegalStateException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;

import uws.AcceptHeader;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;

import uws.job.ErrorSummary;
import uws.job.JobList;
import uws.job.Result;
import uws.job.UWSJob;

import uws.job.parameters.DestructionTimeController;
import uws.job.parameters.ExecutionDurationController;
import uws.job.parameters.InputParamController;
import uws.job.parameters.UWSParameters;
import uws.job.parameters.DestructionTimeController.DateField;

import uws.job.serializer.JSONSerializer;
import uws.job.serializer.UWSSerializer;
import uws.job.serializer.XMLSerializer;

import uws.job.user.JobOwner;

import uws.service.actions.UWSAction;

import uws.service.backup.UWSBackupManager;

import uws.service.error.DefaultUWSErrorWriter;
import uws.service.error.ServiceErrorWriter;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;

import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog;

/**
 * <p>
 * 	This servlet lets initialize and manage a web service implementing the UWS pattern.
 * 	That's to say all methods and functionalities of a UWS are already implemented. All you have to
 * 	do is to initialize and maybe to customize your UWS.
 * </p>
 * 
 * <h3>UWS Definition</h3>
 * <p>
 * 	To create a such servlet, you have to extend this class. Once done, only two functions must be
 * 	implemented: {@link #createJob(Map, JobOwner)} and {@link #initUWS()}.
 * </p>
 * <p>
 * 	The first one will be called by the library each time a job must be created. All the job parameters
 * 	given by the user and the identity of the user are given in the arguments. You just have to return
 * 	an appropriate instance of a job using all these information.
 * </p>
 * <p>
 * 	{@link #initUWS()} must contain at least one line: the creation of a job list. For instance:
 * </p>
 * <code>
 * 	addJobList(new JobList&lt;MyJob&gt;("jlName"));
 * </code>
 * <p>
 * 	The name and the description of the UWS may be specified in the web.xml file as init-param of the servlet:
 * 	<code>name</code> and <code>description</code>. The other way is to directly set the corresponding
 * 	attributes: {@link #name} and {@link #description}.
 * </p>
 * 
 * <h3>UWS customization</h3>
 * <p>
 * 	As for the classic HTTP servlets, this servlet has one method for each method of the implemented protocol.
 * 	Thus, you have one function for the "add job" action, another one for the "get job list" action, ...
 * 	These functions are:
 * </p>
 * <ul>
 * 	<li>{@link #doAddJob(HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doDestroyJob(HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doGetJobParam(HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doJobSummary(HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doListJob(HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doSetJobParam(HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * </ul>
 * <p>
 * 	They are all already implemented following their definition in the IVOA document. However,
 * 	if needed, they can be overridden in order to do your own actions.
 * </p>
 * <p>
 * 	Besides, the classic HTTP servlet methods (i.e. {@link #doGet(HttpServletRequest, HttpServletResponse)}, {@link #doPost(HttpServletRequest, HttpServletResponse)}, ...)
 * 	are called normally if none of the UWS actions match to the received HTTP request.
 * 	So, they can be overridden as in any HTTP servlet.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public abstract class UWSServlet extends HttpServlet implements UWS, UWSFactory {
	private static final long serialVersionUID = 1L;

	/** Name of this UWS. */
	protected String name = null;

	/** Description of this UWS. */
	protected String description = null;

	/** List of all managed jobs lists. <i>(it is a LinkedHashMap so that jobs lists are ordered by insertion)</i> */
	private Map<String,JobList> mapJobLists;

	/** List of available serializers. */
	private Map<String,UWSSerializer> serializers;

	/** The MIME type of the default serialization format. */
	protected String defaultSerializer = null;

	/** The object to use to extract the user ID from the received request. */
	protected UserIdentifier userIdentifier = null;

	/** The "interpreter" of UWS URLs. */
	private UWSUrl urlInterpreter = null;

	/** List of all expected additional parameters. */
	protected final ArrayList<String> expectedAdditionalParams = new ArrayList<String>(10);

	/** List the controllers of all the input parameters. See {@link UWSParameters} and {@link InputParamController} for more details. */
	protected final HashMap<String,InputParamController> inputParamControllers = new HashMap<String,InputParamController>(10);

	/** Lets managing all UWS files (i.e. log, result, backup, ...). */
	private UWSFileManager fileManager = null;

	/** Lets saving and/or restoring the whole UWS.  */
	protected UWSBackupManager backupManager;

	/** Lets logging info/debug/warnings/errors about this UWS. */
	protected UWSLog logger;

	/** Lets writing/formatting any exception/throwable in a HttpServletResponse. */
	protected ServiceErrorWriter errorWriter;

	@Override
	public final void init(ServletConfig config) throws ServletException{
		super.init(config);
	}

	@Override
	public final void init() throws ServletException{
		// Set the general information about this UWS:
		name = getServletConfig().getInitParameter("name");
		description = getServletConfig().getInitParameter("description");

		// Set the file manager to use:
		try{
			fileManager = createFileManager();
			if (fileManager == null)
				throw new ServletException("Missing file manager ! The function createFileManager() MUST return a valid instanceof UWSFileManager !");
		}catch(UWSException ue){
			throw new ServletException("Error while setting the file manager.", ue);
		}

		// Set the logger:
		logger = new DefaultUWSLog(this);
		errorWriter = new DefaultUWSErrorWriter(this);

		// Initialize the list of jobs:
		mapJobLists = new LinkedHashMap<String,JobList>();

		// Initialize the list of available serializers:
		serializers = new HashMap<String,UWSSerializer>();
		addSerializer(new XMLSerializer());
		addSerializer(new JSONSerializer());

		try{
			initUWS();
			logger.uwsInitialized(this);
		}catch(UWSException ue){
			logger.error("UWS initialization impossible !", ue);
			throw new ServletException("Error while initializing UWS ! See the log for more details.");
		}
	}

	public abstract void initUWS() throws UWSException;

	public UWSFileManager createFileManager() throws UWSException{
		UWSFileManager fm = null;
		String rootPath = getServletConfig().getInitParameter("rootDirectory");
		if (rootPath == null || rootPath.trim().isEmpty())
			rootPath = ((name != null && !name.trim().isEmpty()) ? name.replaceAll("/", "_") : "") + "_files";
		if (rootPath.startsWith("/"))
			fm = new LocalUWSFileManager(new File(rootPath));
		else
			fm = new LocalUWSFileManager(new File(getServletContext().getRealPath("/" + rootPath)));
		return fm;
	}

	@Override
	public UWSFileManager getFileManager(){
		return fileManager;
	}

	@Override
	public final void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException{
		super.service(req, resp);
	}

	@Override
	protected final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		String uwsAction = null;
		JobOwner user = null;

		try{
			String method = req.getMethod();

			// Create a URL interpreter if needed:
			if (urlInterpreter == null)
				setUrlInterpreter(new UWSUrl(req));

			// Update the UWS URL interpreter:
			UWSUrl requestUrl = new UWSUrl(this.urlInterpreter);
			requestUrl.load(req);

			// Identify the user:
			user = (userIdentifier == null) ? null : userIdentifier.extractUserId(requestUrl, req);

			// METHOD GET:
			if (method.equals("GET")){
				// HOME PAGE:
				if (!requestUrl.hasJobList()){
					uwsAction = UWSAction.HOME_PAGE;
					writeHomePage(requestUrl, req, resp, user);

				}// LIST JOBS:
				else if (requestUrl.hasJobList() && !requestUrl.hasJob()){
					uwsAction = UWSAction.LIST_JOBS;
					doListJob(requestUrl, req, resp, user);

				}// JOB SUMMARY:
				else if (requestUrl.hasJobList() && requestUrl.hasJob() && !requestUrl.hasAttribute()){
					uwsAction = UWSAction.JOB_SUMMARY;
					doJobSummary(requestUrl, req, resp, user);

				}// GET JOB PARAMETER:
				else if (requestUrl.hasJobList() && requestUrl.hasJobList() && requestUrl.hasAttribute()){
					uwsAction = UWSAction.GET_JOB_PARAM;
					doGetJobParam(requestUrl, req, resp, user);

				}else{
					logger.httpRequest(req, user, null, 0, null, null);
					super.service(req, resp);
					return;
				}

			}// METHOD POST:
			else if (method.equals("POST")){
				// HOME PAGE:
				if (!requestUrl.hasJobList()){
					uwsAction = UWSAction.HOME_PAGE;
					writeHomePage(requestUrl, req, resp, user);

				}// ADD JOB:
				else if (requestUrl.hasJobList() && !requestUrl.hasJob()){
					uwsAction = UWSAction.ADD_JOB;
					doAddJob(requestUrl, req, resp, user);

				}// SET JOB PARAMETER:
				else if (requestUrl.hasJobList() && requestUrl.hasJob() && (!requestUrl.hasAttribute() || requestUrl.getAttributes().length == 1) && req.getParameterMap().size() > 0){
					uwsAction = UWSAction.SET_JOB_PARAM;
					doSetJobParam(requestUrl, req, resp, user);

				}// DESTROY JOB:
				else if (requestUrl.hasJobList() && requestUrl.hasJob() && req.getParameter(UWSJob.PARAM_ACTION) != null && req.getParameter(UWSJob.PARAM_ACTION).equalsIgnoreCase(UWSJob.ACTION_DELETE)){
					uwsAction = UWSAction.DESTROY_JOB;
					doDestroyJob(requestUrl, req, resp, user);

				}else{
					logger.httpRequest(req, user, null, 0, null, null);
					super.service(req, resp);
					return;
				}

			}// METHOD PUT:
			else if (method.equals("PUT")){
				// SET JOB PARAMETER:
				if (requestUrl.hasJobList() && requestUrl.hasJob() && req.getMethod().equalsIgnoreCase("put") && requestUrl.getAttributes().length >= 2 && requestUrl.getAttributes()[0].equalsIgnoreCase(UWSJob.PARAM_PARAMETERS) && req.getParameter(requestUrl.getAttributes()[1]) != null){
					uwsAction = UWSAction.SET_JOB_PARAM;
					doSetJobParam(requestUrl, req, resp, user);

				}else{
					logger.httpRequest(req, user, null, 0, null, null);
					super.service(req, resp);
					return;
				}

			}// METHOD DELETE:
			else if (method.equals("DELETE")){
				// DESTROY JOB:
				if (requestUrl.hasJobList() && requestUrl.hasJob() && req.getMethod().equalsIgnoreCase("delete")){
					uwsAction = UWSAction.DESTROY_JOB;
					doDestroyJob(requestUrl, req, resp, user);

				}else{
					logger.httpRequest(req, user, null, 0, null, null);
					super.service(req, resp);
					return;
				}

			}// ELSE => DEFAULT BEHAVIOR:
			else{
				logger.httpRequest(req, user, null, 0, null, null);
				super.service(req, resp);
				return;
			}

			resp.flushBuffer();
			logger.httpRequest(req, user, uwsAction, HttpServletResponse.SC_OK, "[OK]", null);

		}catch(UWSException ex){
			sendError(ex, req, user, uwsAction, resp);
		}catch(ClientAbortException cae){
			logger.info("Request aborted by the user !");
			logger.httpRequest(req, user, uwsAction, HttpServletResponse.SC_OK, "[Client abort => ClientAbortException]", null);
		}catch(Throwable t){
			logger.error("Request unexpectedly aborted !", t);
			logger.httpRequest(req, user, uwsAction, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage(), t);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
		}
	}

	/* *********** */
	/* UWS ACTIONS */
	/* *********** */
	protected void writeHomePage(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		UWSSerializer serializer = getSerializer(req.getHeader("Accept"));
		resp.setContentType(serializer.getMimeType());
		String serialization = serializer.getUWS(this);
		if (serialization != null){
			PrintWriter output = resp.getWriter();
			output.print(serialization);
			output.flush();
		}else
			throw UWSExceptionFactory.incorrectSerialization(serialization, "the UWS " + getName());
	}

	protected void doListJob(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobList(requestUrl.getJobListName());

		// Write the jobs list:
		UWSSerializer serializer = getSerializer(req.getHeader("Accept"));
		resp.setContentType(serializer.getMimeType());
		jobsList.serialize(resp.getOutputStream(), serializer, user);
	}

	protected void doAddJob(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobList(requestUrl.getJobListName());

		// Forbids the job creation if the user has not the WRITE permission for the specified jobs list:
		if (user != null && !user.hasWritePermission(jobsList))
			throw UWSExceptionFactory.writePermissionDenied(user, true, jobsList.getName());

		// Create the job:
		UWSJob newJob = createJob(req, user);

		// Add it to the jobs list:
		if (jobsList.addNewJob(newJob) != null){
			// Make a redirection to the added job:
			redirect(requestUrl.jobSummary(jobsList.getName(), newJob.getJobId()).getRequestURL(), req, user, UWSAction.ADD_JOB, resp);
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Unable to add the new job " + newJob.getJobId() + " to the jobs list " + jobsList.getName() + ". (ID already used = " + (jobsList.getJob(newJob.getJobId()) != null) + ")");
	}

	protected void doDestroyJob(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobList(requestUrl.getJobListName());

		// Destroy the job:
		jobsList.destroyJob(requestUrl.getJobId(), user);

		// Make a redirection to the jobs list:
		redirect(requestUrl.listJobs(jobsList.getName()).getRequestURL(), req, user, UWSAction.DESTROY_JOB, resp);
	}

	protected void doJobSummary(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the job:
		UWSJob job = getJob(requestUrl);

		// Write the job summary:
		UWSSerializer serializer = getSerializer(req.getHeader("Accept"));
		resp.setContentType(serializer.getMimeType());
		job.serialize(resp.getOutputStream(), serializer, user);
	}

	protected void doGetJobParam(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the job:
		UWSJob job = getJob(requestUrl, user);

		String[] attributes = requestUrl.getAttributes();

		// RESULT CASE: Display the content of the selected result:
		if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_RESULTS) && attributes.length > 1){
			Result result = job.getResult(attributes[1]);
			if (result == null)
				throw UWSExceptionFactory.incorrectJobResult(job.getJobId(), attributes[1]);
			else if (result.getHref() != null && !result.getHref().trim().isEmpty() && !result.getHref().equalsIgnoreCase(req.getRequestURL().toString()))
				redirect(result.getHref(), req, user, UWSAction.GET_JOB_PARAM, resp);
			else{
				InputStream input = null;
				try{
					input = getFileManager().getResultInput(result, job);
					UWSToolBox.write(input, result.getMimeType(), result.getSize(), resp);
				}catch(IOException ioe){
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Can not read the content of the result " + result.getId() + " (job ID: " + job.getJobId() + ").");
				}finally{
					if (input != null)
						input.close();
				}
			}
		}// ERROR DETAILS CASE: Display the full stack trace of the error:
		else if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_ERROR_SUMMARY) && attributes.length > 1 && attributes[1].equalsIgnoreCase("details")){
			ErrorSummary error = job.getErrorSummary();
			if (error == null)
				throw UWSExceptionFactory.noErrorSummary(job.getJobId());
			else{
				InputStream input = null;
				try{
					input = getFileManager().getErrorInput(error, job);
					UWSToolBox.write(input, "text/plain", getFileManager().getErrorSize(error, job), resp);
				}catch(IOException ioe){
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Can not read the error details (job ID: " + job.getJobId() + ").");
				}finally{
					if (input != null)
						input.close();
				}
			}
		}// DEFAULT CASE: Display the serialization of the selected UWS object:
		else{
			// Write the value/content of the selected attribute:
			UWSSerializer serializer = getSerializer(req.getHeader("Accept"));
			String uwsField = attributes[0];
			if (uwsField == null || uwsField.trim().isEmpty() || (attributes.length <= 1 && (uwsField.equalsIgnoreCase(UWSJob.PARAM_ERROR_SUMMARY) || uwsField.equalsIgnoreCase(UWSJob.PARAM_RESULTS) || uwsField.equalsIgnoreCase(UWSJob.PARAM_PARAMETERS))))
				resp.setContentType(serializer.getMimeType());
			else
				resp.setContentType("text/plain");
			job.serialize(resp.getOutputStream(), attributes, serializer);
		}
	}

	protected void doSetJobParam(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the job:
		UWSJob job = getJob(requestUrl);

		UWSParameters params = getFactory().createUWSParameters(req);

		// Update the job parameters:
		job.addOrUpdateParameters(params, user);

		// Make a redirection to the job:
		redirect(requestUrl.jobSummary(requestUrl.getJobListName(), job.getJobId()).getRequestURL(), req, user, UWSAction.SET_JOB_PARAM, resp);
	}

	public UWSJob getJob(UWSUrl requestUrl) throws UWSException{
		return getJob(requestUrl, null);
	}

	public UWSJob getJob(UWSUrl requestUrl, JobOwner user) throws UWSException{
		// Get the jobs list:
		JobList jobsList = getJobList(requestUrl.getJobListName());

		// Get the job ID:
		String jobId = requestUrl.getJobId();

		if (jobId == null)
			throw UWSExceptionFactory.missingJobID();

		// Get the job:
		UWSJob job = jobsList.getJob(jobId, user);
		if (job == null)
			throw UWSExceptionFactory.incorrectJobID(jobsList.getName(), jobId);

		return job;
	}

	/* ************ */
	/* JOB CREATION */
	/* ************ */
	@Override
	public final UWSFactory getFactory(){
		return this;
	}

	@Override
	public UWSJob createJob(HttpServletRequest request, JobOwner user) throws UWSException{
		return new UWSJob(user, createUWSParameters(request));
	}

	@Override
	public UWSJob createJob(final String jobID, final JobOwner owner, final UWSParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws UWSException{
		return new UWSJob(jobID, owner, params, quote, startTime, endTime, results, error);
	}

	@Override
	public UWSParameters createUWSParameters(final Map<String,Object> params) throws UWSException{
		return new UWSParameters(params, expectedAdditionalParams, inputParamControllers);
	}

	@Override
	public UWSParameters createUWSParameters(final HttpServletRequest req) throws UWSException{
		return new UWSParameters(req, expectedAdditionalParams, inputParamControllers);
	}

	/* ****************************** */
	/* REDIRECTION & ERROR MANAGEMENT */
	/* ****************************** */
	/**
	 * <p>Sends a redirection (with the HTTP status code 303) to the given URL/URI into the given response.</p>
	 * 
	 * @param url		The redirection URL/URI.
	 * @param request	The {@link HttpServletRequest} which may be used to make a redirection.
	 * @param response	The {@link HttpServletResponse} which must contain all information to make a redirection.
	 * 
	 * @throws IOException	If there is an error during the redirection.
	 * @throws UWSException	If there is any other error.
	 */
	public void redirect(String url, HttpServletRequest request, JobOwner user, String uwsAction, HttpServletResponse response) throws ServletException, IOException{
		response.setStatus(HttpServletResponse.SC_SEE_OTHER);
		response.setContentType(request.getContentType());
		response.setHeader("Location", url);
		response.flushBuffer();
		logger.httpRequest(request, user, uwsAction, HttpServletResponse.SC_SEE_OTHER, "[Redirection toward " + url + "]", null);
	}

	/**
	 * <p>
	 * 	Fills the response with the given error. The HTTP status code is set in function of the error code of the given UWSException.
	 * 	If the error code is {@link UWSException#SEE_OTHER} this method calls {@link #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)}.
	 * 	Otherwise the function {@link HttpServletResponse#sendError(int, String)} is called.
	 * </p>
	 * 
	 * @param error			The error to send/display.
	 * @param request		The request which has caused the given error <i>(not used by default)</i>.
	 * @param response		The response in which the error must be published.
	 * 
	 * @throws IOException	If there is an error when calling {@link #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)} or {@link HttpServletResponse#sendError(int, String)}.
	 * @throws UWSException	If there is an error when calling {@link #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)}.
	 * 
	 * @see #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)
	 * @see #writeError(Throwable, HttpServletResponse, HttpServletRequest, JobOwner, String)
	 */
	public void sendError(UWSException error, HttpServletRequest request, JobOwner user, String uwsAction, HttpServletResponse response) throws ServletException, IOException{
		if (error.getHttpErrorCode() == UWSException.SEE_OTHER)
			redirect(error.getMessage(), request, user, uwsAction, response);
		else{
			errorWriter.writeError(error, response, request, user, uwsAction);
		}
	}

	/* ************** */
	/* LOG MANAGEMENT */
	/* ************** */
	@Override
	public UWSLog getLogger(){
		return logger;
	}

	/**
	 * Gets the object used to write/format any error in a HttpServletResponse.
	 * 
	 * @return The error writer/formatter.
	 */
	public final ServiceErrorWriter getErrorWriter(){
		return errorWriter;
	}

	/**
	 * <p>Sets the object used to write/format any error in a HttpServletResponse.</p>
	 * 
	 * <p><i><u>Note:</u> Nothing is done if the given writer is NULL !</i></p>
	 * 
	 * @param errorWriter The new error writer/formatter.
	 */
	public final void setErrorWriter(ServiceErrorWriter errorWriter){
		if (errorWriter != null)
			this.errorWriter = errorWriter;
	}

	/* **************** */
	/* INPUT PARAMETERS */
	/* **************** */

	/**
	 * Adds the name of an additional parameter which must be identified without taking into account its case
	 * and then stored with the case of the given name.
	 * 
	 * @param paramName		Name of an additional parameter.
	 */
	public final void addExpectedAdditionalParameter(final String paramName){
		if (paramName != null && !paramName.trim().isEmpty())
			expectedAdditionalParams.add(paramName);
	}

	/**
	 * Gets the number of additional parameters which must be identified with no case sensitivity.
	 * 
	 * @return	Number of expected additional parameters.
	 */
	public final int getNbExpectedAdditionalParameters(){
		return expectedAdditionalParams.size();
	}

	/**
	 * Gets the names of the expected additional parameters. These parameters are identified with no case sensitivity
	 * and stored in the given case.
	 * 
	 * @return	Names of the expected additional parameters.
	 */
	public final ArrayList<String> getExpectedAdditionalParameters(){
		return expectedAdditionalParams;
	}

	/**
	 * Gets an iterator on the names of the expected additional parameters.
	 * 
	 * @return	An iterator on the names of the expected additional parameters.
	 */
	public final Iterator<String> expectedAdditionalParametersIterator(){
		return expectedAdditionalParams.iterator();
	}

	/**
	 * Removes the name of an expected additional parameter.
	 * This parameter will never be identify specifically and so, it will be stored in the same case as
	 * in the initial Map or HttpServletRequest.
	 * 
	 * @param paramName	Name of an additional parameter.
	 */
	public final void removeExpectedAdditionalParam(final String paramName){
		if (paramName != null && !paramName.trim().isEmpty())
			expectedAdditionalParams.remove(paramName);
	}

	/**
	 * Gets the list of all UWS input parameter controllers.
	 * @return	All parameter controllers.
	 */
	public final Map<String,InputParamController> getInputParamControllers(){
		return inputParamControllers;
	}

	/**
	 * Gets an iterator on the list of all UWS input parameter controllers.
	 * @return	An iterator on all parameter controllers.
	 */
	public final Iterator<Map.Entry<String,InputParamController>> getInputParamControllersIterator(){
		return inputParamControllers.entrySet().iterator();
	}

	/**
	 * Gets the controller of the specified UWS input parameter.
	 * @param inputParamName	Name of the parameter whose the controller must be returned.
	 * @return					The corresponding controller or <i>null</i> if there is none.
	 */
	public final InputParamController getInputParamController(final String inputParamName){
		return (inputParamName == null) ? null : inputParamControllers.get(inputParamName);
	}

	/**
	 * Sets the controller of the specified input UWS job parameter.
	 * 
	 * @param paramName		Name of the parameter with which the given controller will be associated.
	 * @param controller	An input parameter controller.
	 * 
	 * @return				The former controller associated with the specified parameter
	 * 						or <i>null</i> if there is no controller before this call
	 * 						or if the given parameter name is <i>null</i> or an empty string.
	 */
	public final InputParamController setInputParamController(final String paramName, final InputParamController controller){
		if (paramName == null || paramName.trim().isEmpty())
			return null;
		if (controller == null)
			return inputParamControllers.remove(paramName);
		else
			return inputParamControllers.put(paramName, controller);
	}

	/**
	 * Removes the controller of the specified input UWS job parameter.
	 * 
	 * @param paramName	Name of the parameter whose the controller must be removed.
	 * 
	 * @return	The removed controller
	 * 			or <i>null</i> if there were no controller
	 * 			or if the given name is <i>null</i> or an empty string.
	 */
	public final InputParamController removeInputParamController(final String paramName){
		return (paramName == null) ? null : inputParamControllers.remove(paramName);
	}

	/**
	 * <p>Lets configuring the execution duration default and maximum value.</p>
	 * 
	 * <p><i><u>note:</u> A new controller is created if needed.
	 * Otherwise the current one (if it is an instance of {@link DestructionTimeController}) is updated.</i></p>
	 * 
	 * @param defaultDuration	Default duration between the start and the end of the execution of a job.
	 * @param maxDuration		Maximum duration between the start and the end of the execution of a job that a user can set when creating/initializing a job.
	 * @param allowModif		<i>true</i> to allow the modification of this parameter after its initialization, <i>false</i> otherwise.
	 * 
	 * @see ExecutionDurationController
	 */
	public final void configureExecution(final long defaultDuration, final long maxDuration, final boolean allowModif){
		InputParamController controller = inputParamControllers.get(UWSJob.PARAM_EXECUTION_DURATION);

		// Configures the controller:
		if (controller != null && controller instanceof ExecutionDurationController){
			ExecutionDurationController durationController = (ExecutionDurationController)controller;
			durationController.setMaxExecutionDuration(maxDuration);
			durationController.setDefaultExecutionDuration(defaultDuration);
			durationController.allowModification(allowModif);

		}// Or creates a new one, if it does not exist:
		else
			inputParamControllers.put(UWSJob.PARAM_EXECUTION_DURATION, new ExecutionDurationController(defaultDuration, maxDuration, allowModif));
	}

	/**
	 * <p>Lets configuring the destruction time default and maximum value.</p>
	 * 
	 * <p><i><u>note:</u> A new controller is created if needed.
	 * Otherwise the current one (if it is an instance of {@link ExecutionDurationController}) is updated.</i></p>
	 * 
	 * @param defaultTime		Default time since the job creation and its destruction.
	 * @param defaultTimeUnit	Unit of the default time (i.e. minutes, days, ...).
	 * @param maxTime			Maximum time since the job creation and its destruction that a user can set when creating/initializing a job.
	 * @param maxTimeUnit		Unit of the maximum time (i.e. minutes, days, ...).
	 * @param allowModif		<i>true</i> to allow the modification of this parameter after its initialization, <i>false</i> otherwise.
	 * 
	 * @see DestructionTimeController
	 */
	public final void configureDestruction(final int defaultTime, final DateField defaultTimeUnit, final int maxTime, final DateField maxTimeUnit, final boolean allowModif){
		InputParamController controller = inputParamControllers.get(UWSJob.PARAM_DESTRUCTION_TIME);

		// Cast the controller or built a new DestructionTimeController, if it does not exist:
		DestructionTimeController destructionController;
		if (controller == null || !(controller instanceof DestructionTimeController)){
			destructionController = new DestructionTimeController();
			inputParamControllers.put(UWSJob.PARAM_DESTRUCTION_TIME, controller);
		}else
			destructionController = (DestructionTimeController)controller;

		// Configure the controller:
		destructionController.setMaxDestructionInterval(maxTime, maxTimeUnit);
		destructionController.setDefaultDestructionInterval(defaultTime, defaultTimeUnit);
		destructionController.allowModification(allowModif);
	}

	/* ************************* */
	/* GENERAL GETTERS & SETTERS */
	/* ************************* */
	/**
	 * @return The name.
	 */
	public final String getName(){
		return name;
	}

	/**
	 * @return The description.
	 */
	public final String getDescription(){
		return description;
	}

	/* ******************** */
	/* JOBS LIST MANAGEMENT */
	/* ******************** */
	public final Iterator<JobList> iterator(){
		return mapJobLists.values().iterator();
	}

	public JobList getJobList(String name) throws UWSException{
		if (name != null)
			name = name.trim();
		if (name == null || name.length() == 0)
			throw UWSExceptionFactory.missingJobListName();
		else if (!mapJobLists.containsKey(name))
			throw UWSExceptionFactory.incorrectJobListName(name);
		else
			return mapJobLists.get(name);
	}

	public final int getNbJobList(){
		return mapJobLists.size();
	}

	public final boolean addJobList(JobList jl){
		if (jl == null)
			return false;
		else if (mapJobLists.containsKey(jl.getName()))
			return false;

		try{
			jl.setUWS(this);
			mapJobLists.put(jl.getName(), jl);
		}catch(IllegalStateException ise){
			logger.error("The jobs list \"" + jl.getName() + "\" can not be added into the UWS " + getName() + " !", ise);
			return false;
		}

		return true;
	}

	/*public final JobList removeJobList(String name){
		JobList jl = mapJobLists.get(name);
		if (jl != null){
			if (removeJobList(jl))
				return jl;
		}
		return null;
	}*/

	/*
	 * Removes the given jobs list from this UWS.
	 * 
	 * @param jl	The jobs list to remove.
	 * 
	 * @return		<i>true</i> if the jobs list has been successfully removed, <i>false</i> otherwise.
	 * 
	 * @see JobList#removeAll()
	 * @see JobList#setUWS(AbstractUWS)
	 *
	public boolean removeJobList(JobList jl){
		if (jl == null)
			return false;

		jl = mapJobLists.remove(jl.getName());
		if (jl != null){
			jl.removeAll();
			jl.setUWS(null);
		}
		return jl != null;
	}*/

	public final boolean destroyJobList(String name){
		return destroyJobList(mapJobLists.get(name));
	}

	/**
	 * Destroys the given jobs list.
	 * 
	 * @param jl	The jobs list to destroy.
	 * 
	 * @return	<i>true</i> if the given jobs list has been destroyed, <i>false</i> otherwise.
	 * 
	 * @see JobList#clear()
	 * @see JobList#setUWS(AbstractUWS)
	 */
	public boolean destroyJobList(JobList jl){
		if (jl == null)
			return false;

		jl = mapJobLists.remove(jl.getName());
		if (jl != null){
			try{
				jl.clear();
				jl.setUWS(null);
			}catch(IllegalStateException ise){
				getLogger().warning("Impossible to erase completely the association between the jobs list \"" + jl.getName() + "\" and the UWS \"" + getName() + "\", because: " + ise.getMessage());
			}
		}
		return jl != null;
	}

	/* **************************** */
	/* JOB SERIALIZATION MANAGEMENT */
	/* **************************** */
	/**
	 * <p>Adds a serializer to this UWS</p>
	 * <p><b><u>WARNING:</u> If there is already a serializer with the same MIME type (see {@link UWSSerializer#getMimeType()}) in this UWS ,
	 * it should be replaced by the given one !</b></p>
	 * 
	 * @param serializer	The serializer to add.
	 * @return				<i>true</i> if the serializer has been successfully added, <i>false</i> otherwise.
	 */
	public final boolean addSerializer(UWSSerializer serializer){
		if (serializer != null){
			serializers.put(serializer.getMimeType(), serializer);
			if (serializers.size() == 1)
				defaultSerializer = serializer.getMimeType();
			return true;
		}
		return false;
	}

	/**
	 * <p>Gets the serializer whose the MIME type is the same as the given one.</p>
	 * 
	 * <p><i><u>Note:</u> If this UWS has no corresponding serializer, its default one will be returned !</i></p>
	 * 
	 * @param mimeTypes		The MIME type of the searched serializer (may be more than one MIME types
	 * 						- comma separated ; see the format of the Accept header of a HTTP-Request).
	 * 
	 * @return				The corresponding serializer
	 * 						or the default serializer of this UWS if no corresponding serializer has been found.
	 * 
	 * @throws UWSException	If there is no corresponding serializer AND if the default serializer of this UWS can not be found.
	 * 
	 * @see AcceptHeader#AcceptHeader(String)
	 * @see AcceptHeader#getOrderedMimeTypes()
	 */
	public final UWSSerializer getSerializer(String mimeTypes) throws UWSException{
		UWSSerializer choosenSerializer = null;

		if (mimeTypes != null){
			// Parse the given MIME types list:
			AcceptHeader accept = new AcceptHeader(mimeTypes);
			ArrayList<String> lstMimeTypes = accept.getOrderedMimeTypes();

			// Try each of them and stop at the first which match with an existing serializer:
			for(int i = 0; choosenSerializer == null && i < lstMimeTypes.size(); i++)
				choosenSerializer = serializers.get(lstMimeTypes.get(i));
		}

		// If no serializer has been found for each given mime type, return the default one:
		if (choosenSerializer == null){
			choosenSerializer = serializers.get(defaultSerializer);
			if (choosenSerializer == null)
				throw UWSExceptionFactory.missingSerializer(mimeTypes + " (given MIME types) and " + defaultSerializer + " (default serializer MIME type)");
		}

		return choosenSerializer;
	}

	/**
	 * Removes the serializer whose the MIME type is the same as the given one.
	 * 
	 * @param mimeType	MIME type of the serializer to remove.
	 * @return			The removed serializer
	 * 					or <i>null</i> if no corresponding serializer has been found.
	 */
	public final UWSSerializer removeSerializer(String mimeType){
		return serializers.remove(mimeType);
	}

	/**
	 * Gets the URL of the XSLT style-sheet that the XML serializer of this UWS is using.
	 * 
	 * @return	The used XSLT URL.
	 */
	public final String getXsltURL(){
		XMLSerializer serializer = (XMLSerializer)serializers.get(UWSSerializer.MIME_TYPE_XML);
		if (serializer != null)
			return serializer.getXSLTPath();
		return null;
	}

	/**
	 * Sets the URL of the XSLT style-sheet that the XML serializer of this UWS must use.
	 * 
	 * @param xsltPath	The new XSLT URL.
	 * 
	 * @return			<i>true</i> if the given path/url has been successfully set, <i>false</i> otherwise.
	 */
	public final boolean setXsltURL(String xsltPath){
		XMLSerializer serializer = (XMLSerializer)serializers.get(UWSSerializer.MIME_TYPE_XML);
		if (serializer != null){
			serializer.setXSLTPath(xsltPath);
			return true;
		}
		return false;
	}

	/* *************** */
	/* USER IDENTIFIER */
	/* *************** */
	/**
	 * <p>Gets the object which lets extracting the user ID from a HTTP request.</p>
	 * <p><i><u>note:</u>If the returned user identifier is NULL, no job should have an owner.</i></p>
	 * 
	 * @return	The used UserIdentifier (MAY BE NULL).
	 */
	public final UserIdentifier getUserIdentifier(){
		return userIdentifier;
	}

	/**
	 * Sets the object which lets extracting the use ID from a received request.
	 * 
	 * @param identifier	The UserIdentifier to use (may be <i>null</i>).
	 */
	public final void setUserIdentifier(UserIdentifier identifier){
		this.userIdentifier = identifier;
	}

	/* ******************* */
	/* UWS URL INTERPRETER */
	/* ******************* */
	/**
	 * Gets the UWS URL interpreter of this UWS.
	 * 
	 * @return	Its UWS URL interpreter.
	 */
	public final UWSUrl getUrlInterpreter(){
		return urlInterpreter;
	}

	/**
	 * Sets the UWS URL interpreter to use in this UWS.
	 * 
	 * @param urlInterpreter	Its new UWS URL interpreter (may be <i>null</i>. In this case, it will be created from the next request ; see {@link #executeRequest(HttpServletRequest, HttpServletResponse)}).
	 */
	public final void setUrlInterpreter(UWSUrl urlInterpreter){
		this.urlInterpreter = urlInterpreter;
		if (name == null && urlInterpreter != null)
			name = urlInterpreter.getUWSName();
	}

	/* ************** */
	/* BACKUP MANAGER */
	/* ************** */
	/**
	 * <p>Gets its backup manager.</p>
	 * 
	 * @return Its backup manager.
	 */
	public final UWSBackupManager getBackupManager(){
		return backupManager;
	}

}
