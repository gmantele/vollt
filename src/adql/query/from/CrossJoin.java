package adql.query.from;

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

import java.util.Collection;

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.ClauseConstraints;
import adql.query.operand.ADQLColumn;

/**
 * Cross join.
 *
 * <p>
 * 	In ADQL <code>T1, T2</code> is equivalent to <code>T1, T2</code> or to
 * 	<code>T1 CROSS JOIN T2</code> in SQL.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (07/2019)
 */
public class CrossJoin extends ADQLJoin {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, "CROSS_JOIN", false, "A cross join between two \"tables\".");

	/**
	 * Builds a CROSS join between the two given "tables".
	 *
	 * @param left	Left "table".
	 * @param right	Right "table".
	 *
	 * @see ADQLJoin#ADQLJoin(FromContent, FromContent)
	 */
	public CrossJoin(FromContent left, FromContent right) {
		super(left, right);
	}

	/**
	 * Builds a copy of the given CROSS join.
	 *
	 * @param toCopy	The CROSS join to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 *
	 * @see ADQLJoin#ADQLJoin(ADQLJoin)
	 */
	public CrossJoin(CrossJoin toCopy) throws Exception {
		super(toCopy);
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public String getJoinType() {
		return "CROSS JOIN";
	}

	/**
	 * Effect-less method: a CROSS join can not be NATURAL.
	 *
	 * @see adql.query.from.ADQLJoin#setNatural(boolean)
	 */
	@Override
	public void setNatural(boolean natural) {
		;
	}

	/**
	 * Effect-less method: no join condition can be specified to make a CROSS
	 * join.
	 *
	 * @see adql.query.from.ADQLJoin#setJoinCondition(adql.query.ClauseConstraints)
	 */
	@Override
	public void setJoinCondition(ClauseConstraints cond) {
		;
	}

	/**
	 * Effect-less method: no columns can be joined in a CROSS join.
	 *
	 * @see adql.query.from.ADQLJoin#setJoinedColumns(java.util.Collection)
	 */
	@Override
	public void setJoinedColumns(Collection<ADQLColumn> columns) {
		;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new CrossJoin(this);
	}

}
