package uws.config;

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
 * Copyright 2016 - Astronomisches Rechen Institut (ARI)
 */

import static uws.config.UWSConfiguration.KEY_REGEXP_MAX_DESTRUCTION_INTERVAL;
import static uws.config.UWSConfiguration.KEY_REQUEST_PARSER;
import static uws.config.UWSConfiguration.REGEXP_DEFAULT_DESTRUCTION_INTERVAL;
import static uws.config.UWSConfiguration.REGEXP_DEFAULT_EXEC_DURATION;
import static uws.config.UWSConfiguration.REGEXP_JOB_THREAD;
import static uws.config.UWSConfiguration.REGEXP_MAX_EXEC_DURATION;
import static uws.config.UWSConfiguration.REGEXP_PARAMETERS;
import static uws.config.UWSConfiguration.extractJobListName;
import static uws.config.UWSConfiguration.fetchConstructor;
import static uws.config.UWSConfiguration.getProperty;
import static uws.config.UWSConfiguration.newInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.parameters.DestructionTimeController;
import uws.job.parameters.DurationParamController;
import uws.job.parameters.ExecutionDurationController;
import uws.job.parameters.InputParamController;
import uws.job.parameters.NumericParamController;
import uws.job.parameters.StringParamController;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSFactory;
import uws.service.UWSUrl;
import uws.service.file.UWSFileManager;
import uws.service.request.RequestParser;
import uws.service.request.UWSRequestParser;

/**
 * Concrete implementation of a {@link UWSFactory} which is parameterized by a UWS configuration file.
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.2 (09/2016)
 * @since 4.2
 */
public class ConfigurableUWSFactory implements UWSFactory {

	/** Constructor of the {@link RequestParser} to create for the whole UWS service.
	 * <p><i>If <code>null</code>, the default UWS request parser (i.e. {@link UWSRequestParser}) will be used.</i></p> */
	protected Constructor<? extends RequestParser> constructorRequestParser = null;

	/** Map associating the name of a job list with the constructor of the class of the jobs to create inside this job list. */
	protected Map<String,Constructor<? extends JobThread>> jobThreads = new HashMap<String,Constructor<? extends JobThread>>(2);

	/** List of parameters (and eventually how to control them) for each job-list/job. */
	protected Map<String,Map<String,InputParamController>> jobParams = new HashMap<String,Map<String,InputParamController>>(2);

	/** Create an empty factory.
	 * 
	 * <p>
	 * 	Using this constructor, no {@link JobThread} can be created.
	 * 	You have to add some in {@link #jobThreads} for each job list you have to support.
	 * 	The parameters of each job/job-list can be listed in {@link #jobParams}.
	 * </p>
	 */
	protected ConfigurableUWSFactory(){}

