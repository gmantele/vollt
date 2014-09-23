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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
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

import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;
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

/**
 * <h3>Brief description</h3>
 * 
 * <p>Default implementation of a job of the UWS pattern.</p>
 * 
 * <h3>Some attributes comments</h3>
 * 
 * <ul>
 * 	<li>
 * 		The job attributes <i>startTime</i> and <i>endTime</i> are automatically managed by {@link UWSJob}. You don't have to do anything !
 * 		However you can customize the used date/time format thanks to the function {@link #setDateFormat(DateFormat)}. The default date/time format is:
 * 		<i>yyyy-MM-dd'T'HH:mm:ss.SSSZ</i>
 * 	</li>
 * 	<br />
 * 	<li>Once set, the <i>destruction</i> and the <i>executionDuration</i> attributes are automatically managed. That is to say:
 * 		<ul>
 * 			<li><u>if the destruction time is reached:</u> the job stops and it is destroyed by its job list</li>
 * 			<li><u>if the execution duration is elapsed:</u> the job stops and the phase is put to {@link ExecutionPhase#ABORTED ABORTED}.</li>
 * 		</ul>
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<u>The <i>owner</i> attribute is set at the job creation and can not be changed after</u> ! If no owner is given at the job creation,
 * 		its default value is <i>null</i>.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		If your job is executable, do not forget to set the <i>quote</i> parameter
 * 		ONLY by using the {@link #setQuote(long)} method (a negative value or {@link #QUOTE_NOT_KNOWN} value
 * 		indicates the quote is not known ; {@link #QUOTE_NOT_KNOWN} is the default value).
 * 	</li>
 * </ul>
 * 
 * <h3>More details</h3>
 * 
 * <ul>
 * 	<li>
 * 		<b>{@link #generateJobId()}:</b>
 * 					This function is called at the construction of any {@link UWSJob}. It allows to generate a unique job ID.
 * 					By default: time (in milliseconds) + a upper-case letter (A, B, C, ....).
 * 					<u>If you want customizing the job ID of your jobs</u>, you need to override this function.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<b>{@link #loadAdditionalParams()}:</b>
 * 					All parameters that are not managed by default are automatically stored in the job attribute {@link #additionalParameters} (a map).
 * 					However if you want manage yourself some or all of these additional parameters (i.e. task parameters), you must override this method.
 * 					<i>(By default nothing is done.)</i>
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<b>{@link #clearResources()}:</b>
 * 					This method is called <u>only at the destruction of the job</u>.
 * 					By default, the job is stopped (if running), thread resources are freed,
 * 					the job is removed from its jobs list and result/error files are deleted.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<b>{@link #setPhaseManager(JobPhase)}:</b>
 * 					Lets customizing the default behaviors of all the execution phases for any job instance.
 * 					For more details see {@link JobPhase}.
 * 	</li>
 * 	<br />
 * 	<li>
 * 		<b>{@link #addObserver(JobObserver)}:</b>
 * 					An instance of any kind of AbstractJob can be observed by objects which implements {@link JobObserver} (i.e. {@link uws.service.UWSService}).
 * 					Observers are notified at any change of the execution phase.
 * 	</li>
 * </ul>
 * 
 * @author	Gr&eacute;gory Mantelet (CDS;ARI)
 * @version	4.1 (08/2014)
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

	/** Default value of {@link #owner} if no ID are given at the job creation. */
	public final static String ANONYMOUS_OWNER = "anonymous";

	/** Default date format pattern.  */
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	/** The quote value that indicates the quote of this job is not known. */
	public static final long QUOTE_NOT_KNOWN = -1;

	/** The duration that implies an unlimited execution duration. */
	public final static long UNLIMITED_DURATION = 0;

	/* ********* */
	/* VARIABLES */
	/* ********* */
	/** The last generated job ID. <b>It SHOULD be used ONLY by the function {@link #generateJobId()} !</b> */
	protected static String lastId = null;

	/** The identifier of the job (it MUST be different from any other job).<BR />
	 * <i><u>Note:</u> It is assigned automatically at the job creation in any job constructor
	 * by the function {@link #generateJobId()}.
	 * To change the way this ID is generated or its format you must override this function.</i> */
	protected final String jobId;

	/** The identifier of the creator of this job.<BR />
	 * <i><u>Note:</u> This object will not exist for all invocations of the UWS conformant protocol,
	 * but only in cases where the access to the service is authenticated.</i> */
	protected final JobOwner owner;

	/** The jobs list which is supposed to managed this job. */
	private JobList myJobList = null;

	/**
	 * <p>The current phase of the job.</p>
	 * <i><u>Remember:</u> A job is treated as a state machine thanks to this attribute.
	 * <ul>
	 * 	<li>A successful job will normally progress through the {@link ExecutionPhase#PENDING PENDING},
	 * 		{@link ExecutionPhase#QUEUED QUEUED}, {@link ExecutionPhase#EXECUTING EXECUTING}, {@link ExecutionPhase#COMPLETED COMPLETED}
	 * 		phases in that order.</li>
	 * 	<li>At any time before the {@link ExecutionPhase#COMPLETED COMPLETED} phase a job may either be {@link ExecutionPhase#ABORTED ABORTED}
	 * 		or may suffer an {@link ExecutionPhase#ERROR ERROR}.</li>
	 * 	<li>If the UWS reports an {@link ExecutionPhase#UNKNOWN UNKNOWN} phase, then all the client can do is re-query the phase until a known phase is reported.</li>
	 * 	<li>A UWS may place a job in a {@link ExecutionPhase#HELD HELD} phase on receipt of a PHASE=RUN request it for some reason the job cannot be immediately queued
	 * 	- in this case it is the responsibility of the client to request PHASE=RUN again at some later time.</li>
	 * </ul></i>
	 */
	private JobPhase phase;

	/** The used date formatter. */
	public static final DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

	/**
	 * This time (in seconds) predicts when the job is likely to complete.<br />
	 * <b>It CAN NOT be changed after the job creation !<br />
	 * <i>By default if no ID is given, {@link #quote} is set to {@link #QUOTE_NOT_KNOWN} (= {@value #QUOTE_NOT_KNOWN}).</i></b>
	 */
	private long quote = QUOTE_NOT_KNOWN;

	/** The time at which the job execution started. */
	private Date startTime = null;

	/** The time at which the job execution ended. */
	private Date endTime = null;

	/** <p>This error summary gives a human-readable error message for the underlying job.</p>
	 * <i><u>Note:</u> This object is intended to be a detailed error message, and consequently,
	 * might be a large piece of text such as a stack trace.</i> */
	protected ErrorSummary errorSummary = null;

	/** This is a list of all results of this job. */
	protected Map<String,Result> results;

	/** List of all input parameters (UWS standard and non-standard parameters). */
	protected final UWSParameters inputParams;

	/** The thread to start for executing the job. */
	protected transient JobThread thread = null;

	/** The time (in ms) to wait the end of the thread after an interruption. */
	protected long waitForStop = 1000;

	/** Objects which want to be notified at each modification of the execution phase of this job. */
	private Vector<JobObserver> observers = new Vector<JobObserver>();

	/** If this job has been restored, this attribute should be set with the date of its restoration. */
	private final Date restorationDate;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * <p>Builds a job with no owner from a map of all parameters (UWS and additional parameters).</p>
	 * 
	 * <p><i><u>Note:</u> if the parameter {@link UWSJob#PARAM_PHASE} (</i>phase<i>) is given with the value {@link UWSJob#PHASE_RUN}
	 * the job execution starts immediately after the job has been added to a job list or after {@link #applyPhaseParam(JobOwner)} is called.</i></p>
	 * 
	 * @param params	UWS standard and non-standard parameters.
	 * 
	 * @see UWSJob#AbstractJob(String, Map)
	 */
	public UWSJob(final UWSParameters params){
		this(null, params);
	}

	/**
	 * <p>Builds a job of the given owner and from a map of all parameters (UWS and additional parameters).</p>
	 * 
	 * <p><i><u>Note:</u> if the parameter {@link #PARAM_PHASE} (</i>phase<i>) is given with the value {@link #PHASE_RUN}
	 * the job execution starts immediately after the job has been added to a job list or after {@link #applyPhaseParam(JobOwner)} is called.</i></p>
	 * 
	 * @param owner		Job.owner ({@link #PARAM_OWNER}).
	 * @param params	UWS standard and non-standard parameters.
	 * 
	 * @see #loadDefaultParams(Map)
	 * @see #loadAdditionalParams()
	 */
	public UWSJob(JobOwner owner, final UWSParameters params){
		this.owner = owner;

		phase = new JobPhase(this);

		results = new HashMap<String,Result>();

		inputParams = params;
		inputParams.init();

		jobId = generateJobId();
		restorationDate = null;
	}

	/**
	 * <p><b>CONSTRUCTOR TO USE TO RESTORE A JOB whatever is its phase.</b></p>
	 * 
	 * <p>Builds a job of the given owner with all the given parameter.</p>
	 * 
	 * <p><i>
	 * 	<u>Note:</u> The job phase is automatically set in function of the last parameters (startTime, endTime, results and error).
	 * 	Only the following execution phase are possible: PENDING, ABORTED, ERROR and COMPLETED.
	 * </i></p>
	 * 
	 * @param jobID			The ID of this job (NOT NULL).
	 * @param owner			Its owner.
	 * @param params		UWS standard and non-standard parameters.
	 * @param quote			Its quote (in seconds).
	 * @param startTime		Its start time if it has already been started.
	 * @param endTime		Its end time if it is already finished.
	 * @param results		Its results (if phase=COMPLETED).
	 * @param error			Its error (if phase=ERROR).
	 * 
	 * @throws NullPointerException	If the given ID is NULL.
	 */
	public UWSJob(final String jobID, final JobOwner owner, final UWSParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws NullPointerException{
		if (jobID == null)
			throw new NullPointerException("Missing job ID => impossible to build a Job without a valid ID!");

		this.jobId = jobID;
		this.owner = owner;

		this.quote = quote;

		if (startTime > 0)
			this.startTime = new Date(startTime);
		if (endTime > 0)
			this.endTime = new Date(endTime);

		this.results = new HashMap<String,Result>();
		if (results != null){
			for(Result r : results){
				if (r != null)
					this.results.put(r.getId(), r);
			}
		}

		errorSummary = error;

		this.phase = new JobPhase(this);

		inputParams = params;
		params.init();

		ExecutionPhase p = ExecutionPhase.PENDING;
		if (startTime > 0 && endTime > 0){
			if (this.results.isEmpty() && this.errorSummary == null)
				p = ExecutionPhase.ABORTED;
			else if (!this.results.isEmpty())
				p = ExecutionPhase.COMPLETED;
			else if (this.errorSummary != null)
				p = ExecutionPhase.ERROR;
		}
		if (phase != null){
			try{
				setPhase(p, true);
			}catch(UWSException ue){
				// Can never append because the "force" parameter is true! 
			}
		}

		restorationDate = new Date();
	}

	/**
	 * <p>This function lets generating a unique ID.</p>
	 * 
	 * <p><i><b>By default:</b> System.currentTimeMillis()+UpperCharacter (UpperCharacter: one upper-case character: A, B, C, ....)</i></p>
	 * 
	 * <p><i><u>note: </u> DO NOT USE in this function any of the following functions: {@link #getLogger()},
	 * {@link #getFileManager()} and {@link #getFactory()}. All of them will return NULL, because this job does not
	 * yet know its jobs list (which is needed to know the UWS and so, all of the objects returned by these functions).</i></p>
	 * 
	 * @return	A unique job identifier.
	 */
	protected String generateJobId(){
		String generatedId = System.currentTimeMillis() + "A";
		if (lastId != null){
			while(lastId.equals(generatedId))
				generatedId = generatedId.substring(0, generatedId.length() - 1) + (char)(generatedId.charAt(generatedId.length() - 1) + 1);
		}
		lastId = generatedId;
		return generatedId;
	}

	/**
	 * <p>Gets the value of the specified parameter.</p>
	 * 
	 * <p><i><u>note:</u> No case sensitivity for the UWS parameters ON THE CONTRARY TO the names of the additional parameters (which are case sensitive).</i></p>
	 * 
	 * @param name	Name of the parameter to get.
	 * 
	 * @return		Its value or <i>null</i> if there is no parameter with the given name or if the value is <i>null</i>.
	 * 
	 * @see UWSParameters#get(String)
	 */
	public Object getParameter(String name){
		if (name == null || name.trim().isEmpty())
			return null;

		name = name.trim();
		if (name.equalsIgnoreCase(PARAM_JOB_ID))
			return jobId;
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
	 * <p>Looks for an additional parameters which corresponds to the Execution Phase. If it exists and:</p>
	 * <ul>
	 * 	<li> is equals to {@link UWSJob#PHASE_RUN RUN} => remove it from the attribute {@link #additionalParameters} and start the job.</li>
	 * 	<li> is equals to {@link UWSJob#PHASE_ABORT ABORT} => remove it from the attribute {@link #additionalParameters} and abort the job.</li>
	 * 	<li> is another value => the attribute stays in the attribute {@link #additionalParameters} and nothing is done.</li>
	 * </ul>
	 * 
	 * @param user			The user who asks to apply the phase parameter (start/abort). (may be NULL)
	 * 
	 * @throws UWSException	If it is impossible the state of this job (into EXECUTING or ABORTED)
	 * 						or if the given user is not allowed to execute this job.
	 * 
	 * @see UWSParameters#hasInputPhase()
	 * @see UWSParameters#getInputPhase()
	 * @see #start()
	 * @see #abort()
	 */
	public void applyPhaseParam(final JobOwner user) throws UWSException{
		synchronized(inputParams){
			if (inputParams.hasInputPhase()){
				String inputPhase = inputParams.getInputPhase();
				if (inputPhase.equalsIgnoreCase(PHASE_RUN)){
					// Forbids the execution if the user has not the required permission:
					if (user != null && !user.equals(owner) && !user.hasExecutePermission(this))
						throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.executePermissionDenied(user, jobId));
					start();
				}else if (inputPhase.equalsIgnoreCase(PHASE_ABORT)){
					// Forbids the execution if the user has not the required permission:
					if (user != null && !user.equals(owner) && !user.hasExecutePermission(this))
						throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.executePermissionDenied(user, jobId));
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
	 * @return	Its file manager or <i>null</i> if this job is not into a {@link JobList} or if this jobs list is not into a {@link UWS}.
	 * 
	 * @see JobList#getUWS()
	 * @see uws.service.UWS#getFileManager()
	 */
	public final UWSFileManager getFileManager(){
		if (myJobList != null && myJobList.getUWS() != null)
			return myJobList.getUWS().getFileManager();
		else
			return null;
	}

	/**
	 * Gets the logger of its UWS or a default one if the job list or the UWS is unknown.
	 * 
	 * @return	A logger.
	 * 
	 * @see JobList#getUWS()
	 * @see uws.service.UWS#getLogger()
	 * @see UWSToolBox#getDefaultLogger()
	 */
	public UWSLog getLogger(){
		if (myJobList != null && myJobList.getUWS() != null)
			return myJobList.getUWS().getLogger();
		else
			return UWSToolBox.getDefaultLogger();
	}

	/**
	 * Gets the factory to use to create the thread to execute when this job starts.
	 * 
	 * @return	The factory to use to create a {@link JobThread}.
	 */
	public final UWSFactory getFactory(){
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
	public final Date getRestorationDate(){
		return restorationDate;
	}

	/**
	 * Gets the phase in which this job is now.
	 * 
	 * @return The current phase of this job.
	 * 
	 * @see JobPhase#getPhase()
	 */
	public final ExecutionPhase getPhase(){
		return phase.getPhase();
	}

	/**
	 * <p>Sets the current phase of this job.</p>
	 * 
	 * <p><b><u>IMPORTANT:</u></b>
	 * <ul><li>The order of all phases must be respected:<i> BY DEFAULT</i> <BR /> {@link ExecutionPhase#PENDING PENDING} ---> {@link ExecutionPhase#QUEUED QUEUED} ---> {@link ExecutionPhase#EXECUTING EXECUTING} ---> {@link ExecutionPhase#COMPLETED COMPLETED}.</li>
	 * 	<li>The only way to go to the {@link ExecutionPhase#EXECUTING EXECUTING} phase is by sending a POST query with the value {@link UWSJob#PHASE_RUN RUN} for the parameter {@link UWSJob#PARAM_PHASE PHASE}.</li>
	 * 	<li>The only way to go to the {@link ExecutionPhase#ABORTED ABORTED} phase is by sending a POST query with the value {@link UWSJob#PHASE_ABORT ABORT} for the parameter {@link UWSJob#PARAM_PHASE PHASE}.</li>
	 * 	<li>The start time and the end time are set automatically when the phase is set to {@link ExecutionPhase#EXECUTING EXECUTING} and {@link ExecutionPhase#COMPLETED COMPLETED}, {@link ExecutionPhase#ABORTED ABORTED} or {@link ExecutionPhase#ERROR ERROR}</li>
	 *</ul></p>
	 * 
	 * @param p					The phase to set for this job.
	 * 
	 * @throws UWSException 	If the given phase does not respect the job's phases order.
	 * 
	 * @see #setPhase(ExecutionPhase, boolean)
	 */
	public final void setPhase(ExecutionPhase p) throws UWSException{
		setPhase(p, false);
	}

	/**
	 * <p>Sets the current phase of this job, respecting or not the imposed order.</p>
	 * 
	 * <p><b><u>IMPORTANT:</u></b>
	 * <ul><li><b><u>If the parameter <i>force</i> is <i>false</i></u></b>, the order of all phases must be respected:<BR /> {@link ExecutionPhase#PENDING PENDING} ---> {@link ExecutionPhase#QUEUED QUEUED} ---> {@link ExecutionPhase#EXECUTING EXECUTING} ---> {@link ExecutionPhase#COMPLETED COMPLETED}.</li>
	 * 	<li>The only way to go to the {@link ExecutionPhase#EXECUTING EXECUTING} phase is by sending a POST query with the value {@link UWSJob#PHASE_RUN RUN} for the parameter {@link UWSJob#PARAM_PHASE PARAM_PHASE}.</li>
	 * 	<li>The only way to go to the {@link ExecutionPhase#ABORTED ABORTED} phase is by sending a POST query with the value {@link UWSJob#PHASE_ABORT ABORT} for the parameter {@link UWSJob#PARAM_PHASE PARAM_PHASE}.</li>
	 * 	<li>The start time and the end time are set automatically when the phase is set to {@link ExecutionPhase#EXECUTING EXECUTING} and {@link ExecutionPhase#COMPLETED COMPLETED}, {@link ExecutionPhase#ABORTED ABORTED} or {@link ExecutionPhase#ERROR ERROR}</li>
	 *</ul></p>
	 * 
	 * @param p		 The phase to set for this job.
	 * @param force	<i>true</i> to impose the given execution phase, <i>false</i> to take into account the order of all phases.
	 * 
	 * @throws UWSException If the given phase does not respect the job's phases order.
	 * 
	 * @see JobPhase#setPhase(ExecutionPhase, boolean)
	 * @see JobPhase#isFinished()
	 * @see ExecutionManager#remove(UWSJob)
	 * @see #notifyObservers(ExecutionPhase)
	 */
	public final void setPhase(ExecutionPhase p, boolean force) throws UWSException{
		synchronized(phase){
			ExecutionPhase oldPhase = phase.getPhase();
			phase.setPhase(p, force);

			getLogger().logJob(LogLevel.INFO, this, "CHANGE_PHASE", "The job \"" + getJobId() + "\" goes from " + oldPhase + " to " + p, null);

			// Notify the execution manager:
			if (phase.isFinished() && getJobList() != null)
				getJobList().getExecutionManager().remove(this);

			// Notify all the observers:
			notifyObservers(oldPhase);
		}
	}

	/**
	 * <p>Gets the phase manager of this job.</p>
	 * 
	 * <p><i><u>Note:</u> The phase manager manages all the transitions between all the execution phases.</i></p>
	 * 
	 * @return	Its phase manager.
	 */
	public final JobPhase getPhaseManager(){
		return phase;
	}

	/**
	 * <p>Sets the phase manager of this job.</p>
	 * 
	 * <p><i><u>Note:</u> The phase manager manages all the transitions between all the execution phases.</i></p>
	 * 
	 * @param jobPhase	Its new phase manager (if <i>null</i> this function does nothing).
	 */
	public final void setPhaseManager(JobPhase jobPhase){
		if (jobPhase != null){
			synchronized(phase){
				phase = jobPhase;
			}
		}
	}

	/**
	 * Gets the time at which the job execution has started.
	 * 
	 * @return The start time of the execution of this job.
	 */
	public final Date getStartTime(){
		return startTime;
	}

	/**
	 * Sets the time at which the job execution has started.
	 * 
	 * @param newDateTime	The start time of the execution of this job.
	 */
	protected final void setStartTime(Date newDateTime){
		startTime = newDateTime;
	}

	/**
	 * Gets the time at which the job execution has finished.
	 * 
	 * @return The end time of the execution of this job.
	 */
	public final Date getEndTime(){
		return endTime;
	}

	/**
	 * Sets the time at which the job execution has finished.
	 * 
	 * @param newDateTime	The end time of the execution of this job.
	 */
	protected final void setEndTime(Date newDateTime){
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
	public final long getExecutionDuration(){
		return inputParams.getExecutionDuration();
	}

	/**
	 * <p>Sets the duration (in seconds) for which this job shall run ONLY IF the job can updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).</p>
	 * 
	 * <p><i><u>Note:</u> A duration of 0 (or less) implies unlimited execution duration.</i></p>
	 * 
	 * @param executionDuration The execution duration of this job.
	 * 
	 * @see UWSParameters#set(String, Object)
	 */
	public final void setExecutionDuration(long executionDuration){
		if (phase.isJobUpdatable()){
			try{
				inputParams.set(PARAM_EXECUTION_DURATION, executionDuration);
			}catch(UWSException ue){
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
	public final Date getDestructionTime(){
		return inputParams.getDestructionTime();
	}

	/**
	 * <p>
	 * 	Sets the instant when the job shall be destroyed ONLY IF the job can updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).
	 * 	If known the jobs list is notify of this destruction time update.
	 * </p>
	 * 
	 * @param destructionTime The destruction time of this job.
	 * 
	 * @see JobList#updateDestruction(UWSJob)
	 * @see UWSParameters#set(String, Object)
	 */
	public final void setDestructionTime(Date destructionTime){
		if (phase.isJobUpdatable()){
			try{
				inputParams.set(PARAM_DESTRUCTION_TIME, destructionTime);
				if (myJobList != null)
					myJobList.updateDestruction(this);
			}catch(UWSException ue){
				getLogger().logJob(LogLevel.WARNING, this, "SET_DESTRUCTION", "Can not set the destruction time of the job \"" + getJobId() + "\" to \"" + destructionTime + "\"!", ue);
			}
		}
	}

	/**
	 * Gets the error that occurs during the execution of this job.
	 * 
	 * @return A summary of the error.
	 */
	public final ErrorSummary getErrorSummary(){
		return errorSummary;
	}

	/**
	 * <p>Sets the error that occurs during the execution of this job.</p>
	 * 
	 * <p><b><u>IMPORTANT:</u> This function will have no effect if the job is finished, that is to say if the current phase is
	 * {@link ExecutionPhase#ABORTED ABORTED}, {@link ExecutionPhase#ERROR ERROR} or {@link ExecutionPhase#COMPLETED COMPLETED}.</i>.</b></p>
	 * 
	 * @param errorSummary	A summary of the error.
	 * 
	 * @throws UWSException	If the job execution is finished that is to say if the phase is ABORTED, ERROR or COMPLETED.
	 * 
	 * @see #isFinished()
	 */
	public final void setErrorSummary(ErrorSummary errorSummary) throws UWSException{
		if (!isFinished())
			this.errorSummary = errorSummary;
		else{
			UWSException ue = new UWSException(UWSException.NOT_ALLOWED, UWSExceptionFactory.jobModificationForbidden(jobId, getPhase(), "ERROR SUMMARY"));
			getLogger().logJob(LogLevel.ERROR, this, "SET_ERROR", "Can not set an error summary when the job is finished (or not yet started)! The current phase is: " + getPhase() + " ; the summary of the error to set is: \"" + errorSummary.message + "\".", ue);
			throw ue;
		}
	}

	/**
	 * Gets the ID of this job (this ID <b>MUST</b> be unique).
	 * 
	 * @return The job ID (unique).
	 */
	public final String getJobId(){
		return jobId;
	}

	/**
	 * <p>Gets the RunID of this job given by the UWS user (presumed to be the owner of this job).
	 * This ID isn't the one used to access to this job thanks to the jobs list: it is more likely a label/name than an ID => it is not unique.</p>
	 * 
	 * <p><b><u>Warning:</u> This ID may be used by other jobs BUT their job id (cf {@link UWSJob#getJobId()}) must be different.</b></p>
	 * 
	 * @return The Run ID (a kind of job name/label).
	 * 
	 * @see UWSParameters#getRunId()
	 */
	public final String getRunId(){
		return inputParams.getRunId();
	}

	/**
	 * <p>Sets the RunID of this job ONLY IF the job can updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).</p>
	 * 
	 * @param name	Its name/label.
	 * 
	 * @see JobPhase#isJobUpdatable()
	 * 
	 * @see UWSParameters#set(String, Object)
	 */
	public final void setRunId(String name){
		if (!phase.isFinished()){
			try{
				inputParams.set(PARAM_RUN_ID, name);
			}catch(UWSException ue){
				;
			}
		}
	}

	/**
	 * Gets the owner of this job.
	 * 
	 * @return The owner.
	 */
	public final JobOwner getOwner(){
		return owner;
	}

	/**
	 * Get the quote attribute of this job.
	 *
	 * @return The estimated duration of the job execution (in seconds).
	 */
	public final long getQuote(){
		return quote;
	}

	/**
	 * <p>Sets the quote attribute of this job ONLY IF the job can updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).</p>
	 * 
	 * @param nbSeconds	The estimated duration of the job execution (in seconds).
	 * 
	 * @see JobPhase#isJobUpdatable()
	 */
	public final void setQuote(long nbSeconds){
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
	public final Set<String> getAdditionalParameters(){
		return inputParams.getAdditionalParameters().keySet();
	}

	/**
	 * Gets the number of additional parameters.
	 * 
	 * @return	Number of additional parameters.
	 */
	public final int getNbAdditionalParameters(){
		return inputParams.getAdditionalParameters().size();
	}

	/**
	 * Gets the value of the specified additional parameter.
	 * 
	 * @param paramName	The name of the parameter whose the value is wanted.
	 * @return			The value of the specified parameter or <i>null</i> if it doesn't exist.
	 */
	public final Object getAdditionalParameterValue(String paramName){
		return inputParams.getAdditionalParameters().get(paramName);
	}

	/**
	 * Adds or updates the specified parameter with the given value ONLY IF the job can be updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).
	 * 
	 * @param paramName		The name of the parameter to add or to update.
	 * @param paramValue	The (new) value of the specified parameter.
	 * 
	 * @return				<ul><li><i>true</i> if the parameter has been successfully added/updated,</li>
	 * 						<li><i>false</i> otherwise <i>(particularly if paramName=null or paramName="" or paramValue=null)</i>.</li></ul>
	 * 
	 * @throws UWSException	If a parameter value is incorrect.
	 * 
	 * @see JobPhase#isJobUpdatable()
	 */
	public final boolean addOrUpdateParameter(String paramName, Object paramValue) throws UWSException{
		if (!phase.isFinished()){
			inputParams.set(paramName, paramValue);
			return true;
		}else
			return false;
	}

	/**
	 * <p>Adds or updates the given parameters ONLY IF the job can be updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).</p>
	 * 
	 * <p>Whatever is the result of {@link #loadDefaultParams(Map)} the method {@link #applyPhaseParam()} is called so that if there is an additional parameter {@link #PARAM_PHASE} with the value:
	 * <ul>
	 * 	<li>{@link UWSJob#PHASE_RUN RUN} then the job is starting and the phase goes to {@link ExecutionPhase#EXECUTING EXECUTING}.</li>
	 * 	<li>{@link UWSJob#PHASE_ABORT ABORT} then the job is aborting.</li>
	 * 	<li>otherwise the parameter {@link UWSJob#PARAM_PHASE PARAM_PHASE} remains in the {@link UWSJob#additionalParameters additionalParameters} list.</li>
	 * </ul></p>
	 * 
	 * @param params		A list of parameters to add/update.
	 * @return				<ul><li><i>true</i> if all the given parameters have been successfully added/updated,</li>
	 * 						<li><i>false</i> if some parameters have not been managed.</li></ul>
	 * 
	 * @throws UWSException	If a parameter value is incorrect.
	 * 
	 * @see #addOrUpdateParameters(UWSParameters, JobOwner)
	 */
	public boolean addOrUpdateParameters(UWSParameters params) throws UWSException{
		return addOrUpdateParameters(params, null);
	}

	/**
	 * <p>Adds or updates the given parameters ONLY IF the job can be updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).</p>
	 * 
	 * <p>Whatever is the result of {@link #loadDefaultParams(Map)} the method {@link #applyPhaseParam()} is called so that if there is an additional parameter {@link #PARAM_PHASE} with the value:
	 * <ul>
	 * 	<li>{@link UWSJob#PHASE_RUN RUN} then the job is starting and the phase goes to {@link ExecutionPhase#EXECUTING EXECUTING}.</li>
	 * 	<li>{@link UWSJob#PHASE_ABORT ABORT} then the job is aborting.</li>
	 * 	<li>otherwise the parameter {@link UWSJob#PARAM_PHASE PARAM_PHASE} remains in the {@link UWSJob#additionalParameters additionalParameters} list.</li>
	 * </ul></p>
	 * 
	 * @param params		The UWS parameters to update.
	 * @return				<ul><li><i>true</i> if all the given parameters have been successfully added/updated,</li>
	 * 						<li><i>false</i> if some parameters have not been managed.</li></ul>
	 * 
	 * @throws UWSException	If a parameter value is incorrect or if the given user can not update or execute this job.
	 * 
	 * @see #loadDefaultParams(Map)
	 * @see JobPhase#isJobUpdatable()
	 * @see #loadAdditionalParams()
	 * @see #applyPhaseParam()
	 */
	public boolean addOrUpdateParameters(UWSParameters params, final JobOwner user) throws UWSException{
		// Forbids the update if the user has not the required permission:
		if (user != null && !user.equals(owner) && !user.hasWritePermission(this))
			throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(user, false, getJobId()));

		// Load all parameters:
		String[] updated = inputParams.update(params);

		// If the destruction time has been updated, the modification must be propagated to the jobs list:
		for(String updatedParam : updated){
			if (updatedParam.equals(PARAM_DESTRUCTION_TIME)){
				if (myJobList != null)
					myJobList.updateDestruction(this);
				break;
			}
		}

		// Apply the retrieved phase:
		applyPhaseParam(user);

		return (updated.length == params.size());
	}

	/**
	 * Removes the specified additional parameter ONLY IF the job can be updated (considering its current execution phase, see {@link JobPhase#isJobUpdatable()}).
	 * 
	 * @param paramName	The name of the parameter to remove.
	 * 
	 * @return	<i>true</i> if the parameter has been successfully removed, <i>false</i> otherwise.
	 * 
	 * @see JobPhase#isJobUpdatable()
	 * @see UWSParameters#remove(String)
	 */
	public final boolean removeAdditionalParameter(String paramName){
		if (phase.isFinished() || paramName == null)
			return false;
		else{
			inputParams.remove(paramName);
			return true;
		}
	}

	/**
	 * Gets the results list of this job.
	 * 
	 * @return An iterator on the results list.
	 */
	public final Iterator<Result> getResults(){
		return results.values().iterator();
	}

	/**
	 * Gets the specified result.
	 * 
	 * @param resultId	ID of the result to return.
	 * 
	 * @return			The corresponding result.
	 */
	public final Result getResult(String resultId){
		return results.get(resultId);
	}

	/**
	 * Gets the total number of results.
	 * 
	 * @return	The number of results.
	 */
	public final int getNbResults(){
		return results.size();
	}

	/**
	 * <p>Adds the given result in the results list of this job.</p>
	 * 
	 * <p><b><u>IMPORTANT:</u> This function will throw an error if the job is finished.</b></p>
	 * 
	 * @param res			The result to add (<b>not null</b>).
	 * 
	 * @return				<i>true</i> if the result has been successfully added, <i>false</i> otherwise (for instance, if a result has the same ID).
	 * 
	 * @throws UWSException	If the job execution is finished that is to say if the phase is ABORTED, ERROR or COMPLETED.
	 * 
	 * @see #isFinished()
	 */
	public boolean addResult(Result res) throws UWSException{
		if (res == null)
			return false;
		else if (isFinished()){
			UWSException ue = new UWSException(UWSException.NOT_ALLOWED, UWSExceptionFactory.jobModificationForbidden(getJobId(), getPhase(), "RESULT"));
			getLogger().logJob(LogLevel.ERROR, this, "ADD_RESULT", "Can not add the result \"" + res.getId() + "\" to the job \"" + getJobId() + "\": this job is already finished (or not yet started). Current phase: " + getPhase(), ue);
			throw ue;
		}else{
			synchronized(results){
				if (results.containsKey(res.getId()))
					return false;
				else{
					results.put(res.getId(), res);
					return true;
				}
			}
		}
	}

	/**
	 * Gets the execution manager of this job, if any.
	 * 
	 * @return	Its execution manager (may be <i>null</i>).
	 */
	public final ExecutionManager getExecutionManager(){
		return getJobList().getExecutionManager();
	}

	/**
	 * Gets its jobs list, if known.
	 * 
	 * @return	Its jobs list (may be <i>null</i>).
	 */
	public final JobList getJobList(){
		return myJobList;
	}

	/**
	 * <p>Sets its jobs list.</p>
	 * 
	 * <p><i><u>note 1:</u> a job can change its jobs list ONLY WHILE PENDING !</i></p>
	 * <p><i><u>note 2:</u> this job is removed from its previous job list, if there is one.</i></p>
	 * <p><i><u>note 3:</u> this job is NOT automatically added into the new jobs list. Indeed, this function should be called by {@link JobList#addNewJob(UWSJob)}.</i></p>
	 * 
	 * @param jobList		Its new jobs list. <i><u>note:</u> if NULL, nothing is done !</i>
	 * 
	 * @throws IllegalStateException	If this job is not PENDING.
	 * 
	 * @see JobList#removeJob(String)
	 * @see JobList#getJob(String)
	 */
	protected final void setJobList(final JobList jobList) throws IllegalStateException{
		if (jobList == null)
			return;
		else if (myJobList != null && jobList.equals(myJobList))
			return;
		else if (myJobList == null || phase.getPhase() == ExecutionPhase.PENDING){
			if (myJobList != null && myJobList.getJob(jobId) != null)
				myJobList.removeJob(jobId);
			myJobList = jobList;
		}else
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
	public final UWSUrl getUrl(){
		if (myJobList != null){
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
	 * @return	The time to wait for the end of the thread  (a negative or null value means no wait for the end of the thread).
	 */
	public final long getTimeToWaitForEnd(){
		return waitForStop;
	}

	/**
	 * Sets the time to wait for the end of the thread after an interruption.
	 * 
	 * @param timeToWait	The new time to wait for the end of the thread (a negative or null value means no wait for the end of the thread).
	 */
	public final void setTimeToWaitForEnd(long timeToWait){
		waitForStop = timeToWait;
	}

	/**
	 * <p>Starts the job by using the execution manager if any.</p>
	 * 
	 * @throws UWSException
	 */
	public final void start() throws UWSException{
		start(getJobList() != null);
	}

	/**
	 * <p>Starts the job.</p>
	 * 
	 * <p><i><u>Note:</u> This function does nothing if the job is already running !</i></p>
	 * 
	 * @param useManager	<i>true</i> to let the execution manager deciding whether the job starts immediately or whether it must be put in a queue until enough resources are available, <i>false</i> to start the execution immediately.
	 * 
	 * @throws NullPointerException	If this job is not associated with a job list or the associated job list is not part of a UWS service or if no thread is created.
	 * @throws UWSException			If there is an error while changing the execution phase or when starting the corresponding thread.
	 * 
	 * @see #isRunning()
	 * @see UWSFactory#createJobThread(UWSJob)
	 * @see ExecutionManager#execute(UWSJob)
	 * @see #setPhase(ExecutionPhase)
	 * @see #isFinished()
	 * @see #startTime
	 */
	public void start(boolean useManager) throws UWSException{
		// This job must know its jobs list and this jobs list must know its UWS:
		if (myJobList == null || myJobList.getUWS() == null)
			throw new NullPointerException("A UWSJob can not start if it is not part of a job list or if its job list is not part of a UWS.");

		// If already running do nothing:
		else if (isRunning())
			return;

		// If asked propagate this request to the execution manager:
		else if (useManager){
			getJobList().getExecutionManager().execute(this);

		}// Otherwise start directly the execution:
		else{
			// Create its corresponding thread:
			thread = getFactory().createJobThread(this);
			if (thread == null)
				throw new NullPointerException("Missing job work ! The thread created by the factory is NULL => The job can't be executed !");

			// Change the job phase:
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
		public JobTimeOut(){
			super(JobThread.tg, "TimeOut_" + jobId);
		}

		@Override
		public void run(){
			long maxDuration = getExecutionDuration();
			if (thread != null && thread.isAlive() && maxDuration != UNLIMITED_DURATION && maxDuration > 0){
				try{
					thread.join(maxDuration * 1000);
					if (!isFinished())
						UWSJob.this.abort();
				}catch(InterruptedException ie){
					/* Not needed to report any interruption while waiting. */
				}catch(UWSException ue){
					getLogger().logJob(LogLevel.WARNING, UWSJob.this, "EXECUTING", "Unexpected error while waiting the end of the execution of the job \"" + jobId + "\" (thread ID: " + thread.getId() + ")!", ue);
				}
			}
		}
	}

	/**
	 * <p>Tells whether the job is still running.</p>
	 * 
	 * <p><i><u>Note:</u> This function tests the execution phase (see {@link JobPhase#isExecuting()}) AND the status of the thread (see {@link #isStopped()}).</i></p>
	 * 
	 * @return	<i>true</i> if the job is still running, <i>false</i> otherwise.
	 * 
	 * @see JobPhase#isExecuting()
	 * @see #isStopped()
	 */
	public final boolean isRunning(){
		return phase.isExecuting() && !isStopped();
	}

	/**
	 * <p>Tells whether the job is already finished (completed, aborted, error, ...).</p>
	 * 
	 * <p><i><u>Note:</u> This function test the execution phase (see {@link JobPhase#isFinished()}) AND the status of the thread (see {@link #isStopped()})</i></p>
	 * 
	 * @return	<i>true</i> if the job is finished, <i>false</i> otherwise.
	 * 
	 * @see JobPhase#isFinished()
	 * @see #isStopped()
	 */
	public final boolean isFinished(){
		return phase.isFinished() && isStopped();
	}

	/**
	 * <p>Stops immediately the job, sets its phase to {@link ExecutionPhase#ABORTED ABORTED} and sets its end time.</p>
	 * 
	 * <p><b><u>IMPORTANT:</u> If the thread does not stop immediately the phase and the end time are not modified. However it can be done by calling one more time {@link #abort()}.
	 * Besides you should check that you test regularly the interrupted flag of the thread in {@link #jobWork()} !</b></p>
	 * 
	 * @throws UWSException	If there is an error while changing the execution phase.
	 * 
	 * @see #stop()
	 * @see #isStopped()
	 * @see #setPhase(ExecutionPhase)
	 * @see #setEndTime(Date)
	 */
	public void abort() throws UWSException{
		// Interrupt the corresponding thread:
		stop();

		if (isStopped()){
			if (!phase.isFinished()){
				// Try to change the phase:
				setPhase(ExecutionPhase.ABORTED);

				// Set the end time:
				setEndTime(new Date());
			}else if (thread == null || (thread != null && !thread.isAlive()))
				throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(getJobId(), phase.getPhase(), ExecutionPhase.ABORTED));
		}else
			getLogger().logJob(LogLevel.WARNING, this, "ABORT", "Abortion of the job \"" + getJobId() + "\" asked but not yet effective (after having waited " + waitForStop + "ms)!", null);
	}

	/**
	 * <p>Stops immediately the job, sets its error summary, sets its phase to {@link ExecutionPhase#ERROR} and sets its end time.</p>
	 * 
	 * <p><b><u>IMPORTANT:</u> If the thread does not stop immediately the phase, the error summary and the end time are not modified.
	 * However it can be done by calling one more time {@link #error(ErrorSummary)}.
	 * Besides you should check that you test regularly the interrupted flag of the thread in {@link #jobWork()} !</b></p>
	 * 
	 * @param error			The error that has interrupted this job.
	 * 
	 * @throws UWSException	If there is an error while setting the error summary or while changing the phase.
	 * 
	 * @see #stop()
	 * @see #isStopped()
	 * @see JobPhase#isFinished()
	 * @see #setErrorSummary(ErrorSummary)
	 * @see #setPhase(ExecutionPhase)
	 * @see #setEndTime(Date)
	 */
	public void error(ErrorSummary error) throws UWSException{
		// Interrupt the corresponding thread:
		stop();

		if (isStopped()){
			if (!phase.isFinished()){
				// Set the error summary:
				setErrorSummary(error);

				// Try to change phase:
				setPhase(ExecutionPhase.ERROR);

				// Set the end time:
				setEndTime(new Date());
			}else if (thread != null && !thread.isAlive())
				throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(jobId, phase.getPhase(), ExecutionPhase.ERROR));
		}else
			getLogger().logJob(LogLevel.WARNING, this, "ERROR", "Stopping of the job \"" + getJobId() + "\" with error asked but not yet effective (after having waited " + waitForStop + "ms)!", null);
	}

	/** Used by the thread to known whether the {@link #stop()} method has already been called, and so, that the job is stopping. */
	protected boolean stopping = false;

	/**
	 * Stops the thread that executes the work of this job.
	 */
	protected void stop(){
		if (!isStopped()){
			synchronized(thread){
				stopping = true;

				// Interrupts the thread:
				thread.interrupt();

				// Wait a little for its end:
				if (waitForStop > 0){
					try{
						thread.join(waitForStop);
					}catch(InterruptedException ie){
						getLogger().logJob(LogLevel.WARNING, this, "END", "Unexpected InterruptedException while waiting for the end of the execution of the job \"" + jobId + "\" (thread ID: " + thread.getId() + ")!", ie);
					}
				}
			}
		}
	}

	/**
	 * Tells whether the thread is different from <i>null</i>, is not alive, is interrupted or is finished (see {@link JobThread#isFinished()}).
	 * 
	 * @return	<i>true</i> if the thread is not still running, <i>false</i> otherwise.
	 */
	protected final boolean isStopped(){
		return thread == null || !thread.isAlive() || thread.isInterrupted() || thread.isFinished();
	}

	/**
	 * <p>Stops the job if running, removes the job from the execution manager, stops the timer for the execution duration
	 * and may clear all files or any other resources associated to this job.</p>
	 * 
	 * <p><i>By default the job is aborted, only the {@link UWSJob#thread} attribute is set to null and the timers are stopped; no other operations (i.e. clear result files and error files) is done.</i></p>
	 */
	public void clearResources(){
		// If still running, abort/stop the job:
		if (isRunning()){
			try{
				abort();
			}catch(UWSException e){
				getLogger().logJob(LogLevel.WARNING, this, "CLEAR_RESOURCES", "Impossible to abort the job \"" + jobId + "\" => trying to stop it...", e);
				stop();
			}
		}

		// Remove this job from its execution manager:
		if (getJobList() != null)
			getJobList().getExecutionManager().remove(this);

		thread = null;

		// Clear all results file:
		for(Result r : results.values()){
			try{
				getFileManager().deleteResult(r, this);
			}catch(IOException ioe){
				getLogger().logJob(LogLevel.ERROR, this, "CLEAR_RESOURCES", "Impossible to delete the file associated with the result '" + r.getId() + "' of the job \"" + jobId + "\"!", ioe);
			}
		}

		// Clear the error file:
		if (errorSummary != null && errorSummary.hasDetail()){
			try{
				getFileManager().deleteError(errorSummary, this);
			}catch(IOException ioe){
				getLogger().logJob(LogLevel.ERROR, this, "CLEAR_RESOURCES", "Impossible to delete the file associated with the error '" + errorSummary.message + "' of the job \"" + jobId + "\"!", ioe);
			}
		}

		getLogger().logJob(LogLevel.INFO, this, "CLEAR_RESOURCES", "Resources associated with the job \"" + getJobId() + "\" have been successfully freed.", null);
	}

	/* ******************* */
	/* OBSERVER MANAGEMENT */
	/* ******************* */
	/**
	 * Lets adding an observer of this job. The observer will be notified each time the execution phase changes.
	 * 
	 * @param observer	A new observer of this job.
	 * 
	 * @return			<i>true</i> if the given object has been successfully added as observer of this job, <i>false</i> otherwise.
	 */
	public final boolean addObserver(JobObserver observer){
		if (observer != null && !observers.contains(observer)){
			observers.add(observer);
			return true;
		}else
			return false;
	}

	/**
	 * Gets the total number of observers this job has.
	 * 
	 * @return	Number of its observers.
	 */
	public final int getNbObservers(){
		return observers.size();
	}

	/**
	 * Gets the observers of this job.
	 * 
	 * @return	An iterator on the list of its observers.
	 */
	public final Iterator<JobObserver> getObservers(){
		return observers.iterator();
	}

	/**
	 * Lets removing the given object from the list of observers of this job.
	 * 
	 * @param observer	The object which must not be considered as observer of this job.
	 * 
	 * @return			<i>true</i> if the given object is not any more an observer of this job, <i>false</i> otherwise.
	 */
	public final boolean removeObserver(JobObserver observer){
		return observers.remove(observer);
	}

	/**
	 * Lets removing all observers of this job.
	 */
	public final void removeAllObservers(){
		observers.clear();
	}

	/**
	 * Notifies all the observer of this job that its phase has changed.
	 * 
	 * @param oldPhase		The former phase of this job.
	 * @throws UWSException	If at least one observer can not have been updated.
	 */
	public final void notifyObservers(ExecutionPhase oldPhase){
		int i = 0;
		JobObserver observer = null;
		String errors = null;

		while(i < observers.size()){
			// Gets the observer:
			if (i == 0 && observer == null)
				observer = observers.get(i);
			else if (observer.equals(observers.get(i))){
				i++;
				if (i < observers.size())
					observer = observers.get(i);
				else
					return;
			}
			// Update this observer:
			try{
				observer.update(this, oldPhase, getPhase());
			}catch(UWSException ex){
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
	 * <p>Gets the error (if any) which has occurred during the job execution.</p>
	 * 
	 * <p><i><u>Note:</u> In the case an error summary can not have been published, the job has no error summary.
	 * However the last {@link UWSException} caught during the execution of a {@link JobThread} is saved and is available thanks to {@link JobThread#getError()}.
	 * In that case, the {@link UWSJob#getWorkError() getWorkError()} method can be used to get back the occurred error.</i></p>
	 * 
	 * @return	The error which interrupts the thread or <i>null</i> if there was no error or if the job is still running.
	 */
	public final UWSException getWorkError(){
		return (thread == null || !thread.isAlive()) ? null : thread.getError();
	}

	/* ************* */
	/* SERIALIZATION */
	/* ************* */
	@Override
	public String serialize(UWSSerializer serializer, JobOwner user) throws UWSException, Exception{
		if (user != null && !user.equals(getOwner()) && !user.hasReadPermission(this))
			throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.readPermissionDenied(user, false, getJobId()));

		return serializer.getJob(this, true);
	}

	/**
	 * Serializes the specified attribute of this job by using the given serializer.
	 * 
	 * @param attributes		All the given attributes (may be <i>null</i> or empty).
	 * @param serializer		The serializer to use.
	 * 
	 * @return					The serialized job attribute (or the whole job if <i>attributes</i> is an empty array or is <i>null</i>).
	 * 
	 * @throws Exception		If there is an unexpected error during the serialization.
	 * 
	 * @see UWSSerializer#getJob(UWSJob, String[], boolean)
	 */
	public String serialize(String[] attributes, UWSSerializer serializer) throws Exception{
		return serializer.getJob(this, attributes, true);
	}

	/**
	 * Serializes the specified attribute of this job in the given output stream by using the given serializer.
	 * 
	 * @param output			The output stream in which the job attribute must be serialized.
	 * @param attributes		The name of the attribute to serialize (if <i>null</i>, the whole job will be serialized).
	 * @param serializer		The serializer to use.
	 * 
	 * @throws Exception		If there is an unexpected error during the serialization.
	 * 
	 * @see #serialize(String[], UWSSerializer)
	 */
	public void serialize(ServletOutputStream output, String[] attributes, UWSSerializer serializer) throws UWSException, IOException, Exception{
		String errorMsgPart = null;
		if (attributes == null || attributes.length <= 0)
			errorMsgPart = "the job \"" + getJobId() + "\"";
		else
			errorMsgPart = "the given attribute \"" + attributes[0] + "\" of the job \"" + getJobId() + "\"";

		if (output == null)
			throw new NullPointerException("Missing serialization output stream when serializing " + errorMsgPart + "!");

		String serialization = serialize(attributes, serializer);
		if (serialization == null){
			getLogger().logJob(LogLevel.ERROR, this, "SERIALIZE", "Error while serializing " + errorMsgPart + ": NULL was returned.", null);
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Incorrect serialization value (=NULL) ! => impossible to serialize " + errorMsgPart + ".");
		}else{
			output.print(serialization);
			output.flush();
		}
	}

	@Override
	public String toString(){
		return "JOB {jobId: " + jobId + "; phase: " + phase + "; runId: " + getRunId() + "; ownerId: " + owner + "; executionDuration: " + getExecutionDuration() + "; destructionTime: " + getDestructionTime() + "; quote: " + quote + "; NbResults: " + results.size() + "; " + ((errorSummary != null) ? errorSummary.toString() : "No error") + " }";
	}

	@Override
	public int hashCode(){
		return jobId.hashCode();
	}

	/**
	 * <p>2 instances of AbstractJob are equals ONLY IF their ID are equals.</p>
	 * 
	 * <p><i><u>Note:</u> If the given object is not an AbstractJob, FALSE is returned.</i></p>
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object anotherJob){
		if (anotherJob instanceof UWSJob)
			return jobId.equals(((UWSJob)anotherJob).jobId);
		else
			return false;
	}

}
