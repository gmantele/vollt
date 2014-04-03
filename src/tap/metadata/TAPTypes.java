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
import java.util.Map.Entry;

/**
 * <p>
 * 	Gathers all types used by a TAP service and described in the IVOA document for TAP.
 * 	This class lets "translating" a DB type into a VOTable field type and vice-versa.
 * 	You can also add some DB type aliases, that's to say other other names for the existing DB types:
 * 	smallint, integer, bigint, real, double, binary, varbinary, char, varchar, blob, clob, timestamp, point, region.
 * 	For instance: TEXT &lt;-&gt; VARCHAR.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 11/2011
 * 
 * @see VotType
 */
public final class TAPTypes {

	private static final Map<String, VotType> dbTypes;
	private static final Map<String, String> dbTypeAliases;
	private static final Map<VotType, String> votTypes;

	public static final String SMALLINT = "SMALLINT";
	public static final String INTEGER = "INTEGER";
	public static final String BIGINT = "BIGINT";
	public static final String REAL = "REAL";
	public static final String DOUBLE = "DOUBLE";
	public static final String BINARY = "BINARY";
	public static final String VARBINARY = "VARBINARY";
	public static final String CHAR = "CHAR";
	public static final String VARCHAR = "VARCHAR";
	public static final String BLOB = "BLOB";
	public static final String CLOB = "CLOB";
	public static final String TIMESTAMP = "TIMESTAMP";
	public static final String POINT = "POINT";
	public static final String REGION = "REGION";

	/** No array size. */
	public static final int NO_SIZE = -1;

	/** Means '*' (i.e. char(*)). */
	public static final int STAR_SIZE = -12345;

	static {
		dbTypes = new HashMap<String, VotType>(14);
		votTypes = new HashMap<VotType, String>(7);

		VotType type = new VotType("short", 1, null);
		dbTypes.put(SMALLINT, type);
		votTypes.put(type, SMALLINT);

		type = new VotType("int", 1, null);
		dbTypes.put(INTEGER, type);
		votTypes.put(type, INTEGER);

		type = new VotType("long", 1, null);
		dbTypes.put(BIGINT, type);
		votTypes.put(type, BIGINT);

		type = new VotType("float", 1, null);
		dbTypes.put(REAL, type);
		votTypes.put(type, REAL);

		type = new VotType("double", 1, null);
		dbTypes.put(DOUBLE, type);
		votTypes.put(type, DOUBLE);

		dbTypes.put(BINARY, new VotType("unsignedByte", 1, null));

		type = new VotType("unsignedByte", STAR_SIZE, null);
		dbTypes.put(VARBINARY, type);
		votTypes.put(type, VARBINARY);

		dbTypes.put(CHAR, new VotType("char", 1, null));

		type = new VotType("char", STAR_SIZE, null);
		dbTypes.put(VARCHAR, type);
		votTypes.put(type, VARCHAR);

		type = new VotType("unsignedByte", STAR_SIZE, "adql:BLOB");
		dbTypes.put(BLOB, type);
		votTypes.put(type, BLOB);

		type = new VotType("char", STAR_SIZE, "adql:CLOB");
		dbTypes.put(CLOB, type);
		votTypes.put(type, CLOB);

		type = new VotType("char", STAR_SIZE, "adql:TIMESTAMP");
		dbTypes.put(TIMESTAMP, type);
		votTypes.put(type, TIMESTAMP);

		type = new VotType("char", STAR_SIZE, "adql:POINT");
		dbTypes.put(POINT, type);
		votTypes.put(type, POINT);

		type = new VotType("char", STAR_SIZE, "adql:REGION");
		dbTypes.put(REGION, type);
		votTypes.put(type, REGION);

		dbTypeAliases = new HashMap<String, String>(8);
		// PostgreSQL data types:
		dbTypeAliases.put("INT2", SMALLINT);
		dbTypeAliases.put("INT", INTEGER);
		dbTypeAliases.put("INT4", INTEGER);
		dbTypeAliases.put("INT8", BIGINT);
		dbTypeAliases.put("FLOAT4", REAL);
		dbTypeAliases.put("FLOAT8", DOUBLE);
		dbTypeAliases.put("TEXT", VARCHAR);
		dbTypeAliases.put("SPOINT", POINT);
	}

