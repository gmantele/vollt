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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomishes Rechen Institute (ARI)
 */

import cds.savot.writer.SavotWriter;

/**
 * <p>Describes a full VOTable type. Thus it includes the following field attributes:</p>
 * <ul>
 * 	<li><code>datatype</code>,</li>
 * 	<li><code>arraysize</code>,</li>
 * 	<li><code>xtype</code>.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2014)
 */
public final class VotType {
	/**
	 * All possible values for a VOTable datatype (i.e. boolean, short, char, ...).
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 2.0 (07/2014)
	 * @since 2.0
	 */
	public static enum VotDatatype{
		BOOLEAN("boolean"), BIT("bit"), UNSIGNED_BYTE("unsignedByte"), SHORT("short"), INT("int"), LONG("long"), CHAR("char"), UNICODE_CHAR("unicodeChar"), FLOAT("float"), DOUBLE("double"), FLOAT_COMPLEX("floatComplex"), DOUBLE_COMPLEX("doubleComplex");

		private final String strExpr;

		private VotDatatype(final String str){
			strExpr = (str == null || str.trim().length() == 0) ? name() : str;
		}

		@Override
		public String toString(){
			return strExpr;
		}
	}

	/** Special VOTable type (XType) for TAP/DB type BLOB.
	 * @since 2.0*/
	public final static String XTYPE_BLOB = "adql:BLOB";
	/** Special VOTable type (XType) for TAP/DB type CLOB.
	 * @since 2.0 */
	public final static String XTYPE_CLOB = "adql:CLOB";
	/** Special VOTable type (XType) for TAP/DB type TIMESTAMP.
	 * @since 2.0 */
	public final static String XTYPE_TIMESTAMP = "adql:TIMESTAMP";
	/** Special VOTable type (XType) for TAP/DB type POINT.
	 * @since 2.0 */
	public final static String XTYPE_POINT = "adql:POINT";
	/** Special VOTable type (XType) for TAP/DB type REGION.
	 * @since 2.0 */
	public final static String XTYPE_REGION = "adql:REGION";

	/** No array size.
	 * @since 2.0 */
	public static final int NO_SIZE = -1;

	/** VOTable datatype
	 * @since 2.0 */
	public final VotDatatype datatype;
	/** A negative or null value means "*" (that's to say: an undetermined arraysize). */
	public final int arraysize;
	/** If true, it means either "n*" (where n is the arraysize when > 0) or "*".
	 * @since 2.0*/
	public final boolean unlimitedArraysize;
	/** Special type specification (i.e. POINT, TIMESTAMP, ...). */
	public final String xtype;

	/**
	 * Build a VOTable field type.
	 * 
	 * @param datatype		A datatype. <b>Null value forbidden</b>
	 * @param arraysize		A non-null positive integer. (any value &le; 0 will be considered as an undetermined arraysize, that's to say {@link #NO_SIZE}).
	 * @param unlimitedSize	Indicate whether a * must be appended at the end of the arraysize attribute (so in these 2 cases: "n*" or "*").
	 */
	public VotType(final VotDatatype datatype, final int arraysize, final boolean unlimitedSize){
		this(datatype, arraysize, unlimitedSize, null);
	}

	/**
	 * Build a VOTable field type.
	 * 
	 * @param datatype		A datatype. <b>Null value forbidden</b>
	 * @param arraysize		A non-null positive integer. (any value &le; 0 will be considered as an undetermined arraysize, that's to say {@link #NO_SIZE}).
	 * @param unlimitedSize	Indicate whether a * must be appended at the end of the arraysize attribute (so in these 2 cases: "n*" or "*").
	 * @param xtype			A special type (ex: adql:POINT, adql:TIMESTAMP, ...). Null value allowed.
	 */
	public VotType(final VotDatatype datatype, final int arraysize, final boolean unlimitedSize, final String xtype){
		if (datatype == null)
			throw new NullPointerException("Missing VOTable datatype !");
		this.datatype = datatype;
		this.arraysize = (arraysize > 0) ? arraysize : NO_SIZE;
		this.unlimitedArraysize = unlimitedSize;
		this.xtype = xtype;
	}

	@Override
	public boolean equals(Object obj){
		if (obj == null)
			return false;
		try{
			return toString().equals(obj);
		}catch(ClassCastException cce){
			;
		}
		return false;
	}

	@Override
	public int hashCode(){
		return datatype.toString().hashCode();
	}

	@Override
	public String toString(){
		StringBuffer str = new StringBuffer("datatype=\"");
		str.append(datatype).append('"');

		if (arraysize > 0){
			str.append(" arraysize=\"").append(SavotWriter.encodeAttribute("" + arraysize));
			if (unlimitedArraysize)
				str.append("*");
			str.append('"');
		}else if (unlimitedArraysize)
			str.append(" arraysize=\"*\"");

		if (xtype != null)
			str.append(" xtype=\"").append(SavotWriter.encodeAttribute(xtype)).append('"');

		return str.toString();
	}

	/**
	 * Convert this VOTable type definition into a TAPColumn type.
	 * 
	 * @return	The corresponding {@link TAPType}.
	 */
	public TAPType toTAPType(){
		return TAPType.convertFromVotType(this);
	}

}
