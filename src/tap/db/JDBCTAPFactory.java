package tap.db;

import java.util.HashMap;
import java.util.Map;

import tap.metadata.TAPType;
import tap.metadata.TAPType.TAPDatatype;

public class JDBCTAPFactory {

	public static enum DBMS{
		POSTGRES;
	}

	public static interface DbmsTypeConverter< T, C > {
		public C convert(final T typeToConvert);
	}

	public static Map<String,TAPDatatype> mapTypeAliases;
	public static Map<String,DbmsTypeConverter<String,TAPType>> mapDbmsToTap;
	public static Map<DBMS,Map<TAPDatatype,DbmsTypeConverter<TAPType,String>>> mapTapToDbms;

	static{
		/* DECLARE DBMS TYPE ALIASES */
		mapTypeAliases = new HashMap<String,TAPType.TAPDatatype>();
		mapTypeAliases.put("int8", TAPDatatype.BIGINT);
		mapTypeAliases.put("bigserial", TAPDatatype.BIGINT);
		mapTypeAliases.put("bit", TAPDatatype.VARCHAR);
		mapTypeAliases.put("bit varying", TAPDatatype.VARCHAR);
		mapTypeAliases.put("varbit", TAPDatatype.VARCHAR);
		mapTypeAliases.put("boolean", TAPDatatype.SMALLINT);
		mapTypeAliases.put("bytea", TAPDatatype.VARBINARY);
		mapTypeAliases.put("character varying", TAPDatatype.VARCHAR);
		mapTypeAliases.put("character", TAPDatatype.CHAR);
		mapTypeAliases.put("double precision", TAPDatatype.DOUBLE);
		mapTypeAliases.put("float8", TAPDatatype.DOUBLE);
		mapTypeAliases.put("integer", TAPDatatype.INTEGER);
		mapTypeAliases.put("int4", TAPDatatype.INTEGER);
		mapTypeAliases.put("float4", TAPDatatype.REAL);
		mapTypeAliases.put("int2", TAPDatatype.SMALLINT);
		mapTypeAliases.put("serial", TAPDatatype.INTEGER);
		mapTypeAliases.put("serial4", TAPDatatype.INTEGER);
		mapTypeAliases.put("text", TAPDatatype.VARCHAR);

		/* DECLARE SPECIAL DBMS->TAP CONVERSIONS */
		mapDbmsToTap = new HashMap<String,JDBCTAPFactory.DbmsTypeConverter<String,TAPType>>();
		mapDbmsToTap.put("numeric", new DbmsTypeConverter<String,TAPType>(){
			@Override
			public TAPType convert(String typeToConvert){
				return new TAPType(TAPDatatype.DOUBLE);
			}
		});
		mapDbmsToTap.put("decimal", new DbmsTypeConverter<String,TAPType>(){
			@Override
			public TAPType convert(String typeToConvert){
				return new TAPType(TAPDatatype.DOUBLE);
			}
		});

		/* DECLARE SPECIAL TAP->DBMS CONVERSIONS */
		mapTapToDbms = new HashMap<DBMS,Map<TAPDatatype,DbmsTypeConverter<TAPType,String>>>();
		// POSTGRES
		HashMap<TAPDatatype,DbmsTypeConverter<TAPType,String>> postgresConverters = new HashMap<TAPDatatype,JDBCTAPFactory.DbmsTypeConverter<TAPType,String>>();
		postgresConverters.put(TAPDatatype.DOUBLE, new DbmsTypeConverter<TAPType,String>(){
			@Override
			public String convert(TAPType typeToConvert){
				return "double precision";
			}
		});
		DbmsTypeConverter<TAPType,String> binaryConverter = new DbmsTypeConverter<TAPType,String>(){
			@Override
			public String convert(TAPType typeToConvert){
				return "bytea";
			}
		};
		postgresConverters.put(TAPDatatype.VARBINARY, binaryConverter);
		postgresConverters.put(TAPDatatype.BINARY, binaryConverter);
		postgresConverters.put(TAPDatatype.BLOB, binaryConverter);
		postgresConverters.put(TAPDatatype.CLOB, binaryConverter);
		mapTapToDbms.put(DBMS.POSTGRES, postgresConverters);
	}

	public JDBCTAPFactory(){
		// TODO Auto-generated constructor stub
	}

