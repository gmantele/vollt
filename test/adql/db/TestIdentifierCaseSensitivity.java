package adql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.query.ADQLQuery;
import adql.translator.PostgreSQLTranslator;

public class TestIdentifierCaseSensitivity {

	@Test
	public void testDBTables() {
		List<DBTable> testTables = new ArrayList<DBTable>(2);
		testTables.add(new DefaultDBTable("NCS_ADQLTable", "dbTable1"));
		testTables.add(new DefaultDBTable("\"CS_ADQLTable\"", "dbTable2"));

		for(ADQLVersion adqlVersion : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(adqlVersion);
			parser.setQueryChecker(new DBChecker(testTables));

			/* CASES: NON-CASE SENSITIVE TABLE NAME.
			 *   It should match only if:
			 *     - in ANY case NOT between double quotes
			 *     - OR in LOWER case BETWEEN double quotes.  */
			try {
				assertNotNull(parser.parseQuery("SELECT * FROM ncs_adqltable"));
				assertNotNull(parser.parseQuery("SELECT * FROM NCS_ADQLTABLE"));
				assertNotNull(parser.parseQuery("SELECT * FROM NCS_ADQLTable"));
				assertNotNull(parser.parseQuery("SELECT * FROM \"ncs_adqltable\""));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("[ADQL-" + adqlVersion + "] This non-case sensitive table should have matched! (see console for more details)");
			}
			try {
				parser.parseQuery("SELECT * FROM \"NCS_ADQLTable\"");
				fail("[ADQL-" + adqlVersion + "] The table name is NOT case sensitive. This test should have failed if the table name is not fully in lowercase.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: NCS_ADQLTable [l.1 c.15 - l.1 c.30]!\n  - Unknown table \"\"NCS_ADQLTable\"\" !", ex.getMessage());
			}

			/* CASES: CASE SENSITIVE TABLE NAME.
			 *   It should match only if:
			 *     - in ANY case NOT between double quotes (if non-ambiguous name)
			 *     - OR in EXACT case BETWEEN double quotes.  */
			try {
				assertNotNull(parser.parseQuery("SELECT * FROM cs_adqltable"));
				assertNotNull(parser.parseQuery("SELECT * FROM CS_ADQLTABLE"));
				assertNotNull(parser.parseQuery("SELECT * FROM CS_ADQLTable"));
				assertNotNull(parser.parseQuery("SELECT * FROM \"CS_ADQLTable\""));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("[ADQL-" + adqlVersion + "]  This case sensitive table should have matched! (see console for more details)");
			}
			try {
				parser.parseQuery("SELECT * FROM \"cs_adqltable\"");
				fail("[ADQL-" + adqlVersion + "]  The table name is case sensitive. This test should have failed if the table name is not written with the exact case.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: cs_adqltable [l.1 c.15 - l.1 c.29]!\n  - Unknown table \"\"cs_adqltable\"\" !", ex.getMessage());
			}
		}
	}

