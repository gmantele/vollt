package tap.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringBufferInputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tap.TAPException;
import tap.metadata.TableSetParser.ForeignKey;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;

@SuppressWarnings("deprecation")
public class TableSetParserTest {

	private static TableSetParser parser = null;
	private static XMLInputFactory factory = null;

	private static final String namespaceDef = "xmlns:vs=\"http://www.ivoa.net/xml/VODataService/v1.1\" xmlns:vtm=\"http://www.ivoa.net/xml/VOSITables/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VODataService/v1.1 http://vo.ari.uni-heidelberg.de/docs/schemata/VODataService-v1.1.xsd http://www.ivoa.net/xml/VOSITables/v1.0 http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd\"";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		// Build an empty parser:
		parser = new TableSetParser();

		// Build the XML factory:
		factory = XMLInputFactory.newInstance();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{}

	@Before
	public void setUp() throws Exception{}

	@After
	public void tearDown() throws Exception{}

	private static XMLStreamReader buildReader(final String xmlContent) throws XMLStreamException{
		return factory.createXMLStreamReader(new StringBufferInputStream(xmlContent));
	}

	private static void close(final XMLStreamReader reader){
		if (reader != null){
			try{
				reader.close();
			}catch(Throwable t){}
		}
	}

	@Test
	public void testGetPosition(){
		XMLStreamReader reader = null;
		try{

			// Build a reader with an empty XML document:
			reader = buildReader("");
			assertEquals("[l.1,c.1]", parser.getPosition(reader));
			// note: reader.next() is throwing an error on an empty document => no need to test that.
			close(reader);

			// Build a reader with a simple XML:
			reader = buildReader("<A anAttr=\"attrValue\">node value</A>");

			// Position before starting reading:
			assertEquals("[l.1,c.1]", parser.getPosition(reader));

			// Position after getting the node:
			reader.next(); // START_ELEMENT("A")
			assertEquals("[l.1,c.23]", parser.getPosition(reader));
			// The position after getting an attribute should not change:
			reader.getAttributeLocalName(0); // ATTRIBUTE("attrValue")
			assertEquals("[l.1,c.23]", parser.getPosition(reader));

			// Position after getting the text:
			reader.next(); // CHARACTERS("node value")
			assertEquals("[l.1,c.35]", parser.getPosition(reader));

			// Position after getting the node ending tag:
			reader.next(); // END_ELEMENT("A")
			assertEquals("[l.1,c.37]", parser.getPosition(reader));

			// Position once the end reached:
			reader.next(); // NULL
			assertEquals("[l.-1,c.-1]", parser.getPosition(reader));

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
		}
	}

