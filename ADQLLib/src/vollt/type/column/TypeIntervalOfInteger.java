package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeIntervalOfInteger extends TypeInterval{
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.INT; }
}
