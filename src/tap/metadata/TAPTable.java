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
 * Copyright 2012-2016 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.awt.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DBType;
import tap.TAPException;

/**
 * <p>Represent a table as described by the IVOA standard in the TAP protocol definition.</p>
 * 
 * <p>
 * 	This object representation has exactly the same fields as the column of the table TAP_SCHEMA.tables.
 * 	But it also provides a way to add other data. For instance, if information not listed in the standard
 * 	may be stored here, they can be using the function {@link #setOtherData(Object)}. This object can be
 * 	a single value (integer, string, ...), but also a {@link Map}, {@link List}, etc...
 * </p>
 * 
 * <p><i><b>Important note:</b>
 * 	A {@link TAPTable} object MUST always have a DB name. That's why, {@link #getDBName()} returns
 * 	what {@link #getADQLName()} returns when no DB name is set. After creation, it is possible to set
 * 	the DB name with {@link #setDBName(String)}.
 * 	<br/>
 * 	This DB name MUST be UNqualified and without double quotes. If a NULL or empty value is provided,
 * 	{@link #getDBName()} returns what {@link #getADQLName()} returns.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.1 (07/2016)
 * 
 * TODO:  needs merge (Updated by G.Landais for VizieR)
 *              - error with some quoted names
 */
public class TAPTable implements DBTable {

	/**
	 * Different types of table according to the TAP protocol.
	 * The default one should be "table".
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 2.0 (08/2014)
	 * 
	 * @since 2.0
	 */
	public enum TableType{
		output, table, view;
	}

	/** Name that this table MUST have in ADQL queries. */
	private final String adqlName;

	/** Indicates whether the given ADQL name must be simplified by {@link #getADQLName()}.
	 * <p>Here, "simplification" means removing the surrounding double quotes and the schema prefix if any.</p>
	 * @since 2.1 */
	private final boolean simplificationNeeded;

	/** <p>Indicate whether the ADQL name has been given at creation with a schema prefix or not.</p>
	 * <p><i>Note: This information is used only when writing TAP_SCHEMA.tables or when writing the output of the resource /tables.</i></p>
	 * @since 2.0
	 * @deprecated See {@link #simplificationNeeded}, {@link #getRawName()} and {@link #getADQLName()}. */
	@Deprecated
	private boolean isInitiallyQualified;

	/** Name that this table have in the database.
	 * <i>Note: If NULL, {@link #getDBName()} returns what {@link #getADQLName()} returns.</i> */
	private String dbName = null;

	/** The schema which owns this table.
	 *  <i>Note: It is NULL only at the construction.
	 * 	Then, this attribute is automatically set by a {@link TAPSchema} when adding this table inside it
	 * 	with {@link TAPSchema#addTable(TAPTable)}.</i> */
	private TAPSchema schema = null;

	/** Type of this table.
	 * <i>Note: Standard TAP table field ; CAN NOT be NULL ; by default, it is "table".</i> */
	private TableType type = TableType.table;

	/** Descriptive, human-interpretable name of the table.
	 * <i>Note: Standard TAP table field ; MAY be NULL.</i>
	 * @since 2.0 */
	private String title = null;

	/** Description of this table.
	 * <i>Note: Standard TAP table field ; MAY be NULL.</i> */
	private String description = null;

	/** UType associating this table with a data-model.
	 * <i>Note: Standard TAP table field ; MAY be NULL.</i> */
	private String utype = null;

	/** Ordering index of this table inside its schema.
	 * <i>Note: Standard TAP table field since TAP 1.1.</i>
	 * @since 2.1 */
	private int index = -1;

	/** List of columns composing this table.
	 * <i>Note: all columns of this list are linked to this table from the moment they are added inside it.</i> */
	protected final Map<String,TAPColumn> columns;

	/** List of all foreign keys linking this table to others. */
	protected final ArrayList<TAPForeignKey> foreignKeys;

	/** Let add some information in addition of the ones of the TAP protocol.
	 * <i>Note: This object can be anything: an {@link Integer}, a {@link String}, a {@link Map}, a {@link List}, ...
	 * Its content is totally free and never used or checked.</i> */
	protected Object otherData = null;

