package uws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestISO8601Format {

	private final long date = 1411737870325L;		// Fri Sep 26 15:24:30 CEST 2014 = 2014-09-26T15:24:30.325+02:00 = 1411737870325 ms
	private final long dateAlone = 1411689600000L;
	private final long dateWithNoTime = 1449792000000L; // Fri Dec 11 2015 = 2015-12-11 = 1449792000000 ms 

	private final long oldDate = -3506029200000L;	// Thu Nov 25 00:00:00 CET 1858 = 1858-11-25T00:00:00+01:00 = -3506029200000 ms

	private static boolean displayMS;
	private static boolean displayTZ;
	private static String targetTZ = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		displayMS = ISO8601Format.displayMilliseconds;
		displayTZ = ISO8601Format.displayTimeZone;
		targetTZ = ISO8601Format.targetTimeZone;
	}

	@Before
	public void setUp() throws Exception{
		ISO8601Format.displayMilliseconds = false;
		ISO8601Format.displayTimeZone = true;
		ISO8601Format.targetTimeZone = "Europe/Berlin";
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{
		ISO8601Format.displayMilliseconds = displayMS;
		ISO8601Format.displayTimeZone = displayTZ;
		ISO8601Format.targetTimeZone = targetTZ;
	}

	@Test
	public void testFormatDate(){
		// Special case: reference for the millisecond representation of dates (1st January 1970):
		assertEquals("1970-01-01T01:00:00+01:00", ISO8601Format.format(0));
		assertEquals("1970-01-01T00:00:00Z", ISO8601Format.formatInUTC(0));

		// Special case: old date (25th November 1858):
		assertEquals("1858-11-25T00:00:00+01:00", ISO8601Format.format(oldDate));
		assertEquals("1858-11-24T23:00:00Z", ISO8601Format.formatInUTC(oldDate));

		// Tests of: FORMAT(Date) && FORMAT(Date, boolean withTimestamp):
		assertEquals("2014-09-26T15:24:30+02:00", ISO8601Format.format(date));
		assertEquals(ISO8601Format.format(date), ISO8601Format.format(date, true));
		assertEquals("2014-09-26T15:24:30", ISO8601Format.format(date, false));

		// Tests of: FORMAT_IN_UTC(Date) && FORMAT_IN_UTC(Date, boolean withTimestamp):
		assertEquals("2014-09-26T13:24:30Z", ISO8601Format.formatInUTC(date));
		assertEquals(ISO8601Format.formatInUTC(date), ISO8601Format.formatInUTC(date, true));
		assertEquals("2014-09-26T13:24:30", ISO8601Format.formatInUTC(date, false));

		// Test with a different time zone:
		assertEquals("2014-09-26T17:24:30+04:00", ISO8601Format.format(date, "Indian/Reunion", true, false));

		// Test with no specified different time zone (the chosen time zone should be the local one):
		assertEquals(ISO8601Format.format(date, TimeZone.getDefault().getID(), true, false), ISO8601Format.format(date, null, true, false));

		// Test with display of milliseconds:
		assertEquals("2014-09-26T15:24:30.325+02:00", ISO8601Format.format(date, null, true, true));
		assertEquals("2014-09-26T15:24:30.325", ISO8601Format.format(date, null, false, true));

		// Same tests but in the UTC time zone:
		assertEquals("2014-09-26T13:24:30.325Z", ISO8601Format.format(date, "UTC", true, true));
		assertEquals("2014-09-26T13:24:30.325", ISO8601Format.format(date, "UTC", false, true));
	}

	@Test
	public void testParse(){
		// Special case: NULL
		try{
			ISO8601Format.parse(null);
			fail("Parse can not theoretically work without a string");
		}catch(Throwable t){
			assertEquals(NullPointerException.class, t.getClass());
		}

		// Special case: ""
		try{
			ISO8601Format.parse("");
			fail("Parse can not theoretically work without a non-empty string");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertEquals("Invalid date format: \"\"! An ISO8601 date was expected.", t.getMessage());
		}

		// Special case: anything stupid rather than a valid date
		try{
			ISO8601Format.parse("stupid thing");
			fail("Parse can not theoretically work without a valid string date");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertEquals("Invalid date format: \"stupid thing\"! An ISO8601 date was expected.", t.getMessage());
		}

		try{
			// Special case: reference for the millisecond representation of dates (1st January 1970):
			assertEquals(0, ISO8601Format.parse("1970-01-01T01:00:00+01:00"));
			assertEquals(0, ISO8601Format.parse("1970-01-01T00:00:00Z"));

			// Special case: old date (25th November 1858):
			assertEquals(oldDate, ISO8601Format.parse("1858-11-25T00:00:00+01:00"));
			assertEquals(oldDate, ISO8601Format.parse("1858-11-24T23:00:00Z"));

			// Test with a date expressed in weeks:
			assertEquals(dateWithNoTime, ISO8601Format.parse("2015-W50-5"));
			assertEquals(dateWithNoTime, ISO8601Format.parse("2015W505"));

			// Test with a date expressed in days of year:
			assertEquals(dateWithNoTime, ISO8601Format.parse("2015-345"));
			assertEquals(dateWithNoTime, ISO8601Format.parse("2015345"));

			// Test with a perfectly valid date in ISO8601: 
			assertEquals(dateAlone, ISO8601Format.parse("2014-09-26"));
			assertEquals(date, ISO8601Format.parse("2014-09-26T15:24:30.325+02:00"));
			assertEquals(date - 325, ISO8601Format.parse("2014-09-26T15:24:30+02:00"));

			// Test with Z as time zone (UTC):
			assertEquals(date, ISO8601Format.parse("2014-09-26T13:24:30.325Z"));
			assertEquals(date - 325, ISO8601Format.parse("2014-09-26T13:24:30Z"));

			// If no time zone is specified, the local one should be used:
			assertEquals(date, ISO8601Format.parse("2014-09-26T13:24:30.325"));
			assertEquals(date - 325, ISO8601Format.parse("2014-09-26T13:24:30"));

			// All the previous tests without the _ between days, month, and years:
			assertEquals(0, ISO8601Format.parse("19700101T01:00:00+01:00"));
			assertEquals(0, ISO8601Format.parse("19700101T00:00:00Z"));
			assertEquals(oldDate, ISO8601Format.parse("18581125T00:00:00+01:00"));
			assertEquals(oldDate, ISO8601Format.parse("18581124T23:00:00Z"));
			assertEquals(dateAlone, ISO8601Format.parse("20140926"));
			assertEquals(date, ISO8601Format.parse("20140926T15:24:30.325+02:00"));
			assertEquals(date - 325, ISO8601Format.parse("20140926T15:24:30+02:00"));
			assertEquals(date, ISO8601Format.parse("20140926T13:24:30.325Z"));
			assertEquals(date - 325, ISO8601Format.parse("20140926T13:24:30Z"));
			assertEquals(date, ISO8601Format.parse("20140926T13:24:30.325"));
			assertEquals(date - 325, ISO8601Format.parse("20140926T13:24:30"));

			// All the previous tests without the : between hours, minutes and seconds:
			assertEquals(0, ISO8601Format.parse("1970-01-01T010000+0100"));
			assertEquals(oldDate, ISO8601Format.parse("1858-11-25T000000+0100"));
			assertEquals(date, ISO8601Format.parse("2014-09-26T152430.325+0200"));
			assertEquals(date - 325, ISO8601Format.parse("2014-09-26T152430+0200"));

			// All the previous tests by replacing the T between date and time by a space:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00Z"));
			assertEquals(oldDate, ISO8601Format.parse("1858-11-24 23:00:00Z"));
			assertEquals(date, ISO8601Format.parse("2014-09-26 13:24:30.325Z"));
			assertEquals(date - 325, ISO8601Format.parse("2014-09-26 13:24:30Z"));
			assertEquals(date, ISO8601Format.parse("2014-09-26 13:24:30.325"));
			assertEquals(date - 325, ISO8601Format.parse("2014-09-26 13:24:30"));

			// Test with only the year: YYYY
			assertEquals(0, ISO8601Format.parse("1970"));

			// Test with only the year and month: YYYY-MM
			assertEquals(0, ISO8601Format.parse("1970-01"));

			// Test with a complete date: YYYY-MM-DD
			assertEquals(0, ISO8601Format.parse("1970-01-01"));

			// Test with a complete date: YYYY-'W'ww-D
			assertEquals(0, ISO8601Format.parse("1970-W01-4"));

			// Test with a complete date plus hours: YYYY-MM-DDThh:mmTZD
			// ...with no time zone information:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00"));
			// ...with Z:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00Z"));
			// ...with time zone:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00+00:00"));

			// Test with a complete date plus hours and minutes: YYYY-MM-DDThh:mmTZD
			// ...with no time zone information:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00"));
			// ...with Z:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00Z"));
			// ...with time zone:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00+00"));

			// Test with a complete date plus hours, minutes and seconds: YYYY-MM-DDThh:mm:ssTZD
			// ...with no time zone information:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00"));
			// ...with Z:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00Z"));
			// ...with time zone:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00-0000"));

			// Test with a complete date plus hours, minutes, seconds and a decimal fraction of a second: YYYY-MM-DDThh:mm:ss.sTZD
			// ...with no time zone information:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00.0"));
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00.0000000"));
			// ...with Z:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00.0Z"));
			// ...with time zone:
			assertEquals(0, ISO8601Format.parse("1970-01-01 00:00:00.0-00"));
			assertEquals(0, ISO8601Format.parse("1970-01-01 03:00:00.0+03"));
			assertEquals(0, ISO8601Format.parse("1970-01-01 03:00:00.0+03:00"));

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("No error should have occured here! All date expressions are correct.");
		}

	}

}
