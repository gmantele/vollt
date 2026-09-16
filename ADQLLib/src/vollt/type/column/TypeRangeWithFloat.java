package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeRangeWithFloat extends TypeRange {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.FLOAT; }
}
