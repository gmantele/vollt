package adql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDefaultDBColumn {

	@Test
	public void testDefaultDBColumnStringString() {
		DefaultDBTable table = new DefaultDBTable("table");

		// CASE: No DB name, no case sensitivity
		DefaultDBColumn col = new DefaultDBColumn("adqlName", table);
		assertEquals("adqlName", col.getADQLName());
		assertEquals(col.getADQLName(), col.getDBName());
		assertFalse(col.isCaseSensitive());

		// CASE: No DB name, case sensitivity
		col = new DefaultDBColumn("\"adqlName\"", table);
		assertEquals("adqlName", col.getADQLName());
		assertEquals(col.getADQLName(), col.getDBName());
		assertTrue(col.isCaseSensitive());

		// CASE: DB name, no case sensitivity
		col = new DefaultDBColumn("adqlName", "dbName", table);
		assertEquals("adqlName", col.getADQLName());
		assertEquals("dbName", col.getDBName());
		assertFalse(col.isCaseSensitive());

		// CASE: DN name, case sensitivity
		col = new DefaultDBColumn("\"adqlName\"", "dbName", table);
		assertEquals("adqlName", col.getADQLName());
		assertEquals("dbName", col.getDBName());
		assertTrue(col.isCaseSensitive());
	}

	@Test
	public void testSetADQLName() {
		DefaultDBTable table = new DefaultDBTable("table");

		// CASE: no case sensitivity, no DB name
		DefaultDBColumn col = new DefaultDBColumn("adqlName", table);
		assertEquals("adqlName", col.getADQLName());
		assertEquals(col.getADQLName(), col.getDBName());
		assertFalse(col.isCaseSensitive());

		// CASE: undelimited name => OK
		col.setADQLName("myColumn");
		assertEquals("myColumn", col.getADQLName());
		assertFalse(col.isCaseSensitive());

		// CASE: delimited name => stored undelimited
		col.setADQLName("\"MyColumn\"");
		assertEquals("MyColumn", col.getADQLName());
		assertTrue(col.isCaseSensitive());

		// CASE: missing DB name => ERROR!
		for(String n : new String[]{ null, "", " 	 ", "\"\"", "\"  \"" }) {
			try {
				new DefaultDBColumn(n, table);
			} catch(Exception ex) {
				assertEquals(NullPointerException.class, ex.getClass());
				assertEquals("Missing ADQL name!", ex.getMessage());
			}
		}
	}

}
