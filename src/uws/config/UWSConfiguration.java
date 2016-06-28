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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import uws.UWSException;
import uws.job.JobThread;
import uws.job.manager.DestructionManager;
import uws.job.manager.ExecutionManager;
import uws.service.UWSFactory;
import uws.service.backup.DefaultUWSBackupManager;
import uws.service.request.RequestParser;
import uws.service.request.UWSRequestParser;

/**
 * <p>Utility class gathering tool functions and properties' names useful to deal with a UWS configuration file.</p>
 * 
 * <p><i>This class implements the Design Pattern "Utility": no instance of this class can be created, it can not be extended,
 * and it must be used only thanks to its static classes and attributes.</i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.2 (06/2016)
 * @since 4.2
 */
public final class UWSConfiguration {

	/** Name of the initial parameter to set in the WEB-INF/web.xml file
	 * in order to specify the location and the name of the UWS configuration file to load. */
	public final static String UWS_CONF_PARAMETER = "uwsconf";
	/** Default UWS configuration file. This file is research automatically
	 * if none is specified in the WEB-INF/web.xml initial parameter {@value #UWS_CONF_PARAMETER}. */
	public final static String DEFAULT_UWS_CONF_FILE = "uws.properties";

	/* HOME PAGE KEY */

	/** Name/Key of the property specifying the UWS home page to use.
	 * It can be a file, a URL or a class. If null, the default UWS home page of the library is used.
	 * By default the default library home page is used. */
	public final static String KEY_HOME_PAGE = "home_page";
	/** Name/Key of the property specifying the MIME type of the set home page.
	 * By default, "text/html" is set. */
	public final static String KEY_HOME_PAGE_MIME_TYPE = "home_page_mime_type";

	/* SERVICE KEYS */

	/** Name/Key of the property specifying the name of this UWS service. */
	public final static String KEY_SERVICE_NAME = "service_name";
	/** Name/Key of the property specifying the description of the UWS service. */
	public final static String KEY_SERVICE_DESCRIPTION = "service_description";

	/* JOB LISTS */

	/** Name/Key of the property listing all the job lists to have in the UWS service. */
	public final static String KEY_JOB_LISTS = "joblists";

	/** Regular Expression of a job list name supposed to represent a job list name.
	 * This name MUST contain NO point, NO equal sign (=) and NO space character. */
	public final static String REGEXP_JOB_LIST_NAME = "[^\\.=\\s]+";

	/* JOB ATTRIBUTES */

	/** Name/Key of the property specifying the {@link JobThread} instance to use for a specific job list. */
	public final static String KEY_JOB_THREAD = "job_thread";
	/** Regular Expression of the name/key of the property specifying the {@link JobThread} instance to use for a given job list.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_JOB_THREAD} ends the regular expression.</i></p> */
	public final static String REGEXP_JOB_THREAD = REGEXP_JOB_LIST_NAME + "\\." + KEY_JOB_THREAD;

	/** Name/Key of the property listing all input parameters of jobs of a specific job list. */
	public final static String KEY_PARAMETERS = "job_parameters";
	/** Regular Expression of the name/key of the property listing all parameters expected for jobs of a given job list.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_PARAMETERS} ends the regular expression.</i></p> */
	public final static String REGEXP_PARAMETERS = REGEXP_JOB_LIST_NAME + "\\." + KEY_PARAMETERS;

	/* EXECUTION MANAGEMENT */

	/** Name/Key of the property specifying the default execution duration (in milliseconds) set automatically to a job
	 * if none has been specified by the user. */
	public final static String KEY_DEFAULT_EXECUTION_DURATION = "default_execution_duration";
	/** Regular Expression of the name/key of the property specifying the default execution duration for jobs of a given job list.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_DEFAULT_EXECUTION_DURATION} ends the regular expression.</i></p> */
	public final static String REGEXP_DEFAULT_EXEC_DURATION = REGEXP_JOB_LIST_NAME + "\\." + KEY_DEFAULT_EXECUTION_DURATION;

