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
 * Copyright 2012-2022 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.db.DBChecker;
import adql.parser.grammar.ParseException;
import adql.query.ADQLSet;

/**
 * Used at the end of the parsing of each ADQL query by the
 * {@link adql.parser.ADQLParser}, to check the generated {@link ADQLSet}
 * object.
 *
 * <p>
 * 	Usually, it consists to check the existence of referenced columns and
 * 	tables. In this case, one default implementation of this interface can be
 * 	used: {@link DBChecker}
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2022)
 */
public interface QueryChecker {

	/**
	 * Checks the given {@link ADQLSet}.
	 *
	 * <p><i>
	 * 	<b>IMPORTANT NOTE:</b>
	 * 	All subqueries must also be checked when calling this function!
	 * </i></p>
	 *
	 * <p>
	 * 	If the query is correct, nothing happens. However at the first
	 * 	detected error, a {@link ParseException} is thrown.
	 * </p>
	 *
	 * @param query				The query to check.
	 *
	 * @throws ParseException	If the given query is not correct.
	 */
	public void check(ADQLSet query) throws ParseException;

}
