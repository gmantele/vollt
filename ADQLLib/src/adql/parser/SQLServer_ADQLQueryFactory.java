package adql.parser;

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

import adql.query.from.ADQLJoin;
import adql.query.from.CrossJoin;
import adql.query.from.FromContent;
import adql.query.from.InnerJoin;
import adql.query.from.OuterJoin;
import adql.query.from.SQLServer_InnerJoin;
import adql.query.from.SQLServer_OuterJoin;
import adql.query.from.OuterJoin.OuterType;
import adql.translator.SQLServerTranslator;

/**
 * <p>Special extension of {@link ADQLQueryFactory} for MS SQL Server.</p>
 * 
 * <p><b>Important:</b>
 * 	This class is generally used when an ADQL translator for MS SQL Server is needed.
 * 	See {@link SQLServerTranslator} for more details.
 * </p>
 * 
 * <p>
 * 	The only difference with {@link ADQLQueryFactory} is the creation of an
 * 	{@link ADQLJoin}. Instead of creating {@link InnerJoin} and {@link OuterJoin},
 * 	{@link SQLServer_InnerJoin} and {@link SQLServer_OuterJoin} are respectively created.
 * 	The only difference between these last classes and the first ones is in the processing
 * 	of NATURAL JOINs and JOINs using the keyword USING.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (03/2016)
 * @since 1.4
 * 
 * @see SQLServer_InnerJoin
 * @see SQLServer_OuterJoin
 * @see SQLServerTranslator
 */
public class SQLServer_ADQLQueryFactory extends ADQLQueryFactory {

	public ADQLJoin createJoin(JoinType type, FromContent leftTable, FromContent rightTable) throws Exception{
		switch(type){
			case CROSS:
				return new CrossJoin(leftTable, rightTable);
			case INNER:
				return new SQLServer_InnerJoin(leftTable, rightTable);
			case OUTER_LEFT:
				return new SQLServer_OuterJoin(leftTable, rightTable, OuterType.LEFT);
			case OUTER_RIGHT:
				return new SQLServer_OuterJoin(leftTable, rightTable, OuterType.RIGHT);
			case OUTER_FULL:
				return new SQLServer_OuterJoin(leftTable, rightTable, OuterType.FULL);
			default:
				throw new Exception("Unknown join type: " + type);
		}
	}
	
}