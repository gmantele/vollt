package adql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDefaultDBTable {

	@Test
	public void testIsDelimited() {
		// CASE: All correctly delimited names
		assertTrue(DefaultDBTable.isDelimited("\"\""));
		assertTrue(DefaultDBTable.isDelimited("\" \""));
		assertTrue(DefaultDBTable.isDelimited("\"a\""));
		assertTrue(DefaultDBTable.isDelimited("\"\"\"\""));
		assertTrue(DefaultDBTable.isDelimited("\"foo.bar\""));
		assertTrue(DefaultDBTable.isDelimited("\"foo\"\".\"\"bar\""));

		// CASE: NOT delimited names
		assertFalse(DefaultDBTable.isDelimited(null));
		assertFalse(DefaultDBTable.isDelimited(""));
		assertFalse(DefaultDBTable.isDelimited("foo"));
		assertFalse(DefaultDBTable.isDelimited("\"foo"));
		assertFalse(DefaultDBTable.isDelimited("foo\""));
		assertFalse(DefaultDBTable.isDelimited("\"foo\".\"bar\""));
	}

	@Test
	public void testSetADQLName() {

		DefaultDBTable table = new DefaultDBTable("dbName");
		assertEquals(table.getDBName(), table.getADQLName());
		assertFalse(table.isCaseSensitive());

		// CASE: undelimited name => OK
		table.setADQLName("myTable");
		assertEquals("myTable", table.getADQLName());
		assertFalse(table.isCaseSensitive());

		// CASE: No name => use the DBName
		table.setADQLName(null);
		assertEquals(table.getDBName(), table.getADQLName());
		assertFalse(table.isCaseSensitive());

		// CASE: delimited name => stored undelimited
		table.setADQLName("\"MyTable\"");
		assertEquals("MyTable", table.getADQLName());
		assertTrue(table.isCaseSensitive());

		// CASE: Empty string => use the DBName (as name=NULL)
		table.setADQLName("");
		assertEquals(table.getDBName(), table.getADQLName());
		assertFalse(table.isCaseSensitive());

		// CASE: dbName delimited and no ADQL name => adqlName = undelimited dbName
		table = new DefaultDBTable("\"DBName\"");
		table.setADQLName(null);
		assertEquals("DBName", table.getADQLName());
		assertTrue(table.isCaseSensitive());

		// CASE: dbName delimited but empty and no ADQL name => adqlName = delimited dbName
		table = new DefaultDBTable("\"  \"");
		table.setADQLName(null);
		assertEquals(table.getDBName(), table.getADQLName());
		assertFalse(table.isCaseSensitive());
	}

}
