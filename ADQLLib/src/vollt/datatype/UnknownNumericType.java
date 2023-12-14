package vollt.datatype;

public class UnknownNumericType extends UnknownType {
    private static final String VOTABLE_DATATYPE = "double";

    public UnknownNumericType() {
        super(VOTABLE_DATATYPE, null, null);
    }

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
    public boolean isArray() { return false; }
}
