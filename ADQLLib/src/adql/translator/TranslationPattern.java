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
 * Copyright 2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.text.ParseException;
import java.util.ArrayDeque;

import adql.query.operand.ADQLOperand;
import adql.query.operand.function.ADQLFunction;

/**
 * Tool box for translation patterns.
 *
 * <p><i><b>Note</b>
 * 	No instance of this class can be created. Its usage is only restricted to
 * 	its public static functions.
 * </i></p>
 *
 * <h2>Translation Pattern</h2>
 * <p>
 * 	A "translation pattern" is a string representing a pattern to follow when
 * 	translating an {@link ADQLFunction} into a target language like SQL thanks
 * 	to an {@link ADQLTranslator}.
 * </p>
 *
 * <h2>Validation &amp; Translation</h2>
 * <p>This class provides 2 main features:</p>
 * <dl>
 * 	<dt>{@link #check(String, int)}</dt>
 * 	<dd>
 * 		To check whether a translation pattern is valid considering a given
 * 		maximum number of function parameters.
 * 	</dd>
 * 	<dt>{@link #apply(String, ADQLFunction, ADQLTranslator)}</dt>
 * 	<dd>
 * 		To translate a given ADQL function using a given translation pattern.
 * 	</dd>
 * </dl>
 * <p>
 * 	Behind the hood, both functions actually use the same function:
 * 	{@link #apply(String, ParameterTranslator)}. This is a more generic
 * 	approach that can be used to parse a pattern, check its syntax and resolve
 * 	any argument reference or ternary expression. In the case of
 * 	{@link #check(String, int)}, the argument reference is returned as such.
 * 	{@link #apply(String, ADQLFunction, ADQLTranslator)} however returns the
 * 	exact translation of each argument reference.
 * <p>
 * <p>
 * 	If the proposed applications of {@link #apply(String, ParameterTranslator)}
 * 	do not suit your needs, you should use this function.
 * </p>
 *
 * <h2>Supported syntax</h2>
 * <h3>Referencing a single function parameter</h3>
 * <p>
 * 	This can be done using the character <code>$</code> followed by a positive
 * 	non-null integer.
 * </p>
 * <p><i><b>Examples:</b> <code>$1</code>, <code>$13</code></i></p>
 *
 * <p>
 * 	If <code>0</code>, a negative value or an index greater than the maximum
 * 	number of allowed parameters in the target function, then a
 * 	{@link ParseException} is thrown. Any validation or translation process
 * 	is then interrupted.
 * </p>
 *
 * <h3>Escaping <code>$</code></h3>
 * <p>
 * 	If the <code>$</code> character aims to be a simple character in the
 * 	translation string instead of a prefix for an argument index, it has to be
 * 	escaped by prefixing it with another <code>$</code>.
 * </p>
 * <p><i><b>Example:</b> <code>$$</code></i></p>
 *
 * <h3>Referencing consecutive function parameters</h3>
 * <p>
 * 	It is possible to reference a list of consecutive function parameters by
 * 	suffixing the start index with <code>..</code>. By doing so, the entire
 * 	dollar expression is replaced by all function parameters separated by a
 * 	comma.
 * </p>
 * <i>
 * <p><b>Example:</b>
 * 	the following expressions are equivalent for a function with 4 parameters:
 * </p>
 * <pre>
 * $2..
 * $2, $3, $4</pre>
 * </i>
 *
 * <h3>Ternary conditional expression</h3>
 * <p>
 * 	If the target function has a variable number of parameters, it is possible
 * 	to adapt the translation in function of the available parameters. To do that
 * 	one has to use the following ternary expression:
 * </p>
 * <pre>$IF?{THEN}{ELSE}</pre>
 * <p>, where:</p>
 * <ul>
 * 	<li><code>$IF</code> must be a reference to the <i>single</i> function
 * 		parameter whose existence has to be tested,</li>
 * 	<li><code>THEN</code> should be the translation to apply if <code>$IF</code>
 * 		exists,</li>
 * 	<li><code>ELSE</code> should be the translation to apply if <code>$IF</code>
 * 		does NOT exist.</li>
 * </ul>
 * <p><i><b>Example:</b> <code>$2?{, $2*10}{}</code></i></p>
 *
 * <p>
 * 	As illustrated above, the expression <code>THEN</code> and <code>ELSE</code>
 * 	can include parameter references (single or consecutive) but also other
 * 	ternary conditional expressions.
 * </p>
 * <i>
 * <p><b>Examples:</b></p>
 * <pre>
 * $2?{, $2..}{}
 * $2?{$3?{$2+$2}{$2}}{-1}
 * </pre>
 *
 * <p><b>Warning:</b>
 * 	There is currently no way to escape the character <code>}</code> indicating
 * 	the end of a <code>THEN</code> or <code>ELSE</code> expression.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0
 * @since 2.0
 */