	@Test
	public void testGoToEndTag(){
		XMLStreamReader reader = null;
		try{

			/* Test with a single empty node AND WITH NULL or ""
			 * => NO TAG SHOULD HAVE BEEN READ: */
			// CASE: null
			reader = buildReader("<A></A>");
			parser.goToEndTag(reader, null);
			assertEquals("[l.1,c.1]", parser.getPosition(reader));
			close(reader);
			// CASE: empty string
			reader = buildReader("<A></A>");
			parser.goToEndTag(reader, "");
			assertEquals("[l.1,c.1]", parser.getPosition(reader));
			close(reader);

			/* Test BEFORE having read the start element:
			 * => AN EXCEPTION SHOULD BE THROWN */
			reader = buildReader("<A></A>");
			try{
				parser.goToEndTag(reader, "A");
				fail("This function should have failed: the START ELEMENT has not yet been read!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.-1,c.-1] Malformed XML document: missing an END TAG </A>!", e.getMessage());
				else
					throw e;
			}
			close(reader);

			/* Test AFTER having read the start element:
			 * => NORMAL USAGE */
			reader = buildReader("<A></A>");
			reader.next(); // START ELEMENT("A")
			parser.goToEndTag(reader, "A");
			assertEquals("[l.1,c.8]", parser.getPosition(reader));
			close(reader);

			/* Test AFTER having read the start element:
			 * => NORMAL USAGE with an embedded node */
			// search for the root node end:
			reader = buildReader("<A><B></B></A>");
			reader.next(); // START ELEMENT("A")
			parser.goToEndTag(reader, "A");
			assertEquals("[l.1,c.15]", parser.getPosition(reader));
			close(reader);
			// variant with some texts:
			reader = buildReader("<A><B>super blabla</B></A>");
			reader.next(); // START ELEMENT("A")
			parser.goToEndTag(reader, "A");
			assertEquals("[l.1,c.27]", parser.getPosition(reader));
			close(reader);
			// variant with some texts + child node:
			reader = buildReader("<A><B>super<C>blabla</C></B></A>");
			reader.next(); // START ELEMENT("A")
			parser.goToEndTag(reader, "A");
			assertEquals("[l.1,c.33]", parser.getPosition(reader));
			close(reader);
			// search for the child node end:
			reader = buildReader("<A><B></B></A>");
			reader.next(); // START ELEMENT("A")
			reader.next(); // START ELEMENT("B")
			parser.goToEndTag(reader, "B");
			assertEquals("[l.1,c.11]", parser.getPosition(reader));
			close(reader);
			// variant with some texts:
			reader = buildReader("<A><B>super blabla</B></A>");
			reader.next(); // START ELEMENT("A")
			reader.next(); // START ELEMENT("B")
			parser.goToEndTag(reader, "B");
			assertEquals("[l.1,c.23]", parser.getPosition(reader));
			close(reader);
			// variant with some texts + child node:
			reader = buildReader("<A><B>super<C>blabla</C></B></A>");
			reader.next(); // START ELEMENT("A")
			reader.next(); // START ELEMENT("B")
			parser.goToEndTag(reader, "B");
			assertEquals("[l.1,c.29]", parser.getPosition(reader));
			close(reader);

			// Test: Search the end tag while the reader is inside one of its children:
			reader = buildReader("<A><B>super<C>blabla</C></B></A>");
			reader.next(); // START ELEMENT("A")
			reader.next(); // START ELEMENT("B")
			parser.goToEndTag(reader, "A");
			assertEquals("[l.1,c.33]", parser.getPosition(reader));
			close(reader);

			// Test with a wrong start node name:
			reader = buildReader("<A></A>");
			reader.next(); // START ELEMENT("A")
			try{
				parser.goToEndTag(reader, "B");
				fail("This function should have failed: the given node name is wrong (no such node in the XML document)!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.-1,c.-1] Malformed XML document: missing an END TAG </B>!", e.getMessage());
				else
					throw e;
			}
			close(reader);

			// Test with malformed XML document:
			// CASE: missing end tag for the root node:
			reader = buildReader("<A><B></B>");
			reader.next(); // START ELEMENT("A")
			try{
				parser.goToEndTag(reader, "A");
				fail("This function should have failed: the node A has no END TAG!");
			}catch(Exception e){
				if (e instanceof XMLStreamException)
					assertEquals("ParseError at [row,col]:[1,11]\nMessage: XML document structures must start and end within the same entity.", e.getMessage());
				else
					throw e;
			}
			close(reader);
			// CASE: missing end tag for a child:
			reader = buildReader("<A><B></A>");
			reader.next(); // START ELEMENT("A")
			try{
				parser.goToEndTag(reader, "A");
				fail("This function should have failed: the node B has no END TAG!");
			}catch(Exception e){
				if (e instanceof XMLStreamException)
					assertEquals("ParseError at [row,col]:[1,9]\nMessage: The element type \"B\" must be terminated by the matching end-tag \"</B>\".", e.getMessage());
				else
					throw e;
			}
			close(reader);
			// CASE: missing end tag for the child to search:
			reader = buildReader("<A><B></A>");
			reader.next(); // START ELEMENT("A")
			reader.next(); // START ELEMENT("B")
			try{
				parser.goToEndTag(reader, "B");
				fail("This function should have failed: the node B has no END TAG!");
			}catch(Exception e){
				if (e instanceof XMLStreamException)
					assertEquals("ParseError at [row,col]:[1,9]\nMessage: The element type \"B\" must be terminated by the matching end-tag \"</B>\".", e.getMessage());
				else
					throw e;
			}
			close(reader);

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
		}
	}

