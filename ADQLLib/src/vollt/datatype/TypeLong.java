package vollt.datatype;

import java.util.Optional;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (05 / 2023)
 */
public class TypeLong extends ExactNumericType {
    private static final String VOTABLE_DATATYPE = "long";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
