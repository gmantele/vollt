package tap.data;

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
import java.io.InputStream;
import java.util.NoSuchElementException;

import tap.TAPException;
import tap.metadata.TAPColumn;
import tap.metadata.TAPType;
import tap.metadata.VotType;
import tap.metadata.VotType.VotDatatype;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.OnceRowPipe;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;

/**
 * <p>{@link TableIterator} which lets iterate over a VOTable input stream using STIL.</p>
 * 
 * <p>{@link #getColType()} will return TAP type based on the type declared in the VOTable metadata part.</p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (07/2014)
 * @since 2.0
 */
public class VOTableIterator implements TableIterator {

	/** Metadata of all columns identified before the iteration. */
	private final TAPColumn[] colMeta;
	/** Number of columns to read. */
	private final int nbColumns;
	/** Sequence of rows over which we must iterate. */
	private final RowSequence rowSeq;

	/** Indicate whether the row iteration has already started. */
	private boolean iterationStarted = false;
	/** Indicate whether the last row has already been reached. */
	private boolean endReached = false;
	/** Index of the last read column (=0 just after {@link #nextRow()} and before {@link #nextCol()}, ={@link #nbColumns} after the last column has been read). */
	private int colIndex;

	/**
	 * Build a TableIterator able to read rows and columns inside the given VOTable input stream.
	 * 
	 * @param input	Input stream over a VOTable document.
	 * 
	 * @throws NullPointerException	If NULL is given in parameter.
	 * @throws DataReadException	If the given VOTable can not be parsed.
	 */
	public VOTableIterator(final InputStream input) throws DataReadException{
		// An input stream MUST BE provided:
		if (input == null)
			throw new NullPointerException("Missing VOTable document input stream over which to iterate!");

		try{

			// Set the VOTable builder/interpreter:
			TableBuilder tb = (new StarTableFactory()).getTableBuilder("votable");

			// Set the TableSink to use in order to stream the data: 
			OnceRowPipe rowPipe = new OnceRowPipe();

			// Initiate the stream process:
			tb.streamStarTable(input, rowPipe, null);

			// Start by reading just the metadata:
			StarTable table = rowPipe.waitForStarTable();

			// Convert columns' information into TAPColumn object:
			colMeta = extractColMeta(table);
			nbColumns = colMeta.length;

			// Set the sequence of rows on which this iterator will iterate:
			rowSeq = table.getRowSequence();

		}catch(TAPException te){
			throw new DataReadException("Unexpected field datatype: " + te.getMessage(), te);
		}catch(Exception ex){
			throw new DataReadException("Unable to parse/read the given VOTable input stream!", ex);
		}
	}

	/**
	 * Extract an array of {@link TAPColumn} objects. Each corresponds to one of the columns listed in the given table,
	 * and so corresponds to the metadata of a column. 
	 * 
	 * @param table		{@link StarTable} which contains only the columns' information.
	 * 
	 * @return			The corresponding list of {@link TAPColumn} objects.
	 * 
	 * @throws TAPException	If there is a problem while resolving the field datatype (for instance: unknown datatype, a multi-dimensional array is provided, a bad number format for the arraysize).
	 */
	private static final TAPColumn[] extractColMeta(final StarTable table) throws TAPException{
		// Count the number columns and initialize the array:
		TAPColumn[] columns = new TAPColumn[table.getColumnCount()];

		// Add all columns meta:
		for(int i = 0; i < columns.length; i++){
			// get the field:
			ColumnInfo colInfo = table.getColumnInfo(i);

			// get the datatype:
			String datatype = getAuxDatumValue(colInfo, "Datatype");

			// get the arraysize:
			String arraysize = ColumnInfo.formatShape(colInfo.getShape());

			// get the xtype:
			String xtype = getAuxDatumValue(colInfo, "xtype");

			// Resolve the field type:
			TAPType type = resolveVotType(datatype, arraysize, xtype).toTAPType();

			// build the TAPColumn object:
			TAPColumn col = new TAPColumn(colInfo.getName(), type, colInfo.getDescription(), colInfo.getUnitString(), colInfo.getUCD(), colInfo.getUtype());
			col.setPrincipal(false);
			col.setIndexed(false);
			col.setStd(false);

			// append it to the array:
			columns[i] = col;
		}

		return columns;
	}

