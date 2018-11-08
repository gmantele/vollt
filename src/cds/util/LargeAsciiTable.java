package cds.util;

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
 * Copyright 2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * 	An object of this class manages a large ASCII table: it receives (non-empty)
 * 	lines to add. Each of these lines is made of columns separated by a given
 * 	separator char. Columns can be aligned ({@link #RIGHT}, {@link #LEFT} or
 * 	{@link #CENTER}) before usage.
 * </p>
 *
 * <h3>Usage example</h3>
 *
 * <p>A typical usage of this class is the following:</p>
 * <pre>
 *   try({@link LargeAsciiTable} myTable = new {@link #LargeAsciiTable(char) LargeAsciiTable('|')}){
 *     // First, set the header (if one is needed):
 *     myTable.{@link #addHeaderLine(String) addHeaderLine("col1|col2")};
 *
 *     // Then add lines:
 *     myTable.{@link #addLine(String) addLine("val1.1|val1.2")};
 *     myTable.{@link #addLine(String) addLine("val2.1|val2.2")};
 *     // ...
 *
 *     // End the table:
 *     myTable.{@link #endTable()};
 *
 *     // Display in the standard output stream the aligned table:
 *     {@link LineProcessor} showLine = new LineProcessor(){
 *       public boolean process(final String alignedLine) throws LineProcessorException{
 *       	System.out.println(alignedLine);
 *          return true;
 *       }
 *     };
 *     final int[] alignmentPolicy = new int[]{{@link #RIGHT}, {@link #CENTER}};
 *     myTable.{@link #streamAligned(LineProcessor, int[]) streamAligned(showLine, alignmentPolicy)};
 *   }
 * </pre>
 *
 * <h3>{@link AsciiTable} vs {@link LargeAsciiTable}</h3>
 *
 * <p>
 * 	The main difference between an {@link AsciiTable} and a
 * 	{@link LargeAsciiTable} is how both are storing the table.
 * 	{@link AsciiTable} is keeping the table entirely in memory, while
 * 	{@link LargeAsciiTable} uses a temporary file after a given number of lines
 * 	(see {@link #DEFAULT_MEMORY_THRESHOLD}). Consequently, using
 * 	{@link AsciiTable} for large tables will consume a lot of memory and my
 * 	trigger an Out Of Memory error. Hence this {@link LargeAsciiTable} variant,
 * 	better suited for such tables.
 * </p>
 *
 * <p><em><strong>Note:</strong>
 * 	It is possible to change the memory threshold ONLY at creation
 * 	({@link #LargeAsciiTable(char, int)}) and when reseting a table
 * 	({@link #reset(char, int)}).
 * </em></p>
 *
 * <h3>Functions mapping</h3>
 *
 * <table border="1">
 * 	<tr><th>AsciiTable</th><th>LargeAsciiTable</th></tr>
 * 	<tr>
 * 		<td>{@link AsciiTable#displayRaw(char) displayRaw(char)}</td>
 * 		<td>{@link #streamRaw(LineProcessor, char)}</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link AsciiTable#displayAligned(int[], char, Thread) displayAligned(int[], char, Thread)}</td>
 * 		<td>{@link #streamAligned(LineProcessor, int[], char, Thread)}</td>
 * 	</tr>
 * </table>
 *
 * <p>
 * 	Instead of returning every lines in a String array, {@link LargeAsciiTable}
 * 	returns the number of processed lines. Each line can be accessed/processed
 * 	thanks to the given {@link LineProcessor}. In this way, only memory required
 * 	for the current line is used in the JVM.
 * </p>
 *
 * <h3>Requirements</h3>
 *
 * <p>
 * 	Because we may now rely on a file, it is required to close the table and
 * 	delete the file once the large ASCII table is no longer needed. This can
 * 	be achieved by the function {@link #close()}.
 * </p>
 *
 * <em>
 * 	<p><strong>Note 1:</strong>
 * 		This class implements the interface {@link AutoCloseable}. This makes
 * 		the creation and resources release much easier:
 * 	</p>
 * 	<pre>
 *   try({@link LargeAsciiTable} myLargeTable = new LargeAsciiTable('|')){
 *     ...
 *   }
 * 	</pre>
 * </em>
 *
 * <p><em><strong>Note 2:</strong>
 * 	Once a {@link LargeAsciiTable} is closed, no more line can be added or
 * 	accessed. But, it is possible to reuse the same instance to build another
 * 	table. For that, one must use the function {@link #reset(char)}.
 * </em></p>
 *
 * <p>
 * 	In addition, in order to ensure data consistency, it is required to declare
 * 	when the table content is complete. See {@link #endTable()}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @author Marc Wenger (CDS)
 *
 * @version 2.3 (11/2018)
 * @since 2.3
 */
public class LargeAsciiTable implements AutoCloseable {

	/** Flag to left align a column content. */
	public static final int LEFT = 0;

	/** Flag to center align a column content. */
	public static final int CENTER = 1;

	/** Flag to right align a column content. */
	public static final int RIGHT = 2;

	/** Error message returned when a table was expected to be IN-complete. */
	protected static final String ERROR_TABLE_COMPLETE = "This table is already complete! No more data can be added. Hint: it must be closed first ; call close() on your LargeAsciiTable.";

	/** Error message returned when a table was expected to be complete. */
	protected static final String ERROR_TABLE_NOT_COMPLETE = "Table not already complete! Impossible to display its current content. Hint: call endTable() on your LargeAsciiTable before attempting to read its content.";

	/** Error message returned when a table was expected to be open. */
	protected static final String ERROR_TABLE_CLOSED = "This table is closed! It can be neither updated nor streamed. ";

	/** Error message returned when the header of a table was expected to be
	 * still open. */
	protected static final String ERROR_HEADER_CLOSED = "Header closed! Impossible to update the header ; there are already table data.";

	/** Error message returned when no more line can be got in an
	 * {@link AsciiTableIterator}.
	 * <em>This error is generally thrown by the next() function.</em> */
	protected static final String ERROR_NO_MORE_LINE = "No more line available!";

	/** Default number of lines to keep in memory.
	 * @see #memoryThreshold */
	public final static int DEFAULT_MEMORY_THRESHOLD = 1000;

	/** Maximum number of lines that can be hold in memory.
	 * <p>
	 * 	If more lines are added, this {@link LargeAsciiTable} will store all
	 * 	lines in a temporary file.
	 * </p>
	 * <p><em>
	 * 	By default set to {@value #DEFAULT_MEMORY_THRESHOLD} lines.
	 * </em></p> */
	protected int memoryThreshold = DEFAULT_MEMORY_THRESHOLD;

	/** List of lines ONLY IF the complete table fits in memory (defined by
	 * {@link #memoryThreshold}). If not, the table is stored in a temporary
	 * file: {@link #bufferFile}.
	 * <p><strong>Important:
	 * 	No empty and NULL lines are allowed in this array.</strong>
	 * 	All the functions of {@link LargeAsciiTable} relies on this rule ;
	 * 	breaking it may trigger the generation of NullPointerException at some
	 * 	point.
	 * </p> */
	protected String[] lines;

	/** Number of lines currently stored in memory - {@link #lines}. */
	protected int nbLines = 0;

	/** Number of lines stored in this table (in memory + on disk, if used). */
	protected long nbTotalLines = 0;

	/** Indicate whether this table is empty. */
	protected boolean empty = true;

	/** Number of lines in the header.
	 * <p><strong>Important:</strong>
	 * 	The header lines are always the lines starting the table.
	 * 	All lines from {@link #headerSize} (included) are the table lines/data.
	 * </p> */
	protected int headerSize = 0;

	/** String separating the header from the table lines. */
	protected String headerPostfix;

	/** Indicate whether this table is complete.
	 * <p><code>true</code> if no more lines can be added.</p> */
	protected boolean complete = false;

	/** Indicate whether this table is closed.
	 * <p>
	 * 	A closed table has no content and uses as less as memory as possible.
	 * </p>
	 * <p><strong>Important:</strong>
	 * 	In order to use a closed table, one must call {@link #reset(char)}
	 * 	first.
	 * </p> */
	protected boolean closed = true;

	/** File containing all the lines previously added and not fitting in memory
	 * according to {@link #memoryThreshold}.
	 * <p>If NULL, no file has been used.</p> */
	protected File bufferFile;

	/** Stream used to update the buffer file.
	 * <p>NULL if no buffer file is used or if this table is closed.</p> */
	protected ObjectOutputStream out;

	/** Buffer of spaces to use when aligning a column value.
	 * <p><em><strong>Note:</strong>
	 * 	It can be extended by {@link #addspaces(StringBuffer, int)} if needed.
	 * </em></p> */
	protected StringBuffer SPACES;

	/** Buffer of column separators to use when formatting a line.
	 * <p><em><strong>Note:</strong>
	 * 	It can be extended by {@link #addHsep(StringBuffer, int)} if needed.
	 * </em></p> */
	protected StringBuffer HSEP;

	/** Size of the columns. */
	protected int[] sizes;

	/** Column separator (as char). */
	protected char csep;

	/** Column separator (as string). */
	protected String sep;

	/** <strong><em>Only for JUnit tests.</em></strong>  */
	LargeAsciiTable(){
	}

	/**
	 * Constructor.
	 *
	 * @param separ	Character defining the column separator in the input lines.
	 */
	public LargeAsciiTable(final char separ){
		this(separ, DEFAULT_MEMORY_THRESHOLD);
	}

	/**
	 * Constructor.
	 *
	 * @param separ				Character defining the column separator in the
	 *             				input lines.
	 * @param memoryThreshold	Maximum number of lines that can be hold in
	 *                       	memory. If more lines are added, the whole table
	 *                       	will be temporarily stored on disk.
	 *                       	<em>If &le; 0, it is automatically set to
	 *                       	{@value #DEFAULT_MEMORY_THRESHOLD} lines.</em>
	 */
	public LargeAsciiTable(final char separ, final int memoryThreshold){
		reset(separ, memoryThreshold);
	}

	/**
	 * Get the number of lines stored in this {@link LargeAsciiTable} instance.
	 *
	 * <p><em><strong>Note:</strong>
	 * 	The returned number includes the size of the header and the separator
	 * 	line (only if there is a header block).
	 * </em></p>
	 *
	 * @return	How many lines currently stored in there.
	 */
	public final long size(){
		return (headerSize > 0) ? nbTotalLines + 1 : nbTotalLines;
	}

	/**
	 * Get the number of lines composing the header block of this table.
	 *
	 * <p><em><strong>Note:</strong>
	 * 	The returned number does not include the separator line.
	 * </p>
	 *
	 * @return	How many lines in the header block of this table.
	 */
	public final int headerSize(){
		return headerSize;
	}

	/**
	 * Get the number of lines above which the entire table will be stored on
	 * disk (in a temporary file).
	 *
	 * <p><strong>Important:</strong>
	 * 	This threshold can be set only when
	 * 	{@link #LargeAsciiTable(char, int) creating} or
	 * 	{@link #reset(char, int) reseting} a {@link LargeAsciiTable}.
	 * </p>
	 *
	 * @return	Maximum size (in nb of lines) of a table to keep ONLY in memory.
	 */
	public final int getMemoryThreshold(){
		return memoryThreshold;
	}

	/**
	 * Get the separator between the header and the table lines.
	 *
	 * @return	The string appended after the header lines.
	 */
	public final String getHeaderPostfix(){
		return headerPostfix;
	}

	/**
	 * Set the separator between the header and the table lines.
	 *
	 * @param postfix	String to append after the header lines.
	 */
	public final void setHeaderPostfix(final String postfix){
		headerPostfix = postfix;
	}

	/**
	* Reset this {@link LargeAsciiTable} so that lines can be safely
	* added.
	*
	* <p><strong>Warning!</strong>
	* 	This function starts by closing this {@link LargeAsciiTable}. Thus, all
	* 	lines already stored inside it will be completely discarded/lost.
	* </p>
	*
	* @param separ				Character defining the column separator in the
	*             				input lines.
	*/
	public final void reset(final char separ){
		reset(separ, DEFAULT_MEMORY_THRESHOLD);
	}

	/**
	 * Reset this {@link LargeAsciiTable} so that lines can be safely
	 * added.
	 *
	 * <p><strong>Warning!</strong>
	 * 	This function starts by closing this {@link LargeAsciiTable}. Thus, all
	 * 	lines already stored inside it will be completely discarded/lost.
	 * </p>
	 *
	 * @param separ				Character defining the column separator in the
	 *             				input lines.
	 * @param memoryThreshold	Maximum number of lines that can be hold in
	 *                       	memory. If more lines are added, the whole table
	 *                       	will be temporarily stored on disk.
	 *                       	<em>If &le; 0, it is automatically set to
	 *                       	{@value #DEFAULT_MEMORY_THRESHOLD} lines.</em>
	 */
	public void reset(final char separ, final int memoryThreshold){
		// First, close this table, if opened:
		close();

		// Set the new separator:
		csep = separ;
		sep = String.valueOf(csep);

		// Reset all variables of this table:
		// ...memory threshold:
		this.memoryThreshold = (memoryThreshold <= 0) ? DEFAULT_MEMORY_THRESHOLD : memoryThreshold;
		// ...lines content:
		lines = new String[this.memoryThreshold];
		bufferFile = null;
		out = null;
		// ...line counters:
		nbLines = 0;
		headerSize = 0;
		nbTotalLines = 0;
		// ...space helpers:
		SPACES = new StringBuffer("          ");
		HSEP = new StringBuffer("--------------------");
		sizes = null;
		// ...flags:
		empty = true;
		headerPostfix = null;
		complete = false;
		closed = false;
	}

	/**
	 * Free all resources associated with this {@link LargeAsciiTable}:
	 *
	 * <ul>
	 * 	<li>close the output stream toward the temporary buffer file, if open,</li>
	 * 	<li>delete the temporary buffer file,</li>
	 * 	<li>discard all memorised lines,</li>
	 * 	<li>and forget about the given header Postfix and spaces.</li>
	 * </ul>
	 *
	 * <p><em><strong>Note:</strong>
	 * 	Once closed, a {@link LargeAsciiTable} can be re-used to store another
	 * 	table. For that, call the function {@link #reset(char)} (which by
	 * 	default starts by calling {@link #close()}).
	 * </em></p>
	 *
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close(){
		// Close the output stream, if open:
		if (out != null){
			// ...silently close the stream:
			try{
				out.close();
			}catch(IOException ioe){
			}
			// ... release the allocated memory:
			out = null;
		}

		// Delete the temporary file, if existing:
		if (bufferFile != null){
			bufferFile.delete();
			bufferFile = null;
		}

		// Discard all memorised lines, if any:
		lines = null;
		complete = true;
		headerPostfix = null;
		SPACES = null;
		HSEP = null;
		sizes = null;
		sep = null;

		// Finally declare this table is closed:
		closed = true;
	}

	/**
	 * Return the whole table, with left alignment on all columns.
	 *
	 * <p><strong>Warning!</strong>
	 * 	This operation is very likely to consume a lot of memory as the	table
	 * 	will have to fit entirely in memory, in a single String.
	 * 	<em>In such case, it is possible to stop this function call by
	 * 	interrupting (cf {@link Thread#interrupt()}) the thread running it.</em>
	 * </p>
	 *
	 * @return	The entire table as a unique String.
	 */
	@Override
	public String toString(){
		try{
			LineProcessor lineAppender = new LineProcessor() {
				private StringBuffer buf = new StringBuffer();

				@Override
				public String toString(){
					return buf.toString();
				}

				@Override
				public boolean process(String line) throws LineProcessorException{
					if (buf.length() > 0)
						buf.append('\n');
					buf.append(line);
					return true;
				}
			};
			streamAligned(lineAppender, new int[]{ LargeAsciiTable.LEFT });

			return lineAppender.toString();
		}catch(IOException ioe){
			return "!!! Operation failed! Cause: " + ioe.getMessage() + " !!!";
		}catch(InterruptedException ie){
			return "!!! Operation interrupted !!!";
		}catch(LineProcessorException lpe){
			return "!!! Operation failed! Cause: " + lpe.getMessage() + " !!!";
		}
	}

	/* **********************************************************************
	 * * CONTENT UPDATE                                                     *
	 * ********************************************************************** */

	/**
	 * Add a header line. <em>Several lines of header can be defined.</em>
	 *
	 * @param headerline	Header line to add.
	 *
	 * @throws IOException				If there is any error while updating
	 *                    				this table header.
	 * @throws IllegalStateException	If this {@link LargeAsciiTable} is
	 *                              	closed or already complete.
	 */
	public boolean addHeaderLine(final String headerline) throws IOException, IllegalStateException{
		// Impossible to update a closed table:
		if (closed)
			throw new IllegalStateException(ERROR_TABLE_CLOSED);
		// Prevent modification if table already complete:
		else if (complete)
			throw new IllegalStateException(ERROR_TABLE_COMPLETE);
		// Prevent modification if some table data are already in there:
		else if (nbTotalLines > headerSize)
			throw new IllegalStateException(ERROR_HEADER_CLOSED);

		// Add the header line:
		if (addLine(headerline)){
			// ...and count it as header line only if successfully added:
			headerSize++;
			return true;
		}else
			return false;
	}

	/**
	 * Declare this table as complete.
	 *
	 * <p><strong>Warning:</strong>
	 * 	Once this function called, it is no longer possible to add new line to
	 * 	this {@link LargeAsciiTable} instance. Any further call to functions
	 * 	like {@link #addLine(String)} will throw an
	 * 	{@link IllegalStateException}.
	 * </p>
	 *
	 * <p><em><strong>Note:</strong>
	 * 	Calling this function on an already complete table has no effect.
	 * </em></p>
	 *
	 * @throws IOException	If there is an error while closing the buffer file.
	 */
	public void endTable() throws IOException{
		// Nothing to do if already complete or closed:
		if (closed || complete)
			return;

		// Remember this table is complete:
		complete = true;

		// Close the output stream, if open:
		if (out != null){
			// append all remaining memorised lines to the buffer file:
			appendToBufferFile();

			// declare that no more rows is available in this buffer:
			out.writeInt(0);
			out.flush();

			// finally close the stream:
			out.close();
			out = null;

			// ...and free the memory allocated for the memorised lines:
			lines = null;
		}
	}

	/**
	 * Add a line to the table.
	 *
	 * <p><strong>Important:</strong>
	 * 	The line should not end up with a newline char. If it is the case,
	 * 	alignment errors can be experienced depending on the alignment type of
	 * 	the last column.
	 * </p>
	 *
	 * <p><strong>Warning!</strong>
	 * 	This function determines the number of columns ONLY with the first added
	 * 	line. Consequently, if some lines have a different number of columns,
	 * 	the measured size of all table columns may be unexpected. This is
	 * 	especially true	in the two following cases:
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		If a line has <em>less columns</em> than the first line, only the
	 * 		size of the first columns will be updated for this new line.
	 * 	</li>
	 * 	<li>
	 * 		If a line has <em>more columns</em> than the first line, the
	 * 		additional columns will be appended inside the last column
	 * 		identified by the first line.
	 * 	</li>
	 * </ul>
	 *
	 * @param line	String containing the line with all the columns separated by
	 *            	the column separator.
	 *
	 * @throws IOException				If there is any error while updating
	 *                    				this table content.
	 * @throws IllegalStateException	If this {@link LargeAsciiTable} is
	 *                              	closed or already complete.
	 */
	public boolean addLine(final String line) throws IOException, IllegalStateException{
		// Impossible to update a closed table:
		if (closed)
			throw new IllegalStateException(ERROR_TABLE_CLOSED);
		// Prevent modification if table already complete:
		else if (complete)
			throw new IllegalStateException(ERROR_TABLE_COMPLETE);
		// Empty line => nothing to add:
		else if (line == null || line.trim().length() == 0)
			return false;

		/* If the allocated memory is currently full, copy the memorised lines
		 * on disk and empty the memory: */
		if (nbLines >= memoryThreshold)
			appendToBufferFile();

		// Compute the number of columns, if we add the first line
		if (empty){
			int sepPos = 0;
			int nbcol = 1;	// at least one column (also: there is one separator less than columns)
			boolean done = false;
			while(!done){
				sepPos = line.indexOf(sep, sepPos);
				if (sepPos >= 0)
					nbcol++;
				else
					done = true;
				sepPos++;
			}
			// initialise the result
			sizes = new int[nbcol];
			for(int i = 0; i < sizes.length; i++){
				sizes[i] = 0;
			}
			empty = false;
		}

		// Get the max size for each column:
		int sepPos0, sepPos1, col, colsize;
		sepPos0 = 0;
		col = 0;
		while(sepPos0 < line.length()){
			/* if last column, consider additional columns as part of the last
			 * expected column: */
			if (col == sizes.length - 1)
				sepPos1 = line.length();
			// otherwise, search for the next column separator:
			else{
				sepPos1 = line.indexOf(sep, sepPos0);
				// if none found, take all remaining characters:
				if (sepPos1 < 0)
					sepPos1 = line.length();
			}

			// measure the column size:
			colsize = sepPos1 - sepPos0;

			// keep only the maximum:
			sizes[col] = Math.max(sizes[col], colsize);

			// update the beginning of the next column:
			sepPos0 = sepPos1 + 1;
			// ...and the column counter:
			col++;
		}

		lines[nbLines++] = line;
		nbTotalLines++;

		return true;
	}

	/**
	 * Append all the memorised lines at the end of the buffer file.
	 *
	 * <p><em><strong>Note:</strong>
	 * 	If the buffer file does not exist yet, it will be created.
	 * </em></p>
	 *
	 * @throws IOException	If there is an error while creating or opening the
	 *                    	buffer file, or when writing inside it.
	 */
	protected void appendToBufferFile() throws IOException{
		// Create the buffer file, if needed:
		if (bufferFile == null){
			bufferFile = File.createTempFile("asciitable_" + System.currentTimeMillis(), ".buffer");
		}

		// Open the output stream, if needed:
		if (out == null)
			out = new ObjectOutputStream(new FileOutputStream(bufferFile, true));

		// Append the memorised lines at the end of this file:
		out.writeInt(nbLines);
		for(int i = 0; i < nbLines; i++){
			out.writeUnshared(lines[i]);
			lines[i] = null;
		}
		out.flush();

		// "Clear" the memory:
		nbLines = 0;
	}

	/* **********************************************************************
	 * * CONTENT ACCESS                                                     *
	 * ********************************************************************** */

	/**
	 * Object letting process any given line.
	 *
	 * <p><em><strong>Note:</strong>
	 * 	This interface is mainly used by the streaming functions in
	 * {@link LargeAsciiTable}, in order to process all streamed lines.
	 * </em></p>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.3 (11/2018)
	 *
	 * @see LargeAsciiTable#streamRaw(LineProcessor, char)
	 * @see LargeAsciiTable#streamAligned(LineProcessor, int[], char, Thread)
	 */
	public static interface LineProcessor {
		/**
		 * Process the given line.
		 *
		 * @param line	Line to process. <em>May be NULL.</em>
		 *
		 * @return	<code>true</code> if the line has been successfully
		 *         	processed,
		 *         	<code>false</code> otherwise.
		 *
		 * @throws LineProcessorException	If there was a grave error while
		 *                               	processing the given line.
		 */
		public boolean process(final String line) throws LineProcessorException;
	}

	/**
	 * Exception thrown when processing a line streamed by
	 * {@link LargeAsciiTable}. Such exception is expected to be initially
	 * thrown by a {@link LineProcessor} instance.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.3 (11/2018)
	 */
	public static class LineProcessorException extends Exception {
		private static final long serialVersionUID = 1L;

		public LineProcessorException(){
			super();
		}

		public LineProcessorException(final String message){
			super(message);
		}

		public LineProcessorException(final Throwable cause){
			super(cause);
		}

		public LineProcessorException(final String message, final Throwable cause){
			super(message, cause);
		}
	}

	/**
	 * Give to the given {@link LineProcessor} all the lines without alignment,
	 * as they were added.
	 *
	 * <p><em><strong>Note:</strong>
	 * 	This function does nothing and returns <code>-1</code> if no
	 * 	{@link LineProcessor} is provided.
	 * </em></p>
	 *
	 * @param lineProc	The action to perform on each line.
	 *
	 * @return	The number of successfully processed lines (including the
	 *        	header).
	 *
	 * @throws IllegalStateException	If this {@link LargeAsciiTable} is not
	 *                              	complete (i.e. if {@link #endTable()}
	 *                              	has not been called before this
	 *                              	function).
	 * @throws InterruptedException		If the current thread has been
	 *                             		interrupted.
	 *                             		<em>This interruption is useful to stop
	 *                             		this streaming operation whenever it
	 *                             		becomes time and memory consuming.</em>
	 * @throws LineProcessorException	If there was an error while processing
	 *                               	a line.
	 *                               	<em>A such exception is generally thrown
	 *                               	by the given {@link LineProcessor}.</em>
	 * @throws IOException				If there is any error while accessing
	 *                    				the buffer file, or while processing a
	 *                    				line (if this processing implies I/O).
	 */
	public final long streamRaw(final LineProcessor lineProc) throws IllegalStateException, LineProcessorException, IOException, InterruptedException{
		return streamRaw(lineProc, csep);
	}

	/**
	 * Give to the given {@link LineProcessor} all the lines without alignment,
	 * as they were added, potentially with some separator control.
	 *
	 * <p><em><strong>Note:</strong>
	 * 	This function does nothing and returns <code>-1</code> if no
	 * 	{@link LineProcessor} is provided.
	 * </em></p>
	 *
	 * @param lineProc	The action to perform on each line.
	 * @param newsep	Separator to use, replacing the original one.
	 *
	 * @return	The number of successfully processed lines (including the
	 *        	header),
	 *        	or <code>-1</code> if nothing has been done.
	 *
	 * @throws IllegalStateException	If this {@link LargeAsciiTable} is not
	 *                              	complete (i.e. if {@link #endTable()}
	 *                              	has not been called before this
	 *                              	function).
	 * @throws InterruptedException		If the current thread has been
	 *                             		interrupted.
	 *                             		<em>This interruption is useful to stop
	 *                             		this streaming operation whenever it
	 *                             		becomes time and memory consuming.</em>
	 * @throws LineProcessorException	If there was an error while processing
	 *                               	a line.
	 *                               	<em>A such exception is generally thrown
	 *                               	by the given {@link LineProcessor}.</em>
	 * @throws IOException				If there is any error while accessing
	 *                    				the buffer file, or while processing a
	 *                    				line (if this processing implies I/O).
	 */
	public long streamRaw(final LineProcessor lineProc, final char newsep) throws IllegalStateException, LineProcessorException, IOException, InterruptedException{
		// Nothing to do here if nothing to do with the streamed lines:
		if (lineProc == null)
			return -1;

		long nbTotalLines = 0;
		try(AsciiTableIterator itLines = getIterator()){
			boolean success;
			String line;
			// For each line...
			while(itLines.hasNext()){

				// stop everything if this thread has been interrupted:
				if (Thread.currentThread().isInterrupted())
					throw new InterruptedException();

				// ...fetch the line:
				line = itLines.next();

				// ...change the separator, if needed:
				if (newsep != csep)
					line = line.replace(csep, newsep);

				// ...process the line:
				success = lineProc.process(line);

				// ...count only successfully processed lines:
				if (success)
					nbTotalLines++;
			}
		}
		return nbTotalLines;
	}

	/**
	 * Give to the given {@link LineProcessor} all lines in which all columns
	 * are aligned.
	 *
	 * <p><strong>IMPORTANT:</strong>
	 * 	The array - pos - should have as many columns as the table has. Each
	 * 	column can contain either {@link LargeAsciiTable#LEFT LEFT},
	 * 	{@link LargeAsciiTable#CENTER CENTER} or
	 * 	{@link LargeAsciiTable#RIGHT RIGHT}.
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		If the array contains only ONE item, it will be used for every
	 * 		column.
	 *	</li>
	 *	<li>
	 *		If the array contains less items than columns to align, the last
	 *		item will be used for all the non-specified alignment.
	 *	</li>
	 *	<li>
	 *		If no alignment at all is specified, the
	 *		{@link LargeAsciiTable#LEFT LEFT} alignment will be used.
	 * 	</li>
	 * </ul>
	 *
	 * <p><em><strong>Note:</strong>
	 * 	This function does nothing and returns <code>-1</code> if no
	 * 	{@link LineProcessor} is provided.
	 * </em></p>
	 *
	 * @param lineProc	The action to perform on each line.
	 * @param pos		Array of flags, indicating how each column should be
	 *           		justified.
	 *
	 * @return	Number of <em>successfully</em> processed lines.
	 *        	<em>This number could be compared to the value returned by
	 *        	{@link #size()} in order to know whether all lines were
	 *        	successfully aligned and processed.</em>
	 *
	 * @throws IllegalStateException	If this {@link LargeAsciiTable} is not
	 *                              	complete (i.e. if {@link #endTable()}
	 *                              	has not been called before this
	 *                              	function).
	 * @throws InterruptedException		If the current thread has been
	 *                             		interrupted.
	 *                             		<em>This interruption is useful to stop
	 *                             		this alignment operation whenever it
	 *                             		becomes time and memory consuming.</em>
	 * @throws LineProcessorException	If there was an error while processing
	 *                               	a line.
	 *                               	<em>A such exception is generally thrown
	 *                               	by the given {@link LineProcessor}.</em>
	 * @throws IOException				If there is any error while accessing
	 *                    				the buffer file, or while processing a
	 *                    				line (if this processing implies I/O).
	 */
	public final long streamAligned(final LineProcessor lineProc, final int[] pos) throws IllegalStateException, LineProcessorException, IOException, InterruptedException{
		return streamAligned(lineProc, pos, '\0', null);
	}

	/**
	 * Give to the given {@link LineProcessor} all lines in which all columns
	 * are aligned.
	 *
	 * <p><strong>IMPORTANT:</strong>
	 * 	The array - pos - should have as many columns as the table has. Each
	 * 	column can contain either {@link LargeAsciiTable#LEFT LEFT},
	 * 	{@link LargeAsciiTable#CENTER CENTER} or
	 * 	{@link LargeAsciiTable#RIGHT RIGHT}.
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		If the array contains only ONE item, it will be used for every
	 * 		column.
	 *	</li>
	 *	<li>
	 *		If the array contains less items than columns to align, the last
	 *		item will be used for all the non-specified alignment.
	 *	</li>
	 *	<li>
	 *		If no alignment at all is specified, the
	 *		{@link LargeAsciiTable#LEFT LEFT} alignment will be used.
	 * 	</li>
	 * </ul>
	 *
	 * <p><em><strong>Note:</strong>
	 * 	This function does nothing and returns <code>-1</code> if no
	 * 	{@link LineProcessor} is provided.
	 * </em></p>
	 *
	 * @param lineProc	The action to perform on each line.
	 * @param pos		Array of flags, indicating how each column should be
	 *           		justified.
	 * @param newsep	Separator to use, replacing the original one.
	 *
	 * @return	Number of <em>successfully</em> processed lines.
	 *        	<em>This number could be compared to the value returned by
	 *        	{@link #size()} in order to know whether all lines were
	 *        	successfully aligned and processed.</em>
	 *
	 * @throws IllegalStateException	If this {@link LargeAsciiTable} is not
	 *                              	complete (i.e. if {@link #endTable()}
	 *                              	has not been called before this
	 *                              	function).
	 * @throws InterruptedException		If the current thread has been
	 *                             		interrupted.
	 *                             		<em>This interruption is useful to stop
	 *                             		this alignment operation whenever it
	 *                             		becomes time and memory consuming.</em>
	 * @throws LineProcessorException	If there was an error while processing
	 *                               	a line.
	 *                               	<em>A such exception is generally thrown
	 *                               	by the given {@link LineProcessor}.</em>
	 * @throws IOException				If there is any error while accessing
	 *                    				the buffer file, or while processing a
	 *                    				line (if this processing implies I/O).
	 */
	public final long streamAligned(final LineProcessor lineProc, final int[] pos, final char newsep) throws IllegalStateException, LineProcessorException, IOException, InterruptedException{
		return streamAligned(lineProc, pos, newsep, null);
	}

	/**
	 * Give to the given {@link LineProcessor} all lines in which all columns
	 * are aligned.
	 *
	 * <p><strong>IMPORTANT:</strong>
	 * 	The array - pos - should have as many columns as the table has. Each
	 * 	column can contain either {@link LargeAsciiTable#LEFT LEFT},
	 * 	{@link LargeAsciiTable#CENTER CENTER} or
	 * 	{@link LargeAsciiTable#RIGHT RIGHT}.
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		If the array contains only ONE item, it will be used for every
	 * 		column.
	 *	</li>
	 *	<li>
	 *		If the array contains less items than columns to align, the last
	 *		item will be used for all the non-specified alignment.
	 *	</li>
	 *	<li>
	 *		If no alignment at all is specified, the
	 *		{@link LargeAsciiTable#LEFT LEFT} alignment will be used.
	 * 	</li>
	 * </ul>
	 *
	 * <p><em><strong>Note:</strong>
	 * 	This function does nothing and returns <code>-1</code> if no
	 * 	{@link LineProcessor} is provided.
	 * </em></p>
	 *
	 * @param lineProc	The action to perform on each line.
	 * @param pos		Array of flags, indicating how each column should be
	 *           		justified.
	 * @param newsep	Separator to use, replacing the original one.
	 * @param thread	Thread to watch. If it is interrupted, this task should
	 *              	be as well.
	 *
	 * @return	Number of <em>successfully</em> processed lines.
	 *        	<em>This number could be compared to the value returned by
	 *        	{@link #size()} in order to know whether all lines were
	 *        	successfully aligned and processed.</em>
	 *
	 * @throws IllegalStateException	If this {@link LargeAsciiTable} is not
	 *                              	complete (i.e. if {@link #endTable()}
	 *                              	has not been called before this
	 *                              	function).
	 * @throws InterruptedException		If the current thread has been
	 *                             		interrupted.
	 *                             		<em>This interruption is useful to stop
	 *                             		this alignment operation whenever it
	 *                             		becomes time and memory consuming.</em>
	 * @throws LineProcessorException	If there was an error while processing
	 *                               	a line.
	 *                               	<em>A such exception is generally thrown
	 *                               	by the given {@link LineProcessor}.</em>
	 * @throws IOException				If there is any error while accessing
	 *                    				the buffer file, or while processing a
	 *                    				line (if this processing implies I/O).
	 */
	public long streamAligned(final LineProcessor lineProc, final int[] pos, char newsep, final Thread thread) throws IllegalStateException, LineProcessorException, IOException, InterruptedException{
		// Nothing to do here if nothing to do with the streamed lines:
		if (lineProc == null)
			return -1;

		/* Optimisation - if exactly the same separator, there is no need for a
		 * character replacement ; so, use the special value '\0' as markup for
		 * that: */
		if (newsep == csep)
			newsep = '\0';

		long nbTotalLines = 0;
		try(AsciiTableIterator itLines = getIterator()){
			StringBuffer buf = new StringBuffer();
			String line;
			int p0, p1, col, fldsize, colsize, n1, inserted;
			int uniqueJustif;
			long numLine = 0;

			// Set the default alignment to use if none is specified:
			if (pos == null || pos.length == 0)
				uniqueJustif = LEFT;
			// ...or if only one is specified:
			else
				uniqueJustif = (pos.length == 1) ? pos[0] : -1;

			while(itLines.hasNext()){

				/* stop everything if this thread or the given one has been
				 * interrupted: */
				if (Thread.currentThread().isInterrupted() || (thread != null && thread.isInterrupted()))
					throw new InterruptedException();

				buf.delete(0, buf.length());
				line = itLines.next();
				p0 = 0;
				col = 0;

				// Align each column of this line:
				for(col = 0; col < sizes.length; col++){
					// write the column separator between each column:
					if (col > 0)
						buf.append(sep);

					/* if last column, consider additional columns as part of
					 * the last expected column: */
					if (col == sizes.length - 1)
						p1 = line.length();
					// otherwise, search for the next column separator:
					else{
						p1 = line.indexOf(sep, p0);
						// if none found, take all remaining characters:
						if (p1 < 0)
							p1 = line.length();
					}

					// measure the actual column size:
					fldsize = p1 - p0;
					// ...nothing to append if the field is empty:
					if (fldsize < 0)
						break;

					// get the maximum length of this column:
					colsize = sizes[col];

					/* count how many spaces must be inserted to fill this
					 * space: */
					inserted = colsize - fldsize;
					if (inserted < 0)
						inserted = 0;

					// choose the most appropriate alignment:
					int justif;
					// ...in the header, all columns are centred:
					if (numLine < headerSize)
						justif = CENTER;
					/* ...in case of unique alignment rule, use it for all
					 *    columns: */
					else if (uniqueJustif >= 0)
						justif = uniqueJustif;
					/* ...if the alignment is not specified for the last columns
					 *    use the last specified one: */
					else if (col >= pos.length)
						justif = pos[pos.length - 1];
					// ...otherwise, use the specified alignment rule:
					else
						justif = pos[col];

					// finally, align and append this column value:
					switch(justif){
						case LEFT:
						default:
							buf.append(line.substring(p0, p1));
							addspaces(buf, inserted);
							break;
						case CENTER:
							n1 = (inserted) / 2;
							addspaces(buf, n1);
							buf.append(line.substring(p0, p1));
							addspaces(buf, inserted - n1);
							break;
						case RIGHT:
							addspaces(buf, inserted);
							buf.append(line.substring(p0, p1));
							break;
					}

					// update the position of the next column to append:
					p0 = p1 + 1;
				}

				// Finally process the aligned line:
				if (lineProc.process((newsep != '\0') ? buf.toString().replace(csep, newsep) : buf.toString()))
					nbTotalLines++;

				/* Increment the line number (only useful to detect whether we
				 * are in the header or in the data part): */
				numLine++;

				// If end of the header, then create the separator line:
				if (numLine > 0 && numLine == headerSize){
					// empty the buffer:
					buf.delete(0, buf.length());

					// write the separator line between the header and the data:
					for(int k = 0; k < sizes.length; k++){
						if (k > 0)
							buf.append((newsep != '\0') ? newsep : csep);
						addHsep(buf, sizes[k]);
					}

					// write the header PostFix:
					if (headerPostfix != null)
						buf.append(headerPostfix);

					// process this line:
					if (lineProc.process(buf.toString()))
						nbTotalLines++;
				}
			}
		}

		return nbTotalLines;
	}

	/**
	 * Add nb spaces to the StringBuffer.
	 *
	 * @param buf	StringBuffer to modify.
	 * @param nb	Number of spaces to add.
	 */
	protected void addspaces(final StringBuffer buf, final int nb){
		if (nb <= 0)
			return;

		while(nb > SPACES.length())
			SPACES.append(SPACES);
		buf.append(SPACES.substring(0, nb));
	}

	/**
	 * Add horizontal separator chars to the StringBuffer.
	 *
	 * @param buf	StringBuffer to modify.
	 * @param nb	Number of chars to add.
	 */
	protected void addHsep(final StringBuffer buf, final int nb){
		if (nb <= 0)
			return;

		while(nb > HSEP.length())
			HSEP.append(HSEP);
		buf.append(HSEP.substring(0, nb));
	}

	/* **********************************************************************
	 * * TABLE ITERATION                                                    *
	 * ********************************************************************** */

	/**
	 * Let iterate over the complete content of this table.
	 *
	 * <p><strong>IMPORTANT:</strong>
	 * 	This function fails if the table is not complete
	 * 	(i.e. {@link #endTable()} has not yet been called) or is closed.
	 * </p>
	 *
	 * @return	An iterator over this table.
	 *
	 * @throws IllegalStateException	If this table is not complete
	 *                              	or is closed.
	 *
	 */
	public AsciiTableIterator getIterator() throws IllegalStateException{
		if (closed)
			throw new IllegalStateException(ERROR_TABLE_CLOSED);
		else if (!complete)
			throw new IllegalStateException(ERROR_TABLE_NOT_COMPLETE);

		return (bufferFile == null) ? new MemoryAsciiTableIterator() : new FileAsciiTableIterator();
	}

	/**
	 * An auto-closeable iterator for {@link LargeAsciiTable}.
	 *
	 * <p>
	 * 	Since {@link LargeAsciiTable} may need a temporary file, an input stream
	 * 	could be used to iterate over this table. Hence the importance to close
	 * 	this iterator after use.
	 * </p>
	 *
	 * <p>
	 * 	The implementation of the interface {@link AutoCloseable} helps a lot
	 * 	with that as a such iterator can be closed automatically by a try
	 * 	statement if initialised inside it:
	 * </p>
	 * <pre>
	 *   try({@link AsciiTableIterator} lineIt = {@link LargeAsciiTable#getIterator() getIterator()}){
	 *     ...
	 *   }</pre>
	 *
	 * <p><em><strong>Note:</strong>
	 * 	No external class should have to create any instance of
	 * 	{@link AsciiTableIterator}. Instead, it is strongly recommended to
	 * 	use the function {@link LargeAsciiTable#getIterator()}.
	 * </em></p>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.3 (11/2018)
	 * @since 2.3
	 */
	public static interface AsciiTableIterator extends Iterator<String>, AutoCloseable {
		@Override
		public void close() throws IOException;
	}

	/**
	 * Iterator to use when the table stored in a {@link LargeAsciiTable}
	 * entirely fits in memory.
	 *
	 * <p><strong>WARNING!</strong>
	 * 	This class does not check whether the {@link LargeAsciiTable} uses only
	 * 	the memory to store the table. In case this condition is not validated,
	 * 	the usage of this class may have an unexpected behaviour (though it is
	 * 	highly possible that a {@link NullPointerException} will be thrown).
	 * </p>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.3 (11/2018)
	 * @since 2.3
	 */
	private class MemoryAsciiTableIterator implements AsciiTableIterator {

		/** Number of the next line to return. */
		private int numLine = 0;

		@Override
		public boolean hasNext(){
			return (numLine < nbLines);
		}

		@Override
		public String next() throws NoSuchElementException{
			if (!hasNext())
				throw new NoSuchElementException(ERROR_NO_MORE_LINE);
			return lines[numLine++];
		}

		@Override
		public void close(){
			// Nothing to close in this iterator!
		}

		@Override
		public void remove() throws UnsupportedOperationException{
			throw new UnsupportedOperationException("Impossible to modify a LargeAsciiTable!");
		}

	}

	/**
	 * Iterator to use when the table is stored in a temporary file by this
	 * {@link LargeAsciiTable}.
	 *
	 * <p><strong>WARNING!</strong>
	 * 	This class does not check whether the {@link LargeAsciiTable} uses only
	 * 	the temporary file to store the table. In case this condition is not
	 * 	validated, the usage of this class may have an unexpected behaviour
	 * 	(though it is highly possible that a {@link NullPointerException} will
	 * 	be thrown).
	 * </p>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.3 (11/2018)
	 * @since 2.3
	 */
	private class FileAsciiTableIterator implements AsciiTableIterator {

		/** Input stream to use to read the temporary buffer file.
		 * <em>NULL if no more lines are available.</em> */
		private ObjectInputStream in = null;

		/** Number of consecutive lines available in the temporary file.
		 * <em>-1 if no more lines are available.</em> */
		private int nbAvailableLines = -1;

		/** Number of lines not yet fetched from the temporary file among the
		 * last {@link #nbAvailableLines} lines.
		 * <em>-1 if no more lines are available.</em> */
		private int nbRemainingLines = -1;

		/** The next line to return.
		 * <em>NULL if no more lines are available.</em> */
		private String nextLine = null;

		/**
		 * Initialise this iterator.
		 *
		 * <p>
		 * 	It basically opens an input stream toward the temporary file and
		 * 	then fetches the first line to return.
		 * </p>
		 */
		public FileAsciiTableIterator(){
			try{
				// Initialise the input stream:
				in = new ObjectInputStream(new FileInputStream(bufferFile));

				// Fetch the first line (if any):
				fetchNextLine();

			}catch(Exception ex){
				/* If the file can not be open, no line can be returned! */
			}
		}

		/**
		 * Fetch the next line to return.
		 *
		 * <p>
		 * 	{@link #close()} is automatically called when no more lines are
		 * 	available.
		 * </p>
		 *
		 * @throws NoSuchElementException	If an error prevented to read a part
		 *                               	of the temporary file.
		 */
		private void fetchNextLine() throws NoSuchElementException{
			try{
				/* get the number of line available after this Integer in
				 * the file (i.e. the number of lines to fetch): */
				if (nbRemainingLines <= 0){
					nbAvailableLines = in.readInt();
					nbRemainingLines = nbAvailableLines;
				}
				// if there are lines, fetch them:
				if (nbAvailableLines > 0){
					nextLine = (String)in.readUnshared();
					nbRemainingLines--;
				}
				// otherwise, immediately close this iterator:
				else
					close();
			}catch(EOFException eofe){
				// If End Of File, silently close the stream:
				try{
					close();
				}catch(IOException ioe){
				}
			}catch(Exception ex){
				/* In case of any other error, throw an exception so that
				 * this iterator can be fixed in order to avoid this
				 * unexpected error. */
				throw new NoSuchElementException("Can not read/get the next line! Cause: " + ex.getMessage());
			}
		}

		@Override
		public boolean hasNext(){
			return (in != null) && (nbAvailableLines > 0);
		}

		@Override
		public String next() throws NoSuchElementException{
			if (!hasNext())
				throw new NoSuchElementException(ERROR_NO_MORE_LINE);

			// remember the line to return:
			String currentLine = nextLine;
			// fetch the next one:
			fetchNextLine();
			// finally return the current line:
			return currentLine;
		}

		@Override
		public void close() throws IOException{
			if (in != null){
				// remember that no more line is available:
				nbAvailableLines = -1;
				nbRemainingLines = -1;
				nextLine = null;
				// close the stream:
				in.close();
				in = null;
			}
		}

		@Override
		public void remove() throws UnsupportedOperationException{
			throw new UnsupportedOperationException("Impossible to modify a LargeAsciiTable!");
		}

	}
}
