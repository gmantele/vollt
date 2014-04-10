package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static tap.config.TAPConfiguration.KEY_DEFAULT_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_FILE_MANAGER;
import static tap.config.TAPConfiguration.KEY_MAX_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_OUTPUT_FORMATS;
import static tap.config.TAPConfiguration.VALUE_CSV;
import static tap.config.TAPConfiguration.VALUE_JSON;
import static tap.config.TAPConfiguration.VALUE_LOCAL;
import static tap.config.TAPConfiguration.VALUE_SV;
import static tap.config.TAPConfiguration.VALUE_TSV;

import java.io.File;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.file.LocalTAPFileManager;
import uws.UWSException;

public class TestDefaultServiceConnection {

	private Properties validProp, noFmProp, fmClassPathProp, incorrectFmProp,
			validFormatsProp, badSVFormat1Prop, badSVFormat2Prop,
			unknownFormatProp, defaultOutputLimitProp, maxOutputLimitProp,
			bothOutputLimitGoodProp, bothOutputLimitBadProp;

	@Before
	public void setUp() throws Exception{
		// LOAD ALL PROPERTIES FILES NEEDED FOR ALL THE TESTS:
		validProp = AllTests.getValidProperties();

		noFmProp = (Properties)validProp.clone();
		noFmProp.setProperty(KEY_FILE_MANAGER, "");

		fmClassPathProp = (Properties)validProp.clone();
		fmClassPathProp.setProperty(KEY_FILE_MANAGER, "{tap.config.TestDefaultServiceConnection$FileManagerTest}");

		incorrectFmProp = (Properties)validProp.clone();
		incorrectFmProp.setProperty(KEY_FILE_MANAGER, "foo");

		validFormatsProp = (Properties)validProp.clone();
		validFormatsProp.setProperty(KEY_OUTPUT_FORMATS, VALUE_JSON + "," + VALUE_CSV + " , " + VALUE_TSV + ",, , " + VALUE_SV + "([])" + ", " + VALUE_SV + "(|):text/psv:psv" + ", " + VALUE_SV + "($)::test" + ", \t  " + VALUE_SV + "(@):text/arobase:");

		badSVFormat1Prop = (Properties)validProp.clone();
		badSVFormat1Prop.setProperty(KEY_OUTPUT_FORMATS, VALUE_SV);

		badSVFormat2Prop = (Properties)validProp.clone();
		badSVFormat2Prop.setProperty(KEY_OUTPUT_FORMATS, VALUE_SV + "()");

		unknownFormatProp = (Properties)validProp.clone();
		unknownFormatProp.setProperty(KEY_OUTPUT_FORMATS, "foo");

		defaultOutputLimitProp = (Properties)validProp.clone();
		defaultOutputLimitProp.setProperty(KEY_DEFAULT_OUTPUT_LIMIT, "100");

		maxOutputLimitProp = (Properties)validProp.clone();
		maxOutputLimitProp.setProperty(KEY_MAX_OUTPUT_LIMIT, "1000R");

		bothOutputLimitGoodProp = (Properties)validProp.clone();
		bothOutputLimitGoodProp.setProperty(KEY_DEFAULT_OUTPUT_LIMIT, "100R");
		bothOutputLimitGoodProp.setProperty(KEY_MAX_OUTPUT_LIMIT, "1000");

		bothOutputLimitBadProp = (Properties)validProp.clone();
		bothOutputLimitBadProp.setProperty(KEY_DEFAULT_OUTPUT_LIMIT, "1000");
		bothOutputLimitBadProp.setProperty(KEY_MAX_OUTPUT_LIMIT, "100");
	}

	/**
	 * CONSTRUCTOR TESTS
	 *  * In general:
	 * 		- A valid configuration file builds successfully a fully functional ServiceConnection object.
	 * 
	 * 	* Over the file manager:
	 * 		- If no TAPFileManager is provided, an exception must be thrown. 
	 * 		- If a classpath toward a valid TAPFileManager is provided, a functional DefaultServiceConnection must be successfully built.
	 * 		- An incorrect file manager value in the configuration file must generate an exception.
	 * 
	 *  * Over the output format:
	 *  	- If a SV format is badly expressed (test with "sv" and "sv()"), an exception must be thrown.
	 *  	- If an unknown output format is provided an exception must be thrown.
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
			assertEquals(e.getMessage(), "Unknown value for the property \"" + KEY_FILE_MANAGER + "\": \"foo\". Only two possible values: " + VALUE_LOCAL + " or a class path between {...}.");
		}

		// Valid output formats list:
		try{
			ServiceConnection<?> connection = new DefaultServiceConnection(validFormatsProp);
			assertNotNull(connection.getOutputFormat(VALUE_JSON));
			assertNotNull(connection.getOutputFormat(VALUE_CSV));
			assertNotNull(connection.getOutputFormat(VALUE_TSV));
			assertNotNull(connection.getOutputFormat("psv"));
			assertNotNull(connection.getOutputFormat("text/psv"));
			assertNotNull(connection.getOutputFormat("text"));
			assertNotNull(connection.getOutputFormat("text/plain"));
			assertNotNull(connection.getOutputFormat("test"));
			assertNotNull(connection.getOutputFormat("text/arobase"));
		}catch(Exception e){
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Bad SV(...) format 1 = "sv":
		try{
			new DefaultServiceConnection(badSVFormat1Prop);
			fail("This MUST have failed because an incorrect SV output format value has been provided!");
		}catch(Exception e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "Missing separator char/string for the SV output format: \"sv\"!");
		}

		// Bad SV(...) format 2 = "sv()":
		try{
			new DefaultServiceConnection(badSVFormat2Prop);
			fail("This MUST have failed because an incorrect SV output format value has been provided!");
		}catch(Exception e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "Missing separator char/string for the SV output format: \"sv()\"!");
		}

		// Unknown output format:
		try{
			new DefaultServiceConnection(unknownFormatProp);
			fail("This MUST have failed because an incorrect output format value has been provided!");
		}catch(Exception e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "Unknown output format: foo");
		}

		// Test with no output limit specified:
		try{
			ServiceConnection<?> connection = new DefaultServiceConnection(validProp);
			assertEquals(connection.getOutputLimit()[0], -1);
			assertEquals(connection.getOutputLimit()[1], -1);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because providing no output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set default output limit:
		try{
			ServiceConnection<?> connection = new DefaultServiceConnection(defaultOutputLimitProp);
			assertEquals(connection.getOutputLimit()[0], 100);
			assertEquals(connection.getOutputLimit()[1], -1);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because setting the default output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set maximum output limit:
		try{
			ServiceConnection<?> connection = new DefaultServiceConnection(maxOutputLimitProp);
			assertEquals(connection.getOutputLimit()[0], -1);
			assertEquals(connection.getOutputLimit()[1], 1000);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because setting only the maximum output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum output limits where default <= max:
		try{
			ServiceConnection<?> connection = new DefaultServiceConnection(bothOutputLimitGoodProp);
			assertEquals(connection.getOutputLimit()[0], 100);
			assertEquals(connection.getOutputLimit()[1], 1000);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because the default output limit is less or equal the maximum one! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum output limits BUT where default > max:
		try{
			new DefaultServiceConnection(bothOutputLimitBadProp);
			fail("This MUST have failed because the default output limit is greater than the maximum one!");
		}catch(Exception e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "The default output limit (here: 1000) MUST be less or equal to the maximum output limit (here: 100)!");
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
