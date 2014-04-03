package uws.job.parameters;

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

import java.text.ParseException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Date;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;

import uws.job.UWSJob;

import uws.service.UWS;

/**
 * <p>Lets extracting all UWS standard and non-standard parameters from a map or a {@link HttpServletRequest}.</p>
 * 
 * <h3>Input parameter check</h3>
 * <p>It is possible to check the value of some or all parameters by calling the function {@link InputParamController#check(Object)}
 * of an {@link InputParamController} associated with the name of the parameter. Input parameter controllers can be
 * provided at the creation of a {@link UWSParameters}. If none are given, default ones are used (see {@link #getDefaultUWSParamControllers()}).</p>
 * 
 * <h3>Default value</h3>
 * <p>By calling the function {@link #init()}, you set a default value to any parameter which has an {@link InputParamController}
 * and which has not yet a value.</p>
 * <p>The function {@link InputParamController#getDefault()} returns a default value for its associated parameter.
 * This value must be obviously different from <i>NULL</i>.</p>
 * 
 * <h3>Updating a {@link UWSParameters}</h3>
 * <p>It is possible to update a {@link UWSParameters} with another {@link UWSParameters} thanks to the function
 * {@link #update(UWSParameters)}. In this case, no check is done since the values given by a
 * {@link UWSParameters} should be theoretically already correct.</p>
 * <p>In order to forbid the modification of some parameters after their initialization, you must associate an
 * {@link InputParamController} with them and override the function {@link InputParamController#allowModification()}
 * so that it returns <i>false</i>.</p>
 * 
 * <h3>Case sensitivity</h3>
 * <p>All UWS STANDARD parameters can be provided in any case: they will always be identified and updated.
 * However any other parameter will be stored as it is provided: so with the same case. Thus, you must respect
 * the case for all UWS additional parameters in your other operations on the parameters.</p>
 * <p>If you want to identify your own parameters without case sensitivity, you must provides a list
 * of all the additional parameters you are expected at the creation: see {@link #UWSParameters(HttpServletRequest, Collection, Map)}
 * and {@link #UWSParameters(Map, Collection, Map)}.</p>
 * 
 * <h3>Additional parameters case normalization</h3>
 * <p>Indeed, the second parameter of these constructors (if != NULL) is used to normalize the name of the additional parameters so
 * that they have exactly the given case.</p>
 * <p>For instance, suppose that the given HttpServletRequest has a parameter named "foo" and
 * you expect a parameter named "FOO" (only the case changes). By providing a second parameter
 * which contains the entry "FOO", all parameters having the same name - even if the case is different -
 * will be named "FOO".</p>
 * <p>In brief:</p>
 * <ul>
 * 	<li><u>With "FOO" in the second parameter of the constructor:</u> {@link #get(String) get("FOO")} will return something if in the HttpServletRequest there is a parameter named: "foo", "FOO", "Foo", ...</li>
 * 	<li><u>If the second parameter is empty, NULL or does not contain "FOO":</u> {@link #get(String) get("FOO")} will return something if in the HttpServletRequest there is a parameter named exactly "FOO".</li>
 * </ul>
 * 
 * <h3>UWS standard parameters</h3>
 * <p>All UWS standard parameters are identified in this class. However, only READ/WRITE parameters are kept.
 * All the others are ignored. The read/write UWS standard parameters are:</p>
 * <ul>
 * 	<li>runId ({@link UWSJob#PARAM_RUN_ID})</li>
 * 	<li>executionDuration ({@link UWSJob#PARAM_EXECUTION_DURATION})</li>
 * 	<li>destruction ({@link UWSJob#PARAM_DESTRUCTION_TIME})</li>
 * </ul>
 * <p><i><u>note:</u> All parameters stored under the parameter {@link UWSJob#PARAM_PARAMETERS} (that's to say, additional parameters)
 * are also considered as READ/WRITE parameters !</i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public class UWSParameters implements Iterable<Entry<String, Object>> {

	/**
	 * <p>Read-Write parameters.</p>
	 * <p>Names of the UWS parameters whose the value can be modified by the user.</p>
	 */
	protected final static String[] UWS_RW_PARAMETERS = new String[]{UWSJob.PARAM_PHASE, UWSJob.PARAM_RUN_ID, UWSJob.PARAM_EXECUTION_DURATION, UWSJob.PARAM_DESTRUCTION_TIME, UWSJob.PARAM_PARAMETERS};
	/**
	 * <p>Read-Only parameters.</p>
	 * <p>Names of the UWS parameters whose the value can NOT be modified by the user. These value are not kept. They are only ignored.</p>
	 */
	protected final static String[] UWS_RO_PARAMETERS = new String[]{UWSJob.PARAM_JOB_ID, UWSJob.PARAM_OWNER, UWSJob.PARAM_QUOTE, UWSJob.PARAM_START_TIME, UWSJob.PARAM_END_TIME, UWSJob.PARAM_RESULTS, UWSJob.PARAM_ERROR_SUMMARY};

	/**
	 * List of all extracted parameters.
	 */
	protected final Map<String, Object> params = new HashMap<String, Object>(10);

	/**
	 * <p>List of all UWS additional parameters.</p>
	 * <p><i><u>IMPORTANT:</u> This map is built on demand ONLY when {@link #getAdditionalParameters()} is called if not already initialized.
	 * It is deleted (set to NULL) when there is a modification in the list of all parameters
	 * (so in the function {@link #set(String, Object)}, {@link #update(UWSParameters)} and {@link #init()}).</i></p>
	 */
	private HashMap<String, Object> additionalParams = null;

	/**
	 * List of the expected additional parameters.
	 */
	protected final Collection<String> expectedAdditionalParams;

	/**
	 * List of the controllers of all the input parameters.
	 */
	protected final Map<String, InputParamController> mapParamControllers;


	/**
	 * Builds an empty list of UWS parameters.
	 * 
	 * @see #UWSParameters(Collection, Map)
	 */
	public UWSParameters(){
		this(null, null);
	}

	/**
	 * <p>Builds an empty list of UWS parameters.</p>
	 * 
	 * <p><i><u>note:</u> Even if no controllers is provided, this constructor sets the default
	 * input parameter controllers (see {@link #getDefaultUWSParamControllers()}).</i></p>
	 * 
	 * @param expectedAdditionalParams	The names of all expected additional parameters (MAY BE NULL).
	 * 									<i><u>note:</u> they will be identified with no case sensitivity
	 * 									and stored with the same case as in this collection.</i>
	 * @param inputParamControllers		Controllers of the input parameters (MAY BE NULL).
	 * 
	 * @see #getDefaultUWSParamControllers()
	 */
	public UWSParameters(final Collection<String> expectedAdditionalParams, final Map<String, InputParamController> inputParamControllers){
		// Set the input parameter controllers:
		mapParamControllers = getDefaultControllers();
		if (inputParamControllers != null)
			mapParamControllers.putAll(inputParamControllers);

		// Set the expected additional parameters:
		this.expectedAdditionalParams = expectedAdditionalParams;
	}

	/**
	 * <p>Extracts and identifies all UWS standard and non-standard parameters from the given {@link HttpServletRequest}.</p>
	 * 
	 * <p><i><u>note:</u> The default input parameter controllers are set by default (see {@link #getDefaultUWSParamControllers()}).</i></p>
	 * 
	 * @param request			The request to parse to extract the parameters.
	 * 
	 * @throws UWSException		If one of the given parameter is incorrect or badly formatted.
	 * 
	 * @see #UWSParameters(HttpServletRequest, Collection, Map)
	 */
	public UWSParameters(final HttpServletRequest request) throws UWSException {
		this(request, null, null);
	}



	/**
	 * <p>Extracts and identifies all UWS standard and non-standard parameters from the given {@link HttpServletRequest}.</p>
	 * 
	 * <p><i><u>note:</u> Even if no controllers is provided, this constructor sets the default
	 * input parameter controllers (see {@link #getDefaultUWSParamControllers()}).</i></p>
	 * 
	 * @param request					The request to parse to extract the parameters.
	 * @param expectedAdditionalParams	The names of all expected additional parameters.
	 * 									<i><u>note:</u> they will be identified with no case sensitivity
	 * 									and stored with the same case as in this collection.</i>
	 * @param inputParamControllers		Controllers of the input parameters.
	 * 
	 * @throws UWSException		If one of the given parameter is incorrect or badly formatted.
	 * 
	 * @see #UWSParameters(Collection, Map)
	 */
	@SuppressWarnings("unchecked")
	public UWSParameters(final HttpServletRequest request, final Collection<String> expectedAdditionalParams, final Map<String, InputParamController> inputParamControllers) throws UWSException {
		this(expectedAdditionalParams, inputParamControllers);

		// Load all parameters:
		if (request != null){
			Enumeration<String> names = request.getParameterNames();
			String paramName;
			while(names.hasMoreElements()){
				paramName = names.nextElement();
				set(paramName, request.getParameter(paramName));
			}
		}
	}

	/**
	 * <p>Extracts and identifies all UWS standard and non-standard parameters from the map.</p>
	 * 
	 * <p><i><u>note:</u> The default input parameter controllers are set by default (see {@link #getDefaultUWSParamControllers()}).</i></p>
	 * 
	 * @param params			A map of parameters.
	 * 
	 * @throws UWSException		If one of the given parameter is incorrect or badly formatted.
	 * 
	 * @see #UWSParameters(Map, Collection, Map)
	 */
	public UWSParameters(final Map<String, Object> params) throws UWSException {
		this(params, null, null);
	}

	/**
	 * <p>Extracts and identifies all UWS standard and non-standard parameters from the map.</p>
	 * 
	 * <p><i><u>note:</u> Even if no controllers is provided, this constructor sets the default
	 * input parameter controllers (see {@link #getDefaultUWSParamControllers()}).</i></p>
	 * 
	 * @param params					A map of parameters.
	 * @param expectedAdditionalParams	The names of all expected additional parameters.
	 * 									<i><u>note:</u> they will be identified with no case sensitivity
	 * 									and stored with the same case as in this collection.</i>
	 * @param inputParamControllers		Controllers of the input parameters.
	 * 
	 * @throws UWSException		If one of the given parameter is incorrect or badly formatted.
	 * 
	 * @see #UWSParameters(Collection, Map)
	 */
	public UWSParameters(final Map<String, Object> params, final Collection<String> expectedAdditionalParams, final Map<String, InputParamController> inputParamControllers) throws UWSException {
		this(expectedAdditionalParams, inputParamControllers);

		// Load all parameters:
		if (params != null && !params.isEmpty()){
			synchronized(params){
				Iterator<Entry<String, Object>> it = params.entrySet().iterator();
				Entry<String, Object> entry;
				while(it.hasNext()){
					entry = it.next();
					set(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	/**
	 * Builds a default list of controllers for the standard UWS input parameters.
	 * 
	 * @return Default list of controllers. <i><u>note:</u> This map can be modified !</i>
	 */
	protected HashMap<String, InputParamController> getDefaultControllers(){
		HashMap<String, InputParamController> controllers = new HashMap<String, InputParamController>(2);
		controllers.put(UWSJob.PARAM_EXECUTION_DURATION, new ExecutionDurationController());
		controllers.put(UWSJob.PARAM_DESTRUCTION_TIME, new DestructionTimeController());
		return controllers;
	}

	/**
	 * <p>Must return the input parameter controller of the specified parameter.</p>
	 * 
	 * <p><i><u>note 1:</u> This function is supposed to be case sensitive !</i></p>
	 * <p><i><u>note 2:</u> By default, this function just asks to the {@link UWS} thanks to the function {@link UWS#getInputParamController(String)}.</i></p>
	 * 
	 * @param inputParamName	The name of the parameter whose the controller is asked.
	 * 
	 * @return					The corresponding controller or <i>null</i> if there is no controller for the specified parameter
	 * 							or if this {@link UWSParameters} instance doesn't know a {@link UWS}.
	 */
	protected InputParamController getController(final String inputParamName){
		return mapParamControllers.get(inputParamName);
	}

	/**
	 * <p>Must return the list of all available input parameter controllers.</p>
	 * 
	 * <p><i><u>note:</u> By default, this function just asks to the {@link UWS} thanks to the function {@link UWS#getInputParamControllers()}.</i></p>
	 * 
	 * @return		The list of all available controllers or <i>null</i> if there is no controller
	 * 				or if this {@link UWSParameters} instance doesn't know a {@link UWS}.
	 */
	protected Iterator<Entry<String, InputParamController>> getControllers(){
		return mapParamControllers.entrySet().iterator();
	}

	public final Iterator<Entry<String, Object>> iterator(){
		return params.entrySet().iterator();
	}

	/**
	 * Gets the name of all parameters.
	 * @return	List of all parameter names.
	 */
	public final Set<String> getNames(){
		return params.keySet();
	}

	/**
	 * Tells whether there is no parameter or not.
	 * @return	<i>true</i> if there is no parameter, <i>false</i> otherwise.
	 */
	public final boolean isEmpty(){
		return params.isEmpty();
	}

	/**
	 * Gets the number of all stored parameters.
	 * @return	Number of all parameters.
	 */
	public final int size(){
		return params.size();
	}

	/**
	 * <p>Set the default value to all missing parameters (ONLY for those who have a controller -&gt; see {@link InputParamController}).</p>
	 * 
	 * <p><i><u>note:</u> This method is thread safe (synchronized on the list of parameters) !</i></p>
	 */
	public final void init(){
		Iterator<Entry<String, InputParamController>> itControllers = getControllers();
		if (itControllers != null){
			synchronized(params){
				Entry<String, InputParamController> entry;
				while(itControllers.hasNext()){
					entry = itControllers.next();
					if (!params.containsKey(entry.getKey()) && entry.getValue().getDefault() != null)
						params.put(entry.getKey(), entry.getValue().getDefault());
				}
			}
		}
	}

	/**
	 * <p>Updates this list of UWS parameters with the given ones. No check is done on the given parameters
	 * (since they come from an instance of {@link UWSParameters}, they are supposed to be correct)</p>
	 * 
	 * <p><i><u>note:</u> This method is thread safe (synchronized on the list of parameters) !</i></p>
	 * 
	 * @param newParams	The parameters to update.
	 * 
	 * @return			An array listing the name of the updated parameters.
	 * 
	 * @exception UWSException	If one of the given parameters is not allowed to be modified.
	 */
	public String[] update(final UWSParameters newParams) throws UWSException {
		if (newParams != null && !newParams.params.isEmpty()){
			synchronized(params){
				additionalParams = null;
				String[] updated = new String[newParams.params.size()];
				int i=0;
				for(Entry<String, Object> entry : newParams){
					// Test whether this parameter is allowed to be modified after its initialization:
					InputParamController controller = getController(entry.getKey());
					if (controller != null && !controller.allowModification())
						throw new UWSException("The parameter \""+entry.getKey()+"\" can not be modified after initialization !");
					// If the value is NULL, removes this parameter:
					if (entry.getValue() == null)
						params.remove(entry.getKey());
					// Else set it:
					else
						params.put(entry.getKey(), entry.getValue());
					updated[i++] = entry.getKey();
				}
				return updated;
			}
		}else
			return new String[0];
	}

	/**
	 * <p>Gets the value of the specified parameter.</p>
	 * 
	 * <p><i><u>note 1:</u> The case of the parameter name MUST BE correct EXCEPT FOR the standard UWS parameters (i.e. runId, executionDuration, destructionTime).</i></p>
	 * <p><i><u>note 2:</u> If the name of the parameter is {@link UWSJob#PARAM_PARAMETERS PARAMETERS}, this function will return exactly what {@link #getAdditionalParameters()} returns.</i></p>
	 * 
	 * @param name	Name of the parameter to get.
	 * 
	 * @return		Value of the specified parameter, or <i>null</i> if the given name is <i>null</i>, empty or has no value.
	 * 
	 * @see #normalizeParamName(String)
	 * @see #getAdditionalParameters()
	 */
	public final Object get(final String name){
		String normalizedName = normalizeParamName(name);
		if (normalizedName == null)
			return null;
		else if (normalizedName.equals(UWSJob.PARAM_PARAMETERS))
			return getAdditionalParameters();
		else
			return params.get(normalizedName);
	}

	/**
	 * <p>Sets the given value to the specified parameter.
	 * But if the given value is <i>null</i>, the specified parameter is merely removed.</p>
	 * 
	 * <p><i><u>note 1:</u> This method is thread safe (synchronized on the list of parameters) !</i></p>
	 * <p><i><u>note 2:</u> The case of the parameter name MUST BE correct EXCEPT FOR the standard UWS parameters (i.e. runId, executionDuration, destructionTime).</i></p>
	 * <p><i><u>note 3:</u> If the specified parameter is a read-only UWS parameter (i.e. jobId, startTime, endTime, results, errorSummary, quote), this function does nothing and merely returns NULL.</i></p>
	 * <p><i><u>note 4:</u> A value equals to NULL, means that the specified parameter must be removed.</i></p>
	 * <p><i><u>note 5:</u> If the parameter {@link UWSJob#PARAM_PARAMETERS PARAMETERS} is given, it must be a Map<String, Object>. In this case, the map is read and all its entries are added individually.</i></p>
	 * 
	 * @param name				Name of the parameter to set (add, update or remove). <i><u>note:</u> not case sensitive ONLY FOR the standard UWS parameters !</i>
	 * @param value				The value to set. <i><u>note:</u> NULL means that the specified parameter must be removed !</i>
	 * 
	 * @return					The old value of the specified parameter. <i>null</i> may mean that the parameter has just been added, but it may also mean that nothing has been done (because, the given name is null, empty or corresponds to a read-only parameter).
	 * 
	 * @throws UWSException		If the given value is incorrect or badly formatted.
	 * 
	 * @see #normalizeParamName(String)
	 */
	@SuppressWarnings("unchecked")
	public final Object set(final String name, Object value) throws UWSException {
		// Normalize (take into account the case ONLY FOR the non-standard UWS parameters) the given parameter name:
		String normalizedName = normalizeParamName(name);

		// If the normalized name is NULL, the parameter must be ignored => nothing is done !
		if (normalizedName == null)
			return null;

		synchronized(params){
			additionalParams = null;

			// If the given value is NULL, the parameter must be removed:
			if (value == null)
				return params.remove(normalizedName);
			else{
				// Case of the PARAMETERS parameter: read all parameters and set them individually into this UWSParameters instance:
				if (normalizedName.equals(UWSJob.PARAM_PARAMETERS)){
					// the value MUST BE a Map<String, Object>:
					if (value instanceof Map){
						try{
							Map<String, Object> otherParams = (Map<String,Object>)value;
							HashMap<String, Object> mapOldValues = new HashMap<String, Object>(otherParams.size());
							Object oldValue = null;
							for(Entry<String, Object> entry : otherParams.entrySet()){
								oldValue = set(entry.getKey(), entry.getValue());
								mapOldValues.put(entry.getKey(), oldValue);
							}
							return mapOldValues;
						}catch(ClassCastException cce){
							return null;
						}
					}else
						return null;
				}else{
					// Check the value before setting it:
					InputParamController controller = getController(normalizedName);
					if (controller != null)
						value = controller.check(value);

					// Set the new value:
					return params.put(normalizedName, value);
				}
			}
		}
	}

	/**
	 * Removes the value of the given input parameter name.
	 * 
	 * @param inputParamName	Name of the parameter to remove. <i><u>note:</u> not case sensitive ONLY FOR the standard UWS parameters !</i>
	 * 
	 * @return					The removed value.
	 * 
	 * @see #normalizeParamName(String)
	 */
	public Object remove(final String inputParamName){
		// Normalize (take into account the case ONLY FOR the non-standard UWS parameters) the given parameter name:
		String normalizedName = normalizeParamName(inputParamName);

		// If the normalized name is NULL, the parameter must be ignored => nothing is done !
		if (normalizedName == null)
			return null;

		synchronized(params){
			additionalParams = null;
			return params.remove(normalizedName);
		}
	}

	/**
	 * Normalizes the given name.
	 * 
	 * @param name	Name of the parameter to identify/normalize.
	 * 
	 * @return		Normalized name or <i>null</i> if the parameters should be ignored (i.e. empty name and UWS read-only parameters).
	 */
	protected String normalizeParamName(final String name){
		if (name == null || name.trim().length() == 0)
			return null;

		for(String uwsParam : UWS_RW_PARAMETERS){
			if (name.equalsIgnoreCase(uwsParam))
				return uwsParam;
		}

		for(String uwsParam : UWS_RO_PARAMETERS){
			if (name.equalsIgnoreCase(uwsParam))
				return null;
		}

		if (expectedAdditionalParams != null){
			for(String param : expectedAdditionalParams){
				if (name.equalsIgnoreCase(param))
					return param;
			}
		}

		return name;
	}

	/**
	 * Tells whether there is a value for the {@link UWSJob#PARAM_PHASE PHASE} parameter.
	 * @return	<i>true</i> if the {@link UWSJob#PARAM_PHASE PHASE} has a value, <i>false</i> otherwise.
	 */
	public final boolean hasInputPhase(){
		return params.get(UWSJob.PARAM_PHASE) != null;
	}

	/**
	 * <p>Gets the value of the parameter {@link UWSJob#PARAM_PHASE phase}.</p>
	 * 
	 * <p><i><u>note:</u> This parameter is removed from the parameters list after the call of this function !</i></p>
	 * 
	 * @return	The given phase.
	 */
	public final String getInputPhase(){
		synchronized(params){
			if (hasInputPhase())
				return params.remove(UWSJob.PARAM_PHASE).toString();
			else
				return null;
		}
	}

	/**
	 * Gets the value of the parameter {@link UWSJob#PARAM_RUN_ID runId}.
	 * @return	The given {@link UWSJob#PARAM_RUN_ID runId} or <i>null</i> if this parameter has no value.
	 */
	public final String getRunId(){
		return (params.get(UWSJob.PARAM_RUN_ID) != null) ? params.get(UWSJob.PARAM_RUN_ID).toString() : null;
	}

	/**
	 * Gets the value of the parameter {@link UWSJob#PARAM_EXECUTION_DURATION executionDuration}.
	 * @return	The given {@link UWSJob#PARAM_EXECUTION_DURATION executionDuration} or {@link UWSJob#UNLIMITED_DURATION} if this parameter has no value.
	 */
	public final long getExecutionDuration(){
		Object value = params.get(UWSJob.PARAM_EXECUTION_DURATION);
		if (value != null){
			if (value instanceof Long)
				return (Long)value;
			else if (value instanceof String){
				try{
					Long duration = Long.parseLong((String)value);
					synchronized(params){
						params.put(UWSJob.PARAM_EXECUTION_DURATION, duration);
					}
					return duration;
				}catch(NumberFormatException nfe){ ; }
			}
		}
		return UWSJob.UNLIMITED_DURATION;
	}

	/**
	 * Gets the value of the parameter {@link UWSJob#PARAM_DESTRUCTION_TIME destruction}.
	 * @return	The given {@link UWSJob#PARAM_DESTRUCTION_TIME destruction} or <i>null</i> if this parameter has no value.
	 */
	public final Date getDestructionTime(){
		Object value = params.get(UWSJob.PARAM_DESTRUCTION_TIME);
		if (value != null){
			if (value instanceof Date)
				return (Date)value;
			else if (value instanceof String){
				try{
					Date destruction = UWSJob.dateFormat.parse((String)value);
					synchronized(params){
						params.put(UWSJob.PARAM_DESTRUCTION_TIME, destruction);
					}
					return destruction;
				}catch(ParseException pe){ ; }
			}
		}
		return null;
	}

	/**
	 * <p>Gets the list of all UWS additional parameters (all these parameters are associated with the parameter name {@link UWSJob#PARAM_PARAMETERS}).</p>
	 * <p><b><u>WARNING:</u> The result of this function MUST BE used ONLY in READ-ONLY.
	 * Any modification applied to the returned map will NEVER be propagated to this instance of {@link UWSParameters} !
	 * <u>You should rather use {@link #set(String, Object)} to add/update/remove an additional parameters !</u></b></p>
	 * 
	 * @return	All the UWS additional parameters.
	 */
	public final Map<String, Object> getAdditionalParameters(){
		if (additionalParams == null){
			synchronized(params){
				additionalParams = new HashMap<String, Object>(params.size());
				for(Entry<String, Object> entry : params.entrySet()){
					boolean uwsParam = false;
					for(int i=0; !uwsParam && i<UWS_RW_PARAMETERS.length; i++)
						uwsParam = entry.getKey().equals(UWS_RW_PARAMETERS[i]);
					for(int i=0; !uwsParam && i<UWS_RO_PARAMETERS.length; i++)
						uwsParam = entry.getKey().equals(UWS_RO_PARAMETERS[i]);
					if (!uwsParam)
						additionalParams.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return additionalParams;
	}

}
