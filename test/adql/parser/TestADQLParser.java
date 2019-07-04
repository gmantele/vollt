package adql.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.db.exception.UnsupportedFeatureException;
import adql.parser.ADQLParserFactory.ADQLVersion;
import adql.parser.feature.LanguageFeature;
import adql.query.ADQLQuery;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.operand.StringConstant;
import adql.query.operand.function.string.LowerFunction;

public class TestADQLParser {

	ADQLParserFactory parserFactory = new ADQLParserFactory();

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
	public void testColumnReference(){
		ADQLParser parser = parserFactory.createParser();
		try{
			// ORDER BY
			parser.parseQuery("SELECT * FROM cat ORDER BY oid;");
			parser.parseQuery("SELECT * FROM cat ORDER BY oid ASC;");
			parser.parseQuery("SELECT * FROM cat ORDER BY oid DESC;");
			parser.parseQuery("SELECT * FROM cat ORDER BY 1;");
			parser.parseQuery("SELECT * FROM cat ORDER BY 1 ASC;");
			parser.parseQuery("SELECT * FROM cat ORDER BY 1 DESC;");
			// GROUP BY
			parser.parseQuery("SELECT * FROM cat GROUP BY oid;");
			parser.parseQuery("SELECT * FROM cat GROUP BY cat.oid;");
			// JOIN ... USING(...)
			parser.parseQuery("SELECT * FROM cat JOIN cat2 USING(oid);");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("These ADQL queries are strictly correct! No error should have occured. (see stdout for more details)");
		}

		try {
			// ORDER BY
			parser.parseQuery("SELECT * FROM cat ORDER BY cat.oid;");
			fail("A qualified column name is forbidden in ORDER BY! This test should have failed.");
		} catch(Exception e) {
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \".\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", e.getMessage());
		}

