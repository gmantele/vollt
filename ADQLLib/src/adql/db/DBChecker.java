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
 * Copyright 2011-2024 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.db.exception.UnresolvedColumnException;
import adql.db.exception.UnresolvedFunctionException;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.db.exception.UnresolvedTableException;
import adql.db.region.CoordSys;
import adql.db.region.Region;
import adql.db.region.STCS;
import adql.parser.QueryChecker;
import adql.parser.grammar.ParseException;
import adql.query.*;
import adql.query.from.ADQLTable;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.UnknownType;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.UserDefinedFunction;
import adql.query.operand.function.geometry.*;
import adql.search.ISearchHandler;
import adql.search.SearchColumnHandler;
import adql.search.SimpleReplaceHandler;
import adql.search.SimpleSearchHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * <h3>ADQL Query verification</h3>
 *
 * This {@link QueryChecker} implementation is able to do the following
 * verifications on an ADQL query:
 * <ol>
 * 	<li>Check existence of all table and column references,</li>
 * 	<li>Resolve User Defined Functions (UDFs),</li>
 * 	<li>Check types of columns and UDFs.</li>
 * </ol>
 *
 * <p><i><b>IMPORTANT note:</b>
 * 	Since v2.0, the check of supported geometric functions, STC-s expressions
 * 	and coordinate systems are performed automatically in
 * 	{@link adql.parser.ADQLParser ADQLParser} through the notion of Optional
 * 	Features. The declaration of supported geometric functions must now be done
 * 	with {@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}
 * 	(see also {@link adql.parser.feature.FeatureSet FeatureSet}).
 * </i></p>
 *
 * <h3>DB content annotations</h3>
 *
 * <p>
 * 	In addition to check the existence of tables and columns referenced in the
 * 	query, database metadata ({@link DBTable} or {@link DBColumn}) will also be
 * 	attached to ({@link ADQLTable} and {@link ADQLColumn} instances when they
 * 	are resolved.
 * </p>
 *
 * <p><i><b>Note:</b>
 * 	Knowing DB metadata of {@link ADQLTable} and {@link ADQLColumn} is
 * 	particularly useful for the translation of the ADQL query to SQL, because
 * 	the ADQL name of columns and tables can be replaced in SQL by their DB name,
 * 	if different. This mapping is done automatically by
 * 	{@link adql.translator.JDBCTranslator JDBCTranslator}.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (10/2024)
 */
public class DBChecker implements QueryChecker {

	/** List of all available tables ({@link DBTable}).
	 * <p><i><b>IMPORTANT: List shared with all threads.</b>
	 * 	This list must list all the tables in common to any ADQL query. It
	 * 	must never contain any temporary table (e.g. uploads).
	 * </i></p> */
	protected SearchTableApi lstTables;

	/** List of all allowed User Defined Functions (UDFs).
	 * <p><i><b>Note:</b>
	 * 	If this list is NULL, any encountered UDF will be allowed.
	 * 	However, if not, all items of this list must be the only allowed UDFs.
	 * 	So, if the list is empty, no UDF is allowed.
	 * </i></p>
	 * <p><i><b>IMPORTANT: List shared with all threads.</b></i></p>
	 * @since 1.3 */
	protected FunctionDef[] allowedUdfs = null;

	/* **********************************************************************
	   *                          CONSTRUCTORS                              *
	   ********************************************************************** */

	/**
	 * Builds a {@link DBChecker} with an empty list of tables.
	 *
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:
	 * 		<b>NO <i>(even unknown or fake tables and columns are allowed)</i></b></li>
	 * 	<li>Existence of User Defined Functions (UDFs):
	 * 		<b>NO <i>(any "unknown" function is allowed)</i></b></li>
	 * 	<li>Types consistency:
	 * 		<b>NO</b></li>
	 * </ul>
	 */
	public DBChecker() {
		this(null, null);
	}

	/**
	 * Builds a {@link DBChecker} with the given list of known tables.
	 *
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:
	 * 		<b>OK</b></li>
	 * 	<li>Existence of User Defined Functions (UDFs):
	 * 		<b>NO <i>(any "unknown" function is allowed)</i></b></li>
	 * 	<li>Types consistency:
	 * 		<b>OK, except with unknown functions</b></li>
	 * </ul>
	 *
	 * @param tables	List of all available tables.
	 */
	public DBChecker(final Collection<? extends DBTable> tables) {
		this(tables, null);
	}

	/**
	 * Builds a {@link DBChecker} with the given list of known tables and with a
	 * restricted list of User Defined Functions (UDFs).
	 *
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:
	 * 		<b>OK</b></li>
	 * 	<li>Existence of User Defined Functions (UDFs):
	 * 		<b>OK</b></li>
	 * 	<li>Types consistency:
	 * 		<b>OK</b></li>
	 * </ul>
	 *
	 * @param tables		List of all available tables.
	 * @param allowedUdfs	List of all allowed user defined functions.
	 *                   	If NULL, no verification will be done (and so, all
	 *                   	UDFs are allowed).
	 *                   	If empty list, no "unknown" (or UDF) is allowed.
	 *                   	<i><b>Note:</b> match with items of this list are
	 *                   	done case insensitively.</i>
	 *
	 * @since 1.3
	 */
	public DBChecker(final Collection<? extends DBTable> tables, final Collection<? extends FunctionDef> allowedUdfs) {
		// Sort and store the given tables:
		setTables(tables);

		Object[] tmp;
		int cnt;

		// Store all allowed UDFs in a sorted array:
		if (allowedUdfs != null) {
			// Remove all NULL:
			tmp = new FunctionDef[allowedUdfs.size()];
			cnt = 0;
			for(FunctionDef udf : allowedUdfs) {
				if (udf != null)
					tmp[cnt++] = udf;
			}
			// make a copy of the array:
			this.allowedUdfs = new FunctionDef[cnt];
			System.arraycopy(tmp, 0, this.allowedUdfs, 0, cnt);

			// sort the values:
			Arrays.sort(this.allowedUdfs);
		}
	}

	/* **********************************************************************
	   *                             SETTERS                                *
	   ********************************************************************** */
	/**
	 * Sets the list of all available tables.
	 *
	 * <p><i><b>Note:</b>
	 * 	Only if the given collection is an implementation of
	 * 	{@link SearchTableApi}, it will be used directly as provided.
	 * 	Otherwise the given collection will be copied inside a new
	 * 	{@link SearchTableList}.
	 * </i></p>
	 *
	 * @param tables	List of {@link DBTable}s.
	 */
	public final void setTables(final Collection<? extends DBTable> tables) {
		if (tables == null)
			lstTables = new SearchTableList();
		else if (tables instanceof SearchTableApi)
			lstTables = (SearchTableApi)tables;
		else
			lstTables = new SearchTableList(tables);
	}

	/* **********************************************************************
	   *                       CHECK METHODS                                *
	   ********************************************************************** */

	/**
	 * Check all the column, table and UDF references inside the given query.
	 *
	 * <p><i><b>Note:</b>
	 * 	This query has already been parsed ; thus it is already syntactically
	 * 	correct. Only the consistency with the published tables, columns and all
	 * 	the defined UDFs will be checked.
	 * </i></p>
	 *
	 * @param query		The query to check.
	 *
	 * @throws ParseException	An {@link UnresolvedIdentifiersException} if
	 *                       	some tables or columns can not be resolved.
	 *
	 * @see #check(ADQLSet, Stack)
	 */
	@Override
	public final void check(final ADQLSet query) throws ParseException {
		check(query, null);
	}

	/**
	 * Process several (semantic) verifications in the given ADQL query.
	 *
	 * <p>Main operations performed in this function:</p>
	 * <ol>
	 * 	<li>Check all tables possibly defined in the WITH clause</li>
	 * 	<li>Check the main query (a simple SELECT) or the set operation
	 * 	    (e.g. UNION, INTERSECT, EXCEPT)</li>
	 * </ol>
	 *
	 * @param query			The (sub-)query to check.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 *
	 * @throws UnresolvedIdentifiersException	An {@link UnresolvedIdentifiersException}
	 *                                       	if one or several of the above
	 *                                       	listed tests have detected some
	 *                                       	semantic errors (i.e. unresolved
	 *                                       	table, columns, function).
	 *
	 * @since 2.0
	 *
	 * @see #check(ADQLQuery, Stack, UnresolvedIdentifiersException)
	 * @see #check(SetOperation, Stack, UnresolvedIdentifiersException)
	 */
	protected void check(final ADQLSet query, Stack<CheckContext> contextList) throws UnresolvedIdentifiersException {
		UnresolvedIdentifiersException errors = new UnresolvedIdentifiersException();

		// Initialize the context:
		if (contextList == null)
			contextList = new Stack<CheckContext>();
		if (contextList.isEmpty())
			contextList.push(new CheckContext(null, null));
		else
			contextList.push(contextList.peek().getCopy());

		// Get the first context:
		final CheckContext context = contextList.peek();

		// Resolve tables/queries declared in the WITH clause, if any:
		for(WithItem withItem : query.getWith()) {

			// Check this query (and set all the metadata on all DB items)
			try {
				check(withItem.getQuery(), contextList);
			} catch(UnresolvedIdentifiersException uie) {
				for(ParseException pe : uie)
					errors.addException(pe);
			}

			// Generate the corresponding DBTable:
			withItem.setDBLink(generateDBTable(withItem));

			// Build a corresponding virtual ADQLTable:
			ADQLTable adqlTable = new ADQLTable(null, withItem.getLabel());
			adqlTable.setCaseSensitive(IdentifierField.TABLE, withItem.isLabelCaseSensitive());
			adqlTable.setDBLink(withItem.getDBLink());

			// Update the context:
			context.cteTables.add(adqlTable.getDBLink());
		}

		// CASE: Simple query:
		if (query instanceof ADQLQuery)
			check((ADQLQuery)query, contextList, errors);

		// CASE: Operation between 2 rows sets:
		else if (query instanceof SetOperation)
			check((SetOperation)query, contextList, errors);

		// ELSE: nothing to do!

		// Throw all errors, if any:
		if (errors.getNbErrors() > 0)
			throw errors;

		// Remove the current context:
		contextList.pop();
	}

	/**
	 * Process several (semantic) verifications in the queries used in the given
	 * UNION/INTERSECT/EXCEPT operation.
	 *
	 * <p>Main operations performed in this function:</p>
	 * <ol>
	 * 	<li>Check left query</li>
	 * 	<li>Check right query</li>
	 * 	<li>Check equality of both sets of columns</li>
	 * 	<li>Check equality of these columns' datatypes</li>
	 * </ol>
	 *
	 * @param setOp			The (sub-)query to check.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 * @param errors        Accumulative list of semantic errors. Detected
	 *                      semantic errors should be appended to this list.
	 *
	 * @since 2.0
	 *
	 * @see #check(ADQLSet, Stack)
	 */
	protected void check(final SetOperation setOp, Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {
		// Check the left set:
		try {
			check(setOp.getLeftSet(), contextList);
		} catch(UnresolvedIdentifiersException uie) {
			Iterator<ParseException> itPe = uie.getErrors();
			while(itPe.hasNext())
				errors.addException(itPe.next());
		}

		// Check the right set:
		try {
			check(setOp.getRightSet(), contextList);
		} catch(UnresolvedIdentifiersException uie) {
			Iterator<ParseException> itPe = uie.getErrors();
			while(itPe.hasNext())
				errors.addException(itPe.next());
		}

		// Check the number of columns:
		final DBColumn[] leftColumns = setOp.getLeftSet().getResultingColumns();
		final DBColumn[] rightColumns = setOp.getRightSet().getResultingColumns();
		if (leftColumns.length != rightColumns.length)
			errors.addException(new ParseException("Columns number mismatch! This sub-query must return the same of number of columns as the left sub-query (i.e. " + leftColumns.length + " instead of " + rightColumns.length + ").", setOp.getRightSet().getPosition()));

		// Check the columns datatype:
		if (leftColumns.length == rightColumns.length) {
			for(int i = 0; i < leftColumns.length; i++) {
				if (leftColumns[i].getDatatype() != null && rightColumns[i].getDatatype() != null && !leftColumns[i].getDatatype().isCompatible(rightColumns[i].getDatatype()))
					errors.addException(new ParseException("Columns datatype mismatch! The " + (i + 1) + "-th SELECT-ed column (named '" + rightColumns[i].getADQLName() + "') was expected to be a " + leftColumns[i].getDatatype() + " instead of a " + rightColumns[i].getDatatype() + "!", setOp.getRightSet().getPosition()));
			}
		}
	}

	/**
	 * Process several (semantic) verifications in the given ADQL query.
	 *
	 * <p>Main verifications done in this function:</p>
	 * <ol>
	 * 	<li>Existence of DB items (tables and columns)</li>
	 * 	<li>Semantic verification of sub-queries</li>
	 * 	<li>Support of every encountered User Defined Functions (UDFs -
	 * 		functions unknown by the syntactic parser)</li>
	 * 	<li>Consistency of types still unknown (because the syntactic parser
	 * 		could not yet resolve them)</li>
	 * </ol>
	 *
	 * @param query			The query to check.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 *
	 * @since 1.2
	 *
	 * @see #checkDBItems(ADQLQuery, Stack, UnresolvedIdentifiersException)
	 * @see #checkSubQueries(ADQLQuery, Stack, UnresolvedIdentifiersException)
	 * @see #checkUDFs(ADQLQuery, UnresolvedIdentifiersException)
	 * @see #checkTypes(ADQLQuery, UnresolvedIdentifiersException)
	 */
	protected void check(final ADQLQuery query, Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {

		// A. Check DB items (tables and columns):
		checkDBItems(query, contextList, errors);

		// B. Check UDFs:
		if (allowedUdfs != null)
			checkUDFs(query, errors);

		// C. Check types:
		checkTypes(query, errors);

		// D. Check sub-queries:
		checkSubQueries(query, contextList, errors);
	}

	/* **********************************************************************
	   *          CHECKING METHODS FOR DB ITEMS (TABLES & COLUMNS)          *
	   ********************************************************************** */

	/**
	 * Check DB items (tables and columns) used in the given ADQL query.
	 *
	 * <p>Operations done in this function:</p>
	 * <ol>
	 * 	<li>Resolve all found tables</li>
	 * 	<li>Get the whole list of all available columns. <i>(<b>note:</b> this list is
	 * 		returned by this function)</i></li>
	 * 	<li>Resolve all found columns</li>
	 * </ol>
	 *
	 * @param query			Query in which the existence of DB items must be
	 *             			checked.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 * @param errors		List of errors to complete in this function each
	 *              		time an unknown table or column is encountered.
	 *
	 * @return	List of all columns available in the given query.
	 *
	 * @see #resolveTables(ADQLQuery, Stack, UnresolvedIdentifiersException)
	 * @see FromContent#getDBColumns()
	 * @see #resolveColumns(ADQLQuery, Stack, UnresolvedIdentifiersException)
	 *
	 * @since 1.3
	 */
	protected SearchColumnList checkDBItems(final ADQLQuery query, final Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {
		// a. Resolve all tables:
		resolveTables(query, contextList, errors);

		// b. Get the list of all columns made available in the clause FROM:
		SearchColumnList availableColumns;
		try {
			availableColumns = query.getFrom().getDBColumns();
		} catch(ParseException pe) {
			errors.addException(pe);
			availableColumns = new SearchColumnList();
		}
		contextList.peek().availableColumns.addAll(availableColumns);

		// c. Resolve all columns:
		resolveColumns(query, contextList, errors);

		return availableColumns;
	}

	/**
	 * Search all table references inside the given query, resolve them against
	 * the available tables, and if there is only one match, attach the matching
	 * metadata to them.
	 *
	 * <h3>Management of Common Table Expressions (CTEs ; WITH expressions)</h3>
	 * <p>
	 * 	If the clause WITH is not empty, any declared CTE/sub-query will be
	 * 	checked. If correct, a {@link DBTable} will be generated using
	 * 	{@link #generateDBTable(WithItem)} representing this sub-query. This
	 * 	{@link DBTable} is immediately added to the current context so that
	 * 	being referenced in the main query.
	 * </p>
	 *
	 * <h3>Management of sub-query tables</h3>
	 * <p>
	 * 	If a table is not a DB table reference but a sub-query, this latter is
	 * 	first checked, using {@link #check(ADQLSet, Stack)}. Then, its
	 * 	corresponding table metadata are generated (using
	 * 	{@link #generateDBTable(ADQLSet, String)}) and attached to it.
	 * </p>
	 *
	 * <h3>Management of "{table}.*" in the SELECT clause</h3>
	 * <p>
	 * 	For each of this SELECT item, this function tries to resolve the table
	 * 	name. If only one match is found, the corresponding ADQL table object
	 * 	is got from the list of resolved tables and attached to this SELECT item
	 * 	(thus, the joker item will also have the good metadata, particularly if
	 * 	the referenced table is a sub-query).
	 * </p>
	 *
	 * <h3>Table alias</h3>
	 * <p>
	 * 	When a simple table (i.e. not a sub-query) is aliased, the metadata of
	 * 	this table will be wrapped inside a {@link DBTableAlias} in order to
	 * 	keep the original metadata but still declare use the table with the
	 * 	alias instead of its original name. The original name will be used
	 * 	only when translating the corresponding FROM item ; the rest of the time
	 * 	(i.e. for references when using a column), the alias name must be used.
	 * </p>
	 * <p>
	 * 	In order to avoid unpredictable behavior at execution of the SQL query,
	 * 	the alias will be put in lower case if not defined between double
	 * 	quotes.
	 * </p>
	 *
	 * @param query			Query in which the existence of tables must be
	 *             			checked.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 * @param errors		List of errors to complete in this function each
	 *              		time an unknown table or column is encountered.
	 */
	protected void resolveTables(final ADQLQuery query, final Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {
		final CheckContext context = contextList.peek();
		ISearchHandler sHandler;

		// Check the existence of all tables in the FROM clause:
		sHandler = new SearchTableHandler();
		sHandler.search(query.getFrom());
		for(ADQLObject result : sHandler) {
			try {
				ADQLTable table = (ADQLTable)result;

				// resolve the table:
				DBTable dbTable = null;
				if (table.isSubQuery()) {
					// check the sub-query tables:
					check(table.getSubQuery(), contextList);
					// generate its DBTable:
					dbTable = generateDBTable(table.getSubQuery(), (table.isCaseSensitive(IdentifierField.ALIAS) ? "\"" + table.getAlias() + "\"" : table.getAlias()));
				} else {
					// search among DB tables:
					dbTable = resolveTable(table, contextList);
					// wrap this table metadata if an alias should be used:
					if (dbTable != null && table.hasAlias()) {
						dbTable = new DBTableAlias(dbTable, (table.isCaseSensitive(IdentifierField.ALIAS) ? "\"" + table.getAlias() + "\"" : table.getAlias().toLowerCase()));
					}
				}

				// link with the matched DBTable:
				table.setDBLink(dbTable);
				if (table.isSubQuery() || table.hasAlias()) {
					if (!context.cteTables.add(dbTable))
						errors.addException(new ParseException("Table name already used: \"" + (dbTable == null ? "-" : dbTable.getADQLName()) + "\". Please, choose a different alias for this table."));
				}
			} catch(ParseException pe) {
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
		for(ADQLObject result : sHandler) {
			try {
				SelectAllColumns wildcard = (SelectAllColumns)result;
				ADQLTable table = wildcard.getAdqlTable();
				DBTable dbTable = null;

				// resolve the table reference:
				dbTable = resolveTable(table, contextList);

				// set the corresponding tables among the list of resolved tables:
				//wildcard.setAdqlTable(mapTables.get(dbTable));
				wildcard.getAdqlTable().setDBLink(dbTable);
			} catch(ParseException pe) {
				errors.addException(pe);
			}
		}
	}

	/**
	 * Resolve the given table, that's to say search for the corresponding
	 * {@link DBTable}.
	 *
	 * @param table			The table to resolve.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 *
	 * @return	The corresponding {@link DBTable} if found.
	 *
	 * @throws ParseException	An {@link UnresolvedTableException} if the given
	 *                       	table can't be resolved.
	 */
	protected DBTable resolveTable(final ADQLTable table, final Stack<CheckContext> contextList) throws ParseException {
		// search among fix tables:
		List<DBTable> tables = lstTables.search(table);

		// complete the search with CTEs:
		tables.addAll(contextList.peek().cteTables.search(table));

		// good if only one table has been found:
		if (tables.size() == 1)
			return tables.get(0);
		// but if more than one: ambiguous table name !
		else if (tables.size() > 1)
			throw new UnresolvedTableException(table, (tables.get(0).getADQLSchemaName() == null ? "" : tables.get(0).getADQLSchemaName() + ".") + tables.get(0).getADQLName(), (tables.get(1).getADQLSchemaName() == null ? "" : tables.get(1).getADQLSchemaName() + ".") + tables.get(1).getADQLName());
		// otherwise (no match): unknown table !
		else
			throw new UnresolvedTableException(table);
	}

	/**
	 * Search all column references inside the given query, resolve them thanks
	 * to the given tables' metadata, and if there is only one match, attach the
	 * matching metadata to them.
	 *
	 * <h3>Management of selected columns' references</h3>
	 * <p>
	 * 	A column reference is not only a direct reference to a table column
	 * 	using a column name. It can also be a reference to an item of the SELECT
	 * 	clause (which will then call a "selected column"). That kind of
	 * 	reference can be either an index (an unsigned integer starting from
	 * 	1 to N, where N is the number selected columns), or the name/alias of
	 * 	the column.
	 * </p>
	 * <p>
	 * 	These references are also checked, in second steps, in this function.
	 * 	Thus, column metadata are also attached to them, as common columns.
	 * </p>
	 *
	 * @param query			Query in which the existence of tables must be
	 *             			checked.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 * @param mapTables		List of all resolved tables.
	 * @param errors		List of errors to complete in this function each
	 *              		time an unknown table or column is encountered.
	 *
	 * @deprecated	Since v2.0, the parameter 'mapTables' is no more used.
	 *            	You should use {@link #resolveColumns(ADQLQuery, Stack, UnresolvedIdentifiersException)} instead.
	 */
	@Deprecated
	protected final void resolveColumns(final ADQLQuery query, final Stack<CheckContext> contextList, final Map<DBTable, ADQLTable> mapTables, final UnresolvedIdentifiersException errors) {
		resolveColumns(query, contextList, errors);
	}

	/**
	 * Search all column references inside the given query, resolve them thanks
	 * to the given tables' metadata, and if there is only one match, attach the
	 * matching metadata to them.
	 *
	 * <h3>Management of selected columns' references</h3>
	 * <p>
	 * 	A column reference is not only a direct reference to a table column
	 * 	using a column name. It can also be a reference to an item of the SELECT
	 * 	clause (which will then call a "selected column"). That kind of
	 * 	reference can be either an index (an unsigned integer starting from
	 * 	1 to N, where N is the number selected columns), or the name/alias of
	 * 	the column.
	 * </p>
	 * <p>
	 * 	These references are also checked, in second steps, in this function.
	 * 	Column metadata are also attached to them, as common columns.
	 * </p>
	 *
	 * @param query			Query in which the existence of columns must be
	 *             			checked.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 * @param errors		List of errors to complete in this function each
	 *              		time an unknown table or column is encountered.
	 *
	 * @since 2.0
	 */
	protected void resolveColumns(final ADQLQuery query, final Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {
		ISearchHandler sHandler;

		// Check the existence of all columns:
		sHandler = new SearchColumnOutsideGroupByHandler();
		sHandler.search(query);
		for(ADQLObject result : sHandler) {
			try {
				resolveColumn((ADQLColumn)result, contextList);
			} catch(ParseException pe) {
				errors.addException(pe);
			}
		}

		// Check the GROUP BY items:
		ClauseSelect select = query.getSelect();
		checkGroupBy(query.getGroupBy(), select, contextList, errors);

		// Check the ORDER BY items:
		checkOrderBy(query.getOrderBy(), select, contextList, errors);

		/* Check the correctness of all column references (= references to
		 * selected columns):
		 *
		 * NOTE: no need to provide the father tables when resolving column
		 *       references, because no father column can be used in ORDER BY. */
		sHandler = new SearchColReferenceHandler();
		sHandler.search(query);
		for(ADQLObject result : sHandler) {
			try {
				ColumnReference colRef = (ColumnReference)result;
				// resolve the column reference:
				DBColumn dbColumn = checkColumnReference(colRef, select, contextList);
				// link with the matched DBColumn:
				colRef.setDBLink(dbColumn);
			} catch(ParseException pe) {
				errors.addException(pe);
			}
		}
	}

	/**
	 * Resolve the given column, that's to say search for the corresponding
	 * {@link DBColumn}.
	 *
	 * @param column		The column to resolve.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 *
	 * @return 	The corresponding {@link DBColumn} if found.
	 *        	Otherwise an exception is thrown.
	 *
	 * @throws ParseException	An {@link UnresolvedColumnException} if the
	 *                       	given column can't be resolved
	 * 							or an {@link UnresolvedTableException} if its
	 *                       	table reference can't be resolved.
	 */
	protected DBColumn resolveColumn(final ADQLColumn column, final Stack<CheckContext> contextList) throws ParseException {
		List<DBColumn> foundColumns = contextList.peek().availableColumns.search(column);

		// good if only one column has been found:
		if (foundColumns.size() == 1) {
			column.setDBLink(foundColumns.get(0));
			return foundColumns.get(0);
		}
		// but if more than one: ambiguous table reference !
		else if (foundColumns.size() > 1) {
			if (column.getTableName() == null)
				throw new UnresolvedColumnException(column, (foundColumns.get(0).getTable() == null) ? "<NULL>" : (foundColumns.get(0).getTable().getADQLName() + "." + foundColumns.get(0).getADQLName()), (foundColumns.get(1).getTable() == null) ? "<NULL>" : (foundColumns.get(1).getTable().getADQLName() + "." + foundColumns.get(1).getADQLName()));
			else
				throw new UnresolvedTableException(column, (foundColumns.get(0).getTable() == null) ? "<NULL>" : foundColumns.get(0).getTable().getADQLName(), (foundColumns.get(1).getTable() == null) ? "<NULL>" : foundColumns.get(1).getTable().getADQLName());
		}// otherwise (i.e. no direct match)...
		else {
			// ...try searching among columns of the parent queries:
			if (contextList.size() > 1) {
				Stack<CheckContext> subStack = new Stack<CheckContext>();
				subStack.addAll(contextList.subList(0, contextList.size() - 1));
				return resolveColumn(column, subStack);
			}
			// ...else, unknown column!
			else
				throw new UnresolvedColumnException(column);
		}
	}

	/**
	 * Check and resolve all columns (or column references) inside the given
	 * GROUP BY clause.
	 *
	 * @param groupBy		The GROUP BY to check.
	 * @param select		The SELECT clause (and all its selected items).
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 * @param errors		List of errors to complete in this function each
	 *              		time an unknown table or column is encountered.
	 *
	 * @since 2.0
	 */
	protected void checkGroupBy(final ClauseADQL<ADQLOperand> groupBy, final ClauseSelect select, final Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {
		for(ADQLOperand obj : groupBy) {
			try {
				if (obj instanceof ADQLColumn) {
					ADQLColumn adqlColumn = (ADQLColumn)obj;
					/* resolve the column either as a selected column reference
					 * or as a normal column: */
					if (adqlColumn.getTableName() == null)
						resolveColumnNameReference(adqlColumn, select, contextList);
					else
						resolveColumn(adqlColumn, contextList);
				} else {
					ISearchHandler sHandler = new SearchColumnHandler();
					sHandler.search(obj);
					for(ADQLObject result : sHandler) {
						try {
							resolveColumn((ADQLColumn)result, contextList);
						} catch(ParseException pe) {
							errors.addException(pe);
						}
					}
				}
			} catch(ParseException pe) {
				errors.addException(pe);
			}
		}
	}

	/**
	 * Check and resolve all columns (or column references) inside the given
	 * ORDER BY clause.
	 *
	 * @param orderBy		The ORDER BY to check.
	 * @param select		The SELECT clause (and all its selected items).
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 * @param errors		List of errors to complete in this function each
	 *              		time an unknown table or column is encountered.
	 *
	 * @since 2.0
	 */
	protected void checkOrderBy(final ClauseADQL<ADQLOrder> orderBy, final ClauseSelect select, final Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {
		for(ADQLOrder order : orderBy) {
			try {
				if (order.getExpression() != null) {
					ADQLOperand expr = order.getExpression();
					if (expr instanceof ADQLColumn) {
						ADQLColumn adqlColumn = (ADQLColumn)expr;
						/* resolve the column either as a selected column reference
						 * or as a normal column: */
						if (adqlColumn.getTableName() == null)
							resolveColumnNameReference(adqlColumn, select, contextList);
						else
							resolveColumn(adqlColumn, contextList);
					} else {
						ISearchHandler sHandler = new SearchColumnHandler();
						sHandler.search(expr);
						for(ADQLObject result : sHandler) {
							try {
								resolveColumn((ADQLColumn)result, contextList);
							} catch(ParseException pe) {
								errors.addException(pe);
							}
						}
					}
				}
			} catch(ParseException pe) {
				errors.addException(pe);
			}
		}
	}

	/**
	 * Check whether the given column corresponds to a selected item's alias or
	 * to an existing column.
	 *
	 * @param col			The column to check.
	 * @param select		The SELECT clause of the ADQL query.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 *
	 * @return 	The corresponding {@link DBColumn} if this column corresponds to
	 *        	an existing column,
	 *        	NULL otherwise.
	 *
	 * @throws ParseException	An {@link UnresolvedColumnException} if the
	 *                       	given column can't be resolved
	 * 							or an {@link UnresolvedTableException} if its
	 *                       	table reference can't be resolved.
	 *
	 * @see ClauseSelect#searchByAlias(String)
	 * @see #resolveColumn(ADQLColumn, Stack)
	 *
	 * @since 2.0
	 */
	protected DBColumn resolveColumnNameReference(final ADQLColumn col, final ClauseSelect select, final Stack<CheckContext> contextList) throws ParseException {
		/* If the column name is not qualified, it may be a SELECT-item's alias.
		 * So, try resolving the name as an alias.
		 * If it fails, perform the normal column resolution.*/
		if (col.getTableName() == null) {
			List<SelectItem> founds = select.searchByAlias(col.getColumnName(), col.isCaseSensitive(IdentifierField.COLUMN));
			if (founds.size() == 1)
				return null;
			else if (founds.size() > 1)
				throw new UnresolvedColumnException(col, founds.get(0).getAlias(), founds.get(1).getAlias());
		}
		return resolveColumn(col, contextList);
	}

	/**
	 * Check whether the given column reference corresponds to a selected item
	 * (column or an expression with an alias).
	 *
	 * @param colRef		The column reference which must be checked.
	 * @param select		The SELECT clause of the ADQL query.
	 * @param contextList	Each item of this stack represents a recursion level
	 *                   	inside the main ADQL query. A such item contains the
	 *                   	list of columns and tables available at this level.
	 *
	 * @return 		The corresponding {@link DBColumn} if this reference is
	 *        		actually referencing a selected column,
	 *        		NULL otherwise.
	 *
	 * @throws ParseException	An {@link UnresolvedColumnException} if the
	 *                       	given column can't be resolved
	 * 							or an {@link UnresolvedTableException} if its
	 *                       	table reference can't be resolved.
	 */
	protected DBColumn checkColumnReference(final ColumnReference colRef, final ClauseSelect select, final Stack<CheckContext> contextList) throws ParseException {
		int index = colRef.getColumnIndex();
		if (index > 0 && index <= select.size()) {
			SelectItem item = select.get(index - 1);
			if (item.getOperand() instanceof ADQLColumn)
				return ((ADQLColumn)item.getOperand()).getDBLink();
			else
				return null;
		} else
			throw new ParseException("Column index out of bounds: " + index + " (must be between 1 and " + select.size() + ") !", colRef.getPosition());
	}

	/**
	 * Generate a {@link DBTable} corresponding to the given sub-query with the
	 * given table name. This {@link DBTable} will contain all {@link DBColumn}
	 * returned by {@link ADQLQuery#getResultingColumns()}.
	 *
	 * @param subQuery	Sub-query in which the specified table must be searched.
	 * @param tableName	Name of the table to search.
	 *                 	<i>If between double quotes, the table name will be
	 *                 	considered as case sensitive.</i>
	 *
	 * @return	The corresponding {@link DBTable} if the table has been found in
	 *        	the given sub-query,
	 *        	or NULL otherwise.
	 *
	 * @throws ParseException	Can be used to explain why the table has not
	 *                       	been found. <i>Not used by default.</i>
	 */
	public static DBTable generateDBTable(final ADQLSet subQuery, final String tableName) throws ParseException {
		// Create default DB meta:
		DefaultDBTable dbTable = new DefaultDBTable((DefaultDBTable.isDelimited(tableName) ? tableName : tableName.toLowerCase()));

		// Fetch all available columns:
		DBColumn[] columns = subQuery.getResultingColumns();

		// Add all available columns:
		for(DBColumn dbCol : columns)
			dbTable.addColumn(dbCol.copy(dbCol.getDBName(), DBIdentifier.denormalize(dbCol.getADQLName(), dbCol.isCaseSensitive()), dbTable));

		return dbTable;
	}

	/**
	 * Generate a {@link DBTable} corresponding to the given
	 * Common Table Expression (i.e. CTE = item of a WITH clause).
	 *
	 * <p>
	 * 	This {@link DBTable} will contain all {@link DBColumn}s returned by
	 * 	{@link WithItem#getResultingColumns()}.
	 * </p>
	 *
	 * @param withItem	CTE declaration.
	 *
	 * @return	The corresponding {@link DBTable},
	 *        	or NULL otherwise.
	 *
	 * @since 2.0
	 */
	public static DBTable generateDBTable(final WithItem withItem) {
		// Create default DB meta:
		DefaultDBTable dbTable = new DefaultDBTable((withItem.isLabelCaseSensitive() ? withItem.getLabel() : withItem.getLabel().toLowerCase()));
		dbTable.setCaseSensitive(withItem.isLabelCaseSensitive());

		// Fetch all available columns:
		DBColumn[] columns = withItem.getResultingColumns();

		// Add all available columns:
		for(DBColumn dbCol : columns)
			dbTable.addColumn(dbCol.copy(dbCol.getDBName(), DBIdentifier.denormalize(dbCol.getADQLName(), dbCol.isCaseSensitive()), dbTable));

		return dbTable;
	}

	/* **********************************************************************
	   *                   CHECKING METHODS FOR UDFs                        *
	   ********************************************************************** */

	/**
	 * Search all UDFs (User Defined Functions) inside the given query, and then
	 * check their signature against the list of allowed UDFs.
	 *
	 * <p><i><b>Note:</b>
	 * 	When more than one allowed function match, the function is considered as
	 * 	correct and no error is added. However, in case of multiple matches, the
	 * 	return type of matching functions could be different and in this case,
	 * 	there would be an error while checking later the types. In such case,
	 * 	throwing an error could make sense, but the user would then need to cast
	 * 	some parameters to help the parser identifying the right function.
	 * 	But the type-casting ability is not yet possible in ADQL.
	 * </i></p>
	 *
	 * @param query		Query in which UDFs must be checked.
	 * @param errors	List of errors to complete in this function each time a
	 *              	UDF does not match to any of the allowed UDFs.
	 *
	 * @since 1.3
	 */
	protected void checkUDFs(final ADQLQuery query, final UnresolvedIdentifiersException errors) {
		// 1. Search all UDFs:
		ISearchHandler sHandler = new SearchUDFHandler();
		sHandler.search(query);

		// If no UDF are allowed, throw immediately an error:
		if (allowedUdfs.length == 0) {
			for(ADQLObject result : sHandler)
				errors.addException(new UnresolvedFunctionException((UserDefinedFunction)result));
		}
		// 2. Try to resolve all of them:
		else {
			ArrayList<UserDefinedFunction> toResolveLater = new ArrayList<UserDefinedFunction>();
			UserDefinedFunction udf;
			int match;
			BinarySearch<FunctionDef, UserDefinedFunction> binSearch = new BinarySearch<FunctionDef, UserDefinedFunction>() {
				@Override
				protected int compare(UserDefinedFunction searchItem, FunctionDef arrayItem) {
					return arrayItem.compareTo(searchItem) * -1;
				}
			};

			// Try to resolve all the found UDFs:
			/* Note: at this stage, it can happen that UDFs can not be yet resolved because the building of
			 *       their signature depends of other UDFs. That's why, these special cases should be kept
			 *       for a later resolution try. */
			for(ADQLObject result : sHandler) {
				udf = (UserDefinedFunction)result;
				// if the type of not all parameters are resolved, postpone the resolution:
				if (!isAllParamTypesResolved(udf))
					toResolveLater.add(udf);
				// otherwise:
				else {
					// search for a match:
					match = binSearch.search(udf, allowedUdfs);
					// if no match...
					if (match < 0)
						errors.addException(new UnresolvedFunctionException(udf));
					// if there is a match, metadata can be attached:
					else
						udf.setDefinition(allowedUdfs[match]);
				}
			}

			// Try to resolve UDFs whose some parameter types are depending of other UDFs:
			/* Note: we need to iterate from the end in order to resolve first the most wrapped functions
			 *       (e.g. fct1(fct2(...)) ; fct2 must be resolved before fct1). */
			for(int i = toResolveLater.size() - 1; i >= 0; i--) {
				udf = toResolveLater.get(i);
				// search for a match:
				match = binSearch.search(udf, allowedUdfs);
				// if no match, add an error:
				if (match < 0)
					errors.addException(new UnresolvedFunctionException(udf));
				// otherwise, metadata can be attached:
				else
					udf.setDefinition(allowedUdfs[match]);
			}

			// 3. Replace all the resolved DefaultUDF by an instance of the class associated with the set signature:
			(new ReplaceDefaultUDFHandler(errors)).searchAndReplace(query);
		}
	}

	/**
	 * Tell whether the type of all parameters of the given ADQL function
	 * is resolved.
	 *
	 * <p>A parameter type may not be resolved for 2 main reasons:</p>
	 * <ul>
	 * 	<li>the parameter is a <b>column</b>, but this column has not been
	 * 		successfully resolved. Thus its type is still unknown.</li>
	 * 	<li>the parameter is a <b>UDF</b>, but this UDF has not been already
	 * 		resolved. Thus, as for the column, its return type is still unknown.
	 * 		But it could be known later if the UDF is resolved later ; a second
	 * 		try should be done afterwards.</li>
	 * </ul>
	 *
	 * @param fct	ADQL function whose the parameters' type should be checked.
	 *
	 * @return	<code>true</code> if the type of all parameters is known,
	 *        	<code>false</code> otherwise.
	 *
	 * @since 1.3
	 */
	protected final boolean isAllParamTypesResolved(final ADQLFunction fct) {
		for(ADQLOperand op : fct.getParameters()) {
			if (op.isGeometry() == op.isNumeric() && op.isNumeric() == op.isString())
				return false;
		}
		return true;
	}

	/* **********************************************************************
	   *       METHODS CHECKING TYPES UNKNOWN WHILE CHECKING SYNTAX         *
	   ********************************************************************** */

	/**
	 * Search all operands whose the type is not yet known and try to resolve it
	 * now and to check whether it matches the type expected by the syntactic
	 * parser.
	 *
	 * <p>
	 * 	Only two operands may have an unresolved type: columns and user defined
	 * 	functions. Indeed, their type can be resolved only if the list of
	 * 	available columns and UDFs is known, and if columns and UDFs used in the
	 * 	query are resolved successfully.
	 * </p>
	 *
	 * <p>
	 * 	When an operand type is still unknown, they will own the three kinds of
	 * 	type and so this function won't raise an error: it is thus automatically
	 * 	on the expected type. This behavior is perfectly correct because if the
	 * 	type is not resolved that means the item/operand has not been resolved
	 * 	in the previous steps and so that an error about this item has already
	 * 	been raised.
	 * </p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function does not check the types exactly, but just roughly by
	 * 	considering only three categories: string, numeric and geometry.
	 * </i></p>
	 *
	 * @param query		Query in which unknown types must be resolved and
	 *             		checked.
	 * @param errors	List of errors to complete in this function each time a
	 *              	types does not match to the expected one.
	 *
	 * @see UnknownType
	 *
	 * @since 1.3
	 */
	protected void checkTypes(final ADQLQuery query, final UnresolvedIdentifiersException errors) {
		// Search all unknown types:
		ISearchHandler sHandler = new SearchUnknownTypeHandler();
		sHandler.search(query);

		// Check whether their type matches the expected one:
		UnknownType unknown;
		for(ADQLObject result : sHandler) {
			unknown = (UnknownType)result;
			switch(unknown.getExpectedType()) {
				case 'G':
				case 'g':
					if (!unknown.isGeometry())
						errors.addException(new ParseException("Type mismatch! A geometry was expected instead of \"" + unknown.toADQL() + "\".", result.getPosition()));
					break;
				case 'N':
				case 'n':
					if (!unknown.isNumeric())
						errors.addException(new ParseException("Type mismatch! A numeric value was expected instead of \"" + unknown.toADQL() + "\".", result.getPosition()));
					break;
				case 'S':
				case 's':
					if (!unknown.isString())
						errors.addException(new ParseException("Type mismatch! A string value was expected instead of \"" + unknown.toADQL() + "\".", result.getPosition()));
					break;
			}
		}
	}

	/* **********************************************************************
	   *               METHODS CHECKING THE SUB-QUERIES                     *
	   ********************************************************************** */

	/**
	 * Search for all sub-queries found in the given query but not in the clause
	 * FROM. These sub-queries are then checked using
	 * {@link #check(ADQLSet, Stack)}.
	 *
	 *
	 * @param query				Query in which sub-queries must be checked.
	 * @param contextList		Each item of this stack represents a recursion
	 *                   		level inside the main ADQL query. A such item
	 *                   		contains the list of columns and tables
	 *                   		available at this level.
	 * @param errors			List of errors to complete in this function each
	 *              			time a semantic error is encountered.
	 *
	 * @since 1.3
	 */
	protected void checkSubQueries(final ADQLQuery query, final Stack<CheckContext> contextList, final UnresolvedIdentifiersException errors) {
		// Check sub-queries outside the clause FROM:
		ISearchHandler sHandler = new SearchSubQueryHandler();
		sHandler.search(query);
		if (sHandler.getNbMatch() > 0) {

			// Check each found sub-query:
			for(ADQLObject result : sHandler) {
				try {
					check((ADQLQuery)result, contextList);
				} catch(UnresolvedIdentifiersException uie) {
					Iterator<ParseException> itPe = uie.getErrors();
					while(itPe.hasNext())
						errors.addException(itPe.next());
				}
			}
		}
	}

	/* **********************************************************************
	   *                        SEARCH HANDLERS                             *
	   ********************************************************************** */

	/**
	 * Lets searching all {@link ADQLColumn} in the given object,
	 * EXCEPT in the GROUP BY and ORDER BY clauses.
	 *
	 * <p>
	 * 	{@link ADQLColumn}s of the GROUP BY and ORDER BY may be aliases and so,
	 * 	they can not be checked exactly as a normal column.
	 * </p>
	 *
	 * <p>
	 * 	{@link ADQLColumn} of a {@link ColumnReference} may be an alias, they
	 * 	can not be checked exactly as a normal column.
	 * </p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI;CDS)
	 * @version 2.0 (08/2019)
	 * @since 1.4
	 */
	private static class SearchColumnOutsideGroupByHandler extends SearchColumnHandler {
		@Override
		protected boolean goInto(final ADQLObject obj) {
			if (obj instanceof ClauseADQL<?> && ((ClauseADQL<?>)obj).getName() != null) {
				ClauseADQL<?> clause = (ClauseADQL<?>)obj;
				return !(clause.getName().equalsIgnoreCase("GROUP BY") || clause.getName().equalsIgnoreCase("ORDER BY"));
			} else
				return super.goInto(obj);
		}
	}

	/**
	 * Lets searching all tables.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 1.0 (07/2011)
	 */
	private static class SearchTableHandler extends SimpleSearchHandler {
		@Override
		public boolean match(final ADQLObject obj) {
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
		public boolean match(final ADQLObject obj) {
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
		public boolean match(final ADQLObject obj) {
			return (obj instanceof ColumnReference);
		}
	}

	/**
	 * Lets searching subqueries in every clause except the WITH and FROM ones
	 * (hence the modification of the {@link #goInto(ADQLObject)}.</p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The function {@link #addMatch(ADQLObject, ADQLIterator)} has been
	 * 	modified in order to not have the root search object (here: the main
	 * 	query) in the list of results.
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI;CDS)
	 * @version 2.0 (08/2019)
	 * @since 1.2
	 */
	private static class SearchSubQueryHandler extends SimpleSearchHandler {
		@Override
		protected void addMatch(ADQLObject matchObj, ADQLIterator it) {
			if (it != null)
				super.addMatch(matchObj, it);
		}

		@Override
		protected boolean goInto(ADQLObject obj) {
			return super.goInto(obj) && !(obj instanceof FromContent) && !(obj instanceof ClauseADQL && "WITH".equals(obj.getName()));
		}

		@Override
		protected boolean match(ADQLObject obj) {
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
		protected boolean match(ADQLObject obj) {
			return (obj instanceof UserDefinedFunction);
		}
	}

	/**
	 * Let replacing every {@link UserDefinedFunction}s whose a
	 * {@link FunctionDef} is set by their corresponding custom
	 * {@link UserDefinedFunction} extension.
	 *
	 * <p><i><b>Important note:</b>
	 * 	If the replacer can not be created using the class returned by
	 * 	{@link FunctionDef#getUDFClass()}, no replacement is performed.
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.0 (05/2021)
	 * @since 1.3
	 */
	private static class ReplaceDefaultUDFHandler extends SimpleReplaceHandler {
		private final UnresolvedIdentifiersException errors;

		public ReplaceDefaultUDFHandler(final UnresolvedIdentifiersException errorsContainer) {
			errors = errorsContainer;
		}

		@Override
		protected boolean match(ADQLObject obj) {
			return (obj instanceof UserDefinedFunction) && (((UserDefinedFunction)obj).getDefinition() != null) && (((UserDefinedFunction)obj).getDefinition().getUDFClass() != null);
		}

		@Override
		protected ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException {
			final UserDefinedFunction udf = ((UserDefinedFunction)objToReplace);
			try {
				return udf.getDefinition().createUDF(udf.getParameters());
			} catch(Exception ex) {
				// IF NO INSTANCE CAN BE CREATED...
				// ...keep the error for further report:
				errors.addException(new UnresolvedFunctionException("Impossible to represent the function \"" + udf.getName() + "\": the following error occured while creating this representation: \"" + ((ex instanceof InvocationTargetException) ? "[" + ex.getCause().getClass().getSimpleName() + "] " + ex.getCause().getMessage() : ex.getMessage()) + "\"", udf));
				// ...keep the same object (i.e. no replacement):
				return objToReplace;
			}
		}
	}

	/**
	 * Let searching all ADQL objects whose the type was not known while
	 * checking the syntax of the ADQL query. These objects are
	 * {@link ADQLColumn}s and {@link UserDefinedFunction}s.
	 *
	 * <p><i><b>Important note:</b>
	 * 	Only {@link UnknownType} instances having an expected type equals to 'S'
	 * 	(or 's' ; for string) or 'N' (or 'n' ; for numeric) are kept by this
	 * 	handler. Others are ignored.
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 */
	private static class SearchUnknownTypeHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj) {
			if (obj instanceof UnknownType) {
				char expected = ((UnknownType)obj).getExpectedType();
				return (expected == 'G' || expected == 'g' || expected == 'S' || expected == 's' || expected == 'N' || expected == 'n');
			} else
				return false;
		}
	}

	/**
	 * Implement the binary search algorithm over a sorted array.
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
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.0 (10/2024)
	 *
	 * @param <T>	Type of items stored in the array.
	 * @param <S>	Type of the item to search.
	 *
	 * @since 1.3
	 */
	protected static abstract class BinarySearch<T, S> {

		/**
		 * Search the given item in the given array.
		 *
		 * <p>
		 * 	In case the given object matches to several items of the array,
		 * 	this function will return the smallest index, pointing thus to the
		 * 	first of all matches.
		 * </p>
		 *
		 * @param searchItem	Object for which a corresponding array item must
		 *                  	be searched.
		 * @param array			Array in which the given object must be searched.
		 *
		 * @return	The array index of the first item of all matches.
		 */
		public int search(final S searchItem, final T[] array) {
			int s = 0;
			int e = array.length - 1;
			while(s < e) {
				// middle of the sorted array:
				int m = s + ((e - s) / 2);
				// compare the fct with the middle item of the array:
				int comp = compare(searchItem, array[m]);
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

	/* **********************************************************************
	   *                DEPRECATED STUFF ABOUT GEOMETRIES                   *
	   ********************************************************************** */

	/** List of all allowed geometrical functions (i.e. CONTAINS, REGION, POINT,
	 * COORD2, ...).
	 *
	 * <p>
	 * 	If this list is NULL, all geometrical functions are allowed.
	 * 	However, if not, all items of this list must be the only allowed
	 * 	geometrical functions. So, if the list is empty, no such function is
	 * 	allowed.
	 * </p>
	 *
	 * @since 1.3
	 * @deprecated Since v2.0, supported geometrical functions must be declared
	 *             in ADQLParser. */
	@Deprecated
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
	 * @since 1.3
	 * @deprecated Since v2.0, supported coordinate systems must be declared
	 *             in ADQLParser. */
	@Deprecated
	protected String[] allowedCoordSys = null;

	/** <p>A regular expression built using the list of allowed coordinate systems.
	 * With this regex, it is possible to known whether a coordinate system expression is allowed or not.</p>
	 * <p>If NULL, all coordinate systems are allowed.</p>
	 * @since 1.3
	 * @deprecated Since v2.0, supported coordinate systems must be declared
	 *             in ADQLParser. */
	@Deprecated
	protected String coordSysRegExp = null;

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
	 * @deprecated	Since v2.0, the check of geometrical functions support is
	 *            	performed in ADQLParser. It must now be done with
	 *            	{@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}
	 * 	          	(see also {@link adql.parser.feature.FeatureSet FeatureSet}).
	 */
	@Deprecated
	public DBChecker(final Collection<? extends DBTable> tables, final Collection<String> allowedGeoFcts, final Collection<String> allowedCoordSys) throws ParseException {
		this(tables, null, allowedGeoFcts, allowedCoordSys);
	}

	/**
	 * <p>Builds a {@link DBChecker}.</p>
	 *
	 * <p>Verifications done by this object after creation:</p>
	 * <ul>
	 * 	<li>Existence of tables and columns:            <b>OK</b></li>
	 * 	<li>Existence of User Defined Functions (UDFs): <b>OK</b></li>
	 * 	<li>Support of coordinate systems:              <b>OK</b></li>
	 * </ul>
	 *
	 * <p><i><b>IMPORTANT note:</b>
	 * 	Since v2.0, the check of supported geometrical functions is performed
	 * 	directly in ADQLParser through the notion of Optional Features.
	 * 	The declaration of supported geometrical functions must now be done
	 * 	with {@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}
	 * 	(see also {@link adql.parser.feature.FeatureSet FeatureSet}).
	 * </i></p>
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
	 * @since 2.0
	 * @deprecated	Since v2.0, the check of geometrical functions support is
	 *            	performed in ADQLParser. It must now be done with
	 *            	{@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}
	 * 	          	(see also {@link adql.parser.feature.FeatureSet FeatureSet}).
	 */
	@Deprecated
	public DBChecker(final Collection<? extends DBTable> tables, final Collection<? extends FunctionDef> allowedUdfs, final Collection<String> allowedGeoFcts, final Collection<String> allowedCoordSys) throws ParseException {
		// Set the list of available tables + Set the list of all known UDFs:
		this(tables, allowedUdfs);

		// Set the list of allowed geometrical functions:
		allowedGeo = specialSort(allowedGeoFcts);

		// Set the list of allowed coordinate systems:
		this.allowedCoordSys = specialSort(allowedCoordSys);
		coordSysRegExp = CoordSys.buildCoordSysRegExp(this.allowedCoordSys);
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
	 *
	 * @deprecated	Since v2.0, this tool function is no longer used. It was
	 *            	useful only to collect allowed geometries and coordinate
	 *            	systems....but these are now checked by
	 *            	{@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	protected static String[] specialSort(final Collection<String> items) {
		// Nothing to do if the array is NULL:
		if (items == null)
			return null;

		// Keep only valid items (not NULL and not empty string):
		String[] tmp = new String[items.size()];
		int cnt = 0;
		for(String item : items) {
			if (item != null && !item.trim().isEmpty())
				tmp[cnt++] = item;
		}

		// Make an adjusted array copy:
		String[] copy = new String[cnt];
		System.arraycopy(tmp, 0, copy, 0, cnt);

		// Sort the values:
		Arrays.sort(copy);

		return copy;
	}

	/**
	 * <p>Check all geometries.</p>
	 *
	 * <p>Operations done in this function:</p>
	 * <ol>
	 * 	<li>Check that all explicit (string constant) coordinate system definitions are supported</i></li>
	 * 	<li>Check all STC-S expressions (only in {@link RegionFunction} for the moment) and
	 * 	    Apply the 2 previous checks on them</li>
	 * </ol>
	 *
	 * <p><i><b>IMPORTANT note:</b>
	 * 	Since v2.0, the check of supported geometrical functions is performed
	 * 	directly in ADQLParser through the notion of Optional Features.
	 * 	The declaration of supported geometrical functions must now be done
	 * 	with {@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}
	 * 	(see also {@link adql.parser.feature.FeatureSet FeatureSet}).
	 * </i></p>
	 *
	 * @param query		Query in which geometries must be checked.
	 * @param errors	List of errors to complete in this function each time a geometry item is not supported.
	 *
	 * @see #resolveCoordinateSystems(ADQLQuery, UnresolvedIdentifiersException)
	 * @see #resolveSTCSExpressions(ADQLQuery, BinarySearch, UnresolvedIdentifiersException)
	 *
	 * @since 1.3
	 *
	 * @deprecated	Since 2.0, validation of the geometric functions is
	 *            	performed automatically by
	 *            	{@link adql.parser.ADQLParser ADQLParser}. Geometric
	 *            	functions are optional features and should be declared as
	 *            	such in the {@link adql.parser.ADQLParser ADQLParser} if
	 *            	they are supported (see
	 *            	{@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}).
	 */
	@Deprecated
	protected void checkGeometries(final ADQLQuery query, final UnresolvedIdentifiersException errors) {
		BinarySearch<String, String> binSearch = new BinarySearch<String, String>() {
			@Override
			protected int compare(String searchItem, String arrayItem) {
				return searchItem.compareToIgnoreCase(arrayItem);
			}
		};

		// a. Check whether the coordinate systems are allowed:
		if (allowedCoordSys != null)
			resolveCoordinateSystems(query, errors);

		// b. Check all STC-S expressions (in RegionFunctions only) + the used coordinate systems (if StringConstant only):
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
	 *
	 * @deprecated	Since 2.0, validation of the geometric functions is
	 *            	performed automatically by
	 *            	{@link adql.parser.ADQLParser ADQLParser}. Geometric
	 *            	functions are optional features and should be declared as
	 *            	such in the {@link adql.parser.ADQLParser ADQLParser} if
	 *            	they are supported (see
	 *            	{@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}).
	 */
	@Deprecated
	protected final void resolveGeometryFunctions(final ADQLQuery query, final BinarySearch<String, String> binSearch, final UnresolvedIdentifiersException errors) {
		ISearchHandler sHandler = new SearchGeometryHandler();
		sHandler.search(query);

		String fctName;
		for(ADQLObject result : sHandler) {
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
	 *
	 * @deprecated	Since 2.0, validation of the geometric functions is
	 *            	performed automatically by
	 *            	{@link adql.parser.ADQLParser ADQLParser}. Geometric
	 *            	functions are optional features and should be declared as
	 *            	such in the {@link adql.parser.ADQLParser ADQLParser} if
	 *            	they are supported (see
	 *            	{@link adql.parser.ADQLParser#getSupportedFeatures() ADQLParser.getSupportedFeatures()}).
	 */
	@Deprecated
	protected final void checkGeometryFunction(final String fctName, final ADQLFunction fct, final BinarySearch<String, String> binSearch, final UnresolvedIdentifiersException errors) {
		int match = -1;
		if (allowedGeo.length != 0)
			match = binSearch.search(fctName, allowedGeo);
		if (match < 0)
			errors.addException(new UnresolvedFunctionException("The geometrical function \"" + fctName + "\" is not available in this implementation!", fct));
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
	 *
	 * @deprecated	Since 2.0, the validation of coordinate systems is performed
	 *            	automatically by {@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	protected void resolveCoordinateSystems(final ADQLQuery query, final UnresolvedIdentifiersException errors) {
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
	 *
	 * @deprecated	Since 2.0, the validation of coordinate systems is performed
	 *            	automatically by {@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	protected void checkCoordinateSystem(final StringConstant adqlCoordSys, final UnresolvedIdentifiersException errors) {
		String coordSysStr = adqlCoordSys.getValue();
		try {
			checkCoordinateSystem(STCS.parseCoordSys(coordSysStr), adqlCoordSys, errors);
		} catch(ParseException pe) {
			errors.addException(new ParseException(pe.getMessage(), adqlCoordSys.getPosition()));
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
	 *
	 * @deprecated	Since 2.0, the validation of coordinate systems is performed
	 *            	automatically by {@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	protected void checkCoordinateSystem(final CoordSys coordSys, final ADQLOperand operand, final UnresolvedIdentifiersException errors) {
		if (coordSysRegExp != null && coordSys != null && !coordSys.toFullSTCS().matches(coordSysRegExp)) {
			StringBuilder buf = new StringBuilder();
			if (allowedCoordSys != null) {
				for(String cs : allowedCoordSys) {
					if (buf.length() > 0)
						buf.append(", ");
					buf.append(cs);
				}
			}
			if (buf.length() == 0)
				buf.append("No coordinate system is allowed!");
			else
				buf.insert(0, "Allowed coordinate systems are: ");
			errors.addException(new ParseException("Coordinate system \"" + ((operand instanceof StringConstant) ? ((StringConstant)operand).getValue() : coordSys.toString()) + "\" (= \"" + coordSys.toFullSTCS() + "\") not allowed in this implementation. " + buf.toString(), operand.getPosition()));
		}
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
	 *
	 * @deprecated	Since 2.0, the validation of STCs expressions is performed
	 *            	automatically by {@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	protected void resolveSTCSExpressions(final ADQLQuery query, final BinarySearch<String, String> binSearch, final UnresolvedIdentifiersException errors) {
		// Search REGION functions:
		ISearchHandler sHandler = new SearchRegionHandler();
		sHandler.search(query);

		// Parse and check their STC-S expression:
		String stcs;
		Region region;
		for(ADQLObject result : sHandler) {
			try {
				// get the STC-S expression:
				stcs = ((StringConstant)((RegionFunction)result).getParameter(0)).getValue();

				// parse the STC-S expression (and so check the syntax):
				region = STCS.parseRegion(stcs);

				// check whether the regions (this one + the possible inner ones) and the coordinate systems are allowed:
				checkRegion(region, (RegionFunction)result, binSearch, errors);
			} catch(ParseException pe) {
				errors.addException(new ParseException(pe.getMessage(), result.getPosition()));
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
	 * @see #checkRegion(Region, RegionFunction, BinarySearch, UnresolvedIdentifiersException)
	 *
	 * @since 1.3
	 *
	 * @deprecated	Since 2.0, the validation of REGIONs is performed
	 *            	automatically by {@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	protected void checkRegion(final Region r, final RegionFunction fct, final BinarySearch<String, String> binSearch, final UnresolvedIdentifiersException errors) {
		if (r == null)
			return;

		// Check the coordinate system (if any):
		if (r.coordSys != null)
			checkCoordinateSystem(r.coordSys, fct, errors);

		// Check that the region type is allowed:
		if (allowedGeo != null) {
			if (allowedGeo.length == 0)
				errors.addException(new UnresolvedFunctionException("The region type \"" + r.type + "\" is not available in this implementation!", fct));
			else
				checkGeometryFunction((r.type == Region.RegionType.POSITION) ? "POINT" : r.type.toString(), fct, binSearch, errors);
		}

		// Check all the inner regions:
		if (r.regions != null) {
			for(Region innerR : r.regions)
				checkRegion(innerR, fct, binSearch, errors);
		}
	}

	/**
	 * Let searching geometrical functions.
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 *
	 * @deprecated	Since 2.0.
	 */
	@Deprecated
	private static class SearchGeometryHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj) {
			return (obj instanceof GeometryFunction);
		}
	}

	/**
	 * Let searching all explicit declaration of coordinate systems.
	 * So, only {@link StringConstant} objects will be returned.
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 * @deprecated	Since 2.0, the validation of REGIONs is performed
	 *            	automatically by {@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	private static class SearchCoordSysHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj) {
			if (obj instanceof PointFunction || obj instanceof BoxFunction || obj instanceof CircleFunction || obj instanceof PolygonFunction)
				return (((GeometryFunction)obj).getCoordinateSystem() instanceof StringConstant);
			else
				return false;
		}

		@Override
		protected void addMatch(ADQLObject matchObj, ADQLIterator it) {
			results.add(((GeometryFunction)matchObj).getCoordinateSystem());
		}

	}

	/**
	 * Let searching all {@link RegionFunction}s.
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 1.3 (10/2014)
	 * @since 1.3
	 * @deprecated	Since 2.0, the validation of REGIONs is performed
	 *            	automatically by {@link adql.parser.ADQLParser ADQLParser}.
	 */
	@Deprecated
	private static class SearchRegionHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj) {
			if (obj instanceof RegionFunction)
				return (((RegionFunction)obj).getParameter(0) instanceof StringConstant);
			else
				return false;
		}

	}
}
