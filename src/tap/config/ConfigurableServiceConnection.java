package tap.config;

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
 * Copyright 2016-2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.db.FunctionDef;
import adql.db.STCS;
import adql.parser.ParseException;
import adql.query.operand.function.UserDefinedFunction;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPFactory;
import tap.db.DBConnection;
import tap.db.JDBCConnection;
import tap.formatter.*;
import tap.log.DefaultTAPLog;
import tap.log.Slf4jTAPLog;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import tap.metadata.TableSetParser;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uws.UWSException;
import uws.service.UserIdentifier;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;
import uws.service.log.UWSLog.LogLevel;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tap.config.TAPConfiguration.*;

/**
 * Concrete implementation of {@link ServiceConnection}, fully parameterized
 * with a TAP configuration file.
 *
 * <p>
 * 	Every aspects of the TAP service are configured here. This instance is also
 * 	creating the {@link TAPFactory} using the TAP configuration file thanks to
 * 	the implementation {@link ConfigurableTAPFactory}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (11/2021)
 * @since 2.0
 */
public final class ConfigurableServiceConnection implements ServiceConnection {

	/** File manager to use in the TAP service. */
	private UWSFileManager fileManager;

	/** Object to use in the TAP service in order to log different types of
	 * messages (e.g. DEBUG, INFO, WARNING, ERROR, FATAL). */
	private TAPLog logger;

	/** Factory which can create different types of objects for the TAP service
	 * (e.g. database connection). */
	private TAPFactory tapFactory;

	/** Object gathering all metadata of this TAP service. */
	private final TAPMetadata metadata;

	/** Name of the organization/person providing the TAP service.  */
	private final String providerName;
	/** Description of the TAP service. */
	private final String serviceDescription;

	/** Base/Root URL of the TAP service.
	 * @since 2.4 */
	private final URL baseURL;

	/** Indicate whether the TAP service is available or not. */
	private boolean isAvailable = false;	// the TAP service must be disabled until the end of its connection initialization
	/** Description of the available or unavailable state of the TAP service. */
	private String availability = "TAP service not yet initialized.";

	/** Maximum number of asynchronous jobs that can run simultaneously. */
	private int maxAsyncJobs = DEFAULT_MAX_ASYNC_JOBS;

	/** Array of 3 integers: resp. default, maximum and sync. execution
	 * durations. <em>All durations are expressed in milliseconds.</em> */
	private int[] executionDuration = new int[3];
	/** Array of 2 integers: resp. default and maximum retention period.
	 * <em>Both period are expressed in seconds.</em> */
	private int[] retentionPeriod = new int[2];

	/** List of all available output formatters. */
	private final ArrayList<OutputFormat> outputFormats;

	/** Array of 2 integers: resp. default and maximum output limit.
	 * <em>Each limit is expressed in a unit specified in the array
	 * {@link #outputLimitTypes}.</em> */
	private int[] outputLimits = new int[]{ -1, -1 };
	/** Array of 2 limit units: resp. unit of the default output limit and unit
	 * of the maximum output limit. */
	private LimitUnit[] outputLimitTypes = new LimitUnit[2];

	/** Indicate whether the UPLOAD feature is enabled or not. */
	private boolean isUploadEnabled = false;
	/** Array of 2 integers: resp. default and maximum upload limit.
	 * <p><em>Each limit is expressed in a unit specified in the array
	 * {@link #uploadLimitTypes}.</em></p> */
	private long[] uploadLimits = new long[]{ -1L, -1L };
	/** Array of 2 limit units: resp. unit of the default upload limit and unit
	 * of the maximum upload limit. */
	private LimitUnit[] uploadLimitTypes = new LimitUnit[2];
	/** The maximum size of a set of uploaded files.
	 * <p><em>This size is expressed in bytes.</em></p> */
	private long maxUploadSize = DEFAULT_UPLOAD_MAX_REQUEST_SIZE;

	/** Array of 2 integers: resp. default and maximum fetch size.
	 * <em>Both sizes are expressed in number of rows.</em> */
	private int[] fetchSize = new int[]{ DEFAULT_ASYNC_FETCH_SIZE, DEFAULT_SYNC_FETCH_SIZE };

	/** The method to use in order to identify a TAP user. */
	private UserIdentifier userIdentifier = null;

	/** List of all allowed coordinate systems.
	 * <em>
	 * 	If NULL, all coord. sys. are allowed. If empty list, none is allowed.
	 * </em> */
	private ArrayList<String> lstCoordSys = null;

	/** List of all allowed ADQL geometrical functions.
	 * <em>
	 * 	If NULL, all geometries are allowed. If empty list, none is allowed.
	 * </em> */
	private ArrayList<String> geometries = null;
	private final String GEOMETRY_REGEXP = "(AREA|BOX|CENTROID|CIRCLE|CONTAINS|DISTANCE|COORD1|COORD2|COORDSYS|INTERSECTS|POINT|POLYGON|REGION)";

	/** List of all known and allowed User Defined Functions.
	 * <em>If NULL, any unknown function is allowed. If empty list, none is
	 * allowed.</em> */
	private Collection<FunctionDef> udfs = new ArrayList<FunctionDef>(0);

	/** Indicate whether the input ADQL query should be automatically fixed
	 * if its parsing fails because of an incorrect tokenization.
	 * @since 2.3 */
	private boolean isFixOnFailEnabled = DEFAULT_FIX_ON_FAIL;

	/**
	 * Create a TAP service description thanks to the given TAP configuration
	 * file.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws NullPointerException	If the given properties set is NULL.
	 * @throws TAPException			If a property is wrong or missing.
	 */
	public ConfigurableServiceConnection(final Properties tapConfig) throws NullPointerException, TAPException {
		this(tapConfig, null);
	}

	/**
	 * Create a TAP service description thanks to the given TAP configuration
	 * file.
	 *
	 * @param tapConfig		The content of the TAP configuration file.
	 * @param webAppRootDir	The directory of the Web Application running this
	 *                     	TAP service. <em>In this directory another directory
	 *                     	may be created in order to store all TAP service
	 *                     	files if none is specified in the given TAP
	 *                     	configuration file.</em>
	 *
	 * @throws NullPointerException	If the given properties set is NULL.
	 * @throws TAPException			If a property is wrong or missing.
	 */
	public ConfigurableServiceConnection(final Properties tapConfig, final String webAppRootDir) throws NullPointerException, TAPException {
		if (tapConfig == null)
			throw new NullPointerException("Missing TAP properties! ");

		// 1. INITIALIZE THE FILE MANAGER:
		initFileManager(tapConfig, webAppRootDir);

		// 2. CREATE THE LOGGER:
		initLogger(tapConfig);

		// 3. BUILD THE TAP FACTORY:
		initFactory(tapConfig);

		// 4. GET THE METADATA:
		metadata = initMetadata(tapConfig, webAppRootDir);

		// 6. SET ALL GENERAL SERVICE CONNECTION INFORMATION:
		providerName = getProperty(tapConfig, KEY_PROVIDER_NAME);
		serviceDescription = getProperty(tapConfig, KEY_SERVICE_DESCRIPTION);
		initMaxAsyncJobs(tapConfig);
		initRetentionPeriod(tapConfig);
		initExecutionDuration(tapConfig);

		// 7. CONFIGURE OUTPUT:
		// default output format = VOTable:
		outputFormats = new ArrayList<OutputFormat>(1);
		// set output formats:
		addOutputFormats(tapConfig);
		// set output limits:
		initOutputLimits(tapConfig);
		// set fetch size:
		initFetchSize(tapConfig);

		// 8. CONFIGURE THE UPLOAD:
		// is upload enabled ?
		isUploadEnabled = Boolean.parseBoolean(getProperty(tapConfig, KEY_UPLOAD_ENABLED));
		// set upload limits:
		initUploadLimits(tapConfig);
		// set the maximum upload file size:
		initMaxUploadSize(tapConfig);

		// 9. SET A USER IDENTIFIER:
		initUserIdentifier(tapConfig);

		// 10. CONFIGURE ADQL:
		initCoordSys(tapConfig);
		initADQLGeometries(tapConfig);
		initUDFs(tapConfig);
		isFixOnFailEnabled = Boolean.parseBoolean(getProperty(tapConfig, KEY_FIX_ON_FAIL));

		// 11. BASE URL:
		baseURL = initBaseURL(tapConfig);
	}

	/**
	 * Parse and return the base/root URL specified in the configuration.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @return	The corresponding URL,
	 *        	or NULL, if none is specified.
	 *
	 * @throws TAPException	If the specified URL is invalid.
	 *
	 * @since 2.4
	 */
	private URL initBaseURL(final Properties tapConfig) throws TAPException {
		try {
			final String value = getProperty(tapConfig, KEY_BASE_URL);
			return (value == null) ? null : new URL(value);
		}
		catch (MalformedURLException mue) {
			throw new TAPException("Incorrect URL for the property \""+KEY_BASE_URL+"\"! Cause: "+mue.getMessage());
		}
	}

