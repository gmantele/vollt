package tap.config;

import static tap.config.TAPConfiguration.DEFAULT_DIRECTORY_PER_USER;
import static tap.config.TAPConfiguration.DEFAULT_EXECUTION_DURATION;
import static tap.config.TAPConfiguration.DEFAULT_GROUP_USER_DIRECTORIES;
import static tap.config.TAPConfiguration.DEFAULT_MAX_ASYNC_JOBS;
import static tap.config.TAPConfiguration.DEFAULT_RETENTION_PERIOD;
import static tap.config.TAPConfiguration.DEFAULT_UPLOAD_MAX_FILE_SIZE;
import static tap.config.TAPConfiguration.KEY_DEFAULT_EXECUTION_DURATION;
import static tap.config.TAPConfiguration.KEY_DEFAULT_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_DEFAULT_RETENTION_PERIOD;
import static tap.config.TAPConfiguration.KEY_DEFAULT_UPLOAD_LIMIT;
import static tap.config.TAPConfiguration.KEY_DIRECTORY_PER_USER;
import static tap.config.TAPConfiguration.KEY_FILE_MANAGER;
import static tap.config.TAPConfiguration.KEY_FILE_ROOT_PATH;
import static tap.config.TAPConfiguration.KEY_GEOMETRIES;
import static tap.config.TAPConfiguration.KEY_GROUP_USER_DIRECTORIES;
import static tap.config.TAPConfiguration.KEY_MAX_ASYNC_JOBS;
import static tap.config.TAPConfiguration.KEY_MAX_EXECUTION_DURATION;
import static tap.config.TAPConfiguration.KEY_MAX_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_MAX_RETENTION_PERIOD;
import static tap.config.TAPConfiguration.KEY_MAX_UPLOAD_LIMIT;
import static tap.config.TAPConfiguration.KEY_METADATA;
import static tap.config.TAPConfiguration.KEY_METADATA_FILE;
import static tap.config.TAPConfiguration.KEY_OUTPUT_FORMATS;
import static tap.config.TAPConfiguration.KEY_PROVIDER_NAME;
import static tap.config.TAPConfiguration.KEY_SERVICE_DESCRIPTION;
import static tap.config.TAPConfiguration.KEY_UDFS;
import static tap.config.TAPConfiguration.KEY_UPLOAD_ENABLED;
import static tap.config.TAPConfiguration.KEY_UPLOAD_MAX_FILE_SIZE;
import static tap.config.TAPConfiguration.KEY_USER_IDENTIFIER;
import static tap.config.TAPConfiguration.VALUE_ALL;
import static tap.config.TAPConfiguration.VALUE_ANY;
import static tap.config.TAPConfiguration.VALUE_CSV;
import static tap.config.TAPConfiguration.VALUE_DB;
import static tap.config.TAPConfiguration.VALUE_FITS;
import static tap.config.TAPConfiguration.VALUE_HTML;
import static tap.config.TAPConfiguration.VALUE_JSON;
import static tap.config.TAPConfiguration.VALUE_LOCAL;
import static tap.config.TAPConfiguration.VALUE_NONE;
import static tap.config.TAPConfiguration.VALUE_SV;
import static tap.config.TAPConfiguration.VALUE_TEXT;
import static tap.config.TAPConfiguration.VALUE_TSV;
import static tap.config.TAPConfiguration.VALUE_VOT;
import static tap.config.TAPConfiguration.VALUE_VOTABLE;
import static tap.config.TAPConfiguration.VALUE_XML;
import static tap.config.TAPConfiguration.fetchClass;
import static tap.config.TAPConfiguration.getProperty;
import static tap.config.TAPConfiguration.isClassPath;
import static tap.config.TAPConfiguration.parseLimit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPFactory;
import tap.db.DBConnection;
import tap.formatter.FITSFormat;
import tap.formatter.HTMLFormat;
import tap.formatter.JSONFormat;
import tap.formatter.OutputFormat;
import tap.formatter.SVFormat;
import tap.formatter.TextFormat;
import tap.formatter.VOTableFormat;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import tap.metadata.TableSetParser;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uws.UWSException;
import uws.service.UserIdentifier;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;
import adql.db.FunctionDef;
import adql.parser.ParseException;
import adql.query.operand.function.UserDefinedFunction;

public final class ConfigurableServiceConnection implements ServiceConnection {

	private UWSFileManager fileManager;

	private TAPLog logger;

	private ConfigurableTAPFactory tapFactory;

	private final TAPMetadata metadata;

	private final String providerName;
	private final String serviceDescription;

