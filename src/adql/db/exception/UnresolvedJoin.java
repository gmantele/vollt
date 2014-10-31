package adql.db.exception;

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
 * Copyright 2013-2014 - Astronomisches Rechen Institut (ARI)
 */

import adql.parser.ParseException;
import adql.query.TextPosition;

/**
 * This exception is thrown when a table between 2 tables can not be resolved,
 * and particularly because of the join condition (i.e. column names not found, ...).
 * 
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 1.2 (11/2013)
 */
public class UnresolvedJoin extends ParseException {
	private static final long serialVersionUID = 1L;

	/**
	 * Build a simple UnresolvedJoin.
	 * It is generally used when a column can not be resolved (linked to one of the joined tables).
	 * 
	 * @param message	Message to display explaining why the join can't be resolved.
	 */
	public UnresolvedJoin(String message){
		super(message);
	}

	/**
	 * Build an UnresolvedJoin and specify, in addition of the error message, the position of the column not resolved.
	 * 
	 * @param message		Message to display explaining why the join can't be resolved.
	 * @param errorPosition	Position of the wrong part of the join.
	 */
	public UnresolvedJoin(String message, TextPosition errorPosition){
		super(message, errorPosition);
	}

}
