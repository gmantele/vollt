package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

public class UnknownType extends SimpleType {
    private static final String DEFAULT_ARRAYSIZE = "*";

    private final String unrecognizedDatatype;

    private final String votArraysize;

    private final String votXtype;

    public UnknownType() {
        this(null, null, null);
    }

    public UnknownType(final String votDatatype, final String votArraysize, final String votXtype){
        if (votDatatype == null || votDatatype.trim().isEmpty()){
            this.unrecognizedDatatype = null;
            this.votArraysize         = DEFAULT_ARRAYSIZE;
            this.votXtype             = null;
        } else {
            this.unrecognizedDatatype = votDatatype.trim();
            this.votArraysize         = (votArraysize == null || votArraysize.trim().isEmpty()) ? null : votArraysize.trim();
            this.votXtype             = (votXtype == null || votXtype.trim().isEmpty()) ? null : votXtype.trim();
        }
    }

    @Override
    public boolean isNumeric() { return true; }

    @Override
    public boolean isBoolean() { return true; }

    @Override
    public boolean isBinary() { return true; }

    @Override
    public boolean isString() { return true; }

    @Override
    public boolean isGeometry() { return true; }

    @Override
    public boolean isTime() { return true; }

    @Override
    public boolean isArray() { return true; }

    /**
     * Get the string serialization of the unrecognized datatype, which is at
     * the origin of this {@link UnknownType}.
     *
     * @return  The unrecognized datatype.
     */
    public final Optional<String> getUnrecognizedDatatype(){ return Optional.ofNullable(unrecognizedDatatype); }

    @Override
    public final VotDatatype getVotDatatype() { return VotDatatype.UNKNOWN; }

    @Override
    public final Optional<String> getVotArraysize() { return Optional.ofNullable(votArraysize); }

    @Override
    public final Optional<String> getVotXtype() { return Optional.ofNullable(votXtype); }
}
