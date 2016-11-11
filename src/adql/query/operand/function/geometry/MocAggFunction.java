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
 * Copyright 2016 - Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;

/**
 * <p>It represents a MOC_AGG function aggregating Healpix index or positions (i.e. POINT) into a MOC.</p>
 * 
 * <p><b>WARNING:</b>
 * 	This function is NOT part of the standard ADQL language (v1.0).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 1.4 (11/2016)
 */
public class MocAggFunction extends GeometryFunction {

	/** The ADQL expression representing an Healpix index (so an unsigned integer).
	 * <p><i>This attribute is <code>null</code> if an ADQL expression representing a POINT is set instead.</i></p> */
	protected ADQLOperand hpxindex = null;

	/** The ADQL expression representing a position (i.e. POINT).
	 * <p><i>This attribute is <code>null</code> if an ADQL expression representing an Healpix index is set instead.</i></p> */
	protected PointFunction point = null;

	/** The Healpix order at which the aggregated MOC must be. */
	protected ADQLOperand hpxorder;

	/**
	 * Create the MOC_AGG function with an ADQL expression representing an Healpix index and an Healpix order.
	 * 
	 * @param hpxindex	ADQL expression representing an Healpix index. (MUST be numerical expression)
	 * @param hpxorder	Healpix order of the MOC to create.
	 * 
	 * @throws NullPointerException	If either the Healpix index's expression or the Healpix order is <code>null</code>.
	 * @throws Exception			If not both the given operands are numerical expressions.
	 */
	public MocAggFunction(final ADQLOperand hpxindex, final ADQLOperand hpxorder) throws NullPointerException, Exception{
		super((ADQLOperand)null);
		setHpxIndex(hpxindex);
		setHpxOrder(hpxorder);
	}

	/**
	 * Create the MOC_AGG function with an ADQL expression representing an Healpix index and an Healpix order.
	 * 
	 * @param hpxindex	ADQL expression representing an Healpix index. (MUST be numerical expression)
	 * @param hpxorder	Healpix order of the MOC to create.
	 * 
	 * @throws NullPointerException	If either the POINT's expression or the Healpix order is <code>null</code>.
	 * @throws Exception			If the POINT's expression is not a geometry,
	 *                  			or if the Healpix order is not a numerical expression.
	 */
	public MocAggFunction(final PointFunction point, final ADQLOperand hpxorder) throws NullPointerException, Exception{
		super((ADQLOperand)null);
		setPoint(point);
		setHpxOrder(hpxorder);
	}

	/**
	 * Builds a MOC_AGG function by copying the given one.
	 * 
	 * @param toCopy		The MOC_AGG function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public MocAggFunction(final MocAggFunction toCopy) throws Exception{
		super(toCopy);
		if (toCopy.hpxindex != null)
			hpxindex = (ADQLOperand)toCopy.hpxindex.getCopy();
		else
			point = (PointFunction)toCopy.point.getCopy();
		hpxorder = (ADQLOperand)toCopy.hpxorder.getCopy();
	}

	/**
	 * Get the ADQL expression representing an Healpix index for each iteration of the aggregate function.
	 * 
	 * <p>
	 * 	This function returns NULL if the first parameter is a POINT
	 * 	instead. In such case, you should call the function {@link #getPoint()}.
	 * </p>
	 * 
	 * @return	The ADQL expression of this aggregate function.
	 */
	public final ADQLOperand getHpxindex(){
		return hpxindex;
	}

	/**
	 * Set the ADQL expression representing an Healpix index for each iteration of the aggregate function.
	 * 
	 * <p><b>Important:</b>
	 * 	This expression MUST return an unsigned BIGINT (i.e. unsigned long integer).
	 * </p>
	 * 
	 * <p><i>Note:</i>
	 * 	If this function was originally set with a POINT, it will dropped and the given expression
	 * 	will be used instead.
	 * </p>
	 * 
	 * @param hpxindex	The new ADQL expression of this aggregate function.
	 * 
	 * @throws NullPointerException	If the given operand is <code>null</code>.
	 * @throws Exception			If the given operand is not a numeric expression.
	 */
	public final void setHpxIndex(final ADQLOperand hpxindex) throws NullPointerException, Exception{
		if (hpxindex == null)
			throw new NullPointerException("Missing aggregate expression!");
		else if (!hpxindex.isNumeric())
			throw new Exception("The function " + getName() + " need a numeric expression as first parameter!");

		this.hpxindex = hpxindex;
		this.point = null;
	}

