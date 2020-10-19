package uws.service.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog;
import uws.service.log.UWSLog.LogLevel;

public class TestLogRotation {

	@Test
	public void testEventFrequencyCreation(){
		EventFrequency freq;

		try{
			String DEFAULT_FREQ = "daily at 00:00";

			// FREQ = NULL => !!! ; frequency = every day
			freq = new EventFrequency(null);
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "" => !!! ; frequency = every day
			freq = new EventFrequency("");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "blabla" => !!!
			freq = new EventFrequency("blabla");
			assertEquals(DEFAULT_FREQ, freq.toString());

			/* *********** */
			/* DAILY EVENT */
			/* *********** */
			DEFAULT_FREQ = "daily at 00:00";

			// FREQ = "D" => ok! ; frequency = every day at midnight
			freq = new EventFrequency("D");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "D 06" => !!! ; frequency = every day at midnight
			freq = new EventFrequency("D 06");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "D 06 30" => ok! ; frequency = every day at 06:30
			freq = new EventFrequency("D 06 30");
			assertEquals("daily at 06:30", freq.toString());

			// FREQ = "D 6 30" => ok! ; frequency = every day at 06:30
			freq = new EventFrequency("D 6 30");
			assertEquals("daily at 06:30", freq.toString());

			// FREQ = "D	06 	 30" => ok! (with spaces and tabs inside) ; frequency = every day at 06:30
			freq = new EventFrequency("D	06 	 30");
			assertEquals("daily at 06:30", freq.toString());

			// FREQ = "D 24 30" => !!! ; frequency = every day at midnight
			freq = new EventFrequency("D 24 30");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "D 06 60" => !!! ; frequency = every day at midnight
			freq = new EventFrequency("D 06 60");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "D 6 30 01 blabla" => ok! ; frequency = every day at 06:30
			freq = new EventFrequency("D 6 30 01 blabla");
			assertEquals("daily at 06:30", freq.toString());

			// FREQ = "d 06 30" => !!! ; frequency = every day at midnight
			freq = new EventFrequency("d 06 30");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "D HH mm" => !!!
			freq = new EventFrequency("D HH mm");
			assertEquals(DEFAULT_FREQ, freq.toString());

			/* ********** */
			/* WEEK EVENT */
			/* ********** */
			DEFAULT_FREQ = "weekly on Sunday at 00:00";

			// FREQ = "W" => ok! ; frequency = every week the Sunday at midnight
			freq = new EventFrequency("W");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "W 06" => !!! ; frequency = every week the Sunday at midnight
			freq = new EventFrequency("W 06");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "W 06 30" => !!! ; frequency = every week the Sunday at midnight
			freq = new EventFrequency("W 06 30");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "W 2" => !!! ; frequency = every week the Sunday at midnight
			freq = new EventFrequency("W 2");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "W 2 06" => !!! ; frequency = every week the Sunday at midnight
			freq = new EventFrequency("W 2 06");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "W 2 06 30" => ok! ; frequency = every week the Monday at 06:30
			freq = new EventFrequency("W 2 06 30");
			assertEquals("weekly on Monday at 06:30", freq.toString());

			// FREQ = "W 0 06 30" => !!! ; frequency = every week the Sunday at 06:30
			freq = new EventFrequency("W 0 06 30");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "W 10 06 30" => !!! ; frequency = every week the Sunday at 06:30
			freq = new EventFrequency("W 10 06 30");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "w 2 06 30" => !!! ; frequency = every day at 00:00
			freq = new EventFrequency("w 2 06 30");
			assertEquals("daily at 00:00", freq.toString());

			// FREQ = "W 2 6 30" => ok! ; frequency = every week the Monday at 06:30
			freq = new EventFrequency("W 2 6 30");
			assertEquals("weekly on Monday at 06:30", freq.toString());

			// FREQ = "W	2   6 	 30" => ok! (with spaces and tabs inside) ; frequency = every week the Monday at 06:30
			freq = new EventFrequency("W	2   6 	 30");
			assertEquals("weekly on Monday at 06:30", freq.toString());

			// FREQ = "W 2 6 30 12 blabla" => ok! ; frequency = every week the Monday at 06:30
			freq = new EventFrequency("W 2 6 30 12 blabla");
			assertEquals("weekly on Monday at 06:30", freq.toString());

			/* ***************************************** */
			/* MONTH EVENT (same code as for WEEK EVENT) */
			/* ***************************************** */
			DEFAULT_FREQ = "monthly on the 1st at 00:00";

			// FREQ = "M 2 06 30" => ok! ; frequency = every month on the 2nd at 06:30
			freq = new EventFrequency("M 2 06 30");
			assertEquals("monthly on the 2nd at 06:30", freq.toString());

			// FREQ = "M	2 	 06   30" => ok! (with spaces and tabs inside) ; frequency = every month on the 2nd at 06:30
			freq = new EventFrequency("M	2 	 06   30");
			assertEquals("monthly on the 2nd at 06:30", freq.toString());

			// FREQ = "m 2 06 30" => !!! ; frequency = every minute
			freq = new EventFrequency("m 2 06 30");
			assertEquals("every minute", freq.toString());

			// FREQ = "M 0 06 30" => !!! ; frequency = every month on the 1st at 00:00
			freq = new EventFrequency("M 0 06 30");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "M 32 06 30" => !!! ; frequency = every month on the 1st at 00:00
			freq = new EventFrequency("M 32 06 30");
			assertEquals(DEFAULT_FREQ, freq.toString());

			/* ********** */
			/* HOUR EVENT */
			/* ********** */
			DEFAULT_FREQ = "hourly at 00";

			// FREQ = "h" => ok! ; frequency = every hour at 00
			freq = new EventFrequency("h");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "h 10" => ok! ; frequency = every hour at 10
			freq = new EventFrequency("h 10");
			assertEquals("hourly at 10", freq.toString());

			// FREQ = "h 	 10" => ok! (with spaces and tabs inside) ; frequency = every hour at 10
			freq = new EventFrequency("h 	 10");
			assertEquals("hourly at 10", freq.toString());

			// FREQ = "H 10" => !!! ; frequency = every day at 00:00
			freq = new EventFrequency("H 10");
			assertEquals("daily at 00:00", freq.toString());

			// FREQ = "h 5" => ok! ; frequency = every hour at 05
			freq = new EventFrequency("h 5");
			assertEquals("hourly at 05", freq.toString());

			// FREQ = "h 60" => !!! ; frequency = every hour at 00
			freq = new EventFrequency("h 60");
			assertEquals("hourly at 00", freq.toString());

			// FREQ = "h 10 12 blabla" => ok! ; frequency = every hour at 10
			freq = new EventFrequency("h 10 12 blabla");
			assertEquals("hourly at 10", freq.toString());

			/* ********** */
			/* HOUR EVENT */
			/* ********** */
			DEFAULT_FREQ = "every minute";

			// FREQ = "m" => ok! ; frequency = every minute
			freq = new EventFrequency("m");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "m 10 blabla" => ok! ; frequency = every minute
			freq = new EventFrequency("m 10 blabla");
			assertEquals(DEFAULT_FREQ, freq.toString());

			// FREQ = "M" => !!! ; frequency = every month on the 1st at 00:00
			freq = new EventFrequency("M");
			assertEquals("monthly on the 1st at 00:00", freq.toString());

		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("UNEXPECTED EXCEPTION: \"" + e.getMessage() + "\"");
		}
	}

