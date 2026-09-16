package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class TypeDoubleComplex extends FloatingType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.DOUBLE_COMPLEX; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