	/** Name/Key of the property specifying the maximum execution duration (in milliseconds) that can be set on a job. */
	public final static String KEY_MAX_EXECUTION_DURATION = "max_execution_duration";
	/** Regular Expression of the name/key of the property specifying the maximum execution duration for jobs of a given job list.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_MAX_EXECUTION_DURATION} ends the regular expression.</i></p> */
	public final static String REGEXP_MAX_EXEC_DURATION = REGEXP_JOB_LIST_NAME + "\\." + KEY_MAX_EXECUTION_DURATION;

	/** Default value of the property {@link #KEY_DEFAULT_EXECUTION_DURATION} and {@link #KEY_MAX_EXECUTION_DURATION}: {@value #DEFAULT_EXECUTION_DURATION}. */
	public final static int DEFAULT_EXECUTION_DURATION = 0;

	/** Name/Key of the property specifying the maximum number of jobs that can run in parallel inside a specific job list. */
	public final static String KEY_MAX_RUNNING_JOBS = "max_running_jobs";
	/** Regular Expression of the name/key of the property specifying maximum number of jobs that can run in parallel inside
	 * the specified job list.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_MAX_RUNNING_JOBS} ends the regular expression.</i></p> */
	public final static String REGEXP_MAX_RUNNING_JOBS = REGEXP_JOB_LIST_NAME + "\\." + KEY_MAX_RUNNING_JOBS;

	/** Name/Key of the property specifying the {@link ExecutionManager} instance that a specific job list must use. */
	public final static String KEY_EXECUTION_MANAGER = "execution_manager";
	/** Regular Expression of the name/key of the property specifying the {@link ExecutionManager} instance that a given job list must use.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_EXECUTION_MANAGER} ends the regular expression.</i></p> */
	public final static String REGEXP_EXECUTION_MANAGER = REGEXP_JOB_LIST_NAME + "\\." + KEY_EXECUTION_MANAGER;

	/* DESTRUCTION MANAGEMENT */

	/** Name/Key of the property specifying the default destruction interval (actually a duration between the creation and the destruction
	 * of the job) set automatically to a job if none has been specified by the user. */
	public final static String KEY_DEFAULT_DESTRUCTION_INTERVAL = "default_destruction_interval";
	/** Regular Expression of the name/key of the property specifying the default destruction interval for jobs of a given job list.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_DEFAULT_DESTRUCTION_INTERVAL} ends the regular expression.</i></p> */
	public final static String REGEXP_DEFAULT_DESTRUCTION_INTERVAL = REGEXP_JOB_LIST_NAME + "\\." + KEY_DEFAULT_DESTRUCTION_INTERVAL;

	/** Name/Key of the property specifying the maximum destruction interval (actually a duration between the creation and the destruction
	 * of the job) set automatically to a job if none has been specified by the user. */
	public final static String KEY_MAX_DESTRUCTION_INTERVAL = "max_destruction_interval";
	/** Regular Expression of the name/key of the property specifying the maximum destruction interval for jobs of a given job list.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.</i></p> */
	public final static String KEY_REGEXP_MAX_DESTRUCTION_INTERVAL = REGEXP_JOB_LIST_NAME + "\\." + KEY_MAX_DESTRUCTION_INTERVAL;

	/** Name/Key of the property specifying the {@link DestructionManager} instance that a specific job list must use. */
	public final static String KEY_DESTRUCTION_MANAGER = "destruction_manager";
	/** Regular Expression of the name/key of the property specifying the {@link DestructionManager} instance that a given job list must use.
	 * <p><i>The first part of this regular expression ({@link #REGEXP_JOB_LIST_NAME}) is supposed to be the job list name.
	 * Then a point is appended and finally {@link #KEY_DESTRUCTION_MANAGER} ends the regular expression.</i></p> */
	public final static String REGEXP_DESTRUCTION_MANAGER = REGEXP_JOB_LIST_NAME + "\\." + KEY_DESTRUCTION_MANAGER;

	/* FILE MANAGER KEYS */

