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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.List;

import tap.parameters.TAPParameters;
import tap.upload.TableLoader;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;

/**
 * TODO JAVADOC OF THE WHOLE CLASS!
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (10/2014)
 */
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
	}

	public TAPJob(final String jobID, final JobOwner owner, final TAPParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws TAPException{
		super(jobID, owner, params, quote, startTime, endTime, results, error);
		this.tapParams = params;
		this.tapParams.check();
	}

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

}
