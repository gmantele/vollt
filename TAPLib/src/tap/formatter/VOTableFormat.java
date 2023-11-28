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
 * Copyright 2012-2020 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import adql.db.DBColumn;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.DataReadException;
import tap.data.TableIterator;
import tap.error.DefaultTAPErrorWriter;
import tap.metadata.TAPColumn;
import tap.metadata.TAPCoosys;
import tap.metadata.VotType;
import tap.metadata.VotType.VotDatatype;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableVersion;

/**
 * Format any given query (table) result into VOTable.
 *
 * <p>
 * 	Format and version of the resulting VOTable can be provided in parameters at
 * 	the construction time. This formatter is using STIL. So all formats and
 * 	versions managed by STIL are also here. Basically, you have the following
 * 	formats: TABLEDATA, BINARY, BINARY2 (only when using VOTable v1.3) and FITS.
 * 	The versions are: 1.0, 1.1, 1.2 and 1.3.
 * </p>
 *
 * <p><i><b>Note:</b>
 * 	The MIME type is automatically set in function of the given VOTable
 * 	serialization:
 * </i></p>
 * <ul>
 * 	<li><b>none or unknown</b>: equivalent to BINARY</li>
 * 	<li><b>BINARY</b>:          "application/x-votable+xml"
 *                              = "votable"</li>
 * 	<li><b>BINARY2</b>:         "application/x-votable+xml;serialization=BINARY2"
 *                              = "votable/b2"</li>
 * 	<li><b>TABLEDATA</b>:       "application/x-votable+xml;serialization=TABLEDATA"
 *                              = "votable/td"</li>
 * 	<li><b>FITS</b>:            "application/x-votable+xml;serialization=FITS"
 *                              = "votable/fits"</li>
 * </ul>
 * <p>
 * 	It is however possible to change these default values thanks to
 * 	{@link #setMimeType(String, String)}.
 * </p>
 *
 * <p>
 * 	In addition of the INFO elements for QUERY_STATUS="OK",
 * 	QUERY_STATUS="OVERFLOW" and QUERY_STATUS="ERROR", two additional INFO
 * 	elements are written:
 * </p>
 * <ul>
 * 	<li>PROVIDER = {@link ServiceConnection#getProviderName()} and
 *      {@link ServiceConnection#getProviderDescription()}</li>
 * 	<li>QUERY = the ADQL query at the origin of this result.</li>
 * </ul>
 *
 * <p>
 * 	Furthermore, this formatter provides a function to format an error in
 * 	VOTable: {@link #writeError(String, Map, PrintWriter)}. This is useful for
 * 	TAP which requires to return in VOTable any error that occurs while any
 * 	operation. <i>See {@link DefaultTAPErrorWriter} for more details.</i>
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (08/2020)
 */
public class VOTableFormat implements OutputFormat {

	/** The {@link ServiceConnection} to use (for the log and to have some
	 * information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/** Format of the VOTable data part in which data must be formatted.
	 * Possible values are: TABLEDATA, BINARY, BINARY2 or FITS.
	 * By default, it is set to BINARY. */
	protected final DataFormat votFormat;

	/** VOTable version in which table data must be formatted.
	 * By default, it is set to v13. */
	protected final VOTableVersion votVersion;

	/** MIME type associated with this format. */
	protected String mimeType;

	/** Short form of the MIME type associated with this format. */
	protected String shortMimeType;

	/**
	 * Creates a VOTable formatter.
	 *
	 * <p><i><b>Note:</b>
	 * 	The MIME type is automatically set to "application/x-votable+xml" =
	 * 	"votable". It is however possible to change this default value thanks to
	 * 	{@link #setMimeType(String, String)}.
	 * </i></p>
	 *
	 * @param service	The service to use (for the log and to have some
	 *               	information about the service (particularly: name,
	 *               	description).
	 *
	 * @throws NullPointerException	If the given service connection is NULL.
	 */
	public VOTableFormat(final ServiceConnection service) throws NullPointerException {
		this(service, null, null);
	}

