package vollt.type.column.converter.exception;

import vollt.type.column.ColumnType;
import vollt.type.column.jdbc.JDBCColumnType;

/**
 * Exception thrown when a conversion from or into a JDBC data type failed.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (02/2026)
 * @since 1.0
 *
 * @see vollt.type.column.converter.jdbc.DefaultJDBCColumnTypeConverter
 */
public class UnknownJDBCTypeException extends UnknownColumnTypeException {

    public UnknownJDBCTypeException(final JDBCColumnType jdbcType) {
        this(jdbcType, null);
    }

    public UnknownJDBCTypeException(final ColumnType colType) {
        this(colType, null);
    }

    public UnknownJDBCTypeException(final JDBCColumnType jdbcType, final Throwable error) {
        super("Failed to convert from the JDBC type \""+jdbcType.getFullName()+"\" (JDBC code: "+jdbcType.getJDBCCode()+") into a VOLLT's ColumnType!", error);
    }

    public UnknownJDBCTypeException(final ColumnType colType, final Throwable error) {
        super("Failed to convert a "+colType+" into a supported JDBC data type!", error);
    }

    public UnknownJDBCTypeException(final Throwable error) {
        super(error);
    }
}
