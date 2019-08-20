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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.operand.function.SQLFunction;

/**
 * Gathers all comparison operators (numeric or not).
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (08/2019)
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
	/** @since 2.0 */
	ILIKE,
	NOTLIKE,
	/** @since 2.0 */
	NOTILIKE;

	/** Description of the ADQL Feature based on this type.
	 * @since 2.0 */
	private final LanguageFeature FEATURE;

	/** @since 2.0 */
	private ComparisonOperator() {
		if (name().endsWith("ILIKE"))
			FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_STRING, "ILIKE", true, "Perform a case-insensitive comparison between its string operands.");
		else
			FEATURE = new LanguageFeature(null, this.name(), false);
	}

	/**
	 * Get the description of the ADQL's Language Feature based on this type.
	 *
	 * <p><i><b>Note:</b>
	 * 	Getting this description is generally only useful when discovery
	 * 	optional features so that determining if they are allowed to be used in
	 * 	ADQL queries.
	 * </i></p>
	 *
	 * @return	Description of this ADQL object as an ADQL's feature.
	 *
	 * @since 2.0
	 * @see SQLFunction#getFeatureDescription()
	 */
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

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
		else if (str.equalsIgnoreCase("ILIKE"))
			return ILIKE;
		else if (str.equalsIgnoreCase("NOT LIKE"))
			return NOTLIKE;
		else if (str.equalsIgnoreCase("NOT ILIKE"))
			return NOTILIKE;
		else
			throw new UnsupportedOperationException("Comparison operator unknown: \"" + str + "\" !");
	}

	public String toADQL() {
		switch(this) {
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
			case ILIKE:
				return "ILIKE";
			case NOTLIKE:
				return "NOT LIKE";
			case NOTILIKE:
				return "NOT ILIKE";
			default:
				return "???";
		}
	}

	@Override
	public String toString() {
		return toADQL();
	}
}
