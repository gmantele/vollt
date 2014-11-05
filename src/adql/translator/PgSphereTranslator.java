package adql.translator;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.sql.SQLException;
import java.util.ArrayList;

import org.postgresql.util.PGobject;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.STCS.Region;
import adql.parser.ParseException;
import adql.query.TextPosition;
import adql.query.constraint.Comparison;
import adql.query.constraint.ComparisonOperator;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;

/**
 * <p>Translates all ADQL objects into the SQL adaptation of Postgres+PgSphere.
 * Actually only the geometrical functions are translated in this class.
 * The other functions are managed by {@link PostgreSQLTranslator}.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (11/2014)
 */
public class PgSphereTranslator extends PostgreSQLTranslator {

	/** Angle between two points generated while transforming a circle into a polygon.
	 * This angle is computed by default to get at the end a polygon of 32 points.
	 * @see #circleToPolygon(double[], double)
	 * @since 1.3 */
	protected static double ANGLE_CIRCLE_TO_POLYGON = 2 * Math.PI / 32;

	/**
	 * Builds a PgSphereTranslator which always translates in SQL all identifiers (schema, table and column) in a case sensitive manner ;
	 * in other words, schema, table and column names will be surrounded by double quotes in the SQL translation.
	 * 
	 * @see PostgreSQLTranslator#PostgreSQLTranslator()
	 */
	public PgSphereTranslator(){
		super();
	}

	/**
	 * Builds a PgSphereTranslator which always translates in SQL all identifiers (schema, table and column) in the specified case sensitivity ;
	 * in other words, schema, table and column names will all be surrounded or not by double quotes in the SQL translation.
	 * 
	 * @param allCaseSensitive	<i>true</i> to translate all identifiers in a case sensitive manner (surrounded by double quotes), <i>false</i> for case insensitivity. 
	 * 
	 * @see PostgreSQLTranslator#PostgreSQLTranslator(boolean)
	 */
	public PgSphereTranslator(boolean allCaseSensitive){
		super(allCaseSensitive);
	}

	/**
	 * Builds a PgSphereTranslator which will always translate in SQL identifiers with the defined case sensitivity.
	 * 
	 * @param catalog	<i>true</i> to translate catalog names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param schema	<i>true</i> to translate schema names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param table		<i>true</i> to translate table names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param column	<i>true</i> to translate column names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * 
	 * @see PostgreSQLTranslator#PostgreSQLTranslator(boolean, boolean, boolean, boolean)
	 */
	public PgSphereTranslator(boolean catalog, boolean schema, boolean table, boolean column){
		super(catalog, schema, table, column);
	}

	@Override
	public String translate(PointFunction point) throws TranslationException{
		StringBuffer str = new StringBuffer("spoint(");
		str.append("radians(").append(translate(point.getCoord1())).append("),");
		str.append("radians(").append(translate(point.getCoord2())).append("))");
		return str.toString();
	}

	@Override
	public String translate(CircleFunction circle) throws TranslationException{
		StringBuffer str = new StringBuffer("scircle(");
		str.append("spoint(radians(").append(translate(circle.getCoord1())).append("),");
		str.append("radians(").append(translate(circle.getCoord2())).append(")),");
		str.append("radians(").append(translate(circle.getRadius())).append("))");
		return str.toString();
	}

	@Override
	public String translate(BoxFunction box) throws TranslationException{
		StringBuffer str = new StringBuffer("sbox(");

		str.append("spoint(").append("radians(").append(translate(box.getCoord1())).append("-(").append(translate(box.getWidth())).append("/2.0)),");
		str.append("radians(").append(translate(box.getCoord2())).append("-(").append(translate(box.getHeight())).append("/2.0))),");

		str.append("spoint(").append("radians(").append(translate(box.getCoord1())).append("+(").append(translate(box.getWidth())).append("/2.0)),");
		str.append("radians(").append(translate(box.getCoord2())).append("+(").append(translate(box.getHeight())).append("/2.0))))");

		return str.toString();
	}

