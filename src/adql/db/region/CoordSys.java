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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adql.db.region.STCS.STCSParser;
import adql.parser.grammar.ParseException;

/**
 * Object representation of an STC coordinate system.
 *
 * <p>
 * 	A coordinate system is composed of three parts: a frame ({@link #frame}),
 * 	a reference position ({@link #refpos}) and a flavor ({@link #flavor}).
 * </p>
 *
 * <p>
 * 	The default value - also corresponding to an empty string - should be:
 * 	{@link Frame#UNKNOWNFRAME} {@link RefPos#UNKNOWNREFPOS} {@link Flavor#SPHERICAL2}.
 * 	Once built, it is possible to know whether the coordinate system is the
 * 	default one or not thanks to function {@link #isDefault()}.
 * </p>
 *
 * <p>
 * 	An instance of this class can be easily serialized into STC-S using
 * 	{@link #toSTCS()}, {@link #toFullSTCS()} or {@link #toString()}.
 * 	{@link #toFullSTCS()} will display default values explicitly on the contrary
 * 	to {@link #toSTCS()} which will replace them by empty strings.
 * </p>
 *
 * <p><i><b>Important note:</b>
 * 	The flavors CARTESIAN2 and CARTESIAN3 can not be used with other frame and
 * 	reference position than UNKNOWNFRAME and UNKNOWNREFPOS. In the contrary case
 * 	an {@link IllegalArgumentException} is throw.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 2.0 (03/2021)
 * @since 1.3
 */
public class CoordSys {
	/**
	 * List of all possible frames in an STC expression.
	 *
	 * <p>
	 * 	When no value is specified, the default one is {@link #UNKNOWNFRAME}.
	 * 	The default value is also accessible through the attribute
	 * 	{@link #DEFAULT} and it is possible to test whether a frame is the
	 * 	default with the function {@link #isDefault()}.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The possible values listed in this enumeration are limited to the subset
	 *  of STC-S described by the section "6 Use of STC-S in TAP (informative)"
	 * 	of the TAP Recommendation 1.0 (27th March 2010).
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.4 (04/2017)
	 * @since 1.3
	 */
	public static enum Frame {
		ECLIPTIC, FK4, FK5, J2000, GALACTIC, ICRS, UNKNOWNFRAME;

		/** Default value for a frame: {@link #UNKNOWNFRAME}. */
		public static final Frame DEFAULT = UNKNOWNFRAME;

		/** Regular expression to test whether a string is a valid frame or not. This regular expression does not take into account
		 * the case of an empty string (which means "default frame"). */
		public static final String regexp = buildRegexp(Frame.class);

		/**
		 * Tell whether this frame is the default one.
		 *
		 * @return	<code>true</code> if this is the default frame,
		 *        	<code>false</code> otherwise.
		 */
		public final boolean isDefault() {
			return this == DEFAULT;
		}
	}

	/**
	 * List of all possible reference positions in an STC expression.
	 *
	 * <p>
	 * 	When no value is specified, the default one is {@link #UNKNOWNREFPOS}.
	 * 	The default value is also accessible through the attribute
	 * 	{@link #DEFAULT} and it is possible to test whether a reference position
	 * 	is the default with the function {@link #isDefault()}.
	 * </p>
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
	public static enum RefPos {
		BARYCENTER, GEOCENTER, HELIOCENTER, LSR, TOPOCENTER, RELOCATABLE, UNKNOWNREFPOS;

		/** Default value for a reference position: {@link #UNKNOWNREFPOS}. */
		public static final RefPos DEFAULT = UNKNOWNREFPOS;

		/** Regular expression to test whether a string is a valid reference
		 * position or not. This regular expression does not take into account
		 * the case of an empty string (which means "default reference
		 * position"). */
		public static final String regexp = buildRegexp(RefPos.class);

