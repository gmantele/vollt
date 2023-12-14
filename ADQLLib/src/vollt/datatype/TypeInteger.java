package vollt.datatype;

import java.util.Optional;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (05 / 2023)
 */
public class TypeInteger extends ExactNumericType {
    private static final String VOTABLE_DATATYPE = "int";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
