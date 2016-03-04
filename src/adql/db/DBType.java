package adql.db;

/*
 * This file is part of ADQLLibrary.
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
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2014-2016 - Astronomisches Rechen Institut (ARI)
 */

/**
 * <p>
 * 	Describe a full column type as it is described in the IVOA document of TAP.
 * 	Thus, this object contains 2 attributes: <code>type</code> (or datatype) and <code>length</code> (or size).
 * </p>
 * 
 * <p>The length/size may be not defined ; in this case, its value is set to {@link #NO_LENGTH} or is negative or null.</p>
 * 
 * <p>All datatypes declared in the IVOA recommendation document of TAP are listed in an enumeration type: {@link DBDatatype}.
 * It is used to set the attribute type/datatype of this class.</p>
 *  
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (03/2016)
 * @since 1.3
 */
public class DBType {

	/**
	 * List of all datatypes declared in the IVOA recommendation of TAP (in the section UPLOAD).
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.4 (03/2016)
	 * @since 1.3
	 */
	public static enum DBDatatype{
		SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, BINARY, VARBINARY, CHAR, VARCHAR, BLOB, CLOB, TIMESTAMP, POINT, REGION,
		/** Type to use when the precise datatype is unknown. 
		 * @since 1.4 */
		UNKNOWN,
		/** <p>Type to use when the type is known as numeric but there is no precise datatype
		 * (e.g. double, float, integer, ...).</p>
		 * <p>It is particularly used when creating a {@link DefaultDBColumn} from an ADQL function
		 * or operation while listing resulting columns of a sub-query.</p>
		 * <p>This type is similar to {@link #UNKNOWN}.</p>
		 * @since 1.4 */
		UNKNOWN_NUMERIC;

		/** String to return when {@link #toString()} is called.
		 * @since 1.4*/
		private String strExp = this.name();

		@Override
		public String toString(){
			return strExp;
		}

		/**
		 * <p>This function lets define the name of the type as provided
		 * <b>ONLY FOR {@link #UNKNOWN} and {@link #UNKNOWN_NUMERIC} {@link DBDatatype}s</b>.</p>
		 * 
		 * <p><i><b>Important:</b>
		 * 	If this {@link DBDatatype} is not {@link #UNKNOWN} or {@link #UNKNOWN_NUMERIC} or
		 * 	if the given name is NULL or empty, this function has no effect.
		 * </i></p>
		 * 
		 * @param typeName	User type name.
		 * 
		 * @since 1.4
		 */
		public void setCustomType(final String typeName){
			if ((this == UNKNOWN || this == UNKNOWN_NUMERIC) && typeName != null && typeName.trim().length() > 0)
				strExp = "?" + typeName.trim() + "?";
		}
	}

	/** Special value in case no length/size is specified. */
	public static final int NO_LENGTH = -1;

	/** Datatype of a column. */
	public final DBDatatype type;

	/** The length parameter (only few datatypes need this parameter: char, varchar, binary and varbinary). */
	public final int length;

	/**
	 * Build a TAP column type by specifying a datatype.
	 * 
	 * @param datatype	Column datatype.
	 */
	public DBType(final DBDatatype datatype){
		this(datatype, NO_LENGTH);
	}

	/**
	 * Build a TAP column type by specifying a datatype and a length (needed only for datatypes like char, varchar, binary and varbinary).
	 * 
	 * @param datatype	Column datatype.
	 * @param length	Length of the column value (needed only for datatypes like char, varchar, binary and varbinary).
	 */
	public DBType(final DBDatatype datatype, final int length){
		if (datatype == null)
			throw new NullPointerException("Missing TAP column datatype !");
		this.type = datatype;
		this.length = length;
	}

	/**
	 * <p>Tells whether this type is a numeric.</p>
	 * 
	 * <p><i>Concerned types:
	 * 	{@link DBDatatype#SMALLINT SMALLINT}, {@link DBDatatype#INTEGER INTEGER}, {@link DBDatatype#BIGINT BIGINT},
	 * 	{@link DBDatatype#REAL REAL}, {@link DBDatatype#DOUBLE DOUBLE}, {@link DBDatatype#BINARY BINARY},
	 * 	{@link DBDatatype#VARBINARY VARBINARY} and {@link DBDatatype#BLOB BLOB}.
	 * </i></p>
	 * 
	 * <p><i><b>Important note</b>:
	 * 	Since {@link DBDatatype#UNKNOWN UNKNOWN} is an unresolved type, it can potentially be anything.
	 * 	But, in order to avoid incorrect operation while expecting a numeric although the type is unknown
	 * 	and is in fact not really a numeric, this function will return <code>false</code> if the type is
	 * 	{@link DBDatatype#UNKNOWN UNKNOWN} <b>BUT</b> <code>true</code> if
	 * 	{@link DBDatatype#UNKNOWN_NUMERIC UNKNOWN_NUMERIC}.
	 * </i></p>
	 * 
	 * @return	<code>true</code> if this type is a numeric, <code>false</code> otherwise.
	 */
	public boolean isNumeric(){
		switch(type){
			case SMALLINT:
			case INTEGER:
			case BIGINT:
			case REAL:
			case DOUBLE:
				/* Note: binaries are also included here because they can also be considered as Numeric,
				 * but not for JOINs. */
			case BINARY:
			case VARBINARY:
			case BLOB:
			case UNKNOWN_NUMERIC:
				return true;
			default:
				return false;
		}
	}

