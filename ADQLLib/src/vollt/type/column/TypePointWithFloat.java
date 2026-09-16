package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypePointWithFloat extends TypePoint {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.FLOAT; }
}
