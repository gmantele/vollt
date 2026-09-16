package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class TypeDouble extends FloatingType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.DOUBLE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
