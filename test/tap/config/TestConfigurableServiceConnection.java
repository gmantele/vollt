package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static tap.config.TAPConfiguration.DEFAULT_MAX_ASYNC_JOBS;
import static tap.config.TAPConfiguration.KEY_DEFAULT_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_FILE_MANAGER;
import static tap.config.TAPConfiguration.KEY_GEOMETRIES;
import static tap.config.TAPConfiguration.KEY_MAX_ASYNC_JOBS;
import static tap.config.TAPConfiguration.KEY_MAX_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_METADATA;
import static tap.config.TAPConfiguration.KEY_METADATA_FILE;
import static tap.config.TAPConfiguration.KEY_OUTPUT_FORMATS;
import static tap.config.TAPConfiguration.KEY_UDFS;
import static tap.config.TAPConfiguration.KEY_USER_IDENTIFIER;
import static tap.config.TAPConfiguration.VALUE_ANY;
import static tap.config.TAPConfiguration.VALUE_CSV;
import static tap.config.TAPConfiguration.VALUE_DB;
import static tap.config.TAPConfiguration.VALUE_JSON;
import static tap.config.TAPConfiguration.VALUE_LOCAL;
import static tap.config.TAPConfiguration.VALUE_NONE;
import static tap.config.TAPConfiguration.VALUE_SV;
import static tap.config.TAPConfiguration.VALUE_TSV;
import static tap.config.TAPConfiguration.VALUE_XML;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.junit.BeforeClass;
import org.junit.Test;

import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import uws.UWSException;
import uws.job.user.DefaultJobOwner;
import uws.job.user.JobOwner;
import uws.service.UWSUrl;
import uws.service.UserIdentifier;
import uws.service.file.LocalUWSFileManager;
import adql.db.FunctionDef;
import adql.db.TestDBChecker.UDFToto;

public class TestConfigurableServiceConnection {

	private final static String XML_FILE = "test/tap/config/tables.xml";

	private static Properties validProp, noFmProp, fmClassPathProp,
			incorrectFmProp, xmlMetaProp, missingMetaProp, missingMetaFileProp,
			wrongMetaProp, wrongMetaFileProp, validFormatsProp,
			badSVFormat1Prop, badSVFormat2Prop, unknownFormatProp,
			maxAsyncProp, negativeMaxAsyncProp, notIntMaxAsyncProp,
			defaultOutputLimitProp, maxOutputLimitProp,
			bothOutputLimitGoodProp, bothOutputLimitBadProp, userIdentProp,
			notClassPathUserIdentProp, geometriesProp, noneGeomProp,
			anyGeomProp, noneInsideGeomProp, unknownGeomProp, anyUdfsProp,
			noneUdfsProp, udfsProp, udfsWithClassPathProp,
			udfsListWithNONEorANYProp, udfsWithWrongParamLengthProp,
			udfsWithMissingBracketsProp, udfsWithMissingDefProp1,
			udfsWithMissingDefProp2, emptyUdfItemProp1, emptyUdfItemProp2,
			udfWithMissingEndBracketProp;

