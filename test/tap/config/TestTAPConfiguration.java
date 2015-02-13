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
import static tap.config.TAPConfiguration.fetchClass;
import static tap.config.TAPConfiguration.isClassPath;
import static tap.config.TAPConfiguration.parseLimit;

import org.junit.Before;
import org.junit.Test;

import tap.ServiceConnection.LimitUnit;
import tap.TAPException;

public class TestTAPConfiguration {

	@Before
	public void setUp() throws Exception{}

	/**
	 * TEST isClassPath(String):
	 * 	- null, "", "{}", "an incorrect syntax" 				=> FALSE must be returned
	 * 	- "{ }", "{ 	}", "{class.path}", "{ class.path	}" 	=> TRUE must be returned
	 * 
	 * @see ConfigurableServiceConnection#isClassPath(String)
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
			Class<? extends String> classObject = fetchClass("{java.lang.String}", KEY_FILE_MANAGER, String.class);
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
			Class<? extends String> classObject = fetchClass("{java.util.ArrayList}", KEY_FILE_MANAGER, String.class);
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
			String[] testValues = new String[]{null,"","  	 ","0","-123"};
			Object[] limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName, false);
				assertEquals(limit[0], -1);
				assertEquals(limit[1], LimitUnit.rows);
			}
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

}
