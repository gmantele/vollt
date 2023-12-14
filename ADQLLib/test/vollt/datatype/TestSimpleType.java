package vollt.datatype;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSimpleType {


    /* **********************************************************************
     * *                          SERIALIZATION                             *
     * ********************************************************************** */

    @Test
    public void testToString_WithXType(){
        final String XTYPE = "timestamp";
        assertEquals(XTYPE, (new SimpleTypeImpl("char", "*", "timestamp")).toString());
    }

    @Test
    public void testToString_WithJustDatatype(){
        final String DATATYPE = "double";
        assertEquals(DATATYPE, (new SimpleTypeImpl(DATATYPE, null, null)).toString());
    }

    @Test
    public void testToString_ArrayWithNoXtype(){
        final String DATATYPE = "char";
        final String ARRAYSIZE = "80*";
        assertEquals(DATATYPE+"["+ARRAYSIZE+"]", (new SimpleTypeImpl(DATATYPE, ARRAYSIZE, null)).toString());
    }


    /* **********************************************************************
     * *                          CAN MERGE WITH                            *
     * ********************************************************************** */

    @Test
    public void testCanMergeWith_Null() {
        assertFalse((new SimpleTypeImpl()).canMergeWith(null));
    }

    @Test
    public void testCanMergeWith_DifferentType() {
        assertFalse((new SimpleTypeImpl()).canMergeWith(new SimpleTypeImpl2()));
    }

    @Test
    public void testCanMergeWith_SameType() {
        assertTrue((new SimpleTypeImpl()).canMergeWith(new SimpleTypeImpl()));
    }

    /* **********************************************************************
     * *                          MOCK TYPES                                *
     * ********************************************************************** */

    private static class SimpleTypeImpl extends SimpleType {

        private final String datatype;
        private final String arraysize;
        private final String xtype;

        public SimpleTypeImpl(){
            this(null, null, null);
        }

        public SimpleTypeImpl(final String datatype, final String arraysize, final String xtype){
            this.datatype  = datatype;
            this.arraysize = arraysize;
            this.xtype     = xtype;
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
        public boolean isArray() {
            return false;
        }

        public String getVotDatatype() { return datatype; }

        public Optional<String> getVotArraysize() { return Optional.ofNullable(arraysize); }

        public Optional<String> getVotXtype() { return Optional.ofNullable(xtype); }
    }

    /*
     * Implementation note:
     *   This second implementation extends the first one in order to also test
     *   the case where two types are the same at a higher level. Even in this
     *   case, they should not be considered as merge-able.
     */
    private static class SimpleTypeImpl2 extends SimpleTypeImpl {
    }

}