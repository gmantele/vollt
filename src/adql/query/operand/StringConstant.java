package adql.query.operand;

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

import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.NullADQLIterator;

/**
 * A string constant.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (10/2014)
 */
public final class StringConstant implements ADQLOperand {

	private String value;

	public StringConstant(String value){
		this.value = value;
	}

	public StringConstant(StringConstant toCopy){
		this.value = toCopy.value;
	}

	public final String getValue(){
		return value;
	}

	public final void setValue(String value){
		this.value = value;
	}

	@Override
	public final boolean isNumeric(){
		return false;
	}

	@Override
	public final boolean isString(){
		return true;
	}

	@Override
	public final boolean isGeometry(){
		return false;
	}

	@Override
	public ADQLObject getCopy(){
		return new StringConstant(this);
	}

	@Override
	public String getName(){
		return toADQL();
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new NullADQLIterator();
	}

	@Override
	public String toADQL(){
		return "'" + value.replaceAll("'", "''") + "'";
	}

}
