package vollt.type.column.votable;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestVOTableType {

    @Test
    public void constructor_ShouldFail_WhenNoDatatype(){
        assertThrows(NullPointerException.class, () -> new VOTableType((VotDatatype) null, "1", "TIMESTAMP"));
        assertThrows(NullPointerException.class, () -> new VOTableType((String) null, "1", "TIMESTAMP"));
    }

    @Test
    public void constructor_ShouldFail_WhenUnsupportedDatatype(){
        final String unknownDatatype = "timestamp";
        try{
            new VOTableType(unknownDatatype, null, null);
        }catch(Throwable t){
            assertEquals(IllegalArgumentException.class, t.getClass());
            assertEquals("Unknown VOTable datatype: \""+unknownDatatype+"\"!", t.getMessage());
        }
    }

    @Test
    public void constructor_ShouldSucceed_WhenKnownDatatype(){
        for(String strType : new String[]{"int", "unicode char", "float_complex", "  Double Complex "}) {
            try {
                new VOTableType(strType, null, null);
            }catch(Throwable t){
                fail("'"+strType+"' is a valid VOTable datatype! This test should have succeeded.");
            }
        }
    }

    @Test
    public void constructor_ShouldLowerCaseArraysize_WhenAnArraySizeIsGiven(){
        final VOTableType type = new VOTableType("int", " \t 2X2 \n ", null);
        assertTrue(type.getArraysize().isPresent());
        assertEquals("2x2", type.getArraysize().get());
    }

    @Test
    public void constructor_ShouldUpperCaseXType_WhenAnXTypeIsGiven(){
        final VOTableType type = new VOTableType("int", null, "Timestamp");
        assertTrue(type.getXtype().isPresent());
        assertEquals("TIMESTAMP", type.getXtype().get());
    }

    @Test
    public void toString_ShouldReturnXType_WhenProvided(){
        final VOTableType type = new VOTableType(VotDatatype.CHAR, null, "adql:CLOB");
        assertEquals("ADQL:CLOB (char)", type.toString());
    }

    @Test
    public void toString_ShouldIncludeArraysize_WhenProvided(){
        final VOTableType type = new VOTableType(VotDatatype.CHAR, "5");
        assertEquals("char[5]", type.toString());
    }

}