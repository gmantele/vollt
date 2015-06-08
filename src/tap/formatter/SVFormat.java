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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;
import adql.db.DBColumn;

/**
 * Format any given query (table) result into CSV or TSV (or with custom separator).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (04/2015)
 */
public class SVFormat implements OutputFormat {

	/** Column separator for CSV format. */
	public static final char COMMA_SEPARATOR = ',';
	/** Column separator for sCSV format. */
	public static final char SEMI_COLON_SEPARATOR = ';';
	/** Column separator for TSV format. */
	public static final char TAB_SEPARATOR = '\t';

	/** The {@link ServiceConnection} to use (for the log and to have some information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/** Column separator to use. */
	protected final String separator;

	/** Indicate whether String values must be delimited by double quotes (default) or not. */
	protected final boolean delimitStr;

	/** MIME type associated with this format.
	 * @since 1.1 */
	protected final String mimeType;

	/** Alias of the MIME type associated with this format.
	 * @since 1.1 */
	protected final String shortMimeType;

	/**
	 * Build a SVFormat (in which String values are delimited by double quotes).
	 * 
	 * @param service		Description of the TAP service.
	 * @param colSeparator	Column separator to use.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public SVFormat(final ServiceConnection service, char colSeparator) throws NullPointerException{
		this(service, colSeparator, true);
	}

	/**
	 * Build a SVFormat.
	 * 
	 * @param service			Description of the TAP service.
	 * @param colSeparator		Column separator to use.
	 * @param delimitStrings	<i>true</i> if String values must be delimited by double quotes, <i>false</i> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public SVFormat(final ServiceConnection service, char colSeparator, boolean delimitStrings) throws NullPointerException{
		this(service, colSeparator, delimitStrings, null, null);
	}

	/**
	 * Build a SVFormat.
	 * 
	 * @param service			Description of the TAP service.
	 * @param colSeparator		Column separator to use.
	 * @param delimitStrings	<i>true</i> if String values must be delimited by double quotes, <i>false</i> otherwise.
	 * @param mime				The MIME type to associate with this format. <i>note: this MIME type is then used by a user to specify the result format he wants.</i>
	 * @param shortMime			The alias of the MIME type to associate with this format. <i>note: this short MIME type is then used by a user to specify the result format he wants.</i>
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 * 
	 * @since 2.0
	 */
	public SVFormat(final ServiceConnection service, char colSeparator, boolean delimitStrings, final String mime, final String shortMime) throws NullPointerException{
		this(service, "" + colSeparator, delimitStrings, mime, shortMime);
	}

	/**
	 * Build a SVFormat (in which String values are delimited by double quotes).
	 * 
	 * @param service		Description of the TAP service.
	 * @param colSeparator	Column separator to use.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public SVFormat(final ServiceConnection service, String colSeparator) throws NullPointerException{
		this(service, colSeparator, true);
	}

	/**
	 * Build a SVFormat.
	 * 
	 * @param service			Description of the TAP service.
	 * @param colSeparator		Column separator to use.
	 * @param delimitStrings	<i>true</i> if String values must be delimited by double quotes, <i>false</i> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public SVFormat(final ServiceConnection service, String colSeparator, boolean delimitStrings) throws NullPointerException{
		this(service, colSeparator, delimitStrings, null, null);
	}

	/**
	 * Build a SVFormat.
	 * 
	 * @param service			Description of the TAP service.
	 * @param colSeparator		Column separator to use.
	 * @param delimitStrings	<i>true</i> if String values must be delimited by double quotes, <i>false</i> otherwise.
	 * @param mime				The MIME type to associate with this format. <i>note: this MIME type is then used by a user to specify the result format he wants.</i>
	 * @param shortMime			The alias of the MIME type to associate with this format. <i>note: this short MIME type is then used by a user to specify the result format he wants.</i>
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 * 
	 * @since 2.0
	 */
	public SVFormat(final ServiceConnection service, String colSeparator, boolean delimitStrings, final String mime, final String shortMime) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("The given service connection is NULL!");

