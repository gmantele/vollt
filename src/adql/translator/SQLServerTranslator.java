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
 * Copyright 2016 - Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Iterator;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DBType;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.SearchColumnList;
import adql.db.DBType.DBDatatype;
import adql.db.STCS.Region;
import adql.db.exception.UnresolvedJoinException;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.query.ADQLQuery;
import adql.query.ClauseSelect;
import adql.query.IdentifierField;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.operand.ADQLColumn;
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
 * <p>MS SQL Server translator.</p>
 * 
 * <p><b>Important:</b>
 * 	This translator works correctly ONLY IF {@link SQLServer_ADQLQueryFactory} has been used
 * 	to create any ADQL query this translator is asked to translate.
 * </p>
 * 
 * TODO See how case sensitivity is supported by MS SQL Server and modify this translator accordingly.
 * 
 * TODO Extend this class for each MS SQL Server extension supporting geometry and particularly
 *      {@link #translateGeometryFromDB(Object)}, {@link #translateGeometryToDB(Region)} and all this other
 *      translate(...) functions for the ADQL's geometrical functions.
 * 
 * TODO Check MS SQL Server datatypes (see {@link #convertTypeFromDB(int, String, String, String[])},
 *      {@link #convertTypeToDB(DBType)}).
 * 
 * <p><i><b>Important note:</b>
 * 	Geometrical functions are not translated ; the translation returned for them is their ADQL expression.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (03/2016)
 * @since 1.4
 * 
 * @see SQLServer_ADQLQueryFactory
 */
public class SQLServerTranslator extends JDBCTranslator {
	
	/* TODO Temporary MAIN function.
	 *      TO REMOVE for the release. */
	public final static void main(final String[] args) throws Exception {
		final String adqlquery = "SELECT id, name, aColumn, anotherColumn FROM aTable A NATURAL JOIN anotherTable B;";
		System.out.println("ADQL Query:\n"+adqlquery);
		
		ArrayList<DBTable> tables = new ArrayList<DBTable>(2);
		DefaultDBTable t = new DefaultDBTable("aTable");
		t.addColumn(new DefaultDBColumn("id", t));
		t.addColumn(new DefaultDBColumn("name", t));
		t.addColumn(new DefaultDBColumn("aColumn", t));
		tables.add(t);
		t = new DefaultDBTable("anotherTable");
		t.addColumn(new DefaultDBColumn("id", t));
		t.addColumn(new DefaultDBColumn("name", t));
		t.addColumn(new DefaultDBColumn("anotherColumn", t));
		tables.add(t);
		
		ADQLQuery query = (new ADQLParser(new DBChecker(tables), new SQLServer_ADQLQueryFactory())).parseQuery(adqlquery);
		
		SQLServerTranslator translator = new SQLServerTranslator();
		System.out.println("\nIn MS SQL Server:\n"+translator.translate(query));
	}

	/** <p>Indicate the case sensitivity to apply to each SQL identifier (only SCHEMA, TABLE and COLUMN).</p>
	 * 
	 * <p><i>Note:
	 * 	In this implementation, this field is set by the constructor and never modified elsewhere.
	 * 	It would be better to never modify it after the construction in order to keep a certain consistency.
	 * </i></p>
	 */
	protected byte caseSensitivity = 0x00;

	/**
	 * Builds an SQLServerTranslator which always translates in SQL all identifiers (schema, table and column) in a case sensitive manner ;
	 * in other words, schema, table and column names will be surrounded by double quotes in the SQL translation.
	 */
	public SQLServerTranslator(){
		caseSensitivity = 0x0F;
	}

	/**
	 * Builds an SQLServerTranslator which always translates in SQL all identifiers (schema, table and column) in the specified case sensitivity ;
	 * in other words, schema, table and column names will all be surrounded or not by double quotes in the SQL translation.
	 * 
	 * @param allCaseSensitive	<i>true</i> to translate all identifiers in a case sensitive manner (surrounded by double quotes), <i>false</i> for case insensitivity. 
	 */
	public SQLServerTranslator(final boolean allCaseSensitive){
		caseSensitivity = allCaseSensitive ? (byte)0x0F : (byte)0x00;
	}

	/**
	 * Builds an SQLServerTranslator which will always translate in SQL identifiers with the defined case sensitivity.
	 * 
	 * @param catalog	<i>true</i> to translate catalog names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param schema	<i>true</i> to translate schema names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param table		<i>true</i> to translate table names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 * @param column	<i>true</i> to translate column names with double quotes (case sensitive in the DBMS), <i>false</i> otherwise.
	 */
	public SQLServerTranslator(final boolean catalog, final boolean schema, final boolean table, final boolean column){
		caseSensitivity = IdentifierField.CATALOG.setCaseSensitive(caseSensitivity, catalog);
		caseSensitivity = IdentifierField.SCHEMA.setCaseSensitive(caseSensitivity, schema);
		caseSensitivity = IdentifierField.TABLE.setCaseSensitive(caseSensitivity, table);
		caseSensitivity = IdentifierField.COLUMN.setCaseSensitive(caseSensitivity, column);
	}

	@Override
	public boolean isCaseSensitive(final IdentifierField field) {
		return field == null ? false : field.isCaseSensitive(caseSensitivity);
	}
	
	/* For SQL Server, translate(ADQLQuery) must be overridden for TOP/LIMIT handling.
	 * We must not add "LIMIT" at the end of the query, it must go in select.
	 * @see adql.translator.ADQLTranslator#translate(adql.query.ADQLQuery)
	 */
	@Override
	public String translate(ADQLQuery query) throws TranslationException{
		StringBuffer sql = new StringBuffer(translate(query.getSelect()));

		sql.append("\nFROM ").append(translate(query.getFrom()));

		if (!query.getWhere().isEmpty())
			sql.append('\n').append(translate(query.getWhere()));

		if (!query.getGroupBy().isEmpty())
			sql.append('\n').append(translate(query.getGroupBy()));

		if (!query.getHaving().isEmpty())
			sql.append('\n').append(translate(query.getHaving()));

		if (!query.getOrderBy().isEmpty())
			sql.append('\n').append(translate(query.getOrderBy()));

		return sql.toString();
	}
	
	/* For SQL Server, translate(ClauseSelect) must be overridden for TOP/LIMIT handling.
	 * We must not add "LIMIT" at the end of the query, it must go in select.
	 * @see adql.translator.ADQLTranslator#translate(adql.query.ClauseSelect)
	 */
	@Override
	public String translate(ClauseSelect clause) throws TranslationException{
		String sql = null;
		
		for(int i = 0; i < clause.size(); i++){
			if (i == 0){
				sql = clause.getName() + 
				(clause.hasLimit() ? " TOP " + clause.getLimit() + " " : "") +
				(clause.distinctColumns() ? " DISTINCT" : "");
			}else
				sql += " " + clause.getSeparator(i);

			sql += " " + translate(clause.get(i));
		}

		return sql;
	}

	@Override
	public String translate(final ADQLJoin join) throws TranslationException {
		StringBuffer sql = new StringBuffer(translate(join.getLeftTable()));

		sql.append(' ').append(join.getJoinType()).append(' ').append(translate(join.getRightTable())).append(' ');

		// CASE: NATURAL
		if (join.isNatural()){
			try{
				StringBuffer buf = new StringBuffer();
			
				// Find duplicated items between the two lists and translate them as ON conditions:
				DBColumn rightCol;
				SearchColumnList leftList = join.getLeftTable().getDBColumns();
				SearchColumnList rightList = join.getRightTable().getDBColumns();
				for(DBColumn leftCol : leftList){
					// search for at most one column with the same name in the RIGHT list
					// and throw an exception if there are several matches:
					rightCol = ADQLJoin.findAtMostOneColumn(leftCol.getADQLName(), (byte)0, rightList, false);
					// if there is one...
					if (rightCol != null){
						// ...check there is only one column with this name in the LEFT list,
						// and throw an exception if it is not the case:
						ADQLJoin.findExactlyOneColumn(leftCol.getADQLName(), (byte)0, leftList, true);
						// ...append the corresponding join condition:
						if (buf.length() > 0)
							buf.append(" AND ");
						
						//if there is an alias for the left table, use it:
						if (leftCol.getTable().getADQLName() != null &&
							leftCol.getTable().getADQLName() != leftCol.getTable().getDBName()) {
							buf.append(leftCol.getTable().getADQLName());
							buf.append('.').append(leftCol.getADQLName());
						}
						else
							buf.append(getQualifiedTableName(leftCol.getTable())).append('.').append(getColumnName(leftCol));
						
						buf.append("=");
						
						//if there is an alias for the right table, use it.
						if (rightCol.getTable().getADQLName() != null &&
								rightCol.getTable().getADQLName() != rightCol.getTable().getDBName()) {
							buf.append(rightCol.getTable().getADQLName());
							buf.append('.').append(rightCol.getADQLName());
						}
						else
							buf.append(getQualifiedTableName(rightCol.getTable())).append('.').append(getColumnName(rightCol));
					}
				}		
				sql.append(" ON ").append(buf.toString());
				
			} catch(UnresolvedJoinException uje){
				throw new TranslationException("Impossible to resolve the NATURAL JOIN between "+join.getLeftTable().toADQL()+" and "+join.getRightTable().toADQL()+"!", uje);
			}
		}
		// CASE: USING
		else if (join.hasJoinedColumns()){
			try{
				StringBuffer buf = new StringBuffer();
				
				// For each columns of usingList, check there is in each list exactly one matching column, and then, translate it as ON condition:
				DBColumn leftCol, rightCol;
				ADQLColumn usingCol;
				SearchColumnList leftList = join.getLeftTable().getDBColumns();
				SearchColumnList rightList = join.getRightTable().getDBColumns();
				Iterator<ADQLColumn> itCols = join.getJoinedColumns();
				while(itCols.hasNext()){
					usingCol = itCols.next();
					// search for exactly one column with the same name in the LEFT list
					// and throw an exception if there is none, or if there are several matches:
					leftCol = ADQLJoin.findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), leftList, true);
					// item in the RIGHT list:
					rightCol = ADQLJoin.findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), rightList, false);
					// append the corresponding join condition:
					if (buf.length() > 0)
						buf.append(" AND ");
					
					//if table has alias, we must use it here.
					if (usingCol.getAdqlTable().getAlias() != null)
						buf.append(usingCol.getAdqlTable().getAlias()).append('.').append(getColumnName(leftCol));
					else
						buf.append(getQualifiedTableName(leftCol.getTable())).append('.').append(getColumnName(leftCol));
					buf.append("=");
					
					//if table has alias, we must use it here.
					if (usingCol.getAdqlTable().getAlias() != null)
						buf.append(usingCol.getAdqlTable().getAlias()).append('.').append(getColumnName(rightCol));
					else
						buf.append(getQualifiedTableName(rightCol.getTable())).append('.').append(getColumnName(rightCol));
				}
				
				sql.append("ON ").append(buf.toString());
			}catch(UnresolvedJoinException uje){
				throw new TranslationException("Impossible to resolve the JOIN USING between "+join.getLeftTable().toADQL()+" and "+join.getRightTable().toADQL()+"!", uje);
			}
		}
		// DEFAULT CASE:
		else
			sql.append(translate(join.getJoinCondition()));

		return sql.toString();
	}
	
	@Override
	public String translate(final ExtractCoord extractCoord) throws TranslationException {
		return getDefaultADQLFunction(extractCoord);
	}

	@Override
	public String translate(final ExtractCoordSys extractCoordSys) throws TranslationException {
		return getDefaultADQLFunction(extractCoordSys);
	}

	@Override
	public String translate(final AreaFunction areaFunction) throws TranslationException {
		return getDefaultADQLFunction(areaFunction);
	}

	@Override
	public String translate(final CentroidFunction centroidFunction) throws TranslationException {
		return getDefaultADQLFunction(centroidFunction);
	}

	@Override
	public String translate(final DistanceFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(final ContainsFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(final IntersectsFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(final PointFunction point) throws TranslationException {
		return getDefaultADQLFunction(point);
	}

	@Override
	public String translate(final CircleFunction circle) throws TranslationException {
		return getDefaultADQLFunction(circle);
	}

	@Override
	public String translate(final BoxFunction box) throws TranslationException {
		return getDefaultADQLFunction(box);
	}

	@Override
	public String translate(final PolygonFunction polygon) throws TranslationException {
		return getDefaultADQLFunction(polygon);
	}

	@Override
	public String translate(final RegionFunction region) throws TranslationException {
		return getDefaultADQLFunction(region);
	}
	
	@Override
	public String translate(MathFunction fct) throws TranslationException{
		 switch(fct.getType()){
		 case RADIANS:	
			 //MSSQL radians returns integer results if given them.
			 //To match other databases' functionality, convert here just in case:
			 if( fct.getNbParameters() > 0 && fct.getParameter(0).isNumeric())
				 return("radians(convert(float, " + (translate(fct.getParameter(0)) + "))"));
			 else
				 return getDefaultADQLFunction(fct);
		 	case TRUNCATE:
		 		// third argument to round nonzero means do a truncate
		    	return "round(" + ((fct.getNbParameters() >= 2) ? (translate(fct.getParameter(0)) + ", " + translate(fct.getParameter(1))) : "" ) + ",1)";
		    case MOD:
		    	return ((fct.getNbParameters() >= 2) ? (translate(fct.getParameter(0)) + "% " + translate(fct.getParameter(1))) : "");                
		    default:
		    	return getDefaultADQLFunction(fct);
		 }
	}	

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
			}catch(NumberFormatException nfe){}
		}

		// SMALLINT
		if (dbmsTypeName.equals("smallint") || dbmsTypeName.equals("tinyint") || dbmsTypeName.equals("bit"))
			return new DBType(DBDatatype.SMALLINT);
		// INTEGER
		else if (dbmsTypeName.equals("int"))
			return new DBType(DBDatatype.INTEGER);
		// BIGINT
		else if (dbmsTypeName.equals("bigint"))
			return new DBType(DBDatatype.BIGINT);
		// REAL (cf https://msdn.microsoft.com/fr-fr/library/ms173773(v=sql.120).aspx)
		else if (dbmsTypeName.equals("real") || (dbmsTypeName.equals("float") && lengthParam >= 1 && lengthParam <= 24))
			return new DBType(DBDatatype.REAL);
		// DOUBLE (cf https://msdn.microsoft.com/fr-fr/library/ms173773(v=sql.120).aspx)
		else if (dbmsTypeName.equals("float") || dbmsTypeName.equals("decimal") || dbmsTypeName.equals("numeric"))
			return new DBType(DBDatatype.DOUBLE);
		// BINARY
		else if (dbmsTypeName.equals("binary"))
			return new DBType(DBDatatype.BINARY, lengthParam);
		// VARBINARY
		else if (dbmsTypeName.equals("varbinary"))
			return new DBType(DBDatatype.VARBINARY, lengthParam);
		// CHAR
		else if (dbmsTypeName.equals("char") || dbmsTypeName.equals("nchar"))
			return new DBType(DBDatatype.CHAR, lengthParam);
		// VARCHAR
		else if (dbmsTypeName.equals("varchar") || dbmsTypeName.equals("nvarchar"))
			return new DBType(DBDatatype.VARCHAR, lengthParam);
		// BLOB
		else if (dbmsTypeName.equals("image"))
			return new DBType(DBDatatype.BLOB);
		// CLOB
		else if (dbmsTypeName.equals("text") || dbmsTypeName.equals("ntext"))
			return new DBType(DBDatatype.CLOB);
		// TIMESTAMP
		else if (dbmsTypeName.equals("timestamp") || dbmsTypeName.equals("datetime") || dbmsTypeName.equals("datetime2") || dbmsTypeName.equals("datetimeoffset") || dbmsTypeName.equals("smalldatetime") || dbmsTypeName.equals("time") || dbmsTypeName.equals("date") || dbmsTypeName.equals("date"))
			return new DBType(DBDatatype.TIMESTAMP);
		// Default:
		else
			return null;
	}

	@Override
	public String convertTypeToDB(final DBType type){
		if (type == null)
			return "varchar";

		switch(type.type){

			case SMALLINT:
			case REAL:
			case BIGINT:
			case CHAR:
			case VARCHAR:
			case BINARY:
			case VARBINARY:
				return type.type.toString().toLowerCase();

			case INTEGER:
				return "int";

			// (cf https://msdn.microsoft.com/fr-fr/library/ms173773(v=sql.120).aspx)
			case DOUBLE:
				return "float(53)";

			case TIMESTAMP:
				return "datetime";

			case BLOB:
				return "image";

			case CLOB:
				return "text";

			case POINT:
			case REGION:
			default:
				return "varchar";
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
