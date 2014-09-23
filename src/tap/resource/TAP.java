package tap.resource;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.error.DefaultTAPErrorWriter;
import tap.formatter.OutputFormat;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uk.ac.starlink.votable.VOSerializer;
import uws.UWSException;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.error.ServiceErrorWriter;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Root/Home of the TAP service. It is also the resource (HOME) which gathers all the others of the same TAP service.</p>
 * 
 * <p>At its creation it is creating and configuring the other resources in function of the given description of the TAP service.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public class TAP implements VOSIResource {

	/** Description of the TAP service owning this resource. */
	protected final ServiceConnection service;

	/** List of all the other TAP resources of the service. */
	protected final Map<String,TAPResource> resources;

	/** Base URL of the TAP service. It is also the URL of this resource (HOME). */
	protected String tapBaseURL = null;

	/** URI of the page or path of the file to display when this resource is requested. */
	protected String homePageURI = null;

	/** MIME type of the custom home page. By default, it is "text/html". */
	protected String homePageMimeType = "text/html";

	/** Object to use when an error occurs or comes until this resource from the others.
	 * This object fills the HTTP response in the most appropriate way in function of the error. */
	protected ServiceErrorWriter errorWriter;

	/** Last generated request ID. If the next generated request ID is equivalent to this one,
	 * a new one will generate in order to ensure the uniqueness.
	 * @since 2.0 */
	protected static String lastRequestID = null;

	/**
	 * Build a HOME resource of a TAP service whose the description is given in parameter.
	 * All the other TAP resources will be created and configured here thanks to the given {@link ServiceConnection}. 
	 * 
	 * @param serviceConnection	Description of the TAP service.
	 * 
	 * @throws UWSException	If an error occurs while creating the /async resource.
	 * @throws TAPException	If any other error occurs.
	 */
	public TAP(ServiceConnection serviceConnection) throws UWSException, TAPException{
		service = serviceConnection;
		resources = new HashMap<String,TAPResource>();

		errorWriter = new DefaultTAPErrorWriter(service);

		TAPResource res = new Availability(service);
		resources.put(res.getName(), res);

		res = new Capabilities(this);
		resources.put(res.getName(), res);

		res = new Sync(service, (Capabilities)res);
		resources.put(res.getName(), res);

		res = new ASync(service);
		resources.put(res.getName(), res);

		TAPMetadata metadata = service.getTAPMetadata();
		if (metadata != null)
			resources.put(metadata.getName(), metadata);
	}

	/**
	 * Get the logger used by this resource and all the other resources managed by it.
	 * 
	 * @return	The used logger.
	 */
	public final TAPLog getLogger(){
		return service.getLogger();
	}

	/**
	 * <p>Let initialize this resource and all the other managed resources.</p>
	 * 
	 * <p>This function is called by the library just once: when the servlet is initialized.</p>
	 * 
	 * @param config	Configuration of the servlet.
	 * 
	 * @throws ServletException	If any error occurs while reading the given configuration.
	 * 
	 * @see TAPResource#init(ServletConfig)
	 */
	public void init(final ServletConfig config) throws ServletException{
		for(TAPResource res : resources.values())
			res.init(config);
	}

	/**
	 * <p>Free all the resources used by this resource and the other managed resources.</p>
	 * 
	 * <p>This function is called by the library just once: when the servlet is destroyed.</p>
	 * 
	 * @see TAPResource#destroy()
	 */
	public void destroy(){
		for(TAPResource res : resources.values())
			res.destroy();
	}

	/**
	 * <p>Set the base URL of this TAP service.</p>
	 * 
	 * <p>
	 * 	This URL must be the same as the one of this resource ; it corresponds to the
	 * 	URL of the root (or home) of the TAP service.
	 * </p>
	 * 
	 * <p>The given URL will be propagated to the other TAP resources automatically.</p>
	 * 
	 * @param baseURL	URL of this resource.
	 * 
	 * @see TAPResource#setTAPBaseURL(String)
	 */
	public void setTAPBaseURL(final String baseURL){
		tapBaseURL = baseURL;
		for(TAPResource res : resources.values())
			res.setTAPBaseURL(tapBaseURL);
	}

	/**
	 * <p>Build the base URL from the given HTTP request, and use it to set the base URL of this TAP service.</p>
	 * 
	 * <p>The given URL will be propagated to the other TAP resources automatically.</p>
	 * 
	 * @param request	HTTP request from which a TAP service's base URL will be extracted.
	 * 
	 * @see #setTAPBaseURL(String)
	 */
	public void setTAPBaseURL(final HttpServletRequest request){
		setTAPBaseURL(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + request.getServletPath());
	}

	/* ******************** */
	/* RESOURCES MANAGEMENT */
	/* ******************** */

	/**
	 * Get the /availability resource of this TAP service.
	 * 
	 * @return	The /availability resource.
	 */
	public final Availability getAvailability(){
		return (Availability)resources.get(Availability.RESOURCE_NAME);
	}

	/**
	 * Get the /capabilities resource of this TAP service.
	 * 
	 * @return	The /capabilities resource.
	 */
	public final Capabilities getCapabilities(){
		return (Capabilities)resources.get(Capabilities.RESOURCE_NAME);
	}

	/**
	 * Get the /sync resource of this TAP service.
	 * 
	 * @return	The /sync resource.
	 */
	public final Sync getSync(){
		return (Sync)resources.get(Sync.RESOURCE_NAME);
	}

	/**
	 * Get the /async resource of this TAP service.
	 * 
	 * @return	The /async resource.
	 */
	public final ASync getASync(){
		return (ASync)resources.get(ASync.RESOURCE_NAME);
	}

	/**
	 * Get the UWS service used for the /async service.
	 * 
	 * @return	The used UWS service.
	 */
	public final UWSService getUWS(){
		TAPResource res = getASync();
		if (res != null)
			return ((ASync)res).getUWS();
		else
			return null;
	}

	/**
	 * <p>Get the object managing all the metadata (information about the published columns and tables)
	 * of this TAP service.</p>
	 * 
	 * <p>This object is also to the /tables resource.</p>
	 * 
	 * @return	List of all metadata of this TAP service.
	 */
	public final TAPMetadata getTAPMetadata(){
		return (TAPMetadata)resources.get(TAPMetadata.RESOURCE_NAME);
	}

	/**
	 * <p>Add the given resource in this TAP service.</p>
	 * 
	 * <p>The ID of this resource (which is also its URI) will be its name (given by {@link TAPResource#getName()}).</p>
	 * 
	 * <p><b>WARNING:
	 * 	If another resource with an ID strictly identical (case sensitively) to the name of the given resource, it will be overwritten!
	 * 	You should check (thanks to {@link #hasResource(String)}) before calling this function that no resource is associated with the same URI.
	 * 	If it is the case, you should then use the function {@link #addResource(String, TAPResource)} with a different ID/URI.
	 * </b></p>
	 * 
	 * <p><i>Note:
	 * 	This function is equivalent to {@link #addResource(String, TAPResource)} with {@link TAPResource#getName()} in first parameter.
	 * </i></p>
	 * 
	 * @param newResource	Resource to add in the service.
	 * 
	 * @return	<i>true</i> if the given resource has been successfully added,
	 *        	<i>false</i> otherwise (and particularly if the given resource is NULL).
	 * 
	 * @see #addResource(String, TAPResource)
	 */
	public final boolean addResource(final TAPResource newResource){
		return addResource(newResource.getName(), newResource);
	}

	/**
	 * <p>Add the given resource in this TAP service with the given ID (which will be also the URI to access this resource).</p>
	 * 
	 * <p><b>WARNING:
	 * 	If another resource with an ID strictly identical (case sensitively) to the name of the given resource, it will be overwritten!
	 * 	You should check (thanks to {@link #hasResource(String)}) before calling this function that no resource is associated with the same URI.
	 * 	If it is the case, you should then use the function {@link #addResource(String, TAPResource)} with a different ID/URI.
	 * </b></p>
	 * 
	 * <p><i>Note:
	 * 	If the given ID is NULL, the name of the resource will be used.
	 * </i></p>
	 * 
	 * @param resourceId	ID/URI of the resource to add.
	 * @param newResource	Resource to add.
	 * 
	 * @return	<i>true</i> if the given resource has been successfully added to this service with the given ID/URI,
	 *        	<i>false</I> otherwise (and particularly if the given resource is NULL).
	 */
	public final boolean addResource(final String resourceId, final TAPResource newResource){
		if (newResource == null)
			return false;
		resources.put((resourceId == null) ? newResource.getName() : resourceId, newResource);
		return true;
	}

	/**
	 * Get the number of all resources managed by this TAP service (this resource - HOME - excluded).
	 * 
	 * @return	Number of managed resources.
	 */
	public final int getNbResources(){
		return resources.size();
	}

	/**
	 * <p>Get the specified resource.</p>
	 * 
	 * <p><i>Note:
	 * 	The research is case sensitive.
	 * </i></p>
	 * 
	 * @param resourceId	Exact ID/URI of the resource to get.
	 * 
	 * @return	The corresponding resource,
	 *        	or NULL if no match can be found.
	 */
	public final TAPResource getResource(final String resourceId){
		return resources.get(resourceId);
	}

	/**
	 * Let iterate over the full list of the TAP resources managed by this TAP service.
	 * 
	 * @return	Iterator over the available TAP resources.
	 */
	public final Iterator<TAPResource> getTAPResources(){
		return resources.values().iterator();
	}

	/**
	 * <p>Tell whether a resource is already associated with the given ID/URI.</p>
	 * 
	 * <p><i>Note:
	 * 	The research is case sensitive.
	 * </i></p>
	 * 
	 * @param resourceId	Exact ID/URI of the resource to find.
	 * 
	 * @return	<i>true</i> if a resource is already associated with the given ID/URI,
	 *        	<i>false</i> otherwise.
	 */
	public final boolean hasResource(final String resourceId){
		return resources.containsKey(resourceId);
	}

	/**
	 * <p>Remove the resource associated with the given ID/URI.</p>
	 * 
	 * <p><i>Note:
	 * 	The research is case sensitive.
	 * </i></p>
	 * 
	 * @param resourceId	Exact ID/URI of the resource to remove.
	 * 
	 * @return	The removed resource, if associated with the given ID/URI,
	 *        	otherwise, NULL is returned.
	 */
	public final TAPResource removeResource(final String resourceId){
		return resources.remove(resourceId);
	}

	/* **************** */
	/* ERROR MANAGEMENT */
	/* **************** */

	/**
	 * Get the object to use in order to report errors to the user in replacement of the expected result.
	 * 
	 * @return	Used error writer.
	 */
	public final ServiceErrorWriter getErrorWriter(){
		return errorWriter;
	}

	/**
	 * Set the object to use in order to report errors to the user in replacement of the expected result.
	 * 
	 * @param errorWriter	Error writer to use. (if NULL, nothing will be done)
	 */
	public final void setErrorWriter(final ServiceErrorWriter errorWriter){
		if (errorWriter != null){
			this.errorWriter = errorWriter;
			getUWS().setErrorWriter(errorWriter);
		}
	}

	@Override
	public String getStandardID(){
		return "ivo://ivoa.net/std/TAP";
	}

	@Override
	public String getAccessURL(){
		return tapBaseURL;
	}

	@Override
	public String getCapability(){
		StringBuffer xml = new StringBuffer();

		// Header:
		xml.append("<capability ").append(VOSerializer.formatAttribute("standardID", getStandardID())).append(" xsi:type=\"tr:TableAccess\">\n");

		// TAP access:
		xml.append("\t<interface role=\"std\" xsi:type=\"vs:ParamHTTP\">\n");
		xml.append("\t\t<accessURL use=\"base\">").append((getAccessURL() == null) ? "" : VOSerializer.formatText(getAccessURL())).append("</accessURL>\n");
		xml.append("\t</interface>\n");

		// Language description:
		xml.append("\t<language>\n");
		xml.append("\t\t<name>ADQL</name>\n");
		xml.append("\t\t<version ivo-id=\"ivo://ivoa.net/std/ADQL#v2.0\">2.0</version>\n");
		xml.append("\t\t<description>ADQL 2.0</description>\n");
		xml.append("\t</language>\n");

		// Available output formats:
		Iterator<OutputFormat> itFormats = service.getOutputFormats();
		OutputFormat formatter;
		while(itFormats.hasNext()){
			formatter = itFormats.next();
			xml.append("\t<outputFormat>\n");
			xml.append("\t\t<mime>").append(VOSerializer.formatText(formatter.getMimeType())).append("</mime>\n");
			if (formatter.getShortMimeType() != null)
				xml.append("\t\t<alias>").append(VOSerializer.formatText(formatter.getShortMimeType())).append("</alias>\n");
			if (formatter.getDescription() != null)
				xml.append("\t\t<description>").append(VOSerializer.formatText(formatter.getDescription())).append("</description>\n");
			xml.append("\t</outputFormat>\n");
		}

		// Retention period (for asynchronous jobs):
		int[] retentionPeriod = service.getRetentionPeriod();
		if (retentionPeriod != null && retentionPeriod.length >= 2){
			if (retentionPeriod[0] > -1 || retentionPeriod[1] > -1){
				xml.append("\t<retentionPeriod>\n");
				if (retentionPeriod[0] > -1)
					xml.append("\t\t<default>").append(retentionPeriod[0]).append("</default>\n");
				if (retentionPeriod[1] > -1)
					xml.append("\t\t<hard>").append(retentionPeriod[1]).append("</hard>\n");
				xml.append("\t</retentionPeriod>\n");
			}
		}

		// Execution duration (still for asynchronous jobs):
		int[] executionDuration = service.getExecutionDuration();
		if (executionDuration != null && executionDuration.length >= 2){
			if (executionDuration[0] > -1 || executionDuration[1] > -1){
				xml.append("\t<executionDuration>\n");
				if (executionDuration[0] > -1)
					xml.append("\t\t<default>").append(executionDuration[0]).append("</default>\n");
				if (executionDuration[1] > -1)
					xml.append("\t\t<hard>").append(executionDuration[1]).append("</hard>\n");
				xml.append("\t</executionDuration>\n");
			}
		}

		// Output/Result limit:
		int[] outputLimit = service.getOutputLimit();
		LimitUnit[] outputLimitType = service.getOutputLimitType();
		if (outputLimit != null && outputLimit.length >= 2 && outputLimitType != null && outputLimitType.length >= 2){
			if (outputLimit[0] > -1 || outputLimit[1] > -1){
				xml.append("\t<outputLimit>\n");
				if (outputLimit[0] > -1)
					xml.append("\t\t<default ").append(VOSerializer.formatAttribute("unit", outputLimitType[0].toString())).append(">").append(outputLimit[0]).append("</default>\n");
				if (outputLimit[1] > -1)
					xml.append("\t\t<hard ").append(VOSerializer.formatAttribute("unit", outputLimitType[1].toString())).append(">").append(outputLimit[1]).append("</hard>\n");
				xml.append("\t</outputLimit>\n");
			}
		}

		// Upload capabilities and limits:
		if (service.uploadEnabled()){
			// Write upload methods: INLINE, HTTP, FTP:
			xml.append("<uploadMethod ivo-id=\"ivo://ivoa.org/tap/uploadmethods#inline\" />");
			xml.append("<uploadMethod ivo-id=\"ivo://ivoa.org/tap/uploadmethods#http\" />");
			xml.append("<uploadMethod ivo-id=\"ivo://ivoa.org/tap/uploadmethods#ftp\" />");
			xml.append("<uploadMethod ivo-id=\"ivo://ivoa.net/std/TAPRegExt#upload-inline\" />");
			xml.append("<uploadMethod ivo-id=\"ivo://ivoa.net/std/TAPRegExt#upload-http\" />");
			xml.append("<uploadMethod ivo-id=\"ivo://ivoa.net/std/TAPRegExt#upload-ftp\" />");

			// Write upload limits:
			int[] uploadLimit = service.getUploadLimit();
			LimitUnit[] uploadLimitType = service.getUploadLimitType();
			if (uploadLimit != null && uploadLimit.length >= 2 && uploadLimitType != null && uploadLimitType.length >= 2){
				if (uploadLimit[0] > -1 || uploadLimit[1] > -1){
					xml.append("\t<uploadLimit>\n");
					if (uploadLimit[0] > -1)
						xml.append("\t\t<default ").append(VOSerializer.formatAttribute("unit", uploadLimitType[0].toString())).append(">").append(uploadLimit[0]).append("</default>\n");
					if (uploadLimit[1] > -1)
						xml.append("\t\t<hard ").append(VOSerializer.formatAttribute("unit", uploadLimitType[1].toString())).append(">").append(uploadLimit[1]).append("</hard>\n");
					xml.append("\t</uploadLimit>\n");
				}
			}
		}

		// Footer:
		xml.append("\t</capability>");

		return xml.toString();
	}

	/* ************************************* */
	/* MANAGEMENT OF THIS RESOURCE'S CONTENT */
	/* ************************************* */

	/**
	 * <p>Get the URL or the file path of a custom home page.</p>
	 * 
	 * <p>The home page will be displayed when this resource is directly requested.</p>
	 * 
	 * @return	URL or file path of the file to display as home page,
	 *        	or NULL if no custom home page has been specified.
	 */
	public final String getHomePageURI(){
		return homePageURI;
	}

	/**
	 * <p>Set the URL or the file path of a custom home page.</p>
	 * 
	 * <p>The home page will be displayed when this resource is directly requested.</p>
	 * 
	 * @param uri	URL or file path of the file to display as home page, or NULL to display the default home page.
	 */
	public final void setHomePageURI(final String uri){
		homePageURI = (uri != null) ? uri.trim() : uri;
		if (homePageURI != null && homePageURI.length() == 0)
			homePageURI = null;
	}

	/**
	 * <p>Get the MIME type of the custom home page.</p>
	 * 
	 * <p>
	 * 	By default, it is the same as the default home page: "text/html".
	 * </p>
	 * 
	 * @return	MIME type of the custom home page.
	 */
	public final String getHomePageMimeType(){
		return homePageMimeType;
	}

	/**
	 * <p>Set the MIME type of the custom home page.</p>
	 * 
	 * <p>A NULL value will be considered as "text/html".</p>
	 * 
	 * @param mime	MIME type of the custom home page.
	 */
	public final void setHomePageMimeType(final String mime){
		homePageMimeType = (mime == null || mime.trim().length() == 0) ? "text/html" : mime.trim();
	}

	/**
	 * <p>Generate a unique ID for the given request.</p>
	 * 
	 * <p>By default, a timestamp is returned.</p>
	 * 
	 * @param request	Request whose an ID is asked.
	 * 
	 * @return	The ID of the given request.
	 * 
	 * @since 2.0
	 */
	protected synchronized String generateRequestID(final HttpServletRequest request){
		String id;
		do{
			id = System.currentTimeMillis() + "";
		}while(lastRequestID != null && lastRequestID.startsWith(id));
		lastRequestID = id;
		return id;
	}

	/**
	 * <p>Execute the given request in the TAP service by forwarding it to the appropriate resource.</p>
	 * 
	 * <h3>Home page</h3>
	 * <p>
	 * 	If the appropriate resource is the home page, the request is not propagated and
	 * 	this class/resource displays directly the home page in the given response by calling {@link #writeHomePage(HttpServletResponse, JobOwner)}.
	 * 	The default implementation of this function takes 2 cases into account:
	 * </p>
	 * <ol>
	 * 	<li><b>A custom home page has been specified</b> using {@link #setHomePageURI(String)}. In this case, the content of the URL or file path will
	 * 	                                                 be directly copied into the HTTP response. The content type of the response must be specified by
	 * 	                                                 {@link #setHomePageMimeType(String)} ; by default, it is "text/html".</li>
	 * 	<li><b>Default home page.</b> When no custom home page has been specified, a default content is displayed. It is an HTML document which merely
	 * 	                              lists all resources available in this TAP service.</li>
	 * </ol>
	 * 
	 * <h3>Error/Exception management</h3>
	 * <p>
	 * 	Only this resource (the root) should write any errors in the response. For that, it catches any {@link Throwable} and
	 * 	write an appropriate message in the HTTP response. The format and the content of this message is designed by the {@link ServiceErrorWriter}
	 * 	set in this class. By changing it, it is then possible to change, for instance, the format of the error responses.
	 * </p>
	 * 
	 * <h3>Request ID &amp; Log</h3>
	 * <p>
	 * 	Each request is identified by a unique identifier (see {@link #generateRequestID(HttpServletRequest)}).
	 * 	This ID is used only for logging purpose. Request and jobs/threads can then be associated more easily in the logs.
	 * 	Besides, every requests and their response are logged as INFO with this ID.
	 * </p>
	 * 
	 * @param request	Request of the user to execute in this TAP service.
	 * @param response	Object in which the result of the request must be written.
	 * 
	 * @throws ServletException	If any grave/fatal error occurs.
	 * @throws IOException		If any error occurs while reading or writing from or into a stream (and particularly the given request or response).
	 */
	public void executeRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException{
		if (request == null || response == null)
			return;

		// Generate a unique ID for this request execution (for log purpose only):
		final String reqID = generateRequestID(request);

		// Retrieve the resource path parts:
		String[] resourcePath = (request.getPathInfo() == null) ? null : request.getPathInfo().split("/");
		final String resourceName = (resourcePath == null || resourcePath.length < 1) ? "homePage" : resourcePath[1].trim().toLowerCase();

		// Log the reception of the request, only if the asked resource is not UWS (because UWS is already logging the received request):
		if (!resourceName.equalsIgnoreCase(ASync.RESOURCE_NAME))
			getLogger().logHttp(LogLevel.INFO, request, reqID, null, null);

		// Initialize the base URL of this TAP service by guessing it from the received request:
		if (tapBaseURL == null){
			// initialize the base URL:
			setTAPBaseURL(request);
			// log the successful initialization:
			getLogger().logUWS(LogLevel.INFO, this, "INIT", "TAP successfully initialized.", null);
		}

		JobOwner owner = null;
		try{
			// Identify the user:
			try{
				if (service.getUserIdentifier() != null)
					owner = service.getUserIdentifier().extractUserId(new UWSUrl(request), request);
			}catch(UWSException ue){
				throw new TAPException(ue);
			}

			// Display the TAP Main Page:
			if (resourceName.equals("homePage"))
				writeHomePage(response, owner);
			// or Display/Execute the selected TAP Resource:
			else{
				// search for the corresponding resource:
				TAPResource res = resources.get(resourceName);
				// if one is found, execute it:
				if (res != null)
					res.executeResource(request, response);
				// otherwise, throw an error:
				else
					throw new TAPException("Unknown TAP resource: \"" + resourceName + "\"!", UWSException.NOT_IMPLEMENTED);
			}

			response.flushBuffer();

			// Log the successful execution of the action, only if the asked resource is not UWS (because UWS is already logging the received request):
			if (!resourceName.equalsIgnoreCase(ASync.RESOURCE_NAME))
				getLogger().logHttp(LogLevel.INFO, response, reqID, owner, "HTTP " + UWSException.OK + " - Action \"" + resourceName + "\" successfully executed.", null);

		}catch(Throwable t){
			// Write the error in the response and return the appropriate HTTP status code:
			errorWriter.writeError(t, response, request, reqID, owner, resourceName);
			// Log the error:
			getLogger().logHttp(LogLevel.ERROR, response, reqID, owner, "HTTP " + response.getStatus() + " - Can not complete the execution of the TAP resource \"" + resourceName + "\"!", t);
		}finally{
			// Notify the queue of the asynchronous jobs that a new connection is available:
			if (resourceName.equalsIgnoreCase(Sync.RESOURCE_NAME) && service.getFactory().countFreeConnections() >= 1)
				getASync().freeConnectionAvailable();
		}
	}

	/**
	 * <p>Write the content of the home page in the given writer.</p>
	 * 
	 * <p>This content can be:</p>
	 * <ul>
	 * 	<li><b>a distance document</b> if a URL has been provided to this class using {@link #setHomePageURI(String)}.
	 * 	                               In this case, the content of the distant document is copied in the given writer.
	 * 	                               No redirection is done.</li>
	 * 	<li><b>a local file</b> if a file path has been provided to this class using {@link #setHomePageURI(String)}.
	 * 	                        In this case, the content of the local file is copied in the given writer.</li>
	 * 	<li><b>a default content</b> if no custom home page has been specified using {@link #setHomePageURI(String)}.
	 * 	                             This default home page is hard-coded in this function and displays just an HTML list of
	 * 	                             links. There is one link for each resources of this TAP service.</li>
	 * </ul>
	 * 
	 * @param response	{@link HttpServletResponse} in which the home page must be written.
	 * @param owner		The identified user who asked this home page.
	 * 
	 * @throws IOException	If any error occurs while writing the home page in the given HTTP response.
	 */
	public void writeHomePage(final HttpServletResponse response, final JobOwner owner) throws IOException{
		PrintWriter writer = response.getWriter();

		// By default, list all available resources:
		if (homePageURI == null){
			// Set the content type: HTML document
			response.setContentType("text/html");

			// Write the home page:
			writer.println("<html><head><title>TAP HOME PAGE</title></head><body><h1 style=\"text-align: center\">TAP HOME PAGE</h1><h2>Available resources:</h2><ul>");
			for(TAPResource res : resources.values())
				writer.println("<li><a href=\"" + tapBaseURL + "/" + res.getName() + "\">" + res.getName() + "</a></li>");
			writer.println("</ul></body></html>");
		}
		// or Display the specified home page:
		else{
			response.setContentType(homePageMimeType);

			// Get an input toward the custom home page:
			BufferedInputStream input = null;
			try{
				// CASE: URL => distant document
				input = new BufferedInputStream((new URL(homePageURI)).openStream());
			}catch(MalformedURLException mue){
				// CASE: file path => local file
				input = new BufferedInputStream(new FileInputStream(new File(homePageURI)));
			}

			// Copy the content of the input into the given writer:
			byte[] buffer = new byte[255];
			int nbReads = 0;
			while((nbReads = input.read(buffer)) > 0)
				writer.print(new String(buffer, 0, nbReads));
		}
	}

}