	/**
	 * Create and initialize a UWS factory thanks to properties coming from a UWS configuration file.
	 * 
	 * <p>The following properties are supported by this constructor:</p>
	 * <ul>
	 * 	<li>{@link UWSConfiguration#KEY_REQUEST_PARSER  KEY_REQUEST_PARSER}</li>
	 * 	<li>{@link UWSConfiguration#REGEXP_JOB_THREAD  KEY_REGEXP_JOB_THREAD}</li>
	 * 	<li>{@link UWSConfiguration#REGEXP_PARAMETERS  KEY_REGEXP_PARAMETERS}</li>
	 * 	<li>{@link UWSConfiguration#REGEXP_DEFAULT_EXEC_DURATION  KEY_REGEXP_DEFAULT_EXEC_DURATION}</li>
	 * 	<li>{@link UWSConfiguration#REGEXP_MAX_EXEC_DURATION  KEY_REGEXP_MAX_EXEC_DURATION}</li>
	 * 	<li>{@link UWSConfiguration#REGEXP_DEFAULT_DESTRUCTION_INTERVAL  KEY_REGEXP_DEFAULT_DESTRUCTION_INTERVAL}</li>
	 * 	<li>{@link UWSConfiguration#KEY_REGEXP_MAX_DESTRUCTION_INTERVAL  KEY_REGEXP_MAX_DESTRUCTION_INTERVAL}</li>
	 * </ul>
	 * 
	 * @param uwsConfig	Configuration of this factory.
	 * 
	 * @throws UWSException	If the initialization fails.
	 */
	public ConfigurableUWSFactory(final Properties uwsConfig) throws UWSException{

		/* 1. Extract the RequestParser */

		String propValue = getProperty(uwsConfig, KEY_REQUEST_PARSER);
		if (propValue != null)
			constructorRequestParser = fetchConstructor(propValue, KEY_REQUEST_PARSER, RequestParser.class, new Class<?>[]{UWSFileManager.class});

		/* 2. Extract information (job thread + job parameters + exec duration + destruction time) about all job lists */

		String propName, jlName = null;
		DurationParamController durationController = new DurationParamController();

		@SuppressWarnings("unchecked")
		Enumeration<String> propNames = (Enumeration<String>)uwsConfig.propertyNames();
		while(propNames.hasMoreElements()){

			propName = propNames.nextElement();

			// Set job thread:
			if (propName.matches(REGEXP_JOB_THREAD)){
				jlName = extractJobListName(propName);
				propValue = getProperty(uwsConfig, propName);

				jobThreads.put(jlName, fetchConstructor(propValue, propName, JobThread.class, new Class<?>[]{UWSJob.class}));

			}
			// Set expected job parameters:
			else if (propName.matches(REGEXP_PARAMETERS)){
				jlName = extractJobListName(propName);
				propValue = getProperty(uwsConfig, propName);

				initParameters(jlName, propValue);

			}
			// Set the default execution duration:
			else if (propName.matches(REGEXP_DEFAULT_EXEC_DURATION)){
				jlName = extractJobListName(propName);
				propValue = getProperty(uwsConfig, propName);

				try{
					// Get map of all controllers for this job list:
					Map<String,InputParamController> mapControllers;
					if (jobParams.containsKey(jlName))
						mapControllers = jobParams.get(jlName);
					else
						mapControllers = new HashMap<String,InputParamController>();

					// Get its execution duration controller:
					ExecutionDurationController controller;
					if (mapControllers.get(UWSJob.PARAM_EXECUTION_DURATION) != null && mapControllers.get(UWSJob.PARAM_EXECUTION_DURATION) instanceof ExecutionDurationController)
						controller = (ExecutionDurationController)mapControllers.get(UWSJob.PARAM_EXECUTION_DURATION);
					else
						controller = new ExecutionDurationController();

					// Set the default execution duration:
					controller.setDefaultExecutionDuration(durationController.parseDuration(propValue) / 1000); // parseDuration(...) returns a duration in ms while executionDuration must be in seconds

					// Update the map of controllers for this job list:
					mapControllers.put(UWSJob.PARAM_EXECUTION_DURATION, controller);

					// Update the map of all job lists' controllers:
					jobParams.put(jlName, mapControllers);

				}catch(NumberFormatException nfe){
					throw new UWSException("Wrong numeric format for the default execution duration: \"" + propValue + "\"!");
				}catch(ParseException pe){
					throw new UWSException("Incorrect syntax for the default execution duration! Cause: " + pe.getMessage());
				}

			}
			// Set the maximum execution duration:
			else if (propName.matches(REGEXP_MAX_EXEC_DURATION)){
				jlName = extractJobListName(propName);
				propValue = getProperty(uwsConfig, propName);

				try{
					// Get map of all controllers for this job list:
					Map<String,InputParamController> mapControllers;
					if (jobParams.containsKey(jlName))
						mapControllers = jobParams.get(jlName);
					else
						mapControllers = new HashMap<String,InputParamController>();

					// Get its execution duration controller:
					ExecutionDurationController controller;
					if (mapControllers.get(UWSJob.PARAM_EXECUTION_DURATION) != null && mapControllers.get(UWSJob.PARAM_EXECUTION_DURATION) instanceof ExecutionDurationController)
						controller = (ExecutionDurationController)mapControllers.get(UWSJob.PARAM_EXECUTION_DURATION);
					else
						controller = new ExecutionDurationController();

					// Set the maximum execution duration:
					controller.setMaxExecutionDuration(durationController.parseDuration(propValue) / 1000); // parseDuration(...) returns a duration in ms while executionDuration must be in seconds

					// Update the map of controllers for this job list:
					mapControllers.put(UWSJob.PARAM_EXECUTION_DURATION, controller);

					// Update the map of all job lists' controllers:
					jobParams.put(jlName, mapControllers);

				}catch(NumberFormatException nfe){
					throw new UWSException("Wrong numeric format for the maximum execution duration: \"" + propValue + "\"!");
				}catch(ParseException pe){
					throw new UWSException("Incorrect syntax for the maximum execution duration! Cause: " + pe.getMessage());
				}

			}
			// Set the default destruction time:
			else if (propName.matches(REGEXP_DEFAULT_DESTRUCTION_INTERVAL)){
				jlName = extractJobListName(propName);
				propValue = getProperty(uwsConfig, propName);

				try{
					// Get map of all controllers for this job list:
					Map<String,InputParamController> mapControllers;
					if (jobParams.containsKey(jlName))
						mapControllers = jobParams.get(jlName);
					else
						mapControllers = new HashMap<String,InputParamController>();

					// Get its destruction time controller:
					DestructionTimeController controller;
					if (mapControllers.get(UWSJob.PARAM_DESTRUCTION_TIME) != null && mapControllers.get(UWSJob.PARAM_DESTRUCTION_TIME) instanceof DestructionTimeController)
						controller = (DestructionTimeController)mapControllers.get(UWSJob.PARAM_DESTRUCTION_TIME);
					else
						controller = new DestructionTimeController();

					// Set the default destruction time:
					controller.setDefaultDestructionInterval((int)durationController.parseDuration(propValue));

					// Update the map of controllers for this job list:
					mapControllers.put(UWSJob.PARAM_DESTRUCTION_TIME, controller);

					// Update the map of all job lists' controllers:
					jobParams.put(jlName, mapControllers);

				}catch(NumberFormatException nfe){
					throw new UWSException("Wrong numeric format for the default destruction time: \"" + propValue + "\"!");
				}catch(ParseException pe){
					throw new UWSException("Incorrect syntax for the default destruction time! Cause: " + pe.getMessage());
				}

			}
			// Set the maximum destruction time:
			else if (propName.matches(KEY_REGEXP_MAX_DESTRUCTION_INTERVAL)){
				jlName = extractJobListName(propName);
				propValue = getProperty(uwsConfig, propName);

				try{
					// Get map of all controllers for this job list:
					Map<String,InputParamController> mapControllers;
					if (jobParams.containsKey(jlName))
						mapControllers = jobParams.get(jlName);
					else
						mapControllers = new HashMap<String,InputParamController>();

					// Get its destruction time controller:
					DestructionTimeController controller;
					if (mapControllers.get(UWSJob.PARAM_DESTRUCTION_TIME) != null && mapControllers.get(UWSJob.PARAM_DESTRUCTION_TIME) instanceof DestructionTimeController)
						controller = (DestructionTimeController)mapControllers.get(UWSJob.PARAM_DESTRUCTION_TIME);
					else
						controller = new DestructionTimeController();

					// Set the maximum destruction time:
					controller.setMaxDestructionInterval((int)durationController.parseDuration(propValue));

					// Update the map of controllers for this job list:
					mapControllers.put(UWSJob.PARAM_DESTRUCTION_TIME, controller);

					// Update the map of all job lists' controllers:
					jobParams.put(jlName, mapControllers);

				}catch(NumberFormatException nfe){
					throw new UWSException("Wrong numeric format for the maximum destruction time: \"" + propValue + "\"!");
				}catch(ParseException pe){
					throw new UWSException("Incorrect syntax for the maximum destruction time! Cause: " + pe.getMessage());
				}

			}
		}
	}

