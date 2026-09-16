package vollt.type.column;

import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (05 / 2023)
 */
public class TypeShort extends ExactNumericType {
    @Override
    public VotDatatype getVotDatatype() { return VotDatatype.SHORT; }

    @Override
    public Optional<String> getVotXtype() { return Optional.empty(); }
}
