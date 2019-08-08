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
 * Copyright 2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import adql.parser.ADQLParserFactory.ADQLVersion;
import adql.parser.feature.FeatureSet;
import adql.query.ADQLOrder;
import adql.query.ADQLQuery;
import adql.query.ClauseADQL;
import adql.query.ClauseConstraints;
import adql.query.ClauseSelect;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;

/**
 * TODO
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (07/2019)
 * @since 2.0
 */
public interface ADQLParser {

	/* **********************************************************************
	 *                           GETTERS & SETTERS
	 * ********************************************************************** */

	public ADQLVersion getADQLVersion();

	public QueryChecker getQueryChecker();

	public void setQueryChecker(final QueryChecker checker);

	public ADQLQueryFactory getQueryFactory();

	public void setQueryFactory(final ADQLQueryFactory factory);

	public FeatureSet getSupportedFeatures();

	public void setSupportedFeatures(final FeatureSet features);

	/* **********************************************************************
	 *                         PARSING DEBUG FUNCTIONS
	 * ********************************************************************** */

	public void setDebug(final boolean debug);

	/* **********************************************************************
	 *                           PARSING FUNCTIONS
	 * ********************************************************************** */

	public void ReInit(InputStream stream);

	public void ReInit(Reader reader);

	public ADQLQuery parseQuery() throws ParseException;

	public ADQLQuery parseQuery(final String query) throws ParseException;

	public ADQLQuery parseQuery(final InputStream stream) throws ParseException;

	public ClauseSelect parseSelect(final String adql) throws ParseException;

	public FromContent parseFrom(final String adql) throws ParseException;

	public ClauseConstraints parseWhere(final String adql) throws ParseException;

	public ClauseADQL<ADQLOrder> parseOrderBy(final String adql) throws ParseException;

	public ClauseADQL<ADQLColumn> parseGroupBy(final String adql) throws ParseException;

	/* **********************************************************************
	 *                           AUTO-FIX FUNCTIONS
	 * ********************************************************************** */

	public String tryQuickFix(final InputStream input) throws IOException, ParseException;

	public String tryQuickFix(final String adqlQuery) throws ParseException;

	/**
	 * Tell whether the given string is a valid ADQL regular identifier.
	 *
	 * <p>
	 * 	According to the ADQL grammar, a regular identifier (i.e. not
	 * 	delimited ; not between double quotes) must be a letter followed by a
	 * 	letter, digit or underscore. So, the following regular expression:
	 * </p>
	 *
	 * <pre>
	 * [a-zA-Z]+[a-zA-Z0-9_]*
	 * </pre>
	 *
	 * <p>
	 * 	This is what this function tests on the given string.
	 * </p>
	 *
	 * @param idCandidate	The string to test.
	 *
	 * @return	<code>true</code> if the given string is a valid regular
	 *        	identifier,
	 *        	<code>false</code> otherwise.
	 *
	 * @see #testRegularIdentifier(adql.parser.Token)
	 */
	public boolean isRegularIdentifier(final String idCandidate);

	public void testRegularIdentifier(final Token token) throws ParseException;

	/* **********************************************************************
	 *                     FUNCTIONS JUST FOR JUNIT
	 * ********************************************************************** */

	ADQLOperand StringExpression() throws ParseException;

	public Token[] tokenize(final String expr) throws ParseException;

}