public final class TranslationPattern {

	/** No instance can be created. */
	private TranslationPattern() {
	}

	/**
	 * Representation of the pattern's parser state.
	 *
	 * <p>
	 * 	All these states together can be seen as state machine with the
	 * 	following transition rules:
	 * </p>
	 * <pre>
	 * TEXT, TEXT_THEN, TEXT_ELSE => DOLLAR
	 *
	 * DOLLAR => TEXT, TEXT_THEN, TEXT_ELSE
	 * DOLLAR => ARG_REF
	 *
	 * ARG_REF => TEXT, TEXT_THEN, TEXT_ELSE
	 * ARG_REF => ARG_LIST
	 * ARG_REF => TERNARY_THEN
	 *
	 * ARG_LIST => TEXT, TEXT_THEN, TEXT_ELSE
	 *
	 * TERNARY_THEN => TEXT_THEN
	 *
	 * TEXT_THEN => DOLLAR
	 * TEXT_THEN => TERNARY_ELSE
	 *
	 * TERNARY_ELSE => TEXT_ELSE
	 *
	 * TEXT_ELSE => TEXT, TEXT_THEN, TEXT_ELSE</pre>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.0
	 * @since 2.0
	 */
	private static enum ParsingState {
		/** Normal text. Nothing special to resolve.
		 * Each character must be kept as such. */
		TEXT,

		/** Normal text inside a THEN expression. Nothing special to resolve.
		 * Each character must be kept as such, except a '}' which close the
		 * THEN expression.
		 * <p>
		 * 	<b>At <code>}</code>:</b> character discarded and state replaced by
		 * 	{@link #TERNARY_ELSE}.
		 * </p> */
		TEXT_THEN,

		/** Normal text inside an ELSE expression. Nothing special to resolve.
		 * Each character must be kept as such, except a '}' which close the
		 * ELSE expression.
		 * <p>
		 * 	<b>At <code>}</code>:</b> character discarded and state dropped.
		 * </p> */
		TEXT_ELSE,

		/** The previous character was <code>$</code>.
		 * <p><b>At <code>$</code>:</b>
		 * 	print a single <code>$</code> and state dropped.
		 * </p>
		 * <p><b>At a digit:</b> state replaced by {@link #ARG_REF}.</p>
		 * <p><b>At any other character:</b> error.</p> */
		DOLLAR,

		/** The previous characters were a <code>$</code> followed by at least
		 * one digit.
		 * <p><b>At a digit:</b>
		 * 	keep accumulated digits for the final index
		 * </p>
		 * <p><b>At <code>?</code>:</b>
		 * 	evaluate the argument reference: if existing, ignore the ELSE
		 * 	expression, else, ignore the THEN expression. Finally, state
		 * 	replaced by {@link #ARG_LIST}.
		 * </p>
		 * <p><b>At any other character:</b>
		 * 	evaluate the argument reference, move back the parser one
		 * 	character before, and state dropped.
		 * </p> */
		ARG_REF,

		/**
		 * The previous characters were a <code>$</code> followed by at least
		 * one digit and ending with exactly one <code>.</code>.
		 * <p><b>At <code>.</code>:</b>
		 * 	evaluate the argument list</p>
		 * <p><b>At any other character:</b>
		 * 	evaluate the argument index, print one <code>.</code>, come back one
		 * 	character before and state dropped.
		 * </p>
		 */
		ARG_LIST,

