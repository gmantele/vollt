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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import adql.db.DBColumn;
import adql.db.DBIdentifier;
import adql.db.DBTable;
import adql.db.DBType;
import tap.TAPException;

/**
 * Represent a table as described by the IVOA standard in the TAP protocol
 * definition.
 *
 * <p>
 * 	This object representation has exactly the same fields as the column of the
 * 	table TAP_SCHEMA.tables. But it also provides a way to add other data. For
 * 	instance, if information not listed in the standard may be stored here, they
 * 	can be using the function {@link #setOtherData(Object)}. This object can be
 * 	a single value (integer, string, ...), but also a {@link Map}, {@link List},
 * 	etc...
 * </p>
 *
 * <i>
 * <p><b>Important note:</b>
 * 	A {@link TAPTable} object MUST always have a DB name. That's why,
 * 	{@link #getDBName()} returns what {@link #getADQLName()} returns when no DB
 * 	name is set. After creation, it is possible to set the DB name with
 * 	{@link #setDBName(String)}.
 * </p>
 * <p>
 * 	This DB name MUST be UNqualified and without double quotes. If a NULL or
 * 	empty value is provided, {@link #getDBName()} returns what
 * 	{@link #getADQLName()} returns.
 * </p>
 * </i>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (09/2019)
 */
public class TAPTable extends DBIdentifier implements DBTable {

	/**
	 * Different types of table according to the TAP protocol.
	 * The default one should be "table".
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 2.0 (08/2014)
	 *
	 * @since 2.0
	 */
	public enum TableType {
		output, table, view;
	}

	/** Name of this table as provided at creation.
	 * <p><i>This name may be qualified and/or delimited.</i></p>
	 * @since 2.4 */
	private String rawName;

	/** The schema which owns this table.
	 * <p><i><b>Note:</b>
	 * 	It is NULL only at the construction. Then, this attribute is
	 * 	automatically set by a {@link TAPSchema} when adding this table inside
	 * 	it with {@link TAPSchema#addTable(TAPTable)}.
	 * </i></p> */
	private TAPSchema schema = null;

	/** Type of this table.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP table field ; CAN NOT be NULL ; by default, it is "table".
	 * </i></p> */
	private TableType type = TableType.table;

	/** Descriptive, human-interpretable name of the table.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP table field ; MAY be NULL.
	 * </i></p>
	 * @since 2.0 */
	private String title = null;

	/** Description of this table.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP table field ; MAY be NULL.
	 * </i></p> */
	private String description = null;

	/** UType associating this table with a data-model.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP table field ; MAY be NULL.
	 * </i></p> */
	private String utype = null;

	/** Ordering index of this table inside its schema.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP table field since TAP 1.1.
	 * </i></p>
	 * @since 2.1 */
	private int index = -1;

	/** List of columns composing this table.
	 * <p><i><b>Note:</b>
	 * 	All columns of this list are linked to this table from the moment they
	 * 	are added inside it.
	 * </i></p> */
	protected final Map<String, TAPColumn> columns;

	/** List of all foreign keys linking this table to others. */
	protected final ArrayList<TAPForeignKey> foreignKeys;

	/** Let add some information in addition of the ones of the TAP protocol.
	 * <p><i><b>Note:</b>
	 * 	This object can be anything: an {@link Integer}, a {@link String}, a
	 * 	{@link Map}, a {@link List}, ... Its content is totally free and never
	 * 	used or checked.
	 * </i></p> */
	protected Object otherData = null;

	/**
	 * Build a {@link TAPTable} instance with the given ADQL name.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	The table type is set by default to "table".
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The schema prefix will not be removed until
	 * 		{@link #setSchema(TAPSchema)}.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the table name. In such case, the ADQL
	 * 		name of this table will be considered as case sensitive and these
	 * 		double quotes will be automatically removed.
	 * 		<em>Note that this case sensitivity may be not identified just after
	 * 		this constructor ; you may have to specify the schema
	 * 		(see {@link #setSchema(TAPSchema)}) so that the schema prefix is
	 * 		removed first.</em>
	 *	</li>
	 * </ul>
	 *
	 * @param tableName		ADQL name of this table.
	 *
	 * @throws NullPointerException	If the given name is NULL or empty.
	 */
	public TAPTable(final String tableName) throws NullPointerException {
		super(tableName);

		rawName = tableName.trim();

		columns = new LinkedHashMap<String, TAPColumn>();
		foreignKeys = new ArrayList<TAPForeignKey>();
	}

