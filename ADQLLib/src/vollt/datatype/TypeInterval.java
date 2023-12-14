package vollt.datatype;

import java.util.Optional;

public abstract class TypeInterval extends ScalarType{

    private static final String VOTABLE_ARRAYSIZE = "2";

    private static final String VOTABLE_XTYPE = "interval";

    @Override
    public boolean isNumeric() { return true; }

    @Override
    public boolean isBoolean() { return false; }

    @Override
    public boolean isBinary() { return false; }

    @Override
    public boolean isString() { return false; }

    @Override
    public boolean isGeometry() { return false; }

    @Override
    public boolean isTime() { return false; }

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }

    public static TypeInterval fromShort(){ return new TypeIntervalOfShort(); }
    public static TypeInterval fromInteger(){ return new TypeIntervalOfInteger(); }
    public static TypeInterval fromLong(){ return new TypeIntervalOfLong(); }
    public static TypeInterval fromFloat(){ return new TypeIntervalOfFloat(); }
    public static TypeInterval fromDouble(){ return new TypeIntervalOfDouble(); }
}