	/** Name/Key of the property setting the file manager to use in the UWS service. */
	public final static String KEY_FILE_MANAGER = "file_manager";
	/** Value of the property {@link #KEY_FILE_MANAGER} specifying a local file manager. */
	public final static String VALUE_LOCAL = "local";
	/** Default value of the property {@link #KEY_FILE_MANAGER}: {@value #DEFAULT_FILE_MANAGER}. */
	public final static String DEFAULT_FILE_MANAGER = VALUE_LOCAL;
	/** Name/Key of the property setting the local root directory where all UWS files must be stored.
	 * <em>This property is used only if {@link #KEY_FILE_MANAGER} is set to {@link #VALUE_LOCAL}.</em> */
	public final static String KEY_FILE_ROOT_PATH = "file_root_path";
	/** Name/Key of the property indicating whether the jobs must be saved by user or not.
	 * If yes, there will be one directory per user. Otherwise, all jobs are backuped in the same directory
	 * (generally {@link #KEY_FILE_ROOT_PATH}). */
	public final static String KEY_DIRECTORY_PER_USER = "directory_per_user";
	/** Default value of the property {@link #KEY_DIRECTORY_PER_USER}: {@value #DEFAULT_DIRECTORY_PER_USER}. */
	public final static boolean DEFAULT_DIRECTORY_PER_USER = false;
	/** Name/Key of the property indicating whether the user directories (in which jobs of the user are backuped)
	 * must be gathered in less directories. If yes, the groups are generally made using the alphabetic order.
	 * The idea is to reduce the number of apparent directories and to easier the research of a user directory. */
	public final static String KEY_GROUP_USER_DIRECTORIES = "group_user_directories";
	/** Default value of the property {@link #KEY_GROUP_USER_DIRECTORIES}: {@value #DEFAULT_GROUP_USER_DIRECTORIES}. */
	public final static boolean DEFAULT_GROUP_USER_DIRECTORIES = false;

	/* LOG KEYS */

	/** Name/Key of the property specifying the minimum type of messages (i.e. DEBUG, INFO, WARNING, ERROR, FATAL)
	 * that must be logged. By default all messages are logged...which is equivalent to set this property to "DEBUG". */
	public final static String KEY_MIN_LOG_LEVEL = "min_log_level";
	/** Name/Key of the property specifying the frequency of the log file rotation.
	 * By default the log rotation occurs every day at midnight. */
	public final static String KEY_LOG_ROTATION = "log_rotation";

	/* UWS BACKUP */

	/** Name/Key of the property specifying the frequency (in milliseconds) of jobs backup.
	 * This property accepts three types of value: "never" (default), "user_action" (the backup of a job is done when
	 * it is modified), or a numeric positive value (expressed in milliseconds). */
	public final static String KEY_BACKUP_FREQUENCY = "backup_frequency";
	/** Value of the property {@link #KEY_BACKUP_FREQUENCY} indicating that jobs should never be backuped. */
	public final static String VALUE_NEVER = "never";
	/** Value of the property {@link #KEY_BACKUP_FREQUENCY} indicating that job backup should occur only when the user
	 * creates or modifies one of his jobs. This value can be used ONLY IF {@link #KEY_BACKUP_BY_USER} is "true". */
	public final static String VALUE_USER_ACTION = "user_action";
	/** Default value of the property {@link #KEY_BACKUP_FREQUENCY}: {@link #DEFAULT_BACKUP_FREQUENCY}. */
	public final static long DEFAULT_BACKUP_FREQUENCY = DefaultUWSBackupManager.MANUAL;	// = "never" => no UWS backup manager
	/** Name/Key of the property indicating whether there should be one backup file per user or one file for all. */
	public final static String KEY_BACKUP_BY_USER = "backup_by_user";
	/** Default value of the property {@link #KEY_BACKUP_BY_USER}: {@value #DEFAULT_BACKUP_BY_USER}.
	 * This property can be enabled only if a user identification method is provided. */
	public final static boolean DEFAULT_BACKUP_BY_USER = false;

	/* USER IDENTIFICATION */

	/** Name/Key of the property specifying the user identification method to use.
	 * None is implemented by the library, so a class must be provided as value of this property. */
	public final static String KEY_USER_IDENTIFIER = "user_identifier";

