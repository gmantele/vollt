package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static tap.config.TAPConfiguration.KEY_DEFAULT_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_FILE_MANAGER;
import static tap.config.TAPConfiguration.KEY_MAX_OUTPUT_LIMIT;
import static tap.config.TAPConfiguration.KEY_TAP_FACTORY;
import static tap.config.TAPConfiguration.fetchClass;
import static tap.config.TAPConfiguration.hasConstructor;
import static tap.config.TAPConfiguration.isClassName;
import static tap.config.TAPConfiguration.newInstance;
import static tap.config.TAPConfiguration.parseLimit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.TAPFactory;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPSchema;
import adql.query.ColumnReference;

public class TestTAPConfiguration {

	@Before
	public void setUp() throws Exception{}

	/**
	 * TEST isClassName(String):
	 * 	- null, "", "{}", "an incorrect syntax" 				=> FALSE must be returned
	 * 	- "{ }", "{ 	}", "{class.path}", "{ class.path	}" 	=> TRUE must be returned
	 * 
	 * @see ConfigurableServiceConnection#isClassName(String)
	 */
	@Test
	public void testIsClassPath(){
		// NULL and EMPTY:
		assertFalse(isClassName(null));
		assertFalse(isClassName(""));

		// EMPTY CLASSPATH:
		assertFalse(isClassName("{}"));

		// INCORRECT CLASSPATH:
		assertFalse(isClassName("incorrect class name ; missing {}"));

		// VALID CLASSPATH:
		assertTrue(isClassName("{class.path}"));

		// CLASSPATH VALID ONLY IN THE SYNTAX:
		assertTrue(isClassName("{ }"));
		assertTrue(isClassName("{		}"));

		// NOT TRIM CLASSPATH:
		assertTrue(isClassName("{ class.name	}"));
	}

