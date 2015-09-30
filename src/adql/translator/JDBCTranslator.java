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
 * Copyright 2015 - Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import tap.data.DataReadException;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DBType;
import adql.db.STCS.Region;
import adql.db.exception.UnresolvedJoinException;
import adql.parser.ParseException;
import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ADQLOrder;
import adql.query.ADQLQuery;
import adql.query.ClauseConstraints;
import adql.query.ClauseSelect;
import adql.query.ColumnReference;
import adql.query.IdentifierField;
import adql.query.SelectAllColumns;
import adql.query.SelectItem;
import adql.query.constraint.ADQLConstraint;
import adql.query.constraint.Between;
import adql.query.constraint.Comparison;
import adql.query.constraint.ConstraintsGroup;
import adql.query.constraint.Exists;
import adql.query.constraint.In;
import adql.query.constraint.IsNull;
import adql.query.constraint.NotConstraint;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.Concatenation;
import adql.query.operand.NegativeOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.Operation;
import adql.query.operand.StringConstant;
import adql.query.operand.WrappedOperand;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.MathFunction;
import adql.query.operand.function.SQLFunction;
import adql.query.operand.function.SQLFunctionType;
import adql.query.operand.function.UserDefinedFunction;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.ExtractCoordSys;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

/**
 * <p>Implementation of {@link ADQLTranslator} which translates ADQL queries in SQL queries.</p>
 * 
 * <p>
 * 	It is already able to translate all SQL standard features, but lets abstract the translation of all
 * 	geometrical functions. So, this translator must be extended as {@link PostgreSQLTranslator} and
 * 	{@link PgSphereTranslator} are doing.
 * </p>
 * 
 * <p><i>Note:
 * 	Its default implementation of the SQL syntax has been inspired by the PostgreSQL one.
 * 	However, it should work also with SQLite and MySQL, but some translations might be needed
 * 	(as it is has been done for PostgreSQL about the mathematical functions).
 * </i></p>
 * 
 * <h3>PostgreSQLTranslator and PgSphereTranslator</h3>
 * 
 * <p>
 * 	{@link PgSphereTranslator} extends {@link PostgreSQLTranslator} and is able to translate geometrical
 * 	functions according to the syntax given by PgSphere. But it can also convert geometrical types
 * 	(from and toward the database), translate PgSphere regions into STC expression and vice-versa.
 * </p>
 * 
 * <p>
 * 	{@link PostgreSQLTranslator} overwrites the translation of mathematical functions whose some have
 * 	a different name or signature. Besides, it is also implementing the translation of the geometrical
 * 	functions. However, it does not really translate them. It is just returning the ADQL expression
 * 	(by calling {@link #getDefaultADQLFunction(ADQLFunction)}).
 * 	And so, of course, the execution of a SQL query containing geometrical functions and translated
 * 	using this translator will not work. It is just a default implementation in case there is no interest
 * 	of these geometrical functions.
 * </p>
 * 
 * <h3>SQL with or without case sensitivity?</h3>
 * 
 * <p>
 * 	In ADQL and in SQL, it is possible to tell the parser to respect the exact case or not of an identifier (schema, table or column name)
 * 	by surrounding it with double quotes. However ADQL identifiers and SQL ones may be different. In that way, the case sensitivity specified
 * 	in ADQL on the different identifiers can not be kept in SQL. That's why this translator lets specify a general rule on which types of
 * 	SQL identifier must be double quoted. This can be done by implementing the abstract function {@link #isCaseSensitive(IdentifierField)}.
 * 	The functions translating column and table names will call this function in order to surround the identifiers by double quotes or not.
 * 	So, <b>be careful if you want to override the functions translating columns and tables!</b>
 * </p>
 * 
 * <h3>Translation of "SELECT TOP"</h3>
 * 
 * <p>
 * 	The default behavior of this translator is to translate the ADQL "TOP" into the SQL "LIMIT" at the end of the query.
 * 	This is ok for some DBMS, but not all. So, if your DBMS does not know the "LIMIT" keyword, you should override the function
 * 	translating the whole query: {@link #translate(ADQLQuery)}. Here is its current implementation: 
 * </p>
 * <pre>
 * 	StringBuffer sql = new StringBuffer(translate(query.getSelect()));
 * 	sql.append("\nFROM ").append(translate(query.getFrom()));
 *	if (!query.getWhere().isEmpty())
 *		sql.append('\n').append(translate(query.getWhere()));
 *	if (!query.getGroupBy().isEmpty())
 *		sql.append('\n').append(translate(query.getGroupBy()));
 *	if (!query.getHaving().isEmpty())
 *		sql.append('\n').append(translate(query.getHaving()));
 *	if (!query.getOrderBy().isEmpty())
 *		sql.append('\n').append(translate(query.getOrderBy()));
 *	if (query.getSelect().hasLimit())
 *		sql.append("\nLimit ").append(query.getSelect().getLimit());
 *	return sql.toString();
 * </pre>
 * 
 * <h3>Translation of ADQL functions</h3>
 * 
 * <p>
 * 	All ADQL functions are by default not translated. Consequently, the SQL translation is
 * 	actually the ADQL expression. Generally the ADQL expression is generic enough. However some mathematical functions may need
 * 	to be translated differently. For instance {@link PostgreSQLTranslator} is translating differently: LOG, LOG10, RAND and TRUNC. 
 * </p>
 * 
 * <p><i>Note:
 * 	Geometrical regions and types have not been managed here. They stay abstract because it is obviously impossible to have a generic
 * 	translation and conversion ; it totally depends from the database system.
 * </i></p>
 * 
 * <h3>Translation of "FROM" with JOINs</h3>
 * 
 * <p>
 * 	The FROM clause is translated into SQL as written in ADQL. There is no differences except the identifiers that are replaced.
 * 	The tables' aliases and their case sensitivity are kept like in ADQL.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (09/2015)
 * @since 1.4
 * 
 * @see PostgreSQLTranslator
 * @see PgSphereTranslator
 */
