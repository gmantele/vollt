package adql.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.db.DBColumn;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.grammar.ParseException;

public class TestWithItem {

	@Test
	public void testWithItemStringADQLQuery() {
		// CASE: No label => ERROR!
		String[] toTest = new String[]{ null, "", " 	 " };
		for(String label : toTest) {
			try {
				new WithItem(label, null);
				fail("It should be impossible to create a WithItem without a label!");
			} catch(Exception ex) {
				assertEquals(NullPointerException.class, ex.getClass());
				assertEquals("Missing label of the WITH item!", ex.getMessage());
			}
		}

		// CASE: No query => ERROR!
		try {
			new WithItem("query", null);
			fail("It should be impossible to create a WithItem without a query!");
		} catch(Exception ex) {
			assertEquals(NullPointerException.class, ex.getClass());
			assertEquals("Missing query of the WITH item!", ex.getMessage());
		}

		// CASE: label + query => OK!
		final String WITH_LABEL = "aNamedQuery";
		WithItem item = new WithItem(WITH_LABEL, new ADQLQuery());
		assertEquals(WITH_LABEL, item.getLabel());
		assertFalse(item.isLabelCaseSensitive());
		assertNotNull(item.getQuery());

		// CASE: label + query => OK!
		item = new WithItem("\"" + WITH_LABEL + "\"", new ADQLQuery());
		assertEquals(WITH_LABEL, item.getLabel());
		assertTrue(item.isLabelCaseSensitive());
		assertNotNull(item.getQuery());
	}

	@Test
	public void testToADQL() {
		try {
			ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

			// CASE: WITH column labels
			WithItem item = new WithItem("myQuery", parser.parseQuery("SELECT foo, stuff FROM bar"));
			assertEquals("myQuery AS (\n" + "SELECT foo , stuff\n" + "FROM bar\n" + ")", item.toADQL());

			// CASE: after an integral parsing
			ADQLQuery query = parser.parseQuery("WITH myQuery AS (SELECT foo, stuff FROM bar) SELECT * FROM myQuery");
			assertEquals("WITH myQuery AS (\n" + "SELECT foo , stuff\n" + "FROM bar\n" + ")\nSELECT *\nFROM myQuery", query.toADQL());
		} catch(ParseException ex) {
			ex.printStackTrace();
			fail("Unexpected parsing error!");
		}
	}

	@Test
	public void testGetResultingColumns() {
		try {
			ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);
			WithItem item = new WithItem("myQuery", parser.parseQuery("SELECT foo FROM bar"));
			DBColumn[] lstCol = item.getResultingColumns();
			assertNotNull(lstCol);
			assertEquals(1, lstCol.length);
			assertEquals("foo", lstCol[0].getADQLName());
			assertEquals(lstCol[0].getADQLName(), lstCol[0].getDBName());
			assertFalse(lstCol[0].isCaseSensitive());
		} catch(ParseException ex) {
			ex.printStackTrace();
			fail("Unexpected parsing error!");
		}
	}

}
