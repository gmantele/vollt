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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TAPSchema implements Iterable<TAPTable> {

	private final String adqlName;

	private String dbName = null;

	private String description = null;

	private String utype = null;

	protected Object otherData = null;

	protected final Map<String,TAPTable> tables;

	public TAPSchema(String schemaName){
		adqlName = schemaName;
		dbName = adqlName;
		tables = new HashMap<String,TAPTable>();
	}

	public TAPSchema(String schemaName, String description){
		this(schemaName, description, null);
	}

	public TAPSchema(String schemaName, String description, String utype){
		this(schemaName);
		this.description = description;
		this.utype = utype;
	}

	/**
	 * @return The name.
	 */
	public final String getName(){
		return getADQLName();
	}

	public final String getADQLName(){
		return adqlName;
	}

	public final String getDBName(){
		return dbName;
	}

	public final void setDBName(String name){
		name = (name != null) ? name.trim() : name;
		dbName = (name == null || name.length() == 0) ? adqlName : name;
	}

	/**
	 * @return The description.
	 */
	public final String getDescription(){
		return description;
	}

	/**
	 * @param description The description to set.
	 */
	public final void setDescription(String description){
		this.description = description;
	}

	/**
	 * @return The utype.
	 */
	public final String getUtype(){
		return utype;
	}

	/**
	 * @param utype The utype to set.
	 */
	public final void setUtype(String utype){
		this.utype = utype;
	}

	public Object getOtherData(){
		return otherData;
	}

	public void setOtherData(Object data){
		otherData = data;
	}

	public final void addTable(TAPTable newTable){
		if (newTable != null && newTable.getName() != null){
			tables.put(newTable.getName(), newTable);
			newTable.setSchema(this);
		}
	}

	public TAPTable addTable(String tableName){
		if (tableName == null)
			return null;

		TAPTable t = new TAPTable(tableName);
		addTable(t);
		return t;
	}

	public TAPTable addTable(String tableName, String tableType, String description, String utype){
		if (tableName == null)
			return null;

		TAPTable t = new TAPTable(tableName, tableType, description, utype);
		addTable(t);
		return t;
	}

	public final boolean hasTable(String tableName){
		if (tableName == null)
			return false;
		else
			return tables.containsKey(tableName);
	}

	public final TAPTable getTable(String tableName){
		if (tableName == null)
			return null;
		else
			return tables.get(tableName);
	}

	public final int getNbTables(){
		return tables.size();
	}

	public final boolean isEmpty(){
		return tables.isEmpty();
	}

	public final TAPTable removeTable(String tableName){
		if (tableName == null)
			return null;

		TAPTable removedTable = tables.remove(tableName);
		if (removedTable != null){
			removedTable.setSchema(null);
			removedTable.removeAllForeignKeys();
		}
		return removedTable;
	}

	public final void removeAllTables(){
		Iterator<Map.Entry<String,TAPTable>> it = tables.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String,TAPTable> entry = it.next();
			it.remove();
			entry.getValue().setSchema(null);
			entry.getValue().removeAllForeignKeys();
		}
	}

	@Override
	public Iterator<TAPTable> iterator(){
		return tables.values().iterator();
	}

}
