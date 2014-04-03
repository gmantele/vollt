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

import adql.parser.ParseException;
import adql.translator.TranslationException;
import uws.UWSException;

import uws.job.JobThread;

public class AsyncThread<R> extends JobThread {

	protected final ADQLExecutor<R> executor;

	public AsyncThread(TAPJob j, ADQLExecutor<R> executor) throws UWSException {
		super(j, "Execute the ADQL query of the TAP request "+j.getJobId());
		this.executor = executor;
	}

	@Override
	public void interrupt() {
		if (isAlive()){
			try {
				executor.closeDBConnection();
			} catch (TAPException e) {
				if (job != null && job.getLogger() != null)
					job.getLogger().error("Can not close the DBConnection for the executing job \""+job.getJobId()+"\" ! => the job will be probably not totally aborted.", e);
			}
		}
		super.interrupt();
	}

	@Override
	protected void jobWork() throws UWSException, InterruptedException {
		try{
			executor.start(this);
		}catch(InterruptedException ie){
			throw ie;
		}catch(UWSException ue){
			throw ue;
		}catch(TAPException te) {
			throw new UWSException(te.getHttpErrorCode(), te, te.getMessage());
		}catch(ParseException pe) {
			throw new UWSException(UWSException.BAD_REQUEST, pe, pe.getMessage());
		}catch(TranslationException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, te.getMessage());
		}catch(Exception ex) {
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ex, "Error while processing the ADQL query of the job "+job.getJobId()+" !");
		}finally{
			getTAPJob().setExecReport(executor.getExecReport());
		}
	}

	public final TAPJob getTAPJob(){
		return (TAPJob)job;
	}

}