	/**
	 * Regular Expression of a job parameters list.
	 * 
	 * <p>The following parameter types are supported:</p>
	 * <ul>
	 * 	<li><em>Just a parameter name (i.e. no controller):</em>
	 * 		<pre>\s*([^,\[\]]*)\s*(,(.*))?</pre>
	 * 		<p>Example: <code>param1</code></p>
	 * 		<ul>
	 * 			<li><em>group(16) = </em>parameter name</li>
	 * 			<li><em>group(18) = </em>all other parameters (without the leading comma)</li>
	 * 		</ul>
	 * 	</li>
	 * 	<li><em>A parameter name and the class of an {@link InputParamController}:</em>
	 * 		<pre>\s*\[([^,]+),(\{[^\}]*\})\s*\]\s*(,(.*))?</pre>
	 * 		<p>Example: <code>[param1, {aPackage.MyCustomController}]</code></p>
	 * 		<ul>
	 * 			<li><em>group(2) = </em>parameter name</li>
	 * 			<li><em>group(4) = </em>controller class name</li>
	 * 		</ul>
	 * 	</li>
	 * 	<li><em>A parameter name and description of a string controller:</em>
	 * 		<pre>\s*\[([^,]+),([^,]*),\s*(string)\s*,\s*"([^,]*)"\s*,\s* /(([^\/]|//)*)/(i)?\s*\]\s*(,(.*))?</pre>
	 * 		<p>Example: <code>[param1, true, string, "default", /mySuperRegexp/i]</code></p>
	 * 		<ul>
	 * 			<li><em>group(2) = </em>parameter name</li>
	 * 			<li><em>group(5) = </em>a boolean expression (yes, y, true, t) to indicate whether the parameter can be modified by the user</li>
	 * 			<li><em>group(11) = </em><code>string</code></li>
	 * 			<li><em>group(12) = </em>default value (it must be provided between brackets as in the example above)</li>
	 * 			<li><em>group(13) = </em>the regular expression</li>
	 * 			<li><em>group(15) = </em><code>i</code> if the regular expression must be evaluated in a case insensitive manner</li>
	 * 		</ul>
	 * 	</li>
	 * 	<li><em>A parameter name and description of a numeric controller:</em>
	 * 		<pre>\s*\[([^,]+),([^,]*),\s*(numeric)\s*,([^,]*),([^,]*),([^,]*)\s*\]\s*(,(.*))?</pre>
	 * 		<p>Example: <code>[param1, true, numeric, 10, 0, 20]</code></p>
	 * 		<ul>
	 * 			<li><em>group(2) = </em>parameter name</li>
	 * 			<li><em>group(5) = </em>a boolean expression (yes, y, true, t) to indicate whether the parameter can be modified by the user</li>
	 * 			<li><em>group(7) = </em><code>numeric</code></li>
	 * 			<li><em>group(8) = </em>default value ; only a numeric value (floating number or not ; negative or not) will be allowed after the expression has been parsed</li>
	 * 			<li><em>group(9) = </em>minimum value ; only a numeric value (floating number or not ; negative or not) will be allowed after the expression has been parsed</li>
	 * 			<li><em>group(10) = </em>maximum value ; only a numeric value (floating number or not ; negative or not) will be allowed after the expression has been parsed</li>
	 * 		</ul>
	 * 	</li>
	 * 	<li><em>A parameter name and description of a duration controller:</em>
	 * 		<pre>\s*\[([^,]+),([^,]*),\s*(duration)\s*,([^,]*),([^,]*),([^,]*)\s*\]\s*(,(.*))?</pre>
	 * 		<p>Example: <code>[param1, true, numeric, 10, 0, 20]</code></p>
	 * 		<ul>
	 * 			<li><em>group(2) = </em>parameter name</li>
	 * 			<li><em>group(5) = </em>a boolean expression (yes, y, true, t) to indicate whether the parameter can be modified by the user</li>
	 * 			<li><em>group(7) = </em><code>numeric</code></li>
	 * 			<li><em>group(8) = </em>default value</li>
	 * 			<li><em>group(9) = </em>minimum value</li>
	 * 			<li><em>group(10) = </em>maximum value</li>
	 * 		</ul>
	 * 		<p>The default, minimum and maximum value must a positive integer value followed eventually by a unit. If no unit is specified, the default is <code>milliseconds</code>.
	 * 			See below for all supported units:</p>
	 * 		<ul>
	 * 			<li>milliseconds, ms</li>
	 * 			<li>seconds, sec, s</li>
	 * 			<li>minutes, min, m</li>
	 * 			<li>hours, h</li>
	 * 			<li>days, D</li>
	 * 			<li>weeks, W</li>
	 * 			<li>months, M</li>
	 * 			<li>years, Y</li>
	 * 		</ul>
	 * 	</li>
	 * </ul>
	 */
	protected static final Pattern PATTERN_PARAMETER = Pattern.compile("\\s*(\\[([^,]+),(\\s*(\\{[^\\}]*\\})|([^,]*),(\\s*(numeric|duration)\\s*,([^,]*),([^,]*),([^,]*)|\\s*(string)\\s*,\\s*\"([^\"]*)\"\\s*,\\s*/(([^/]|//)*)/(i)?))\\s*\\]|([^,\\[\\]]*))\\s*(,(.*))?", Pattern.CASE_INSENSITIVE);