	@Test
	public void testDBColumns() {
		DefaultDBTable testT = new DefaultDBTable("adqlTable", "dbTableName");
		testT.addColumn(new DefaultDBColumn("NCS_ADQLColumn", "dbCol1", testT));
		testT.addColumn(new DefaultDBColumn("\"CS_ADQLColumn\"", "dbCol2", testT));

		List<DBTable> testTables = new ArrayList<DBTable>(1);
		testTables.add(testT);

		for(ADQLVersion adqlVersion : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(adqlVersion);
			parser.setQueryChecker(new DBChecker(testTables));

			/* CASES: NON-CASE SENSITIVE COLUMN NAME.
			 *   It should match only if:
			 *     - in ANY case NOT between double quotes
			 *     - OR in LOWER case BETWEEN double quotes.  */
			try {
				assertNotNull(parser.parseQuery("SELECT ncs_adqlcolumn FROM adqltable"));
				assertNotNull(parser.parseQuery("SELECT NCS_ADQLCOLUMN FROM adqltable"));
				assertNotNull(parser.parseQuery("SELECT NCS_ADQLColumn FROM adqltable"));
				assertNotNull(parser.parseQuery("SELECT \"ncs_adqlcolumn\" FROM adqltable"));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("[ADQL-" + adqlVersion + "]  This non-case sensitive column should have matched! (see console for more details)");
			}
			try {
				parser.parseQuery("SELECT \"NCS_ADQLColumn\" FROM adqltable");
				fail("[ADQL-" + adqlVersion + "]  The column name is NOT case sensitive. This test should have failed if the column name is not fully in lowercase.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: NCS_ADQLColumn [l.1 c.8 - l.1 c.24]!\n  - Unknown column \"\"NCS_ADQLColumn\"\" !", ex.getMessage());
			}

			/* CASES: CASE SENSITIVE COLUMN NAME.
			 *   It should match only if:
			 *     - in ANY case NOT between double quotes (if non-ambiguous name)
			 *     - OR in EXACT case BETWEEN double quotes.  */
			try {
				assertNotNull(parser.parseQuery("SELECT cs_adqlcolumn FROM adqltable"));
				assertNotNull(parser.parseQuery("SELECT CS_ADQLCOLUMN FROM adqltable"));
				assertNotNull(parser.parseQuery("SELECT CS_ADQLColumn FROM adqltable"));
				assertNotNull(parser.parseQuery("SELECT \"CS_ADQLColumn\" FROM adqltable"));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("[ADQL-" + adqlVersion + "]  This case sensitive column should have matched! (see console for more details)");
			}
			try {
				parser.parseQuery("SELECT \"cs_adqlcolumn\" FROM adqltable");
				fail("[ADQL-" + adqlVersion + "]  The column name is case sensitive. This test should have failed if the column name is not written with the exact case.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: cs_adqlcolumn [l.1 c.8 - l.1 c.23]!\n  - Unknown column \"\"cs_adqlcolumn\"\" !", ex.getMessage());
			}
		}
	}

	@Test
	public void testFROMNames() {
		List<DBTable> testTables = new ArrayList<DBTable>();
		testTables.add(new DefaultDBTable("adqlTable", "dbTable"));

		for(ADQLVersion adqlVersion : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(adqlVersion);
			parser.setQueryChecker(new DBChecker(testTables));

			/* CASES: NON-CASE SENSITIVE TABLE NAME.
			 *   (see testDBTables())  */
			try {
				// SUB-CASE: table reference
				assertNotNull(parser.parseQuery("SELECT atable.* FROM adqltable AS aTable"));
				assertNotNull(parser.parseQuery("SELECT ATABLE.* FROM adqltable AS aTable"));
				assertNotNull(parser.parseQuery("SELECT aTable.* FROM adqltable AS aTable"));
				assertNotNull(parser.parseQuery("SELECT \"atable\".* FROM adqltable AS aTable"));
				// SUB-CASE: sub-query
				assertNotNull(parser.parseQuery("SELECT atable.* FROM (SELECT * FROM adqltable) AS aTable"));
				assertNotNull(parser.parseQuery("SELECT ATABLE.* FROM (SELECT * FROM adqltable) AS aTable"));
				assertNotNull(parser.parseQuery("SELECT aTable.* FROM (SELECT * FROM adqltable) AS aTable"));
				assertNotNull(parser.parseQuery("SELECT \"atable\".* FROM (SELECT * FROM adqltable) AS aTable"));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("[ADQL-" + adqlVersion + "]  This non-case sensitive table should have matched! (see console for more details)");
			}
			try {
				// SUB-CASE: table reference
				parser.parseQuery("SELECT \"aTable\".* FROM adqltable AS aTable");
				fail("[ADQL-" + adqlVersion + "]  The table name is NOT case sensitive. This test should have failed if the table name is not fully in lowercase.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: aTable [l.1 c.8 - l.1 c.16]!\n  - Unknown table \"\"aTable\"\" !", ex.getMessage());
			}
			try {
				// SUB-CASE: sub-query
				parser.parseQuery("SELECT \"aTable\".* FROM (SELECT * FROM adqltable) AS aTable");
				fail("[ADQL-" + adqlVersion + "]  The table name is NOT case sensitive. This test should have failed if the table name is not fully in lowercase.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: aTable [l.1 c.8 - l.1 c.16]!\n  - Unknown table \"\"aTable\"\" !", ex.getMessage());
			}

			/* CASES: CASE SENSITIVE TABLE NAME.
			 *   (see testDBTables())  */
			try {
				// SUB-CASE: table reference
				assertNotNull(parser.parseQuery("SELECT atable.* FROM adqltable AS \"aTable\""));
				assertNotNull(parser.parseQuery("SELECT ATABLE.* FROM adqltable AS \"aTable\""));
				assertNotNull(parser.parseQuery("SELECT aTable.* FROM adqltable AS \"aTable\""));
				assertNotNull(parser.parseQuery("SELECT \"aTable\".* FROM adqltable AS \"aTable\""));
				// SUB-CASE: sub-query
				assertNotNull(parser.parseQuery("SELECT atable.* FROM (SELECT * FROM adqltable) AS \"aTable\""));
				assertNotNull(parser.parseQuery("SELECT ATABLE.* FROM (SELECT * FROM adqltable) AS \"aTable\""));
				assertNotNull(parser.parseQuery("SELECT aTable.* FROM (SELECT * FROM adqltable) AS \"aTable\""));
				assertNotNull(parser.parseQuery("SELECT \"aTable\".* FROM (SELECT * FROM adqltable) AS \"aTable\""));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("[ADQL-" + adqlVersion + "]  This case sensitive table should have matched! (see console for more details)");
			}
			try {
				// SUB-CASE: table reference
				parser.parseQuery("SELECT \"atable\".* FROM adqltable AS \"aTable\"");
				fail("[ADQL-" + adqlVersion + "]  The table name is case sensitive. This test should have failed if the table name is not written with the exact case.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: atable [l.1 c.8 - l.1 c.16]!\n  - Unknown table \"\"atable\"\" !", ex.getMessage());
			}
			try {
				// SUB-CASE: sub-query
				parser.parseQuery("SELECT \"atable\".* FROM (SELECT * FROM adqltable) AS \"aTable\"");
				fail("[ADQL-" + adqlVersion + "]  The table name is case sensitive. This test should have failed if the table name is not written with the exact case.");
			} catch(Exception ex) {
				assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
				assertEquals("1 unresolved identifiers: atable [l.1 c.8 - l.1 c.16]!\n  - Unknown table \"\"atable\"\" !", ex.getMessage());
			}
		}
	}

