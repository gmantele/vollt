package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeChar extends StringType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.CHAR; }
}
