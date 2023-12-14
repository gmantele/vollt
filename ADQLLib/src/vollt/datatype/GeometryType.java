package vollt.datatype;

public abstract class GeometryType extends ScalarType {
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
    public boolean canMergeWith(final DataType otherDatatype) {
        return super.canMergeWith(otherDatatype)
                || (otherDatatype instanceof GeometryType);
    }
}
