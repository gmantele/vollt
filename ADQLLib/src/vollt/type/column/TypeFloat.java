package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class TypeFloat extends FloatingType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.FLOAT; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
