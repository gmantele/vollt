package vollt.type.column.converter.jdbc;

import org.junit.Test;
import vollt.type.column.*;
import vollt.type.column.jdbc.JDBCColumnType;

import java.sql.JDBCType;
import java.sql.Types;

import static org.junit.Assert.*;

/**
 * Test to check whether all conversions described in the
 * data-type-conversion.md matrix are respected.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (02/2026)
 */
public class TestDefaultJDBCColumnTypeConverter {

    @Test
    public void toColumnType_ShouldSucceed_WhenSimpleVOTableTypes() {
        /*
         * Note: only VOTable simple types can be resolved when using the JDBC
         *       type. Types like floatComplex, HMS, point, moc, ..., need
         *       additional information coming from the TAP_SCHEMA or from the
         *       uploaded VOTable's metadata.
         */
        // Given:
        final JDBCColumnType[] supportedJDBCTypes = new JDBCColumnType[]{
                new JDBCColumnType(JDBCType.BOOLEAN.getName()                , Types.BOOLEAN                , null, null),
                new JDBCColumnType(JDBCType.BIT.getName()                    , Types.BIT                    , null, null),
                new JDBCColumnType(JDBCType.BINARY.getName()                 , Types.BINARY                 , 1   , null),
                new JDBCColumnType(JDBCType.SMALLINT.getName()               , Types.SMALLINT               , null, null),
                new JDBCColumnType(JDBCType.TINYINT.getName()                , Types.TINYINT                , null, null),
                new JDBCColumnType(JDBCType.INTEGER.getName()                , Types.INTEGER                , null, null),
                new JDBCColumnType(JDBCType.BIGINT.getName()                 , Types.BIGINT                 , null, null),
                new JDBCColumnType(JDBCType.ROWID.getName()                  , Types.ROWID                  , null, null),
                new JDBCColumnType(JDBCType.CHAR.getName()                   , Types.CHAR                   , 1   , null),
                new JDBCColumnType(JDBCType.NCHAR.getName()                  , Types.NCHAR                  , 1   , null),
                new JDBCColumnType(JDBCType.FLOAT.getName()                  , Types.FLOAT                  , null, null),
                new JDBCColumnType(JDBCType.REAL.getName()                   , Types.REAL                   , null, null),
                new JDBCColumnType(JDBCType.DOUBLE.getName()                 , Types.DOUBLE                 , null, null),
                new JDBCColumnType(JDBCType.DECIMAL.getName()                , Types.DECIMAL                , null, null),
                new JDBCColumnType(JDBCType.NUMERIC.getName()                , Types.NUMERIC                , null, null),
                new JDBCColumnType(JDBCType.TIMESTAMP.getName()              , Types.TIMESTAMP              , null, null),
                new JDBCColumnType(JDBCType.DATE.getName()                   , Types.DATE                   , null, null),
                new JDBCColumnType(JDBCType.TIME.getName()                   , Types.TIME                   , null, null),
                new JDBCColumnType(JDBCType.TIME_WITH_TIMEZONE.getName()     , Types.TIME_WITH_TIMEZONE     , null, null),
                new JDBCColumnType(JDBCType.TIMESTAMP_WITH_TIMEZONE.getName(), Types.TIMESTAMP_WITH_TIMEZONE, null, null)
        };
        final ColumnType[] expectedColumnTypes = new ColumnType[]{
                new TypeBoolean(),
                new TypeBit(),
                new TypeUnsignedByte(),
                new TypeShort(),
                new TypeShort(),
                new TypeInteger(),
                new TypeLong(),
                new TypeLong(),
                new TypeChar(),
                new TypeUnicodeChar(),
                new TypeFloat(),
                new TypeFloat(),
                new TypeDouble(),
                new TypeDouble(),
                new TypeDouble(),
                new TypeTimestamp(),
                new TypeTimestamp(),
                new TypeTimestamp(),
                new TypeTimestamp(),
                new TypeTimestamp()
        };

        final DefaultJDBCColumnTypeConverter converter = new DefaultJDBCColumnTypeConverter();
        for(int i=0; i<supportedJDBCTypes.length; i++)
        {
            // When:
            final ColumnType returnedType = converter.toColumnType(supportedJDBCTypes[i]);

            // Then:
            final ColumnType expectedType = expectedColumnTypes[i];

            System.out.println("Tested  : "+supportedJDBCTypes[i].getSimpleName()+" ("+supportedJDBCTypes[i].getJDBCCode()+")");
            System.out.println("Returned: "+returnedType);
            System.out.println("Expected: "+expectedType);

            assertEquals(expectedType.getVotDatatype() , returnedType.getVotDatatype());

            assertEquals(expectedType.getVotArraysize().isPresent(), returnedType.getVotArraysize().isPresent());
            if (returnedType.getVotArraysize().isPresent())
                assertEquals(expectedType.getVotArraysize(), returnedType.getVotArraysize());

            assertEquals(expectedType.getVotXtype().isPresent(), returnedType.getVotXtype().isPresent());
            if (expectedType.getVotXtype().isPresent())
                assertEquals(expectedType.getVotXtype()    , returnedType.getVotXtype());
        }
    }

