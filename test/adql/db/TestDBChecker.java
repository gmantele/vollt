package adql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.DBType.DBDatatype;
import adql.db.FunctionDef.FunctionParam;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.grammar.ParseException;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.ADQLSet;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.function.UserDefinedFunction;
import adql.search.SimpleSearchHandler;
import adql.translator.ADQLTranslator;
import adql.translator.PostgreSQLTranslator;
import adql.translator.TranslationException;

public class TestDBChecker {

	private static List<DBTable> tables;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tables = new ArrayList<DBTable>();

		DefaultDBTable fooTable = new DefaultDBTable(null, "aschema", "foo");
		DBColumn col = new DefaultDBColumn("\"colS\"", new DBType(DBDatatype.VARCHAR), fooTable);
		fooTable.addColumn(col);
		col = new DefaultDBColumn("colI", new DBType(DBDatatype.INTEGER), fooTable);
		fooTable.addColumn(col);
		col = new DefaultDBColumn("colG", new DBType(DBDatatype.POINT), fooTable);
		fooTable.addColumn(col);

		tables.add(fooTable);

		DefaultDBTable fooTable2 = new DefaultDBTable(null, null, "foo2");
		col = new DefaultDBColumn("oid", new DBType(DBDatatype.BIGINT), fooTable2);
		fooTable2.addColumn(col);

		tables.add(fooTable2);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testWithClause() {

		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);
		parser.setQueryChecker(new DBChecker(tables));

		try {

			// CASE: Only 1 CTE, no column label, no CTE case sensibility
			assertNotNull(parser.parseQuery("WITH myfoo AS (SELECT * FROM foo) SELECT * FROM myfoo"));

			// CASE: CTE case sensibility respected
			assertNotNull(parser.parseQuery("WITH \"MyFoo\" AS (SELECT * FROM foo) SELECT * FROM \"MyFoo\""));
			assertNotNull(parser.parseQuery("WITH \"MyFoo\" AS (SELECT * FROM foo) SELECT * FROM MyFoo"));

			// CASE: reference between WITH clause in the correct order
			assertNotNull(parser.parseQuery("WITH MyFoo AS (SELECT * FROM foo), MyOtherFoo AS (SELECT * FROM MyFoo WHERE colS IS NULL) SELECT * FROM MyOtherFoo"));

		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error while parsing+checking a valid ADQL query! (see console for more details)");
		}

