package vollt.type.column;

import org.junit.Test;
import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

import static org.junit.Assert.*;

public class TestSimpleType {


    /* **********************************************************************
     * *                          SERIALIZATION                             *
     * ********************************************************************** */

    @Test
    public void testToString_WithXType(){
        final String XTYPE = "timestamp";
        assertEquals(XTYPE, (new SimpleTypeImpl(VotDatatype.CHAR, "*", "timestamp")).toString());
    }

    @Test
    public void testToString_WithJustDatatype(){
        final VotDatatype datatype = VotDatatype.DOUBLE;
        assertEquals(datatype.toString(), (new SimpleTypeImpl(datatype, null, null)).toString());
    }

    @Test
    public void testToString_ArrayWithNoXtype(){
        final VotDatatype datatype = VotDatatype.CHAR;
        final String arraysize = "80*";
        assertEquals(datatype+"["+arraysize+"]", (new SimpleTypeImpl(datatype, arraysize, null)).toString());
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

    // TODO Complete tests of canMergeWith(...)

    /* **********************************************************************
     * *                          MOCK TYPES                                *
     * ********************************************************************** */

    private static class SimpleTypeImpl extends SimpleType {

        private final VotDatatype datatype;
        private final String arraysize;
        private final String xtype;

        public SimpleTypeImpl(){
            this(null, null, null);
        }

        public SimpleTypeImpl(final VotDatatype datatype, final String arraysize, final String xtype){
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

        public VotDatatype getVotDatatype() { return datatype; }

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