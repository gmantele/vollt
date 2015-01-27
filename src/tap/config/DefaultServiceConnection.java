package tap.config;

import static tap.config.TAPConfiguration.DEFAULT_DIRECTORY_PER_USER;
import static tap.config.TAPConfiguration.DEFAULT_EXECUTION_DURATION;
import static tap.config.TAPConfiguration.DEFAULT_GROUP_USER_DIRECTORIES;
import static tap.config.TAPConfiguration.DEFAULT_RETENTION_PERIOD;
import static tap.config.TAPConfiguration.DEFAULT_UPLOAD_MAX_FILE_SIZE;
import static tap.config.TAPConfiguration.KEY_DEFAULT_EXECUTION_DURATION;
import static tap.config.TAPConfiguration.KEY_DEFAULT_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_DEFAULT_RETENTION_PERIOD;
import static tap.config.TAPConfiguration.KEY_DEFAULT_UPLOAD_LIMIT;
import static tap.config.TAPConfiguration.KEY_DIRECTORY_PER_USER;
import static tap.config.TAPConfiguration.KEY_FILE_MANAGER;
import static tap.config.TAPConfiguration.KEY_FILE_ROOT_PATH;
import static tap.config.TAPConfiguration.KEY_GROUP_USER_DIRECTORIES;
import static tap.config.TAPConfiguration.KEY_MAX_EXECUTION_DURATION;
import static tap.config.TAPConfiguration.KEY_MAX_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_MAX_RETENTION_PERIOD;
import static tap.config.TAPConfiguration.KEY_MAX_UPLOAD_LIMIT;
import static tap.config.TAPConfiguration.KEY_OUTPUT_FORMATS;
import static tap.config.TAPConfiguration.KEY_PROVIDER_NAME;
import static tap.config.TAPConfiguration.KEY_SERVICE_DESCRIPTION;
import static tap.config.TAPConfiguration.KEY_UPLOAD_ENABLED;
import static tap.config.TAPConfiguration.KEY_UPLOAD_MAX_FILE_SIZE;
import static tap.config.TAPConfiguration.VALUE_CSV;
import static tap.config.TAPConfiguration.VALUE_JSON;
import static tap.config.TAPConfiguration.VALUE_LOCAL;
import static tap.config.TAPConfiguration.VALUE_SV;
import static tap.config.TAPConfiguration.VALUE_TSV;
import static tap.config.TAPConfiguration.fetchClass;
import static tap.config.TAPConfiguration.getProperty;
import static tap.config.TAPConfiguration.isClassPath;
import static tap.config.TAPConfiguration.parseLimit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPFactory;
import tap.formatter.JSONFormat;
import tap.formatter.OutputFormat;
import tap.formatter.SVFormat;
import tap.formatter.VOTableFormat;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.UWSException;
import uws.service.UserIdentifier;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;
import adql.db.FunctionDef;

public final class DefaultServiceConnection implements ServiceConnection {

	private UWSFileManager fileManager;

	private TAPLog logger;

	private DefaultTAPFactory tapFactory;

	private final String providerName;
	private final String serviceDescription;

	private boolean isAvailable = false;	// the TAP service must be disabled until the end of its connection initialization 
	private String availability = "TAP service not yet initialized.";

	private int[] executionDuration = new int[2];
	private int[] retentionPeriod = new int[2];

	private final ArrayList<OutputFormat> outputFormats;

	private int[] outputLimits = new int[]{-1,-1};
	private LimitUnit[] outputLimitTypes = new LimitUnit[2];

	private boolean isUploadEnabled = false;
	private int[] uploadLimits = new int[]{-1,-1};
	private LimitUnit[] uploadLimitTypes = new LimitUnit[2];
	private int maxUploadSize = DEFAULT_UPLOAD_MAX_FILE_SIZE;

	private final Collection<FunctionDef> udfs = new ArrayList<FunctionDef>(0);

