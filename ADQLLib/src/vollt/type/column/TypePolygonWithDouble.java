package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

public class TypePolygonWithDouble extends TypePolygon {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.DOUBLE; }
}
