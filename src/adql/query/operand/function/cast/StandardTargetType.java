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
 * Copyright 2023 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NumericConstant;

/**
 * Representation of an ADQL standard target type of the CAST function.
 *
 * <h3>Parameters</h3>
 * <p>
 * 	All target types represented by this class can not be parameterized, except
 * 	variable datatypes (such as {@link DBDatatype#CHAR} and
 * 	{@link DBDatatype#VARCHAR}) which may have only one - the length.
 * </p>
 *
 * <h3>Standard datatypes</h3>
 * <p>
 * 	Datatypes considered as standard by this class can be retrieved thanks to
 * 	{@link #getStandardDatatypes()}. It is also possible to get them by category
 * 	with {@link #getNumericDatatypes()}, {@link #getStringDatatypes()} and
 * 	{@link #getGeometricDatatypes()}. A given {@link DBDatatype} can be tested
 * 	to know whether it is considered as standard or not with
 * 	{@link #isStandardDatatype(DBDatatype)}.
 * </p>
 *
 * <p><i><b>Note:</b>
 * 	{@link DBDatatype#DOUBLE} is represented in ADQL with the string
 * 	<code>DOUBLE PRECISION</code>. This specificity is already taken into
 * 	account by {@link #toADQL()} and when creating a {@link StandardTargetType}
 * 	with a string (see below).
 * </i></p>
 *
 * <h3>Datatype normalization</h3>
 * <p>
 * 	The constructors {@link #StandardTargetType(String)} and
 * 	{@link #StandardTargetType(String, int)} accepts the string version of
 * 	a standard datatype. To resolve it, they rely on the static function
 * 	{@link #resolveDatatype(String)}. This latter always normalize the input
 * 	string so that consecutive whitespace characters are replaced by only one
 * 	and all other characters are upper-case. The normalization is performed by
 * 	the static function {@link #normalizeDatatype(String)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (01/2023)
 * @since 2.0
 *
 * @see CastFunction
 */
public class StandardTargetType implements TargetType {

	/** All datatypes allowed by ADQL as CAST's target type. */
	protected final static DBDatatype[] STANDARD_DATATYPES = new DBDatatype[]{ DBDatatype.CHAR, DBDatatype.VARCHAR, DBDatatype.INTEGER, DBDatatype.SMALLINT, DBDatatype.BIGINT, DBDatatype.REAL, DBDatatype.DOUBLE, DBDatatype.TIMESTAMP, DBDatatype.POINT, DBDatatype.CIRCLE, DBDatatype.POLYGON, DBDatatype.REGION };

	/** The represented target type. */
	protected DBType type;

	/** Position of this type parameter. */
	private TextPosition position = null;

	/**
	 * Create a standard target type with just a datatype (no length parameter).
	 *
	 * <p><i><b>Implementation note:</b>
	 * 	This constructor is similar to {@link #StandardTargetType(String, int)}
	 * 	with <code>(typeName, {@link DBType#NO_LENGTH})</code>.
	 * </i></p>
	 *
	 * @param typeName	Target type's name.
	 *
	 * @throws NullPointerException		If the type name is NULL or empty.
	 * @throws IllegalArgumentException	If the given type name is unknown or not
	 *                                 	standard.
	 */
	public StandardTargetType(final String typeName) throws NullPointerException, IllegalArgumentException {
		this(typeName, DBType.NO_LENGTH);
	}

	/**
	 * Create a standard target type, possibly a variable-length one.
	 *
	 * <p><i><b>Warning:</b>
	 * 	A length can be provided only for a variable-length datatype (e.g. CHAR,
	 * 	VARCHAR).
	 * </i></p>
	 *
	 * @param typeName	Name of the type.
	 * @param length	Length of the type (or &le;0 if undefined).
	 *
	 * @throws NullPointerException		If the type name is NULL or empty.
	 * @throws IllegalArgumentException	If the given type name is unknown or not
	 *                                 	standard, or if a positive non-zero
	 *                                 	length is provided for a non
	 *                                 	variable-length datatype.
	 */
	public StandardTargetType(final String typeName, final int length) throws NullPointerException, IllegalArgumentException {
		this(new DBType(resolveDatatype(typeName), length));
	}

