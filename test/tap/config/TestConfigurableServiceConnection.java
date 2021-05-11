package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static tap.config.TAPConfiguration.DEFAULT_ASYNC_FETCH_SIZE;
import static tap.config.TAPConfiguration.DEFAULT_LOGGER;
import static tap.config.TAPConfiguration.DEFAULT_MAX_ASYNC_JOBS;
import static tap.config.TAPConfiguration.DEFAULT_SYNC_FETCH_SIZE;
import static tap.config.TAPConfiguration.KEY_ASYNC_FETCH_SIZE;
import static tap.config.TAPConfiguration.KEY_COORD_SYS;
import static tap.config.TAPConfiguration.KEY_DEFAULT_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_DEFAULT_UPLOAD_LIMIT;
import static tap.config.TAPConfiguration.KEY_FILE_MANAGER;
import static tap.config.TAPConfiguration.KEY_GEOMETRIES;
import static tap.config.TAPConfiguration.KEY_LOGGER;
import static tap.config.TAPConfiguration.KEY_LOG_ROTATION;
import static tap.config.TAPConfiguration.KEY_MAX_ASYNC_JOBS;
import static tap.config.TAPConfiguration.KEY_MAX_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_MAX_UPLOAD_LIMIT;
import static tap.config.TAPConfiguration.KEY_METADATA;
import static tap.config.TAPConfiguration.KEY_METADATA_FILE;
import static tap.config.TAPConfiguration.KEY_MIN_LOG_LEVEL;
import static tap.config.TAPConfiguration.KEY_OUTPUT_FORMATS;
import static tap.config.TAPConfiguration.KEY_SYNC_FETCH_SIZE;
import static tap.config.TAPConfiguration.KEY_TAP_FACTORY;
import static tap.config.TAPConfiguration.KEY_UDFS;
import static tap.config.TAPConfiguration.KEY_USER_IDENTIFIER;
import static tap.config.TAPConfiguration.VALUE_ANY;
import static tap.config.TAPConfiguration.VALUE_CSV;
import static tap.config.TAPConfiguration.VALUE_DB;
import static tap.config.TAPConfiguration.VALUE_FITS;
import static tap.config.TAPConfiguration.VALUE_JSON;
import static tap.config.TAPConfiguration.VALUE_LOCAL;
import static tap.config.TAPConfiguration.VALUE_NONE;
import static tap.config.TAPConfiguration.VALUE_SV;
import static tap.config.TAPConfiguration.VALUE_TEXT;
import static tap.config.TAPConfiguration.VALUE_TSV;
import static tap.config.TAPConfiguration.VALUE_VOTABLE;
import static tap.config.TAPConfiguration.VALUE_XML;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.FunctionDef;
import adql.db.TestDBChecker.UDFToto;
import adql.db.region.CoordSys.Flavor;
import adql.db.region.CoordSys.Frame;
import adql.db.region.CoordSys.RefPos;
import adql.query.operand.function.ADQLFunction;
import adql.translator.ADQLTranslator;
import adql.translator.AstroH2Translator;
import adql.translator.FunctionTranslator;
import adql.translator.TranslationException;
import tap.AbstractTAPFactory;
import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.db.DBConnection;
import tap.db.DBException;
import tap.db.JDBCConnection;
import tap.db_testtools.DBTools;
import tap.formatter.OutputFormat;
import tap.formatter.VOTableFormat;
import tap.log.DefaultTAPLog;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPSchema;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uws.UWSException;
import uws.job.user.DefaultJobOwner;
import uws.job.user.JobOwner;
import uws.service.UWSUrl;
import uws.service.UserIdentifier;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;
import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog.LogLevel;

public class TestConfigurableServiceConnection {

	private final static String XML_FILE = "test/tap/config/tables.xml";

	private static Properties validProp, noFmProp, fmClassNameProp,
			incorrectFmProp, correctLogProp, incorrectLogLevelProp,
			incorrectLogRotationProp, customLoggerProp,
			explicitDefaultLoggerProp, xmlMetaProp,
			xmlMetaPropWithCustomMetaClass, xmlMetaPropWithBadCustomMetaClass,
			xmlMetaPropWithANonMetaClass, wrongManualMetaProp, missingMetaProp,
			missingMetaFileProp, wrongMetaProp, wrongMetaFileProp,
			validFormatsProp, validVOTableFormatsProp, badSVFormat1Prop,
			badSVFormat2Prop, badVotFormat1Prop, badVotFormat2Prop,
			badVotFormat3Prop, badVotFormat4Prop, badVotFormat5Prop,
			badVotFormat6Prop, unknownFormatProp, maxAsyncProp,
			negativeMaxAsyncProp, notIntMaxAsyncProp, defaultOutputLimitProp,
			maxOutputLimitProp, bothOutputLimitGoodProp, bothOutputLimitBadProp,
			defaultUploadLimitProp, maxUploadLimitProp, bothUploadLimitGoodProp,
			bothUploadLimitBadProp, syncFetchSizeProp, notIntSyncFetchSizeProp,
			negativeSyncFetchSizeProp, notIntAsyncFetchSizeProp,
			negativeAsyncFetchSizeProp, asyncFetchSizeProp, userIdentProp,
			notClassPathUserIdentProp, coordSysProp, noneCoordSysProp,
			anyCoordSysProp, noneInsideCoordSysProp, unknownCoordSysProp,
			geometriesProp, noneGeomProp, anyGeomProp, noneInsideGeomProp,
			unknownGeomProp, anyUdfsProp, noneUdfsProp, udfsProp,
			udfsWithTranslationProp, udfsWithUDFClassProp, udfsWithTranslatorClassProp,
			udfsWithClassNameAndDescriptionProp, udfsWithEmptyOptParamsProp,
			udfsListWithNONEorANYProp, udfsWithWrongDescriptionFormatProp,
			udfsWithWrongParamLengthProp, udfsWithMissingBracketsProp,
			udfsWithMissingDefProp1, udfsWithMissingDefProp2, emptyUdfItemProp1,
			emptyUdfItemProp2, udfWithMissingEndBracketProp, customFactoryProp,
			customConfigurableFactoryProp, badCustomFactoryProp;