		/**
		 * The previous character was a <code>?</code>.
		 * <p><b>At <code>{</code>:</b>
		 * 	character discarded and state replaced by {@link #TEXT_THEN}.
		 * </p>
		 * <p><b>At any other character:</b> error.</p>
		 */
		TERNARY_THEN,

		/**
		 * The previous character was a <code>}</code> while being in the state
		 * {@link #TEXT_THEN}.
		 * <p><b>At <code>{</code>:</b>
		 * 	character discarded and state replaced by {@link #TEXT_ELSE}.
		 * </p>
		 * <p><b>At any other character:</b> error.</p>
		 */
		TERNARY_ELSE;
	}

	/**
	 * A class implementing this interface should help the parser of translation
	 * patterns answering the following questions:
	 *
	 * <dl>
	 * 	<dt>How many parameters in the target function?</dt>
	 * 	<dd>{@link #getNbParameters()}</dd>
	 * 	<dt>How to translate/serialize the parameter specified by its index?</dt>
	 * 	<dd>{@link #translate(int)}</dd>
	 * </dl>
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.0
	 * @since 2.0
	 */
	public static interface ParameterTranslator {

		/**
		 * Get the maximum number of parameters in the target function.
		 *
		 * @return	A positive or null integer.
		 */
		public int getNbParameters();

		/**
		 * Translate or serialize the specified target function's parameter.
		 *
		 * @param paramIndex	Index of the parameter to translate.
		 *                  	<i><b>Important:</b> must be an integer starting
		 *                  	from 1.</i>
		 *
		 * @return	The parameter translation.
		 *
		 * @throws TranslationException	If there is a grave error during the
		 *                             	translation process.
		 */
		public String translate(final int paramIndex) throws TranslationException;

	}

	/**
	 * Ensure the given parameter index is between 1 (included) and the given
	 * maximum number of parameters. If this condition is not reached, an error
	 * is thrown.
	 *
	 * @param argIndex	Parameter index to check.
	 * @param nbParams	Maximum number of parameters.
	 * @param textPos	Text position where the parameter index comes from.
	 *               	<i>Used for the error message.</i>
	 *
	 * @throws ParseException	If the given parameter index is incorrect.
	 */
	private static void checkArgIndex(final int argIndex, final int nbParams, final int textPos) throws ParseException {
		if (argIndex < 1 || argIndex > nbParams)
			throw new ParseException("[c." + textPos + "] Incorrect argument index: '$" + argIndex + "'. Expected: an integer between [1;" + nbParams + "].", textPos);
	}

	/**
	 * Validate the syntax of the translation pattern and all included argument
	 * indices.
	 *
	 * <p>
	 * 	This function returns a resolved version of the input pattern. This
	 * 	means that:
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		Single argument references are kept as such (e.g. <code>$1</code>)
	 * 	</li>
	 * 	<li>
	 * 		Argument references list (e.g. <code>$2..</code>) are "exploded"
	 * 		(e.g. <code>$2, $3, $4</code>).
	 * 	</li>
	 * 	<li>
	 * 		Ternary conditional expressions are replaced by either the THEN or
	 * 		the ELSE expression depending on whether $IF exists or not.
	 * 	</li>
	 * </ul>
	 *
	 * <p><i><b>Note:</b>
	 * 	This resolved version of the pattern is just informative.
	 * </i></p>
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	This function is actually a specialized usage of
	 * 	{@link #apply(String, ParameterTranslator)}. Instead of translating the
	 * 	target function arguments, only argument indices are checked and no
	 * 	real translation is operated.
	 * </i></p>
	 *
	 * @param transPattern	The translation pattern to validate.
	 * @param nbMaxParams	Maximum number of parameters for the target
	 *                   	function. <i>If negative, replaced by 0.</i>
	 *
	 * @return	The resolved pattern.
	 *
	 * @throws NullPointerException	If any of the parameter is NULL or empty.
	 * @throws ParseException		If the given pattern is incorrect.
	 *
	 * @see #apply(String, ParameterTranslator)
	 */
	public static String check(final String transPattern, final int nbMaxParams) throws NullPointerException, ParseException {
		try {
			return apply(transPattern, new ParameterTranslator() {

				private final int NB_PARAMS = Math.max(0, nbMaxParams);

				@Override
				public String translate(int paramIndex) throws TranslationException {
					return "$" + paramIndex;
				}

				@Override
				public int getNbParameters() {
					return NB_PARAMS;
				}
			});
		} catch(NullPointerException | ParseException ex) {
			throw ex;
		} catch(Exception ex) {
			throw new ParseException(ex.getMessage(), -1);
		}
	}

