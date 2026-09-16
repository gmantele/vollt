package vollt.type.column.converter.jdbc;

import vollt.type.column.*;
import vollt.type.column.jdbc.JDBCType;

import java.sql.Types;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (02/2026)
 */
public class PostgresTypeConverter extends DefaultJDBCTypeConverter {

    @Override
    protected ColumnType resolveColumnTypeAsArray(final String dbmsTypeName) {
        if (!isArrayType(dbmsTypeName))
            return null;

        final String scalarType    = dbmsTypeName.substring(1);
        final String defaultLength = "*";

        if (isBit(scalarType))
            return new VectorType(new TypeBit(), defaultLength);

        else if (isBoolean(scalarType))
            return new VectorType(new TypeBoolean(), defaultLength);

        else if (isShort(scalarType))
            return new VectorType(new TypeShort(), defaultLength);

        else if (isInteger(scalarType))
            return new VectorType(new TypeInteger(), defaultLength);

        else if (isLong(scalarType))
            return new VectorType(new TypeLong(), defaultLength);

        else if (isFloat(scalarType))
            return new VectorType(new TypeFloat(), defaultLength);

        else if (isDouble(scalarType))
            return new VectorType(new TypeDouble(), defaultLength);

        else if (isBlob(scalarType))
            return new VectorType(new TypeUnsignedByte(), defaultLength);

        else if (isCharacter(scalarType))
            return new VectorType(new TypeChar(), defaultLength);

        else if (isTimestamp(scalarType))
            return new VectorType(new TypeTimestamp(), defaultLength);

        else
            return null;
    }

    protected boolean isArrayType(final String dbmsTypeName){
        return dbmsTypeName.charAt(0) == '_';
    }

    @Override
    public JDBCType fromColumnType(ColumnType type) {
        if (type instanceof TypeDouble)
            return new JDBCType("DOUBLE PRECISION", Types.DOUBLE);
        else if (type instanceof TypeFloatComplex)
            return new JDBCType("FLOAT[2]", Types.ARRAY);
        else if (type instanceof TypeDoubleComplex)
            return new JDBCType("DOUBLE PRECISION[2]", Types.ARRAY);
        else
            return super.fromColumnType(type);
    }
}
