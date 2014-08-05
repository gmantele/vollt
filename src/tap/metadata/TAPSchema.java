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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.awt.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tap.metadata.TAPTable.TableType;

/**
 * <p>Represent a schema as described by the IVOA standard in the TAP protocol definition.</p>
 * 
 * <p>
 * 	This object representation has exactly the same fields as the column of the table TAP_SCHEMA.schemas.
 * 	But it also provides a way to add other data. For instance, if information not listed in the standard
 * 	may be stored here, they can be using the function {@link #setOtherData(Object)}. This object can be
 * 	a single value (integer, string, ...), but also a {@link Map}, {@link List}, etc...
 * </p>
 * 
 * <p><i>Note:
 * 	On the contrary to {@link TAPColumn} and {@link TAPTable}, a {@link TAPSchema} object MAY have no DB name.
 * 	But by default, at the creation the DB name is the ADQL name. Once created, it is possible to set the DB
 * 	name with {@link #setDBName(String)}. This DB name MAY be qualified, BUT MUST BE without double quotes.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (08/2014)
 */
public class TAPSchema implements Iterable<TAPTable> {

	/** Name that this schema MUST have in ADQL queries. */
	private final String adqlName;

	/** Name that this schema have in the database.
	 * <i>Note: It MAY be NULL. By default, it is the ADQL name.</i> */
	private String dbName = null;

	/** Description of this schema.
	 * <i>Note: Standard TAP schema field ; MAY be NULL.</i> */
	private String description = null;

	/** UType describing the scientific content of this schema.
	 * <i>Note: Standard TAP schema field ; MAY be NULL.</i> */
	private String utype = null;

	/** Let add some information in addition of the ones of the TAP protocol.
	 * <i>Note: This object can be anything: an {@link Integer}, a {@link String}, a {@link Map}, a {@link List}, ...
	 * Its content is totally free and never used or checked.</i> */
	protected Object otherData = null;

	/** List all tables contained inside this schema. */
	protected final Map<String,TAPTable> tables;

	/**
	 * <p>Build a {@link TAPSchema} instance with the given ADQL name.</p>
	 * 
	 * <p><i>Note:
	 * 	The DB name is set by default with the ADQL name. To set the DB name,
	 * 	you MUST call then {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given ADQL name is prefixed (= it has some text separated by a '.' before the schema name),
	 * 	this prefix will be removed. Only the part after the '.' character will be kept.
	 * </i></p>
	 * 
	 * @param schemaName	Name that this schema MUST have in ADQL queries. <i>CAN'T be NULL ; this name can never be changed after.</i>
	 */
	public TAPSchema(String schemaName){
		if (schemaName == null || schemaName.trim().length() == 0)
			throw new NullPointerException("Missing schema name !");
		int indPrefix = schemaName.lastIndexOf('.');
		adqlName = (indPrefix >= 0) ? schemaName.substring(indPrefix + 1).trim() : schemaName.trim();
		dbName = adqlName;
		tables = new HashMap<String,TAPTable>();
	}

	/**
	 * <p>Build a {@link TAPSchema} instance with the given ADQL name and description.</p>
	 * 
	 * <p><i>Note:
	 * 	The DB name is set by default with the ADQL name. To set the DB name,
	 * 	you MUST call then {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given ADQL name is prefixed (= it has some text separated by a '.' before the schema name),
	 * 	this prefix will be removed. Only the part after the '.' character will be kept.
	 * </i></p>
	 * 
	 * @param schemaName	Name that this schema MUST have in ADQL queries. <i>CAN'T be NULL ; this name can never be changed after.</i>
	 * @param description	Description of this schema. <i>MAY be NULL</i>
	 */
	public TAPSchema(String schemaName, String description){
		this(schemaName, description, null);
	}

