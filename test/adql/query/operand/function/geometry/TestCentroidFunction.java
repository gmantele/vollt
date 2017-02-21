package adql.query.operand.function.geometry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;

public class TestCentroidFunction {

	@Test
	public void testIsGeometry(){
		try{
			CentroidFunction centfc = new CentroidFunction(new GeometryValue<GeometryFunction>(new CircleFunction(new StringConstant("ICRS"), new NumericConstant(128.23), new NumericConstant(0.53), new NumericConstant(2))));
			assertTrue(centfc.isGeometry());
			assertFalse(centfc.isNumeric());
			assertFalse(centfc.isString());
		}catch(Throwable t){
			t.printStackTrace(System.err);
			fail("An error occured while building a simple CentroidFunction! (see the console for more details)");
		}
	}

}
