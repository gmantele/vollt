package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypePointWithDouble extends TypePoint {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.DOUBLE; }
}
