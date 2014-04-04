package tap.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import tap.TAPException;
import tap.backup.DefaultTAPBackupManager;

public final class TAPConfiguration {

	/* FILE MANAGER KEYS */
	public final static String KEY_FILE_MANAGER = "file_manager";
	public final static String VALUE_LOCAL = "local";
	public final static String DEFAULT_FILE_MANAGER = VALUE_LOCAL;
	public final static String KEY_FILE_ROOT_PATH = "file_root_path";
	public final static String KEY_DIRECTORY_PER_USER = "directory_per_user";
	public final static boolean DEFAULT_DIRECTORY_PER_USER = false;
	public final static String KEY_GROUP_USER_DIRECTORIES = "group_user_directories";
	public final static boolean DEFAULT_GROUP_USER_DIRECTORIES = false;
	public final static String KEY_DEFAULT_RETENTION_PERIOD = "default_retention_period";
	public final static String KEY_MAX_RETENTION_PERIOD = "max_retention_period";
	public final static int DEFAULT_RETENTION_PERIOD = 0;

	/* UWS BACKUP */
	public final static String KEY_BACKUP_FREQUENCY = "backup_frequency";
	public final static String VALUE_USER_ACTION = "user_action";
	public final static long DEFAULT_BACKUP_FREQUENCY = DefaultTAPBackupManager.MANUAL;	// = "never" => no UWS backup manager
	public final static String KEY_BACKUP_BY_USER = "backup_by_user";
	public final static boolean DEFAULT_BACKUP_BY_USER = false;

	/* EXECUTION DURATION */
	public final static String KEY_DEFAULT_EXECUTION_DURATION = "default_execution_duration";
	public final static String KEY_MAX_EXECUTION_DURATION = "max_execution_duration";
	public final static int DEFAULT_EXECUTION_DURATION = 0;

	/* DATABASE KEYS */
	public final static String KEY_JDBC_DRIVER = "jdbc_driver";
	public final static HashMap<String,String> VALUE_JDBC_DRIVERS = new HashMap<String,String>(4);
	static{
		VALUE_JDBC_DRIVERS.put("oracle", "oracle.jdbc.OracleDriver");
		VALUE_JDBC_DRIVERS.put("postgresql", "org.postgresql.Driver");
		VALUE_JDBC_DRIVERS.put("mysql", "com.mysql.jdbc.Driver");
		VALUE_JDBC_DRIVERS.put("sqlite", "org.sqlite.JDBC");
	}
	public final static String KEY_SQL_TRANSLATOR = "sql_translator";
	public final static String VALUE_POSTGRESQL = "postgres";
	public final static String VALUE_PGSPHERE = "pgsphere";
	public final static String KEY_JDBC_URL = "jdbc_url";
	public final static String KEY_DB_USERNAME = "db_username";
	public final static String KEY_DB_PASSWORD = "db_password";

	/* PROVIDER KEYS */
	public final static String KEY_PROVIDER_NAME = "provider_name";
	public final static String KEY_SERVICE_DESCRIPTION = "service_description";

	/* AVAILABILITY KEYS */
	public final static String KEY_IS_AVAILABLE = "is_available";
	public final static boolean DEFAULT_IS_AVAILABLE = true;
	public final static String KEY_DISABILITY_REASON = "disability_reason";

	/**
	 * Read the asked property from the given Properties object.
	 * 	- The returned property value is trimmed (no space at the beginning and at the end of the string).
	 * 	- If the value is empty (length=0), NULL is returned.
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
	 * Test whether a property value is a class path.
	 * Expected syntax: a non-empty string surrounded by brackets ('{' and '}').
	 * 
	 * Note: The class path itself is not checked!
	 * 
	 * @param value	Property value.
	 * 
	 * @return <i>true</i> if the given value is formatted as a class path, <i>false</i> otherwise.
	 */
	public final static boolean isClassPath(final String value){
		return (value != null && value.length() > 2 && value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}');
	}

	/**
	 * Fetch the class object corresponding to the classpath provided between brackets in the given value. 
	 * 
	 * @param value			Value which is supposed to contain the classpath between brackets (see {@link #isClassPath(String)} for more details)
	 * @param propertyName	Name of the property associated with the parameter "value".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * 
	 * @return	The corresponding Class object.
	 * 
	 * @throws TAPException	If the classpath is incorrect or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType").
	 * 
	 * @see {@link #isClassPath(String)}
	 */
	@SuppressWarnings("unchecked")
	public final static < C > Class<C> fetchClass(final String value, final String propertyName, final Class<C> expectedType) throws TAPException{
		if (!isClassPath(value))
			return null;

		String classPath = value.substring(1, value.length() - 1).trim();
		if (classPath.isEmpty())
			return null;

		try{
			Class<C> classObject = (Class<C>)ClassLoader.getSystemClassLoader().loadClass(classPath);
			if (!expectedType.isAssignableFrom(classObject))
				throw new TAPException("The class specified by the property " + propertyName + " (" + value + ") is not implementing " + expectedType.getName() + ".");
			else
				return classObject;
		}catch(ClassNotFoundException cnfe){
			throw new TAPException("The class specified by the property " + propertyName + " (" + value + ") can not be found.");
		}catch(ClassCastException cce){
			throw new TAPException("The class specified by the property " + propertyName + " (" + value + ") is not implementing " + expectedType.getName() + ".");
		}
	}

	public final static void main(final String[] args) throws Throwable{

		FileInputStream configFileStream = null;
		try{
			final File configFile = new File("src/ext/tap_min.properties");
			configFileStream = new FileInputStream(configFile);

			Properties config = new Properties();
			config.load(configFileStream);

			configFileStream.close();
			configFileStream = null;

			Enumeration<Object> keys = config.keys();
			String key;
			while(keys.hasMoreElements()){
				key = keys.nextElement().toString();
				System.out.println("* " + key + " = " + config.getProperty(key));
			}
		}finally{
			if (configFileStream != null)
				configFileStream.close();
		}
	}

}
