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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.query.ADQLObject;

import adql.query.operand.ADQLOperand;

/**
 * <p>It represents the box function of the ADQL language.</p>
 * 
 * <p>It is specified by a center position and a size (in both coordinates).
 * The coordinates of the center position are expressed in the given coordinate system.
 * The size of the box is in degrees.</p>
 * 
 * <p><i><u>Example:</u><br />
 * BOX('ICRS GEOCENTER', 25.4, -20.0, 10, 10)<br />
 * In this example the function expressing a box o ten degrees centered in a position (25.4,-20.0) in degrees and defined according
 * to the ICRS coordinate system with GEOCENTER reference position.<br /><br />
 * BOX('ICRS GEOCENTER', t.ra, t.dec, 10, 10)<br />
 * In this second example the coordinates of the center position are extracted from a coordinate's column reference.
 * </i></p>
 * 
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 */
public class BoxFunction extends GeometryFunction {

	/** The first coordinate of the center of this box. */
	private ADQLOperand coord1;

	/** The second coordinate of the center of this box. */
	private ADQLOperand coord2;

	/** The width of this box (in degrees). */
	private ADQLOperand width;

	/** The height of this box (in degrees). */
	private ADQLOperand height;


	/**
	 * <p>Builds a BOX function.</p>
	 * 
	 * @param coordinateSystem		The coordinate system of the center position.
	 * @param firstCoord			The first coordinate of the center of this box.
	 * @param secondCoord			The second coordinate of the center of this box.
	 * @param boxWidth				The width of this box (in degrees).
	 * @param boxHeight				The height of this box (in degrees).
	 * @throws NullPointerException	If one parameter is <i>null</i>.
	 * @throws Exception 			If there is another error.
	 */
	public BoxFunction(ADQLOperand coordinateSystem, ADQLOperand firstCoord, ADQLOperand secondCoord, ADQLOperand boxWidth, ADQLOperand boxHeight) throws NullPointerException, Exception {
		super(coordinateSystem);

		if (firstCoord == null || secondCoord == null || boxWidth == null || boxHeight == null)
			throw new NullPointerException("All the parameters of the BOX function must be different from NULL !");

		coord1 = firstCoord;
		coord2 = secondCoord;
		width = boxWidth;
		height = boxHeight;
	}

	/**
	 * Builds a BOX function by copying the given one.
	 * 
	 * @param toCopy		The BOX function to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public BoxFunction(BoxFunction toCopy) throws Exception {
		super(toCopy);
		coord1 = (ADQLOperand)(toCopy.coord1.getCopy());
		coord2 = (ADQLOperand)(toCopy.coord2.getCopy());
		width = (ADQLOperand)(toCopy.width.getCopy());
		height = (ADQLOperand)(toCopy.height.getCopy());
	}

	public ADQLObject getCopy() throws Exception {
		return new BoxFunction(this);
	}

	public String getName() {
		return "BOX";
	}

	public boolean isNumeric() {
		return false;
	}

	public boolean isString() {
		return true;
	}

	/**
	 * Gets the first coordinate (i.e. right ascension).
	 * 
	 * @return The first coordinate.
	 */
	public final ADQLOperand getCoord1() {
		return coord1;
	}

	/**
	 * Sets the first coordinate (i.e. right ascension).
	 * 
	 * @param coord1 The first coordinate.
	 */
	public final void setCoord1(ADQLOperand coord1) {
		this.coord1 = coord1;
	}

	/**
	 * Gets the second coordinate (i.e. declination).
	 * 
	 * @return The second coordinate.
	 */
	public final ADQLOperand getCoord2() {
		return coord2;
	}

	/**
	 * Sets the second coordinate (i.e. declination).
	 * 
	 * @param coord2 The second coordinate.
	 */
	public final void setCoord2(ADQLOperand coord2) {
		this.coord2 = coord2;
	}

	/**
	 * Gets the width of the box.
	 * 
	 * @return The width.
	 */
	public final ADQLOperand getWidth() {
		return width;
	}

	/**
	 * Sets the width of the box.
	 * 
	 * @param width The width.
	 */
	public final void setWidth(ADQLOperand width) {
		this.width = width;
	}

	/**
	 * Gets the height of the box.
	 * 
	 * @return The height.
	 */
	public final ADQLOperand getHeight() {
		return height;
	}

	/**
	 * Sets the height of the box.
	 * 
	 * @param height The height.
	 */
	public final void setHeight(ADQLOperand height) {
		this.height = height;
	}

	@Override
	public ADQLOperand[] getParameters() {
		return new ADQLOperand[]{coordSys, coord1, coord2, width, height};
	}

	@Override
	public int getNbParameters() {
		return 5;
	}

	@Override
	public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
		switch(index){
		case 0:
			return coordSys;
		case 1:
			return coord1;
		case 2:
			return coord2;
		case 3:
			return width;
		case 4:
			return height;
		default:
			throw new ArrayIndexOutOfBoundsException("No "+index+"-th parameter for the function \""+getName()+"\" !");
		}
	}

	@Override
	public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
		if (replacer == null)
			throw new NullPointerException("Impossible to remove one parameter from a "+getName()+" function !");
		else if (!(replacer instanceof ADQLOperand))
			throw new Exception("Impossible to replace an ADQLOperand by a "+replacer.getClass().getName()+" ("+replacer.toADQL()+") !");

		ADQLOperand replaced = null;
		switch(index){
		case 0:
			replaced = coordSys;
			setCoordinateSystem(replacer);
			break;
		case 1:
			replaced = coord1;
			coord1 = replacer;
			break;
		case 2:
			replaced = coord2;
			coord2 = replacer;
			break;
		case 3:
			replaced = width;
			width = replacer;
			break;
		case 4:
			replaced = height;
			height = replacer;
			break;
		default:
			throw new ArrayIndexOutOfBoundsException("No "+index+"-th parameter for the function \""+getName()+"\" !");
		}
		return replaced;
	}

}
