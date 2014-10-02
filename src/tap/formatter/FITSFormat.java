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

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;
import tap.formatter.VOTableFormat.LimitedStarTable;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uws.service.log.UWSLog.LogLevel;

/**
 * Format any given query (table) result into FITS.
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (10/2014)
 * @since 2.0
 */
public class FITSFormat implements OutputFormat {

	/** Indicates whether a format report (start and end date/time) must be printed in the log output.  */
	private boolean logFormatReport;

	/** The {@link ServiceConnection} to use (for the log and to have some information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/**
	 * Creates a FITS formatter.
	 * 
	 * @param service	The service to use (for the log and to have some information about the service (particularly: name, description).
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public FITSFormat(final ServiceConnection service) throws NullPointerException{
		this(service, true);
	}

	/**
	 * Creates a FITS formatter.
	 * 
	 * @param service				The service to use (for the log and to have some information about the service (particularly: name, description).
	 * @param logFormatReport		<code>true</code> to append a format report (start and end date/time) in the log output, <code>false</code> otherwise.
	 * 
	 * @throws NullPointerException	If the given service connection is <code>null</code>.
	 */
	public FITSFormat(final ServiceConnection service, final boolean logFormatReport) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("The given service connection is NULL !");

		this.service = service;
		this.logFormatReport = logFormatReport;
	}

	@Override
	public String getMimeType(){
		return "application/fits";
	}

	@Override
	public String getShortMimeType(){
		return "fits";
	}

	@Override
	public String getDescription(){
		return null;
	}

	@Override
	public String getFileExtension(){
		return "fits";
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, InterruptedException{
		try{
			long start = System.currentTimeMillis();

			// Extract the columns' metadata:
			ColumnInfo[] colInfos = VOTableFormat.toColumnInfos(result, execReport, thread);

			// Turns the result set into a table:
			LimitedStarTable table = new LimitedStarTable(result, colInfos, execReport.parameters.getMaxRec());

			// Copy the table on disk (or in memory if the table is short):
			StarTable copyTable = StoragePolicy.PREFER_DISK.copyTable(table);

			/* Format the table in FITS (2 passes are needed for that, hence the copy on disk),
			 * and write it in the given output stream: */
			new FitsTableWriter().writeStarTable(copyTable, output);

			output.flush();

			if (logFormatReport)
				service.getLogger().logTAP(LogLevel.INFO, execReport, "FORMAT", "Result formatted (in FITS ; " + table.getNbReadRows() + " rows ; " + table.getColumnCount() + " columns) in " + (System.currentTimeMillis() - start) + "ms!", null);
		}catch(IOException ioe){
			throw new TAPException("Error while writing a query result in FITS!", ioe);
		}
	}

}
