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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;

import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;

/**
 * Describes an output format and formats a given query result into this format.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * 
 * @version 2.0 (03/2015)
 */
public interface OutputFormat {

	/**
	 * Gets the MIME type corresponding to this format.
	 * 
	 * @return	Its MIME type (MUST BE DIFFERENT FROM NULL).
	 */
	public String getMimeType();

	/**
	 * Gets a short expression of its MIME type.
	 * 
	 * @return	Its short MIME type.
	 */
	public String getShortMimeType();

	/**
	 * Gets a description of this format.
	 * 
	 * @return	Its description.
	 */
	public String getDescription();

	/**
	 * Gets a file extension for this format.
	 * 
	 * @return	Its file extension.
	 */
	public String getFileExtension();

	/**
	 * <p>Formats the given query result and writes it in the given output stream.</p>
	 * 
	 * <p><i>Note: the given output stream should not be closed at the end of this function. It is up to the called to do it.</i></p>
	 * 
	 * @param result		The raw (table) result to format.
	 * @param output		The output stream (a ServletOutputStream or a stream on a file) in which the formatted result must be written.
	 * @param execReport	The report of the execution of the TAP query whose the result must be now written.
	 * @param thread		The thread which has asked the result writing.
	 * 
	 * @throws TAPException			If there is an error while formatting the query result.
	 * @throws IOException			If any error occurs while writing into the given stream.
	 * @throws InterruptedException	If the query has been interrupted/aborted.
	 */
	public void writeResult(final TableIterator result, final OutputStream output, final TAPExecutionReport execReport, final Thread thread) throws TAPException, IOException, InterruptedException;

}
