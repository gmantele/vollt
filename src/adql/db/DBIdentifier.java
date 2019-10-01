package adql.db;

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
 * Copyright 2019- UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * Generic implementation of any kind of ADQL/DB identifier.
 *
 * <p>
 * 	It already implements functions getting and setting the ADQL and DB names
 * 	of the interfaces {@link DBTable} and {@link DBColumn}. Thus, it guarantees
 * 	that all DB... identifiers will behave the same way when manipulating their
 * 	ADQL and DB names.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (09/2019)
 * @since 2.0
 *
 * @see DBTable
 * @see DBColumn
 */
public abstract class DBIdentifier {

	/** Regular expression of a delimited identifier (i.e. an identifier between
	 * double quotes ; an inner double quote is escaped by doubling it). */
	private final static String REGEXP_DELIMITED = "\"(\"\"|[^\"])*\"";

	/** Name (not delimited, not prefixed) to use in ADQL queries.
	 * <p><i><b>Important:</b> It must never be NULL.</i></p> */
	protected String adqlName = null;

	/** A flag indicating if the ADQL name is case sensitive or not (i.e. if it
	 * must be delimited or not in an ADQL query).  */
	protected boolean adqlCaseSensitive = false;

	/** Name (not delimited, not prefixed) of this identifier in the "database".
	 * This name must be used, for example, while translating an ADQL query into
	 * SQL.
	 * <p><i><b>Note:</b> It may be NULL. In such case, {@link #getDBName()}
	 * must return {@link #adqlName}.</i></p> */
	protected String dbName = null;

	/**
	 * Create an identifier with the given ADQL name.
	 *
	 * <p>
	 * 	In this constructor, the DB name is not set. Thus, {@link #getDBName()}
	 * 	will return the same as {@link #getADQLName()}.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given name is delimited, the surrounding double quotes will be
	 * 	removed and {@link #isCaseSensitive()} will return <code>true</code>.
	 * </i></p>
	 *
	 * @param adqlName	The ADQL and DB name of this identifier.
	 *                	<i>It may be delimited and/or qualified.</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or empty.
	 *
	 * @see #setADQLName(String)
	 */
	protected DBIdentifier(final String adqlName) throws NullPointerException {
		setADQLName(adqlName);
	}

	/**
	 * Create an identifier with the given ADQL and DB names.
	 *
	 * <p>
	 * 	In this constructor, the DB name is not set. Thus, {@link #getDBName()}
	 * 	will return the same as {@link #getADQLName()}.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given name is delimited, the surrounding double quotes will be
	 * 	removed and {@link #isCaseSensitive()} will return <code>true</code>.
	 * </i></p>
	 *
	 * @param adqlName	The ADQL and DB name of this identifier.
	 *                	<i>It may be delimited and/or qualified.</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or empty.
	 *
	 * @see #setADQLName(String)
	 * @see #setDBName(String)
	 */
	protected DBIdentifier(final String adqlName, final String dbName) throws NullPointerException {
		setADQLName(adqlName);
		setDBName(dbName);
	}

	/**
	 * Get the ADQL version of this identifier.
	 *
	 * <p>
	 * 	This name is neither delimited, nor prefixed.
	 * 	To determine whether it should be delimited in an ADQL query, use
	 * 	{@link #isCaseSensitive()}.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The returned string is never empty or NULL.
	 * </i></p>
	 *
	 * @return	The name to use in ADQL queries.
	 */
	public String getADQLName() {
		return adqlName;
	}

	/**
	 * Set the ADQL version of this identifier.
	 *
	 * <p>
	 * 	If the given name is delimited, the surrounding double quotes will be
	 * 	removed and case sensitivity will be set to <code>true</code>
	 * 	(i.e. {@link #isCaseSensitive()} will return <code>true</code>).
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The given name must not be prefixed.
	 * </i></p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	If the given name is NULL or empty (even after removal of surrounding
	 * 	double quotes, if delimited), this function will immediately throw an
	 * 	exception.
	 * </i></p>
	 *
	 * @param newName	New ADQL version of this identifier.
	 *
	 * @throws NullPointerException	If the given name is NULL or empty.
	 *
	 * @see #isDelimited(String)
	 * @see #normalize(String)
	 */
	public void setADQLName(final String newName) throws NullPointerException {
		boolean adqlCS = isDelimited(newName);
		String normName = normalize(newName);

		if (normName == null)
			throw new NullPointerException("Missing ADQL name!");

		this.adqlName = normName;
		this.adqlCaseSensitive = adqlCS;
	}

	/**
	 * Tell whether the ADQL version of this identifier is case sensitive or
	 * not.
	 *
	 * <p>
	 * 	If case sensitive, the ADQL name must be written between double quotes
	 * 	(and all inner double quotes should be doubled).
	 * </p>
	 *
	 * @return	<code>true</code> if case sensitive,
	 *        	<code>false</code> otherwise.
	 */
	public boolean isCaseSensitive() {
		return adqlCaseSensitive;
	}

