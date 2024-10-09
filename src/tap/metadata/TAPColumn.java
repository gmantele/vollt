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
 * Copyright 2012-2024 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
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

/**
 * <p>Represent a column as described by the IVOA standard in the TAP protocol definition.</p>
 * 
 * <p>
 * 	This object representation has exactly the same fields as the column of the table TAP_SCHEMA.columns.
 * 	But it also provides a way to add other data. For instance, if information not listed in the standard
 * 	may be stored here, they can be using the function {@link #setOtherData(Object)}. This object can be
 * 	a single value (integer, string, ...), but also a {@link Map}, {@link List}, etc...
 * </p>
 * 
 * <p><i><b>Important note:</b>
 * 	A {@link TAPColumn} object MUST always have a DB name. That's why, {@link #getDBName()} returns
 * 	what {@link #getADQLName()} returns when no DB name is set. After creation, it is possible to set
 * 	the DB name with {@link #setDBName(String)}.
 * 	<br/>
 * 	This DB name MUST be UNqualified and without double quotes. If a NULL or empty value is provided,
 * 	{@link #getDBName()} returns what {@link #getADQLName()} returns.
 * </i></p>
 * 
 * <h3>Set a table</h3>
 * 
 * <p>
 *	By default a column is detached (not part of a table). To specify the table in which this column is,
 *	you must use {@link TAPTable#addColumn(TAPColumn)}. By doing this, the table link inside this column
 *	will be set automatically and you will be able to get the table with {@link #getTable()}.
 * </p>
 * 
 * <h3>Foreign keys</h3>
 * 
 * <p>
 * 	In case this column is linked to one or several of other tables, it will be possible to list all
 * 	foreign keys where the target columns is with {@link #getTargets()}. In the same way, it will be
 * 	possible to list all foreign keys in which this column is a target with {@link #getSources()}.
 * 	However, in order to ensure the consistency between all metadata, these foreign key's links are
 * 	set at the table level by the table itself using {@link #addSource(TAPForeignKey)} and
 * 	{@link #addTarget(TAPForeignKey)}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (10/2024)
 */
public class TAPColumn implements DBColumn {

	/** Name that this column MUST have in ADQL queries. */
	private final String adqlName;

	/** Indicates whether the given ADQL name must be simplified by {@link #getADQLName()}.
	 * <p>Here, "simplification" means removing the surrounding double quotes and the table prefix if any.</p>
	 * @since 2.1 */
	private final boolean simplificationNeeded;

	/** Name that this column have in the database.
	 * <i>Note: It CAN NOT be NULL. By default, it is the ADQL name.</i> */
	private String dbName = null;

	/** Table which owns this column.
	 * <i>Note: It should be NULL only at the construction or for a quick representation of a column.
	 * 	Then, this attribute is automatically set by a {@link TAPTable} when adding this column inside it
	 * 	with {@link TAPTable#addColumn(TAPColumn)}.</i> */
	private DBTable table = null;

	/** Description of this column.
	 * <i>Note: Standard TAP column field ; MAY be NULL.</i> */
	private String description = null;

	/** Unit of this column's values.
	 * <i>Note: Standard TAP column field ; MAY be NULL.</i> */
	private String unit = null;

	/** UCD describing the scientific content of this column.
	 * <i>Note: Standard TAP column field ; MAY be NULL.</i> */
	private String ucd = null;

	/** UType associating this column with a data-model.
	 * <i>Note: Standard TAP column field ; MAY be NULL.</i> */
	private String utype = null;

	/** Type of this column.
	 * <i>Note: Standard TAP column field ; CAN'T be NULL.</i> */
	private DBType datatype = new DBType(DBDatatype.UNKNOWN);

	/** Flag indicating whether this column is one of those that should be returned by default.
	 * <i>Note: Standard TAP column field ; FALSE by default.</i> */
	private boolean principal = false;

	/** Flag indicating whether this column is indexed in the database.
	 * <i>Note: Standard TAP column field ; FALSE by default.</i> */
	private boolean indexed = false;

