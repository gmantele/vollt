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

public class TAPForeignKey implements Iterable<Map.Entry<String,String>> {

	private final String keyId;

	private final TAPTable fromTable;

	private final TAPTable targetTable;

	private String description = null;

	private String utype = null;

	protected final Map<String,String> columnsAssoc;

	protected Object otherData = null;

	public TAPForeignKey(String keyId, TAPTable fromTable, TAPTable targetTable, Map<String,String> columns){
		this.keyId = keyId;
		this.fromTable = fromTable;
		this.targetTable = targetTable;
		columnsAssoc = new HashMap<String,String>(columns);
	}

	public TAPForeignKey(String keyId, TAPTable fromTable, TAPTable targetTable, Map<String,String> columns, String description, String utype){
		this(keyId, fromTable, targetTable, columns);
		this.description = description;
		this.utype = utype;
	}

	/**
	 * @return The keyId.
	 */
	public final String getKeyId(){
		return keyId;
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

	/**
	 * @return The fromTable.
	 */
	public final TAPTable getFromTable(){
		return fromTable;
	}

	/**
	 * @return The targetTable.
	 */
	public final TAPTable getTargetTable(){
		return targetTable;
	}

	public final boolean isSource(String fromColumnName){
		return columnsAssoc.containsKey(fromColumnName);
	}

	public final String getTarget(String fromColumnName){
		return columnsAssoc.get(fromColumnName);
	}

	public final boolean isTarget(String targetColumnName){
		return columnsAssoc.values().contains(targetColumnName);
	}

	public final String getSource(String targetColumnName){
		for(Map.Entry<String,String> relation : this)
			if (relation.getValue().equals(targetColumnName))
				return relation.getKey();
		return null;
	}

	public final int getRelationType(String columnName){
		if (isSource(columnName))
			return -1;
		else if (isTarget(columnName))
			return 1;
		else
			return 0;
	}

	public final boolean isEmpty(){
		return columnsAssoc.isEmpty();
	}

	public final int getNbRelations(){
		return columnsAssoc.size();
	}

	@Override
	public Iterator<Map.Entry<String,String>> iterator(){
		return columnsAssoc.entrySet().iterator();
	}

	@Override
	public boolean equals(Object obj){
		return (obj instanceof TAPForeignKey) && ((TAPForeignKey)obj).keyId.equals(keyId);
	}

	@Override
	public String toString(){
		return keyId;
	}

}
