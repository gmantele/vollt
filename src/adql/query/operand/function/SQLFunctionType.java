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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.feature.LanguageFeature;

/**
 * All the types of SQL functions (COUNT, SUM, AVG, etc...).
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (07/2019)
 *
 * @see SQLFunction
 */
public enum SQLFunctionType {
	COUNT_ALL, COUNT, AVG, MAX, MIN, SUM;

	/** Description of the ADQL Feature based on this type.
	 * @since 2.0 */
	private final LanguageFeature FEATURE;

	/** @since 2.0 */
	private SQLFunctionType() {
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
}
