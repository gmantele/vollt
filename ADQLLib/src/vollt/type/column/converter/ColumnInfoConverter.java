package vollt.type.column.converter;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.votable.VOStarTable;
import vollt.type.column.*;
import vollt.type.column.converter.exception.UnknownColumnTypeException;
import vollt.type.column.votable.VOTableType;
import vollt.type.column.votable.VotDatatype;

import java.util.Arrays;
import java.util.Optional;

/**
 * A utility class to convert STIL internal types (wrapped into
 * {@link ColumnInfo} objects) into the internal VOLLT model for types
 * ({@link ColumnType}).
 *
 * <p>
 *  This class is mostly used by {@link ColumnTypeFactory} with its functions
 *  {@link ColumnTypeFactory#fromColumnInfo(ColumnInfo) fromColumnInfo(ColumnInfo)}
 *  and {@link ColumnTypeFactory#toColumnInfo(ColumnType) toColumnInfo(ColumnType)}.
 * </p>
 *
 * <p>
 *     See the following documentation for more details about the applied
 *     mapping:
 * </p>
 * <ul>
 *     <li><a href="https://www.star.bris.ac.uk/~mbt/stil/sun252.html#voDatamap">
 *         STIL documentation
 *     </a></li>
 *     <li><a href="https://www.ivoa.net/documents/VOTable/20250116/REC-VOTable-1.5.html#tth_sEc2.1">
 *         sections 2.1 and 2.2 of VOTable (1.5 here)</a></li>
 * </ul>
 *
 * <p><b>Implementation note: This class is fully Thread-safe.</b></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (09/2026)
 * @since 1.0
 */
public class ColumnInfoConverter implements ColumnTypeConverter<ColumnInfo> {

    private static final String AUX_UNICODE_CHAR = "unicodeChar";

    @Override
    public ColumnInfo fromColumnType(final ColumnType originType) {
        // Build a ColumnInfo with the name, type and description:
        final ColumnInfo colInfo = new ColumnInfo("-", getDatatypeClass(originType), null);

        // Set auxiliary datatype (for special VOTable types like floatComplex):
        createAuxiliaryDatatype(originType).ifPresent(colInfo::setAuxDatum);

        // Set the unsigned flag if necessary:
        createUnsignedByteFlag(originType).ifPresent(colInfo::setAuxDatum);

        // Set the shape (VOTable arraysize):
        colInfo.setShape(createShape(originType));
        // ...and element size (only for Strings):
        colInfo.setElementSize(createElementSize(originType));

        // Set this value may be NULL (note: it is not really necessary since STIL set this flag to TRUE by default):
        colInfo.setNullable(true);

        // Set the XType (if any):
        originType.getVotXtype().ifPresent(colInfo::setXtype);

        return colInfo;
    }

    /**
     * Convert the column type into a corresponding {@link Class} object.
     *
     * <p>
     *     This function guess the datatype class (i.e. Java class)
     *     corresponding to the given {@link ColumnType}.
     *     This mapping is based on Section "7.1.4 Data Types" of the
     *     <a href="https://www.star.bris.ac.uk/~mbt/stil/sun252.html#voDatamap">STIL documentation</a>.
     * </p>
     *
     * @param colType	Column full type.
     *
     * @return	The corresponding {@link Class} object.
     */
    protected Class<?> getDatatypeClass(final ColumnType colType) {
        final boolean isScalar     = isScalar(colType);
        final VotDatatype datatype = getKnownDatatype(colType);

        switch(datatype) {
            case BIT:
                return boolean[].class;
            case BOOLEAN:
                return isScalar ? Boolean.class : boolean[].class;
            case DOUBLE:
                return isScalar ? Double.class : double[].class;
            case DOUBLE_COMPLEX:
                return double[].class;
            case FLOAT:
                return isScalar ? Float.class : float[].class;
            case FLOAT_COMPLEX:
                return float[].class;
            case INT:
                return isScalar ? Integer.class : int[].class;
            case LONG:
                return isScalar ? Long.class : long[].class;
            case SHORT:
            case UNSIGNED_BYTE:
                return isScalar ? Short.class : short[].class;
            case CHAR:
            case UNICODE_CHAR:
            default: /* If the type is not know (theoretically, never happens), return char[*] by default. */
                return isScalar ? Character.class : String.class;
        }
    }

    protected VotDatatype getKnownDatatype(final ColumnType colType){
        VotDatatype datatype = colType.getVotDatatype();

        if (ColumnType.isUnknown(colType))
        {
            if (colType instanceof UnknownNumericType)
                datatype = VotDatatype.DOUBLE;
            else
                datatype = VotDatatype.CHAR;
        }

        return datatype;
    }

