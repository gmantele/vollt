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
import adql.query.constraint.ComparisonOperator;
import adql.query.operand.ADQLColumn;
import adql.query.operand.function.CastFunction;
import adql.query.operand.function.DatatypeParam;
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
	public void testTranslateCast() {
		JDBCTranslator tr = new SQLServerTranslator();
		try {
			for(DatatypeParam.DatatypeName datatype : DatatypeParam.DatatypeName.values()) {
				CastFunction castFn = new CastFunction(new ADQLColumn("aColumn"), new DatatypeParam(datatype));
				switch(datatype) {

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
			ADQLQuery query = parser.parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			// Test the FROM part:
			assertEquals("\"aTable\" AS \"a\" INNER JOIN \"anotherTable\" AS \"b\" ON \"a\".\"id\"=\"b\".\"id\" AND \"a\".\"name\"=\"b\".\"name\"", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to NATURAL) are actually translated as columns of the first joined table):
			assertEquals("SELECT \"a\".\"id\" AS \"id\" , \"a\".\"name\" AS \"name\" , \"a\".\"aColumn\" AS \"acolumn\" , \"b\".\"anotherColumn\" AS \"anothercolumn\"", translator.translate(query.getSelect()));

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
			ADQLQuery query = parser.parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			// Test the FROM part:
			assertEquals("\"aTable\" AS \"a\" INNER JOIN \"anotherTable\" AS \"b\" ON \"a\".\"name\"=\"b\".\"name\"", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to USING) are actually translated as columns of the first joined table):
			assertEquals("SELECT \"b\".\"id\" AS \"id\" , \"a\".\"name\" AS \"name\" , \"a\".\"aColumn\" AS \"acolumn\" , \"b\".\"anotherColumn\" AS \"anothercolumn\"", translator.translate(query.getSelect()));

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
			ADQLQuery query = parser.parseQuery("SELECT 'abc' || ' ' || 'def' FROM aTable");
			assertEquals("SELECT 'abc' + ' ' + 'def' AS \"concat\"", translator.translate(query.getSelect()));

			// Test with an easy translation:
			query = parser.parseQuery("SELECT 'a||b||c' || ' ' || 'd+e|f' FROM aTable");
			assertEquals("SELECT 'a||b||c' + ' ' + 'd+e|f' AS \"concat\"", translator.translate(query.getSelect()));

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
