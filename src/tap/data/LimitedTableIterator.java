package tap.data;

/*
 * This file is part of TAPLibrary.
 * 
 * ADQLLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ADQLLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

import tap.ServiceConnection.LimitUnit;
import tap.metadata.TAPColumn;
import tap.metadata.TAPType;
import tap.upload.LimitedSizeInputStream;

import com.oreilly.servlet.multipart.ExceededSizeException;

/**
 * <p>Wrap a {@link TableIterator} in order to limit its reading to a fixed number of rows.</p>
 * 
 * <p>
 * 	This wrapper can be "mixed" with a {@link LimitedSizeInputStream}, by wrapping the original input stream by a {@link LimitedSizeInputStream}
 * 	and then by wrapping the {@link TableIterator} based on this wrapped input stream by {@link LimitedTableIterator}.
 * 	Thus, this wrapper will be able to detect embedded {@link ExceededSizeException} thrown by a {@link LimitedSizeInputStream} through another {@link TableIterator}.
 * 	If a such exception is detected, it will declare this wrapper as overflowed as it would be if a rows limit is reached.
 * </p>
 * 
 * <p><b>Warning:</b>
 * 	To work together with a {@link LimitedSizeInputStream}, this wrapper relies on the hypothesis that any {@link IOException} (including {@link ExceededSizeException})
 * 	will be embedded in a {@link DataReadException} as cause of this exception (using {@link DataReadException#DataReadException(Throwable)}
 * 	or {@link DataReadException#DataReadException(String, Throwable)}). If it is not the case, no overflow detection could be done and the exception will just be forwarded.
 * </p>
 * 
 * <p>
 *	If a limit - either of rows or of bytes - is reached, a flag "overflow" is set to true. This flag can be got with {@link #isOverflow()}.
 *	Thus, when a {@link DataReadException} is caught, it will be easy to detect whether the error occurred because of an overflow
 *	or of another problem. 
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (08/2014)
 * @since 2.0
 */
public class LimitedTableIterator implements TableIterator {

	/** The wrapped {@link TableIterator}. */
	private final TableIterator innerIt;

	/** Limit on the number of rows to read. <i>note: a negative value means "no limit".</i> */
	private final int maxNbRows;

	/** The number of rows already read. */
	private int countRow = 0;

	/** Indicate whether a limit (rows or bytes) has been reached or not. */
	private boolean overflow = false;

	/**
	 * Wrap the given {@link TableIterator} so that limiting the number of rows to read.
	 * 
	 * @param it		The iterator to wrap. <i>MUST NOT be NULL</i>
	 * @param maxNbRows	Maximum number of rows that can be read. There is overflow if more than this number of rows is asked. <i>A negative value means "no limit".</i>
	 */
	public LimitedTableIterator(final TableIterator it, final int nbMaxRows) throws DataReadException{
		if (it == null)
			throw new NullPointerException("Missing TableIterator to wrap!");
		innerIt = it;
		this.maxNbRows = nbMaxRows;
	}

	/**
	 * <p>Build the specified {@link TableIterator} instance and wrap it so that limiting the number of rows OR bytes to read.</p>
	 * 
	 * <p>
	 * 	If the limit is on the <b>number of bytes</b>, the given input stream will be first wrapped inside a {@link LimitedSizeInputStream}.
	 * 	Then, it will be given as only parameter of the constructor of the specified {@link TableIterator} instance.
	 * </p>
	 * 
	 * <p>If the limit is on the <b>number of rows</b>, this {@link LimitedTableIterator} will count and limit itself the number of rows.</p>
	 * 
	 * <p><i><b>IMPORTANT:</b> The specified class must:</i></p>
	 * <i><ul>
	 * 	<li>extend {@link TableIterator},</li>
	 * 	<li>be a concrete class,</li>
	 * 	<li>have at least one constructor with only one parameter of type {@link InputStream}.</li>
	 * </ul></i>
	 * 
	 * <p><i>Note:
	 * 	If the given limit type is NULL (or different from ROWS and BYTES), or the limit value is <=0, no limit will be set.
	 * 	All rows and bytes will be read until the end of input is reached.
	 * </i></p>
	 * 
	 * @param classIt	Class of the {@link TableIterator} implementation to create and whose the output must be limited.
	 * @param input		Input stream toward the table to read.
	 * @param type		Type of the limit: ROWS or BYTES. <i>MAY be NULL</i>
	 * @param limit		Limit in rows or bytes, depending of the "type" parameter. <i>MAY BE <=0</i>
	 * 
	 * @throws DataReadException	If no instance of the given class can be created,
	 *                          	or if the {@link TableIterator} instance can not be initialized,
	 *                          	or if the limit (in rows or bytes) has been reached.
	 */
	public < T extends TableIterator > LimitedTableIterator(final Class<T> classIt, final InputStream input, final LimitUnit type, final int limit) throws DataReadException{
		try{
			Constructor<T> construct = classIt.getConstructor(InputStream.class);
			if (type == LimitUnit.bytes && limit > 0){
				maxNbRows = -1;
				innerIt = construct.newInstance(new LimitedSizeInputStream(input, limit));
			}else{
				innerIt = construct.newInstance(input);
				maxNbRows = (type == null || type != LimitUnit.rows) ? -1 : limit;
			}
		}catch(InvocationTargetException ite){
			Throwable t = ite.getCause();
			if (t != null && t instanceof DataReadException){
				ExceededSizeException exceedEx = getExceededSizeException(t);
				// if an error caused by an ExceedSizeException occurs, set this iterator as overflowed and throw the exception: 
				if (exceedEx != null)
					throw new DataReadException(exceedEx.getMessage(), exceedEx);
				else
					throw (DataReadException)t;
			}else
				throw new DataReadException("Can not create a LimitedTableIterator!", ite);
		}catch(Exception ex){
			throw new DataReadException("Can not create a LimitedTableIterator!", ex);
		}
	}