	/**
	 * Gets all DB types.
	 * @return	An iterator on DB type name.
	 */
	public static final Iterator<String> getDBTypes(){
		return dbTypes.keySet().iterator();
	}

	/**
	 * Gets all DB type aliases.
	 * @return	An iterator on Entry&lt;String,String&gt; whose the key is the alias and the value is its corresponding DB type.
	 */
	public static final Iterator<Entry<String,String>> getDBTypeAliases(){
		return dbTypeAliases.entrySet().iterator();
	}

	/**
	 * Gets all VOTable types.
	 * @return	An iterator on {@link VotType}.
	 */
	public static final Iterator<VotType> getVotTypes(){
		return votTypes.keySet().iterator();
	}

	/**
	 * <p>Gets the VOTable type corresponding to the given DB type (or a DB type alias).</p>
	 * <b>Important:</b>
	 * <ul>
	 * 	<li>Spaces before and after the DB type are automatically removed,</li>
	 * 	<li>The DB type is automatically formatted in UPPER-CASE,</li>
	 * 	<li>Nothing is done if the given DB type is <code>null</code> or empty.</li>
	 * </ul>
	 * 
	 * @param dbType	A DB type (ex: SMALLINT, INTEGER, VARCHAR, POINT, ...)
	 * 
	 * @return	The corresponding VOTable type or <code>null</code> if not found.
	 */
	public static final VotType getVotType(String dbType){
		if (dbType == null)
			return null;

		// Normalize the type name (upper case and with no leading and trailing spaces):
		dbType = dbType.trim().toUpperCase();
		if (dbType.length() == 0)
			return null;

		// Search the corresponding VOTable type:
		VotType votType = dbTypes.get(dbType);
		// If no match, try again considering the given type as an alias:
		if (votType == null)
			votType = dbTypes.get(dbTypeAliases.get(dbType));

		return votType;
	}

	/**
	 * <p>Gets the VOTable type (with the given arraysize) corresponding to the given DB type (or a DB type alias).</p>
	 * <b>Important:</b>
	 * <ul>
	 * 	<li>Spaces before and after the DB type are automatically removed,</li>
	 * 	<li>The DB type is automatically formatted in UPPER-CASE,</li>
	 * 	<li>Nothing is done if the given DB type is <code>null</code> or empty,</li>
	 * 	<li>The given arraysize is used only if the found VOTable type is not special (that's to say: <code>xtype</code> is <code>null</code>).</li>
	 * </ul>
	 * 
	 * @param dbType	A DB type (ex: SMALLINT, INTEGER, VARCHAR, POINT, ...)
	 * @param arraysize	Arraysize to set in the found VOTable type.
	 * 
	 * @return	The corresponding VOTable type or <code>null</code> if not found.
	 */
	public static final VotType getVotType(String dbType, int arraysize){
		VotType votType = getVotType(dbType);

		// If there is a match, set the arraysize:
		if (votType != null && votType.xtype == null && arraysize > 0)
			votType = new VotType(votType.datatype, arraysize, null);

		return votType;
	}

	/**
	 * 
	 * <p>Gets the DB type corresponding to the given DB type alias.</p>
	 * <b>Important:</b>
	 * <ul>
	 * 	<li>Spaces before and after the DB type are automatically removed,</li>
	 * 	<li>The DB type is automatically formatted in UPPER-CASE,</li>
	 * 	<li>If the given DB type is not alias but directly a DB type, it is immediately return.</li>
	 * </ul>
	 * 
	 * @param dbTypeAlias	A DB type alias.
	 * 
	 * @return		The corresponding DB type or <code>null</code> if not found.
	 */
	public static final String getDBType(String dbTypeAlias){
		if (dbTypeAlias == null)
			return null;

		// Normalize the type name:
		dbTypeAlias = dbTypeAlias.trim().toUpperCase();
		if (dbTypeAlias.length() == 0)
			return null;

		// Get the corresponding DB type:
		if (dbTypes.containsKey(dbTypeAlias))
			return dbTypeAlias;
		else
			return dbTypeAliases.get(dbTypeAlias);
	}

