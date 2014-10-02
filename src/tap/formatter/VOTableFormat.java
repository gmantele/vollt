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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.DataReadException;
import tap.data.TableIterator;
import tap.error.DefaultTAPErrorWriter;
import tap.metadata.TAPColumn;
import tap.metadata.TAPType;
import tap.metadata.TAPType.TAPDatatype;
import tap.metadata.VotType;
import tap.metadata.VotType.VotDatatype;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOTableVersion;
import uws.service.log.UWSLog.LogLevel;
import adql.db.DBColumn;

/**
 * <p>Format any given query (table) result into VOTable.</p>
 * 
 * <p>
 * 	Format and version of the resulting VOTable can be provided in parameters at the construction time.
 * 	This formatter is using STIL. So all formats and versions managed by STIL are also here.
 * 	Basically, you have the following formats: TABLEDATA, BINARY, BINARY2 (only when using VOTable v1.3) and FITS.
 * 	The versions are: 1.0, 1.1, 1.2 and 1.3.
 * </p>
 * 
 * <p>Note: The MIME type is automatically set in function of the given VOTable serialization:</p>
 * <ul>
 * 	<li><b>none or unknown</b>: equivalent to BINARY</li>
 * 	<li><b>BINARY</b>:          "application/x-votable+xml" = "votable"</li>
 * 	<li><b>BINARY2</b>:         "application/x-votable+xml;serialization=BINARY2" = "votable/b2"</li>
 * 	<li><b>TABLEDATA</b>:       "application/x-votable+xml;serialization=TABLEDATA" = "votable/td"</li>
 * 	<li><b>FITS</b>:            "application/x-votable+xml;serialization=FITS" = "votable/fits"</li>
 * </ul>
 * <p>It is however possible to change these default values thanks to {@link #setMimeType(String, String)}.</p>
 * 
 * <p>In addition of the INFO elements for QUERY_STATUS="OK" and QUERY_STATUS="OVERFLOW", two additional INFO elements are written:</p>
 * <ul>
 * 	<li>PROVIDER = {@link ServiceConnection#getProviderName()} and {@link ServiceConnection#getProviderDescription()}</li>
 * 	<li>QUERY = the ADQL query at the origin of this result.</li>
 * </ul>
 * 
 * <p>
 * 	Furthermore, this formatter provides a function to format an error in VOTable: {@link #writeError(String, Map, PrintWriter)}.
 * 	This is useful for TAP which requires to return in VOTable any error that occurs while any operation.
 * 	<i>See {@link DefaultTAPErrorWriter} for more details.</i>
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (10/2014)
 */
public class VOTableFormat implements OutputFormat {

	/** Indicates whether a format report (start and end date/time) must be printed in the log output.  */
	private boolean logFormatReport;

	/** The {@link ServiceConnection} to use (for the log and to have some information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/** Format of the VOTable data part in which data must be formatted. Possible values are: TABLEDATA, BINARY, BINARY2 or FITS. By default, it is set to BINARY. */
	protected final DataFormat votFormat;

	/** VOTable version in which table data must be formatted. By default, it is set to v13. */
	protected final VOTableVersion votVersion;

	/** MIME type associated with this format. */
	protected String mimeType;

	/** Short form of the MIME type associated with this format. */
	protected String shortMimeType;

