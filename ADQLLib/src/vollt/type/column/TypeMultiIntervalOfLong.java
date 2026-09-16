package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeMultiIntervalOfLong extends TypeMultiInterval{
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.LONG; }
}
