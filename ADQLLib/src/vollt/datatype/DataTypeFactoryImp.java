package vollt.datatype;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class DataTypeFactoryImp implements DataTypeFactory {

    static final DataTypeFactoryImp DEFAULT_INSTANCE = new DataTypeFactoryImp();

    /** Exhaustive collection of all supported scalar types. */
    protected Map<String, Class<? extends ScalarType>> mapSupportedTypes = new HashMap<>();

    public DataTypeFactoryImp() {
        // Simple types:
        support(TypeBit.class);
        support(TypeBoolean.class);
        support(TypeChar.class);
        support(TypeDouble.class);
        support(TypeFloat.class);
        support(TypeInteger.class);
        support(TypeLong.class);
        support(TypeShort.class);
        support(TypeUnicodeChar.class);
        support(TypeUnsignedByte.class);

        // Special types:
        support(TypeCircleWithDouble.class);
        support(TypeCircleWithFloat.class);
        support(TypeIntervalOfDouble.class);
        support(TypeIntervalOfFloat.class);
        support(TypeIntervalOfInteger.class);
        support(TypeIntervalOfLong.class);
        support(TypeIntervalOfShort.class);
        support(TypeMOC.class);
        support(TypePointWithDouble.class);
        support(TypePointWithFloat.class);
        support(TypePolygonWithDouble.class);
        support(TypePolygonWithFloat.class);
        support(TypeRegion.class);
        support(TypeShape.class);
        support(TypeTimestamp.class);
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
     * @param datatype  A data type. SHOULD NOT BE <code>null</code>.
     * @param xtype     An extension of the given datatype. MAY BE
     *                  <code>null</code>.
     *
     * @return  The corresponding key.
     */
    protected String buildTypeKey(final String datatype, final String xtype){
        return (xtype == null ? "" : normalizeXtype(xtype)) + "#" + normalizeDatatype(datatype);
    }

    @Override
    public DataType fromVOTable(final String datatype, final String arraysize, String xtype) {
        // Search with the xtype and datatype:
        ScalarType foundType = getType(datatype, xtype);

        // If not found, search without the xtype, if any is provided:
        if (foundType == null && xtype != null){
            foundType = getType(datatype, null);
        }

        // If still not found, return an UnknownType:
        if (foundType == null){
            return new UnknownType(datatype, arraysize, xtype);
        }
        // If specified by arraysize, create a vector of this type:
        else if (isVector(foundType, arraysize)){
            return new VectorType(foundType, arraysize);
        }
        // Otherwise, return the found scalar type:
        else{
            return foundType;
        }
    }

    /**
     * Search for the exact match for the specified type and return an instance
     * of this type.
     *
     * @param datatype  VOTable datatype. SHOULD NOT BE <code>null</code>.
     * @param xtype     VOTable special type. MAY BE <code>null</code>.
     *
     * @return  The corresponding scalar type,
     *          or <code>null</code> if no match is found.
     */
    protected ScalarType getType(final String datatype, final String xtype){
        // If no datatype, no type to return:
        if (datatype == null || datatype.trim().isEmpty())
            return null;

        // Build the mapping key for the specified type:
        String typeKey = buildTypeKey(datatype, xtype);

        // Search for a corresponding type:
        final Class<? extends ScalarType> foundType = mapSupportedTypes.get(typeKey);

        // If found, create and return an instance:
        if (foundType != null) {
            try {
                return foundType.newInstance();
            } catch (Exception t) {
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
        final int arrayDimension = (arraysize != null && !arraysize.trim().isEmpty()) ? arraysize.toLowerCase().split("x").length : 0;
        if (arrayDimension > 0)
            return (!scalarType.getVotArraysize().isPresent() || arrayDimension > 1);
        else
            return false;
    }

    /**
     * Normalize the given VOTable datatype.
     *
     * <p>In practice, it means:</p>
     * <ul>
     *     <li>leading and trailing space characters are removed,</li>
     *     <li>the whole string is lower-cased.</li>
     * </ul>
     *
     * <p>
     *     <code>Null</code> is returned if the given datatype is
     *     <code>null</code> or an empty string.
     * </p>
     *
     * @param datatype The datatype to normalize.
     *
     * @return  The normalized datatype,
     *          or <code>null</code> if no datatype provided.
     */
    protected String normalizeDatatype(String datatype){
        // Nothing to do, if no xtype:
        if (datatype == null)
            return null;

        // Remove leading and trailing spaces and put in lower case:
        datatype = datatype.trim().toLowerCase();

        // NULL if nothing to return:
        if (datatype.isEmpty())
            return null;
        else
            return datatype;
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
     *     <code>Null</code> is returned if the given XType is <code>null</code>
     *     or an empty string (even after schema prefix removal).
     * </p>
     *
     * @param xtype The XType to normalize.
     *
     * @return  The normalized XType,
     *          or <code>null</code> if no XType provided.
     */
    protected String normalizeXtype(String xtype){
        // Nothing to do, if no xtype:
        if (xtype == null)
            return null;

        // Remove any schema prefix:
        final int indSchemaPrefix = xtype.indexOf(':');
        if (indSchemaPrefix >= 0)
            xtype = xtype.substring(indSchemaPrefix+1);

        // Remove leading and trailing spaces and put in lower case:
        xtype = xtype.trim().toLowerCase();

        // NULL if nothing to return:
        if (xtype.isEmpty())
            return null;
        else
            return xtype;
    }

}
