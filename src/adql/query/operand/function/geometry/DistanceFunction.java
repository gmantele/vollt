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
 * Copyright 2012-2020 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.function.UserDefinedFunction;

/**
 * It represents the DISTANCE function of the ADQL language.
 *
 * <p>
 * 	This function computes the arc length along a great circle between two
 * 	points and returns a numeric value expression in degrees.
 * </p>
 *
 * <p>
 * 	The specification (2.1) defines two versions of the DISTANCE function, one
 * 	that accepts two POINT values (the only supported in 2.0), and a second that
 * 	accepts four separate numeric values.
 * </p>
 *
 * <p><i><b>Implementation note:</b>
 * 	In this current implementation, the 2-argument form allows 2 geometries
 * 	instead of 2 points. The goal is to be more generic. POINT is supposed to
 * 	be the main expected type of argument, but it could also be a CENTROID
 * 	(which returns a POINT). Moreover, some extension of this library might
 * 	want to support DISTANCE between any type of geometries instead of just
 * 	points.
 * </i></p>
 *
 * <p>
 * 	If an ADQL service implementation declares support for DISTANCE, then it
 * 	must implement both the two parameter and four parameter forms of the
 * 	function.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	An expression calculating the distance between two points of coordinates
 * 	(25,-19.5) and (25.4,-20) could be written as follows:
 * </p>
 * <pre>DISTANCE(POINT(25.0, -19.5), POINT(25.4, -20.0))</pre>
 * <p>, where all numeric values and the returned arc length are in degrees.</p>
 * <p>
 * 	The equivalent call to the four parameter form of the function would be:
 * </p>
 * <pre>DISTANCE(25.0, -19.5, 25.4, -20.0)</pre>
 * </i>
 *
 * <p>
 * 	The DISTANCE function may be applied to any expression that returns a
 * 	geometric POINT value.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	The distance between two points stored in the database could be calculated
 * 	as follows:
 * </p>
 * <pre>DISTANCE(t1.base, t2.target)</pre>
 * <p>
 * 	, where t1.base and t2.target are references to database columns that
 * 	contain POINT values.
 * </p>
 * </i>
 *
 * <p>
 * 	If the geometric arguments are expressed in different coordinate systems,
 * 	the DISTANCE function is responsible for converting one, or both, of the
 * 	arguments into a different coordinate system. If the DISTANCE function
 * 	cannot perform the required conversion then it SHOULD throw an error.
 * 	Details of the mechanism for reporting the error condition are
 * 	implementation dependent.
 * </p>
 *
 * <p>
 * 	It is assumed that the arguments for the four numeric parameter form all use
 * 	the same coordinate system.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (04/2020)
 */
public class DistanceFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "DISTANCE", true, "Compute the arc length along a great circle between two points and returns a numeric value expression in degrees.");

	/** The first point. */
	private GeometryValue<GeometryFunction> p1;

	/** The second point. */
	private GeometryValue<GeometryFunction> p2;

	/**
	 * Builds a DISTANCE function.
	 *
	 * @param point1				The first point.
	 * @param point2				The second point.
	 * @throws NullPointerException	If one of the parameters are incorrect.
	 */
	public DistanceFunction(GeometryValue<GeometryFunction> point1, GeometryValue<GeometryFunction> point2) throws NullPointerException {
		super();
		if (point1 == null || point2 == null)
			throw new NullPointerException("All parameters of the DISTANCE function must be different from null!");

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
	public DistanceFunction(DistanceFunction toCopy) throws Exception {
		super(toCopy);
		p1 = (GeometryValue<GeometryFunction>)(toCopy.p1.getCopy());
		p2 = (GeometryValue<GeometryFunction>)(toCopy.p2.getCopy());
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public void setCoordinateSystem(ADQLOperand coordSys) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("A DISTANCE function is not associated to a coordinate system!");
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new DistanceFunction(this);
	}

	@Override
	public String getName() {
		return "DISTANCE";
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

	@Override
	public boolean isString() {
		return false;
	}

	@Override
	public boolean isGeometry() {
		return false;
	}

	/**
	 * Gets the first point.
	 *
	 * @return A point.
	 */
	public final GeometryValue<GeometryFunction> getP1() {
		return p1;
	}

	/**
	 * Sets the first point.
	 *
	 * @param p1 A point.
	 */
	public final void setP1(GeometryValue<GeometryFunction> p1) {
		this.p1 = p1;
		setPosition(null);
	}

	/**
	 * Gets the second point.
	 *
	 * @return A point.
	 */
	public final GeometryValue<GeometryFunction> getP2() {
		return p2;
	}

	/**
	 * Sets the second point.
	 *
	 * @param p2 A point.
	 */
	public final void setP2(GeometryValue<GeometryFunction> p2) {
		this.p2 = p2;
		setPosition(null);
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ p1.getValue(), p2.getValue() };
	}

	@Override
	public int getNbParameters() {
		return 2;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		switch(index) {
			case 0:
				return p1;
			case 1:
				return p2;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (replacer == null)
			throw new NullPointerException("Impossible to remove a parameter from the function " + getName() + "!");
		else if (!(replacer instanceof GeometryValue || replacer instanceof ADQLColumn || replacer instanceof GeometryFunction || replacer instanceof UserDefinedFunction))
			throw new Exception("Impossible to replace a GeometryValue/Column/GeometryFunction/UDF by " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");

		ADQLOperand replaced = null;
		GeometryValue<GeometryFunction> toUpdate = null;
		switch(index) {
			case 0:
				replaced = p1.getValue();
				if (replacer instanceof GeometryValue)
					p1 = (GeometryValue<GeometryFunction>)replacer;
				else
					toUpdate = p1;
				break;
			case 1:
				replaced = p2.getValue();
				if (replacer instanceof GeometryValue)
					p2 = (GeometryValue<GeometryFunction>)replacer;
				else
					toUpdate = p2;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
		}

		if (toUpdate != null) {
			if (replacer instanceof ADQLColumn)
				toUpdate.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				toUpdate.setGeometry((GeometryFunction)replacer);
			else if (replacer instanceof UserDefinedFunction)
				toUpdate.setUDF((UserDefinedFunction)replacer);
		}

		setPosition(null);

		return replaced;
	}

}
