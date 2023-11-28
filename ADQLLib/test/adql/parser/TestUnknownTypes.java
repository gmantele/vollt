<<<<<<< HEAD:test/adql/parser/TestUnknownTypes.java
package adql.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.FunctionDef;
import adql.query.ADQLQuery;
import adql.query.ADQLSet;

public class TestUnknownTypes {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DBType.DBDatatype.UNKNOWN.setCustomType(null);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	public void testForFctDef() {
		// Test with the return type:
		try {
			FunctionDef fct = FunctionDef.parse("foo()->aType");
			assertTrue(fct.isUnknown());
			assertFalse(fct.isString());
			assertFalse(fct.isNumeric());
			assertFalse(fct.isGeometry());
			assertEquals("?aType?", fct.returnType.type.toString());
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}

		// Test with a parameter type:
		try {
			FunctionDef fct = FunctionDef.parse("foo(param1 aType)");
			assertTrue(fct.getParam(0).type.isUnknown());
			assertFalse(fct.getParam(0).type.isString());
			assertFalse(fct.getParam(0).type.isNumeric());
			assertFalse(fct.getParam(0).type.isGeometry());
			assertEquals("?aType?", fct.getParam(0).type.toString());
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}
	}

	@Test
	public void testForColumns() {
		final String QUERY_TXT = "SELECT FOO(C1), FOO(C2), FOO(C4), C1, C2, C3, C4 FROM T1";

		try {
			// Create the parser:
			ADQLParser parser = new ADQLParser();

			// Create table/column metadata:
			DefaultDBTable table1 = new DefaultDBTable("T1");
			table1.addColumn(new DefaultDBColumn("C1", table1));
			table1.addColumn(new DefaultDBColumn("C2", new DBType(DBDatatype.UNKNOWN), table1));
			table1.addColumn(new DefaultDBColumn("C3", new DBType(DBDatatype.VARCHAR), table1));
			table1.addColumn(new DefaultDBColumn("C4", new DBType(DBDatatype.UNKNOWN_NUMERIC), table1));
			Collection<DBTable> tList = Arrays.asList(new DBTable[]{ table1 });

			// Check the type of the column T1.C1:
			DBColumn col = table1.getColumn("C1", true);
			assertNotNull(col);
			assertNull(col.getDatatype());

			// Check the type of the column T1.C2:
			col = table1.getColumn("C2", true);
			assertNotNull(col);
			assertNotNull(col.getDatatype());
			assertTrue(col.getDatatype().isUnknown());
			assertFalse(col.getDatatype().isNumeric());
			assertFalse(col.getDatatype().isString());
			assertFalse(col.getDatatype().isGeometry());
			assertEquals("UNKNOWN", col.getDatatype().toString());

			// Check the type of the column T1.C4:
			col = table1.getColumn("C4", true);
			assertNotNull(col);
			assertNotNull(col.getDatatype());
			assertTrue(col.getDatatype().isUnknown());
			assertTrue(col.getDatatype().isNumeric());
			assertFalse(col.getDatatype().isString());
			assertFalse(col.getDatatype().isGeometry());
			assertEquals("UNKNOWN_NUMERIC", col.getDatatype().toString());

			// Define a UDF, and allow all geometrical functions and coordinate systems:
			FunctionDef udf1 = FunctionDef.parse("FOO(x INTEGER) -> INTEGER");
			parser.getSupportedFeatures().support(udf1.toLanguageFeature());
			Collection<FunctionDef> udfList = Arrays.asList(new FunctionDef[]{ udf1 });
			Collection<String> geoList = null;
			Collection<String> csList = null;

			// Create the Query checker:
			QueryChecker checker = new DBChecker(tList, udfList, geoList, csList);

			// Parse the query:
			ADQLSet pq = parser.parseQuery(QUERY_TXT);

			// Check the parsed query:
			checker.check(pq);

			/* Ensure the type of every ADQLColumn is as expected: */
			// isNumeric() = true for FOO(C1), but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(0).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(0).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(0).getOperand().isGeometry());
			// isNumeric() = true for FOO(C2), but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(1).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(1).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(1).getOperand().isGeometry());
			// isNumeric() = true for FOO(C4), but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(2).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(2).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(2).getOperand().isGeometry());
			// isNumeric() = isString() = isGeometry() for C1
			assertTrue(((ADQLQuery)pq).getSelect().get(3).getOperand().isNumeric());
			assertTrue(((ADQLQuery)pq).getSelect().get(3).getOperand().isString());
			assertTrue(((ADQLQuery)pq).getSelect().get(3).getOperand().isGeometry());
			// isNumeric() = isString() = isGeometry() for C2
			assertTrue(((ADQLQuery)pq).getSelect().get(4).getOperand().isNumeric());
			assertTrue(((ADQLQuery)pq).getSelect().get(4).getOperand().isString());
			assertTrue(((ADQLQuery)pq).getSelect().get(4).getOperand().isGeometry());
			// isString() = true for C3, but false for the others
			assertFalse(((ADQLQuery)pq).getSelect().get(5).getOperand().isNumeric());
			assertTrue(((ADQLQuery)pq).getSelect().get(5).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(5).getOperand().isGeometry());
			// isString() = true for C4, but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(6).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(6).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(6).getOperand().isGeometry());
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("The construction, configuration and usage of the parser are correct. Nothing should have failed here. (see console for more details)");
		}
	}
}
=======
package adql.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.FunctionDef;
import adql.query.ADQLQuery;
import adql.query.ADQLSet;

