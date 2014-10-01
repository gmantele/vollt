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
import java.sql.Timestamp;
import java.util.NoSuchElementException;

import tap.metadata.TAPColumn;
import tap.metadata.TAPType;
import tap.metadata.TAPType.TAPDatatype;
import uws.ISO8601Format;
import adql.db.DBColumn;

/**
 * <p>{@link TableIterator} which lets iterate over a SQL {@link ResultSet}.</p>
 * 
 * <p><i>Note:
 * 	{@link #getColType()} will return a TAP type based on the one declared in the {@link ResultSetMetaData} object.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (08/2014)
 * @since 2.0
 */
public class ResultSetTableIterator implements TableIterator {

	/** ResultSet/Dataset to read. */
	private final ResultSet data;

	/** Number of columns to read. */
	private final int nbColumns;
	/** Metadata of all columns identified before the iteration. */
	private final TAPColumn[] colMeta;

	/** Indicate whether the row iteration has already started. */
	private boolean iterationStarted = false;
	/** Indicate whether the last row has already been reached. */
	private boolean endReached = false;
	/** Index of the last read column (=0 just after {@link #nextRow()} and before {@link #nextCol()}, ={@link #nbColumns} after the last column has been read). */
	private int colIndex;

	/**
	 * <p>Build a TableIterator able to read rows and columns of the given ResultSet.</p>
	 * 
	 * <p>
	 * 	In order to provide the metadata through {@link #getMetadata()}, this constructor is trying to guess the datatype
	 * 	from the DBMS column datatype (using {@link #convertType(String, String)}).
	 * </p>
	 * 
	 * <h3>Type guessing</h3>
	 * 
	 * <p>
	 * 	In order to guess a TAP type from a DBMS type, this constructor will call {@link #convertType(String, String)}
	 * 	which deals with all standard datatypes known in Postgres, SQLite, MySQL, Oracle and JavaDB/Derby.
	 * </p>
	 * 
	 * <p><i><b>Important</b>:
	 * 	To guess the TAP type from a DBMS type, {@link #convertType(String, String)} may not need to know the DBMS,
	 * 	except for SQLite. Indeed, SQLite has so many datatype restrictions that it is absolutely needed to know
	 * 	it is the DBMS from which the ResultSet is coming. Without this information, type guessing will be unpredictable!
	 * 
	 * 	<b>So, if your ResultSet is coming from a SQLite connection, you SHOULD really use one of the 2 other constructors</b>
	 * 	and provide "sqlite" as value for the second parameter. 
	 * </i></p>
	 * 
	 * @param dataSet		Dataset over which this iterator must iterate.
	 * 
	 * @throws NullPointerException	If NULL is given in parameter.
	 * @throws DataReadException	If the given ResultSet is closed or if the metadata (columns count and types) can not be fetched.
	 * 
	 * @see #convertType(String, String)
	 * @see ResultSetTableIterator#ResultSetTableIterator(ResultSet, String, DBColumn[])
	 */
	public ResultSetTableIterator(final ResultSet dataSet) throws NullPointerException, DataReadException{
		this(dataSet, null, null);
	}

	/**
	 * <p>Build a TableIterator able to read rows and columns of the given ResultSet.</p>
	 * 
	 * <p>
	 * 	In order to provide the metadata through {@link #getMetadata()}, this constructor is trying to guess the datatype
	 * 	from the DBMS column datatype (using {@link #convertType(String, String)}).
	 * </p>
	 * 
	 * <h3>Type guessing</h3>
	 * 
	 * <p>
	 * 	In order to guess a TAP type from a DBMS type, this constructor will call {@link #convertType(String, String)}
	 * 	which deals with all standard datatypes known in Postgres, SQLite, MySQL, Oracle and JavaDB/Derby.
	 * </p>
	 * 
	 * <p><i><b>Important</b>:
	 * 	The second parameter of this constructor is given as second parameter of {@link #convertType(String, String)}.
	 * 	<b>This parameter is really used ONLY when the DBMS is SQLite ("sqlite").</b> Indeed, SQLite has so many datatype
	 * 	restrictions that it is absolutely needed to know it is the DBMS from which the ResultSet is coming. Without this
	 * 	information, type guessing will be unpredictable! 
	 * </i></p>
	 * 
	 * @param dataSet		Dataset over which this iterator must iterate.
	 * @param dbms			Lower-case string which indicates from which DBMS the given ResultSet is coming. <i>note: MAY be NULL.</i>
	 * 
	 * @throws NullPointerException	If NULL is given in parameter.
	 * @throws DataReadException	If the given ResultSet is closed or if the metadata (columns count and types) can not be fetched.
	 * 
	 * @see #convertType(String, String)
	 * @see ResultSetTableIterator#ResultSetTableIterator(ResultSet, String)
	 */
	public ResultSetTableIterator(final ResultSet dataSet, final String dbms) throws NullPointerException, DataReadException{
		this(dataSet, dbms, null);
	}

