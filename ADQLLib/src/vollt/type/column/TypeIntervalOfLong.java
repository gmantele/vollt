package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeIntervalOfLong extends TypeInterval{
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.LONG; }
}
