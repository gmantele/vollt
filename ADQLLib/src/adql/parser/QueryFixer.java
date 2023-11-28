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

import java.util.HashMap;
import java.util.Map;

import adql.parser.grammar.ADQLGrammar;
import adql.parser.grammar.ADQLGrammar.Tokenizer;
import adql.parser.grammar.ParseException;
import adql.parser.grammar.Token;
import adql.parser.grammar.TokenMgrError;

/**
 * Tool able to fix some common errors in ADQL queries.
 *
 * <p><i>See {@link #fix(String)} for more details.</i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (10/2019)
 *
 * @since 2.0
 *
 * @see ADQLParser
 */
public class QueryFixer {

	/** The used internal ADQL grammar parser. */
	protected final ADQLGrammar grammarParser;

	/**
	* All of the most common Unicode confusable characters and their
	* ASCII/UTF-8 alternative.
	*
	* <p>
	* 	Keys of this map represent the ASCII character while the values are the
	* 	regular expression for all possible Unicode alternatives.
	* </p>
	*
	* <p><i><b>Note:</b>
	* 	All of them have been listed using
	* 	<a href="https://unicode.org/cldr/utility/confusables.jsp">Unicode Utilities: Confusables</a>.
	* </i></p>
	*/
	protected final Map<String, String> mapRegexUnicodeConfusable;

	/** Regular expression matching all Unicode alternatives for <code>-</code>. */
	protected final String REGEX_DASH = "[-\u02d7\u06d4\u2010\u2011\u2012\u2013\u2043\u2212\u2796\u2cba\ufe58\u2014\u2015\u207b\u208b\u0096\u058a\ufe63\uff0d]";
	/** Regular expression matching all Unicode alternatives for <code>_</code>. */
	protected final String REGEX_UNDERSCORE = "[_\u07fa\ufe4d\ufe4e\ufe4f]";
	/** Regular expression matching all Unicode alternatives for <code>'</code>. */
	protected final String REGEX_QUOTE = "['`\u00b4\u02b9\u02bb\u02bc\u02bd\u02be\u02c8\u02ca\u02cb\u02f4\u0374\u0384\u055a\u055d\u05d9\u05f3\u07f4\u07f5\u144a\u16cc\u1fbd\u1fbf\u1fef\u1ffd\u1ffe\u2018\u2019\u201b\u2032\u2035\ua78c\uff07\uff40]";
	/** Regular expression matching all Unicode alternatives for <code>"</code>. */
	protected final String REGEX_DOUBLE_QUOTE = "[\u02ba\u02dd\u02ee\u02f6\u05f2\u05f4\u1cd3\u201c\u201d\u201f\u2033\u2036\u3003\uff02]";
	/** Regular expression matching all Unicode alternatives for <code>.</code>. */
	protected final String REGEX_STOP = "[.\u0660\u06f0\u0701\u0702\u2024\ua4f8\ua60e]";
	/** Regular expression matching all Unicode alternatives for <code>+</code>. */
	protected final String REGEX_PLUS = "[+\u16ed\u2795]";
	/** Regular expression matching all Unicode alternatives for <code> </code>. */
	protected final String REGEX_SPACE = "[ \u00a0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u202f\u205f\uF0A0]";
	/** Regular expression matching all Unicode alternatives for <code>&lt;</code>. */
	protected final String REGEX_LESS_THAN = "[<\u02c2\u1438\u16b2\u2039\u276e]";
	/** Regular expression matching all Unicode alternatives for <code>&gt;</code>. */
	protected final String REGEX_GREATER_THAN = "[>\u02c3\u1433\u203a\u276f]";
	/** Regular expression matching all Unicode alternatives for <code>=</code>. */
	protected final String REGEX_EQUAL = "[=\u1400\u2e40\u30a0\ua4ff]";