	/**
	 * TEST getClass(String,String,Class):
	 * 	- null, "", "{}", "an incorrect syntax", "{ }", "{ 	}" 						=> NULL must be returned
	 * 	- "{java.lang.String}", "{ java.lang.String	}"								=> a valid DefaultServiceConnection must be returned
	 * 	- "{mypackage.foo}", "{java.util.ArrayList}" (while a String is expected)	=> a TAPException must be thrown
	 */
	@Test
	public void testGetClassStringStringClass(){
		// NULL and EMPTY:
		try{
			assertNull(fetchClass(null, KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If a NULL value is provided as class name: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}
		try{
			assertNull(fetchClass("", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY value is provided as class name: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// EMPTY CLASS NAME:
		try{
			assertNull(fetchClass("{}", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// INCORRECT SYNTAX:
		try{
			assertNull(fetchClass("incorrect class name ; missing {}", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an incorrect class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// VALID CLASS NAME:
		try{
			Class<? extends String> classObject = fetchClass("{java.lang.String}", KEY_FILE_MANAGER, String.class);
			assertNotNull(classObject);
			assertEquals(classObject.getName(), "java.lang.String");
		}catch(TAPException e){
			fail("If a VALID class name is provided: getClass(...) MUST return a Class object of the wanted type!\nCaught exception: " + getPertinentMessage(e));
		}

		// INCORRECT CLASS NAME:
		try{
			fetchClass("{mypackage.foo}", KEY_FILE_MANAGER, String.class);
			fail("This MUST have failed because an incorrect class name is provided!");
		}catch(TAPException e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "The class specified by the property \"" + KEY_FILE_MANAGER + "\" ({mypackage.foo}) can not be found.");
		}

		// INCOMPATIBLE TYPES:
		try{
			@SuppressWarnings("unused")
			Class<? extends String> classObject = fetchClass("{java.util.ArrayList}", KEY_FILE_MANAGER, String.class);
			fail("This MUST have failed because a class of a different type has been asked!");
		}catch(TAPException e){
			assertEquals(e.getClass(), TAPException.class);
			assertEquals(e.getMessage(), "The class specified by the property \"" + KEY_FILE_MANAGER + "\" ({java.util.ArrayList}) is not implementing " + String.class.getName() + ".");
		}

		// CLASS NAME VALID ONLY IN THE SYNTAX:
		try{
			assertNull(fetchClass("{ }", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}
		try{
			assertNull(fetchClass("{		}", KEY_FILE_MANAGER, String.class));
		}catch(TAPException e){
			fail("If an EMPTY class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// NOT TRIM CLASS NAME:
		try{
			Class<?> classObject = fetchClass("{ java.lang.String	}", KEY_FILE_MANAGER, String.class);
			assertNotNull(classObject);
			assertEquals(classObject.getName(), "java.lang.String");
		}catch(TAPException e){
			fail("If a VALID class name is provided: getClass(...) MUST return a Class object of the wanted type!\nCaught exception: " + getPertinentMessage(e));
		}
	}

	/**
	 * TEST hasConstructor(String,String,Class,Class[]):
	 * 	(tests already performed by {@link #testGetClassStringStringClass()})
	 * 	- null, "", "{}", "an incorrect syntax", "{ }", "{ 	}" 						=> must fail with a TAPException
	 * 	- "{java.lang.String}", "{ java.lang.String	}"								=> a valid DefaultServiceConnection must be returned
	 * 	- "{mypackage.foo}", "{java.util.ArrayList}" (while a String is expected)	=> a TAPException must be thrown
	 * 	(new tests)
	 * 	- if the specified constructor exists return <code>true</code>, else <code>false</code> must be returned.
	 */
	@Test
	public void testHasConstructor(){
		/* hasConstructor(...) must throw an exception if the specification of the class (1st and 3rd parameters)
		 * is wrong. But that is performed by fetchClass(...) which is called at the beginning of the function
		 * and is not surrounded by a try-catch. So all these tests are already done by testGetClassStringStringClass(). */

		// With a missing list of parameters:
		try{
			assertTrue(hasConstructor("{java.lang.String}", "STRING", String.class, null));
		}catch(TAPException te){
			te.printStackTrace();
			fail("\"No list of parameters\" MUST be interpreted as the specification of a constructor with no parameter! This test has failed.");
		}

		// With an empty list of parameters
		try{
			assertTrue(hasConstructor("{java.lang.String}", "STRING", String.class, new Class[0]));
		}catch(TAPException te){
			te.printStackTrace();
			fail("\"An empty list of parameters\" MUST be interpreted as the specification of a constructor with no parameter! This test has failed.");
		}

		// With a wrong list of parameters - 1
		try{
			assertFalse(hasConstructor("{tap.config.ConfigurableTAPFactory}", KEY_TAP_FACTORY, TAPFactory.class, new Class[]{}));
		}catch(TAPException te){
			te.printStackTrace();
			fail("ConfigurableTAPFactory does not have an empty constructor ; this test should have failed!");
		}

		// With a wrong list of parameters - 2
		try{
			assertFalse(hasConstructor("{tap.config.ConfigurableTAPFactory}", KEY_TAP_FACTORY, TAPFactory.class, new Class[]{String.class,String.class}));
		}catch(TAPException te){
			te.printStackTrace();
			fail("ConfigurableTAPFactory does not have a constructor with 2 Strings as parameter ; this test should have failed!");
		}

		// With a good list of parameters - 1
		try{
			assertTrue(hasConstructor("{tap.config.ConfigurableTAPFactory}", KEY_TAP_FACTORY, TAPFactory.class, new Class[]{ServiceConnection.class,Properties.class}));
		}catch(TAPException te){
			te.printStackTrace();
			fail("ConfigurableTAPFactory has a constructor with a ServiceConnection and a Properties in parameters ; this test should have failed!");
		}

		// With a good list of parameters - 2
		try{
			assertTrue(hasConstructor("{java.lang.String}", "STRING", String.class, new Class[]{String.class}));
		}catch(TAPException te){
			te.printStackTrace();
			fail("String has a constructor with a String as parameter ; this test should have failed!");
		}
	}

	@Test
	public void testNewInstance(){
		// VALID CONSTRUCTOR with no parameters:
		try{
			TAPMetadata metadata = newInstance("{tap.metadata.TAPMetadata}", "metadata", TAPMetadata.class);
			assertNotNull(metadata);
			assertEquals("tap.metadata.TAPMetadata", metadata.getClass().getName());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded: the parameters of newInstance(...) are all valid.");
		}

		// VALID CONSTRUCTOR with some parameters:
		try{
			final String schemaName = "MySuperSchema", description = "And its less super description.", utype = "UTYPE";
			TAPSchema schema = newInstance("{tap.metadata.TAPSchema}", "schema", TAPSchema.class, new Class<?>[]{String.class,String.class,String.class}, new String[]{schemaName,description,utype});
			assertNotNull(schema);
			assertEquals("tap.metadata.TAPSchema", schema.getClass().getName());
			assertEquals(schemaName, schema.getADQLName());
			assertEquals(description, schema.getDescription());
			assertEquals(utype, schema.getUtype());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded: the constructor TAPSchema(String,String,String) exists.");
		}

		// VALID CONSTRUCTOR with some parameters whose the type is an extension (not the exact type):
		OutputStream output = null;
		File tmp = new File("tmp.empty");
		try{
			output = newInstance("{java.io.BufferedOutputStream}", "stream", OutputStream.class, new Class<?>[]{OutputStream.class}, new OutputStream[]{new FileOutputStream(tmp)});
			assertNotNull(output);
			assertEquals(BufferedOutputStream.class, output.getClass());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded: the constructor TAPSchema(String,String,String) exists.");
		}finally{
			try{
				tmp.delete();
				if (output != null)
					output.close();
			}catch(IOException ioe){}
		}

		// NOT A CLASS NAME:
		try{
			newInstance("tap.metadata.TAPMetadata", "metadata", TAPMetadata.class);
			fail("This MUST have failed because the property value is not a class name!");
		}catch(Exception ex){
			assertEquals(TAPException.class, ex.getClass());
			assertEquals("Class name expected for the property \"metadata\" instead of: \"tap.metadata.TAPMetadata\"! The specified class must extend/implement tap.metadata.TAPMetadata.", ex.getMessage());
		}

		// NO MATCHING CONSTRUCTOR:
		try{
			newInstance("{tap.metadata.TAPSchema}", "schema", TAPSchema.class, new Class<?>[]{Integer.class}, new Object[]{new Integer(123)});
			fail("This MUST have failed because the specified class does not have any expected constructor!");
		}catch(Exception ex){
			assertEquals(TAPException.class, ex.getClass());
			assertEquals("Missing constructor tap.metadata.TAPSchema(java.lang.Integer)! See the value \"{tap.metadata.TAPSchema}\" of the property \"schema\".", ex.getMessage());
		}

		// VALID CONSTRUCTOR with primitive type:
		try{
			ColumnReference colRef = newInstance("{adql.query.ColumnReference}", "colRef", ColumnReference.class, new Class<?>[]{int.class}, new Object[]{123});
			assertNotNull(colRef);
			assertEquals(ColumnReference.class, colRef.getClass());
			assertEquals(123, colRef.getColumnIndex());
			colRef = newInstance("{adql.query.ColumnReference}", "colRef", ColumnReference.class, new Class<?>[]{int.class}, new Object[]{new Integer(123)});
			assertNotNull(colRef);
			assertEquals(ColumnReference.class, colRef.getClass());
			assertEquals(123, colRef.getColumnIndex());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded: the constructor ColumnReference(int) exists.");
		}

		// WRONG CONSTRUCTOR with primitive type:
		try{
			newInstance("{adql.query.ColumnReference}", "colRef", ColumnReference.class, new Class<?>[]{Integer.class}, new Object[]{new Integer(123)});
			fail("This MUST have failed because the constructor of the specified class expects an int, not an java.lang.Integer!");
		}catch(Exception ex){
			assertEquals(TAPException.class, ex.getClass());
			assertEquals("Missing constructor adql.query.ColumnReference(java.lang.Integer)! See the value \"{adql.query.ColumnReference}\" of the property \"colRef\".", ex.getMessage());
		}

		// THE CONSTRUCTOR THROWS AN EXCEPTION:
		try{
			newInstance("{tap.metadata.TAPSchema}", "schema", TAPSchema.class, new Class<?>[]{String.class}, new Object[]{null});
			fail("This MUST have failed because the constructor of the specified class throws an exception!");
		}catch(Exception ex){
			assertEquals(TAPException.class, ex.getClass());
			assertNotNull(ex.getCause());
			assertEquals(NullPointerException.class, ex.getCause().getClass());
			assertEquals("Missing schema name!", ex.getCause().getMessage());
		}

		// THE CONSTRUCTOR THROWS A TAPEXCEPTION:
		try{
			newInstance("{tap.config.TestTAPConfiguration$ClassAlwaysThrowTAPError}", "tapError", ClassAlwaysThrowTAPError.class);
			fail("This MUST have failed because the constructor of the specified class throws a TAPException!");
		}catch(Exception ex){
			assertEquals(TAPException.class, ex.getClass());
			assertEquals("This error is always thrown by ClassAlwaysThrowTAPError ^^", ex.getMessage());
		}
	}

	/**
	 * TEST parseLimit(String,String):
	 * 	- nothing, -123, 0			=> {-1,LimitUnit.rows}
	 * 	- 20, 20r, 20R				=> {20,LimitUnit.rows}
	 * 	- 100B, 100 B				=> {100,LimitUnit.bytes}
	 * 	- 100kB, 100 k B			=> {100000,LimitUnit.bytes}
	 * 	- 100MB, 1 0 0MB			=> {100000000,LimitUnit.bytes}
	 * 	- 100GB, 1 0 0 G B			=> {100000000000,LimitUnit.bytes}
	 * 	- r							=> {-1,LimitUnit.rows}
	 * 	- kB						=> {-1,LimitUnit.bytes}
	 * 	- foo, 100b, 100TB, 1foo	=> an exception must occur
	 */
	@Test
	public void testParseLimitStringString(){
		final String propertyName = KEY_DEFAULT_OUTPUT_LIMIT + " or " + KEY_MAX_OUTPUT_LIMIT;
		// Test empty or negative or null values => OK!
		try{
			String[] testValues = new String[]{null,"","  	 ","-123"};
			Object[] limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName, false);
				assertEquals(limit[0], -1);
				assertEquals(limit[1], LimitUnit.rows);
			}
			// 0 test:
			limit = parseLimit("0", propertyName, false);
			assertEquals(limit[0], 0);
			assertEquals(limit[1], LimitUnit.rows);
		}catch(TAPException te){
			fail("All these empty limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted rows values:
		try{
			String[] testValues = new String[]{"20","20r","20 R"};
			Object[] limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName, false);
				assertEquals(limit[0], 20);
				assertEquals(limit[1], LimitUnit.rows);
			}
		}catch(TAPException te){
			fail("All these rows limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted bytes values:
		try{
			String[] testValues = new String[]{"100B","100 B"};
			Object[] limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName, true);
				assertEquals(limit[0], 100);
				assertEquals(limit[1], LimitUnit.bytes);
			}
		}catch(TAPException te){
			fail("All these bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted kilo-bytes values:
		try{
			String[] testValues = new String[]{"100kB","100 k B"};
			Object[] limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName, true);
				assertEquals(limit[0], 100);
				assertEquals(limit[1], LimitUnit.kilobytes);
			}
		}catch(TAPException te){
			fail("All these kilo-bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted mega-bytes values:
		try{
			String[] testValues = new String[]{"100MB","1 0 0MB"};
			Object[] limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName, true);
				assertEquals(limit[0], 100);
				assertEquals(limit[1], LimitUnit.megabytes);
			}
		}catch(TAPException te){
			fail("All these mega-bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted giga-bytes values:
		try{
			String[] testValues = new String[]{"100GB","1 0 0 G B"};
			Object[] limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName, true);
				assertEquals(limit[0], 100);
				assertEquals(limit[1], LimitUnit.gigabytes);
			}
		}catch(TAPException te){
			fail("All these giga-bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test with only the ROWS unit provided:
		try{
			Object[] limit = parseLimit("r", propertyName, false);
			assertEquals(limit[0], -1);
			assertEquals(limit[1], LimitUnit.rows);
		}catch(TAPException te){
			fail("Providing only the ROWS unit is valid, so this test should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test with only the BYTES unit provided:
		try{
			Object[] limit = parseLimit("kB", propertyName, true);
			assertEquals(limit[0], -1);
			assertEquals(limit[1], LimitUnit.kilobytes);
		}catch(TAPException te){
			fail("Providing only the BYTES unit is valid, so this test should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test with incorrect limit formats:
		String[] values = new String[]{"","100","100","1"};
		String[] unitPart = new String[]{"foo","b","TB","foo"};
		for(int i = 0; i < values.length; i++){
			try{
				parseLimit(values[i] + unitPart[i], propertyName, true);
				fail("This test should have failed because an incorrect limit is provided: \"" + values[i] + unitPart[i] + "\"!");
			}catch(TAPException te){
				assertEquals(te.getClass(), TAPException.class);
				assertEquals(te.getMessage(), "Unknown limit unit (" + unitPart[i] + ") for the property " + propertyName + ": \"" + values[i] + unitPart[i] + "\"!");

			}
		}
		// Test with an incorrect numeric limit value:
		try{
			parseLimit("abc100b", propertyName, true);
			fail("This test should have failed because an incorrect limit is provided: \"abc100b\"!");
		}catch(TAPException te){
			assertEquals(te.getClass(), TAPException.class);
			assertEquals(te.getMessage(), "Integer expected for the property " + propertyName + " for the substring \"abc100\" of the whole value: \"abc100b\"!");
		}

		// Test with a BYTES unit whereas the BYTES unit is forbidden:
		try{
			parseLimit("100B", propertyName, false);
			fail("This test should have failed because an incorrect limit is provided: \"100B\"!");
		}catch(TAPException te){
			assertEquals(te.getClass(), TAPException.class);
			assertEquals(te.getMessage(), "BYTES unit is not allowed for the property " + propertyName + " (100B)!");
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

	private static class ClassAlwaysThrowTAPError {
		@SuppressWarnings("unused")
		public ClassAlwaysThrowTAPError() throws TAPException{
			throw new TAPException("This error is always thrown by ClassAlwaysThrowTAPError ^^");
		}
	}

}
