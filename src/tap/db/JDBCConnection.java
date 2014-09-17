package tap.db;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomishes Rechen Institut (ARI)
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tap.data.DataReadException;
import tap.data.ResultSetTableIterator;
import tap.data.TableIterator;
import tap.log.TAPLog;
import tap.metadata.TAPColumn;
import tap.metadata.TAPForeignKey;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPMetadata.STDSchema;
import tap.metadata.TAPMetadata.STDTable;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.metadata.TAPTable.TableType;
import tap.metadata.TAPType;
import tap.metadata.TAPType.TAPDatatype;
import uws.service.log.UWSLog.LogLevel;
import adql.query.ADQLQuery;
import adql.query.IdentifierField;
import adql.translator.ADQLTranslator;
import adql.translator.JDBCTranslator;
import adql.translator.TranslationException;

/**
 * <p>This {@link DBConnection} implementation is theoretically able to deal with any DBMS JDBC connection.</p>
 * 
 * <p><i>Note:
 * 	"Theoretically", because its design has been done using information about Postgres, SQLite, Oracle, MySQL and Java DB (Derby).
 * 	Then it has been really tested successfully with Postgres and SQLite.
 * </i></p>
 * 
 * <h3>Deal with different DBMS features</h3>
 * 
 * <p>Update queries are taking into account whether the following features are supported by the DBMS:</p>
 * <ul>
 * 	<li><b>data definition</b>: when not supported, no update operation will be possible.
 * 	                            All corresponding functions will then throw a {@link DBException} ;
 * 	                            only {@link #executeQuery(ADQLQuery)} will be possibly called.</li>
 * 
 * 	<li><b>transactions</b>: when not supported, no transaction is started or merely used.
 * 	                         It means that in case of update failure, no rollback will be possible
 * 	                         and that already done modification will remain in the database.</li>
 * 
 * 	<li><b>schemas</b>: when the DBMS does not have the notion of schema (like SQLite), no schema creation or dropping will be obviously processed.
 * 	                    Besides, if not already done, database name of all tables will be prefixed by the schema name.
 * 	                    The prefix to apply is returned by {@link #getTablePrefix(String)}.</li>
 * 
 * 	<li><b>batch updates</b>: when not supported, updates will just be done, "normally, one by one.
 * 	                          In one word, there will be merely no optimization.
 * 	                          Anyway, this feature concerns only the insertions into tables.</li>
 * 
 * 	<li><b>case sensitivity of identifiers</b>: the case sensitivity of quoted identifier varies from the used DBMS. This {@link DBConnection}
 * 	                                            implementation is able to adapt itself in function of the way identifiers are stored and
 * 	                                            researched in the database. How the case sensitivity is managed by the DBMS is the problem
 * 	                                            of only one function (which can be overwritten if needed): {@link #equals(String, String, boolean)}.</li>
 * </ul>
 * 
 * <p><i><b>Warning</b>:
 * 	All these features have no impact at all on ADQL query executions ({@link #executeQuery(ADQLQuery)}).
 * </i></p>
 * 
 * <h3>Datatypes</h3>
 * 
 * <p>Column types are converted from DBMS to TAP types with {@link #getTAPType(String)} and from TAP to DBMS types with {@link #getDBMSDatatype(TAPType)}.</p>
 * 
 * <p>
 * 	All typical DBMS datatypes are taken into account, <b>EXCEPT the geometrical types</b> (POINT and REGION). For these types, the only object having this
 * 	information is the translator thanks to {@link JDBCTranslator#isPointType(String)}, {@link JDBCTranslator#isRegionType(String)},
 * 	{@link JDBCTranslator#getPointType()} and {@link JDBCTranslator#getRegionType()}. The two first functions are used to identify a DBMS type as a point or
 * 	a region (note: several DBMS datatypes may be identified as a geometry type). The two others provide the DBMS type corresponding the best to the TAP types
 * 	POINT and REGION.
 * </p>
 * 
 * <p><i><b>Warning:</b>
 * 	The TAP type REGION can be either a circle, a box or a polygon. Since several DBMS types correspond to one TAP type, {@link JDBCTranslator#getRegionType()}
 * 	MUST return a type covering all these region datatypes. Generally, it will be a VARCHAR whose the values would be STC-S expressions.
 * 	Note that this function is used ONLY WHEN tables with a geometrical value is uploaded. On the contrary, {@link JDBCTranslator#isRegionType(String)}
 * 	is used much more often: in order to write the metadata part of a query result.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 * @since 2.0
 */
public class JDBCConnection implements DBConnection {

	/** DBMS name of PostgreSQL used in the database URL. */
	protected final static String DBMS_POSTGRES = "postgresql";

	/** DBMS name of SQLite used in the database URL. */
	protected final static String DBMS_SQLITE = "sqlite";

	/** DBMS name of MySQL used in the database URL. */
	protected final static String DBMS_MYSQL = "mysql";

	/** DBMS name of Oracle used in the database URL. */
	protected final static String DBMS_ORACLE = "oracle";

	/** Connection ID (typically, the job ID). It lets identify the DB errors linked to the Job execution in the logs. */
	protected final String ID;

	/** JDBC connection (created and initialized at the creation of this {@link JDBCConnection} instance). */
	protected final Connection connection;

	/** The translator this connection must use to translate ADQL into SQL. It is also used to get information about the case sensitivity of all types of identifier (schema, table, column). */
	protected final JDBCTranslator translator;

	/** Object to use if any message needs to be logged. <i>note: this logger may be NULL. If NULL, messages will never be printed.</i> */
	protected final TAPLog logger;

	/* JDBC URL MANAGEMENT */

	/** JDBC prefix of any database URL (for instance: jdbc:postgresql://127.0.0.1/myDB or jdbc:postgresql:myDB). */
	public final static String JDBC_PREFIX = "jdbc";

	/** Name (in lower-case) of the DBMS with which the connection is linked. */
	protected final String dbms;

	/* DBMS SUPPORTED FEATURES */

	/** Indicate whether the DBMS supports transactions (start, commit, rollback and end). <i>note: If no transaction is possible, no transaction will be used, but then, it will never possible to cancel modifications in case of error.</i> */
	protected boolean supportsTransaction;

	/** Indicate whether the DBMS supports the definition of data (create, update, drop, insert into schemas and tables). <i>note: If not supported, it will never possible to create TAP_SCHEMA from given metadata (see {@link #setTAPSchema(TAPMetadata)}) and to upload/drop tables (see {@link #addUploadedTable(TAPTable, TableIterator)} and {@link #dropUploadedTable(TAPTable)}).</i> */
	protected boolean supportsDataDefinition;

	/** Indicate whether the DBMS supports several updates in once (using {@link Statement#addBatch(String)} and {@link Statement#executeBatch()}). <i>note: If not supported, every updates will be done one by one. So it is not really a problem, but just a loss of optimization.</i> */
	protected boolean supportsBatchUpdates;

	/** Indicate whether the DBMS has the notion of SCHEMA. Most of the DBMS has it, but not SQLite for instance. <i>note: If not supported, the DB table name will be prefixed by the DB schema name followed by the character "_". Nevertheless, if the DB schema name is NULL, the DB table name will never be prefixed.</i> */
	protected boolean supportsSchema;

	/* CASE SENSITIVITY SUPPORT */

	/** Indicate whether UNquoted identifiers will be considered as case INsensitive and stored in mixed case by the DBMS. <i>note: If FALSE, unquoted identifiers will still be considered as case insensitive for the researches, but will be stored in lower or upper case (in function of {@link #lowerCaseUnquoted} and {@link #upperCaseUnquoted}). If none of these two flags is TRUE, the storage case will be though considered as mixed.</i> */
	protected boolean supportsMixedCaseUnquotedIdentifier;
	/** Indicate whether the unquoted identifiers are stored in lower case in the DBMS. */
	protected boolean lowerCaseUnquoted;
	/** Indicate whether the unquoted identifiers are stored in upper case in the DBMS. */
	protected boolean upperCaseUnquoted;

	/** Indicate whether quoted identifiers will be considered as case INsensitive and stored in mixed case by the DBMS. <i>note: If FALSE, quoted identifiers will be considered as case sensitive and will be stored either in lower, upper or in mixed case (in function of {@link #lowerCaseQuoted}, {@link #upperCaseQuoted} and {@link #mixedCaseQuoted}). If none of these three flags is TRUE, the storage case will be mixed case.</i> */
	protected boolean supportsMixedCaseQuotedIdentifier;
	/** Indicate whether the quoted identifiers are stored in lower case in the DBMS. */
	protected boolean lowerCaseQuoted;
	/** Indicate whether the quoted identifiers are stored in mixed case in the DBMS. */
	protected boolean mixedCaseQuoted;
	/** Indicate whether the quoted identifiers are stored in upper case in the DBMS. */
	protected boolean upperCaseQuoted;

	/**
	 * <p>Creates a JDBC connection to the specified database and with the specified JDBC driver.
	 * This connection is established using the given user name and password.<p>
	 * 
	 * <p><i><u>note:</u> the JDBC driver is loaded using <pre>Class.forName(driverPath)</pre> and the connection is created with <pre>DriverManager.getConnection(dbUrl, dbUser, dbPassword)</pre>.</i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	This constructor really creates a new SQL connection. Creating a SQL connection is time consuming!
	 * 	That's why it is recommended to use a pool of connections. When doing so, you should use the other constructor of this class
	 * 	({@link #JDBCConnection(Connection, String, TAPLog)}).
	 * </i></p>
	 * 
	 * @param driverPath	Full class name of the JDBC driver.
	 * @param dbUrl			URL to the database. <i><u>note</u> This URL may not be prefixed by "jdbc:". If not, the prefix will be automatically added.</i>
	 * @param dbUser		Name of the database user.
	 * @param dbPassword	Password of the given database user.
	 * @param translator	{@link ADQLTranslator} to use in order to get SQL from an ADQL query and to get qualified DB table names.
	 * @param connID		ID of this connection. <i>note: may be NULL ; but in this case, logs concerning this connection will be more difficult to localize.</i>
	 * @param logger		Logger to use in case of need. <i>note: may be NULL ; in this case, error will never be logged, but sometimes DBException may be raised.</i>
	 * 
	 * @throws DBException	If the driver can not be found or if the connection can not merely be created (usually because DB parameters are wrong).
	 */
	public JDBCConnection(final String driverPath, final String dbUrl, final String dbUser, final String dbPassword, final JDBCTranslator translator, final String connID, final TAPLog logger) throws DBException{
		this(createConnection(driverPath, dbUrl, dbUser, dbPassword), translator, connID, logger);
	}

	/**
	 * Create a JDBC connection by wrapping the given connection.
	 * 
	 * @param conn			Connection to wrap.
	 * @param translator	{@link ADQLTranslator} to use in order to get SQL from an ADQL query and to get qualified DB table names.
	 * @param connID		ID of this connection. <i>note: may be NULL ; but in this case, logs concerning this connection will be more difficult to localize.</i>
	 * @param logger		Logger to use in case of need. <i>note: may be NULL ; in this case, error will never be logged, but sometimes DBException may be raised.</i>
	 */
	public JDBCConnection(final Connection conn, final JDBCTranslator translator, final String connID, final TAPLog logger) throws DBException{
		if (conn == null)
			throw new NullPointerException("Missing SQL connection! => can not create a JDBCConnection object.");
		if (translator == null)
			throw new NullPointerException("Missing ADQL translator! => can not create a JDBCConnection object.");

		this.connection = conn;
		this.translator = translator;
		this.ID = connID;
		this.logger = logger;

		// Set the supporting features' flags + DBMS type:
		try{
			DatabaseMetaData dbMeta = connection.getMetaData();
			dbms = getDBMSName(dbMeta.getURL());
			supportsTransaction = dbMeta.supportsTransactions();
			supportsBatchUpdates = dbMeta.supportsBatchUpdates();
			supportsDataDefinition = dbMeta.supportsDataDefinitionAndDataManipulationTransactions();
			supportsSchema = dbMeta.supportsSchemasInTableDefinitions();
			lowerCaseUnquoted = dbMeta.storesLowerCaseIdentifiers();
			upperCaseUnquoted = dbMeta.storesUpperCaseIdentifiers();
			supportsMixedCaseUnquotedIdentifier = dbMeta.supportsMixedCaseIdentifiers();
			lowerCaseQuoted = dbMeta.storesLowerCaseQuotedIdentifiers();
			mixedCaseQuoted = dbMeta.storesMixedCaseQuotedIdentifiers();
			upperCaseQuoted = dbMeta.storesUpperCaseQuotedIdentifiers();
			supportsMixedCaseQuotedIdentifier = dbMeta.supportsMixedCaseQuotedIdentifiers();
		}catch(SQLException se){
			throw new DBException("Unable to access to one or several DB metadata (url, supportsTransaction, supportsBatchUpdates, supportsDataDefinitionAndDataManipulationTransactions, supportsSchemasInTableDefinitions, storesLowerCaseIdentifiers, storesUpperCaseIdentifiers, supportsMixedCaseIdentifiers, storesLowerCaseQuotedIdentifiers, storesMixedCaseQuotedIdentifiers, storesUpperCaseQuotedIdentifiers and supportsMixedCaseQuotedIdentifiers) from the given Connection!");
		}
	}