    protected boolean isScalar(final ColumnType colType){
        return !colType.isArray() || colType.getVotArraysize().orElse("").equals("1");
        /* NOTE:
         * See the Note in section 2.2 of VOTable-1.5
         * (https://www.ivoa.net/documents/VOTable/20250116/REC-VOTable-1.5.html#tth_sEc2.2)
         */
    }

    protected Optional<DescribedValue> createAuxiliaryDatatype(final ColumnType colType){
        String datatype = null;

        if (isUnicodeChar(colType))
            datatype = AUX_UNICODE_CHAR;
        else if (isFloatComplex(colType))
            datatype = VotDatatype.FLOAT_COMPLEX.toString();
        else if (isDoubleComplex(colType))
            datatype = VotDatatype.DOUBLE_COMPLEX.toString();

        if (datatype == null)
            return Optional.empty();
        else
            return Optional.of(new DescribedValue(VOStarTable.DATATYPE_INFO, datatype));
    }

    private boolean isUnicodeChar(final ColumnType colType){
        return colType instanceof TypeUnicodeChar
            || (colType instanceof VectorType && ((VectorType)colType).getSubType() instanceof TypeUnicodeChar);
    }

    private boolean isFloatComplex(final ColumnType colType){
        return colType instanceof TypeFloatComplex
            || (colType instanceof VectorType && ((VectorType)colType).getSubType() instanceof TypeFloatComplex);
    }

    private boolean isDoubleComplex(final ColumnType colType){
        return colType instanceof TypeDoubleComplex
            || (colType instanceof VectorType && ((VectorType)colType).getSubType() instanceof TypeDoubleComplex);
    }

    protected Optional<DescribedValue> createUnsignedByteFlag(final ColumnType colType){
        if (isUnsignedByte(colType))
            return Optional.of(new DescribedValue(Tables.UBYTE_FLAG_INFO, true));
        else
            return Optional.empty();
    }

    private boolean isUnsignedByte(final ColumnType colType){
        return colType instanceof TypeUnsignedByte
            || (colType instanceof VectorType && ((VectorType)colType).getSubType() instanceof TypeUnsignedByte);
    }

    /**
     * Create the shape for a complex value (float or double) depending on
     * whether it is an array of complex values or not.
     *
     * @param colType	The origin column type.
     *
     * @return	The corresponding {@link ColumnInfo} shape.
     */
    protected int[] createShape(final ColumnType colType)
    {
        final int[] shape = createShape(colType.getVotArraysize().orElse(null));

        if (isFloatComplex(colType) || isDoubleComplex(colType))
            return prefixShapeWith(shape, 2);
        else if (isString(colType)) {
            if (shape.length <= 1)
                return new int[0]; // discard the possible fixed length and replace it by an unbounded one (which is the default in STIL)
            else
                return Arrays.copyOfRange(shape, 1, shape.length);
        }else
            return shape;
    }

    private int[] createShape(final String arraysize) {
        final String[] dimensions   = getShapeDimensions(arraysize);
        final int      nbDimensions = dimensions.length;

        final int[] shape = new int[nbDimensions];

        for(int i=0; i<nbDimensions; i++)
        {
            final String dim = dimensions[i].trim();

            if (isUnboundedDimension(dim))
                shape[i] = getUnboundedDimensionShape(i, nbDimensions);
            else
                shape[i] = convertDimensionIntoShape(dim, i, nbDimensions);
        }

        return shape;
    }

    private String[] getShapeDimensions(final String arraysize){
        if (arraysize == null)
            return new String[0];
        else
            return arraysize.split("x");
    }

    private boolean isUnboundedDimension(final String dimension){
        return dimension.charAt(dimension.length()-1) == '*';
    }

    private int getUnboundedDimensionShape(final int dimIndex, final int nbDimensions){
        if (dimIndex == nbDimensions-1)
            return -1;
        else
            return Integer.MAX_VALUE;
    }

    private int convertDimensionIntoShape(final String dimension, final int dimIndex, final int nbDimensions){
        try {
            return Integer.parseInt(dimension);
        }
        catch (NumberFormatException nfe) {
            return getUnboundedDimensionShape(dimIndex, nbDimensions);
        }
    }

    protected boolean isString(final ColumnType colType){
        return colType instanceof VectorType
            && (((VectorType)colType).getSubType() instanceof TypeChar
                || ((VectorType)colType).getSubType() instanceof TypeUnicodeChar);
    }