	/**
	 * <p>Build a TableIterator able to read rows and columns of the given ResultSet.</p>
	 * 
	 * <p>
	 * 	In order to provide the metadata through {@link #getMetadata()}, this constructor is reading first the given metadata (if any),
	 * 	and then, try to guess the datatype from the DBMS column datatype (using {@link #convertType(String, String)}).
	 * </p>
	 * 
	 * <h3>Provided metadata</h3>
	 * 
	 * <p>The third parameter of this constructor aims to provide the metadata expected for each column of the ResultSet.</p>
	 * 
	 * <p>
	 * 	For that, it is expected that all these metadata are {@link TAPColumn} objects. Indeed, simple {@link DBColumn}
	 * 	instances do not have the type information. If just {@link DBColumn}s are provided, the ADQL name it provides will be kept
	 * 	but the type will be guessed from the type provide by the ResultSetMetadata.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	If this parameter is incomplete (array length less than the column count returned by the ResultSet or some array items are NULL),
	 * 	column metadata will be associated in the same order as the ResultSet columns. Missing metadata will be built from the
	 * 	{@link ResultSetMetaData} and so the types will be guessed.
	 * </i></p>
	 * 
	 * <h3>Type guessing</h3>
	 * 
	 * <p>
	 * 	In order to guess a TAP type from a DBMS type, this constructor will call {@link #convertType(String, String)}
	 * 	which deals with all standard datatypes known in Postgres, SQLite, MySQL, Oracle and JavaDB/Derby.
	 * </p>
	 * 
	 * <p><i><b>Important</b>:
	 * 	The second parameter of this constructor is given as second parameter of {@link #convertType(String, String)}.
	 * 	<b>This parameter is really used ONLY when the DBMS is SQLite ("sqlite").</b> Indeed, SQLite has so many datatype
	 * 	restrictions that it is absolutely needed to know it is the DBMS from which the ResultSet is coming. Without this
	 * 	information, type guessing will be unpredictable! 
	 * </i></p>
	 * 
	 * @param dataSet		Dataset over which this iterator must iterate.
	 * @param dbms			Lower-case string which indicates from which DBMS the given ResultSet is coming. <i>note: MAY be NULL.</i>
	 * @param resultMeta	List of expected columns. <i>note: these metadata are expected to be really {@link TAPColumn} objects ; MAY be NULL.</i>
	 * 
	 * @throws NullPointerException	If NULL is given in parameter.
	 * @throws DataReadException	If the metadata (columns count and types) can not be fetched.
	 * 
	 * @see #convertType(String, String)
	 */
	public ResultSetTableIterator(final ResultSet dataSet, final String dbms, final DBColumn[] resultMeta) throws NullPointerException, DataReadException{
		// A dataset MUST BE provided:
		if (dataSet == null)
			throw new NullPointerException("Missing ResultSet object over which to iterate!");

		// Keep a reference to the ResultSet:
		data = dataSet;

		// Count columns and determine their type:
		try{
			// get the metadata:
			ResultSetMetaData metadata = data.getMetaData();
			// count columns:
			nbColumns = metadata.getColumnCount();
			// determine their type:
			colMeta = new TAPColumn[nbColumns];
			for(int i = 1; i <= nbColumns; i++){
				if (resultMeta != null && (i - 1) < resultMeta.length && resultMeta[i - 1] != null){
					try{
						colMeta[i - 1] = (TAPColumn)resultMeta[i - 1];
					}catch(ClassCastException cce){
						TAPType datatype = convertType(metadata.getColumnTypeName(i), dbms);
						colMeta[i - 1] = new TAPColumn(resultMeta[i - 1].getADQLName(), datatype);
					}
				}else{
					TAPType datatype = convertType(metadata.getColumnTypeName(i), dbms);
					colMeta[i - 1] = new TAPColumn(metadata.getColumnLabel(i), datatype);
				}
			}
		}catch(SQLException se){
			throw new DataReadException("Can not get the column types of the given ResultSet!", se);
		}
	}