	@Override
	public String translate(PolygonFunction polygon) throws TranslationException{
		try{
			StringBuffer str = new StringBuffer("spoly('{'");

			if (polygon.getNbParameters() > 2){
				PointFunction point = new PointFunction(polygon.getCoordinateSystem(), polygon.getParameter(1), polygon.getParameter(2));
				str.append(" || ").append(translate(point));

				for(int i = 3; i < polygon.getNbParameters() && i + 1 < polygon.getNbParameters(); i += 2){
					point.setCoord1(polygon.getParameter(i));
					point.setCoord2(polygon.getParameter(i + 1));
					str.append(" || ',' || ").append(translate(point));
				}
			}

			str.append(" || '}')");

			return str.toString();
		}catch(Exception e){
			e.printStackTrace();
			throw new TranslationException(e);
		}
	}

	@Override
	public String translate(ExtractCoord extractCoord) throws TranslationException{
		StringBuffer str = new StringBuffer("degrees(");
		if (extractCoord.getName().equalsIgnoreCase("COORD1"))
			str.append("long(");
		else
			str.append("lat(");
		str.append(translate(extractCoord.getParameter(0))).append("))");
		return str.toString();
	}

	@Override
	public String translate(DistanceFunction fct) throws TranslationException{
		StringBuffer str = new StringBuffer("degrees(");
		str.append(translate(fct.getP1())).append(" <-> ").append(translate(fct.getP2())).append(")");
		return str.toString();
	}

	@Override
	public String translate(AreaFunction areaFunction) throws TranslationException{
		StringBuffer str = new StringBuffer("degrees(degrees(area(");
		str.append(translate(areaFunction.getParameter())).append(")))");
		return str.toString();
	}

	@Override
	public String translate(ContainsFunction fct) throws TranslationException{
		StringBuffer str = new StringBuffer("(");
		str.append(translate(fct.getLeftParam())).append(" @ ").append(translate(fct.getRightParam())).append(")");
		return str.toString();
	}

	@Override
	public String translate(IntersectsFunction fct) throws TranslationException{
		StringBuffer str = new StringBuffer("(");
		str.append(translate(fct.getLeftParam())).append(" && ").append(translate(fct.getRightParam())).append(")");
		return str.toString();
	}

	@Override
	public String translate(Comparison comp) throws TranslationException{
		if ((comp.getLeftOperand() instanceof ContainsFunction || comp.getLeftOperand() instanceof IntersectsFunction) && (comp.getOperator() == ComparisonOperator.EQUAL || comp.getOperator() == ComparisonOperator.NOT_EQUAL) && comp.getRightOperand().isNumeric())
			return translate(comp.getLeftOperand()) + " " + comp.getOperator().toADQL() + " '" + translate(comp.getRightOperand()) + "'";
		else if ((comp.getRightOperand() instanceof ContainsFunction || comp.getRightOperand() instanceof IntersectsFunction) && (comp.getOperator() == ComparisonOperator.EQUAL || comp.getOperator() == ComparisonOperator.NOT_EQUAL) && comp.getLeftOperand().isNumeric())
			return "'" + translate(comp.getLeftOperand()) + "' " + comp.getOperator().toADQL() + " " + translate(comp.getRightOperand());
		else
			return super.translate(comp);
	}

	@Override
	public DBType convertTypeFromDB(final int dbmsType, final String rawDbmsTypeName, String dbmsTypeName, final String[] params){
		// If no type is provided return VARCHAR:
		if (dbmsTypeName == null || dbmsTypeName.trim().length() == 0)
			return new DBType(DBDatatype.VARCHAR, DBType.NO_LENGTH);

		// Put the dbmsTypeName in lower case for the following comparisons:
		dbmsTypeName = dbmsTypeName.toLowerCase();

		if (dbmsTypeName.equals("spoint"))
			return new DBType(DBDatatype.POINT);
		else if (dbmsTypeName.equals("scircle") || dbmsTypeName.equals("sbox") || dbmsTypeName.equals("spoly"))
			return new DBType(DBDatatype.REGION);
		else
			return super.convertTypeFromDB(dbmsType, rawDbmsTypeName, dbmsTypeName, params);
	}

	@Override
	public String convertTypeToDB(final DBType type){
		if (type != null){
			if (type.type == DBDatatype.POINT)
				return "spoint";
			else if (type.type == DBDatatype.REGION)
				return "spoly";
		}
		return super.convertTypeToDB(type);
	}

