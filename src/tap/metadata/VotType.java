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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
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
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 11/2011
 */
public final class VotType {
	public final String datatype;
	/** A negative or null value means "*" (that's to say: an undetermined arraysize). */
	public int    arraysize;
	public final String xtype;

	/**
	 * @param datatype		A datatype (ex: char, int, long, ...). <b>Null value forbidden</b>
	 * @param arraysize		A non-null positive integer. (any value &le; 0 will be considered as an undetermined arraysize).
	 * @param xtype			A special type (ex: adql:POINT, adql:TIMESTAMP, ...). Null value allowed.
	 */
	public VotType(final String datatype, final int arraysize, final String xtype){
		if (datatype == null)
			throw new NullPointerException("Null VOTable datatype !");
		this.datatype = datatype;
		this.arraysize = arraysize;
		this.xtype = xtype;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		try{
			VotType vot = (VotType)obj;
			if (datatype.equalsIgnoreCase(vot.datatype)){
				if (xtype == null)
					return (vot.xtype == null);
				else
					return xtype.equalsIgnoreCase(vot.xtype);
			}
		}catch(ClassCastException cce){ ; }
		return false;
	}

	@Override
	public int hashCode() {
		return datatype.toLowerCase().hashCode();
	}

	@Override
	public String toString(){
		StringBuffer str = new StringBuffer("datatype=\"");
		str.append(datatype).append('"');

		if (arraysize == TAPTypes.STAR_SIZE)
			str.append(" arraysize=\"*\"");
		else if (arraysize != TAPTypes.NO_SIZE && arraysize > 0)
			str.append(" arraysize=\"").append(SavotWriter.encodeAttribute(""+arraysize)).append('"');

		if (xtype != null)
			str.append(" xtype=\"").append(SavotWriter.encodeAttribute(xtype)).append('"');

		return str.toString();
	}

}