		// CASE: CTE case sensibility not respected
		try {
			parser.parseQuery("WITH \"MyFoo\" AS (SELECT * FROM foo) SELECT * FROM \"myfoo\"");
			fail("WITH item's label is case sensitive....references to this CTE should also be case sensitive.");
		} catch(Exception ex) {
			assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
			assertEquals("1 unresolved identifiers: myfoo [l.1 c.51 - l.1 c.58]!\n  - Unknown table \"\"myfoo\"\" !", ex.getMessage());
		}

	}

	@Test
	public void testSplitTableName() {
		String[] names = DefaultDBTable.splitTableName("foo");
		String[] expected = new String[]{ null, null, "foo" };
		assertEquals(expected.length, names.length);
		for(int i = 0; i < names.length; i++)
			assertEquals(expected[i], names[i]);

		names = DefaultDBTable.splitTableName("aschema.foo");
		expected = new String[]{ null, "aschema", "foo" };
		assertEquals(expected.length, names.length);
		for(int i = 0; i < names.length; i++)
			assertEquals(expected[i], names[i]);

		names = DefaultDBTable.splitTableName("acat.aschema.foo");
		expected = new String[]{ "acat", "aschema", "foo" };
		assertEquals(expected.length, names.length);
		for(int i = 0; i < names.length; i++)
			assertEquals(expected[i], names[i]);

		names = DefaultDBTable.splitTableName("weird.acat.aschema.foo");
		expected = new String[]{ "weird.acat", "aschema", "foo" };
		assertEquals(expected.length, names.length);
		for(int i = 0; i < names.length; i++)
			assertEquals(expected[i], names[i]);
	}

	@Test
	public void testClauseADQLWithNameNull() {
		/* The name of an ADQLClause is got in DBChecker by SearchColumnOutsideGroupByHandler.goInto(...)
		 * and possibly in other locations in the future. If this name is NULL, no NullPointerException
		 * should be thrown.
		 *
		 * This issue can be tested by creating a ConstraintsGroup (i.e. in a constraints location like WHERE or JOIN...ON,
		 * a constraint (or more) between parenthesis). */
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(tables, new ArrayList<FunctionDef>(0)));
			try {
				parser.parseQuery("SELECT * FROM foo WHERE (colI BETWEEN 1 AND 10)");
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed with ADQL-" + version);
			}
		}
	}

	@Test
	public void testGroupByWithQualifiedColName() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(tables, new ArrayList<FunctionDef>(0)));
			try {
				// Not qualified column name:
				parser.parseQuery("SELECT colI, COUNT(*) AS cnt FROM foo GROUP BY colI");
				// Qualified with the table name:
				parser.parseQuery("SELECT foo.colI, COUNT(*) AS cnt FROM foo GROUP BY foo.colI");
				// Qualified with the table alias:
				parser.parseQuery("SELECT f.colI, COUNT(*) AS cnt FROM foo AS f GROUP BY f.colI");
				// With the SELECT item alias:
				parser.parseQuery("SELECT colI AS mycol, COUNT(*) AS cnt FROM foo GROUP BY mycol");
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed with ADQL-" + version);
			}
		}
	}

	@Test
	public void testQualifiedName() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(tables, new ArrayList<FunctionDef>(0)));
			try {
				// Tests with a table whose the schema is specified:
				parser.parseQuery("SELECT * FROM foo;");
				parser.parseQuery("SELECT * FROM aschema.foo;");
				parser.parseQuery("SELECT foo.* FROM foo;");
				parser.parseQuery("SELECT aschema.foo.* FROM foo;");
				parser.parseQuery("SELECT aschema.foo.* FROM aschema.foo;");
				parser.parseQuery("SELECT \"colS\" FROM foo;");
				parser.parseQuery("SELECT foo.\"colS\" FROM foo;");
				parser.parseQuery("SELECT foo.\"colS\" FROM aschema.\"foo\";");
				parser.parseQuery("SELECT \"aschema\".\"foo\".\"colS\" FROM foo;");

				// Tests with a table without schema:
				parser.parseQuery("SELECT * FROM foo2;");
				parser.parseQuery("SELECT foo2.* FROM foo2;");
				parser.parseQuery("SELECT foo2.* FROM \"foo2\";");
				parser.parseQuery("SELECT \"foo2\".* FROM \"foo2\";");
				parser.parseQuery("SELECT oid FROM foo2;");
				parser.parseQuery("SELECT \"oid\" FROM \"foo2\";");
				parser.parseQuery("SELECT foo2.oid FROM foo2;");
				parser.parseQuery("SELECT \"foo2\".\"oid\" FROM \"foo2\";");
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail();
			}

			// If no schema is specified, then the table is not part of a schema and so, there is no reason a table with a fake schema prefix should work:
			try {
				parser.parseQuery("SELECT * FROM noschema.foo2;");
				fail("[ADQL-" + version + "] The table \"foo2\" has no schema specified and so, is not part of a schema. A fake schema prefix should then be forbidden!");
			} catch(ParseException pe) {
			}
			try {
				parser.parseQuery("SELECT noschema.foo2.* FROM foo2;");
				fail("[ADQL-" + version + "] The table \"foo2\" has no schema specified and so, is not part of a schema. A fake schema prefix should then be forbidden!");
			} catch(ParseException pe) {
			}
		}
	}

	@Test
	public void testColRefWithDottedAlias() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(tables));
			try {
				// ORDER BY
				ADQLSet adql = parser.parseQuery("SELECT colI AS \"col.I\" FROM aschema.foo ORDER BY \"col.I\"");
				assertNotNull(adql);
				assertEquals("SELECT \"aschema\".\"foo\".\"colI\" AS \"col.I\"\nFROM \"aschema\".\"foo\"\nORDER BY \"col.I\" ASC", (new PostgreSQLTranslator()).translate(adql));

				// GROUP BY
				adql = parser.parseQuery("SELECT colI AS \"col.I\" FROM aschema.foo GROUP BY \"col.I\"");
				assertNotNull(adql);
				assertEquals("SELECT \"aschema\".\"foo\".\"colI\" AS \"col.I\"\nFROM \"aschema\".\"foo\"\nGROUP BY \"col.I\"", (new PostgreSQLTranslator()).translate(adql));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed with ADQL-" + version);
			} catch(TranslationException te) {
				te.printStackTrace();
				fail("Failed with ADQL-" + version);
			}
		}
	}

	@Test
	public void testNumericOrStringValueExpressionPrimary() {
		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			try {
				assertNotNull(parser.parseQuery("SELECT 'toto' FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT ('toto') FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT (('toto')) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT 'toto' || 'blabla' FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT ('toto' || 'blabla') FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT (('toto' || 'blabla')) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT (('toto') || (('blabla'))) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT 3 FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT ((2+3)*5) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT ABS(-123) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT ABS(2*-1+5) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT ABS(COUNT(*)) FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT toto FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT toto * 3 FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT toto || 'blabla' FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT 'toto' || 1 FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT 1 || 'toto' FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT 'toto' || (-1) FROM foo;"));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed with ADQL-" + version);
			}
			try {
				parser.parseQuery("SELECT ABS('toto') FROM foo;");
				fail("Failed with ADQL-" + version);
			} catch(ParseException pe) {
			}
			try {
				parser.parseQuery("SELECT 'toto' || -1 FROM foo;");
				fail("Failed with ADQL-" + version);
			} catch(ParseException pe) {
			}
			try {
				parser.parseQuery("SELECT -1 || 'toto' FROM foo;");
				fail("Failed with ADQL-" + version);
			} catch(ParseException pe) {
			}
			try {
				parser.parseQuery("SELECT ABS(('toto' || 'blabla')) FROM foo;");
				fail("Failed with ADQL-" + version);
			} catch(ParseException pe) {
			}
			try {
				parser.parseQuery("SELECT 'toto' * 3 FROM foo;");
				fail("Failed with ADQL-" + version);
			} catch(ParseException pe) {
			}
		}
	}

	@Test
	public void testUDFManagement() {
		for(ADQLVersion version : ADQLVersion.values()) {
			// UNKNOWN FUNCTIONS ARE NOT ALLOWED:
			ADQLParser parser = new ADQLParser(version);
			parser.allowAnyUdf(true);
			parser.setQueryChecker(new DBChecker(tables, new ArrayList<FunctionDef>(0)));

			// Test with a simple ADQL query without unknown or user defined function:
			try {
				assertNotNull(parser.parseQuery("SELECT * FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("A simple and basic query should not be a problem for the parser!");
			}

			// Test with an ADQL query containing one not declared UDF:
			try {
				parser.parseQuery("SELECT toto() FROM foo;");
				fail("This query contains a UDF while it's not allowed: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Unresolved function: \"toto()\"! No UDF has been defined or found with the signature: toto().", ex.getErrors().next().getMessage());
			}

			// DECLARE THE UDFs:
			FunctionDef[] udfs;
			try {
				udfs = new FunctionDef[]{ new FunctionDef("toto", new DBType(DBDatatype.VARCHAR)), new FunctionDef("tata", new DBType(DBDatatype.INTEGER)) };
				parser = new ADQLParser(version);
				parser.allowAnyUdf(true);
				parser.setQueryChecker(new DBChecker(tables, Arrays.asList(udfs)));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
			}

			// Test again:
			try {
				assertNotNull(parser.parseQuery("SELECT toto() FROM foo;"));
				assertNotNull(parser.parseQuery("SELECT tata() FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("This query contains a DECLARED UDF: this test should have succeeded!");
			}

			// Test but with at least one parameter:
			try {
				parser.parseQuery("SELECT toto('blabla') FROM foo;");
				fail("This query contains an unknown UDF signature (the fct toto is declared with no parameter): this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Unresolved function: \"toto('blabla')\"! No UDF has been defined or found with the signature: toto(STRING).", ex.getErrors().next().getMessage());
			}

			// Test but with at least one column parameter:
			try {
				parser.parseQuery("SELECT toto(colS) FROM foo;");
				fail("This query contains an unknown UDF signature (the fct toto is declared with no parameter): this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Unresolved function: \"toto(colS)\"! No UDF has been defined or found with the signature: toto(STRING).", ex.getErrors().next().getMessage());
			}

			// Test but with at least one unknown column parameter:
			try {
				parser.parseQuery("SELECT toto(whatami) FROM foo;");
				fail("This query contains an unknown UDF signature (the fct toto is declared with no parameter): this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(2, ex.getNbErrors());
				Iterator<ParseException> errors = ex.getErrors();
				assertEquals("Unknown column \"whatami\" !", errors.next().getMessage());
				assertEquals("Unresolved function: \"toto(whatami)\"! No UDF has been defined or found with the signature: toto(param1).", errors.next().getMessage());
			}

			// Test with a UDF whose the class is specified ; the corresponding object in the ADQL tree must be replace by an instance of this class:
			try {
				udfs = new FunctionDef[]{ new FunctionDef("toto", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{ new FunctionParam("txt", new DBType(DBDatatype.VARCHAR)) }) };
				udfs[0].setUDFClass(UDFToto.class);
				parser = new ADQLParser(version);
				parser.allowAnyUdf(true);
				parser.setQueryChecker(new DBChecker(tables, Arrays.asList(udfs)));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
			}

			try {
				ADQLSet query = parser.parseQuery("SELECT toto('blabla') FROM foo;");
				assertNotNull(query);
				Iterator<ADQLObject> it = query.search(new SimpleSearchHandler() {
					@Override
					protected boolean match(ADQLObject obj) {
						return (obj instanceof UserDefinedFunction) && ((UserDefinedFunction)obj).getName().equals("toto");
					}
				});
				assertTrue(it.hasNext());
				assertEquals(UDFToto.class.getName(), it.next().getClass().getName());
				assertFalse(it.hasNext());
			} catch(Exception e) {
				e.printStackTrace();
				fail("This query contains a DECLARED UDF with a valid UserDefinedFunction class: this test should have succeeded!");
			}

			// Test with a wrong parameter type:
			try {
				parser.parseQuery("SELECT toto(123) FROM foo;");
				fail("This query contains an unknown UDF signature (the fct toto is declared with one parameter of type STRING...here it is a numeric): this test should have failed!");
			} catch(Exception e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Unresolved function: \"toto(123)\"! No UDF has been defined or found with the signature: toto(NUMERIC).", ex.getErrors().next().getMessage());
			}

			// Test with UDF class constructor throwing an exception:
			try {
				udfs = new FunctionDef[]{ new FunctionDef("toto", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{ new FunctionParam("txt", new DBType(DBDatatype.VARCHAR)) }) };
				udfs[0].setUDFClass(WrongUDFToto.class);
				parser = new ADQLParser(version);
				parser.allowAnyUdf(true);
				parser.setQueryChecker(new DBChecker(tables, Arrays.asList(udfs)));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
			}

			try {
				parser.parseQuery("SELECT toto('blabla') FROM foo;");
				fail("The set UDF class constructor has throw an error: this test should have failed!");
			} catch(Exception e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Impossible to represent the function \"toto\": the following error occured while creating this representation: \"[Exception] Systematic error!\"", ex.getErrors().next().getMessage());
			}
		}
	}

	@Test
	public void testTypesChecking() {
		for(ADQLVersion version : ADQLVersion.values()) {
			// DECLARE A SIMPLE PARSER:
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(tables));

			// Test the type of columns generated by the parser:
			try {
				ADQLSet query = parser.parseQuery("SELECT colS, colI, colG FROM foo;");
				ADQLOperand colS = ((ADQLQuery)query).getSelect().get(0).getOperand();
				ADQLOperand colI = ((ADQLQuery)query).getSelect().get(1).getOperand();
				ADQLOperand colG = ((ADQLQuery)query).getSelect().get(2).getOperand();
				// test string column:
				assertTrue(colS instanceof ADQLColumn);
				assertTrue(colS.isString());
				assertFalse(colS.isNumeric());
				assertFalse(colS.isGeometry());
				// test integer column:
				assertTrue(colI instanceof ADQLColumn);
				assertFalse(colI.isString());
				assertTrue(colI.isNumeric());
				assertFalse(colI.isGeometry());
				// test geometry column:
				assertTrue(colG instanceof ADQLColumn);
				assertFalse(colG.isString());
				assertFalse(colG.isNumeric());
				assertTrue(colG.isGeometry());
			} catch(ParseException e1) {
				if (e1 instanceof UnresolvedIdentifiersException)
					((UnresolvedIdentifiersException)e1).getErrors().next().printStackTrace();
				else
					e1.printStackTrace();
				fail("This query contains known columns: this test should have succeeded!");
			}

			// Test the expected type - NUMERIC - generated by the parser:
			try {
				assertNotNull(parser.parseQuery("SELECT colI * 3 FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("This query contains a product between 2 numerics: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT colS * 3 FROM foo;");
				fail("This query contains a product between a string and an integer: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A numeric value was expected instead of \"colS\".", ex.getErrors().next().getMessage());
			}
			try {
				parser.parseQuery("SELECT colG * 3 FROM foo;");
				fail("This query contains a product between a geometry and an integer: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A numeric value was expected instead of \"colG\".", ex.getErrors().next().getMessage());
			}

			// Test the expected type - STRING - generated by the parser:
			try {
				assertNotNull(parser.parseQuery("SELECT colS || 'blabla' FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("This query contains a concatenation between 2 strings: this test should have succeeded!");
			}
			try {
				assertNotNull(parser.parseQuery("SELECT colI || 'blabla' FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("This query contains a concatenation between a column (whatever its type) and a string: this test should have succeeded!");
			}
			try {
				assertNotNull(parser.parseQuery("SELECT colG || 'blabla' FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("This query contains a concatenation between a column (whatever its type) and a string: this test should have succeeded!");
			}

			// Test the expected type - GEOMETRY - generated by the parser:
			try {
				assertNotNull(parser.parseQuery("SELECT CONTAINS(colG, CIRCLE('', 1, 2, 5)) FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("This query contains a geometrical predicate between 2 geometries: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT CONTAINS(colI, CIRCLE('', 1, 2, 5)) FROM foo;");
				fail("This query contains a geometrical predicate between an integer and a geometry: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A geometry was expected instead of \"colI\".", ex.getErrors().next().getMessage());
			}
			try {
				parser.parseQuery("SELECT CONTAINS(colS, CIRCLE('', 1, 2, 5)) FROM foo;");
				fail("This query contains a geometrical predicate between a string and a geometry: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A geometry was expected instead of \"colS\".", ex.getErrors().next().getMessage());
			}

			// DECLARE SOME UDFs:
			FunctionDef[] udfs;
			try {
				udfs = new FunctionDef[]{ new FunctionDef("toto", new DBType(DBDatatype.VARCHAR)), new FunctionDef("tata", new DBType(DBDatatype.INTEGER)), new FunctionDef("titi", new DBType(DBDatatype.REGION)) };
				parser = new ADQLParser(version);
				for(FunctionDef udf : udfs)
					parser.getSupportedFeatures().support(udf.toLanguageFeature());
				parser.setQueryChecker(new DBChecker(tables, Arrays.asList(udfs)));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
			}

			// Test the return type of the function TOTO generated by the parser:
			try {
				ADQLSet query = parser.parseQuery("SELECT toto() FROM foo;");
				assertEquals(ADQLQuery.class, query.getClass());
				ADQLOperand fct = ((ADQLQuery)query).getSelect().get(0).getOperand();
				assertTrue(fct instanceof UserDefinedFunction);
				assertNotNull(((UserDefinedFunction)fct).getDefinition());
				assertTrue(fct.isString());
				assertFalse(fct.isNumeric());
				assertFalse(fct.isGeometry());
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("This query contains a DECLARED UDF: this test should have succeeded!");
			}

			// Test the return type checking inside a whole query:
			try {
				assertNotNull(parser.parseQuery("SELECT toto() || 'Blabla ' AS \"SuperText\" FROM foo;"));
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("This query contains a DECLARED UDF concatenated to a String: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT toto()*3 AS \"SuperError\" FROM foo;");
				fail("This query contains a DECLARED UDF BUT used as numeric...which is here not possible: this test should have failed!");
			} catch(ParseException e1) {
				assertTrue(e1 instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A numeric value was expected instead of \"toto()\".", ex.getErrors().next().getMessage());
			}

			// Test the return type of the function TATA generated by the parser:
			try {
				ADQLSet query = parser.parseQuery("SELECT tata() FROM foo;");
				assertEquals(ADQLQuery.class, query.getClass());
				ADQLOperand fct = ((ADQLQuery)query).getSelect().get(0).getOperand();
				assertTrue(fct instanceof UserDefinedFunction);
				assertNotNull(((UserDefinedFunction)fct).getDefinition());
				assertFalse(fct.isString());
				assertTrue(fct.isNumeric());
				assertFalse(fct.isGeometry());
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("This query contains a DECLARED UDF: this test should have succeeded!");
			}

			// Test the return type checking inside a whole query:
			try {
				assertNotNull(parser.parseQuery("SELECT tata()*3 AS \"aNumeric\" FROM foo;"));
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("This query contains a DECLARED UDF multiplicated by 3: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT 'Blabla ' || tata() AS \"SuperError\" FROM foo;");
				fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
			} catch(ParseException e1) {
				assertTrue(e1 instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A string value was expected instead of \"tata()\".", ex.getErrors().next().getMessage());
			}
			try {
				parser.parseQuery("SELECT tata() || 'Blabla ' AS \"SuperError\" FROM foo;");
				fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
			} catch(ParseException e1) {
				assertTrue(e1 instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A string value was expected instead of \"tata()\".", ex.getErrors().next().getMessage());
			}

			// Test the return type of the function TITI generated by the parser:
			try {
				ADQLSet query = parser.parseQuery("SELECT titi() FROM foo;");
				assertEquals(ADQLQuery.class, query.getClass());
				ADQLOperand fct = ((ADQLQuery)query).getSelect().get(0).getOperand();
				assertTrue(fct instanceof UserDefinedFunction);
				assertNotNull(((UserDefinedFunction)fct).getDefinition());
				assertFalse(fct.isString());
				assertFalse(fct.isNumeric());
				assertTrue(fct.isGeometry());
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("This query contains a DECLARED UDF: this test should have succeeded!");
			}

			// Test the return type checking inside a whole query:
			if (parser.getADQLVersion() == ADQLVersion.V2_0) {
				try {
					parser.parseQuery("SELECT CONTAINS(colG, titi()) AS \"SuperError\" FROM foo;");
					fail("Geometrical UDFs are not allowed for the moment in the ADQL language: this test should have failed!");
				} catch(ParseException e1) {
					assertTrue(e1 instanceof ParseException);
					assertEquals(" Encountered \"(\". Was expecting one of: \")\" \".\" \".\" \")\" ", e1.getMessage());
				}
			}
			try {
				parser.parseQuery("SELECT titi()*3 AS \"SuperError\" FROM foo;");
				fail("This query contains a DECLARED UDF BUT used as numeric...which is here not possible: this test should have failed!");
			} catch(ParseException e1) {
				assertTrue(e1 instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A numeric value was expected instead of \"titi()\".", ex.getErrors().next().getMessage());
			}

			// Try with functions wrapped on 2 levels:
			// i.e. fct1('blabla', fct2(fct3('blabla')))
			try {
				FunctionDef[] complexFcts = new FunctionDef[3];
				complexFcts[0] = new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{ new FunctionParam("str", new DBType(DBDatatype.VARCHAR)), new FunctionParam("num", new DBType(DBDatatype.INTEGER)) });
				complexFcts[1] = new FunctionDef("fct2", new DBType(DBDatatype.INTEGER), new FunctionParam[]{ new FunctionParam("str", new DBType(DBDatatype.VARCHAR)) });
				complexFcts[2] = new FunctionDef("fct3", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{ new FunctionParam("str", new DBType(DBDatatype.VARCHAR)) });
				parser = new ADQLParser(version);
				for(FunctionDef udf : complexFcts)
					parser.getSupportedFeatures().support(udf.toLanguageFeature());
				parser.setQueryChecker(new DBChecker(tables, Arrays.asList(complexFcts)));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
			}

			// With parameters of the good type:
			try {
				assertNotNull(parser.parseQuery("SELECT fct1('blabla', fct2(fct3('blabla'))) FROM foo"));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Types are matching: this test should have succeeded!");
			}
			// With parameters of the bad type:
			try {
				parser.parseQuery("SELECT fct2(fct1('blabla', fct3('blabla'))) FROM foo");
				fail("Parameters types are not matching: the parsing should have failed!");
			} catch(ParseException pe) {
				assertEquals(UnresolvedIdentifiersException.class, pe.getClass());
				assertEquals(1, ((UnresolvedIdentifiersException)pe).getNbErrors());
				ParseException innerPe = ((UnresolvedIdentifiersException)pe).getErrors().next();
				assertEquals("Unresolved function: \"fct1('blabla', fct3('blabla'))\"! No UDF has been defined or found with the signature: fct1(STRING, STRING).", innerPe.getMessage());
			}

			// CLEAR ALL UDFs AND ALLOW UNKNOWN FUNCTION:
			parser = new ADQLParser(version);
			parser.allowAnyUdf(true);
			parser.setQueryChecker(new DBChecker(tables, null));

			// Test again:
			try {
				assertNotNull(parser.parseQuery("SELECT toto() FROM foo;"));
			} catch(ParseException e) {
				e.printStackTrace();
				fail("The parser allow ANY unknown function: this test should have succeeded!");
			}

			// Test the return type of the function generated by the parser:
			try {
				ADQLSet query = parser.parseQuery("SELECT toto() FROM foo;");
				assertEquals(ADQLQuery.class, query.getClass());
				ADQLOperand fct = ((ADQLQuery)query).getSelect().get(0).getOperand();
				assertTrue(fct instanceof UserDefinedFunction);
				assertNull(((UserDefinedFunction)fct).getDefinition());
				assertTrue(fct.isString());
				assertTrue(fct.isNumeric());
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("The parser allow ANY unknown function: this test should have succeeded!");
			}

			// DECLARE THE UDF (while unknown functions are allowed):
			try {
				parser = new ADQLParser(version);
				FunctionDef udf = new FunctionDef("toto", new DBType(DBDatatype.VARCHAR));
				parser.getSupportedFeatures().support(udf.toLanguageFeature());
				parser.setQueryChecker(new DBChecker(tables, Arrays.asList(udf)));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
			}

			// Test the return type of the function generated by the parser:
			try {
				ADQLSet query = parser.parseQuery("SELECT toto() FROM foo;");
				assertEquals(ADQLQuery.class, query.getClass());
				ADQLOperand fct = ((ADQLQuery)query).getSelect().get(0).getOperand();
				assertTrue(fct instanceof UserDefinedFunction);
				assertNotNull(((UserDefinedFunction)fct).getDefinition());
				assertTrue(fct.isString());
				assertFalse(fct.isNumeric());
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("The parser allow ANY unknown function: this test should have succeeded!");
			}

			// DECLARE UDFs WITH SAME NAMES BUT DIFFERENT TYPE OF ARGUMENT:
			try {
				udfs = new FunctionDef[]{ new FunctionDef("toto", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{ new FunctionParam("attr", new DBType(DBDatatype.VARCHAR)) }), new FunctionDef("toto", new DBType(DBDatatype.INTEGER), new FunctionParam[]{ new FunctionParam("attr", new DBType(DBDatatype.INTEGER)) }), new FunctionDef("toto", new DBType(DBDatatype.INTEGER), new FunctionParam[]{ new FunctionParam("attr", new DBType(DBDatatype.POINT)) }) };
				parser = new ADQLParser(version);
				for(FunctionDef udf : udfs)
					assertTrue(parser.getSupportedFeatures().support(udf.toLanguageFeature()));
				parser.setQueryChecker(new DBChecker(tables, Arrays.asList(udfs)));
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
			}

			// Test the return type in function of the parameter:
			try {
				assertNotNull(parser.parseQuery("SELECT toto('blabla') AS toto1, toto(123) AS toto2, toto(POINT('', 1, 2)) AS toto3  FROM foo;"));
			} catch(ParseException e1) {
				e1.printStackTrace();
				fail("This query contains two DECLARED UDFs used here: this test should have succeeded!");
			}
			try {
				parser.parseQuery("SELECT toto('blabla') * 123 AS \"SuperError\" FROM foo;");
				fail("This query contains a DECLARED UDF BUT used as numeric...which is here not possible: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A numeric value was expected instead of \"toto('blabla')\".", ex.getErrors().next().getMessage());
			}
			try {
				parser.parseQuery("SELECT toto(123) || 'blabla' AS \"SuperError\" FROM foo;");
				fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A string value was expected instead of \"toto(123)\".", ex.getErrors().next().getMessage());
			}
			try {
				parser.parseQuery("SELECT toto(POINT('', 1, 2)) || 'blabla' AS \"SuperError\" FROM foo;");
				fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
			} catch(ParseException e) {
				assertTrue(e instanceof UnresolvedIdentifiersException);
				UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
				assertEquals(1, ex.getNbErrors());
				assertEquals("Type mismatch! A string value was expected instead of \"toto(POINT('', 1, 2))\".", ex.getErrors().next().getMessage());
			}
		}
	}

	private static class WrongUDFToto extends UDFToto {
		public WrongUDFToto(final ADQLOperand[] params) throws Exception {
			super(params);
			throw new Exception("Systematic error!");
		}
	}

	public static class UDFToto extends UserDefinedFunction {

		protected StringConstant fakeParam;

		public UDFToto(final ADQLOperand[] params) throws Exception {
			if (params == null || params.length == 0)
				throw new Exception("Missing parameter for the user defined function \"toto\"!");
			else if (params.length > 1)
				throw new Exception("Too many parameters for the function \"toto\"! Only one is required.");
			else if (!(params[0] instanceof StringConstant))
				throw new Exception("Wrong parameter type! The parameter of the UDF \"toto\" must be a string constant.");
			fakeParam = (StringConstant)params[0];
			functionName = "toto";
			languageFeature = (new FunctionDef(getName(), new DBType(DBDatatype.VARCHAR), new FunctionParam[]{ new FunctionParam("txt", new DBType(DBDatatype.VARCHAR)) })).toLanguageFeature();
		}

		@Override
		public final boolean isNumeric() {
			return false;
		}

		@Override
		public final boolean isString() {
			return true;
		}

		@Override
		public final boolean isGeometry() {
			return false;
		}

		@Override
		public ADQLObject getCopy() throws Exception {
			ADQLOperand[] params = new ADQLOperand[]{ (StringConstant)fakeParam.getCopy() };
			return new UDFToto(params);
		}

		@Override
		public final ADQLOperand[] getParameters() {
			return new ADQLOperand[]{ fakeParam };
		}

		@Override
		public final int getNbParameters() {
			return 1;
		}

		@Override
		public final ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
			if (index != 0)
				throw new ArrayIndexOutOfBoundsException("Incorrect parameter index: " + index + "! The function \"toto\" has only one parameter.");
			return fakeParam;
		}

		@Override
		public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
			if (index != 0)
				throw new ArrayIndexOutOfBoundsException("Incorrect parameter index: " + index + "! The function \"toto\" has only one parameter.");
			else if (!(replacer instanceof StringConstant))
				throw new Exception("Wrong parameter type! The parameter of the UDF \"toto\" must be a string constant.");
			return (fakeParam = (StringConstant)replacer);
		}

		@Override
		public String translate(final ADQLTranslator caller) throws TranslationException {
			/* Note: Since this function is totally fake, this function will be replaced in SQL by its parameter (the string). */
			return caller.translate(fakeParam);
		}
	}

}
