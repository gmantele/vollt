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
 * It represents the AREA function of ADQL.
 *
 * <p>
 * 	This function computes the area, in square degrees, of a given geometry.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	An expression to calculate the area of a POLYGON could be written as
 * 	follows:
 * </p>
 * <pre>AREA(POLYGON(10.0, -10.5, 20.0, 20.5, 30.0, 30.5))</pre>
 * </i>
 *
 * <p>The AREA of a single POINT is zero.</p>
 *
 * <p>
 * 	The geometry argument may be a literal value, as above, or it may be a
 * 	column reference, function or expression that returns a geometric type.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>AREA(t1.footprint)</pre>
 * <p>
 * 	,where t1.footprint is a reference to a database column that contains
 * 	geometric (POINT, BOX, CIRCLE, POLYGON or REGION) values.
 * </p>
 * </i>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class AreaFunction extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "AREA", true, "Compute the area (in square degrees) of a given geometry. Note: AREA of a POINT is always zero.");

	/** The only parameter of this function. */
	private GeometryValue<GeometryFunction> parameter;

	/**
	 * Builds an AREA function with its parameter.
	 *
	 * @param param					Parameter of AREA.
	 * @throws NullPointerException	If the given operand is NULL
	 *                             	or if it's not a {@link GeometryFunction}.
	 */
	public AreaFunction(GeometryValue<GeometryFunction> param) throws NullPointerException {
		super();
		if (param == null)
			throw new NullPointerException("The only parameter of an AREA function must be different from NULL!");
		if (!(param instanceof GeometryValue))
			throw new NullPointerException("The ADQL function AREA must have one geometric parameter (a GeometryValue)!");

		parameter = param;
	}

	/**
	 * Builds an AREA function by copying the given one.
	 *
	 * @param toCopy		The AREA function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public AreaFunction(AreaFunction toCopy) throws Exception {
		super();
		parameter = (GeometryValue<GeometryFunction>)(toCopy.parameter.getCopy());
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Gets the parameter of the AREA function (so, a region whose the area must
	 * be computed).
	 *
	 * @return A region.
	 */
	public final GeometryValue<GeometryFunction> getParameter() {
		return parameter;
	}

	/**
	 * Sets the parameter of the AREA function (so, a region whose the area must
	 * be computed).
	 *
	 * @param parameter A region.
	 */
	public final void setParameter(GeometryValue<GeometryFunction> parameter) {
		this.parameter = parameter;
		setPosition(null);
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new AreaFunction(this);
	}

	@Override
	public String getName() {
		return "AREA";
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
			ADQLOperand replaced = parameter.getValue();
			if (replacer == null)
				throw new NullPointerException("");
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