	public QueryFixer(final ADQLGrammar grammar) throws NullPointerException {
		if (grammar == null)
			throw new NullPointerException("Missing ADQL grammar parser!");
		grammarParser = grammar;

		mapRegexUnicodeConfusable = new HashMap<String, String>(10);
		mapRegexUnicodeConfusable.put("-", REGEX_DASH);
		mapRegexUnicodeConfusable.put("_", REGEX_UNDERSCORE);
		mapRegexUnicodeConfusable.put("'", REGEX_QUOTE);
		mapRegexUnicodeConfusable.put("\u005c"", REGEX_DOUBLE_QUOTE);
		mapRegexUnicodeConfusable.put(".", REGEX_STOP);
		mapRegexUnicodeConfusable.put("+", REGEX_PLUS);
		mapRegexUnicodeConfusable.put(" ", REGEX_SPACE);
		mapRegexUnicodeConfusable.put("<", REGEX_LESS_THAN);
		mapRegexUnicodeConfusable.put(">", REGEX_GREATER_THAN);
		mapRegexUnicodeConfusable.put("=", REGEX_EQUAL);
	}

	/**
	* Try fixing tokens/terms of the given ADQL query.
	*
	* <p>
	* 	<b>This function does not try to fix syntactical or semantical errors.</b>
	*  It just try to fix the most common issues in ADQL queries, such as:
	* </p>
	* <ul>
	* 	<li>some Unicode characters confusable with ASCII characters (like a
	* 		space, a dash, ...) ; this function replace them by their ASCII
	* 		alternative,</li>
	* 	<li>any of the following are double quoted:
	* 		<ul>
	* 			<li>non regular ADQL identifiers
	* 				(e.g. <code>_RAJ2000</code>),</li>
	* 			<li>ADQL function names used as identifiers
	* 				(e.g. <code>distance</code>)</li>
	* 			<li>and SQL reserved keywords
	* 				(e.g. <code>public</code>).</li>
	* 		</ul>
	* 	</li>
	* </ul>
	*
	* <p><i><b>Note:</b>
	* 	This function does not use any instance variable of this parser
	* 	(especially the InputStream or Reader provided at initialisation or
	* 	ReInit).
	* </i></p>
	*
	* @param adqlQuery	The input ADQL query to fix.
	*
	* @return	The suggested correction of the given ADQL query.
	*
	* @throws ParseException	If any unrecognised character is encountered,
	*                       	or if anything else prevented the tokenization
	*                       	   of some characters/words/terms.
	*/
	public String fix(String adqlQuery) throws ParseException {
		StringBuffer suggestedQuery = new StringBuffer();

		// 1. Replace all Unicode confusable characters:
		adqlQuery = replaceUnicodeConfusables(adqlQuery);

		/* 1.bis. Normalise new lines and tabulations
		*        (to simplify the column counting): */
		adqlQuery = adqlQuery.replaceAll("(\u005cr\u005cn|\u005cr|\u005cn)", System.getProperty("line.separator")).replaceAll("\u005ct", "    ");

		// 2. Analyse the query token by token:
		Tokenizer tokenizer = grammarParser.getTokenizer(adqlQuery);

		final String[] lines = adqlQuery.split(System.getProperty("line.separator"));

		try {
			String suggestedToken;
			int lastLine = 1, lastCol = 1;

			Token token = null, nextToken = tokenizer.nextToken();
			// for all tokens until the EOF or EOQ:
			do {
				// get the next token:
				token = nextToken;
				nextToken = (grammarParser.isEnd(token) ? null : tokenizer.nextToken());

				// 3. Double quote any suspect token:
				if (mustEscape(token, nextToken)) {
					suggestedToken = "\u005c"" + token.image + "\u005c"";
				} else
					suggestedToken = token.image;

				/* 4. Append all space characters (and comments) before the
				*    token: */
				/* same line, just get the space characters between the last
				* token and the one to append: */
				if (lastLine == token.beginLine) {
					if (grammarParser.isEOF(token))
						suggestedQuery.append(lines[lastLine - 1].substring(lastCol - 1));
					else
						suggestedQuery.append(lines[lastLine - 1].substring(lastCol - 1, token.beginColumn - (grammarParser.isEnd(token) ? 0 : 1)));
					lastCol = token.endColumn + 1;
				}
				// not the same line...
				else {
					/* append all remaining space characters until the position
					* of the token to append: */
					do {
						suggestedQuery.append(lines[lastLine - 1].substring(lastCol - 1)).append('\u005cn');
						lastLine++;
						lastCol = 1;
					} while(lastLine < token.beginLine);
					/* if there are still space characters before the token,
					* append them as well: */
					if (lastCol < token.beginColumn)
						suggestedQuery.append(lines[lastLine - 1].substring(lastCol - 1, token.beginColumn - 1));
					// finally, set the correct column position:
					lastCol = token.endColumn + 1;
				}

				// 5. Append the suggested token:
				suggestedQuery.append(suggestedToken);

			} while(!grammarParser.isEnd(token));

		} catch(TokenMgrError err) {
			// wrap such errors and propagate them:
			throw new ParseException(err);
		}

		return suggestedQuery.toString();
	}

	/**
	* Replace all Unicode characters that can be confused with other ASCI/UTF-8
	* characters (e.g. different spaces, dashes, ...) in their ASCII version.
	*
	* @param adqlQuery	The ADQL query string in which Unicode confusable
	*                 	characters must be replaced.
	*
	* @return	The same query without the most common Unicode confusable
	*        	characters.
	*/
	protected String replaceUnicodeConfusables(final String adqlQuery) {
		String newAdqlQuery = adqlQuery;
		for(java.util.Map.Entry<String, String> confusable : mapRegexUnicodeConfusable.entrySet())
			newAdqlQuery = newAdqlQuery.replaceAll(confusable.getValue(), confusable.getKey());
		return newAdqlQuery;
	}

	/**
	* Tell whether the given token must be double quoted.
	*
	* <p>
	* 	This function considers all the following as terms to double quote:
	* </p>
	* <ul>
	* 	<li>SQL reserved keywords</li>,
	* 	<li>unrecognised regular identifiers (e.g. neither a delimited nor a
	* 		valid ADQL regular identifier)</li>
	* 	<li>and ADQL function name without a parameters list.</li>
	* </ul>
	*
	* @param token		The token to analyze.
	* @param nextToken	The following token. (useful to detect the start of a
	*                 	function's parameters list)
	*
	* @return	<code>true</code> if the given token must be double quoted,
	*        	<code>false</code> to keep it as provided.
	*/
	protected boolean mustEscape(final Token token, final Token nextToken) {
		if (grammarParser.isSQLReservedWord(token))
			return true;
		else if (grammarParser.isRegularIdentifierCandidate(token))
			return !grammarParser.isRegularIdentifier(token.image);
		else
			return token.isFunctionName && !grammarParser.isLeftPar(nextToken);
	}
}
