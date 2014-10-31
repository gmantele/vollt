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

import java.util.Collection;
import java.util.Vector;

import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * <p>It represents the POLYGON function of the ADQL language.</p>
 * 
 * <p> This function expresses a region on the sky with sides denoted by great circles passing through specified coordinates.
 * It corresponds semantically to the STC Polygon region. The arguments specify the coordinate system and three or more sets of
 * 2-D coordinates.</p>
 * 
 * <p>The polygon is a list of vertices in a single coordinate system, with each vertex connected to the next along a great circle
 * and the last vertex implicitly connected to the first vertex.</p>
 * 
 * <p><i><u>Example:</u><br />
 * POLYGON('ICRS GEOCENTER', 10.0, -10.5, 20.0, 20.5, 30.0, 30.5)<br />
 * In this example the function expresses a triangle, whose vertices are (10.0, -10.5), (20.0, 20.5) and (30.0, 30.5) in degrees
 * according to the STC coordinate system with GEOCENTER reference position.</i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public class PolygonFunction extends GeometryFunction {

	/** The coordinates of vertices. */
	protected Vector<ADQLOperand> coordinates;

	/**
	 * Builds a polygon function with at least 3 2-D coordinates (that is to say, the array must contain at least 6 operands).
	 * 
	 * @param coordSystem						A string operand which corresponds to a valid coordinate system.
	 * @param coords							An array of at least 3 2-D coordinates (length>=6).
	 * 
	 * @throws UnsupportedOperationException	If this function is not associated with a coordinate system.
	 * @throws NullPointerException				If one of the parameters is <i>null</i>.
	 * @throws Exception						If there is another error.
	 */
	public PolygonFunction(ADQLOperand coordSystem, ADQLOperand[] coords) throws UnsupportedOperationException, NullPointerException, Exception{
		super(coordSystem);
		if (coords == null || coords.length < 6)
			throw new NullPointerException("A POLYGON function must have at least 3 2-D coordinates !");
		else{
			coordinates = new Vector<ADQLOperand>(coords.length);
			for(int i = 0; i < coords.length; i++)
				coordinates.add(coords[i]);
		}
	}

	/**
	 * Builds a polygon function with at least 3 2-D coordinates (that is to say, the vector must contain at least 6 operands).
	 * 
	 * @param coordSystem						A string operand which corresponds to a valid coordinate system.
	 * @param coords							A vector of at least 3 2-D coordinates (size()>=6).
	 * 
	 * @throws UnsupportedOperationException	If this function is not associated with a coordinate system.
	 * @throws NullPointerException				If one of the parameters is <i>null</i>.
	 * @throws Exception						If there is another error.
	 */
	public PolygonFunction(ADQLOperand coordSystem, Collection<? extends ADQLOperand> coords) throws UnsupportedOperationException, NullPointerException, Exception{
		super(coordSystem);
		if (coords == null || coords.size() < 6)
			throw new NullPointerException("A POLYGON function must have at least 3 2-D coordinates !");
		else{
			coordinates = new Vector<ADQLOperand>(coords.size());
			coordinates.addAll(coords);
		}
	}

	/**
	 * Builds a POLYGON function by copying the given one.
	 * 
	 * @param toCopy		The POLYGON function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public PolygonFunction(PolygonFunction toCopy) throws Exception{
		super(toCopy);
		coordinates = new Vector<ADQLOperand>(toCopy.coordinates.size());
		for(ADQLOperand item : toCopy.coordinates)
			coordinates.add((ADQLOperand)(item.getCopy()));
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new PolygonFunction(this);
	}

	@Override
	public String getName(){
		return "POLYGON";
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
	public ADQLOperand[] getParameters(){
		ADQLOperand[] params = new ADQLOperand[coordinates.size() + 1];

		params[0] = coordSys;
		for(int i = 0; i < coordinates.size(); i++)
			params[i + 1] = coordinates.get(i);

		return params;
	}

	@Override
	public int getNbParameters(){
		return coordinates.size() + 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index == 0)
			return coordSys;
		else if (index >= 1 && index <= coordinates.size())
			return coordinates.get(index - 1);
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" (" + toADQL() + ") !");
	}

	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (replacer == null)
			throw new NullPointerException("Impossible to remove only one parameter from the function POLYGON !");

		ADQLOperand replaced = null;
		if (index == 0){
			replaced = coordSys;
			setCoordinateSystem(replacer);
		}else if (index >= 1 && index <= coordinates.size()){
			replaced = coordinates.get(index - 1);
			coordinates.set(index - 1, replacer);
		}else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" (" + toADQL() + ") !");
		return replaced;
	}

}
