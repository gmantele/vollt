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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * It represents the CIRCLE function of the ADQL language.
 *
 * <p>
 * 	This function expresses a circular region on the sky (a cone in space) and
 * 	it corresponds semantically to the equivalent term, Circle, defined in the
 * 	STC specification.
 * </p>
 *
 * <p>
 * 	The function arguments specify the center position and the radius, where:
 * </p>
 * <ul>
 * 	<li>the center position is given by a pair of numeric coordinates in
 * 		degrees, or a single geometric POINT</li>
 * 	<li>the radius is a numeric value in degrees</li>
 * 	<li>the center position and the radius MUST be within the ranges
 * 		[0,360] and [-90,90]</li>
 * </ul>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	A CIRCLE of ten degrees radius centered on position (25.4, -20.0) in degrees
 * 	could be written as follows:
 * </p>
 * <pre>CIRCLE(25.4, -20.0, 10.0)</pre>
 * <p>Alternatively, the center position may be expressed as a POINT:</p>
 * <pre>CIRCLE(POINT(25.4, -20.0), 10.0)</pre>
 * </i>
 *
 * <p>
 * 	The position argument may be a literal value, as above, or it may be a
 * 	column reference, function or expression that returns a geometric type.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>CIRCLE(t1.center, t1.radius)</pre>
 * <p>
 * 	, where t1.center and t1.radius are references to database columns that
 * 	contain POINT and DOUBLE values respectively.
 * </p>
 * </i>
 *
 * <p>
 * 	For historical reasons, the CIRCLE function accepts an optional string value
 * 	as the first argument. As of this version of the specification (2.1) this
 * 	parameter has been marked as deprecated. Future versions of this
 * 	specification (>2.1) may remove this parameter.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class CircleFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "CIRCLE", true, "Express a circular region on the sky (i.e. a cone in space).");

	/** The first coordinate of the center position. */
	private ADQLOperand coord1;

	/** The second coordinate of the center position. */
	private ADQLOperand coord2;

	/** The radius of the circle (in degrees). */
	private ADQLOperand radius;

	/**
	 * Builds a CIRCLE function.
	 *
	 * @param coordinateSystem		The coordinate system in which the center
	 *                        		position is expressed.
	 * @param firstCoord			The first coordinate of the center position.
	 * @param secondCoord			The second coordinate of the center
	 *                   			position.
	 * @param radius				The radius of the circle (in degrees).
	 * @throws NullPointerException	If at least one parameter is incorrect
	 *                             	or if the coordinate system is unknown.
	 * @throws Exception 			If there is another error.
	 */
	public CircleFunction(ADQLOperand coordinateSystem, ADQLOperand firstCoord, ADQLOperand secondCoord, ADQLOperand radius) throws NullPointerException, Exception {
		super(coordinateSystem);

		if (firstCoord == null || secondCoord == null || radius == null)
			throw new NullPointerException("All parameters of a CIRCLE function must be different from NULL!");

		coord1 = firstCoord;
		coord2 = secondCoord;
		this.radius = radius;
	}

	/**
	 * Builds a CIRCLE function by copying the given one.
	 *
	 * @param toCopy		The CIRCLE function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public CircleFunction(CircleFunction toCopy) throws Exception {
		super(toCopy);
		coord1 = (ADQLOperand)(toCopy.coord1.getCopy());
		coord2 = (ADQLOperand)(toCopy.coord2.getCopy());
		radius = (ADQLOperand)(toCopy.radius.getCopy());
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new CircleFunction(this);
	}

	@Override
	public String getName() {
		return "CIRCLE";
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

	@Override
	public boolean isString() {
		return false;
	}

	@Override
	public boolean isGeometry() {
		return true;
	}

	/**
	 * Gets the first coordinate of the center (i.e. right ascension).
	 *
	 * @return The first coordinate.
	 */
	public final ADQLOperand getCoord1() {
		return coord1;
	}

	/**
	 * Sets the first coordinate of the center (i.e. right ascension).
	 *
	 * @param coord1 The first coordinate.
	 */
	public final void setCoord1(ADQLOperand coord1) {
		this.coord1 = coord1;
		setPosition(null);
	}

	/**
	 * Gets the second coordinate of the center (i.e. declination).
	 *
	 * @return The second coordinate.
	 */
	public final ADQLOperand getCoord2() {
		return coord2;
	}

	/**
	 * Sets the second coordinate of the center (i.e. declination).
	 *
	 * @param coord2 The second coordinate.
	 */
	public final void setCoord2(ADQLOperand coord2) {
		this.coord2 = coord2;
		setPosition(null);
	}

	/**
	 * Gets the radius of the center.
	 *
	 * @return The radius.
	 */
	public final ADQLOperand getRadius() {
		return radius;
	}

	/**
	 * Sets the radius of the center.
	 *
	 * @param radius The radius.
	 */
	public final void setRadius(ADQLOperand radius) {
		this.radius = radius;
		setPosition(null);
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ coordSys, coord1, coord2, radius };
	}

	@Override
	public int getNbParameters() {
		return 4;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		switch(index) {
			case 0:
				return coordSys;
			case 1:
				return coord1;
			case 2:
				return coord2;
			case 3:
				return radius;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
		}
	}

	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (replacer == null)
			throw new NullPointerException("Impossible to remove one parameter of a " + getName() + " function!");

		ADQLOperand replaced = null;
		switch(index) {
			case 0:
				replaced = coordSys;
				setCoordinateSystem(replacer);
				break;
			case 1:
				replaced = coord1;
				coord1 = replacer;
				break;
			case 2:
				replaced = coord2;
				coord2 = replacer;
				break;
			case 3:
				replaced = radius;
				radius = replacer;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
		}
		setPosition(null);
		return replaced;
	}

}
