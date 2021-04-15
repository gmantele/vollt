package adql.db.region;

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
 * Copyright 2014-2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;

import adql.parser.ADQLQueryFactory;
import adql.parser.grammar.ParseException;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NegativeOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

/**
 * Object representation of a geometric region.
 *
 * <p>
 * 	This class contains a field for each possible parameter of a region.
 * 	Depending of the region type some are not used. In such case, these unused
 * 	fields are set to NULL.
 * </p>
 *
 * <p>
 * 	An instance of this class can be easily serialized into any supported
 * 	string serialization:
 * </p>
 * <ul>
 * 	<li>
 * 		STC-S using {@link #toSTCS()}, {@link #toFullSTCS()}.
 * 		{@link #toFullSTCS()} will display default value explicit on the
 * 		contrary to {@link #toSTCS()} which will replace them by empty strings.
 * 	</li>
 * 	<li>
 * 		DALI using {@link #toDALI()}.
 * 	</li>
 * </ul>
 * <p>
 * 	The default serialization is returned by {@link #toString()}. The default
 * 	syntax is the one defined by DALI. However, if the type of the represented
 * 	region is not supported by DALI or if a specific Coordinate System (which is
 * 	not the default one) is used, then the STC/s syntax is used.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 2.0 (04/2021)
 * @since 1.3
 */
public class Region {

	/**
	 * List all possible region types allowed in an STC-S expression.
	 *
	 * <p><i><b>Note:</b>
	 * 	The possible values listed in this enumeration are limited to the subset
	 * 	of STC-S described by the section "6 Use of STC-S in TAP (informative)"
	 * 	of the TAP Recommendation 1.0 (27th March 2010).
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static enum RegionType {
		POSITION, CIRCLE, BOX, POLYGON, UNION, INTERSECTION, NOT;
	}

	/** Type of the region. */
	public final RegionType type;

	/** Coordinate system used by this region.
	 * <p><i><b>Note:</b>
	 * 	Only the NOT region does not declare a coordinate system ; so only for
	 * 	this region this field is NULL.
	 * </i></p> */
	public final CoordSys coordSys;

	/** List of coordinates' pairs. The second dimension of this array
	 * represents a pair of coordinates ; it is then an array of two elements.
	 * <p><i><b>Note:</b>
	 * 	This field is used by POINT, BOX, CIRCLE and POLYGON.
	 * </i></p> */
	public final double[][] coordinates;

	/** Width of the BOX region. */
	public final double width;

	/** Height of the BOX region. */
	public final double height;

	/** Radius of the CIRCLE region. */
	public final double radius;

	/** List of regions unified (UNION), intersected (INTERSECTION) or
	 * avoided (NOT). */
	public final Region[] regions;

	/** STC-S representation of this region, in which default values of the
	 * coordinate system (if any) are not written (they are replaced by empty
	 * strings).
	 * <p><i><b>Note:</b>
	 * 	This attribute is NULL until the first call of the function
	 * 	{@link #toSTCS()} where it is built.
	 * </i></p> */
	private String stcs = null;

	/** STC-S representation of this region, in which default values of the
	 * coordinate system (if any) are explicitly written.
	 * <p><i><b>Note:</b>
	 * 	This attribute is NULL until the first call of the function
	 * 	{@link #toFullSTCS()} where it is built.
	 * </i></p> */
	private String fullStcs = null;

	/** The ADQL function object representing this region.
	 * <p><i><b>Note:</b>
	 * 	This attribute is NULL until the first call of the function
	 * 	{@link #toGeometry()} or {@link #toGeometry(ADQLQueryFactory)}.
	 * </i></p> */
	private GeometryFunction geometry = null;

	/**
	 * Constructor for a POINT/POSITION region.
	 *
	 * <p><i><b>Important note:</b>
	 * 	The array of coordinates is used like that. No copy is done.
	 * </i></p>
	 *
	 * @param coordSys		Coordinate system.
	 *                		<i>note: It MAY BE null ; if so, the default
	 *                		coordinate system will be chosen</li>
	 * @param coordinates	A pair of coordinates ; coordinates[0] and
	 *                   	coordinates[1].
	 */
	public Region(final CoordSys coordSys, final double[] coordinates) {
		this(coordSys, new double[][]{ coordinates });
	}

