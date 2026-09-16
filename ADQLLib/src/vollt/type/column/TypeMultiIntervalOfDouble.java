package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeMultiIntervalOfDouble extends TypeMultiInterval{
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.DOUBLE; }
}
