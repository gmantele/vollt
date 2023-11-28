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
 * Copyright 2017-2022 - Astronomisches Rechen Institut (ARI),
 *                       UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.Iterator;

import adql.db.DBColumn;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.SearchColumnList;
import adql.db.exception.UnresolvedJoinException;
import adql.db.region.Region;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.ADQLQuery;
import adql.query.ClauseSelect;
import adql.query.IdentifierField;
import adql.query.SetOperation;
import adql.query.constraint.Comparison;
import adql.query.constraint.ComparisonOperator;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.Concatenation;
import adql.query.operand.function.InUnitFunction;
import adql.query.operand.function.MathFunction;
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

/**
 * MS SQL Server translator.
 *
 * <p><b>Important:</b>
 * 	This translator works correctly ONLY IF {@link SQLServer_ADQLQueryFactory}
 * 	has been used to create any ADQL query this translator is asked to
 * 	translate.
 * </p>
 *
 * TODO Translation of Set operations, TOP/LIMIT, OFFSET, ORDER BY
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
 * <p><i><b>Important note 1:</b>
 * 	Geometrical functions and IN_UNIT are not translated ; the translation
 * 	returned for them is their ADQL expression.
 * </i></p>
 *
 * <p><i><b>Important note 2:</b>
 * 	If new optional features are supported in an extension of this translator,
 * 	they should be visible in {@link #getSupportedFeatures()}. To customize this
 * 	list, you must overwrite {@link #initSupportedFeatures()} and update in
 * 	there the attribute {@link #supportedFeatures}.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 2.0 (07/2022)
 * @since 1.4
 *
 * @see SQLServer_ADQLQueryFactory
 */
public class SQLServerTranslator extends JDBCTranslator {

	/** <p>Indicate the case sensitivity to apply to each SQL identifier (only SCHEMA, TABLE and COLUMN).</p>
	 *
	 * <p><i>Note:
	 * 	In this implementation, this field is set by the constructor and never modified elsewhere.
	 * 	It would be better to never modify it after the construction in order to keep a certain consistency.
	 * </i></p>
	 */
	protected byte caseSensitivity = 0x00;

	/** List of all optional features supported by this translator.
	 * <p><i><b>Note:</b>
	 * 	This list can be customized by extending this translator and then
	 * 	overwriting {@link #initSupportedFeatures()}.
	 * </i></p>
	 * @since 2.0 */
	protected final FeatureSet supportedFeatures = new FeatureSet();

	/**
	 * Builds an SQLServerTranslator which always translates in SQL all
	 * identifiers (schema, table and column) in a case sensitive manner ; in
	 * other words, schema, table and column names will be surrounded by double
	 * quotes in the SQL translation.
	 */
	public SQLServerTranslator() {
		caseSensitivity = 0x0F;
		initSupportedFeatures();
	}

	/**
	 * Builds an SQLServerTranslator which always translates in SQL all
	 * identifiers (schema, table and column) in the specified case
	 * sensitivity ; in other words, schema, table and column names will all be
	 * surrounded or not by double quotes in the SQL translation.
	 *
	 * @param allCaseSensitive	<code>true</code> to translate all identifiers
	 *                        	in a case sensitive manner (surrounded by double
	 *                        	quotes),
	 *                        	<code>false</code> for case insensitivity.
	 */
	public SQLServerTranslator(final boolean allCaseSensitive) {
		caseSensitivity = allCaseSensitive ? (byte)0x0F : (byte)0x00;
		initSupportedFeatures();
	}

