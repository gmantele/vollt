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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import adql.db.DBTable;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import tap.metadata.TAPTable.TableType;
import tap.resource.Capabilities;
import tap.resource.TAPResource;
import tap.resource.VOSIResource;
import uk.ac.starlink.votable.VOSerializer;
import uws.ClientAbortException;
import uws.UWSToolBox;

/**
 * <p>Let listing all schemas, tables and columns available in a TAP service.
 * This list also corresponds to the TAP resource "/tables".</p>
 * 
 * <p>
 * 	Only schemas are stored in this object. So that's why only schemas can be added and removed
 * 	from this class. However, {@link TAPSchema} objects are listing tables, whose the object
 * 	representation is listing columns. So to add tables, you must first embed them in a schema.
 * </p>
 * 
 * <p>
 * 	All metadata have two names: one to use in ADQL queries and the other to use when really querying
 * 	the database. This is very useful to hide the real complexity of the database and propose
 * 	a simpler view of the query-able data. It is particularly useful if a schema does not exist in the
 * 	database but has been added in the TAP schema for more logical separation on the user point of view.
 * 	In a such case, the schema would have an ADQL name but no DB name (NULL value ; which is possible only
 * 	with {@link TAPSchema} objects).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.1 (03/2017)
 */
public class TAPMetadata implements Iterable<TAPSchema>, VOSIResource, TAPResource {

	/** Resource name of the TAP metadata. This name is also used - in this class - in the TAP URL to identify this resource.
	 * Here it corresponds to the following URI: ".../tables". */
	public static final String RESOURCE_NAME = "tables";

	/** List of all schemas available through the TAP service. */
	protected final Map<String,TAPSchema> schemas;

	/** Part of the TAP URI which identify this TAP resource.
	 * By default, it is the resource name ; so here, the corresponding TAP URI would be: "/tables". */
	protected String accessURL = getName();

	/** The path of the XSLT style-sheet to apply.
	 * @version 2.1 */
	protected String xsltPath = null;

	/**
	 * <p>Build an empty list of metadata.</p>
	 * 
	 * <p><i>Note:
	 * 	By default, a TAP service must have at least a TAP_SCHEMA schema which contains a set of 5 tables
	 * 	(schemas, tables, columns, keys and key_columns). This schema is not created here by default
	 * 	because it can be customized by the service implementor. Besides, the DB name may be different.
	 * 	However, you can easily get this schema thanks to the function {@link #getStdSchema(boolean)}
	 * 	which returns the standard definition of this schema (including all tables and columns described
	 * 	by the standard). For a standard definition of this schema, you can then write the following:
	 * </i></p>
	 * <pre>
	 * TAPMetadata meta = new TAPMetadata();
	 * meta.addSchema(TAPMetadata.getStdSchema());
	 * </pre>
	 * <p><i>
	 * 	Of course, this schema (and its tables and their columns) can be customized after if needed.
	 * 	Otherwise, if you want customize just some part of this schema, you can also use the function
	 * 	{@link #getStdTable(STDTable)} to get just the standard definition of some of its tables, either
	 * 	to customize them or to merely get them and keep them like they are.
	 * </i></p>
	 */
	public TAPMetadata(){
		schemas = new LinkedHashMap<String,TAPSchema>();
	}

	/**
	 * Gets the path/URL of the XSLT style-sheet to use.
	 * 
	 * @return	XSLT path/url.
	 * 
	 * @version 2.1
	 */
	public final String getXSLTPath(){
		return xsltPath;
	}

	/**
	 * Sets the path/URL of the XSLT style-sheet to use.
	 * 
	 * @param path	The new XSLT path/URL.
	 * 
	 * @version 2.1
	 */
	public final void setXSLTPath(final String path){
		if (path == null)
			xsltPath = null;
		else{
			xsltPath = path.trim();
			if (xsltPath.isEmpty())
				xsltPath = null;
		}
	}

	/**
	 * <p>Add the given schema inside this TAP metadata set.</p>
	 * 
	 * <p><i>Note:
	 * 	If the given schema is NULL, nothing will be done.
	 * </i></p>
	 * 
	 * @param s	The schema to add.
	 */
	public final void addSchema(TAPSchema s){
		if (s != null && s.getADQLName() != null)
			schemas.put(s.getADQLName(), s);
	}

