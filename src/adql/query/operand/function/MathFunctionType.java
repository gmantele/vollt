package adql.query.operand.function;

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

/**
 * All types of managed mathematical functions.
 * 
 * @author Gr&eacute;gory Mantelet (CDS,ARI)
 * @version 03/2014
 * 
 * @see MathFunction
 */
public enum MathFunctionType{
	ABS(1), CEILING(1), DEGREES(1), EXP(1), FLOOR(1), LOG(1),		// returns the natural logarithm (base e) of a double value.
	LOG10(1),	// returns the base 10 logarithm of a double value.
	MOD(2), PI(0), POWER(2), RADIANS(1), SQRT(1), RAND(0,1), ROUND(1,2), TRUNCATE(1,2), SQUARE(1), SIGN(1),

	ACOS(1), ASIN(1), ATAN(1), ATAN2(2), COS(1), COT(1), SIN(1), TAN(1);

	/** @since 1.2 */
	private final int nbMinRequiredParameters;
	/** @since 1.2 */
	private final int nbMaxRequiredParameters;

	private MathFunctionType(int nbParams){
		this(nbParams, nbParams);
	}

	/** @since 1.2 */
	private MathFunctionType(int nbMinParams, int nbMaxParams){
		nbMinRequiredParameters = nbMinParams;
		nbMaxRequiredParameters = nbMaxParams;
	}

	/** @since 1.2 */
	public final int nbMinParams(){
		return nbMinRequiredParameters;
	}

	/** @since 1.2 */
	public final int nbMaxParams(){
		return nbMaxRequiredParameters;
	}
}
