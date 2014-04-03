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

import java.io.OutputStream;

import adql.db.DBColumn;

import cds.util.AsciiTable;

import tap.ServiceConnection;

import tap.TAPException;
import tap.TAPExecutionReport;

public abstract class TextFormat<R> implements OutputFormat<R> {

	/** Indicates whether a format report (start and end date/time) must be printed in the log output.  */
	private boolean logFormatReport;

	protected final ServiceConnection<R> service;

	public TextFormat(final ServiceConnection<R> service){
		this(service, false);
	}

	public TextFormat(final ServiceConnection<R> service, final boolean logFormatReport){
		this.service = service;
		this.logFormatReport = logFormatReport;
	}

	public String getMimeType() { return "text/plain"; }

	public String getShortMimeType() { return "text"; }

	public String getDescription() { return null; }

	public String getFileExtension() { return "txt"; }

	@Override
	public void writeResult(R queryResult, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, InterruptedException {
		try{
			AsciiTable asciiTable = new AsciiTable('|');

			final long startTime = System.currentTimeMillis();

			// Write header:
			String headerLine = getHeader(queryResult, execReport, thread);
			asciiTable.addHeaderLine(headerLine);
			asciiTable.endHeaderLine();

			// Write data:
			int nbRows = writeData(queryResult, asciiTable, execReport, thread);

			// Write all lines in the output stream:
			String[] lines = asciiTable.displayAligned(new int[]{AsciiTable.LEFT});
			for(String l : lines){
				output.write(l.getBytes());
				output.write('\n');
			}
			output.flush();

			if (logFormatReport)
				service.getLogger().info("JOB "+execReport.jobID+" WRITTEN\tResult formatted (in text ; "+nbRows+" rows ; "+((execReport != null && execReport.resultingColumns != null)?"?":execReport.resultingColumns.length)+" columns) in "+(System.currentTimeMillis()-startTime)+" ms !");

		}catch(Exception ex){
			service.getLogger().error("While formatting in text/plain !", ex);
		}
	}

	protected abstract String getHeader(final R queryResult, final TAPExecutionReport execReport, final Thread thread) throws TAPException;

	protected abstract int writeData(final R queryResult, final AsciiTable asciiTable, final TAPExecutionReport execReport, final Thread thread) throws TAPException;

	protected void writeFieldValue(final Object value, final DBColumn tapCol, final StringBuffer line){
		Object obj = value;
		if (obj != null)
			line.append(obj.toString());
	}
}
