package adql.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import adql.parser.ADQLParserFactory;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.MathFunction;
import adql.query.operand.function.MathFunctionType;

public class TestSimpleReplaceHandler {

	ADQLParserFactory parserFactory = new ADQLParserFactory();

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testReplaceRecursiveMatch() {
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
		try {
			// Parse the query:
			ADQLQuery query = parserFactory.createParser().parseQuery(testQuery);

			// Check it is as expected, before the replacements:
			assertEquals(testQuery, query.toADQL().replaceAll("\\n", " "));

			// Create a replace handler:
			SimpleReplaceHandler replaceHandler = new SimpleReplaceHandler() {
				@Override
				protected boolean match(ADQLObject obj) {
					return obj instanceof MathFunction;
				}

				@Override
				protected ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException {
					return new DefaultUDF("foo", ((MathFunction)objToReplace).getParameters());
				}
			};

			// Apply the replacement of all mathematical functions by a dumb UDF:
			replaceHandler.searchAndReplace(query);
			assertEquals(2, replaceHandler.getNbMatch());
			assertEquals(replaceHandler.getNbMatch(), replaceHandler.getNbReplacement());
			assertEquals("SELECT foo(foo(81)) FROM myTable", query.toADQL().replaceAll("\\n", " "));

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("No error should have occured here since nothing is wrong in the ADQL query used for the test. See the stack trace in the console for more details.");
		}

	}

	@Test
	public void testWrappingReplacement() {
		/* WHY THIS TEST?
		 *
		 * In case you just want to wrap a matched object, you replace it by the wrapping object initialized
		 * with the matched object.
		 *
		 * In a first version, the replacement was done and then the ReplaceHandler was going inside the new object to replace
		 * other matching objects. But of course, it will find again the first matched object and will wrap it again, and so on
		 * indefinitely => "nasty" infinite loop.
		 *
		 * So, the replacement of the matched objects should be always done after having looked inside it.
		 */

		String testQuery = "SELECT foo(bar(123)) FROM myTable";
		try {
			// Parse the query:
			ADQLQuery query = parserFactory.createParser().parseQuery(testQuery);

			// Check it is as expected, before the replacements:
			assertEquals(testQuery, query.toADQL().replaceAll("\\n", " "));

			// Create a replace handler:
			SimpleReplaceHandler replaceHandler = new SimpleReplaceHandler() {
				@Override
				protected boolean match(ADQLObject obj) {
					return obj instanceof DefaultUDF && ((DefaultUDF)obj).getName().toLowerCase().matches("(foo|bar)");
				}

				@Override
				protected ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException {
					try {
						return new MathFunction(MathFunctionType.ROUND, (DefaultUDF)objToReplace);
					} catch(Exception e) {
						e.printStackTrace(System.err);
						fail("No error should have occured here since nothing is wrong in the ADQL query used for the test. See the stack trace in the console for more details.");
						return null;
					}
				}
			};

			// Apply the wrapping:
			replaceHandler.searchAndReplace(query);
			assertEquals(2, replaceHandler.getNbMatch());
			assertEquals(replaceHandler.getNbMatch(), replaceHandler.getNbReplacement());
			assertEquals("SELECT ROUND(foo(ROUND(bar(123)))) FROM myTable", query.toADQL().replaceAll("\\n", " "));

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("No error should have occured here since nothing is wrong in the ADQL query used for the test. See the stack trace in the console for more details.");
		}
	}

}
