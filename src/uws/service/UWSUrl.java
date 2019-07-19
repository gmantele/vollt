package uws.service;

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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import uws.UWSToolBox;
import uws.job.UWSJob;

/**
 * This class helps managing with UWS URLs and URIs.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (09/2014)
 */
public class UWSUrl implements Serializable {
	private static final long serialVersionUID = 1L;

	/** The whole request URL (i.e. http://foo.org/mySite/uws/jobList/job1/results/report). */
	protected String requestURL = null;									// http://cds-dev-gm:8080/uwstuto/basic/timers/job1/results/report

	/** The URL prefix (i.e. http://foo.org/mySite). */
	protected String urlHeader = null;									// http://cds-dev-gm:8080/uwstuto

	/** The request URI (i.e. /uws/jobList/job1/results/report) */
	protected String requestURI = null;									// /basic/timers/job1/results/report

	/** Base UWS URI (i.e. /uws). */
	protected final String baseURI;										// /basic

	/** The URI from the base UWS URI (i.e. /jobList/job1/results/report). */
	protected String uwsURI = null;										// /timers/job1/results/report

	/** Name of a jobs list found in uwsURI (i.e. jobList). */
	protected String jobListName = null;								// timers

	/** The JobID found in uwsURI (i.e. job1). */
	protected String jobId = null;										// job1

	/** Name of the job attribute found in uwsURI (i.e. {results, report}). */
	protected String[] attributes = new String[0];						// {results, report}

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds a copy of the given UWSUrl.
	 * 
	 * @param toCopy	The UWSUrl to copy.
	 */
	public UWSUrl(UWSUrl toCopy){
		requestURL = toCopy.requestURL;
		urlHeader = toCopy.urlHeader;
		requestURI = toCopy.requestURI;
		baseURI = toCopy.baseURI;
		uwsURI = toCopy.uwsURI;
		jobListName = toCopy.jobListName;
		jobId = toCopy.jobId;
		attributes = new String[toCopy.attributes.length];
		System.arraycopy(toCopy.attributes, 0, attributes, 0, attributes.length);
	}

	/**
	 * Builds a UWSUrl with a fixed baseURI.
	 * 
	 * @param baseURI			The baseURI to consider in all URL or request parsing.
	 * 
	 * @throws NullPointerException		If the given baseURI is <i>null</i> or is an empty string.
	 */
	public UWSUrl(String baseURI) throws NullPointerException{
		if (baseURI == null)
			throw new NullPointerException("The given base UWS URI is NULL!");

		this.baseURI = normalizeURI(baseURI);

		if (baseURI.length() == 0)
			throw new NullPointerException("The given base UWS URI is empty!");
	}

	/**
	 * Builds a UWSUrl considering the given request to set the baseURI.
	 * 
	 * @param request		The request to parse to get the baseURI.
	 * 
	 * @throws NullPointerException	If the given request is <i>null</i> or if the extracted baseURI is <i>null</i> or is an empty string.
	 * 
	 * @see #extractBaseURI(HttpServletRequest)
	 */
	public UWSUrl(HttpServletRequest request) throws NullPointerException{
		// Extract the base URI:
		String uri = extractBaseURI(request);
		if (uri == null)
			throw new NullPointerException("The extracted base UWS URI is NULL!");
		else
			baseURI = normalizeURI(uri);

		// Load the rest of the request:
		load(request);
	}

	/**
	 * Extracts the base UWS URI from the given request.
	 * 
	 * @param request	The request from which the base UWS URI must be extracted.
	 * 
	 * @return			The extracted URI (may be <i>null</i>).
	 */
	protected String extractBaseURI(HttpServletRequest request){
		if (request == null)
			return null;

		return request.getServletPath();
	}

