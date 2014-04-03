package adql.search;

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
import adql.query.operand.ADQLColumn;

/**
 * Lets searching all {@link ADQLColumn} objects.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 */
public class SearchColumnHandler extends SimpleSearchHandler {

	public SearchColumnHandler(){
		super();
	}

	public SearchColumnHandler(boolean recursive, boolean onlyFirstMatch){
		super(recursive, onlyFirstMatch);
	}

	public SearchColumnHandler(boolean recursive){
		super(recursive);
	}

	@Override
	public boolean match(ADQLObject obj){
		return (obj instanceof ADQLColumn);
	}

}