	/**
	 * Creates a VOTable formatter.
	 *
	 * <p><i><b>Note:</b>
	 * 	The MIME type is automatically set in function of the given VOTable
	 * 	serialization:
	 * </i></p>
	 * <ul>
	 * 	<li><i><b>none or unknown</b>: equivalent to BINARY</i></li>
	 * 	<li><i><b>BINARY</b>:          "application/x-votable+xml"
	 *                                 = "votable"</i></li>
	 * 	<li><i><b>BINARY2</b>:         "application/x-votable+xml;serialization=BINARY2"
	 *                                 = "votable/b2"</i></li>
	 * 	<li><i><b>TABLEDATA</b>:       "application/x-votable+xml;serialization=TABLEDATA"
	 *                                 = "votable/td"</i></li>
	 * 	<li><i><b>FITS</b>:            "application/x-votable+xml;serialization=FITS"
	 *                                 = "votable/fits"</i></li>
	 * </ul>
	 * <p><i>
	 * 	It is however possible to change these default values thanks to
	 * 	{@link #setMimeType(String, String)}.
	 * </i></p>
	 *
	 * @param service	The service to use (for the log and to have some
	 *                  information about the service (particularly: name,
	 *                  description).
	 * @param votFormat	Serialization of the VOTable data part.
	 *                  (TABLEDATA, BINARY, BINARY2 or FITS).
	 *
	 * @throws NullPointerException	If the given service connection is NULL.
	 */
	public VOTableFormat(final ServiceConnection service, final DataFormat votFormat) throws NullPointerException {
		this(service, votFormat, null);
	}

	/**
	 * Creates a VOTable formatter.
	 *
	 * <p><i><b>Note:</b>
	 * 	The MIME type is automatically set in function of the given VOTable
	 * 	serialization:
	 * </i></p>
	 * <ul>
	 * 	<li><i><b>none or unknown</b>: equivalent to BINARY</i></li>
	 * 	<li><i><b>BINARY</b>:          "application/x-votable+xml"
	 *                                 = "votable"</i></li>
	 * 	<li><i><b>BINARY2</b>:         "application/x-votable+xml;serialization=BINARY2"
	 *                                 = "votable/b2"</i></li>
	 * 	<li><i><b>TABLEDATA</b>:       "application/x-votable+xml;serialization=TABLEDATA"
	 *                                 = "votable/td"</i></li>
	 * 	<li><i><b>FITS</b>:            "application/x-votable+xml;serialization=FITS"
	 *                                 = "votable/fits"</i></li>
	 * </ul>
	 * <p><i>
	 * 	It is however possible to change these default values thanks to
	 * 	{@link #setMimeType(String, String)}.
	 * </i></p>
	 *
	 * @param service		The service to use (for the log and to have some
	 *                  	information about the service (particularly: name,
	 *                  	description).
	 * @param votFormat		Serialization of the VOTable data part.
	 *                  	(TABLEDATA, BINARY, BINARY2 or FITS).
	 * @param votVersion	Version of the resulting VOTable.
	 *
	 * @throws NullPointerException	If the given service connection is NULL.
	 */
	public VOTableFormat(final ServiceConnection service, final DataFormat votFormat, final VOTableVersion votVersion) throws NullPointerException {
		if (service == null)
			throw new NullPointerException("The given service connection is NULL!");

		this.service = service;

		// Set the VOTable serialization and version:
		this.votFormat = (votFormat == null) ? DataFormat.BINARY : votFormat;
		this.votVersion = (votVersion == null) ? VOTableVersion.V13 : votVersion;

		// Deduce automatically the MIME type and its short expression:
		if (this.votFormat.equals(DataFormat.BINARY)) {
			this.mimeType = "application/x-votable+xml";
			this.shortMimeType = "votable";
		} else if (this.votFormat.equals(DataFormat.BINARY2)) {
			this.mimeType = "application/x-votable+xml;serialization=BINARY2";
			this.shortMimeType = "votable/b2";
		} else if (this.votFormat.equals(DataFormat.TABLEDATA)) {
			this.mimeType = "application/x-votable+xml;serialization=TABLEDATA";
			this.shortMimeType = "votable/td";
		} else if (this.votFormat.equals(DataFormat.FITS)) {
			this.mimeType = "application/x-votable+xml;serialization=FITS";
			this.shortMimeType = "votable/fits";
		} else {
			this.mimeType = "application/x-votable+xml";
			this.shortMimeType = "votable";
		}
	}

