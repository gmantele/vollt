package uws.service.actions;

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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.UWSToolBox;

import uws.job.ErrorSummary;
import uws.job.Result;
import uws.job.UWSJob;

import uws.job.serializer.UWSSerializer;

import uws.job.user.JobOwner;

import uws.service.UWSService;
import uws.service.UWSUrl;

/**
 * <p>The "Get Job Parameter" action of a UWS.</p>
 * 
 * <p><i><u>Note:</u> The corresponding name is {@link UWSAction#GET_JOB_PARAM}.</i></p>
 * 
 * <p>This action returns the value of the specified job attribute.
 * If the attribute is basic (i.e. jobID, startTime, ...) the value is returned with the content type <i>text/plain</i>
 * whereas it is a complex type (i.e. results, parameters, ...) the value is the serialization of the job attribute itself.
 * The serializer is choosen in function of the HTTP Accept header.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public class GetJobParam extends UWSAction {
	private static final long serialVersionUID = 1L;

	public GetJobParam(UWSService u){
		super(u);
	}

	/**
	 * @see UWSAction#GET_JOB_PARAM
	 * @see uws.service.actions.UWSAction#getName()
	 */
	@Override
	public String getName() {
		return GET_JOB_PARAM;
	}

	@Override
	public String getDescription() {
		return "Gets the value of a job attribute/parameter of the specified job. (URL: {baseUWS_URL}/{jobListName}/{job-id}/{job-attribute}, Method: HTTP-GET, No parameter)";
	}

	/**
	 * Checks whether:
	 * <ul>
	 * 	<li>a job list name is specified in the given UWS URL <i>(<u>note:</u> the existence of the jobs list is not checked)</i>,</li>
	 * 	<li>a job ID is given in the UWS URL <i>(<u>note:</u> the existence of the job is not checked)</i>,</li>
	 * 	<li>there is a job attribute,</li>
	 * 	<li>the HTTP method is HTTP-GET.</li>
	 * </ul>
	 * 
	 * @see uws.service.actions.UWSAction#match(uws.service.UWSUrl, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException {
		return (urlInterpreter.hasJobList()
				&& urlInterpreter.hasJobList()
				&& urlInterpreter.hasAttribute()
				&& request.getMethod().equalsIgnoreCase("get"));
	}

	/**
	 * <p>Gets the specified job <i>(and throw an error if not found)</i>,
	 * chooses the serializer and write the serialization of the job attribute in the given response.</p>
	 * 
	 * <p><i><u>Note:</u> if the specified attribute is simple (i.e. jobID, runID, startTime, ...) it will not serialized ! The response will
	 * merely be the job attribute value (so, the content type will be: text/plain).</i></p>
	 * 
	 * @see #getJob(UWSUrl, String)
	 * @see UWSService#getSerializer(String)
	 * @see UWSJob#serialize(ServletOutputStream, UWSSerializer)
	 * 
	 * @see uws.service.actions.UWSAction#apply(uws.service.UWSUrl, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException {
		// Get the job:
		UWSJob job = getJob(urlInterpreter, user);

		String[] attributes = urlInterpreter.getAttributes();

		// RESULT CASE: Display the content of the selected result:
		if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_RESULTS) && attributes.length > 1){
			Result result = job.getResult(attributes[1]);
			if (result == null)
				throw UWSExceptionFactory.incorrectJobResult(job.getJobId(), attributes[1]);
			else if (result.isRedirectionRequired())
				uws.redirect(result.getHref(), request, user, getName(), response);
			else{
				InputStream input = null;
				try{
					input = uws.getFileManager().getResultInput(result, job);
					UWSToolBox.write(input, result.getMimeType(), result.getSize(), response);
				}catch(IOException ioe){
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Can not read the content of the result "+result.getId()+" (job ID: "+job.getJobId()+").");
				}finally{
					if (input != null)
						input.close();
				}
			}
		}// ERROR DETAILS CASE: Display the full stack trace of the error:
		else if (attributes[0].equalsIgnoreCase(UWSJob.PARAM_ERROR_SUMMARY) && attributes.length > 1 && attributes[1].equalsIgnoreCase("details")){
			ErrorSummary error = job.getErrorSummary();
			if (error == null)
				throw UWSExceptionFactory.noErrorSummary(job.getJobId());
			else{
				InputStream input = null;
				try{
					input = uws.getFileManager().getErrorInput(error, job);
					UWSToolBox.write(input, "text/plain", uws.getFileManager().getErrorSize(error, job), response);
				}catch(IOException ioe){
					throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Can not read the error details (job ID: "+job.getJobId()+").");
				}finally{
					if (input != null)
						input.close();
				}
			}
		}
		// DEFAULT CASE: Display the serialization of the selected UWS object:
		else{
			// Write the value/content of the selected attribute:
			UWSSerializer serializer = uws.getSerializer(request.getHeader("Accept"));
			String uwsField = attributes[0];
			if (uwsField == null || uwsField.trim().isEmpty()	|| (attributes.length <= 1
					&& (uwsField.equalsIgnoreCase(UWSJob.PARAM_ERROR_SUMMARY)
							|| uwsField.equalsIgnoreCase(UWSJob.PARAM_RESULTS)
							|| uwsField.equalsIgnoreCase(UWSJob.PARAM_PARAMETERS))))
				response.setContentType(serializer.getMimeType());
			else
				response.setContentType("text/plain");
			job.serialize(response.getOutputStream(), attributes, serializer);
		}

		return true;
	}



	/**
	 * Tells whether the URL of this Result is the default one.
	 * In this case, the result must be read from the result file and written in response of the HTTP request.
	 * In the other case, the result content can be fetched only by a redirection to the given URL.
	 * 
	 * @return	<i>true</i> if the URL of this Result is the default one, <i>false</i> if a redirection to its URL must be done to get the result content.
	 */
	public final boolean isDefaultUrl(final Result result, final UWSJob job){
		if (result.getHref() == null || result.getHref().trim().isEmpty())
			return true;
		else
			return result.getHref().equals(Result.getDefaultUrl(result.getId(), job));
	}

}
