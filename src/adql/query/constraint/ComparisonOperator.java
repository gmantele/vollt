package adql.query.constraint;

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
 * Gathers all comparison operators (numeric or not).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 11/2010
 * 
 * @see Comparison
 */
public enum ComparisonOperator {
	EQUAL,
	NOT_EQUAL,
	LESS_THAN,
	LESS_OR_EQUAL,
	GREATER_THAN,
	GREATER_OR_EQUAL,
	LIKE,
	NOTLIKE;

	public static ComparisonOperator getOperator(String str) throws UnsupportedOperationException {
		if (str.equalsIgnoreCase("="))
			return EQUAL;
		else if (str.equalsIgnoreCase("!=") || str.equalsIgnoreCase("<>"))
			return NOT_EQUAL;
		else if (str.equalsIgnoreCase("<"))
			return LESS_THAN;
		else if (str.equalsIgnoreCase("<="))
			return LESS_OR_EQUAL;
		else if (str.equalsIgnoreCase(">"))
			return GREATER_THAN;
		else if (str.equalsIgnoreCase(">="))
			return GREATER_OR_EQUAL;
		else if (str.equalsIgnoreCase("LIKE"))
			return LIKE;
		else if (str.equalsIgnoreCase("NOT LIKE"))
			return NOTLIKE;
		else
			throw new UnsupportedOperationException("Comparison operator unknown: \""+str+"\" !");
	}

	public String toADQL(){
		switch(this){
		case EQUAL:
			return "=";
		case NOT_EQUAL:
			return "!=";
		case LESS_THAN:
			return "<";
		case LESS_OR_EQUAL:
			return "<=";
		case GREATER_THAN:
			return ">";
		case GREATER_OR_EQUAL:
			return ">=";
		case LIKE:
			return "LIKE";
		case NOTLIKE:
			return "NOT LIKE";
		default:
			return "???";
		}
	}

	@Override
	public String toString(){
		return toADQL();
	}
}
