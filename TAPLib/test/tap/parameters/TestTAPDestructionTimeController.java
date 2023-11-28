package tap.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uws.ISO8601Format;

public class TestTAPDestructionTimeController {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{}

	@Before
	public void setUp() throws Exception{}

	@After
	public void tearDown() throws Exception{}

	@Test
	public void testCheck(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		TAPDestructionTimeController controller = new TAPDestructionTimeController(service);
		Calendar calendar = Calendar.getInstance();

		try{
			// A NULL destruction time will always return NULL:
			assertNull(controller.check(null));

			// By default, the controller has no limit on the destruction time, so let's try with a destruction in 100 years:
			calendar.add(Calendar.YEAR, 100);
			checkDate(calendar.getTime(), controller.check(calendar.getTime()));
			checkDate(calendar.getTime(), controller.check(ISO8601Format.format(calendar.getTimeInMillis())));

			// With just a default destruction time (of 10 minutes):
			service.setRetentionPeriod(600, -1);
			Calendar defaultTime = Calendar.getInstance();
			defaultTime.add(Calendar.MINUTE, 10);
			checkDate(defaultTime.getTime(), controller.check(null));
			checkDate(calendar.getTime(), controller.check(calendar.getTime()));

			// With just a maximum destruction time (of 1 hour):
			service.setRetentionPeriod(0, 3600);
			Calendar maxTime = Calendar.getInstance();
			maxTime.add(Calendar.HOUR, 1);
			checkDate(maxTime.getTime(), controller.check(null));
			checkDate(defaultTime.getTime(), controller.check(defaultTime.getTime()));
			checkDate(maxTime.getTime(), controller.check(calendar.getTime()));

			// With a default (10 minutes) AND a maximum (1 hour) destruction time:
			service.setRetentionPeriod(600, 3600);
			checkDate(defaultTime.getTime(), controller.check(null));
			checkDate(maxTime.getTime(), controller.check(calendar.getTime()));
			calendar = Calendar.getInstance();
			calendar.add(Calendar.MINUTE, 30);
			checkDate(calendar.getTime(), controller.check(calendar.getTime()));

		}catch(Exception t){
			t.printStackTrace();
			fail();
		}
	}

	@Test
	public void testGetDefault(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		TAPDestructionTimeController controller = new TAPDestructionTimeController(service);

		// By default, when nothing is set, the default destruction time is NULL (the job will never be destroyed):
		assertNull(controller.getDefault());

		// With no interval, the default destruction time should remain NULL (the job will never be destroyed):
		service.setRetentionPeriod(0, -1);
		assertNull(controller.getDefault());

		// With a negative interval, the destruction time should also be NULL:
		service.setRetentionPeriod(-1, -1);
		assertNull(controller.getDefault());

		// With a destruction interval of 100 minutes:
		Calendar calendar = Calendar.getInstance();
		service.setRetentionPeriod(6000, -1);
		calendar.add(Calendar.SECOND, 6000);	// note: in seconds rather than minutes, in order to take into account the time shift if any
		checkDate(calendar.getTime(), controller.getDefault());

		// With a destruction interval of 100 seconds:
		service.setRetentionPeriod(100, -1);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 100);
		checkDate(calendar.getTime(), controller.getDefault());

		// With a destruction interval of 1 week:
		service.setRetentionPeriod(7 * 24 * 3600, -1);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 7 * 24 * 3600);	// note: in seconds rather than days, in order to take into account the time shift if any
		checkDate(calendar.getTime(), controller.getDefault());
	}

	@Test
	public void testGetMaxDestructionTime(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		TAPDestructionTimeController controller = new TAPDestructionTimeController(service);

		// By default, when nothing is set, the maximum destruction time is NULL (the job will never be destroyed):
		assertNull(controller.getMaxDestructionTime());

		// With no interval, the maximum destruction time should remain NULL (the job will never be destroyed):
		service.setRetentionPeriod(-1, 0);
		assertNull(controller.getMaxDestructionTime());

		// With a negative interval, the destruction time should also be NULL:
		service.setRetentionPeriod(-1, -1);
		assertNull(controller.getMaxDestructionTime());

		// With a destruction interval of 100 minutes:
		Calendar calendar = Calendar.getInstance();
		service.setRetentionPeriod(-1, 6000);
		calendar.add(Calendar.SECOND, 6000);	// note: in seconds rather than minutes, in order to take into account the time shift if any
		checkDate(calendar.getTime(), controller.getMaxDestructionTime());

		// With a destruction interval of 100 seconds:
		service.setRetentionPeriod(-1, 100);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 100);
		checkDate(calendar.getTime(), controller.getMaxDestructionTime());

		// With a destruction interval of 1 week:
		service.setRetentionPeriod(-1, 7 * 24 * 3600);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 7 * 24 * 3600);	// note: in seconds rather than days, in order to take into account the time shift if any
		checkDate(calendar.getTime(), controller.getMaxDestructionTime());
	}

	@Test
	public void testAllowModification(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		TAPDestructionTimeController controller = new TAPDestructionTimeController(service);

		// By default, user modification of the destruction time is allowed:
		assertTrue(controller.allowModification());

		controller.allowModification(true);
		assertTrue(controller.allowModification());

		controller.allowModification(false);
		assertFalse(controller.allowModification());
	}

	private void checkDate(final Date expected, final Object val){
		assertTrue(val instanceof Date);

		if (expected != null && val != null){
			Calendar cexpected = Calendar.getInstance(),
					cval = Calendar.getInstance();
			cexpected.setTime(expected);
			cval.setTime((Date)val);

			try{
				assertEquals(cexpected.get(Calendar.DAY_OF_MONTH), cval.get(Calendar.DAY_OF_MONTH));
				assertEquals(cexpected.get(Calendar.MONTH), cval.get(Calendar.MONTH));
				assertEquals(cexpected.get(Calendar.YEAR), cval.get(Calendar.YEAR));
				assertEquals(cexpected.get(Calendar.HOUR), cval.get(Calendar.HOUR));
				assertEquals(cexpected.get(Calendar.MINUTE), cval.get(Calendar.MINUTE));
				assertTrue(cval.get(Calendar.SECOND) - cexpected.get(Calendar.SECOND) >= 0 && cval.get(Calendar.SECOND) - cexpected.get(Calendar.SECOND) <= 1);
			}catch(AssertionError e){
				fail("Expected <" + expected + "> but was <" + val + ">");
			}
		}else if (expected == null && val == null)
			return;
		else
			fail("Expected <" + expected + "> but was <" + val + ">");
	}

}
