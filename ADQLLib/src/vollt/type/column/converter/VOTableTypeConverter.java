package vollt.type.column.converter;

import vollt.type.column.*;
import vollt.type.column.converter.exception.UnknownColumnTypeException;
import vollt.type.column.converter.exception.UnknownVOTableDatatypeException;
import vollt.type.column.votable.VOTableType;
import vollt.type.column.votable.VotDatatype;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A utility class to convert VOTable types ({@link VOTableType}) into the
 * internal VOLLT model for types ({@link ColumnType}).
 *
 * <p>
 *  This class is mostly used by {@link ColumnTypeFactory} with its functions
 *  {@link ColumnTypeFactory#fromVOTable(VOTableType) fromVOTable(VOTableType)}
 *  and {@link ColumnTypeFactory#toVOTable(ColumnType) toVOTable(ColumnType)}.
 * </p>
 *
 * <p><b>Implementation note: This class is considered as Thread-safe.</b>
 *  The internal map is neither synchronized nor unmodifiable, but only read
 *  accesses are performed in all its functions (except in the constructor when
 *  the map is built). When extending this class, one should take care to not
 *  break this Thread-safe status as it is crucial for {@link ColumnTypeFactory}.
 *  In order to ensure the Thread-safe status, one could use the
 *  {@link java.util.Collections#synchronizedMap(Map)} or
 *  {@link java.util.Collections#unmodifiableMap(Map)} function ; or merely
 *  by modifying the map only in this class constructor.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (02/2026)
 * @since 1.0
 */
public class VOTableTypeConverter implements ColumnTypeConverter<VOTableType>{

    /** Exhaustive collection of all supported scalar types. */
    protected final Map<String, Class<? extends ScalarType>> mapSupportedTypes = new HashMap<>();

    public VOTableTypeConverter() {
        // Simple types:
        support(TypeBoolean.class);
        support(TypeBit.class);
        support(TypeUnsignedByte.class);
        support(TypeShort.class);
        support(TypeInteger.class);
        support(TypeLong.class);
        support(TypeChar.class);
        support(TypeUnicodeChar.class);
        support(TypeFloat.class);
        support(TypeDouble.class);
        support(TypeFloatComplex.class);
        support(TypeDoubleComplex.class);

        // Special types:
        support(TypeTimestamp.class);
        support(TypeIntervalOfShort.class);
        support(TypeIntervalOfInteger.class);
        support(TypeIntervalOfLong.class);
        support(TypeIntervalOfFloat.class);
        support(TypeIntervalOfDouble.class);
        support(TypeMultiIntervalOfShort.class);
        support(TypeMultiIntervalOfInteger.class);
        support(TypeMultiIntervalOfLong.class);
        support(TypeMultiIntervalOfFloat.class);
        support(TypeMultiIntervalOfDouble.class);
        support(TypeHMS.class);
        support(TypeDMS.class);
        support(TypePointWithFloat.class);
        support(TypePointWithDouble.class);
        support(TypeCircleWithFloat.class);
        support(TypeCircleWithDouble.class);
        support(TypeRangeWithFloat.class);
        support(TypeRangeWithDouble.class);
        support(TypePolygonWithFloat.class);
        support(TypePolygonWithDouble.class);
        support(TypeMOC.class);
        support(TypeShape.class);
        support(TypeMultiShape.class);
        support(TypeURI.class);
        support(TypeUUID.class);
        support(TypeJSON.class);
        support(TypeRegion.class);
    }

    /**
     * Declare a data type to support.
     *
     * <p>
     *  This function should be used only by the constructor to update the
     *  collection of supported scalar types: {@link #mapSupportedTypes}.
     * </p>
     *
     * <p><b>IMPORTANT:</b>
     *  The given class must represent a scalar type and must provide an empty
     *  constructor.
     * </p>
     *
     * @param typeClass Class of the type to support.
     *
     * @throws IncorrectDataTypeClass If the given type cannot be instantiated
     *                                or a similar type is already supported in
     *                                this factory.
     */
    protected void support(final Class<? extends ScalarType> typeClass) throws IncorrectDataTypeClass {
        // Try to get an instance of the given type:
        ScalarType typeInstance;
        try {
            final Constructor<? extends ScalarType> constructor = typeClass.getConstructor();
            typeInstance = constructor.newInstance();
        }catch(Exception ex) {
            throw new IncorrectDataTypeClass("Cannot create a "+this.getClass().getSimpleName()+"! Cause: impossible to create an instance of "+typeClass.getName()+".", ex);
        }

        // Create a hash key with the XType and the datatype:
        final String typeKey = buildTypeKey(typeInstance.getVotDatatype(), typeInstance.getVotXtype().orElse(null));
        if (mapSupportedTypes.containsKey(typeKey))
            throw new IncorrectDataTypeClass("Cannot create a "+this.getClass().getSimpleName()+"! Cause: a similar datatype (i.e. '" + typeKey + "') is already supported!");
        else
            mapSupportedTypes.put(typeKey, typeClass);
    }

    /**
     * Build a key for mapping in a unique way a given data type.
     *
     * @param datatype  A data type.
     * @param xtype     An extension of the given datatype. MAY BE
     *                  <code>null</code>.
     *
     * @return  The corresponding key.
     */
    protected String buildTypeKey(final VotDatatype datatype, final String xtype){
        return normalizeXtype(xtype) + "#" + datatype;
    }

    /**
     * Normalize the given VOTable XType.
     *
     * <p>In practice, it means:</p>
     * <ul>
     *     <li>the schema prefix is removed, if any is found
     *         (e.g. <code>adql:POINT</code>),</li>
     *     <li>leading and trailing space characters are removed,</li>
     *     <li>the whole string is lower-cased.</li>
     * </ul>
     *
     * <p>
     *     An empty string is returned if the given XType is <code>null</code>
     *     or an empty string (even after schema prefix removal).
     * </p>
     *
     * @param xtype The XType to normalize.
     *
     * @return  The normalized XType,
     *          or empty string if no XType provided.
     */
    protected String normalizeXtype(String xtype){
        // Nothing to do, if no xtype:
        if (xtype == null)
            return "";

        // Remove any schema prefix:
        final int indSchemaPrefix = xtype.indexOf(':');
        if (indSchemaPrefix >= 0)
            xtype = xtype.substring(indSchemaPrefix+1);

        // Remove leading and trailing spaces and put in lower case:
        xtype = xtype.trim().toLowerCase();

        return xtype;
    }

    public ColumnType toColumnType(final VOTableType originType) throws UnknownColumnTypeException {
        final VotDatatype datatype  = originType.getDatatype();
        final String arraysize      = originType.getArraysize().orElse(null);
        final String xtype          = originType.getXtype().orElse(null);

        // Search with the xtype and datatype:
        ScalarType foundType = getType(datatype, xtype);

        // If not found, search without the xtype, if any is provided:
        if (foundType == null && xtype != null){
            foundType = getType(datatype, null);
        }

        // If still not found, throw an Exception:
        if (foundType == null) {
            throw new UnknownVOTableDatatypeException(originType);
        }
        // If specified by arraysize, create a vector of this type:
        else if (isVector(foundType, arraysize)){
            return new VectorType(foundType, getArraysizeAppliedToScalarType(foundType, arraysize));
        }
        // Otherwise, return the found scalar type:
        else{
            return foundType;
        }
    }

    public VOTableType fromColumnType(final ColumnType originType){
        return originType.getVOTableType();
    }

    /**
     * Get the size of the vector containing the given subtype by extracting it
     * from the main type's arraysize.
     *
     * <p>
     *     The given subtype has its own arraysize (e.g. INTERVAL = DOUBLE[2]).
     *     This function removes the subtype arraysize from the main type
     *     arraysize so that returning the "number" of subtype. For instance:
     * </p>
     * <pre>datatype="DOUBLE" arraysize="2x*" xtype="interval"</pre>
     * <p>
     *     Then, one would call this function with the following parameters:
     * </p>
     * <pre>(new {@link TypeDouble}(), "2x*")</pre>
     * <p>
     *     This function would then return <code>*</code>.
     * </p>
     *
     * @param subType   Subtype (with its own arraysize).
     * @param arraysize Overall arraysize (including the subtype arraysize).
     *
     * @return  The vector size (i.e. number of subtype).
     */
    protected String getArraysizeAppliedToScalarType(final ColumnType subType, final String arraysize){
        final Optional<String> votArraysize = subType.getVotArraysize();
        if (votArraysize.isPresent())
        {
            final String subTypeArraysize = votArraysize.get();

            if (arraysize.startsWith(subTypeArraysize+"x"))
                return arraysize.substring(subTypeArraysize.length()+1);
            else
                return "*";
        }
        else
            return arraysize;
    }

    /**
     * Search for the exact match for the specified type and return an instance
     * of this type.
     *
     * @param datatype  VOTable datatype.
     * @param xtype     VOTable special type. MAY BE <code>null</code>.
     *
     * @return  The corresponding scalar type,
     *          or <code>null</code> if no match is found.
     */
    protected ScalarType getType(final VotDatatype datatype, final String xtype){
        // Build the mapping key for the specified type:
        String typeKey = buildTypeKey(datatype, xtype);

        // Search for a corresponding type:
        final Class<? extends ScalarType> foundType = mapSupportedTypes.get(typeKey);

        // If found, create and return an instance:
        if (foundType != null) {
            try {
                return foundType.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
                /* Implementation note:
                 *     Can never happen, as the empty constructor of all types
                 *     available in mapSupportedTypes have been tested by the
                 *     function support(...).
                 */
                return null;
            }
        }else
            return null;
    }

    /**
     * Tell whether the given arraysize represents a vector of the given scalar
     * type or not.
     *
     * <p>
     *  Some special types (e.g. timestamp, point) are actually arrays in the
     *  VOTable representation. In other words, for such type, the VOTable
     *  attribute "arraysize" is not empty. However, with a multi-dimensional
     *  arraysize, it is still possible to represent a vector of such special
     *  scalar type. This function aims to detect simple cases like vector of
     *  simple scalar types as well as complex cases like a vector of special
     *  types.
     * </p>
     *
     * @param scalarType    The scalar type that may be the vector subtype.
     * @param arraysize     The specified VOTable arraysize.
     *
     * @return  <code>true</code> if the given arraysize represents a vector of
     *          the given scalar type,
     *          <code>false</code> if the final type is really just a scalar type.
     */
    protected boolean isVector(final ScalarType scalarType, final String arraysize){
        final int              arrayDimension      = getArrayDimension(arraysize);
        final Optional<String> votArraySize        = scalarType.getVotArraysize();
        final int              scalarTypeDimension = votArraySize.map(this::getArrayDimension).orElse(1);

        if (arrayDimension > 0)
            return (!votArraySize.isPresent() || arrayDimension > scalarTypeDimension);
        else
            return false;
    }

    protected int getArrayDimension(final String arraysize){
        return (arraysize != null && !arraysize.trim().isEmpty()) ? arraysize.toLowerCase().split("x").length : 0;
    }

}
