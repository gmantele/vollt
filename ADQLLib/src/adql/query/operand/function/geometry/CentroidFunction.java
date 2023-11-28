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
 * It represents the CENTROID function of the ADQL language.
 *
 * <p>
 * 	This function computes the centroid of a given geometry and returns a POINT.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	An expression to calculate the centroid of a POLYGON could be written as
 * 	follows :
 * </p>
 * <pre>CENTROID(POLYGON(10.0, -10.5, 20.0, 20.5, 30.0, 30.5))</pre>
 * </i>
 *
 * <p>The CENTROID of a single POINT is that POINT.</p>
 *
 * <p>
 * 	The geometry argument may be a literal value, as above, or it may be a
 * 	column reference, function or expression that returns a geometric type.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>CENTROID(t1.footprint)</pre>
 * <p>
 * 	, where t1.footprint is a reference to a database column that contains
 * 	geometric (POINT, BOX, CIRCLE, POLYGON or REGION) values.
 * </p>
 * </i>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class CentroidFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "CENTROID", true, "Compute the centroid of a given geometry and returns a POINT. Note: the CENTROID of a POINT is that POINT.");

	/** The geometry whose the centroid must be extracted. */
	protected GeometryValue<GeometryFunction> parameter;

	/**
	 * Builds a CENTROID function.
	 *
	 * @param param					The geometry whose the centroid must be
	 *             					extracted.
	 * @throws NullPointerException	If the given parameter is NULL.
	 */
	public CentroidFunction(GeometryValue<GeometryFunction> param) throws NullPointerException {
		super();
		if (param == null)
			throw new NullPointerException("The ADQL function CENTROID must have exactly one parameter!");
		parameter = param;
	}

	/**
	 * Builds a CENTROID function by copying the given one.
	 *
	 * @param toCopy		The CENTROID function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public CentroidFunction(CentroidFunction toCopy) throws Exception {
		super();
		parameter = (GeometryValue<GeometryFunction>)(toCopy.parameter.getCopy());
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new CentroidFunction(this);
	}

	@Override
	public String getName() {
		return "CENTROID";
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

	@Override
	public boolean isString() {
		return false;
	}

	@Override
	public boolean isGeometry() {
		return true;
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ parameter.getValue() };
	}

	@Override
	public int getNbParameters() {
		return 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		if (index == 0)
			return parameter.getValue();
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (index == 0) {
			ADQLOperand replaced = null;
			if (replacer == null)
				throw new NullPointerException("Impossible to remove the only required parameter of the " + getName() + " function!");
			else if (replacer instanceof GeometryValue)
				parameter = (GeometryValue<GeometryFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				parameter.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				parameter.setGeometry((GeometryFunction)replacer);
			else
				throw new Exception("Impossible to replace a GeometryValue/Column/GeometryFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
			setPosition(null);
			return replaced;
		} else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\"!");
	}

}
