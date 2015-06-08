package uws.job.manager;

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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import uws.job.UWSJob;

/**
 * <p>
 * 	The default implementation of the {@link DestructionManager} interface.
 * 	Its goal is to manage the automatic destruction any given jobs.
 * </p>
 * 
 * <p>
 *	Jobs can be added thanks to {@link #update(UWSJob)} and removed with {@link #remove(UWSJob)}.
 *	All added jobs are stored in a {@link TreeSet} which sorts them by ascending destruction time.
 *	The job which must be destroyed in first is used to start a timer.
 *	This one will destroyed the job once its destruction time is reached.
 * </p>
 * 
 * <p>
 * 	The list of jobs to destroy is supposed to be updated each time the destruction time of a job is changed. This update works only if
 *  the job knows its jobs list ({@link UWSJob#getJobList()} != null) and its jobs list has a destruction manager.
 * </p>
 * 
 * <p><i>Note:
 * 	The {@link #stop()} function lets stop this manager to watch for destructions of job until {@link #refresh()} or
 * 	{@link #update(UWSJob)} or {@link #remove(UWSJob)} is called. When stopped, the inner timer is canceled and set
 * 	to NULL ; no more thread resources is used.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (12/2014)
 */
public class DefaultDestructionManager implements DestructionManager {
	private static final long serialVersionUID = 1L;

	/** The list of jobs to destroy. Jobs are sorted by ascending destruction time thanks to {@link TimeComparator}.*/
	protected final TreeSet<UWSJob> jobsToDestroy;

	/** Timer for the job destruction time.
	 * Once the date-time indicated by this timer is reached the job is destroyed and removed from the jobs list. */
	protected transient Timer timDestruction = null;

	/** The job currently planned for destruction. This job will be destroyed by the timer timDestruction. */
	protected UWSJob currentJob = null;

	/** The date used by the timer to trigger the destruction of {@link #currentJob}. */
	protected Date currentDate = null;

	/* *********** */
	/* CONSTRUCTOR */
	/* *********** */
	/**
	 * Builds the default destruction manager.
	 * The list of jobs to destroy is initialized so that sorting jobs by ascending destruction time.
	 * 
	 * @see TimeComparator
	 */
	public DefaultDestructionManager(){
		jobsToDestroy = new TreeSet<UWSJob>(new TimeComparator());
	}

	/* ************ */
	/* TOOL METHODS */
	/* ************ */
	/**
	 * Stops the timer if running and set to <i>null</i> {@link #timDestruction}, {@link #currentDate} and {@link #currentJob}.
	 */
	@Override
	public synchronized final void stop(){
		if (timDestruction != null)
			timDestruction.cancel();
		timDestruction = null;
		currentDate = null;
		currentJob = null;
	}

