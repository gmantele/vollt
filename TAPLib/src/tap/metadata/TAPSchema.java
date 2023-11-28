package tap.metadata;

/*
 * This file is part of TAPLibrary.
 *
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.awt.List;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import adql.db.DBIdentifier;
import tap.metadata.TAPTable.TableType;

/**
 * Represent a schema as described by the IVOA standard in the TAP protocol
 * definition.
 *
 * <p>
 * 	This object representation has exactly the same fields as the column of the
 * 	table TAP_SCHEMA.schemas. But it also provides a way to add other data. For
 * 	instance, if information not listed in the standard may be stored here, they
 * 	can be using the function {@link #setOtherData(Object)}. This object can be
 * 	a single value (integer, string, ...), but also a {@link Map}, {@link List},
 * 	etc...
 * </p>
 *
 * <p><i><b>Note:</b>
 * 	On the contrary to {@link TAPColumn} and {@link TAPTable}, a
 * 	{@link TAPSchema} object MAY have no DB name. But by default, at the
 * 	creation the DB name is the simplified ADQL name (i.e. as it is returned by
 * 	{@link #getADQLName()}). Once created, it is possible to set the DB name
 * 	with {@link #setDBName(String)}. This DB name MAY be qualified, BUT MUST BE
 * 	without double quotes.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (09/2019)
 */
public class TAPSchema extends DBIdentifier implements Iterable<TAPTable> {

	/** Descriptive, human-interpretable name of the schema.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP schema field ; MAY be NULL.
	 * </i></p>
	 * @since 2.0 */
	private String title = null;

	/** Description of this schema.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP schema field ; MAY be NULL.
	 * </i></p> */
	private String description = null;

	/** UType describing the scientific content of this schema.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP schema field ; MAY be NULL.
	 * </i></p> */
	private String utype = null;

	/** Ordering index of this schema inside its whole schema set.
	 * <p><i><b>Note:</b>
	 * 	SHOULD be a standard TAP schema field in TAP 1.1, as table_index and
	 * 	column_index are resp. in TAP_SCHEMA.tables and TAP_SCHEMA.columns.
	 * </i></p>
	 * @since 2.1 */
	private int index = -1;

	/** Let add some information in addition of the ones of the TAP protocol.
	 * <p><i><b>Note:</b>
	 * 	This object can be anything: an {@link Integer}, a {@link String}, a
	 * 	{@link Map}, a {@link List}, ... Its content is totally free and never
	 * 	used or checked.
	 * </i></p> */
	protected Object otherData = null;

	/** List all tables contained inside this schema. */
	protected final Map<String, TAPTable> tables;

	/**
	 * Build a {@link TAPSchema} instance with the given ADQL name.
	 *
	 * <p><i><b>Note:</b>
	 * 	The DB name is set by default to the ADQL name (as returned by
	 * 	{@link #getADQLName()}). To set the DB name, you MUST call then
	 * 	{@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		No catalog prefix is supported. <i>For instance:
	 * 		<code>myCatalog.mySchema</code> will be considered as the schema
	 * 		name instead of <code>mySchema</code>.</i>
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the ADQL name. They will be removed by
	 * 		{@link #getADQLName()} but will still appear in the result of
	 * 		{@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param schemaName	ADQL name of this schema.
	 *
	 * @throws NullPointerException	If the given name is NULL or empty.
	 */
	public TAPSchema(String schemaName) throws NullPointerException {
		super(schemaName);

		dbName = getADQLName();

		tables = new LinkedHashMap<String, TAPTable>();
	}

	/**
	 * Build a {@link TAPSchema} instance with the given ADQL name and
	 * description.
	 *
	 * <p><i><b>Note:</b>
	 * 	The DB name is set by default to the ADQL name (as returned by
	 * 	{@link #getADQLName()}). To set the DB name, you MUST call then
	 * 	{@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		No catalog prefix is supported. <i>For instance:
	 * 		<code>myCatalog.mySchema</code> will be considered as the schema
	 * 		name instead of <code>mySchema</code>.</i>
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the ADQL name. They will be removed by
	 * 		{@link #getADQLName()} but will still appear in the result of
	 * 		{@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param schemaName	ADQL name of this schema.
	 * @param description	Description of this schema. <i>MAY be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPSchema(final String schemaName, final String description) throws NullPointerException {
		this(schemaName, description, null);
	}

	/**
	 * Build a {@link TAPSchema} instance with the given ADQL name,
	 * description and UType.
	 *
	 * <p><i><b>Note:</b>
	 * 	The DB name is set by default to the ADQL name (as returned by {@link #getADQLName()}).
	 * 	To set the DB name, you MUST call then {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		No catalog prefix is supported. <i>For instance:
	 * 		<code>myCatalog.mySchema</code> will be considered as the schema
	 * 		name instead of <code>mySchema</code>.</i>
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the ADQL name. They will be removed by
	 * 		{@link #getADQLName()} but will still appear in the result of
	 * 		{@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param schemaName	ADQL name of this schema.
	 * @param description	Description of this schema. <i>MAY be NULL</i>
	 * @param utype			UType associating this schema with a data-model.
	 *             			<i>MAY be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPSchema(final String schemaName, final String description, final String utype) throws NullPointerException {
		this(schemaName);
		this.description = description;
		this.utype = utype;
	}

	/**
	 * Get the ADQL name (the name this schema MUST have in ADQL queries).
	 *
	 * @return	Its ADQL name.
	 * @see #getADQLName()
	 * @deprecated	Does not do anything special: just call {@link #getADQLName()}.
	 */
	@Deprecated
	public final String getName() {
		return getADQLName();
	}

