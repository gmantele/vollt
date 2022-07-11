package adql.translator;

import static adql.translator.TestJDBCTranslator.countFeatures;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBChecker;
import adql.db.DBTable;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.ADQLQuery;
import adql.query.ADQLSet;
import adql.query.constraint.ComparisonOperator;
import adql.query.operand.function.InUnitFunction;

public class TestSQLServerTranslator {

	private List<DBTable> tables = null;

	@Before
	public void setUp() throws Exception {
		tables = new ArrayList<DBTable>(2);
		DefaultDBTable t = new DefaultDBTable("aTable");
		t.addColumn(new DefaultDBColumn("id", t));
		t.addColumn(new DefaultDBColumn("name", t));
		t.addColumn(new DefaultDBColumn("aColumn", t));
		tables.add(t);
		t = new DefaultDBTable("anotherTable");
		t.addColumn(new DefaultDBColumn("id", t));
		t.addColumn(new DefaultDBColumn("name", t));
		t.addColumn(new DefaultDBColumn("anotherColumn", t));
		tables.add(t);
	}

	@Test
	public void testTranslateSetOperation() {
		SQLServerTranslator tr = new SQLServerTranslator();
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		try {
			// CASE: A simple UNION
			ADQLSet query = parser.parseQuery("SELECT * FROM foo UNION SELECT * FROM bar");
			assertEquals("SELECT *\nFROM foo\nUNION\nSELECT *\nFROM bar", tr.translate(query));

			// CASE: With quantifier ALL
			query = parser.parseQuery("SELECT * FROM foo EXCEPT ALL SELECT * FROM bar");
			assertEquals("SELECT *\nFROM foo\nEXCEPT ALL\nSELECT *\nFROM bar", tr.translate(query));

			// CASE: With a TOP:
			query = parser.parseQuery("SELECT TOP 10 * FROM foo EXCEPT ALL SELECT TOP 20 * FROM bar");
			assertEquals("SELECT TOP 10 *\nFROM foo\nEXCEPT ALL\nSELECT TOP 20 *\nFROM bar", tr.translate(query));

			// CASE: With ORDER BY or OFFSET:
			query = parser.parseQuery("(SELECT * FROM foo ORDER BY id DESC) INTERSECT (SELECT * FROM bar WHERE mag < 5 OFFSET 10)");
			assertTrue(tr.translate(query).matches("SELECT \\* FROM\n\\(SELECT \\*\nFROM foo\nORDER BY id DESC\\) AS t[0-9]+_1\nINTERSECT\nSELECT \\* FROM\n\\(SELECT \\*\nFROM bar\nWHERE mag < 5\nORDER BY 1 ASC\nOFFSET 10 ROWS\\) AS t[0-9]+_2"));

			// CASE: CTE at the top:
			query = parser.parseQuery("WITH toto AS (SELECT * FROM tt) SELECT first_col FROM foo INTERSECT SELECT col1 FROM toto");
			assertEquals("WITH \"toto\" AS (\nSELECT *\nFROM tt\n)\nSELECT first_col AS \"first_col\"\nFROM foo\nINTERSECT\nSELECT col1 AS \"col1\"\nFROM toto", tr.translate(query));

			// CASE: Set operation with a grouped set operation:
			query = parser.parseQuery("( SELECT col1 FROM foo WHERE col1 <= 10 UNION SELECT col1 FROM foo WHERE col1 > 120406 ) INTERSECT SELECT col1 FROM foo WHERE (col1 <= 10 OR col1> 120406) AND mod(col1, 2) = 0 ORDER BY 1 DESC");
			assertTrue(tr.translate(query).matches("SELECT \\* FROM\n\\(SELECT col1 AS \"col1\"\nFROM foo\nWHERE col1 <= 10\nUNION\nSELECT col1 AS \"col1\"\nFROM foo\nWHERE col1 > 120406\\) AS t[0-9]+_1\nINTERSECT\nSELECT col1 AS \"col1\"\nFROM foo\nWHERE \\(col1 <= 10 OR col1 > 120406\\) AND convert\\(numeric, col1\\) % convert\\(numeric, 2\\) = 0\nORDER BY 1 DESC"));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected parsing failure! (see console for more details)");
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while translating a correct SET operation! (see console for more details)");
		}
	}

