package vollt.datatype;

import java.util.Optional;

public class TypeUnsignedByte extends ScalarType {
    private static final String VOTABLE_DATATYPE = "unsignedByte";

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
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
