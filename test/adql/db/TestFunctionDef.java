package adql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.db.DBType.DBDatatype;
import adql.db.FunctionDef.FunctionParam;
import adql.parser.ParseException;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.geometry.PointFunction;

public class TestFunctionDef {

	@Test
	public void testIsString(){
		for(DBDatatype type : DBDatatype.values()){
			switch(type){
				case CHAR:
				case VARCHAR:
				case TIMESTAMP:
				case CLOB:
				case UNKNOWN:
					assertTrue(new FunctionDef("foo", new DBType(type)).isString);
					break;
				default:
					assertFalse(new FunctionDef("foo", new DBType(type)).isString);
			}
		}
	}

	@Test
	public void testIsGeometry(){
		for(DBDatatype type : DBDatatype.values()){
			switch(type){
				case POINT:
				case REGION:
				case UNKNOWN:
					assertTrue(new FunctionDef("foo", new DBType(type)).isGeometry);
					break;
				default:
					assertFalse(new FunctionDef("foo", new DBType(type)).isGeometry);
			}
		}
	}

	@Test
	public void testIsNumeric(){
		for(DBDatatype type : DBDatatype.values()){
			switch(type){
				case CHAR:
				case VARCHAR:
				case TIMESTAMP:
				case POINT:
				case REGION:
				case CLOB:
					assertFalse(new FunctionDef("foo", new DBType(type)).isNumeric);
					break;
				default:
					assertTrue(new FunctionDef("foo", new DBType(type)).isNumeric);
			}
		}
	}