	/**
	 * <p>Build a {@link TAPSchema} instance with the given ADQL name, description and UType.</p>
	 * 
	 * <p><i>Note:
	 * 	The DB name is set by default with the ADQL name. To set the DB name,
	 * 	you MUST call then {@link #setDBName(String)}.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the given ADQL name is prefixed (= it has some text separated by a '.' before the schema name),
	 * 	this prefix will be removed. Only the part after the '.' character will be kept.
	 * </i></p>
	 * 
	 * @param schemaName	Name that this schema MUST have in ADQL queries. <i>CAN'T be NULL ; this name can never be changed after.</i>
	 * @param description	Description of this schema. <i>MAY be NULL</i>
	 * @param utype			UType associating this schema with a data-model. <i>MAY be NULL</i>
	 */
	public TAPSchema(String schemaName, String description, String utype){
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
	public final String getName(){
		return getADQLName();
	}

	/**
	 * Get the name this schema MUST have in ADQL queries.
	 * 
	 * @return	Its ADQL name. <i>CAN'T be NULL</i>
	 */
	public final String getADQLName(){
		return adqlName;
	}

	/**
	 * Get the name this schema MUST have in the database.
	 * 
	 * @return	Its DB name. <i>MAY be NULL</i>
	 */
	public final String getDBName(){
		return dbName;
	}

	/**
	 * Set the name this schema MUST have in the database.
	 * 
	 * @param name	Its new DB name. <i>MAY be NULL</i>
	 */
	public final void setDBName(String name){
		name = (name != null) ? name.trim() : name;
		dbName = name;
	}

	/**
	 * Get the description of this schema.
	 * 
	 * @return	Its description. <i>MAY be NULL</i>
	 */
	public final String getDescription(){
		return description;
	}

	/**
	 * Set the description of this schema.
	 * 
	 * @param description	Its new description. <i>MAY be NULL</i>
	 */
	public final void setDescription(String description){
		this.description = description;
	}

	/**
	 * Get the UType associating this schema with a data-model.
	 * 
	 * @return	Its UType. <i>MAY be NULL</i>
	 */
	public final String getUtype(){
		return utype;
	}

	/**
	 * Set the UType associating this schema with a data-model.
	 * 
	 * @param utype	Its new UType. <i>MAY be NULL</i>
	 */
	public final void setUtype(String utype){
		this.utype = utype;
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
	public Object getOtherData(){
		return otherData;
	}

	/**
	 * Set the other (piece of) information associated with this schema.
	 * 
	 * @param data	Another information about this schema. <i>MAY be NULL</i>
	 */
	public void setOtherData(Object data){
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
	public final void addTable(TAPTable newTable){
		if (newTable != null && newTable.getADQLName() != null){
			tables.put(newTable.getADQLName(), newTable);
			newTable.setSchema(this);
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
	public TAPTable addTable(String tableName){
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
	 * @param unit			Unit of the new table's values. <i>MAY be NULL</i>
	 * @param ucd			UCD describing the scientific content of the new column. <i>MAY be NULL</i>
	 * @param utype			UType associating the new column with a data-model. <i>MAY be NULL</i>
	 * 
	 * @return	The created and added {@link TAPTable} object,
	 *        	or NULL if the given name is NULL or an empty string.
	 * 
	 * @see TAPTable#TAPTable(String, TableType, String, String, String, String)
	 * @see #addTable(TAPSchema)
	 */
	public TAPTable addTable(String tableName, TableType tableType, String description, String utype){
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
	public final boolean hasTable(String tableName){
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
	public final TAPTable getTable(String tableName){
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
	public final int getNbTables(){
		return tables.size();
	}

	/**
	 * Tell whether this schema contains no table.
	 * 
	 * @return	<i>true</i> if this schema contains no table,
	 *        	<i>false</i> if it has at least one table.
	 */
	public final boolean isEmpty(){
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
	public final TAPTable removeTable(String tableName){
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
	public final void removeAllTables(){
		Iterator<Map.Entry<String,TAPTable>> it = tables.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String,TAPTable> entry = it.next();
			it.remove();
			entry.getValue().setSchema(null);
		}
	}

	@Override
	public Iterator<TAPTable> iterator(){
		return tables.values().iterator();
	}

}
