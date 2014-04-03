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

import adql.query.from.ADQLTable;

import adql.query.operand.ADQLColumn;

/**
 * This exception is thrown by {@link DBChecker} when a table does not exist
 * or whose the schema reference is ambiguous.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 * 
 * @see DBChecker
 */
public class UnresolvedTableException extends ParseException {
	private static final long serialVersionUID = 1L;

	private final String tableName;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * <p><b>UNKNOWN TABLE</b></p>
	 * 
	 * <p>Builds the exception with an {@link ADQLTable} which does not exist.</p>
	 * 
	 * @param table	The unresolved {@link ADQLTable}.
	 */
	public UnresolvedTableException(ADQLTable table) {
		super(buildMessage("Unknown table", table));
		initPosition(table);
		tableName = (table!=null)?table.getTableName():null;
	}

	/**
	 * <p><b>AMBIGUOUS TABLE NAME</b></p>
	 * 
	 * <p>
	 * 	Builds the exception with an {@link ADQLTable} which does not have a schema reference AND which may come from more than one schema.
	 * 	The two given schema names are schemas which contain a table with the same name as the given one.
	 * </p>
	 * 
	 * @param table		The ambiguous {@link ADQLTable} (no schema reference).
	 * @param t1		First possibility.
	 * @param t2		A second possibility.
	 */
	public UnresolvedTableException(ADQLTable table, String t1, String t2){
		super(buildMessage("Ambiguous table name", table, t1, t2));
		initPosition(table);
		tableName = (table!=null)?table.getTableName():null;
	}

	/**
	 * Initializes the position at which this exception occurs.
	 * 
	 * @param table The unresolved table.
	 */
	protected final void initPosition(ADQLTable table){
		position = table.getPosition();
	}

	/**
	 * <p><b>UNKNOWN TABLE REFERENCE</b></p>
	 * 
	 * <p>Builds the exception with an {@link ADQLColumn} whose the table reference is unknown.</p>
	 * 
	 * @param column	The {@link ADQLColumn} whose the table reference is unresolved.
	 */
	public UnresolvedTableException(ADQLColumn column){
		super(buildMessage("Unknown table reference", column));
		initPosition(column);
		tableName = (column!=null)?column.getTableName():null;
	}

	/**
	 * <p><b>AMBIGUOUS TABLE REFERENCE</b></p>
	 * 
	 * <p>
	 * 	Builds the exception with an {@link ADQLColumn} which has an ambiguous table reference.
	 * 	The two given table correspond to tables which match with the table reference of the given {@link ADQLColumn}.
	 * </p>
	 * 
	 * @param column	The {@link ADQLColumn} whose the table reference is ambiguous.
	 * @param table1	A table whose the name match with the table reference of the column.
	 * @param table2	Another table whose the name match with the table reference of the column.
	 */
	public UnresolvedTableException(ADQLColumn column, String table1, String table2){
		super(buildMessage("Ambiguous table reference", column, table1, table2));
		initPosition(column);
		tableName = (column!=null)?column.getTableName():null;
	}

	protected final void initPosition(ADQLColumn column){
		position = column.getPosition();
	}

	private static final String buildMessage(String msgStart, ADQLTable t){
		StringBuffer msg = new StringBuffer();

		msg.append(msgStart).append(" \"");
		if (t.isSubQuery())
			msg.append(t.getAlias()).append("\" !");
		else
			msg.append(t.getFullTableName()).append("\"").append(t.hasAlias()?(" (alias "+t.getAlias()+")"):"").append(" !");

		return msg.toString();
	}

	private static final String buildMessage(String msgStart, ADQLTable t, String t1, String t2){
		if (t1 != null && t2 != null){
			StringBuffer msg = new StringBuffer(buildMessage(msgStart, t));
			msg.append(" It may be (at least) \"").append(t1).append("\" or \"").append(t2).append("\".");
			return msg.toString();
		}else
			return buildMessage(msgStart, t);
	}

	private static final String buildMessage(String msgStart, ADQLColumn c){
		StringBuffer msg = new StringBuffer();

		msg.append(msgStart);
		msg.append(" \"").append(c.getFullColumnPrefix()).append("\" in \"").append(c.getFullColumnName()).append("\" !");

		return msg.toString();
	}

	private static final String buildMessage(String msgStart, ADQLColumn c, String table1, String table2){
		if (table1 != null && table2 != null){
			StringBuffer msg = new StringBuffer(buildMessage(msgStart, c));
			msg.append(" It may come (at least) from \"").append(table1).append("\" or from \"").append(table2).append("\".");
			return msg.toString();
		}else
			return buildMessage(msgStart, c);
	}

	public final String getTableName() {
		return tableName;
	}

}
