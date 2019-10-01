package tap.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import org.junit.Test;

/**
 * This class aims to ensure schema, table and column names are interpreted correctly
 * when quoted and/or qualified (with a schema or table prefix).
 */
public class TestMetadataNames {

	/** TEST SCHEMA NAME */
	@Test
	public void testSchemaName() {
		TAPSchema schema;

		// NULL
		try {
			new TAPSchema(null);
			fail("It should be impossible to create a TAPSchema with a NULL name.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty string (not a single character):
		try {
			new TAPSchema("");
			fail("It should be impossible to create a TAPSchema with an empty name.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// String with only space characters:
		try {
			new TAPSchema(" 	");
			fail("It should be impossible to create a TAPSchema with a name just composed of space characters.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty quoted string I:
		try {
			new TAPSchema("\"\"");
			fail("It should be impossible to create a TAPSchema with a empty name even if quoted.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty quoted string II:
		try {
			new TAPSchema("\" \"");
			fail("It should be impossible to create a TAPSchema with a empty name even if quoted.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Non quoted names => ADQL_NAME = RAW_NAME = TRIMMED(GIVEN_NAME)
		try {
			schema = new TAPSchema("foo");
			assertEquals("foo", schema.getADQLName());
			assertFalse(schema.isCaseSensitive());
			assertEquals("foo", schema.getRawName());
			assertEquals(schema.getRawName(), schema.toString());

			schema = new TAPSchema("	foo ");
			assertEquals("foo", schema.getADQLName());
			assertFalse(schema.isCaseSensitive());
			assertEquals("foo", schema.getRawName());
			assertEquals(schema.getRawName(), schema.toString());

			// Qualified name => Not supported as a catalog name!
			schema = new TAPSchema("myCat.foo");
			assertEquals("myCat.foo", schema.getADQLName());
			assertFalse(schema.isCaseSensitive());
			assertEquals("myCat.foo", schema.getRawName());
			assertEquals(schema.getRawName(), schema.toString());

		} catch(NullPointerException npe) {
			npe.printStackTrace(System.err);
			fail("Unexpected error! The schema name is not empty or NULL. (see console for more details)");
		}

		// Quoted names => ADQL_NAME <> RAW_NAME = TRIMMED(GIVEN_NAME)
		try {
			schema = new TAPSchema("\"foo\"");
			assertEquals("foo", schema.getADQLName());
			assertTrue(schema.isCaseSensitive());
			assertEquals("\"foo\"", schema.getRawName());
			assertEquals(schema.getRawName(), schema.toString());

			schema = new TAPSchema(" \"	foo \"	");
			assertEquals("	foo ", schema.getADQLName());
			assertTrue(schema.isCaseSensitive());
			assertEquals("\"	foo \"", schema.getRawName());
			assertEquals(schema.getRawName(), schema.toString());

			// Qualified name => Not supported as a catalog name!
			schema = new TAPSchema("myCat.\"foo\"");
			assertEquals("myCat.\"foo\"", schema.getADQLName());
			assertFalse(schema.isCaseSensitive());
			assertEquals("myCat.\"foo\"", schema.getRawName());
			assertEquals(schema.getRawName(), schema.toString());

		} catch(NullPointerException npe) {
			npe.printStackTrace(System.err);
			fail("Unexpected error! The schema name is not empty or NULL. (see console for more details)");
		}
	}

	/** TEST TABLE NAME */
	@Test
	public void testTableName() {
		TAPTable table, table2, table3;

		// NULL
		try {
			new TAPTable(null);
			fail("It should be impossible to create a TAPTable with a NULL name.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty string (not a single character):
		try {
			new TAPTable("");
			fail("It should be impossible to create a TAPTable with an empty name.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// String with only space characters:
		try {
			new TAPTable(" 	");
			fail("It should be impossible to create a TAPTable with a name just composed of space characters.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty quoted string I:
		try {
			new TAPTable("\"\"");
			fail("It should be impossible to create a TAPTable with a empty name even if quoted.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty quoted string II:
		try {
			new TAPTable("\" \"");
			fail("It should be impossible to create a TAPTable with a empty name even if quoted.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Non quoted names => ADQL_NAME = RAW_NAME = TRIMMED(GIVEN_NAME)
		try {
			table = new TAPTable("foo");
			assertEquals("foo", table.getADQLName());
			assertFalse(table.isCaseSensitive());
			assertEquals("foo", table.getRawName());

			table = new TAPTable("	foo ");
			assertEquals("foo", table.getADQLName());
			assertFalse(table.isCaseSensitive());
			assertEquals("foo", table.getRawName());

			// Qualified name => Without a schema link, a default cut is done:
			table = new TAPTable("mySchema.foo");
			assertEquals("mySchema.foo", table.getRawName());
			assertEquals(table.getRawName(), table.getADQLName());
			assertFalse(table.isCaseSensitive());

			// Qualified name + Schema with the WRONG name:
			table.setSchema(new TAPSchema("Blabla"));
			assertEquals("mySchema.foo", table.getADQLName());
			assertFalse(table.isCaseSensitive());
			assertEquals("mySchema.foo", table.getRawName());

			// Qualified name + Schema with the RIGHT name:
			table.setSchema(new TAPSchema("mySchema"));
			assertEquals("foo", table.getADQLName());
			assertFalse(table.isCaseSensitive());
			assertEquals("mySchema.foo", table.getRawName());

		} catch(NullPointerException npe) {
			npe.printStackTrace(System.err);
			fail("Unexpected error! The table name is not empty or NULL. (see console for more details)");
		}

		// Quoted names => ADQL_NAME <> RAW_NAME = TRIMMED(GIVEN_NAME)
		try {
			table = new TAPTable("\"foo\"");
			assertEquals("foo", table.getADQLName());
			assertTrue(table.isCaseSensitive());
			assertEquals("\"foo\"", table.getRawName());

			table = new TAPTable(" \"	foo \"	");
			assertEquals("	foo ", table.getADQLName());
			assertTrue(table.isCaseSensitive());
			assertEquals("\"	foo \"", table.getRawName());

			// Qualified name => a default cut is done:
			table = new TAPTable("mySchema.\"foo\"");
			assertEquals("mySchema.\"foo\"", table.getRawName());
			assertEquals(table.getRawName(), table.getADQLName());
			assertFalse(table.isCaseSensitive());
			table2 = new TAPTable(" \"mySchema\". \"foo\"");
			assertEquals("\"mySchema\". \"foo\"", table2.getRawName());
			assertEquals(table2.getRawName(), table2.getADQLName());
			assertFalse(table2.isCaseSensitive());
			table3 = new TAPTable(" \"mySchema\". foo");
			assertEquals("\"mySchema\". foo", table3.getRawName());
			assertEquals(table3.getRawName(), table3.getADQLName());
			assertFalse(table3.isCaseSensitive());

			// Qualified name + Schema with the WRONG name:
			table.setSchema(new TAPSchema("Blabla"));
			assertEquals("mySchema.\"foo\"", table.getRawName());
			assertEquals(table.getRawName(), table.getADQLName());
			assertFalse(table.isCaseSensitive());

			// Qualified name + Schema with the RIGHT name (not case sensitive):
			table.setSchema(new TAPSchema("MYSchema"));
			assertEquals("foo", table.getADQLName());
			assertTrue(table.isCaseSensitive());
			assertEquals("mySchema.\"foo\"", table.getRawName());
			table2.setSchema(new TAPSchema("MYSchema"));
			assertEquals("foo", table2.getADQLName());
			assertTrue(table2.isCaseSensitive());
			assertEquals("\"mySchema\". \"foo\"", table2.getRawName());

			// Qualified name + Schema with the RIGHT name (case sensitive):
			table3.setSchema(new TAPSchema("\"mySchema\""));
			assertEquals("foo", table3.getADQLName());
			assertFalse(table3.isCaseSensitive());
			assertEquals("\"mySchema\". foo", table3.getRawName());
			table3.setSchema(new TAPSchema("\"MYSchema\""));
			assertEquals("\"mySchema\". foo", table3.getRawName());
			assertEquals(table3.getRawName(), table3.getADQLName());
			assertFalse(table3.isCaseSensitive());

		} catch(NullPointerException npe) {
			npe.printStackTrace(System.err);
			fail("Unexpected error! The table name is not empty or NULL. (see console for more details)");
		}
	}

	/** TEST COLUMN NAME */
	@Test
	public void testColumnName() {
		TAPColumn column, column2, column3, column4, column5;

		// NULL
		try {
			new TAPColumn(null);
			fail("It should be impossible to create a TAPColumn with a NULL name.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty string (not a single character):
		try {
			new TAPColumn("");
			fail("It should be impossible to create a TAPColumn with an empty name.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// String with only space characters:
		try {
			new TAPColumn(" 	");
			fail("It should be impossible to create a TAPColumn with a name just composed of space characters.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty quoted string I:
		try {
			new TAPColumn("\"\"");
			fail("It should be impossible to create a TAPColumn with a empty name even if quoted.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Empty quoted string II:
		try {
			new TAPColumn("\" \"");
			fail("It should be impossible to create a TAPColumn with a empty name even if quoted.");
		} catch(NullPointerException npe) {
			assertEquals("Missing ADQL name!", npe.getMessage());
		}

		// Non quoted names => ADQL_NAME = RAW_NAME = TRIMMED(GIVEN_NAME)
		try {
			column = new TAPColumn("foo");
			assertEquals("foo", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());

			column = new TAPColumn("	foo ");
			assertEquals("foo", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());

			// Qualified => Not supported
			column = new TAPColumn("myTable.foo");
			assertEquals("myTable.foo", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());
			// ...even with a DB link, still not supported:
			column.setTable(new TAPTable("Blabla"));
			assertEquals("myTable.foo", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());
			column.setTable(new TAPTable("myTable"));
			assertEquals("myTable.foo", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());

		} catch(NullPointerException npe) {
			npe.printStackTrace(System.err);
			fail("Unexpected error! The column name is not empty or NULL. (see console for more details)");
		}

		// Quoted names => ADQL_NAME <> RAW_NAME = TRIMMED(GIVEN_NAME)
		try {
			column = new TAPColumn("\"foo\"");
			assertEquals("foo", column.getADQLName());
			assertTrue(column.isCaseSensitive());
			assertEquals("\"foo\"", column.getRawName());

			column = new TAPColumn(" \"	foo \"	");
			assertEquals("	foo ", column.getADQLName());
			assertTrue(column.isCaseSensitive());
			assertEquals("\"	foo \"", column.getRawName());

			// Qualified name => not supported
			column = new TAPColumn("myTable.\"foo\"");
			assertEquals("myTable.\"foo\"", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());
			column2 = new TAPColumn(" \"myTable\". \"foo\"");
			assertEquals("\"myTable\". \"foo\"", column2.getRawName());
			assertEquals(column2.getRawName(), column2.getADQLName());
			assertFalse(column2.isCaseSensitive());
			column3 = new TAPColumn(" \"myTable\". foo");
			assertEquals("\"myTable\". foo", column3.getRawName());
			assertEquals(column3.getRawName(), column3.getADQLName());
			assertFalse(column3.isCaseSensitive());
			column4 = new TAPColumn(" mySchema.\"myTable\". foo");
			assertEquals("mySchema.\"myTable\". foo", column4.getRawName());
			assertEquals(column4.getRawName(), column4.getADQLName());
			assertFalse(column4.isCaseSensitive());
			column5 = new TAPColumn(" \"mySchema\".\"myTable\". foo");
			assertEquals("\"mySchema\".\"myTable\". foo", column5.getRawName());
			assertEquals(column5.getRawName(), column5.getADQLName());
			assertFalse(column5.isCaseSensitive());

			// ...even with a DB link, still not supported:
			column.setTable(new TAPTable("Blabla"));
			assertEquals("myTable.\"foo\"", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());
			column2.setTable(new TAPTable("myTable"));
			assertEquals("\"myTable\". \"foo\"", column2.getRawName());
			assertEquals(column2.getRawName(), column2.getADQLName());
			assertFalse(column2.isCaseSensitive());
			column3.setTable(new TAPTable("myTable"));
			assertEquals("\"myTable\". foo", column3.getRawName());
			assertEquals(column3.getRawName(), column3.getADQLName());
			assertFalse(column3.isCaseSensitive());
			TAPTable t = new TAPTable("mySchema.myTable");
			t.setSchema(new TAPSchema("mySchema"));
			column4.setTable(t);
			assertEquals("mySchema.\"myTable\". foo", column4.getRawName());
			assertEquals(column4.getRawName(), column4.getADQLName());
			assertFalse(column4.isCaseSensitive());
			t = new TAPTable("\"mySchema\".myTable");
			t.setSchema(new TAPSchema("\"mySchema\""));
			column5.setTable(t);
			assertEquals("\"mySchema\".\"myTable\". foo", column5.getRawName());
			assertEquals(column5.getRawName(), column5.getADQLName());
			assertFalse(column5.isCaseSensitive());
			column.setTable(new TAPTable("myTable"));
			assertEquals("myTable.\"foo\"", column.getRawName());
			assertEquals(column.getRawName(), column.getADQLName());
			assertFalse(column.isCaseSensitive());
			column2.setTable(new TAPTable("\"myTable\""));
			assertEquals("\"myTable\". \"foo\"", column2.getRawName());
			assertEquals(column2.getRawName(), column2.getADQLName());
			assertFalse(column2.isCaseSensitive());
			column3.setTable(new TAPTable("\"myTable\""));
			assertEquals("\"myTable\". foo", column3.getRawName());
			assertEquals(column3.getRawName(), column3.getADQLName());
			assertFalse(column3.isCaseSensitive());
			t = new TAPTable("mySchema.\"myTable\"");
			t.setSchema(new TAPSchema("mySchema"));
			column4.setTable(t);
			assertEquals("mySchema.\"myTable\". foo", column4.getRawName());
			assertEquals(column4.getRawName(), column4.getADQLName());
			assertFalse(column4.isCaseSensitive());
			t = new TAPTable("\"mySchema\".\"myTable\"");
			t.setSchema(new TAPSchema("\"mySchema\""));
			column5.setTable(t);
			assertEquals("\"mySchema\".\"myTable\". foo", column5.getRawName());
			assertEquals(column5.getRawName(), column5.getADQLName());
			assertFalse(column5.isCaseSensitive());

		} catch(NullPointerException npe) {
			npe.printStackTrace(System.err);
			fail("Unexpected error! The column name is not empty or NULL. (see console for more details)");
		}
	}

	/** TEST XML METADATA */
	@Test
	public void testXMLMetadata() {
		TAPMetadata metadata = new TAPMetadata();

		TAPSchema schema = new TAPSchema("blabla");
		assertEquals("blabla", schema.getADQLName());

		TAPTable table = new TAPTable("foo");
		table.addColumn(new TAPColumn("col1"));
		table.addColumn(new TAPColumn("col2"));
		table.addColumn(new TAPColumn("foo.col3"));
		table.addColumn(new TAPColumn("blabla.foo.col4"));
		table.addColumn(new TAPColumn("foo2.col5"));
		schema.addTable(table);

		assertEquals("foo", table.getADQLName());
		Iterator<TAPColumn> itCol = table.getColumns();
		assertEquals("col1", itCol.next().getADQLName());
		assertEquals("col2", itCol.next().getADQLName());
		assertEquals("foo.col3", itCol.next().getADQLName());
		assertEquals("blabla.foo.col4", itCol.next().getADQLName());
		assertEquals("foo2.col5", itCol.next().getADQLName());

		table = new TAPTable(" \"foo.bar\"");
		table.addColumn(new TAPColumn("foo.bar.col1"));
		table.addColumn(new TAPColumn("\"foo.bar\".col2"));
		table.addColumn(new TAPColumn("blabla.foo.bar.col3"));
		table.addColumn(new TAPColumn("blabla.\"foo.bar\".col4"));
		schema.addTable(table);

		assertEquals("foo.bar", table.getADQLName());
		itCol = table.getColumns();
		assertEquals("foo.bar.col1", itCol.next().getADQLName());
		assertEquals("\"foo.bar\".col2", itCol.next().getADQLName());
		assertEquals("blabla.foo.bar.col3", itCol.next().getADQLName());
		assertEquals("blabla.\"foo.bar\".col4", itCol.next().getADQLName());
		/* Note for below:
		 * Same as for the 4th column of the table "foo". */

		metadata.addSchema(schema);

		schema = new TAPSchema("myCat.bloblo");
		assertEquals("myCat.bloblo", schema.getADQLName());

		table = new TAPTable("myCat.bloblo.bar");
		table.addColumn(new TAPColumn("col1"));
		table.addColumn(new TAPColumn("bloblo.col2"));
		table.addColumn(new TAPColumn("bar.col3"));
		table.addColumn(new TAPColumn("bloblo.bar.col4"));
		table.addColumn(new TAPColumn("myCat.bloblo.bar.col5"));
		schema.addTable(table);

		assertEquals("bar", table.getADQLName());
		itCol = table.getColumns();
		assertEquals("col1", itCol.next().getADQLName());
		assertEquals("bloblo.col2", itCol.next().getADQLName());
		assertEquals("bar.col3", itCol.next().getADQLName());
		/* Note for below:
		 * Same as for the 4th column of the table "foo". */
		assertEquals("bloblo.bar.col4", itCol.next().getADQLName());
		assertEquals("myCat.bloblo.bar.col5", itCol.next().getADQLName());

		metadata.addSchema(schema);

		schema = new TAPSchema("\"Mon Super Schema\"");
		assertEquals("Mon Super Schema", schema.getADQLName());
		metadata.addSchema(schema);

		try {
			StringWriter str = new StringWriter();
			metadata.write(new PrintWriter(str));
			//System.out.println(str.toString());
			assertEquals(expectedXMLMetadata, str.toString());
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("Unexpected error when writing TAP metadata into an XML format! (see console for more details)");
		}

	}

	public final static String expectedXMLMetadata = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<vosi:tableset xmlns:vosi=\"http://www.ivoa.net/xml/VOSITables/v1.0\" xmlns:vod=\"http://www.ivoa.net/xml/VODataService/v1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VOSITables/v1.0 http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd\">\n\t<schema>\n\t\t<name>blabla</name>\n\t\t<table>\n\t\t\t<name>foo</name>\n\t\t\t<column>\n\t\t\t\t<name>col1</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>col2</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>foo.col3</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>blabla.foo.col4</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>foo2.col5</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t</table>\n\t\t<table>\n\t\t\t<name>\"foo.bar\"</name>\n\t\t\t<column>\n\t\t\t\t<name>foo.bar.col1</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>\"foo.bar\".col2</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>blabla.foo.bar.col3</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>blabla.\"foo.bar\".col4</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t</table>\n\t</schema>\n\t<schema>\n\t\t<name>myCat.bloblo</name>\n\t\t<table>\n\t\t\t<name>myCat.bloblo.bar</name>\n\t\t\t<column>\n\t\t\t\t<name>col1</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>bloblo.col2</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>bar.col3</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>bloblo.bar.col4</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t\t<column>\n\t\t\t\t<name>myCat.bloblo.bar.col5</name>\n\t\t\t\t<dataType xsi:type=\"vod:TAPType\">UNKNOWN</dataType>\n\t\t\t</column>\n\t\t</table>\n\t</schema>\n\t<schema>\n\t\t<name>\"Mon Super Schema\"</name>\n\t</schema>\n</vosi:tableset>\n";

}
