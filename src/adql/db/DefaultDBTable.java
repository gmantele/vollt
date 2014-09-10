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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Default implementation of {@link DBTable}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.3 (09/2014)
 */
public class DefaultDBTable implements DBTable {

	protected String dbCatalogName;
	protected String dbSchemaName;
	protected String dbName;

	protected String adqlCatalogName = null;
	protected String adqlSchemaName = null;
	protected String adqlName = null;

	protected HashMap<String,DBColumn> columns = new HashMap<String,DBColumn>();

	/**
	 * <p>Builds a default {@link DBTable} with the given DB name.</p>
	 * 
	 * <p>With this constructor: ADQL name = DB name.</p>
	 * 
	 * <p><i><u>Note:</u> The table name can be prefixed by a schema and a catalog: t1 or schema1.t1 or cat1.schema1.t2</i></p>
	 * 
	 * @param dbName	Database name (it will be also used as ADQL table name).
	 * 
	 * @see #DefaultDBTable(String, String)
	 */
	public DefaultDBTable(final String dbName){
		this(dbName, null);
	}

	/**
	 * <p>Builds a default {@link DBTable} with the given DB and ADQL names.</p>
	 * 
	 * <p><i><u>Note:</u> The table names can be prefixed by a schema and a catalog: t1 or schema1.t1 or cat1.schema1.t2</i></p>
	 * 
	 * @param dbName	Database name.
	 * @param adqlName	Name used in ADQL queries.
	 */
	public DefaultDBTable(final String dbName, final String adqlName){
		// DB names:
		String[] names = splitTableName(dbName);
		if (names[2] == null || names[2].length() == 0)
			throw new NullPointerException("Missing DB name !");
		else
			this.dbName = names[2];
		this.dbSchemaName = names[1];
		this.dbCatalogName = names[0];

		// ADQL names:
		names = splitTableName(adqlName);
		if (names[2] == null || names[2].length() == 0){
			this.adqlName = this.dbName;
			this.adqlSchemaName = this.dbSchemaName;
			this.adqlCatalogName = this.dbCatalogName;
		}else{
			this.adqlName = names[2];
			this.adqlSchemaName = names[1];
			this.adqlCatalogName = names[0];
		}
	}

	/**
	 * Builds default {@link DBTable} with a DB catalog, schema and table names.
	 * 
	 * @param dbCatName		Database catalog name (it will be also used as ADQL catalog name).
	 * @param dbSchemName	Database schema name (it will be also used as ADQL schema name).
	 * @param dbName		Database table name (it will be also used as ADQL table name).
	 * 
	 * @see #DefaultDBTable(String, String, String, String, String, String)
	 */
	public DefaultDBTable(final String dbCatName, final String dbSchemName, final String dbName){
		this(dbCatName, null, dbSchemName, null, dbName, null);
	}

	/**
	 * Builds default {@link DBTable} with the DB and ADQL names for the catalog, schema and table.
	 * 
	 * @param dbCatName		Database catalog name.
	 * @param adqlCatName	Catalog name used in ADQL queries.
	 * @param dbSchemName	Database schema name.
	 * @param adqlSchemName	Schema name used in ADQL queries.
	 * @param dbName		Database table name.
	 * @param adqlName		Table name used in ADQL queries.
	 */
	public DefaultDBTable(final String dbCatName, final String adqlCatName, final String dbSchemName, final String adqlSchemName, final String dbName, final String adqlName){

		if (dbName == null || dbName.length() == 0)
			throw new NullPointerException("Missing DB name !");

		this.dbName = dbName;
		this.adqlName = adqlName;

		dbSchemaName = dbSchemName;
		adqlSchemaName = adqlSchemName;

		dbCatalogName = dbCatName;
		adqlCatalogName = adqlCatName;
	}

	@Override
	public final String getDBName(){
		return dbName;
	}

	@Override
	public final String getDBSchemaName(){
		return dbSchemaName;
	}

	@Override
	public final String getDBCatalogName(){
		return dbCatalogName;
	}

	@Override
	public final String getADQLName(){
		return adqlName;
	}

	public void setADQLName(final String name){
		adqlName = (name != null) ? name : dbName;
	}

	@Override
	public final String getADQLSchemaName(){
		return adqlSchemaName;
	}

	public void setADQLSchemaName(final String name){
		adqlSchemaName = (name != null) ? name : dbSchemaName;
	}

	@Override
	public final String getADQLCatalogName(){
		return adqlCatalogName;
	}

