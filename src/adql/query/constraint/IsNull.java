package adql.query.constraint;

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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institute (ARI)
 */

import java.util.NoSuchElementException;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.TextPosition;
import adql.query.operand.ADQLColumn;

/**
 * Represents a comparison between a column to the NULL value.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public class IsNull implements ADQLConstraint {

	/** The column whose the value must be compared to <i>null</i>. */
	private ADQLColumn column;

	/** Indicates whether the predicate IS NOT NULL must be used rather than IS NULL. */
	private boolean isNotNull = false;

	/** Position of this {@link IsNull} in the given ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Builds a comparison between the given column and NULL.
	 * 
	 * @param column				The column whose the value must be compared to NULL.
	 * @throws NullPointerException	If the given column is <i>null</i>.
	 */
	public IsNull(ADQLColumn column) throws NullPointerException{
		this(column, false);
	}

	/**
	 * Builds a comparison between the column and NULL.
	 * 
	 * @param column				The column whose the value must be compared to NULL.
	 * @param isNot					<i>true</i> means IS NOT NULL, <i>false</i> means IS NULL.
	 * @throws NullPointerException	If the given column is <i>null</i>.
	 */
	public IsNull(ADQLColumn column, boolean isNot) throws NullPointerException{
		setColumn(column);
		isNotNull = isNot;
	}

	/**
	 * Builds a IsNull constraint by copying the given one.
	 * 
	 * @param toCopy		The IsNull to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public IsNull(IsNull toCopy) throws Exception{
		column = (ADQLColumn)toCopy.column.getCopy();
		isNotNull = toCopy.isNotNull;
		position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
	}

	/**
	 * Gets the column whose the value is compared to <i>NULL</i>.
	 * 
	 * @return	The column compared to <i>NULL</i>.
	 */
	public final ADQLColumn getColumn(){
		return column;
	}

	/**
	 * Lets changing the column whose the value must be compared to <i>NULL</i>.
	 * 
	 * @param column					The new column to compare to <i>NULL</i>.
	 * @throws NullPointerException		If the given column is <i>null</i>.
	 */
	public final void setColumn(ADQLColumn column) throws NullPointerException{
		if (column == null)
			throw new NullPointerException("Impossible to compare nothing to NULL: no column has been given to build a IsNull constraint !");
		else{
			this.column = column;
			position = null;
		}
	}

	/**
	 * Tells whether the predicate is IS NULL or IS NOT NULL.
	 * 
	 * @return	<i>true</i> for IS NOT NULL, <i>false</i> for IS NULL.
	 */
	public final boolean isNotNull(){
		return isNotNull;
	}

	/**
	 * Lets indicating which predicate must be used (IS NULL or IS NOT NULL).
	 * 
	 * @param notNull	<i>true</i> for IS NOT NULL, <i>false</i> for IS NULL.
	 */
	public final void setNotNull(boolean notNull){
		isNotNull = notNull;
		position = null;
	}

	@Override
	public final TextPosition getPosition(){
		return position;
	}

	/**
	 * Set the position of this {@link IsNull} in the given ADQL query string.
	 * 
	 * @param position	New position of this {@link IsNull}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position){
		this.position = position;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new IsNull(this);
	}

	@Override
	public String getName(){
		return "IS" + (isNotNull ? " NOT " : " ") + "NULL";
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private boolean columnGot = (column == null);

			@Override
			public ADQLObject next(){
				if (columnGot)
					throw new NoSuchElementException();
				columnGot = true;
				return column;
			}

			@Override
			public boolean hasNext(){
				return !columnGot;
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (!columnGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLColumn){
					column = (ADQLColumn)replacer;
					position = null;
				}else
					throw new UnsupportedOperationException("Impossible to replace a column (" + column.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") in a IsNull constraint (" + toADQL() + ") !");
			}

			@Override
			public void remove(){
				if (!columnGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the only column (" + column.toADQL() + ") of a constraint IsNull (" + toADQL() + ") !");
			}
		};
	}

	@Override
	public String toADQL(){
		return column.toADQL() + " " + getName();
	}

}
