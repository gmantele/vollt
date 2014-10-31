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