	/**
	 * <p>Creates a VOTable formatter.</p>
	 * 
	 * <p><i>Note:
	 * 	The MIME type is automatically set to "application/x-votable+xml" = "votable".
	 * 	It is however possible to change this default value thanks to {@link #setMimeType(String, String)}.
	 * </i></p>
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public VOTableFormat(final ServiceConnection service) throws NullPointerException{
		this(service, true);
	}

	/**
	 * <p>Creates a VOTable formatter.</p>
	 * 
	 * <i>Note: The MIME type is automatically set in function of the given VOTable serialization:</i>
	 * <ul>
	 * 	<li><i><b>none or unknown</b>: equivalent to BINARY</i></li>
	 * 	<li><i><b>BINARY</b>:          "application/x-votable+xml" = "votable"</i></li>
	 * 	<li><i><b>BINARY2</b>:         "application/x-votable+xml;serialization=BINARY2" = "votable/b2"</i></li>
	 * 	<li><i><b>TABLEDATA</b>:       "application/x-votable+xml;serialization=TABLEDATA" = "votable/td"</i></li>
	 * 	<li><i><b>FITS</b>:            "application/x-votable+xml;serialization=FITS" = "votable/fits"</i></li>
	 * </ul>
	 * <p><i>It is however possible to change these default values thanks to {@link #setMimeType(String, String)}.</i></p>
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * @param votFormat				Serialization of the VOTable data part. (TABLEDATA, BINARY, BINARY2 or FITS).
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public VOTableFormat(final ServiceConnection service, final DataFormat votFormat) throws NullPointerException{
		this(service, votFormat, null, true);
	}

	/**
	 * <p>Creates a VOTable formatter.</p>
	 * 
	 * <i>Note: The MIME type is automatically set in function of the given VOTable serialization:</i>
	 * <ul>
	 * 	<li><i><b>none or unknown</b>: equivalent to BINARY</i></li>
	 * 	<li><i><b>BINARY</b>:          "application/x-votable+xml" = "votable"</i></li>
	 * 	<li><i><b>BINARY2</b>:         "application/x-votable+xml;serialization=BINARY2" = "votable/b2"</i></li>
	 * 	<li><i><b>TABLEDATA</b>:       "application/x-votable+xml;serialization=TABLEDATA" = "votable/td"</i></li>
	 * 	<li><i><b>FITS</b>:            "application/x-votable+xml;serialization=FITS" = "votable/fits"</i></li>
	 * </ul>
	 * <p><i>It is however possible to change these default values thanks to {@link #setMimeType(String, String)}.</i></p>
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * @param votFormat				Serialization of the VOTable data part. (TABLEDATA, BINARY, BINARY2 or FITS).
	 * @param votVersion			Version of the resulting VOTable.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public VOTableFormat(final ServiceConnection service, final DataFormat votFormat, final VOTableVersion votVersion) throws NullPointerException{
		this(service, votFormat, votVersion, true);
	}

	/**
	 * <p>Creates a VOTable formatter.</p>
	 * 
	 * <p><i>Note:
	 * 	The MIME type is automatically set to "application/x-votable+xml" = "votable".
	 * 	It is however possible to change this default value thanks to {@link #setMimeType(String, String)}.
	 * </i></p>
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * @param logFormatReport		<code>true</code> to append a format report (start and end date/time) in the log output, <code>false</code> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public VOTableFormat(final ServiceConnection service, final boolean logFormatReport) throws NullPointerException{
		this(service, null, null, logFormatReport);
	}

	/**
	 * <p>Creates a VOTable formatter.</p>
	 * 
	 * <i>Note: The MIME type is automatically set in function of the given VOTable serialization:</i>
	 * <ul>
	 * 	<li><i><b>none or unknown</b>: equivalent to BINARY</i></li>
	 * 	<li><i><b>BINARY</b>:          "application/x-votable+xml" = "votable"</i></li>
	 * 	<li><i><b>BINARY2</b>:         "application/x-votable+xml;serialization=BINARY2" = "votable/b2"</i></li>
	 * 	<li><i><b>TABLEDATA</b>:       "application/x-votable+xml;serialization=TABLEDATA" = "votable/td"</i></li>
	 * 	<li><i><b>FITS</b>:            "application/x-votable+xml;serialization=FITS" = "votable/fits"</i></li>
	 * </ul>
	 * <p><i>It is however possible to change these default values thanks to {@link #setMimeType(String, String)}.</i></p> 
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * @param votFormat				Serialization of the VOTable data part. (TABLEDATA, BINARY, BINARY2 or FITS).
	 * @param votVersion			Version of the resulting VOTable.
	 * @param logFormatReport		<code>true</code> to append a format report (start and end date/time) in the log output, <code>false</code> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public VOTableFormat(final ServiceConnection service, final DataFormat votFormat, final VOTableVersion votVersion, final boolean logFormatReport) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("The given service connection is NULL!");

		this.service = service;
		this.logFormatReport = logFormatReport;

		// Set the VOTable serialization and version:
		this.votFormat = (votFormat == null) ? DataFormat.BINARY : votFormat;
		this.votVersion = (votVersion == null) ? VOTableVersion.V13 : votVersion;

		// Deduce automatically the MIME type and its short expression:
		if (this.votFormat.equals(DataFormat.BINARY)){
			this.mimeType = "application/x-votable+xml";
			this.shortMimeType = "votable";
		}else if (this.votFormat.equals(DataFormat.BINARY2)){
			this.mimeType = "application/x-votable+xml;serialization=BINARY2";
			this.shortMimeType = "votable/b2";
		}else if (this.votFormat.equals(DataFormat.TABLEDATA)){
			this.mimeType = "application/x-votable+xml;serialization=TABLEDATA";
			this.shortMimeType = "votable/td";
		}else if (this.votFormat.equals(DataFormat.FITS)){
			this.mimeType = "application/x-votable+xml;serialization=FITS";
			this.shortMimeType = "votable/fits";
		}else{
			this.mimeType = "application/x-votable+xml";
			this.shortMimeType = "votable";
		}
	}

	@Override
	public final String getMimeType(){
		return mimeType;
	}

	@Override
	public final String getShortMimeType(){
		return shortMimeType;
	}

	/**
	 * <p>Set the MIME type associated with this format.</p>
	 * 
	 * <p><i>Note:
	 * 	Nothing will be done if the given MIME type is NULL.
	 * 	But the short form may be NULL.
	 * </i></p>
	 * 
	 * @param mimeType	Full MIME type of this VOTable format.	<i>note: if NULL, this function does nothing</i>
	 * @param shortForm	Short form of this MIME type. <i>note: MAY be NULL</i>
	 */
	public final void setMimeType(final String mimeType, final String shortForm){
		if (mimeType != null){
			this.mimeType = mimeType;
			this.shortMimeType = shortForm;
		}
	}

