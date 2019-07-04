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
 * Copyright 2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.query.ADQLObject;

/**
 * Handler searching for all {@link ADQLObject}s declared as optional in the
 * ADQL language.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (07/2019)
 * @since 2.0
 *
 * @see ADQLObject#getFeatureDescription()
 * @see adql.parser.feature.LanguageFeature#optional
 */
public class SearchOptionalFeaturesHandler extends SimpleSearchHandler {

	public SearchOptionalFeaturesHandler() {
		super();
	}

	public SearchOptionalFeaturesHandler(final boolean recursive) {
		super(recursive);
	}

	public SearchOptionalFeaturesHandler(final boolean recursive, final boolean onlyFirstMatch) {
		super(recursive, onlyFirstMatch);
	}

	@Override
	protected boolean match(final ADQLObject obj) {
		return obj.getFeatureDescription().optional;
	}

}