	/**
	 * Build a {@link TAPTable} instance with the given ADQL name and table
	 * type.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the table type is set by default to "table".
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The schema prefix will not be removed until
	 * 		{@link #setSchema(TAPSchema)}.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the table name. In such case, the ADQL
	 * 		name of this table will be considered as case sensitive and these
	 * 		double quotes will be automatically removed.
	 * 		<em>Note that this case sensitivity may not be identified just after
	 * 		this constructor ; you may have to specify the schema
	 * 		(see {@link #setSchema(TAPSchema)}) so that the schema prefix is
	 * 		removed first.</em>
	 *	</li>
	 * </ul>
	 *
	 * @param tableName		ADQN name of this table.
	 * @param tableType		Type of this table.
	 *                 		<i>If NULL, "table" will be set by default.</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 *
	 * @see #setType(TableType)
	 */
	public TAPTable(final String tableName, final TableType tableType) throws NullPointerException {
		this(tableName);
		setType(tableType);
	}

	/**
	 * Build a {@link TAPTable} instance with the given ADQL name, table type,
	 * description and UType.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the table type is set by default to "table".
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The schema prefix will not be removed until
	 * 		{@link #setSchema(TAPSchema)}.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the table name. In such case, the ADQL
	 * 		name of this table will be considered as case sensitive and these
	 * 		double quotes will be automatically removed.
	 * 		<em>Note that this case sensitivity may not be identified just after
	 * 		this constructor ; you may have to specify the schema
	 * 		(see {@link #setSchema(TAPSchema)}) so that the schema prefix is
	 * 		removed first.</em>
	 *	</li>
	 * </ul>
	 *
	 * @param tableName		ADQL name of this table.
	 * @param tableType		Type of this table.
	 *                 		<i>If NULL, "table" will be set by default.</i>
	 * @param description	Description of this table.
	 *                   	<i>MAY be NULL.</i>
	 * @param utype			UType associating this table with a data-model.
	 *             			<i>MAY be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPTable(final String tableName, final TableType tableType, final String description, final String utype) throws NullPointerException {
		this(tableName, tableType);
		this.description = description;
		this.utype = utype;
	}

	/**
	 * Get the qualified and delimited (if case sensitive) name of this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	If this table is not attached to a schema, this function will just
	 * 	return the ADQL name of this table.
	 * </i></p>
	 *
	 * @return	Qualified and delimited (if needed) ADQL name of this table.
	 */
	public final String getFullName() {
		return (schema != null ? schema.getADQLName() + "." : "") + denormalize(getADQLName(), isCaseSensitive());
	}

	/**
	 * Get the ADQL name (the name this table MUST have in ADQL queries).
	 *
	 * @return	Its ADQL name.
	 *
	 * @see #getADQLName()
	 *
	 * @deprecated	Does not do anything special: just call {@link #getADQLName()}.
	 */
	@Deprecated
	public final String getName() {
		return getADQLName();
	}

	/**
	 * Get the full ADQL name of this table, as it has been provided at
	 * initialization.
	 *
	 * @return	Get the original ADQL name.
	 *
	 * @since 2.1
	 */
	public final String getRawName() {
		return rawName;
	}

	/**
	 * Simplify the original ADQL name and set {@link #adqlName} with the
	 * simplified version.
	 *
	 * <p>
	 * 	The simplification consists in removing the schema prefix (if any)
	 * 	and to detect case sensitivity (i.e. surrounded by double quotes). In
	 * 	this last case, the detected double quotes are automatically removed.
	 * </p>
	 *
	 * @since 2.4
	 */
	@Override
	public void setADQLName(final String name) throws NullPointerException {
		/* Start by setting the new ADQL name (ignoring prefix if any
		 * + detection of NULL and empty string): */
		super.setADQLName(name);

		// Memorize the new raw name:
		rawName = name.trim();

		// If a schema is specified, remove the schema prefix (if any):
		if (schema != null) {
			String tmp = name;

			// strict comparison if schema is case sensitive:
			if (schema.isCaseSensitive()) {
				if (tmp.startsWith(schema.getRawName() + "."))
					tmp = tmp.substring(schema.getRawName().length() + 1).trim();
			}

			// if no case sensitivity...
			else {
				// ...search not-case-sensitively for a prefix:
				if (tmp.toLowerCase().startsWith(schema.getADQLName().toLowerCase() + "."))
					tmp = tmp.substring(schema.getADQLName().length() + 1).trim();
				// ...otherwise, try with a strict comparison (as if schema was case sensitive):
				else if (tmp.toLowerCase().startsWith(denormalize(schema.getADQLName().toLowerCase(), true) + "."))
					tmp = tmp.substring(denormalize(schema.getADQLName(), true).length() + 1).trim();
			}

			// Finally, re-update the ADQL name (with prefix removed):
			super.setADQLName(tmp);
		}
	}