	@BeforeClass
	public static void setUp() throws Exception {
		// LOAD THE VALID PROPERTIES:
		validProp = AllTAPConfigTests.getValidProperties();

		// CREATE THE DB AND ITS TAP_SCHEMA:
		DBTools.createTestDB();
		DBTools.createAddTAPSchema();

		// LOAD ALL OTHER PROPERTIES FILES NEEDED FOR ALL THE TESTS:

		noFmProp = (Properties)validProp.clone();
		noFmProp.setProperty(KEY_FILE_MANAGER, "");

		fmClassNameProp = (Properties)validProp.clone();
		fmClassNameProp.setProperty(KEY_FILE_MANAGER, "{tap.config.TestConfigurableServiceConnection$FileManagerTest}");

		incorrectFmProp = (Properties)validProp.clone();
		incorrectFmProp.setProperty(KEY_FILE_MANAGER, "foo");

		correctLogProp = (Properties)validProp.clone();
		correctLogProp.setProperty(KEY_LOG_ROTATION, "	M	  	5  6	03 ");
		correctLogProp.setProperty(KEY_MIN_LOG_LEVEL, "	WARNing ");

		incorrectLogLevelProp = (Properties)validProp.clone();
		incorrectLogLevelProp.setProperty(KEY_MIN_LOG_LEVEL, "foo");

		incorrectLogRotationProp = (Properties)validProp.clone();
		incorrectLogRotationProp.setProperty(KEY_LOG_ROTATION, "foo");

		customLoggerProp = (Properties)validProp.clone();
		customLoggerProp.setProperty(KEY_LOGGER, "{tap.config.TestConfigurableServiceConnection$CustomLogger}");

		explicitDefaultLoggerProp = (Properties)validProp.clone();
		explicitDefaultLoggerProp.setProperty(KEY_LOGGER, DEFAULT_LOGGER);

		xmlMetaProp = (Properties)validProp.clone();
		xmlMetaProp.setProperty(KEY_METADATA, VALUE_XML);
		xmlMetaProp.setProperty(KEY_METADATA_FILE, XML_FILE);

		xmlMetaPropWithCustomMetaClass = (Properties)validProp.clone();
		xmlMetaPropWithCustomMetaClass.setProperty(KEY_METADATA, VALUE_XML + " {tap.config.TestConfigurableServiceConnection$MyCustomTAPMetadata}");
		xmlMetaPropWithCustomMetaClass.setProperty(KEY_METADATA_FILE, XML_FILE);

		xmlMetaPropWithBadCustomMetaClass = (Properties)validProp.clone();
		xmlMetaPropWithBadCustomMetaClass.setProperty(KEY_METADATA, VALUE_XML + " {tap.config.TestConfigurableServiceConnection$MyBadCustomTAPMetadata}");
		xmlMetaPropWithBadCustomMetaClass.setProperty(KEY_METADATA_FILE, XML_FILE);

		xmlMetaPropWithANonMetaClass = (Properties)validProp.clone();
		xmlMetaPropWithANonMetaClass.setProperty(KEY_METADATA, VALUE_XML + " MyCustomTAPMetadata");
		xmlMetaPropWithANonMetaClass.setProperty(KEY_METADATA_FILE, XML_FILE);

		wrongManualMetaProp = (Properties)validProp.clone();
		wrongManualMetaProp.setProperty(KEY_METADATA, "{tap.metadata.TAPMetadata}");

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
		validFormatsProp.setProperty(KEY_OUTPUT_FORMATS, VALUE_FITS + "," + VALUE_TEXT + "," + VALUE_JSON + "," + VALUE_CSV + " , " + VALUE_TSV + ",, , " + VALUE_SV + "([])" + ", " + VALUE_SV + "(|):text/psv:psv" + ", " + VALUE_SV + "($)::test" + ", \t  " + VALUE_SV + "(@):text/arobase:" + ", {tap.formatter.HTMLFormat}");

		validVOTableFormatsProp = (Properties)validProp.clone();
		validVOTableFormatsProp.setProperty(KEY_OUTPUT_FORMATS, "votable, votable()::, vot(), vot::, votable:, votable(Td, 1.0), vot(TableData), votable(,1.2), vot(Fits):application/fits:supervot");

		badSVFormat1Prop = (Properties)validProp.clone();
		badSVFormat1Prop.setProperty(KEY_OUTPUT_FORMATS, VALUE_SV);

		badSVFormat2Prop = (Properties)validProp.clone();
		badSVFormat2Prop.setProperty(KEY_OUTPUT_FORMATS, VALUE_SV + "()");

		badVotFormat1Prop = (Properties)validProp.clone();
		badVotFormat1Prop.setProperty(KEY_OUTPUT_FORMATS, "votable(foo)");

		badVotFormat2Prop = (Properties)validProp.clone();
		badVotFormat2Prop.setProperty(KEY_OUTPUT_FORMATS, "vot(,foo)");

		badVotFormat3Prop = (Properties)validProp.clone();
		badVotFormat3Prop.setProperty(KEY_OUTPUT_FORMATS, "text, vot(TD");

		badVotFormat4Prop = (Properties)validProp.clone();
		badVotFormat4Prop.setProperty(KEY_OUTPUT_FORMATS, "vot(TD, text");

		badVotFormat5Prop = (Properties)validProp.clone();
		badVotFormat5Prop.setProperty(KEY_OUTPUT_FORMATS, "vot(TD, 1.0, foo)");

		badVotFormat6Prop = (Properties)validProp.clone();
		badVotFormat6Prop.setProperty(KEY_OUTPUT_FORMATS, "vot:application/xml:votable:foo");

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

		defaultUploadLimitProp = (Properties)validProp.clone();
		defaultUploadLimitProp.setProperty(KEY_DEFAULT_UPLOAD_LIMIT, "100r");

		maxUploadLimitProp = (Properties)validProp.clone();
		maxUploadLimitProp.setProperty(KEY_MAX_UPLOAD_LIMIT, "1000R");

		bothUploadLimitGoodProp = (Properties)validProp.clone();
		bothUploadLimitGoodProp.setProperty(KEY_DEFAULT_UPLOAD_LIMIT, "100R");
		bothUploadLimitGoodProp.setProperty(KEY_MAX_UPLOAD_LIMIT, "10kB");

		bothUploadLimitBadProp = (Properties)validProp.clone();
		bothUploadLimitBadProp.setProperty(KEY_DEFAULT_UPLOAD_LIMIT, "1000r");
		bothUploadLimitBadProp.setProperty(KEY_MAX_UPLOAD_LIMIT, "100R");

		syncFetchSizeProp = (Properties)validProp.clone();
		syncFetchSizeProp.setProperty(KEY_SYNC_FETCH_SIZE, "50");

		notIntSyncFetchSizeProp = (Properties)validProp.clone();
		notIntSyncFetchSizeProp.setProperty(KEY_SYNC_FETCH_SIZE, "foo");

		negativeSyncFetchSizeProp = (Properties)validProp.clone();
		negativeSyncFetchSizeProp.setProperty(KEY_SYNC_FETCH_SIZE, "-3");

		asyncFetchSizeProp = (Properties)validProp.clone();
		asyncFetchSizeProp.setProperty(KEY_ASYNC_FETCH_SIZE, "50");

		notIntAsyncFetchSizeProp = (Properties)validProp.clone();
		notIntAsyncFetchSizeProp.setProperty(KEY_ASYNC_FETCH_SIZE, "foo");

		negativeAsyncFetchSizeProp = (Properties)validProp.clone();
		negativeAsyncFetchSizeProp.setProperty(KEY_ASYNC_FETCH_SIZE, "-3");

		userIdentProp = (Properties)validProp.clone();
		userIdentProp.setProperty(KEY_USER_IDENTIFIER, "{tap.config.TestConfigurableServiceConnection$UserIdentifierTest}");

		notClassPathUserIdentProp = (Properties)validProp.clone();
		notClassPathUserIdentProp.setProperty(KEY_USER_IDENTIFIER, "foo");

		coordSysProp = (Properties)validProp.clone();
		coordSysProp.setProperty(KEY_COORD_SYS, "icrs *		*, ICrs * (Spherical2|  	CARTEsian2)");

		noneCoordSysProp = (Properties)validProp.clone();
		noneCoordSysProp.setProperty(KEY_COORD_SYS, VALUE_NONE);

		anyCoordSysProp = (Properties)validProp.clone();
		anyCoordSysProp.setProperty(KEY_COORD_SYS, VALUE_ANY);

		noneInsideCoordSysProp = (Properties)validProp.clone();
		noneInsideCoordSysProp.setProperty(KEY_COORD_SYS, "	ICRS * *, none, FK4 (GEOCENTER|heliocenter) *");

		unknownCoordSysProp = (Properties)validProp.clone();
		unknownCoordSysProp.setProperty(KEY_COORD_SYS, "ICRS foo *");

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

		udfsWithTranslationProp = (Properties)validProp.clone();
		udfsWithTranslationProp.setProperty(KEY_UDFS, "[my_lower(a string)->VARCHAR, \"toLowerCase($1)\"],[my_substring(str string, sub string, startIndex integer)->VARCHAR, \"substr($1, $2, $3)\", \"Extract a substring.\"]");

		udfsWithUDFClassProp = (Properties)validProp.clone();
		udfsWithUDFClassProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}]");

		udfsWithTranslatorClassProp = (Properties)validProp.clone();
		udfsWithTranslatorClassProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR, {tap.config.TestConfigurableServiceConnection$TotoTranslator}]");

		udfsWithClassNameAndDescriptionProp = (Properties)validProp.clone();
		udfsWithClassNameAndDescriptionProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}, \"Bla \\\"bla\\\".\"], [ titi(b REAL) -> double, {adql.db.TestDBChecker$UDFToto}, \"Function titi.\"]");

		udfsWithEmptyOptParamsProp = (Properties)validProp.clone();
		udfsWithEmptyOptParamsProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR,,  	 ]");

		udfsListWithNONEorANYProp = (Properties)validProp.clone();
		udfsListWithNONEorANYProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR],ANY");

		udfsWithWrongDescriptionFormatProp = (Properties)validProp.clone();
		udfsWithWrongDescriptionFormatProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}, Blabla]");

		udfsWithWrongParamLengthProp = (Properties)validProp.clone();
		udfsWithWrongParamLengthProp.setProperty(KEY_UDFS, "[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}, \"Blabla\", foo]");

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

		customFactoryProp = (Properties)validProp.clone();
		customFactoryProp.setProperty(KEY_TAP_FACTORY, "{tap.config.TestConfigurableServiceConnection$CustomTAPFactory}");

		customConfigurableFactoryProp = (Properties)validProp.clone();
		customConfigurableFactoryProp.setProperty(KEY_TAP_FACTORY, "{tap.config.TestConfigurableServiceConnection$CustomConfigurableTAPFactory}");

		badCustomFactoryProp = (Properties)validProp.clone();
		badCustomFactoryProp.setProperty(KEY_TAP_FACTORY, "{tap.config.TestConfigurableServiceConnection$BadCustomTAPFactory}");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DBTools.dropTestDB();
	}

	/**
	 * CONSTRUCTOR TESTS
	 *  * In general:
	 * 		- A valid configuration file builds successfully a fully functional ServiceConnection object.
	 *
	 * 	* Over the file manager:
	 * 		- If no TAPFileManager is provided, an exception must be thrown.
	 * 		- If a class name toward a valid TAPFileManager is provided, a functional DefaultServiceConnection must be successfully built.
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
	public void testDefaultServiceConnectionProperties() {
		// Valid Configuration File:
		PrintWriter writer = null;
		int nbSchemas = -1, nbTables = -1;
		try {
			// build the ServiceConnection:
			ServiceConnection connection = new ConfigurableServiceConnection(validProp);

			// tests:
			assertNotNull(connection.getLogger());
			assertEquals(LogLevel.DEBUG, ((DefaultUWSLog)connection.getLogger()).getMinLogLevel());
			assertNotNull(connection.getFileManager());
			assertEquals("daily at 00:00", ((LocalUWSFileManager)connection.getFileManager()).getLogRotationFreq());
			assertNotNull(connection.getFactory());
			assertNotNull(connection.getTAPMetadata());
			assertTrue(connection.getTAPMetadata().getNbSchemas() >= 1);
			assertTrue(connection.getTAPMetadata().getNbTables() >= 5);
			assertFalse(connection.isAvailable());
			assertEquals(DEFAULT_MAX_ASYNC_JOBS, connection.getNbMaxAsyncJobs());
			assertTrue(connection.getRetentionPeriod()[0] <= connection.getRetentionPeriod()[1]);
			assertTrue(connection.getExecutionDuration()[0] <= connection.getExecutionDuration()[1]);
			assertNull(connection.getUserIdentifier());
			assertNull(connection.getGeometries());
			assertEquals(0, connection.getUDFs().size());
			assertNotNull(connection.getFetchSize());
			assertEquals(2, connection.getFetchSize().length);
			assertEquals(DEFAULT_ASYNC_FETCH_SIZE, connection.getFetchSize()[0]);
			assertEquals(DEFAULT_SYNC_FETCH_SIZE, connection.getFetchSize()[1]);

			// finally, save metadata in an XML file for the other tests:
			writer = new PrintWriter(new File(XML_FILE));
			connection.getTAPMetadata().write(writer);
			nbSchemas = connection.getTAPMetadata().getNbSchemas();
			nbTables = connection.getTAPMetadata().getNbTables();

		} catch(Exception e) {
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		} finally {
			if (writer != null)
				writer.close();
		}

		// Valid XML metadata:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(xmlMetaProp);
			assertNotNull(connection.getLogger());
			assertEquals(LogLevel.DEBUG, ((DefaultUWSLog)connection.getLogger()).getMinLogLevel());
			assertNotNull(connection.getFileManager());
			assertEquals("daily at 00:00", ((LocalUWSFileManager)connection.getFileManager()).getLogRotationFreq());
			assertNotNull(connection.getFactory());
			assertNotNull(connection.getTAPMetadata());
			assertEquals(nbSchemas, connection.getTAPMetadata().getNbSchemas());
			assertEquals(nbTables, connection.getTAPMetadata().getNbTables());
			assertFalse(connection.isAvailable());
			assertEquals(DEFAULT_MAX_ASYNC_JOBS, connection.getNbMaxAsyncJobs());
			assertTrue(connection.getRetentionPeriod()[0] <= connection.getRetentionPeriod()[1]);
			assertTrue(connection.getExecutionDuration()[0] <= connection.getExecutionDuration()[1]);
			assertNull(connection.getUserIdentifier());
			assertNull(connection.getGeometries());
			assertEquals(0, connection.getUDFs().size());
			assertNotNull(connection.getFetchSize());
			assertEquals(2, connection.getFetchSize().length);
			assertEquals(DEFAULT_ASYNC_FETCH_SIZE, connection.getFetchSize()[0]);
			assertEquals(DEFAULT_SYNC_FETCH_SIZE, connection.getFetchSize()[1]);
		} catch(Exception e) {
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Missing metadata property:
		try {
			new ConfigurableServiceConnection(missingMetaProp);
			fail("This MUST have failed because the property 'metadata' is missing!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The property \"" + KEY_METADATA + "\" is missing! It is required to create a TAP Service. Three possible values: " + VALUE_XML + " (to get metadata from a TableSet XML document), " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA) or the name (between {}) of a class extending TAPMetadata. Only " + VALUE_XML + " and " + VALUE_DB + " can be followed by the path of a class extending TAPMetadata.", e.getMessage());
		}

		// Missing metadata_file property:
		try {
			new ConfigurableServiceConnection(missingMetaFileProp);
			fail("This MUST have failed because the property 'metadata_file' is missing!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The property \"" + KEY_METADATA_FILE + "\" is missing! According to the property \"" + KEY_METADATA + "\", metadata must be fetched from an XML document. The local file path of it MUST be provided using the property \"" + KEY_METADATA_FILE + "\".", e.getMessage());
		}

		// Wrong metadata property:
		try {
			new ConfigurableServiceConnection(wrongMetaProp);
			fail("This MUST have failed because the property 'metadata' has a wrong value!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unsupported value for the property \"" + KEY_METADATA + "\": \"foo\"! Only two values are allowed: " + VALUE_XML + " (to get metadata from a TableSet XML document) or " + VALUE_DB + " (to fetch metadata from the database schema TAP_SCHEMA). Only " + VALUE_XML + " and " + VALUE_DB + " can be followed by the path of a class extending TAPMetadata.", e.getMessage());
		}

		// Wrong MANUAL metadata:
		try {
			new ConfigurableServiceConnection(wrongManualMetaProp);
			fail("This MUST have failed because the class specified in the property 'metadata' does not extend TAPMetadata but is TAPMetadata!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong class for the property \"" + KEY_METADATA + "\": \"tap.metadata.TAPMetadata\"! The class provided in this property MUST EXTEND tap.metadata.TAPMetadata.", e.getMessage());
		}

		// XML metadata method WITH a custom TAPMetadata class:
		try {
			ServiceConnection sConn = new ConfigurableServiceConnection(xmlMetaPropWithCustomMetaClass);
			assertEquals(MyCustomTAPMetadata.class, sConn.getTAPMetadata().getClass());
		} catch(Exception e) {
			e.printStackTrace();
			fail("This MUST have succeeded because the property 'metadata' is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// XML metadata method WITH a BAD custom TAPMetadata class:
		try {
			new ConfigurableServiceConnection(xmlMetaPropWithBadCustomMetaClass);
			fail("This MUST have failed because the custom class specified in the property 'metadata' does not have any of the required constructor!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing constructor by copy tap.metadata.TAPMetadata(tap.metadata.TAPMetadata) or tap.metadata.TAPMetadata(tap.metadata.TAPMetadata, uws.service.file.UWSFileManager, tap.TAPFactory, tap.log.TAPLog)! See the value \"xml\" of the property \"" + KEY_METADATA + "\".", e.getMessage());
		}

		// XML metadata method WITH a BAD custom TAPMetadata class:
		try {
			new ConfigurableServiceConnection(xmlMetaPropWithANonMetaClass);
			fail("This MUST have failed because the class specified in the property 'metadata' is not a class name!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unexpected string after the fetching method \"xml\": \"MyCustomTAPMetadata\"! The full name of a class extending TAPMetadata was expected. If it is a class name, then it must be specified between {}.", e.getMessage());
		}

		// Wrong metadata_file property:
		try {
			new ConfigurableServiceConnection(wrongMetaFileProp);
			fail("This MUST have failed because the property 'metadata_file' has a wrong value!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("A grave error occurred while reading/parsing the TableSet XML document: \"foo\"!", e.getMessage());
		}

		// No File Manager:
		try {
			new ConfigurableServiceConnection(noFmProp);
			fail("This MUST have failed because no File Manager is specified!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The property \"" + KEY_FILE_MANAGER + "\" is missing! It is required to create a TAP Service. Two possible values: " + VALUE_LOCAL + " or a class name between {...}.", e.getMessage());
		}

		// File Manager = Class Name:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(fmClassNameProp);
			assertNotNull(connection.getLogger());
			assertEquals(LogLevel.DEBUG, ((DefaultUWSLog)connection.getLogger()).getMinLogLevel());
			assertNotNull(connection.getFileManager());
			assertEquals("daily at 00:00", ((LocalUWSFileManager)connection.getFileManager()).getLogRotationFreq());
			assertNotNull(connection.getFactory());
			assertNotNull(connection.getTAPMetadata());
			assertFalse(connection.isAvailable());

			/* Retention periods and execution durations are different in this configuration file from the valid one (validProp).
			 * Max period and max duration are set in this file as less than respectively the default period and the default duration.
			 * In such situation, the default period/duration is set to the maximum one, in order to ensure that the maximum value is
			 * still greater or equals than the default one. So the max and default values must be equal there.
			 */
			assertTrue(connection.getRetentionPeriod()[0] == connection.getRetentionPeriod()[1]);
			assertTrue(connection.getExecutionDuration()[0] == connection.getExecutionDuration()[1]);
		} catch(Exception e) {
			fail("This MUST have succeeded because the provided file manager is a class name valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Incorrect File Manager Value:
		try {
			new ConfigurableServiceConnection(incorrectFmProp);
			fail("This MUST have failed because an incorrect File Manager value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Class name expected for the property \"file_manager\" instead of: \"foo\"! The specified class must extend/implement uws.service.file.UWSFileManager.", e.getMessage());
		}

		// Custom log level and log rotation:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(correctLogProp);
			assertNotNull(connection.getLogger());
			assertEquals(LogLevel.WARNING, ((DefaultUWSLog)connection.getLogger()).getMinLogLevel());
			assertNotNull(connection.getFileManager());
			assertEquals("monthly on the 5th at 06:03", ((LocalUWSFileManager)connection.getFileManager()).getLogRotationFreq());
		} catch(Exception e) {
			fail("This MUST have succeeded because the provided log level and log rotation are valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Explicit default logger:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(explicitDefaultLoggerProp);
			assertNotNull(connection.getFileManager());
			assertEquals(DefaultTAPLog.class, connection.getLogger().getClass());
		} catch(Exception e) {
			fail("This MUST have succeeded because the value 'default' should be supported and should set the default TAP logger! \nCaught exception: " + getPertinentMessage(e));
		}

		// Custom logger:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(customLoggerProp);
			assertNotNull(connection.getFileManager());
			assertEquals(CustomLogger.class, connection.getLogger().getClass());
		} catch(Exception e) {
			fail("This MUST have succeeded because the specified class implements TAPLog and has a constructor with a single parameter of type UWSFileManager! \nCaught exception: " + getPertinentMessage(e));
		}

		// Incorrect log level:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(incorrectLogLevelProp);
			assertNotNull(connection.getLogger());
			assertEquals(LogLevel.DEBUG, ((DefaultUWSLog)connection.getLogger()).getMinLogLevel());
		} catch(Exception e) {
			fail("This MUST have succeeded because even if the provided log level is incorrect the default behavior is to not throw exception and set the default value! \nCaught exception: " + getPertinentMessage(e));
		}

		// Incorrect log rotation:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(incorrectLogRotationProp);
			assertNotNull(connection.getFileManager());
			assertEquals("daily at 00:00", ((LocalUWSFileManager)connection.getFileManager()).getLogRotationFreq());
		} catch(Exception e) {
			fail("This MUST have succeeded because even if the provided log rotation is incorrect the default behavior is to not throw exception and set the default value! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid output formats list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(validFormatsProp);
			assertNotNull(connection.getOutputFormat(VALUE_VOTABLE));
			assertNotNull(connection.getOutputFormat(VALUE_JSON));
			assertNotNull(connection.getOutputFormat(VALUE_CSV));
			assertNotNull(connection.getOutputFormat(VALUE_TSV));
			assertNotNull(connection.getOutputFormat("psv"));
			assertNotNull(connection.getOutputFormat("text/psv"));
			assertNotNull(connection.getOutputFormat("text"));
			assertNotNull(connection.getOutputFormat("text/plain"));
			assertNotNull(connection.getOutputFormat("test"));
			assertNotNull(connection.getOutputFormat("text/arobase"));
		} catch(Exception e) {
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid VOTable output formats list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(validVOTableFormatsProp);
			Iterator<OutputFormat> it = connection.getOutputFormats();
			OutputFormat f = it.next(); /* votable */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml", f.getMimeType());
			assertEquals(VALUE_VOTABLE, f.getShortMimeType());
			assertEquals(DataFormat.BINARY, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V13, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* votable():: */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml", f.getMimeType());
			assertEquals(VALUE_VOTABLE, f.getShortMimeType());
			assertEquals(DataFormat.BINARY, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V13, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* vot() */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml", f.getMimeType());
			assertEquals(VALUE_VOTABLE, f.getShortMimeType());
			assertEquals(DataFormat.BINARY, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V13, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* vot:: */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml", f.getMimeType());
			assertEquals(VALUE_VOTABLE, f.getShortMimeType());
			assertEquals(DataFormat.BINARY, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V13, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* votable: */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml", f.getMimeType());
			assertEquals(VALUE_VOTABLE, f.getShortMimeType());
			assertEquals(DataFormat.BINARY, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V13, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* votable(Td, 1.0) */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml;serialization=TABLEDATA", f.getMimeType());
			assertEquals("votable/td", f.getShortMimeType());
			assertEquals(DataFormat.TABLEDATA, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V10, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* votable(TableData) */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml;serialization=TABLEDATA", f.getMimeType());
			assertEquals("votable/td", f.getShortMimeType());
			assertEquals(DataFormat.TABLEDATA, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V13, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* votable(, 1.2) */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/x-votable+xml", f.getMimeType());
			assertEquals(VALUE_VOTABLE, f.getShortMimeType());
			assertEquals(DataFormat.BINARY, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V12, ((VOTableFormat)f).getVotVersion());
			f = it.next(); /* vot(fits):application/fits,supervot */
			assertEquals(VOTableFormat.class, f.getClass());
			assertEquals("application/fits", f.getMimeType());
			assertEquals("supervot", f.getShortMimeType());
			assertEquals(DataFormat.FITS, ((VOTableFormat)f).getVotSerialization());
			assertEquals(VOTableVersion.V13, ((VOTableFormat)f).getVotVersion());
			assertFalse(it.hasNext());
		} catch(Exception e) {
			fail("This MUST have succeeded because the property file is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Bad SV(...) format 1 = "sv":
		try {
			new ConfigurableServiceConnection(badSVFormat1Prop);
			fail("This MUST have failed because an incorrect SV output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing separator char/string for the SV output format: \"sv\"!", e.getMessage());
		}

		// Bad SV(...) format 2 = "sv()":
		try {
			new ConfigurableServiceConnection(badSVFormat2Prop);
			fail("This MUST have failed because an incorrect SV output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing separator char/string for the SV output format: \"sv()\"!", e.getMessage());
		}

		// Bad VOTable(...) format 1 = "votable(foo)":
		try {
			new ConfigurableServiceConnection(badVotFormat1Prop);
			fail("This MUST have failed because an incorrect VOTable output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unsupported VOTable serialization: \"foo\"! Accepted values: 'binary' (or 'b'), 'binary2' (or 'b2'), 'tabledata' (or 'td') and 'fits'.", e.getMessage());
		}

		// Bad VOTable(...) format 2 = "votable(,foo)":
		try {
			new ConfigurableServiceConnection(badVotFormat2Prop);
			fail("This MUST have failed because an incorrect VOTable output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unsupported VOTable version: \"foo\"! Accepted values: '1.0' (or 'v1.0'), '1.1' (or 'v1.1'), '1.2' (or 'v1.2') and '1.3' (or 'v1.3').", e.getMessage());
		}

		// Bad VOTable(...) format 3 = "text, vot(TD":
		try {
			new ConfigurableServiceConnection(badVotFormat3Prop);
			fail("This MUST have failed because an incorrect VOTable output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong output format specification syntax in: \"vot(TD\"! A VOTable parameters list must end with ')'.", e.getMessage());
		}

		// Bad VOTable(...) format 4 = "vot(TD, text":
		try {
			new ConfigurableServiceConnection(badVotFormat4Prop);
			fail("This MUST have failed because an incorrect VOTable output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing right parenthesis in: \"vot(TD, text\"!", e.getMessage());
		}

		// Bad VOTable(...) format 5 = "vot(TD, 1.0, foo)":
		try {
			new ConfigurableServiceConnection(badVotFormat5Prop);
			fail("This MUST have failed because an incorrect VOTable output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong number of parameters for the output format VOTable: \"vot(TD, 1.0, foo)\"! Only two parameters may be provided: serialization and version.", e.getMessage());
		}

		// Bad VOTable(...) format 6 = "vot:application/xml:votable:foo":
		try {
			new ConfigurableServiceConnection(badVotFormat6Prop);
			fail("This MUST have failed because an incorrect VOTable output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong output format specification syntax in: \"vot:application/xml:votable:foo\"! After a MIME type and a short MIME type, no more information is expected.", e.getMessage());
		}

		// Unknown output format:
		try {
			new ConfigurableServiceConnection(unknownFormatProp);
			fail("This MUST have failed because an incorrect output format value has been provided!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unknown output format: foo", e.getMessage());
		}

		// Valid value for max_async_jobs:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(maxAsyncProp);
			assertEquals(10, connection.getNbMaxAsyncJobs());
		} catch(Exception e) {
			fail("This MUST have succeeded because a valid max_async_jobs is provided! \nCaught exception: " + getPertinentMessage(e));
		}

		// Negative value for max_async_jobs:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(negativeMaxAsyncProp);
			assertEquals(-2, connection.getNbMaxAsyncJobs());
		} catch(Exception e) {
			fail("This MUST have succeeded because a negative max_async_jobs is equivalent to 'no restriction'! \nCaught exception: " + getPertinentMessage(e));
		}

		// A not integer value for max_async_jobs:
		try {
			new ConfigurableServiceConnection(notIntMaxAsyncProp);
			fail("This MUST have failed because a not integer value has been provided for \"" + KEY_MAX_ASYNC_JOBS + "\"!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Integer expected for the property \"" + KEY_MAX_ASYNC_JOBS + "\", instead of: \"foo\"!", e.getMessage());
		}

		// Test with no output limit specified:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(validProp);
			assertEquals(connection.getOutputLimit()[0], -1);
			assertEquals(connection.getOutputLimit()[1], -1);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		} catch(Exception e) {
			fail("This MUST have succeeded because providing no output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set default output limit:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(defaultOutputLimitProp);
			assertEquals(connection.getOutputLimit()[0], 100);
			assertEquals(connection.getOutputLimit()[1], -1);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		} catch(Exception e) {
			fail("This MUST have succeeded because setting the default output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set maximum output limit:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(maxOutputLimitProp);
			assertEquals(1000, connection.getOutputLimit()[0]);
			assertEquals(1000, connection.getOutputLimit()[1]);
			assertEquals(LimitUnit.rows, connection.getOutputLimitType()[0]);
			assertEquals(LimitUnit.rows, connection.getOutputLimitType()[1]);
		} catch(Exception e) {
			fail("This MUST have succeeded because setting only the maximum output limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum output limits where default <= max:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(bothOutputLimitGoodProp);
			assertEquals(connection.getOutputLimit()[0], 100);
			assertEquals(connection.getOutputLimit()[1], 1000);
			assertEquals(connection.getOutputLimitType()[0], LimitUnit.rows);
			assertEquals(connection.getOutputLimitType()[1], LimitUnit.rows);
		} catch(Exception e) {
			fail("This MUST have succeeded because the default output limit is less or equal the maximum one! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum output limits BUT where default > max:
		/* In a such case, the default value is set silently to the maximum one. */
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(bothOutputLimitBadProp);
			assertEquals(100, connection.getOutputLimit()[1]);
			assertEquals(connection.getOutputLimit()[1], connection.getOutputLimit()[0]);
			assertEquals(LimitUnit.rows, connection.getOutputLimitType()[1]);
			assertEquals(connection.getOutputLimitType()[1], connection.getOutputLimitType()[0]);
		} catch(Exception e) {
			fail("This MUST have succeeded because the default output limit is set automatically to the maximum one if bigger! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with no upload limit specified:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(validProp);
			assertEquals(1000000, connection.getUploadLimit()[1]);
			assertEquals(connection.getUploadLimit()[1], connection.getUploadLimit()[0]);
			assertEquals(LimitUnit.rows, connection.getUploadLimitType()[1]);
			assertEquals(connection.getUploadLimitType()[1], connection.getUploadLimitType()[0]);
		} catch(Exception e) {
			fail("This MUST have succeeded because providing no upload limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set default upload limit:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(defaultUploadLimitProp);
			assertEquals(100, connection.getUploadLimit()[1]);
			assertEquals(connection.getUploadLimit()[1], connection.getUploadLimit()[0]);
			assertEquals(LimitUnit.rows, connection.getUploadLimitType()[1]);
			assertEquals(connection.getUploadLimitType()[1], connection.getUploadLimitType()[0]);
		} catch(Exception e) {
			fail("This MUST have succeeded because setting the default upload limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with only a set maximum upload limit:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(maxUploadLimitProp);
			assertEquals(1000, connection.getUploadLimit()[1]);
			assertEquals(connection.getUploadLimit()[1], connection.getUploadLimit()[0]);
			assertEquals(LimitUnit.rows, connection.getUploadLimitType()[1]);
			assertEquals(connection.getUploadLimitType()[1], connection.getUploadLimitType()[0]);
		} catch(Exception e) {
			fail("This MUST have succeeded because setting only the maximum upload limit is valid! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum upload limits where default <= max:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(bothUploadLimitGoodProp);
			assertEquals(10, connection.getUploadLimit()[1]);
			assertEquals(connection.getUploadLimit()[1], connection.getUploadLimit()[0]);
			assertEquals(LimitUnit.kilobytes, connection.getUploadLimitType()[1]);
			assertEquals(connection.getUploadLimitType()[1], connection.getUploadLimitType()[0]);
		} catch(Exception e) {
			fail("This MUST have succeeded because the default upload limit is less or equal the maximum one! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with both a default and a maximum output limits BUT where default > max:
		/* In a such case, the default value is set silently to the maximum one. */
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(bothUploadLimitBadProp);
			assertEquals(100, connection.getUploadLimit()[1]);
			assertEquals(connection.getUploadLimit()[1], connection.getUploadLimit()[0]);
			assertEquals(LimitUnit.rows, connection.getUploadLimitType()[1]);
			assertEquals(connection.getUploadLimitType()[1], connection.getUploadLimitType()[0]);
		} catch(Exception e) {
			fail("This MUST have succeeded because the default upload limit is set automatically to the maximum one if bigger! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with a not integer sync. fetch size:
		try {
			new ConfigurableServiceConnection(notIntSyncFetchSizeProp);
			fail("This MUST have failed because the set sync. fetch size is not an integer!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Integer expected for the property " + KEY_SYNC_FETCH_SIZE + ": \"foo\"!", e.getMessage());
		}

		// Test with a negative sync. fetch size:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(negativeSyncFetchSizeProp);
			assertNotNull(connection.getFetchSize());
			assertTrue(connection.getFetchSize().length >= 2);
			assertEquals(connection.getFetchSize()[1], 0);
		} catch(Exception e) {
			fail("This MUST have succeeded because a negative fetch size must be set by default to 0 (meaning default JDBC driver value)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with any valid sync. fetch size:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(syncFetchSizeProp);
			assertNotNull(connection.getFetchSize());
			assertTrue(connection.getFetchSize().length >= 2);
			assertEquals(connection.getFetchSize()[1], 50);
		} catch(Exception e) {
			fail("This MUST have succeeded because a valid fetch size has been provided! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with a not integer async. fetch size:
		try {
			new ConfigurableServiceConnection(notIntAsyncFetchSizeProp);
			fail("This MUST have failed because the set async. fetch size is not an integer!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Integer expected for the property " + KEY_ASYNC_FETCH_SIZE + ": \"foo\"!", e.getMessage());
		}

		// Test with a negative async. fetch size:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(negativeAsyncFetchSizeProp);
			assertNotNull(connection.getFetchSize());
			assertTrue(connection.getFetchSize().length >= 1);
			assertEquals(connection.getFetchSize()[0], 0);
		} catch(Exception e) {
			fail("This MUST have succeeded because a negative fetch size must be set by default to 0 (meaning default JDBC driver value)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Test with any valid async. fetch size:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(asyncFetchSizeProp);
			assertNotNull(connection.getFetchSize());
			assertTrue(connection.getFetchSize().length >= 1);
			assertEquals(connection.getFetchSize()[0], 50);
		} catch(Exception e) {
			fail("This MUST have succeeded because a valid fetch size has been provided! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid user identifier:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(userIdentProp);
			assertNotNull(connection.getUserIdentifier());
			assertNotNull(connection.getUserIdentifier().extractUserId(null, null));
			assertEquals("everybody", connection.getUserIdentifier().extractUserId(null, null).getID());
		} catch(Exception e) {
			fail("This MUST have succeeded because the class path toward the fake UserIdentifier is correct! \nCaught exception: " + getPertinentMessage(e));
		}

		// Not a class name for user_identifier:
		try {
			new ConfigurableServiceConnection(notClassPathUserIdentProp);
			fail("This MUST have failed because the user_identifier value is not a class name!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Class name expected for the property \"" + KEY_USER_IDENTIFIER + "\" instead of: \"foo\"! The specified class must extend/implement uws.service.UserIdentifier.", e.getMessage());
		}

		// Valid geometry list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(geometriesProp);
			assertNotNull(connection.getGeometries());
			assertEquals(4, connection.getGeometries().size());
			assertEquals("POINT", ((ArrayList<String>)connection.getGeometries()).get(0));
			assertEquals("CIRCLE", ((ArrayList<String>)connection.getGeometries()).get(1));
			assertEquals("CONTAINS", ((ArrayList<String>)connection.getGeometries()).get(2));
			assertEquals("INTERSECTS", ((ArrayList<String>)connection.getGeometries()).get(3));
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of geometries is correct! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" as geometry list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(noneGeomProp);
			assertNotNull(connection.getGeometries());
			assertEquals(0, connection.getGeometries().size());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of geometries is correct (reduced to only NONE)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "ANY" as geometry list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(anyGeomProp);
			assertNull(connection.getGeometries());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of geometries is correct (reduced to only ANY)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" inside a geometry list:
		try {
			new ConfigurableServiceConnection(noneInsideGeomProp);
			fail("This MUST have failed because the given geometry list contains at least 2 items, whose one is NONE!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The special value \"" + VALUE_NONE + "\" can not be used inside a list! It MUST be used in replacement of a whole list to specify that no value is allowed.", e.getMessage());
		}

		// Unknown geometrical function:
		try {
			new ConfigurableServiceConnection(unknownGeomProp);
			fail("This MUST have failed because the given geometry list contains at least 1 unknown ADQL geometrical function!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Unknown ADQL geometrical function: \"foo\"!", e.getMessage());
		}

		// Valid coordinate systems list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(coordSysProp);
			assertNotNull(connection.getCoordinateSystems());
			assertEquals(2, connection.getCoordinateSystems().size());
			assertEquals("icrs *		*", ((ArrayList<String>)connection.getCoordinateSystems()).get(0));
			assertEquals("ICrs * (Spherical2|  	CARTEsian2)", ((ArrayList<String>)connection.getCoordinateSystems()).get(1));
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of coordinate systems is correct! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" as coordinate systems list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(noneCoordSysProp);
			assertNotNull(connection.getCoordinateSystems());
			assertEquals(0, connection.getCoordinateSystems().size());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of coordinate systems is correct (reduced to only NONE)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "ANY" as coordinate systems list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(anyCoordSysProp);
			assertNull(connection.getCoordinateSystems());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of coordinate systems is correct (reduced to only ANY)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" inside a coordinate systems list:
		try {
			new ConfigurableServiceConnection(noneInsideCoordSysProp);
			fail("This MUST have failed because the given coordinate systems list contains at least 3 items, whose one is NONE!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("The special value \"" + VALUE_NONE + "\" can not be used inside a list! It MUST be used in replacement of a whole list to specify that no value is allowed.", e.getMessage());
		}

		// Unknown coordinate system function:
		try {
			new ConfigurableServiceConnection(unknownCoordSysProp);
			fail("This MUST have failed because the given coordinate systems list contains at least 1 unknown coordinate system!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Incorrect coordinate system regular expression (\"ICRS foo *\"): Wrong allowed coordinate system syntax for the 1-th item: \"ICRS foo *\"! Expected: \"frameRegExp refposRegExp flavorRegExp\" ; where each xxxRegExp = (xxx | '*' | '('xxx ('|' xxx)*')'), frame=\"" + Frame.regexp + "\", refpos=\"" + RefPos.regexp + "\" and flavor=\"" + Flavor.regexp + "\" ; an empty string is also allowed and will be interpreted as '*' (so all possible values).", e.getMessage());
		}

		// "ANY" as UDFs list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(anyUdfsProp);
			assertNull(connection.getUDFs());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs is correct (reduced to only ANY)! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" as UDFs list:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(noneUdfsProp);
			assertNotNull(connection.getUDFs());
			assertEquals(0, connection.getUDFs().size());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs is correct (reduced to only NONE)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(udfsProp);
			assertNotNull(connection.getUDFs());
			assertEquals(2, connection.getUDFs().size());
			Iterator<FunctionDef> it = connection.getUDFs().iterator();
			assertEquals("toto(a VARCHAR)", it.next().toString());
			assertEquals("titi(b REAL) -> DOUBLE", it.next().toString());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs containing UDFs with translation pattern:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(udfsWithTranslationProp);
			assertNotNull(connection.getUDFs());
			assertEquals(2, connection.getUDFs().size());
			Iterator<FunctionDef> it = connection.getUDFs().iterator();
			FunctionDef def = it.next();
			assertEquals("my_lower(a VARCHAR) -> VARCHAR", def.toString());
			assertEquals("toLowerCase($1)", def.getTranslationPattern());
			assertNull(def.description);
			def = it.next();
			assertEquals("my_substring(str VARCHAR, sub VARCHAR, startIndex INTEGER) -> VARCHAR", def.toString());
			assertEquals("substr($1, $2, $3)", def.getTranslationPattern());
			assertEquals("Extract a substring.", def.description);
		} catch(Exception e) {
			e.printStackTrace();
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs containing one UDF with the name of a UserDefinedFunction class:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(udfsWithUDFClassProp);
			assertNotNull(connection.getUDFs());
			assertEquals(1, connection.getUDFs().size());
			FunctionDef def = connection.getUDFs().iterator().next();
			assertEquals("toto(a VARCHAR) -> VARCHAR", def.toString());
			assertEquals(UDFToto.class, def.getUDFClass());
			assertNull(def.getTranslatorClass());
			assertNull(def.getTranslationPattern());
			assertNull(def.description);
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs containing one UDF with the name of a FunctionTranslator class:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(udfsWithTranslatorClassProp);
			assertNotNull(connection.getUDFs());
			assertEquals(1, connection.getUDFs().size());
			FunctionDef def = connection.getUDFs().iterator().next();
			assertEquals("toto(a VARCHAR) -> VARCHAR", def.toString());
			assertEquals(TotoTranslator.class, def.getTranslatorClass());
			assertNull(def.getUDFClass());
			assertNull(def.getTranslationPattern());
			assertNull(def.description);
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs containing one UDF with a class name AND a description:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(udfsWithClassNameAndDescriptionProp);
			assertNotNull(connection.getUDFs());
			assertEquals(2, connection.getUDFs().size());
			Iterator<FunctionDef> itUdfs = connection.getUDFs().iterator();
			FunctionDef def = itUdfs.next();
			assertEquals("toto(a VARCHAR) -> VARCHAR", def.toString());
			assertEquals(UDFToto.class, def.getUDFClass());
			assertEquals("Bla \\\"bla\\\".", def.description);
			def = itUdfs.next();
			assertEquals("titi(b REAL) -> DOUBLE", def.toString());
			assertEquals(UDFToto.class, def.getUDFClass());
			assertEquals("Function titi.", def.description);
		} catch(Exception e) {
			e.printStackTrace();
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid list of UDFs containing one UDF with empty optional parameters:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(udfsWithEmptyOptParamsProp);
			assertNotNull(connection.getUDFs());
			assertEquals(1, connection.getUDFs().size());
			FunctionDef def = connection.getUDFs().iterator().next();
			assertEquals("toto(a VARCHAR) -> VARCHAR", def.toString());
			assertNull(def.getUDFClass());
			assertNull(def.description);
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs contains valid items! \nCaught exception: " + getPertinentMessage(e));
		}

		// "NONE" inside a UDFs list:
		try {
			new ConfigurableServiceConnection(udfsListWithNONEorANYProp);
			fail("This MUST have failed because the given UDFs list contains at least 2 items, whose one is ANY!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: \"ANY\"! (position in the property " + KEY_UDFS + ": 27-30)", e.getMessage());
		}

		// UDF with no brackets:
		try {
			new ConfigurableServiceConnection(udfsWithMissingBracketsProp);
			fail("This MUST have failed because one UDFs list item has no brackets!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: \"toto(a string)->VARCHAR\"! (position in the property " + KEY_UDFS + ": 1-24)", e.getMessage());
		}

		// UDF with a badly formatted description:
		try {
			new ConfigurableServiceConnection(udfsWithWrongDescriptionFormatProp);
			fail("This MUST have failed because one UDFs list item has too many parameters!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: \"[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}, Blabla]\"! (position in the property " + KEY_UDFS + ": 1-67)", e.getMessage());
		}

		// UDFs whose one item have more parts than supported:
		try {
			new ConfigurableServiceConnection(udfsWithWrongParamLengthProp);
			fail("This MUST have failed because one UDFs list item has too many parameters!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: \"[toto(a string)->VARCHAR, {adql.db.TestDBChecker$UDFToto}, \"Blabla\", foo]\"! (position in the property " + KEY_UDFS + ": 1-74)", e.getMessage());
		}

		// UDF with missing definition part (or wrong since there is no comma):
		try {
			new ConfigurableServiceConnection(udfsWithMissingDefProp1);
			fail("This MUST have failed because one UDFs list item has a wrong signature part (it has been forgotten)!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: Wrong function definition syntax! Expected syntax: \"<regular_identifier>(<parameters>?) <return_type>?\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <return_type>=\" -> <type_name>\", <parameters>=\"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"foo()\", \"foo() -> VARCHAR\", \"foo(param INTEGER)\", \"foo(param1 INTEGER, param2 DOUBLE) -> DOUBLE\" (position in the property " + KEY_UDFS + ": 2-33)", e.getMessage());
		}

		// UDF with missing definition part (or wrong since there is no comma):
		try {
			new ConfigurableServiceConnection(udfsWithMissingDefProp2);
			fail("This MUST have failed because one UDFs list item has no signature part!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing UDF declaration! (position in the property " + KEY_UDFS + ": 2-2)", e.getMessage());
		}

		// Empty UDF item (without comma):
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(emptyUdfItemProp1);
			assertNotNull(connection.getUDFs());
			assertEquals(0, connection.getUDFs().size());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs contains one empty UDF (which should be merely ignored)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Empty UDF item (with comma):
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(emptyUdfItemProp2);
			assertNotNull(connection.getUDFs());
			assertEquals(0, connection.getUDFs().size());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given list of UDFs contains one empty UDF (which should be merely ignored)! \nCaught exception: " + getPertinentMessage(e));
		}

		// UDF item without its closing bracket:
		try {
			new ConfigurableServiceConnection(udfWithMissingEndBracketProp);
			fail("This MUST have failed because one UDFs list item has no closing bracket!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Wrong UDF declaration syntax: \"[toto(a string)->VARCHAR\"! (position in the property " + KEY_UDFS + ": 1-25)", e.getMessage());
		}

		// Valid custom TAPFactory:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(customFactoryProp);
			assertNotNull(connection.getFactory());
			assertEquals(CustomTAPFactory.class, connection.getFactory().getClass());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given custom TAPFactory exists and have the required constructor (ServiceConnection)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Valid custom "configurable" TAPFactory:
		try {
			ServiceConnection connection = new ConfigurableServiceConnection(customConfigurableFactoryProp);
			assertNotNull(connection.getFactory());
			assertEquals(CustomConfigurableTAPFactory.class, connection.getFactory().getClass());
		} catch(Exception e) {
			fail("This MUST have succeeded because the given custom configurable TAPFactory exists and have the required constructor (ServiceConnection, Properties)! \nCaught exception: " + getPertinentMessage(e));
		}

		// Bad custom TAPFactory (required constructor missing):
		try {
			new ConfigurableServiceConnection(badCustomFactoryProp);
			fail("This MUST have failed because the specified TAPFactory extension does not have a constructor with ServiceConnection!");
		} catch(Exception e) {
			assertEquals(TAPException.class, e.getClass());
			assertEquals("Missing constructor tap.config.TestConfigurableServiceConnection$BadCustomTAPFactory(tap.ServiceConnection)! See the value \"{tap.config.TestConfigurableServiceConnection$BadCustomTAPFactory}\" of the property \"" + KEY_TAP_FACTORY + "\".", e.getMessage());
		}
	}

	@Test
	public void testGetFile() {
		final String rootPath = "/ROOT", propertyName = "SuperProperty";
		String path;

		try {
			// NULL test => NULL must be returned.
			assertNull(ConfigurableServiceConnection.getFile(null, rootPath, propertyName));

			// Valid absolute file path:
			path = "/custom/user/dir";
			assertEquals(path, ConfigurableServiceConnection.getFile(path, rootPath, propertyName).getAbsolutePath());

			// File name relative to the given rootPath:
			path = "dir";
			assertEquals(rootPath + File.separator + path, ConfigurableServiceConnection.getFile(path, rootPath, propertyName).getAbsolutePath());

			// Idem but with a relative file path:
			path = "gmantele/workspace";
			assertEquals(rootPath + File.separator + path, ConfigurableServiceConnection.getFile(path, rootPath, propertyName).getAbsolutePath());

		} catch(Exception ex) {
			ex.printStackTrace();
			fail("None of these tests should have failed!");
		}

		// Test with a file URI:
		path = "file:/custom/user/dir";
		try {
			ConfigurableServiceConnection.getFile(path, rootPath, propertyName);
			fail("This test should have failed, because URIs are no longer supported!");
		} catch(Exception ex) {
			assertEquals(TAPException.class, ex.getClass());
			assertEquals("Incorrect file path for the property \"" + propertyName + "\": \"" + path + "\"! URI/URLs are not expected here.", ex.getMessage());
		}

		// Test with an URL:
		path = "http://www.google.com";
		try {
			ConfigurableServiceConnection.getFile(path, rootPath, propertyName);
			fail("This test should have failed, because the given URI uses the HTTP protocol (actually, it uses a protocol different from \"file\"!");
		} catch(Exception ex) {
			assertEquals(TAPException.class, ex.getClass());
			assertEquals("Incorrect file path for the property \"" + propertyName + "\": \"" + path + "\"! URI/URLs are not expected here.", ex.getMessage());
		}

	}

	public static final String getPertinentMessage(final Exception ex) {
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
		public FileManagerTest(Properties tapConfig) throws UWSException {
			super(new File(tapConfig.getProperty("file_root_path")), true, false);
		}
	}

	/**
	 * A {@link FunctionTranslator} which always return the same translation:
	 * <code>toto()</code>.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 05/2021
	 */
	public static class TotoTranslator implements FunctionTranslator {

		@Override
		public String translate(final ADQLFunction fct, final ADQLTranslator caller) throws TranslationException {
			return "toto()";
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
		public JobOwner extractUserId(UWSUrl urlInterpreter, HttpServletRequest request) throws UWSException {
			return everybody;
		}

		@Override
		public JobOwner restoreUser(String id, String pseudo, Map<String, Object> otherData) throws UWSException {
			return everybody;
		}

	}

	/**
	 * TAPFactory just to test whether the property tap_factory works well.
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 03/2017
	 */
	private static class CustomTAPFactory extends AbstractTAPFactory {

		private final JDBCConnection dbConn;

		public CustomTAPFactory(final ServiceConnection conn) throws DBException {
			super(conn);
			dbConn = new JDBCConnection(DBTools.DB_TEST_JDBC_DRIVER, DBTools.DB_TEST_URL, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD, new AstroH2Translator(), "TheOnlyConnection", conn.getLogger());
		}

		@Override
		public DBConnection getConnection(final String jobID) throws TAPException {
			return dbConn;
		}

		@Override
		public void freeConnection(final DBConnection conn) {
		}

		@Override
		public void destroy() {
			try {
				dbConn.getInnerConnection().close();
			} catch(Exception ex) {
			}
		}

	}

	/**
	 * ConfigurableTAPFactory just to test whether the property tap_factory allows TAPFactory
	 * with a constructor (ServiceConnection, Properties).
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 03/2017
	 */
	private static class CustomConfigurableTAPFactory extends AbstractTAPFactory {

		private final JDBCConnection dbConn;

		public CustomConfigurableTAPFactory(final ServiceConnection conn, final Properties prop) throws DBException {
			super(conn);
			dbConn = new JDBCConnection(DBTools.DB_TEST_JDBC_DRIVER, DBTools.DB_TEST_URL, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD, new AstroH2Translator(), "TheOnlyConnection", conn.getLogger());
		}

		@Override
		public DBConnection getConnection(final String jobID) throws TAPException {
			return dbConn;
		}

		@Override
		public void freeConnection(final DBConnection conn) {
		}

		@Override
		public void destroy() {
			try {
				dbConn.getInnerConnection().close();
			} catch(Exception ex) {
			}
		}

	}

	/**
	 * TAPFactory just to test whether the property tap_factory is rejected when no constructor with a single parameter of type ServiceConnection exists.
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 02/2015
	 */
	private static class BadCustomTAPFactory extends AbstractTAPFactory {

		public BadCustomTAPFactory() throws DBException {
			super(null);
		}

		@Override
		public DBConnection getConnection(final String jobID) throws TAPException {
			return null;
		}

		@Override
		public void freeConnection(final DBConnection conn) {
		}

		@Override
		public void destroy() {
		}

	}

	/**
	 * TAPMetadata extension just to test whether it is possible to customize the output class of ConfigurableServiceConnection with the
	 * metadata fetching methods "db" and "xml".
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 08/2015
	 */
	private static class MyCustomTAPMetadata extends TAPMetadata {
		public MyCustomTAPMetadata(TAPMetadata meta) {
			for(TAPSchema s : meta) {
				this.addSchema(s);
			}
		}
	}

	/**
	 * TAPMetadata extension just to test whether it is possible to customize the output class of ConfigurableServiceConnection with the
	 * metadata fetching methods "db" and "xml".
	 *
	 * <strong>This extension is however bad because it does not have any of the required constructor.</strong>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 08/2015
	 */
	private static class MyBadCustomTAPMetadata extends TAPMetadata {
		public MyBadCustomTAPMetadata() {

		}
	}

	/**
	 * Custom TAPLog implementation.
	 *
	 * <p><i>
	 * 	Actually, for quick implementation, this class just extends
	 * 	{@link DefaultTAPLog} (and so, implements TAPLog).
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 09/2017
	 */
	private static class CustomLogger extends DefaultTAPLog {
		public CustomLogger(final UWSFileManager fm) {
			super(fm);
		}

		@Override
		public void logTAP(LogLevel level, Object obj, String event, String message, Throwable error) {
			super.logTAP(level, obj, event, "[MY] " + message, error);
		}

	}

}
