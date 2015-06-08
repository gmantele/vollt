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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

/**
 * 
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
 * @version 1.3 (10/2014)
 * @since 1.3
 */
public class DBType {

	/**
	 * List of all datatypes declared in the IVOA recommendation of TAP (in the section UPLOAD).
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static enum DBDatatype{
		SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, BINARY, VARBINARY, CHAR, VARCHAR, BLOB, CLOB, TIMESTAMP, POINT, REGION;
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
				return true;
			default:
				return false;
		}
	}

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

	public boolean isGeometry(){
		return (type == DBDatatype.POINT || type == DBDatatype.REGION);
	}

	public boolean isCompatible(final DBType t){
		if (t == null)
			return false;
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
