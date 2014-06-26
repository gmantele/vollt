package tap.formatter;

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
import java.io.OutputStream;
import java.io.PrintWriter;

import org.json.JSONException;
import org.json.JSONWriter;

import cds.savot.writer.SavotWriter;
import adql.db.DBColumn;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTypes;

public abstract class JSONFormat< R > implements OutputFormat<R> {

	/** Indicates whether a format report (start and end date/time) must be printed in the log output.  */
	private boolean logFormatReport;

	/** The {@link ServiceConnection} to use (for the log and to have some information about the service (particularly: name, description). */
	protected final ServiceConnection<R> service;

	public JSONFormat(final ServiceConnection<R> service){
		this(service, false);
	}

	public JSONFormat(final ServiceConnection<R> service, final boolean logFormatReport){
		this.service = service;
		this.logFormatReport = logFormatReport;
	}

	public String getMimeType(){
		return "application/json";
	}

	public String getShortMimeType(){
		return "json";
	}

	public String getDescription(){
		return null;
	}

	public String getFileExtension(){
		return "json";
	}

	@Override
	public void writeResult(R queryResult, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, InterruptedException{
		try{
			long start = System.currentTimeMillis();

			PrintWriter writer = new PrintWriter(output);
			JSONWriter out = new JSONWriter(writer);

			out.object();

			out.key("metadata");
			DBColumn[] columns = writeMetadata(queryResult, out, execReport, thread);

			writer.flush();

			out.key("data");
			int nbRows = writeData(queryResult, columns, out, execReport, thread);

			out.endObject();
			writer.flush();

			if (logFormatReport)
				service.getLogger().info("JOB " + execReport.jobID + " WRITTEN\tResult formatted (in JSON ; " + nbRows + " rows ; " + columns.length + " columns) in " + (System.currentTimeMillis() - start) + " ms !");
		}catch(JSONException je){
			throw new TAPException("Error while writing a query result in JSON !", je);
		}catch(IOException ioe){
			throw new TAPException("Error while writing a query result in JSON !", ioe);
		}
	}

	protected abstract DBColumn[] writeMetadata(R queryResult, JSONWriter out, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException, JSONException;

	/**
	 * <p>Formats in a VOTable field and writes the given {@link TAPColumn} in the given Writer.</p>
	 * 
	 * <p><i><u>Note:</u> If the VOTable datatype is <code>int</code>, <code>short</code> or <code>long</code> a NULL values is set by adding a node VALUES: &lt;VALUES null="..." /&gt;</i></p>
	 * 
	 * @param col				The column metadata to format into a VOTable field.
	 * @param out				The stream in which the formatted column metadata must be written.
	 * 
	 * @throws IOException		If there is an error while writing the field metadata.
	 * @throws TAPException		If there is any other error (by default: never happen).
	 */
	protected void writeFieldMeta(TAPColumn tapCol, JSONWriter out) throws IOException, TAPException, JSONException{
		out.object();

		out.key("name").value(tapCol.getName());

		if (tapCol.getDescription() != null && tapCol.getDescription().trim().length() > 0)
			out.key("description").value(tapCol.getDescription());

		out.key("datatype").value(tapCol.getVotType().datatype);

		int arraysize = tapCol.getVotType().arraysize;
		if (arraysize == TAPTypes.STAR_SIZE)
			out.key("arraysize").value("*");
		else if (arraysize > 0)
			out.key("arraysize").value(arraysize);

		if (tapCol.getVotType().xtype != null)
			out.key("xtype").value(tapCol.getVotType().xtype);

		if (tapCol.getUnit() != null && tapCol.getUnit().length() > 0)
			out.key("unit").value(tapCol.getUnit());

		if (tapCol.getUcd() != null && tapCol.getUcd().length() > 0)
			out.key("ucd").value(tapCol.getUcd());

		if (tapCol.getUtype() != null && tapCol.getUtype().length() > 0)
			out.key("utype").value(tapCol.getUtype());

		out.endObject();
	}

	protected abstract int writeData(R queryResult, DBColumn[] selectedColumns, JSONWriter out, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException, JSONException;

	/**
	 * <p>Writes the given field value in the given OutputStream.</p>
	 * 
	 * <p>
	 * 	The given value will be encoded as an XML element (see {@link SavotWriter#encodeElement(String)}.
	 * 	Besides, if the given value is <code>null</code> and if the column datatype is <code>int</code>,
	 * 	<code>short</code> or <code>long</code>, the NULL values declared in the field metadata will be written.</p>
	 * 
	 * @param value				The value to write.
	 * @param column			The corresponding column metadata.
	 * @param out				The stream in which the field value must be written.
	 * 
	 * @throws IOException		If there is an error while writing the given field value in the given stream.
	 * @throws TAPException		If there is any other error (by default: never happen).
	 */
	protected void writeFieldValue(final Object value, final DBColumn column, final JSONWriter out) throws IOException, TAPException, JSONException{
		if (value instanceof Double && (((Double)value).isNaN() || ((Double)value).isInfinite()))
			out.value((Object)null);
		else if (value instanceof Float && (((Float)value).isNaN() || ((Float)value).isInfinite()))
			out.value((Object)null);
		else
			out.value(value);
	}
}