	/**
	 * <p>Build a {@link TAPTable} instance with the given ADQL name.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The table type is set by default to "table".
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the table name is prefixed by its schema name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the schema name must be exactly the same
	 * 		as what the function {@link TAPSchema#getRawName()} of the set schema returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single table name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param tableName	Name that this table MUST have in ADQL queries.
	 *                 	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPTable(String tableName) throws NullPointerException{
		if (tableName == null)
			throw new NullPointerException("Missing table name!");

		adqlName = tableName.trim();
		simplificationNeeded = (adqlName.indexOf('.') > 0 || adqlName.indexOf('"') >= 0);
		isInitiallyQualified = (adqlName.indexOf('.') > 0);

		if (getADQLName().length() == 0)
			throw new NullPointerException("Missing table name!");

		dbName = null;

		columns = new LinkedHashMap<String,TAPColumn>();
		foreignKeys = new ArrayList<TAPForeignKey>();
	}

	/**
	 * <p>Build a {@link TAPTable} instance with the given ADQL name and table type.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The table type is set by default to "table".
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the table name is prefixed by its schema name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the schema name must be exactly the same
	 * 		as what the function {@link TAPSchema#getRawName()} of the set schema returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single table name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param tableName	Name that this table MUST have in ADQL queries.
	 *                 	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param tableType	Type of this table. <i>If NULL, "table" will be the type of this table.</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 * 
	 * @see #setType(TableType)
	 */
	public TAPTable(String tableName, TableType tableType) throws NullPointerException{
		this(tableName);
		setType(tableType);
	}

	/**
	 * <p>Build a {@link TAPTable} instance with the given ADQL name, table type, description and UType.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The table type is set by default to "table".
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the table name is prefixed by its schema name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the schema name must be exactly the same
	 * 		as what the function {@link TAPSchema#getRawName()} of the set schema returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single table name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param tableName		Name that this table MUST have in ADQL queries.
	 *                 		<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param tableType		Type of this table. <i>If NULL, "table" will be the type of this table.</i>
	 * @param description	Description of this table. <i>MAY be NULL.</i>
	 * @param utype			UType associating this table with a data-model. <i>MAY be NULL</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 * 
	 * @see #setType(TableType)
	 */
	public TAPTable(String tableName, TableType tableType, String description, String utype) throws NullPointerException{
		this(tableName, tableType);
		this.description = description;
		this.utype = utype;
	}

	/**
	 * <p>Get the qualified name of this table.</p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	The part of the returned full name won't be double quoted!
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If this table is not attached to a schema, this function will just return
	 * 	the ADQL name of this table.
	 * </i></p>
	 * 
	 * @return	Qualified ADQL name of this table.
	 */
	public final String getFullName(){
		if (schema != null)
			return schema.getADQLName() + "." + getADQLName();
		else
			return adqlName;
	}

	/**
	 * Get the ADQL name (the name this table MUST have in ADQL queries).
	 * 
	 * @return	Its ADQL name.
	 * @see #getADQLName()
	 * @deprecated	Does not do anything special: just call {@link #getADQLName()}.
	 */
	@Deprecated
	public final String getName(){
		return getADQLName();
	}

	@Override
	public final String getADQLName(){
		if (simplificationNeeded){
			String tmp = adqlName;
			// Remove the schema prefix if any:
			if (schema != null && tmp.startsWith(schema.getRawName() + "."))
				tmp = tmp.substring((schema.getRawName() + ".").length()).trim();
			// Remove the surrounding double-quotes if any:
			/*****TODO: G.Landais - this code doesn't work!.... 
			if (tmp.matches("\"[^\"]*\""))
				tmp = tmp.substring(1, tmp.length() - 1);*/
			// Finally, return the result:
			return tmp;
		}else
			return adqlName;
	}

	/**
	 * Get the full ADQL name of this table, as it has been provided at initialization.
	 * 
	 * @return	Get the original ADQL name.
	 * 
	 * @since 2.1
	 */
	public final String getRawName(){
		return adqlName;
	}

