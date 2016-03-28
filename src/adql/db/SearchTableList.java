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
 * Copyright 2012,2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Collection;

import adql.query.IdentifierField;
import adql.query.from.ADQLTable;
import cds.utils.TextualSearchList;

/**
 * <p>A list of {@link DBTable} elements ordered by their ADQL name in an ascending manner.</p>
 * 
 * <p>
 * 	In addition to an ADQL name, {@link DBTable} elements can be searched by specifying their schema and catalog.
 * 	These last information will be used only if the ADQL table name is ambiguous, otherwise all matching elements are returned.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (08/2015)
 */
public class SearchTableList extends TextualSearchList<DBTable> implements SearchTableApi{
	private static final long serialVersionUID = 1L;

	/** Indicates whether multiple occurrences are allowed. */
	private boolean distinct = false;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Void constructor.
	 */
	public SearchTableList(){
		super(new DBTableKeyExtractor());
	}

	/**
	 * Constructor by copy: all the elements of the given collection of {@link DBTable} are copied ordered into this list.
	 * 
	 * @param collection	Collection of {@link DBTable} to copy.
	 */
	public SearchTableList(final Collection<? extends DBTable> collection){
		super(collection, new DBTableKeyExtractor());
	}

	/**
	 * Constructor with the initial capacity.
	 * 
	 * @param initialCapacity	Initial capacity of this list.
	 */
	public SearchTableList(final int initialCapacity){
		super(initialCapacity, new DBTableKeyExtractor());
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	/**
	 * Tells whether multiple occurrences are allowed.
	 * 
	 * @return <i>true</i> means that multiple occurrences are allowed, <i>false</i> otherwise.
	 */
	public final boolean isDistinct(){
		return distinct;
	}

	/**
	 * Lets indicating that multiple occurrences are allowed.
	 * 
	 * @param distinct <i>true</i> means that multiple occurrences are allowed, <i>false</i> otherwise.
	 */
	public final void setDistinct(final boolean distinct){
		this.distinct = distinct;
	}

	/* ************** */
	/* SEARCH METHODS */
	/* ************** */
	/**
	 * Searches all {@link DBTable} elements which has the given name (case insensitive).
	 * 
	 * @param tableName	ADQL name of {@link DBTable} to search for.
	 * 
	 * @return			The corresponding {@link DBTable} elements.
	 * 
	 * @see TextualSearchList#get(String)
	 */
	public ArrayList<DBTable> search(final String tableName){
		return get(tableName);
	}

	/**
	 * Searches all {@link DBTable} elements which have the given catalog, schema, and table name (case insensitive).
	 * 
	 * @param catalog	Catalog name.
	 * @param schema	Schema name.
	 * @param table		Table name.
	 * 
	 * @return			The list of all matching {@link DBTable} elements.
	 * 
	 * @see #search(String, String, String, byte)
	 */
	public final ArrayList<DBTable> search(final String catalog, final String schema, final String table){
		return search(catalog, schema, table, (byte)0);
	}

	/**
	 * Searches all {@link DBTable} elements corresponding to the given {@link ADQLTable} (case insensitive).
	 * 
	 * @param table	An {@link ADQLTable}.
	 * 
	 * @return		The list of all corresponding {@link DBTable} elements.
	 * 
	 * @see #search(String, String, String, byte)
	 */
	public ArrayList<DBTable> search(final ADQLTable table){
		return search(table.getCatalogName(), table.getSchemaName(), table.getTableName(), table.getCaseSensitive());
	}

	/**
	 * Searches all {@link DBTable} elements which have the given catalog, schema, and table name, with the specified case sensitivity.
	 * 
	 * @param catalog			Catalog name.
	 * @param schema			Schema name.
	 * @param table				Table name.
	 * @param caseSensitivity	Case sensitivity for each table parts (one bit by part ; 0=sensitive,1=insensitive ; see {@link IdentifierField} for more details).
	 * 
	 * @return					The list of all matching {@link DBTable} elements.
	 * 
	 * @see IdentifierField
	 */
	public ArrayList<DBTable> search(final String catalog, final String schema, final String table, final byte caseSensitivity){
		ArrayList<DBTable> tmpResult = get(table, IdentifierField.TABLE.isCaseSensitive(caseSensitivity));

		if (schema != null){
			ArrayList<DBTable> result = new ArrayList<DBTable>();

			for(DBTable match : tmpResult){
				// No schema name (<=> no schema), then this table can not be a good match:
				if (match.getADQLSchemaName() == null)
					continue;
				if (IdentifierField.SCHEMA.isCaseSensitive(caseSensitivity)){
					if (!match.getADQLSchemaName().equals(schema))
						continue;
				}else{
					if (!match.getADQLSchemaName().equalsIgnoreCase(schema))
						continue;
				}

				if (catalog != null){
					// No catalog name (<=> no catalog), then this table can not be a good match:
					if (match.getADQLCatalogName() == null)
						continue;
					if (IdentifierField.CATALOG.isCaseSensitive(caseSensitivity)){
						if (!match.getADQLCatalogName().equals(catalog))
							continue;
					}else{
						if (!match.getADQLCatalogName().equalsIgnoreCase(catalog))
							continue;
					}
				}

				result.add(match);
			}
			return result;

		}else
			return tmpResult;
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	@Override
	public boolean add(final DBTable item){
		if (distinct && contains(item))
			return false;
		else
			return super.add(item);
	}

	/**
	 * Lets extracting a key to associate with a given {@link DBTable} instance.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 09/2011
	 */
	private static class DBTableKeyExtractor implements KeyExtractor<DBTable> {
		@Override
		public String getKey(DBTable obj){
			return obj.getADQLName();
		}
	}

}
