package vollt.type.column.converter;

import vollt.type.column.ColumnType;
import vollt.type.column.converter.exception.UnknownColumnTypeException;

/**
 * A {@link ColumnTypeConverter} lets convert from and into a
 * {@link ColumnType}.
 *
 * <p>
 *  Implementations of this interface will mostly be used by
 *  {@link vollt.type.column.ColumnTypeFactory}.
 * </p>
 *
 * @param <T> Alternative type to convert from and into.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (09/2026)
 * @since 1.0
 */
public interface ColumnTypeConverter<T> {

    /**
     * Convert the given {@link ColumnType} into the alternative type.
     *
     * <p><b>Important:</b>
     *     This function MUST NOT return <code>null</code>.
     *     When the conversion seems impossible an
     *     {@link UnknownColumnTypeException} must be thrown.
     * </p>
     *
     * @param originType    {@link ColumnType} to convert.
     *
     * @return  Corresponding alternative type.
     *
     * @throws UnknownColumnTypeException When the conversion is not possible.
     */
    T fromColumnType(final ColumnType originType) throws UnknownColumnTypeException;

    /**
     * Convert the given alternative type into a {@link ColumnType}.
     *
     * <p><b>Important:</b>
     *     This function MUST NOT return <code>null</code>.
     *     When the conversion seems impossible an
     *     {@link UnknownColumnTypeException} must be thrown.
     * </p>
     *
     * @param originType    Alternative type to convert.
     *
     * @return  Corresponding {@link ColumnType}.
     *
     * @throws UnknownColumnTypeException When the conversion is not possible.
     */
    ColumnType toColumnType(final T originType) throws UnknownColumnTypeException;

}
