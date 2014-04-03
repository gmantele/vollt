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

import adql.db.DBColumn;
import adql.query.from.ADQLTable;
import adql.query.operand.ADQLColumn;

/**
 * Represents a reference to a selected column either by an index or by a name/alias.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 * 
 * @see ADQLOrder
 */
public class ColumnReference implements ADQLObject {

	/** Position in the original ADQL query string. */
	private TextPosition position = null;

	/** Index of a selected column. */
	private int columnIndex = -1;

	/** Name or alias of a selected column. */
	private String columnName = null;

	/** The corresponding column in the "database". By default, this field is automatically filled by {@link adql.db.DBChecker}. */
	private DBColumn dbLink = null;

	/** The {@link ADQLTable} which is supposed to contain this column. By default, this field is automatically filled by {@link adql.db.DBChecker}. */
	private ADQLTable adqlTable = null;

	/** Indicates whether the column name/alias is case sensitive. */
	private boolean caseSensitive = false;


	/**
	 * Builds a column reference with an index of a selected column.
	 * 
	 * @param index								Index of a selected column (from 1).
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If the given index is less or equal 0.
	 */
	public ColumnReference(int index) throws ArrayIndexOutOfBoundsException {
		if (index <= 0)
			throw new IndexOutOfBoundsException("Impossible to make a reference to the "+index+"th column: a column index must be greater or equal 1 !");

		columnIndex = index;
		columnName = null;
	}

	/**
	 * Builds a column reference with a name/alias of a selected column.
	 * 
	 * @param colName					A column name/alias.
	 * 
	 * @throws NullPointerException 	If the given name is <i>null</i> or is an empty string.
	 */
	public ColumnReference(String colName) throws NullPointerException {
		if (!setColumnName(colName))
			throw new NullPointerException("Impossible to make a reference: the given column name is null or is an empty string !");
	}

	/**
	 * Builds a column reference by copying the given one.
	 * 
	 * @param toCopy	The column reference to copy.
	 */
	public ColumnReference(ColumnReference toCopy) {
		columnName = toCopy.columnName;
		caseSensitive = toCopy.caseSensitive;
		columnIndex = toCopy.columnIndex;
	}

	/**
	 * Gets the position in the original ADQL query string.
	 * 
	 * @return	The position of this {@link ColumnReference}.
	 */
	public final TextPosition getPosition(){
		return position;
	}

	/**
	 * Sets the position at which this {@link ColumnReference} has been found in the original ADQL query string.
	 * 
	 * @param pos	Position of this {@link ColumnReference}.
	 */
	public void setPosition(final TextPosition pos) {
		position = pos;
	}

	/**
	 * Gets the index of the referenced column.
	 * 
	 * @return The index of the referenced column or <i>-1</i> if this column reference has been made with a column name/alias.
	 */
	public final int getColumnIndex() {
		return columnIndex;
	}

	/**
	 * Sets the index of the referenced column.
	 * 
	 * @param index	The index of the referenced column (must be > 0).
	 * @return		<i>true</i> if the column referenced has been updated, <i>false</i> otherwise (if index &lt;= 0).
	 */
	public final boolean setColumnIndex(int index){
		if (index > 0){
			columnName = null;
			columnIndex = index;
			return true;
		}
		return false;
	}

	/**
	 * Tells whether the column is referenced by its index or by its name/alias.
	 * 
	 * @return	<i>true</i> if by index, <i>false</i> if by name/alias.
	 */
	public final boolean isIndex(){
		return columnName == null;
	}

	/**
	 * Gets the name/alias of the referenced column.
	 * 
	 * @return The referenced column's name/alias or <i>null</i> if this column reference has been made with a column index.
	 */
	public final String getColumnName() {
		return columnName;
	}

	/**
	 * Sets the name/alias of the referenced column.
	 * 
	 * @param name	The referenced column's name/alias (must be different from <i>null</i> and from an empty string).
	 * @return		<i>true</i> if the column reference has been updated, <i>false</i> otherwise (if name is <i>null</i> or is an empty string).
	 */
	public final boolean setColumnName(String name){
		if (name == null)
			return false;

		StringBuffer n = new StringBuffer(name);
		n.trimToSize();
		if (n.length() > 1 && n.charAt(0) == '\"' && n.charAt(name.length()-1) == '\"'){
			n.deleteCharAt(0);
			n.deleteCharAt(n.length()-1);
			n.trimToSize();
			if (n.length() > 0)
				caseSensitive = true;
		}
		if (n.length() == 0)
			return false;
		else{
			columnIndex = -1;
			columnName = n.toString();
			return true;
		}
	}

	/**
	 * Tells whether the column reference on a column name/alias is case sensitive.
	 * 
	 * @return	<i>true</i> if the column name/alias is case sensitive, <i>false</i> otherwise.
	 */
	public final boolean isCaseSensitive(){
		return caseSensitive;
	}

	/**
	 * Sets the case sensitivity on the column name/alias.
	 * 
	 * @param sensitive		<i>true</i> to make case sensitive the column name/alias, <i>false</i> otherwise.
	 */
	public final void setCaseSensitive(boolean sensitive){
		caseSensitive = sensitive;
	}

	/**
	 * Gets the corresponding {@link DBColumn}.
	 * 
	 * @return The corresponding {@link DBColumn} if {@link #getColumnName()} is a column name (not an alias), <i>null</i> otherwise.
	 */
	public final DBColumn getDBLink() {
		return dbLink;
	}

	/**
	 * <p>Sets the {@link DBColumn} corresponding to this {@link ADQLColumn}.</p>
	 * 
	 * <p>By default, this field is automatically filled by {@link adql.db.DBChecker}.</p>
	 * 
	 * @param dbLink Its corresponding {@link DBColumn} if {@link #getColumnName()} is a column name (not an alias), <i>null</i> otherwise.
	 */
	public final void setDBLink(DBColumn dbLink) {
		this.dbLink = dbLink;
	}

	/**
	 * Gets the {@link ADQLTable} from which this column is supposed to come.
	 * 
	 * @return 	Its source table if {@link #getColumnName()} is a column name (not an alias), otherwise <i>null</i>.
	 */
	public final ADQLTable getAdqlTable() {
		return adqlTable;
	}

	/**
	 * <p>Sets the {@link ADQLTable} from which this column is supposed to come.</p>
	 * 
	 * <p>By default, this field is automatically filled by {@link adql.db.DBChecker} when {@link adql.db.DBChecker#check(adql.query.ADQLQuery)} is called.</p>
	 * 
	 * @param adqlTable Its source table if {@link #getColumnName()} is a column name (not an alias), <i>null</i> otherwise.
	 */
	public final void setAdqlTable(ADQLTable adqlTable) {
		this.adqlTable = adqlTable;
	}

	public ADQLObject getCopy() throws Exception {
		return new ColumnReference(this);
	}

	public String getName() {
		return isIndex()?(columnIndex+""):columnName;
	}

	public final ADQLIterator adqlIterator(){
		return new NullADQLIterator();
	}

	public String toADQL(){
		return isIndex()?(""+columnIndex):(isCaseSensitive()?("\""+columnName+"\""):columnName);
	}

}