	/**
	 * Merely destroys the given job (if not <i>null</i>).
	 * 
	 * @param job	The job to destroy.
	 * 
	 * @see UWSJob#getJobList()
	 * @see uws.job.JobList#destroyJob(String)
	 */
	protected final void destroyJob(UWSJob job){
		if (job != null && job.getJobList() != null){
			job.getJobList().destroyJob(job.getJobId());
		}
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	/**
	 * <p>Returns <i>true</i> if {@link #currentDate} is different from <i>null</i>.</p>
	 */
	@Override
	public final boolean isRunning(){
		return currentDate != null;
	}

	@Override
	public final Date getNextDestruction(){
		return currentDate;
	}

	@Override
	public final String getNextJobToDestroy(){
		return (currentJob == null) ? null : currentJob.getJobId();
	}

	@Override
	public final int getNbJobsToDestroy(){
		return jobsToDestroy.size() + (isRunning() ? 1 : 0);
	}

	/**
	 * <ol>
	 * 	<li>First, check the currently planned job:
	 * 		<ul>
	 * 			<li>if {@link #currentDate} is past, the timer is stopped.</li>
	 * 			<li>else if another job must be destroyed before the current one or if the destruction time of the current job has been changed, the timer is stopped and the job is re-added into the list of jobs to destroy.</li>
	 * 		</ul>
	 * 	</li>
	 * 	<li>Then, only if there is no currently planned job:
	 * 		<ul>
	 * 			<li>Clean the list of jobs to destroy (jobs which are supposed to be destroyed are destroyed and the others are only removed from this manager).</li>
	 * 			<li>Restart the timer with the first job to destroy.</li>
	 * 		</ul>
	 * 	</li>
	 * </ol>
	 * 
	 * @see uws.job.manager.DestructionManager#refresh()
	 * @see #stop()
	 * @see #destroyJob(UWSJob)
	 */
	@Override
	public synchronized void refresh(){
		// Finish the current timer if...
		if (isRunning()){
			// ...the time is elapsed:
			if (currentDate.before(new Date()))
				stop();

			// ...the running timer is deprecated:
			else if (!currentJob.getDestructionTime().equals(currentDate) || (!jobsToDestroy.isEmpty() && currentDate.after(jobsToDestroy.first().getDestructionTime()))){
				jobsToDestroy.add(currentJob);
				stop();
			}
		}

		// Restart the timer if there is no running timer:
		if (!isRunning()){
			// get the next job on which a timer can be put:
			currentJob = jobsToDestroy.pollFirst();
			while(!jobsToDestroy.isEmpty() && (currentJob == null || currentJob.getDestructionTime() == null || currentJob.getDestructionTime().before(new Date()))){
				if (currentJob.getDestructionTime() != null)
					destroyJob(currentJob);
				currentJob = jobsToDestroy.pollFirst();
			}
			// restart the timer:
			if (currentJob != null){
				timDestruction = new Timer();
				currentDate = currentJob.getDestructionTime();
				timDestruction.schedule(new TimerTask(){
					@Override
					public void run(){
						destroyJob(currentJob);
					}
				}, currentDate);
			}
		}
	}

	/**
	 * This function does something only if the given job knows its jobs list and has a valid destruction time.
	 * Then there are two cases:
	 * <ol>
	 * 	<li><u>The destruction time of the given job is past:</u> the job is destroyed (see {@link #destroyJob(UWSJob)}).</li>
	 * 	<li><u>Otherwise:</u> the job is removed and then added to the list of jobs to destroy. Finally this manager is refreshed (see {@link #refresh()}).</li>
	 * </ol>
	 * 
	 * @see uws.job.manager.DestructionManager#update(uws.job.UWSJob)
	 * @see #destroyJob(UWSJob)
	 * @see #refresh()
	 */
	@Override
	public synchronized void update(UWSJob job){
		if (job != null && job.getJobList() != null && job.getDestructionTime() != null){
			if (job.getDestructionTime().before(new Date()))
				destroyJob(job);
			else{
				if (!jobsToDestroy.contains(job)){
					jobsToDestroy.add(job);
					refresh();
				}
			}
		}
	}

	/**
	 * Merely removes the given job from the list of jobs to destroyed.
	 * However if the given job is the currently planned job, the timer is stopped and the manager is refreshed.
	 * 
	 * @see uws.job.manager.DestructionManager#remove(uws.job.UWSJob)
	 * @see	#stop()
	 * @see #refresh()
	 */
	@Override
	public synchronized void remove(UWSJob job){
		if (job == null)
			return;

		if (isRunning() && currentJob != null && currentJob.equals(job)){
			stop();
			refresh();
		}else
			jobsToDestroy.remove(job);
	}

	/* ************** */
	/* JOB COMPARATOR */
	/* ************** */
	/**
	 * Lets a TreeSet or a TreeMap sorting {@link UWSJob} instances in an ascending order
	 * and according to their destruction time.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 05/2012
	 */
	protected static class TimeComparator implements Serializable, Comparator<UWSJob> {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(UWSJob job1, UWSJob job2){
			if (job1 == null && job2 == null)
				return 0;

			Date date1 = job1.getDestructionTime(), date2 = job2.getDestructionTime();

			if (date1 == null && date2 == null)
				return 0;
			else if (date1 != null && (date2 == null || date1.before(date2)))
				return -1;
			else if (date2 != null && (date1 == null || date1.after(date2)))
				return 1;
			else
				return 0;
		}
	}
}
