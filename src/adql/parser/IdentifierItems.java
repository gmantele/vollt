package adql.parser;

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

import adql.query.IdentifierField;
import adql.query.TextPosition;

/**
 * <p><b><u>Important:</u> This class is designed to be filled ONLY by {@link ADQLParser} !</b></p>
 * 
 * <p>This class is an array of maximum 4 {@link IdentifierItem}.</p>
 * <p>
 * 	The goal is to represent complex ADQL identifiers (column, table, ...)
 * 	which may be composed of more than only one identifier.
 * </p>
 * <p>
 * 	For instance, a table can be referenced either by only its name or by the name of its schema and its name.
 * 	So, in this last case there are 2 identifiers.
 * </p>
 * <p>
 * 	It is possible to get one by one each identifier item (by using the getters),
 * 	or the concatenation of all (thanks to {@link #join(String)}).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 * 
 * see IdentifierItem
 */
public class IdentifierItems {

	/**
	 * Represent any ADQL identifier (column name, table name or table/column alias).
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 01/2012
	 */
	public static class IdentifierItem {
		public String identifier = null;
		public boolean caseSensitivity = false;
		public TextPosition position = null;

		public IdentifierItem(final Token token, final boolean caseSensitive){
			identifier = token.image;
			caseSensitivity = caseSensitive;
			position = new TextPosition(token);
		}

		@Override
		public String toString(){
			return identifier;
		}
	}


	/** All identifiers. The position of the different fields change in function of the number of elements.
	 * If count=4: [0]=catalog, [1]=schema, [2]=table, [3]=column.  */
	private IdentifierItem[] identifiers = new IdentifierItem[4];

	/** Number of identifier fields added. */
	private int count = 0;

	/** Indicates whether this {@link IdentifierItems} is supposed to represent a table name (true), or a column name (false). */
	private boolean tableIdent = false;


	/**
	 * Builds an IdentifierItems by specifying it is a table or a column identifier.
	 * 
	 * @param tableIdentifier	<i>true</i> if this IdentifierItems is a table identifier, <i>false</i> otherwise.
	 */
	public IdentifierItems(final boolean tableIdentifier){
		tableIdent = tableIdentifier;
	}

	/**
	 * <p>Apppends a simple identifier, that's to say an additional field (catalog, schema, table, column).</p>
	 * 
	 * <p><i><u>Note:</u> This function has no effect if there are already 4 identifiers.</i></p>
	 * 
	 * @param item	Additional item (may be null).
	 */
	public void append(final IdentifierItem item) {
		if (count >= 4)
			return;
		identifiers[count++] = item;
	}

	/**
	 * Gets the number of fields/identifiers stored in this {@link IdentifierItems}.
	 * 
	 * @return	The number of identifiers.
	 */
	public int size() { return count; }

	/**
	 * Gets the whole ind-th identifier/field.
	 * 
	 * @param ind	Index of the identifier/field to get.
	 * 
	 * @return The wanted identifier/field.
	 */
	public IdentifierItem get(final int ind) { return (ind < 0 || identifiers[ind] == null) ? null : identifiers[ind]; }

	/**
	 * Gets the value of the ind-th identifier/field.
	 * 
	 * @param ind	Index of the identifier/field to get.
	 * 
	 * @return	The value of the wanted identifier/field.
	 */
	public String getIdentifier(final int ind) { return (ind < 0 || identifiers[ind] == null) ? null : identifiers[ind].identifier; }
	public String getCatalog()	{ return getIdentifier(tableIdent ? (count-3) : (count-4)); }
	public String getSchema()	{ return getIdentifier(tableIdent ? (count-2) : (count-3)); }
	public String getTable()	{ return getIdentifier(tableIdent ? (count-1) : (count-2)); }
	public String getColumn()	{ return getIdentifier(tableIdent ?    -1     : (count-1)); }

	public int getBeginLine()	{ return (count == 0 || identifiers[0] == null) ? -1 : identifiers[0].position.beginLine; }
	public int getEndLine()		{ return (count == 0 || identifiers[count-1] == null) ? -1 : identifiers[count-1].position.endLine; }
	public int getBeginColumn()	{ return (count == 0 || identifiers[0] == null) ? -1 : identifiers[0].position.beginColumn; }
	public int getEndColumn()	{ return (count == 0 || identifiers[count-1] == null) ? -1 : identifiers[count-1].position.endColumn; }

	public TextPosition getPosition() { return new TextPosition(getBeginLine(), getBeginColumn(), getEndLine(), getEndColumn()); }

	public byte getCaseSensitivity(){
		byte sensitivity = IdentifierField.getFullCaseSensitive(false);
		if (count == 0)
			return sensitivity;

		int ind = count-1;
		// COLUMN:
		if (!tableIdent){
			if (identifiers[ind] != null)
				sensitivity = IdentifierField.COLUMN.setCaseSensitive(sensitivity, identifiers[ind].caseSensitivity);
			ind--;
		}
		if (ind < 0) return sensitivity;

		// TABLE:
		if (identifiers[ind] != null)
			sensitivity = IdentifierField.TABLE.setCaseSensitive(sensitivity, identifiers[ind].caseSensitivity);
		ind--;
		if (ind < 0) return sensitivity;

		// SCHEMA:
		if (identifiers[ind] != null)
			sensitivity = IdentifierField.SCHEMA.setCaseSensitive(sensitivity, identifiers[ind].caseSensitivity);
		ind--;
		if (ind < 0) return sensitivity;

		// CATALOG:
		if (identifiers[ind] != null)
			sensitivity = IdentifierField.CATALOG.setCaseSensitive(sensitivity, identifiers[ind].caseSensitivity);

		return sensitivity;
	}

	public boolean getColumnCaseSensitivity(){
		if (count == 0 || tableIdent || identifiers[count-1] == null)
			return false;
		return identifiers[count-1].caseSensitivity;
	}

	/**
	 * Joins all identifiers with the given delimiter.
	 * 
	 * @param delim	The string which must separate the identifiers (if <i>null</i>, the delimiter will be an empty string).
	 * 
	 * @return		The joint complex identifier.
	 */
	public String join(String delim){
		if (count == 0)
			return null;

		if (delim == null)
			delim = "";

		StringBuffer str = new StringBuffer();
		for(int i=0; i<count; i++){
			if (identifiers[i] != null){
				if (str.length() > 0)
					str.append(delim);
				str.append( (identifiers[i]==null) ? "" : identifiers[i].identifier );
			}
		}
		return str.toString();
	}

	@Override
	public String toString(){
		return join(".");
	}
}