	@Test
	public void testToString(){
		assertEquals("fct1()", new FunctionDef("fct1").toString());
		assertEquals("fct1() -> VARCHAR", new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR)).toString());
		assertEquals("fct1(foo DOUBLE) -> VARCHAR", new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("foo", new DBType(DBDatatype.DOUBLE))}).toString());
		assertEquals("fct1(foo DOUBLE)", new FunctionDef("fct1", new FunctionParam[]{new FunctionParam("foo", new DBType(DBDatatype.DOUBLE))}).toString());
		assertEquals("fct1(foo DOUBLE, pt POINT) -> VARCHAR", new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("foo", new DBType(DBDatatype.DOUBLE)),new FunctionParam("pt", new DBType(DBDatatype.POINT))}).toString());
		assertEquals("fct1(foo DOUBLE, pt POINT)", new FunctionDef("fct1", null, new FunctionParam[]{new FunctionParam("foo", new DBType(DBDatatype.DOUBLE)),new FunctionParam("pt", new DBType(DBDatatype.POINT))}).toString());
	}

	@Test
	public void testParse(){
		final String WRONG_FULL_SYNTAX = "Wrong function definition syntax! Expected syntax: \"<regular_identifier>(<parameters>?) <return_type>?\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <return_type>=\" -> <type_name>\", <parameters>=\"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"foo()\", \"foo() -> VARCHAR\", \"foo(param INTEGER)\", \"foo(param1 INTEGER, param2 DOUBLE) -> DOUBLE\"";
		final String WRONG_PARAM_SYNTAX = "Wrong parameters syntax! Expected syntax: \"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"()\", \"(param INTEGER)\", \"(param1 INTEGER, param2 DOUBLE)\"";

		// NULL test:
		try{
			FunctionDef.parse(null);
			fail("A NULL string is not valide!");
		}catch(Exception ex){
			assertTrue(ex instanceof NullPointerException);
			assertEquals("Missing string definition to build a FunctionDef!", ex.getMessage());
		}

		// EMPTY STRING test:
		try{
			FunctionDef.parse("");
			fail("An empty string is not valide!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_FULL_SYNTAX, ex.getMessage());
		}

		// CORRECT string definitions:
		try{
			assertEquals("foo()", FunctionDef.parse("foo()").toString());
			assertEquals("foo() -> VARCHAR", FunctionDef.parse("foo() -> string").toString());
			assertEquals("foo() -> VARCHAR", FunctionDef.parse("foo()->string").toString());
			assertEquals("foo(toto VARCHAR) -> SMALLINT", FunctionDef.parse("foo(toto varchar) -> boolean").toString());
			assertEquals("foo(param1 DOUBLE, param2 INTEGER) -> DOUBLE", FunctionDef.parse(" foo ( param1	numeric,	param2    int )	->	DOUBLE ").toString());
			assertEquals("foo_ALTernative2first(p POINT, d TIMESTAMP) -> TIMESTAMP", FunctionDef.parse("foo_ALTernative2first	(p POINT,d date) -> time").toString());
			assertEquals("blabla_123(toto INTEGER, bla SMALLINT, truc CLOB, bidule CHAR, smurph POINT, date TIMESTAMP) -> SMALLINT", FunctionDef.parse("blabla_123(toto int4, bla bool, truc text, bidule character, smurph point, date timestamp) -> BOOLEAN").toString());
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("All this string definitions are correct.");
		}

		// TYPE PARAMETER test:
		try{
			for(DBDatatype t : DBDatatype.values()){
				switch(t){
					case CHAR:
					case VARCHAR:
					case BINARY:
					case VARBINARY:
						assertEquals("foo() -> " + t.toString() + "(10)", FunctionDef.parse("foo() -> " + t.toString() + "(10)").toString());
						break;
					default:
						assertEquals("foo() -> " + t.toString(), FunctionDef.parse("foo() -> " + t.toString() + "(10)").toString());
				}
			}
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Wrong type parsing!");
		}

		// TYPE WITH SPACES AND/OR PARAMETER test:
		try{
			assertEquals("foo() -> DOUBLE", FunctionDef.parse("foo() -> double precision").toString());
			assertEquals("foo(bar DOUBLE)", FunctionDef.parse("foo(bar   DOUBLE  Precision  )").toString());
			assertEquals("foo() -> VARCHAR", FunctionDef.parse("foo() -> character varying").toString());
			assertEquals("foo(bar VARBINARY)", FunctionDef.parse("foo(bar bit   varying)").toString());
			assertEquals("foo(bar VARCHAR(12))", FunctionDef.parse("foo(bar varchar  (12))").toString());
			assertEquals("foo(bar VARCHAR(12))", FunctionDef.parse("foo(bar character varying (12))").toString());
			assertEquals("foo() -> DOUBLE", FunctionDef.parse("foo() -> double precision (2)").toString());
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Wrong type parsing!");
		}

		// WRONG string definitions:
		try{
			FunctionDef.parse("123()");
			fail("No number is allowed as first character of a function name!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_FULL_SYNTAX, ex.getMessage());
		}
		try{
			FunctionDef.parse("1foo()");
			fail("No number is allowed as first character of a function name!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_FULL_SYNTAX, ex.getMessage());
		}
		try{
			FunctionDef.parse("foo,truc()");
			fail("No other character than [a-zA-Z0-9_] is allowed after a first character [a-zA-Z] in a function name!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_FULL_SYNTAX, ex.getMessage());
		}
		try{
			FunctionDef.parse("foo");
			fail("A function definition must contain at list parenthesis even if there is no parameter.");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_FULL_SYNTAX, ex.getMessage());
		}
		try{
			FunctionDef.parse("foo(param)");
			fail("A parameter must always have a type!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals("Wrong syntax for the 1-th parameter: \"param\"! Expected syntax: \"(<regular_identifier> <type_name> (, <regular_identifier> <type_name>)*)\", where <regular_identifier>=\"[a-zA-Z]+[a-zA-Z0-9_]*\", <type_name> should be one of the types described in the UPLOAD section of the TAP documentation. Examples of good syntax: \"()\", \"(param INTEGER)\", \"(param1 INTEGER, param2 DOUBLE)\"", ex.getMessage());
		}
		try{
			FunctionDef fct = FunctionDef.parse("foo()->aType");
			assertTrue(fct.isUnknown);
			assertTrue(fct.isString);
			assertTrue(fct.isNumeric);
			assertTrue(fct.isGeometry);
			assertEquals("?aType?", fct.returnType.type.toString());
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}
		try{
			FunctionDef fct = FunctionDef.parse("foo()->aType(10)");
			assertTrue(fct.isUnknown);
			assertTrue(fct.isString);
			assertTrue(fct.isNumeric);
			assertTrue(fct.isGeometry);
			assertEquals("?aType(10)?", fct.returnType.type.toString());
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}
		try{
			FunctionDef.parse("foo() -> ");
			fail("The return type is missing!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_FULL_SYNTAX, ex.getMessage());
		}
		try{
			FunctionDef.parse("foo(,)");
			fail("Missing parameter definition!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_PARAM_SYNTAX, ex.getMessage());
		}
		try{
			FunctionDef.parse("foo(param1 int,)");
			fail("Missing parameter definition!");
		}catch(Exception ex){
			assertTrue(ex instanceof ParseException);
			assertEquals(WRONG_PARAM_SYNTAX, ex.getMessage());
		}
		try{
			FunctionDef fct = FunctionDef.parse("foo(param1 aType)");
			assertTrue(fct.getParam(0).type.isUnknown());
			assertTrue(fct.getParam(0).type.isString());
			assertTrue(fct.getParam(0).type.isNumeric());
			assertTrue(fct.getParam(0).type.isGeometry());
			assertEquals("?aType?", fct.getParam(0).type.toString());
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}
		try{
			FunctionDef fct = FunctionDef.parse("foo(param1 aType(10))");
			assertTrue(fct.getParam(0).type.isUnknown());
			assertTrue(fct.getParam(0).type.isString());
			assertTrue(fct.getParam(0).type.isNumeric());
			assertTrue(fct.getParam(0).type.isGeometry());
			assertEquals("?aType(10)?", fct.getParam(0).type.toString());
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}
	}

	@Test
	public void testCompareToFunctionDef(){
		// DEFINITION 1 :: fct1() -> VARCHAR
		FunctionDef def1 = new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR));

		// TEST :: Identity test (def1 with def1): [EQUAL]
		assertEquals(0, def1.compareTo(def1));

		// TEST :: With a function having a different name and also no parameter: [GREATER]
		assertEquals(1, def1.compareTo(new FunctionDef("fct0", new DBType(DBDatatype.VARCHAR))));

		// TEST :: With a function having the same name, but a different return type: [EQUAL}
		assertEquals(0, def1.compareTo(new FunctionDef("fct1", new DBType(DBDatatype.INTEGER))));

		// TEST :: With a function having the same name, but 2 parameters: [LESS (4 characters: ø against 1010)]
		assertEquals(-6, def1.compareTo(new FunctionDef("fct1", new DBType(DBDatatype.INTEGER), new FunctionParam[]{new FunctionParam("foo", new DBType(DBDatatype.INTEGER)),new FunctionParam("foo", new DBType(DBDatatype.INTEGER))})));

		// DEFINITION 1 :: fct1(foo1 CHAR(12), foo2 DOUBLE) -> VARCHAR
		def1 = new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("foo1", new DBType(DBDatatype.CHAR, 12)),new FunctionParam("foo2", new DBType(DBDatatype.DOUBLE))});

		// TEST :: Identity test (def1 with def1): [EQUAL]
		assertEquals(0, def1.compareTo(def1));

		// DEFINITION 2 :: fct1(foo1 CHAR(12), foo2 VARCHAR) -> VARCHAR
		FunctionDef def2 = new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("foo1", new DBType(DBDatatype.CHAR, 12)),new FunctionParam("foo2", new DBType(DBDatatype.VARCHAR))});

		// TEST :: Identity test (def2 with def2): [EQUAL]
		assertEquals(0, def2.compareTo(def2));

		// TEST :: Same name, but different type for the last parameter only: [GREATER (because Numeric = 10 > String = 01)]
		assertEquals(1, def1.compareTo(def2));

		// DEFINITION 2 :: fct2(foo1 CHAR(12), foo2 DOUBLE) -> VARCHAR
		def2 = new FunctionDef("fct2", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("foo1", new DBType(DBDatatype.CHAR, 12)),new FunctionParam("foo2", new DBType(DBDatatype.DOUBLE))});

		// TEST :: Identity test (def2 with def2): [EQUAL]
		assertEquals(0, def2.compareTo(def2));

		// TEST :: Different name but same parameters: [LESS]
		assertEquals(-1, def1.compareTo(def2));

		// DEFINITION 2 :: fct1(foo1 CHAR(12), foo2 POINT) -> VARCHAR
		def2 = new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("foo1", new DBType(DBDatatype.CHAR, 12)),new FunctionParam("foo2", new DBType(DBDatatype.POINT))});

		// TEST :: Identity test (def2 with def2): [EQUAL]
		assertEquals(0, def2.compareTo(def2));

		// TEST :: Same name, but different type for the last parameter only: [GREATER]
		assertEquals(1, def1.compareTo(def2));
	}

	@Test
	public void testCompareToADQLFunction(){
		// DEFINITION :: fct1() -> VARCHAR
		FunctionDef def = new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR));

		// TEST :: NULL:
		try{
			def.compareTo((ADQLFunction)null);
			fail("Missing ADQL function for comparison with FunctionDef!");
		}catch(Exception e){
			assertTrue(e instanceof NullPointerException);
			assertEquals("Missing ADQL function with which comparing this function definition!", e.getMessage());
		}

		// TEST :: "fct1()": [EQUAL]
		assertEquals(0, def.compareTo(new DefaultUDF("fct1", null)));

		// TEST :: "fct0()": [GREATER]
		assertEquals(1, def.compareTo(new DefaultUDF("fct0", null)));

		// TEST :: "fct1(12.3, 3.14)": [LESS (of 2 params)]
		assertEquals(-2, def.compareTo(new DefaultUDF("fct1", new ADQLOperand[]{new NumericConstant(12.3),new NumericConstant(3.14)})));

		// DEFINITION :: fct1(foo1 CHAR(12), foo2 DOUBLE) -> VARCHAR
		def = new FunctionDef("fct1", new DBType(DBDatatype.VARCHAR), new FunctionParam[]{new FunctionParam("foo1", new DBType(DBDatatype.CHAR, 12)),new FunctionParam("foo2", new DBType(DBDatatype.DOUBLE))});

		// TEST :: "fct1('blabla', 'blabla2')": [GREATER (because the second param is numeric and Numeric = 10 > String = 01)]
		assertEquals(1, def.compareTo(new DefaultUDF("fct1", new ADQLOperand[]{new StringConstant("blabla"),new StringConstant("blabla2")})));

		// TEST :: "fct1('blabla', POINT('COORDSYS', 1.2, 3.4))": [GREATER (same reason ; POINT is considered as a String)]
		try{
			assertEquals(1, def.compareTo(new DefaultUDF("fct1", new ADQLOperand[]{new StringConstant("blabla"),new PointFunction(new StringConstant("COORDSYS"), new NumericConstant(1.2), new NumericConstant(3.4))})));
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}

}