public abstract class JDBCTranslator implements ADQLTranslator {

	/**
	 * <p>Tell whether the specified identifier MUST be translated so that being interpreted case sensitively or not.
	 * By default, an identifier that must be translated with case sensitivity will be surrounded by double quotes.
	 * But, if this function returns FALSE, the SQL name will be written just as given in the metadata, without double quotes.</p>
	 * 
	 * <p><b>WARNING</b>:
	 * 	An {@link IdentifierField} object can be a SCHEMA, TABLE, COLUMN and ALIAS. However, in this translator,
	 * 	aliases are translated like in ADQL (so, with the same case sensitivity specification as in ADQL).
	 * 	So, this function will never be used to know the case sensitivity to apply to an alias. It is then
	 * 	useless to write a special behavior for the ALIAS value.
	 * </p>
	 * 
	 * @param field	The identifier whose the case sensitive to apply is asked.
	 * 
	 * @return	<i>true</i> if the specified identifier must be translated case sensitivity, <i>false</i> otherwise (included if ALIAS or NULL).
	 */
	public abstract boolean isCaseSensitive(final IdentifierField field);

	/**
	 * <p>Get the qualified DB name of the schema containing the given table.</p>
	 * 
	 * <p><i>Note:
	 * 	This function will, by default, add double quotes if the schema name must be case sensitive in the SQL query.
	 * 	This information is provided by {@link #isCaseSensitive(IdentifierField)}.
	 * </i></p>
	 * 
	 * @param table	A table of the schema whose the qualified DB name is asked.
	 * 
	 * @return	The qualified (with DB catalog name prefix if any, and with double quotes if needed) DB schema name,
	 *        	or an empty string if there is no schema or no DB name.
	 */
	public String getQualifiedSchemaName(final DBTable table){
		if (table == null || table.getDBSchemaName() == null)
			return "";

		StringBuffer buf = new StringBuffer();

		if (table.getDBCatalogName() != null)
			appendIdentifier(buf, table.getDBCatalogName(), IdentifierField.CATALOG).append('.');

		appendIdentifier(buf, table.getDBSchemaName(), IdentifierField.SCHEMA);

		return buf.toString();
	}

