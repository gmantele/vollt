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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.awt.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBTable;

/**
 * Represent a column as described by the IVOA standard in the TAP protocol
 * definition.
 *
 * <p>
 * 	This object representation has exactly the same fields as the column of the
 * 	table TAP_SCHEMA.columns. But it also provides a way to add other data. For
 * 	instance, if information not listed in the standard may be stored here, they
 * 	can be using the function {@link #setOtherData(Object)}. This object can be
 * 	a single value (integer, string, ...), but also a {@link Map}, {@link List},
 * 	etc...
 * </p>
 *
 * <i>
 * <p><b>Important note:</b>
 * 	A {@link TAPColumn} object MUST always have a DB name. That's why,
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
 * <h3>Set a table</h3>
 *
 * <p>
 *	By default a column is detached (not part of a table). To specify the table
 *	in which this column is, you must use {@link TAPTable#addColumn(TAPColumn)}.
 *	By doing this, the table link inside this column will be set automatically
 *	and you will be able to get the table with {@link #getTable()}.
 * </p>
 *
 * <h3>Foreign keys</h3>
 *
 * <p>
 * 	In case this column is linked to one or several of other tables, it will be
 * 	possible to list all foreign keys where the target columns is with
 * 	{@link #getTargets()}. In the same way, it will be possible to list all
 * 	foreign keys in which this column is a target with {@link #getSources()}.
 * 	However, in order to ensure the consistency between all metadata, these
 * 	foreign key's links are set at the table level by the table itself using
 * 	{@link #addSource(TAPForeignKey)} and {@link #addTarget(TAPForeignKey)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (09/2019)
 */
public class TAPColumn implements DBColumn {

	/** ADQL name of this column. */
	private final String adqlName;

	/** Indicate whether the ADQL column name is case sensitive. In such case,
	 * this name will be put between double quotes in ADQL.
	 * @since 2.4 */
	private boolean columnCaseSensitive = false;

	/** Name that this column have in the database.
	 * <p><i><b>Note:</b>
	 * 	It CAN NOT be NULL. By default, it is the ADQL name.
	 * </i></p> */
	private String dbName = null;

	/** Table which owns this column.
	 * <p><i><b>Note:</b>
	 * 	It should be NULL only at the construction or for a quick representation
	 * 	of a column. Then, this attribute is automatically set by a
	 * 	{@link TAPTable} when adding this column inside it with
	 * 	{@link TAPTable#addColumn(TAPColumn)}.
	 * </i></p> */
	private DBTable table = null;

	/** Description of this column.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; MAY be NULL.
	 * </i></p> */
	private String description = null;

	/** Unit of this column's values.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; MAY be NULL.
	 * </i></p> */
	private String unit = null;

	/** UCD describing the scientific content of this column.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; MAY be NULL.
	 * </i></p> */
	private String ucd = null;

	/** UType associating this column with a data-model.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; MAY be NULL.
	 * </i></p> */
	private String utype = null;

	/** Type of this column.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; CAN'T be NULL.
	 * </i></p> */
	private DBType datatype = new DBType(DBDatatype.UNKNOWN);

	/** Flag indicating whether this column is one of those that should be
	 * returned by default.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; FALSE by default.
	 * </i></p> */
	private boolean principal = false;

	/** Flag indicating whether this column is indexed in the database.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; FALSE by default.
	 * </i></p> */
	private boolean indexed = false;

	/** Flag indicating whether this column can be set to NULL in the database.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; FALSE by default.
	 * </i></p>
	 * @since 2.0 */
	private boolean nullable = false;

	/** Flag indicating whether this column is defined by a standard.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field ; FALSE by default.
	 * </i></p> */
	private boolean std = false;

	/** Ordering index of this column inside its table.
	 * <p><i><b>Note:</b>
	 * 	Standard TAP column field since TAP 1.1.
	 * </i></p>
	 * @since 2.1 */
	private int index = -1;

	/** Coordinate system used by this column values.
	 * <p><i><b>Note:</b>
	 * 	Of course, this attribute has to be set only on coordinate columns.
	 * </i></p>
	 * @since 2.1 */
	private TAPCoosys coosys = null;

	/** Let add some information in addition of the ones of the TAP protocol.
	 * <p><i><b>Note:</b>
	 * 	This object can be anything: an {@link Integer}, a {@link String}, a
	 * 	{@link Map}, a {@link List}, ... Its content is totally free and never
	 * 	used or checked.
	 * </i></p> */
	protected Object otherData = null;