	@Test
	public void testCTENames() {
		List<DBTable> testTables = new ArrayList<DBTable>();
		testTables.add(new DefaultDBTable("adqlTable", "dbTable"));

		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);
		parser.setQueryChecker(new DBChecker(testTables));

		/* CASES: NON-CASE SENSITIVE TABLE NAME.
		 *   (see testDBTables())  */
		try {
			assertNotNull(parser.parseQuery("WITH aTable AS (SELECT * FROM adqltable) SELECT * FROM atable"));
			assertNotNull(parser.parseQuery("WITH aTable AS (SELECT * FROM adqltable) SELECT * FROM ATABLE"));
			assertNotNull(parser.parseQuery("WITH aTable AS (SELECT * FROM adqltable) SELECT * FROM aTable"));
			assertNotNull(parser.parseQuery("WITH aTable AS (SELECT * FROM adqltable) SELECT * FROM \"atable\""));
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("This non-case sensitive table name should have matched! (see console for more details)");
		}
		try {
			parser.parseQuery("WITH aTable AS (SELECT * FROM adqltable) SELECT * FROM \"aTable\"");
			fail("The table name is NOT case sensitive. This test should have failed if the table name is not fully in lowercase.");
		} catch(Exception ex) {
			assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
			assertEquals("1 unresolved identifiers: aTable [l.1 c.56 - l.1 c.64]!\n  - Unknown table \"\"aTable\"\" !", ex.getMessage());
		}

