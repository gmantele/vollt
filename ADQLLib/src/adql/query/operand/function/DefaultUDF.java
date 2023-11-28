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
 * Copyright 2012-2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.operand.ADQLOperand;

/**
 * It represents any function which is not managed by ADQL.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (05/2021)
 * @deprecated Use directly {@link UserDefinedFunction} instead.
 */
@Deprecated
public final class DefaultUDF extends UserDefinedFunction {

	/**
	 * Creates a user function.
	 *
	 * @param params	Parameters of the function.
	 */
	public DefaultUDF(final String name, final ADQLOperand[] params) throws NullPointerException {
		super(name, params);
	}

	/**
	 * Builds a UserFunction by copying the given one.
	 *
	 * @param toCopy		The UserFunction to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	public DefaultUDF(final DefaultUDF toCopy) throws Exception {
		super(toCopy);
	}

}
