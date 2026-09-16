package vollt.type.column.votable;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class TestVotDatatype {

    @Test
    public void fromString() {
        for(VotDatatype datatype : VotDatatype.values()){
            String expectedSerialization = null;
            switch(datatype){
                case BOOLEAN:        expectedSerialization = "boolean";
                    break;
                case BIT:            expectedSerialization = "bit";
                    break;
                case UNSIGNED_BYTE:  expectedSerialization = "unsignedByte";
                    break;
                case SHORT:          expectedSerialization = "short";
                    break;
                case INT:            expectedSerialization = "int";
                    break;
                case LONG:           expectedSerialization = "long";
                    break;
                case CHAR:           expectedSerialization = "char";
                    break;
                case UNICODE_CHAR:   expectedSerialization = "unicodeChar";
                    break;
                case FLOAT:          expectedSerialization = "float";
                    break;
                case DOUBLE:         expectedSerialization = "double";
                    break;
                case FLOAT_COMPLEX:  expectedSerialization = "floatComplex";
                    break;
                case DOUBLE_COMPLEX: expectedSerialization = "doubleComplex";
                    break;
                case UNKNOWN:        expectedSerialization = "unknown";
                    break;
                default:
                    fail("Untested VotDatatype value: \""+datatype+"\"!");
            }

            assertEquals(expectedSerialization, datatype.toString());
        }
    }

    @Test
    public void fromString_ShouldFail_WhenNullOrEmpty() {
        final String[] emptyDatatypes = new String[]{null, "", "  \t\n  "};
        for(String datatype : emptyDatatypes)
            assertThrows(NullPointerException.class, ()->VotDatatype.fromString(datatype));
    }

    @Test
    public void fromString_ShouldFail_WhenNotExisting() {
        assertFalse(VotDatatype.fromString("notExisting").isPresent());
    }

    @Test
    public void fromString_ShouldSucceed_WhenSimpleExistingDatatype() {
        final String[] booleanDatatypes = new String[]{"boolean", "  boolean  ", "BOOLEAN", "Boo Lean"};
        for(String datatype : booleanDatatypes) {
            final Optional<VotDatatype> foundDatatype = VotDatatype.fromString(datatype);
            assertTrue(foundDatatype.isPresent());
            assertEquals(VotDatatype.BOOLEAN, foundDatatype.get());
        }
    }

    @Test
    public void fromString_ShouldSucceed_WhenComplexExistingDatatype() {
        final String[] unicodeDatatypes = new String[]{"unicodechar", "  Unicode Char  ", "UNICODE_CHAR"};
        for(String datatype : unicodeDatatypes) {
            final Optional<VotDatatype> foundDatatype = VotDatatype.fromString(datatype);
            assertTrue(foundDatatype.isPresent());
            assertEquals(VotDatatype.UNICODE_CHAR, foundDatatype.get());
        }
    }

}