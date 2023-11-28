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
 * Copyright 2012-2016 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPJob;
import uws.UWSException;
import uws.job.parameters.InputParamController;
import uws.job.parameters.StringParamController;
import uws.job.parameters.UWSParameters;

/**
 * This class lets list and describe all standard TAP parameters
 * submitted by a TAP client to this TAP service.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.1 (04/2017)
 */
public class TAPParameters extends UWSParameters {

	/** All the TAP parameters. */
	protected static final List<String> TAP_PARAMETERS = Arrays.asList(new String[]{TAPJob.PARAM_REQUEST,TAPJob.PARAM_LANGUAGE,TAPJob.PARAM_VERSION,TAPJob.PARAM_FORMAT,TAPJob.PARAM_QUERY,TAPJob.PARAM_MAX_REC,TAPJob.PARAM_UPLOAD});

	/**
	 * Create an empty list of parameters.
	 * 
	 * @param service	Description of the TAP service in which the parameters are created and will be used.
	 */
	public TAPParameters(final ServiceConnection service){
		super(TAP_PARAMETERS, buildDefaultControllers(service, null));
	}

	/**
	 * Create a {@link TAPParameters} instance whose the parameters must be extracted from the given {@link HttpServletRequest}.
	 * 
	 * @param request	HTTP request containing the parameters to gather inside this class.
	 * @param service	Description of the TAP service in which the parameters are created and will be used.
	 * 
	 * @throws TAPException	If any error occurs while extracting the DALIParameters OR while setting a parameter.
	 * 
	 * @see #getParameters(HttpServletRequest)
	 */
	public TAPParameters(final HttpServletRequest request, final ServiceConnection service) throws TAPException{
		this(service, getParameters(request));
	}

	/**
	 * Create a {@link TAPParameters} instance whose the parameters must be extracted from the given {@link HttpServletRequest}.
	 * 
	 * @param request		HTTP request containing the parameters to gather inside this class.
	 * @param service		Description of the TAP service in which the parameters are created and will be used.
	 * @param controllers	Additional/Replacing controllers to apply on some input parameters.
	 *                      <i>Ignored if <code>NULL</code>.</i>
	 * 
	 * @throws TAPException	If any error occurs while extracting the DALIParameters OR while setting a parameter.
	 * 
	 * @see #getParameters(HttpServletRequest)
	 * 
	 * @since 2.1
	 */
	public TAPParameters(final HttpServletRequest request, final ServiceConnection service, final Map<String,InputParamController> controllers) throws TAPException{
		this(service, getParameters(request), controllers);
	}

	/**
	 * Create a {@link TAPParameters} instance whose the parameters are given in parameter.
	 * 
	 * @param service	Description of the TAP service. Limits of the standard TAP parameters are listed in it.
	 * @param params	List of parameters to load inside this object.
	 * 
	 * @throws TAPException	If any error occurs while extracting the DALIParameters OR while setting a parameter.
	 * 
	 * @see #TAPParameters(ServiceConnection, Map, Map)
	 */
	public TAPParameters(final ServiceConnection service, final Map<String,Object> params) throws TAPException{
		this(service, params, null);
	}

	/**
	 * Create a {@link TAPParameters} instance whose the parameters are given in parameter.
	 * 
	 * @param service		Description of the TAP service. Limits of the standard TAP parameters are listed in it.
	 * @param params		List of parameters to load inside this object.
	 * @param controllers	Additional/Replacing controllers to apply on some input parameters.
	 *                      <i>Ignored if <code>NULL</code>.</i>
	 * 
	 * @throws TAPException	If any error occurs while extracting the DALIParameters OR while setting a parameter.
	 */
	public TAPParameters(final ServiceConnection service, final Map<String,Object> params, final Map<String,InputParamController> controllers) throws TAPException{
		super(TAP_PARAMETERS, buildDefaultControllers(service, controllers));

		if (params != null && !params.isEmpty()){
			// Deal with the UPLOAD parameter(s):
			DALIUpload.getDALIUploads(params, true, service.getFileManager());

			// Load all parameters:
			Iterator<Entry<String,Object>> it = params.entrySet().iterator();
			Entry<String,Object> entry;
			try{
				while(it.hasNext()){
					entry = it.next();
					set(entry.getKey(), entry.getValue());
				}
			}catch(UWSException ue){
				throw new TAPException(ue);
			}
		}
	}

