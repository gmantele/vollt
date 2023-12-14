package vollt.datatype;

import java.util.Optional;

public abstract class ScalarType extends SimpleType {
    @Override
    public final boolean isArray() { return false; }

    @Override
    public Optional<String> getVotArraysize() {
        return Optional.empty();
    }
}