	/** List all foreign keys in which this column is a source.
	 * <p><i><b>CAUTION:</b>
	 * 	For consistency consideration, this attribute SHOULD never be modified!
	 * 	It is set by the constructor and filled ONLY by the table.
	 * </i></p> */
	protected final ArrayList<TAPForeignKey> lstTargets;

	/** List all foreign keys in which this column is a target.
	 * <p><i><b>CAUTION:</b>
	 * 	For consistency consideration, this attribute SHOULD never be modified!
	 * 	It is set by the constructor and filled ONLY by the table.
	 * </i></p> */
	protected final ArrayList<TAPForeignKey> lstSources;

	/**
	 * Build a VARCHAR {@link TAPColumn} instance with the given ADQL name.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	The datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPColumn(String columnName) throws NullPointerException {
		if (columnName == null)
			throw new NullPointerException("Missing column name!");

		columnName = columnName.trim();
		columnCaseSensitive = DefaultDBTable.isDelimited(columnName);
		adqlName = (columnCaseSensitive ? columnName.substring(1, columnName.length() - 1).replaceAll("\"\"", "\"") : columnName);

		if (adqlName.trim().length() == 0)
			throw new NullPointerException("Missing column name!");

		dbName = null;

		lstTargets = new ArrayList<TAPForeignKey>(1);
		lstSources = new ArrayList<TAPForeignKey>(1);
	}

	/**
	 * Build a {@link TAPColumn} instance with the given ADQL name and datatype.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 * @param type			Datatype of this column.
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 *
	 * @see #setDatatype(DBType)
	 */
	public TAPColumn(String columnName, DBType type) throws NullPointerException {
		this(columnName);
		setDatatype(type);
	}

	/**
	 * Build a VARCHAR {@link TAPColumn} instance with the given ADQL name and
	 * description.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 * @param description	Description of the column's content.
	 *                   	<i>May be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPColumn(String columnName, String description) throws NullPointerException {
		this(columnName, (DBType)null, description);
	}

	/**
	 * Build a {@link TAPColumn} instance with the given ADQL name, datatype and
	 * description.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 * @param type			Datatype of this column.
	 * @param description	Description of the column's content.
	 *                   	<i>May be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPColumn(String columnName, DBType type, String description) throws NullPointerException {
		this(columnName, type);
		this.description = description;
	}

	/**
	 * Build a VARCHAR {@link TAPColumn} instance with the given ADQL name,
	 * description and unit.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 * @param description	Description of the column's content.
	 *                   	<i>May be NULL</i>
	 * @param unit			Unit of the column's values.
	 *            			<i>May be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPColumn(String columnName, String description, String unit) throws NullPointerException {
		this(columnName, null, description, unit);
	}

	/**
	 * Build a {@link TAPColumn} instance with the given ADQL name, type, description and unit.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 * @param type			Datatype of this column.
	 * @param description	Description of the column's content.
	 *                   	<i>May be NULL</i>
	 * @param unit			Unit of the column's values.
	 *            			<i>May be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPColumn(String columnName, DBType type, String description, String unit) throws NullPointerException {
		this(columnName, type, description);
		this.unit = unit;
	}

	/**
	 * Build a VARCHAR {@link TAPColumn} instance with the given fields.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 * @param description	Description of the column's content.
	 *                   	<i>May be NULL</i>
	 * @param unit			Unit of the column's values.
	 *            			<i>May be NULL</i>
	 * @param ucd			UCD describing the scientific content of this column.
	 *            			<i>May be NULL</i>
	 * @param utype			UType associating this column with a data-model.
	 *            			<i>May be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPColumn(String columnName, String description, String unit, String ucd, String utype) throws NullPointerException {
		this(columnName, null, description, unit, ucd, utype);
	}

	/**
	 * Build a {@link TAPColumn} instance with the given fields.
	 *
	 * <p><i><b>Note 1:</b>
	 * 	The DB name is set by default to NULL so that {@link #getDBName()}
	 * 	returns exactly what {@link #getADQLName()} returns. To set a specific
	 * 	DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 *
	 * <p><i><b>Note 2:</b>
	 * 	If omitted, the datatype is set by default to VARCHAR.
	 * </i></p>
	 *
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		The column name MUST NOT be prefixed by its table name.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the column name. In such case, they
	 * 		indicate that the column name must be considered as case sensitive.
	 * 		If present, these double quotes will be removed but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 *
	 * @param columnName	ADQL name of this column.
	 * @param type			Datatype of this column.
	 * @param description	Description of the column's content.
	 *                   	<i>May be NULL</i>
	 * @param unit			Unit of the column's values.
	 *                   	<i>May be NULL</i>
	 * @param ucd			UCD describing the scientific content of this column.
	 *                   	<i>May be NULL</i>
	 * @param utype			UType associating this column with a data-model.
	 *                   	<i>May be NULL</i>
	 *
	 * @throws NullPointerException	If the given name is NULL or an empty string.
	 */
	public TAPColumn(String columnName, DBType type, String description, String unit, String ucd, String utype) throws NullPointerException {
		this(columnName, type, description, unit);
		this.ucd = ucd;
		this.utype = utype;
	}

