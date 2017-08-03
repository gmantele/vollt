package adql.db;

import adql.db.STCS.*;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.query.operand.*;
import adql.query.operand.function.geometry.*;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;
import org.junit.*;

import java.io.StringBufferInputStream;

import static org.junit.Assert.*;

@SuppressWarnings("deprecation")
public class TestSTCS {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{}

	@Before
	public void setUp() throws Exception{}

	@After
	public void tearDown() throws Exception{}

	@Test
	public void buildRegion(){
		// Special values:
		try{
			new Region((GeometryFunction)null);
			fail();
		}catch(Exception e){
			assertTrue(e instanceof NullPointerException);
			assertEquals("Missing geometry to convert into STCS.Region!", e.getMessage());
		}

		try{
			new Region((Region)null);
			fail();
		}catch(Exception e){
			assertTrue(e instanceof NullPointerException);
			assertEquals("Missing region to NOT select!", e.getMessage());
		}

		try{
			new Region(new ContainsFunction(new GeometryValue<GeometryFunction>(new RegionFunction(new StringConstant("position 1 2"))), new GeometryValue<GeometryFunction>(new RegionFunction(new StringConstant("circle 0 1 4")))));
			fail();
		}catch(Exception e){
			assertTrue(e instanceof IllegalArgumentException);
			assertEquals("Unknown region type! Only geometrical function PointFunction, CircleFunction, BoxFunction, PolygonFunction and RegionFunction are allowed.", e.getMessage());
		}

		// Allowed values (1 test for each type of region):
		try{
			Region r = new Region(new PointFunction(new StringConstant(""), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3))));
			assertEquals(RegionType.POSITION, r.type);
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

