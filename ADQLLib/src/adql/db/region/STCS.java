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

import static adql.db.region.CoordSys.COORD_SYS_SYNTAX;

import java.util.ArrayList;

import adql.db.region.CoordSys.Flavor;
import adql.db.region.CoordSys.Frame;
import adql.db.region.CoordSys.RefPos;
import adql.db.region.Region.RegionType;
import adql.parser.grammar.ParseException;
import adql.query.TextPosition;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

/**
 * This class helps dealing with the subset of STC-S expressions described by
 * the section "6 Use of STC-S in TAP (informative)" of the TAP Recommendation
 * 1.0 (27th March 2010). This subset is limited to the most common coordinate
 * systems and regions.
 *
 * <p><i><b>Note:</b>
 * 	No instance of this class can be created. Its usage is only limited to its
 * 	static functions and classes.
 * </i></p>
 *
 * <h3>Coordinate system</h3>
 * <p>
 * 	The function {@link #parseCoordSys(String)} is able to parse a string
 * 	containing only the STC-S expression of a coordinate system (or an empty
 * 	string or null which would be interpreted as the default coordinate system -
 * 	UNKNOWNFRAME UNKNOWNREFPOS SPHERICAL2). When successful, this parsing
 * 	returns an object representation of the coordinate system: {@link CoordSys}.
 * </p>
 * <p>
 * 	To serialize into STC-S a coordinate system, you have to create a
 * 	{@link CoordSys} instance with the desired values and to call the function
 * 	{@link CoordSys#toSTCS()}. The static function {@link #toSTCS(CoordSys)} is
 * 	just calling the {@link CoordSys#toSTCS()} on the given coordinate system.
 * </p>
 *
 * <h3>Geometrical region</h3>
 * <p>
 * 	As for the coordinate system, there is a static function to parse the STC-S
 * 	representation of a geometric region: {@link #parseRegion(String)}. Here
 * 	again, when the parsing is successful an object representation is returned:
 * 	{@link Region}.
 * </p>
 * <p>
 * 	This class lets also serializing into STC-S a region, thanks to
 * 	{@link #toSTCS(Region)}. Alternatively, the shortcut function
 * 	{@link Region#toSTCS()} can be used.
 * </p>
 * <p>
 * 	The class {@link Region} lets also dealing with the {@link ADQLFunction}
 * 	implementing a region. It is then possible to create a {@link Region}
 * 	object from a such {@link ADQLFunction} and to get the corresponding STC-S
 * 	representation. The static function {@link #toSTCS(GeometryFunction)}
 * 	is a helpful function which does these both actions in once.
 * </p>
 * <p><i><b>Note:</b>
 * 	The conversion from {@link ADQLFunction} to {@link Region} or STC-S is
 * 	possible only if the {@link ADQLFunction} contains constants as parameter.
 * 	Thus, a such function using a column, a concatenation, a math operation or
 * 	using another function can not be converted into STC-S using this class.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 2.0 (04/2021)
 * @since 1.3
 */
public final class STCS {

	/**
	 * Empty private constructor ; in order to prevent any instance creation.
	 */
	private STCS() {
	}

	/* ***************** */
	/* COORDINATE SYSTEM */
	/* ***************** */

	/**
	 * Parse the given STC-S representation of a coordinate system.
	 *
	 * @param stcs	STC-S expression of a coordinate system.
	 *            	<i>Note: a NULL or empty string will be interpreted as a
	 *            	default coordinate system.</i>
	 *
	 * @return	The object representation of the specified coordinate system.
	 *
	 * @throws ParseException	If the given expression has a wrong STC-S syntax.
	 */
	public static CoordSys parseCoordSys(final String stcs) throws ParseException {
		return (new STCSParser().parseCoordSys(stcs));
	}

