package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeMultiIntervalOfFloat extends TypeMultiInterval{
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.FLOAT; }
}
