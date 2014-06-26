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

import tap.TAPExecutionReport;
import tap.TAPJob;
import tap.TAPException;
import uws.job.Result;

import java.io.OutputStream;
import java.io.PrintWriter;

import cds.savot.writer.SavotWriter;
import tap.ServiceConnection;
import tap.metadata.TAPColumn;
import tap.metadata.VotType;
import adql.db.DBColumn;

/**
 * <p>Formats the given type of query result in VOTable.</p>
 * <p>
 * 	This abstract class is only able to format the skeleton of the VOTable.
 * 	However, it also provides useful methods to format field metadata and field value (including NULL values).
 * </p>
 * <p>
 * 	Attributes of the VOTable node are by default set by this class but can be overridden if necessary thanks to the corresponding class attributes:
 * 	{@link #votTableVersion}, {@link #xmlnsXsi}, {@link #xsiNoNamespaceSchemaLocation}, {@link #xsiSchemaLocation} and
 *  {@link #xmlns}.
 * </p>
 * <p>
 *	When overridding this class, you must implement {@link #writeMetadata(Object, PrintWriter, TAPJob)} and
 *	{@link #writeData(Object, DBColumn[], OutputStream, TAPJob)}.
 *	Both are called by {@link #writeResult(Object, OutputStream, TAPJob)}. Finally you will also have to implement
 *	{@link #writeResult(Object, TAPJob)}, which must format the given result into a VOTable saved in some way accessible
 *	through the returned {@link Result}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 *
 * @param <R>	Type of the result to format in VOTable (i.e. {@link java.sql.ResultSet}).
 * 
 * @see ResultSet2VotableFormatter
 */
public abstract class VOTableFormat< R > implements OutputFormat<R> {

	/** Indicates whether a format report (start and end date/time) must be printed in the log output.  */
	private boolean logFormatReport;

	/** The {@link ServiceConnection} to use (for the log and to have some information about the service (particularly: name, description). */
	protected final ServiceConnection<R> service;

	protected String votTableVersion = "1.2";
	protected String xmlnsXsi = "http://www.w3.org/2001/XMLSchema-instance";
	protected String xsiSchemaLocation = "http://www.ivoa.net/xml/VOTable/v1.2";
	protected String xsiNoNamespaceSchemaLocation = null;
	protected String xmlns = "http://www.ivoa.net/xml/VOTable/v1.2";

	/**
	 * Creates a VOTable formatter without format report.
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 * 
	 * @see #VOTableFormat(ServiceConnection, boolean)
	 */
	public VOTableFormat(final ServiceConnection<R> service) throws NullPointerException{
		this(service, false);
	}

	/**
	 * Creates a VOTable formatter.
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * @param logFormatReport		<code>true</code> to append a format report (start and end date/time) in the log output, <code>false</code> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public VOTableFormat(final ServiceConnection<R> service, final boolean logFormatReport) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("The given service connection is NULL !");
		this.service = service;
		this.logFormatReport = logFormatReport;
	}

	public final String getMimeType(){
		return "text/xml";
	}

	public final String getShortMimeType(){
		return "votable";
	}

	public String getDescription(){
		return null;
	}

	public String getFileExtension(){
		return "xml";
	}

	/**
	 * <p>The skeleton of the resulting VOTable is written in this method:</p>
	 * <ul>
	 * 	<li>&lt;?xml version="1.0" encoding="UTF-8"&gt;</li>
	 *	<li><i>{@link #writeHeader(PrintWriter, TAPJob)}</i></li>
	 *	<li>&lt;TABLE&gt;</li>
	 *	<li>&lt;DATA&gt;</li>
	 *	<li><i>{@link #writeData(Object, DBColumn[], OutputStream, TAPJob)}</i></li>
	 *	<li>&lt;/DATA&gt;</li>
	 *	<li><i>if (nbRows >= job.getMaxRec()) </i>&lt;INFO name="QUERY_STATUS" value="OVERFLOW" /&gt;</li>
	 *	<li>&lt;/RESOURCE&gt;</li>
	 *	<li>&lt;/VOTABLE&gt;</li>
	 * </ul>
	 * 
	 * @see tap.formatter.OutputFormat#writeResult(Object, OutputStream, TAPExecutionReport)
	 */
	public final void writeResult(final R queryResult, final OutputStream output, final TAPExecutionReport execReport, final Thread thread) throws TAPException, InterruptedException{
		try{
			long start = System.currentTimeMillis();

			PrintWriter out = new PrintWriter(output);
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writeHeader(out, execReport);
			out.println("\t\t<TABLE>");
			DBColumn[] columns = writeMetadata(queryResult, out, execReport, thread);
			out.println("\t\t\t<DATA>");
			out.flush();
			int nbRows = writeData(queryResult, columns, output, execReport, thread);
			output.flush();
			out.println("\t\t\t</DATA>");
			out.println("\t\t</TABLE>");
			// OVERFLOW ?
			if (execReport.parameters.getMaxRec() > 0 && nbRows >= execReport.parameters.getMaxRec())
				out.println("\t\t<INFO name=\"QUERY_STATUS\" value=\"OVERFLOW\" />");
			out.println("\t</RESOURCE>");
			out.println("</VOTABLE>");
			out.flush();

			if (logFormatReport)
				service.getLogger().info("JOB " + execReport.jobID + " WRITTEN\tResult formatted (in VOTable ; " + nbRows + " rows ; " + columns.length + " columns) in " + (System.currentTimeMillis() - start) + " ms !");
		}catch(IOException ioe){
			throw new TAPException("Error while writing a query result in VOTable !", ioe);
		}
	}

