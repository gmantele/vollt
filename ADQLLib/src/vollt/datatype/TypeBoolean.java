package vollt.datatype;

import java.util.Optional;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (05 / 2023)
 */
public class TypeBoolean extends ScalarType {
    private static final String VOTABLE_DATATYPE = "boolean";

    @Override
    public boolean isNumeric() { return true; }

    @Override
    public boolean isBoolean() { return true; }

    @Override
    public boolean isBinary() { return false; }

    @Override
    public boolean isString() { return false; }

    @Override
    public boolean isGeometry() { return false; }

    @Override
    public boolean isTime() { return false; }

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
