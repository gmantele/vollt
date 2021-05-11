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
 * Interface for a custom ADQL function translation.
 *
 * <h3>Implementations</h3>
 *
 * <p>
 * 	An implementation of this interface is already provided by VOLLT:
 * 	{@link FunctionTranslatorWithPattern}. It lets translate a function by
 * 	applying a simple string pattern with <code>$i</code> variables to match
 * 	function arguments.
 * </p>
 *
 * <h3>Implementation example</h3>
 *
 * <p>
 * 	If the default ADQL function translation was provided by an implementation
 * 	of this interface, it would look like the following:
 * </p>
 *
 * <pre>
 * public class DefaultFunctionTranslator implements FunctionTranslator {
 *
 * 	public DefaultFunctionTranslator(){}
 *
 * 	public String translate(final ADQLFunction fct, final ADQLTranslator caller) throws TranslationException{
 * 		final StringBuilder sql = new StringBuilder(fct.getName());
 * 		sql.append('(');
 * 		for(int i = 0; i < fct.getNbParameters(); i++){
 *			if (i > 0)
 *				sql.append(',').append(' ');
 * 			sql.append(caller.translate(fct.getParameter(i)));
 *		}
 *		sql.append(')');
 *		return sql.toString();
 * 	}
 *
 * }
 * </pre>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (05/2021)
 * @since 2.0
 */
public interface FunctionTranslator {

	/**
	 * Translate the given ADQL function into the language supported by the
	 * given translator.
	 *
	 * <p><b>VERY IMPORTANT:</b>
	 * 	This function <b>MUST NOT use</b>
	 * 	{@link ADQLTranslator#translate(ADQLFunction)} to translate the given
	 * 	{@link ADQLFunction}. The given {@link ADQLTranslator}
	 * 	<b>must be used ONLY</b> to translate the function's parameters.
	 * </p>
	 *
	 * @param fct		The function to translate.
	 * @param caller	Translator to use in order to translate <b>ONLY</b>
	 *              	function parameters.
	 *
	 * @return	The translation of this function into the language supported by
	 *        	the given translator,
	 *        	or NULL to let the calling translator apply a default
	 *        	translation.
	 *
	 * @throws TranslationException	If the translation fails.
	 */
	public abstract String translate(final ADQLFunction fct, final ADQLTranslator caller) throws TranslationException;

}
