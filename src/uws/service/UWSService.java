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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.AcceptHeader;
import uws.UWSException;
import uws.UWSToolBox;
import uws.job.JobList;
import uws.job.JobThread;
import uws.job.serializer.JSONSerializer;
import uws.job.serializer.UWSSerializer;
import uws.job.serializer.XMLSerializer;
import uws.job.user.JobOwner;
import uws.service.actions.AddJob;
import uws.service.actions.DestroyJob;
import uws.service.actions.GetJobParam;
import uws.service.actions.JobSummary;
import uws.service.actions.ListJobs;
import uws.service.actions.SetJobParam;
import uws.service.actions.SetUWSParameter;
import uws.service.actions.ShowHomePage;
import uws.service.actions.UWSAction;
import uws.service.backup.UWSBackupManager;
import uws.service.error.DefaultUWSErrorWriter;
import uws.service.error.ServiceErrorWriter;
import uws.service.file.UWSFileManager;
import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.RequestParser;
import uws.service.wait.BlockingPolicy;

/**
 * <p>This class implements directly the interface {@link UWS} and so, it represents the core of a UWS service.</p>
 *
 * <h3>Usage</h3>
 *
 * <p>
 * 	Using this class is very simple! An instance must be created by providing at a factory - {@link UWSFactory} - and a file manager - {@link UWSFileManager}.
 * 	This creation must be done in the init() function of a {@link HttpServlet}. Then, still in init(), at least one job list must be created.
 * 	Finally, in order to ensure that all requests are interpreted by the UWS service, they must be sent to the created {@link UWSService} in the function
 * 	{@link #executeRequest(HttpServletRequest, HttpServletResponse)}.
 * </p>
 * <p>Here is an example of what should look like the servlet class:</p>
 * <pre>
 * public class MyUWSService extends HttpServlet {
 * 	private UWS uws;
 *
 * 	public void init(ServletConfig config) throws ServletException {
 * 		try{
 * 			// Create the UWS service:
 * 			uws = new UWSService(new MyUWSFactory(), new LocalUWSFileManager(new File(config.getServletContext().getRealPath("UWSFiles"))));
 * 			// Create at least one job list (otherwise no job can be started):
 * 			uws.addJobList("jobList");
 * 		}catch(UWSException ue){
 * 			throw new ServletException("Can not initialize the UWS service!", ue);
 * 		}
 * 	}
 *
 * 	public void destroy(){
 *		if (uws != null)
 * 			uws.destroy();
 * 	}
 *
 * 	public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException{
 * 		try{
 *			service.executeRequest(request, response);
 * 		}catch(UWSException ue){
 * 			response.sendError(ue.getHttpErrorCode(), ue.getMessage());
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h3>UWS actions</h3>
 *
 * <p>
 * 	All standard UWS actions are already implemented in this class. However, it is still possible to modify their implementation and/or to
 * 	add or remove some actions.
 * </p>
 * <p>
 * 	A UWS action is actually implemented here by a class extending the abstract class {@link UWSAction}. Here is the full list of all
 * 	the available and already implemented actions:
 * </p>
 * <ul>
 * 	<li>{@link AddJob}</li>
 * 	<li>{@link DestroyJob}</li>
 * 	<li>{@link JobSummary}</li>
 * 	<li>{@link GetJobParam}</li>
 * 	<li>{@link SetJobParam}</li>
 * 	<li>{@link ListJobs}</li>
 * </ul>
 * <p>
 * 	To add an action, you should use the function {@link #addUWSAction(UWSAction)}, to remove one {@link #removeUWSAction(int)} or {@link #removeUWSAction(String)}.
 * 	Note that this last function takes a String parameter. This parameter is the name of the UWS action to remove. Indeed, each UWS action must have an internal
 * 	name representing the action. Thus, it is possible to replace a UWS action implementation by using the function {@link #replaceUWSAction(UWSAction)} ; this
 * 	function will replace the action having the same name as the given action.
 * </p>
 *
 * <h3>Home page</h3>
 *
 * <p>
 * 	In addition of all the actions listed above, a last action is automatically added: {@link ShowHomePage}. This is the action which will display the home page of
 * 	the UWS service. It is called when the root resource of the web service is asked. To change it, you can either overwrite this action
 * 	(see {@link #replaceUWSAction(UWSAction)}) or set an home page URL with the function {@link #setHomePage(String)} <i>(the parameter is a URI pointing on either
 * 	a local or a remote resource)</i> or {@link #setHomePage(URL, boolean)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.4 (08/2018)
 */
public class UWSService implements UWS {

	/** Name of this UWS. */
	protected String name = null;

	/** Description of this UWS. */
	protected String description = null;

