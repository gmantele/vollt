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
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import tap.metadata.TAPColumn;
import tap.metadata.TAPType;
import tap.metadata.VotType;
import tap.metadata.VotType.VotDatatype;
import cds.savot.model.DataBinaryReader;
import cds.savot.model.FieldSet;
import cds.savot.model.SavotBinary;
import cds.savot.model.SavotField;
import cds.savot.model.SavotResource;
import cds.savot.model.SavotTD;
import cds.savot.model.SavotTR;
import cds.savot.model.SavotTableData;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;

/**
 * <p>{@link TableIterator} which lets iterate over a VOTable input stream using Savot ({@link SavotPullParser} more exactly).</p>
 * 
 * <p>{@link #getColType()} will return TAP type based on the type declared in the VOTable metadata part.</p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 2.0 (06/2014)
 * @since 2.0
 */
public class VOTableIterator implements TableIterator {

	/** Metadata of all columns identified before the iteration. */
	private final TAPColumn[] colMeta;
	/** Inner TableIterator. It lets iterate over a binary or a table data set in a transparent way. */
	private final TableIterator it;

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
			// Start parsing the VOTable:
			SavotPullParser parser = new SavotPullParser(input, SavotPullEngine.SEQUENTIAL, null);

			// Get the first resource:
			SavotResource resource = parser.getNextResource();
			if (resource == null)
				throw new DataReadException("Incorrect VOTable format: missing resource node!");

			// Extract the metadata about all fields:
			FieldSet fields = resource.getFieldSet(0);
			colMeta = extractColMeta(fields);