	/**
	 * Extract the DBMS name from the given database URL.
	 * 
	 * @param dbUrl	JDBC URL to access the database. <b>This URL must start with "jdbc:" ; otherwise an exception will be thrown.</b>
	 * 
	 * @return	The DBMS name as found in the given URL.
	 * 
	 * @throws DBException	If NULL has been given, if the URL is not a JDBC one (starting with "jdbc:") or if the DBMS name is missing.
	 */
	protected static final String getDBMSName(String dbUrl) throws DBException{
		if (dbUrl == null)
			throw new DBException("Missing database URL!");

		if (!dbUrl.startsWith(JDBC_PREFIX + ":"))
			throw new DBException("This DBConnection implementation is only able to deal with JDBC connection! (the DB URL must start with \"" + JDBC_PREFIX + ":\" ; given url: " + dbUrl + ")");

		dbUrl = dbUrl.substring(5);
		int indSep = dbUrl.indexOf(':');
		if (indSep <= 0)
			throw new DBException("Incorrect database URL: " + dbUrl);

		return dbUrl.substring(0, indSep).toLowerCase();
	}

	/**
	 * Create a {@link Connection} instance using the specified JDBC Driver and the given database parameters.
	 * 
	 * @param driverPath	Path to the JDBC driver.
	 * @param dbUrl			JDBC URL to connect to the database. <i><u>note</u> This URL may not be prefixed by "jdbc:". If not, the prefix will be automatically added.</i>
	 * @param dbUser		Name of the user to use to connect to the database.
	 * @param dbPassword	Password of the user to use to connect to the database.
	 * 
	 * @return	A new DB connection.
	 * 
	 * @throws DBException	If the driver can not be found or if the connection can not merely be created (usually because DB parameters are wrong).
	 * 
	 * @see DriverManager#getConnection(String, String, String)
	 */
	private final static Connection createConnection(final String driverPath, final String dbUrl, final String dbUser, final String dbPassword) throws DBException{
		// Load the specified JDBC driver:
		try{
			Class.forName(driverPath);
		}catch(ClassNotFoundException cnfe){
			throw new DBException("Impossible to find the JDBC driver \"" + driverPath + "\" !", cnfe);
		}

		// Build a connection to the specified database:
		String url = dbUrl.startsWith(JDBC_PREFIX) ? dbUrl : (JDBC_PREFIX + dbUrl);
		try{
			return DriverManager.getConnection(url, dbUser, dbPassword);
		}catch(SQLException se){
			throw new DBException("Impossible to establish a connection to the database \"" + url + "\" !", se);
		}
	}

	@Override
	public final String getID(){
		return ID;
	}

	/**
	 * <p>Get the JDBC connection wrapped by this {@link JDBCConnection} object.</p>
	 * 
	 * <p><i>Note:
	 * 	This is the best way to get the JDBC connection in order to properly close it. 
	 * </i></p>
	 * 
	 * @return	The wrapped JDBC connection.
	 */
	public final Connection getInnerConnection(){
		return connection;
	}

	/* ********************* */
	/* INTERROGATION METHODS */
	/* ********************* */
	@Override
	public TableIterator executeQuery(final ADQLQuery adqlQuery) throws DBException{
		String sql = null;
		ResultSet result = null;
		try{
			// 1. Translate the ADQL query into SQL:
			logger.logDB(LogLevel.INFO, this, "TRANSLATE", "Translating ADQL: " + adqlQuery.toADQL().replaceAll("(\t|\r?\n)+", " "), null);
			sql = translator.translate(adqlQuery);

			// 2. Execute the SQL query:
			Statement stmt = connection.createStatement();
			logger.logDB(LogLevel.INFO, this, "EXECUTE", "Executing translated query: " + sql.replaceAll("(\t|\r?\n)+", " "), null);
			result = stmt.executeQuery(sql);

			// 3. Return the result through a TableIterator object:
			logger.logDB(LogLevel.INFO, this, "RESULT", "Returning result", null);
			return new ResultSetTableIterator(result, dbms, adqlQuery.getResultingColumns());

		}catch(SQLException se){
			close(result);
			logger.logDB(LogLevel.ERROR, this, "EXECUTE", "Unexpected error while EXECUTING SQL query!", se);
			throw new DBException("Unexpected error while executing a SQL query: " + se.getMessage(), se);
		}catch(TranslationException te){
			close(result);
			logger.logDB(LogLevel.ERROR, this, "TRANSLATE", "Unexpected error while TRANSLATING ADQL into SQL!", te);
			throw new DBException("Unexpected error while translating ADQL into SQL: " + te.getMessage(), te);
		}catch(DataReadException dre){
			close(result);
			logger.logDB(LogLevel.ERROR, this, "RESULT", "Unexpected error while reading the query result!", dre);
			throw new DBException("Impossible to read the query result, because: " + dre.getMessage(), dre);
		}
	}

	/* *********************** */
	/* TAP_SCHEMA MANIPULATION */
	/* *********************** */

	/**
	 * Tell when, compared to the other TAP standard tables, a given standard TAP table should be created.
	 * 
	 * @param table	Standard TAP table.
	 * 
	 * @return	An index between 0 and 4 (included) - 0 meaning the first table to create whereas 4 is the last one.
	 *          -1 is returned if NULL is given in parameter of if the standard table is not taken into account here.
	 */
	protected int getCreationOrder(final STDTable table){
		if (table == null)
			return -1;

		switch(table){
			case SCHEMAS:
				return 0;
			case TABLES:
				return 1;
			case COLUMNS:
				return 2;
			case KEYS:
				return 3;
			case KEY_COLUMNS:
				return 4;
			default:
				return -1;
		}
	}

	/* ************************************ */
	/* GETTING TAP_SCHEMA FROM THE DATABASE */
	/* ************************************ */

	/**
	 * <p>In this implementation, this function is first creating a virgin {@link TAPMetadata} object
	 * that will be filled progressively by calling the following functions:</p>
	 * <ol>
	 * 	<li>{@link #loadSchemas(TAPTable, TAPMetadata, Statement)}</li>
	 * 	<li>{@link #loadTables(TAPTable, TAPMetadata, Statement)}</li>
	 * 	<li>{@link #loadColumns(TAPTable, List, Statement)}</li>
	 * 	<li>{@link #loadKeys(TAPTable, TAPTable, List, Statement)}</li>
	 * </ol>
	 * 
	 * <p><i>Note:
	 * 	If schemas are not supported by this DBMS connection, the DB name of all tables will be set to NULL
	 * 	and the DB name of all tables will be prefixed by the ADQL name of their respective schema (using {@link #getTablePrefix(String)}).
	 * </i></p>
	 * 
	 * @see tap.db.DBConnection#getTAPSchema()
	 */
	@Override
	public TAPMetadata getTAPSchema() throws DBException{
		// Build a virgin TAP metadata:
		TAPMetadata metadata = new TAPMetadata();

		// Get the definition of the standard TAP_SCHEMA tables:
		TAPSchema tap_schema = TAPMetadata.getStdSchema();

		// If schemas are not supported by the DBMS connection, the schema must not be translated in the DB:
		if (!supportsSchema){
			String namePrefix = getTablePrefix(tap_schema.getADQLName());
			tap_schema.setDBName(null);
			for(TAPTable t : tap_schema)
				t.setDBName(namePrefix + t.getDBName());
		}

		// LOAD ALL METADATA FROM THE STANDARD TAP TABLES:
		Statement stmt = null;
		try{
			// create a common statement for all loading functions:
			stmt = connection.createStatement();

			// load all schemas from TAP_SCHEMA.schemas:
			logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.schemas.", null);
			loadSchemas(tap_schema.getTable(STDTable.SCHEMAS.label), metadata, stmt);

			// load all tables from TAP_SCHEMA.tables:
			logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.tables.", null);
			List<TAPTable> lstTables = loadTables(tap_schema.getTable(STDTable.TABLES.label), metadata, stmt);

			// load all columns from TAP_SCHEMA.columns:
			logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.columns.", null);
			loadColumns(tap_schema.getTable(STDTable.COLUMNS.label), lstTables, stmt);

			// load all foreign keys from TAP_SCHEMA.keys and TAP_SCHEMA.key_columns:
			logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.keys and TAP_SCHEMA.key_columns.", null);
			loadKeys(tap_schema.getTable(STDTable.KEYS.label), tap_schema.getTable(STDTable.KEY_COLUMNS.label), lstTables, stmt);

		}catch(SQLException se){
			logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to create a Statement!", se);
			throw new DBException("Can not create a Statement!", se);
		}finally{
			close(stmt);
		}

		return metadata;
	}

