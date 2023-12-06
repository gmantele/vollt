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
 * Copyright 2012-2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import adql.db.DBColumn;
import cds.util.LargeAsciiTable;
import cds.util.LargeAsciiTable.LineProcessor;
import cds.util.LargeAsciiTable.LineProcessorException;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;

/**
 * Format any given query (table) result into a simple table ASCII
 * representation (columns' width are adjusted so that all columns are well
 * aligned and of the same width).
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.3 (11/2018)
 */
public class TextFormat implements OutputFormat {

	/** Internal column separator.
	 * Note: the output separator is however always a |.
	 * @since 2.0 */
	protected static final char COL_SEP = '\u25c6';

	/** How all columns must be aligned.
	 * @see LargeAsciiTable#streamAligned(LineProcessor, int[])
	 * @since 2.3 */
	protected int[] alignment = new int[]{ LargeAsciiTable.LEFT };

	/** The {@link ServiceConnection} to use (for the log and to have some
	 * information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/**
	 * Build a {@link TextFormat}.
	 *
	 * <p><em><strong>Note:</strong>
	 * 	All columns values will be aligned on the left.
	 * 	To change this default alignment, use
	 * 	{@link #TextFormat(ServiceConnection, int[])} instead.
	 * </em></p>
	 *
	 * @param service	Description of the TAP service.
	 *
	 * @throws NullPointerException	If the given service connection is NULL.
	 */
	public TextFormat(final ServiceConnection service) throws NullPointerException{
		this(service, new int[]{ LargeAsciiTable.LEFT });
	}

	/**
	 * Build a {@link TextFormat}.
	 *
	 * @param service			Description of the TAP service.
	 * @param customAlignment	How columns must be aligned.
	 *                       	<em>(see {@link LargeAsciiTable#streamAligned(LineProcessor, int[])}
	 *                       	to know the rules about this array)</em>
	 *
	 * @throws NullPointerException	If the given service connection is NULL.
	 *
	 * @since 2.3
	 */
	public TextFormat(final ServiceConnection service, final int[] customAlignment) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("The given service connection is NULL!");