	/**
	 * Convert an object representation of a coordinate system into an STC-S
	 * expression.
	 *
	 * <p><i><b>Note:</b>
	 * 	A NULL object will be interpreted as the default coordinate system and
	 * 	so an empty string will be returned. Otherwise, this function is
	 * 	equivalent to {@link CoordSys#toSTCS()} (in which default values for
	 * 	each coordinate system part is not displayed).
	 * </i></p>
	 *
	 * @param coordSys	The object representation of the coordinate system to
	 *                	convert into STC-S.
	 *
	 * @return	The corresponding STC-S expression.
	 *
	 * @see CoordSys#toSTCS()
	 * @see CoordSys#toFullSTCS()
	 */
	public static String toSTCS(final CoordSys coordSys) {
		if (coordSys == null)
			return "";
		else
			return coordSys.toSTCS();
	}

	/* ****** */
	/* REGION */
	/* ****** */

	/**
	 * Parse the given STC-S expression representing a geometric region.
	 *
	 * @param stcsRegion	STC-S expression of a region.
	 *                  	<i>Note: MUST be different from NULL.</i>
	 *
	 * @return	The object representation of the specified geometric region.
	 *
	 * @throws ParseException	If the given expression is NULL, empty string
	 *                       	or if the STC-S syntax is wrong.
	 */
	public static Region parseRegion(final String stcsRegion) throws ParseException {
		if (stcsRegion == null || stcsRegion.trim().length() == 0)
			throw new ParseException("Missing STC-S expression to parse!");
		return (new STCSParser().parseRegion(stcsRegion));
	}

	/**
	 * Convert into STC-S the given object representation of a geometric region.
	 *
	 * @param region	Region to convert into STC-S.
	 *
	 * @return	The corresponding STC-S expression.
	 *
	 * @since 2.0
	 */
	public static String toSTCS(final Region region) {
		return toSTCS(region, false);
	}

	/**
	 * Convert into STC-S the given object representation of a geometric region.
	 *
	 * @param region	Region to convert into STC-S.
	 *
	 * @return	The corresponding STC-S expression.
	 */
	public static String toSTCS(final Region region, final boolean explicitCoordSys) {
		if (region == null)
			throw new NullPointerException("Missing region to serialize into STC-S!");

		// Write the region type:
		StringBuffer buf = new StringBuffer(region.type.toString());

		// Write the coordinate system (except for NOT):
		if (region.type != RegionType.NOT) {
			String coordSysStr = (explicitCoordSys ? region.coordSys.toFullSTCS() : region.coordSys.toSTCS());
			if (coordSysStr != null && coordSysStr.length() > 0)
				buf.append(' ').append(coordSysStr);
			buf.append(' ');
		}

		// Write the other parameters (coordinates, regions, ...):
		switch(region.type) {
			case POSITION:
			case POLYGON:
				appendCoordinates(buf, region.coordinates);
				break;
			case CIRCLE:
				appendCoordinates(buf, region.coordinates);
				buf.append(' ').append(region.radius);
				break;
			case BOX:
				appendCoordinates(buf, region.coordinates);
				buf.append(' ').append(region.width).append(' ').append(region.height);
				break;
			case UNION:
			case INTERSECTION:
			case NOT:
				buf.append('(');
				appendRegions(buf, region.regions, false);
				buf.append(')');
				break;
		}

		// Return the built STC-S:
		return buf.toString();
	}

	/**
	 * Convert into STC-S the given ADQL representation of a geometric function.
	 *
	 * <p><i><b>Important note:</b>
	 * 	Only {@link PointFunction}, {@link CircleFunction}, {@link BoxFunction},
	 * 	{@link PolygonFunction} and {@link RegionFunction} are accepted here.
	 * 	Other extensions of {@link GeometryFunction} will throw an
	 * 	{@link IllegalArgumentException}.
	 * </i></p>
	 *
	 * @param region	ADQL representation of the region to convert into STC-S.
	 *
	 * @return	The corresponding STC-S expression.
	 *
	 * @throws ParseException	If the given object is NULL
	 *                       	or not of the good type.
	 */
	public static String toSTCS(final GeometryFunction region) throws ParseException {
		if (region == null)
			throw new NullPointerException("Missing region to serialize into STC-S!");
		return (new Region(region)).toSTCS();
	}

