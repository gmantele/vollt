package vollt.type.column.converter;

import org.junit.BeforeClass;
import org.junit.Test;
import vollt.type.column.*;
import vollt.type.column.converter.exception.UnknownColumnTypeException;
import vollt.type.column.votable.VOTableType;
import vollt.type.column.votable.VotDatatype;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class TestVOTableTypeConverter {

    private static VOTableTypeConverter converter;

    @BeforeClass
    public static void initAll() {
        converter = new VOTableTypeConverter();
    }


    /***************************************************************************
     * FROM_COLUMN_TYPE
     */

    @Test
    public void fromColumnType_ShouldFail_WhenNull(){
        assertThrows(NullPointerException.class, ()->converter.fromColumnType(null));
    }


    /***************************************************************************
     * TO_COLUMN_TYPE
     */

    @Test
    public void toColumnType_ShouldFail_WhenNull(){
        assertThrows(NullPointerException.class, ()->converter.toColumnType(null));
    }

    @Test
    public void toColumnType_ShouldFail_WhenUnknownDatatype() {
        final VOTableType colType = new VOTableType(VotDatatype.UNKNOWN);
        assertThrows(UnknownColumnTypeException.class, ()->converter.toColumnType(colType));
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenKnownDatatype() throws UnknownColumnTypeException {
        for(VotDatatype votDatatype : VotDatatype.values())
        {
            if (votDatatype == VotDatatype.UNKNOWN)
                continue;

            final ColumnType colType = converter.toColumnType(new VOTableType(votDatatype));
            assertNotNull(colType);

            Class<?> expectedClass;
            switch(votDatatype){
                case BOOLEAN:
                    expectedClass = TypeBoolean.class;
                    break;
                case BIT:
                    expectedClass = TypeBit.class;
                    break;
                case UNSIGNED_BYTE:
                    expectedClass = TypeUnsignedByte.class;
                    break;
                case SHORT:
                    expectedClass = TypeShort.class;
                    break;
                case INT:
                    expectedClass = TypeInteger.class;
                    break;
                case LONG:
                    expectedClass = TypeLong.class;
                    break;
                case CHAR:
                    expectedClass = TypeChar.class;
                    break;
                case UNICODE_CHAR:
                    expectedClass = TypeUnicodeChar.class;
                    break;
                case FLOAT:
                    expectedClass = TypeFloat.class;
                    break;
                case DOUBLE:
                    expectedClass = TypeDouble.class;
                    break;
                case FLOAT_COMPLEX:
                    expectedClass = TypeFloatComplex.class;
                    break;
                case DOUBLE_COMPLEX:
                    expectedClass = TypeDoubleComplex.class;
                    break;
                default:
                    expectedClass = null;
            }

            assertEquals(expectedClass, colType.getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenKnownSpecialType() throws UnknownColumnTypeException {
        // Given:
        final Map<VOTableType, Class<? extends ColumnType>> mapKnownAndExpectedTypes = new HashMap<>();
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "timestamp"    ), TypeTimestamp.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.SHORT , "2", "interval"     ), TypeIntervalOfShort.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.INT   , "2", "interval"     ), TypeIntervalOfInteger.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.LONG  , "2", "interval"     ), TypeIntervalOfLong.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.FLOAT , "2", "interval"     ), TypeIntervalOfFloat.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.DOUBLE, "2", "interval"     ), TypeIntervalOfDouble.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.SHORT , "2", "multiinterval"), TypeMultiIntervalOfShort.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.INT   , "2", "multiinterval"), TypeMultiIntervalOfInteger.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.LONG  , "2", "multiinterval"), TypeMultiIntervalOfLong.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.FLOAT , "2", "multiinterval"), TypeMultiIntervalOfFloat.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.DOUBLE, "2", "multiinterval"), TypeMultiIntervalOfDouble.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "hms"          ), TypeHMS.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "dms"          ), TypeDMS.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.FLOAT , "2", "point"        ), TypePointWithFloat.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.DOUBLE, "2", "point"        ), TypePointWithDouble.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.FLOAT , "3", "circle"       ), TypeCircleWithFloat.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.DOUBLE, "3", "circle"       ), TypeCircleWithDouble.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.FLOAT , "4", "range"        ), TypeRangeWithFloat.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.DOUBLE, "4", "range"        ), TypeRangeWithDouble.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.FLOAT , "*", "polygon"      ), TypePolygonWithFloat.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.DOUBLE, "*", "polygon"      ), TypePolygonWithDouble.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "moc"          ), TypeMOC.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "shape"        ), TypeShape.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "multishape"   ), TypeMultiShape.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "uri"          ), TypeURI.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "uuid"         ), TypeUUID.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "json"         ), TypeJSON.class);
        mapKnownAndExpectedTypes.put(new VOTableType(VotDatatype.CHAR  , "*", "region"       ), TypeRegion.class);

        for(Map.Entry<VOTableType, Class<? extends ColumnType>> entry : mapKnownAndExpectedTypes.entrySet())
        {
            // When:
            final ColumnType returnedType = converter.toColumnType(entry.getKey());

            // Then:
            assertNotNull(returnedType);
            assertEquals(entry.getValue(), returnedType.getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceedByIgnoringTheXType_WhenBLOB() throws UnknownColumnTypeException {
        for(String xtype : new String[]{"ADQL:BLOB", "BLOB"}) {
            final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.UNSIGNED_BYTE, "*", xtype));
            assertNotNull(colType);
            assertEquals(VectorType.class, colType.getClass());
            assertEquals(TypeUnsignedByte.class, ((VectorType) colType).getSubType().getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceedByIgnoringTheXType_WhenCLOB() throws UnknownColumnTypeException {
        for(String xtype : new String[]{"ADQL:CLOB", "CLOB"}) {
            final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.CHAR, "*", xtype));
            assertNotNull(colType);
            assertEquals(VectorType.class, colType.getClass());
            assertEquals(TypeChar.class, ((VectorType) colType).getSubType().getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenKnownSpecialTypeWithPrefix() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.CHAR, "*", "ADQL:TIMESTAMP"));
        assertNotNull(colType);
        assertEquals(TypeTimestamp.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSpecialTypeNeedsNormalization() throws UnknownColumnTypeException {
        for(String xtype : new String[]{"timestamp", "TIMESTAMP", "Timestamp", "   TimeStamp \t \n \r "}) {
            final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.CHAR, "*", xtype));
            assertNotNull(colType);
            assertEquals(TypeTimestamp.class, colType.getClass());
        }
    }

    @Test
    public void toColumnType_ShouldFail_WhenSpecialTypeWithIncorrectDatatype() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.FLOAT, "*", "timestamp"));
        assertNotNull(colType);
        assertNotEquals(TypeTimestamp.class, colType.getClass());
        assertEquals(VectorType.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceedAnyway_WhenSpecialTypeWithIncorrectArraysize() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.FLOAT, "2", "range"));
        assertNotNull(colType);
        assertEquals(TypeRangeWithFloat.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSimpleArray() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.DOUBLE, "2"));
        assertNotNull(colType);
        assertEquals(VectorType.class, colType.getClass());

        final VectorType arrayType = (VectorType)colType;
        assertEquals(TypeDouble.class, arrayType.getSubType().getClass());
        assertEquals("2", arrayType.getVectorSize());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenMultiDimensionalArray() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.DOUBLE, "2x3x*"));
        assertNotNull(colType);
        assertEquals(VectorType.class, colType.getClass());

        final VectorType arrayType = (VectorType)colType;
        assertEquals("2x3x*", arrayType.getVotArraysize().orElse(null));
        assertEquals("2x3x*", arrayType.getVectorSize());

        assertEquals(TypeDouble.class, arrayType.getSubType().getClass());
        assertFalse(arrayType.getSubType().getVotArraysize().isPresent());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenComplexArray() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.INT, "2x3", "interval"));
        assertNotNull(colType);
        assertEquals(VectorType.class, colType.getClass());

        final VectorType arrayType = (VectorType)colType;
        assertEquals("2x3", colType.getVotArraysize().orElse(null));
        assertEquals("3", arrayType.getVectorSize());

        assertEquals(TypeIntervalOfInteger.class, arrayType.getSubType().getClass());
        assertEquals("2", arrayType.getSubType().getVotArraysize().orElse(null));
    }

    @Test
    public void toColumnType_ShouldDegrade_WhenComplexArrayWithWeirdArraysize() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.INT, "4x3", "interval"));
        assertNotNull(colType);
        assertEquals(VectorType.class, colType.getClass());

        /* Note: When the arraysize of INTERVAL does not match the expected one,
        *        the expected one is set with 'x*'. */

        final VectorType arrayType = (VectorType)colType;
        assertEquals("2x*", colType.getVotArraysize().orElse(null));
        assertEquals("*", arrayType.getVectorSize());

        assertEquals(TypeIntervalOfInteger.class, arrayType.getSubType().getClass());
        assertEquals("2", arrayType.getSubType().getVotArraysize().orElse(null));
    }

    @Test
    public void toColumnType_ShouldDegrade_WhenUnknownSpecialType() throws UnknownColumnTypeException {
        final ColumnType colType = converter.toColumnType(new VOTableType(VotDatatype.DOUBLE, null, "so special"));
        assertNotNull(colType);
        assertEquals(TypeDouble.class, colType.getClass());

        /* Note: no more xtype ; it is just a normal DOUBLE value. */
    }


    /***************************************************************************
     * SUPPORT
     */

    @Test
    public void support_ShouldFail_WhenSupportingASecondTimeTheSameType(){
        try{
            new VOTableTypeConverterWithDuplicatedType();
            fail("It should be impossible to support a type already supported!");
        }
        catch(Exception ex){
            assertEquals(IncorrectDataTypeClass.class, ex.getClass());
            assertEquals("Cannot create a "+VOTableTypeConverterWithDuplicatedType.class.getSimpleName()+"! Cause: a similar datatype (i.e. '#char') is already supported!", ex.getMessage());
        }
    }

    private static class VOTableTypeConverterWithDuplicatedType extends VOTableTypeConverter {
        public VOTableTypeConverterWithDuplicatedType() {
            super();
            support(TypeChar.class);
        }
    }

    @Test
    public void support_ShouldFail_WhenTypeWithoutEmptyConstructor(){
        try{
            new VOTableTypeConverterWithNonEmptyTypeConstructor();
            fail("It should be impossible to support a type with no empty constructor!");
        }
        catch(Exception ex){
            assertEquals(IncorrectDataTypeClass.class, ex.getClass());
            assertEquals("Cannot create a "+VOTableTypeConverterWithNonEmptyTypeConstructor.class.getSimpleName()+"! Cause: impossible to create an instance of "+TypeWithoutEmptyConstructor.class.getName()+".", ex.getMessage());
        }
    }

    private static class VOTableTypeConverterWithNonEmptyTypeConstructor extends VOTableTypeConverter {
        public VOTableTypeConverterWithNonEmptyTypeConstructor() {
            super();
            support(TypeWithoutEmptyConstructor.class);
        }
    }

    private static class TypeWithoutEmptyConstructor extends ScalarType {

        public TypeWithoutEmptyConstructor(final String ignored){
            // No need to use it ; we just need to declare a non-empty constructor.
        }

        @Override
        public boolean isNumeric() {
            return false;
        }

        @Override
        public boolean isBoolean() {
            return false;
        }

        @Override
        public boolean isBinary() {
            return false;
        }

        @Override
        public boolean isString() {
            return false;
        }

        @Override
        public boolean isGeometry() {
            return false;
        }

        @Override
        public boolean isTime() {
            return false;
        }

        @Override
        public VotDatatype getVotDatatype() {
            return null;
        }

        @Override
        public Optional<String> getVotXtype() {
            return Optional.empty();
        }
    }

}