	/**
	 * Builds an SQLServerTranslator which will always translate in SQL
	 * identifiers with the defined case sensitivity.
	 *
	 * @param catalog	<code>true</code> to translate catalog names with double
	 *               	quotes (case sensitive in the DBMS),
	 *               	<code>false</code> otherwise.
	 * @param schema	<code>true</code> to translate schema names with double
	 *              	quotes (case sensitive in the DBMS),
	 *              	<code>false</code> otherwise.
	 * @param table		<code>true</code> to translate table names with double
	 *             		quotes (case sensitive in the DBMS),
	 *             		<code>false</code> otherwise.
	 * @param column	<code>true</code> to translate column names with double
	 *              	quotes (case sensitive in the DBMS),
	 *              	<code>false</code> otherwise.
	 */
	public SQLServerTranslator(final boolean catalog, final boolean schema, final boolean table, final boolean column) {
		caseSensitivity = IdentifierField.CATALOG.setCaseSensitive(caseSensitivity, catalog);
		caseSensitivity = IdentifierField.SCHEMA.setCaseSensitive(caseSensitivity, schema);
		caseSensitivity = IdentifierField.TABLE.setCaseSensitive(caseSensitivity, table);
		caseSensitivity = IdentifierField.COLUMN.setCaseSensitive(caseSensitivity, column);
		initSupportedFeatures();
	}

	/**
	 * Initialize the list of optional features supported by this translator.
	 *
	 * <p>
	 * 	By default, all optional features are supported except the following:
	 * </p>
	 * <ul>
	 * 	<li>All geometric functions,</li>
	 * 	<li>ILIKE,</li>
	 * 	<li>and IN_UNIT</li>
	 * </ul>
	 *
	 * @since 2.0
	 */
	protected void initSupportedFeatures() {
		// Support all features...
		supportedFeatures.supportAll();
		// ...except all geometries:
		supportedFeatures.unsupportAll(LanguageFeature.TYPE_ADQL_GEO);
		// ...except ILIKE:
		supportedFeatures.unsupport(ComparisonOperator.ILIKE.getFeatureDescription());
		// ...except IN_UNIT:
		supportedFeatures.unsupport(InUnitFunction.FEATURE);
	}

	@Override
	public final FeatureSet getSupportedFeatures() {
		return supportedFeatures;
	}

	@Override
	public boolean isCaseSensitive(final IdentifierField field) {
		return field != null && field.isCaseSensitive(caseSensitivity);
	}

	/**
	 * For SQL Server, {@link #translate(ClauseSelect)} must be overridden for
	 * LIMIT and OFFSET handling.
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	If an OFFSET is specified, TOP can no longer be used to specify a limit
	 * 	in SQL. In such case, TOP is replaced by FETCH NEXT right after the
	 * 	OFFSET instruction. Besides, SQLServer requires an ORDER BY clause in
	 * 	order to use OFFSET. If none is given in the ADQL query, the default
	 * 	<code>ORDER BY 1 ASC</code> (i.e. sort on the first column) is applied.
	 * </i></p>
	 */
	@Override
	public String translate(ADQLQuery query) throws TranslationException {
		StringBuffer sql = new StringBuffer();

		if (!query.getWith().isEmpty())
			sql.append(translate(query.getWith())).append('\n');

		// Start with the SELECT clause:
		/* NOTE: If a limit is specified, TOP should be used if no OFFSET is
		 *       used, otherwise the limit must be specified right after
		 *       OFFSET. */
		final boolean withOffset = (query.getOffset() != null && query.getOffset().getValue() > 0);
		sql.append(translate(query.getSelect(), !withOffset));

		sql.append("\nFROM ").append(translate(query.getFrom()));

		if (!query.getWhere().isEmpty())
			sql.append('\n').append(translate(query.getWhere()));

		if (!query.getGroupBy().isEmpty())
			sql.append('\n').append(translate(query.getGroupBy()));

		if (!query.getHaving().isEmpty())
			sql.append('\n').append(translate(query.getHaving()));

		if (!query.getOrderBy().isEmpty())
			sql.append('\n').append(translate(query.getOrderBy()));

		// Deal with OFFSET:
		if (withOffset) {

			// An ORDER BY is required by OFFSET ; so, ensure there is one:
			if (query.getOrderBy().isEmpty())
				sql.append("\nORDER BY 1 ASC"); // default: order on the 1st col

			// Append the OFFSET:
			sql.append("\nOFFSET ").append(query.getOffset().getValue()).append(" ROWS");

			// With OFFSET, the TOP/LIMIT must be expressed differently:
			if (query.hasLimit())
				sql.append(" FETCH NEXT ").append(query.getLimit()).append(" ROWS ONLY");
		}

		return sql.toString();
	}

