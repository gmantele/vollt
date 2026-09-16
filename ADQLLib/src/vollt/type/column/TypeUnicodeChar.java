package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypeUnicodeChar extends StringType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.UNICODE_CHAR; }
}
