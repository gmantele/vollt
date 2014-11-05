package adql.db;

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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adql.parser.ADQLQueryFactory;
import adql.parser.ParseException;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NegativeOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

/**
 * <p>This class helps dealing with the subset of STC-S expressions described by the section "6 Use of STC-S in TAP (informative)"
 * of the TAP Recommendation 1.0 (27th March 2010). This subset is limited to the most common coordinate systems and regions.</p>
 * 
 * <p><i>Note:
 * 	No instance of this class can be created. Its usage is only limited to its static functions and classes.
 * </i></p>
 * 
 * <h3>Coordinate system</h3>
 * <p>
 * 	The function {@link #parseCoordSys(String)} is able to parse a string containing only the STC-S expression of a coordinate system
 * 	(or an empty string or null which would be interpreted as the default coordinate system - UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2).
 * 	When successful, this parsing returns an object representation of the coordinate system: {@link CoordSys}.
 * </p>
 * <p>
 * 	To serialize into STC-S a coordinate system, you have to create a {@link CoordSys} instance with the desired values
 * 	and to call the function {@link CoordSys#toSTCS()}. The static function {@link #toSTCS(CoordSys)} is just calling the
 * 	{@link CoordSys#toSTCS()} on the given coordinate system.
 * </p>
 * 
 * <h3>Geometrical region</h3>
 * <p>
 * 	As for the coordinate system, there is a static function to parse the STC-S representation of a geometrical region: {@link #parseRegion(String)}.
 * 	Here again, when the parsing is successful an object representation is returned: {@link Region}.
 * </p>
 * <p>
 * 	This class lets also serializing into STC-S a region. The procedure is the same as with a coordinate system: create a {@link Region} and then
 * 	call {@link Region#toString()}.
 * </p>
 * <p>
 * 	The class {@link Region} lets also dealing with the {@link ADQLFunction} implementing a region. It is then possible to create a {@link Region}
 * 	object from a such {@link ADQLFunction} and to get the corresponding STC-S representation. The static function {@link #toSTCS(GeometryFunction)}
 * 	is a helpful function which do these both actions in once.
 * </p>
 * <p><i>Note:
 * 	The conversion from {@link ADQLFunction} to {@link Region} or STC-S is possible only if the {@link ADQLFunction} contains constants as parameter.
 * 	Thus, a such function using a column, a concatenation, a math operation or using another function can not be converted into STC-S using this class.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.3 (10/2014)
 * @since 1.3
 */
public final class STCS {

	/**
	 * Empty private constructor ; in order to prevent any instance creation.
	 */
	private STCS(){}

	/* ***************** */
	/* COORDINATE SYSTEM */
	/* ***************** */

	/** Regular expression for a STC-S representation of a coordinate system. It takes into account the fact that each part of
	 * a coordinate system is optional and so that a full coordinate system expression can be reduced to an empty string. */
	private final static String coordSysRegExp = Frame.regexp + "?\\s*" + RefPos.regexp + "?\\s*" + Flavor.regexp + "?";
	/** Regular expression of an expression exclusively limited to a coordinate system. */
	private final static String onlyCoordSysRegExp = "^\\s*" + coordSysRegExp + "\\s*$";
	/** Regular expression of a default coordinate system: either an empty string or a string containing only default values. */
	private final static String defaultCoordSysRegExp = "^\\s*" + Frame.DEFAULT + "?\\s*" + RefPos.DEFAULT + "?\\s*" + Flavor.DEFAULT + "?\\s*$";
	/** Regular expression of a pattern describing a set of allowed coordinate systems. <i>See {@link #buildAllowedRegExp(String)} for more details.</i> */
	/* With this regular expression, we get the following matching groups:
	 *       0: All the expression
	 * 1+(6*N): The N-th part of the coordinate system (N is an unsigned integer between 0 and 2 (included) ; it is reduced to '*' if the two following groups are NULL
	 * 2+(6*N): A single value for the N-th part
	 * 3+(6*N): A list of values for the N-th part
	 * 4+(6*N): First value of the list for the N-th part
	 * 5+(6*N): All the other values (starting with a |) of the list for the N-th part
	 * 6+(6*N): Last value of the list for the N-th part.
	 */
	private final static String allowedCoordSysRegExp = "^\\s*" + buildAllowedRegExp(Frame.regexp) + "\\s+" + buildAllowedRegExp(RefPos.regexp) + "\\s+" + buildAllowedRegExp(Flavor.regexp) + "\\s*$";

	/** Pattern of an allowed coordinate system pattern. This object has been compiled with {@link #allowedCoordSysRegExp}. */
	private final static Pattern allowedCoordSysPattern = Pattern.compile(allowedCoordSysRegExp);

	/** Human description of the syntax of a full coordinate system expression. */
	private final static String COORD_SYS_SYNTAX = "\"[" + Frame.regexp + "] [" + RefPos.regexp + "] [" + Flavor.regexp + "]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used";

	/**
	 * Build the regular expression of a string defining the allowed values for one part of the whole coordinate system.
	 * 
	 * @param rootRegExp	All allowed part values.
	 * 
	 * @return	The corresponding regular expression.
	 */
	private static String buildAllowedRegExp(final String rootRegExp){
		return "(" + rootRegExp + "|\\*|(\\(\\s*" + rootRegExp + "\\s*(\\|\\s*" + rootRegExp + "\\s*)*\\)))";
	}

	/**
	 * <p>List of all possible frames in an STC expression.</p>
	 * 
	 * <p>
	 * 	When no value is specified, the default one is {@link #UNKNOWNFRAME}.
	 * 	The default value is also accessible through the attribute {@link #DEFAULT}
	 * 	and it is possible to test whether a frame is the default with the function {@link #isDefault()}.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	The possible values listed in this enumeration are limited to the subset of STC-S described by the section "6 Use of STC-S in TAP (informative)"
	 * 	of the TAP Recommendation 1.0 (27th March 2010).
	 * </i></p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static enum Frame{
		ECLIPTIC, FK4, FK5, GALACTIC, ICRS, UNKNOWNFRAME;

		/** Default value for a frame: {@link #UNKNOWNFRAME}. */
		public static final Frame DEFAULT = UNKNOWNFRAME;

		/** Regular expression to test whether a string is a valid frame or not. This regular expression does not take into account
		 * the case of an empty string (which means "default frame"). */
		public static final String regexp = buildRegexp(Frame.class);

