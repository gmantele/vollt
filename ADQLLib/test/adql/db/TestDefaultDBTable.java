package adql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDefaultDBTable {

	@Test
	public void testDefaultDBTableStringString() {
		// CASE: No DN name, no case sensitivity
		DefaultDBTable table = new DefaultDBTable("adqlName");
		assertEquals("adqlName", table.getADQLName());
		assertEquals(table.getADQLName(), table.getDBName());
		assertFalse(table.isCaseSensitive());

		// CASE: No DB name, case sensitivity
		table = new DefaultDBTable("\"adqlName\"");
		assertEquals("adqlName", table.getADQLName());
		assertEquals(table.getADQLName(), table.getDBName());
		assertTrue(table.isCaseSensitive());

		// CASE: DB name, no case sensitivity
		table = new DefaultDBTable("adqlName", "dbName");
		assertEquals("adqlName", table.getADQLName());
		assertEquals("dbName", table.getDBName());
		assertFalse(table.isCaseSensitive());

		// CASE: DB name, case sensitivity
		table = new DefaultDBTable("\"adqlName\"", "dbName");
		assertEquals("adqlName", table.getADQLName());
		assertEquals("dbName", table.getDBName());
		assertTrue(table.isCaseSensitive());
	}

	@Test
	public void testSetADQLName() {

		// CASE: no case sensitivity, no DB name
		DefaultDBTable table = new DefaultDBTable("adqlName");
		assertEquals("adqlName", table.getADQLName());
		assertEquals(table.getADQLName(), table.getDBName());
		assertFalse(table.isCaseSensitive());

		// CASE: undelimited name => OK
		table.setADQLName("myTable");
		assertEquals("myTable", table.getADQLName());
		assertFalse(table.isCaseSensitive());

		// CASE: No DB name => use the ADQLName
		table.setDBName(null);
		assertEquals(table.getADQLName(), table.getDBName());
		assertFalse(table.isCaseSensitive());

		// CASE: delimited name => stored undelimited
		table.setADQLName("\"MyTable\"");
		assertEquals("MyTable", table.getADQLName());
		assertTrue(table.isCaseSensitive());

		// CASE: Empty string => use the ADQLName (as name=NULL)
		table.setDBName("");
		assertEquals(table.getADQLName(), table.getDBName());
		assertTrue(table.isCaseSensitive());

		// CASE: adqlName delimited and no DB name => dbName = undelimited adqlName
		table = new DefaultDBTable("\"ADQLName\"");
		table.setDBName(null);
		assertEquals("ADQLName", table.getDBName());
		assertTrue(table.isCaseSensitive());

		// CASE: missing DB name => ERROR!
		for(String n : new String[]{ null, "", " 	 ", "\"\"", "\"  \"" }) {
			try {
				new DefaultDBTable(n);
			} catch(Exception ex) {
				assertEquals(NullPointerException.class, ex.getClass());
				assertEquals("Missing ADQL name!", ex.getMessage());
			}
		}
	}

}
