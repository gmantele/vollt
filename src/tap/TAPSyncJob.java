package tap;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import tap.parameters.TAPParameters;
import uws.UWSException;
import uws.job.JobThread;

public class TAPSyncJob {

	/** The time (in ms) to wait the end of the thread after an interruption. */
	protected long waitForStop = 1000;

	protected static String lastId = null;

	protected final ServiceConnection<?> service;

	protected final String ID;
	protected final TAPParameters tapParams;

	protected SyncThread thread;

	protected TAPExecutionReport execReport = null;

	private Date startedAt = null;

	public TAPSyncJob(final ServiceConnection<?> service, final TAPParameters params) throws NullPointerException {
		if (params == null)
			throw new NullPointerException("Missing TAP parameters ! => Impossible to create a synchronous TAP job.");
		tapParams = params;
		tapParams.init();

		if (service == null)
			throw new NullPointerException("Missing the service description ! => Impossible to create a synchronous TAP job.");
		this.service = service;

		ID = generateId();
	}

	/**
	 * <p>This function lets generating a unique ID.</p>
	 * 
	 * <p><i><b>By default:</b> "S"+System.currentTimeMillis()+UpperCharacter (UpperCharacter: one upper-case character: A, B, C, ....)</i></p>
	 * 
	 * <p><i><u>note: </u> DO NOT USE in this function any of the following functions: {@link #getLogger()},
	 * {@link #getFileManager()} and {@link #getFactory()}. All of them will return NULL, because this job does not
	 * yet know its jobs list (which is needed to know the UWS and so, all of the objects returned by these functions).</i></p>
	 * 
	 * @return	A unique job identifier.
	 */
	protected String generateId() {
		String generatedId = "S"+System.currentTimeMillis()+"A";
		if (lastId != null){
			while(lastId.equals(generatedId))
				generatedId = generatedId.substring(0, generatedId.length()-1)+(char)(generatedId.charAt(generatedId.length()-1)+1);
		}
		lastId = generatedId;
		return generatedId;
	}

	public final String getID(){
		return ID;
	}

	public final TAPParameters getTapParams(){
		return tapParams;
	}

	public final TAPExecutionReport getExecReport() {
		return execReport;
	}

	public synchronized boolean start(final HttpServletResponse response) throws IllegalStateException, UWSException, TAPException {
		if (startedAt != null)
			throw new IllegalStateException("Impossible to restart a synchronous TAP query !");

		ADQLExecutor<?> executor;
		try {
			executor = service.getFactory().createADQLExecutor();
		} catch (TAPException e) {
			// TODO Log this error !
			return true;
		}
		thread = new SyncThread(executor, ID, tapParams, response);
		thread.start();
		boolean timeout = false;

		try{
			System.out.println("Joining...");
			thread.join(tapParams.getExecutionDuration());
			if (thread.isAlive()){
				timeout = true;
				System.out.println("Aborting...");
				thread.interrupt();
				thread.join(waitForStop);
			}
		}catch(InterruptedException ie){
			;
		}finally{
			execReport = thread.getExecutionReport();
		}

		if (!thread.isSuccess()){
			if (thread.isAlive())
				throw new TAPException("Time out (="+tapParams.getExecutionDuration()+"ms) ! However, the thread (synchronous query) can not be stopped !", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			else if (timeout)
				throw new TAPException("Time out ! The execution of this synchronous TAP query was limited to "+tapParams.getExecutionDuration()+"ms.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			else{
				Throwable t = thread.getError();
				if (t instanceof InterruptedException)
					throw new TAPException("The execution of this synchronous TAP query has been unexpectedly aborted !");
				else if (t instanceof UWSException)
					throw (UWSException)t;
				else
					throw new TAPException(t);
			}
		}

		return thread.isInterrupted();
	}

	public class SyncThread extends Thread {

		private final String taskDescription;
		public final ADQLExecutor<?> executor;
		protected final HttpServletResponse response;
		protected final String ID;
		protected final TAPParameters tapParams;
		protected Throwable exception = null;
		protected TAPExecutionReport report = null;

		public SyncThread(final ADQLExecutor<?> executor, final String ID, final TAPParameters tapParams, final HttpServletResponse response){
			super(JobThread.tg, ID);
			taskDescription = "Executing the synchronous TAP query "+ID;
			this.executor = executor;
			this.ID = ID;
			this.tapParams = tapParams;
			this.response = response;
		}

		public final boolean isSuccess(){
			return !isAlive() && report != null && exception == null;
		}

		public final Throwable getError(){
			return exception;
		}

		public final TAPExecutionReport getExecutionReport(){
			return report;
		}

		@Override
		public void run() {
			// Log the start of this thread:
			executor.getLogger().threadStarted(this, taskDescription);

			try {
				report = executor.start(this, ID, tapParams, response);
				executor.getLogger().threadFinished(this, taskDescription);
			} catch (Throwable e) {
				exception = e;
				if (e instanceof InterruptedException){
					// Log the abortion:
					executor.getLogger().threadInterrupted(this, taskDescription, e);
				}
			}
		}

	}

}