	@Override
	public void close() throws DataReadException{
		try{
			data.close();
		}catch(SQLException se){
			throw new DataReadException("Can not close the iterated ResultSet!", se);
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
	 * 
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
			Object o = data.getObject(++colIndex);
			// if the column value is a Timestamp object, format it in ISO8601:
			if (o != null && o instanceof Timestamp)
				o = ISO8601Format.format(((Timestamp)o).getTime());
			return o;
		}catch(SQLException se){
			throw new DataReadException("Can not read the value of the " + colIndex + "-th column!", se);
		}
	}

	@Override
	public TAPType getColType() throws IllegalStateException, DataReadException{
		// Basically check the read state (for rows iteration):
		checkReadState();

		// Check deeper the read state (for columns iteration):
		if (colIndex <= 0)
			throw new IllegalStateException("No column has yet been read!");
		else if (colIndex > nbColumns)
			throw new IllegalStateException("All columns have already been read!");

		// Return the column type:
		return colMeta[colIndex - 1].getDatatype();
	}

	/**
	 * <p>Convert the given DBMS type into the better matching {@link TAPType} instance.
	 * This function is used to guess the TAP type of a column when it is not provided in the constructor.
	 * It aims not to be exhaustive, but just to provide a type when the given TAP metadata are incomplete.</p>
	 * 
	 * <p><i>Note:
	 * 	Any unknown DBMS datatype will be considered and translated as a VARCHAR.
	 * 	The same type will be returned if the given parameter is an empty string or NULL.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This type conversion function has been designed to work with all standard datatypes of the following DBMS:
	 * 	PostgreSQL, SQLite, MySQL, Oracle and JavaDB/Derby.
	 * </i></p>
	 * 
	 * <p><i><b>Important</b>:
	 * 	<b>The second parameter is REALLY NEEDED when the DBMS is SQLite ("sqlite")!</b>
	 * 	Indeed, SQLite has a so restrictive list of datatypes that this function can reliably convert its types
	 * 	only if it knows the DBMS is SQLite. Otherwise, the conversion result would be unpredictable.
	 * 	</i>In this default implementation of this function, all other DBMS values are ignored.<i>
	 * </i></p>
	 * 
	 * <p><b>Warning</b>:
	 * 	This function is not translating the geometrical datatypes. If a such datatype is encountered,
	 * 	it will considered as unknown and so, a VARCHAR TAP type will be returned.
	 * </p>
	 * 
	 * @param dbmsType	DBMS column datatype name.
	 * @param dbms		Lower-case string which indicates which DBMS the ResultSet is coming from. <i>note: MAY be NULL.</i>
	 * 
	 * @return	The best suited {@link TAPType} object.
	 */
	protected TAPType convertType(String dbmsType, final String dbms){
		// If no type is provided return VARCHAR:
		if (dbmsType == null || dbmsType.trim().length() == 0)
			return new TAPType(TAPDatatype.VARCHAR, TAPType.NO_LENGTH);

		// Extract the type prefix and lower-case it:
		dbmsType = dbmsType.toLowerCase();
		int paramIndex = dbmsType.indexOf('(');
		String dbmsTypePrefix = (paramIndex <= 0) ? dbmsType : dbmsType.substring(0, paramIndex);
		int firstParam = getLengthParam(dbmsTypePrefix, paramIndex);

		// CASE: SQLITE
		if (dbms != null && dbms.equals("sqlite")){
			// INTEGER -> SMALLINT, INTEGER, BIGINT
			if (dbmsTypePrefix.equals("integer"))
				return new TAPType(TAPDatatype.BIGINT);
			// REAL -> REAL, DOUBLE
			else if (dbmsTypePrefix.equals("real"))
				return new TAPType(TAPDatatype.DOUBLE);
			// TEXT -> CHAR, VARCHAR, CLOB, TIMESTAMP
			else if (dbmsTypePrefix.equals("text"))
				return new TAPType(TAPDatatype.VARCHAR);
			// BLOB -> BINARY, VARBINARY, BLOB
			else if (dbmsTypePrefix.equals("blob"))
				return new TAPType(TAPDatatype.BLOB);
			// Default:
			else
				return new TAPType(TAPDatatype.VARCHAR, TAPType.NO_LENGTH);
		}
		// CASE: OTHER DBMS
		else{
			// SMALLINT
			if (dbmsTypePrefix.equals("smallint") || dbmsTypePrefix.equals("int2"))
				return new TAPType(TAPDatatype.SMALLINT);
			// INTEGER
			else if (dbmsTypePrefix.equals("integer") || dbmsTypePrefix.equals("int") || dbmsTypePrefix.equals("int4"))
				return new TAPType(TAPDatatype.INTEGER);
			// BIGINT
			else if (dbmsTypePrefix.equals("bigint") || dbmsTypePrefix.equals("int8") || dbmsTypePrefix.equals("int4") || dbmsTypePrefix.equals("number"))
				return new TAPType(TAPDatatype.BIGINT);
			// REAL
			else if (dbmsTypePrefix.equals("float4") || (dbmsTypePrefix.equals("float") && firstParam <= 63))
				return new TAPType(TAPDatatype.REAL);
			// DOUBLE
			else if (dbmsTypePrefix.equals("double") || dbmsTypePrefix.equals("double precision") || dbmsTypePrefix.equals("float8") || (dbmsTypePrefix.equals("float") && firstParam > 63))
				return new TAPType(TAPDatatype.DOUBLE);
			// BINARY
			else if (dbmsTypePrefix.equals("binary") || dbmsTypePrefix.equals("raw") || ((dbmsTypePrefix.equals("char") || dbmsTypePrefix.equals("character")) && dbmsType.endsWith(" for bit data")))
				return new TAPType(TAPDatatype.BINARY, firstParam);
			// VARBINARY
			else if (dbmsTypePrefix.equals("varbinary") || dbmsTypePrefix.equals("long raw") || ((dbmsTypePrefix.equals("varchar") || dbmsTypePrefix.equals("character varying")) && dbmsType.endsWith(" for bit data")))
				return new TAPType(TAPDatatype.VARBINARY, firstParam);
			// CHAR
			else if (dbmsTypePrefix.equals("char") || dbmsTypePrefix.equals("character"))
				return new TAPType(TAPDatatype.CHAR, firstParam);
			// VARCHAR
			else if (dbmsTypePrefix.equals("varchar") || dbmsTypePrefix.equals("varchar2") || dbmsTypePrefix.equals("character varying"))
				return new TAPType(TAPDatatype.VARBINARY, firstParam);
			// BLOB
			else if (dbmsTypePrefix.equals("bytea") || dbmsTypePrefix.equals("blob") || dbmsTypePrefix.equals("binary large object"))
				return new TAPType(TAPDatatype.BLOB);
			// CLOB
			else if (dbmsTypePrefix.equals("text") || dbmsTypePrefix.equals("clob") || dbmsTypePrefix.equals("character large object"))
				return new TAPType(TAPDatatype.CLOB);
			// TIMESTAMP
			else if (dbmsTypePrefix.equals("timestamp"))
				return new TAPType(TAPDatatype.TIMESTAMP);
			// Default:
			else
				return new TAPType(TAPDatatype.VARCHAR, TAPType.NO_LENGTH);
		}
	}

