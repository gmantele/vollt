package vollt.type.column;

import vollt.type.column.votable.VOTableType;
import vollt.type.column.votable.VotDatatype;

import java.util.Optional;

/**
 * Representation of a data type.
 *
 * <h3>VOTable type system</h3>
 * <p>
 *     Implementations can return the equivalent type in the VOTable type
 *     system. In other words: a {@link #getVotDatatype() datatype},
 *     an {@link #getVotArraysize() arraysize} and a
 *     {@link #getVotXtype() xtype}. Only the VOTable datatype is mandatory and
 *     must never be NULL.
 * </p>
 *
 * <h3>Type comparison</h3>
 * <p>
 *     In some cases, it is useful to merge two information. The condition to
 *     perform this operation is to ensure their data-types are compatible
 *     enough for a such purpose. That's the goal of the function
 *     {@link #canMergeWith(ColumnType)}.
 * </p>
 *
 * <h3>Simple serialization</h3>
 * <p>
 *     Any {@link ColumnType} can be serialized into a string thanks to
 *     {@link #toString()}. This latter should use the VOTable type
 *     information. Example: <code>char[*]</code>. See
 *     {@link SimpleType#toString()} for inspiration.
 * </p>
 *
 * <h3>Implementing {@link ColumnType}</h3>
 * <p>
 *     An implementation of the functions {@link #canMergeWith(ColumnType)}
 *     and {@link #toString()} is already provided in the abstract class
 *     {@link SimpleType}. It is recommended to extends this class when defining
 *     new {@link ColumnType} implementations.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (01/2025)
 *
 * @see SimpleType
 */
public interface ColumnType {

    boolean isNumeric();

    boolean isBoolean();

    boolean isBinary();

    boolean isString();

    boolean isGeometry();

    boolean isTime();

    boolean isArray();

    /**
     * Get the corresponding VOTable type.
     *
     * <p>This function is just a shortcut for the following:</p>
     * <pre>new VOTableType(colType.getVotDatatype(),
     *               colType.getVotArraysize().orElse(null),
     *               colType.getVotXtype().orElse(null));</pre>
     *
     * @return  The corresponding {@link VOTableType}.
     */
    default VOTableType getVOTableType(){
        return new VOTableType(getVotDatatype(),
                               getVotArraysize().orElse(null),
                               getVotXtype().orElse(null));
    }

    /**
     * Get the corresponding VOTable datatype.
     *
     * <p><b>IMPORTANT:</b>
     *  This function MUST never return NULL.
     * </p>
     *
     * @return The corresponding VOTable datatype.
     */
    VotDatatype getVotDatatype();

    /**
     * Get the size of the represented array in the VOTable representation.
     *
     * <p>
     *     This function must return an empty {@link Optional} object if
     *     this data type does not represent an array.
     * </p>
     *
     * @return The corresponding VOTable array size.
     */
    Optional<String> getVotArraysize();

    /**
     * Get the special type corresponding to this data type in the VOTable
     * representation. In VOTable term, this piece of information is called an
     * <code>XType</code>.
     *
     * <p>
     *     This function must return an empty {@link Optional} object if
     *     this data type does not represent a special VOTable type.
     * </p>
     *
     * @return  The corresponding VOTable XType.
     */
    Optional<String> getVotXtype();

    String toString();

    /**
     * Tell whether a data of this type could be merged with a data of the given
     * type.
     *
     * <p>
     *     To return <code>true</code> this function has to evaluate whether
     *     the two given types are compatible at some level so that the two data
     *     to merge can be implicitly casted in each other type.
     * </p>
     *
     * <p>
     *     By default (see {@link SimpleType#canMergeWith(ColumnType)}), two
     *     data types are declared compatible for merge when they are exactly
     *     the same class. But in some cases, this merge compatibility should be
     *     extended. For instance for exact numeric types: int, short and long.
     *     To indicate their compatibility, the corresponding {@link ColumnType}
     *     implementations should implement the same interface. For instance:
     *     {@link ExactNumericType} is the interface making {@link TypeShort},
     *     {@link TypeInteger} and {@link TypeLong} compatible for merge.
     * </p>
     *
     * @param otherDatatype The data type of another datum.
     *
     * @return <code>true</code> if the two data types are compatible for merge,
     *         <code>false</code> otherwise (and especially if otherDatatype is
     *         <code>null</code>).
     */
    boolean canMergeWith(final ColumnType otherDatatype);

    /**
     * Tell whether the given data type is a resolved data type or not.
     *
     * @param type  The data type to test.
     *
     * @return  <code>true</code> if the data type is NOT resolved or is
     *          <code>null</code>,
     *          <code>false</code> otherwise.
     */
    static boolean isUnknown(final ColumnType type){
        return type == null || type instanceof UnknownType || type.getVotDatatype() == VotDatatype.UNKNOWN;
    }

}