	/**
	 * Constructor for a POINT/POSITION or a POLYGON region.
	 *
	 * <p>
	 * 	Whether it is a polygon or a point depends on the number of given
	 * 	coordinates:
	 * </p>
	 * <ul>
	 * 	<li>1 item => POINT/POSITION</li>
	 * 	<li>more items => POLYGON</li>
	 * </ul>
	 *
	 * <p><i><b>Important note:</b>
	 * 	The array of coordinates is used like that. No copy is done.
	 * </i></p>
	 *
	 * @param coordSys		Coordinate system.
	 *                		<i>note: It MAY BE null ; if so, the default
	 *                		coordinate system will be chosen</li>
	 * @param coordinates	List of coordinates' pairs ;
	 *                   	coordinates[n] = 1 pair = 2 items (coordinates[n][0]
	 *                   	and coordinates[n][1]) ; if 1 pair, it is a
	 *                   	POINT/POSITION, but if more, it is a POLYGON.
	 */
	public Region(final CoordSys coordSys, final double[][] coordinates) {
		// Check roughly the coordinates:
		if (coordinates == null || coordinates.length == 0)
			throw new NullPointerException("Missing coordinates!");
		else if (coordinates[0].length != 2)
			throw new IllegalArgumentException("Wrong number of coordinates! Expected at least 2 pairs of coordinates (so coordinates[0], coordinates[1] and coordinates[n].length = 2).");

		// Decide of the region type in function of the number of coordinates' pairs:
		type = (coordinates.length > 1) ? RegionType.POLYGON : RegionType.POSITION;

		// Set the coordinate system (if NULL, choose the default one):
		this.coordSys = (coordSys == null ? new CoordSys() : coordSys);

		// Set the coordinates:
		this.coordinates = coordinates;

		// Set the other fields as not used:
		width = Double.NaN;
		height = Double.NaN;
		radius = Double.NaN;
		regions = null;
	}

	/**
	 * Constructor for a CIRCLE region.
	 *
	 * <p><i><b>Important note:</b>
	 * 	The array of coordinates is used like that. No copy is done.
	 * </i></p>
	 *
	 * @param coordSys		Coordinate system.
	 *                		<i>note: It MAY BE null ; if so, the default
	 *                		coordinate system will be chosen</li>
	 * @param coordinates	A pair of coordinates ; coordinates[0] and
	 *                   	coordinates[1].
	 * @param radius		The circle radius.
	 */
	public Region(final CoordSys coordSys, final double[] coordinates, final double radius) {
		// Check roughly the coordinates:
		if (coordinates == null || coordinates.length == 0)
			throw new NullPointerException("Missing coordinates!");
		else if (coordinates.length != 2)
			throw new IllegalArgumentException("Wrong number of coordinates! Expected exactly 2 values.");

		// Set the region type:
		type = RegionType.CIRCLE;

		// Set the coordinate system (if NULL, choose the default one):
		this.coordSys = (coordSys == null ? new CoordSys() : coordSys);

		// Set the coordinates:
		this.coordinates = new double[][]{ coordinates };

		// Set the radius:
		this.radius = radius;

		// Set the other fields as not used:
		width = Double.NaN;
		height = Double.NaN;
		regions = null;
	}

