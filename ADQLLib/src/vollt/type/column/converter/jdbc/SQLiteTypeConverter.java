package vollt.type.column.converter.jdbc;

import vollt.type.column.*;
import vollt.type.column.jdbc.JDBCType;

import java.sql.Types;
import java.util.Optional;

public class SQLiteTypeConverter extends DefaultJDBCTypeConverter {

    @Override
    protected Optional<ColumnType> resolveColumnTypeFromCode(final int jdbcCode, final String typeName, final String lengthSize){
        ColumnType colType;

        switch(jdbcCode)
        {
            case Types.INTEGER:
                colType = new TypeLong();
                break;

            case Types.REAL:
                colType = new TypeDouble();
                break;

            case Types.VARCHAR:
                colType = ColumnTypeFactory.createString();
                break;

            case Types.BLOB:
                colType = new VectorType(new TypeUnsignedByte(), "*");
                break;

            /* Other datatypes can NOT be returned by the JDBC driver of SQLite
             * (because SQLite does not support any other type): */
            default:
                colType = null;
        }

        return Optional.ofNullable(colType);


    }

    @Override
    protected Optional<ColumnType> resolveColumnTypeFromName(final String dbmsTypeName, final String lengthSize) {
        ColumnType colType;

        switch (dbmsTypeName)
        {
            case "integer":
                colType = new TypeLong();
                break;

            case "real":
                colType = new TypeDouble();
                break;

            case "text":
                colType = ColumnTypeFactory.createString();
                break;

            case "blob":
                colType = new VectorType(new TypeUnsignedByte(), "*");
                break;

            default:
                colType = null;
        }

        return Optional.ofNullable(colType);
    }


    @Override
    protected ColumnType resolveColumnTypeAsArray(final String dbmsTypeName) {
        /* Arrays are not supported! */
        return null;
    }

    @Override
    public JDBCType fromColumnType(ColumnType type)
    {
        if (type instanceof TypeBoolean
            || type instanceof TypeShort
            || type instanceof TypeInteger
            || type instanceof TypeLong)
            return new JDBCType(java.sql.JDBCType.INTEGER.getName(), Types.INTEGER);

        else if (type instanceof TypeFloat
                 || type instanceof TypeDouble)
            return new JDBCType("REAL", Types.FLOAT);

        else if (type instanceof TypeBit
                 || type instanceof TypeUnsignedByte)
            return new JDBCType(java.sql.JDBCType.BLOB.getName(), Types.BLOB);

        else
            return new JDBCType(java.sql.JDBCType.VARCHAR.getName(), Types.VARCHAR);
    }
}
