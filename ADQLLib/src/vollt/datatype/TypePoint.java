package vollt.datatype;

import java.util.Optional;

public abstract class TypePoint extends GeometryType {
    private static final String VOTABLE_ARRAYSIZE = "2";

    private static final String VOTABLE_XTYPE = "point";

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }

    public static TypePoint fromFloat(){ return new TypePointWithFloat(); }

    public static TypePoint fromDouble(){ return new TypePointWithDouble(); }
}
