package vollt.type.column;

import java.util.Optional;

public abstract class TypeMultiInterval extends ScalarType{

    private static final String VOTABLE_ARRAYSIZE = "*";

    private static final String VOTABLE_XTYPE = "multiinterval";

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

    public static TypeMultiInterval fromShort()  { return new TypeMultiIntervalOfShort(); }
    public static TypeMultiInterval fromInteger(){ return new TypeMultiIntervalOfInteger(); }
    public static TypeMultiInterval fromLong()   { return new TypeMultiIntervalOfLong(); }
    public static TypeMultiInterval fromFloat()  { return new TypeMultiIntervalOfFloat(); }
    public static TypeMultiInterval fromDouble() { return new TypeMultiIntervalOfDouble(); }
}
