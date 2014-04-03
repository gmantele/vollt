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

import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ADQLOrder;
import adql.query.ADQLQuery;
import adql.query.ClauseConstraints;
import adql.query.ClauseSelect;
import adql.query.ColumnReference;
import adql.query.SelectAllColumns;
import adql.query.SelectItem;

import adql.query.constraint.ADQLConstraint;
import adql.query.constraint.Between;
import adql.query.constraint.Comparison;
import adql.query.constraint.Exists;
import adql.query.constraint.In;
import adql.query.constraint.IsNull;
import adql.query.constraint.NotConstraint;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.Concatenation;
import adql.query.operand.NegativeOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.Operation;
import adql.query.operand.StringConstant;
import adql.query.operand.WrappedOperand;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.MathFunction;
import adql.query.operand.function.SQLFunction;
import adql.query.operand.function.UserDefinedFunction;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.ExtractCoordSys;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;

/**
 * Translates ADQL objects into any language (i.e. SQL).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 * 
 * @see PostgreSQLTranslator
 */
public interface ADQLTranslator {

	public String translate(ADQLObject obj) throws TranslationException;

	public String translate(ADQLQuery query) throws TranslationException;

	/* ***** LIST & CLAUSE ***** */
	public String translate(ADQLList<? extends ADQLObject> list) throws TranslationException;

	public String translate(ClauseSelect clause) throws TranslationException;

	public String translate(ClauseConstraints clause) throws TranslationException;

	public String translate(SelectItem item) throws TranslationException;

	public String translate(SelectAllColumns item) throws TranslationException;

	public String translate(ColumnReference ref) throws TranslationException;

	public String translate(ADQLOrder order) throws TranslationException;

	/* ***** TABLE & JOIN ***** */
	public String translate(FromContent content) throws TranslationException;

	public String translate(ADQLTable table) throws TranslationException;

	public String translate(ADQLJoin join) throws TranslationException;

	/* ***** OPERAND ***** */
	public String translate(ADQLOperand op) throws TranslationException;

	public String translate(ADQLColumn column) throws TranslationException;

	public String translate(Concatenation concat) throws TranslationException;

	public String translate(NegativeOperand negOp) throws TranslationException;

	public String translate(NumericConstant numConst) throws TranslationException;

	public String translate(StringConstant strConst) throws TranslationException;

	public String translate(WrappedOperand op) throws TranslationException;

	public String translate(Operation op) throws TranslationException;

	/* ***** CONSTRAINT ***** */
	public String translate(ADQLConstraint cons) throws TranslationException;

	public String translate(Comparison comp) throws TranslationException;

	public String translate(Between comp) throws TranslationException;

	public String translate(Exists exists) throws TranslationException;

	public String translate(In in) throws TranslationException;

	public String translate(IsNull isNull) throws TranslationException;

	public String translate(NotConstraint notCons) throws TranslationException;

	/* ***** FUNCTIONS ***** */
	public String translate(ADQLFunction fct) throws TranslationException;

	public String translate(SQLFunction fct) throws TranslationException;

	public String translate(MathFunction fct) throws TranslationException;

	public String translate(UserDefinedFunction fct) throws TranslationException;

	/* ***** GEOMETRICAL FUNCTIONS ***** */
	public String translate(GeometryFunction fct) throws TranslationException;

	public String translate(GeometryValue<? extends GeometryFunction> geomValue) throws TranslationException;

	public String translate(ExtractCoord extractCoord) throws TranslationException;

	public String translate(ExtractCoordSys extractCoordSys) throws TranslationException;

	public String translate(AreaFunction areaFunction) throws TranslationException;

	public String translate(CentroidFunction centroidFunction) throws TranslationException;

	public String translate(DistanceFunction fct) throws TranslationException;

	public String translate(ContainsFunction fct) throws TranslationException;

	public String translate(IntersectsFunction fct) throws TranslationException;

	public String translate(PointFunction point) throws TranslationException;

	public String translate(CircleFunction circle) throws TranslationException;

	public String translate(BoxFunction box) throws TranslationException;

	public String translate(PolygonFunction polygon) throws TranslationException;

	public String translate(RegionFunction region) throws TranslationException;
}
