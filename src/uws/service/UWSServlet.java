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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.AcceptHeader;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;
import uws.job.ErrorSummary;
import uws.job.JobList;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.jobInfo.JobInfo;
import uws.job.parameters.DestructionTimeController;
import uws.job.parameters.DestructionTimeController.DateField;
import uws.job.parameters.ExecutionDurationController;
import uws.job.parameters.InputParamController;
import uws.job.parameters.UWSParameters;
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
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.RequestParser;
import uws.service.request.UWSRequestParser;
import uws.service.request.UploadFile;

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
 * 	implemented: {@link #createJob(HttpServletRequest, JobOwner)} and {@link #initUWS()}.
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
 * <p>The below code show an example of usage of this class:</p>
 * <pre>
 * public class MyUWSServlet extends UWSServlet {
 *
 * 	// Initialize the UWS service by creating at least one job list.
 * 	public void initUWS() throws UWSException {
 * 		addJobList(new JobList("jobList"));
 * 	}
 *
 * 	// Create the job process corresponding to the job to execute ; generally, the process identification can be merely done by checking the job list name.
 * 	public JobThread createJobThread(UWSJob job) throws UWSException {
 * 		if (job.getJobList().getName().equals("jobList"))
 * 			return new MyJobThread(job);
 * 		else
 * 			throw new UWSException("Impossible to create a job inside the jobs list \"" + job.getJobList().getName() + "\" !");
 * 	}
 * }
 * </pre>
 * <p>
 * 	The name and the description of the UWS may be specified in the web.xml file as init-param of the servlet:
 * 	<code>name</code> and <code>description</code>. The other way is to directly set the corresponding
 * 	attributes: {@link #name} and {@link #description}.
 * </p>
 *
 * <p><i>Note:
 * 	If any error occurs while the initialization or the creation of a {@link UWSServlet} instance, a {@link ServletException}
 * 	will be thrown with a basic message dedicated to the service users. This basic and non-informative message is
 * 	obviously not intended to the administrator which will be able to get the reason of the failure
 * 	(with a stack trace when available) in the log files.
 * </i></p>
 *
 * <h3>UWS customization</h3>
 * <p>
 * 	As for the classic HTTP servlets, this servlet has one method for each method of the implemented protocol.
 * 	Thus, you have one function for the "add job" action, another one for the "get job list" action, ...
 * 	These functions are:
 * </p>
 * <ul>
 * 	<li>{@link #doAddJob(UWSUrl, HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doDestroyJob(UWSUrl, HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doGetJobParam(UWSUrl, HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doJobSummary(UWSUrl, HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doListJob(UWSUrl, HttpServletRequest, HttpServletResponse, JobOwner)}</li>
 * 	<li>{@link #doSetJobParam(UWSUrl, HttpServletRequest, HttpServletResponse, JobOwner)}</li>
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
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.2 (09/2017)
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

	/** Lets extract all parameters from an HTTP request, whatever is its content-type.
	 * @since 4.1*/
	protected RequestParser requestParser;

	/** Lets writing/formatting any exception/throwable in a HttpServletResponse. */
	protected ServiceErrorWriter errorWriter;

	@Override
	public final void init(ServletConfig config) throws ServletException{
		super.init(config);
	}

	@Override
	public final void init() throws ServletException{
		final String INIT_ERROR_MSG = "UWS initialization ERROR! Contact the administrator of the service to figure out the failure.";

		// Set the general information about this UWS:
		name = getServletConfig().getInitParameter("name");
		description = getServletConfig().getInitParameter("description");

		// Set the file manager to use:
		try{
			fileManager = createFileManager();
			if (fileManager == null)
				throw new ServletException(INIT_ERROR_MSG);
		}catch(UWSException ue){
			throw new ServletException(INIT_ERROR_MSG, ue);
		}

		// Set the logger:
		logger = new DefaultUWSLog(this);
		errorWriter = new DefaultUWSErrorWriter(logger);

		// Set the request parser:
		try{
			requestParser = createRequestParser(fileManager);
		}catch(UWSException ue){
			logger.logUWS(LogLevel.FATAL, null, "INIT", "Can't create a request parser!", ue);
			throw new ServletException(INIT_ERROR_MSG, ue);
		}

		// Initialize the list of jobs:
		mapJobLists = new LinkedHashMap<String,JobList>();

		// Initialize the list of available serializers:
		serializers = new HashMap<String,UWSSerializer>();
		addSerializer(new XMLSerializer());
		addSerializer(new JSONSerializer());

		try{
			// Initialize the service:
			initUWS();

			// Log the successful initialization:
			logger.logUWS(LogLevel.INFO, this, "INIT", "UWS successfully initialized.", null);

		}catch(UWSException ue){
			logger.logUWS(LogLevel.FATAL, null, "INIT", "Can't execute the custom initialization of this UWS service (UWSServlet.initUWS())!", ue);
			throw new ServletException(INIT_ERROR_MSG);
		}
	}

	public abstract void initUWS() throws UWSException;

	@Override
	public void destroy(){
		// Backup all jobs:
		/* Jobs are backuped now so that running jobs are set back to the PENDING phase in the backup.
		 * Indeed, the "stopAll" operation of the ExecutionManager may fail and would set the phase to ERROR
		 * for the wrong reason. */
		if (backupManager != null){
			// save all jobs:
			backupManager.setEnabled(true);
			backupManager.saveAll();
			// stop the automatic backup, if there is one:
			backupManager.setEnabled(false);
		}

		// Stop all jobs and stop watching for the jobs' destruction:
		for(JobList jl : mapJobLists.values()){
			jl.getExecutionManager().stopAll();
			jl.getDestructionManager().stop();
		}

		// Just in case that previous clean "stop"s did not work, try again an interruption for all running threads:
		/* note: timers are not part of this ThreadGroup and so, they won't be affected by this function call. */
		JobThread.tg.interrupt();

		// Log the service is stopped:
		if (logger != null)
			logger.logUWS(LogLevel.INFO, this, "STOP", "UWS Service \"" + getName() + "\" stopped!", null);

		// Default destroy function:
		super.destroy();
	}

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
	public RequestParser getRequestParser(){
		return requestParser;
	}

	@Override
	public final void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException{
		super.service(req, resp);
	}

	protected static String lastRequestID = null;

	protected synchronized String generateRequestID(final HttpServletRequest request){
		String id;
		do{
			id = System.currentTimeMillis() + "";
		}while(lastRequestID != null && lastRequestID.startsWith(id));
		lastRequestID = id;
		return id;
	}

	@Override
	protected final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		String uwsAction = null;
		JobOwner user = null;

		// Generate a unique ID for this request execution (for log purpose only):
		final String reqID = generateRequestID(req);
		req.setAttribute(UWS.REQ_ATTRIBUTE_ID, reqID);

		// Extract all parameters:
		try{
			req.setAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS, requestParser.parse(req));
		}catch(UWSException ue){
			logger.log(LogLevel.WARNING, "REQUEST_PARSER", "Can not extract the HTTP request parameters!", ue);
		}

		// Log the reception of the request:
		logger.logHttp(LogLevel.INFO, req, reqID, null, null);

		try{
			String method = req.getMethod();

			// Create a URL interpreter if needed:
			if (urlInterpreter == null)
				setUrlInterpreter(new UWSUrl(req));

			// Update the UWS URL interpreter:
			UWSUrl requestUrl = new UWSUrl(this.urlInterpreter);
			requestUrl.load(req);

			// Identify the user:
			user = UWSToolBox.getUser(req, userIdentifier);

			// Set the character encoding:
			resp.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

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

				}else
					throw new UWSException(UWSException.NOT_IMPLEMENTED, "Unknown UWS action!");

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

				}// DESTROY JOB:
				else if (requestUrl.hasJobList() && requestUrl.hasJob() && requestUrl.getAttributes().length == 0 && UWSToolBox.hasParameter(UWSJob.PARAM_ACTION, UWSJob.ACTION_DELETE, req, false)){
					uwsAction = UWSAction.DESTROY_JOB;
					doDestroyJob(requestUrl, req, resp, user);

				}// SET JOB's UWS STANDARD PARAMETER
				else if (requestUrl.hasJobList() && requestUrl.hasJob() && requestUrl.getAttributes().length == 1 && requestUrl.getAttributes()[0].toLowerCase().matches(UWSParameters.UWS_RW_PARAMETERS_REGEXP) && UWSToolBox.hasParameter(requestUrl.getAttributes()[0], req, false)){
					uwsAction = UWSAction.SET_UWS_PARAMETER;
					doSetUWSParameter(requestUrl, req, resp, user);

				}// SET JOB PARAMETER:
				else if (requestUrl.hasJobList() && requestUrl.hasJob() && (!requestUrl.hasAttribute() || requestUrl.getAttributes().length == 1 && requestUrl.getAttributes()[0].equalsIgnoreCase(UWSJob.PARAM_PARAMETERS)) && UWSToolBox.getNbParameters(req) > 0){
					uwsAction = UWSAction.SET_JOB_PARAM;
					doSetJobParam(requestUrl, req, resp, user);

				}else
					throw new UWSException(UWSException.NOT_IMPLEMENTED, "Unknown UWS action!");

			}// METHOD PUT:
			else if (method.equals("PUT")){
				// SET JOB PARAMETER:
				if (requestUrl.hasJobList() && requestUrl.hasJob() && requestUrl.getAttributes().length >= 2 && requestUrl.getAttributes()[0].equalsIgnoreCase(UWSJob.PARAM_PARAMETERS)){
					uwsAction = UWSAction.SET_JOB_PARAM;
					if (!UWSToolBox.hasParameter(requestUrl.getAttributes()[1], req, false))
						throw new UWSException(UWSException.BAD_REQUEST, "Wrong parameter name in the PUT request! Expected: " + requestUrl.getAttributes()[1]);
					doSetJobParam(requestUrl, req, resp, user);

				}// SET JOB's UWS STANDARD PARAMETER
				else if (requestUrl.hasJobList() && requestUrl.hasJob() && requestUrl.getAttributes().length == 1 && requestUrl.getAttributes()[0].toLowerCase().matches(UWSParameters.UWS_RW_PARAMETERS_REGEXP) && UWSToolBox.hasParameter(requestUrl.getAttributes()[0], req, false)){
					uwsAction = UWSAction.SET_UWS_PARAMETER;
					doSetUWSParameter(requestUrl, req, resp, user);

				}else
					throw new UWSException(UWSException.NOT_IMPLEMENTED, "Unknown UWS action!");

			}// METHOD DELETE:
			else if (method.equals("DELETE")){
				// DESTROY JOB:
				if (requestUrl.hasJobList() && requestUrl.hasJob() && req.getMethod().equalsIgnoreCase("delete")){
					uwsAction = UWSAction.DESTROY_JOB;
					doDestroyJob(requestUrl, req, resp, user);

				}else
					throw new UWSException(UWSException.NOT_IMPLEMENTED, "Unknown UWS action!");

			}// ELSE ERROR:
			else
				throw new UWSException(UWSException.NOT_IMPLEMENTED, "Unknown UWS action!");

			resp.flushBuffer();

			// Log the successful execution of the action:
			logger.logHttp(LogLevel.INFO, resp, reqID, user, "UWS action \"" + uwsAction + "\" successfully executed.", null);

		}catch(IOException ioe){
			/*
			 *   Any IOException thrown while writing the HTTP response is generally caused by a client abortion (intentional or timeout)
			 * or by a connection closed with the client for another reason.
			 *   Consequently, a such error should not be considered as a real error from the server or the library: the request is
			 * canceled, and so the response is not expected. It is anyway not possible any more to send it (header and/or body) totally
			 * or partially.
			 *   Nothing can solve this error. So the "error" is just reported as a simple information and theoretically the action
			 * executed when this error has been thrown is already stopped.
			 */
			logger.logHttp(LogLevel.INFO, resp, reqID, user, "HTTP request aborted or connection with the client closed => the UWS action \"" + uwsAction + "\" has stopped and the body of the HTTP response can not have been partially or completely written!", null);

		}catch(UWSException ex){
			/*
			 *   Any known/"expected" UWS exception is logged but also returned to the HTTP client in an error document.
			 *   Since the error is known, it is supposed to have already been logged with a full stack trace. Thus, there
			 * is no need to log again its stack trace...just its message is logged.
			 *   Besides, this error may also be just a redirection and not a true error. In such case, the error message
			 * is not logged.
			 */
			sendError(ex, req, reqID, user, uwsAction, resp);

		}catch(IllegalStateException ise){
			/*
			 *   Any IllegalStateException that reaches this point, is supposed coming from a HttpServletResponse operation which
			 * has to reset the response buffer (e.g. resetBuffer(), sendRedirect(), sendError()).
			 *   If this exception happens, the library tried to rewrite the HTTP response body with a message or a result,
			 * while this body has already been partially sent to the client. It is then no longer possible to change its content.
			 *   Consequently, the error is logged as FATAL and a message will be appended at the end of the already submitted response
			 * to alert the HTTP client that an error occurs and the response should not be considered as complete and reliable.
			 */
			// Write the error in the response and return the appropriate HTTP status code:
			errorWriter.writeError(ise, resp, req, reqID, user, uwsAction);
			// Log the error:
			getLogger().logHttp(LogLevel.FATAL, resp, reqID, user, "HTTP response already partially committed => the UWS action \"" + uwsAction + "\" has stopped and the body of the HTTP response can not have been partially or completely written!", (ise.getCause() != null) ? ise.getCause() : ise);

		}catch(Throwable t){
			/*
			 *   Any other error is considered as unexpected if it reaches this point. Consequently, it has not yet been logged.
			 * So its stack trace will be fully logged, and an appropriate message will be returned to the HTTP client. The
			 * returned document should contain not too technical information which would be useless for the user.
			 */
			sendError(t, req, reqID, user, uwsAction, resp);

		}finally{
			// Free resources about uploaded files ; only unused files will be deleted:
			UWSToolBox.deleteUploads(req);
		}
	}

	/* *********** */
	/* UWS ACTIONS */
	/* *********** */
	protected void writeHomePage(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		UWSSerializer serializer = getSerializer(req.getHeader("Accept"));
		resp.setContentType(serializer.getMimeType());
		resp.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);
		String serialization;
		try{
			serialization = serializer.getUWS(this);
		}catch(Exception e){
			if (!(e instanceof UWSException)){
				getLogger().logUWS(LogLevel.ERROR, requestUrl, "SERIALIZE", "Can't display the default home page, due to a serialization error!", e);
				throw new UWSException(UWSException.NO_CONTENT, e, "No home page available for this UWS service!");
			}else
				throw (UWSException)e;
		}
		if (serialization != null){
			PrintWriter output = resp.getWriter();
			output.print(serialization);
			output.flush();
		}else
			throw new UWSException(UWSException.NO_CONTENT, "No home page available for this UWS service.");
	}

	protected void doListJob(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobList(requestUrl.getJobListName());

		// Write the jobs list:
		UWSSerializer serializer = getSerializer(req.getHeader("Accept"));
		resp.setContentType(serializer.getMimeType());
		resp.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);
		try{
			jobsList.serialize(resp.getOutputStream(), serializer, user);
		}catch(Exception e){
			if (!(e instanceof UWSException)){
				getLogger().logUWS(LogLevel.ERROR, requestUrl, "SERIALIZE", "Can not serialize the jobs list \"" + jobsList.getName() + "\"!", e);
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e, "Can not format properly the jobs list \"" + jobsList.getName() + "\"!");
			}else
				throw (UWSException)e;
		}
	}

	protected void doAddJob(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobList(requestUrl.getJobListName());

		// Forbids the job creation if the user has not the WRITE permission for the specified jobs list:
		if (user != null && !user.hasWritePermission(jobsList))
			throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(user, true, jobsList.getName()));

		// Create the job:
		UWSJob newJob = createJob(req, user);

		// Add it to the jobs list:
		if (jobsList.addNewJob(newJob) != null){
			// Start the job if the phase parameter was provided with the "RUN" value:
			if (UWSToolBox.hasParameter(UWSJob.PARAM_PHASE, UWSJob.PHASE_RUN, req, false))
				newJob.start();
			// Make a redirection to the added job:
			redirect(requestUrl.jobSummary(jobsList.getName(), newJob.getJobId()).getRequestURL(), req, user, UWSAction.ADD_JOB, resp);
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Unable to add the new job " + newJob.getJobId() + " to the jobs list " + jobsList.getName() + ". (ID already used = " + (jobsList.getJob(newJob.getJobId()) != null) + ")");
	}

	protected void doSetUWSParameter(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the job:
		UWSJob job = getJob(requestUrl);

		// Forbids the action if the user has not the WRITE permission for the specified job:
		if (user != null && !user.hasWritePermission(job))
			throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(user, true, job.getJobId()));

		String name = requestUrl.getAttributes()[0];
		job.addOrUpdateParameter(name, UWSToolBox.getParameter(name, req, false), user);

		// Make a redirection to the job:
		redirect(requestUrl.jobSummary(requestUrl.getJobListName(), job.getJobId()).getRequestURL(), req, user, getName(), resp);
	}

	protected void doDestroyJob(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the jobs list:
		JobList jobsList = getJobList(requestUrl.getJobListName());

		// Destroy the job:
		try{
			jobsList.destroyJob(requestUrl.getJobId(), user);
		}catch(UWSException ue){
			getLogger().logUWS(LogLevel.ERROR, requestUrl, "DESTROY_JOB", "Can not destroy the job \"" + requestUrl.getJobId() + "\"!", ue);
			throw ue;
		}

		// Make a redirection to the jobs list:
		redirect(requestUrl.listJobs(jobsList.getName()).getRequestURL(), req, user, UWSAction.DESTROY_JOB, resp);
	}

	protected void doJobSummary(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the job:
		UWSJob job = getJob(requestUrl);

		// Write the job summary:
		UWSSerializer serializer = getSerializer(req.getHeader("Accept"));
		resp.setContentType(serializer.getMimeType());
		resp.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);
		try{
			job.serialize(resp.getOutputStream(), serializer, user);
		}catch(Exception e){
			if (!(e instanceof UWSException)){
				getLogger().logUWS(LogLevel.ERROR, requestUrl, "SERIALIZE", "Can not serialize the job \"" + job.getJobId() + "\"!", e);
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e, "Can not format properly the job \"" + job.getJobId() + "\"!");
			}else
				throw (UWSException)e;
		}
	}

	protected void doGetJobParam(UWSUrl requestUrl, HttpServletRequest req, HttpServletResponse resp, JobOwner user) throws UWSException, ServletException, IOException{
		// Get the job:
		UWSJob job = getJob(requestUrl, user);

		String[] attributes = requestUrl.getAttributes();

		// RESULT CASE: Display the content of the selected result:
		if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_RESULTS) && attributes.length > 1){
			Result result = job.getResult(attributes[1]);
			if (result == null)
				throw new UWSException(UWSException.NOT_FOUND, "No result identified with \"" + attributes[1] + "\" in the job \"" + job.getJobId() + "\"!");
			else if (result.getHref() != null && !result.getHref().trim().isEmpty() && !result.getHref().equalsIgnoreCase(req.getRequestURL().toString()))
				redirect(result.getHref(), req, user, UWSAction.GET_JOB_PARAM, resp);
			else{
				InputStream input = null;
				try{
					input = getFileManager().getResultInput(result, job);
					UWSToolBox.write(input, result.getMimeType(), result.getSize(), resp);
				}catch(IOException ioe){
					getLogger().logUWS(LogLevel.ERROR, result, "GET_RESULT", "Can not read the content of the result \"" + result.getId() + "\" of the job \"" + job.getJobId() + "\"!", ioe);
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Can not read the content of the result " + result.getId() + " (job ID: " + job.getJobId() + ").");
				}finally{
					if (input != null)
						input.close();
				}
			}
		}
		// ERROR DETAILS CASE: Display the full stack trace of the error:
		else if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_ERROR_SUMMARY) && attributes.length > 1 && attributes[1].equalsIgnoreCase("details")){
			ErrorSummary error = job.getErrorSummary();
			if (error == null)
				throw new UWSException(UWSException.NOT_FOUND, "No error summary for the job \"" + job.getJobId() + "\"!");
			else{
				InputStream input = null;
				try{
					input = getFileManager().getErrorInput(error, job);
					UWSToolBox.write(input, errorWriter.getErrorDetailsMIMEType(), getFileManager().getErrorSize(error, job), resp);
				}catch(IOException ioe){
					getLogger().logUWS(LogLevel.ERROR, error, "GET_ERROR", "Can not read the details of the error summary of the job \"" + job.getJobId() + "\"!", ioe);
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Can not read the error details (job ID: " + job.getJobId() + ").");
				}finally{
					if (input != null)
						input.close();
				}
			}
		}
		// JOB INFO: Display the content of the JobInfo field (if any):
		else if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_JOB_INFO)){

			if (job.getJobInfo() == null)
				resp.sendError(HttpServletResponse.SC_NO_CONTENT);
			else
				job.getJobInfo().write(resp);
		}
		// REFERENCE FILE: Display the content of the uploaded file or redirect to the URL (if it is a URL):
		else if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_PARAMETERS) && attributes.length > 1 && job.getAdditionalParameterValue(attributes[1]) != null && job.getAdditionalParameterValue(attributes[1]) instanceof UploadFile){
			UploadFile upl = (UploadFile)job.getAdditionalParameterValue(attributes[1]);
			if (upl.getLocation().matches("^http(s)?://"))
				redirect(upl.getLocation(), req, user, getName(), resp);
			else{
				InputStream input = null;
				try{
					input = getFileManager().getUploadInput(upl);
					UWSToolBox.write(input, upl.mimeType, upl.length, resp);
				}catch(IOException ioe){
					getLogger().logUWS(LogLevel.ERROR, upl, "GET_PARAMETER", "Can not read the content of the uploaded file \"" + upl.paramName + "\" of the job \"" + job.getJobId() + "\"!", ioe);
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Can not read the content of the uploaded file " + upl.paramName + " (job ID: " + job.getJobId() + ").");
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
			boolean jobSerialization = false;
			// Set the content type:
			if (uwsField == null || uwsField.trim().isEmpty() || (attributes.length <= 1 && (uwsField.equalsIgnoreCase(UWSJob.PARAM_ERROR_SUMMARY) || uwsField.equalsIgnoreCase(UWSJob.PARAM_RESULTS) || uwsField.equalsIgnoreCase(UWSJob.PARAM_PARAMETERS)))){
				resp.setContentType(serializer.getMimeType());
				jobSerialization = true;
			}else
				resp.setContentType("text/plain");
			// Set the character encoding:
			resp.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);
			// Serialize the selected attribute:
			try{
				job.serialize(resp.getOutputStream(), attributes, serializer);
			}catch(Exception e){
				if (!(e instanceof UWSException)){
					String errorMsgPart = (jobSerialization ? "the job \"" + job.getJobId() + "\"" : "the parameter " + uwsField + " of the job \"" + job.getJobId() + "\"");
					getLogger().logUWS(LogLevel.ERROR, requestUrl, "SERIALIZE", "Can not serialize " + errorMsgPart + "!", e);
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e, "Can not format properly " + errorMsgPart + "!");
				}else
					throw (UWSException)e;
			}
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
		// Get the job ID:
		String jobId = requestUrl.getJobId();
		UWSJob job = null;

		if (jobId != null){
			// Get the jobs list:
			JobList jobsList = getJobList(requestUrl.getJobListName());
			// Get the job:
			job = jobsList.getJob(jobId, user);
			if (job == null)
				throw new UWSException(UWSException.NOT_FOUND, "Incorrect job ID! The job \"" + jobId + "\" does not exist in the jobs list \"" + jobsList.getName() + "\".");
		}else
			throw new UWSException(UWSException.BAD_REQUEST, "Missing job ID!");

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
		// Create the job:
		UWSJob newJob = new UWSJob(user, createUWSParameters(request));

		// Set the XML job description if any:
		Object jobDesc = request.getAttribute(UWS.REQ_ATTRIBUTE_JOB_DESCRIPTION);
		if (jobDesc != null && jobDesc instanceof JobInfo)
			newJob.setJobInfo((JobInfo)jobDesc);

		return newJob;
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

	@Override
	public RequestParser createRequestParser(final UWSFileManager fileManager) throws UWSException{
		return new UWSRequestParser(fileManager);
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
		response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);
		response.setHeader("Location", url);
		response.flushBuffer();
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
	 * @param reqID			ID of the request.
	 * @param user			The user which executes the given request.
	 * @param uwsAction	The UWS action corresponding to the given request.
	 * @param response		The response in which the error must be published.
	 *
	 * @throws IOException	If there is an error when calling {@link #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)} or {@link HttpServletResponse#sendError(int, String)}.
	 * @throws UWSException	If there is an error when calling {@link #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)}.
	 *
	 * @see #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)
	 * @see #sendError(Throwable, HttpServletRequest, String, JobOwner, String, HttpServletResponse)
	 */
	public final void sendError(UWSException error, HttpServletRequest request, String reqID, JobOwner user, String uwsAction, HttpServletResponse response) throws ServletException{
		if (error.getHttpErrorCode() == UWSException.SEE_OTHER){
			// Log the redirection, if any:
			logger.logHttp(LogLevel.INFO, response, reqID, user, "HTTP " + UWSException.SEE_OTHER + " [Redirection toward " + error.getMessage() + "] - Action \"" + uwsAction + "\" successfully executed.", null);
			// Apply the redirection:
			try{
				redirect(error.getMessage(), request, user, uwsAction, response);
			}catch(IOException ioe){
				logger.logHttp(LogLevel.FATAL, request, reqID, "Can not redirect the response toward " + error.getMessage(), error);
				throw new ServletException("Can not redirect the response! You should notify the administrator of the service (FATAL-" + reqID + "). However, while waiting a correction of this problem, you can manually go toward " + error.getMessage() + ".");
			}
		}else
			sendError((Exception)error, request, reqID, user, uwsAction, response);
	}

	/**
	 * <p>
	 * 	Fills the response with the given error.
	 * 	The stack trace of the error is printed on the standard output and then the function
	 * 	{@link HttpServletResponse#sendError(int, String)} is called with the HTTP status code is {@link UWSException#INTERNAL_SERVER_ERROR}
	 * 	and the message of the given exception.
	 * </p>
	 *
	 *
	 * @param error			The error to send/display.
	 * @param request		The request which has caused the given error <i>(not used by default)</i>.
	 * @param reqID			ID of the request.
	 * @param user			The user which executes the given request.
	 * @param uwsAction	The UWS action corresponding to the given request.
	 * @param response		The response in which the error must be published.
	 *
	 * @throws IOException	If there is an error when calling {@link HttpServletResponse#sendError(int, String)}.
	 * @throws UWSException
	 *
	 * @see ServiceErrorWriter#writeError(Throwable, HttpServletResponse, HttpServletRequest, String, JobOwner, String)
	 */
	public final void sendError(Throwable error, HttpServletRequest request, String reqID, JobOwner user, String uwsAction, HttpServletResponse response) throws ServletException{
		// Write the error in the response and return the appropriate HTTP status code:
		errorWriter.writeError(error, response, request, reqID, user, uwsAction);
		// Log the error:
		logger.logHttp(LogLevel.ERROR, response, reqID, user, "Can not complete the UWS action \"" + uwsAction + "\", because: " + error.getMessage(), error);
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
	public final List<String> getExpectedAdditionalParameters(){
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
			inputParamControllers.put(UWSJob.PARAM_DESTRUCTION_TIME, destructionController);
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
	@Override
	public final String getName(){
		return name;
	}

	/**
	 * @return The description.
	 */
	@Override
	public final String getDescription(){
		return description;
	}

	/* ******************** */
	/* JOBS LIST MANAGEMENT */
	/* ******************** */
	@Override
	public final Iterator<JobList> iterator(){
		return mapJobLists.values().iterator();
	}

	@Override
	public JobList getJobList(String name) throws UWSException{
		if (name != null)
			name = name.trim();
		if (name == null || name.length() == 0)
			throw new UWSException(UWSException.BAD_REQUEST, "Missing job list name!");
		else if (!mapJobLists.containsKey(name))
			throw new UWSException(UWSException.NOT_FOUND, "Incorrect job list name ! The jobs list \"" + name + "\" does not exist.");
		else
			return mapJobLists.get(name);
	}

	@Override
	public final int getNbJobList(){
		return mapJobLists.size();
	}

	@Override
	public final boolean addJobList(JobList jl){
		if (jl == null)
			return false;
		else if (mapJobLists.containsKey(jl.getName()))
			return false;

		try{
			jl.setUWS(this);
			mapJobLists.put(jl.getName(), jl);
		}catch(IllegalStateException ise){
			logger.logUWS(LogLevel.ERROR, jl, "ADD_JOB_LIST", "The jobs list \"" + jl.getName() + "\" can not be added into the UWS " + getName() + ": it may already be associated with one!", ise);
			return false;
		}

		return true;
	}

	@Override
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
	 * @see JobList#setUWS(UWS)
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
				logger.logUWS(LogLevel.WARNING, jl, "DESTROY_JOB_LIST", "Impossible to erase completely the association between the jobs list \"" + jl.getName() + "\" and the UWS \"" + getName() + "\"!", ise);
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
	@Override
	public final UWSSerializer getSerializer(String mimeTypes) throws UWSException{
		UWSSerializer choosenSerializer = null;

		if (mimeTypes != null){
			// Parse the given MIME types list:
			AcceptHeader accept = new AcceptHeader(mimeTypes);
			List<String> lstMimeTypes = accept.getOrderedMimeTypes();

			// Try each of them and stop at the first which match with an existing serializer:
			for(int i = 0; choosenSerializer == null && i < lstMimeTypes.size(); i++)
				choosenSerializer = serializers.get(lstMimeTypes.get(i));
		}

		// If no serializer has been found for each given mime type, return the default one:
		if (choosenSerializer == null){
			choosenSerializer = serializers.get(defaultSerializer);
			if (choosenSerializer == null)
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Missing UWS serializer for the MIME types: " + mimeTypes + " (given MIME types) and " + defaultSerializer + " (default serializer MIME type)" + "!");
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
	@Override
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
	@Override
	public final UWSUrl getUrlInterpreter(){
		return urlInterpreter;
	}

	/**
	 * Sets the UWS URL interpreter to use in this UWS.
	 *
	 * @param urlInterpreter	Its new UWS URL interpreter (may be <i>null</i>. In this case, it will be created from the next request ; see {@link #service(HttpServletRequest, HttpServletResponse)}).
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
	@Override
	public final UWSBackupManager getBackupManager(){
		return backupManager;
	}

}