		/* CASES: CASE SENSITIVE TABLE NAME.
		 *   (see testDBTables())  */
		try {
			assertNotNull(parser.parseQuery("WITH \"aTable\" AS (SELECT * FROM adqltable) SELECT * FROM atable"));
			assertNotNull(parser.parseQuery("WITH \"aTable\" AS (SELECT * FROM adqltable) SELECT * FROM ATABLE"));
			assertNotNull(parser.parseQuery("WITH \"aTable\" AS (SELECT * FROM adqltable) SELECT * FROM aTable"));
			assertNotNull(parser.parseQuery("WITH \"aTable\" AS (SELECT * FROM adqltable) SELECT * FROM \"aTable\""));
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("This case sensitive table should have matched! (see console for more details)");
		}
		try {
			parser.parseQuery("WITH \"aTable\" AS (SELECT * FROM adqltable) SELECT * FROM \"atable\"");
			fail("The table name is case sensitive. This test should have failed if the table name is not written with the exact case.");
		} catch(Exception ex) {
			assertEquals(UnresolvedIdentifiersException.class, ex.getClass());
			assertEquals("1 unresolved identifiers: atable [l.1 c.58 - l.1 c.66]!\n  - Unknown table \"\"atable\"\" !", ex.getMessage());
		}
	}

	@Test
	public void testTranslateDBTables() {
		List<DBTable> testTables = new ArrayList<DBTable>(4);
		testTables.add(new DefaultDBTable("Table1", "dbTable1"));
		testTables.add(new DefaultDBTable("\"Table2\"", "dbTable2"));
		testTables.add(new DefaultDBTable("Table3"));
		testTables.add(new DefaultDBTable("\"Table4\""));

		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(testTables));
			PostgreSQLTranslator trCS = new PostgreSQLTranslator(true);
			PostgreSQLTranslator trCI = new PostgreSQLTranslator(false);

			// CASE: Non-case-sensitive ADQL name, Specified DB name:
			try {
				ADQLQuery query = parser.parseQuery("SELECT * FROM table1");
				assertEquals("SELECT *\nFROM table1", query.toADQL());
				assertEquals("SELECT *\nFROM \"dbTable1\"", trCS.translate(query));
				assertEquals("SELECT *\nFROM dbTable1", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}

			// CASE: Case-sensitive ADQL name, Specified DB name:
			try {
				ADQLQuery query = parser.parseQuery("SELECT * FROM \"Table2\"");
				assertEquals("SELECT *\nFROM \"Table2\"", query.toADQL());
				assertEquals("SELECT *\nFROM \"dbTable2\"", trCS.translate(query));
				assertEquals("SELECT *\nFROM dbTable2", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}

			// CASE: Non-case-sensitive ADQL name, UNspecified DB name:
			try {
				ADQLQuery query = parser.parseQuery("SELECT * FROM table3");
				assertEquals("SELECT *\nFROM table3", query.toADQL());
				assertEquals("SELECT *\nFROM \"Table3\"", trCS.translate(query));
				assertEquals("SELECT *\nFROM Table3", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}

			// CASE: Case-sensitive ADQL name, UNspecified DB name:
			try {
				ADQLQuery query = parser.parseQuery("SELECT * FROM table4");
				assertEquals("SELECT *\nFROM table4", query.toADQL());
				assertEquals("SELECT *\nFROM \"Table4\"", trCS.translate(query));
				assertEquals("SELECT *\nFROM Table4", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}
		}
	}

	@Test
	public void testTranslateFROMNames() {
		List<DBTable> testTables = new ArrayList<DBTable>(1);
		DefaultDBTable t = new DefaultDBTable("Table1", "dbTable1");
		t.addColumn(new DefaultDBColumn("col1", t));
		testTables.add(t);

		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(testTables));
			PostgreSQLTranslator trCS = new PostgreSQLTranslator(true);
			PostgreSQLTranslator trCI = new PostgreSQLTranslator(false);

			// CASE: Non-case-sensitive lower-case name:
			try {
				ADQLQuery query = parser.parseQuery("SELECT T1.*, t1.col1 FROM table1 AS t1");
				assertEquals("SELECT T1.* , t1.col1\nFROM table1 AS t1", query.toADQL());
				assertEquals("SELECT \"t1\".\"col1\" AS \"col1\" , \"t1\".\"col1\" AS \"col1\"\nFROM \"dbTable1\" AS \"t1\"", trCS.translate(query));
				assertEquals("SELECT t1.col1 AS \"col1\" , t1.col1 AS \"col1\"\nFROM dbTable1 AS \"t1\"", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}

			// CASE: Non-case-sensitive mixed-case name:
			try {
				ADQLQuery query = parser.parseQuery("SELECT T1.*, t1.col1 FROM table1 AS T1");
				assertEquals("SELECT T1.* , t1.col1\nFROM table1 AS T1", query.toADQL());
				assertEquals("SELECT \"t1\".\"col1\" AS \"col1\" , \"t1\".\"col1\" AS \"col1\"\nFROM \"dbTable1\" AS \"t1\"", trCS.translate(query));
				assertEquals("SELECT t1.col1 AS \"col1\" , t1.col1 AS \"col1\"\nFROM dbTable1 AS \"t1\"", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}

			// CASE: Case-sensitive ADQL name:
			try {
				ADQLQuery query = parser.parseQuery("SELECT T1.*, t1.col1 FROM table1 AS \"T1\"");
				assertEquals("SELECT T1.* , t1.col1\nFROM table1 AS \"T1\"", query.toADQL());
				assertEquals("SELECT \"T1\".\"col1\" AS \"col1\" , \"T1\".\"col1\" AS \"col1\"\nFROM \"dbTable1\" AS \"T1\"", trCS.translate(query));
				assertEquals("SELECT T1.col1 AS \"col1\" , T1.col1 AS \"col1\"\nFROM dbTable1 AS \"T1\"", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}
		}
	}

	@Test
	public void testTranslateCTENames() {
		List<DBTable> testTables = new ArrayList<DBTable>(1);
		DefaultDBTable t = new DefaultDBTable("Table1", "dbTable1");
		t.addColumn(new DefaultDBColumn("col1", t));
		testTables.add(t);

		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);
		parser.setQueryChecker(new DBChecker(testTables));
		PostgreSQLTranslator trCS = new PostgreSQLTranslator(true);
		PostgreSQLTranslator trCI = new PostgreSQLTranslator(false);

		// CASE: Non-case-sensitive lower-case name:
		try {
			ADQLQuery query = parser.parseQuery("WITH t1 AS (SELECT * FROM table1) SELECT * FROM t1");
			assertEquals("WITH t1 AS (\nSELECT *\nFROM table1\n)\nSELECT *\nFROM t1", query.toADQL());
			assertEquals("WITH \"t1\"(\"col1\") AS (\nSELECT \"dbTable1\".\"col1\" AS \"col1\"\nFROM \"dbTable1\"\n)\nSELECT \"t1\".\"col1\" AS \"col1\"\nFROM \"t1\"", trCS.translate(query));
			assertEquals("WITH \"t1\"(\"col1\") AS (\nSELECT dbTable1.col1 AS \"col1\"\nFROM dbTable1\n)\nSELECT t1.col1 AS \"col1\"\nFROM t1", trCI.translate(query));
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception! (see console for more details)");
		}

		// CASE: Non-case-sensitive mixed-case name:
		try {
			ADQLQuery query = parser.parseQuery("WITH T1 AS (SELECT * FROM table1) SELECT * FROM t1");
			assertEquals("WITH T1 AS (\nSELECT *\nFROM table1\n)\nSELECT *\nFROM t1", query.toADQL());
			assertEquals("WITH \"t1\"(\"col1\") AS (\nSELECT \"dbTable1\".\"col1\" AS \"col1\"\nFROM \"dbTable1\"\n)\nSELECT \"t1\".\"col1\" AS \"col1\"\nFROM \"t1\"", trCS.translate(query));
			assertEquals("WITH \"t1\"(\"col1\") AS (\nSELECT dbTable1.col1 AS \"col1\"\nFROM dbTable1\n)\nSELECT t1.col1 AS \"col1\"\nFROM t1", trCI.translate(query));
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception! (see console for more details)");
		}

		// CASE: Case-sensitive ADQL name:
		try {
			ADQLQuery query = parser.parseQuery("WITH \"T1\" AS (SELECT * FROM table1) SELECT * FROM t1");
			assertEquals("WITH \"T1\" AS (\nSELECT *\nFROM table1\n)\nSELECT *\nFROM t1", query.toADQL());
			assertEquals("WITH \"T1\"(\"col1\") AS (\nSELECT \"dbTable1\".\"col1\" AS \"col1\"\nFROM \"dbTable1\"\n)\nSELECT \"T1\".\"col1\" AS \"col1\"\nFROM \"T1\"", trCS.translate(query));
			assertEquals("WITH \"T1\"(\"col1\") AS (\nSELECT dbTable1.col1 AS \"col1\"\nFROM dbTable1\n)\nSELECT T1.col1 AS \"col1\"\nFROM T1", trCI.translate(query));
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception! (see console for more details)");
		}
	}

	@Test
	public void testTranslateDBColumns() {
		List<DBTable> testTables = new ArrayList<DBTable>(1);
		DefaultDBTable table = new DefaultDBTable("Table1", "dbTable1");
		table.addColumn(new DefaultDBColumn("Col1", table));
		table.addColumn(new DefaultDBColumn("\"Col2\"", table));
		testTables.add(table);

		PostgreSQLTranslator trCS = new PostgreSQLTranslator(true);
		PostgreSQLTranslator trCI = new PostgreSQLTranslator(false);

		for(ADQLVersion version : ADQLVersion.values()) {
			ADQLParser parser = new ADQLParser(version);
			parser.setQueryChecker(new DBChecker(testTables));

			// CASE: Columns from database:
			try {
				ADQLQuery query = parser.parseQuery("SELECT COL1, col2, col1 AS \"SuperCol\" FROM table1");
				assertEquals("SELECT COL1 , col2 , col1 AS \"SuperCol\"\nFROM table1", query.toADQL());
				assertEquals("SELECT \"dbTable1\".\"Col1\" AS \"col1\" , \"dbTable1\".\"Col2\" AS \"Col2\" , \"dbTable1\".\"Col1\" AS \"SuperCol\"\nFROM \"dbTable1\"", trCS.translate(query));
				assertEquals("SELECT dbTable1.Col1 AS \"col1\" , dbTable1.Col2 AS \"Col2\" , dbTable1.Col1 AS \"SuperCol\"\nFROM dbTable1", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}

			// CASE: Columns from subquery
			try {
				ADQLQuery query = parser.parseQuery("SELECT * FROM (SELECT col1, col2, col1 AS Col3, col2 AS \"COL4\" FROM table1) AS table2");
				assertEquals("SELECT *\nFROM (SELECT col1 , col2 , col1 AS Col3 , col2 AS \"COL4\"\nFROM table1) AS table2", query.toADQL());
				assertEquals("SELECT \"table2\".\"Col1\" AS \"col1\" , \"table2\".\"Col2\" AS \"Col2\" , \"table2\".\"col3\" AS \"col3\" , \"table2\".\"COL4\" AS \"COL4\"\nFROM (SELECT \"dbTable1\".\"Col1\" AS \"col1\" , \"dbTable1\".\"Col2\" AS \"Col2\" , \"dbTable1\".\"Col1\" AS \"col3\" , \"dbTable1\".\"Col2\" AS \"COL4\"\nFROM \"dbTable1\") AS \"table2\"", trCS.translate(query));
				assertEquals("SELECT table2.Col1 AS \"col1\" , table2.Col2 AS \"Col2\" , table2.col3 AS \"col3\" , table2.COL4 AS \"COL4\"\nFROM (SELECT dbTable1.Col1 AS \"col1\" , dbTable1.Col2 AS \"Col2\" , dbTable1.Col1 AS \"col3\" , dbTable1.Col2 AS \"COL4\"\nFROM dbTable1) AS \"table2\"", trCI.translate(query));
			} catch(Exception ex) {
				ex.printStackTrace();
				fail("Unexpected exception! (see console for more details)");
			}

			// CASE: Columns from CTE
			if (parser.getADQLVersion() != ADQLVersion.V2_0) {
				try {
					ADQLQuery query = parser.parseQuery("WITH table3 AS (SELECT COL1, col2, col1 AS \"SuperCol\" FROM table1) SELECT * FROM table3");
					assertEquals("WITH table3 AS (\nSELECT COL1 , col2 , col1 AS \"SuperCol\"\nFROM table1\n)\nSELECT *\nFROM table3", query.toADQL());
					assertEquals("WITH \"table3\"(\"col1\",\"Col2\",\"SuperCol\") AS (\nSELECT \"dbTable1\".\"Col1\" AS \"col1\" , \"dbTable1\".\"Col2\" AS \"Col2\" , \"dbTable1\".\"Col1\" AS \"SuperCol\"\nFROM \"dbTable1\"\n)\nSELECT \"table3\".\"col1\" AS \"col1\" , \"table3\".\"Col2\" AS \"Col2\" , \"table3\".\"SuperCol\" AS \"SuperCol\"\nFROM \"table3\"", trCS.translate(query));
					assertEquals("WITH \"table3\"(\"col1\",\"Col2\",\"SuperCol\") AS (\nSELECT dbTable1.Col1 AS \"col1\" , dbTable1.Col2 AS \"Col2\" , dbTable1.Col1 AS \"SuperCol\"\nFROM dbTable1\n)\nSELECT table3.col1 AS \"col1\" , table3.Col2 AS \"Col2\" , table3.SuperCol AS \"SuperCol\"\nFROM table3", trCI.translate(query));
				} catch(Exception ex) {
					ex.printStackTrace();
					fail("Unexpected exception! (see console for more details)");
				}
			}
		}
	}

}
