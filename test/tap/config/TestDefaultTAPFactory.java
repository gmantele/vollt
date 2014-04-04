package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPFactory;
import tap.db.DBException;
import tap.file.LocalTAPFileManager;
import tap.file.TAPFileManager;
import tap.formatter.OutputFormat;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.service.UWSService;
import uws.service.UserIdentifier;

public class TestDefaultTAPFactory {

	final String VALID_CONF_FILE = "bin/ext/test/tap_valid.properties";
	final String NO_JDBC_1_CONF_FILE = "bin/ext/test/tap_no_jdbc_driver_1.properties";
	final String NO_JDBC_2_CONF_FILE = "bin/ext/test/tap_no_jdbc_driver_2.properties";
	final String BAD_JDBC_CONF_FILE = "bin/ext/test/tap_bad_jdbc_driver.properties";
	final String BAD_TRANSLATOR_CONF_FILE = "bin/ext/test/tap_bad_translator.properties";
	final String BAD_DB_NAME_CONF_FILE = "bin/ext/test/tap_bad_db_name.properties";
	final String BAD_USERNAME_CONF_FILE = "bin/ext/test/tap_bad_username.properties";
	final String BAD_PASSWORD_CONF_FILE = "bin/ext/test/tap_bad_password.properties";

	private Properties validProp, noJdbcProp1, noJdbcProp2, badJdbcProp,
			badTranslatorProp, badDBNameProp, badUsernameProp, badPasswordProp;

	private ServiceConnection<ResultSet> serviceConnection = null;

	@Before
	public void setUp() throws Exception{
		// BUILD A FAKE SERVICE CONNECTION:
		serviceConnection = new ServiceConnectionTest();

		// LOAD ALL PROPERTIES FILES NEEDED FOR ALL THE TESTS:
		FileInputStream input = null;
		try{
			validProp = new Properties();
			input = new FileInputStream(VALID_CONF_FILE);
			validProp.load(input);
			input.close();
			input = null;

			noJdbcProp1 = new Properties();
			input = new FileInputStream(NO_JDBC_1_CONF_FILE);
			noJdbcProp1.load(input);
			input.close();
			input = null;

			noJdbcProp2 = new Properties();
			input = new FileInputStream(NO_JDBC_2_CONF_FILE);
			noJdbcProp2.load(input);
			input.close();
			input = null;

			badJdbcProp = new Properties();
			input = new FileInputStream(BAD_JDBC_CONF_FILE);
			badJdbcProp.load(input);
			input.close();
			input = null;

			badTranslatorProp = new Properties();
			input = new FileInputStream(BAD_TRANSLATOR_CONF_FILE);
			badTranslatorProp.load(input);
			input.close();
			input = null;

			badDBNameProp = new Properties();
			input = new FileInputStream(BAD_DB_NAME_CONF_FILE);
			badDBNameProp.load(input);
			input.close();
			input = null;

			badUsernameProp = new Properties();
			input = new FileInputStream(BAD_USERNAME_CONF_FILE);
			badUsernameProp.load(input);
			input.close();
			input = null;

			badPasswordProp = new Properties();
			input = new FileInputStream(BAD_PASSWORD_CONF_FILE);
			badPasswordProp.load(input);
			input.close();
			input = null;

		}finally{
			if (input != null)
				input.close();
		}
	}