	/**
	 * <p>Tells whether the ADQL name of this table must be qualified in the "table_name" column of TAP_SCHEMA.tables
	 * and in the /schema/table/name field of the resource /tables.</p>
	 * 
	 * <p><i>Note: this value is set automatically by the constructor: "true" if the table name was qualified,
	 * "false" otherwise. It can be changed with the function {@link #setInitiallyQualifed(boolean)}, BUT by doing so
	 * you may generate a mismatch between the table name of TAP_SCHEMA.tables and the one of /tables.</i></p>
	 * 
	 * @return	<i>true</i> if the table name must be qualified in TAP_SCHEMA.tables and in /tables, <i>false</i> otherwise.
	 * 
	 * @since 2.0
	 * @deprecated	To get name of the table as it should be used: {@link #getRawName()}.
	 *            	To get just the table name (with no prefix and surrounding double quotes): {@link #getADQLName()}.
	 */
	@Deprecated
	public final boolean isInitiallyQualified(){
		return isInitiallyQualified;
	}

	/**
	 * <p>Let specifying whether the table name must be qualified in TAP_SCHEMA.tables and in the resource /tables.</p>
	 * 
	 * <p><b>WARNING: Calling this function may generate a mismatch between the table name of TAP_SCHEMA.tables and
	 * the one of the resource /tables. So, be sure to change this flag before setting the content of TAP_SCHEMA.tables
	 * using {@link tap.db.JDBCConnection#setTAPSchema(TAPMetadata)}.</b></p>
	 * 
	 * @param mustBeQualified	<i>true</i> if the table name in TAP_SCHEMA.tables and in the resource /tables must be qualified by the schema name,
	 *                       	<i>false</i> otherwise.
	 * 
	 * @since 2.0
	 * @deprecated	To get name of the table as it should be used: {@link #getRawName()}.
	 *            	To get just the table name (with no prefix and surrounding double quotes): {@link #getADQLName()}.
	 */
	@Deprecated
	public final void setInitiallyQualifed(final boolean mustBeQualified){
		isInitiallyQualified = mustBeQualified;
	}

	@Override
	public final String getDBName(){
		return (dbName == null) ? getADQLName() : dbName;
	}

	/**
	 * <p>Change the name that this table MUST have in the database (i.e. in SQL queries).</p>
	 * 
	 * <p><i>Note:
	 * 	If the given value is NULL or an empty string, {@link #getDBName()} will return exactly what {@link #getADQLName()} returns.
	 * </i></p>
	 * 
	 * @param name	The new database name of this table.
	 */
	public final void setDBName(String name){
		name = (name != null) ? name.trim() : name;
		if (name != null && name.length() > 0)
			dbName = name;
		else
			dbName = null;
	}

	@Override
	public String getADQLCatalogName(){
		return null;
	}

	@Override
	public String getDBCatalogName(){
		return null;
	}

	@Override
	public final String getADQLSchemaName(){
		return schema == null ? null : schema.getADQLName();
	}

	@Override
	public final String getDBSchemaName(){
		return schema == null ? null : schema.getDBName();
	}

	/**
	 * Get the schema that owns this table.
	 * 
	 * @return Its schema. <i>MAY be NULL</i>
	 */
	public final TAPSchema getSchema(){
		return schema;
	}

	/**
	 * <p>Set the schema in which this schema is.</p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPSchema}
	 * 	that owns this table.
	 * </i></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If this table was already linked with another {@link TAPSchema} object, the previous link is removed
	 * 	here, but also in the schema (by calling {@link TAPSchema#removeTable(String)}).
	 * </i></p>
	 * 
	 * @param schema	The schema that owns this table.
	 */
	protected final void setSchema(final TAPSchema schema){
		if (this.schema != null && (schema == null || !schema.equals(this.schema)))
			this.schema.removeTable(adqlName);
		this.schema = schema;
	}

	/**
	 * Get the type of this table.
	 * 
	 * @return	Its type.
	 */
	public final TableType getType(){
		return type;
	}