	/**
	 * Create a standard target type.
	 *
	 * @param type	Target type.
	 *
	 * @throws NullPointerException		If the target type is missing.
	 * @throws IllegalArgumentException	If the given type is not standard,
	 *                                 	or if a length is provided for non
	 *                                 	variable-length datatype.
	 */
	public StandardTargetType(final DBType type) throws NullPointerException, IllegalArgumentException {
		// A type MUST be provided:
		if (type == null)
			throw new NullPointerException("Missing target type!");

		// Set the given type, if allowed:
		this.type = type;

		// Ensure the datatype is standard:
		if (!isStandardDatatype(type.type))
			throw new IllegalArgumentException("Not a standard ADQL CAST's datatype: \"" + type.type + "\"!");

		// ...and ensure the length is allowed for this datatype:
		if (this.type.length > 0) {
			// allowed only for variable length datatypes:
			if (type.type != DBDatatype.CHAR && type.type != DBDatatype.VARCHAR)
				throw new IllegalArgumentException("No length allowed for the datatype \"" + type.type + "\"! It is not a variable-length datatype like CHAR, VARCHAR, ...");
		}

		// No text position by default:
		position = null;
	}

	/**
	 * Normalize the given datatype serialization.
	 *
	 * <p>The normalization performed here...</p>
	 * <ul>
	 * 	<li>...remove leading and trailing space characters,</li>
	 * 	<li>...replace all consecutive space characters by a single space,</li>
	 * 	<li>...and put all characters in upper-case.</li>
	 * </ul>
	 *
	 * @param str	The string to normalize.
	 *
	 * @return	Its normalized version.
	 *
	 * @throws NullPointerException	If the given string is NULL or empty.
	 */
	public final static String normalizeDatatype(final String str) throws NullPointerException {
		// Ensure something has been provided:
		if (str == null || str.trim().length() == 0)
			throw new NullPointerException("Missing datatype name!");

		/* Normalize the datatype name by removing leading and trailing spaces,
		 * squeezing middle spaces and put the result into upper case: */
		return str.trim().replaceAll("\\s+", " ").toUpperCase();
	}

	/**
	 * Resolve the given type serialization into a {@link DBDatatype}.
	 *
	 * <p>
	 * 	The given string is {@link #normalizeDatatype(String) normalized} and
	 * 	then resolved into a {@link DBDatatype} value. If no DBDatatype matches,
	 * 	an {@link IllegalArgumentException} is thrown.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The returned datatype is not guaranteed to be a standard CAST's target
	 * 	datatype: this is not checked in this function. To check the datatype
	 * 	use {@link #isStandardDatatype(DBDatatype)} with the {@link DBDatatype}
	 * 	returned by this function.
	 * <i></p>
	 *
	 * @param str	The type's string serialization.
	 *
	 * @return	The resolved datatype.
	 *
	 * @throws NullPointerException		If the given string is NULL or empty.
	 * @throws IllegalArgumentException	If the serialized datatype is unknown.
	 */
	public final static DBDatatype resolveDatatype(final String str) throws NullPointerException, IllegalArgumentException {
		// Normalize the input datatype name:
		final String normDatatype = normalizeDatatype(str);

		// Try to resolve the normalized datatype:
		DBDatatype resolved = null;
		try {
			resolved = ("DOUBLE PRECISION".equals(normDatatype) ? DBDatatype.DOUBLE : DBDatatype.valueOf(normDatatype));
		} catch(IllegalArgumentException iae) {
			throw new IllegalArgumentException("Unknown datatype: \"" + str + "\"!");
		}

		// If it is, return it:
		return resolved;
	}

	/**
	 * Tell whether the given datatype is allowed as target for CAST function.
	 *
	 * @param datatype	The datatype to test.
	 *
	 * @return	<code>true</code> if allowed by ADQL as CAST' target type,
	 *        	<code>false</code> otherwise.
	 */
	public static boolean isStandardDatatype(final DBDatatype datatype) {
		if (datatype != null) {
			for(DBDatatype d : STANDARD_DATATYPES) {
				if (d == datatype)
					return true;
			}
		}
		return false;
	}

