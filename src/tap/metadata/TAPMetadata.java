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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.metadata.TAPTable.TableType;
import tap.metadata.TAPType.TAPDatatype;
import tap.resource.Capabilities;
import tap.resource.TAPResource;
import tap.resource.VOSIResource;
import adql.db.DBTable;

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
 * @version 2.0 (08/2014)
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

	/**
	 * <p>Build an empty list of metadata.</p>
	 * 
	 * <p><i>Note:
	 * 	By default, a TAP service must have at least a TAP_SCHEMA schema which contains a set of 5 tables
	 * 	(schemas, tables, columns, keys and key_columns). This schema is not created here by default
	 * 	because it can be customized by the service implementor. Besides, the DB name may be different.
	 * 	However, you can easily get this schema thanks to the function {@link #getStdSchema()}
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
		schemas = new HashMap<String,TAPSchema>();
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
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		response.setContentType("application/xml");

		PrintWriter writer = response.getWriter();

		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		// TODO Change the xsi:schemaLocation attribute with a CDS URL !
		//writer.println("<vosi:tableset xmlns:vosi=\"http://www.ivoa.net/xml/VOSITables/v1.0\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VOSITables/v1.0 http://vo.ari.uni-heidelberg.de/docs/schemata/VODataService-v1.1.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:vod=\"http://www.ivoa.net/xml/VODataService/v1.1\">");
		writer.println("<tableset xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:vod=\"http://www.ivoa.net/xml/VODataService/v1.1\" xsi:type=\"vod:TableSet\">");

		for(TAPSchema s : schemas.values())
			writeSchema(s, writer);

		writer.println("</tableset>");

		writer.flush();

		return false;
	}

	/**
	 * <p>Format in XML the given schema and then write it in the given writer.</p>
	 * 
	 * <p>Written lines:</p>
	 * <pre>
	 * &lt;schema&gt;
	 * 	&lt;name&gt;...&lt;/name&gt;
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
	 * @throws IOException	If there is any error while writing the XML in the given writer.
	 * 
	 * @see #writeTable(TAPTable, PrintWriter)
	 */
	private void writeSchema(TAPSchema s, PrintWriter writer) throws IOException{
		final String prefix = "\t\t";
		writer.println("\t<schema>");

		writeAtt(prefix, "name", s.getADQLName(), writer);
		writeAtt(prefix, "description", s.getDescription(), writer);
		writeAtt(prefix, "utype", s.getUtype(), writer);

		for(TAPTable t : s)
			writeTable(t, writer);

		writer.println("\t</schema>");
	}

	/**
	 * <p>Format in XML the given table and then write it in the given writer.</p>
	 * 
	 * <p>Written lines:</p>
	 * <pre>
	 * &lt;table type="..."&gt;
	 * 	&lt;name&gt;...&lt;/name&gt;
	 * 	&lt;description&gt;...&lt;/description&gt;
	 * 	&lt;utype&gt;...&lt;/utype&gt;
	 * 		// call #writeColumn(TAPColumn, PrintWriter) for each column
	 * 		// call #writeForeignKey(TAPForeignKey, PrintWriter) for each foreign key
	 * &lt;/table&gt;
	 * </pre>
	 * 
	 * <p><i>Note:
	 * 	When NULL an attribute or a field is not written. Here this rule concerns: description and utype.
	 * </i></p>
	 * 
	 * @param t			The table to format and to write in XML.
	 * @param writer	Output in which the XML serialization of the given table must be written.
	 * 
	 * @throws IOException	If there is any error while writing the XML in the given writer.
	 */
	private void writeTable(TAPTable t, PrintWriter writer) throws IOException{
		final String prefix = "\t\t\t";

		writer.print("\t\t<table type=\"");
		writer.print(t.getType().toString());
		writer.println("\">");

		writeAtt(prefix, "name", t.getFullName(), writer);
		writeAtt(prefix, "description", t.getDescription(), writer);
		writeAtt(prefix, "utype", t.getUtype(), writer);

		Iterator<TAPColumn> itCols = t.getColumns();
		while(itCols.hasNext())
			writeColumn(itCols.next(), writer);

		Iterator<TAPForeignKey> itFK = t.getForeignKeys();
		while(itFK.hasNext())
			writeForeignKey(itFK.next(), writer);

		writer.println("\t\t</table>");
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
	 * 
	 * @throws IOException	If there is any error while writing the XML in the given writer.
	 */
	private void writeColumn(TAPColumn c, PrintWriter writer) throws IOException{
		final String prefix = "\t\t\t\t";

		writer.print("\t\t\t<column std=\"");
		writer.print(c.isStd());
		writer.println("\">");

		writeAtt(prefix, "name", c.getADQLName(), writer);
		writeAtt(prefix, "description", c.getDescription(), writer);
		writeAtt(prefix, "unit", c.getUnit(), writer);
		writeAtt(prefix, "ucd", c.getUcd(), writer);
		writeAtt(prefix, "utype", c.getUtype(), writer);

		if (c.getDatatype() != null){
			writer.print(prefix);
			writer.print("<dataType xsi:type=\"vod:TAPType\"");
			if (c.getDatatype().length > 0){
				writer.print(" size=\"");
				writer.print(c.getDatatype().length);
				writer.print("\"");
			}
			writer.print('>');
			writer.print(c.getDatatype().type.toString().toUpperCase());
			writer.println("</dataType>");
		}

		if (c.isIndexed())
			writeAtt(prefix, "flag", "indexed", writer);
		if (c.isPrincipal())
			writeAtt(prefix, "flag", "primary", writer);

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
	 * 
	 * @throws IOException	If there is any error while writing the XML in the given writer.
	 */
	private void writeForeignKey(TAPForeignKey fk, PrintWriter writer) throws IOException{
		final String prefix = "\t\t\t\t";

		writer.println("\t\t\t<foreignKey>");

		writeAtt(prefix, "targetTable", fk.getTargetTable().getFullName(), writer);
		writeAtt(prefix, "description", fk.getDescription(), writer);
		writeAtt(prefix, "utype", fk.getUtype(), writer);

		final String prefix2 = prefix + "\t";
		for(Map.Entry<String,String> entry : fk){
			writer.print(prefix);
			writer.println("<fkColumn>");
			writeAtt(prefix2, "fromColumn", entry.getKey(), writer);
			writeAtt(prefix2, "targetColumn", entry.getValue(), writer);
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
	 * @param writer			Output in which the XML node must be written.
	 * 
	 * @throws IOException	If there is a problem while writing the XML node inside the given writer.
	 */
	private void writeAtt(String prefix, String attributeName, String attributeValue, PrintWriter writer) throws IOException{
		if (attributeValue != null){
			StringBuffer xml = new StringBuffer(prefix);
			xml.append('<').append(attributeName).append('>').append(attributeValue).append("</").append(attributeName).append('>');
			writer.println(xml.toString());
		}
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
	 * @return	The whole TAP_SCHEMA definition.
	 * 
	 * @see STDSchema#TAPSCHEMA
	 * @see STDTable
	 * @see #getStdTable(STDTable)
	 * 
	 * @since 2.0
	 */
	public static final TAPSchema getStdSchema(){
		TAPSchema tap_schema = new TAPSchema(STDSchema.TAPSCHEMA.toString(), "Set of tables listing and describing the schemas, tables and columns published in this TAP service.", null);
		for(STDTable t : STDTable.values()){
			TAPTable table = getStdTable(t);
			tap_schema.addTable(table);
		}
		return tap_schema;
	}

	/**
	 * <p>Get the definition of the specified standard TAP table.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	The returned table is not linked at all with a schema, on the contrary of {@link #getStdSchema()} which returns tables linked with the returned schema.
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
				TAPTable schemas = new TAPTable(STDTable.SCHEMAS.toString(), TableType.table, "List of schemas published in this TAP service.", null);
				schemas.addColumn("schema_name", new TAPType(TAPDatatype.VARCHAR), "schema name, possibly qualified", null, null, null, true, true, true);
				schemas.addColumn("description", new TAPType(TAPDatatype.VARCHAR), "brief description of schema", null, null, null, false, false, true);
				schemas.addColumn("utype", new TAPType(TAPDatatype.VARCHAR), "UTYPE if schema corresponds to a data model", null, null, null, false, false, true);
				return schemas;

			case TABLES:
				TAPTable tables = new TAPTable(STDTable.TABLES.toString(), TableType.table, "List of tables published in this TAP service.", null);
				tables.addColumn("schema_name", new TAPType(TAPDatatype.VARCHAR), "the schema name from TAP_SCHEMA.schemas", null, null, null, true, true, true);
				tables.addColumn("table_name", new TAPType(TAPDatatype.VARCHAR), "table name as it should be used in queries", null, null, null, true, true, true);
				tables.addColumn("table_type", new TAPType(TAPDatatype.VARCHAR), "one of: table, view", null, null, null, false, false, true);
				tables.addColumn("description", new TAPType(TAPDatatype.VARCHAR), "brief description of table", null, null, null, false, false, true);
				tables.addColumn("utype", new TAPType(TAPDatatype.VARCHAR), "UTYPE if table corresponds to a data model", null, null, null, false, false, true);
				return tables;

			case COLUMNS:
				TAPTable columns = new TAPTable(STDTable.COLUMNS.toString(), TableType.table, "List of columns of all tables listed in TAP_SCHEMA.TABLES and published in this TAP service.", null);
				columns.addColumn("table_name", new TAPType(TAPDatatype.VARCHAR), "table name from TAP_SCHEMA.tables", null, null, null, true, true, true);
				columns.addColumn("column_name", new TAPType(TAPDatatype.VARCHAR), "column name", null, null, null, true, true, true);
				columns.addColumn("description", new TAPType(TAPDatatype.VARCHAR), "brief description of column", null, null, null, false, false, true);
				columns.addColumn("unit", new TAPType(TAPDatatype.VARCHAR), "unit in VO standard format", null, null, null, false, false, true);
				columns.addColumn("ucd", new TAPType(TAPDatatype.VARCHAR), "UCD of column if any", null, null, null, false, false, true);
				columns.addColumn("utype", new TAPType(TAPDatatype.VARCHAR), "UTYPE of column if any", null, null, null, false, false, true);
				columns.addColumn("datatype", new TAPType(TAPDatatype.VARCHAR), "ADQL datatype as in section 2.5", null, null, null, false, false, true);
				columns.addColumn("size", new TAPType(TAPDatatype.INTEGER), "length of variable length datatypes", null, null, null, false, false, true);
				columns.addColumn("principal", new TAPType(TAPDatatype.INTEGER), "a principal column; 1 means true, 0 means false", null, null, null, false, false, true);
				columns.addColumn("indexed", new TAPType(TAPDatatype.INTEGER), "an indexed column; 1 means true, 0 means false", null, null, null, false, false, true);
				columns.addColumn("std", new TAPType(TAPDatatype.INTEGER), "a standard column; 1 means true, 0 means false", null, null, null, false, false, true);
				return columns;

			case KEYS:
				TAPTable keys = new TAPTable(STDTable.KEYS.toString(), TableType.table, "List all foreign keys but provides just the tables linked by the foreign key. To know which columns of these tables are linked, see in TAP_SCHEMA.key_columns using the key_id.", null);
				keys.addColumn("key_id", new TAPType(TAPDatatype.VARCHAR), "unique key identifier", null, null, null, true, true, true);
				keys.addColumn("from_table", new TAPType(TAPDatatype.VARCHAR), "fully qualified table name", null, null, null, false, false, true);
				keys.addColumn("target_table", new TAPType(TAPDatatype.VARCHAR), "fully qualified table name", null, null, null, false, false, true);
				keys.addColumn("description", new TAPType(TAPDatatype.VARCHAR), "description of this key", null, null, null, false, false, true);
				keys.addColumn("utype", new TAPType(TAPDatatype.VARCHAR), "utype of this key", null, null, null, false, false, true);
				return keys;

			case KEY_COLUMNS:
				TAPTable key_columns = new TAPTable(STDTable.KEY_COLUMNS.toString(), TableType.table, "List all foreign keys but provides just the columns linked by the foreign key. To know the table of these columns, see in TAP_SCHEMA.keys using the key_id.", null);
				key_columns.addColumn("key_id", new TAPType(TAPDatatype.VARCHAR), "unique key identifier", null, null, null, true, true, true);
				key_columns.addColumn("from_column", new TAPType(TAPDatatype.VARCHAR), "key column name in the from_table", null, null, null, false, false, true);
				key_columns.addColumn("target_column", new TAPType(TAPDatatype.VARCHAR), "key column name in the target_table", null, null, null, false, false, true);
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
