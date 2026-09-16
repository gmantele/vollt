package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class TypeUnsignedByte extends ScalarType {
    @Override
    public boolean isNumeric() { return false; }

    @Override
    public boolean isBoolean() { return false; }

    @Override
    public boolean isBinary() { return true; }

    @Override
    public boolean isString() { return false; }

    @Override
    public boolean isGeometry() { return false; }

    @Override
    public boolean isTime() { return false; }

    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.UNSIGNED_BYTE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