	/**
	 * <p>Convert the given TAP column type into a column type compatible with the specified DBMS.</p>
	 * 
	 * <p><i>Note 1: if no {@link TAPType} is provided, the returned DBMS type will correspond to a
	 * VARCHAR.</i></p>
	 * 
	 * <p><i>Note 2: if no DBMS is specified or if the conversion has failed, the given TAP type will be
	 * just "stringified" (by calling {@link TAPType#toString()})</i></p>
	 * 
	 * @param tapType	A TAP column type.
	 * @param dbms		DBMS target in which the given TAP column type must be converted.
	 * 
	 * @return	The corresponding DBMS column type.
	 */
	public static String toDbmsType(TAPType tapType, final DBMS dbms){
		// If no TAP type is specified, consider it by default as a VARCHAR type:
		if (tapType == null)
			tapType = new TAPType(TAPDatatype.VARCHAR);

		// By default, just "stringify" the given TAP type:
		String dbmsType = tapType.toString();

		// If some converters are defined for the specified DBMS...
		if (dbms != null && mapTapToDbms.containsKey(dbms)){
			Map<TAPDatatype,DbmsTypeConverter<TAPType,String>> dbmsMap = mapTapToDbms.get(dbms);
			// ...and if a converter exists for the given TAP datatype...
			DbmsTypeConverter<TAPType,String> converter = dbmsMap.get(tapType.type);
			if (converter != null){
				// ...convert the given TAP type:
				String conversion = converter.convert(tapType);
				// ...and set the DBMS conversion if NOT NULL:
				if (conversion != null)
					dbmsType = conversion;
			}
		}

		return dbmsType;
	}

	/**
	 * <p>Convert the given DBMS column type into a compatible TAP datatype.</p>
	 * 
	 * <p><i>Note: If no DBMS type is specified or if the DBMS type can not be identified,
	 * it will be converted as a VARCHAR.</i></p>
	 * 
	 * @param dbmsType	DBMS column datatype.
	 * 
	 * @return	The corresponding TAP column datatype.
	 */
	public static TAPType toTAPType(final String dbmsType){
		// If no type is provided return VARCHAR:
		if (dbmsType == null || dbmsType.trim().length() == 0)
			return new TAPType(TAPDatatype.VARCHAR);

		// Extract the type prefix and lower-case it:
		int paramIndex = dbmsType.indexOf('(');
		String dbmsTypePrefix = (paramIndex <= 0) ? dbmsType : dbmsType.substring(0, paramIndex);
		dbmsTypePrefix = dbmsTypePrefix.toLowerCase();

		// Use this type prefix as key to determine if it's a DBMS type alias and get its corresponding TAP datatype:
		TAPDatatype datatype = mapTypeAliases.get(dbmsTypePrefix);

		// If it's an alias, build the corresponding TAP type:
		if (datatype != null)
			return new TAPType(datatype, getLengthParam(dbmsType, paramIndex));

		// If it's not an alias, use the type prefix as key to get a corresponding converter:
		DbmsTypeConverter<String,TAPType> converter = mapDbmsToTap.get(dbmsTypePrefix);

		// Try the type conversion using this converter:
		TAPType taptype = null;
		if (converter != null)
			taptype = converter.convert(dbmsType);

		/* 
		 * If no converter was found OR if the type conversion has failed,
		 * consider the given type as equivalent to a declared TAP type.
		 * 
		 * /!\ But if no equivalent exists, the given type will be ignore and
		 *     VARCHAR will be returned!
		 */
		if (taptype == null){
			try{

				// Try to find an equivalent TAPType:
				datatype = TAPDatatype.valueOf(dbmsTypePrefix.toUpperCase());

				// If there is one return directly the TAPType:
				taptype = new TAPType(datatype, getLengthParam(dbmsType, paramIndex));

			}catch(IllegalArgumentException iae){
				// If none exists, return VARCHAR:
				taptype = new TAPType(TAPDatatype.VARCHAR, TAPType.NO_LENGTH);
			}
		}

		return taptype;
	}

	/**
	 * <p>Extract the 'length' parameter of a DBMS type string.</p>
	 * 
	 * <p>
	 * 	If the given type string does not contain any parameter
	 * 	OR if the first parameter can not be casted into an integer,
	 * 	{@link TAPType#NO_LENGTH} will be returned.
	 * </p>
	 * 
	 * @param dbmsType		DBMS type string (containing the datatype and the 'length' parameter).
	 * @param paramIndex	Index of the open bracket.
	 * 
	 * @return	The 'length' parameter value if found, {@link TAPType#NO_LENGTH} otherwise.
	 */
	private static int getLengthParam(final String dbmsType, final int paramIndex){
		// If no parameter has been previously detected, no length parameter:
		if (paramIndex <= 0)
			return TAPType.NO_LENGTH;

		// If there is one and that at least ONE parameter is provided....
		else{
			int lengthParam = TAPType.NO_LENGTH;
			String paramsStr = dbmsType.substring(paramIndex + 1);

			// ...extract the 'length' parameter:
			/* note: we suppose here that no other parameter is possible ;
			 *       but if there are, they are ignored and we try to consider the first parameter
			 *       as the length */
			int paramEndIndex = paramsStr.indexOf(',');
			if (paramEndIndex <= 0)
				paramEndIndex = paramsStr.indexOf(')');

			// ...cast it into an integer:
			try{
				lengthParam = Integer.parseInt(paramsStr.substring(0, paramEndIndex));
			}catch(Exception ex){}

			// ...and finally return it:
			return lengthParam;
		}
	}

}
