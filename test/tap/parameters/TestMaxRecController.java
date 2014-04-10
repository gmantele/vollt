package tap.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import tap.TAPJob;
import tap.config.AllTests;
import tap.config.DefaultServiceConnection;
import uws.UWSException;

public class TestMaxRecController {

	private DefaultServiceConnection serviceConn;
	private MaxRecController controller;

	@Before
	public void setUp() throws Exception{
		serviceConn = new DefaultServiceConnection(AllTests.getValidProperties());
		controller = new MaxRecController(serviceConn);
	}

	@Test
	public void testAll(){
		final int value = 100, defaultValue = 200, maxValue = 1000, biggerValue = 2000;

		// No default or max limit set => Unlimited max_rec:
		assertEquals(controller.getDefault(), TAPJob.UNLIMITED_MAX_REC);
		assertEquals(controller.getMaxOutputLimit(), TAPJob.UNLIMITED_MAX_REC);
		try{
			assertEquals(controller.check(null), TAPJob.UNLIMITED_MAX_REC);
			assertEquals(controller.check(value), value);
		}catch(Exception e){
			fail("This MUST have succeeded because no limit is set and that no value is provided! \nCaught exception: " + getPertinentMessage(e));
		}

		// Only a max limit is set => default=max:
		assertEquals(serviceConn.setMaxOutputLimit(maxValue), true);
		assertEquals(controller.getDefault(), maxValue);
		assertEquals(controller.getMaxOutputLimit(), maxValue);
		try{
			assertEquals(controller.check(null), maxValue);
			assertEquals(controller.check(value), value);
		}catch(Exception e){
			fail("This MUST have succeeded because only the maximum limit is set and the given value is less than the max value! \nCaught exception: " + getPertinentMessage(e));
		}
		try{
			controller.check(biggerValue);
			fail("This MUST have failed because the given value is bigger than the maximum one!");
		}catch(Exception e){
			assertEquals(e.getClass(), UWSException.class);
			assertEquals(((UWSException)e).getHttpErrorCode(), UWSException.BAD_REQUEST);
			assertEquals(e.getMessage(), "The TAP limits the maxRec parameter (=output limit) to maximum " + maxValue + " rows !");
		}

		// Only a default limit is set => max=unlimited:
		assertEquals(serviceConn.setDefaultOutputLimit(defaultValue), true);
		assertEquals(serviceConn.setMaxOutputLimit(TAPJob.UNLIMITED_MAX_REC), true);
		assertEquals(controller.getDefault(), defaultValue);
		assertEquals(controller.getMaxOutputLimit(), TAPJob.UNLIMITED_MAX_REC);
		try{
			assertEquals(controller.check(null), defaultValue);
			assertEquals(controller.check(value), value);
			assertEquals(controller.check(biggerValue), biggerValue);
		}catch(Exception e){
			fail("This MUST have succeeded because only the default value is set! \nCaught exception: " + getPertinentMessage(e));
		}

		// Both limits are set => default=max:
		assertEquals(serviceConn.setDefaultOutputLimit(defaultValue), true);
		assertEquals(serviceConn.setMaxOutputLimit(maxValue), true);
		assertEquals(controller.getDefault(), defaultValue);
		assertEquals(controller.getMaxOutputLimit(), maxValue);
		try{
			assertEquals(controller.check(null), defaultValue);
			assertEquals(controller.check(value), value);
		}catch(Exception e){
			fail("This MUST have succeeded because both default and max value are set and the given value is less than the max value! \nCaught exception: " + getPertinentMessage(e));
		}
		try{
			controller.check(biggerValue);
			fail("This MUST have failed because the given value is bigger than the maximum one!");
		}catch(Exception e){
			assertEquals(e.getClass(), UWSException.class);
			assertEquals(((UWSException)e).getHttpErrorCode(), UWSException.BAD_REQUEST);
			assertEquals(e.getMessage(), "The TAP limits the maxRec parameter (=output limit) to maximum " + maxValue + " rows !");
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

}
