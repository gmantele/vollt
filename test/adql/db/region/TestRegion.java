package adql.db.region;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.parser.ADQLParser;
import adql.parser.grammar.ParseException;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NegativeOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.Operation;
import adql.query.operand.OperationType;
import adql.query.operand.StringConstant;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

public class TestRegion {

	@Test
	public void buildRegion() {
		// Special values:
		try {
			new Region((GeometryFunction)null);
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof NullPointerException);
			assertEquals("Missing geometry to convert into STCS.Region!", e.getMessage());
		}

		try {
			new Region((Region)null);
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof NullPointerException);
			assertEquals("Missing region to NOT select!", e.getMessage());
		}

		try {
			new Region(new ContainsFunction(new GeometryValue<GeometryFunction>(new RegionFunction(new StringConstant("position 1 2"))), new GeometryValue<GeometryFunction>(new RegionFunction(new StringConstant("circle 0 1 4")))));
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
			assertEquals("Unknown region type! Only geometrical function PointFunction, CircleFunction, BoxFunction, PolygonFunction and RegionFunction are allowed.", e.getMessage());
		}

		// Allowed values (1 test for each type of region):
		try {
			Region r = new Region(new PointFunction(new StringConstant(""), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3))));
			assertEquals(Region.RegionType.POSITION, r.type);
			assertEquals("", r.coordSys.toSTCS());
			assertEquals(1, r.coordinates.length);
			assertEquals(2, r.coordinates[0].length);
			assertEquals(1.2, r.coordinates[0][0], 0);
			assertEquals(-2.3, r.coordinates[0][1], 0);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertNull(r.regions);
			assertEquals("POSITION 1.2 -2.3", r.toSTCS());
			assertEquals("1.2 -2.3", r.toDALI());
			assertEquals(r.toDALI(), r.toString());

			r = new Region(new CircleFunction(new StringConstant("ICRS"), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3)), new NumericConstant(5)));
			assertEquals(Region.RegionType.CIRCLE, r.type);
			assertEquals("ICRS", r.coordSys.toSTCS());
			assertEquals(1, r.coordinates.length);
			assertEquals(2, r.coordinates[0].length);
			assertEquals(1.2, r.coordinates[0][0], 0);
			assertEquals(-2.3, r.coordinates[0][1], 0);
			assertEquals(5, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertNull(r.regions);
			assertEquals("CIRCLE ICRS 1.2 -2.3 5.0", r.toSTCS());
			assertEquals("1.2 -2.3 5.0", r.toDALI());
			assertEquals(r.toSTCS(), r.toString()); // STC/s expected because there is a specified coord. sys.

			r = new Region(new BoxFunction(new StringConstant("ICRS heliocenter"), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3)), new NumericConstant(5), new NumericConstant(4.6)));
			assertEquals(Region.RegionType.BOX, r.type);
			assertEquals("ICRS HELIOCENTER", r.coordSys.toSTCS());
			assertEquals(1, r.coordinates.length);
			assertEquals(2, r.coordinates[0].length);
			assertEquals(1.2, r.coordinates[0][0], 0);
			assertEquals(-2.3, r.coordinates[0][1], 0);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(5, r.width, 0);
			assertEquals(4.6, r.height, 0);
			assertNull(r.regions);
			assertEquals("BOX ICRS HELIOCENTER 1.2 -2.3 5.0 4.6", r.toSTCS());
			assertNull(r.toDALI()); // because BOX not supported
			assertEquals(r.toSTCS(), r.toString()); // STC/s expected because BOX not supported

			r = new Region(new PolygonFunction(new StringConstant("cartesian2"), new ADQLOperand[]{ new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3)), new NumericConstant(5), new NumericConstant(4.6), new NegativeOperand(new NumericConstant(.89)), new NumericConstant(1) }));
			assertEquals(Region.RegionType.POLYGON, r.type);
			assertEquals("CARTESIAN2", r.coordSys.toSTCS());
			assertEquals(3, r.coordinates.length);
			assertEquals(2, r.coordinates[0].length);
			assertEquals(1.2, r.coordinates[0][0], 0);
			assertEquals(-2.3, r.coordinates[0][1], 0);
			assertEquals(5, r.coordinates[1][0], 0);
			assertEquals(4.6, r.coordinates[1][1], 0);
			assertEquals(-0.89, r.coordinates[2][0], 0);
			assertEquals(1, r.coordinates[2][1], 0);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertNull(r.regions);
			assertEquals("POLYGON CARTESIAN2 1.2 -2.3 5.0 4.6 -0.89 1.0", r.toSTCS());
			assertEquals("1.2 -2.3 5.0 4.6 -0.89 1.0", r.toDALI());
			assertEquals(r.toSTCS(), r.toString()); // STC/s expected because specific coord. sys.

			r = new Region(new RegionFunction(new StringConstant("position ICrs 1.2 -2.3")));
			assertEquals(Region.RegionType.POSITION, r.type);
			assertEquals("ICRS", r.coordSys.toSTCS());
			assertEquals(1, r.coordinates.length);
			assertEquals(2, r.coordinates[0].length);
			assertEquals(1.2, r.coordinates[0][0], 0);
			assertEquals(-2.3, r.coordinates[0][1], 0);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertNull(r.regions);
			assertEquals("POSITION ICRS 1.2 -2.3", r.toSTCS());
			assertEquals("1.2 -2.3", r.toDALI());
			assertEquals(r.toSTCS(), r.toString()); // STC/s expected because specific coord.sys.

			r = new Region(new RegionFunction(new StringConstant("Union ICRS (Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)")));
			assertEquals(Region.RegionType.UNION, r.type);
			assertEquals("ICRS", r.coordSys.toSTCS());
			assertNull(r.coordinates);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertEquals(2, r.regions.length);
			assertEquals("UNION ICRS (POLYGON 1.0 4.0 2.0 4.0 2.0 5.0 1.0 5.0 POLYGON 3.0 4.0 4.0 4.0 4.0 5.0 3.0 5.0)", r.toSTCS());
			assertNull(r.toDALI());
			assertEquals(r.toSTCS(), r.toString()); // STC/s expected because UNION not supported by DALI
			// inner region 1
			Region innerR = r.regions[0];
			assertEquals(Region.RegionType.POLYGON, innerR.type);
			assertEquals("", innerR.coordSys.toSTCS());
			assertEquals(4, innerR.coordinates.length);
			assertEquals(2, innerR.coordinates[0].length);
			assertEquals(1, innerR.coordinates[0][0], 0);
			assertEquals(4, innerR.coordinates[0][1], 0);
			assertEquals(2, innerR.coordinates[1][0], 0);
			assertEquals(4, innerR.coordinates[1][1], 0);
			assertEquals(2, innerR.coordinates[2][0], 0);
			assertEquals(5, innerR.coordinates[2][1], 0);
			assertEquals(1, innerR.coordinates[3][0], 0);
			assertEquals(5, innerR.coordinates[3][1], 0);
			assertEquals(Double.NaN, innerR.radius, 0);
			assertEquals(Double.NaN, innerR.width, 0);
			assertEquals(Double.NaN, innerR.height, 0);
			assertNull(innerR.regions);
			assertEquals("POLYGON 1.0 4.0 2.0 4.0 2.0 5.0 1.0 5.0", innerR.toSTCS());
			assertEquals("1.0 4.0 2.0 4.0 2.0 5.0 1.0 5.0", innerR.toDALI());
			assertEquals(innerR.toDALI(), innerR.toString());
			// inner region 2
			innerR = r.regions[1];
			assertEquals(Region.RegionType.POLYGON, innerR.type);
			assertEquals("", innerR.coordSys.toSTCS());
			assertEquals(4, innerR.coordinates.length);
			assertEquals(2, innerR.coordinates[0].length);
			assertEquals(3, innerR.coordinates[0][0], 0);
			assertEquals(4, innerR.coordinates[0][1], 0);
			assertEquals(4, innerR.coordinates[1][0], 0);
			assertEquals(4, innerR.coordinates[1][1], 0);
			assertEquals(4, innerR.coordinates[2][0], 0);
			assertEquals(5, innerR.coordinates[2][1], 0);
			assertEquals(3, innerR.coordinates[3][0], 0);
			assertEquals(5, innerR.coordinates[3][1], 0);
			assertEquals(Double.NaN, innerR.radius, 0);
			assertEquals(Double.NaN, innerR.width, 0);
			assertEquals(Double.NaN, innerR.height, 0);
			assertNull(innerR.regions);
			assertEquals("POLYGON 3.0 4.0 4.0 4.0 4.0 5.0 3.0 5.0", innerR.toSTCS());
			assertEquals("3.0 4.0 4.0 4.0 4.0 5.0 3.0 5.0", innerR.toDALI());
			assertEquals(innerR.toDALI(), innerR.toString());

			r = new Region(new RegionFunction(new StringConstant("NOT(CIRCLE ICRS 1.2 -2.3 5)")));
			assertEquals(Region.RegionType.NOT, r.type);
			assertNull(r.coordSys);
			assertNull(r.coordinates);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertEquals(1, r.regions.length);
			assertEquals("NOT(CIRCLE ICRS 1.2 -2.3 5.0)", r.toSTCS());
			assertNull(r.toDALI());
			assertEquals(r.toSTCS(), r.toString()); // STC/s expected because NOT not supported by DALI
			// inner region
			innerR = r.regions[0];
			assertEquals(Region.RegionType.CIRCLE, innerR.type);
			assertEquals("ICRS", innerR.coordSys.toSTCS());
			assertEquals(1, innerR.coordinates.length);
			assertEquals(2, innerR.coordinates[0].length);
			assertEquals(1.2, innerR.coordinates[0][0], 0);
			assertEquals(-2.3, innerR.coordinates[0][1], 0);
			assertEquals(5, innerR.radius, 0);
			assertEquals(Double.NaN, innerR.width, 0);
			assertEquals(Double.NaN, innerR.height, 0);
			assertNull(innerR.regions);
			assertEquals("CIRCLE ICRS 1.2 -2.3 5.0", innerR.toSTCS());
			assertEquals("1.2 -2.3 5.0", innerR.toDALI());
			assertEquals(innerR.toSTCS(), innerR.toString()); // STC/s expected because specific coord.sys.
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail();
		}

		// Test with incorrect syntaxes:
		try {
			new Region(new PointFunction(new StringConstant(""), new StringConstant("1.2"), new NegativeOperand(new NumericConstant(2.3))));
			fail("The first coordinate is a StringConstant rather than a NumericConstant!");
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non numeric argument (including ADQLColumn and Operation)!", e.getMessage());
		}
		try {
			new Region(new PointFunction(new NumericConstant(.65), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3))));
			fail("The coordinate system is a NumericConstant rather than a StringConstant!");
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("A coordinate system must be a string literal: \"0.65\" is not a string operand!", e.getMessage());
		}
		try {
			new Region(new PointFunction(new StringConstant(""), null, new NegativeOperand(new NumericConstant(2.3))));
			fail("The first coordinate is missing!");
		} catch(Exception e) {
			assertTrue(e instanceof NullPointerException);
			assertEquals("The POINT function must have non-null coordinates!", e.getMessage());
		}
		try {
			new Region(new RegionFunction(new StringConstant("")));
			fail("Missing STC-S expression!");
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Unsupported region serialization!", e.getMessage());
		}
		try {
			new Region(new RegionFunction(new StringConstant("MyRegion HERE 1.2")));
			fail("Totally incorrect region type!");
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Unsupported region serialization!", e.getMessage());
		}
		try {
			ADQLOperand concat = (new ADQLParser()).parseSelect("SELECT 'POSITION ' || coordinateSys || ' ' || ra || ' ' || dec").get(0).getOperand();
			new Region(new RegionFunction(concat));
			fail("String concatenation can not be managed!");
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non string argument (including ADQLColumn and Concatenation)!", e.getMessage());
		}
		try {
			new Region(new PointFunction(new ADQLColumn("coordSys"), new NumericConstant(1), new NumericConstant(2)));
			fail("Columns can not be managed!");
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non string argument (including ADQLColumn and Concatenation)!", e.getMessage());
		}
		try {
			new Region(new PointFunction(new StringConstant("ICRS"), new Operation(new NumericConstant(2), OperationType.MULT, new NumericConstant(5)), new NumericConstant(2)));
			fail("Operations can not be managed!");
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non numeric argument (including ADQLColumn and Operation)!", e.getMessage());
		}
	}

}
