package adql.query.operand.function;

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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.operand.UnknownType;
import adql.translator.ADQLTranslator;
import adql.translator.TranslationException;

/**
 * Function defined by the user (i.e. PSQL functions).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (02/2015)
 * 
 * @see DefaultUDF
 */
public abstract class UserDefinedFunction extends ADQLFunction implements UnknownType {

	/** Type expected by the parser.
	 * @since 1.3 */
	private char expectedType = '?';

	@Override
	public char getExpectedType(){
		return expectedType;
	}

	@Override
	public void setExpectedType(final char c){
		expectedType = c;
	}

	/**
	 * <p>Translate this User Defined Function into the language supported by the given translator.</p>
	 * 
	 * <p><b>VERY IMPORTANT:</b> This function <b>MUST NOT use</b> {@link ADQLTranslator#translate(UserDefinedFunction)} to translate itself.
	 * The given {@link ADQLTranslator} <b>must be used ONLY</b> to translate UDF's operands.</p>
	 * 
	 * <p>Implementation example (extract of {@link DefaultUDF#translate(ADQLTranslator)}):</p>
	 * <pre>
	 * public String translate(final ADQLTranslator caller) throws TranslationException{
	 * 	StringBuffer sql = new StringBuffer(functionName);
	 * 	sql.append('(');
	 * 	for(int i = 0; i < parameters.size(); i++){
	 *		if (i > 0)
	 *			sql.append(',').append(' ');
	 * 		sql.append(caller.translate(parameters.get(i)));
	 *	}
	 *	sql.append(')');
	 *	return sql.toString();
	 * }
	 * </pre>
	 * 
	 * 
	 * @param caller	Translator to use in order to translate <b>ONLY</b> function parameters.
	 * 
	 * @return	The translation of this UDF into the language supported by the given translator.
	 * 
	 * @throws TranslationException	If one of the parameters can not be translated.
	 * 
	 * @since 1.3
	 */
	public abstract String translate(final ADQLTranslator caller) throws TranslationException;

}
