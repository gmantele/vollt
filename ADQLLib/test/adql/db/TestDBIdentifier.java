package adql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TestDBIdentifier {

	@Test
	public void testIsDelimited() {
		// CASE: All correctly delimited names
		assertTrue(DBIdentifier.isDelimited("\"\""));
		assertTrue(DBIdentifier.isDelimited("\" \""));
		assertTrue(DBIdentifier.isDelimited("\"a\""));
		assertTrue(DBIdentifier.isDelimited("\"\"\"\""));
		assertTrue(DBIdentifier.isDelimited("\"foo.bar\""));
		assertTrue(DBIdentifier.isDelimited("\"foo\"\".\"\"bar\""));

		// CASE: NOT delimited names
		assertFalse(DBIdentifier.isDelimited(null));
		assertFalse(DBIdentifier.isDelimited(""));
		assertFalse(DBIdentifier.isDelimited("foo"));
		assertFalse(DBIdentifier.isDelimited("\"foo"));
		assertFalse(DBIdentifier.isDelimited("foo\""));
		assertFalse(DBIdentifier.isDelimited("\"foo\".\"bar\""));
	}

	@Test
	public void testNormalize() {
		// CASE: NULL, empty string, delimited empty string => NULL
		for(String str : new String[]{ null, "", "  ", "  \t \r  \n ", "\"\"", "  \"\" ", "\" \t \n \r  \"" })
			assertNull(DBIdentifier.normalize(str));

		// CASE: Non-delimited string => same, just trimmed
		assertEquals("IDent", DBIdentifier.normalize(" \t IDent  \n"));
		assertEquals("ID\"ent\"", DBIdentifier.normalize(" \t ID\"ent\"  \n"));
		assertEquals("\" ID\"ent\" \"", DBIdentifier.normalize("\" ID\"ent\" \""));

		// CASE: Delimited string => remove double quotes
		assertEquals("IDent", DBIdentifier.normalize("\"IDent\""));
		assertEquals(" IDent ", DBIdentifier.normalize(" \t \" IDent \" \n"));
		assertEquals(" ID\"ent\" ", DBIdentifier.normalize("\" ID\"\"ent\"\" \""));
	}

	@Test
	public void testDenormalize() {
		// CASE: NULL => NULL
		assertNull(DBIdentifier.denormalize(null, true));
		assertNull(DBIdentifier.denormalize(null, false));

		// CASE: Non-case-sensitive string => exactly same as provided
		assertEquals(" \t IDent  \n", DBIdentifier.denormalize(" \t IDent  \n", false));

		// CASE: Case-sensitive string => surrounded by double quotes
		assertEquals("\" ID\"\"ent\"\"\"", DBIdentifier.denormalize(" ID\"ent\"", true));
	}

	@Test
	public void testSetADQLName() {
		DBIdentifier dbid = new DBIdentifier4Test("foo");
		assertEquals("foo", dbid.adqlName);
		assertFalse(dbid.adqlCaseSensitive);
		assertNull(dbid.dbName);

		// CASE: missing ADQL name => NullPointerException
		for(String str : new String[]{ null, "", "  ", "  \t \r  \n ", "\"\"", "  \"\" ", "\" \t \n \r  \"" }) {
			try {
				dbid.setADQLName(str);
				fail("Setting a NULL or empty ADQL name should have failed with a NullPointerException!");
			} catch(Exception ex) {
				assertEquals(NullPointerException.class, ex.getClass());
				assertEquals("Missing ADQL name!", ex.getMessage());
			}
		}

		// CASE: Non-delimited ADQL name
		dbid.setADQLName("Ident");
		assertEquals("Ident", dbid.getADQLName());
		assertFalse(dbid.isCaseSensitive());
		assertNull(dbid.dbName);
		assertEquals(dbid.getADQLName(), dbid.getDBName());

		// CASE: Delimited ADQL name
		dbid.setADQLName("\"Ident\"");
		assertEquals("Ident", dbid.getADQLName());
		assertTrue(dbid.isCaseSensitive());
		assertNull(dbid.dbName);
		assertEquals(dbid.getADQLName(), dbid.getDBName());
	}

	@Test
	public void testSetDBName() {
		DBIdentifier dbid = new DBIdentifier4Test("foo", "dbFoo");
		assertEquals("foo", dbid.adqlName);
		assertFalse(dbid.adqlCaseSensitive);
		assertEquals("dbFoo", dbid.dbName);

		// CASE: missing ADQL name => NullPointerException
		for(String str : new String[]{ null, "", "  ", "  \t \r  \n ", "\"\"", "  \"\" ", "\" \t \n \r  \"" }) {
			dbid.setDBName(str);
			assertNull(dbid.dbName);
			assertEquals(dbid.getADQLName(), dbid.getDBName());
		}

		// CASE: Non-delimited DB name
		dbid.setDBName("Ident");
		assertEquals("Ident", dbid.dbName);
		assertEquals(dbid.dbName, dbid.getDBName());

		// CASE: Delimited DB name
		dbid.setDBName("\"Ident\"");
		assertEquals("Ident", dbid.getDBName());
		assertEquals("Ident", dbid.dbName);
		assertEquals(dbid.dbName, dbid.getDBName());
	}

	@Test
	public void testSetCaseSensitive() {
		DBIdentifier dbid = new DBIdentifier4Test("foo", "dbFoo");
		assertFalse(dbid.isCaseSensitive());

		// CASE: set case-sensitive
		dbid.setCaseSensitive(true);
		assertTrue(dbid.isCaseSensitive());

		// CASE: set INcase-sensitive
		dbid.setCaseSensitive(false);
		assertFalse(dbid.isCaseSensitive());

	}

	private final static class DBIdentifier4Test extends DBIdentifier {

		public DBIdentifier4Test(String adqlName, String dbName) throws NullPointerException {
			super(adqlName, dbName);
		}

		public DBIdentifier4Test(String adqlName) throws NullPointerException {
			super(adqlName);
		}

	}

}
