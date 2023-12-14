package vollt.datatype;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (05 / 2023)
 */
public abstract class ExactNumericType extends ScalarType {
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
    public boolean canMergeWith(final DataType otherDatatype) {
        return super.canMergeWith(otherDatatype)
               || (otherDatatype instanceof ExactNumericType);
    }
}
