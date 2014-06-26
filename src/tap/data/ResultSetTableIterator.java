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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.NoSuchElementException;

/**
 * <p>{@link TableIterator} which lets iterate over a SQL {@link ResultSet}.</p>
 * 
 * <p>{@link #getColType()} will return the type declared in the {@link ResultSetMetaData} object.</p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 2.0 (06/2014)
 * @since 2.0
 */
public class ResultSetTableIterator implements TableIterator {

	/** ResultSet/Dataset to read. */
	private final ResultSet data;

	/** Number of columns to read. */
	private final int nbColumns;
	/** Type of all columns. */
	private final String[] colTypes;

	/** Indicate whether the row iteration has already started. */
	private boolean iterationStarted = false;
	/** Indicate whether the last row has already been reached. */
	private boolean endReached = false;
	/** Index of the last read column (=0 just after {@link #nextRow()} and before {@link #nextCol()}, ={@link #nbColumns} after the last column has been read). */
	private int colIndex;

	/**
	 * Build a TableIterator able to read rows and columns of the given ResultSet.
	 * 
	 * @param dataSet	Dataset over which this iterator must iterate.
	 * 
	 * @throws NullPointerException	If NULL is given in parameter.
	 * @throws DataReadException	If the given ResultSet is closed
	 *                              or if the metadata (columns count and types) can not be fetched.
	 */
	public ResultSetTableIterator(final ResultSet dataSet) throws NullPointerException, DataReadException{
		// A dataset MUST BE provided:
		if (dataSet == null)
			throw new NullPointerException("Missing ResultSet object over which to iterate!");

		// It MUST also BE OPEN:
		try{
			if (dataSet.isClosed())
				throw new DataReadException("Closed ResultSet: impossible to iterate over it!");
		}catch(SQLException se){
			throw new DataReadException("Can not know whether the ResultSet is open!", se);
		}

		// Keep a reference to the ResultSet:
		data = dataSet;

		// Count columns and determine their type:
		try{
			// get the metadata:
			ResultSetMetaData metadata = data.getMetaData();
			// count columns:
			nbColumns = metadata.getColumnCount();
			// determine their type:
			colTypes = new String[nbColumns];
			for(int i = 1; i <= nbColumns; i++)
				colTypes[i - 1] = metadata.getColumnTypeName(i);
		}catch(SQLException se){
			throw new DataReadException("Can not get the column types of the given ResultSet!", se);
		}
	}

	@Override
	public boolean nextRow() throws DataReadException{
		try{
			// go to the next row:
			boolean rowFetched = data.next();
			endReached = !rowFetched;
			// prepare the iteration over its columns:
			colIndex = 0;
			iterationStarted = true;
			return rowFetched;
		}catch(SQLException e){
			throw new DataReadException("Unable to read a result set row!", e);
		}
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
			throw new IllegalStateException("End of ResultSet already reached!");
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
			throw new NoSuchElementException("No more column to read!");

		// Get the column value:
		try{
			return data.getObject(++colIndex);
		}catch(SQLException se){
			throw new DataReadException("Can not read the value of the " + colIndex + "-th column!", se);
		}
	}

	@Override
	public String getColType() throws IllegalStateException, DataReadException{
		// Basically check the read state (for rows iteration):
		checkReadState();

		// Check deeper the read state (for columns iteration):
		if (colIndex <= 0)
			throw new IllegalStateException("No column has yet been read!");
		else if (colIndex > nbColumns)
			throw new IllegalStateException("All columns have already been read!");

		// Return the column type:
		return colTypes[colIndex - 1];
	}

}