	private boolean isAvailable = false;	// the TAP service must be disabled until the end of its connection initialization 
	private String availability = "TAP service not yet initialized.";

	private int maxAsyncJobs = DEFAULT_MAX_ASYNC_JOBS;

	private int[] executionDuration = new int[2];
	private int[] retentionPeriod = new int[2];

	private final ArrayList<OutputFormat> outputFormats;

	private int[] outputLimits = new int[]{-1,-1};
	private LimitUnit[] outputLimitTypes = new LimitUnit[2];

	private boolean isUploadEnabled = false;
	private int[] uploadLimits = new int[]{-1,-1};
	private LimitUnit[] uploadLimitTypes = new LimitUnit[2];
	private int maxUploadSize = DEFAULT_UPLOAD_MAX_FILE_SIZE;

	private UserIdentifier userIdentifier = null;

	private ArrayList<String> geometries = null;
	private final String GEOMETRY_REGEXP = "(AREA|BOX|CENTROID|CIRCLE|CONTAINS|DISTANCE|COORD1|COORD2|COORDSYS|INTERSECTS|POINT|POLYGON|REGION)";

	private Collection<FunctionDef> udfs = new ArrayList<FunctionDef>(0);

	public ConfigurableServiceConnection(final Properties tapConfig) throws NullPointerException, TAPException, UWSException{
		// 1. INITIALIZE THE FILE MANAGER:
		initFileManager(tapConfig);

		// 2. CREATE THE LOGGER:
		logger = new DefaultTAPLog(fileManager);

		// 3. BUILD THE TAP FACTORY:
		tapFactory = new ConfigurableTAPFactory(this, tapConfig);

		// 4. GET THE METADATA:
		metadata = initMetadata(tapConfig);

		// 5. SET ALL GENERAL SERVICE CONNECTION INFORMATION:
		providerName = getProperty(tapConfig, KEY_PROVIDER_NAME);
		serviceDescription = getProperty(tapConfig, KEY_SERVICE_DESCRIPTION);
		initMaxAsyncJobs(tapConfig);
		initRetentionPeriod(tapConfig);
		initExecutionDuration(tapConfig);

		// 6. CONFIGURE OUTPUT:
		// default output format = VOTable:
		outputFormats = new ArrayList<OutputFormat>(1);
		// set output formats:
		addOutputFormats(tapConfig);
		// set output limits:
		initOutputLimits(tapConfig);

		// 7. CONFIGURE THE UPLOAD:
		// is upload enabled ?
		isUploadEnabled = Boolean.parseBoolean(getProperty(tapConfig, KEY_UPLOAD_ENABLED));
		// set upload limits:
		initUploadLimits(tapConfig);
		// set the maximum upload file size:
		initMaxUploadSize(tapConfig);

		// 8. SET A USER IDENTIFIER:
		initUserIdentifier(tapConfig);

		// 9. CONFIGURE ADQL:
		initADQLGeometries(tapConfig);
		initUDFs(tapConfig);
	}

	private void initFileManager(final Properties tapConfig) throws TAPException{
		// Read the desired file manager:
		String fileManagerType = getProperty(tapConfig, KEY_FILE_MANAGER);
		if (fileManagerType == null)
			throw new TAPException("The property \"" + KEY_FILE_MANAGER + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_LOCAL + " or a class path between {...}.");
		else
			fileManagerType = fileManagerType.trim();

		// LOCAL file manager:
		if (fileManagerType.equalsIgnoreCase(VALUE_LOCAL)){
			// Read the desired root path:
			String rootPath = getProperty(tapConfig, KEY_FILE_ROOT_PATH);
			if (rootPath == null)
				throw new TAPException("The property \"" + KEY_FILE_ROOT_PATH + "\" is missing! It is required to create a TAP Service. Please provide a path toward a directory which will contain all files related to the service.");
			File rootFile = new File(rootPath);

			// Determine whether there should be one directory for each user:
			String propValue = getProperty(tapConfig, KEY_DIRECTORY_PER_USER);
			boolean oneDirectoryPerUser = (propValue == null) ? DEFAULT_DIRECTORY_PER_USER : Boolean.parseBoolean(propValue);

			// Determine whether there should be one directory for each user:
			propValue = getProperty(tapConfig, KEY_GROUP_USER_DIRECTORIES);
			boolean groupUserDirectories = (propValue == null) ? DEFAULT_GROUP_USER_DIRECTORIES : Boolean.parseBoolean(propValue);

			// Build the Local TAP File Manager:
			try{
				fileManager = new LocalUWSFileManager(rootFile, oneDirectoryPerUser, groupUserDirectories);
			}catch(UWSException e){
				throw new TAPException("The property \"" + KEY_FILE_ROOT_PATH + "\" (" + rootPath + ") is incorrect: " + e.getMessage());
			}
		}
		// CUSTOM file manager:
		else{
			Class<? extends UWSFileManager> classObj = fetchClass(fileManagerType, KEY_FILE_MANAGER, UWSFileManager.class);
			if (classObj == null)
				throw new TAPException("Unknown value for the property \"" + KEY_FILE_MANAGER + "\": \"" + fileManagerType + "\". Only two possible values: " + VALUE_LOCAL + " or a class path between {...}.");

			try{
				fileManager = classObj.getConstructor(Properties.class).newInstance(tapConfig);
			}catch(Exception e){
				if (e instanceof TAPException)
					throw (TAPException)e;
				else
					throw new TAPException("Impossible to create a TAPFileManager instance with the constructor (java.util.Properties tapConfig) of \"" + classObj.getName() + "\" for the following reason: " + e.getMessage());
			}
		}
	}