	@Test
	public void testGetLogOutput(){
		try{
			final LocalUWSFileManager fileManager = new LocalUWSFileManager(new File("."));
			fileManager.logRotation = new EventFrequency("m");
			final int MAX_TIME = 3000; // 3 seconds => 68 messages (for 5 threads)
			int nbExpectedMessages = 0;

			// Delete old log file:
			fileManager.getLogFile(LogLevel.DEBUG, null).delete();

			// Log a lot of messages:
			final UWSLog logger = new DefaultUWSLog(fileManager);
			for(int i = 0; i < 5; i++){
				final int logFreq = i + 1;
				nbExpectedMessages += 30 / logFreq;
				(new Thread(new Runnable() {
					@Override
					public void run(){
						try{
							final int nbMsgs = 30 / logFreq;
							final int freq = MAX_TIME / nbMsgs;
							for(int cnt = 0; cnt < nbMsgs; cnt++){
								logger.log(LogLevel.INFO, "TEST", "LOG MESSAGE FROM Thread-" + logFreq, null);
								assertFalse(fileManager.getLogOutput(LogLevel.INFO, "UWS").checkError());	// if true, it means that at least one attempt to write something fails, and so, that write attempts have been done after a log rotation!
								Thread.sleep(freq);
							}
						}catch(InterruptedException e){
							e.printStackTrace(System.err);
							fail("ERROR WITH THE THREAD-" + logFreq);
						}catch(IOException e){
							e.printStackTrace(System.err);
							fail("IO ERROR WHEN RETRIEVING THE LOG OUTPUT IN THE THREAD-" + logFreq);
						}
					}
				})).start();
			}
			Thread.sleep(MAX_TIME);

			// Check that all messages have been well written:
			BufferedReader input = new BufferedReader(new InputStreamReader(fileManager.getLogInput(LogLevel.DEBUG, null)));
			int nbLines = 0;
			while(input.readLine() != null)
				nbLines++;
			nbLines -= 3; // deduce the number of 3 header lines
			assertEquals(nbExpectedMessages, nbLines);

			// Delete log file if no error:
			fileManager.getLogFile(LogLevel.DEBUG, null).delete();

		}catch(InterruptedException e){
			e.printStackTrace(System.err);
			fail("CAN NOT WAIT 3 SECONDS!");
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("CAN NOT CREATE THE FILE MANAGER!");
		}
	}

