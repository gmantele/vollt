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
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.UserDefinedFunction;
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
 * @author Theresa Dower (STScI)
 * @version 1.4 (06/2016)
 * @since 1.4
 * 
 * @see SQLServerTranslator, SQLServer_ADQLQueryFactory
 */


public class MAST_SQLServerTranslator extends SQLServerTranslator {

	private static String[] CatalogUserFunctionNames = null;
	
	public MAST_SQLServerTranslator(){
		super(false);
	}
	
	public MAST_SQLServerTranslator(final String userFunctionNames[]){
		super(false);
		if( userFunctionNames == null ) return;
		
		//We're just interested in the function name to qualify it with the 'dbo.' schema.
		CatalogUserFunctionNames = new String[userFunctionNames.length];
		for( int i = 0; i < userFunctionNames.length; ++i){
			if(userFunctionNames[i].contains("("))
					CatalogUserFunctionNames[i] = userFunctionNames[i].substring(0, userFunctionNames[i].indexOf("(")).trim();
			else
				CatalogUserFunctionNames[i] = userFunctionNames[i].trim();
		}
	}
	
	private String QualifyUserFunctionNames(String input) {
		if( CatalogUserFunctionNames == null ) return input;
		for (int iFunctionName = 0; iFunctionName < CatalogUserFunctionNames.length; ++iFunctionName){	
			if(input.contains(CatalogUserFunctionNames[iFunctionName]))
				input = input.replace(CatalogUserFunctionNames[iFunctionName], "dbo." + CatalogUserFunctionNames[iFunctionName]);
		}
		return input;
	}
	
	@Override
	public String translate(ADQLQuery query) throws TranslationException {
		//SQLServerTranslator functionality will suffice except for the MSSQL-required function schema prefix.
		String sql = super.translate(query);
		return QualifyUserFunctionNames(sql);
	}
}