	/** List of all managed jobs lists. <i>(it is a LinkedHashMap so that jobs lists are ordered by insertion)</i> */
	protected final Map<String, JobList> mapJobLists;

	/** The "interpreter" of UWS URLs. */
	protected UWSUrl urlInterpreter = null;

	/** List of available serializers. */
	protected final Map<String, UWSSerializer> serializers;

	/** The MIME type of the default serialization format. */
	protected String defaultSerializer = null;

	/** The serializer chosen during the last call of {@link #executeRequest(HttpServletRequest, HttpServletResponse)}. */
	protected UWSSerializer choosenSerializer = null;

	/** URL of the home page. (<i>NULL if there is no home page</i>) */
	protected String homePage = null;

	/** Indicates whether the home page must be a copy or a redirection to the given URL. */
	protected boolean homeRedirection = false;

	/** MIME type of the custom home page. By default, it is "text/html".
	 * @since 4.2 */
	protected String homePageMimeType = "text/html";

	/** List of UWS actions (i.e. to list jobs, to get a job, to set a job parameter, etc...). */
	protected final Vector<UWSAction> uwsActions;

	/** The action executed during the last call of {@link #executeRequest(HttpServletRequest, HttpServletResponse)}. */
	protected UWSAction executedAction = null;

	/** The object to use to extract the user ID from the received request. */
	protected UserIdentifier userIdentifier = null;

	/** Factory which lets creating the UWS jobs and their thread. */
	protected final UWSFactory factory;

	/** Lets managing all UWS files (i.e. log, result, backup, ...). */
	protected final UWSFileManager fileManager;

	/** Lets saving and/or restoring the whole UWS.  */
	protected UWSBackupManager backupManager;

	/** Lets logging info/debug/warnings/errors about this UWS. */
	protected UWSLog logger;

	/** Lets extract all parameters from an HTTP request, whatever is its content-type.
	 * @since 4.1*/
	protected final RequestParser requestParser;

	/** Lets writing/formatting any exception/throwable in a HttpServletResponse. */
	protected ServiceErrorWriter errorWriter;

	/**
	 * Strategy to use for the blocking/wait process concerning the
	 * {@link JobSummary} action.
	 * <p>
	 * 	If NULL, the standard strategy will be used: wait exactly the time asked
	 * 	by the user (or indefinitely if none is specified).
	 * </p>
	 * @since 4.3 */
	protected BlockingPolicy waitPolicy = null;

