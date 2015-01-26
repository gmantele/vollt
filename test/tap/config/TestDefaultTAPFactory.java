package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static tap.config.TAPConfiguration.KEY_DB_PASSWORD;
import static tap.config.TAPConfiguration.KEY_DB_USERNAME;
import static tap.config.TAPConfiguration.KEY_JDBC_DRIVER;
import static tap.config.TAPConfiguration.KEY_JDBC_URL;
import static tap.config.TAPConfiguration.KEY_SQL_TRANSLATOR;

import java.io.File;
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
import tap.formatter.OutputFormat;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.service.UWSService;
import uws.service.UserIdentifier;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;
import adql.db.FunctionDef;

public class TestDefaultTAPFactory {

	private Properties validProp, noJdbcProp1, noJdbcProp2, badJdbcProp,
			badTranslatorProp, badDBNameProp, badUsernameProp, badPasswordProp;

	private ServiceConnection serviceConnection = null;

	@Before
	public void setUp() throws Exception{
		// BUILD A FAKE SERVICE CONNECTION:
		serviceConnection = new ServiceConnectionTest();

		// LOAD ALL PROPERTIES FILES NEEDED FOR ALL THE TESTS:
		validProp = AllTests.getValidProperties();

		noJdbcProp1 = (Properties)validProp.clone();
		noJdbcProp1.remove(KEY_JDBC_DRIVER);

		noJdbcProp2 = (Properties)noJdbcProp1.clone();
		noJdbcProp2.setProperty(KEY_JDBC_URL, "jdbc:foo:gmantele");

		badJdbcProp = (Properties)validProp.clone();
		badJdbcProp.setProperty(KEY_JDBC_DRIVER, "foo");
		badJdbcProp.setProperty(KEY_JDBC_URL, "jdbc:foo:gmantele");

		badTranslatorProp = (Properties)validProp.clone();
		badTranslatorProp.setProperty(KEY_SQL_TRANSLATOR, "foo");

		badDBNameProp = (Properties)validProp.clone();
		badDBNameProp.setProperty(KEY_JDBC_URL, "jdbc:postgresql:foo");

		badUsernameProp = (Properties)validProp.clone();
		badUsernameProp.setProperty(KEY_DB_USERNAME, "foo");

		badPasswordProp = (Properties)validProp.clone();
		badPasswordProp.setProperty(KEY_DB_PASSWORD, "foo");
	}

	@Test
	public void testDefaultServiceConnection(){
		// Correct Parameters:
		try{
			TAPFactory factory = new DefaultTAPFactory(serviceConnection, validProp);
			assertNotNull(factory.getConnection("0"));
			assertNull(factory.createUWSBackupManager(new UWSService(factory, new LocalUWSFileManager(new File(".")))));
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
			assertEquals(TAPException.class, ex.getClass());
			assertTrue(ex.getMessage().matches("No JDBC driver known for the DBMS \"[^\\\"]*\"!"));
		}

		// Bad JDBC Driver:
		try{
			new DefaultTAPFactory(serviceConnection, badJdbcProp);
			fail("This MUST have failed because the provided JDBC Driver doesn't exist!");
		}catch(Exception ex){
			assertEquals(DBException.class, ex.getClass());
			assertTrue(ex.getMessage().matches("Impossible to find the JDBC driver \"[^\\\"]*\" !"));
		}

		// Bad Translator:
		try{
			new DefaultTAPFactory(serviceConnection, badTranslatorProp);
			fail("This MUST have failed because the provided SQL translator is incorrect!");
		}catch(Exception ex){
			assertEquals(TAPException.class, ex.getClass());
			assertTrue(ex.getMessage().matches("Unsupported value for the property sql_translator: \"[^\\\"]*\" !"));
		}

		// Bad DB Name:
		try{
			new DefaultTAPFactory(serviceConnection, badDBNameProp);
			fail("This MUST have failed because the provided database name is incorrect!");
		}catch(Exception ex){
			assertEquals(DBException.class, ex.getClass());
			assertTrue(ex.getMessage().matches("Impossible to establish a connection to the database \"[^\\\"]*\" !"));
			assertEquals(PSQLException.class, ex.getCause().getClass());
			assertTrue(ex.getCause().getMessage().matches("FATAL: password authentication failed for user \"[^\\\"]*\""));
		}

		// Bad DB Username: ABORTED BECAUSE THE BAD USERNAME IS NOT DETECTED FOR THE DB WHICH HAS THE SAME NAME AS THE USERNAME !
		try{
			new DefaultTAPFactory(serviceConnection, badUsernameProp);
			fail("This MUST have failed because the provided database username is incorrect!");
		}catch(Exception ex){
			assertEquals(DBException.class, ex.getClass());
			assertTrue(ex.getMessage().matches("Impossible to establish a connection to the database \"[^\\\"]*\" !"));
			assertEquals(PSQLException.class, ex.getCause().getClass());
			assertTrue(ex.getCause().getMessage().matches("FATAL: password authentication failed for user \"[^\\\"]*\""));
		}

		// Bad DB Password:
		try{
			new DefaultTAPFactory(serviceConnection, badPasswordProp);
			//fail("This MUST have failed because the provided database password is incorrect!"); // NOTE: In function of the database configuration, a password may be required or not. So this test is not automatic! 
		}catch(Exception ex){
			assertEquals(DBException.class, ex.getClass());
			assertTrue(ex.getMessage().matches("Impossible to establish a connection to the database \"[^\\\"]*\" !"));
			assertEquals(PSQLException.class, ex.getCause().getClass());
			assertTrue(ex.getCause().getMessage().matches("FATAL: password authentication failed for user \"[^\\\"]*\""));
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

	public static class ServiceConnectionTest implements ServiceConnection {

		private TAPLog logger = new DefaultTAPLog((UWSFileManager)null);
		private boolean isAvailable = true;

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
			return isAvailable;
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
		public TAPFactory getFactory(){
			return null;
		}

		@Override
		public UWSFileManager getFileManager(){
			return null;
		}

		@Override
		public Iterator<OutputFormat> getOutputFormats(){
			return null;
		}

		@Override
		public OutputFormat getOutputFormat(String mimeOrAlias){
			return null;
		}

		@Override
		public void setAvailable(boolean isAvailable, String message){
			this.isAvailable = isAvailable;
		}

		@Override
		public Collection<String> getGeometries(){
			return null;
		}

		@Override
		public Collection<FunctionDef> getUDFs(){
			return null;
		}

		@Override
		public int getNbMaxAsyncJobs(){
			return -1;
		}
	}

}