	@Test
	public void testTranslateOffset() {
		SQLServerTranslator tr = new SQLServerTranslator();
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		try {

			// CASE: Only OFFSET = 0 => No OFFSET in SQL
			assertEquals("SELECT *\nFROM foo", tr.translate(parser.parseQuery("Select * From foo OffSet 0")));

			// CASE: Only TOP (or with OFFSET=0)
			assertEquals("SELECT TOP 5 *\nFROM foo", tr.translate(parser.parseQuery("Select Top 5 * From foo")));
			assertEquals("SELECT TOP 5 *\nFROM foo", tr.translate(parser.parseQuery("Select Top 5 * From foo Offset 0")));

			// CASE: Only OFFSET
			assertEquals("SELECT *\nFROM foo\nORDER BY 1 ASC\nOFFSET 10 ROWS", tr.translate(parser.parseQuery("Select * From foo OffSet 10")));

			// CASE: TOP + OFFSET but no ORDER BY
			assertEquals("SELECT *\nFROM foo\nORDER BY 1 ASC\nOFFSET 10 ROWS FETCH NEXT 5 ROWS ONLY", tr.translate(parser.parseQuery("Select Top 5 * From foo OffSet 10")));

			// CASE: TOP + OFFSET + ORDER BY
			assertEquals("SELECT *\nFROM foo\nORDER BY id DESC\nOFFSET 10 ROWS FETCH NEXT 5 ROWS ONLY", tr.translate(parser.parseQuery("Select Top 5 * From foo Order By id Desc OffSet 10")));

		} catch(ParseException pe) {
			pe.printStackTrace(System.err);
			fail("Unexpected failed query parsing! (see console for more details)");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate a query with offset into SQL.");
		}
	}

	@Test
	public void testNaturalJoin() {
		final String adqlquery = "SELECT id, name, aColumn, anotherColumn FROM aTable A NATURAL JOIN anotherTable B;";

		try {
			ADQLParser parser = new ADQLParser();
			parser.setQueryChecker(new DBChecker(tables));
			parser.setQueryFactory(new SQLServer_ADQLQueryFactory());
			ADQLSet query = parser.parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			// Test the FROM part:
			assertEquals("\"aTable\" AS \"a\" INNER JOIN \"anotherTable\" AS \"b\" ON \"a\".\"id\"=\"b\".\"id\" AND \"a\".\"name\"=\"b\".\"name\"", translator.translate(((ADQLQuery)query).getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to NATURAL) are actually translated as columns of the first joined table):
			assertEquals("SELECT \"a\".\"id\" AS \"id\" , \"a\".\"name\" AS \"name\" , \"a\".\"aColumn\" AS \"acolumn\" , \"b\".\"anotherColumn\" AS \"anothercolumn\"", translator.translate(((ADQLQuery)query).getSelect()));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		} catch(TranslationException te) {
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

	@Test
	public void testJoinWithUSING() {
		final String adqlquery = "SELECT B.id, name, aColumn, anotherColumn FROM aTable A JOIN anotherTable B USING(name);";

		try {
			ADQLParser parser = new ADQLParser();
			parser.setQueryChecker(new DBChecker(tables));
			parser.setQueryFactory(new SQLServer_ADQLQueryFactory());
			ADQLSet query = parser.parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			// Test the FROM part:
			assertEquals("\"aTable\" AS \"a\" INNER JOIN \"anotherTable\" AS \"b\" ON \"a\".\"name\"=\"b\".\"name\"", translator.translate(((ADQLQuery)query).getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to USING) are actually translated as columns of the first joined table):
			assertEquals("SELECT \"b\".\"id\" AS \"id\" , \"a\".\"name\" AS \"name\" , \"a\".\"aColumn\" AS \"acolumn\" , \"b\".\"anotherColumn\" AS \"anothercolumn\"", translator.translate(((ADQLQuery)query).getSelect()));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		} catch(TranslationException te) {
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

	@Test
	public void testConcat() {
		try {
			SQLServerTranslator translator = new SQLServerTranslator();

			ADQLParser parser = new ADQLParser();
			parser.setQueryFactory(new SQLServer_ADQLQueryFactory());

			// Test with an easy translation:
			ADQLSet query = parser.parseQuery("SELECT 'abc' || ' ' || 'def' FROM aTable");
			assertEquals("SELECT 'abc' + ' ' + 'def' AS \"concat\"", translator.translate(((ADQLQuery)query).getSelect()));

			// Test with an easy translation:
			query = parser.parseQuery("SELECT 'a||b||c' || ' ' || 'd+e|f' FROM aTable");
			assertEquals("SELECT 'a||b||c' + ' ' + 'd+e|f' AS \"concat\"", translator.translate(((ADQLQuery)query).getSelect()));

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
		final FeatureSet supportedFeatures = (new SQLServerTranslator()).getSupportedFeatures();

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