	/** Last generated request ID. If the next generated request ID is equivalent to this one,
	 * a new one will generate in order to ensure the unicity.
	 * @since 4.1 */
	protected static String lastRequestID = null;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * <p>Builds a UWS (the base URI will be extracted at the first request directly from the request itself).</p>
	 *
	 * <p>
	 * 	By default, this UWS has 2 serialization formats: XML ({@link XMLSerializer}) and JSON ({@link JSONSerializer}).
	 * 	All the default actions of a UWS are also already implemented.
	 * 	However, you still have to create at least one job list !
	 * </p>
	 *
	 * <p><i><u>note:</u> since no logger is provided, a default one is set automatically (see {@link DefaultUWSLog}).</i></p>
	 *
	 * @param jobFactory	Object which lets creating the UWS jobs managed by this UWS and their thread/task.
	 * @param fileManager	Object which lets managing all files managed by this UWS (i.e. log, result, backup, error, ...).
	 *
	 * @throws NullPointerException	If at least one of the parameters is <i>null</i>.
	 * @throws UWSException			If unable to create a request parser using the factory (see {@link UWSFactory#createRequestParser(UWSFileManager)}).
	 *
	 * @see #UWSService(UWSFactory, UWSFileManager, UWSLog)
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager) throws UWSException{
		this(jobFactory, fileManager, (UWSLog)null);
	}

	/**
	 * <p>Builds a UWS (the base URI will be extracted at the first request directly from the request itself).</p>
	 *
	 * <p>
	 * 	By default, this UWS has 2 serialization formats: XML ({@link XMLSerializer}) and JSON ({@link JSONSerializer}).
	 * 	All the default actions of a UWS are also already implemented.
	 * 	However, you still have to create at least one job list !
	 * </p>
	 *
	 * @param jobFactory	Object which lets creating the UWS jobs managed by this UWS and their thread/task.
	 * @param fileManager	Object which lets managing all files managed by this UWS (i.e. log, result, backup, error, ...).
	 * @param logger		Object which lets printing any message (error, info, debug, warning).
	 *
	 * @throws NullPointerException	If at least one of the parameters is <i>null</i>.
	 * @throws UWSException			If unable to create a request parser using the factory (see {@link UWSFactory#createRequestParser(UWSFileManager)}).
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSLog logger) throws UWSException{
		if (jobFactory == null)
			throw new NullPointerException("Missing UWS factory! Can not create a UWSService.");
		factory = jobFactory;

		if (fileManager == null)
			throw new NullPointerException("Missing UWS file manager! Can not create a UWSService.");
		this.fileManager = fileManager;

		this.logger = (logger == null) ? new DefaultUWSLog(this) : logger;

		requestParser = jobFactory.createRequestParser(fileManager);

		errorWriter = new DefaultUWSErrorWriter(this.logger);

		// Initialize the list of jobs:
		mapJobLists = new LinkedHashMap<String, JobList>();

		// Initialize the list of available serializers:
		serializers = new HashMap<String, UWSSerializer>();
		addSerializer(new XMLSerializer());
		addSerializer(new JSONSerializer());

		// Initialize the list of UWS actions:
		uwsActions = new Vector<UWSAction>();

		// Load the default UWS actions:
		uwsActions.add(new ShowHomePage(this));
		uwsActions.add(new ListJobs(this));
		uwsActions.add(new AddJob(this));
		uwsActions.add(new SetUWSParameter(this));
		uwsActions.add(new DestroyJob(this));
		uwsActions.add(new JobSummary(this));
		uwsActions.add(new GetJobParam(this));
		uwsActions.add(new SetJobParam(this));
	}

	/**
	 * <p>Builds a UWS with its base UWS URI.</p>
	 *
	 * <p><i><u>note:</u> since no logger is provided, a default one is set automatically (see {@link DefaultUWSLog}).</i></p>
	 *
	 * @param jobFactory	Object which lets creating the UWS jobs managed by this UWS and their thread/task.
	 * @param fileManager	Object which lets managing all files managed by this UWS (i.e. log, result, backup, error, ...).
	 * @param baseURI		Base UWS URI.
	 *
	 * @throws UWSException	If the given URI is <i>null</i> or empty.
	 *
	 * @see #UWSService(UWSFactory, UWSFileManager, UWSLog, String)
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final String baseURI) throws UWSException{
		this(jobFactory, fileManager, null, baseURI);
	}

	/**
	 * Builds a UWS with its base UWS URI.
	 *
	 * @param jobFactory	Object which lets creating the UWS jobs managed by this UWS and their thread/task.
	 * @param fileManager	Object which lets managing all files managed by this UWS (i.e. log, result, backup, error, ...).
	 * @param logger		Object which lets printing any message (error, info, debug, warning).
	 * @param baseURI		Base UWS URI.
	 *
	 * @throws UWSException	If the given URI is <i>null</i> or empty.
	 *
	 * @see UWSUrl#UWSUrl(String)
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSLog logger, final String baseURI) throws UWSException{
		this(jobFactory, fileManager, logger);

		// Extract the name of the UWS:
		try{
			// Set the URL interpreter:
			urlInterpreter = new UWSUrl(baseURI);

			// ...and the name of this service:
			name = urlInterpreter.getUWSName();

			// Log the successful initialization:
			this.logger.logUWS(LogLevel.INFO, this, "INIT", "UWS successfully initialized!", null);

		}catch(NullPointerException ex){
			// Log the exception:
			// (since the first constructor has already been called successfully, the logger is now NOT NULL):
			logger.logUWS(LogLevel.FATAL, null, "INIT", "Invalid base UWS URI: " + baseURI + "! You should check the configuration of the service.", ex);

			// Throw a new UWSException with a more understandable message:
			throw new UWSException(UWSException.BAD_REQUEST, ex, "Invalid base UWS URI (" + baseURI + ")!");
		}
	}

	/**
	 * <p>Builds a UWS with the given UWS URL interpreter.</p>
	 *
	 * <p><i><u>note:</u> since no logger is provided, a default one is set automatically (see {@link DefaultUWSLog}).</i></p>
	 *
	 * @param jobFactory	Object which lets creating the UWS jobs managed by this UWS and their thread/task.
	 * @param fileManager	Object which lets managing all files managed by this UWS (i.e. log, result, backup, error, ...).
	 * @param urlInterpreter	The UWS URL interpreter to use in this UWS.
	 *
	 * @throws UWSException			If unable to create a request parser using the factory (see {@link UWSFactory#createRequestParser(UWSFileManager)}).
	 *
	 * @see #UWSService(UWSFactory, UWSFileManager, UWSLog, UWSUrl)
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSUrl urlInterpreter) throws UWSException{
		this(jobFactory, fileManager, null, urlInterpreter);
	}

	/**
	 * Builds a UWS with the given UWS URL interpreter.
	 *
	 * @param jobFactory	Object which lets creating the UWS jobs managed by this UWS and their thread/task.
	 * @param fileManager	Object which lets managing all files managed by this UWS (i.e. log, result, backup, error, ...).
	 * @param logger		Object which lets printing any message (error, info, debug, warning).
	 * @param urlInterpreter	The UWS URL interpreter to use in this UWS.
	 *
	 * @throws UWSException			If unable to create a request parser using the factory (see {@link UWSFactory#createRequestParser(UWSFileManager)}).
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSLog logger, final UWSUrl urlInterpreter) throws UWSException{
		this(jobFactory, fileManager, logger);
		setUrlInterpreter(urlInterpreter);
		if (this.urlInterpreter != null)
			this.logger.logUWS(LogLevel.INFO, this, "INIT", "UWS successfully initialized.", null);
	}

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

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	@Override
	public final String getName(){
		return name;
	}

	/**
	 * Sets the name of this UWS.
	 *
	 * @param name	Its new name.
	 */
	public final void setName(String name){
		this.name = name;
	}

	@Override
	public final String getDescription(){
		return description;
	}

	/**
	 * Sets the description of this UWS.
	 *
	 * @param description	Its new description.
	 */
	public final void setDescription(String description){
		this.description = description;
	}

	/**
	 * Gets the base UWS URL.
	 *
	 * @return	The base UWS URL.
	 *
	 * @see UWSUrl#getBaseURI()
	 */
	public final String getBaseURI(){
		return (urlInterpreter == null) ? null : urlInterpreter.getBaseURI();
	}

	@Override
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
		if (this.urlInterpreter != null)
			this.urlInterpreter.setUwsURI(null);
	}

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
		userIdentifier = identifier;
	}

	@Override
	public final UWSFactory getFactory(){
		return factory;
	}

	@Override
	public final UWSFileManager getFileManager(){
		return fileManager;
	}

	@Override
	public final UWSBackupManager getBackupManager(){
		return backupManager;
	}

	/**
	 * <p>
	 * 	Sets its backup manager.
	 * 	This manager will be called at each user action to save only its own jobs list by calling {@link UWSBackupManager#saveOwner(JobOwner)}.
	 * </p>
	 *
	 * @param backupManager Its new backup manager.
	 */
	public final void setBackupManager(final UWSBackupManager backupManager){
		this.backupManager = backupManager;
	}

	@Override
	public final RequestParser getRequestParser(){
		return requestParser;
	}

	/**
	 * Get the currently used strategy for the blocking behavior of the
	 * Job Summary action.
	 *
	 * <p>
	 * 	This strategy lets decide how long a WAIT request must block a HTTP
	 * 	request. With a such policy, the waiting time specified by the user may
	 * 	be modified.
	 * </p>
	 *
	 * @return	The WAIT strategy,
	 *        	or NULL if the default one (i.e. wait the time specified by the
	 *        	user) is used.
	 *
	 * @since 4.3
	 */
	public final BlockingPolicy getWaitPolicy(){
		return waitPolicy;
	}

	/**
	 * Set the strategy to use for the blocking behavior of the
	 * Job Summary action.
	 *
	 * <p>
	 * 	This strategy lets decide whether a WAIT request must block a HTTP
	 * 	request and how long. With a such policy, the waiting time specified by
	 * 	the user may be modified.
	 * </p>
	 *
	 * @param waitPolicy	The WAIT strategy to use,
	 *                  	or NULL if the default one (i.e. wait the time
	 *                  	specified by the user ;
	 *                  	if no time is specified the HTTP request may be
	 *                  	blocked indefinitely) must be used.
	 *
	 * @since 4.3
	 */
	public final void setWaitPolicy(final BlockingPolicy waitPolicy){
		this.waitPolicy = waitPolicy;
	}

	/* ******************** */
	/* HOME PAGE MANAGEMENT */
	/* ******************** */
	/**
	 * Gets the URL of the resource which must be used as home page of this UWS.
	 *
	 * @return	The URL of the home page.
	 */
	public final String getHomePage(){
		return homePage;
	}

	/**
	 * Tells whether a redirection to the specified home page must be done or not.
	 *
	 * @return	<i>true</i> if a redirection to the specified resource must be done
	 * 			or <i>false</i> to copy it.
	 */
	public final boolean isHomePageRedirection(){
		return homeRedirection;
	}

	/**
	 * Sets the URL of the resource which must be used as home page of this UWS.
	 *
	 * @param homePageUrl	The URL of the home page (may be <i>null</i>).
	 * @param redirect		<i>true</i> if a redirection to the specified resource must be done
	 * 						or <i>false</i> to copy it.
	 */
	public final void setHomePage(URL homePageUrl, boolean redirect){
		homePage = homePageUrl.toString();
		homeRedirection = redirect;
	}

	/**
	 * <p>Sets the URI of the resource which must be used as home page of this UWS.</p>
	 * <i>A redirection will always be done on the specified resource.</i>
	 *
	 * @param homePageURI	The URL of the home page.
	 */
	public final void setHomePage(String homePageURI){
		homePage = homePageURI;
		homeRedirection = true;
	}

	/**
	 * Indicates whether the current home page is the default one (the UWS serialization)
	 * or if it has been specified manually using {@link UWSService#setHomePage(URL, boolean)}.
	 *
	 * @return	<i>true</i> if it is the default home page, <i>false</i> otherwise.
	 */
	public final boolean isDefaultHomePage(){
		return homePage == null;
	}

	/**
	 * Forgets the home page specified by using {@link UWSService#setHomePage(URL, boolean)} - if any -
	 * and go back to the default home page (XML format).
	 */
	public final void setDefaultHomePage(){
		homePage = null;
		homeRedirection = false;
	}

	/**
	 * <p>Get the MIME type of the custom home page.</p>
	 *
	 * <p>By default, it is the same as the default home page: "text/html".</p>
	 *
	 * <p><i>Note:
	 * 	This function has a sense only if the HOME PAGE resource of this UWS service
	 * 	is still the default home page (i.e. {@link ShowHomePage}).
	 * </i></p>
	 *
	 * @return	MIME type of the custom home page.
	 *
	 * @since 4.2
	 */
	public final String getHomePageMimeType(){
		return homePageMimeType;
	}

	/**
	 * <p>Set the MIME type of the custom home page.</p>
	 *
	 * <p>A NULL value will be considered as "text/html".</p>
	 *
	 * <p><i>Note:
	 * 	This function has a sense only if the HOME PAGE resource of this UWS service
	 * 	is still the default home page (i.e. {@link ShowHomePage}).
	 * </i></p>
	 *
	 * @param mime	MIME type of the custom home page.
	 *
	 * @since 4.2
	 */
	public final void setHomePageMimeType(final String mime){
		homePageMimeType = (mime == null || mime.trim().length() == 0) ? "text/html" : mime.trim();
	}

	/* ********************** */
	/* SERIALIZERS MANAGEMENT */
	/* ********************** */
	/**
	 * Gets the MIME type of the serializer to use by default.
	 *
	 * @return	The MIME type of the default serializer.
	 */
	public final String getDefaultSerializer(){
		return defaultSerializer;
	}

	/**
	 * Sets the MIME of the serializer to use by default.
	 *
	 * @param mimeType		The MIME type (only one).
	 *
	 * @throws UWSException If there is no serializer with this MIME type available in this UWS.
	 */
	public final void setDefaultSerializer(String mimeType) throws UWSException{
		if (serializers.containsKey(mimeType))
			defaultSerializer = mimeType;
		else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Missing UWS serializer for the MIME types: " + mimeType + "! The default serializer won't be set.");
	}

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
	 * Tells whether this UWS has already a serializer with the given MIME type.
	 *
	 * @param mimeType	A MIME type (only one).
	 *
	 * @return			<i>true</i> if a serializer exists with the given MIME type, <i>false</i> otherwise.
	 */
	public final boolean hasSerializerFor(String mimeType){
		return serializers.containsKey(mimeType);
	}

	/**
	 * Gets the total number of serializers available in this UWS.
	 *
	 * @return	The number of its serializers.
	 */
	public final int getNbSerializers(){
		return serializers.size();
	}

	/**
	 * Gets an iterator of the list of all serializers available in this UWS.
	 *
	 * @return	An iterator on its serializers.
	 */
	public final Iterator<UWSSerializer> getSerializers(){
		return serializers.values().iterator();
	}

	@Override
	public final UWSSerializer getSerializer(String mimeTypes) throws UWSException{
		choosenSerializer = null;

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
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "No UWS Serializer available neither for \"" + mimeTypes + "\" (given MIME types) nor \"" + defaultSerializer + "\" (default serializer MIME type) !");
		}

		return choosenSerializer;
	}

	/**
	 * Gets the serializer choosen during the last call of {@link #getSerializer(String)}.
	 *
	 * @return	The last used serializer.
	 */
	public final UWSSerializer getChoosenSerializer(){
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

	/* ********************* */
	/* JOBS LISTS MANAGEMENT */
	/* ********************* */
	/**
	 * An iterator on the jobs lists list.
	 *
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public final Iterator<JobList> iterator(){
		return mapJobLists.values().iterator();
	}

	@Override
	public final JobList getJobList(String name){
		return mapJobLists.get(name);
	}

	@Override
	public final int getNbJobList(){
		return mapJobLists.size();
	}

	/**
	 * Adds a jobs list to this UWS.
	 *
	 * @param jl	The jobs list to add.
	 *
	 * @return		<i>true</i> if the jobs list has been successfully added,
	 * 				<i>false</i> if the given jobs list is <i>null</i> or if a jobs list with this name already exists
	 * 				or if a UWS is already associated with another UWS.
	 *
	 * @see JobList#setUWS(UWS)
	 * @see UWS#addJobList(JobList)
	 */
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

	/**
	 * Destroys all managed jobs lists.
	 *
	 * @see #destroyJobList(String)
	 */
	public final void destroyAllJobLists(){
		ArrayList<String> jlNames = new ArrayList<String>(mapJobLists.keySet());
		for(String jlName : jlNames)
			destroyJobList(jlName);
	}

	/* ********************** */
	/* UWS ACTIONS MANAGEMENT */
	/* ********************** */
	/**
	 * <p>Lets adding the given action to this UWS.</p>
	 *
	 * <p><b><u>WARNING:</u> The action will be added at the end of the actions list of this UWS. That means, it will be evaluated (call of
	 * the method {@link UWSAction#match(UWSUrl, JobOwner, HttpServletRequest)}) lastly !</b></p>
	 *
	 * @param action	The UWS action to add.
	 *
	 * @return			<i>true</i> if the given action has been successfully added, <i>false</i> otherwise.
	 */
	public final boolean addUWSAction(UWSAction action){
		if (!uwsActions.contains(action))
			return uwsActions.add(action);
		else
			return false;
	}

	/**
	 * <p>Lets inserting the given action at the given position in the actions list of this UWS.</p>
	 *
	 * @param indAction							The index where the given action must be inserted.
	 * @param action							The action to add.
	 *
	 * @return									<i>true</i> if the given action has been successfully added, <i>false</i> otherwise.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the given index is incorrect (index < 0 || index >= uwsActions.size()).
	 */
	public final boolean addUWSAction(int indAction, UWSAction action) throws ArrayIndexOutOfBoundsException{
		if (!uwsActions.contains(action)){
			uwsActions.add(indAction, action);
			return true;
		}
		return false;
	}

	/**
	 * Replaces the specified action by the given action.
	 *
	 * @param indAction							Index of the action to replace.
	 * @param action							The replacer.
	 *
	 * @return									<i>true</i> if the replacement has been a success, <i>false</i> otherwise.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the index is incorrect (index < 0 || index >= uwsActions.size()).
	 */
	public final boolean setUWSAction(int indAction, UWSAction action) throws ArrayIndexOutOfBoundsException{
		if (!uwsActions.contains(action)){
			uwsActions.set(indAction, action);
			return true;
		}
		return false;
	}

	/**
	 * Replaces the action which has the same name that the given action.
	 *
	 * @param action	The replacer.
	 *
	 * @return			The replaced action
	 * 					or <i>null</i> if the given action is <i>null</i>
	 * 									or if there is no action with the same name (in this case, the given action is not added).
	 */
	public final UWSAction replaceUWSAction(UWSAction action){
		if (action == null)
			return null;
		else{
			for(int i = 0; i < uwsActions.size(); i++){
				if (uwsActions.get(i).equals(action))
					return uwsActions.set(i, action);
			}
			return null;
		}
	}

	/**
	 * Gets the number of actions this UWS has.
	 *
	 * @return	The number of its actions.
	 */
	public final int getNbUWSActions(){
		return uwsActions.size();
	}

	/**
	 * Gets the action of this UWS which has the same name as the given one.
	 *
	 * @param actionName	The name of the searched action.
	 *
	 * @return				The corresponding action
	 * 						or <i>null</i> if there is no corresponding action.
	 */
	public final UWSAction getUWSAction(String actionName){
		for(int i = 0; i < uwsActions.size(); i++){
			if (uwsActions.get(i).getName().equals(actionName))
				return uwsActions.get(i);
		}
		return null;
	}

	/**
	 * Gets all actions of this UWS.
	 *
	 * @return	An iterator on its actions.
	 */
	public final Iterator<UWSAction> getUWSActions(){
		return uwsActions.iterator();
	}

	/**
	 * Gets the UWS action executed during the last call of {@link #executeRequest(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @return	The last used UWS action.
	 */
	public final UWSAction getExecutedAction(){
		return executedAction;
	}

	/**
	 * Removes the specified action from this UWS.
	 *
	 * @param indAction							The index of the UWS action to remove.
	 *
	 * @return									The removed action.
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the given index is incorrect (index < 0 || index >= uwsActions.size()).
	 */
	public final UWSAction removeUWSAction(int indAction) throws ArrayIndexOutOfBoundsException{
		return uwsActions.remove(indAction);
	}

	/**
	 * Removes the action of this UWS which has the same name as the given one.
	 *
	 * @param actionName	The name of the UWS to remove.
	 * @return				The removed action
	 * 						or <i>null</i> if there is no corresponding action.
	 */
	public final UWSAction removeUWSAction(String actionName){
		for(int i = 0; i < uwsActions.size(); i++){
			if (uwsActions.get(i).getName().equals(actionName))
				return uwsActions.remove(i);
		}
		return null;
	}

	/* ********************** */
	/* UWS MANAGEMENT METHODS */
	/* ********************** */

	/**
	 * <p>Generate a unique ID for the given request.</p>
	 *
	 * <p>By default, a timestamp is returned.</p>
	 *
	 * @param request	Request whose an ID is asked.
	 *
	 * @return	The ID of the given request.
	 *
	 * @since 4.1
	 */
	protected synchronized String generateRequestID(final HttpServletRequest request){
		String id;
		do{
			id = System.currentTimeMillis() + "";
		}while(lastRequestID != null && lastRequestID.startsWith(id));
		lastRequestID = id;
		return id;
	}

	/**
	 * <p>Executes the given request according to the <a href="http://www.ivoa.net/Documents/UWS/20100210/">IVOA Proposed Recommendation of 2010-02-10</a>.
	 * The result is returned in the given response.</p>
	 *
	 * <p>Here is the followed algorithm:</p>
	 * <ol>
	 * 	<li>Load the request in the UWS URL interpreter (see {@link UWSUrl#load(HttpServletRequest)})</li>
	 * 	<li>Extract the user ID (see {@link UserIdentifier#extractUserId(UWSUrl, HttpServletRequest)})</li>
	 * 	<li>Iterate - in order - on all available actions and apply the first which matches.
	 * 		(see {@link UWSAction#match(UWSUrl, JobOwner, HttpServletRequest)} and {@link UWSAction#apply(UWSUrl, JobOwner, HttpServletRequest, HttpServletResponse)})</li>
	 * </ol>
	 *
	 * @param request		The UWS request.
	 * @param response		The response of this request which will be edited by the found UWS actions.
	 *
	 * @return				<i>true</i> if the request has been executed successfully, <i>false</i> otherwise.
	 *
	 * @throws UWSException	If no action matches or if any error has occurred while applying the found action.
	 * @throws IOException	If it is impossible to write in the given {@link HttpServletResponse}.
	 *
	 * @see UWSUrl#UWSUrl(HttpServletRequest)
	 * @see UWSUrl#load(HttpServletRequest)
	 * @see UserIdentifier#extractUserId(UWSUrl, HttpServletRequest)
	 * @see UWSAction#match(UWSUrl, JobOwner, HttpServletRequest)
	 * @see UWSAction#apply(UWSUrl, JobOwner, HttpServletRequest, HttpServletResponse)
	 */
	public boolean executeRequest(HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException{
		if (request == null || response == null)
			return false;

		// Generate a unique ID for this request execution (for log purpose only):
		final String reqID = (request.getAttribute(UWS.REQ_ATTRIBUTE_ID) == null ? generateRequestID(request) : request.getAttribute(UWS.REQ_ATTRIBUTE_ID).toString());
		if (request.getAttribute(UWS.REQ_ATTRIBUTE_ID) == null)
			request.setAttribute(UWS.REQ_ATTRIBUTE_ID, reqID);

		// Extract all parameters:
		if (request.getAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS) == null){
			try{
				request.setAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS, requestParser.parse(request));
			}catch(UWSException ue){
				logger.log(LogLevel.ERROR, "REQUEST_PARSER", "Can not extract the HTTP request parameters!", ue);
			}
		}

		// Log the reception of the request:
		logger.logHttp(LogLevel.INFO, request, reqID, null, null);

		boolean actionApplied = false;
		UWSAction action = null;
		JobOwner user = null;

		try{
			if (this.urlInterpreter == null){
				// Initialize the URL interpreter if not already done:
				setUrlInterpreter(new UWSUrl(request));

				// Log the successful initialization:
				logger.logUWS(LogLevel.INFO, this, "INIT", "UWS successfully initialized.", null);
			}

			// Update the UWS URL interpreter:
			UWSUrl urlInterpreter = new UWSUrl(this.urlInterpreter);
			urlInterpreter.load(request);

			// Identify the user:
			user = UWSToolBox.getUser(request, userIdentifier);

			// Set the character encoding:
			response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

			// Apply the appropriate UWS action:
			for(int i = 0; action == null && i < uwsActions.size(); i++){
				if (uwsActions.get(i).match(urlInterpreter, user, request)){
					action = uwsActions.get(i);
					choosenSerializer = null;
					actionApplied = action.apply(urlInterpreter, user, request, response);
				}
			}

			// If no corresponding action has been found, throw an error:
			if (action == null)
				throw new UWSException(UWSException.NOT_IMPLEMENTED, "Unknown UWS action!");

			response.flushBuffer();

			// Log the successful execution of the action:
			logger.logHttp(LogLevel.INFO, response, reqID, user, "UWS action \"" + ((action != null) ? action.getName() : null) + "\" successfully executed.", null);

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
			logger.logHttp(LogLevel.INFO, response, reqID, user, "HTTP request aborted or connection with the client closed => the UWS action \"" + action.getName() + "\" has stopped and the body of the HTTP response can not have been partially or completely written!", null);

		}catch(UWSException ex){
			/*
			 *   Any known/"expected" UWS exception is logged but also returned to the HTTP client in an error document.
			 *   Since the error is known, it is supposed to have already been logged with a full stack trace. Thus, there
			 * is no need to log again its stack trace...just its message is logged.
			 *   Besides, this error may also be just a redirection and not a true error. In such case, the error message
			 * is not logged.
			 */
			// If redirection, flag the action as executed with success:
			if (ex.getHttpErrorCode() == UWSException.SEE_OTHER)
				actionApplied = true;
			sendError(ex, request, reqID, user, ((action != null) ? action.getName() : null), response);

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
			errorWriter.writeError(ise, response, request, reqID, user, ((action != null) ? action.getName() : null));
			// Log the error:
			getLogger().logHttp(LogLevel.FATAL, response, reqID, user, "HTTP response already partially committed => the UWS action \"" + action.getName() + "\" has stopped and the body of the HTTP response can not have been partially or completely written!", (ise.getCause() != null) ? ise.getCause() : ise);

		}catch(Throwable t){
			/*
			 *   Any other error is considered as unexpected if it reaches this point. Consequently, it has not yet been logged.
			 * So its stack trace will be fully logged, and an appropriate message will be returned to the HTTP client. The
			 * returned document should contain not too technical information which would be useless for the user.
			 */
			sendError(t, request, reqID, user, ((action != null) ? action.getName() : null), response);

		}finally{
			executedAction = action;
			// Free resources about uploaded files ; only unused files will be deleted:
			UWSToolBox.deleteUploads(request);
		}

		return actionApplied;
	}

	/**
	 * <p>Sends a redirection (with the HTTP status code 303) to the given URL/URI into the given response.</p>
	 *
	 * @param url		The redirection URL/URI.
	 * @param request	The {@link HttpServletRequest} which may be used to make a redirection.
	 * @param user		The user which executes the given request.
	 * @param uwsAction	The UWS action corresponding to the given request.
	 * @param response	The {@link HttpServletResponse} which must contain all information to make a redirection.
	 *
	 * @throws IOException	If there is an error during the redirection.
	 * @throws UWSException	If there is any other error.
	 */
	public void redirect(String url, HttpServletRequest request, JobOwner user, String uwsAction, HttpServletResponse response) throws IOException, UWSException{
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
	public final void sendError(UWSException error, HttpServletRequest request, String reqID, JobOwner user, String uwsAction, HttpServletResponse response) throws IOException, UWSException{
		if (error.getHttpErrorCode() == UWSException.SEE_OTHER){
			// Log the redirection, if any:
			logger.logHttp(LogLevel.INFO, response, reqID, user, "HTTP " + UWSException.SEE_OTHER + " [Redirection toward " + error.getMessage() + "] - Action \"" + uwsAction + "\" successfully executed.", null);
			// Apply the redirection:
			redirect(error.getMessage(), request, user, uwsAction, response);
		}else
			sendError((Throwable)error, request, reqID, user, uwsAction, response);
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
	 * @param uwsAction		The UWS action corresponding to the given request.
	 * @param response		The response in which the error must be published.
	 *
	 * @throws IOException	If there is an error when calling {@link HttpServletResponse#sendError(int, String)}.
	 *
	 * @see ServiceErrorWriter#writeError(Throwable, HttpServletResponse, HttpServletRequest, String, JobOwner, String)
	 */
	public final void sendError(Throwable error, HttpServletRequest request, String reqID, JobOwner user, String uwsAction, HttpServletResponse response) throws IOException{
		// Write the error in the response and return the appropriate HTTP status code:
		errorWriter.writeError(error, response, request, reqID, user, uwsAction);
		// Log the error:
		if (error instanceof UWSException)
			logger.logHttp(LogLevel.ERROR, response, reqID, user, "UWS action \"" + uwsAction + "\" FAILED with the error: \"" + error.getMessage() + "\"!", null);
		else
			logger.logHttp(LogLevel.FATAL, response, reqID, user, "UWS action \"" + uwsAction + "\" execution FAILED with a GRAVE error!", error);
	}

}
