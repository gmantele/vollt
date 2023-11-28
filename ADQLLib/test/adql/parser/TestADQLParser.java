package adql.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.FunctionDef;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.db.exception.UnsupportedFeatureException;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ADQLGrammar200Constants;
import adql.parser.grammar.ParseException;
import adql.parser.grammar.Token;
import adql.query.ADQLQuery;
import adql.query.ADQLSet;
import adql.query.SetOperation;
import adql.query.SetOperationType;
import adql.query.WithItem;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.operand.StringConstant;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.RegionFunction;
import adql.query.operand.function.string.LowerFunction;

public class TestADQLParser {

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
	public void testEncapsulatedConstraints(){
		for(ADQLVersion vers : ADQLVersion.values()){
			ADQLParser parser = new ADQLParser(vers);
			try {
				parser.parseQuery("SELECT * FROM myTable WHERE (((4+5)>6))");
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Multi level constraints should be supported!");
			}
		}
	}

	@Test
	public void testEncapsulatedConstraints_ForReal(){
		/* Note: The tested query comes from a real and more complex ADQL query
		 *       which originally did not pass the VOLLT/ADQLLib parser. */
		for(ADQLVersion vers : ADQLVersion.values()){
			ADQLParser parser = new ADQLParser(vers);
			try {
				parser.parseQuery("SELECT * FROM ivoa.Obscore \n" +
						"WHERE ((em_min < 2.2E-6 AND em_max > 2.2E-6)    -- wavelength covering (either the Ks band\n" +
						"   OR  (em_min < 1.65E-6 AND em_max > 1.65E-6)  --                          or the H band,\n" +
						"   OR  (em_min < 1.25E-6 AND em_max > 1.25E-6)) --                          or the J band).");
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Multi level constraints should be supported!");
			}
		}
	}

	@Test
	public void testSetOperation() {
		// CASE: ADQL-2.0 => ERROR
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_0);
		try {
			parser.parseQuery("SELECT * FROM foo UNION SELECT * FROM bar");
			fail("In ADQL-2.0, UNION should not be allowed....it does not exist!");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals(" Encountered \"UNION\". Was expecting one of: <EOF> \".\" \",\" \";\" \"AS\" \"WHERE\" \"GROUP\" \"HAVING\" \"ORDER\" \"\\\"\" <REGULAR_IDENTIFIER_CANDIDATE> \n(HINT: \"UNION\" is not supported in ADQL v2.0, but is however a reserved word. To use it as a column/table/schema name/alias, write it between double quotes.)", ex.getMessage());
		}