	/* REQUEST PARSER */

	/** Name/Key of the property specifying the {@link RequestParser} class to use instead of the default {@link UWSRequestParser}. */
	public final static String KEY_REQUEST_PARSER = "request_parser";

	/* SERIALIZATION */

	/** Name/Key of the property specifying a list of UWS serializers to add to the UWS service.
	 * By default, this list if empty ; only the default UWS serializers exist. */
	public final static String KEY_ADD_SERIALIZERS = "additional_serializers";
	/** Name/Key of the property specifying the XSLT stylesheet to use when job are serialized in XML. */
	public final static String KEY_XSLT_STYLESHEET = "xslt_stylesheet";
	/** Name/Key of the property specifying the error writer the UWS service must use. */
	public final static String KEY_ERROR_WRITER = "error_writer";

	/* ADDITIONAL UWS ACTIONS */

	/** Name/Key of the property specifying a list of actions to add to the UWS service.
	 * By default, this list if empty ; only the default UWS actions exist. */
	public final static String KEY_ADD_UWS_ACTIONS = "additional_actions";

	/* CUSTOM FACTORY */

	/** Name/Key of the property specifying the {@link UWSFactory} class to use instead of the default {@link ConfigurableUWSFactory}.
	 * <em>Setting a value to this property could disable several properties of the UWS configuration file.</em> */
	public final static String KEY_UWS_FACTORY = "uws_factory";

	/** No instance of this class should be created. */
	private UWSConfiguration(){}

	/**
	 * <p>Read the asked property from the given Properties object.</p>
	 * <ul>
	 * 	<li>The returned property value is trimmed (no space at the beginning and at the end of the string).</li>
	 * 	<li>If the value is empty (length=0), NULL is returned.</li>
	 * </ul>
	 * 
	 * @param prop	List of property
	 * @param key	Property whose the value is requested.
	 * 
	 * @return		Return property value.
	 */
	public final static String getProperty(final Properties prop, final String key){
		if (prop == null)
			return null;

		String value = prop.getProperty(key);
		if (value != null){
			value = value.trim();
			return (value.length() == 0) ? null : value;
		}

		return value;
	}

	/**
	 * Extract the job list name prefixing the given property name.
	 * 
	 * <p><b>Important:</b>
	 * 	This function aims to be used for properties prefixed by a job list name such as
	 * 	{@link #REGEXP_JOB_THREAD}, {@link #REGEXP_PARAMETERS}, ...
	 * </p>
	 * 
	 * @param compoundPropertyName	Property name prefixed by a job list name.
	 * 
	 * @return	The prefix of the given property name,
	 *        	or <code>null</code> if the given name is <code>null</code>
	 *             or if it is not prefixed by a valid job list name.
	 */
	public final static String extractJobListName(final String compoundPropertyName){
		if (compoundPropertyName == null || !compoundPropertyName.matches(REGEXP_JOB_LIST_NAME + "\\..+"))
			return null;
		return compoundPropertyName.substring(0, compoundPropertyName.indexOf('.'));
	}

	/**
	 * Test whether a property value is a class name.
	 * Expected syntax: a non-empty string surrounded by brackets ('{' and '}').
	 * 
	 * Note: The class name itself is not checked!
	 * 
	 * @param value	Property value.
	 * 
	 * @return <i>true</i> if the given value is formatted as a class name, <i>false</i> otherwise.
	 */
	public final static boolean isClassName(final String value){
		return (value != null && value.length() > 2 && value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}');
	}