	/**
	 * Extract the specified auxiliary datum value from the given {@link ColumnInfo}.
	 * 
	 * @param colInfo			{@link ColumnInfo} from which the auxiliary datum must be extracted.
	 * @param auxDatumName		The name of the datum to extract.
	 * 
	 * @return	The extracted value as String.
	 */
	private static final String getAuxDatumValue(final ColumnInfo colInfo, final String auxDatumName){
		DescribedValue value = colInfo.getAuxDatumByName(auxDatumName);
		return (value != null) ? value.getValue().toString() : null;
	}

	/**
	 * Resolve a VOTable field type by using the datatype, arraysize and xtype strings as specified in a VOTable document.
	 * 
	 * @param datatype		Attribute value of VOTable corresponding to the datatype.
	 * @param arraysize		Attribute value of VOTable corresponding to the arraysize.
	 * @param xtype			Attribute value of VOTable corresponding to the xtype.
	 * 
	 * @return	The resolved VOTable field type, or a CHAR(*) type if the specified type can not be resolved.
	 * 
	 * @throws TAPException	If a field datatype is unknown.
	 */
	private static VotType resolveVotType(final String datatype, final String arraysize, final String xtype) throws TAPException{
		// If no datatype is specified, return immediately a CHAR(*) type:
		if (datatype == null || datatype.trim().length() == 0)
			return new VotType(VotDatatype.CHAR, "*");

		// Identify the specified datatype:
		VotDatatype votdatatype;
		try{
			votdatatype = VotDatatype.valueOf(datatype.toUpperCase());
		}catch(IllegalArgumentException iae){
			throw new TAPException("unknown field datatype: \"" + datatype + "\"");
		}

		// Build the VOTable type:
		return new VotType(votdatatype, arraysize, xtype);
	}

	/**
	 * <p>Check the row iteration state. That's to say whether:</p>
	 * <ul>
	 * 	<li>the row iteration has started = the first row has been read = a first call of {@link #nextRow()} has been done</li>
	 * 	<li>AND the row iteration is not finished = the last row has been read.</li>
	 * </ul>
	 * @throws IllegalStateException
	 */
	private void checkReadState() throws IllegalStateException{
		if (!iterationStarted)
			throw new IllegalStateException("No row has yet been read!");
		else if (endReached)
			throw new IllegalStateException("End of VOTable file already reached!");
	}

	@Override
	public void close() throws DataReadException{
		try{
			rowSeq.close();
		}catch(IOException ioe){
			throw new DataReadException("Can not close the iterated VOTable!", ioe);
		}
	}

	@Override
	public TAPColumn[] getMetadata(){
		return colMeta;
	}

	@Override
	public boolean nextRow() throws DataReadException{
		try{
			// go to the next row:
			boolean rowFetched = rowSeq.next();
			endReached = !rowFetched;
			// prepare the iteration over its columns:
			colIndex = 0;
			iterationStarted = true;
			return rowFetched;
		}catch(IOException e){
			throw new DataReadException("Unable to read the next VOTable row!", e);
		}
	}

	@Override
	public boolean hasNextCol() throws IllegalStateException, DataReadException{
		// Check the read state:
		checkReadState();

		// Determine whether the last column has been reached or not:
		return (colIndex < nbColumns);
	}

	@Override
	public Object nextCol() throws NoSuchElementException, IllegalStateException, DataReadException{
		// Check the read state and ensure there is still at least one column to read:
		if (!hasNextCol())
			throw new NoSuchElementException("No more field to read!");

		// Get the column value:
		try{
			return rowSeq.getCell(colIndex++);
		}catch(IOException se){
			throw new DataReadException("Can not read the value of the " + colIndex + "-th field!", se);
		}
	}

	@Override
	public TAPType getColType() throws IllegalStateException, DataReadException{
		// Basically check the read state (for rows iteration):
		checkReadState();

		// Check deeper the read state (for columns iteration):
		if (colIndex <= 0)
			throw new IllegalStateException("No field has yet been read!");
		else if (colIndex > nbColumns)
			throw new IllegalStateException("All fields have already been read!");

		// Return the column type:
		return colMeta[colIndex - 1].getDatatype();
	}

}
