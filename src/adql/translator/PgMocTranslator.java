package adql.translator;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.MocAggFunction;
import adql.query.operand.function.geometry.MocFunction;

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
 * Copyright 2016 - Astronomisches Rechen Institut (ARI)
 */

/**
 * <p>Translates all ADQL objects into the SQL adaptation of Postgres+PgSphere+PgMoc.
 * Actually only the MOC related functions are translated in this class.
 * The other functions are managed by {@link PostgreSQLTranslator}.</p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (11/2016)
 */
public class PgMocTranslator extends PgSphereTranslator {

	/**
	 * Builds a PgMocTranslator which always translates in SQL all identifiers (schema, table and column) in a case sensitive manner ;
	 * in other words, schema, table and column names will be surrounded by double quotes in the SQL translation.
	 * 
	 * @see PgSphereTranslator#PgSphereTranslator()
	 */
	public PgMocTranslator(){
		super();
	}

	/**
	 * Builds a PgMocTranslator which always translates in SQL all identifiers (schema, table and column) in the specified case sensitivity ;
	 * in other words, schema, table and column names will all be surrounded or not by double quotes in the SQL translation.
	 * 
	 * @param allCaseSensitive	<i>true</i> to translate all identifiers in a case sensitive manner (surrounded by double quotes), <i>false</i> for case insensitivity.
	 * 
	 * @see PgSphereTranslator#PgSphereTranslator(boolean)
	 */
	public PgMocTranslator(boolean allCaseSensitive){
		super(allCaseSensitive);
	}

	/**
	 * Builds a PgMocTranslator which will always translate in SQL identifiers with the defined case sensitivity.
	 * 
	 * @param catalog	<i>true</i> to translate catalog names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param schema	<i>true</i> to translate schema names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param table		<i>true</i> to translate table names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param column	<i>true</i> to translate column names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * 
	 * @see PgSphereTranslator#PgSphereTranslator(boolean, boolean, boolean, boolean)
	 */
	public PgMocTranslator(boolean catalog, boolean schema, boolean table, boolean column){
		super(catalog, schema, table, column);
	}

	/*
	 * No need to implement the below functions because
	 * they have exactly the same name and signature in PostgreSQL+PgMoc.
	 * 
	@Override
	public String translate(final MocFunction moc) throws TranslationException{
		return super.translate(moc);
	}
	
	@Override
	public String translate(MocAggFunction mocAgg) throws TranslationException{
		return super.translate(mocAgg);
	}*/

	@Override
	public String translate(final AreaFunction areaFunction) throws TranslationException{

		/* TODO VERY IMPORTANT: In its current state this function does not work with ADQLColumn representing a MOC.
		 *                      In such case, the statement for a CIRCLE/POINT/BOX/POLYGON/REGION will be executed. */

		if (areaFunction.getParameter(0) instanceof MocFunction || areaFunction.getParameter(0) instanceof MocAggFunction)
			return "moc_area(" + translate(areaFunction.getParameter(0)) + ")";
		else
			return super.translate(areaFunction);
	}

	@Override
	public DBType convertTypeFromDB(final int dbmsType, final String rawDbmsTypeName, String dbmsTypeName, final String[] params){
		// If no type is provided return VARCHAR:
		if (dbmsTypeName == null || dbmsTypeName.trim().length() == 0)
			return null;

		// Put the dbmsTypeName in lower case for the following comparisons:
		dbmsTypeName = dbmsTypeName.toLowerCase();

		if (dbmsTypeName.equals("moc"))
			return new DBType(DBDatatype.MOC);
		else
			return super.convertTypeFromDB(dbmsType, rawDbmsTypeName, dbmsTypeName, params);
	}

	@Override
	public String convertTypeToDB(final DBType type){
		if (type != null && type.type == DBDatatype.MOC)
			return "moc";
		else
			return super.convertTypeToDB(type);
	}

}
