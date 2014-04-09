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
 *                       Astronomisches Rechen Institute (ARI)
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import tap.db.DBConnection;
import tap.error.DefaultTAPErrorWriter;
import tap.formatter.OutputFormat;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.UWSException;
import uws.job.ErrorType;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.error.ServiceErrorWriter;

/**
 * @author Gr&eacute;gory Mantelet (CDS;ARI) - gmantele@ari.uni-heidelberg.de
 * @version 1.1 (12/2013)
 * 
 * @param <R>
 */
public class TAP< R > implements VOSIResource {

	private static final long serialVersionUID = 1L;

	protected final ServiceConnection<R> service;

	protected final Map<String,TAPResource> resources;

	protected String tapBaseURL = null;

	protected String homePageURI = null;

	protected ServiceErrorWriter errorWriter;

	public TAP(ServiceConnection<R> serviceConnection) throws UWSException, TAPException{
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
		getUWS().setErrorWriter(errorWriter);

		if (service.uploadEnabled()){
			DBConnection<?> dbConn = null;
			try{
				dbConn = service.getFactory().createDBConnection("TAP(ServiceConnection)");
				dbConn.dropSchema("TAP_UPLOAD");
				dbConn.createSchema("TAP_UPLOAD");
			}catch(TAPException e){
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e, "Error while creating the schema TAP_UPLOAD !");
			}finally{
				if (dbConn != null)
					dbConn.close();
			}
		}