	@Override
	public final String getMimeType() {
		return mimeType;
	}

	@Override
	public final String getShortMimeType() {
		return shortMimeType;
	}

	/**
	 * Set the MIME type associated with this format.
	 *
	 * <p><i><b>Note:</b>
	 * 	NULL means no modification of the current value:
	 * </i></p>
	 *
	 * @param mimeType	Full MIME type of this VOTable format.
	 *                  <i>note: if NULL, the MIME type is not modified.</i>
	 * @param shortForm	Short form of this MIME type.
	 *                  <i>note: if NULL, the short MIME type is not modified.</i>
	 */
	public final void setMimeType(final String mimeType, final String shortForm) {
		if (mimeType != null)
			this.mimeType = mimeType;
		if (shortForm != null)
			this.shortMimeType = shortForm;
	}

	/**
	 * Get the set VOTable data serialization/format (e.g. BINARY, TABLEDATA).
	 *
	 * @return	The data format.
	 */
	public final DataFormat getVotSerialization() {
		return votFormat;
	}

	/**
	 * Get the set VOTable version.
	 *
	 * @return	The VOTable version.
	 */
	public final VOTableVersion getVotVersion() {
		return votVersion;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getFileExtension() {
		return "xml";
	}

	/**
	 * Write the given error message as VOTable document.
	 *
	 * <p><i><b>Note:</b>
	 * 	In the TAP protocol, all errors must be returned as VOTable. The class
	 * 	{@link DefaultTAPErrorWriter} is in charge of the management and
	 * 	reporting of all errors. It is calling this function while the error
	 * 	message to display to the user is ready and must be written in the HTTP
	 * 	response.
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
	 * @param otherInfo	List of other additional information to display.
	 *                 	<i>optional</i>
	 * @param writer	Stream in which the VOTable error must be written.
	 *
	 * @throws IOException	If any error occurs while writing in the given output.
	 *
	 * @since 2.0
	 */
	public void writeError(final String message, final Map<String, String> otherInfo, final PrintWriter writer) throws IOException {
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
		if (service.getProviderName() != null) {
			out.write("<INFO name=\"PROVIDER\"" + VOSerializer.formatAttribute("value", service.getProviderName()) + ">" + ((service.getProviderDescription() == null) ? "" : VOSerializer.formatText(service.getProviderDescription())) + "</INFO>");
			out.newLine();
		}

		// Append the ADQL query at the origin of this result:	[OPTIONAL]
		if (otherInfo != null) {
			Iterator<Map.Entry<String, String>> it = otherInfo.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<String, String> entry = it.next();
				if (entry.getValue() != null) {
					if (entry.getValue().startsWith("\n")) {
						int sep = entry.getValue().substring(1).indexOf('\n');
						if (sep < 0)
							sep = 0;
						else
							sep++;
						out.write("<INFO " + VOSerializer.formatAttribute("name", entry.getKey()) + VOSerializer.formatAttribute("value", entry.getValue().substring(1, sep)) + ">\n" + entry.getValue().substring(sep + 1) + "\n</INFO>");
					} else
						out.write("<INFO " + VOSerializer.formatAttribute("name", entry.getKey()) + VOSerializer.formatAttribute("value", entry.getValue()) + "/>");
					out.newLine();
				}
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
	public final void writeResult(final TableIterator queryResult, final OutputStream output, final TAPExecutionReport execReport, final Thread thread) throws TAPException, IOException, InterruptedException {
		ColumnInfo[] colInfos = toColumnInfos(queryResult, execReport, thread);

		/* Turns the result set into a table. */
		LimitedStarTable table = new LimitedStarTable(queryResult, colInfos, execReport.parameters.getMaxRec(), thread);
		table.setName("result_" + execReport.jobID);

		/* Prepares the object that will do the serialization work. */
		VOSerializer voser = null;
		/* if FITS, copy the table on disk (or in memory if the table is short):
		 * (note: this is needed because STIL needs at least 2 passes on this
		 *        table to format it correctly in FITS format) */
		if (votFormat == DataFormat.FITS) {
			try {
				voser = VOSerializer.makeSerializer(votFormat, votVersion, StoragePolicy.PREFER_DISK.copyTable(table));
			} catch(IOException ioe) {
				/* As in the class FITSFormat, the caught IOException may be due
				 * to an interruption from LimitedStarTable. In such case,
				 * propagate the interruption: */
				if (thread.isInterrupted())
					throw new InterruptedException();
				/* Any other error should be properly wrapped: */
				else
					throw new TAPException("Unexpected error while formatting the result!", ioe);
			}
		}
		// otherwise, just use the default VOTable serializer:
		else
			voser = VOSerializer.makeSerializer(votFormat, votVersion, table);

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(output));

		/* Write header. */
		writeHeader(votVersion, execReport, out);

		/* Write table element. */
		if (!thread.isInterrupted()) {
			try {
				voser.writeInlineTableElement(out);
				execReport.nbRows = table.getNbReadRows();
				out.flush();
			} catch(Exception ex) {
				/* If synchronous, the partially written VOTable should be
				 * properly closed and an error INFO should be appended: */
				if (execReport.synchronous) {
					if (votFormat != DataFormat.TABLEDATA) {
						out.write("</STREAM>\n</BINARY>\n</DATA>\n</TABLE>");
						out.newLine();
					}
					out.write("<INFO name=\"QUERY_STATUS\" value=\"ERROR\">Result truncated due to an unexpected grave error: " + VOSerializer.formatText(ex.getMessage()) + "</INFO>");
					out.newLine();
				}
				// If asynchronous, just propagate the error:
				else {
					if (ex instanceof TAPException || ex instanceof IOException || ex instanceof InterruptedException)
						throw ex;
					else
						throw new TAPException(ex);
				}
			}
		}

		/* If Timed Out... */
		if (thread.isInterrupted()) {
			// ...if synchronous, end properly the VOTable with an error INFO:
			if (execReport != null && execReport.synchronous) {
				out.write("<INFO name=\"QUERY_STATUS\" value=\"ERROR\">Time out! (Hint: Try running this query in asynchronous mode to get the complete result)</INFO>");
				out.newLine();
			}
			// ...if asynchronous, merely propagate the interruption:
			else
				throw new InterruptedException();
		}
		/* If Overflow, declare this in an INFO: */
		else if (table.lastSequenceOverflowed()) {
			out.write("<INFO name=\"QUERY_STATUS\" value=\"OVERFLOW\"/>");
			out.newLine();
		}

		/* Write footer. */
		out.write("</RESOURCE>");
		out.newLine();
		out.write("</VOTABLE>");
		out.newLine();

		out.flush();
	}

	/**
	 * Writes the first VOTable nodes/elements preceding the data: VOTABLE,
	 * RESOURCE and 3 INFOS (QUERY_STATUS, PROVIDER, QUERY).
	 *
	 * @param votVersion	Target VOTable version.
	 * @param execReport	The report of the query execution.
	 * @param out			Writer in which the root node must be written.
	 *
	 * @throws IOException	If there is an error while writing the root node in
	 *                    	the given Writer.
	 * @throws TAPException	If there is any other error
	 *                     	(by default: never happen).
	 */
	protected void writeHeader(final VOTableVersion votVersion, final TAPExecutionReport execReport, final BufferedWriter out) throws IOException, TAPException {
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
		if (service.getProviderName() != null) {
			out.write("<INFO name=\"PROVIDER\"" + VOSerializer.formatAttribute("value", service.getProviderName()) + ">" + ((service.getProviderDescription() == null) ? "" : VOSerializer.formatText(service.getProviderDescription())) + "</INFO>");
			out.newLine();
		}

		// Append the ADQL query at the origin of this result:	[OPTIONAL]
		String adqlQuery = execReport.parameters.getQuery();
		if (adqlQuery != null) {
			out.write("<INFO name=\"QUERY\"" + VOSerializer.formatAttribute("value", adqlQuery) + "/>");
			out.newLine();
		}

		// Append the fixed ADQL query, if any:	[OPTIONAL]
		String fixedQuery = execReport.fixedQuery;
		if (fixedQuery != null) {
			out.write("<INFO name=\"QUERY_AFTER_AUTO_FIX\"" + VOSerializer.formatAttribute("value", fixedQuery) + "/>");
			out.newLine();
		}

		// Insert the definition of all used coordinate systems:
		HashSet<String> insertedCoosys = new HashSet<String>(10);
		for(DBColumn col : execReport.resultingColumns) {
			// ignore columns with no coossys:
			if (col instanceof TAPColumn && ((TAPColumn)col).getCoosys() != null) {
				// get its coosys:
				TAPCoosys coosys = ((TAPColumn)col).getCoosys();
				// insert the coosys definition ONLY if not already done because of another column:
				if (!insertedCoosys.contains(coosys.getId())) {
					// write the VOTable serialization of this coordinate system definition:
					out.write("<COOSYS" + VOSerializer.formatAttribute("ID", coosys.getId()));
					if (coosys.getSystem() != null)
						out.write(VOSerializer.formatAttribute("system", coosys.getSystem()));
					if (coosys.getEquinox() != null)
						out.write(VOSerializer.formatAttribute("equinox", coosys.getEquinox()));
					if (coosys.getEpoch() != null)
						out.write(VOSerializer.formatAttribute("epoch", coosys.getEpoch()));
					out.write(" />");
					out.newLine();
					// remember this coosys has already been written:
					insertedCoosys.add(coosys.getId());
				}
			}
		}

		out.flush();
	}

	/**
	 * Writes fields' metadata of the given query result.
	 *
	 * @param result		The query result from whose fields' metadata must be
	 *              		written.
	 * @param execReport	The report of the query execution.
	 * @param thread		The thread which asked for the result writing.
	 *
	 * @return	Extracted field's metadata, or NULL if no metadata have been
	 *        	found (theoretically, it never happens).
	 *
	 * @throws IOException			If there is an error while writing the
	 *                    			metadata.
	 * @throws TAPException			If there is any other error.
	 * @throws InterruptedException	If the given thread has been
	 *                             	interrupted.
	 */
	public static final ColumnInfo[] toColumnInfos(final TableIterator result, final TAPExecutionReport execReport, final Thread thread) throws IOException, TAPException, InterruptedException {
		// Get the metadata extracted/guesses from the ADQL query:
		DBColumn[] columnsFromQuery = execReport.resultingColumns;

		// Get the metadata extracted from the result:
		TAPColumn[] columnsFromResult = result.getMetadata();

		int indField = 0;
		if (columnsFromQuery != null) {

			// Initialize the resulting array:
			ColumnInfo[] colInfos = new ColumnInfo[columnsFromQuery.length];

			// For each column:
			for(DBColumn field : columnsFromQuery) {

				// Try to build/get appropriate metadata for this field/column:
				TAPColumn colFromResult = (columnsFromResult != null && indField < columnsFromResult.length) ? columnsFromResult[indField] : null;
				TAPColumn tapCol = getValidColMeta(field, colFromResult);

				// Build the corresponding ColumnInfo object:
				colInfos[indField] = getColumnInfo(tapCol);

				indField++;
			}

			return colInfos;
		} else
			return null;
	}

	/**
	 * Try to get or otherwise to build appropriate metadata using those
	 * extracted from the ADQL query and those extracted from the result.
	 *
	 * @param typeFromQuery		Metadata extracted/guessed from the ADQL query.
	 * @param typeFromResult	Metadata extracted/guessed from the result.
	 *
	 * @return	The most appropriate metadata.
	 */
	protected static final TAPColumn getValidColMeta(final DBColumn typeFromQuery, final TAPColumn typeFromResult) {
		if (typeFromQuery != null && typeFromQuery instanceof TAPColumn) {
			TAPColumn colMeta = (TAPColumn)typeFromQuery;
			if (colMeta.getDatatype().isUnknown() && typeFromResult != null && !typeFromResult.getDatatype().isUnknown())
				colMeta.setDatatype(typeFromResult.getDatatype());
			return colMeta;
		} else if (typeFromResult != null) {
			if (typeFromQuery != null)
				return (TAPColumn)typeFromResult.copy(typeFromQuery.getDBName(), typeFromQuery.getADQLName(), null);
			else
				return (TAPColumn)typeFromResult.copy();
		} else
			return new TAPColumn((typeFromQuery != null) ? typeFromQuery.getADQLName() : "?", new DBType(DBDatatype.VARCHAR), "?");
	}

	/**
	 * Convert the given {@link TAPColumn} object into a {@link ColumnInfo}
	 * object.
	 *
	 * @param tapCol	{@link TAPColumn} to convert into {@link ColumnInfo}.
	 *
	 * @return	The corresponding {@link ColumnInfo}.
	 */
	protected static final ColumnInfo getColumnInfo(final TAPColumn tapCol) {
		// Get the VOTable type:
		VotType votType = new VotType(tapCol.getDatatype());

		// Build a ColumnInfo with the name, type and description:
		ColumnInfo colInfo = new ColumnInfo(tapCol.getADQLName(), getDatatypeClass(votType.datatype, votType.arraysize), tapCol.getDescription());

		// Set the shape (VOTable arraysize):
		colInfo.setShape(getShape(votType.arraysize));

		// Set this value may be NULL (note: it is not really necessary since STIL set this flag to TRUE by default):
		colInfo.setNullable(true);

		// Set the XType (if any):
		if (votType.xtype != null)
			colInfo.setAuxDatum(new DescribedValue(VOStarTable.XTYPE_INFO, votType.xtype));

		// Set the additional information: unit, UCD and UType:
		colInfo.setUnitString(tapCol.getUnit());
		colInfo.setUCD(tapCol.getUcd());
		colInfo.setUtype(tapCol.getUtype());

		// Set the coosys ref (if any):
		if (tapCol.getCoosys() != null)
			colInfo.setAuxDatum(new DescribedValue(VOStarTable.REF_INFO, tapCol.getCoosys().getId()));

		return colInfo;
	}

	/**
	 * Convert the VOTable datatype string into a corresponding {@link Class}
	 * object.
	 *
	 * @param datatype	Value of the VOTable attribute "datatype".
	 * @param arraysize	Value of the VOTable attribute "arraysize".
	 *
	 * @return	The corresponding {@link Class} object.
	 */
	protected static final Class<?> getDatatypeClass(final VotDatatype datatype, final String arraysize) {
		// Determine whether this type is an array or not:
		boolean isScalar = arraysize == null || (arraysize.length() == 1 && arraysize.equals("1"));

		// Guess the corresponding Class object (see section "7.1.4 Data Types" of the STIL documentation):
		switch(datatype) {
			case BIT:
				return boolean[].class;
			case BOOLEAN:
				return isScalar ? Boolean.class : boolean[].class;
			case DOUBLE:
				return isScalar ? Double.class : double[].class;
			case DOUBLECOMPLEX:
				return double[].class;
			case FLOAT:
				return isScalar ? Float.class : float[].class;
			case FLOATCOMPLEX:
				return float[].class;
			case INT:
				return isScalar ? Integer.class : int[].class;
			case LONG:
				return isScalar ? Long.class : long[].class;
			case SHORT:
				return isScalar ? Short.class : short[].class;
			case UNSIGNEDBYTE:
				return isScalar ? Short.class : short[].class;
			case CHAR:
			case UNICODECHAR:
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
	protected static final int[] getShape(final String arraysize) {
		/*
		 * Note: multi-dimensional arrays are forbidden in the TAP library,
		 * so no 'nxm...' is possible.
		 */

		// No arraysize => empty array:
		if (arraysize == null)
			return new int[0];

		// '*' or 'n*' => {-1}:
		else if (arraysize.charAt(arraysize.length() - 1) == '*')
			return new int[]{ -1 };

		// 'n' => {n}:
		else {
			try {
				return new int[]{ Integer.parseInt(arraysize) };
			} catch(NumberFormatException nfe) {
				// if the given arraysize is incorrect (theoretically, never happens), it is like no arraysize has been provided:
				return new int[0];
			}
		}
	}

	/**
	 * Special {@link StarTable} able to read a fixed maximum number of rows
	 * {@link TableIterator}. However, if no limit is provided, all rows are
	 * read.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.1 (11/2015)
	 * @since 2.0
	 */
	public static class LimitedStarTable extends AbstractStarTable {

		/** Number of columns to read. */
		private final int nbCol;

		/** Information about all columns to read. */
		private final ColumnInfo[] columnInfos;

		/** Iterator over the data to read using this special {@link StarTable} */
		private final TableIterator tableIt;

		/** Thread covering this execution. If it is interrupted, the writing
		 * must stop as soon as possible.
		 * @since 2.1 */
		private final Thread threadToWatch;

		/** Limit on the number of rows to read. Over this limit, an "overflow"
		 * event occurs and {@link #overflow} is set to TRUE. */
		private final long maxrec;

		/** Indicates whether the maximum allowed number of rows has already
		 * been read or not. When true, no more row can be read. */
		private boolean overflow;

		/** Last read row. If NULL, no row has been read or no more row is
		 * available. */
		private Object[] row = null;

		/** Number of rows read until now. */
		private int nbRows;

		/**
		 * Build this special {@link StarTable}.
		 *
		 * @param tableIt	Data on which to iterate using this special
		 *               	{@link StarTable}.
		 * @param colInfos	Information about all columns.
		 * @param maxrec	Limit on the number of rows to read.
		 *              	<i>(if negative, there will be no limit)</i>
		 * @param thread	Parent thread. When an interruption is detected the
		 *              	writing must stop as soon as possible.
		 */
		LimitedStarTable(final TableIterator tableIt, final ColumnInfo[] colInfos, final long maxrec, final Thread thread) {
			this.tableIt = tableIt;
			this.threadToWatch = thread;
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
		public boolean lastSequenceOverflowed() {
			return overflow;
		}

		/**
		 * Get the number of rows that have been successfully read until now.
		 *
		 * @return   Number of all read rows.
		 */
		public int getNbReadRows() {
			return nbRows;
		}

		@Override
		public int getColumnCount() {
			return nbCol;
		}

		@Override
		public ColumnInfo getColumnInfo(final int colInd) {
			return columnInfos[colInd];
		}

		@Override
		public long getRowCount() {
			return -1;
		}

		@Override
		public RowSequence getRowSequence() throws IOException {
			overflow = false;
			row = new Object[nbCol];

			return new RowSequence() {
				long irow = -1;

				@Override
				public boolean next() throws IOException {
					irow++;
					try {
						if (!threadToWatch.isInterrupted() && (maxrec < 0 || irow < maxrec)) {
							boolean hasNext = tableIt.nextRow();
							if (hasNext) {
								for(int i = 0; i < nbCol && tableIt.hasNextCol(); i++)
									row[i] = tableIt.nextCol();
								nbRows++;
							} else
								row = null;
							return hasNext;
						} else {
							overflow = tableIt.nextRow();
							row = null;
							return false;
						}
					} catch(DataReadException dre) {
						if (dre.getCause() != null && dre.getCause() instanceof IOException)
							throw (IOException)(dre.getCause());
						else
							throw new IOException(dre);
					}
				}

				@Override
				public Object[] getRow() throws IOException {
					return row;
				}

				@Override
				public Object getCell(int cellIndex) throws IOException {
					return row[cellIndex];
				}

				@Override
				public void close() throws IOException {
				}
			};
		}
	}
}