		/**
		 * Tell whether this frame is the default one.
		 * 
		 * @return	<i>true</i> if this is the default frame, <i>false</i>
		 */
		public final boolean isDefault(){
			return this == DEFAULT;
		}
	}

	/**
	 * <p>List of all possible reference positions in an STC expression.</p>
	 * 
	 * <p>
	 * 	When no value is specified, the default one is {@link #UNKNOWNREFPOS}.
	 * 	The default value is also accessible through the attribute {@link #DEFAULT}
	 * 	and it is possible to test whether a reference position is the default with the function {@link #isDefault()}.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	The possible values listed in this enumeration are limited to the subset of STC-S described by the section "6 Use of STC-S in TAP (informative)"
	 * 	of the TAP Recommendation 1.0 (27th March 2010).
	 * </i></p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static enum RefPos{
		BARYCENTER, GEOCENTER, HELIOCENTER, LSR, TOPOCENTER, RELOCATABLE, UNKNOWNREFPOS;

		/** Default value for a reference position: {@link #UNKNOWNREFPOS}. */
		public static final RefPos DEFAULT = UNKNOWNREFPOS;

		/** Regular expression to test whether a string is a valid reference position or not. This regular expression does not take into account
		 * the case of an empty string (which means "default reference position"). */
		public static final String regexp = buildRegexp(RefPos.class);

		/**
		 * Tell whether this reference position is the default one.
		 * 
		 * @return	<i>true</i> if this is the default reference position, <i>false</i>
		 */
		public final boolean isDefault(){
			return this == DEFAULT;
		}
	}

	/**
	 * <p>List of all possible flavors in an STC expression.</p>
	 * 
	 * <p>
	 * 	When no value is specified, the default one is {@link #SPHERICAL2}.
	 * 	The default value is also accessible through the attribute {@link #DEFAULT}
	 * 	and it is possible to test whether a flavor is the default with the function {@link #isDefault()}.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	The possible values listed in this enumeration are limited to the subset of STC-S described by the section "6 Use of STC-S in TAP (informative)"
	 * 	of the TAP Recommendation 1.0 (27th March 2010).
	 * </i></p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static enum Flavor{
		CARTESIAN2, CARTESIAN3, SPHERICAL2;

		/** Default value for a flavor: {@link #SPHERICAL2}. */
		public static final Flavor DEFAULT = SPHERICAL2;

		/** Regular expression to test whether a string is a valid flavor or not. This regular expression does not take into account
		 * the case of an empty string (which means "default flavor"). */
		public static final String regexp = buildRegexp(Flavor.class);

		/**
		 * Tell whether this flavor is the default one.
		 * 
		 * @return	<i>true</i> if this is the default flavor, <i>false</i>
		 */
		public final boolean isDefault(){
			return this == DEFAULT;
		}
	}

	/**
	 * Build a regular expression covering all possible values of the given enumeration.
	 * 
	 * @param enumType	Class of an enumeration type.
	 * 
	 * @return	The build regular expression or "\s*" if the given enumeration contains no constants/values.
	 * 
	 * @throws IllegalArgumentException	If the given class is not an enumeration type.
	 */
	private static String buildRegexp(final Class<?> enumType) throws IllegalArgumentException{
		// The given class must be an enumeration type:
		if (!enumType.isEnum())
			throw new IllegalArgumentException("An enum class was expected, but a " + enumType.getName() + " has been given!");

		// Get the enumeration constants/values:
		Object[] constants = enumType.getEnumConstants();
		if (constants == null || constants.length == 0)
			return "\\s*";

		// Concatenate all constants with pipe to build a choice regular expression:
		StringBuffer buf = new StringBuffer("(");
		for(int i = 0; i < constants.length; i++){
			buf.append(constants[i]);
			if ((i + 1) < constants.length)
				buf.append('|');
		}
		return buf.append(')').toString();
	}

	/**
	 * <p>Object representation of an STC coordinate system.</p>
	 * 
	 * <p>
	 * 	A coordinate system is composed of three parts: a frame ({@link #frame}),
	 * 	a reference position ({@link #refpos}) and a flavor ({@link #flavor}).
	 * </p>
	 * 
	 * <p>
	 * 	The default value - also corresponding to an empty string - should be:
	 * 	{@link Frame#UNKNOWNFRAME} {@link RefPos#UNKNOWNREFPOS} {@link Flavor#SPHERICAL2}.
	 * 	Once built, it is possible to know whether the coordinate system is the default one
	 * 	or not thanks to function {@link #isDefault()}.
	 * </p>
	 * 
	 * <p>
	 * 	An instance of this class can be easily serialized into STC-S using {@link #toSTCS()}, {@link #toFullSTCS()}
	 * 	or {@link #toString()}. {@link #toFullSTCS()} will display default values explicitly
	 * 	on the contrary to {@link #toSTCS()} which will replace them by empty strings.
	 * </p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	The flavors CARTESIAN2 and CARTESIAN3 can not be used with other frame and reference position than
	 * 	UNKNOWNFRAME and UNKNOWNREFPOS. In the contrary case an {@link IllegalArgumentException} is throw.
	 * </i></p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static class CoordSys {
		/** First item of a coordinate system expression: the frame. */
		public final Frame frame;

		/** Second item of a coordinate system expression: the reference position. */
		public final RefPos refpos;

		/** Third and last item of a coordinate system expression: the flavor. */
		public final Flavor flavor;

		/** Indicate whether all parts of the coordinate system are set to their default value. */
		private final boolean isDefault;

		/** STC-S representation of this coordinate system. Default items are not written (that's to say, they are replaced by an empty string). */
		private final String stcs;

		/** STC-S representation of this coordinate system. Default items are explicitly written. */
		private final String fullStcs;

		/**
		 * Build a default coordinate system (UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2).
		 */
		public CoordSys(){
			this(null, null, null);
		}

		/**
		 * Build a coordinate system with the given parts.
		 * 
		 * @param fr	Frame part.
		 * @param rp	Reference position part.
		 * @param fl	Flavor part.
		 * 
		 * @throws IllegalArgumentException	If a cartesian flavor is used with a frame and reference position other than UNKNOWNFRAME and UNKNOWNREFPOS.
		 */
		public CoordSys(final Frame fr, final RefPos rp, final Flavor fl) throws IllegalArgumentException{
			frame = (fr == null) ? Frame.DEFAULT : fr;
			refpos = (rp == null) ? RefPos.DEFAULT : rp;
			flavor = (fl == null) ? Flavor.DEFAULT : fl;

			if (flavor != Flavor.SPHERICAL2 && (frame != Frame.UNKNOWNFRAME || refpos != RefPos.UNKNOWNREFPOS))
				throw new IllegalArgumentException("a coordinate system expressed with a cartesian flavor MUST have an UNKNOWNFRAME and UNKNOWNREFPOS!");

			isDefault = frame.isDefault() && refpos.isDefault() && flavor.isDefault();

			stcs = ((!frame.isDefault() ? frame + " " : "") + (!refpos.isDefault() ? refpos + " " : "") + (!flavor.isDefault() ? flavor : "")).trim();
			fullStcs = frame + " " + refpos + " " + flavor;
		}

		/**
		 * Build a coordinate system by parsing the given STC-S expression.
		 * 
		 * @param coordsys	STC-S expression representing a coordinate system. <i>Empty string and NULL are allowed values ; they correspond to a default coordinate system.</i>
		 * 
		 * @throws ParseException	If the syntax of the given STC-S expression is wrong or if it is not a coordinate system only.
		 */
		public CoordSys(final String coordsys) throws ParseException{
			CoordSys tmp = new STCSParser().parseCoordSys(coordsys);
			frame = tmp.frame;
			refpos = tmp.refpos;
			flavor = tmp.flavor;
			isDefault = tmp.isDefault;
			stcs = tmp.stcs;
			fullStcs = tmp.fullStcs;
		}

		/**
		 * Tell whether this is the default coordinate system (UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2).
		 * 
		 * @return	<i>true</i> if it is the default coordinate system, <i>false</i> otherwise.
		 */
		public final boolean isDefault(){
			return isDefault;
		}

		/**
		 * Get the STC-S expression of this coordinate system,
		 * in which default values are not written (they are replaced by empty strings).
		 * 
		 * @return	STC-S representation of this coordinate system.
		 */
		public String toSTCS(){
			return stcs;
		}

		/**
		 * Get the STC-S expression of this coordinate system,
		 * in which default values are explicitly written.
		 * 
		 * @return	STC-S representation of this coordinate system.
		 */
		public String toFullSTCS(){
			return fullStcs;
		}

		/**
		 * Convert this coordinate system into a STC-S expression.
		 * 
		 * @see java.lang.Object#toString()
		 * @see #toSTCS()
		 */
		@Override
		public String toString(){
			return stcs;
		}
	}

	/**
	 * Parse the given STC-S representation of a coordinate system.
	 * 
	 * @param stcs	STC-S expression of a coordinate system. <i>Note: a NULL or empty string will be interpreted as a default coordinate system.</i>
	 * 
	 * @return	The object representation of the specified coordinate system.
	 * 
	 * @throws ParseException	If the given expression has a wrong STC-S syntax.
	 */
	public static CoordSys parseCoordSys(final String stcs) throws ParseException{
		return (new STCSParser().parseCoordSys(stcs));
	}

	/**
	 * <p>Convert an object representation of a coordinate system into an STC-S expression.</p>
	 * 
	 * <p><i>Note:
	 * 	A NULL object will be interpreted as the default coordinate system and so an empty string will be returned.
	 * 	Otherwise, this function is equivalent to {@link CoordSys#toSTCS()} (in which default values for each
	 * 	coordinate system part is not displayed).
	 * </i></p>
	 * 
	 * @param coordSys	The object representation of the coordinate system to convert into STC-S.
	 * 
	 * @return	The corresponding STC-S expression.
	 * 
	 * @see CoordSys#toSTCS()
	 * @see CoordSys#toFullSTCS()
	 */
	public static String toSTCS(final CoordSys coordSys){
		if (coordSys == null)
			return "";
		else
			return coordSys.toSTCS();
	}

	/**
	 * <p>Build a big regular expression gathering all of the given coordinate system syntaxes.</p>
	 * 
	 * <p>
	 * 	Each item of the given list must respect a strict syntax. Each part of the coordinate system
	 * 	may be a single value, a list of values or a '*' (meaning all values are allowed).
	 * 	A list of values must have the following syntax: <code>({value1}|{value2}|...)</code>.
	 * 	An empty string is NOT here accepted.
	 * </p>
	 * 
	 * <p><i>Example:
	 * 	<code>(ICRS|FK4|FK5) * SPHERICAL2</code> is OK,
	 * 	but <code>(ICRS|FK4|FK5) *</code> is not valid because the flavor value is not defined.
	 * </i></p>
	 * 
	 * <p>
	 * 	Since the default value of each part of a coordinate system should always be possible,
	 * 	this function ensure these default values are always possible in the returned regular expression.
	 * 	Thus, if some values except the default one are specified, the default value is automatically appended.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	If the given array is NULL, all coordinate systems are allowed.
	 * 	But if the given array is empty, none except an empty string or the default value will be allowed.
	 * </i></p>
	 * 
	 * @param allowedCoordSys	List of all coordinate systems that are allowed.
	 * 
	 * @return	The corresponding regular expression.
	 * 
	 * @throws ParseException	If the syntax of one of the given allowed coordinate system is wrong.
	 */
	public static String buildCoordSysRegExp(final String[] allowedCoordSys) throws ParseException{
		// NULL array => all coordinate systems are allowed:
		if (allowedCoordSys == null)
			return onlyCoordSysRegExp;
		// Empty array => no coordinate system (except the default one) is allowed:
		else if (allowedCoordSys.length == 0)
			return defaultCoordSysRegExp;

		// The final regular expression must be reduced to a coordinate system and nothing else before:
		StringBuffer finalRegExp = new StringBuffer("^\\s*(");

		// For each allowed coordinate system:
		Matcher m;
		int nbCoordSys = 0;
		for(int i = 0; i < allowedCoordSys.length; i++){

			// NULL item => skipped!
			if (allowedCoordSys[i] == null)
				continue;
			else{
				if (nbCoordSys > 0)
					finalRegExp.append('|');
				nbCoordSys++;
			}

			// Check its syntax and identify all of its parts:
			m = allowedCoordSysPattern.matcher(allowedCoordSys[i].toUpperCase());
			if (m.matches()){
				finalRegExp.append('(');
				for(int g = 0; g < 3; g++){	// See the comment after the Javadoc of #allowedCoordSysRegExp for a complete list of available groups returned by the pattern.

					// SINGLE VALUE:
					if (m.group(2 + (6 * g)) != null)
						finalRegExp.append('(').append(defaultChoice(g, m.group(2 + (6 * g)))).append(m.group(2 + (6 * g))).append(')');

					// LIST OF VALUES:
					else if (m.group(3 + (6 * g)) != null)
						finalRegExp.append('(').append(defaultChoice(g, m.group(3 + (6 * g)))).append(m.group(3 + (6 * g)).replaceAll("\\s", "").substring(1));

					// JOKER (*):
					else{
						switch(g){
							case 0:
								finalRegExp.append(Frame.regexp);
								break;
							case 1:
								finalRegExp.append(RefPos.regexp);
								break;
							case 2:
								finalRegExp.append(Flavor.regexp);
								break;
						}
						finalRegExp.append('?');
					}
					finalRegExp.append("\\s*");
				}
				finalRegExp.append(')');
			}else
				throw new ParseException("Wrong allowed coordinate system syntax for the " + (i + 1) + "-th item: \"" + allowedCoordSys[i] + "\"! Expected: \"frameRegExp refposRegExp flavorRegExp\" ; where each xxxRegExp = (xxx | '*' | '('xxx ('|' xxx)*')'), frame=\"" + Frame.regexp + "\", refpos=\"" + RefPos.regexp + "\" and flavor=\"" + Flavor.regexp + "\" ; an empty string is also allowed and will be interpreted as '*' (so all possible values).");
		}

		// The final regular expression must be reduced to a coordinate system and nothing else after:
		finalRegExp.append(")\\s*");

		return (nbCoordSys > 0) ? finalRegExp.append(")$").toString() : defaultCoordSysRegExp;
	}

	/**
	 * Get the default value appended by a '|' character, ONLY IF the given value does not already contain the default value.
	 * 
	 * @param g		Index of the coordinate system part (0: Frame, 1: RefPos, 2: Flavor, another value will return an empty string).
	 * @param value	Value in which the default value must prefix.
	 * 
	 * @return	A prefix for the given value (the default value and a '|' if the default value is not already in the given value, "" otherwise).
	 */
	private static String defaultChoice(final int g, final String value){
		switch(g){
			case 0:
				return value.contains(Frame.DEFAULT.toString()) ? "" : Frame.DEFAULT + "|";
			case 1:
				return value.contains(RefPos.DEFAULT.toString()) ? "" : RefPos.DEFAULT + "|";
			case 2:
				return value.contains(Flavor.DEFAULT.toString()) ? "" : Flavor.DEFAULT + "|";
			default:
				return "";
		}
	}

	/* ****** */
	/* REGION */
	/* ****** */

	/**
	 * <p>List all possible region types allowed in an STC-S expression.</p>
	 * 
	 * <p><i>Note:
	 * 	The possible values listed in this enumeration are limited to the subset of STC-S described by the section "6 Use of STC-S in TAP (informative)"
	 * 	of the TAP Recommendation 1.0 (27th March 2010).
	 * </i></p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static enum RegionType{
		POSITION, CIRCLE, BOX, POLYGON, UNION, INTERSECTION, NOT;
	}

	/**
	 * <p>Object representation of an STC region.</p>
	 * 
	 * <p>
	 * 	This class contains a field for each possible parameter of a region. Depending of the region type
	 * 	some are not used. In such case, these unused fields are set to NULL.
	 * </p>
	 * 
	 * <p>
	 * 	An instance of this class can be easily serialized into STC-S using {@link #toSTCS()}, {@link #toFullSTCS()}
	 * 	or {@link #toString()}. {@link #toFullSTCS()} will display default value explicit
	 * 	on the contrary to {@link #toSTCS()} which will replace them by empty strings.
	 * </p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	public static class Region {
		/** Type of the region. */
		public final RegionType type;

		/** Coordinate system used by this region.
		 * <i>Note: only the NOT region does not declare a coordinate system ; so only for this region this field is NULL.</i> */
		public final CoordSys coordSys;

		/** List of coordinates' pairs. The second dimension of this array represents a pair of coordinates ; it is then an array of two elements.
		 * <i>Note: this field is used by POINT, BOX, CIRCLE and POLYGON.</i> */
		public final double[][] coordinates;

		/** Width of the BOX region. */
		public final double width;

		/** Height of the BOX region. */
		public final double height;

		/** Radius of the CIRCLE region. */
		public final double radius;

		/** List of regions unified (UNION), intersected (INTERSECTION) or avoided (NOT). */
		public final Region[] regions;

		/** STC-S representation of this region, in which default values of the coordinate system (if any) are not written (they are replaced by empty strings).
		 * <i>Note: This attribute is NULL until the first call of the function {@link #toSTCS()} where it is built.</i> */
		private String stcs = null;

		/** STC-S representation of this region, in which default values of the coordinate system (if any) are explicitly written.
		 * <i>Note: This attribute is NULL until the first call of the function {@link #toFullSTCS()} where it is built.</i> */
		private String fullStcs = null;

		/** The ADQL function object representing this region.
		 * <i>Note: this attribute is NULL until the first call of the function {@link #toGeometry()} or {@link #toGeometry(ADQLQueryFactory)}.</i> */
		private GeometryFunction geometry = null;

		/**
		 * <p>Constructor for a POINT/POSITION region.</p>
		 * 
		 * <p><i><b>Important note:</b>
		 * 	The array of coordinates is used like that. No copy is done.
		 * </i></p>
		 * 
		 * @param coordSys		Coordinate system. <i>note: It MAY BE null ; if so, the default coordinate system will be chosen</li>
		 * @param coordinates	A pair of coordinates ; coordinates[0] and coordinates[1].
		 */
		public Region(final CoordSys coordSys, final double[] coordinates){
			this(coordSys, new double[][]{coordinates});
		}

		/**
		 * <p>Constructor for a POINT/POSITION or a POLYGON region.</p>
		 * 
		 * <p>Whether it is a polygon or a point depends on the number of given coordinates:</p>
		 * <ul>
		 * 	<li>1 item => POINT/POSITION</li>
		 * 	<li>more items => POLYGON</li>
		 * </ul>
		 * 
		 * <p><i><b>Important note:</b>
		 * 	The array of coordinates is used like that. No copy is done.
		 * </i></p>
		 * 
		 * @param coordSys		Coordinate system. <i>note: It MAY BE null ; if so, the default coordinate system will be chosen</li>
		 * @param coordinates	List of coordinates' pairs ; coordinates[n] = 1 pair = 2 items (coordinates[n][0] and coordinates[n][1]) ; if 1 pair, it is a POINT/POSITION, but if more, it is a POLYGON.
		 */
		public Region(final CoordSys coordSys, final double[][] coordinates){
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
		 * <p>Constructor for a CIRCLE region.</p>
		 * 
		 * <p><i><b>Important note:</b>
		 * 	The array of coordinates is used like that. No copy is done.
		 * </i></p>
		 * 
		 * @param coordSys		Coordinate system. <i>note: It MAY BE null ; if so, the default coordinate system will be chosen</li>
		 * @param coordinates	A pair of coordinates ; coordinates[0] and coordinates[1].
		 * @param radius		The circle radius.
		 */
		public Region(final CoordSys coordSys, final double[] coordinates, final double radius){
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
			this.coordinates = new double[][]{coordinates};

			// Set the radius:
			this.radius = radius;

			// Set the other fields as not used:
			width = Double.NaN;
			height = Double.NaN;
			regions = null;
		}

		/**
		 * <p>Constructor for a BOX region.</p>
		 * 
		 * <p><i><b>Important note:</b>
		 * 	The array of coordinates is used like that. No copy is done.
		 * </i></p>
		 * 
		 * @param coordSys		Coordinate system. <i>note: It MAY BE null ; if so, the default coordinate system will be chosen</li>
		 * @param coordinates	A pair of coordinates ; coordinates[0] and coordinates[1].
		 * @param width			Width of the box.
		 * @param height		Height of the box.
		 */
		public Region(final CoordSys coordSys, final double[] coordinates, final double width, final double height){
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
			this.coordinates = new double[][]{coordinates};

			// Set the size of the box:
			this.width = width;
			this.height = height;

			// Set the other fields as not used:
			radius = Double.NaN;
			regions = null;
		}

		/**
		 * <p>Constructor for a UNION or INTERSECTION region.</p>
		 * 
		 * <p><i><b>Important note:</b>
		 * 	The array of regions is used like that. No copy is done.
		 * </i></p>
		 * 
		 * @param unionOrIntersection	Type of the region to create. <i>Note: It can be ONLY a UNION or INTERSECTION. Another value will throw an IllegalArgumentException).</i>
		 * @param coordSys				Coordinate system. <i>note: It MAY BE null ; if so, the default coordinate system will be chosen</li>
		 * @param regions				Regions to unite or to intersect. <i>Note: At least two regions must be provided.</i>
		 */
		public Region(final RegionType unionOrIntersection, final CoordSys coordSys, final Region[] regions){
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
		public Region(final Region region){
			// Check the region parameter:
			if (region == null)
				throw new NullPointerException("Missing region to NOT select!");

			// Set the region type:
			type = RegionType.NOT;

			// Set the regions:
			this.regions = new Region[]{region};

			// Set the other fields as not used:
			coordSys = null;
			coordinates = null;
			radius = Double.NaN;
			width = Double.NaN;
			height = Double.NaN;
		}

		/**
		 * <p>Build a Region from the given ADQL representation.</p>
		 * 
		 * <p><i>Note:
		 * 	Only {@link PointFunction}, {@link CircleFunction}, {@link BoxFunction}, {@link PolygonFunction} and {@link RegionFunction}
		 * 	are accepted here. Other extensions of {@link GeometryFunction} will throw an {@link IllegalArgumentException}.
		 * </i></p>
		 * 
		 * @param geometry	The ADQL representation of the region to create here.
		 * 
		 * @throws IllegalArgumentException	If the given geometry is neither of {@link PointFunction}, {@link BoxFunction}, {@link PolygonFunction} and {@link RegionFunction}.
		 * @throws ParseException			If the declared coordinate system, the coordinates or the STC-S definition has a wrong syntax.
		 */
		public Region(final GeometryFunction geometry) throws IllegalArgumentException, ParseException{
			if (geometry == null)
				throw new NullPointerException("Missing geometry to convert into STCS.Region!");

			if (geometry instanceof PointFunction){
				type = RegionType.POSITION;
				coordSys = STCS.parseCoordSys(extractString(geometry.getCoordinateSystem()));
				coordinates = new double[][]{{extractNumeric(((PointFunction)geometry).getCoord1()),extractNumeric(((PointFunction)geometry).getCoord2())}};
				width = Double.NaN;
				height = Double.NaN;
				radius = Double.NaN;
				regions = null;
			}else if (geometry instanceof CircleFunction){
				type = RegionType.CIRCLE;
				coordSys = STCS.parseCoordSys(extractString(geometry.getCoordinateSystem()));
				coordinates = new double[][]{{extractNumeric(((CircleFunction)geometry).getCoord1()),extractNumeric(((CircleFunction)geometry).getCoord2())}};
				radius = extractNumeric(((CircleFunction)geometry).getRadius());
				width = Double.NaN;
				height = Double.NaN;
				regions = null;
			}else if (geometry instanceof BoxFunction){
				type = RegionType.BOX;
				coordSys = STCS.parseCoordSys(extractString(geometry.getCoordinateSystem()));
				coordinates = new double[][]{{extractNumeric(((BoxFunction)geometry).getCoord1()),extractNumeric(((BoxFunction)geometry).getCoord2())}};
				width = extractNumeric(((BoxFunction)geometry).getWidth());
				height = extractNumeric(((BoxFunction)geometry).getHeight());
				radius = Double.NaN;
				regions = null;
			}else if (geometry instanceof PolygonFunction){
				PolygonFunction poly = (PolygonFunction)geometry;
				type = RegionType.POLYGON;
				coordSys = STCS.parseCoordSys(extractString(poly.getCoordinateSystem()));
				coordinates = new double[(poly.getNbParameters() - 1) / 2][2];
				for(int i = 0; i < coordinates.length; i++)
					coordinates[i] = new double[]{extractNumeric(poly.getParameter(1 + i * 2)),extractNumeric(poly.getParameter(2 + i * 2))};
				width = Double.NaN;
				height = Double.NaN;
				radius = Double.NaN;
				regions = null;
			}else if (geometry instanceof RegionFunction){
				Region r = STCS.parseRegion(extractString(((RegionFunction)geometry).getParameter(0)));
				type = r.type;
				coordSys = r.coordSys;
				coordinates = r.coordinates;
				width = r.width;
				height = r.height;
				radius = r.radius;
				regions = r.regions;
			}else
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
		 * @throws ParseException	If the given operand is not an instance of {@link StringConstant}.
		 */
		private static String extractString(final ADQLOperand op) throws ParseException{
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
		 * @throws ParseException	If the given operand is not an instance of {@link NumericConstant} or a {@link NegativeOperand}.
		 */
		private static double extractNumeric(final ADQLOperand op) throws ParseException{
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
		 * <p>Get the STC-S representation of this region (in which default values
		 * of the coordinate system are not written ; they are replaced by empty strings).</p>
		 * 
		 * <p><i>Note:
		 * 	This function build the STC-S just once and store it in a class attribute.
		 * 	The value of this attribute is then returned at next calls of this function.
		 * </i></p>
		 * 
		 * @return	Its STC-S representation.
		 */
		public String toSTCS(){
			if (stcs != null)
				return stcs;
			else{
				// Write the region type:
				StringBuffer buf = new StringBuffer(type.toString());

				// Write the coordinate system (except for NOT):
				if (type != RegionType.NOT){
					String coordSysStr = coordSys.toSTCS();
					if (coordSysStr != null && coordSysStr.length() > 0)
						buf.append(' ').append(coordSysStr);
					buf.append(' ');
				}

				// Write the other parameters (coordinates, regions, ...):
				switch(type){
					case POSITION:
					case POLYGON:
						appendCoordinates(buf, coordinates);
						break;
					case CIRCLE:
						appendCoordinates(buf, coordinates);
						buf.append(' ').append(radius);
						break;
					case BOX:
						appendCoordinates(buf, coordinates);
						buf.append(' ').append(width).append(' ').append(height);
						break;
					case UNION:
					case INTERSECTION:
					case NOT:
						buf.append('(');
						appendRegions(buf, regions, false);
						buf.append(')');
						break;
				}

				// Return the built STC-S:
				return (stcs = buf.toString());
			}
		}

		/**
		 * <p>Get the STC-S representation of this region (in which default values
		 * of the coordinate system are explicitly written).</p>
		 * 
		 * <p><i>Note:
		 * 	This function build the STC-S just once and store it in a class attribute.
		 * 	The value of this attribute is then returned at next calls of this function.
		 * </i></p>
		 * 
		 * @return	Its STC-S representation.
		 */
		public String toFullSTCS(){
			if (fullStcs != null)
				return fullStcs;
			else{
				// Write the region type:
				StringBuffer buf = new StringBuffer(type.toString());

				// Write the coordinate system (except for NOT):
				if (type != RegionType.NOT){
					String coordSysStr = coordSys.toFullSTCS();
					if (coordSysStr != null && coordSysStr.length() > 0)
						buf.append(' ').append(coordSysStr);
					buf.append(' ');
				}

				// Write the other parameters (coordinates, regions, ...):
				switch(type){
					case POSITION:
					case POLYGON:
						appendCoordinates(buf, coordinates);
						break;
					case CIRCLE:
						appendCoordinates(buf, coordinates);
						buf.append(' ').append(radius);
						break;
					case BOX:
						appendCoordinates(buf, coordinates);
						buf.append(' ').append(width).append(' ').append(height);
						break;
					case UNION:
					case INTERSECTION:
					case NOT:
						buf.append('(');
						appendRegions(buf, regions, true);
						buf.append(')');
						break;
				}

				// Return the built STC-S:
				return (fullStcs = buf.toString());
			}
		}

		/**
		 * Append all the given coordinates to the given buffer.
		 * 
		 * @param buf		Buffer in which coordinates must be appended.
		 * @param coords	Coordinates to append.
		 */
		private static void appendCoordinates(final StringBuffer buf, final double[][] coords){
			for(int i = 0; i < coords.length; i++){
				if (i > 0)
					buf.append(' ');
				buf.append(coords[i][0]).append(' ').append(coords[i][1]);
			}
		}

		/**
		 * Append all the given regions in the given buffer.
		 * 
		 * @param buf			Buffer in which regions must be appended.
		 * @param regions		Regions to append.
		 * @param fullCoordSys	Indicate whether the coordinate system of the regions must explicitly display the default values.
		 */
		private static void appendRegions(final StringBuffer buf, final Region[] regions, final boolean fullCoordSys){
			for(int i = 0; i < regions.length; i++){
				if (i > 0)
					buf.append(' ');
				if (fullCoordSys)
					buf.append(regions[i].toFullSTCS());
				else
					buf.append(regions[i].toSTCS());
			}
		}

		@Override
		public String toString(){
			return toSTCS();
		}

		/**
		 * <p>Convert this region into its corresponding ADQL representation.</p>
		 * 
		 * <ul>
		 * 	<li><b>POSITION:</b> {@link PointFunction}</li>
		 * 	<li><b>CIRCLE:</b> {@link CircleFunction}</li>
		 * 	<li><b>BOX:</b> {@link BoxFunction}</li>
		 * 	<li><b>POLYGON:</b> {@link PolygonFunction}</li>
		 * 	<li><b>UNION, INTERSECTION, NOT:</b> {@link RegionFunction}</li>
		 * </ul>
		 * 
		 * <p><i>Note:
		 * 	This function is using the default ADQL factory, built using {@link ADQLQueryFactory#ADQLQueryFactory()}.
		 * </i></p>
		 * 
		 * @return	The corresponding ADQL representation.
		 * 
		 * @see #toGeometry(ADQLQueryFactory)
		 */
		public GeometryFunction toGeometry(){
			return toGeometry(null);
		}

		/**
		 * <p>Convert this region into its corresponding ADQL representation.</p>
		 * 
		 * <ul>
		 * 	<li><b>POSITION:</b> {@link PointFunction}</li>
		 * 	<li><b>CIRCLE:</b> {@link CircleFunction}</li>
		 * 	<li><b>BOX:</b> {@link BoxFunction}</li>
		 * 	<li><b>POLYGON:</b> {@link PolygonFunction}</li>
		 * 	<li><b>UNION, INTERSECTION, NOT:</b> {@link RegionFunction}</li>
		 * </ul>
		 * 
		 * <p><i>Note:
		 * 	This function build the ADQL representation just once and store it in a class attribute.
		 * 	The value of this attribute is then returned at next calls of this function.
		 * </i></p>
		 * 
		 * @param factory	The factory of ADQL objects to use.
		 * 
		 * @return	The corresponding ADQL representation.
		 */
		public GeometryFunction toGeometry(ADQLQueryFactory factory){
			if (factory == null)
				factory = new ADQLQueryFactory();

			try{
				if (geometry != null)
					return geometry;
				else{
					StringConstant coordSysObj = factory.createStringConstant(coordSys == null ? "" : coordSys.toString());
					switch(type){
						case POSITION:
							return (geometry = factory.createPoint(coordSysObj, toNumericObj(coordinates[0][0], factory), toNumericObj(coordinates[0][1], factory)));
						case CIRCLE:
							return (geometry = factory.createCircle(coordSysObj, toNumericObj(coordinates[0][0], factory), toNumericObj(coordinates[0][1], factory), toNumericObj(radius, factory)));
						case BOX:
							return (geometry = factory.createBox(coordSysObj, toNumericObj(coordinates[0][0], factory), toNumericObj(coordinates[0][1], factory), toNumericObj(width, factory), toNumericObj(height, factory)));
						case POLYGON:
							ArrayList<ADQLOperand> coords = new ArrayList<ADQLOperand>(coordinates.length * 2);
							for(int i = 0; i < coordinates.length; i++){
								coords.add(toNumericObj(coordinates[i][0], factory));
								coords.add(toNumericObj(coordinates[i][1], factory));
							}
							return (geometry = factory.createPolygon(coordSysObj, coords));
						default:
							return (geometry = factory.createRegion(factory.createStringConstant(toString())));
					}
				}
			}catch(Exception pe){
				return null;
			}
		}

		/**
		 * <p>Convert a numeric value into an ADQL representation:</p>
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
		private ADQLOperand toNumericObj(final double val, final ADQLQueryFactory factory) throws Exception{
			if (val >= 0)
				return factory.createNumericConstant("" + val);
			else
				return factory.createNegativeOperand(factory.createNumericConstant("" + (val * -1)));
		}
	}

	/**
	 * Parse the given STC-S expression representing a geometrical region.
	 * 
	 * @param stcsRegion	STC-S expression of a region. <i>Note: MUST be different from NULL.</i>
	 * 
	 * @return	The object representation of the specified geometrical region.
	 * 
	 * @throws ParseException	If the given expression is NULL, empty string or if the STC-S syntax is wrong.
	 */
	public static Region parseRegion(final String stcsRegion) throws ParseException{
		if (stcsRegion == null || stcsRegion.trim().length() == 0)
			throw new ParseException("Missing STC-S expression to parse!");
		return (new STCSParser().parseRegion(stcsRegion));
	}

	/**
	 * Convert into STC-S the given object representation of a geometrical region.
	 * 
	 * @param region	Region to convert into STC-S.
	 * 
	 * @return	The corresponding STC-S expression.
	 */
	public static String toSTCS(final Region region){
		if (region == null)
			throw new NullPointerException("Missing region to serialize into STC-S!");
		return region.toSTCS();
	}

	/**
	 * <p>Convert into STC-S the given ADQL representation of a geometrical function.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	Only {@link PointFunction}, {@link CircleFunction}, {@link BoxFunction}, {@link PolygonFunction}
	 * 	and {@link RegionFunction} are accepted here. Other extensions of {@link GeometryFunction} will
	 * 	throw an {@link IllegalArgumentException}.
	 * </i></p>
	 * 
	 * @param region	ADQL representation of the region to convert into STC-S.
	 * 
	 * @return	The corresponding STC-S expression.
	 * 
	 * @throws ParseException	If the given object is NULL or not of the good type.
	 * 
	 * @see {@link Region#Region(GeometryFunction)}
	 */
	public static String toSTCS(final GeometryFunction region) throws ParseException{
		if (region == null)
			throw new NullPointerException("Missing region to serialize into STC-S!");
		return (new Region(region)).toSTCS();
	}

	/* *************************** */
	/* PARSER OF STC-S EXPRESSIONS */
	/* *************************** */

	/**
	 * Let parse any STC-S expression.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (11/2014)
	 * @since 1.3
	 */
	private static class STCSParser {
		/** Regular expression of a numerical value. */
		private final static String numericRegExp = "(\\+|-)?(\\d+(\\.\\d*)?|\\.\\d+)([Ee](\\+|-)?\\d+)?";

		/** Position of the next characters to read in the STC-S expression to parse. */
		private int pos;
		/** Full STC-S expression to parse. */
		private String stcs;
		/** Last read token (can be a numeric, a string, a region type, ...). */
		private String token;
		/** Buffer used to read tokens. */
		private StringBuffer buffer;

		/**
		 * Exception sent when the end of the expression
		 * (EOE = End Of Expression) is reached.
		 * 
		 * @author Gr&eacute;gory Mantelet (ARI)
		 * @version 1.3 (10/2014)
		 * @since 1.3
		 */
		private static class EOEException extends ParseException {
			private static final long serialVersionUID = 1L;

			/** Build a simple EOEException. */
			public EOEException(){
				super("Unexpected End Of Expression!");
			}
		}

		/**
		 * Build the STC-S parser.
		 */
		public STCSParser(){}

		/**
		 * Parse the given STC-S expression, expected as a coordinate system.
		 * 
		 * @param stcs	The STC-S expression to parse.
		 * 
		 * @return	The corresponding object representation of the specified coordinate system.
		 * 
		 * @throws ParseException	If the syntax of the given STC-S expression is wrong or if it is not a coordinate system.
		 */
		public CoordSys parseCoordSys(final String stcs) throws ParseException{
			init(stcs);
			CoordSys coordsys = null;
			try{
				coordsys = coordSys();
				end(COORD_SYS_SYNTAX);
				return coordsys;
			}catch(EOEException ex){
				ex.printStackTrace();
				return new CoordSys();
			}
		}

		/**
		 * Parse the given STC-S expression, expected as a geometrical region.
		 * 
		 * @param stcs	The STC-S expression to parse.
		 * 
		 * @return	The corresponding object representation of the specified geometrical region.
		 * 
		 * @throws ParseException	If the syntax of the given STC-S expression is wrong or if it is not a geometrical region.
		 */
		public Region parseRegion(final String stcs) throws ParseException{
			init(stcs);
			Region region = region();
			end("\"POSITION <coordsys> <coordPair>\", \"CIRCLE <coordSys> <coordPair> <numeric>\", \"BOX <coordSys> <coordPair> <coordPair>\", \"POLYGON <coordSys> <coordPair> <coordPair> <coordPair> [<coordPair> ...]\", \"UNION <coordSys> ( <region> <region> [<region> ...] )\", \"INTERSECTION [<coordSys>] ( <region> <region> [<region> ...] )\" or \"NOT ( <region> )\"");
			return region;
		}

		/**
		 * Prepare the parser in order to read the given STC-S expression.
		 * 
		 * @param newStcs	New STC-S expression to parse from now.
		 */
		private void init(final String newStcs){
			stcs = (newStcs == null) ? "" : newStcs;
			token = null;
			buffer = new StringBuffer();
			pos = 0;
		}

		/**
		 * Finalize the parsing.
		 * No more characters (except eventually some space characters) should remain in the STC-S expression to parse.
		 * 
		 * @param expectedSyntax	Description of the good syntax expected. This description is used only to write the
		 *                      	{@link ParseException} in case other non-space characters are found among the remaining characters.
		 * 
		 * @throws ParseException	If other non-space characters remains. 
		 */
		private void end(final String expectedSyntax) throws ParseException{
			// Skip all spaces:
			skipSpaces();

			// If there is still some characters, they are not expected, and so throw an exception:
			if (stcs.length() > 0 && pos < stcs.length())
				throw new ParseException("Incorrect syntax: \"" + stcs.substring(pos) + "\" was unexpected! Expected syntax: " + expectedSyntax + ".", new TextPosition(1, pos, 1, stcs.length()));

			// Reset the buffer, token and the STC-S expression to parse:
			buffer = null;
			stcs = null;
			token = null;
		}

		/**
		 * Tool function which skip all next space characters until the next meaningful characters. 
		 */
		private void skipSpaces(){
			while(pos < stcs.length() && Character.isWhitespace(stcs.charAt(pos)))
				pos++;
		}

		/**
		 * <p>Get the next meaningful word. This word can be a numeric, any string constant or a region type.</p>
		 * 
		 * <p>
		 * 	In case the end of the expression is reached before getting any meaningful character, an {@link EOEException} is thrown.
		 * </p>
		 * 
		 * @return	The full read word/token.
		 * 
		 * @throws EOEException	If the end of the STC-S expression is reached before getting any meaningful character.
		 */
		private String nextToken() throws EOEException{
			// Skip all spaces:
			skipSpaces();

			// Fetch all characters until word separator (a space or a open/close parenthesis):
			while(pos < stcs.length() && !Character.isWhitespace(stcs.charAt(pos)) && stcs.charAt(pos) != '(' && stcs.charAt(pos) != ')')
				buffer.append(stcs.charAt(pos++));

			// If no character has been fetched while at least one was expected, throw an exception:
			if (buffer.length() == 0)
				throw new EOEException();

			// Save the read token and reset the buffer:
			token = buffer.toString();
			buffer.delete(0, token.length());

			return token;
		}

		/**
		 * Read the next token as a numeric.
		 * If not a numeric, a {@link ParseException} is thrown.
		 * 
		 * @return	The read numerical value.
		 * 
		 * @throws ParseException	If the next token is not a numerical expression.
		 */
		private double numeric() throws ParseException{
			if (nextToken().matches(numericRegExp))
				return Double.parseDouble(token);
			else
				throw new ParseException("a numeric was expected!", new TextPosition(1, pos - token.length(), 1, pos));	// TODO Check the begin and end!
		}

		/**
		 * Read the next 2 tokens as a coordinate pairs (so as 2 numerical values).
		 * If not 2 numeric, a {@link ParseException} is thrown.
		 * 
		 * @return	The read coordinate pairs.
		 * 
		 * @throws ParseException	If the next 2 tokens are not 2 numerical expressions.
		 */
		private double[] coordPair() throws ParseException{
			skipSpaces();
			int startPos = pos;
			try{
				return new double[]{numeric(),numeric()};
			}catch(ParseException pe){
				if (pe instanceof EOEException)
					throw pe;
				else
					throw new ParseException("a coordinates pair (2 numerics separated by one or more spaces) was expected!", new TextPosition(1, startPos, 1, pos));	// TODO Check the begin and end!
			}
		}

		/**
		 * Read and parse the next tokens as a coordinate system expression.
		 * If they do not match, a {@link ParseException} is thrown.
		 * 
		 * @return	The object representation of the read coordinate system.
		 * 
		 * @throws ParseException	If the next tokens are not representing a valid coordinate system.
		 */
		private CoordSys coordSys() throws ParseException{
			// Skip all spaces:
			skipSpaces();

			// Backup the current position:
			/* (because every parts of a coordinate system are optional ;
			 * like this, it will be possible to go back in the expression
			 * to parse if optional parts are not written) */
			String oldToken = token;
			int startPos = pos;

			Frame fr = null;
			RefPos rp = null;
			Flavor fl = null;

			try{
				// Read the token:
				nextToken();
				// Try to parse it as a frame:
				if ((fr = frame()) != null){
					// if success, go the next token:
					startPos = pos;
					oldToken = token;
					nextToken();
				}
				// Try to parse the last read token as a reference position:
				if ((rp = refpos()) != null){
					// if success, go the next token:
					startPos = pos;
					oldToken = token;
					nextToken();
				}
				// Try to parse the last read token as a flavor:
				if ((fl = flavor()) == null){
					// if NOT a success, go back "in time" (go back to the position before reading the token):
					pos = startPos;
					token = oldToken;
				}
			}catch(EOEException ex){
				/* End Of Expression may happen here since all parts of a coordinate system are optional.
				 * So, there is no need to treat the error. */
			}

			// Build the object representation of the read coordinate system:
			/* Note: if nothing has been read for one or all parts of the coordinate system,
			 * the NULL value will be replaced automatically in the constructor
			 * by the default value of the corresponding part(s). */
			try{
				return new CoordSys(fr, rp, fl);
			}catch(IllegalArgumentException iae){
				throw new ParseException(iae.getMessage(), new TextPosition(1, startPos, 1, pos));
			}
		}

		/**
		 * Parse the last read token as FRAME.
		 * 
		 * @return	The corresponding enumeration item, or NULL if the last token is not a valid FRAME item.
		 */
		private Frame frame(){
			try{
				return Frame.valueOf(token.toUpperCase());
			}catch(IllegalArgumentException iae){
				return null;
			}
		}

		/**
		 * Parse the last read token as REFERENCE POSITION.
		 * 
		 * @return	The corresponding enumeration item, or NULL if the last token is not a valid REFERENCE POSITION item.
		 */
		private RefPos refpos(){
			try{
				return RefPos.valueOf(token.toUpperCase());
			}catch(IllegalArgumentException iae){
				return null;
			}
		}

		/**
		 * Parse the last read token as FLAVOR.
		 * 
		 * @return	The corresponding enumeration item, or NULL if the last token is not a valid FLAVOR item.
		 */
		private Flavor flavor(){
			try{
				return Flavor.valueOf(token.toUpperCase());
			}catch(IllegalArgumentException iae){
				return null;
			}
		}

		/**
		 * Read and parse the next tokens as a geometrical region.
		 * If they do not match, a {@link ParseException} is thrown.
		 * 
		 * @return	The object representation of the read geometrical region.
		 * 
		 * @throws ParseException	If the next tokens are not representing a valid geometrical region.
		 */
		private Region region() throws ParseException{
			// Skip all spaces:
			skipSpaces();

			// Read the next token (it should be the region type):
			int startPos = pos;
			token = nextToken().toUpperCase();

			/* Identify the region type, next the expected parameters and finally build the corresponding object representation */
			// POSITION case:
			if (token.equals("POSITION")){
				try{
					CoordSys coordSys = coordSys();
					double[] coords = coordPair();
					return new Region(coordSys, coords);
				}catch(Exception e){
					throw buildException(e, "\"POSITION <coordSys> <coordPair>\", where coordPair=\"<numeric> <numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// CIRCLE case:
			else if (token.equals("CIRCLE")){
				try{
					CoordSys coordSys = coordSys();
					double[] coords = coordPair();
					double radius = numeric();
					return new Region(coordSys, coords, radius);
				}catch(Exception e){
					throw buildException(e, "\"CIRCLE <coordSys> <coordPair> <radius>\", where coordPair=\"<numeric> <numeric>\", radius=\"<numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// BOX case:
			else if (token.equals("BOX")){
				try{
					CoordSys coordSys = coordSys();
					double[] coords = coordPair();
					double width = numeric(), height = numeric();
					return new Region(coordSys, coords, width, height);
				}catch(Exception e){
					throw buildException(e, "\"BOX <coordSys> <coordPair> <width> <height>\", where coordPair=\"<numeric> <numeric>\", width and height=\"<numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// POLYGON case:
			else if (token.equals("POLYGON")){
				try{
					CoordSys coordSys = coordSys();
					ArrayList<Double> coordinates = new ArrayList<Double>(6);
					double[] coords;
					for(int i = 0; i < 3; i++){
						coords = coordPair();
						coordinates.add(coords[0]);
						coordinates.add(coords[1]);
					}
					boolean moreCoord = true;
					int posBackup;
					do{
						posBackup = pos;
						try{
							coords = coordPair();
							coordinates.add(coords[0]);
							coordinates.add(coords[1]);
						}catch(ParseException pe){
							moreCoord = false;
							pos = posBackup;
						}
					}while(moreCoord);
					double[][] allCoords = new double[coordinates.size() / 2][2];
					for(int i = 0; i < coordinates.size() && i + 1 < coordinates.size(); i += 2)
						allCoords[i / 2] = new double[]{coordinates.get(i),coordinates.get(i + 1)};
					return new Region(coordSys, allCoords);
				}catch(Exception e){
					throw buildException(e, "\"POLYGON <coordSys> <coordPair> <coordPair> <coordPair> [<coordPair> ...]\", where coordPair=\"<numeric> <numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// UNION & INTERSECTION cases:
			else if (token.equals("UNION") || token.equals("INTERSECTION")){
				RegionType type = (token.equals("UNION") ? RegionType.UNION : RegionType.INTERSECTION);
				try{
					CoordSys coordSys = coordSys();
					ArrayList<Region> regions = new ArrayList<Region>(2);

					skipSpaces();
					if (stcs.charAt(pos) != '(')
						throw buildException(new ParseException("a opening parenthesis - ( - was expected!", new TextPosition(1, pos, 1, pos + 1)), "\"" + type + " <coordSys> ( <region> <region> [<region> ...] )\", where coordSys=" + COORD_SYS_SYNTAX, startPos);
					else
						pos++;

					// parse and add the FIRST region:
					regions.add(region());

					// parse and add the SECOND region:
					regions.add(region());

					skipSpaces();
					while(stcs.charAt(pos) != ')'){
						regions.add(region());
						skipSpaces();
					}
					pos++;

					return new Region(type, coordSys, regions.toArray(new Region[regions.size()]));
				}catch(Exception e){
					if (e instanceof ParseException && e.getMessage().startsWith("Incorrect syntax: \""))
						throw (ParseException)e;
					else
						throw buildException(e, "\"" + type + " <coordSys> ( <region> <region> [<region> ...] )\", where coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// NOT case:
			else if (token.equals("NOT")){
				try{
					skipSpaces();
					if (stcs.charAt(pos) != '(')
						throw buildException(new ParseException("an opening parenthesis - ( - was expected!", new TextPosition(1, pos, 1, pos + 1)), "\"NOT ( <region> )\"", startPos);
					else
						pos++;
					Region region = region();
					skipSpaces();
					if (stcs.charAt(pos) != ')')
						throw buildException(new ParseException("a closing parenthesis - ) - was expected!", new TextPosition(1, pos, 1, pos + 1)), "\"NOT ( <region> )\"", startPos);
					else
						pos++;
					return new Region(region);
				}catch(Exception e){
					if (e instanceof ParseException && e.getMessage().startsWith("Incorrect syntax: "))
						throw (ParseException)e;
					else
						throw buildException(e, "\"NOT ( <region> )\"", startPos);
				}
			}
			// Otherwise, the region type is not known and so a ParseException is thrown:
			else
				throw new ParseException("Unknown STC region type: \"" + token + "\"!", new TextPosition(1, startPos, 1, pos));
		}

		/**
		 * Build a {@link ParseException} based on the given one and by adding the human description of what was expected, if needed.
		 * 
		 * @param ex				Root exception.
		 * @param expectedSyntax	Human description of what was expected.
		 * @param startPos			Position of the first character of the wrong part of expression.
		 * 
		 * @return	The build exception.
		 */
		private ParseException buildException(final Exception ex, final String expectedSyntax, int startPos){
			if (ex instanceof EOEException)
				return new ParseException("Unexpected End Of Expression! Expected syntax: " + expectedSyntax + ".", new TextPosition(1, startPos, 1, pos));
			else if (ex instanceof ParseException)
				return new ParseException("Incorrect syntax: " + ex.getMessage() + " Expected syntax: " + expectedSyntax + ".", (((ParseException)ex).getPosition() != null ? ((ParseException)ex).getPosition() : new TextPosition(1, startPos, 1, pos)));
			else
				return new ParseException(ex.getMessage(), new TextPosition(1, startPos, 1, pos));
		}
	}
}