	/**
	 * <p>Set the type of this table.</p>
	 * 
	 * <p><i>Note:
	 * 	If the given type is NULL, nothing will be done ; the type of this table won't be changed.
	 * </i></p>
	 * 
	 * @param type	Its new type.
	 */
	public final void setType(TableType type){
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
	public final String getTitle(){
		return title;
	}

	/**
	 * Set the title of this table.
	 * 
	 * @param title	Its new title. <i>MAY be NULL</i>
	 * 
	 * @since 2.0
	 */
	public final void setTitle(final String title){
		this.title = title;
	}

	/**
	 * Get the description of this table.
	 * 
	 * @return	Its description. <i>MAY be NULL</i>
	 */
	public final String getDescription(){
		return description;
	}

	/**
	 * Set the description of this table.
	 * 
	 * @param description	Its new description. <i>MAY be NULL</i>
	 */
	public final void setDescription(String description){
		this.description = description;
	}

	/**
	 * Get the UType associating this table with a data-model.
	 * 
	 * @return	Its UType. <i>MAY be NULL</i>
	 */
	public final String getUtype(){
		return utype;
	}

	/**
	 * Set the UType associating this table with a data-model.
	 * 
	 * @param utype	Its new UType. <i>MAY be NULL</i>
	 */
	public final void setUtype(String utype){
		this.utype = utype;
	}

	/**
	 * Get the ordering index of this table inside its schema.
	 * 
	 * @return	Its ordering index.
	 * 
	 * @since 2.1
	 */
	public final int getIndex(){
		return index;
	}

	/**
	 * Set the ordering index of this table inside its schema.
	 * 
	 * @param tableIndex	Its new ordering index.
	 * 
	 * @since 2.1
	 */
	public final void setIndex(int tableIndex){
		this.index = tableIndex;
	}

	/**
	 * <p>Get the other (piece of) information associated with this table.</p>
	 * 
	 * <p><i>Note:
	 * 	By default, NULL is returned, but it may be any kind of value ({@link Integer},
	 * 	{@link String}, {@link Map}, {@link List}, ...).
	 * </i></p>
	 * 
	 * @return	The other (piece of) information. <i>MAY be NULL</i>
	 */
	public Object getOtherData(){
		return otherData;
	}

	/**
	 * Set the other (piece of) information associated with this table.
	 * 
	 * @param data	Another information about this table. <i>MAY be NULL</i>
	 */
	public void setOtherData(Object data){
		otherData = data;
	}

	/**
	 * <p>Add a column to this table.</p>
	 * 
	 * <p><i>Note:
	 * 	If the given column is NULL, nothing will be done.
	 * </i></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	By adding the given column inside this table, it
	 * 	will be linked with this table using {@link TAPColumn#setTable(DBTable)}.
	 * 	In this function, if the column was already linked with another {@link TAPTable},
	 * 	the former link is removed using {@link TAPTable#removeColumn(String)}.
	 * </i></p>
	 * 
	 * @param newColumn	Column to add inside this table.
	 */
	public final void addColumn(final TAPColumn newColumn){
		if (newColumn != null && newColumn.getADQLName() != null){
			newColumn.setTable(this);
			columns.put(newColumn.getADQLName(), newColumn);
		}
	}

	/**
	 * <p>Build a {@link TAPColumn} object whose the ADQL and DB name will the given one.
	 * Then, add this column inside this table.</p>
	 * 
	 * <p><i>Note:
	 * 	The built {@link TAPColumn} object is returned, so that being modified afterwards if needed.
	 * </i></p>
	 * 
	 * @param columnName	ADQL name (and indirectly also the DB name) of the column to create and add.
	 * 
	 * @return	The created and added {@link TAPColumn} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 * 
	 * @see TAPColumn#TAPColumn(String)
	 * @see #addColumn(TAPColumn)
	 */
	public final TAPColumn addColumn(String columnName){
		if (columnName == null || columnName.trim().length() <= 0)
			return null;

		TAPColumn c = new TAPColumn(columnName);
		addColumn(c);
		return c;
	}

	/**
	 * <p>Build a {@link TAPColumn} object whose the ADQL and DB name will the given one.
	 * Then, add this column inside this table.</p>
	 * 
	 * <p><i>Note:
	 * 	The built {@link TAPColumn} object is returned, so that being modified afterwards if needed.
	 * </i></p>
	 * 
	 * @param columnName	ADQL name (and indirectly also the DB name) of the column to create and add.
	 * @param datatype		Type of the new column's values. <i>If NULL, VARCHAR will be the type of the created column.</i>
	 * @param description	Description of the new column. <i>MAY be NULL</i>
	 * @param unit			Unit of the new column's values. <i>MAY be NULL</i>
	 * @param ucd			UCD describing the scientific content of the new column. <i>MAY be NULL</i>
	 * @param utype			UType associating the new column with a data-model. <i>MAY be NULL</i>
	 * 
	 * @return	The created and added {@link TAPColumn} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 * 
	 * @see TAPColumn#TAPColumn(String, DBType, String, String, String, String)
	 * @see #addColumn(TAPColumn)
	 */
	public TAPColumn addColumn(String columnName, DBType datatype, String description, String unit, String ucd, String utype){
		if (columnName == null || columnName.trim().length() <= 0)
			return null;

		TAPColumn c = new TAPColumn(columnName, datatype, description, unit, ucd, utype);
		addColumn(c);
		return c;
	}

	/**
	 * <p>Build a {@link TAPColumn} object whose the ADQL and DB name will the given one.
	 * Then, add this column inside this table.</p>
	 * 
	 * <p><i>Note:
	 * 	The built {@link TAPColumn} object is returned, so that being modified afterwards if needed.
	 * </i></p>
	 * 
	 * @param columnName	ADQL name (and indirectly also the DB name) of the column to create and add.
	 * @param datatype		Type of the new column's values. <i>If NULL, VARCHAR will be the type of the created column.</i>
	 * @param description	Description of the new column. <i>MAY be NULL</i>
	 * @param unit			Unit of the new column's values. <i>MAY be NULL</i>
	 * @param ucd			UCD describing the scientific content of the new column. <i>MAY be NULL</i>
	 * @param utype			UType associating the new column with a data-model. <i>MAY be NULL</i>
	 * @param principal		<i>true</i> if the new column should be returned by default, <i>false</i> otherwise.
	 * @param indexed		<i>true</i> if the new column is indexed, <i>false</i> otherwise.
	 * @param std			<i>true</i> if the new column is defined by a standard, <i>false</i> otherwise.
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
	public TAPColumn addColumn(String columnName, DBType datatype, String description, String unit, String ucd, String utype, boolean principal, boolean indexed, boolean std){
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
	 * <p>Tell whether this table contains a column with the given ADQL name.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive.
	 * </i></p>
	 * 
	 * @param columnName	ADQL name (case sensitive) of the column whose the existence must be checked.
	 * 
	 * @return	<i>true</i> if a column having the given ADQL name exists in this table, <i>false</i> otherwise.
	 */
	public final boolean hasColumn(String columnName){
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
	public Iterator<TAPColumn> getColumns(){
		return columns.values().iterator();
	}

	@Override
	public DBColumn getColumn(String colName, boolean byAdqlName){
		if (byAdqlName)
			return getColumn(colName);
		else{
			if (colName != null && colName.length() > 0){
				Collection<TAPColumn> collColumns = columns.values();
				for(TAPColumn column : collColumns){
					if (column.getDBName().equals(colName))
						return column;
				}
			}
			return null;
		}
	}

	/**
	 * <p>Search a column inside this table having the given ADQL name.</p>
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
	public final TAPColumn getColumn(String columnName){
		if (columnName == null)
			return null;
		else
			return columns.get(columnName);
	}

	/**
	 * <p>Tell whether this table contains a column with the given ADQL or DB name.</p>
	 * 
	 * <p><i>Note:
	 * 	This functions is just calling {@link #getColumn(String, boolean)} and compare its result
	 * 	with NULL in order to check the existence of the specified column.
	 * </i></p>
	 * 
	 * @param colName		ADQL or DB name that the column to search must have.
	 * @param byAdqlName	<i>true</i> to search the column by ADQL name, <i>false</i> to search by DB name.
	 * 
	 * @return	<i>true</i> if a column has been found inside this table with the given ADQL or DB name,
	 *        	<i>false</i> otherwise.
	 * 
	 * @see #getColumn(String, boolean)
	 */
	public boolean hasColumn(String colName, boolean byAdqlName){
		return (getColumn(colName, byAdqlName) != null);
	}

	/**
	 * Get the number of columns composing this table.
	 * 
	 * @return	Number of its columns.
	 */
	public final int getNbColumns(){
		return columns.size();
	}

	/**
	 * Tell whether this table contains no column.
	 * 
	 * @return	<i>true</i> if this table is empty (no column),
	 *        	<i>false</i> if it contains at least one column.
	 */
	public final boolean isEmpty(){
		return columns.isEmpty();
	}

	/**
	 * <p>Remove the specified column.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function is case sensitive!
	 * </i></p>
	 * 
	 * <p><i>Note:
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
	public final TAPColumn removeColumn(String columnName){
		if (columnName == null)
			return null;

		TAPColumn removedColumn = columns.remove(columnName);
		if (removedColumn != null)
			deleteColumnRelations(removedColumn);

		return removedColumn;
	}

	/**
	 * Delete all foreign keys having the given column in the sources or the targets list.
	 * 
	 * @param col	A column.
	 */
	protected final void deleteColumnRelations(TAPColumn col){
		// Remove the relation between the column and this table:
		col.setTable(null);

		// Remove the relations between the column and other tables/columns:
		Iterator<TAPForeignKey> it = col.getTargets();
		while(it.hasNext())
			removeForeignKey(it.next());

		it = col.getSources();
		while(it.hasNext()){
			TAPForeignKey key = it.next();
			key.getFromTable().removeForeignKey(key);
		}
	}

	/**
	 * Remove all columns composing this table.
	 * Foreign keys will also be deleted.
	 */
	public final void removeAllColumns(){
		Iterator<Map.Entry<String,TAPColumn>> it = columns.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String,TAPColumn> entry = it.next();
			it.remove();
			deleteColumnRelations(entry.getValue());
		}
	}

	/**
	 * <p>Add the given foreign key to this table.</p>
	 * 
	 * <p><i>Note:
	 * 	This function will do nothing if the given foreign key is NULL.
	 * </i></p>
	 * 
	 * <p><i><b>WARNING:</b>
	 * 	The source table ({@link TAPForeignKey#getFromTable()}) of the given foreign key MUST be this table
	 * 	and the foreign key MUST be completely defined.
	 * 	If not, an exception will be thrown and the key won't be added.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given foreign key is added to this table, all the columns of this key will be
	 * 	linked to the foreign key using either {@link TAPColumn#addSource(TAPForeignKey)} or
	 * 	{@link TAPColumn#addTarget(TAPForeignKey)}.
	 * </i></p>
	 * 
	 * @param key	Foreign key (whose the FROM table is this table) to add inside this table.
	 * 
	 * @throws TAPException	If the source table of the given foreign key is not this table
	 *                     	or if the given key is not completely defined.
	 */
	public final void addForeignKey(TAPForeignKey key) throws TAPException{
		if (key == null)
			return;

		String keyId = key.getKeyId();
		final String errorMsgPrefix = "Impossible to add the foreign key \"" + keyId + "\" because ";

		if (key.getFromTable() == null)
			throw new TAPException(errorMsgPrefix + "no source table is specified !");

		if (!this.equals(key.getFromTable()))
			throw new TAPException(errorMsgPrefix + "the source table is not \"" + getADQLName() + "\"");

		if (key.getTargetTable() == null)
			throw new TAPException(errorMsgPrefix + "no target table is specified !");

		if (key.isEmpty())
			throw new TAPException(errorMsgPrefix + "it defines no relation !");

		if (foreignKeys.add(key)){
			try{
				TAPTable targetTable = key.getTargetTable();
				for(Map.Entry<String,String> relation : key){
					if (!hasColumn(relation.getKey()))
						throw new TAPException(errorMsgPrefix + "the source column \"" + relation.getKey() + "\" doesn't exist in \"" + getADQLName() + "\" !");
					else if (!targetTable.hasColumn(relation.getValue()))
						throw new TAPException(errorMsgPrefix + "the target column \"" + relation.getValue() + "\" doesn't exist in \"" + targetTable.getADQLName() + "\" !");
					else{
						getColumn(relation.getKey()).addTarget(key);
						targetTable.getColumn(relation.getValue()).addSource(key);
					}
				}
			}catch(TAPException ex){
				foreignKeys.remove(key);
				throw ex;
			}
		}
	}

	/**
	 * <p>Build a foreign key using the ID, the target table and the given list of columns.
	 * Then, add the created foreign key to this table.</p>
	 * 
	 * <p><i>Note:
	 * 	The source table of the created foreign key ({@link TAPForeignKey#getFromTable()}) will be this table.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given foreign key is added to this table, all the columns of this key will be
	 * 	linked to the foreign key using either {@link TAPColumn#addSource(TAPForeignKey)} or
	 * 	{@link TAPColumn#addTarget(TAPForeignKey)}.
	 * </i></p>
	 * 
	 * @return	The created and added foreign key.
	 * 
	 * @throws TAPException	If the specified key is not completely or correctly defined.
	 * 
	 * @see TAPForeignKey#TAPForeignKey(String, TAPTable, TAPTable, Map)
	 */
	public TAPForeignKey addForeignKey(String keyId, TAPTable targetTable, Map<String,String> columns) throws TAPException{
		TAPForeignKey key = new TAPForeignKey(keyId, this, targetTable, columns);
		addForeignKey(key);
		return key;
	}

	/**
	 * <p>Build a foreign key using the ID, the target table, the given list of columns, the given description and the given UType.
	 * Then, add the created foreign key to this table.</p>
	 * 
	 * <p><i>Note:
	 * 	The source table of the created foreign key ({@link TAPForeignKey#getFromTable()}) will be this table.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given foreign key is added to this table, all the columns of this key will be
	 * 	linked to the foreign key using either {@link TAPColumn#addSource(TAPForeignKey)} or
	 * 	{@link TAPColumn#addTarget(TAPForeignKey)}.
	 * </i></p>
	 * 
	 * @return	The created and added foreign key.
	 * 
	 * @throws TAPException	If the specified key is not completely or correctly defined.
	 * 
	 * @see TAPForeignKey#TAPForeignKey(String, TAPTable, TAPTable, Map, String, String)
	 */
	public TAPForeignKey addForeignKey(String keyId, TAPTable targetTable, Map<String,String> columns, String description, String utype) throws TAPException{
		TAPForeignKey key = new TAPForeignKey(keyId, this, targetTable, columns, description, utype);
		addForeignKey(key);
		return key;
	}

	/**
	 * Get the list of all foreign keys associated whose the source is this table.
	 * 
	 * @return	An iterator over all its foreign keys.
	 */
	public final Iterator<TAPForeignKey> getForeignKeys(){
		return foreignKeys.iterator();
	}

	/**
	 * Get the number of all foreign keys whose the source is this table
	 * 
	 * @return	Number of all its foreign keys.
	 */
	public final int getNbForeignKeys(){
		return foreignKeys.size();
	}

	/**
	 * <p>Remove the given foreign key from this table.</p>
	 * 
	 * <p><i>Note:
	 * 	This function will also delete the link between the columns of the foreign key
	 * 	and the foreign key, using {@link #deleteRelations(TAPForeignKey)}.
	 * </i></p>
	 * 
	 * @param keyToRemove	Foreign key to removed from this table.
	 * 
	 * @return	<i>true</i> if the key has been successfully removed,
	 *        	<i>false</i> otherwise.
	 */
	public final boolean removeForeignKey(TAPForeignKey keyToRemove){
		if (foreignKeys.remove(keyToRemove)){
			deleteRelations(keyToRemove);
			return true;
		}else
			return false;
	}

	/**
	 * <p>Remove all the foreign keys whose the source is this table.</p>
	 * 
	 * <p><i>Note:
	 * 	This function will also delete the link between the columns of all the removed foreign keys
	 * 	and the foreign keys, using {@link #deleteRelations(TAPForeignKey)}.
	 * </i></p>
	 */
	public final void removeAllForeignKeys(){
		Iterator<TAPForeignKey> it = foreignKeys.iterator();
		while(it.hasNext()){
			deleteRelations(it.next());
			it.remove();
		}
	}

	/**
	 * Delete the link between all columns of the given foreign key
	 * and this foreign key. Thus, these columns won't be anymore source or target
	 * of this foreign key.
	 * 
	 * @param key	A foreign key whose links with its columns must be deleted.
	 */
	protected final void deleteRelations(TAPForeignKey key){
		for(Map.Entry<String,String> relation : key){
			TAPColumn col = key.getFromTable().getColumn(relation.getKey());
			if (col != null)
				col.removeTarget(key);

			col = key.getTargetTable().getColumn(relation.getValue());
			if (col != null)
				col.removeSource(key);
		}
	}

	@Override
	public Iterator<DBColumn> iterator(){
		return new Iterator<DBColumn>(){
			private final Iterator<TAPColumn> it = getColumns();

			@Override
			public boolean hasNext(){
				return it.hasNext();
			}

			@Override
			public DBColumn next(){
				return it.next();
			}

			@Override
			public void remove(){
				it.remove();
			}
		};
	}

	@Override
	public String toString(){
		return ((schema != null) ? (schema.getADQLName() + ".") : "") + getADQLName();
	}

	@Override
	public DBTable copy(final String dbName, final String adqlName){
		TAPTable copy = new TAPTable((adqlName == null) ? this.adqlName : adqlName);
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