	/**
	 * Constructor for a BOX region.
	 *
	 * <p><i><b>Important note:</b>
	 * 	The array of coordinates is used like that. No copy is done.
	 * </i></p>
	 *
	 * @param coordSys		Coordinate system.
	 *                		<i>note: It MAY BE null ; if so, the default
	 *                		coordinate system will be chosen</li>
	 * @param coordinates	A pair of coordinates ; coordinates[0] and
	 *                   	coordinates[1].
	 * @param width			Width of the box.
	 * @param height		Height of the box.
	 */
	public Region(final CoordSys coordSys, final double[] coordinates, final double width, final double height) {
		// Check roughly the coordinates:
		if (coordinates == null || coordinates.length == 0)
			throw new NullPointerException("Missing coordinates!");
		else if (coordinates.length != 2)
			throw new IllegalArgumentException("Wrong number of coordinates! Expected exactly 2 values.");

		// Set the region type:
		type = RegionType.BOX;

		// Set the coordinate system (if NULL, choose the default one):
		this.coordSys = (coordSys == null ? new CoordSys() : coordSys);

		// Set the coordinates:
		this.coordinates = new double[][]{ coordinates };

		// Set the size of the box:
		this.width = width;
		this.height = height;

		// Set the other fields as not used:
		radius = Double.NaN;
		regions = null;
	}

	/**
	 * Constructor for a UNION or INTERSECTION region.
	 *
	 * <p><i><b>Important note:</b>
	 * 	The array of regions is used like that. No copy is done.
	 * </i></p>
	 *
	 * @param unionOrIntersection	Type of the region to create.
	 *                           	<i>Note: It can be ONLY a UNION or
	 *                           	INTERSECTION. Another value will throw an
	 *                           	IllegalArgumentException).</i>
	 * @param coordSys				Coordinate system.
	 *                				<i>note: It MAY BE null ; if so, the default
	 *                				coordinate system will be chosen</li>
	 * @param regions				Regions to unite or to intersect.
	 *               				<i>Note: At least two regions must be
	 *               				provided.</i>
	 */
	public Region(final RegionType unionOrIntersection, final CoordSys coordSys, final Region[] regions) {
		// Check the type:
		if (unionOrIntersection == null)
			throw new NullPointerException("Missing type of region (UNION or INTERSECTION here)!");
		else if (unionOrIntersection != RegionType.UNION && unionOrIntersection != RegionType.INTERSECTION)
			throw new IllegalArgumentException("Wrong region type: \"" + unionOrIntersection + "\"! This constructor lets create only an UNION or INTERSECTION region.");

		// Check the list of regions:
		if (regions == null || regions.length == 0)
			throw new NullPointerException("Missing regions to " + (unionOrIntersection == RegionType.UNION ? "unite" : "intersect") + "!");
		else if (regions.length < 2)
			throw new IllegalArgumentException("Wrong number of regions! Expected at least 2 regions.");

		// Set the region type:
		type = unionOrIntersection;

		// Set the coordinate system (if NULL, choose the default one):
		this.coordSys = (coordSys == null ? new CoordSys() : coordSys);

		// Set the regions:
		this.regions = regions;

		// Set the other fields as not used:
		coordinates = null;
		radius = Double.NaN;
		width = Double.NaN;
		height = Double.NaN;
	}

	/**
	 * Constructor for a NOT region.
	 *
	 * @param region	Any region to not select.
	 */
	public Region(final Region region) {
		// Check the region parameter:
		if (region == null)
			throw new NullPointerException("Missing region to NOT select!");

		// Set the region type:
		type = RegionType.NOT;

		// Set the regions:
		this.regions = new Region[]{ region };

		// Set the other fields as not used:
		coordSys = null;
		coordinates = null;
		radius = Double.NaN;
		width = Double.NaN;
		height = Double.NaN;
	}