	/**
	 * Initialize the management of TAP service files using the given TAP configuration file.
	 *
	 * @param tapConfig		The content of the TAP configuration file.
	 * @param webAppRootDir	The directory of the Web Application running this TAP service.
	 *                     	<em>This directory may be used only to search the root TAP directory
	 *                     	if specified with a relative path in the TAP configuration file.</em>
	 *
	 * @throws TAPException	If a property is wrong or missing, or if an error occurs while creating the file manager.
	 */
	private void initFileManager(final Properties tapConfig, final String webAppRootDir) throws TAPException {
		// Read the desired file manager:
		String fileManagerType = getProperty(tapConfig, KEY_FILE_MANAGER);
		if (fileManagerType == null)
			throw new TAPException("The property \"" + KEY_FILE_MANAGER + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_LOCAL + " or a class name between {...}.");
		else
			fileManagerType = fileManagerType.trim();

		// LOCAL file manager:
		if (fileManagerType.equalsIgnoreCase(VALUE_LOCAL)) {
			// Read the desired root path:
			String rootPath = getProperty(tapConfig, KEY_FILE_ROOT_PATH);
			if (rootPath == null)
				throw new TAPException("The property \"" + KEY_FILE_ROOT_PATH + "\" is missing! It is required to create a TAP Service. Please provide a path toward a directory which will contain all files related to the service.");
			File rootFile = getFile(rootPath, webAppRootDir, KEY_FILE_ROOT_PATH);

			// Determine whether there should be one directory for each user:
			String propValue = getProperty(tapConfig, KEY_DIRECTORY_PER_USER);
			boolean oneDirectoryPerUser = (propValue == null) ? DEFAULT_DIRECTORY_PER_USER : Boolean.parseBoolean(propValue);

			// Determine whether there should be one directory for each user:
			propValue = getProperty(tapConfig, KEY_GROUP_USER_DIRECTORIES);
			boolean groupUserDirectories = (propValue == null) ? DEFAULT_GROUP_USER_DIRECTORIES : Boolean.parseBoolean(propValue);

			// Build the Local TAP File Manager:
			try {
				fileManager = new LocalUWSFileManager(rootFile, oneDirectoryPerUser, groupUserDirectories);
			} catch(UWSException e) {
				throw new TAPException("The property \"" + KEY_FILE_ROOT_PATH + "\" (" + rootPath + ") is incorrect: " + e.getMessage());
			}
		}
		// CUSTOM file manager:
		else
			fileManager = newInstance(fileManagerType, KEY_FILE_MANAGER, UWSFileManager.class, new Class<?>[]{ Properties.class }, new Object[]{ tapConfig });
	}

	/**
	 * <p>Resolve the given file name/path.</p>
	 *
	 * <p>
	 * 	If not an absolute path, the given path may be either relative or absolute. A relative path is always considered
	 * 	as relative from the Web Application directory (supposed to be given in 2nd parameter).
	 * </p>
	 *
	 * @param filePath			Path/Name of the file to get.
	 * @param webAppRootPath	Web Application directory local path.
	 * @param propertyName		Name of the property which gives the given file path.
	 *
	 * @return	The specified File instance.
	 *
	 * @throws ParseException	If the given file path is a URI/URL.
	 */
	protected static final File getFile(final String filePath, final String webAppRootPath, final String propertyName) throws TAPException {
		if (filePath == null)
			return null;
		else if (filePath.matches(".*:.*"))
			throw new TAPException("Incorrect file path for the property \"" + propertyName + "\": \"" + filePath + "\"! URI/URLs are not expected here.");

		File f = new File(filePath);
		if (f.isAbsolute())
			return f;
		else
			return new File(webAppRootPath, filePath);
	}

	/**
	 * Initialise the TAP logger with the given TAP configuration file.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If no instance of the specified custom logger can
	 *                     	be created.
	 */
	private void initLogger(final Properties tapConfig) throws TAPException {
		// Create the logger:
		String propValue = getProperty(tapConfig, KEY_LOGGER);
		if (propValue == null || propValue.trim().equalsIgnoreCase(DEFAULT_LOGGER))
			logger = new DefaultTAPLog(fileManager);
		else if (propValue == null || propValue.trim().equalsIgnoreCase(SLF4J_LOGGER))
			logger = new Slf4jTAPLog();
		else
			logger = newInstance(propValue, KEY_LOGGER, TAPLog.class, new Class<?>[]{ UWSFileManager.class }, new Object[]{ fileManager });

		// Set some options for the default logger:
		if (propValue == null || propValue.trim().equalsIgnoreCase(DEFAULT_LOGGER)) {

			// Set the minimum log level:
			propValue = getProperty(tapConfig, KEY_MIN_LOG_LEVEL);
			if (propValue != null) {
				try {
					((DefaultTAPLog)logger).setMinLogLevel(LogLevel.valueOf(propValue.toUpperCase()));
				} catch(IllegalArgumentException iae) {
				}
			}

			// Set the log rotation period, if any:
			if (fileManager instanceof LocalUWSFileManager) {
				propValue = getProperty(tapConfig, KEY_LOG_ROTATION);
				if (propValue != null)
					((LocalUWSFileManager)fileManager).setLogRotationFreq(propValue);
			}
		}

		// Log the successful initialisation of the logger:
		logger.info("Logger initialized - {" + logger.getConfigString() + "}");
	}

	/**
	 * <p>Initialize the {@link TAPFactory} to use.</p>
	 *
	 * <p>
	 * 	The built factory is either a {@link ConfigurableTAPFactory} instance (by default) or
	 * 	an instance of the class specified in the TAP configuration file.
	 * </p>
	 *
	 * @param tapConfig		The content of the TAP configuration file.
	 *
	 * @throws TAPException	If an error occurs while building the specified {@link TAPFactory}.
	 *
	 * @see ConfigurableTAPFactory
	 */
	private void initFactory(final Properties tapConfig) throws TAPException {
		String propValue = getProperty(tapConfig, KEY_TAP_FACTORY);
		if (propValue == null)
			tapFactory = new ConfigurableTAPFactory(this, tapConfig);
		else if (hasConstructor(propValue, KEY_TAP_FACTORY, TAPFactory.class, new Class<?>[]{ ServiceConnection.class, Properties.class }))
			tapFactory = newInstance(propValue, KEY_TAP_FACTORY, TAPFactory.class, new Class<?>[]{ ServiceConnection.class, Properties.class }, new Object[]{ this, tapConfig });
		else
			tapFactory = newInstance(propValue, KEY_TAP_FACTORY, TAPFactory.class, new Class<?>[]{ ServiceConnection.class }, new Object[]{ this });
	}