	public DefaultServiceConnection(final Properties tapConfig) throws NullPointerException, TAPException, UWSException{
		// 1. INITIALIZE THE FILE MANAGER:
		initFileManager(tapConfig);

		// 2. CREATE THE LOGGER:
		logger = new DefaultTAPLog(fileManager);

		// 3. BUILD THE TAP FACTORY:
		tapFactory = new DefaultTAPFactory(this, tapConfig);

		// 4. SET ALL GENERAL SERVICE CONNECTION INFORMATION:
		providerName = getProperty(tapConfig, KEY_PROVIDER_NAME);
		serviceDescription = getProperty(tapConfig, KEY_SERVICE_DESCRIPTION);
		initRetentionPeriod(tapConfig);
		initExecutionDuration(tapConfig);

		// 5. CONFIGURE OUTPUT:
		// default output format = VOTable:
		outputFormats = new ArrayList<OutputFormat>(1);
		outputFormats.add(new VOTableFormat(this));
		// set additional output formats:
		addOutputFormats(tapConfig);
		// set output limits:
		initOutputLimits(tapConfig);

		// 6. CONFIGURE THE UPLOAD:
		// is upload enabled ?
		isUploadEnabled = Boolean.parseBoolean(getProperty(tapConfig, KEY_UPLOAD_ENABLED));
		// set upload limits:
		initUploadLimits(tapConfig);
		// set the maximum upload file size:
		initMaxUploadSize(tapConfig);

		// 7. MAKE THE SERVICE AVAILABLE:
		setAvailable(true, "TAP service available.");
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

	private void initRetentionPeriod(final Properties tapConfig){
		retentionPeriod = new int[2];

		// Set the default period:
		String propValue = getProperty(tapConfig, KEY_DEFAULT_RETENTION_PERIOD);
		try{
			retentionPeriod[0] = (propValue == null) ? DEFAULT_RETENTION_PERIOD : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			retentionPeriod[0] = DEFAULT_RETENTION_PERIOD;
		}

		// Set the maximum period:
		propValue = getProperty(tapConfig, KEY_MAX_RETENTION_PERIOD);
		try{
			retentionPeriod[1] = (propValue == null) ? DEFAULT_RETENTION_PERIOD : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			retentionPeriod[1] = DEFAULT_RETENTION_PERIOD;
		}

		// The maximum period MUST be greater or equals than the default period.
		// If not, the default period is set (so decreased) to the maximum period.
		if (retentionPeriod[1] > 0 && retentionPeriod[1] < retentionPeriod[0])
			retentionPeriod[0] = retentionPeriod[1];
	}

	private void initExecutionDuration(final Properties tapConfig){
		executionDuration = new int[2];

		// Set the default duration:
		String propValue = getProperty(tapConfig, KEY_DEFAULT_EXECUTION_DURATION);
		try{
			executionDuration[0] = (propValue == null) ? DEFAULT_EXECUTION_DURATION : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			executionDuration[0] = DEFAULT_EXECUTION_DURATION;
		}

		// Set the maximum duration:
		propValue = getProperty(tapConfig, KEY_MAX_EXECUTION_DURATION);
		try{
			executionDuration[1] = (propValue == null) ? DEFAULT_EXECUTION_DURATION : Integer.parseInt(propValue);
		}catch(NumberFormatException nfe){
			executionDuration[1] = DEFAULT_EXECUTION_DURATION;
		}

		// The maximum duration MUST be greater or equals than the default duration.
		// If not, the default duration is set (so decreased) to the maximum duration.
		if (executionDuration[1] > 0 && executionDuration[1] < executionDuration[0])
			executionDuration[0] = executionDuration[1];
	}

	private void addOutputFormats(final Properties tapConfig) throws TAPException{
		// Fetch the value of the property for additional output formats:
		String formats = TAPConfiguration.getProperty(tapConfig, KEY_OUTPUT_FORMATS);

		// Since it is a comma separated list of output formats, a loop will parse this list comma by comma:
		String f;
		int indexSep;
		while(formats != null && formats.length() > 0){
			// Get a format item from the list:
			indexSep = formats.indexOf(',');
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
			// JSON
			if (f.equalsIgnoreCase(VALUE_JSON))
				outputFormats.add(new JSONFormat(this));
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
						throw new TAPException("Impossible to create an OutputFormat<ResultSet> instance with the constructor (ServiceConnection<ResultSet>) of \"" + userOutputFormatClass.getName() + "\" (see the property output_add_format) for the following reason: " + e.getMessage());
				}
			}
			// unknown format
			else
				throw new TAPException("Unknown output format: " + f);
		}
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
	public UserIdentifier getUserIdentifier(){
		return null;	// NO USER IDENTIFICATION
	}

	@Override
	public TAPMetadata getTAPMetadata(){
		// TODO GET METADATA
		return null;
	}

	@Override
	public Collection<String> getGeometries(){
		return null;	// ALL GEOMETRIES ALLOWED
	}

	@Override
	public Collection<FunctionDef> getUDFs(){
		return udfs;	// FORBID ANY UNKNOWN FUNCTION
	}

	@Override
	public int getNbMaxAsyncJobs(){
		return -1;	// UNLIMITED
	}

}
