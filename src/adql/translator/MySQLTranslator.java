package adql.translator;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.STCS.Region;
import adql.parser.ParseException;
import adql.query.IdentifierField;
import adql.query.operand.ADQLOperand;
import adql.query.operand.Concatenation;
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
 * Copyright 2017-2019 - Astronomisches Rechen Institut (ARI),
 *                       UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * Translates all ADQL objects into an SQL interrogation query designed for
 * MySQL.
 *
 * <p><i><b>Important</b>:
 * 	The geometrical functions are translated exactly as in ADQL.
 * 	You will probably need to extend this translator to correctly manage the
 * 	geometrical functions.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 1.5 (03/2019)
 * @since 1.4
 */
public class MySQLTranslator extends JDBCTranslator {

	/** MySQL requires a length for variable-length types such as CHAR, VARCHAR,
	 * BINARY and VARBINARY. This static attributes is the default value set
	 * by this translator if no length is specified. */
	public static int DEFAULT_VARIABLE_LENGTH = 200;

	/** Indicate the case sensitivity to apply to each SQL identifier
	 * (only SCHEMA, TABLE and COLUMN).
	 * <p><i>Note:
	 * 	In this implementation, this field is set by the constructor and never
	 * 	modified elsewhere. It would be better to never modify it after the
	 * construction in order to keep a certain consistency.
	 * </i></p>
	 */
	protected byte caseSensitivity = 0x00;

	/**
	 * Build a MySQLTranslator which always translates in SQL all identifiers
	 * (schema, table and column) in a case sensitive manner ; in other words,
	 * schema, table and column names will be surrounded by back-quotes in the
	 * SQL translation.
	 */
	public MySQLTranslator(){
		caseSensitivity = 0x0F;
	}

	/**
	 * Build a MySQLTranslator which always translates in SQL all identifiers
	 * (schema, table and column) in the specified case sensitivity ; in other
	 * words, schema, table and column names will all be surrounded or not by
	 * back-quotes in the SQL translation.
	 *
	 * @param allCaseSensitive	<i>true</i> to translate all identifiers in a
	 *                        	case sensitive manner
	 *                        	(surrounded by back-quotes),
	 *                        	<i>false</i> for case insensitivity.
	 */
	public MySQLTranslator(final boolean allCaseSensitive){
		caseSensitivity = allCaseSensitive ? (byte)0x0F : (byte)0x00;
	}