	@Override
	public String getDescription(){
		return null;
	}

	@Override
	public String getFileExtension(){
		return "xml";
	}

	/**
	 * <p>Write the given error message as VOTable document.</p>
	 * 
	 * <p><i>Note:
	 * 	In the TAP protocol, all errors must be returned as VOTable. The class {@link DefaultTAPErrorWriter} is in charge of the management
	 * 	and reporting of all errors. It is calling this function while the error message to display to the user is ready and
	 * 	must be written in the HTTP response.
	 * </i></p>
	 * 
	 * <p>Here is the XML format of this VOTable error:</p>
	 * <pre>
	 * 	&lt;VOTABLE version="..." xmlns="..." &gt;
	 * 		&lt;RESOURCE type="results"&gt;
	 * 			&lt;INFO name="QUERY_STATUS" value="ERROR&gt;
	 * 				...
	 * 			&lt;/INFO&gt;
	 * 			&lt;INFO name="PROVIDER" value="..."&gt;...&lt;/INFO&gt;
	 * 			&lt;!-- other optional INFOs (e.g. request parameters) --&gt;
	 * 		&lt;/RESOURCE&gt;
	 * 	&lt;/VOTABLE&gt;
	 * </pre>
	 * 
	 * @param message	Error message to display to the user.
	 * @param otherInfo	List of other additional information to display. <i>optional</i>
	 * @param out		Stream in which the VOTable error must be written.
	 * 
	 * @throws IOException	If any error occurs while writing in the given output.
	 * 
	 * @since 2.0
	 */
	public void writeError(final String message, final Map<String,String> otherInfo, final PrintWriter writer) throws IOException{
		BufferedWriter out = new BufferedWriter(writer);

		// Set the root VOTABLE node:
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		out.newLine();
		out.write("<VOTABLE" + VOSerializer.formatAttribute("version", votVersion.getVersionNumber()) + VOSerializer.formatAttribute("xmlns", votVersion.getXmlNamespace()) + VOSerializer.formatAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance") + VOSerializer.formatAttribute("xsi:schemaLocation", votVersion.getXmlNamespace() + " " + votVersion.getSchemaLocation()) + ">");
		out.newLine();

		// The RESOURCE note MUST have a type "results":	[REQUIRED]
		out.write("<RESOURCE type=\"results\">");
		out.newLine();

		// Indicate that the query has been successfully processed:	[REQUIRED]
		out.write("<INFO name=\"QUERY_STATUS\" value=\"ERROR\">" + (message == null ? "" : VOSerializer.formatText(message)) + "</INFO>");
		out.newLine();

		// Append the PROVIDER information (if any):	[OPTIONAL]
		if (service.getProviderName() != null){
			out.write("<INFO name=\"PROVIDER\"" + VOSerializer.formatAttribute("value", service.getProviderName()) + ">" + ((service.getProviderDescription() == null) ? "" : VOSerializer.formatText(service.getProviderDescription())) + "</INFO>");
			out.newLine();
		}

		// Append the ADQL query at the origin of this result:	[OPTIONAL]
		if (otherInfo != null){
			Iterator<Map.Entry<String,String>> it = otherInfo.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<String,String> entry = it.next();
				out.write("<INFO " + VOSerializer.formatAttribute("name", entry.getKey()) + VOSerializer.formatAttribute("value", entry.getValue()) + "/>");
				out.newLine();
			}
		}

		out.flush();

		/* Write footer. */
		out.write("</RESOURCE>");
		out.newLine();
		out.write("</VOTABLE>");
		out.newLine();

		out.flush();
	}

