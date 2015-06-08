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
 * <p>It represents the DISTANCE function of the ADQL language.</p>
 * 
 * <p>This function computes the arc length along a great circle between two points, and returns a numeric value expression in degrees.</p>
 * 
 * <p><i><u>Example:</u><br />
 * DISTANCE(POINT('ICRS GEOCENTER', 25.0, -19.5), POINT('ICRS GEOCENTER', 25.4, -20.0))<br />
 * In this example the function computes the distance between two points of coordinates (25, -19.5) and (25.4, -20) both expressed according to the ICRS
 * coordinate system with GEOCENTER reference position.</i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public class DistanceFunction extends GeometryFunction {

	/** The first point. */
	private GeometryValue<PointFunction> p1;

	/** The second point. */
	private GeometryValue<PointFunction> p2;

	/**
	 * Builds a DISTANCE function.
	 * 
	 * @param point1				The first point.
	 * @param point2				The second point.
	 * @throws NullPointerException	If one of the parameters are incorrect.
	 */
	public DistanceFunction(GeometryValue<PointFunction> point1, GeometryValue<PointFunction> point2) throws NullPointerException{
		super();
		if (point1 == null || point2 == null)
			throw new NullPointerException("All parameters of the DISTANCE function must be different from null !");

		p1 = point1;
		p2 = point2;
	}

	/**
	 * Builds a DISTANCE function by copying the given one.
	 * 
	 * @param toCopy		The DISTANCE function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public DistanceFunction(DistanceFunction toCopy) throws Exception{
		super(toCopy);
		p1 = (GeometryValue<PointFunction>)(toCopy.p1.getCopy());
		p2 = (GeometryValue<PointFunction>)(toCopy.p2.getCopy());
	}

	@Override
	public void setCoordinateSystem(ADQLOperand coordSys) throws UnsupportedOperationException{
		throw new UnsupportedOperationException("A DISTANCE function is not associated to a coordinate system !");
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new DistanceFunction(this);
	}

	@Override
	public String getName(){
		return "DISTANCE";
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
	 * Gets the first point.
	 * 
	 * @return A point.
	 */
	public final GeometryValue<PointFunction> getP1(){
		return p1;
	}

	/**
	 * Sets the first point.
	 * 
	 * @param p1 A point.
	 */
	public final void setP1(GeometryValue<PointFunction> p1){
		this.p1 = p1;
		setPosition(null);
	}

	/**
	 * Gets the second point.
	 * 
	 * @return A point.
	 */
	public final GeometryValue<PointFunction> getP2(){
		return p2;
	}

	/**
	 * Sets the second point.
	 * 
	 * @param p2 A point.
	 */
	public final void setP2(GeometryValue<PointFunction> p2){
		this.p2 = p2;
		setPosition(null);
	}

	@Override
	public ADQLOperand[] getParameters(){
		return new ADQLOperand[]{p1.getValue(),p2.getValue()};
	}

	@Override
	public int getNbParameters(){
		return 2;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		switch(index){
			case 0:
				return p1.getValue();
			case 1:
				return p2.getValue();
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (replacer == null)
			throw new NullPointerException("Impossible to remove a parameter from the function " + getName() + " !");
		else if (!(replacer instanceof GeometryValue || replacer instanceof ADQLColumn || replacer instanceof PointFunction))
			throw new Exception("Impossible to replace a GeometryValue/Column/PointFunction by " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");

		ADQLOperand replaced = null;
		GeometryValue<PointFunction> toUpdate = null;
		switch(index){
			case 0:
				replaced = p1.getValue();
				if (replacer instanceof GeometryValue)
					p1 = (GeometryValue<PointFunction>)replacer;
				else
					toUpdate = p1;
				break;
			case 1:
				replaced = p2.getValue();
				if (replacer instanceof GeometryValue)
					p2 = (GeometryValue<PointFunction>)replacer;
				else
					toUpdate = p2;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
		}

		if (toUpdate != null){
			if (replacer instanceof ADQLColumn)
				toUpdate.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof PointFunction)
				toUpdate.setGeometry((PointFunction)replacer);
		}

		setPosition(null);

		return replaced;
	}

}