			r = new Region(new CircleFunction(new StringConstant("ICRS"), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3)), new NumericConstant(5)));
			assertEquals(RegionType.CIRCLE, r.type);
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

			r = new Region(new BoxFunction(new StringConstant("ICRS heliocenter"), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3)), new NumericConstant(5), new NumericConstant(4.6)));
			assertEquals(RegionType.BOX, r.type);
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

			r = new Region(new PolygonFunction(new StringConstant("cartesian2"), new ADQLOperand[]{new NumericConstant(1.2),new NegativeOperand(new NumericConstant(2.3)),new NumericConstant(5),new NumericConstant(4.6),new NegativeOperand(new NumericConstant(.89)),new NumericConstant(1)}));
			assertEquals(RegionType.POLYGON, r.type);
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

			r = new Region(new RegionFunction(new StringConstant("position ICrs 1.2 -2.3")));
			assertEquals(RegionType.POSITION, r.type);
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

			r = new Region(new RegionFunction(new StringConstant("Union ICRS (Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)")));
			assertEquals(RegionType.UNION, r.type);
			assertEquals("ICRS", r.coordSys.toSTCS());
			assertNull(r.coordinates);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertEquals(2, r.regions.length);
			assertEquals("UNION ICRS (POLYGON 1.0 4.0 2.0 4.0 2.0 5.0 1.0 5.0 POLYGON 3.0 4.0 4.0 4.0 4.0 5.0 3.0 5.0)", r.toString());
			// inner region 1
			Region innerR = r.regions[0];
			assertEquals(RegionType.POLYGON, innerR.type);
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
			// inner region 2
			innerR = r.regions[1];
			assertEquals(RegionType.POLYGON, innerR.type);
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

			r = new Region(new RegionFunction(new StringConstant("NOT(CIRCLE ICRS 1.2 -2.3 5)")));
			assertEquals(RegionType.NOT, r.type);
			assertNull(r.coordSys);
			assertNull(r.coordinates);
			assertEquals(Double.NaN, r.radius, 0);
			assertEquals(Double.NaN, r.width, 0);
			assertEquals(Double.NaN, r.height, 0);
			assertEquals(1, r.regions.length);
			assertEquals("NOT(CIRCLE ICRS 1.2 -2.3 5.0)", r.toSTCS());
			// inner region
			innerR = r.regions[0];
			assertEquals(RegionType.CIRCLE, innerR.type);
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
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}

		// Test with incorrect syntaxes:
		try{
			new Region(new PointFunction(new StringConstant(""), new StringConstant("1.2"), new NegativeOperand(new NumericConstant(2.3))));
			fail("The first coordinate is a StringConstant rather than a NumericConstant!");
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non numeric argument (including ADQLColumn and Operation)!", e.getMessage());
		}
		try{
			new Region(new PointFunction(new NumericConstant(.65), new NumericConstant(1.2), new NegativeOperand(new NumericConstant(2.3))));
			fail("The coordinate system is a NumericConstant rather than a StringConstant!");
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("A coordinate system must be a string literal: \"0.65\" is not a string operand!", e.getMessage());
		}
		try{
			new Region(new PointFunction(new StringConstant(""), null, new NegativeOperand(new NumericConstant(2.3))));
			fail("The first coordinate is missing!");
		}catch(Exception e){
			assertTrue(e instanceof NullPointerException);
			assertEquals("The POINT function must have non-null coordinates!", e.getMessage());
		}
		try{
			new Region(new RegionFunction(new StringConstant("")));
			fail("Missing STC-S expression!");
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Missing STC-S expression to parse!", e.getMessage());
		}
		try{
			new Region(new RegionFunction(new StringConstant("MyRegion HERE 1.2")));
			fail("Totally incorrect region type!");
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Unknown STC region type: \"MYREGION\"!", e.getMessage());
		}
		try{
			new Region(new RegionFunction((new ADQLParser(new StringBufferInputStream("'POSITION ' || coordinateSys || ' ' || ra || ' ' || dec"))).StringExpression()));
			fail("String concatenation can not be managed!");
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non string argument (including ADQLColumn and Concatenation)!", e.getMessage());
		}
		try{
			new Region(new PointFunction(new ADQLColumn("coordSys"), new NumericConstant(1), new NumericConstant(2)));
			fail("Columns can not be managed!");
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non string argument (including ADQLColumn and Concatenation)!", e.getMessage());
		}
		try{
			new Region(new PointFunction(new StringConstant("ICRS"), new Operation(new NumericConstant(2), OperationType.MULT, new NumericConstant(5)), new NumericConstant(2)));
			fail("Operations can not be managed!");
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Can not convert into STC-S a non numeric argument (including ADQLColumn and Operation)!", e.getMessage());
		}
	}

	@Test
	public void parseCoordSys(){
		// GOOD SYNTAXES:
		try{
			CoordSys p;

			// Default coordinate system (should be then interpreted as local coordinate system):
			for(String s : new String[]{null,"","  	"}){
				p = STCS.parseCoordSys(s);
				assertEquals(Frame.UNKNOWNFRAME, p.frame);
				assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
				assertEquals(Flavor.SPHERICAL2, p.flavor);
				assertTrue(p.isDefault());
			}

			// Just a frame:
			p = STCS.parseCoordSys("ICRS");
			assertEquals(Frame.ICRS, p.frame);
			assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
			assertEquals(Flavor.SPHERICAL2, p.flavor);
			assertFalse(p.isDefault());

			// Just a reference position:
			p = STCS.parseCoordSys("LSR");
			assertEquals(Frame.UNKNOWNFRAME, p.frame);
			assertEquals(RefPos.LSR, p.refpos);
			assertEquals(Flavor.SPHERICAL2, p.flavor);
			assertFalse(p.isDefault());

			// Just a flavor:
			p = STCS.parseCoordSys("CARTESIAN2");
			assertEquals(Frame.UNKNOWNFRAME, p.frame);
			assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
			assertEquals(Flavor.CARTESIAN2, p.flavor);
			assertFalse(p.isDefault());

			// Frame + RefPos:
			p = STCS.parseCoordSys("ICRS LSR");
			assertEquals(Frame.ICRS, p.frame);
			assertEquals(RefPos.LSR, p.refpos);
			assertEquals(Flavor.SPHERICAL2, p.flavor);
			assertFalse(p.isDefault());

			// Frame + Flavor:
			p = STCS.parseCoordSys("ICRS SPHERICAL2");
			assertEquals(Frame.ICRS, p.frame);
			assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
			assertEquals(Flavor.SPHERICAL2, p.flavor);
			assertFalse(p.isDefault());

			// RefPos + Flavor:
			p = STCS.parseCoordSys("HELIOCENTER SPHERICAL2");
			assertEquals(Frame.UNKNOWNFRAME, p.frame);
			assertEquals(RefPos.HELIOCENTER, p.refpos);
			assertEquals(Flavor.SPHERICAL2, p.flavor);
			assertFalse(p.isDefault());

			// Frame + RefPos + Flavor
			p = STCS.parseCoordSys("ICRS GEOCENTER SPHERICAL2");
			assertEquals(Frame.ICRS, p.frame);
			assertEquals(RefPos.GEOCENTER, p.refpos);
			assertEquals(Flavor.SPHERICAL2, p.flavor);
			assertFalse(p.isDefault());

			// Lets try in a different case:
			p = STCS.parseCoordSys("icrs Geocenter SpheriCAL2");
			assertEquals(Frame.ICRS, p.frame);
			assertEquals(RefPos.GEOCENTER, p.refpos);
			assertEquals(Flavor.SPHERICAL2, p.flavor);
			assertFalse(p.isDefault());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}

		// WRONG SYNTAXES:
		try{
			STCS.parseCoordSys("HOME");
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Incorrect syntax: \"HOME\" was unexpected! Expected syntax: \"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used.", e.getMessage());
		}

		// With wrong reference position:
		try{
			STCS.parseCoordSys("ICRS HOME SPHERICAL2");
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Incorrect syntax: \"HOME SPHERICAL2\" was unexpected! Expected syntax: \"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used.", e.getMessage());
		}

		// With a cartesian flavor:
		try{
			STCS.parseCoordSys("ICRS CARTESIAN2");
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("a coordinate system expressed with a cartesian flavor MUST have an UNKNOWNFRAME and UNKNOWNREFPOS!", e.getMessage());
		}
		try{
			STCS.parseCoordSys("LSR CARTESIAN3");
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("a coordinate system expressed with a cartesian flavor MUST have an UNKNOWNFRAME and UNKNOWNREFPOS!", e.getMessage());
		}
		try{
			CoordSys p = STCS.parseCoordSys("CARTESIAN2");
			assertEquals(Frame.UNKNOWNFRAME, p.frame);
			assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
			assertEquals(Flavor.CARTESIAN2, p.flavor);

			p = STCS.parseCoordSys("CARTESIAN3");
			assertEquals(Frame.UNKNOWNFRAME, p.frame);
			assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
			assertEquals(Flavor.CARTESIAN3, p.flavor);
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}

		// Without spaces:
		try{
			STCS.parseCoordSys("icrsGeocentercarteSIAN2");
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Incorrect syntax: \"icrsGeocentercarteSIAN2\" was unexpected! Expected syntax: \"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used.", e.getMessage());
		}
	}

	@Test
	public void serializeCoordSys(){
		try{
			assertEquals("", STCS.toSTCS((CoordSys)null));

			assertEquals("", STCS.toSTCS(new CoordSys()));

			assertEquals("", STCS.toSTCS(new CoordSys(null, null, null)));
			assertEquals("", STCS.toSTCS(new CoordSys(Frame.DEFAULT, RefPos.DEFAULT, Flavor.DEFAULT)));
			assertEquals("", STCS.toSTCS(new CoordSys(Frame.UNKNOWNFRAME, RefPos.UNKNOWNREFPOS, Flavor.SPHERICAL2)));

			assertEquals("", STCS.toSTCS(new CoordSys(null)));
			assertEquals("", STCS.toSTCS(new CoordSys("")));
			assertEquals("", STCS.toSTCS(new CoordSys("   	\n\r")));

			assertEquals("ICRS", STCS.toSTCS(new CoordSys(Frame.ICRS, null, null)));
			assertEquals("ICRS", STCS.toSTCS(new CoordSys(Frame.ICRS, RefPos.DEFAULT, Flavor.DEFAULT)));
			assertEquals("ICRS", STCS.toSTCS(new CoordSys(Frame.ICRS, RefPos.UNKNOWNREFPOS, Flavor.SPHERICAL2)));

			assertEquals("GEOCENTER", STCS.toSTCS(new CoordSys(null, RefPos.GEOCENTER, null)));
			assertEquals("GEOCENTER", STCS.toSTCS(new CoordSys(Frame.DEFAULT, RefPos.GEOCENTER, Flavor.DEFAULT)));
			assertEquals("GEOCENTER", STCS.toSTCS(new CoordSys(Frame.UNKNOWNFRAME, RefPos.GEOCENTER, Flavor.SPHERICAL2)));

			assertEquals("CARTESIAN3", STCS.toSTCS(new CoordSys(null, null, Flavor.CARTESIAN3)));
			assertEquals("CARTESIAN3", STCS.toSTCS(new CoordSys(Frame.DEFAULT, RefPos.UNKNOWNREFPOS, Flavor.CARTESIAN3)));
			assertEquals("CARTESIAN3", STCS.toSTCS(new CoordSys(Frame.UNKNOWNFRAME, RefPos.UNKNOWNREFPOS, Flavor.CARTESIAN3)));

			assertEquals("ICRS GEOCENTER", STCS.toSTCS(new CoordSys(Frame.ICRS, RefPos.GEOCENTER, null)));
			assertEquals("ICRS GEOCENTER", STCS.toSTCS(new CoordSys(Frame.ICRS, RefPos.GEOCENTER, Flavor.DEFAULT)));

			assertEquals("UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2", new CoordSys().toFullSTCS());
			assertEquals("UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2", new CoordSys("").toFullSTCS());
			assertEquals("UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2", new CoordSys(null).toFullSTCS());
			assertEquals("UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2", new CoordSys("   	\n\t").toFullSTCS());
			assertEquals("UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2", new CoordSys(null, null, null).toFullSTCS());
			assertEquals("UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2", new CoordSys(Frame.DEFAULT, RefPos.DEFAULT, Flavor.DEFAULT).toFullSTCS());
			assertEquals("ICRS UNKNOWNREFPOS SPHERICAL2", new CoordSys(Frame.ICRS, null, null).toFullSTCS());
			assertEquals("ICRS UNKNOWNREFPOS SPHERICAL2", new CoordSys(Frame.ICRS, RefPos.DEFAULT, Flavor.DEFAULT).toFullSTCS());
			assertEquals("UNKNOWNFRAME GEOCENTER SPHERICAL2", new CoordSys(Frame.UNKNOWNFRAME, RefPos.GEOCENTER, Flavor.DEFAULT).toFullSTCS());
			assertEquals("UNKNOWNFRAME UNKNOWNREFPOS CARTESIAN3", new CoordSys(Frame.DEFAULT, RefPos.DEFAULT, Flavor.CARTESIAN3).toFullSTCS());
			assertEquals("ICRS GEOCENTER SPHERICAL2", new CoordSys(Frame.ICRS, RefPos.GEOCENTER, Flavor.DEFAULT).toFullSTCS());
		}catch(ParseException pe){
			pe.printStackTrace(System.err);
			fail();
		}
	}

	@Test
	public void parseRegion(){
		// TESTS WITH NO STC-S:
		try{
			STCS.parseRegion(null);
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Missing STC-S expression to parse!", e.getMessage());
		}
		try{
			STCS.parseRegion("");
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Missing STC-S expression to parse!", e.getMessage());
		}
		try{
			STCS.parseRegion("   	\n\r");
			fail();
		}catch(Exception e){
			assertTrue(e instanceof ParseException);
			assertEquals("Missing STC-S expression to parse!", e.getMessage());
		}

		// TESTS WITH A VALID EXPRESSION, EACH OF A DIFFERENT REGION TYPE:
		String[] expressions = new String[]{" Position GALACTIC 10 20","Circle  	 ICRS    GEOCENTER	10	20	0.5 ","BOX cartesian2 3 3 2 2","Polygon 1 4 2 4 2 5 1 5","Union ICRS (Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)","INTERSECTION ICRS (Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)","NOT(Circle ICRS GEOCENTER 10 20 0.5)"};
		try{
			for(String e : expressions)
				STCS.parseRegion(e);
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail();
		}

		// TEST WITH A MISSING PARAMETER:
		expressions = new String[]{" Position GALACTIC 10 ","BOX cartesian2 3 3 2","NOT()"};
		for(String e : expressions){
			try{
				STCS.parseRegion(e);
				fail();
			}catch(Exception ex){
				assertTrue(ex instanceof ParseException);
				assertTrue(ex.getMessage().startsWith("Unexpected End Of Expression! Expected syntax: \""));
			}
		}

		// TEST WITH A WRONG COORDINATE SYSTEM (since it is optional in all these expressions, it will be considered as a coordinate...which is of course, not the case):
		try{
			STCS.parseRegion("Circle  	 HERE	10	20	0.5 ");
			fail();
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertTrue(ex.getMessage().startsWith("Incorrect syntax: a coordinates pair (2 numerics separated by one or more spaces) was expected! Expected syntax: \"CIRCLE <coordSys> <coordPair> <radius>\", where coordPair=\"<numeric> <numeric>\", radius=\"<numeric>\" and coordSys=\"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used."));
		}

		// TEST WITH EITHER A WRONG NUMERIC (L in lower case instead of 1) OR A MISSING OPENING PARENTHESIS:
		expressions = new String[]{"Polygon 1 4 2 4 2 5 l 5","Union ICRS Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)"};
		for(String e : expressions){
			try{
				STCS.parseRegion(e);
				fail();
			}catch(Exception ex){
				assertTrue(ex instanceof ParseException);
				assertTrue(ex.getMessage().startsWith("Incorrect syntax: "));
			}
		}
	}
}