	/**
	 * <p>Build a new {@link TAPSchema} object with the given ADQL name.
	 * Then, add it inside this TAP metadata set.</p>
	 * 
	 * <p><i>Note:
	 * 	The built {@link TAPSchema} object is returned, so that being modified afterwards if needed.
	 * </i></p>
	 * 
	 * @param schemaName	ADQL name of the schema to create and add inside this TAP metadata set.
	 * 
	 * @return	The created and added schema,
	 *        	or NULL if the given schema is NULL or an empty string.
	 * 
	 * @see TAPSchema#TAPSchema(String)
	 * @see #addSchema(TAPSchema)
	 */
	public TAPSchema addSchema(String schemaName){
		if (schemaName == null || schemaName.trim().length() <= 0)
			return null;

		TAPSchema s = new TAPSchema(schemaName);
		addSchema(s);
		return s;
	}

	/**
	 * <p>Build a new {@link TAPSchema} object with the given ADQL name.
	 * Then, add it inside this TAP metadata set.</p>
	 * 
	 * <p><i>Note:
	 * 	The built {@link TAPSchema} object is returned, so that being modified afterwards if needed.
	 * </i></p>
	 * 
	 * @param schemaName	ADQL name of the schema to create and add inside this TAP metadata set.
	 * @param description	Description of the new schema. <i>MAY be NULL</i>
	 * @param utype			UType associating the new schema with a data-model. <i>MAY be NULL</i>
	 * 
	 * @return	The created and added schema,
	 *        	or NULL if the given schema is NULL or an empty string.
	 * 
	 * @see TAPSchema#TAPSchema(String, String, String)
	 * @see #addSchema(TAPSchema)
	 */
	public TAPSchema addSchema(String schemaName, String description, String utype){
		if (schemaName == null)
			return null;

		TAPSchema s = new TAPSchema(schemaName, description, utype);
		addSchema(s);
		return s;
	}

	/**
	 * <p>Tell whether there is a schema with the given ADQL name.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * @param schemaName	ADQL name of the schema whose the existence must be checked.
	 * 
	 * @return	<i>true</i> if a schema with the given ADQL name exists, <i>false</i> otherwise.
	 */
	public final boolean hasSchema(String schemaName){
		if (schemaName == null)
			return false;
		else
			return schemas.containsKey(schemaName);
	}

	/**
	 * <p>Search for a schema having the given ADQL name.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * @param schemaName	ADQL name of the schema to search.
	 * 
	 * @return	The schema having the given ADQL name,
	 *        	or NULL if no such schema can be found.
	 */
	public final TAPSchema getSchema(String schemaName){
		if (schemaName == null)
			return null;
		else
			return schemas.get(schemaName);
	}

	/**
	 * Get the number of schemas contained in this TAP metadata set.
	 * 
	 * @return	Number of all schemas.
	 */
	public final int getNbSchemas(){
		return schemas.size();
	}

	/**
	 * Tell whether this TAP metadata set contains no schema.
	 * 
	 * @return	<i>true</i> if this TAP metadata set has no schema,
	 *        	<i>false</i> if it contains at least one schema.
	 */
	public final boolean isEmpty(){
		return schemas.isEmpty();
	}

	/**
	 * <p>Remove the schema having the given ADQL name.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * <p><i><b>WARNING:</b>
	 * 	If the goal of this function's call is to delete definitely the specified schema
	 * 	from the metadata, you SHOULD also call {@link TAPTable#removeAllForeignKeys()} on the
	 * 	removed table. Indeed, foreign keys of this table would still link the removed table
	 * 	with other tables AND columns of the whole metadata set.
	 * </i></p>
	 * 
	 * @param schemaName	ADQL name of the schema to remove from this TAP metadata set.
	 * 
	 * @return	The removed schema,
	 *        	or NULL if no such schema can be found.
	 */
	public final TAPSchema removeSchema(String schemaName){
		if (schemaName == null)
			return null;
		else
			return schemas.remove(schemaName);
	}

	/**
	 * Remove all schemas of this metadata set.
	 */
	public final void removeAllSchemas(){
		schemas.clear();
	}

	@Override
	public final Iterator<TAPSchema> iterator(){
		return schemas.values().iterator();
	}

	/**
	 * Get the list of all tables available in this TAP metadata set.
	 * 
	 * @return	An iterator over the list of all tables contained in this TAP metadata set.
	 */
	public Iterator<TAPTable> getTables(){
		return new TAPTableIterator(this);
	}

