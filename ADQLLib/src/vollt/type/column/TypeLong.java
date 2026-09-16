package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (12/2023)
 */
public class TypeLong extends ExactNumericType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.LONG; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
