package vollt.datatype;

import java.util.Optional;

public class TypeCircleWithFloat extends GeometryType {
    private static final String VOTABLE_DATATYPE = "float";

    private static final String VOTABLE_ARRAYSIZE = "3";

    private static final String VOTABLE_XTYPE = "circle";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }
}