	/**
	 * <p>Normalizes the given URI.</p>
	 * 
	 * <p><i><u>Note:</u> A normalized URI always starts with a / and ends with no /.</i></p>
	 * 
	 * @param uri	The URI to normalize.
	 * 
	 * @return		The normalized URI.
	 */
	protected static final String normalizeURI(String uri){
		uri = uri.trim();
		if (uri.length() > 0 && uri.charAt(0) != '/')
			uri = "/" + uri;

		// Trim out duplicate leading slashes
		while(uri.length() >= 2 && uri.charAt(1) == '/')
			uri = uri.substring(1);
		
		while(uri.length() >= 1 && uri.charAt(uri.length() - 1) == '/')
			uri = uri.substring(0, uri.length() - 1).trim();
		return uri.trim();
	}

	/* ************ */
	/* LOAD METHODS */
	/* ************ */
	/**
	 * <p>Parses and loads the given request.</p>
	 * 
	 * <p>
	 * 	Before all, {@link #extractBaseURI(HttpServletRequest)} is called so that extracting the base URI from the request.
	 * 	If this URI is different from the URI stored in this UWSUrl, {@link #load(URL)} is called so that parsing only the request URL
	 * 	and then this method ends immediately.
	 * </p>
	 * 
	 * <p>
	 * 	Otherwise this method sets its fields as following:
	 * 	<ul>
	 * 		<li>requestURL = {@link HttpServletRequest#getRequestURL()}</li>
	 * 		<li>urlHeader = {@link HttpServletRequest#getScheme()}+"://"+{@link HttpServletRequest#getServerName()}+":"+{@link HttpServletRequest#getServerPort()}+{@link HttpServletRequest#getContextPath()}</li>
	 * 		<li>requestURI = {@link HttpServletRequest#getRequestURI()}</li>
	 * 		<li>uwsURI = {@link HttpServletRequest#getPathInfo()}</li>
	 * 		<li>for jobListName, jobId and attributes, see {@link #loadUwsURI()}</li>
	 * 	</ul>
	 * </p>
	 * 
	 * <p><i><u>Note:</u> If the given request is NULL, all fields are set to NULL.</i></p>
	 * 
	 * @param request	The request to parse and to load.
	 * 
	 * @see #extractBaseURI(HttpServletRequest)
	 * @see #load(URL)
	 * @see #loadUwsURI()
	 */
	public void load(HttpServletRequest request){
		if (request == null){
			urlHeader = null;
			requestURL = null;
			requestURI = null;
			uwsURI = null;
		}else{
			if (extractBaseURI(request).equalsIgnoreCase(baseURI)){
				requestURL = request.getRequestURL().toString();
				urlHeader = (new StringBuffer(request.getScheme())).append("://").append(request.getServerName()).append(":").append(request.getServerPort()).append(request.getContextPath()).toString();
				requestURI = request.getRequestURI().substring(request.getRequestURI().indexOf(baseURI));
				uwsURI = request.getPathInfo();
			}else{
				URL url = null;
				try{
					url = new URL(request.getRequestURL().toString());
				}catch(MalformedURLException ex){
					;
				}
				load(url);
				return;
			}
		}

		loadUwsURI();
	}

