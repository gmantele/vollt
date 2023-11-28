package adql.query;

import adql.parser.feature.LanguageFeature;

/**
 * Object representation of an OFFSET clause.
 *
 * <p>
 * 	This clause is special hence the fact it does not extend
 * 	{@link adql.query.ClauseADQL}. It contains only one value: the number of
 * 	rows removed from the query's result head.
 * </p>
 *
 * <p><i><b>Important note:</b>
 * 	The OFFSET value stored in this object MUST always be positive.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (08/2019)
 * @since 2.0
 */
public class ClauseOffset implements ADQLObject {

	/** Description of this ADQL Feature. */
	public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_OFFSET, "OFFSET", true, "Remove the specified number of rows from the head of the query result.");

	/** Name of this ADQL object. */
	private static final String NAME = "OFFSET";

	/** Value of the query's OFFSET.
	 * <p><i><b>Important note:</b>
	 * 	This value can never be negative.
	 * </i></p> */
	protected int value;

	/** Position of this {@link ClauseOffset} in the original ADQL query
	 * string. */
	private TextPosition position = null;

	/**
	 * Create a clause OFFSET with the given offset value.
	 *
	 * @param offsetValue	Value of the query's result OFFSET.
	 *
	 * @throws IndexOutOfBoundsException	If the given value is negative.
	 */
	public ClauseOffset(final int offsetValue) throws IndexOutOfBoundsException {
		setValue(offsetValue);
	}

	/**
	 * Get the query's OFFSET.
	 *
	 * @return	Query's OFFSET. <i>Always positive.</i>
	 */
	public final int getValue() {
		return value;
	}

	/**
	 * Set the query's OFFSET.
	 *
	 * @param offsetValue	Value of the query's result OFFSET.
	 *
	 * @throws IndexOutOfBoundsException	If the given value is negative.
	 */
	public void setValue(final int offsetValue) throws IndexOutOfBoundsException {
		if (offsetValue < 0)
			throw new IndexOutOfBoundsException("Incorrect OFFSET value: \"" + offsetValue + "\"! It must be a positive value.");
		this.value = offsetValue;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ClauseOffset(value);
	}

	@Override
	public final String getName() {
		return NAME;
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	@Override
	public final TextPosition getPosition() {
		return position;
	}

	/**
	 * Sets the position at which this {@link ClauseOffset} has been found in
	 * the original ADQL query string.
	 *
	 * @param position	Position of this {@link ClauseOffset}.
	 */
	public final void setPosition(final TextPosition newPosition) {
		position = newPosition;
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new NullADQLIterator();
	}

	@Override
	public String toADQL() {
		return "OFFSET " + value;
	}

}
