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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;

/**
 * It represents the CONTAINS function of the ADQL language.
 *
 * <p>
 * 	This function determines if a geometry is wholly contained within another.
 * 	This is most commonly used to express a "point-in-shape" condition.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	An expression to determine whether the point (25.0, -19.5) degrees is within
 * 	a circle of ten degrees radius centered on position (25.4, -20.0) degrees,
 * 	could be written as follows:
 * </p>
 * <pre>CONTAINS(POINT(25.0, -19.5), CIRCLE(25.4, -20.0, 10.0))</pre>
 * </i>
 *
 * <p>
 * 	The CONTAINS function is not symmetric in the meaning of the arguments.
 * </p>
 *
 * <p>
 * 	The CONTAINS function returns the numeric value 1 if the first argument is
 * 	in, or on, the boundary of the second argument and the numeric value 0 if it
 * 	is not.
 * </p>
 *
 * <p>
 * 	When used as a predicate in the WHERE clause of a query, the numeric return
 * 	value must be compared to the numeric values 0 or 1 to form a SQL predicate:
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>WHERE 1 = CONTAINS(POINT(25.0, -19.5), CIRCLE(25.4, -20.0, 10.0))</pre>
 * <p>for "does contain" and</p>
 * <pre>WHERE 0 = CONTAINS(POINT(25.0, -19.5), CIRCLE(25.4, -20.0, 10.0))</pre>
 * <p>for "does not contain".</p>
 * </i>
 *
 * <p>
 * 	The geometric arguments for CONTAINS may be literal values, as above, or
 * 	they may be column references, functions or expressions that return
 * 	geometric values.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>WHERE 0 = CONTAINS(t1.center, t2.footprint)</pre>
 * <p>
 * 	, where t1.center and t2.footprint are references to database columns that
 * 	contain POINT and geometric (BOX, CIRCLE, POLYGON or REGION) values
 * 	respectively.
 * </p>
 * </i>
 *
 * <p>
 * 	If the geometric arguments are expressed in different coordinate systems,
 * 	the CONTAINS function is responsible for converting one, or both, of the
 * 	arguments into a different coordinate system. If the CONTAINS function
 * 	cannot perform the required conversion then it SHOULD throw an error.
 * 	Details of the mechanism for reporting the error condition are
 * 	implementation dependent.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class ContainsFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "CONTAINS", true, "Determine if a geometry is wholly contained within another.");

	/** The first geometry. */
	private GeometryValue<GeometryFunction> leftParam;

	/** The second geometry. */
	private GeometryValue<GeometryFunction> rightParam;

	/**
	 * Builds a CONTAINS function.
	 *
	 * @param left					Its first geometry (the one which must be
	 *            					included the second).
	 * @param right					Its second geometry (the one which must
	 *             					include the first).
	 * @throws NullPointerException	If one parameter is NULL.
	 */
	public ContainsFunction(GeometryValue<GeometryFunction> left, GeometryValue<GeometryFunction> right) throws NullPointerException {
		super();
		if (left == null || right == null)
			throw new NullPointerException("A CONTAINS function must have two parameters different from NULL!");

		leftParam = left;
		rightParam = right;
	}

	/**
	 * Builds a CONTAINS function by copying the given one.
	 *
	 * @param toCopy		The CONTAINS function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public ContainsFunction(ContainsFunction toCopy) throws Exception {
		super();
		leftParam = (GeometryValue<GeometryFunction>)(toCopy.leftParam.getCopy());
		rightParam = (GeometryValue<GeometryFunction>)(toCopy.rightParam.getCopy());
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ContainsFunction(this);
	}

	@Override
	public String getName() {
		return "CONTAINS";
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

	@Override
	public boolean isString() {
		return false;
	}

	@Override
	public boolean isGeometry() {
		return false;
	}

	/**
	 * @return The leftParam.
	 */
	public final GeometryValue<GeometryFunction> getLeftParam() {
		return leftParam;
	}

	/**
	 * @param leftParam The leftParam to set.
	 */
	public final void setLeftParam(GeometryValue<GeometryFunction> leftParam) {
		if (leftParam != null) {
			this.leftParam = leftParam;
			setPosition(null);
		}
	}

	/**
	 * @return The rightParam.
	 */
	public final GeometryValue<GeometryFunction> getRightParam() {
		return rightParam;
	}

	/**
	 * @param rightParam The rightParam to set.
	 */
	public final void setRightParam(GeometryValue<GeometryFunction> rightParam) {
		if (rightParam != null) {
			this.rightParam = rightParam;
			setPosition(null);
		}
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ leftParam, rightParam };
	}

	@Override
	public int getNbParameters() {
		return 2;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		if (index == 0)
			return leftParam.getValue();
		else if (index == 1)
			return rightParam.getValue();
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
	}

	@Override
	@SuppressWarnings("unchecked")
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (replacer == null)
			throw new NullPointerException("Impossible to remove one parameter from the " + getName() + " function!");
		else if (!(replacer instanceof GeometryValue || replacer instanceof ADQLColumn || replacer instanceof GeometryFunction))
			throw new Exception("Impossible to replace GeometryValue/Column/GeometryFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");

		ADQLOperand replaced = null;
		if (index == 0) {
			replaced = leftParam;
			if (replacer instanceof GeometryValue)
				leftParam = (GeometryValue<GeometryFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				leftParam.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				leftParam.setGeometry((GeometryFunction)replacer);
		} else if (index == 1) {
			replaced = rightParam;
			if (replacer instanceof GeometryValue)
				rightParam = (GeometryValue<GeometryFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				rightParam.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				rightParam.setGeometry((GeometryFunction)replacer);
		} else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
		setPosition(null);
		return replaced;
	}

}
