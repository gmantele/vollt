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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import adql.db.DBColumn;
import adql.db.FunctionDef;
import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.error.DefaultTAPErrorWriter;
import tap.formatter.OutputFormat;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import uk.ac.starlink.votable.VOSerializer;
import uws.UWSException;
import uws.UWSToolBox;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSService;
import uws.service.error.ServiceErrorWriter;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Root/Home of the TAP service. It is also the resource (HOME) which gathers all the others of the same TAP service.</p>
 *
 * <p>At its creation it is creating and configuring the other resources in function of the given description of the TAP service.</p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.1 (09/2017)
 */
public class TAP implements VOSIResource {

	/** Version of the TAP protocol used in this library.
	 * @since 2.1 */
	public final static String VERSION = "1.0";

	/** <p>Name of the TAP AVAILABILITY resource.
	 * This resource tells whether the TAP service is available (i.e. whether it accepts queries or not).</p>
	 * <p><i>Note: this name is suffixing the root TAP URL in order to access one of its resources.</i></p>
	 * @since 2.0 */
	public final static String RESOURCE_AVAILABILITY = "availability";
	/** <p>Name of the TAP CAPABILITIES resource.
	 * This resource list all capabilities (e.g. output limits and formats, uploads, ...) of this TAP resource.</p>
	 * <p><i>Note: this name is suffixing the root TAP URL in order to access one of its resources.</i></p>
	 * @since 2.0 */
	public final static String RESOURCE_CAPABILITIES = "capabilities";
	/** <p>Name of the TAP HOME PAGE resource.
	 * This resource lists and describes all published and query-able schemas, tables and columns.</p>
	 * <p><i>Note: this name is suffixing the root TAP URL in order to access one of its resources.</i></p>
	 * @since 2.0 */
	public final static String RESOURCE_METADATA = "tables";
	/** <p>Name of the TAP HOME PAGE resource.
	 * This resource is used to submit ADQL queries to run asynchronously.</p>
	 * <p><i>Note: this name is suffixing the root TAP URL in order to access one of its resources.</i></p>
	 * @since 2.0 */
	public final static String RESOURCE_ASYNC = "async";
	/** <p>Name of the TAP HOME PAGE resource.
	 * This resource is used to submit ADQL queries to run synchronously.</p>
	 * <p><i>Note: this name is suffixing the root TAP URL in order to access one of its resources.</i></p>
	 * @since 2.0 */
	public final static String RESOURCE_SYNC = "sync";

	/** Description of the TAP service owning this resource. */
	protected final ServiceConnection service;

	/** List of all the other TAP resources of the service. */
	protected final Map<String,TAPResource> resources;

	/** Base URL of the TAP service. It is also the URL of this resource (HOME). */
	protected String tapBaseURL = null;

