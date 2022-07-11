package adql.translator;

import static adql.translator.TestJDBCTranslator.countFeatures;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.ADQLQuery;
import adql.query.ADQLSet;
import adql.query.constraint.ComparisonOperator;
import adql.query.operand.ADQLColumn;
import adql.query.operand.function.InUnitFunction;
import adql.query.operand.function.cast.CastFunction;
import adql.query.operand.function.cast.StandardTargetType;

public class TestMySQLTranslator {

	@Test
	public void testTranslateSetOperation() {
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);
		MySQLTranslator tr = new MySQLTranslator();

		try {
			// CASE: A simple UNION
			ADQLSet query = parser.parseQuery("SELECT * FROM foo UNION SELECT * FROM bar");
			assertEquals("SELECT *\nFROM foo\nUNION\nSELECT *\nFROM bar", tr.translate(query));

			// CASE: A simple INTERSECT
			query = parser.parseQuery("SELECT col1, col2 FROM foo INTERSECT SELECT c1, c2 FROM bar");
			assertTrue(tr.translate(query).matches("SELECT DISTINCT `t[0-9]+_1`\\.\\*\nFROM \\(\nSELECT col1 AS `col1` , col2 AS `col2`\nFROM foo\\) AS `t[0-9]+_1`\nINNER JOIN \\(\nSELECT c1 AS `c1` , c2 AS `c2`\nFROM bar\\) AS `t[0-9]+_2`\nON `t[0-9]+_1`\\.`col1`=`t[0-9]+_2`\\.`c1` AND `t[0-9]+_1`\\.`col2`=`t[0-9]+_2`\\.`c2`"));

			// CASE: A simple EXCEPT
			query = parser.parseQuery("SELECT col1, col2 FROM foo EXCEPT SELECT c1, c2 FROM bar");
			assertTrue(tr.translate(query).matches("SELECT DISTINCT `t[0-9]+_1`\\.\\*\nFROM \\(\nSELECT col1 AS `col1` , col2 AS `col2`\nFROM foo\\) AS `t[0-9]+_1`\nLEFT JOIN \\(\nSELECT c1 AS `c1` , c2 AS `c2`\nFROM bar\\) AS `t[0-9]+_2`\nON `t[0-9]+_1`\\.`col1`=`t[0-9]+_2`\\.`c1` AND `t[0-9]+_1`\\.`col2`=`t[0-9]+_2`\\.`c2`\nWHERE `t[0-9]+_2`\\.`c1` IS NULL AND `t[0-9]+_2`\\.`c2` IS NULL"));

			// CASE: With quantifier ALL
			query = parser.parseQuery("SELECT * FROM foo UNION ALL SELECT * FROM bar");
			assertEquals("SELECT *\nFROM foo\nUNION ALL\nSELECT *\nFROM bar", tr.translate(query));
			// INTERSECT ALL => same test as with INTERSECT without the DISTINCT
			query = parser.parseQuery("SELECT col1, col2 FROM foo INTERSECT ALL SELECT c1, c2 FROM bar");
			assertTrue(tr.translate(query).matches("SELECT `t[0-9]+_1`\\.\\*\nFROM \\(\nSELECT col1 AS `col1` , col2 AS `col2`\nFROM foo\\) AS `t[0-9]+_1`\nINNER JOIN \\(\nSELECT c1 AS `c1` , c2 AS `c2`\nFROM bar\\) AS `t[0-9]+_2`\nON `t[0-9]+_1`\\.`col1`=`t[0-9]+_2`\\.`c1` AND `t[0-9]+_1`\\.`col2`=`t[0-9]+_2`\\.`c2`"));
			// EXCEPT ALL => same test as with EXCEPT without the DISTINCT
			query = parser.parseQuery("SELECT col1, col2 FROM foo EXCEPT ALL SELECT c1, c2 FROM bar");
			assertTrue(tr.translate(query).matches("SELECT `t[0-9]+_1`\\.\\*\nFROM \\(\nSELECT col1 AS `col1` , col2 AS `col2`\nFROM foo\\) AS `t[0-9]+_1`\nLEFT JOIN \\(\nSELECT c1 AS `c1` , c2 AS `c2`\nFROM bar\\) AS `t[0-9]+_2`\nON `t[0-9]+_1`\\.`col1`=`t[0-9]+_2`\\.`c1` AND `t[0-9]+_1`\\.`col2`=`t[0-9]+_2`\\.`c2`\nWHERE `t[0-9]+_2`\\.`c1` IS NULL AND `t[0-9]+_2`\\.`c2` IS NULL"));

			// CASE: With a TOP:
			query = parser.parseQuery("SELECT TOP 10 * FROM foo UNION ALL SELECT TOP 20 * FROM bar");
			assertEquals("(SELECT *\nFROM foo\nLIMIT 10)\nUNION ALL\n(SELECT *\nFROM bar\nLIMIT 20)", tr.translate(query));

			// CASE: With ORDER BY or OFFSET:
			query = parser.parseQuery("(SELECT col1, col2 FROM foo ORDER BY id DESC) INTERSECT (SELECT col1, col2 FROM bar WHERE mag < 5 OFFSET 10)");
			assertTrue(tr.translate(query).matches("SELECT DISTINCT `t[0-9]+_1`\\.\\*\nFROM \\(\nSELECT col1 AS `col1` , col2 AS `col2`\nFROM foo\nORDER BY id DESC\\) AS `t[0-9]+_1`\nINNER JOIN \\(\nSELECT col1 AS `col1` , col2 AS `col2`\nFROM bar\nWHERE mag < 5\nOFFSET 10\\) AS `t[0-9]+_2`\nON `t[0-9]+_1`\\.`col1`=`t[0-9]+_2`\\.`col1` AND `t[0-9]+_1`\\.`col2`=`t[0-9]+_2`\\.`col2`"));

			// CASE: CTE at the top:
			query = parser.parseQuery("WITH toto AS (SELECT * FROM tt) SELECT first_col FROM foo INTERSECT SELECT col1 FROM toto");
			assertTrue(tr.translate(query).matches("WITH `toto` AS \\(\nSELECT \\*\nFROM tt\n\\)\nSELECT DISTINCT `t[0-9]+_1`\\.\\*\nFROM \\(\nSELECT first_col AS `first_col`\nFROM foo\\) AS `t[0-9]+_1`\nINNER JOIN \\(\nSELECT col1 AS `col1`\nFROM toto\\) AS `t[0-9]+_2`\nON `t[0-9]+_1`\\.`first_col`=`t[0-9]+_2`\\.`col1`"));

			// CASE: Set operation with a grouped set operation:
			query = parser.parseQuery("( SELECT col1 FROM foo WHERE col1 <= 10 UNION SELECT col1 FROM foo WHERE col1 > 120406 ) INTERSECT SELECT col1 FROM foo WHERE (col1 <= 10 OR col1> 120406) AND mod(col1, 2) = 0 ORDER BY 1 DESC");
			assertTrue(tr.translate(query).matches("SELECT DISTINCT `t[0-9]+_1`\\.\\*\nFROM \\(\nSELECT col1 AS `col1`\nFROM foo\nWHERE col1 <= 10\nUNION\nSELECT col1 AS `col1`\nFROM foo\nWHERE col1 > 120406\\) AS `t[0-9]+_1`\nINNER JOIN \\(\nSELECT col1 AS `col1`\nFROM foo\nWHERE \\(col1 <= 10 OR col1 > 120406\\) AND MOD\\(col1, 2\\) = 0\\) AS `t[0-9]+_2`\nON `t[0-9]+_1`.`col1`=`t[0-9]+_2`.`col1`\nORDER BY 1 DESC"));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected parsing failure! (see console for more details)");
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while translating a correct SET operation! (see console for more details)");
		}
	}

	@Test
	public void testTranslateCast() {
		JDBCTranslator tr = new MySQLTranslator();
		try {
			for(DBDatatype datatype : StandardTargetType.getStandardDatatypes()) {
				CastFunction castFn = new CastFunction(new ADQLColumn("aColumn"), new StandardTargetType(new DBType(datatype)));
				switch(datatype) {
					// All integers into `SIGNED INTEGER`:
					case SMALLINT:
					case INTEGER:
					case BIGINT:
						assertEquals("CAST(aColumn AS SIGNED INTEGER)", tr.translate(castFn));
						break;

					// No VARCHAR[(n)] => CHAR[(n)]
					case VARCHAR:
						assertEquals("CAST(aColumn AS CHAR)", tr.translate(castFn));
						castFn = new CastFunction(new ADQLColumn("aColumn"), new StandardTargetType(new DBType(datatype, 1)));
						assertEquals("CAST(aColumn AS CHAR(1))", tr.translate(castFn));
						break;

					// TIMESTAMP into `DATETIME`:
					case TIMESTAMP:
						assertEquals("CAST(aColumn AS DATETIME)", tr.translate(castFn));
						break;

					// All others are the same as in ADQL:
					default:
						assertEquals(castFn.toADQL(), tr.translate(castFn));
						break;
				}
			}
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
			ADQLSet query = parser.parseQuery("SELECT 'abc' || ' ' || 'def' FROM aTable");
			assertEquals("SELECT CONCAT('abc', ' ', 'def') AS `concat`", translator.translate(((ADQLQuery)query).getSelect()));

			// Test with an easy translation:
			query = parser.parseQuery("SELECT 'a||b||c' || ' ' || 'd+e|f' FROM aTable");
			assertEquals("SELECT CONCAT('a||b||c', ' ', 'd+e|f') AS `concat`", translator.translate(((ADQLQuery)query).getSelect()));

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
