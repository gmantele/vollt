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
 * It represents the POINT function of the ADQL language.
 *
 * <p>
 * 	This function expresses a single location on the sky, and it corresponds
 * 	semantically to the equivalent term, SpatialCoord, defined in the STC
 * 	specification.
 * </p>
 *
 * <p>The function arguments specify the position, where:</p>
 * <ul>
 * 	<li>the position is given by a pair of numeric coordinates in degrees</li>
 * 	<li>the numeric coordinates MUST be within the ranges resp. [0,360] and
 * 		[-90,90].</li>
 * </ul>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	A function expressing a point with right ascension of 25 degrees and
 * 	declination of -19.5 degrees would be written as follows:
 * </p>
 * <pre>POINT(25.0, -19.5)</pre>
 * <p>, where numeric values are in degrees.</p>
 * </i>
 *
 * <p>
 * 	The coordinates for POINT may be literal values, as above, or they may be
 * 	column references, functions or expressions that return numeric values.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>POINT(t.ra, t.dec)</pre>
 * <p>
 * 	, where t.ra and t.dec are references to database columns that contain
 * 	numeric values.
 * </p>
 *
 * <p>
 * 	For historical reasons, the POINT function accepts an optional string value
 * 	as the first argument. As of this version of the specification (2.1) this
 * 	parameter has been marked as deprecated. Future versions of this
 * 	specification (> 2.1) may remove this parameter.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (06/2020)
 */
public class PointFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "POINT", true, "Express a single location on the sky.");

	/** The first coordinate for this position. */
	private ADQLOperand coord1;

	/** The second coordinate for this position. */
	private ADQLOperand coord2;

	/**
	 * Builds a POINT function.
	 *
	 * @param coordinateSystem					The coordinate system to use.
	 * @param firstCoord						The first coordinate.
	 * @param secondCoord						The second coordinate.
	 * @throws UnsupportedOperationException	If this function is not
	 *                                      	associated with a coordinate
	 *                                      	system.
	 * @throws NullPointerException				If the given operand is NULL.
	 * @throws ParseException					If at least one of the given
	 *                       					parameters is incorrect.
	 */
	@SuppressWarnings("deprecation")
	public PointFunction(ADQLOperand coordinateSystem, ADQLOperand firstCoord, ADQLOperand secondCoord) throws UnsupportedOperationException, NullPointerException, Exception {
		super(coordinateSystem);

		if (firstCoord == null || secondCoord == null)
			throw new NullPointerException("The POINT function must have non-null coordinates!");

		coord1 = firstCoord;
		coord2 = secondCoord;
	}

	/**
	 * Builds a POINT function by copying the given one.
	 *
	 * @param toCopy		The POINT function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public PointFunction(PointFunction toCopy) throws Exception {
		super(toCopy);
		coord1 = (ADQLOperand)toCopy.coord1.getCopy();
		coord2 = (ADQLOperand)toCopy.coord2.getCopy();
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Gets the first coordinate of this point.
	 *
	 * @return Its first coordinate.
	 */
	public final ADQLOperand getCoord1() {
		return coord1;
	}

	/**
	 * Changes the first coordinate of this POINT function.
	 *
	 * @param coord1					Its new first coordinate.
	 * @throws NullPointerException		If the given operand is <i>null</i>.
	 * @throws Exception				If the given operand is not numeric.
	 */
	public void setCoord1(ADQLOperand coord1) throws NullPointerException, Exception {
		if (coord1 == null)
			throw new NullPointerException("The first coordinate of a POINT function must be different from NULL!");
		else if (!coord1.isNumeric())
			throw new Exception("Coordinates of a POINT function must be numeric!");
		else {
			this.coord1 = coord1;
			setPosition(null);
		}
	}

	/**
	 * Gets the second coordinate of this point.
	 *
	 * @return Its second coordinate.
	 */
	public final ADQLOperand getCoord2() {
		return coord2;
	}

	/**
	 * Changes the second coordinate of this POINT function.
	 *
	 * @param coord2				Its new second coordinate.
	 * @throws NullPointerException	If the given operand is <i>null</i>.
	 * @throws Exception			If the given operand is not numeric.
	 */
	public void setCoord2(ADQLOperand coord2) throws NullPointerException, Exception {
		if (coord2 == null)
			throw new NullPointerException("The second coordinate of a POINT function must be different from NULL!");
		else if (!coord2.isNumeric())
			throw new Exception("Coordinates of a POINT function must be numeric!");
		else {
			this.coord2 = coord2;
			setPosition(null);
		}
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new PointFunction(this);
	}

	@Override
	public String getName() {
		return "POINT";
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

	@SuppressWarnings("deprecation")
	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ coordSys, coord1, coord2 };
	}

	@Override
	public int getNbParameters() {
		return 3;
	}

	@Override
	@SuppressWarnings("deprecation")
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		switch(index) {
			case 0:
				return getCoordinateSystem();
			case 1:
				return getCoord1();
			case 2:
				return getCoord2();
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "!");
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (replacer == null)
			throw new NullPointerException("Impossible to remove a parameter from the function " + getName() + "!");

		ADQLOperand replaced = null;
		switch(index) {
			case 0:
				replaced = getCoordinateSystem();
				setCoordinateSystem(replacer);
				break;
			case 1:
				replaced = getCoord1();
				setCoord1(replacer);
				break;
			case 2:
				replaced = getCoord2();
				setCoord2(replacer);
				break;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
		}

		setPosition(null);

		return replaced;
	}

}
