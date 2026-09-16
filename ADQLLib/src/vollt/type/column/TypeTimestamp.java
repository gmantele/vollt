package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class TypeTimestamp extends ScalarType {
    private static final String VOTABLE_ARRAYSIZE = "*";

    private static final String VOTABLE_XTYPE = "timestamp";

    @Override
    public boolean isNumeric() { return false; }

    @Override
    public boolean isBoolean() { return false; }

    @Override
    public boolean isBinary() { return false; }

    @Override
    public boolean isString() { return false; }

    @Override
    public boolean isGeometry() { return false; }

    @Override
    public boolean isTime() { return true; }

    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.CHAR; }

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }
}