	/**
	 * Parse the given list of parameters + their limits/controller and update the list of parameters for the specified job list.
	 * 
	 * <p>
	 * 	The given string is parsed using {@link #PATTERN_PARAMETER}. The expected syntax of one item of this list is explained
	 * 	in the javadoc of {@link #PATTERN_PARAMETER}.
	 * </p>
	 * 
	 * <p>
	 * 	If just a parameter name is given, it is anyway added to the map of parameters of the specified job list, but with a <code>null</code>
	 * 	{@link InputParamController}. The idea is to indicate the parameter is expected with this name, but there is no specific restriction on its value.
	 * </p>
	 * 
	 * @param jlName		Name of the job list for which the given list of parameters is expected.
	 * @param propValue		String expressing the list of expected parameters (and eventually their limits/controller).
	 * 
	 * @throws UWSException	If the syntax of a parameter description is incorrect.
	 */
	protected void initParameters(final String jlName, final String propValue) throws UWSException{
		// Get the existing list of parameters, if any;
		/* Note: it is possible if the same property key is found more than once in a property file.
		 *       In this case, the second list of parameters should update the first one. */
		Map<String,InputParamController> jlParameters = jobParams.get(jlName);

		// If no list of controllers exists, create an empty one:
		if (jlParameters == null)
			jlParameters = new HashMap<String,InputParamController>();

		int indParameter = 1;

		Matcher matcher;
		String remaining = propValue;
		String paramName, temp;
		boolean modif = true;
		while(remaining != null && remaining.trim().length() > 0){
			// Apply the regular expression:
			matcher = PATTERN_PARAMETER.matcher(remaining);
			if (!matcher.matches())
				throw new UWSException("Incorrect parameter list syntax from the parameter N°" + indParameter + ": " + remaining);

			// CASE: no controller specified
			if (matcher.group(16) != null){

				// Fetch the parameter name:
				paramName = matcher.group(16).trim();

				// Check the name:
				/* a parameter name must NOT be empty */
				if (paramName.length() > 0){
					/* a parameter name must NOT contain any space character */
					if (!paramName.replaceFirst("\\s", "_").equals(paramName))
						throw new UWSException("Incorrect syntax for the parameter name \"" + paramName + "\"! Space characters are forbidden.");
					else
						jlParameters.put(paramName, null);
				}
			}else{

				// Fetch the parameter name:
				paramName = matcher.group(2).trim();

				// Check the name:
				/* a parameter name must NOT be empty */
				if (paramName.length() == 0)
					throw new UWSException("Missing name for the parameter N°" + indParameter + "!");
				/* a parameter name must NOT contain any space character */
				else if (!paramName.replaceFirst("\\s", "_").equals(paramName))
					throw new UWSException("Incorrect syntax for the parameter name \"" + paramName + "\"! Space characters are forbidden.");

				// CASE: CUSTOM CONTROLLER
				if (matcher.group(4) != null){
					jlParameters.put(paramName, newInstance(matcher.group(4), jlName + "." + UWSConfiguration.KEY_PARAMETERS, InputParamController.class, new Class<?>[0], new Object[0]));
				}
				// CASE: STRING/NUMERIC/DURATION
				else{
					// Fetch the modification flag:
					temp = matcher.group(5).trim();
					if (temp.length() == 0 || temp.matches("(?i)(true|t|yes|y)"))
						modif = true;
					else
						modif = false;

					// CASE: STRING
					if (matcher.group(11) != null){

						// Create the controller:
						StringParamController controller = new StringParamController(paramName);

						// Set its modification flag:
						controller.allowModification(modif);

						// Set its default value, if any:
						if (matcher.group(12).length() > 0)
							controller.setDefaultValue(matcher.group(12));

						// Set the regular expression:
						if (matcher.group(13).length() > 0)
							controller.setRegExp((matcher.group(15) != null ? "(?i)" : "") + matcher.group(13));

						// Add the controller:
						jlParameters.put(paramName, controller);

					}

					// CASE: NUMERIC/DURATION
					else if (matcher.group(7) != null){

						// CASE: NUMERIC
						if (matcher.group(7).trim().equalsIgnoreCase("numeric")){

							// Create the controller:
							NumericParamController controller = new NumericParamController();

							// Set its modification flag:
							controller.allowModification(modif);

							// Set the default value:
							if (matcher.group(8).trim().length() > 0){
								try{
									controller.setDefault(Double.parseDouble(matcher.group(8).trim()));
								}catch(NumberFormatException nfe){
									throw new UWSException("Wrong numeric format for the default value of the parameter \"" + paramName + "\": \"" + matcher.group(8).trim() + "\"!");
								}
							}

							// Set the minimum value:
							if (matcher.group(9).trim().length() > 0){
								try{
									controller.setMinimum(Double.parseDouble(matcher.group(9).trim()));
								}catch(NumberFormatException nfe){
									throw new UWSException("Wrong numeric format for the minimum value of the parameter \"" + paramName + "\": \"" + matcher.group(9).trim() + "\"!");
								}
							}

							// Set the maximum value:
							if (matcher.group(10).trim().length() > 0){
								try{
									controller.setMaximum(Double.parseDouble(matcher.group(10).trim()));
								}catch(NumberFormatException nfe){
									throw new UWSException("Wrong numeric format for the maximum value of the parameter \"" + paramName + "\": \"" + matcher.group(10).trim() + "\"!");
								}
							}

							// Add the controller:
							jlParameters.put(paramName, controller);

						}
						// CASE: DURATION
						else{

							// Create the controller:
							DurationParamController controller = new DurationParamController();

							// Set its modification flag:
							controller.allowModification(modif);

							// Set the default value:
							if (matcher.group(8).trim().length() > 0){
								try{
									controller.setDefault(controller.parseDuration(matcher.group(8).trim()));
								}catch(NumberFormatException nfe){
									throw new UWSException("Wrong numeric format for the default value of the parameter \"" + paramName + "\": \"" + matcher.group(8).trim() + "\"!");
								}catch(ParseException pe){
									throw new UWSException("Incorrect syntax for the default duration of the parameter \"" + paramName + "\"! Cause: " + pe.getMessage());
								}
							}

							// Set the minimum value:
							if (matcher.group(9).trim().length() > 0){
								try{
									controller.setMinimum(controller.parseDuration(matcher.group(9).trim()));
								}catch(NumberFormatException nfe){
									throw new UWSException("Wrong numeric format for the minimum value of the parameter \"" + paramName + "\": \"" + matcher.group(9).trim() + "\"!");
								}catch(ParseException pe){
									throw new UWSException("Incorrect syntax for the minimu duration of the parameter \"" + paramName + "\"! Cause: " + pe.getMessage());
								}
							}

							// Set the maximum value:
							if (matcher.group(10).trim().length() > 0){
								try{
									controller.setMaximum(controller.parseDuration(matcher.group(10).trim()));
								}catch(NumberFormatException nfe){
									throw new UWSException("Wrong numeric format for the maximum value of the parameter \"" + paramName + "\": \"" + matcher.group(10).trim() + "\"!");
								}catch(ParseException pe){
									throw new UWSException("Incorrect syntax for the maximum duration of the parameter \"" + paramName + "\"! Cause: " + pe.getMessage());
								}
							}

							// Add the controller:
							jlParameters.put(paramName, controller);
						}
					}
				}
			}

			// Read the other parameters:
			remaining = matcher.group(18);
			indParameter++;
			modif = true;
		}

		// Add this new list of parameters:
		if (jlParameters.size() > 0)
			jobParams.put(jlName, jlParameters);
	}