	/**
	 * <p>Writes the root node of the VOTable: &lt;VOTABLE&gt;.</p>
	 * <p>
	 * 	Attributes of this node are written thanks to their corresponding attributes in this class:
	 * 	{@link #votTableVersion}, {@link #xmlnsXsi}, {@link #xsiNoNamespaceSchemaLocation}, {@link #xsiSchemaLocation} and {@link #xmlns}.
	 * 	They are written only if different from <code>null</code>.
	 * </p>
	 * 
	 * @param output		Writer in which the root node must be written.
	 * @param execReport	The report of the query execution.
	 * 
	 * @throws IOException	If there is an error while writing the root node in the given Writer.
	 * @throws TAPException	If there is any other error (by default: never happen).
	 */
	protected void writeHeader(final PrintWriter output, final TAPExecutionReport execReport) throws IOException, TAPException{
		StringBuffer strBuf = new StringBuffer("<VOTABLE");
		if (votTableVersion != null)
			strBuf.append(" version=\"").append(SavotWriter.encodeAttribute(votTableVersion)).append('\"');
		if (xmlnsXsi != null)
			strBuf.append(" xmlns:xsi=\"").append(SavotWriter.encodeAttribute(xmlnsXsi)).append('\"');
		if (xsiSchemaLocation != null)
			strBuf.append(" xsi:schemaLocation=\"").append(SavotWriter.encodeAttribute(xsiSchemaLocation)).append('\"');
		if (xsiNoNamespaceSchemaLocation != null)
			strBuf.append(" xsi:noNamespaceSchemaLocation=\"").append(SavotWriter.encodeAttribute(xsiNoNamespaceSchemaLocation)).append('\"');
		if (xmlns != null)
			strBuf.append(" xmlns=\"").append(SavotWriter.encodeAttribute(xmlns)).append('\"');
		strBuf.append('>');
		output.println(strBuf);

		output.println("\t<RESOURCE type=\"results\">");

		// INFO items:
		output.println("\t\t<INFO name=\"QUERY_STATUS\" value=\"OK\" />");
		output.println("\t\t<INFO name=\"PROVIDER\" value=\"" + ((service.getProviderName() == null) ? "" : SavotWriter.encodeAttribute(service.getProviderName())) + "\">" + ((service.getProviderDescription() == null) ? "" : SavotWriter.encodeElement(service.getProviderDescription())) + "</INFO>");
		output.println("\t\t<INFO name=\"QUERY\"><![CDATA[" + execReport.parameters.getQuery() + "]]></INFO>");
	}