	/** Flag indicating whether this column can be set to NULL in the database.
	 * <i>Note: Standard TAP column field ; FALSE by default.</i>
	 * @since 2.0 */
	private boolean nullable = false;

	/** Flag indicating whether this column is defined by a standard.
	 * <i>Note: Standard TAP column field ; FALSE by default.</i> */
	private boolean std = false;

	/** Ordering index of this column inside its table.
	 * <i>Note: Standard TAP column field since TAP 1.1.</i>
	 * @since 2.1 */
	private int index = -1;
	
	/** Coordinate system used by this column values.
	 * <i>Note: Of course, this attribute has to be set only on coordinate columns.</i>
	 * @since 2.1 */
	private TAPCoosys coosys = null;

	/** Let add some information in addition of the ones of the TAP protocol.
	 * <i>Note: This object can be anything: an {@link Integer}, a {@link String}, a {@link Map}, a {@link List}, ...
	 * Its content is totally free and never used or checked.</i> */
	protected Object otherData = null;

	/** List all foreign keys in which this column is a source.
	 * <p><b>CAUTION: For consistency consideration, this attribute SHOULD never be modified!
	 * 	It is set by the constructor and filled ONLY by the table.</b></p> */
	protected final ArrayList<TAPForeignKey> lstTargets;

	/** List all foreign keys in which this column is a target.
	 * <p><b>CAUTION: For consistency consideration, this attribute SHOULD never be modified!
	 * 	It is set by the constructor and filled ONLY by the table.</b></p> */
	protected final ArrayList<TAPForeignKey> lstSources;

	/**
	 * <p>Build a VARCHAR {@link TAPColumn} instance with the given ADQL name.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The datatype is set by default to VARCHAR.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPColumn(String columnName) throws NullPointerException{
		if (columnName == null)
			throw new NullPointerException("Missing column name!");

		adqlName = columnName.trim();
		simplificationNeeded = (adqlName.indexOf('.') > 0 || adqlName.matches("\"[^\"]*\""));

		if (getADQLName().length() == 0)
			throw new NullPointerException("Missing column name!");

		dbName = null;

		lstTargets = new ArrayList<TAPForeignKey>(1);
		lstSources = new ArrayList<TAPForeignKey>(1);
	}

	/**
	 * <p>Build a {@link TAPColumn} instance with the given ADQL name and datatype.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The datatype is set by default to VARCHAR.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param type			Datatype of this column. <i>If NULL, VARCHAR will be the datatype of this column</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 * 
	 * @see #setDatatype(DBType)
	 */
	public TAPColumn(String columnName, DBType type) throws NullPointerException{
		this(columnName);
		setDatatype(type);
	}

