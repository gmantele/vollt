package adql.parser.grammar;

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

import java.io.InputStream;
import java.util.Stack;

import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.ADQLQueryFactory;
import adql.query.ADQLQuery;
import adql.query.TextPosition;

/**
 * Common partial implementation of an {@link ADQLGrammar}.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (08/2019)
 * @since 2.0
 */
public abstract class ADQLGrammarBase implements ADQLGrammar {

	/** Tool to build the object representation of the ADQL query. */
	protected ADQLQueryFactory queryFactory = new ADQLQueryFactory();

	/** The object representation of the ADQL query to parse.
	 * (ONLY USED DURING THE PARSING, otherwise it is always NULL). */
	protected ADQLQuery query = null;

	/** The stack of queries (in order to deal with sub-queries). */
	protected Stack<ADQLQuery> stackQuery = new Stack<ADQLQuery>();

	/* **********************************************************************
	   *                         GETTERS/SETTERS                            *
	   ********************************************************************** */

	@Override
	public final ADQLQuery getQuery() {
		return query;
	}

	@Override
	public final ADQLQueryFactory getQueryFactory() {
		return queryFactory;
	}

	@Override
	public final void setQueryFactory(final ADQLQueryFactory factory) throws NullPointerException {
		if (factory == null)
			throw new NullPointerException("Missing new ADQLQueryFactory! An ADQLGrammar can not work without a query factory.");
		this.queryFactory = factory;
	}

	/* **********************************************************************
	   *                      PARSER INITIALIZATION                         *
	   ********************************************************************** */

	@Override
	public final void reset(final InputStream inputADQLExpression) throws Exception {
		// Error if no input:
		if (inputADQLExpression == null)
			throw new NullPointerException("Missing ADQL expression to parse!");

		// Empty the stack:
		stackQuery.clear();

		// Create a new and empty ADQL query tree:
		query = queryFactory.createQuery(getVersion());

		// Finally re-initialize the parser with the expression to parse:
		ReInit(inputADQLExpression);
	}

	/**
	 * Re-initialize the input of the ADQL grammar parser.
	 *
	 * @param stream	The new input stream to parse.
	 */
	public abstract void ReInit(InputStream stream);

	/* **********************************************************************
	   *                     REGULAR IDENTIFIER TEST                        *
	   ********************************************************************** */

	@Override
	public final boolean isRegularIdentifier(final String idCandidate) {
		return idCandidate != null && idCandidate.matches("[a-zA-Z]+[a-zA-Z0-9_]*");
	}

	@Override
	public final void testRegularIdentifier(final Token token) throws ParseException {
		if (token == null)
			throw new ParseException("Impossible to test whether NULL is a valid ADQL regular identifier!");
		else if (!isRegularIdentifier(token.image)) {
			String message = "Invalid ADQL regular identifier: \u005c"" + token.image + "\u005c"!";
			if (getVersion() == ADQLVersion.V2_0 && token.image.matches("0[Xx][0-9a-fA-F]+"))
				message += " HINT: hexadecimal values are not supported in ADQL-2.0. You should change the grammar version of the ADQL parser to at least ADQL-2.1.";
			else
				message += " HINT: If it aims to be a column/table name/alias, you should write it between double quotes.";
			throw new ParseException(message, new TextPosition(token));
		}
	}

	/* **********************************************************************
	   *                        TOKEN KIND TESTS                            *
	   ********************************************************************** */

	@Override
	public boolean isEnd(final Token token) {
		return token == null || token.kind == ADQLGrammar200Constants.EOF || token.kind == ADQLGrammar200Constants.EOQ;
	}

	/* **********************************************************************
	   *                     DEBUG & ERRORS MANAGEMENT                      *
	   ********************************************************************** */

	@Override
	public final ParseException generateParseException(Exception ex) {
		if (!(ex instanceof ParseException)) {
			ParseException pex = new ParseException("[" + ex.getClass().getName() + "] " + ex.getMessage());
			pex.setStackTrace(ex.getStackTrace());
			return pex;
		} else
			return (ParseException)ex;
	}
}
