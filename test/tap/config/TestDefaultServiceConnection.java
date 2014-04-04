package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static tap.config.TAPConfiguration.KEY_FILE_MANAGER;
import static tap.config.TAPConfiguration.VALUE_LOCAL;
import static tap.config.TAPConfiguration.fetchClass;
import static tap.config.TAPConfiguration.isClassPath;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import tap.ServiceConnection;
import tap.TAPException;
import tap.file.LocalTAPFileManager;
import uws.UWSException;

public class TestDefaultServiceConnection {

	final String VALID_CONF_FILE = "bin/ext/test/tap_valid.properties";
	final String NO_FM_CONF_FILE = "bin/ext/test/tap_no_fm.properties";
	final String FM_CLASS_PATH_CONF_FILE = "bin/ext/test/tap_fm_clp.properties";
	final String INCORRECT_FM_CONF_FILE = "bin/ext/test/tap_incorrect_fm.properties";

	private Properties validProp, noFmProp, fmClassPathProp, incorrectFmProp;

	@Before
	public void setUp() throws Exception{
		// LOAD ALL PROPERTIES FILES NEEDED FOR ALL THE TESTS:
		FileInputStream input = null;
		try{
			validProp = new Properties();
			input = new FileInputStream(VALID_CONF_FILE);
			validProp.load(input);
			input.close();
			input = null;

			noFmProp = new Properties();
			input = new FileInputStream(NO_FM_CONF_FILE);
			noFmProp.load(input);
			input.close();
			input = null;

			fmClassPathProp = new Properties();
			input = new FileInputStream(FM_CLASS_PATH_CONF_FILE);
			fmClassPathProp.load(input);
			input.close();
			input = null;

			incorrectFmProp = new Properties();
			input = new FileInputStream(INCORRECT_FM_CONF_FILE);
			incorrectFmProp.load(input);
			input.close();
			input = null;

		}finally{
			if (input != null)
				input.close();
		}
	}