	/**
	 * <p>Build a map containing all controllers for all standard TAP parameters.</p>
	 * 
	 * <p><i>Note:
	 * 	All standard parameters, except UPLOAD. Indeed, since this parameter can be provided in several times (in one HTTP request)
	 * 	and needs to be interpreted immediately after initialization, no controller has been set for it. Its value will be actually
	 * 	tested in the constructor while interpreting it.
	 * </i></p>
	 * 
	 * @param service			Description of the TAP service.
	 * @param customControllers	Additional/Replacing controllers to apply on some input parameters.
	 *                         	<i>Ignored if <code>NULL</code>.</i>
	 * 
	 * @return	Map of all default controllers.
	 * 
	 * @since 2.0
	 */
	protected static final Map<String,InputParamController> buildDefaultControllers(final ServiceConnection service, final Map<String,InputParamController> customControllers){
		Map<String,InputParamController> controllers = new HashMap<String,InputParamController>(10);

		// Set the default controllers:
		controllers.put(TAPJob.PARAM_EXECUTION_DURATION, new TAPExecutionDurationController(service));
		controllers.put(TAPJob.PARAM_DESTRUCTION_TIME, new TAPDestructionTimeController(service));
		controllers.put(TAPJob.PARAM_REQUEST, new StringParamController(TAPJob.PARAM_REQUEST, null, new String[]{TAPJob.REQUEST_DO_QUERY,TAPJob.REQUEST_GET_CAPABILITIES}, true));
		controllers.put(TAPJob.PARAM_LANGUAGE, new StringParamController(TAPJob.PARAM_LANGUAGE, TAPJob.LANG_ADQL, (String[])null, true));
		controllers.put(TAPJob.PARAM_VERSION, new StringParamController(TAPJob.PARAM_VERSION, TAPJob.VERSION_1_0, new String[]{TAPJob.VERSION_1_0}, true));
		controllers.put(TAPJob.PARAM_QUERY, new StringParamController(TAPJob.PARAM_QUERY));
		controllers.put(TAPJob.PARAM_FORMAT, new FormatController(service));
		controllers.put(TAPJob.PARAM_MAX_REC, new MaxRecController(service));

		// Add/Replace with the given controllers:
		if (customControllers != null){
			for(Map.Entry<String,InputParamController> item : customControllers.entrySet()){
				if (item.getKey() != null && item.getValue() != null)
					controllers.put(item.getKey(), item.getValue());
			}
		}

		return controllers;
	}

	/**
	 * <p>Get the value of the given parameter, but as a String, whatever is its original type.</p>
	 * 
	 * <p>Basically, the different cases of conversion into String are the following:</p>
	 * <ul>
	 * 	<li><b>NULL</b>: NULL is returned.</li>
	 * 	<li><b>An array (of whatever is the items' type)</b>: a string in which each Object.toString() are concatenated ; each item is separated by a semicolon</li>
	 * 	<li><b>Anything else</b>: Object.toString()</li>
	 * </ul>
	 * 
	 * @param paramName	Name of the parameter whose the value must be returned as a String.
	 * 
	 * @return	The string value of the specified parameter.
	 */
	protected final String getStringParam(final String paramName){
		// Get the parameter value as an Object:
		Object value = params.get(paramName);

		// Convert this Object into a String:
		// CASE: NULL
		if (value == null)
			return null;

		// CASE: ARRAY
		else if (value.getClass().isArray()){
			StringBuffer buf = new StringBuffer();
			for(Object o : (Object[])value){
				if (buf.length() > 0)
					buf.append(';');
				buf.append(o.toString());
			}
			return buf.toString();
		}
		// DEFAULT:
		else
			return value.toString();
	}

