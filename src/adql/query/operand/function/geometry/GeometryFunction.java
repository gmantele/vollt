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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;
import adql.query.operand.ADQLColumn;
import adql.query.operand.function.ADQLFunction;

/**
 * <p>It represents any geometric function of ADQL.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 */
public abstract class GeometryFunction extends ADQLFunction {

	/** The coordinate system used to express the coordinates. */
	protected ADQLOperand coordSys = null;


	/**
	 * Builds a geometry function with no coordinate system.
	 */
	protected GeometryFunction(){
		coordSys = null;
	}

	/**
	 * Builds a geometry function with its coordinate system.
	 * 
	 * @param coordSys							A string operand which corresponds to a valid coordinate system.
	 * @throws UnsupportedOperationException	If this function is not associated with a coordinate system.
	 * @throws NullPointerException				If the given operand is <i>null</i>.
	 * @throws Exception						If the given operand is not a string.
	 */
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
	}

	/**
	 * Gets the used coordinate system.
	 * 
	 * @return	Its coordinate system.
	 */
	public ADQLOperand getCoordinateSystem(){
		return coordSys;
	}

	/**
	 * Changes the coordinate system.
	 * 
	 * @param coordSys							Its new coordinate system.
	 * @throws UnsupportedOperationException	If this function is not associated with a coordinate system.
	 * @throws NullPointerException				If the given operand is <i>null</i>.
	 * @throws Exception						If the given operand is not a string.
	 */
	public void setCoordinateSystem(ADQLOperand coordSys) throws UnsupportedOperationException, NullPointerException, Exception {
		if (coordSys == null)
			throw new NullPointerException("");
		else if (!coordSys.isString())
			throw new Exception("A coordinate system must be a string literal: \""+coordSys.toADQL()+"\" is not a string operand !");
		else
			this.coordSys = coordSys;
	}


	/**
	 * This class represents a parameter of a geometry function
	 * which, in general, is either a GeometryFunction or a Column.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 06/2011
	 */
	public static final class GeometryValue<F extends GeometryFunction> implements ADQLOperand {
		private ADQLColumn column;
		private F geomFunct;

		public GeometryValue(ADQLColumn col) throws NullPointerException {
			if (col == null)
				throw new NullPointerException("Impossible to build a GeometryValue without a column or a geometry function !");
			setColumn(col);
		}

		public GeometryValue(F geometry) throws NullPointerException {
			if (geometry == null)
				throw new NullPointerException("Impossible to build a GeometryValue without a column or a geometry function !");
			setGeometry(geometry);
		}

		@SuppressWarnings("unchecked")
		public GeometryValue(GeometryValue<F> toCopy) throws Exception {
			column = (toCopy.column == null)?null:((ADQLColumn)(toCopy.column.getCopy()));
			geomFunct = (toCopy.geomFunct == null)?null:((F)(toCopy.geomFunct.getCopy()));
		}

		public void setColumn(ADQLColumn col){
			if (col != null){
				geomFunct = null;
				column = col;
			}
		}

		public void setGeometry(F geometry){
			if (geometry != null){
				column = null;
				geomFunct = geometry;
			}
		}

		public ADQLOperand getValue(){
			return (column != null)?column:geomFunct;
		}

		public boolean isColumn(){
			return column != null;
		}

		public boolean isNumeric() {
			return getValue().isNumeric();
		}

		public boolean isString() {
			return getValue().isString();
		}

		public ADQLObject getCopy() throws Exception {
			return new GeometryValue<F>(this);
		}

		public String getName() {
			return getValue().getName();
		}

		public ADQLIterator adqlIterator(){
			return getValue().adqlIterator();
		}

		public String toADQL() {
			return getValue().toADQL();
		}
	}
}