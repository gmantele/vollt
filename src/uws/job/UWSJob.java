package uws.job;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletOutputStream;

import uws.ISO8601Format;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;
import uws.job.jobInfo.JobInfo;
import uws.job.jobInfo.SingleValueJobInfo;
import uws.job.manager.ExecutionManager;
import uws.job.parameters.UWSParameters;
import uws.job.serializer.UWSSerializer;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSFactory;
import uws.service.UWSUrl;
import uws.service.file.UWSFileManager;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.UploadFile;

/**
 * <h3>Brief description</h3>
 *
 * <p>Default implementation of a job of the UWS pattern.</p>
 *
 * <h3>Some attributes comments</h3>
 *
 * <ul>
 * 	<li>
 * 		The job attributes <i>creationTime</i>, <i>startTime</i> and
 * 		<i>endTime</i> are automatically managed by {@link UWSJob}. You don't
 * 		have to do anything! The date/time format is managed automatically by
 * 		the library and can not be customized since it is imposed by the UWS
 * 		protocol definition: ISO-8601.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		Once set, the <i>destruction</i> and the <i>executionDuration</i>
 * 		attributes are automatically managed. That is to say:
 * 		<ul>
 * 			<li><u>if the destruction time is reached:</u> the job stops and it
 * 				is destroyed by its job list</li>
 * 			<li><u>if the execution duration is elapsed:</u> the job stops and
 * 				the phase is put to {@link ExecutionPhase#ABORTED ABORTED}.</li>
 * 		</ul>
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<u>The <i>owner</i> attribute is set at the job creation and can not be
 * 		changed after</u> ! If no owner is given at the job creation, its
 * 		default value is <i>null</i>.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		If your job is executable, do not forget to set the <i>quote</i>
 * 		parameter ONLY by using the {@link #setQuote(long)} method (a negative
 * 		value or {@link #QUOTE_NOT_KNOWN} value indicates the quote is not
 * 		known ; {@link #QUOTE_NOT_KNOWN} is the default value). This duration in
 * 		seconds will be added to the <i>startTime</i> and then automatically
 * 		formatted into an ISO-8601 date by the used serializer.
 * 	</li>
 * </ul>
 *
 * <h3>More details</h3>
 *
 * <ul>
 * 	<li>
 * 		<b>{@link #generateJobId()}:</b>
 * 		This function is called at the construction of any {@link UWSJob}. It
 * 		allows to generate a unique job ID. By default:
 * 		       time (in milliseconds) + a upper-case letter (A, B, C, ....).
 * 		<u>If you want customizing the job ID of your jobs</u>, you need to
 * 		override this function or to use the new function
 * 		{@link #UWSJob(JobOwner, UWSParameters, String)}.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<b>{@link #clearResources()}:</b>
 * 		This method is called <u>only at the destruction of the job</u>.
 * 		By default, the job is stopped (if running), thread resources are freed,
 * 		the job is removed from its jobs list and result/error files are
 * 		deleted.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<b>{@link #setPhaseManager(JobPhase)}:</b>
 * 		Lets customizing the default behaviors of all the execution phases for
 * 		any job instance. For more details see {@link JobPhase}.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<b>{@link #addObserver(JobObserver)}:</b>
 * 		An instance of any kind of AbstractJob can be observed by objects which
 * 		implements {@link JobObserver} (i.e. {@link uws.service.UWSService}).
 * 		Observers are notified at any change of the execution phase.
 * 	</li>
 * </ul>
 *
 * @author	Gr&eacute;gory Mantelet (CDS;ARI)
 * @version	4.5 (07/2020)
 */
public class UWSJob extends SerializableUWSObject {
	private static final long serialVersionUID = 1L;

	/* ********* */
	/* CONSTANTS */
	/* ********* */
	/** Name of the parameter <i>ACTION</i>. */
	public static final String PARAM_ACTION = "ACTION";

	/** Name of the DELETE action. */
	public static final String ACTION_DELETE = "DELETE";

	/** Name of the parameter <i>jobId</i>. */
	public static final String PARAM_JOB_ID = "jobId";

	/** Name of the parameter <i>creationTime</i>.
	 * @since 4.3 */
	public static final String PARAM_CREATION_TIME = "creationTime";

	/** Name of the parameter <i>runId</i>. */
	public static final String PARAM_RUN_ID = "runId";

	/** Name of the parameter <i>owner</i>. */
	public static final String PARAM_OWNER = "owner";

	/** Name of the parameter <i>phase</i>. */
	public static final String PARAM_PHASE = "phase";

	/** Value of the parameter <i>phase</i> which starts the job. */
	public static final String PHASE_RUN = "RUN";

	/** Value of the parameter <i>phase</i> which aborts the job. */
	public static final String PHASE_ABORT = "ABORT";

	/** Name of the parameter <i>quote</i>. */
	public static final String PARAM_QUOTE = "quote";

	/** Name of the parameter <i>startTime</i>. */
	public static final String PARAM_START_TIME = "startTime";

	/** Name of the parameter <i>endTime</i>. */
	public static final String PARAM_END_TIME = "endTime";

	/** Name of the parameter <i>executionDuration</i>. */
	public static final String PARAM_EXECUTION_DURATION = "executionDuration";

	/** Name of the parameter <i>destructionTime</i>. */
	public static final String PARAM_DESTRUCTION_TIME = "destruction";

	/** Name of the parameter <i>errorSummary</i>. */
	public static final String PARAM_ERROR_SUMMARY = "error";

	/** Name of the parameter <i>otherParameters</i>. */
	public static final String PARAM_PARAMETERS = "parameters";

	/** Name of the parameter <i>results</i>. */
	public static final String PARAM_RESULTS = "results";

	/** Name of the parameter <i>jobInfo</i>.
	 * @since 4.2 */
	public static final String PARAM_JOB_INFO = "jobInfo";

	/** Default value of {@link #owner} if no ID are given at the job creation. */
	public final static String ANONYMOUS_OWNER = "anonymous";

	/** Default date format pattern.
	 * @deprecated Replaced by {@link ISO8601Format}.*/
	@Deprecated
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	/** The quote value that indicates the quote of this job is not known. */
	public static final long QUOTE_NOT_KNOWN = -1;

	/** The duration that implies an unlimited execution duration. */
	public final static long UNLIMITED_DURATION = 0;

	/* ********* */
	/* VARIABLES */
	/* ********* */
	/** The last generated job ID. <b>It SHOULD be used ONLY by the function
	 * {@link #generateJobId()} !</b> */
	protected static String lastId = System.currentTimeMillis() + "A";

	/** The identifier of the job (it MUST be different from any other job).
	 * <p><i>Note:
	 * 	It is assigned automatically at the job creation in any job constructor
	 * 	by the function {@link #generateJobId()}. To change the way this ID is
	 * 	generated or its format you must override this function.
	 * </i></p> */
	protected final String jobId;

	/** Date of the initial creation of this job.
	  * <p><i>Note:
	  * 	This attribute can be set only automatically at creation by the UWS
	  * 	service and can not be set or changed by a user (even its owner).
	  * </i></p>
	  * @since 4.3 */
	protected final Date creationTime;

	/** The identifier of the creator of this job.
	 * <p><i>Note:
	 * 	This object will not exist for all invocations of the UWS conformant
	 * 	protocol, but only in cases where the access to the service is
	 * 	authenticated.
	 * </i></p> */
	protected final JobOwner owner;

	/** The jobs list which is supposed to managed this job. */
	private JobList myJobList = null;

	/**
	 * The current phase of the job.
	 * <p><b>Remember:</b>
	 * 	A job is treated as a state machine thanks to this attribute.
	 * </p>
	 * <ul>
	 * 	<li>A successful job will normally progress through the
	 * 		{@link ExecutionPhase#PENDING PENDING},
	 * 		{@link ExecutionPhase#QUEUED QUEUED},
	 * 		{@link ExecutionPhase#EXECUTING EXECUTING},
	 * 		{@link ExecutionPhase#COMPLETED COMPLETED} phases in that
	 * 		order.</li>
	 * 	<li>At any time before the
	 * 		{@link ExecutionPhase#COMPLETED COMPLETED} phase a job may
	 * 		either be {@link ExecutionPhase#ABORTED ABORTED} or may suffer
	 * 		an {@link ExecutionPhase#ERROR ERROR}.</li>
	 * 	<li>If the UWS reports an {@link ExecutionPhase#UNKNOWN UNKNOWN}
	 * 		phase, then all the client can do is re-query the phase until a
	 * 		known phase is reported.</li>
	 * 	<li>A UWS may place a job in a {@link ExecutionPhase#HELD HELD}
	 * 		phase on receipt of a PHASE=RUN request it for some reason the
	 * 		job cannot be immediately queued - in this case it is the
	 * 		responsibility of the client to request PHASE=RUN again at some
	 * 		later time.</li>
	 * </ul> */
	private JobPhase phase;

	/** The used date formatter.
	 * @deprecated Replaced by {@link ISO8601Format}. */
	@Deprecated
	public static final DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

	/**
	 * This time predicts when the job is likely to complete.
	 * <p>
	 * 	It represents the estimated amount of time (in seconds) from the
	 * 	job starting date-time to its successful end.
	 * </p>
	 * <p><i>Note:</i>
	 * 	By default, if no ID is given, {@link #quote} is set to
	 * 	{@link #QUOTE_NOT_KNOWN} (= {@value #QUOTE_NOT_KNOWN}).
	 * </p>
	 */
	private long quote = QUOTE_NOT_KNOWN;

