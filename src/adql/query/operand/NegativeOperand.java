package adql.query.operand;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.NoSuchElementException;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;

/**
 * Lets putting a minus sign in front of any numeric operand.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public final class NegativeOperand implements ADQLOperand {

	private ADQLOperand operand;

	/**
	 * <p>Builds an operand which will negativate the given operand.</p>
	 * 
	 * <p><b><u>Important:</u> The given operand must be numeric ({@link ADQLOperand#isNumeric()} must return <i>true</i>) !</b></p>
	 * 
	 * @param operand							The operand to negativate.
	 * 
	 * @throws NullPointerException				If the given operand is <i>null</i>.
	 * @throws UnsupportedOperationException	If the given operand is not numeric (if {@link ADQLOperand#isNumeric()} does not return <i>true</i>).
	 */
	public NegativeOperand(ADQLOperand operand) throws NullPointerException, UnsupportedOperationException{
		if (operand == null)
			throw new NullPointerException("Impossible to negativate an operand equals to NULL !");

		if (operand.isNumeric())
			this.operand = operand;
		else
			throw new UnsupportedOperationException("Impossible to negativate a non-numeric operand (" + operand.toADQL() + ") !");
	}

	/**
	 * Gets the operand on which the minus sign is applied.
	 * 
	 * @return	The negativated operand.
	 */
	public final ADQLOperand getOperand(){
		return operand;
	}

	/** Always returns <i>true</i>.
	 * @see adql.query.operand.ADQLOperand#isNumeric()
	 */
	@Override
	public final boolean isNumeric(){
		return true;
	}

	/** Always returns <i>false</i>.
	 * @see adql.query.operand.ADQLOperand#isString()
	 */
	@Override
	public final boolean isString(){
		return false;
	}

	/** Always returns <i>false</i>.
	 * @see adql.query.operand.ADQLOperand#isGeometry()
	 */
	@Override
	public final boolean isGeometry(){
		return false;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		NegativeOperand copy = new NegativeOperand((ADQLOperand)operand.getCopy());
		return copy;
	}

	@Override
	public String getName(){
		return operand.getName();
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private boolean operandGot = (operand == null);

			@Override
			public ADQLObject next(){
				if (operandGot)
					throw new NoSuchElementException();
				operandGot = true;
				return operand;
			}

			@Override
			public boolean hasNext(){
				return !operandGot;
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (!operandGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLOperand && ((ADQLOperand)replacer).isNumeric())
					operand = (ADQLOperand)replacer;
				else
					throw new UnsupportedOperationException("Impossible to replace the operand \"" + operand.toADQL() + "\" by \"" + replacer.toADQL() + "\" in the NegativeOperand \"" + toADQL() + "\" because the replacer is not an ADQLOperand or is not numeric !");
			}

			@Override
			public void remove(){
				if (!operandGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the only operand (" + operand.toADQL() + ") of a NegativeOperand (" + toADQL() + "). However you can remove the whole NegativeOperand.");
			}
		};
	}

	@Override
	public String toADQL(){
		return "-" + operand.toADQL();
	}

}
