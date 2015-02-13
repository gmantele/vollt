package tap.config;

import static tap.config.TAPConfiguration.DEFAULT_BACKUP_BY_USER;
import static tap.config.TAPConfiguration.DEFAULT_BACKUP_FREQUENCY;
import static tap.config.TAPConfiguration.KEY_BACKUP_BY_USER;
import static tap.config.TAPConfiguration.KEY_BACKUP_FREQUENCY;
import static tap.config.TAPConfiguration.KEY_DATABASE_ACCESS;
import static tap.config.TAPConfiguration.KEY_DATASOURCE_JNDI_NAME;
import static tap.config.TAPConfiguration.KEY_DB_PASSWORD;
import static tap.config.TAPConfiguration.KEY_DB_USERNAME;
import static tap.config.TAPConfiguration.KEY_JDBC_DRIVER;
import static tap.config.TAPConfiguration.KEY_JDBC_URL;
import static tap.config.TAPConfiguration.KEY_SQL_TRANSLATOR;
import static tap.config.TAPConfiguration.VALUE_JDBC;
import static tap.config.TAPConfiguration.VALUE_JDBC_DRIVERS;
import static tap.config.TAPConfiguration.VALUE_JNDI;
import static tap.config.TAPConfiguration.VALUE_PGSPHERE;
import static tap.config.TAPConfiguration.VALUE_POSTGRESQL;
import static tap.config.TAPConfiguration.VALUE_USER_ACTION;
import static tap.config.TAPConfiguration.getProperty;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import tap.AbstractTAPFactory;
import tap.ServiceConnection;
import tap.TAPException;
import tap.backup.DefaultTAPBackupManager;
import tap.db.DBConnection;
import tap.db.JDBCConnection;
import uws.UWSException;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;
import adql.translator.JDBCTranslator;
import adql.translator.PgSphereTranslator;
import adql.translator.PostgreSQLTranslator;

public final class ConfigurableTAPFactory extends AbstractTAPFactory {

	private Class<? extends JDBCTranslator> translator;

	private final DataSource datasource;

	private final String driverPath;
	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;

	private boolean backupByUser;
	private long backupFrequency;

