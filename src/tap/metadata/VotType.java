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
 *                       Astronomisches Rechen Institut (ARI)
 */

import tap.TAPException;
import uk.ac.starlink.votable.VOSerializer;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;

/**
 * <p>Describes a full VOTable type. Thus it includes the following field attributes:</p>
 * <ul>
 * 	<li><code>datatype</code>,</li>
 * 	<li><code>arraysize</code>,</li>
 * 	<li><code>xtype</code>.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (02/2015)
 */
public final class VotType {
	/**
	 * All possible values for a VOTable datatype (i.e. boolean, short, char, ...).
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 2.0 (01/2015)
	 * @since 2.0
	 */
	public static enum VotDatatype{
		BOOLEAN("boolean"), BIT("bit"), UNSIGNEDBYTE("unsignedByte"), SHORT("short"), INT("int"), LONG("long"), CHAR("char"), UNICODECHAR("unicodeChar"), FLOAT("float"), DOUBLE("double"), FLOATCOMPLEX("floatComplex"), DOUBLECOMPLEX("doubleComplex");

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

	/** VOTable datatype
	 * @since 2.0 */
	public final VotDatatype datatype;
	/** Arraysize string of a VOTable field element. */
	public final String arraysize;
	/** Special type specification (i.e. POINT, TIMESTAMP, ...). */
	public final String xtype;

	/**
	 * Build a VOTable field type.
	 * 
	 * @param datatype		A datatype. <b>Null value forbidden</b>
	 * @param arraysize		VOTable arraysize string (<i>may be NULL</i>).
	 */
	public VotType(final VotDatatype datatype, final String arraysize){
		this(datatype, arraysize, null);
	}

	/**
	 * Build a VOTable field type.
	 * 
	 * @param datatype		A datatype. <b>Null value forbidden</b>
	 * @param arraysize		VOTable arraysize string (<i>may be NULL</i>).
	 * @param xtype			A special type (ex: adql:POINT, adql:TIMESTAMP, ...). (<i>may be NULL</i>).
	 */
	public VotType(final VotDatatype datatype, final String arraysize, final String xtype){
		// set the datatype:
		if (datatype == null)
			throw new NullPointerException("missing VOTable datatype !");
		else
			this.datatype = datatype;

		// set the array-size:
		if (arraysize != null && arraysize.trim().length() > 0)
			this.arraysize = arraysize.trim();
		else
			this.arraysize = null;

		// set the xtype:
		if (xtype != null && xtype.trim().length() > 0)
			this.xtype = xtype.trim();
		else
			this.xtype = null;
	}

	/**
	 * Build a {@link VotType} object by converting the given {@link DBType}.
	 * 
	 * @param tapType	{@link DBType} to convert.
	 */
	public VotType(final DBType tapType){
		switch(tapType.type){
			case SMALLINT:
				this.datatype = VotDatatype.SHORT;
				this.arraysize = "1";
				this.xtype = null;
				break;

			case INTEGER:
				this.datatype = VotDatatype.INT;
				this.arraysize = "1";
				this.xtype = null;
				break;

			case BIGINT:
				this.datatype = VotDatatype.LONG;
				this.arraysize = "1";
				this.xtype = null;
				break;

			case REAL:
				this.datatype = VotDatatype.FLOAT;
				this.arraysize = "1";
				this.xtype = null;
				break;

			case DOUBLE:
				this.datatype = VotDatatype.DOUBLE;
				this.arraysize = "1";
				this.xtype = null;
				break;

			case CHAR:
				this.datatype = VotDatatype.CHAR;
				this.arraysize = Integer.toString(tapType.length > 0 ? tapType.length : 1);
				this.xtype = null;
				break;

			case BINARY:
				this.datatype = VotDatatype.UNSIGNEDBYTE;
				this.arraysize = Integer.toString(tapType.length > 0 ? tapType.length : 1);
				this.xtype = null;
				break;

			case VARBINARY:
				/* TODO HOW TO MANAGE VALUES WHICH WHERE ORIGINALLY NUMERIC ARRAYS ?
				 * (cf the IVOA document TAP#Upload: votable numeric arrays should be converted into VARBINARY...no more array information and particularly the datatype)
				 */
				this.datatype = VotDatatype.UNSIGNEDBYTE;
				this.arraysize = (tapType.length > 0 ? tapType.length + "*" : "*");
				this.xtype = null;
				break;

			case BLOB:
				this.datatype = VotDatatype.UNSIGNEDBYTE;
				this.arraysize = "*";
				this.xtype = VotType.XTYPE_BLOB;
				break;

			case CLOB:
				this.datatype = VotDatatype.CHAR;
				this.arraysize = "*";
				this.xtype = VotType.XTYPE_CLOB;
				break;

			case TIMESTAMP:
				this.datatype = VotDatatype.CHAR;
				this.arraysize = "*";
				this.xtype = VotType.XTYPE_TIMESTAMP;
				break;

			case POINT:
				this.datatype = VotDatatype.CHAR;
				this.arraysize = "*";
				this.xtype = VotType.XTYPE_POINT;
				break;

			case REGION:
				this.datatype = VotDatatype.CHAR;
				this.arraysize = "*";
				this.xtype = VotType.XTYPE_REGION;
				break;

			case VARCHAR:
			default:
				this.datatype = VotDatatype.CHAR;
				this.arraysize = (tapType.length > 0 ? tapType.length + "*" : "*");
				this.xtype = null;
				break;
		}
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
		StringBuffer str = new StringBuffer(VOSerializer.formatAttribute("datatype", datatype.toString()));
		str.deleteCharAt(0);

		if (arraysize != null)
			str.append(VOSerializer.formatAttribute("arraysize", arraysize));

		if (xtype != null)
			str.append(VOSerializer.formatAttribute("xtype", xtype));

		return str.toString();
	}

