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
 * It represents the INTERSECTS function of the ADQL language.
 *
 * <p>
 * 	This function determines if two geometry values overlap. This is most
 * 	commonly used to express a "shape-vs-shape" intersection test.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	An expression to determine whether a circle of one degree radius centered on
 * 	position (25.4, -20.0) degrees overlaps with a box of ten degrees centered
 * 	on position (20.0, -15.0) degrees, could be written as follows:
 * </p>
 * <pre>INTERSECTS(CIRCLE(25.4, -20.0, 1), BOX(20.0, -15.0, 10, 10))</pre>
 * <p>
 * 	, where the INTERSECTS function returns the numeric value 1 if the two
 * 	arguments overlap and 0 if they do not.
 * </p>
 * </i>
 *
 * <p>
 * 	When used as a predicate in the WHERE clause of a query, the numeric return
 * 	value should be compared to the numeric values 0 or 1 to form a SQL
 * 	predicate.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>WHERE 1 = INTERSECTS(CIRCLE(25.4, -20.0, 1), BOX(20.0, -15.0, 10, 10))</pre>
 * <p>for "does intersect" and</p>
 * <pre>WHERE 0 = INTERSECTS(CIRCLE(25.4, -20.0, 1), BOX(20.0, -15.0, 10, 10))</pre>
 * <p>for "does not intersect".</p>
 * </i>
 *
 * <p>
 * 	The geometric arguments for INTERSECTS may be literal values, as above, or
 * 	they may be column references, functions or expressions that return
 * 	geometric values.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>WHERE 0 = INTERSECTS(t1.target, t2.footprint)</pre>
 * <p>
 * 	, where t1.target and t2.footprint are references to database columns that
 * 	contain geometric (BOX, CIRCLE, POLYGON or REGION) values.
 * </p>
 * </i>
 *
 * <p>
 * 	The arguments to INTERSECTS SHOULD be geometric expressions evaluating to
 * 	either BOX, CIRCLE, POLYGON or REGION. Previous versions of this
 * 	specification (< 2.1) also allowed POINT values and required server
 * 	implementations to interpret the expression as a CONTAINS with the POINT
 * 	moved into the first position. Server implementations SHOULD still
 * 	implement that behaviour, but clients SHOULD NOT expect it. This behaviour
 * 	MAY be dropped in the next major version of this specification (> 2.1).
 * </p>
 *
 * <p>
 * 	If the geometric arguments are expressed in different coordinate systems,
 * 	the INTERSECTS function is responsible for converting one, or both, of the
 * 	arguments into a different coordinate system. If the INTERSECTS function
 *  cannot perform the required conversion then it SHOULD throw an error.
 *  Details of the mechanism for reporting the error condition are
 *  implementation dependent.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class IntersectsFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "CONTAINS", true, "Determines if two geometry values overlap.");

	/** The first geometry. */
	private GeometryValue<GeometryFunction> leftParam;

	/** The second geometry. */
	private GeometryValue<GeometryFunction> rightParam;

	/**
	 * Builds an INTERSECTS function.
	 *
	 * @param param1				The first geometry.
	 * @param param2				The second geometry.
	 * @throws NullPointerException	If there is an error with at least one of
	 *                             	the parameters.
	 */
	public IntersectsFunction(GeometryValue<GeometryFunction> param1, GeometryValue<GeometryFunction> param2) throws NullPointerException {
		super();
		if (param1 == null || param2 == null)
			throw new NullPointerException("An INTERSECTS function must have two parameters different from NULL!");

		leftParam = param1;
		rightParam = param2;
	}

	/**
	 * Builds an INTERSECTS function by copying the given one.
	 *
	 * @param toCopy		The INTERSECTS function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public IntersectsFunction(IntersectsFunction toCopy) throws Exception {
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
		return new IntersectsFunction(this);
	}

	@Override
	public String getName() {
		return "INTERSECTS";
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
			throw new NullPointerException("Impossible to remove one parameter from a " + getName() + " function!");
		else if (!(replacer instanceof GeometryValue || replacer instanceof ADQLColumn || replacer instanceof GeometryFunction))
			throw new Exception("Impossible to replace a GeometryValue/Column/GeometryFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");

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