		updateTAPMetadata();
	}

	public final TAPLog getLogger(){
		return service.getLogger();
	}

	public void setTAPBaseURL(String baseURL){
		tapBaseURL = baseURL;
		for(TAPResource res : resources.values())
			res.setTAPBaseURL(tapBaseURL);
	}

	public void setTAPBaseURL(HttpServletRequest request){
		setTAPBaseURL(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + request.getServletPath());
	}

	public final Availability getAvailability(){
		return (Availability)resources.get(Availability.RESOURCE_NAME);
	}

	public final Capabilities getCapabilities(){
		return (Capabilities)resources.get(Capabilities.RESOURCE_NAME);
	}

	public final Sync getSync(){
		return (Sync)resources.get(Sync.RESOURCE_NAME);
	}

	public final ASync getASync(){
		return (ASync)resources.get(ASync.RESOURCE_NAME);
	}

	public final TAPMetadata getTAPMetadata(){
		return (TAPMetadata)resources.get(TAPMetadata.RESOURCE_NAME);
	}

	public final Iterator<TAPResource> getTAPResources(){
		return resources.values().iterator();
	}

	public final ServiceErrorWriter getErrorWriter(){
		return errorWriter;
	}

	public final void setErrorWriter(ServiceErrorWriter errorWriter){
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

		xml.append("<capability standardID=\"").append(getStandardID()).append("\" xsi:type=\"tr:TableAccess\">\n");
		xml.append("\t<interface role=\"std\" xsi:type=\"vs:ParamHTTP\">\n");
		xml.append("\t\t<accessURL use=\"base\">").append(getAccessURL()).append("</accessURL>\n");
		xml.append("\t</interface>\n");
		xml.append("\t<language>\n");
		xml.append("\t\t<name>ADQL</name>\n");
		xml.append("\t\t<version>2.0</version>\n");
		xml.append("\t\t<description>ADQL 2.0</description>\n");
		xml.append("\t</language>\n");

		Iterator<OutputFormat<R>> itFormats = service.getOutputFormats();
		OutputFormat<R> formatter;
		while(itFormats.hasNext()){
			formatter = itFormats.next();
			xml.append("\t<outputFormat>\n");
			xml.append("\t\t<mime>").append(formatter.getMimeType()).append("</mime>\n");
			if (formatter.getShortMimeType() != null)
				xml.append("\t\t<alias>").append(formatter.getShortMimeType()).append("</alias>\n");
			if (formatter.getDescription() != null)
				xml.append("\t\t<description>").append(formatter.getDescription()).append("</description>\n");
			xml.append("\t</outputFormat>\n");
		}

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

		int[] outputLimit = service.getOutputLimit();
		LimitUnit[] outputLimitType = service.getOutputLimitType();
		if (outputLimit != null && outputLimit.length >= 2 && outputLimitType != null && outputLimitType.length >= 2){
			if (outputLimit[0] > -1 || outputLimit[1] > -1){
				xml.append("\t<outputLimit>\n");
				if (outputLimit[0] > -1)
					xml.append("\t\t<default unit=\"").append(outputLimitType[0]).append("\">").append(outputLimit[0]).append("</default>\n");
				if (outputLimit[1] > -1)
					xml.append("\t\t<hard unit=\"").append(outputLimitType[1]).append("\">").append(outputLimit[1]).append("</hard>\n");
				xml.append("\t</outputLimit>\n");
			}
		}

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
					if (uploadLimit[0] > -1){
						String limitType;
						long limit = uploadLimit[0];
						switch(uploadLimitType[0]){
							case kilobytes:
								limit *= 1000l;
								limitType = LimitUnit.rows.toString();
								break;
							case megabytes:
								limit *= 1000000l;
								limitType = LimitUnit.rows.toString();
								break;
							case gigabytes:
								limit *= 1000000000l;
								limitType = LimitUnit.rows.toString();
								break;
							default:
								limitType = uploadLimitType[0].toString();
								break;
						}
						xml.append("\t\t<default unit=\"").append(limitType).append("\">").append(limit).append("</default>\n");
					}
					if (uploadLimit[1] > -1){
						String limitType;
						long limit = uploadLimit[1];
						switch(uploadLimitType[1]){
							case kilobytes:
								limit *= 1000l;
								limitType = LimitUnit.rows.toString();
								break;
							case megabytes:
								limit *= 1000000l;
								limitType = LimitUnit.rows.toString();
								break;
							case gigabytes:
								limit *= 1000000000l;
								limitType = LimitUnit.rows.toString();
								break;
							default:
								limitType = uploadLimitType[1].toString();
								break;
						}
						xml.append("\t\t<hard unit=\"").append(limitType).append("\">").append(limit).append("</hard>\n");
					}
					xml.append("\t</uploadLimit>\n");
				}
			}
		}

		xml.append("\t</capability>");

		return xml.toString();
	}

	public final UWSService getUWS(){
		TAPResource res = resources.get("async");
		if (res != null)
			return ((ASync)res).getUWS();
		else
			return null;
	}

	/**
	 * @return The homePageURI.
	 */
	public final String getHomePageURI(){
		return homePageURI;
	}

	public final void setHomePageURI(String uri){
		homePageURI = (uri != null) ? uri.trim() : uri;
		if (homePageURI != null && homePageURI.length() == 0)
			homePageURI = null;
	}

	public void init(ServletConfig config) throws ServletException{
		for(TAPResource res : resources.values())
			res.init(config);
	}

	public void destroy(){
		for(TAPResource res : resources.values())
			res.destroy();
	}

	public void executeRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		response.setContentType("text/plain");

		if (tapBaseURL == null)
			setTAPBaseURL(request);

		JobOwner owner = null;
		String resourceName = null;

		try{
			// Identify the user:
			if (service.getUserIdentifier() != null)
				owner = service.getUserIdentifier().extractUserId(new UWSUrl(request), request);

			String[] resourcePath = (request.getPathInfo() == null) ? null : request.getPathInfo().split("/");
			// Display the TAP Main Page:
			if (resourcePath == null || resourcePath.length < 1){
				resourceName = "homePage";
				response.setContentType("text/html");
				writeHomePage(response.getWriter(), owner);
			}
			// or Display/Execute the selected TAP Resource:
			else{
				resourceName = resourcePath[1].trim().toLowerCase();
				TAPResource res = resources.get(resourceName);
				if (res != null)
					res.executeResource(request, response);
				else
					errorWriter.writeError("This TAP service does not have a resource named \"" + resourceName + "\" !", ErrorType.TRANSIENT, HttpServletResponse.SC_NOT_FOUND, response, request, null, "Get a TAP resource");
			}

			service.getLogger().httpRequest(request, owner, resourceName, HttpServletResponse.SC_OK, "[OK]", null);

			response.flushBuffer();
		}catch(IOException ioe){
			errorWriter.writeError(ioe, response, request, owner, (resourceName == null) ? "Writing the TAP home page" : ("Executing the TAP resource " + resourceName));
		}catch(UWSException ue){
			errorWriter.writeError(ue, response, request, owner, (resourceName == null) ? "Writing the TAP home page" : ("Executing the TAP resource " + resourceName));
		}catch(TAPException te){
			writeError(te, response);
		}catch(Throwable t){
			errorWriter.writeError(t, response, request, owner, (resourceName == null) ? "Writing the TAP home page" : ("Executing the TAP resource " + resourceName));
		}
	}

	public void writeHomePage(final PrintWriter writer, final JobOwner owner) throws IOException{
		// By default, list all available resources:
		if (homePageURI == null){
			writer.println("<html><head><title>TAP HOME PAGE</title></head><body><h1 style=\"text-align: center\">TAP HOME PAGE</h1><h2>Available resources:</h2><ul>");
			for(TAPResource res : resources.values())
				writer.println("<li><a href=\"" + tapBaseURL + "/" + res.getName() + "\">" + res.getName() + "</a></li>");
			writer.println("</ul></body></html>");
		}
		// or Display the specified home page:
		else{
			BufferedInputStream input = null;
			try{
				input = new BufferedInputStream((new URL(homePageURI)).openStream());
			}catch(MalformedURLException mue){
				input = new BufferedInputStream(new FileInputStream(new File(homePageURI)));
			}
			if (input == null)
				throw new IOException("Incorrect TAP home page URI !");
			byte[] buffer = new byte[255];
			int nbReads = 0;
			while((nbReads = input.read(buffer)) > 0)
				writer.print(new String(buffer, 0, nbReads));
		}
	}

	public void writeError(TAPException ex, HttpServletResponse response) throws ServletException, IOException{
		service.getLogger().error(ex);
		response.reset();
		response.setStatus(ex.getHttpErrorCode());
		response.setContentType("text/xml");
		writeError(ex, response.getWriter());
	}

	protected void writeError(TAPException ex, PrintWriter output) throws ServletException, IOException{
		output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		output.println("<VOTABLE xmlns=\"http://www.ivoa.net/xml/VOTable/v1.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VOTable/v1.2\" version=\"1.2\">");
		output.println("\t<RESOURCE type=\"results\">");

		// Print the error:
		output.println("\t\t<INFO name=\"QUERY_STATUS\" value=\"ERROR\">");
		output.print("\t\t\t<![CDATA[ ");
		if (ex.getExecutionStatus() != null)
			output.print("[WHILE " + ex.getExecutionStatus() + "] ");
		output.print(ex.getMessage().replace('«', '\"').replace('»', '\"'));
		output.println("]]>\t\t</INFO>");

		// Print the current date:
		DateFormat dateFormat = new SimpleDateFormat(UWSJob.DEFAULT_DATE_FORMAT);
		output.print("\t\t<INFO name=\"DATE\" value=\"");
		output.print(dateFormat.format(new Date()));
		output.println("\" />");

		// Print the provider (if any):
		if (service.getProviderName() != null){
			output.print("\t\t<INFO name=\"PROVIDER\" value=\"");
			output.print(service.getProviderName());
			if (service.getProviderDescription() != null){
				output.print("\">\n\t\t\t<![CDATA[");
				output.print(service.getProviderDescription());
				output.println("]]>\n\t\t</INFO>");
			}else
				output.println("\" />");
		}

		// Print the query (if any):
		if (ex.getQuery() != null){
			output.print("\t\t<INFO name=\"QUERY\">\n\t\t\t<![CDATA[");
			output.println(ex.getQuery());
			output.println("]]>\t\t</INFO>");
		}

		output.println("\t</RESOURCE>");
		output.println("</VOTABLE>");

		output.flush();
	}

	public final boolean addResource(TAPResource newResource){
		if (newResource == null)
			return false;
		resources.put(newResource.getName(), newResource);
		return true;
	}

	public final boolean addResource(String resourceId, TAPResource newResource){
		if (newResource == null)
			return false;
		resources.put((resourceId == null) ? newResource.getName() : resourceId, newResource);
		return true;
	}

	public final int getNbResources(){
		return resources.size();
	}

	public final TAPResource getResource(String resourceId){
		return resources.get(resourceId);
	}

	public final boolean hasResource(String resourceId){
		return resources.containsKey(resourceId);
	}

	public final TAPResource removeResource(String resourceId){
		return resources.remove(resourceId);
	}

	public boolean updateTAPMetadata(){
		TAPMetadata metadata = service.getTAPMetadata();
		if (metadata != null){
			resources.put(metadata.getName(), metadata);
			return true;
		}
		return false;
	}

}
