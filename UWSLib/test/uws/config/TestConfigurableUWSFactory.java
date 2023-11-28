package uws.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uws.config.ConfigurableUWSFactory.PATTERN_PARAMETER;

import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;

import uws.UWSException;
import uws.job.parameters.DurationParamController;
import uws.job.parameters.NumericParamController;
import uws.job.parameters.StringParamController;

public class TestConfigurableUWSFactory {

	@Before
	public void setUp() throws Exception{}

	@Test
	public void testPatternParameter(){
		/* JUST A NAME => OK */
		Matcher matcher = PATTERN_PARAMETER.matcher("param1");
		assertTrue(matcher.matches());
		assertEquals("param1", matcher.group(16));
		assertNull(matcher.group(6));
		assertNull(matcher.group(7));
		assertNull(matcher.group(11));
		assertNull(matcher.group(18));

		/* EMPTY STRING => OK */
		matcher = PATTERN_PARAMETER.matcher("");
		assertTrue(matcher.matches());
		assertEquals(0, matcher.group(1).length());
		assertNull(matcher.group(6));
		assertNull(matcher.group(7));
		assertNull(matcher.group(11));
		assertNull(matcher.group(18));

		/* EMPTY STRING + , (SOMETHING OR NOT) => OK */
		matcher = PATTERN_PARAMETER.matcher(",");
		assertTrue(matcher.matches());
		assertEquals(0, matcher.group(1).length());
		assertEquals(0, matcher.group(18).length());
		matcher = PATTERN_PARAMETER.matcher(",something");
		assertTrue(matcher.matches());
		assertEquals(0, matcher.group(1).length());
		assertEquals("something", matcher.group(18));
		assertNull(matcher.group(6));
		assertNull(matcher.group(7));
		assertNull(matcher.group(11));

		/* A FULL DEFINITION WITH JUST NAME => ERROR */
		matcher = PATTERN_PARAMETER.matcher("[param1]");
		assertFalse(matcher.matches());

		/* NUMERIC */
		matcher = PATTERN_PARAMETER.matcher("[param1,yes, numeric, 123,45,65]");
		assertTrue(matcher.matches());
		assertEquals("param1", matcher.group(2));
		assertEquals("yes", matcher.group(5));
		assertEquals("numeric", matcher.group(7));
		assertEquals(" 123", matcher.group(8));
		assertEquals("45", matcher.group(9));
		assertEquals("65", matcher.group(10));
		assertNull(matcher.group(11));
		assertNull(matcher.group(16));
		assertNull(matcher.group(18));

		matcher = PATTERN_PARAMETER.matcher("[param1,yes, numeric, 123]");
		assertFalse(matcher.matches());

		/* DURATION */
		matcher = PATTERN_PARAMETER.matcher("[param1,false,duration,10D,1h,1M]");
		assertTrue(matcher.matches());
		assertEquals("param1", matcher.group(2));
		assertEquals("false", matcher.group(5));
		assertEquals("duration", matcher.group(7));
		assertEquals("10D", matcher.group(8));
		assertEquals("1h", matcher.group(9));
		assertEquals("1M", matcher.group(10));
		assertNull(matcher.group(11));
		assertNull(matcher.group(16));
		assertNull(matcher.group(18));

		/* STRING */
		matcher = PATTERN_PARAMETER.matcher(" [ param1, true, string , \"foo\" , /toto///i ] ");
		assertTrue(matcher.matches());
		assertEquals(" param1", matcher.group(2));
		assertEquals(" true", matcher.group(5));
		assertEquals("string", matcher.group(11));
		assertEquals("foo", matcher.group(12));
		assertEquals("toto//", matcher.group(13));
		assertEquals("i", matcher.group(15));
		assertNull(matcher.group(7));
		assertNull(matcher.group(16));
		assertNull(matcher.group(18));

		matcher = PATTERN_PARAMETER.matcher(" [ param1, true, STRING, \"foo\" , /toto///I ] ");
		assertTrue(matcher.matches());
		assertEquals(" param1", matcher.group(2));
		assertEquals(" true", matcher.group(5));
		assertEquals("STRING", matcher.group(11));
		assertEquals("foo", matcher.group(12));
		assertEquals("toto//", matcher.group(13));
		assertEquals("I", matcher.group(15));
		assertNull(matcher.group(7));
		assertNull(matcher.group(16));
		assertNull(matcher.group(18));

		matcher = PATTERN_PARAMETER.matcher(" [ param1, true, STRING, \"foo\" , /f.*/i ] ");
		assertTrue(matcher.matches());
		assertEquals(" param1", matcher.group(2));
		assertEquals(" true", matcher.group(5));
		assertEquals("STRING", matcher.group(11));
		assertEquals("foo", matcher.group(12));
		assertEquals("f.*", matcher.group(13));
		assertEquals("i", matcher.group(15));
		assertNull(matcher.group(7));
		assertNull(matcher.group(16));
		assertNull(matcher.group(18));

		// MORE PARAMETER
		matcher = PATTERN_PARAMETER.matcher("param1, [ param2, true, STRING, \"foo\" , /toto///I ] ");
		assertTrue(matcher.matches());
		assertEquals("param1", matcher.group(16));
		assertNull(matcher.group(6));
		assertNull(matcher.group(7));
		assertNull(matcher.group(11));
		assertEquals(" [ param2, true, STRING, \"foo\" , /toto///I ] ", matcher.group(18));

		matcher = PATTERN_PARAMETER.matcher("param1,  ");
		assertTrue(matcher.matches());
		assertEquals("param1", matcher.group(16));
		assertNull(matcher.group(6));
		assertNull(matcher.group(7));
		assertNull(matcher.group(11));
		assertEquals("  ", matcher.group(18));
		
		// CUSTOM CONTROLLER
		matcher = PATTERN_PARAMETER.matcher("[param1, {aPackage.MyCustomController} ]");
		assertTrue(matcher.matches());
		assertEquals("param1", matcher.group(2));
		assertEquals("{aPackage.MyCustomController}", matcher.group(4));
		assertNull(matcher.group(5));
		assertNull(matcher.group(6));
		assertNull(matcher.group(16));
		assertNull(matcher.group(18));
	}
	