	/**
	 * Set the case sensitivity of the ADQL version of this identifier.
	 *
	 * <p>
	 * 	Setting the case sensitivity to <code>true</code> will force the
	 * 	delimited form of the ADQL name (i.e. it will be written between
	 * 	double quotes).
	 * </p>
	 *
	 * @param caseSensitive	<code>true</code> to declare the ADQL name as case
	 *                     	sensitive,
	 *                     	<code>false</code> otherwise.
	 */
	public void setCaseSensitive(final boolean caseSensitive) {
		this.adqlCaseSensitive = caseSensitive;
	}

	/**
	 * Get the database version of this identifier.
	 *
	 * <p>This name is neither delimited, nor prefixed.</p>
	 *
	 * <p>In an SQL query, this name should be considered as case sensitive.</p>
	 *
	 * <p><i><b>Note:</b>
	 * 	The returned string is never empty or NULL.
	 * </i></p>
	 *
	 * @return	The real name of this identifier in the "database".
	 */
	public String getDBName() {
		return (dbName == null) ? getADQLName() : dbName;
	}

	/**
	 * Set the database version of this identifier.
	 *
	 * <p>
	 * 	If the given name is delimited, the surrounding double quotes will be
	 * 	removed.
	 * </p>
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The given name should not be prefixed.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If the given name is NULL or empty (even after removal of surrounding
	 * 	double quotes if delimited), {@link #getDBName()} will return the same
	 * 	as {@link #getADQLName()}.
	 * </i></p>
	 *
	 * @param newName	The real name of this identifier in the "database".
	 *
	 * @see #normalize(String)
	 */
	public void setDBName(final String newName) {
		dbName = normalize(newName);
	}

	/**
	 * Tell whether the given identifier is delimited (i.e. within the same pair
	 * of double quotes - <code>"</code>).
	 *
	 * <i>
	 * <p>The following identifiers ARE delimited:</p>
	 * <ul>
	 * 	<li><code>"a"</code></li>
	 * 	<li><code>""</code> (empty string ; but won't be considered as a
	 * 	                     valid ADQL name)</li>
	 * 	<li><code>" "</code> (string with spaces ; but won't be considered as a
	 * 	                      valid ADQL name)</li>
	 * 	<li><code>"foo.bar"</code></li>
	 * 	<li><code>"foo"".""bar"</code> (with escaped double quotes)</li>
	 * 	<li><code>""""</code> (idem)</li>
	 * </ul>
	 * </i>
	 *
	 * <i>
	 * <p>The following identifiers are NOT considered as delimited:</p>
	 * <ul>
	 * 	<li><code>"foo</code> (missing ending double quote)</li>
	 * 	<li><code>foo"</code> (missing leading double quote)</li>
	 * 	<li><code>"foo"."bar"</code> (not the same pair of double quotes)</li>
	 * </ul>
	 * </i>
	 *
	 * @param ident	Identifier that may be delimited.
	 *
	 * @return	<code>true</code> if the given identifier is delimited,
	 *        	<code>false</code> otherwise.
	 */
	public static boolean isDelimited(final String ident) {
		return ident != null && ident.trim().matches(REGEXP_DELIMITED);
	}

	/**
	 * Normalize the given identifier.
	 *
	 * <p>This function performs the following operations:</p>
	 * <ol>
	 * 	<li>Remove leading and trailing space characters.</li>
	 * 	<li>If the resulting string is empty, return NULL.</li>
	 * 	<li>If {@link #isDelimited(String) delimited}, remove the leading and
	 * 		trailing double quotes.</li>
	 * 	<li>If the resulting string without leading and trailing spaces is
	 * 		empty, return NULL.</li>
	 * 	<li>Return the resulting string.</li>
	 * </ol>
	 *
	 * @param ident	The identifier to normalize.
	 *
	 * @return	The normalized string,
	 *        	or NULL if NULL or empty.
	 *
	 * @see #denormalize(String, boolean)
	 */
	public static String normalize(final String ident) {
		// Return NULL if empty:
		if (ident == null || ident.trim().length() == 0)
			return null;

		// Remove leading and trailing space characters:
		String normIdent = ident.trim();

		// If delimited, remove the leading and trailing ":
		if (isDelimited(normIdent)) {
			normIdent = normIdent.substring(1, normIdent.length() - 1).replaceAll("\"\"", "\"");
			return (normIdent.trim().length() == 0) ? null : normIdent;
		} else
			return normIdent;
	}

	/**
	 * De-normalize the given string.
	 *
	 * <p>
	 * 	This function does something only if the given string is declared as
	 * 	case sensitive. In such case, it will surround it by double quotes.
	 * 	All inner double quotes will be escaped by doubling them.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given string is NULL, it will be returned as such (i.e. NULL).
	 * </i></p>
	 *
	 * @param ident			The identifier to de-normalize.
	 * @param caseSensitive	<code>true</code> if the given identifier is
	 *                     	considered as case sensitive,
	 *                     	<code>false</code> otherwise.
	 *
	 * @return	The de-normalized identifier.
	 *
	 * @see #normalize(String)
	 */
	public static String denormalize(final String ident, final boolean caseSensitive) {
		if (caseSensitive && ident != null)
			return "\"" + ident.replaceAll("\"", "\"\"") + "\"";
		else
			return ident;
	}

}