	/**
	 * Get the ADQL name (the name this column MUST have in ADQL queries).
	 *
	 * @return	Its ADQL name.
	 * @see #getADQLName()
	 * @deprecated	Does not do anything special: just call {@link #getADQLName()}.
	 */
	@Deprecated
	public final String getName() {
		return getADQLName();
	}

	@Override
	public final String getADQLName() {
		return adqlName;
	}

	/**
	 * Get the ADQL name of this column, as it has been provided at
	 * initialization.
	 *
	 * @return	Get the original ADQL name.
	 *
	 * @since 2.1
	 */
	public final String getRawName() {
		return (columnCaseSensitive ? "\"" + adqlName.replaceAll("\"", "\"\"") + "\"" : adqlName);
	}

	@Override
	public final boolean isCaseSensitive() {
		return columnCaseSensitive;
	}

	@Override
	public final String getDBName() {
		return (dbName == null) ? getADQLName() : dbName;
	}

	/**
	 * Change the name that this column MUST have in the database (i.e. in SQL
	 * queries).
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given value is NULL or an empty string, nothing is done ; the DB
	 * 	name keeps is former value.
	 * </i></p>
	 *
	 * @param name	The new database name of this column.
	 */
	public final void setDBName(String name) {
		name = (name != null) ? name.trim() : name;
		if (name != null && name.length() > 0)
			dbName = name;
		else
			dbName = null;
	}

	@Override
	public final DBTable getTable() {
		return table;
	}

	/**
	 * Set the table in which this column is.
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPTable} that owns this column.
	 * </i></p>
	 *
	 * <p><i><b>Important note:</b>
	 * 	If this column was already linked with another {@link TAPTable} object,
	 * 	the previous link is removed here, but also in the table (by calling
	 * 	{@link TAPTable#removeColumn(String)}).
	 * </i></p>
	 *
	 * @param table	The table that owns this column.
	 */
	protected final void setTable(final DBTable table) {
		if (this.table != null && this.table instanceof TAPTable && (table == null || !table.equals(this.table)))
			((TAPTable)this.table).removeColumn(adqlName);
		this.table = table;
	}

	/**
	 * Get the description of this column.
	 *
	 * @return	Its description. <i>MAY be NULL</i>
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Set the description of this column.
	 *
	 * @param description	Its new description. <i>MAY be NULL</i>
	 */
	public final void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the unit of the column's values.
	 *
	 * @return	Its unit. <i>MAY be NULL</i>
	 */
	public final String getUnit() {
		return unit;
	}

	/**
	 * Set the unit of the column's values.
	 *
	 * @param unit	Its new unit. <i>MAY be NULL</i>
	 */
	public final void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * Get the UCD describing the scientific content of this column.
	 *
	 * @return	Its UCD. <i>MAY be NULL</i>
	 */
	public final String getUcd() {
		return ucd;
	}

	/**
	 * Set the UCD describing the scientific content of this column.
	 *
	 * @param ucd	Its new UCD. <i>MAY be NULL</i>
	 */
	public final void setUcd(String ucd) {
		this.ucd = ucd;
	}

	/**
	 * Get the UType associating this column with a data-model.
	 *
	 * @return	Its UType. <i>MAY be NULL</i>
	 */
	public final String getUtype() {
		return utype;
	}

	/**
	 * Set the UType associating this column with a data-model.
	 *
	 * @param utype	Its new UType. <i>MAY be NULL</i>
	 */
	public final void setUtype(String utype) {
		this.utype = utype;
	}

	/**
	 * Get the type of the column's values.
	 *
	 * @return	Its datatype. <i>CAN'T be NULL</i>
	 */
	@Override
	public final DBType getDatatype() {
		return datatype;
	}

