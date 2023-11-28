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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;

/**
 * <p>Any ADQL operand (an operation, a constant, a column name, a function, ...) must implement this interface
 * and indicates whether it corresponds to a numeric, a string or a geometrical region value.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public interface ADQLOperand extends ADQLObject {

	/**
	 * Tell whether this operand is numeric or not.
	 * 
	 * @return	<i>true</i> if this operand is numeric, <i>false</i> otherwise.
	 */
	public boolean isNumeric();

	/**
	 * Tell whether this operand is a string or not.
	 * 
	 * @return	<i>true</i> if this operand is a string, <i>false</i> otherwise.
	 */
	public boolean isString();

	/**
	 * Tell whether this operand is a geometrical region or not.
	 * 
	 * @return	<i>true</i> if this operand is a geometry, <i>false</i> otherwise.
	 * 
	 * @since 1.3
	 */
	public boolean isGeometry();

}
