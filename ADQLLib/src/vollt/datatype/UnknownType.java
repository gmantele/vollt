package vollt.datatype;

import java.util.Optional;

public class UnknownType extends SimpleType {
    private static final String DEFAULT_DATATYPE = "char";

    private static final String DEFAULT_ARRAYSIZE = "*";

    private final String votDatatype;

    private final String votArraysize;

    private final String votXtype;

    public UnknownType() {
        this(null, null, null);
    }

    public UnknownType(final String votDatatype, final String votArraysize, final String votXtype){
        if (votDatatype == null || votDatatype.trim().length() == 0){
            this.votDatatype  = DEFAULT_DATATYPE;
            this.votArraysize = DEFAULT_ARRAYSIZE;
            this.votXtype     = null;
        } else {
            this.votDatatype = votDatatype.trim();
            this.votArraysize = (votArraysize == null || votArraysize.trim().length() == 0) ? null : votArraysize.trim();
            this.votXtype = (votXtype == null || votXtype.trim().length() == 0) ? null : votXtype.trim();
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

    @Override
    public final String getVotDatatype() { return votDatatype; }

    @Override
    public final Optional<String> getVotArraysize() { return Optional.ofNullable(votArraysize); }

    @Override
    public final Optional<String> getVotXtype() { return Optional.ofNullable(votXtype); }
}
