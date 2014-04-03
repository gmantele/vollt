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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.db.DBChecker;
import adql.parser.ParseException;
import adql.query.operand.ADQLColumn;

/**
 * This exception is thrown by {@link DBChecker} when a column does not exist
 * or whose the table reference is ambiguous.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 * 
 * @see DBChecker
 */
public class UnresolvedColumnException extends ParseException {
	private static final long serialVersionUID = 1L;

	private final String columnName;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * <p><b>UNKNOWN COLUMN</b></p>
	 * 
	 * <p>Builds the exception with an {@link ADQLColumn} which does not exist.</p>
	 * 
	 * @param c	The unresolved {@link ADQLColumn}.
	 */
	public UnresolvedColumnException(ADQLColumn c){
		super(buildMessage("Unknown column", c));
		initPosition(c);
		columnName = (c!=null)?c.getColumnName():null;
	}

	/**
	 * <p><b>AMBIGUOUS COLUMN NAME</b></p>
	 * 
	 * <p>
	 * 	Builds the exception with an {@link ADQLColumn} which does not have a table reference AND which may come from more than one table
	 * 	OR with an {@link ADQLColumn} which may reference more than one column in the table.
	 * </p>
	 * 
	 * @param c			The ambiguous {@link ADQLColumn}.
	 * @param col1		First possibility.
	 * @param col2		A second possibility.
	 */
	public UnresolvedColumnException(ADQLColumn c, String col1, String col2){
		super(buildMessage("Ambiguous column name", c, col1, col2));
		initPosition(c);
		columnName = (c!=null)?c.getColumnName():null;
	}

	protected final void initPosition(final ADQLColumn c){
		position = c.getPosition();
	}

	public final String getColumnName(){
		return columnName;
	}

	private static final String buildMessage(String msgStart, ADQLColumn c){
		StringBuffer msg = new StringBuffer();
		msg.append(msgStart).append(" \"").append(c.getFullColumnName()).append("\" !");
		return msg.toString();
	}

	private static final String buildMessage(String msgStart, ADQLColumn c, String col1, String col2){
		if (col1 != null && col2 != null){
			StringBuffer msg = new StringBuffer(buildMessage(msgStart, c));
			msg.append(" It may be (at least) \"").append(col1).append("\" or \"").append(col2).append("\".");
			return msg.toString();
		}else
			return buildMessage(msgStart, c);
	}

}