	/**
	 * Convert this VOTable type definition into a TAPColumn type.
	 * 
	 * @return	The corresponding {@link DBType}.
	 * 
	 * @throws TAPException	If the conversion is impossible (particularly if the array-size refers to a multi-dimensional array ; only 1D arrays are allowed). 
	 */
	public DBType toTAPType() throws TAPException{

		/* Stop immediately if the arraysize refers to a multi-dimensional array:
		 * (Note: 'x' is the dimension separator of the VOTable attribute 'arraysize') */
		if (arraysize != null && arraysize.indexOf('x') >= 0)
			throw new TAPException("failed conversion of a VOTable datatype: multi-dimensional arrays (" + datatype + "[" + arraysize + "]) are not allowed!");

		// Convert the VOTable datatype into TAP datatype:
		switch(datatype){
		/* NUMERIC TYPES */
			case SHORT:
			case BOOLEAN:
				return convertNumericType(DBDatatype.SMALLINT);

			case INT:
				return convertNumericType(DBDatatype.INTEGER);

			case LONG:
				return convertNumericType(DBDatatype.BIGINT);

			case FLOAT:
				return convertNumericType(DBDatatype.REAL);

			case DOUBLE:
				return convertNumericType(DBDatatype.DOUBLE);

				/* BINARY TYPES */
			case UNSIGNEDBYTE:
				// BLOB exception:
				if (xtype != null && xtype.equalsIgnoreCase(XTYPE_BLOB))
					return new DBType(DBDatatype.BLOB);

				// Or else, just (var)binary:
				else
					return convertVariableLengthType(DBDatatype.VARBINARY, DBDatatype.BINARY);

				/* CHARACTER TYPES */
			case CHAR:
			default:
				/* Special type cases: */
				if (xtype != null){
					if (xtype.equalsIgnoreCase(VotType.XTYPE_CLOB))
						return new DBType(DBDatatype.CLOB);
					else if (xtype.equalsIgnoreCase(VotType.XTYPE_TIMESTAMP))
						return new DBType(DBDatatype.TIMESTAMP);
					else if (xtype.equalsIgnoreCase(VotType.XTYPE_POINT))
						return new DBType(DBDatatype.POINT);
					else if (xtype.equalsIgnoreCase(VotType.XTYPE_REGION))
						return new DBType(DBDatatype.REGION);
				}

				// Or if not known or missing, just a (var)char:
				return convertVariableLengthType(DBDatatype.VARCHAR, DBDatatype.CHAR);
		}
	}

	/**
	 * <p>Convert this numeric {@link VotType} object into a corresponding {@link DBType} whose the datatype is provided in parameter.</p>
	 * 
	 * <p>
	 * 	Thus, just the arraysize must be managed here. If there is no arraysize or if equals to '1', the given datatype will be used.
	 * 	Otherwise, it is ignored and a {@link DBType} with VARBINARY is returned.
	 * </p>
	 * 
	 * @param tapDatatype	TAP datatype corresponding to this {@link VotType} (only when arraysize != '*' and 'n').
	 * 
	 * @return	The corresponding {@link DBType}.
	 */
	protected DBType convertNumericType(final DBDatatype tapDatatype){
		// If no arraysize:
		if (arraysize == null || arraysize.equals("1"))
			return new DBType(tapDatatype);

		// If only one dimension:
		else
			return new DBType(DBDatatype.VARBINARY);

		/* Note: The test of multi-dimensional array should have been already done at the beginning of #toTAPType(). */
	}

	/**
	 * <p>
	 * 	Convert this variable length {@link VotType} (unsignedByte and char) object into a corresponding {@link DBType}
	 * 	whose the variable length and fixed length versions are given in parameters.
	 * </p>
	 * 
	 * <p>Thus, just the arraysize must be managed here. The following cases are taken into account:</p>
	 * <ul>
	 * 	<li><i>No arraysize or '*'</i>: variable length type (i.e. VARCHAR, VARBINARY),</li>
	 * 	<li><i>'n*'</i>: variable length type with the maximal length (i.e. VARCHAR(n), VARBINARY(n)),</li>
	 * 	<li><i>'n'</i>: fixed length type with the exact length (i.e. CHAR(n), BINARY(n)).</li>
	 * </ul>
	 * 
	 * @param varType		Variable length type (i.e. VARCHAR, VARBINARY).
	 * @param fixedType		Fixed length type (i.e. CHAR, BINARY).
	 * 
	 * @return	The corresponding {@link DBType}.
	 * 
	 * @throws TAPException	If the arraysize is not valid (that's to say, different from the following syntaxes: NULL, '*', 'n' or 'n*' (where n is a positive and not-null integer)).
	 */
	protected DBType convertVariableLengthType(final DBDatatype varType, final DBDatatype fixedType) throws TAPException{
		try{
			// no arraysize or '*' => VARCHAR or VARBINARY
			if (arraysize == null || arraysize.equals("*"))
				return new DBType(varType);

			// 'n*' => VARCHAR(n) or VARBINARY(n)
			else if (arraysize.charAt(arraysize.length() - 1) == '*')
				return new DBType(varType, Integer.parseInt(arraysize.substring(0, arraysize.length() - 1)));

			// 'n' => CHAR(n) or BINARY(n)
			else
				return new DBType(fixedType, Integer.parseInt(arraysize));

		}catch(NumberFormatException nfe){
			throw new TAPException("failed conversion of a VOTable datatype: non-numeric arraysize (" + arraysize + ")!");
		}
	}

}
