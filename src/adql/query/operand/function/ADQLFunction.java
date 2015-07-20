package adql.query.operand.function;

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

import java.util.Iterator;
import java.util.NoSuchElementException;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;

/**
 * Represents any kind of function.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public abstract class ADQLFunction implements ADQLOperand {

	/** Position of this {@link ADQLFunction} in the ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	@Override
	public final TextPosition getPosition(){
		return position;
	}

	/**
	 * Set the position of this {@link ADQLFunction} in the ADQL query string.
	 * 
	 * @param position	New position of this {@link ADQLFunction}
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position){
		this.position = position;
	}

	/**
	 * Gets the number of parameters this function has.
	 * 
	 * @return	Number of parameters.
	 */
	public abstract int getNbParameters();

	/**
	 * Gets the list of all parameters of this function.
	 * 
	 * @return	Its parameters list.
	 */
	public abstract ADQLOperand[] getParameters();

	/**
	 * Gets the index-th parameter.
	 * 
	 * @param index								Parameter number.
	 * @return									The corresponding parameter.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If the index is incorrect (index < 0 || index >= getNbParameters()).
	 */
	public abstract ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException;

	/**
	 * Replaces the index-th parameter by the given one.
	 * 
	 * @param index									Index of the parameter to replace.
	 * @param replacer								The replacer.
	 * 
	 * @return										The replaced parameter.
	 * 
	 * @throws ArrayIndexOutOfBoundsException		If the index is incorrect (index < 0 || index >= getNbParameters()).
	 * @throws NullPointerException					If a required parameter must be replaced by a NULL object.
	 * @throws Exception							If another error occurs.
	 */
	public abstract ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception;

	/**
	 * Creates an iterator on the parameters of this function.
	 * 
	 * @return	Parameters iterator.
	 */
	public Iterator<ADQLOperand> paramIterator(){
		return new ParameterIterator(this);
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private int index = -1;

			@Override
			public ADQLObject next(){
				try{
					return getParameter(++index);
				}catch(ArrayIndexOutOfBoundsException ex){
					throw new NoSuchElementException();
				}
			}

			@Override
			public boolean hasNext(){
				return index + 1 < getNbParameters();
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLOperand){
					try{
						setParameter(index, (ADQLOperand)replacer);
					}catch(Exception e){
						e.printStackTrace();
						throw new UnsupportedOperationException(e);
					}
				}else
					throw new UnsupportedOperationException("Impossible to replace the " + index + "-th parameter of \"" + toADQL() + "\" by an object whose the class (" + replacer.getClass().getName() + ") is not ADQLOperand !");
			}

			@Override
			public void remove(){
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove a parameter of an ADQL function (here the " + index + "-th parameter of \"" + toADQL() + "\")");
			}
		};
	}

	@Override
	public String toADQL(){
		String adql = getName() + "(";

		for(int i = 0; i < getNbParameters(); i++)
			adql += ((i == 0) ? "" : ", ") + getParameter(i).toADQL();

		return adql + ")";
	}

	/**
	 * Lets iterating on all parameters of the given function.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 01/2012
	 */
	protected class ParameterIterator implements Iterator<ADQLOperand> {

		protected final ADQLFunction function;
		protected int index = -1;

		public ParameterIterator(ADQLFunction fct) throws NullPointerException{
			if (fct == null)
				throw new NullPointerException("Impossible to build an iterator on a function without the function on which the iterator must be applied !");
			else
				function = fct;
		}

		@Override
		public boolean hasNext(){
			return (index + 1) < function.getNbParameters();
		}

		@Override
		public ADQLOperand next(){
			index++;
			return function.getParameter(index);
		}

		@Override
		public void remove() throws UnsupportedOperationException{
			try{
				function.setParameter(index, null);
			}catch(Exception e){
				throw new UnsupportedOperationException(e);
			}
		}

	}

}