	@Override
	public String translate(SetOperation set) throws TranslationException {
		StringBuffer sql = new StringBuffer();

		String tPrefix = "t" + System.currentTimeMillis() + "_";
		int tCnt = 1;

		if (!set.getWith().isEmpty())
			sql.append(translate(set.getWith())).append('\n');

		boolean extendedSetExp = (set.getLeftSet() instanceof SetOperation || !set.getLeftSet().getWith().isEmpty() || !set.getLeftSet().getOrderBy().isEmpty() || set.getLeftSet().getOffset() != null);
		if (extendedSetExp)
			sql.append("SELECT * FROM\n(");
		sql.append(translate(set.getLeftSet()));
		if (extendedSetExp)
			sql.append(") AS ").append(tPrefix + (tCnt++));
		sql.append('\n');

		sql.append(set.getOperation());
		if (set.isWithDuplicates())
			sql.append(" ALL");
		sql.append('\n');

		extendedSetExp = (set.getRightSet() instanceof SetOperation || !set.getRightSet().getWith().isEmpty() || !set.getRightSet().getOrderBy().isEmpty() || set.getRightSet().getOffset() != null);
		if (extendedSetExp)
			sql.append("SELECT * FROM\n(");
		sql.append(translate(set.getRightSet()));
		if (extendedSetExp)
			sql.append(") AS ").append(tPrefix + (tCnt++));

		if (!set.getOrderBy().isEmpty())
			sql.append('\n').append(translate(set.getOrderBy()));

		if (set.getOffset() != null)
			sql.append("\nOFFSET ").append(set.getOffset().getValue());

		return sql.toString();
	}

	/**
	 * This version of {@link #translate(ClauseSelect)} lets translate the given
	 * SELECT clause with or without the TOP instruction in case a limit is
	 * specified in ADQL.
	 *
	 * @param clause		The SELECT clause to translate.
	 * @param topAllowed	If <code>true</code> and a TOP limit is specified,
	 *                  	it will be translated as a TOP in SQL (exactly as
	 *                  	in ADQL).
	 *                  	If <code>false</code> and a TOP limit is specified,
	 *                  	it will never appear in the SQL translation. <i>(in
	 *                  	such case, {@link #translate(ADQLQuery)} will take
	 *                  	care of this limit)</i>
	 *
	 * @since 2.0 */
	protected String translate(final ClauseSelect clause, final boolean topAllowed) throws TranslationException {
		String sql = null;

		for(int i = 0; i < clause.size(); i++) {
			if (i == 0)
				sql = clause.getName() + (clause.distinctColumns() ? " DISTINCT" : "") + (topAllowed && clause.hasLimit() ? " TOP " + clause.getLimit() : "");
			else
				sql += " " + clause.getSeparator(i);

			sql += " " + translate(clause.get(i));
		}

		return sql;
	}

	/**
	 * Translate the given SELECT clause into an SQL compatible with
	 * MS-SQLServer.
	 *
	 * <p><i><b>Note:</b>
	 * 	The TOP limit, if any, will always be present in the SQL translation.
	 * </i></p>
	 *
	 * @see #translate(ClauseSelect, boolean)
	 */
	@Override
	public String translate(ClauseSelect clause) throws TranslationException {
		return translate(clause, true);
	}

	@Override
	public String translate(Comparison comp) throws TranslationException {
		switch(comp.getOperator()) {
			case ILIKE:
			case NOTILIKE:
				throw new TranslationException("Translation of ILIKE impossible! This is not supported natively in MS-SQL Server.");
			default:
				return translate(comp.getLeftOperand()) + " " + comp.getOperator().toADQL() + " " + translate(comp.getRightOperand());
		}
	}

