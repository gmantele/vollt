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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;
import uk.ac.starlink.votable.VOSerializer;
import uws.ISO8601Format;
import adql.db.DBColumn;

/**
 * Format any given query (table) result into HTML.
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (10/2014)
 * @since 2.0
 */
public class HTMLFormat implements OutputFormat {

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
		if (service == null)
			throw new NullPointerException("The given service connection is NULL!");

		this.service = service;
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
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, IOException, InterruptedException{
		// Prepare the output stream:
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
		writer.write("<table>");
		writer.newLine();

		// Write header:
		DBColumn[] columns = writeHeader(result, writer, execReport, thread);

		if (thread.isInterrupted())
			throw new InterruptedException();

		// Write data:
		writeData(result, columns, writer, execReport, thread);

		writer.write("</table>");
		writer.newLine();
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
		// Prepend a description of this result:
		writer.write("<caption>TAP result");
		if (service.getProviderName() != null)
			writer.write(" from " + VOSerializer.formatText(service.getProviderName()));
		writer.write(" on " + ISO8601Format.format(System.currentTimeMillis()));
		writer.write("<br/><em>" + VOSerializer.formatText(execReport.parameters.getQuery()) + "</em>");
		writer.write("</caption>");
		writer.newLine();

		// Get the columns meta:
		DBColumn[] selectedColumns = execReport.resultingColumns;

		// If meta are not known, no header will be written:
		int nbColumns = (selectedColumns == null) ? -1 : selectedColumns.length;
		if (nbColumns > 0){
			writer.write("<thead>");
			writer.newLine();
			writer.write("<tr>");

			// Write all columns' name:
			for(int i = 0; i < nbColumns; i++){
				writer.write("<th>");
				writer.write(VOSerializer.formatText(selectedColumns[i].getADQLName()));
				writer.write("</th>");
			}

			// Go to a new line (in order to prepare the data writing):
			writer.write("</tr>");
			writer.newLine();
			writer.write("</thead>");
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
	 * @param writer			Print writer in which the data must be written.
	 * @param execReport		Execution report (which contains the maximum allowed number of records to output).
	 * @param thread			Thread which has asked for this formatting (it must be used in order to test the {@link Thread#isInterrupted()} flag and so interrupt everything if need).
	 * 
	 * @throws IOException				If there is an error while writing something in the output stream.
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws TAPException				If any other error occurs.
	 */
	protected void writeData(TableIterator result, DBColumn[] selectedColumns, BufferedWriter writer, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException{
		execReport.nbRows = 0;

		writer.write("<tbody>");
		writer.newLine();

		while(result.nextRow()){
			// Stop right now the formatting if the job has been aborted/canceled/interrupted:
			if (thread.isInterrupted())
				throw new InterruptedException();

			// Deal with OVERFLOW, if needed:
			if (execReport.parameters.getMaxRec() > 0 && execReport.nbRows >= execReport.parameters.getMaxRec()){ // that's to say: OVERFLOW !
				writer.write("<tr class=\"OVERFLOW\"><td colspan=\"" + selectedColumns.length + "\"><em><strong>OVERFLOW</strong> (more rows were available but have been truncated by the TAP service)</em></td></tr>");
				writer.newLine();
				break;
			}

			writer.write("<tr>");

			while(result.hasNextCol()){
				writer.write("<td>");

				// Write the column value:
				Object colVal = result.nextCol();
				if (colVal != null)
					writer.write(VOSerializer.formatText(colVal.toString()));

				writer.write("</td>");

				if (thread.isInterrupted())
					throw new InterruptedException();
			}
			writer.write("</tr>");
			writer.newLine();
			execReport.nbRows++;

			// flush the writer every 30 lines:
			if (execReport.nbRows % 30 == 0)
				writer.flush();
		}

		writer.write("</tbody>");
		writer.newLine();
		writer.flush();
	}

}