	private TAPMetadata initMetadata(final Properties tapConfig) throws TAPException{
		// Get the fetching method to use:
		String metaFetchType = getProperty(tapConfig, KEY_METADATA);
		if (metaFetchType == null)
			throw new TAPException("The property \"" + KEY_METADATA + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_XML + " (to get metadata from a TableSet XML document) or " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA).");

		TAPMetadata metadata = null;

		// GET METADATA FROM XML & UPDATE THE DATABASE (schema TAP_SCHEMA only):
		if (metaFetchType.equalsIgnoreCase(VALUE_XML)){
			// Get the XML file path:
			String xmlFilePath = getProperty(tapConfig, KEY_METADATA_FILE);
			if (xmlFilePath == null)
				throw new TAPException("The property \"" + KEY_METADATA_FILE + "\" is missing! According to the property \"" + KEY_METADATA + "\", metadata must be fetched from an XML document. The local file path of it MUST be provided using the property \"" + KEY_METADATA_FILE + "\".");

			// Parse the XML document and build the corresponding metadata:
			try{
				metadata = (new TableSetParser()).parse(new File(xmlFilePath));
			}catch(IOException ioe){
				throw new TAPException("A grave error occurred while reading/parsing the TableSet XML document: \"" + xmlFilePath + "\"!", ioe);
			}

			// Update the database:
			DBConnection conn = null;
			try{
				conn = tapFactory.getConnection("SET_TAP_SCHEMA");
				conn.setTAPSchema(metadata);
			}finally{
				if (conn != null)
					tapFactory.freeConnection(conn);
			}
		}
		// GET METADATA FROM DATABASE (schema TAP_SCHEMA):
		else if (metaFetchType.equalsIgnoreCase(VALUE_DB)){
			DBConnection conn = null;
			try{
				conn = tapFactory.getConnection("GET_TAP_SCHEMA");
				metadata = conn.getTAPSchema();
			}finally{
				if (conn != null)
					tapFactory.freeConnection(conn);
			}
		}
		// INCORRECT VALUE => ERROR!
		else
			throw new TAPException("Unsupported value for the property \"" + KEY_METADATA + "\": \"" + metaFetchType + "\"! Only two values are allowed: " + VALUE_XML + " (to get metadata from a TableSet XML document) or " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA).");

		return metadata;
	}

