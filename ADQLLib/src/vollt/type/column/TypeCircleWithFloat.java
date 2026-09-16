package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class TypeCircleWithFloat extends GeometryType {
    private static final String VOTABLE_ARRAYSIZE = "3";

    private static final String VOTABLE_XTYPE = "circle";

    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.FLOAT; }

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }
}
