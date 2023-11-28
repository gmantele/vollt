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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * It represents the POLYGON function of the ADQL language.
 *
 * <p>
 * 	This function expresses a region on the sky with boundaries denoted by great
 * 	circles passing through specified vertices. It corresponds semantically
 * 	to the STC Polygon.
 * </p>
 *
 * <p>
 * 	A polygon is described by a list of vertices in a single coordinate system,
 * 	with each vertex connected to the next along a great circle and the last
 * 	vertex implicitly connected to the first vertex.
 * </p>
 *
 * <p>The function arguments specify three or more vertices, where:</p>
 * <ul>
 * 	<li>the position of the vertices are given as a sequence of numeric
 * 		coordinates in degrees, or as a sequence of geometric POINTs</li>
 * 	<li>the numeric coordinates MUST be within the ranges [0,360] and
 * 		[-90,90]</li>
 * </ul>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	A function expressing a triangle, whose vertices are (10.0, -10.5),
 * 	(20.0, 20.5) and (30.0,30.5) in degrees would be written as follows:
 * </p>
 * <pre>POLYGON(10.0, -10.5, 20.0, 20.5, 30.0, 30.5)</pre>
 * <p>, where all numeric values are in degrees.</p>
 * <p>or alternatively as follows:</p>
 * <pre>POLYGON(POINT(10.0, -10.5), POINT(20.0, 20.5), POINT(30.0, 30.5))</pre>
 * </i>
 *
 * <p>
 * 	The coordinates for the POLYGON vertices may be literal values, as above, or
 * 	they may be column references, functions or expressions that return numeric
 * 	values.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>POLYGON(t1.ra, t1.dec + 5, t1.ra - 5, t1.dec - 5, t1.ra - 5, t1.dec + 5)</pre>
 * <p>
 * 	, where t1.ra and t1.dec are references to database columns that contain
 * 	numeric values.
 * </p>
 * </i>
 *
 * <p>
 * 	Alternatively, the coordinates for the POLYGON vertices may be column
 * 	references, functions or expressions that return POINT values.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>POLYGON(t2.toppoint, t2.bottomleft, t2.bottomright)</pre>
 * <p>
 * 	, where t2.toppoint, t2.bottomleft and t2.bottomright are references to
 * 	database columns that contain POINT values.
 * </p>
 * </i>
 *
 * <p>
 * 	The coordinates for the vertices MUST all be expressed in the same datatype.
 * 	The POLYGON function does not support a mixture of numeric and POINT
 * 	arguments.
 * </p>
 *
 * <p>
 * 	For historical reasons, the POLYGON function accepts an optional string
 * 	value as the first argument. As of this version of the specification (2.1)
 * 	this parameter has been marked as deprecated. Future versions of this
 * 	specification (> 2.1) may remove this parameter.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (06/2020)
 */
public class PolygonFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "POLYGON", true, "Express a region on the sky with boundaries denoted by great circles passing through specified coordinates.");

	/** The vertices. */
	protected Vector<ADQLOperand> coordinates;

	/**
	 * Builds a polygon function with at least 3 2-D coordinates (that is to
	 * say, the array must contain at least 6 operands).
	 *
	 * @param coordSystem						A string operand which
	 *                   						corresponds to a valid
	 *                   						coordinate system.
	 * @param coords							An array of at least 3 2-D
	 *              							coordinates (length>=6).
	 *
	 * @throws UnsupportedOperationException	If this function is not
	 *                                      	associated with a coordinate
	 *                                      	system.
	 * @throws NullPointerException				If one of the parameters is
	 *                             				NULL.
	 * @throws Exception						If there is another error.
	 */
	@SuppressWarnings("deprecation")
	public PolygonFunction(ADQLOperand coordSystem, ADQLOperand[] coords) throws UnsupportedOperationException, NullPointerException, Exception {
		super(coordSystem);
		if (coords == null || coords.length < 6)
			throw new NullPointerException("A POLYGON function must have at least 3 2-D coordinates!");
		else {
			coordinates = new Vector<ADQLOperand>(coords.length);
			for(int i = 0; i < coords.length; i++)
				coordinates.add(coords[i]);
		}
	}

	/**
	 * Builds a polygon function with at least:
	 *
	 * <ul>
	 * 	<li>3 pairs of coordinates (that is to say, the vector must contain at
	 * 		least 6 items),</li>
	 * 	<li>OR 3 point values (so, a vector containing at least 3 items).</li>
	 * </ul>
	 *
	 * @param coordSystem						A string operand which
	 *                   						corresponds to a valid
	 *                   						coordinate system.
	 * @param coords							A vector of at least 3 vertices.
	 *
	 * @throws UnsupportedOperationException	If this function is not
	 *                                      	associated with a coordinate
	 *                                      	system.
	 * @throws NullPointerException				If one of the parameters is
	 *                             				NULL.
	 * @throws Exception						If there is another error.
	 */
	@SuppressWarnings("deprecation")
	public PolygonFunction(ADQLOperand coordSystem, Collection<? extends ADQLOperand> coords) throws UnsupportedOperationException, NullPointerException, Exception {
		super(coordSystem);
		if (coords == null)
			throw new NullPointerException("A POLYGON function must have vertices!");

		Iterator<?> itCoords = coords.iterator();
		Object firstItem = (itCoords.hasNext() ? itCoords.next() : null);
		if (firstItem == null || (firstItem instanceof GeometryValue<?> && coords.size() < 3) || (!(firstItem instanceof GeometryValue<?>) && coords.size() < 6))
			throw new NullPointerException("A POLYGON function must have at least 3 vertices (i.e. 3 points or 3 pairs of coordinates)!");
		else {
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
	public PolygonFunction(PolygonFunction toCopy) throws Exception {
		super(toCopy);
		coordinates = new Vector<ADQLOperand>(toCopy.coordinates.size());
		for(ADQLOperand item : toCopy.coordinates)
			coordinates.add((ADQLOperand)(item.getCopy()));
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new PolygonFunction(this);
	}

	@Override
	public String getName() {
		return "POLYGON";
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

	@Override
	@SuppressWarnings("deprecation")
	public ADQLOperand[] getParameters() {
		ADQLOperand[] params = new ADQLOperand[coordinates.size() + 1];

		params[0] = coordSys;
		for(int i = 0; i < coordinates.size(); i++)
			params[i + 1] = coordinates.get(i);

		return params;
	}

	@Override
	public int getNbParameters() {
		return coordinates.size() + 1;
	}

	@Override
	@SuppressWarnings("deprecation")
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		if (index == 0)
			return coordSys;
		else if (index >= 1 && index <= coordinates.size())
			return coordinates.get(index - 1);
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" (" + toADQL() + ")!");
	}

	@Override
	@SuppressWarnings("deprecation")
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (replacer == null)
			throw new NullPointerException("Impossible to remove only one parameter from the function POLYGON!");

		ADQLOperand replaced = null;
		if (index == 0) {
			replaced = coordSys;
			setCoordinateSystem(replacer);
		} else if (index >= 1 && index <= coordinates.size()) {
			replaced = coordinates.get(index - 1);
			coordinates.set(index - 1, replacer);
		} else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" (" + toADQL() + ")!");

		setPosition(null);

		return replaced;
	}

}
