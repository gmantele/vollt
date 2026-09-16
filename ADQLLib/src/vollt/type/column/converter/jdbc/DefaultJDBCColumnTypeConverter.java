package vollt.type.column.converter.jdbc;

import vollt.type.column.*;
import vollt.type.column.converter.ColumnTypeConverter;
import vollt.type.column.jdbc.JDBCColumnType;

import java.sql.JDBCType;
import java.sql.SQLType;
import java.sql.Types;
import java.util.Optional;

public class DefaultJDBCColumnTypeConverter implements ColumnTypeConverter<JDBCColumnType> {

    @Override
    public ColumnType toColumnType(final JDBCColumnType dbType) {
        final String lengthSize = extractLength(dbType);

        Optional<ColumnType> colType = extractColumnTypeFromTypeCode(dbType, lengthSize);

        if (!colType.isPresent())
            colType = extractColumnTypeFromTypeName(dbType, lengthSize);

        return colType.orElseGet(ColumnTypeFactory::createString);
    }

    protected Optional<ColumnType> extractColumnTypeFromTypeCode(final JDBCColumnType dbType, final String lengthSize){
        return dbType.getJDBCCode()
                     .flatMap(code -> resolveColumnTypeFromCode(code, dbType.getSimpleName(), lengthSize));
    }

    protected String extractLength(final JDBCColumnType dbType){
        return dbType.getPrecision()
                .map(precision -> precision >= 0 ? precision.toString() : "*")
                .orElse("*");
    }

    protected Optional<ColumnType> resolveColumnTypeFromCode(final int jdbcCode, final String typeName, final String lengthSize){
        ColumnType colType;

        switch(jdbcCode){
            case Types.BOOLEAN:
                colType = new TypeBoolean();
                break;
            case Types.BIT:
                colType = new TypeBit();
                break;
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                if (lengthSize.equals("1"))
                    colType = new TypeUnsignedByte();
                else
                    colType = new VectorType(new TypeUnsignedByte(), lengthSize);
                break;
            case Types.SMALLINT:
            case Types.TINYINT:
                colType = new TypeShort();
                break;
            case Types.INTEGER:
                colType = new TypeInteger();
                break;
            case Types.BIGINT:
            case Types.ROWID:
                colType = new TypeLong();
                break;
            case Types.CHAR:
                if (lengthSize.equals("1"))
                    colType = new TypeChar();
                else
                    colType = new VectorType(new TypeChar(), lengthSize);
                break;
            case Types.NCHAR:
                if (lengthSize.equals("1"))
                    colType = new TypeUnicodeChar();
                else
                    colType = new VectorType(new TypeUnicodeChar(), lengthSize);
                break;
            case Types.FLOAT:
            case Types.REAL:
                colType = new TypeFloat();
                break;
            case Types.DOUBLE:
            case Types.DECIMAL:
            case Types.NUMERIC:
                colType = new TypeDouble();
                break;
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                colType = new TypeTimestamp();
                break;
            case Types.ARRAY:
                colType = resolveColumnTypeAsArray(typeName);
                break;
            case Types.CLOB:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                colType = new VectorType(new TypeChar(), lengthSize);
                break;
            case Types.NCLOB:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                colType = new VectorType(new TypeUnicodeChar(), lengthSize);
                break;
            default:
                colType = null;
        }

        return Optional.ofNullable(colType);
    }

    protected Optional<ColumnType> extractColumnTypeFromTypeName(final JDBCColumnType dbType, final String lengthSize){
        final String typeName = dbType.getSimpleName();
        return resolveColumnTypeFromName(typeName, lengthSize);
    }

    protected Optional<ColumnType> resolveColumnTypeFromName(final String dbmsTypeName, final String lengthSize) {
        ColumnType colType = null;

        if (isBit(dbmsTypeName))
            colType = new TypeBit();

        else if (isBoolean(dbmsTypeName))
            colType = new TypeBoolean();

        if (isShort(dbmsTypeName))
            colType = new TypeShort();

        else if (isInteger(dbmsTypeName))
            colType = new TypeInteger();

        else if (isLong(dbmsTypeName))
            colType = new TypeLong();

        else if (isFloat(dbmsTypeName))
            colType = new TypeFloat();

        else if (isDouble(dbmsTypeName))
            colType = new TypeDouble();

        else if (isBlob(dbmsTypeName))
            colType = new VectorType(new TypeUnsignedByte(), lengthSize);

        else if (isSingleCharacter(dbmsTypeName, lengthSize))
            colType = new TypeChar();

        else if (isCharacter(dbmsTypeName))
            colType = new VectorType(new TypeChar(), lengthSize);

        else if (isTimestamp(dbmsTypeName))
            colType = new TypeTimestamp();

        return Optional.ofNullable(colType);
    }