	/**
	 * <p>Get the qualified DB name of the given table.</p>
	 * 
	 * <p><i>Note:
	 * 	This function will, by default, add double quotes if the table name must be case sensitive in the SQL query.
	 * 	This information is provided by {@link #isCaseSensitive(IdentifierField)}.
	 * </i></p>
	 * 
	 * @param table	The table whose the qualified DB name is asked.
	 * 
	 * @return	The qualified (with DB catalog and schema prefix if any, and with double quotes if needed) DB table name,
	 *        	or an empty string if the given table is NULL or if there is no DB name.
	 * 
	 * @see #getTableName(DBTable, boolean)
	 */
	public String getQualifiedTableName(final DBTable table){
		return getTableName(table, true);
	}

	/**
	 * <p>Get the DB name of the given table.
	 * The second parameter lets specify whether the table name must be prefixed by the qualified schema name or not.</p>
	 * 
	 * <p><i>Note:
	 * 	This function will, by default, add double quotes if the table name must be case sensitive in the SQL query.
	 * 	This information is provided by {@link #isCaseSensitive(IdentifierField)}.
	 * </i></p>
	 * 
	 * @param table			The table whose the DB name is asked.
	 * @param withSchema	<i>true</i> if the qualified schema name must prefix the table name, <i>false</i> otherwise. 
	 * 
	 * @return	The DB table name (prefixed by the qualified schema name if asked, and with double quotes if needed),
	 *        	or an empty string if the given table is NULL or if there is no DB name.
	 * 
	 * @since 2.0
	 */
	public String getTableName(final DBTable table, final boolean withSchema){
		if (table == null)
			return "";

		StringBuffer buf = new StringBuffer();
		if (withSchema){
			buf.append(getQualifiedSchemaName(table));
			if (buf.length() > 0)
				buf.append('.');
		}
		appendIdentifier(buf, table.getDBName(), IdentifierField.TABLE);

		return buf.toString();
	}

	/**
	 * <p>Get the DB name of the given column</p>
	 *  
	 * <p><i>Note:
	 * 	This function will, by default, add double quotes if the column name must be case sensitive in the SQL query.
	 * 	This information is provided by {@link #isCaseSensitive(IdentifierField)}.
	 * </i></p>
	 * 
	 * <p><b>Caution:
	 * 	The given column may be NULL and in this case an empty string will be returned.
	 * 	But if the given column is not NULL, its DB name MUST NOT BE NULL!
	 * </b></p>
	 * 
	 * @param column	The column whose the DB name is asked.
	 * 
	 * @return	The DB column name (with double quotes if needed),
	 *        	or an empty string if the given column is NULL.
	 */
	public String getColumnName(final DBColumn column){
		return (column == null) ? "" : appendIdentifier(new StringBuffer(), column.getDBName(), IdentifierField.COLUMN).toString();
	}

	/**
	 * Appends the given identifier in the given StringBuffer.
	 * 
	 * @param str		The string buffer.
	 * @param id		The identifier to append.
	 * @param field		The type of identifier (column, table, schema, catalog or alias ?).
	 * 
	 * @return			The string buffer + identifier.
	 */
	public final StringBuffer appendIdentifier(final StringBuffer str, final String id, final IdentifierField field){
		return appendIdentifier(str, id, isCaseSensitive(field));
	}

	/**
	 * Appends the given identifier to the given StringBuffer.
	 * 
	 * @param str				The string buffer.
	 * @param id				The identifier to append.
	 * @param caseSensitive		<i>true</i> to format the identifier so that preserving the case sensitivity, <i>false</i> otherwise.
	 * 
	 * @return					The string buffer + identifier.
	 */
	public static final StringBuffer appendIdentifier(final StringBuffer str, final String id, final boolean caseSensitive){
		if (caseSensitive)
			return str.append('"').append(id).append('"');
		else
			return str.append(id);
	}