	/**
	 * Build a Region from the given ADQL representation.
	 *
	 * <p><i><b>Note:</b>
	 * 	Only {@link PointFunction}, {@link CircleFunction}, {@link BoxFunction},
	 * 	{@link PolygonFunction} and {@link RegionFunction} are accepted here.
	 * 	Other extensions of {@link GeometryFunction} will throw an
	 * 	{@link IllegalArgumentException}.
	 * </i></p>
	 *
	 * @param geometry	The ADQL representation of the region to create here.
	 *
	 * @throws IllegalArgumentException	If the given geometry is neither of
	 *                                 	{@link PointFunction},
	 *                                 	{@link BoxFunction},
	 *                                 	{@link PolygonFunction}
	 *                                 	and {@link RegionFunction}.
	 * @throws ParseException			If the declared coordinate system, the
	 *                       			coordinates or the STC-S definition has
	 *                       			a wrong syntax.
	 */
	public Region(final GeometryFunction geometry) throws IllegalArgumentException, ParseException {
		if (geometry == null)
			throw new NullPointerException("Missing geometry to convert into STCS.Region!");

		if (geometry instanceof PointFunction) {
			type = RegionType.POSITION;
			coordSys = STCS.parseCoordSys(extractString(geometry.getCoordinateSystem()));
			coordinates = new double[][]{ { extractNumeric(((PointFunction)geometry).getCoord1()), extractNumeric(((PointFunction)geometry).getCoord2()) } };
			width = Double.NaN;
			height = Double.NaN;
			radius = Double.NaN;
			regions = null;
		} else if (geometry instanceof CircleFunction) {
			type = RegionType.CIRCLE;
			coordSys = STCS.parseCoordSys(extractString(geometry.getCoordinateSystem()));
			coordinates = new double[][]{ { extractNumeric(((CircleFunction)geometry).getCoord1()), extractNumeric(((CircleFunction)geometry).getCoord2()) } };
			radius = extractNumeric(((CircleFunction)geometry).getRadius());
			width = Double.NaN;
			height = Double.NaN;
			regions = null;
		} else if (geometry instanceof BoxFunction) {
			type = RegionType.BOX;
			coordSys = STCS.parseCoordSys(extractString(geometry.getCoordinateSystem()));
			coordinates = new double[][]{ { extractNumeric(((BoxFunction)geometry).getCoord1()), extractNumeric(((BoxFunction)geometry).getCoord2()) } };
			width = extractNumeric(((BoxFunction)geometry).getWidth());
			height = extractNumeric(((BoxFunction)geometry).getHeight());
			radius = Double.NaN;
			regions = null;
		} else if (geometry instanceof PolygonFunction) {
			PolygonFunction poly = (PolygonFunction)geometry;
			type = RegionType.POLYGON;
			coordSys = STCS.parseCoordSys(extractString(poly.getCoordinateSystem()));
			coordinates = new double[(poly.getNbParameters() - 1) / 2][2];
			for(int i = 0; i < coordinates.length; i++)
				coordinates[i] = new double[]{ extractNumeric(poly.getParameter(1 + i * 2)), extractNumeric(poly.getParameter(2 + i * 2)) };
			width = Double.NaN;
			height = Double.NaN;
			radius = Double.NaN;
			regions = null;
		} else if (geometry instanceof RegionFunction) {
			Region r = parse(extractString(((RegionFunction)geometry).getParameter(0)));
			type = r.type;
			coordSys = r.coordSys;
			coordinates = r.coordinates;
			width = r.width;
			height = r.height;
			radius = r.radius;
			regions = r.regions;
		} else
			throw new IllegalArgumentException("Unknown region type! Only geometrical function PointFunction, CircleFunction, BoxFunction, PolygonFunction and RegionFunction are allowed.");
	}

	/**
	 * Extract a string value from the given {@link ADQLOperand}
	 * which is <b>expected to be a {@link StringConstant} instance</b>.
	 *
	 * @param op	A string operand.
	 *
	 * @return	The string value embedded in the given operand.
	 *
	 * @throws ParseException	If the given operand is not an instance of
	 *                       	{@link StringConstant}.
	 */
	private static String extractString(final ADQLOperand op) throws ParseException {
		if (op == null)
			throw new NullPointerException("Missing operand!");
		else if (op instanceof StringConstant)
			return ((StringConstant)op).getValue();
		else
			throw new ParseException("Can not convert into STC-S a non string argument (including ADQLColumn and Concatenation)!");
	}