	/**
	 * <p>Tell whether this TAP metadata set contains the specified table.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * @param schemaName	ADQL name of the schema owning the table to search.
	 * @param tableName		ADQL name of the table to search.
	 * 
	 * @return	<i>true</i> if the specified table exists, <i>false</i> otherwise.
	 */
	public boolean hasTable(String schemaName, String tableName){
		TAPSchema s = getSchema(schemaName);
		if (s != null)
			return s.hasTable(tableName);
		else
			return false;
	}

	/**
	 * <p>Tell whether this TAP metadata set contains a table with the given ADQL name, whatever is its schema.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * @param tableName	ADQL name of the table to search.
	 * 
	 * @return	<i>true</i> if the specified table exists, <i>false</i> otherwise.
	 */
	public boolean hasTable(String tableName){
		for(TAPSchema s : this)
			if (s.hasTable(tableName))
				return true;
		return false;
	}

	/**
	 * <p>Search for the specified table in this TAP metadata set.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * @param schemaName	ADQL name of the schema owning the table to search.
	 * @param tableName		ADQL name of the table to search.
	 * 
	 * @return	The table which has the given ADQL name and which is inside the specified schema,
	 *        	or NULL if no such table can be found.
	 */
	public TAPTable getTable(String schemaName, String tableName){
		TAPSchema s = getSchema(schemaName);
		if (s != null)
			return s.getTable(tableName);
		else
			return null;
	}

	/**
	 * <p>Search in this TAP metadata set for all tables whose the ADQL name matches the given one,
	 * whatever is their schema.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * @param tableName		ADQL name of the tables to search.
	 * 
	 * @return	A list of all the tables which have the given ADQL name,
	 *        	or an empty list if no such table can be found.
	 */
	public ArrayList<DBTable> getTable(String tableName){
		ArrayList<DBTable> tables = new ArrayList<DBTable>();
		for(TAPSchema s : this)
			if (s.hasTable(tableName))
				tables.add(s.getTable(tableName));
		return tables;
	}

	/**
	 * Get the description of the ObsCore table, if it is defined.
	 * 
	 * <p>
	 * 	This function is case sensitive only on the schema name
	 * 	(i.e. <code>ivoa</code>) which must be defined in full lower case.
	 * 	The table name (i.e. <code>ObsCore</code>) will be found whatever
	 * 	the case it is written in.
	 * </p>
	 * 
	 * @return	Description of the ObsCore table,
	 *        	or <code>NULL</code> if this table is not provided by this TAP service.
	 * 
	 * @since 2.1
	 */
	public TAPTable getObsCoreTable(){
		TAPSchema ivoaSchema = getSchema("ivoa");
		if (ivoaSchema != null){
			for(TAPTable t : ivoaSchema){
				if (t.getADQLName().equalsIgnoreCase("obscore"))
					return t;
			}
		}
		return null;
	}

	/**
	 * Get the number of all tables contained in this TAP metadata set.
	 * 
	 * @return	Number of all its tables.
	 */
	public int getNbTables(){
		int nbTables = 0;
		for(TAPSchema s : this)
			nbTables += s.getNbTables();
		return nbTables;
	}

	/**
	 * Let iterating over the list of all tables contained in a given {@link TAPMetadata} object.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.0 (08/2014)
	 */
	protected static class TAPTableIterator implements Iterator<TAPTable> {
		private Iterator<TAPSchema> it;
		private Iterator<TAPTable> itTables;

		public TAPTableIterator(TAPMetadata tapSchema){
			it = tapSchema.iterator();

			if (it.hasNext())
				itTables = it.next().iterator();

			prepareNext();
		}

		protected void prepareNext(){
			while(!itTables.hasNext() && it.hasNext())
				itTables = it.next().iterator();

			if (!itTables.hasNext()){
				it = null;
				itTables = null;
			}
		}

		@Override
		public boolean hasNext(){
			return itTables != null;
		}

		@Override
		public TAPTable next(){
			if (itTables == null)
				throw new NoSuchElementException("No more table in TAP_SCHEMA !");
			else{
				TAPTable t = itTables.next();

				prepareNext();

				return t;
			}
		}

		@Override
		public void remove(){
			if (itTables != null)
				itTables.remove();
			else
				throw new IllegalStateException("Impossible to remove the table because there is no more table in TAP_SCHEMA !");
		}
	}

	@Override
	public String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public void setTAPBaseURL(String baseURL){
		accessURL = ((baseURL == null) ? "" : (baseURL + "/")) + getName();
	}

