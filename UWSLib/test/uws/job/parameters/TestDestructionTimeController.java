package uws.job.parameters;

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
import uws.job.parameters.DestructionTimeController.DateField;

public class TestDestructionTimeController {

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
		DestructionTimeController controller = new DestructionTimeController();
		Calendar calendar = Calendar.getInstance();

		try{
			// A NULL destruction time will always return NULL:
			assertNull(controller.check(null));

			// By default, the controller has no limit on the destruction time, so let's try with a destruction in 100 years:
			calendar.add(Calendar.YEAR, 100);
			checkDate(calendar.getTime(), controller.check(calendar.getTime()));
			checkDate(calendar.getTime(), controller.check(ISO8601Format.format(calendar.getTimeInMillis())));

			// With just a default destruction time (of 10 minutes):
			controller.setDefaultDestructionInterval(10);
			Calendar defaultTime = Calendar.getInstance();
			defaultTime.add(Calendar.MINUTE, 10);
			checkDate(defaultTime.getTime(), controller.check(null));
			checkDate(calendar.getTime(), controller.check(calendar.getTime()));

			// With just a maximum destruction time (of 1 hour):
			controller.setDefaultDestructionInterval(0);
			controller.setMaxDestructionInterval(1, DateField.HOUR);
			Calendar maxTime = Calendar.getInstance();
			maxTime.add(Calendar.HOUR, 1);
			checkDate(maxTime.getTime(), controller.check(null));
			checkDate(defaultTime.getTime(), controller.check(defaultTime.getTime()));
			checkDate(maxTime.getTime(), controller.check(calendar.getTime()));

			// With a default (10 minutes) AND a maximum (1 hour) destruction time:
			controller.setDefaultDestructionInterval(10);
			controller.setMaxDestructionInterval(1, DateField.HOUR);
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
		DestructionTimeController controller = new DestructionTimeController();

		// By default, when nothing is set, the default destruction time is NULL (the job will never be destroyed):
		assertNull(controller.getDefault());

		// With no interval, the default destruction time should remain NULL (the job will never be destroyed):
		controller.setDefaultDestructionInterval(DestructionTimeController.NO_INTERVAL);
		assertNull(controller.getDefault());

		// With a negative interval, the destruction time should also be NULL:
		controller.setDefaultDestructionInterval(-1);
		assertNull(controller.getDefault());

		// With a destruction interval of 100 minutes:
		Calendar calendar = Calendar.getInstance();
		controller.setDefaultDestructionInterval(100);
		calendar.add(Calendar.MINUTE, 100);
		checkDate(calendar.getTime(), controller.getDefault());

		// With a destruction interval of 100 seconds:
		controller.setDefaultDestructionInterval(100, DateField.SECOND);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 100);
		checkDate(calendar.getTime(), controller.getDefault());

		// With a destruction interval of 1 week:
		controller.setDefaultDestructionInterval(7, DateField.DAY);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, 7);
		checkDate(calendar.getTime(), controller.getDefault());
	}

	@Test
	public void testGetMaxDestructionTime(){
		DestructionTimeController controller = new DestructionTimeController();

		// By default, when nothing is set, the maximum destruction time is NULL (the job will never be destroyed):
		assertNull(controller.getMaxDestructionTime());

		// With no interval, the maximum destruction time should remain NULL (the job will never be destroyed):
		controller.setMaxDestructionInterval(DestructionTimeController.NO_INTERVAL);
		assertNull(controller.getMaxDestructionTime());

		// With a negative interval, the destruction time should also be NULL:
		controller.setMaxDestructionInterval(-1);
		assertNull(controller.getMaxDestructionTime());

		// With a destruction interval of 100 minutes:
		Calendar calendar = Calendar.getInstance();
		controller.setMaxDestructionInterval(100);
		calendar.add(Calendar.MINUTE, 100);
		checkDate(calendar.getTime(), controller.getMaxDestructionTime());

		// With a destruction interval of 100 seconds:
		controller.setMaxDestructionInterval(100, DateField.SECOND);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 100);
		checkDate(calendar.getTime(), controller.getMaxDestructionTime());

		// With a destruction interval of 1 week:
		controller.setMaxDestructionInterval(7, DateField.DAY);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, 7);
		checkDate(calendar.getTime(), controller.getMaxDestructionTime());
	}

	@Test
	public void testAllowModification(){
		DestructionTimeController controller = new DestructionTimeController();

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
			Calendar cexpected = Calendar.getInstance(), cval = Calendar.getInstance();
			cexpected.setTime(expected);
			cval.setTime((Date)val);

			try{
				assertEquals(cexpected.get(Calendar.DAY_OF_MONTH), cval.get(Calendar.DAY_OF_MONTH));
				assertEquals(cexpected.get(Calendar.MONTH), cval.get(Calendar.MONTH));
				assertEquals(cexpected.get(Calendar.YEAR), cval.get(Calendar.YEAR));
				assertEquals(cexpected.get(Calendar.HOUR), cval.get(Calendar.HOUR));
				assertEquals(cexpected.get(Calendar.MINUTE), cval.get(Calendar.MINUTE));
				assertEquals(cexpected.get(Calendar.SECOND), cval.get(Calendar.SECOND));
			}catch(AssertionError e){
				fail("Expected <" + expected + "> but was <" + val + ">");
			}
		}else if (expected == null && val == null)
			return;
		else
			fail("Expected <" + expected + "> but was <" + val + ">");
	}

}
