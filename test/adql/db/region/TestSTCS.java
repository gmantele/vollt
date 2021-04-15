package adql.db.region;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.region.CoordSys.Flavor;
import adql.db.region.CoordSys.Frame;
import adql.db.region.CoordSys.RefPos;
import adql.parser.grammar.ParseException;

public class TestSTCS {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void parseCoordSys() {
		// GOOD SYNTAXES:
		try {
			CoordSys p;

			// Default coordinate system (should be then interpreted as local coordinate system):
			for(String s : new String[]{ null, "", "  	" }) {
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
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail();
		}

		// WRONG SYNTAXES:
		try {
			STCS.parseCoordSys("HOME");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Incorrect syntax: \"HOME\" was unexpected! Expected syntax: \"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used.", e.getMessage());
		}

		// With wrong reference position:
		try {
			STCS.parseCoordSys("ICRS HOME SPHERICAL2");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Incorrect syntax: \"HOME SPHERICAL2\" was unexpected! Expected syntax: \"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used.", e.getMessage());
		}

		// With a cartesian flavor:
		try {
			STCS.parseCoordSys("ICRS CARTESIAN2");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("a coordinate system expressed with a cartesian flavor MUST have an UNKNOWNFRAME and UNKNOWNREFPOS!", e.getMessage());
		}
		try {
			STCS.parseCoordSys("LSR CARTESIAN3");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("a coordinate system expressed with a cartesian flavor MUST have an UNKNOWNFRAME and UNKNOWNREFPOS!", e.getMessage());
		}
		try {
			CoordSys p = STCS.parseCoordSys("CARTESIAN2");
			assertEquals(Frame.UNKNOWNFRAME, p.frame);
			assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
			assertEquals(Flavor.CARTESIAN2, p.flavor);

			p = STCS.parseCoordSys("CARTESIAN3");
			assertEquals(Frame.UNKNOWNFRAME, p.frame);
			assertEquals(RefPos.UNKNOWNREFPOS, p.refpos);
			assertEquals(Flavor.CARTESIAN3, p.flavor);
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail();
		}

		// Without spaces:
		try {
			STCS.parseCoordSys("icrsGeocentercarteSIAN2");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Incorrect syntax: \"icrsGeocentercarteSIAN2\" was unexpected! Expected syntax: \"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used.", e.getMessage());
		}
	}

	@Test
	public void serializeCoordSys() {
		try {
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
		} catch(ParseException pe) {
			pe.printStackTrace(System.err);
			fail();
		}
	}

	@Test
	public void parseRegion() {
		// TESTS WITH NO STC-S:
		try {
			STCS.parseRegion(null);
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Missing STC-S expression to parse!", e.getMessage());
		}
		try {
			STCS.parseRegion("");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Missing STC-S expression to parse!", e.getMessage());
		}
		try {
			STCS.parseRegion("   	\n\r");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Missing STC-S expression to parse!", e.getMessage());
		}

		// TESTS WITH A VALID EXPRESSION, EACH OF A DIFFERENT REGION TYPE:
		String[] expressions = new String[]{ " Position GALACTIC 10 20", "Circle  	 ICRS    GEOCENTER	10	20	0.5 ", "BOX cartesian2 3 3 2 2", "Polygon 1 4 2 4 2 5 1 5", "Union ICRS (Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)", "INTERSECTION ICRS (Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)", "NOT(Circle ICRS GEOCENTER 10 20 0.5)" };
		try {
			for(String e : expressions)
				STCS.parseRegion(e);
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail();
		}

		// TEST WITH A MISSING PARAMETER:
		expressions = new String[]{ " Position GALACTIC 10 ", "BOX cartesian2 3 3 2", "NOT()" };
		for(String e : expressions) {
			try {
				STCS.parseRegion(e);
				fail();
			} catch(Exception ex) {
				assertTrue(ex instanceof ParseException);
				assertTrue(ex.getMessage().startsWith("Unexpected End Of Expression! Expected syntax: \""));
			}
		}

		// TEST WITH A WRONG COORDINATE SYSTEM (since it is optional in all these expressions, it will be considered as a coordinate...which is of course, not the case):
		try {
			STCS.parseRegion("Circle  	 HERE	10	20	0.5 ");
			fail();
		} catch(Exception ex) {
			assertTrue(ex instanceof ParseException);
			assertTrue(ex.getMessage().startsWith("Incorrect syntax: a coordinates pair (2 numerics separated by one or more spaces) was expected! Expected syntax: \"CIRCLE <coordSys> <coordPair> <radius>\", where coordPair=\"<numeric> <numeric>\", radius=\"<numeric>\" and coordSys=\"[(ECLIPTIC|FK4|FK5|J2000|GALACTIC|ICRS|UNKNOWNFRAME)] [(BARYCENTER|GEOCENTER|HELIOCENTER|LSR|TOPOCENTER|RELOCATABLE|UNKNOWNREFPOS)] [(CARTESIAN2|CARTESIAN3|SPHERICAL2)]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used."));
		}

		// TEST WITH EITHER A WRONG NUMERIC (L in lower case instead of 1) OR A MISSING OPENING PARENTHESIS:
		expressions = new String[]{ "Polygon 1 4 2 4 2 5 l 5", "Union ICRS Polygon 1 4 2 4 2 5 1 5 Polygon 3 4 4 4 4 5 3 5)" };
		for(String e : expressions) {
			try {
				STCS.parseRegion(e);
				fail();
			} catch(Exception ex) {
				assertTrue(ex instanceof ParseException);
				assertTrue(ex.getMessage().startsWith("Incorrect syntax: "));
			}
		}
	}
}