	/**
	 * Initialize the TAP metadata (i.e. database schemas, tables and columns and their attached metadata).
	 *
	 * @param tapConfig		The content of the TAP configuration file.
	 * @param webAppRootDir	Web Application directory local path.
	 *                     	<em>This directory may be used if a relative path is given for an XML metadata file.</em>
	 *
	 * @return	The extracted TAP metadata.
	 *
	 * @throws TAPException	If some TAP configuration file properties are wrong or missing,
	 *                     	or if an error has occurred while extracting the metadata from the database or the XML file.
	 *
	 * @see DBConnection#getTAPSchema()
	 * @see TableSetParser
	 */
	private TAPMetadata initMetadata(final Properties tapConfig, final String webAppRootDir) throws TAPException {
		// Get the fetching method to use:
		String metaFetchType = getProperty(tapConfig, KEY_METADATA);
		if (metaFetchType == null)
			throw new TAPException("The property \"" + KEY_METADATA + "\" is missing! It is required to create a TAP Service. Three possible values: " + VALUE_XML + " (to get metadata from a TableSet XML document), " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA) or the name (between {}) of a class extending TAPMetadata. Only " + VALUE_XML + " and " + VALUE_DB + " can be followed by the path of a class extending TAPMetadata.");

		// Extract a custom class suffix if any for XML and DB options:
		String customMetaClass = null;
		if (metaFetchType.toLowerCase().matches("(" + VALUE_XML + "|" + VALUE_DB + ").*")) {
			int indSep = metaFetchType.toLowerCase().startsWith(VALUE_XML) ? 3 : 2;
			customMetaClass = metaFetchType.substring(indSep).trim();
			metaFetchType = metaFetchType.substring(0, indSep);
			if (customMetaClass.length() == 0)
				customMetaClass = null;
			else if (!isClassName(customMetaClass))
				throw new TAPException("Unexpected string after the fetching method \"" + metaFetchType + "\": \"" + customMetaClass + "\"! The full name of a class extending TAPMetadata was expected. If it is a class name, then it must be specified between {}.");
		}

		TAPMetadata metadata = null;

		// GET METADATA FROM XML & UPDATE THE DATABASE (schema TAP_SCHEMA only):
		if (metaFetchType.equalsIgnoreCase(VALUE_XML)) {
			// Get the XML file path:
			String xmlFilePath = getProperty(tapConfig, KEY_METADATA_FILE);
			if (xmlFilePath == null)
				throw new TAPException("The property \"" + KEY_METADATA_FILE + "\" is missing! According to the property \"" + KEY_METADATA + "\", metadata must be fetched from an XML document. The local file path of it MUST be provided using the property \"" + KEY_METADATA_FILE + "\".");

			// Parse the XML document and build the corresponding metadata:
			try {
				metadata = (new TableSetParser()).parse(getFile(xmlFilePath, webAppRootDir, KEY_METADATA_FILE));
			} catch(IOException ioe) {
				throw new TAPException("A grave error occurred while reading/parsing the TableSet XML document: \"" + xmlFilePath + "\"!", ioe);
			}

			// Update the database:
			DBConnection conn = null;
			try {
				conn = tapFactory.getConnection("SET_TAP_SCHEMA");
				conn.setTAPSchema(metadata);
			} finally {
				if (conn != null)
					tapFactory.freeConnection(conn);
			}
		}
		// GET METADATA FROM DATABASE (schema TAP_SCHEMA):
		else if (metaFetchType.equalsIgnoreCase(VALUE_DB)) {
			DBConnection conn = null;
			try {
				// get a db connection:
				conn = tapFactory.getConnection("GET_TAP_SCHEMA");

				// fetch and set the ADQL<->DB mapping for all standard TAP_SCHEMA items:
				if (conn instanceof JDBCConnection) {
					HashMap<String, String> dbMapping = new HashMap<String, String>(10);
					// fetch the mapping from the Property file:
					for(String key : tapConfig.stringPropertyNames()) {
						if (key.trim().startsWith("TAP_SCHEMA") && tapConfig.getProperty(key) != null && tapConfig.getProperty(key).trim().length() > 0)
							dbMapping.put(key.trim(), tapConfig.getProperty(key));
					}
					// set the mapping into the DB connection:
					((JDBCConnection)conn).setDBMapping(dbMapping);
				}

				// fetch TAP_SCHEMA:
				metadata = conn.getTAPSchema();
			} finally {
				if (conn != null)
					tapFactory.freeConnection(conn);
			}
		}
		// MANUAL ~ TAPMETADATA CLASS
		else if (isClassName(metaFetchType)) {
			/* 1. Get the metadata */
			// get the class:
			Class<? extends TAPMetadata> metaClass = fetchClass(metaFetchType, KEY_METADATA, TAPMetadata.class);
			if (metaClass == TAPMetadata.class)
				throw new TAPException("Wrong class for the property \"" + KEY_METADATA + "\": \"" + metaClass.getName() + "\"! The class provided in this property MUST EXTEND tap.metadata.TAPMetadata.");
			try {
				// get one of the expected constructors:
				try {
					// (UWSFileManager, TAPFactory, TAPLog):
					Constructor<? extends TAPMetadata> constructor = metaClass.getConstructor(UWSFileManager.class, TAPFactory.class, TAPLog.class);
					// create the TAP metadata:
					metadata = constructor.newInstance(fileManager, tapFactory, logger);
				} catch(NoSuchMethodException nsme) {
					// () (empty constructor):
					Constructor<? extends TAPMetadata> constructor = metaClass.getConstructor();
					// create the TAP metadata:
					metadata = constructor.newInstance();
				}
			} catch(NoSuchMethodException nsme) {
				throw new TAPException("Missing constructor tap.metadata.TAPMetadata() or tap.metadata.TAPMetadata(uws.service.file.UWSFileManager, tap.TAPFactory, tap.log.TAPLog)! See the value \"" + metaFetchType + "\" of the property \"" + KEY_METADATA + "\".");
			} catch(InstantiationException ie) {
				throw new TAPException("Impossible to create an instance of an abstract class: \"" + metaClass.getName() + "\"! See the value \"" + metaFetchType + "\" of the property \"" + KEY_METADATA + "\".");
			} catch(InvocationTargetException ite) {
				if (ite.getCause() != null) {
					if (ite.getCause() instanceof TAPException)
						throw (TAPException)ite.getCause();
					else
						throw new TAPException(ite.getCause());
				} else
					throw new TAPException(ite);
			} catch(Exception ex) {
				throw new TAPException("Impossible to create an instance of tap.metadata.TAPMetadata as specified in the property \"" + KEY_METADATA + "\": \"" + metaFetchType + "\"!", ex);
			}

			/* 2. Update the database */
			DBConnection conn = null;
			try {
				conn = tapFactory.getConnection("SET_TAP_SCHEMA");
				conn.setTAPSchema(metadata);
			} finally {
				if (conn != null)
					tapFactory.freeConnection(conn);
			}
		}
		// INCORRECT VALUE => ERROR!
		else
			throw new TAPException("Unsupported value for the property \"" + KEY_METADATA + "\": \"" + metaFetchType + "\"! Only two values are allowed: " + VALUE_XML + " (to get metadata from a TableSet XML document) or " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA). Only " + VALUE_XML + " and " + VALUE_DB + " can be followed by the path of a class extending TAPMetadata.");

		// Create the custom TAPMetadata extension if any is provided (THEORETICALLY, JUST FOR XML and DB):
		if (customMetaClass != null) {
			// get the class:
			Class<? extends TAPMetadata> metaClass = fetchClass(customMetaClass, KEY_METADATA, TAPMetadata.class);
			if (metaClass == TAPMetadata.class)
				throw new TAPException("Wrong class for the property \"" + KEY_METADATA + "\": \"" + metaClass.getName() + "\"! The class provided in this property MUST EXTEND tap.metadata.TAPMetadata.");
			try {
				// get one of the expected constructors:
				try {
					// (TAPMetadata, UWSFileManager, TAPFactory, TAPLog):
					Constructor<? extends TAPMetadata> constructor = metaClass.getConstructor(TAPMetadata.class, UWSFileManager.class, TAPFactory.class, TAPLog.class);
					// create the TAP metadata:
					metadata = constructor.newInstance(metadata, fileManager, tapFactory, logger);
				} catch(NoSuchMethodException nsme) {
					// (TAPMetadata):
					Constructor<? extends TAPMetadata> constructor = metaClass.getConstructor(TAPMetadata.class);
					// create the TAP metadata:
					metadata = constructor.newInstance(metadata);
				}
			} catch(NoSuchMethodException nsme) {
				throw new TAPException("Missing constructor by copy tap.metadata.TAPMetadata(tap.metadata.TAPMetadata) or tap.metadata.TAPMetadata(tap.metadata.TAPMetadata, uws.service.file.UWSFileManager, tap.TAPFactory, tap.log.TAPLog)! See the value \"" + metaFetchType + "\" of the property \"" + KEY_METADATA + "\".");
			} catch(InstantiationException ie) {
				throw new TAPException("Impossible to create an instance of an abstract class: \"" + metaClass.getName() + "\"! See the value \"" + metaFetchType + "\" of the property \"" + KEY_METADATA + "\".");
			} catch(InvocationTargetException ite) {
				if (ite.getCause() != null) {
					if (ite.getCause() instanceof TAPException)
						throw (TAPException)ite.getCause();
					else
						throw new TAPException(ite.getCause());
				} else
					throw new TAPException(ite);
			} catch(Exception ex) {
				throw new TAPException("Impossible to create an instance of tap.metadata.TAPMetadata as specified in the property \"" + KEY_METADATA + "\": \"" + metaFetchType + "\"!", ex);
			}
		}

		return metadata;
	}