	/**
	 * <p>Writes fields' metadata of the given query result in the given Writer.</p>
	 * <p><b><u>Important:</u> To write write metadata of a given field you can use {@link #writeFieldMeta(TAPColumn, PrintWriter)}.</b></p>
	 * 
	 * @param queryResult	The query result from whose fields' metadata must be written.
	 * @param output		Writer in which fields' metadata must be written.
	 * @param execReport	The report of the query execution.
	 * @param thread		The thread which asked for the result writting.
	 * 
	 * @return				Extracted field's metadata.
	 * 
	 * @throws IOException				If there is an error while writing the metadata in the given Writer.
	 * @throws TAPException				If there is any other error.
	 * @throws InterruptedException		If the given thread has been interrupted.
	 */
	protected abstract DBColumn[] writeMetadata(final R queryResult, final PrintWriter output, final TAPExecutionReport execReport, final Thread thread) throws IOException, TAPException, InterruptedException;

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
	protected void writeFieldMeta(TAPColumn col, PrintWriter out) throws IOException, TAPException{
		StringBuffer fieldline = new StringBuffer("\t\t\t");

		fieldline.append("<FIELD ID=").append('"').append(SavotWriter.encodeAttribute(col.getADQLName())).append('"');
		fieldline.append(" name=").append('"').append(SavotWriter.encodeAttribute(col.getADQLName())).append('"');

		VotType type = col.getVotType();
		String nullVal = getNullValue(type.datatype), description = null;

		fieldline.append(' ').append(type.toString());

		if (col.getUcd() != null && col.getUcd().length() > 0)
			fieldline.append(" ucd=").append('"').append(SavotWriter.encodeAttribute(col.getUcd())).append('"');

		if (col.getUtype() != null && col.getUtype().length() > 0)
			fieldline.append(" utype=").append('"').append(SavotWriter.encodeAttribute(col.getUtype())).append('"');

		if (col.getUnit() != null && col.getUnit().length() > 0)
			fieldline.append(" unit=").append('"').append(SavotWriter.encodeAttribute(col.getUnit())).append('"');

		if (col.getDescription() != null && !col.getDescription().trim().isEmpty())
			description = col.getDescription().trim();
		else
			description = null;

		if (nullVal != null || description != null){
			fieldline.append(">\n");
			if (nullVal != null)
				fieldline.append("<VALUES null=\"" + nullVal + "\" />\n");
			if (description != null)
				fieldline.append("<DESCRIPTION>").append(SavotWriter.encodeElement(description)).append("</DESCRIPTION>\n");
			fieldline.append("</FIELD>");
			out.println(fieldline);
		}else{
			fieldline.append("/>");
			out.println(fieldline);
		}
	}

	/**
	 * <p>Writes the data of the given query result in the given OutputStream.</p>
	 * <p><b><u>Important:</u> To write a field value you can use {@link #writeFieldValue(Object, DBColumn, OutputStream)}.</b></p>
	 * 
	 * @param queryResult		The query result which contains the data to write.
	 * @param selectedColumns	The columns selected by the query.
	 * @param output			The stream in which the data must be written.
	 * @param execReport		The report of the query execution.
	 * @param thread		The thread which asked for the result writting.
	 * 
	 * @return					The number of written rows. (<i>note: if this number is greater than the value of MAXREC: OVERFLOW</i>)
	 * 
	 * @throws IOException				If there is an error while writing the data in the given stream.
	 * @throws TAPException				If there is any other error.
	 * @throws InterruptedException		If the given thread has been interrupted.
	 */
	protected abstract int writeData(final R queryResult, final DBColumn[] selectedColumns, final OutputStream output, final TAPExecutionReport execReport, final Thread thread) throws IOException, TAPException, InterruptedException;

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
	 * @param output			The stream in which the field value must be written.
	 * 
	 * @throws IOException		If there is an error while writing the given field value in the given stream.
	 * @throws TAPException		If there is any other error (by default: never happen).
	 */
	protected void writeFieldValue(final Object value, final DBColumn column, final OutputStream output) throws IOException, TAPException{
		String fieldValue = (value == null) ? null : value.toString();
		if (fieldValue == null && column instanceof TAPColumn)
			fieldValue = getNullValue(((TAPColumn)column).getVotType().datatype);
		if (fieldValue != null)
			output.write(SavotWriter.encodeElement(fieldValue).getBytes());
	}

	/**
	 * <p>Gets the NULL value corresponding to the given datatype:</p>
	 * <ul>
	 * 	<li>for <code>int</code>: {@link Integer#MIN_VALUE}</li>
	 * 	<li>for <code>short</code>: {@link Short#MIN_VALUE}</li>
	 * 	<li>for <code>long</code>: {@link Long#MIN_VALUE}</li>
	 * 	<li>for anything else, <code>null</code> will be returned.</li>
	 * </ul>
	 * 
	 * @param datatype	A VOTable datatype.
	 * 
	 * @return			The corresponding NULL value, or <code>null</code> if there is none.
	 */
	public static final String getNullValue(String datatype){
		if (datatype == null)
			return null;

		datatype = datatype.trim().toLowerCase();

		if (datatype.equals("short"))
			return "" + Short.MIN_VALUE;
		else if (datatype.equals("int"))
			return "" + Integer.MIN_VALUE;
		else if (datatype.equals("long"))
			return "" + Long.MIN_VALUE;
		else
			return null;
	}
}