	@Override
	public String getAccessURL(){
		return accessURL;
	}

	@Override
	public String getCapability(){
		return Capabilities.getDefaultCapability(this);
	}

	@Override
	public String getStandardID(){
		return "ivo://ivoa.net/std/VOSI#tables";
	}

	@Override
	public void init(ServletConfig config) throws ServletException{}

	@Override
	public void destroy(){}

	@Override
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws IOException{
		response.setContentType("application/xml");
		response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

		PrintWriter writer = response.getWriter();
		write(writer);

		return false;
	}

	/**
	 * Format in XML this whole metadata set and write it in the given writer.
	 * 
	 * @param writer	Stream in which the XML representation of this metadata must be written.
	 * 
	 * @throws IOException	If there is any error while writing the XML in the given writer.
	 * 
	 * @since 2.0
	 */
	public void write(final PrintWriter writer) throws IOException{
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		if (xsltPath != null){
			writer.print("<?xml-stylesheet type=\"text/xsl\" ");
			writer.print(VOSerializer.formatAttribute("href", xsltPath));
			writer.println("?>");
		}

		/* TODO The XSD schema for VOSITables should be fixed soon! This schema should be changed here before the library is released!
		 * Note: the XSD schema at http://www.ivoa.net/xml/VOSITables/v1.0 contains an incorrect targetNamespace ("http://www.ivoa.net/xml/VOSICapabilities/v1.0").
		 *       In order to make this XML document valid, a custom location toward a correct XSD schema is used: http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd */
		writer.println("<vosi:tableset xmlns:vosi=\"http://www.ivoa.net/xml/VOSITables/v1.0\" xmlns:vod=\"http://www.ivoa.net/xml/VODataService/v1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VOSITables/v1.0 http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd\">");

		for(TAPSchema s : schemas.values())
			writeSchema(s, writer);

		writer.println("</vosi:tableset>");

		UWSToolBox.flush(writer);
	}

	/**
	 * <p>Format in XML the given schema and then write it in the given writer.</p>
	 * 
	 * <p>Written lines:</p>
	 * <pre>
	 * &lt;schema&gt;
	 * 	&lt;name&gt;...&lt;/name&gt;
	 * 	&lt;title&gt;...&lt;/title&gt;
	 * 	&lt;description&gt;...&lt;/description&gt;
	 * 	&lt;utype&gt;...&lt;/utype&gt;
	 * 		// call #writeTable(TAPTable, PrintWriter) for each table
	 * &lt;/schema&gt;
	 * </pre>
	 * 
	 * <p><i>Note:
	 * 	When NULL an attribute or a field is not written. Here this rule concerns: description and utype.
	 * </i></p>
	 * 
	 * @param s			The schema to format and to write in XML.
	 * @param writer	Output in which the XML serialization of the given schema must be written.
	 * 
	 * @throws IOException	If the connection with the HTTP client has been either canceled or closed for another reason.
	 * 
	 * @see #writeTable(TAPTable, PrintWriter)
	 */
	protected void writeSchema(TAPSchema s, PrintWriter writer) throws IOException{
		final String prefix = "\t\t";
		writer.println("\t<schema>");

		writeAtt(prefix, "name", s.getRawName(), false, writer);
		writeAtt(prefix, "title", s.getTitle(), true, writer);
		writeAtt(prefix, "description", s.getDescription(), true, writer);
		writeAtt(prefix, "utype", s.getUtype(), true, writer);

		int nbColumns = 0;
		for(TAPTable t : s){

			// write each table:
			nbColumns += writeTable(t, writer);

			// flush the PrintWriter buffer when at least 30 tables have been read:
			/* Note: the buffer may have already been flushed before automatically,
			 *       but this manual flush is also checking whether any error has occurred while writing the previous characters.
			 *       If so, a ClientAbortException (extension of IOException) is thrown in order to interrupt the writing of the
			 *       metadata and thus, in order to spare server resources (and particularly memory if the metadata set is large). */
			if (nbColumns / 30 > 1){
				UWSToolBox.flush(writer);
				nbColumns = 0;
			}

		}

		writer.println("\t</schema>");

		if (nbColumns > 0)
			UWSToolBox.flush(writer);
	}

