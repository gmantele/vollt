package vollt.type.column.converter.jdbc;

import vollt.type.column.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (02/2026)
 */
public class H2ColumnTypeConverter extends DefaultJDBCColumnTypeConverter {

    private static final Pattern patternForArrayType = Pattern.compile("(\\w+) +ARRAY(\\[\\d+])?");

    @Override
    protected ColumnType resolveColumnTypeAsArray(final String dbmsTypeName) {
        final Matcher matcherForScalarType = patternForArrayType.matcher(dbmsTypeName);

        if (!matcherForScalarType.matches())
            return null;

        final String scalarType    = matcherForScalarType.group(1);
        final String defaultLength = extractLength(matcherForScalarType);

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

    protected String extractLength(final Matcher matcherForScalarType){
        String length = matcherForScalarType.group(3);
        if (length == null)
            length = "*";
        return length;
    }

}