	/**
	 * Apply the given translation pattern to the given ADQL function.
	 *
	 * @param transPattern	The translation pattern to apply.
	 * @param fct			The actual ADQL function to translate.
	 * @param translator	The ADQL translator to use for parameters
	 *                  	translation (e.g. columns, other functions).
	 *
	 * @return	The resulting translation.
	 *
	 * @throws NullPointerException	If any of the parameter is missing.
	 * @throws TranslationException	If there is a problem when parsing the given
	 *                             	translation pattern,
	 *                             	or when translating arguments.
	 *
	 * @see #apply(String, ParameterTranslator)
	 */
	public static String apply(final String transPattern, final ADQLFunction fct, final ADQLTranslator translator) throws NullPointerException, TranslationException {
		if (fct == null)
			throw new NullPointerException("Missing the ADQL function to translate!");
		else if (translator == null)
			throw new NullPointerException("Missing the ADQL translation to use!");

		try {
			return apply(transPattern, new ParameterTranslator() {

				@Override
				public String translate(int paramIndex) throws TranslationException {
					// get the argument:
					ADQLOperand arg = fct.getParameter(paramIndex - 1);
					// and translate it:
					return translator.translate(arg);
				}

				@Override
				public int getNbParameters() {
					return fct.getNbParameters();
				}
			});
		} catch(NullPointerException | TranslationException ex) {
			throw ex;
		} catch(ParseException ex) {
			throw new TranslationException(ex.getMessage(), ex);
		} catch(Exception ex) {
			throw new TranslationException(ex);
		}
	}

