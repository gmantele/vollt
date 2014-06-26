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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
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

import adql.db.DBTable;
import tap.resource.Capabilities;
import tap.resource.TAPResource;
import tap.resource.VOSIResource;

public class TAPMetadata implements Iterable<TAPSchema>, VOSIResource, TAPResource {

	public static final String RESOURCE_NAME = "tables";

	protected final Map<String,TAPSchema> schemas;
	protected String accessURL = getName();

	public TAPMetadata(){
		schemas = new HashMap<String,TAPSchema>();
	}

	public final void addSchema(TAPSchema s){
		if (s != null && s.getName() != null)
			schemas.put(s.getName(), s);
	}

	public TAPSchema addSchema(String schemaName){
		if (schemaName == null)
			return null;

		TAPSchema s = new TAPSchema(schemaName);
		addSchema(s);
		return s;
	}

	public TAPSchema addSchema(String schemaName, String description, String utype){
		if (schemaName == null)
			return null;

		TAPSchema s = new TAPSchema(schemaName, description, utype);
		addSchema(s);
		return s;
	}

	public final boolean hasSchema(String schemaName){
		if (schemaName == null)
			return false;
		else
			return schemas.containsKey(schemaName);
	}

	public final TAPSchema getSchema(String schemaName){
		if (schemaName == null)
			return null;
		else
			return schemas.get(schemaName);
	}

	public final int getNbSchemas(){
		return schemas.size();
	}

	public final boolean isEmpty(){
		return schemas.isEmpty();
	}

	public final TAPSchema removeSchema(String schemaName){
		if (schemaName == null)
			return null;
		else
			return schemas.remove(schemaName);
	}

	public final void removeAllSchemas(){
		schemas.clear();
	}

	@Override
	public final Iterator<TAPSchema> iterator(){
		return schemas.values().iterator();
	}

	public Iterator<TAPTable> getTables(){
		return new TAPTableIterator(this);
	}

	public boolean hasTable(String schemaName, String tableName){
		TAPSchema s = getSchema(schemaName);
		if (s != null)
			return s.hasTable(tableName);
		else
			return false;
	}

	public boolean hasTable(String tableName){
		for(TAPSchema s : this)
			if (s.hasTable(tableName))
				return true;
		return false;
	}

	//		@Override
	public TAPTable getTable(String schemaName, String tableName){
		TAPSchema s = getSchema(schemaName);
		if (s != null)
			return s.getTable(tableName);
		else
			return null;
	}

	//		@Override
	public ArrayList<DBTable> getTable(String tableName){
		ArrayList<DBTable> tables = new ArrayList<DBTable>();
		for(TAPSchema s : this)
			if (s.hasTable(tableName))
				tables.add(s.getTable(tableName));
		return tables;
	}

	public int getNbTables(){
		int nbTables = 0;
		for(TAPSchema s : this)
			nbTables += s.getNbTables();
		return nbTables;
	}

	public static class TAPTableIterator implements Iterator<TAPTable> {
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
	public void init(ServletConfig config) throws ServletException{
		;
	}

	@Override
	public void destroy(){
		;
	}

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

	private void writeSchema(TAPSchema s, PrintWriter writer) throws IOException{
		final String prefix = "\t\t";
		writer.println("\t<schema>");

		writeAtt(prefix, "name", s.getName(), writer);
		writeAtt(prefix, "description", s.getDescription(), writer);
		writeAtt(prefix, "utype", s.getUtype(), writer);

		for(TAPTable t : s)
			writeTable(t, writer);

		writer.println("\t</schema>");
	}

	private void writeTable(TAPTable t, PrintWriter writer) throws IOException{
		final String prefix = "\t\t\t";

		writer.print("\t\t<table type=\"");
		writer.print(t.getType().equalsIgnoreCase("table") ? "base_table" : t.getType());
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

	private void writeColumn(TAPColumn c, PrintWriter writer) throws IOException{
		final String prefix = "\t\t\t\t";

		writer.print("\t\t\t<column std=\"");
		writer.print(c.isStd());
		writer.println("\">");

		writeAtt(prefix, "name", c.getName(), writer);
		writeAtt(prefix, "description", c.getDescription(), writer);
		writeAtt(prefix, "unit", c.getUnit(), writer);
		writeAtt(prefix, "utype", c.getUtype(), writer);
		writeAtt(prefix, "ucd", c.getUcd(), writer);

		if (c.getDatatype() != null){
			writer.print(prefix);
			writer.print("<dataType xsi:type=\"vod:TAPType\"");
			if (c.getArraySize() >= 0){
				writer.print(" size=\"");
				writer.print(c.getArraySize());
				writer.print("\"");
			}
			writer.print('>');
			writer.print(c.getDatatype().toUpperCase());
			writer.println("</dataType>");
		}

		if (c.isIndexed())
			writeAtt(prefix, "flag", "indexed", writer);
		if (c.isPrincipal())
			writeAtt(prefix, "flag", "primary", writer);

		writer.println("\t\t\t</column>");
	}

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

	private void writeAtt(String prefix, String attributeName, String attributeValue, PrintWriter writer) throws IOException{
		if (attributeValue != null){
			StringBuffer xml = new StringBuffer(prefix);
			xml.append('<').append(attributeName).append('>').append(attributeValue).append("</").append(attributeName).append('>');
			writer.println(xml.toString());
		}
	}

}