	/**
	 * <p>Extract the 'length' parameter of a DBMS type string.</p>
	 * 
	 * <p>
	 * 	If the given type string does not contain any parameter
	 * 	OR if the first parameter can not be casted into an integer,
	 * 	{@link TAPType#NO_LENGTH} will be returned.
	 * </p>
	 * 
	 * @param dbmsType		DBMS type string (containing the datatype and the 'length' parameter).
	 * @param paramIndex	Index of the open bracket.
	 * 
	 * @return	The 'length' parameter value if found, {@link TAPType#NO_LENGTH} otherwise.
	 */
	protected final int getLengthParam(final String dbmsType, final int paramIndex){
		// If no parameter has been previously detected, no length parameter:
		if (paramIndex <= 0)
			return TAPType.NO_LENGTH;

		// If there is one and that at least ONE parameter is provided....
		else{
			int lengthParam = TAPType.NO_LENGTH;
			String paramsStr = dbmsType.substring(paramIndex + 1);

			// ...extract the 'length' parameter:
			/* note: we suppose here that no other parameter is possible ;
			 *       but if there are, they are ignored and we try to consider the first parameter
			 *       as the length */
			int paramEndIndex = paramsStr.indexOf(',');
			if (paramEndIndex <= 0)
				paramEndIndex = paramsStr.indexOf(')');

			// ...cast it into an integer:
			try{
				lengthParam = Integer.parseInt(paramsStr.substring(0, paramEndIndex));
			}catch(Exception ex){}

			// ...and finally return it:
			return lengthParam;
		}
	}

}
