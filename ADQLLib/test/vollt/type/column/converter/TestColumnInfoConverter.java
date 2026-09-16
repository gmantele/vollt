package vollt.type.column.converter;

import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.votable.VOStarTable;
import vollt.type.column.*;
import vollt.type.column.converter.exception.UnknownColumnTypeException;
import vollt.type.column.votable.VOTableType;
import vollt.type.column.votable.VotDatatype;

import java.io.InputStream;

import static org.junit.Assert.*;

public class TestColumnInfoConverter {

    private static ColumnInfoConverter converter;

    @BeforeClass
    public static void initAll() {
        converter = new ColumnInfoConverter();
    }


    /***************************************************************************
     * FROM_COLUMN_TYPE
     */

    @Test
    public void fromColumnType_ShouldSucceed_WhenSimpleType() {
        final ColumnType[] supportedColumnTypes = new ColumnType[] {new TypeBoolean(), new TypeChar(), new TypeUnicodeChar(), new TypeDouble(), new TypeFloat(), new TypeInteger(), new TypeLong(), new TypeShort(), new TypeUnsignedByte()};
        final Class<?>[] expectedClasses = new Class[] {Boolean.class, Character.class, Character.class, Double.class, Float.class, Integer.class, Long.class, Short.class, Short.class};

        for(int i = 0; i < supportedColumnTypes.length; i++)
        {
            // Given:
            final ColumnType simpleType = supportedColumnTypes[i];

            // When:
            final ColumnInfo colInfo = converter.fromColumnType(simpleType);

            // Then:
            assertEquals("-", colInfo.getName());
            assertNull(colInfo.getDescription());
            assertEquals(0, colInfo.getShape().length);
            assertEquals(expectedClasses[i], colInfo.getContentClass());
        }
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenVariableLengthString() {
        // Given:
        final ColumnType stringType = ColumnTypeFactory.createString();

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(stringType);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals( 0, colInfo.getShape().length);
        assertEquals(-1, colInfo.getElementSize());
        assertEquals(String.class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenFixedLengthString() {
        for(ScalarType charType : new ScalarType[]{new TypeChar(), new TypeUnicodeChar()})
        {
            // Given:
            final int stringLength = 3;
            final ColumnType stringType = new VectorType(charType, Integer.toString(stringLength));

            // When:
            final ColumnInfo colInfo = converter.fromColumnType(stringType);

            // Then:
            assertEquals("-", colInfo.getName());
            assertNull(colInfo.getDescription());
            assertEquals(0, colInfo.getShape().length);
            assertEquals(stringLength, colInfo.getElementSize());
            assertEquals(String.class, colInfo.getContentClass());
        }
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenMultidimensionalString() {
        // Given:
        final int stringLength = 3;
        final String[] otherDimensions = new String[]{"2", "-1"};
        String arraysize = Integer.toString(stringLength);
        for(String dim : otherDimensions)
            arraysize += "x" + ("-1".equals(dim) ? "*" : dim);
        final ColumnType stringType = new VectorType(new TypeChar(), arraysize);

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(stringType);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(stringLength, colInfo.getElementSize());
        assertEquals(otherDimensions.length, colInfo.getShape().length);
        for(int i=0; i<otherDimensions.length; i++)
            assertEquals(otherDimensions[i] , Integer.toString(colInfo.getShape()[i]));
        assertEquals(String.class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenUnknownType() {
        // Given:
        final ColumnType simpleType = new UnknownType();

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(simpleType);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(1, colInfo.getShape().length);
        assertEquals(-1, colInfo.getShape()[0]);
        assertEquals(String.class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldFallback_WhenUnknownNumericType() {
        // Given:
        final ColumnType simpleType = new UnknownNumericType();

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(simpleType);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(0, colInfo.getShape().length);
        assertEquals(Double.class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenSimpleArray() {
        final ScalarType[] supportedColumnTypes = new ScalarType[] {new TypeBit(), new TypeBoolean(), new TypeDouble(), new TypeFloat(), new TypeInteger(), new TypeLong(), new TypeShort(), new TypeUnsignedByte()};
        final Class<?>[] expectedClasses = new Class[] {boolean[].class, boolean[].class, double[].class, float[].class, int[].class, long[].class, short[].class, short[].class};

        for(int i = 0; i < supportedColumnTypes.length; i++)
        {
            // Given:
            final ColumnType simpleArray = new VectorType(supportedColumnTypes[i], "2");

            // When:
            final ColumnInfo colInfo = converter.fromColumnType(simpleArray);

            // Then:
            assertEquals("-", colInfo.getName());
            assertNull(colInfo.getDescription());
            assertEquals(1, colInfo.getShape().length);
            assertEquals(2, colInfo.getShape()[0]);
            assertEquals(expectedClasses[i], colInfo.getContentClass());
        }
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenSimpleArrayOfUnknownNumeric() {
        // Given:
        final ColumnType simpleType = new UnknownNumericType("buzz", "3", null);

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(simpleType);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(1, colInfo.getShape().length);
        assertEquals(3, colInfo.getShape()[0]);
        assertEquals(Double.class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenSimpleButUnboundedArray() {
        // Given:
        final ColumnType simpleArray = new VectorType(new TypeDouble(), "*");

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(simpleArray);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(1, colInfo.getShape().length);
        assertEquals(-1, colInfo.getShape()[0]);
        assertEquals(double[].class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldFallback_WhenNotNumberArraysize() {
        // Given:
        final ColumnType simpleArray = new VectorType(new TypeDouble(), "T");

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(simpleArray);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(1, colInfo.getShape().length);
        assertEquals(-1, colInfo.getShape()[0]);
        assertEquals(double[].class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenMultiDimensionalArray() throws UnknownColumnTypeException {
        // Given:
        final ColumnType multiDimArray = ColumnTypeFactory.fromVOTable(new VOTableType(VotDatatype.DOUBLE, "2x3x4", null));

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(multiDimArray);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(3, colInfo.getShape().length);
        assertEquals(2, colInfo.getShape()[0]);
        assertEquals(3, colInfo.getShape()[1]);
        assertEquals(4, colInfo.getShape()[2]);
        assertEquals(double[].class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldSucceed_WhenUnboundedMultiDimensionalArray() throws UnknownColumnTypeException {
        // Given:
        final ColumnType multiDimArray = ColumnTypeFactory.fromVOTable(new VOTableType(VotDatatype.DOUBLE, "2x3x*", null));

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(multiDimArray);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(3, colInfo.getShape().length);
        assertEquals(2, colInfo.getShape()[0]);
        assertEquals(3, colInfo.getShape()[1]);
        assertEquals(-1, colInfo.getShape()[2]);
        assertEquals(double[].class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldFallback_WhenIncorrectMultiDimensionalArray() throws UnknownColumnTypeException {
        // Given:
        final ColumnType multiDimArray = ColumnTypeFactory.fromVOTable(new VOTableType(VotDatatype.DOUBLE, "2x*x3", null));

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(multiDimArray);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(3, colInfo.getShape().length);
        assertEquals(2, colInfo.getShape()[0]);
        assertEquals(Integer.MAX_VALUE, colInfo.getShape()[1]);
        assertEquals(3, colInfo.getShape()[2]);
        assertEquals(double[].class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldFallback_WhenNotNumberMultiDimensionalArray() throws UnknownColumnTypeException {
        // Given:
        final ColumnType multiDimArray = ColumnTypeFactory.fromVOTable(new VOTableType(VotDatatype.DOUBLE, "2x3xT", null));

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(multiDimArray);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(3, colInfo.getShape().length);
        assertEquals(2, colInfo.getShape()[0]);
        assertEquals(3, colInfo.getShape()[1]);
        assertEquals(-1, colInfo.getShape()[2]);
        assertEquals(double[].class, colInfo.getContentClass());
    }

    @Test
    public void fromColumnType_ShouldFallback_WhenNotNumberInMiddleOfMultiDimensionalArray() throws UnknownColumnTypeException {
        // Given:
        final ColumnType multiDimArray = ColumnTypeFactory.fromVOTable(new VOTableType(VotDatatype.DOUBLE, "2xTx3", null));

        // When:
        final ColumnInfo colInfo = converter.fromColumnType(multiDimArray);

        // Then:
        assertEquals("-", colInfo.getName());
        assertNull(colInfo.getDescription());
        assertEquals(3, colInfo.getShape().length);
        assertEquals(2, colInfo.getShape()[0]);
        assertEquals(Integer.MAX_VALUE, colInfo.getShape()[1]);
        assertEquals(3, colInfo.getShape()[2]);
        assertEquals(double[].class, colInfo.getContentClass());
    }


    /***************************************************************************
     * TO_COLUMN_TYPE
     */

    @Test
    public void toColumnType_ShouldFail_WhenNull() {
        assertThrows(NullPointerException.class, () -> converter.toColumnType(null));
    }

    @Test
    public void toColumnType_ShouldFail_WhenImpossibleToConvert(){
        final ColumnInfo colInfo = new ColumnInfo("col", InputStream.class, "");
        assertThrows(UnknownColumnTypeException.class, () -> converter.toColumnType(colInfo));
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSimpleScalarTypes() throws UnknownColumnTypeException {
        final Class<?>[] inputScalarTypes     = new Class[]{Boolean.class    , Byte.class     , Short.class    , Integer.class    , Long.class    , Float.class    , Double.class};
        final Class<?>[] expectedTypes        = new Class[]{TypeBoolean.class, TypeShort.class, TypeShort.class, TypeInteger.class, TypeLong.class, TypeFloat.class, TypeDouble.class};

        for(int i=0; i<inputScalarTypes.length; i++)
        {
            // Given:
            final ColumnInfo colInfo = new ColumnInfo("col", inputScalarTypes[i], "");

            // When:
            final ColumnType colType = converter.toColumnType(colInfo);

            // Then:
            assertNotNull(colType);
            assertEquals(expectedTypes[i], colType.getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenScalarUnsignedByte() throws UnknownColumnTypeException {
        for (Class<?> inputScalarType : new Class[]{Byte.class, Short.class})
        {
            // Given:
            final ColumnInfo colInfo = new ColumnInfo("col", inputScalarType, "");
            colInfo.setAuxDatum(new DescribedValue(Tables.UBYTE_FLAG_INFO, Boolean.TRUE));

            // When:
            final ColumnType colType = converter.toColumnType(colInfo);

            // Then:
            assertNotNull(colType);
            assertEquals(TypeUnsignedByte.class, colType.getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSimpleVector() throws UnknownColumnTypeException {
        final Class<?>[] inputScalarTypes   = new Class[]{boolean[].class  , byte[].class   , short[].class  , int[].class      , long[].class  , float[].class  , double[].class};
        final Class<?>[] expectedInnerTypes = new Class[]{TypeBoolean.class, TypeShort.class, TypeShort.class, TypeInteger.class, TypeLong.class, TypeFloat.class, TypeDouble.class};

        final int arraysize            = 42;
        final String expectedArraysize = Integer.toString(arraysize);

        for(int i=0; i<inputScalarTypes.length; i++)
        {
            // Given:
            final ColumnInfo colInfo = new ColumnInfo("col", inputScalarTypes[i], "");
            colInfo.setShape(new int[]{arraysize});

            // When:
            final ColumnType colType = converter.toColumnType(colInfo);

            // Then:
            assertNotNull(colType);
            assertEquals(VectorType.class, colType.getClass());
            assertEquals(expectedArraysize, colType.getVotArraysize().orElse(null));
            assertEquals(expectedInnerTypes[i], ((VectorType)colType).getSubType().getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSimpleMultidimensional() throws UnknownColumnTypeException {
        final Class<?>[] inputScalarTypes   = new Class[]{boolean[].class  , byte[].class   , short[].class  , int[].class      , long[].class  , float[].class  , double[].class};
        final Class<?>[] expectedInnerTypes = new Class[]{TypeBoolean.class, TypeShort.class, TypeShort.class, TypeInteger.class, TypeLong.class, TypeFloat.class, TypeDouble.class};

        final int[] shape              = {3,2,-1};
        final String expectedArraysize = "3x2x*";

        for(int i=0; i<inputScalarTypes.length; i++)
        {
            // Given:
            final ColumnInfo colInfo = new ColumnInfo("col", inputScalarTypes[i], "");
            colInfo.setShape(shape);

            // When:
            final ColumnType colType = converter.toColumnType(colInfo);

            // Then:
            assertNotNull(colType);
            assertEquals(VectorType.class, colType.getClass());
            assertEquals(expectedArraysize, colType.getVotArraysize().orElse(null));
            assertEquals(expectedInnerTypes[i], ((VectorType)colType).getSubType().getClass());
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSingleCharacter() throws UnknownColumnTypeException {
        // Given: => in VOTable: 'name="col" datatype="char"'
        final ColumnInfo colInfo = new ColumnInfo("col", Character.class, "");

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypeChar.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSingleArrayOfCharacter() throws UnknownColumnTypeException {
        // Given: => in VOTable: 'name="col" datatype="char" arraysize="1"'
        final ColumnInfo colInfo = new ColumnInfo("col", Character.class, "");
        colInfo.setElementSize(1);

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypeChar.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenFixedLengthString() throws UnknownColumnTypeException {
        for(Class<?> contentClass : new Class[]{Character.class, String.class})
        {
            // Given: => in VOTable: 'name="col" datatype="char" arraysize="12"'
            final ColumnInfo colInfo = new ColumnInfo("col", contentClass, "");
            colInfo.setElementSize(12);

            // When:
            final ColumnType colType = converter.toColumnType(colInfo);

            // Then:
            assertNotNull(colType);
            assertEquals(VectorType.class, colType.getClass());
            assertEquals(TypeChar.class, ((VectorType) colType).getSubType().getClass());
            assertEquals("12", colType.getVotArraysize().orElse(null));
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenVariableLengthString() throws UnknownColumnTypeException {
        // Given: => in VOTable: 'name="col" datatype="char" arraysize="*"'
        final ColumnInfo colInfo = new ColumnInfo("col", String.class, "");

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(VectorType.class, colType.getClass());
        assertEquals(TypeChar.class, ((VectorType)colType).getSubType().getClass());
        assertEquals("*", colType.getVotArraysize().orElse(null));
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSpecialVariableLengthString() throws UnknownColumnTypeException {
        for(Class<?> contentClass : new Class[]{Character.class, String.class})
        {
            // Given: => in VOTable: 'name="col" datatype="char" arraysize="0"'
            final ColumnInfo colInfo = new ColumnInfo("col", contentClass, "");
            colInfo.setElementSize(0);

            // When:
            final ColumnType colType = converter.toColumnType(colInfo);

            // Then:
            assertNotNull(colType);
            assertEquals(VectorType.class, colType.getClass());
            assertEquals(TypeChar.class, ((VectorType) colType).getSubType().getClass());
            assertEquals("*", colType.getVotArraysize().orElse(null));
        }
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenMultidimensionalString() throws UnknownColumnTypeException {
        // Given: => in VOTable: 'name="col" datatype="char" arraysize="3x2x*"'
        final ColumnInfo colInfo = new ColumnInfo("col", String[].class, "");
        colInfo.setElementSize(3);
        colInfo.setShape(new int[]{2,-1});

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(VectorType.class, colType.getClass());
        assertEquals(TypeChar.class, ((VectorType)colType).getSubType().getClass());
        assertEquals("3x2x*", colType.getVotArraysize().orElse(null));
    }

    @Test
    public void toColumnType_ShouldFail_WhenVariableMultidimensionalString() throws UnknownColumnTypeException {
        // Given: => in VOTable: 'name="col" datatype="char" arraysize="*x2"'
        final ColumnInfo colInfo = new ColumnInfo("col", String[].class, "");
        colInfo.setElementSize(-1);
        colInfo.setShape(new int[]{2});

        // When + Then:
        assertThrows(UnknownColumnTypeException.class, () -> converter.toColumnType(colInfo));
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSingleUnicodeCharacter() throws UnknownColumnTypeException {
        // Given:
        final ColumnInfo colInfo = new ColumnInfo("col", Character.class, "");
        colInfo.setAuxDatum(new DescribedValue(VOStarTable.DATATYPE_INFO, "unicodeChar"));

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypeUnicodeChar.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSingleItemArrayOfUnicodeCharacter() throws UnknownColumnTypeException {
        // Given:
        final ColumnInfo colInfo = new ColumnInfo("col", Character.class, "");
        colInfo.setAuxDatum(new DescribedValue(VOStarTable.DATATYPE_INFO, "unicodeChar"));
        colInfo.setShape(new int[]{1});

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypeUnicodeChar.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSingleFloatComplex() throws UnknownColumnTypeException {
        // Given: => datatype="floatComplex"
        final ColumnInfo colInfo = new ColumnInfo("col", float[].class, "");
        colInfo.setAuxDatum(new DescribedValue(VOStarTable.DATATYPE_INFO, "floatComplex"));
        colInfo.setShape(new int[]{2});

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypeFloatComplex.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSingleDoubleComplex() throws UnknownColumnTypeException {
        // Given: => datatype="doubleComplex"
        final ColumnInfo colInfo = new ColumnInfo("col", double[].class, "");
        colInfo.setAuxDatum(new DescribedValue(VOStarTable.DATATYPE_INFO, "doubleComplex"));
        colInfo.setShape(new int[]{2});

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypeDoubleComplex.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenSimpleXtype_Timestamp() throws UnknownColumnTypeException {
        // Given:
        final ColumnInfo colInfo = new ColumnInfo("col", String.class, "");
        colInfo.setXtype("Timestamp");

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypeTimestamp.class, colType.getClass());
    }

    @Test
    public void toColumnType_ShouldSucceed_WhenComplexXtype_Point() throws UnknownColumnTypeException {
        // Given:
        final ColumnInfo colInfo = new ColumnInfo("col", double[].class, "");
        colInfo.setXtype("point");
        colInfo.setShape(new int[]{2});

        // When:
        final ColumnType colType = converter.toColumnType(colInfo);

        // Then:
        assertNotNull(colType);
        assertEquals(TypePointWithDouble.class, colType.getClass());
    }

    /**
     * This test aims to ensure the conversion is bijective.
     */
    @Test
    public void toAndFromColumnType_ShouldSucceed_WhenConvertingBackAndForth() throws UnknownColumnTypeException {
        // Given:
        for (ColumnType originType : new ColumnType[]{
                new TypeInteger(),
                new TypeUnsignedByte(),
                new TypeUnicodeChar(),
                new TypeFloatComplex(),
                new TypeDoubleComplex(),
                new VectorType(new TypeInteger(), "3x2"),
                ColumnTypeFactory.createString(),
                new VectorType(new TypeChar(), "4"),
                new VectorType(new TypeUnicodeChar(), "*"),
                new VectorType(new TypeUnicodeChar(), "3x2x*"),
                new TypeTimestamp(),
                new TypePointWithDouble(),
                new VectorType(new TypeFloatComplex(), "3")})
        {
            // When:
            final ColumnInfo destType         = converter.fromColumnType(originType);
            final ColumnType backToOriginType = converter.toColumnType(destType);

            // Then:
            assertEquals(originType.getClass(), backToOriginType.getClass());
            assertEquals(originType.getVotArraysize(), backToOriginType.getVotArraysize());
        }
    }

}