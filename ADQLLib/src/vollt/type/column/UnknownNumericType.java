package vollt.type.column;

public class UnknownNumericType extends UnknownType {
    private static final String VOTABLE_DATATYPE = "double";

    public UnknownNumericType() {
        super(VOTABLE_DATATYPE, null, null);
    }

    public UnknownNumericType(final String unrecognizedDatatype, final String arraysize, final String xtype) {
        super(unrecognizedDatatype != null && !unrecognizedDatatype.trim().isEmpty() ? unrecognizedDatatype : VOTABLE_DATATYPE, arraysize, xtype);
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