	/**
	 * 
	 * <p>Gets the DB type corresponding to the given VOTable field type.</p>
	 * <b>Important:</b>
	 * <ul>
	 * 	<li>The research is made only on the following fields: <code>datatype</code> and <code>xtype</code>,</li>
	 * 	<li>Case <b>insensitive</b> research.</li>
	 * </ul>
	 * 
	 * @param type	A VOTable type.
	 * 
	 * @return		The corresponding DB type or <code>null</code> if not found.
	 */
	public static final String getDBType(final VotType type){
		if (type == null)
			return null;
		return votTypes.get(type);
	}

	/**
	 * <p>Adds, replaces or removes a DB type alias.</p>
	 * <b>Important:</b>
	 * <ul>
	 * 	<li>Spaces before and after the DB type are automatically removed,</li>
	 * 	<li>The DB type is automatically formatted in UPPER-CASE,</li>
	 * 	<li>The same "normalizations" are done on the given alias (so the case sensitivity is ignored),</li>
	 * 	<li>Nothing is done if the given alias is <code>null</code> or empty,</li>
	 * 	<li>If the given DB type is <code>null</code>, the given alias is removed,</li>
	 * 	<li>Nothing is done if the given DB type (!= null) does not match with a known DB type.</li>
	 * </ul>
	 * 
	 * @param alias		A DB type alias (ex: spoint)
	 * @param dbType	A DB type (ex: POINT).
	 * 
	 * @return	<code>true</code> if the association has been updated, <code>false</code> otherwise.
	 */
	public static final boolean putDBTypeAlias(String alias, String dbType){
		if (alias == null)
			return false;

		// Normalize the given alias:
		alias = alias.trim().toUpperCase();
		if (alias.length() == 0)
			return false;

		// Check the existence of the given DB type:
		if (dbType != null){
			dbType = dbType.trim().toUpperCase();
			if (dbType.length() == 0)
				return false;
			else if (!dbTypes.containsKey(dbType))
				return false;
		}

		// Update the map of aliases:
		if (dbType == null)
			dbTypeAliases.remove(alias);
		else
			dbTypeAliases.put(alias, dbType);

		return true;
	}


	/** SELF TEST */
	public final static void main(final String[] args) throws Exception {
		System.out.println("***** DB TYPES *****");
		Iterator<String> itDB = TAPTypes.getDBTypes();
		while(itDB.hasNext())
			System.out.println("\t- "+itDB.next());

		System.out.println("\n***** DB TYPE ALIASES *****");
		Iterator<Entry<String,String>> itAliases = TAPTypes.getDBTypeAliases();
		while(itAliases.hasNext()){
			Entry<String, String> e = itAliases.next();
			System.out.println("\t- "+e.getKey()+" = "+e.getValue());
		}

		System.out.println("\n***** VOTABLE TYPES *****");
		Iterator<VotType> itVot = TAPTypes.getVotTypes();
		while(itVot.hasNext())
			System.out.println("\t- "+itVot.next());


		byte[] buffer = new byte[1024];
		int nbRead = 0;
		String type = null;

		System.out.print("\nDB Type ? ");
		nbRead=System.in.read(buffer); type = new String(buffer, 0, nbRead);
		System.out.println(TAPTypes.getVotType(type));

		int arraysize = 1;
		String xtype = null;
		VotType votType = null;
		System.out.print("\nVOTable datatype ? ");
		nbRead=System.in.read(buffer); type = (new String(buffer, 0, nbRead)).trim();
		System.out.print("VOTable arraysize ? ");
		nbRead=System.in.read(buffer);
		try{
			arraysize = Integer.parseInt((new String(buffer, 0, nbRead)).trim());
		}catch(NumberFormatException nfe){
			arraysize = STAR_SIZE;
		}
		System.out.print("VOTable xtype ? ");
		nbRead=System.in.read(buffer); xtype = (new String(buffer, 0, nbRead)).trim(); if (xtype != null && xtype.length() == 0) xtype = null;
		votType = new VotType(type, arraysize, xtype);
		System.out.println(TAPTypes.getDBType(votType));
	}

}