	@Test
	public void testDefaultServiceConnection(){
		// Correct Parameters:
		try{
			TAPFactory<ResultSet> factory = new DefaultTAPFactory(serviceConnection, validProp);
			assertNotNull(factory.createADQLTranslator());
			assertNotNull(factory.createDBConnection("0"));
			assertNull(factory.createUWSBackupManager(new UWSService(factory, new LocalTAPFileManager(new File(".")))));
		}catch(Exception ex){
			fail(getPertinentMessage(ex));
		}

		// No JDBC Driver but the database type is known:
		try{
			new DefaultTAPFactory(serviceConnection, noJdbcProp1);
		}catch(Exception ex){
			fail(getPertinentMessage(ex));
		}

		// No JDBC Driver but the database type is UNKNOWN:
		try{
			new DefaultTAPFactory(serviceConnection, noJdbcProp2);
			fail("This MUST have failed because no JDBC Driver has been successfully guessed from the database type!");
		}catch(Exception ex){
			assertEquals(ex.getClass(), TAPException.class);
			assertTrue(ex.getMessage().matches("No JDBC driver known for the DBMS \"[^\\\"]*\"!"));
		}

		// Bad JDBC Driver:
		try{
			new DefaultTAPFactory(serviceConnection, badJdbcProp);
			fail("This MUST have failed because the provided JDBC Driver doesn't exist!");
		}catch(Exception ex){
			assertEquals(ex.getClass(), DBException.class);
			assertTrue(ex.getMessage().matches("Impossible to find the JDBC driver \"[^\\\"]*\" !"));
		}

		// Bad Translator:
		try{
			new DefaultTAPFactory(serviceConnection, badTranslatorProp);
			fail("This MUST have failed because the provided SQL translator is incorrect!");
		}catch(Exception ex){
			assertEquals(ex.getClass(), TAPException.class);
			assertTrue(ex.getMessage().matches("Unsupported value for the property sql_translator: \"[^\\\"]*\" !"));
		}

		// Bad DB Name:
		try{
			new DefaultTAPFactory(serviceConnection, badDBNameProp);
			fail("This MUST have failed because the provided database name is incorrect!");
		}catch(Exception ex){
			assertEquals(ex.getClass(), DBException.class);
			assertTrue(ex.getMessage().matches("Impossible to establish a connection to the database \"[^\\\"]*\" !"));
			assertEquals(ex.getCause().getClass(), PSQLException.class);
			assertTrue(ex.getCause().getMessage().matches("FATAL: database \"[^\\\"]*\" does not exist"));
		}

		// Bad DB Username: ABORTED BECAUSE THE BAD USERNAME IS NOT DETECTED FOR THE DB WHICH HAS THE SAME NAME AS THE USERNAME !
		// TODO CREATE A NEW DB USER WHICH CAN ACCESS TO THE SAME DB AND TEST AGAIN !
		/*try {
			new DefaultTAPFactory(serviceConnection, badUsernameProp);
			fail("This MUST have failed because the provided database username is incorrect!");
		} catch (Exception ex) {
			ex.printStackTrace();
			assertEquals(ex.getClass(), DBException.class);
			assertEquals(ex.getMessage().substring(0,54), "Impossible to establish a connection to the database \"");
		}*/

		// Bad DB Password:
		try{
			new DefaultTAPFactory(serviceConnection, badPasswordProp);
			fail("This MUST have failed because the provided database password is incorrect!");
		}catch(Exception ex){
			assertEquals(ex.getClass(), DBException.class);
			assertTrue(ex.getMessage().matches("Impossible to establish a connection to the database \"[^\\\"]*\" !"));
			assertEquals(ex.getCause().getClass(), PSQLException.class);
			assertTrue(ex.getCause().getMessage().matches("FATAL: password authentication failed for user \"[^\\\"]*\""));
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

	public static class ServiceConnectionTest implements ServiceConnection<ResultSet> {

		private TAPLog logger = new DefaultTAPLog((TAPFileManager)null);

		@Override
		public String getProviderName(){
			return null;
		}

		@Override
		public String getProviderDescription(){
			return null;
		}

		@Override
		public boolean isAvailable(){
			return true;
		}

		@Override
		public String getAvailability(){
			return null;
		}

		@Override
		public int[] getRetentionPeriod(){
			return null;
		}

		@Override
		public int[] getExecutionDuration(){
			return null;
		}

		@Override
		public int[] getOutputLimit(){
			return null;
		}

		@Override
		public tap.ServiceConnection.LimitUnit[] getOutputLimitType(){
			return null;
		}

		@Override
		public UserIdentifier getUserIdentifier(){
			return null;
		}

		@Override
		public boolean uploadEnabled(){
			return false;
		}

		@Override
		public int[] getUploadLimit(){
			return null;
		}

		@Override
		public tap.ServiceConnection.LimitUnit[] getUploadLimitType(){
			return null;
		}

		@Override
		public int getMaxUploadSize(){
			return 0;
		}

		@Override
		public TAPMetadata getTAPMetadata(){
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
			return null;
		}

		@Override
		public TAPFileManager getFileManager(){
			return null;
		}

		@Override
		public Iterator<OutputFormat<ResultSet>> getOutputFormats(){
			return null;
		}

		@Override
		public OutputFormat<ResultSet> getOutputFormat(String mimeOrAlias){
			return null;
		}
	}

}
