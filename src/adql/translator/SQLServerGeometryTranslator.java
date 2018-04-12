package adql.translator;

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
 * Copyright 2017 - European Southern Observatory (ESO)
 */

import adql.db.DBType;
import adql.db.STCS.Region;
import adql.parser.ParseException;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.function.geometry.*;

/**
 * Class that implements the translation of ADQL spatial functions
 * into SQL specific to MS SQL Server.
 *
 * MS SQL Server uses Latitude and Longitude instead of RA and Dec,
 * where RA = Long + 180 and Dec = Lat.
 *
 * Box and Region are currently not implemented.
 *
 * @author Vincenzo Forch&igrave (ESO), vforchi@eso.org, vincenzo.forchi@gmail.com
 */
public class SQLServerGeometryTranslator extends SQLServerTranslator {

	/**
	 * the precision used in the str function to convert coordinate values: if this is not
	 * specified, they are rounded to integers
	 */
    public static final String strPrecision = ", 11, 7";

	@Override
	public String translate(final ExtractCoord extractCoord) throws TranslationException {
		StringBuffer buf = new StringBuffer();
		buf.append(translate(extractCoord.getParameter(0)));
		if (extractCoord.getName().equals("COORD1"))
			buf.append(".Long+180");
		else
			buf.append(".Lat");

		return buf.toString();
	}

	@Override
	public String translate(final ExtractCoordSys extractCoordSys) throws TranslationException {
		throw new TranslationException("COORDSYS has been deprecated and is not supported");
	}

	@Override
	public String translate(final AreaFunction area) throws TranslationException {
		StringBuffer buf = new StringBuffer();
		buf.append("degrees(degrees((");
		buf.append(translate(area.getParameter()));
		buf.append(").STArea()").append("))");
		return buf.toString();
	}

	@Override
	public String translate(final CentroidFunction centroid) throws TranslationException {
		StringBuffer buf = new StringBuffer("(");
		buf.append(translate(centroid.getParameter(0)));
		buf.append(").EnvelopeCenter()");
		return buf.toString();
	}

	@Override
	public String translate(final DistanceFunction distance) throws TranslationException {
		StringBuffer buf = new StringBuffer();
		buf.append("degrees((");
		buf.append(translate(distance.getP1()));
		buf.append(").STDistance(");
		buf.append(translate(distance.getP2()));
		buf.append("))");
		return buf.toString();
	}

	@Override
	public String translate(final ContainsFunction contains) throws TranslationException {
		StringBuffer buf = new StringBuffer("(");

		/*
		 * The standard specifies that this function has two parameters and it returns
		 * true if the first one is contained in the second one.
		 * If the left parameter is a region we use STWithin, to make sure that the DB
		 * uses the index.
		 */
		if (contains.getLeftParam().getValue() instanceof ADQLColumn) {
			buf.append(translate(contains.getLeftParam()));
			buf.append(").STWithin(");
			buf.append(translate(contains.getRightParam()));
		} else {
			buf.append(translate(contains.getRightParam()));
			buf.append(").STContains(");
			buf.append(translate(contains.getLeftParam()));
		}
		buf.append(")");
		return buf.toString();
	}

	@Override
	public String translate(final IntersectsFunction intersects) throws TranslationException {
		StringBuffer buf = new StringBuffer("(");
		/*
		 *  if the right parameter is a column we use it as the first operand, to make the DB
		 *  use the index.
		 */
		if (intersects.getRightParam().getValue() instanceof ADQLColumn) {
			buf.append(translate(intersects.getRightParam()));
			buf.append(").STIntersects(");
			buf.append(translate(intersects.getLeftParam()));
		} else {
			buf.append(translate(intersects.getLeftParam()));
			buf.append(").STIntersects(");
			buf.append(translate(intersects.getRightParam()));
		}
		buf.append(")");
		return buf.toString();
	}

	@Override
	public StringBuffer appendIdentifier(final StringBuffer str, final String id, final boolean caseSensitive) {
		/* in SQLServer columns are escaped with square brackets */
		if (caseSensitive && !id.matches("\"[^\"]*\""))
			return str.append('[').append(id).append(']');
		else
			return str.append(id);
	}

	@Override
	public String translate(final PointFunction point) throws TranslationException {
		return translatePoint(point.getCoord1(), point.getCoord2());
	}

	@Override
	public String translate(final CircleFunction circle) throws TranslationException {
		StringBuffer buf = new StringBuffer("(");
		buf.append(translatePoint(circle.getCoord1(), circle.getCoord2()));
		buf.append(").STBuffer(radians(cast(");
		buf.append(translate(circle.getRadius()));
		buf.append(" as double precision)))");
		return buf.toString();
	}

	/**
	 * Helper function to convert two operands into a POINT
	 * @param coord1
	 * @param coord2
	 * @return
	 * @throws TranslationException
	 */
	private String translatePoint(ADQLOperand coord1, ADQLOperand coord2) throws TranslationException {
		/*
		 * the point needs to be translated to something like
		 * 'POINT('+str(s_-ra+180.0, 11, 7)+' '+ str(s_dec, 11, 7)+')'
		 */
		StringBuffer buf = new StringBuffer();
		buf.append("geography::STGeomFromText('POINT(");
		buf.append("'+str(180 -(");
		buf.append(translate(coord1));
		buf.append(")").append(strPrecision).append(")+' '+str(");
		buf.append(translate(coord2)).append(strPrecision);
		buf.append(")+')', 104001)");
		return buf.toString();
	}

	@Override
	public String translate(final BoxFunction box) throws TranslationException {
		throw new TranslationException("BOX is currently not implemented");
	}

	@Override
	public String translate(final PolygonFunction polygon) throws TranslationException {
		StringBuffer buf = new StringBuffer();
		buf.append("geography::STGeomFromText('POLYGON((");

		for (int i = 1; i < polygon.getNbParameters(); i+=2) {
			buf.append("'+str(180-(");
			buf.append(translate(polygon.getParameter(i)));
			buf.append(")").append(strPrecision).append(")+' '+str(");
			buf.append(translate(polygon.getParameter(i+1))).append(strPrecision);
			buf.append(")+',");
		}

		/* In SQLServer the polygon has to be closed, so we add the first point again */
		buf.append("'+str(180 - (");
		buf.append(translate(polygon.getParameter(1)));
		buf.append(")").append(strPrecision).append(")+' '+str(");
		buf.append(translate(polygon.getParameter(2))).append(strPrecision);
		buf.append(")+'))', 104001)");
		return buf.toString();
	}

	@Override
	public String translate(final RegionFunction region) throws TranslationException {
		throw new TranslationException("REGION is currently not implemented");
	}

	@Override
	public Region translateGeometryFromDB(final Object jdbcColValue) throws ParseException {
		return CLRToRegionParser.parseRegion((byte[]) jdbcColValue);
	}

	@Override
	public DBType convertTypeFromDB(final int dbmsType, final String rawDbmsTypeName, String dbmsTypeName, final String[] params) {
		if ("geography".equals(dbmsTypeName))
			return new DBType(DBType.DBDatatype.REGION);
		else
			return super.convertTypeFromDB(dbmsType, rawDbmsTypeName, dbmsTypeName, params);
	}
}
