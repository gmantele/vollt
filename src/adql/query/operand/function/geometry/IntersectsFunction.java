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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;

/**
 * <p>It represents the INTERSECTS function of the ADQL language.</p>
 * 
 * <p>This numeric function determines if two geometry values overlap.
 * This is most commonly used to express a "shape-vs-shape" intersection test.</p>
 * 
 * <p><i><u>Example:</u><br />
 * INTERSECTS(CIRCLE('ICRS GEOCENTER', 25.4, -20.0, 1), BOX('ICRS GEOCENTER', 20.0, -15.0, 10, 10)) = 1<br />
 * In this example the function determines whether the circle of one degree radius centered in a position (25.4, -20.0) degrees and defined
 * according to the ICRS coordinate system with GEOCENTER reference position overlaps with a box of ten degrees centered in a position
 *  (20.0, -15.0) in degrees and defined according to the same coordinate system.</i></p>
 * 
 * <p><b><u>Warning:</u>
 * <ul><li>The INTERSECTS function returns 1 (true) if the two arguments overlap and 0 (false) otherwise.</li>
 * <li>Since the two argument geometries may be expressed in different coordinate systems, the function is responsible for converting one (or both).
 * If it can not do so, it SHOULD throw an error message, to be defined by the service making use of ADQL.</li></ul>
 * </b></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public class IntersectsFunction extends GeometryFunction {

	/** The first geometry. */
	private GeometryValue<GeometryFunction> leftParam;

	/** The second geometry. */
	private GeometryValue<GeometryFunction> rightParam;

	/**
	 * Builds an INTERSECTS function.
	 * 
	 * @param param1				The first geometry.
	 * @param param2				The second geometry.
	 * @throws NullPointerException	If there is an error with at least one of the parameters.
	 */
	public IntersectsFunction(GeometryValue<GeometryFunction> param1, GeometryValue<GeometryFunction> param2) throws NullPointerException{
		super();
		if (param1 == null || param2 == null)
			throw new NullPointerException("An INTERSECTS function must have two parameters different from NULL !");

		leftParam = param1;
		rightParam = param2;
	}

	/**
	 * Builds an INTERSECTS function by copying the given one.
	 * 
	 * @param toCopy		The INTERSECTS function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public IntersectsFunction(IntersectsFunction toCopy) throws Exception{
		super();
		leftParam = (GeometryValue<GeometryFunction>)(toCopy.leftParam.getCopy());
		rightParam = (GeometryValue<GeometryFunction>)(toCopy.rightParam.getCopy());
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new IntersectsFunction(this);
	}

	@Override
	public String getName(){
		return "INTERSECTS";
	}

	@Override
	public boolean isNumeric(){
		return true;
	}

	@Override
	public boolean isString(){
		return false;
	}

	@Override
	public boolean isGeometry(){
		return false;
	}

	/**
	 * @return The leftParam.
	 */
	public final GeometryValue<GeometryFunction> getLeftParam(){
		return leftParam;
	}

	/**
	 * @param leftParam The leftParam to set.
	 */
	public final void setLeftParam(GeometryValue<GeometryFunction> leftParam){
		if (leftParam != null)
			this.leftParam = leftParam;
	}

	/**
	 * @return The rightParam.
	 */
	public final GeometryValue<GeometryFunction> getRightParam(){
		return rightParam;
	}

	/**
	 * @param rightParam The rightParam to set.
	 */
	public final void setRightParam(GeometryValue<GeometryFunction> rightParam){
		if (rightParam != null)
			this.rightParam = rightParam;
	}

	@Override
	public ADQLOperand[] getParameters(){
		return new ADQLOperand[]{leftParam,rightParam};
	}

	@Override
	public int getNbParameters(){
		return 2;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index == 0)
			return leftParam.getValue();
		else if (index == 1)
			return rightParam.getValue();
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
	}

	@Override
	@SuppressWarnings("unchecked")
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (replacer == null)
			throw new NullPointerException("Impossible to remove one parameter from a " + getName() + " function !");
		else if (!(replacer instanceof GeometryValue || replacer instanceof ADQLColumn || replacer instanceof GeometryFunction))
			throw new Exception("Impossible to replace a GeometryValue/Column/GeometryFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");

		ADQLOperand replaced = null;
		if (index == 0){
			replaced = leftParam;
			if (replacer instanceof GeometryValue)
				leftParam = (GeometryValue<GeometryFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				leftParam.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				leftParam.setGeometry((GeometryFunction)replacer);
		}else if (index == 1){
			replaced = rightParam;
			if (replacer instanceof GeometryValue)
				rightParam = (GeometryValue<GeometryFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				rightParam.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				rightParam.setGeometry((GeometryFunction)replacer);
		}else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
		return replaced;
	}

}