package vollt.datatype;

import java.util.Optional;

public class TypeFloat extends FloatingType {
    private static final String VOTABLE_DATATYPE = "float";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