	private void initMaxAsyncJobs(final Properties tapConfig) throws TAPException{
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_MAX_ASYNC_JOBS);
		try{
			// If a value is provided, cast it into an integer and set the attribute:
			maxAsyncJobs = (propValue == null) ? DEFAULT_MAX_ASYNC_JOBS : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			throw new TAPException("Integer expected for the property \"" + KEY_MAX_ASYNC_JOBS + "\", instead of: \"" + propValue + "\"!");
		}
	}

	private void initRetentionPeriod(final Properties tapConfig) throws TAPException{
		retentionPeriod = new int[2];

		// Set the default period:
		String propValue = getProperty(tapConfig, KEY_DEFAULT_RETENTION_PERIOD);
		try{
			retentionPeriod[0] = (propValue == null) ? DEFAULT_RETENTION_PERIOD : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			throw new TAPException("Integer expected for the property \"" + KEY_DEFAULT_RETENTION_PERIOD + "\", instead of: \"" + propValue + "\"!");
		}

		// Set the maximum period:
		propValue = getProperty(tapConfig, KEY_MAX_RETENTION_PERIOD);
		try{
			retentionPeriod[1] = (propValue == null) ? DEFAULT_RETENTION_PERIOD : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			throw new TAPException("Integer expected for the property \"" + KEY_MAX_RETENTION_PERIOD + "\", instead of: \"" + propValue + "\"!");
		}

		// The maximum period MUST be greater or equals than the default period.
		// If not, the default period is set (so decreased) to the maximum period.
		if (retentionPeriod[1] > 0 && retentionPeriod[1] < retentionPeriod[0])
			retentionPeriod[0] = retentionPeriod[1];
	}

	private void initExecutionDuration(final Properties tapConfig) throws TAPException{
		executionDuration = new int[2];

		// Set the default duration:
		String propValue = getProperty(tapConfig, KEY_DEFAULT_EXECUTION_DURATION);
		try{
			executionDuration[0] = (propValue == null) ? DEFAULT_EXECUTION_DURATION : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			throw new TAPException("Integer expected for the property \"" + KEY_DEFAULT_EXECUTION_DURATION + "\", instead of: \"" + propValue + "\"!");
		}

		// Set the maximum duration:
		propValue = getProperty(tapConfig, KEY_MAX_EXECUTION_DURATION);
		try{
			executionDuration[1] = (propValue == null) ? DEFAULT_EXECUTION_DURATION : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			throw new TAPException("Integer expected for the property \"" + KEY_MAX_EXECUTION_DURATION + "\", instead of: \"" + propValue + "\"!");
		}

		// The maximum duration MUST be greater or equals than the default duration.
		// If not, the default duration is set (so decreased) to the maximum duration.
		if (executionDuration[1] > 0 && executionDuration[1] < executionDuration[0])
			executionDuration[0] = executionDuration[1];
	}

	private void addOutputFormats(final Properties tapConfig) throws TAPException{
		// Fetch the value of the property for additional output formats:
		String formats = getProperty(tapConfig, KEY_OUTPUT_FORMATS);

		// SPECIAL VALUE "ALL":
		if (formats == null || formats.equalsIgnoreCase(VALUE_ALL)){
			outputFormats.add(new VOTableFormat(this, DataFormat.BINARY));
			outputFormats.add(new VOTableFormat(this, DataFormat.BINARY2));
			outputFormats.add(new VOTableFormat(this, DataFormat.TABLEDATA));
			outputFormats.add(new VOTableFormat(this, DataFormat.FITS));
			outputFormats.add(new FITSFormat(this));
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
		while(formats != null && formats.length() > 0){
			// Get a format item from the list:
			indexSep = formats.indexOf(',');
			// if a comma is after a left parenthesis
			indexLPar = formats.indexOf('(');
			if (indexSep > 0 && indexLPar > 0 && indexSep > indexLPar){
				indexRPar = formats.indexOf(')', indexLPar);
				if (indexRPar > 0)
					indexSep = formats.indexOf(',', indexRPar);
				else
					throw new TAPException("Missing right parenthesis in: \"" + formats + "\"!");
			}
			// no comma => only one format
			if (indexSep < 0){
				f = formats;
				formats = null;
			}
			// comma at the first position => empty list item => go to the next item
			else if (indexSep == 0){
				formats = formats.substring(1).trim();
				continue;
			}
			// else => get the first format item, and then remove it from the list for the next iteration
			else{
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
			else if (f.toLowerCase().startsWith(VALUE_SV)){
				// get the separator:
				int endSep = f.indexOf(')');
				if (VALUE_SV.length() < f.length() && f.charAt(VALUE_SV.length()) == '(' && endSep > VALUE_SV.length() + 1){
					String separator = f.substring(VALUE_SV.length() + 1, f.length() - 1);
					// get the MIME type and its alias, if any of them is provided:
					String mimeType = null, shortMimeType = null;
					if (endSep + 1 < f.length() && f.charAt(endSep + 1) == ':'){
						int endMime = f.indexOf(':', endSep + 2);
						if (endMime < 0)
							mimeType = f.substring(endSep + 2, f.length());
						else if (endMime > 0){
							mimeType = f.substring(endSep + 2, endMime);
							shortMimeType = f.substring(endMime + 1);
						}
					}
					// add the defined SV(...) format:
					outputFormats.add(new SVFormat(this, separator, true, mimeType, shortMimeType));
				}else
					throw new TAPException("Missing separator char/string for the SV output format: \"" + f + "\"!");
			}
			// VOTABLE
			else if (f.toLowerCase().startsWith(VALUE_VOTABLE) || f.toLowerCase().startsWith(VALUE_VOT)){
				// Parse the format:
				VOTableFormat votFormat = parseVOTableFormat(f);

				// Add the VOTable format:
				outputFormats.add(votFormat);

				// Determine whether the MIME type is the VOTable expected one:
				if (votFormat.getShortMimeType().equals("votable") || votFormat.getMimeType().equals("votable"))
					hasVotableFormat = true;
			}
			// custom OutputFormat
			else if (isClassPath(f)){
				Class<? extends OutputFormat> userOutputFormatClass = fetchClass(f, KEY_OUTPUT_FORMATS, OutputFormat.class);
				try{
					OutputFormat userOutputFormat = userOutputFormatClass.getConstructor(ServiceConnection.class).newInstance(this);
					outputFormats.add(userOutputFormat);
				}catch(Exception e){
					if (e instanceof TAPException)
						throw (TAPException)e;
					else
						throw new TAPException("Impossible to create an OutputFormat instance with the constructor (ServiceConnection) of \"" + userOutputFormatClass.getName() + "\" (see the property output_add_format) for the following reason: " + e.getMessage());
				}
			}
			// unknown format
			else
				throw new TAPException("Unknown output format: " + f);
		}

		// Add by default VOTable format if none is specified:
		if (!hasVotableFormat)
			outputFormats.add(new VOTableFormat(this));
	}

	private VOTableFormat parseVOTableFormat(final String propValue) throws TAPException{
		DataFormat serialization = null;
		VOTableVersion votVersion = null;
		String mimeType = null, shortMimeType = null;

		// Get the parameters, if any:
		int beginSep = propValue.indexOf('(');
		if (beginSep > 0){
			int endSep = propValue.indexOf(')');
			if (endSep <= beginSep)
				throw new TAPException("Wrong output format specification syntax in: \"" + propValue + "\"! A VOTable parameters list must end with ')'.");
			// split the parameters:
			String[] params = propValue.substring(beginSep + 1, endSep).split(",");
			if (params.length > 2)
				throw new TAPException("Wrong number of parameters for the output format VOTable: \"" + propValue + "\"! Only two parameters may be provided: serialization and version.");
			else if (params.length >= 1){
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
				if (params.length == 2){
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
		if (beginSep > 0){
			int endSep = propValue.indexOf(':', beginSep + 1);
			if (endSep < 0)
				endSep = propValue.length();
			// extract the MIME type, if any:
			mimeType = propValue.substring(beginSep + 1, endSep).trim();
			if (mimeType.length() == 0)
				mimeType = null;
			// extract the short MIME type, if any:
			if (endSep < propValue.length()){
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

	private void initOutputLimits(final Properties tapConfig) throws TAPException{
		Object[] limit = parseLimit(getProperty(tapConfig, KEY_DEFAULT_OUTPUT_LIMIT), KEY_DEFAULT_OUTPUT_LIMIT, false);
		outputLimitTypes[0] = (LimitUnit)limit[1];	// it should be "rows" since the parameter areBytesAllowed of parseLimit =false
		setDefaultOutputLimit((Integer)limit[0]);

		limit = parseLimit(getProperty(tapConfig, KEY_MAX_OUTPUT_LIMIT), KEY_DEFAULT_OUTPUT_LIMIT, false);
		outputLimitTypes[1] = (LimitUnit)limit[1];	// it should be "rows" since the parameter areBytesAllowed of parseLimit =false

		if (!setMaxOutputLimit((Integer)limit[0]))
			throw new TAPException("The default output limit (here: " + outputLimits[0] + ") MUST be less or equal to the maximum output limit (here: " + limit[0] + ")!");
	}

	private void initUploadLimits(final Properties tapConfig) throws TAPException{
		Object[] limit = parseLimit(getProperty(tapConfig, KEY_DEFAULT_UPLOAD_LIMIT), KEY_DEFAULT_UPLOAD_LIMIT, true);
		uploadLimitTypes[0] = (LimitUnit)limit[1];
		setDefaultUploadLimit((Integer)limit[0]);

		limit = parseLimit(getProperty(tapConfig, KEY_MAX_UPLOAD_LIMIT), KEY_MAX_UPLOAD_LIMIT, true);
		if (!((LimitUnit)limit[1]).isCompatibleWith(uploadLimitTypes[0]))
			throw new TAPException("The default upload limit (in " + uploadLimitTypes[0] + ") and the maximum upload limit (in " + limit[1] + ") MUST be expressed in the same unit!");
		else
			uploadLimitTypes[1] = (LimitUnit)limit[1];

		if (!setMaxUploadLimit((Integer)limit[0]))
			throw new TAPException("The default upload limit (here: " + getProperty(tapConfig, KEY_DEFAULT_UPLOAD_LIMIT) + ") MUST be less or equal to the maximum upload limit (here: " + getProperty(tapConfig, KEY_MAX_UPLOAD_LIMIT) + ")!");
	}

	private void initMaxUploadSize(final Properties tapConfig) throws TAPException{
		String propValue = getProperty(tapConfig, KEY_UPLOAD_MAX_FILE_SIZE);
		// If a value is specified...
		if (propValue != null){
			// ...parse the value:
			Object[] limit = parseLimit(propValue, KEY_UPLOAD_MAX_FILE_SIZE, true);
			// ...check that the unit is correct (bytes): 
			if (!LimitUnit.bytes.isCompatibleWith((LimitUnit)limit[1]))
				throw new TAPException("The maximum upload file size " + KEY_UPLOAD_MAX_FILE_SIZE + " (here: " + propValue + ") can not be expressed in a unit different from bytes (B, kB, MB, GB)!");
			// ...set the max file size:
			int value = (int)((Integer)limit[0] * ((LimitUnit)limit[1]).bytesFactor());
			setMaxUploadSize(value);
		}
	}

	private void initUserIdentifier(final Properties tapConfig) throws TAPException{
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_USER_IDENTIFIER);
		if (propValue == null)
			return;

		// Check the value is a class path:
		if (!isClassPath(propValue))
			throw new TAPException("Class path expected for the property \"" + KEY_USER_IDENTIFIER + "\", instead of: \"" + propValue + "\"!");

		// Fetch the class:
		Class<? extends UserIdentifier> c = fetchClass(propValue, KEY_USER_IDENTIFIER, UserIdentifier.class);

		// Create an instance with the empty constructor:
		try{
			userIdentifier = c.getConstructor().newInstance();
		}catch(Exception e){
			if (e instanceof TAPException)
				throw (TAPException)e;
			else
				throw new TAPException("Impossible to create a UserIdentifier instance with the empty constructor of \"" + c.getName() + "\" (see the property user_identifier) for the following reason: " + e.getMessage());
		}
	}

	private void initADQLGeometries(final Properties tapConfig) throws TAPException{
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
		else{
			// split all the list items:
			String[] items = propValue.split(",");
			if (items.length > 0){
				geometries = new ArrayList<String>(items.length);
				for(String item : items){
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
			}else
				geometries = null;
		}
	}

	private void initUDFs(final Properties tapConfig) throws TAPException{
		// Get the property value:
		String propValue = getProperty(tapConfig, KEY_UDFS);

		// NO VALUE => NO UNKNOWN FCT ALLOWED!
		if (propValue == null)
			udfs = new ArrayList<FunctionDef>(0);

		// "NONE" => NO UNKNOWN FCT ALLOWED (= none of the unknown functions are allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_NONE))
			udfs = new ArrayList<FunctionDef>(0);

		// "ANY" => ALL UNKNOWN FCT ALLOWED (= all of the unknown functions are allowed)!
		else if (propValue.equalsIgnoreCase(VALUE_ANY))
			udfs = null;

		// OTHERWISE, JUST THE ALLOWED ONE ARE LISTED:
		else{

			char c;
			int ind = 0;
			short nbComma = 0;
			boolean within_item = false, within_params = false, within_classpath = false;
			StringBuffer buf = new StringBuffer();
			String signature, classpath;
			int[] posSignature = new int[]{-1,-1}, posClassPath = new int[]{-1,-1};

			signature = null;
			classpath = null;
			buf.delete(0, buf.length());

			while(ind < propValue.length()){
				// Get the character:
				c = propValue.charAt(ind++);
				// If space => ignore
				if (!within_params && Character.isWhitespace(c))
					continue;
				// If inside a parameters list, keep all characters until the list end (')'):
				if (within_params){
					if (c == ')')
						within_params = false;
					buf.append(c);
				}
				// If inside a classpath, keep all characters until the classpath end ('}'):
				else if (within_classpath){
					if (c == '}')
						within_classpath = false;
					buf.append(c);
				}
				// If inside an UDF declaration:
				else if (within_item){
					switch(c){
						case '(': /* start of a parameters list */
							within_params = true;
							buf.append(c);
							break;
						case '{': /* start of a classpath */
							within_classpath = true;
							buf.append(c);
							break;
						case ',': /* separation between the signature and the classpath */
							// count commas within this item:
							if (++nbComma > 1)
								// if more than 1, throw an error:
								throw new TAPException("Wrong UDF declaration syntax: only two items (signature and classpath) can be given within brackets. (position in the property " + KEY_UDFS + ": " + ind + ")");
							else{
								// end of the signature and start of the class path:
								signature = buf.toString();
								buf.delete(0, buf.length());
								posSignature[1] = ind;
								posClassPath[0] = ind + 1;
							}
							break;
						case ']': /* end of a UDF declaration */
							within_item = false;
							if (nbComma == 0){
								signature = buf.toString();
								posSignature[1] = ind;
							}else{
								classpath = (buf.length() == 0 ? null : buf.toString());
								if (classpath != null)
									posClassPath[1] = ind;
							}
							buf.delete(0, buf.length());

							// no signature...
							if (signature == null || signature.length() == 0){
								// ...BUT a classpath => error
								if (classpath != null)
									throw new TAPException("Missing UDF declaration! (position in the property " + KEY_UDFS + ": " + posSignature[0] + "-" + posSignature[1] + ")");
								// ... => ignore this item
								else
									continue;
							}

							// add the new UDF in the list:
							try{
								// resolve the function signature:
								FunctionDef def = FunctionDef.parse(signature);
								// resolve the class path:
								if (classpath != null){
									if (isClassPath(classpath)){
										Class<? extends UserDefinedFunction> fctClass = null;
										try{
											// fetch the class:
											fctClass = fetchClass(classpath, KEY_UDFS, UserDefinedFunction.class);
											// set the class inside the UDF definition:
											def.setUDFClass(fctClass);
										}catch(TAPException te){
											throw new TAPException("Invalid class path for the UDF definition \"" + def + "\": " + te.getMessage() + " (position in the property " + KEY_UDFS + ": " + posClassPath[0] + "-" + posClassPath[1] + ")", te);
										}catch(IllegalArgumentException iae){
											throw new TAPException("Invalid class path for the UDF definition \"" + def + "\": missing a constructor with a single parameter of type ADQLOperand[] " + (fctClass != null ? "in the class \"" + fctClass.getName() + "\"" : "") + "! (position in the property " + KEY_UDFS + ": " + posClassPath[0] + "-" + posClassPath[1] + ")");
										}
									}else
										throw new TAPException("Invalid class path for the UDF definition \"" + def + "\": \"" + classpath + "\" is not a class path (or is not surrounding by {} as expected in this property file)! (position in the property " + KEY_UDFS + ": " + posClassPath[0] + "-" + posClassPath[1] + ")");
								}
								// add the UDF:
								udfs.add(def);
							}catch(ParseException pe){
								throw new TAPException("Wrong UDF declaration syntax: " + pe.getMessage() + " (position in the property " + KEY_UDFS + ": " + posSignature[0] + "-" + posSignature[1] + ")", pe);
							}

							// reset some variables:
							nbComma = 0;
							signature = null;
							classpath = null;
							break;
						default: /* keep all other characters */
							buf.append(c);
							break;
					}
				}
				// If outside of everything, just starting a UDF declaration or separate each declaration is allowed:
				else{
					switch(c){
						case '[':
							within_item = true;
							posSignature[0] = ind + 1;
							break;
						case ',':
							break;
						default:
							throw new TAPException("Wrong UDF declaration syntax: unexpected character at position " + ind + " in the property " + KEY_UDFS + ": \"" + c + "\"! A UDF declaration must have one of the following syntaxes: \"[signature]\" or \"[signature,{classpath}]\".");
					}
				}
			}

			// If the parsing is not finished, throw an error:
			if (within_item)
				throw new TAPException("Wrong UDF declaration syntax: missing closing bracket at position " + propValue.length() + "!");
		}
	}

	@Override
	public String getProviderName(){
		return providerName;
	}

	@Override
	public String getProviderDescription(){
		return serviceDescription;
	}

	@Override
	public boolean isAvailable(){
		return isAvailable;
	}

	@Override
	public String getAvailability(){
		return availability;
	}

	@Override
	public void setAvailable(boolean isAvailable, String message){
		this.isAvailable = isAvailable;
		availability = message;
	}

	@Override
	public int[] getRetentionPeriod(){
		return retentionPeriod;
	}

	public boolean setDefaultRetentionPeriod(final int period){
		if ((retentionPeriod[1] <= 0) || (period > 0 && period <= retentionPeriod[1])){
			retentionPeriod[0] = period;
			return true;
		}else
			return false;
	}

	public boolean setMaxRetentionPeriod(final int period){
		if (period <= 0 || (retentionPeriod[0] > 0 && period >= retentionPeriod[0])){
			retentionPeriod[1] = period;
			return true;
		}else
			return false;
	}

	@Override
	public int[] getExecutionDuration(){
		return executionDuration;
	}

	public boolean setDefaultExecutionDuration(final int period){
		if ((executionDuration[1] <= 0) || (period > 0 && period <= executionDuration[1])){
			executionDuration[0] = period;
			return true;
		}else
			return false;
	}

	public boolean setMaxExecutionDuration(final int period){
		if (period <= 0 || (executionDuration[0] > 0 && period >= executionDuration[0])){
			executionDuration[1] = period;
			return true;
		}else
			return false;
	}

	@Override
	public Iterator<OutputFormat> getOutputFormats(){
		return outputFormats.iterator();
	}

	@Override
	public OutputFormat getOutputFormat(final String mimeOrAlias){
		if (mimeOrAlias == null || mimeOrAlias.trim().isEmpty())
			return null;

		for(OutputFormat f : outputFormats){
			if ((f.getMimeType() != null && f.getMimeType().equalsIgnoreCase(mimeOrAlias)) || (f.getShortMimeType() != null && f.getShortMimeType().equalsIgnoreCase(mimeOrAlias)))
				return f;
		}
		return null;
	}

	public void addOutputFormat(final OutputFormat newOutputFormat){
		outputFormats.add(newOutputFormat);
	}

	public boolean removeOutputFormat(final String mimeOrAlias){
		OutputFormat of = getOutputFormat(mimeOrAlias);
		if (of != null)
			return outputFormats.remove(of);
		else
			return false;
	}

	@Override
	public int[] getOutputLimit(){
		return outputLimits;
	}

	public boolean setDefaultOutputLimit(final int limit){
		if ((outputLimits[1] <= 0) || (limit > 0 && limit <= outputLimits[1])){
			outputLimits[0] = limit;
			return true;
		}else
			return false;
	}

	public boolean setMaxOutputLimit(final int limit){
		if (limit > 0 && outputLimits[0] > 0 && limit < outputLimits[0])
			return false;
		else{
			outputLimits[1] = limit;
			return true;
		}
	}

	@Override
	public final LimitUnit[] getOutputLimitType(){
		return new LimitUnit[]{LimitUnit.rows,LimitUnit.rows};
	}

	@Override
	public Collection<String> getCoordinateSystems(){
		return null;
	}

	@Override
	public TAPLog getLogger(){
		return logger;
	}

	@Override
	public TAPFactory getFactory(){
		return tapFactory;
	}

	@Override
	public UWSFileManager getFileManager(){
		return fileManager;
	}

	@Override
	public boolean uploadEnabled(){
		return isUploadEnabled;
	}

	public void setUploadEnabled(final boolean enabled){
		isUploadEnabled = enabled;
	}

	@Override
	public int[] getUploadLimit(){
		return uploadLimits;
	}

	@Override
	public LimitUnit[] getUploadLimitType(){
		return uploadLimitTypes;
	}

	public void setUploadLimitType(final LimitUnit type){
		if (type != null)
			uploadLimitTypes = new LimitUnit[]{type,type};
	}

	public boolean setDefaultUploadLimit(final int limit){
		try{
			if ((uploadLimits[1] <= 0) || (limit > 0 && LimitUnit.compare(limit, uploadLimitTypes[0], uploadLimits[1], uploadLimitTypes[1]) <= 0)){
				uploadLimits[0] = limit;
				return true;
			}
		}catch(TAPException e){}
		return false;
	}

	public boolean setMaxUploadLimit(final int limit){
		try{
			if (limit > 0 && uploadLimits[0] > 0 && LimitUnit.compare(limit, uploadLimitTypes[1], uploadLimits[0], uploadLimitTypes[0]) < 0)
				return false;
			else{
				uploadLimits[1] = limit;
				return true;
			}
		}catch(TAPException e){
			return false;
		}
	}

	@Override
	public int getMaxUploadSize(){
		return maxUploadSize;
	}

	public boolean setMaxUploadSize(final int maxSize){
		// No "unlimited" value possible there:
		if (maxSize <= 0)
			return false;

		// Otherwise, set the maximum upload file size:
		maxUploadSize = maxSize;
		return true;
	}

	@Override
	public int getNbMaxAsyncJobs(){
		return maxAsyncJobs;
	}

	@Override
	public UserIdentifier getUserIdentifier(){
		return userIdentifier;
	}

	@Override
	public TAPMetadata getTAPMetadata(){
		return metadata;
	}

	@Override
	public Collection<String> getGeometries(){
		return geometries;
	}

	@Override
	public Collection<FunctionDef> getUDFs(){
		return udfs;
	}

}