	/**
	 * Extract a numeric value from the given {@link ADQLOperand}
	 * which is <b>expected to be a {@link NumericConstant} instance</b>
	 * or a {@link NegativeOperand} embedding a {@link NumericConstant}.
	 *
	 * @param op	A numeric operand.
	 *
	 * @return	The numeric value embedded in the given operand.
	 *
	 * @throws ParseException	If the given operand is not an instance of
	 *                       	{@link NumericConstant} or a
	 *                       	{@link NegativeOperand}.
	 */
	private static double extractNumeric(final ADQLOperand op) throws ParseException {
		if (op == null)
			throw new NullPointerException("Missing operand!");
		else if (op instanceof NumericConstant)
			return Double.parseDouble(((NumericConstant)op).getValue());
		else if (op instanceof NegativeOperand)
			return extractNumeric(((NegativeOperand)op).getOperand()) * -1;
		else
			throw new ParseException("Can not convert into STC-S a non numeric argument (including ADQLColumn and Operation)!");
	}

	/**
	 * Parse the given string serialization of a region.
	 *
	 * <p><i><b>Note:</b>
	 * 	Only two serializations are supported and tested (in this order):
	 * 	DALI and STC/s.
	 * </i></p>
	 *
	 * @param strRegion	The string serialization to parse.
	 *
	 * @return	The corresponding region.
	 *
	 * @throws ParseException	If the used serialization is not supported.
	 *
	 * @since 2.0
	 */
	public static Region parse(final String strRegion) throws ParseException {
		Region region = null;

		// First try as a DALI serialization:
		try {
			region = DALI.parseRegion(strRegion);
		} catch(ParseException pe) {
		}

		// Then, try as an STC/s serialization:
		if (region == null) {
			try {
				region = STCS.parseRegion(strRegion);
			} catch(ParseException pe) {
			}
		}

		// If still not working, throw an error:
		if (region == null)
			throw new ParseException("Unsupported region serialization!");

		return region;
	}

	/**
	 * Get the DALI representation of this region.
	 *
	 * <p><i><b>Warning:</b>
	 * 	The DALI serialization can not include the coordinate system
	 * 	information. Only the coordinates, radius, width and height are then
	 * 	used for this representation. No conversion is done. These values are
	 * 	returned as provided in the given coordinate system.
	 * </i></p>
	 *
	 * @return Its DALI representation.
	 */
	public String toDALI() {
		return DALI.toDALI(this);
	}

	/**
	 * Get the STC-S representation of this region (in which default values
	 * of the coordinate system are not written ; they are replaced by empty
	 * strings).
	 *
	 * <p><i><b>Note:</b>
	 * 	This function build the STC-S just once and store it in a class
	 * 	attribute. The value of this attribute is then returned at next calls of
	 * 	this function.
	 * </i></p>
	 *
	 * @return	Its STC-S representation.
	 *
	 * @see STCS#toSTCS(Region)
	 */
	public String toSTCS() {
		if (stcs != null)
			return stcs;
		else
			return (stcs = STCS.toSTCS(this));
	}

	/**
	 * Get the STC-S representation of this region (in which default values
	 * of the coordinate system are explicitly written).
	 *
	 * <p><i><b>Note:</b>
	 * 	This function build the STC-S just once and store it in a class
	 * 	attribute. The value of this attribute is then returned at next calls of
	 * 	this function.
	 * </i></p>
	 *
	 * @return	Its STC-S representation.
	 *
	 * @see STCS#toSTCS(Region, boolean)
	 */
	public String toFullSTCS() {
		if (fullStcs != null)
			return fullStcs;
		else
			return (fullStcs = STCS.toSTCS(this, true));
	}

	@Override
	public String toString() {
		// Serialize with DALI syntax:
		String str = toDALI();

		/* But if the region type is not supported or if there is a specific
		 * Coordinate System, use the STC/s syntax instead: */
		if (str == null || (coordSys != null && !coordSys.isDefault()))
			str = toSTCS();

		return str;
	}

