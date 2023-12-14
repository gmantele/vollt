package vollt.datatype;

/**
 * Default implementation for data type serialization ({@link #toString()})
 * and the type comparison for merging ({@link #canMergeWith(DataType)}).
 *
 * <h3>String serialization</h3>
 * <p>
 *     In this implementation, a {@link DataType} is serialized in string
 *     using the VOTable type information. In short, if an
 *     {@link #getVotXtype() XType} if available, {@link #toString()} returns
 *     just this piece of information. Otherwise, the
 *     {@link #getVotDatatype() VOTable datatype} is returned. It is suffixed
 *     with the VOTable array size between square brackets, if representing an
 *     array.
 * </p>
 * <p>
 *     <b>Examples:</b> <code>char[*]</code>, <code>timestamp</code>,
 *     <code>double</code>, <code>float[2]</code>, <code>int[2x3*]</code>.
 * </p>
 *
 * <h3>Merge compatibility</h3>
 * <p>
 *     {@link DataType} extending {@link SimpleType} are declared compatible
 *     for merge only with a datum of the exact same {@link DataType}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (05/2023)
 */
public abstract class SimpleType implements DataType {

    @Override
    public String toString() {
        // An XType gives the precise datatype name, so use it if provided:
        if (getVotXtype().isPresent())
            return getVotXtype().get();
            // Otherwise...
        else {
            // ...combine the datatype (mandatory):
            final StringBuilder str = new StringBuilder(getVotDatatype());
            // ...with the array size (if available):
            if (getVotArraysize().isPresent())
                str.append('[').append(getVotArraysize().get()).append(']');
            return str.toString();
        }
    }

    @Override
    public boolean canMergeWith(final DataType otherDatatype) {
        return (otherDatatype != null)
                && (this.getClass().equals(otherDatatype.getClass())
                    || this instanceof UnknownType || otherDatatype instanceof UnknownType);
        // TODO Review merge condition!
    }

}
