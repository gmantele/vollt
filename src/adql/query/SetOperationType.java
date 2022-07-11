package adql.query;

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
 * Copyright 2022 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.feature.LanguageFeature;

/**
 * Type of possible operation between two rows sets.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (07/2022)
 * @since 2.0
 *
 * @see SetOperation
 */
public enum SetOperationType {
	UNION,
	INTERSECT,
	EXCEPT;

	/** Description of the ADQL Feature based on this type. */
	private final LanguageFeature FEATURE;

	private SetOperationType() {
		FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_SETS, this.name(), true, "Perform a " + this + " operation on 2 rows sets.");
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
	 */
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	public String toADQL() {
		return toString();
	}
}