package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class TypeFloatComplex extends FloatingType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.FLOAT_COMPLEX; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