	@BeforeClass
	public static void setUp() throws Exception{
		// LOAD ALL PROPERTIES FILES NEEDED FOR ALL THE TESTS:
		validProp = AllTests.getValidProperties();

		noFmProp = (Properties)validProp.clone();
		noFmProp.setProperty(KEY_FILE_MANAGER, "");

		fmClassPathProp = (Properties)validProp.clone();
		fmClassPathProp.setProperty(KEY_FILE_MANAGER, "{tap.config.TestDefaultServiceConnection$FileManagerTest}");

		incorrectFmProp = (Properties)validProp.clone();
		incorrectFmProp.setProperty(KEY_FILE_MANAGER, "foo");

		xmlMetaProp = (Properties)validProp.clone();
		xmlMetaProp.setProperty(KEY_METADATA, VALUE_XML);
		xmlMetaProp.setProperty(KEY_METADATA_FILE, XML_FILE);

		missingMetaProp = (Properties)validProp.clone();
		missingMetaProp.remove(KEY_METADATA);

		wrongMetaProp = (Properties)validProp.clone();
		wrongMetaProp.setProperty(KEY_METADATA, "foo");

		wrongMetaFileProp = (Properties)validProp.clone();
		wrongMetaFileProp.setProperty(KEY_METADATA, VALUE_XML);
		wrongMetaFileProp.setProperty(KEY_METADATA_FILE, "foo");

		missingMetaFileProp = (Properties)validProp.clone();
		missingMetaFileProp.setProperty(KEY_METADATA, VALUE_XML);
		missingMetaFileProp.remove(KEY_METADATA_FILE);

		validFormatsProp = (Properties)validProp.clone();
		validFormatsProp.setProperty(KEY_OUTPUT_FORMATS, VALUE_JSON + "," + VALUE_CSV + " , " + VALUE_TSV + ",, , " + VALUE_SV + "([])" + ", " + VALUE_SV + "(|):text/psv:psv" + ", " + VALUE_SV + "($)::test" + ", \t  " + VALUE_SV + "(@):text/arobase:");

		badSVFormat1Prop = (Properties)validProp.clone();
		badSVFormat1Prop.setProperty(KEY_OUTPUT_FORMATS, VALUE_SV);

		badSVFormat2Prop = (Properties)validProp.clone();
		badSVFormat2Prop.setProperty(KEY_OUTPUT_FORMATS, VALUE_SV + "()");

		unknownFormatProp = (Properties)validProp.clone();
		unknownFormatProp.setProperty(KEY_OUTPUT_FORMATS, "foo");

		maxAsyncProp = (Properties)validProp.clone();
		maxAsyncProp.setProperty(KEY_MAX_ASYNC_JOBS, "10");

		negativeMaxAsyncProp = (Properties)validProp.clone();
		negativeMaxAsyncProp.setProperty(KEY_MAX_ASYNC_JOBS, "-2");

		notIntMaxAsyncProp = (Properties)validProp.clone();
		notIntMaxAsyncProp.setProperty(KEY_MAX_ASYNC_JOBS, "foo");

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

		userIdentProp = (Properties)validProp.clone();
		userIdentProp.setProperty(KEY_USER_IDENTIFIER, "{tap.config.TestDefaultServiceConnection$UserIdentifierTest}");

		notClassPathUserIdentProp = (Properties)validProp.clone();
		notClassPathUserIdentProp.setProperty(KEY_USER_IDENTIFIER, "foo");

		geometriesProp = (Properties)validProp.clone();
		geometriesProp.setProperty(KEY_GEOMETRIES, "point, CIRCle  ,	cONTAins,intersECTS");

		noneGeomProp = (Properties)validProp.clone();
		noneGeomProp.setProperty(KEY_GEOMETRIES, VALUE_NONE);

		anyGeomProp = (Properties)validProp.clone();
		anyGeomProp.setProperty(KEY_GEOMETRIES, VALUE_ANY);

		noneInsideGeomProp = (Properties)validProp.clone();
		noneInsideGeomProp.setProperty(KEY_GEOMETRIES, "POINT, Box, none, circle");

		unknownGeomProp = (Properties)validProp.clone();
		unknownGeomProp.setProperty(KEY_GEOMETRIES, "POINT, Contains, foo, circle,Polygon");

		anyUdfsProp = (Properties)validProp.clone();
		anyUdfsProp.setProperty(KEY_UDFS, VALUE_ANY);

		noneUdfsProp = (Properties)validProp.clone();
		noneUdfsProp.setProperty(KEY_UDFS, VALUE_NONE);

		udfsProp = (Properties)validProp.clone();
		udfsProp.setProperty(KEY_UDFS, "[toto(a string)] ,	[  titi(b REAL) -> double 	]");

		udfsWithClassPathProp = (Properties)validProp.clone();
		udfsWithClassPathProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}]");

		udfsListWithNONEorANYProp = (Properties)validProp.clone();
		udfsListWithNONEorANYProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR],ANY");

		udfsWithWrongParamLengthProp = (Properties)validProp.clone();
		udfsWithWrongParamLengthProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}, foo]");

		udfsWithMissingBracketsProp = (Properties)validProp.clone();
		udfsWithMissingBracketsProp.setProperty(KEY_UDFS, "toto(a string)->VARCHAR");

		udfsWithMissingDefProp1 = (Properties)validProp.clone();
		udfsWithMissingDefProp1.setProperty(KEY_UDFS, "[{adql.db.TestDBChecker$UDFToto}]");

		udfsWithMissingDefProp2 = (Properties)validProp.clone();
		udfsWithMissingDefProp2.setProperty(KEY_UDFS, "[,{adql.db.TestDBChecker$UDFToto}]");

		emptyUdfItemProp1 = (Properties)validProp.clone();
		emptyUdfItemProp1.setProperty(KEY_UDFS, "[ ]");

		emptyUdfItemProp2 = (Properties)validProp.clone();
		emptyUdfItemProp2.setProperty(KEY_UDFS, "[ ,	 ]");

		udfWithMissingEndBracketProp = (Properties)validProp.clone();
		udfWithMissingEndBracketProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR");
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
	 * Note: the good configuration of the TAPFactory built by the DefaultServiceConnection is tested in {@link TestConfigurableTAPFactory}.
	 * 
	 * @see ConfigurableServiceConnection#DefaultServiceConnection(Properties)
	 */
	@Test
	public void testDefaultServiceConnectionProperties(){
		// Valid Configuration File:
		PrintWriter writer = null;
		int nbSchemas = -1, nbTables = -1;
		try{
			// build the ServiceConnection:
			ServiceConnection connection = new ConfigurableServiceConnection(validProp);

			// tests:
			assertNotNull(connection.getLogger());
			assertNotNull(connection.getFileManager());
			assertNotNull(connection.getFactory());
			assertNotNull(connection.getTAPMetadata());
			assertTrue(connection.getTAPMetadata().getNbSchemas() >= 1);
			assertTrue(connection.getTAPMetadata().getNbTables() >= 5);
			assertTrue(connection.isAvailable());
			assertEquals(DEFAULT_MAX_ASYNC_JOBS, connection.getNbMaxAsyncJobs());
			assertTrue(connection.getRetentionPeriod()[0] <= connection.getRetentionPeriod()[1]);
			assertTrue(connection.getExecutionDuration()[0] <= connection.getExecutionDuration()[1]);
			assertNull(connection.getUserIdentifier());
			assertNull(connection.getGeometries());
			assertEquals(0, connection.getUDFs().size());

			// finally, save metadata in an XML file for the other tests:
			writer = new PrintWriter(new File(XML_FILE));
			connection.getTAPMetadata().write(writer);
			nbSchemas = connection.getTAPMetadata().getNbSchemas();
			nbTables = connection.getTAPMetadata().getNbTables();

		}catch(Exception e){
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		}finally{
			if (writer != null)
				writer.close();
		}

		// Valid XML metadata:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(xmlMetaProp);
			assertNotNull(connection.getLogger());
			assertNotNull(connection.getFileManager());
			assertNotNull(connection.getFactory());
			assertNotNull(connection.getTAPMetadata());
			assertEquals(nbSchemas, connection.getTAPMetadata().getNbSchemas());
			assertEquals(nbTables, connection.getTAPMetadata().getNbTables());
			assertTrue(connection.isAvailable());
			assertEquals(DEFAULT_MAX_ASYNC_JOBS, connection.getNbMaxAsyncJobs());
			assertTrue(connection.getRetentionPeriod()[0] <= connection.getRetentionPeriod()[1]);
			assertTrue(connection.getExecutionDuration()[0] <= connection.getExecutionDuration()[1]);
			assertNull(connection.getUserIdentifier());
			assertNull(connection.getGeometries());
			assertEquals(0, connection.getUDFs().size());
		}catch(Exception e){
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Missing metadata property:
		try{
			new ConfigurableServiceConnection(missingMetaProp);
			fail("This MUST have failed because the property 'metadata' is missing!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The property \"" + KEY_METADATA + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_XML + " (to get metadata from a TableSet XML document) or " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA).", e.getMessage());
		}

		// Missing metadata_file property:
		try{
			new ConfigurableServiceConnection(missingMetaFileProp);
			fail("This MUST have failed because the property 'metadata_file' is missing!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The property \"" + KEY_METADATA_FILE + "\" is missing! According to the property \"" + KEY_METADATA + "\", metadata must be fetched from an XML document. The local file path of it MUST be provided using the property \"" + KEY_METADATA_FILE + "\".", e.getMessage());
		}

		// Wrong metadata property:
		try{
			new ConfigurableServiceConnection(wrongMetaProp);
			fail("This MUST have failed because the property 'metadata' has a wrong value!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unsupported value for the property \"" + KEY_METADATA + "\": \"foo\"! Only two values are allowed: " + VALUE_XML + " (to get metadata from a TableSet XML document) or " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA).", e.getMessage());
		}

		// Wrong metadata_file property:
		try{
			new ConfigurableServiceConnection(wrongMetaFileProp);
			fail("This MUST have failed because the property 'metadata_file' has a wrong value!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("A grave error occurred while reading/parsing the TableSet XML document: \"foo\"!", e.getMessage());
		}

		// No File Manager:
		try{
			new ConfigurableServiceConnection(noFmProp);
			fail("This MUST have failed because no File Manager is specified!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The property \"" + KEY_FILE_MANAGER + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_LOCAL + " or a class path between {...}.", e.getMessage());
		}

		// File Manager = Class Path:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(fmClassPathProp);
			assertNotNull(connection.getLogger());
			assertNotNull(connection.getFileManager());
			assertNotNull(connection.getFactory());
			assertNotNull(connection.getTAPMetadata());
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
			new ConfigurableServiceConnection(incorrectFmProp);
			fail("This MUST have failed because an incorrect File Manager value has been provided!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unknown value for the property \"" + KEY_FILE_MANAGER + "\": \"foo\". Only two possible values: " + VALUE_LOCAL + " or a class path between {...}.", e.getMessage());
		}

		// Valid output formats list:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(validFormatsProp);
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
			new ConfigurableServiceConnection(badSVFormat1Prop);
			fail("This MUST have failed because an incorrect SV output format value has been provided!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing separator char/string for the SV output format: \"sv\"!", e.getMessage());
		}

		// Bad SV(...) format 2 = "sv()":
		try{
			new ConfigurableServiceConnection(badSVFormat2Prop);
			fail("This MUST have failed because an incorrect SV output format value has been provided!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing separator char/string for the SV output format: \"sv()\"!", e.getMessage());
		}

		// Unknown output format:
		try{
			new ConfigurableServiceConnection(unknownFormatProp);
			fail("This MUST have failed because an incorrect output format value has been provided!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unknown output format: foo", e.getMessage());
		}

		// Valid value for max_async_jobs:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(maxAsyncProp);
			assertEquals(10, connection.getNbMaxAsyncJobs());
		}catch(Exception e){
			fail("This MUST have succeeded because a valid max_async_jobs is provided! \nCaught exception: " + getPertinentMessage(e));
		}

		// Negative value for max_async_jobs:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(negativeMaxAsyncProp);
			assertEquals(-2, connection.getNbMaxAsyncJobs());
		}catch(Exception e){
			fail("This MUST have succeeded because a negative max_async_jobs is equivalent to 'no restriction'! \nCaught exception: " + getPertinentMessage(e));
		}

		// A not integer value for max_async_jobs:
		try{
			new ConfigurableServiceConnection(notIntMaxAsyncProp);
			fail("This MUST have failed because a not integer value has been provided for \"" + KEY_MAX_ASYNC_JOBS + "\"!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Integer expected for the property \"" + KEY_MAX_ASYNC_JOBS + "\", instead of: \"foo\"!", e.getMessage());
		}

		// Test with no output limit specified:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(validProp);
			assertEquals(connection.getOutputLimit()[0], -1);
			assertEquals(connection.getOutputLimit()[1], -1);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because providing no output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set default output limit:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(defaultOutputLimitProp);
			assertEquals(connection.getOutputLimit()[0], 100);
			assertEquals(connection.getOutputLimit()[1], -1);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because setting the default output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set maximum output limit:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(maxOutputLimitProp);
			assertEquals(connection.getOutputLimit()[0], -1);
			assertEquals(connection.getOutputLimit()[1], 1000);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because setting only the maximum output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum output limits where default <= max:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(bothOutputLimitGoodProp);
			assertEquals(connection.getOutputLimit()[0], 100);
			assertEquals(connection.getOutputLimit()[1], 1000);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		}catch(Exception e){
			fail("This MUST have succeeded because the default output limit is less or equal the maximum one! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum output limits BUT where default > max:
		try{
			new ConfigurableServiceConnection(bothOutputLimitBadProp);
			fail("This MUST have failed because the default output limit is greater than the maximum one!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The default output limit (here: 1000) MUST be less or equal to the maximum output limit (here: 100)!", e.getMessage());
		}

		// Valid user identifier:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(userIdentProp);
			assertNotNull(connection.getUserIdentifier());
			assertNotNull(connection.getUserIdentifier().extractUserId(null, null));
			assertEquals("everybody", connection.getUserIdentifier().extractUserId(null, null).getID());
		}catch(Exception e){
			fail("This MUST have succeeded because the class path toward the fake UserIdentifier is correct! \nCaught exception: " + getPertinentMessage(e));
		}

		// Not a class path for user_identifier:
		try{
			new ConfigurableServiceConnection(notClassPathUserIdentProp);
			fail("This MUST have failed because the user_identifier value is not a class path!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Class path expected for the property \"" + KEY_USER_IDENTIFIER + "\", instead of: \"foo\"!", e.getMessage());
		}

		// Valid geometry list:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(geometriesProp);
			assertNotNull(connection.getGeometries());
			assertEquals(4, connection.getGeometries().size());
			assertEquals("POINT", ((ArrayList<String>)connection.getGeometries()).get(0));
			assertEquals("CIRCLE", ((ArrayList<String>)connection.getGeometries()).get(1));
			assertEquals("CONTAINS", ((ArrayList<String>)connection.getGeometries()).get(2));
			assertEquals("INTERSECTS", ((ArrayList<String>)connection.getGeometries()).get(3));
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of geometries is correct! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" as geometry list:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(noneGeomProp);
			assertNotNull(connection.getGeometries());
			assertEquals(0, connection.getGeometries().size());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of geometries is correct (reduced to only NONE)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "ANY" as geometry list:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(anyGeomProp);
			assertNull(connection.getGeometries());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of geometries is correct (reduced to only ANY)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" inside a geometry list:
		try{
			new ConfigurableServiceConnection(noneInsideGeomProp);
			fail("This MUST have failed because the given geometry list contains at least 2 items, whose one is NONE!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The special value \"" + VALUE_NONE + "\" can not be used inside a list! It MUST be used in replacement of a whole list to specify that no value is allowed.", e.getMessage());
		}

		// Unknown geometrical function:
		try{
			new ConfigurableServiceConnection(unknownGeomProp);
			fail("This MUST have failed because the given geometry list contains at least 1 unknown ADQL geometrical function!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unknown ADQL geometrical function: \"foo\"!", e.getMessage());
		}

		// "ANY" as UDFs list:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(anyUdfsProp);
			assertNull(connection.getUDFs());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of UDFs is correct (reduced to only ANY)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" as UDFs list:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(noneUdfsProp);
			assertNotNull(connection.getUDFs());
			assertEquals(0, connection.getUDFs().size());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of UDFs is correct (reduced to only NONE)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(udfsProp);
			assertNotNull(connection.getUDFs());
			assertEquals(2, connection.getUDFs().size());
			Iterator<FunctionDef> it = connection.getUDFs().iterator();
			assertEquals("toto(a VARCHAR)", it.next().toString());
			assertEquals("titi(b REAL) -> DOUBLE", it.next().toString());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs containing one UDF with a classpath:
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(udfsWithClassPathProp);
			assertNotNull(connection.getUDFs());
			assertEquals(1, connection.getUDFs().size());
			FunctionDef def = connection.getUDFs().iterator().next();
			assertEquals("toto(a VARCHAR) -> VARCHAR", def.toString());
			assertEquals(UDFToto.class, def.getUDFClass());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" inside a UDFs list:
		try{
			new ConfigurableServiceConnection(udfsListWithNONEorANYProp);
			fail("This MUST have failed because the given UDFs list contains at least 2 items, whose one is ANY!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: unexpected character at position 27 in the property " + KEY_UDFS + ": \"A\"! A UDF declaration must have one of the following syntaxes: \"[signature]\" or \"[signature,{classpath}]\".", e.getMessage());
		}

		// UDF with no brackets:
		try{
			new ConfigurableServiceConnection(udfsWithMissingBracketsProp);
			fail("This MUST have failed because one UDFs list item has no brackets!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: unexpected character at position 1 in the property " + KEY_UDFS + ": \"t\"! A UDF declaration must have one of the following syntaxes: \"[signature]\" or \"[signature,{classpath}]\".", e.getMessage());
		}

		// UDFs whose one item have more parts than supported:
		try{
			new ConfigurableServiceConnection(udfsWithWrongParamLengthProp);
			fail("This MUST have failed because one UDFs list item has too many parameters!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: only two items (signature and classpath) can be given within brackets. (position in the property " + KEY_UDFS + ": 58)", e.getMessage());
		}

		// UDF with missing definition part (or wrong since there is no comma):
		try{
			new ConfigurableServiceConnection(udfsWithMissingDefProp1);
			fail("This MUST have failed because one UDFs list item has a wrong signature part (it has been forgotten)!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: Wrong function definition syntax! Expected syntax: \"<regular_identifier>(<parameters>?) <return_type>?\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <return_type>=\" -> <type_name>\", <parameters>=\"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"foo()\", \"foo() -> VARCHAR\", \"foo(param INTEGER)\", \"foo(param1 INTEGER, param2 DOUBLE) -> DOUBLE\" (position in the property " + KEY_UDFS + ": 2-33)", e.getMessage());
		}

		// UDF with missing definition part (or wrong since there is no comma):
		try{
			new ConfigurableServiceConnection(udfsWithMissingDefProp2);
			fail("This MUST have failed because one UDFs list item has no signature part!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing UDF declaration! (position in the property " + KEY_UDFS + ": 2-2)", e.getMessage());
		}

		// Empty UDF item (without comma):
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(emptyUdfItemProp1);
			assertNotNull(connection.getUDFs());
			assertEquals(0, connection.getUDFs().size());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of UDFs contains one empty UDF (which should be merely ignored)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Empty UDF item (with comma):
		try{
			ServiceConnection connection = new ConfigurableServiceConnection(emptyUdfItemProp2);
			assertNotNull(connection.getUDFs());
			assertEquals(0, connection.getUDFs().size());
		}catch(Exception e){
			fail("This MUST have succeeded because the given list of UDFs contains one empty UDF (which should be merely ignored)! \nCaught exception: " + getPertinentMessage(e));
		}

		// UDF item without its closing bracket:
		try{
			new ConfigurableServiceConnection(udfWithMissingEndBracketProp);
			fail("This MUST have failed because one UDFs list item has no closing bracket!");
		}catch(Exception e){
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: missing closing bracket at position 24!", e.getMessage());
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

	/**
	 * A UWSFileManager to test the load of a UWSFileManager from the configuration file with a class path.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 01/2015
	 * @see TestConfigurableServiceConnection#testDefaultServiceConnectionProperties()
	 */
	public static class FileManagerTest extends LocalUWSFileManager {
		public FileManagerTest(Properties tapConfig) throws UWSException{
			super(new File(tapConfig.getProperty("file_root_path")), true, false);
		}
	}

	/**
	 * A UserIdentifier which always return the same user...that's to say, all users are in a way still anonymous :-)
	 * This class is only for test purpose.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 02/2015
	 */
	public static class UserIdentifierTest implements UserIdentifier {
		private static final long serialVersionUID = 1L;

		private final JobOwner everybody = new DefaultJobOwner("everybody");

		@Override
		public JobOwner extractUserId(UWSUrl urlInterpreter, HttpServletRequest request) throws UWSException{
			return everybody;
		}

		@Override
		public JobOwner restoreUser(String id, String pseudo, Map<String,Object> otherData) throws UWSException{
			return everybody;
		}

	}

}
