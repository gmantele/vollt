package vollt.type.column.converter;

import vollt.type.column.ColumnType;
import vollt.type.column.converter.exception.UnknownColumnTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tool class allowing to convert an ADQL column type (e.g. column types used in
 * the CAST function) into an internal {@link ColumnType column type}.
 *
 * <p>
 *     The mapping between ADQL column types and the internal one (i.e. VOTable
 *     field type system) is loaded from the Properties file:
 *     TODO set file name here
 * </p>
 * <p>
 *     The mapping can be overloaded by a custom Properties file thanks to the
 *     function {@link #updateADQLColumnTypeMapping(Properties)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (04/2024)
 */
public final class ADQLColumnTypeConverter implements ColumnTypeConverter<String> {

    private static final Map<String, ColumnType> mapADQLTypes = new HashMap<>();

    @Override
    public String fromColumnType(ColumnType originType) {
        throw new UnsupportedOperationException();
        // TODO ADQLColumnTypeConverter.fromColumnType(ColumnType)
    }

    /**
     * Resolve the given ADQL column type (e.g. target type used in the CAST
     * function) into an internal {@link ColumnType column type}.
     *
     * <p>
     *     The given name is normalized (i.e. trimmed, lower cased and all
     *     consecutive space characters replaced by a single _) automatically by
     *     this function.
     * </p>
     *
     * @param adqlType  The ADQL column type to convert.
     *
     * @return  The corresponding {@link ColumnType}.
     *
     * @throws UnknownColumnTypeException   If no corresponding
     *                                              {@link ColumnType} can be
     *                                              found.
     */
    @Override
    public ColumnType toColumnType(final String adqlType) throws UnknownColumnTypeException {
        final String normalizedTypeName = normalizeADQLTypeName(adqlType);

        final ColumnType matchingType = mapADQLTypes.get(normalizedTypeName);

        if (matchingType == null)
            throw new UnknownColumnTypeException(adqlType);
        else
            return matchingType;
    }

    private static String normalizeADQLTypeName(final String adqlType){
        return adqlType.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    public static void updateADQLColumnTypeMapping(final Properties mappingUpdate){
        // TODO Load ADQL column type mapping from a Properties file
    }

}
