package adql.db.region;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.parser.grammar.ParseException;

public class TestDALI {

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
	public void parseRegion() {
		// TESTS WITH NO STRING:
		try {
			DALI.parseRegion(null);
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Missing DALI expression to parse!", e.getMessage());
		}
		try {
			DALI.parseRegion("");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Missing DALI expression to parse!", e.getMessage());
		}
		try {
			DALI.parseRegion("   	\n\r");
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Missing DALI expression to parse!", e.getMessage());
		}

		// TESTS WITH A VALID EXPRESSION, EACH OF A DIFFERENT REGION TYPE:
		String[] expressions = new String[]{ " 10 20", "  	 10	20	0.5 ", "1 4 2 4 2 5 1 5" };
		try {
			for(String e : expressions)
				DALI.parseRegion(e);
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail();
		}

		// TEST WITH A MISSING PARAMETER:
		expressions = new String[]{ " 10 ", " 1 4 2 4", "1 4 2 4 2" };
		for(String e : expressions) {
			try {
				DALI.parseRegion(e);
				fail();
			} catch(Exception ex) {
				assertTrue(ex instanceof ParseException);
				assertEquals("Incorrect DALI region!", ex.getMessage());
			}
		}
	}

}