public class TestUnknownTypes {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DBType.DBDatatype.UNKNOWN.setCustomType(null);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	public void testForFctDef() {
		// Test with the return type:
		try {
			FunctionDef fct = FunctionDef.parse("foo()->aType");
			assertTrue(fct.isUnknown());
			assertFalse(fct.isString());
			assertFalse(fct.isNumeric());
			assertFalse(fct.isGeometry());
			assertEquals("?aType?", fct.returnType.type.toString());
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}

		// Test with a parameter type:
		try {
			FunctionDef fct = FunctionDef.parse("foo(param1 aType)");
			assertTrue(fct.getParam(0).type.isUnknown());
			assertFalse(fct.getParam(0).type.isString());
			assertFalse(fct.getParam(0).type.isNumeric());
			assertFalse(fct.getParam(0).type.isGeometry());
			assertEquals("?aType?", fct.getParam(0).type.toString());
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("Unknown types MUST be allowed!");
		}
	}

	@Test
	public void testForColumns() {
		final String QUERY_TXT = "SELECT FOO(C1), FOO(C2), FOO(C4), C1, C2, C3, C4 FROM T1";

		try {
			// Create the parser:
			ADQLParser parser = new ADQLParser();

			// Create table/column metadata:
			DefaultDBTable table1 = new DefaultDBTable("T1");
			table1.addColumn(new DefaultDBColumn("C1", table1));
			table1.addColumn(new DefaultDBColumn("C2", new DBType(DBDatatype.UNKNOWN), table1));
			table1.addColumn(new DefaultDBColumn("C3", new DBType(DBDatatype.VARCHAR), table1));
			table1.addColumn(new DefaultDBColumn("C4", new DBType(DBDatatype.UNKNOWN_NUMERIC), table1));
			Collection<DBTable> tList = Arrays.asList(new DBTable[]{ table1 });

			// Check the type of the column T1.C1:
			DBColumn col = table1.getColumn("C1", true);
			assertNotNull(col);
			assertNull(col.getDatatype());

			// Check the type of the column T1.C2:
			col = table1.getColumn("C2", true);
			assertNotNull(col);
			assertNotNull(col.getDatatype());
			assertTrue(col.getDatatype().isUnknown());
			assertFalse(col.getDatatype().isNumeric());
			assertFalse(col.getDatatype().isString());
			assertFalse(col.getDatatype().isGeometry());
			assertEquals("UNKNOWN", col.getDatatype().toString());

			// Check the type of the column T1.C4:
			col = table1.getColumn("C4", true);
			assertNotNull(col);
			assertNotNull(col.getDatatype());
			assertTrue(col.getDatatype().isUnknown());
			assertTrue(col.getDatatype().isNumeric());
			assertFalse(col.getDatatype().isString());
			assertFalse(col.getDatatype().isGeometry());
			assertEquals("UNKNOWN_NUMERIC", col.getDatatype().toString());

			// Define a UDF, and allow all geometrical functions and coordinate systems:
			FunctionDef udf1 = FunctionDef.parse("FOO(x INTEGER) -> INTEGER");
			parser.getSupportedFeatures().support(udf1.toLanguageFeature());
			Collection<FunctionDef> udfList = Arrays.asList(new FunctionDef[]{ udf1 });
			Collection<String> geoList = null;
			Collection<String> csList = null;

			// Create the Query checker:
			QueryChecker checker = new DBChecker(tList, udfList, geoList, csList);

			// Parse the query:
			ADQLSet pq = parser.parseQuery(QUERY_TXT);

			// Check the parsed query:
			checker.check(pq);

			/* Ensure the type of every ADQLColumn is as expected: */
			// isNumeric() = true for FOO(C1), but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(0).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(0).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(0).getOperand().isGeometry());
			// isNumeric() = true for FOO(C2), but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(1).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(1).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(1).getOperand().isGeometry());
			// isNumeric() = true for FOO(C4), but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(2).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(2).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(2).getOperand().isGeometry());
			// isNumeric() = isString() = isGeometry() for C1
			assertTrue(((ADQLQuery)pq).getSelect().get(3).getOperand().isNumeric());
			assertTrue(((ADQLQuery)pq).getSelect().get(3).getOperand().isString());
			assertTrue(((ADQLQuery)pq).getSelect().get(3).getOperand().isGeometry());
			// isNumeric() = isString() = isGeometry() for C2
			assertTrue(((ADQLQuery)pq).getSelect().get(4).getOperand().isNumeric());
			assertTrue(((ADQLQuery)pq).getSelect().get(4).getOperand().isString());
			assertTrue(((ADQLQuery)pq).getSelect().get(4).getOperand().isGeometry());
			// isString() = true for C3, but false for the others
			assertFalse(((ADQLQuery)pq).getSelect().get(5).getOperand().isNumeric());
			assertTrue(((ADQLQuery)pq).getSelect().get(5).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(5).getOperand().isGeometry());
			// isString() = true for C4, but false for the others
			assertTrue(((ADQLQuery)pq).getSelect().get(6).getOperand().isNumeric());
			assertFalse(((ADQLQuery)pq).getSelect().get(6).getOperand().isString());
			assertFalse(((ADQLQuery)pq).getSelect().get(6).getOperand().isGeometry());
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("The construction, configuration and usage of the parser are correct. Nothing should have failed here. (see console for more details)");
		}
	}
}
>>>>>>> 6430497 (Initial split into sub-projects):ADQLLib/test/adql/parser/TestUnknownTypes.java
