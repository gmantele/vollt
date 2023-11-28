package adql.query.operand.function.cast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.query.operand.NumericConstant;

public class TestStandardTargetType {

	/* ****************************************
	 * CREATE A TARGET TYPE WITH NULL => Error!
	 */

	@Test(expected = NullPointerException.class)
	public void testStandardTargetTypeDBType_Null() {
		new StandardTargetType((DBType)null);
	}

	/* *******************************************
	 * CREATE A NON-STANDARD TARGET TYPE => Error!
	 */

	@Test
	public void testStandardTargetTypeDBType_NotStandard() {
		try {
			new StandardTargetType(new DBType(DBDatatype.CLOB, 128));
			fail("A non-standard type => IllegalArgumentException");
		} catch(Exception ex) {
			assertEquals(IllegalArgumentException.class, ex.getClass());
			assertEquals("Not a standard ADQL CAST's datatype: \"" + DBDatatype.CLOB + "\"!", ex.getMessage());
		}
	}

	/* *********************************************************
	 * CREATE A NON-VARIABLE-LENGTH TYPE WITH A LENGTH => Error!
	 */

	@Test
	public void testStandardTargetTypeDBType_StandardFixedWithLength() {
		try {
			new StandardTargetType(new DBType(DBDatatype.INTEGER, 12));
			fail("A non-variable-length type with a length => IllegalArgumentException");
		} catch(Exception ex) {
			assertEquals(IllegalArgumentException.class, ex.getClass());
			assertEquals("No length allowed for the datatype \"" + DBDatatype.INTEGER + "\"! It is not a variable-length datatype like CHAR, VARCHAR, ...", ex.getMessage());
		}
	}

	/* ******************************
	 * CREATE VALID TARGET TYPE => OK
	 */

	@Test
	public void testStandardTargetTypeDBType_StandardWithNoLength() {
		StandardTargetType type = new StandardTargetType(new DBType(DBDatatype.INTEGER));
		assertTrue(type.isNumeric());
		assertFalse(type.isString());
		assertFalse(type.isGeometry());
		assertNotNull(type.getReturnType());
		assertEquals(DBDatatype.INTEGER, type.getReturnType().type);
		assertEquals(DBType.NO_LENGTH, type.getReturnType().length);
		assertEquals("INTEGER", type.getName());
		assertEquals("INTEGER", type.toADQL());
		assertEquals(type.toADQL(), type.toString());
		assertEquals(0, type.getNbParameters());
		assertNotNull(type.getParameters());
		assertEquals(0, type.getParameters().length);
	}

	@Test
	public void testStandardTargetTypeDBType_VariableStandardWithNoLength() {
		StandardTargetType type = new StandardTargetType(new DBType(DBDatatype.CHAR));
		assertFalse(type.isNumeric());
		assertTrue(type.isString());
		assertFalse(type.isGeometry());
		assertNotNull(type.getReturnType());
		assertEquals(DBDatatype.CHAR, type.getReturnType().type);
		assertEquals(DBType.NO_LENGTH, type.getReturnType().length);
		assertEquals("CHAR", type.getName());
		assertEquals("CHAR", type.toADQL());
		assertEquals(type.toADQL(), type.toString());
		assertEquals(0, type.getNbParameters());
		assertNotNull(type.getParameters());
		assertEquals(0, type.getParameters().length);
	}

	@Test
	public void testStandardTargetTypeDBType_VariableStandardWithLength() {
		StandardTargetType type = new StandardTargetType(new DBType(DBDatatype.VARCHAR, 128));
		assertFalse(type.isNumeric());
		assertTrue(type.isString());
		assertFalse(type.isGeometry());
		assertNotNull(type.getReturnType());
		assertEquals(DBDatatype.VARCHAR, type.getReturnType().type);
		assertEquals(128, type.getReturnType().length);
		assertEquals("VARCHAR", type.getName());
		assertEquals("VARCHAR(128)", type.toADQL());
		assertEquals(type.toADQL(), type.toString());
		assertEquals(1, type.getNbParameters());
		assertNotNull(type.getParameters());
		assertEquals(1, type.getParameters().length);
		assertNotNull(type.getParameter(0));
		assertEquals(NumericConstant.class, type.getParameter(0).getClass());
		assertEquals(128.0, ((NumericConstant)type.getParameter(0)).getNumericValue(), 1e-10);
	}

	/* ***********************************
	 * TEST NULL DATATYPE => false
	 */

	@Test
	public void testIsStandardDatatype_Null() {
		assertFalse(StandardTargetType.isStandardDatatype(null));
	}

	@Test
	public void testIsStandardDatatype_NonStandard() {
		assertFalse(StandardTargetType.isStandardDatatype(DBDatatype.CLOB));
	}

	@Test
	public void testIsStandardDatatype_AllStandard() {
		for(DBDatatype dt : StandardTargetType.getStandardDatatypes())
			assertTrue(StandardTargetType.isStandardDatatype(dt));
	}

	/* ***********************************
	 * NORMALIZING NULL OR EMPTY => Error!
	 */

	@Test(expected = NullPointerException.class)
	public void testNormalizeDatatype_Null() {
		StandardTargetType.normalizeDatatype(null);
	}

	@Test(expected = NullPointerException.class)
	public void testNormalizeDatatype_EmptyString() {
		StandardTargetType.normalizeDatatype("");
	}

	@Test(expected = NullPointerException.class)
	public void testNormalizeDatatype_EmptyStringWithSpaces() {
		StandardTargetType.normalizeDatatype(" 	 ");
	}

	/* ***************************************************************
	 * NORMALIZATION with LEADING, TRAILING AND MIDDLE MULTIPLE SPACES
	 *  => no more leading and trailing spaces
	 *  => multiple space characters replaced by only one
	 *  => all characters in upper-case
	 */

	@Test
	public void testNormalizeDatatype() {
		assertEquals("HELLO WORLD", StandardTargetType.normalizeDatatype("  hello 	 world\r"));
	}

	/* *******************************************
	 * RESOLVE NULL OR EMPTY AS DATATYPE => Error!
	 */

	@Test(expected = NullPointerException.class)
	public void testResolveDatatype_Null() {
		StandardTargetType.resolveDatatype(null);
	}

	@Test(expected = NullPointerException.class)
	public void testResolveDatatype_Empty() {
		StandardTargetType.resolveDatatype("");
	}

	@Test(expected = NullPointerException.class)
	public void testResolveDatatype_EmptyWithSpaces() {
		StandardTargetType.resolveDatatype("  	\n ");
	}

	/* *************************************
	 * RESOLVE AN UNKNOWN DATATYPE => Error!
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDatatype_Unknown() {
		StandardTargetType.resolveDatatype("custom datatype");
	}

	/* *************************************************
	 * RESOLVE A STANDARD OR NON-STANDARD DATATYPE => OK
	 */

	@Test
	public void testResolveDatatype_Standard() {
		assertEquals(DBDatatype.VARCHAR, StandardTargetType.resolveDatatype("  	 VarChar \n "));
	}

	@Test
	public void testResolveDatatype_NonStandard() {
		assertEquals(DBDatatype.VARBINARY, StandardTargetType.resolveDatatype("	VARBinary "));
	}

	/* ******************************
	 * RESOLVE DOUBLE PRECISION => OK
	 */

	@Test
	public void testResolveDatatype_DoublePrecision() {
		assertEquals(DBDatatype.DOUBLE, StandardTargetType.resolveDatatype("  	 double   Precision \n "));
	}

}
