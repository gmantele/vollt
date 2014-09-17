package uws.job.serializer;

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

import org.json.JSONException;
import org.json.Json4Uws;

import uws.job.ErrorSummary;
import uws.job.JobList;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSUrl;

/**
 * Lets serializing any UWS resource in JSON.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (08/2014)
 * 
 * @see Json4Uws
 */
public class JSONSerializer extends UWSSerializer {
	private static final long serialVersionUID = 1L;

	@Override
	public final String getMimeType(){
		return MIME_TYPE_JSON;
	}

	@Override
	public String getUWS(final UWS uws, final JobOwner user) throws JSONException{
		return Json4Uws.getJson(uws).toString();
	}

	@Override
	public String getJobList(final JobList jobsList, final JobOwner owner, final boolean root) throws JSONException{
		return Json4Uws.getJson(jobsList, owner).toString();
	}

	@Override
	public String getJob(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJson(job, null, false).toString();
	}

	@Override
	public String getJobRef(final UWSJob job, final UWSUrl jobsListUrl) throws JSONException{
		return Json4Uws.getJson(job, jobsListUrl, true).toString();
	}

	@Override
	public String getJobID(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJson(UWSJob.PARAM_JOB_ID, job.getJobId()).toString();
	}

	@Override
	public String getRunID(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJson(UWSJob.PARAM_RUN_ID, job.getRunId()).toString();
	}

	@Override
	public String getOwnerID(final UWSJob job, final boolean root) throws JSONException{
		if (job.getOwner() == null)
			return "{}";
		else
			return Json4Uws.getJson(UWSJob.PARAM_OWNER, job.getOwner().getPseudo()).toString();
	}

	@Override
	public String getPhase(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJson(UWSJob.PARAM_PHASE, job.getPhase().toString()).toString();
	}

	@Override
	public String getQuote(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJson(UWSJob.PARAM_QUOTE, job.getQuote()).toString();
	}

	@Override
	public String getExecutionDuration(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJson(UWSJob.PARAM_EXECUTION_DURATION, job.getExecutionDuration()).toString();
	}

	@Override
	public String getDestructionTime(final UWSJob job, final boolean root) throws JSONException{
		if (job.getDestructionTime() != null){
			return Json4Uws.getJson(UWSJob.PARAM_DESTRUCTION_TIME, UWSJob.dateFormat.format(job.getDestructionTime())).toString();
		}else
			return "{}";
	}

	@Override
	public String getStartTime(final UWSJob job, final boolean root) throws JSONException{
		if (job.getDestructionTime() != null)
			return Json4Uws.getJson(UWSJob.PARAM_START_TIME, UWSJob.dateFormat.format(job.getDestructionTime())).toString();
		else
			return "{}";
	}

	@Override
	public String getEndTime(final UWSJob job, final boolean root) throws JSONException{
		if (job.getDestructionTime() != null)
			return Json4Uws.getJson(UWSJob.PARAM_END_TIME, UWSJob.dateFormat.format(job.getDestructionTime())).toString();
		else
			return "{}";
	}

	@Override
	public String getErrorSummary(final ErrorSummary error, final boolean root) throws JSONException{
		return Json4Uws.getJson(error).toString();
	}

	@Override
	public String getAdditionalParameters(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJobParamsJson(job).toString();
	}

	@Override
	public String getAdditionalParameter(final String paramName, final Object paramValue, final boolean root) throws JSONException{
		return Json4Uws.getJson(paramName, (paramValue == null) ? null : paramValue.toString()).toString();
	}

	@Override
	public String getResults(final UWSJob job, final boolean root) throws JSONException{
		return Json4Uws.getJobResultsJson(job).toString();
	}

	@Override
	public String getResult(final Result result, final boolean root) throws JSONException{
		return Json4Uws.getJobResultJson(result).toString();
	}

}