		parser = new ADQLParser(ADQLVersion.V2_1);
		try {

			// CASE: Same with ADQL-2.1 => OK
			for(SetOperationType setOp : SetOperationType.values()) {
				ADQLSet query = parser.parseQuery("SELECT * FROM foo " + setOp + " SELECT * FROM bar");
				assertEquals(SetOperation.class, query.getClass());
				assertEquals("SELECT *\nFROM foo", ((SetOperation)query).getLeftSet().toADQL());
				assertEquals(setOp, ((SetOperation)query).getOperation());
				assertFalse(((SetOperation)query).isWithDuplicates());
				assertEquals("SELECT *\nFROM bar", ((SetOperation)query).getRightSet().toADQL());
			}

			// CASE: with sub-queries more elaborated (e.g. with, order by) => OK
			for(SetOperationType setOp : SetOperationType.values()) {
				ADQLSet query = parser.parseQuery("WITH tt AS (SELECT * FROM titi) (SELECT * FROM foo ORDER BY 1) " + setOp + " (SELECT * FROM bar OFFSET 1)");
				assertEquals(SetOperation.class, query.getClass());
				assertNotNull(query.getWith());
				assertEquals(1, query.getWith().size());
				assertEquals("SELECT *\nFROM foo\nORDER BY 1 ASC", ((SetOperation)query).getLeftSet().toADQL());
				assertEquals(setOp, ((SetOperation)query).getOperation());
				assertFalse(((SetOperation)query).isWithDuplicates());
				assertEquals("SELECT *\nFROM bar\nOFFSET 1", ((SetOperation)query).getRightSet().toADQL());
			}

			// CASE: sub-queries with set operation => OK
			ADQLSet query = parser.parseQuery("SELECT * FROM titi UNION SELECT * FROM foo UNION SELECT * FROM bar");
			assertEquals(SetOperation.class, query.getClass());
			assertEquals(SetOperation.class, ((SetOperation)query).getLeftSet().getClass());
			assertEquals("SELECT *\nFROM titi", ((SetOperation)((SetOperation)query).getLeftSet()).getLeftSet().toADQL());
			assertEquals(SetOperationType.UNION, ((SetOperation)((SetOperation)query).getLeftSet()).getOperation());
			assertFalse(((SetOperation)((SetOperation)query).getLeftSet()).isWithDuplicates());
			assertEquals("SELECT *\nFROM foo", ((SetOperation)((SetOperation)query).getLeftSet()).getRightSet().toADQL());
			assertEquals(SetOperationType.UNION, ((SetOperation)query).getOperation());
			assertFalse(((SetOperation)query).isWithDuplicates());
			assertEquals("SELECT *\nFROM bar", ((SetOperation)query).getRightSet().toADQL());
			assertEquals("((SELECT *\nFROM titi\nUNION\nSELECT *\nFROM foo)\nUNION\nSELECT *\nFROM bar)", query.toADQL());

			// CASE: sub-queries with set operation (using operation precedence) => OK
			query = parser.parseQuery("SELECT * FROM titi UNION ALL SELECT * FROM foo INTERSECT SELECT * FROM bar");
			assertEquals(SetOperation.class, query.getClass());
			assertEquals("SELECT *\nFROM titi", ((SetOperation)query).getLeftSet().toADQL());
			assertEquals(SetOperationType.UNION, ((SetOperation)query).getOperation());
			assertTrue(((SetOperation)query).isWithDuplicates());
			assertEquals(SetOperation.class, ((SetOperation)query).getRightSet().getClass());
			assertEquals("SELECT *\nFROM foo", ((SetOperation)((SetOperation)query).getRightSet()).getLeftSet().toADQL());
			assertEquals(SetOperationType.INTERSECT, ((SetOperation)((SetOperation)query).getRightSet()).getOperation());
			assertFalse(((SetOperation)((SetOperation)query).getRightSet()).isWithDuplicates());
			assertEquals("SELECT *\nFROM bar", ((SetOperation)((SetOperation)query).getRightSet()).getRightSet().toADQL());
			assertEquals("(SELECT *\nFROM titi\nUNION ALL\n(SELECT *\nFROM foo\nINTERSECT\nSELECT *\nFROM bar))", query.toADQL());

			// CASE: sub-queries with set operation (using parenthesis precedence) => OK
			query = parser.parseQuery("(SELECT * FROM titi UNION ALL SELECT * FROM foo) INTERSECT SELECT * FROM bar");
			assertEquals(SetOperation.class, query.getClass());
			assertEquals(SetOperation.class, ((SetOperation)query).getLeftSet().getClass());
			assertEquals("SELECT *\nFROM titi", ((SetOperation)((SetOperation)query).getLeftSet()).getLeftSet().toADQL());
			assertEquals(SetOperationType.UNION, ((SetOperation)((SetOperation)query).getLeftSet()).getOperation());
			assertTrue(((SetOperation)((SetOperation)query).getLeftSet()).isWithDuplicates());
			assertEquals("SELECT *\nFROM foo", ((SetOperation)((SetOperation)query).getLeftSet()).getRightSet().toADQL());
			assertEquals(SetOperationType.INTERSECT, ((SetOperation)query).getOperation());
			assertFalse(((SetOperation)query).isWithDuplicates());
			assertEquals("SELECT *\nFROM bar", ((SetOperation)query).getRightSet().toADQL());
			assertEquals("((SELECT *\nFROM titi\nUNION ALL\nSELECT *\nFROM foo)\nINTERSECT\nSELECT *\nFROM bar)", query.toADQL());

		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while parsing a valid query with a UNION operation! (see console for more details)");
		}
	}

	@Test
	public void testCastTypeChecking() {
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		// Test the expected type - NUMERIC - generated by the parser:
		try {
			assertNotNull(parser.parseQuery("SELECT CAST('1' AS INTEGER) * 3 FROM foo;"));
		} catch(ParseException e) {
			e.printStackTrace();
			fail("This query contains a product between 2 numerics: this test should have succeeded!");
		}
		try {
			parser.parseQuery("SELECT CAST('abc' AS VARCHAR) * 3 FROM foo;");
			fail("This query contains a product between a string and an integer: this test should have failed!");
		} catch(ParseException e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Encountered: \"VARCHAR\"! Was expecting one numeric datatype among: \"SMALLINT\", \"INTEGER\", \"BIGINT\", \"REAL\", \"DOUBLE PRECISION\".", e.getMessage());
		}

		// Test the expected type - STRING - generated by the parser:
		try {
			assertNotNull(parser.parseQuery("SELECT CAST('abc' AS TIMESTAMP) || 'blabla' FROM foo;"));
		} catch(ParseException e) {
			e.printStackTrace();
			fail("This query contains a concatenation between 2 strings: this test should have succeeded!");
		}
		try {
			assertNotNull(parser.parseQuery("SELECT CAST('1' AS REAL) || 'blabla' FROM foo;"));
			fail("This query contains a concatenation between a numeric and a string: this test should have failed!");
		} catch(ParseException e) {
			assertTrue(e instanceof ParseException);
			assertEquals("Encountered: \"REAL\"! Was expecting one character datatype among: \"CHAR\", \"VARCHAR\", \"TIMESTAMP\".", e.getMessage());
		}
	}

	@Test
	public void testWithClause() {

		// CASE: ADQL-2.0 => ERROR
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_0);
		try {
			parser.parseQuery("WITH foo AS (SELECT * FROM bar) SELECT * FROM foo");
			fail("In ADQL-2.0, the WITH should not be allowed....it does not exist!");
		}
		catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals(" Encountered \"WITH\". Was expecting: \"SELECT\" \n(HINT: \"WITH\" is not supported in ADQL v2.0, but is however a reserved word. To use it as a column/table/schema name/alias, write it between double quotes.)", ex.getMessage());
		}

		parser = new ADQLParser(ADQLVersion.V2_1);
		try {

			// CASE: Same with ADQL-2.1 => OK
			ADQLSet query = parser.parseQuery("WITH foo AS (SELECT * FROM bar) SELECT * FROM foo");
			assertNotNull(query.getWith());
			assertEquals(1, query.getWith().size());
			WithItem item = query.getWith().get(0);
			assertEquals("foo", item.getLabel());
			assertFalse(item.isLabelCaseSensitive());
			assertEquals("SELECT *\nFROM bar", item.getQuery().toADQL());

			// CASE: WITH clause with a column set => OK
			query = parser.parseQuery("WITH foo AS (SELECT col1, col2, col3 FROM bar) SELECT * FROM foo");
			assertNotNull(query.getWith());
			assertEquals(1, query.getWith().size());
			item = query.getWith().get(0);
			assertEquals("foo", item.getLabel());
			assertFalse(item.isLabelCaseSensitive());
			assertEquals("SELECT col1 , col2 , col3\nFROM bar", item.getQuery().toADQL());

			// CASE: more than 1 WITH clause + CTE's label case sensitivity
			query = parser.parseQuery("WITH foo AS (SELECT col1, col2, col3 FROM bar), \"Foo2\" AS (SELECT * FROM bar2) SELECT * FROM foo NATURAL JOIN \"Foo2\"");
			assertNotNull(query.getWith());
			assertEquals(2, query.getWith().size());
			item = query.getWith().get(0);
			assertEquals("foo", item.getLabel());
			assertFalse(item.isLabelCaseSensitive());
			assertEquals("SELECT col1 , col2 , col3\nFROM bar", item.getQuery().toADQL());
			item = query.getWith().get(1);
			assertEquals("Foo2", item.getLabel());
			assertTrue(item.isLabelCaseSensitive());
			assertEquals("SELECT *\nFROM bar2", item.getQuery().toADQL());
		}
		catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while parsing a valid query with a WITH clause! (see console for more details)");
		}

		// CASE: WITH clause inside a WITH clause => ERROR
		try{
			parser.parseQuery("WITH foo  AS (WITH innerFoo AS (SELECT col1, col2, col3 FROM bar) SELECT * FROM stars NATURAL JOIN innerFoo) SELECT * FROM foo");
		}
		catch(Exception ex){
			assertEquals(ParseException.class, ex.getClass());
			assertEquals(" Encountered \"WITH\". Was expecting one of: \"(\" \"SELECT\" \n(HINT: \"WITH\" is a reserved ADQL word in v2.1. To use it as a column/table/schema name/alias, write it between double quotes.)", ex.getMessage());
		}
	}

	@Test
	public void testConstraintList() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);

			/* TEST: in a constraint (i.e. Constraint() in the grammar),
			 *       avoid ambiguity between (OPERAND) and (CONSTRAINT) */
			try {
				parser.parseWhere("WHERE (mag + 2) < 5"); // CONSTRAINT = (OPERAND) COMP_OPERATOR OPERAND
				parser.parseWhere("WHERE (mag < 2)");     // CONSTRAINT = (CONSTRAINT)
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("[ADQL-" + version + "] Unexpected error while parsing WHERE valid conditions! (see console for more details)");
			}

			// CASE: same test but this time with an incorrect function argument
			/*
			 * NOTE: If this expression is not correctly parsed, the raised
			 *       error will be about CONTAINS instead of being about PI.
			 */
			try {
				parser.parseWhere("WHERE CONTAINS(PI(), CIRCLE('', ra, dec)) = 1");
				fail("PI() is not a valid argument of CONTAINS(...)!");
			} catch(Exception ex) {
				assertEquals(ParseException.class, ex.getClass());
				assertTrue(ex.getMessage().startsWith(" Encountered \"PI\". Was expecting one of: \"BOX\" \"CENTROID\" \"CIRCLE\" \"POINT\" \"POLYGON\""));
			}
		}
	}

	@Test
	public void testNumericFunctionParams() {

		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		/* CASE: LOWER can only take a string in parameter, but according to the
		 *       grammar (and BNF), an unsigned numeric is a string (??).
		 *       In such case, an error should be raised: */
		try {
			parser.parseQuery("SELECT LOWER(123) FROM foo");
			fail("LOWER can not take a numeric in parameter.");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("Incorrect argument: The ADQL function LOWER must have one parameter of type VARCHAR (i.e. a String)!", ex.getMessage());
		}

		// CASE: Idem for a second parameter:
		try {
			parser.parseQuery("SELECT IN_UNIT(12.3, 123) FROM foo");
			fail("IN_UNIT can not take a numeric in 2nd parameter.");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("Incorrect argument: The 2nd argument of the ADQL function IN_UNIT (i.e. target unit) must be of type VARCHAR (i.e. a string)!", ex.getMessage());
		}
	}

	@Test
	public void testOffset() {

		// CASE: No OFFSET in ADQL-2.0
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_0);
		try {
			parser.parseQuery("SELECT * FROM foo ORDER BY id OFFSET 10");
			fail("OFFSET should not be allowed with ADQL-2.0!");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals(" Encountered \"OFFSET\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", ex.getMessage());
		}

		// CASE: OFFSET allowed in ADQL-2.1
		parser = new ADQLParser(ADQLVersion.V2_1);
		try {
			assertEquals("SELECT *\nFROM foo\nOFFSET 10", parser.parseQuery("SELECT * FROM foo OFFSET 10").toADQL());
			assertEquals("SELECT *\nFROM foo\nORDER BY id ASC\nOFFSET 10", parser.parseQuery("SELECT * FROM foo ORDER BY id OFFSET 10").toADQL());
			assertEquals("SELECT *\nFROM foo\nORDER BY id ASC\nOFFSET 0", parser.parseQuery("SELECT * FROM foo ORDER BY id OFFSET 0").toADQL());
			assertEquals("SELECT TOP 5 *\nFROM foo\nORDER BY id ASC\nOFFSET 10", parser.parseQuery("SELECT TOP 5 * FROM foo ORDER BY id OFFSET 10").toADQL());
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error with a valid OFFSET syntax! (see console for more details)");
		}

		// CASE: Only an unsigned integer constant is allowed
		String[] offsets = new String[]{ "-1", "colOffset", "2*5" };
		String[] expectedErrors = new String[]{ " Encountered \"-\". Was expecting: <UNSIGNED_INTEGER> ", " Encountered \"colOffset\". Was expecting: <UNSIGNED_INTEGER> ", " Encountered \"*\". Was expecting one of: <EOF> \";\" " };
		for(int i = 0; i < offsets.length; i++) {
			try {
				parser.parseQuery("SELECT * FROM foo OFFSET " + offsets[i]);
				fail("Incorrect offset expression (\"" + offsets[i] + "\"). This test should have failed.");
			} catch(Exception ex) {
				assertEquals(ParseException.class, ex.getClass());
				assertEquals(expectedErrors[i], ex.getMessage());
			}
		}
	}

	@Test
	public void testGroupBy() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);

			// CASE: simple or qualified column name => OK!
			try {
				parser.parseQuery("SELECT * FROM cat GROUP BY oid;");
				parser.parseQuery("SELECT * FROM cat GROUP BY cat.oid;");
			} catch(Exception e) {
				e.printStackTrace(System.err);
				fail("These ADQL queries are strictly correct! No error should have occured. (see stdout for more details)");
			}

			/* CASE: in ADQL-2.0, only a column is allowed (on the contrary to
			 *       ORDER BY which allows also an index of a selected column)
			 *       => ERROR! */
			final String Q_INDEX = "SELECT * FROM cat GROUP BY 1;";
			if (version == ADQLVersion.V2_0) {
				try {
					// GROUP BY with a SELECT item index
					parser.parseQuery(Q_INDEX);
					fail("A SELECT item index is forbidden in GROUP BY! This test should have failed.");
				} catch(Exception e) {
					assertEquals(ParseException.class, e.getClass());
					assertEquals(" Encountered \"1\". Was expecting one of: \"\\\"\" <REGULAR_IDENTIFIER_CANDIDATE> ", e.getMessage());
				}
			} else {
				try {
					parser.parseQuery(Q_INDEX);
				} catch(Exception e) {
					e.printStackTrace(System.err);
					fail("These ADQL queries are strictly correct! No error should have occured. (see stdout for more details)");
				}
			}

			// CASE: only from ADQL-2.1, any ADQL expression/operand => OK!
			final String Q1 = "SELECT * FROM cat GROUP BY 1+2;", Q2 = "SELECT * FROM cat GROUP BY sqrt(foo);";
			if (version == ADQLVersion.V2_0) {
				try {
					parser.parseQuery(Q1);
					fail("In ADQL-v2.0, GROUP BY with an expression is forbidden!");
				} catch(Exception e) {
					assertEquals(ParseException.class, e.getClass());
					assertEquals(" Encountered \"1\". Was expecting one of: \"\\\"\" <REGULAR_IDENTIFIER_CANDIDATE> ", e.getMessage());
				}
				try {
					parser.parseQuery(Q2);
					fail("In ADQL-v2.0, GROUP BY with an expression is forbidden!");
				} catch(Exception e) {
					assertEquals(ParseException.class, e.getClass());
					assertEquals(" Encountered \"sqrt\". Was expecting one of: \"\\\"\" <REGULAR_IDENTIFIER_CANDIDATE> \n" + "(HINT: \"sqrt\" is a reserved ADQL word in v2.0. To use it as a column/table/schema name/alias, write it between double quotes.)", e.getMessage());
				}
			} else {
				try {
					parser.parseQuery(Q1);
					parser.parseQuery(Q2);
				} catch(Exception e) {
					e.printStackTrace(System.err);
					fail("In ADQL-" + version + ", GROUP BY with expression SHOULD be allowed. (see console for more details)");
				}
			}
		}
	}

	@Test
	public void testOrderBy() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				// CASE: Simple column name
				ADQLSet query;
				query = parser.parseQuery("SELECT * FROM cat ORDER BY oid;");
				assertNotNull(query.getOrderBy().get(0).getExpression());
				query = parser.parseQuery("SELECT * FROM cat ORDER BY oid ASC;");
				assertNotNull(query.getOrderBy().get(0).getExpression());
				query = parser.parseQuery("SELECT * FROM cat ORDER BY oid DESC;");
				assertNotNull(query.getOrderBy().get(0).getExpression());

				// CASE: selected column reference
				query = parser.parseQuery("SELECT * FROM cat ORDER BY 1;");
				assertNotNull(query.getOrderBy().get(0).getColumnReference());
				query = parser.parseQuery("SELECT * FROM cat ORDER BY 1 ASC;");
				assertNotNull(query.getOrderBy().get(0).getColumnReference());
				query = parser.parseQuery("SELECT * FROM cat ORDER BY 1 DESC;");
				assertNotNull(query.getOrderBy().get(0).getColumnReference());
			} catch(Exception e) {
				e.printStackTrace(System.err);
				fail("These ADQL queries are strictly correct! No error should have occured. (cf console for more details)");
			}

			// CASE: only in ADQL-2.0, qualified columns are forbidden
			String Q_QUAL_COL = "SELECT * FROM cat ORDER BY cat.oid;";
			if (version == ADQLVersion.V2_0) {
				try {
					parser.parseQuery(Q_QUAL_COL);
					fail("In ADQL-v2.0, a qualified column name is forbidden in ORDER BY! This test should have failed.");
				} catch(Exception e) {
					assertEquals(ParseException.class, e.getClass());
					assertEquals(" Encountered \".\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", e.getMessage());
				}
			} else {
				try {
					parser.parseQuery(Q_QUAL_COL);
				} catch(Exception e) {
					e.printStackTrace();
					fail("In ADQL-" + version + ", ORDER BY with a qualified column name should be allowed! (see console for more details)");
				}
			}

			// CASE: Query reported as in error before a bug correction:
			/*
			 * NOTE: same as above but with a real bug report.
			 */
			Q_QUAL_COL = "SELECT TOP 10 browndwarfs.cat.jmag FROM browndwarfs.cat ORDER BY browndwarfs.cat.jmag";
			if (version == ADQLVersion.V2_0) {
				try {
					parser.parseQuery(Q_QUAL_COL);
					fail("A qualified column name is forbidden in ORDER BY! This test should have failed.");
				} catch(Exception e) {
					assertEquals(ParseException.class, e.getClass());
					assertEquals(" Encountered \".\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", e.getMessage());
				}
			} else {
				try {
					parser.parseQuery(Q_QUAL_COL);
				} catch(Exception e) {
					e.printStackTrace();
					fail("In ADQL-" + version + ", ORDER BY with a qualified column name should be allowed! (see console for more details)");
				}
			}

			// CASE: only from ADQL-2.1, any ADQL expression/operand => OK!
			final String Q1 = "SELECT * FROM cat ORDER BY 1+2;", Q2 = "SELECT * FROM cat ORDER BY sqrt(foo);";
			if (version == ADQLVersion.V2_0) {
				try {
					parser.parseQuery(Q1);
					fail("In ADQL-v2.0, ORDER BY with an expression is forbidden!");
				} catch(Exception e) {
					assertEquals(ParseException.class, e.getClass());
					assertEquals(" Encountered \"+\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", e.getMessage());
				}
				try {
					parser.parseQuery(Q2);
					fail("In ADQL-v2.0, ORDER BY with an expression is forbidden!");
				} catch(Exception e) {
					assertEquals(ParseException.class, e.getClass());
					assertEquals(" Encountered \"sqrt\". Was expecting one of: <UNSIGNED_INTEGER> \"\\\"\" <REGULAR_IDENTIFIER_CANDIDATE> \n" + "(HINT: \"sqrt\" is a reserved ADQL word in v2.0. To use it as a column/table/schema name/alias, write it between double quotes.)", e.getMessage());
				}
			} else {
				try {
					parser.parseQuery(Q1);
					parser.parseQuery(Q2);
				} catch(Exception e) {
					e.printStackTrace(System.err);
					fail("In ADQL-" + version + ", ORDER BY with expression SHOULD be allowed. (see console for more details)");
				}
			}
		}
	}

	@Test
	public void testJoinUsing() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				// JOIN ... USING(...)
				parser.parseQuery("SELECT * FROM cat JOIN cat2 USING(oid);");
			} catch(Exception e) {
				e.printStackTrace(System.err);
				fail("This ADQL query is strictly correct! No error should have occured. (see stdout for more details)");
			}
			try {
				// JOIN ... USING(...)
				parser.parseQuery("SELECT * FROM cat JOIN cat2 USING(cat.oid);");
				fail("A qualified column name is forbidden in USING(...)! This test should have failed.");
			} catch(Exception e) {
				assertEquals(ParseException.class, e.getClass());
				assertEquals(" Encountered \".\". Was expecting one of: \")\" \",\" ", e.getMessage());
			}

			try {
				// JOIN ... USING(...)
				parser.parseQuery("SELECT * FROM cat JOIN cat2 USING(1);");
				fail("A column index is forbidden in USING(...)! This test should have failed.");
			} catch(Exception e) {
				assertEquals(ParseException.class, e.getClass());
				assertEquals(" Encountered \"1\". Was expecting one of: \"\\\"\" <REGULAR_IDENTIFIER_CANDIDATE> ", e.getMessage());
			}
		}
	}

	@Test
	public void testDelimitedIdentifiersWithDot() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				ADQLSet query = parser.parseQuery("SELECT * FROM \"B/avo.rad/catalog\";");
				assertEquals("B/avo.rad/catalog", ((ADQLQuery)query).getFrom().getTables().get(0).getTableName());
			} catch(Exception e) {
				e.printStackTrace(System.err);
				fail("The ADQL query is strictly correct! No error should have occured. (see stdout for more details)");
			}
		}
	}

	@Test
	public void testJoinTree() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				String[] queries = new String[]{ "SELECT * FROM aTable A JOIN aSecondTable B ON A.id = B.id JOIN aThirdTable C ON B.id = C.id;", "SELECT * FROM aTable A NATURAL JOIN aSecondTable B NATURAL JOIN aThirdTable C;" };
				for(String q : queries) {
					ADQLSet query = parser.parseQuery(q);

					assertTrue(((ADQLQuery)query).getFrom() instanceof ADQLJoin);

					ADQLJoin join = ((ADQLJoin)((ADQLQuery)query).getFrom());
					assertTrue(join.getLeftTable() instanceof ADQLJoin);
					assertTrue(join.getRightTable() instanceof ADQLTable);
					assertEquals("aThirdTable", ((ADQLTable)join.getRightTable()).getTableName());

					join = (ADQLJoin)join.getLeftTable();
					assertTrue(join.getLeftTable() instanceof ADQLTable);
					assertEquals("aTable", ((ADQLTable)join.getLeftTable()).getTableName());
					assertTrue(join.getRightTable() instanceof ADQLTable);
					assertEquals("aSecondTable", ((ADQLTable)join.getRightTable()).getTableName());
				}
			} catch(Exception e) {
				e.printStackTrace(System.err);
				fail("The ADQL query is strictly correct! No error should have occured. (see stdout for more details)");
			}
		}
	}

	@Test
	public void test() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				ADQLSet query = parser.parseQuery("SELECT 'truc''machin'  	'bidule' --- why not a comment now ^^\n'FIN' FROM foo;");
				assertNotNull(query);
				assertEquals("truc'machinbiduleFIN", ((StringConstant)(((ADQLQuery)query).getSelect().get(0).getOperand())).getValue());
				assertEquals("'truc''machinbiduleFIN'", ((ADQLQuery)query).getSelect().get(0).getOperand().toADQL());
			} catch(Exception ex) {
				fail("String litteral concatenation is perfectly legal according to the ADQL standard.");
			}

			// With a comment ending the query
			try {
				ADQLSet query = parser.parseQuery("SELECT TOP 1 * FROM ivoa.ObsCore -- comment");
				assertNotNull(query);
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("String litteral concatenation is perfectly legal according to the ADQL standard.");
			}
		}
	}

	@Test
	public void testIncorrectCharacter() {
		for(ADQLVersion version : ADQLVersion.values()) {
			/* An identifier must be written only with digits, an underscore or
			 * regular latin characters: */
			try {
				(new ADQLParser(version)).parseQuery("select gr\u00e9gory FROM aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().startsWith("Incorrect character encountered at l.1, c.10: "));
				assertTrue(t.getMessage().endsWith("Possible cause: a non-ASCI/UTF-8 character (solution: remove/replace it)."));
			}

			/* Un-finished double/single quoted string: */
			try {
				(new ADQLParser(version)).parseQuery("select \"stuff FROM aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().startsWith("Incorrect character encountered at l.1, c.26: <EOF>"));
				assertTrue(t.getMessage().endsWith("Possible cause: a string between single or double quotes which is never closed (solution: well...just close it!)."));
			}

			// But in a string, delimited identifier or a comment, it is fine:
			try {
				(new ADQLParser(version)).parseQuery("select 'gr\u00e9gory' FROM aTable");
				(new ADQLParser(version)).parseQuery("select \"gr\u00e9gory\" FROM aTable");
				(new ADQLParser(version)).parseQuery("select * FROM aTable -- a comment by Gr\u00e9gory");
			} catch(Throwable t) {
				fail("This error should never occurs because all these queries have an accentuated character but at a correct place.");
			}
		}
	}

	@Test
	public void testMultipleSpacesInOrderAndGroupBy() {
		for(ADQLVersion version : ADQLVersion.values()) {
			try {
				ADQLParser parser = new ADQLParser(version);

				// Single space:
				parser.parseQuery("select * from aTable ORDER BY aCol");
				parser.parseQuery("select * from aTable GROUP BY aCol");

				// More than one space:
				parser.parseQuery("select * from aTable ORDER      BY aCol");
				parser.parseQuery("select * from aTable GROUP      BY aCol");

				// With any other space character:
				parser.parseQuery("select * from aTable ORDER\tBY aCol");
				parser.parseQuery("select * from aTable ORDER\nBY aCol");
				parser.parseQuery("select * from aTable ORDER \t\nBY aCol");

				parser.parseQuery("select * from aTable GROUP\tBY aCol");
				parser.parseQuery("select * from aTable GROUP\nBY aCol");
				parser.parseQuery("select * from aTable GROUP \t\nBY aCol");
			} catch(Throwable t) {
				t.printStackTrace();
				fail("Having multiple space characters between the ORDER/GROUP and the BY keywords should not generate any parsing error.");
			}
		}
	}

	@Test
	public void testADQLReservedWord() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);

			final String hintAbs = ".*\n\\(HINT: \"abs\" is a reserved ADQL word in v[0-9]+\\.[0-9]+\\. To use it as a column/table/schema name/alias, write it between double quotes\\.\\)";
			final String hintPoint = ".*\n\\(HINT: \"point\" is a reserved ADQL word in v[0-9]+\\.[0-9]+\\. To use it as a column/table/schema name/alias, write it between double quotes\\.\\)";
			final String hintExists = ".*\n\\(HINT: \"exists\" is a reserved ADQL word in v[0-9]+\\.[0-9]+\\. To use it as a column/table/schema name/alias, write it between double quotes\\.\\)";
			final String hintLike = ".*\n\\(HINT: \"LIKE\" is a reserved ADQL word in v[0-9]+\\.[0-9]+\\. To use it as a column/table/schema name/alias, write it between double quotes\\.\\)";

			/* TEST AS A COLUMN/TABLE/SCHEMA NAME... */
			// ...with a numeric function name (but no param):
			try {
				parser.parseQuery("select abs from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintAbs));
			}
			// ...with a geometric function name (but no param):
			try {
				parser.parseQuery("select point from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintPoint));
			}
			// ...with an ADQL function name (but no param):
			try {
				parser.parseQuery("select exists from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintExists));
			}
			// ...with an ADQL syntax item:
			try {
				parser.parseQuery("select LIKE from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintLike));
			}

			/* TEST AS AN ALIAS... */
			// ...with a numeric function name (but no param):
			try {
				parser.parseQuery("select aCol AS abs from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintAbs));
			}
			// ...with a geometric function name (but no param):
			try {
				parser.parseQuery("select aCol AS point from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintPoint));
			}
			// ...with an ADQL function name (but no param):
			try {
				parser.parseQuery("select aCol AS exists from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintExists));
			}
			// ...with an ADQL syntax item:
			try {
				parser.parseQuery("select aCol AS LIKE from aTable");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintLike));
			}

			/* TEST AT THE END OF THE QUERY (AND IN A WHERE) */
			try {
				parser.parseQuery("select aCol from aTable WHERE toto = abs");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(hintAbs));
			}
		}
	}

	@Test
	public void testSQLReservedWord() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);

			try {
				parser.parseQuery("SELECT rows FROM aTable");
				fail("\"ROWS\" is an SQL reserved word. This query should not pass.");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(".*\n\\(HINT: \"rows\" is not supported in ADQL v[0-9]+\\.[0-9]+, but is however a reserved word\\. To use it as a column/table/schema name/alias, write it between double quotes\\.\\)"));
			}

			try {
				parser.parseQuery("SELECT CASE WHEN aCol = 2 THEN 'two' ELSE 'smth else' END as str FROM aTable");
				fail("ADQL does not support the CASE syntax. This query should not pass.");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertTrue(t.getMessage().matches(".*\n\\(HINT: \"CASE\" is not supported in ADQL v[0-9]+\\.[0-9]+, but is however a reserved word\\. To use it as a column/table/schema name/alias, write it between double quotes\\.\\)"));
			}
		}
	}

	@Test
	public void testUDFName() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.allowAnyUdf(true); // for test purpose

			// CASE: Valid UDF name => OK
			try {
				parser.parseQuery("SELECT foo(p1,p2) FROM aTable");
			} catch(Throwable t) {
				t.printStackTrace();
				fail("Unexpected parsing error! This query should have passed. (see console for more details)");
			}

			// CASE: Invalid UDF name => ParseException
			final String[] functionsToTest = new String[]{ "_foo", "2do", "do?" };
			for(String fct : functionsToTest) {
				try {
					parser.parseQuery("SELECT " + fct + "(p1,p2) FROM aTable");
					fail("A UDF name like \"" + fct + "\" is not allowed by the ADQL grammar. This query should not pass.");
				} catch(Throwable t) {
					assertEquals(ParseException.class, t.getClass());
					assertEquals("Invalid (User Defined) Function name: \"" + fct + "\"!", t.getMessage());
				}
			}
		}
	}

	@Test
	public void testUDFDeclaration() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);

			// CASE: Any UDF allowed => OK!
			parser.allowAnyUdf(true);
			try {
				assertNotNull(parser.parseQuery("SELECT foo(1,2) FROM bar"));
			} catch(Throwable t) {
				t.printStackTrace();
				fail("Unexpected parsing error! This query should have passed. (see console for more details)");
			}

			// CASE: No UDF allowed => ERROR
			parser.allowAnyUdf(false);
			try {
				parser.parseQuery("SELECT foo(1,2) FROM bar");
				fail("No UDF is allowed. This query should have failed!");
			} catch(Throwable t) {
				assertEquals(UnresolvedIdentifiersException.class, t.getClass());
				assertEquals("1 unsupported expressions!\n  - Unsupported ADQL feature: \"foo(param1 ?type?, param2 ?type?) -> ?type?\" (of type 'ivo://ivoa.net/std/TAPRegExt#features-udf')!", t.getMessage());
			}

			// CASE: a single UDF declared => OK!
			try {
				parser.getSupportedFeatures().support(FunctionDef.parse("foo(i1 INTEGER, i2 INTEGER) -> INTEGER").toLanguageFeature());
				assertNotNull(parser.parseQuery("SELECT foo(1,2) FROM bar"));
			} catch(Throwable t) {
				t.printStackTrace();
				fail("Unexpected parsing error! This query should have passed. (see console for more details)");
			}
		}
	}

	@Test
	public void testOptionalFeatures() {
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_0);

		// CASE: No support for the ADQL-2.1 function - LOWER => ERROR
		try {
			parser.parseQuery("SELECT LOWER(foo) FROM aTable");
			fail("The function \"LOWER\" is not yet supported in ADQL-2.0. This query should not pass.");
		} catch(Throwable t) {
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().contains("(HINT: \"LOWER\" is not supported in ADQL v2.0, but is however a reserved word."));
		}

		// CASE: LOWER supported by default in ADQL-2.1 => OK
		parser = new ADQLParser(ADQLVersion.V2_1);
		try {
			ADQLSet q = parser.parseQuery("SELECT LOWER(foo) FROM aTable");
			assertNotNull(q);
			assertEquals("SELECT LOWER(foo)\nFROM aTable", q.toADQL());
		} catch(Throwable t) {
			t.printStackTrace();
			fail("The function \"LOWER\" is available in ADQL-2.1 and is declared as supported. This query should pass.");
		}

		// CASE: LOWER now declared as not supported => ERROR
		assertTrue(parser.getSupportedFeatures().unsupport(LowerFunction.FEATURE));
		try {
			parser.parseQuery("SELECT LOWER(foo) FROM aTable");
			fail("The function \"LOWER\" is not available in ADQL-2.1 and is here declared as not supported. This query should not pass.");
		} catch(Throwable t) {
			assertEquals(UnresolvedIdentifiersException.class, t.getClass());
			UnresolvedIdentifiersException uie = (UnresolvedIdentifiersException)t;
			assertEquals(1, uie.getNbErrors());
			Exception err = uie.getErrors().next();
			assertEquals(UnsupportedFeatureException.class, err.getClass());
			assertEquals("Unsupported ADQL feature: \"LOWER\" (of type '" + LanguageFeature.TYPE_ADQL_STRING + "')!", err.getMessage());
		}

		/* ***************************************************************** */
		/* NOTE: Geometrical functions are the only optional features in 2.0 */
		/* ***************************************************************** */

		parser = new ADQLParser(ADQLVersion.V2_0);

		// CASE: By default all geometries are supported so if one is used => OK
		try {
			assertNotNull(parser.parseQuery("SELECT POINT('', ra, dec) FROM aTable"));
		} catch(Throwable t) {
			t.printStackTrace();
			fail("Unexpected error! This query should have passed. (see console for more details)");
		}

		// unsupport all features:
		parser.getSupportedFeatures().unsupportAll();

		// CASE: No geometry supported so if one is used => ERROR
		try {
			parser.parseQuery("SELECT POINT('', ra, dec) FROM aTable");
			fail("The geometrical function \"POINT\" is not declared. This query should not pass.");
		} catch(Throwable t) {
			assertEquals(UnresolvedIdentifiersException.class, t.getClass());
			UnresolvedIdentifiersException allErrors = (UnresolvedIdentifiersException)t;
			assertEquals(1, allErrors.getNbErrors());
			assertEquals("Unsupported ADQL feature: \"POINT\" (of type '"+LanguageFeature.TYPE_ADQL_GEO+"')!", allErrors.getErrors().next().getMessage());
		}

		// now support only POINT:
		assertTrue(parser.getSupportedFeatures().support(PointFunction.FEATURE));

		// CASE: Just supporting the only used geometry => OK
		try {
			assertNotNull(parser.parseQuery("SELECT POINT('', ra, dec) FROM aTable"));
		} catch(Throwable t) {
			t.printStackTrace();
			fail("Unexpected error! This query should have passed. (see console for more details)");
		}
	}

	@Test
	public void testGeometry() {
		for(ADQLVersion version : ADQLVersion.values()) {
			// DECLARE A SIMPLE PARSER where all geometries are allowed by default:
			ADQLParser parser = new ADQLParser(version);

			// Test with several geometries while all are allowed:
			try {
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("This query contains several geometries, and all are theoretically allowed: this test should have succeeded!");
			}

			// Test with several geometries while only the allowed ones:
			try {
				parser = new ADQLParser(version);
				parser.getSupportedFeatures().unsupportAll(LanguageFeature.TYPE_ADQL_GEO);
				parser.getSupportedFeatures().support(ContainsFunction.FEATURE);
				parser.getSupportedFeatures().support(PointFunction.FEATURE);
				parser.getSupportedFeatures().support(CircleFunction.FEATURE);

				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("This query contains several geometries, and all are theoretically allowed: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT * FROM foo WHERE INTERSECTS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;");
				fail("This query contains a not-allowed geometry function (INTERSECTS): this test should have failed!");
			} catch(ParseException pe) {
				assertTrue(pe instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Unsupported ADQL feature: \"INTERSECTS\" (of type '"+LanguageFeature.TYPE_ADQL_GEO+"')!", ex.getErrors().next().getMessage());
			}

			// TODO Test by adding REGION: // Only possible with ADQL-2.0 since in ADQL-2.1, REGION has been removed!
			try {
				parser = new ADQLParser(ADQLVersion.V2_0);
				parser.getSupportedFeatures().unsupportAll(LanguageFeature.TYPE_ADQL_GEO);
				parser.getSupportedFeatures().support(ContainsFunction.FEATURE);
				parser.getSupportedFeatures().support(PointFunction.FEATURE);
				parser.getSupportedFeatures().support(CircleFunction.FEATURE);
				parser.getSupportedFeatures().support(RegionFunction.FEATURE);

				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('Position 12.3 45.6'), REGION('circle 1.2 2.3 5')) = 1;"));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("[ADQL-" + parser.getADQLVersion() + "] This query contains several geometries, and all are theoretically allowed: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('Position 12.3 45.6'), REGION('BOX 1.2 2.3 5 9')) = 1;");
				fail("This query contains a not-allowed geometry function (BOX): this test should have failed!");
			} catch(ParseException pe) {
				assertTrue(pe instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Unsupported region type: \"BOX\" (equivalent to the ADQL feature \"BOX\" of type '"+LanguageFeature.TYPE_ADQL_GEO+"')!", ex.getErrors().next().getMessage());
			}

			// Test with several geometries while none geometry is allowed:
			try {
				parser = new ADQLParser(version);
				parser.getSupportedFeatures().unsupportAll(LanguageFeature.TYPE_ADQL_GEO);

				parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;");
				fail("This query contains geometries while they are all forbidden: this test should have failed!");
			} catch(ParseException pe) {
				assertTrue(pe instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
				assertEquals(3, ex.getNbErrors());
				Iterator<ParseException> itErrors = ex.getErrors();
				assertEquals("Unsupported ADQL feature: \"CONTAINS\" (of type '"+LanguageFeature.TYPE_ADQL_GEO+"')!", itErrors.next().getMessage());
				assertEquals("Unsupported ADQL feature: \"POINT\" (of type '"+LanguageFeature.TYPE_ADQL_GEO+"')!", itErrors.next().getMessage());
				assertEquals("Unsupported ADQL feature: \"CIRCLE\" (of type '"+LanguageFeature.TYPE_ADQL_GEO+"')!", itErrors.next().getMessage());
			}
		}
	}

	@Test
	public void testGeometryWithOptionalArgs() {
		/*
		 * NOTE:
		 * 	Since ADQL-2.1, the coordinate system argument becomes optional.
		 *  Besides, BOX, CIRCLE and POLYGON can now accept POINTs instead of
		 *  pairs of coordinates.
		 */

		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		// CASE: with no coordinate system => equivalent to coosys = ''
		try {
			assertEquals("POINT('', 1, 2)", parser.parseSelect("SELECT POINT(1, 2)").get(0).toADQL());

			assertEquals("CIRCLE('', 1, 2, 3)", parser.parseSelect("SELECT CIRCLE(1, 2, 3)").get(0).toADQL());
			assertEquals("CIRCLE('', POINT('', 1, 2), 3)", parser.parseSelect("SELECT CIRCLE(POINT(1,2), 3)").get(0).toADQL());
			assertEquals("CIRCLE('', colCenter, 3)", parser.parseSelect("SELECT CIRCLE(colCenter, 3)").get(0).toADQL());

			assertEquals("BOX('', 1, 2, 3, 4)", parser.parseSelect("SELECT BOX(1, 2, 3, 4)").get(0).toADQL());
			assertEquals("BOX('', POINT('', 1, 2), 3, 4)", parser.parseSelect("SELECT BOX(POINT(1, 2), 3, 4)").get(0).toADQL());
			assertEquals("BOX('', colCenter, 3, 4)", parser.parseSelect("SELECT BOX(colCenter, 3, 4)").get(0).toADQL());

			assertEquals("POLYGON('', 1, 2, 3, 4, 5, 6)", parser.parseSelect("SELECT POLYGON(1, 2, 3, 4, 5, 6)").get(0).toADQL());
			assertEquals("POLYGON('', POINT('', 1, 2), POINT('', 3, 4), POINT('', 5, 6))", parser.parseSelect("SELECT POLYGON(POINT(1, 2), POINT(3, 4), POINT(5, 6))").get(0).toADQL());
			assertEquals("POLYGON('', point1, point2, point3)", parser.parseSelect("SELECT POLYGON(point1, point2, point3)").get(0).toADQL());
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error! All parsed geometries are correct.");
		}

		// CASE: wrong nb of arguments for POLYGON
		for(String wrongQuery : new String[]{ "SELECT POLYGON(ra, dec, 3, 4, 5)", "SELECT POLYGON(ra, dec, 3, 4, 5, 6, 7)", "SELECT POLYGON(p1, p2)" })
			try {
				parser.parseSelect(wrongQuery);
				fail("Impossible to create a POLYGON with an incomplete list of vertices! The last point is missing or incomplete.");
			} catch(Exception ex) {
				assertEquals(ParseException.class, ex.getClass());
				assertTrue(ex.getMessage().trim().startsWith("Encountered \""));
			}
	}

	@Test
	public void testRegion() {
		for(ADQLVersion version : ADQLVersion.values()) {
			// DECLARE A SIMPLE PARSER where all coordinate systems are allowed by default:
			ADQLParser parser = new ADQLParser(version);

			// CASE: REGION can ONLY accept a string literal => check the syntax, region and coordsys:
			parser.allowExtendedRegionParam(false);
			assertFalse(parser.isExtendedRegionParamAllowed());

			// ...CASE: all possible geometries using DALI syntax:
			try {
				parser.parseSelect("SELECT REGION('1 2') AS p");
				parser.parseSelect("SELECT REGION('1 2 3') AS c");
				parser.parseSelect("SELECT REGION('1 2  3 4  5 6') AS poly");
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! All parsed regions with DALI syntax are correct.");
			}

			// ...CASE: all possible geometries using STC/s syntax:
			try {
				parser.parseSelect("SELECT REGION('Position 1 2') AS p");
				parser.parseSelect("SELECT REGION('Circle 1 2 3') AS c");
				parser.parseSelect("SELECT REGION('Box 1 2 3 4') AS b");
				parser.parseSelect("SELECT REGION('Polygon 1 2  3 4  5 6') AS poly");
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! All parsed regions with STC/s syntax are correct.");
			}

			// ...CASE: a different syntax => Error!
			try {
				parser.parseSelect("SELECT REGION('[1, 2]') AS p");
				fail("The syntax used to expressed this point is neither DALI nor STC/s!");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals(1, ((UnresolvedIdentifiersException)ex).getNbErrors());
				assertEquals("Unsupported region serialization!", ((UnresolvedIdentifiersException)ex).getErrors().next().getMessage());
			}

			// ...CASE: not a string literal => Error!
			try {
				parser.parseSelect("SELECT REGION(mySerializedCircle) AS c");
				fail("Something else than a string literal should NOT be accepted here!");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals(1, ((UnresolvedIdentifiersException)ex).getNbErrors());
				assertEquals("Unsupported REGION(...) parameter! Only a string literal is accepted.", ((UnresolvedIdentifiersException)ex).getErrors().next().getMessage());
			}

			// Ensure any REGION is resolved before translation:
			try {
				final String[] supportedSerializations = new String[]{ "1 2", "Circle 1 2 3" };
				for(String str : supportedSerializations) {
					RegionFunction fct = (RegionFunction)(parser.parseSelect("SELECT REGION('" + str + "') AS r").get(0).getOperand());
					assertFalse(fct.isExtendedRegionExpression());
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! All parsed regions are correct.");
			}

			// CASE: REGION can accept any kind of string expression => no check done ; everything passes!
			parser.allowExtendedRegionParam(true);
			assertTrue(parser.isExtendedRegionParamAllowed());

			try {
				// any string literal (not only DALI or STC/s) is allowed (because not anymore checked):
				parser.parseSelect("SELECT REGION('1 2 3') AS c");        // DALI
				parser.parseSelect("SELECT REGION('Circle 1 2 3') AS c"); // STC/s
				parser.parseSelect("SELECT REGION('[[1, 2], 3]') AS c");  // something else
				// a concatenation:
				parser.parseSelect("SELECT REGION('1' || ' ' || '2') AS p");
				// a column:
				parser.parseSelect("SELECT REGION(mySerializedPolygon) AS poly");
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! All regions are valid: they are all string expressions.");
			}

			// Ensure NO REGION is resolved before translation:
			try {
				final String[] supportedSerializations = new String[]{ "1 2", "Circle 1 2 3", "[[1, 2], 3]" };
				for(String str : supportedSerializations) {
					RegionFunction fct = (RegionFunction)(parser.parseSelect("SELECT REGION('" + str + "') AS r").get(0).getOperand());
					assertTrue(fct.isExtendedRegionExpression());
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! All parsed regions are correct.");
			}
		}
	}

	@Test
	public void testCoordSys() {
		for(ADQLVersion version : ADQLVersion.values()) {
			// DECLARE A SIMPLE PARSER where all coordinate systems are allowed by default:
			ADQLParser parser = new ADQLParser(version);

			// A coordinate system MUST be a string literal:
			try {
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('From ' || 'here', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
				fail("A coordinate system can NOT be a string concatenation!");
			} catch(ParseException pe) {
				assertEquals(ParseException.class, pe.getClass());
				assertEquals(48, pe.getPosition().beginColumn);
			}
			try {
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT(aColumn, 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
				fail("A coordinate system can NOT be a column reference!");
			} catch(ParseException pe) {
				assertEquals(ParseException.class, pe.getClass());
				assertEquals(40, pe.getPosition().beginColumn);
			}

			// Test with several coordinate systems while all are allowed:
			try {
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('icrs', 12.3, 45.6), CIRCLE('cartesian2', 1.2, 2.3, 5)) = 1;"));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('lsr', 12.3, 45.6), CIRCLE('galactic heliocenter', 1.2, 2.3, 5)) = 1;"));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('unknownframe', 12.3, 45.6), CIRCLE('galactic unknownrefpos spherical2', 1.2, 2.3, 5)) = 1;"));
				if (version == ADQLVersion.V2_0) {
					assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('position icrs lsr 12.3 45.6'), REGION('circle fk5 1.2 2.3 5')) = 1;"));
					assertNotNull(parser.parseQuery("SELECT Region('not(position 1 2)') FROM foo;"));
				}
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("This query contains several valid coordinate systems, and all are theoretically allowed: this test should have succeeded!");
			}

			// Test with several coordinate systems while only some allowed:
			try {
				parser = new ADQLParser(version);
				parser.setAllowedCoordSys(Arrays.asList(new String[]{ "icrs * *", "fk4 geocenter *", "galactic * spherical2" }));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('icrs', 12.3, 45.6), CIRCLE('cartesian3', 1.2, 2.3, 5)) = 1;"));
				assertNotNull(parser.parseQuery("SELECT POINT('fk4', 12.3, 45.6) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('fk4 geocenter', 12.3, 45.6), CIRCLE('cartesian2', 1.2, 2.3, 5)) = 1;"));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('galactic', 12.3, 45.6), CIRCLE('galactic spherical2', 1.2, 2.3, 5)) = 1;"));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('galactic geocenter', 12.3, 45.6), CIRCLE('galactic lsr spherical2', 1.2, 2.3, 5)) = 1;"));
				if (version == ADQLVersion.V2_0) {
					assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('position galactic lsr 12.3 45.6'), REGION('circle icrs 1.2 2.3 5')) = 1;"));
					assertNotNull(parser.parseQuery("SELECT Region('not(position 1 2)') FROM foo;"));
				}
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("This query contains several valid coordinate systems, and all are theoretically allowed: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT POINT('fk5 geocenter', 12.3, 45.6) FROM foo;");
				fail("This query contains a not-allowed coordinate system ('fk5' is not allowed): this test should have failed!");
			} catch(ParseException pe) {
				assertTrue(pe instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Coordinate system \"fk5 geocenter\" (= \"FK5 GEOCENTER SPHERICAL2\") not allowed in this implementation. Allowed coordinate systems are: fk4 geocenter *, galactic * spherical2, icrs * *", ex.getErrors().next().getMessage());
			}
			if (version == ADQLVersion.V2_0) {
				try {
					parser.parseQuery("SELECT Region('not(position fk5 heliocenter 1 2)') FROM foo;");
					fail("This query contains a not-allowed coordinate system ('fk5' is not allowed): this test should have failed!");
				} catch(ParseException pe) {
					assertTrue(pe instanceof UnresolvedIdentifiersException);
					UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
					assertEquals(1, ex.getNbErrors());
					assertEquals("Coordinate system \"FK5 HELIOCENTER\" (= \"FK5 HELIOCENTER SPHERICAL2\") not allowed in this implementation. Allowed coordinate systems are: fk4 geocenter *, galactic * spherical2, icrs * *", ex.getErrors().next().getMessage());
				}
			}

			// Test with a coordinate system while none is allowed:
			try {
				parser = new ADQLParser(version);
				parser.setAllowedCoordSys(new ArrayList<String>(0));
				assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
				if (version == ADQLVersion.V2_0) {
					assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('position 12.3 45.6'), REGION('circle 1.2 2.3 5')) = 1;"));
					assertNotNull(parser.parseQuery("SELECT Region('not(position 1 2)') FROM foo;"));
				}
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("This query specifies none coordinate system: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('ICRS SPHERICAL2', 12.3, 45.6), CIRCLE('icrs', 1.2, 2.3, 5)) = 1;");
				fail("This query specifies coordinate systems while they are all forbidden: this test should have failed!");
			} catch(ParseException pe) {
				assertTrue(pe instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
				assertEquals(2, ex.getNbErrors());
				Iterator<ParseException> itErrors = ex.getErrors();
				assertEquals("Coordinate system \"ICRS SPHERICAL2\" (= \"ICRS UNKNOWNREFPOS SPHERICAL2\") not allowed in this implementation. No coordinate system is allowed!", itErrors.next().getMessage());
				assertEquals("Coordinate system \"icrs\" (= \"ICRS UNKNOWNREFPOS SPHERICAL2\") not allowed in this implementation. No coordinate system is allowed!", itErrors.next().getMessage());
			}
			if (version == ADQLVersion.V2_0) {
				try {
					parser.parseQuery("SELECT Region('not(position fk4 1 2)') FROM foo;");
					fail("This query specifies coordinate systems while they are all forbidden: this test should have failed!");
				} catch(ParseException pe) {
					assertTrue(pe instanceof UnresolvedIdentifiersException);
					UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
					assertEquals(1, ex.getNbErrors());
					assertEquals("Coordinate system \"FK4\" (= \"FK4 UNKNOWNREFPOS SPHERICAL2\") not allowed in this implementation. No coordinate system is allowed!", ex.getErrors().next().getMessage());
				}
			}
		}
	}

	@Test
	public void testTokenize() {
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_0);

		final String[] EMPTY_STRINGS = new String[]{ null, "", "  ", " 	 " };

		// TEST: NULL or empty string with end at EOF => only one token=EOF
		try {
			for(String str : EMPTY_STRINGS) {
				Token[] tokens = parser.tokenize(str, false);
				assertEquals(1, tokens.length);
				assertEquals(ADQLGrammar200Constants.EOF, tokens[0].kind);
			}
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected error when providing a NULL or empty string to tokenize! (see console for more details)");
		}

		// TEST: NULL or empty string with truncation at EOQ/EOF => empty array
		try {
			for(String str : EMPTY_STRINGS)
				assertEquals(0, parser.tokenize(str, true).length);
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected error when providing a NULL or empty string to tokenize! (see console for more details)");
		}

		// TEST: unknown token => ParseException
		try {
			parser.tokenize("grégory", false);
			fail("No known token is provided. A ParseException was expected.");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("Incorrect character encountered at l.1, c.3: \"\\u00e9\" ('é'), after : \"\"!" + System.getProperty("line.separator", "\n") + "Possible cause: a non-ASCI/UTF-8 character (solution: remove/replace it).", ex.getMessage());
		}

		// TEST: correct list of token => ok
		final String TEST_STR = "SELECT FROM Where foo; join";
		try {
			Token[] tokens = parser.tokenize(TEST_STR, false);
			assertEquals(7, tokens.length);
			int[] expected = new int[]{ ADQLGrammar200Constants.SELECT, ADQLGrammar200Constants.FROM, ADQLGrammar200Constants.WHERE, ADQLGrammar200Constants.REGULAR_IDENTIFIER_CANDIDATE, ADQLGrammar200Constants.EOQ, ADQLGrammar200Constants.JOIN, ADQLGrammar200Constants.EOF };
			for(int i = 0; i < tokens.length; i++)
				assertEquals(expected[i], tokens[i].kind);
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error! All ADQL expressions were composed of correct tokens. (see console for more details)");
		}

		// TEST: same with truncation at EOQ/EOF => same but truncated from EOQ
		try {
			Token[] tokens = parser.tokenize(TEST_STR, true);
			assertEquals(4, tokens.length);
			int[] expected = new int[]{ ADQLGrammar200Constants.SELECT, ADQLGrammar200Constants.FROM, ADQLGrammar200Constants.WHERE, ADQLGrammar200Constants.REGULAR_IDENTIFIER_CANDIDATE };
			for(int i = 0; i < tokens.length; i++)
				assertEquals(expected[i], tokens[i].kind);
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error! All ADQL expressions were composed of correct tokens. (see console for more details)");
		}
	}

	@Test
	public void testDistance() {
		// CASE: In ADQL-2.0, DISTANCE(POINT, POINT) is allowed:
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_0);
		try {
			assertEquals("DISTANCE(POINT('', ra, dec), POINT('', ra2, dec2))", parser.parseSelect("SELECT DISTANCE(POINT('', ra, dec), POINT('', ra2, dec2))").get(0).toADQL());
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error! All ADQL expressions were composed of correct tokens. (see console for more details)");
		}

		// CASE: ...BUT not DISTANCE(lon1, lat1, lon2, lat2)
		try {
			parser.parseSelect("SELECT DISTANCE(ra, dec, ra2, dec2)");
			fail("In ADQL-2.0, DISTANCE(lon1, lat1, lon2, lat2) should not be allowed!");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals(" Encountered \",\". Was expecting one of: \")\" \".\" \".\" \")\" ", ex.getMessage());
		}

		/* CASE: In ADQL-2.1 (and more), DISTANCE(POINT, POINT) and
		 *       DISTANCE(lon1, lat1, lon2, lat2) are both allowed: */
		parser = new ADQLParser(ADQLVersion.V2_1);
		try {
			assertEquals("DISTANCE(POINT('', ra, dec), POINT('', ra2, dec2))", parser.parseSelect("SELECT DISTANCE(POINT('', ra, dec), POINT('', ra2, dec2))").get(0).toADQL());
			assertEquals("DISTANCE(POINT('', ra, dec), POINT('', ra2, dec2))", parser.parseSelect("SELECT DISTANCE(ra, dec, ra2, dec2)").get(0).toADQL());
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error! All ADQL expressions were composed of correct tokens. (see console for more details)");
		}
	}

	/* ************************************************************************
	   TESTS WITH NULL VALUES                                                 */

	@Test
	public void testNULL_in_SELECT(){
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				assertEquals("SELECT NULL\nFROM atable", parser.parseQuery("SELECT NULL FROM atable").toADQL());
			} catch (Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! It must be possible to select NULL. (see console for more details)");
			}
		}
	}

	@Test
	public void testNULL_operation(){
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				assertEquals("SELECT 2*NULL , NULL+1\nFROM atable", parser.parseQuery("SELECT 2*NULL, NULL+1 FROM atable").toADQL());
			} catch (Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! It must be possible to make numeric operations with NULL. (see console for more details)");
			}
		}
	}

	@Test
	public void testNULL_concat(){
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				assertEquals("SELECT 'Something ' || NULL , NULL || ' is absolute'\nFROM atable", parser.parseQuery("SELECT 'Something ' || NULL, NULL || ' is absolute' FROM atable").toADQL());
			} catch (Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! It must be possible to make string concatenations with NULL. (see console for more details)");
			}
		}
	}

	@Test
	public void testNULL_coordsys(){
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				assertEquals("SELECT POINT(NULL, ra, dec)\nFROM atable", parser.parseQuery("SELECT POINT(NULL, ra, dec) FROM atable").toADQL());
			} catch (Exception ex) {
				ex.printStackTrace();
				fail("Unexpected error! It must be possible to create a point with NULL as coord sys. (see console for more details)");
			}
		}
	}

	@Test
	public void testCOALESCE_NULL(){
		for(ADQLVersion version : ADQLVersion.values()) {
			// Skip the test for ADQL-2.0 where COALESCE(...) is not supported:
			if (version != ADQLVersion.V2_0) {
				ADQLParser parser = new ADQLParser(version);
				try {
					assertEquals("SELECT COALESCE(NULL)\nFROM atable", parser.parseQuery("SELECT COALESCE(NULL) FROM atable").toADQL());
				} catch (Exception ex) {
					ex.printStackTrace();
					fail("Unexpected error! It must be possible to select COALESCE(NULL). (see console for more details)");
				}
			}
		}
	}

}