	@Override
	public Region translateGeometryFromDB(final Object jdbcColValue) throws ParseException{
		// A NULL value stays NULL:
		if (jdbcColValue == null)
			return null;
		// Only a special object is expected:
		else if (!(jdbcColValue instanceof PGobject))
			throw new ParseException("Incompatible type! The column value \"" + jdbcColValue.toString() + "\" was supposed to be a geometrical object.");

		PGobject pgo = (PGobject)jdbcColValue;

		// In case one or both of the fields of the given object are NULL:
		if (pgo == null || pgo.getType() == null || pgo.getValue() == null || pgo.getValue().length() == 0)
			return null;

		// Extract the object type and its value:
		String objType = pgo.getType().toLowerCase();
		String geomStr = pgo.getValue();

		/* Only spoint, scircle, sbox and spoly are supported ; 
		 * these geometries are parsed and transformed in Region instances:*/
		if (objType.equals("spoint"))
			return (new PgSphereGeometryParser()).parsePoint(geomStr);
		else if (objType.equals("scircle"))
			return (new PgSphereGeometryParser()).parseCircle(geomStr);
		else if (objType.equals("sbox"))
			return (new PgSphereGeometryParser()).parseBox(geomStr);
		else if (objType.equals("spoly"))
			return (new PgSphereGeometryParser()).parsePolygon(geomStr);
		else
			throw new ParseException("Unsupported PgSphere type: \"" + objType + "\"! Impossible to convert the column value \"" + geomStr + "\" into a Region.");
	}

	@Override
	public Object translateGeometryToDB(final Region region) throws ParseException{
		// A NULL value stays NULL:
		if (region == null)
			return null;

		try{
			PGobject dbRegion = new PGobject();
			StringBuffer buf;

			// Build the PgSphere expression from the given geometry in function of its type:
			switch(region.type){

				case POSITION:
					dbRegion.setType("spoint");
					dbRegion.setValue("(" + region.coordinates[0][0] + "d," + region.coordinates[0][1] + "d)");
					break;

				case POLYGON:
					dbRegion.setType("spoly");
					buf = new StringBuffer("{");
					for(int i = 0; i < region.coordinates.length; i++){
						if (i > 0)
							buf.append(',');
						buf.append('(').append(region.coordinates[i][0]).append("d,").append(region.coordinates[i][1]).append("d)");
					}
					buf.append('}');
					dbRegion.setValue(buf.toString());
					break;

				case BOX:
					dbRegion.setType("spoly");
					buf = new StringBuffer("{");
					// south west
					buf.append('(').append(region.coordinates[0][0] - region.width / 2).append("d,").append(region.coordinates[0][1] - region.height / 2).append("d),");
					// north west
					buf.append('(').append(region.coordinates[0][0] - region.width / 2).append("d,").append(region.coordinates[0][1] + region.height / 2).append("d),");
					// north east
					buf.append('(').append(region.coordinates[0][0] + region.width / 2).append("d,").append(region.coordinates[0][1] + region.height / 2).append("d),");
					// south east
					buf.append('(').append(region.coordinates[0][0] + region.width / 2).append("d,").append(region.coordinates[0][1] - region.height / 2).append("d)");
					buf.append('}');
					dbRegion.setValue(buf.toString());
					break;

				case CIRCLE:
					dbRegion.setType("spoly");
					dbRegion.setValue(circleToPolygon(region.coordinates[0], region.radius));
					break;

				default:
					throw new ParseException("Unsupported geometrical region: \"" + region.type + "\"!");
			}
			return dbRegion;
		}catch(SQLException e){
			/* This error could never happen! */
			return null;
		}
	}

	/**
	 * <p>Convert the specified circle into a polygon.
	 * The generated polygon is formatted using the PgSphere syntax.</p>
	 * 
	 * <p><i>Note:
	 * 	The center coordinates and the radius are expected in degrees.
	 * </i></p>
	 * 
	 * @param center	Center of the circle ([0]=ra and [1]=dec).
	 * @param radius	Radius of the circle.
	 * 
	 * @return	The PgSphere serialization of the corresponding polygon.
	 * 
	 * @since 1.3
	 */
	protected String circleToPolygon(final double[] center, final double radius){
		double angle = 0, x, y;
		StringBuffer buf = new StringBuffer();
		while(angle < 2 * Math.PI){
			x = center[0] + radius * Math.cos(angle);
			y = center[1] + radius * Math.sin(angle);
			if (buf.length() > 0)
				buf.append(',');
			buf.append('(').append(x).append("d,").append(y).append("d)");
			angle += ANGLE_CIRCLE_TO_POLYGON;
		}
		return "{" + buf + "}";
	}

