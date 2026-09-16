package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class VectorType extends SimpleType {
    protected final ScalarType subType;

    protected final String arraysize;

    public VectorType(final ScalarType subType, final String arraysize) throws NullPointerException, IllegalArgumentException {
        if (subType == null)
            throw new NullPointerException("Missing vector sub-type!");

        if (arraysize == null)
            throw new NullPointerException("Missing vector size!");
        else if (arraysize.trim().isEmpty())
            throw new IllegalArgumentException("Empty vector size!");

        this.subType   = subType;
        this.arraysize = arraysize.trim();
    }

    public ScalarType getSubType(){ return subType; }

    public String getVectorSize(){ return arraysize; }

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
    public VotDatatype getVotDatatype() { return subType.getVotDatatype(); }

    @Override
    public Optional<String> getVotArraysize() {
        final Optional<String> votArraySize = subType.getVotArraysize();
        return votArraySize.map(s -> Optional.of(s + "x" + arraysize))
                           .orElseGet(() -> Optional.of(arraysize));
    }

    @Override
    public Optional<String> getVotXtype() { return subType.getVotXtype(); }

    @Override
    public boolean canMergeWith(final ColumnType otherDatatype) {
        return (otherDatatype instanceof VectorType)
                && subType.canMergeWith(((VectorType)otherDatatype).subType);
    }
}