    /**
     * Resolve the raw datatype name as a datatype representing an array type.
     *
     * <p><b><u>IMPORTANT:</u></b>
     *  Because this string is DBMS dependent and that most DBMS do not support
     *  arrays, this function returns empty by default. <b>If your DBMS supports
     *  arrays, this function must be overwritten in a custom
     *  {@link ColumnTypeConverter} which will then extend this one.</b>
     * </p>
     *
     * <p>
     *     For instance, in Postgres, an array of integer is written as
     *     <code>_int4</code> while in H2 it would be
     *     <code>INTEGER ARRAY</code>.
     * </p>
     *
     * @param dbmsTypeName  Raw datatype name.
     *
     * @return  The corresponding {@link ColumnType}.
     *
     * @see PostgresColumnTypeConverter#resolveColumnTypeAsArray(String)
     */
    protected ColumnType resolveColumnTypeAsArray(final String dbmsTypeName) {
        /* By default, assume that arrays are not supported! */
        return null;
    }

    protected boolean isBit(final String dbmsTypeName){
        return dbmsTypeName.equals("bit")
                || dbmsTypeName.equals("binary")
                || dbmsTypeName.equals("raw")
                || ((dbmsTypeName.startsWith("char") || dbmsTypeName.startsWith("character")) && dbmsTypeName.endsWith(" for bit data"));
    }

    protected boolean isBoolean(final String dbmsTypeName){
        return dbmsTypeName.equals("boolean")
                || dbmsTypeName.equals("bool");
    }

    protected boolean isShort(final String dbmsTypeName){
        return dbmsTypeName.equals("smallint")
                || dbmsTypeName.equals("int2")
                || dbmsTypeName.equals("smallserial")
                || dbmsTypeName.equals("serial2");
    }

    protected boolean isInteger(final String dbmsTypeName){
        return dbmsTypeName.equals("integer")
                || dbmsTypeName.equals("int")
                || dbmsTypeName.equals("int4")
                || dbmsTypeName.equals("serial")
                || dbmsTypeName.equals("serial4");
    }

    protected boolean isLong(final String dbmsTypeName){
        return dbmsTypeName.equals("bigint")
                || dbmsTypeName.equals("int8")
                || dbmsTypeName.equals("bigserial")
                || dbmsTypeName.equals("bigserial8")
                || dbmsTypeName.equals("number");
    }

    protected boolean isFloat(final String dbmsTypeName){
        return dbmsTypeName.equals("real")
                || dbmsTypeName.equals("float4")
                || (dbmsTypeName.equals("float") /* TODO re-interpret: && lengthParam <= 63*/);
    }

    protected boolean isDouble(final String dbmsTypeName){
        return dbmsTypeName.equals("double")
                || dbmsTypeName.equals("double precision")
                || dbmsTypeName.equals("float8")
                || (dbmsTypeName.equals("float") /* TODO re-interpret: && lengthParam > 63*/)
                || dbmsTypeName.equals("numeric");
    }

    protected boolean isBlob(final String dbmsTypeName){
        return dbmsTypeName.equals("bit varying")
                || dbmsTypeName.equals("varbit")
                || dbmsTypeName.equals("varbinary")
                || dbmsTypeName.equals("long raw")
                || ((dbmsTypeName.startsWith("varchar") || dbmsTypeName.startsWith("character varying")) && dbmsTypeName.endsWith(" for bit data"))
                || dbmsTypeName.equals("bytea")
                || dbmsTypeName.equals("blob")
                || dbmsTypeName.equals("binary large object");
    }

    protected boolean isSingleCharacter(final String dbmsTypeName, final String lengthSize){
        return (dbmsTypeName.equals("char") || dbmsTypeName.equals("character"))
                && lengthSize.equals("1");
    }

    protected boolean isCharacter(final String dbmsTypeName){
        return dbmsTypeName.equals("char")
                || dbmsTypeName.equals("character")
                || dbmsTypeName.equals("varchar")
                || dbmsTypeName.equals("varchar2")
                || dbmsTypeName.equals("character varying")
                || dbmsTypeName.equals("text")
                || dbmsTypeName.equals("clob")
                || dbmsTypeName.equals("character large object");
    }

    protected boolean isTimestamp(final String dbmsTypeName){
        return dbmsTypeName.equals("timestamp")
                || dbmsTypeName.equals("timestamptz")
                || dbmsTypeName.equals("time")
                || dbmsTypeName.equals("timetz")
                || dbmsTypeName.equals("date");
    }

