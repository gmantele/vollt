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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.STCS.Region;
import adql.parser.ParseException;
import adql.query.IdentifierField;
import adql.query.operand.StringConstant;
import adql.query.operand.function.MathFunction;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.ExtractCoordSys;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

/**
 * <p>Translates all ADQL objects into an SQL interrogation query designed for PostgreSQL.</p>
 * 
 * <p><i><b>Important</b>:
 * 	The geometrical functions are translated exactly as in ADQL.
 * 	You will probably need to extend this translator to correctly manage the geometrical functions.
 * 	An extension is already available for PgSphere: {@link PgSphereTranslator}.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (11/2014)
 * 
 * @see PgSphereTranslator
 */
public class PostgreSQLTranslator extends JDBCTranslator {

	/** <p>Indicate the case sensitivity to apply to each SQL identifier (only SCHEMA, TABLE and COLUMN).</p>
	 * 
	 * <p><i>Note:
	 * 	In this implementation, this field is set by the constructor and never modified elsewhere.
	 * 	It would be better to never modify it after the construction in order to keep a certain consistency.
	 * </i></p>
	 */
	protected byte caseSensitivity = 0x00;

	/**
	 * Builds a PostgreSQLTranslator which always translates in SQL all identifiers (schema, table and column) in a case sensitive manner ;
	 * in other words, schema, table and column names will be surrounded by double quotes in the SQL translation.
	 */
	public PostgreSQLTranslator(){
		caseSensitivity = 0x0F;
	}

	/**
	 * Builds a PostgreSQLTranslator which always translates in SQL all identifiers (schema, table and column) in the specified case sensitivity ;
	 * in other words, schema, table and column names will all be surrounded or not by double quotes in the SQL translation.
	 * 
	 * @param allCaseSensitive	<i>true</i> to translate all identifiers in a case sensitive manner (surrounded by double quotes), <i>false</i> for case insensitivity. 
	 */
	public PostgreSQLTranslator(final boolean allCaseSensitive){
		caseSensitivity = allCaseSensitive ? (byte)0x0F : (byte)0x00;
	}

	/**
	 * Builds a PostgreSQLTranslator which will always translate in SQL identifiers with the defined case sensitivity.
	 * 
	 * @param catalog	<i>true</i> to translate catalog names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param schema	<i>true</i> to translate schema names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param table		<i>true</i> to translate table names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param column	<i>true</i> to translate column names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 */
	public PostgreSQLTranslator(final boolean catalog, final boolean schema, final boolean table, final boolean column){
		caseSensitivity = IdentifierField.CATALOG.setCaseSensitive(caseSensitivity, catalog);
		caseSensitivity = IdentifierField.SCHEMA.setCaseSensitive(caseSensitivity, schema);
		caseSensitivity = IdentifierField.TABLE.setCaseSensitive(caseSensitivity, table);
		caseSensitivity = IdentifierField.COLUMN.setCaseSensitive(caseSensitivity, column);
	}

	@Override
	public boolean isCaseSensitive(final IdentifierField field){
		return field == null ? false : field.isCaseSensitive(caseSensitivity);
	}

	@Override
	public String translate(StringConstant strConst) throws TranslationException{
		// Deal with the special escaping syntax of Postgres:
		/* A string containing characters to escape must be prefixed by an E.
		 * Without this prefix, Potsgres does not escape the concerned characters and
		 * consider backslashes as normal characters.
		 * For instance: E'foo\tfoo2'. */
		if (strConst.getValue() != null && strConst.getValue().contains("\\"))
			return "E'" + strConst.getValue() + "'";
		else
			return super.translate(strConst);
	}

	@Override
	public String translate(MathFunction fct) throws TranslationException{
		switch(fct.getType()){
			case LOG:
				return "ln(" + ((fct.getNbParameters() >= 1) ? translate(fct.getParameter(0)) : "") + ")";
			case LOG10:
				return "log(10, " + ((fct.getNbParameters() >= 1) ? translate(fct.getParameter(0)) : "") + ")";
			case RAND:
				return "random()";
			case TRUNCATE:
				return "trunc(" + ((fct.getNbParameters() >= 2) ? (translate(fct.getParameter(0)) + ", " + translate(fct.getParameter(1))) : "") + ")";
			default:
				return getDefaultADQLFunction(fct);
		}
	}

	@Override
	public String translate(ExtractCoord extractCoord) throws TranslationException{
		return getDefaultADQLFunction(extractCoord);
	}

	@Override
	public String translate(ExtractCoordSys extractCoordSys) throws TranslationException{
		return getDefaultADQLFunction(extractCoordSys);
	}

	@Override
	public String translate(AreaFunction areaFunction) throws TranslationException{
		return getDefaultADQLFunction(areaFunction);
	}

	@Override
	public String translate(CentroidFunction centroidFunction) throws TranslationException{
		return getDefaultADQLFunction(centroidFunction);
	}

