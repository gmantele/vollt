package adql.translator;

import java.util.List;
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
 *
 *
 * This file was contributed by the Space Telescope Science Institute (STScI)
 * for the purpose of implementing geometric search queries in ADQL
 * under a specific MSSQL architecture. It relies on stored procedures
 * available as a separate library at https://github.com/openSAIL/SQLServerTAPSupport
 * under the GPLv3 license.
 */

import adql.db.STCS.Region;
import adql.db.exception.UnresolvedJoinException;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ADQLOrder;
import adql.query.ADQLQuery;
import adql.query.ClauseADQL;
import adql.query.ClauseConstraints;
import adql.query.ClauseSelect;
import adql.query.constraint.Comparison;
import adql.query.from.ADQLTable;
import adql.query.from.FromContent;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NegativeOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

/**
 * <p>
 * MS SQL Server translator.
 * </p>
 * 
 * <p>
 * <b>Important:</b> This translator works correctly ONLY IF
 * {@link SQLServer_ADQLQueryFactory} has been used to create any ADQL query
 * this translator is asked to translate.
 * </p>
 * 
 * <p> This class handles queries where there are multiple potential
 * tables that are indexed for geometric queries, however it assumes
 * that only one of these geometric functions is called in a single query.
 * 
 * There is currently no error handling at this time for multiple geometry calls
 * in a single query, or for 'ra' and 'dec' columns to be incorrectly
 * listed or ordered in the ADQL query itself.
 * 
 * </p>
 * 
 * @author Theresa Dower
 * @version 1.4 (1/2019)
 * @since 1.4
 * 
 * @see SQLServer_ADQLQueryFactory
 */
public class MAST_Geometry_SQLServerTranslator extends SQLServerTranslator {

	// The fnSpatial_SearchSTCS functions used for geometry at MAST
	// return a table mimicking the table with a geometry index upon which we're searching.

	// In order to translate complex queries, we need to give the
	// temporary function tables a unique name, or catch aliasing in the query.
	private static String defaultFunctionTableName = "geometryTempResults";
	private static String defaultFunctionName = "STCSFootprint";

	private static String[] CatalogSchemaNames = null;
	private static String[] CatalogTableNames = null;
	private static String[] CatalogFunctionNames = null;	
	private static String[] CatalogUserFunctionNames = null;
	private static float maxRadiusAllowed = (float) 0.25; //default. negative for no limit.
	
	public MAST_Geometry_SQLServerTranslator(final String geometrySchemaNames[], 
			final String geometryTableNames[], final String userFunctionNames[], final String geometryFunctionNames[], final float geometryMaxRadius)
	{
		super(false); //all of our SQL databases are case-insensitive.

		CatalogSchemaNames = geometrySchemaNames;
		CatalogTableNames = geometryTableNames;		
		CatalogFunctionNames = geometryFunctionNames;
		
		//We're just interested in the function name to qualify it with the 'dbo.' schema.
		if( userFunctionNames != null) {
			CatalogUserFunctionNames = new String[userFunctionNames.length];
			for( int i = 0; i < userFunctionNames.length; ++i){
				if(userFunctionNames[i].contains("("))
						CatalogUserFunctionNames[i] = userFunctionNames[i].substring(0, userFunctionNames[i].indexOf("(")).trim();
				else
					CatalogUserFunctionNames[i] = userFunctionNames[i].trim();
			}
		}
		
		if( geometryMaxRadius >= 0)
			maxRadiusAllowed = geometryMaxRadius;
		else
			maxRadiusAllowed = -1;	
	}
	
	private boolean HasGeometryTables() {
		if(CatalogTableNames != null )
			return true;
		return false;
	}
	
	private String ReplaceTableNames(String input, String newValue) {
		for (int iGeometryTable = 0; iGeometryTable < CatalogTableNames.length; ++iGeometryTable){	
			if(input.contains(CatalogTableNames[iGeometryTable]))
				input = input.replace(CatalogTableNames[iGeometryTable], newValue);
		}
		return input;
	}
	
