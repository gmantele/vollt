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

/**
 * Lets apply the logical operator NOT on any constraint.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public class NotConstraint implements ADQLConstraint {

	private ADQLConstraint constraint;

	/** Position of this {@link NotConstraint} in the ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Builds a NotConstraint just with the constraint on which the logical operator NOT must be applied.
	 * 
	 * @param constraint			The constraint on which NOT must be applied.
	 * 
	 * @throws NullPointerException	If the given constraint is <i>null</i>.
	 */
	public NotConstraint(ADQLConstraint constraint) throws NullPointerException{
		if (constraint == null)
			throw new NullPointerException("Impossible to apply the logical operator NOT on a NULL constraint !");
		this.constraint = constraint;
	}

	/**
	 * Gets the constraint on which the NOT operator is applied.
	 * 
	 * @return	An {@link ADQLConstraint}.
	 */
	public final ADQLConstraint getConstraint(){
		return constraint;
	}

	@Override
	public final TextPosition getPosition(){
		return position;
	}

	/**
	 * Set the position of this {@link NotConstraint} in the given ADQL query string.
	 * 
	 * @param position	New position of this {@link NotConstraint}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position){
		this.position = position;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new NotConstraint((ADQLConstraint)constraint.getCopy());
	}

	@Override
	public String getName(){
		return "NOT " + constraint.getName();
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private boolean constraintGot = (constraint == null);

			@Override
			public ADQLObject next(){
				if (constraintGot)
					throw new NoSuchElementException();
				constraintGot = true;
				return constraint;
			}

			@Override
			public boolean hasNext(){
				return !constraintGot;
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (!constraintGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLConstraint){
					constraint = (ADQLConstraint)replacer;
					position = null;
				}else
					throw new UnsupportedOperationException("Impossible to replace an ADQLConstraint by a " + replacer.getClass().getName() + " !");
			}

			@Override
			public void remove(){
				if (!constraintGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the constraint of a NOT predicated !");
			}
		};
	}

	@Override
	public String toADQL(){
		return "NOT " + constraint.toADQL();
	}

}
