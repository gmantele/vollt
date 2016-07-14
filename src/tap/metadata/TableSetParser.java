package tap.metadata;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2015-2016 - Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.helpers.DefaultHandler;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import tap.TAPException;
import tap.data.VOTableIterator;
import tap.metadata.TAPTable.TableType;

/**
 * <p>Let parse an XML document representing a table set, and return the corresponding {@link TAPMetadata} instance.</p>
 * 
 * <p><i>Note 1: the table set must follow the syntax specified by the XML Schema http://www.ivoa.net/xml/VODataService.</i></p>
 * <p><i>Note 2: only tags specified by VODataService are checked. If there is any other tag, they are merely ignored.</i></p>
 * 
 * <h3>Exceptions</h3>
 * 
 * <p>A {@link TAPException} is thrown in the following cases:</p>
 * <ul>
 * 	<li>the root node is not "tableset"</li>
 * 	<li>table name syntax ([schema.]table) is incorrect</li>
 * 	<li>a single table name (just "table" without schema prefix) is ambiguous (that's to say, the same name is used for tables of different schemas)</li>
 * 	<li>"name" node is missing in nodes "schema", "table" and "column"</li>
 * 	<li>"targetTable" is missing in node "foreignKey"</li>
 * 	<li>"fromColumn" or "targetColumn" is missing in node "fkColumn"</li>
 * 	<li>"name" node is duplicated in the same node</li>
 * 	<li>missing "xsi:type" as attribute in a "dataType" node</li>
 * 	<li>unknown column datatype</li>
 * </ul>
 * 
 * <p><i>Note: catalog prefixes are not supported in this parser.</i></p>
 * 
 * <h3>Datatype</h3>
 * 
 * <p>
 * 	A column datatype may be specified either as a TAP or a VOTable datatype. Thus, the type of specification must be given with the attribute xsi:type of the
 * 	node "dataType". For instance:
 * </p>
 * <ul>
 * 	<li><code>&lt;dataType xsi:type="vs:VOTableType" arraysize="1"&gt;float&lt;/dataType&gt;</code> for a VOTable datatype</li>
 * 	<li><code>&lt;dataType xsi:type="vod:TAPType"&gt;VARCHAR&lt;/dataType&gt;</code> for a TAP datatype</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.1 (07/2016)
 * @since 2.0
 */
public class TableSetParser extends DefaultHandler {

	/** XML namespace for the XML schema XMLSchema-instance. */
	protected final static String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

	/** XML namespace for the XML schema VODataService. */
	protected final static String VODATASERVICE_NAMESPACE = "http://www.ivoa.net/xml/VODataService";

	/**
	 * <p>Intermediary representation of a Foreign Key.</p>
	 * 
	 * <p>
	 * 	An instance of this class lets save all information provided in the XML document and needed to create the corresponding TAP metadata ({@link TAPForeignKey})
	 * 	at the end of XML document parsing, once all available tables are listed.
	 * </p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 2.0 (02/2015)
	 * @since 2.0
	 * 
	 * @see TableSetParser#parseFKey(XMLStreamReader)
	 * @see TableSetParser#parse(InputStream)
	 */
	protected static class ForeignKey {
		/** Foreign key description */
		public String description = null;
		/** UType associated with this foreign key. */
		public String utype = null;
		/** Source table of the foreign key.
		 * <i>Note: In the XML document, the foreign key is described inside its table ;
		 * hence the type of this attribute: TAPTable (it is indeed already known).</i> */
		public TAPTable fromTable = null;
		/** Target table of the foreign key. */
		public String targetTable = null;
		/** Position of the "targetTable" node inside the XML document.
		 * <i>Note: this attribute may be used only in case of error.</i> */
		public String targetTablePosition = "";
		/** Columns associations.
		 * Keys are columns of the source table, whereas values are columns of the target table to associate with. */
		public Map<String,String> keyColumns = new HashMap<String,String>();
	}