	/**
	 * Get the iterator wrapped by this {@link TableIterator} instance.
	 * 
	 * @return	The wrapped iterator.
	 */
	public final TableIterator getWrappedIterator(){
		return innerIt;
	}

	/**
	 * <p>Tell whether a limit (in rows or bytes) has been reached.</p>
	 * 
	 * <p><i>Note:
	 * 	If <i>true</i> is returned (that's to say, if a limit has been reached) no more rows or column values
	 * 	can be read ; an {@link IllegalStateException} would then be thrown.
	 * </i></p>
	 * 
	 * @return	<i>true</i> if a limit has been reached, <i>false</i> otherwise.
	 */
	public final boolean isOverflow(){
		return overflow;
	}

	@Override
	public void close() throws DataReadException{
		innerIt.close();
	}

	@Override
	public TAPColumn[] getMetadata(){
		return innerIt.getMetadata();
	}

	@Override
	public boolean nextRow() throws DataReadException{
		// Test the overflow flag and proceed only if not overflowed:
		if (overflow)
			throw new DataReadException("Data read overflow: the limit has already been reached! No more data can be read.");

		// Read the next row:
		boolean nextRow;
		try{
			nextRow = innerIt.nextRow();
			countRow++;
		}catch(DataReadException ex){
			ExceededSizeException exceedEx = getExceededSizeException(ex);
			// if an error caused by an ExceedSizeException occurs, set this iterator as overflowed and throw the exception: 
			if (exceedEx != null){
				overflow = true;
				throw new DataReadException(exceedEx.getMessage());
			}else
				throw ex;
		}

		// If, counting this one, the number of rows exceeds the limit, set this iterator as overflowed and throw an exception:
		if (nextRow && maxNbRows >= 0 && countRow > maxNbRows){
			overflow = true;
			throw new DataReadException("Data read overflow: the limit of " + maxNbRows + " rows has been reached!");
		}

		// Send back the value returned by the inner iterator:
		return nextRow;
	}

	@Override
	public boolean hasNextCol() throws IllegalStateException, DataReadException{
		testOverflow();
		return innerIt.hasNextCol();
	}

	@Override
	public Object nextCol() throws NoSuchElementException, IllegalStateException, DataReadException{
		testOverflow();
		return innerIt.nextCol();
	}

	@Override
	public TAPType getColType() throws IllegalStateException, DataReadException{
		testOverflow();
		return innerIt.getColType();
	}

	/**
	 * Test the overflow flag and throw an {@link IllegalStateException} if <i>true</i>.
	 * 
	 * @throws IllegalStateException	If this iterator is overflowed (because of either a bytes limit or a rows limit).
	 */
	private void testOverflow() throws IllegalStateException{
		if (overflow)
			throw new IllegalStateException("Data read overflow: the limit has already been reached! No more data can be read.");
	}

	/**
	 * Get the first {@link ExceededSizeException} found in the given {@link Throwable} trace.
	 * 
	 * @param ex	A {@link Throwable}
	 * 
	 * @return	The first {@link ExceededSizeException} encountered, or NULL if none has been found.
	 */
	private ExceededSizeException getExceededSizeException(Throwable ex){
		if (ex == null)
			return null;
		while(!(ex instanceof ExceededSizeException) && ex.getCause() != null)
			ex = ex.getCause();
		return (ex instanceof ExceededSizeException) ? (ExceededSizeException)ex : null;
	}

}
