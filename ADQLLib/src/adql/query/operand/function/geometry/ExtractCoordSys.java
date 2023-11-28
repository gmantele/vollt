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
 * It represents the COORDSYS function the ADQL language.
 *
 * <p>
 * 	This function returns the formal name of the coordinate system for a given
 * 	geometry as a string.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>
 * 	The following example would return the coordinate system of a POINT literal:
 * </p>
 * <pre>COORDSYS(POINT(25.0, -19.5))</pre>
 * <p>
 * 	, which would return a string value representing the coordinate system used
 * 	to create the POINT.
 * </p>
 * </i>
 *
 * <p>
 * 	The COORDSYS function may be applied to any expression that returns
 * 	a geometric datatype.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>COORDSYS(t.footprint)</pre>
 * <p>
 * 	, where t.footprint is a reference to a database column that contains
 * 	geometric (POINT, BOX, CIRCLE, POLYGON or REGION) values.
 * </p>
 * </i>
 *
 * <p>
 * 	From version 2.1, this function has been marked as deprecated. It may be
 * 	removed in future versions of this specification (> 2.1). Details of the
 * 	coordinate system for a database column are available as part of the service
 * 	metadata, available via the TAP_SCHEMA tables defined in the TAP
 * 	specification and the /tables web-service response defined in the VOSI
 * 	specification.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2019)
 */
public class ExtractCoordSys extends GeometryFunction {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_GEO, "COORDSYS", true, "Return the formal name of the coordinate system for a given geometry.");

	/** The geometry from which the coordinate system string must be extracted. */
	protected GeometryValue<GeometryFunction> geomExpr;

	/**
	 * Builds a COORDSYS function.
	 *
	 * @param param	The geometry from which the coordinate system string must be
	 *             	extracted.
	 */
	public ExtractCoordSys(GeometryValue<GeometryFunction> param) {
		super();
		geomExpr = param;
	}

	/**
	 * Builds a COORDSYS function by copying the given one.
	 *
	 * @param toCopy		The COORDSYS function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public ExtractCoordSys(ExtractCoordSys toCopy) throws Exception {
		super();
		geomExpr = (GeometryValue<GeometryFunction>)(toCopy.geomExpr.getCopy());
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ExtractCoordSys(this);
	}

	@Override
	public String getName() {
		return "COORDSYS";
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

	@Override
	public boolean isString() {
		return true;
	}

	@Override
	public boolean isGeometry() {
		return false;
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{ geomExpr.getValue() };
	}

	@Override
	public int getNbParameters() {
		return 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		if (index == 0)
			return geomExpr.getValue();
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function " + getName() + "!");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (index == 0) {
			ADQLOperand replaced = geomExpr.getValue();
			if (replacer == null)
				throw new NullPointerException("Impossible to remove the only required parameter of the " + getName() + " function!");
			else if (replacer instanceof GeometryValue)
				geomExpr = (GeometryValue<GeometryFunction>)replacer;
			else if (replacer instanceof ADQLColumn)
				geomExpr.setColumn((ADQLColumn)replacer);
			else if (replacer instanceof GeometryFunction)
				geomExpr.setGeometry((GeometryFunction)replacer);
			else
				throw new Exception("Impossible to replace GeometryValue/Column/GeometryFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
			setPosition(null);
			return replaced;
		} else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function " + getName() + "!");
	}

}