	/**
	 * <p>Format in XML the given table and then write it in the given writer.</p>
	 * 
	 * <p>Written lines:</p>
	 * <pre>
	 * &lt;table type="..."&gt;
	 * 	&lt;name&gt;...&lt;/name&gt;
	 * 	&lt;title&gt;...&lt;/title&gt;
	 * 	&lt;description&gt;...&lt;/description&gt;
	 * 	&lt;utype&gt;...&lt;/utype&gt;
	 * 		// call #writeColumn(TAPColumn, PrintWriter) for each column
	 * 		// call #writeForeignKey(TAPForeignKey, PrintWriter) for each foreign key
	 * &lt;/table&gt;
	 * </pre>
	 * 
	 * <p><i>Note 1:
	 * 	When NULL an attribute or a field is not written. Here this rule concerns: description and utype.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The PrintWriter buffer is flushed all the 10 columns. At that moment the writer is checked for errors.
	 * 	If the error flag is set, a {@link ClientAbortException} is thrown in order to stop the metadata writing.
	 * 	This is particularly useful if the metadata data is pretty large.
	 * </i></p>
	 * 
	 * @param t			The table to format and to write in XML.
	 * @param writer	Output in which the XML serialization of the given table must be written.
	 * 
	 * @return	The total number of written columns.
	 */
	protected int writeTable(TAPTable t, PrintWriter writer){
		final String prefix = "\t\t\t";

		writer.print("\t\t<table");
		if (t.getType() != null){
			if (t.getType() != TableType.table)
				writer.print(VOSerializer.formatAttribute("type", t.getType().toString()));
		}
		writer.println(">");

		writeAtt(prefix, "name", t.getRawName(), false, writer);
		writeAtt(prefix, "title", t.getTitle(), true, writer);
		writeAtt(prefix, "description", t.getDescription(), true, writer);
		writeAtt(prefix, "utype", t.getUtype(), true, writer);

		int nbCol = 0;
		Iterator<TAPColumn> itCols = t.getColumns();
		while(itCols.hasNext()){
			writeColumn(itCols.next(), writer);
			nbCol++;
		}

		Iterator<TAPForeignKey> itFK = t.getForeignKeys();
		while(itFK.hasNext())
			writeForeignKey(itFK.next(), writer);

		writer.println("\t\t</table>");

		return nbCol;
	}

	/**
	 * <p>Format in XML the given column and then write it in the given writer.</p>
	 * 
	 * <p>Written lines:</p>
	 * <pre>
	 * &lt;column std="true|false"&gt; // the value of this field is TAPColumn#isStd()
	 * 	&lt;name&gt;...&lt;/name&gt;
	 * 	&lt;description&gt;...&lt;/description&gt;
	 * 	&lt;unit&gt;...&lt;/unit&gt;
	 * 	&lt;utype&gt;...&lt;/utype&gt;
	 * 	&lt;ucd&gt;...&lt;/ucd&gt;
	 * 	&lt;dataType xsi:type="vod:TAPType" size="..."&gt;...&lt;/dataType&gt;
	 * 	&lt;flag&gt;indexed&lt;/flag&gt; // if TAPColumn#isIndexed()
	 * 	&lt;flag&gt;primary&lt;/flag&gt; // if TAPColumn#isPrincipal()
	 * &lt;/column&gt;
	 * </pre>
	 * 
	 * <p><i>Note:
	 * 	When NULL an attribute or a field is not written. Here this rule concerns: description, unit, utype, ucd and flags.
	 * </i></p>
	 * 
	 * @param c			The column to format and to write in XML.
	 * @param writer	Output in which the XML serialization of the given column must be written.
	 */
	protected void writeColumn(TAPColumn c, PrintWriter writer){
		final String prefix = "\t\t\t\t";

		writer.print("\t\t\t<column");
		if (c.isStd())
			writer.print(" std=\"true\"");
		writer.println(">");

		writeAtt(prefix, "name", c.getRawName(), false, writer);
		writeAtt(prefix, "description", c.getDescription(), true, writer);
		writeAtt(prefix, "unit", c.getUnit(), true, writer);
		writeAtt(prefix, "ucd", c.getUcd(), true, writer);
		writeAtt(prefix, "utype", c.getUtype(), true, writer);

		if (c.getDatatype() != null){
			writer.print(prefix);
			writer.print("<dataType xsi:type=\"vod:TAPType\"");
			if (c.getDatatype().length > 0){
				writer.print(" size=\"");
				writer.print(c.getDatatype().length);
				writer.print("\"");
			}
			writer.print('>');
			writer.print(VOSerializer.formatText(c.getDatatype().type.toString().toUpperCase()));
			writer.println("</dataType>");
		}

		if (c.isIndexed())
			writeAtt(prefix, "flag", "indexed", true, writer);
		if (c.isPrincipal())
			writeAtt(prefix, "flag", "primary", true, writer);
		if (c.isNullable())
			writeAtt(prefix, "flag", "nullable", true, writer);

		writer.println("\t\t\t</column>");
	}

