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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;

/**
 * <p>It represents the AREA function of ADQL.</p>
 * 
 * <p>This function computes the area, in square degrees, of a given geometry.</p>
 * 
 * <p><i><u>Example:</u><br/>AREA(CIRCLE('ICRS GEOCENTER', 25.4, -20.0, 1)).</i></p>
 * 
 * <p>Inappropriate geometries for this construct (e.g. POINT) SHOULD either return zero or throw an error message. <b>This choice must be done in an extended class of {@link AreaFunction}</b>.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public class AreaFunction extends GeometryFunction {

	/** The only parameter of this function. */
	private GeometryValue<GeometryFunction> parameter;

	/**
	 * Builds an AREA function with its parameter.
	 * 
	 * @param param					Parameter of AREA.
	 * @throws NullPointerException	If the given operand is <i>null</i> or if it's not a {@link GeometryFunction}.
	 */
	public AreaFunction(GeometryValue<GeometryFunction> param) throws NullPointerException{
		super();
		if (param == null)
			throw new NullPointerException("The only parameter of an AREA function must be different from NULL !");
		if (!(param instanceof GeometryValue))
			throw new NullPointerException("The ADQL function AREA must have one geometric parameter (a GeometryValue) !");

		parameter = param;
	}

	/**
	 * Builds an AREA function by copying the given one.
	 * 
	 * @param toCopy		The AREA function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public AreaFunction(AreaFunction toCopy) throws Exception{
		super();
		parameter = (GeometryValue<GeometryFunction>)(toCopy.parameter.getCopy());
	}

	/**
	 * Gets the parameter of the AREA function (so, a region whose the area must be computed).
	 * 
	 * @return A region.
	 */
	public final GeometryValue<GeometryFunction> getParameter(){
		return parameter;
	}

	/**
	 * Sets the parameter of the AREA function (so, a region whose the area must be computed).
	 * 
	 * @param parameter A region.
	 */
	public final void setParameter(GeometryValue<GeometryFunction> parameter){
		this.parameter = parameter;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new AreaFunction(this);
	}

	@Override
	public String getName(){
		return "AREA";
	}

	@Override
	public boolean isNumeric(){
		return true;
	}

	@Override
	public boolean isString(){
		return false;
	}

	@Override
	public boolean isGeometry(){
		return false;
	}

	@Override
	public ADQLOperand[] getParameters(){
		return new ADQLOperand[]{parameter.getValue()};
	}

	@Override
	public int getNbParameters(){
		return 1;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException{
		if (index == 0)
			return parameter.getValue();
		else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		if (index == 0){
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
				throw new Exception("Impossible to replace a GeometryValue/Column/GeometryFunction by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
			return replaced;
		}else
			throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + "\" !");
	}

}