    @Override
    public JDBCColumnType fromColumnType(final ColumnType type) {
        if (type instanceof TypeBoolean)
            return new JDBCColumnType(JDBCType.BOOLEAN.getName(), Types.BOOLEAN);
        else if (type instanceof TypeBit)
            return new JDBCColumnType(JDBCType.BIT.getName(), Types.BIT);
        else if (type instanceof TypeUnsignedByte)
            return new JDBCColumnType(JDBCType.BINARY.getName(), Types.BINARY);
        else if (type instanceof TypeShort)
            return new JDBCColumnType(JDBCType.SMALLINT.getName(), Types.SMALLINT);
        else if (type instanceof TypeInteger)
            return new JDBCColumnType(JDBCType.INTEGER.getName(), Types.INTEGER);
        else if (type instanceof TypeLong)
            return new JDBCColumnType(JDBCType.BIGINT.getName(), Types.BIGINT);
        else if (type instanceof TypeChar)
            return new JDBCColumnType(JDBCType.CHAR.getName(), Types.CHAR);
        else if (type instanceof TypeUnicodeChar)
            return new JDBCColumnType(JDBCType.NCHAR.getName(), Types.NCHAR);
        else if (type instanceof TypeFloat)
            return new JDBCColumnType(JDBCType.FLOAT.getName(), Types.FLOAT);
        else if (type instanceof TypeDouble)
            return new JDBCColumnType(JDBCType.DOUBLE.getName(), Types.DOUBLE);
        else if (type instanceof TypeTimestamp)
            return new JDBCColumnType(JDBCType.TIMESTAMP.getName(), Types.TIMESTAMP);
        else if (type instanceof TypeFloatComplex
                || type instanceof TypeDoubleComplex
                || type instanceof TypeInterval
                || type instanceof TypeMultiInterval
                || type instanceof TypeHMS
                || type instanceof TypeDMS
                || type instanceof GeometryType
                || type instanceof TypeMOC
                || type instanceof TypeShape
                || type instanceof TypeMultiShape
                || type instanceof TypeURI
                || type instanceof TypeUUID
                || type instanceof TypeJSON)
            return new JDBCColumnType(JDBCType.VARCHAR.getName(), Types.VARCHAR);
        else
            return new JDBCColumnType(JDBCType.VARCHAR.getName(), Types.VARCHAR); // TODO Throw an exception or a special message according to DALI-1.2
    }

    /*
        if (datatype == null)
            datatype = new DBType(DBType.DBDatatype.VARCHAR);

        switch(type.getVotDatatype()) {

            case SMALLINT:
                return dbms.equals("sqlite") ? "INTEGER" : "SMALLINT";

            case INTEGER:
            case REAL:
                return datatype.type.toString();

            case BIGINT:
                if (dbms.equals("oracle"))
                    return "NUMBER(19,0)";
                else if (dbms.equals("sqlite"))
                    return "INTEGER";
                else
                    return "BIGINT";

            case DOUBLE:
                if (dbms.equals("postgresql") || dbms.equals("oracle"))
                    return "DOUBLE PRECISION";
                else if (dbms.equals("sqlite"))
                    return "REAL";
                else
                    return "DOUBLE";

            case BINARY:
                if (dbms.equals("postgresql"))
                    return "bytea";
                else if (dbms.equals("sqlite"))
                    return "BLOB";
                else if (dbms.equals("oracle"))
                    return "RAW" + (datatype.length > 0 ? "(" + datatype.length + ")" : "");
                else if (dbms.equals("derby"))
                    return "CHAR" + (datatype.length > 0 ? "(" + datatype.length + ")" : "") + " FOR BIT DATA";
                else
                    return datatype.type.toString();

            case VARBINARY:
                if (dbms.equals("postgresql"))
                    return "bytea";
                else if (dbms.equals("sqlite"))
                    return "BLOB";
                else if (dbms.equals("oracle"))
                    return "LONG RAW" + (datatype.length > 0 ? "(" + datatype.length + ")" : "");
                else if (dbms.equals("derby"))
                    return "VARCHAR" + (datatype.length > 0 ? "(" + datatype.length + ")" : "") + " FOR BIT DATA";
                else
                    return datatype.type.toString();

            case CHAR:
                if (dbms.equals("sqlite"))
                    return "TEXT";
                else
                    return "CHAR";

            case BLOB:
                if (dbms.equals("postgresql"))
                    return "bytea";
                else
                    return "BLOB";

            case CLOB:
                if (dbms.equals("postgresql") || dbms.equals("mysql") || dbms.equals("sqlite"))
                    return "TEXT";
                else
                    return "CLOB";

            case TIMESTAMP:
                if (dbms.equals("sqlite"))
                    return "TEXT";
                else
                    return "TIMESTAMP";

            case POINT:
            case REGION:
            case VARCHAR:
            default:
                if (dbms.equals("sqlite"))
                    return "TEXT";
                else
                    return "VARCHAR";
        }
    */
}
