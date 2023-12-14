package vollt.datatype;

public class TypeChar extends StringType {
    private static final String VOTABLE_DATATYPE = "char";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }
}
