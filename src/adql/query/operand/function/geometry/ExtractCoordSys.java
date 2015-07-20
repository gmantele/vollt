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
 * <p>It represents the COORDSYS function the ADQL language.</p>
 * 
 * <p>This function extracts the coordinate system string value from a given geometry.</p>
 * 
 * <p><i><u>Example:</u><br />
 * COORDSYS(POINT('ICRS GEOCENTER', 25.0, -19.5))<br />
 * In this example the function extracts the coordinate system of a point with position (25, -19.5) in degrees according to the ICRS coordinate
 * system with GEOCENTER reference position.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public class ExtractCoordSys extends GeometryFunction {

	/** The geometry from which the coordinate system string must be extracted. */
	protected GeometryValue<GeometryFunction> geomExpr;

	/**
	 * Builds a COORDSYS function.
	 * 
	 * @param param	The geometry from which the coordinate system string must be extracted.
	 */
	public ExtractCoordSys(GeometryValue<GeometryFunction> param){
		super();
		geomExpr = param;
	}

	/**
	 * Builds a COORDSYS function by copying the given one.
	 * 
	 * @param toCopy		The COORDSYS function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public ExtractCoordSys(ExtractCoordSys toCopy) throws Exception{
		super();
		geomExpr = (GeometryValue<GeometryFunction>)(toCopy.geomExpr.getCopy());
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new ExtractCoordSys(this);
	}

	@Override
	public String getName(){
		return "COORDSYS";
	}

	@Override
	public boolean isNumeric(){
		return false;
	}

	@Override
	public boolean isString(){
		return true;
	}

	@Override
	public boolean isGeometry(){
		return false;
	}

	@Override
	public ADQLOperand[] getParameters(){
		return new ADQLOperand[]{geomExpr.getValue()};
	}

	@Override
	public int getNbParameters(){
		return 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index == 0)
			return geomExpr.getValue();
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function " + getName() + " !");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (index == 0){
			ADQLOperand replaced = geomExpr.getValue();
			if (replacer == null)
				throw new NullPointerException("Impossible to remove the only required parameter of the " + getName() + " function !");
			else if (replacer instanceof GeometryValue)
				geomExpr = (GeometryValue<GeometryFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				geomExpr.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				geomExpr.setGeometry((GeometryFunction)replacer);
			else
				throw new Exception("Impossible to replace GeometryValue/Column/GeometryFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
			setPosition(null);
			return replaced;
		}else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function " + getName() + " !");
	}

}