	@Override
	public String translate(final InUnitFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(Concatenation concat) throws TranslationException {
		StringBuffer translated = new StringBuffer();

		for(ADQLOperand op : concat) {
			if (translated.length() > 0)
				translated.append(" + ");
			translated.append(translate(op));
		}

		return translated.toString();
	}

	@Override
	public String translate(final ADQLJoin join) throws TranslationException {
		StringBuilder sql = new StringBuilder(translate(join.getLeftTable()));

		sql.append(' ').append(join.getJoinType()).append(' ').append(translate(join.getRightTable())).append(' ');

		// CASE: NATURAL
		if (join.isNatural()) {
			try {
				StringBuilder buf = new StringBuilder();

				// Find duplicated items between the two lists and translate them as ON conditions:
				DBColumn rightCol;
				SearchColumnList leftList = join.getLeftTable().getDBColumns();
				SearchColumnList rightList = join.getRightTable().getDBColumns();
				for(DBColumn leftCol : leftList) {
					// search for at most one column with the same name in the RIGHT list
					// and throw an exception is there are several matches:
					rightCol = ADQLJoin.findAtMostOneColumn(leftCol.getADQLName(), (byte)0, rightList, false);
					// if there is one...
					if (rightCol != null) {
						// ...check there is only one column with this name in the LEFT list,
						// and throw an exception if it is not the case:
						ADQLJoin.findExactlyOneColumn(leftCol.getADQLName(), (byte)0, leftList, true);
						// ...append the corresponding join condition:
						if (buf.length() > 0)
							buf.append(" AND ");
						buf.append(translate(generateJoinColumn(join.getLeftTable(), leftCol, new ADQLColumn(leftCol.getADQLName()))));
						buf.append("=");
						buf.append(translate(generateJoinColumn(join.getRightTable(), rightCol, new ADQLColumn(rightCol.getADQLName()))));
					}
				}

				sql.append("ON ").append(buf.toString());
			} catch(UnresolvedJoinException uje) {
				throw new TranslationException("Impossible to resolve the NATURAL JOIN between " + join.getLeftTable().toADQL() + " and " + join.getRightTable().toADQL() + "!", uje);
			}
		}
		// CASE: USING
		else if (join.hasJoinedColumns()) {
			try {
				StringBuilder buf = new StringBuilder();

				// For each columns of usingList, check there is in each list exactly one matching column, and then, translate it as ON condition:
				DBColumn leftCol, rightCol;
				ADQLColumn usingCol;
				SearchColumnList leftList = join.getLeftTable().getDBColumns();
				SearchColumnList rightList = join.getRightTable().getDBColumns();
				Iterator<ADQLColumn> itCols = join.getJoinedColumns();
				while(itCols.hasNext()) {
					usingCol = itCols.next();
					// search for exactly one column with the same name in the LEFT list
					// and throw an exception if there is none, or if there are several matches:
					leftCol = ADQLJoin.findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), leftList, true);
					// item in the RIGHT list:
					rightCol = ADQLJoin.findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), rightList, false);
					// append the corresponding join condition:
					if (buf.length() > 0)
						buf.append(" AND ");
					buf.append(translate(generateJoinColumn(join.getLeftTable(), leftCol, usingCol)));
					buf.append("=");
					buf.append(translate(generateJoinColumn(join.getRightTable(), rightCol, usingCol)));
				}

