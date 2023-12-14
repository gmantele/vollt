package vollt.datatype;

import java.util.Optional;

public abstract class StringType extends ScalarType {
    @Override
    public boolean isNumeric() { return false; }

    @Override
    public boolean isBoolean() { return false; }

    @Override
    public boolean isBinary() { return false; }

    @Override
    public boolean isString() { return true; }

    @Override
    public boolean isGeometry() { return false; }

    @Override
    public boolean isTime() { return false; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