	/**
	 * Get the full ADQL name of this schema, as it has been provided at
	 * initialization (i.e. delimited if {@link #isCaseSensitive() case sensitive}).
	 *
	 * @return	Get the original ADQL name.
	 *
	 * @since 2.1
	 */
	public final String getRawName() {
		return toString();
	}

	/**
	 * Get the name this schema MUST have in the database.
	 *
	 * @return	Its DB name. <i>MAY be NULL</i>
	 */
	@Override
	public final String getDBName() {
		return dbName;
	}

	/**
	 * Set the name this schema MUST have in the database.
	 *
	 * <i>
	 * <p><b>Notes:</b></p>
	 * <ul>
	 * 	<li>The given name may be NULL. In such case {@link #getDBName()} will
	 * 		then return NULL.</li>
	 * 	<li>It may be prefixed by a catalog name.</li>
	 * 	<li>
	 * 		It MUST be NON delimited/double-quoted. Otherwise an SQL error will
	 * 		be raised when querying any item of this schema because the library
	 * 		double-quotes systematically the DB name of schemas, tables and
	 * 		columns.
	 *	</li>
	 * </ul>
	 * </i>
	 *
	 * @param name	Its new DB name. <i>MAY be NULL</i>
	 */
	@Override
	public final void setDBName(String name) {
		name = (name != null) ? name.trim() : name;
		dbName = name;
	}

	/**
	 * Get the title of this schema.
	 *
	 * @return	Its title. <i>MAY be NULL</i>
	 *
	 * @since 2.0
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Set the title of this schema.
	 *
	 * @param title	Its new title. <i>MAY be NULL</i>
	 *
	 * @since 2.0
	 */
	public final void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Get the description of this schema.
	 *
	 * @return	Its description. <i>MAY be NULL</i>
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Set the description of this schema.
	 *
	 * @param description	Its new description. <i>MAY be NULL</i>
	 */
	public final void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the UType associating this schema with a data-model.
	 *
	 * @return	Its UType. <i>MAY be NULL</i>
	 */
	public final String getUtype() {
		return utype;
	}

	/**
	 * Set the UType associating this schema with a data-model.
	 *
	 * @param utype	Its new UType. <i>MAY be NULL</i>
	 */
	public final void setUtype(String utype) {
		this.utype = utype;
	}

	/**
	 * Get the ordering index of this schema inside its whole schema set.
	 *
	 * @return	Its ordering index.
	 *
	 * @since 2.1
	 */
	public final int getIndex() {
		return index;
	}

	/**
	 * Set the ordering index of this schema inside its whole schema set.
	 *
	 * @param schemaIndex	Its new ordering index.
	 *
	 * @since 2.1
	 */
	public final void setIndex(int schemaIndex) {
		this.index = schemaIndex;
	}

	/**
	 * <p>Get the other (piece of) information associated with this schema.</p>
	 *
	 * <p><i>Note:
	 * 	By default, NULL is returned, but it may be any kind of value ({@link Integer},
	 * 	{@link String}, {@link Map}, {@link List}, ...).
	 * </i></p>
	 *
	 * @return	The other (piece of) information. <i>MAY be NULL</i>
	 */
	public Object getOtherData() {
		return otherData;
	}

	/**
	 * Set the other (piece of) information associated with this schema.
	 *
	 * @param data	Another information about this schema. <i>MAY be NULL</i>
	 */
	public void setOtherData(Object data) {
		otherData = data;
	}

	/**
	 * <p>Add the given table inside this schema.</p>
	 *
	 * <p><i>Note:
	 * 	If the given table is NULL, nothing will be done.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	By adding the given table inside this schema, it
	 * 	will be linked with this schema using {@link TAPTable#setSchema(TAPSchema)}.
	 * 	In this function, if the table was already linked with another {@link TAPSchema},
	 * 	the former link is removed using {@link TAPSchema#removeTable(String)}.
	 * </i></p>
	 *
	 * @param newTable	Table to add inside this schema.
	 */
	public final void addTable(TAPTable newTable) {
		if (newTable != null && newTable.getADQLName() != null) {
			newTable.setSchema(this);
			tables.put(newTable.getADQLName(), newTable);
		}
	}

