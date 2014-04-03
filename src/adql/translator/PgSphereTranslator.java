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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.query.constraint.Comparison;
import adql.query.constraint.ComparisonOperator;

import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;

import adql.translator.PostgreSQLTranslator;
import adql.translator.TranslationException;

/**
 * <p>Translates all ADQL objects into the SQL adaptation of Postgres+PgSphere.
 * Actually only the geometrical functions are translated in this class.
 * The other functions are managed by {@link PostgreSQLTranslator}.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 * 
 * @see PostgreSQLTranslator
 */
public class PgSphereTranslator extends PostgreSQLTranslator {

	/**
	 * Builds a PgSphereTranslator which takes into account the case sensitivity on column names.
	 * It means that column names which have been written between double quotes, will be also translated between double quotes.
	 * 
	 * @see PostgreSQLTranslator#PostgreSQLTranslator()
	 */
	public PgSphereTranslator() {
		super();
	}

	/**
	 * Builds a PgSphereTranslator.
	 * 
	 * @param column	<i>true</i> to take into account the case sensitivity of column names, <i>false</i> otherwise.
	 * 
	 * @see PostgreSQLTranslator#PostgreSQLTranslator(boolean)
	 */
	public PgSphereTranslator(boolean column) {
		super(column);
	}

	/**
	 * Builds a PgSphereTranslator.
	 * 
	 * @param catalog	<i>true</i> to take into account the case sensitivity of catalog names, <i>false</i> otherwise.
	 * @param schema	<i>true</i> to take into account the case sensitivity of schema names, <i>false</i> otherwise.
	 * @param table		<i>true</i> to take into account the case sensitivity of table names, <i>false</i> otherwise.
	 * @param column	<i>true</i> to take into account the case sensitivity of column names, <i>false</i> otherwise.
	 * 
	 * @see PostgreSQLTranslator#PostgreSQLTranslator(boolean, boolean, boolean, boolean)
	 */
	public PgSphereTranslator(boolean catalog, boolean schema, boolean table, boolean column) {
		super(catalog, schema, table, column);
	}

	@Override
	public String translate(PointFunction point) throws TranslationException {
		StringBuffer str = new StringBuffer("spoint(");
		str.append("radians(").append(translate(point.getCoord1())).append("),");
		str.append("radians(").append(translate(point.getCoord2())).append("))");
		return str.toString();
	}

	@Override
	public String translate(CircleFunction circle) throws TranslationException {
		StringBuffer str = new StringBuffer("scircle(");
		str.append("spoint(radians(").append(translate(circle.getCoord1())).append("),");
		str.append("radians(").append(translate(circle.getCoord2())).append(")),");
		str.append("radians(").append(translate(circle.getRadius())).append("))");
		return str.toString();
	}

	@Override
	public String translate(BoxFunction box) throws TranslationException {
		StringBuffer str = new StringBuffer("sbox(");

		str.append("spoint(").append("radians(").append(translate(box.getCoord1())).append("+(").append(translate(box.getWidth())).append("/2.0)),");
		str.append("radians(").append(translate(box.getCoord2())).append("+(").append(translate(box.getHeight())).append("/2.0))),");

		str.append("spoint(").append("radians(").append(translate(box.getCoord1())).append("-(").append(translate(box.getWidth())).append("/2.0)),");
		str.append("radians(").append(translate(box.getCoord2())).append("-(").append(translate(box.getHeight())).append("/2.0))))");
		return str.toString();
	}

	@Override
	public String translate(PolygonFunction polygon) throws TranslationException {
		try {
			StringBuffer str = new StringBuffer("spoly('{'");

			if (polygon.getNbParameters() > 2){
				PointFunction point = new PointFunction(polygon.getCoordinateSystem(), polygon.getParameter(1), polygon.getParameter(2));
				str.append(" || ").append(translate(point));

				for(int i=3; i<polygon.getNbParameters() && i+1<polygon.getNbParameters(); i+=2){
					point.setCoord1(polygon.getParameter(i));
					point.setCoord2(polygon.getParameter(i+1));
					str.append(" || ',' || ").append(translate(point));
				}
			}

			str.append(" || '}')");

			return str.toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw new TranslationException(e);
		}
	}

	@Override
	public String translate(ExtractCoord extractCoord) throws TranslationException {
		StringBuffer str = new StringBuffer("degrees(");
		if (extractCoord.getName().equalsIgnoreCase("COORD1"))
			str.append("long(");
		else
			str.append("lat(");
		str.append(translate(extractCoord.getParameter(0))).append("))");
		return str.toString();
	}

	@Override
	public String translate(DistanceFunction fct) throws TranslationException {
		StringBuffer str = new StringBuffer("degrees(");
		str.append(translate(fct.getP1())).append(" <-> ").append(translate(fct.getP2())).append(")");
		return str.toString();
	}

	@Override
	public String translate(AreaFunction areaFunction) throws TranslationException {
		StringBuffer str = new StringBuffer("degrees(area(");
		str.append(translate(areaFunction.getParameter())).append("))");
		return str.toString();
	}

	@Override
	public String translate(ContainsFunction fct) throws TranslationException {
		StringBuffer str = new StringBuffer("(");
		str.append(translate(fct.getLeftParam())).append(" @ ").append(translate(fct.getRightParam())).append(")");
		return str.toString();
	}

	@Override
	public String translate(IntersectsFunction fct) throws TranslationException {
		StringBuffer str = new StringBuffer("(");
		str.append(translate(fct.getLeftParam())).append(" && ").append(translate(fct.getRightParam())).append(")");
		return str.toString();
	}

	@Override
	public String translate(Comparison comp) throws TranslationException {
		if ((comp.getLeftOperand() instanceof ContainsFunction || comp.getLeftOperand() instanceof IntersectsFunction) && (comp.getOperator() == ComparisonOperator.EQUAL || comp.getOperator() == ComparisonOperator.NOT_EQUAL) && comp.getRightOperand().isNumeric())
			return translate(comp.getLeftOperand())+" "+comp.getOperator().toADQL()+" '"+translate(comp.getRightOperand())+"'";
		else if ((comp.getRightOperand() instanceof ContainsFunction || comp.getRightOperand() instanceof IntersectsFunction) && (comp.getOperator() == ComparisonOperator.EQUAL || comp.getOperator() == ComparisonOperator.NOT_EQUAL) && comp.getLeftOperand().isNumeric())
			return "'"+translate(comp.getLeftOperand())+"' "+comp.getOperator().toADQL()+" "+translate(comp.getRightOperand());
		else
			return super.translate(comp);
	}



}
