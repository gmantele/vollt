package adql.query.operand.function.cast;

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

import adql.db.DBType;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;

/**
 * Representation of the CAST's target type.
 *
 * <p>
 * 	Since it can never be an ADQL operand, it does not implements the interface
 * 	{@link ADQLOperand} or {@link adql.query.ADQLObject}. However, it has
 * 	numerous functions that are similar in order to easily identify the nature
 * 	of the returned value ({@link #isNumeric()}, {@link #isString()},
 * 	{@link #isGeometry()} and {@link #getReturnType()}) but also to know how it
 * 	is parameterized ({@link #getNbParameters()}, {@link #getParameters()},
 * 	...). Besides, as any ADQL operand it also provides an information about its
 * 	position ({@link #getPosition()}).
 * </p>
 *
 * <p>
 * 	The target type of the CAST function is an interface in order to easier its
 * 	customization. By default the ADQL library uses two default implementations:
 * </p>
 * <dl>
 * 	<dt>{@link StandardTargetType}</dt>
 * 	<dd>
 * 		For any of the ADQL standard type allowed for the CAST function.
 * 	</dd>
 * 	<dt>{@link CustomTargetType}</dt>
 * 	<dd>
 * 		For any other type. On the contrary to the other implementation, more
 * 		than one parameter is allowed and there is no constraint on their type.
 * 	</dd>
 * </dl>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (05/2021)
 * @since 2.0
 */
public interface TargetType {

	/**
	 * Get the type name (as written in ADQL).
	 *
	 * <p><b>IMPORTANT:</b>
	 * 	This function MUST never return NULL or an empty string.
	 * </p>
	 *
	 * @return	The type name.
	 */
	public String getName();

	/**
	 * Position of the type name (start) and all its parameters (end).
	 *
	 * @return	Position of this target type in the input ADQL query,
	 *        	or NULL if this piece of information is not available.
	 */
	public TextPosition getPosition();

	/**
	 * Sets the position at which this {@link TargetType} has been found in the
	 * original ADQL query string.
	 *
	 * @param position	Position of this {@link TargetType}.
	 */
	public void setPosition(final TextPosition newPosition);

	/**
	 * Indicate whether the output of the CAST function is numeric or not.
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	If the return type is unknown, it is a good practice to make
	 * 	{@link #isNumeric()}, {@link #isGeometry()} and {@link #isString()} to
	 * 	return <code>true</code>. This information is used only by the parser
	 * 	when checking whether the CAST function can be used in some context.
	 * 	So, when the type of an ADQL operand is unknown, it is assumed it could
	 * 	be placed anywhere until its type can be precisely determined or until
	 * 	the query reaches the database.
	 * </i></p>
	 *
	 * @return	<code>true</code> if the CAST function returns a numeric,
	 *        	<code>false</code> otherwise.
	 */
	public boolean isNumeric();

	/**
	 * Indicate whether the output of the CAST function is a character string or
	 * not.
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	If the return type is unknown, it is a good practice to make
	 * 	{@link #isNumeric()}, {@link #isGeometry()} and {@link #isString()} to
	 * 	return <code>true</code>. This information is used only by the parser
	 * 	when checking whether the CAST function can be used in some context.
	 * 	So, when the type of an ADQL operand is unknown, it is assumed it could
	 * 	be placed anywhere until its type can be precisely determined or until
	 * 	the query reaches the database.
	 * </i></p>
	 *
	 * @return	<code>true</code> if the CAST function returns a string,
	 *        	<code>false</code> otherwise.
	 */
	public boolean isString();

	/**
	 * Indicate whether the output of the CAST function is a geometry or not.
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	If the return type is unknown, it is a good practice to make
	 * 	{@link #isNumeric()}, {@link #isGeometry()} and {@link #isString()} to
	 * 	return <code>true</code>. This information is used only by the parser
	 * 	when checking whether the CAST function can be used in some context.
	 * 	So, when the type of an ADQL operand is unknown, it is assumed it could
	 * 	be placed anywhere until its type can be precisely determined or until
	 * 	the query reaches the database.
	 * </i></p>
	 *
	 * @return	<code>true</code> if the CAST function returns a geometry,
	 *        	<code>false</code> otherwise.
	 */
	public boolean isGeometry();

	/**
	 * Indicate the precise type of the value expected to be returned by
	 * the CAST function.
	 *
	 * <p>
	 * 	This information is used only when the CAST function is used as output
	 * 	column (so, in the SELECT clause). It is a hint when formatting the
	 * 	query result to make the most appropriate conversion.
	 * </p>
	 *
	 * @return	The expected type returned by the CAST function,
	 *        	or NULL if unknown.
	 */
	public DBType getReturnType();

	/**
	 * Get the actual number of parameters.
	 *
	 * @return	Number of parameters.
	 */
	public int getNbParameters();

	/**
	 * Get the ordered list of all parameters.
	 *
	 * @return	All type parameters,
	 *        	or an empty array if no parameter.
	 */
	public ADQLOperand[] getParameters();

	/**
	 * Get the indParam-th parameter.
	 *
	 * @param indParam	Index (&ge; 0) of the parameter to get.
	 *
	 * @return	The corresponding parameter.
	 *
	 * @throws IndexOutOfBoundsException	If the index is incorrect (i.e.
	 *                                  	&lt; 0
	 *                                  	or &ge; {@link #getNbParameters()}).
	 */
	public ADQLOperand getParameter(final int indParam) throws IndexOutOfBoundsException;

	/**
	 * Replace the indParam-th parameter.
	 *
	 * @param indParam	Index (&ge; 0) of the parameter to replace.
	 * @param newParam	The operand to set instead of the current indParam-th
	 *                	parameter.
	 *
	 * @return	The former indParam-th parameter.
	 *
	 * @throws IndexOutOfBoundsException	If the index is incorrect (i.e.
	 *                                  	&lt; 0
	 *                                  	or &ge; {@link #getNbParameters()}).
	 */
	public ADQLOperand setParameter(final int indParam, final ADQLOperand newParam) throws IndexOutOfBoundsException;

	/**
	 * Serialize this type into ADQL.
	 *
	 * @return	Its ADQL serialization.
	 */
	public String toADQL();

	/**
	 * Create a deep copy of this target type.
	 *
	 * @return	Type copy.
	 *
	 * @throws Exception	If the copy fails.
	 */
	public TargetType getCopy() throws Exception;

}
