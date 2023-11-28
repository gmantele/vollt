package uws.job.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uws.job.UWSJob;

public class TestExecutionDurationController {

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
		ExecutionDurationController controller = new ExecutionDurationController();

		try{
			// A NULL duration will always return an unlimited duration:
			assertEquals(UWSJob.UNLIMITED_DURATION, controller.check(null));
			assertEquals(UWSJob.UNLIMITED_DURATION, controller.check(0));
			assertEquals(UWSJob.UNLIMITED_DURATION, controller.check(-1));
			assertEquals(UWSJob.UNLIMITED_DURATION, controller.check(-123));

			// By default, the controller has no limit on the execution duration, so let's try with a duration of 1e6 seconds:
			assertEquals(1000000L, controller.check(1000000));

			// With just a default execution duration (of 10 minutes):
			controller.setDefaultExecutionDuration(600);
			assertEquals(600L, controller.check(null));
			assertEquals(UWSJob.UNLIMITED_DURATION, controller.check(-1));
			assertEquals(UWSJob.UNLIMITED_DURATION, controller.check(UWSJob.UNLIMITED_DURATION));

			// With just a maximum execution duration (of 1 hour):
			controller.setDefaultExecutionDuration(-1);
			controller.setMaxExecutionDuration(3600);
			assertEquals(3600L, controller.check(null));
			assertEquals(60L, controller.check(60));
			assertEquals(3600L, controller.check(-1));
			assertEquals(3600L, controller.check(UWSJob.UNLIMITED_DURATION));
			assertEquals(3600L, controller.check(3601));

			// With a default (10 minutes) AND a maximum (1 hour) execution duration:
			controller.setDefaultExecutionDuration(600);
			controller.setMaxExecutionDuration(3600);
			assertEquals(600L, controller.check(null));
			assertEquals(10L, controller.check(10));
			assertEquals(600L, controller.check(600));
			assertEquals(3600L, controller.check(3600));
			assertEquals(3600L, controller.check(-1));
			assertEquals(3600L, controller.check(UWSJob.UNLIMITED_DURATION));
			assertEquals(3600L, controller.check(3601));

		}catch(Exception t){
			t.printStackTrace();
			fail();
		}
	}

	@Test
	public void testGetDefault(){
		ExecutionDurationController controller = new ExecutionDurationController();

		// By default, when nothing is set, the default execution duration is UNLIMITED:
		assertEquals(UWSJob.UNLIMITED_DURATION, controller.getDefault());

		// With no duration, the default execution duration should remain UNLIMITED:
		controller.setDefaultExecutionDuration(UWSJob.UNLIMITED_DURATION);
		assertEquals(UWSJob.UNLIMITED_DURATION, controller.getDefault());

		// With a negative duration, the execution duration should also be UNLIMITED:
		controller.setDefaultExecutionDuration(-1);
		assertEquals(UWSJob.UNLIMITED_DURATION, controller.getDefault());

		// With an execution duration of 10 minutes:
		controller.setDefaultExecutionDuration(600);
		assertEquals(600L, controller.getDefault());

		// The default value must always be less than the maximum value:
		controller.setMaxExecutionDuration(300);
		assertEquals(300L, controller.getDefault());
	}

	@Test
	public void testGetMaxExecutionDuration(){
		ExecutionDurationController controller = new ExecutionDurationController();

		// By default, when nothing is set, the maximum execution duration is UNLIMITED:
		assertEquals(UWSJob.UNLIMITED_DURATION, controller.getMaxExecutionDuration());

		// With no duration, the maximum execution duration should remain UNLIMITED:
		controller.setMaxExecutionDuration(UWSJob.UNLIMITED_DURATION);
		assertEquals(UWSJob.UNLIMITED_DURATION, controller.getMaxExecutionDuration());

		// With a negative duration, the execution duration should also be UNLIMITED:
		controller.setMaxExecutionDuration(-1);
		assertEquals(UWSJob.UNLIMITED_DURATION, controller.getMaxExecutionDuration());

		// With an execution duration of 10 minutes:
		controller.setMaxExecutionDuration(600);
		assertEquals(600L, controller.getMaxExecutionDuration());
	}

	@Test
	public void testAllowModification(){
		ExecutionDurationController controller = new ExecutionDurationController();

		// By default, user modification of the execution duration is allowed:
		assertTrue(controller.allowModification());

		controller.allowModification(true);
		assertTrue(controller.allowModification());

		controller.allowModification(false);
		assertFalse(controller.allowModification());
	}

}