	public ConfigurableTAPFactory(ServiceConnection service, final Properties tapConfig) throws NullPointerException, TAPException{
		super(service);

		/* 1. Configure the database access */
		final String dbAccessMethod = getProperty(tapConfig, KEY_DATABASE_ACCESS);

		// Case a: Missing access method => error!
		if (dbAccessMethod == null)
			throw new TAPException("The property \"" + KEY_DATABASE_ACCESS + "\" is missing! It is required to connect to the database. Two possible values: \"" + VALUE_JDBC + "\" and \"" + VALUE_JNDI + "\".");

		// Case b: JDBC ACCESS
		else if (dbAccessMethod.equalsIgnoreCase(VALUE_JDBC)){
			// Extract the DB type and deduce the JDBC Driver path:
			String jdbcDriver = getProperty(tapConfig, KEY_JDBC_DRIVER);
			String dbUrl = getProperty(tapConfig, KEY_JDBC_URL);
			if (jdbcDriver == null){
				if (dbUrl == null)
					throw new TAPException("The property \"" + KEY_JDBC_URL + "\" is missing! Since the choosen database access method is \"" + VALUE_JDBC + "\", this property is required.");
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
			// Set the DB connection parameters:
			this.driverPath = jdbcDriver;
			this.dbUrl = dbUrl;
			this.dbUser = getProperty(tapConfig, KEY_DB_USERNAME);
			this.dbPassword = getProperty(tapConfig, KEY_DB_PASSWORD);
			// Set the other DB connection parameters:
			this.datasource = null;
		}
		// Case c: JNDI ACCESS
		else if (dbAccessMethod.equalsIgnoreCase(VALUE_JNDI)){
			// Get the datasource JDNI name:
			String dsName = getProperty(tapConfig, KEY_DATASOURCE_JNDI_NAME);
			if (dsName == null)
				throw new TAPException("The property \"" + KEY_DATASOURCE_JNDI_NAME + "\" is missing! Since the choosen database access method is \"" + VALUE_JNDI + "\", this property is required.");
			try{
				// Load the JNDI context:
				InitialContext cxt = new InitialContext();
				// Look for the specified datasource:
				datasource = (DataSource)cxt.lookup(dsName);
				if (datasource == null)
					throw new TAPException("No datasource found with the JNDI name \"" + dsName + "\"!");
				// Set the other DB connection parameters:
				this.driverPath = null;
				this.dbUrl = null;
				this.dbUser = null;
				this.dbPassword = null;
			}catch(NamingException ne){
				throw new TAPException("No datasource found with the JNDI name \"" + dsName + "\"!");
			}
		}
		// Case d: unsupported value
		else
			throw new TAPException("Unsupported value for the property " + KEY_DATABASE_ACCESS + ": \"" + dbAccessMethod + "\"! Allowed values: \"" + VALUE_JNDI + "\" or \"" + VALUE_JDBC + "\".");

		/* 2. Set the ADQLTranslator to use in function of the sql_translator property */
		String sqlTranslator = getProperty(tapConfig, KEY_SQL_TRANSLATOR);
		// case a: no translator specified
		if (sqlTranslator == null)
			throw new TAPException("The property \"" + KEY_SQL_TRANSLATOR + "\" is missing! ADQL queries can not be translated without it. Allowed values: \"" + VALUE_POSTGRESQL + "\", \"" + VALUE_PGSPHERE + "\" or a class path of a class implementing SQLTranslator.");

		// case b: PostgreSQL translator
		else if (sqlTranslator.equalsIgnoreCase(VALUE_POSTGRESQL))
			translator = PostgreSQLTranslator.class;

		// case c: PgSphere translator
		else if (sqlTranslator.equalsIgnoreCase(VALUE_PGSPHERE))
			translator = PgSphereTranslator.class;

		// case d: a client defined ADQLTranslator (with the provided class path)
		else if (TAPConfiguration.isClassPath(sqlTranslator))
			translator = TAPConfiguration.fetchClass(sqlTranslator, KEY_SQL_TRANSLATOR, JDBCTranslator.class);

		// case e: unsupported value
		else
			throw new TAPException("Unsupported value for the property " + KEY_SQL_TRANSLATOR + ": \"" + sqlTranslator + "\" !");

		/* 3. Test the construction of the ADQLTranslator */
		createADQLTranslator();

		/* 4. Test the DB connection (note: a translator is needed to create a connection) */
		DBConnection dbConn = getConnection("0");
		freeConnection(dbConn);

		/* 5. Set the UWS Backup Parameter */
		// Set the backup frequency:
		String propValue = getProperty(tapConfig, KEY_BACKUP_FREQUENCY);
		boolean isTime = false;
		// determine whether the value is a time period ; if yes, set the frequency:
		if (propValue != null){
			try{
				backupFrequency = Long.parseLong(propValue);
				if (backupFrequency > 0)
					isTime = true;
			}catch(NumberFormatException nfe){
				throw new TAPException("Long expected for the property \"" + KEY_BACKUP_FREQUENCY + "\", instead of: \"" + propValue + "\"!");
			}
		}
		// if the value was not a valid numeric time period, try to identify the different textual options:
		if (!isTime){
			if (propValue != null && propValue.equalsIgnoreCase(VALUE_USER_ACTION))
				backupFrequency = DefaultTAPBackupManager.AT_USER_ACTION;
			else
				backupFrequency = DEFAULT_BACKUP_FREQUENCY;
		}
		// Specify whether the backup must be organized by user or not:
		propValue = getProperty(tapConfig, KEY_BACKUP_BY_USER);
		backupByUser = (propValue == null) ? DEFAULT_BACKUP_BY_USER : Boolean.parseBoolean(propValue);
	}

	/**
	 * Build a {@link JDBCTranslator} instance with the given class ({@link #translator} ;
	 * specified by the property sql_translator). If the instance can not be build,
	 * whatever is the reason, a TAPException MUST be thrown.
	 * 
	 * Note: This function is called at the initialization of {@link ConfigurableTAPFactory}
	 * in order to check that a translator can be created.
	 */
	protected JDBCTranslator createADQLTranslator() throws TAPException{
		try{
			return translator.getConstructor().newInstance();
		}catch(Exception ex){
			if (ex instanceof TAPException)
				throw (TAPException)ex;
			else
				throw new TAPException("Impossible to create a JDBCTranslator instance with the empty constructor of \"" + translator.getName() + "\" (see the property " + KEY_SQL_TRANSLATOR + ") for the following reason: " + ex.getMessage());
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
	public DBConnection getConnection(String jobID) throws TAPException{
		if (datasource != null){
			try{
				return new JDBCConnection(datasource.getConnection(), createADQLTranslator(), jobID, this.service.getLogger());
			}catch(SQLException se){
				throw new TAPException("Impossible to establish a connection to the database using the set up datasource!", se);
			}
		}else
			return new JDBCConnection(driverPath, dbUrl, dbUser, dbPassword, createADQLTranslator(), jobID, this.service.getLogger());
	}

	@Override
	public void freeConnection(DBConnection conn){
		try{
			((JDBCConnection)conn).getInnerConnection().close();
		}catch(SQLException se){
			service.getLogger().error("Can not close properly the connection \"" + conn.getID() + "\"!", se);
		}
	}

	@Override
	public void destroy(){
		// Unregister the JDBC driver:
		try{
			DriverManager.deregisterDriver(DriverManager.getDriver(dbUrl));
		}catch(SQLException e){
			service.getLogger().warning("Can not deregister the JDBC driver manager!");
		}

		// TODO Nothing else to do!
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
	public UWSBackupManager createUWSBackupManager(UWSService uws) throws TAPException{
		try{
			return (backupFrequency < 0) ? null : new DefaultTAPBackupManager(uws, backupByUser, backupFrequency);
		}catch(UWSException ex){
			throw new TAPException("Impossible to create a backup manager, because: " + ex.getMessage(), ex);
		}
	}

}
