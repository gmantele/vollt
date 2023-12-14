package vollt.datatype;

import java.util.Optional;

public class TypeDouble extends FloatingType {
    private static final String VOTABLE_DATATYPE = "double";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
