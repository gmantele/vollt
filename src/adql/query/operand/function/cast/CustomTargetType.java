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
import adql.db.DBType.DBDatatype;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;

/**
 * Representation of a non-standard target type of the CAST function.
 *
 * <h3>Parameters</h3>
 * <p>
 * 	On the contrary to a standard target type, a custom type does not have any
 * 	restriction on parameters: it may have as many parameters as desired. There
 * 	is no constraint as well on their type: no strict signature to follow.
 * </p>
 *
 * <h3>Return type</h3>
 *
 * <p>
 * 	Because the target type is custom, there is no way to automatically
 * 	determine the (approximate or precise) type of the value returned by the
 * 	CAST function. The constructor of {@link CustomTargetType} tries anyway to
 * 	resolve the given custom type name, in case it is a datatype known by the
 * 	ADQL library (see {@link DBDatatype}). If successfully resolved, a
 * 	{@link DBType} is automatically created (with no length parameter) and will
 * 	be returned by {@link #getReturnType()}. If still unknown (or erroneous),
 * 	one has to explicitly use {@link #setReturnType(DBType)}.
 * </p>
 *
 * <p>
 * 	The functions {@link #isNumeric()}, {@link #isString()} and
 * 	{@link #isGeometry()} are used to give hint to the ADQL query parser about
 * 	where the CAST function can be used. By default, they return what the
 * 	same functions of the {@link #getReturnType()} returns. If this latter
 * 	returns NULL, all these functions return <code>true</code> so that the CAST
 * 	function can be used anywhere in an ADQL query. However, this may lead to
 * 	errors when running the query against a database.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (05/2021)
 * @since 2.0
 *
 * @see CastFunction
 */
public class CustomTargetType implements TargetType {

	/** Datatype's name.
	 * <i>Never NULL or empty.</i> */
	protected String typeName;

	/** Ordered list of all type parameters.
	 * <i>>NULL (preferably ; by default) or empty if no parameter.</i> */
	protected ADQLOperand[] parameters = null;

	/** Type of the value returned by the CAST function.
	 * <i>NULL if unknown (default).</i> */
	protected DBType returnType = null;

	/** Position of this entire target type in the input ADQL query.
	 * <i>NULL if unknown.</i> */
	private TextPosition position = null;

	/**
	 * Create a custom CAST's target type with no parameter.
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	This constructor is equivalent to {@link #CustomTargetType(String, ADQLOperand[])}
	 * 	with <code>(typeName, null)</code>.
	 * </i></p>
	 *
	 * @param typeName	Name of the custom type.
	 *
	 * @throws NullPointerException	If the given name is NULL or empty.
	 */
	public CustomTargetType(final String typeName) throws NullPointerException {
		this(typeName, null);
	}

	/**
	 * Create a custom CAST's target type.
	 *
	 * @param typeName		Name of the custom type.
	 * @param parameters	Type parameters (e.g. a length).
	 *                  	<i>NULL or empty if no parameter.</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or empty,
	 *                             	or if one parameter is NULL.
	 */
	public CustomTargetType(final String typeName, final ADQLOperand[] parameters) {
		// Ensure there is a valid type name:
		if (typeName == null || typeName.trim().isEmpty())
			throw new NullPointerException("Impossible to create a custom datatype without a datatype name!");

		// Normalize it:
		this.typeName = StandardTargetType.normalizeDatatype(typeName);

		// Try to resolve the datatype as the CAST's return type:
		try {
			returnType = new DBType(StandardTargetType.resolveDatatype(typeName));
		} catch(IllegalArgumentException iae) {
			/* Unknown datatype (which is almost normal for a custom datatype),
			 * so, nothing to do until setReturnType(...) is used. */
		}

		// Set the parameters (if any):
		if (parameters == null || parameters.length == 0)
			this.parameters = null;
		else {
			// ensure there is no NULL item:
			for(int i = 0; i < parameters.length; i++) {
				if (parameters[i] == null)
					throw new NullPointerException("The " + (i + 1) + "-th parameter of the custom datatype \"" + typeName + "\" is NULL! Null parameters are forbidden inside custom datatype definition.");
			}
			// finally set the list of parameters:
			this.parameters = parameters;
		}
	}

	@Override
	public final String getName() {
		return typeName;
	}

	@Override
	public final TextPosition getPosition() {
		return position;
	}

	public final void setPosition(final TextPosition newPosition) {
		position = newPosition;
	}

	@Override
	public boolean isNumeric() {
		return (returnType != null ? returnType.isNumeric() : true);
	}

	@Override
	public boolean isString() {
		return (returnType != null ? returnType.isString() : true);
	}

	@Override
	public boolean isGeometry() {
		return (returnType != null ? returnType.isGeometry() : true);
	}

	@Override
	public final DBType getReturnType() {
		return returnType;
	}

	/**
	 * Set the expected type returned by the CAST function.
	 *
	 * @param newType	The presumed returned type,
	 *               	or NULL if unknown.
	 */
	public void setReturnType(final DBType newType) {
		returnType = newType;
	}

	@Override
	public int getNbParameters() {
		return (parameters == null ? 0 : parameters.length);
	}

	@Override
	public ADQLOperand[] getParameters() {
		return (parameters == null ? new ADQLOperand[0] : parameters.clone());
	}

	@Override
	public ADQLOperand getParameter(final int indParam) {
		if (parameters == null || indParam < 0 || indParam >= parameters.length)
			throw new IndexOutOfBoundsException("Incorrect parameter index: " + indParam + "! Nb max. parameters: " + (parameters == null ? 0 : parameters.length) + ".");
		return parameters[indParam];
	}

	@Override
	public ADQLOperand setParameter(final int indParam, final ADQLOperand newParam) {
		if (parameters == null || indParam < 0 || indParam >= parameters.length)
			throw new IndexOutOfBoundsException("Incorrect parameter index: " + indParam + "! Nb max. parameters: " + (parameters == null ? 0 : parameters.length) + ".");
		if (newParam == null)
			throw new NullPointerException("Impossible to remove a custom datatype's parameter!");
		final ADQLOperand oldParam = parameters[indParam];
		parameters[indParam] = newParam;
		return oldParam;
	}

	@Override
	public String toADQL() {
		if (parameters == null)
			return typeName;
		else {
			final StringBuilder adql = new StringBuilder(typeName);
			adql.append('(');
			for(int i = 0; i < parameters.length; i++) {
				if (i > 0)
					adql.append(',').append(' ');
				adql.append(parameters[i].toADQL());
			}
			adql.append(')');
			return adql.toString();
		}
	}

	@Override
	public TargetType getCopy() throws Exception {
		return new CustomTargetType(typeName, getParameters());
	}

	@Override
	public String toString() {
		return toADQL();
	}

}
