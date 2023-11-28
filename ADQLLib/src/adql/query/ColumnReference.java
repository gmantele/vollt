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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.db.DBColumn;
import adql.parser.feature.LanguageFeature;
import adql.query.from.ADQLTable;
import adql.query.operand.ADQLColumn;

/**
 * Represents a reference to a selected column by an index.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (08/2019)
 *
 * @see ADQLOrder
 */
public class ColumnReference implements ADQLObject {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, "COLUMN_REF", false, "Reference to an item of the SELECT clause (i.e. ref. to an output column).");

	/** Position in the original ADQL query string. */
	private TextPosition position = null;

	/** Index of a selected column. */
	private int columnIndex;

	/** The corresponding column in the "database".
	 * <p><i>By default, this field is automatically filled by
	 * {@link adql.db.DBChecker}.</i></p> */
	private DBColumn dbLink = null;

	/** The {@link ADQLTable} which is supposed to contain this column.
	 * <p><i>By default, this field is automatically filled by
	 * {@link adql.db.DBChecker}.</i></p> */
	private ADQLTable adqlTable = null;

	/** Indicates whether the column name/alias is case sensitive. */
	private boolean caseSensitive = false;

	/**
	 * Builds a column reference with an index of a selected column.
	 *
	 * @param index		Index of a selected column (from 1).
	 *
	 * @throws ArrayIndexOutOfBoundsException	If the given index is less or
	 *                                       	equal 0.
	 */
	public ColumnReference(int index) throws ArrayIndexOutOfBoundsException {
		if (index <= 0)
			throw new IndexOutOfBoundsException("Impossible to make a reference to the " + index + "th column: a column index must be greater or equal 1 !");

		columnIndex = index;
	}

	/**
	 * Builds a column reference by copying the given one.
	 *
	 * @param toCopy	The column reference to copy.
	 */
	public ColumnReference(ColumnReference toCopy) {
		caseSensitive = toCopy.caseSensitive;
		columnIndex = toCopy.columnIndex;
	}

	@Override
	public LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Gets the position in the original ADQL query string.
	 *
	 * @return	The position of this {@link ColumnReference}.
	 */
	@Override
	public final TextPosition getPosition() {
		return position;
	}

	/**
	 * Sets the position at which this {@link ColumnReference} has been found in
	 * the original ADQL query string.
	 *
	 * @param pos	Position of this {@link ColumnReference}.
	 */
	public void setPosition(final TextPosition pos) {
		position = pos;
	}

	/**
	 * Gets the index of the referenced column.
	 *
	 * @return	The index of the referenced column.
	 */
	public final int getColumnIndex() {
		return columnIndex;
	}

	/**
	 * Sets the index of the referenced column.
	 *
	 * @param index	The index of the referenced column (must be > 0).
	 *
	 * @return	<code>true</code> if the column referenced has been updated,
	 *        	<code>false</code> otherwise (if index &lt;= 0).
	 */
	public final boolean setColumnIndex(int index) {
		if (index > 0) {
			columnIndex = index;
			return true;
		}
		return false;
	}

	/**
	 * Tells whether the column reference on a column name/alias is case
	 * sensitive.
	 *
	 * @return	<code>true</code> if the column name/alias is case sensitive,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Sets the case sensitivity on the column name/alias.
	 *
	 * @param sensitive	<code>true</code> to make case sensitive the column
	 *                 	name/alias,
	 *                 	<code>false</code> otherwise.
	 */
	public final void setCaseSensitive(boolean sensitive) {
		caseSensitive = sensitive;
	}

	/**
	 * Gets the corresponding {@link DBColumn}.
	 *
	 * @return	The corresponding {@link DBColumn} if {@link #getColumnName()}
	 *        	is a column name (not an alias),
	 *        	or NULL otherwise.
	 */
	public final DBColumn getDBLink() {
		return dbLink;
	}

	/**
	 * Sets the {@link DBColumn} corresponding to this {@link ADQLColumn}.
	 *
	 * <p><i>
	 * 	By default, this field is automatically filled by
	 * 	{@link adql.db.DBChecker}.
	 * </i></p>
	 *
	 * @param dbLink	Its corresponding {@link DBColumn} if
	 *              	{@link #getColumnName()} is a column name (not an alias),
	 *              	or NULL otherwise.
	 */
	public final void setDBLink(DBColumn dbLink) {
		this.dbLink = dbLink;
	}

	/**
	 * Gets the {@link ADQLTable} from which this column is supposed to come.
	 *
	 * @return 	Its source table if {@link #getColumnName()} is a column name
	 *        	(not an alias),
	 *        	or NULL otherwise.
	 *
	 * @deprecated	Since v2.0. This function is never used.
	 */
	@Deprecated
	public final ADQLTable getAdqlTable() {
		return adqlTable;
	}

	/**
	 * Sets the {@link ADQLTable} from which this column is supposed to come.
	 *
	 * <p><i>
	 * 	By default, this field is automatically filled by
	 * 	{@link adql.db.DBChecker} when
	 * 	{@link adql.db.DBChecker#check(adql.query.ADQLQuery)} is called.
	 * </i></p>
	 *
	 * @param adqlTable	Its source table if {@link #getColumnName()} is a column
	 *                 	name (not an alias),
	 *                 	or NULL otherwise.
	 *
	 * @deprecated	Since v2.0. This piece of information is never used.
	 */
	@Deprecated
	public final void setAdqlTable(ADQLTable adqlTable) {
		this.adqlTable = adqlTable;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ColumnReference(this);
	}

	@Override
	public String getName() {
		return columnIndex + "";
	}

	@Override
	public final ADQLIterator adqlIterator() {
		return new NullADQLIterator();
	}

	@Override
	public String toADQL() {
		return "" + columnIndex;
	}

}