	@Override
	public String getADQLCatalogName() {
		return null;
	}

	@Override
	public String getDBCatalogName() {
		return null;
	}

	@Override
	public final String getADQLSchemaName() {
		return schema == null ? null : schema.getADQLName();
	}

	@Override
	public final String getDBSchemaName() {
		return schema == null ? null : schema.getDBName();
	}

	/**
	 * Get the schema that owns this table.
	 *
	 * @return Its schema. <i>MAY be NULL</i>
	 */
	public final TAPSchema getSchema() {
		return schema;
	}

	/**
	 * Set the schema in which this schema is.
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPSchema} that owns this table.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	If this table was already linked with another {@link TAPSchema} object,
	 * 	the previous link is removed here, but also in the schema (by calling
	 * 	{@link TAPSchema#removeTable(String)}).
	 * </i></p>
	 *
	 * @param schema	The schema that owns this table.
	 */
	protected final void setSchema(final TAPSchema schema) {
		// Update the former TAPSchema, if any:
		if (this.schema != null && (schema == null || !schema.equals(this.schema)))
			this.schema.removeTable(adqlName);

		// Set the new schema:
		this.schema = schema;

		/* Update the ADQL name of this table:
		 * (i.e. whether or not schema prefix should be removed) */
		setADQLName(rawName);
	}

	/**
	 * Get the type of this table.
	 *
	 * @return	Its type.
	 */
	public final TableType getType() {
		return type;
	}

	/**
	 * <p>Set the type of this table.</p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given type is NULL, nothing will be done ; the type of this table
	 * 	won't be changed.
	 * </i></p>
	 *
	 * @param type	Its new type.
	 */
	public final void setType(TableType type) {
		if (type != null)
			this.type = type;
	}

	/**
	 * Get the title of this table.
	 *
	 * @return	Its title. <i>MAY be NULL</i>
	 *
	 * @since 2.0
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Set the title of this table.
	 *
	 * @param title	Its new title. <i>MAY be NULL</i>
	 *
	 * @since 2.0
	 */
	public final void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Get the description of this table.
	 *
	 * @return	Its description. <i>MAY be NULL</i>
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Set the description of this table.
	 *
	 * @param description	Its new description. <i>MAY be NULL</i>
	 */
	public final void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the UType associating this table with a data-model.
	 *
	 * @return	Its UType. <i>MAY be NULL</i>
	 */
	public final String getUtype() {
		return utype;
	}

	/**
	 * Set the UType associating this table with a data-model.
	 *
	 * @param utype	Its new UType. <i>MAY be NULL</i>
	 */
	public final void setUtype(String utype) {
		this.utype = utype;
	}

	/**
	 * Get the ordering index of this table inside its schema.
	 *
	 * @return	Its ordering index.
	 *
	 * @since 2.1
	 */
	public final int getIndex() {
		return index;
	}

	/**
	 * Set the ordering index of this table inside its schema.
	 *
	 * @param tableIndex	Its new ordering index.
	 *
	 * @since 2.1
	 */
	public final void setIndex(int tableIndex) {
		this.index = tableIndex;
	}

	/**
	 * Get the other (piece of) information associated with this table.
	 *
	 * <p><i>Note:
	 * 	By default, NULL is returned, but it may be any kind of value
	 * 	({@link Integer}, {@link String}, {@link Map}, {@link List}, ...).
	 * </i></p>
	 *
	 * @return	The other (piece of) information. <i>MAY be NULL</i>
	 */
	public Object getOtherData() {
		return otherData;
	}