	/**
	 * <p>Parses and loads the given request URL.</p>
	 * 
	 * <p>
	 * 	All the fields are set as following:
	 * 	<ul>
	 * 		<li>requestURL = requestUrl.substring(0, requestUrl.indexOf("?")</li>
	 * 		<li>urlHeader = requestURL.substring(0, requestURL.indexOf(baseURI))</li>
	 * 		<li>requestURI = requestURL.substring(requestURL.indexOf(baseURI))</li>
	 * 		<li>uwsURI = requestURI.substring(baseURI.length())</li>
	 * 		<li>for jobListName, jobId and attributes, see {@link #loadUwsURI()}</li>
	 * 	</ul>
	 * </p>
	 * 
	 * <p><i><u>Note:</u> If the given URL is NULL, all fields are set to NULL.</i></p>
	 * 
	 * @param requestUrl	The URL to parse and to load.
	 * 
	 * @see #loadUwsURI()
	 */
	public void load(URL requestUrl){
		if (requestUrl == null){
			urlHeader = null;
			requestURL = null;
			requestURI = null;
			uwsURI = null;
		}else{
			requestURL = requestUrl.toString();
			if (requestURL.indexOf("?") > 0)
				requestURL = requestURL.substring(0, requestURL.indexOf("?"));

			int indBaseURI = requestURL.indexOf(baseURI);
			if (indBaseURI >= 0){
				urlHeader = requestURL.substring(0, indBaseURI);
				requestURI = requestURL.substring(indBaseURI);
				uwsURI = requestURI.substring(baseURI.length());
			}else{
				urlHeader = null;
				requestURI = null;
				uwsURI = null;
			}
		}

		loadUwsURI();
	}