	@Override
	@SuppressWarnings({"unchecked","rawtypes"})
	public String translate(ADQLObject obj) throws TranslationException{
		if (obj instanceof ADQLQuery)
			return translate((ADQLQuery)obj);
		else if (obj instanceof ADQLList)
			return translate((ADQLList)obj);
		else if (obj instanceof SelectItem)
			return translate((SelectItem)obj);
		else if (obj instanceof ColumnReference)
			return translate((ColumnReference)obj);
		else if (obj instanceof ADQLTable)
			return translate((ADQLTable)obj);
		else if (obj instanceof ADQLJoin)
			return translate((ADQLJoin)obj);
		else if (obj instanceof ADQLOperand)
			return translate((ADQLOperand)obj);
		else if (obj instanceof ADQLConstraint)
			return translate((ADQLConstraint)obj);
		else
			return obj.toADQL();
	}

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

		if (query.getSelect().hasLimit())
			sql.append("\nLimit ").append(query.getSelect().getLimit());

		return sql.toString();
	}

	/* *************************** */
	/* ****** LIST & CLAUSE ****** */
	/* *************************** */
	@Override
	public String translate(ADQLList<? extends ADQLObject> list) throws TranslationException{
		if (list instanceof ClauseSelect)
			return translate((ClauseSelect)list);
		else if (list instanceof ClauseConstraints)
			return translate((ClauseConstraints)list);
		else
			return getDefaultADQLList(list);
	}

	/**
	 * Gets the default SQL output for a list of ADQL objects.
	 * 
	 * @param list	List to format into SQL.
	 * 
	 * @return		The corresponding SQL.
	 * 
	 * @throws TranslationException If there is an error during the translation.
	 */
	protected String getDefaultADQLList(ADQLList<? extends ADQLObject> list) throws TranslationException{
		String sql = (list.getName() == null) ? "" : (list.getName() + " ");

		for(int i = 0; i < list.size(); i++)
			sql += ((i == 0) ? "" : (" " + list.getSeparator(i) + " ")) + translate(list.get(i));

		return sql;
	}

	@Override
	public String translate(ClauseSelect clause) throws TranslationException{
		String sql = null;

		for(int i = 0; i < clause.size(); i++){
			if (i == 0){
				sql = clause.getName() + (clause.distinctColumns() ? " DISTINCT" : "");
			}else
				sql += " " + clause.getSeparator(i);

			sql += " " + translate(clause.get(i));
		}

		return sql;
	}

	@Override
	public String translate(ClauseConstraints clause) throws TranslationException{
		if (clause instanceof ConstraintsGroup)
			return "(" + getDefaultADQLList(clause) + ")";
		else
			return getDefaultADQLList(clause);
	}

	@Override
	public String translate(SelectItem item) throws TranslationException{
		if (item instanceof SelectAllColumns)
			return translate((SelectAllColumns)item);

		StringBuffer translation = new StringBuffer(translate(item.getOperand()));
		if (item.hasAlias()){
			translation.append(" AS ");
			appendIdentifier(translation, item.getAlias(), item.isCaseSensitive());
		}else{
			translation.append(" AS ");
			appendIdentifier(translation, item.getName(), true);
		}

		return translation.toString();
	}

	@Override
	public String translate(SelectAllColumns item) throws TranslationException{
		HashMap<String,String> mapAlias = new HashMap<String,String>();

		// Fetch the full list of columns to display:
		Iterable<DBColumn> dbCols = null;
		if (item.getAdqlTable() != null && item.getAdqlTable().getDBLink() != null){
			ADQLTable table = item.getAdqlTable();
			dbCols = table.getDBLink();
			if (table.hasAlias()){
				String key = getQualifiedTableName(table.getDBLink());
				mapAlias.put(key, table.isCaseSensitive(IdentifierField.ALIAS) ? ("\"" + table.getAlias() + "\"") : table.getAlias());
			}
		}else if (item.getQuery() != null){
			try{
				dbCols = item.getQuery().getFrom().getDBColumns();
			}catch(UnresolvedJoinException pe){
				throw new TranslationException("Due to a join problem, the ADQL to SQL translation can not be completed!", pe);
			}
			ArrayList<ADQLTable> tables = item.getQuery().getFrom().getTables();
			for(ADQLTable table : tables){
				if (table.hasAlias()){
					String key = getQualifiedTableName(table.getDBLink());
					mapAlias.put(key, table.isCaseSensitive(IdentifierField.ALIAS) ? ("\"" + table.getAlias() + "\"") : table.getAlias());
				}
			}
		}

		// Write the DB name of all these columns:
		if (dbCols != null){
			StringBuffer cols = new StringBuffer();
			for(DBColumn col : dbCols){
				if (cols.length() > 0)
					cols.append(',');
				if (col.getTable() != null){
					String fullDbName = getQualifiedTableName(col.getTable());
					if (mapAlias.containsKey(fullDbName))
						appendIdentifier(cols, mapAlias.get(fullDbName), false).append('.');
					else
						cols.append(fullDbName).append('.');
				}
				appendIdentifier(cols, col.getDBName(), IdentifierField.COLUMN);
				cols.append(" AS \"").append(col.getADQLName()).append('\"');
			}
			return (cols.length() > 0) ? cols.toString() : item.toADQL();
		}else{
			return item.toADQL();
		}
	}

	@Override
	public String translate(ColumnReference ref) throws TranslationException{
		if (ref instanceof ADQLOrder)
			return translate((ADQLOrder)ref);
		else
			return getDefaultColumnReference(ref);
	}

	/**
	 * Gets the default SQL output for a column reference.
	 * 
	 * @param ref	The column reference to format into SQL.
	 * 
	 * @return		The corresponding SQL.
	 * 
	 * @throws TranslationException If there is an error during the translation.
	 */
	protected String getDefaultColumnReference(ColumnReference ref) throws TranslationException{
		if (ref.isIndex()){
			return "" + ref.getColumnIndex();
		}else{
			if (ref.getDBLink() == null){
				return (ref.isCaseSensitive() ? ("\"" + ref.getColumnName() + "\"") : ref.getColumnName());
			}else{
				DBColumn dbCol = ref.getDBLink();
				StringBuffer colName = new StringBuffer();
				// Use the table alias if any:
				if (ref.getAdqlTable() != null && ref.getAdqlTable().hasAlias())
					appendIdentifier(colName, ref.getAdqlTable().getAlias(), ref.getAdqlTable().isCaseSensitive(IdentifierField.ALIAS)).append('.');

				// Use the DBTable if any:
				else if (dbCol.getTable() != null)
					colName.append(getQualifiedTableName(dbCol.getTable())).append('.');

				appendIdentifier(colName, dbCol.getDBName(), IdentifierField.COLUMN);

				return colName.toString();
			}
		}
	}

	@Override
	public String translate(ADQLOrder order) throws TranslationException{
		return getDefaultColumnReference(order) + (order.isDescSorting() ? " DESC" : " ASC");
	}

	/* ************************** */
	/* ****** TABLE & JOIN ****** */
	/* ************************** */
	@Override
	public String translate(FromContent content) throws TranslationException{
		if (content instanceof ADQLTable)
			return translate((ADQLTable)content);
		else if (content instanceof ADQLJoin)
			return translate((ADQLJoin)content);
		else
			return content.toADQL();
	}

	@Override
	public String translate(ADQLTable table) throws TranslationException{
		StringBuffer sql = new StringBuffer();

		// CASE: SUB-QUERY:
		if (table.isSubQuery())
			sql.append('(').append(translate(table.getSubQuery())).append(')');

		// CASE: TABLE REFERENCE:
		else{
			// Use the corresponding DB table, if known:
			if (table.getDBLink() != null)
				sql.append(getQualifiedTableName(table.getDBLink()));
			// Otherwise, use the whole table name given in the ADQL query:
			else
				sql.append(table.getFullTableName());
		}

		// Add the table alias, if any:
		if (table.hasAlias()){
			sql.append(" AS ");
			appendIdentifier(sql, table.getAlias(), table.isCaseSensitive(IdentifierField.ALIAS));
		}

		return sql.toString();
	}

	@Override
	public String translate(ADQLJoin join) throws TranslationException{
		StringBuffer sql = new StringBuffer(translate(join.getLeftTable()));

		if (join.isNatural())
			sql.append(" NATURAL");

		sql.append(' ').append(join.getJoinType()).append(' ').append(translate(join.getRightTable())).append(' ');

		if (!join.isNatural()){
			if (join.getJoinCondition() != null)
				sql.append(translate(join.getJoinCondition()));
			else if (join.hasJoinedColumns()){
				StringBuffer cols = new StringBuffer();
				Iterator<ADQLColumn> it = join.getJoinedColumns();
				while(it.hasNext()){
					ADQLColumn item = it.next();
					if (cols.length() > 0)
						cols.append(", ");
					if (item.getDBLink() == null)
						appendIdentifier(cols, item.getColumnName(), item.isCaseSensitive(IdentifierField.COLUMN));
					else
						appendIdentifier(cols, item.getDBLink().getDBName(), IdentifierField.COLUMN);
				}
				sql.append("USING (").append(cols).append(')');
			}
		}

		return sql.toString();
	}

	/* ********************* */
	/* ****** OPERAND ****** */
	/* ********************* */
	@Override
	public String translate(ADQLOperand op) throws TranslationException{
		if (op instanceof ADQLColumn)
			return translate((ADQLColumn)op);
		else if (op instanceof Concatenation)
			return translate((Concatenation)op);
		else if (op instanceof NegativeOperand)
			return translate((NegativeOperand)op);
		else if (op instanceof NumericConstant)
			return translate((NumericConstant)op);
		else if (op instanceof StringConstant)
			return translate((StringConstant)op);
		else if (op instanceof WrappedOperand)
			return translate((WrappedOperand)op);
		else if (op instanceof Operation)
			return translate((Operation)op);
		else if (op instanceof ADQLFunction)
			return translate((ADQLFunction)op);
		else
			return op.toADQL();
	}

	@Override
	public String translate(ADQLColumn column) throws TranslationException{
		// Use its DB name if known:
		if (column.getDBLink() != null){
			DBColumn dbCol = column.getDBLink();
			StringBuffer colName = new StringBuffer();
			// Use the table alias if any:
			if (column.getAdqlTable() != null && column.getAdqlTable().hasAlias())
				appendIdentifier(colName, column.getAdqlTable().getAlias(), column.getAdqlTable().isCaseSensitive(IdentifierField.ALIAS)).append('.');

			// Use the DBTable if any:
			else if (dbCol.getTable() != null && dbCol.getTable().getDBName() != null)
				colName.append(getQualifiedTableName(dbCol.getTable())).append('.');

			// Otherwise, use the prefix of the column given in the ADQL query:
			else if (column.getTableName() != null)
				colName = column.getFullColumnPrefix().append('.');

			appendIdentifier(colName, dbCol.getDBName(), IdentifierField.COLUMN);

			return colName.toString();
		}
		// Otherwise, use the whole name given in the ADQL query:
		else
			return column.getFullColumnName();
	}

	@Override
	public String translate(Concatenation concat) throws TranslationException{
		return translate((ADQLList<ADQLOperand>)concat);
	}

	@Override
	public String translate(NegativeOperand negOp) throws TranslationException{
		return "-" + translate(negOp.getOperand());
	}

	@Override
	public String translate(NumericConstant numConst) throws TranslationException{
		return numConst.getValue();
	}

	@Override
	public String translate(StringConstant strConst) throws TranslationException{
		return "'" + strConst.getValue().replaceAll("'", "''") + "'";
	}

	@Override
	public String translate(WrappedOperand op) throws TranslationException{
		return "(" + translate(op.getOperand()) + ")";
	}

	@Override
	public String translate(Operation op) throws TranslationException{
		return translate(op.getLeftOperand()) + op.getOperation().toADQL() + translate(op.getRightOperand());
	}

	/* ************************ */
	/* ****** CONSTRAINT ****** */
	/* ************************ */
	@Override
	public String translate(ADQLConstraint cons) throws TranslationException{
		if (cons instanceof Comparison)
			return translate((Comparison)cons);
		else if (cons instanceof Between)
			return translate((Between)cons);
		else if (cons instanceof Exists)
			return translate((Exists)cons);
		else if (cons instanceof In)
			return translate((In)cons);
		else if (cons instanceof IsNull)
			return translate((IsNull)cons);
		else if (cons instanceof NotConstraint)
			return translate((NotConstraint)cons);
		else
			return cons.toADQL();
	}

	@Override
	public String translate(Comparison comp) throws TranslationException{
		return translate(comp.getLeftOperand()) + " " + comp.getOperator().toADQL() + " " + translate(comp.getRightOperand());
	}

	@Override
	public String translate(Between comp) throws TranslationException{
		return translate(comp.getLeftOperand()) + " " + comp.getName() + " " + translate(comp.getMinOperand()) + " AND " + translate(comp.getMaxOperand());
	}

	@Override
	public String translate(Exists exists) throws TranslationException{
		return "EXISTS(" + translate(exists.getSubQuery()) + ")";
	}

	@Override
	public String translate(In in) throws TranslationException{
		return translate(in.getOperand()) + " " + in.getName() + " (" + (in.hasSubQuery() ? translate(in.getSubQuery()) : translate(in.getValuesList())) + ")";
	}

	@Override
	public String translate(IsNull isNull) throws TranslationException{
		return translate(isNull.getColumn()) + " " + isNull.getName();
	}

	@Override
	public String translate(NotConstraint notCons) throws TranslationException{
		return "NOT " + translate(notCons.getConstraint());
	}

	/* *********************** */
	/* ****** FUNCTIONS ****** */
	/* *********************** */
	@Override
	public String translate(ADQLFunction fct) throws TranslationException{
		if (fct instanceof GeometryFunction)
			return translate((GeometryFunction)fct);
		else if (fct instanceof MathFunction)
			return translate((MathFunction)fct);
		else if (fct instanceof SQLFunction)
			return translate((SQLFunction)fct);
		else if (fct instanceof UserDefinedFunction)
			return translate((UserDefinedFunction)fct);
		else
			return getDefaultADQLFunction(fct);
	}

	/**
	 * Gets the default SQL output for the given ADQL function.
	 * 
	 * @param fct	The ADQL function to format into SQL.
	 * 
	 * @return		The corresponding SQL.
	 * 
	 * @throws TranslationException	If there is an error during the translation.
	 */
	protected final String getDefaultADQLFunction(ADQLFunction fct) throws TranslationException{
		String sql = fct.getName() + "(";

		for(int i = 0; i < fct.getNbParameters(); i++)
			sql += ((i == 0) ? "" : ", ") + translate(fct.getParameter(i));

		return sql + ")";
	}

	@Override
	public String translate(SQLFunction fct) throws TranslationException{
		if (fct.getType() == SQLFunctionType.COUNT_ALL)
			return "COUNT(" + (fct.isDistinct() ? "DISTINCT " : "") + "*)";
		else
			return fct.getName() + "(" + (fct.isDistinct() ? "DISTINCT " : "") + translate(fct.getParameter(0)) + ")";
	}

	@Override
	public String translate(MathFunction fct) throws TranslationException{
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(UserDefinedFunction fct) throws TranslationException{
		return fct.translate(this);
	}

	/* *********************************** */
	/* ****** GEOMETRICAL FUNCTIONS ****** */
	/* *********************************** */
	@Override
	public String translate(GeometryFunction fct) throws TranslationException{
		if (fct instanceof AreaFunction)
			return translate((AreaFunction)fct);
		else if (fct instanceof BoxFunction)
			return translate((BoxFunction)fct);
		else if (fct instanceof CentroidFunction)
			return translate((CentroidFunction)fct);
		else if (fct instanceof CircleFunction)
			return translate((CircleFunction)fct);
		else if (fct instanceof ContainsFunction)
			return translate((ContainsFunction)fct);
		else if (fct instanceof DistanceFunction)
			return translate((DistanceFunction)fct);
		else if (fct instanceof ExtractCoord)
			return translate((ExtractCoord)fct);
		else if (fct instanceof ExtractCoordSys)
			return translate((ExtractCoordSys)fct);
		else if (fct instanceof IntersectsFunction)
			return translate((IntersectsFunction)fct);
		else if (fct instanceof PointFunction)
			return translate((PointFunction)fct);
		else if (fct instanceof PolygonFunction)
			return translate((PolygonFunction)fct);
		else if (fct instanceof RegionFunction)
			return translate((RegionFunction)fct);
		else
			return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(GeometryValue<? extends GeometryFunction> geomValue) throws TranslationException{
		return translate(geomValue.getValue());
	}

	/**
	 * Convert any type provided by a JDBC driver into a type understandable by the ADQL/TAP library.
	 * 
	 * @param dbmsType			Type returned by a JDBC driver. <i>Note: this value is returned by ResultSetMetadata.getColumnType(int) and correspond to a type of java.sql.Types</i>
	 * @param rawDbmsTypeName	Full name of the type returned by a JDBC driver. <i>Note: this name is returned by ResultSetMetadata.getColumnTypeName(int) ; this name may contain parameters</i>
	 * @param dbmsTypeName		Name of type, without the eventual parameters. <i>Note: this name is extracted from rawDbmsTypeName.</i>
	 * @param typeParams		The eventual type parameters (e.g. char string length). <i>Note: these parameters are extracted from rawDbmsTypeName.</i>
	 * 
	 * @return	The corresponding ADQL/TAP type or NULL if the specified type is unknown.
	 */
	public abstract DBType convertTypeFromDB(final int dbmsType, final String rawDbmsTypeName, final String dbmsTypeName, final String[] typeParams);

	/**
	 * <p>Convert any type provided by the ADQL/TAP library into a type understandable by a JDBC driver.</p>
	 * 
	 * <p><i>Note:
	 * 	The returned DBMS type may contain some parameters between brackets.
	 * </i></p>
	 * 
	 * @param type	The ADQL/TAP library's type to convert.
	 * 
	 * @return	The corresponding DBMS type or NULL if the specified type is unknown.
	 */
	public abstract String convertTypeToDB(final DBType type);

	/**
	 * <p>Parse the given JDBC column value as a geometry object and convert it into a {@link Region}.</p>
	 * 
	 * <p><i>Note:
	 * 	Generally the returned object will be used to get its STC-S expression.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given column value is NULL, NULL will be returned.
	 * </i></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function is called ONLY for value of columns flagged as geometries by
	 * 	{@link #convertTypeFromDB(int, String, String, String[])}. So the value should always
	 * 	be of the expected type and format. However, if it turns out that the type is wrong
	 * 	and that the conversion is finally impossible, this function SHOULD throw a
	 * 	{@link DataReadException}.
	 * </i></p>
	 * 
	 * @param jdbcColValue	A JDBC column value (returned by ResultSet.getObject(int)).
	 * 
	 * @return	The corresponding {@link Region} if the given value is a geometry.
	 * 
	 * @throws ParseException	If the given object is not a geometrical object
	 *                       	or can not be transformed into a {@link Region} object.
	 */
	public abstract Region translateGeometryFromDB(final Object jdbcColValue) throws ParseException;

	/**
	 * <p>Convert the given STC region into a DB column value.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is used only by the UPLOAD feature, to import geometries provided as STC-S expression in
	 * 	a VOTable document inside a DB column.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given region is NULL, NULL will be returned.
	 * </i></p>
	 * 
	 * @param region	The region to store in the DB.
	 * 
	 * @return	The corresponding DB column object.
	 * 
	 * @throws ParseException	If the given STC Region can not be converted into a DB object.
	 */
	public abstract Object translateGeometryToDB(final Region region) throws ParseException;

}