	@Override
	public JobThread createJobThread(final UWSJob jobDescription) throws UWSException{
		// No job => No thread ^^
		if (jobDescription == null)
			return null;
		// If the job is not inside a job list, it is impossible to know which instance of JobThread to create => Error!
		else if (jobDescription.getJobList() == null)
			throw new UWSException("Job without job list! Impossible to create a thread for the job \"" + jobDescription.getJobId() + "\".");

		// Extract the job list name:
		String jlName = jobDescription.getJobList().getName();

		// If no class is associated with this job list name, no JobThread can be created => Error!
		if (jlName == null || jobThreads.get(jlName) == null)
			throw new UWSException("No thread associated with jobs of the job list \"" + jlName + "\"! Impossible to create a thread for the job \"" + jobDescription.getJobId() + "\".");

		// Get the constructor:
		Constructor<? extends JobThread> threadConstructor = jobThreads.get(jlName);
		try{

			// Create the JobThread:
			return threadConstructor.newInstance(jobDescription);

		}catch(InstantiationException ie){
			throw new UWSException("Impossible to create an instance of an abstract class: \"" + threadConstructor.getDeclaringClass().getName() + "\"!");

		}catch(InvocationTargetException ite){
			if (ite.getCause() != null){
				if (ite.getCause() instanceof UWSException)
					throw (UWSException)ite.getCause();
				else
					throw new UWSException(ite.getCause());
			}else
				throw new UWSException(ite);

		}catch(Exception ex){
			throw new UWSException(UWSException.NOT_FOUND, ex, "Impossible to create the thread " + threadConstructor.getDeclaringClass().getName() + " for the job \"" + jobDescription.getJobId() + "\"!");
		}
	}