	/**
	 * Set the other (piece of) information associated with this table.
	 *
	 * @param data	Another information about this table. <i>MAY be NULL</i>
	 */
	public void setOtherData(Object data) {
		otherData = data;
	}

	/**
	 * Add a column to this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given column is NULL, nothing will be done.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	By adding the given column inside this table, it
	 * 	will be linked with this table using {@link TAPColumn#setTable(DBTable)}.
	 * 	In this function, if the column was already linked with another
	 * 	{@link TAPTable}, the former link is removed using
	 * 	{@link TAPTable#removeColumn(String)}.
	 * </i></p>
	 *
	 * @param newColumn	Column to add inside this table.
	 */
	public final void addColumn(final TAPColumn newColumn) {
		if (newColumn != null && newColumn.getADQLName() != null) {
			newColumn.setTable(this);
			columns.put(newColumn.getADQLName(), newColumn);
		}
	}

	/**
	 * Build a {@link TAPColumn} object whose the ADQL and DB name will the
	 * given one. Then, add this column inside this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	The built {@link TAPColumn} object is returned, so that being modified
	 * 	afterwards if needed.
	 * </i></p>
	 *
	 * @param columnName	ADQL name (and indirectly also the DB name) of the
	 *                  	column to create and add.
	 *
	 * @return	The created and added {@link TAPColumn} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 *
	 * @see TAPColumn#TAPColumn(String)
	 * @see #addColumn(TAPColumn)
	 */
	public final TAPColumn addColumn(String columnName) {
		if (columnName == null || columnName.trim().length() <= 0)
			return null;

		TAPColumn c = new TAPColumn(columnName);
		addColumn(c);
		return c;
	}

	/**
	 * Build a {@link TAPColumn} object whose the ADQL and DB name will the given one.
	 * Then, add this column inside this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	The built {@link TAPColumn} object is returned, so that being modified
	 * 	afterwards if needed.
	 * </i></p>
	 *
	 * @param columnName	ADQL name (and indirectly also the DB name) of the
	 *                  	column to create and add.
	 * @param datatype		Type of the new column's values. <i>If NULL, VARCHAR
	 *                		will be the type of the created column.</i>
	 * @param description	Description of the new column. <i>MAY be NULL</i>
	 * @param unit			Unit of the new column's values. <i>MAY be NULL</i>
	 * @param ucd			UCD describing the scientific content of the new
	 *           			column. <i>MAY be NULL</i>
	 * @param utype			UType associating the new column with a data-model.
	 *             			<i>MAY be NULL</i>
	 *
	 * @return	The created and added {@link TAPColumn} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 *
	 * @see TAPColumn#TAPColumn(String, DBType, String, String, String, String)
	 * @see #addColumn(TAPColumn)
	 */
	public TAPColumn addColumn(String columnName, DBType datatype, String description, String unit, String ucd, String utype) {
		if (columnName == null || columnName.trim().length() <= 0)
			return null;

		TAPColumn c = new TAPColumn(columnName, datatype, description, unit, ucd, utype);
		addColumn(c);
		return c;
	}

	/**
	 * Build a {@link TAPColumn} object whose the ADQL and DB name will the
	 * given one. Then, add this column inside this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	The built {@link TAPColumn} object is returned, so that being modified
	 * 	afterwards if needed.
	 * </i></p>
	 *
	 * @param columnName	ADQL name (and indirectly also the DB name) of the
	 *                  	column to create and add.
	 * @param datatype		Type of the new column's values. <i>If NULL, VARCHAR
	 *                		will be the type of the created column.</i>
	 * @param description	Description of the new column. <i>MAY be NULL</i>
	 * @param unit			Unit of the new column's values. <i>MAY be NULL</i>
	 * @param ucd			UCD describing the scientific content of the new
	 *           			column. <i>MAY be NULL</i>
	 * @param utype			UType associating the new column with a data-model.
	 *             			<i>MAY be NULL</i>
	 * @param principal		<code>true</code> if the new column should be
	 *                 		returned by default, <code>false</code> otherwise.
	 * @param indexed		<code>true</code> if the new column is indexed,
	 *               		<code>false</code> otherwise.
	 * @param std			<code>true</code> if the new column is defined by a
	 *           			standard, <code>false</code> otherwise.
	 *
	 * @return	The created and added {@link TAPColumn} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 *
	 * @see TAPColumn#TAPColumn(String, DBType, String, String, String, String)
	 * @see TAPColumn#setPrincipal(boolean)
	 * @see TAPColumn#setIndexed(boolean)
	 * @see TAPColumn#setStd(boolean)
	 * @see #addColumn(TAPColumn)
	 */
	public TAPColumn addColumn(String columnName, DBType datatype, String description, String unit, String ucd, String utype, boolean principal, boolean indexed, boolean std) {
		if (columnName == null || columnName.trim().length() <= 0)
			return null;

		TAPColumn c = new TAPColumn(columnName, datatype, description, unit, ucd, utype);
		c.setPrincipal(principal);
		c.setIndexed(indexed);
		c.setStd(std);
		addColumn(c);
		return c;
	}