	@Override
	public final void writeResult(final TableIterator queryResult, final OutputStream output, final TAPExecutionReport execReport, final Thread thread) throws TAPException, InterruptedException{
		try{
			long start = System.currentTimeMillis();

			ColumnInfo[] colInfos = toColumnInfos(queryResult, execReport, thread);

			/* Turns the result set into a table. */
			LimitedStarTable table = new LimitedStarTable(queryResult, colInfos, execReport.parameters.getMaxRec());

			/* Prepares the object that will do the serialization work. */
			VOSerializer voser = VOSerializer.makeSerializer(votFormat, votVersion, table);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(output));

			/* Write header. */
			writeHeader(votVersion, execReport, out);

			/* Write table element. */
			voser.writeInlineTableElement(out);
			out.flush();

			/* Check for overflow and write INFO if required. */
			if (table.lastSequenceOverflowed()){
				out.write("<INFO name=\"QUERY_STATUS\" value=\"OVERFLOW\"/>");
				out.newLine();
			}

			/* Write footer. */
			out.write("</RESOURCE>");
			out.newLine();
			out.write("</VOTABLE>");
			out.newLine();

			out.flush();

			if (logFormatReport)
				service.getLogger().logTAP(LogLevel.INFO, execReport, "FORMAT", "Result formatted (in VOTable ; " + table.getNbReadRows() + " rows ; " + table.getColumnCount() + " columns) in " + (System.currentTimeMillis() - start) + "ms!", null);
		}catch(IOException ioe){
			throw new TAPException("Error while writing a query result in VOTable!", ioe);
		}
	}

	/**
	 * <p>Writes the first VOTable nodes/elements preceding the data: VOTABLE, RESOURCE and 3 INFOS (QUERY_STATUS, PROVIDER, QUERY).</p>
	 * 
	 * @param votVersion	Target VOTable version.
	 * @param execReport	The report of the query execution.
	 * @param out			Writer in which the root node must be written.
	 * 
	 * @throws IOException	If there is an error while writing the root node in the given Writer.
	 * @throws TAPException	If there is any other error (by default: never happen).
	 */
	protected void writeHeader(final VOTableVersion votVersion, final TAPExecutionReport execReport, final BufferedWriter out) throws IOException, TAPException{
		// Set the root VOTABLE node:
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		out.newLine();
		out.write("<VOTABLE" + VOSerializer.formatAttribute("version", votVersion.getVersionNumber()) + VOSerializer.formatAttribute("xmlns", votVersion.getXmlNamespace()) + VOSerializer.formatAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance") + VOSerializer.formatAttribute("xsi:schemaLocation", votVersion.getXmlNamespace() + " " + votVersion.getSchemaLocation()) + ">");
		out.newLine();

		// The RESOURCE note MUST have a type "results":	[REQUIRED]
		out.write("<RESOURCE type=\"results\">");
		out.newLine();

		// Indicate that the query has been successfully processed:	[REQUIRED]
		out.write("<INFO name=\"QUERY_STATUS\" value=\"OK\"/>");
		out.newLine();

		// Append the PROVIDER information (if any):	[OPTIONAL]
		if (service.getProviderName() != null){
			out.write("<INFO name=\"PROVIDER\"" + VOSerializer.formatAttribute("value", service.getProviderName()) + ">" + ((service.getProviderDescription() == null) ? "" : VOSerializer.formatText(service.getProviderDescription())) + "</INFO>");
			out.newLine();
		}

		// Append the ADQL query at the origin of this result:	[OPTIONAL]
		String adqlQuery = execReport.parameters.getQuery();
		if (adqlQuery != null){
			out.write("<INFO name=\"QUERY\"" + VOSerializer.formatAttribute("value", adqlQuery) + "/>");
			out.newLine();
		}

		out.flush();
	}

	/**
	 * <p>Writes fields' metadata of the given query result in the given Writer.</p>
	 * <p><b><u>Important:</u> To write write metadata of a given field you can use {@link #writeFieldMeta(TAPColumn, PrintWriter)}.</b></p>
	 * 
	 * @param result		The query result from whose fields' metadata must be written.
	 * @param output		Writer in which fields' metadata must be written.
	 * @param execReport	The report of the query execution.
	 * @param thread		The thread which asked for the result writing.
	 * 
	 * @return				Extracted field's metadata, or NULL if no metadata have been found (theoretically, it never happens).
	 * 
	 * @throws IOException				If there is an error while writing the metadata in the given Writer.
	 * @throws TAPException				If there is any other error.
	 * @throws InterruptedException		If the given thread has been interrupted.
	 */
	public static final ColumnInfo[] toColumnInfos(final TableIterator result, final TAPExecutionReport execReport, final Thread thread) throws IOException, TAPException, InterruptedException{
		// Get the metadata extracted/guesses from the ADQL query:
		DBColumn[] columnsFromQuery = execReport.resultingColumns;

		// Get the metadata extracted from the result:
		TAPColumn[] columnsFromResult = result.getMetadata();

		int indField = 0;
		if (columnsFromQuery != null){

			// Initialize the resulting array:
			ColumnInfo[] colInfos = new ColumnInfo[columnsFromQuery.length];

			// For each column:
			for(DBColumn field : columnsFromQuery){

				// Try to build/get appropriate metadata for this field/column:
				TAPColumn colFromResult = (columnsFromResult != null && indField < columnsFromResult.length) ? columnsFromResult[indField] : null;
				TAPColumn tapCol = getValidColMeta(field, colFromResult);

				// Build the corresponding ColumnInfo object:
				colInfos[indField] = getColumnInfo(tapCol);

				indField++;

				if (thread.isInterrupted())
					throw new InterruptedException();
			}

			return colInfos;
		}else
			return null;
	}

	/**
	 * Try to get or otherwise to build appropriate metadata using those extracted from the ADQL query and those extracted from the result.
	 * 
	 * @param typeFromQuery		Metadata extracted/guessed from the ADQL query.
	 * @param typeFromResult	Metadata extracted/guessed from the result.
	 * 
	 * @return	The most appropriate metadata.
	 */
	protected static final TAPColumn getValidColMeta(final DBColumn typeFromQuery, final TAPColumn typeFromResult){
		if (typeFromQuery != null && typeFromQuery instanceof TAPColumn)
			return (TAPColumn)typeFromQuery;
		else if (typeFromResult != null){
			if (typeFromQuery != null)
				return (TAPColumn)typeFromResult.copy(typeFromQuery.getDBName(), typeFromQuery.getADQLName(), null);
			else
				return (TAPColumn)typeFromResult.copy();
		}else
			return new TAPColumn((typeFromQuery != null) ? typeFromQuery.getADQLName() : "?", new TAPType(TAPDatatype.VARCHAR), "?");
	}

	/**
	 * Convert the given {@link TAPColumn} object into a {@link ColumnInfo} object.
	 * 
	 * @param tapCol	{@link TAPColumn} to convert into {@link ColumnInfo}.
	 * 
	 * @return	The corresponding {@link ColumnInfo}.
	 */
	protected static final ColumnInfo getColumnInfo(final TAPColumn tapCol){
		// Get the VOTable type:
		VotType votType = tapCol.getDatatype().toVotType();

		// Build a ColumnInfo with the name, type and description:
		ColumnInfo colInfo = new ColumnInfo(tapCol.getADQLName(), getDatatypeClass(votType.datatype, votType.arraysize), tapCol.getDescription());

		// Set the shape (VOTable arraysize):
		colInfo.setShape(getShape(votType.arraysize));

		// Set this value may be NULL (note: it is not really necessary since STIL set this flag to TRUE by default):
		colInfo.setNullable(true);

		// Set the XType (if any):
		if (votType.xtype != null)
			colInfo.setAuxDatum(new DescribedValue(new DefaultValueInfo("xtype", String.class, "VOTable xtype attribute"), votType.xtype));

		// Set the additional information: unit, UCD and UType:
		colInfo.setUnitString(tapCol.getUnit());
		colInfo.setUCD(tapCol.getUcd());
		colInfo.setUtype(tapCol.getUtype());

		return colInfo;
	}

	/**
	 * Convert the VOTable datatype string into a corresponding {@link Class} object.
	 * 
	 * @param datatype	Value of the VOTable attribute "datatype". 
	 * @param arraysize	Value of the VOTable attribute "arraysize".
	 * 
	 * @return	The corresponding {@link Class} object.
	 */
	protected static final Class<?> getDatatypeClass(final VotDatatype datatype, final String arraysize){
		// Determine whether this type is an array or not:
		boolean isScalar = arraysize == null || (arraysize.length() == 1 && arraysize.equals("1"));

		// Guess the corresponding Class object (see section "7.1.4 Data Types" of the STIL documentation): 
		switch(datatype){
			case BIT:
				return boolean[].class;
			case BOOLEAN:
				return isScalar ? Boolean.class : boolean[].class;
			case DOUBLE:
				return isScalar ? Double.class : double[].class;
			case DOUBLE_COMPLEX:
				return double[].class;
			case FLOAT:
				return isScalar ? Float.class : float[].class;
			case FLOAT_COMPLEX:
				return float[].class;
			case INT:
				return isScalar ? Integer.class : int[].class;
			case LONG:
				return isScalar ? Long.class : long[].class;
			case SHORT:
				return isScalar ? Short.class : short[].class;
			case UNSIGNED_BYTE:
				return isScalar ? Short.class : short[].class;
			case CHAR:
			case UNICODE_CHAR:
			default: /* If the type is not know (theoretically, never happens), return char[*] by default. */
				return isScalar ? Character.class : String.class;
		}
	}

	/**
	 * Convert the given VOTable arraysize into a {@link ColumnInfo} shape.
	 * 
	 * @param arraysize	Value of the VOTable attribute "arraysize".
	 * 
	 * @return	The corresponding {@link ColumnInfo} shape.
	 */
	protected static final int[] getShape(final String arraysize){
		/*
		 * Note: multi-dimensional arrays are forbidden in the TAP library,
		 * so no 'nxm...' is possible.
		 */

		// No arraysize => empty array:
		if (arraysize == null)
			return new int[0];

		// '*' or 'n*' => {-1}:
		else if (arraysize.charAt(arraysize.length() - 1) == '*')
			return new int[]{-1};

		// 'n' => {n}:
		else{
			try{
				return new int[]{Integer.parseInt(arraysize)};
			}catch(NumberFormatException nfe){
				// if the given arraysize is incorrect (theoretically, never happens), it is like no arraysize has been provided:
				return new int[0];
			}
		}
	}

	/**
	 * <p>
	 * 	Special {@link StarTable} able to read a fixed maximum number of rows {@link TableIterator}.
	 * 	However, if no limit is provided, all rows are read.
	 * </p>
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.0 (10/2014)
	 * @since 2.0
	 */
	public static class LimitedStarTable extends AbstractStarTable {

		/** Number of columns to read. */
		private final int nbCol;

		/** Information about all columns to read. */
		private final ColumnInfo[] columnInfos;

		/** Iterator over the data to read using this special {@link StarTable} */
		private final TableIterator tableIt;

		/** Limit on the number of rows to read. Over this limit, an "overflow" event occurs and {@link #overflow} is set to TRUE. */
		private final long maxrec;

		/** Indicates whether the maximum allowed number of rows has already been read or not. When true, no more row can be read. */
		private boolean overflow;

		/** Last read row. If NULL, no row has been read or no more row is available. */
		private Object[] row = null;

		/** Number of rows read until now. */
		private int nbRows;

		/**
		 * Build this special {@link StarTable}.
		 * 
		 * @param tableIt	Data on which to iterate using this special {@link StarTable}.
		 * @param colInfos	Information about all columns.
		 * @param maxrec	Limit on the number of rows to read. <i>(if negative, there will be no limit)</i>
		 */
		LimitedStarTable(final TableIterator tableIt, final ColumnInfo[] colInfos, final long maxrec){
			this.tableIt = tableIt;
			nbCol = colInfos.length;
			columnInfos = colInfos;
			this.maxrec = maxrec;
			overflow = false;
		}

		/**
		 * Indicates whether the last row sequence dispensed by
		 * this table's getRowSequence method was truncated at maxrec rows.
		 *
		 * @return   true if the last row sequence overflowed
		 */
		public boolean lastSequenceOverflowed(){
			return overflow;
		}

		/**
		 * Get the number of rows that have been successfully read until now.
		 *
		 * @return   Number of all read rows.
		 */
		public int getNbReadRows(){
			return nbRows;
		}

		@Override
		public int getColumnCount(){
			return nbCol;
		}

		@Override
		public ColumnInfo getColumnInfo(final int colInd){
			return columnInfos[colInd];
		}

		@Override
		public long getRowCount(){
			return -1;
		}

		@Override
		public RowSequence getRowSequence() throws IOException{
			overflow = false;
			row = new Object[nbCol];

			return new RowSequence(){
				long irow = -1;

				@Override
				public boolean next() throws IOException{
					irow++;
					try{
						if (maxrec < 0 || irow < maxrec){
							boolean hasNext = tableIt.nextRow();
							if (hasNext){
								for(int i = 0; i < nbCol && tableIt.hasNextCol(); i++)
									row[i] = tableIt.nextCol();
								nbRows++;
							}else
								row = null;
							return hasNext;
						}else{
							overflow = tableIt.nextRow();
							row = null;
							return false;
						}
					}catch(DataReadException dre){
						if (dre.getCause() != null && dre.getCause() instanceof IOException)
							throw (IOException)(dre.getCause());
						else
							throw new IOException(dre);
					}
				}

				@Override
				public Object[] getRow() throws IOException{
					return row;
				}

				@Override
				public Object getCell(int cellIndex) throws IOException{
					return row[cellIndex];
				}

				@Override
				public void close() throws IOException{}
			};
		}
	}
}
