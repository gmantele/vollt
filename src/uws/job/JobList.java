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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;
import uws.job.manager.DefaultDestructionManager;
import uws.job.manager.DefaultExecutionManager;
import uws.job.manager.DestructionManager;
import uws.job.manager.ExecutionManager;
import uws.job.serializer.UWSSerializer;
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
 * <p>An instance of this class lets listing UWS jobs (possible actions: get, add and remove).</p>
 * 
 * <p><i><u>Note:</u>This list implements the interface {@link Iterable} which lets you iterating more easily among the jobs.</i></p>
 * 
 * <h3>Jobs list by user</h3>
 * 
 * <p>So that avoiding any user to interact on jobs of anybody else, it is possible (and strongly encouraged)
 * to get the list of jobs only of the current user. For that you must use the function {@link #getJobs(JobOwner)} with a owner ID.</p>
 * 
 * <p>At each request sent to any instance of {@link UWSService} the function {@link UserIdentifier#extractUserId(uws.service.UWSUrl, javax.servlet.http.HttpServletRequest)}
 * may extract the user ID. Thus the action GetJobList may know who is the current user. If the extracted owner ID is different from <i>null</i>
 * only the jobs of the current user will be returned.<br />
 * <b>However you need to find a way to identify for each UWS request the current user and then to override correctly the function
 * {@link UserIdentifier#extractUserId(uws.service.UWSUrl, javax.servlet.http.HttpServletRequest)}.</b></p>
 * 
 * <p><i><u>Note:</u> if no owner is specified (NULL value), all jobs are concerned by the function action.</i></p>
 * 
 * <h3>Searching in a jobs list</h3>
 * 
 * <ul>
 * 	<li><b>{@link #getJobs()} or {@link #iterator()}:</b> to get all the jobs of this jobs list.</li>
 * 	<li><b>{@link #getJob(String)}:</b> to get the job that has the given jobID.</li>
 * 	<li><b>{@link #searchJobs(String)}:</b> to search all the jobs that have the given runID.</li>
 * 	<li><b>{@link #getJobs(JobOwner)}: </b> to get the jobs of the specified user.</li>
 * </ul>
 * 
 * <h3>Get the users list</h3>
 * 
 * <p>If you are interested in (probably for some statistics or for an administrator) you can ask the list of users
 * who have at least one job in this jobs list ({@link #getUsers()}) and known how many they are ({@link #getNbUsers()}).</p>
 * 
 * <h3>Execution management</h3>
 * 
 * <p>The execution of the jobs of this jobs list is managed by an implementation of {@link ExecutionManager}.
 * {@link DefaultExecutionManager} is used by default, but you can easily set your owne implementation of this interface,
 * either at the job list creation or with {@link #setExecutionManager(ExecutionManager)}.</p>
 * 
 * <h3>Automatic job destruction</h3>
 * 
 * <p>
 * 	A job has a field - destructionTime - which indicates the date at which it must destroyed.
 * 	Remember that destroying a job consists in removing it from its jobs list and then clearing all its resources (result files, threads, ...).
 * 	This task is done by an "instance" of the interface {@link DestructionManager}. By default a jobs list has a default implementation of this interface: {@link DefaultDestructionManager}.
 * 	However when added in a UWS, the jobs list inherits the destruction manager of its UWS. Thus all jobs list of a UWS have the same destruction manager.
 * </p>
 * 
 * <p>
 * 	To use a custom destruction manager, you can use the method {@link #setDestructionManager(DestructionManager)}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.2 (09/2017)
 * 
 * @see UWSJob
 */
public class JobList extends SerializableUWSObject implements Iterable<UWSJob> {
	private static final long serialVersionUID = 1L;

	/** <b>[Required]</b> Name of the jobs list. */
	private final String name;

	/** <b>[Required]</b> List of jobs. */
	protected final Map<String,UWSJob> jobsList;

	/** <b>[Required]</b> List of jobs per owner. */
	protected final Map<JobOwner,Map<String,UWSJob>> ownerJobs;

	/** The destruction manager to use to take into account the destructionTime field of contained jobs. */
	private DestructionManager destructionManager = null;

	/** This object, if not null, decides whether a managed job can start immediately or must be put in a queue. */
	private ExecutionManager executionManager = null;

	/** <b>[Optional]</b> Useful only to get the URL of this job list. */
	private UWS uws = null;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * <p>Builds a jobs list with its name.</p>
	 * 
	 * @param jobListName				The jobs list name.
	 * 
	 * @throws NullPointerException	If the given job list name is NULL.
	 * 
	 * @see #JobList(String, ExecutionManager)
	 */
	public JobList(String jobListName) throws NullPointerException{
		this(jobListName, null, new DefaultDestructionManager());
	}

	/**
	 * Builds a jobs list with its name and the job list manager.
	 * 
	 * @param jobListName		The jobs list name.
	 * @param executionManager	The object which will manage the execution of all jobs of this list.
	 *
	 * @throws NullPointerException	If the given job list name is NULL or empty or if no execution manager is provided.
	 */
	public JobList(String jobListName, ExecutionManager executionManager) throws NullPointerException{
		this(jobListName, executionManager, new DefaultDestructionManager());
	}

	/**
	 * Builds a jobs list with its name and the destruction manager to use.
	 * 
	 * @param jobListName			The jobs list name.
	 * @param destructionManager	The object which manages the automatic destruction of jobs when they have reached their destruction date.
	 *
	 * @throws NullPointerException	If the given job list name is NULL or empty or if no destruction manager is provided.
	 */
	public JobList(String jobListName, DestructionManager destructionManager) throws NullPointerException{
		this(jobListName, new DefaultExecutionManager(), destructionManager);
	}

	/**
	 * Builds a jobs list with its name, the job list manager and the destruction manager.
	 * 
	 * @param jobListName			The jobs list name.
	 * @param executionManager		The object which will manage the execution of all jobs of this list.
	 * @param destructionManager	The object which manages the automatic destruction of jobs when they have reached their destruction date.
	 *
	 * @throws NullPointerException	If the given job list name is NULL or empty or if no execution manager and destruction manager are provided.
	 */
	public JobList(String jobListName, ExecutionManager executionManager, DestructionManager destructionManager) throws NullPointerException{
		if (jobListName == null)
			throw new NullPointerException("Missing job list name ! => Impossible to build the job list.");
		else{
			jobListName = jobListName.trim();
			if (jobListName.length() == 0)
				throw new NullPointerException("Missing job list name ! => Impossible to build the job list.");
		}

		name = jobListName;
		jobsList = new ConcurrentHashMap<String,UWSJob>();
		ownerJobs = new ConcurrentHashMap<JobOwner,Map<String,UWSJob>>();

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
	 * @return	Its UWS or <i>null</i> if this jobs list is not yet part of a specific UWS).
	 */
	public final UWS getUWS(){
		return uws;
	}

	/**
	 * <p>Sets the UWS which aims to manage this jobs list.</p>
	 * 
	 * <p><i><u>note:</u> The UWS association can be changed ONLY IF the jobs list is not yet associated with a UWS
	 * OR IF it is empty.</i></p>
	 * 
	 * @param newUws	Its new UWS. <i><u>note:</u> if NULL, nothing is done !</i>
	 * 
	 * @throws IllegalStateException	If this jobs list is already associated with a UWS (different from the given one) and contains some jobs.
	 */
	public final void setUWS(UWS newUws) throws IllegalStateException{
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
	public UWSLog getLogger(){
		if (getUWS() != null && getUWS().getLogger() != null)
			return getUWS().getLogger();
		else
			return UWSToolBox.getDefaultLogger();
	}

	/**
	 * <p>Gets the used destruction manager.</p>
	 * 
	 * <p><i><u>Note:</u> Remember that the destruction manager lets destroying automatically jobs only when their destructionTime has been reached.</i></p>
	 * 
	 * @return	Its destruction manager.
	 */
	public final DestructionManager getDestructionManager(){
		return destructionManager;
	}

	/**
	 * <p>
	 * 	Sets the destruction manager to use.
	 * 	All the jobs are removed from the former destruction manager and then added in the new one.
	 * </p>
	 * 
	 * <p><i><u>Note:</u> Remember that the destruction manager lets destroying automatically jobs only when their destructionTime has been reached.</i></p>
	 * 
	 * @param newManager	Its new destruction manager (MUST be different from <i>null</i> otherwise nothing is done).
	 * 
	 * @see DestructionManager#remove(UWSJob)
	 * @see DestructionManager#update(UWSJob)
	 */
	public final void setDestructionManager(DestructionManager newManager){
		if (newManager == null)
			return;

		DestructionManager oldManager = destructionManager;
		destructionManager = newManager;

		for(UWSJob job : this){
			oldManager.remove(job);
			destructionManager.update(job);
		}
	}

	/**
	 * Gets the used execution manager.
	 * 
	 * @return	The used execution manager.
	 */
	public final ExecutionManager getExecutionManager(){
		if (executionManager == null){
			if (uws == null)
				executionManager = new DefaultExecutionManager();
			else
				executionManager = new DefaultExecutionManager(uws.getLogger());
		}
		return executionManager;
	}

	/**
	 * <p>Sets the execution manager to use.</p>
	 * 
	 * <p><i><u>note:</u> All jobs managed by the old execution manager are removed from it and added to the new manager.</i></p>
	 * 
	 * @param manager	The execution manager to use (MUST be different from <i>null</i> otherwise nothing is done).
	 * 
	 * @see ExecutionManager#remove(UWSJob)
	 * @see ExecutionManager#execute(UWSJob)
	 */
	public synchronized final void setExecutionManager(final ExecutionManager manager){
		if (manager == null)
			return;

		ExecutionManager oldManager = executionManager;
		executionManager = manager;

		if (oldManager != null){
			for(UWSJob job : this){
				if (job.getPhase() != ExecutionPhase.PENDING && !job.isFinished()){
					oldManager.remove(job);
					executionManager.execute(job);
				}
			}
		}
	}

	/**
	 * Gets the UWS URL of this jobs list in function of its UWS.
	 * 
	 * @return	Its corresponding UWSUrl.
	 * 
	 * @see UWSService#getUrlInterpreter()
	 * @see UWSUrl#listJobs(String)
	 */
	public UWSUrl getUrl(){
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
	public final String getName(){
		return name;
	}

	/**
	 * Gets the job whose the ID is given in parameter.
	 * 
	 * @param jobID	The ID of the job to get.
	 * 
	 * @return		The requested job or <i>null</i> if there is no job with the given ID.
	 */
	public final UWSJob getJob(String jobID){
		return jobsList.get(jobID);
	}

	/**
	 * Gets the job whose the ID is given in parameter ONLY IF it is the one of the specified user OR IF the specified job is owned by an anonymous user.
	 * 
	 * @param jobID		ID of the job to get.
	 * @param user		The user who asks this job (<i>null</i> means no particular owner => cf {@link #getJob(String)}).
	 * 
	 * @return			The requested job or <i>null</i> if there is no job with the given ID or if the user is not allowed to get the given job.
	 * 
	 * @throws UWSException	If the given user is not allowed to read the content of this jobs list or to read the specified job.
	 */
	public UWSJob getJob(String jobID, JobOwner user) throws UWSException{
		if (user != null && !user.hasReadPermission(this))
			throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.readPermissionDenied(user, true, getName()));

		// Get the specified job:
		UWSJob job = jobsList.get(jobID);

		// Check the right of the specified user to see the job:
		if (user != null && job != null && job.getOwner() != null){
			JobOwner owner = job.getOwner();
			if (!owner.equals(user) && !user.hasReadPermission(job))
				throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.readPermissionDenied(user, false, job.getJobId()));
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
	public final Iterator<UWSJob> getJobs(){
		return iterator();
	}

	/**
	 * Gets an iterator on the jobs list of the specified user.
	 * 
	 * @param user 	The owner/user who asks for this operation (may be <i>null</i>).
	 * 
	 * @return 			An iterator on all jobs which have been created by the specified owner/user
	 * 					or a NullIterator if the specified owner/user has no job
	 * 					or an iterator on all the jobs if <i>ownerId</i> is <i>null</i>.
	 */
	public Iterator<UWSJob> getJobs(JobOwner user){
		if (user == null)
			return iterator();
		else{
			if (ownerJobs.containsKey(user))
				return ownerJobs.get(user).values().iterator();
			else
				return new Iterator<UWSJob>(){
					@Override
					public boolean hasNext(){
						return false;
					}

					@Override
					public UWSJob next(){
						return null;
					}

					@Override
					public void remove(){
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
	public final Iterator<UWSJob> iterator(){
		return jobsList.values().iterator();
	}

	/**
	 * Gets the number of jobs into this list.
	 * 
	 * @return	Number of jobs.
	 */
	public final int getNbJobs(){
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
	public final int getNbJobs(JobOwner user){
		if (user == null)
			return getNbJobs();
		else{
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
	public final Iterator<JobOwner> getUsers(){
		return ownerJobs.keySet().iterator();
	}

	/**
	 * Gets the number of all users that have at least one job in this list.
	 * 
	 * @return	The number of job owners.
	 */
	public final int getNbUsers(){
		return ownerJobs.size();
	}

	/* ********************** */
	/* JOB MANAGEMENT METHODS */
	/* ********************** */
	/**
	 * Gets all job whose the runID is equals (not case sensitive) to the given runID.
	 * 
	 * @param runID	The runID of jobs to search.
	 * 
	 * @return		All the corresponding jobs.
	 */
	public final List<UWSJob> searchJobs(String runID){
		ArrayList<UWSJob> foundJobs = new ArrayList<UWSJob>();
		runID = (runID != null) ? runID.trim() : runID;

		if (runID != null && !runID.isEmpty()){
			for(UWSJob job : this)
				if (job.getRunId().equalsIgnoreCase(runID))
					foundJobs.add(job);
		}

		return foundJobs;
	}

	/**
	 * <p>Add the given job to the list except if a job with the same jobID already exists.
	 * The jobs list of the new job's owner is always updated if the job has been added.</p>
	 * 
	 * @param j			The job to add.
	 * 
	 * @return	The JobID if the job has been successfully added, <i>null</i> otherwise.
	 * 
	 * @throws UWSException If the owner of the given job is not allowed to add any job into this jobs list.
	 * 
	 * @see UWSJob#setJobList(JobList)
	 * @see UWSService#getBackupManager()
	 * @see UWSBackupManager#saveOwner(JobOwner)
	 * @see DestructionManager#update(UWSJob)
	 * @see UWSJob#applyPhaseParam(JobOwner)
	 */
	public synchronized String addNewJob(final UWSJob j) throws UWSException{
		if (uws == null)
			throw new IllegalStateException("Jobs can not be added to this job list until this job list is linked to a UWS!");
		else if (j == null || jobsList.containsKey(j.getJobId())){
			return null;
		}else{
			JobOwner owner = j.getOwner();

			// Check the WRITE permission of the owner of this job:
			if (owner != null && !owner.hasWritePermission(this))
				throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(owner, true, getName()));

			// Set its job list:
			j.setJobList(this);

			// Add the job to the jobs list:
			jobsList.put(j.getJobId(), j);
			if (owner != null){
				// Index also this job in function of its owner:
				if (!ownerJobs.containsKey(owner))
					ownerJobs.put(owner, new ConcurrentHashMap<String,UWSJob>());
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
	 * <p>Lets notifying the destruction manager of a possible modification of the destructionTime of the given job.</p>
	 * 
	 * <p><i><u>Note:</u> This method does nothing if this jobs list has no destruction manager, if the given job is NULL or if this jobs list does not know the given job.</i></p>
	 * 
	 * @param job	The job whose the destructionTime may have been modified.
	 * 
	 * @see DestructionManager#update(UWSJob)
	 */
	public final void updateDestruction(UWSJob job){
		if (destructionManager != null && job != null && job.getJobList() != null && job.getJobList().equals(this))
			destructionManager.update(job);
	}

	/**
	 * <p>Lets removing (NOT DESTROYING) the specified job from this jobs list.</p>
	 * 
	 * @param jobId		The ID of the job to remove.
	 * 
	 * @return			The removed job or <i>null</i> if there is no job with the given jobID.
	 * 
	 * @see DestructionManager#remove(UWSJob)
	 */
	protected UWSJob removeJob(final String jobId){
		// Remove the specified job:
		UWSJob removedJob = (jobId == null) ? null : jobsList.remove(jobId);

		if (removedJob != null){
			// Clear its owner index:
			JobOwner owner = removedJob.getOwner();
			if (owner != null && ownerJobs.containsKey(owner)){
				ownerJobs.get(owner).remove(jobId);
				if (ownerJobs.get(owner).isEmpty())
					ownerJobs.remove(owner);
			}

			// Remove it from the destruction manager:
			if (destructionManager != null)
				destructionManager.remove(removedJob);
			return removedJob;
		}else
			return null;
	}

	/**
	 * Removes the job from the list and deletes all its attached resources ({@link UWSJob#clearResources()}.
	 * The jobs list of the new job's owner is always saved.
	 * 
	 * @param jobId		The ID of the job to destroy.
	 * 
	 * @return			<i>true</i> if it has been successfully destroyed, <i>false</i> otherwise.
	 * 
	 * @see #removeJob(String)
	 * @see UWSJob#clearResources()
	 * @see UWSService#getBackupManager()
	 * @see UWSBackupManager#saveOwner(JobOwner)
	 */
	public boolean destroyJob(final String jobId){
		// Remove the job:
		UWSJob destroyedJob = removeJob(jobId);

		if (destroyedJob != null){
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

	/**
	 * Removes the job from the list and deletes all its attached resources ({@link UWSJob#clearResources()}.
	 * The jobs list of the new job's owner is always saved.
	 * 
	 * @param jobId		The ID of the job to destroy.
	 * @param user		The user who asks to destroy the specified job.
	 * 
	 * @return			<i>true</i> if it has been successfully destroyed, <i>false</i> otherwise.
	 * 
	 * @throws UWSException	If the given user is not allowed to update the content of this jobs list or to destroy the specified job.
	 */
	public boolean destroyJob(final String jobId, final JobOwner user) throws UWSException{
		if (user != null){
			if (!user.hasWritePermission(this))
				throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(user, true, getName()));
			UWSJob job = getJob(jobId);
			if (job != null && job.getOwner() != null && !user.equals(job.getOwner()) && !user.hasWritePermission(job))
				throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(user, false, jobId));
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
	public synchronized void clear(){
		ArrayList<String> jobIDs = new ArrayList<String>(jobsList.keySet());
		for(String id : jobIDs)
			destroyJob(id);
	}

	/**
	 * Destroys all jobs owned by the specified user.
	 * 
	 * @param owner The owner/user who asks for this operation.
	 * 
	 * @throws UWSException	If the given user is not allowed to update of the content of this jobs list.
	 * 
	 * @see #clear()
	 * @see #destroyJob(String)
	 */
	public synchronized void clear(JobOwner owner) throws UWSException{
		if (owner == null)
			clear();
		else if (!owner.hasWritePermission(this))
			throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(owner, true, getName()));
		else{
			if (ownerJobs.containsKey(owner)){
				ArrayList<String> jobIDs = new ArrayList<String>(ownerJobs.get(owner).keySet());
				for(String id : jobIDs)
					destroyJob(id);
				ownerJobs.remove(owner);
			}
		}
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	@Override
	public String serialize(UWSSerializer serializer, JobOwner user) throws UWSException, Exception{
		if (user != null && !user.hasReadPermission(this))
			throw new UWSException(UWSException.PERMISSION_DENIED, UWSExceptionFactory.writePermissionDenied(user, true, getName()));

		return serializer.getJobList(this, user, true);
	}

	@Override
	public String toString(){
		return "JOB_LIST {name: \"" + getName() + "\"; nbJobs: " + jobsList.size() + "}";
	}

}