	/**
	 * Get the value of the standard TAP parameter "REQUEST".
	 * @return	"REQUEST" value.
	 */
	public final String getRequest(){
		return getStringParam(TAPJob.PARAM_REQUEST);
	}

	/**
	 * Get the value of the standard TAP parameter "LANG".
	 * @return	"LANG" value.
	 */
	public final String getLang(){
		return getStringParam(TAPJob.PARAM_LANGUAGE);
	}

	/**
	 * Get the value of the standard TAP parameter "VERSION".
	 * @return	"VERSION" value.
	 */
	public final String getVersion(){
		return getStringParam(TAPJob.PARAM_VERSION);
	}

	/**
	 * Get the value of the standard TAP parameter "FORMAT".
	 * @return	"FORMAT" value.
	 */
	public final String getFormat(){
		return getStringParam(TAPJob.PARAM_FORMAT);
	}

	/**
	 * Get the value of the standard TAP parameter "QUERY".
	 * @return	"QUERY" value.
	 */
	public final String getQuery(){
		return getStringParam(TAPJob.PARAM_QUERY);
	}

	/**
	 * <p>Get the value of the standard TAP parameter "UPLOAD".</p>
	 * <p><i>Note:
	 * 	This parameter is generally a set of several Strings, each representing one table to upload.
	 * 	This function returns this set as a String in which each items are joined, semicolon separated, inside a single String.
	 * <i></p>
	 * @return	"UPLOAD" value.
	 */
	public final String getUpload(){
		return getStringParam(TAPJob.PARAM_UPLOAD);
	}

	/**
	 * Get the list of all tables uploaded and defined by the standard TAP parameter "UPLOAD".
	 * 
	 * @return	Tables to upload in database at query execution.
	 */
	public final DALIUpload[] getUploadedTables(){
		return (DALIUpload[])get(TAPJob.PARAM_UPLOAD);
	}

	/**
	 * Get the value of the standard TAP parameter "MAX_REC".
	 * This value is the maximum number of rows that the result of the query must contain.
	 * 
	 * @return	Maximum number of output rows.
	 */
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
	 * <p>Check the coherence between all TAP parameters.</p>
	 * 
	 * <p>
	 * 	This function does not test individually each parameters, but all of them as a coherent whole.
	 * 	Thus, the parameter REQUEST must be provided and if its value is "doQuery", the parameters LANG and QUERY must be also provided.
	 * </p>
	 * 
	 * @throws TAPException	If one required parameter is missing.
	 */
	public void check() throws TAPException{
		// Check that required parameters are not NON-NULL:
		String requestParam = getRequest();
		if (requestParam == null)
			throw new TAPException("The parameter \"" + TAPJob.PARAM_REQUEST + "\" must be provided and its value must be equal to \"" + TAPJob.REQUEST_DO_QUERY + "\" or \"" + TAPJob.REQUEST_GET_CAPABILITIES + "\"!", UWSException.BAD_REQUEST);

		if (requestParam.equals(TAPJob.REQUEST_DO_QUERY)){
			if (get(TAPJob.PARAM_LANGUAGE) == null)
				throw new TAPException("The parameter \"" + TAPJob.PARAM_LANGUAGE + "\" must be provided if " + TAPJob.PARAM_REQUEST + "=" + TAPJob.REQUEST_DO_QUERY + "!", UWSException.BAD_REQUEST);
			else if (get(TAPJob.PARAM_QUERY) == null)
				throw new TAPException("The parameter \"" + TAPJob.PARAM_QUERY + "\" must be provided if " + TAPJob.PARAM_REQUEST + "=" + TAPJob.REQUEST_DO_QUERY + "!", UWSException.BAD_REQUEST);
		}
	}
}
