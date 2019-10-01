package adql.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import adql.db.DBColumn;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.grammar.ParseException;
import adql.query.operand.ADQLColumn;

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
		assertNull(item.getColumnLabels());

		// CASE: label + query => OK!
		item = new WithItem("\"" + WITH_LABEL + "\"", new ADQLQuery());
		assertEquals(WITH_LABEL, item.getLabel());
		assertTrue(item.isLabelCaseSensitive());
		assertNotNull(item.getQuery());
		assertNull(item.getColumnLabels());
	}

	@Test
	public void testWithItemStringADQLQueryCollectionOfIdentifierItem() {

		// CASE: No label => ERROR!
		String[] toTest = new String[]{ null, "", " 	 " };
		for(String label : toTest) {
			try {
				new WithItem(label, null, null);
				fail("It should be impossible to create a WithItem without a label!");
			} catch(Exception ex) {
				assertEquals(NullPointerException.class, ex.getClass());
				assertEquals("Missing label of the WITH item!", ex.getMessage());
			}
		}

		// CASE: No query => ERROR!
		try {
			new WithItem("query", null, null);
			fail("It should be impossible to create a WithItem without a query!");
		} catch(Exception ex) {
			assertEquals(NullPointerException.class, ex.getClass());
			assertEquals("Missing query of the WITH item!", ex.getMessage());
		}

		// CASE: label + query but no col. label => OK!
		final String WITH_LABEL = "aNamedQuery";
		WithItem item = new WithItem(WITH_LABEL, new ADQLQuery(), null);
		assertEquals(WITH_LABEL, item.getLabel());
		assertNotNull(item.getQuery());
		assertNull(item.getColumnLabels());

		// CASE: label + query + col. labels => OK!
		item = new WithItem(WITH_LABEL, new ADQLQuery(), Arrays.asList(new ADQLColumn[]{ new ADQLColumn("aColumn") }));
		assertEquals(WITH_LABEL, item.getLabel());
		assertNotNull(item.getQuery());
		assertNotNull(item.getColumnLabels());
		assertEquals(1, item.getColumnLabels().size());
	}

	@Test
	public void testToADQL() {
		try {
			ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

			// CASE: NO column labels
			WithItem item = new WithItem("myQuery", parser.parseQuery("SELECT foo FROM bar"));
			assertEquals("myQuery AS (\n" + "SELECT foo\n" + "FROM bar\n" + ")", item.toADQL());

			// CASE: WITH column labels
			item = new WithItem("myQuery", parser.parseQuery("SELECT foo, stuff FROM bar"), Arrays.asList(new ADQLColumn[]{ new ADQLColumn("aColumn"), new ADQLColumn("\"Another\"Column\"") }));
			assertEquals("myQuery(aColumn,\"Another\"\"Column\") AS (\n" + "SELECT foo , stuff\n" + "FROM bar\n" + ")", item.toADQL());

			// CASE: after an integral parsing
			ADQLQuery query = parser.parseQuery("WITH myQuery(aColumn, \"Another\"\"Column\") AS (SELECT foo, stuff FROM bar) SELECT * FROM myQuery");
			assertEquals("WITH myQuery(aColumn,\"Another\"\"Column\") AS (\n" + "SELECT foo , stuff\n" + "FROM bar\n" + ")\nSELECT *\nFROM myQuery", query.toADQL());
		} catch(ParseException ex) {
			ex.printStackTrace();
			fail("Unexpected parsing error!");
		}
	}

	@Test
	public void testGetResultingColumns() {
		try {
			ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

			// CASE: NO column labels
			WithItem item = new WithItem("myQuery", parser.parseQuery("SELECT foo FROM bar"));
			DBColumn[] lstCol = item.getResultingColumns();
			assertNotNull(lstCol);
			assertEquals(1, lstCol.length);
			assertEquals("foo", lstCol[0].getADQLName());
			assertEquals(lstCol[0].getADQLName(), lstCol[0].getDBName());
			assertFalse(lstCol[0].isCaseSensitive());

			// CASE: WITH column labels
			item = new WithItem("myQuery", parser.parseQuery("SELECT foo, stuff FROM bar"), Arrays.asList(new ADQLColumn[]{ new ADQLColumn("aColumn"), new ADQLColumn("\"Another\"Column\"") }));
			assertEquals("myQuery(aColumn,\"Another\"\"Column\") AS (\n" + "SELECT foo , stuff\n" + "FROM bar\n" + ")", item.toADQL());
			lstCol = item.getResultingColumns();
			assertNotNull(lstCol);
			assertEquals(2, lstCol.length);
			assertEquals("acolumn", lstCol[0].getADQLName());
			assertEquals(lstCol[0].getADQLName(), lstCol[0].getDBName());
			assertFalse(lstCol[0].isCaseSensitive());
			assertEquals("Another\"Column", lstCol[1].getADQLName());
			assertEquals(lstCol[1].getADQLName(), lstCol[1].getDBName());
			assertTrue(lstCol[1].isCaseSensitive());
		} catch(ParseException ex) {
			ex.printStackTrace();
			fail("Unexpected parsing error!");
		}
	}

}
