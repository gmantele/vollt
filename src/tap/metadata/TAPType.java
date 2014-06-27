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
 * Copyright 2014 - Astronomishes Rechen Institute (ARI)
 */

import tap.metadata.VotType.VotDatatype;

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
	 * @see #convertIntoVotType(TAPType)
	 */
	public VotType toVotType(){
		return convertIntoVotType(this);
	}

	@Override
	public String toString(){
		if (length > 0)
			return type + "(" + length + ")";
		else
			return type.toString();
	}

	/**
	 * Convert the given TAP column type into a VOTable field type.
	 * 
	 * @param taptype	The TAP column type to convert.
	 * 
	 * @return	The corresponding VOTable field type.
	 */
	public static VotType convertIntoVotType(final TAPType taptype){
		VotType vot = new VotType(VotDatatype.CHAR, VotType.NO_SIZE, false);

		switch(taptype.type){
			case SMALLINT:
				vot = new VotType(VotDatatype.SHORT, 1, false);
				break;

			case INTEGER:
				vot = new VotType(VotDatatype.INT, 1, false);
				break;

			case BIGINT:
				vot = new VotType(VotDatatype.LONG, 1, false);
				break;

			case REAL:
				vot = new VotType(VotDatatype.FLOAT, 1, false);
				break;

			case DOUBLE:
				vot = new VotType(VotDatatype.DOUBLE, 1, false);
				break;

			case CHAR:
				vot = new VotType(VotDatatype.CHAR, (taptype.length > 0 ? taptype.length : 1), false);
				break;

			case BINARY:
				vot = new VotType(VotDatatype.UNSIGNED_BYTE, (taptype.length > 0 ? taptype.length : VotType.NO_SIZE), false);
				break;

			case VARBINARY:
				vot = new VotType(VotDatatype.UNSIGNED_BYTE, (taptype.length > 0 ? taptype.length : VotType.NO_SIZE), (taptype.length > 0));
				break;

			case BLOB:
				vot = new VotType(VotDatatype.UNSIGNED_BYTE, VotType.NO_SIZE, true, VotType.XTYPE_BLOB);
				break;

			case CLOB:
				vot = new VotType(VotDatatype.CHAR, VotType.NO_SIZE, true, VotType.XTYPE_CLOB);
				break;

			case TIMESTAMP:
				vot = new VotType(VotDatatype.CHAR, VotType.NO_SIZE, true, VotType.XTYPE_TIMESTAMP);
				break;

			case POINT:
				vot = new VotType(VotDatatype.CHAR, VotType.NO_SIZE, true, VotType.XTYPE_POINT);
				break;

			case REGION:
				vot = new VotType(VotDatatype.CHAR, VotType.NO_SIZE, true, VotType.XTYPE_REGION);
				break;

			case VARCHAR:
			default:
				vot = new VotType(VotDatatype.CHAR, (taptype.length > 0 ? taptype.length : VotType.NO_SIZE), (taptype.length > 0), null);
				break;
		}

		return vot;
	}

	/**
	 * Convert the given VOTable field type into a TAP column type.
	 * 
	 * @param vottype	The VOTable field type to convert.
	 * 
	 * @return	The corresponding TAP column type.
	 */
	public static TAPType convertFromVotType(final VotType vottype){
		if (vottype == null)
			return new TAPType(TAPDatatype.VARCHAR);

		switch(vottype.datatype){
			case SHORT:
			case BOOLEAN:
				if ((vottype.arraysize <= 1 || vottype.arraysize == VotType.NO_SIZE) && !vottype.unlimitedArraysize)
					return new TAPType(TAPDatatype.SMALLINT);
				else
					return new TAPType(TAPDatatype.VARBINARY);

			case INT:
				if ((vottype.arraysize <= 1 || vottype.arraysize == VotType.NO_SIZE) && !vottype.unlimitedArraysize)
					return new TAPType(TAPDatatype.INTEGER);
				else
					return new TAPType(TAPDatatype.VARBINARY);

			case LONG:
				if ((vottype.arraysize <= 1 || vottype.arraysize == VotType.NO_SIZE) && !vottype.unlimitedArraysize)
					return new TAPType(TAPDatatype.BIGINT);
				else
					return new TAPType(TAPDatatype.VARBINARY);

			case FLOAT:
				if ((vottype.arraysize <= 1 || vottype.arraysize == VotType.NO_SIZE) && !vottype.unlimitedArraysize)
					return new TAPType(TAPDatatype.REAL);
				else
					return new TAPType(TAPDatatype.VARBINARY);

			case DOUBLE:
				if ((vottype.arraysize <= 1 || vottype.arraysize == VotType.NO_SIZE) && !vottype.unlimitedArraysize)
					return new TAPType(TAPDatatype.DOUBLE);
				else
					return new TAPType(TAPDatatype.VARBINARY);

			case UNSIGNED_BYTE:
				if (vottype.arraysize > 0){
					if (vottype.unlimitedArraysize)
						return new TAPType(TAPDatatype.VARBINARY, vottype.arraysize);
					else
						return new TAPType(TAPDatatype.BINARY, vottype.arraysize);
				}else
					return new TAPType(TAPDatatype.VARBINARY);

			case CHAR:
			default:
				TAPType taptype = null;
				if (vottype.xtype != null && vottype.xtype.trim().length() > 0){
					if (vottype.xtype.equalsIgnoreCase(VotType.XTYPE_BLOB))
						taptype = new TAPType(TAPDatatype.BLOB);
					else if (vottype.xtype.equalsIgnoreCase(VotType.XTYPE_CLOB))
						taptype = new TAPType(TAPDatatype.CLOB);
					else if (vottype.xtype.equalsIgnoreCase(VotType.XTYPE_TIMESTAMP))
						taptype = new TAPType(TAPDatatype.TIMESTAMP);
					else if (vottype.xtype.equalsIgnoreCase(VotType.XTYPE_POINT))
						taptype = new TAPType(TAPDatatype.POINT);
					else if (vottype.xtype.equalsIgnoreCase(VotType.XTYPE_REGION))
						taptype = new TAPType(TAPDatatype.REGION);
				}
				if (taptype == null){
					if (vottype.unlimitedArraysize)
						taptype = new TAPType(TAPDatatype.VARCHAR, (vottype.arraysize > 0) ? vottype.arraysize : NO_LENGTH);
					else{
						if (vottype.arraysize <= 0 || vottype.arraysize == VotType.NO_SIZE)
							taptype = new TAPType(TAPDatatype.VARCHAR);
						else if (vottype.arraysize == 1)
							taptype = new TAPType(TAPDatatype.CHAR, 1);
						else
							taptype = new TAPType(TAPDatatype.CHAR, vottype.arraysize);
					}
				}
				return taptype;
		}
	}
}