	/**
	 * <p>Let parse a geometry serialized with the PgSphere syntax.</p>
	 * 
	 * <p>
	 * 	There is one function parseXxx(String) for each supported geometry.
	 * 	These functions always return a {@link Region} object,
	 * 	which is the object representation of an STC region.
	 * </p>
	 * 
	 * <p>Only the following geometries are supported:</p>
	 * <ul>
	 * 	<li>spoint =&gt; Position</li>
	 * 	<li>scircle =&gt; Circle</li>
	 * 	<li>sbox =&gt; Box</li>
	 * 	<li>spoly =&gt; Polygon</li>
	 * </ul>
	 * 
	 * <p>
	 * 	This parser supports all the known PgSphere representations of an angle.
	 * 	However, it always returns angle (coordinates, radius, width and height) in degrees.
	 * </p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (11/2014)
	 * @since 1.3
	 */
	protected static class PgSphereGeometryParser {
		/** Position of the next characters to read in the PgSphere expression to parse. */
		private int pos;
		/** Full PgSphere expression to parse. */
		private String expr;
		/** Last read token (either a string/numeric or a separator). */
		private String token;
		/** Buffer used to read tokens. */
		private StringBuffer buffer;

		private static final char OPEN_PAR = '(';
		private static final char CLOSE_PAR = ')';
		private static final char COMMA = ',';
		private static final char LESS_THAN = '<';
		private static final char GREATER_THAN = '>';
		private static final char OPEN_BRACE = '{';
		private static final char CLOSE_BRACE = '}';
		private static final char DEGREE = 'd';
		private static final char HOUR = 'h';
		private static final char MINUTE = 'm';
		private static final char SECOND = 's';

		/**
		 * Exception sent when the end of the expression
		 * (EOE = End Of Expression) is reached.
		 * 
		 * @author Gr&eacute;gory Mantelet (ARI)
		 * @version 1.3 (11/2014)
		 * @since 1.3
		 */
		private static class EOEException extends ParseException {
			private static final long serialVersionUID = 1L;

			/** Build a simple EOEException. */
			public EOEException(){
				super("Unexpected End Of PgSphere Expression!");
			}
		}

		/**
		 * Build the PgSphere parser.
		 */
		public PgSphereGeometryParser(){}

		/**
		 * Prepare the parser in order to read the given PgSphere expression.
		 * 
		 * @param newStcs	New PgSphere expression to parse from now.
		 */
		private void init(final String newExpr){
			expr = (newExpr == null) ? "" : newExpr;
			token = null;
			buffer = new StringBuffer();
			pos = 0;
		}

		/**
		 * Finalize the parsing.
		 * No more characters (except eventually some space characters) should remain in the PgSphere expression to parse.
		 * 
		 * @throws ParseException	If other non-space characters remains. 
		 */
		private void end() throws ParseException{
			// Skip all spaces:
			skipSpaces();

			// If there is still some characters, they are not expected, and so throw an exception:
			if (expr.length() > 0 && pos < expr.length())
				throw new ParseException("Unexpected end of PgSphere region expression: \"" + expr.substring(pos) + "\" was unexpected!", new TextPosition(1, pos, 1, expr.length()));

			// Reset the buffer, token and the PgSphere expression to parse:
			buffer = null;
			expr = null;
			token = null;
		}

		/**
		 * Tool function which skips all next space characters until the next meaningful characters. 
		 */
		private void skipSpaces(){
			while(pos < expr.length() && Character.isWhitespace(expr.charAt(pos)))
				pos++;
		}

		/**
		 * <p>Get the next meaningful word. This word can be a numeric, any string constant or a separator.
		 * This function returns this token but also stores it in the class attribute {@link #token}.</p>
		 * 
		 * <p>
		 * 	In case the end of the expression is reached before getting any meaningful character,
		 * 	an {@link EOEException} is thrown.
		 * </p>
		 * 
		 * @return	The full read word/token, or NULL if the end has been reached.
		 */
		private String nextToken() throws EOEException{
			// Skip all spaces:
			skipSpaces();

			if (pos >= expr.length())
				throw new EOEException();

			// Fetch all characters until word separator (a space or a open/close parenthesis):
			buffer.append(expr.charAt(pos++));
			if (!isSyntaxSeparator(buffer.charAt(0))){
				while(pos < expr.length() && !isSyntaxSeparator(expr.charAt(pos))){
					// skip eventual white-spaces:
					if (!Character.isWhitespace(expr.charAt(pos)))
						buffer.append(expr.charAt(pos));
					pos++;
				}
			}

			// Save the read token and reset the buffer:
			token = buffer.toString();
			buffer.delete(0, token.length());

			return token;
		}