	/**
	 * Initialize the maximum number of asynchronous jobs.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration property is wrong.
	 */
	private void initMaxAsyncJobs(final Properties tapConfig) throws TAPException {
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_MAX_ASYNC_JOBS);
		try {
			// If a value is provided, cast it into an integer and set the attribute:
			maxAsyncJobs = (propValue == null) ? DEFAULT_MAX_ASYNC_JOBS : Integer.parseInt(propValue);
		} catch(NumberFormatException nfe) {
			throw new TAPException("Integer expected for the property \"" + KEY_MAX_ASYNC_JOBS + "\", instead of: \"" + propValue + "\"!");
		}
	}

	/**
	 * Initialize the default and maximum retention period.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void initRetentionPeriod(final Properties tapConfig) throws TAPException {
		retentionPeriod = new int[2];

		// Set the default period:
		String propValue = getProperty(tapConfig, KEY_DEFAULT_RETENTION_PERIOD);
		try {
			retentionPeriod[0] = (propValue == null) ? DEFAULT_RETENTION_PERIOD : Integer.parseInt(propValue);
		} catch(NumberFormatException nfe) {
			throw new TAPException("Integer expected for the property \"" + KEY_DEFAULT_RETENTION_PERIOD + "\", instead of: \"" + propValue + "\"!");
		}

		// Set the maximum period:
		propValue = getProperty(tapConfig, KEY_MAX_RETENTION_PERIOD);
		try {
			retentionPeriod[1] = (propValue == null) ? DEFAULT_RETENTION_PERIOD : Integer.parseInt(propValue);
		} catch(NumberFormatException nfe) {
			throw new TAPException("Integer expected for the property \"" + KEY_MAX_RETENTION_PERIOD + "\", instead of: \"" + propValue + "\"!");
		}

		// The maximum period MUST be greater or equals than the default period.
		// If not, the default period is set (so decreased) to the maximum period.
		if (retentionPeriod[1] > 0 && retentionPeriod[1] < retentionPeriod[0])
			retentionPeriod[0] = retentionPeriod[1];
	}

	/**
	 * Initialize the default and maximum execution duration.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void initExecutionDuration(final Properties tapConfig) throws TAPException {
		executionDuration = new int[3];

		// Set the default duration:
		String propValue = getProperty(tapConfig, KEY_DEFAULT_EXECUTION_DURATION);
		try {
			executionDuration[0] = (propValue == null) ? DEFAULT_EXECUTION_DURATION : Integer.parseInt(propValue);
		} catch(NumberFormatException nfe) {
			throw new TAPException("Integer expected for the property \"" + KEY_DEFAULT_EXECUTION_DURATION + "\", instead of: \"" + propValue + "\"!");
		}

		// Set the maximum duration:
		propValue = getProperty(tapConfig, KEY_MAX_EXECUTION_DURATION);
		try {
			executionDuration[1] = (propValue == null) ? DEFAULT_EXECUTION_DURATION : Integer.parseInt(propValue);
		} catch(NumberFormatException nfe) {
			throw new TAPException("Integer expected for the property \"" + KEY_MAX_EXECUTION_DURATION + "\", instead of: \"" + propValue + "\"!");
		}

		// Set the synchronous duration:
		propValue = getProperty(tapConfig, KEY_SYNC_EXECUTION_DURATION);
		try {
			executionDuration[2] = (propValue == null) ? DEFAULT_EXECUTION_DURATION : Integer.parseInt(propValue);
		} catch(NumberFormatException nfe) {
			throw new TAPException("Integer expected for the property \"" + KEY_SYNC_EXECUTION_DURATION + "\", instead of: \"" + propValue + "\"!");
		}

		/* The maximum duration MUST be greater or equals than the default
		 * duration. If not, the default duration is set (so decreased) to the
		 * maximum duration: */
		if (executionDuration[1] > 0 && executionDuration[1] < executionDuration[0])
			executionDuration[0] = executionDuration[1];

		/* The synchronous duration MUST be less or equals than the default
		 * duration (or the max if no default is set). If not, the sync.
		 * duration is set (so decreased) to the default duration (or max if no
		 * default): */
		if (executionDuration[0] > 0 && executionDuration[0] < executionDuration[2])
			executionDuration[2] = executionDuration[0];
		else if (executionDuration[0] <= 0 && executionDuration[1] > 0 && executionDuration[1] < executionDuration[2])
			executionDuration[2] = executionDuration[1];
	}

	/**
	 * <p>Initialize the list of all output format that the TAP service must support.</p>
	 *
	 * <p>
	 * 	This function ensures that at least one VOTable format is part of the returned list,
	 * 	even if none has been specified in the TAP configuration file. Indeed, the VOTable format is the only
	 * 	format required for a TAP service.
	 * </p>
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void addOutputFormats(final Properties tapConfig) throws TAPException {
		// Fetch the value of the property for additional output formats:
		String formats = getProperty(tapConfig, KEY_OUTPUT_FORMATS);

		// SPECIAL VALUE "ALL":
		if (formats == null || formats.equalsIgnoreCase(VALUE_ALL)) {
			outputFormats.add(new VOTableFormat(this, DataFormat.BINARY));
			outputFormats.add(new VOTableFormat(this, DataFormat.BINARY2));
			outputFormats.add(new VOTableFormat(this, DataFormat.TABLEDATA));
			outputFormats.add(new VOTableFormat(this, DataFormat.FITS));
			outputFormats.add(new FITSFormat(this));
			outputFormats.add(new JSONFormat(this));
			outputFormats.add(new SVFormat(this, ",", true));
			outputFormats.add(new SVFormat(this, "\t", true));
			outputFormats.add(new TextFormat(this));
			outputFormats.add(new HTMLFormat(this));
			return;
		}

		// LIST OF FORMATS:
		// Since it is a comma separated list of output formats, a loop will parse this list comma by comma:
		String f;
		int indexSep, indexLPar, indexRPar;
		boolean hasVotableFormat = false;
		while(formats != null && formats.length() > 0) {
			// Get a format item from the list:
			indexSep = formats.indexOf(',');
			// if a comma is after a left parenthesis
			indexLPar = formats.indexOf('(');
			if (indexSep > 0 && indexLPar > 0 && indexSep > indexLPar) {
				indexRPar = formats.indexOf(')', indexLPar);
				if (indexRPar > 0)
					indexSep = formats.indexOf(',', indexRPar);
				else
					throw new TAPException("Missing right parenthesis in: \"" + formats + "\"!");
			}
			// no comma => only one format
			if (indexSep < 0) {
				f = formats;
				formats = null;
			}
			// comma at the first position => empty list item => go to the next item
			else if (indexSep == 0) {
				formats = formats.substring(1).trim();
				continue;
			}
			// else => get the first format item, and then remove it from the list for the next iteration
			else {
				f = formats.substring(0, indexSep).trim();
				formats = formats.substring(indexSep + 1).trim();
			}

			// Identify the format and append it to the output format list of the service:
			// FITS
			if (f.equalsIgnoreCase(VALUE_FITS))
				outputFormats.add(new FITSFormat(this));
			// JSON
			else if (f.equalsIgnoreCase(VALUE_JSON))
				outputFormats.add(new JSONFormat(this));
			// HTML
			else if (f.equalsIgnoreCase(VALUE_HTML))
				outputFormats.add(new HTMLFormat(this));
			// TEXT
			else if (f.equalsIgnoreCase(VALUE_TEXT))
				outputFormats.add(new TextFormat(this));
			// CSV
			else if (f.equalsIgnoreCase(VALUE_CSV))
				outputFormats.add(new SVFormat(this, ",", true));
			// TSV
			else if (f.equalsIgnoreCase(VALUE_TSV))
				outputFormats.add(new SVFormat(this, "\t", true));
			// any SV (separated value) format
			else if (f.toLowerCase().startsWith(VALUE_SV)) {
				// get the separator:
				int endSep = f.indexOf(')');
				if (VALUE_SV.length() < f.length() && f.charAt(VALUE_SV.length()) == '(' && endSep > VALUE_SV.length() + 1) {
					String separator = f.substring(VALUE_SV.length() + 1, f.length() - 1);
					// get the MIME type and its alias, if any of them is provided:
					String mimeType = null, shortMimeType = null;
					if (endSep + 1 < f.length() && f.charAt(endSep + 1) == ':') {
						int endMime = f.indexOf(':', endSep + 2);
						if (endMime < 0)
							mimeType = f.substring(endSep + 2, f.length());
						else if (endMime > 0) {
							mimeType = f.substring(endSep + 2, endMime);
							shortMimeType = f.substring(endMime + 1);
						}
					}
					// add the defined SV(...) format:
					outputFormats.add(new SVFormat(this, separator, true, mimeType, shortMimeType));
				} else
					throw new TAPException("Missing separator char/string for the SV output format: \"" + f + "\"!");
			}
			// VOTABLE
			else if (f.toLowerCase().startsWith(VALUE_VOTABLE) || f.toLowerCase().startsWith(VALUE_VOT)) {
				// Parse the format:
				VOTableFormat votFormat = parseVOTableFormat(f);

				// Add the VOTable format:
				outputFormats.add(votFormat);

				// Determine whether the MIME type is the VOTable expected one:
				if (votFormat.getShortMimeType().equals("votable") || votFormat.getMimeType().equals("votable"))
					hasVotableFormat = true;
			}
			// custom OutputFormat
			else if (isClassName(f))
				outputFormats.add(TAPConfiguration.newInstance(f, KEY_OUTPUT_FORMATS, OutputFormat.class, new Class<?>[]{ ServiceConnection.class }, new Object[]{ this }));
			// unknown format
			else
				throw new TAPException("Unknown output format: " + f);
		}

		// Add by default VOTable format if none is specified:
		if (!hasVotableFormat)
			outputFormats.add(new VOTableFormat(this));
	}

	/**
	 * <p>Parse the given VOTable format specification.</p>
	 *
	 * <p>This specification is expected to be an item of the property {@link TAPConfiguration#KEY_OUTPUT_FORMATS}.</p>
	 *
	 * @param propValue	A single VOTable format specification.
	 *
	 * @return	The corresponding configured {@link VOTableFormat} instance.
	 *
	 * @throws TAPException	If the syntax of the given specification is incorrect,
	 *                     	or if the specified VOTable version or serialization does not exist.
	 */
	private VOTableFormat parseVOTableFormat(final String propValue) throws TAPException {
		DataFormat serialization = null;
		VOTableVersion votVersion = null;
		String mimeType = null, shortMimeType = null;

		// Get the parameters, if any:
		int beginSep = propValue.indexOf('(');
		if (beginSep > 0) {
			int endSep = propValue.indexOf(')');
			if (endSep <= beginSep)
				throw new TAPException("Wrong output format specification syntax in: \"" + propValue + "\"! A VOTable parameters list must end with ')'.");
			// split the parameters:
			String[] params = propValue.substring(beginSep + 1, endSep).split(",");
			if (params.length > 2)
				throw new TAPException("Wrong number of parameters for the output format VOTable: \"" + propValue + "\"! Only two parameters may be provided: serialization and version.");
			else if (params.length >= 1) {
				// resolve the serialization format:
				params[0] = params[0].trim().toLowerCase();
				if (params[0].length() == 0 || params[0].equals("b") || params[0].equals("binary"))
					serialization = DataFormat.BINARY;
				else if (params[0].equals("b2") || params[0].equals("binary2"))
					serialization = DataFormat.BINARY2;
				else if (params[0].equals("td") || params[0].equals("tabledata"))
					serialization = DataFormat.TABLEDATA;
				else if (params[0].equals("fits"))
					serialization = DataFormat.FITS;
				else
					throw new TAPException("Unsupported VOTable serialization: \"" + params[0] + "\"! Accepted values: 'binary' (or 'b'), 'binary2' (or 'b2'), 'tabledata' (or 'td') and 'fits'.");
				// resolve the version:
				if (params.length == 2) {
					params[1] = params[1].trim();
					if (params[1].equals("1.0") || params[1].equalsIgnoreCase("v1.0"))
						votVersion = VOTableVersion.V10;
					else if (params[1].equals("1.1") || params[1].equalsIgnoreCase("v1.1"))
						votVersion = VOTableVersion.V11;
					else if (params[1].equals("1.2") || params[1].equalsIgnoreCase("v1.2"))
						votVersion = VOTableVersion.V12;
					else if (params[1].equals("1.3") || params[1].equalsIgnoreCase("v1.3"))
						votVersion = VOTableVersion.V13;
					else
						throw new TAPException("Unsupported VOTable version: \"" + params[1] + "\"! Accepted values: '1.0' (or 'v1.0'), '1.1' (or 'v1.1'), '1.2' (or 'v1.2') and '1.3' (or 'v1.3').");
				}
			}
		}

		// Get the MIME type and its alias, if any:
		beginSep = propValue.indexOf(':');
		if (beginSep > 0) {
			int endSep = propValue.indexOf(':', beginSep + 1);
			if (endSep < 0)
				endSep = propValue.length();
			// extract the MIME type, if any:
			mimeType = propValue.substring(beginSep + 1, endSep).trim();
			if (mimeType.length() == 0)
				mimeType = null;
			// extract the short MIME type, if any:
			if (endSep < propValue.length()) {
				beginSep = endSep;
				endSep = propValue.indexOf(':', beginSep + 1);
				if (endSep >= 0)
					throw new TAPException("Wrong output format specification syntax in: \"" + propValue + "\"! After a MIME type and a short MIME type, no more information is expected.");
				else
					endSep = propValue.length();
				shortMimeType = propValue.substring(beginSep + 1, endSep).trim();
				if (shortMimeType.length() == 0)
					shortMimeType = null;
			}
		}

		// Create the VOTable format:
		VOTableFormat votFormat = new VOTableFormat(this, serialization, votVersion);
		votFormat.setMimeType(mimeType, shortMimeType);

		return votFormat;
	}

	/**
	 * Initialize the default and maximum output limits.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void initOutputLimits(final Properties tapConfig) throws TAPException {
		Object[] limit = parseLimit(getProperty(tapConfig, KEY_DEFAULT_OUTPUT_LIMIT), KEY_DEFAULT_OUTPUT_LIMIT, false);
		outputLimitTypes[0] = (LimitUnit)limit[1];	// it should be "rows" since the parameter areBytesAllowed of parseLimit =false
		setDefaultOutputLimit((Integer)limit[0]);

		limit = parseLimit(getProperty(tapConfig, KEY_MAX_OUTPUT_LIMIT), KEY_DEFAULT_OUTPUT_LIMIT, false);
		outputLimitTypes[1] = (LimitUnit)limit[1];	// it should be "rows" since the parameter areBytesAllowed of parseLimit =false
		setMaxOutputLimit((Integer)limit[0]);
	}

	/**
	 * Initialize the fetch size for the synchronous and for the asynchronous resources.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void initFetchSize(final Properties tapConfig) throws TAPException {
		fetchSize = new int[2];

		// Set the fetch size for asynchronous queries:
		String propVal = getProperty(tapConfig, KEY_ASYNC_FETCH_SIZE);
		if (propVal == null)
			fetchSize[0] = DEFAULT_ASYNC_FETCH_SIZE;
		else {
			try {
				fetchSize[0] = Integer.parseInt(propVal);
				if (fetchSize[0] < 0)
					fetchSize[0] = 0;
			} catch(NumberFormatException nfe) {
				throw new TAPException("Integer expected for the property " + KEY_ASYNC_FETCH_SIZE + ": \"" + propVal + "\"!");
			}
		}

		// Set the fetch size for synchronous queries:
		propVal = getProperty(tapConfig, KEY_SYNC_FETCH_SIZE);
		if (propVal == null)
			fetchSize[1] = DEFAULT_SYNC_FETCH_SIZE;
		else {
			try {
				fetchSize[1] = Integer.parseInt(propVal);
				if (fetchSize[1] < 0)
					fetchSize[1] = 0;
			} catch(NumberFormatException nfe) {
				throw new TAPException("Integer expected for the property " + KEY_SYNC_FETCH_SIZE + ": \"" + propVal + "\"!");
			}
		}
	}

	/**
	 * Initialise the maximum upload limit.
	 *
	 * <p><em><b>Note:</b>
	 * 	The default upload limit is still fetched in this function, but only
	 * 	in case no maximum limit is provided, just for backward compatibility
	 * 	with versions 2.2 or less.
	 * </em></p>
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties
	 *                     	are wrong.
	 */
	private void initUploadLimits(final Properties tapConfig) throws TAPException {
		// Fetch the given default and maximum limits:
		String defaultDBLimit = getProperty(tapConfig, KEY_DEFAULT_UPLOAD_LIMIT);
		String maxDBLimit = getProperty(tapConfig, KEY_MAX_UPLOAD_LIMIT);
		Object[] limit = null;

		/* Parse the given maximum limit. */
		if (maxDBLimit != null)
			limit = parseLimit(maxDBLimit, KEY_MAX_UPLOAD_LIMIT, true, true);

		/* If none is provided, try to use the deprecated default limit
		 * (just for backward compatibility). */
		else if (defaultDBLimit != null) {
			logger.warning("The property `" + KEY_DEFAULT_UPLOAD_LIMIT + "` has been deprecated! This value is currently used anyway, but not forever. You should now use only `" + KEY_MAX_UPLOAD_LIMIT + "` instead. (comment or delete the property `" + KEY_DEFAULT_UPLOAD_LIMIT + "` from your configuration file to remove this WARNING)");
			limit = parseLimit(defaultDBLimit, KEY_DEFAULT_UPLOAD_LIMIT, true, true);
		}

		/* If still no value is provided, set the default value. */
		else
			limit = parseLimit(DEFAULT_MAX_UPLOAD_LIMIT, KEY_DEFAULT_UPLOAD_LIMIT, true, true);

		// Finally, set the new limits:
		uploadLimitTypes[0] = uploadLimitTypes[1] = (LimitUnit)limit[1];
		setDefaultUploadLimit((Long)limit[0]);
		setMaxUploadLimit((Long)limit[0]);

	}

	/**
	 * Initialise the maximum size (in bytes) of a whole HTTP Multipart request.
	 *
	 * <p><em><b>Note 1:</b>
	 * 	This maximum size includes the HTTP header (normal parameters included)
	 * 	and the sum of the size of all uploaded files.
	 * </em></p>
	 *
	 * <p><em><b>Note 2:</b>
	 * 	The former property name
	 * 	({@link TAPConfiguration#KEY_UPLOAD_MAX_FILE_SIZE KEY_UPLOAD_MAX_FILE_SIZE})
	 * 	for this limit is still supported yet for some time....but ONLY IF the
	 * 	new one ({@link TAPConfiguration#KEY_UPLOAD_MAX_REQUEST_SIZE KEY_UPLOAD_MAX_REQUEST_SIZE})
	 * 	is not defined.
	 * </em></p>
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration property is
	 *                     	wrong.
	 */
	private void initMaxUploadSize(final Properties tapConfig) throws TAPException {
		String propName = KEY_UPLOAD_MAX_REQUEST_SIZE;
		String propValue = getProperty(tapConfig, propName);

		// temporary backward compatibility with the deprecated property name:
		if (propValue == null) {
			propName = KEY_UPLOAD_MAX_FILE_SIZE;
			propValue = getProperty(tapConfig, propName);
			if (propValue != null)
				logger.warning("The property `" + KEY_UPLOAD_MAX_FILE_SIZE + "` has been replaced by `" + KEY_UPLOAD_MAX_REQUEST_SIZE + "`! This value is currently used anyway, but not forever. You should rename it into `" + KEY_UPLOAD_MAX_REQUEST_SIZE + "`.");
		}

		// If a value is specified...
		if (propValue != null) {
			// ...parse the value:
			Object[] limit = parseLimit(propValue, propName, true, true);
			// ...check that the unit is correct (bytes):
			if (!LimitUnit.bytes.isCompatibleWith((LimitUnit)limit[1]))
				throw new TAPException("The maximum upload request size " + propName + " (here: " + propValue + ") can not be expressed in a unit different from bytes (B, kB, MB, GB)!");
			// ...set the max request size:
			long value = (Long)limit[0] * ((LimitUnit)limit[1]).bytesFactor();
			setMaxUploadSize(value);
		}
	}

	/**
	 * Initialize the TAP user identification method.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration property is wrong.
	 */
	private void initUserIdentifier(final Properties tapConfig) throws TAPException {
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_USER_IDENTIFIER);
		if (propValue != null)
			userIdentifier = newInstance(propValue, KEY_USER_IDENTIFIER, UserIdentifier.class);
	}

	/**
	 * Initialize the list of all allowed coordinate systems.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void initCoordSys(final Properties tapConfig) throws TAPException {
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_COORD_SYS);

		// NO VALUE => ALL COORD SYS ALLOWED!
		if (propValue == null)
			lstCoordSys = null;

		// "NONE" => ALL COORD SYS FORBIDDEN (= no coordinate system expression is allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_NONE))
			lstCoordSys = new ArrayList<String>(0);

		// "ANY" => ALL COORD SYS ALLOWED (= any coordinate system is allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_ANY))
			lstCoordSys = null;

		// OTHERWISE, JUST THE ALLOWED ONE ARE LISTED:
		else {
			// split all the list items:
			String[] items = propValue.split(",");
			if (items.length > 0) {
				lstCoordSys = new ArrayList<String>(items.length);
				for(String item : items) {
					item = item.trim();
					// empty item => ignored
					if (item.length() <= 0)
						continue;
					// "NONE" is not allowed inside a list => error!
					else if (item.toUpperCase().equals(VALUE_NONE))
						throw new TAPException("The special value \"" + VALUE_NONE + "\" can not be used inside a list! It MUST be used in replacement of a whole list to specify that no value is allowed.");
					// "ANY" is not allowed inside a list => error!
					else if (item.toUpperCase().equals(VALUE_ANY))
						throw new TAPException("The special value \"" + VALUE_ANY + "\" can not be used inside a list! It MUST be used in replacement of a whole list to specify that any value is allowed.");
					// parse the coordinate system regular expression in order to check it:
					else {
						try {
							STCS.buildCoordSysRegExp(new String[]{ item });
							lstCoordSys.add(item);
						} catch(ParseException pe) {
							throw new TAPException("Incorrect coordinate system regular expression (\"" + item + "\"): " + pe.getMessage(), pe);
						}
					}
				}
				// if finally no item has been specified, consider it as "any coordinate system allowed":
				if (lstCoordSys.size() == 0)
					lstCoordSys = null;
			} else
				lstCoordSys = null;
		}
	}

	/**
	 * Initialize the list of all allowed ADQL geometrical functions.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void initADQLGeometries(final Properties tapConfig) throws TAPException {
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_GEOMETRIES);

		// NO VALUE => ALL FCT ALLOWED!
		if (propValue == null)
			geometries = null;

		// "NONE" => ALL FCT FORBIDDEN (= none of these functions are allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_NONE))
			geometries = new ArrayList<String>(0);

		// "ANY" => ALL FCT ALLOWED (= all of these functions are allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_ANY))
			geometries = null;

		// OTHERWISE, JUST THE ALLOWED ONE ARE LISTED:
		else {
			// split all the list items:
			String[] items = propValue.split(",");
			if (items.length > 0) {
				geometries = new ArrayList<String>(items.length);
				for(String item : items) {
					item = item.trim();
					// empty item => ignored
					if (item.length() <= 0)
						continue;
					// if it is a name of known ADQL geometrical function, add it to the list:
					else if (item.toUpperCase().matches(GEOMETRY_REGEXP))
						geometries.add(item.toUpperCase());
					// "NONE" is not allowed inside a list => error!
					else if (item.toUpperCase().equals(VALUE_NONE))
						throw new TAPException("The special value \"" + VALUE_NONE + "\" can not be used inside a list! It MUST be used in replacement of a whole list to specify that no value is allowed.");
					// "ANY" is not allowed inside a list => error!
					else if (item.toUpperCase().equals(VALUE_ANY))
						throw new TAPException("The special value \"" + VALUE_ANY + "\" can not be used inside a list! It MUST be used in replacement of a whole list to specify that any value is allowed.");
					// unknown value => error!
					else
						throw new TAPException("Unknown ADQL geometrical function: \"" + item + "\"!");
				}
				// if finally no item has been specified, consider it as "all functions allowed":
				if (geometries.size() == 0)
					geometries = null;
			} else
				geometries = null;
		}
	}

	private final String REGEXP_SIGNATURE = "(\\([^()]*\\)|[^,])*";

	private final String REGEXP_CLASSPATH = "(\\{[^{}]*\\})";

	private final String REGEXP_TRANSLATION = "\"((\\\\\"|[^\"])*)\"";

	private final String REGEXP_DESCRIPTION = "\"((\\\\\"|[^\"])*)\"";

	private final String REGEXP_UDF = "\\[\\s*(" + REGEXP_SIGNATURE + ")\\s*(,\\s*(" + REGEXP_CLASSPATH + "|" + REGEXP_TRANSLATION + ")?\\s*(,\\s*(" + REGEXP_DESCRIPTION + ")?\\s*)?)?\\]";

	private final String REGEXP_UDFS = "\\s*(" + REGEXP_UDF + ")\\s*(,(.*))?";
	private final int GROUP_SIGNATURE = 2;
	private final int GROUP_CLASSPATH = 6;
	private final int GROUP_TRANSLATION = 7;
	private final int GROUP_DESCRIPTION = 11;
	private final int GROUP_NEXT_UDFs = 14;

	/**
	 * Initialize the list of all known and allowed User Defined Functions.
	 *
	 * @param tapConfig	The content of the TAP configuration file.
	 *
	 * @throws TAPException	If the corresponding TAP configuration properties are wrong.
	 */
	private void initUDFs(final Properties tapConfig) throws TAPException {
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_UDFS);

		// NO VALUE => NO UNKNOWN FCT ALLOWED!
		if (propValue == null || propValue.trim().length() == 0)
			udfs = new ArrayList<FunctionDef>(0);

		// "NONE" => NO UNKNOWN FCT ALLOWED (= none of the unknown functions are allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_NONE))
			udfs = new ArrayList<FunctionDef>(0);

		// "ANY" => ALL UNKNOWN FCT ALLOWED (= all of the unknown functions are allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_ANY))
			udfs = null;

		// OTHERWISE, JUST THE ALLOWED ONE ARE LISTED:
		else {

			Pattern patternUDFS = Pattern.compile(REGEXP_UDFS);
			String udfList = propValue;
			int udfOffset = 1;
			while(udfList != null) {
				Matcher matcher = patternUDFS.matcher(udfList);
				if (matcher.matches()) {

					// Fetch the signature, classpath and description:
					String signature = matcher.group(GROUP_SIGNATURE),
							classpath = matcher.group(GROUP_CLASSPATH),
							translation = matcher.group(GROUP_TRANSLATION),
							description = matcher.group(GROUP_DESCRIPTION);

					// If no signature...
					boolean ignoreUdf = false;
					if (signature == null || signature.length() == 0) {
						// ...BUT a class name => error
						if (classpath != null)
							throw new TAPException("Missing UDF declaration! (position in the property " + KEY_UDFS + ": " + (udfOffset + matcher.start(GROUP_SIGNATURE)) + "-" + (udfOffset + matcher.end(GROUP_SIGNATURE)) + ")");
						// ... => ignore this item
						else
							ignoreUdf = true;
					}

					if (!ignoreUdf) {
						// Add the new UDF in the list:
						try {
							// resolve the function signature:
							FunctionDef def = FunctionDef.parse(signature);
							// resolve the class name...
							if (classpath != null) {
								if (isClassName(classpath)) {
									Class<? extends UserDefinedFunction> fctClass = null;
									try {
										// fetch the class:
										fctClass = fetchClass(classpath, KEY_UDFS, UserDefinedFunction.class);
										// set the class inside the UDF definition:
										def.setUDFClass(fctClass);
									} catch(TAPException te) {
										throw new TAPException("Invalid class name for the UDF definition \"" + def + "\": " + te.getMessage() + " (position in the property " + KEY_UDFS + ": " + (udfOffset + matcher.start(GROUP_CLASSPATH)) + "-" + (udfOffset + matcher.end(GROUP_CLASSPATH)) + ")", te);
									} catch(IllegalArgumentException iae) {
										throw new TAPException("Invalid class name for the UDF definition \"" + def + "\": missing a constructor with a single parameter of type ADQLOperand[] " + (fctClass != null ? "in the class \"" + fctClass.getName() + "\"" : "") + "! (position in the property " + KEY_UDFS + ": " + (udfOffset + matcher.start(GROUP_CLASSPATH)) + "-" + (udfOffset + matcher.end(GROUP_CLASSPATH)) + ")");
									}
								} else
									throw new TAPException("Invalid class name for the UDF definition \"" + def + "\": \"" + classpath + "\" is not a class name (or is not surrounding by {} as expected in this property file)! (position in the property " + KEY_UDFS + ": " + (udfOffset + matcher.start(GROUP_CLASSPATH)) + "-" + (udfOffset + matcher.end(GROUP_CLASSPATH)) + ")");
							}
							// ...or the given translation:
							else if (translation != null) {
								try {
									def.setTranslationPattern(translation);
								} catch(IllegalArgumentException iae) {
									throw new TAPException("Invalid argument reference in the translation pattern for the UDF \"" + def + "\"! Cause: " + iae.getMessage());
								}
							}
							// set the description if any:
							if (description != null)
								def.description = description;
							// add the UDF:
							udfs.add(def);
						} catch(ParseException pe) {
							throw new TAPException("Wrong UDF declaration syntax: " + pe.getMessage() + " (position in the property " + KEY_UDFS + ": " + (udfOffset + matcher.start(GROUP_SIGNATURE)) + "-" + (udfOffset + matcher.end(GROUP_SIGNATURE)) + ")", pe);
						}
					}

					// Prepare the next iteration (i.e. the other UDFs):
					udfList = matcher.group(GROUP_NEXT_UDFs);
					if (udfList != null && udfList.trim().length() == 0)
						udfList = null;
					udfOffset += matcher.start(GROUP_NEXT_UDFs);
				} else
					throw new TAPException("Wrong UDF declaration syntax: \"" + udfList + "\"! (position in the property " + KEY_UDFS + ": " + udfOffset + "-" + (propValue.length() + 1) + ")");
			}
		}
	}

	@Override
	public String getProviderName() {
		return providerName;
	}

	@Override
	public String getProviderDescription() {
		return serviceDescription;
	}

	@Override
	public URL getBaseUrl() { return baseURL; }

	@Override
	public boolean isAvailable() {
		return isAvailable;
	}

	@Override
	public String getAvailability() {
		return availability;
	}

	@Override
	public void setAvailable(boolean isAvailable, String message) {
		this.isAvailable = isAvailable;
		availability = message;
	}

	@Override
	public int[] getRetentionPeriod() {
		return retentionPeriod;
	}

	/**
	 * <p>Set the default retention period.</p>
	 *
	 * <p>This period is set by default if the user did not specify one before the execution of his query.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function will apply the given retention period only if legal compared to the currently set maximum value.
	 * 	In other words, if the given value is less or equals to the current maximum retention period.
	 * </em></p>
	 *
	 * @param period	New default retention period (in seconds).
	 *
	 * @return	<i>true</i> if the given retention period has been successfully set, <i>false</i> otherwise.
	 */
	public boolean setDefaultRetentionPeriod(final int period) {
		if ((retentionPeriod[1] <= 0) || (period > 0 && period <= retentionPeriod[1])) {
			retentionPeriod[0] = period;
			return true;
		} else
			return false;
	}

	/**
	 * <p>Set the maximum retention period.</p>
	 *
	 * <p>This period limits the default retention period and the retention period specified by a user.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function may reduce the default retention period if the current default retention period is bigger
	 * 	to the new maximum retention period. In a such case, the default retention period is set to the
	 * 	new maximum retention period.
	 * </em></p>
	 *
	 * @param period	New maximum retention period (in seconds).
	 */
	public void setMaxRetentionPeriod(final int period) {
		// Decrease the default retention period if it will be bigger than the new maximum retention period:
		if (period > 0 && (retentionPeriod[0] <= 0 || period < retentionPeriod[0]))
			retentionPeriod[0] = period;
		// Set the new maximum retention period:
		retentionPeriod[1] = period;
	}

	@Override
	public int[] getExecutionDuration() {
		return executionDuration;
	}

	/**
	 * <p>Set the default execution duration.</p>
	 *
	 * <p>This duration is set by default if the user did not specify one before the execution of his query.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function will apply the given execution duration only if legal compared to the currently set maximum value.
	 * 	In other words, if the given value is less or equals to the current maximum execution duration.
	 * </em></p>
	 *
	 * @param duration	New default execution duration (in milliseconds).
	 *
	 * @return	<i>true</i> if the given execution duration has been successfully set, <i>false</i> otherwise.
	 */
	public boolean setDefaultExecutionDuration(final int duration) {
		if ((executionDuration[1] <= 0) || (duration > 0 && duration <= executionDuration[1])) {
			executionDuration[0] = duration;
			return true;
		} else
			return false;
	}

	/**
	 * <p>Set the maximum execution duration.</p>
	 *
	 * <p>This duration limits the default execution duration and the execution duration specified by a user.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function may reduce the default execution duration if the current default execution duration is bigger
	 * 	to the new maximum execution duration. In a such case, the default execution duration is set to the
	 * 	new maximum execution duration.
	 * </em></p>
	 *
	 * @param duration	New maximum execution duration (in milliseconds).
	 */
	public void setMaxExecutionDuration(final int duration) {
		// Decrease the default execution duration if it will be bigger than the new maximum execution duration:
		if (duration > 0 && (executionDuration[0] <= 0 || duration < executionDuration[0]))
			executionDuration[0] = duration;
		// Set the new maximum execution duration:
		executionDuration[1] = duration;
	}

	@Override
	public Iterator<OutputFormat> getOutputFormats() {
		return outputFormats.iterator();
	}

	@Override
	public OutputFormat getOutputFormat(final String mimeOrAlias) {
		if (mimeOrAlias == null || mimeOrAlias.trim().isEmpty())
			return null;

		for(OutputFormat f : outputFormats) {
			if ((f.getMimeType() != null && f.getMimeType().equalsIgnoreCase(mimeOrAlias)) || (f.getShortMimeType() != null && f.getShortMimeType().equalsIgnoreCase(mimeOrAlias)))
				return f;
		}
		return null;
	}

	/**
	 * <p>Add the given {@link OutputFormat} in the list of output formats supported by the TAP service.</p>
	 *
	 * <p><b>Warning:
	 * 	No verification is done in order to avoid duplicated output formats in the list.
	 * 	NULL objects are merely ignored silently.
	 * </b></p>
	 *
	 * @param newOutputFormat	New output format.
	 */
	public void addOutputFormat(final OutputFormat newOutputFormat) {
		if (newOutputFormat != null)
			outputFormats.add(newOutputFormat);
	}

	/**
	 * Remove the specified output format.
	 *
	 * @param mimeOrAlias	Full or short MIME type of the output format to remove.
	 *
	 * @return	<i>true</i> if the specified format has been found and successfully removed from the list,
	 *        	<i>false</i> otherwise.
	 */
	public boolean removeOutputFormat(final String mimeOrAlias) {
		OutputFormat of = getOutputFormat(mimeOrAlias);
		if (of != null)
			return outputFormats.remove(of);
		else
			return false;
	}

	@Override
	public int[] getOutputLimit() {
		return outputLimits;
	}

	/**
	 * <p>Set the default output limit.</p>
	 *
	 * <p>This limit is set by default if the user did not specify one before the execution of his query.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function will apply the given output limit only if legal compared to the currently set maximum value.
	 * 	In other words, if the given value is less or equals to the current maximum output limit.
	 * </em></p>
	 *
	 * @param limit	New default output limit (in number of rows).
	 *
	 * @return	<i>true</i> if the given output limit has been successfully set, <i>false</i> otherwise.
	 */
	public boolean setDefaultOutputLimit(final int limit) {
		if ((outputLimits[1] <= 0) || (limit > 0 && limit <= outputLimits[1])) {
			outputLimits[0] = limit;
			return true;
		} else
			return false;
	}

	/**
	 * <p>Set the maximum output limit.</p>
	 *
	 * <p>This output limit limits the default output limit and the output limit specified by a user.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function may reduce the default output limit if the current default output limit is bigger
	 * 	to the new maximum output limit. In a such case, the default output limit is set to the
	 * 	new maximum output limit.
	 * </em></p>
	 *
	 * @param limit	New maximum output limit (in number of rows).
	 */
	public void setMaxOutputLimit(final int limit) {
		// Decrease the default output limit if it will be bigger than the new maximum output limit:
		if (limit > 0 && (outputLimits[0] <= 0 || limit < outputLimits[0]))
			outputLimits[0] = limit;
		// Set the new maximum output limit:
		outputLimits[1] = limit;
	}

	@Override
	public final LimitUnit[] getOutputLimitType() {
		return new LimitUnit[]{ LimitUnit.rows, LimitUnit.rows };
	}

	@Override
	public Collection<String> getCoordinateSystems() {
		return lstCoordSys;
	}

	@Override
	public TAPLog getLogger() {
		return logger;
	}

	@Override
	public TAPFactory getFactory() {
		return tapFactory;
	}

	@Override
	public UWSFileManager getFileManager() {
		return fileManager;
	}

	@Override
	public boolean uploadEnabled() {
		return isUploadEnabled;
	}

	public void setUploadEnabled(final boolean enabled) {
		isUploadEnabled = enabled;
	}

	@Override
	public long[] getUploadLimit() {
		return uploadLimits;
	}

	@Override
	public LimitUnit[] getUploadLimitType() {
		return uploadLimitTypes;
	}

	/**
	 * Set the unit of the upload limit.
	 *
	 * @param type	Unit of upload limit (rows or bytes).
	 */
	public void setUploadLimitType(final LimitUnit type) {
		if (type != null)
			uploadLimitTypes = new LimitUnit[]{ type, type };
	}

	/**
	 * Set the default upload limit.
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function will apply the given upload limit only if legal compared
	 * 	to the currently set maximum value. In other words, if the given value
	 * 	is less or equals to the current maximum upload limit.
	 * </em></p>
	 *
	 * @param limit	New default upload limit.
	 *
	 * @return	<i>true</i> if the given upload limit has been successfully set,
	 *        	<i>false</i> otherwise.
	 *
	 * @deprecated	Since 2.3, use {@link #setDefaultUploadLimit(long)} instead.
	 */
	@Deprecated
	public boolean setDefaultUploadLimit(final int limit) {
		return setDefaultUploadLimit((long)limit);
	}

	/**
	 * Set the default upload limit.
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function will apply the given upload limit only if legal compared
	 * 	to the currently set maximum value. In other words, if the given value
	 * 	is less or equals to the current maximum upload limit.
	 * </em></p>
	 *
	 * @param limit	New default upload limit.
	 *
	 * @return	<i>true</i> if the given upload limit has been successfully set,
	 *        	<i>false</i> otherwise.
	 *
	 * @since 2.3
	 */
	public boolean setDefaultUploadLimit(final long limit) {
		try {
			if ((uploadLimits[1] <= 0) || (limit > 0 && LimitUnit.compare(limit, uploadLimitTypes[0], uploadLimits[1], uploadLimitTypes[1]) <= 0)) {
				uploadLimits[0] = limit;
				return true;
			}
		} catch(TAPException e) {
		}
		return false;
	}

	/**
	 * Set the maximum upload limit.
	 *
	 * <p>This upload limit limits the default upload limit.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function may reduce the default upload limit if the current default
	 * 	upload limit is bigger to the new maximum upload limit. In a such case,
	 * 	the default upload limit is set to the new maximum upload limit.
	 * </em></p>
	 *
	 * @param limit	New maximum upload limit.
	 *
	 * @deprecated	Since 2.3, use {@link #setMaxUploadLimit(long)} instead.
	 */
	@Deprecated
	public void setMaxUploadLimit(final int limit) {
		setMaxUploadLimit((long)limit);
	}

	/**
	 * Set the maximum upload limit.
	 *
	 * <p>This upload limit limits the default upload limit.</p>
	 *
	 * <p><em><b>Important note:</b>
	 * 	This function may reduce the default upload limit if the current default
	 * 	upload limit is bigger to the new maximum upload limit. In a such case,
	 * 	the default upload limit is set to the new maximum upload limit.
	 * </em></p>
	 *
	 * @param limit	New maximum upload limit.
	 *
	 * @since 2.3
	 */
	public void setMaxUploadLimit(final long limit) {
		try {
			// Decrease the default output limit if it will be bigger than the new maximum output limit:
			if (limit > 0 && (uploadLimits[0] <= 0 || LimitUnit.compare(limit, uploadLimitTypes[1], uploadLimits[0], uploadLimitTypes[0]) < 0))
				uploadLimits[0] = limit;
			// Set the new maximum output limit:
			uploadLimits[1] = limit;
		} catch(TAPException e) {
		}
	}

	@Override
	public long getMaxUploadSize() {
		return maxUploadSize;
	}

	/**
	 * Set the maximum size of a VOTable files set that can be uploaded in once.
	 *
	 * <p><b>Warning:
	 * 	This size can not be negative or 0. If the given value is in this case,
	 * 	nothing will be done and <i>false</i> will be returned. On the contrary
	 * 	to the other limits, no "unlimited" limit is possible here ; only the
	 * 	maximum value can be set (i.e. maximum positive integer value).
	 * </b></p>
	 *
	 * @param maxSize	New maximum size (in bytes).
	 *
	 * @return	<i>true</i> if the size has been successfully set,
	 *        	<i>false</i> otherwise.
	 *
	 * @deprecated	Since 2.3, use {@link #setMaxUploadSize(long)} instead.
	 */
	@Deprecated
	public boolean setMaxUploadSize(final int maxSize) {
		// No "unlimited" value possible there:
		if (maxSize <= 0)
			return false;

		// Otherwise, set the maximum upload file size:
		maxUploadSize = maxSize;
		return true;
	}

	/**
	 * Set the maximum size of a VOTable files set that can be uploaded in once.
	 *
	 * <p><b>Warning:
	 * 	This size can not be negative or 0. If the given value is in this case,
	 * 	nothing will be done and <i>false</i> will be returned. On the contrary
	 * 	to the other limits, no "unlimited" limit is possible here ; only the
	 * 	maximum value can be set (i.e. maximum positive integer value).
	 * </b></p>
	 *
	 * @param maxSize	New maximum size (in bytes).
	 *
	 * @return	<i>true</i> if the size has been successfully set,
	 *        	<i>false</i> otherwise.
	 *
	 * @since 2.3
	 */
	public boolean setMaxUploadSize(final long maxSize) {
		// No "unlimited" value possible there:
		if (maxSize <= 0)
			return false;

		// Otherwise, set the maximum upload file size:
		maxUploadSize = maxSize;
		return true;
	}

	@Override
	public int getNbMaxAsyncJobs() {
		return maxAsyncJobs;
	}

	@Override
	public UserIdentifier getUserIdentifier() {
		return userIdentifier;
	}

	@Override
	public TAPMetadata getTAPMetadata() {
		return metadata;
	}

	@Override
	public Collection<String> getGeometries() {
		return geometries;
	}

	@Override
	public Collection<FunctionDef> getUDFs() {
		return udfs;
	}

	@Override
	public int[] getFetchSize() {
		return fetchSize;
	}

	@Override
	public boolean fixOnFailEnabled() {
		return isFixOnFailEnabled;
	}

}