	/**
	 * Fetch the class object corresponding to the class name provided between brackets in the given value.
	 * 
	 * @param value			Value which is supposed to contain the class name between brackets (see {@link #isClassName(String)} for more details)
	 * @param propertyName	Name of the property associated with the parameter "value".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * 
	 * @return	The corresponding Class object.
	 * 
	 * @throws UWSException	If the class name is incorrect
	 *                     	or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType").
	 * 
	 * @see #isClassName(String)
	 */
	@SuppressWarnings("unchecked")
	public final static < C > Class<? extends C> fetchClass(final String value, final String propertyName, final Class<C> expectedType) throws UWSException{
		if (!isClassName(value))
			return null;

		String classPath = value.substring(1, value.length() - 1).trim();
		if (classPath.isEmpty())
			return null;

		try{
			Class<? extends C> classObject = (Class<? extends C>)Class.forName(classPath);
			if (!expectedType.isAssignableFrom(classObject))
				throw new UWSException("The class specified by the property \"" + propertyName + "\" (" + value + ") is not implementing " + expectedType.getName() + ".");
			else
				return classObject;
		}catch(ClassNotFoundException cnfe){
			throw new UWSException("The class specified by the property \"" + propertyName + "\" (" + value + ") can not be found.");
		}catch(ClassCastException cce){
			throw new UWSException("The class specified by the property \"" + propertyName + "\" (" + value + ") is not implementing " + expectedType.getName() + ".");
		}
	}

	/**
	 * Test whether the specified class has a constructor with the specified parameters.
	 * 
	 * @param propValue		Value which is supposed to contain the class name between brackets (see {@link #isClassName(String)} for more details)
	 * @param propName		Name of the property associated with the parameter "propValue".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * @param pTypes		List of each constructor parameter type. Each type MUST be exactly the type declared in the class constructor to select. <i>NULL or empty array if no parameter.</i>
	 * 
	 * @return	<code>true</code> if the specified class has a constructor with the specified parameters,
	 *        	<code>false</code> otherwise.
	 * 
	 * @throws UWSException	If the class name is incorrect
	 *                     	or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType").
	 */
	public final static < C > boolean hasConstructor(final String propValue, final String propName, final Class<C> expectedType, final Class<?>[] pTypes) throws UWSException{
		// Ensure the given name is a class name specification:
		if (!isClassName(propValue))
			throw new UWSException("Class name expected for the property \"" + propName + "\" instead of: \"" + propValue + "\"! The specified class must extend/implement " + expectedType.getName() + ".");

		// Fetch the class object:
		Class<? extends C> classObj = fetchClass(propValue, propName, expectedType);
		try{

			// Get a constructor matching the given parameters list:
			classObj.getConstructor((pTypes == null) ? new Class<?>[0] : pTypes);

			return true;

		}catch(Exception e){
			return false;
		}
	}

