package adql.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import adql.parser.ADQLParser;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.MathFunction;

public class TestSimpleReplaceHandler {

	@Before
	public void setUp() throws Exception{}

	@Test
	public void testReplaceRecursiveMatch(){
		/* WHY THIS TEST?
		 * 
		 * When a match item had also a match item inside it (e.g. function parameter or sub-query),
		 * both matched items (e.g. the parent and the child) must be replaced.
		 * 
		 * However, if the parent is replaced first, the reference of the new parent is lost by the SimpleReplaceHandler and so,
		 * the replacement of the child will be performed on the former parent. Thus, after the whole process,
		 * in the final ADQL query, the replacement of the child won't be visible since the former parent is
		 * not referenced any more.
		 */

		String testQuery = "SELECT SQRT(ABS(81)) FROM myTable";
		try{
			// Parse the query:
			ADQLQuery query = (new ADQLParser()).parseQuery(testQuery);

			// Check it is as expected, before the replacements:
			assertEquals(testQuery, query.toADQL().replaceAll("\\n", " "));

			// Create a replace handler:
			SimpleReplaceHandler replaceHandler = new SimpleReplaceHandler(){
				@Override
				protected boolean match(ADQLObject obj){
					return obj instanceof MathFunction;
				}

				@Override
				protected ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException{
					return new DefaultUDF("foo", ((MathFunction)objToReplace).getParameters());
				}
			};

			// Apply the replacement of all mathematical functions by a dumb UDF:
			replaceHandler.searchAndReplace(query);
			assertEquals(2, replaceHandler.getNbMatch());
			assertEquals(replaceHandler.getNbMatch(), replaceHandler.getNbReplacement());
			assertEquals("SELECT foo(foo(81)) FROM myTable", query.toADQL().replaceAll("\\n", " "));

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("No error should have occured here since nothing is wrong in the ADQL query used for the test. See the stack trace in the console for more details.");
		}

	}

}
