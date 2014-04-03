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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.NoSuchElementException;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;

/**
 * Lets wrapping an operand by parenthesis.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 */
public class WrappedOperand implements ADQLOperand {

	/** The wrapped operand. */
	private ADQLOperand operand;

	/**
	 * Wraps the given operand.
	 * 
	 * @param operand				Operand to wrap.
	 * 
	 * @throws NullPointerException	If the given operand is <i>NULL</i>.
	 */
	public WrappedOperand(ADQLOperand operand) throws NullPointerException {
		if (operand == null)
			throw new NullPointerException("Impossible to wrap a NULL operand: (NULL) has no sense !");
		this.operand = operand;
	}

	/**
	 * Gets the wrapped operand.
	 * 
	 * @return Its operand.
	 */
	public final ADQLOperand getOperand(){
		return operand;
	}

	public final boolean isNumeric() {
		return operand.isNumeric();
	}

	public final boolean isString() {
		return operand.isString();
	}

	public ADQLObject getCopy() throws Exception {
		return new WrappedOperand((ADQLOperand)operand.getCopy());
	}

	public String getName() {
		return "("+operand.getName()+")";
	}

	public ADQLIterator adqlIterator(){
		return new ADQLIterator() {

			private boolean operandGot = (operand == null);

			public ADQLObject next() {
				if (operandGot)
					throw new NoSuchElementException();
				operandGot = true;
				return operand;
			}

			public boolean hasNext() {
				return !operandGot;
			}

			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (!operandGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (replacer instanceof ADQLOperand)
					operand = (ADQLOperand)replacer;
				else
					throw new UnsupportedOperationException("Impossible to replace an ADQLOperand (\""+operand+"\") by a "+replacer.getClass().getName()+" (\""+replacer.toADQL()+"\") !");
			}

			public void remove() {
				if (!operandGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the only item of the WrappedOperand \""+toADQL()+"\": the WrappedOperand would be empty !");
			}
		};
	}

	public String toADQL() {
		return "("+operand.toADQL()+")";
	}

}
