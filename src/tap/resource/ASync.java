package tap.resource;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPFactory;
import uws.UWSException;
import uws.job.JobList;
import uws.job.UWSJob;
import uws.job.manager.AbstractQueuedExecutionManager;
import uws.job.manager.QueuedExecutionManager;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Asynchronous resource of a TAP service.</p>
 * 
 * <p>
 * 	Requests sent to this resource are ADQL queries (plus some execution parameters) to execute asynchronously.
 * 	Results and/or errors of the execution are stored on the server side and can be fetched by the user whenever he wants.
 * </p>
 * 
 * <p>
 * 	This resource is actually another VO service: a UWS (Universal Worker Service pattern).
 * 	That's why all requests sent to this resource are actually forwarded to an instance of {@link UWSService}.
 * 	All the behavior of UWS described by the IVOA is already fully implemented by this implementation.
 * </p>
 * 
 * <p>This resource is also representing the only jobs' list of this UWS service.</p>
 * 
 * <p>The UWS service is created and configured at the creation of this resource. Here are the list of the most important configured elements:</p>
 * <ul>
 * 	<li><b>User identification:</b> the user identifier is the same as the one used by the TAP service. It is provided by the given {@link ServiceConnection}.</li>
 * 	<li><b>Jobs' lists:</b> the /async resource of TAP contains only one jobs' list. Its name is "async" and is accessed directly when requesting the /async resource.</li>
 * 	<li><b>Job execution management:</b> an execution manager is created at the creation of this resource. It is queuing jobs when a maximum number of asynchronous jobs
 * 	                                     is already running. This maximum is provided by the TAP service description: {@link ServiceConnection#getNbMaxAsyncJobs()}. Jobs are also queued if no more DB
 * 	                                     connection is available ; when connection(s) will be available, this resource will be notified by {@link #freeConnectionAvailable()} so that the execution manager
 * 	                                     can be refreshed.</li>
 * 	<li><b>Backup and Restoration:</b> UWS jobs can be saved at any defined moment. It is particularly useful when an grave error occurs and merely when the service must be restarted.
 * 	                                   Then, at the creation of this resource, the jobs are restored. Thus, the restart has been transparent for the users: they did not lose any job
 * 	                                   (except those at the origin of the grave error maybe).</li>
 * 	<li><b>Error logging:</b> the created {@link UWSService} instance is using the same logger as the TAP service. It is also provided by the given {@link ServiceConnection} object at creation.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (12/2014)
 * 
 * @see UWSService
 */
public class ASync implements TAPResource {

	/** Name of this TAP resource. */
	public static final String RESOURCE_NAME = "async";

	/** Description of the TAP service owning this resource. */
	protected final ServiceConnection service;
	/** UWS service represented by this TAP resource. */
	protected final UWSService uws;
	/** The only jobs' list managed by the inner UWS service. This resource represent the UWS but also this jobs' list. */
	protected final JobList jobList;

	/**
	 * Build an Asynchronous Resource of a TAP service.
	 * 
	 * @param service	Description of the TAP service which will own this resource.
	 * 
	 * @throws TAPException	If any error occurs while creating a UWS service or its backup manager.
	 * @throws UWSException	If any error occurs while setting a new execution manager to the recent inner UWS service,
	 *                     	or while restoring a UWS backup.
	 */
	public ASync(final ServiceConnection service) throws UWSException, TAPException{
		this.service = service;

		uws = service.getFactory().createUWS();

		if (uws.getUserIdentifier() == null)
			uws.setUserIdentifier(service.getUserIdentifier());

		if (uws.getJobList(getName()) == null){
			jobList = new JobList(getName());
			uws.addJobList(jobList);
			jobList.setExecutionManager(new AsyncExecutionManager(service.getFactory(), service.getLogger(), service.getNbMaxAsyncJobs()));
		}else
			jobList = uws.getJobList(getName());

		if (uws.getBackupManager() == null)
			uws.setBackupManager(service.getFactory().createUWSBackupManager(uws));

		UWSBackupManager backupManager = uws.getBackupManager();
		if (backupManager != null){
			backupManager.setEnabled(false);
			int[] report = uws.getBackupManager().restoreAll();
			String errorMsg = null;
			if (report == null || report.length == 0)
				errorMsg = "GRAVE error while the restoration of the asynchronous jobs!";
			else if (report.length < 4)
				errorMsg = "Incorrect restoration report format! => Impossible to know the restoration status!";
			else if (report[0] != report[1])
				errorMsg = "FAILED restoration of the asynchronous jobs: " + report[0] + " on " + report[1] + " restored!";
			else
				backupManager.setEnabled(true);

			if (errorMsg != null){
				errorMsg += " => Backup disabled.";
				service.getLogger().logTAP(LogLevel.FATAL, null, "ASYNC_INIT", errorMsg, null);
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, errorMsg);
			}
		}
	}

	/**
	 * <p>Notify this TAP resource that free DB connection(s) is(are) now available.
	 * It means that the execution manager should be refreshed in order to execute one or more queued jobs.</p>
	 * 
	 * <p><i>Note:
	 * 	This function has no effect if there is no execution manager.
	 * </i></p>
	 */
	public void freeConnectionAvailable(){
		if (jobList.getExecutionManager() != null)
			jobList.getExecutionManager().refresh();
	}

	@Override
	public String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public void setTAPBaseURL(final String baseURL){
		;
	}

	/**
	 * Get the UWS behind this TAP resource.
	 * 
	 * @return	The inner UWS used by this TAP resource.
	 */
	public final UWSService getUWS(){
		return uws;
	}

	@Override
	public void init(final ServletConfig config) throws ServletException{
		;
	}

	@Override
	public void destroy(){
		;
	}

	@Override
	public boolean executeResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException, TAPException{
		try{

			// Ensure the service is currently available:
			if (!service.isAvailable())
				throw new TAPException("Can not execute a query: this TAP service is not available! " + service.getAvailability(), UWSException.SERVICE_UNAVAILABLE);

			// Forward the request to the UWS service:
			return uws.executeRequest(request, response);

		}catch(UWSException ue){
			throw new TAPException(ue);
		}
	}

	/**
	 * An execution manager which queues jobs when too many asynchronous jobs are running or
	 * when no more DB connection is available for the moment.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.0 (09/2014)
	 * @since 2.0
	 */
	private class AsyncExecutionManager extends AbstractQueuedExecutionManager {

		/** A factory of TAP objects. */
		private final TAPFactory factory;

		/** The maximum number of running jobs. */
		protected int nbMaxRunningJobs = QueuedExecutionManager.NO_QUEUE;

		/**
		 * Build a queuing execution manager.
		 * 
		 * @param factory			Factory of TAP objects.
		 * @param logger			Logger to use.
		 * @param maxRunningJobs	Maximum number of asynchronous jobs that can run in the same time.
		 */
		public AsyncExecutionManager(final TAPFactory factory, UWSLog logger, int maxRunningJobs){
			super(logger);
			this.factory = factory;
			nbMaxRunningJobs = (maxRunningJobs <= 0) ? QueuedExecutionManager.NO_QUEUE : maxRunningJobs;
		}

		@Override
		public boolean isReadyForExecution(final UWSJob jobToExecute){
			if (!hasQueue())
				return factory.countFreeConnections() >= 1;
			else
				return (runningJobs.size() < nbMaxRunningJobs) && (factory.countFreeConnections() >= 1);
		}

	}

}
