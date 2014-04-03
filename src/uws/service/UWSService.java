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

import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uws.AcceptHeader;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.job.ExecutionPhase;
import uws.job.JobList;
import uws.job.UWSJob;

import uws.job.manager.DefaultExecutionManager;
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
import uws.service.actions.ShowHomePage;
import uws.service.actions.UWSAction;
import uws.service.backup.UWSBackupManager;

import uws.service.error.DefaultUWSErrorWriter;
import uws.service.error.ServiceErrorWriter;
import uws.service.file.UWSFileManager;
import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog;

/**
 * <h3>General description</h3>
 * 
 * <p>An abstract facility to implement the <b>U</b>niversal <b>W</b>orker <b>S</b>ervice pattern.</p>
 * 
 * <p>It can manage several jobs lists (create new, get and remove).</p>
 * 
 * <p>It also interprets {@link HttpServletRequest}, applies the action specified in its given URL and parameters
 * <i>(according to the <a href="http://www.ivoa.net/Documents/UWS/20100210">IVOA Proposed Recommendation of 2010-02-10</a>)</i>
 * and returns the corresponding response in a {@link HttpServletResponse}.</p>
 * 
 * <h3>The UWS URL interpreter</h3>
 * 
 * <p>Any subclass of {@link UWSService} has one object called the UWS URL interpreter. It is stored in the field {@link #urlInterpreter}.
 * It lets interpreting the URL of any received request. Thus you can know on which jobs list, job and/or job attribute(s)
 * the request applies.</p>
 * 
 * <p>This interpreter must be initialized with the base URL/URI of this UWS. By using the default constructor (the one with no parameter),
 * the URL interpreter will be built at the first request (see {@link UWSUrl#UWSUrl(HttpServletRequest)}) and so the base URI is
 * extracted directly from the request).</p>
 * 
 * <p>You want to set another base URI or to use a custom URL interpreter, you have to set yourself the interpreter
 * by using the method {@link #setUrlInterpreter(UWSUrl)}.</p>
 * 
 * <h3>Create a job</h3>
 * 
 * <p>The most important abstract function of this class is {@link UWSService#createJob(Map)}. It allows to create an instance
 * of the type of job which is managed by this UWS. The only parameter is a map of a job attributes. It is the same map that
 * take the functions {@link UWSJob#UWSJob(Map)} and {@link UWSJob#addOrUpdateParameters(Map)}.</p>
 * 
 * <p>There are two convenient implementations of this abstract method in {@link BasicUWS} and {@link ExtendedUWS}. These two implementations
 * are based on the Java Reflection.</p>
 * 
 * <h3>UWS actions</h3>
 * 
 * <p>All the actions described in the IVOA recommendation are already managed. Each of these actions are defined in
 * an instance of {@link UWSAction}:</p>
 * <ul>
 * 	<li>{@link UWSAction#LIST_JOBS LIST_JOBS}: see the class {@link ListJobs}</li>
 * 	<li>{@link UWSAction#ADD_JOB ADD_JOB}: see the class {@link AddJob}</li>
 * 	<li>{@link UWSAction#DESTROY_JOB DESTROY_JOB}: see the class {@link DestroyJob}</li>
 * 	<li>{@link UWSAction#JOB_SUMMARY JOB_SUMMARY}: see the class {@link JobSummary}</li>
 * 	<li>{@link UWSAction#GET_JOB_PARAM GET_JOB_PARAM}: see the class {@link GetJobParam}</li>
 * 	<li>{@link UWSAction#SET_JOB_PARAM SET_JOB_PARAM}: see the class {@link SetJobParam}</li>
 * 	<li>{@link UWSAction#HOME_PAGE HOME_PAGE}: see the class {@link ShowHomePage}</li>
 * </ul>
 * 
 * <p><b>However you can add your own UWS actions !</b> To do that you just need to implement the abstract class {@link UWSAction}
 * and to call the method {@link #addUWSAction(UWSAction)} with an instance of this implementation.</p>
 * 
 * <p><b><u>IMPORTANT:</u> You must be careful when you override the function {@link UWSAction#match(UWSUrl, String, HttpServletRequest)}
 * so that your test is as precise as possible ! Indeed the order in which the actions of a UWS are evaluated is very important !<br />
 * <u>If you want to be sure your custom UWS action is always evaluated before any other UWS action you can use the function
 * {@link #addUWSAction(int, UWSAction)} with 0 as first parameter !</u></b></p>
 * 
 * <p><i><u>Note:</u> You can also replace an existing UWS action thanks to the method {@link #replaceUWSAction(UWSAction)} or
 * {@link #setUWSAction(int, UWSAction)} !</i></p>
 * 
 * <h3>User identification</h3>
 * 
 * <p>Some UWS actions need to know the current user so that they can adapt their response (i.e. LIST_JOBS must return the jobs of only
 * one user: the current one). Thus, before executing a UWS action (and even before choosing the good action in function of the request)
 * the function {@link UserIdentifier#extractUserId(UWSUrl, HttpServletRequest)} is called. Its goal
 * is to identify the current user in function of the received request.</p>
 * 
 * <p>
 * 	<i><u>Notes:</u>
 * 		<ul>
 * 			<li>If this function returns NULL, the UWS actions must be executed on all jobs, whatever is their owner !</li>
 * 			<li>{@link UserIdentifier} is an interface. So you must implement it and then set its extension to this UWS
 * 				by using {@link #setUserIdentifier(UserIdentifier)}.</li>
 *		</ul>
 * 	</i></p>
 * </p>
 * 
 * <h3>Queue management</h3>
 * 
 * <p>One of the goals of a UWS is to manage an execution queue for all managed jobs. This task is given to an instance
 * of {@link DefaultExecutionManager}, stored in the field {@link #executionManager}. Each time a job is created,
 * the UWS sets it the execution manager (see {@link AddJob}). Thus the {@link UWSJob#start()} method will ask to the manager
 * whether it can execute now or whether it must be put in a {@link ExecutionPhase#QUEUED QUEUED} phase until enough resources are available for its execution.</p>
 * 
 * <p>By extending the class {@link DefaultExecutionManager} and by overriding {@link DefaultExecutionManager#isReadyForExecution(UWSJob)}
 * you can change the condition which puts a job in the {@link ExecutionPhase#EXECUTING EXECUTING} or in the {@link ExecutionPhase#QUEUED QUEUED} phase. By default, a job is put
 * in the {@link ExecutionPhase#QUEUED QUEUED} phase if there are more running jobs than a given number.</p>
 * 
 * <p>With this manager it is also possible to list all running jobs in addition of all queued jobs, thanks to the methods:
 * {@link DefaultExecutionManager#getRunningJobs()}, {@link DefaultExecutionManager#getQueuedJobs()}, {@link DefaultExecutionManager#getNbRunningJobs()}
 * and {@link DefaultExecutionManager#getNbQueuedJobs()}.</p>
 * 
 * <h3>Serializers & MIME types</h3>
 * 
 * <p>According to the IVOA recommendation, the XML format is the default format in which each UWS resource must be returned. However it
 * is told that other formats can also be managed. To allow that, {@link UWSService} manages a list of {@link UWSSerializer} and
 * lets define which is the default one to use. <i>By default, there are two serializers: {@link XMLSerializer} (the default choice)
 * and {@link JSONSerializer}.</i></p>
 * 
 * <p>One proposed way to choose automatically the format to use is to look at the Accept header of a HTTP-Request. This header field is
 * a list of MIME types (each one with a quality - a sort of priority). Thus each {@link UWSSerializer} is associated with a MIME type so
 * that {@link UWSService} can choose automatically the preferred format and so, the serializer to use.</p>
 * 
 * <p><b><u>WARNING:</u> Only one {@link UWSSerializer} can be associated with a given MIME type in an {@link UWSService} instance !
 * Thus, if you add a {@link UWSSerializer} to a UWS, and this UWS has already a serializer for the same MIME type,
 * it will be replaced by the added one.</b></p>
 * 
 * <p><i><u>Note:</u> A XML document can be linked to a XSLT style-sheet. By using the method {@link XMLSerializer#setXSLTPath(String)}
 * you can define the path/URL of the XLST to link to each UWS resource. <br />
 * <u>Since the {@link XMLSerializer} is the default format for a UWS resource you can also use the function
 * {@link UWSService#setXsltURL(String)} !</u></i></p>
 * 
 * <h3>The UWS Home page</h3>
 * 
 * <p>As for a job or a jobs list, a UWS is also a UWS resource. That's why it can also be serialized !</p>
 * 
 * <p>However in some cases it could more interesting to substitute this resource by a home page of the whole UWS by using the function:
 * {@link #setHomePage(String)} or {@link #setHomePage(URL, boolean)}.
 * </p>
 * 
 * <p><i><u>Note:</u> To go back to the UWS serialization (that is to say to abort a call to {@link #setHomePage(String)}),
 * use the method {@link #setDefaultHomePage()} !</i></p>
 * 
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public class UWSService implements UWS {
	private static final long serialVersionUID = 1L;

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

	/** Lets writing/formatting any exception/throwable in a HttpServletResponse. */
	protected ServiceErrorWriter errorWriter;


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ *//**
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
	 * 
	 * @see #UWSService(UWSFactory, UWSFileManager, UWSLog)
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager) {
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
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSLog logger) {
		if (jobFactory == null)
			throw new NullPointerException("Missing UWS factory ! Can not create a UWSService.");
		factory = jobFactory;

		if (fileManager == null)
			throw new NullPointerException("Missing UWS file manager ! Can not create a UWSService.");
		this.fileManager = fileManager;

		this.logger = (logger == null) ? new DefaultUWSLog(this) : logger;
		errorWriter = new DefaultUWSErrorWriter(this);

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
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final String baseURI) throws UWSException {
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
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSLog logger, final String baseURI) throws UWSException {
		this(jobFactory, fileManager, logger);

		// Extract the name of the UWS:
		try{
			urlInterpreter = new UWSUrl(baseURI);
			name = urlInterpreter.getUWSName();
			getLogger().uwsInitialized(this);
		}catch(UWSException ex){
			throw new UWSException(UWSException.BAD_REQUEST, ex, "Invalid base UWS URI ("+baseURI+") !");
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
	 * @see #UWSService(UWSFactory, UWSFileManager, UWSLog, UWSUrl)
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSUrl urlInterpreter) {
		this(jobFactory, fileManager, null, urlInterpreter);
	}

	/**
	 * Builds a UWS with the given UWS URL interpreter.
	 * 
	 * @param jobFactory	Object which lets creating the UWS jobs managed by this UWS and their thread/task.
	 * @param fileManager	Object which lets managing all files managed by this UWS (i.e. log, result, backup, error, ...).
	 * @param logger		Object which lets printing any message (error, info, debug, warning).
	 * @param urlInterpreter	The UWS URL interpreter to use in this UWS.
	 */
	public UWSService(final UWSFactory jobFactory, final UWSFileManager fileManager, final UWSLog logger, final UWSUrl urlInterpreter) {
		this(jobFactory, fileManager, logger);
		setUrlInterpreter(urlInterpreter);
		if (this.urlInterpreter != null)
			getLogger().uwsInitialized(this);
	}

	/* ************** */
	/* LOG MANAGEMENT */
	/* ************** */
	public UWSLog getLogger(){
		return logger;
	}

	/**
	 * Gets the object used to write/format any error in a HttpServletResponse.
	 * 
	 * @return The error writer/formatter.
	 */
	public final ServiceErrorWriter getErrorWriter() {
		return errorWriter;
	}

	/**
	 * <p>Sets the object used to write/format any error in a HttpServletResponse.</p>
	 * 
	 * <p><i><u>Note:</u> Nothing is done if the given writer is NULL !</i></p>
	 * 
	 * @param errorWriter The new error writer/formatter.
	 */
	public final void setErrorWriter(ServiceErrorWriter errorWriter) {
		if (errorWriter != null)
			this.errorWriter = errorWriter;
	}


	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	public final String getName() {
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

	public final String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this UWS.
	 * 
	 * @param description	Its new description.
	 */
	public final void setDescription(String description) {
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
		userIdentifier = identifier;
	}

	public final UWSFactory getFactory(){
		return factory;
	}

	public final UWSFileManager getFileManager(){
		return fileManager;
	}

	public final UWSBackupManager getBackupManager() {
		return backupManager;
	}

	/**
	 * <p>
	 * 	Sets its backup manager.
	 * 	This manager will be called at each user action to save only its own jobs list by calling {@link UWSBackupManager#saveOwner(String)}.
	 * </p>
	 * 
	 * @param backupManager Its new backup manager.
	 */
	public final void setBackupManager(final UWSBackupManager backupManager) {
		this.backupManager = backupManager;
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
	public final void setDefaultSerializer(String mimeType) throws UWSException {
		if (serializers.containsKey(mimeType))
			defaultSerializer = mimeType;
		else
			throw UWSExceptionFactory.missingSerializer(mimeType, "Impossible to set the default serializer.");
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

	public final UWSSerializer getSerializer(String mimeTypes) throws UWSException {
		choosenSerializer = null;

		if (mimeTypes != null){
			// Parse the given MIME types list:
			AcceptHeader accept = new AcceptHeader(mimeTypes);
			ArrayList<String> lstMimeTypes = accept.getOrderedMimeTypes();

			// Try each of them and stop at the first which match with an existing serializer:
			for(int i=0; choosenSerializer == null && i<lstMimeTypes.size(); i++)
				choosenSerializer = serializers.get(lstMimeTypes.get(i));
		}

		// If no serializer has been found for each given mime type, return the default one:
		if (choosenSerializer == null){
			choosenSerializer = serializers.get(defaultSerializer);
			if (choosenSerializer == null)
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "No UWS Serializer available neither for \""+mimeTypes+"\" (given MIME types) nor \""+defaultSerializer+"\" (default serializer MIME type) !");
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
	public final Iterator<JobList> iterator(){
		return mapJobLists.values().iterator();
	}

	public final JobList getJobList(String name){
		return mapJobLists.get(name);
	}

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
	 * @see JobList#setUWS(AbstractUWS)
	 * @see UWS#addJobList(JobList)
	 */
	public final boolean addJobList(JobList jl){
		if (jl == null)
			return false;
		else if (mapJobLists.containsKey(jl.getName()))
			return false;

		try{
			jl.setUWS(this);
			mapJobLists.put(jl.getName(), jl);
		}catch (IllegalStateException ise){
			logger.error("The jobs list \""+jl.getName()+"\" can not be added into the UWS "+getName()+" !", ise);
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
	 * @see JobList#setUWS(UWSService)
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
	 * @see JobList#setUWS(UWSService)
	 */
	public boolean destroyJobList(JobList jl) {
		if (jl == null)
			return false;

		jl = mapJobLists.remove(jl.getName());
		if (jl != null){
			try{
				jl.clear();
				jl.setUWS(null);
			}catch(IllegalStateException ise){
				getLogger().warning("Impossible to erase completely the association between the jobs list \""+jl.getName()+"\" and the UWS \""+getName()+"\", because: "+ise.getMessage());
			}
		}
		return jl != null;
	}

	/*
	 * Removes all managed jobs lists.
	 * 
	 * @see #removeJobList(String)
	 *
	public final void removeAllJobLists(){
		ArrayList<String> jlNames = new ArrayList<String>(mapJobLists.keySet());
		for(String jlName : jlNames)
			removeJobList(jlName);
	}*/

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
	 * the method {@link UWSAction#match(UWSUrl, String, HttpServletRequest)}) lastly !</b></p>
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
	public final boolean addUWSAction(int indAction, UWSAction action) throws ArrayIndexOutOfBoundsException {
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
	public final boolean setUWSAction(int indAction, UWSAction action) throws ArrayIndexOutOfBoundsException {
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
			for(int i=0; i<uwsActions.size(); i++){
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
		for(int i=0; i<uwsActions.size(); i++){
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
	public final UWSAction removeUWSAction(int indAction) throws ArrayIndexOutOfBoundsException {
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
		for(int i=0; i<uwsActions.size(); i++){
			if (uwsActions.get(i).getName().equals(actionName))
				return uwsActions.remove(i);
		}
		return null;
	}

	/* ********************** */
	/* UWS MANAGEMENT METHODS */
	/* ********************** */
	/**
	 * <p>Executes the given request according to the <a href="http://www.ivoa.net/Documents/UWS/20100210/">IVOA Proposed Recommendation of 2010-02-10</a>.
	 * The result is returned in the given response.</p>
	 * 
	 * <p>Here is the followed algorithm:</p>
	 * <ol>
	 * 	<li>Load the request in the UWS URL interpreter (see {@link UWSUrl#load(HttpServletRequest)})</li>
	 * 	<li>Extract the user ID (see {@link UserIdentifier#extractUserId(UWSUrl, HttpServletRequest)})</li>
	 * 	<li>Iterate - in order - on all available actions and apply the first which matches.
	 * 		(see {@link UWSAction#match(UWSUrl, String, HttpServletRequest)} and {@link UWSAction#apply(UWSUrl, String, HttpServletRequest, HttpServletResponse)})</li>
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
	 * @see UWSAction#match(UWSUrl, String, HttpServletRequest)
	 * @see UWSAction#apply(UWSUrl, String, HttpServletRequest, HttpServletResponse)
	 */
	public boolean executeRequest(HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException {
		if (request == null || response == null)
			return false;

		boolean actionApplied = false;
		UWSAction action = null;
		JobOwner user = null;

		try{
			// Update the UWS URL interpreter:
			if (urlInterpreter == null){
				setUrlInterpreter(new UWSUrl(request));
				getLogger().uwsInitialized(this);
			}
			urlInterpreter.load(request);

			// Identify the user:
			user = (userIdentifier == null)?null:userIdentifier.extractUserId(urlInterpreter, request);

			// Apply the appropriate UWS action:
			for(int i=0; action == null && i<uwsActions.size(); i++){
				if (uwsActions.get(i).match(urlInterpreter, user, request)){
					action = uwsActions.get(i);
					choosenSerializer = null;
					actionApplied = action.apply(urlInterpreter, user, request, response);
				}
			}

			// If no corresponding action has been found, throw an error:
			if (action == null)
				throw new UWSException(UWSException.NOT_IMPLEMENTED, "Unknown UWS action ! This HTTP request can not be interpreted by this UWS service !");

			response.flushBuffer();
			logger.httpRequest(request, user, (action != null)?action.getName():null, HttpServletResponse.SC_OK, "[OK]", null);

		}catch(UWSException ex){
			if (ex.getHttpErrorCode() == UWSException.SEE_OTHER)
				actionApplied = true;
			sendError(ex, request, user, (action != null)?action.getName():null, response);
		}catch(Exception ex){
			sendError(ex, request, user, (action != null)?action.getName():null, response);
		}finally{
			executedAction = action;
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
	public void redirect(String url, HttpServletRequest request, JobOwner user, String uwsAction, HttpServletResponse response) throws IOException, UWSException {
		response.setStatus(HttpServletResponse.SC_SEE_OTHER);
		response.setContentType(request.getContentType());
		response.setHeader("Location", url);
		response.flushBuffer();
		logger.httpRequest(request, user, uwsAction, HttpServletResponse.SC_SEE_OTHER, "[Redirection toward "+url+"]", null);
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
	 * @param user			The user which executes the given request.
	 * @param uwsAction	The UWS action corresponding to the given request.
	 * @param response		The response in which the error must be published.
	 * 
	 * @throws IOException	If there is an error when calling {@link #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)} or {@link HttpServletResponse#sendError(int, String)}.
	 * @throws UWSException	If there is an error when calling {@link #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse))}.
	 * 
	 * @see #redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)
	 * @see {@link ServiceErrorWriter#writeError(Throwable, HttpServletResponse, HttpServletRequest, JobOwner, String)}
	 */
	public final void sendError(UWSException error, HttpServletRequest request, JobOwner user, String uwsAction, HttpServletResponse response) throws IOException, UWSException {
		if (error.getHttpErrorCode() == UWSException.SEE_OTHER)
			redirect(error.getMessage(), request, user, uwsAction, response);
		else{
			errorWriter.writeError(error, response, request, user, uwsAction);
			/*if (error.getHttpErrorCode() == UWSException.INTERNAL_SERVER_ERROR)
				logger.error(error);
			response.sendError(error.getHttpErrorCode(), error.getMessage());
			logger.httpRequest(request, user, uwsAction, error.getHttpErrorCode(), error.getMessage(), error);*/
		}
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
	 * @param user			The user which executes the given request.
	 * @param uwsAction	The UWS action corresponding to the given request.
	 * @param response		The response in which the error must be published.
	 * 
	 * @throws IOException	If there is an error when calling {@link HttpServletResponse#sendError(int, String)}.
	 * @throws UWSException
	 * 
	 * @see {@link ServiceErrorWriter#writeError(Throwable, HttpServletResponse, HttpServletRequest, JobOwner, String)}
	 */
	public final void sendError(Exception error, HttpServletRequest request, JobOwner user, String uwsAction, HttpServletResponse response) throws IOException, UWSException {
		errorWriter.writeError(error, response, request, user, uwsAction);
		/*logger.error(error);
		response.sendError(UWSException.INTERNAL_SERVER_ERROR, error.getMessage());
		logger.httpRequest(request, user, uwsAction, UWSException.INTERNAL_SERVER_ERROR, error.getMessage(), error);*/
	}

}