		separator = (colSeparator == null || colSeparator.length() <= 0) ? ("" + COMMA_SEPARATOR) : colSeparator;
		delimitStr = delimitStrings;
		mimeType = (mime == null || mime.trim().length() <= 0) ? guessMimeType(separator) : mime;
		shortMimeType = (shortMime == null || shortMime.trim().length() <= 0) ? guessShortMimeType(separator) : shortMime;
		this.service = service;
	}

	/**
	 * <p>Try to guess the MIME type to associate with this SV format, in function of the column separator.</p>
	 * 
	 * <p>
	 * 	By default, only "," or ";" (text/csv) and [TAB] (text/tab-separated-values) are supported.
	 * 	If the separator is unknown, "text/plain" will be returned.
	 * </p>
	 * 
	 * <p><i>Note: In order to automatically guess more MIME types, you should overwrite this function.</i></p>
	 * 
	 * @param separator	Column separator of this SV format.
	 * 
	 * @return	The guessed MIME type.
	 * 
	 * @since 2.0
	 */
	protected String guessMimeType(final String separator){
		switch(separator.charAt(0)){
			case COMMA_SEPARATOR:
			case SEMI_COLON_SEPARATOR:
				return "text/csv";
			case TAB_SEPARATOR:
				return "text/tab-separated-values";
			default:
				return "text/plain";
		}
	}

	/**
	 * <p>Try to guess the short MIME type to associate with this SV format, in function of the column separator.</p>
	 * 
	 * <p>
	 * 	By default, only "," or ";" (csv) and [TAB] (tsv) are supported.
	 * 	If the separator is unknown, "text" will be returned.
	 * </p>
	 * 
	 * <p><i>Note: In order to automatically guess more short MIME types, you should overwrite this function.</i></p>
	 * 
	 * @param separator	Column separator of this SV format.
	 * 
	 * @return	The guessed short MIME type.
	 * 
	 * @since 2.0
	 */
	protected String guessShortMimeType(final String separator){
		switch(separator.charAt(0)){
			case COMMA_SEPARATOR:
			case SEMI_COLON_SEPARATOR:
				return "csv";
			case TAB_SEPARATOR:
				return "tsv";
			default:
				return "text";
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

	@Override
	public String getDescription(){
		return null;
	}

	@Override
	public String getFileExtension(){
		switch(separator.charAt(0)){
			case COMMA_SEPARATOR:
			case SEMI_COLON_SEPARATOR:
				return "csv";
			case TAB_SEPARATOR:
				return "tsv";
			default:
				return "txt";
		}
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, IOException, InterruptedException{
		// Prepare the output stream:
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

		// Write header:
		DBColumn[] columns = writeHeader(result, writer, execReport, thread);

		if (thread.isInterrupted())
			throw new InterruptedException();

		// Write data:
		writeData(result, columns, writer, execReport, thread);

		writer.flush();
	}

	/**
	 * Write the whole header (one row whose columns are just the columns' name).
	 * 
	 * @param result		Result to write later (but it contains also metadata that was extracted from the result itself).
	 * @param writer		Output in which the metadata must be written.
	 * @param execReport	Execution report (which contains the metadata extracted/guessed from the ADQL query).
	 * @param thread		Thread which has asked for this formatting (it must be used in order to test the {@link Thread#isInterrupted()} flag and so interrupt everything if need).
	 * 
	 * @return	All the written metadata.
	 * 
	 * @throws IOException				If there is an error while writing something in the output.
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws TAPException				If any other error occurs.
	 */
	protected DBColumn[] writeHeader(TableIterator result, BufferedWriter writer, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException{
		// Get the columns meta:
		DBColumn[] selectedColumns = execReport.resultingColumns;

		// If meta are not known, no header will be written:
		int nbColumns = (selectedColumns == null) ? -1 : selectedColumns.length;
		if (nbColumns > 0){
			// Write all columns' name:
			for(int i = 0; i < nbColumns - 1; i++){
				writer.write(selectedColumns[i].getADQLName());
				writer.write(separator);
			}
			writer.write(selectedColumns[nbColumns - 1].getADQLName());

			// Go to a new line (in order to prepare the data writing):
			writer.newLine();
			writer.flush();
		}

		// Returns the written columns:
		return selectedColumns;
	}

	/**
	 * Write all the data rows.
	 * 
	 * @param result			Result to write.	
	 * @param selectedColumns	All columns' metadata.
	 * @param writer			Writer in which the data must be written.
	 * @param execReport		Execution report (which contains the maximum allowed number of records to output).
	 * @param thread			Thread which has asked for this formatting (it must be used in order to test the {@link Thread#isInterrupted()} flag and so interrupt everything if need).
	 * 
	 * @throws IOException				If there is an error while writing something in the given writer.
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws TAPException				If any other error occurs.
	 */
	protected void writeData(TableIterator result, DBColumn[] selectedColumns, BufferedWriter writer, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException{
		execReport.nbRows = 0;

		while(result.nextRow()){
			// Stop right now the formatting if the job has been aborted/canceled/interrupted:
			if (thread.isInterrupted())
				throw new InterruptedException();

			// Deal with OVERFLOW, if needed:
			if (execReport.parameters.getMaxRec() > 0 && execReport.nbRows >= execReport.parameters.getMaxRec()) // that's to say: OVERFLOW !
				break;

			int indCol = 0;
			while(result.hasNextCol()){
				// Write the column value:
				writeFieldValue(result.nextCol(), selectedColumns[indCol++], writer);

				// Append the column separator:
				if (result.hasNextCol())
					writer.write(separator);
			}
			writer.newLine();

			execReport.nbRows++;

			// flush the writer every 30 lines:
			if (execReport.nbRows % 30 == 0)
				writer.flush();
		}
		writer.flush();
	}

	/**
	 * <p>Writes the given field value in the given Writer.</p>
	 * 
	 * <p>
	 * 	A String value will be delimited if {@link #delimitStr} is true, otherwise this type of value will
	 *  be processed like the other type of values: no delimiter and just transformed into a string.
	 * </p>
	 * 
	 * @param value				The value to write.
	 * @param column			The corresponding column metadata.
	 * @param writer			The stream in which the field value must be written.
	 * 
	 * @throws IOException		If there is an error while writing the given field value in the given stream.
	 * @throws TAPException		If there is any other error (by default: never happen).
	 */
	protected void writeFieldValue(final Object value, final DBColumn column, final BufferedWriter writer) throws IOException, TAPException{
		if (value != null){
			if ((delimitStr && value instanceof String) || value.toString().contains(separator)){
				writer.write('"');
				writer.write(value.toString().replaceAll("\"", "'"));
				writer.write('"');
			}else
				writer.write(value.toString());
		}
	}
}
