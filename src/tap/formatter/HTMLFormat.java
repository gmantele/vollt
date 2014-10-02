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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;
import uk.ac.starlink.votable.VOSerializer;
import uws.ISO8601Format;
import uws.service.log.UWSLog.LogLevel;
import adql.db.DBColumn;

/**
 * Format any given query (table) result into HTML.
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (10/2014)
 * @since 2.0
 */
public class HTMLFormat implements OutputFormat {

	/** Indicates whether a format report (start and end date/time) must be printed in the log output.  */
	private boolean logFormatReport;

	/** The {@link ServiceConnection} to use (for the log and to have some information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/**
	 * Creates an HTML formatter.
	 * 
	 * @param service	Description of the TAP service.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public HTMLFormat(final ServiceConnection service) throws NullPointerException{
		this(service, true);
	}

	/**
	 * Creates an HTML formatter
	 * 
	 * @param service			Description of the TAP service.
	 * @param logFormatReport	<i>true</i> to write a log entry (with nb rows and columns + writing duration) each time a result is written, <i>false</i> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public HTMLFormat(final ServiceConnection service, final boolean logFormatReport) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("The given service connection is NULL!");

		this.service = service;
		this.logFormatReport = logFormatReport;
	}

	@Override
	public String getMimeType(){
		return "text/html";
	}

	@Override
	public String getShortMimeType(){
		return "html";
	}

	@Override
	public String getDescription(){
		return null;
	}

	@Override
	public String getFileExtension(){
		return ".html";
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, InterruptedException{
		try{
			final long startTime = System.currentTimeMillis();

			// Prepare the output stream:
			final PrintWriter writer = new PrintWriter(output);
			writer.println("<table>");

			// Write header:
			DBColumn[] columns = writeHeader(result, writer, execReport, thread);

			// Write data:
			int nbRows = writeData(result, columns, writer, execReport, thread);

			writer.println("</table>");
			writer.flush();

			// Report stats about the result writing:
			if (logFormatReport)
				service.getLogger().logTAP(LogLevel.INFO, execReport, "FORMAT", "Result formatted (in HTML ; " + nbRows + " rows ; " + columns.length + " columns) in " + (System.currentTimeMillis() - startTime) + "ms!", null);

		}catch(Exception ex){
			service.getLogger().logTAP(LogLevel.ERROR, execReport, "FORMAT", "Error while formatting in HTML!", ex);
		}
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
	protected DBColumn[] writeHeader(TableIterator result, PrintWriter writer, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException{
		// Prepend a description of this result:
		writer.print("<caption>TAP result");
		if (service.getProviderName() != null)
			writer.print(" from " + VOSerializer.formatText(service.getProviderName()));
		writer.print(" on " + ISO8601Format.format(System.currentTimeMillis()));
		writer.print("<br/><em>" + VOSerializer.formatText(execReport.parameters.getQuery()) + "</em>");
		writer.println("</caption>");

		// Get the columns meta:
		DBColumn[] selectedColumns = execReport.resultingColumns;

		// If meta are not known, no header will be written:
		int nbColumns = (selectedColumns == null) ? -1 : selectedColumns.length;
		if (nbColumns > 0){
			writer.println("<thead>");
			writer.print("<tr>");

			// Write all columns' name:
			for(int i = 0; i < nbColumns; i++){
				writer.print("<th>");
				writer.print(VOSerializer.formatText(selectedColumns[i].getADQLName()));
				writer.print("</th>");
			}

			// Go to a new line (in order to prepare the data writing):
			writer.println("</tr>");
			writer.println("</thead>");
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
	 * @param writer			Print writer in which the data must be written.
	 * @param execReport		Execution report (which contains the maximum allowed number of records to output).
	 * @param thread			Thread which has asked for this formatting (it must be used in order to test the {@link Thread#isInterrupted()} flag and so interrupt everything if need).
	 * 
	 * @return	The number of written result rows. (<i>note: if this number is greater than the value of MAXREC: OVERFLOW</i>)
	 * 
	 * @throws IOException				If there is an error while writing something in the output stream.
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws TAPException				If any other error occurs.
	 */
	protected int writeData(TableIterator result, DBColumn[] selectedColumns, PrintWriter writer, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException{
		int nbRows = 0;

		writer.println("<tbody>");

		while(result.nextRow()){
			// Deal with OVERFLOW, if needed:
			if (execReport.parameters.getMaxRec() > 0 && nbRows >= execReport.parameters.getMaxRec()){ // that's to say: OVERFLOW !
				writer.println("<tr class=\"OVERFLOW\"><td colspan=\"" + selectedColumns.length + "\"><em><strong>OVERFLOW</strong> (more rows were available but have been truncated by the TAP service)</em></td></tr>");
				break;
			}

			writer.print("<tr>");

			while(result.hasNextCol()){
				writer.print("<td>");

				// Write the column value:
				Object colVal = result.nextCol();
				if (colVal != null)
					writer.print(VOSerializer.formatText(colVal.toString()));

				writer.print("</td>");

				if (thread.isInterrupted())
					throw new InterruptedException();
			}
			writer.println("</tr>");
			nbRows++;

			if (thread.isInterrupted())
				throw new InterruptedException();
		}

		writer.println("</tbody>");
		writer.flush();

		return nbRows;
	}

}
