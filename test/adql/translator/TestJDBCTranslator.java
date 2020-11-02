package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBChecker;
import adql.db.DBTable;
import adql.db.DBType;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.FunctionDef;
import adql.db.STCS.Region;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.ADQLQuery;
import adql.query.ClauseADQL;
import adql.query.IdentifierField;
import adql.query.WithItem;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.InUnitFunction;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.ExtractCoordSys;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

public class TestJDBCTranslator {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testTranslateWithClause() {
		JDBCTranslator tr = new AJDBCTranslator();
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		try {
			// CASE: No WITH clause
			ADQLQuery query = parser.parseQuery("SELECT * FROM foo");
			ClauseADQL<WithItem> withClause = query.getWith();
			assertTrue(withClause.isEmpty());
			assertEquals("WITH ", tr.translate(withClause));
			assertEquals("SELECT *\nFROM foo", tr.translate(query));

			// CASE: A single WITH item
			query = parser.parseQuery("WITH foo AS (SELECT * FROM bar) SELECT * FROM foo");
			withClause = query.getWith();
			assertEquals(1, withClause.size());
			assertEquals("WITH \"foo\" AS (\nSELECT *\nFROM bar\n)", tr.translate(withClause));
			assertEquals("WITH \"foo\" AS (\nSELECT *\nFROM bar\n)\nSELECT *\nFROM foo", tr.translate(query));

			// CASE: Several WITH items
			query = parser.parseQuery("WITH foo AS (SELECT * FROM bar), Foo2 AS (SELECT myCol FROM myTable) SELECT * FROM foo JOIN foo2 ON foo.id = foo2.myCol");
			withClause = query.getWith();
			assertEquals(2, withClause.size());
			assertEquals("WITH \"foo\" AS (\nSELECT *\nFROM bar\n) , \"foo2\" AS (\nSELECT myCol AS \"myCol\"\nFROM myTable\n)", tr.translate(withClause));
			assertEquals("WITH \"foo\" AS (\nSELECT *\nFROM bar\n) , \"foo2\" AS (\nSELECT myCol AS \"myCol\"\nFROM myTable\n)\nSELECT *\nFROM foo INNER JOIN foo2 ON foo.id = foo2.myCol", tr.translate(query));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected parsing failure! (see console for more details)");
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while translating a correct WITH item! (see console for more details)");
		}
	}

	@Test
	public void testTranslateWithItem() {
		JDBCTranslator tr = new AJDBCTranslator();

		try {
			// CASE: Simple WITH item (no case sensitivity)
			WithItem item = new WithItem("Foo", (new ADQLParser(ADQLVersion.V2_1)).parseQuery("SELECT * FROM bar"));
			item.setLabelCaseSensitive(false);
			assertEquals("\"foo\" AS (\nSELECT *\nFROM bar\n)", tr.translate(item));

			// CASE: WITH item with case sensitivity
			item = new WithItem("Foo", (new ADQLParser(ADQLVersion.V2_1)).parseQuery("SELECT col1, col2 FROM bar"));
			item.setLabelCaseSensitive(true);
			assertEquals("\"Foo\" AS (\nSELECT col1 AS \"col1\" , col2 AS \"col2\"\nFROM bar\n)", tr.translate(item));

			// CASE: query with an inner WITH
			item = new WithItem("Foo", (new ADQLParser(ADQLVersion.V2_1)).parseQuery("WITH bar AS (SELECT aCol, anotherCol FROM stuff) SELECT * FROM bar"));
			assertEquals("\"foo\" AS (\nWITH \"bar\" AS (\nSELECT aCol AS \"aCol\" , anotherCol AS \"anotherCol\"\nFROM stuff\n)\nSELECT *\nFROM bar\n)", tr.translate(item));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected parsing failure! (see console for more details)");
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while translating a correct WITH item! (see console for more details)");
		}
	}

	public final static int countFeatures(final FeatureSet features) {
		int cnt = 0;
		for(LanguageFeature feat : features)
			cnt++;
		return cnt;
	}

	@Test
	public void testTranslateOffset() {
		JDBCTranslator tr = new AJDBCTranslator();
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		try {

			// CASE: Only OFFSET
			assertEquals("SELECT *\nFROM foo\nOFFSET 10", tr.translate(parser.parseQuery("Select * From foo OffSet 10")));

			// CASE: Only OFFSET = 0
			assertEquals("SELECT *\nFROM foo\nOFFSET 0", tr.translate(parser.parseQuery("Select * From foo OffSet 0")));

			// CASE: TOP + OFFSET
			assertEquals("SELECT *\nFROM foo\nLIMIT 5\nOFFSET 10", tr.translate(parser.parseQuery("Select Top 5 * From foo OffSet 10")));

			// CASE: TOP + ORDER BY + OFFSET
			assertEquals("SELECT *\nFROM foo\nORDER BY id ASC\nLIMIT 5\nOFFSET 10", tr.translate(parser.parseQuery("Select Top 5 * From foo Order By id Asc OffSet 10")));

		} catch(ParseException pe) {
			pe.printStackTrace(System.err);
			fail("Unexpected failed query parsing! (see console for more details)");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate a query with offset into SQL.");
		}
	}