	/**
	 * <p>HOME PAGE resource.
	 * This resource lets write the home page.</p>
	 * <p><i>Note:
	 * 	at the URI {@link #homePageURI} or it is a very simple HTML page listing the link of all available
	 * 	TAP resources.
	 * </i></p>
	 * @since 2.0
	 */
	protected HomePage homePage = null;

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
	public TAP(final ServiceConnection serviceConnection) throws UWSException, TAPException{
		service = serviceConnection;
		resources = new HashMap<String,TAPResource>();

		// Get the error writer to use, or create a default instance if none are provided by the factory:
		errorWriter = serviceConnection.getFactory().getErrorWriter();
		if (errorWriter == null)
			errorWriter = new DefaultTAPErrorWriter(service);

		// Set the default home page:
		homePage = new HomePage(this);

		// Set all the standard TAP resources:
		TAPResource res = new Availability(service);
		resources.put(res.getName(), res);

		res = new Capabilities(this);
		resources.put(res.getName(), res);

		res = new Sync(service, (Capabilities)res);
		resources.put(res.getName(), res);

		res = new ASync(service);
		resources.put(res.getName(), res);

		TAPMetadata metadata = service.getTAPMetadata();
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
		// Set the availability to "false" and the reason to "The application server is stopping!":
		service.setAvailable(false, "The application server is stopping!");

		// Destroy all web resources:
		for(TAPResource res : resources.values())
			res.destroy();

		// Destroy also all resources allocated in the factory:
		service.getFactory().destroy();

		// Log the end:
		getLogger().logTAP(LogLevel.INFO, this, "STOP", "TAP Service stopped!", null);
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
	 * Get the description of this service.
	 *
	 * @return	Description/Configuration of this TAP service.
	 *
	 * @since 2.0
	 */
	public final ServiceConnection getServiceConnection(){
		return service;
	}

	/**
	 * Get the /availability resource of this TAP service.
	 *
	 * @return	The /availability resource.
	 */
	public final Availability getAvailability(){
		return (Availability)resources.get(RESOURCE_AVAILABILITY);
	}

	/**
	 * Get the /capabilities resource of this TAP service.
	 *
	 * @return	The /capabilities resource.
	 */
	public final Capabilities getCapabilities(){
		return (Capabilities)resources.get(RESOURCE_CAPABILITIES);
	}

	/**
	 * Get the /sync resource of this TAP service.
	 *
	 * @return	The /sync resource.
	 */
	public final Sync getSync(){
		return (Sync)resources.get(RESOURCE_SYNC);
	}

	/**
	 * Get the /async resource of this TAP service.
	 *
	 * @return	The /async resource.
	 */
	public final ASync getASync(){
		return (ASync)resources.get(RESOURCE_ASYNC);
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
	public final Iterator<TAPResource> getResources(){
		return resources.values().iterator();
	}

	/**
	 * Let iterate over the full list of the TAP resources managed by this TAP service.
	 *
	 * @return	Iterator over the available TAP resources.
	 * @deprecated	The name of this function has been normalized. So now, you should use {@link #getResources()}
	 *            	which is doing exactly the same thing.
	 */
	@Deprecated
	public final Iterator<TAPResource> getTAPResources(){
		return getResources();
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

		// Data models:
		appendDataModels(xml, "\t");

		// Language description:
		xml.append("\t<language>\n");
		xml.append("\t\t<name>ADQL</name>\n");
		xml.append("\t\t<version ivo-id=\"ivo://ivoa.net/std/ADQL#v2.0\">2.0</version>\n");
		xml.append("\t\t<description>ADQL 2.0</description>\n");

		// Geometrical functions:
		if (service.getGeometries() != null && service.getGeometries().size() > 0){
			xml.append("\t\t<languageFeatures type=\"ivo://ivoa.net/std/TAPRegExt#features-adqlgeo\">");
			for(String geom : service.getGeometries()){
				if (geom != null){
					xml.append("\t\t\t<feature>");
					xml.append("\t\t\t\t<form>").append(VOSerializer.formatText(geom.toUpperCase())).append("</form>");
					xml.append("\t\t\t</feature>");
				}
			}
			xml.append("\t\t</languageFeatures>");
		}

		// User Defined Functions (UDFs):
		if (service.getUDFs() != null && service.getUDFs().size() > 0){
			xml.append("\t\t<languageFeatures type=\"ivo://ivoa.net/std/TAPRegExt#features-udf\">");
			for(FunctionDef udf : service.getUDFs()){
				if (udf != null){
					xml.append("\t\t\t<feature>");
					xml.append("\t\t\t\t<form>").append(VOSerializer.formatText(udf.toString())).append("</form>");
					if (udf.description != null && udf.description.length() > 0)
						xml.append("\t\t\t\t<description>").append(VOSerializer.formatText(udf.description)).append("</description>");
					xml.append("\t\t\t</feature>");
				}
			}
			xml.append("\t\t</languageFeatures>");
		}

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

		// Write upload methods: INLINE, HTTP, FTP:
		if (service.uploadEnabled()){
			xml.append("\t<uploadMethod ivo-id=\"ivo://ivoa.net/std/TAPRegExt#upload-inline\" />\n");
			xml.append("\t<uploadMethod ivo-id=\"ivo://ivoa.net/std/TAPRegExt#upload-http\" />\n");
			xml.append("\t<uploadMethod ivo-id=\"ivo://ivoa.net/std/TAPRegExt#upload-ftp\" />\n");
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
					xml.append("\t\t<default>").append(executionDuration[0] / 1000).append("</default>\n");
				if (executionDuration[1] > -1)
					xml.append("\t\t<hard>").append(executionDuration[1] / 1000).append("</hard>\n");
				xml.append("\t</executionDuration>\n");
			}
		}

		// Output/Result limit:
		int[] outputLimit = service.getOutputLimit();
		LimitUnit[] outputLimitType = service.getOutputLimitType();
		if (outputLimit != null && outputLimit.length >= 2 && outputLimitType != null && outputLimitType.length >= 2){
			if (outputLimit[0] > -1 || outputLimit[1] > -1){
				xml.append("\t<outputLimit>\n");
				String limitType;
				if (outputLimit[0] > -1){
					long limit = outputLimit[0] * outputLimitType[0].bytesFactor();
					limitType = (outputLimitType[0] == null || outputLimitType[0] == LimitUnit.rows) ? LimitUnit.rows.toString() : LimitUnit.bytes.toString();
					xml.append("\t\t<default ").append(VOSerializer.formatAttribute("unit", limitType)).append(">").append(limit).append("</default>\n");
				}
				if (outputLimit[1] > -1){
					long limit = outputLimit[1] * outputLimitType[1].bytesFactor();
					limitType = (outputLimitType[1] == null || outputLimitType[1] == LimitUnit.rows) ? LimitUnit.rows.toString() : LimitUnit.bytes.toString();
					xml.append("\t\t<hard ").append(VOSerializer.formatAttribute("unit", limitType)).append(">").append(limit).append("</hard>\n");
				}
				xml.append("\t</outputLimit>\n");
			}
		}

		// Upload limits
		if (service.uploadEnabled()){
			// Write upload limits:
			int[] uploadLimit = service.getUploadLimit();
			LimitUnit[] uploadLimitType = service.getUploadLimitType();
			if (uploadLimit != null && uploadLimit.length >= 2 && uploadLimitType != null && uploadLimitType.length >= 2){
				if (uploadLimit[0] > -1 || uploadLimit[1] > -1){
					xml.append("\t<uploadLimit>\n");
					String limitType;
					if (uploadLimit[0] > -1){
						long limit = uploadLimit[0] * uploadLimitType[0].bytesFactor();
						limitType = (uploadLimitType[0] == null || uploadLimitType[0] == LimitUnit.rows) ? LimitUnit.rows.toString() : LimitUnit.bytes.toString();
						xml.append("\t\t<default ").append(VOSerializer.formatAttribute("unit", limitType)).append(">").append(limit).append("</default>\n");
					}
					if (uploadLimit[1] > -1){
						long limit = uploadLimit[1] * uploadLimitType[1].bytesFactor();
						limitType = (uploadLimitType[1] == null || uploadLimitType[1] == LimitUnit.rows) ? LimitUnit.rows.toString() : LimitUnit.bytes.toString();
						xml.append("\t\t<hard ").append(VOSerializer.formatAttribute("unit", limitType)).append(">").append(limit).append("</hard>\n");
					}
					xml.append("\t</uploadLimit>\n");
				}
			}
		}

		// Footer:
		xml.append("\t</capability>");

		return xml.toString();
	}

	/**
	 * List and declare all IVOA Data Models supported by this TAP service.
	 *
	 * <p>Currently, only the following DMs are natively supported:</p>
	 * <ul>
	 * 	<li>Obscore (1.0 and PR-1.1)</li>
	 * 	<li>RegTAP (1.0)</li>
	 * </ul>
	 *
	 * <p>
	 * 	More can be supported by extending this function
	 * 	(but not overwriting it completely otherwise the above
	 * 	supported DMs won't be anymore).
	 * </p>
	 *
	 * <p>A DM declaration should follow this XML syntax:</p>
	 * <pre>&lt;dataModel ivo-id="{DM-IVO_ID}"&gt;{DM-NAME}&lt;/dataModel&gt;</pre>
	 *
	 * @param xml			The <code>/capabilities</code> in-progress content in which
	 *           			implemented DMs can be declared.
	 * @param linePrefix	Tabulations/Spaces that should prefix all lines
	 *                  	(for human readability).
	 *
	 * @since 2.1
	 */
	protected void appendDataModels(final StringBuffer xml, final String linePrefix){
		// ObsCore:
		appendObsCoreDM(xml, linePrefix);
		// RegTAP:
		appendRegTAPDM(xml, linePrefix);
	}

	/**
	 * <p>Append the ObsCore DM declaration in the given {@link StringBuffer}
	 * if an <code>ivoa.Obscore</code> table can be found in <code>TAP_SCHEMA</code>.</p>
	 *
	 * <p>
	 * 	This function has no effect if <code>ivoa.Obscore</code> can not
	 * 	be found. The <code>ivoa</code> schema is searched case sensitively,
	 * 	but not the table name <code>Obscore</code> which can be written
	 * 	in any possible case.
	 * </p>
	 *
	 * <p>
	 * 	If an <code>ivoa.Obscore</code> table is found, this function
	 * 	detects automatically which version of Obscore is implemented.
	 * 	It will be declared as Obscore 1.1 if ALL the following columns
	 * 	are found (case INsensitively): <code>s_xel1</code>, <code>x_xel2</code>,
	 * 	<code>t_xel</code>, <code>em_xel</code> and <code>pol_xel</code>.
	 * 	If not, the Obscore table will be declared as Obscore 1.0.
	 * </p>
	 *
	 * @param xml			The <code>/capabilities</code> in-progress content
	 *           			in which Obscore-DM should be declared if found.
	 * @param linePrefix	Tabulations/Spaces that should prefix all lines
	 *                  	(for human readability).
	 *
	 * @see TAPMetadata#getObsCoreTable()
	 *
	 * @since 2.1
	 */
	protected void appendObsCoreDM(final StringBuffer xml, final String linePrefix){
		// Try to get the ObsCore table definition:
		TAPTable obscore = service.getTAPMetadata().getObsCoreTable();

		// If there is one, determine the supported DM version and declare it:
		if (obscore != null){
			/* ObsCore 1.1 MUST have s_xel1, s_xel2, t_xel, em_xel and pol_xel
			 * These columns do not exist in ObsCore 1.0. */
			byte hasAllXel = 0x0;
			for(DBColumn col : obscore){
				if (col.getADQLName().equalsIgnoreCase("s_xel1"))
					hasAllXel |= 1;  // 2^0 = 0000 0001
				else if (col.getADQLName().equalsIgnoreCase("s_xel2"))
					hasAllXel |= 2;  // 2^1 = 0000 0010
				else if (col.getADQLName().equalsIgnoreCase("t_xel"))
					hasAllXel |= 4;  // 2^2 = 0000 0100
				else if (col.getADQLName().equalsIgnoreCase("em_xel"))
					hasAllXel |= 8;  // 2^3 = 0000 1000
				else if (col.getADQLName().equalsIgnoreCase("pol_xel"))
					hasAllXel |= 16; // 2^4 = 0001 0000
			}
			// Finally add the appropriate DM declaration:
			if (hasAllXel == 31) // 2^5 - 1 =  0001 1111
				xml.append(linePrefix + "<dataModel ivo-id=\"ivo://ivoa.net/std/ObsCore#core-1.1\">ObsCore-1.1</dataModel>\n");
			else
				xml.append(linePrefix + "<dataModel ivo-id=\"ivo://ivoa.net/std/ObsCore/v1.0\">ObsCore-1.0</dataModel>\n");
		}
	}

	/**
	 * <p>Append the RegTAP DM declaration in the given {@link StringBuffer}
	 * if a schema <code>rr</code> can be found in <code>TAP_SCHEMA</code>
	 * with all its required tables.</p>
	 *
	 * <p>
	 * 	This function has no effect if the schema <code>rr</code> or its
	 * 	mandatory children tables can not be found. The research is done
	 * 	case sensitively by {@link TAPMetadata#getRegTAPSchema()}.
	 * </p>
	 *
	 * <p>
	 * 	If there is a valid schema <code>rr</code>, this function
	 * 	detects automatically which version of RegTAP is implemented. For the
	 * 	moment only one is supported: RegTAP-1.0.
	 * </p>
	 *
	 * @param xml			The <code>/capabilities</code> in-progress content
	 *           			in which RegTAP-DM should be declared if found.
	 * @param linePrefix	Tabulations/Spaces that should prefix all lines
	 *                  	(for human readability).
	 *
	 * @see TAPMetadata#getRegTAPTable()
	 *
	 * @since 2.1
	 */
	protected void appendRegTAPDM(final StringBuffer xml, final String linePrefix){
		// Try to get the RegTAP schema definition:
		TAPSchema regtap = service.getTAPMetadata().getRegTAPSchema();

		// If there is one, determine the supported DM version and declare it:
		if (regtap != null)
			xml.append(linePrefix + "<dataModel ivo-id=\"ivo://ivoa.net/std/RegTAP#1.0\">Registry 1.0</dataModel>\n");
	}

	/* ************************************* */
	/* MANAGEMENT OF THIS RESOURCE'S CONTENT */
	/* ************************************* */

	/**
	 * Get the HOME PAGE resource of this TAP service.
	 *
	 * @return	The HOME PAGE resource.
	 *
	 * @since 2.0
	 */
	public final HomePage getHomePage(){
		return homePage;
	}

	/**
	 * <p>Change the whole behavior of the TAP home page.</p>
	 *
	 * <p><i>Note:
	 * 	If the given resource is NULL, the default home page (i.e. {@link HomePage}) is set.
	 * </i></p>
	 *
	 * @param newHomePageResource	The new HOME PAGE resource for this TAP service.
	 *
	 * @since 2.0
	 */
	public final void setHomePage(final HomePage newHomePageResource){
		if (newHomePageResource == null){
			if (homePage == null || !(homePage instanceof HomePage))
				homePage = new HomePage(this);
		}else
			homePage = newHomePageResource;
	}

	/**
	 * <p>Get the URL or the file path of a custom home page.</p>
	 *
	 * <p>The home page will be displayed when this resource is directly requested.</p>
	 *
	 * <p><i>Note:
	 * 	This function has a sense only if the HOME PAGE resource of this TAP service
	 * 	is still the default home page (i.e. {@link HomePage}).
	 * </i></p>
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
	 * <p><i>Note:
	 * 	This function has a sense only if the HOME PAGE resource of this TAP service
	 * 	is still the default home page (i.e. {@link HomePage}).
	 * </i></p>
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
	 * <p>By default, it is the same as the default home page: "text/html".</p>
	 *
	 * <p><i>Note:
	 * 	This function has a sense only if the HOME PAGE resource of this TAP service
	 * 	is still the default home page (i.e. {@link HomePage}).
	 * </i></p>
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
	 * <p><i>Note:
	 * 	This function has a sense only if the HOME PAGE resource of this TAP service
	 * 	is still the default home page (i.e. {@link HomePage}).
	 * </i></p>
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
		String prefix = (request != null && request.getRequestURI() != null && request.getRequestURI().contains("/sync") ? "S" : "");
		do{
			id = prefix + System.currentTimeMillis() + "";
		}while(lastRequestID != null && lastRequestID.startsWith(id));
		lastRequestID = id;
		return id;
	}

	/**
	 * <p>Execute the given request in the TAP service by forwarding it to the appropriate resource.</p>
	 *
	 * <h3>Home page</h3>
	 * <p>
	 * 	If the appropriate resource is the home page, the request is propagated to a {@link TAPResource}
	 * 	(by default {@link HomePage}) whose the resource name is "HOME PAGE". Once called, this resource
	 * 	displays directly the home page in the given response by calling.
	 * 	The default implementation of the default implementation ({@link HomePage}) takes several cases into account.
	 * 	Those are well documented in the Javadoc of {@link HomePage}. What you should know, is that sometimes it is
	 * 	using the following attributes of this class: {@link #getHomePage()}, {@link #getHomePageURI()}, {@link #getHomePageMimeType()}.
	 * </p>
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
		if (request.getAttribute(UWS.REQ_ATTRIBUTE_ID) == null)
			request.setAttribute(UWS.REQ_ATTRIBUTE_ID, reqID);

		// Extract all parameters:
		if (request.getAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS) == null){
			try{
				request.setAttribute(UWS.REQ_ATTRIBUTE_PARAMETERS, getUWS().getRequestParser().parse(request));
			}catch(UWSException ue){
				getLogger().log(LogLevel.WARNING, "REQUEST_PARSER", "Can not extract the HTTP request parameters!", ue);
			}
		}

		// Retrieve the resource path parts:
		String[] resourcePath = (request.getPathInfo() == null) ? null : request.getPathInfo().split("/");
		String resourceName = (resourcePath == null || resourcePath.length < 1) ? "" : resourcePath[1].trim();

		// Log the reception of the request, only if the asked resource is not UWS (because UWS is already logging the received request):
		if (!resourceName.equalsIgnoreCase(ASync.RESOURCE_NAME))
			getLogger().logHttp(LogLevel.INFO, request, reqID, null, null);

		// Initialize the base URL of this TAP service by guessing it from the received request:
		if (tapBaseURL == null){
			// initialize the base URL:
			setTAPBaseURL(request);
			// log the successful initialization:
			getLogger().logUWS(LogLevel.INFO, this, "INIT", "TAP successfully initialized (" + tapBaseURL + ").", null);
		}

		JobOwner user = null;
		try{
			// Identify the user:
			try{
				user = UWSToolBox.getUser(request, service.getUserIdentifier());
			}catch(UWSException ue){
				getLogger().logTAP(LogLevel.ERROR, null, "IDENT_USER", "Can not identify the HTTP request user!", ue);
				throw new TAPException(ue);
			}

			// Set the character encoding:
			response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

			// Display the TAP Home Page:
			if (resourceName.length() == 0){
				resourceName = homePage.getName();
				homePage.executeResource(request, response);
			}
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
				getLogger().logHttp(LogLevel.INFO, response, reqID, user, "Action \"" + resourceName + "\" successfully executed.", null);

		}catch(IOException ioe){
			/*
			 *   Any IOException thrown while writing the HTTP response is generally caused by a client abortion (intentional or timeout)
			 * or by a connection closed with the client for another reason.
			 *   Consequently, a such error should not be considered as a real error from the server or the library: the request is
			 * canceled, and so the response is not expected. It is anyway not possible any more to send it (header and/or body) totally
			 * or partially.
			 *   Nothing can solve this error. So the "error" is just reported as a simple information and theoretically the action
			 * executed when this error has been thrown is already stopped.
			 */
			getLogger().logHttp(LogLevel.INFO, response, reqID, user, "HTTP request aborted or connection with the client closed => the TAP resource \"" + resourceName + "\" has stopped and the body of the HTTP response can not have been partially or completely written!", null);

		}catch(TAPException te){
			/*
			 *   Any known/"expected" TAP exception is logged but also returned to the HTTP client in an XML error document.
			 *   Since the error is known, it is supposed to have already been logged with a full stack trace. Thus, there
			 * is no need to log again its stack trace...just its message is logged.
			 */
			// Write the error in the response and return the appropriate HTTP status code:
			errorWriter.writeError(te, response, request, reqID, user, resourceName);
			// Log the error:
			getLogger().logHttp(LogLevel.ERROR, response, reqID, user, "TAP resource \"" + resourceName + "\" execution FAILED with the error: \"" + te.getMessage() + "\"!", null);

		}catch(IllegalStateException ise){
			/*
			 *   Any IllegalStateException that reaches this point, is supposed coming from a HttpServletResponse operation which
			 * has to reset the response buffer (e.g. resetBuffer(), sendRedirect(), sendError()).
			 *   If this exception happens, the library tried to rewrite the HTTP response body with a message or a result,
			 * while this body has already been partially sent to the client. It is then no longer possible to change its content.
			 *   Consequently, the error is logged as FATAL and a message will be appended at the end of the already submitted response
			 * to alert the HTTP client that an error occurs and the response should not be considered as complete and reliable.
			 */
			// Write the error in the response and return the appropriate HTTP status code:
			errorWriter.writeError(ise, response, request, reqID, user, resourceName);
			// Log the error:
			getLogger().logHttp(LogLevel.FATAL, response, reqID, user, "HTTP response already partially committed => the TAP resource \"" + resourceName + "\" has stopped and the body of the HTTP response can not have been partially or completely written!", (ise.getCause() != null) ? ise.getCause() : ise);

		}catch(Throwable t){
			/*
			 *   Any other error is considered as unexpected if it reaches this point. Consequently, it has not yet been logged.
			 * So its stack trace will be fully logged, and an appropriate message will be returned to the HTTP client. The
			 * returned XML document should contain not too technical information which would be useless for the user.
			 */
			// Write the error in the response and return the appropriate HTTP status code:
			errorWriter.writeError(t, response, request, reqID, user, resourceName);
			// Log the error:
			getLogger().logHttp(LogLevel.FATAL, response, reqID, user, "TAP resource \"" + resourceName + "\" execution FAILED with a GRAVE error!", t);

		}finally{
			// Notify the queue of the asynchronous jobs that a new connection may be available:
			if (resourceName.equalsIgnoreCase(Sync.RESOURCE_NAME))
				getASync().freeConnectionAvailable();
		}
	}

}
