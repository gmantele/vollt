package tap.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tap.TAPJob;

public class TestTAPExecutionDurationController {

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
		TAPExecutionDurationController controller = new TAPExecutionDurationController(service);

		try{
			// A NULL duration will always return an unlimited duration:
			assertEquals(TAPJob.UNLIMITED_DURATION, controller.check(null));
			assertEquals(TAPJob.UNLIMITED_DURATION, controller.check(0));
			assertEquals(TAPJob.UNLIMITED_DURATION, controller.check(-1));
			assertEquals(TAPJob.UNLIMITED_DURATION, controller.check(-123));

			// By default, the controller has no limit on the execution duration, so let's try with a duration of 1e6 seconds:
			assertEquals(1000000L, controller.check(1000000));

			// With just a default execution duration (of 10 minutes):
			service.setExecutionDuration(600, -1);
			assertEquals(600L, controller.check(null));
			assertEquals(TAPJob.UNLIMITED_DURATION, controller.check(-1));
			assertEquals(TAPJob.UNLIMITED_DURATION, controller.check(TAPJob.UNLIMITED_DURATION));

			// With just a maximum execution duration (of 1 hour):
			service.setExecutionDuration(-1, 3600);
			assertEquals(3600L, controller.check(null));
			assertEquals(60L, controller.check(60));
			assertEquals(3600L, controller.check(-1));
			assertEquals(3600L, controller.check(TAPJob.UNLIMITED_DURATION));
			assertEquals(3600L, controller.check(3601));

			// With a default (10 minutes) AND a maximum (1 hour) execution duration:
			service.setExecutionDuration(600, 3600);
			assertEquals(600L, controller.check(null));
			assertEquals(10L, controller.check(10));
			assertEquals(600L, controller.check(600));
			assertEquals(3600L, controller.check(3600));
			assertEquals(3600L, controller.check(-1));
			assertEquals(3600L, controller.check(TAPJob.UNLIMITED_DURATION));
			assertEquals(3600L, controller.check(3601));

		}catch(Exception t){
			t.printStackTrace();
			fail();
		}
	}

	@Test
	public void testGetDefault(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		TAPExecutionDurationController controller = new TAPExecutionDurationController(service);

		// By default, when nothing is set, the default execution duration is UNLIMITED:
		assertEquals(TAPJob.UNLIMITED_DURATION, controller.getDefault());

		// With no duration, the default execution duration should remain UNLIMITED:
		service.setExecutionDuration((int)TAPJob.UNLIMITED_DURATION, -1);
		assertEquals(TAPJob.UNLIMITED_DURATION, controller.getDefault());

		// With a negative duration, the execution duration should also be UNLIMITED:
		service.setExecutionDuration(-1, -1);
		assertEquals(TAPJob.UNLIMITED_DURATION, controller.getDefault());

		// With an execution duration of 10 minutes:
		service.setExecutionDuration(600, -1);
		assertEquals(600L, controller.getDefault());

		// The default value must always be less than the maximum value:
		service.setExecutionDuration(600, 300);
		assertEquals(300L, controller.getDefault());
	}

	@Test
	public void testGetMaxExecutionDuration(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		TAPExecutionDurationController controller = new TAPExecutionDurationController(service);

		// By default, when nothing is set, the maximum execution duration is UNLIMITED:
		assertEquals(TAPJob.UNLIMITED_DURATION, controller.getMaxDuration());

		// With no duration, the maximum execution duration should remain UNLIMITED:
		service.setExecutionDuration(-1, (int)TAPJob.UNLIMITED_DURATION);
		assertEquals(TAPJob.UNLIMITED_DURATION, controller.getMaxDuration());

		// With a negative duration, the execution duration should also be UNLIMITED:
		service.setExecutionDuration(-1, -1);
		assertEquals(TAPJob.UNLIMITED_DURATION, controller.getMaxDuration());

		// With an execution duration of 10 minutes:
		service.setExecutionDuration(-1, 600);
		assertEquals(600L, controller.getMaxDuration());
	}

	@Test
	public void testAllowModification(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		TAPExecutionDurationController controller = new TAPExecutionDurationController(service);

		// By default, user modification of the execution duration is allowed:
		assertTrue(controller.allowModification());

		controller.allowModification(true);
		assertTrue(controller.allowModification());

		controller.allowModification(false);
		assertFalse(controller.allowModification());
	}

}
