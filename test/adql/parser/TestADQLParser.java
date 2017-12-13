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

import adql.query.ADQLQuery;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.operand.StringConstant;

public class TestADQLParser {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{}

	@Before
	public void setUp() throws Exception{}

	@After
	public void tearDown() throws Exception{}

	@Test
	public void testColumnReference(){
		ADQLParser parser = new ADQLParser();
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
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("These ADQL queries are strictly correct! No error should have occured. (see stdout for more details)");
		}

		try{
			// ORDER BY
			parser.parseQuery("SELECT * FROM cat ORDER BY cat.oid;");
			fail("A qualified column name is forbidden in ORDER BY! This test should have failed.");
		}catch(Exception e){
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \".\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", e.getMessage());
		}

		// Query reported as in error before the bug correction:
		try{
			parser.parseQuery("SELECT TOP 10 browndwarfs.cat.jmag FROM browndwarfs.cat ORDER BY browndwarfs.cat.jmag");
			fail("A qualified column name is forbidden in ORDER BY! This test should have failed.");
		}catch(Exception e){
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \".\". Was expecting one of: <EOF> \",\" \";\" \"ASC\" \"DESC\" ", e.getMessage());
		}

		try{
			// GROUP BY with a SELECT item index
			parser.parseQuery("SELECT * FROM cat GROUP BY 1;");
			fail("A SELECT item index is forbidden in GROUP BY! This test should have failed.");
		}catch(Exception e){
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \"1\". Was expecting one of: \"\\\"\" <REGULAR_IDENTIFIER> ", e.getMessage());
		}

		try{
			// JOIN ... USING(...)
			parser.parseQuery("SELECT * FROM cat JOIN cat2 USING(cat.oid);");
			fail("A qualified column name is forbidden in USING(...)! This test should have failed.");
		}catch(Exception e){
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \".\". Was expecting one of: \")\" \",\" ", e.getMessage());
		}

		try{
			// JOIN ... USING(...)
			parser.parseQuery("SELECT * FROM cat JOIN cat2 USING(1);");
			fail("A column index is forbidden in USING(...)! This test should have failed.");
		}catch(Exception e){
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \"1\". Was expecting one of: \"\\\"\" <REGULAR_IDENTIFIER> ", e.getMessage());
		}
	}

	@Test
	public void testDelimitedIdentifiersWithDot(){
		ADQLParser parser = new ADQLParser();
		try{
			ADQLQuery query = parser.parseQuery("SELECT * FROM \"B/avo.rad/catalog\";");
			assertEquals("B/avo.rad/catalog", query.getFrom().getTables().get(0).getTableName());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("The ADQL query is strictly correct! No error should have occured. (see stdout for more details)");
		}
	}

	@Test
	public void testJoinTree(){
		ADQLParser parser = new ADQLParser();
		try{
			String[] queries = new String[]{"SELECT * FROM aTable A JOIN aSecondTable B ON A.id = B.id JOIN aThirdTable C ON B.id = C.id;","SELECT * FROM aTable A NATURAL JOIN aSecondTable B NATURAL JOIN aThirdTable C;"};
			for(String q : queries){
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
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("The ADQL query is strictly correct! No error should have occured. (see stdout for more details)");
		}
	}

	@Test
	public void test(){
		ADQLParser parser = new ADQLParser();
		try{
			ADQLQuery query = parser.parseQuery("SELECT 'truc''machin'  	'bidule' --- why not a comment now ^^\n'FIN' FROM foo;");
			assertNotNull(query);
			assertEquals("truc'machinbiduleFIN", ((StringConstant)(query.getSelect().get(0).getOperand())).getValue());
			assertEquals("'truc''machinbiduleFIN'", query.getSelect().get(0).getOperand().toADQL());
		}catch(Exception ex){
			fail("String litteral concatenation is perfectly legal according to the ADQL standard.");
		}

		// With a comment ending the query
		try{
			ADQLQuery query = parser.parseQuery("SELECT TOP 1 * FROM ivoa.ObsCore -- comment");
			assertNotNull(query);
		}catch(Exception ex){
			ex.printStackTrace();
			fail("String litteral concatenation is perfectly legal according to the ADQL standard.");
		}
	}

	@Test
	public void testIncorrectCharacter(){
		/* An identifier must be written only with digits, an underscore or
		 * regular latin characters: */
		try{
			(new ADQLParser()).parseQuery("select grégory FROM aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().startsWith("Incorrect character encountered at l.1, c.10: \"\\u00e9\" ('"));
		}

		// But in a string, delimited identifier or a comment, it is fine:
		try{
			(new ADQLParser()).parseQuery("select 'grégory' FROM aTable");
			(new ADQLParser()).parseQuery("select \"grégory\" FROM aTable");
			(new ADQLParser()).parseQuery("select * FROM aTable -- a comment by Grégory");
		}catch(Throwable t){
			fail("This error should never occurs because all these queries have an accentuated character but at a correct place.");
		}
	}

	@Test
	public void testMultipleSpacesInOrderAndGroupBy(){
		try{
			ADQLParser parser = new ADQLParser();

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
		}catch(Throwable t){
			t.printStackTrace();
			fail("Having multiple space characters between the ORDER/GROUP and the BY keywords should not generate any parsing error.");
		}
	}

	@Test
	public void testADQLReservedWord(){
		ADQLParser parser = new ADQLParser();

		final String hintAbs = "\n(HINT: \"abs\" is a reserved ADQL word. To use it as a column/table/schema name/alias, write it between double quotes.)";
		final String hintPoint = "\n(HINT: \"point\" is a reserved ADQL word. To use it as a column/table/schema name/alias, write it between double quotes.)";
		final String hintExists = "\n(HINT: \"exists\" is a reserved ADQL word. To use it as a column/table/schema name/alias, write it between double quotes.)";
		final String hintLike = "\n(HINT: \"LIKE\" is a reserved ADQL word. To use it as a column/table/schema name/alias, write it between double quotes.)";

		/* TEST AS A COLUMN/TABLE/SCHEMA NAME... */
		// ...with a numeric function name (but no param):
		try{
			parser.parseQuery("select abs from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintAbs));
		}
		// ...with a geometric function name (but no param):
		try{
			parser.parseQuery("select point from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintPoint));
		}
		// ...with an ADQL function name (but no param):
		try{
			parser.parseQuery("select exists from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintExists));
		}
		// ...with an ADQL syntax item:
		try{
			parser.parseQuery("select LIKE from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintLike));
		}

		/* TEST AS AN ALIAS... */
		// ...with a numeric function name (but no param):
		try{
			parser.parseQuery("select aCol AS abs from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintAbs));
		}
		// ...with a geometric function name (but no param):
		try{
			parser.parseQuery("select aCol AS point from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintPoint));
		}
		// ...with an ADQL function name (but no param):
		try{
			parser.parseQuery("select aCol AS exists from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintExists));
		}
		// ...with an ADQL syntax item:
		try{
			parser.parseQuery("select aCol AS LIKE from aTable");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintLike));
		}

		/* TEST AT THE END OF THE QUERY (AND IN A WHERE) */
		try{
			parser.parseQuery("select aCol from aTable WHERE toto = abs");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith(hintAbs));
		}
	}

	@Test
	public void testSQLReservedWord(){
		ADQLParser parser = new ADQLParser();

		try{
			parser.parseQuery("SELECT rows FROM aTable");
			fail("\"ROWS\" is an SQL reserved word. This query should not pass.");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith("\n(HINT: \"rows\" is not supported in ADQL, but is however a reserved word. To use it as a column/table/schema name/alias, write it between double quotes.)"));
		}

		try{
			parser.parseQuery("SELECT CASE WHEN aCol = 2 THEN 'two' ELSE 'smth else' END as str FROM aTable");
			fail("ADQL does not support the CASE syntax. This query should not pass.");
		}catch(Throwable t){
			assertEquals(ParseException.class, t.getClass());
			assertTrue(t.getMessage().endsWith("\n(HINT: \"CASE\" is not supported in ADQL, but is however a reserved word. To use it as a column/table/schema name/alias, write it between double quotes.)"));
		}
	}

}
