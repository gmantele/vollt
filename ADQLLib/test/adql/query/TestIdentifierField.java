package adql.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestIdentifierField {

	@Test
	public void testIsCaseSensitive(){
		byte b = 0x00;
		assertFalse(IdentifierField.SCHEMA.isCaseSensitive(b));
		b = IdentifierField.SCHEMA.setCaseSensitive(b, true);
		assertTrue(IdentifierField.SCHEMA.isCaseSensitive(b));
	}

	/*@Test
	public void testSetCaseSensitive(){
		fail("Not yet implemented");
	}*/

}