		// Query reported as in error before the bug correction:
		try {
			parser.parseQuery("SELECT TOP 10 browndwarfs.cat.jmag FROM browndwarfs.cat ORDER BY browndwarfs.cat.jmag");
			fail("A qualified column name is forbidden in ORDER BY! This test should have failed.");
		} catch(Exception e) {
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \".\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", e.getMessage());
		}

		try {
			// GROUP BY with a SELECT item index
			parser.parseQuery("SELECT * FROM cat GROUP BY 1;");
			fail("A SELECT item index is forbidden in GROUP BY! This test should have failed.");
		} catch(Exception e) {
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \"1\". Was expecting one of: \"\\\"\" <REGULAR_IDENTIFIER_CANDIDATE> ", e.getMessage());
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

	@Test
	public void testDelimitedIdentifiersWithDot(){
		ADQLParser parser = parserFactory.createParser();
		try{
			ADQLQuery query = parser.parseQuery("SELECT * FROM \"B/avo.rad/catalog\";");
			assertEquals("B/avo.rad/catalog", query.getFrom().getTables().get(0).getTableName());
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("The ADQL query is strictly correct! No error should have occured. (see stdout for more details)");
		}
	}

	@Test
	public void testJoinTree(){
		ADQLParser parser = parserFactory.createParser();
		try{
			String[] queries = new String[]{ "SELECT * FROM aTable A JOIN aSecondTable B ON A.id = B.id JOIN aThirdTable C ON B.id = C.id;", "SELECT * FROM aTable A NATURAL JOIN aSecondTable B NATURAL JOIN aThirdTable C;" };
			for(String q : queries) {
				ADQLQuery query = parser.parseQuery(q);

				assertTrue(query.getFrom() instanceof ADQLJoin);

				ADQLJoin join = ((ADQLJoin)query.getFrom());
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

	@Test
	public void test(){
		ADQLParser parser = parserFactory.createParser();
		try{
			ADQLQuery query = parser.parseQuery("SELECT 'truc''machin'  	'bidule' --- why not a comment now ^^\n'FIN' FROM foo;");
			assertNotNull(query);
			assertEquals("truc'machinbiduleFIN", ((StringConstant)(query.getSelect().get(0).getOperand())).getValue());
			assertEquals("'truc''machinbiduleFIN'", query.getSelect().get(0).getOperand().toADQL());
		} catch(Exception ex) {
			fail("String litteral concatenation is perfectly legal according to the ADQL standard.");
		}

		// With a comment ending the query
		try {
			ADQLQuery query = parser.parseQuery("SELECT TOP 1 * FROM ivoa.ObsCore -- comment");
			assertNotNull(query);
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("String litteral concatenation is perfectly legal according to the ADQL standard.");
		}
	}

	@Test
	public void testIncorrectCharacter() {
		/* An identifier must be written only with digits, an underscore or
		 * regular latin characters: */
		try{
			(parserFactory.createParser()).parseQuery("select gr\u00e9gory FROM aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().startsWith("Incorrect character encountered at l.1, c.10: "));
			assertTrue(t.getMessage().endsWith("Possible cause: a non-ASCI/UTF-8 character (solution: remove/replace it)."));
		}

		/* Un-finished double/single quoted string: */
		try{
			(parserFactory.createParser()).parseQuery("select \"stuff FROM aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().startsWith("Incorrect character encountered at l.1, c.26: <EOF>"));
			assertTrue(t.getMessage().endsWith("Possible cause: a string between single or double quotes which is never closed (solution: well...just close it!)."));
		}

		// But in a string, delimited identifier or a comment, it is fine:
		try{
			(parserFactory.createParser()).parseQuery("select 'gr\u00e9gory' FROM aTable");
			(parserFactory.createParser()).parseQuery("select \"gr\u00e9gory\" FROM aTable");
			(parserFactory.createParser()).parseQuery("select * FROM aTable -- a comment by Gr\u00e9gory");
		}catch(Throwable t){
			fail("This error should never occurs because all these queries have an accentuated character but at a correct place.");
		}
	}

	@Test
	public void testMultipleSpacesInOrderAndGroupBy(){
		try{
			ADQLParser parser = parserFactory.createParser();

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

	@Test
	public void testADQLReservedWord(){
		ADQLParser parser = parserFactory.createParser();

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

	@Test
	public void testSQLReservedWord(){
		ADQLParser parser = parserFactory.createParser();

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

	@Test
	public void testUDFName(){
		ADQLParser parser = parserFactory.createParser();
		// TODO [ADQL-2.1] Add the support for this specific UDF in the the FeatureSet!

		// CASE: Valid UDF name => OK
		try {
			parser.parseQuery("SELECT foo(p1,p2) FROM aTable");
		} catch(Throwable t) {
			t.printStackTrace();
			fail("Unexpected parsing error! This query should have passed. (see console for more details)");
		}

		// CASE: Invalid UDF name => ParseException
		final String[] functionsToTest = new String[]{ "_foo", "2do", "do!" };
		for(String fct : functionsToTest){
			try{
				parser.parseQuery("SELECT " + fct + "(p1,p2) FROM aTable");
				fail("A UDF name like \"" + fct + "\" is not allowed by the ADQL grammar. This query should not pass.");
			} catch(Throwable t) {
				assertEquals(ParseException.class, t.getClass());
				assertEquals("Invalid (User Defined) Function name: \"" + fct + "\"!", t.getMessage());
			}
		}
	}

	@Test
	public void testTryQuickFix(){
		ADQLParser parser = parserFactory.createParser();

		try {
			/* CASE: Nothing to fix => exactly the same as provided */
			// raw ASCII query with perfectly regular ADQL identifiers:
			assertEquals("SELECT foo, bar FROM aTable", parser.tryQuickFix("SELECT foo, bar FROM aTable"));
			// same with \n, \r and \t (replaced by 4 spaces):
			assertEquals("SELECT foo," + System.getProperty("line.separator") + "    bar" + System.getProperty("line.separator") + "FROM aTable", parser.tryQuickFix("SELECT foo,\r\n\tbar\nFROM aTable"));
			// still ASCII query with delimited identifiers and ADQL functions:
			assertEquals("SELECT \"foo\"," + System.getProperty("line.separator") + "    \"_bar\", AVG(col1)" + System.getProperty("line.separator") + "FROM \"public\".aTable", parser.tryQuickFix("SELECT \"foo\",\r\n\t\"_bar\", AVG(col1)\nFROM \"public\".aTable"));

			/* CASE: Unicode confusable characters => replace by their ASCII alternative */
			assertEquals("SELECT \"_bar\" FROM aTable", parser.tryQuickFix("SELECT \"\uFE4Dbar\" FROM aTable"));

			/* CASE: incorrect regular identifier */
			assertEquals("SELECT \"_bar\" FROM aTable", parser.tryQuickFix("SELECT _bar FROM aTable"));
			assertEquals("SELECT \"_bar\" FROM aTable", parser.tryQuickFix("SELECT \uFE4Dbar FROM aTable"));
			assertEquals("SELECT \"2mass_id\" FROM aTable", parser.tryQuickFix("SELECT 2mass_id FROM aTable"));
			assertEquals("SELECT \"col?\" FROM aTable", parser.tryQuickFix("SELECT col? FROM aTable"));
			assertEquals("SELECT \"col[2]\" FROM aTable", parser.tryQuickFix("SELECT col[2] FROM aTable"));

			/* CASE: SQL reserved keyword */
			assertEquals("SELECT \"date\", \"year\", \"user\" FROM \"public\".aTable", parser.tryQuickFix("SELECT date, year, user FROM public.aTable"));

			/* CASE: ADQL function name without parameters list */
			assertEquals("SELECT \"count\", \"distance\" FROM \"schema\".aTable", parser.tryQuickFix("SELECT count, distance FROM schema.aTable"));

			/* CASE: a nice combination of everything (with comments at beginning, middle and end) */
			assertEquals("-- begin comment" + System.getProperty("line.separator") + "SELECT id, \"_raj2000\", \"distance\", (\"date\")," + System.getProperty("line.separator") + "    \"min\",min(mag), \"_dej2000\" -- in-between commment" + System.getProperty("line.separator") + "FROM \"public\".mytable -- end comment", parser.tryQuickFix("-- begin comment\r\nSELECT id, \uFE4Draj2000, distance, (date),\r\tmin,min(mag), \"_dej2000\" -- in-between commment\nFROM public.mytable -- end comment"));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected parsing error! This query should have passed. (see console for more details)");
		}
	}

	@Test
	public void testOptionalFeatures() {
		ADQLParser parser = parserFactory.createParser(ADQLVersion.V2_0);

		// CASE: No support for the ADQL-2.1 function - LOWER => ERROR
		try {
			parser.parseQuery("SELECT LOWER(foo) FROM aTable");
			fail("The function \"LOWER\" is not yet supported in ADQL-2.0. This query should not pass.");
		} catch(Throwable t) {
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().contains("(HINT: \"LOWER\" is not supported in ADQL v2.0, but is however a reserved word."));
		}

		// CASE: LOWER supported by default in ADQL-2.1 => OK
		parser = parserFactory.createParser(ADQLVersion.V2_1);
		try {
			ADQLQuery q = parser.parseQuery("SELECT LOWER(foo) FROM aTable");
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
	}

}
