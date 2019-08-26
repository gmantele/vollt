package adql.translator;

import static adql.translator.TestJDBCTranslator.countFeatures;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.parser.ADQLParser;
import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.ADQLQuery;
import adql.query.constraint.ComparisonOperator;
import adql.query.operand.function.InUnitFunction;

public class TestMySQLTranslator {

	@Test
	public void testConcat() {
		try {
			ADQLParser parser = new ADQLParser();
			MySQLTranslator translator = new MySQLTranslator();

			// Test with an easy translation:
			ADQLQuery query = parser.parseQuery("SELECT 'abc' || ' ' || 'def' FROM aTable");
			assertEquals("SELECT CONCAT('abc', ' ', 'def') AS `concat`", translator.translate(query.getSelect()));

			// Test with an easy translation:
			query = parser.parseQuery("SELECT 'a||b||c' || ' ' || 'd+e|f' FROM aTable");
			assertEquals("SELECT CONCAT('a||b||c', ' ', 'd+e|f') AS `concat`", translator.translate(query.getSelect()));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		} catch(TranslationException te) {
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

	@Test
	public void testSupportedFeatures() {
		final FeatureSet supportedFeatures = (new MySQLTranslator()).getSupportedFeatures();

		// TEST: Not NULL:
		assertNotNull(supportedFeatures);

		// TEST: Any UDF should be allowed, by default:
		assertTrue(supportedFeatures.isAnyUdfAllowed());

		// Create the list of all expected supported features:
		final FeatureSet expectedFeatures = new FeatureSet(true, true);
		expectedFeatures.unsupportAll(LanguageFeature.TYPE_ADQL_GEO);
		expectedFeatures.unsupport(ComparisonOperator.ILIKE.getFeatureDescription());
		expectedFeatures.unsupport(InUnitFunction.FEATURE);

		// TEST: same number of features:
		assertEquals(countFeatures(expectedFeatures), countFeatures(supportedFeatures));

		// TEST: same features:
		for(LanguageFeature expected : expectedFeatures)
			assertTrue(supportedFeatures.isSupporting(expected));
	}

}