	/**
	 * CONSTRUCTOR TESTS
	 *  * In general:
	 * 		- A valid configuration file builds successfully a fully functional 
	 * 
	 * 	* Over the file manager:
	 * 		- If no TAPFileManager is provided, an exception must be thrown. 
	 * 		- If a classpath toward a valid TAPFileManager is provided, a functional DefaultServiceConnection must be successfully built.
	 * 		- An incorrect file manager value in the configuration file must generate an exception.
	 * 
	 * Note: the good configuration of the TAPFactory built by the DefaultServiceConnection is tested in {@link TestDefaultTAPFactory}.
	 * 
	 * @see DefaultServiceConnection#DefaultServiceConnection(Properties)
	 */
	@Test
	public void testDefaultServiceConnectionProperties(){
		// Valid Configuration File:
		try{
			ServiceConnection<?> connection = new DefaultServiceConnection(validProp);
			assertNotNull(connection.getLogger());
			assertNotNull(connection.getFileManager());
			assertNotNull(connection.getFactory());
			assertTrue(connection.isAvailable());
			assertTrue(connection.getRetentionPeriod()[0] <= connection.getRetentionPeriod()[1]);
			assertTrue(connection.getExecutionDuration()[0] <= connection.getExecutionDuration()[1]);
		}catch(Exception e){
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// No File Manager:
		try{
			new DefaultServiceConnection(noFmProp);
			fail("This MUST have failed because no File Manager is specified!");
		}catch(Exception e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "The property \"" + KEY_FILE_MANAGER + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_LOCAL + " or a class path between {...}.");
		}

		// File Manager = Class Path:
		try{
			ServiceConnection<?> connection = new DefaultServiceConnection(fmClassPathProp);
			assertNotNull(connection.getLogger());
			assertNotNull(connection.getFileManager());
			assertNotNull(connection.getFactory());
			assertTrue(connection.isAvailable());

			/* Retention periods and execution durations are different in this configuration file from the valid one (validProp).
			 * Max period and max duration are set in this file as less than respectively the default period and the default duration.
			 * In such situation, the default period/duration is set to the maximum one, in order to ensure that the maximum value is
			 * still greater or equals than the default one. So the max and default values must be equal there.
			 */
			assertTrue(connection.getRetentionPeriod()[0] == connection.getRetentionPeriod()[1]);
			assertTrue(connection.getExecutionDuration()[0] == connection.getExecutionDuration()[1]);
		}catch(Exception e){
			fail("This MUST have succeeded because the provided file manager is a class path valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Incorrect File Manager Value:
		try{
			new DefaultServiceConnection(incorrectFmProp);
			fail("This MUST have failed because an incorrect File Manager value has been provided!");
		}catch(Exception e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "Unknown value for the propertie \"" + KEY_FILE_MANAGER + "\": \"foo\". Only two possible values: " + VALUE_LOCAL + " or a class path between {...}.");
		}
	}

	/**
	 * TEST isClassPath(String):
	 * 	- null, "", "{}", "an incorrect syntax" 				=> FALSE must be returned
	 * 	- "{ }", "{ 	}", "{class.path}", "{ class.path	}" 	=> TRUE must be returned
	 * 
	 * @see DefaultServiceConnection#isClassPath(String)
	 */
	@Test
	public void testIsClassPath(){
		// NULL and EMPTY:
		assertFalse(isClassPath(null));
		assertFalse(isClassPath(""));

		// EMPTY CLASSPATH:
		assertFalse(isClassPath("{}"));

		// INCORRECT CLASSPATH:
		assertFalse(isClassPath("incorrect class path ; missing {}"));

		// VALID CLASSPATH:
		assertTrue(isClassPath("{class.path}"));

		// CLASSPATH VALID ONLY IN THE SYNTAX:
		assertTrue(isClassPath("{ }"));
		assertTrue(isClassPath("{		}"));

		// NOT TRIM CLASSPATH:
		assertTrue(isClassPath("{ class.path	}"));
	}

	/**
	 * TEST getClass(String,String,String):
	 * 	- null, "", "{}", "an incorrect syntax", "{ }", "{ 	}" 						=> NULL must be returned
	 * 	- "{java.lang.String}", "{ java.lang.String	}"								=> a valid DefaultServiceConnection must be returned
	 * 	- "{mypackage.foo}", "{java.util.ArrayList}" (while a String is expected)	=> a TAPException must be thrown
	 */
	@Test
	public void testGetClassStringStringString(){
		// NULL and EMPTY:
		try{
			assertNull(fetchClass(null, KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If a NULL value is provided as classpath: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}
		try{
			assertNull(fetchClass("", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY value is provided as classpath: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// EMPTY CLASSPATH:
		try{
			assertNull(fetchClass("{}", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY classpath is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// INCORRECT SYNTAX:
		try{
			assertNull(fetchClass("incorrect class path ; missing {}", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an incorrect classpath is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// VALID CLASSPATH:
		try{
			Class<String> classObject = fetchClass("{java.lang.String}", KEY_FILE_MANAGER, String.class);
			assertNotNull(classObject);
			assertEquals(classObject.getName(), "java.lang.String");
		}catch(TAPException e){
			fail("If a VALID classpath is provided: getClass(...) MUST return a Class object of the wanted type!\nCaught exception: " + getPertinentMessage(e));
		}

		// INCORRECT CLASSPATH:
		try{
			fetchClass("{mypackage.foo}", KEY_FILE_MANAGER, String.class);
			fail("This MUST have failed because an incorrect classpath is provided!");
		}catch(TAPException e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "The class specified by the property " + KEY_FILE_MANAGER + " ({mypackage.foo}) can not be found.");
		}

		// INCOMPATIBLE TYPES:
		try{
			@SuppressWarnings("unused")
			Class<String> classObject = fetchClass("{java.util.ArrayList}", KEY_FILE_MANAGER, String.class);
			fail("This MUST have failed because a class of a different type has been asked!");
		}catch(TAPException e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "The class specified by the property " + KEY_FILE_MANAGER + " ({java.util.ArrayList}) is not implementing " + String.class.getName() + ".");
		}

		// CLASSPATH VALID ONLY IN THE SYNTAX:
		try{
			assertNull(fetchClass("{ }", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY classpath is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}
		try{
			assertNull(fetchClass("{		}", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY classpath is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// NOT TRIM CLASSPATH:
		try{
			Class<?> classObject = fetchClass("{ java.lang.String	}", KEY_FILE_MANAGER, String.class);
			assertNotNull(classObject);
			assertEquals(classObject.getName(), "java.lang.String");
		}catch(TAPException e){
			fail("If a VALID classpath is provided: getClass(...) MUST return a Class object of the wanted type!\nCaught exception: " + getPertinentMessage(e));
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

	/**
	 * A TAPFileManager to test the load of a TAPFileManager from the configuration file with a class path.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 11/2013
	 * @see TestDefaultServiceConnection#testDefaultServiceConnectionProperties()
	 */
	public static class FileManagerTest extends LocalTAPFileManager {
		public FileManagerTest(Properties tapConfig) throws UWSException{
			super(new File(tapConfig.getProperty("file_root_path")), true, false);
		}
	}

}