    @Test
    public void fromColumnType()
    {
        // Given:
        final ColumnType[] supportedColumnTypes = new ColumnType[]{
                new TypeBoolean(),
                new TypeBit(),
                new TypeUnsignedByte(),
                new TypeShort(),
                new TypeInteger(),
                new TypeLong(),
                new TypeChar(),
                new TypeUnicodeChar(),
                new TypeFloat(),
                new TypeDouble(),
                new TypeFloatComplex(),
                new TypeDoubleComplex(),
                new TypeTimestamp(),
                new TypeIntervalOfShort(),
                new TypeIntervalOfInteger(),
                new TypeIntervalOfLong(),
                new TypeIntervalOfFloat(),
                new TypeIntervalOfDouble(),
                new TypeMultiIntervalOfShort(),
                new TypeMultiIntervalOfInteger(),
                new TypeMultiIntervalOfLong(),
                new TypeMultiIntervalOfFloat(),
                new TypeMultiIntervalOfDouble(),
                new TypeHMS(),
                new TypeDMS(),
                new TypePointWithFloat(),
                new TypePointWithDouble(),
                new TypeCircleWithFloat(),
                new TypeCircleWithDouble(),
                new TypeRangeWithFloat(),
                new TypeRangeWithDouble(),
                new TypePolygonWithFloat(),
                new TypePolygonWithDouble(),
                new TypeMOC(),
                new TypeShape(),
                new TypeMultiShape(),
                new TypeURI(),
                new TypeUUID(),
                new TypeJSON(),
                new TypeRegion()
        };

        final JDBCColumnType[] expectedJDBCTypes = new JDBCColumnType[]{
                new JDBCColumnType(JDBCType.BOOLEAN.getName()                , Types.BOOLEAN                , null, null),
                new JDBCColumnType(JDBCType.BIT.getName()                    , Types.BIT                    , null, null),
                new JDBCColumnType(JDBCType.BINARY.getName()                 , Types.BINARY                 , 1   , null),
                new JDBCColumnType(JDBCType.SMALLINT.getName()               , Types.SMALLINT               , null, null),
                new JDBCColumnType(JDBCType.INTEGER.getName()                , Types.INTEGER                , null, null),
                new JDBCColumnType(JDBCType.BIGINT.getName()                 , Types.BIGINT                 , null, null),
                new JDBCColumnType(JDBCType.CHAR.getName()                   , Types.CHAR                   , 1   , null),
                new JDBCColumnType(JDBCType.NCHAR.getName()                  , Types.NCHAR                  , 1   , null),
                new JDBCColumnType(JDBCType.FLOAT.getName()                  , Types.FLOAT                  , null, null),
                new JDBCColumnType(JDBCType.DOUBLE.getName()                 , Types.DOUBLE                 , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.TIMESTAMP.getName()              , Types.TIMESTAMP              , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null),
                new JDBCColumnType(JDBCType.VARCHAR.getName()                , Types.VARCHAR                , null, null)
        };

        final DefaultJDBCColumnTypeConverter converter = new DefaultJDBCColumnTypeConverter();
        for(int i=0; i<supportedColumnTypes.length; i++)
        {
            // When:
            final JDBCColumnType returnedType = converter.fromColumnType(supportedColumnTypes[i]);

            // Then:
            final JDBCColumnType expectedType = expectedJDBCTypes[i];

            System.out.println("Tested  : "+supportedColumnTypes[i]);
            System.out.println("Returned: "+returnedType.getSimpleName() + " ("+returnedType.getJDBCCode()+")");
            System.out.println("Expected: "+expectedType.getSimpleName() + " ("+expectedType.getJDBCCode()+")");

            assertEquals(expectedType.getSimpleName() , returnedType.getSimpleName());

            assertEquals(expectedType.getJDBCCode(), returnedType.getJDBCCode());
        }
    }
}