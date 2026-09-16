package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeIntervalOfDouble extends TypeInterval{
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.DOUBLE; }
}
