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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.jobInfo.JobInfo;
import uws.job.parameters.DestructionTimeController;
import uws.job.parameters.DestructionTimeController.DateField;
import uws.job.parameters.ExecutionDurationController;
import uws.job.parameters.InputParamController;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.file.UWSFileManager;
import uws.service.request.RequestParser;
import uws.service.request.UWSRequestParser;

/**
 * Abstract implementation of {@link UWSFactory}.
 * Only the function which creates a {@link JobThread} from a {@link UWSJob}
 * needs to be implemented.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (09/2017)
 */
public abstract class AbstractUWSFactory implements UWSFactory {

	/** List the controllers of all the input parameters. See {@link UWSParameters} and {@link InputParamController} for more details. */
	protected final HashMap<String,InputParamController> inputParamControllers = new HashMap<String,InputParamController>(10);

	/** List of all expected additional parameters. */
	protected final ArrayList<String> expectedAdditionalParams = new ArrayList<String>(10);

	public AbstractUWSFactory(){
		;
	}

	/**
	 * Builds a factory with a list of the name of all expected additional parameters.
	 * These parameters will be identified by {@link UWSParameters} without taking into account their case
	 * and they will stored with the case of their name in the given list.
	 *
	 * @param expectedAdditionalParams	Names of the expected parameters.
	 */
	public AbstractUWSFactory(final String[] expectedAdditionalParams){
		if (expectedAdditionalParams != null){
			for(String p : expectedAdditionalParams){
				if (p != null && !p.trim().isEmpty())
					this.expectedAdditionalParams.add(p);
			}
		}
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */

	@Override
	public UWSJob createJob(final HttpServletRequest request, final JobOwner user) throws UWSException{
		// Extract the HTTP request ID (the job ID should be the same, if not already used by another job):
		String requestID = null;
		if (request != null && request.getAttribute(UWS.REQ_ATTRIBUTE_ID) != null && request.getAttribute(UWS.REQ_ATTRIBUTE_ID) instanceof String)
			requestID = request.getAttribute(UWS.REQ_ATTRIBUTE_ID).toString();

		// Create the job:
		UWSJob newJob = new UWSJob(user, createUWSParameters(request), requestID);

		// Set the XML job description if any:
		Object jobDesc = request.getAttribute(UWS.REQ_ATTRIBUTE_JOB_DESCRIPTION);
		if (jobDesc != null && jobDesc instanceof JobInfo)
			newJob.setJobInfo((JobInfo)jobDesc);

		return newJob;
	}

	@Override
	public UWSJob createJob(String jobID, long creationTime, JobOwner owner, UWSParameters params, long quote, long startTime, long endTime, List<Result> results, ErrorSummary error) throws UWSException{
		return new UWSJob(jobID, creationTime, owner, params, quote, startTime, endTime, results, error);
	}

	@Override
	public UWSParameters createUWSParameters(final Map<String,Object> params) throws UWSException{
		return new UWSParameters(params, expectedAdditionalParams, inputParamControllers);
	}

	@Override
	public UWSParameters createUWSParameters(final HttpServletRequest req) throws UWSException{
		return new UWSParameters(req, expectedAdditionalParams, inputParamControllers);
	}

	@Override
	public RequestParser createRequestParser(final UWSFileManager fileManager) throws UWSException{
		return new UWSRequestParser(fileManager);
	}

	/**
	 * Adds the name of an additional parameter which must be identified without taking into account its case
	 * and then stored with the case of the given name.
	 *
	 * @param paramName		Name of an additional parameter.
	 */
	public final void addExpectedAdditionalParameter(final String paramName){
		if (paramName != null && !paramName.trim().isEmpty())
			expectedAdditionalParams.add(paramName);
	}

	/**
	 * Gets the number of additional parameters which must be identified with no case sensitivity.
	 *
	 * @return	Number of expected additional parameters.
	 */
	public final int getNbExpectedAdditionalParameters(){
		return expectedAdditionalParams.size();
	}

	/**
	 * Gets the names of the expected additional parameters. These parameters are identified with no case sensitivity
	 * and stored in the given case.
	 *
	 * @return	Names of the expected additional parameters.
	 */
	public final List<String> getExpectedAdditionalParameters(){
		return expectedAdditionalParams;
	}

	/**
	 * Gets an iterator on the names of the expected additional parameters.
	 *
	 * @return	An iterator on the names of the expected additional parameters.
	 */
	public final Iterator<String> expectedAdditionalParametersIterator(){
		return expectedAdditionalParams.iterator();
	}

	/**
	 * Removes the name of an expected additional parameter.
	 * This parameter will never be identify specifically and so, it will be stored in the same case as
	 * in the initial Map or HttpServletRequest.
	 *
	 * @param paramName	Name of an additional parameter.
	 */
	public final void removeExpectedAdditionalParam(final String paramName){
		if (paramName != null && !paramName.trim().isEmpty())
			expectedAdditionalParams.remove(paramName);
	}

	/**
	 * Gets the list of all UWS input parameter controllers.
	 * @return	All parameter controllers.
	 */
	public final Map<String,InputParamController> getInputParamControllers(){
		return inputParamControllers;
	}

	/**
	 * Gets an iterator on the list of all UWS input parameter controllers.
	 * @return	An iterator on all parameter controllers.
	 */
	public final Iterator<Map.Entry<String,InputParamController>> getInputParamControllersIterator(){
		return inputParamControllers.entrySet().iterator();
	}

	/**
	 * Gets the controller of the specified UWS input parameter.
	 * @param inputParamName	Name of the parameter whose the controller must be returned.
	 * @return					The corresponding controller or <i>null</i> if there is none.
	 */
	public final InputParamController getInputParamController(final String inputParamName){
		return (inputParamName == null) ? null : inputParamControllers.get(inputParamName);
	}

	/**
	 * Sets the controller of the specified input UWS job parameter.
	 *
	 * @param paramName		Name of the parameter with which the given controller will be associated.
	 * @param controller	An input parameter controller.
	 *
	 * @return				The former controller associated with the specified parameter
	 * 						or <i>null</i> if there is no controller before this call
	 * 						or if the given parameter name is <i>null</i> or an empty string.
	 */
	public final InputParamController setInputParamController(final String paramName, final InputParamController controller){
		if (paramName == null || paramName.trim().isEmpty())
			return null;
		if (controller == null)
			return inputParamControllers.remove(paramName);
		else
			return inputParamControllers.put(paramName, controller);
	}

	/**
	 * Removes the controller of the specified input UWS job parameter.
	 *
	 * @param paramName	Name of the parameter whose the controller must be removed.
	 *
	 * @return	The removed controller
	 * 			or <i>null</i> if there were no controller
	 * 			or if the given name is <i>null</i> or an empty string.
	 */
	public final InputParamController removeInputParamController(final String paramName){
		return (paramName == null) ? null : inputParamControllers.remove(paramName);
	}

	/**
	 * <p>Lets configuring the execution duration default and maximum value.</p>
	 *
	 * <p><i><u>note:</u> A new controller is created if needed.
	 * Otherwise the current one (if it is an instance of {@link DestructionTimeController}) is updated.</i></p>
	 *
	 * @param defaultDuration	Default duration between the start and the end of the execution of a job.
	 * @param maxDuration		Maximum duration between the start and the end of the execution of a job that a user can set when creating/initializing a job.
	 * @param allowModif		<i>true</i> to allow the modification of this parameter after its initialization, <i>false</i> otherwise.
	 *
	 * @see ExecutionDurationController
	 */
	public final void configureExecution(final long defaultDuration, final long maxDuration, final boolean allowModif){
		InputParamController controller = inputParamControllers.get(UWSJob.PARAM_EXECUTION_DURATION);

		// Configures the controller:
		if (controller != null && controller instanceof ExecutionDurationController){
			ExecutionDurationController durationController = (ExecutionDurationController)controller;
			durationController.setMaxExecutionDuration(maxDuration);
			durationController.setDefaultExecutionDuration(defaultDuration);
			durationController.allowModification(allowModif);

		}// Or creates a new one, if it does not exist:
		else
			inputParamControllers.put(UWSJob.PARAM_EXECUTION_DURATION, new ExecutionDurationController(defaultDuration, maxDuration, allowModif));
	}

	/**
	 * <p>Lets configuring the destruction time default and maximum value.</p>
	 *
	 * <p><i><u>note:</u> A new controller is created if needed.
	 * Otherwise the current one (if it is an instance of {@link ExecutionDurationController}) is updated.</i></p>
	 *
	 * @param defaultTime		Default time since the job creation and its destruction.
	 * @param defaultTimeUnit	Unit of the default time (i.e. minutes, days, ...).
	 * @param maxTime			Maximum time since the job creation and its destruction that a user can set when creating/initializing a job.
	 * @param maxTimeUnit		Unit of the maximum time (i.e. minutes, days, ...).
	 * @param allowModif		<i>true</i> to allow the modification of this parameter after its initialization, <i>false</i> otherwise.
	 *
	 * @see DestructionTimeController
	 */
	public final void configureDestruction(final int defaultTime, final DateField defaultTimeUnit, final int maxTime, final DateField maxTimeUnit, final boolean allowModif){
		InputParamController controller = inputParamControllers.get(UWSJob.PARAM_DESTRUCTION_TIME);

		// Cast the controller or built a new DestructionTimeController, if it does not exist:
		DestructionTimeController destructionController;
		if (controller == null || !(controller instanceof DestructionTimeController)){
			destructionController = new DestructionTimeController();
			inputParamControllers.put(UWSJob.PARAM_DESTRUCTION_TIME, destructionController);
		}else
			destructionController = (DestructionTimeController)controller;

		// Configure the controller:
		destructionController.setMaxDestructionInterval(maxTime, maxTimeUnit);
		destructionController.setDefaultDestructionInterval(defaultTime, defaultTimeUnit);
		destructionController.allowModification(allowModif);
	}

}