    private int[] prefixShapeWith(final int[] oldShape, final int prefix){
        if (oldShape.length == 0)
            return new int[]{prefix};
        else {
            final int[] newShape = new int[oldShape.length + 1];

            newShape[0] = prefix;
            System.arraycopy(oldShape, 0, newShape, 1, oldShape.length);

            return newShape;
        }
    }

    protected int createElementSize(final ColumnType colType){
        if (isString(colType)){
            final Optional<String> arraysize = colType.getVotArraysize();
            return arraysize.map(size -> convertDimensionIntoShape(size.split("x")[0], 0, 1)).orElse(-1);
        }else
            return -1;
    }


    @Override
    public ColumnType toColumnType(final ColumnInfo colInfo) throws UnknownColumnTypeException {
        final VOTableType votType = getVOTableType(colInfo);
        return ColumnTypeFactory.fromVOTable(votType);
    }

    protected VOTableType getVOTableType(final ColumnInfo colInfo) throws UnknownColumnTypeException {
        /* Get default arraysize (it might be changed in function of the final
         * datatype): */
        final String      arraysize         = getArraysize(colInfo);

        final String      xtype             = getXType(colInfo).orElse(null);
        final Class<?>    clazz             = colInfo.getContentClass();
        final VotDatatype auxiliaryDatatype = getAuxiliaryDatatype(colInfo).orElse(null);
        final VotDatatype charDatatype      = getCharDatatype(auxiliaryDatatype);

        /* See if unsigned byte output is explicitly requested: */
        final Boolean     ubyteFlag         = colInfo.getAuxDatumValue(Tables.UBYTE_FLAG_INFO, Boolean.class);

        final Optional<VOTableType> datatype = getVOTableType(clazz, auxiliaryDatatype, ubyteFlag, charDatatype, arraysize, colInfo.getElementSize(), colInfo.getShape(), xtype);

        if (datatype.isPresent())
            return datatype.get();
        else
            throw new UnknownColumnTypeException("Unsupported input datatype (clazz="+clazz+"; xtype="+xtype+", auxDatatype="+auxiliaryDatatype+", arraysize="+arraysize+")!");
    }

    protected String getArraysize(final ColumnInfo colInfo){
        String formattedShape = DefaultValueInfo.formatShape(colInfo.getShape());
        formattedShape = formattedShape.replace(",", "x");
        return formattedShape;
    }

    /**
     * Extract the XType info from the given {@link ColumnInfo}.
     *
     * @param colInfo {@link ColumnInfo} which may contain the XType.
     *
     * @return	The serialized extracted XType.
     */
    protected Optional<String> getXType(final ColumnInfo colInfo) {
        return Optional.ofNullable(colInfo.getXtype());
    }

    /**
     * Extract the datatype from the auxiliary data of the given
     * {@link ColumnInfo}.
     *
     * @param colInfo {@link ColumnInfo} which may contain the datatype.
     *
     * @return	The extracted auxiliary datatype.
     */
    protected Optional<VotDatatype> getAuxiliaryDatatype(final ColumnInfo colInfo){
        final String auxDatatype = colInfo.getAuxDatumValue(VOStarTable.DATATYPE_INFO, String.class);
        if (auxDatatype != null)
            return VotDatatype.fromString(auxDatatype);
        else
            return Optional.empty();
    }

    private Optional<VOTableType> getVOTableType(final Class<?> clazz, final VotDatatype auxiliaryDatatype, final Boolean ubyteFlag, final VotDatatype charDatatype, String arraysize, final int elementSize, final int[] shape, final String xtype) {
        VotDatatype datatype;

        if (isBoolean(clazz))
            datatype = VotDatatype.BOOLEAN;

        else if (isUnsignedByte(clazz, ubyteFlag))
            datatype = VotDatatype.UNSIGNED_BYTE;

        else if (isShort(clazz))
            datatype = VotDatatype.SHORT;

        else if (isInteger(clazz))
            datatype = VotDatatype.INT;

        else if (isLong(clazz))
            datatype = VotDatatype.LONG;

        else if (isFloatComplex(clazz, auxiliaryDatatype, arraysize)) {
            datatype = VotDatatype.FLOAT_COMPLEX;
            arraysize = reduceArraysizeForComplexNumber(arraysize);
        }

        else if (isFloat(clazz))
            datatype = VotDatatype.FLOAT;

        else if (isDoubleComplex(clazz, auxiliaryDatatype, arraysize)){
            datatype = VotDatatype.DOUBLE_COMPLEX;
            arraysize = reduceArraysizeForComplexNumber(arraysize);
        }

        else if (isDouble(clazz))
            datatype = VotDatatype.DOUBLE;

        else if (isSingleCharacter(clazz, elementSize)) {
            datatype  = charDatatype;
            arraysize = null;
            /* NOTE:
             * See section 7.3.4 of STIL
             * (https://www.star.bris.ac.uk/~mbt/stil/sun252.html#voStandard)
             * as well as the Note in section 2.2 of VOTable-1.5
             * (https://www.ivoa.net/documents/VOTable/20250116/REC-VOTable-1.5.html#tth_sEc2.2)
             */
        }

        else if (isVariableMultidimensionalString(clazz, elementSize)) {
            System.out.println("WARNING: can't serialize array of variable-length strings to VOTable - degrading to datatype=\"char\" arraysize=\"*\"!"); // TODO Log properly the failed conversion!
            return Optional.empty();
        }

        else if (isString(clazz, elementSize)) {
            datatype  = charDatatype;
            arraysize = computeArraysizeForMultidimensionalString(elementSize, shape);
        }

        else
            return Optional.empty();

        return Optional.of(new VOTableType(datatype, arraysize, xtype));
    }

