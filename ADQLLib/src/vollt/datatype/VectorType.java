package vollt.datatype;

import java.util.Optional;

public class VectorType extends SimpleType {
    protected final ScalarType subType;

    protected final String arraysize;

    public VectorType(final ScalarType subType, final String arraysize) throws NullPointerException, IllegalArgumentException {
        if (subType == null)
            throw new NullPointerException("Missing vector sub-type!");

        if (arraysize == null)
            throw new NullPointerException("Missing vector size!");
        else if (arraysize.trim().length() == 0)
            throw new IllegalArgumentException("Empty vector size!");

        this.subType   = subType;
        this.arraysize = arraysize.trim();
    }

    @Override
    public boolean isNumeric() { return subType.isNumeric(); }

    @Override
    public boolean isBoolean() { return subType.isBoolean(); }

    @Override
    public boolean isBinary() { return subType.isBinary(); }

    @Override
    public boolean isString() { return subType.isString(); }

    @Override
    public boolean isGeometry() { return subType.isGeometry(); }

    @Override
    public boolean isTime() { return subType.isTime(); }

    @Override
    public boolean isArray() { return true; }

    @Override
    public String getVotDatatype() { return subType.getVotDatatype(); }

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(arraysize); }

    @Override
    public Optional<String> getVotXtype() { return subType.getVotXtype(); }

    @Override
    public boolean canMergeWith(final DataType otherDatatype) {
        return (otherDatatype instanceof VectorType)
                && subType.canMergeWith(((VectorType)otherDatatype).subType);
    }
}
