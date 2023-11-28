package adql.query.operand;

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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import adql.query.operand.function.UserDefinedFunction;

/**
 * <p>Operand whose the type can not be known at the parsing time.
 * A post-parsing step with column metadata is needed to resolved their types.</p>
 * 
 * <p><i>Note:
 * 	For the moment, only two operands are concerned: columns ({@link ADQLColumn}) and user defined functions ({@link UserDefinedFunction}).
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.3 (10/2014)
 * @since 1.3
 */
public interface UnknownType extends ADQLOperand {

	/**
	 * Get the type expected by the syntactic parser according to the context.
	 * 
	 * @return	Expected type: 'n' or 'N' for numeric, 's' or 'S' for string, 'g' or 'G' for geometry.
	 */
	public char getExpectedType();

	/**
	 * Set the type expected for this operand.
	 * 
	 * @param c	Expected type: 'n' or 'N' for numeric, 's' or 'S' for string, 'g' or 'G' for geometry.
	 */
	public void setExpectedType(final char c);

}