	@Test
	public void testInitParameters(){
		// NO PARAMETER
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", "");
			assertFalse(factory.jobParams.containsKey("jobs"));
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", "  	");
			assertFalse(factory.jobParams.containsKey("jobs"));
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", null);
			assertFalse(factory.jobParams.containsKey("jobs"));
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
		
		// PARAMETER WITH NO CONTROLLER
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", "param1");
			assertNotNull(factory.jobParams.get("jobs"));
			assertEquals(1, factory.jobParams.get("jobs").size());
			assertTrue(factory.jobParams.get("jobs").containsKey("param1"));
			assertNull(factory.jobParams.get("jobs").get("param1"));
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
		
		// PARAMETER WITH A CUSTOM CONTROLLER
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", "[ param1 , {uws.job.parameters.NumericParamController} ]");
			assertNotNull(factory.jobParams.get("jobs"));
			assertEquals(1, factory.jobParams.get("jobs").size());
			assertTrue(factory.jobParams.get("jobs").containsKey("param1"));
			assertNotNull(factory.jobParams.get("jobs").get("param1"));
			assertEquals(NumericParamController.class, factory.jobParams.get("jobs").get("param1").getClass());
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
		
		// PARAMETER WITH A STRING CONTROLLER
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", "[ param1 , yes, string, \"blabla\" , /b.*/i ], [ param2 , , string, \"blabla\" , /b.*/ ], [ param3 , false , string, \" a\" , /\\s+[a-zA-Z]/ ]");
			assertNotNull(factory.jobParams.get("jobs"));
			assertEquals(3, factory.jobParams.get("jobs").size());
			// test param1
			assertTrue(factory.jobParams.get("jobs").containsKey("param1"));
			assertNotNull(factory.jobParams.get("jobs").get("param1"));
			assertEquals(StringParamController.class, factory.jobParams.get("jobs").get("param1").getClass());
			StringParamController controller = (StringParamController)factory.jobParams.get("jobs").get("param1");
			assertEquals("blabla", controller.getDefault());
			assertEquals("(?i)b.*", controller.getRegExp());
			assertTrue(controller.allowModification());
			// test param2
			assertTrue(factory.jobParams.get("jobs").containsKey("param2"));
			assertNotNull(factory.jobParams.get("jobs").get("param2"));
			assertEquals(StringParamController.class, factory.jobParams.get("jobs").get("param2").getClass());
			controller = (StringParamController)factory.jobParams.get("jobs").get("param2");
			assertEquals("blabla", controller.getDefault());
			assertEquals("b.*", controller.getRegExp());
			assertTrue(controller.allowModification());
			// test param3
			assertTrue(factory.jobParams.get("jobs").containsKey("param3"));
			assertNotNull(factory.jobParams.get("jobs").get("param3"));
			assertEquals(StringParamController.class, factory.jobParams.get("jobs").get("param3").getClass());
			controller = (StringParamController)factory.jobParams.get("jobs").get("param3");
			assertEquals(" a", controller.getDefault());
			assertEquals("\\s+[a-zA-Z]", controller.getRegExp());
			assertFalse(controller.allowModification());
			
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
		
		// PARAMETER WITH A NUMERIC CONTROLLER
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", "[ param1 , yes, numeric, 20, 10,30], [ param2 , , numeric, 0, -10, 10 ], [ param3 , false , numeric, , , 100]");
			assertNotNull(factory.jobParams.get("jobs"));
			assertEquals(3, factory.jobParams.get("jobs").size());
			// test param1
			assertTrue(factory.jobParams.get("jobs").containsKey("param1"));
			assertNotNull(factory.jobParams.get("jobs").get("param1"));
			assertEquals(NumericParamController.class, factory.jobParams.get("jobs").get("param1").getClass());
			NumericParamController controller = (NumericParamController)factory.jobParams.get("jobs").get("param1");
			assertEquals(20.0, controller.getDefault());
			assertEquals(10.0, controller.getMinimum());
			assertEquals(30.0, controller.getMaximum());
			assertTrue(controller.allowModification());
			// test param2
			assertTrue(factory.jobParams.get("jobs").containsKey("param2"));
			assertNotNull(factory.jobParams.get("jobs").get("param2"));
			assertEquals(NumericParamController.class, factory.jobParams.get("jobs").get("param2").getClass());
			controller = (NumericParamController)factory.jobParams.get("jobs").get("param2");
			assertEquals(0.0, controller.getDefault());
			assertEquals(-10.0, controller.getMinimum());
			assertEquals(10.0, controller.getMaximum());
			assertTrue(controller.allowModification());
			// test param3
			assertTrue(factory.jobParams.get("jobs").containsKey("param3"));
			assertNotNull(factory.jobParams.get("jobs").get("param3"));
			assertEquals(NumericParamController.class, factory.jobParams.get("jobs").get("param3").getClass());
			controller = (NumericParamController)factory.jobParams.get("jobs").get("param3");
			assertNull(controller.getDefault());
			assertNull(controller.getMinimum());
			assertEquals(100.0, controller.getMaximum());
			assertFalse(controller.allowModification());
			
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
		
		// PARAMETER WITH A DURATION CONTROLLER
		try{
			ConfigurableUWSFactory factory = new ConfigurableUWSFactory();
			factory.initParameters("jobs", "[ param1 , yes, duration, 20sec, 1s, 30 seconds], [ param2 , , duration, 10min, 1 m, 1 hours ], [ param3 , false , duration, 1D , , 1 years]");
			assertNotNull(factory.jobParams.get("jobs"));
			assertEquals(3, factory.jobParams.get("jobs").size());
			// test param1
			assertTrue(factory.jobParams.get("jobs").containsKey("param1"));
			assertNotNull(factory.jobParams.get("jobs").get("param1"));
			assertEquals(DurationParamController.class, factory.jobParams.get("jobs").get("param1").getClass());
			DurationParamController controller = (DurationParamController)factory.jobParams.get("jobs").get("param1");
			assertEquals(20000l, controller.getDefault());
			assertEquals(1000l, controller.getMinimum());
			assertEquals(30000l, controller.getMaximum());
			assertTrue(controller.allowModification());
			// test param2
			assertTrue(factory.jobParams.get("jobs").containsKey("param2"));
			assertNotNull(factory.jobParams.get("jobs").get("param2"));
			assertEquals(DurationParamController.class, factory.jobParams.get("jobs").get("param2").getClass());
			controller = (DurationParamController)factory.jobParams.get("jobs").get("param2");
			assertEquals(600000l, controller.getDefault());
			assertEquals(60000l, controller.getMinimum());
			assertEquals(3600000l, controller.getMaximum());
			assertTrue(controller.allowModification());
			// test param3
			assertTrue(factory.jobParams.get("jobs").containsKey("param3"));
			assertNotNull(factory.jobParams.get("jobs").get("param3"));
			assertEquals(DurationParamController.class, factory.jobParams.get("jobs").get("param3").getClass());
			controller = (DurationParamController)factory.jobParams.get("jobs").get("param3");
			assertEquals(86400000l, controller.getDefault());
			assertNull(controller.getMinimum());
			assertEquals(31536000000l, controller.getMaximum());
			assertFalse(controller.allowModification());
			
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("Unexpected error! See the console for a full stack trace.");
		}
	}

}