	/**
	 * Append all the given coordinates to the given buffer.
	 *
	 * @param buf		Buffer in which coordinates must be appended.
	 * @param coords	Coordinates to append.
	 */
	private static void appendCoordinates(final StringBuffer buf, final double[][] coords) {
		for(int i = 0; i < coords.length; i++) {
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
	 * @param fullCoordSys	Indicate whether the coordinate system of the
	 *                    	regions must explicitly display the default values.
	 */
	private static void appendRegions(final StringBuffer buf, final Region[] regions, final boolean fullCoordSys) {
		for(int i = 0; i < regions.length; i++) {
			if (i > 0)
				buf.append(' ');
			if (fullCoordSys)
				buf.append(regions[i].toFullSTCS());
			else
				buf.append(regions[i].toSTCS());
		}
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
	static class STCSParser {
		/** Regular expression of a numerical value. */
		private final static String numericRegExp = "(\\+|-)?(\\d+(\\.\\d*)?|\\.\\d+)([Ee](\\+|-)?\\d+)?";

		/** Position of the next characters to read in the STC-S expression to
		 * parse. */
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
			public EOEException() {
				super("Unexpected End Of Expression!");
			}
		}

		/**
		 * Build the STC-S parser.
		 */
		public STCSParser() {
		}

		/**
		 * Parse the given STC-S expression, expected as a coordinate system.
		 *
		 * @param stcs	The STC-S expression to parse.
		 *
		 * @return	The corresponding object representation of the specified
		 *        	coordinate system.
		 *
		 * @throws ParseException	If the syntax of the given STC-S expression
		 *                       	is wrong or if it is not a coordinate system.
		 */
		public CoordSys parseCoordSys(final String stcs) throws ParseException {
			init(stcs);
			CoordSys coordsys = null;
			try {
				coordsys = coordSys();
				end(COORD_SYS_SYNTAX);
				return coordsys;
			} catch(EOEException ex) {
				ex.printStackTrace();
				return new CoordSys();
			}
		}

		/**
		 * Parse the given STC-S expression, expected as a geometric region.
		 *
		 * @param stcs	The STC-S expression to parse.
		 *
		 * @return	The corresponding object representation of the specified
		 *        	geometric region.
		 *
		 * @throws ParseException	If the syntax of the given STC-S expression
		 *                       	is wrong or if it is not a geometric region.
		 */
		public Region parseRegion(final String stcs) throws ParseException {
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
		private void init(final String newStcs) {
			stcs = (newStcs == null) ? "" : newStcs;
			token = null;
			buffer = new StringBuffer();
			pos = 0;
		}

		/**
		 * Finalize the parsing.
		 * No more characters (except eventually some space characters) should
		 * remain in the STC-S expression to parse.
		 *
		 * @param expectedSyntax	Description of the good syntax expected.
		 *                      	This description is used only to write the
		 *                      	{@link ParseException} in case other
		 *                      	non-space characters are found among the
		 *                      	remaining characters.
		 *
		 * @throws ParseException	If other non-space characters remains.
		 */
		private void end(final String expectedSyntax) throws ParseException {
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
		private void skipSpaces() {
			while(pos < stcs.length() && Character.isWhitespace(stcs.charAt(pos)))
				pos++;
		}

		/**
		 * Get the next meaningful word. This word can be a numeric, any
		 * string constant or a region type.
		 *
		 * <p>
		 * 	In case the end of the expression is reached before getting any
		 * 	meaningful character, an {@link EOEException} is thrown.
		 * </p>
		 *
		 * @return	The full read word/token.
		 *
		 * @throws EOEException	If the end of the STC-S expression is reached
		 *                     	before getting any meaningful character.
		 */
		private String nextToken() throws EOEException {
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
		 * @throws ParseException	If the next token is not a numerical
		 *                       	expression.
		 */
		private double numeric() throws ParseException {
			if (nextToken().matches(numericRegExp))
				return Double.parseDouble(token);
			else
				throw new ParseException("a numeric was expected!", new TextPosition(1, pos - token.length(), 1, pos));
		}

		/**
		 * Read the next 2 tokens as a coordinate pairs (so as 2 numerical
		 * values). If not 2 numeric, a {@link ParseException} is thrown.
		 *
		 * @return	The read coordinate pairs.
		 *
		 * @throws ParseException	If the next 2 tokens are not 2 numerical
		 *                       	expressions.
		 */
		private double[] coordPair() throws ParseException {
			skipSpaces();
			int startPos = pos;
			try {
				return new double[]{ numeric(), numeric() };
			} catch(ParseException pe) {
				if (pe instanceof EOEException)
					throw pe;
				else
					throw new ParseException("a coordinates pair (2 numerics separated by one or more spaces) was expected!", new TextPosition(1, startPos, 1, pos));
			}
		}

		/**
		 * Read and parse the next tokens as a coordinate system expression.
		 * If they do not match, a {@link ParseException} is thrown.
		 *
		 * @return	The object representation of the read coordinate system.
		 *
		 * @throws ParseException	If the next tokens are not representing a
		 *                       	valid coordinate system.
		 */
		private CoordSys coordSys() throws ParseException {
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

			try {
				// Read the token:
				nextToken();
				// Try to parse it as a frame:
				if ((fr = frame()) != null) {
					// if success, go the next token:
					startPos = pos;
					oldToken = token;
					nextToken();
				}
				// Try to parse the last read token as a reference position:
				if ((rp = refpos()) != null) {
					// if success, go the next token:
					startPos = pos;
					oldToken = token;
					nextToken();
				}
				// Try to parse the last read token as a flavor:
				if ((fl = flavor()) == null) {
					// if NOT a success, go back "in time" (go back to the position before reading the token):
					pos = startPos;
					token = oldToken;
				}
			} catch(EOEException ex) {
				/* End Of Expression may happen here since all parts of a coordinate system are optional.
				 * So, there is no need to treat the error. */
			}

			// Build the object representation of the read coordinate system:
			/* Note: if nothing has been read for one or all parts of the coordinate system,
			 * the NULL value will be replaced automatically in the constructor
			 * by the default value of the corresponding part(s). */
			try {
				return new CoordSys(fr, rp, fl);
			} catch(IllegalArgumentException iae) {
				throw new ParseException(iae.getMessage(), new TextPosition(1, startPos, 1, pos));
			}
		}

		/**
		 * Parse the last read token as FRAME.
		 *
		 * @return	The corresponding enumeration item, or NULL if the last
		 *        	token is not a valid FRAME item.
		 */
		private Frame frame() {
			try {
				return Frame.valueOf(token.toUpperCase());
			} catch(IllegalArgumentException iae) {
				return null;
			}
		}

		/**
		 * Parse the last read token as REFERENCE POSITION.
		 *
		 * @return	The corresponding enumeration item,
		 *        	or NULL if the last token is not a valid REFERENCE POSITION
		 *        	item.
		 */
		private RefPos refpos() {
			try {
				return RefPos.valueOf(token.toUpperCase());
			} catch(IllegalArgumentException iae) {
				return null;
			}
		}

		/**
		 * Parse the last read token as FLAVOR.
		 *
		 * @return	The corresponding enumeration item,
		 *        	or NULL if the last token is not a valid FLAVOR item.
		 */
		private Flavor flavor() {
			try {
				return Flavor.valueOf(token.toUpperCase());
			} catch(IllegalArgumentException iae) {
				return null;
			}
		}

		/**
		 * Read and parse the next tokens as a geometric region.
		 * If they do not match, a {@link ParseException} is thrown.
		 *
		 * @return	The object representation of the read geometric region.
		 *
		 * @throws ParseException	If the next tokens are not representing a
		 *                       	valid geometric region.
		 */
		private Region region() throws ParseException {
			// Skip all spaces:
			skipSpaces();

			// Read the next token (it should be the region type):
			int startPos = pos;
			token = nextToken().toUpperCase();

			/* Identify the region type, next the expected parameters and finally build the corresponding object representation */
			// POSITION case:
			if (token.equals("POSITION")) {
				try {
					CoordSys coordSys = coordSys();
					double[] coords = coordPair();
					return new Region(coordSys, coords);
				} catch(Exception e) {
					throw buildException(e, "\"POSITION <coordSys> <coordPair>\", where coordPair=\"<numeric> <numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// CIRCLE case:
			else if (token.equals("CIRCLE")) {
				try {
					CoordSys coordSys = coordSys();
					double[] coords = coordPair();
					double radius = numeric();
					return new Region(coordSys, coords, radius);
				} catch(Exception e) {
					throw buildException(e, "\"CIRCLE <coordSys> <coordPair> <radius>\", where coordPair=\"<numeric> <numeric>\", radius=\"<numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// BOX case:
			else if (token.equals("BOX")) {
				try {
					CoordSys coordSys = coordSys();
					double[] coords = coordPair();
					double width = numeric(), height = numeric();
					return new Region(coordSys, coords, width, height);
				} catch(Exception e) {
					throw buildException(e, "\"BOX <coordSys> <coordPair> <width> <height>\", where coordPair=\"<numeric> <numeric>\", width and height=\"<numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// POLYGON case:
			else if (token.equals("POLYGON")) {
				try {
					CoordSys coordSys = coordSys();
					ArrayList<Double> coordinates = new ArrayList<Double>(6);
					double[] coords;
					for(int i = 0; i < 3; i++) {
						coords = coordPair();
						coordinates.add(coords[0]);
						coordinates.add(coords[1]);
					}
					boolean moreCoord = true;
					int posBackup;
					do {
						posBackup = pos;
						try {
							coords = coordPair();
							coordinates.add(coords[0]);
							coordinates.add(coords[1]);
						} catch(ParseException pe) {
							moreCoord = false;
							pos = posBackup;
						}
					} while(moreCoord);
					double[][] allCoords = new double[coordinates.size() / 2][2];
					for(int i = 0; i < coordinates.size() && i + 1 < coordinates.size(); i += 2)
						allCoords[i / 2] = new double[]{ coordinates.get(i), coordinates.get(i + 1) };
					return new Region(coordSys, allCoords);
				} catch(Exception e) {
					throw buildException(e, "\"POLYGON <coordSys> <coordPair> <coordPair> <coordPair> [<coordPair> ...]\", where coordPair=\"<numeric> <numeric>\" and coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// UNION & INTERSECTION cases:
			else if (token.equals("UNION") || token.equals("INTERSECTION")) {
				Region.RegionType type = (token.equals("UNION") ? Region.RegionType.UNION : Region.RegionType.INTERSECTION);
				try {
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
					while(stcs.charAt(pos) != ')') {
						regions.add(region());
						skipSpaces();
					}
					pos++;

					return new Region(type, coordSys, regions.toArray(new Region[regions.size()]));
				} catch(Exception e) {
					if (e instanceof ParseException && e.getMessage().startsWith("Incorrect syntax: \""))
						throw (ParseException)e;
					else
						throw buildException(e, "\"" + type + " <coordSys> ( <region> <region> [<region> ...] )\", where coordSys=" + COORD_SYS_SYNTAX, startPos);
				}
			}
			// NOT case:
			else if (token.equals("NOT")) {
				try {
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
				} catch(Exception e) {
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
		 * Build a {@link ParseException} based on the given one and by adding
		 * the human description of what was expected, if needed.
		 *
		 * @param ex				Root exception.
		 * @param expectedSyntax	Human description of what was expected.
		 * @param startPos			Position of the first character of the wrong
		 *                			part of expression.
		 *
		 * @return	The build exception.
		 */
		private ParseException buildException(final Exception ex, final String expectedSyntax, int startPos) {
			if (ex instanceof EOEException)
				return new ParseException("Unexpected End Of Expression! Expected syntax: " + expectedSyntax + ".", new TextPosition(1, startPos, 1, pos));
			else if (ex instanceof ParseException)
				return new ParseException("Incorrect syntax: " + ex.getMessage() + " Expected syntax: " + expectedSyntax + ".", (((ParseException)ex).getPosition() != null ? ((ParseException)ex).getPosition() : new TextPosition(1, startPos, 1, pos)));
			else
				return new ParseException(ex.getMessage(), new TextPosition(1, startPos, 1, pos));
		}
	}
}