    private boolean isBoolean(final Class<?> clazz){
        return clazz == Boolean.class || clazz == boolean[].class;
    }

    private boolean isUnsignedByte(final Class<?> clazz, final Boolean ubyteFlag){
        return Boolean.TRUE.equals(ubyteFlag)
            && isShort(clazz);
    }

    private boolean isShort(final Class<?> clazz){
        return clazz == Byte.class  || clazz == byte[].class
            || clazz == Short.class || clazz == short[].class;
    }

    private boolean isInteger(final Class<?> clazz){
        return clazz == Integer.class || clazz == int[].class;
    }

    private boolean isLong(final Class<?> clazz){
        return clazz == Long.class || clazz == long[].class;
    }

    private boolean isFloatComplex(final Class<?> clazz, final VotDatatype auxiliaryDatatype, final String arraysize){
        return isFloat(clazz)
            && VotDatatype.FLOAT_COMPLEX == auxiliaryDatatype
            && isArraysizeCompatibleWithComplexNumber(arraysize);
    }

    private boolean isDoubleComplex(final Class<?> clazz, final VotDatatype auxiliaryDatatype, final String arraysize){
        return isDouble(clazz)
                && VotDatatype.DOUBLE_COMPLEX == auxiliaryDatatype
                && isArraysizeCompatibleWithComplexNumber(arraysize);
    }

    private boolean isFloat(final Class<?> clazz){
        return clazz == Float.class || clazz == float[].class;
    }

    private boolean isDouble(final Class<?> clazz){
        return clazz == Double.class || clazz == double[].class;
    }

    private boolean isArraysizeCompatibleWithComplexNumber(final String arraysize){
        return arraysize != null && ("2".equals(arraysize) || arraysize.startsWith("2x"));
    }

    private String reduceArraysizeForComplexNumber(final String arraysize){
        if (arraysize == null || "2".equals(arraysize))
            return null;
        else
            return arraysize.substring(2);
    }

    private boolean isSingleCharacter(final Class<?> clazz, final int elementSize){
        return (clazz == Character.class && (elementSize < 0 || elementSize == 1))
            || (clazz == String.class && elementSize == 1);
    }

    private boolean isVariableMultidimensionalString(final Class<?> clazz, final int elementSize){
        return clazz == String[].class && elementSize < 0;
    }

    private boolean isString(final Class<?> clazz, final int elementSize){
        return (clazz == Character.class && (elementSize == 0 || elementSize > 1))
            || clazz == String.class
            || clazz == String[].class;
    }

    private String computeArraysizeForMultidimensionalString(final int elementSize, int[] shape){
        if (shape == null)
            shape = new int[0];

        /* Add an extra dimension since writing treats a string as an
         * array of chars. */
        final int[] charDims = new int[shape.length+1];
        charDims[0] = elementSize;
        System.arraycopy(shape, 0, charDims, 1, shape.length);

        /* Work out the arraysize attribute. */
        final StringBuilder buf = new StringBuilder();
        for(int i = 0; i < charDims.length; i++) {
            if (i > 0)
                buf.append('x');

            if (i == charDims.length - 1 && charDims[i] <= 0)
                buf.append('*');
            else
                buf.append(charDims[i]);
        }

        return buf.toString();
    }

    protected VotDatatype getCharDatatype(final VotDatatype auxiliaryDatatype){
        if (auxiliaryDatatype == VotDatatype.UNICODE_CHAR)
            return VotDatatype.UNICODE_CHAR;
        else
            return VotDatatype.CHAR;
    }

}
