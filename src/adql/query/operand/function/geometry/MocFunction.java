package adql.query.operand.function.geometry;

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
 * Copyright 2016 - Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;
import adql.query.operand.UnknownType;

/**
 * <p>It represents a MOC function building a MOC from its ASCII serialisation.</p>
 * 
 * <p><b>WARNING:</b>
 * 	This function is NOT part of the standard ADQL language (v1.0).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (11/2016)
 */
public class MocFunction extends GeometryFunction {

	/** ADQL expression providing the ASCII serialisation of the MOC to represent. */
	protected ADQLOperand asciiMoc;

	/**
	 * Create an ADQL object representing the MOC function.
	 * 
	 * @param asciiMoc	The ASCII serialisation of the MOC to represent in this MOC function.
	 * 
	 * @throws NullPointerException	If the given operand is <code>null</code>
	 * @throws Exception			If the given operand is not a String expression.
	 */
	public MocFunction(final ADQLOperand asciiMoc) throws NullPointerException, Exception{
		super((ADQLOperand)null);

		setAsciiMoc(asciiMoc);
	}

	/**
	 * Builds a MOC function by copying the given one.
	 * 
	 * @param toCopy		The MOC function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public MocFunction(final MocFunction toCopy) throws Exception{
		super(toCopy);
		asciiMoc = (ADQLOperand)toCopy.asciiMoc.getCopy();
	}

	/**
	 * Get the ASCII serialisation of the MOC to represent
	 * 
	 * @return	The ASCII serialisation of the MOC to represent.
	 */
	public final ADQLOperand getAsciiMoc(){
		return asciiMoc;
	}

	/**
	 * Set the ASCII serialisation of the MOC to represent.
	 * 
	 * @param asciiMoc	The new ASCII serialisation of the MOC to represent.
	 * 
	 * @throws NullPointerException	If the given operand is <code>null</code>
	 * @throws Exception			If the given operand is not a String expression.
	 */
	public final void setAsciiMoc(final ADQLOperand asciiMoc) throws NullPointerException, Exception{
		if (asciiMoc == null)
			throw new NullPointerException("Missing MOC's ASCII serialisation!");
		else if (!asciiMoc.isString()){
			if (asciiMoc instanceof UnknownType){
				((UnknownType)asciiMoc).setExpectedType('S');
				this.asciiMoc = asciiMoc;
			}else
				throw new Exception("The function " + getName() + " needs a String expression as parameter!");
		}else{
			this.asciiMoc = asciiMoc;
		}
	}

	@Override
	public boolean isNumeric(){
		return false;
	}

	@Override
	public boolean isString(){
		return false;
	}

	@Override
	public boolean isGeometry(){
		return true;
	}

	@Override
	public String getName(){
		return "MOC";
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new MocFunction(this);
	}

	@Override
	public int getNbParameters(){
		return 1;
	}

	@Override
	public ADQLOperand[] getParameters(){
		return new ADQLOperand[]{asciiMoc};
	}

	@Override
	public ADQLOperand getParameter(final int index) throws ArrayIndexOutOfBoundsException{
		if (index != 0)
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + " !");
		else
			return asciiMoc;
	}

	@Override
	public ADQLOperand setParameter(final int index, final ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (replacer == null)
			throw new NullPointerException("Impossible to remove a parameter from the function " + getName() + " !");
		else if (index != 0)
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + " !");

		ADQLOperand replaced = asciiMoc;
		setAsciiMoc(replacer);
		return replaced;
	}

}