	// Only qualify these once; avoid calling multiple times in nested subquery parsing
	private String QualifyUserFunctionNames(String input) {
		if( CatalogUserFunctionNames == null ) return input;
		for (int iFunctionName = 0; iFunctionName < CatalogUserFunctionNames.length; ++iFunctionName){	
			if(input.contains(" " + CatalogUserFunctionNames[iFunctionName]))
				input = input.replace(" " + CatalogUserFunctionNames[iFunctionName], " dbo." + CatalogUserFunctionNames[iFunctionName]);
		} 
		return input;
	}

	// If the query has a CONTAINS geometric function in it, an extensive
	// rewrite is needed for specific spatial functions.
	// Due to the expense of searching for said geometric functions, this check is only done once,
	// and then specific Geometry-related translation functions defined here are used.
	//
	// Do not call the WithGeometry functions on queries that *may* not have geometry;
	// translation errors will be incorrectly generated.

	private boolean queryHasGeometry(ADQLQuery query) throws TranslationException {
		if (query != null && query.getWhere() != null && findGeometryFunction(query.getWhere()) != null)
			return true;
		return false;
	}

	private ADQLFunction findGeometryFunction(ADQLList<? extends ADQLObject> list) throws TranslationException {
		Comparison comp;
		GeometryFunction fn = null;
		ADQLOperand comparator = null;
		for (int i = 0; i < list.size(); ++i) {
			if (list.get(i) instanceof Comparison) {
				comp = (adql.query.constraint.Comparison) list.get(i);
				if (comp.getLeftOperand() instanceof GeometryFunction) {
					fn = (GeometryFunction) comp.getLeftOperand();
					comparator = (ADQLOperand) comp.getRightOperand();
				}
				if (comp.getRightOperand() instanceof GeometryFunction) {
					fn = (GeometryFunction) comp.getRightOperand();
					comparator = (ADQLOperand) comp.getLeftOperand();
				}
			}
			// Some tests for validity specific to what our translator supports:
			if (fn != null) {
				if (comparator instanceof NegativeOperand || ((NumericConstant) comparator).getNumericValue() > 1) {
					throw new TranslationException(
							"ADQL geometric queries can only be compared to 1 (true) or 0 (false). False is not currently supported.");
				}
				if (((NumericConstant) comparator).getNumericValue() == 0) {
					throw new TranslationException(
							"ADQL geometric function support is currently limited to tests against 1 (true)");
				}
				return (ADQLFunction) fn;
			}
		}
		return null;
	}

	/*We are currently only supporting one geometry function/query per table.
	 * This will be part of a significant rewrite if we change that.
	 */
	private ADQLTable getGeometryTable(FromContent from) throws TranslationException {
		if( !HasGeometryTables())
			throw new TranslationException("No table with geometry indexing defined.");
		
		List<ADQLTable> tables = from.getTables();
		for (int iGeometryTable = 0; iGeometryTable < CatalogTableNames.length; ++iGeometryTable){
			String compareTableName = CatalogTableNames[iGeometryTable].toLowerCase();
			String compareSchemaName = CatalogSchemaNames[iGeometryTable].toLowerCase();
			for (int i = 0; i < tables.size(); i++) {
				String tablename = tables.get(i).getFullTableName().toLowerCase();
				if (tablename.equals(compareTableName) ||
					compareTableName.equals(compareSchemaName + '.' + tablename))
					return tables.get(i);
			}
		}
		throw new TranslationException("Geometry query requested for non-indexed table.");
	}
	
	public String translateSelectWithGeometry(ClauseSelect clause, String functionTableName)
			throws TranslationException {
		String sql = translate(clause);	
		
		sql = ReplaceTableNames(sql, functionTableName);
		return sql;
	}
	
