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
 *
 *
 * This file was contributed by the Space Telescope Science Institute (STScI)
 * for the purpose of implementing geometric search queries in ADQL
 * under a specific MSSQL architecture. It relies on stored procedures
 * available as a separate library at https://github.com/openSAIL/SQLServerTAPSupport
 * under the GPLv3 license.
 */

import adql.db.STCS.Region;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.ClauseConstraints;
import adql.query.ClauseSelect;
import adql.query.constraint.Comparison;
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
 * <p>MS SQL Server translator.</p>
 * 
 * <p><b>Important:</b>
 * 	This translator works correctly ONLY IF {@link SQLServer_ADQLQueryFactory} has been used
 * 	to create any ADQL query this translator is asked to translate.
 * </p>
 * 
 * TODO Extend this class for each MS SQL Server extension supporting geometry and particularly
 *      {@link #translateGeometryFromDB(Object)}, {@link #translateGeometryToDB(Region)} and all this other
 *      translate(...) functions for the ADQL's geometrical functions.
 * 
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (03/2016)
 * @since 1.4
 * 
 * @see SQLServer_ADQLQueryFactory
 */
public class MAST_ObsCore_SQLServerTranslator extends SQLServerTranslator {

	//The fnSpatial functions used for geometry at MAST return a table 
	//which is a join of the ivoa.obscore table and the geometric search results.
	//It has all the columns of the ivoa.obscore table.
	//In order to translate complex queries, we need to give this temporary table a unique name.
	private static String functionTableName = "obscoreTempResults";
	private static String baseTableName = "ivoa.obscore";
	
	public MAST_ObsCore_SQLServerTranslator(){
		super();
	}

	public MAST_ObsCore_SQLServerTranslator(final boolean allCaseSensitive){
		super(allCaseSensitive);
	}


	public MAST_ObsCore_SQLServerTranslator(final boolean catalog, final boolean schema, final boolean table, final boolean column){
		super(catalog, schema, table, column);
	}
	
	//If the query has a CONTAINS geometric function in it, an extensive rewrite is needed
	//for the Obscore specific spatial functions.
	//Due to the expense of searching for said geometric functions, this check is only done once,
	//and then specific Geometry-related translation functions defined here are used.
	//
	//Do not call the WithGeometry functions on queries that *may* not have geometry; 
	//translation errors will be incorrectly generated.
	private boolean queryHasGeometry(ADQLQuery query) throws TranslationException {
		if( query != null && 
			query.getWhere() != null && 
			findGeometryFunction(query.getWhere()) != null)
			return true;
		return false;
	}
	
	private ADQLFunction findGeometryFunction(ADQLList<? extends ADQLObject> list) throws TranslationException {
		Comparison comp;
		GeometryFunction fn = null;
		ADQLOperand comparator = null;
		for( int i = 0; i < list.size(); ++i) {
			if(list.get(i) instanceof Comparison) {
				comp = (adql.query.constraint.Comparison)list.get(i);
				if( comp.getLeftOperand() instanceof GeometryFunction) {
					fn = (GeometryFunction)comp.getLeftOperand();
					comparator = (ADQLOperand)comp.getRightOperand();
				}
				if( comp.getRightOperand() instanceof GeometryFunction) {
					fn = (GeometryFunction)comp.getRightOperand();
					comparator = (ADQLOperand)comp.getLeftOperand();
				}
			}
			//quick test for validity against what our translator handles.
			if( fn != null) {
				if( comparator instanceof NegativeOperand || ((NumericConstant)comparator).getNumericValue() > 1)
					throw new TranslationException("ADQL geometric queries can only be compared to 1 (true) or 0 (false). False is not currently supported.");
				if (((NumericConstant)comparator).getNumericValue() == 0)
					throw new TranslationException("ADQL geometric function support is currently limited to tests against 1 (true)");
				
				return (ADQLFunction)fn;
			}
		}	
		return null;
	}
	
	public String translateSelectWithGeometry(ClauseSelect clause) throws TranslationException{
		String sql = null;
		
		//tdower todo: this, cleanly
		sql = translate(clause);
		return sql.replace(baseTableName, functionTableName);
	}
	
	private String translateFunctionFromWhereWithGeometry(ADQLList<? extends ADQLObject> list) throws TranslationException {
		return translate(findGeometryFunction(list)) + " as " + functionTableName;
	}

	//Finds geometry comparison in where. If there is one, removes it. 
	//If there's anything left, translates the rest of the where clause by usual rules.
	private String translateRestOfWhereWithGeometry(ClauseConstraints where) throws TranslationException {
		if(findGeometryFunction(where) != null) {
			try {
				Comparison comp;
				for( int i = 0; i < where.size(); ++i) {
					comp = (adql.query.constraint.Comparison)where.get(i);
					if( comp.getLeftOperand() instanceof GeometryFunction || comp.getRightOperand() instanceof GeometryFunction) {
						if( where.size() > 1 ) {
							where.remove(i);
							break;
						}
						else
							return "";
					}
				}
			} catch (Exception e) {
				throw new TranslationException("Unexpected issue translating non-geometry section of WHERE constraints: " + e.getMessage());
			}
			
			//tdower todo: this, cleanly.
			String sql = translate(where);
			return sql.replace(baseTableName, functionTableName);
		}
		else
			return translate(where);
	}

	@Override
	public String translate(ADQLQuery query) throws TranslationException{
		//Normally the SQLServerTranslator functionality will suffice.
		if(!queryHasGeometry(query)) {
			return super.translate(query);
		}
		//but if the query contains geometry, 
		//it needs a rewrite with respect to the spatial function temporary table.
		else {
			StringBuffer sql = new StringBuffer(translateSelectWithGeometry(query.getSelect()));

			sql.append("\nFROM ");
			sql.append(translateFunctionFromWhereWithGeometry(query.getWhere()));
			
			sql.append('\n').append(translateRestOfWhereWithGeometry(query.getWhere()));

			if (!query.getGroupBy().isEmpty())
				sql.append('\n').append(translate(query.getGroupBy()));

			if (!query.getHaving().isEmpty())
				sql.append('\n').append(translate(query.getHaving()));

			if (!query.getOrderBy().isEmpty())
				sql.append('\n').append(translate(query.getOrderBy()));

			return sql.toString();
		}
	}

	@Override
	//we will be ignoring the Contains() in favor of using the fnSpatial syntax 
	//for the underlying spatial function.
	public String translate(final ContainsFunction fct) throws TranslationException {
		return(translate(fct.getLeftParam()));
	}

	@Override
	public String translate(final IntersectsFunction fct) throws TranslationException {
		throw new TranslationException("INTERSECTS queries not currently supported; only CONTAINS");
	}

	@Override
	public String translate(final PointFunction point) throws TranslationException {
		return ("dbo.fnObsCore_SearchSTCSFootprint('point " +
				((NumericConstant)(point.getCoord1())).getValue() + " " +
				((NumericConstant)(point.getCoord2())).getValue() + "')"); 
	}

	@Override
	public String translate(final CircleFunction circle) throws TranslationException {
		return ("dbo.fnObsCore_SearchSTCSFootprint('circle " +
				circle.getCoord1().toADQL() + " " +
				circle.getCoord2().toADQL() + " " +
				circle.getRadius().toADQL() + "')");
	}

	@Override
	public String translate(final BoxFunction box) throws TranslationException {
		return ("dbo.fnObsCore_SearchSTCSFootprint('box " +
				box.getCoord1().toADQL() + " " +
				box.getCoord2().toADQL() + " " +
				box.getWidth().toADQL() + " " +
				box.getHeight().toADQL() + "')");
	}

	@Override
	public String translate(final PolygonFunction polygon) throws TranslationException {
		StringBuilder fnCall = new StringBuilder("dbo.fnObsCore_SearchSTCSFootprint('polygon ");
		ADQLOperand[] operands = polygon.getParameters();
		for( int i = 0; i < operands.length; ++i) {
			if( operands[i] instanceof NumericConstant || operands[i] instanceof NegativeOperand)
				fnCall.append(operands[i].toADQL());
			if( i < operands.length - 1 )
				fnCall.append(" ");
		}
		
		fnCall.append("') ");
		return fnCall.toString();
	}

	@Override
	public String translate(final RegionFunction region) throws TranslationException {
		throw new TranslationException("Region queries are not currently supported, only POINT, CIRCLE, BOX, and POLYGON.");
	}
}