	@Override
	public UWSJob createJob(final HttpServletRequest request, final JobOwner user) throws UWSException{
		// Extract the HTTP request ID (the job ID should be the same, if not already used by another job):
		String requestID = null;
		if (request != null && request.getAttribute(UWS.REQ_ATTRIBUTE_ID) != null && request.getAttribute(UWS.REQ_ATTRIBUTE_ID) instanceof String)
			requestID = request.getAttribute(UWS.REQ_ATTRIBUTE_ID).toString();

		// Create the job:
		return new UWSJob(user, createUWSParameters(request), requestID);
	}

	@Override
	public UWSJob createJob(String jobID, JobOwner owner, UWSParameters params, long quote, long startTime, long endTime, List<Result> results, ErrorSummary error) throws UWSException{
		return new UWSJob(jobID, owner, params, quote, startTime, endTime, results, error);
	}

	@Override
	public UWSParameters createUWSParameters(final Map<String,Object> params) throws UWSException{
		return new UWSParameters(params);
	}

	@Override
	public UWSParameters createUWSParameters(final HttpServletRequest req) throws UWSException{
		UWSUrl url = new UWSUrl(req);
		if (url.getJobListName() != null && jobParams.get(url.getJobListName()) != null){
			return new UWSParameters(req, jobParams.get(url.getJobListName()).keySet(), jobParams.get(url.getJobListName()));
		}else
			return new UWSParameters(req);
	}

	@Override
	public RequestParser createRequestParser(final UWSFileManager fileManager) throws UWSException{
		if (constructorRequestParser == null)
			return new UWSRequestParser(fileManager);
		else{
			try{
				return constructorRequestParser.newInstance(fileManager);
			}catch(InstantiationException ie){
				throw new UWSException("Impossible to create an instance of an abstract class: \"" + constructorRequestParser.getDeclaringClass().getName() + "\"!");

			}catch(InvocationTargetException ite){
				if (ite.getCause() != null){
					if (ite.getCause() instanceof UWSException)
						throw (UWSException)ite.getCause();
					else
						throw new UWSException(ite.getCause());
				}else
					throw new UWSException(ite);

			}catch(Exception ex){
				throw new UWSException(UWSException.NOT_FOUND, ex, "Impossible to create the request parser " + constructorRequestParser.getDeclaringClass().getName() + "\"!");
			}
		}
	}

}