	//note: side effect, we end up with lower casing. It could be prettier but would take longer to run.
	private String translateFromWithGeometry(FromContent from, ADQLFunction contains) 
			throws TranslationException {	
		
		String mainFromContent = translate(from).toLowerCase();		
		String translatedContains = translate(contains);		
		
		ADQLTable table = getGeometryTable(from);		
		String geometryTableAlias = table.getAlias();
		
		//technically we should be able to query without the schema.
		String geometryTableName = table.getFullTableName().toLowerCase();
		if( !geometryTableName.contains(".")) {			
			for (int iGeometryTable = 0; iGeometryTable < CatalogTableNames.length; ++iGeometryTable){		
				if( CatalogTableNames[iGeometryTable].toLowerCase().contains(geometryTableName))
					geometryTableName = CatalogSchemaNames[iGeometryTable] + '.' + geometryTableName;
			}
		}
		
		int beginTable = mainFromContent.indexOf(geometryTableName.toLowerCase());		
		int endTable = mainFromContent.indexOf(" ", beginTable);
		
		String results = mainFromContent.substring(0, beginTable) + translatedContains;	
		
		//For geometry function call to work, a known alias or the default must be used:
		if( geometryTableAlias == null ) {
			results += " AS " + defaultFunctionTableName;
			if (endTable > -1 )
				results += mainFromContent.substring(endTable).replace(geometryTableName, defaultFunctionTableName);
		}
		else if( endTable > -1 )
			results += mainFromContent.substring(endTable);

		for (int iGeometryTable = 0; iGeometryTable < CatalogTableNames.length; ++iGeometryTable){		
			if( geometryTableName.equals(CatalogTableNames[iGeometryTable].toLowerCase())) {
				results = results.replace(defaultFunctionName, CatalogFunctionNames[iGeometryTable]);
			}
		}
		
		return results;
	}

	// Finds geometry comparison in where. If there is one, removes it.
	// If there's anything left, translates the rest of the where clause by
	// usual rules.
	private String translateRestOfWhereWithGeometry(ClauseConstraints where, String functionTableName)
			throws TranslationException {
		if (findGeometryFunction(where) != null) {
			try {
				Comparison comp;
				for (int i = 0; i < where.size(); ++i) {
					comp = (adql.query.constraint.Comparison) where.get(i);
					if (comp.getLeftOperand() instanceof GeometryFunction
							|| comp.getRightOperand() instanceof GeometryFunction) {
						if (where.size() > 1) {
							where.remove(i);
							break;
						} else
							return "";
					}
				}
			} catch (Exception e) {
				throw new TranslationException(
						"Unexpected issue translating non-geometry section of WHERE constraints: " + e.getMessage());
			}

			String sql = translate(where);
			sql = ReplaceTableNames(sql, functionTableName);
			return sql;
		} 
		else {
			return translate(where);
		}
	}
	
	@Override
	public String translate(ADQLQuery query) throws TranslationException {

		// if the query contains geometry,
		// it needs a rewrite with respect to the spatial function temporary
		// table.
		if (queryHasGeometry(query)) {
			if(!HasGeometryTables())
				throw new TranslationException("No table with geometry indexing defined.");
						
			String functionTableName = defaultFunctionTableName; 
			ADQLTable table = getGeometryTable(query.getFrom()); //use alias if user-provided
			if (table.getAlias() != null)
				functionTableName = table.getAlias();

			StringBuffer sql = new StringBuffer(
					translateSelectWithGeometry(query.getSelect(), functionTableName));

			sql.append("\nFROM ");
			
			//We move the contains logic from the WHERE clause to a function call in FROM:
			ADQLFunction contains = findGeometryFunction(query.getWhere());
			sql.append(translateFromWithGeometry(query.getFrom(), contains));

			sql.append('\n').append(translateRestOfWhereWithGeometry(query.getWhere(), functionTableName));

			String geomReplace;
			if (!query.getGroupBy().isEmpty()) {
				geomReplace = translate(query.getGroupBy());
				geomReplace = ReplaceTableNames(geomReplace, functionTableName);
				sql.append('\n').append(geomReplace);
			}

			if (!query.getHaving().isEmpty()) {
				geomReplace = translate(query.getHaving());
				geomReplace = ReplaceTableNames(geomReplace, functionTableName);
				sql.append('\n').append(geomReplace);
			}

			//If no order by is given AND this is not a count query,
			//create a default to have deterministic results in geometry responses.
			if(!(query.getSelect().toADQL().contains(" COUNT"))) {
				if(query.getOrderBy().isEmpty()) {
					ClauseADQL<ADQLOrder> newOrderBy = new ClauseADQL<ADQLOrder>("ORDER BY");													
					ADQLOrder defaultGeometryOrder = new ADQLOrder(functionTableName + ".distance");
					newOrderBy.add(defaultGeometryOrder);				
					query.setOrderBy(newOrderBy);				
				}
				geomReplace = translate(query.getOrderBy());
				geomReplace = ReplaceTableNames(geomReplace, functionTableName);
				sql.append('\n').append(geomReplace);
			}
			
			String sqlString = sql.toString();
			return QualifyUserFunctionNames(sqlString);
		} 

		//Without geometry, the SQLServerTranslator functionality will suffice
		//except for the MSSQL-required function schema prefix.
		String sql = super.translate(query);
		return QualifyUserFunctionNames(sql);
	}