	@Test
	public void testNextEvent(){
		// Event = weekly on Sunday at midnight
		EventFrequency event = new EventFrequency("W 1 0 0");
		assertEquals("weekly on Sunday at 00:00", event.toString());

		/* CASE: Today = Friday */

		// Reference date for the test: Friday 27th July 2018 at noon
		GregorianCalendar date = new GregorianCalendar(2018, Calendar.JULY, 27, 12, 0);
		assertEquals(Calendar.FRIDAY, date.get(Calendar.DAY_OF_WEEK));

		// Get the next event from the given date:
		Date nextEvent = event.nextEvent(date.getTime());
		GregorianCalendar expectedNextEvent = new GregorianCalendar(2018, Calendar.JULY, 29, 0, 0);
		assertEquals(expectedNextEvent.getTime().getTime(), nextEvent.getTime());

		/* CASE: Today = Saturday */

		// Reference date for the test: Saturday 28th July 2018 at noon
		date = new GregorianCalendar(2018, Calendar.JULY, 28, 12, 0);
		assertEquals(Calendar.SATURDAY, date.get(Calendar.DAY_OF_WEEK));

		// Get the next event from the given date:
		nextEvent = event.nextEvent(date.getTime());
		assertEquals(expectedNextEvent.getTime().getTime(), nextEvent.getTime());

		/* CASE: Today = Sunday */

		// Reference date for the test: Sunday 28th July 2018 at noon
		date = new GregorianCalendar(2018, Calendar.JULY, 29, 12, 0);
		assertEquals(Calendar.SUNDAY, date.get(Calendar.DAY_OF_WEEK));

		// Get the next event from the given date:
		nextEvent = event.nextEvent(date.getTime());
		expectedNextEvent = new GregorianCalendar(2018, Calendar.AUGUST, 5, 0, 0);
		assertEquals(expectedNextEvent.getTime().getTime(), nextEvent.getTime());
	}

}
