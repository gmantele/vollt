package tap.config;

import static tap.config.TAPConfiguration.*;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.Properties;

import adql.translator.ADQLTranslator;
import adql.translator.PgSphereTranslator;
import adql.translator.PostgreSQLTranslator;
import tap.AbstractTAPFactory;
import tap.ServiceConnection;
import tap.TAPException;
import tap.backup.DefaultTAPBackupManager;
import tap.db.DBConnection;
import tap.db.JDBCConnection;
import uws.UWSException;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;

public final class DefaultTAPFactory extends AbstractTAPFactory<ResultSet> {

	private Class<? extends ADQLTranslator> translator;

	private final String driverPath;
	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;

	private boolean backupByUser;
	private long backupFrequency;

	@SuppressWarnings("unchecked")
	public DefaultTAPFactory(ServiceConnection<ResultSet> service, final Properties tapConfig) throws NullPointerException, TAPException{
		super(service);

		/* 0. Extract the DB type and deduce the JDBC Driver path */
		String jdbcDriver = getProperty(tapConfig, KEY_JDBC_DRIVER);
		String dbUrl = null;
		if (jdbcDriver == null){
			dbUrl = getProperty(tapConfig, KEY_JDBC_URL);
			if (dbUrl == null)
				throw new TAPException("JDBC URL missing.");
			else if (!dbUrl.startsWith(JDBCConnection.JDBC_PREFIX + ":"))
				throw new TAPException("JDBC URL format incorrect! It MUST begins with " + JDBCConnection.JDBC_PREFIX + ":");
			else{
				String dbType = dbUrl.substring(JDBCConnection.JDBC_PREFIX.length() + 1);
				if (dbType.indexOf(':') <= 0)
					throw new TAPException("JDBC URL format incorrect! Database type name is missing.");
				dbType = dbType.substring(0, dbType.indexOf(':'));

				jdbcDriver = VALUE_JDBC_DRIVERS.get(dbType);
				if (jdbcDriver == null)
					throw new TAPException("No JDBC driver known for the DBMS \"" + dbType + "\"!");
			}
		}

		/* 1. Set the ADQLTranslator to use in function of the sql_translator property */
		String sqlTranslator = getProperty(tapConfig, KEY_SQL_TRANSLATOR);
		// case a.) no translator specified
		if (sqlTranslator == null || sqlTranslator.isEmpty())
			throw new TAPException("No SQL translator specified !");

		// case b.) PostgreSQL translator
		else if (sqlTranslator.equalsIgnoreCase(VALUE_POSTGRESQL))
			translator = PostgreSQLTranslator.class;

		// case c.) PgSphere translator
		else if (sqlTranslator.equals(VALUE_PGSPHERE))
			translator = PgSphereTranslator.class;

		// case d.) a client defined ADQLTranslator (with the provided class path)
		else if (sqlTranslator.charAt(0) == '{' && sqlTranslator.charAt(sqlTranslator.length() - 1) == '}'){
			sqlTranslator = sqlTranslator.substring(1, sqlTranslator.length() - 2);
			try{
				translator = (Class<? extends ADQLTranslator>)ClassLoader.getSystemClassLoader().loadClass(sqlTranslator);
			}catch(ClassNotFoundException cnfe){
				throw new TAPException("Unable to load the SQL Translator! The class specified by the property sql_translator (" + sqlTranslator + ") can not be found.");
			}catch(ClassCastException cce){
				throw new TAPException("Unable to load the SQL Translator! The class specified by the property sql_translator (" + sqlTranslator + ") is not implementing adql.translator.ADQLTranslator.");
			}
		}
		// case e.) unsupported value
		else
			throw new TAPException("Unsupported value for the property sql_translator: \"" + sqlTranslator + "\" !");

		/* 2. Test the construction of the ADQLTranslator */
		createADQLTranslator();

		/* 3. Store the DB connection parameters */
		this.driverPath = jdbcDriver;
		this.dbUrl = dbUrl;
		this.dbUser = getProperty(tapConfig, KEY_DB_USERNAME);;
		this.dbPassword = getProperty(tapConfig, KEY_DB_PASSWORD);

		/* 4. Test the DB connection */
		DBConnection<ResultSet> dbConn = createDBConnection("0");
		dbConn.close();

		/* 5. Set the UWS Backup Parameter */
		// BACKUP FREQUENCY:
		String propValue = getProperty(tapConfig, KEY_BACKUP_FREQUENCY);
		boolean isTime = false;
		// determine whether the value is a time period ; if yes, set the frequency:
		if (propValue != null){
			try{
				backupFrequency = Long.parseLong(propValue);
				if (backupFrequency > 0)
					isTime = true;
			}catch(NumberFormatException nfe){}
		}
		// if the value was not a valid numeric time period, try to identify the different textual options:
		if (!isTime){
			if (propValue != null && propValue.equalsIgnoreCase(VALUE_USER_ACTION))
				backupFrequency = DefaultTAPBackupManager.AT_USER_ACTION;
			else
				backupFrequency = DEFAULT_BACKUP_FREQUENCY;
		}
		// BACKUP BY USER:
		propValue = getProperty(tapConfig, KEY_BACKUP_BY_USER);
		backupByUser = (propValue == null) ? DEFAULT_BACKUP_BY_USER : Boolean.parseBoolean(propValue);
	}

	/**
	 * Build an {@link ADQLTranslator} instance with the given class ({@link #translator} ;
	 * specified by the property sql_translator). If the instance can not be build,
	 * whatever is the reason, a TAPException MUST be thrown.
	 * 
	 * Note: This function is called at the initialization of {@link DefaultTAPFactory}
	 * in order to check that a translator can be created.
	 * 
	 * @see tap.TAPFactory#createADQLTranslator()
	 */
	@Override
	public ADQLTranslator createADQLTranslator() throws TAPException{
		try{
			return translator.getConstructor().newInstance();
		}catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e){
			throw new TAPException("Impossible to create an ADQLTranslator instance with the empty constructor of \"" + translator.getName() + "\" (see the property sql_translator) for the following reason: " + e.getMessage());
		}
	}

	/**
	 * Build a {@link JDBCConnection} thanks to the database parameters specified
	 * in the TAP configuration file (the properties: jdbc_driver_path, db_url, db_user, db_password).
	 * 
	 * @see tap.TAPFactory#createDBConnection(java.lang.String)
	 * @see JDBCConnection
	 */
	@Override
	public DBConnection<ResultSet> createDBConnection(String jobID) throws TAPException{
		return new JDBCConnection(jobID, driverPath, dbUrl, dbUser, dbPassword, this.service.getLogger());
	}

	/**
	 * Build an {@link DefaultTAPBackupManager} thanks to the backup manager parameters specified
	 * in the TAP configuration file (the properties: backup_frequency, backup_by_user).
	 * 
	 * Note: If the specified backup_frequency is negative, no backup manager is returned.
	 * 
	 * @return	null if the specified backup frequency is negative, or an instance of {@link DefaultTAPBackupManager} otherwise.
	 * 
	 * @see tap.AbstractTAPFactory#createUWSBackupManager(uws.service.UWSService)
	 * @see DefaultTAPBackupManager
	 */
	@Override
	public UWSBackupManager createUWSBackupManager(UWSService uws) throws TAPException, UWSException{
		return (backupFrequency < 0) ? null : new DefaultTAPBackupManager(uws, backupByUser, backupFrequency);
	}

}
