package vollt.type.column.jdbc;

import vollt.type.column.ColumnType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Temporary column type representation provided by or for a JDBC driver.
 *
 * <p>
 *     Instances of this class aim to be used very shortly when getting data
 *     from a database or when uploading data into a database. In the first
 *     case, it will be immediately converted into a {@link ColumnType}
 *     instance. In the second case, it will be generated from a
 *     {@link ColumnType} just before sending data into a database. In both
 *     cases, conversions should be done with
 *     {@link vollt.type.column.ColumnTypeFactory} thanks to its functions:
 *     {@link vollt.type.column.ColumnTypeFactory#fromJDBCColumnType(JDBCColumnType) fromJDBCColumnType(JDBCColumnType)}
 *     and
 *     {@link vollt.type.column.ColumnTypeFactory#toJDBCColumnType(ColumnType, String) toJDBCColumnType(ColumnType, String)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (02/2026)
 */
public class JDBCColumnType {

    private final String   dbms;
    private final Integer  jdbcCode;
    private final String   fullName;
    private final String   simpleName;
    private final Integer  precision;
    private final String[] parameters;

    public JDBCColumnType(final String dbmsTypeName){
        this(dbmsTypeName, null, null, null);
    }

    public JDBCColumnType(final String dbmsTypeName, final Integer jdbcTypeCode){
        this(dbmsTypeName, jdbcTypeCode, null, null);
    }

    public JDBCColumnType(final String dbmsTypeName, final Integer jdbcTypeCode, final Integer precision, final String dbms){
        this.dbms      = dbms;
        this.jdbcCode  = jdbcTypeCode;
        this.fullName  = dbmsTypeName;
        this.precision = precision;

        final int[] paramIndices = getParametersIndices(fullName);

        this.simpleName = extractSimpleName(fullName, paramIndices);
        this.parameters = extractParameters(fullName, paramIndices);
    }

    private static int[] getParametersIndices(final String fullName){
        final int startParamIndex = fullName.indexOf('(');
        final int endParamIndex = fullName.indexOf(')');
        return new int[]{startParamIndex, endParamIndex};
    }

    private static String extractSimpleName(final String fullName, final int[] paramIndices){
        if (paramIndices[0] <= 0)
            return fullName;
        else
            return fullName.substring(0, paramIndices[0]);
    }

    private static String[] extractParameters(final String fullName, final int[] paramIndices){
        if (paramIndices[0] <= 0)
            return new String[0];
        else {
            final String paramString = fullName.substring(paramIndices[0] + 1, paramIndices[1]);
            final String[] params = paramString.split(",");
            return trimArrayValues(params);
        }
    }

    private static String[] trimArrayValues(String[] array){
        for(int i=0; i<array.length; i++){
            if (array[i] != null)
                array[i] = array[i].trim();
        }
        return array;
    }

    /**
     * Name of the target or origin database.
     *
     * <p><i>NOTE: this piece of information is always stored in lower case.</i></p>
     */
    public Optional<String> getDBMS() {
        return Optional.ofNullable(dbms);
    }

    /**
     * Type returned by a JDBC driver.
     *
     * <p><i>NOTE: this value is returned by
     *   ResultSetMetadata.getColumnType(int) and corresponds to a value of
     *   {@link java.sql.Types}.
     * </i></p> */
    public Optional<Integer> getJDBCCode() {
        return Optional.ofNullable(jdbcCode);
    }

    /**
     * Full name of the type returned by a JDBC driver.
     *
     * <p><i>NOTE: this name is returned by
     *   ResultSetMetadata.getColumnTypeName(int) ; this name may contain
     *   parameters.
     * </i></p> */
    public String getFullName() {
        return fullName;
    }

    /**
     * Name of type, without the eventual parameters.
     *
     * <p><i>NOTE: this name is extracted from {@link #fullName}.</i></p>
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * Precision of the type. It is often used to give the length of some types
     * (e.g. CHAR, VARCHAR, BINARY, VARBINARY).
     */
    public Optional<Integer> getPrecision() {
        return Optional.ofNullable(precision);
    }

    /**
     * The eventual type parameters (e.g. char string length).
     *
     * <p><i>NOTE: these parameters are extracted from {@link #fullName}.</i></p>
     */
    public String[] getParameters(){
        return Arrays.copyOf(parameters, parameters.length);
    }
}