	@Test
	public void testTranslateStringConstant() {
		JDBCTranslator tr = new AJDBCTranslator();

		/* Ensure the translation from ADQL to SQL of strings is correct ;
		 * particularly, ' should be escaped otherwise it would mean the end of
		 * a string in SQL (the way to escape a such character is by doubling
		 * the character '): */
		try {
			assertEquals("'SQL''s translation'", tr.translate(new StringConstant("SQL's translation")));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate a StringConstant object into SQL.");
		}
	}

	@Test
	public void testTranslateUserDefinedFunction() {
		JDBCTranslator tr = new AJDBCTranslator();

		DefaultUDF udf = new DefaultUDF("split", new ADQLOperand[]{ new ADQLColumn("values"), new StringConstant(";") });

		// TEST: no FunctionDef, so no translation pattern => just return the ADQL
		try {
			assertEquals("split(values, ';')", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		}

		// TEST: a FunctionDef with no translation pattern => just return the ADQL
		try {
			udf.setDefinition(FunctionDef.parse("split(str VARCHAR, sep VARCHAR) -> VARCHAR"));
			assertEquals("split(values, ';')", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem preparing this test.");
		}

		// TEST: a FunctionDef with a translation pattern but no argument reference => the pattern should be returned as such
		try {
			udf.getDefinition().setTranslationPattern("foobar");
			assertEquals("foobar", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem preparing this test.");
		}

		// TEST: a FunctionDef with a translation pattern and with argument reference => the pattern should be applied
		try {
			udf.getDefinition().setTranslationPattern("splitWith($2, $1)");
			assertEquals("splitWith(" + tr.translate(udf.getParameter(1)) + ", " + tr.translate(udf.getParameter(0)) + ")", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem preparing this test.");
		}
	}

	@Test
	public void testNaturalJoin() {
		ArrayList<DBTable> tables = new ArrayList<DBTable>(2);
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

		final String adqlquery = "SELECT id, name, aColumn, anotherColumn FROM aTable A NATURAL JOIN anotherTable B;";

		try {
			ADQLParser parser = new ADQLParser();
			parser.setQueryChecker(new DBChecker(tables));
			ADQLQuery query = parser.parseQuery(adqlquery);
			JDBCTranslator translator = new AJDBCTranslator();

			// Test the FROM part:
			assertEquals("aTable AS \"a\" NATURAL INNER JOIN anotherTable AS \"b\" ", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to NATURAL) are actually translated as columns of the first joined table):
			assertEquals("SELECT id AS \"id\" , name AS \"name\" , a.aColumn AS \"acolumn\" , b.anotherColumn AS \"anothercolumn\"", translator.translate(query.getSelect()));

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
		ArrayList<DBTable> tables = new ArrayList<DBTable>(2);
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

		final String adqlquery = "SELECT B.id, name, aColumn, anotherColumn FROM aTable A JOIN anotherTable B USING(name);";

		try {
			ADQLParser parser = new ADQLParser();
			parser.setQueryChecker(new DBChecker(tables));
			ADQLQuery query = parser.parseQuery(adqlquery);
			JDBCTranslator translator = new AJDBCTranslator();

			// Test the FROM part:
			assertEquals("aTable AS \"a\" INNER JOIN anotherTable AS \"b\" USING (name)", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to USING) are actually translated as columns of the first joined table):
			assertEquals("SELECT b.id AS \"id\" , name AS \"name\" , a.aColumn AS \"acolumn\" , b.anotherColumn AS \"anothercolumn\"", translator.translate(query.getSelect()));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		} catch(TranslationException te) {
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

	public final static class AJDBCTranslator extends JDBCTranslator {

		@Override
		public FeatureSet getSupportedFeatures() {
			return new FeatureSet(true, true);
		}

		@Override
		public String translate(InUnitFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(ExtractCoord extractCoord) throws TranslationException {
			return null;
		}

		@Override
		public String translate(ExtractCoordSys extractCoordSys) throws TranslationException {
			return null;
		}

		@Override
		public String translate(AreaFunction areaFunction) throws TranslationException {
			return null;
		}

		@Override
		public String translate(CentroidFunction centroidFunction) throws TranslationException {
			return null;
		}

		@Override
		public String translate(DistanceFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(ContainsFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(IntersectsFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(PointFunction point) throws TranslationException {
			return null;
		}

		@Override
		public String translate(CircleFunction circle) throws TranslationException {
			return null;
		}

		@Override
		public String translate(BoxFunction box) throws TranslationException {
			return null;
		}

		@Override
		public String translate(PolygonFunction polygon) throws TranslationException {
			return null;
		}

		@Override
		public String translate(RegionFunction region) throws TranslationException {
			return null;
		}

		@Override
		public boolean isCaseSensitive(IdentifierField field) {
			return false;
		}

		@Override
		public DBType convertTypeFromDB(int dbmsType, String rawDbmsTypeName, String dbmsTypeName, String[] typeParams) {
			return null;
		}

		@Override
		public String convertTypeToDB(DBType type) {
			return null;
		}

		@Override
		public Region translateGeometryFromDB(Object jdbcColValue) throws ParseException {
			return null;
		}

		@Override
		public Object translateGeometryToDB(Region region) throws ParseException {
			return null;
		}

	}

}
