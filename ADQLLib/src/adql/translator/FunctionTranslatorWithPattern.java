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

import adql.query.operand.function.ADQLFunction;

/**
 * A {@link FunctionTranslator} working with a translation pattern.
 *
 * <p>
 * 	A translation pattern is a string with a syntax allowing to make references
 * 	to function arguments. <i>See {@link TranslationPattern} for details about
 * 	the pattern syntax and examples.</i>
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (05/2021)
 * @since 2.0
 *
 * @see TranslationPattern
 */
public class FunctionTranslatorWithPattern implements FunctionTranslator {

	/** Pattern to apply to translate a given ADQL function. */
	protected final String pattern;

	/**
	 * Create a {@link FunctionTranslator} with a translation pattern.
	 *
	 * @param translationPattern	The translation pattern to use.
	 *
	 * @throws NullPointerException	If the given pattern is NULL or empty.
	 */
	public FunctionTranslatorWithPattern(final String translationPattern) throws NullPointerException {
		if (translationPattern == null || translationPattern.trim().isEmpty())
			throw new NullPointerException("Missing translation pattern!");
		pattern = translationPattern;
	}

	/**
	 * Get the translation pattern used in this {@link FunctionTranslator}.
	 *
	 * @return	The used translation pattern.
	 */
	public final String getPattern() {
		return pattern;
	}

	@Override
	public String translate(final ADQLFunction fct, final ADQLTranslator caller) throws TranslationException {
		return TranslationPattern.apply(pattern, fct, caller);
	}

}