	/**
	 * <p>Build a {@link TAPTable} object whose the ADQL and DB name will the given one.
	 * Then, add this table inside this schema.</p>
	 *
	 * <p><i>Note:
	 * 	The built {@link TAPTable} object is returned, so that being modified afterwards if needed.
	 * </i></p>
	 *
	 * @param tableName	ADQL name (and indirectly also the DB name) of the table to create and add.
	 *
	 * @return	The created and added {@link TAPTable} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 *
	 * @see TAPTable#TAPTable(String)
	 * @see #addTable(TAPTable)
	 */
	public TAPTable addTable(String tableName) {
		if (tableName == null)
			return null;

		TAPTable t = new TAPTable(tableName);
		addTable(t);
		return t;
	}

	/**
	 * <p>Build a {@link TAPTable} object whose the ADQL and DB name will the given one.
	 * Then, add this table inside this schema.</p>
	 *
	 * <p><i>Note:
	 * 	The built {@link TAPTable} object is returned, so that being modified afterwards if needed.
	 * </i></p>
	 *
	 * @param tableName		ADQL name (and indirectly also the DB name) of the table to create and add.
	 * @param tableType		Type of the new table. <i>If NULL, "table" will be the type of the created table.</i>
	 * @param description	Description of the new table. <i>MAY be NULL</i>
	 * @param utype			UType associating the new column with a data-model. <i>MAY be NULL</i>
	 *
	 * @return	The created and added {@link TAPTable} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 *
	 * @see TAPTable#TAPTable(String, TableType, String, String)
	 * @see #addTable(TAPTable)
	 */
	public TAPTable addTable(String tableName, TableType tableType, String description, String utype) {
		if (tableName == null)
			return null;

		TAPTable t = new TAPTable(tableName, tableType, description, utype);
		addTable(t);

		return t;
	}

	/**
	 * <p>Tell whether this schema contains a table having the given ADQL name.</p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 *
	 * @param tableName	Name of the table whose the existence in this schema must be checked.
	 *
	 * @return	<i>true</i> if a table with the given ADQL name exists, <i>false</i> otherwise.
	 */
	public final boolean hasTable(String tableName) {
		if (tableName == null)
			return false;
		else
			return tables.containsKey(tableName);
	}

	/**
	 * <p>Search for a table having the given ADQL name.</p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 *
	 * @param tableName	ADQL name of the table to search.
	 *
	 * @return	The table having the given ADQL name,
	 *        	or NULL if no such table can be found.
	 */
	public final TAPTable getTable(String tableName) {
		if (tableName == null)
			return null;
		else
			return tables.get(tableName);
	}

	/**
	 * Get the number of all tables contained inside this schema.
	 *
	 * @return	Number of its tables.
	 */
	public final int getNbTables() {
		return tables.size();
	}

	/**
	 * Tell whether this schema contains no table.
	 *
	 * @return	<i>true</i> if this schema contains no table,
	 *        	<i>false</i> if it has at least one table.
	 */
	public final boolean isEmpty() {
		return tables.isEmpty();
	}

	/**
	 * <p>Remove the table having the given ADQL name.</p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 *
	 * <p><i>Note:
	 * 	If the specified table is removed, its schema link is also deleted.
	 * </i></p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	If the goal of this function's call is to delete definitely the specified table
	 * 	from the metadata, you SHOULD also call {@link TAPTable#removeAllForeignKeys()}.
	 * 	Indeed, foreign keys of the table would still link the removed table with other tables
	 * 	AND columns of the whole metadata set.
	 * </i></p>
	 *
	 * @param tableName	ADQL name of the table to remove from this schema.
	 *
	 * @return	The removed table,
	 *        	or NULL if no table with the given ADQL name can be found.
	 */
	public final TAPTable removeTable(String tableName) {
		if (tableName == null)
			return null;

		TAPTable removedTable = tables.remove(tableName);
		if (removedTable != null)
			removedTable.setSchema(null);
		return removedTable;
	}

	/**
	 * <p>Remove all the tables contained inside this schema.</p>
	 *
	 * <p><i>Note:
	 * 	When a table is removed, its schema link is also deleted.
	 * </i></p>
	 *
	 * <p><b>CAUTION:
	 * 	If the goal of this function's call is to delete definitely all the tables of this schema
	 * 	from the metadata, you SHOULD also call {@link TAPTable#removeAllForeignKeys()}
	 * 	on all tables before calling this function.
	 * 	Indeed, foreign keys of the tables would still link the removed tables with other tables
	 * 	AND columns of the whole metadata set.
	 * </b></p>
	 */
	public final void removeAllTables() {
		Iterator<Map.Entry<String, TAPTable>> it = tables.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, TAPTable> entry = it.next();
			it.remove();
			entry.getValue().setSchema(null);
		}
	}

	@Override
	public Iterator<TAPTable> iterator() {
		return tables.values().iterator();
	}

	@Override
	public String toString() {
		return denormalize(getADQLName(), isCaseSensitive());
	}

}
