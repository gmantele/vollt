package vollt.datatype;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestDataTypeFactoryImp {

    /* **********************************************************************
     * *                          FROM_VOTABLE                              *
     * ********************************************************************** */

    @Test
    public void testFromVOTable_WithNoDatatype(){
        final DataType type = (new DataTypeFactoryImp()).fromVOTable(null, null, null);
        assertNotNull(type);
        assertEquals(UnknownType.class, type.getClass());
        assertEquals("char", type.getVotDatatype());
        assertTrue(type.getVotArraysize().isPresent());
        assertEquals("*", type.getVotArraysize().get());
        assertFalse(type.getVotXtype().isPresent());
    }

    @Test
    public void testFromVOTable_WithUnknownDatatype(){
        final String UNKNOWN_DATATYPE = "unknown";
        final DataType type = (new DataTypeFactoryImp()).fromVOTable(UNKNOWN_DATATYPE, null, null);
        assertNotNull(type);
        assertEquals(UnknownType.class, type.getClass());
        assertEquals(UNKNOWN_DATATYPE, type.getVotDatatype());
        assertFalse(type.getVotArraysize().isPresent());
        assertFalse(type.getVotXtype().isPresent());
    }

    @Test
    public void testFromVOTable_WithKnownDatatype(){
        final DataType type = (new DataTypeFactoryImp()).fromVOTable("double", null, null);
        assertNotNull(type);
        assertEquals(TypeDouble.class, type.getClass());
    }

    @Test
    public void testFromVOTable_WithKnownXtype(){
        final DataType type = (new DataTypeFactoryImp()).fromVOTable("char", "*", "timestamp");
        assertNotNull(type);
        assertEquals(TypeTimestamp.class, type.getClass());
    }

    @Test
    public void testFromVOTable_WithUnknownXtypeButKnownDatatype(){
        final String UNKNOWN_XTYPE = "long_text";
        final DataType type = (new DataTypeFactoryImp()).fromVOTable("char", null, UNKNOWN_XTYPE);
        assertNotNull(type);
        assertEquals(TypeChar.class, type.getClass());
        assertFalse(type.getVotArraysize().isPresent());
        assertFalse(type.getVotXtype().isPresent());
    }

    @Test
    public void testFromVOTable_WithKnownXtypeButIncorrectDatatype(){
        final DataType type = (new DataTypeFactoryImp()).fromVOTable("long", null, "timestamp");
        assertNotNull(type);
        assertEquals(TypeLong.class, type.getClass());
        assertFalse(type.getVotArraysize().isPresent());
        assertFalse(type.getVotXtype().isPresent());
    }

    @Test
    public void testFromVOTable_WithUnknownXtypeAndUnknownDatatype(){
        final String UNKNOWN_DATATYPE = "unknown";
        final String UNKNOWN_XTYPE = "long_text";
        final DataType type = (new DataTypeFactoryImp()).fromVOTable(UNKNOWN_DATATYPE, null, UNKNOWN_XTYPE);
        assertNotNull(type);
        assertEquals(UnknownType.class, type.getClass());
        assertEquals(UNKNOWN_DATATYPE, type.getVotDatatype());
        assertFalse(type.getVotArraysize().isPresent());
        assertTrue(type.getVotXtype().isPresent());
        assertEquals(UNKNOWN_XTYPE, type.getVotXtype().get());
    }

    @Test
    public void testFromVOTable_WithVectorOfSimpleType(){
        final DataType type = (new DataTypeFactoryImp()).fromVOTable("long", "3", null);
        assertNotNull(type);
        assertEquals(VectorType.class, type.getClass());
        assertEquals(TypeLong.class, ((VectorType)type).subType.getClass());
        assertTrue(type.getVotArraysize().isPresent());
        assertEquals("3", type.getVotArraysize().get());
        assertFalse(type.getVotXtype().isPresent());
    }

    @Test
    public void testFromVOTable_WithVectorOfSpecialType(){
        final DataType type = (new DataTypeFactoryImp()).fromVOTable("double", "2x*", " POINT  ");
        assertNotNull(type);
        assertEquals(VectorType.class, type.getClass());
        assertEquals(TypePointWithDouble.class, ((VectorType)type).subType.getClass());
        assertTrue(type.getVotArraysize().isPresent());
        assertEquals("2x*", type.getVotArraysize().get());
        assertTrue(type.getVotXtype().isPresent());
        assertEquals("point", type.getVotXtype().get());
    }

    /* **********************************************************************
     * *                             GET_TYPE                               *
     * ********************************************************************** */

    @Test
    public void testGetType_WithNoDatatype(){
        assertNull((new DataTypeFactoryImp()).getType(null, null));
    }

    @Test
    public void testGetType_WithNoXtype(){
        final DataType type = (new DataTypeFactoryImp()).getType("char", null);
        assertNotNull(type);
        assertEquals(TypeChar.class, type.getClass());
    }

    @Test
    public void testGetType_WithUnknownDatatype(){
        assertNull((new DataTypeFactoryImp()).getType("unknown", null));
    }

    @Test
    public void testGetType_WithUnknownXtype(){
        assertNull((new DataTypeFactoryImp()).getType("char", "unknown"));
    }

    /* **********************************************************************
     * *                             IS_VECTOR                              *
     * ********************************************************************** */

    @Test
    public void testIsVector_WithNoType(){
        try{
            (new DataTypeFactoryImp()).isVector(null, null);
        }catch(Throwable t){
            assertEquals(NullPointerException.class, t.getClass());
        }
    }

    @Test
    public void testIsVector_WithNoArraysize(){
        for(String str : new String[]{null, "", "    \n "})
            assertFalse((new DataTypeFactoryImp()).isVector(new TypeTimestamp(), str));
    }

    @Test
    public void testIsVector_ScalarWithArraysizeButNotAVector(){
        assertFalse((new DataTypeFactoryImp()).isVector(TypePoint.fromDouble(), "*"));
    }

    @Test
    public void testIsVector_SpecialTypeWithArraysizeAndAVector(){
        assertTrue((new DataTypeFactoryImp()).isVector(TypePoint.fromFloat(), "2x*"));
    }

    @Test
    public void testIsVector_ScalarWithArraysizeAndAVector(){
        assertTrue((new DataTypeFactoryImp()).isVector(new TypeDouble(), "3"));
    }

    /* **********************************************************************
     * *                             SUPPORT                                *
     * ********************************************************************** */

    @Test
    public void testSupport_WithNull(){
        try{
            new DataTypeFactoryImp();
        }catch(Throwable t){
            assertEquals(NullPointerException.class, t.getClass());
        }
    }

    @Test
    public void testSupport_WithMissingEmptyConstructor(){
        try{
            (new DataTypeFactoryImp()).support(TypeScalarMock.class);
        }catch(Throwable t){
            assertEquals(IncorrectDataTypeClass.class, t.getClass());
            assertEquals("Cannot create a DataTypeFactoryImp! Cause: impossible to create an instance of "+TypeScalarMock.class.getName()+".", t.getMessage());
        }
    }

    /* **********************************************************************
     * *                     DATATYPE NORMALIZATION                         *
     * ********************************************************************** */

    @Test
    public void testNormalizeDatatype_WithNull(){
        assertNull((new DataTypeFactoryImp()).normalizeDatatype(null));
    }

    @Test
    public void testNormalizeDatatype_WithEmptyString(){
        for(String str : new String[]{"", "     \n "})
            assertNull((new DataTypeFactoryImp()).normalizeDatatype(str));
    }

    @Test
    public void testNormalizeDatatype(){
        assertEquals("double", (new DataTypeFactoryImp()).normalizeDatatype("   double "));
    }

    /* **********************************************************************
     * *                        XTYPE NORMALIZATION                         *
     * ********************************************************************** */

    @Test
    public void testNormalizeXtype_WithNull(){
        assertNull((new DataTypeFactoryImp()).normalizeXtype(null));
    }

    @Test
    public void testNormalizeXtype_WithEmptyString(){
        for(String str : new String[]{"", "     \n "})
            assertNull((new DataTypeFactoryImp()).normalizeXtype(str));
    }

    @Test
    public void testNormalizeXtype_WithJustPrefixSeparator(){
        assertNull((new DataTypeFactoryImp()).normalizeXtype(":"));
    }

    @Test
    public void testNormalizeXtype_WithPrefix(){
        assertEquals("point", (new DataTypeFactoryImp()).normalizeXtype(" adql : POINT\n"));
    }

    @Test
    public void testNormalizeXtype_WithNoPrefix(){
        assertEquals("point", (new DataTypeFactoryImp()).normalizeXtype("   POINT "));
    }

    /* **********************************************************************
     * *                            MOCK TYPE                               *
     * ********************************************************************** */

    private static class TypeScalarMock extends ScalarType {
        public TypeScalarMock(final boolean uselessAttribute){}

        @Override
        public boolean isNumeric() { return false; }

        @Override
        public boolean isBoolean() { return false; }

        @Override
        public boolean isBinary() { return false; }

        @Override
        public boolean isString() { return false; }

        @Override
        public boolean isGeometry() { return false; }

        @Override
        public boolean isTime() { return false; }

        @Override
        public String getVotDatatype() { return "mock"; }

        @Override
        public Optional<String> getVotXtype() { return Optional.empty(); }
    }

}