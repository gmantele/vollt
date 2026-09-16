package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeMultiIntervalOfInteger extends TypeMultiInterval{
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.INT; }
}