	/**
	 * Set the type of the column's values.
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given type is NULL, an {@link DBDatatype#UNKNOWN UNKNOWN} type
	 * 	will be set instead.
	 * </i></p>
	 *
	 * @param type	Its new datatype.
	 */
	public final void setDatatype(final DBType type) {
		if (type != null)
			datatype = type;
		else
			datatype = new DBType(DBDatatype.UNKNOWN);
	}

	/**
	 * Tell whether this column is one of those returned by default.
	 *
	 * @return	<code>true</code> if this column should be returned by default,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean isPrincipal() {
		return principal;
	}

	/**
	 * Set whether this column should be one of those returned by default.
	 *
	 * @param principal	<code>true</code> if this column should be returned by
	 *                 	default,
	 *                 	<code>false</code> otherwise.
	 */
	public final void setPrincipal(boolean principal) {
		this.principal = principal;
	}

	/**
	 * Tell whether this column is indexed.
	 *
	 * @return	<code>true</code> if this column is indexed,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean isIndexed() {
		return indexed;
	}

	/**
	 * Set whether this column is indexed or not.
	 *
	 * @param indexed	<code>true</code> if this column is indexed,
	 *               	<code>false</code> otherwise.
	 */
	public final void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}

	/**
	 * Tell whether this column is nullable.
	 *
	 * @return	<code>true</code> if this column is nullable,
	 *        	<code>false</code> otherwise.
	 *
	 * @since 2.0
	 */
	public final boolean isNullable() {
		return nullable;
	}

	/**
	 * Set whether this column is nullable or not.
	 *
	 * @param nullable	<code>true</code> if this column is nullable,
	 *                	<code>false</code> otherwise.
	 *
	 * @since 2.0
	 */
	public final void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	/**
	 * Tell whether this column is defined by a standard.
	 *
	 * @return	<code>true</code> if this column is defined by a standard,
	 *        	<code>false</code> otherwise.
	 */
	public final boolean isStd() {
		return std;
	}

	/**
	 * Set whether this column is defined by a standard.
	 *
	 * @param std	<code>true</code> if this column is defined by a standard,
	 *            	<code>false</code> otherwise.
	 */
	public final void setStd(boolean std) {
		this.std = std;
	}

	/**
	 * Get the ordering index of this column inside its table.
	 *
	 * @return	Its ordering index.
	 *
	 * @since 2.1
	 */
	public final int getIndex() {
		return index;
	}

	/**
	 * Set the ordering index of this column inside its table.
	 *
	 * @param columnIndex	Its new ordering index.
	 *
	 * @since 2.1
	 */
	public final void setIndex(int columnIndex) {
		this.index = columnIndex;
	}

	/**
	 * Get the used coordinate system.
	 *
	 * @return	Its coordinate system.
	 *
	 * @since 2.1
	 */
	public final TAPCoosys getCoosys() {
		return coosys;
	}

	/**
	 * Set the the coordinate system to use.
	 *
	 * @param newCoosys	Its new coordinate system.
	 *
	 * @since 2.1
	 */
	public final void setCoosys(final TAPCoosys newCoosys) {
		this.coosys = newCoosys;
	}

	/**
	 * Get the other (piece of) information associated with this column.
	 *
	 * <p><i><b>Note:</b>
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
	 * Set the other (piece of) information associated with this column.
	 *
	 * @param data	Another information about this column. <i>MAY be NULL</i>
	 */
	public void setOtherData(Object data) {
		otherData = data;
	}

	/**
	 * Let add a foreign key in which this column is a source (= which is
	 * targeting another column).
	 *
	 * <p><i><b>Note:</b>
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPTable} that owns this column or that is part of the foreign
	 * 	key.
	 * </i></p>
	 *
	 * @param key	A foreign key.
	 */
	protected void addTarget(TAPForeignKey key) {
		if (key != null)
			lstTargets.add(key);
	}

	/**
	 * Get the number of times this column is targeting another column.
	 *
	 * @return	How many this column is source in a foreign key.
	 */
	public int getNbTargets() {
		return lstTargets.size();
	}

	/**
	 * Get the list of foreign keys in which this column is a source
	 * (= is targeting another column).
	 *
	 * @return	List of foreign keys in which this column is a source.
	 */
	public Iterator<TAPForeignKey> getTargets() {
		return lstTargets.iterator();
	}

	/**
	 * Remove the fact that this column is a source (= is targeting another
	 * column) in the given foreign key.
	 *
	 * <p><i><b>Note:</b>
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPTable} that owns this column or that is part of the foreign
	 * 	key.
	 * </i></p>
	 *
	 * @param key	Foreign key in which this column was targeting another
	 *           	column.
	 */
	protected void removeTarget(TAPForeignKey key) {
		if (key != null)
			lstTargets.remove(key);
	}

	/**
	 * Remove the fact that this column is a source (= is targeting another
	 * column) in any foreign key in which it was.
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPTable} that owns this column or that is part of the foreign
	 * 	key.
	 * </i></p>
	 */
	protected void removeAllTargets() {
		lstTargets.clear();
	}

	/**
	 * Let add a foreign key in which this column is a target (= which is
	 * targeted by another column).
	 *
	 * <p><i><b>Note:</b>
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPTable} that owns this column or that is part of the foreign
	 * 	key.
	 * </i></p>
	 *
	 * @param key	A foreign key.
	 */
	protected void addSource(TAPForeignKey key) {
		if (key != null)
			lstSources.add(key);
	}

	/**
	 * Get the number of times this column is targeted by another column.
	 *
	 * @return	How many this column is target in a foreign key.
	 */
	public int getNbSources() {
		return lstSources.size();
	}

	/**
	 * Get the list of foreign keys in which this column is a target (= is
	 * targeted another column).
	 *
	 * @return	List of foreign keys in which this column is a target.
	 */
	public Iterator<TAPForeignKey> getSources() {
		return lstSources.iterator();
	}

	/**
	 * Remove the fact that this column is a target (= is targeted by another
	 * column) in the given foreign key.
	 *
	 * <p><i><b>Note:</b>
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPTable} that owns this column or that is part of the foreign
	 * 	key.
	 * </i></p>
	 *
	 * @param key	Foreign key in which this column was targeted by another
	 *           	column.
	 */
	protected void removeSource(TAPForeignKey key) {
		lstSources.remove(key);
	}

	/**
	 * Remove the fact that this column is a target (= is targeted by
	 * another column) in any foreign key in which it was.
	 *
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the
	 * 	{@link TAPTable} that owns this column or that is part of the foreign
	 * 	key.
	 * </i></p>
	 */
	protected void removeAllSources() {
		lstSources.clear();
	}

	/**
	 * <p><i><b>Warning:</b>
	 * 	Since the type of the other data is not known, the copy of its value
	 * 	can not be done properly. So, this column and its copy will share the
	 * 	same other data object. If it is also needed to make a deep copy of this
	 * 	other data object, this function MUST be overridden.
	 * </i></b>
	 *
	 * @see adql.db.DBColumn#copy(java.lang.String, java.lang.String, adql.db.DBTable)
	 */
	@Override
	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable) {
		TAPColumn copy = new TAPColumn((adqlName == null) ? this.adqlName : adqlName, datatype, description, unit, ucd, utype);
		copy.columnCaseSensitive = this.columnCaseSensitive;
		copy.setDBName((dbName == null) ? this.getDBName() : dbName);
		copy.setTable(dbTable);

		copy.setIndexed(indexed);
		copy.setPrincipal(principal);
		copy.setStd(std);
		copy.setOtherData(otherData);

		return copy;
	}

	/**
	 * Provide a deep copy (included the other data) of this column.
	 *
	 * <p><i><b>Warning:</b>
	 * 	Since the type of the other data is not known, the copy of its value
	 * 	can not be done properly. So, this column and its copy will share the
	 * 	same other data object. If it is also needed to make a deep copy of this
	 * 	other data object, this function MUST be overridden.
	 * </i></b>
	 *
	 * @return	The deep copy of this column.
	 */
	public DBColumn copy() {
		TAPColumn copy = new TAPColumn(adqlName, datatype, description, unit, ucd, utype);
		copy.columnCaseSensitive = this.columnCaseSensitive;
		copy.setDBName(dbName);
		copy.setTable(table);
		copy.setIndexed(indexed);
		copy.setPrincipal(principal);
		copy.setStd(std);
		copy.setOtherData(otherData);
		return copy;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TAPColumn))
			return false;

		TAPColumn col = (TAPColumn)obj;
		return col.getTable().equals(table) && col.getADQLName().equals(adqlName) && col.columnCaseSensitive == this.columnCaseSensitive;
	}

	@Override
	public String toString() {
		return (table != null ? table.toString() : "") + getRawName();
	}

}