	/**
	 * <p>Load into the given metadata all schemas listed in TAP_SCHEMA.schemas.</p>
	 * 
	 * <p><i>Note:
	 * 	If schemas are not supported by this DBMS connection, the DB name of the loaded schemas is set to NULL.
	 * </i></p>
	 * 
	 * @param tablesDef		Definition of the table TAP_SCHEMA.schemas.
	 * @param metadata		Metadata to fill with all found schemas.
	 * @param stmt			Statement to use in order to interact with the database.
	 * 
	 * @throws DBException	If any error occurs while interacting with the database. 
	 */
	protected void loadSchemas(final TAPTable tableDef, final TAPMetadata metadata, final Statement stmt) throws DBException{
		ResultSet rs = null;
		try{
			// Build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("SELECT ");
			sqlBuf.append(translator.getColumnName(tableDef.getColumn("schema_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("utype")));
			sqlBuf.append(" FROM ").append(translator.getQualifiedTableName(tableDef)).append(';');

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all schemas:
			while(rs.next()){
				String schemaName = rs.getString(1), description = rs.getString(2), utype = rs.getString(3);

				// create the new schema:
				TAPSchema newSchema = new TAPSchema(schemaName, nullifyIfNeeded(description), nullifyIfNeeded(utype));

				// If schemas are not supported by the DBMS connection, the schema must not be translated in the DB:
				if (!supportsSchema)
					newSchema.setDBName(null);

				// add the new schema inside the given metadata:
				metadata.addSchema(newSchema);
			}
		}catch(SQLException se){
			logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load schemas from TAP_SCHEMA.schemas!", se);
			throw new DBException("Impossible to load schemas from TAP_SCHEMA.schemas!", se);
		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Load into the corresponding metadata all tables listed in TAP_SCHEMA.tables.</p>
	 * 
	 * <p><i>Note:
	 * 	Schemas are searched in the given metadata by their ADQL name and case sensitively.
	 * 	If they can not be found a {@link DBException} is thrown.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If schemas are not supported by this DBMS connection, the DB name of the loaded {@link TAPTable}s is prefixed by the ADQL name of their respective schema.
	 * 	The table prefix is built by {@link #getTablePrefix(String)}.  
	 * </i></p>
	 * 
	 * @param tablesDef		Definition of the table TAP_SCHEMA.tables.
	 * @param metadata		Metadata (containing already all schemas listed in TAP_SCHEMA.schemas).
	 * @param stmt			Statement to use in order to interact with the database.
	 * 
	 * @return	The complete list of all loaded tables. <i>note: this list is required by {@link #loadColumns(TAPTable, List, Statement)}.</i> 
	 * 
	 * @throws DBException	If a schema can not be found, or if any other error occurs while interacting with the database.
	 * 
	 * @see #getTablePrefix(String) 
	 */
	protected List<TAPTable> loadTables(final TAPTable tableDef, final TAPMetadata metadata, final Statement stmt) throws DBException{
		ResultSet rs = null;
		try{
			// Build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("SELECT ");
			sqlBuf.append(translator.getColumnName(tableDef.getColumn("schema_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("table_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("table_type")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("utype")));
			sqlBuf.append(" FROM ").append(translator.getQualifiedTableName(tableDef)).append(';');

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all tables:
			ArrayList<TAPTable> lstTables = new ArrayList<TAPTable>();
			while(rs.next()){
				String schemaName = rs.getString(1), tableName = rs.getString(2), typeStr = rs.getString(3), description = rs.getString(4), utype = rs.getString(5);

				// get the schema:
				TAPSchema schema = metadata.getSchema(schemaName);
				if (schema == null){
					logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to find the schema of the table \"" + tableName + "\": \"" + schemaName + "\"!", null);
					throw new DBException("Impossible to find the schema of the table \"" + tableName + "\": \"" + schemaName + "\"!");
				}

				// resolve the table type (if any) ; by default, it will be "table":
				TableType type = TableType.table;
				if (typeStr != null){
					try{
						type = TableType.valueOf(typeStr.toLowerCase());
					}catch(IllegalArgumentException iae){}
				}

				// create the new table:
				TAPTable newTable = new TAPTable(tableName, type, nullifyIfNeeded(description), nullifyIfNeeded(utype));

				// If schemas are not supported by the DBMS connection, the DB table name must be prefixed by the schema name:
				if (!supportsSchema)
					newTable.setDBName(getTablePrefix(schema.getADQLName()) + newTable.getDBName());

				// add the new table inside its corresponding schema:
				schema.addTable(newTable);
				lstTables.add(newTable);
			}

			return lstTables;
		}catch(SQLException se){
			logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load tables from TAP_SCHEMA.tables!", se);
			throw new DBException("Impossible to load tables from TAP_SCHEMA.tables!", se);
		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Load into the corresponding tables all columns listed in TAP_SCHEMA.columns.</p>
	 * 
	 * <p><i>Note:
	 * 	Tables are searched in the given list by their ADQL name and case sensitively.
	 * 	If they can not be found a {@link DBException} is thrown.
	 * </i></p>
	 * 
	 * @param columnsDef	Definition of the table TAP_SCHEMA.columns.
	 * @param lstTables		List of all published tables (= all tables listed in TAP_SCHEMA.tables).
	 * @param stmt			Statement to use in order to interact with the database.
	 * 
	 * @throws DBException	If a table can not be found, or if any other error occurs while interacting with the database. 
	 */
	protected void loadColumns(final TAPTable tableDef, final List<TAPTable> lstTables, final Statement stmt) throws DBException{
		ResultSet rs = null;
		try{
			// Build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("SELECT ");
			sqlBuf.append(translator.getColumnName(tableDef.getColumn("table_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("column_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("unit")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("ucd")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("utype")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("datatype")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("size")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("principal")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("indexed")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("std")));
			sqlBuf.append(" FROM ").append(translator.getQualifiedTableName(tableDef)).append(';');

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all tables:
			while(rs.next()){
				String tableName = rs.getString(1), columnName = rs.getString(2), description = rs.getString(3), unit = rs.getString(4), ucd = rs.getString(5), utype = rs.getString(6), datatype = rs.getString(7);
				int size = rs.getInt(8);
				boolean principal = toBoolean(rs.getObject(9)), indexed = toBoolean(rs.getObject(10)), std = toBoolean(rs.getObject(11));

				// get the table:
				TAPTable table = searchTable(tableName, lstTables.iterator());
				if (table == null){
					logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to find the table of the column \"" + columnName + "\": \"" + tableName + "\"!", null);
					throw new DBException("Impossible to find the table of the column \"" + columnName + "\": \"" + tableName + "\"!");
				}

				// resolve the column type (if any) ; by default, it will be "VARCHAR" if unknown or missing:
				TAPDatatype tapDatatype = null;
				// ...try to resolve the datatype in function of all datatypes declared by the TAP standard.
				if (datatype != null){
					try{
						tapDatatype = TAPDatatype.valueOf(datatype.toUpperCase());
					}catch(IllegalArgumentException iae){}
				}
				// ...build the column type:
				TAPType type;
				if (tapDatatype == null)
					type = new TAPType(TAPDatatype.VARCHAR);
				else
					type = new TAPType(tapDatatype, size);

				// create the new column:
				TAPColumn newColumn = new TAPColumn(columnName, type, nullifyIfNeeded(description), nullifyIfNeeded(unit), nullifyIfNeeded(ucd), nullifyIfNeeded(utype));
				newColumn.setPrincipal(principal);
				newColumn.setIndexed(indexed);
				newColumn.setStd(std);

				// add the new column inside its corresponding table:
				table.addColumn(newColumn);
			}
		}catch(SQLException se){
			logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load columns from TAP_SCHEMA.columns!", se);
			throw new DBException("Impossible to load columns from TAP_SCHEMA.columns!", se);
		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Load into the corresponding tables all keys listed in TAP_SCHEMA.keys and detailed in TAP_SCHEMA.key_columns.</p>
	 * 
	 * <p><i>Note:
	 * 	Tables and columns are searched in the given list by their ADQL name and case sensitively.
	 * 	If they can not be found a {@link DBException} is thrown.
	 * </i></p>
	 * 
	 * @param keysDef		Definition of the table TAP_SCHEMA.keys.
	 * @param keyColumnsDef	Definition of the table TAP_SCHEMA.key_columns.
	 * @param lstTables		List of all published tables (= all tables listed in TAP_SCHEMA.tables).
	 * @param stmt			Statement to use in order to interact with the database.
	 * 
	 * @throws DBException	If a table or a column can not be found, or if any other error occurs while interacting with the database. 
	 */
	protected void loadKeys(final TAPTable keysDef, final TAPTable keyColumnsDef, final List<TAPTable> lstTables, final Statement stmt) throws DBException{
		ResultSet rs = null;
		PreparedStatement keyColumnsStmt = null;
		try{
			// Prepare the query to get the columns of each key:
			StringBuffer sqlBuf = new StringBuffer("SELECT ");
			sqlBuf.append(translator.getColumnName(keyColumnsDef.getColumn("key_id")));
			sqlBuf.append(", ").append(translator.getColumnName(keyColumnsDef.getColumn("from_column")));
			sqlBuf.append(", ").append(translator.getColumnName(keyColumnsDef.getColumn("target_column")));
			sqlBuf.append(" FROM ").append(translator.getQualifiedTableName(keyColumnsDef));
			sqlBuf.append(" WHERE ").append(translator.getColumnName(keyColumnsDef.getColumn("key_id"))).append(" = ?").append(';');
			keyColumnsStmt = connection.prepareStatement(sqlBuf.toString());

			// Build the SQL query to get the keys:
			sqlBuf.delete(0, sqlBuf.length());
			sqlBuf.append("SELECT ").append(translator.getColumnName(keysDef.getColumn("key_id")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("from_table")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("target_table")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("utype")));
			sqlBuf.append(" FROM ").append(translator.getQualifiedTableName(keysDef)).append(';');

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all foreign keys:
			while(rs.next()){
				String key_id = rs.getString(1), from_table = rs.getString(2), target_table = rs.getString(3), description = rs.getString(4), utype = rs.getString(5);

				// get the two tables (source and target):
				TAPTable sourceTable = searchTable(from_table, lstTables.iterator());
				if (sourceTable == null){
					logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to find the source table of the foreign key \"" + key_id + "\": \"" + from_table + "\"!", null);
					throw new DBException("Impossible to find the source table of the foreign key \"" + key_id + "\": \"" + from_table + "\"!");
				}
				TAPTable targetTable = searchTable(target_table, lstTables.iterator());
				if (targetTable == null){
					logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to find the target table of the foreign key \"" + key_id + "\": \"" + target_table + "\"!", null);
					throw new DBException("Impossible to find the target table of the foreign key \"" + key_id + "\": \"" + target_table + "\"!");
				}

				// get the list of columns joining the two tables of the foreign key:
				HashMap<String,String> columns = new HashMap<String,String>();
				ResultSet rsKeyCols = null;
				try{
					keyColumnsStmt.setString(1, key_id);
					rsKeyCols = keyColumnsStmt.executeQuery();
					while(rsKeyCols.next())
						columns.put(rsKeyCols.getString(1), rsKeyCols.getString(2));
				}catch(SQLException se){
					logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load key columns from TAP_SCHEMA.key_columns for the foreign key: \"" + key_id + "\"!", se);
					throw new DBException("Impossible to load key columns from TAP_SCHEMA.key_columns for the foreign key: \"" + key_id + "\"!", se);
				}finally{
					close(rsKeyCols);
				}

				// create and add the new foreign key inside the source table:
				try{
					sourceTable.addForeignKey(key_id, targetTable, columns, nullifyIfNeeded(description), nullifyIfNeeded(utype));
				}catch(Exception ex){
					logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to create the foreign key \"" + key_id + "\" because: " + ex.getMessage(), ex);
					throw new DBException("Impossible to create the foreign key \"" + key_id + "\" because: " + ex.getMessage(), ex);
				}
			}
		}catch(SQLException se){
			logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load columns from TAP_SCHEMA.columns!", se);
			throw new DBException("Impossible to load columns from TAP_SCHEMA.columns!", se);
		}finally{
			close(rs);
			close(keyColumnsStmt);
		}
	}

	/* ********************************** */
	/* SETTING TAP_SCHEMA IN THE DATABASE */
	/* ********************************** */

	/**
	 * <p>This function is just calling the following functions:</p>
	 * <ol>
	 * 	<li>{@link #mergeTAPSchemaDefs(TAPMetadata)}</li>
	 * 	<li>{@link #startTransaction()}</li>
	 * 	<li>{@link #resetTAPSchema(Statement, TAPTable[])}</li>
	 * 	<li>{@link #createTAPSchemaTable(TAPTable, Statement)} for each standard TAP_SCHEMA table</li>
	 * 	<li>{@link #fillTAPSchema(TAPMetadata)}</li>
	 * 	<li>{@link #createTAPTableIndexes(TAPTable, Statement)} for each standard TA_SCHEMA table</li>
	 * 	<li>{@link #commit()} or {@link #rollback()}</li>
	 * 	<li>{@link #endTransaction()}</li>
	 * </ol>
	 * 
	 * <p><i><b>Important note:
	 * 	If the connection does not support transactions, then there will be merely no transaction.
	 * 	Consequently, any failure (exception/error) will not clean the partial modifications done by this function.
	 * </i></p>
	 * 
	 * @see tap.db.DBConnection#setTAPSchema(tap.metadata.TAPMetadata)
	 */
	@Override
	public void setTAPSchema(final TAPMetadata metadata) throws DBException{
		Statement stmt = null;

		try{
			// A. GET THE DEFINITION OF ALL STANDARD TAP TABLES:
			TAPTable[] stdTables = mergeTAPSchemaDefs(metadata);

			startTransaction();

			// B. RE-CREATE THE STANDARD TAP_SCHEMA TABLES:
			stmt = connection.createStatement();

			// 1. Ensure TAP_SCHEMA exists and drop all its standard TAP tables:
			logger.logDB(LogLevel.INFO, this, "CLEAN_TAP_SCHEMA", "Cleaning TAP_SCHEMA.", null);
			resetTAPSchema(stmt, stdTables);

			// 2. Create all standard TAP tables:
			logger.logDB(LogLevel.INFO, this, "CREATE_TAP_SCHEMA", "Creating TAP_SCHEMA tables.", null);
			for(TAPTable table : stdTables)
				createTAPSchemaTable(table, stmt);

			// C. FILL THE NEW TABLE USING THE GIVEN DATA ITERATOR:
			logger.logDB(LogLevel.INFO, this, "CREATE_TAP_SCHEMA", "Filling TAP_SCHEMA tables.", null);
			fillTAPSchema(metadata);

			// D. CREATE THE INDEXES OF ALL STANDARD TAP TABLES:
			logger.logDB(LogLevel.INFO, this, "CREATE_TAP_SCHEMA", "Creating TAP_SCHEMA tables' indexes.", null);
			for(TAPTable table : stdTables)
				createTAPTableIndexes(table, stmt);

			commit();
		}catch(SQLException se){
			logger.logDB(LogLevel.ERROR, this, "CREATE_TAP_SCHEMA", "Impossible to SET TAP_SCHEMA in DB!", se);
			rollback();
			throw new DBException("Impossible to SET TAP_SCHEMA in DB!", se);
		}finally{
			close(stmt);
			endTransaction();
		}
	}

	/**
	 * <p>Merge the definition of TAP_SCHEMA tables given in parameter with the definition provided in the TAP standard.</p>
	 * 
	 * <p>
	 * 	The goal is to get in output the list of all standard TAP_SCHEMA tables. But it must take into account the customized
	 * 	definition given in parameter if there is one. Indeed, if a part of TAP_SCHEMA is not provided, it will be completed here by the
	 * 	definition provided in the TAP standard. And so, if the whole TAP_SCHEMA is not provided at all, the returned tables will be those
	 * 	of the IVOA standard.
	 * </p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If the TAP_SCHEMA definition is missing or incomplete in the given metadata, it will be added or completed automatically
	 * 	by this function with the definition provided in the IVOA TAP standard.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	Only the standard tables of TAP_SCHEMA are considered. The others are skipped (that's to say: never returned by this function ;
	 *  however, they will stay in the given metadata).
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If schemas are not supported by this DBMS connection, the DB name of schemas is set to NULL and
	 * 	the DB name of tables is prefixed by the schema name (using {@link #getTablePrefix(String)}).
	 * </i></p>
	 * 
	 * @param metadata	Metadata (with or without TAP_SCHEMA schema or some of its table). <i>Must not be NULL</i>
	 * 
	 * @return	The list of all standard TAP_SCHEMA tables, ordered by creation order (see {@link #getCreationOrder(STDTable)}).
	 * 
	 * @see TAPMetadata#resolveStdTable(String)
	 * @see TAPMetadata#getStdSchema()
	 * @see TAPMetadata#getStdTable(STDTable)
	 */
	protected TAPTable[] mergeTAPSchemaDefs(final TAPMetadata metadata){
		// 1. Get the TAP_SCHEMA schema from the given metadata:
		TAPSchema tapSchema = null;
		Iterator<TAPSchema> itSchema = metadata.iterator();
		while(tapSchema == null && itSchema.hasNext()){
			TAPSchema schema = itSchema.next();
			if (schema.getADQLName().equalsIgnoreCase(STDSchema.TAPSCHEMA.label))
				tapSchema = schema;
		}

		// 2. Get the provided definition of the standard TAP tables:
		TAPTable[] customStdTables = new TAPTable[5];
		if (tapSchema != null){

			/* if the schemas are not supported with this DBMS,
			 * remove its DB name: */
			if (!supportsSchema)
				tapSchema.setDBName(null);

			// retrieve only the standard TAP tables:
			Iterator<TAPTable> itTable = tapSchema.iterator();
			while(itTable.hasNext()){
				TAPTable table = itTable.next();
				int indStdTable = getCreationOrder(TAPMetadata.resolveStdTable(table.getADQLName()));
				if (indStdTable > -1)
					customStdTables[indStdTable] = table;
			}
		}

		// 3. Build a common TAPSchema, if needed:
		if (tapSchema == null){

			// build a new TAP_SCHEMA definition based on the standard definition:
			tapSchema = TAPMetadata.getStdSchema();

			/* if the schemas are not supported with this DBMS,
			 * remove its DB name: */
			if (!supportsSchema)
				tapSchema.setDBName(null);

			// add the new TAP_SCHEMA definition in the given metadata object:
			metadata.addSchema(tapSchema);
		}

		// 4. Finally, build the join between the standard tables and the custom ones:
		TAPTable[] stdTables = new TAPTable[]{TAPMetadata.getStdTable(STDTable.SCHEMAS),TAPMetadata.getStdTable(STDTable.TABLES),TAPMetadata.getStdTable(STDTable.COLUMNS),TAPMetadata.getStdTable(STDTable.KEYS),TAPMetadata.getStdTable(STDTable.KEY_COLUMNS)};
		for(int i = 0; i < stdTables.length; i++){

			// CASE: no custom definition:
			if (customStdTables[i] == null){
				/* if the schemas are not supported with this DBMS,
				 * prefix the DB name with "tap_schema_": */
				if (!supportsSchema)
					stdTables[i].setDBName(getTablePrefix(tapSchema.getADQLName()) + stdTables[i].getDBName());
				// add the table to the fetched or built-in schema:
				tapSchema.addTable(stdTables[i]);
			}
			// CASE: custom definition
			else
				stdTables[i] = customStdTables[i];
		}

		return stdTables;
	}

	/**
	 * <p>Ensure the TAP_SCHEMA schema exists in the database AND it must especially drop all of its standard tables
	 * (schemas, tables, columns, keys and key_columns), if they exist.</p>
	 * 
	 * <p><i><b>Important note</b>:
	 * 	If TAP_SCHEMA already exists and contains other tables than the standard ones, they will not be dropped and they will stay in place.
	 * </i></p>
	 * 
	 * @param stmt			The statement to use in order to interact with the database.
	 * @param stdTables		List of all standard tables that must be (re-)created.
	 *                      They will be used just to know the name of the standard tables that should be dropped here. 
	 * 
	 * @throws SQLException	If any error occurs while querying or updating the database.
	 */
	protected void resetTAPSchema(final Statement stmt, final TAPTable[] stdTables) throws SQLException{
		DatabaseMetaData dbMeta = connection.getMetaData();

		// 1. Get the qualified DB schema name:
		String dbSchemaName = stdTables[0].getDBSchemaName();

		/* 2. Test whether the schema TAP_SCHEMA exists
		 *    and if it does not, create it: */
		if (dbSchemaName != null){
			// test whether the schema TAP_SCHEMA exists:
			boolean hasTAPSchema = isSchemaExisting(dbSchemaName, dbMeta);

			// create TAP_SCHEMA if it does not exist:
			if (!hasTAPSchema)
				stmt.executeUpdate("CREATE SCHEMA " + translator.getQualifiedSchemaName(stdTables[0]) + ";");
		}

		// 2-bis. Drop all its standard tables:
		dropTAPSchemaTables(stdTables, stmt, dbMeta);
	}

	/**
	 * <p>Remove/Drop all standard TAP_SCHEMA tables given in parameter.</p>
	 * 
	 * <p><i>Note:
	 * 	To test the existence of tables to drop, {@link DatabaseMetaData#getTables(String, String, String, String[])} is called.
	 * 	Then the schema and table names are compared with the case sensitivity defined by the translator.
	 * 	Only tables matching with these comparisons will be dropped.
	 * </i></p>
	 * 
	 * @param stdTables	Tables to drop. (they should be provided ordered by their creation order (see {@link #getCreationOrder(STDTable)})).
	 * @param stmt		Statement to use in order to interact with the database.
	 * @param dbMeta	Database metadata. Used to list all existing tables.
	 * 
	 * @throws SQLException	If any error occurs while querying or updating the database.
	 * 
	 * @see ADQLTranslator#isCaseSensitive(IdentifierField)
	 */
	private void dropTAPSchemaTables(final TAPTable[] stdTables, final Statement stmt, final DatabaseMetaData dbMeta) throws SQLException{
		String[] stdTablesToDrop = new String[]{null,null,null,null,null};

		ResultSet rs = null;
		try{
			// Retrieve only the schema name and determine whether the search should be case sensitive:
			String tapSchemaName = stdTables[0].getDBSchemaName();
			boolean schemaCaseSensitive = translator.isCaseSensitive(IdentifierField.SCHEMA);
			boolean tableCaseSensitive = translator.isCaseSensitive(IdentifierField.TABLE);

			// Identify which standard TAP tables must be dropped:
			rs = dbMeta.getTables(null, null, null, null);
			while(rs.next()){
				String rsSchema = nullifyIfNeeded(rs.getString(2)), rsTable = rs.getString(3);
				if (!supportsSchema || (tapSchemaName == null && rsSchema == null) || equals(rsSchema, tapSchemaName, schemaCaseSensitive)){
					int indStdTable;
					indStdTable = getCreationOrder(isStdTable(rsTable, stdTables, tableCaseSensitive));
					if (indStdTable > -1){
						stdTablesToDrop[indStdTable] = (rsSchema != null ? "\"" + rsSchema + "\"." : "") + "\"" + rsTable + "\"";
					}
				}
			}
		}finally{
			close(rs);
		}

		// Drop the existing tables (in the reverse order of creation):
		for(int i = stdTablesToDrop.length - 1; i >= 0; i--){
			if (stdTablesToDrop[i] != null)
				stmt.executeUpdate("DROP TABLE " + stdTablesToDrop[i] + ";");
		}
	}

	/**
	 * <p>Create the specified standard TAP_SCHEMA tables into the database.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	Only standard TAP_SCHEMA tables (schemas, tables, columns, keys and key_columns) can be created here.
	 * 	If the given table is not part of the schema TAP_SCHEMA (comparison done on the ADQL name case-sensitively)
	 * 	and is not a standard TAP_SCHEMA table (comparison done on the ADQL name case-sensitively),
	 * 	this function will do nothing and will throw an exception.
	 * </i></p>
	 * 
	 * @param table	Table to create.
	 * @param stmt	Statement to use in order to interact with the database.
	 * 
	 * @throws DBException	If the given table is not a standard TAP_SCHEMA table.
	 * @throws SQLException	If any error occurs while querying or updating the database.
	 */
	protected void createTAPSchemaTable(final TAPTable table, final Statement stmt) throws DBException, SQLException{
		// 1. ENSURE THE GIVEN TABLE IS REALLY A TAP_SCHEMA TABLE (according to the ADQL names):
		if (!table.getADQLSchemaName().equalsIgnoreCase(STDSchema.TAPSCHEMA.label) || TAPMetadata.resolveStdTable(table.getADQLName()) == null)
			throw new DBException("Forbidden table creation: " + table + " is not a standard table of TAP_SCHEMA!");

		// 2. BUILD THE SQL QUERY TO CREATE THE TABLE:
		StringBuffer sql = new StringBuffer("CREATE TABLE ");

		// a. Write the fully qualified table name:
		sql.append(translator.getQualifiedTableName(table));

		// b. List all the columns:
		sql.append('(');
		Iterator<TAPColumn> it = table.getColumns();
		while(it.hasNext()){
			TAPColumn col = it.next();

			// column name:
			sql.append(translator.getColumnName(col));

			// column type:
			sql.append(' ').append(getDBMSDatatype(col.getDatatype()));

			// last column ?
			if (it.hasNext())
				sql.append(',');
		}

		// c. Append the primary key definition, if needed:
		String primaryKey = getPrimaryKeyDef(table.getADQLName());
		if (primaryKey != null)
			sql.append(',').append(primaryKey);

		// d. End the query:
		sql.append(')').append(';');

		// 3. FINALLY CREATE THE TABLE:
		stmt.executeUpdate(sql.toString());
	}

	/**
	 * <p>Get the primary key corresponding to the specified table.</p>
	 * 
	 * <p>If the specified table is not a standard TAP_SCHEMA table, NULL will be returned.</p>
	 * 
	 * @param tableName	ADQL table name.
	 * 
	 * @return	The primary key definition (prefixed by a space) corresponding to the specified table (ex: " PRIMARY KEY(schema_name)"),
	 *          or NULL if the specified table is not a standard TAP_SCHEMA table.
	 */
	private String getPrimaryKeyDef(final String tableName){
		STDTable stdTable = TAPMetadata.resolveStdTable(tableName);
		if (stdTable == null)
			return null;

		boolean caseSensitive = translator.isCaseSensitive(IdentifierField.COLUMN);
		switch(stdTable){
			case SCHEMAS:
				return " PRIMARY KEY(" + (caseSensitive ? "\"schema_name\"" : "schema_name") + ")";
			case TABLES:
				return " PRIMARY KEY(" + (caseSensitive ? "\"schema_name\"" : "schema_name") + ", " + (caseSensitive ? "\"table_name\"" : "table_name") + ")";
			case COLUMNS:
				return " PRIMARY KEY(" + (caseSensitive ? "\"table_name\"" : "table_name") + ", " + (caseSensitive ? "\"column_name\"" : "column_name") + ")";
			case KEYS:
			case KEY_COLUMNS:
				return " PRIMARY KEY(" + (caseSensitive ? "\"key_id\"" : "key_id") + ")";
			default:
				return null;
		}
	}

	/**
	 * <p>Create the DB indexes corresponding to the given TAP_SCHEMA table.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	Only standard TAP_SCHEMA tables (schemas, tables, columns, keys and key_columns) can be created here.
	 * 	If the given table is not part of the schema TAP_SCHEMA (comparison done on the ADQL name case-sensitively)
	 * 	and is not a standard TAP_SCHEMA table (comparison done on the ADQL name case-sensitively),
	 * 	this function will do nothing and will throw an exception.
	 * </i></p>
	 * 
	 * @param table	Table whose indexes must be created here.
	 * @param stmt	Statement to use in order to interact with the database.
	 * 
	 * @throws DBException	If the given table is not a standard TAP_SCHEMA table.
	 * @throws SQLException	If any error occurs while querying or updating the database.
	 */
	protected void createTAPTableIndexes(final TAPTable table, final Statement stmt) throws DBException, SQLException{
		// 1. Ensure the given table is really a TAP_SCHEMA table (according to the ADQL names):
		if (!table.getADQLSchemaName().equalsIgnoreCase(STDSchema.TAPSCHEMA.label) || TAPMetadata.resolveStdTable(table.getADQLName()) == null)
			throw new DBException("Forbidden index creation: " + table + " is not a standard table of TAP_SCHEMA!");

		// Build the fully qualified DB name of the table: 
		final String dbTableName = translator.getQualifiedTableName(table);

		// Build the name prefix of all the indexes to create: 
		final String indexNamePrefix = "INDEX_" + ((table.getADQLSchemaName() != null) ? (table.getADQLSchemaName() + "_") : "") + table.getADQLName() + "_";

		Iterator<TAPColumn> it = table.getColumns();
		while(it.hasNext()){
			TAPColumn col = it.next();
			// Create an index only for columns that have the 'indexed' flag:
			if (col.isIndexed() && !isPartOfPrimaryKey(col.getADQLName()))
				stmt.executeUpdate("CREATE INDEX " + indexNamePrefix + col.getADQLName() + " ON " + dbTableName + "(" + translator.getColumnName(col) + ");");
		}
	}

	/**
	 * Tell whether the specified column is part of the primary key of its table.
	 * 
	 * @param adqlName	ADQL name of a column.
	 * 
	 * @return	<i>true</i> if the specified column is part of the primary key,
	 *          <i>false</i> otherwise.
	 */
	private boolean isPartOfPrimaryKey(final String adqlName){
		if (adqlName == null)
			return false;
		else
			return (adqlName.equalsIgnoreCase("schema_name") || adqlName.equalsIgnoreCase("table_name") || adqlName.equalsIgnoreCase("column_name") || adqlName.equalsIgnoreCase("key_id"));
	}

	/**
	 * <p>Fill all the standard tables of TAP_SCHEMA (schemas, tables, columns, keys and key_columns).</p>
	 * 
	 * <p>This function just call the following functions:</p>
	 * <ol>
	 * 	<li>{@link #fillSchemas(TAPTable, Iterator)}</li>
	 * 	<li>{@link #fillTables(TAPTable, Iterator)}</li>
	 * 	<li>{@link #fillColumns(TAPTable, Iterator)}</li>
	 * 	<li>{@link #fillKeys(TAPTable, TAPTable, Iterator)}</li>
	 * </ol>
	 * 
	 * @param meta	All schemas and tables to list inside the TAP_SCHEMA tables.
	 * 
	 * @throws DBException	If rows can not be inserted because the SQL update query has failed.
	 * @throws SQLException	If any other SQL exception occurs.
	 */
	protected void fillTAPSchema(final TAPMetadata meta) throws SQLException, DBException{
		TAPTable metaTable;

		// 1. Fill SCHEMAS:
		metaTable = meta.getTable(STDSchema.TAPSCHEMA.label, STDTable.SCHEMAS.label);
		Iterator<TAPTable> allTables = fillSchemas(metaTable, meta.iterator());

		// 2. Fill TABLES:
		metaTable = meta.getTable(STDSchema.TAPSCHEMA.label, STDTable.TABLES.label);
		Iterator<TAPColumn> allColumns = fillTables(metaTable, allTables);
		allTables = null;

		// Fill COLUMNS:
		metaTable = meta.getTable(STDSchema.TAPSCHEMA.label, STDTable.COLUMNS.label);
		Iterator<TAPForeignKey> allKeys = fillColumns(metaTable, allColumns);
		allColumns = null;

		// Fill KEYS and KEY_COLUMNS:
		metaTable = meta.getTable(STDSchema.TAPSCHEMA.label, STDTable.KEYS.label);
		TAPTable metaTable2 = meta.getTable(STDSchema.TAPSCHEMA.label, STDTable.KEY_COLUMNS.label);
		fillKeys(metaTable, metaTable2, allKeys);
	}

	/**
	 * <p>Fill the standard table TAP_SCHEMA.schemas with the list of all published schemas.</p>
	 * 
	 * <p><i>Note:
	 * 	Batch updates may be done here if its supported by the DBMS connection.
	 * 	In case of any failure while using this feature, it will be flagged as unsupported and one-by-one updates will be processed.
	 * </i></p>
	 * 
	 * @param metaTable	Description of TAP_SCHEMA.schemas.
	 * @param itSchemas	Iterator over the list of schemas.
	 * 
	 * @return	Iterator over the full list of all tables (whatever is their schema).
	 * 
	 * @throws DBException	If rows can not be inserted because the SQL update query has failed.
	 * @throws SQLException	If any other SQL exception occurs.
	 */
	private Iterator<TAPTable> fillSchemas(final TAPTable metaTable, final Iterator<TAPSchema> itSchemas) throws SQLException, DBException{
		List<TAPTable> allTables = new ArrayList<TAPTable>();

		// Build the SQL update query:
		StringBuffer sql = new StringBuffer("INSERT INTO ");
		sql.append(translator.getQualifiedTableName(metaTable)).append(" (");
		sql.append(translator.getColumnName(metaTable.getColumn("schema_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("description")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("utype")));
		sql.append(") VALUES (?, ?, ?);");

		// Prepare the statement:
		PreparedStatement stmt = null;
		try{
			stmt = connection.prepareStatement(sql.toString());

			// Execute the query for each schema:
			int nbRows = 0;
			while(itSchemas.hasNext()){
				TAPSchema schema = itSchemas.next();
				nbRows++;

				// list all tables of this schema:
				appendAllInto(allTables, schema.iterator());

				// add the schema entry into the DB:
				stmt.setString(1, schema.getADQLName());
				stmt.setString(2, schema.getDescription());
				stmt.setString(3, schema.getUtype());
				executeUpdate(stmt, nbRows);
			}
			executeBatchUpdates(stmt, nbRows);
		}finally{
			close(stmt);
		}

		return allTables.iterator();
	}

	/**
	 * <p>Fill the standard table TAP_SCHEMA.tables with the list of all published tables.</p>
	 * 
	 * <p><i>Note:
	 * 	Batch updates may be done here if its supported by the DBMS connection.
	 * 	In case of any failure while using this feature, it will be flagged as unsupported and one-by-one updates will be processed.
	 * </i></p>
	 * 
	 * @param metaTable	Description of TAP_SCHEMA.tables.
	 * @param itTables	Iterator over the list of tables.
	 * 
	 * @return	Iterator over the full list of all columns (whatever is their table).
	 * 
	 * @throws DBException	If rows can not be inserted because the SQL update query has failed.
	 * @throws SQLException	If any other SQL exception occurs.
	 */
	private Iterator<TAPColumn> fillTables(final TAPTable metaTable, final Iterator<TAPTable> itTables) throws SQLException, DBException{
		List<TAPColumn> allColumns = new ArrayList<TAPColumn>();

		// Build the SQL update query:
		StringBuffer sql = new StringBuffer("INSERT INTO ");
		sql.append(translator.getQualifiedTableName(metaTable)).append(" (");
		sql.append(translator.getColumnName(metaTable.getColumn("schema_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("table_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("table_type")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("description")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("utype")));
		sql.append(") VALUES (?, ?, ?, ?, ?);");

		// Prepare the statement:
		PreparedStatement stmt = null;
		try{
			stmt = connection.prepareStatement(sql.toString());

			// Execute the query for each table:
			int nbRows = 0;
			while(itTables.hasNext()){
				TAPTable table = itTables.next();
				nbRows++;

				// list all columns of this table:
				appendAllInto(allColumns, table.getColumns());

				// add the table entry into the DB:
				stmt.setString(1, table.getADQLSchemaName());
				stmt.setString(2, table.getADQLName());
				stmt.setString(3, table.getType().toString());
				stmt.setString(4, table.getDescription());
				stmt.setString(5, table.getUtype());
				executeUpdate(stmt, nbRows);
			}
			executeBatchUpdates(stmt, nbRows);
		}finally{
			close(stmt);
		}

		return allColumns.iterator();
	}

	/**
	 * <p>Fill the standard table TAP_SCHEMA.columns with the list of all published columns.</p>
	 * 
	 * <p><i>Note:
	 * 	Batch updates may be done here if its supported by the DBMS connection.
	 * 	In case of any failure while using this feature, it will be flagged as unsupported and one-by-one updates will be processed.
	 * </i></p>
	 * 
	 * @param metaTable	Description of TAP_SCHEMA.columns.
	 * @param itColumns	Iterator over the list of columns.
	 * 
	 * @return	Iterator over the full list of all foreign keys.
	 * 
	 * @throws DBException	If rows can not be inserted because the SQL update query has failed.
	 * @throws SQLException	If any other SQL exception occurs.
	 */
	private Iterator<TAPForeignKey> fillColumns(final TAPTable metaTable, final Iterator<TAPColumn> itColumns) throws SQLException, DBException{
		List<TAPForeignKey> allKeys = new ArrayList<TAPForeignKey>();

		// Build the SQL update query:
		StringBuffer sql = new StringBuffer("INSERT INTO ");
		sql.append(translator.getQualifiedTableName(metaTable)).append(" (");
		sql.append(translator.getColumnName(metaTable.getColumn("table_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("column_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("description")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("unit")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("ucd")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("utype")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("datatype")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("size")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("principal")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("indexed")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("std")));
		sql.append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

		// Prepare the statement:
		PreparedStatement stmt = null;
		try{
			stmt = connection.prepareStatement(sql.toString());

			// Execute the query for each column:
			int nbRows = 0;
			while(itColumns.hasNext()){
				TAPColumn col = itColumns.next();
				nbRows++;

				// list all foreign keys of this column:
				appendAllInto(allKeys, col.getTargets());

				// add the column entry into the DB:
				stmt.setString(1, col.getTable().getADQLName());
				stmt.setString(2, col.getADQLName());
				stmt.setString(3, col.getDescription());
				stmt.setString(4, col.getUnit());
				stmt.setString(5, col.getUcd());
				stmt.setString(6, col.getUtype());
				stmt.setString(7, col.getDatatype().type.toString());
				stmt.setInt(8, col.getDatatype().length);
				stmt.setInt(9, col.isPrincipal() ? 1 : 0);
				stmt.setInt(10, col.isIndexed() ? 1 : 0);
				stmt.setInt(11, col.isStd() ? 1 : 0);
				executeUpdate(stmt, nbRows);
			}
			executeBatchUpdates(stmt, nbRows);
		}finally{
			close(stmt);
		}

		return allKeys.iterator();
	}

	/**
	 * <p>Fill the standard tables TAP_SCHEMA.keys and TAP_SCHEMA.key_columns with the list of all published foreign keys.</p>
	 * 
	 * <p><i>Note:
	 * 	Batch updates may be done here if its supported by the DBMS connection.
	 * 	In case of any failure while using this feature, it will be flagged as unsupported and one-by-one updates will be processed.
	 * </i></p>
	 * 
	 * @param metaKeys			Description of TAP_SCHEMA.keys.
	 * @param metaKeyColumns	Description of TAP_SCHEMA.key_columns.
	 * @param itKeys			Iterator over the list of foreign keys.
	 * 
	 * @throws DBException	If rows can not be inserted because the SQL update query has failed.
	 * @throws SQLException	If any other SQL exception occurs.
	 */
	private void fillKeys(final TAPTable metaKeys, final TAPTable metaKeyColumns, final Iterator<TAPForeignKey> itKeys) throws SQLException, DBException{
		// Build the SQL update query for KEYS:
		StringBuffer sqlKeys = new StringBuffer("INSERT INTO ");
		sqlKeys.append(translator.getQualifiedTableName(metaKeys)).append(" (");
		sqlKeys.append(translator.getColumnName(metaKeys.getColumn("key_id")));
		sqlKeys.append(", ").append(translator.getColumnName(metaKeys.getColumn("from_table")));
		sqlKeys.append(", ").append(translator.getColumnName(metaKeys.getColumn("target_table")));
		sqlKeys.append(", ").append(translator.getColumnName(metaKeys.getColumn("description")));
		sqlKeys.append(", ").append(translator.getColumnName(metaKeys.getColumn("utype")));
		sqlKeys.append(") VALUES (?, ?, ?, ?, ?);");

		PreparedStatement stmtKeys = null, stmtKeyCols = null;
		try{
			// Prepare the statement for KEYS:
			stmtKeys = connection.prepareStatement(sqlKeys.toString());

			// Build the SQL update query for KEY_COLUMNS:
			StringBuffer sqlKeyCols = new StringBuffer("INSERT INTO ");
			sqlKeyCols.append(translator.getQualifiedTableName(metaKeyColumns)).append(" (");
			sqlKeyCols.append(translator.getColumnName(metaKeyColumns.getColumn("key_id")));
			sqlKeyCols.append(", ").append(translator.getColumnName(metaKeyColumns.getColumn("from_column")));
			sqlKeyCols.append(", ").append(translator.getColumnName(metaKeyColumns.getColumn("target_column")));
			sqlKeyCols.append(") VALUES (?, ?, ?);");

			// Prepare the statement for KEY_COLUMNS:
			stmtKeyCols = connection.prepareStatement(sqlKeyCols.toString());

			// Execute the query for each column:
			int nbKeys = 0, nbKeyColumns = 0;
			while(itKeys.hasNext()){
				TAPForeignKey key = itKeys.next();
				nbKeys++;

				// add the key entry into KEYS:
				stmtKeys.setString(1, key.getKeyId());
				stmtKeys.setString(2, key.getFromTable().getFullName());
				stmtKeys.setString(3, key.getTargetTable().getFullName());
				stmtKeys.setString(4, key.getDescription());
				stmtKeys.setString(5, key.getUtype());
				executeUpdate(stmtKeys, nbKeys);

				// add the key columns into KEY_COLUMNS:
				Iterator<Map.Entry<String,String>> itAssoc = key.iterator();
				while(itAssoc.hasNext()){
					nbKeyColumns++;
					Map.Entry<String,String> assoc = itAssoc.next();
					stmtKeyCols.setString(1, key.getKeyId());
					stmtKeyCols.setString(2, assoc.getKey());
					stmtKeyCols.setString(3, assoc.getValue());
					executeUpdate(stmtKeyCols, nbKeyColumns);
				}
			}

			executeBatchUpdates(stmtKeys, nbKeys);
			executeBatchUpdates(stmtKeyCols, nbKeyColumns);
		}finally{
			close(stmtKeys);
			close(stmtKeyCols);
		}
	}

	/* ***************** */
	/* UPLOAD MANAGEMENT */
	/* ***************** */

	/**
	 * <p><i><b>Important note:</b>
	 * 	Only tables uploaded by users can be created in the database. To ensure that, the schema name of this table MUST be {@link STDSchema#UPLOADSCHEMA} ("TAP_UPLOAD") in ADQL.
	 * 	If it has another ADQL name, an exception will be thrown. Of course, the DB name of this schema MAY be different.
	 * </i></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function may modify the given {@link TAPTable} object if schemas are not supported by this connection.
	 * 	In this case, this function will prefix the table's DB name by the schema's DB name directly inside the given {@link TAPTable} object
	 * 	(building the prefix with {@link #getTablePrefix(String)}). Then the DB name of the schema will be set to NULL.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	If the upload schema does not already exist in the database, it will be created.
	 * </i></p>
	 * 
	 * @see tap.db.DBConnection#addUploadedTable(tap.metadata.TAPTable, tap.data.TableIterator)
	 * @see #checkUploadedTableDef(TAPTable)
	 */
	@Override
	public boolean addUploadedTable(TAPTable tableDef, TableIterator data) throws DBException, DataReadException{
		// If no table to upload, consider it has been dropped and return TRUE:
		if (tableDef == null)
			return true;

		// Check the table is well defined (and particularly the schema is well set with an ADQL name = TAP_UPLOAD):
		checkUploadedTableDef(tableDef);

		Statement stmt = null;
		try{

			// Start a transaction:
			startTransaction();
			// ...create a statement:
			stmt = connection.createStatement();

			DatabaseMetaData dbMeta = connection.getMetaData();

			// 1. Create the upload schema, if it does not already exist:
			if (!isSchemaExisting(tableDef.getDBSchemaName(), dbMeta)){
				stmt.executeUpdate("CREATE SCHEMA " + translator.getQualifiedSchemaName(tableDef) + ";");
				logger.logDB(LogLevel.INFO, this, "SCHEMA_CREATED", "Schema \"" + tableDef.getADQLSchemaName() + "\" (in DB: " + translator.getQualifiedSchemaName(tableDef) + ") created.", null);
			}
			// 1bis. Ensure the table does not already exist and if it is the case, throw an understandable exception:
			else if (isTableExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), dbMeta)){
				DBException de = new DBException("Impossible to create the user uploaded table in the database: " + translator.getQualifiedTableName(tableDef) + "! This table already exists.");
				logger.logDB(LogLevel.ERROR, this, "ADD_UPLOAD_TABLE", de.getMessage(), de);
				throw de;
			}

			// 2. Create the table:
			// ...build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("CREATE TABLE ");
			sqlBuf.append(translator.getQualifiedTableName(tableDef)).append(" (");
			Iterator<TAPColumn> it = tableDef.getColumns();
			while(it.hasNext()){
				TAPColumn col = it.next();
				// column name:
				sqlBuf.append(translator.getColumnName(col));
				// column type:
				sqlBuf.append(' ').append(getDBMSDatatype(col.getDatatype()));
				// last column ?
				if (it.hasNext())
					sqlBuf.append(',');
			}
			sqlBuf.append(");");
			// ...execute the update query:
			stmt.executeUpdate(sqlBuf.toString());

			// 3. Fill the table:
			fillUploadedTable(tableDef, data);

			// Commit the transaction:
			commit();

			// Log the end:
			logger.logDB(LogLevel.INFO, this, "TABLE_CREATED", "Table \"" + tableDef.getADQLName() + "\" (in DB: " + translator.getQualifiedTableName(tableDef) + ") created.", null);

			return true;

		}catch(SQLException se){
			rollback();
			logger.logDB(LogLevel.WARNING, this, "ADD_UPLOAD_TABLE", "Impossible to create the uploaded table: " + translator.getQualifiedTableName(tableDef) + "!", se);
			throw new DBException("Impossible to create the uploaded table: " + translator.getQualifiedTableName(tableDef) + "!", se);
		}catch(DBException de){
			rollback();
			throw de;
		}catch(DataReadException dre){
			rollback();
			throw dre;
		}finally{
			close(stmt);
			endTransaction();
		}
	}

	/**
	 * <p>Fill the table uploaded by the user with the given data.</p>
	 * 
	 * <p><i>Note:
	 * 	Batch updates may be done here if its supported by the DBMS connection.
	 * 	In case of any failure while using this feature, it will be flagged as unsupported and one-by-one updates will be processed.
	 * </i></p>
	 * 
	 * @param metaTable	Description of the updated table.
	 * @param data		Iterator over the rows to insert.
	 * 
	 * @return	Number of inserted rows.
	 * 
	 * @throws DBException			If rows can not be inserted because the SQL update query has failed.
	 * @throws SQLException			If any other SQL exception occurs.
	 * @throws DataReadException	If there is any error while reading the data from the given {@link TableIterator} (and particularly if a limit - in byte or row - has been reached).
	 */
	protected int fillUploadedTable(final TAPTable metaTable, final TableIterator data) throws SQLException, DBException, DataReadException{
		// 1. Build the SQL update query:
		StringBuffer sql = new StringBuffer("INSERT INTO ");
		StringBuffer varParam = new StringBuffer();
		// ...table name:
		sql.append(translator.getQualifiedTableName(metaTable)).append(" (");
		// ...list of columns:
		TAPColumn[] cols = data.getMetadata();
		for(int c = 0; c < cols.length; c++){
			if (c > 0){
				sql.append(", ");
				varParam.append(", ");
			}
			sql.append(translator.getColumnName(cols[c]));
			varParam.append('?');
		}
		// ...values pattern:
		sql.append(") VALUES (").append(varParam).append(");");

		// 2. Prepare the statement:
		PreparedStatement stmt = null;
		int nbRows = 0;
		try{
			stmt = connection.prepareStatement(sql.toString());

			// 3. Execute the query for each given row:
			while(data.nextRow()){
				nbRows++;
				int c = 1;
				while(data.hasNextCol())
					stmt.setObject(c++, data.nextCol());
				executeUpdate(stmt, nbRows);
			}
			executeBatchUpdates(stmt, nbRows);

			return nbRows;

		}finally{
			close(stmt);
		}
	}

	/**
	 * <p><i><b>Important note:</b>
	 * 	Only tables uploaded by users can be dropped from the database. To ensure that, the schema name of this table MUST be {@link STDSchema#UPLOADSCHEMA} ("TAP_UPLOAD") in ADQL.
	 * 	If it has another ADQL name, an exception will be thrown. Of course, the DB name of this schema MAY be different.
	 * </i></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function may modify the given {@link TAPTable} object if schemas are not supported by this connection.
	 * 	In this case, this function will prefix the table's DB name by the schema's DB name directly inside the given {@link TAPTable} object
	 * 	(building the prefix with {@link #getTablePrefix(String)}). Then the DB name of the schema will be set to NULL.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This implementation is able to drop only one uploaded table. So if this function finds more than one table matching to the given one,
	 * 	an exception will be thrown and no table will be dropped.
	 * </i></p>
	 * 
	 * @see tap.db.DBConnection#dropUploadedTable(tap.metadata.TAPTable)
	 * @see #checkUploadedTableDef(TAPTable)
	 */
	@Override
	public boolean dropUploadedTable(final TAPTable tableDef) throws DBException{
		// If no table to upload, consider it has been dropped and return TRUE:
		if (tableDef == null)
			return true;

		// Check the table is well defined (and particularly the schema is well set with an ADQL name = TAP_UPLOAD):
		checkUploadedTableDef(tableDef);

		Statement stmt = null;
		try{

			// Check the existence of the table to drop:
			if (!isTableExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), connection.getMetaData()))
				return true;

			// Execute the update:
			stmt = connection.createStatement();
			int cnt = stmt.executeUpdate("DROP TABLE " + translator.getQualifiedTableName(tableDef) + ";");

			// Log the end:
			if (cnt == 0)
				logger.logDB(LogLevel.INFO, this, "TABLE_DROPPED", "Table \"" + tableDef.getADQLName() + "\" (in DB: " + translator.getQualifiedTableName(tableDef) + ") dropped.", null);
			else
				logger.logDB(LogLevel.ERROR, this, "TABLE_DROPPED", "Table \"" + tableDef.getADQLName() + "\" (in DB: " + translator.getQualifiedTableName(tableDef) + ") NOT dropped.", null);

			// Ensure the update is successful:
			return (cnt == 0);

		}catch(SQLException se){
			logger.logDB(LogLevel.WARNING, this, "DROP_UPLOAD_TABLE", "Impossible to drop the uploaded table: " + translator.getQualifiedTableName(tableDef) + "!", se);
			throw new DBException("Impossible to drop the uploaded table: " + translator.getQualifiedTableName(tableDef) + "!", se);
		}finally{
			close(stmt);
		}
	}

	/**
	 * <p>Ensures that the given table MUST be inside the upload schema in ADQL.</p>
	 * 
	 * <p>Thus, the following cases are taken into account:</p>
	 * <ul>
	 * 	<li>
	 * 		The schema name of the given table MUST be {@link STDSchema#UPLOADSCHEMA} ("TAP_UPLOAD") in ADQL.
	 * 		If it has another ADQL name, an exception will be thrown. Of course, the DB name of this schema MAY be different.
	 * 	</li>
	 * 	<li>
	 * 		If schemas are not supported by this connection, this function will prefix the table DB name by the schema DB name directly
	 * 		inside the given {@link TAPTable} object (building the prefix with {@link #getTablePrefix(String)}). Then the DB name
	 * 		of the schema will be set to NULL.
	 * 	</li>
	 * </ul>
	 * 
	 * @param tableDef	Definition of the table to create/drop.
	 * 
	 * @throws DBException	If the given table is not in a schema
	 *                    	or if the ADQL name of this schema is not {@link STDSchema#UPLOADSCHEMA} ("TAP_UPLOAD").
	 */
	protected void checkUploadedTableDef(final TAPTable tableDef) throws DBException{
		// If the table has no defined schema or if the ADQL name of the schema is not TAP_UPLOAD, throw an exception:
		if (tableDef.getSchema() == null || !tableDef.getSchema().getADQLName().equals(STDSchema.UPLOADSCHEMA.label))
			throw new DBException("Missing upload schema! An uploaded table must be inside a schema whose the ADQL name is strictly equals to \"" + STDSchema.UPLOADSCHEMA.label + "\" (but the DB name may be different).");

		// If schemas are not supported, prefix the table name and set to NULL the DB schema name:
		if (!supportsSchema && tableDef.getDBSchemaName() != null){
			tableDef.setDBName(getTablePrefix(tableDef.getDBSchemaName()) + tableDef.getDBName());
			tableDef.getSchema().setDBName(null);
		}
	}

	/* ************** */
	/* TOOL FUNCTIONS */
	/* ************** */

	/**
	 * <p>Get the DBMS compatible datatype corresponding to the given column TAPType.</p>
	 * 
	 * <p><i>Note 1:
	 * 	This function is able to generate a DB datatype compatible with the currently used DBMS.
	 * 	In this current implementation, only Postgresql, Oracle, SQLite, MySQL and Java DB/Derby have been considered.
	 * 	Most of the TAP types have been tested only with Postgresql and SQLite without any problem.
	 * 	If the DBMS you are using has not been considered, note that this function will return the TAP type expression by default.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	In case the given datatype is NULL or not managed here, the DBMS type corresponding to "VARCHAR" will be returned.
	 * </i></p>
	 * 
	 * <p><i>Note 3:
	 * 	The special TAP types POINT and REGION are converted into the DBMS type corresponding to "VARCHAR".
	 * </i></p>
	 * 
	 * @param datatype	Column TAP type.
	 * 
	 * @return	The corresponding DB type, or NULL if the given type is not managed or is NULL.
	 */
	protected String getDBMSDatatype(TAPType datatype){
		if (datatype == null)
			datatype = new TAPType(TAPDatatype.VARCHAR);

		switch(datatype.type){

			case SMALLINT:
				return dbms.equals("sqlite") ? "INTEGER" : "SMALLINT";

			case INTEGER:
			case REAL:
				return datatype.type.toString();

			case BIGINT:
				if (dbms.equals("oracle"))
					return "NUMBER(19,0)";
				else if (dbms.equals("sqlite"))
					return "INTEGER";
				else
					return "BIGINT";

			case DOUBLE:
				if (dbms.equals("postgresql") || dbms.equals("oracle"))
					return "DOUBLE PRECISION";
				else if (dbms.equals("sqlite"))
					return "REAL";
				else
					return "DOUBLE";

			case BINARY:
				if (dbms.equals("postgresql"))
					return "bytea";
				else if (dbms.equals("sqlite"))
					return "BLOB";
				else if (dbms.equals("oracle"))
					return "RAW" + (datatype.length > 0 ? "(" + datatype.length + ")" : "");
				else if (dbms.equals("derby"))
					return "CHAR" + (datatype.length > 0 ? "(" + datatype.length + ")" : "") + " FOR BIT DATA";
				else
					return datatype.type.toString();

			case VARBINARY:
				if (dbms.equals("postgresql"))
					return "bytea";
				else if (dbms.equals("sqlite"))
					return "BLOB";
				else if (dbms.equals("oracle"))
					return "LONG RAW" + (datatype.length > 0 ? "(" + datatype.length + ")" : "");
				else if (dbms.equals("derby"))
					return "VARCHAR" + (datatype.length > 0 ? "(" + datatype.length + ")" : "") + " FOR BIT DATA";
				else
					return datatype.type.toString();

			case CHAR:
				if (dbms.equals("sqlite"))
					return "TEXT";
				else
					return "CHAR";

			case BLOB:
				if (dbms.equals("postgresql"))
					return "bytea";
				else
					return "BLOB";

			case CLOB:
				if (dbms.equals("postgresql") || dbms.equals("mysql") || dbms.equals("sqlite"))
					return "TEXT";
				else
					return "CLOB";

			case TIMESTAMP:
				if (dbms.equals("sqlite"))
					return "TEXT";
				else
					return "TIMESTAMP";

			case POINT:
			case REGION:
			case VARCHAR:
			default:
				if (dbms.equals("sqlite"))
					return "TEXT";
				else
					return "VARCHAR";
		}
	}

	/**
	 * <p>Start a transaction.</p>
	 * 
	 * <p>
	 * 	Basically, if transactions are supported by this connection, the flag AutoCommit is just turned off.
	 * 	It will be turned on again when {@link #endTransaction()} is called.
	 * </p>
	 * 
	 * <p>If transactions are not supported by this connection, nothing is done.</p>
	 * 
	 * <p><b><i>Important note:</b>
	 * 	If any error interrupts the START TRANSACTION operation, transactions will be afterwards considered as not supported by this connection.
	 * 	So, subsequent call to this function (and any other transaction related function) will never do anything.
	 * </i></p>
	 * 
	 * @throws DBException	If it is impossible to start a transaction though transactions are supported by this connection.
	 *                    	If these are not supported, this error can never be thrown.
	 */
	protected void startTransaction() throws DBException{
		try{
			if (supportsTransaction){
				connection.setAutoCommit(false);
				logger.logDB(LogLevel.INFO, this, "START_TRANSACTION", "Transaction STARTED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			logger.logDB(LogLevel.ERROR, this, "START_TRANSACTION", "Transaction STARTing impossible!", se);
			throw new DBException("Transaction STARTing impossible!", se);
		}
	}

	/**
	 * <p>Commit the current transaction.</p>
	 * 
	 * <p>
	 * 	{@link #startTransaction()} must have been called before. If it's not the case the connection
	 * 	may throw a {@link SQLException} which will be transformed into a {@link DBException} here.
	 * </p>
	 * 
	 * <p>If transactions are not supported by this connection, nothing is done.</p>
	 * 
	 * <p><b><i>Important note:</b>
	 * 	If any error interrupts the COMMIT operation, transactions will be afterwards considered as not supported by this connection.
	 * 	So, subsequent call to this function (and any other transaction related function) will never do anything.
	 * </i></p>
	 * 
	 * @throws DBException	If it is impossible to commit a transaction though transactions are supported by this connection..
	 *                    	If these are not supported, this error can never be thrown.
	 */
	protected void commit() throws DBException{
		try{
			if (supportsTransaction){
				connection.commit();
				logger.logDB(LogLevel.INFO, this, "COMMIT", "Transaction COMMITED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			logger.logDB(LogLevel.ERROR, this, "COMMIT", "Transaction COMMIT impossible!", se);
			throw new DBException("Transaction COMMIT impossible!", se);
		}
	}

	/**
	 * <p>Rollback the current transaction.</p>
	 * 
	 * <p>
	 * 	{@link #startTransaction()} must have been called before. If it's not the case the connection
	 * 	may throw a {@link SQLException} which will be transformed into a {@link DBException} here.
	 * </p>
	 * 
	 * <p>If transactions are not supported by this connection, nothing is done.</p>
	 * 
	 * <p><b><i>Important note:</b>
	 * 	If any error interrupts the ROLLBACK operation, transactions will considered afterwards as not supported by this connection.
	 * 	So, subsequent call to this function (and any other transaction related function) will never do anything.
	 * </i></p>
	 * 
	 * @throws DBException	If it is impossible to rollback a transaction though transactions are supported by this connection..
	 *                    	If these are not supported, this error can never be thrown.
	 */
	protected void rollback(){
		try{
			if (supportsTransaction){
				connection.rollback();
				logger.logDB(LogLevel.INFO, this, "ROLLBACK", "Transaction ROLLBACKED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			logger.logDB(LogLevel.ERROR, this, "ROLLBACK", "Transaction ROLLBACK impossible!", se);
		}
	}

	/**
	 * <p>End the current transaction.</p>
	 * 
	 * <p>
	 * 	Basically, if transactions are supported by this connection, the flag AutoCommit is just turned on.
	 * </p>
	 * 
	 * <p>If transactions are not supported by this connection, nothing is done.</p>
	 * 
	 * <p><b><i>Important note:</b>
	 * 	If any error interrupts the END TRANSACTION operation, transactions will be afterwards considered as not supported by this connection.
	 * 	So, subsequent call to this function (and any other transaction related function) will never do anything.
	 * </i></p>
	 * 
	 * @throws DBException	If it is impossible to end a transaction though transactions are supported by this connection.
	 *                    	If these are not supported, this error can never be thrown.
	 */
	protected void endTransaction(){
		try{
			if (supportsTransaction){
				connection.setAutoCommit(true);
				logger.logDB(LogLevel.INFO, this, "END_TRANSACTION", "Transaction ENDED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			logger.logDB(LogLevel.ERROR, this, "END_TRANSACTION", "Transaction ENDing impossible!", se);
		}
	}

	/**
	 * <p>Close silently the given {@link ResultSet}.</p>
	 * 
	 * <p>If the given {@link ResultSet} is NULL, nothing (even exception/error) happens.</p>
	 * 
	 * <p>
	 * 	If any {@link SQLException} occurs during this operation, it is caught and just logged (see {@link #log(int, String, Exception)}).
	 * 	No error is thrown and nothing else is done.
	 * </p>
	 * 
	 * @param rs	{@link ResultSet} to close.
	 */
	protected final void close(final ResultSet rs){
		try{
			if (rs != null)
				rs.close();
		}catch(SQLException se){
			logger.logDB(LogLevel.WARNING, this, "CLOSE", "Can not close a ResultSet!", null);
		}
	}

	/**
	 * <p>Close silently the given {@link Statement}.</p>
	 * 
	 * <p>If the given {@link Statement} is NULL, nothing (even exception/error) happens.</p>
	 * 
	 * <p>
	 * 	If any {@link SQLException} occurs during this operation, it is caught and just logged (see {@link #log(int, String, Exception)}).
	 * 	No error is thrown and nothing else is done.
	 * </p>
	 * 
	 * @param rs	{@link Statement} to close.
	 */
	protected final void close(final Statement stmt){
		try{
			if (stmt != null)
				stmt.close();
		}catch(SQLException se){
			logger.logDB(LogLevel.WARNING, this, "CLOSE", "Can not close a Statement!", null);
		}
	}

	/**
	 * <p>Transform the given column value in a boolean value.</p>
	 * 
	 * <p>The following cases are taken into account in function of the given value's type:</p>
	 * <ul>
	 * 	<li><b>NULL</b>: <i>false</i> is always returned.</li>
	 * 
	 * 	<li><b>{@link Boolean}</b>: the boolean value is returned as provided (but casted in boolean).</li>
	 * 
	 * 	<li><b>{@link Integer}</b>: <i>true</i> is returned only if the integer value is strictly greater than 0, otherwise <i>false</i> is returned.</li>
	 * 
	 * 	<li><b>Other</b>: toString().trim() is first called on this object. Then, an integer value is tried to be extracted from it.
	 *                    If it succeeds, the previous rule is applied. If it fails, <i>true</i> will be returned only if the string is "t" or "true" (case insensitively).</li>
	 * </ul>
	 * 
	 * @param colValue	The column value to transform in boolean.
	 * 
	 * @return	Its corresponding boolean value.
	 */
	protected final boolean toBoolean(final Object colValue){
		// NULL => false:
		if (colValue == null)
			return false;

		// Boolean value => cast in boolean and return this value:
		else if (colValue instanceof Boolean)
			return ((Boolean)colValue).booleanValue();

		// Integer value => cast in integer and return true only if the value is positive and not null:
		else if (colValue instanceof Integer){
			int intFlag = ((Integer)colValue).intValue();
			return (intFlag > 0);
		}
		// Otherwise => get the string representation and:
		//     1/ try to cast it into an integer and apply the same test as before
		//     2/ if the cast fails, return true only if the value is "t" or "true" (case insensitively):
		else{
			String strFlag = colValue.toString().trim();
			try{
				int intFlag = Integer.parseInt(strFlag);
				return (intFlag > 0);
			}catch(NumberFormatException nfe){
				return strFlag.equalsIgnoreCase("t") || strFlag.equalsIgnoreCase("true");
			}
		}
	}

	/**
	 * Return NULL if the given column value is an empty string (or it just contains space characters) or NULL.
	 * Otherwise the given given is returned as provided.
	 * 
	 * @param dbValue	Value to nullify if needed.
	 * 
	 * @return	NULL if the given string is NULL or empty, otherwise the given value.
	 */
	protected final String nullifyIfNeeded(final String dbValue){
		return (dbValue != null && dbValue.trim().length() <= 0) ? null : dbValue;
	}

	/**
	 * Search a {@link TAPTable} instance whose the ADQL name matches (case sensitively) to the given one.
	 * 
	 * @param tableName	ADQL name of the table to search.
	 * @param itTables	Iterator over the set of tables in which the research must be done.
	 * 
	 * @return	The found table, or NULL if not found.
	 */
	private TAPTable searchTable(String tableName, final Iterator<TAPTable> itTables){
		// Get the schema name, if any prefix the given table name:
		String schemaName = null;
		int indSep = tableName.indexOf('.');
		if (indSep > 0){
			schemaName = tableName.substring(0, indSep);
			tableName = tableName.substring(indSep + 1);
		}

		// Search by schema name (if any) and then by table name:
		while(itTables.hasNext()){
			// get the table:
			TAPTable table = itTables.next();
			// test the schema name (if one was prefixing the table name) (case sensitively):
			if (schemaName != null){
				if (table.getADQLSchemaName() == null || !schemaName.equals(table.getADQLSchemaName()))
					continue;
			}
			// test the table name (case sensitively):
			if (tableName.equals(table.getADQLName()))
				return table;
		}

		// NULL if no table matches:
		return null;
	}

	/**
	 * <p>Tell whether the specified schema exists in the database.
	 * 	To do so, it is using the given {@link DatabaseMetaData} object to query the database and list all existing schemas.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is completely useless if the connection is not supporting schemas.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	Test on the schema name is done considering the case sensitivity indicated by the translator
	 * 	(see {@link ADQLTranslator#isCaseSensitive(IdentifierField)}).
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This functions is used by {@link #addUploadedTable(TAPTable, TableIterator)} and {@link #resetTAPSchema(Statement, TAPTable[])}.
	 * </i></p>
	 * 
	 * @param schemaName	DB name of the schema whose the existence must be checked.
	 * @param dbMeta		Metadata about the database, and mainly the list of all existing schemas.
	 * 
	 * @return	<i>true</i> if the specified schema exists, <i>false</i> otherwise.
	 * 
	 * @throws SQLException	If any error occurs while interrogating the database about existing schema.
	 */
	protected boolean isSchemaExisting(String schemaName, final DatabaseMetaData dbMeta) throws SQLException{
		if (schemaName == null || schemaName.length() == 0)
			return true;

		// Determine the case sensitivity to use for the equality test:
		boolean caseSensitive = translator.isCaseSensitive(IdentifierField.SCHEMA);

		ResultSet rs = null;
		try{
			// List all schemas available and stop when a schema name matches ignoring the case:
			rs = dbMeta.getSchemas();
			boolean hasSchema = false;
			while(!hasSchema && rs.next())
				hasSchema = equals(rs.getString(1), schemaName, caseSensitive);
			return hasSchema;
		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Tell whether the specified table exists in the database.
	 * 	To do so, it is using the given {@link DatabaseMetaData} object to query the database and list all existing tables.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If schemas are not supported by this connection but a schema name is even though provided in parameter,
	 * 	the table name will be prefixed by the schema name using {@link #getTablePrefix(String)}.
	 * 	The research will then be done with NULL as schema name and this prefixed table name.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	Test on the schema name is done considering the case sensitivity indicated by the translator
	 * 	(see {@link ADQLTranslator#isCaseSensitive(IdentifierField)}).
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This function is used by {@link #addUploadedTable(TAPTable, TableIterator)} and {@link #dropUploadedTable(TAPTable)}.
	 * </i></p>
	 * 
	 * @param schemaName	DB name of the schema in which the table to search is. <i>If NULL, the table is expected in any schema but ONLY one MUST exist.</i>
	 * @param tableName		DB name of the table to search.
	 * @param dbMeta		Metadata about the database, and mainly the list of all existing tables.
	 * 
	 * @return	<i>true</i> if the specified table exists, <i>false</i> otherwise.
	 * 
	 * @throws SQLException	If any error occurs while interrogating the database about existing tables.
	 */
	protected boolean isTableExisting(String schemaName, String tableName, final DatabaseMetaData dbMeta) throws DBException, SQLException{
		if (tableName == null || tableName.length() == 0)
			return true;

		// Determine the case sensitivity to use for the equality test:
		boolean schemaCaseSensitive = translator.isCaseSensitive(IdentifierField.SCHEMA);
		boolean tableCaseSensitive = translator.isCaseSensitive(IdentifierField.TABLE);

		ResultSet rs = null;
		try{
			// Prefix the table name by the schema name if needed (if schemas are not supported by this connection):
			if (!supportsSchema)
				tableName = getTablePrefix(schemaName) + tableName;

			// List all matching tables:
			if (supportsSchema){
				String schemaPattern = schemaCaseSensitive ? schemaName : null;
				String tablePattern = tableCaseSensitive ? tableName : null;
				rs = dbMeta.getTables(null, schemaPattern, tablePattern, null);
			}else{
				String tablePattern = tableCaseSensitive ? tableName : null;
				rs = dbMeta.getTables(null, null, tablePattern, null);
			}

			// Stop on the first table which match completely (schema name + table name in function of their respective case sensitivity):
			int cnt = 0;
			while(rs.next()){
				String rsSchema = nullifyIfNeeded(rs.getString(2));
				String rsTable = rs.getString(3);
				if (!supportsSchema || schemaName == null || equals(rsSchema, schemaName, schemaCaseSensitive)){
					if (equals(rsTable, tableName, tableCaseSensitive))
						cnt++;
				}
			}

			if (cnt > 1){
				logger.logDB(LogLevel.ERROR, this, "TABLE_EXIST", "More than one table match to these criteria (schema=" + schemaName + " (case sensitive?" + schemaCaseSensitive + ") && table=" + tableName + " (case sensitive?" + tableCaseSensitive + "))!", null);
				throw new DBException("More than one table match to these criteria (schema=" + schemaName + " (case sensitive?" + schemaCaseSensitive + ") && table=" + tableName + " (case sensitive?" + tableCaseSensitive + "))!");
			}

			return cnt == 1;

		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Build a table prefix with the given schema name.</p>
	 * 
	 * <p>By default, this function returns: schemaName + "_".</p>
	 * 
	 * <p><b>CAUTION:
	 * 	This function is used only when schemas are not supported by the DBMS connection.
	 * 	It aims to propose an alternative of the schema notion by prefixing the table name by the schema name.
	 * </b></p>
	 * 
	 * <p><i>Note:
	 * 	If the given schema is NULL or is an empty string, an empty string will be returned.
	 * 	Thus, no prefix will be set....which is very useful when the table name has already been prefixed
	 * 	(in such case, the DB name of its schema has theoretically set to NULL).
	 * </i></p>
	 * 
	 * @param schemaName	(DB) Schema name.
	 * 
	 * @return	The corresponding table prefix, or "" if the given schema name is an empty string or NULL.
	 */
	protected String getTablePrefix(final String schemaName){
		if (schemaName != null && schemaName.trim().length() > 0)
			return schemaName + "_";
		else
			return "";
	}

	/**
	 * Tell whether the specified table (using its DB name only) is a standard one or not.
	 * 
	 * @param dbTableName	DB (unqualified) table name.
	 * @param stdTables		List of all tables to consider as the standard ones.
	 * @param caseSensitive	Indicate whether the equality test must be done case sensitively or not.
	 * 
	 * @return	The corresponding {@link STDTable} if the specified table is a standard one,
	 *        	NULL otherwise.
	 * 
	 * @see TAPMetadata#resolveStdTable(String)
	 */
	protected final STDTable isStdTable(final String dbTableName, final TAPTable[] stdTables, final boolean caseSensitive){
		if (dbTableName != null){
			for(TAPTable t : stdTables){
				if (equals(dbTableName, t.getDBName(), caseSensitive))
					return TAPMetadata.resolveStdTable(t.getADQLName());
			}
		}
		return null;
	}

	/**
	 * <p>"Execute" the query update. <i>This update must concern ONLY ONE ROW.</i></p>
	 * 
	 * <p>
	 * 	Note that the "execute" action will be different in function of whether batch update queries are supported or not by this connection:
	 * </p>
	 * <ul>
	 * 	<li>
	 * 		If <b>batch update queries are supported</b>, just {@link PreparedStatement#addBatch()} will be called.
	 * 		It means, the query will be appended in a list and will be executed only if
	 * 		{@link #executeBatchUpdates(PreparedStatement, int)} is then called. 
	 * 	</li>
	 * 	<li>
	 * 		If <b>they are NOT supported</b>, {@link PreparedStatement#executeUpdate()} will merely be called.
	 * 	</li>
	 * </ul>
	 * 
	 * <p>
	 *	Before returning, and only if batch update queries are not supported, this function is ensuring that exactly one row has been updated.
	 *	If it is not the case, a {@link DBException} is thrown.
	 * </p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If the function {@link PreparedStatement#addBatch()} fails by throwing an {@link SQLException}, batch updates
	 * 	will be afterwards considered as not supported by this connection. Besides, if this row is the first one in a batch update (parameter indRow=1),
	 * 	then, the error will just be logged and an {@link PreparedStatement#executeUpdate()} will be tried. However, if the row is not the first one,
	 * 	the error will be logged but also thrown as a {@link DBException}. In both cases, a subsequent call to
	 * 	{@link #executeBatchUpdates(PreparedStatement, int)} will have obviously no effect.
	 * </i></p>
	 * 
	 * @param stmt		{@link PreparedStatement} in which the update query has been prepared.
	 * @param indRow	Index of the row in the whole update process. It is used only for error management purpose.
	 * 
	 * @throws SQLException	If {@link PreparedStatement#executeUpdate()} fails.</i>
	 * @throws DBException	If {@link PreparedStatement#addBatch()} fails and this update does not concern the first row, or if the number of updated rows is different from 1.
	 */
	protected final void executeUpdate(final PreparedStatement stmt, int indRow) throws SQLException, DBException{
		// BATCH INSERTION: (the query is queued and will be executed later)
		if (supportsBatchUpdates){
			// Add the prepared query in the batch queue of the statement:
			try{
				stmt.addBatch();
			}catch(SQLException se){
				supportsBatchUpdates = false;
				/*
				 * If the error happens for the first row, it is still possible to insert all rows
				 * with the non-batch function - executeUpdate().
				 * 
				 * Otherwise, it is impossible to insert the previous batched rows ; an exception must be thrown
				 * and must stop the whole TAP_SCHEMA initialization.
				 */
				if (indRow == 1)
					logger.logDB(LogLevel.WARNING, this, "EXEC_UPDATE", "BATCH query impossible => TRYING AGAIN IN A NORMAL EXECUTION (executeUpdate())!", se);
				else{
					logger.logDB(LogLevel.ERROR, this, "EXEC_UPDATE", "BATCH query impossible!", se);
					throw new DBException("BATCH query impossible!", se);
				}
			}
		}

		// NORMAL INSERTION: (immediate insertion)
		if (!supportsBatchUpdates){

			// Insert the row prepared in the given statement:
			int nbRowsWritten = stmt.executeUpdate();

			// Check the row has been inserted with success:
			if (nbRowsWritten != 1){
				logger.logDB(LogLevel.ERROR, this, "EXEC_UPDATE", "ROW " + indRow + " not inserted!", null);
				throw new DBException("ROW " + indRow + " not inserted!");
			}
		}
	}

	/**
	 * <p>Execute all batched queries.</p>
	 * 
	 * <p>To do so, {@link PreparedStatement#executeBatch()} and then, if the first was successful, {@link PreparedStatement#clearBatch()} is called.</p>
	 * 
	 * <p>
	 *	Before returning, this function is ensuring that exactly the given number of rows has been updated.
	 *	If it is not the case, a {@link DBException} is thrown.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	This function has no effect if batch queries are not supported.
	 * </i></p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	In case {@link PreparedStatement#executeBatch()} fails by throwing an {@link SQLException},
	 * 	batch update queries will be afterwards considered as not supported by this connection.
	 * </i></p>
	 * 
	 * @param stmt		{@link PreparedStatement} in which the update query has been prepared.
	 * @param nbRows	Number of rows that should be updated.
	 * 
	 * @throws DBException	If {@link PreparedStatement#executeBatch()} fails, or if the number of updated rows is different from the given one.
	 */
	protected final void executeBatchUpdates(final PreparedStatement stmt, int nbRows) throws DBException{
		if (supportsBatchUpdates){
			// Execute all the batch queries:
			int[] rows;
			try{
				rows = stmt.executeBatch();
			}catch(SQLException se){
				supportsBatchUpdates = false;
				logger.logDB(LogLevel.ERROR, this, "EXEC_UPDATE", "BATCH execution impossible!", se);
				throw new DBException("BATCH execution impossible!", se);
			}

			// Remove executed queries from the statement:
			try{
				stmt.clearBatch();
			}catch(SQLException se){
				logger.logDB(LogLevel.WARNING, this, "EXEC_UPDATE", "CLEAR BATCH impossible!", se);
			}

			// Count the updated rows:
			int nbRowsUpdated = 0;
			for(int i = 0; i < rows.length; i++)
				nbRowsUpdated += rows[i];

			// Check all given rows have been inserted with success:
			if (nbRowsUpdated != nbRows){
				logger.logDB(LogLevel.ERROR, this, "EXEC_UPDATE", "ROWS not all update (" + nbRows + " to update ; " + nbRowsUpdated + " updated)!", null);
				throw new DBException("ROWS not all updated (" + nbRows + " to update ; " + nbRowsUpdated + " updated)!");
			}
		}
	}

	/**
	 * Append all items of the iterator inside the given list.
	 * 
	 * @param lst	List to update.
	 * @param it	All items to append inside the list.
	 */
	private < T > void appendAllInto(final List<T> lst, final Iterator<T> it){
		while(it.hasNext())
			lst.add(it.next());
	}

	/**
	 * <p>Tell whether the given DB name is equals (case sensitively or not, in function of the given parameter)
	 * 	to the given name coming from a {@link TAPMetadata} object.</p>
	 * 
	 * <p>If at least one of the given name is NULL, <i>false</i> is returned.</p>
	 * 
	 * <p><i>Note:
	 * 	The comparison will be done in function of the specified case sensitivity BUT ALSO of the case supported and stored by the DBMS.
	 * 	For instance, if it has been specified a case insensitivity and that mixed case is not supported by unquoted identifier,
	 * 	the comparison must be done, surprisingly, by considering the case if unquoted identifiers are stored in lower or upper case.
	 * 	Thus, this special way to evaluate equality should be as closed as possible to the identifier storage and research policies of the used DBMS.
	 * </i></p> 
	 * 
	 * @param dbName		Name provided by the database.
	 * @param metaName		Name provided by a {@link TAPMetadata} object.
	 * @param caseSensitive	<i>true</i> if the equality test must be done case sensitively, <i>false</i> otherwise.
	 * 
	 * @return	<i>true</i> if both names are equal, <i>false</i> otherwise.
	 */
	protected final boolean equals(final String dbName, final String metaName, final boolean caseSensitive){
		if (dbName == null || metaName == null)
			return false;

		if (caseSensitive){
			if (supportsMixedCaseQuotedIdentifier || mixedCaseQuoted)
				return dbName.equals(metaName);
			else if (lowerCaseQuoted)
				return dbName.equals(metaName.toLowerCase());
			else if (upperCaseQuoted)
				return dbName.equals(metaName.toUpperCase());
			else
				return dbName.equalsIgnoreCase(metaName);
		}else{
			if (supportsMixedCaseUnquotedIdentifier)
				return dbName.equalsIgnoreCase(metaName);
			else if (lowerCaseUnquoted)
				return dbName.equals(metaName.toLowerCase());
			else if (upperCaseUnquoted)
				return dbName.equals(metaName.toUpperCase());
			else
				return dbName.equalsIgnoreCase(metaName);
		}
	}
}
