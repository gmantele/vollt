package adql.translator;

import static adql.translator.TestJDBCTranslator.countFeatures;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.query.operand.NumericConstant;
import adql.query.operand.function.InUnitFunction;
import adql.query.operand.function.MathFunction;
import adql.query.operand.function.MathFunctionType;

public class TestPostgreSQLTranslator {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testTranslateMathFunction() {
		// Check that all math functions, except PI, operates a cast to their DOUBLE/REAL parameters:
		PostgreSQLTranslator trans = new PostgreSQLTranslator();
		MathFunctionType[] types = MathFunctionType.values();
		NumericConstant num = new NumericConstant("1.234"), prec = new NumericConstant("2");
		for(MathFunctionType type : types) {
			try {
				switch(type) {
					case PI:
						assertEquals("PI()", trans.translate(new MathFunction(type)));
						break;
					case RAND:
						assertEquals("random()", trans.translate(new MathFunction(type)));
						assertEquals("random()", trans.translate(new MathFunction(type, num)));
						break;
					case LOG:
						assertEquals("ln(CAST(1.234 AS numeric))", trans.translate(new MathFunction(type, num)));
						break;
					case LOG10:
						assertEquals("log(10, CAST(1.234 AS numeric))", trans.translate(new MathFunction(type, num)));
						break;
					case TRUNCATE:
						assertEquals("trunc(CAST(1.234 AS numeric))", trans.translate(new MathFunction(type, num)));
						assertEquals("trunc(CAST(1.234 AS numeric), 2)", trans.translate(new MathFunction(type, num, prec)));
						break;
					case ROUND:
						assertEquals("round(CAST(1.234 AS numeric))", trans.translate(new MathFunction(type, num)));
						assertEquals("round(CAST(1.234 AS numeric), 2)", trans.translate(new MathFunction(type, num, prec)));
						break;
					default:
						if (type.nbMaxParams() == 1 || type.nbMinParams() == 1)
							assertEquals(type + "(CAST(1.234 AS numeric))", trans.translate(new MathFunction(type, num)));
						if (type.nbMaxParams() == 2)
							assertEquals(type + "(CAST(1.234 AS numeric), CAST(1.234 AS numeric))", trans.translate(new MathFunction(type, num, num)));
						break;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Translation exception for the type \"" + type + "\": " + ex.getMessage());
			}
		}
	}

	@Test
	public void testSupportedFeatures() {
		final FeatureSet supportedFeatures = (new PostgreSQLTranslator()).getSupportedFeatures();

		// TEST: Not NULL:
		assertNotNull(supportedFeatures);

		// TEST: Any UDF should be allowed, by default:
		assertTrue(supportedFeatures.isAnyUdfAllowed());

		// Create the list of all expected supported features:
		final FeatureSet expectedFeatures = new FeatureSet(true, true);
		expectedFeatures.unsupportAll(LanguageFeature.TYPE_ADQL_GEO);
		expectedFeatures.unsupport(InUnitFunction.FEATURE);

		// TEST: same number of features:
		assertEquals(countFeatures(expectedFeatures), countFeatures(supportedFeatures));

		// TEST: same features:
		for(LanguageFeature expected : expectedFeatures)
			assertTrue(supportedFeatures.isSupporting(expected));
	}

}