				sql.append("ON ").append(buf.toString());
			} catch(UnresolvedJoinException uje) {
				throw new TranslationException("Impossible to resolve the JOIN USING between " + join.getLeftTable().toADQL() + " and " + join.getRightTable().toADQL() + "!", uje);
			}
		}
		// DEFAULT CASE:
		else if (join.getJoinCondition() != null)
			sql.append(translate(join.getJoinCondition()));

		return sql.toString();
	}

	/**
	 * Generate an ADQL column of the given table and with the given metadata.
	 *
	 * @param table			Parent table of the column to generate.
	 * @param colMeta		DB metadata of the column to generate.
	 * @param joinedColumn	The joined column (i.e. the ADQL column listed in a
	 *                   	USING) from which the generated column should
	 *                   	derive.
	 *                   	<i>If NULL, an {@link ADQLColumn} instance will be
	 *                   	created from scratch using the ADQL name of the
	 *                   	given DB metadata.</i>
	 *
	 * @return	The generated column.
	 */
	protected ADQLColumn generateJoinColumn(final FromContent table, final DBColumn colMeta, final ADQLColumn joinedColumn) {
		ADQLColumn newCol = (joinedColumn == null ? new ADQLColumn(colMeta.getADQLName()) : new ADQLColumn(joinedColumn));
		if (table != null) {
			if (table instanceof ADQLTable)
				newCol.setAdqlTable((ADQLTable)table);
			else
				newCol.setAdqlTable(new ADQLTable(table.getName()));
		}
		newCol.setDBLink(colMeta);
		return newCol;
	}

	@Override
	public String translate(final ExtractCoord extractCoord) throws TranslationException {
		return getDefaultADQLFunction(extractCoord);
	}

	@Override
	public String translate(final ExtractCoordSys extractCoordSys) throws TranslationException {
		return getDefaultADQLFunction(extractCoordSys);
	}

	@Override
	public String translate(final AreaFunction areaFunction) throws TranslationException {
		return getDefaultADQLFunction(areaFunction);
	}

	@Override
	public String translate(final CentroidFunction centroidFunction) throws TranslationException {
		return getDefaultADQLFunction(centroidFunction);
	}

	@Override
	public String translate(final DistanceFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(final ContainsFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(final IntersectsFunction fct) throws TranslationException {
		return getDefaultADQLFunction(fct);
	}

	@Override
	public String translate(final PointFunction point) throws TranslationException {
		return getDefaultADQLFunction(point);
	}

	@Override
	public String translate(final CircleFunction circle) throws TranslationException {
		return getDefaultADQLFunction(circle);
	}

	@Override
	public String translate(final BoxFunction box) throws TranslationException {
		return getDefaultADQLFunction(box);
	}

	@Override
	public String translate(final PolygonFunction polygon) throws TranslationException {
		return getDefaultADQLFunction(polygon);
	}

	@Override
	public String translate(MathFunction fct) throws TranslationException {
		switch(fct.getType()) {
			case TRUNCATE:
				// third argument to round nonzero means do a truncate
				if (fct.getNbParameters() >= 2)
					return "round(convert(float, " + translate(fct.getParameter(0)) + "), convert(float, " + translate(fct.getParameter(1)) + "), 1)";
				else
					return "round(convert(float, " + translate(fct.getParameter(0)) + "), 0, 1)";
			case MOD:
				return ((fct.getNbParameters() >= 2) ? ("convert(numeric, " + translate(fct.getParameter(0)) + ") % convert(numeric, " + translate(fct.getParameter(1)) + ")") : "");
			case ATAN2:
				return "ATN2(" + translate(fct.getParameter(0)) + ", " + translate(fct.getParameter(1)) + ")";

			/* In MS-SQLServer, the following functions returns a value of the
			 * same type as the given argument. However, ADQL requires that an
			 * SQLServer float (so a double in ADQL) is returned. So, in order
			 * to follow the ADQL standard, the given parameter must be
			 * converted into a float: */
			case ABS:
				return "abs(convert(float, " + translate(fct.getParameter(0)) + "))";
			case CEILING:
				return "ceiling(convert(float, " + translate(fct.getParameter(0)) + "))";
			case DEGREES:
				return "degrees(convert(float, " + translate(fct.getParameter(0)) + "))";
			case FLOOR:
				return "floor(convert(float, " + translate(fct.getParameter(0)) + "))";
			case RADIANS:
				return "radians(convert(float, " + translate(fct.getParameter(0)) + "))";
			case ROUND:
				if (fct.getNbParameters() >= 2)
					return "round(convert(float, " + translate(fct.getParameter(0)) + ")" + ", " + translate(fct.getParameter(1)) + ")";
				else
					return "round(convert(float, " + translate(fct.getParameter(0)) + ")" + ", 0)";

			default:
				return getDefaultADQLFunction(fct);
		}
	}

	/* ********************************************************************** */
	/* *                                                                    * */
	/* * TYPE MANAGEMENT                                                    * */
	/* *                                                                    * */
	/* ********************************************************************** */

	@Override
	public DBType convertTypeFromDB(final int dbmsType, final String rawDbmsTypeName, String dbmsTypeName, final String[] params) {
		// If no type is provided return VARCHAR:
		if (dbmsTypeName == null || dbmsTypeName.trim().length() == 0)
			return null;

		// Put the dbmsTypeName in lower case for the following comparisons:
		dbmsTypeName = dbmsTypeName.toLowerCase();

		// Extract the length parameter (always the first one):
		int lengthParam = DBType.NO_LENGTH;
		if (params != null && params.length > 0) {
			try {
				lengthParam = Integer.parseInt(params[0]);
			} catch(NumberFormatException nfe) {
			}
		}

		// SMALLINT
		if (dbmsTypeName.equals("smallint") || dbmsTypeName.equals("tinyint") || dbmsTypeName.equals("bit"))
			return new DBType(DBDatatype.SMALLINT);
		// INTEGER
		else if (dbmsTypeName.equals("int"))
			return new DBType(DBDatatype.INTEGER);
		// BIGINT
		else if (dbmsTypeName.equals("bigint") || dbmsTypeName.equals("unsigned bigint"))
			return new DBType(DBDatatype.BIGINT);
		// REAL (cf https://msdn.microsoft.com/fr-fr/library/ms173773(v=sql.120).aspx)
		else if (dbmsTypeName.equals("real") || (dbmsTypeName.equals("float") && lengthParam >= 1 && lengthParam <= 24))
			return new DBType(DBDatatype.REAL);
		// DOUBLE (cf https://msdn.microsoft.com/fr-fr/library/ms173773(v=sql.120).aspx)
		else if (dbmsTypeName.equals("float") || dbmsTypeName.equals("decimal") || dbmsTypeName.equals("numeric"))
			return new DBType(DBDatatype.DOUBLE);
		// BINARY
		else if (dbmsTypeName.equals("binary"))
			return new DBType(DBDatatype.BINARY, lengthParam);
		// VARBINARY
		else if (dbmsTypeName.equals("varbinary"))
			return new DBType(DBDatatype.VARBINARY, lengthParam);
		// CHAR
		else if (dbmsTypeName.equals("char") || dbmsTypeName.equals("nchar"))
			return new DBType(DBDatatype.CHAR, lengthParam);
		// VARCHAR
		else if (dbmsTypeName.equals("varchar") || dbmsTypeName.equals("nvarchar"))
			return new DBType(DBDatatype.VARCHAR, lengthParam);
		// BLOB
		else if (dbmsTypeName.equals("image"))
			return new DBType(DBDatatype.BLOB);
		// CLOB
		else if (dbmsTypeName.equals("text") || dbmsTypeName.equals("ntext"))
			return new DBType(DBDatatype.CLOB);
		// TIMESTAMP
		else if (dbmsTypeName.equals("timestamp") || dbmsTypeName.equals("datetime") || dbmsTypeName.equals("datetime2") || dbmsTypeName.equals("datetimeoffset") || dbmsTypeName.equals("smalldatetime") || dbmsTypeName.equals("time") || dbmsTypeName.equals("date") || dbmsTypeName.equals("date"))
			return new DBType(DBDatatype.TIMESTAMP);
		// Default:
		else
			return null;
	}

	@Override
	public String convertTypeToDB(final DBType type) {
		if (type == null)
			return "varchar";

		switch(type.type) {

			case SMALLINT:
			case REAL:
			case BIGINT:
			case CHAR:
			case VARCHAR:
			case BINARY:
			case VARBINARY:
				return type.type.toString().toLowerCase();

			case INTEGER:
				return "int";

			// (cf https://msdn.microsoft.com/fr-fr/library/ms173773(v=sql.120).aspx)
			case DOUBLE:
				return "float(53)";

			case TIMESTAMP:
				return "datetime";

			case BLOB:
				return "image";

			case CLOB:
				return "text";

			case POINT:
			case REGION:
			default:
				return "varchar";
		}
	}

	@Override
	public Region translateGeometryFromDB(final Object jdbcColValue) throws ParseException {
		throw new ParseException("Unsupported geometrical value! The value \"" + jdbcColValue + "\" can not be parsed as a region.");
	}

	@Override
	public Object translateGeometryToDB(final Region region) throws ParseException {
		throw new ParseException("Geometries can not be uploaded in the database in this implementation!");
	}

}
