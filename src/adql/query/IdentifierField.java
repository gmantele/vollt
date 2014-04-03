package adql.query;

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
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.query.from.ADQLTable;

import adql.query.operand.ADQLColumn;

/**
 * <p>
 * 	Lets getting or setting the case sensitivity of an identifier (column, table, schema, catalog or alias)
 * 	of an {@link ADQLColumn} or an {@link ADQLTable}.
 * </p>
 * <p>
 * 	The case sensitivity of an ADQL identifier is defined in a single attribute of type 'byte'. Each bit is designed to
 * 	indicate the case sensitivity of a particular identifier part (from right to left):
 * </p>
 * <ul>
 * 	<li>1st bit = column</li>
 * 	<li>2nd bit = table</li>
 * 	<li>3rd bit = schema</li>
 * 	<li>4th bit = catalog</li>
 * 	<li>5th bit = alias</li>
 * </ul>
 * 
 * <p>
 * 	Consequently to manage the case sensitivity of an identifier, you can use the following methods:
 * </p>
 * <ul>
 * 	<li>{@link #isCaseSensitive(byte)}: to know whether an identifier part is case sensitive or not in the given byte</li>
 * 	<li>{@link #setCaseSensitive(byte, boolean)}: to modify the given byte so that setting the case sensitivity of an identifier part</li>
 * </ul>
 * 
 * <p><i><u>Example:</u> In {@link ADQLColumn}, the attribute 'caseSensitivity' lets managing the case sensitivity of all parts of the column identifier.</i></p>
 * <pre>
 * public class ADQLColumn implements ADQLOperand {
 * 	...
 * 	private byte caseSensitivity = 0;
 * 	...
 * 	public final boolean isCaseSensitive(IdentifierField field){
 * 		return field.isCaseSensitive(caseSensitivity);
 * 	}
 * 
 * 	public final void setCaseSensitive(IdentifierField field, boolean sensitive){
 * 		caseSensitivity = field.setCaseSensitive(caseSensitivity, sensitive);
 * 	}
 * 	...
 * }
 * 
 * ADQLColumn column = new ADQLColumn("myCat.mySchema.myTable.colName");
 * column.setCaseSensitive(IdentifierField.TABLE, true);
 * System.out.println("Is column name case sensitive ? "+column.isCaseSensitive(IdentifierField.COLUMN));
 * </pre>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 * 
 * @see ADQLTable
 * @see ADQLColumn
 */
public enum IdentifierField {
	COLUMN(0),
	TABLE(1),
	SCHEMA(2),
	CATALOG(3),
	ALIAS(4);

	private final byte nbShift;

	private IdentifierField(int b){
		nbShift = (byte)b;
	}

	/**
	 * Tells whether this field is case sensitive in the given global case sensitivity definition.
	 * 
	 * @param caseSensitivity	Definition of the case sensitivity of a whole ADQL identifier.
	 * 
	 * @return	<i>true</i> if this field is case sensitive, <i>false</i> otherwise.
	 */
	public final boolean isCaseSensitive(final byte caseSensitivity){
		return ((caseSensitivity >> nbShift) & 0x01) == 1;
	}

	/**
	 * Sets the case sensitivity of this identifier part in the given global case sensitivity definition.
	 * 
	 * @param caseSensitivity	Definition of the case sensitivity of a whole ADQL identifier.
	 * @param sensitive			<i>true</i> for case sensitive, <i>false</i> otherwise.
	 * 
	 * @return	The modified case sensitivity definition.
	 */
	public final byte setCaseSensitive(final byte caseSensitivity, final boolean sensitive){
		byte mask = (byte)(0x01 << nbShift);
		if (sensitive)
			return (byte)(caseSensitivity | mask);
		else
			return (byte)(caseSensitivity & ~mask);
	}

	/**
	 * Tells whether all identifier parts are case sensitive in the given global case sensitivity definition.
	 * 
	 * @param caseSensitivity	Definition of the case sensitivity of a whole ADQL identifier.
	 * 
	 * @return	<i>true</i> if all identifier parts are case sensitive, <i>false</i> otherwise.
	 */
	public static final boolean isFullCaseSensitive(final byte caseSensitivity){
		return caseSensitivity == 0x1F;
	}

	/**
	 * Gets a byte in which all identifier parts are case sensitive or not.
	 * 
	 * @param sensitive	<i>true</i> to set all identifier parts case sensitive, <i>false</i> otherwise.
	 * 
	 * @return	A byte with all identifier parts case sensitive if <i>sensitive</i> is <i>true</i>, <i>false</i> otherwise.
	 */
	public static final byte getFullCaseSensitive(final boolean sensitive){
		if (sensitive)
			return 0x1F;
		else
			return 0;
	}
}