	/**
	 * Parse the XML TableSet stored in the specified file.
	 * 
	 * @param file	The regular file containing the TableSet to parse.
	 * 
	 * @return	The corresponding TAP metadata.
	 * 
	 * @throws IOException	If any error occurs while reading the given file.
	 * @throws TAPException	If any error occurs in the XML parsing or in the TAP metadata creation.
	 * 
	 * @since {@link #parse(InputStream)}
	 */
	public TAPMetadata parse(final File file) throws IOException, TAPException{
		InputStream input = null;
		try{
			input = new BufferedInputStream(new FileInputStream(file));
			return parse(input);
		}finally{
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe2){}
			}
		}
	}

	/**
	 * Parse the XML TableSet stored in the given stream.
	 * 
	 * @param input	The stream containing the TableSet to parse.
	 * 
	 * @return	The corresponding TAP metadata.
	 * 
	 * @throws IOException	If any error occurs while reading the given stream.
	 * @throws TAPException	If any error occurs in the XML parsing or in the TAP metadata creation.
	 * 
	 * @see #parseSchema(XMLStreamReader, List)
	 */
	public TAPMetadata parse(final InputStream input) throws IOException, TAPException{
		TAPMetadata meta = null;

		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader reader = null;

		try{
			// Create the XML streaming reader:
			reader = factory.createXMLStreamReader(input);

			// Read the first XML tag => MUST BE <tableset> :
			int event = nextTag(reader);
			if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equalsIgnoreCase("tableset")){

				// Build the metadata object:
				meta = new TAPMetadata();

				// Prepare the listing of all foreign keys for a later resolution:
				ArrayList<ForeignKey> allForeignKeys = new ArrayList<ForeignKey>(20);

				// Read the next XML tag => MUST BE <schema> :
				while(reader.hasNext() && (event = nextTag(reader)) == XMLStreamConstants.START_ELEMENT){
					if (reader.getLocalName().equalsIgnoreCase("schema")){
						// fetch the schema description and content:
						meta.addSchema(parseSchema(reader, allForeignKeys));
					}
				}

				// Read the final XML tag => MUST BE </tableset> :
				if (event != XMLStreamConstants.END_ELEMENT || !reader.getLocalName().equalsIgnoreCase("tableset")){
					// throw an error if the tag is not the expected one:
					throw new TAPException(getPosition(reader) + " XML tag mismatch: <" + (event == XMLStreamConstants.END_ELEMENT ? "/" : "") + reader.getLocalName() + ">! Expected: </tableset>.");
				}

				// Resolve all ForeignKey objects into TAPForeignKeys and add them into the dedicated TAPTable:
				long keyId = 0;
				for(ForeignKey fk : allForeignKeys){
					// search for the target table:
					TAPTable targetTable = searchTable(fk.targetTable, meta, fk.targetTablePosition);
					// build and add the foreign key:
					fk.fromTable.addForeignKey("" + (++keyId), targetTable, fk.keyColumns, fk.description, fk.utype);
				}

			}else
				throw new TAPException(getPosition(reader) + " Missing root tag: \"tableset\"!");

		}catch(XMLStreamException xse){
			throw new TAPException(getPosition(reader) + " XML ERROR: " + xse.getMessage() + "!", xse);
		}

		return meta;
	}

	/* **************************** */
	/* INDIVIDUAL PARSING FUNCTIONS */
	/* **************************** */

	/**
	 * <p>Parse the XML representation of a TAP schema.</p>
	 * 
	 * <p><b>Important: This function MUST be called just after the start element "schema" has been read!</b></p>
	 * 
	 * <h3>Attributes</h3>
	 * 
	 * <p>No attribute is expected in the start element "schema".</p>
	 * 
	 * <h3>Children</h3>
	 * 
	 * Only the following nodes are taken into account ; the others are ignored:
	 * <ul>
	 * 	<li>name <i>REQUIRED</i></li>
	 * 	<li>description <i>{0..1}</i></li>
	 * 	<li>title <i>{0..1}</i></li>
	 * 	<li>utype <i>{0..1}</i></li>
	 * 	<li>table <i>{*}</i></li>
	 * </ul>
	 * 
	 * @param reader					XML reader.
	 * @param allForeignKeys			List to fill with all encountered foreign keys.
	 *                      			<i>note: these keys are not the final TAP meta, but a collection of all information found in the XML document.
	 *                      			The final TAP meta will be created later, once all available tables and columns are available.</i>
	 * @throws IllegalStateException	If this function is called while the reader has not just read the START ELEMENT tag of "table".
	 * 
	 * @return	The corresponding TAP schema.
	 * 
	 * @throws XMLStreamException	If there is an error processing the underlying XML source.
	 * @throws TAPException			If several "name" nodes are found, or if none such node is found ; exactly one "name" node must be found.
	 * 
	 * @see #parseTable(XMLStreamReader, List)
	 */
	protected TAPSchema parseSchema(final XMLStreamReader reader, final List<ForeignKey> allForeignKeys) throws XMLStreamException, TAPException{
		// Ensure the reader has just read the START ELEMENT of schema:
		if (reader.getEventType() != XMLStreamConstants.START_ELEMENT || reader.getLocalName() == null || !reader.getLocalName().equalsIgnoreCase("schema"))
			throw new IllegalStateException(getPosition(reader) + " Illegal usage of TableSetParser.parseSchema(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"schema\".");

		TAPSchema schema = null;
		String tag = null, name = null, description = null, title = null,
				utype = null;
		ArrayList<TAPTable> tables = new ArrayList<TAPTable>(10);

		while(nextTag(reader) == XMLStreamConstants.START_ELEMENT){
			// Get the tag name:
			tag = reader.getLocalName();

			// Identify the current tag:
			if (tag.equalsIgnoreCase("name")){
				if (name != null)
					throw new TAPException(getPosition(reader) + " Only one \"name\" element can exist in a /tableset/schema!");
				name = getText(reader);
			}else if (tag.equalsIgnoreCase("description"))
				description = ((description != null) ? (description + "\n") : "") + getText(reader);
			else if (tag.equalsIgnoreCase("table")){
				ArrayList<ForeignKey> keys = new ArrayList<ForeignKey>(2);
				TAPTable newTable = parseTable(reader, keys);
				newTable.setIndex(tables.size());
				tables.add(newTable);
				allForeignKeys.addAll(keys);
			}else if (tag.equalsIgnoreCase("title"))
				title = ((title != null) ? (title + "\n") : "") + getText(reader);
			else if (tag.equalsIgnoreCase("utype"))
				utype = getText(reader);
		}

		// Only one info is required: the schema name!
		if (name == null)
			throw new TAPException(getPosition(reader) + " Missing schema \"name\"!");

		// Build the schema:
		schema = new TAPSchema(name, description, utype);
		schema.setTitle(title);
		for(TAPTable t : tables)
			schema.addTable(t);
		tables = null;

		return schema;
	}

	/**
	 * <p>Parse the XML representation of a TAP table.</p>
	 * 
	 * <p><b>Important: This function MUST be called just after the start element "table" has been read!</b></p>
	 * 
	 * <h3>Attributes</h3>
	 * 
	 * The attribute "type" may be provided in the start element "table". One of the following value is expected:
	 * <ul>
	 * 	<li>base_table <i>or table</i></li>
	 * 	<li>output</li>
	 * 	<li>view</li>
	 * </ul>
	 * 
	 * <h3>Children</h3>
	 * 
	 * Only the following nodes are taken into account ; the others are ignored:
	 * <ul>
	 * 	<li>name <i>REQUIRED</i></li>
	 * 	<li>description <i>{0..1}</i></li>
	 * 	<li>title <i>{0..1}</i></li>
	 * 	<li>utype <i>{0..1}</i></li>
	 * 	<li>column <i>{*}</i></li>
	 * 	<li>foreignKey <i>{*}</i></li>
	 * </ul>
	 * 
	 * @param reader			XML reader.
	 * @param keys				List to fill with all encountered foreign keys.
	 *                      	<i>note: these keys are not the final TAP meta, but a collection of all information found in the XML document.
	 *                      	The final TAP meta will be created later, once all available tables and columns are available.</i>
	 * 
	 * @return	The corresponding TAP table.
	 * 
	 * @throws XMLStreamException		If there is an error processing the underlying XML source.
	 * @throws TAPException				If several "name" nodes are found, or if none such node is found ; exactly one "name" node must be found.
	 * @throws IllegalStateException	If this function is called while the reader has not just read the START ELEMENT tag of "table".
	 * 
	 * @see #parseColumn(XMLStreamReader)
	 * @see #parseFKey(XMLStreamReader)
	 */
	protected TAPTable parseTable(final XMLStreamReader reader, final List<ForeignKey> keys) throws XMLStreamException, TAPException{
		// Ensure the reader has just read the START ELEMENT of table:
		if (reader.getEventType() != XMLStreamConstants.START_ELEMENT || reader.getLocalName() == null || !reader.getLocalName().equalsIgnoreCase("table"))
			throw new IllegalStateException(getPosition(reader) + " Illegal usage of TableSetParser.parseTable(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"table\".");

		TAPTable table = null;
		TableType type = TableType.table;
		String tag = null, name = null, description = null, title = null,
				utype = null;
		ArrayList<TAPColumn> columns = new ArrayList<TAPColumn>(10);

		// Get the table type (attribute "type") [OPTIONAL] :
		if (reader.getAttributeCount() > 0){
			int indType = 0;
			while(indType < reader.getAttributeCount() && !reader.getAttributeLocalName(indType).equalsIgnoreCase("type"))
				indType++;
			if (indType < reader.getAttributeCount() && reader.getAttributeLocalName(indType).equalsIgnoreCase("type")){
				String typeTxt = reader.getAttributeValue(indType);
				if (typeTxt != null && typeTxt.trim().length() > 0){
					typeTxt = typeTxt.trim().toLowerCase();
					try{
						if (typeTxt.equals("base_table"))
							type = TableType.table;
						else
							type = TableType.valueOf(typeTxt);
					}catch(IllegalArgumentException iae){
						/* Note: If type unknown, the given value is ignored and the default type - TableType.table - is kept. */
					}
				}
			}
		}

		// Fetch the other information (tags):
		while(nextTag(reader) == XMLStreamConstants.START_ELEMENT){
			// Get the tag name:
			tag = reader.getLocalName();

			// Identify the current tag:
			if (tag.equalsIgnoreCase("name")){
				if (name != null)
					throw new TAPException(getPosition(reader) + " Only one \"name\" element can exist in a /tableset/schema/table!");
				name = getText(reader);
			}else if (tag.equalsIgnoreCase("description"))
				description = ((description != null) ? (description + "\n") : "") + getText(reader);
			else if (tag.equalsIgnoreCase("column")){
				TAPColumn newCol = parseColumn(reader);
				newCol.setIndex(columns.size());
				columns.add(newCol);
			}else if (tag.equalsIgnoreCase("foreignKey"))
				keys.add(parseFKey(reader));
			else if (tag.equalsIgnoreCase("title"))
				title = ((title != null) ? (title + "\n") : "") + getText(reader);
			else if (tag.equalsIgnoreCase("utype"))
				utype = getText(reader);
		}

		// Only one info is required: the table name!
		if (name == null)
			throw new TAPException(getPosition(reader) + " Missing table \"name\"!");

		// Build the table:
		table = new TAPTable(name, type, description, utype);
		table.setTitle(title);
		for(TAPColumn c : columns)
			table.addColumn(c);
		for(ForeignKey k : keys)
			k.fromTable = table;

		return table;
	}

	/**
	 * <p>Parse the XML representation of a TAP column.</p>
	 * 
	 * <p><b>Important: This function MUST be called just after the start element "column" has been read!</b></p>
	 * 
	 * <h3>Attributes</h3>
	 * 
	 * The attribute "std" may be provided in the start element "column". One of the following value is expected:
	 * <ul>
	 * 	<li>false <i>(default value if the attribute is omitted)</i></li>
	 * 	<li>true</li>
	 * </ul>
	 * 
	 * <h3>Children</h3>
	 * 
	 * Only the following nodes are taken into account ; the others are ignored:
	 * <ul>
	 * 	<li>name <i>REQUIRED</i></li>
	 * 	<li>description <i>{0..1}</i></li>
	 * 	<li>ucd <i>{0..1}</i></li>
	 * 	<li>unit <i>{0..1}</i></li>
	 * 	<li>utype <i>{0..1}</i></li>
	 * 	<li>dataType <i>{0..1}</i></li>
	 * 	<li>flag <i>{*}, but only the values 'nullable', 'indexed' and 'primary' are currently supported by the library)</i></li>
	 * </ul>
	 * 
	 * @param reader	XML reader.
	 * 
	 * @return	The corresponding TAP column.
	 * 
	 * @throws XMLStreamException		If there is an error processing the underlying XML source.
	 * @throws TAPException				If several "name" nodes are found, or if none such node is found ; exactly one "name" node must be found.
	 * @throws IllegalStateException	If this function is called while the reader has not just read the START ELEMENT tag of "column".
	 * 
	 * @see #parseDataType(XMLStreamReader)
	 */
	protected TAPColumn parseColumn(final XMLStreamReader reader) throws XMLStreamException, TAPException{
		// Ensure the reader has just read the START ELEMENT of column:
		if (reader.getEventType() != XMLStreamConstants.START_ELEMENT || reader.getLocalName() == null || !reader.getLocalName().equalsIgnoreCase("column"))
			throw new IllegalStateException(getPosition(reader) + " Illegal usage of TableSetParser.parseColumn(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"column\".");

		TAPColumn column = null;
		boolean std = false, indexed = false, primary = false, nullable = false;
		String tag = null, name = null, description = null, unit = null,
				ucd = null, utype = null;
		DBType type = null;

		// Get the column STD flag (attribute "std") [OPTIONAL] :
		if (reader.getAttributeCount() > 0){
			int indType = 0;
			while(indType < reader.getAttributeCount() && !reader.getAttributeLocalName(indType).equalsIgnoreCase("std"))
				indType++;
			if (indType < reader.getAttributeCount() && reader.getAttributeLocalName(indType).equalsIgnoreCase("std")){
				String stdTxt = reader.getAttributeValue(indType);
				if (stdTxt != null)
					std = Boolean.parseBoolean(stdTxt.trim().toLowerCase());
			}
		}

		// Fetch the other information (tags):
		while(nextTag(reader) == XMLStreamConstants.START_ELEMENT){
			// Get the tag name:
			tag = reader.getLocalName();

			// Identify the current tag:
			if (tag.equalsIgnoreCase("name")){
				if (name != null)
					throw new TAPException(getPosition(reader) + " Only one \"name\" element can exist in a /tableset/schema/table/column!");
				name = getText(reader);
			}else if (tag.equalsIgnoreCase("description"))
				description = ((description != null) ? (description + "\n") : "") + getText(reader);
			else if (tag.equalsIgnoreCase("dataType"))
				type = parseDataType(reader);
			else if (tag.equalsIgnoreCase("unit"))
				unit = getText(reader);
			else if (tag.equalsIgnoreCase("ucd"))
				ucd = getText(reader);
			else if (tag.equalsIgnoreCase("utype"))
				utype = getText(reader);
			else if (tag.equalsIgnoreCase("flag")){
				String txt = getText(reader);
				if (txt != null){
					if (txt.equalsIgnoreCase("indexed"))
						indexed = true;
					else if (txt.equalsIgnoreCase("primary"))
						primary = true;
					else if (txt.equalsIgnoreCase("nullable"))
						nullable = true;
				}
			}
		}

		// Only one info is required: the table name!
		if (name == null)
			throw new TAPException(getPosition(reader) + " Missing column \"name\"!");

		// Build the column:
		column = new TAPColumn(name, type, description, unit, ucd, utype);
		column.setStd(std);
		column.setIndexed(indexed);
		column.setPrincipal(primary);
		column.setNullable(nullable);

		return column;
	}

	/**
	 * <p>Parse the XML representation of a column datatype.</p>
	 * 
	 * <p><b>Important: This function MUST be called just after the start element "dataType" has been read!</b></p>
	 * 
	 * <h3>Attributes</h3>
	 * 
	 * The attribute "xsi:type" (where xsi = http://www.w3.org/2001/XMLSchema-instance) MUST be provided. Only the following values are supported and accepted
	 * <i>(below, vs = http://www.ivoa.net/xml/VODataService)</i>:
	 * <ul>
	 * 	<li><b>vs:VOTableType</b>, and the following attributes may be also provided:
	 * 		<ul>
	 * 			<li>arraysize</li>
	 * 			<li>xtype</li>
	 * 		</ul></li>
	 * 	<li><b>vs:TAPType</b>, and the attribute "size" may be also provided</li>
	 * </ul>
	 * 
	 * <h3>Children</h3>
	 * 
	 * No child, but a text MUST be provided. Its value depends of the attribute "xsi:type": a VOTable datatype (e.g. char, float, short) if "xsi:type=vs:VOTableType",
	 * or a TAP type (e.g. VARCHAR, REAL, SMALLINT) if "xsi:type=vs:TAPType". <i>Any other value will be rejected.</i>
	 * 
	 * <p><i>IMPORTANT: All VOTable datatypes will be converted into TAPType automatically by the library.</i></p>
	 * 
	 * @param reader	XML reader.
	 * 
	 * @return	The corresponding column datatype.
	 * 
	 * @throws XMLStreamException		If there is an error processing the underlying XML source.
	 * @throws TAPException				If the attribute "xsi:type" is missing or incorrect,
	 *                     				or if the datatype is unknown or not supported.
	 * @throws IllegalStateException	If this function is called while the reader has not just read the START ELEMENT tag of "dataType".
	 * 
	 * @see VOTableIterator#resolveVotType(String, String, String)
	 * @see DBType#DBType(DBDatatype, int)
	 */
	protected DBType parseDataType(final XMLStreamReader reader) throws XMLStreamException, TAPException{
		// Ensure the reader has just read the START ELEMENT of dataType:
		if (reader.getEventType() != XMLStreamConstants.START_ELEMENT || reader.getLocalName() == null || !reader.getLocalName().equalsIgnoreCase("dataType"))
			throw new IllegalStateException(getPosition(reader) + " Illegal usage of TableSetParser.parseDataType(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"dataType\".");

		String typeOfType = null, datatype = null, size = null, xtype = null,
				arraysize = null;

		/* Note:
		 * The 1st parameter of XMLStreamReader.getAttributeValue(String, String) should be the namespace of the attribute.
		 * If this value is NULL, the namespace condition is ignored.
		 * If it is an empty string - "" - an attribute without namespace will be searched. */

		// Get the type of datatype :
		typeOfType = reader.getAttributeValue(XSI_NAMESPACE, "type");

		// Resolve the datatype:
		if (typeOfType == null || typeOfType.trim().length() == 0)
			throw new TAPException(getPosition(reader) + " Missing attribute \"xsi:type\" (where xsi = \"" + XSI_NAMESPACE + "\")! Expected attribute value: vs:VOTableType or vs:TAPType, where vs = " + VODATASERVICE_NAMESPACE + ".");

		// Separate the namespace and type parts:
		String[] split = typeOfType.split(":");

		// Ensure the number of parts is 2:
		if (split.length != 2)
			throw new TAPException(getPosition(reader) + " Unresolved type: \"" + typeOfType + "\"! Missing namespace prefix.");
		// ...and ensure the namespace is the expected value:
		else{
			String datatypeNamespace = reader.getNamespaceURI(split[0]);
			if (datatypeNamespace == null)
				throw new TAPException(getPosition(reader) + " Unresolved type: \"" + typeOfType + "\"! Unknown namespace.");
			else if (!datatypeNamespace.startsWith(VODATASERVICE_NAMESPACE))
				throw new TAPException(getPosition(reader) + " Unsupported type: \"" + typeOfType + "\"! Expected: vs:VOTableType or vs:TAPType, where vs = " + VODATASERVICE_NAMESPACE + ".");
		}

		// Get the other attributes:
		size = reader.getAttributeValue("", "size");
		xtype = reader.getAttributeValue("", "xtype");
		arraysize = reader.getAttributeValue("", "arraysize");

		// Get the datatype:
		datatype = getText(reader);
		if (datatype == null || datatype.trim().length() == 0)
			throw new TAPException(getPosition(reader) + " Missing column datatype!");
		datatype = datatype.trim();

		// Resolve the datatype in function of the value of xsi:type:
		// CASE: VOTable
		if (split[1].equalsIgnoreCase("VOTableType"))
			return VOTableIterator.resolveVotType(datatype, arraysize, xtype).toTAPType();

		// CASE: TAP type
		else if (split[1].equalsIgnoreCase("TAPType")){
			// normalize the size attribute:
			int colSize = -1;
			if (size != null && size.trim().length() > 0){
				try{
					colSize = Integer.parseInt(size);
				}catch(NumberFormatException nfe){}
			}
			// build and return the corresponding type:
			try{
				return new DBType(DBDatatype.valueOf(datatype.toUpperCase()), colSize);
			}catch(IllegalArgumentException iae){
				throw new TAPException(getPosition(reader) + " Unknown TAPType: \"" + datatype + "\"!");
			}
		}
		// DEFAULT => Throw an exception!
		else
			throw new TAPException(getPosition(reader) + " Unsupported type: \"" + typeOfType + "\"! Expected: vs:VOTableType or vs:TAPType, where vs = " + VODATASERVICE_NAMESPACE + ".");
	}

	/**
	 * <p>Parse the XML representation of a TAP foreign key.</p>
	 * 
	 * <p><b>Important: This function MUST be called just after the start element "foreignKey" has been read!</b></p>
	 * 
	 * <h3>Attributes</h3>
	 * 
	 * <p>No attribute is expected in the start element "foreignKey".</p>
	 * 
	 * <h3>Children</h3>
	 * 
	 * Only the following nodes are taken into account ; the others are ignored:
	 * <ul>
	 * 	<li>targetTable <i>REQUIRED</i></li>
	 * 	<li>description <i>{0..1}</i></li>
	 * 	<li>utype <i>{0..1}</i></li>
	 * 	<li>fkColumn <i>{1..*}</i>
	 * 		<ul>
	 * 			<li>fromColumn <i>REQUIRED</i></li>
	 * 			<li>targetColumn <i>REQUIRED</i></li>
	 * 		</ul></li>
	 * </ul>
	 * 
	 * @param reader	XML reader.
	 * 
	 * @return	An object containing all information found in the XML node about the foreign key.
	 * 
	 * @throws XMLStreamException		If there is an error processing the underlying XML source.
	 * @throws TAPException				If "targetTable" node is missing,
	 *                     				or if no "fkColumn" is provided.
	 * @throws IllegalStateException	If this function is called while the reader has not just read the START ELEMENT tag of "foreignKey".
	 * 
	 * @see #parseDataType(XMLStreamReader)
	 */
	protected ForeignKey parseFKey(final XMLStreamReader reader) throws XMLStreamException, TAPException{
		// Ensure the reader has just read the START ELEMENT of foreignKey:
		if (reader.getEventType() != XMLStreamConstants.START_ELEMENT || reader.getLocalName() == null || !reader.getLocalName().equalsIgnoreCase("foreignKey"))
			throw new IllegalStateException(getPosition(reader) + " Illegal usage of TableSetParser.parseFKey(XMLStreamParser)! This function can be called only when the reader has just read the START ELEMENT tag \"foreignKey\".");

		String tag;
		ForeignKey fk = new ForeignKey();

		// Fetch the other information (tags):
		while(nextTag(reader) == XMLStreamConstants.START_ELEMENT){
			// Get the tag name:
			tag = reader.getLocalName();

			// Identify the current tag:
			if (tag.equalsIgnoreCase("targetTable")){
				if (fk.targetTable != null)
					throw new TAPException(getPosition(reader) + " Only one \"targetTable\" element can exist in a /tableset/schema/table/foreignKey!");
				fk.targetTable = getText(reader);
				fk.targetTablePosition = getPosition(reader);
			}else if (tag.equalsIgnoreCase("description"))
				fk.description = getText(reader);
			else if (tag.equalsIgnoreCase("utype"))
				fk.utype = getText(reader);
			else if (tag.equalsIgnoreCase("fkColumn")){
				String innerTag, fromCol = null, targetCol = null;
				while(nextTag(reader) == XMLStreamConstants.START_ELEMENT){
					innerTag = reader.getLocalName();
					if (innerTag.equalsIgnoreCase("fromColumn")){
						if (fromCol != null)
							throw new TAPException(getPosition(reader) + " Only one \"fromColumn\" element can exist in a /tableset/schema/table/foreignKey/fkColumn !");
						fromCol = getText(reader);
					}else if (innerTag.equalsIgnoreCase("targetColumn")){
						if (targetCol != null)
							throw new TAPException(getPosition(reader) + " Only one \"targetColumn\" element can exist in a /tableset/schema/table/foreignKey/fkColumn !");
						targetCol = getText(reader);
					}else
						goToEndTag(reader, reader.getLocalName());
				}
				// Only two info are required: the source and the target columns!
				if (fromCol == null)
					throw new TAPException(getPosition(reader) + " Missing \"fromColumn\"!");
				else if (targetCol == null)
					throw new TAPException(getPosition(reader) + " Missing \"targetColumn\"!");
				else
					fk.keyColumns.put(fromCol, targetCol);
			}else
				goToEndTag(reader, tag);
		}

		// Check the last read tag is the END ELEMENT of a foreignKey node:
		if (reader.getEventType() != XMLStreamConstants.END_ELEMENT)
			throw new TAPException(getPosition(reader) + " Unexpected tag! An END ELEMENT tag for foreignKey was expected.");
		else if (!reader.getLocalName().equalsIgnoreCase("foreignKey"))
			throw new TAPException(getPosition(reader) + " Unexpected node end tag: </" + reader.getLocalName() + ">! An END ELEMENT tag for foreignKey was expected.");

		// The target table name is required!
		if (fk.targetTable == null)
			throw new TAPException(getPosition(reader) + " Missing \"targetTable\"!");
		// At least one columns association is expected!
		else if (fk.keyColumns.size() == 0)
			throw new TAPException(getPosition(reader) + " Missing at least one \"fkColumn\"!");

		return fk;
	}

	/* ***************** */
	/* UTILITY FUNCTIONS */
	/* ***************** */

	/**
	 * <p>Get the current position of the given reader.</p>
	 * 
	 * <p>
	 * 	This position is returned as a string having the following syntax: "[l.x,c.y]"
	 * 	(where x is the line number and y the column number ; x and y start at 1 ; x and y
	 * 	are both -1 if the end of the XML document has been reached).
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	The column position is generally just after the read element (node start/end tag, characters).
	 * 	However, with CHARACTERS items, this column position may be 2 characters after the real end.
	 * </i></p>
	 * 
	 * @param reader	XML reader whose the current position must be returned.
	 * 
	 * @return	A string representing the current reader position.
	 */
	protected final String getPosition(final XMLStreamReader reader){
		return "[l." + reader.getLocation().getLineNumber() + ",c." + reader.getLocation().getColumnNumber() + "]";
	}

	/**
	 * Skip every elements until a START ELEMENT or an END ELEMENT is reached.
	 * 
	 * @param reader	XML reader.
	 * 
	 * @return	The event of the last read tag. <i>Here, either {@link XMLStreamConstants#START_ELEMENT} or {@link XMLStreamConstants#END_ELEMENT}.</i>
	 * 
	 * @throws XMLStreamException	If there is an error processing the underlying XML source.
	 */
	protected final int nextTag(final XMLStreamReader reader) throws XMLStreamException{
		int event = -1;
		do{
			event = reader.next();
		}while(event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT);
		return event;
	}

	/**
	 * <p>Skip all tags from the current position to the end of the specified node.</p>
	 * 
	 * <p><b>IMPORTANT:
	 * 	This function MUST be called ONLY IF the reader is inside the node whose the end tag is searched.
	 * 	It may be in a child of this node or not, but the most important is to be inside it.
	 * </b></p>
	 * 
	 * <p><i>Note:
	 * 	No tag will be read if the given startNode is NULL or an empty string.
	 * </i></p>
	 * 
	 * @param reader	XML reader.
	 * @param startNode	Name of the node whose the end must be reached.
	 * 
	 * @throws XMLStreamException	If there is an error processing the underlying XML source.
	 * @throws TAPException			If the name of the only corresponding end element does not match the given one,
	 *                     			or if the END ELEMENT can not be found <i>(2 possible reasons for that:
	 *                     			1/ malformed XML document, 2/ this function has been called before the START ELEMENT has been read)</i>.
	 */
	protected final void goToEndTag(final XMLStreamReader reader, final String startNode) throws XMLStreamException, TAPException{
		if (startNode == null || startNode.trim().length() <= 0)
			return;
		else if (reader.getEventType() == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equalsIgnoreCase(startNode))
			return;

		int level = 0, event;
		while(reader.hasNext()){
			event = reader.next();
			switch(event){
				case XMLStreamConstants.START_ELEMENT:
					level++;
					break;
				case XMLStreamConstants.END_ELEMENT:
					if (level <= 0 && reader.getLocalName().equalsIgnoreCase(startNode)) // "level <= 0" because the reader may be inside a child of the node whose the end is searched.
						return;
					else
						level--;
			}
		}

		/* If no matching END ELEMENT, then either the XML document is malformed
		 * or #goToEndTag(...) has been called before the corresponding START ELEMENT has been read: */
		throw new TAPException(getPosition(reader) + " Malformed XML document: missing an END TAG </" + startNode + ">!");
	}

	/**
	 * <p>Get the text of the current node.</p>
	 * 
	 * <p>
	 * 	This function iterates while the next tags are of type CHARACTERS.
	 * 	Consequently, the next tag (start or end element) is already read when returning this function.
	 * </p>
	 * 
	 * <p>
	 * 	All CHARACTERS elements are concatenated.
	 * 	All leading and trailing space characters (\r \n \t and ' ') of every lines are deleted ; only the last or the first \n or \r are kept.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	This function is also skipping all COMMENT elements. This is particularly useful if a COMMENT is splitting a node text content ;
	 * 	in such case, the comment is ignored and both divided text are concatenated.
	 * </i></p>
	 * 
	 * @param reader	XML reader.
	 * 
	 * @return	The whole text content of the current node.
	 * 
	 * @throws XMLStreamException	If there is an error processing the underlying XML source.
	 */
	protected final String getText(final XMLStreamReader reader) throws XMLStreamException{
		StringBuffer txt = new StringBuffer();
		while(reader.next() == XMLStreamConstants.CHARACTERS || reader.getEventType() == XMLStreamConstants.COMMENT){
			if (reader.getEventType() == XMLStreamConstants.CHARACTERS){
				if (reader.getText() != null)
					txt.append(reader.getText().replaceAll("[ \\t]+([\\n\\r]+)", "$1").replaceAll("([\\n\\r]+)[ \\t]+", "$1"));
			}
		};
		return txt.toString().trim();
	}

	/**
	 * <p>Search for the specified table in the given TAP metadata.</p>
	 * 
	 * <p><i>Note: This function is not case sensitive.</i></p>
	 * 
	 * @param tableName	Name of the table to search. <i>The table name MAY be prefixed by a schema name (e.g. "mySchema.myTable").</i>
	 * @param meta		All fetched TAP metadata.
	 * @param position	Position of the table name in the XML document. <i>This parameter is ONLY used in case of error.</i>
	 * 
	 * @return	The corresponding TAP table.
	 * 
	 * @throws TAPException	If the table name syntax ([schema.]table) is incorrect,
	 *                    	or if several tables match to the specified table name (which is not prefixed by a schema name),
	 *                     	or if no match can be found.
	 */
	protected final TAPTable searchTable(final String tableName, final TAPMetadata meta, final String position) throws TAPException{
		// Extract the schema name and normalize the table name:
		String schema = null, table = tableName.trim();
		if (tableName.indexOf('.') >= 0){
			// get the schema name:
			schema = tableName.substring(0, tableName.indexOf('.')).trim();
			// test that the schema name is not null:
			if (schema.length() == 0)
				throw new TAPException(position + " Incorrect full table name - \"" + tableName + "\": empty schema name!");
			// test that the remaining table name is not null:
			else if (tableName.substring(schema.length() + 1).trim().length() == 0)
				throw new TAPException(position + " Incorrect full table name - \"" + tableName + "\": empty table name!");
			// test there is no more '.' separator in the remaining table name:
			else if (tableName.indexOf('.', schema.length() + 1) >= 0)
				throw new TAPException(position + " Incorrect full table name - \"" + tableName + "\": only a schema and a table name can be specified (expected syntax: \"schema.table\")\"!");
			// get the table name:
			table = tableName.substring(schema.length() + 1).trim();
		}

		// Find all matching tables:
		ArrayList<TAPTable> founds = new ArrayList<TAPTable>(1);
		StringBuffer foundsAsTxt = new StringBuffer();
		TAPTable t;
		Iterator<TAPTable> allTables = meta.getTables();
		while(allTables.hasNext()){
			// get the table to test:
			t = allTables.next();
			if (t == null)
				continue;
			// store it if the schema and table names match:
			if ((schema == null || t.getADQLSchemaName().equalsIgnoreCase(schema)) && t.getADQLName().equalsIgnoreCase(table)){
				// update the result array:
				founds.add(t);
				// update the text list:
				if (foundsAsTxt.length() > 0)
					foundsAsTxt.append(", ");
				foundsAsTxt.append(t.getADQLSchemaName()).append('.').append(t.getADQLName());
			}
		}

		if (founds.size() == 0)
			throw new TAPException(position + " Unknown table: \"" + tableName + "\"!");
		else if (founds.size() > 1)
			throw new TAPException(position + " Unresolved table: \"" + tableName + "\"! Several tables have the same name but in different schemas (here: " + foundsAsTxt.toString() + "). You must prefix this table name by a schema name (expected syntax: \"schema.table\").");
		else
			return founds.get(0);
	}

}
