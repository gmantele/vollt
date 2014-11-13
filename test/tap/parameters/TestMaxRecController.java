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

public class TestMaxRecController {

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
		MaxRecController controller = new MaxRecController(service);

		try{
			// A NULL limit will always return an unlimited duration:
			assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.check(null));
			assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.check(-1));
			assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.check(-123));

			// A 0 value, means that only the metadata of the result must be returned (without executing the query);
			// this value should stay like that:
			assertEquals(0, controller.check(0));

			// By default, the controller has no limit on the output limit, so let's try with a limit of 1000000 rows:
			assertEquals(1000000, controller.check(1000000));

			// With just a default output limit (of 100 rows):
			service.setOutputLimit(100, -1);
			assertEquals(100, controller.check(null));
			assertEquals(0, controller.check(0));
			assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.check(-1));
			assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.check(TAPJob.UNLIMITED_MAX_REC));

			// With just a maximum output limit (of 10000 rows):
			service.setOutputLimit(-1, 10000);
			assertEquals(10000, controller.check(null));
			assertEquals(0, controller.check(0));
			assertEquals(60, controller.check(60));
			assertEquals(10000, controller.check(-1));
			assertEquals(10000, controller.check(TAPJob.UNLIMITED_MAX_REC));
			assertEquals(10000, controller.check(10001));

			// With a default (100 rows) AND a maximum (10000 rows) output limit:
			service.setOutputLimit(100, 10000);
			assertEquals(100, controller.check(null));
			assertEquals(0, controller.check(0));
			assertEquals(10, controller.check(10));
			assertEquals(600, controller.check(600));
			assertEquals(10000, controller.check(10000));
			assertEquals(10000, controller.check(-1));
			assertEquals(10000, controller.check(TAPJob.UNLIMITED_MAX_REC));
			assertEquals(10000, controller.check(10001));

		}catch(Exception t){
			t.printStackTrace();
			fail();
		}
	}

	@Test
	public void testGetDefault(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		MaxRecController controller = new MaxRecController(service);

		// By default, when nothing is set, the default output limit is UNLIMITED:
		assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.getDefault());

		// With no duration, the default output limit should remain UNLIMITED:
		service.setOutputLimit(TAPJob.UNLIMITED_MAX_REC, -1);
		assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.getDefault());

		// With a negative limit, the output limit should also be UNLIMITED:
		service.setOutputLimit(-1, -1);
		assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.getDefault());

		// With an output limit of 100 rows:
		service.setOutputLimit(100, -1);
		assertEquals(100, controller.getDefault());

		// The default value must always be less than the maximum value:
		service.setOutputLimit(600, 300);
		assertEquals(300, controller.getDefault());
	}

	@Test
	public void testGetMaxExecutionDuration(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		MaxRecController controller = new MaxRecController(service);

		// By default, when nothing is set, the maximum output limit is UNLIMITED:
		assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.getMaxOutputLimit());

		// With no duration, the maximum output limit should remain UNLIMITED:
		service.setOutputLimit(-1, TAPJob.UNLIMITED_MAX_REC);
		assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.getMaxOutputLimit());

		// With a negative limit, the output limit should also be UNLIMITED:
		service.setOutputLimit(-1, -1);
		assertEquals(TAPJob.UNLIMITED_MAX_REC, controller.getMaxOutputLimit());

		// With an output limit of 10000 rows:
		service.setOutputLimit(-1, 10000);
		assertEquals(10000, controller.getMaxOutputLimit());
	}

	@Test
	public void testAllowModification(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		MaxRecController controller = new MaxRecController(service);

		// By default, user modification of the destruction time is allowed:
		assertTrue(controller.allowModification());

		controller.allowModification(true);
		assertTrue(controller.allowModification());

		controller.allowModification(false);
		assertFalse(controller.allowModification());
	}

}
