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

import static uws.config.UWSConfiguration.DEFAULT_BACKUP_BY_USER;
import static uws.config.UWSConfiguration.DEFAULT_BACKUP_FREQUENCY;
import static uws.config.UWSConfiguration.DEFAULT_DIRECTORY_PER_USER;
import static uws.config.UWSConfiguration.DEFAULT_GROUP_USER_DIRECTORIES;
import static uws.config.UWSConfiguration.DEFAULT_UWS_CONF_FILE;
import static uws.config.UWSConfiguration.KEY_ADD_SERIALIZERS;
import static uws.config.UWSConfiguration.KEY_ADD_UWS_ACTIONS;
import static uws.config.UWSConfiguration.KEY_BACKUP_BY_USER;
import static uws.config.UWSConfiguration.KEY_BACKUP_FREQUENCY;
import static uws.config.UWSConfiguration.KEY_DESTRUCTION_MANAGER;
import static uws.config.UWSConfiguration.KEY_DIRECTORY_PER_USER;
import static uws.config.UWSConfiguration.KEY_ERROR_WRITER;
import static uws.config.UWSConfiguration.KEY_EXECUTION_MANAGER;
import static uws.config.UWSConfiguration.KEY_FILE_MANAGER;
import static uws.config.UWSConfiguration.KEY_FILE_ROOT_PATH;
import static uws.config.UWSConfiguration.KEY_GROUP_USER_DIRECTORIES;
import static uws.config.UWSConfiguration.KEY_HOME_PAGE;
import static uws.config.UWSConfiguration.KEY_HOME_PAGE_MIME_TYPE;
import static uws.config.UWSConfiguration.KEY_JOB_LISTS;
import static uws.config.UWSConfiguration.KEY_LOG_ROTATION;
import static uws.config.UWSConfiguration.KEY_MAX_RUNNING_JOBS;
import static uws.config.UWSConfiguration.KEY_MIN_LOG_LEVEL;
import static uws.config.UWSConfiguration.KEY_SERVICE_DESCRIPTION;
import static uws.config.UWSConfiguration.KEY_SERVICE_NAME;
import static uws.config.UWSConfiguration.KEY_USER_IDENTIFIER;
import static uws.config.UWSConfiguration.KEY_UWS_FACTORY;
import static uws.config.UWSConfiguration.KEY_XSLT_STYLESHEET;
import static uws.config.UWSConfiguration.REGEXP_JOB_LIST_NAME;
import static uws.config.UWSConfiguration.UWS_CONF_PARAMETER;
import static uws.config.UWSConfiguration.VALUE_LOCAL;
import static uws.config.UWSConfiguration.VALUE_NEVER;
import static uws.config.UWSConfiguration.VALUE_USER_ACTION;
import static uws.config.UWSConfiguration.getProperty;
import static uws.config.UWSConfiguration.hasConstructor;
import static uws.config.UWSConfiguration.isClassName;
import static uws.config.UWSConfiguration.newInstance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.job.JobList;
import uws.job.manager.DefaultDestructionManager;
import uws.job.manager.DefaultExecutionManager;
import uws.job.manager.DestructionManager;
import uws.job.manager.ExecutionManager;
import uws.job.manager.QueuedExecutionManager;
import uws.job.serializer.UWSSerializer;
import uws.job.serializer.XMLSerializer;
import uws.service.UWSFactory;
import uws.service.UWSService;
import uws.service.UserIdentifier;
import uws.service.actions.ShowHomePage;
import uws.service.actions.UWSAction;
import uws.service.backup.DefaultUWSBackupManager;
import uws.service.backup.UWSBackupManager;
import uws.service.error.ServiceErrorWriter;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;
import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>HTTP servlet fully configured with a UWS configuration file.</p>
 * 
 * <p>
 * 	This configuration file may be specified in the initial parameter named {@link UWSConfiguration#UWS_CONF_PARAMETER}
 * 	of this servlet inside the WEB-INF/web.xml file. If none is specified, the file {@link UWSConfiguration#DEFAULT_UWS_CONF_FILE}
 * 	will be searched inside the directories of the classpath, and inside WEB-INF and META-INF.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.2 (06/2016)
 * @since 4.2
 */
public class ConfigurableUWSServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** UWS object representing the UWS service. */
	private UWSService uws = null;

	@Override
	public void init(final ServletConfig config) throws ServletException{
		// Nothing to do, if UWS is already initialized:
		if (uws != null)
			return;

		/* 1. GET THE FILE PATH OF THE UWS CONFIGURATION FILE */
		String uwsConfPath = config.getInitParameter(UWS_CONF_PARAMETER);
		if (uwsConfPath == null || uwsConfPath.trim().length() == 0)
			uwsConfPath = null;
		//throw new ServletException("Configuration file path missing! You must set a servlet init parameter whose the name is \"" + UWS_CONF_PARAMETER + "\".");

		/* 2. OPEN THE CONFIGURATION FILE */
		InputStream input = null;
		// CASE: No file specified => search in the classpath for a file having the default name "uws.properties".
		if (uwsConfPath == null)
			input = searchFile(DEFAULT_UWS_CONF_FILE, config);
		else{
			File f = new File(uwsConfPath);
			// CASE: The given path matches to an existing local file.
			if (f.exists()){
				try{
					input = new FileInputStream(f);
				}catch(IOException ioe){
					throw new ServletException("Impossible to read the UWS configuration file (" + uwsConfPath + ")!", ioe);
				}
			}
			// CASE: The given path seems to be relative to the servlet root directory.
			else
				input = searchFile(uwsConfPath, config);
		}
		// If no file has been found, cancel the servlet loading:
		if (input == null)
			throw new ServletException("Configuration file not found with the path: \"" + ((uwsConfPath == null) ? DEFAULT_UWS_CONF_FILE : uwsConfPath) + "\"! Please provide a correct file path in servlet init parameter (\"" + UWS_CONF_PARAMETER + "\") or put your configuration file named \"" + DEFAULT_UWS_CONF_FILE + "\" in a directory of the classpath or in WEB-INF or META-INF.");

		/* 3. PARSE IT INTO A PROPERTIES SET */
		Properties uwsConf = new Properties();
		try{
			uwsConf.load(input);
		}catch(IOException ioe){
			throw new ServletException("Impossible to read the UWS configuration file (" + uwsConfPath + ")!", ioe);
		}finally{
			try{
				input.close();
			}catch(IOException ioe2){}
		}

		/* 4. CREATE THE FILE MANAGER */
		UWSFileManager fileManager = null;
		try{
			fileManager = createFileManager(uwsConf, config.getServletContext().getRealPath(""));
		}catch(Exception ex){
			if (ex instanceof UWSException)
				throw new ServletException(ex.getMessage(), ex.getCause());
			else
				throw new ServletException("Impossible to initialize the UWS file manager!", ex);
		}

		/* 5. CREATE THE LOGGER */
		UWSLog logger = null;
		try{
			logger = createLogger(uwsConf, fileManager);
		}catch(Exception ex){
			if (ex instanceof UWSException)
				throw new ServletException(ex.getMessage(), ex.getCause());
			else
				throw new ServletException("Impossible to initialize the UWS logger!", ex);
		}

		/* 6. CREATE THE UWS SERVICE */
		try{
			uws = new UWSService(createFactory(uwsConf), fileManager, logger);
		}catch(Exception ex){
			uws = null;
			if (ex instanceof UWSException)
				throw new ServletException(ex.getMessage(), ex.getCause());
			else
				throw new ServletException("Impossible to initialize the UWS service!", ex);
		}

		/* 6Bis. SET THE HOME PAGE */
		String propValue = getProperty(uwsConf, KEY_HOME_PAGE);
		if (propValue != null){
			// If it is a class path, replace the current home page by an instance of this class:
			if (isClassName(propValue)){
				try{
					uws.replaceUWSAction(newInstance(propValue, KEY_HOME_PAGE, ShowHomePage.class, new Class<?>[]{UWSService.class}, new Object[]{uws}));
				}catch(UWSException te){
					throw new ServletException(te.getMessage(), te.getCause());
				}
			}
			// If it is a file URI (null, file inside WebContent, file://..., http://..., etc...):
			else{
				// ...set the given URI:
				uws.setHomePage(propValue);
				// ...and its MIME type (if any):
				propValue = getProperty(uwsConf, KEY_HOME_PAGE_MIME_TYPE);
				if (propValue != null)
					uws.setHomePageMimeType(propValue);
			}
		}

		/* 6Ter. SET ALL GENERAL SERVICE CONNECTION INFORMATION */
		uws.setName(getProperty(uwsConf, KEY_SERVICE_NAME));
		uws.setDescription(getProperty(uwsConf, KEY_SERVICE_DESCRIPTION));

		/* 7. CONFIGURE THE BACKUP MANAGER */
		initBackup(uwsConf);

		/* 8. CONFIGURE THE USER IDENTIFICATION */
		initUserIdentifier(uwsConf);

		/* 9. CREATE THE JOB LISTS */
		initJobLists(uwsConf);

		/* 10. INITIALIZE ADDITIONAL ACTIONS */
		addCustomActions(uwsConf);

		/* 11. INITIALIZE THE SERIALIZATION */
		addCustomSerializers(uwsConf);
		initXSLTStylesheet(uwsConf);
		initErrorWriter(uwsConf);

		/* 12. DEFAULT SERVLET INITIALIZATION */
		super.init(config);
	}

	/**
	 * Initialize the management of UWS service files using the given UWS configuration file.
	 * 
	 * @param uwsConfig		The content of the UWS configuration file.
	 * @param webAppRootDir	The directory of the Web Application running this UWS service.
	 *                     	<em>This directory may be used only to search the root UWS directory
	 *                     	if specified with a relative path in the UWS configuration file.</em>
	 * 
	 * @return	The created file manager.
	 * 
	 * @throws UWSException	If a property is wrong or missing, or if an error occurs while creating the file manager.
	 */
	private UWSFileManager createFileManager(final Properties uwsConfig, final String webAppRootDir) throws UWSException{
		// Read the desired file manager:
		String fileManagerType = getProperty(uwsConfig, KEY_FILE_MANAGER);
		if (fileManagerType == null)
			throw new UWSException("The property \"" + KEY_FILE_MANAGER + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_LOCAL + " or a class name between {...}.");
		else
			fileManagerType = fileManagerType.trim();

		// LOCAL file manager:
		if (fileManagerType.equalsIgnoreCase(VALUE_LOCAL)){
			// Read the desired root path:
			String rootPath = getProperty(uwsConfig, KEY_FILE_ROOT_PATH);
			if (rootPath == null)
				throw new UWSException("The property \"" + KEY_FILE_ROOT_PATH + "\" is missing! It is required to create a TAP Service. Please provide a path toward a directory which will contain all files related to the service.");
			File rootFile = getFile(rootPath, webAppRootDir, KEY_FILE_ROOT_PATH);

			// Determine whether there should be one directory for each user:
			String propValue = getProperty(uwsConfig, KEY_DIRECTORY_PER_USER);
			boolean oneDirectoryPerUser = (propValue == null) ? DEFAULT_DIRECTORY_PER_USER : Boolean.parseBoolean(propValue);

			// Determine whether there should be one directory for each user:
			propValue = getProperty(uwsConfig, KEY_GROUP_USER_DIRECTORIES);
			boolean groupUserDirectories = (propValue == null) ? DEFAULT_GROUP_USER_DIRECTORIES : Boolean.parseBoolean(propValue);

			// Build the Local TAP File Manager:
			try{
				return new LocalUWSFileManager(rootFile, oneDirectoryPerUser, groupUserDirectories);
			}catch(UWSException e){
				throw new UWSException("The property \"" + KEY_FILE_ROOT_PATH + "\" (" + rootPath + ") is incorrect: " + e.getMessage());
			}
		}
		// CUSTOM file manager:
		else
			return newInstance(fileManagerType, KEY_FILE_MANAGER, UWSFileManager.class, new Class<?>[]{Properties.class}, new Object[]{uwsConfig});
	}

	/**
	 * Initialize the UWS logger with the given UWS configuration file.
	 * 
	 * @param uwsConfig		The content of the UWS configuration file.
	 * @param fileManager	The file manager to access the log file(s).
	 */
	private UWSLog createLogger(final Properties uwsConfig, final UWSFileManager fileManager){
		// Create the logger:
		UWSLog logger = new DefaultUWSLog(fileManager);

		StringBuffer buf = new StringBuffer("Logger initialized");

		// Set the minimum log level:
		String propValue = getProperty(uwsConfig, KEY_MIN_LOG_LEVEL);
		if (propValue != null){
			try{
				((DefaultUWSLog)logger).setMinLogLevel(LogLevel.valueOf(propValue.toUpperCase()));
			}catch(IllegalArgumentException iae){}
		}
		buf.append(" (minimum log level: ").append(((DefaultUWSLog)logger).getMinLogLevel());

		// Set the log rotation period, if any:
		if (fileManager instanceof LocalUWSFileManager){
			propValue = getProperty(uwsConfig, KEY_LOG_ROTATION);
			if (propValue != null)
				((LocalUWSFileManager)fileManager).setLogRotationFreq(propValue);
			buf.append(", log rotation: ").append(((LocalUWSFileManager)fileManager).getLogRotationFreq());
		}

		// Log the successful initialization with set parameters:
		buf.append(").");
		logger.info(buf.toString());

		return logger;
	}

	/**
	 * <p>Initialize the {@link UWSFactory} to use.</p>
	 * 
	 * <p>
	 * 	The built factory is either a {@link ConfigurableUWSFactory} instance (by default) or
	 * 	an instance of the class specified in the UWS configuration file.
	 * </p>
	 * 
	 * @param uwsConfig		The content of the UWS configuration file.
	 * 
	 * @throws UWSException	If an error occurs while building the specified {@link UWSFactory}.
	 * 
	 * @see ConfigurableUWSFactory
	 */
	private UWSFactory createFactory(final Properties uwsConfig) throws UWSException{
		String propValue = getProperty(uwsConfig, KEY_UWS_FACTORY);
		if (propValue == null)
			return new ConfigurableUWSFactory(uwsConfig);
		else if (hasConstructor(propValue, KEY_UWS_FACTORY, UWSFactory.class, new Class<?>[]{Properties.class}))
			return newInstance(propValue, KEY_UWS_FACTORY, UWSFactory.class, new Class<?>[]{Properties.class}, new Object[]{uwsConfig});
		else
			return newInstance(propValue, KEY_UWS_FACTORY, UWSFactory.class, new Class<?>[]{}, new Object[]{});
	}

	/**
	 * Create a {@link UWSBackupManager} if needed, thanks to the configuration provided in the UWS configuration file.
	 * 
	 * @param uwsConf	List of properties set in the UWS configuration file.
	 * 
	 * @throws ServletException	If any error occurs when initializing the {@link UWSBackupManager}.
	 */
	private void initBackup(final Properties uwsConf) throws ServletException{
		try{
			/* Set the backup frequency: */
			String propValue = getProperty(uwsConf, KEY_BACKUP_FREQUENCY);

			// determine whether the value is a time period ; if yes, set the frequency:
			long backupFrequency;
			boolean backupByUser;
			if (propValue != null){
				try{
					backupFrequency = Long.parseLong(propValue);
					if (backupFrequency <= 0)
						backupFrequency = DEFAULT_BACKUP_FREQUENCY;
				}catch(NumberFormatException nfe){
					// if the value was not a valid numeric time period, try to identify the different textual options:
					if (propValue.equalsIgnoreCase(VALUE_NEVER))
						backupFrequency = DefaultUWSBackupManager.MANUAL;
					else if (propValue.equalsIgnoreCase(VALUE_USER_ACTION))
						backupFrequency = DefaultUWSBackupManager.AT_USER_ACTION;
					else
						throw new UWSException("Long expected for the property \"" + KEY_BACKUP_FREQUENCY + "\", instead of: \"" + propValue + "\"!");
				}
			}else
				backupFrequency = DEFAULT_BACKUP_FREQUENCY;

			// Specify whether the backup must be organized by user or not:
			propValue = getProperty(uwsConf, KEY_BACKUP_BY_USER);
			backupByUser = (propValue == null) ? DEFAULT_BACKUP_BY_USER : Boolean.parseBoolean(propValue);

			// Finally create and set the backup manager:
			uws.setBackupManager(new DefaultUWSBackupManager(uws, backupByUser, backupFrequency));

		}catch(UWSException ue){
			throw new ServletException("Impossible to initialize the Backup system (and so to restore all the last backuped jobs)!", ue);
		}
	}

	/**
	 * Initialize the UWS user identification method.
	 * 
	 * @param uwsConfig	The content of the UWS configuration file.
	 * 
	 * @throws ServletException	If the corresponding UWS configuration property is wrong.
	 */
	private void initUserIdentifier(final Properties uwsConfig) throws ServletException{
		// Get the property value:
		String propValue = getProperty(uwsConfig, KEY_USER_IDENTIFIER);
		if (propValue != null){
			try{
				uws.setUserIdentifier(newInstance(propValue, KEY_USER_IDENTIFIER, UserIdentifier.class));
			}catch(UWSException ue){
				throw new ServletException("Impossible to initialize the user identification!", ue);
			}
		}
	}

	/**
	 * Initialize all the specified job lists.
	 * 
	 * @param uwsConfig	The content of the UWS configuration file.
	 * 
	 * @throws ServletException	If the corresponding UWS configuration property is wrong.
	 */
	private void initJobLists(final Properties uwsConf) throws ServletException{
		String propValue = getProperty(uwsConf, KEY_JOB_LISTS);
		if (propValue != null){
			// split the list of job list names:
			String[] jlNames = propValue.split(",");
			if (jlNames == null || jlNames.length == 0)
				throw new ServletException("Missing job list name! At least one job list name must be provided. See property \"" + KEY_JOB_LISTS + "\".");

			// for each job list name:
			int nbMaxRunningJobs;
			ExecutionManager execManager;
			DestructionManager destManager;
			for(String jlName : jlNames){
				// normalize and test the name:
				jlName = jlName.trim();
				if (!jlName.matches(REGEXP_JOB_LIST_NAME))
					throw new ServletException("Incorrect job list name: \"" + jlName + "\"! It must not contain space characters, point and equal sign.");

				// configure the execution manager, if any is specified in the configuration:
				nbMaxRunningJobs = -1;
				execManager = null;
				try{
					// if an execution manager is provided, set it:
					propValue = getProperty(uwsConf, jlName + "." + KEY_EXECUTION_MANAGER);
					if (propValue != null)
						execManager = newInstance(propValue, jlName + "." + KEY_EXECUTION_MANAGER, ExecutionManager.class, new Class<?>[]{UWSLog.class}, new Object[]{uws.getLogger()});

					/* if none is provided, the default execution manager will be used
					 * EXCEPT if a maximum number of running jobs is specified ; in such case a QueuedExecutionManager will be used. */
					else{
						propValue = getProperty(uwsConf, jlName + "." + KEY_MAX_RUNNING_JOBS);
						if (propValue != null){
							try{
								nbMaxRunningJobs = Integer.parseInt(propValue);
								if (nbMaxRunningJobs > 0)
									execManager = new QueuedExecutionManager(uws.getLogger(), nbMaxRunningJobs);
							}catch(NumberFormatException nfe){
								uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Incorrect value for the property \"" + jlName + "." + KEY_MAX_RUNNING_JOBS + "\": \"" + propValue + "\"! It should be a positive integer value. No execution queue is set for this job list.", nfe);
							}
						}
					}
				}catch(UWSException ue){
					uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Impossible to set a custom execution manager to the job list \"" + jlName + "\"! The default one will be used.", ue);
				}

				// configure the destruction manager, if any is specified in the configuration:
				destManager = null;
				try{
					propValue = getProperty(uwsConf, jlName + "." + KEY_DESTRUCTION_MANAGER);
					if (propValue != null)
						destManager = newInstance(propValue, jlName + "." + KEY_DESTRUCTION_MANAGER, DestructionManager.class, new Class<?>[]{}, new Object[]{});
				}catch(UWSException ue){
					uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Impossible to set a custom destruction manager to the job list \"" + jlName + "\"! The default one will be used.", ue);
				}

				// add the job list to this UWS service:
				if (execManager == null)
					execManager = new DefaultExecutionManager(uws.getLogger());
				if (destManager == null)
					destManager = new DefaultDestructionManager();
				uws.addJobList(new JobList(jlName, execManager, destManager));
			}
		}else
			throw new ServletException("Missing job list name! At least one job list name must be provided. See property \"" + KEY_JOB_LISTS + "\".");
	}

	/**
	 * Add all additional custom actions listed in the configuration file.
	 * 
	 * @param uwsConfig	The content of the UWS configuration file.
	 * 
	 * @throws ServletException	If the corresponding UWS configuration property is wrong.
	 */
	private void addCustomActions(final Properties uwsConfig) throws ServletException{
		// Get the property value:
		String propValue = getProperty(uwsConfig, KEY_ADD_UWS_ACTIONS);
		if (propValue != null){
			// Tokenise the list of classes:
			String[] actionClasses = propValue.split(",");
			if (actionClasses == null || actionClasses.length == 0)
				return;

			// For each item:
			for(String actionClass : actionClasses){
				try{
					// extract the action index, if any is provided:
					int actionIndex = actionClass.indexOf(':');
					if (actionIndex > 0){
						actionIndex = Integer.parseInt(actionClass.substring(0, actionIndex));
						actionClass = actionClass.substring(actionIndex + 1);
					}
					// create an instance of the specified action:
					UWSAction action = newInstance(actionClass, KEY_ADD_UWS_ACTIONS, UWSAction.class, new Class<?>[]{UWSService.class}, new Object[]{uws});
					// add or replacing depending if an action with the same name already exists or not:
					boolean added = false;
					if (uws.getUWSAction(action.getName()) != null)
						added = (uws.replaceUWSAction(action) != null);
					else if (actionIndex >= 0)
						added = uws.addUWSAction(actionIndex, action);
					else
						added = uws.addUWSAction(action);
					// log an error if the addition is not successful:
					if (!added)
						uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Failed to add the UWS action \"" + action.getName() + "\" implemented with the class \"" + actionClass + "\"! See property \"" + KEY_ADD_UWS_ACTIONS + "\".", null);
				}catch(UWSException ue){
					uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Impossible to create the UWS action \"" + actionClass + "\" specified in the property \"" + KEY_ADD_UWS_ACTIONS + "\"!", ue);
				}catch(NumberFormatException nfe){
					uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Impossible to extract the given action index for the UWS action \"" + actionClass + "\" specified in the property \"" + KEY_ADD_UWS_ACTIONS + "\"! A positive integer value is expected.", nfe);
				}
			}
		}
	}

	/**
	 * Add all additional UWS serialized listed in the configuration file.
	 * 
	 * @param uwsConfig	The content of the UWS configuration file.
	 * 
	 * @throws ServletException	If the corresponding UWS configuration property is wrong.
	 */
	private void addCustomSerializers(final Properties uwsConfig) throws ServletException{
		// Get the property value:
		String propValue = getProperty(uwsConfig, KEY_ADD_SERIALIZERS);
		if (propValue != null){
			// Tokenise the list of classes:
			String[] serializerClasses = propValue.split(",");
			if (serializerClasses == null || serializerClasses.length == 0)
				return;

			// For each item:
			for(String serializerClass : serializerClasses){
				try{
					// create and add the specified serializer to this UWS service:
					uws.addSerializer(newInstance(serializerClass, KEY_ADD_SERIALIZERS, UWSSerializer.class, new Class<?>[]{}, new Object[]{}));
				}catch(UWSException ue){
					uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Impossible to create the UWS serializer \"" + serializerClass + "\" specified in the property \"" + KEY_ADD_SERIALIZERS + "\"!", ue);
				}
			}
		}
	}

	/**
	 * Initialize the error writer of the UWS service.
	 * 
	 * @param uwsConfig	The content of the UWS configuration file.
	 * 
	 * @throws ServletException	If the corresponding UWS configuration property is wrong.
	 */
	private void initXSLTStylesheet(final Properties uwsConfig) throws ServletException{
		// Get the property value:
		String propValue = getProperty(uwsConfig, KEY_XSLT_STYLESHEET);
		if (propValue != null){
			try{
				if (uws.getSerializer(XMLSerializer.MIME_TYPE_XML) instanceof XMLSerializer)
					((XMLSerializer)uws.getSerializer(XMLSerializer.MIME_TYPE_XML)).setXSLTPath(propValue);
			}catch(UWSException ue){
				uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Impossible to set the specified XSLT stylesheet: \"" + propValue + "\"! Then, no XSLT stylesheet is used.", ue);
			}
		}
	}

	/**
	 * Initialize the error writer of the UWS service.
	 * 
	 * @param uwsConfig	The content of the UWS configuration file.
	 * 
	 * @throws ServletException	If the corresponding UWS configuration property is wrong.
	 */
	private void initErrorWriter(final Properties uwsConfig) throws ServletException{
		// Get the property value:
		String propValue = getProperty(uwsConfig, KEY_ERROR_WRITER);
		if (propValue != null){
			try{
				uws.setErrorWriter(newInstance(propValue, KEY_ERROR_WRITER, ServiceErrorWriter.class));
			}catch(UWSException ue){
				uws.getLogger().logUWS(LogLevel.ERROR, uws, "INIT", "Impossible to initialize the error writer! Then, the default one will be used.", ue);
			}
		}
	}

	/**
	 * Search the given file name/path in the directories of the classpath, then inside WEB-INF and finally inside META-INF.
	 * 
	 * @param filePath	A file name/path.
	 * @param config	Servlet configuration (containing also the context class loader - link with the servlet classpath).
	 * 
	 * @return	The input stream toward the specified file, or NULL if no file can be found.
	 * 
	 * @since 2.0
	 */
	protected final InputStream searchFile(String filePath, final ServletConfig config){
		InputStream input = null;

		// Try to search in the classpath (with just a file name or a relative path):
		input = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);

		// If not found, try searching in WEB-INF and META-INF (as this fileName is a file path relative to one of these directories):
		if (input == null){
			if (filePath.startsWith("/"))
				filePath = filePath.substring(1);
			// ...try at the root of WEB-INF:
			input = config.getServletContext().getResourceAsStream("/WEB-INF/" + filePath);
			// ...and at the root of META-INF:
			if (input == null)
				input = config.getServletContext().getResourceAsStream("/META-INF/" + filePath);
		}

		return input;
	}

	/**
	 * <p>Resolve the given file name/path.</p>
	 * 
	 * <p>Only the URI protocol "file:" is allowed. If the protocol is different a {@link UWSException} is thrown.</p>
	 * 
	 * <p>
	 * 	If not an absolute URI, the given path may be either relative or absolute. A relative path is always considered
	 * 	as relative from the Web Application directory (supposed to be given in 2nd parameter).
	 * </p>
	 * 
	 * @param filePath			URI/Path/Name of the file to get.
	 * @param webAppRootPath	Web Application directory local path.
	 * @param propertyName		Name of the property which gives the given file path.
	 * 
	 * @return	The specified File instance.
	 * 
	 * @throws UWSException	If the given URI is malformed or if the used URI scheme is different from "file:".
	 */
	protected final File getFile(final String filePath, final String webAppRootPath, final String propertyName) throws UWSException{
		if (filePath == null)
			return null;

		try{
			URI uri = new URI(filePath);
			if (uri.isAbsolute()){
				if (uri.getScheme().equalsIgnoreCase("file"))
					return new File(uri);
				else
					throw new UWSException("Incorrect file URI for the property \"" + propertyName + "\": \"" + filePath + "\"! Only URI with the protocol \"file:\" are allowed.");
			}else{
				File f = new File(filePath);
				if (f.isAbsolute())
					return f;
				else
					return new File(webAppRootPath, filePath);
			}
		}catch(URISyntaxException use){
			throw new UWSException(UWSException.NOT_FOUND, use, "Incorrect file URI for the property \"" + propertyName + "\": \"" + filePath + "\"! Bad syntax for the given file URI.");
		}
	}

	@Override
	public void destroy(){
		// Free all resources used by UWS:
		if (uws != null){
			uws.destroy();
			uws = null;
		}
		super.destroy();
	}

	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException{
		if (uws != null){
			try{
				uws.executeRequest(req, resp);
			}catch(Throwable t){
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
			}
		}else
			resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "UWS service not yet initialized!");
	}

}
