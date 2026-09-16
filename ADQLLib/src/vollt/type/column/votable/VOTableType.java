package vollt.type.column.votable;

import java.util.Objects;
import java.util.Optional;

/**
 * Type used in VOTable documents.
 *
 * @version 1.0 (01/2025)
 * @author Gr&eacute;gory Mantelet (CDS)
 */
public final class VOTableType {

    private final VotDatatype datatype;

    private final String arraysize;

    private final String xtype;

    public VOTableType(final String datatype, final String arraysize, final String xtype) throws IllegalArgumentException {
        this(VotDatatype.fromString(datatype)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown VOTable datatype: \""+datatype+"\"!")),
             arraysize,
             xtype);
    }

    public VOTableType(final VotDatatype datatype){
        this(datatype, null, null);
    }

    public VOTableType(final VotDatatype datatype, final String arraysize){
        this(datatype, arraysize, null);
    }

    public VOTableType(final VotDatatype datatype, final String arraysize, final String xtype){
        this.datatype  = Objects.requireNonNull(datatype);
        this.arraysize = normalizeArraysize(arraysize);
        this.xtype     = normalizeXType(xtype);
    }

    private static String normalizeArraysize(final String strArraysize){
        if (strArraysize == null || strArraysize.isEmpty())
            return null;
        else
            return strArraysize.trim().toLowerCase();
    }

    private static String normalizeXType(final String strXType){
        if (strXType == null || strXType.isEmpty())
            return null;
        else
            return strXType.trim().toUpperCase();
    }

    /**
     * Get the basic datatype characterizing this VOTable type.
     *
     * @return  VOTable datatype.
     */
    public VotDatatype getDatatype() {
        return datatype;
    }

    /**
     * If any, get the arraysize of this VOTable type.
     *
     * @return  Arraysize value (always trimmed and in lower-case).
     */
    public Optional<String> getArraysize() {
        return Optional.ofNullable(arraysize);
    }

    /**
     * If any, get the XType (special type) attached to this VOTable type.
     *
     * @return  XType value (always trimmed and in upper-case).
     */
    public Optional<String> getXtype() {
        return Optional.ofNullable(xtype);
    }

    @Override
    public String toString() {
        final String strArraysize = (arraysize != null ? "["+arraysize+"]" : "");
        final String strType = datatype + strArraysize;
        if (xtype != null)
            return xtype+" ("+strType+")";
        else
            return strType;
    }
}
