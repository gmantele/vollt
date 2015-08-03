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
import adql.parser.ParseException;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.UserDefinedFunction;
import adql.search.SimpleSearchHandler;
import adql.translator.ADQLTranslator;
import adql.translator.TranslationException;

public class TestDBChecker {

	private static List<DBTable> tables;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		tables = new ArrayList<DBTable>();

		DefaultDBTable fooTable = new DefaultDBTable("foo");
		DBColumn col = new DefaultDBColumn("colS", new DBType(DBDatatype.VARCHAR), fooTable);
		fooTable.addColumn(col);
		col = new DefaultDBColumn("colI", new DBType(DBDatatype.INTEGER), fooTable);
		fooTable.addColumn(col);
		col = new DefaultDBColumn("colG", new DBType(DBDatatype.POINT), fooTable);
		fooTable.addColumn(col);

		tables.add(fooTable);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{}

	@Before
	public void setUp() throws Exception{}

	@After
	public void tearDown() throws Exception{}

	@Test
	public void testNumericOrStringValueExpressionPrimary(){
		ADQLParser parser = new ADQLParser();
		try{
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
		}catch(ParseException pe){
			pe.printStackTrace();
			fail();
		}
		try{
			parser.parseQuery("SELECT ABS('toto') FROM foo;");
			fail();
		}catch(ParseException pe){}
		try{
			parser.parseQuery("SELECT ABS(('toto' || 'blabla')) FROM foo;");
			fail();
		}catch(ParseException pe){}
		try{
			parser.parseQuery("SELECT 'toto' || 1 FROM foo;");
			fail();
		}catch(ParseException pe){}
		try{
			parser.parseQuery("SELECT 1 || 'toto' FROM foo;");
			fail();
		}catch(ParseException pe){}
		try{
			parser.parseQuery("SELECT 'toto' * 3 FROM foo;");
			fail();
		}catch(ParseException pe){}
	}

	@Test
	public void testUDFManagement(){
		// UNKNOWN FUNCTIONS ARE NOT ALLOWED:
		ADQLParser parser = new ADQLParser(new DBChecker(tables, new ArrayList<FunctionDef>(0)));

		// Test with a simple ADQL query without unknown or user defined function:
		try{
			assertNotNull(parser.parseQuery("SELECT * FROM foo;"));
		}catch(ParseException e){
			e.printStackTrace();
			fail("A simple and basic query should not be a problem for the parser!");
		}

		// Test with an ADQL query containing one not declared UDF:
		try{
			parser.parseQuery("SELECT toto() FROM foo;");
			fail("This query contains a UDF while it's not allowed: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Unresolved function: \"toto()\"! No UDF has been defined or found with the signature: toto().", ex.getErrors().next().getMessage());
		}

		// DECLARE THE UDFs:
		FunctionDef[] udfs = new FunctionDef[]{new FunctionDef("toto", new DBType(DBDatatype.VARCHAR)),new FunctionDef("tata", new DBType(DBDatatype.INTEGER))};
		parser = new ADQLParser(new DBChecker(tables, Arrays.asList(udfs)));

		// Test again:
		try{
			assertNotNull(parser.parseQuery("SELECT toto() FROM foo;"));
			assertNotNull(parser.parseQuery("SELECT tata() FROM foo;"));
		}catch(ParseException e){
			e.printStackTrace();
			fail("This query contains a DECLARED UDF: this test should have succeeded!");
		}

		// Test but with at least one parameter:
		try{
			parser.parseQuery("SELECT toto('blabla') FROM foo;");
			fail("This query contains an unknown UDF signature (the fct toto is declared with no parameter): this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Unresolved function: \"toto('blabla')\"! No UDF has been defined or found with the signature: toto(STRING).", ex.getErrors().next().getMessage());
		}

		// Test but with at least one column parameter:
		try{
			parser.parseQuery("SELECT toto(colS) FROM foo;");
			fail("This query contains an unknown UDF signature (the fct toto is declared with no parameter): this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Unresolved function: \"toto(colS)\"! No UDF has been defined or found with the signature: toto(STRING).", ex.getErrors().next().getMessage());
		}

		// Test but with at least one unknown column parameter:
		try{
			parser.parseQuery("SELECT toto(whatami) FROM foo;");
			fail("This query contains an unknown UDF signature (the fct toto is declared with no parameter): this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(2, ex.getNbErrors());
			Iterator<ParseException> errors = ex.getErrors();
			assertEquals("Unknown column \"whatami\" !", errors.next().getMessage());
			assertEquals("Unresolved function: \"toto(whatami)\"! No UDF has been defined or found with the signature: toto(param1).", errors.next().getMessage());
		}

		// Test with a UDF whose the class is specified ; the corresponding object in the ADQL tree must be replace by an instance of this class:
		udfs = new FunctionDef[]{new FunctionDef("toto", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("txt", new DBType(DBDatatype.VARCHAR))})};
		udfs[0].setUDFClass(UDFToto.class);
		parser = new ADQLParser(new DBChecker(tables, Arrays.asList(udfs)));
		try{
			ADQLQuery query = parser.parseQuery("SELECT toto('blabla') FROM foo;");
			assertNotNull(query);
			Iterator<ADQLObject> it = query.search(new SimpleSearchHandler(){
				@Override
				protected boolean match(ADQLObject obj){
					return (obj instanceof UserDefinedFunction) && ((UserDefinedFunction)obj).getName().equals("toto");
				}
			});
			assertTrue(it.hasNext());
			assertEquals(UDFToto.class.getName(), it.next().getClass().getName());
			assertFalse(it.hasNext());
		}catch(Exception e){
			e.printStackTrace();
			fail("This query contains a DECLARED UDF with a valid UserDefinedFunction class: this test should have succeeded!");
		}

		// Test with a wrong parameter type:
		try{
			parser.parseQuery("SELECT toto(123) FROM foo;");
			fail("This query contains an unknown UDF signature (the fct toto is declared with one parameter of type STRING...here it is a numeric): this test should have failed!");
		}catch(Exception e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Unresolved function: \"toto(123)\"! No UDF has been defined or found with the signature: toto(NUMERIC).", ex.getErrors().next().getMessage());
		}

