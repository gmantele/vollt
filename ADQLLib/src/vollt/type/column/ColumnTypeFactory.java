package vollt.type.column;

import uk.ac.starlink.table.ColumnInfo;
import vollt.type.column.converter.ColumnInfoConverter;
import vollt.type.column.converter.ColumnTypeConverter;
import vollt.type.column.converter.VOTableTypeConverter;
import vollt.type.column.converter.exception.UnknownColumnTypeException;
import vollt.type.column.converter.jdbc.*;
import vollt.type.column.jdbc.JDBCColumnType;
import vollt.type.column.votable.VOTableType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory to build special {@link ColumnType} instances or to convert from and
 * into {@link ColumnType}
 *
 * @version 1.0 (02/2026)
 * @author Gr&eacute;gory Mantelet (CDS)
 *
 * @see ColumnTypeConverter
 */
public final class ColumnTypeFactory {
    private ColumnTypeFactory(){}

    /**
     * Create a {@link ColumnType} representing a character string
     * (in VOTable: datatype="char" arraysize="*").
     *
     * @return  The {@link ColumnType} representing a String.
     */
    public static ColumnType createString() {
        return new VectorType(new TypeChar(), "*");
    }


    /* **********************************************************************
     * *                            VOTABLE                                 *
     * ********************************************************************** */

    private static ColumnTypeConverter<VOTableType> votableConverter = new VOTableTypeConverter();

    /**
     * Set the converter to use between {@link ColumnType} and
     * {@link VOTableType}.
     *
     * <p>
     *     This converter is used by {@link #fromVOTable(VOTableType)} and
     *     {@link #toVOTable(ColumnType)}.
     * </p>
     *
     * <p><i><b>Note: </b>
     *  By default it is set to {@link VOTableTypeConverter}.
     * </i></p>
     *
     * @param converter The converted to use (must not be <code>null</code>).
     */
    public static void setVOTableTypeConverter(final ColumnTypeConverter<VOTableType> converter){
        votableConverter = Objects.requireNonNull(converter);
    }

    /**
     * Create a {@link ColumnType} matching the given VOTable type.
     *
     * <p>
     *  This function only the function
     *  {@link ColumnTypeConverter#toColumnType(Object) toColumnType(Object)}
     *  set with {@link #setVOTableTypeConverter(ColumnTypeConverter)}.
     * </p>
     *
     * @param votType   The VOTable type to convert.
     *
     * @return  The {@link ColumnType} matching as much as possible.
     *
     * @throws UnknownColumnTypeException   If the given VOTable type can not be
     *                                      converted into a {@link ColumnType}.
     */
    public static ColumnType fromVOTable(final VOTableType votType) throws UnknownColumnTypeException {
        return votableConverter.toColumnType(votType);
    }

    /**
     * Create a {@link VOTableType} matching the given {@link ColumnType}.
     *
     * <p>
     *  This function only calls the function
     *  {@link ColumnTypeConverter#fromColumnType(ColumnType) fromColumnType(ColumnType)}
     *  set with {@link #setVOTableTypeConverter(ColumnTypeConverter)}.
     * </p>
     *
     * @param colType   The {@link ColumnType} to convert.
     *
     * @return  The {@link VOTableType} matching as much as possible.
     *
     * @throws UnknownColumnTypeException   If the given {@link ColumnType} can
     *                                      not be converted into a VOTable type.
     */
    public static VOTableType toVOTable(final ColumnType colType) throws UnknownColumnTypeException {
        return votableConverter.fromColumnType(colType);
    }


    /* **********************************************************************
     * *                         STIL (COLUMN_INFO)                         *
     * ********************************************************************** */

    private static ColumnTypeConverter<ColumnInfo> columnInfoConverter = new ColumnInfoConverter();

    /**
     * Set the converter to use between {@link ColumnType} and
     * {@link ColumnInfo} (used by the STIL library).
     *
     * <p>
     *     This converter is used by {@link #fromColumnInfo(ColumnInfo)} and
     *     {@link #toColumnInfo(ColumnType)}.
     * </p>
     *
     * <p><i><b>Note: </b>
     *  By default it is set to {@link ColumnInfoConverter}.
     * </i></p>
     *
     * @param converter The converted to use (must not be <code>null</code>).
     */
    public static void setColumnInfoConverter(final ColumnTypeConverter<ColumnInfo> converter){
        columnInfoConverter = Objects.requireNonNull(converter);
    }

    /**
     * Create a {@link ColumnInfo} (used by the STIL library) matching the given
     * {@link ColumnType}.
     *
     * <p>
     *  This function only calls the function
     *  {@link ColumnTypeConverter#fromColumnType(ColumnType) fromColumnType(ColumnType)}
     *  set with {@link #setColumnInfoConverter(ColumnTypeConverter)}.
     * </p>
     *
     * @param colType   The {@link ColumnType} to convert.
     *
     * @return  The {@link ColumnInfo} matching as much as possible.
     *
     * @throws UnknownColumnTypeException   If the given {@link ColumnType} can
     *                                      not be converted into a
     *                                      {@link ColumnInfo}.
     */
    public static ColumnInfo toColumnInfo(final ColumnType colType) throws UnknownColumnTypeException {
        return columnInfoConverter.fromColumnType(colType);
    }

