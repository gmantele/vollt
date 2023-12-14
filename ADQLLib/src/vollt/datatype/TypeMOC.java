package vollt.datatype;

import java.util.Optional;

public class TypeMOC extends ScalarType {

    private static final String VOTABLE_DATATYPE = "char";

    private static final String VOTABLE_ARRAYSIZE = "*";

    private static final String VOTABLE_XTYPE = "moc";

    @Override
    public boolean isNumeric() { return false; }

    @Override
    public boolean isBoolean() { return false; }

    @Override
    public boolean isBinary() { return false; }

    @Override
    public boolean isString() { return false; }

    @Override
    public boolean isGeometry() { return true; }

    @Override
    public boolean isTime() { return false; }

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }
}
