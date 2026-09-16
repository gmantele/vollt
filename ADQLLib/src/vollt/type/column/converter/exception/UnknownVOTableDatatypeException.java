package vollt.type.column.converter.exception;

import vollt.type.column.ColumnType;
import vollt.type.column.votable.VOTableType;

/**
 * Exception thrown when a conversion from or into a VOTable data type failed.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (02/2026)
 * @since 1.0
 *
 * @see vollt.type.column.converter.VOTableTypeConverter
 */
public class UnknownVOTableDatatypeException extends UnknownColumnTypeException {

    public UnknownVOTableDatatypeException(final VOTableType votType) {
        this(votType, null);
    }

    public UnknownVOTableDatatypeException(final ColumnType colType) {
        this(colType, null);
    }

    public UnknownVOTableDatatypeException(final VOTableType votType, final Throwable error) {
        super("Failed to convert from the VOTable type " + votType + " into a VOLLT's ColumnType!", error);
    }

    public UnknownVOTableDatatypeException(final ColumnType colType, final Throwable error) {
        super("Failed to convert a "+colType+" into a supported VOTable type!", error);
    }

    public UnknownVOTableDatatypeException(final Throwable error) {
        super(error);
    }
}