	/** The time at which the job execution started. */
	private Date startTime = null;

	/** The time at which the job execution ended. */
	private Date endTime = null;

	/** This error summary gives a human-readable error message for the
	 * underlying job.
	 * <p><i>Note:
	 * 	This object is intended to be a detailed error message, and
	 * 	consequently, might be a large piece of text such as a stack trace.
	 * </i></p> */
	protected ErrorSummary errorSummary = null;

	/** This is a list of all results of this job. */
	protected Map<String, Result> results;

	/** List of all input parameters (UWS standard and non-standard parameters). */
	protected final UWSParameters inputParams;

	/** Additional description of this job.
	 * @since 4.2 */
	protected JobInfo jobInfo = null;

	/** The thread to start for executing the job. */
	protected transient JobThread thread = null;

	/** The time (in ms) to wait the end of the thread after an interruption. */
	protected long waitForStop = 1000;

	/** Objects which want to be notified at each modification of the execution
	 * phase of this job. */
	private Vector<JobObserver> observers = new Vector<JobObserver>();

	/** If this job has been restored, this attribute should be set with the
	 * date of its restoration. */
	private final Date restorationDate;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds a job with no owner from a map of all parameters (UWS and
	 * additional parameters).
	 *
	 * <p><i>Note:
	 * 	If the parameter {@link UWSJob#PARAM_PHASE} (</i>phase<i>) is given with
	 * 	the value {@link UWSJob#PHASE_RUN} the job execution starts immediately
	 * 	after the job has been added to a job list or after
	 * 	{@link #applyPhaseParam(JobOwner)} is called.
	 * </i></p>
	 *
	 * @param params	UWS standard and non-standard parameters.
	 *
	 * @see UWSJob#UWSJob(JobOwner, UWSParameters)
	 */
	public UWSJob(final UWSParameters params) {
		this(null, params);
	}

	/**
	 * Builds a job of the given owner and from a map of all parameters (UWS
	 * and additional parameters).
	 *
	 * <p><i><u>Note:</u>
	 * 	if the parameter {@link #PARAM_PHASE} (</i>phase<i>) is given with the
	 * 	value {@link #PHASE_RUN} the job execution starts immediately after the
	 * 	job has been added to a job list or after
	 * 	{@link #applyPhaseParam(JobOwner)} is called.
	 * </i></p>
	 *
	 * @param owner		Job.owner ({@link #PARAM_OWNER}).
	 * @param params	UWS standard and non-standard parameters.
	 *
	 * @see UWSParameters#init()
	 */
	public UWSJob(JobOwner owner, final UWSParameters params) {
		this.creationTime = new Date();

		this.owner = owner;

		phase = new JobPhase(this);

		results = new HashMap<String, Result>();

		inputParams = (params == null ? new UWSParameters() : params);
		inputParams.init();

		jobId = generateJobId();
		restorationDate = null;

		// Move all uploaded files in a location related with this job:
		Iterator<UploadFile> files = inputParams.getFiles();
		while(files.hasNext()) {
			try {
				files.next().move(this);
			} catch(IOException ioe) {
			}
		}
	}

	/**
	 * Builds a job of the given owner and from a map of all parameters (UWS and
	 * additional parameters). The given HTTP request ID will be used as Job ID
	 * if not already used by another job.
	 *
	 * <p><i>Note:
	 * 	If the parameter {@link #PARAM_PHASE} (</i>phase<i>) is given with the
	 * 	value {@link #PHASE_RUN} the job execution starts immediately after the
	 * 	job has been added to a job list or after
	 * 	{@link #applyPhaseParam(JobOwner)} is called.
	 * </i></p>
	 *
	 * @param owner		Job.owner ({@link #PARAM_OWNER}).
	 * @param params	UWS standard and non-standard parameters.
	 * @param requestID	ID of the HTTP request which has initiated the creation
	 *                 	of this job.
	 *                 	<i>Note: if NULL, empty or already used, a job ID will
	 *                 	be generated thanks to {@link #generateJobId()}.</i>
	 *
	 * @see UWSParameters#init()
	 *
	 * @since 4.2
	 */
	public UWSJob(JobOwner owner, final UWSParameters params, final String requestID) {
		this.creationTime = new Date();

		this.owner = owner;

		phase = new JobPhase(this);

		results = new HashMap<String, Result>();

		inputParams = (params == null ? new UWSParameters() : params);
		inputParams.init();

		// Set the Job ID with the value of the HTTP request ID (if not already used by a job):
		synchronized (lastId) {
			if (requestID == null || requestID.trim().length() == 0 || lastId.equals(requestID))
				jobId = generateJobId();
			else {
				jobId = requestID;
				lastId = requestID;
			}
		}
		restorationDate = null;

		// Move all uploaded files in a location related with this job:
		Iterator<UploadFile> files = inputParams.getFiles();
		while(files.hasNext()) {
			try {
				files.next().move(this);
			} catch(IOException ioe) {
			}
		}
	}

	/**
	 * <p><b>CONSTRUCTOR TO USE TO RESTORE A JOB whatever is its phase.</b></p>
	 *
	 * <p>Builds a job of the given owner with all the given parameter.</p>
	 *
	 * <p><i><u>Note:</u>
	 * 	The job phase is automatically set in function of the last parameters
	 * 	(startTime, endTime, results and error). Only the following execution
	 * 	phase are possible: PENDING, ABORTED, ERROR and COMPLETED.
	 * </i></p>
	 *
	 * @param jobID			The ID of this job (NOT NULL).
	 * @param creationTime	Its creation date/time (SHOULD NOT BE NEGATIVE OR
	 *                    	NULL).
	 * @param owner			Its owner.
	 * @param params		UWS standard and non-standard parameters.
	 * @param quote			Its quote (in seconds).
	 * @param startTime		Its start time if it has already been started.
	 * @param endTime		Its end time if it is already finished.
	 * @param results		Its results (if phase=COMPLETED).
	 * @param error			Its error (if phase=ERROR).
	 *
	 * @throws NullPointerException	If the given ID is NULL.
	 *
	 * @since 4.3
	 */
	public UWSJob(final String jobID, final long creationTime, final JobOwner owner, final UWSParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws NullPointerException {
		if (jobID == null)
			throw new NullPointerException("Missing job ID => impossible to build a Job without a valid ID!");

		this.creationTime = (creationTime <= 0) ? new Date() : new Date(creationTime);
		/* Note:
		 *   If no creation date is provided, it may be because we are getting
		 *   the data from an old backup file (so, created by a UWS service
		 *   previously implementing UWS-1.0). Except this missing information,
		 *   the rest of the job properties may be ok, so there is no need to
		 *   throw an error for an accessory property like the creation time.
		 *   So, the current date is set instead. */

		this.jobId = jobID;
		this.owner = owner;

		this.quote = quote;

		if (startTime > 0)
			this.startTime = new Date(startTime);
		if (endTime > 0)
			this.endTime = new Date(endTime);

		this.results = new HashMap<String, Result>();
		if (results != null) {
			for(Result r : results) {
				if (r != null)
					this.results.put(r.getId(), r);
			}
		}

		errorSummary = error;

		this.phase = new JobPhase(this);

		inputParams = params;
		params.init();

		ExecutionPhase p = ExecutionPhase.PENDING;
		if (startTime > 0 && endTime > 0) {
			if (this.results.isEmpty() && this.errorSummary == null)
				p = ExecutionPhase.ABORTED;
			else if (!this.results.isEmpty())
				p = ExecutionPhase.COMPLETED;
			else if (this.errorSummary != null)
				p = ExecutionPhase.ERROR;
		}
		if (phase != null) {
			try {
				setPhase(p, true);
			} catch(UWSException ue) {
				// Can never append because the "force" parameter is true!
			}
		}

		restorationDate = new Date();
	}

	/**
	 * This function lets generating a unique ID.
	 *
	 * <p><b>By default:</b>
	 * 	System.currentTimeMillis()+UpperCharacter (UpperCharacter: one
	 * 	upper-case character chosen in order to guarantee the unicity of the
	 * 	ID: A, B, C, ....)
	 * </p>
	 *
	 * <p><i>Note:
	 * 	DO NOT USE in this function any of the following functions:
	 * 	{@link #getLogger()}, {@link #getFileManager()} and
	 * 	{@link #getFactory()}. All of them will return NULL, because this job
	 * 	does not yet know its jobs list (which is needed to know the UWS and so,
	 * 	all of the objects returned by these functions).
	 * </i></p>
	 *
	 * @return	A unique job identifier.
	 */
	protected String generateJobId() {
		synchronized (lastId) {
			String generatedId = System.currentTimeMillis() + "A";
			if (lastId != null) {
				while(lastId.equals(generatedId))
					generatedId = generatedId.substring(0, generatedId.length() - 1) + (char)(generatedId.charAt(generatedId.length() - 1) + 1);
			}
			lastId = generatedId;
			return generatedId;
		}
	}

	/**
	 * <p>Gets the value of the specified parameter.</p>
	 *
	 * <p><i>Note:
	 * 	No case sensitivity for the UWS parameters ON THE CONTRARY TO the names
	 * 	of the additional parameters (which are case sensitive).
	 * </i></p>
	 *
	 * @param name	Name of the parameter to get.
	 *
	 * @return	Its value or <i>null</i> if there is no parameter with the given
	 *        	name or if the value is <i>null</i>.
	 *
	 * @see UWSParameters#get(String)
	 */
	public Object getParameter(String name) {
		if (name == null || name.trim().isEmpty())
			return null;

		name = name.trim();
		if (name.equalsIgnoreCase(PARAM_JOB_ID))
			return jobId;
		else if (name.equalsIgnoreCase(PARAM_CREATION_TIME))
			return creationTime;
		else if (name.equalsIgnoreCase(PARAM_OWNER))
			return owner;
		else if (name.equalsIgnoreCase(PARAM_PHASE))
			return phase.getPhase();
		else if (name.equalsIgnoreCase(PARAM_QUOTE))
			return quote;
		else if (name.equalsIgnoreCase(PARAM_START_TIME))
			return startTime;
		else if (name.equalsIgnoreCase(PARAM_END_TIME))
			return endTime;
		else
			return inputParams.get(name);
	}

	/**
	 * Looks for an additional parameters which corresponds to the Execution
	 * Phase. If it exists and:
	 *
	 * <ul>
	 * 	<li>is equals to {@link UWSJob#PHASE_RUN RUN}
	 * 		=> remove it from the attribute {@link #inputParams}
	 * 		   and start the job.</li>
	 * 	<li>is equals to {@link UWSJob#PHASE_ABORT ABORT}
	 * 		=> remove it from the attribute {@link #inputParams}
	 * 		   and abort the job.</li>
	 * 	<li>is another value
	 * 		=> the attribute is though removed from the attribute
	 * 		   {@link #inputParams} but nothing is done.</li>
	 * </ul>
	 *
	 * @param user	The user who asks to apply the phase parameter
	 *            	(start/abort). <i>May be NULL.</i>
	 *
	 * @throws UWSException	If it is impossible the state of this job
	 *                     	(into EXECUTING or ABORTED) or if the given user is
	 *                     	not allowed to execute this job.
	 *
	 * @see UWSParameters#hasInputPhase()
	 * @see UWSParameters#getInputPhase()
	 * @see #start()
	 * @see #abort()
	 */
	public void applyPhaseParam(final JobOwner user) throws UWSException {
		synchronized (inputParams) {
			if (inputParams.hasInputPhase()) {
				String inputPhase = inputParams.getInputPhase();
				if (inputPhase.equalsIgnoreCase(PHASE_RUN)) {
					// Forbids the execution if the user has not the required permission:
					if (user != null && !user.equals(owner) && !user.hasExecutePermission(this))
						throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.executePermissionDenied(user, jobId));
					start();
				} else if (inputPhase.equalsIgnoreCase(PHASE_ABORT)) {
					// Forbids the execution if the user has not the required permission:
					if (user != null && !user.equals(owner) && !user.hasExecutePermission(this))
						throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.executePermissionDenied(user, jobId));
					abort();
				}
			}
		}
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	/**
	 * Gets the file manager used in this job.
	 *
	 * @return	Its file manager or <i>null</i> if this job is not into a
	 *        	{@link JobList} or if this jobs list is not into a {@link UWS}.
	 *
	 * @see JobList#getUWS()
	 * @see uws.service.UWS#getFileManager()
	 */
	public final UWSFileManager getFileManager() {
		if (myJobList != null && myJobList.getUWS() != null)
			return myJobList.getUWS().getFileManager();
		else
			return null;
	}

	/**
	 * Gets the logger of its UWS or a default one if the job list or the UWS
	 * is unknown.
	 *
	 * @return	A logger.
	 *
	 * @see JobList#getUWS()
	 * @see uws.service.UWS#getLogger()
	 * @see UWSToolBox#getDefaultLogger()
	 */
	public UWSLog getLogger() {
		if (myJobList != null && myJobList.getUWS() != null)
			return myJobList.getUWS().getLogger();
		else
			return UWSToolBox.getDefaultLogger();
	}

	/**
	 * Gets the factory to use to create the thread to execute when this job
	 * starts.
	 *
	 * @return	The factory to use to create a {@link JobThread}.
	 */
	public final UWSFactory getFactory() {
		if (myJobList != null && myJobList.getUWS() != null)
			return myJobList.getUWS().getFactory();
		else
			return null;
	}

	/**
	 * Gets the date of the restoration of this job.
	 *
	 * @return	Date of its restoration.
	 */
	public final Date getRestorationDate() {
		return restorationDate;
	}

	/**
	 * Gets the phase in which this job is now.
	 *
	 * @return The current phase of this job.
	 *
	 * @see JobPhase#getPhase()
	 */
	public final ExecutionPhase getPhase() {
		return phase.getPhase();
	}

	/**
	 * Sets the current phase of this job.
	 *
	 * <p><b>IMPORTANT:</b></p>
	 * <ul>
	 * 	<li>
	 * 		The order of all phases must be respected. By default:
	 * 		{@link ExecutionPhase#PENDING PENDING} --->
	 * 		{@link ExecutionPhase#QUEUED QUEUED} --->
	 * 		{@link ExecutionPhase#EXECUTING EXECUTING} --->
	 * 		{@link ExecutionPhase#COMPLETED COMPLETED}.</li>
	 * 	<li>
	 * 		The only way to go to the {@link ExecutionPhase#EXECUTING EXECUTING}
	 * 		phase is by sending a POST query with the value
	 * 		{@link UWSJob#PHASE_RUN RUN} for the parameter
	 * 		{@link UWSJob#PARAM_PHASE PHASE}.</li>
	 * 	<li>
	 * 		The only way to go to the {@link ExecutionPhase#ABORTED ABORTED}
	 * 		phase is by sending a POST query with the value
	 * 		{@link UWSJob#PHASE_ABORT ABORT} for the parameter
	 * 		{@link UWSJob#PARAM_PHASE PHASE}.</li>
	 * 	<li>
	 * 		The start time and the end time are set automatically when the phase
	 * 		is set to {@link ExecutionPhase#EXECUTING EXECUTING} and
	 * 		{@link ExecutionPhase#COMPLETED COMPLETED},
	 * 		{@link ExecutionPhase#ABORTED ABORTED}
	 * 		or {@link ExecutionPhase#ERROR ERROR}</li>
	 * </ul>
	 *
	 * @param p	The phase to set for this job.
	 *
	 * @throws UWSException If the given phase does not respect the job's
	 *                     	phases order.
	 *
	 * @see #setPhase(ExecutionPhase, boolean)
	 */
	public final void setPhase(ExecutionPhase p) throws UWSException {
		setPhase(p, false);
	}

	/**
	 * Sets the current phase of this job, respecting or not the imposed order.
	 *
	 * <p><b>IMPORTANT:</b></p>
	 * <ul>
	 * 	<li>
	 * 		<b><u>If the parameter <i>force</i> is <i>false</i></u></b>,
	 * 		the order of all phases must be respected:
	 * 		{@link ExecutionPhase#PENDING PENDING} --->
	 * 		{@link ExecutionPhase#QUEUED QUEUED} --->
	 * 		{@link ExecutionPhase#EXECUTING EXECUTING} --->
	 * 		{@link ExecutionPhase#COMPLETED COMPLETED}.</li>
	 * 	<li>
	 * 		The only way to go to the {@link ExecutionPhase#EXECUTING EXECUTING}
	 * 		phase is by sending a POST query with the value
	 * 		{@link UWSJob#PHASE_RUN RUN} for the parameter
	 * 		{@link UWSJob#PARAM_PHASE PARAM_PHASE}.</li>
	 * 	<li>
	 * 		The only way to go to the {@link ExecutionPhase#ABORTED ABORTED}
	 * 		phase is by sending a POST query with the value
	 * 		{@link UWSJob#PHASE_ABORT ABORT} for the parameter
	 * 		{@link UWSJob#PARAM_PHASE PARAM_PHASE}.</li>
	 * 	<li>
	 * 		The start time and the end time are set automatically when the phase
	 * 		is set to {@link ExecutionPhase#EXECUTING EXECUTING} and
	 * 		{@link ExecutionPhase#COMPLETED COMPLETED},
	 * 		{@link ExecutionPhase#ABORTED ABORTED}
	 * 		or {@link ExecutionPhase#ERROR ERROR}</li>
	 *</ul>
	 *
	 * @param p		The phase to set for this job.
	 * @param force	<i>true</i> to impose the given execution phase,
	 *             	<i>false</i> to take into account the order of all phases.
	 *
	 * @throws UWSException If the given phase does not respect the job's
	 *                     	phases order.
	 *
	 * @see JobPhase#setPhase(ExecutionPhase, boolean)
	 * @see JobPhase#isFinished()
	 * @see ExecutionManager#remove(UWSJob)
	 * @see #notifyObservers(ExecutionPhase)
	 */
	public final void setPhase(ExecutionPhase p, boolean force) throws UWSException {
		synchronized (phase) {
			ExecutionPhase oldPhase = phase.getPhase();
			phase.setPhase(p, force);

			if (!force)
				getLogger().logJob(LogLevel.INFO, this, "CHANGE_PHASE", "The job \"" + getJobId() + "\" goes from " + oldPhase + " to " + p, null);

			// Notify the execution manager:
			if (phase.isFinished() && getJobList() != null)
				getJobList().getExecutionManager().remove(this);

			// Notify all the observers:
			notifyObservers(oldPhase);
		}
	}

	/**
	 * Gets the phase manager of this job.
	 *
	 * <p><i>Note:
	 * 	The phase manager manages all the transitions between all the execution
	 * 	phases.
	 * </i></p>
	 *
	 * @return	Its phase manager.
	 */
	public final JobPhase getPhaseManager() {
		return phase;
	}

	/**
	 * Sets the phase manager of this job.
	 *
	 * <p><i>Note:
	 * 	The phase manager manages all the transitions between all the execution
	 * 	phases.
	 * </i></p>
	 *
	 * @param jobPhase	Its new phase manager (if <i>null</i> this function does
	 *                	nothing).
	 */
	public final void setPhaseManager(JobPhase jobPhase) {
		if (jobPhase != null) {
			synchronized (phase) {
				phase = jobPhase;
			}
		}
	}

	/**
	 * Gets the time at which the job execution has started.
	 *
	 * @return The start time of the execution of this job.
	 */
	public final Date getStartTime() {
		return startTime;
	}

	/**
	 * Sets the time at which the job execution has started.
	 *
	 * @param newDateTime	The start time of the execution of this job.
	 */
	protected final void setStartTime(Date newDateTime) {
		startTime = newDateTime;
	}

	/**
	 * Gets the time at which the job execution has finished.
	 *
	 * @return The end time of the execution of this job.
	 */
	public final Date getEndTime() {
		return endTime;
	}

	/**
	 * Sets the time at which the job execution has finished.
	 *
	 * @param newDateTime	The end time of the execution of this job.
	 */
	protected final void setEndTime(Date newDateTime) {
		endTime = newDateTime;

		// Save the owner jobs list:
		if (phase.isFinished() && owner != null && getJobList() != null && getJobList().getUWS() != null && getJobList().getUWS().getBackupManager() != null)
			getJobList().getUWS().getBackupManager().saveOwner(owner);

		// Log the end of this job:
		getLogger().logJob(LogLevel.INFO, this, "END", "Job \"" + jobId + "\" ended with the status " + phase, null);
	}

	/**
	 * Gets the duration (in seconds) for which this job shall run.
	 *
	 * @return The execution duration of this job.
	 *
	 * @see UWSParameters#getExecutionDuration()
	 */
	public final long getExecutionDuration() {
		return inputParams.getExecutionDuration();
	}

	/**
	 * Sets the duration (in seconds) for which this job shall run ONLY IF the
	 * job can updated (considering its current execution phase, see
	 * {@link JobPhase#isJobUpdatable()}).
	 *
	 * <p><i>Note:
	 * 	A duration of 0 (or less) implies unlimited execution duration.
	 * </i></p>
	 *
	 * @param executionDuration The execution duration of this job.
	 *
	 * @see UWSParameters#set(String, Object)
	 */
	public final void setExecutionDuration(long executionDuration) {
		if (phase.isJobUpdatable()) {
			try {
				inputParams.set(PARAM_EXECUTION_DURATION, executionDuration);
			} catch(UWSException ue) {
				;
			}
		}
	}

	/**
	 * Gets the instant when the job shall be destroyed.
	 *
	 * @return The destruction time of this job.
	 *
	 * @see UWSParameters#getDestructionTime()
	 */
	public final Date getDestructionTime() {
		return inputParams.getDestructionTime();
	}

	/**
	 * Sets the instant when the job shall be destroyed ONLY IF the job can
	 * updated (considering its current execution phase, see
	 * {@link JobPhase#isJobUpdatable()}). If known the jobs list is notify of
	 * this destruction time update.
	 *
	 * @param destructionTime	The destruction time of this job.
	 *                       	<i>MUST NOT be NULL</i>
	 *
	 * @see JobList#updateDestruction(UWSJob)
	 * @see UWSParameters#set(String, Object)
	 */
	public final void setDestructionTime(Date destructionTime) {
		if (destructionTime != null && phase.isJobUpdatable()) {
			try {
				inputParams.set(PARAM_DESTRUCTION_TIME, destructionTime);
				if (myJobList != null)
					myJobList.updateDestruction(this);
			} catch(UWSException ue) {
				getLogger().logJob(LogLevel.WARNING, this, "SET_DESTRUCTION", "Can not set the destruction time of the job \"" + getJobId() + "\" to \"" + destructionTime + "\"!", ue);
			}
		}
	}

	/**
	 * Gets the error that occurs during the execution of this job.
	 *
	 * @return A summary of the error.
	 */
	public final ErrorSummary getErrorSummary() {
		return errorSummary;
	}

	/**
	 * Sets the error that occurs during the execution of this job.
	 *
	 * <p><b>IMPORTANT:</b>
	 * 	This function will have no effect if the job is finished, that is to say
	 * 	if the current phase is {@link ExecutionPhase#ABORTED ABORTED},
	 * 	{@link ExecutionPhase#ERROR ERROR},
	 * 	{@link ExecutionPhase#COMPLETED COMPLETED}
	 * 	or {@link ExecutionPhase#ARCHIVED ARCHIVED}.
	 * </p>
	 *
	 * @param errorSummary	A summary of the error. <i>MUST NOT be NULL</i>
	 *
	 * @throws UWSException	If the job execution is finished that is to say if
	 *                     	the phase is ABORTED, ERROR, COMPLETED or ARCHIVED.
	 *
	 * @see #isFinished()
	 */
	public final void setErrorSummary(ErrorSummary errorSummary) throws UWSException {
		if (errorSummary == null)
			return;
		else if (!isFinished())
			this.errorSummary = errorSummary;
		else {
			getLogger().logJob(LogLevel.ERROR, this, "SET_ERROR", "Can not set an error summary when the job is finished (or not yet started)! The current phase is: " + getPhase() + " ; the summary of the error to set is: \"" + errorSummary.message + "\".", null);
			throw new UWSException(UWSException.NOT_ALLOWED, UWSExceptionFactory.jobModificationForbidden(jobId, getPhase(), "ERROR SUMMARY"));
		}
	}

	/**
	 * Gets the ID of this job (this ID <b>MUST</b> be unique).
	 *
	 * @return The job ID (unique).
	 */
	public final String getJobId() {
		return jobId;
	}

	/**
	 * Gets the RunID of this job given by the UWS user (presumed to be the
	 * owner of this job). This ID isn't the one used to access to this job
	 * thanks to the jobs list: it is more likely a label/name than an ID
	 * => it is not unique.
	 *
	 * <p><b>WARNING:</b>
	 * 	This ID may be used by other jobs BUT their job id
	 * 	(cf {@link UWSJob#getJobId()}) must be different.
	 * </p>
	 *
	 * @return The Run ID (a kind of job name/label).
	 *
	 * @see UWSParameters#getRunId()
	 */
	public final String getRunId() {
		return inputParams.getRunId();
	}

	/**
	 * Sets the RunID of this job ONLY IF the job can updated (considering
	 * its current execution phase, see {@link JobPhase#isJobUpdatable()}).
	 *
	 * @param name	Its name/label.
	 *
	 * @see JobPhase#isJobUpdatable()
	 *
	 * @see UWSParameters#set(String, Object)
	 */
	public final void setRunId(String name) {
		if (!phase.isFinished()) {
			try {
				inputParams.set(PARAM_RUN_ID, name);
			} catch(UWSException ue) {
				;
			}
		}
	}

	/**
	 * Gets the owner of this job.
	 *
	 * @return The owner.
	 */
	public final JobOwner getOwner() {
		return owner;
	}

	/**
	 * Get the quote attribute of this job.
	 *
	 * @return The estimated duration of the job execution (in seconds).
	 */
	public final long getQuote() {
		return quote;
	}

	/**
	 * Sets the quote attribute of this job ONLY IF the job is not yet
	 * finished according to its current status (i.e.
	 * {@link JobPhase#isFinished()}).
	 *
	 * <p><i>Note:</i>
	 * 	A negative or NULL value will be considered as 'no quote for this job'.
	 * 	One could use the constant {@link #QUOTE_NOT_KNOWN}
	 * 	(= {@value #QUOTE_NOT_KNOWN}) for this exact purpose.
	 * </p>
	 *
	 * @param nbSeconds	The estimated duration of the job execution
	 *                 	(in seconds).
	 */
	public final void setQuote(long nbSeconds) {
		if (!phase.isFinished())
			quote = nbSeconds;
	}

	/**
	 * Gets the list of parameters' name.
	 *
	 * @return	The additional parameters of this job.
	 *
	 * @see UWSParameters#getNames()
	 */
	public final Set<String> getAdditionalParameters() {
		return inputParams.getAdditionalParameters().keySet();
	}

	/**
	 * Gets the number of additional parameters.
	 *
	 * @return	Number of additional parameters.
	 */
	public final int getNbAdditionalParameters() {
		return inputParams.getAdditionalParameters().size();
	}

	/**
	 * Gets the value of the specified additional parameter.
	 *
	 * @param paramName	The name of the parameter whose the value is wanted.
	 *
	 * @return	The value of the specified parameter
	 *        	or <i>null</i> if it doesn't exist.
	 */
	public final Object getAdditionalParameterValue(String paramName) {
		return inputParams.getAdditionalParameters().get(paramName);
	}

	/**
	 * Adds or updates the specified parameter with the given value ONLY IF the
	 * job can be updated (considering its current execution phase, see
	 * {@link JobPhase#isJobUpdatable()}).
	 *
	 * <p><i><b>Important note:</b>
	 * 	If the given parameter value is an {@link UploadFile} and that it is
	 * 	impossible to move it close to the job, this parameter will be removed.
	 * 	No error is thrown, but a warning message is logged.
	 * </i></p>
	 *
	 * @param paramName		The name of the parameter to add or to update.
	 * @param paramValue	The (new) value of the specified parameter.
	 *
	 * @return	<i>true</i> if the parameter has been successfully added/updated,
	 *        	<i>false</i> otherwise <i>(particularly if paramName=null or
	 *        	paramName="" or paramValue=null)</i>.
	 *
	 * @throws UWSException	If a parameter value is incorrect.
	 *
	 * @see JobPhase#isJobUpdatable()
	 */
	public final boolean addOrUpdateParameter(String paramName, Object paramValue) throws UWSException {
		return addOrUpdateParameter(paramName, paramValue, null);
	}

	/**
	 * Adds or updates the specified parameter with the given value ONLY IF the
	 * job can be updated (considering its current execution phase, see
	 * {@link JobPhase#isJobUpdatable()}).
	 *
	 * <p><i><b>Important note:</b>
	 * 	If the given parameter value is an {@link UploadFile} and that it is
	 * 	impossible to move it close to the job, this parameter will be removed.
	 * 	No error is thrown, but a warning message is logged.
	 * </i></p>
	 *
	 * @param paramName		The name of the parameter to add or to update.
	 * @param paramValue	The (new) value of the specified parameter.
	 * @param user			The user who asks for this update.
	 *
	 * @return	<i>true</i> if the parameter has been successfully added/updated,
	 *        	<i>false</i> otherwise <i>(particularly if paramName=null or
	 *        	paramName="" or paramValue=null)</i>.
	 *
	 * @throws UWSException	If a parameter value is incorrect.
	 *
	 * @since 4.1
	 *
	 * @see JobPhase#isJobUpdatable()
	 */
	public final boolean addOrUpdateParameter(String paramName, Object paramValue, final JobOwner user) throws UWSException {
		if (paramValue != null && !phase.isFinished()) {

			// Set the parameter:
			inputParams.set(paramName, paramValue);

			// CASE DESTRUCTION_TIME: update the thread dedicated to the destruction:
			if (paramValue.equals(PARAM_DESTRUCTION_TIME)) {
				if (myJobList != null)
					myJobList.updateDestruction(this);
			}
			// DEFAULT: test whether the parameter is a file, and if yes, move it in a location related to this job:
			else {
				if (paramValue != null && paramValue instanceof UploadFile) {
					try {
						((UploadFile)paramValue).move(this);
					} catch(IOException ioe) {
						getLogger().logJob(LogLevel.WARNING, this, "MOVE_UPLOAD", "Can not move an uploaded file in the job \"" + jobId + "\"!", ioe);
						inputParams.remove(paramName);
					}
				}
			}

			// Apply the retrieved phase:
			applyPhaseParam(user);

			return true;
		} else
			return false;
	}

	/**
	 * Adds or updates the given parameters ONLY IF the job can be updated
	 * (considering its current execution phase, see
	 * {@link JobPhase#isJobUpdatable()}).
	 *
	 * <p>
	 * 	At the end of this function, the method
	 * 	{@link #applyPhaseParam(JobOwner)} is called so that if there is an
	 * 	additional parameter {@link #PARAM_PHASE} with the value:
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		{@link UWSJob#PHASE_RUN RUN} then the job is starting and the phase
	 * 		goes to {@link ExecutionPhase#EXECUTING EXECUTING}.</li>
	 * 	<li>
	 * 		{@link UWSJob#PHASE_ABORT ABORT} then the job is aborting.</li>
	 * 	<li>
	 * 		otherwise the parameter {@link UWSJob#PARAM_PHASE PARAM_PHASE} is
	 * 		removed from {@link UWSJob#inputParams inputParams} and nothing is
	 * 		done.</li>
	 * </ul>
	 *
	 * <p><i><b>Important note:</b>
	 * 	If a given parameter value is an {@link UploadFile} and that it is
	 * 	impossible to move it close to the job, this parameter will be removed.
	 * 	No error is thrown, but a warning message is logged.
	 * </i></p>
	 *
	 * @param params	A list of parameters to add/update.
	 * @return	<i>true</i> if all the given parameters have been successfully
	 *        	added/updated,
	 *        	<i>false</i> if some parameters have not been managed.
	 *
	 * @throws UWSException	If a parameter value is incorrect.
	 *
	 * @see #addOrUpdateParameters(UWSParameters, JobOwner)
	 */
	public boolean addOrUpdateParameters(UWSParameters params) throws UWSException {
		return addOrUpdateParameters(params, null);
	}

	/**
	 * Gets the creation date/time of this job.
	 *
	 * @return	The job creation date/time.
	 *
	 * @since 4.3
	 */
	public final Date getCreationTime() {
		return creationTime;
	}

	/**
	 * Adds or updates the given parameters ONLY IF the job can be updated
	 * (considering its current execution phase, see
	 * {@link JobPhase#isJobUpdatable()}).
	 *
	 * <p>
	 * 	At the end of this function, the method
	 * 	{@link #applyPhaseParam(JobOwner)} is called so that if there is an
	 * 	additional parameter {@link #PARAM_PHASE} with the value:
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		{@link UWSJob#PHASE_RUN RUN} then the job is starting and the phase
	 * 		goes to {@link ExecutionPhase#EXECUTING EXECUTING}.</li>
	 * 	<li>
	 * 		{@link UWSJob#PHASE_ABORT ABORT} then the job is aborting.</li>
	 * 	<li>
	 * 		otherwise the parameter {@link UWSJob#PARAM_PHASE PARAM_PHASE} is
	 * 		removed from {@link UWSJob#inputParams inputParams} and nothing is
	 * 		done.</li>
	 * </ul></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	If a given parameter value is an {@link UploadFile} and that it is
	 * 	impossible to move it close to the job, this parameter will be removed.
	 * 	No error is thrown, but a warning message is logged.
	 * </i></p>
	 *
	 * @param params	The UWS parameters to update.
	 * @param user		The user who asks for this update.
	 *
	 * @return	<i>true</i> if all the given parameters have been successfully
	 *        	added/updated,
	 *        	<i>false</i> if some parameters have not been managed.
	 *
	 * @throws UWSException	If a parameter value is incorrect or if the given
	 *                     	user can not update or execute this job.
	 *
	 * @see JobPhase#isJobUpdatable()
	 * @see #applyPhaseParam(JobOwner)
	 */
	public boolean addOrUpdateParameters(UWSParameters params, final JobOwner user) throws UWSException {
		// The job can be modified ONLY IF in PENDING phase:
		if (!phase.isJobUpdatable())
			throw new UWSException(UWSException.BAD_REQUEST, "Forbidden parameters modification: the job is not any more in the PENDING phase!");

		// Forbids the update if the user has not the required permission:
		if (user != null && !user.equals(owner) && !user.hasWritePermission(this))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, false, getJobId()));

		// Load all parameters:
		String[] updated = inputParams.update(params);

		// If the destruction time has been updated, the modification must be propagated to the jobs list:
		Object newValue;
		for(String updatedParam : updated) {
			// CASE DESTRUCTION_TIME: update the thread dedicated to the destruction:
			if (updatedParam.equals(PARAM_DESTRUCTION_TIME)) {
				if (myJobList != null)
					myJobList.updateDestruction(this);
			}
			// DEFAULT: test whether the parameter is a file, and if yes, move it in a location related to this job:
			else {
				newValue = inputParams.get(updatedParam);
				if (newValue != null && newValue instanceof UploadFile) {
					try {
						((UploadFile)newValue).move(this);
					} catch(IOException ioe) {
						getLogger().logJob(LogLevel.WARNING, this, "MOVE_UPLOAD", "Can not move an uploaded file in the job \"" + jobId + "\"!", ioe);
						inputParams.remove(updatedParam);
					}
				}
			}
		}

		// Apply the retrieved phase:
		applyPhaseParam(user);

		return (updated.length == params.size());
	}

	/**
	 * Removes the specified additional parameter ONLY IF the job can be updated
	 * (considering its current execution phase, see
	 * {@link JobPhase#isJobUpdatable()}).
	 *
	 * @param paramName	The name of the parameter to remove.
	 *
	 * @return	<i>true</i> if the parameter has been successfully removed,
	 *        	<i>false</i> otherwise.
	 *
	 * @see JobPhase#isJobUpdatable()
	 * @see UWSParameters#remove(String)
	 */
	public final boolean removeAdditionalParameter(String paramName) {
		if (phase.isFinished() || paramName == null)
			return false;
		else {
			// Remove the parameter from the map:
			Object removed = inputParams.remove(paramName);
			// If the parameter value was an uploaded file, delete it physically:
			if (removed != null && removed instanceof UploadFile) {
				try {
					((UploadFile)removed).deleteFile();
				} catch(IOException ioe) {
					getLogger().logJob(LogLevel.WARNING, this, "MOVE_UPLOAD", "Can not delete the uploaded file \"" + paramName + "\" of the job \"" + jobId + "\"!", ioe);
				}
			}
			return true;
		}
	}

	/**
	 * Gets the results list of this job.
	 *
	 * @return An iterator on the results list.
	 */
	public final Iterator<Result> getResults() {
		return results.values().iterator();
	}

	/**
	 * Gets the specified result.
	 *
	 * @param resultId	ID of the result to return.
	 *
	 * @return			The corresponding result.
	 */
	public final Result getResult(String resultId) {
		return results.get(resultId);
	}

	/**
	 * Gets the total number of results.
	 *
	 * @return	The number of results.
	 */
	public final int getNbResults() {
		return results.size();
	}

	/**
	 * Adds the given result in the results list of this job.
	 *
	 * <p><b>IMPORTANT:</b>
	 * 	This function will throw an error if the job is finished.
	 * </p>
	 *
	 * @param res	The result to add (<b>not null</b>).
	 *
	 * @return	<i>true</i> if the result has been successfully added,
	 *        	<i>false</i> otherwise (for instance, if a result has the same
	 *        	ID).
	 *
	 * @throws UWSException	If the job execution is finished that is to say if
	 *                     	the phase is ABORTED, ERROR, COMPLETED or ARCHIVED.
	 *
	 * @see #isFinished()
	 */
	public boolean addResult(Result res) throws UWSException {
		if (res == null)
			return false;
		else if (isFinished()) {
			UWSException ue = new UWSException(UWSException.NOT_ALLOWED, UWSExceptionFactory.jobModificationForbidden(getJobId(), getPhase(), "RESULT"));
			getLogger().logJob(LogLevel.ERROR, this, "ADD_RESULT", "Can not add the result \"" + res.getId() + "\" to the job \"" + getJobId() + "\": this job is already finished (or not yet started). Current phase: " + getPhase(), ue);
			throw ue;
		} else {
			synchronized (results) {
				if (results.containsKey(res.getId()))
					return false;
				else {
					results.put(res.getId(), res);
					return true;
				}
			}
		}
	}

	/**
	 * Get the additional information about this job.
	 *
	 * @return	Additional info. about this job,
	 *        	or NULL if there is none.
	 *
	 * @since 4.2
	 */
	public final JobInfo getJobInfo() {
		return jobInfo;
	}

	/**
	 * Set the additional information about this job.
	 *
	 * <p><i>Note:
	 * 	By default, this function replaces the current {@link JobInfo}
	 * 	of this job by the given one (even if NULL). This behavior
	 * 	can be changed by overwriting this function and by returning the
	 * 	extended {@link UWSJob} in the used {@link UWSFactory}.
	 * </i></p>
	 *
	 * <p><b>Important note:</b>
	 * 	When attributing a {@link JobInfo} to a {@link UWSJob}, you
	 * 	may have to call {@link JobInfo#setJob(UWSJob)} on the former
	 * 	and the new jobInfo (see the default implementation for an example)
	 * 	for some implementations of {@link JobInfo}.
	 * </p>
	 *
	 * @param newJobInfo	The new additional info. about this job.
	 *                  	<i>NULL is allowed and should be used to remove a
	 *                  	JobInfo from a job.</i>
	 *
	 * @since 4.2
	 */
	public void setJobInfo(final JobInfo newJobInfo) {
		// Cut the link between the former jobInfo and this job:
		if (this.jobInfo != null)
			this.jobInfo.setJob(null);

		// Establish a link between the new jobInfo and this job:
		if (newJobInfo != null)
			newJobInfo.setJob(this);

		// Replace the former jobInfo by the given one:
		this.jobInfo = newJobInfo;
	}

	/**
	 * Gets the execution manager of this job, if any.
	 *
	 * @return	Its execution manager (may be <i>null</i>).
	 */
	public final ExecutionManager getExecutionManager() {
		return getJobList().getExecutionManager();
	}

	/**
	 * Gets its jobs list, if known.
	 *
	 * @return	Its jobs list (may be <i>null</i>).
	 */
	public final JobList getJobList() {
		return myJobList;
	}

	/**
	 * Sets its jobs list.
	 *
	 * <p><i>Note 1:
	 * 	A job can change its jobs list ONLY WHILE PENDING!
	 * </i></p>
	 * <p><i>Note 2:
	 * 	This job is removed from its previous job list, if there is one.
	 * </i></p>
	 * <p><i>Note 3:
	 * 	This job is NOT automatically added into the new jobs list. Indeed, this
	 * 	function should be called by {@link JobList#addNewJob(UWSJob)}.
	 * </i></p>
	 *
	 * @param jobList	Its new jobs list.
	 *               	<i>Note: if NULL, nothing is done!</i>
	 *
	 * @throws IllegalStateException	If this job is not PENDING.
	 *
	 * @see JobList#removeJob(String)
	 * @see JobList#getJob(String)
	 */
	protected final void setJobList(final JobList jobList) throws IllegalStateException {
		if (jobList == null)
			return;
		else if (myJobList != null && jobList.equals(myJobList))
			return;
		else if (myJobList == null || phase.getPhase() == ExecutionPhase.PENDING) {
			if (myJobList != null && myJobList.getJob(jobId) != null)
				myJobList.removeJob(jobId);
			myJobList = jobList;
		} else
			throw new IllegalStateException("Impossible to move a job (here: " + jobId + ") from a jobs list (here: " + ((myJobList == null) ? "null" : myJobList.getName()) + ") to another (here: " + ((jobList == null) ? "null" : jobList.getName()) + ") if the job is not PENDING !");
	}

	/**
	 * Gets the UWS URL of this job in function of its jobs list.
	 *
	 * @return	Its corresponding UWSUrl.
	 *
	 * @see JobList#getUrl()
	 * @see UWSUrl#jobSummary(String, String)
	 */
	public final UWSUrl getUrl() {
		if (myJobList != null) {
			UWSUrl url = myJobList.getUrl();
			if (url != null)
				return url.jobSummary(myJobList.getName(), jobId);
		}
		return null;
	}

	/* ******************** */
	/* EXECUTION MANAGEMENT */
	/* ******************** */
	/**
	 * Gets the time to wait for the end of the thread after an interruption.
	 *
	 * @return	The time to wait for the end of the thread (a negative or null
	 *          value means no wait for the end of the thread).
	 */
	public final long getTimeToWaitForEnd() {
		return waitForStop;
	}

	/**
	 * Sets the time to wait for the end of the thread after an interruption.
	 *
	 * @param timeToWait	The new time to wait for the end of the thread (a
	 *                  	negative or null value means no wait for the end of
	 *                  	the thread).
	 */
	public final void setTimeToWaitForEnd(long timeToWait) {
		waitForStop = timeToWait;
	}

	/**
	 * Starts the job by using the execution manager if any.
	 *
	 * @throws UWSException
	 */
	public final void start() throws UWSException {
		start(getJobList() != null);
	}

	/**
	 * Starts the job.
	 *
	 * <p><i>Note:
	 * 	This function does nothing if the job is already running!
	 * </i></p>
	 *
	 * @param useManager	<i>true</i> to let the execution manager deciding
	 *                  	whether the job starts immediately or whether it
	 *                  	must be put in a queue until enough resources are
	 *                  	available,
	 *                  	<i>false</i> to start the execution immediately.
	 *
	 * @throws NullPointerException	If this job is not associated with a job
	 *                             	list or the associated job list is not part
	 *                             	of a UWS service or if no thread is created.
	 * @throws UWSException			If there is an error while changing the
	 *                     			execution phase or when starting the
	 *                     			corresponding thread.
	 *
	 * @see #isRunning()
	 * @see UWSFactory#createJobThread(UWSJob)
	 * @see ExecutionManager#execute(UWSJob)
	 * @see #setPhase(ExecutionPhase)
	 * @see #isFinished()
	 * @see #startTime
	 */
	public void start(boolean useManager) throws UWSException {
		// This job must know its jobs list and this jobs list must know its UWS:
		if (myJobList == null || myJobList.getUWS() == null)
			throw new IllegalStateException("A UWSJob can not start if it is not linked to a job list or if its job list is not linked to a UWS.");

		// If already running do nothing:
		else if (isRunning())
			return;

		// If asked propagate this request to the execution manager:
		else if (useManager) {
			getJobList().getExecutionManager().execute(this);

		}// Otherwise start directly the execution:
		else {
			// Create its corresponding thread:
			thread = getFactory().createJobThread(this);
			if (thread == null)
				throw new NullPointerException("Missing job work! The thread created by the factory is NULL => The job can't be executed!");

			// Change the job phase:
			setPhase(ExecutionPhase.QUEUED);
			setPhase(ExecutionPhase.EXECUTING);

			// Set the start time:
			setStartTime(new Date());

			// Run the job:
			thread.start();
			(new JobTimeOut()).start();

			// Log the start of this job:
			getLogger().logJob(LogLevel.INFO, this, "START", "Job \"" + jobId + "\" started.", null);
		}
	}

	/**
	 * Stop/Cancel this job when its maximum execution duration has been reached.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 4.1 (09/2014)
	 */
	protected final class JobTimeOut extends Thread {
		public JobTimeOut() {
			super(JobThread.tg, "TimeOut_" + jobId);
		}

		@Override
		public void run() {
			long maxDuration = getExecutionDuration();
			if (thread != null && thread.isAlive() && maxDuration != UNLIMITED_DURATION && maxDuration > 0) {
				try {
					thread.join(maxDuration * 1000);
					if (!isFinished())
						UWSJob.this.abort();
				} catch(InterruptedException ie) {
					/* Not needed to report any interruption while waiting. */
				} catch(UWSException ue) {
					getLogger().logJob(LogLevel.WARNING, UWSJob.this, "EXECUTING", "Unexpected error while waiting the end of the execution of the job \"" + jobId + "\" (thread ID: " + thread.getId() + ")!", ue);
				}
			}
		}
	}

	/**
	 * Tells whether the job is still running.
	 *
	 * <p><i>Note:
	 * 	This function tests the execution phase (see
	 * 	{@link JobPhase#isExecuting()}) AND the status of the thread (see
	 * 	{@link #isStopped()}).
	 * </i></p>
	 *
	 * @return	<i>true</i> if the job is still running,
	 *        	<i>false</i> otherwise.
	 *
	 * @see JobPhase#isExecuting()
	 * @see #isStopped()
	 */
	public final boolean isRunning() {
		return phase.isExecuting() && !isStopped();
	}

	/**
	 * Tells whether the job is already finished (completed, aborted,
	 * error, archived, ...).
	 *
	 * <p><i>Note:
	 * 	This function test the execution phase (see
	 * 	{@link JobPhase#isFinished()}) AND the status of the thread (see
	 * 	{@link #isStopped()}).
	 * </i></p>
	 *
	 * @return	<i>true</i> if the job is finished,
	 *        	<i>false</i> otherwise.
	 *
	 * @see JobPhase#isFinished()
	 * @see #isStopped()
	 */
	public final boolean isFinished() {
		return phase.isFinished() && isStopped();
	}

	/**
	 * Stops immediately the job, sets its phase to
	 * {@link ExecutionPhase#ABORTED ABORTED} and sets its end time.
	 *
	 * <p><b>IMPORTANT:</b>
	 * 	If the thread does not stop immediately the phase and the end time are
	 * 	not modified. However it can be done by calling one more time
	 * 	{@link #abort()}. Besides you should check that you test regularly the
	 * 	interrupted flag of the thread in {@link JobThread#jobWork()}!
	 * </p>
	 *
	 * @throws UWSException	If there is an error while changing the execution
	 *                     	phase.
	 *
	 * @see #stop()
	 * @see #isStopped()
	 * @see #setPhase(ExecutionPhase)
	 * @see #setEndTime(Date)
	 */
	public void abort() throws UWSException {
		// Interrupt the corresponding thread:
		stop();

		if (isStopped()) {
			if (!phase.isFinished()) {
				// Try to change the phase:
				setPhase(ExecutionPhase.ABORTED);

				// Set the end time:
				setEndTime(new Date());
			} else if ((thread == null || (thread != null && !thread.isAlive())) && phase.getPhase() != ExecutionPhase.ABORTED)
				throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(getJobId(), phase.getPhase(), ExecutionPhase.ABORTED));
		} else
			getLogger().logJob(LogLevel.WARNING, this, "ABORT", "Abortion of the job \"" + getJobId() + "\" asked but not yet effective (after having waited " + waitForStop + "ms)!", null);
	}

