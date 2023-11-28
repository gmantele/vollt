package adql.query.operand.function.cast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;

public class TestCustomTargetType {

	/* *************************************************
	 * CREATE A TARGET TYPE WITH NULL OR EMPTY => Error!
	 */

	@Test(expected = NullPointerException.class)
	public void testCustomTargetTypeStringADQLOperandArray_Null() {
		new CustomTargetType(null);
	}

	@Test(expected = NullPointerException.class)
	public void testCustomTargetTypeStringADQLOperandArray_Empty() {
		new CustomTargetType("");
	}

	@Test(expected = NullPointerException.class)
	public void testCustomTargetTypeStringADQLOperandArray_EmptyWithSpaces() {
		new CustomTargetType("  	  \n ");
	}

	/* ******************************************************
	 * CREATE A TARGET TYPE WITH ONE PARAMETER NULL => Error!
	 */

	@Test(expected = NullPointerException.class)
	public void testCustomTargetTypeStringADQLOperandArray_OneParamNull() {
		new CustomTargetType("moc", new ADQLOperand[]{ new NumericConstant(1), null, new NumericConstant(2) });
	}

	/* ************************************************
	 * CREATE A TARGET TYPE WITH A KNOWN DATATYPE => OK
	 */

	@Test
	public void testCustomTargetTypeStringADQLOperandArray_KnownDatatype() {
		CustomTargetType type = new CustomTargetType("	 	Blob  ");
		assertEquals("BLOB", type.getName());
		assertEquals(0, type.getNbParameters());
		assertNotNull(type.getParameters());
		assertEquals(0, type.getParameters().length);
		assertNotNull(type.getReturnType());
		assertEquals(DBDatatype.BLOB, type.getReturnType().type);
		assertEquals(DBType.NO_LENGTH, type.getReturnType().length);
		assertEquals("BLOB", type.toADQL());
		assertEquals(type.toADQL(), type.toString());
	}

	@Test
	public void testCustomTargetTypeStringADQLOperandArray_KnownDatatypeWithParam() {
		CustomTargetType type = new CustomTargetType("	 	Blob  ", new ADQLOperand[]{ new NumericConstant(128) });
		assertEquals("BLOB", type.getName());
		assertEquals(1, type.getNbParameters());
		assertNotNull(type.getParameters());
		assertEquals(1, type.getParameters().length);
		assertNotNull(type.getParameter(0));
		assertEquals(NumericConstant.class, type.getParameter(0).getClass());
		assertEquals(128, ((NumericConstant)type.getParameter(0)).getNumericValue(), 1e-10);
		assertNotNull(type.getReturnType());
		assertEquals(DBDatatype.BLOB, type.getReturnType().type);
		assertEquals(DBType.NO_LENGTH, type.getReturnType().length);
		assertEquals("BLOB(128)", type.toADQL());
		assertEquals(type.toADQL(), type.toString());
	}

	/* ***************************************************
	 * CREATE A TARGET TYPE WITH AN UNKNOWN DATATYPE => OK
	 */

	@Test
	public void testCustomTargetTypeStringADQLOperandArray_UnknownDatatype() {
		CustomTargetType type = new CustomTargetType("	 my custom    type  ");
		assertEquals("MY CUSTOM TYPE", type.getName());
		assertEquals(0, type.getNbParameters());
		assertNotNull(type.getParameters());
		assertEquals(0, type.getParameters().length);
		assertNull(type.getReturnType());
		assertEquals("MY CUSTOM TYPE", type.toADQL());
		assertEquals(type.toADQL(), type.toString());
	}

	@Test
	public void testCustomTargetTypeStringADQLOperandArray_UnknownDatatypeWithParam() {
		CustomTargetType type = new CustomTargetType("	 my custom    type  ", new ADQLOperand[]{ new StringConstant("foo"), new NumericConstant(42) });
		assertEquals("MY CUSTOM TYPE", type.getName());
		assertEquals(2, type.getNbParameters());
		assertNotNull(type.getParameters());
		assertEquals(2, type.getParameters().length);
		assertNotNull(type.getParameter(0));
		assertEquals(StringConstant.class, type.getParameter(0).getClass());
		assertEquals("foo", ((StringConstant)type.getParameter(0)).getValue());
		assertNotNull(type.getParameter(1));
		assertEquals(NumericConstant.class, type.getParameter(1).getClass());
		assertEquals(42, ((NumericConstant)type.getParameter(1)).getNumericValue(), 1e-10);
		assertNull(type.getReturnType());
		assertEquals("MY CUSTOM TYPE('foo', 42)", type.toADQL());
		assertEquals(type.toADQL(), type.toString());
	}

}
