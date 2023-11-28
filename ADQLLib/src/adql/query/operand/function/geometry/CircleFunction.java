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
 * Copyright 2012-2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;
import adql.query.operand.UnknownType;

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
 * 	column reference, function or expression that returns a POINT value.
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
 * @version 2.0 (04/2021)
 */
public class CircleFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "CIRCLE", true, "Express a circular region on the sky (i.e. a cone in space).");

	/** Center position.
	 * <p><i><b>Note:</b>
	 * 	NULL, if both {@link #coord1} and {@link #coord2} are provided.
	 * </i></p>
	 * @since 2.0 */
	private GeometryValue<GeometryFunction> centerPoint;

	/** The first coordinate of the center position.
	 * <p><i><b>Note:</b>
	 * 	NULL, if {@link #centerPoint} is provided.
	 * </i></p> */
	private ADQLOperand coord1;

	/** The second coordinate of the center position.
	 * <p><i><b>Note:</b>
	 * 	NULL, if {@link #centerPoint} is provided.
	 * </i></p> */
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
	@SuppressWarnings("deprecation")
	public CircleFunction(ADQLOperand coordinateSystem, ADQLOperand firstCoord, ADQLOperand secondCoord, ADQLOperand radius) throws NullPointerException, Exception {
		super(coordinateSystem);

		if (firstCoord == null || secondCoord == null || radius == null)
			throw new NullPointerException("All parameters of a CIRCLE function must be different from NULL!");

		coord1 = firstCoord;
		coord2 = secondCoord;
		centerPoint = null;
		this.radius = radius;
	}

	/**
	 * Builds a CIRCLE function.
	 *
	 * @param coordinateSystem		The coordinate system in which the center
	 *                        		position is expressed.
	 * @param center				The center position.
	 * @param radius				The radius of the circle (in degrees).
	 * @throws NullPointerException	If at least one parameter is incorrect
	 *                             	or if the coordinate system is unknown.
	 * @throws Exception 			If there is another error.
	 *
	 * @since 2.0
	 */
	@SuppressWarnings("deprecation")
	public CircleFunction(ADQLOperand coordinateSystem, GeometryValue<GeometryFunction> center, ADQLOperand radius) throws NullPointerException, Exception {
		super(coordinateSystem);

		if (center == null || radius == null)
			throw new NullPointerException("All parameters of a CIRCLE function must be different from NULL!");

		coord1 = null;
		coord2 = null;
		centerPoint = center;
		this.radius = radius;
	}

	/**
	 * Builds a CIRCLE function by copying the given one.
	 *
	 * @param toCopy		The CIRCLE function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public CircleFunction(CircleFunction toCopy) throws Exception {
		super(toCopy);
		coord1 = (ADQLOperand)(toCopy.coord1.getCopy());
		coord2 = (ADQLOperand)(toCopy.coord2.getCopy());
		centerPoint = (GeometryValue<GeometryFunction>)(toCopy.centerPoint.getCopy());
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
	 * Gets the center point, exactly as provided.
	 *
	 * <p><b>IMPORTANT NOTE:</b>
	 * 	If this {@link CircleFunction} has been initialized with a pair of
	 * 	coordinates, this function will return NULL.
	 * </p>
	 *
	 * @return	The center point of the represented circle,
	 *        	or NULL if created with a pair of coordinates.
	 *
	 * @since 2.0
	 */
	public final GeometryValue<GeometryFunction> getCenter() {
		return centerPoint;
	}

	/**
	 * Sets the center point.
	 *
	 * <p><b>WARNING:</b>
	 * 	Calling this function will erase the single coordinates already set:
	 * 	{@link #getCoord1()} and {@link #getCoord2()} will both return NULL.
	 * </p>
	 *
	 * @param newCenter	The new center point of the represented circle.
	 *
	 * @since 2.0
	 */
	public final void setCenter(final GeometryValue<GeometryFunction> newCenter) {
		centerPoint = newCenter;
		coord1 = null;
		coord2 = null;
		setPosition(null);
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
	 * <p><b>WARNING:</b>
	 * 	Calling this function will erase the center point already set:
	 * 	{@link #getCenter()} will return NULL.
	 * </p>
	 *
	 * @param coord1 The first coordinate.
	 */
	public final void setCoord1(ADQLOperand coord1) {
		this.coord1 = coord1;
		centerPoint = null;
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
		centerPoint = null;
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
	@SuppressWarnings("deprecation")
	public ADQLOperand[] getParameters() {
		if (centerPoint == null)
			return new ADQLOperand[]{ coordSys, coord1, coord2, radius };
		else
			return new ADQLOperand[]{ coordSys, centerPoint.getValue(), radius };
	}

	@Override
	public int getNbParameters() {
		return (centerPoint == null ? 4 : 3);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		if (centerPoint == null) {
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
		} else {
			switch(index) {
				case 0:
					return coordSys;
				case 1:
					return centerPoint.getValue();
				case 2:
					return radius;
				default:
					throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
			}
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "deprecation" })
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (replacer == null)
			throw new NullPointerException("Impossible to remove one parameter of a " + getName() + " function!");

		ADQLOperand replaced = null;
		if (centerPoint == null) {
			switch(index) {
				case 0:
					replaced = coordSys;
					setCoordinateSystem(replacer);
					break;
				case 1:
					replaced = coord1;
					setCoord1(replacer);
					break;
				case 2:
					replaced = coord2;
					setCoord2(replacer);
					break;
				case 3:
					replaced = radius;
					setRadius(replacer);
					break;
				default:
					throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
			}
		} else {
			switch(index) {
				case 0:
					replaced = coordSys;
					setCoordinateSystem(replacer);
					break;
				case 1:
					replaced = centerPoint.getValue();
					if (replacer instanceof GeometryValue)
						setCenter((GeometryValue<GeometryFunction>)replacer);
					else if (replaced instanceof UnknownType)
						centerPoint.setUnknownTypeValue((UnknownType)replaced);
					else if (replaced instanceof GeometryFunction)
						centerPoint.setGeometry((GeometryFunction)replaced);
					else
						throw new Exception("Impossible to replace a GeometryValue/GeometryFunction/UnknownType by " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
					break;
				case 2:
					replaced = radius;
					setRadius(replacer);
					break;
				default:
					throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
			}
		}
		return replaced;
	}

}
