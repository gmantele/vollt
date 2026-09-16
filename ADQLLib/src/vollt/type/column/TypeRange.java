package vollt.type.column;

import java.util.Optional;

public abstract class TypeRange extends GeometryType {

    private static final String VOTABLE_ARRAYSIZE = "*";

    private static final String VOTABLE_XTYPE = "range";

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }

    public static TypeRange fromFloat(){ return new TypeRangeWithFloat(); }

    public static TypeRange fromDouble(){ return new TypeRangeWithDouble(); }
}