	public void setADQLCatalogName(final String name){
		adqlName = (name != null) ? null : dbName;
	}

	/**
	 * <p>Case sensitive !</p>
	 * <p>Research optimized for researches by ADQL name.</p>
	 * 
	 * @see adql.db.DBTable#getColumn(java.lang.String, boolean)
	 */
	@Override
	public DBColumn getColumn(String colName, boolean byAdqlName){
		if (byAdqlName)
			return columns.get(colName);
		else{
			for(DBColumn col : columns.values()){
				if (col.getDBName().equals(colName))
					return col;
			}
			return null;
		}
	}

	public boolean hasColumn(String colName, boolean byAdqlName){
		return (getColumn(colName, byAdqlName) != null);
	}

	@Override
	public Iterator<DBColumn> iterator(){
		return columns.values().iterator();
	}

	public void addColumn(DBColumn column){
		if (column != null)
			columns.put(column.getADQLName(), column);
	}

	public void addAllColumns(Collection<DBColumn> colList){
		if (colList != null){
			for(DBColumn column : colList)
				addColumn(column);
		}
	}

	/**
	 * Splits the given table name in 3 parts: catalog, schema, table.
	 * 
	 * @param table	The table name to split.
	 * 
	 * @return	A String array of 3 items: [0]=catalog, [1]=schema, [0]=table.
	 */
	public static final String[] splitTableName(final String table){
		String[] splitRes = new String[]{null,null,null};

		if (table == null || table.trim().length() == 0)
			return splitRes;

		String[] names = table.trim().split("\\.");
		switch(names.length){
			case 1:
				splitRes[2] = table.trim();
				break;
			case 2:
				splitRes[2] = names[1].trim();
				splitRes[1] = names[0].trim();
				break;
			case 3:
				splitRes[2] = names[2].trim();
				splitRes[1] = names[1].trim();
				splitRes[0] = names[0].trim();
				break;
			default:
				splitRes[2] = names[names.length - 1].trim();
				splitRes[1] = names[names.length - 2].trim();
				StringBuffer buff = new StringBuffer(names[0].trim());
				for(int i = 1; i < names.length - 2; i++)
					buff.append('.').append(names[i].trim());
				splitRes[0] = buff.toString();
		}

		return splitRes;
	}

	/**
	 * <p>Join the last 3 items of the given string array with a dot ('.').
	 * These three parts should be: [0]=catalog name, [1]=schema name, [2]=table name.</p>
	 * 
	 * <p>
	 * 	If the array contains less than 3 items, all the given items will be though joined.
	 * 	However, if it contains more than 3 items, only the three last items will be.
	 * </p>
	 * 
	 * <p>A null item will be written as an empty string (string of length 0 ; "").</p>
	 * 
	 * <p>
	 * 	In the case the first and the third items are not null, but the second is null, the final string will contain in the middle two dots.
	 * 	Example: if the array is {"cat", NULL, "table"}, then the joined string will be: "cat..table".
	 * </p>
	 * 
	 * @param nameParts	String items to join.
	 * 
	 * @return	A string joining the 3 last string items of the given array,
	 *        	or an empty string if the given array is NULL.
	 * 
	 * @since 1.3
	 */
	public static final String joinTableName(final String[] nameParts){
		if (nameParts == null)
			return "";

		StringBuffer str = new StringBuffer();
		boolean empty = true;
		for(int i = (nameParts.length <= 3) ? 0 : (nameParts.length - 3); i < nameParts.length; i++){
			if (!empty)
				str.append('.');

			String part = (nameParts[i] == null) ? null : nameParts[i].trim();
			if (part != null && part.length() > 0){
				str.append(part);
				empty = false;
			}
		}
		return str.toString();
	}

	@Override
	public DBTable copy(String dbName, String adqlName){
		dbName = (dbName == null) ? joinTableName(new String[]{dbCatalogName,dbSchemaName,this.dbName}) : dbName;
		adqlName = (adqlName == null) ? joinTableName(new String[]{adqlCatalogName,adqlSchemaName,this.adqlName}) : adqlName;
		DefaultDBTable copy = new DefaultDBTable(dbName, adqlName);
		for(DBColumn col : this){
			if (col instanceof DBCommonColumn)
				copy.addColumn(new DBCommonColumn((DBCommonColumn)col, col.getDBName(), col.getADQLName()));
			else
				copy.addColumn(col.copy(col.getDBName(), col.getADQLName(), copy));
		}
		return copy;
	}
}
