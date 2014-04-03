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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.NoSuchElementException;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;

/**
 * Lets apply the logical operator NOT on any constraint.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 */
public class NotConstraint implements ADQLConstraint {

	private ADQLConstraint constraint;

	/**
	 * Builds a NotConstraint just with the constraint on which the logical operator NOT must be applied.
	 * 
	 * @param constraint			The constraint on which NOT must be applied.
	 * 
	 * @throws NullPointerException	If the given constraint is <i>null</i>.
	 */
	public NotConstraint(ADQLConstraint constraint) throws NullPointerException {
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

	public ADQLObject getCopy() throws Exception {
		return new NotConstraint((ADQLConstraint)constraint.getCopy());
	}

	public String getName() {
		return "NOT "+constraint.getName();
	}

	public ADQLIterator adqlIterator(){
		return new ADQLIterator() {

			private boolean constraintGot = (constraint == null);

			public ADQLObject next() {
				if (constraintGot)
					throw new NoSuchElementException();
				constraintGot = true;
				return constraint;
			}

			public boolean hasNext() {
				return !constraintGot;
			}

			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (!constraintGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLConstraint)
					constraint = (ADQLConstraint)replacer;
				else
					throw new UnsupportedOperationException("Impossible to replace an ADQLConstraint by a "+replacer.getClass().getName()+" !");
			}

			public void remove() {
				if (!constraintGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the constraint of a NOT predicated !");
			}
		};
	}

	public String toADQL() {
		return "NOT "+constraint.toADQL();
	}

}