	/**
	 * Fetch the specified constructor of the class corresponding to the class name provided between brackets in the given value.
	 * 
	 * <p><b>IMPORTANT:</b>
	 * 	The number and types of given parameters MUST match exactly to the list of parameter types.
	 * </p>
	 * 
	 * @param propValue		Value which is supposed to contain the class name between brackets (see {@link #isClassName(String)} for more details)
	 * @param propName		Name of the property associated with the parameter "propValue".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * @param pTypes		List of each constructor parameter type. Each type MUST be exactly the type declared in the class constructor to select. <i>NULL or empty array if no parameter.</i>
	 * 
	 * @return	The corresponding constructor.
	 * 
	 * @throws UWSException	If the class name is incorrect
	 *                     	or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType")
	 *                     	or if the constructor with the specified parameters can not be found.
	 */
	public final static < C > Constructor<? extends C> fetchConstructor(final String propValue, final String propName, final Class<C> expectedType, final Class<?>[] pTypes) throws UWSException{
		// Ensure the given name is a class name specification:
		if (!isClassName(propValue))
			throw new UWSException("Class name expected for the property \"" + propName + "\" instead of: \"" + propValue + "\"! The specified class must extend/implement " + expectedType.getName() + ".");

		// Fetch the class object:
		Class<? extends C> classObj = fetchClass(propValue, propName, expectedType);
		try{

			// Get a constructor matching the given parameters list:
			return classObj.getConstructor((pTypes == null) ? new Class<?>[0] : pTypes);

		}catch(NoSuchMethodException e){
			// List parameters' type:
			StringBuffer pTypesStr = new StringBuffer();
			if (pTypes != null){
				for(int i = 0; i < pTypes.length; i++){
					if (pTypesStr.length() > 0)
						pTypesStr.append(", ");
					if (pTypes[i] == null)
						pTypesStr.append("NULL");
					pTypesStr.append(pTypes[i].getName());
				}
			}
			// Throw the error:
			throw new UWSException("Missing constructor " + classObj.getName() + "(" + pTypesStr.toString() + ")! See the value \"" + propValue + "\" of the property \"" + propName + "\".");
		}catch(SecurityException se){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, se, "Security error when trying to fetch the constructor with a single parameter of type " + expectedType.getName() + " of the class \"" + propValue + "\" specified by the property \"" + propName + "\"!");
		}
	}

	/**
	 * <p>Create an instance of the specified class. The class name is expected to be surrounded by {} in the given value.</p>
	 * 
	 * <p>The instance is created using the empty constructor of the specified class.</p>
	 * 
	 * @param propValue		Value which is supposed to contain the class name between brackets (see {@link #isClassName(String)} for more details)
	 * @param propName		Name of the property associated with the parameter "propValue".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * 
	 * @return	The corresponding instance.
	 * 
	 * @throws UWSException	If the class name is incorrect
	 *                     	or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType")
	 *                     	or if the specified class has no empty constructor
	 *                     	or if an error occurred while calling this constructor.
	 * 
	 * @see #isClassName(String)
	 * @see #fetchClass(String, String, Class)
	 */
	public final static < C > C newInstance(final String propValue, final String propName, final Class<C> expectedType) throws UWSException{
		return newInstance(propValue, propName, expectedType, null, null);
	}

	/**
	 * <p>Create an instance of the specified class. The class name is expected to be surrounded by {} in the given value.</p>
	 * 
	 * <p><b>IMPORTANT:</b>
	 * 	The instance is created using the constructor whose the declaration matches exactly with the given list of parameter types.
	 * 	The number and types of given parameters MUST match exactly to the list of parameter types.
	 * </p>
	 * 
	 * @param propValue		Value which is supposed to contain the class name between brackets (see {@link #isClassName(String)} for more details)
	 * @param propName		Name of the property associated with the parameter "propValue".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * @param pTypes		List of each constructor parameter type. Each type MUST be exactly the type declared in the class constructor to select. <i>NULL or empty array if no parameter.</i>
	 * @param parameters	List of all constructor parameters. The number of object MUST match exactly the number of classes provided in the parameter pTypes. <i>NULL or empty array if no parameter.</i>
	 * 
	 * @return	The corresponding instance.
	 * 
	 * @throws UWSException	If the class name is incorrect
	 *                     	or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType")
	 *                     	or if the constructor with the specified parameters can not be found
	 *                     	or if an error occurred while calling this constructor.
	 * 
	 * @see #isClassName(String)
	 * @see #fetchClass(String, String, Class)
	 */
	public final static < C > C newInstance(final String propValue, final String propName, final Class<C> expectedType, final Class<?>[] pTypes, final Object[] parameters) throws UWSException{
		// Ensure the given name is a class name specification:
		if (!isClassName(propValue))
			throw new UWSException("Class name expected for the property \"" + propName + "\" instead of: \"" + propValue + "\"! The specified class must extend/implement " + expectedType.getName() + ".");

		Class<? extends C> classObj = null;
		try{

			// Fetch the class object:
			classObj = fetchClass(propValue, propName, expectedType);

			// Get a constructor matching the given parameters list:
			Constructor<? extends C> constructor = classObj.getConstructor((pTypes == null) ? new Class<?>[0] : pTypes);

			// Finally create a new instance:
			return constructor.newInstance((parameters == null) ? new Object[0] : parameters);

		}catch(NoSuchMethodException e){
			// List parameters' type:
			StringBuffer pTypesStr = new StringBuffer();
			if (pTypes != null){
				for(int i = 0; i < pTypes.length; i++){
					if (pTypesStr.length() > 0)
						pTypesStr.append(", ");
					if (pTypes[i] == null)
						pTypesStr.append("NULL");
					pTypesStr.append(pTypes[i].getName());
				}
			}
			// Throw the error:
			throw new UWSException("Missing constructor " + classObj.getName() + "(" + pTypesStr.toString() + ")! See the value \"" + propValue + "\" of the property \"" + propName + "\".");
		}catch(InstantiationException ie){
			throw new UWSException("Impossible to create an instance of an abstract class: \"" + classObj.getName() + "\"! See the value \"" + propValue + "\" of the property \"" + propName + "\".");
		}catch(InvocationTargetException ite){
			if (ite.getCause() != null){
				if (ite.getCause() instanceof UWSException)
					throw (UWSException)ite.getCause();
				else
					throw new UWSException(ite.getCause());
			}else
				throw new UWSException(ite);
		}catch(UWSException te){
			throw te;
		}catch(Exception ex){
			throw new UWSException(UWSException.NOT_FOUND, ex, "Impossible to create an instance of " + expectedType.getName() + " as specified in the property \"" + propName + "\": \"" + propValue + "\"!");
		}
	}

	/**
	 * <p>Lets parsing a limit (for upload, ...) with its numeric value and its unit.</p>
	 * <p>
	 * 	Here is the expected syntax: num_val[unit].
	 * 	Where unit is optional and should be one of the following values: B, kB, MB or GB.
	 * 	If the unit is not specified, it is set by default to BYTES.
	 * </p>
	 * <p><i>Note: If the value is strictly less than 0 (whatever is the unit), the returned value will be -1.</i></p>
	 * 
	 * @param value				Property value (must follow the limit syntax: num_val[unit] ; ex: 20kB or 2000 (for 2000 bytes)).
	 * @param propertyName		Name of the property which specify the limit.
	 * 
	 * @return	The expressed unit in bytes
	 *        	or -1, if the given value was incorrect or negative.
	 * 
	 * @throws UWSException	If the syntax is incorrect or if a not allowed unit has been used.
	 */
	public final static long parseLimit(String value, final String propertyName) throws UWSException{
		// Remove any whitespace inside or outside the numeric value and its unit:
		if (value != null)
			value = value.replaceAll("\\s", "");

		// If empty value, return an infinite limit:
		if (value == null || value.length() == 0)
			return -1;

		// A. Parse the string from the end in order to extract the unit part.
		//    The final step of the loop is the extraction of the numeric value, when the first digit is encountered.
		long numValue = -1;
		StringBuffer buf = new StringBuffer();
		for(int i = value.length() - 1; i >= 0; i--){
			// if a digit, extract the numeric value:
			if (value.charAt(i) >= '0' && value.charAt(i) <= '9'){
				try{
					numValue = Integer.parseInt(value.substring(0, i + 1));
					break;
				}catch(NumberFormatException nfe){
					throw new UWSException("Integer expected for the property " + propertyName + " for the substring \"" + value.substring(0, i + 1) + "\" of the whole value: \"" + value + "\"!");
				}
			}
			// if a character, store it for later processing:
			else
				buf.append(value.charAt(i));

		}

		// B. Parse the unit.
		// if no unit, set BYTES by default:
		if (buf.length() == 0)
			;
		// if the unit is too long, throw an exception:
		else if (buf.length() > 2)
			throw new UWSException("Unknown limit unit (" + buf.reverse().toString() + ") for the property " + propertyName + ": \"" + value + "\"!");
		// try to identify the unit:
		else{
			// the base unit: bytes
			if (buf.charAt(0) != 'B')
				throw new UWSException("Unknown limit unit (" + buf.reverse().toString() + ") for the property " + propertyName + ": \"" + value + "\"!");
			// the 10-power of the base unit, if any:
			if (buf.length() > 1){
				switch(buf.charAt(1)){
					case 'G':
						numValue *= 1000;
					case 'M':
						numValue *= 1000;
					case 'k':
						numValue *= 1000;
						break;
					default:
						throw new UWSException("Unknown limit unit (" + buf.reverse().toString() + ") for the property " + propertyName + ": \"" + value + "\"!");
				}
			}
		}

		return (numValue < 0) ? -1 : numValue;
	}

}
