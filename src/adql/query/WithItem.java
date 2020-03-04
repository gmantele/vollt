package adql.query;

import java.util.NoSuchElementException;

import adql.db.DBColumn;
import adql.db.DBIdentifier;
import adql.db.DBTable;
import adql.parser.feature.LanguageFeature;

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

	/** ADQL query providing the CTE's content. */
	protected ADQLQuery query;

	/** Position of this WITH item in the original ADQL query. */
	protected TextPosition position = null;

	/** Database description of the resulting (temporary) table. */
	protected DBTable dbLink = null;

	/**
	 * Create a WITH item.
	 *
	 * @param label	Name of the resulting table/CTE.
	 * @param query	ADQL query returning the content of this CTE.
	 */
	public WithItem(final String label, final ADQLQuery query) {
		if (label == null || label.trim().isEmpty())
			throw new NullPointerException("Missing label of the WITH item!");

		if (query == null)
			throw new NullPointerException("Missing query of the WITH item!");

		setLabel(label);
		this.query = query;

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
		return DBIdentifier.denormalize(label, caseSensitive) + " AS (\n" + query.toADQL() + "\n)";
	}

	/**
	 * Get the description of all output columns.
	 *
	 * @return	List and description of all output columns.
	 */
	public DBColumn[] getResultingColumns() {
		return query.getResultingColumns();
	}

}