	/**
	 * Get all datatypes considered as standard CAST's target types.
	 *
	 * @return	All standard CAST's target types.
	 */
	public final static DBDatatype[] getStandardDatatypes() {
		return STANDARD_DATATYPES.clone();
	}

	/**
	 * Get all numeric datatypes among all the standard CAST's target types.
	 *
	 * @return	All standard numeric CAST's target types.
	 */
	public final static DBDatatype[] getNumericDatatypes() {
		return new DBDatatype[]{ DBDatatype.SMALLINT, DBDatatype.INTEGER, DBDatatype.BIGINT, DBDatatype.REAL, DBDatatype.DOUBLE };
	}

	/**
	 * Get all string datatypes among all the standard CAST's target types.
	 *
	 * @return	All standard string CAST's target types.
	 */
	public final static DBDatatype[] getStringDatatypes() {
		return new DBDatatype[]{ DBDatatype.CHAR, DBDatatype.VARCHAR, DBDatatype.TIMESTAMP };
	}

	/**
	 * Get all geometric datatypes among all the standard CAST's target types.
	 *
	 * @return	All standard geometric CAST's target types.
	 */
	public final static DBDatatype[] getGeometricDatatypes() {
		return new DBDatatype[]{ DBDatatype.POINT, DBDatatype.CIRCLE, DBDatatype.POLYGON, DBDatatype.REGION };
	}

	@Override
	public boolean isNumeric() {
		for(DBDatatype d : getNumericDatatypes()) {
			if (d == type.type)
				return true;
		}
		return false;
	}

	@Override
	public boolean isString() {
		for(DBDatatype d : getStringDatatypes()) {
			if (d == type.type)
				return true;
		}
		return false;
	}

	@Override
	public boolean isGeometry() {
		for(DBDatatype d : getGeometricDatatypes()) {
			if (d == type.type)
				return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return (type.type == DBDatatype.DOUBLE ? "DOUBLE PRECISION" : type.type.toString());
	}

	/**
	 * Get the length associated with this target type.
	 *
	 * <p><i><b>Note:</b>
	 * 	The returned length can be positive and not null ONLY IF the target type
	 * 	is a variable length type (e.g. CHAR, VARCHAR).
	 * </i></p>
	 *
	 * @return	Type length.
	 */
	public int getTypeLength() {
		return type.length;
	}

	@Override
	public DBType getReturnType() {
		return type;
	}

	@Override
	public TargetType getCopy() throws Exception {
		return new StandardTargetType(type);
	}

	@Override
	public TextPosition getPosition() {
		return position;
	}

	@Override
	public void setPosition(final TextPosition newPosition) {
		position = newPosition;
	}

	@Override
	public String toString() {
		return toADQL();
	}

	@Override
	public int getNbParameters() {
		return (type.length > 0 ? 1 : 0);
	}

	@Override
	public ADQLOperand[] getParameters() {
		if (type.length > 0)
			return new ADQLOperand[]{ new NumericConstant(type.length) };
		else
			return new ADQLOperand[0];
	}

	@Override
	public ADQLOperand getParameter(int indParam) {
		if (type.length > 0)
			return new NumericConstant(type.length);
		else
			throw new IndexOutOfBoundsException("Incorrect parameter index: " + indParam + "! Nb max. parameters: " + getNbParameters() + ".");
	}

	@Override
	public ADQLOperand setParameter(int indParam, ADQLOperand newParam) {
		if (type.length > 0 && indParam == 0) {
			final ADQLOperand oldLength = (type.length > 0 ? new NumericConstant(type.length) : null);
			if (newParam == null)
				type = new DBType(type.type, DBType.NO_LENGTH);
			else if (newParam instanceof NumericConstant)
				type = new DBType(type.type, (int)((NumericConstant)newParam).getNumericValue());
			else
				throw new IllegalArgumentException("Impossible to replace a datatype length with a " + newParam.getClass().getName() + "! A NumericConstant was expected.");
			return oldLength;
		} else
			throw new IndexOutOfBoundsException("Incorrect parameter index: " + indParam + "! Nb max. parameters: " + getNbParameters() + ".");
	}

	@Override
	public String toADQL() {
		if (type.length > 0)
			return getName() + "(" + type.length + ")";
		else
			return getName();
	}
}