		// Test with UDF class constructor throwing an exception:
		udfs = new FunctionDef[]{new FunctionDef("toto", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("txt", new DBType(DBDatatype.VARCHAR))})};
		udfs[0].setUDFClass(WrongUDFToto.class);
		parser = new ADQLParser(new DBChecker(tables, Arrays.asList(udfs)));
		try{
			parser.parseQuery("SELECT toto('blabla') FROM foo;");
			fail("The set UDF class constructor has throw an error: this test should have failed!");
		}catch(Exception e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Impossible to represent the function \"toto\": the following error occured while creating this representation: \"[Exception] Systematic error!\"", ex.getErrors().next().getMessage());
		}
	}

	@Test
	public void testGeometry(){
		// DECLARE A SIMPLE PARSER where all geometries are allowed by default:
		ADQLParser parser = new ADQLParser(new DBChecker(tables));

		// Test with several geometries while all are allowed:
		try{
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
		}catch(ParseException pe){
			pe.printStackTrace();
			fail("This query contains several geometries, and all are theoretically allowed: this test should have succeeded!");
		}

		// Test with several geometries while only the allowed ones:
		try{
			parser = new ADQLParser(new DBChecker(tables, new ArrayList<FunctionDef>(0), Arrays.asList(new String[]{"CONTAINS","POINT","CIRCLE"}), null));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
		}catch(ParseException pe){
			pe.printStackTrace();
			fail("This query contains several geometries, and all are theoretically allowed: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT * FROM foo WHERE INTERSECTS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;");
			fail("This query contains a not-allowed geometry function (INTERSECTS): this test should have failed!");
		}catch(ParseException pe){
			assertTrue(pe instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
			assertEquals(1, ex.getNbErrors());
			assertEquals("The geometrical function \"INTERSECTS\" is not available in this implementation!", ex.getErrors().next().getMessage());
		}

		// Test by adding REGION:
		try{
			parser = new ADQLParser(new DBChecker(tables, new ArrayList<FunctionDef>(0), Arrays.asList(new String[]{"CONTAINS","POINT","CIRCLE","REGION"}), null));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('Position 12.3 45.6'), REGION('circle 1.2 2.3 5')) = 1;"));
		}catch(ParseException pe){
			pe.printStackTrace();
			fail("This query contains several geometries, and all are theoretically allowed: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('Position 12.3 45.6'), REGION('BOX 1.2 2.3 5 9')) = 1;");
			fail("This query contains a not-allowed geometry function (BOX): this test should have failed!");
		}catch(ParseException pe){
			assertTrue(pe instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
			assertEquals(1, ex.getNbErrors());
			assertEquals("The geometrical function \"BOX\" is not available in this implementation!", ex.getErrors().next().getMessage());
		}

		// Test with several geometries while none geometry is allowed:
		try{
			parser = new ADQLParser(new DBChecker(tables, new ArrayList<FunctionDef>(0), new ArrayList<String>(0), null));
			parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;");
			fail("This query contains geometries while they are all forbidden: this test should have failed!");
		}catch(ParseException pe){
			assertTrue(pe instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
			assertEquals(3, ex.getNbErrors());
			Iterator<ParseException> itErrors = ex.getErrors();
			assertEquals("The geometrical function \"CONTAINS\" is not available in this implementation!", itErrors.next().getMessage());
			assertEquals("The geometrical function \"POINT\" is not available in this implementation!", itErrors.next().getMessage());
			assertEquals("The geometrical function \"CIRCLE\" is not available in this implementation!", itErrors.next().getMessage());
		}
	}

	@Test
	public void testCoordSys(){
		// DECLARE A SIMPLE PARSER where all coordinate systems are allowed by default:
		ADQLParser parser = new ADQLParser(new DBChecker(tables));

		// Test with several coordinate systems while all are allowed:
		try{
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('icrs', 12.3, 45.6), CIRCLE('cartesian2', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('lsr', 12.3, 45.6), CIRCLE('galactic heliocenter', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('unknownframe', 12.3, 45.6), CIRCLE('galactic unknownrefpos spherical2', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('position icrs lsr 12.3 45.6'), REGION('circle fk5 1.2 2.3 5')) = 1;"));
			assertNotNull(parser.parseQuery("SELECT Region('not(position 1 2)') FROM foo;"));
		}catch(ParseException pe){
			pe.printStackTrace();
			fail("This query contains several valid coordinate systems, and all are theoretically allowed: this test should have succeeded!");
		}

		// Concatenation as coordinate systems not checked:
		try{
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('From ' || 'here', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
		}catch(ParseException pe){
			pe.printStackTrace();
			fail("This query contains a concatenation as coordinate systems (but only string constants are checked): this test should have succeeded!");
		}

		// Test with several coordinate systems while only some allowed:
		try{
			parser = new ADQLParser(new DBChecker(tables, new ArrayList<FunctionDef>(0), null, Arrays.asList(new String[]{"icrs * *","fk4 geocenter *","galactic * spherical2"})));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('icrs', 12.3, 45.6), CIRCLE('cartesian3', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT POINT('fk4', 12.3, 45.6) FROM foo;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('fk4 geocenter', 12.3, 45.6), CIRCLE('cartesian2', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('galactic', 12.3, 45.6), CIRCLE('galactic spherical2', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('galactic geocenter', 12.3, 45.6), CIRCLE('galactic lsr spherical2', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('position galactic lsr 12.3 45.6'), REGION('circle icrs 1.2 2.3 5')) = 1;"));
			assertNotNull(parser.parseQuery("SELECT Region('not(position 1 2)') FROM foo;"));
		}catch(ParseException pe){
			pe.printStackTrace();
			fail("This query contains several valid coordinate systems, and all are theoretically allowed: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT POINT('fk5 geocenter', 12.3, 45.6) FROM foo;");
			fail("This query contains a not-allowed coordinate system ('fk5' is not allowed): this test should have failed!");
		}catch(ParseException pe){
			assertTrue(pe instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Coordinate system \"fk5 geocenter\" (= \"FK5 GEOCENTER SPHERICAL2\") not allowed in this implementation. Allowed coordinate systems are: fk4 geocenter *, galactic * spherical2, icrs * *", ex.getErrors().next().getMessage());
		}
		try{
			parser.parseQuery("SELECT Region('not(position fk5 heliocenter 1 2)') FROM foo;");
			fail("This query contains a not-allowed coordinate system ('fk5' is not allowed): this test should have failed!");
		}catch(ParseException pe){
			assertTrue(pe instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Coordinate system \"FK5 HELIOCENTER\" (= \"FK5 HELIOCENTER SPHERICAL2\") not allowed in this implementation. Allowed coordinate systems are: fk4 geocenter *, galactic * spherical2, icrs * *", ex.getErrors().next().getMessage());
		}

		// Test with a coordinate system while none is allowed:
		try{
			parser = new ADQLParser(new DBChecker(tables, new ArrayList<FunctionDef>(0), null, new ArrayList<String>(0)));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('', 12.3, 45.6), CIRCLE('', 1.2, 2.3, 5)) = 1;"));
			assertNotNull(parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(REGION('position 12.3 45.6'), REGION('circle 1.2 2.3 5')) = 1;"));
			assertNotNull(parser.parseQuery("SELECT Region('not(position 1 2)') FROM foo;"));
		}catch(ParseException pe){
			pe.printStackTrace();
			fail("This query specifies none coordinate system: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT * FROM foo WHERE CONTAINS(POINT('ICRS SPHERICAL2', 12.3, 45.6), CIRCLE('icrs', 1.2, 2.3, 5)) = 1;");
			fail("This query specifies coordinate systems while they are all forbidden: this test should have failed!");
		}catch(ParseException pe){
			assertTrue(pe instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
			assertEquals(2, ex.getNbErrors());
			Iterator<ParseException> itErrors = ex.getErrors();
			assertEquals("Coordinate system \"ICRS SPHERICAL2\" (= \"ICRS UNKNOWNREFPOS SPHERICAL2\") not allowed in this implementation. No coordinate system is allowed!", itErrors.next().getMessage());
			assertEquals("Coordinate system \"icrs\" (= \"ICRS UNKNOWNREFPOS SPHERICAL2\") not allowed in this implementation. No coordinate system is allowed!", itErrors.next().getMessage());
		}
		try{
			parser.parseQuery("SELECT Region('not(position fk4 1 2)') FROM foo;");
			fail("This query specifies coordinate systems while they are all forbidden: this test should have failed!");
		}catch(ParseException pe){
			assertTrue(pe instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)pe;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Coordinate system \"FK4\" (= \"FK4 UNKNOWNREFPOS SPHERICAL2\") not allowed in this implementation. No coordinate system is allowed!", ex.getErrors().next().getMessage());
		}
	}

	@Test
	public void testTypesChecking(){
		// DECLARE A SIMPLE PARSER:
		ADQLParser parser = new ADQLParser(new DBChecker(tables));

		// Test the type of columns generated by the parser:
		try{
			ADQLQuery query = parser.parseQuery("SELECT colS, colI, colG FROM foo;");
			ADQLOperand colS = query.getSelect().get(0).getOperand();
			ADQLOperand colI = query.getSelect().get(1).getOperand();
			ADQLOperand colG = query.getSelect().get(2).getOperand();
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
		}catch(ParseException e1){
			if (e1 instanceof UnresolvedIdentifiersException)
				((UnresolvedIdentifiersException)e1).getErrors().next().printStackTrace();
			else
				e1.printStackTrace();
			fail("This query contains known columns: this test should have succeeded!");
		}

		// Test the expected type - NUMERIC - generated by the parser:
		try{
			assertNotNull(parser.parseQuery("SELECT colI * 3 FROM foo;"));
		}catch(ParseException e){
			e.printStackTrace();
			fail("This query contains a product between 2 numerics: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT colS * 3 FROM foo;");
			fail("This query contains a product between a string and an integer: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A numeric value was expected instead of \"colS\".", ex.getErrors().next().getMessage());
		}
		try{
			parser.parseQuery("SELECT colG * 3 FROM foo;");
			fail("This query contains a product between a geometry and an integer: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A numeric value was expected instead of \"colG\".", ex.getErrors().next().getMessage());
		}

		// Test the expected type - STRING - generated by the parser:
		try{
			assertNotNull(parser.parseQuery("SELECT colS || 'blabla' FROM foo;"));
		}catch(ParseException e){
			e.printStackTrace();
			fail("This query contains a concatenation between 2 strings: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT colI || 'blabla' FROM foo;");
			fail("This query contains a concatenation between an integer and a string: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A string value was expected instead of \"colI\".", ex.getErrors().next().getMessage());
		}
		try{
			parser.parseQuery("SELECT colG || 'blabla' FROM foo;");
			fail("This query contains a concatenation between a geometry and a string: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A string value was expected instead of \"colG\".", ex.getErrors().next().getMessage());
		}

		// Test the expected type - GEOMETRY - generated by the parser:
		try{
			assertNotNull(parser.parseQuery("SELECT CONTAINS(colG, CIRCLE('', 1, 2, 5)) FROM foo;"));
		}catch(ParseException e){
			e.printStackTrace();
			fail("This query contains a geometrical predicate between 2 geometries: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT CONTAINS(colI, CIRCLE('', 1, 2, 5)) FROM foo;");
			fail("This query contains a geometrical predicate between an integer and a geometry: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A geometry was expected instead of \"colI\".", ex.getErrors().next().getMessage());
		}
		try{
			parser.parseQuery("SELECT CONTAINS(colS, CIRCLE('', 1, 2, 5)) FROM foo;");
			fail("This query contains a geometrical predicate between a string and a geometry: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A geometry was expected instead of \"colS\".", ex.getErrors().next().getMessage());
		}

		// DECLARE SOME UDFs:
		FunctionDef[] udfs = new FunctionDef[]{new FunctionDef("toto", new DBType(DBDatatype.VARCHAR)),new FunctionDef("tata", new DBType(DBDatatype.INTEGER)),new FunctionDef("titi", new DBType(DBDatatype.REGION))};
		parser = new ADQLParser(new DBChecker(tables, Arrays.asList(udfs)));

		// Test the return type of the function TOTO generated by the parser:
		try{
			ADQLQuery query = parser.parseQuery("SELECT toto() FROM foo;");
			ADQLOperand fct = query.getSelect().get(0).getOperand();
			assertTrue(fct instanceof DefaultUDF);
			assertNotNull(((DefaultUDF)fct).getDefinition());
			assertTrue(fct.isString());
			assertFalse(fct.isNumeric());
			assertFalse(fct.isGeometry());
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("This query contains a DECLARED UDF: this test should have succeeded!");
		}

		// Test the return type checking inside a whole query:
		try{
			assertNotNull(parser.parseQuery("SELECT toto() || 'Blabla ' AS \"SuperText\" FROM foo;"));
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("This query contains a DECLARED UDF concatenated to a String: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT toto()*3 AS \"SuperError\" FROM foo;");
			fail("This query contains a DECLARED UDF BUT used as numeric...which is here not possible: this test should have failed!");
		}catch(ParseException e1){
			assertTrue(e1 instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A numeric value was expected instead of \"toto()\".", ex.getErrors().next().getMessage());
		}

		// Test the return type of the function TATA generated by the parser:
		try{
			ADQLQuery query = parser.parseQuery("SELECT tata() FROM foo;");
			ADQLOperand fct = query.getSelect().get(0).getOperand();
			assertTrue(fct instanceof DefaultUDF);
			assertNotNull(((DefaultUDF)fct).getDefinition());
			assertFalse(fct.isString());
			assertTrue(fct.isNumeric());
			assertFalse(fct.isGeometry());
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("This query contains a DECLARED UDF: this test should have succeeded!");
		}

		// Test the return type checking inside a whole query:
		try{
			assertNotNull(parser.parseQuery("SELECT tata()*3 AS \"aNumeric\" FROM foo;"));
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("This query contains a DECLARED UDF multiplicated by 3: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT 'Blabla ' || tata() AS \"SuperError\" FROM foo;");
			fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
		}catch(ParseException e1){
			assertTrue(e1 instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A string value was expected instead of \"tata()\".", ex.getErrors().next().getMessage());
		}
		try{
			parser.parseQuery("SELECT tata() || 'Blabla ' AS \"SuperError\" FROM foo;");
			fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
		}catch(ParseException e1){
			assertTrue(e1 instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A string value was expected instead of \"tata()\".", ex.getErrors().next().getMessage());
		}

		// Test the return type of the function TITI generated by the parser:
		try{
			ADQLQuery query = parser.parseQuery("SELECT titi() FROM foo;");
			ADQLOperand fct = query.getSelect().get(0).getOperand();
			assertTrue(fct instanceof DefaultUDF);
			assertNotNull(((DefaultUDF)fct).getDefinition());
			assertFalse(fct.isString());
			assertFalse(fct.isNumeric());
			assertTrue(fct.isGeometry());
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("This query contains a DECLARED UDF: this test should have succeeded!");
		}

		// Test the return type checking inside a whole query:
		try{
			parser.parseQuery("SELECT CONTAINS(colG, titi()) ' AS \"Super\" FROM foo;");
			fail("Geometrical UDFs are not allowed for the moment in the ADQL language: this test should have failed!");
		}catch(ParseException e1){
			assertTrue(e1 instanceof ParseException);
			assertEquals(" Encountered \"(\". Was expecting one of: \")\" \".\" \".\" \")\" ", e1.getMessage());
		}
		try{
			parser.parseQuery("SELECT titi()*3 AS \"SuperError\" FROM foo;");
			fail("This query contains a DECLARED UDF BUT used as numeric...which is here not possible: this test should have failed!");
		}catch(ParseException e1){
			assertTrue(e1 instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e1;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A numeric value was expected instead of \"titi()\".", ex.getErrors().next().getMessage());
		}

		// CLEAR ALL UDFs AND ALLOW UNKNOWN FUNCTION:
		parser = new ADQLParser(new DBChecker(tables, null));

		// Test again:
		try{
			assertNotNull(parser.parseQuery("SELECT toto() FROM foo;"));
		}catch(ParseException e){
			e.printStackTrace();
			fail("The parser allow ANY unknown function: this test should have succeeded!");
		}

		// Test the return type of the function generated by the parser:
		try{
			ADQLQuery query = parser.parseQuery("SELECT toto() FROM foo;");
			ADQLOperand fct = query.getSelect().get(0).getOperand();
			assertTrue(fct instanceof DefaultUDF);
			assertNull(((DefaultUDF)fct).getDefinition());
			assertTrue(fct.isString());
			assertTrue(fct.isNumeric());
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("The parser allow ANY unknown function: this test should have succeeded!");
		}

		// DECLARE THE UDF (while unknown functions are allowed):
		parser = new ADQLParser(new DBChecker(tables, Arrays.asList(new FunctionDef[]{new FunctionDef("toto", new DBType(DBDatatype.VARCHAR))})));

		// Test the return type of the function generated by the parser:
		try{
			ADQLQuery query = parser.parseQuery("SELECT toto() FROM foo;");
			ADQLOperand fct = query.getSelect().get(0).getOperand();
			assertTrue(fct instanceof DefaultUDF);
			assertNotNull(((DefaultUDF)fct).getDefinition());
			assertTrue(fct.isString());
			assertFalse(fct.isNumeric());
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("The parser allow ANY unknown function: this test should have succeeded!");
		}

		// DECLARE UDFs WITH SAME NAMES BUT DIFFERENT TYPE OF ARGUMENT:
		udfs = new FunctionDef[]{new FunctionDef("toto", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("attr", new DBType(DBDatatype.VARCHAR))}),new FunctionDef("toto", new DBType(DBDatatype.INTEGER), new FunctionParam[]{new FunctionParam("attr", new DBType(DBDatatype.INTEGER))}),new FunctionDef("toto", new DBType(DBDatatype.INTEGER), new FunctionParam[]{new FunctionParam("attr", new DBType(DBDatatype.POINT))})};
		parser = new ADQLParser(new DBChecker(tables, Arrays.asList(udfs)));

		// Test the return type in function of the parameter:
		try{
			assertNotNull(parser.parseQuery("SELECT toto('blabla') AS toto1, toto(123) AS toto2, toto(POINT('', 1, 2)) AS toto3  FROM foo;"));
		}catch(ParseException e1){
			e1.printStackTrace();
			fail("This query contains two DECLARED UDFs used here: this test should have succeeded!");
		}
		try{
			parser.parseQuery("SELECT toto('blabla') * 123 AS \"SuperError\" FROM foo;");
			fail("This query contains a DECLARED UDF BUT used as numeric...which is here not possible: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A numeric value was expected instead of \"toto('blabla')\".", ex.getErrors().next().getMessage());
		}
		try{
			parser.parseQuery("SELECT toto(123) || 'blabla' AS \"SuperError\" FROM foo;");
			fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A string value was expected instead of \"toto(123)\".", ex.getErrors().next().getMessage());
		}
		try{
			parser.parseQuery("SELECT toto(POINT('', 1, 2)) || 'blabla' AS \"SuperError\" FROM foo;");
			fail("This query contains a DECLARED UDF BUT used as string...which is here not possible: this test should have failed!");
		}catch(ParseException e){
			assertTrue(e instanceof UnresolvedIdentifiersException);
			UnresolvedIdentifiersException ex = (UnresolvedIdentifiersException)e;
			assertEquals(1, ex.getNbErrors());
			assertEquals("Type mismatch! A string value was expected instead of \"toto(POINT('', 1, 2))\".", ex.getErrors().next().getMessage());
		}
	}

	private static class WrongUDFToto extends UDFToto {
		public WrongUDFToto(final ADQLOperand[] params) throws Exception{
			super(params);
			throw new Exception("Systematic error!");
		}
	}

	public static class UDFToto extends UserDefinedFunction {
		protected StringConstant fakeParam;

		public UDFToto(final ADQLOperand[] params) throws Exception{
			if (params == null || params.length == 0)
				throw new Exception("Missing parameter for the user defined function \"toto\"!");
			else if (params.length > 1)
				throw new Exception("Too many parameters for the function \"toto\"! Only one is required.");
			else if (!(params[0] instanceof StringConstant))
				throw new Exception("Wrong parameter type! The parameter of the UDF \"toto\" must be a string constant.");
			fakeParam = (StringConstant)params[0];
		}

		@Override
		public final boolean isNumeric(){
			return false;
		}

		@Override
		public final boolean isString(){
			return true;
		}

		@Override
		public final boolean isGeometry(){
			return false;
		}

		@Override
		public ADQLObject getCopy() throws Exception{
			ADQLOperand[] params = new ADQLOperand[]{(StringConstant)fakeParam.getCopy()};
			return new UDFToto(params);
		}

		@Override
		public final String getName(){
			return "toto";
		}

		@Override
		public final ADQLOperand[] getParameters(){
			return new ADQLOperand[]{fakeParam};
		}

		@Override
		public final int getNbParameters(){
			return 1;
		}

		@Override
		public final ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
			if (index != 0)
				throw new ArrayIndexOutOfBoundsException("Incorrect parameter index: " + index + "! The function \"toto\" has only one parameter.");
			return fakeParam;
		}

		@Override
		public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
			if (index != 0)
				throw new ArrayIndexOutOfBoundsException("Incorrect parameter index: " + index + "! The function \"toto\" has only one parameter.");
			else if (!(replacer instanceof StringConstant))
				throw new Exception("Wrong parameter type! The parameter of the UDF \"toto\" must be a string constant.");
			return (fakeParam = (StringConstant)replacer);
		}

		@Override
		public String translate(final ADQLTranslator caller) throws TranslationException{
			/* Note: Since this function is totally fake, this function will be replaced in SQL by its parameter (the string). */
			return caller.translate(fakeParam);
		}
	}

}