	/**
	 * <p>Format in XML the given foreign key and then write it in the given writer.</p>
	 * 
	 * <p>Written lines:</p>
	 * <pre>
	 * &lt;foreignKey&gt;
	 * 	&lt;targetTable&gt;...&lt;/targetTable&gt;
	 * 	&lt;description&gt;...&lt;/description&gt;
	 * 	&lt;utype&gt;...&lt;/utype&gt;
	 * 	&lt;fkColumn&gt;
	 * 		&lt;fromColumn&gt;...&lt;/fromColumn&gt;
	 * 		&lt;targetColumn&gt;...&lt;/targetColumn&gt;
	 * 	&lt;/fkColumn&gt;
	 * 	...
	 * &lt;/foreignKey&gt;
	 * </pre>
	 * 
	 * <p><i>Note:
	 * 	When NULL an attribute or a field is not written. Here this rule concerns: description and utype.
	 * </i></p>
	 * 
	 * @param fk		The foreign key to format and to write in XML.
	 * @param writer	Output in which the XML serialization of the given foreign key must be written.
	 */
	protected void writeForeignKey(TAPForeignKey fk, PrintWriter writer){
		final String prefix = "\t\t\t\t";

		writer.println("\t\t\t<foreignKey>");

		writeAtt(prefix, "targetTable", fk.getTargetTable().getRawName(), false, writer);
		writeAtt(prefix, "description", fk.getDescription(), true, writer);
		writeAtt(prefix, "utype", fk.getUtype(), true, writer);

		final String prefix2 = prefix + "\t";
		for(Map.Entry<String,String> entry : fk){
			writer.print(prefix);
			writer.println("<fkColumn>");
			writeAtt(prefix2, "fromColumn", entry.getKey(), false, writer);
			writeAtt(prefix2, "targetColumn", entry.getValue(), false, writer);
			writer.print(prefix);
			writer.println("</fkColumn>");
		}

		writer.println("\t\t\t</foreignKey>");
	}

	/**
	 * Write the specified metadata attribute as a simple XML node.
	 * 
	 * @param prefix			Prefix of the XML node. (generally, space characters)
	 * @param attributeName		Name of the metadata attribute to write (= Name of the XML node).
	 * @param attributeValue	Value of the metadata attribute (= Value of the XML node).
	 * @param isOptionalAttr	<i>true</i> if the attribute to write is optional (in this case, if the value is NULL or an empty string, the whole attribute item won't be written),
	 *                      	<i>false</i> otherwise (here, if the value is NULL or an empty string, the XML item will be written with an empty string as value).
	 * @param writer			Output in which the XML node must be written.
	 */
	protected final void writeAtt(String prefix, String attributeName, String attributeValue, boolean isOptionalAttr, PrintWriter writer){
		if (attributeValue != null && attributeValue.trim().length() > 0){
			StringBuffer xml = new StringBuffer(prefix);
			xml.append('<').append(attributeName).append('>').append(VOSerializer.formatText(attributeValue)).append("</").append(attributeName).append('>');
			writer.println(xml.toString());
		}else if (!isOptionalAttr)
			writer.println("<" + attributeName + "></" + attributeName + ">");
	}

	/**
	 * <p>
	 * 	Get the definition of the whole standard TAP_SCHEMA. Thus, all standard TAP_SCHEMA tables
	 * 	(with all their columns) are also included in this object.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	This function create the {@link TAPSchema} and all its {@link TAPTable}s objects on the fly.
	 * </p>
	 * 
	 * @param isSchemaSupported	<i>false</i> if the DB name must be prefixed by "TAP_SCHEMA_", <i>true</i> otherwise.
	 * 
	 * @return	The whole TAP_SCHEMA definition.
	 * 
	 * @see STDSchema#TAPSCHEMA
	 * @see STDTable
	 * @see #getStdTable(STDTable)
	 * 
	 * @since 2.0
	 */
	public static final TAPSchema getStdSchema(final boolean isSchemaSupported){
		TAPSchema tap_schema = new TAPSchema(STDSchema.TAPSCHEMA.toString(), "Set of tables listing and describing the schemas, tables and columns published in this TAP service.", null);
		if (!isSchemaSupported)
			tap_schema.setDBName(null);
		for(STDTable t : STDTable.values()){
			TAPTable table = getStdTable(t);
			tap_schema.addTable(table);
			if (!isSchemaSupported)
				table.setDBName(STDSchema.TAPSCHEMA.label + "_" + table.getADQLName());
		}
		return tap_schema;
	}