			// Build the iterator over the data:
			SavotBinary binary = resource.getData(0).getBinary();
			if (binary != null)
				it = new BinaryVOTableIterator(binary, fields, colMeta);
			else
				it = new DataVOTableIterator(resource.getData(0).getTableData(), colMeta);
		}catch(Exception ex){
			throw new DataReadException("Unable to parse/read the given VOTable input stream!", ex);
		}
	}

	/**
	 * Extract an array of {@link TAPColumn} objects. Each corresponds to one of the fields given in parameter,
	 * and so corresponds to the metadata of a column. 
	 * 
	 * @param fields	List of metadata fields provided in a VOTable.
	 * 
	 * @return			The corresponding list of {@link TAPColumn} objects.
	 */
	private static final TAPColumn[] extractColMeta(final FieldSet fields){
		// Count the number columns and initialize the array:
		TAPColumn[] columns = new TAPColumn[fields.getItemCount()];

		// Add all columns meta:
		for(int i = 0; i < fields.getItemCount(); i++){
			// get the field:
			SavotField field = (SavotField)fields.getItemAt(i);

			// Resolve the field type:
			TAPType type = resolveVotType(field.getDataType(), field.getArraySize(), field.getXtype()).toTAPType();

			// build the TAPColumn object:
			TAPColumn col = new TAPColumn(field.getName(), type, field.getDescription(), field.getUnit(), field.getUcd(), field.getUtype());
			col.setPrincipal(false);
			col.setIndexed(false);
			col.setStd(false);

			// append it to the array:
			columns[i] = col;
		}

		return columns;
	}

	/**
	 * Resolve a VOTable field type by using the datatype, arraysize and xtype strings as specified in a VOTable document.
	 * 
	 * @param datatype		Attribute value of VOTable corresponding to the datatype.
	 * @param arraysize		Attribute value of VOTable corresponding to the arraysize.
	 * @param xtype			Attribute value of VOTable corresponding to the xtype.
	 * 
	 * @return	The resolved VOTable field type, or a CHAR(*) type if the specified type can not be resolved.
	 */
	private static VotType resolveVotType(final String datatype, final String arraysize, final String xtype){
		// If no datatype is specified, return immediately a CHAR(*) type:
		if (datatype == null || datatype.trim().length() == 0)
			return new VotType(VotDatatype.CHAR, VotType.NO_SIZE, true);

		// 1. IDENTIFY THE DATATYPE:

		// Identify the specified datatype:
		VotDatatype votdatatype;
		try{
			votdatatype = VotDatatype.valueOf(datatype.toUpperCase());
		}catch(IllegalArgumentException iae){
			// if it can't be identified, return immediately a CHAR(*) type:
			return new VotType(VotDatatype.CHAR, VotType.NO_SIZE, true);
		}

		// 2. DETERMINE ITS ARRAYSIZE:

		int votarraysize = VotType.NO_SIZE;
		boolean votunlimitedSize = false;

		// If no arraysize is specified, let's set it to 1 (for an elementary value):
		if (arraysize == null || arraysize.trim().isEmpty())
			votarraysize = 1;

		// Otherwise, get it:
		else{
			String str = arraysize.trim();

			// Determine whether an "unlimited size" character is specified:
			votunlimitedSize = str.endsWith("*");

			// If one is specified, remove it from the arraysize string:
			if (votunlimitedSize)
				str = str.substring(0, str.length() - 1);

			// If a size is really specified (more characters than "*"), get the arraysize value:
			if (str.length() > 0){
				try{
					votarraysize = Integer.parseInt(str);
				}catch(NumberFormatException nfe){}
			}
		}

		// And finally build the VOTable type:
		return new VotType(votdatatype, votarraysize, votunlimitedSize, xtype);
	}

	/**
	 * <p>Check the row iteration state. That's to say whether:</p>
	 * <ul>
	 * 	<li>the row iteration has started = the first row has been read = a first call of {@link #nextRow()} has been done</li>
	 * 	<li>AND the row iteration is not finished = the last row has been read.</li>
	 * </ul>
	 * @throws IllegalStateException
	 */
	private static void checkReadState(final boolean iterationStarted, final boolean endReached) throws IllegalStateException{
		if (!iterationStarted)
			throw new IllegalStateException("No row has yet been read!");
		else if (endReached)
			throw new IllegalStateException("End of ResultSet already reached!");
	}

	@Override
	public TAPColumn[] getMetadata(){
		return colMeta;
	}

	@Override
	public boolean nextRow() throws DataReadException{
		return it.nextRow();
	}

	@Override
	public boolean hasNextCol() throws IllegalStateException, DataReadException{
		return it.hasNextCol();
	}

	@Override
	public Object nextCol() throws NoSuchElementException, IllegalStateException, DataReadException{
		return it.nextCol();
	}

	@Override
	public TAPType getColType() throws IllegalStateException, DataReadException{
		return it.getColType();
	}

	/**
	 * <p>{@link TableIterator} which lets iterate over a VOTable binary data part.</p>
	 * 
	 * <p>This {@link TableIterator} is only usable by {@link VOTableIterator}.</p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 2.0 (Jun 27, 2014)
	 * @since 2.0
	 */
	private static class BinaryVOTableIterator implements TableIterator {

		/** Binary data reader which lets read rows and columns, and thus iterate over them. */
		private final DataBinaryReader reader;
		/** Metadata of all columns identified before the iteration. <i>(In this TableIterator, they are completely provided by {@link VOTableIterator}).</i> */
		private final TAPColumn[] colMeta;

		/** The last read row. Each item is a column value. */
		private Object[] row;

		/** Indicate whether the row iteration has already started. */
		private boolean iterationStarted = false;
		/** Indicate whether the last row has already been reached. */
		private boolean endReached = false;
		/** Index of the last read column (=0 just after {@link #nextRow()} and before {@link #nextCol()}). */
		private int colIndex;

		/**
		 * Build a TableIterator on the given binary data part of a VOTable whose fields are also described in parameter.
		 * 
		 * @param binary		Binary data part of a VOTable document.
		 * @param fields		Description of all the fields that should be read.
		 * @param columnsMeta	Metadata information extracted from the VOTable metadata part.
		 * 
		 * @throws DataReadException	If there is an error while starting reading the given binary data.
		 */
		public BinaryVOTableIterator(final SavotBinary binary, final FieldSet fields, final TAPColumn[] columnsMeta) throws DataReadException{
			try{
				reader = new DataBinaryReader(binary.getStream(), fields, false);
				colMeta = columnsMeta;
			}catch(IOException ioe){
				throw new DataReadException("Can not open a stream to decode the binary VOTable data!", ioe);
			}
		}

		@Override
		public TAPColumn[] getMetadata(){
			return null;
		}

		@Override
		public boolean nextRow() throws DataReadException{
			try{
				// Go to the next row:
				boolean rowFetched = reader.next();
				// prepare the iteration over its columns:
				if (rowFetched){
					row = reader.getRow();
					colIndex = -1;
					iterationStarted = true;
				}else{
					row = null;
					colIndex = -1;
					endReached = true;
				}
				return rowFetched;
			}catch(IOException e){
				throw new DataReadException("Unable to read a VOTable row!", e);
			}
		}

		@Override
		public boolean hasNextCol() throws IllegalStateException, DataReadException{
			// Check the read state:
			checkReadState(iterationStarted, endReached);

			// Determine whether the last column has been reached or not:
			return (colIndex + 1 < row.length);
		}

		@Override
		public Object nextCol() throws NoSuchElementException, IllegalStateException, DataReadException{
			// Check the read state and ensure there is still at least one column to read:
			if (!hasNextCol())
				throw new NoSuchElementException("No more column to read!");

			// Get the column value:
			return row[++colIndex];
		}

		@Override
		public TAPType getColType() throws IllegalStateException, DataReadException{
			// Basically check the read state (for rows iteration):
			checkReadState(iterationStarted, endReached);

			// Check deeper the read state (for columns iteration):
			if (colIndex < 0)
				throw new IllegalStateException("No column has yet been read!");
			else if (colIndex >= colMeta.length)
				return null;

			// Get the column value:
			return colMeta[colIndex].getDatatype();
		}

	}

	/**
	 * <p>{@link TableIterator} which lets iterate over a VOTable table data part.</p>
	 * 
	 * <p>This {@link TableIterator} is only usable by {@link VOTableIterator}.</p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 2.0 (Jun 27, 2014)
	 * @since 2.0
	 */
	private static class DataVOTableIterator implements TableIterator {

		/** Iterator over the rows contained in the VOTable data part. */
		private final Iterator<Object> data;
		/** Metadata of all columns identified before the iteration. <i>(In this TableIterator, they are completely provided by {@link VOTableIterator}).</i> */
		private final TAPColumn[] colMeta;

		/** Iterator over the columns contained in the last read row. */
		private Iterator<Object> colsIt;

		/** Indicate whether the row iteration has already started. */
		private boolean iterationStarted = false;
		/** Indicate whether the last row has already been reached. */
		private boolean endReached = false;
		/** Index of the last read column (=0 just after {@link #nextRow()} and before {@link #nextCol()}). */
		private int colIndex;

		/**
		 * Build a TableIterator on the given table data part of a VOTable.
		 * 
		 * @param dataset		Table data part of a VOTable document.
		 * @param columnsMeta	Metadata information extracted from the VOTable metadata part.
		 */
		public DataVOTableIterator(final SavotTableData dataset, final TAPColumn[] columnsMeta){
			Collection<Object> trset = dataset.getTRs().getItems();
			if (trset == null){
				data = new NullIterator();
				colMeta = columnsMeta;
				iterationStarted = true;
				endReached = true;
			}else{
				data = trset.iterator();
				colMeta = columnsMeta;
			}
		}

		@Override
		public TAPColumn[] getMetadata(){
			return null;
		}

		@Override
		public boolean nextRow() throws DataReadException{
			if (data.hasNext()){
				// Go to the next row:
				SavotTR row = (SavotTR)data.next();

				// Prepare the iteration over its columns:
				Collection<Object> tdset = row.getTDSet().getItems();
				if (tdset == null)
					colsIt = new NullIterator();
				else
					colsIt = tdset.iterator();

				colIndex = -1;
				iterationStarted = true;

				return true;
			}else{
				// No more row to read => end of VOTable reached:
				endReached = true;
				return false;
			}
		}

		@Override
		public boolean hasNextCol() throws IllegalStateException, DataReadException{
			// Check the read state:
			checkReadState(iterationStarted, endReached);

			// Determine whether the last column has been reached or not:
			return colsIt.hasNext();
		}

		@Override
		public Object nextCol() throws NoSuchElementException, IllegalStateException, DataReadException{
			// Check the read state and ensure there is still at least one column to read:
			if (!hasNextCol())
				throw new NoSuchElementException("No more column to read!");

			// Get the column value:
			Object value = ((SavotTD)colsIt.next()).getContent();
			colIndex++;
			return value;
		}

		@Override
		public TAPType getColType() throws IllegalStateException, DataReadException{
			// Basically check the read state (for rows iteration):
			checkReadState(iterationStarted, endReached);

			// Check deeper the read state (for columns iteration):
			if (colIndex < 0)
				throw new IllegalStateException("No column has yet been read!");
			else if (colIndex >= colMeta.length)
				return null;

			// Get the column value:
			return colMeta[colIndex].getDatatype();
		}

	}

	/**
	 * Iterator over nothing.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 2.0 (06/2014)
	 * @version 2.0
	 */
	private final static class NullIterator implements Iterator<Object> {
		@Override
		public boolean hasNext(){
			return false;
		}

		@Override
		public Object next(){
			return null;
		}

		@Override
		public void remove(){}

	}

}