	/**
	 * Apply a translation pattern in a generic way. The target function's
	 * arguments are translating through a {@link ParameterTranslator}.
	 *
	 * <i>
	 * <p><b>Implementation note:</b>
	 * 	There are 2 more specific variants of this function:
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		{@link #check(String, int)} which only check the syntax and argument
	 * 		indices
	 * 	</li>
	 * 	<li>
	 * 		{@link #apply(String, ADQLFunction, ADQLTranslator)} which translate
	 * 		a given ADQL function using a given ADQL translator.
	 * 	</li>
	 * </ul>
	 * </i>
	 *
	 * @param transPattern		The translation pattern to apply.
	 * @param paramTranslator	How to translate any target function's argument.
	 *
	 * @return	The resulting translation.
	 *
	 * @throws NullPointerException	If any input argument is missing.
	 * @throws ParseException		If the syntax of the given translation
	 *                       		pattern is incorrect.
	 * @throws TranslationException	If an error occurred while translating the
	 *                             	target function's arguments.
	 */
	public static String apply(final String transPattern, final ParameterTranslator paramTranslator) throws NullPointerException, ParseException, TranslationException {
		// Ensure the pattern is not empty:
		if (transPattern == null || transPattern.trim().length() == 0)
			throw new NullPointerException("Missing translation pattern!");

		// Ensure a translator is provided:
		if (paramTranslator == null)
			throw new NullPointerException("Missing the ParameterTranslator to use!");

		// Create a buffer for the final translation:
		final StringBuffer sql = new StringBuffer();

		// Maximum number of arguments accepted by the target function:
		final int NB_PARAMS = paramTranslator.getNbParameters();

		/*
		 * Prepare parsing the translation pattern:
		 */

		// Stack of parsing states (to deal with recursive expressions):
		final ArrayDeque<ParsingState> stackState = new ArrayDeque<>(5);

		// Stack of ignore flags (one for each recursive step requiring one):
		final ArrayDeque<Boolean> stackIgnore = new ArrayDeque<>(5);

		// All characters of the translation pattern to parse:
		final char[] pattern = transPattern.toCharArray();

		// Current character:
		char c;

		// Current parsing state:
		ParsingState state;

		/* Flag indicating whether or not text should be written (to deal with
		 * ternary conditional expressions): */
		boolean ignore = false;

		// Parsed argument index:
		int argIndex = 0;

		/*
		 * Parse the translation pattern, one character at a time:
		 */
		for(int textPos = 0; textPos < pattern.length; textPos++) {

			// get the character:
			c = pattern[textPos];

			// get the parsing state:
			state = stackState.isEmpty() ? ParsingState.TEXT : stackState.peek();

			// know whether characters should be printed or not:
			ignore = (!stackIgnore.isEmpty() && stackIgnore.peek());

			switch(state) {
				/* *********************
				 * CASE: previous = '$'
				 */
				case DOLLAR:

					// If a second dollar => print only one dollar:
					if (c == '$') {
						if (!ignore)
							sql.append(c);
						stackState.poll();
					}

					// If a digit => prepare to read an argument index:
					else if (Character.isDigit(c)) {
						// set the first digit of the argument index:
						argIndex = Character.digit(c, 10);
						// replace the state:
						stackState.poll();
						stackState.push(ParsingState.ARG_REF);
					}

					// Any other character => error!
					else
						throw new ParseException("[c." + textPos + "] Unexpected character after '$': '" + c + "'! Expected: '$' or an argument index (i.e. an integer).", textPos);

					break;

				/* ***************************************************
				 * CASE: ARGUMENT REFERENCE (i.e. previous = '$[0-9]')
				 */
				case ARG_REF:

					// If a digit => part of the argument index:
					if (Character.isDigit(c))
						argIndex = argIndex * 10 + Character.digit(c, 10);

					// If a dot => possible argument list:
					else if (c == '.') {
						// replace the state:
						stackState.pop();
						stackState.push(ParsingState.ARG_LIST);
					}

					// If a '?' => ternary conditional expression:
					else if (c == '?') {
						// determine whether THEN must be ignored or ELSE:
						stackIgnore.push(ignore || argIndex < 1 || argIndex > NB_PARAMS);
						// reset the argument index:
						argIndex = 0;
						// push a new state (i.e. THEN block expected):
						stackState.pop();
						stackState.push(ParsingState.TERNARY_THEN);
					}

					// Any other character => translate the argument:
					else {
						// eventually....
						if (!ignore) {
							// ...check the argument index:
							checkArgIndex(argIndex, NB_PARAMS, textPos);
							// ...and translate the argument:
							sql.append(paramTranslator.translate(argIndex));
						}
						// come back to the previous character:
						textPos--;
						// reset the state:
						stackState.poll();
						argIndex = 0;
					}
					break;

				/* *********************************************************
				 * CASE: POSSIBLE ARGUMENT LIST (i.e. previous = '$[0-9]*.')
				 */
				case ARG_LIST:

					// Check the argument index:
					checkArgIndex(argIndex, NB_PARAMS, textPos);

					// If a second '.' => print all corresponding arguments:
					if (c == '.') {
						// if not ignored...
						if (!ignore) {
							// ...print each argument from the given one:
							for(int pIndex = argIndex; pIndex <= NB_PARAMS; pIndex++) {
								// translate this argument:
								sql.append(paramTranslator.translate(pIndex));
								// if a next one, prepare concatenation:
								if (pIndex + 1 <= NB_PARAMS)
									sql.append(", ");
							}
						}
					}

					/* Any other character => translate the argument and append
					 *                        the previous '.': */
					else {
						// if not ignored...
						if (!ignore) {
							// ...translate the argument:
							sql.append(paramTranslator.translate(argIndex));
							// ...append the previous character ('.'):
							sql.append('.');
						}
						/* but come back one character before (it could be a '$'
						 * or a '}'): */
						textPos--;
					}

					// Anyway, reset the state:
					stackState.poll();
					argIndex = 0;

					break;

				/* ***********************************************************
				 * CASE: START OF A THEN/ELSE's TERNARY CONDITIONAL EXPRESSION
				 */
				case TERNARY_THEN:
				case TERNARY_ELSE:

					// If a '{' => change state
					if (c == '{') {
						stackState.pop();
						if (state == ParsingState.TERNARY_THEN)
							stackState.push(ParsingState.TEXT_THEN);
						else
							stackState.push(ParsingState.TEXT_ELSE);
					}

					// Any other non-whitespace character => error!
					else if (!Character.isWhitespace(c))
						throw new ParseException("[c." + textPos + "] Unexpected character: '" + c + "'! Expected: '{'.", textPos);

					break;

				/* ******************************************************
				 * CASE: NORMAL CONTENT (inside a THEN/ELSE block or not)
				 */
				case TEXT:
				case TEXT_THEN:
				case TEXT_ELSE:
				default:

					// If a '$' => prepare reading an argument index:
					if (c == '$')
						stackState.push(ParsingState.DOLLAR);

					// If a '{' => end a THEN/ELSE block:
					else if (c == '}') {

						// if ending a THEN block, prepare for an ELSE block:
						if (state == ParsingState.TEXT_THEN) {
							/* inverse the 'ignore' flag, while taking into
							 * account the parent's 'ignore' flag: */
							ignore = !stackIgnore.pop();
							stackIgnore.push((!stackIgnore.isEmpty() && stackIgnore.peek()) || ignore);
							// replace the state:
							stackState.pop();
							stackState.push(ParsingState.TERNARY_ELSE);
						}

						// if ending an ELSE block, delete state:
						else if (state == ParsingState.TEXT_ELSE) {
							stackIgnore.pop();
							stackState.pop();
						}

						// any other state, just print the character...
						else {
							// ...if allowed:
							if (!ignore)
								sql.append(c);
						}

					}

					// Any other character => just print the character...
					else {
						// ...if allowed:
						if (!ignore)
							sql.append(c);
					}

					break;
			}
		}

		// While there is still a special state...
		while(!stackState.isEmpty()) {

			// ...get this state:
			state = stackState.pop();

			// ...complete it:
			switch(state) {

				// Previous character = '$' => error!
				case DOLLAR:
					throw new ParseException("[c." + pattern.length + "] Missing character after '$'! Expected: '$' or an argument index (i.e. an integer).", pattern.length);

					// Argument index => translate this argument:
				case ARG_REF:
				case ARG_LIST:
					// check the argument index:
					checkArgIndex(argIndex, NB_PARAMS, pattern.length);
					// translate the argument:
					sql.append(paramTranslator.translate(argIndex));
					// if needed, append a '.':
					if (state == ParsingState.ARG_LIST)
						sql.append('.');
					// reset the state:
					argIndex = 0;
					break;

				// Expecting starting a THEN/ELSE block => error!
				case TERNARY_THEN:
					throw new ParseException("[c." + pattern.length + "] Missing start of the THEN block of a ternary expression! Expected: '{'.", pattern.length);
				case TERNARY_ELSE:
					throw new ParseException("[c." + pattern.length + "] Missing start of the ELSE block of a ternary expression! Expected: '{'.", pattern.length);

					// THEN/ELSE block not yet closed => error!
				case TEXT_THEN:
					throw new ParseException("[c." + pattern.length + "] Missing end of the THEN block of a ternary expression! Expected: '}'.", pattern.length);
				case TEXT_ELSE:
					throw new ParseException("[c." + pattern.length + "] Missing end of the ELSE block of a ternary expression! Expected: '}'.", pattern.length);

				default:
					break;
			}
		}

		// Finally return the complete translation:
		return sql.toString();
	}

}