	@Test
	public void testGetText(){
		XMLStreamReader reader = null;
		String txt;
		try{

			// Test with a simple XML and an empty text:
			reader = buildReader("<A></A>");
			txt = parser.getText(reader);
			assertEquals(0, txt.length());
			assertEquals("[l.1,c.4]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.START_ELEMENT, reader.getEventType());
			close(reader);
			// variant with spaces and tabs:
			reader = buildReader("  	 <A></A>");
			txt = parser.getText(reader);
			assertEquals(0, txt.length());
			assertEquals("[l.1,c.8]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.START_ELEMENT, reader.getEventType());
			close(reader);
			// variant with line returns:
			reader = buildReader("  \n <A></A>");
			txt = parser.getText(reader);
			assertEquals(0, txt.length());
			assertEquals("[l.2,c.5]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.START_ELEMENT, reader.getEventType());
			close(reader);

			// Test with a single line text:
			reader = buildReader("<A>	Super  blabla     </A>");
			reader.next(); // START ELEMENT("A")
			txt = parser.getText(reader);
			assertEquals("Super  blabla", txt);
			assertEquals("[l.1,c.27]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
			close(reader);
			// variant with CDATA:
			reader = buildReader("<A>	Super  <![CDATA[blabla   ]]>  </A>");
			reader.next(); // START ELEMENT("A")
			txt = parser.getText(reader);
			assertEquals("Super  blabla", txt);
			assertEquals("[l.1,c.39]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
			close(reader);

			// Test with a text of 2 lines:
			reader = buildReader("<A>	Super \n	 blabla     </A>");
			reader.next(); // START ELEMENT("A")
			txt = parser.getText(reader);
			assertEquals("Super\nblabla", txt);
			assertEquals("[l.2,c.18]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
			close(reader);
			// same test but with an empty line between both:
			reader = buildReader("<A>	Super \n \n	 blabla     </A>");
			reader.next(); // START ELEMENT("A")
			txt = parser.getText(reader);
			assertEquals("Super\n\nblabla", txt);
			assertEquals("[l.3,c.18]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
			close(reader);
			// same test but starting with an empty line:
			reader = buildReader("<A>\n	 Super \n	 bla  bla     </A>");
			reader.next(); // START ELEMENT("A")
			txt = parser.getText(reader);
			assertEquals("Super\nbla  bla", txt);
			assertEquals("[l.3,c.20]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
			close(reader);
			// same test but a comment splitting a text part:
			reader = buildReader("<A> Super \n	 bla<!-- a super comment -->  bla     </A>");
			reader.next(); // START ELEMENT("A")
			txt = parser.getText(reader);
			assertEquals("Super\nbla  bla", txt);
			assertEquals("[l.2,c.44]", parser.getPosition(reader));
			assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
			close(reader);

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
		}
	}

	@Test
	public void testSearchTable(){
		try{

			// Create fake metadata:
			TAPMetadata meta = new TAPMetadata();
			TAPSchema schema = new TAPSchema("SA");
			schema.addTable("TA");
			schema.addTable("TB");
			meta.addSchema(schema);
			schema = new TAPSchema("SB");
			schema.addTable("TB");
			meta.addSchema(schema);

			// Create a fake position:
			final String pos = "[l.10,c.1]";

			// Search for an existing table WITHOUT SCHEMA specification:
			TAPTable t = parser.searchTable("TA", meta, pos);
			assertEquals("TA", t.getADQLName());
			assertEquals("SA", t.getADQLSchemaName());
			// variant with a different case:
			t = parser.searchTable("ta", meta, pos);
			assertEquals("TA", t.getADQLName());
			assertEquals("SA", t.getADQLSchemaName());

			// Search for an existing table WITH SCHEMA specification:
			t = parser.searchTable("SA.TA", meta, pos);
			assertEquals("TA", t.getADQLName());
			assertEquals("SA", t.getADQLSchemaName());
			// variant with a different case:
			t = parser.searchTable("sa.ta", meta, pos);
			assertEquals("TA", t.getADQLName());
			assertEquals("SA", t.getADQLSchemaName());

			// Search with a wrong table name:
			try{
				parser.searchTable("TC", meta, pos);
				fail("This test should have not failed: there is no table named TC in the given metadata.");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals(pos + " Unknown table: \"TC\"!", e.getMessage());
				else
					throw e;
			}
			// variant with a correct schema name:
			try{
				parser.searchTable("SA.TC", meta, pos);
				fail("This test should have not failed: there is no table named SA.TC in the given metadata.");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals(pos + " Unknown table: \"SA.TC\"!", e.getMessage());
				else
					throw e;
			}

			// Search with a wrong schema name:
			try{
				parser.searchTable("SC.TB", meta, pos);
				fail("This test should have not failed: there is no schema named SC in the given metadata.");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals(pos + " Unknown table: \"SC.TB\"!", e.getMessage());
				else
					throw e;
			}

			// Search with an ambiguous table name (missing schema name):
			try{
				parser.searchTable("TB", meta, pos);
				fail("This test should have not failed: there are two table named TB ; a schema name is required to choose the table to select.");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals(pos + " Unresolved table: \"TB\"! Several tables have the same name but in different schemas (here: SA.TB, SB.TB). You must prefix this table name by a schema name (expected syntax: \"schema.table\").", e.getMessage());
				else
					throw e;
			}

			// Provide a schema + table name with a wrong syntax (missing table name or schema name):
			try{
				parser.searchTable(".TB", meta, pos);
				fail("This test should have not failed: the schema name is missing before the '.'.");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals(pos + " Incorrect full table name - \".TB\": empty schema name!", e.getMessage());
				else
					throw e;
			}
			try{
				parser.searchTable("SB.", meta, pos);
				fail("This test should have not failed: the table name is missing after the '.'.");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals(pos + " Incorrect full table name - \"SB.\": empty table name!", e.getMessage());
				else
					throw e;
			}
			try{
				parser.searchTable("toto.SB.TB", meta, pos);
				fail("This test should have not failed: the table name is missing after the '.'.");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals(pos + " Incorrect full table name - \"toto.SB.TB\": only a schema and a table name can be specified (expected syntax: \"schema.table\")\"!", e.getMessage());
				else
					throw e;
			}

		}catch(Exception e){
			e.printStackTrace();
			fail("Unexpected error: " + e.getMessage());
		}
	}

	@Test
	public void testParseFKey(){
		XMLStreamReader reader = null;
		try{

			// Test while search outside from the foreignKey node:
			reader = buildReader("<table><foreignKey><targetTable>SA.TB</targetTable><utype>truc.chose</utype><description>Foreign key\ndescription.</description><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey></table>");
			reader.next(); // START ELEMENT("table")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: the reader has not just read the \"foreignKey\" START ELEMENT tag.");
			}catch(Exception e){
				if (e instanceof IllegalStateException)
					assertEquals("[l.1,c.8] Illegal usage of TableSetParser.parseFKey(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"foreignKey\".", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}

			// Test with a complete and correct XML foreignKey node:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><utype>truc.chose</utype><description>Foreign key\ndescription.</description><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			ForeignKey fk = parser.parseFKey(reader);
			assertEquals("SA.TB", fk.targetTable);
			assertEquals("[l.1,c.45]", fk.targetTablePosition);
			assertEquals("truc.chose", fk.utype);
			assertEquals("Foreign key\ndescription.", fk.description);
			assertEquals(1, fk.keyColumns.size());
			assertEquals("col2", fk.keyColumns.get("col1"));
			close(reader);
			// variant with some comments:
			reader = buildReader("<foreignKey><!-- Here, we are inside! --><targetTable><!-- coucou -->SA.TB</targetTable><description>Foreign key\ndescription.</description><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn><!-- Here is the end! --></foreignKey><!-- Nothing more! -->");
			reader.next(); // START ELEMENT("foreignKey")
			fk = parser.parseFKey(reader);
			assertEquals("SA.TB", fk.targetTable);
			assertEquals("Foreign key\ndescription.", fk.description);
			assertEquals(1, fk.keyColumns.size());
			assertEquals("col2", fk.keyColumns.get("col1"));
			close(reader);
			// variant with texts at unapropriate places:
			reader = buildReader("<foreignKey>Here, we are <![CDATA[inside!]]><targetTable>SA.TB</targetTable><description>Foreign key\ndescription.</description><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn>Here is the end!</foreignKey>Nothing more!");
			reader.next(); // START ELEMENT("foreignKey")
			fk = parser.parseFKey(reader);
			assertEquals("SA.TB", fk.targetTable);
			assertEquals("Foreign key\ndescription.", fk.description);
			assertEquals(1, fk.keyColumns.size());
			assertEquals("col2", fk.keyColumns.get("col1"));
			close(reader);

			// Test with a missing targetTable:
			reader = buildReader("<foreignKey><description>Foreign key\ndescription.</description><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: the targetTable node is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.2,c.123] Missing \"targetTable\"!", e.getMessage());
			}finally{
				close(reader);
			}
			// variant with duplicated targetTable:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><targetTable>SA.TA</targetTable><description>Foreign key\ndescription.</description><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: the targetTable node is duplicated!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.58] Only one \"targetTable\" element can exist in a /tableset/schema/table/foreignKey!", e.getMessage());
			}finally{
				close(reader);
			}

			// Test with a missing fkColumn:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><description>Foreign key\ndescription.</description></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: at least 1 fkColumn node is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.2,c.40] Missing at least one \"fkColumn\"!", e.getMessage());
			}finally{
				close(reader);
			}
			// variant with several fkColumn:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><description>Foreign key\ndescription.</description><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn><fkColumn><fromColumn>col3</fromColumn><targetColumn>col4</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			fk = parser.parseFKey(reader);
			assertEquals("SA.TB", fk.targetTable);
			assertEquals("Foreign key\ndescription.", fk.description);
			assertEquals(2, fk.keyColumns.size());
			assertEquals("col2", fk.keyColumns.get("col1"));
			assertEquals("col4", fk.keyColumns.get("col3"));
			close(reader);

			// Test with a missing fromColumn:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><fkColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: the fromColumn node is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.99] Missing \"fromColumn\"!", e.getMessage());
			}finally{
				close(reader);
			}
			// variant with several fromColumn:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><fkColumn><fromColumn>col1</fromColumn><fromColumn>col1bis</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: sereval fromColumn are found!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.96] Only one \"fromColumn\" element can exist in a /tableset/schema/table/foreignKey/fkColumn !", e.getMessage());
			}finally{
				close(reader);
			}

			// Test with a missing targetColumn:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><fkColumn><fromColumn>col1</fromColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: the targetColumn node is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.95] Missing \"targetColumn\"!", e.getMessage());
			}finally{
				close(reader);
			}
			// variant with several fromColumn:
			reader = buildReader("<foreignKey><targetTable>SA.TB</targetTable><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn><targetColumn>col2bis</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			try{
				parser.parseFKey(reader);
				fail("This test should have failed: several targetColumn are found!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.131] Only one \"targetColumn\" element can exist in a /tableset/schema/table/foreignKey/fkColumn !", e.getMessage());
			}finally{
				close(reader);
			}

			// Test with a additional node:
			reader = buildReader("<foreignKey><super>blabla</super><foo>anything</foo><targetTable>SA.TB</targetTable><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey>");
			reader.next(); // START ELEMENT("foreignKey")
			fk = parser.parseFKey(reader);
			assertEquals("SA.TB", fk.targetTable);
			assertNull(fk.description);
			assertEquals(1, fk.keyColumns.size());
			assertEquals("col2", fk.keyColumns.get("col1"));
			close(reader);

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
		}
	}

	@Test
	public void testParseDataType(){
		XMLStreamReader reader = null;
		try{

			// Test while search outside from the dataType node:
			reader = buildReader("<column " + namespaceDef + "><dataType arraysize=\"*\">char</dataType></column>");
			reader.next(); // START ELEMENT("column")
			try{
				parser.parseDataType(reader);
				fail("This test should have failed: the reader has not just read the \"dataType\" START ELEMENT tag.");
			}catch(Exception e){
				if (e instanceof IllegalStateException)
					assertEquals("[l.1,c.408] Illegal usage of TableSetParser.parseDataType(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"dataType\".", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}

			// Test with a correct TAP type:
			reader = buildReader("<column " + namespaceDef + "><dataType xsi:type=\"vs:TAPType\">varchar</dataType></column>");
			reader.next(); // START ELEMENT("column")
			reader.next(); // START ELEMENT("dataType")
			DBType dt = parser.parseDataType(reader);
			assertEquals(DBDatatype.VARCHAR, dt.type);
			assertEquals(-1, dt.length);
			close(reader);

			// Test with a correct VOTable type:
			reader = buildReader("<dataType " + namespaceDef + " xsi:type=\"vs:VOTableType\" arraysize=\"*\">char</dataType>");
			reader.next(); // START ELEMENT("dataType")
			dt = parser.parseDataType(reader);
			assertEquals(DBDatatype.VARCHAR, dt.type);
			assertEquals(-1, dt.length);
			close(reader);

			// Test with a missing xsi:type:
			reader = buildReader("<dataType " + namespaceDef + " arraysize=\"*\">char</dataType>");
			reader.next(); // START ELEMENT("dataType")
			try{
				parser.parseDataType(reader);
				fail("This test should have failed: the attribute xsi:type is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.424] Missing attribute \"xsi:type\" (where xsi = \"" + TableSetParser.XSI_NAMESPACE + "\")! Expected attribute value: vs:VOTableType or vs:TAPType, where vs = " + TableSetParser.VODATASERVICE_NAMESPACE + ".", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}
			// variant with a wrong namespace prefix
			reader = buildReader("<dataType " + namespaceDef + " xsj:type=\"vs:VOTableType\" arraysize=\"*\">char</dataType>");
			try{
				reader.next(); // START ELEMENT("dataType")
				fail("This test should have failed: the prefix of the xsi:type attribute is wrong!");
			}catch(Exception e){
				if (e instanceof XMLStreamException)
					assertEquals("ParseError at [row,col]:[1,450]\nMessage: http://www.w3.org/TR/1999/REC-xml-names-19990114#AttributePrefixUnbound?dataType&xsj:type&xsj", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}
			// variant with a missing namespace prefix:
			reader = buildReader("<dataType xsi:type=\"vs:VOTableType\" arraysize=\"*\">char</dataType>");
			try{
				reader.next(); // START ELEMENT("dataType")
				fail("This test should have failed: the namespace xsi is not defined!");
			}catch(Exception e){
				if (e instanceof XMLStreamException)
					assertEquals("ParseError at [row,col]:[1,51]\nMessage: http://www.w3.org/TR/1999/REC-xml-names-19990114#AttributePrefixUnbound?dataType&xsi:type&xsi", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}

			// Test with an unsupported xsi:type:
			reader = buildReader("<dataType " + namespaceDef + " xsi:type=\"vs:foo\" arraysize=\"*\">char</dataType>");
			reader.next(); // START ELEMENT("dataType")
			try{
				parser.parseDataType(reader);
				fail("This test should have failed: the type foo is not defined in VODataService!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.457] Unsupported type: \"vs:foo\"! Expected: vs:VOTableType or vs:TAPType, where vs = " + TableSetParser.VODATASERVICE_NAMESPACE + ".", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}
			// variant with no namespace prefix in front of the wrong type:
			reader = buildReader("<dataType " + namespaceDef + " xsi:type=\"foo\" arraysize=\"*\">char</dataType>");
			reader.next(); // START ELEMENT("dataType")
			try{
				parser.parseDataType(reader);
				fail("This test should have failed: the namespace prefix is missing in the value of xsi:type!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.439] Unresolved type: \"foo\"! Missing namespace prefix.", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}

			// Test with a missing datatype:
			reader = buildReader("<dataType " + namespaceDef + " xsi:type=\"vs:TAPType\"></dataType>");
			reader.next(); // START ELEMENT("dataType")
			try{
				parser.parseDataType(reader);
				fail("This test should have failed: the datatype value is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.443] Missing column datatype!", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}
			// variant with a wrong datatype:
			reader = buildReader("<dataType " + namespaceDef + " xsi:type=\"vs:TAPType\">foo</dataType>");
			reader.next(); // START ELEMENT("dataType")
			try{
				parser.parseDataType(reader);
				fail("This test should have failed: the datatype value is unknown!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.446] Unknown TAPType: \"foo\"!", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
		}
	}

	@Test
	public void testParseColumn(){
		XMLStreamReader reader = null;
		try{

			// Test while search outside from the column node:
			reader = buildReader("<table " + namespaceDef + "><column std=\"true\"><name>col1</name><description>Column\ndescription.</description><dataType xsi:type=\"vs:TAPType\">SMALLINT</dataType><utype>truc.chose</utype><ucd>t.c</ucd><unit>deg</unit><flag>nullable</flag><flag>primary</flag><flag>indexed</flag></column></table>");
			reader.next(); // START ELEMENT("table")
			try{
				parser.parseColumn(reader);
				fail("This test should have failed: the reader has not just read the \"column\" START ELEMENT tag.");
			}catch(Exception e){
				if (e instanceof IllegalStateException)
					assertEquals("[l.1,c.407] Illegal usage of TableSetParser.parseColumn(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"column\".", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
			}

			// Test with a complete and correct XML column node:
			reader = buildReader("<table " + namespaceDef + "><column std=\"true\"><name>col1</name><description>Column\ndescription.</description><dataType xsi:type=\"vs:TAPType\">SMALLINT</dataType><utype>truc.chose</utype><ucd>t.c</ucd><unit>deg</unit><flag>nullable</flag><flag>primary</flag><flag>indexed</flag></column></table>");
			reader.next(); // START ELEMENT("table")
			reader.next(); // START ELEMENT("column")
			TAPColumn col = parser.parseColumn(reader);
			assertEquals("col1", col.getADQLName());
			assertEquals("Column\ndescription.", col.getDescription());
			assertEquals(DBDatatype.SMALLINT, col.getDatatype().type);
			assertEquals(-1, col.getDatatype().length);
			assertEquals("truc.chose", col.getUtype());
			assertEquals("t.c", col.getUcd());
			assertEquals("deg", col.getUnit());
			assertTrue(col.isIndexed());
			assertTrue(col.isPrincipal());
			assertTrue(col.isNullable());
			assertTrue(col.isStd());
			close(reader);
			// variant with entering inside the foreignKey node (as it is done by TableSetParser):
			reader = buildReader("<column " + namespaceDef + "><name>col1</name><description>Column\ndescription.</description><dataType xsi:type=\"vs:TAPType\">SMALLINT</dataType><utype>truc.chose</utype><ucd>t.c</ucd><unit>deg</unit></column>");
			reader.next(); // START ELEMENT("column")
			col = parser.parseColumn(reader);
			assertEquals("col1", col.getADQLName());
			assertEquals("Column\ndescription.", col.getDescription());
			assertEquals(DBDatatype.SMALLINT, col.getDatatype().type);
			assertEquals(-1, col.getDatatype().length);
			assertEquals("truc.chose", col.getUtype());
			assertEquals("t.c", col.getUcd());
			assertEquals("deg", col.getUnit());
			assertFalse(col.isIndexed());
			assertFalse(col.isPrincipal());
			assertFalse(col.isNullable());
			assertFalse(col.isStd());
			close(reader);
			// variant with some comments:
			reader = buildReader("<column " + namespaceDef + "><!-- Here we are inside! --><name>col1</name><!-- Here blabla about the column. --><description>Column\ndescription.</description><dataType xsi:type=\"vs:TAPType\">SMALLINT</dataType><utype>truc.chose</utype><ucd>t.c</ucd><unit>deg</unit><!-- Here is the end! --></column><!-- Nothing more! -->");
			reader.next(); // START ELEMENT("column")
			col = parser.parseColumn(reader);
			assertEquals("col1", col.getADQLName());
			assertEquals("Column\ndescription.", col.getDescription());
			assertEquals(DBDatatype.SMALLINT, col.getDatatype().type);
			assertEquals(-1, col.getDatatype().length);
			assertEquals("truc.chose", col.getUtype());
			assertEquals("t.c", col.getUcd());
			assertEquals("deg", col.getUnit());
			assertFalse(col.isIndexed());
			assertFalse(col.isPrincipal());
			assertFalse(col.isNullable());
			assertFalse(col.isStd());
			close(reader);
			// variant with texts at unapropriate places:
			reader = buildReader("<column " + namespaceDef + ">Here we are <![CDATA[inside!]]><name>col1</name><description>Column\ndescription.</description><dataType xsi:type=\"vs:TAPType\">SMALLINT</dataType><utype>truc.chose</utype><ucd>t.c</ucd><unit>deg</unit></column>Nothing more!");
			reader.next(); // START ELEMENT("column")
			col = parser.parseColumn(reader);
			assertEquals("col1", col.getADQLName());
			assertEquals("Column\ndescription.", col.getDescription());
			assertEquals(DBDatatype.SMALLINT, col.getDatatype().type);
			assertEquals(-1, col.getDatatype().length);
			assertEquals("truc.chose", col.getUtype());
			assertEquals("t.c", col.getUcd());
			assertEquals("deg", col.getUnit());
			assertFalse(col.isIndexed());
			assertFalse(col.isPrincipal());
			assertFalse(col.isNullable());
			assertFalse(col.isStd());
			close(reader);

			// Test with a missing "name" node:
			reader = buildReader("<column></column>");
			reader.next(); // START ELEMENT("column")
			try{
				parser.parseColumn(reader);
				fail("This test should have failed: the \"name\" node is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.18] Missing column \"name\"!", e.getMessage());
			}finally{
				close(reader);
			}
			// variant with duplicated "name":
			reader = buildReader("<column><name>col1</name><name>colA</name></column>");
			reader.next(); // START ELEMENT("column")
			try{
				parser.parseColumn(reader);
				fail("This test should have failed: the \"name\" node is duplicated!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.32] Only one \"name\" element can exist in a /tableset/schema/table/column!", e.getMessage());
			}finally{
				close(reader);
			}

			// Test with a additional node:
			reader = buildReader("<column><name>col1</name><dbname>colA</dbname><foo>blabla</foo></column>");
			reader.next(); // START ELEMENT("foreignKey")
			col = parser.parseColumn(reader);
			assertEquals("col1", col.getADQLName());
			assertNull(col.getDescription());
			assertEquals(DBDatatype.VARCHAR, col.getDatatype().type);
			assertEquals(-1, col.getDatatype().length);
			assertNull(col.getUtype());
			assertNull(col.getUcd());
			assertNull(col.getUnit());
			assertFalse(col.isIndexed());
			assertFalse(col.isPrincipal());
			assertFalse(col.isNullable());
			assertFalse(col.isStd());
			close(reader);

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
		}
	}

	@Test
	public void testParseTable(){
		XMLStreamReader reader = null;
		ArrayList<ForeignKey> fkeys = new ArrayList<ForeignKey>(0);
		try{

			// Test while search outside from the table node:
			reader = buildReader("<schema><table><name>TableA</name></table></schema>");
			reader.next(); // START ELEMENT("schema")
			try{
				parser.parseTable(reader, fkeys);
				fail("This test should have failed: the reader has not just read the \"table\" START ELEMENT tag.");
			}catch(Exception e){
				if (e instanceof IllegalStateException)
					assertEquals("[l.1,c.9] Illegal usage of TableSetParser.parseTable(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"table\".", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
				fkeys.clear();
			}

			// Test with a complete and correct XML table node:
			reader = buildReader("<table><name>TableA</name><description>Table \ndescription.</description><utype>truc.chose</utype><title>Table title</title><column><name>col1</name></column><foreignKey><targetTable>TB</targetTable><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey></table>");
			reader.next(); // START ELEMENT("table")
			TAPTable t = parser.parseTable(reader, fkeys);
			assertEquals("TableA", t.getADQLName());
			assertEquals("Table\ndescription.", t.getDescription());
			assertEquals("truc.chose", t.getUtype());
			assertEquals("Table title", t.getTitle());
			assertEquals(1, t.getNbColumns());
			assertNotNull(t.getColumn("col1"));
			assertEquals(0, t.getNbForeignKeys());
			assertEquals(1, fkeys.size());
			assertEquals("TB", fkeys.get(0).targetTable);
			assertEquals(t, fkeys.get(0).fromTable);
			close(reader);
			fkeys.clear();
			// variant with some comments:
			reader = buildReader("<table><!-- Here we are inside! --><name>TableA</name><description>Table \ndescription.</description><utype><!-- Table UType -->truc.chose</utype><title>Table title</title><column><name>col1</name></column><foreignKey><targetTable>TB</targetTable><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey></table><!-- Nothing more! -->");
			reader.next(); // START ELEMENT("table")
			t = parser.parseTable(reader, fkeys);
			assertEquals("TableA", t.getADQLName());
			assertEquals("Table\ndescription.", t.getDescription());
			assertEquals("truc.chose", t.getUtype());
			assertEquals("Table title", t.getTitle());
			assertEquals(1, t.getNbColumns());
			assertNotNull(t.getColumn("col1"));
			assertEquals(0, t.getNbForeignKeys());
			assertEquals(1, fkeys.size());
			assertEquals("TB", fkeys.get(0).targetTable);
			assertEquals(t, fkeys.get(0).fromTable);
			close(reader);
			fkeys.clear();
			// variant with texts at unapropriate places:
			reader = buildReader("<table>Here we are <![CDATA[inside!]]><name>TableA</name><description>Table \ndescription.</description><utype><!-- Table UType -->truc.chose</utype><title>Table title</title><column><name>col1</name></column><foreignKey><targetTable>TB</targetTable><fkColumn><fromColumn>col1</fromColumn><targetColumn>col2</targetColumn></fkColumn></foreignKey></table>Nothing more!");
			reader.next(); // START ELEMENT("table")
			t = parser.parseTable(reader, fkeys);
			assertEquals("TableA", t.getADQLName());
			assertEquals("Table\ndescription.", t.getDescription());
			assertEquals("truc.chose", t.getUtype());
			assertEquals("Table title", t.getTitle());
			assertEquals(1, t.getNbColumns());
			assertNotNull(t.getColumn("col1"));
			assertEquals(0, t.getNbForeignKeys());
			assertEquals(1, fkeys.size());
			assertEquals("TB", fkeys.get(0).targetTable);
			assertEquals(t, fkeys.get(0).fromTable);
			close(reader);
			fkeys.clear();

			// Test with a missing "name" node:
			reader = buildReader("<table></table>");
			reader.next(); // START ELEMENT("table")
			try{
				parser.parseTable(reader, fkeys);
				fail("This test should have failed: the \"name\" node is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.16] Missing table \"name\"!", e.getMessage());
			}finally{
				close(reader);
				fkeys.clear();
			}
			// variant with duplicated "name":
			reader = buildReader("<table><name>Table1</name><name>TableA</name></table>");
			reader.next(); // START ELEMENT("table")
			try{
				parser.parseTable(reader, fkeys);
				fail("This test should have failed: the \"name\" node is duplicated!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.33] Only one \"name\" element can exist in a /tableset/schema/table!", e.getMessage());
			}finally{
				close(reader);
				fkeys.clear();
			}

			// Test with a additional node:
			reader = buildReader("<table><name>TableA</name><dbname>SuperTableA</dbname><foo>blabla</foo></table>");
			reader.next(); // START ELEMENT("table")
			t = parser.parseTable(reader, fkeys);
			assertEquals("TableA", t.getADQLName());
			assertNull(t.getDescription());
			assertNull(t.getUtype());
			assertNull(t.getTitle());
			assertEquals(0, t.getNbColumns());
			assertEquals(0, t.getNbForeignKeys());
			assertEquals(0, fkeys.size());
			close(reader);
			fkeys.clear();

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
			fkeys.clear();
		}
	}

	@Test
	public void testParseSchema(){
		XMLStreamReader reader = null;
		ArrayList<ForeignKey> fkeys = new ArrayList<ForeignKey>(0);
		try{

			// Test while search outside from the schema node:
			reader = buildReader("<tableset><schema><name>PublicSchema</name></schema></tableset>");
			reader.next(); // START ELEMENT("tableset")
			try{
				parser.parseSchema(reader, fkeys);
				fail("This test should have failed: the reader has not just read the \"schema\" START ELEMENT tag.");
			}catch(Exception e){
				if (e instanceof IllegalStateException)
					assertEquals("[l.1,c.11] Illegal usage of TableSetParser.parseSchema(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"schema\".", e.getMessage());
				else
					throw e;
			}finally{
				close(reader);
				fkeys.clear();
			}

			// Test with a complete and correct XML table node:
			reader = buildReader("<schema><name>PublicSchema</name><description>Schema \ndescription.</description><utype>truc.chose</utype><title>Schema title</title><table><name>TableA</name></table></schema>");
			reader.next(); // START ELEMENT("schema")
			TAPSchema s = parser.parseSchema(reader, fkeys);
			assertEquals("PublicSchema", s.getADQLName());
			assertEquals("Schema\ndescription.", s.getDescription());
			assertEquals("truc.chose", s.getUtype());
			assertEquals("Schema title", s.getTitle());
			assertEquals(1, s.getNbTables());
			assertNotNull(s.getTable("TableA"));
			close(reader);
			fkeys.clear();
			// variant with some comments:
			reader = buildReader("<schema><!-- Here we are inside! --><name>PublicSchema <!-- schema name --></name><description>Schema \ndescription.</description><utype>truc.chose</utype><title>Schema title</title><table><name>TableA</name></table></schema><!-- Nothing more! -->");
			reader.next(); // START ELEMENT("schema")
			s = parser.parseSchema(reader, fkeys);
			assertEquals("PublicSchema", s.getADQLName());
			assertEquals("Schema\ndescription.", s.getDescription());
			assertEquals("truc.chose", s.getUtype());
			assertEquals("Schema title", s.getTitle());
			assertEquals(1, s.getNbTables());
			assertNotNull(s.getTable("TableA"));
			close(reader);
			fkeys.clear();
			// variant with texts at unapropriate places:
			reader = buildReader("<schema>Here we are <![CDATA[inside!]]><name>PublicSchema <!-- schema name --></name><description>Schema \ndescription.</description><utype>truc.chose</utype><title>Schema title</title><table><name>TableA</name></table></schema>Nothing more!");
			reader.next(); // START ELEMENT("schema")
			s = parser.parseSchema(reader, fkeys);
			assertEquals("PublicSchema", s.getADQLName());
			assertEquals("Schema\ndescription.", s.getDescription());
			assertEquals("truc.chose", s.getUtype());
			assertEquals("Schema title", s.getTitle());
			assertEquals(1, s.getNbTables());
			assertNotNull(s.getTable("TableA"));
			close(reader);
			fkeys.clear();

			// Test with a missing "name" node:
			reader = buildReader("<schema></schema>");
			reader.next(); // START ELEMENT("schema")
			try{
				parser.parseSchema(reader, fkeys);
				fail("This test should have failed: the \"name\" node is missing!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.18] Missing schema \"name\"!", e.getMessage());
			}finally{
				close(reader);
				fkeys.clear();
			}
			// variant with duplicated "name":
			reader = buildReader("<schema><name>PublicSchema</name><name>PrivateSchema</name></schema>");
			reader.next(); // START ELEMENT("schema")
			try{
				parser.parseSchema(reader, fkeys);
				fail("This test should have failed: the \"name\" node is duplicated!");
			}catch(Exception e){
				if (e instanceof TAPException)
					assertEquals("[l.1,c.40] Only one \"name\" element can exist in a /tableset/schema!", e.getMessage());
			}finally{
				close(reader);
				fkeys.clear();
			}

			// Test with a additional node:
			reader = buildReader("<schema><name>PublicSchema</name><dbname>public</dbname><foo>blabla</foo></schema>");
			reader.next(); // START ELEMENT("schema")
			s = parser.parseSchema(reader, fkeys);
			assertEquals("PublicSchema", s.getADQLName());
			assertNull(s.getDescription());
			assertNull(s.getUtype());
			assertNull(s.getTitle());
			assertEquals(0, s.getNbTables());
			close(reader);
			fkeys.clear();

		}catch(Exception e){
			e.printStackTrace();
			if (e instanceof XMLStreamException)
				fail("Unexpected error while reading the XML content: " + e.getMessage());
			else
				fail("Unexpected error: " + e.getMessage());
		}finally{
			close(reader);
			fkeys.clear();
		}
	}

}
