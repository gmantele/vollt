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

import java.io.Serializable;

import uws.UWSException;
import uws.UWSExceptionFactory;

import uws.job.ErrorSummary;
import uws.job.JobList;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;

import uws.service.UWSService;
import uws.service.UWS;
import uws.service.UWSUrl;

/**
 * <p>Lets returning any UWS resource in a given format.</p>
 * By default two formats are already implemented:
 * <ul>
 * 	<li>XML by the class {@link XMLSerializer}</li>
 * 	<li>JSON by the class {@link JSONSerializer}</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 * 
 * @see XMLSerializer
 * @see JSONSerializer
 */
public abstract class UWSSerializer implements Serializable {
	private static final long serialVersionUID = 1L;

	/** MIME type for XML: application/xml */
	public static final String MIME_TYPE_XML = "application/xml";
	/** MIME type for JSON: application/json */
	public static final String MIME_TYPE_JSON = "application/json";
	/** MIME type for TEXT: text/plain */
	public static final String MIME_TYPE_TEXT = "text/plain";
	/** MIME type for HTML: text/html */
	public static final String MIME_TYPE_HTML = "text/html";


	/**
	 * Serializes the given parameter of the given job
	 * or serializes the whole job if the given attributes array is empty or <i>null</i>.
	 * 
	 * @param job			The job whose the attribute must be serialized.
	 * @param attributes	All the given attributes (may be <i>null</i> or empty).
	 * @param root			<i>false</i> if the attribute to serialize will be included
	 * 						in a top level serialization (for a job attribute: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the given attribute
	 * 						or the serialization of the whole job if the given attributes array is empty or <i>null</i>.
	 * 
	 * @throws UWSException If the specified attribute/parameter/result does not exist.
	 */
	public String getJob(final UWSJob job, final String[] attributes, final boolean root) throws UWSException {
		if (attributes == null || attributes.length <= 0)
			return getJob(job, root);

		String firstAttribute = attributes[0];

		// JOB ID:
		if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_JOB_ID))
			return job.getJobId();
		// RUN ID:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_RUN_ID))
			return (job.getRunId() == null)?"":job.getRunId();
		// OWNER:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_OWNER))
			return (job.getOwner() == null)?"":job.getOwner().getPseudo();
		// PHASE:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_PHASE))
			return job.getPhase().toString();
		// QUOTE:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_QUOTE))
			return job.getQuote()+"";
		// START TIME:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_START_TIME))
			return (job.getStartTime() == null)?"":UWSJob.dateFormat.format(job.getStartTime());
		// END TIME:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_END_TIME))
			return (job.getEndTime() == null)?"":UWSJob.dateFormat.format(job.getEndTime());
		// EXECUTION DURATION:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_EXECUTION_DURATION))
			return job.getExecutionDuration()+"";
		// DESTRUCTION TIME:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_DESTRUCTION_TIME))
			return (job.getDestructionTime() == null)?"":UWSJob.dateFormat.format(job.getDestructionTime());
		// PARAMETERS LIST:
		else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_PARAMETERS)){
			if (attributes.length <= 1)
				return getAdditionalParameters(job, root);
			else{
				// PARAMETER:
				String secondAttribute = attributes[1];
				Object value = job.getAdditionalParameterValue(secondAttribute);
				if (value != null)
					return value.toString();
				else
					throw UWSExceptionFactory.incorrectJobParameter(job.getJobId(), secondAttribute);
			}
			// RESULTS LIST:
		} else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_RESULTS)){
			if (attributes.length <= 1)
				return getResults(job, root);
			else{
				// RESULT:
				String secondAttribute = attributes[1];
				Result r = job.getResult(secondAttribute);
				if (r != null)
					return getResult(r, root);
				else
					throw UWSExceptionFactory.incorrectJobResult(job.getJobId(), secondAttribute);
			}
			// ERROR DETAILS or ERROR SUMMARY:
		} else if (firstAttribute.equalsIgnoreCase(UWSJob.PARAM_ERROR_SUMMARY))
			if (job.getErrorSummary() != null && job.getErrorSummary().hasDetail())
				throw new UWSException(UWSException.SEE_OTHER, job.getErrorSummary().getDetails().toString());
			else
				return getErrorSummary(job.getErrorSummary(), root);
		// OTHERS:
		else
			throw UWSExceptionFactory.incorrectJobParameter(job.getJobId(), firstAttribute);
	}

	@Override
	public String toString(){
		return getMimeType();
	}

	/**
	 * Gets the MIME type of the serialization format used by this serializer.
	 * 
	 * @return	The corresponding MIME type.
	 */
	public abstract String getMimeType();

	/**
	 * Serializes the given UWS.
	 * 
	 * @param uws			The UWS to serialize.
	 * @return				The serialization of the given UWS.
	 * @throws UWSException	If there is an error during the serialization.
	 * 
	 * @see UWSSerializer#getUWS(UWSService, String)
	 */
	public String getUWS(final UWS uws) throws UWSException {
		return getUWS(uws, null);
	}

	/**
	 * Serializes the given UWS for the specified user.
	 * 
	 * @param uws			The UWS to serialize.
	 * @param user			The user which has asked the serialization of the given UWS.
	 * 
	 * @return				The serialization of the UWS.
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getUWS(final UWS uws, final JobOwner user) throws UWSException;

	/**
	 * Serializes the given jobs list.
	 * 
	 * @param jobsList		The jobs list to serialize.
	 * @param root			<i>false</i> if the jobs list to serialize will be included
	 * 						in a top level serialization (for a jobs list: uws), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the given jobs list.
	 * @throws UWSException If there is an error during the serialization.
	 */
	public String getJobList(final JobList jobsList, final boolean root) throws UWSException{
		return getJobList(jobsList, null, root);
	}

	/**
	 * Serializes the given jobs list.
	 * 
	 * @param jobsList		The jobs list to serialize.
	 * @param owner			The user which has asked the serialization of the given jobs list.
	 * @param root			<i>false</i> if the jobs list to serialize will be included
	 * 						in a top level serialization (for a jobs list: uws), <i>true</i> otherwise.
	 * @return				The serialization of the given jobs list.
	 * 
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getJobList(final JobList jobsList, JobOwner owner, final boolean root) throws UWSException;

	/**
	 * Serializes the whole given job.
	 * 
	 * @param job			The job to serialize.
	 * @param root			<i>false</i> if the job to serialize will be included
	 * 						in a top level serialization (for a job: jobList), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the given job.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getJob(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes just a reference on the given job.
	 * 
	 * @param job			The job to reference.
	 * @param jobsListUrl	URL to the jobs lists which contains the given job.
	 * 
	 * @return				The serialization of a reference on the given job.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 * 
	 * @since 3.1
	 */
	public abstract String getJobRef(final UWSJob job, final UWSUrl jobsListUrl) throws UWSException;

	/**
	 * Serializes the ID of the given job.
	 * 
	 * @param job			The job whose the ID must be serialized.
	 * @param root			<i>false</i> if the job ID to serialize will be included
	 * 						in a top level serialization (for a job ID: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the job ID.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getJobID(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the run ID of the given job.
	 * 
	 * @param job			The job whose the run ID must be serialized.
	 * @param root			<i>false</i> if the run ID to serialize will be included
	 * 						in a top level serialization (for a run ID: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the run ID.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getRunID(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the owner ID of the given job.
	 * 
	 * @param job			The job whose the owner ID must be serialized.
	 * @param root			<i>false</i> if the owner ID to serialize will be included
	 * 						in a top level serialization (for a owner ID: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the owner ID.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getOwnerID(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the phase of the given job.
	 * 
	 * @param job			The job whose the phase must be serialized.
	 * @param root			<i>false</i> if the phase to serialize will be included
	 * 						in a top level serialization (for a phase: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the phase.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getPhase(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the quote of the given job.
	 * 
	 * @param job			The job whose the quote must be serialized.
	 * @param root			<i>false</i> if the quote to serialize will be included
	 * 						in a top level serialization (for a quote: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the quote.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getQuote(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the start time of the given job.
	 * 
	 * @param job			The job whose the start time must be serialized.
	 * @param root			<i>false</i> if the start time to serialize will be included
	 * 						in a top level serialization (for a start time: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the start time.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getStartTime(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the end time of the given job.
	 * 
	 * @param job			The job whose the end time must be serialized.
	 * @param root			<i>false</i> if the end time to serialize will be included
	 * 						in a top level serialization (for a end time: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the end time.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getEndTime(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the execution duration of the given job.
	 * 
	 * @param job			The job whose the execution duration must be serialized.
	 * @param root			<i>false</i> if the execution duration to serialize will be included
	 * 						in a top level serialization (for a execution duration: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the execution duration.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getExecutionDuration(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the destruction time of the given job.
	 * 
	 * @param job			The job whose the destruction time must be serialized.
	 * @param root			<i>false</i> if the destruction time to serialize will be included
	 * 						in a top level serialization (for a destruction time: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the destruction time.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getDestructionTime(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the given error summary.
	 * 
	 * @param error			The error to serialize.
	 * @param root			<i>false</i> if the error summary to serialize will be included
	 * 						in a top level serialization (for an error summary: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the error summary.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getErrorSummary(final ErrorSummary error, final boolean root) throws UWSException;

	/**
	 * Serializes the results of the given job.
	 * 
	 * @param job			The job whose the results must be serialized.
	 * @param root			<i>false</i> if the results list to serialize will be included
	 * 						in a top level serialization (for a list of results: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the results.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getResults(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the given result.
	 * 
	 * @param result		The result to serialize.
	 * @param root			<i>false</i> if the result to serialize will be included
	 * 						in a top level serialization (for a result: results), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the result.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getResult(final Result result, final boolean root) throws UWSException;

	/**
	 * Serializes the parameters of the given job.
	 * 
	 * @param job			The job whose the parameters must be serialized.
	 * @param root			<i>false</i> if the parameters list to serialize will be included
	 * 						in a top level serialization (for a list of parameters: job), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the parameters.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getAdditionalParameters(final UWSJob job, final boolean root) throws UWSException;

	/**
	 * Serializes the specified parameter.
	 * 
	 * @param paramName		The name of the parameter to serialize.
	 * @param paramValue	The value of the parameter to serialize.
	 * @param root			<i>false</i> if the parameter to serialize will be included
	 * 						in a top level serialization (for a parameter: parameters), <i>true</i> otherwise.
	 * 
	 * @return				The serialization of the parameter.
	 * 
	 * @throws UWSException	If there is an error during the serialization.
	 */
	public abstract String getAdditionalParameter(final String paramName, final Object paramValue, final boolean root) throws UWSException;
}
