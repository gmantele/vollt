package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeRangeWithDouble extends TypeRange {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.DOUBLE; }
}
