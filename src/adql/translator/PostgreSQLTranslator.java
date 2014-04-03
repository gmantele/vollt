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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import adql.db.DBColumn;
import adql.db.DBTable;

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
import adql.query.constraint.Exists;
import adql.query.constraint.In;
import adql.query.constraint.IsNull;
import adql.query.constraint.NotConstraint;

import adql.query.from.FromContent;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;

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
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;

/**
 * <p>Translates all ADQL objects into the SQL adaptation of Postgres.</p>
 * 
 * <p><b><u>IMPORTANT:</u> The geometrical functions are translated exactly as in ADQL.
 * You will probably need to extend this translator to correctly manage the geometrical functions.
 * An extension is already available for PgSphere: {@link PgSphereTranslator}.</b></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 * 
 * @see PgSphereTranslator
 */
public class PostgreSQLTranslator implements ADQLTranslator {

	protected boolean inSelect = false;
	protected byte caseSensitivity = 0x00;

	/**
	 * Builds a PostgreSQLTranslator which takes into account the case sensitivity on column names.
	 * It means that column names which have been written between double quotes, will be also translated between double quotes.
	 */
	public PostgreSQLTranslator(){
		this(true);
	}

	/**
	 * Builds a PostgreSQLTranslator.
	 * 
	 * @param column	<i>true</i> to take into account the case sensitivity of column names, <i>false</i> otherwise.
	 */
	public PostgreSQLTranslator(final boolean column){
		caseSensitivity = IdentifierField.COLUMN.setCaseSensitive(caseSensitivity, column);
	}

	/**
	 * Builds a PostgreSQLTranslator.
	 * 
	 * @param catalog	<i>true</i> to take into account the case sensitivity of catalog names, <i>false</i> otherwise.
	 * @param schema	<i>true</i> to take into account the case sensitivity of schema names, <i>false</i> otherwise.
	 * @param table		<i>true</i> to take into account the case sensitivity of table names, <i>false</i> otherwise.
	 * @param column	<i>true</i> to take into account the case sensitivity of column names, <i>false</i> otherwise.
	 */
	public PostgreSQLTranslator(final boolean catalog, final boolean schema, final boolean table, final boolean column){
		caseSensitivity = IdentifierField.CATALOG.setCaseSensitive(caseSensitivity, catalog);
		caseSensitivity = IdentifierField.SCHEMA.setCaseSensitive(caseSensitivity, schema);
		caseSensitivity = IdentifierField.TABLE.setCaseSensitive(caseSensitivity, table);
		caseSensitivity = IdentifierField.COLUMN.setCaseSensitive(caseSensitivity, column);
	}

	/**
	 * Appends the full name of the given table to the given StringBuffer.
	 * 
	 * @param str		The string buffer.
	 * @param dbTable	The table whose the full name must be appended.
	 * 
	 * @return			The string buffer + full table name.
	 */
	public final StringBuffer appendFullDBName(final StringBuffer str, final DBTable dbTable){
		if (dbTable != null){
			if (dbTable.getDBCatalogName() != null)
				appendIdentifier(str, dbTable.getDBCatalogName(), IdentifierField.CATALOG).append('.');

			if (dbTable.getDBSchemaName() != null)
				appendIdentifier(str, dbTable.getDBSchemaName(), IdentifierField.SCHEMA).append('.');

			appendIdentifier(str, dbTable.getDBName(), IdentifierField.TABLE);
		}
		return str;
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
		return appendIdentifier(str, id, field.isCaseSensitive(caseSensitivity));
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
			return str.append('\"').append(id).append('\"');
		else
			return str.append(id);
	}