	@Override
	// we use the fnSpatial syntax for the underlying spatial function.
	public String translate(final ContainsFunction fct) throws TranslationException {

		StringBuilder fnCall = new StringBuilder(defaultFunctionName + "(");
	
		// Check for region simple case from CAOM/ObsCore data models
		if (fct.getRightParam().isColumn()) {
			fnCall.append(translate(fct.getLeftParam()));
		} // else find and work with ra, dec, etc.
		else if (fct.getLeftParam().isGeometry() && fct.getRightParam().isGeometry()) {
			fnCall.append(translate(fct.getRightParam()));
		} else {
			throw new TranslationException("Invalid CONTAINS parameters");
		}	

		fnCall.append(")");
		return fnCall.toString();
	}

	@Override
	public String translate(final PointFunction point) throws TranslationException {
		return ("'point " + point.getCoord1().toADQL() + " "
				+ point.getCoord2().toADQL() + "'");
	}

	@Override
	public String translate(final CircleFunction circle) throws TranslationException {
		String strRadius = circle.getRadius().toADQL();
		if( maxRadiusAllowed > 0 && Float.valueOf(strRadius) > maxRadiusAllowed )
			throw new TranslationException("Maximum query radius of " + maxRadiusAllowed + " degrees exceeded.");
		else
			return ("'circle " + circle.getCoord1().toADQL() + " " + circle.getCoord2().toADQL() + " "
				+ circle.getRadius().toADQL() + "'");
	}

	@Override
	public String translate(final BoxFunction box) throws TranslationException {
		String strWidth = box.getWidth().toADQL();
		String strHeight = box.getHeight().toADQL();
		
		if( maxRadiusAllowed > 0 && (Float.valueOf(strWidth) > maxRadiusAllowed * 2 ) )
			throw new TranslationException("Maximum query width of " + maxRadiusAllowed * 2 + " degrees exceeded.");		
		if( maxRadiusAllowed > 0 && (Float.valueOf(strHeight) > maxRadiusAllowed * 2 ) )
			throw new TranslationException("Maximum query height of " + maxRadiusAllowed + " degrees exceeded.");		
		
		return ("'box " + box.getCoord1().toADQL() + " " + box.getCoord2().toADQL() + " " + box.getWidth().toADQL()
				+ " " + box.getHeight().toADQL() + "'");
	}

	@Override
	//TODO: size limiting, see maximum query height/width in other functions.
	//We can put a check in the db spatial function itself, or make another area function to call.
	//The +/- and 0/360 boundaries on the celestial sphere make this difficult to recreate here.
	public String translate(final PolygonFunction polygon) throws TranslationException {
		
		//first parameter is coordinate frame, checked elsewhere, followed by points.
		ADQLOperand[] parameters = polygon.getParameters();			
		
		String coords = "'polygon ";
		for( int i = 1; i < parameters.length; ++i ) {
			coords += parameters[i].toADQL();
			if( i == parameters.length - 1)
				coords += "'";
			else
				coords += " ";
		}
		return coords;		
	}
}