	/**
	 * Stops immediately the job, sets its error summary, sets its phase to
	 * {@link ExecutionPhase#ERROR} and sets its end time.
	 *
	 * <p><b>IMPORTANT:</b>
	 * 	If the thread does not stop immediately the phase, the error summary and
	 * 	the end time are not modified. However it can be done by calling one
	 * 	more time {@link #error(ErrorSummary)}. Besides you should check that
	 * 	you test regularly the interrupted flag of the thread in
	 * 	{@link JobThread#jobWork()}!
	 * </p>
	 *
	 * @param error	The error that has interrupted this job.
	 *
	 * @throws UWSException	If there is an error while setting the error summary
	 *                     	or while changing the phase.
	 *
	 * @see #stop()
	 * @see #isStopped()
	 * @see JobPhase#isFinished()
	 * @see #setErrorSummary(ErrorSummary)
	 * @see #setPhase(ExecutionPhase)
	 * @see #setEndTime(Date)
	 */
	public void error(ErrorSummary error) throws UWSException {
		// Interrupt the corresponding thread:
		stop();

		if (isStopped()) {
			if (!phase.isFinished()) {
				// Set the error summary:
				setErrorSummary(error);

				// Try to change phase:
				setPhase(ExecutionPhase.ERROR);

				// Set the end time:
				setEndTime(new Date());
			} else if (thread != null && !thread.isAlive())
				throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(jobId, phase.getPhase(), ExecutionPhase.ERROR));
		} else
			getLogger().logJob(LogLevel.WARNING, this, "ERROR", "Stopping of the job \"" + getJobId() + "\" with error asked but not yet effective (after having waited " + waitForStop + "ms)!", null);
	}

	/** Used by the thread to known whether the {@link #stop()} method has
	 * already been called, and so, that the job is stopping. */
	protected boolean stopping = false;

	/**
	 * Stops the thread that executes the work of this job.
	 */
	protected void stop() {
		if (!isStopped()) {
			synchronized (thread) {
				stopping = true;

				// Interrupts the thread:
				thread.interrupt();

				// Wait a little for its end:
				if (waitForStop > 0) {
					try {
						thread.join(waitForStop);
					} catch(InterruptedException ie) {
						getLogger().logJob(LogLevel.WARNING, this, "END", "Unexpected InterruptedException while waiting for the end of the execution of the job \"" + jobId + "\" (thread ID: " + thread.getId() + ")!", ie);
					}
				}
			}
		}
	}

	/**
	 * Tells whether the thread is different from <i>null</i>, is not alive or
	 * is finished (see {@link JobThread#isFinished()}).
	 *
	 * <p><i><b>Important note:</b>
	 * 	Having the interrupted flag set to <code>true</code> is not enough to
	 * 	consider the job as stopped. So, if the job has been interrupted but is
	 * 	still running, it should mean that the {@link JobThread#jobWork()} does
	 * 	not check the interrupted flag of the thread often enough or not at the
	 * 	right moments. In such case, the job can not be considered as
	 * 	stopped/aborted - so the phase stays
	 * 	{@link ExecutionPhase#EXECUTING EXECUTING} - until the thread is
	 * 	"unblocked" and the interruption is detected.
	 * </i></p>
	 *
	 * @return	<i>true</i> if the thread is not still running,
	 *        	<i>false</i> otherwise.
	 */
	protected final boolean isStopped() {
		return thread == null || !thread.isAlive() || thread.isFinished();
	}

	/**
	 * Archive this job.
	 *
	 * <p>
	 * 	An archive job can not be executed any more. Threads, results and input
	 * 	files are destroyed but the description and the error summary of
	 * 	the job stay unchanged (except the execution phase which will then be
	 * 	{@link ExecutionPhase#ARCHIVED ARCHIVED}).
	 * </p>
	 *
	 * <p><i>Note:
	 * 	The current phase is stored as job information (only if no JobInfo is
	 * 	already set) in order to satisfy the user curiosity (i.e. "in what
	 * 	phase was this job before being archived?").
	 * </i></p>
	 *
	 * @return	<code>true</code> if this job has been successfully archived,
	 *        	<code>false</code> otherwise.
	 *
	 * @throws UWSException	If any error occurs while clearing resources
	 *                     	or changing the phase of this job.
	 *
	 * @since 4.3
	 */
	public boolean archive() {
		/* Interrupt the corresponding thread
		 * and remove results and input files attached to this job: */
		clearResources(false);

		// Ensure this job is no longer in the destruction manager:
		if (getJobList() != null && getJobList().getDestructionManager() != null)
			getJobList().getDestructionManager().remove(this);

		// Change the phase:
		try {
			// store the current phase as additional JobInfo for user curiosity
			//  (only if no JobInfo is already set):
			if (getJobInfo() == null)
				setJobInfo(new SingleValueJobInfo("oldPhase", getPhase().toString()));
			// change phase:
			setPhase(ExecutionPhase.ARCHIVED);
			// log the success of the archiving operation:
			getLogger().logJob(LogLevel.INFO, this, "ARCHIVE", "Job successfully archived!", null);
			return true;
		} catch(UWSException ue) {
			getLogger().logJob(LogLevel.ERROR, this, "ARCHIVE", "Impossible to change the phase of this job into ARCHIVED!", ue);
			return false;
		}
	}

	/**
	 * Stops the job if running, removes the job from the execution manager,
	 * stops the timer for the execution duration.
	 *
	 * <p>
	 * 	Besides, ALL files AND ANY other resources (e.g. thread) associated with
	 * 	this job are destroyed.
	 * </p>
	 *
	 * @see #clearResources(boolean)
	 */
	public void clearResources() {
		clearResources(true);
	}

	/**
	 * Stops the job if running, removes the job from the execution manager,
	 * stops the timer for the execution duration.
	 *
	 * <p>
	 * 	Besides, resources (e.g. thread) associated with this job are freed.
	 * 	Depending on the given parameter, all (<code>true</code>) or just input
	 * 	and result files (<code>false</code>) are destroyed.
	 * </p>
	 *
	 * @param fullClean	Indicate whether all resources or just some input and
	 *                 	result files must be freed.
	 *                  <code>true</code> to stop the job and delete everything
	 *                  (input files, results, jobInfos and error summary),
	 *                  or <code>false</code> to stop the job and delete only
	 *                  all input files and results but not the jobInfos, the
	 *                  error summary and the other parameters.
	 *
	 * @since 4.3
	 */
	public void clearResources(final boolean fullClean) {
		// If still running, abort/stop the job:
		if (!phase.isFinished()) {
			try {
				abort();
			} catch(UWSException e) {
				getLogger().logJob(LogLevel.WARNING, this, "CLEAR_RESOURCES", "Impossible to abort the job \"" + jobId + "\" => trying to stop it...", e);
				stop();
			}
		}

		// Remove this job from its execution manager:
		if (getJobList() != null)
			getJobList().getExecutionManager().remove(this);

		thread = null;

		// Clear all uploaded files:
		Iterator<UploadFile> files = inputParams.getFiles();
		UploadFile upl;
		while(files.hasNext()) {
			upl = files.next();
			try {
				// delete the file:
				upl.deleteFile();
				// delete the internal reference to this input parameter:
				files.remove();
			} catch(IOException ioe) {
				getLogger().logJob(LogLevel.ERROR, this, "CLEAR_RESOURCES", "Impossible to delete the file uploaded as parameter \"" + upl.paramName + "\" (" + upl.getLocation() + ") of the job \"" + jobId + "\"!", null);
			}
		}

		// Clear all results file:
		Iterator<Result> itResults = getResults();
		Result r;
		while(itResults.hasNext()) {
			r = itResults.next();
			try {
				// delete the file:
				getFileManager().deleteResult(r, this);
				// delete the internal reference to this result:
				itResults.remove();
			} catch(IOException ioe) {
				getLogger().logJob(LogLevel.ERROR, this, "CLEAR_RESOURCES", "Impossible to delete the file associated with the result '" + r.getId() + "' of the job \"" + jobId + "\"!", ioe);
			}
		}

		if (fullClean) {
			// Clear the error file:
			if (errorSummary != null && errorSummary.hasDetail()) {
				try {
					// delete the file associated with the error details:
					getFileManager().deleteError(errorSummary, this);
					// delete the error summary:
					errorSummary = null;
				} catch(IOException ioe) {
					getLogger().logJob(LogLevel.ERROR, this, "CLEAR_RESOURCES", "Impossible to delete the file associated with the error '" + errorSummary.message + "' of the job \"" + jobId + "\"!", ioe);
				}
			}

			// Destroy the additional job info.:
			if (jobInfo != null) {
				try {
					jobInfo.destroy();
				} catch(UWSException ue) {
					getLogger().logJob(LogLevel.ERROR, this, "CLEAR_RESOURCES", "Impossible to destroy the additional information about the job \"" + jobId + "\"", ue);
				}
			}
		}

		getLogger().logJob(LogLevel.INFO, this, "CLEAR_RESOURCES", (fullClean ? "All resources" : "Threads and input and result files") + " associated with the job \"" + getJobId() + "\" have been successfully freed.", null);
	}

	/* ******************* */
	/* OBSERVER MANAGEMENT */
	/* ******************* */
	/**
	 * Lets adding an observer of this job. The observer will be notified each
	 * time the execution phase changes.
	 *
	 * @param observer	A new observer of this job.
	 *
	 * @return	<i>true</i> if the given object has been successfully added as
	 *        	observer of this job,
	 *        	<i>false</i> otherwise.
	 */
	public final boolean addObserver(JobObserver observer) {
		if (observer != null && !observers.contains(observer)) {
			observers.add(observer);
			return true;
		} else
			return false;
	}

	/**
	 * Gets the total number of observers this job has.
	 *
	 * @return	Number of its observers.
	 */
	public final int getNbObservers() {
		return observers.size();
	}

	/**
	 * Gets the observers of this job.
	 *
	 * @return	An iterator on the list of its observers.
	 */
	public final Iterator<JobObserver> getObservers() {
		return observers.iterator();
	}

	/**
	 * Lets removing the given object from the list of observers of this job.
	 *
	 * @param observer	The object which must not be considered as observer of
	 *                	this job.
	 *
	 * @return	<i>true</i> if the given object is not any more an observer of
	 *        	this job,
	 *        	<i>false</i> otherwise.
	 */
	public final boolean removeObserver(JobObserver observer) {
		return observers.remove(observer);
	}

	/**
	 * Lets removing all observers of this job.
	 */
	public final void removeAllObservers() {
		observers.clear();
	}

	/**
	 * Notifies all the observer of this job that its phase has changed.
	 *
	 * @param oldPhase		The former phase of this job.
	 * @throws UWSException	If at least one observer can not have been updated.
	 */
	public final void notifyObservers(ExecutionPhase oldPhase) {
		String errors = null;

		for(JobObserver observer : observers) {
			// Update this observer:
			try {
				observer.update(this, oldPhase, getPhase());
			} catch(UWSException ex) {
				if (errors == null)
					errors = "\t* " + ex.getMessage();
				else
					errors += "\n\t* " + ex.getMessage();
			}
		}

		if (errors != null)
			getLogger().logJob(LogLevel.WARNING, this, "NOTIFY", "Some observers of the job \"" + jobId + "\" can not have been updated:\n" + errors, null);
	}

	/* **************** */
	/* ERROR MANAGEMENT */
	/* **************** */
	/**
	 * Gets the error (if any) which has occurred during the job execution.
	 *
	 * <p><i>Note:
	 * 	In the case an error summary can not have been published, the job has no
	 * 	error summary. However the last {@link UWSException} caught during the
	 * 	execution of a {@link JobThread} is saved and is available thanks to
	 * 	{@link JobThread#getError()}. In that case, the
	 * 	{@link UWSJob#getWorkError() getWorkError()} method can be used to get
	 * 	back the occurred error.
	 * </i></p>
	 *
	 * @return	The error which interrupts the thread
	 *        	or <i>null</i> if there was no error or if the job is still
	 *        	running.
	 */
	public final UWSException getWorkError() {
		return (thread == null || !thread.isAlive()) ? null : thread.getError();
	}

	/* ************* */
	/* SERIALIZATION */
	/* ************* */
	@Override
	public String serialize(UWSSerializer serializer, JobOwner user) throws UWSException, Exception {
		if (user != null && !user.equals(getOwner()) && !user.hasReadPermission(this))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.readPermissionDenied(user, false, getJobId()));

		return serializer.getJob(this, true);
	}

	/**
	 * Serializes the specified attribute of this job by using the given
	 * serializer.
	 *
	 * @param attributes	All the given attributes.
	 *                  	<i>May be <i>null</i> or empty.</i>
	 * @param serializer	The serializer to use.
	 *
	 * @return	The serialized job attribute (or the whole job if
	 *        	<i>attributes</i> is an empty array or is <i>null</i>).
	 *
	 * @throws Exception	If there is an unexpected error during the
	 *                  	serialization.
	 *
	 * @see UWSSerializer#getJob(UWSJob, String[], boolean)
	 */
	public String serialize(String[] attributes, UWSSerializer serializer) throws Exception {
		return serializer.getJob(this, attributes, true);
	}

	/**
	 * Serializes the specified attribute of this job in the given output stream
	 * by using the given serializer.
	 *
	 * @param output		The output stream in which the job attribute must be
	 *              		serialized.
	 * @param attributes	The name of the attribute to serialize (if
	 *                  	<i>null</i>, the whole job will be serialized).
	 * @param serializer	The serializer to use.
	 *
	 * @throws Exception	If there is an unexpected error during the
	 *                  	serialization.
	 *
	 * @see #serialize(String[], UWSSerializer)
	 */
	public void serialize(ServletOutputStream output, String[] attributes, UWSSerializer serializer) throws UWSException, IOException, Exception {
		String errorMsgPart = null;
		if (attributes == null || attributes.length <= 0)
			errorMsgPart = "the job \"" + getJobId() + "\"";
		else
			errorMsgPart = "the given attribute \"" + attributes[0] + "\" of the job \"" + getJobId() + "\"";

		if (output == null)
			throw new NullPointerException("Missing serialization output stream when serializing " + errorMsgPart + "!");

		String serialization = serialize(attributes, serializer);
		if (serialization == null) {
			getLogger().logJob(LogLevel.ERROR, this, "SERIALIZE", "Error while serializing " + errorMsgPart + ": NULL was returned.", null);
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Incorrect serialization value (=NULL) ! => impossible to serialize " + errorMsgPart + ".");
		} else {
			output.print(serialization);
			output.flush();
		}
	}

	@Override
	public String toString() {
		return "JOB {jobId: " + jobId + "; phase: " + phase + "; runId: " + getRunId() + "; ownerId: " + owner + "; executionDuration: " + getExecutionDuration() + "; destructionTime: " + getDestructionTime() + "; quote: " + quote + "; NbResults: " + results.size() + "; " + ((errorSummary != null) ? errorSummary.toString() : "No error") + " ; HasJobInfo: \"" + ((jobInfo != null) ? "yes" : "no") + "\"  }";
	}

	@Override
	public int hashCode() {
		return jobId.hashCode();
	}

	/**
	 * 2 instances of AbstractJob are equals ONLY IF their ID are equals.
	 *
	 * <p><i>Note:
	 * 	If the given object is not an AbstractJob, FALSE is returned.
	 * </i></p>
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object anotherJob) {
		if (anotherJob instanceof UWSJob)
			return jobId.equals(((UWSJob)anotherJob).jobId);
		else
			return false;
	}

}
