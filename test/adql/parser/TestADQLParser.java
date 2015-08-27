package adql.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.query.ADQLQuery;
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
			parser.parseQuery("SELECT * FROM cat GROUP BY 1;");
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
			// GROUP BY
			parser.parseQuery("SELECT * FROM cat GROUP BY cat.oid;");
			fail("A qualified column name is forbidden in ORDER BY! This test should have failed.");
		}catch(Exception e){
			assertEquals(ParseException.class, e.getClass());
			assertEquals(" Encountered \".\". Was expecting one of: <EOF> \",\" \";\" \"HAVING\" \"ORDER BY\" ", e.getMessage());
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
	public void test(){
		ADQLParser parser = new ADQLParser();
		try{
			ADQLQuery query = parser.parseQuery("SELECT 'truc''machin'  	'bidule' -- why not a comment now ^^\n'FIN' FROM foo;");
			assertNotNull(query);
			assertEquals("truc'machinbiduleFIN", ((StringConstant)(query.getSelect().get(0).getOperand())).getValue());
			assertEquals("'truc''machinbiduleFIN'", query.getSelect().get(0).getOperand().toADQL());
		}catch(Exception ex){
			fail("String litteral concatenation is perfectly legal according to the ADQL standard.");
		}
	}

}
