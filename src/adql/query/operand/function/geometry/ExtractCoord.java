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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;

/**
 * <p>It represents the COORD1 and the COORD2 functions of the ADQL language.</p>
 * 
 * <p>This function extracts the first or the second coordinate value (in degrees) of a given POINT or column reference.</p>
 * 
 * <p><i><u>Example:</u><br />
 * COORD1(POINT('ICRS GEOCENTER', 25.0, -19.5))<br />
 * In this example the function extracts the right ascension of a point with position (25, -19.5) in degrees according to the ICRS coordinate
 * system with GEOCENTER reference position.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public class ExtractCoord extends GeometryFunction {

	/** Number of the coordinate to extract (1 or 2). */
	protected final int indCoord;

	/** The point from which the coordinate must be extracted. */
	protected GeometryValue<PointFunction> point;

	/**
	 * Builds a COORD1 or a COORD2 function with the given point (a POINT function or a column which contains a POINT function).
	 * 
	 * @param indiceCoord						1 or 2: the index of the coordinate to extract.
	 * @param p									The POINT function from which the <i>indiceCoord</i>-th coordinate must be extracted.
	 * @throws ArrayIndexOutOfBoundsException	If the given index is different from 1 and 2.
	 * @throws NullPointerException				If the given geometry is <i>null</i>.
	 */
	public ExtractCoord(int indiceCoord, GeometryValue<PointFunction> p) throws ArrayIndexOutOfBoundsException, NullPointerException{
		super();
		if (indiceCoord <= 0 || indiceCoord > 2)
			throw new ArrayIndexOutOfBoundsException("Impossible to extract another coordinate that the two first: only COORD1 and COORD2 exists in ADQL !");
		indCoord = indiceCoord;

		if (p == null)
			throw new NullPointerException("Impossible to build a COORD" + indCoord + " function without a point (a POINT function or a column which contains a POINT function) !");
		point = p;
	}

	/**
	 * Builds a COORD1 or a COORD2 function by copying the given one.
	 * 
	 * @param toCopy		The COORD1 or the COORD2 to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public ExtractCoord(ExtractCoord toCopy) throws Exception{
		super();
		indCoord = toCopy.indCoord;
		point = (GeometryValue<PointFunction>)(toCopy.point.getCopy());
	}

	@Override
	public String getName(){
		return "COORD" + indCoord;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new ExtractCoord(this);
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

	@Override
	public ADQLOperand[] getParameters(){
		return new ADQLOperand[]{point.getValue()};
	}

	@Override
	public int getNbParameters(){
		return 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index == 0)
			return point.getValue();
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
	}

	@Override
	@SuppressWarnings("unchecked")
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (index == 0){
			if (replacer == null)
				throw new NullPointerException("Impossible to remove the only required parameter of the function " + getName() + " !");
			ADQLOperand replaced = point.getValue();
			if (replacer instanceof GeometryValue)
				point = (GeometryValue<PointFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				point.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof PointFunction)
				point.setGeometry((PointFunction)replacer);
			else
				throw new Exception("Impossible to replace GeometryValue/Column/PointFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
			setPosition(null);
			return replaced;
		}else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
	}

}
