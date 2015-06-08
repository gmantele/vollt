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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import uws.UWSException;
import uws.job.JobThread;
import uws.service.error.ServiceErrorWriter;

/**
 * Thread in charge of a TAP job execution.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (02/2015)
 */
public class AsyncThread extends JobThread {

	/** The only object which knows how to execute an ADQL query. */
	protected final ADQLExecutor executor;

	/**
	 * Build a TAP asynchronous job execution.
	 * 
	 * @param j				Description of the job to execute.
	 * @param executor		The object to use for the ADQL execution itself.
	 * @param errorWriter	The object to use to format and to write an execution error for the user.
	 * 
	 * @throws NullPointerException	If the job parameter or the {@link ADQLExecutor} is missing.
	 */
	public AsyncThread(final TAPJob j, final ADQLExecutor executor, final ServiceErrorWriter errorWriter) throws NullPointerException{
		super(j, "Execute the ADQL query of the TAP request " + j.getJobId(), errorWriter);
		if (executor == null)
			throw new NullPointerException("Missing ADQLExecutor! Can not create an instance of AsyncThread without.");
		this.executor = executor;
	}

	/**
	 * <p>Check whether this thread is able to start right now.</p>
	 * 
	 * <p>
	 * 	Basically, this function asks to the {@link ADQLExecutor} to get a database connection. If no DB connection is available,
	 * 	then this thread can not start and this function return FALSE. In all the other cases, TRUE is returned.
	 * </p>
	 * 
	 * <p><b>Warning:</b> This function will indirectly open and keep a database connection, so that the job can be started just after its call.
	 * If it turns out that the execution won't start just after this call, the DB connection should be closed in some way in order to save database resources.</i></p>
	 * 
	 * @return	<i>true</i> if this thread can start right now, <i>false</i> otherwise.
	 * 
	 * @since 2.0
	 */
	public final boolean isReadyForExecution(){
		try{
			executor.initDBConnection(job.getJobId());
			return true;
		}catch(TAPException te){
			return false;
		}
	}

	@Override
	protected void jobWork() throws UWSException, InterruptedException{
		try{
			executor.start(this);
		}catch(InterruptedException ie){
			throw ie;
		}catch(UWSException ue){
			throw ue;
		}catch(Exception ex){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ex, "Error while processing the ADQL query of the job " + job.getJobId() + " !");
		}finally{
			getTAPJob().setExecReport(executor.getExecReport());
		}
	}

	/**
	 * Get the description of the job that this thread is executing.
	 * 
	 * @return	The executed job.
	 */
	public final TAPJob getTAPJob(){
		return (TAPJob)job;
	}

}
