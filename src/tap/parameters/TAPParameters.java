package tap.parameters;

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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPJob;
import tap.upload.TableLoader;
import uws.UWSException;
import uws.job.parameters.InputParamController;
import uws.job.parameters.StringParamController;
import uws.job.parameters.UWSParameters;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.FileRenamePolicy;

/**
 * This class describes all defined parameters of a TAP request.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public class TAPParameters extends UWSParameters {

	/**
	 * All the TAP parameters.
	 */
	protected static final String[] TAP_PARAMETERS = new String[]{TAPJob.PARAM_REQUEST,TAPJob.PARAM_LANGUAGE,TAPJob.PARAM_VERSION,TAPJob.PARAM_FORMAT,TAPJob.PARAM_QUERY,TAPJob.PARAM_MAX_REC,TAPJob.PARAM_UPLOAD};

	/** Part of HTTP content type header. */
	public static final String MULTIPART = "multipart/";

	/** All the tables to upload. If NULL, there is no tables to upload. */
	protected TableLoader[] tablesToUpload = null;

	public TAPParameters(final ServiceConnection service){
		this(service, null, null);
	}

	public TAPParameters(final ServiceConnection service, final Collection<String> expectedAdditionalParams, final Map<String,InputParamController> inputParamControllers){
		super(expectedAdditionalParams, inputParamControllers);
		initDefaultTAPControllers(service);
	}

	public TAPParameters(final HttpServletRequest request, final ServiceConnection service) throws UWSException, TAPException{
		this(request, service, null, null);
	}

	@SuppressWarnings("unchecked")
	public TAPParameters(final HttpServletRequest request, final ServiceConnection service, final Collection<String> expectedAdditionalParams, final Map<String,InputParamController> inputParamControllers) throws UWSException, TAPException{
		this(service, expectedAdditionalParams, inputParamControllers);
		MultipartRequest multipart = null;

		// Multipart HTTP parameters:
		if (isMultipartContent(request)){
			if (!service.uploadEnabled())
				throw new TAPException("Request error! This TAP service has no Upload capability!", UWSException.BAD_REQUEST);

			File uploadDir = service.getFileManager().getUploadDirectory();
			try{
				multipart = new MultipartRequest(request, (uploadDir != null) ? uploadDir.getAbsolutePath() : null, service.getMaxUploadSize(), new FileRenamePolicy(){
					@Override
					public File rename(File file){
						return new File(file.getParentFile(), (new Date()).toString() + "_" + file.getName());
					}
				});
				Enumeration<String> e = multipart.getParameterNames();
				while(e.hasMoreElements()){
					String param = e.nextElement();
					set(param, multipart.getParameter(param));
				}
			}catch(IOException ioe){
				throw new TAPException("Error while reading the Multipart content!", ioe);
			}catch(IllegalArgumentException iae){
				String confError = iae.getMessage();
				if (service.getMaxUploadSize() <= 0)
					confError = "The maximum upload size (see ServiceConnection.getMaxUploadSize() must be positive!";
				else if (uploadDir == null)
					confError = "Missing upload directory (see TAPFileManager.getUploadDirectory())!";
				throw new TAPException("Incorrect Upload capability configuration! " + confError, iae);
			}

		}// Classic HTTP parameters (GET or POST):
		else{
			// Extract and identify each pair (key,value):
			Enumeration<String> e = request.getParameterNames();
			while(e.hasMoreElements()){
				String name = e.nextElement();
				set(name, request.getParameter(name));
			}
		}

		// Identify the tables to upload, if any:
		String uploadParam = getUpload();
		if (service.uploadEnabled() && uploadParam != null)
			tablesToUpload = buildLoaders(uploadParam, multipart);
	}

	public TAPParameters(final ServiceConnection service, final Map<String,Object> params) throws UWSException, TAPException{
		this(service, params, null, null);
	}

	public TAPParameters(final ServiceConnection service, final Map<String,Object> params, final Collection<String> expectedAdditionalParams, final Map<String,InputParamController> inputParamControllers) throws UWSException, TAPException{
		super(params, expectedAdditionalParams, inputParamControllers);
		initDefaultTAPControllers(service);
	}

	@Override
	protected final HashMap<String,InputParamController> getDefaultControllers(){
		return new HashMap<String,InputParamController>(10);
	}

	protected < R > void initDefaultTAPControllers(final ServiceConnection service){
		if (!mapParamControllers.containsKey(TAPJob.PARAM_EXECUTION_DURATION))
			mapParamControllers.put(TAPJob.PARAM_EXECUTION_DURATION, new TAPExecutionDurationController(service));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_DESTRUCTION_TIME))
			mapParamControllers.put(TAPJob.PARAM_DESTRUCTION_TIME, new TAPDestructionTimeController(service));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_REQUEST))
			mapParamControllers.put(TAPJob.PARAM_REQUEST, new StringParamController(TAPJob.PARAM_REQUEST, null, new String[]{TAPJob.REQUEST_DO_QUERY,TAPJob.REQUEST_GET_CAPABILITIES}, true));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_LANGUAGE))
			mapParamControllers.put(TAPJob.PARAM_LANGUAGE, new StringParamController(TAPJob.PARAM_LANGUAGE, TAPJob.LANG_ADQL, null, true));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_VERSION))
			mapParamControllers.put(TAPJob.PARAM_VERSION, new StringParamController(TAPJob.PARAM_VERSION, TAPJob.VERSION_1_0, new String[]{TAPJob.VERSION_1_0}, true));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_QUERY))
			mapParamControllers.put(TAPJob.PARAM_QUERY, new StringParamController(TAPJob.PARAM_QUERY));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_UPLOAD))
			mapParamControllers.put(TAPJob.PARAM_UPLOAD, new StringParamController(TAPJob.PARAM_UPLOAD));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_FORMAT))
			mapParamControllers.put(TAPJob.PARAM_FORMAT, new FormatController(service));

		if (!mapParamControllers.containsKey(TAPJob.PARAM_MAX_REC))
			mapParamControllers.put(TAPJob.PARAM_MAX_REC, new MaxRecController(service));
	}

	@Override
	protected String normalizeParamName(String name){
		if (name != null && !name.trim().isEmpty()){
			for(String tapParam : TAP_PARAMETERS){
				if (name.equalsIgnoreCase(tapParam))
					return tapParam;
			}
		}
		return super.normalizeParamName(name);
	}

	@Override
	public String[] update(UWSParameters newParams) throws UWSException{
		if (newParams != null && !(newParams instanceof TAPParameters))
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Can not update a TAPParameters instance with only a UWSException !");

		String[] updated = super.update(newParams);
		for(String p : updated){
			if (p.equals(TAPJob.PARAM_UPLOAD)){
				tablesToUpload = ((TAPParameters)newParams).tablesToUpload;
				break;
			}
		}
		return updated;
	}

	protected final String getStringParam(final String paramName){
		return (params.get(paramName) != null) ? params.get(paramName).toString() : null;
	}

	public final String getRequest(){
		return getStringParam(TAPJob.PARAM_REQUEST);
	}

	public final String getLang(){
		return getStringParam(TAPJob.PARAM_LANGUAGE);
	}

	public final String getVersion(){
		return getStringParam(TAPJob.PARAM_VERSION);
	}

	public final String getFormat(){
		return getStringParam(TAPJob.PARAM_FORMAT);
	}

	public final String getQuery(){
		return getStringParam(TAPJob.PARAM_QUERY);
	}

	public final String getUpload(){
		return getStringParam(TAPJob.PARAM_UPLOAD);
	}

	public final TableLoader[] getTableLoaders(){
		return tablesToUpload;
	}

	public final Integer getMaxRec(){
		Object value = params.get(TAPJob.PARAM_MAX_REC);
		if (value != null){
			if (value instanceof Integer)
				return (Integer)value;
			else if (value instanceof String){
				try{
					Integer maxRec = Integer.parseInt((String)value);
					synchronized(params){
						params.put(TAPJob.PARAM_MAX_REC, maxRec);
					}
					return maxRec;
				}catch(NumberFormatException nfe){
					;
				}
			}
		}
		return null;
	}

	/**
	 * Utility method that determines whether the request contains multipart
	 * content.
	 *
	 * @param request The servlet request to be evaluated. Must be non-null.
	 *
	 * @return <code>true</code> if the request is multipart;
	 *         <code>false</code> otherwise.
	 */
	public static final boolean isMultipartContent(HttpServletRequest request){
		if (!"post".equals(request.getMethod().toLowerCase())){
			return false;
		}
		String contentType = request.getContentType();
		if (contentType == null){
			return false;
		}
		if (contentType.toLowerCase().startsWith(MULTIPART)){
			return true;
		}
		return false;
	}

	/**
	 * Builds as many TableLoader instances as tables to upload.
	 * 
	 * @param upload	The upload field (syntax: "tableName1,URI1 ; tableName2,URI2 ; ...", where URI may start by "param:" to indicate that the VOTable is inline).
	 * @param multipart	The multipart content of the request if any.
	 * 
	 * @return			All table loaders (one per table to upload).
	 * 
	 * @throws TAPException	If the syntax of the "upload" field is incorrect.
	 */
	private TableLoader[] buildLoaders(final String upload, final MultipartRequest multipart) throws TAPException{
		if (upload == null || upload.trim().isEmpty())
			return new TableLoader[0];

		String[] pairs = upload.split(";");
		TableLoader[] loaders = new TableLoader[pairs.length];

		for(int i = 0; i < pairs.length; i++){
			String[] table = pairs[i].split(",");
			if (table.length != 2)
				throw new TAPException("UPLOAD parameter incorrect: bad syntax! An UPLOAD parameter must contain a list of pairs separated by a ';'. Each pair is composed of 2 parts, a table name and a URI separated by a ','.", UWSException.BAD_REQUEST);
			loaders[i] = new TableLoader(table[0], table[1], multipart);
		}

		return loaders;
	}

	public void check() throws TAPException{
		// Check that required parameters are not NON-NULL:
		String requestParam = getRequest();
		if (requestParam == null)
			throw new TAPException("The parameter \"" + TAPJob.PARAM_REQUEST + "\" must be provided and its value must be equal to \"" + TAPJob.REQUEST_DO_QUERY + "\" or \"" + TAPJob.REQUEST_GET_CAPABILITIES + "\" !", UWSException.BAD_REQUEST);

		if (requestParam.equals(TAPJob.REQUEST_DO_QUERY)){
			if (get(TAPJob.PARAM_LANGUAGE) == null)
				throw new TAPException("The parameter \"" + TAPJob.PARAM_LANGUAGE + "\" must be provided if " + TAPJob.PARAM_REQUEST + "=" + TAPJob.REQUEST_DO_QUERY + " !", UWSException.BAD_REQUEST);
			else if (get(TAPJob.PARAM_QUERY) == null)
				throw new TAPException("The parameter \"" + TAPJob.PARAM_QUERY + "\" must be provided if " + TAPJob.PARAM_REQUEST + "=" + TAPJob.REQUEST_DO_QUERY + " !", UWSException.BAD_REQUEST);
		}

		// Check the version if needed:
		/*Object versionParam = get(TAPJob.PARAM_VERSION);
		if (versionParam != null && !versionParam.equals("1") && !versionParam.equals("1.0"))
			throw new TAPException("Version \""+versionParam+"\" of TAP not implemented !");*/

		/*// Check format if needed:
		if (format == null)
			format = FORMAT_VOTABLE;

		// Check maxrec:
		if (maxrec <= -1)
			maxrec = defaultOutputLimit;

		if (maxOutputLimit > -1){
			if (maxrec > maxOutputLimit)
				maxrec = maxOutputLimit;
			else if (maxrec <= -1)
				maxrec = maxOutputLimit;
		}*/
	}

	public static final void deleteUploadedTables(final TableLoader[] loaders){
		if (loaders != null){
			for(TableLoader loader : loaders)
				loader.deleteFile();
		}
	}
}