	/**
	 * <p>Get the definition of the specified standard TAP table.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	The returned table is not linked at all with a schema, on the contrary of {@link #getStdSchema(boolean)} which returns tables linked with the returned schema.
	 * 	So, you may have to linked this table to schema (by using {@link TAPSchema#addTable(TAPTable)}) whose the ADQL name is TAP_SCHEMA after calling this function.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This function create the {@link TAPTable} object on the fly.
	 * </p>
	 * 
	 * @param tableId	ID of the TAP table to return.
	 * 
	 * @return	The corresponding table definition (with no schema).
	 * 
	 * @since 2.0
	 */
	public static final TAPTable getStdTable(final STDTable tableId){
		switch(tableId){

			case SCHEMAS:
				TAPTable schemas = new TAPTable(STDSchema.TAPSCHEMA + "." + STDTable.SCHEMAS, TableType.table, "List of schemas published in this TAP service.", null);
				schemas.addColumn("schema_index", new DBType(DBDatatype.INTEGER), "this index is used to recommend schema ordering for clients", null, null, null, false, false, true);
				schemas.addColumn("schema_name", new DBType(DBDatatype.VARCHAR), "schema name, possibly qualified", null, null, null, true, true, true);
				schemas.addColumn("description", new DBType(DBDatatype.VARCHAR), "brief description of schema", null, null, null, true, false, true);
				schemas.addColumn("utype", new DBType(DBDatatype.VARCHAR), "UTYPE if schema corresponds to a data model", null, null, null, false, false, true);
				return schemas;

			case TABLES:
				TAPTable tables = new TAPTable(STDSchema.TAPSCHEMA + "." + STDTable.TABLES, TableType.table, "List of tables published in this TAP service.", null);
				tables.addColumn("table_index", new DBType(DBDatatype.INTEGER), "this index is used to recommend table ordering for clients", null, null, null, false, false, true);
				tables.addColumn("schema_name", new DBType(DBDatatype.VARCHAR), "the schema name from TAP_SCHEMA.schemas", null, null, null, true, false, true);
				tables.addColumn("table_name", new DBType(DBDatatype.VARCHAR), "table name as it should be used in queries", null, null, null, true, true, true);
				tables.addColumn("table_type", new DBType(DBDatatype.VARCHAR), "one of: table, view", null, null, null, false, false, true);
				tables.addColumn("description", new DBType(DBDatatype.VARCHAR), "brief description of table", null, null, null, true, false, true);
				tables.addColumn("utype", new DBType(DBDatatype.VARCHAR), "UTYPE if table corresponds to a data model", null, null, null, false, false, true);
				return tables;

			case COLUMNS:
				TAPTable columns = new TAPTable(STDSchema.TAPSCHEMA + "." + STDTable.COLUMNS, TableType.table, "List of columns of all tables listed in TAP_SCHEMA.TABLES and published in this TAP service.", null);
				columns.addColumn("column_index", new DBType(DBDatatype.INTEGER), "this index is used to recommend column ordering for clients", null, null, null, false, false, true);
				columns.addColumn("table_name", new DBType(DBDatatype.VARCHAR), "table name from TAP_SCHEMA.tables", null, null, null, true, true, true);
				columns.addColumn("column_name", new DBType(DBDatatype.VARCHAR), "column name", null, null, null, true, true, true);
				columns.addColumn("datatype", new DBType(DBDatatype.VARCHAR), "an XType or a TAPType", null, null, null, true, false, true);
				columns.addColumn("arraysize", new DBType(DBDatatype.INTEGER), "length of variable length datatypes", null, null, null, false, false, true);
				columns.addColumn("\"size\"", new DBType(DBDatatype.INTEGER), "same as \"arraysize\" but kept for backward compatibility only", null, null, null, false, false, true);
				columns.addColumn("description", new DBType(DBDatatype.VARCHAR), "brief description of column", null, null, null, true, false, true);
				columns.addColumn("utype", new DBType(DBDatatype.VARCHAR), "UTYPE of column if any", null, null, null, false, false, true);
				columns.addColumn("unit", new DBType(DBDatatype.VARCHAR), "unit in VO standard format", null, null, null, true, false, true);
				columns.addColumn("ucd", new DBType(DBDatatype.VARCHAR), "UCD of column if any", null, null, null, true, false, true);
				columns.addColumn("indexed", new DBType(DBDatatype.INTEGER), "an indexed column; 1 means true, 0 means false", null, null, null, false, false, true);
				columns.addColumn("principal", new DBType(DBDatatype.INTEGER), "a principal column; 1 means true, 0 means false", null, null, null, false, false, true);
				columns.addColumn("std", new DBType(DBDatatype.INTEGER), "a standard column; 1 means true, 0 means false", null, null, null, false, false, true);
				return columns;

			case KEYS:
				TAPTable keys = new TAPTable(STDSchema.TAPSCHEMA + "." + STDTable.KEYS, TableType.table, "List all foreign keys but provides just the tables linked by the foreign key. To know which columns of these tables are linked, see in TAP_SCHEMA.key_columns using the key_id.", null);
				keys.addColumn("key_id", new DBType(DBDatatype.VARCHAR), "unique key identifier", null, null, null, true, true, true);
				keys.addColumn("from_table", new DBType(DBDatatype.VARCHAR), "fully qualified table name", null, null, null, true, false, true);
				keys.addColumn("target_table", new DBType(DBDatatype.VARCHAR), "fully qualified table name", null, null, null, true, false, true);
				keys.addColumn("description", new DBType(DBDatatype.VARCHAR), "description of this key", null, null, null, true, false, true);
				keys.addColumn("utype", new DBType(DBDatatype.VARCHAR), "utype of this key", null, null, null, false, false, true);
				return keys;

			case KEY_COLUMNS:
				TAPTable key_columns = new TAPTable(STDSchema.TAPSCHEMA + "." + STDTable.KEY_COLUMNS, TableType.table, "List all foreign keys but provides just the columns linked by the foreign key. To know the table of these columns, see in TAP_SCHEMA.keys using the key_id.", null);
				key_columns.addColumn("key_id", new DBType(DBDatatype.VARCHAR), "unique key identifier", null, null, null, true, true, true);
				key_columns.addColumn("from_column", new DBType(DBDatatype.VARCHAR), "key column name in the from_table", null, null, null, true, true, true);
				key_columns.addColumn("target_column", new DBType(DBDatatype.VARCHAR), "key column name in the target_table", null, null, null, true, true, true);
				return key_columns;

			default:
				return null;
		}
	}

