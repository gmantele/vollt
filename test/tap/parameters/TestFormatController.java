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
import uws.UWSException;

public class TestFormatController {

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
		FormatController controller = new FormatController(service);

		try{
			assertEquals(controller.getDefault(), controller.check(null));
			assertEquals(controller.getDefault(), controller.check(""));
			assertEquals(controller.getDefault(), controller.check("   "));
			assertEquals(controller.getDefault(), controller.check("	"));
			assertEquals(controller.getDefault(), controller.check(" 	 "));
			assertEquals("votable", controller.check("votable"));
			assertEquals("application/x-votable+xml", controller.check("application/x-votable+xml"));
			assertEquals("csv", controller.check("csv"));
			assertEquals("fits", controller.check("fits"));
		}catch(Exception ex){
			ex.printStackTrace();
			fail();
		}

		try{
			controller.check("toto");
		}catch(Exception ex){
			assertTrue(ex instanceof UWSException);
			assertTrue(ex.getMessage().startsWith("Unknown value for the parameter \"format\": \"toto\". It should be "));
		}

		try{
			controller.check("application/xml");
		}catch(Exception ex){
			assertTrue(ex instanceof UWSException);
			assertTrue(ex.getMessage().startsWith("Unknown value for the parameter \"format\": \"application/xml\". It should be "));
		}
	}

	@Test
	public void testGetDefault(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		FormatController controller = new FormatController(service);

		assertEquals(TAPJob.FORMAT_VOTABLE, controller.getDefault());
	}

	@Test
	public void testAllowModification(){
		ServiceConnectionOfTest service = new ServiceConnectionOfTest();
		FormatController controller = new FormatController(service);

		// By default, user modification of the destruction time is allowed:
		assertTrue(controller.allowModification());

		controller.allowModification(true);
		assertTrue(controller.allowModification());

		controller.allowModification(false);
		assertFalse(controller.allowModification());
	}

}