	/**
	 * Get the ADQL expression representing a POINT for each iteration of the aggregate function.
	 * 
	 * <p>
	 * 	This function returns NULL if the first parameter is numeric expression
	 * 	instead. In such case, you should call the function {@link #getHpxIndex()}.
	 * </p>
	 * 
	 * @return	The ADQL expression of this aggregate function.
	 */
	public final PointFunction getPoint(){
		return point;
	}

	/**
	 * Set the ADQL expression representing a POINT for each iteration of the aggregate function.
	 * 
	 * <p><b>Important:</b>
	 * 	This expression MUST return a POINT.
	 * </p>
	 * 
	 * <p><i>Note:</i>
	 * 	If this function was originally set with a numeric expression, it will dropped and the given expression
	 * 	will be used instead.
	 * </p>
	 * 
	 * @param point	The new ADQL expression of this aggregate function.
	 * 
	 * @throws NullPointerException	If the given operand is <code>null</code>.
	 * @throws Exception			If the given operand is not a geometry.
	 */
	public final void setPoint(final PointFunction point) throws NullPointerException, Exception{
		if (point == null)
			throw new NullPointerException("Missing aggregate expression!");
		else if (!point.isGeometry())
			throw new Exception("The function " + getName() + " need a POINT expression as first parameter!");

		this.point = point;
		this.hpxindex = null;
	}

	/**
	 * Get the Healpix order to use in this aggregate function.
	 * 
	 * @return	The Healpix order of this function.
	 */
	public final ADQLOperand getHpxorder(){
		return hpxorder;
	}

	/**
	 * Set the Healpix order to use in this aggregate function.
	 * 
	 * @param hpxorder	The new Healpix order.
	 * 
	 * @throws NullPointerException	If the given operand is <code>null</code>.
	 * @throws Exception			If the given operand is not a numeric.
	 */
	public final void setHpxOrder(final ADQLOperand hpxorder) throws NullPointerException, Exception{
		if (hpxorder == null)
			throw new NullPointerException("Missing Healpix order!");
		else if (!hpxorder.isNumeric())
			throw new Exception("The function " + getName() + " need a numeric expression as second parameter!");

		this.hpxorder = hpxorder;
	}

	@Override
	public boolean isNumeric(){
		return false;
	}

	@Override
	public boolean isString(){
		return false;
	}

	@Override
	public boolean isGeometry(){
		return true;
	}

	@Override
	public String getName(){
		return "MOC_AGG";
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new MocAggFunction(this);
	}

	@Override
	public int getNbParameters(){
		return 2;
	}

	@Override
	public ADQLOperand[] getParameters(){
		if (hpxindex != null)
			return new ADQLOperand[]{hpxindex,hpxorder};
		else
			return new ADQLOperand[]{point,hpxorder};
	}

	@Override
	public ADQLOperand getParameter(final int index) throws ArrayIndexOutOfBoundsException{
		switch(index){
			case 0:
				if (hpxindex != null)
					return hpxindex;
				else
					return point;
			case 1:
				return hpxorder;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + " !");
		}
	}

	@Override
	public ADQLOperand setParameter(final int index, final ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception{
		ADQLOperand replaced = null;
		switch(index){
			case 0:
				if (replacer instanceof PointFunction){
					replaced = point;
					setPoint((PointFunction)replacer);
				}else{
					replaced = hpxindex;
					setHpxIndex(replacer);
				}
				break;
			case 1:
				replaced = hpxorder;
				setHpxOrder(replacer);
				break;
			default:
				throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \"" + getName() + " !");
		}
		return replaced;
	}

}