		/**
		 * <p>Tell whether the given character is a separator defined in the syntax.</p>
		 * 
		 * <p>Here, the following characters are considered as separators/specials:
		 * ',', 'd', 'h', 'm', 's', '(', ')', '&lt;', '&gt;', '{' and '}'.</p>
		 * 
		 * @param c	Character to test.
		 * 
		 * @return	<i>true</i> if the given character must be considered as a separator, <i>false</i> otherwise.
		 */
		private static boolean isSyntaxSeparator(final char c){
			return (c == COMMA || c == DEGREE || c == HOUR || c == MINUTE || c == SECOND || c == OPEN_PAR || c == CLOSE_PAR || c == LESS_THAN || c == GREATER_THAN || c == OPEN_BRACE || c == CLOSE_BRACE);
		}

		/**
		 * Get the next character and ensure it is the same as the character given in parameter.
		 * If the read character is not matching the expected one, a {@link ParseException} is thrown.
		 * 
		 * @param expected	Expected character.
		 * 
		 * @throws ParseException	If the next character is not matching the given one.
		 */
		private void nextToken(final char expected) throws ParseException{
			// Skip all spaces:
			skipSpaces();

			// Test whether the end is reached:
			if (pos >= expr.length())
				throw new EOEException();

			// Fetch the next character:
			char t = expr.charAt(pos++);
			token = new String(new char[]{t});

			/* Test the the fetched character with the expected one
			 * and throw an error if they don't match: */
			if (t != expected)
				throw new ParseException("Incorrect syntax for \"" + expr + "\"! \"" + expected + "\" was expected instead of \"" + t + "\".", new TextPosition(1, pos - 1, 1, pos));
		}

		/**
		 * Parse the given PgSphere geometry as a point.
		 * 
		 * @param pgsphereExpr	The PgSphere expression to parse as a point.
		 * 
		 * @return	A {@link Region} implementing a STC Position region.
		 * 
		 * @throws ParseException	If the PgSphere syntax of the given expression is wrong or does not correspond to a point.
		 */
		public Region parsePoint(final String pgsphereExpr) throws ParseException{
			// Init the parser:
			init(pgsphereExpr);
			// Parse the expression:
			double[] coord = parsePoint();
			// No more character should remain after that:
			end();
			// Build the STC Position region:
			return new Region(null, coord);
		}

		/**
		 * Internal spoint parsing function. It parses the PgSphere expression stored in this parser as a point. 
		 * 
		 * @return	The ra and dec coordinates (in degrees) of the parsed point.
		 * 
		 * @throws ParseException	If the PgSphere syntax of the given expression is wrong or does not correspond to a point.
		 * 
		 * @see #parseAngle()
		 * @see #parsePoint(String)
		 */
		private double[] parsePoint() throws ParseException{
			nextToken(OPEN_PAR);
			double x = parseAngle();
			nextToken(COMMA);
			double y = parseAngle();
			nextToken(CLOSE_PAR);
			return new double[]{x,y};
		}

		/**
		 * Parse the given PgSphere geometry as a circle.
		 * 
		 * @param pgsphereExpr	The PgSphere expression to parse as a circle.
		 * 
		 * @return	A {@link Region} implementing a STC Circle region.
		 * 
		 * @throws ParseException	If the PgSphere syntax of the given expression is wrong or does not correspond to a circle.
		 */
		public Region parseCircle(final String pgsphereExpr) throws ParseException{
			// Init the parser:
			init(pgsphereExpr);

			// Parse the expression:
			nextToken(LESS_THAN);
			double[] center = parsePoint();
			nextToken(COMMA);
			double radius = parseAngle();
			nextToken(GREATER_THAN);

			// No more character should remain after that:
			end();

			// Build the STC Circle region:
			return new Region(null, center, radius);
		}

