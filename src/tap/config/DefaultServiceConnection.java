package tap.config;

import static tap.config.TAPConfiguration.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPFactory;
import tap.file.LocalTAPFileManager;
import tap.file.TAPFileManager;
import tap.formatter.OutputFormat;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.UWSException;
import uws.service.UserIdentifier;

public final class DefaultServiceConnection implements ServiceConnection<ResultSet> {

	private TAPFileManager fileManager;

	private TAPLog logger;

	private DefaultTAPFactory tapFactory;

	private final String providerName;
	private final String serviceDescription;

	private boolean isAvailable = false;
	private String availability = null;

	private int[] executionDuration = new int[2];
	private int[] retentionPeriod = new int[2];

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
		availability = getProperty(tapConfig, KEY_DISABILITY_REASON);
		initRetentionPeriod(tapConfig);
		initExecutionDuration(tapConfig);

		// 5. MAKE THE SERVICE AVAILABLE (or not, depending on the property value):
		String propValue = getProperty(tapConfig, KEY_IS_AVAILABLE);
		isAvailable = (propValue == null) ? DEFAULT_IS_AVAILABLE : Boolean.parseBoolean(propValue);
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
				fileManager = new LocalTAPFileManager(rootFile, oneDirectoryPerUser, groupUserDirectories);
			}catch(UWSException e){
				throw new TAPException("The property \"" + KEY_FILE_ROOT_PATH + "\" (" + rootPath + ") is incorrect: " + e.getMessage());
			}
		}
		// CUSTOM file manager:
		else{
			Class<TAPFileManager> classObj = fetchClass(fileManagerType, KEY_FILE_MANAGER, TAPFileManager.class);
			if (classObj == null)
				throw new TAPException("Unknown value for the propertie \"" + KEY_FILE_MANAGER + "\": \"" + fileManagerType + "\". Only two possible values: " + VALUE_LOCAL + " or a class path between {...}.");

			try{
				fileManager = classObj.getConstructor(Properties.class).newInstance(tapConfig);
			}catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e){
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

	public void setAvailability(final boolean isAvailable){
		this.isAvailable = isAvailable;
	}

	@Override
	public String getAvailability(){
		return availability;
	}

	public void setDisabilityReason(final String disabilityReason){
		availability = disabilityReason;
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
	public int[] getOutputLimit(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public tap.ServiceConnection.LimitUnit[] getOutputLimitType(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserIdentifier getUserIdentifier(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean uploadEnabled(){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int[] getUploadLimit(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public tap.ServiceConnection.LimitUnit[] getUploadLimitType(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMaxUploadSize(){
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TAPMetadata getTAPMetadata(){
		// TODO Auto-generated method stub
		return null;
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
	public TAPFactory<ResultSet> getFactory(){
		return tapFactory;
	}

	@Override
	public TAPFileManager getFileManager(){
		return fileManager;
	}

	@Override
	public Iterator<OutputFormat<ResultSet>> getOutputFormats(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputFormat<ResultSet> getOutputFormat(String mimeOrAlias){
		// TODO Auto-generated method stub
		return null;
	}

}
