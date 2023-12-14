package vollt.datatype;

import java.util.Optional;

public class TypeBit extends ScalarType{
    private static final String VOTABLE_DATATYPE = "bit";

    @Override
    public boolean isNumeric() { return true; }

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