		/**
		 * Parse the given PgSphere geometry as a box.
		 * 
		 * @param pgsphereExpr	The PgSphere expression to parse as a box.
		 * 
		 * @return	A {@link Region} implementing a STC Box region.
		 * 
		 * @throws ParseException	If the PgSphere syntax of the given expression is wrong or does not correspond to a box.
		 */
		public Region parseBox(final String pgsphereExpr) throws ParseException{
			// Init the parser:
			init(pgsphereExpr);

			// Parse the expression:
			nextToken(OPEN_PAR);
			double[] southwest = parsePoint();
			nextToken(COMMA);
			double[] northeast = parsePoint();
			nextToken(CLOSE_PAR);

			// No more character should remain after that:
			end();

			// Build the STC Box region:
			double width = Math.abs(northeast[0] - southwest[0]), height = Math.abs(northeast[1] - southwest[1]);
			double[] center = new double[]{northeast[0] - width / 2,northeast[1] - height / 2};
			return new Region(null, center, width, height);
		}

		/**
		 * Parse the given PgSphere geometry as a point.
		 * 
		 * @param pgsphereExpr	The PgSphere expression to parse as a point.
		 * 
		 * @return	A {@link Region} implementing a STC Position region.
		 * 
		 * @throws ParseException	If the PgSphere syntax of the given expression is wrong or does not correspond to a point.
		 */
		public Region parsePolygon(final String pgsphereExpr) throws ParseException{
			// Init the parser:
			init(pgsphereExpr);

			// Parse the expression:
			nextToken(OPEN_BRACE);
			ArrayList<double[]> points = new ArrayList<double[]>(3);
			// at least 3 points are expected:
			points.add(parsePoint());
			nextToken(COMMA);
			points.add(parsePoint());
			nextToken(COMMA);
			points.add(parsePoint());
			// but if there are more points, parse and keep them:
			while(nextToken().length() == 1 && token.charAt(0) == COMMA)
				points.add(parsePoint());
			// the expression must end with a } :
			if (token.length() != 1 || token.charAt(0) != CLOSE_BRACE)
				throw new ParseException("Incorrect syntax for \"" + expr + "\"! \"}\" was expected instead of \"" + token + "\".", new TextPosition(1, pos - token.length(), 1, pos));

			// No more character should remain after that:
			end();

			// Build the STC Polygon region:
			return new Region(null, points.toArray(new double[points.size()][2]));
		}

		/**
		 * <p>Read the next tokens as an angle expression and returns the corresponding angle in <b>degrees</b>.</p>
		 * 
		 * <p>This function supports the 4 following syntaxes:</p>
		 * <ul>
		 * 	<li><b>RAD:</b> {number}</li>
		 * 	<li><b>DEG:</b> {number}d</li>
		 * 	<li><b>DMS:</b> {number}d {number}m {number}s</li>
		 * 	<li><b>HMS:</b> {number}h {number}m {number}s</li>
		 * </ul>
		 * 
		 * @return	The corresponding angle in degrees.
		 * 
		 * @throws ParseException	If the angle syntax is wrong or not supported.
		 */
		private double parseAngle() throws ParseException{
			int oldPos = pos;
			String number = nextToken();
			try{
				double degrees = Double.parseDouble(number);
				int sign = (degrees < 0) ? -1 : 1;
				degrees = Math.abs(degrees);

				oldPos = pos;
				try{
					if (nextToken().length() == 1 && token.charAt(0) == HOUR)
						sign *= 15;
					else if (token.length() != 1 || token.charAt(0) != DEGREE){
						degrees = degrees * 180 / Math.PI;
						pos -= token.length();
						return degrees * sign;
					}

					oldPos = pos;
					number = nextToken();
					if (nextToken().length() == 1 && token.charAt(0) == MINUTE)
						degrees += Double.parseDouble(number) / 60;
					else if (token.length() == 1 && token.charAt(0) == SECOND){
						degrees += Double.parseDouble(number) / 3600;
						return degrees * sign;
					}else{
						pos = oldPos;
						return degrees * sign;
					}

					oldPos = pos;
					number = nextToken();
					if (nextToken().length() == 1 && token.charAt(0) == SECOND)
						degrees += Double.parseDouble(number) / 3600;
					else
						pos = oldPos;
				}catch(EOEException ex){
					pos = oldPos;
				}

				return degrees * sign;

			}catch(NumberFormatException nfe){
				throw new ParseException("Incorrect numeric syntax: \"" + number + "\"!", new TextPosition(1, pos - token.length(), 1, pos));
			}
		}
	}

}
