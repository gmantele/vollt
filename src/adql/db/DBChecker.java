package adql.db;

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
 * Copyright 2011,2013-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                            Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import adql.db.STCS.CoordSys;
import adql.db.STCS.Region;
import adql.db.STCS.RegionType;
import adql.db.exception.UnresolvedColumnException;
import adql.db.exception.UnresolvedFunction;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.db.exception.UnresolvedTableException;
import adql.parser.ParseException;
import adql.parser.QueryChecker;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.query.ClauseSelect;
import adql.query.ColumnReference;
import adql.query.IdentifierField;
import adql.query.SelectAllColumns;
import adql.query.SelectItem;
import adql.query.from.ADQLTable;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.UnknownType;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.UserDefinedFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;
import adql.search.ISearchHandler;
import adql.search.SearchColumnHandler;
import adql.search.SimpleSearchHandler;

/**
 * This {@link QueryChecker} implementation is able to do the following verifications on an ADQL query:
 * <ol>
 * 	<li>Check the existence of all table and column references found in a query</li>
 * 	<li>Resolve all unknown functions as supported User Defined Functions (UDFs)</li>
 * 	<li>Check whether all used geometrical functions are supported</li>
 * 	<li>Check whether all used coordinate systems are supported</li>
 * 	<li>Check that types of columns and UDFs match with their context</li>
 * </ol>
 * 
 * <h3>Check tables and columns</h3>
 * <p>
 * 	In addition to check the existence of tables and columns referenced in the query,
 * 	this checked will also attach database metadata on these references ({@link ADQLTable}
 * 	and {@link ADQLColumn} instances when they are resolved.
 * </p>
 * 
 * <p>These information are:</p>
 * <ul>
 * 	<li>the corresponding {@link DBTable} or {@link DBColumn} (see getter and setter for DBLink in {@link ADQLTable} and {@link ADQLColumn})</li>
 * 	<li>the link between an {@link ADQLColumn} and its {@link ADQLTable}</li>
 * </ul>
 * 
 * <p><i><u>Note:</u>
 * 	Knowing DB metadata of {@link ADQLTable} and {@link ADQLColumn} is particularly useful for the translation of the ADQL query to SQL,
 * 	because the ADQL name of columns and tables can be replaced in SQL by their DB name, if different. This mapping is done automatically
 * 	by {@link adql.translator.JDBCTranslator}.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public class DBChecker implements QueryChecker {

	/** List of all available tables ({@link DBTable}). */
	protected SearchTableList lstTables;

	/** <p>List of all allowed geometrical functions (i.e. CONTAINS, REGION, POINT, COORD2, ...).</p>
	 * <p>
	 * 	If this list is NULL, all geometrical functions are allowed.
	 * 	However, if not, all items of this list must be the only allowed geometrical functions.
	 * 	So, if the list is empty, no such function is allowed.
	 * </p>
	 * @since 1.3 */
	protected String[] allowedGeo = null;

	/** <p>List of all allowed coordinate systems.</p>
	 * <p>
	 * 	Each item of this list must be of the form: "{frame} {refpos} {flavor}".
	 * 	Each of these 3 items can be either of value, a list of values expressed with the syntax "({value1}|{value2}|...)"
	 * 	or a '*' to mean all possible values.
	 * </p>
	 * <p><i>Note: since a default value (corresponding to the empty string - '') should always be possible for each part of a coordinate system,
	 * the checker will always add the default value (UNKNOWNFRAME, UNKNOWNREFPOS or SPHERICAL2) into the given list of possible values for each coord. sys. part.</i></p>
	 * <p>
	 * 	If this list is NULL, all coordinates systems are allowed.
	 * 	However, if not, all items of this list must be the only allowed coordinate systems.
	 * 	So, if the list is empty, none is allowed.
	 * </p>
	 * @since 1.3 */
	protected String[] allowedCoordSys = null;

	/** <p>A regular expression built using the list of allowed coordinate systems.
	 * With this regex, it is possible to known whether a coordinate system expression is allowed or not.</p>
	 * <p>If NULL, all coordinate systems are allowed.</p>
	 * @since 1.3 */
	protected String coordSysRegExp = null;

	/** <p>List of all allowed User Defined Functions (UDFs).</p>
	 * <p>
	 * 	If this list is NULL, any encountered UDF will be allowed.
	 * 	However, if not, all items of this list must be the only allowed UDFs.
	 * 	So, if the list is empty, no UDF is allowed.
	 * </p>
	 * @since 1.3 */
	protected FunctionDef[] allowedUdfs = null;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * <p>Builds a {@link DBChecker} with an empty list of tables.</p>
	 * 
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:            <b>NO <i>(even unknown or fake tables and columns are allowed)</i></b></li>
	 * 	<li>Existence of User Defined Functions (UDFs): <b>NO <i>(any "unknown" function is allowed)</i></b></li>
	 * 	<li>Support of geometrical functions:           <b>NO <i>(all valid geometrical functions are allowed)</i></b></li>
	 * 	<li>Support of coordinate systems:              <b>NO <i>(all valid coordinate systems are allowed)</i></b></li>
	 * </ul>
	 */
	public DBChecker(){
		this(null, null);
	}

	/**
	 * <p>Builds a {@link DBChecker} with the given list of known tables.</p>
	 * 
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:            <b>OK</b></li>
	 * 	<li>Existence of User Defined Functions (UDFs): <b>NO <i>(any "unknown" function is allowed)</i></b></li>
	 * 	<li>Support of geometrical functions:           <b>NO <i>(all valid geometrical functions are allowed)</i></b></li>
	 * 	<li>Support of coordinate systems:              <b>NO <i>(all valid coordinate systems are allowed)</i></b></li>
	 * </ul>
	 * 
	 * @param tables	List of all available tables.
	 */
	public DBChecker(final Collection<? extends DBTable> tables){
		this(tables, null);
	}

	/**
	 * <p>Builds a {@link DBChecker} with the given list of known tables and with a restricted list of user defined functions.</p>
	 * 
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:            <b>OK</b></li>
	 * 	<li>Existence of User Defined Functions (UDFs): <b>OK</b></li>
	 * 	<li>Support of geometrical functions:           <b>NO <i>(all valid geometrical functions are allowed)</i></b></li>
	 * 	<li>Support of coordinate systems:              <b>NO <i>(all valid coordinate systems are allowed)</i></b></li>
	 * </ul>
	 * 
	 * @param tables		List of all available tables.
	 * @param allowedUdfs	List of all allowed user defined functions.
	 *                   	If NULL, no verification will be done (and so, all UDFs are allowed).
	 *                   	If empty list, no "unknown" (or UDF) is allowed.
	 *                   	<i>Note: match with items of this list are done case insensitively.</i>
	 * 
	 * @since 1.3
	 */
	public DBChecker(final Collection<? extends DBTable> tables, final Collection<? extends FunctionDef> allowedUdfs){
		// Sort and store the given tables:
		setTables(tables);

		Object[] tmp;
		int cnt;

		// Store all allowed UDFs in a sorted array:
		if (allowedUdfs != null){
			// Remove all NULL and empty strings:
			tmp = new FunctionDef[allowedUdfs.size()];
			cnt = 0;
			for(FunctionDef udf : allowedUdfs){
				if (udf != null && udf.name.trim().length() > 0)
					tmp[cnt++] = udf;
			}
			// make a copy of the array:
			this.allowedUdfs = Arrays.copyOf(tmp, cnt, FunctionDef[].class);
			tmp = null;
			// sort the values:
			Arrays.sort(this.allowedUdfs);
		}
	}

	/**
	 * <p>Builds a {@link DBChecker} with the given list of known tables and with a restricted list of user defined functions.</p>
	 * 
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:            <b>OK</b></li>
	 * 	<li>Existence of User Defined Functions (UDFs): <b>NO <i>(any "unknown" function is allowed)</i></b></li>
	 * 	<li>Support of geometrical functions:           <b>OK</b></li>
	 * 	<li>Support of coordinate systems:              <b>OK</b></li>
	 * </ul>
	 * 
	 * @param tables			List of all available tables.
	 * @param allowedGeoFcts	List of all allowed geometrical functions (i.e. CONTAINS, POINT, UNION, CIRCLE, COORD1).
	 *                      	If NULL, no verification will be done (and so, all geometries are allowed).
	 *                      	If empty list, no geometry function is allowed.
	 *                      	<i>Note: match with items of this list are done case insensitively.</i>
	 * @param allowedCoordSys	List of all allowed coordinate system patterns. The syntax of a such pattern is the following:
	 *                       	"{frame} {refpos} {flavor}" ; on the contrary to a coordinate system expression, here no part is optional.
	 *                       	Each part of this pattern can be one the possible values (case insensitive), a list of possible values
	 *                       	expressed with the syntax "({value1}|{value2}|...)", or a '*' for any valid value.
	 *                       	For instance: "ICRS (GEOCENTER|heliocenter) *".
	 *                       	If the given list is NULL, no verification will be done (and so, all coordinate systems are allowed).
	 *                       	If it is empty, no coordinate system is allowed (except the default values - generally expressed by an empty string: '').
	 * 
	 * @since 1.3
	 */
	public DBChecker(final Collection<? extends DBTable> tables, final Collection<String> allowedGeoFcts, final Collection<String> allowedCoordSys) throws ParseException{
		this(tables, null, allowedGeoFcts, allowedCoordSys);
	}

	/**
	 * <p>Builds a {@link DBChecker}.</p>
	 * 
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:            <b>OK</b></li>
	 * 	<li>Existence of User Defined Functions (UDFs): <b>OK</b></li>
	 * 	<li>Support of geometrical functions:           <b>OK</b></li>
	 * 	<li>Support of coordinate systems:              <b>OK</b></li>
	 * </ul>
	 * 
	 * @param tables			List of all available tables.
	 * @param allowedUdfs		List of all allowed user defined functions.
	 *                   		If NULL, no verification will be done (and so, all UDFs are allowed).
	 *                   		If empty list, no "unknown" (or UDF) is allowed.
	 *                   		<i>Note: match with items of this list are done case insensitively.</i>
	 * @param allowedGeoFcts	List of all allowed geometrical functions (i.e. CONTAINS, POINT, UNION, CIRCLE, COORD1).
	 *                      	If NULL, no verification will be done (and so, all geometries are allowed).
	 *                      	If empty list, no geometry function is allowed.
	 *                      	<i>Note: match with items of this list are done case insensitively.</i>
	 * @param allowedCoordSys	List of all allowed coordinate system patterns. The syntax of a such pattern is the following:
	 *                       	"{frame} {refpos} {flavor}" ; on the contrary to a coordinate system expression, here no part is optional.
	 *                       	Each part of this pattern can be one the possible values (case insensitive), a list of possible values
	 *                       	expressed with the syntax "({value1}|{value2}|...)", or a '*' for any valid value.
	 *                       	For instance: "ICRS (GEOCENTER|heliocenter) *".
	 *                       	If the given list is NULL, no verification will be done (and so, all coordinate systems are allowed).
	 *                       	If it is empty, no coordinate system is allowed (except the default values - generally expressed by an empty string: '').
	 * 
	 * @since 1.3
	 */
	public DBChecker(final Collection<? extends DBTable> tables, final Collection<? extends FunctionDef> allowedUdfs, final Collection<String> allowedGeoFcts, final Collection<String> allowedCoordSys) throws ParseException{
		// Set the list of available tables + Set the list of all known UDFs:
		this(tables, allowedUdfs);

		// Set the list of allowed geometrical functions:
		allowedGeo = specialSort(allowedGeoFcts);

		// Set the list of allowed coordinate systems:
		this.allowedCoordSys = specialSort(allowedCoordSys);
		coordSysRegExp = STCS.buildCoordSysRegExp(this.allowedCoordSys);
	}

	/**
	 * Transform the given collection of string elements in a sorted array.
	 * Only non-NULL and non-empty strings are kept.
	 * 
	 * @param items	Items to copy and sort.
	 * 
	 * @return	A sorted array containing all - except NULL and empty strings - items of the given collection.
	 * 
	 * @since 1.3
	 */
	protected final static String[] specialSort(final Collection<String> items){
		// Nothing to do if the array is NULL:
		if (items == null)
			return null;

		// Keep only valid items (not NULL and not empty string):
		String[] tmp = new String[items.size()];
		int cnt = 0;
		for(String item : items){
			if (item != null && item.trim().length() > 0)
				tmp[cnt++] = item;
		}

		// Make an adjusted array copy:
		String[] copy = Arrays.copyOf(tmp, cnt);

		// Sort the values:
		Arrays.sort(copy);

		return copy;
	}

	/* ****** */
	/* SETTER */
	/* ****** */
	/**
	 * <p>Sets the list of all available tables.</p>
	 * 
	 * <p><i><u>Note:</u>
	 * 	Only if the given collection is NOT an instance of {@link SearchTableList},
	 * 	the collection will be copied inside a new {@link SearchTableList}, otherwise it is used as provided.
	 * </i></p>
	 * 
	 * @param tables	List of {@link DBTable}s.
	 */
	public final void setTables(final Collection<? extends DBTable> tables){
		if (tables == null)
			lstTables = new SearchTableList();
		else if (tables instanceof SearchTableList)
			lstTables = (SearchTableList)tables;
		else
			lstTables = new SearchTableList(tables);
	}

	/* ************* */
	/* CHECK METHODS */
	/* ************* */
	/**
	 * <p>Check all the columns, tables and UDFs references inside the given query.</p>
	 * 
	 * <p><i>
	 * 	<u>Note:</u> This query has already been parsed ; thus it is already syntactically correct.
	 * 	Only the consistency with the published tables, columns and all the defined UDFs must be checked.
	 * </i></p>
	 * 
	 * @param query		The query to check.
	 * 
	 * @throws ParseException	An {@link UnresolvedIdentifiersException} if some tables or columns can not be resolved.
	 * 
	 * @see #check(ADQLQuery, Stack)
	 */
	@Override
	public final void check(final ADQLQuery query) throws ParseException{
		check(query, null);
	}

	/**
	 * <p>Process several (semantic) verifications in the given ADQL query.</p>
	 * 
	 * <p>Main verifications done in this function:</p>
	 * <ol>
	 * 	<li>Existence of DB items (tables and columns)</li>
	 * 	<li>Semantic verification of sub-queries</li>
	 * 	<li>Support of every encountered User Defined Functions (UDFs - functions unknown by the syntactic parser)</li>
	 * 	<li>Support of every encountered geometries (functions, coordinate systems and STC-S expressions)</li>
	 * 	<li>Consistency of types still unknown (because the syntactic parser could not yet resolve them)</li>
	 * </ol>
	 * 
	 * @param query			The query to check.
	 * @param fathersList	List of all columns available in the father queries and that should be accessed in sub-queries.
	 *                   	Each item of this stack is a list of columns available in each father-level query.
	 *                   	<i>Note: this parameter is NULL if this function is called with the root/father query as parameter.</i>
	 * 
	 * @throws UnresolvedIdentifiersException	An {@link UnresolvedIdentifiersException} if one or several of the above listed tests have detected
	 *                                       	some semantic errors (i.e. unresolved table, columns, function).
	 * 
	 * @since 1.2
	 * 
	 * @see #checkDBItems(ADQLQuery, Stack, UnresolvedIdentifiersException)
	 * @see #checkSubQueries(ADQLQuery, Stack, SearchColumnList, UnresolvedIdentifiersException)
	 * @see #checkUDFs(ADQLQuery, UnresolvedIdentifiersException)
	 * @see #checkGeometries(ADQLQuery, UnresolvedIdentifiersException)
	 * @see #checkTypes(ADQLQuery, UnresolvedIdentifiersException)
	 */
	protected void check(final ADQLQuery query, final Stack<SearchColumnList> fathersList) throws UnresolvedIdentifiersException{
		UnresolvedIdentifiersException errors = new UnresolvedIdentifiersException();

		// A. Check DB items (tables and columns):
		SearchColumnList availableColumns = checkDBItems(query, fathersList, errors);

		// B. Check UDFs:
		if (allowedUdfs != null)
			checkUDFs(query, errors);

		// C. Check geometries:
		checkGeometries(query, errors);

		// D. Check types:
		checkTypes(query, errors);

		// E. Check sub-queries:
		checkSubQueries(query, fathersList, availableColumns, errors);

		// Throw all errors, if any:
		if (errors.getNbErrors() > 0)
			throw errors;
	}

	/* ************************************************ */
	/* CHECKING METHODS FOR DB ITEMS (TABLES & COLUMNS) */
	/* ************************************************ */

	/**
	 * <p>Check DB items (tables and columns) used in the given ADQL query.</p>
	 * 
	 * <p>Operations done in this function:</p>
	 * <ol>
	 * 	<li>Resolve all found tables</li>
	 * 	<li>Get the whole list of all available columns <i>Note: this list is returned by this function.</i></li>
	 * 	<li>Resolve all found columns</li>
	 * </ol>
	 * 
	 * @param query			Query in which the existence of DB items must be checked.
	 * @param fathersList	List of all columns available in the father queries and that should be accessed in sub-queries.
	 *                   	Each item of this stack is a list of columns available in each father-level query.
	 *                   	<i>Note: this parameter is NULL if this function is called with the root/father query as parameter.</i>
	 * @param errors		List of errors to complete in this function each time an unknown table or column is encountered.
	 * 
	 * @return	List of all columns available in the given query.
	 * 
	 * @see #resolveTables(ADQLQuery, Stack, UnresolvedIdentifiersException)
	 * @see FromContent#getDBColumns()
	 * @see #resolveColumns(ADQLQuery, Stack, Map, SearchColumnList, UnresolvedIdentifiersException)
	 * 
	 * @since 1.3
	 */
	protected SearchColumnList checkDBItems(final ADQLQuery query, final Stack<SearchColumnList> fathersList, final UnresolvedIdentifiersException errors){
		// a. Resolve all tables:
		Map<DBTable,ADQLTable> mapTables = resolveTables(query, fathersList, errors);

		// b. Get the list of all columns made available in the clause FROM:
		SearchColumnList availableColumns;
		try{
			availableColumns = query.getFrom().getDBColumns();
		}catch(ParseException pe){
			errors.addException(pe);
			availableColumns = new SearchColumnList();
		}

		// c. Resolve all columns:
		resolveColumns(query, fathersList, mapTables, availableColumns, errors);

		return availableColumns;
	}

	/**
	 * <p>Search all table references inside the given query, resolve them against the available tables, and if there is only one match,
	 * attach the matching metadata to them.</p>
	 * 
	 * <b>Management of sub-query tables</b>
	 * <p>
	 * 	If a table is not a DB table reference but a sub-query, this latter is first checked (using {@link #check(ADQLQuery, Stack)} ;
	 * 	but the father list must not contain tables of the given query, because on the same level) and then corresponding table metadata
	 * 	are generated (using {@link #generateDBTable(ADQLQuery, String)}) and attached to it.
	 * </p>
	 * 
	 * <b>Management of "{table}.*" in the SELECT clause</b>
	 * <p>
	 * 	For each of this SELECT item, this function tries to resolve the table name. If only one match is found, the corresponding ADQL table object
	 * 	is got from the list of resolved tables and attached to this SELECT item (thus, the joker item will also have the good metadata,
	 * 	particularly if the referenced table is a sub-query).
	 * </p>
	 * 
	 * @param query			Query in which the existence of tables must be checked.
	 * @param fathersList	List of all columns available in the father queries and that should be accessed in sub-queries.
	 *                      Each item of this stack is a list of columns available in each father-level query.
	 *                   	<i>Note: this parameter is NULL if this function is called with the root/father query as parameter.</i>
	 * @param errors		List of errors to complete in this function each time an unknown table or column is encountered.
	 * 
	 * @return	An associative map of all the resolved tables.
	 */
	protected Map<DBTable,ADQLTable> resolveTables(final ADQLQuery query, final Stack<SearchColumnList> fathersList, final UnresolvedIdentifiersException errors){
		HashMap<DBTable,ADQLTable> mapTables = new HashMap<DBTable,ADQLTable>();
		ISearchHandler sHandler;

		// Check the existence of all tables:
		sHandler = new SearchTableHandler();
		sHandler.search(query.getFrom());
		for(ADQLObject result : sHandler){
			try{
				ADQLTable table = (ADQLTable)result;

				// resolve the table:
				DBTable dbTable = null;
				if (table.isSubQuery()){
					// check the sub-query tables:
					check(table.getSubQuery(), fathersList);
					// generate its DBTable:
					dbTable = generateDBTable(table.getSubQuery(), table.getAlias());
				}else{
					dbTable = resolveTable(table);
					if (table.hasAlias())
						dbTable = dbTable.copy(null, table.getAlias());
				}

				// link with the matched DBTable:
				table.setDBLink(dbTable);
				mapTables.put(dbTable, table);
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}

		// Attach table information on wildcards with the syntax "{tableName}.*" of the SELECT clause:
		/* Note: no need to check the table name among the father tables, because there is
		 *       no interest to select a father column in a sub-query
		 *       (which can return only one column ; besides, no aggregate is allowed
		 *       in sub-queries).*/
		sHandler = new SearchWildCardHandler();
		sHandler.search(query.getSelect());
		for(ADQLObject result : sHandler){
			try{
				SelectAllColumns wildcard = (SelectAllColumns)result;
				ADQLTable table = wildcard.getAdqlTable();
				DBTable dbTable = null;

				// first, try to resolve the table by table alias:
				if (table.getTableName() != null && table.getSchemaName() == null){
					ArrayList<ADQLTable> tables = query.getFrom().getTablesByAlias(table.getTableName(), table.isCaseSensitive(IdentifierField.TABLE));
					if (tables.size() == 1)
						dbTable = tables.get(0).getDBLink();
				}

				// then try to resolve the table reference by table name:
				if (dbTable == null)
					dbTable = resolveTable(table);

				// set the corresponding tables among the list of resolved tables:
				wildcard.setAdqlTable(mapTables.get(dbTable));
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}

		return mapTables;
	}

	/**
	 * Resolve the given table, that's to say search for the corresponding {@link DBTable}.
	 * 
	 * @param table	The table to resolve.
	 * 
	 * @return		The corresponding {@link DBTable} if found, <i>null</i> otherwise.
	 * 
	 * @throws ParseException	An {@link UnresolvedTableException} if the given table can't be resolved.
	 */
	protected DBTable resolveTable(final ADQLTable table) throws ParseException{
		ArrayList<DBTable> tables = lstTables.search(table);

		// good if only one table has been found:
		if (tables.size() == 1)
			return tables.get(0);
		// but if more than one: ambiguous table name !
		else if (tables.size() > 1)
			throw new UnresolvedTableException(table, tables.get(0).getADQLSchemaName() + "." + tables.get(0).getADQLName(), tables.get(1).getADQLSchemaName() + "." + tables.get(1).getADQLName());
		// otherwise (no match): unknown table !
		else
			throw new UnresolvedTableException(table);
	}

	/**
	 * <p>Search all column references inside the given query, resolve them thanks to the given tables' metadata,
	 * and if there is only one match, attach the matching metadata to them.</p>
	 * 
	 * <b>Management of selected columns' references</b>
	 * <p>
	 * 	A column reference is not only a direct reference to a table column using a column name.
	 * 	It can also be a reference to an item of the SELECT clause (which will then call a "selected column").
	 * 	That kind of reference can be either an index (an unsigned integer starting from 1 to N, where N is the
	 * 	number selected columns), or the name/alias of the column.
	 * </p>
	 * <p>
	 * 	These references are also checked, in a second step, in this function. Thus, column metadata are
	 * 	also attached to them, as common columns.
	 * </p>
	 * 
	 * @param query			Query in which the existence of tables must be checked.
	 * @param fathersList	List of all columns available in the father queries and that should be accessed in sub-queries.
	 *                      Each item of this stack is a list of columns available in each father-level query.
	 *                   	<i>Note: this parameter is NULL if this function is called with the root/father query as parameter.</i>
	 * @param mapTables		List of all resolved tables.
	 * @param list			List of column metadata to complete in this function each time a column reference is resolved.
	 * @param errors		List of errors to complete in this function each time an unknown table or column is encountered.
	 */
	protected void resolveColumns(final ADQLQuery query, final Stack<SearchColumnList> fathersList, final Map<DBTable,ADQLTable> mapTables, final SearchColumnList list, final UnresolvedIdentifiersException errors){
		ISearchHandler sHandler;

		// Check the existence of all columns:
		sHandler = new SearchColumnHandler();
		sHandler.search(query);
		for(ADQLObject result : sHandler){
			try{
				ADQLColumn adqlColumn = (ADQLColumn)result;
				// resolve the column:
				DBColumn dbColumn = resolveColumn(adqlColumn, list, fathersList);
				// link with the matched DBColumn:
				adqlColumn.setDBLink(dbColumn);
				adqlColumn.setAdqlTable(mapTables.get(dbColumn.getTable()));
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}

		// Check the correctness of all column references (= references to selected columns):
		/* Note: no need to provide the father tables when resolving column references,
		 *       because no father column can be used in ORDER BY and/or GROUP BY. */
		sHandler = new SearchColReferenceHandler();
		sHandler.search(query);
		ClauseSelect select = query.getSelect();
		for(ADQLObject result : sHandler){
			try{
				ColumnReference colRef = (ColumnReference)result;
				// resolve the column reference:
				DBColumn dbColumn = checkColumnReference(colRef, select, list);
				// link with the matched DBColumn:
				colRef.setDBLink(dbColumn);
				if (dbColumn != null)
					colRef.setAdqlTable(mapTables.get(dbColumn.getTable()));
			}catch(ParseException pe){
				errors.addException(pe);
			}
		}
	}

	/**
	 * <p>Resolve the given column, that's to say search for the corresponding {@link DBColumn}.</p>
	 * 
	 * <p>
	 * 	The third parameter is used only if this function is called inside a sub-query. In this case,
	 * 	the column is tried to be resolved with the first list (dbColumns). If no match is found,
	 * 	the resolution is tried with the father columns list (fathersList).
	 * </p>
	 * 
	 * @param column		The column to resolve.
	 * @param dbColumns		List of all available {@link DBColumn}s.
	 * @param fathersList	List of all columns available in the father queries and that should be accessed in sub-queries.
	 *                      Each item of this stack is a list of columns available in each father-level query.
	 *                   	<i>Note: this parameter is NULL if this function is called with the root/father query as parameter.</i>
	 * 
	 * @return 				The corresponding {@link DBColumn} if found. Otherwise an exception is thrown.
	 * 
	 * @throws ParseException	An {@link UnresolvedColumnException} if the given column can't be resolved
	 * 							or an {@link UnresolvedTableException} if its table reference can't be resolved.
	 */
	protected DBColumn resolveColumn(final ADQLColumn column, final SearchColumnList dbColumns, Stack<SearchColumnList> fathersList) throws ParseException{
		ArrayList<DBColumn> foundColumns = dbColumns.search(column);

		// good if only one column has been found:
		if (foundColumns.size() == 1)
			return foundColumns.get(0);
		// but if more than one: ambiguous table reference !
		else if (foundColumns.size() > 1){
			if (column.getTableName() == null)
				throw new UnresolvedColumnException(column, (foundColumns.get(0).getTable() == null) ? "<NULL>" : (foundColumns.get(0).getTable().getADQLName() + "." + foundColumns.get(0).getADQLName()), (foundColumns.get(1).getTable() == null) ? "<NULL>" : (foundColumns.get(1).getTable().getADQLName() + "." + foundColumns.get(1).getADQLName()));
			else
				throw new UnresolvedTableException(column, (foundColumns.get(0).getTable() == null) ? "<NULL>" : foundColumns.get(0).getTable().getADQLName(), (foundColumns.get(1).getTable() == null) ? "<NULL>" : foundColumns.get(1).getTable().getADQLName());
		}// otherwise (no match): unknown column !
		else{
			if (fathersList == null || fathersList.isEmpty())
				throw new UnresolvedColumnException(column);
			else{
				Stack<SearchColumnList> subStack = new Stack<SearchColumnList>();
				subStack.addAll(fathersList.subList(0, fathersList.size() - 1));
				return resolveColumn(column, fathersList.peek(), subStack);
			}
		}
	}

	/**
	 * Check whether the given column reference corresponds to a selected item (column or an expression with an alias)
	 * or to an existing column.
	 * 
	 * @param colRef		The column reference which must be checked.
	 * @param select		The SELECT clause of the ADQL query.
	 * @param dbColumns		The list of all available columns.
	 * 
	 * @return 		The corresponding {@link DBColumn} if this reference is actually the name of a column, <i>null</i> otherwise.
	 * 
	 * @throws ParseException	An {@link UnresolvedColumnException} if the given column can't be resolved
	 * 							or an {@link UnresolvedTableException} if its table reference can't be resolved.
	 * 
	 * @see ClauseSelect#searchByAlias(String)
	 * @see #resolveColumn(ADQLColumn, SearchColumnList, Stack)
	 */
	protected DBColumn checkColumnReference(final ColumnReference colRef, final ClauseSelect select, final SearchColumnList dbColumns) throws ParseException{
		if (colRef.isIndex()){
			int index = colRef.getColumnIndex();
			if (index > 0 && index <= select.size()){
				SelectItem item = select.get(index - 1);
				if (item.getOperand() instanceof ADQLColumn)
					return ((ADQLColumn)item.getOperand()).getDBLink();
				else
					return null;
			}else
				throw new ParseException("Column index out of bounds: " + index + " (must be between 1 and " + select.size() + ") !", colRef.getPosition());
		}else{
			ADQLColumn col = new ADQLColumn(colRef.getColumnName());
			col.setCaseSensitive(colRef.isCaseSensitive());
			col.setPosition(colRef.getPosition());

			// search among the select_item aliases:
			if (col.getTableName() == null){
				ArrayList<SelectItem> founds = select.searchByAlias(colRef.getColumnName(), colRef.isCaseSensitive());
				if (founds.size() == 1)
					return null;
				else if (founds.size() > 1)
					throw new UnresolvedColumnException(col, founds.get(0).getAlias(), founds.get(1).getAlias());
			}

			// check the corresponding column:
			return resolveColumn(col, dbColumns, null);
		}
	}

	/**
	 * Generate a {@link DBTable} corresponding to the given sub-query with the given table name.
	 * This {@link DBTable} will contain all {@link DBColumn} returned by {@link ADQLQuery#getResultingColumns()}.
	 * 
	 * @param subQuery	Sub-query in which the specified table must be searched.
	 * @param tableName	Name of the table to search.
	 * 
	 * @return	The corresponding {@link DBTable} if the table has been found in the given sub-query, <i>null</i> otherwise.
	 * 
	 * @throws ParseException	Can be used to explain why the table has not been found. <i>Note: not used by default.</i>
	 */
	public static DBTable generateDBTable(final ADQLQuery subQuery, final String tableName) throws ParseException{
		DefaultDBTable dbTable = new DefaultDBTable(tableName);

		DBColumn[] columns = subQuery.getResultingColumns();
		for(DBColumn dbCol : columns)
			dbTable.addColumn(dbCol.copy(dbCol.getADQLName(), dbCol.getADQLName(), dbTable));

		return dbTable;
	}

	/* ************************* */
	/* CHECKING METHODS FOR UDFs */
	/* ************************* */

	/**
	 * <p>Search all UDFs (User Defined Functions) inside the given query, and then
	 * check their signature against the list of allowed UDFs.</p>
	 * 
	 * <p><i>Note:
	 * 	When more than one allowed function match, the function is considered as correct
	 * 	and no error is added.
	 * 	However, in case of multiple matches, the return type of matching functions could
	 * 	be different and in this case, there would be an error while checking later
	 * 	the types. In such case, throwing an error could make sense, but the user would
	 * 	then need to cast some parameters to help the parser identifying the right function.
	 * 	But the type-casting ability is not yet possible in ADQL.
	 * </i></p>
	 * 
	 * @param query		Query in which UDFs must be checked.
	 * @param errors	List of errors to complete in this function each time a UDF does not match to any of the allowed UDFs.
	 * 
	 * @since 1.3
	 */
	protected void checkUDFs(final ADQLQuery query, final UnresolvedIdentifiersException errors){
		// Search all UDFs:
		ISearchHandler sHandler = new SearchUDFHandler();
		sHandler.search(query);

		// If no UDF are allowed, throw immediately an error:
		if (allowedUdfs.length == 0){
			for(ADQLObject result : sHandler)
				errors.addException(new UnresolvedFunction((UserDefinedFunction)result));
		}
		// Otherwise, try to resolve all of them:
		else{
			ArrayList<UserDefinedFunction> toResolveLater = new ArrayList<UserDefinedFunction>();
			UserDefinedFunction udf;
			int match;
			BinarySearch<FunctionDef,UserDefinedFunction> binSearch = new BinarySearch<FunctionDef,UserDefinedFunction>(){
				@Override
				protected int compare(UserDefinedFunction searchItem, FunctionDef arrayItem){
					return arrayItem.compareTo(searchItem) * -1;
				}
			};

			// Try to resolve all the found UDFs:
			/* Note: at this stage, it can happen that UDFs can not be yet resolved because the building of
			 *       their signature depends of other UDFs. That's why, these special cases should be kept
			 *       for a later resolution try. */
			for(ADQLObject result : sHandler){
				udf = (UserDefinedFunction)result;
				// search for a match:
				match = binSearch.search(udf, allowedUdfs);
				// if no match...
				if (match < 0){
					// ...if the type of all parameters is resolved, add an error (no match is possible):
					if (isAllParamTypesResolved(udf))
						errors.addException(new UnresolvedFunction(udf));	// TODO Add the ADQLOperand position!
					// ...otherwise, try to resolved it later (when other UDFs will be mostly resolved):
					else
						toResolveLater.add(udf);
				}
				// if there is a match, metadata may be attached (particularly if the function is built automatically by the syntactic parser):
				else if (udf instanceof DefaultUDF)
					((DefaultUDF)udf).setDefinition(allowedUdfs[match]);
			}

			// Try to resolve UDFs whose some parameter types are depending of other UDFs:
			for(int i = 0; i < toResolveLater.size(); i++){
				udf = toResolveLater.get(i);
				// search for a match:
				match = binSearch.search(udf, allowedUdfs);
				// if no match, add an error:
				if (match < 0)
					errors.addException(new UnresolvedFunction(udf));	// TODO Add the ADQLOperand position!
				// otherwise, metadata may be attached (particularly if the function is built automatically by the syntactic parser):
				else if (udf instanceof DefaultUDF)
					((DefaultUDF)udf).setDefinition(allowedUdfs[match]);
			}
		}
	}

	/**
	 * <p>Tell whether the type of all parameters of the given ADQL function
	 * is resolved.</p>
	 * 
	 * <p>A parameter type may not be resolved for 2 main reasons:</p>
	 * <ul>
	 * 	<li>the parameter is a <b>column</b>, but this column has not been successfully resolved. Thus its type is still unknown.</li>
	 * 	<li>the parameter is a <b>UDF</b>, but this UDF has not been already resolved. Thus, as for the column, its return type is still unknown.
	 * 		But it could be known later if the UDF is resolved later ; a second try should be done afterwards.</li>
	 * </ul>
	 * 
	 * @param fct	ADQL function whose the parameters' type should be checked.
	 * 
	 * @return	<i>true</i> if the type of all parameters is known, <i>false</i> otherwise.
	 * 
	 * @since 1.3
	 */
	protected final boolean isAllParamTypesResolved(final ADQLFunction fct){
		for(ADQLOperand op : fct.getParameters()){
			if (op.isNumeric() == op.isString())
				return false;
		}
		return true;
	}

	/* ************************************************************************************************* */
	/* METHODS CHECKING THE GEOMETRIES (geometrical functions, coordinate systems and STC-S expressions) */
	/* ************************************************************************************************* */

	/**
	 * <p>Check all geometries.</p>
	 * 
	 * <p>Operations done in this function:</p>
	 * <ol>
	 * 	<li>Check that all geometrical functions are supported</li>
	 * 	<li>Check that all explicit (string constant) coordinate system definitions are supported</i></li>
	 * 	<li>Check all STC-S expressions (only in {@link RegionFunction} for the moment) and
	 * 	    Apply the 2 previous checks on them</li>
	 * </ol>
	 * 
	 * @param query		Query in which geometries must be checked.
	 * @param errors	List of errors to complete in this function each time a geometry item is not supported.
	 * 
	 * @see #resolveGeometryFunctions(ADQLQuery, BinarySearch, UnresolvedIdentifiersException)
	 * @see #resolveCoordinateSystems(ADQLQuery, UnresolvedIdentifiersException)
	 * @see #resolveSTCSExpressions(ADQLQuery, BinarySearch, UnresolvedIdentifiersException)
	 * 
	 * @since 1.3
	 */
	protected void checkGeometries(final ADQLQuery query, final UnresolvedIdentifiersException errors){
		BinarySearch<String,String> binSearch = new BinarySearch<String,String>(){
			@Override
			protected int compare(String searchItem, String arrayItem){
				return searchItem.compareToIgnoreCase(arrayItem);
			}
		};

		// a. Ensure that all used geometry functions are allowed:
		if (allowedGeo != null)
			resolveGeometryFunctions(query, binSearch, errors);

		// b. Check whether the coordinate systems are allowed:
		if (allowedCoordSys != null)
			resolveCoordinateSystems(query, errors);

		// c. Check all STC-S expressions (in RegionFunctions only) + the used coordinate systems (if StringConstant only):
		if (allowedGeo == null || (allowedGeo.length > 0 && binSearch.search("REGION", allowedGeo) >= 0))
			resolveSTCSExpressions(query, binSearch, errors);
	}

	/**
	 * Search for all geometrical functions and check whether they are allowed.
	 * 
	 * @param query		Query in which geometrical functions must be checked.
	 * @param errors	List of errors to complete in this function each time a geometrical function is not supported.
	 * 
	 * @see #checkGeometryFunction(String, ADQLFunction, BinarySearch, UnresolvedIdentifiersException)
	 * 
	 * @since 1.3
	 */
	protected void resolveGeometryFunctions(final ADQLQuery query, final BinarySearch<String,String> binSearch, final UnresolvedIdentifiersException errors){
		ISearchHandler sHandler = new SearchGeometryHandler();
		sHandler.search(query);

		String fctName;
		for(ADQLObject result : sHandler){
			fctName = result.getName();
			checkGeometryFunction(fctName, (ADQLFunction)result, binSearch, errors);
		}
	}

	/**
	 * <p>Check whether the specified geometrical function is allowed by this implementation.</p>
	 * 
	 * <p><i>Note:
	 * 	If the list of allowed geometrical functions is empty, this function will always add an errors to the given list.
	 * 	Indeed, it means that no geometrical function is allowed and so that the specified function is automatically not supported.
	 * </i></p>
	 * 
	 * @param fctName		Name of the geometrical function to test.
	 * @param fct			The function instance being or containing the geometrical function to check. <i>Note: this function can be the function to test or a function embedding the function under test (i.e. RegionFunction).
	 * @param binSearch		The object to use in order to search a function name inside the list of allowed functions.
	 *                 		It is able to perform a binary search inside a sorted array of String objects. The interest of
	 *                 		this object is its compare function which must be overridden and tells how to compare the item
	 *                 		to search and the items of the array (basically, a non-case-sensitive comparison between 2 strings).
	 * @param errors		List of errors to complete in this function each time a geometrical function is not supported.
	 * 
	 * @since 1.3
	 */
	protected void checkGeometryFunction(final String fctName, final ADQLFunction fct, final BinarySearch<String,String> binSearch, final UnresolvedIdentifiersException errors){
		int match = -1;
		if (allowedGeo.length != 0)
			match = binSearch.search(fctName, allowedGeo);
		if (match < 0)
			errors.addException(new UnresolvedFunction("The geometrical function \"" + fctName + "\" is not available in this implementation!", fct));
	}

	/**
	 * <p>Search all explicit coordinate system declarations, check their syntax and whether they are allowed by this implementation.</p>
	 * 
	 * <p><i>Note:
	 * 	"explicit" means here that all {@link StringConstant} instances. Only coordinate systems expressed as string can
	 * 	be parsed and so checked. So if a coordinate system is specified by a column, no check can be done at this stage...
	 * 	it will be possible to perform such test only at the execution.
	 * </i></p>
	 * 
	 * @param query		Query in which coordinate systems must be checked.
	 * @param errors	List of errors to complete in this function each time a coordinate system has a wrong syntax or is not supported.
	 * 
	 * @see #checkCoordinateSystem(StringConstant, UnresolvedIdentifiersException)
	 * 
	 * @since 1.3
	 */
	protected void resolveCoordinateSystems(final ADQLQuery query, final UnresolvedIdentifiersException errors){
		ISearchHandler sHandler = new SearchCoordSysHandler();
		sHandler.search(query);
		for(ADQLObject result : sHandler)
			checkCoordinateSystem((StringConstant)result, errors);
	}

	/**
	 * Parse and then check the coordinate system contained in the given {@link StringConstant} instance.
	 * 
	 * @param adqlCoordSys	The {@link StringConstant} object containing the coordinate system to check.
	 * @param errors		List of errors to complete in this function each time a coordinate system has a wrong syntax or is not supported.
	 * 
	 * @see STCS#parseCoordSys(String)
	 * @see #checkCoordinateSystem(CoordSys, ADQLOperand, UnresolvedIdentifiersException)
	 * 
	 * @since 1.3
	 */
	protected void checkCoordinateSystem(final StringConstant adqlCoordSys, final UnresolvedIdentifiersException errors){
		String coordSysStr = adqlCoordSys.getValue();
		try{
			checkCoordinateSystem(STCS.parseCoordSys(coordSysStr), adqlCoordSys, errors);
		}catch(ParseException pe){
			errors.addException(new ParseException(pe.getMessage())); // TODO Missing object position!
		}
	}

	/**
	 * Check whether the given coordinate system is allowed by this implementation. 
	 * 
	 * @param coordSys	Coordinate system to test.
	 * @param operand	The operand representing or containing the coordinate system under test.
	 * @param errors	List of errors to complete in this function each time a coordinate system is not supported.
	 * 
	 * @since 1.3
	 */
	protected void checkCoordinateSystem(final CoordSys coordSys, final ADQLOperand operand, final UnresolvedIdentifiersException errors){
		if (coordSysRegExp != null && coordSys != null && !coordSys.toFullSTCS().matches(coordSysRegExp))
			errors.addException(new ParseException("Coordinate system \"" + ((operand instanceof StringConstant) ? ((StringConstant)operand).getValue() : coordSys.toString()) + "\" (= \"" + coordSys.toFullSTCS() + "\") not allowed in this implementation."));	// TODO Missing object position! + List of accepted coordinate systems
	}

	/**
	 * <p>Search all STC-S expressions inside the given query, parse them (and so check their syntax) and then determine
	 * whether the declared coordinate system and the expressed region are allowed in this implementation.</p>
	 * 
	 * <p><i>Note:
	 * 	In the current ADQL language definition, STC-S expressions can be found only as only parameter of the REGION function.
	 * </i></p>
	 * 
	 * @param query			Query in which STC-S expressions must be checked.
	 * @param binSearch		The object to use in order to search a region name inside the list of allowed functions/regions.
	 *                 		It is able to perform a binary search inside a sorted array of String objects. The interest of
	 *                 		this object is its compare function which must be overridden and tells how to compare the item
	 *                 		to search and the items of the array (basically, a non-case-sensitive comparison between 2 strings).
	 * @param errors		List of errors to complete in this function each time the STC-S syntax is wrong or each time the declared coordinate system or region is not supported.
	 * 
	 * @see STCS#parseRegion(String)
	 * @see #checkRegion(Region, RegionFunction, BinarySearch, UnresolvedIdentifiersException)
	 * 
	 * @since 1.3
	 */
	protected void resolveSTCSExpressions(final ADQLQuery query, final BinarySearch<String,String> binSearch, final UnresolvedIdentifiersException errors){
		// Search REGION functions:
		ISearchHandler sHandler = new SearchRegionHandler();
		sHandler.search(query);

		// Parse and check their STC-S expression:
		String stcs;
		Region region;
		for(ADQLObject result : sHandler){
			try{
				// get the STC-S expression:
				stcs = ((StringConstant)((RegionFunction)result).getParameter(0)).getValue();

				// parse the STC-S expression (and so check the syntax):
				region = STCS.parseRegion(stcs);

				// check whether the regions (this one + the possible inner ones) and the coordinate systems are allowed:
				checkRegion(region, (RegionFunction)result, binSearch, errors);
			}catch(ParseException pe){
				errors.addException(new ParseException(pe.getMessage())); // TODO Missing object position!
			}
		}
	}

	/**
	 * <p>Check the given region.</p>
	 * 
	 * <p>The following points are checked in this function:</p>
	 * <ul>
	 * 	<li>whether the coordinate system is allowed</li>
	 * 	<li>whether the type of region is allowed</li>
	 * 	<li>whether the inner regions are correct (here this function is called recursively on each inner region).</li>
	 * </ul>
	 * 
	 * @param r			The region to check.
	 * @param fct		The REGION function containing the region to check.
	 * @param errors	List of errors to complete in this function if the given region or its inner regions are not supported.
	 * 
	 * @see #checkCoordinateSystem(CoordSys, ADQLOperand, UnresolvedIdentifiersException)
	 * @see #checkGeometryFunction(String, ADQLFunction, BinarySearch, UnresolvedIdentifiersException)
	 * 
	 * @since 1.3
	 */
	protected void checkRegion(final Region r, final RegionFunction fct, final BinarySearch<String,String> binSearch, final UnresolvedIdentifiersException errors){
		if (r == null)
			return;

		// Check the coordinate system (if any):
		if (r.coordSys != null)
			checkCoordinateSystem(r.coordSys, fct, errors);

		// Check that the region type is allowed:
		if (allowedGeo != null){
			if (allowedGeo.length == 0)
				errors.addException(new UnresolvedFunction("The region type \"" + r.type + "\" is not available in this implementation!", fct));
			else
				checkGeometryFunction((r.type == RegionType.POSITION) ? "POINT" : r.type.toString(), fct, binSearch, errors);
		}

		// Check all the inner regions:
		if (r.regions != null){
			for(Region innerR : r.regions)
				checkRegion(innerR, fct, binSearch, errors);
		}
	}

	/* **************************************************** */
	/* METHODS CHECKING TYPES UNKNOWN WHILE CHECKING SYNTAX */
	/* **************************************************** */

	/**
	 * <p>Search all operands whose the type is not yet known and try to resolve it now
	 * and to check whether it matches the type expected by the syntactic parser.</p>
	 * 
	 * <p>
	 * 	Only two operands may have an unresolved type: columns and user defined functions.
	 * 	Indeed, their type can be resolved only if the list of available columns and UDFs is known,
	 * 	and if columns and UDFs used in the query are resolved successfully.
	 * </p>
	 * 
	 * <p>
	 * 	When an operand type is still unknown, they will own the three kinds of type and
	 * 	so this function won't raise an error: it is thus automatically on the expected type.
	 * 	This behavior is perfectly correct because if the type is not resolved
	 * 	that means the item/operand has not been resolved in the previous steps and so that
	 * 	an error about this item has already been raised.
	 * </p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function does not check the types exactly, but just roughly by considering only three categories:
	 * 	string, numeric and geometry.
	 * </i></p>
	 * 
	 * @param query		Query in which unknown types must be resolved and checked.
	 * @param errors	List of errors to complete in this function each time a types does not match to the expected one.
	 * 
	 * @see UnknownType
	 * 
	 * @since 1.3
	 */
	protected void checkTypes(final ADQLQuery query, final UnresolvedIdentifiersException errors){
		// Search all unknown types:
		ISearchHandler sHandler = new SearchUnknownTypeHandler();
		sHandler.search(query);

		// Check whether their type matches the expected one:
		UnknownType unknown;
		for(ADQLObject result : sHandler){
			unknown = (UnknownType)result;
			switch(unknown.getExpectedType()){
				case 'G':
				case 'g':
					if (!unknown.isGeometry())
						errors.addException(new ParseException("Type mismatch! A geometry was expected instead of \"" + unknown.toADQL() + "\"."));	// TODO Add the ADQLOperand position!
					break;
				case 'N':
				case 'n':
					if (!unknown.isNumeric())
						errors.addException(new ParseException("Type mismatch! A numeric value was expected instead of \"" + unknown.toADQL() + "\"."));	// TODO Add the ADQLOperand position!
					break;
				case 'S':
				case 's':
					if (!unknown.isString())
						errors.addException(new ParseException("Type mismatch! A string value was expected instead of \"" + unknown.toADQL() + "\"."));	// TODO Add the ADQLOperand position!
					break;
			}
		}
	}

	/* ******************************** */
	/* METHODS CHECKING THE SUB-QUERIES */
	/* ******************************** */

	/**
	 * <p>Search all sub-queries found in the given query but not in the clause FROM.
	 * These sub-queries are then checked using {@link #check(ADQLQuery, Stack)}.</p>
	 * 
	 * <b>Fathers stack</b>
	 * <p>
	 * 	Each time a sub-query must be checked with {@link #check(ADQLQuery, Stack)},
	 * 	the list of all columns available in each of its father queries must be provided.
	 * 	This function is composing itself this stack by adding the given list of available
	 * 	columns (= all columns resolved in the given query) at the end of the given stack.
	 * 	If this stack is given empty, then a new stack is created.
	 * </p>
	 * <p>
	 * 	This modification of the given stack is just the execution time of this function.
	 * 	Before returning, this function removes the last item of the stack.
	 * </p>
	 * 
	 * 
	 * @param query				Query in which sub-queries must be checked.
	 * @param fathersList		List of all columns available in the father queries and that should be accessed in sub-queries.
	 *                      	Each item of this stack is a list of columns available in each father-level query.
	 *                   		<i>Note: this parameter is NULL if this function is called with the root/father query as parameter.</i>
	 * @param availableColumns	List of all columns resolved in the given query.
	 * @param errors			List of errors to complete in this function each time a semantic error is encountered.
	 * 
	 * @since 1.3
	 */
	protected void checkSubQueries(final ADQLQuery query, Stack<SearchColumnList> fathersList, final SearchColumnList availableColumns, final UnresolvedIdentifiersException errors){
		// Check sub-queries outside the clause FROM:
		ISearchHandler sHandler = new SearchSubQueryHandler();
		sHandler.search(query);
		if (sHandler.getNbMatch() > 0){

			// Push the list of columns into the father columns stack:
			if (fathersList == null)
				fathersList = new Stack<SearchColumnList>();
			fathersList.push(availableColumns);

			// Check each found sub-query:
			for(ADQLObject result : sHandler){
				try{
					check((ADQLQuery)result, fathersList);
				}catch(UnresolvedIdentifiersException uie){
					Iterator<ParseException> itPe = uie.getErrors();
					while(itPe.hasNext())
						errors.addException(itPe.next());
				}
			}

			// Pop the list of columns from the father columns stack:
			fathersList.pop();
		}
	}

	/* *************** */
	/* SEARCH HANDLERS */
	/* *************** */
	/**
	 * Lets searching all tables.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 1.0 (07/2011)
	 */
	private static class SearchTableHandler extends SimpleSearchHandler {
		@Override
		public boolean match(final ADQLObject obj){
			return obj instanceof ADQLTable;
		}
	}

	/**
	 * Lets searching all wildcards.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 1.0 (09/2011)
	 */
	private static class SearchWildCardHandler extends SimpleSearchHandler {
		@Override
		public boolean match(final ADQLObject obj){
			return (obj instanceof SelectAllColumns) && (((SelectAllColumns)obj).getAdqlTable() != null);
		}
	}

	/**
	 * Lets searching column references.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 1.0 (11/2011)
	 */
	private static class SearchColReferenceHandler extends SimpleSearchHandler {
		@Override
		public boolean match(final ADQLObject obj){
			return (obj instanceof ColumnReference);
		}
	}

	/**
	 * <p>Lets searching subqueries in every clause except the FROM one (hence the modification of the {@link #goInto(ADQLObject)}.</p>
	 * 
	 * <p><i>
	 * 	<u>Note:</u> The function {@link #addMatch(ADQLObject, ADQLIterator)} has been modified in order to
	 * 	not have the root search object (here: the main query) in the list of results.
	 * </i></p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.2 (12/2013)
	 * @since 1.2
	 */
	private static class SearchSubQueryHandler extends SimpleSearchHandler {
		@Override
		protected void addMatch(ADQLObject matchObj, ADQLIterator it){
			if (it != null)
				super.addMatch(matchObj, it);
		}

		@Override
		protected boolean goInto(ADQLObject obj){
			return super.goInto(obj) && !(obj instanceof FromContent);
		}

		@Override
		protected boolean match(ADQLObject obj){
			return (obj instanceof ADQLQuery);
		}
	}

	/**
	 * Let searching user defined functions.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	private static class SearchUDFHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj){
			return (obj instanceof UserDefinedFunction);
		}
	}

	/**
	 * Let searching geometrical functions.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	private static class SearchGeometryHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj){
			return (obj instanceof GeometryFunction);
		}
	}

	/**
	 * <p>Let searching all ADQL objects whose the type was not known while checking the syntax of the ADQL query.
	 * These objects are {@link ADQLColumn}s and {@link UserDefinedFunction}s.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	Only {@link UnknownType} instances having an expected type equals to 'S' (or 's' ; for string) or 'N' (or 'n' ; for numeric)
	 * 	are kept by this handler. Others are ignored.
	 * </i></p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	private static class SearchUnknownTypeHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj){
			if (obj instanceof UnknownType){
				char expected = ((UnknownType)obj).getExpectedType();
				return (expected == 'G' || expected == 'g' || expected == 'S' || expected == 's' || expected == 'N' || expected == 'n');
			}else
				return false;
		}
	}

	/**
	 * Let searching all explicit declaration of coordinate systems.
	 * So, only {@link StringConstant} objects will be returned.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	private static class SearchCoordSysHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj){
			if (obj instanceof PointFunction || obj instanceof BoxFunction || obj instanceof CircleFunction || obj instanceof PolygonFunction)
				return (((GeometryFunction)obj).getCoordinateSystem() instanceof StringConstant);
			else
				return false;
		}

		@Override
		protected void addMatch(ADQLObject matchObj, ADQLIterator it){
			results.add(((GeometryFunction)matchObj).getCoordinateSystem());
		}

	}

	/**
	 * Let searching all {@link RegionFunction}s.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	private static class SearchRegionHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj){
			if (obj instanceof RegionFunction)
				return (((RegionFunction)obj).getParameter(0) instanceof StringConstant);
			else
				return false;
		}

	}

	/**
	 * <p>Implement the binary search algorithm over a sorted array.</p>
	 * 
	 * <p>
	 * 	The only difference with the standard implementation of Java is
	 * 	that this object lets perform research with a different type
	 * 	of object than the types of array items.
	 * </p>
	 * 
	 * <p>
	 * 	For that reason, the "compare" function must always be implemented.
	 * </p>
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * 
	 * @param <T>	Type of items stored in the array.
	 * @param <S>	Type of the item to search.
	 * 
	 * @since 1.3
	 */
	protected static abstract class BinarySearch< T, S > {
		private int s, e, m, comp;

		/**
		 * <p>Search the given item in the given array.</p>
		 * 
		 * <p>
		 * 	In case the given object matches to several items of the array,
		 * 	this function will return the smallest index, pointing thus to the first
		 * 	of all matches.
		 * </p>
		 * 
		 * @param searchItem	Object for which a corresponding array item must be searched.
		 * @param array			Array in which the given object must be searched.
		 * 
		 * @return	The array index of the first item of all matches.
		 */
		public int search(final S searchItem, final T[] array){
			s = 0;
			e = array.length - 1;
			while(s < e){
				// middle of the sorted array:
				m = s + ((e - s) / 2);
				// compare the fct with the middle item of the array:
				comp = compare(searchItem, array[m]);
				// if the fct is after, trigger the inspection of the right part of the array: 
				if (comp > 0)
					s = m + 1;
				// otherwise, the left part:
				else
					e = m;
			}
			if (s != e || compare(searchItem, array[s]) != 0)
				return -1;
			else
				return s;
		}

		/**
		 * Compare the search item and the array item.
		 * 
		 * @param searchItem	Item whose a corresponding value must be found in the array.
		 * @param arrayItem		An item of the array.
		 * 
		 * @return	Negative value if searchItem is less than arrayItem, 0 if they are equals, or a positive value if searchItem is greater.
		 */
		protected abstract int compare(final S searchItem, final T arrayItem);
	}

}
