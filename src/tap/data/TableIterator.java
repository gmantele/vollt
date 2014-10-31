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

import java.util.NoSuchElementException;

import adql.db.DBType;
import tap.metadata.TAPColumn;

/**
 * <p>Let's iterate on each row and then on each column over a table dataset.</p>
 * 
 * <p>Initially, no rows are loaded and the "cursor" inside the dataset is set before the first row.
 * Thus, a first call to {@link #nextRow()} is required to read each of the column values of the first row.</p>
 * 
 * <p>Example of an expected usage:</p>
 * <pre>
 * 	TableIterator it = ...;
 * 	try{
 * 		while(it.nextRow()){
 * 			while(it.hasNextCol()){
 * 				Object colValue = it.nextCol();
 * 				String colType = it.getColType();
 * 				...
 * 			}
 * 		}
 * 	}catch(DataReadException dre){
 * 		...
 * 	}finally{
 * 		try{
 * 			it.close();
 * 		}catch(DataReadException dre){ ... }
 * 	}
 * </pre>
 * 
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 2.0 (08/2014)
 * @since 2.0
 */
public interface TableIterator {
	/**
	 * <p>Get all the metadata column that have been successfully extracted at the creation of this iterator.</p>
	 * 
	 * <p><b>Important:</b> This function should be callable at any moment from the creation of the iterator until the end of the table dataset has been reached.</p>
	 * 
	 * <p><i>Note: This function MAY BE NOT IMPLEMENTED or the metadata can not be fetched. In this case, NULL will be returned.</i></p>
	 * 
	 * <p><i><b>Warning:</b> If the metadata part of the original document is corrupted (i.e. false number of columns),
	 * the column type information should be fetched thanks to {@link #getColType()} while iterating over rows and columns.</i></p>
	 * 
	 * @return	An array of {@link TAPColumn} objects (each for a column of any row),
	 *          or NULL if this function is not implemented OR if it was not possible to get these metadata.
	 * 
	 * @see #getColType()
	 */
	public TAPColumn[] getMetadata();

	/**
	 * <p>Go to the next row if there is one.</p>
	 * 
	 * <p><i>Note: After a call to this function the columns must be fetched individually using {@link #nextCol()}
	 * <b>IF</b> this function returned </i>true<i>.</i></p>
	 * 
	 * @return	<i>true</i> if the next row has been successfully reached,
	 *          <i>false</i> if no more rows can be read.
	 * 
	 * @throws DataReadException	If an error occurs while reading the table dataset.
	 */
	public boolean nextRow() throws DataReadException;

	/**
	 * Tell whether another column is available.
	 * 
	 * @return	<i>true</i> if {@link #nextCol()} will return the value of the next column with no error,
	 *          <i>false</i> otherwise.
	 * 
	 * @throws IllegalStateException	If {@link #nextRow()} has not yet been called.
	 * @throws DataReadException	If an error occurs while reading the table dataset.
	 */
	public boolean hasNextCol() throws IllegalStateException, DataReadException;

	/**
	 * <p>Return the value of the next column.</p>
	 * 
	 * <p><i>Note: The column type can be fetched using {@link #getColType()} <b>after</b> a call to {@link #nextCol()}.</i></p>
	 * 
	 * @return	Get the value of the next column.
	 * 
	 * @throws NoSuchElementException	If no more column value is available.
	 * @throws IllegalStateException	If {@link #nextRow()} has not yet been called.
	 * @throws DataReadException	If an error occurs while reading the table dataset.
	 */
	public Object nextCol() throws NoSuchElementException, IllegalStateException, DataReadException;

	/**
	 * <p>Get the type of the current column value.</p>
	 * 
	 * <p><i>Note 1: "Current column value" means here "the value last returned by {@link #nextCol()}".</i></p>
	 * 
	 * <p><i>Note 2: This function MAY BE NOT IMPLEMENTED or the type information can not be fetched. If this is the case, NULL will be returned.</i></p>
	 * 
	 * <p><i><b>Warning:</b> In some cases, the metadata part of the original document does not match with the data
	 * it should have represented. In such case, the types returned here and by {@link #getMetadata()} would be different.
	 * <b>In case of such mismatch, the type returned by {@link #getColType()} should be considered as more correct/accurate.</b></i></p>
	 * 
	 * @return	Type of the current column value,
	 *          or NULL if this information is not available or if this function is not implemented. 
	 * 
	 * @throws IllegalStateException	If {@link #nextCol()} has not yet been called.
	 * @throws DataReadException		If an error occurs while reading the table dataset.
	 */
	public DBType getColType() throws IllegalStateException, DataReadException;

	/**
	 * Close the stream or input over which this class iterates.
	 * 
	 * @throws DataReadException	If any error occurs while closing it.
	 */
	public void close() throws DataReadException;

}
