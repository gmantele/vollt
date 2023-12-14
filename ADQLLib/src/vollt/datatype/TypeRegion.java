package vollt.datatype;

import java.util.Optional;

/**
 * Type used to represent an STC-s geometrical region.
 *
 * <p>
 *     This type may be deprecated by future version of DALI. Then, it would be
 *     replaced by {@link TypeShape}.
 *     TODO - A mapping between a region and a shape should be done.
 * </p>
 *
 * @see TypeShape
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (05/2023)
 */
public class TypeRegion extends GeometryType {
    private static final String VOTABLE_DATATYPE = "char";

    private static final String VOTABLE_ARRAYSIZE = "*";

    private static final String VOTABLE_XTYPE = "region";

    @Override
    public String getVotDatatype() { return VOTABLE_DATATYPE; }

    @Override
    public Optional<String> getVotArraysize() { return Optional.of(VOTABLE_ARRAYSIZE); }

    @Override
    public Optional<String> getVotXtype() { return Optional.of(VOTABLE_XTYPE); }
}