	/**
	 * <p>Build a VARCHAR {@link TAPColumn} instance with the given ADQL name and description.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The datatype is set by default to VARCHAR.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param description	Description of the column's content. <i>May be NULL</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPColumn(String columnName, String description) throws NullPointerException{
		this(columnName, (DBType)null, description);
	}

	/**
	 * <p>Build a {@link TAPColumn} instance with the given ADQL name, datatype and description.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 *	The datatype is set by calling the function {@link #setDatatype(DBType)} which does not do
	 *	anything if the given datatype is NULL.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param type			Datatype of this column. <i>If NULL, VARCHAR will be the datatype of this column</i>
	 * @param description	Description of the column's content. <i>May be NULL</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPColumn(String columnName, DBType type, String description) throws NullPointerException{
		this(columnName, type);
		this.description = description;
	}

	/**
	 * <p>Build a VARCHAR {@link TAPColumn} instance with the given ADQL name, description and unit.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The datatype is set by default to VARCHAR.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param description	Description of the column's content. <i>May be NULL</i>
	 * @param unit			Unit of the column's values. <i>May be NULL</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPColumn(String columnName, String description, String unit) throws NullPointerException{
		this(columnName, null, description, unit);
	}

	/**
	 * <p>Build a {@link TAPColumn} instance with the given ADQL name, type, description and unit.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 *	The datatype is set by calling the function {@link #setDatatype(DBType)} which does not do
	 *	anything if the given datatype is NULL.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param type			Datatype of this column. <i>If NULL, VARCHAR will be the datatype of this column</i>
	 * @param description	Description of the column's content. <i>May be NULL</i>
	 * @param unit			Unit of the column's values. <i>May be NULL</i>
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPColumn(String columnName, DBType type, String description, String unit) throws NullPointerException{
		this(columnName, type, description);
		this.unit = unit;
	}

	/**
	 * <p>Build a VARCHAR {@link TAPColumn} instance with the given fields.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The datatype is set by default to VARCHAR.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param description	Description of the column's content. <i>May be NULL</i>
	 * @param unit			Unit of the column's values. <i>May be NULL</i>
	 * @param ucd			UCD describing the scientific content of this column.
	 * @param utype			UType associating this column with a data-model.
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPColumn(String columnName, String description, String unit, String ucd, String utype) throws NullPointerException{
		this(columnName, null, description, unit, ucd, utype);
	}

	/**
	 * <p>Build a {@link TAPColumn} instance with the given fields.</p>
	 * 
	 * <p><i>Note 1:
	 * 	The DB name is set by default to NULL so that {@link #getDBName()} returns exactly what {@link #getADQLName()} returns.
	 * 	To set a specific DB name, you MUST call {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 *	The datatype is set by calling the function {@link #setDatatype(DBType)} which does not do
	 *	anything if the given datatype is NULL.
	 * </i></p>
	 * 
	 * <p><b>Important notes on the given ADQL name:</b></p>
	 * <ul>
	 * 	<li>Any leading or trailing space is immediately deleted.</li>
	 * 	<li>
	 * 		If the column name is prefixed by its table name, this prefix is removed by {@link #getADQLName()}
	 * 		but will be still here when using {@link #getRawName()}. To work, the table name must be exactly the same
	 * 		as what the function {@link TAPTable#getRawName()} of the set table returns.
	 *	</li>
	 * 	<li>
	 * 		Double quotes may surround the single column name. They will be removed by {@link #getADQLName()} but will
	 * 		still appear in the result of {@link #getRawName()}.
	 *	</li>
	 * </ul>
	 * 
	 * @param columnName	Name that this column MUST have in ADQL queries.
	 *                  	<i>CAN'T be NULL ; this name can never be changed after initialization.</i>
	 * @param type			Datatype of this column. <i>If NULL, VARCHAR will be the datatype of this column</i>
	 * @param description	Description of the column's content. <i>May be NULL</i>
	 * @param unit			Unit of the column's values. <i>May be NULL</i>
	 * @param ucd			UCD describing the scientific content of this column.
	 * @param utype			UType associating this column with a data-model.
	 * 
	 * @throws NullPointerException	If the given name is <code>null</code>,
	 *                             	or if the given string is empty after simplification
	 *                             	(i.e. without the surrounding double quotes).
	 */
	public TAPColumn(String columnName, DBType type, String description, String unit, String ucd, String utype) throws NullPointerException{
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
	public final String getName(){
		return getADQLName();
	}

	@Override
	public final String getADQLName(){
		if (simplificationNeeded){
			String tmp = adqlName;
			// Remove the table prefix if any:
			if (table != null){
				String tablePrefix = ((table instanceof TAPTable) ? ((TAPTable)table).getRawName() : table.getADQLName()) + ".";
				if (tmp.startsWith(tablePrefix))
					tmp = tmp.substring(tablePrefix.length()).trim();
			}
			// Remove the surrounding double-quotes if any:
			if (tmp.matches("\"[^\"]*\""))
				tmp = tmp.substring(1, tmp.length() - 1);
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

	@Override
	public final String getDBName(){
		return (dbName == null) ? getADQLName() : dbName;
	}

	/**
	 * <p>Change the name that this column MUST have in the database (i.e. in SQL queries).</p>
	 * 
	 * <p><i>Note:
	 * 	If the given value is NULL or an empty string, nothing is done ; the DB name keeps is former value.
	 * </i></p>
	 * 
	 * @param name	The new database name of this column.
	 */
	public final void setDBName(String name){
		name = (name != null) ? name.trim() : name;
		if (name != null && name.length() > 0)
			dbName = name;
		else
			dbName = null;
	}

	@Override
	public final DBTable getTable(){
		return table;
	}

	/**
	 * <p>Set the table in which this column is.</p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPTable}
	 * 	that owns this column.
	 * </i></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If this column was already linked with another {@link TAPTable} object, the previous link is removed
	 * 	here, but also in the table (by calling {@link TAPTable#removeColumn(String)}).
	 * </i></p>
	 * 
	 * @param table	The table that owns this column.
	 */
	protected final void setTable(final DBTable table){
		if (this.table != null && this.table instanceof TAPTable && (table == null || !table.equals(this.table)))
			((TAPTable)this.table).removeColumn(adqlName);
		this.table = table;
	}

	/**
	 * Get the description of this column.
	 * 
	 * @return	Its description. <i>MAY be NULL</i>
	 */
	public final String getDescription(){
		return description;
	}

	/**
	 * Set the description of this column.
	 * 
	 * @param description	Its new description. <i>MAY be NULL</i>
	 */
	public final void setDescription(String description){
		this.description = description;
	}

	/**
	 * Get the unit of the column's values.
	 * 
	 * @return	Its unit. <i>MAY be NULL</i>
	 */
	public final String getUnit(){
		return unit;
	}

	/**
	 * Set the unit of the column's values.
	 * 
	 * @param unit	Its new unit. <i>MAY be NULL</i>
	 */
	public final void setUnit(String unit){
		this.unit = unit;
	}

	/**
	 * Get the UCD describing the scientific content of this column.
	 * 
	 * @return	Its UCD. <i>MAY be NULL</i>
	 */
	public final String getUcd(){
		return ucd;
	}

	/**
	 * Set the UCD describing the scientific content of this column.
	 * 
	 * @param ucd	Its new UCD. <i>MAY be NULL</i>
	 */
	public final void setUcd(String ucd){
		this.ucd = ucd;
	}

	/**
	 * Get the UType associating this column with a data-model.
	 * 
	 * @return	Its UType. <i>MAY be NULL</i>
	 */
	public final String getUtype(){
		return utype;
	}

	/**
	 * Set the UType associating this column with a data-model.
	 * 
	 * @param utype	Its new UType. <i>MAY be NULL</i>
	 */
	public final void setUtype(String utype){
		this.utype = utype;
	}

	/**
	 * Get the type of the column's values.
	 * 
	 * @return	Its datatype. <i>CAN'T be NULL</i>
	 */
	@Override
	public final DBType getDatatype(){
		return datatype;
	}

	/**
	 * <p>Set the type of the column's values.</p>
	 * 
	 * <p><i>Note:
	 * 	If the given type is NULL, an {@link DBDatatype#UNKNOWN UNKNOWN} type will be set instead.
	 * </i></p>
	 * 
	 * @param type	Its new datatype.
	 */
	public final void setDatatype(final DBType type){
		if (type != null)
			datatype = type;
		else
			datatype = new DBType(DBDatatype.UNKNOWN);
	}

	/**
	 * Tell whether this column is one of those returned by default.
	 * 
	 * @return	<i>true</i> if this column should be returned by default, <i>false</i> otherwise.
	 */
	public final boolean isPrincipal(){
		return principal;
	}

	/**
	 * Set whether this column should be one of those returned by default.
	 * 
	 * @param  principal	<i>true</i> if this column should be returned by default, <i>false</i> otherwise.
	 */
	public final void setPrincipal(boolean principal){
		this.principal = principal;
	}

	/**
	 * Tell whether this column is indexed.
	 * 
	 * @return	<i>true</i> if this column is indexed, <i>false</i> otherwise.
	 */
	public final boolean isIndexed(){
		return indexed;
	}

	/**
	 * Set whether this column is indexed or not.
	 * 
	 * @param  indexed	<i>true</i> if this column is indexed, <i>false</i> otherwise.
	 */
	public final void setIndexed(boolean indexed){
		this.indexed = indexed;
	}

	/**
	 * Tell whether this column is nullable.
	 * 
	 * @return	<i>true</i> if this column is nullable, <i>false</i> otherwise.
	 * 
	 * @since 2.0
	 */
	public final boolean isNullable(){
		return nullable;
	}

	/**
	 * Set whether this column is nullable or not.
	 * 
	 * @param  nullable	<i>true</i> if this column is nullable, <i>false</i> otherwise.
	 * 
	 * @since 2.0
	 */
	public final void setNullable(boolean nullable){
		this.nullable = nullable;
	}

	/**
	 * Tell whether this column is defined by a standard.
	 * 
	 * @return	<i>true</i> if this column is defined by a standard, <i>false</i> otherwise.
	 */
	public final boolean isStd(){
		return std;
	}

	/**
	 * Set whether this column is defined by a standard.
	 * 
	 * @param  std	<i>true</i> if this column is defined by a standard, <i>false</i> otherwise.
	 */
	public final void setStd(boolean std){
		this.std = std;
	}

	/**
	 * Get the ordering index of this column inside its table.
	 * 
	 * @return	Its ordering index.
	 * 
	 * @since 2.1
	 */
	public final int getIndex(){
		return index;
	}

	/**
	 * Set the ordering index of this column inside its table.
	 * 
	 * @param columnIndex	Its new ordering index.
	 * 
	 * @since 2.1
	 */
	public final void setIndex(int columnIndex){
		this.index = columnIndex;
	}

	/**
	 * Get the used coordinate system.
	 * 
	 * @return	Its coordinate system.
	 * 
	 * @since 2.1
	 */
	public final TAPCoosys getCoosys(){
		return coosys;
	}

	/**
	 * Set the the coordinate system to use.
	 * 
	 * @param newCoosys	Its new coordinate system.
	 * 
	 * @since 2.1
	 */
	public final void setCoosys(final TAPCoosys newCoosys){
		this.coosys = newCoosys;
	}

	/**
	 * <p>Get the other (piece of) information associated with this column.</p>
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
	 * Set the other (piece of) information associated with this column.
	 * 
	 * @param data	Another information about this column. <i>MAY be NULL</i>
	 */
	public void setOtherData(Object data){
		otherData = data;
	}

	/**
	 * <p>Let add a foreign key in which this column is a source (= which is targeting another column).</p>
	 * 
	 * <p><i>Note:
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPTable}
	 * 	that owns this column or that is part of the foreign key.
	 * </i></p>
	 * 
	 * @param key	A foreign key.
	 */
	protected void addTarget(TAPForeignKey key){
		if (key != null)
			lstTargets.add(key);
	}

	/**
	 * Get the number of times this column is targeting another column.
	 * 
	 * @return	How many this column is source in a foreign key.
	 */
	public int getNbTargets(){
		return lstTargets.size();
	}

	/**
	 * Get the list of foreign keys in which this column is a source (= is targeting another column).
	 * 
	 * @return	List of foreign keys in which this column is a source.
	 */
	public Iterator<TAPForeignKey> getTargets(){
		return lstTargets.iterator();
	}

	/**
	 * <p>Remove the fact that this column is a source (= is targeting another column)
	 * in the given foreign key.</p>
	 * 
	 * <p><i>Note:
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPTable}
	 * 	that owns this column or that is part of the foreign key.
	 * </i></p>
	 * 
	 * @param key	Foreign key in which this column was targeting another column.
	 */
	protected void removeTarget(TAPForeignKey key){
		if (key != null)
			lstTargets.remove(key);
	}

	/**
	 * <p>Remove the fact that this column is a source (= is targeting another column)
	 * in any foreign key in which it was.</p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPTable}
	 * 	that owns this column or that is part of the foreign key.
	 * </i></p>
	 */
	protected void removeAllTargets(){
		lstTargets.clear();
	}

	/**
	 * <p>Let add a foreign key in which this column is a target (= which is targeted by another column).</p>
	 * 
	 * <p><i>Note:
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPTable}
	 * 	that owns this column or that is part of the foreign key.
	 * </i></p>
	 * 
	 * @param key	A foreign key.
	 */
	protected void addSource(TAPForeignKey key){
		if (key != null)
			lstSources.add(key);
	}

	/**
	 * Get the number of times this column is targeted by another column.
	 * 
	 * @return	How many this column is target in a foreign key.
	 */
	public int getNbSources(){
		return lstSources.size();
	}

	/**
	 * Get the list of foreign keys in which this column is a target (= is targeted another column).
	 * 
	 * @return	List of foreign keys in which this column is a target.
	 */
	public Iterator<TAPForeignKey> getSources(){
		return lstSources.iterator();
	}

	/**
	 * <p>Remove the fact that this column is a target (= is targeted by another column)
	 * in the given foreign key.</p>
	 * 
	 * <p><i>Note:
	 * 	Nothing is done if the given value is NULL.
	 * </i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPTable}
	 * 	that owns this column or that is part of the foreign key.
	 * </i></p>
	 * 
	 * @param key	Foreign key in which this column was targeted by another column.
	 */
	protected void removeSource(TAPForeignKey key){
		lstSources.remove(key);
	}

	/**
	 * <p>Remove the fact that this column is a target (= is targeted by another column)
	 * in any foreign key in which it was.</p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	For consistency reasons, this function SHOULD be called only by the {@link TAPTable}
	 * 	that owns this column or that is part of the foreign key.
	 * </i></p>
	 */
	protected void removeAllSources(){
		lstSources.clear();
	}

	/**
	 * <p><i><b>Warning:</b>
	 * 	Since the type of the other data is not known, the copy of its value
	 * 	can not be done properly. So, this column and its copy will share the same other data object.
	 * 	If it is also needed to make a deep copy of this other data object, this function MUST be
	 * 	overridden.
	 * </i></b>
	 * 
	 * @see adql.db.DBColumn#copy(java.lang.String, java.lang.String, adql.db.DBTable)
	 */
	@Override
	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable){
		TAPColumn copy = new TAPColumn("\""+((adqlName == null) ? this.adqlName : adqlName)+"\"", datatype, description, unit, ucd, utype);
		copy.setDBName((dbName == null) ? this.getDBName() : dbName);
		copy.setTable(dbTable);

		copy.setIndexed(indexed);
		copy.setPrincipal(principal);
		copy.setStd(std);
		copy.setOtherData(otherData);

		return copy;
	}

	/**
	 * <p>Provide a deep copy (included the other data) of this column.</p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	Since the type of the other data is not known, the copy of its value
	 * 	can not be done properly. So, this column and its copy will share the same other data object.
	 * 	If it is also needed to make a deep copy of this other data object, this function MUST be
	 * 	overridden.
	 * </i></b>
	 * 
	 * @return	The deep copy of this column.
	 */
	public DBColumn copy(){
		TAPColumn copy = new TAPColumn(adqlName, datatype, description, unit, ucd, utype);
		copy.setDBName(dbName);
		copy.setTable(table);
		copy.setIndexed(indexed);
		copy.setPrincipal(principal);
		copy.setStd(std);
		copy.setOtherData(otherData);
		return copy;
	}

	@Override
	public boolean equals(Object obj){
		if (!(obj instanceof TAPColumn))
			return false;

		TAPColumn col = (TAPColumn)obj;
		return col.getTable().equals(table) && col.getADQLName().equals(adqlName);
	}

	@Override
	public String toString(){
		return ((table != null) ? (((table.getADQLSchemaName() != null) ? table.getADQLSchemaName() : "") + table.getADQLName() + ".") : "") + getADQLName();
	}

}
