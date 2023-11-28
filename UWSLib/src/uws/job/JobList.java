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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletOutputStream;

import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;
import uws.job.manager.DefaultDestructionManager;
import uws.job.manager.DefaultExecutionManager;
import uws.job.manager.DestructionManager;
import uws.job.manager.ExecutionManager;
import uws.job.serializer.UWSSerializer;
import uws.job.serializer.filter.JobListRefiner;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.UserIdentifier;
import uws.service.backup.UWSBackupManager;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * <h3>General description</h3>
 *
 * <p>
 * 	An instance of this class lets listing UWS jobs (possible actions: get, add
 * 	and remove).
 * </p>
 *
 * <p><i>Note:
 * 	This list implements the interface {@link Iterable} which lets you iterating
 * 	more easily among the jobs.
 * </i></p>
 *
 * <h3>Jobs list by user</h3>
 *
 * <p>
 * 	So that avoiding any user to interact on jobs of anybody else, it is
 * 	possible (and strongly encouraged) to get the list of jobs only of the
 * 	current user. For that you must use the function {@link #getJobs(JobOwner)}
 * 	with a owner ID.
 * </p>
 *
 * <p>
 * 	At each request sent to any instance of {@link UWSService} the function
 * 	{@link UserIdentifier#extractUserId(uws.service.UWSUrl, javax.servlet.http.HttpServletRequest)}
 * 	may extract the user ID. Thus the action GetJobList may know who is the
 * 	current user. If the extracted owner ID is different from <i>null</i> only
 * 	the jobs of the current user will be returned.
 * </p>
 * <p><b>WARNING:</b>
 * 	You need to find a way to identify for each UWS request the current user and
 * 	then to override correctly the function
 * 	{@link UserIdentifier#extractUserId(uws.service.UWSUrl, javax.servlet.http.HttpServletRequest)}.
 * </p>
 *
 * <p><i>Note:
 * 	If no owner is specified (NULL value), all jobs are concerned by the
 * 	function action.
 * </i></p>
 *
 * <h3>Searching in a jobs list</h3>
 *
 * <ul>
 * 	<li><b>{@link #getJobs()} or {@link #iterator()}:</b> to get all the jobs of
 * 	                                                      this jobs list.</li>
 * 	<li><b>{@link #getJob(String)}:</b> to get the job that has the given
 * 	                                    jobID.</li>
 * 	<li><b>{@link #searchJobs(String)}:</b> to search all the jobs that have the
 * 	                                        given runID.</li>
 * 	<li><b>{@link #getJobs(JobOwner)}:</b> to get the jobs of the specified
 * 	                                        user.</li>
 * </ul>
 *
 * <h3>Get the users list</h3>
 *
 * <p>
 * 	If you are interested in (probably for some statistics or for an
 * 	administrator) you can ask the list of users who have at least one job in
 * 	this jobs list ({@link #getUsers()}) and known how many they are
 * 	({@link #getNbUsers()}).
 * </p>
 *
 * <h3>Execution management</h3>
 *
 * <p>
 * 	The execution of the jobs of this jobs list is managed by an implementation
 * 	of {@link ExecutionManager}. {@link DefaultExecutionManager} is used by
 * 	default, but you can easily set your own implementation of this interface,
 * 	either at the job list creation or with
 * 	{@link #setExecutionManager(ExecutionManager)}.
 * </p>
 *
 * <h3>Automatic job destruction</h3>
 *
 * <p>
 * 	A job has a field - destructionTime - which indicates the date at which it
 * 	must be destroyed. Remember that destroying a job consists in removing it
 * 	from its jobs list and then clearing all its resources (result and input
 * 	files, threads, ...). This task is done by an "instance" of the interface
 * 	{@link DestructionManager}. By default a jobs list has a default
 * 	implementation of this interface: {@link DefaultDestructionManager}.
 * </p>
 * <p><b>WARNING:</b>
 * 	When added in a UWS, the jobs list inherits the destruction manager of its
 * 	UWS. Thus all jobs list of a UWS have the same destruction manager.
 * </p>
 *
 * <p>
 * 	To use a custom destruction manager, you can use the method
 * 	{@link #setDestructionManager(DestructionManager)}.
 * </p>
 *
 * <h3>Archiving and Job destruction policy</h3>
 *
 * <p>
 * 	Since UWS-1.1, it is possible to archive a job. The behavior described in
 * 	the UWS-1.1 document is that a job goes into the ARCHIVED phase when its
 * 	destruction date is reached. This behavior is a destruction alternative
 * 	introduced in order to keep only the metadata part of a job whenever it
 * 	is automatically destroyed by the UWS service. It is not imposed in the
 * 	UWS-1.1 document ; it is just a description of the ARCHIVED phase.
 * </p>
 *
 * <p>
 * 	The UWS Library proposes 2 other strategies: always delete a job
 * 	(whatever its destruction date is reached or not ; default behavior in
 * 	UWS 1.0), or always archive first. Thus, it is now possible to specify on a
 * 	{@link JobList} instance which policy should be used. This can be done
 * 	thanks to the function {@link #setDestructionPolicy(JobDestructionPolicy)}
 * 	and with the enum class {@link JobDestructionPolicy}.
 * </p>
 *
 * <p>
 * 	By default, the default behavior proposed by UWS 1.1 is set:
 * 	{@link JobDestructionPolicy#ALWAYS_DELETE}, mainly for backward
 * 	compatibility with former library versions but also to prevent using more
 * 	and more memory for ARCHIVED jobs without the explicit approval from the
 * 	UWS service manager.
 * </p>
 *
 * <p><i>Note:
 * 	An archived job can be destroyed definitely by calling again
 * 	{@link #destroyJob(String)} (i.e. DELETE on {jobs}/{job-id} or POST
 * 	ACTION=DELETE on {jobs}/{job-id}).
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.5 (07/2020)
 *
 * @see UWSJob
 */
public class JobList extends SerializableUWSObject implements Iterable<UWSJob> {
	private static final long serialVersionUID = 1L;

	/** Default policy applied to a job list when a job destruction is asked.
	 * @since 4.3 */
	public final static JobDestructionPolicy DEFAULT_JOB_DESTRUCTION_POLICY = JobDestructionPolicy.ALWAYS_DELETE;

	/** <b>[Required]</b> Name of the jobs list. */
	private final String name;

	/** <b>[Required]</b> List of jobs. */
	protected final Map<String, UWSJob> jobsList;

	/** <b>[Required]</b> List of jobs per owner. */
	protected final Map<JobOwner, Map<String, UWSJob>> ownerJobs;

	/** The destruction manager to use to take into account the destructionTime
	 * field of contained jobs. */
	private DestructionManager destructionManager = null;

	/** Indicate how this job list behaves when a job destruction is asked.
	 * <p>
	 * 	By default, a job is always destroyed (i.e. when the destruction is
	 * 	reached but also when a user requested it).
	 * </p>
	 * @since 4.3 */
	private JobDestructionPolicy destructionPolicy = DEFAULT_JOB_DESTRUCTION_POLICY;

	/** This object, if not null, decides whether a managed job can start
	 * immediately or must be put in a queue. */
	private ExecutionManager executionManager = null;

	/** <b>[Optional]</b> Useful only to get the URL of this job list. */
	private UWS uws = null;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds a jobs list with its name.
	 *
	 * @param jobListName	The jobs list name.
	 *
	 * @throws NullPointerException	If the given job list name is NULL.
	 *
	 * @see #JobList(String, ExecutionManager)
	 */
	public JobList(String jobListName) throws NullPointerException {
		this(jobListName, null, new DefaultDestructionManager());
	}

	/**
	 * Builds a jobs list with its name and the job list manager.
	 *
	 * @param jobListName		The jobs list name.
	 * @param executionManager	The object which will manage the execution of
	 *                        	all jobs of this list.
	 *
	 * @throws NullPointerException	If the given job list name is NULL
	 *                             	or empty
	 *                             	or if no execution manager is provided.
	 */
	public JobList(String jobListName, ExecutionManager executionManager) throws NullPointerException {
		this(jobListName, executionManager, new DefaultDestructionManager());
	}

	/**
	 * Builds a jobs list with its name and the destruction manager to use.
	 *
	 * @param jobListName			The jobs list name.
	 * @param destructionManager	The object which manages the automatic
	 *                          	destruction of jobs when they have reached
	 *                          	their destruction date.
	 *
	 * @throws NullPointerException	If the given job list name is NULL
	 *                             	or empty
	 *                             	or if no destruction manager is provided.
	 */
	public JobList(String jobListName, DestructionManager destructionManager) throws NullPointerException {
		this(jobListName, new DefaultExecutionManager(), destructionManager);
	}

	/**
	 * Builds a jobs list with its name, the job list manager and the
	 * destruction manager.
	 *
	 * @param jobListName			The jobs list name.
	 * @param executionManager		The object which will manage the execution
	 *                        		of all jobs of this list.
	 * @param destructionManager	The object which manages the automatic
	 *                          	destruction of jobs when they have reached
	 *                          	their destruction date.
	 *
	 * @throws NullPointerException	If the given job list name is NULL or empty
	 *                             	or if no execution manager and destruction
	 *                             	manager are provided.
	 */
	public JobList(String jobListName, ExecutionManager executionManager, DestructionManager destructionManager) throws NullPointerException {
		if (jobListName == null)
			throw new NullPointerException("Missing job list name ! => Impossible to build the job list.");
		else {
			jobListName = jobListName.trim();
			if (jobListName.length() == 0)
				throw new NullPointerException("Missing job list name ! => Impossible to build the job list.");
		}

		name = jobListName;
		jobsList = new ConcurrentHashMap<String, UWSJob>();
		ownerJobs = new ConcurrentHashMap<JobOwner, Map<String, UWSJob>>();

		this.executionManager = executionManager;

		if (destructionManager == null)
			throw new NullPointerException("Missing destruction manager ! => Impossible to build the job list.");
		else
			this.destructionManager = destructionManager;
	}

	/* ******************* */
	/* GETTERS AND SETTERS */
	/* ******************* */
	/**
	 * Gets the UWS which manages this jobs list.
	 *
	 * @return	Its UWS or <i>null</i> if this jobs list is not yet part of a
	 *        	specific UWS).
	 */
	public final UWS getUWS() {
		return uws;
	}

	/**
	 * Sets the UWS which aims to manage this jobs list.
	 *
	 * <p><i>Note:
	 * 	The UWS association can be changed ONLY IF the jobs list is not yet
	 * 	associated with a UWS OR IF it is empty.
	 * </i></p>
	 *
	 * @param newUws	Its new UWS.
	 *              	<i>If NULL, nothing is done!</i>
	 *
	 * @throws IllegalStateException	If this jobs list is already associated
	 *                              	with a UWS (different from the given
	 *                              	one) and contains some jobs.
	 */
	public final void setUWS(UWS newUws) throws IllegalStateException {
		if (newUws == null)
			return;
		else if (newUws.equals(uws))
			return;
		else if (uws == null || getNbJobs() == 0)
			uws = newUws;
		else
			throw new IllegalStateException("This jobs list (here: " + getName() + ") is already associated with a UWS and contains some jobs (size=" + getNbJobs() + "). A job list can not be part of several UWS instances and can not be moved into another UWS if not empty !");
	}

	/**
	 * Gets the logger of its UWS, or a default one if the UWS is unknown.
	 *
	 * @return A logger.
	 *
	 * @see #getUWS()
	 * @see UWS#getLogger()
	 * @see UWSToolBox#getDefaultLogger()
	 */
	public UWSLog getLogger() {
		if (getUWS() != null && getUWS().getLogger() != null)
			return getUWS().getLogger();
		else
			return UWSToolBox.getDefaultLogger();
	}

	/**
	 * Gets the used destruction manager.
	 *
	 * <p><i>Note:
	 * 	Remember that the destruction manager lets destroying automatically jobs
	 * 	only when their destructionTime has been reached.
	 * </i></p>
	 *
	 * @return	Its destruction manager.
	 */
	public final DestructionManager getDestructionManager() {
		return destructionManager;
	}

	/**
	 * Sets the destruction manager to use.
	 *
	 * <p>
	 * 	All the jobs are removed from the former destruction manager and then
	 * 	added in the new one.
	 * </p>
	 *
	 * <p><i>Note:
	 * 	Remember that the destruction manager lets destroying automatically jobs
	 * 	only when their destructionTime has been reached.
	 * </i></p>
	 *
	 * @param newManager	Its new destruction manager (MUST be different from
	 *                  	<i>null</i> otherwise nothing is done).
	 *
	 * @see DestructionManager#remove(UWSJob)
	 * @see DestructionManager#update(UWSJob)
	 */
	public final void setDestructionManager(DestructionManager newManager) {
		if (newManager == null)
			return;

		DestructionManager oldManager = destructionManager;
		destructionManager = newManager;

		for(UWSJob job : this) {
			oldManager.remove(job);
			destructionManager.update(job);
		}
	}

	/**
	 * Gets the used execution manager.
	 *
	 * @return	The used execution manager.
	 */
	public final ExecutionManager getExecutionManager() {
		if (executionManager == null) {
			if (uws == null)
				executionManager = new DefaultExecutionManager();
			else
				executionManager = new DefaultExecutionManager(uws.getLogger());
		}
		return executionManager;
	}

	/**
	 * Sets the execution manager to use.
	 *
	 * <p><i>Note:
	 * 	All jobs managed by the old execution manager are removed from it and
	 * 	added to the new manager.
	 * </i></p>
	 *
	 * @param manager	The execution manager to use (MUST be different from
	 *               	<i>null</i> otherwise nothing is done).
	 *
	 * @see ExecutionManager#remove(UWSJob)
	 * @see ExecutionManager#execute(UWSJob)
	 */
	public synchronized final void setExecutionManager(final ExecutionManager manager) {
		if (manager == null)
			return;

		ExecutionManager oldManager = executionManager;
		executionManager = manager;

		if (oldManager != null) {
			for(UWSJob job : this) {
				if (job.getPhase() != ExecutionPhase.PENDING && !job.isFinished()) {
					oldManager.remove(job);
					executionManager.execute(job);
				}
			}
		}
	}

	/**
	 * Tell how this job list behaves when a job destruction is asked.
	 *
	 * @return	The job destruction policy of this job list.
	 *
	 * @since 4.3
	 */
	public final JobDestructionPolicy getDestroyPolicy() {
		return destructionPolicy;
	}

	/**
	 * Set how this job list must behave when a job destruction is asked.
	 *
	 * @param destroyPolicy	The job destruction policy to set.
	 *                     	<i>If NULL, the default policy (i.e.
	 *                     	{@link #DEFAULT_JOB_DESTRUCTION_POLICY}) will be
	 *                     	set.</i>
	 *
	 * @since 4.3
	 */
	public final void setDestructionPolicy(JobDestructionPolicy destroyPolicy) {
		this.destructionPolicy = (destroyPolicy == null) ? DEFAULT_JOB_DESTRUCTION_POLICY : destroyPolicy;
	}

	/**
	 * Gets the UWS URL of this jobs list in function of its UWS.
	 *
	 * @return	Its corresponding UWSUrl.
	 *
	 * @see UWSService#getUrlInterpreter()
	 * @see UWSUrl#listJobs(String)
	 */
	public UWSUrl getUrl() {
		if (uws == null || uws.getUrlInterpreter() == null)
			return null;
		else
			return uws.getUrlInterpreter().listJobs(getName());
	}

	/**
	 * Gets the name of this jobs list.
	 *
	 * @return	JobList name.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Gets the job whose the ID is given in parameter.
	 *
	 * @param jobID	The ID of the job to get.
	 *
	 * @return	The requested job or <i>null</i> if there is no job with the
	 *        	given ID.
	 */
	public final UWSJob getJob(String jobID) {
		return jobsList.get(jobID);
	}

	/**
	 * Gets the job whose the ID is given in parameter ONLY IF it is the one of
	 * the specified user OR IF the specified job is owned by an anonymous user.
	 *
	 * @param jobID	ID of the job to get.
	 * @param user	The user who asks this job (<i>null</i> means no particular
	 *            	owner => cf {@link #getJob(String)}).
	 *
	 * @return	The requested job or <i>null</i> if there is no job with the
	 *        	given ID or if the user is not allowed to get the given job.
	 *
	 * @throws UWSException	If the given user is not allowed to read the content
	 *                     	of this jobs list or to read the specified job.
	 */
	public UWSJob getJob(String jobID, JobOwner user) throws UWSException {
		if (user != null && !user.hasReadPermission(this))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.readPermissionDenied(user, true, getName()));

		// Get the specified job:
		UWSJob job = jobsList.get(jobID);

		// Check the right of the specified user to see the job:
		if (user != null && job != null && job.getOwner() != null) {
			JobOwner owner = job.getOwner();
			if (!owner.equals(user) && !user.hasReadPermission(job))
				throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.readPermissionDenied(user, false, job.getJobId()));
		}

		return job;
	}

	/**
	 * Gets an iterator on the whole jobs list.
	 *
	 * @return 	All jobs of this list.
	 *
	 * @see #iterator()
	 */
	public final Iterator<UWSJob> getJobs() {
		return iterator();
	}

	/**
	 * Gets an iterator on the jobs list of the specified user.
	 *
	 * @param user 	The owner/user who asks for this operation
	 *            	(may be <i>null</i>).
	 *
	 * @return 	An iterator on all jobs which have been created by the specified
	 *        	owner/user
	 * 			or a NullIterator if the specified owner/user has no job
	 * 			or an iterator on all the jobs if <i>ownerId</i> is <i>null</i>.
	 */
	public Iterator<UWSJob> getJobs(JobOwner user) {
		if (user == null)
			return iterator();
		else {
			if (ownerJobs.containsKey(user))
				return ownerJobs.get(user).values().iterator();
			else
				return new Iterator<UWSJob>() {
					@Override
					public boolean hasNext() {
						return false;
					}

					@Override
					public UWSJob next() {
						return null;
					}

					@Override
					public void remove() {
						;
					}
				};
		}
	}

	/**
	 * Gets an iterator on the jobs list.
	 *
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public final Iterator<UWSJob> iterator() {
		return jobsList.values().iterator();
	}

	/**
	 * Gets the number of jobs into this list.
	 *
	 * @return	Number of jobs.
	 */
	public final int getNbJobs() {
		return jobsList.size();
	}

	/**
	 * Gets the number of jobs owned by the given user into this list.
	 *
	 * @param user The owner/user (may be <i>null</i>).
	 *
	 * @return	Number of jobs that the given owner/user has,
	 * 			or the number of all jobs if <i>user</i> is <i>null</i>.
	 */
	public final int getNbJobs(JobOwner user) {
		if (user == null)
			return getNbJobs();
		else {
			if (ownerJobs.containsKey(user))
				return ownerJobs.get(user).size();
			else
				return 0;
		}
	}

	/**
	 * Gets all users that own at least one job in this list.
	 *
	 * @return	An iterator on owners.
	 */
	public final Iterator<JobOwner> getUsers() {
		return ownerJobs.keySet().iterator();
	}

	/**
	 * Gets the number of all users that have at least one job in this list.
	 *
	 * @return	The number of job owners.
	 */
	public final int getNbUsers() {
		return ownerJobs.size();
	}

	/* ********************** */
	/* JOB MANAGEMENT METHODS */
	/* ********************** */
	/**
	 * Gets all job whose the runID is equals (not case sensitive) to the given
	 * runID.
	 *
	 * @param runID	The runID of jobs to search.
	 *
	 * @return	All the corresponding jobs.
	 */
	public final List<UWSJob> searchJobs(String runID) {
		ArrayList<UWSJob> foundJobs = new ArrayList<UWSJob>();
		runID = (runID != null) ? runID.trim() : runID;

		if (runID != null && !runID.isEmpty()) {
			for(UWSJob job : this)
				if (job.getRunId().equalsIgnoreCase(runID))
					foundJobs.add(job);
		}

		return foundJobs;
	}

	/**
	 * Add the given job to the list except if a job with the same jobID already
	 * exists. The jobs list of the new job's owner is always updated if the job
	 * has been added.
	 *
	 * @param j	The job to add.
	 *
	 * @return	The JobID if the job has been successfully added,
	 *        	<i>null</i> otherwise.
	 *
	 * @throws UWSException If the owner of the given job is not allowed to add
	 *                     	any job into this jobs list.
	 *
	 * @see UWSJob#setJobList(JobList)
	 * @see UWSService#getBackupManager()
	 * @see UWSBackupManager#saveOwner(JobOwner)
	 * @see DestructionManager#update(UWSJob)
	 * @see UWSJob#applyPhaseParam(JobOwner)
	 */
	public synchronized String addNewJob(final UWSJob j) throws UWSException {
		if (uws == null)
			throw new IllegalStateException("Jobs can not be added to this job list until this job list is linked to a UWS!");
		else if (j == null || jobsList.containsKey(j.getJobId())) {
			return null;
		} else {
			JobOwner owner = j.getOwner();

			// Check the WRITE permission of the owner of this job:
			if (owner != null && !owner.hasWritePermission(this))
				throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(owner, true, getName()));

			// Set its job list:
			j.setJobList(this);

			// Add the job to the jobs list:
			jobsList.put(j.getJobId(), j);
			if (owner != null) {
				// Index also this job in function of its owner:
				if (!ownerJobs.containsKey(owner))
					ownerJobs.put(owner, new ConcurrentHashMap<String, UWSJob>());
				ownerJobs.get(owner).put(j.getJobId(), j);
			}

			// Save the owner jobs list:
			if (owner != null && uws.getBackupManager() != null && j.getRestorationDate() == null)
				uws.getBackupManager().saveOwner(j.getOwner());

			// Add it to the destruction manager:
			destructionManager.update(j);

			// Execute the job if asked in the additional parameters:
			j.applyPhaseParam(null);	// Note: can not throw an exception since no user is specified (so, no permission check is done).

			// Log the "creation" of the job:
			if (j.getRestorationDate() == null)
				getLogger().logJob(LogLevel.INFO, j, "CREATED", "Job \"" + j.getJobId() + "\" successfully created and added in the job list \"" + getName() + "\".", null);

			return j.getJobId();
		}
	}

	/**
	 * Archive the specified job.
	 *
	 * <p>
	 * 	Resources (i.e. threads and files) of an archived jobs are destroyed.
	 * 	Only the description is still available. The job can not be executed any
	 * 	more. It is visible in the job list ONLY IF a filter on the
	 * 	{@link ExecutionPhase#ARCHIVED ARCHIVED} phase is set.
	 * </p>
	 *
	 * @param jobId	The ID of the job to archive.
	 *
	 * @return	<i>true</i> if it has been successfully archived,
	 *        	<i>false</i> otherwise.
	 *
	 * @see UWSJob#archive()
	 *
	 * @since 4.3
	 */
	public boolean archiveJob(final String jobId) {
		UWSJob job = getJob(jobId);

		if (job != null) {
			// Archive the job:
			job.archive();

			// Save the owner jobs list:
			if (job.getOwner() != null && uws.getBackupManager() != null)
				uws.getBackupManager().saveOwner(job.getOwner());

			return true;
		} else
			return false;
	}

	/**
	 * Archive the specified job if the given user is allowed to.
	 *
	 * <p>
	 * 	Resources (i.e. threads and files) of an archived jobs are destroyed.
	 * 	Only the description is still available. The job can not be executed any
	 * 	more. It is visible in the job list ONLY IF a filter on the
	 * 	{@link ExecutionPhase#ARCHIVED ARCHIVED} phase is set.
	 * </p>
	 *
	 * @param jobId	The ID of the job to archive.
	 * @param user	The user who asks to archive the specified job.
	 *
	 * @return	<i>true</i> if it has been successfully archived,
	 *        	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If the given user is not allowed to update the
	 *                     	content of this jobs list or of the specified job.
	 *
	 * @since 4.3
	 */
	public boolean archiveJob(final String jobId, final JobOwner user) throws UWSException {
		if (user != null) {
			if (!user.hasWritePermission(this))
				throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, true, getName()));
			UWSJob job = getJob(jobId);
			if (job != null && job.getOwner() != null && !user.equals(job.getOwner()) && !user.hasWritePermission(job))
				throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, false, jobId));
		}
		return archiveJob(jobId);
	}

	/**
	 * Lets notifying the destruction manager of a possible modification of the
	 * destructionTime of the given job.
	 *
	 * <p><i>Note:
	 * 	This method does nothing if this jobs list has no destruction manager,
	 * 	if the given job is NULL or if this jobs list does not know the given
	 * 	job.
	 * </i></p>
	 *
	 * @param job	The job whose the destructionTime may have been modified.
	 *
	 * @see DestructionManager#update(UWSJob)
	 */
	public final void updateDestruction(UWSJob job) {
		if (destructionManager != null && job != null && job.getJobList() != null && job.getJobList().equals(this))
			destructionManager.update(job);
	}

	/**
	 * Lets removing (NOT DESTROYING) the specified job from this jobs list.
	 *
	 * @param jobId	The ID of the job to remove.
	 *
	 * @return	The removed job
	 *        	or <i>null</i> if there is no job with the given jobID.
	 *
	 * @see DestructionManager#remove(UWSJob)
	 */
	protected UWSJob removeJob(final String jobId) {
		// Remove the specified job:
		UWSJob removedJob = (jobId == null) ? null : jobsList.remove(jobId);

		if (removedJob != null) {
			// Clear its owner index:
			JobOwner owner = removedJob.getOwner();
			if (owner != null && ownerJobs.containsKey(owner)) {
				ownerJobs.get(owner).remove(jobId);
				if (ownerJobs.get(owner).isEmpty())
					ownerJobs.remove(owner);
			}

			// Remove it from the destruction manager:
			if (destructionManager != null)
				destructionManager.remove(removedJob);
			return removedJob;
		} else
			return null;
	}

	/**
	 * Removes the job from the list and deletes all its attached resources
	 * ({@link UWSJob#clearResources()}.
	 *
	 * <p><i>Note:
	 * 	The jobs list of the destroyed job's owner is always updated.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function will not destroy the job, but will archive it in the
	 * 	following cases:
	 * </i></p>
	 * <ul>
	 * 	<li><i>the {@link #getDestroyPolicy() destroy policy}
	 * 	    is {@link JobDestructionPolicy#ALWAYS_ARCHIVE ALWAYS_ARCHIVE},</i></li>
	 * 	<li><i>the {@link #getDestroyPolicy() destroy policy}
	 * 	    is {@link JobDestructionPolicy#ARCHIVE_ON_DATE ARCHIVE_ON_DATE}
	 * 	    and the destruction time has been reached,</i></li>
	 * </ul>
	 * <p><i>
	 * 	In all the other cases but also if the job is already in
	 * 	{@link ExecutionPhase#ARCHIVED ARCHIVED} phase, the specified job will
	 * 	be destroyed.
	 * </i></p>
	 *
	 * @param jobId	The ID of the job to destroy.
	 *
	 * @return	<i>true</i> if it has been successfully destroyed,
	 *        	<i>false</i> otherwise.
	 *
	 * @see #removeJob(String)
	 * @see UWSJob#clearResources()
	 * @see UWSService#getBackupManager()
	 * @see UWSBackupManager#saveOwner(JobOwner)
	 * @see #archiveJob(String)
	 */
	public boolean destroyJob(final String jobId) {
		// Get the corresponding job and return immediately if none can be found:
		UWSJob job = getJob(jobId);
		if (job == null)
			return false;

		// Determine whether the destruction date is already reached or not:
		boolean dateReached = job.getDestructionTime() != null && job.getDestructionTime().compareTo(new Date()) <= 0;

		/* 3 policies are possible for a job destruction:
		 *   a. ALWAYS_DELETE => whatever is the job destruction time or the job
		 *                       phase, the job is immediately destroyed.
		 *   b. ALWAYS_ARCHIVE => the job is always archived, EXCEPT if it is
		 *                        already in ARCHIVED phase. In this last case,
		 *                        it will be destroyed.
		 *   c. ARCHIVE_ON_DATE => the job is archived ONLY IF the destruction
		 *                         time is reached. Otherwise the job is
		 *                         destroyed.
		 *
		 *  Which gives the 2 following cases: */

		/* CASE 1: Destroy the job if in any of the following cases:
		 *           - already ARCHIVED,
		 *           - policy = ALWAYS_DELETE,
		 *           - policy = ARCHIVE_ON_DATE and the destruction date is not
		 *             yet reached. */
		if (job.getPhase() == ExecutionPhase.ARCHIVED || destructionPolicy == JobDestructionPolicy.ALWAYS_DELETE || (destructionPolicy == JobDestructionPolicy.ARCHIVE_ON_DATE && !dateReached)) {
			// Remove the job:
			UWSJob destroyedJob = removeJob(jobId);

			if (destroyedJob != null) {
				// Clear associated resources:
				destroyedJob.clearResources();

				// Save the owner jobs list:
				if (destroyedJob.getOwner() != null && uws.getBackupManager() != null)
					uws.getBackupManager().saveOwner(destroyedJob.getOwner());

				// Log this job destruction:
				getLogger().logJob(LogLevel.INFO, destroyedJob, "DESTROY", "The job \"" + destroyedJob.getJobId() + "\" has been removed from the job list \"" + name + "\".", null);

				return true;
			}
			return false;
		}
		/* CASE 2: Archive the job if not already ARCHIVED and if the policy is
		 *         either ALWAYS_ARCHIVE or ARCHIVE_ON_DATE while the
		 *         destruction date is reached: */
		else
			return archiveJob(jobId);
	}

	/**
	 * Removes the job from the list and deletes all its attached resources
	 * ({@link UWSJob#clearResources()}. The jobs list of the new job's owner is
	 * always saved.
	 *
	 * @param jobId	The ID of the job to destroy.
	 * @param user	The user who asks to destroy the specified job.
	 *
	 * @return	<i>true</i> if it has been successfully destroyed,
	 *        	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If the given user is not allowed to update the
	 *                     	content of this jobs list or to destroy the
	 *                     	specified job.
	 */
	public boolean destroyJob(final String jobId, final JobOwner user) throws UWSException {
		if (user != null) {
			if (!user.hasWritePermission(this))
				throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, true, getName()));
			UWSJob job = getJob(jobId);
			if (job != null && job.getOwner() != null && !user.equals(job.getOwner()) && !user.hasWritePermission(job))
				throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, false, jobId));
		}
		return destroyJob(jobId);
	}

	/*
	 * Removes all jobs of this list.
	 *
	 * @see #removeJob(String)
	 *
	public synchronized void removeAll(){
		ArrayList<String> jobIDs = new ArrayList<String>(jobsList.keySet());
		for(String id : jobIDs)
			removeJob(id);
	}*/

	/**
	 * Destroys all jobs of this list.
	 *
	 * @see #destroyJob(String)
	 */
	public synchronized void clear() {
		ArrayList<String> jobIDs = new ArrayList<String>(jobsList.keySet());
		for(String id : jobIDs)
			destroyJob(id);
	}

	/**
	 * Destroys all jobs owned by the specified user.
	 *
	 * @param owner The owner/user who asks for this operation.
	 *
	 * @throws UWSException	If the given user is not allowed to update of the
	 *                     	content of this jobs list.
	 *
	 * @see #clear()
	 * @see #destroyJob(String)
	 */
	public synchronized void clear(JobOwner owner) throws UWSException {
		if (owner == null)
			clear();
		else if (!owner.hasWritePermission(this))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(owner, true, getName()));
		else {
			if (ownerJobs.containsKey(owner)) {
				ArrayList<String> jobIDs = new ArrayList<String>(ownerJobs.get(owner).keySet());
				for(String id : jobIDs)
					destroyJob(id);
				ownerJobs.remove(owner);
			}
		}
	}

	/**
	 * Serializes the while object in the given output stream,
	 * considering the given owner, the given job filters and thanks to the
	 * given serializer.
	 *
	 * @param output		The ouput stream in which this object must be
	 *              		serialized.
	 * @param serializer	The serializer to use.
	 * @param owner			The current user.
	 * @param listRefiner	Special filter able to refine the list of jobs with
	 *                   	job filters specified by the user
	 *                   	(i.e. filter, sort and limit).
	 *
	 * @throws UWSException		If the owner is not allowed to see the content
	 *                     		of the serializable object.
	 * @throws IOException		If there is an error while writing in the given
	 *                    		stream.
	 * @throws Exception		If there is any other error during the
	 *                  		serialization.
	 *
	 * @see UWSSerializer#getJobList(JobList, JobOwner, JobListRefiner, boolean)
	 *
	 * @since 4.3
	 */
	public void serialize(ServletOutputStream output, UWSSerializer serializer, JobOwner owner, JobListRefiner listRefiner) throws UWSException, IOException, Exception {
		if (output == null)
			throw new NullPointerException("Missing serialization output stream!");

		if (owner != null && !owner.hasReadPermission(this))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(owner, true, getName()));

		String serialization = serializer.getJobList(this, owner, listRefiner, true);
		if (serialization != null) {
			output.print(serialization);
			output.flush();
		} else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Incorrect serialization value (=NULL) ! => impossible to serialize " + toString() + ".");
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	@Override
	public String serialize(UWSSerializer serializer, JobOwner user) throws UWSException, Exception {
		if (user != null && !user.hasReadPermission(this))
			throw new UWSException(UWSException.FORBIDDEN, UWSExceptionFactory.writePermissionDenied(user, true, getName()));

		return serializer.getJobList(this, user, true);
	}

	@Override
	public String toString() {
		return "JOB_LIST {name: \"" + getName() + "\"; nbJobs: " + jobsList.size() + "}";
	}

}
