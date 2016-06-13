package uws.job.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import uws.UWSException;

public class TestNumericParamController {

	@Before
	public void setUp() throws Exception{}

	@Test
	public void testSetMinimum(){
		// Just a minimum value => no problem to change it:
		NumericParamController controller = new NumericParamController();
		controller.setMinimum(-23);
		assertEquals(-23, controller.getMinimum().intValue());
		controller.setMinimum(0);
		assertEquals(0, controller.getMinimum().intValue());

		// But if there is a maximum...
		// either the new minimum is less than the maximum => ok
		controller = new NumericParamController();
		controller.setMaximum(100);
		controller.setMinimum(20);
		assertEquals(20, controller.getMinimum());
		// or it is bigger => it must be set to the maximum:
		controller = new NumericParamController();
		controller.setMaximum(10);
		controller.setMinimum(20);
		assertEquals(10, controller.getMinimum());

		// If there is a default value and this value is smaller than the new minimum => default = min
		controller = new NumericParamController();
		controller.setDefault(10);
		controller.setMaximum(50);
		assertNull(controller.getMinimum());
		assertEquals(50, controller.getMaximum());
		assertEquals(10, controller.getDefault());
		controller.setMinimum(20);
		assertEquals(20, controller.getMinimum());
		assertEquals(50, controller.getMaximum());
		assertEquals(20, controller.getDefault());

		// NULL given => NULL set:
		controller = new NumericParamController();
		controller.setMinimum(null);
		assertNull(controller.getMinimum());
		controller.setMinimum(23);
		assertEquals(23, controller.getMinimum());
		controller.setMinimum(null);
		assertNull(controller.getMinimum());
	}

	@Test
	public void testSetMaximum(){
		// Just a minimum value => no problem to change it:
		NumericParamController controller = new NumericParamController();
		controller.setMaximum(23);
		assertEquals(23, controller.getMaximum().intValue());
		controller.setMaximum(0);
		assertEquals(0, controller.getMaximum().intValue());

		// But if there is a minimum...
		// either the new maximum is bigger than the minimum => ok
		controller = new NumericParamController();
		controller.setMinimum(10);
		controller.setMaximum(20);
		assertEquals(20, controller.getMaximum());
		// or it is smaller => it must be set to the minimum:
		controller = new NumericParamController();
		controller.setMinimum(20);
		controller.setMaximum(0);
		assertEquals(20, controller.getMaximum());

		// If there is a default value and this value is bigger than the new maximum => default = max
		controller = new NumericParamController();
		controller.setDefault(50);
		controller.setMinimum(20);
		assertEquals(20, controller.getMinimum());
		assertNull(controller.getMaximum());
		assertEquals(50, controller.getDefault());
		controller.setMaximum(30);
		assertEquals(20, controller.getMinimum());
		assertEquals(30, controller.getMaximum());
		assertEquals(30, controller.getDefault());

		// NULL given => NULL set:
		controller = new NumericParamController();
		controller.setMaximum(null);
		assertNull(controller.getMaximum());
		controller.setMaximum(23);
		assertEquals(23, controller.getMaximum());
		controller.setMaximum(null);
		assertNull(controller.getMaximum());
	}

	@Test
	public void testSetDefault(){
		// No limit => everything is allowed for the default value:
		NumericParamController controller = new NumericParamController();
		controller.setDefault(23);
		assertEquals(23, ((Number)controller.getDefault()).intValue());
		controller.setDefault(-123);
		assertEquals(-123, ((Number)controller.getDefault()).intValue());

		// If there is a maximum...
		// either default is smaller (or equals) => ok
		controller = new NumericParamController();
		controller.setMaximum(50);
		controller.setDefault(20);
		assertEquals(20, ((Number)controller.getDefault()).intValue());
		controller.setDefault(50);
		assertEquals(50, ((Number)controller.getDefault()).intValue());
		controller.setDefault(-123);
		assertEquals(-123, ((Number)controller.getDefault()).intValue());
		// or default is bigger => default = max
		controller = new NumericParamController();
		controller.setMaximum(50);
		controller.setDefault(70);
		assertEquals(50, ((Number)controller.getDefault()).intValue());

		// If there is a minimum...
		// either default is bigger (or equals) => ok
		controller = new NumericParamController();
		controller.setMinimum(10);
		controller.setDefault(20);
		assertEquals(20, ((Number)controller.getDefault()).intValue());
		controller.setDefault(10);
		assertEquals(10, ((Number)controller.getDefault()).intValue());
		controller.setDefault(123);
		assertEquals(123, ((Number)controller.getDefault()).intValue());
		// or default is smaller => default = min
		controller = new NumericParamController();
		controller.setMinimum(10);
		controller.setDefault(0);
		assertEquals(10, ((Number)controller.getDefault()).intValue());

		// NULL given => NULL set:
		controller = new NumericParamController();
		controller.setDefault(null);
		assertNull(controller.getDefault());
		controller.setDefault(23);
		assertEquals(23, controller.getDefault());
		controller.setDefault(null);
		assertNull(controller.getDefault());
	}

	@Test
	public void testReset(){
		// Just a minimum:
		NumericParamController controller = new NumericParamController();
		controller.reset(null, 23, null);
		assertNull(controller.getDefault());
		assertNull(controller.getMaximum());
		assertEquals(23, controller.getMinimum());

		// Just a maximum:
		controller.reset(null, null, 23);
		assertNull(controller.getDefault());
		assertNull(controller.getMinimum());
		assertEquals(23, controller.getMaximum());

		// Just a default:
		controller.reset(23, null, null);
		assertNull(controller.getMinimum());
		assertNull(controller.getMaximum());
		assertEquals(23, controller.getDefault());

		// min < default < max
		controller.reset(20, 10, 30);
		assertEquals(10, controller.getMinimum());
		assertEquals(30, controller.getMaximum());
		assertEquals(20, controller.getDefault());

		// default < min < max
		controller.reset(0, 10, 30);
		assertEquals(10, controller.getMinimum());
		assertEquals(30, controller.getMaximum());
		assertEquals(10, controller.getDefault());

		// default > max > min
		controller.reset(50, 10, 30);
		assertEquals(10, controller.getMinimum());
		assertEquals(30, controller.getMaximum());
		assertEquals(30, controller.getDefault());
	}

	@Test
	public void testCheck(){
		// min < value < max  => value
		NumericParamController controller = new NumericParamController();
		controller.reset(20, 10, 30);
		try{
			assertEquals(15, controller.check(15));
		}catch(UWSException e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// No value => default
		controller = new NumericParamController();
		controller.reset(20, 10, 30);
		try{
			assertEquals(20, controller.check(null));
		}catch(UWSException e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// value < min => min
		controller = new NumericParamController();
		controller.reset(20, 10, 30);
		try{
			assertEquals(10, controller.check(0));
		}catch(UWSException e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

		// value > max => max
		controller = new NumericParamController();
		controller.reset(20, 10, 30);
		try{
			assertEquals(30, controller.check(50));
		}catch(UWSException e){
			e.printStackTrace(System.err);
			fail("Unexpected error! See the console for more details about the error.");
		}

	}

}