	/**
	 * Tell whether this table contains a column with the given ADQL name.
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive.
	 * </i></p>
	 *
	 * @param columnName	ADQL name (case sensitive) of the column whose the
	 *                  	existence must be checked.
	 *
	 * @return	<code>true</code> if a column having the given ADQL name exists
	 *        	in this table,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean hasColumn(String columnName) {
		if (columnName == null)
			return false;
		else
			return columns.containsKey(columnName);
	}

	/**
	 * Get the list of all columns contained in this table.
	 *
	 * @return	An iterator over the list of this table's columns.
	 */
	public Iterator<TAPColumn> getColumns() {
		return columns.values().iterator();
	}

	@Override
	public DBColumn getColumn(String colName, boolean byAdqlName) {
		if (byAdqlName)
			return getColumn(colName);
		else {
			if (colName != null && colName.length() > 0) {
				Collection<TAPColumn> collColumns = columns.values();
				for(TAPColumn column : collColumns) {
					if (column.getDBName().equals(colName))
						return column;
				}
			}
			return null;
		}
	}

	/**
	 * Search a column inside this table having the given ADQL name.
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive.
	 * </i></p>
	 *
	 * @param columnName	ADQL name of the column to search.
	 *
	 * @return	The matching column,
	 *        	or NULL if no column with this ADQL name has been found.
	 */
	public final TAPColumn getColumn(String columnName) {
		if (columnName == null)
			return null;
		else
			return columns.get(columnName);
	}

	/**
	 * Tell whether this table contains a column with the given ADQL or DB name.
	 *
	 * <p><i><b>Note:</b>
	 * 	This functions is just calling {@link #getColumn(String, boolean)} and
	 * 	compare its result with NULL in order to check the existence of the
	 * 	specified column.
	 * </i></p>
	 *
	 * @param colName		ADQL or DB name that the column to search must have.
	 * @param byAdqlName	<code>true</code> to search the column by ADQL name,
	 *                  	<code>false</code> to search by DB name.
	 *
	 * @return	<code>true</code> if a column has been found inside this table
	 *        	with the given ADQL or DB name,
	 *        	<code>false</code> otherwise.
	 *
	 * @see #getColumn(String, boolean)
	 */
	public boolean hasColumn(String colName, boolean byAdqlName) {
		return (getColumn(colName, byAdqlName) != null);
	}

	/**
	 * Get the number of columns composing this table.
	 *
	 * @return	Number of its columns.
	 */
	public final int getNbColumns() {
		return columns.size();
	}

	/**
	 * Tell whether this table contains no column.
	 *
	 * @return	<code>true</code> if this table is empty (no column),
	 *        	<code>false</code> if it contains at least one column.
	 */
	public final boolean isEmpty() {
		return columns.isEmpty();
	}

	/**
	 * Remove the specified column.
	 *
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If some foreign keys were associating the column to remove,
	 * 	they will be also deleted.
	 * </i></p>
	 *
	 * @param columnName	ADQL name of the column to remove.
	 *
	 * @return	The removed column,
	 *        	or NULL if no column with the given ADQL name has been found.
	 *
	 * @see #deleteColumnRelations(TAPColumn)
	 */
	public final TAPColumn removeColumn(String columnName) {
		if (columnName == null)
			return null;

		TAPColumn removedColumn = columns.remove(columnName);
		if (removedColumn != null)
			deleteColumnRelations(removedColumn);

		return removedColumn;
	}