	/**
	 * Build a MySQLTranslator which will always translate in SQL identifiers
	 * with the defined case sensitivity.
	 *
	 * @param catalog	<i>true</i> to translate catalog names with back-quotes
	 *               	(case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param schema	<i>true</i> to translate schema names with back-quotes
	 *              	(case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param table		<i>true</i> to translate table names with back-quotes
	 *             		(case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param column	<i>true</i> to translate column names with back-quotes
	 *              	(case sensitive in the DBMS), <i>false</i> otherwise.
	 */
	public MySQLTranslator(final boolean catalog, final boolean schema, final boolean table, final boolean column){
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
	public StringBuffer appendIdentifier(final StringBuffer str, final String id, final boolean caseSensitive){
		/* Note: In MySQL the identifier quoting character is a back-quote. */
		if (caseSensitive && !id.matches("\"[^\"]*\""))
			return str.append('`').append(id).append('`');
		else
			return str.append(id);
	}

	/* ********************************************************************** */
	/* *                                                                    * */
	/* * GENERAL TRANSLATIONS                                               * */
	/* *                                                                    * */
	/* ********************************************************************** */

	@Override
	public String translate(Concatenation concat) throws TranslationException{
		StringBuffer translated = new StringBuffer();

		for(ADQLOperand op : concat){
			if (translated.length() == 0)
				translated.append("CONCAT(");
			else
				translated.append(", ");
			translated.append(translate(op));
		}
		translated.append(")");

		return translated.toString();
	}

	/* ********************************************************************** */
	/* *                                                                    * */
	/* * TYPE MANAGEMENT                                                    * */
	/* *                                                                    * */
	/* ********************************************************************** */

	@Override
	public DBType convertTypeFromDB(final int dbmsType, final String rawDbmsTypeName, String dbmsTypeName, final String[] params){
		// If no type is provided return VARCHAR:
		if (dbmsTypeName == null || dbmsTypeName.trim().length() == 0)
			return null;

		// Put the dbmsTypeName in lower case for the following comparisons:
		dbmsTypeName = dbmsTypeName.toLowerCase();

		// Extract the length parameter (always the first one):
		int lengthParam = DBType.NO_LENGTH;
		if (params != null && params.length > 0){
			try{
				lengthParam = Integer.parseInt(params[0]);
			}catch(NumberFormatException nfe){
			}
		}

		// SMALLINT
		if (dbmsTypeName.equals("smallint") || dbmsTypeName.equals("tinyint") || dbmsTypeName.equals("bool") || dbmsTypeName.equals("boolean"))
			return new DBType(DBDatatype.SMALLINT);
		// INTEGER
		else if (dbmsTypeName.equals("integer") || dbmsTypeName.equals("int") || dbmsTypeName.equals("mediumint"))
			return new DBType(DBDatatype.INTEGER);
		// BIGINT
		else if (dbmsTypeName.equals("bigint"))
			return new DBType(DBDatatype.BIGINT);
		// REAL
		else if (dbmsTypeName.equals("float") || dbmsTypeName.equals("real"))
			return new DBType(DBDatatype.REAL);
		// DOUBLE
		else if (dbmsTypeName.equals("double") || dbmsTypeName.equals("double precision") || dbmsTypeName.equals("dec") || dbmsTypeName.equals("decimal") || dbmsTypeName.equals("numeric") || dbmsTypeName.equals("fixed"))
			return new DBType(DBDatatype.DOUBLE);
		// BINARY
		else if (dbmsTypeName.equals("bit") || dbmsTypeName.equals("binary") || dbmsTypeName.equals("char byte"))
			return new DBType(DBDatatype.BINARY, lengthParam);
		// VARBINARY
		else if (dbmsTypeName.equals("varbinary"))
			return new DBType(DBDatatype.VARBINARY, lengthParam);
		// CHAR
		else if (dbmsTypeName.equals("char") || dbmsTypeName.equals("character") || dbmsTypeName.equals("nchar") || dbmsTypeName.equals("national char"))
			return new DBType(DBDatatype.CHAR, lengthParam);
		// VARCHAR
		else if (dbmsTypeName.equals("varchar") || dbmsTypeName.equals("character varying") || dbmsTypeName.equals("nvarchar") || dbmsTypeName.equals("national varchar"))
			return new DBType(DBDatatype.VARCHAR, lengthParam);
		// BLOB
		else if (dbmsTypeName.equals("blob") || dbmsTypeName.equals("tinyblob") || dbmsTypeName.equals("mediumblob") || dbmsTypeName.equals("longblob"))
			return new DBType(DBDatatype.BLOB);
		// CLOB
		else if (dbmsTypeName.equals("text") || dbmsTypeName.equals("tinytext") || dbmsTypeName.equals("mediumtext") || dbmsTypeName.equals("longtext"))
			return new DBType(DBDatatype.CLOB);
		// TIMESTAMP
		else if (dbmsTypeName.equals("timestamp") || dbmsTypeName.equals("date") || dbmsTypeName.equals("datetime") || dbmsTypeName.equals("time") || dbmsTypeName.equals("year"))
			return new DBType(DBDatatype.TIMESTAMP);
		// Default:
		else
			return null;
	}

	@Override
	public String convertTypeToDB(final DBType type){
		if (type == null)
			return "VARCHAR(" + DEFAULT_VARIABLE_LENGTH + ")";

		switch(type.type){

			case SMALLINT:
			case INTEGER:
			case REAL:
			case BIGINT:
			case TIMESTAMP:
				return type.type.toString();

			case DOUBLE:
				return "DOUBLE PRECISION";

			case CHAR:
			case VARCHAR:
			case BINARY:
			case VARBINARY:
				return type.type.toString() + "(" + (type.length > 0 ? type.length : DEFAULT_VARIABLE_LENGTH) + ")";

			case BLOB:
				return "BLOB";

			case CLOB:
				return "TEXT";

			case POINT:
			case REGION:
			default:
				return "VARCHAR(" + DEFAULT_VARIABLE_LENGTH + ")";
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

	/* ********************************************************************** */
	/* *                                                                    * */
	/* * SPATIAL FUNCTIONS TRANSLATION                                      * */
	/* *                                                                    * */
	/* ********************************************************************** */

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

}