	/**
	 * Convert this region into its corresponding ADQL representation.
	 *
	 * <ul>
	 * 	<li><b>POSITION:</b> {@link PointFunction}</li>
	 * 	<li><b>CIRCLE:</b> {@link CircleFunction}</li>
	 * 	<li><b>BOX:</b> {@link BoxFunction}</li>
	 * 	<li><b>POLYGON:</b> {@link PolygonFunction}</li>
	 * 	<li><b>UNION, INTERSECTION, NOT:</b> {@link RegionFunction}</li>
	 * </ul>
	 *
	 * <p><i><b>Note:</b>
	 * 	This function is using the default ADQL factory, built using
	 * 	{@link ADQLQueryFactory#ADQLQueryFactory()}.
	 * </i></p>
	 *
	 * @return	The corresponding ADQL representation.
	 *
	 * @see #toGeometry(ADQLQueryFactory)
	 */
	public GeometryFunction toGeometry() {
		return toGeometry(null);
	}

	/**
	 * Convert this region into its corresponding ADQL representation.
	 *
	 * <ul>
	 * 	<li><b>POSITION:</b> {@link PointFunction}</li>
	 * 	<li><b>CIRCLE:</b> {@link CircleFunction}</li>
	 * 	<li><b>BOX:</b> {@link BoxFunction}</li>
	 * 	<li><b>POLYGON:</b> {@link PolygonFunction}</li>
	 * 	<li><b>UNION, INTERSECTION, NOT:</b> {@link RegionFunction}</li>
	 * </ul>
	 *
	 * <p><i><b>Note:</b>
	 * 	This function build the ADQL representation just once and store it in a
	 * 	class attribute. The value of this attribute is then returned at next
	 * 	calls of this function.
	 * </i></p>
	 *
	 * @param factory	The factory of ADQL objects to use.
	 *
	 * @return	The corresponding ADQL representation.
	 */
	public GeometryFunction toGeometry(ADQLQueryFactory factory) {
		if (factory == null)
			factory = new ADQLQueryFactory();

		try {
			if (geometry != null)
				return geometry;
			else {
				StringConstant coordSysObj = factory.createStringConstant(coordSys == null ? "" : coordSys.toString());
				switch(type) {
					case POSITION:
						return (geometry = factory.createPoint(coordSysObj, toNumericObj(coordinates[0][0], factory), toNumericObj(coordinates[0][1], factory)));
					case CIRCLE:
						return (geometry = factory.createCircle(coordSysObj, toNumericObj(coordinates[0][0], factory), toNumericObj(coordinates[0][1], factory), toNumericObj(radius, factory)));
					case BOX:
						return (geometry = factory.createBox(coordSysObj, toNumericObj(coordinates[0][0], factory), toNumericObj(coordinates[0][1], factory), toNumericObj(width, factory), toNumericObj(height, factory)));
					case POLYGON:
						ArrayList<ADQLOperand> coords = new ArrayList<ADQLOperand>(coordinates.length * 2);
						for(int i = 0; i < coordinates.length; i++) {
							coords.add(toNumericObj(coordinates[i][0], factory));
							coords.add(toNumericObj(coordinates[i][1], factory));
						}
						return (geometry = factory.createPolygon(coordSysObj, coords));
					default:
						return (geometry = factory.createRegion(factory.createStringConstant(toSTCS())));
				}
			}
		} catch(Exception pe) {
			return null;
		}
	}

	/**
	 * Convert a numeric value into an ADQL representation:
	 *
	 * <ul>
	 * 	<li>If negative: NegativeOperand(NumericConstant(val))</li>
	 * 	<li>Otherwise: NumericConstant(val)</li>
	 * </ul>
	 *
	 * @param val		The value to embed in an ADQL object.
	 * @param factory	The factory to use to created ADQL objects.
	 *
	 * @return	The representing ADQL representation.
	 *
	 * @throws Exception	If an error occurs while creating the ADQL object.
	 */
	private ADQLOperand toNumericObj(final double val, final ADQLQueryFactory factory) throws Exception {
		if (val >= 0)
			return factory.createNumericConstant("" + val);
		else
			return factory.createNegativeOperand(factory.createNumericConstant("" + (val * -1)));
	}
}