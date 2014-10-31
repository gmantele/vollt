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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.json.JSONException;
import org.json.JSONWriter;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;
import tap.metadata.TAPColumn;
import tap.metadata.VotType;
import uws.service.log.UWSLog.LogLevel;
import adql.db.DBColumn;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;

/**
 * Format any given query (table) result into JSON.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (10/2014)
 */
public class JSONFormat implements OutputFormat {

	/** Indicates whether a format report (start and end date/time) must be printed in the log output.  */
	private boolean logFormatReport;

	/** The {@link ServiceConnection} to use (for the log and to have some information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/**
	 * <p>Build a JSON formatter.</p>
	 * 
	 * <p><i>note: The built formatter will not write a log entry each time a result is written.
	 * However if you want this behavior you must you {@link #JSONFormat(ServiceConnection, boolean)}.</i></p>
	 * 
	 * @param service	Description of the TAP service.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public JSONFormat(final ServiceConnection service) throws NullPointerException{
		this(service, true);
	}

	/**
	 * Build a JSON formatter.
	 * 
	 * @param service			Description of the TAP service.
	 * @param logFormatReport	<i>true</i> to write a log entry (with nb rows and columns + writing duration) each time a result is written, <i>false</i> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public JSONFormat(final ServiceConnection service, final boolean logFormatReport) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("The given service connection is NULL!");

		this.service = service;
		this.logFormatReport = logFormatReport;
	}

	@Override
	public String getMimeType(){
		return "application/json";
	}

	@Override
	public String getShortMimeType(){
		return "json";
	}

	@Override
	public String getDescription(){
		return null;
	}

	@Override
	public String getFileExtension(){
		return "json";
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, InterruptedException{
		try{
			long start = System.currentTimeMillis();

			// Prepare the output stream for JSON:
			PrintWriter writer = new PrintWriter(output);
			JSONWriter out = new JSONWriter(writer);

			// {
			out.object();

			// "metadata": [...]
			out.key("metadata");

			// Write metadata part:
			DBColumn[] columns = writeMetadata(result, out, execReport, thread);

			writer.flush();

			// "data": [...]
			out.key("data");

			// Write the data part:
			int nbRows = writeData(result, columns, out, execReport, thread);

			// }
			out.endObject();
			writer.flush();

			// Report stats about the result writing:
			if (logFormatReport)
				service.getLogger().logTAP(LogLevel.INFO, execReport, "FORMAT", "Result formatted (in JSON ; " + nbRows + " rows ; " + columns.length + " columns) in " + (System.currentTimeMillis() - start) + "ms!", null);

		}catch(JSONException je){
			throw new TAPException("Error while writing a query result in JSON!", je);
		}catch(IOException ioe){
			throw new TAPException("Error while writing a query result in JSON!", ioe);
		}
	}

	/**
	 * Write the whole metadata part of the JSON file.
	 * 
	 * @param result		Result to write later (but it contains also metadata that was extracted from the result itself).
	 * @param out			Output stream in which the metadata must be written.
	 * @param execReport	Execution report (which contains the metadata extracted/guessed from the ADQL query).
	 * @param thread		Thread which has asked for this formatting (it must be used in order to test the {@link Thread#isInterrupted()} flag and so interrupt everything if need).
	 * 
	 * @return	All the written metadata.
	 * 
	 * @throws IOException				If there is an error while writing something in the output stream.
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws JSONException			If there is an error while formatting something in JSON.
	 * @throws TAPException				If any other error occurs.
	 * 
	 * @see #getValidColMeta(DBColumn, TAPColumn)
	 */
	protected DBColumn[] writeMetadata(TableIterator result, JSONWriter out, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException, JSONException{
		out.array();

		// Get the metadata extracted/guesses from the ADQL query:
		DBColumn[] columnsFromQuery = execReport.resultingColumns;

		// Get the metadata extracted from the result:
		TAPColumn[] columnsFromResult = result.getMetadata();

		int indField = 0;
		if (columnsFromQuery != null){

			// For each column:
			for(DBColumn field : columnsFromQuery){

				// Try to build/get appropriate metadata for this field/column:
				TAPColumn colFromResult = (columnsFromResult != null && indField < columnsFromResult.length) ? columnsFromResult[indField] : null;
				TAPColumn tapCol = getValidColMeta(field, colFromResult);

				// Ensure these metadata are well returned at the end of this function:
				columnsFromQuery[indField] = tapCol;

				// Write the field/column metadata in the JSON output:
				writeFieldMeta(tapCol, out);
				indField++;

				if (thread.isInterrupted())
					throw new InterruptedException();
			}
		}

		out.endArray();
		return columnsFromQuery;
	}

	/**
	 * Try to get or otherwise to build appropriate metadata using those extracted from the ADQL query and those extracted from the result.
	 * 
	 * @param typeFromQuery		Metadata extracted/guessed from the ADQL query.
	 * @param typeFromResult	Metadata extracted/guessed from the result.
	 * 
	 * @return	The most appropriate metadata.
	 */
	protected TAPColumn getValidColMeta(final DBColumn typeFromQuery, final TAPColumn typeFromResult){
		if (typeFromQuery != null && typeFromQuery instanceof TAPColumn)
			return (TAPColumn)typeFromQuery;
		else if (typeFromResult != null){
			if (typeFromQuery != null)
				return (TAPColumn)typeFromResult.copy(typeFromQuery.getDBName(), typeFromQuery.getADQLName(), null);
			else
				return (TAPColumn)typeFromResult.copy();
		}else
			return new TAPColumn((typeFromQuery != null) ? typeFromQuery.getADQLName() : "?", new DBType(DBDatatype.VARCHAR), "?");
	}

	/**
	 * Formats in JSON and writes the given {@link TAPColumn} in the given output.
	 * 
	 * @param col				The column metadata to format/write in JSON.
	 * @param out				The stream in which the formatted column metadata must be written.
	 * 
	 * @throws IOException		If there is an error while writing the field metadata.
	 * @throws JSONException	If there is an error while formatting something in JSON format.
	 * @throws TAPException		If there is any other error (by default: never happen).
	 */
	protected void writeFieldMeta(TAPColumn tapCol, JSONWriter out) throws IOException, TAPException, JSONException{
		// {
		out.object();

		// "name": "..."
		out.key("name").value(tapCol.getADQLName());

		// "description": "..." (if any)
		if (tapCol.getDescription() != null && tapCol.getDescription().trim().length() > 0)
			out.key("description").value(tapCol.getDescription());

		// "datatype": "..."
		VotType votType = new VotType(tapCol.getDatatype());
		out.key("datatype").value(votType.datatype);

		// "arraysize": "..." (if any)
		if (votType.arraysize != null)
			out.key("arraysize").value(votType.arraysize);

		// "xtype": "..." (if any)
		if (votType.xtype != null)
			out.key("xtype").value(votType.xtype);

		// "unit": "..." (if any)
		if (tapCol.getUnit() != null && tapCol.getUnit().length() > 0)
			out.key("unit").value(tapCol.getUnit());

		// "ucd": "..." (if any)
		if (tapCol.getUcd() != null && tapCol.getUcd().length() > 0)
			out.key("ucd").value(tapCol.getUcd());

		// "utype": "..." (if any)
		if (tapCol.getUtype() != null && tapCol.getUtype().length() > 0)
			out.key("utype").value(tapCol.getUtype());

		// }
		out.endObject();
	}

	/**
	 * Write the whole data part of the JSON file.
	 * 
	 * @param result			Result to write.	
	 * @param selectedColumns	All columns' metadata.
	 * @param out				Output stream in which the data must be written.
	 * @param execReport		Execution report (which contains the maximum allowed number of records to output).
	 * @param thread			Thread which has asked for this formatting (it must be used in order to test the {@link Thread#isInterrupted()} flag and so interrupt everything if need).
	 * 
	 * @return	The number of written result rows. (<i>note: if this number is greater than the value of MAXREC: OVERFLOW</i>)
	 * 
	 * @throws IOException				If there is an error while writing something in the output stream.
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws JSONException			If there is an error while formatting something in JSON.
	 * @throws TAPException				If any other error occurs.
	 */
	protected int writeData(TableIterator result, DBColumn[] selectedColumns, JSONWriter out, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException, JSONException{
		// [
		out.array();

		int nbRows = 0;
		while(result.nextRow()){
			// Deal with OVERFLOW, if needed:
			if (execReport.parameters.getMaxRec() > 0 && nbRows >= execReport.parameters.getMaxRec())
				break;

			// [
			out.array();
			int indCol = 0;
			while(result.hasNextCol()){
				// ...
				writeFieldValue(result.nextCol(), selectedColumns[indCol++], out);

				if (thread.isInterrupted())
					throw new InterruptedException();
			}
			// ]
			out.endArray();
			nbRows++;

			if (thread.isInterrupted())
				throw new InterruptedException();
		}

		// ]
		out.endArray();
		return nbRows;
	}

	/**
	 * <p>Writes the given field value in JSON and into the given output.</p>
	 * 
	 * <p><i>note: special numeric values NaN and Inf (double or float) will be written as NULL values.</i></p>
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
