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
import adql.query.operand.ADQLColumn;
import adql.query.operand.function.CastFunction;
import adql.query.operand.function.DatatypeParam;
import adql.query.operand.function.InUnitFunction;

public class TestMySQLTranslator {

	@Test
	public void testTranslateCast() {
		JDBCTranslator tr = new MySQLTranslator();
		try {
			for(DatatypeParam.DatatypeName datatype : DatatypeParam.DatatypeName.values()) {
				CastFunction castFn = new CastFunction(new ADQLColumn("aColumn"), new DatatypeParam(datatype));
				switch(datatype) {

					// All integers into `SIGNED INTEGER`:
					case SMALLINT:
					case INTEGER:
					case BIGINT:
						assertEquals("SIGNED INTEGER", tr.translate(castFn.getTargetType()));
						assertEquals("CAST(aColumn AS SIGNED INTEGER)", tr.translate(castFn));
						break;

					// No VARCHAR[(n)] => CHAR[(n)]
					case VARCHAR:
						assertEquals("CHAR", tr.translate(castFn.getTargetType()));
						assertEquals("CAST(aColumn AS CHAR)", tr.translate(castFn));
						castFn = new CastFunction(new ADQLColumn("aColumn"), new DatatypeParam(datatype, 1));
						assertEquals("CHAR(1)", tr.translate(castFn.getTargetType()));
						assertEquals("CAST(aColumn AS CHAR(1))", tr.translate(castFn));
						break;

					// TIMESTAMP into `DATETIME`:
					case TIMESTAMP:
						assertEquals("DATETIME", tr.translate(castFn.getTargetType()));
						assertEquals("CAST(aColumn AS DATETIME)", tr.translate(castFn));
						break;

					// All others are the same as in ADQL:
					default:
						assertEquals(datatype.toString(), tr.translate(castFn.getTargetType()));
						assertEquals(castFn.toADQL(), tr.translate(castFn));
				}
			}
		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected parsing failure! (see console for more details)");
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while translating a correct CAST function! (see console for more details)");
		}
	}

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

		// Create the list of all expected supported features:
		final FeatureSet expectedFeatures = new FeatureSet(true);
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
