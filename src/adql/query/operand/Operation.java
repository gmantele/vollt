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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.NoSuchElementException;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.TextPosition;

/**
 * It represents a simple numeric operation (sum, difference, multiplication and division).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 * 
 * @see OperationType
 */
public class Operation implements ADQLOperand {

	/** Part of the operation at the left of the operator. */
	private ADQLOperand leftOperand;

	/**
	 * Operation symbol: +, -, * ,/ ,...
	 * @see OperationType
	 */
	private OperationType operation;

	/** Part of the operation at the right of the operator. */
	private ADQLOperand rightOperand;

	/** Position of the operation in the ADQL query string.
	 * @since 1.4 */
	private TextPosition position = null;

	/**
	 * Builds an operation.
	 * 
	 * @param leftOp				Left operand.
	 * @param op					Operation symbol.
	 * @param rightOp				Right operand.
	 * 
	 * @throws NullPointerException	If one of the given parameters is <i>null</i>.
	 * 
	 * @see Operation#setLeftOperand(ADQLOperand)
	 * @see Operation#setRightOperand(ADQLOperand)
	 */
	public Operation(ADQLOperand leftOp, OperationType op, ADQLOperand rightOp) throws NullPointerException, UnsupportedOperationException{

		setLeftOperand(leftOp);

		if (op == null)
			throw new NullPointerException("Impossible to build an Operation without an operation type (SUM, SUB, MULT or DIV) !");
		else
			operation = op;

		setRightOperand(rightOp);

		position = null;
	}

	/**
	 * Builds an Operation by copying the given one.
	 * 
	 * @param toCopy		The Operand to copy.
	 * 
	 * @throws Exception	If there is an error during the copy.
	 */
	public Operation(Operation toCopy) throws Exception{
		leftOperand = (ADQLOperand)toCopy.leftOperand.getCopy();
		operation = toCopy.operation;
		rightOperand = (ADQLOperand)toCopy.rightOperand.getCopy();
		position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
	}

	/**
	 * Gets the left part of the operation.
	 * 
	 * @return The left operand.
	 */
	public final ADQLOperand getLeftOperand(){
		return leftOperand;
	}

	/**
	 * Changes the left operand of this operation.
	 * 
	 * @param newLeftOperand					The new left operand.
	 * 
	 * @throws NullPointerException				If the given operand is <i>null</i>.
	 * @throws UnsupportedOperationException	If the given operand is not numeric (see {@link ADQLOperand#isNumeric()}).
	 */
	public void setLeftOperand(ADQLOperand newLeftOperand) throws NullPointerException, UnsupportedOperationException{
		if (newLeftOperand == null)
			throw new NullPointerException("Impossible to update an Operation with a left operand equals to NULL !");
		else if (!newLeftOperand.isNumeric())
			throw new UnsupportedOperationException("Impossible to update an Operation because the left operand is not numeric (" + newLeftOperand.toADQL() + ") !");

		leftOperand = newLeftOperand;

		position = null;
	}

	/**
	 * Gets the operation symbol.
	 * 
	 * @return The operation type.
	 * @see OperationType
	 */
	public final OperationType getOperation(){
		return operation;
	}

	/**
	 * Changes the type of this operation (SUM, SUB, MULT, DIV).
	 * 
	 * @param newOperation	The new type of this operation.
	 * 
	 * @see OperationType
	 */
	public void setOperation(OperationType newOperation){
		if (newOperation != null)
			operation = newOperation;
	}

	/**
	 * Gets the right part of the operation.
	 * 
	 * @return The right operand.
	 */
	public final ADQLOperand getRightOperand(){
		return rightOperand;
	}

	/**
	 * Changes the right operand of this operation.
	 * 
	 * @param newRightOperand					The new right operand of this operation.
	 * 
	 * @throws NullPointerException				If the given operand is <i>null</i>.
	 * @throws UnsupportedOperationException	If the given operand is not numeric (see {@link ADQLOperand#isNumeric()}).
	 */
	public void setRightOperand(ADQLOperand newRightOperand) throws NullPointerException, UnsupportedOperationException{
		if (newRightOperand == null)
			throw new NullPointerException("Impossible to update an Operation with a right operand equals to NULL !");
		else if (!newRightOperand.isNumeric())
			throw new UnsupportedOperationException("Impossible to update an Operation because the right operand is not numeric (" + newRightOperand.toADQL() + ") !");

		rightOperand = newRightOperand;

		position = null;
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

	@Override
	public final TextPosition getPosition(){
		return this.position;
	}

	/**
	 * Sets the position at which this {@link WrappedOperand} has been found in the original ADQL query string.
	 * 
	 * @param position	Position of this {@link WrappedOperand}.
	 * @since 1.4
	 */
	public final void setPosition(final TextPosition position){
		this.position = position;
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
		return new Operation(this);
	}

	@Override
	public String getName(){
		return operation.name();
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private int index = -1;
			private ADQLOperand operand = null;

			@Override
			public ADQLObject next(){
				index++;

				if (index == 0)
					operand = leftOperand;
				else if (index == 1)
					operand = rightOperand;
				else
					throw new NoSuchElementException();
				return operand;
			}

			@Override
			public boolean hasNext(){
				return index + 1 < 2;
			}

			@Override
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else{
					if (replacer instanceof ADQLOperand && ((ADQLOperand)replacer).isNumeric()){
						if (index == 0)
							leftOperand = (ADQLOperand)replacer;
						else if (index == 1)
							rightOperand = (ADQLOperand)replacer;
						position = null;
					}else
						throw new UnsupportedOperationException("Impossible to replace the operand \"" + operand.toADQL() + "\" by \"" + replacer.toADQL() + "\" in the operation \"" + toADQL() + "\" because the replacer is not an ADQLOperand or is not numeric !");
				}
			}

			@Override
			public void remove(){
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove one operand (" + operand.toADQL() + ") of an operation (" + toADQL() + "). However you can replace the whole operation by the remaining operand.");
			}
		};
	}

	@Override
	public String toADQL(){
		return leftOperand.toADQL() + operation.toADQL() + rightOperand.toADQL();
	}

}