	/**
	 * <p>Loads and parses the URI stored in the member {@link #uwsURI}.</p>
	 * 
	 * <p>
	 * 	The URI is split by the / character. The items of the resulting array corresponds to:
	 * 	<ul>
	 * 		<li>item 0 = empty (the empty string before the first / of a URI)</li>
	 * 		<li>item 1 = jobListName</li>
	 * 		<li>item 2 = jobId</li>
	 * 		<li>item 3 and more = attributes</li>
	 * 	</ul>
	 * </p>
	 * 
	 * <p><i><u>Note:</u> If {@link #uwsURI} is NULL, jobListName and jobId are set to null while attributes is set to an empty array.</i></p>
	 */
	protected void loadUwsURI(){
		jobListName = null;
		jobId = null;
		attributes = new String[0];

		if (uwsURI != null){
			// URI normalization: it must always begin with a / but never ends with a / !
			uwsURI = normalizeURI(uwsURI);

			String[] uriParts = uwsURI.split("/");

			// uwsURI begins always with a / so uriParts[0] is always null !

			if (uriParts.length >= 2){
				jobListName = uriParts[1].trim();
				if (jobListName.length() == 0)
					jobListName = null;
			}

			if (uriParts.length >= 3){
				jobId = uriParts[2].trim();
				if (jobId.length() == 0)
					jobId = null;
			}

			if (uriParts.length >= 4){
				attributes = new String[uriParts.length - 3];
				for(int i = 3; i < uriParts.length; i++)
					attributes[i - 3] = uriParts[i].trim();
			}
		}
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	/**
	 * Gets the base UWS URI given at the initialization of this instance of {@link UWSUrl}.
	 * 
	 * @return The baseUri.
	 */
	public final String getBaseURI(){
		return baseURI;
	}

	/**
	 * Gets the <b>SUPPOSED</b> name of the UWS from its baseURI.
	 * 
	 * @return	The presumed UWS name.
	 */
	public final String getUWSName(){
		return baseURI.substring(baseURI.lastIndexOf('/') + 1);
	}

	/**
	 * Gets the request URL.
	 * 
	 * @return	The last loaded request URL.
	 */
	public final String getRequestURL(){
		return requestURL;
	}

	/**
	 * <p>Gets the URL header of the request URL.</p>
	 * 
	 * <p>
	 * 	<i><u>Example:</u>
	 * 		If the base URI is "/uws" and the request URL is "http://foo.org/mySite/uws/jobList/job1/results/report", then the URL header will be:
	 * 		"http://foo.org/mySite".
	 * 	</i>
	 * </p>
	 * 
	 * @return	The last loaded URL header.
	 */
	public final String getUrlHeader(){
		return urlHeader;
	}

	/**
	 * <p>Gets the request URI.</p>
	 * 
	 * <p>
	 * 	<i><u>Example:</u>
	 * 		If the base URI is "/uws" and the request URL is "http://foo.org/mySite/uws/jobList/job1/results/report", then the request URI will be:
	 * 		"/uws/jobList/jobId/results/report".
	 * 	</i>
	 * </p>
	 * 
	 * @return The last loaded request URI.
	 */
	public final String getRequestURI(){
		return requestURI;
	}

	/**
	 * <p>Gets the UWS URI.</p>
	 * 
	 * <p>
	 * 	<i><u>Example:</u>
	 * 		If the base URI is "/uws" and the request URL is "http://foo.org/mySite/uws/jobList/job1/results/report", then the request URI will be:
	 * 		"/jobList/jobId/results/report".
	 * 	</i>
	 * </p>
	 * 
	 * @return	The extracted UWS URI.
	 */
	public final String getUwsURI(){
		return uwsURI;
	}

	/**
	 * Tells whether the last loaded request or request URL contains a jobs list name.
	 * 
	 * @return	<i>true</i> if a jobs list name has been extracted, <i>false</i> otherwise.
	 */
	public final boolean hasJobList(){
		return jobListName != null;
	}

	/**
	 * Gets the jobs list name extracted from the last parsed request or request URL.
	 * 
	 * @return The extracted jobs list name.
	 */
	public final String getJobListName(){
		return jobListName;
	}

	/**
	 * Tells whether the last loaded request or request URL contains a job ID.
	 * 
	 * @return	<i>true</i> if a job ID has been extracted, <i>false</i> otherwise.
	 */
	public final boolean hasJob(){
		return jobId != null;
	}

	/**
	 * Gets the job ID extracted from the last parsed request or request URL.
	 * 
	 * @return The extracted job ID.
	 */
	public final String getJobId(){
		return jobId;
	}

	/**
	 * Tells whether the last loaded request or request URL contains at least one job attribute.
	 * 
	 * @return	<i>true</i> if at least one job attribute has been extracted, <i>false</i> otherwise.
	 */
	public final boolean hasAttribute(){
		return attributes != null && attributes.length > 0;
	}

	/**
	 * Tells whether the last loaded request or request URL contains a job attribute with the given name.
	 * 
	 * @param attributeName	The name of the job attribute expected in the last request or request URL.
	 * 
	 * @return				<i>true</i> if the specified job attribute has been extracted, <i>false</i> otherwise.
	 */
	public final boolean hasAttribute(String attributeName){
		for(String att : attributes){
			if (att.equalsIgnoreCase(attributeName))
				return true;
		}
		return false;
	}

	/**
	 * Gets all the job attributes extracted from the last request or request URL.
	 * 
	 * @return	The extracted job attributes.
	 */
	public final String[] getAttributes(){
		return attributes;
	}

	/* ******* */
	/* SETTERS */
	/* ******* */
	/**
	 * <p>Updates the field {@link #uwsURI} in function of {@link #jobListName}, {@link #jobId} and {@link #attributes} as following:
	 * uwsURI = "/"+jobListName+"/"+jobId+"/"+attributes.</p>
	 * 
	 * <p>Once {@link #uwsURI} updated the request URL and URI are also updated.</p>
	 * 
	 * @see #updateRequestURL()
	 */
	protected void updateUwsURI(){
		uwsURI = null;
		if (hasJobList()){
			StringBuffer uri = new StringBuffer("/");
			uri.append(jobListName);
			if (hasJob()){
				uri.append("/").append(jobId);
				if (hasAttribute()){
					for(String att : attributes)
						uri.append("/").append(att);
				}
			}
			uwsURI = uri.toString();
		}

		updateRequestURL();
	}

	/**
	 * <p>
	 * 	Updates the fields {@link #requestURI} and {@link #requestURL} as following:
	 * 	<ul>
	 * 		<li>requestURI = baseURI+uwsURI</li>
	 * 		<li>requestURL = urlHeader+requestURI (or <i>null</i> if urlHeader is <i>null</i>)</li>
	 * 	</ul>
	 * </p>
	 */
	protected void updateRequestURL(){
		requestURI = baseURI + ((uwsURI != null) ? uwsURI : "");
		requestURL = (urlHeader == null) ? null : (urlHeader + requestURI);
	}

	/**
	 * Sets the whole UWS URI (that is to say a URI starting with the jobs list name).
	 * Once done all the other fields of this UWS URL are updated.
	 * 
	 * @param uwsURI	The UWS URI to set.
	 * 
	 * @see #loadUwsURI()
	 * @see #updateRequestURL()
	 */
	public final void setUwsURI(String uwsURI){
		if (uwsURI == null || uwsURI.trim().length() == 0)
			this.uwsURI = null;
		else
			this.uwsURI = uwsURI.trim();

		loadUwsURI();
		updateRequestURL();
	}

	/**
	 * Sets the jobs list name.
	 * Once done all the other fields of this UWS URL are updated.
	 * 
	 * @param jobListName A jobs list name.
	 * 
	 * @see #updateUwsURI()
	 */
	public final void setJobListName(String jobListName){
		this.jobListName = jobListName;
		updateUwsURI();
	}

	/**
	 * Sets the job ID.
	 * Once done all the other fields of this UWS URL are updated.
	 * 
	 * @param jobId	A job ID.
	 * 
	 * @see #updateUwsURI()
	 */
	public final void setJobId(String jobId){
		this.jobId = jobId;
		updateUwsURI();
	}

	/**
	 * <p>Sets all the job attributes.
	 * Once done all the other fields of this UWS URL are updated.</p>
	 * 
	 * <p><i><u>Note:</u> The given array is entirely copied.</i></p>
	 * 
	 * @param newAttributes	The new job attributes.
	 * 
	 * @see #updateUwsURI()
	 */
	public final void setAttributes(String[] newAttributes){
		if (newAttributes == null)
			attributes = new String[0];

		attributes = new String[newAttributes.length];
		System.arraycopy(newAttributes, 0, attributes, 0, attributes.length);

		updateUwsURI();
	}

	/* ******************** */
	/* URL BUILDING METHODS */
	/* ******************** */
	/** Gets the base UWS URI = UWS home page. */
	public final UWSUrl homePage(){
		UWSUrl url = new UWSUrl(this);
		url.setUwsURI(null);
		return url;
	}

	/** Gets the UWS URL to get the specified <b>jobs list</b>. */
	public final UWSUrl listJobs(String jobListName){
		UWSUrl url = homePage();
		url.setJobListName(jobListName);
		return url;
	}

	/** Gets the UWS URL to get the <b>summary</b>. */
	public final UWSUrl jobSummary(String jobListName, String jobId){
		UWSUrl url = listJobs(jobListName);
		url.setJobId(jobId);
		return url;
	}

	/** Gets the UWS URL to get the <b>runID</b>. */
	public final UWSUrl jobName(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_RUN_ID});
		return url;
	}

	/** Gets the UWS URL to get the <b>phase</b>. */
	public final UWSUrl jobPhase(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_PHASE});
		return url;
	}

	/** Gets the UWS URL to get the <b>execution duration</b>. */
	public final UWSUrl jobExecDuration(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_EXECUTION_DURATION});
		return url;
	}

	/** Gets the UWS URL to get the <b>destruction time</b>. */
	public final UWSUrl jobDestruction(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_DESTRUCTION_TIME});
		return url;
	}

	/** Gets the UWS URL to get the <b>error summary</b>. */
	public final UWSUrl jobError(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_ERROR_SUMMARY});
		return url;
	}

	/** Gets the UWS URL to get the <b>quote</b>. */
	public final UWSUrl jobQuote(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_QUOTE});
		return url;
	}

	/** Gets the UWS URL to get the <b>results</b>. */
	public final UWSUrl jobResults(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_RESULTS});
		return url;
	}

	/** Gets the UWS URL to get the <b>specified result</b>. */
	public final UWSUrl jobResult(String jobListName, String jobId, String resultId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_RESULTS,resultId});
		return url;
	}

	/** Gets the UWS URL to get the <b>parameters</b>. */
	public final UWSUrl jobParameters(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_PARAMETERS});
		return url;
	}

	/** Gets the UWS URL to get the <b>parameters/parameter</b>. */
	public final UWSUrl jobParameter(String jobListName, String jobId, String paramName){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_PARAMETERS,paramName});
		return url;
	}

	/** Gets the UWS URL to get the <b>owner ID</b>. */
	public final UWSUrl jobOwner(String jobListName, String jobId){
		UWSUrl url = jobSummary(jobListName, jobId);
		url.setAttributes(new String[]{UWSJob.PARAM_OWNER});
		return url;
	}

	/* ******************************* */
	/* URL BUILDING METHODS (HTTP GET) */
	/* ******************************* */
	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to <b>create</b> a job with the given parameters. */
	public final String createJob(String jobListName, Map<String,String> parameters){
		return listJobs(jobListName) + "?" + UWSToolBox.getQueryPart(parameters);
	}

	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to <b>delete</b> the specified job. */
	public final String deleteJob(String jobListName, String jobId){
		return jobSummary(jobListName, jobId) + "?" + UWSJob.PARAM_ACTION + "=" + UWSJob.ACTION_DELETE;
	}

	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to <b>start</b> the specified job. */
	public final String startJob(String jobListName, String jobId){
		return jobPhase(jobListName, jobId) + "?" + UWSJob.PARAM_PHASE.toUpperCase() + "=" + UWSJob.PHASE_RUN;
	}

	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to <b>abort</b> the specified job. */
	public final String abortJob(String jobListName, String jobId){
		return jobPhase(jobListName, jobId) + "?" + UWSJob.PARAM_PHASE.toUpperCase() + "=" + UWSJob.PHASE_ABORT;
	}

	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to change the run ID. */
	public final String changeJobName(String jobListName, String jobId, String newName){
		return jobName(jobListName, jobId) + "?" + UWSJob.PARAM_RUN_ID.toUpperCase() + "=" + newName;
	}

	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to change the destruction time. */
	public final String changeDestructionTime(String jobListName, String jobId, String newDestructionTime){
		return jobDestruction(jobListName, jobId) + "?" + UWSJob.PARAM_DESTRUCTION_TIME.toUpperCase() + "=" + newDestructionTime;
	}

	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to change the execution duration. */
	public final String changeExecDuration(String jobListName, String jobId, String newExecDuration){
		return jobExecDuration(jobListName, jobId) + "?" + UWSJob.PARAM_EXECUTION_DURATION.toUpperCase() + "=" + newExecDuration;
	}

	/** Gets the UWS URL <b>(HTTP-GET ONLY)</b> to change the specified parameter. */
	public final String changeJobParam(String jobListName, String jobId, String paramName, String paramValue){
		return jobParameters(jobListName, jobId) + "?" + paramName.toUpperCase() + "=" + paramValue;
	}

	/**
	 * Gets the full request URL corresponding to this UWSUrl.
	 * 
	 * @return	The corresponding request URL.
	 * 
	 * @throws MalformedURLException If there is an error while building the URL object from the {@link #requestURL} field.
	 * 
	 * @see #getRequestURL()
	 */
	public URL toURL() throws MalformedURLException{
		String url = getRequestURL();
		return (url != null) ? (new URL(url)) : null;
	}

	/**
	 * Gets the full request URI corresponding to this UWSUrl.
	 * 
	 * @return	The corresponding request URI.
	 * 
	 * @see #getRequestURI()
	 */
	public String toURI(){
		return getRequestURI();
	}

	/**
	 * Gets the corresponding request URL.
	 * 
	 * @see #getRequestURL()
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		return getRequestURL();
	}

}
