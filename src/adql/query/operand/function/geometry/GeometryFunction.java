package adql.query.operand.function.geometry;

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

import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.TextPosition;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.UnknownType;
import adql.query.operand.function.ADQLFunction;

/**
 * It represents any geometric function of ADQL.
 *
 * <p>
 * 	For historical reasons, the geometry regions accept an optional string value
 * 	as the first argument. As of this version of the specification (2.1) this
 * 	parameter has been marked as deprecated. Future versions of this
 * 	specification (>2.1) may remove this parameter.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (04/2021)
 */
public abstract class GeometryFunction extends ADQLFunction {

	/** The coordinate system used to express the coordinates.
	 * @deprecated Since ADQL-2.1. */
	@Deprecated
	protected ADQLOperand coordSys = null;

	/**
	 * Builds a geometry function with no coordinate system.
	 */
	protected GeometryFunction() {
		coordSys = null;
	}

	/**
	 * Builds a geometry function with its coordinate system.
	 *
	 * @param coordSys							A string operand which
	 *                							corresponds to a valid
	 *                							coordinate system.
	 * @throws UnsupportedOperationException	If this function is not
	 *                                      	associated with a coordinate
	 *                                      	system.
	 * @throws NullPointerException				If the given operand is NULL.
	 * @throws Exception						If the given operand is not a
	 *                  						string.
	 *
	 * @deprecated Since ADQL-2.1, the coordinate system argument is deprecated.
	 */
	@Deprecated
	protected GeometryFunction(ADQLOperand coordSys) throws UnsupportedOperationException, NullPointerException, Exception {
		setCoordinateSystem(coordSys);
	}

	/**
	 * Builds a geometry function by copying the given one.
	 *
	 * @param toCopy		The geometry function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	protected GeometryFunction(GeometryFunction toCopy) throws Exception {
		coordSys = (ADQLOperand)(toCopy.coordSys.getCopy());
		setPosition((toCopy.getPosition() == null) ? null : new TextPosition(toCopy.getPosition()));
	}

	/**
	 * Gets the used coordinate system.
	 *
	 * @return	Its coordinate system.
	 *
	 * @deprecated Since ADQL-2.1.
	 */
	@Deprecated
	public ADQLOperand getCoordinateSystem() {
		return coordSys;
	}

	/**
	 * Changes the coordinate system.
	 *
	 * @param coordSys							Its new coordinate system.
	 * @throws UnsupportedOperationException	If this function is not
	 *                                      	associated with a coordinate
	 *                                      	system.
	 * @throws NullPointerException				If the given operand is NULL.
	 * @throws ParseException					If the given operand is not a
	 *                       					string.
	 *
	 * @deprecated Since ADQL-2.1.
	 */
	@Deprecated
	public void setCoordinateSystem(ADQLOperand coordSys) throws UnsupportedOperationException, NullPointerException, ParseException {
		if (coordSys == null)
			this.coordSys = new StringConstant("");
		else if (!coordSys.isString())
			throw new ParseException("A coordinate system must be a string literal: \"" + coordSys.toADQL() + "\" is not a string operand!");
		else {
			this.coordSys = coordSys;
			setPosition(null);
		}
	}

	/**
	 * This class represents a parameter of a geometry function
	 * which, in general, is either a GeometryFunction or an UnknownType operand
	 * (e.g. a Column, a UserDefinedFunction, a CastFunction).
	 *
	 * @author Gr&eacute;gory Mantelet (CDS;ARI)
	 * @version 2.0 (04/2021)
	 */
	public static final class GeometryValue<F extends GeometryFunction> implements ADQLOperand {

		private F geomFunct;

		/** @since 2.0 */
		private UnknownType unknownValue;

		/** Position of this {@link GeometryValue} in the ADQL query string.
		 * @since 1.4 */
		private TextPosition position = null;

		public GeometryValue(F geometry) throws NullPointerException {
			if (geometry == null)
				throw new NullPointerException("Impossible to build a GeometryValue without a geometry function or an operand whose type can not be determined at parsing time (e.g. a column, a User Defined Function, a casting function)!");
			setGeometry(geometry);
		}

		/** @since 2.0 */
		public GeometryValue(UnknownType val) throws NullPointerException {
			if (val == null)
				throw new NullPointerException("Impossible to build a GeometryValue without a geometry function or an operand whose type can not be determined at parsing time (e.g. a column, a User Defined Function, a casting function)!");
			setUnknownTypeValue(val);
		}

		@SuppressWarnings("unchecked")
		public GeometryValue(GeometryValue<F> toCopy) throws Exception {
			unknownValue = (toCopy.unknownValue == null) ? null : ((UnknownType)(toCopy.unknownValue.getCopy()));
			geomFunct = (toCopy.geomFunct == null) ? null : ((F)(toCopy.geomFunct.getCopy()));
			position = (toCopy.position == null) ? null : new TextPosition(toCopy.position);
		}

		@Override
		public final LanguageFeature getFeatureDescription() {
			return (unknownValue != null ? unknownValue.getFeatureDescription() : geomFunct.getFeatureDescription());
		}

		/**
		 * @deprecated Use {@link #setUnknownTypeValue(UnknownType)} instead.
		 */
		@Deprecated
		public final void setColumn(ADQLColumn col) {
			setUnknownTypeValue(col);
		}

		public void setGeometry(F geometry) {
			if (geometry != null) {
				unknownValue = null;
				geomFunct = geometry;
				position = (geomFunct.getPosition() != null) ? geomFunct.getPosition() : null;
			}
		}

		/** @since 2.0 */
		public void setUnknownTypeValue(UnknownType val) {
			if (val != null) {
				unknownValue = val;
				geomFunct = null;
				position = (val.getPosition() != null) ? val.getPosition() : null;
			}
		}

		public ADQLOperand getValue() {
			if (geomFunct != null)
				return geomFunct;
			else
				return unknownValue;
		}

		public boolean isColumn() {
			return unknownValue != null && unknownValue instanceof ADQLColumn;
		}

		@Override
		public boolean isNumeric() {
			return getValue().isNumeric();
		}

		@Override
		public boolean isString() {
			return getValue().isString();
		}

		@Override
		public TextPosition getPosition() {
			return position;
		}

		@Override
		public boolean isGeometry() {
			return getValue().isGeometry();
		}

		@Override
		public ADQLObject getCopy() throws Exception {
			return new GeometryValue<F>(this);
		}

		@Override
		public String getName() {
			return getValue().getName();
		}

		@Override
		public ADQLIterator adqlIterator() {
			return getValue().adqlIterator();
		}

		@Override
		public String toADQL() {
			return getValue().toADQL();
		}
	}
}
