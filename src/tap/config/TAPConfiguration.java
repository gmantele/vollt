package tap.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Properties;

import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.backup.DefaultTAPBackupManager;

public final class TAPConfiguration {

	public final static String TAP_CONF_PARAMETER = "tapconf";
	public final static String DEFAULT_TAP_CONF_FILE = "tap.properties";

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

	/* LOG KEYS */
	public final static String KEY_MIN_LOG_LEVEL = "min_log_level";
	public final static String KEY_LOG_ROTATION = "log_rotation";

	/* UWS BACKUP */
	public final static String KEY_BACKUP_FREQUENCY = "backup_frequency";
	public final static String VALUE_USER_ACTION = "user_action";
	public final static long DEFAULT_BACKUP_FREQUENCY = DefaultTAPBackupManager.MANUAL;	// = "never" => no UWS backup manager
	public final static String KEY_BACKUP_BY_USER = "backup_by_user";
	public final static boolean DEFAULT_BACKUP_BY_USER = false;

	/* ASYNCHRONOUS JOBS */
	public final static String KEY_MAX_ASYNC_JOBS = "max_async_jobs";
	public final static int DEFAULT_MAX_ASYNC_JOBS = 0;

	/* EXECUTION DURATION */
	public final static String KEY_DEFAULT_EXECUTION_DURATION = "default_execution_duration";
	public final static String KEY_MAX_EXECUTION_DURATION = "max_execution_duration";
	public final static int DEFAULT_EXECUTION_DURATION = 0;

	/* DATABASE KEYS */
	public final static String KEY_DATABASE_ACCESS = "database_access";
	public final static String VALUE_JDBC = "jdbc";
	public final static String VALUE_JNDI = "jndi";
	public final static String KEY_SQL_TRANSLATOR = "sql_translator";
	public final static String VALUE_POSTGRESQL = "postgres";
	public final static String VALUE_PGSPHERE = "pgsphere";
	public final static String KEY_DATASOURCE_JNDI_NAME = "datasource_jndi_name";
	public final static String KEY_JDBC_DRIVER = "jdbc_driver";
	public final static HashMap<String,String> VALUE_JDBC_DRIVERS = new HashMap<String,String>(4);
	static{
		VALUE_JDBC_DRIVERS.put("oracle", "oracle.jdbc.OracleDriver");
		VALUE_JDBC_DRIVERS.put("postgresql", "org.postgresql.Driver");
		VALUE_JDBC_DRIVERS.put("mysql", "com.mysql.jdbc.Driver");
		VALUE_JDBC_DRIVERS.put("sqlite", "org.sqlite.JDBC");
	}
	public final static String KEY_JDBC_URL = "jdbc_url";
	public final static String KEY_DB_USERNAME = "db_username";
	public final static String KEY_DB_PASSWORD = "db_password";

	/* METADATA KEYS */
	public final static String KEY_METADATA = "metadata";
	public final static String VALUE_XML = "xml";
	public final static String VALUE_DB = "db";
	public final static String KEY_METADATA_FILE = "metadata_file";

	/* HOME PAGE KEY */
	public final static String KEY_HOME_PAGE = "home_page";
	public final static String KEY_HOME_PAGE_MIME_TYPE = "home_page_mime_type";

	/* PROVIDER KEYS */
	public final static String KEY_PROVIDER_NAME = "provider_name";
	public final static String KEY_SERVICE_DESCRIPTION = "service_description";

	/* UPLOAD KEYS */
	public final static String KEY_UPLOAD_ENABLED = "upload_enabled";
	public final static String KEY_DEFAULT_UPLOAD_LIMIT = "upload_default_db_limit";
	public final static String KEY_MAX_UPLOAD_LIMIT = "upload_max_db_limit";
	public final static String KEY_UPLOAD_MAX_FILE_SIZE = "upload_max_file_size";
	public final static int DEFAULT_UPLOAD_MAX_FILE_SIZE = Integer.MAX_VALUE;

	/* OUTPUT KEYS */
	public final static String KEY_OUTPUT_FORMATS = "output_add_formats";
	public final static String VALUE_ALL = "ALL";
	public final static String VALUE_VOTABLE = "votable";
	public final static String VALUE_VOT = "vot";
	public final static String VALUE_FITS = "fits";
	public final static String VALUE_JSON = "json";
	public final static String VALUE_HTML = "html";
	public final static String VALUE_TEXT = "text";
	public final static String VALUE_CSV = "csv";
	public final static String VALUE_TSV = "tsv";
	public final static String VALUE_SV = "sv";
	public final static String KEY_DEFAULT_OUTPUT_LIMIT = "output_default_limit";
	public final static String KEY_MAX_OUTPUT_LIMIT = "output_max_limit";

