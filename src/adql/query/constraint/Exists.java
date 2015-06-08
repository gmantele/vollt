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
import adql.query.ADQLQuery;
import adql.query.TextPosition;

/**
 * <p>Represents the predicate EXISTS of SQL and ADQL.</p>
 * 
 * <p>This function returns <i>true</i> if the sub-query given in parameter returns at least one result, else it returns <i>false</i>.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public class Exists implements ADQLConstraint {

	/** The sub-query. */
	private ADQLQuery subQuery;

	/** Position of this {@link Exists} in the given ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Builds an Exists constraint instance.
	 * 
	 * @param query	Its sub-query.
	 */
	public Exists(ADQLQuery query){
		subQuery = query;
	}

	/**
	 * Builds an Exists constraint by copying the given one.
	 * 
	 * @param toCopy		The Exists constraint to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public Exists(Exists toCopy) throws Exception{
		subQuery = (ADQLQuery)toCopy.subQuery.getCopy();
		position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
	}

	/**
	 * Gets the sub-query of this EXISTS constraint.
	 * 
	 * @return	Its sub-query.
	 */
	public final ADQLQuery getSubQuery(){
		return subQuery;
	}

	/**
	 * Replaces the sub-query of this EXISTS constraint by the given one.
	 * 
	 * @param query					Its new sub-query.
	 * @throws NullPointerException	If the given query is <i>null</i>.
	 */
	public void setSubQuery(ADQLQuery query) throws NullPointerException{
		if (query == null)
			throw new NullPointerException("Impossible to build an EXISTS constraint with a sub-query NULL !");
		else{
			subQuery = query;
			position = null;
		}
	}

	@Override
	public final TextPosition getPosition(){
		return position;
	}

	/**
	 * Set the position of this {@link Exists} in the given ADQL query string.
	 * 
	 * @param position	New position of this {@link Exists}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position){
		this.position = position;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new Exists(this);
	}

	@Override
	public String getName(){
		return "EXISTS";
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private boolean subQueryGot = (subQuery == null);

			@Override
			public ADQLObject next(){
				if (subQueryGot)
					throw new NoSuchElementException();
				subQueryGot = true;
				return subQuery;
			}

			@Override
			public boolean hasNext(){
				return !subQueryGot;
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (!subQueryGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLQuery){
					subQuery = (ADQLQuery)replacer;
					position = null;
				}else
					throw new UnsupportedOperationException("Impossible to replace an ADQLQuery by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
			}

			@Override
			public void remove(){
				if (!subQueryGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the sub-query of an EXISTS constraint !");
			}
		};
	}

	@Override
	public String toADQL(){
		return getName() + "(" + subQuery.toADQL() + ")";
	}

}