	/**
	 * <p>Tells whether this type is a list of bytes.</p>
	 * 
	 * <p><i>Concerned types:
	 * 	{@link DBDatatype#BINARY BINARY}, {@link DBDatatype#VARBINARY VARBINARY} and {@link DBDatatype#BLOB BLOB}.
	 * </i></p>
	 * 
	 * <p><i><b>Important note</b>:
	 * 	Since {@link DBDatatype#UNKNOWN UNKNOWN} is an unresolved type, it can potentially be anything.
	 * 	But, in order to avoid incorrect operation while expecting a binary although the type is unknown
	 * 	and is in fact not really a binary, this function will return <code>false</code> if the type is
	 * 	{@link DBDatatype#UNKNOWN UNKNOWN} or {@link DBDatatype#UNKNOWN_NUMERIC UNKNOWN_NUMERIC}.
	 * </i></p>
	 * 
	 * @return	<code>true</code> if this type is a binary, <code>false</code> otherwise.
	 */
	public boolean isBinary(){
		switch(type){
			case BINARY:
			case VARBINARY:
			case BLOB:
				return true;
			default:
				return false;
		}
	}

	/**
	 * <p>Tells whether this type is about characters.</p>
	 * 
	 * <p><i>Concerned types:
	 * 	{@link DBDatatype#CHAR CHAR}, {@link DBDatatype#VARCHAR VARCHAR}, {@link DBDatatype#CLOB CLOB}
	 * 	and {@link DBDatatype#TIMESTAMP TIMESTAMP}.
	 * </i></p>
	 * 
	 * <p><i><b>Important note</b>:
	 * 	Since {@link DBDatatype#UNKNOWN UNKNOWN} is an unresolved type, it can potentially be anything.
	 * 	But, in order to avoid incorrect operation while expecting a string although the type is unknown
	 * 	and is in fact not really a string, this function will return <code>false</code> if the type is
	 * 	{@link DBDatatype#UNKNOWN UNKNOWN} or {@link DBDatatype#UNKNOWN_NUMERIC UNKNOWN_NUMERIC}
	 * </i></p>
	 * 
	 * @return	<code>true</code> if this type is a string, <code>false</code> otherwise.
	 */
	public boolean isString(){
		switch(type){
			case CHAR:
			case VARCHAR:
			case CLOB:
			case TIMESTAMP:
				return true;
			default:
				return false;
		}
	}

	/**
	 * <p>Tells whether this type is a geometrical region.</p>
	 * 
	 * <p><i>Concerned types:
	 * 	{@link DBDatatype#POINT POINT} and {@link DBDatatype#REGION REGION}.
	 * </i></p>
	 * 
	 * <p><i><b>Important note</b>:
	 * 	Since {@link DBDatatype#UNKNOWN UNKNOWN} is an unresolved type, it can potentially be anything.
	 * 	But, in order to avoid incorrect operation while expecting a geometry although the type is unknown
	 * 	and is in fact not really a geometry, this function will return <code>false</code> if the type is
	 * 	{@link DBDatatype#UNKNOWN UNKNOWN} or {@link DBDatatype#UNKNOWN_NUMERIC UNKNOWN_NUMERIC}.
	 * </i></p>
	 * 
	 * @return	<code>true</code> if this type is a geometry, <code>false</code> otherwise.
	 */
	public boolean isGeometry(){
		return (type == DBDatatype.POINT || type == DBDatatype.REGION);
	}

	/**
	 * <p>Tell whether this type has been resolved or not.</p>
	 * 
	 * <p><i>Concerned types:
	 * 	{@link DBDatatype#UNKNOWN UNKNOWN} and {@link DBDatatype#UNKNOWN_NUMERIC UNKNOWN_NUMERIC}.
	 * </i></p>
	 * 
	 * @return	<code>true</code> if this type has NOT been resolved, <code>false</code> otherwise.
	 * 
	 * @since 1.4
	 */
	public boolean isUnknown(){
		return type == DBDatatype.UNKNOWN || type == DBDatatype.UNKNOWN_NUMERIC;
	}

	/**
	 * <p>Tell whether this {@link DBType} is compatible with the given one.</p>
	 * 
	 * <p>
	 * 	Two {@link DBType}s are said compatible if they are both binary, numeric, geometric or string.
	 * 	If one of the two types is {@link DBDatatype#UNKNOWN unknown} or {@link DBDatatype#UNKNOWN_NUMERIC unknown_numeric},
	 * 	this function will consider them as compatible and will return <code>true</code>.
	 * </p>
	 * 
	 * @param t	The type to compare to.
	 * 
	 * @return	<code>true</code> if this type is compatible with the given one, <code>false</code> otherwise.
	 */
	public boolean isCompatible(final DBType t){
		if (t == null)
			return false;
		else if (isUnknown() || t.isUnknown())
			return true;
		else if (isBinary() == t.isBinary())
			return (type == DBDatatype.BLOB && t.type == DBDatatype.BLOB) || (type != DBDatatype.BLOB && t.type != DBDatatype.BLOB);
		else if (isNumeric() == t.isNumeric())
			return true;
		else if (isGeometry() == t.isGeometry())
			return (type == t.type);
		else if (isString())
			return (type == DBDatatype.CLOB && t.type == DBDatatype.CLOB) || (type != DBDatatype.CLOB && t.type != DBDatatype.CLOB);
		else
			return (type == t.type);
	}

	@Override
	public String toString(){
		if (length > 0)
			return type + "(" + length + ")";
		else
			return type.toString();
	}

}