	/**
	 * Delete all foreign keys having the given column in the sources or the
	 * targets list.
	 *
	 * @param col	A column.
	 */
	protected final void deleteColumnRelations(TAPColumn col) {
		// Remove the relation between the column and this table:
		col.setTable(null);

		// Remove the relations between the column and other tables/columns:
		Iterator<TAPForeignKey> it = col.getTargets();
		while(it.hasNext())
			removeForeignKey(it.next());

		it = col.getSources();
		while(it.hasNext()) {
			TAPForeignKey key = it.next();
			key.getFromTable().removeForeignKey(key);
		}
	}

	/**
	 * Remove all columns composing this table.
	 * Foreign keys will also be deleted.
	 */
	public final void removeAllColumns() {
		Iterator<Map.Entry<String, TAPColumn>> it = columns.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, TAPColumn> entry = it.next();
			it.remove();
			deleteColumnRelations(entry.getValue());
		}
	}

	/**
	 * Add the given foreign key to this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	This function will do nothing if the given foreign key is NULL.
	 * </i></p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	The source table ({@link TAPForeignKey#getFromTable()}) of the given
	 * 	foreign key MUST be this table and the foreign key MUST be completely
	 * 	defined. If not, an exception will be thrown and the key won't be added.
	 * </i></p>
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given foreign key is added to this table, all the columns of this
	 * 	key will be linked to the foreign key using either
	 * 	{@link TAPColumn#addSource(TAPForeignKey)} or
	 * 	{@link TAPColumn#addTarget(TAPForeignKey)}.
	 * </i></p>
	 *
	 * @param key	Foreign key (whose the FROM table is this table) to add
	 *           	inside this table.
	 *
	 * @throws TAPException	If the source table of the given foreign key is not
	 *                     	this table or if the given key is not completely
	 *                     	defined.
	 */
	public final void addForeignKey(TAPForeignKey key) throws TAPException {
		if (key == null)
			return;

		String keyId = key.getKeyId();
		final String errorMsgPrefix = "Impossible to add the foreign key \"" + keyId + "\" because ";

		if (key.getFromTable() == null)
			throw new TAPException(errorMsgPrefix + "no source table is specified!");

		if (!this.equals(key.getFromTable()))
			throw new TAPException(errorMsgPrefix + "the source table is not \"" + getADQLName() + "\"");

		if (key.getTargetTable() == null)
			throw new TAPException(errorMsgPrefix + "no target table is specified!");

		if (key.isEmpty())
			throw new TAPException(errorMsgPrefix + "it defines no relation!");

		if (foreignKeys.add(key)) {
			try {
				TAPTable targetTable = key.getTargetTable();
				for(Map.Entry<String, String> relation : key) {
					if (!hasColumn(relation.getKey()))
						throw new TAPException(errorMsgPrefix + "the source column \"" + relation.getKey() + "\" doesn't exist in \"" + getADQLName() + "\"!");
					else if (!targetTable.hasColumn(relation.getValue()))
						throw new TAPException(errorMsgPrefix + "the target column \"" + relation.getValue() + "\" doesn't exist in \"" + targetTable.getADQLName() + "\"!");
					else {
						getColumn(relation.getKey()).addTarget(key);
						targetTable.getColumn(relation.getValue()).addSource(key);
					}
				}
			} catch(TAPException ex) {
				foreignKeys.remove(key);
				throw ex;
			}
		}
	}

	/**
	 * Build a foreign key using the ID, the target table and the given list of
	 * columns. Then, add the created foreign key to this table.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The source table of the created foreign key
	 * 	({@link TAPForeignKey#getFromTable()}) will be this table.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If the given foreign key is added to this table, all the columns of this
	 * 	key will be linked to the foreign key using either
	 * 	{@link TAPColumn#addSource(TAPForeignKey)} or
	 * 	{@link TAPColumn#addTarget(TAPForeignKey)}.
	 * </i></p>
	 *
	 * @return	The created and added foreign key.
	 *
	 * @throws TAPException	If the specified key is not completely or correctly
	 *                     	defined.
	 *
	 * @see TAPForeignKey#TAPForeignKey(String, TAPTable, TAPTable, Map)
	 */
	public TAPForeignKey addForeignKey(String keyId, TAPTable targetTable, Map<String, String> columns) throws TAPException {
		TAPForeignKey key = new TAPForeignKey(keyId, this, targetTable, columns);
		addForeignKey(key);
		return key;
	}

	/**
	 * Build a foreign key using the ID, the target table, the given list of
	 * columns, the given description and the given UType. Then, add the created
	 * foreign key to this table.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The source table of the created foreign key
	 * 	({@link TAPForeignKey#getFromTable()}) will be this table.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If the given foreign key is added to this table, all the columns of this
	 * 	key will be linked to the foreign key using either
	 * 	{@link TAPColumn#addSource(TAPForeignKey)} or
	 * 	{@link TAPColumn#addTarget(TAPForeignKey)}.
	 * </i></p>
	 *
	 * @return	The created and added foreign key.
	 *
	 * @throws TAPException	If the specified key is not completely or correctly
	 *                     	defined.
	 *
	 * @see TAPForeignKey#TAPForeignKey(String, TAPTable, TAPTable, Map, String, String)
	 */
	public TAPForeignKey addForeignKey(String keyId, TAPTable targetTable, Map<String, String> columns, String description, String utype) throws TAPException {
		TAPForeignKey key = new TAPForeignKey(keyId, this, targetTable, columns, description, utype);
		addForeignKey(key);
		return key;
	}

	/**
	 * Get the list of all foreign keys associated whose the source is this table.
	 *
	 * @return	An iterator over all its foreign keys.
	 */
	public final Iterator<TAPForeignKey> getForeignKeys() {
		return foreignKeys.iterator();
	}

	/**
	 * Get the number of all foreign keys whose the source is this table
	 *
	 * @return	Number of all its foreign keys.
	 */
	public final int getNbForeignKeys() {
		return foreignKeys.size();
	}

	/**
	 * Remove the given foreign key from this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	This function will also delete the link between the columns of the
	 * 	foreign key and the foreign key, using
	 * 	{@link #deleteRelations(TAPForeignKey)}.
	 * </i></p>
	 *
	 * @param keyToRemove	Foreign key to removed from this table.
	 *
	 * @return	<code>true</code> if the key has been successfully removed,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean removeForeignKey(TAPForeignKey keyToRemove) {
		if (foreignKeys.remove(keyToRemove)) {
			deleteRelations(keyToRemove);
			return true;
		} else
			return false;
	}

	/**
	 * Remove all the foreign keys whose the source is this table.
	 *
	 * <p><i><b>Note:</b>
	 * 	This function will also delete the link between the columns of all the
	 * 	removed foreign keys and the foreign keys, using
	 * 	{@link #deleteRelations(TAPForeignKey)}.
	 * </i></p>
	 */
	public final void removeAllForeignKeys() {
		Iterator<TAPForeignKey> it = foreignKeys.iterator();
		while(it.hasNext()) {
			deleteRelations(it.next());
			it.remove();
		}
	}

	/**
	 * Delete the link between all columns of the given foreign key
	 * and this foreign key. Thus, these columns won't be anymore source or
	 * target of this foreign key.
	 *
	 * @param key	A foreign key whose links with its columns must be deleted.
	 */
	protected final void deleteRelations(TAPForeignKey key) {
		for(Map.Entry<String, String> relation : key) {
			TAPColumn col = key.getFromTable().getColumn(relation.getKey());
			if (col != null)
				col.removeTarget(key);

			col = key.getTargetTable().getColumn(relation.getValue());
			if (col != null)
				col.removeSource(key);
		}
	}

	@Override
	public Iterator<DBColumn> iterator() {
		return new Iterator<DBColumn>() {
			private final Iterator<TAPColumn> it = getColumns();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public DBColumn next() {
				return it.next();
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}

	@Override
	public String toString() {
		return ((schema != null) ? (schema.toString() + ".") : "") + denormalize(getADQLName(), isCaseSensitive());
	}

	@Override
	public DBTable copy(final String dbName, final String adqlName) {
		TAPTable copy = new TAPTable((adqlName == null) ? this.rawName : adqlName);
		copy.setDBName((dbName == null) ? this.getDBName() : dbName);
		copy.setSchema(schema);
		Collection<TAPColumn> collColumns = columns.values();
		for(TAPColumn col : collColumns)
			copy.addColumn((TAPColumn)col.copy(col.getDBName(), col.getADQLName(), copy));
		copy.setDescription(description);
		copy.setOtherData(otherData);
		copy.setType(type);
		copy.setUtype(utype);
		return copy;
	}

}
