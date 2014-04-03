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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.db.DBChecker;
import adql.query.ADQLQuery;

/**
 * <p>Used at the end of the parsing of each ADQL query by the {@link adql.parser.ADQLParser}, to check the generated {@link ADQLQuery} object.</p>
 * 
 * <p>Usually, it consists to check the existence of referenced columns and tables. In this case, one default implementation of this interface can be used: {@link DBChecker}</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 */
public interface QueryChecker {

	/**
	 * <p>Checks (non-recursively in sub-queries) the given {@link ADQLQuery}.</p>
	 * <p>If the query is correct, nothing happens. However at the first detected error, a {@link ParseException} is thrown.</p>
	 * 
	 * @param query				The query to check.
	 * 
	 * @throws ParseException	If the given query is not correct.
	 */
	public void check(ADQLQuery query) throws ParseException;

}
