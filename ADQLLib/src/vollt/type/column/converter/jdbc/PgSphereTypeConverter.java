package vollt.type.column.converter.jdbc;

import vollt.type.column.*;
import vollt.type.column.jdbc.JDBCType;

import java.util.Optional;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (02 / 2025)
 */
public class PgSphereTypeConverter extends PostgresTypeConverter {

    protected Optional<ColumnType> resolveColumnTypeFromName(final String dbmsTypeName, final String lengthSize) {
        if ("spoint".equalsIgnoreCase(dbmsTypeName))
            return Optional.of(new TypePointWithDouble());
        else if ("scircle".equalsIgnoreCase(dbmsTypeName))
            return Optional.of(new TypeCircleWithDouble());
        else if ("spoly".equalsIgnoreCase(dbmsTypeName))
            return Optional.of(new TypePolygonWithDouble());
        else if ("sbox".equalsIgnoreCase(dbmsTypeName))
            return Optional.of(new TypeRangeWithDouble());
        else
            return super.resolveColumnTypeFromName(dbmsTypeName, lengthSize);
    }

    @Override
    public JDBCType fromColumnType(final ColumnType type) {
        return super.fromColumnType(type);
    }

}