		/**
		 * Tell whether this reference position is the default one.
		 *
		 * @return	<code>true</code> if this is the default reference position,
		 *        	<code>false</code> otherwise.
		 */
		public final boolean isDefault() {
			return this == DEFAULT;
		}
	}

	/**
	 * List of all possible flavors in an STC expression.
	 *
	 * <p>
	 * 	When no value is specified, the default one is {@link #SPHERICAL2}.
	 * 	The default value is also accessible through the attribute
	 * 	{@link #DEFAULT} and it is possible to test whether a flavor is the
	 * 	default with the function {@link #isDefault()}.
	 * </p>
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
	public static enum Flavor {
		CARTESIAN2, CARTESIAN3, SPHERICAL2;

		/** Default value for a flavor: {@link #SPHERICAL2}. */
		public static final Flavor DEFAULT = SPHERICAL2;

		/** Regular expression to test whether a string is a valid flavor or
		 * not. This regular expression does not take into account the case of
		 * an empty string (which means "default flavor"). */
		public static final String regexp = buildRegexp(Flavor.class);

		/**
		 * Tell whether this flavor is the default one.
		 *
		 * @return	<code>true</code> if this is the default flavor,
		 *        	<code>false</code> otherwise.
		 */
		public final boolean isDefault() {
			return this == DEFAULT;
		}
	}

	/** Regular expression for a STC-S representation of a coordinate system. It
	 * takes into account the fact that each part of a coordinate system is
	 * optional and so that a full coordinate system expression can be reduced
	 * to an empty string. */
	private final static String coordSysRegExp = Frame.regexp + "?\\s*" + RefPos.regexp + "?\\s*" + Flavor.regexp + "?";
	/** Regular expression of an expression exclusively limited to a coordinate
	 * system. */
	private final static String onlyCoordSysRegExp = "^\\s*" + coordSysRegExp + "\\s*$";
	/** Regular expression of a default coordinate system: either an empty
	 * string or a string containing only default values. */
	private final static String defaultCoordSysRegExp = "^\\s*" + Frame.DEFAULT + "?\\s*" + RefPos.DEFAULT + "?\\s*" + Flavor.DEFAULT + "?\\s*$";
	/** Regular expression of a pattern describing a set of allowed coordinate
	 * systems. <i>See {@link #buildAllowedRegExp(String)} for more details.</i> */
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

	/** Pattern of an allowed coordinate system pattern. This object has been
	 * compiled with {@link #allowedCoordSysRegExp}. */
	private final static Pattern allowedCoordSysPattern = Pattern.compile(allowedCoordSysRegExp);

	/** Human description of the syntax of a full coordinate system expression. */
	public final static String COORD_SYS_SYNTAX = "\"[" + Frame.regexp + "] [" + RefPos.regexp + "] [" + Flavor.regexp + "]\" ; an empty string is also allowed and will be interpreted as the coordinate system locally used";

	/**
	 * Build the regular expression of a string defining the allowed values for
	 * one part of the whole coordinate system.
	 *
	 * @param rootRegExp	All allowed part values.
	 *
	 * @return	The corresponding regular expression.
	 */
	private static String buildAllowedRegExp(final String rootRegExp) {
		return "(" + rootRegExp + "|\\*|(\\(\\s*" + rootRegExp + "\\s*(\\|\\s*" + rootRegExp + "\\s*)*\\)))";
	}

	/** First item of a coordinate system expression: the frame. */
	public final Frame frame;

	/** Second item of a coordinate system expression: the reference position. */
	public final RefPos refpos;

	/** Third and last item of a coordinate system expression: the flavor. */
	public final Flavor flavor;

	/** Indicate whether all parts of the coordinate system are set to their
	 * default value. */
	private final boolean isDefault;

	/** STC-S representation of this coordinate system. Default items are not
	 * written (that's to say, they are replaced by an empty string). */
	private final String stcs;

	/** STC-S representation of this coordinate system. Default items are
	 * explicitly written. */
	private final String fullStcs;

	/**
	 * Build a default coordinate system (UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2).
	 */
	public CoordSys() {
		this(null, null, null);
	}

	/**
	 * Build a coordinate system with the given parts.
	 *
	 * @param fr	Frame part.
	 * @param rp	Reference position part.
	 * @param fl	Flavor part.
	 *
	 * @throws IllegalArgumentException	If a cartesian flavor is used with a
	 * frame and reference position other than UNKNOWNFRAME and UNKNOWNREFPOS.
	 */
	public CoordSys(final Frame fr, final RefPos rp, final Flavor fl) throws IllegalArgumentException {
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
	 * @param coordsys	STC-S expression representing a coordinate system.
	 *                	<i>Empty string and NULL are allowed values ; they
	 *                	correspond to a default coordinate system.</i>
	 *
	 * @throws ParseException	If the syntax of the given STC-S expression is
	 *                       	wrong or if it is not a coordinate system only.
	 */
	public CoordSys(final String coordsys) throws ParseException {
		CoordSys tmp = new STCSParser().parseCoordSys(coordsys);
		frame = tmp.frame;
		refpos = tmp.refpos;
		flavor = tmp.flavor;
		isDefault = tmp.isDefault;
		stcs = tmp.stcs;
		fullStcs = tmp.fullStcs;
	}

	/**
	 * Build a regular expression covering all possible values of the given
	 * enumeration.
	 *
	 * @param enumType	Class of an enumeration type.
	 *
	 * @return	The build regular expression or "\s*" if the given enumeration
	 *        	contains no constants/values.
	 *
	 * @throws IllegalArgumentException	If the given class is not an enumeration
	 *                                 	type.
	 */
	private static String buildRegexp(final Class<?> enumType) throws IllegalArgumentException {
		// The given class must be an enumeration type:
		if (!enumType.isEnum())
			throw new IllegalArgumentException("An enum class was expected, but a " + enumType.getName() + " has been given!");

		// Get the enumeration constants/values:
		Object[] constants = enumType.getEnumConstants();
		if (constants == null || constants.length == 0)
			return "\\s*";

		// Concatenate all constants with pipe to build a choice regular expression:
		StringBuffer buf = new StringBuffer("(");
		for(int i = 0; i < constants.length; i++) {
			buf.append(constants[i]);
			if ((i + 1) < constants.length)
				buf.append('|');
		}
		return buf.append(')').toString();
	}

	/**
	 * Build a big regular expression gathering all of the given coordinate
	 * system syntaxes.
	 *
	 * <p>
	 * 	Each item of the given list must respect a strict syntax. Each part of
	 * 	the coordinate system may be a single value, a list of values or a '*'
	 * 	(meaning all values are allowed). A list of values must have the
	 * 	following syntax: <code>({value1}|{value2}|...)</code>. An empty string
	 * 	is NOT here accepted.
	 * </p>
	 *
	 * <p><i><b>Example:</b>
	 * 	<code>(ICRS|FK4|FK5) * SPHERICAL2</code> is OK,
	 * 	but <code>(ICRS|FK4|FK5) *</code> is not valid because the flavor value
	 * 	is not defined.
	 * </i></p>
	 *
	 * <p>
	 * 	Since the default value of each part of a coordinate system should
	 * 	always be possible, this function ensure these default values are always
	 * 	possible in the returned regular expression. Thus, if some values except
	 * 	the default one are specified, the default value is automatically
	 * 	appended.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given array is NULL, all coordinate systems are allowed. But if
	 * 	the given array is empty, none except an empty string or the default
	 * 	value will be allowed.
	 * </i></p>
	 *
	 * @param allowedCoordSys	List of all coordinate systems that are allowed.
	 *
	 * @return	The corresponding regular expression.
	 *
	 * @throws ParseException	If the syntax of one of the given allowed
	 *                       	coordinate system is wrong.
	 */
	public static String buildCoordSysRegExp(final String[] allowedCoordSys) throws ParseException {
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
		for(int i = 0; i < allowedCoordSys.length; i++) {

			// NULL item => skipped!
			if (allowedCoordSys[i] == null)
				continue;
			else {
				if (nbCoordSys > 0)
					finalRegExp.append('|');
				nbCoordSys++;
			}

			// Check its syntax and identify all of its parts:
			m = allowedCoordSysPattern.matcher(allowedCoordSys[i].toUpperCase());
			if (m.matches()) {
				finalRegExp.append('(');
				for(int g = 0; g < 3; g++) {	// See the comment after the Javadoc of #allowedCoordSysRegExp for a complete list of available groups returned by the pattern.

					// SINGLE VALUE:
					if (m.group(2 + (6 * g)) != null)
						finalRegExp.append('(').append(defaultChoice(g, m.group(2 + (6 * g)))).append(m.group(2 + (6 * g))).append(')');

					// LIST OF VALUES:
					else if (m.group(3 + (6 * g)) != null)
						finalRegExp.append('(').append(defaultChoice(g, m.group(3 + (6 * g)))).append(m.group(3 + (6 * g)).replaceAll("\\s", "").substring(1));

					// JOKER (*):
					else {
						switch(g) {
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
			} else
				throw new ParseException("Wrong allowed coordinate system syntax for the " + (i + 1) + "-th item: \"" + allowedCoordSys[i] + "\"! Expected: \"frameRegExp refposRegExp flavorRegExp\" ; where each xxxRegExp = (xxx | '*' | '('xxx ('|' xxx)*')'), frame=\"" + Frame.regexp + "\", refpos=\"" + RefPos.regexp + "\" and flavor=\"" + Flavor.regexp + "\" ; an empty string is also allowed and will be interpreted as '*' (so all possible values).");
		}

		// The final regular expression must be reduced to a coordinate system and nothing else after:
		finalRegExp.append(")\\s*$");

		return (nbCoordSys > 0) ? finalRegExp.toString() : defaultCoordSysRegExp;
	}

	/**
	 * Get the default value appended by a '|' character, ONLY IF the given
	 * value does not already contain the default value.
	 *
	 * @param g		Index of the coordinate system part (0: Frame, 1: RefPos,
	 *         		2: Flavor, another value will return an empty string).
	 * @param value	Value in which the default value must prefix.
	 *
	 * @return	A prefix for the given value (the default value and a '|' if the
	 *        	default value is not already in the given value, "" otherwise).
	 */
	private static String defaultChoice(final int g, final String value) {
		switch(g) {
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

	/**
	 * Tell whether this is the default coordinate system
	 * (UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2).
	 *
	 * @return	<code>true</code> if it is the default coordinate system,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean isDefault() {
		return isDefault;
	}

	/**
	 * Get the STC-S expression of this coordinate system, in which default
	 * values are not written (they are replaced by empty strings).
	 *
	 * @return	STC-S representation of this coordinate system.
	 */
	public String toSTCS() {
		return stcs;
	}

	/**
	 * Get the STC-S expression of this coordinate system, in which default
	 * values are explicitly written.
	 *
	 * @return	STC-S representation of this coordinate system.
	 */
	public String toFullSTCS() {
		return fullStcs;
	}

	/**
	 * Convert this coordinate system into a STC-S expression.
	 *
	 * @see java.lang.Object#toString()
	 * @see #toSTCS()
	 */
	@Override
	public String toString() {
		return stcs;
	}
}