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

import java.util.List;

import tap.parameters.TAPParameters;
import tap.upload.TableLoader;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;

public class TAPJob extends UWSJob {

	private static final long serialVersionUID = 1L;

	public static final String PARAM_REQUEST = "request";
	public static final String REQUEST_DO_QUERY = "doQuery";
	public static final String REQUEST_GET_CAPABILITIES = "getCapabilities";

	public static final String PARAM_LANGUAGE = "lang";
	public static final String LANG_ADQL = "ADQL";
	public static final String LANG_PQL = "PQL";

	public static final String PARAM_VERSION = "version";
	public static final String VERSION_1_0 = "1.0";

	public static final String PARAM_FORMAT = "format";
	public static final String FORMAT_VOTABLE = "votable";

	public static final String PARAM_MAX_REC = "maxRec";
	public static final int UNLIMITED_MAX_REC = -1;

	public static final String PARAM_QUERY = "query";
	public static final String PARAM_UPLOAD = "upload";

	public static final String PARAM_PROGRESSION = "progression";

	protected TAPExecutionReport execReport;

	protected final TAPParameters tapParams;

	public TAPJob(final JobOwner owner, final TAPParameters tapParams) throws TAPException{
		super(owner, tapParams);
		this.tapParams = tapParams;
		tapParams.check();
		//progression = ExecutionProgression.PENDING;
		//loadTAPParams(tapParams);
	}

	public TAPJob(final String jobID, final JobOwner owner, final TAPParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws TAPException{
		super(jobID, owner, params, quote, startTime, endTime, results, error);
		this.tapParams = params;
		this.tapParams.check();
	}

	/*protected void loadTAPParams(TAPParameters params) {
		adqlQuery = params.query;
		additionalParameters.put(TAPParameters.PARAM_QUERY, adqlQuery);

		format = (params.format == null)?"application/x-votable+xml":params.format;
		additionalParameters.put(TAPParameters.PARAM_FORMAT, format);

		maxRec = params.maxrec;
		additionalParameters.put(TAPParameters.PARAM_MAX_REC, maxRec+"");

		upload = params.upload;
		tablesToUpload = params.tablesToUpload;
		additionalParameters.put(TAPParameters.PARAM_UPLOAD, upload);
	}*/

	/**
	 * @return The tapParams.
	 */
	public final TAPParameters getTapParams(){
		return tapParams;
	}

	public final String getRequest(){
		return tapParams.getRequest();
	}

	public final String getFormat(){
		return tapParams.getFormat();
	}

	public final String getLanguage(){
		return tapParams.getLang();
	}

	public final int getMaxRec(){
		return tapParams.getMaxRec();
	}

	public final String getQuery(){
		return tapParams.getQuery();
	}

	public final String getVersion(){
		return tapParams.getVersion();
	}

	public final String getUpload(){
		return tapParams.getUpload();
	}

	public final TableLoader[] getTablesToUpload(){
		return tapParams.getTableLoaders();
	}

	/**
	 * @return The execReport.
	 */
	public final TAPExecutionReport getExecReport(){
		return execReport;
	}

	/**
	 * @param execReport The execReport to set.
	 */
	public final void setExecReport(TAPExecutionReport execReport) throws UWSException{
		if (getRestorationDate() == null && !isRunning())
			throw new UWSException("Impossible to set an execution report if the job is not in the EXECUTING phase ! Here, the job \"" + jobId + "\" is in the phase " + getPhase());
		this.execReport = execReport;
	}

	/*
	 * <p>Starts in an asynchronous manner this ADQLExecutor.</p>
	 * <p>The execution will stop after the duration specified in the given {@link TAPJob}
	 * (see {@link TAPJob#getExecutionDuration()}).</p>
	 * 
	 * @param output
	 * @return
	 * @throws IllegalStateException
	 * @throws InterruptedException
	 *
	public synchronized final boolean startSync(final OutputStream output) throws IllegalStateException, InterruptedException, UWSException {
		// TODO Set the output stream so that the result is written directly in the given output !
		start();
		System.out.println("Joining...");
		thread.join(getExecutionDuration());
		System.out.println("Aborting...");
		thread.interrupt();
		thread.join(getTimeToWaitForEnd());
		return thread.isInterrupted();
	}*/

	@Override
	protected void stop(){
		if (!isStopped()){
			//try {
			stopping = true;
			// TODO closeDBConnection();
			super.stop();
			/*} catch (TAPException e) {
				getLogger().error("Impossible to cancel the query execution !", e);
				return;
			}*/
		}
	}

	/*protected boolean deleteResultFiles(){
		try{
			// TODO service.deleteResults(this);
			return true;
		}catch(TAPException ex){
			service.log(LogType.ERROR, "Job "+getJobId()+" - Can't delete results files: "+ex.getMessage());
			return false;
		}
	}*/

	@Override
	public void clearResources(){
		super.clearResources();
		// TODO deleteResultFiles();
	}

}
