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
 * Copyright 2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import adql.db.DBColumn;
import adql.db.DBIdentifier;
import adql.db.DBTable;
import adql.parser.feature.LanguageFeature;
import adql.query.operand.ADQLColumn;

/**
 * Object representation of the definition of a Common Table Expression (CTE).
 *
 * <p>
 * 	A such table is defined inside the ADQL clause <code>WITH</code>. It must
 * 	be an ADQL query with a name for the resulting temporary table. Labels of
 * 	the resulting columns may be also provided.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (09/2019)
 * @since 2.0
 */
public class WithItem implements ADQLObject {

	/** Description of this ADQL Feature. */
	public final static LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_COMMON_TABLE, "WITH", true, "A Common Table Expression lets create a temporary named result set that can be referred to elsewhere in a main query.");

	/** Name of the resulting table. */
	protected String label;

	/** Flag indicating whether the table name is case sensitive or not. */
	protected boolean caseSensitive = false;

	/** Labels of the resulting columns.
	 * <p><i><b>Note:</b>
	 * 	If NULL or empty, the default output column names must be used.
	 * </i></p> */
	protected List<ADQLColumn> columnLabels = null;

	/** ADQL query providing the CTE's content. */
	protected ADQLQuery query;

	/** Position of this WITH item in the original ADQL query. */
	protected TextPosition position = null;

	/** Database description of the resulting (temporary) table. */
	protected DBTable dbLink = null;

	/**
	 * Create a WITH item with the minimum mandatory information.
	 *
	 * @param label	Name of the resulting table/CTE.
	 * @param query	ADQL query returning the content of this CTE.
	 */
	public WithItem(final String label, final ADQLQuery query) {
		this(label, query, null);
	}

	/**
	 * Create a WITH item with column labels.
	 *
	 * <p><i><b>Warning:</b>
	 * 	If the given list is NULL or empty, the default output column names will
	 * 	be used. However, if not NULL, the given list should contain as many
	 * 	elements as columns returned by the given query.
	 * </i></p>
	 *
	 * @param label			Name of the resulting table/CTE.
	 * @param query			ADQL query returning the content of this CTE.
	 * @param columnLabels	Labels of the output columns.
	 */
	public WithItem(final String label, final ADQLQuery query, final Collection<ADQLColumn> columnLabels) {
		if (label == null || label.trim().isEmpty())
			throw new NullPointerException("Missing label of the WITH item!");

		if (query == null)
			throw new NullPointerException("Missing query of the WITH item!");

		setLabel(label);
		this.query = query;
		this.columnLabels = (columnLabels == null || columnLabels.isEmpty()) ? null : new ArrayList<>(columnLabels);

	}

	/**
	 * Create a deep copy of the given WITH item.
	 *
	 * @param toCopy	The WITH item to duplicate.
	 */
	public WithItem(final WithItem toCopy) {
		label = toCopy.label;
		query = toCopy.query;
		position = toCopy.position;
		if (columnLabels != null) {
			columnLabels = new ArrayList<>(toCopy.columnLabels.size());
			for(ADQLColumn colLabel : toCopy.columnLabels)
				columnLabels.add(colLabel);
		}
	}

	@Override
	public final String getName() {
		return label;
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Get the name of the resulting table.
	 *
	 * @return	CTE's name.
	 */
	public final String getLabel() {
		return label;
	}

	/**
	 * Set the name of the resulting table.
	 *
	 * <p><i><b>Note:</b>
	 * 	The given name may be delimited (i.e. surrounded by double quotes). If
	 * 	so, it will be considered as case sensitive. Surrounding double quotes
	 * 	will be removed and inner escaped double quotes will be un-escaped.
	 * </i></p>
	 *
	 * @param label	New CTE's name.
	 *
	 * @throws NullPointerException	If the given name is NULL or empty.
	 */
	public final void setLabel(String label) throws NullPointerException {
		String tmp = DBIdentifier.normalize(label);
		if (tmp == null)
			throw new NullPointerException("Missing CTE's label! (CTE = WITH's query)");
		else {
			this.label = tmp;
			this.caseSensitive = DBIdentifier.isDelimited(label);
		}
	}

	/**
	 * Tell whether the resulting table name is case sensitive or not.
	 *
	 * @return	<code>true</code> if the CTE's name is case sensitive,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean isLabelCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Specify whether the resulting table name should be case sensitive or not.
	 *
	 * @param caseSensitive	<code>true</code> to make the CTE's name case
	 *                     	sensitive,
	 *                     	<code>false</code> otherwise.
	 */
	public final void setLabelCaseSensitive(final boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Get the specified labels of the output columns of this CTE.
	 *
	 * @return	CTE's columns labels,
	 *        	or NULL if none is specified.
	 */
	public final List<ADQLColumn> getColumnLabels() {
		return columnLabels;
	}

	/**
	 * Specify the tables of all the output columns.
	 *
	 * @param newColumnLabels	New labels of the CTE's output columns,
	 *                       	or NULL (or an empty list) to use the default
	 *                       	column names instead.
	 */
	public final void setColumnLabels(final Collection<ADQLColumn> newColumnLabels) {
		columnLabels = (newColumnLabels == null || newColumnLabels.isEmpty()) ? null : new ArrayList<>(newColumnLabels);
	}

	/**
	 * Get the query corresponding to this CTE.
	 *
	 * @return	CTE's query.
	 */
	public final ADQLQuery getQuery() {
		return query;
	}

	/**
	 * Set the query returning the content of this CTE.
	 *
	 * @param query	New CTE's query.
	 */
	public final void setQuery(ADQLQuery query) {
		this.query = query;
	}

	/**
	 * Database description of this CTE.
	 *
	 * @return	CTE's metadata.
	 */
	public final DBTable getDBLink() {
		return dbLink;
	}

	/**
	 * Set the database description of this CTE.
	 *
	 * @param dbMeta	The new CTE's metadata.
	 */
	public final void setDBLink(final DBTable dbMeta) {
		this.dbLink = dbMeta;
	}

	@Override
	public final TextPosition getPosition() {
		return position;
	}

	public final void setPosition(final TextPosition newPosition) {
		position = newPosition;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new WithItem(this);
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLIterator() {

			private boolean queryReturned = false;

			@Override
			public ADQLObject next() {
				if (queryReturned)
					throw new NoSuchElementException("Iteration already finished! No more element available.");
				else {
					queryReturned = true;
					return query;
				}
			}

			@Override
			public boolean hasNext() {
				return !queryReturned;
			}

			@Override
			public void replace(final ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (!queryReturned)
					throw new IllegalStateException("No iteration yet started!");
				else if (replacer == null)
					throw new UnsupportedOperationException("Impossible to remove the query from a WithItem object! You have to remove the WithItem from its ClauseWith for that.");
				else if (!(replacer instanceof ADQLQuery))
					throw new UnsupportedOperationException("Impossible to replace an ADQLQuery by a " + replacer.getClass() + "!");
				else
					query = (ADQLQuery)replacer;
			}
		};
	}

	@Override
	public String toADQL() {
		// Serialize the list of output columns:
		StringBuffer bufOutColumns = new StringBuffer();
		if (columnLabels != null && !columnLabels.isEmpty()) {
			for(ADQLColumn col : columnLabels) {
				bufOutColumns.append(bufOutColumns.length() == 0 ? '(' : ',');
				bufOutColumns.append(DBIdentifier.denormalize(col.getColumnName(), col.isCaseSensitive(IdentifierField.COLUMN)));
			}
			bufOutColumns.append(')');
		}
		// And now serialize the whole WithItem:
		return DBIdentifier.denormalize(label, caseSensitive) + bufOutColumns.toString() + " AS (\n" + query.toADQL() + "\n)";
	}

	/**
	 * Get the description of all output columns.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	All resulting columns are returned, even if no column's label is
	 * 	provided.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	List generated on the fly!
	 * </i></p>
	 *
	 * @return	List and description of all output columns.
	 */
	public DBColumn[] getResultingColumns() {
		// Fetch all resulting columns from the query:
		DBColumn[] dbColumns = query.getResultingColumns();

		// Force the writing of the column names:
		boolean caseSensitive;
		String newColLabel;
		for(int i = 0; i < dbColumns.length; i++) {
			// fetch the default column name and case sensitivity:
			caseSensitive = dbColumns[i].isCaseSensitive();
			newColLabel = dbColumns[i].getADQLName();

			// if an explicit label is given, use it instead:
			if (columnLabels != null && i < columnLabels.size()) {
				caseSensitive = columnLabels.get(i).isCaseSensitive(IdentifierField.COLUMN);
				newColLabel = columnLabels.get(i).getColumnName();
			}

			// reformat the column label in function of its case sensitivity:
			if (caseSensitive)
				newColLabel = DBIdentifier.denormalize(newColLabel, true);
			else
				newColLabel = newColLabel.toLowerCase();

			// finally, copy the original column with this new name:
			dbColumns[i] = dbColumns[i].copy(newColLabel, newColLabel, dbColumns[i].getTable());
		}

		return dbColumns;
	}

}
