package vollt.type.column.converter.exception;

/**
 * Exception thrown when a column type cannot be resolved from or into a known
 * type. This exception is thrown when converting from or into a
 * {@link vollt.type.column.ColumnType}.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (09/2026)
 * @since 1.0
 *
 * @see vollt.type.column.converter.ColumnTypeConverter
 */
public class UnknownColumnTypeException extends Exception {

    public UnknownColumnTypeException() {
    }

    public UnknownColumnTypeException(final String message) {
        super(message);
    }

    public UnknownColumnTypeException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public UnknownColumnTypeException(final Throwable throwable) {
        super(throwable);
    }

}
