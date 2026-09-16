package vollt.type.column.converter.exception;

import vollt.type.column.ColumnType;

/**
 * Exception thrown when a conversion from or into an ADQL data type failed.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (02/2026)
 * @since 1.0
 *
 * @see vollt.type.column.converter.ADQLColumnTypeConverter
 */
public class UnknownADQLTypeException extends UnknownColumnTypeException {

    public UnknownADQLTypeException(final String adqlType) {
        this(adqlType, null);
    }

    public UnknownADQLTypeException(final ColumnType colType) {
        this(colType, null);
    }

    public UnknownADQLTypeException(final String adqlType, final Throwable error) {
        super("Failed to convert from the JDBC type \""+adqlType+"\" into a VOLLT's ColumnType!", error);
    }

    public UnknownADQLTypeException(final ColumnType colType, final Throwable error) {
        super("Failed to convert a "+colType+" into a supported ADQL type!", error);
    }

    public UnknownADQLTypeException(final Throwable error) {
        super(error);
    }
}