	@Override
	public String translate(DistanceFunction fct) throws TranslationException{
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(ContainsFunction fct) throws TranslationException{
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(IntersectsFunction fct) throws TranslationException{
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(BoxFunction box) throws TranslationException{
		return getDefaultADQLFunction(box);
	}

	@Override
	public String translate(CircleFunction circle) throws TranslationException{
		return getDefaultADQLFunction(circle);
	}

	@Override
	public String translate(PointFunction point) throws TranslationException{
		return getDefaultADQLFunction(point);
	}

	@Override
	public String translate(PolygonFunction polygon) throws TranslationException{
		return getDefaultADQLFunction(polygon);
	}

	@Override
	public String translate(RegionFunction region) throws TranslationException{
		return getDefaultADQLFunction(region);
	}

	@Override
	public DBType convertTypeFromDB(final int dbmsType, final String rawDbmsTypeName, String dbmsTypeName, final String[] params){
		// If no type is provided return VARCHAR:
		if (dbmsTypeName == null || dbmsTypeName.trim().length() == 0)
			return new DBType(DBDatatype.VARCHAR, DBType.NO_LENGTH);

		// Put the dbmsTypeName in lower case for the following comparisons:
		dbmsTypeName = dbmsTypeName.toLowerCase();

		// Extract the length parameter (always the first one):
		int lengthParam = DBType.NO_LENGTH;
		if (params != null && params.length > 0){
			try{
				lengthParam = Integer.parseInt(params[0]);
			}catch(NumberFormatException nfe){}
		}

		// SMALLINT
		if (dbmsTypeName.equals("smallint") || dbmsTypeName.equals("int2") || dbmsTypeName.equals("smallserial") || dbmsTypeName.equals("serial2") || dbmsTypeName.equals("boolean") || dbmsTypeName.equals("bool"))
			return new DBType(DBDatatype.SMALLINT);
		// INTEGER
		else if (dbmsTypeName.equals("integer") || dbmsTypeName.equals("int") || dbmsTypeName.equals("int4") || dbmsTypeName.equals("serial") || dbmsTypeName.equals("serial4"))
			return new DBType(DBDatatype.INTEGER);
		// BIGINT
		else if (dbmsTypeName.equals("bigint") || dbmsTypeName.equals("int8") || dbmsTypeName.equals("bigserial") || dbmsTypeName.equals("bigserial8"))
			return new DBType(DBDatatype.BIGINT);
		// REAL
		else if (dbmsTypeName.equals("real") || dbmsTypeName.equals("float4"))
			return new DBType(DBDatatype.REAL);
		// DOUBLE
		else if (dbmsTypeName.equals("double precision") || dbmsTypeName.equals("float8"))
			return new DBType(DBDatatype.DOUBLE);
		// BINARY
		else if (dbmsTypeName.equals("bit"))
			return new DBType(DBDatatype.BINARY, lengthParam);
		// VARBINARY
		else if (dbmsTypeName.equals("bit varying") || dbmsTypeName.equals("varbit"))
			return new DBType(DBDatatype.VARBINARY, lengthParam);
		// CHAR
		else if (dbmsTypeName.equals("char") || dbmsTypeName.equals("character"))
			return new DBType(DBDatatype.CHAR, lengthParam);
		// VARCHAR
		else if (dbmsTypeName.equals("varchar") || dbmsTypeName.equals("character varying"))
			return new DBType(DBDatatype.VARCHAR, lengthParam);
		// BLOB
		else if (dbmsTypeName.equals("bytea"))
			return new DBType(DBDatatype.BLOB);
		// CLOB
		else if (dbmsTypeName.equals("text"))
			return new DBType(DBDatatype.CLOB);
		// TIMESTAMP
		else if (dbmsTypeName.equals("timestamp") || dbmsTypeName.equals("timestamptz") || dbmsTypeName.equals("time") || dbmsTypeName.equals("timetz") || dbmsTypeName.equals("date"))
			return new DBType(DBDatatype.TIMESTAMP);
		// Default:
		else
			return new DBType(DBDatatype.VARCHAR, DBType.NO_LENGTH);
	}

	@Override
	public String convertTypeToDB(final DBType type){
		if (type == null)
			return "VARCHAR";

		switch(type.type){

			case SMALLINT:
			case INTEGER:
			case REAL:
			case BIGINT:
			case CHAR:
			case VARCHAR:
			case TIMESTAMP:
				return type.type.toString();

			case DOUBLE:
				return "DOUBLE PRECISION";

			case BINARY:
			case VARBINARY:
				return "bytea";

			case BLOB:
				return "bytea";

			case CLOB:
				return "TEXT";

			case POINT:
			case REGION:
			default:
				return "VARCHAR";
		}
	}

	@Override
	public Region translateGeometryFromDB(final Object jdbcColValue) throws ParseException{
		throw new ParseException("Unsupported geometrical value! The value \"" + jdbcColValue + "\" can not be parsed as a region.");
	}

	@Override
	public Object translateGeometryToDB(final Region region) throws ParseException{
		throw new ParseException("Geometries can not be uploaded in the database in this implementation!");
	}

}