	/* USER IDENTIFICATION */
	public final static String KEY_USER_IDENTIFIER = "user_identifier";

	/* ADQL RESTRICTIONS */
	public final static String KEY_GEOMETRIES = "geometries";
	public final static String VALUE_NONE = "NONE";
	public final static String KEY_UDFS = "udfs";
	public final static String VALUE_ANY = "ANY";

	/* ADDITIONAL TAP RESOURCES */
	public final static String KEY_ADD_TAP_RESOURCES = "additional_resources";

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
	 * @throws TAPException	If the class name is incorrect or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType").
	 * 
	 * @see {@link #isClassName(String)}
	 */
	@SuppressWarnings("unchecked")
	public final static < C > Class<? extends C> fetchClass(final String value, final String propertyName, final Class<C> expectedType) throws TAPException{
		if (!isClassName(value))
			return null;

		String classPath = value.substring(1, value.length() - 1).trim();
		if (classPath.isEmpty())
			return null;

		try{
			Class<? extends C> classObject = (Class<? extends C>)Class.forName(classPath);
			if (!expectedType.isAssignableFrom(classObject))
				throw new TAPException("The class specified by the property \"" + propertyName + "\" (" + value + ") is not implementing " + expectedType.getName() + ".");
			else
				return classObject;
		}catch(ClassNotFoundException cnfe){
			throw new TAPException("The class specified by the property \"" + propertyName + "\" (" + value + ") can not be found.");
		}catch(ClassCastException cce){
			throw new TAPException("The class specified by the property \"" + propertyName + "\" (" + value + ") is not implementing " + expectedType.getName() + ".");
		}
	}

	/**
	 * <p>Create an instance of the specified class. The class name is expected to be surrounded by {} in the given value.</p>
	 * 
	 * <p>The instance is created using the empty constructor of the specified class.</p>
	 * 
	 * @param value			Value which is supposed to contain the class name between brackets (see {@link #isClassName(String)} for more details)
	 * @param propertyName	Name of the property associated with the parameter "value".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * 
	 * @return	The corresponding instance.
	 * 
	 * @throws TAPException	If the class name is incorrect
	 *                     	or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType")
	 *                     	or if the specified class has no empty constructor
	 *                     	or if an error occurred while calling this constructor.
	 * 
	 * @see {@link #isClassName(String)}
	 * @see #fetchClass(String, String, Class)
	 */
	public final static < C > C newInstance(final String propValue, final String propName, final Class<C> expectedType) throws TAPException{
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
	 * @param value			Value which is supposed to contain the class name between brackets (see {@link #isClassName(String)} for more details)
	 * @param propertyName	Name of the property associated with the parameter "value".
	 * @param expectedType	Type of the class expected to be returned ; it is also the type which parameterizes this function: C.
	 * @param pTypes		List of each constructor parameter type. Each type MUST be exactly the type declared in the class constructor to select. <i>NULL or empty array if no parameter.</i>
	 * @param parameters	List of all constructor parameters. The number of object MUST match exactly the number of classes provided in the parameter pTypes. <i>NULL or empty array if no parameter.</i>
	 * 
	 * @return	The corresponding instance.
	 * 
	 * @throws TAPException	If the class name is incorrect
	 *                     	or if its type is not compatible with the parameterized type C (represented by the parameter "expectedType")
	 *                     	or if the constructor with the specified parameters can not be found
	 *                     	or if an error occurred while calling this constructor.
	 * 
	 * @see {@link #isClassName(String)}
	 * @see #fetchClass(String, String, Class)
	 */
	public final static < C > C newInstance(final String propValue, final String propName, final Class<C> expectedType, final Class<?>[] pTypes, final Object[] parameters) throws TAPException{
		// Ensure the given name is a class name specification:
		if (!isClassName(propValue))
			throw new TAPException("Class name expected for the property \"" + propName + "\" instead of: \"" + propValue + "\"! The specified class must extend/implement " + expectedType.getName() + ".");

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
			for(int i = 0; i < pTypes.length; i++){
				if (pTypesStr.length() > 0)
					pTypesStr.append(", ");
				if (pTypes[i] == null)
					pTypesStr.append("NULL");
				pTypesStr.append(pTypes[i].getName());
			}
			// Throw the error:
			throw new TAPException("Missing constructor " + classObj.getName() + "(" + pTypesStr.toString() + ")! See the value \"" + propValue + "\" of the property \"" + propName + "\".");
		}catch(InstantiationException ie){
			throw new TAPException("Impossible to create an instance of an abstract class: \"" + classObj.getName() + "\"! See the value \"" + propValue + "\" of the property \"" + propName + "\".");
		}catch(InvocationTargetException ite){
			if (ite.getCause() != null){
				if (ite.getCause() instanceof TAPException)
					throw (TAPException)ite.getCause();
				else
					throw new TAPException(ite.getCause());
			}else
				throw new TAPException(ite);
		}catch(TAPException te){
			throw te;
		}catch(Exception ex){
			throw new TAPException("Impossible to create an instance of " + expectedType.getName() + " as specified in the property \"" + propName + "\": \"" + propValue + "\"!", ex);
		}
	}

	/**
	 * <p>Lets parsing a limit (for output, upload, ...) with its numeric value and its unit.</p>
	 * <p>
	 * 	Here is the expected syntax: num_val[unit].
	 * 	Where unit is optional and should be one of the following values: r or R, B, kB, MB, GB.
	 * 	If the unit is not specified, it is set by default to ROWS.
	 * </p>
	 * 
	 * @param value				Property value (must follow the limit syntax: num_val[unit] ; ex: 20kB or 2000 (for 2000 rows)).
	 * @param propertyName		Name of the property which specify the limit.
	 * @param areBytesAllowed	Tells whether the unit BYTES is allowed. If not and a BYTES unit is encountered, then an exception is thrown.
	 * 
	 * @return	An array with always 2 items: [0]=numeric value (of type Integer), [1]=unit (of type {@link LimitUnit}).
	 * 
	 * @throws TAPException	If the syntax is incorrect or if a not allowed unit has been used.
	 */
	public final static Object[] parseLimit(String value, final String propertyName, final boolean areBytesAllowed) throws TAPException{
		// Remove any whitespace inside or outside the numeric value and its unit:
		if (value != null)
			value = value.replaceAll("\\s", "");

		// If empty value, return an infinite limit:
		if (value == null || value.length() == 0)
			return new Object[]{-1,LimitUnit.rows};

		// A. Parse the string from the end in order to extract the unit part.
		//    The final step of the loop is the extraction of the numeric value, when the first digit is encountered.
		int numValue = -1;
		LimitUnit unit;
		StringBuffer buf = new StringBuffer();
		for(int i = value.length() - 1; i >= 0; i--){
			// if a digit, extract the numeric value:
			if (value.charAt(i) >= '0' && value.charAt(i) <= '9'){
				try{
					numValue = Integer.parseInt(value.substring(0, i + 1));
					break;
				}catch(NumberFormatException nfe){
					throw new TAPException("Integer expected for the property " + propertyName + " for the substring \"" + value.substring(0, i + 1) + "\" of the whole value: \"" + value + "\"!");
				}
			}
			// if a character, store it for later processing:
			else
				buf.append(value.charAt(i));

		}

		// B. Parse the unit.
		// if no unit, set ROWS by default:
		if (buf.length() == 0)
			unit = LimitUnit.rows;
		// if the unit is too long, throw an exception:
		else if (buf.length() > 2)
			throw new TAPException("Unknown limit unit (" + buf.reverse().toString() + ") for the property " + propertyName + ": \"" + value + "\"!");
		// try to identify the unit:
		else{
			// the base unit: bytes or rows
			switch(buf.charAt(0)){
				case 'B':
					if (!areBytesAllowed)
						throw new TAPException("BYTES unit is not allowed for the property " + propertyName + " (" + value + ")!");
					unit = LimitUnit.bytes;
					break;
				case 'r':
				case 'R':
					unit = LimitUnit.rows;
					break;
				default:
					throw new TAPException("Unknown limit unit (" + buf.reverse().toString() + ") for the property " + propertyName + ": \"" + value + "\"!");
			}
			// the 10-power of the base unit, if any:
			if (buf.length() > 1){
				if (unit == LimitUnit.bytes){
					switch(buf.charAt(1)){
						case 'k':
							unit = LimitUnit.kilobytes;
							break;
						case 'M':
							unit = LimitUnit.megabytes;
							break;
						case 'G':
							unit = LimitUnit.gigabytes;
							break;
						default:
							throw new TAPException("Unknown limit unit (" + buf.reverse().toString() + ") for the property " + propertyName + ": \"" + value + "\"!");
					}
				}else
					throw new TAPException("Unknown limit unit (" + buf.reverse().toString() + ") for the property " + propertyName + ": \"" + value + "\"!");
			}
		}

		return new Object[]{((numValue <= 0) ? -1 : numValue),unit};
	}

}
