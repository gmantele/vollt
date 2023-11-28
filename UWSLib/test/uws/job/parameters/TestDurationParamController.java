package uws.job.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

public class TestDurationParamController {

	@Before
	public void setUp() throws Exception{}

	@Test
	public void testCheck(){
		// min < value < max => value (in ms):
		DurationParamController controller = new DurationParamController();
		try{
			controller.reset(10, 5, controller.parseDuration("1M"));
			assertEquals(23l, controller.check(23));
			assertEquals(23l, controller.check("23"));
			assertEquals(1987200000l, controller.check("23D"));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// value < min < max => min:
		controller = new DurationParamController();
		try{
			controller.reset(10, 5, controller.parseDuration("1M"));
			assertEquals(5l, controller.check(2));
			assertEquals(5l, controller.check("5"));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// value > max => max:
		controller = new DurationParamController();
		try{
			controller.reset(10, 5, controller.parseDuration("1M"));
			assertEquals(2592000000l, controller.check(31536000000l));
			assertEquals(2592000000l, controller.check("31536000000"));
			assertEquals(2592000000l, controller.check("2 months"));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// NULL value => default
		controller = new DurationParamController();
		try{
			controller.reset(10, 5, controller.parseDuration("1M"));
			assertEquals(10l, controller.check(null));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}
	}

	@Test
	public void testToStringLong(){
		DurationParamController controller = new DurationParamController();
		try{
			assertEquals(0, controller.toString(null).length());
			assertEquals("0ms", controller.toString(0l));
			assertEquals("23ms", controller.toString(23l));
			assertEquals("1s", controller.toString(1000l));
			assertEquals("60001ms", controller.toString(60001l));
			assertEquals("1m", controller.toString(60000l));
			assertEquals("1D", controller.toString(controller.parseDuration("24 hours")));
			assertEquals("23h", controller.toString(controller.parseDuration("23 hours")));
			assertEquals("23D", controller.toString(controller.parseDuration("23 days")));
			assertEquals("2W", controller.toString(controller.parseDuration("14 days")));
			assertEquals("23W", controller.toString(controller.parseDuration("23 weeks")));
			assertEquals("1M", controller.toString(controller.parseDuration("30 days")));
			assertEquals("23M", controller.toString(controller.parseDuration("23 months")));
			assertEquals("1Y", controller.toString(controller.parseDuration("365 days")));
			assertEquals("23Y", controller.toString(controller.parseDuration("23 years")));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}
	}

	@Test
	public void testParseDuration(){
		// NULL => -1
		DurationParamController controller = new DurationParamController();
		try{
			assertEquals(-1, controller.parseDuration(null));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// Negative duration:
		controller = new DurationParamController();
		try{
			assertEquals(-1, controller.parseDuration("-23"));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// Duration without unit:
		controller = new DurationParamController();
		try{
			assertEquals(23, controller.parseDuration("23"));
			assertEquals(23, controller.parseDuration("  23"));
			assertEquals(23, controller.parseDuration("23 	"));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// Duration with unit:
		controller = new DurationParamController();
		try{
			// MILLISECONDS
			String[] units = new String[]{"ms","milliseconds"," ms"," milliseconds  "};
			for(String u : units)
				assertEquals(23, controller.parseDuration("23" + u));

			// SECONDS
			units = new String[]{"s","sec","seconds"," s","sec  "," seconds  "};
			for(String u : units)
				assertEquals(23000, controller.parseDuration("23" + u));

			// MINUTES
			units = new String[]{"m","min","minutes"," m","min  "," minutes  "};
			for(String u : units)
				assertEquals(1380000, controller.parseDuration("23" + u));

			// HOURS
			units = new String[]{"h","hours"," h"," hours  "};
			for(String u : units)
				assertEquals(82800000, controller.parseDuration("23" + u));

			// DAYS
			units = new String[]{"D","days"," D"," days  "};
			for(String u : units)
				assertEquals(1987200000, controller.parseDuration("23" + u));

			// WEEKS
			units = new String[]{"W","weeks"," W"," weeks  "};
			for(String u : units)
				assertEquals(13910400000l, controller.parseDuration("23" + u));

			// MONTHS
			units = new String[]{"M","months"," M"," months  "};
			for(String u : units)
				assertEquals(59616000000l, controller.parseDuration("23" + u));

			// YEARS
			units = new String[]{"Y","years"," Y"," years  "};
			for(String u : units)
				assertEquals(725328000000l, controller.parseDuration("23" + u));

		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// Duration with an unknown unit:
		controller = new DurationParamController();
		String[] units = new String[]{"MS","Seconds","H","foo"};
		for(String u : units){
			try{
				controller.parseDuration("23" + u);
				fail("This unit (" + u + ") is not supported! An error should have been thrown.");
			}catch(Exception e){
				assertEquals(ParseException.class, e.getClass());
				assertEquals("Unexpected format for a duration: \"23" + u + "\"! Cause: it does not match the following Regular Expression: " + DurationParamController.PATTERN_DURATION.pattern(), e.getMessage());
			}
		}
	}

}