		this.service = service;
		this.alignment = customAlignment;
	}

	@Override
	public String getMimeType(){
		return "text/plain";
	}

	@Override
	public String getShortMimeType(){
		return "text";
	}

	@Override
	public String getDescription(){
		return null;
	}

	@Override
	public String getFileExtension(){
		return "txt";
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, IOException, InterruptedException{
		// Prepare the formatting of the whole output:
		try(LargeAsciiTable asciiTable = new LargeAsciiTable(COL_SEP)){

			// Write header:
			String headerLine = getHeader(result, execReport, thread);
			asciiTable.addHeaderLine(headerLine);

			if (thread.isInterrupted())
				throw new InterruptedException();

			// Write data into the AsciiTable object:
			boolean overflow = writeData(result, asciiTable, execReport, thread);

			// Finally write the formatted ASCII table (header + data) in the output stream:
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
			execReport.nbRows = 0;
			try{
				// Align and write the result:
				execReport.nbRows = asciiTable.streamAligned(new LineFormatter(writer), alignment, '|', thread);

				// Add a line in case of an OVERFLOW:
				if (overflow){
					writer.write("\nOVERFLOW (more rows were available but have been truncated by the TAP service)");
					writer.newLine();
				}

				writer.flush();
			}catch(LineProcessorException lpe){
				throw new TAPException("Unexpected error while formatting a result line!", lpe);
			}
		}
	}

	/**
	 * Lets format a line and then write it in the given {@link BufferedWriter}.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.3 (11/2018)
	 * @since 2.3
	 */
	protected static class LineFormatter implements LineProcessor {
		private final BufferedWriter writer;
		private long nbFormattedLines = 0;

		public LineFormatter(final BufferedWriter writer){
			this.writer = writer;
		}

		@Override
		public boolean process(String line) throws LineProcessorException{
			try{
				// write the line:
				writer.write(line);
				writer.newLine();
				// update the counter of written lines:
				//execReport.nbRows++;
				nbFormattedLines++;
				// flush the writer every 30 lines:
				if (nbFormattedLines % 30 == 0)
					writer.flush();
				return true;
			}catch(IOException ioe){
				throw new LineProcessorException("Impossible to write the given result line!", ioe);
			}
		}
	}

	/**
	 * Get the whole header (one row whose columns are just the columns' name).
	 *
	 * @param result		Result to write later (but it contains also metadata
	 *              		that was extracted from the result itself).
	 * @param execReport	Execution report (which contains the metadata
	 *                  	extracted/guessed from the ADQL query).
	 * @param thread		Thread which has asked for this formatting (it must
	 *              		be used in order to test the
	 *              		{@link Thread#isInterrupted()} flag and so interrupt
	 *              		everything if need).
	 *
	 * @return	All the written metadata.
	 *
	 * @throws TAPException	If any other error occurs.
	 */
	protected String getHeader(final TableIterator result, final TAPExecutionReport execReport, final Thread thread) throws TAPException{
		// Get the columns meta:
		DBColumn[] selectedColumns = execReport.resultingColumns;

		StringBuffer line = new StringBuffer();

		// If meta are not known, no header will be written:
		int nbColumns = (selectedColumns == null) ? -1 : selectedColumns.length;
		if (nbColumns > 0){

			// Write all columns' name:
			for(int i = 0; i < nbColumns - 1; i++)
				line.append(selectedColumns[i].getADQLName()).append(COL_SEP);
			line.append(selectedColumns[nbColumns - 1].getADQLName());
		}

		// Return the header line:
		return line.toString();
	}

	/**
	 * Write all the data rows into the given {@link LargeAsciiTable} object.
	 *
	 * @param queryResult	Result to write.
	 * @param asciiTable	Output in which the rows (as string) must be
	 *                  	written.
	 * @param execReport	Execution report (which contains the maximum allowed
	 *                  	number of records to output).
	 * @param thread		Thread which has asked for this formatting (it must
	 *              		be used in order to test the
	 *              		{@link Thread#isInterrupted()} flag and so interrupt
	 *              		everything if need).
	 *
	 * @return	<i>true</i> if an overflow (i.e. nbDBRows > MAXREC) is detected,
	 *        	<i>false</i> otherwise.
	 *
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws TAPException				If any other error occurs.
	 */
	protected boolean writeData(final TableIterator queryResult, final LargeAsciiTable asciiTable, final TAPExecutionReport execReport, final Thread thread) throws IOException, TAPException, InterruptedException{
		execReport.nbRows = 0;
		boolean overflow = false;

		// Get the list of columns:
		DBColumn[] selectedColumns = execReport.resultingColumns;
		int nbColumns = selectedColumns.length;

		StringBuffer line = new StringBuffer();
		while(queryResult.nextRow()){
			// Stop right now the formatting if the job has been aborted/cancelled/interrupted:
			if (thread.isInterrupted())
				throw new InterruptedException();

			// Deal with OVERFLOW, if needed:
			if (execReport.parameters.getMaxRec() > 0 && execReport.nbRows >= execReport.parameters.getMaxRec()){
				overflow = true;
				break;
			}

			// Clear the line buffer:
			line.delete(0, line.length());

			int indCol = 0;
			while(queryResult.hasNextCol()){

				// Write the column value:
				writeFieldValue(queryResult.nextCol(), selectedColumns[indCol++], line);

				// Write the column separator (if needed):
				if (indCol != nbColumns)
					line.append(COL_SEP);
			}

			// Append the line/row in the ASCII table:
			asciiTable.addLine(line.toString());

			execReport.nbRows++;
		}

		// Declare the table as complete:
		asciiTable.endTable();

		return overflow;
	}

	/**
	 * Writes the given field value in the given buffer.
	 *
	 * @param value		The value to write.
	 * @param tapCol	The corresponding column metadata.
	 * @param line		The buffer in which the field value must be written.
	 */
	protected void writeFieldValue(final Object value, final DBColumn tapCol, final StringBuffer line){
		if (value != null){
			if (value instanceof String)
				line.append('"').append(value.toString()).append('"');
			else
				line.append(value.toString());
		}
	}
}