    /**
     * Create a {@link ColumnType} matching the given {@link ColumnInfo} (used
     * by the STIL library).
     *
     * <p>
     *  This function only the function
     *  {@link ColumnTypeConverter#toColumnType(Object) toColumnType(Object)}
     *  set with {@link #setColumnInfoConverter(ColumnTypeConverter)}.
     * </p>
     *
     * @param colInfo   The {@link ColumnInfo} to convert.
     *
     * @return  The {@link ColumnType} matching as much as possible.
     *
     * @throws UnknownColumnTypeException   If the given {@link ColumnInfo} can
     *                                      not be converted into a
     *                                      {@link ColumnType}.
     */
    public static ColumnType fromColumnInfo(final ColumnInfo colInfo) throws UnknownColumnTypeException {
        return columnInfoConverter.toColumnType(colInfo);
    }


    /* **********************************************************************
     *  *                              JDBC                                 *
     * ********************************************************************** */

    private static ColumnTypeConverter<JDBCColumnType> defaultJDBCConverter = new DefaultJDBCColumnTypeConverter();

    private static final Map<String, ColumnTypeConverter<JDBCColumnType>> mapDbmsConverters = new HashMap<>();
    /* NOTE: not thread safe container, but as it can be modified only at
     *       initialization, it should not be an issue. As long as it is not an
     *       issue, lets keep it unsafe for an efficiency reason (i.e. faster). */

    public static final String DBMS_POSTGRES  = "postgresql";
    public static final String DBMS_PGSPHERE  = "pgsphere";
    public static final String DBMS_MYSQL     = "mysql";
    public static final String DBMS_SQLSERVER = "sqlserver";
    public static final String DBMS_SQLITE    = "sqlite";
    public static final String DBMS_H2        = "h2";

    static{
        mapDbmsConverters.put(DBMS_POSTGRES , new PostgresColumnTypeConverter());
        mapDbmsConverters.put(DBMS_PGSPHERE , new PgSphereColumnTypeConverter());
        mapDbmsConverters.put(DBMS_MYSQL    , new MySQLColumnTypeConverter());
        mapDbmsConverters.put(DBMS_SQLSERVER, new SQLServerColumnTypeConverter());
        mapDbmsConverters.put(DBMS_SQLITE   , new SQLiteColumnTypeConverter());
        mapDbmsConverters.put(DBMS_H2       , new H2ColumnTypeConverter());
    }

    /**
     * Set the converter to use between {@link ColumnType} and
     * {@link JDBCColumnType} (used by JDBC drivers).
     *
     * <p>
     *     This converter is used by {@link #fromJDBCColumnType(JDBCColumnType)} and
     *     {@link #toJDBCColumnType(ColumnType, String)}.
     * </p>
     *
     * <p><i><b>Note: </b>
     *  By default it is set to {@link DefaultJDBCColumnTypeConverter}.
     * </i></p>
     *
     * @param converter The converted to use (must not be <code>null</code>).
     */
    public static void setDefaultJDBCConverter(final ColumnTypeConverter<JDBCColumnType> converter){
        defaultJDBCConverter = Objects.requireNonNull(converter);
    }

    /**
     * Set the converter to use between {@link ColumnType} and
     * {@link ColumnInfo} (used by the STIL library).
     *
     * <p>
     *     This converter is used by {@link #fromColumnInfo(ColumnInfo)} and
     *     {@link #toColumnInfo(ColumnType)}.
     * </p>
     *
     * <p><i><b>Note: </b>
     *  By default it is set to {@link ColumnInfoConverter}.
     * </i></p>
     *
     * @param converter The converted to use (must not be <code>null</code>).
     */
    public static void setJDBCColumnTypeConverter(final String dbms, final ColumnTypeConverter<JDBCColumnType> converter){
        mapDbmsConverters.put(Objects.requireNonNull(dbms).toLowerCase(), Objects.requireNonNull(converter));
    }

    public static JDBCColumnType toJDBCColumnType(final ColumnType colType, final String dbms) throws UnknownColumnTypeException {
        final String normalizedDBMS = Objects.requireNonNull(dbms).toLowerCase();
        final ColumnTypeConverter<JDBCColumnType> converter = mapDbmsConverters.getOrDefault(normalizedDBMS, defaultJDBCConverter);
        return converter.fromColumnType(colType);
    }

    public static ColumnType fromJDBCColumnType(final JDBCColumnType jdbcType) throws UnknownColumnTypeException {
        final Optional<String> dbms = Objects.requireNonNull(jdbcType).getDBMS();
        if (dbms.isPresent())
        {
            final ColumnTypeConverter<JDBCColumnType> converter = mapDbmsConverters.getOrDefault(dbms.get().toLowerCase(), defaultJDBCConverter);
            return converter.toColumnType(jdbcType);
        }
        else
            return defaultJDBCConverter.toColumnType(jdbcType);
    }

}