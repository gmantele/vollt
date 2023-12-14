package vollt.datatype;

public class TypeUnicodeChar extends StringType {
    private static final String VOTABLE_DATATYPE = "unicodeChar";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }
}