	/**
	 * <p>Tell whether the given table name is a standard TAP table.</p>
	 * 
	 * <p><i>Note:
	 *	This function is case sensitive. Indeed TAP_SCHEMA tables are defined by the TAP standard by a given case.
	 *	Thus, this case is expected here.
	 * </i></p>
	 * 
	 * @param tableName	Unqualified table name.
	 * 
	 * @return	The corresponding {@link STDTable} or NULL if the given table is not part of the TAP standard.
	 * 
	 * @since 2.0
	 */
	public static final STDTable resolveStdTable(String tableName){
		if (tableName == null || tableName.trim().length() == 0)
			return null;

		for(STDTable t : STDTable.values()){
			if (t.label.equals(tableName))
				return t;
		}

		return null;
	}

	/**
	 * Enumeration of all schemas defined in the TAP standard.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 2.0 (07/2014)
	 * @since 2.0
	 */
	public enum STDSchema{
		TAPSCHEMA("TAP_SCHEMA"), UPLOADSCHEMA("TAP_UPLOAD");

		/** Real name of the schema. */
		public final String label;

		private STDSchema(final String name){
			this.label = name;
		}

		@Override
		public String toString(){
			return label;
		}
	}

	/**
	 * Enumeration of all tables of TAP_SCHEMA.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 2.0 (07/2014)
	 * @since 2.0
	 */
	public enum STDTable{
		SCHEMAS("schemas"), TABLES("tables"), COLUMNS("columns"), KEYS("keys"), KEY_COLUMNS("key_columns");

		/** Real name of the table. */
		public final String label;

		private STDTable(final String name){
			this.label = name;
		}

		@Override
		public String toString(){
			return label;
		}
	}

}
