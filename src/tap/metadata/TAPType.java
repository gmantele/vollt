package tap.metadata;

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
 * Copyright 2014 - Astronomishes Rechen Institut (ARI)
 */

/**
 * 
 * <p>
 * 	Describe a full TAP column type as it is described in the IVOA document.
 * 	Thus, this object contains 2 attributes: <code>type</code> (or datatype) and <code>length</code> (or size).
 * </p>
 * 
 * <p>The length/size may be not defined ; in this case, its value is set to {@link #NO_LENGTH} or is negative or null.</p>
 * 
 * <p>All datatypes declared in the IVOA recommendation document of TAP are listed in an enumeration type: {@link TAPDatatype}.
 * It is used to set the attribute type/datatype of this class.</p>
 *  
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 2.0 (06/2014)
 * @since 2.0
 */
public class TAPType {

	/**
	 * List of all datatypes declared in the IVOA recommendation of TAP (in the section UPLOAD).
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 2.0 (06/2014)
	 * @since 2.0
	 */
	public static enum TAPDatatype{
		SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, BINARY, VARBINARY, CHAR, VARCHAR, BLOB, CLOB, TIMESTAMP, POINT, REGION;
	}

	/** Special value in case no length/size is specified. */
	public static final int NO_LENGTH = -1;

	/** Datatype of a column. */
	public final TAPDatatype type;

	/** The length parameter (only few datatypes need this parameter: char, varchar, binary and varbinary). */
	public final int length;

	/**
	 * Build a TAP column type by specifying a datatype.
	 * 
	 * @param datatype	Column datatype.
	 */
	public TAPType(final TAPDatatype datatype){
		this(datatype, NO_LENGTH);
	}

	/**
	 * Build a TAP column type by specifying a datatype and a length (needed only for datatypes like char, varchar, binary and varbinary).
	 * 
	 * @param datatype	Column datatype.
	 * @param length	Length of the column value (needed only for datatypes like char, varchar, binary and varbinary).
	 */
	public TAPType(final TAPDatatype datatype, final int length){
		if (datatype == null)
			throw new NullPointerException("Missing TAP column datatype !");
		this.type = datatype;
		this.length = length;
	}

	/**
	 * Convert this TAP column type into a VOTable field type.
	 * 
	 * @return	The corresponding VOTable field type.
	 * 
	 * @see VotType#VotType(TAPType)
	 */
	public VotType toVotType(){
		return new VotType(this);
	}

	@Override
	public String toString(){
		if (length > 0)
			return type + "(" + length + ")";
		else
			return type.toString();
	}

}
