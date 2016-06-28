package uws.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uws.config.UWSConfiguration.KEY_FILE_MANAGER;
import static uws.config.UWSConfiguration.KEY_UWS_FACTORY;
import static uws.config.UWSConfiguration.fetchClass;
import static uws.config.UWSConfiguration.hasConstructor;
import static uws.config.UWSConfiguration.isClassName;
import static uws.config.UWSConfiguration.newInstance;
import static uws.config.UWSConfiguration.parseLimit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import uws.ISO8601Format;
import uws.UWSException;
import uws.job.ErrorType;
import uws.service.UWSFactory;

public class TestUWSConfiguration {

	@Before
	public void setUp() throws Exception{}

	/**
	 * TEST isClassName(String):
	 * 	- null, "", "{}", "an incorrect syntax" 				=> FALSE must be returned
	 * 	- "{ }", "{ 	}", "{class.path}", "{ class.path	}" 	=> TRUE must be returned
	 * 
	 * @see ConfigurableUWSServlet#isClassName(String)
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
	 * 	- "{mypackage.foo}", "{java.util.ArrayList}" (while a String is expected)	=> a UWSException must be thrown
	 */
	@Test
	public void testGetClassStringStringClass(){
		// NULL and EMPTY:
		try{
			assertNull(fetchClass(null, KEY_FILE_MANAGER, String.class));
		}catch(UWSException e){
			fail("If a NULL value is provided as class name: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}
		try{
			assertNull(fetchClass("", KEY_FILE_MANAGER, String.class));
		}catch(UWSException e){
			fail("If an EMPTY value is provided as class name: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// EMPTY CLASS NAME:
		try{
			assertNull(fetchClass("{}", KEY_FILE_MANAGER, String.class));
		}catch(UWSException e){
			fail("If an EMPTY class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// INCORRECT SYNTAX:
		try{
			assertNull(fetchClass("incorrect class name ; missing {}", KEY_FILE_MANAGER, String.class));
		}catch(UWSException e){
			fail("If an incorrect class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// VALID CLASS NAME:
		try{
			Class<? extends String> classObject = fetchClass("{java.lang.String}", KEY_FILE_MANAGER, String.class);
			assertNotNull(classObject);
			assertEquals(classObject.getName(), "java.lang.String");
		}catch(UWSException e){
			fail("If a VALID class name is provided: getClass(...) MUST return a Class object of the wanted type!\nCaught exception: " + getPertinentMessage(e));
		}

		// INCORRECT CLASS NAME:
		try{
			fetchClass("{mypackage.foo}", KEY_FILE_MANAGER, String.class);
			fail("This MUST have failed because an incorrect class name is provided!");
		}catch(UWSException e){
			assertEquals(e.getClass(), UWSException.class);
			assertEquals(e.getMessage(), "The class specified by the property \"" + KEY_FILE_MANAGER + "\" ({mypackage.foo}) can not be found.");
		}

		// INCOMPATIBLE TYPES:
		try{
			@SuppressWarnings("unused")
			Class<? extends String> classObject = fetchClass("{java.util.ArrayList}", KEY_FILE_MANAGER, String.class);
			fail("This MUST have failed because a class of a different type has been asked!");
		}catch(UWSException e){
			assertEquals(e.getClass(), UWSException.class);
			assertEquals(e.getMessage(), "The class specified by the property \"" + KEY_FILE_MANAGER + "\" ({java.util.ArrayList}) is not implementing " + String.class.getName() + ".");
		}

		// CLASS NAME VALID ONLY IN THE SYNTAX:
		try{
			assertNull(fetchClass("{ }", KEY_FILE_MANAGER, String.class));
		}catch(UWSException e){
			fail("If an EMPTY class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}
		try{
			assertNull(fetchClass("{		}", KEY_FILE_MANAGER, String.class));
		}catch(UWSException e){
			fail("If an EMPTY class name is provided: getClass(...) MUST return null!\nCaught exception: " + getPertinentMessage(e));
		}

		// NOT TRIM CLASS NAME:
		try{
			Class<?> classObject = fetchClass("{ java.lang.String	}", KEY_FILE_MANAGER, String.class);
			assertNotNull(classObject);
			assertEquals(classObject.getName(), "java.lang.String");
		}catch(UWSException e){
			fail("If a VALID class name is provided: getClass(...) MUST return a Class object of the wanted type!\nCaught exception: " + getPertinentMessage(e));
		}
	}

	/**
	 * TEST hasConstructor(String,String,Class,Class[]):
	 * 	(tests already performed by {@link #testGetClassStringStringClass()})
	 * 	- null, "", "{}", "an incorrect syntax", "{ }", "{ 	}" 						=> must fail with a UWSException
	 * 	- "{java.lang.String}", "{ java.lang.String	}"								=> a valid DefaultServiceConnection must be returned
	 * 	- "{mypackage.foo}", "{java.util.ArrayList}" (while a String is expected)	=> a UWSException must be thrown
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
		}catch(UWSException te){
			te.printStackTrace();
			fail("\"No list of parameters\" MUST be interpreted as the specification of a constructor with no parameter! This test has failed.");
		}

		// With an empty list of parameters
		try{
			assertTrue(hasConstructor("{java.lang.String}", "STRING", String.class, new Class[0]));
		}catch(UWSException te){
			te.printStackTrace();
			fail("\"An empty list of parameters\" MUST be interpreted as the specification of a constructor with no parameter! This test has failed.");
		}

		// With a wrong list of parameters - 1
		try{
			assertFalse(hasConstructor("{uws.config.ConfigurableUWSFactory}", KEY_UWS_FACTORY, UWSFactory.class, new Class[]{}));
		}catch(UWSException te){
			te.printStackTrace();
			fail("ConfigurableUWSFactory does not have an empty constructor ; this test should have failed!");
		}

		// With a wrong list of parameters - 2
		try{
			assertFalse(hasConstructor("{uws.config.ConfigurableUWSFactory}", KEY_UWS_FACTORY, UWSFactory.class, new Class[]{String.class,String.class}));
		}catch(UWSException te){
			te.printStackTrace();
			fail("ConfigurableUWSFactory does not have a constructor with 2 Strings as parameter ; this test should have failed!");
		}

		// With a good list of parameters - 1
		try{
			assertTrue(hasConstructor("{uws.config.ConfigurableUWSFactory}", KEY_UWS_FACTORY, UWSFactory.class, new Class[]{Properties.class}));
		}catch(UWSException te){
			te.printStackTrace();
			fail("ConfigurableUWSFactory has a constructor with a Properties in parameter ; this test should have failed!");
		}

		// With a good list of parameters - 2
		try{
			assertTrue(hasConstructor("{java.lang.String}", "STRING", String.class, new Class[]{String.class}));
		}catch(UWSException te){
			te.printStackTrace();
			fail("String has a constructor with a String as parameter ; this test should have failed!");
		}
	}

	@Test
	public void testNewInstance(){
		// VALID CONSTRUCTOR with no parameters:
		try{
			ISO8601Format dateFormat = newInstance("{uws.ISO8601Format}", "dateFormat", ISO8601Format.class);
			assertNotNull(dateFormat);
			assertEquals("uws.ISO8601Format", dateFormat.getClass().getName());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded: the parameters of newInstance(...) are all valid.");
		}

		// VALID CONSTRUCTOR with some parameters:
		try{
			final int errorCode = 503;
			final String message = "My super test exception.";
			final ErrorType type = ErrorType.TRANSIENT;
			UWSException exception = newInstance("{uws.UWSException}", "exception", UWSException.class, new Class<?>[]{int.class,String.class,ErrorType.class}, new Object[]{errorCode,message,type});
			assertNotNull(exception);
			assertEquals("uws.UWSException", exception.getClass().getName());
			assertEquals(errorCode, exception.getHttpErrorCode());
			assertEquals(message, exception.getMessage());
			assertEquals(type, exception.getUWSErrorType());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded: the constructor UWSException(int,String,ErrorType) exists.");
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
			newInstance("uws.ISO8601Format", "dateFormat", ISO8601Format.class);
			fail("This MUST have failed because the property value is not a class name!");
		}catch(Exception ex){
			assertEquals(UWSException.class, ex.getClass());
			assertEquals("Class name expected for the property \"dateFormat\" instead of: \"uws.ISO8601Format\"! The specified class must extend/implement uws.ISO8601Format.", ex.getMessage());
		}

		// NO MATCHING CONSTRUCTOR:
		try{
			newInstance("{uws.UWSException}", "exception", UWSException.class, new Class<?>[]{String.class,String.class}, new Object[]{"foo","bar"});
			fail("This MUST have failed because the specified class does not have any expected constructor!");
		}catch(Exception ex){
			assertEquals(UWSException.class, ex.getClass());
			assertEquals("Missing constructor uws.UWSException(java.lang.String, java.lang.String)! See the value \"{uws.UWSException}\" of the property \"exception\".", ex.getMessage());
		}

		// VALID CONSTRUCTOR with primitive type:
		try{
			ClassWithAPrimitiveConstructor aClass = newInstance("{uws.config.TestUWSConfiguration$ClassWithAPrimitiveConstructor}", "aClass", ClassWithAPrimitiveConstructor.class, new Class<?>[]{int.class}, new Object[]{123});
			assertNotNull(aClass);
			assertEquals(ClassWithAPrimitiveConstructor.class, aClass.getClass());
			assertEquals(123, aClass.myParam);
			aClass = newInstance("{uws.config.TestUWSConfiguration$ClassWithAPrimitiveConstructor}", "aClass", ClassWithAPrimitiveConstructor.class, new Class<?>[]{int.class}, new Object[]{new Integer(123)});
			assertNotNull(aClass);
			assertEquals(ClassWithAPrimitiveConstructor.class, aClass.getClass());
			assertEquals(123, aClass.myParam);
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded: the constructor ClassWithAPrimitiveConstructor(int) exists.");
		}

		// WRONG CONSTRUCTOR with primitive type:
		try{
			newInstance("{uws.config.TestUWSConfiguration$ClassWithAPrimitiveConstructor}", "aClass", ClassWithAPrimitiveConstructor.class, new Class<?>[]{Integer.class}, new Object[]{new Integer(123)});
			fail("This MUST have failed because the constructor of the specified class expects an int, not an java.lang.Integer!");
		}catch(Exception ex){
			assertEquals(UWSException.class, ex.getClass());
			assertEquals("Missing constructor uws.config.TestUWSConfiguration$ClassWithAPrimitiveConstructor(java.lang.Integer)! See the value \"{uws.config.TestUWSConfiguration$ClassWithAPrimitiveConstructor}\" of the property \"aClass\".", ex.getMessage());
		}

		// THE CONSTRUCTOR THROWS A UWSException:
		try{
			newInstance("{uws.config.TestUWSConfiguration$ClassAlwaysThrowUWSError}", "uwsError", ClassAlwaysThrowUWSError.class);
			fail("This MUST have failed because the constructor of the specified class throws a UWSException!");
		}catch(Exception ex){
			assertEquals(UWSException.class, ex.getClass());
			assertEquals("This error is always thrown by ClassAlwaysThrowUWSError ^^", ex.getMessage());
		}
	}

	/**
	 * TEST parseLimit(String,String):
	 * 	- nothing, -123, 0				=> -1
	 * 	- 20, 20B, 20 B					=> 20
	 * 	- 100kB, 100 k B				=> 100000
	 * 	- 100MB, 1 0 0MB				=> 100000000
	 * 	- 100GB, 1 0 0 G B				=> 100000000000
	 * 	- B								=> -1
	 * 	- kB							=> -1
	 * 	- foo, 100b, 100TB, 1foo, 20r	=> an exception must occur
	 */
	@Test
	public void testParseLimitStringString(){
		final String propertyName = "LIMIT_PROPERTY"; // TODO Change the string of the propertyName variable
		// Test empty or negative or null values => OK!
		try{
			String[] testValues = new String[]{null,"","  	 ","-123"};
			long limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName);
				assertEquals(limit, -1);
			}
			// 0 test:
			limit = parseLimit("0", propertyName);
			assertEquals(limit, 0);
		}catch(UWSException te){
			fail("All these empty limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted bytes values:
		try{
			String[] testValues = new String[]{"20","20B","20 B"};
			long limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName);
				assertEquals(limit, 20);
			}
		}catch(UWSException te){
			te.printStackTrace();
			fail("All these bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted kilo-bytes values:
		try{
			String[] testValues = new String[]{"100kB","100 k B"};
			long limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName);
				assertEquals(limit, 100000);
			}
		}catch(UWSException te){
			fail("All these kilo-bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted mega-bytes values:
		try{
			String[] testValues = new String[]{"100MB","1 0 0MB"};
			long limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName);
				assertEquals(limit, 100000000);
			}
		}catch(UWSException te){
			fail("All these mega-bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test all accepted giga-bytes values:
		try{
			String[] testValues = new String[]{"100GB","1 0 0 G B"};
			long limit;
			for(String v : testValues){
				limit = parseLimit(v, propertyName);
				assertEquals(limit, 100000000000l);
			}
		}catch(UWSException te){
			fail("All these giga-bytes limit values are valid, so these tests should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test with only the BYTES unit provided:
		try{
			long limit = parseLimit("B", propertyName);
			assertEquals(limit, -1);
		}catch(UWSException te){
			fail("Providing only the ROWS unit is valid, so this test should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test with only the KILO BYTES unit provided:
		try{
			long limit = parseLimit("kB", propertyName);
			assertEquals(limit, -1);
		}catch(UWSException te){
			fail("Providing only the BYTES unit is valid, so this test should have succeeded!\nCaught exception: " + getPertinentMessage(te));
		}

		// Test with incorrect limit formats:
		String[] values = new String[]{"","100","100","1"};
		String[] unitPart = new String[]{"foo","b","TB","foo"};
		for(int i = 0; i < values.length; i++){
			try{
				parseLimit(values[i] + unitPart[i], propertyName);
				fail("This test should have failed because an incorrect limit is provided: \"" + values[i] + unitPart[i] + "\"!");
			}catch(UWSException te){
				assertEquals(te.getClass(), UWSException.class);
				assertEquals(te.getMessage(), "Unknown limit unit (" + unitPart[i] + ") for the property " + propertyName + ": \"" + values[i] + unitPart[i] + "\"!");

			}
		}
		// Test with an incorrect numeric limit value:
		try{
			parseLimit("abc100b", propertyName);
			fail("This test should have failed because an incorrect limit is provided: \"abc100b\"!");
		}catch(UWSException te){
			assertEquals(te.getClass(), UWSException.class);
			assertEquals(te.getMessage(), "Integer expected for the property " + propertyName + " for the substring \"abc100\" of the whole value: \"abc100b\"!");
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

	private static class ClassAlwaysThrowUWSError {
		@SuppressWarnings("unused")
		public ClassAlwaysThrowUWSError() throws UWSException{
			throw new UWSException("This error is always thrown by ClassAlwaysThrowUWSError ^^");
		}
	}

	private static class ClassWithAPrimitiveConstructor {
		private final int myParam;

		@SuppressWarnings("unused")
		public ClassWithAPrimitiveConstructor(int aParam) throws UWSException{
			myParam = aParam;
		}
	}

}
