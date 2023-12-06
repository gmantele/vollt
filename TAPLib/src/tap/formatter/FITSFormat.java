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
 * Copyright 2014-2020 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
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

/**
 * Format any given query (table) result into FITS.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (08/2020)
 * @since 2.0
 */
public class FITSFormat implements OutputFormat {

	/** The {@link ServiceConnection} to use (for the log and to have some
	 * information about the service (particularly: name, description). */
	protected final ServiceConnection service;

	/**
	 * Creates a FITS formatter.
	 *
	 * @param service	The service to use (for the log and to have some
	 *               	information about the service (particularly: name,
	 *               	description).
	 *
	 * @throws NullPointerException	If the given service connection is NULL.
	 */
	public FITSFormat(final ServiceConnection service) throws NullPointerException {
		if (service == null)
			throw new NullPointerException("The given service connection is NULL !");

		this.service = service;
	}

	@Override
	public String getMimeType() {
		return "application/fits";
	}

	@Override
	public String getShortMimeType() {
		return "fits";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getFileExtension() {
		return "fits";
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread) throws TAPException, IOException, InterruptedException {
		// Extract the columns' metadata:
		ColumnInfo[] colInfos = VOTableFormat.toColumnInfos(result, execReport, thread);

		// Turns the result set into a table:
		LimitedStarTable table = new LimitedStarTable(result, colInfos, execReport.parameters.getMaxRec(), thread);

		// Copy the table on disk (or in memory if the table is short):
		StarTable copyTable;
		try {
			copyTable = StoragePolicy.PREFER_DISK.copyTable(table);
		} catch(IOException ioe) {
			/* In case of time out, LimitedStarTable makes copyTable to stop by
			 * throwing an IOException. In such case, this IOException has to be
			 * interpreted as a normal interruption: */
			if (thread.isInterrupted())
				throw new InterruptedException();
			/* Otherwise, the error has to be managed properly (so, wrap it
			 * inside a TAPException): */
			else
				throw new TAPException("Unexpected error while formatting the result!", ioe);
		}

		if (thread.isInterrupted())
			throw new InterruptedException();

		/* Format the table in FITS (2 passes are needed for that, hence the copy on disk),
		 * and write it in the given output stream: */
		new FitsTableWriter().writeStarTable(copyTable, output);

		if (thread.isInterrupted())
			throw new InterruptedException();

		execReport.nbRows = table.getNbReadRows();

		output.flush();
	}

}
