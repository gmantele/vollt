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

/**
 * Represents an item of the ORDER BY list: that's to say a column reference plus a sorting indication (ASC, DESC).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 */
public class ADQLOrder extends ColumnReference {

	/** Gives an indication about how to order the results of a query. (<i>true</i> for DESCending, <i>false</i> for ASCending) */
	private boolean descSorting = false;


	/**
	 * Builds an order indication with the index of the selected column on which an ASCending ordering will be done.
	 * 
	 * @param colIndex							The index of a selected column (from 1).
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If the index is less or equal 0.
	 * 
	 * @see ADQLOrder#ADQLOrder(int, boolean)
	 */
	public ADQLOrder(int colIndex) throws ArrayIndexOutOfBoundsException {
		this(colIndex, false);
	}

	/**
	 * Builds an order indication with the index of the selected column on which the specified ordering will be done.
	 * 
	 * @param colIndex							The index of a selected column (from 1).
	 * @param desc								<i>true</i> means DESCending order, <i>false</i> means ASCending order.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If the index is less or equal 0.
	 */
	public ADQLOrder(int colIndex, boolean desc) throws ArrayIndexOutOfBoundsException {
		super(colIndex);
		descSorting = desc;
	}

	/**
	 * Builds an order indication with the name or the alias of the selected column on which an ASCending ordering will be done.
	 * 
	 * @param colName				The name or the alias of a selected column.
	 * 
	 * @throws NullPointerException	If the given name is <i>null</i> or is an empty string.
	 * 
	 * @see ADQLOrder#ADQLOrder(String, boolean)
	 */
	public ADQLOrder(String colName) throws NullPointerException {
		this(colName, false);
	}

	/**
	 * Builds an order indication with the name of the alias of the selected column on which the specified ordering will be done.
	 * 
	 * @param colName				The name of the alias of a selected column.
	 * @param desc					<i>true</i> means DESCending order, <i>false</i> means ASCending order.
	 * 
	 * @throws NullPointerException	If the given name is <i>null</i> or is an empty string.
	 */
	public ADQLOrder(String colName, boolean desc) throws NullPointerException {
		super(colName);
		descSorting = desc;
	}

	/**
	 * Builds an ORDER BY item by copying the given one.
	 * 
	 * @param toCopy		The ORDER BY item to copy.
	 */
	public ADQLOrder(ADQLOrder toCopy) {
		super(toCopy);
		descSorting = toCopy.descSorting;
	}

	/**
	 * Tells how the results will be sorted.
	 * 
	 * @return <i>true</i> DESCending order, <i>false</i> ASCending order.
	 */
	public boolean isDescSorting() {
		return descSorting;
	}

	/**
	 * Updates the current order indication.
	 * 
	 * @param colIndex						The index of a selected column (from 1).
	 * @param desc							<i>true</i> means DESCending order, <i>false</i> means ASCending order.
	 * 
	 * @throws IndexOutOfBoundsException	If the given index is less or equal 0.
	 */
	public void setOrder(int colIndex, boolean desc) throws ArrayIndexOutOfBoundsException {
		if (colIndex <= 0)
			throw new ArrayIndexOutOfBoundsException("Impossible to make a reference to the "+colIndex+"th column: a column index must be greater or equal 1 !");

		setColumnIndex(colIndex);
		descSorting = desc;
	}

	/**
	 * Updates the current order indication.
	 * 
	 * @param colName				The name or the alias of a selected column.
	 * @param desc					<i>true</i> means DESCending order, <i>false</i> means ASCending order.
	 * 
	 * @throws NullPointerException	If the given name is <i>null</i> or is an empty string.
	 */
	public void setOrder(String colName, boolean desc) throws NullPointerException {
		if (colName ==  null)
			throw new NullPointerException("Impossible to make a reference: the given name is null or is an empty string !");

		setColumnName(colName);
		descSorting = desc;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ADQLOrder(this);
	}

	@Override
	public String getName() {
		return super.getName()+(descSorting?" DESC":" ASC");
	}

	@Override
	public String toADQL(){
		return super.toADQL()+(descSorting?" DESC":" ASC");
	}

}
