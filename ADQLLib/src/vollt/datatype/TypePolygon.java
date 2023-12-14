package vollt.datatype;

import java.util.Optional;

public abstract class TypePolygon extends GeometryType {

    private static final String VOTABLE_ARRAYSIZE = "*";

    private static final String VOTABLE_XTYPE = "polygon";

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }

    public static TypePolygon fromFloat(){ return new TypePolygonWithFloat(); }

    public static TypePolygon fromDouble(){ return new TypePolygonWithDouble(); }
}
