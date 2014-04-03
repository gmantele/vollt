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

import java.util.NoSuchElementException;

import adql.query.operand.ADQLOperand;

/**
 * <p>Represents an item of a SELECT clause.</p>
 * 
 * <p>It merely encapsulates an operand and allows to associate to it an alias (according to the following syntax: "SELECT operand AS alias").</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 * 
 * @see ClauseSelect
 */
public class SelectItem implements ADQLObject {

	/** The corresponding operand. */
	private ADQLOperand operand;

	/** Alias of the operand (ADQL syntax: "AS alias"). */
	private String alias = null;

	/** Indicates whether the alias is case sensitive (if yes, the alias is written between double-quotes). */
	private boolean caseSensitive = false;

	/**
	 * Builds a SELECT item just with an operand.
	 * 
	 * @param operand	Corresponding operand.
	 */
	public SelectItem(ADQLOperand operand){
		this(operand, null);
	}

	/**
	 * Builds a SELECT item with an operand and its alias.
	 * 
	 * @param operand	Corresponding operand.
	 * @param alias		Operand alias.
	 */
	public SelectItem(ADQLOperand operand, String alias){
		this.operand = operand;
		setAlias(alias);
	}

	/**
	 * Builds a SELECT item by copying the given one.
	 * 
	 * @param toCopy		The SELECT item to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public SelectItem(SelectItem toCopy) throws Exception{
		if (toCopy.getOperand() != null)
			operand = (ADQLOperand)toCopy.getOperand().getCopy();
		else
			operand = null;
		alias = toCopy.getAlias();
		caseSensitive = toCopy.caseSensitive;
	}

	/**
	 * Gets the corresponding operand.
	 * 
	 * @return	The corresponding operand.
	 */
	public final ADQLOperand getOperand(){
		return operand;
	}

	/**
	 * Indicates whether the corresponding operand has an alias.
	 * 
	 * @return	<i>true</i> if there is an alias, <i>false</i> otherwise.
	 */
	public final boolean hasAlias(){
		return alias != null;
	}

	/**
	 * Gets the alias of the corresponding operand.
	 * 
	 * @return	Alias of the operand.
	 */
	public final String getAlias(){
		return alias;
	}

	/**
	 * Changes the alias of the corresponding operand.
	 * 
	 * @param newAlias	The new alias of the operand.
	 */
	public final void setAlias(String newAlias){
		alias = newAlias;
		caseSensitive = false;

		if (alias != null){
			StringBuffer a = new StringBuffer(alias);
			a.trimToSize();
			if (a.length() == 0){
				alias = null;
				return;
			}else if (a.length() > 1 && a.charAt(0) == '\"' && a.charAt(a.length() - 1) == '\"'){
				a.deleteCharAt(0);
				a.deleteCharAt(a.length() - 1);
				a.trimToSize();
				if (a.length() == 0){
					alias = null;
					return;
				}
				caseSensitive = true;
			}
			alias = a.toString();
		}
	}

	/**
	 * Tells whether the alias is case sensitive.
	 * 
	 * @return	<i>true</i> if the alias is case sensitive, <i>false</i> otherwise.
	 */
	public final boolean isCaseSensitive(){
		return caseSensitive;
	}

	/**
	 * Sets the case sensitivity on the alias.
	 * 
	 * @param sensitive		<i>true</i> to make case sensitive the alias, <i>false</i> otherwise.
	 */
	public final void setCaseSensitive(boolean sensitive){
		caseSensitive = sensitive;
	}

	public ADQLObject getCopy() throws Exception{
		return new SelectItem(this);
	}

	public String getName(){
		return hasAlias() ? alias : operand.getName();
	}

	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private boolean operandGot = (operand == null);

			public ADQLObject next() throws NoSuchElementException{
				if (operandGot)
					throw new NoSuchElementException();
				operandGot = true;
				return operand;
			}

			public boolean hasNext(){
				return !operandGot;
			}

			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (replacer == null)
					remove();
				else if (!operandGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");
				else if (!(replacer instanceof ADQLOperand))
					throw new IllegalStateException("Impossible to replace an ADQLOperand by a " + replacer.getClass().getName() + " !");
				else
					operand = (ADQLOperand)replacer;
			}

			public void remove(){
				if (!operandGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the only operand (" + operand.toADQL() + ") from a SelectItem (" + toADQL() + ") !");
			}
		};
	}

	public String toADQL(){
		StringBuffer adql = new StringBuffer(operand.toADQL());
		if (hasAlias()){
			adql.append(" AS ");
			if (isCaseSensitive())
				adql.append('\"').append(alias).append('\"');
			else
				adql.append(alias);
		}
		return adql.toString();
	}

}