	@SuppressWarnings("unchecked")
	public String translate(ADQLObject obj) throws TranslationException {
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

	public String translate(ADQLQuery query) throws TranslationException {
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
	public String translate(ADQLList<? extends ADQLObject> list) throws TranslationException {
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
	protected String getDefaultADQLList(ADQLList<? extends ADQLObject> list) throws TranslationException {
		String sql = (list.getName()==null)?"":(list.getName()+" ");

		boolean oldInSelect = inSelect;
		inSelect = (list.getName() != null) && list.getName().equalsIgnoreCase("select");

		try{
			for(int i=0; i<list.size(); i++)
				sql += ((i == 0)?"":(" "+list.getSeparator(i)+" ")) + translate(list.get(i));
		}finally{
			inSelect = oldInSelect;
		}

		return sql;
	}

	public String translate(ClauseSelect clause) throws TranslationException {
		String sql = null;

		for(int i=0; i<clause.size(); i++){
			if (i == 0){
				sql = clause.getName()+(clause.distinctColumns()?" DISTINCT":"");
			}else
				sql += " "+clause.getSeparator(i);

			sql += " "+translate(clause.get(i));
		}

		return sql;
	}

	public String translate(ClauseConstraints clause) throws TranslationException {
		return getDefaultADQLList(clause);
	}

	public String translate(SelectItem item) throws TranslationException {
		if (item instanceof SelectAllColumns)
			return translate((SelectAllColumns)item);

		StringBuffer translation = new StringBuffer(translate(item.getOperand()));
		if (item.hasAlias()){
			translation.append(" AS ");
			appendIdentifier(translation, item.getAlias(), item.isCaseSensitive());
		}else
			translation.append(" AS ").append(item.getName());

		return translation.toString();
	}

	public String translate(SelectAllColumns item) throws TranslationException {
		HashMap<String, String> mapAlias = new HashMap<String, String>();

		// Fetch the full list of columns to display:
		Iterable<DBColumn> dbCols = null;
		if (item.getAdqlTable() != null && item.getAdqlTable().getDBLink() != null){
			ADQLTable table = item.getAdqlTable();
			dbCols = table.getDBLink();
			if (table.hasAlias()){
				String key = appendFullDBName(new StringBuffer(), table.getDBLink()).toString();
				mapAlias.put(key, table.isCaseSensitive(IdentifierField.ALIAS) ? ("\""+table.getAlias()+"\"") : table.getAlias());
			}
		}else if (item.getQuery() != null){
			dbCols = item.getQuery().getFrom().getDBColumns();
			ArrayList<ADQLTable> tables = item.getQuery().getFrom().getTables();
			for(ADQLTable table : tables){
				if (table.hasAlias()){
					String key = appendFullDBName(new StringBuffer(), table.getDBLink()).toString();
					mapAlias.put(key, table.isCaseSensitive(IdentifierField.ALIAS) ? ("\""+table.getAlias()+"\"") : table.getAlias());
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
					String fullDbName = appendFullDBName(new StringBuffer(), col.getTable()).toString();
					if (mapAlias.containsKey(fullDbName))
						appendIdentifier(cols, mapAlias.get(fullDbName), false).append('.');
					else
						cols.append(fullDbName).append('.');
				}
				appendIdentifier(cols, col.getDBName(), IdentifierField.COLUMN);
				cols.append(" AS \"").append(col.getADQLName()).append('\"');
			}
			return cols.toString();
		}else{
			return item.toADQL();
		}
	}

	public String translate(ColumnReference ref) throws TranslationException {
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
	protected String getDefaultColumnReference(ColumnReference ref) throws TranslationException {
		if (ref.isIndex()){
			return ""+ref.getColumnIndex();
		}else{
			if (ref.getDBLink() == null){
				return (ref.isCaseSensitive()?("\""+ref.getColumnName()+"\""):ref.getColumnName());
			}else{
				DBColumn dbCol = ref.getDBLink();
				StringBuffer colName = new StringBuffer();
				// Use the table alias if any:
				if (ref.getAdqlTable() != null && ref.getAdqlTable().hasAlias())
					appendIdentifier(colName, ref.getAdqlTable().getAlias(), ref.getAdqlTable().isCaseSensitive(IdentifierField.ALIAS)).append('.');

				// Use the DBTable if any:
				else if (dbCol.getTable() != null)
					appendFullDBName(colName, dbCol.getTable()).append('.');

				appendIdentifier(colName, dbCol.getDBName(), IdentifierField.COLUMN);

				return colName.toString();
			}
		}
	}

	public String translate(ADQLOrder order) throws TranslationException {
		return getDefaultColumnReference(order)+(order.isDescSorting()?" DESC":" ASC");
	}

	/* ************************** */
	/* ****** TABLE & JOIN ****** */
	/* ************************** */
	public String translate(FromContent content) throws TranslationException {
		if (content instanceof ADQLTable)
			return translate((ADQLTable)content);
		else if (content instanceof ADQLJoin)
			return translate((ADQLJoin)content);
		else
			return content.toADQL();
	}

	public String translate(ADQLTable table) throws TranslationException {
		StringBuffer sql = new StringBuffer();

		// CASE: SUB-QUERY:
		if (table.isSubQuery())
			sql.append('(').append(translate(table.getSubQuery())).append(')');

		// CASE: TABLE REFERENCE:
		else{
			// Use the corresponding DB table, if known:
			if (table.getDBLink() != null)
				appendFullDBName(sql, table.getDBLink());
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

	public String translate(ADQLJoin join) throws TranslationException {
		StringBuffer sql = new StringBuffer(translate(join.getLeftTable()));

		if (join.isNatural())
			sql.append(" NATURAL");

		sql.append(' ').append(join.getJoinType()).append(' ').append(translate(join.getRightTable())).append(' ');

		if (!join.isNatural()){
			if (join.getJoinCondition() != null)
				sql.append(translate(join.getJoinCondition()));
			else if (join.hasJoinedColumns()) {
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
	public String translate(ADQLOperand op) throws TranslationException {
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

	public String translate(ADQLColumn column) throws TranslationException {
		// Use its DB name if known:
		if (column.getDBLink() != null){
			DBColumn dbCol = column.getDBLink();
			StringBuffer colName = new StringBuffer();
			// Use the table alias if any:
			if (column.getAdqlTable() != null && column.getAdqlTable().hasAlias())
				appendIdentifier(colName, column.getAdqlTable().getAlias(), column.getAdqlTable().isCaseSensitive(IdentifierField.ALIAS)).append('.');

			// Use the DBTable if any:
			else if (dbCol.getTable() != null && dbCol.getTable().getDBName() != null)
				appendFullDBName(colName, dbCol.getTable()).append('.');

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

	public String translate(Concatenation concat) throws TranslationException {
		return translate((ADQLList<ADQLOperand>)concat);
	}

	public String translate(NegativeOperand negOp) throws TranslationException {
		return "-"+translate(negOp.getOperand());
	}

	public String translate(NumericConstant numConst) throws TranslationException {
		return numConst.getValue();
	}

	public String translate(StringConstant strConst) throws TranslationException {
		return "'"+strConst.getValue()+"'";
	}

	public String translate(WrappedOperand op) throws TranslationException {
		return "("+translate(op.getOperand())+")";
	}

	public String translate(Operation op) throws TranslationException {
		return translate(op.getLeftOperand())+op.getOperation().toADQL()+translate(op.getRightOperand());
	}

	/* ************************ */
	/* ****** CONSTRAINT ****** */
	/* ************************ */
	public String translate(ADQLConstraint cons) throws TranslationException {
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

	public String translate(Comparison comp) throws TranslationException {
		return translate(comp.getLeftOperand())+" "+comp.getOperator().toADQL()+" "+translate(comp.getRightOperand());
	}

	public String translate(Between comp) throws TranslationException {
		return translate(comp.getLeftOperand())+" BETWEEN "+translate(comp.getMinOperand())+" AND "+translate(comp.getMaxOperand());
	}

	public String translate(Exists exists) throws TranslationException {
		return "EXISTS("+translate(exists.getSubQuery())+")";
	}

	public String translate(In in) throws TranslationException {
		return translate(in.getOperand())+" "+in.getName()+" ("+(in.hasSubQuery()?translate(in.getSubQuery()):translate(in.getValuesList()))+")";
	}

	public String translate(IsNull isNull) throws TranslationException {
		return translate(isNull.getColumn())+" IS "+(isNull.isNotNull()?"NOT ":"")+"NULL";
	}

	public String translate(NotConstraint notCons) throws TranslationException {
		return "NOT "+translate(notCons.getConstraint());
	}

	/* *********************** */
	/* ****** FUNCTIONS ****** */
	/* *********************** */
	public String translate(ADQLFunction fct) throws TranslationException {
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
	protected String getDefaultADQLFunction(ADQLFunction fct) throws TranslationException {
		String sql = fct.getName()+"(";

		for(int i=0; i<fct.getNbParameters(); i++)
			sql += ((i==0)?"":", ")+translate(fct.getParameter(i));

		return sql+")";
	}

	public String translate(SQLFunction fct) throws TranslationException{
		if (fct.getType() == SQLFunctionType.COUNT_ALL)
			return "COUNT("+(fct.isDistinct()?"DISTINCT ":"")+"*)";
		else
			return fct.getName()+"("+(fct.isDistinct()?"DISTINCT ":"")+translate(fct.getParameter(0))+")";
	}

	public String translate(MathFunction fct) throws TranslationException {
		switch(fct.getType()){
		case LOG:
			return "ln("+((fct.getNbParameters()>=1)?translate(fct.getParameter(0)):"")+")";
		case LOG10:
			return "log(10, "+((fct.getNbParameters()>=1)?translate(fct.getParameter(0)):"")+")";
		case RAND:
			return "random()";
		case TRUNCATE:
			return "trunc("+((fct.getNbParameters()>=2)?(translate(fct.getParameter(0))+", "+translate(fct.getParameter(1))):"")+")";
		default:
			return getDefaultADQLFunction(fct);
		}
	}

	public String translate(UserDefinedFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	/* *********************************** */
	/* ****** GEOMETRICAL FUNCTIONS ****** */
	/* *********************************** */
	public String translate(GeometryFunction fct) throws TranslationException {
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
			return getDefaultGeometryFunction(fct);
	}

	/**
	 * <p>Gets the default SQL output for the given geometrical function.</p>
	 * 
	 * <p><i><u>Note:</u> By default, only the ADQL serialization is returned.</i></p>
	 * 
	 * @param fct	The geometrical function to translate.
	 * 
	 * @return		The corresponding SQL.
	 * 
	 * @throws TranslationException If there is an error during the translation.
	 */
	protected String getDefaultGeometryFunction(GeometryFunction fct) throws TranslationException {
		if (inSelect)
			return "'"+fct.toADQL().replaceAll("'", "''")+"'";
		else
			return getDefaultADQLFunction(fct);
	}

	public String translate(GeometryValue<? extends GeometryFunction> geomValue) throws TranslationException {
		return translate(geomValue.getValue());
	}

	public String translate(ExtractCoord extractCoord) throws TranslationException {
		return getDefaultGeometryFunction(extractCoord);
	}

	public String translate(ExtractCoordSys extractCoordSys) throws TranslationException {
		return getDefaultGeometryFunction(extractCoordSys);
	}

	public String translate(AreaFunction areaFunction) throws TranslationException {
		return getDefaultGeometryFunction(areaFunction);
	}

	public String translate(CentroidFunction centroidFunction) throws TranslationException {
		return getDefaultGeometryFunction(centroidFunction);
	}

	public String translate(DistanceFunction fct) throws TranslationException {
		return getDefaultGeometryFunction(fct);
	}

	public String translate(ContainsFunction fct) throws TranslationException {
		return getDefaultGeometryFunction(fct);
	}

	public String translate(IntersectsFunction fct) throws TranslationException {
		return getDefaultGeometryFunction(fct);
	}

	public String translate(BoxFunction box) throws TranslationException {
		return getDefaultGeometryFunction(box);
	}

	public String translate(CircleFunction circle) throws TranslationException {
		return getDefaultGeometryFunction(circle);
	}

	public String translate(PointFunction point) throws TranslationException {
		return getDefaultGeometryFunction(point);
	}

	public String translate(PolygonFunction polygon) throws TranslationException {
		return getDefaultGeometryFunction(polygon);
	}

	public String translate(RegionFunction region) throws TranslationException {
		return getDefaultGeometryFunction(region);
	}

}
