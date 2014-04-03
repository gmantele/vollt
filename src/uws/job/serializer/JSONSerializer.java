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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import org.json.JSONException;
import org.json.Json4Uws;

import uws.UWSException;

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
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 * 
 * @see Json4Uws
 */
public class JSONSerializer extends UWSSerializer {
	private static final long serialVersionUID = 1L;


	@Override
	public final String getMimeType() {
		return MIME_TYPE_JSON;
	}

	@Override
	public String getUWS(final UWS uws, final JobOwner user) throws UWSException {
		try{
			return Json4Uws.getJson(uws).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getJobList(final JobList jobsList, final JobOwner owner, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(jobsList, owner).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getJob(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(job, null, false).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getJobRef(final UWSJob job, final UWSUrl jobsListUrl) throws UWSException {
		try{
			return Json4Uws.getJson(job, jobsListUrl, true).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getJobID(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(UWSJob.PARAM_JOB_ID, job.getJobId()).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getRunID(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(UWSJob.PARAM_RUN_ID, job.getRunId()).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getOwnerID(final UWSJob job, final boolean root) throws UWSException {
		if (job.getOwner() == null)
			return "{}";
		else{
			try{
				return Json4Uws.getJson(UWSJob.PARAM_OWNER, job.getOwner().getPseudo()).toString();
			}catch(JSONException je){
				throw new UWSException(je);
			}
		}
	}

	@Override
	public String getPhase(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(UWSJob.PARAM_PHASE, job.getPhase().toString()).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getQuote(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(UWSJob.PARAM_QUOTE, job.getQuote()).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getExecutionDuration(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(UWSJob.PARAM_EXECUTION_DURATION, job.getExecutionDuration()).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getDestructionTime(final UWSJob job, final boolean root) throws UWSException {
		if (job.getDestructionTime() != null){
			try{
				return Json4Uws.getJson(UWSJob.PARAM_DESTRUCTION_TIME, UWSJob.dateFormat.format(job.getDestructionTime())).toString();
			}catch(JSONException je){
				throw new UWSException(je);
			}
		}else
			return "{}";
	}

	@Override
	public String getStartTime(final UWSJob job, final boolean root) throws UWSException {
		if (job.getDestructionTime() != null){
			try{
				return Json4Uws.getJson(UWSJob.PARAM_START_TIME, UWSJob.dateFormat.format(job.getDestructionTime())).toString();
			}catch(JSONException je){
				throw new UWSException(je);
			}
		}else
			return "{}";
	}

	@Override
	public String getEndTime(final UWSJob job, final boolean root) throws UWSException {
		if (job.getDestructionTime() != null){
			try{
				return Json4Uws.getJson(UWSJob.PARAM_END_TIME, UWSJob.dateFormat.format(job.getDestructionTime())).toString();
			}catch(JSONException je){
				throw new UWSException(je);
			}
		}else
			return "{}";
	}

	@Override
	public String getErrorSummary(final ErrorSummary error, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(error).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getAdditionalParameters(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJobParamsJson(job).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getAdditionalParameter(final String paramName, final Object paramValue, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJson(paramName, (paramValue == null) ? null : paramValue.toString()).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getResults(final UWSJob job, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJobResultsJson(job).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

	@Override
	public String getResult(final Result result, final boolean root) throws UWSException {
		try{
			return Json4Uws.getJobResultJson(result).toString();
		}catch(JSONException je){
			throw new UWSException(je);
		}
	}

}
