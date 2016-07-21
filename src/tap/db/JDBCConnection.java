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
 * Copyright 2012-2016 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import adql.db.DBColumn;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.STCS;
import adql.db.STCS.Region;
import adql.query.ADQLQuery;
import adql.query.IdentifierField;
import adql.translator.ADQLTranslator;
import adql.translator.JDBCTranslator;
import adql.translator.TranslationException;
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
import uws.ISO8601Format;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>This {@link DBConnection} implementation is theoretically able to deal with any DBMS JDBC connection.</p>
 * 
 * <p><i>Note:
 * 	"Theoretically", because its design has been done using information about Postgres, SQLite, Oracle, MySQL and Java DB (Derby).
 * 	Then it has been really tested successfully with Postgres and SQLite.
 * </i></p>
 * 
 * 
 * <h3>Only one query executed at a time!</h3>
 * 
 * <p>
 * 	With a single instance of {@link JDBCConnection} it is possible to execute only one query (whatever the type: SELECT, UPDATE, DELETE, ...)
 * 	at a time. This is indeed the simple way chosen with this implementation in order to allow the cancellation of any query by managing only
 * 	one {@link Statement}. Indeed, only a {@link Statement} has a cancel function able to stop any query execution on the database.
 * 	So all queries are executed with the same {@link Statement}. Thus, allowing the execution of one query at a time lets
 * 	abort only one query rather than several in once (though just one should have been stopped).
 * </p>
 * 
 * <p>
 * 	All the following functions are synchronized in order to prevent parallel execution of them by several threads:
 * 	{@link #addUploadedTable(TAPTable, TableIterator)}, {@link #dropUploadedTable(TAPTable)}, {@link #executeQuery(ADQLQuery)},
 * 	{@link #getTAPSchema()} and {@link #setTAPSchema(TAPMetadata)}.
 * </p>
 * 
 * <p>
 * 	To cancel a query execution the function {@link #cancel(boolean)} must be called. No error is returned by this function in case
 * 	no query is currently executing.
 * </p>
 * 
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
 * 	                    Besides, if not already done, database name of all tables will be prefixed by the schema name.</li>
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
 * 
 * <h3>Datatypes</h3>
 * 
 * <p>
 * 	All datatype conversions done while fetching a query result (via a {@link ResultSet})
 * 	are done exclusively by the returned {@link TableIterator} (so, here {@link ResultSetTableIterator}).
 * </p>
 * 
 * <p>
 * 	However, datatype conversions done while uploading a table are done here by the function
 * 	{@link #convertTypeToDB(DBType)}. This function uses first the conversion function of the translator
 * 	({@link JDBCTranslator#convertTypeToDB(DBType)}), and then {@link #defaultTypeConversion(DBType)}
 * 	if it fails.
 * </p>
 * 
 * <p>
 * 	In this default conversion, all typical DBMS datatypes are taken into account, <b>EXCEPT the geometrical types</b>
 * 	(POINT and REGION). That's why it is recommended to use a translator in which the geometrical types are supported
 * 	and managed.
 * </p>
 * 
 * 
 * <h3>Fetch size</h3>
 * 
 * <p>
 * 	The possibility to specify a "fetch size" to the JDBC driver (and more exactly to a {@link Statement}) may reveal
 * 	very helpful when dealing with large datasets. Thus, it is possible to fetch rows by block of a size represented
 * 	by this "fetch size". This is also possible with this {@link DBConnection} thanks to the function {@link #setFetchSize(int)}.
 * </p>
 * 
 * <p>
 * 	However, some JDBC driver or DBMS may not support this feature. In such case, it is then automatically disabled by
 * 	{@link JDBCConnection} so that any subsequent queries do not attempt to use it again. The {@link #supportsFetchSize}
 * 	is however reset to <code>true</code> when {@link #setFetchSize(int)} is called.
 * </p>
 * 
 * <p><i>Note 1:
 * 	The "fetch size" feature is used only for SELECT queries executed by {@link #executeQuery(ADQLQuery)}. In all other functions,
 * 	results of SELECT queries are fetched with the default parameter of the JDBC driver and its {@link Statement} implementation.
 * </i></p>
 * 
 * <p><i>Note 2:
 * 	By default, this feature is disabled. So the default value of the JDBC driver is used.
 * 	To enable it, a simple call to {@link #setFetchSize(int)} is enough, whatever is the given value.
 * </i></p>
 * 
 * <p><i>Note 3:
 * 	Generally set a fetch size starts a transaction in the database. So, after the result of the fetched query
 * 	is not needed any more, do not forget to call {@link #endQuery()} in order to end the implicitly opened transaction.
 * 	However, generally closing the returned {@link TableIterator} is fully enough (see the sources of
 * 	{@link ResultSetTableIterator#close()} for more details).
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.1 (07/2016)
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

	/** Name of the database column giving the database name of a TAP column, table or schema. */
	protected final static String DB_NAME_COLUMN = "dbname";

	/** Connection ID (typically, the job ID). It lets identify the DB errors linked to the Job execution in the logs. */
	protected final String ID;

	/** JDBC connection (created and initialized at the creation of this {@link JDBCConnection} instance). */
	protected final Connection connection;

	/** <p>The only {@link Statement} instance that should be used in this {@link JDBCConnection}.
	 * Having the same {@link Statement} for all the interactions with the database lets cancel any when needed (e.g. when the execution is too long).</p>
	 * <p>This statement is by default NULL ; it must be initialized by the function {@link #getStatement()}.</p>
	 * @since 2.1 */
	protected Statement stmt = null;

	/**
	 * <p>It <code>true</code>, this flag indicates that the function {@link #cancel(boolean)} has been called successfully.</p>
	 * 
	 * <p>{@link #cancel(boolean)} sets this flag to <code>true</code>.</p>
	 * <p>
	 * 	All functions executing any kind of query on the database MUST set this flag to <code>false</code> before doing anything
	 * 	by calling the function {@link #resetCancel()}.
	 * </p>
	 * <p>
	 * 	This flag is particularly useful for debugging: when an exception is detected inside a function executing a query,
	 * 	this flag is used to know whether the exception should be ignored for logging (if <code>true</code>) or not.
	 * </p>
	 * <p>
	 * 	Any access (write AND read) to this flag MUST be synchronized on it using one of the following functions:
	 * 	{@link #cancel(boolean)}, {@link #resetCancel()} and {@link #isCancelled()}.
	 * </p>
	 * @since 2.1 */
	private Boolean cancelled = false;

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

	/** <p>Indicate whether a DBMS statement is able to cancel a query execution.</p>
	 * <p> Since this information is not provided by {@link DatabaseMetaData} a first attempt is always performed.
	 * In case a {@link SQLFeatureNotSupportedException} is caught, this flag is set to false preventing any further
	 * attempt of canceling a query.</p>
	 * @since 2.1 */
	protected boolean supportsCancel = true;

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

	/* FETCH SIZE */

	/** Special fetch size meaning that the JDBC driver is free to set its own guess for this value. */
	public final static int IGNORE_FETCH_SIZE = 0;
	/** Default fetch size.
	 * <i>Note 1: this value may be however ignored if the JDBC driver does not support this feature.</i>
	 * <i>Note 2: by default set to {@link #IGNORE_FETCH_SIZE}.</i> */
	public final static int DEFAULT_FETCH_SIZE = IGNORE_FETCH_SIZE;

	/** <p>Indicate whether the last fetch size operation works.</p>
	 * <p>By default, this attribute is set to <code>false</code>, meaning that the "fetch size" feature is
	 * disabled. To enable it, a simple call to {@link #setFetchSize(int)} is enough, whatever is the given value.</p>
	 * <p>If just once this operation fails, the fetch size feature will be always considered as unsupported in this {@link JDBCConnection}
	 * until the next call of {@link #setFetchSize(int)}.</p> */
	protected boolean supportsFetchSize = false;

	/** <p>Fetch size to set in the {@link Statement} in charge of executing a SELECT query.</p>
	 * <p><i>Note 1: this value must always be positive. If negative or null, it will be ignored and the {@link Statement} will keep its default behavior.</i></p>
	 * <p><i>Note 2: if this feature is enabled (i.e. has a value &gt; 0), the AutoCommit will be disabled.</i></p> */
	protected int fetchSize = DEFAULT_FETCH_SIZE;

	/**
	 * <p>Creates a JDBC connection to the specified database and with the specified JDBC driver.
	 * This connection is established using the given user name and password.<p>
	 * 
	 * <p><i><u>note:</u> the JDBC driver is loaded using <pre>Class.forName(driverPath)</pre> and the connection is created with <pre>DriverManager.getConnection(dbUrl, dbUser, dbPassword)</pre>.</i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	This constructor really creates a new SQL connection. Creating a SQL connection is time consuming!
	 * 	That's why it is recommended to use a pool of connections. When doing so, you should use the other constructor of this class
	 * 	({@link #JDBCConnection(Connection, JDBCTranslator, String, TAPLog)}).
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
	 * Create a {@link Connection} instance using the given database parameters.
	 * The path of the JDBC driver will be used to load the adequate driver if none is found by default.
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
	 * @see DriverManager#getDriver(String)
	 * @see Driver#connect(String, Properties)
	 */
	private final static Connection createConnection(final String driverPath, final String dbUrl, final String dbUser, final String dbPassword) throws DBException{
		// Normalize the DB URL:
		String url = dbUrl.startsWith(JDBC_PREFIX) ? dbUrl : (JDBC_PREFIX + dbUrl);

		// Select the JDBDC driver:
		Driver d;
		try{
			d = DriverManager.getDriver(dbUrl);
		}catch(SQLException e){
			try{
				// ...load it, if necessary:
				if (driverPath == null)
					throw new DBException("Missing JDBC driver path! Since the required JDBC driver is not yet loaded, this path is needed to load it.");
				Class.forName(driverPath);
				// ...and try again:
				d = DriverManager.getDriver(dbUrl);
			}catch(ClassNotFoundException cnfe){
				throw new DBException("Impossible to find the JDBC driver \"" + driverPath + "\" !", cnfe);
			}catch(SQLException se){
				throw new DBException("No suitable JDBC driver found for the database URL \"" + dbUrl + "\" and the driver path \"" + driverPath + "\"!", se);
			}
		}

		// Build a connection to the specified database:
		try{
			Properties p = new Properties();
			if (dbUser != null)
				p.setProperty("user", dbUser);
			if (dbPassword != null)
				p.setProperty("password", dbPassword);
			Connection con = d.connect(url, p);
			return con;
		}catch(SQLException se){
			throw new DBException("Impossible to establish a connection to the database \"" + url + "\"!", se);
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

	/**
	 * <p>Tell whether this {@link JDBCConnection} is already associated with a {@link Statement}.</p>
	 * 
	 * @return	<code>true</code> if a {@link Statement} instance is already associated with this {@link JDBCConnection}
	 *        	<code>false</code> otherwise.
	 * 
	 * @throws SQLException	In case the open/close status of the current {@link Statement} instance can not be checked.
	 * 
	 * @since 2.1
	 */
	protected boolean hasStatement() throws SQLException{
		return (stmt != null && !stmt.isClosed());
	}

	/**
	 * <p>Get the only statement associated with this {@link JDBCConnection}.</p>
	 * 
	 * <p>
	 * 	If no {@link Statement} is yet existing, one is created, stored in this {@link JDBCConnection} (for further uses)
	 * 	and then returned.
	 * </p>
	 * 
	 * @return	The {@link Statement} instance associated with this {@link JDBCConnection}. <i>Never NULL</i>
	 * 
	 * @throws SQLException	In case a {@link Statement} can not be created.
	 * 
	 * @since 2.1
	 */
	protected Statement getStatement() throws SQLException{
		if (hasStatement())
			return stmt;
		else
			return (stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
	}

	/**
	 * Close the only statement associated with this {@link JDBCConnection}.
	 * 
	 * @since 2.1
	 */
	protected void closeStatement(){
		close(stmt);
		stmt = null;
	}

	/**
	 * <p>Cancel (and rollback when possible) the currently running query of this {@link JDBCConnection} instance.</p>
	 * 
	 * <p><b>Important note:</b>
	 * 	This function is effective only if the JDBC driver and DBMS both support
	 * 	this operation.
	 * </p>
	 * <p>
	 * 	If a call of this function fails the flag {@link #supportsCancel} is set to false
	 * 	so that any subsequent call of this function for this instance of {@link JDBCConnection}
	 * 	does not try any other cancellation attempt. <b>HOWEVER</b> the rollback will still continue
	 * 	to be performed if the parameter <code>rollback</code> is set to <code>true</code>.
	 * </p>
	 * 
	 * <p><i>Note 1:
	 * 	A failure of a rollback is not considered as a not supported cancellation feature by the JDBC driver or the DBMS.
	 * 	So if the cancellation succeeds but a rollback fails, a next call of this function will still try canceling the given statement.
	 * 	In case of a rollback failure, only a WARNING is written in the log file ; no exception is thrown.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	In case of cancellation success, the flag {@link #cancelled} is set to <code>true</code>.
	 * 	Thus, the function executing a query can know that if any SQL exception is thrown, it will be due to the cancellation and
	 * 	should not be then considered as a real error (=> exception not logged but anyway propagated in order to stop any processing).
	 * </i></p></p>
	 * 
	 * <p><i>Note 3:
	 * 	This function is synchronized on the {@link #cancelled} flag.
	 * 	Thus, it may block until another synchronized block on this same flag is finished.
	 * </i></p>
	 * 
	 * @param rollback	<code>true</code> to cancel the statement AND rollback the current connection transaction,
	 *                	<code>false</code> to just cancel the statement.
	 * 
	 * @see DBConnection#cancel(boolean)
	 * @see #cancel(Statement, boolean)
	 * 
	 * @since 2.1
	 */
	@Override
	public final void cancel(final boolean rollback){
		synchronized(cancelled){
			cancelled = cancel(stmt, rollback);
			// Log the success of the cancellation:
			if (cancelled && logger != null)
				logger.logDB(LogLevel.INFO, this, "CANCEL", "Query execution successfully stopped!", null);
		}
	}

	/**
	 * <p>Cancel (and rollback when asked and if possible) the given statement.</p>
	 * 
	 * <p><b>Important note:</b>
	 * 	This function is effective only if the JDBC driver and DBMS both support
	 * 	this operation.
	 * </p>
	 * <p>
	 * 	If a call of this function fails the flag {@link #supportsCancel} is set to false
	 * 	so that any subsequent call of this function for this instance of {@link JDBCConnection}
	 * 	does not try any other cancellation attempt. <b>HOWEVER</b> the rollback will still continue
	 * 	to be performed if the parameter <code>rollback</code> is set to <code>true</code>.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	A failure of a rollback is not considered as a not supported cancellation feature by the JDBC driver or the DBMS.
	 * 	So if the cancellation succeeds but a rollback fails, a next call of this function will still try canceling the given statement.
	 * 	In case of a rollback failure, only a WARNING is written in the log file ; no exception is thrown.
	 * </i></p>
	 * 
	 * @param stmt		The statement to cancel. <i>Note: if closed or NULL, no exception will be thrown and only a rollback will be attempted if asked in parameter.</i>
	 * @param rollback	<code>true</code> to cancel the statement AND rollback the current connection transaction,
	 *                	<code>false</code> to just cancel the statement.
	 * 
	 * @return	<code>true</code> if the cancellation succeeded (or none was running),
	 *        	<code>false</code> otherwise (and especially if the "cancel" operation is not supported).
	 * 
	 * @since 2.1
	 */
	protected boolean cancel(final Statement stmt, final boolean rollback){
		try{
			// If the statement is not already closed, cancel its current query execution:
			if (supportsCancel && stmt != null && !stmt.isClosed()){
				stmt.cancel();
				return true;
			}else
				return false;

		}catch(SQLFeatureNotSupportedException sfnse){
			// prevent further cancel attempts:
			supportsCancel = false;
			// log a warning:
			if (logger != null)
				logger.logDB(LogLevel.WARNING, this, "CANCEL", "This JDBC driver does not support Statement.cancel(). No further cancel attempt will be performed with this JDBCConnection instance.", sfnse);
			return false;

		}catch(SQLException se){
			if (logger != null)
				logger.logDB(LogLevel.ERROR, this, "CANCEL", "Abortion of the current query apparently fails! The query may still run on the database server.", se);
			return false;
		}
		// Whatever happens, rollback all executed operations (only if rollback=true and if in a transaction ; that's to say if AutoCommit = false):
		finally{
			if (rollback && supportsTransaction)
				rollback();
		}
	}

	/**
	 * <p>Tell whether the last query execution has been canceled.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is synchronized on the {@link #cancelled} flag.
	 * 	Thus, it may block until another synchronized block on this same flag is finished.
	 * </i></p>
	 * 
	 * @return	<code>true</code> if the last query execution has been cancelled,
	 *        	<code>false</code> otherwise.
	 * 
	 * @since 2.1
	 */
	protected final boolean isCancelled(){
		synchronized(cancelled){
			return cancelled;
		}
	}

	/**
	 * <p>Reset the {@link #cancelled} flag to <code>false</code>.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is synchronized on the {@link #cancelled} flag.
	 * 	Thus, it may block until another synchronized block on this same flag is finished.
	 * </i></p>
	 * 
	 * @since 2.1
	 */
	protected final void resetCancel(){
		synchronized(cancelled){
			cancelled = false;
		}
	}

	@Override
	public void endQuery(){
		// Cancel the last query processing, if still running:
		cancel(stmt, false);  // note: this function is called instead of cancel(false) in order to avoid a log message about the cancellation operation result.
		// Close the statement, if still opened:
		closeStatement();
		// Rollback the transaction, if one has been opened:
		rollback(false);
		// End the transaction (i.e. go back to autocommit=true), if one has been opened:
		endTransaction(false);
	}

	/* ********************* */
	/* INTERROGATION METHODS */
	/* ********************* */
	@Override
	public synchronized TableIterator executeQuery(final ADQLQuery adqlQuery) throws DBException{
		// Starting of new query execution => disable the cancel flag:
		resetCancel();

		String sql = null;
		ResultSet result = null;
		try{
			// 1. Translate the ADQL query into SQL:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "TRANSLATE", "Translating ADQL: " + adqlQuery.toADQL().replaceAll("(\t|\r?\n)+", " "), null);
			sql = translator.translate(adqlQuery);

			// 2. Create the statement and if needed, configure it for the given fetch size:
			if (supportsTransaction && supportsFetchSize && fetchSize > 0){
				try{
					connection.setAutoCommit(false);
				}catch(SQLException se){
					if (!isCancelled()){
						supportsFetchSize = false;
						if (logger != null)
							logger.logDB(LogLevel.WARNING, this, "RESULT", "Fetch size unsupported!", null);
					}
				}
			}

			getStatement();

			if (supportsFetchSize){
				try{
					stmt.setFetchSize(fetchSize);
				}catch(SQLException se){
					if (!isCancelled()){
						supportsFetchSize = false;
						if (logger != null)
							logger.logDB(LogLevel.WARNING, this, "RESULT", "Fetch size unsupported!", null);
					}
				}
			}

			// 3. Execute the SQL query:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "EXECUTE", "SQL query: " + sql.replaceAll("(\t|\r?\n)+", " "), null);
			result = stmt.executeQuery(sql);

			// 4. Return the result through a TableIterator object:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "RESULT", "Returning result (" + (supportsFetchSize ? "fetch size = " + fetchSize : "all in once") + ").", null);
			return createTableIterator(result, adqlQuery.getResultingColumns());

		}catch(Exception ex){
			// Close the ResultSet, if one was open:
			close(result);
			// End properly the query:
			endQuery();
			// Propagate the exception with an appropriate error message:
			if (ex instanceof SQLException)
				throw new DBException("Unexpected error while executing a SQL query: " + ex.getMessage(), ex);
			else if (ex instanceof TranslationException)
				throw new DBException("Unexpected error while translating ADQL into SQL: " + ex.getMessage(), ex);
			else if (ex instanceof DataReadException)
				throw new DBException("Impossible to read the query result, because: " + ex.getMessage(), ex);
			else
				throw new DBException("Unexpected error while executing an ADQL query: " + ex.getMessage(), ex);
		}
	}

	/**
	 * <p>Create a {@link TableIterator} instance which lets reading the given result table.</p>
	 * 
	 * <p><i>Note:
	 * 	The statement currently opened is not closed by this function. Actually, it is still associated with
	 * 	this {@link JDBCConnection}. However, this latter is provided to the {@link TableIterator} returned by
	 * 	this function. Thus, when the {@link TableIterator#close()} is called, the function {@link #endQuery()}
	 * 	will be called. It will then close the {@link ResultSet}, the {@link Statement} and end any opened
	 * 	transaction (with rollback). See {@link #endQuery()} for more details.
	 * </i></p>
	 * 
	 * @param rs				Result of an SQL query.
	 * @param resultingColumns	Metadata corresponding to each columns of the result.
	 * 
	 * @return	A {@link TableIterator} instance.
	 * 
	 * @throws DataReadException	If the metadata (columns count and types) can not be fetched
	 *                          	or if any other error occurs.
	 * 
	 * @see ResultSetTableIterator#ResultSetTableIterator(DBConnection, ResultSet, DBColumn[], JDBCTranslator, String)
	 */
	protected TableIterator createTableIterator(final ResultSet rs, final DBColumn[] resultingColumns) throws DataReadException{
		try{
			return new ResultSetTableIterator(this, rs, resultingColumns, translator, dbms);
		}catch(Throwable t){
			throw (t instanceof DataReadException) ? (DataReadException)t : new DataReadException(t);
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
	 * 	and the DB name of all tables will be prefixed by the ADQL name of their respective schema.
	 * </i></p>
	 * 
	 * @see tap.db.DBConnection#getTAPSchema()
	 */
	@Override
	public synchronized TAPMetadata getTAPSchema() throws DBException{
		// Starting of new query execution => disable the cancel flag:
		resetCancel();

		// Build a virgin TAP metadata:
		TAPMetadata metadata = new TAPMetadata();

		// Get the definition of the standard TAP_SCHEMA tables:
		TAPSchema tap_schema = TAPMetadata.getStdSchema(supportsSchema);

		// LOAD ALL METADATA FROM THE STANDARD TAP TABLES:
		try{
			// create a common statement for all loading functions:
			getStatement();

			// load all schemas from TAP_SCHEMA.schemas:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.schemas.", null);
			loadSchemas(tap_schema.getTable(STDTable.SCHEMAS.label), metadata, stmt);

			// load all tables from TAP_SCHEMA.tables:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.tables.", null);
			List<TAPTable> lstTables = loadTables(tap_schema.getTable(STDTable.TABLES.label), metadata, stmt);

			// load all columns from TAP_SCHEMA.columns:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.columns.", null);
			loadColumns(tap_schema.getTable(STDTable.COLUMNS.label), lstTables, stmt);

			// load all foreign keys from TAP_SCHEMA.keys and TAP_SCHEMA.key_columns:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "LOAD_TAP_SCHEMA", "Loading TAP_SCHEMA.keys and TAP_SCHEMA.key_columns.", null);
			loadKeys(tap_schema.getTable(STDTable.KEYS.label), tap_schema.getTable(STDTable.KEY_COLUMNS.label), lstTables, stmt);

		}catch(SQLException se){
			if (!isCancelled() && logger != null)
				logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to create a Statement!", se);
			throw new DBException("Can not create a Statement!", se);
		}finally{
			cancel(stmt, true); // note: this function is called instead of cancel(true) in order to avoid a log message about the cancellation operation result.
			closeStatement();
		}

		return metadata;
	}

	/**
	 * <p>Load into the given metadata all schemas listed in TAP_SCHEMA.schemas.</p>
	 * 
	 * <p><i>Note 1:
	 * 	If schemas are not supported by this DBMS connection, the DB name of the loaded schemas is set to NULL.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	Schema entries are retrieved ordered by ascending schema_name.
	 * </i></p>
	 * 
	 * @param tableDef		Definition of the table TAP_SCHEMA.schemas.
	 * @param metadata		Metadata to fill with all found schemas.
	 * @param stmt			Statement to use in order to interact with the database.
	 * 
	 * @throws DBException	If any error occurs while interacting with the database.
	 */
	protected void loadSchemas(final TAPTable tableDef, final TAPMetadata metadata, final Statement stmt) throws DBException{
		ResultSet rs = null;
		try{
			// Determine whether the dbName column exists:
			/* note: if the schema notion is not supported by this DBMS, the column "dbname" is ignored. */
			boolean hasDBName = supportsSchema && isColumnExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), DB_NAME_COLUMN, connection.getMetaData());

			// Determine whether the schemaIndex column exists:
			boolean hasSchemaIndex = isColumnExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), "schema_index", connection.getMetaData());

			// Build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("SELECT ");
			sqlBuf.append(translator.getColumnName(tableDef.getColumn("schema_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("utype")));
			if (hasSchemaIndex)
				sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("schema_index")));
			if (hasDBName){
				sqlBuf.append(", ");
				translator.appendIdentifier(sqlBuf, DB_NAME_COLUMN, IdentifierField.COLUMN);
			}
			sqlBuf.append(" FROM ").append(translator.getTableName(tableDef, supportsSchema));
			if (hasSchemaIndex)
				sqlBuf.append(" ORDER BY 4");
			else
				sqlBuf.append(" ORDER BY 1");

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all schemas:
			while(rs.next()){
				String schemaName = rs.getString(1),
						description = rs.getString(2), utype = rs.getString(3),
						dbName = (hasDBName ? (hasSchemaIndex ? rs.getString(5) : rs.getString(4)) : null);
				int schemaIndex = (hasSchemaIndex ? (rs.getObject(4) == null ? -1 : rs.getInt(4)) : -1);

				// create the new schema:
				TAPSchema newSchema = new TAPSchema(schemaName, nullifyIfNeeded(description), nullifyIfNeeded(utype));
				if (dbName != null && dbName.trim().length() > 0)
					newSchema.setDBName(dbName);
				newSchema.setIndex(schemaIndex);

				// add the new schema inside the given metadata:
				metadata.addSchema(newSchema);
			}
		}catch(SQLException se){
			if (!isCancelled() && logger != null)
				logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load schemas from TAP_SCHEMA.schemas!", se);
			throw new DBException("Impossible to load schemas from TAP_SCHEMA.schemas!", se);
		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Load into the corresponding metadata all tables listed in TAP_SCHEMA.tables.</p>
	 * 
	 * <p><i>Note 1:
	 * 	Schemas are searched in the given metadata by their ADQL name and case sensitively.
	 * 	If they can not be found a {@link DBException} is thrown.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	If schemas are not supported by this DBMS connection, the DB name of the loaded
	 * 	{@link TAPTable}s is prefixed by the ADQL name of their respective schema.
	 * </i></p>
	 * 
	 * <p><i>Note 3:
	 * 	If the column table_index exists, table entries are retrieved ordered by ascending schema_name, then table_index, and finally table_name.
	 * 	If this column does not exist, table entries are retrieved ordered by ascending schema_name and then table_name.
	 * </i></p>
	 * 
	 * @param tableDef		Definition of the table TAP_SCHEMA.tables.
	 * @param metadata		Metadata (containing already all schemas listed in TAP_SCHEMA.schemas).
	 * @param stmt			Statement to use in order to interact with the database.
	 * 
	 * @return	The complete list of all loaded tables. <i>note: this list is required by {@link #loadColumns(TAPTable, List, Statement)}.</i>
	 * 
	 * @throws DBException	If a schema can not be found, or if any other error occurs while interacting with the database.
	 */
	protected List<TAPTable> loadTables(final TAPTable tableDef, final TAPMetadata metadata, final Statement stmt) throws DBException{
		ResultSet rs = null;
		try{
			// Determine whether the dbName column exists:
			boolean hasDBName = isColumnExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), DB_NAME_COLUMN, connection.getMetaData());

			// Determine whether the tableIndex column exists:
			boolean hasTableIndex = isColumnExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), "table_index", connection.getMetaData());

			// Build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("SELECT ");
			sqlBuf.append(translator.getColumnName(tableDef.getColumn("schema_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("table_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("table_type")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("utype")));
			if (hasTableIndex)
				sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("table_index")));
			if (hasDBName){
				sqlBuf.append(", ");
				translator.appendIdentifier(sqlBuf, DB_NAME_COLUMN, IdentifierField.COLUMN);
			}
			sqlBuf.append(" FROM ").append(translator.getTableName(tableDef, supportsSchema));
			if (hasTableIndex)
				sqlBuf.append(" ORDER BY 1,6,2");
			else
				sqlBuf.append(" ORDER BY 1,2");
			sqlBuf.append(';');

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all tables:
			ArrayList<TAPTable> lstTables = new ArrayList<TAPTable>();
			while(rs.next()){
				String schemaName = rs.getString(1),
						tableName = rs.getString(2), typeStr = rs.getString(3),
						description = rs.getString(4), utype = rs.getString(5),
						dbName = (hasDBName ? (hasTableIndex ? rs.getString(7) : rs.getString(6)) : null);
				int tableIndex = (hasTableIndex ? (rs.getObject(6) == null ? -1 : rs.getInt(6)) : -1);

				// get the schema:
				TAPSchema schema = metadata.getSchema(schemaName);
				if (schema == null){
					if (logger != null)
						logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to find the schema of the table \"" + tableName + "\": \"" + schemaName + "\"!", null);
					throw new DBException("Impossible to find the schema of the table \"" + tableName + "\": \"" + schemaName + "\"!");
				}

				// If the table name is qualified, check its prefix (it must match to the schema name):
				int endPrefix = tableName.indexOf('.');
				if (endPrefix >= 0){
					if (endPrefix == 0)
						throw new DBException("Incorrect table name syntax: \"" + tableName + "\"! Missing schema name (before '.').");
					else if (endPrefix == tableName.length() - 1)
						throw new DBException("Incorrect table name syntax: \"" + tableName + "\"! Missing table name (after '.').");
					else if (schemaName == null)
						throw new DBException("Incorrect schema prefix for the table \"" + tableName.substring(endPrefix + 1) + "\": this table is not in a schema, according to the column \"schema_name\" of TAP_SCHEMA.tables!");
					else if (!tableName.substring(0, endPrefix).trim().equalsIgnoreCase(schemaName))
						throw new DBException("Incorrect schema prefix for the table \"" + schemaName + "." + tableName.substring(tableName.indexOf('.') + 1) + "\": " + tableName + "! Mismatch between the schema specified in prefix of the column \"table_name\" and in the column \"schema_name\".");
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
				newTable.setDBName(dbName);
				newTable.setIndex(tableIndex);

				// add the new table inside its corresponding schema:
				schema.addTable(newTable);
				lstTables.add(newTable);
			}

			return lstTables;
		}catch(SQLException se){
			if (!isCancelled() && logger != null)
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
	 * <p><i>Note 2:
	 * 	If the column column_index exists, column entries are retrieved ordered by ascending table_name, then column_index, and finally column_name.
	 * 	If this column does not exist, column entries are retrieved ordered by ascending table_name and then column_name.
	 * </i></p>
	 * 
	 * @param tableDef		Definition of the table TAP_SCHEMA.columns.
	 * @param lstTables		List of all published tables (= all tables listed in TAP_SCHEMA.tables).
	 * @param stmt			Statement to use in order to interact with the database.
	 * 
	 * @throws DBException	If a table can not be found, or if any other error occurs while interacting with the database.
	 */
	protected void loadColumns(final TAPTable tableDef, final List<TAPTable> lstTables, final Statement stmt) throws DBException{
		ResultSet rs = null;
		try{
			// Determine whether the dbName column exists:
			boolean hasArraysize = isColumnExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), "arraysize", connection.getMetaData());

			// Determine whether the dbName column exists:
			boolean hasDBName = isColumnExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), DB_NAME_COLUMN, connection.getMetaData());

			// Determine whether the columnIndex column exists:
			boolean hasColumnIndex = isColumnExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), "column_index", connection.getMetaData());

			// Build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("SELECT ");
			sqlBuf.append(translator.getColumnName(tableDef.getColumn("table_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("column_name")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("unit")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("ucd")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("utype")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("datatype")));
			if (hasArraysize)
				sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("arraysize")));
			else
				sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("size")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("principal")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("indexed")));
			sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("std")));
			if (hasColumnIndex)
				sqlBuf.append(", ").append(translator.getColumnName(tableDef.getColumn("column_index")));
			if (hasDBName){
				sqlBuf.append(", ");
				translator.appendIdentifier(sqlBuf, DB_NAME_COLUMN, IdentifierField.COLUMN);
			}
			sqlBuf.append(" FROM ").append(translator.getTableName(tableDef, supportsSchema));
			if (hasColumnIndex)
				sqlBuf.append(" ORDER BY 1,12,2");
			else
				sqlBuf.append(" ORDER BY 1,2");
			sqlBuf.append(';');

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all tables:
			while(rs.next()){
				String tableName = rs.getString(1),
						columnName = rs.getString(2),
						description = rs.getString(3), unit = rs.getString(4),
						ucd = rs.getString(5), utype = rs.getString(6),
						datatype = rs.getString(7),
						dbName = (hasDBName ? (hasColumnIndex ? rs.getString(13) : rs.getString(12)) : null);
				int size = rs.getInt(8),
						colIndex = (hasColumnIndex ? (rs.getObject(12) == null ? -1 : rs.getInt(12)) : -1);
				boolean principal = toBoolean(rs.getObject(9)),
						indexed = toBoolean(rs.getObject(10)),
						std = toBoolean(rs.getObject(11));

				// get the table:
				TAPTable table = searchTable(tableName, lstTables.iterator());
				if (table == null){
					if (logger != null)
						logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to find the table of the column \"" + columnName + "\": \"" + tableName + "\"!", null);
					throw new DBException("Impossible to find the table of the column \"" + columnName + "\": \"" + tableName + "\"!");
				}

				// resolve the column type (if any) ; by default, it will be "VARCHAR" if unknown or missing:
				DBDatatype tapDatatype = null;
				// ...try to resolve the datatype in function of all datatypes declared by the TAP standard.
				if (datatype != null){
					try{
						tapDatatype = DBDatatype.valueOf(datatype.toUpperCase());
					}catch(IllegalArgumentException iae){}
				}
				// ...build the column type:
				DBType type;
				if (tapDatatype == null)
					type = new DBType(DBDatatype.UNKNOWN);
				else
					type = new DBType(tapDatatype, size);

				// create the new column:
				TAPColumn newColumn = new TAPColumn(columnName, type, nullifyIfNeeded(description), nullifyIfNeeded(unit), nullifyIfNeeded(ucd), nullifyIfNeeded(utype));
				newColumn.setPrincipal(principal);
				newColumn.setIndexed(indexed);
				newColumn.setStd(std);
				newColumn.setDBName(dbName);
				newColumn.setIndex(colIndex);

				// add the new column inside its corresponding table:
				table.addColumn(newColumn);
			}
		}catch(SQLException se){
			if (!isCancelled() && logger != null)
				logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load columns from TAP_SCHEMA.columns!", se);
			throw new DBException("Impossible to load columns from TAP_SCHEMA.columns!", se);
		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Load into the corresponding tables all keys listed in TAP_SCHEMA.keys and detailed in TAP_SCHEMA.key_columns.</p>
	 * 
	 * <p><i>Note 1:
	 * 	Tables and columns are searched in the given list by their ADQL name and case sensitively.
	 * 	If they can not be found a {@link DBException} is thrown.
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	Key entries are retrieved ordered by ascending key_id, then from_table and finally target_table.
	 * 	Key_Column entries are retrieved ordered by ascending from_column and then target_column.
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
			sqlBuf.append(translator.getColumnName(keyColumnsDef.getColumn("from_column")));
			sqlBuf.append(", ").append(translator.getColumnName(keyColumnsDef.getColumn("target_column")));
			sqlBuf.append(" FROM ").append(translator.getTableName(keyColumnsDef, supportsSchema));
			sqlBuf.append(" WHERE ").append(translator.getColumnName(keyColumnsDef.getColumn("key_id"))).append(" = ?");
			sqlBuf.append(" ORDER BY 1,2");
			keyColumnsStmt = connection.prepareStatement(sqlBuf.toString());

			// Build the SQL query to get the keys:
			sqlBuf.delete(0, sqlBuf.length());
			sqlBuf.append("SELECT ").append(translator.getColumnName(keysDef.getColumn("key_id")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("from_table")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("target_table")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("description")));
			sqlBuf.append(", ").append(translator.getColumnName(keysDef.getColumn("utype")));
			sqlBuf.append(" FROM ").append(translator.getTableName(keysDef, supportsSchema));
			sqlBuf.append(" ORDER BY 1,2,3;");

			// Execute the query:
			rs = stmt.executeQuery(sqlBuf.toString());

			// Create all foreign keys:
			while(rs.next()){
				String key_id = rs.getString(1), from_table = rs.getString(2),
						target_table = rs.getString(3),
						description = rs.getString(4), utype = rs.getString(5);

				// get the two tables (source and target):
				TAPTable sourceTable = searchTable(from_table, lstTables.iterator());
				if (sourceTable == null){
					if (logger != null)
						logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to find the source table of the foreign key \"" + key_id + "\": \"" + from_table + "\"!", null);
					throw new DBException("Impossible to find the source table of the foreign key \"" + key_id + "\": \"" + from_table + "\"!");
				}
				TAPTable targetTable = searchTable(target_table, lstTables.iterator());
				if (targetTable == null){
					if (logger != null)
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
					if (!isCancelled() && logger != null)
						logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to load key columns from TAP_SCHEMA.key_columns for the foreign key: \"" + key_id + "\"!", se);
					throw new DBException("Impossible to load key columns from TAP_SCHEMA.key_columns for the foreign key: \"" + key_id + "\"!", se);
				}finally{
					close(rsKeyCols);
				}

				// create and add the new foreign key inside the source table:
				try{
					sourceTable.addForeignKey(key_id, targetTable, columns, nullifyIfNeeded(description), nullifyIfNeeded(utype));
				}catch(Exception ex){
					if ((ex instanceof SQLException && !isCancelled()) && logger != null)
						logger.logDB(LogLevel.ERROR, this, "LOAD_TAP_SCHEMA", "Impossible to create the foreign key \"" + key_id + "\" because: " + ex.getMessage(), ex);
					throw new DBException("Impossible to create the foreign key \"" + key_id + "\" because: " + ex.getMessage(), ex);
				}
			}
		}catch(SQLException se){
			if (!isCancelled() && logger != null)
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
	public synchronized void setTAPSchema(final TAPMetadata metadata) throws DBException{
		// Starting of new query execution => disable the cancel flag:
		resetCancel();

		try{
			// A. GET THE DEFINITION OF ALL STANDARD TAP TABLES:
			TAPTable[] stdTables = mergeTAPSchemaDefs(metadata);

			startTransaction();

			// B. RE-CREATE THE STANDARD TAP_SCHEMA TABLES:
			getStatement();

			// 1. Ensure TAP_SCHEMA exists and drop all its standard TAP tables:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "CLEAN_TAP_SCHEMA", "Cleaning TAP_SCHEMA.", null);
			resetTAPSchema(stmt, stdTables);

			// 2. Create all standard TAP tables:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "CREATE_TAP_SCHEMA", "Creating TAP_SCHEMA tables.", null);
			for(TAPTable table : stdTables)
				createTAPSchemaTable(table, stmt);

			// C. FILL THE NEW TABLE USING THE GIVEN DATA ITERATOR:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "CREATE_TAP_SCHEMA", "Filling TAP_SCHEMA tables.", null);
			fillTAPSchema(metadata);

			// D. CREATE THE INDEXES OF ALL STANDARD TAP TABLES:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "CREATE_TAP_SCHEMA", "Creating TAP_SCHEMA tables' indexes.", null);
			for(TAPTable table : stdTables)
				createTAPTableIndexes(table, stmt);

			commit();
		}catch(SQLException se){
			if (!isCancelled() && logger != null)
				logger.logDB(LogLevel.ERROR, this, "CREATE_TAP_SCHEMA", "Impossible to SET TAP_SCHEMA in DB!", se);
			rollback();
			throw new DBException("Impossible to SET TAP_SCHEMA in DB!", se);
		}finally{
			closeStatement();
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
	 * 	the DB name of tables is prefixed by the schema name.
	 * </i></p>
	 * 
	 * @param metadata	Metadata (with or without TAP_SCHEMA schema or some of its table). <i>Must not be NULL</i>
	 * 
	 * @return	The list of all standard TAP_SCHEMA tables, ordered by creation order (see {@link #getCreationOrder(tap.metadata.TAPMetadata.STDTable)}).
	 * 
	 * @see TAPMetadata#resolveStdTable(String)
	 * @see TAPMetadata#getStdSchema(boolean)
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
			tapSchema = TAPMetadata.getStdSchema(supportsSchema);

			// add the new TAP_SCHEMA definition in the given metadata object:
			metadata.addSchema(tapSchema);
		}

		// 4. Finally, build the join between the standard tables and the custom ones:
		TAPTable[] stdTables = new TAPTable[]{TAPMetadata.getStdTable(STDTable.SCHEMAS),TAPMetadata.getStdTable(STDTable.TABLES),TAPMetadata.getStdTable(STDTable.COLUMNS),TAPMetadata.getStdTable(STDTable.KEYS),TAPMetadata.getStdTable(STDTable.KEY_COLUMNS)};
		for(int i = 0; i < stdTables.length; i++){

			// CASE: no custom definition:
			if (customStdTables[i] == null){
				if (!supportsSchema)
					stdTables[i].setDBName(STDSchema.TAPSCHEMA.label + "_" + stdTables[i].getADQLName());
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
		String dbSchemaName = (supportsSchema ? stdTables[0].getDBSchemaName() : null);

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
	 * @see JDBCTranslator#isCaseSensitive(IdentifierField)
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
				String rsSchema = nullifyIfNeeded(rs.getString(2)),
						rsTable = rs.getString(3);
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
	 * <p><i>Note:
	 * 	An extra column is added in TAP_SCHEMA.schemas, TAP_SCHEMA.tables and TAP_SCHEMA.columns: {@value #DB_NAME_COLUMN}.
	 * 	This column is particularly used when getting the TAP metadata from the database to alias some schema, table and/or column names in ADQL.
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
		sql.append(translator.getTableName(table, supportsSchema));

		// b. List all the columns:
		sql.append('(');
		Iterator<TAPColumn> it = table.getColumns();
		while(it.hasNext()){
			TAPColumn col = it.next();

			// column name:
			sql.append(translator.getColumnName(col));

			// column type:
			sql.append(' ').append(convertTypeToDB(col.getDatatype()));

			// last column ?
			if (it.hasNext())
				sql.append(',');
		}

		// b bis. Add the extra dbName column (giving the database name of a schema, table or column):
		if ((supportsSchema && table.getADQLName().equalsIgnoreCase(STDTable.SCHEMAS.label)) || table.getADQLName().equalsIgnoreCase(STDTable.TABLES.label) || table.getADQLName().equalsIgnoreCase(STDTable.COLUMNS.label))
			sql.append(',').append(DB_NAME_COLUMN).append(" VARCHAR");

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
				return " PRIMARY KEY(" + (caseSensitive ? "\"table_name\"" : "table_name") + ")";
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
		final String dbTableName = translator.getTableName(table, supportsSchema);

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
		sql.append(translator.getTableName(metaTable, supportsSchema)).append(" (");
		sql.append(translator.getColumnName(metaTable.getColumn("schema_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("description")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("utype")));
		if (supportsSchema){
			sql.append(", ").append(DB_NAME_COLUMN);
			sql.append(") VALUES (?, ?, ?, ?);");
		}else
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
				if (supportsSchema)
					stmt.setString(4, (schema.getDBName() == null || schema.getDBName().equals(schema.getADQLName())) ? null : schema.getDBName());
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
		sql.append(translator.getTableName(metaTable, supportsSchema)).append(" (");
		sql.append(translator.getColumnName(metaTable.getColumn("schema_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("table_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("table_type")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("description")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("utype")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("table_index")));
		sql.append(", ").append(DB_NAME_COLUMN);
		sql.append(") VALUES (?, ?, ?, ?, ?, ?, ?);");

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
				if (table instanceof TAPTable)
					stmt.setString(2, ((TAPTable)table).getRawName());
				else
					stmt.setString(2, table.getADQLName());
				stmt.setString(3, table.getType().toString());
				stmt.setString(4, table.getDescription());
				stmt.setString(5, table.getUtype());
				stmt.setInt(6, table.getIndex());
				stmt.setString(7, (table.getDBName() == null || table.getDBName().equals(table.getADQLName())) ? null : table.getDBName());
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
		sql.append(translator.getTableName(metaTable, supportsSchema)).append(" (");
		sql.append(translator.getColumnName(metaTable.getColumn("table_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("column_name")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("description")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("unit")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("ucd")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("utype")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("datatype")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("arraysize")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("size")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("principal")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("indexed")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("std")));
		sql.append(", ").append(translator.getColumnName(metaTable.getColumn("column_index")));
		sql.append(", ").append(DB_NAME_COLUMN);
		sql.append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

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
				if (col.getTable() instanceof TAPTable)
					stmt.setString(1, ((TAPTable)col.getTable()).getRawName());
				else
					stmt.setString(1, col.getTable().getADQLName());
				stmt.setString(2, col.getADQLName());
				stmt.setString(3, col.getDescription());
				stmt.setString(4, col.getUnit());
				stmt.setString(5, col.getUcd());
				stmt.setString(6, col.getUtype());
				stmt.setString(7, col.getDatatype().type.toString());
				stmt.setInt(8, col.getDatatype().length);
				stmt.setInt(9, col.getDatatype().length);
				stmt.setInt(10, col.isPrincipal() ? 1 : 0);
				stmt.setInt(11, col.isIndexed() ? 1 : 0);
				stmt.setInt(12, col.isStd() ? 1 : 0);
				stmt.setInt(13, col.getIndex());
				stmt.setString(14, (col.getDBName() == null || col.getDBName().equals(col.getADQLName())) ? null : col.getDBName());
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
		sqlKeys.append(translator.getTableName(metaKeys, supportsSchema)).append(" (");
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
			sqlKeyCols.append(translator.getTableName(metaKeyColumns, supportsSchema)).append(" (");
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
				if (key.getFromTable() instanceof TAPTable)
					stmtKeys.setString(2, ((TAPTable)key.getFromTable()).getRawName());
				else
					stmtKeys.setString(2, key.getFromTable().getADQLName());
				if (key.getTargetTable() instanceof TAPTable)
					stmtKeys.setString(3, ((TAPTable)key.getTargetTable()).getRawName());
				else
					stmtKeys.setString(3, key.getTargetTable().getADQLName());
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
	 * 	In this case, this function will prefix the table's DB name by the schema's DB name directly inside the given
	 * 	{@link TAPTable} object. Then the DB name of the schema will be set to NULL.
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
	public synchronized boolean addUploadedTable(TAPTable tableDef, TableIterator data) throws DBException, DataReadException{
		// If no table to upload, consider it has been dropped and return TRUE:
		if (tableDef == null)
			return true;

		// Starting of new query execution => disable the cancel flag:
		resetCancel();

		// Check the table is well defined (and particularly the schema is well set with an ADQL name = TAP_UPLOAD):
		checkUploadedTableDef(tableDef);

		try{

			// Start a transaction:
			startTransaction();
			// ...create a statement:
			getStatement();

			DatabaseMetaData dbMeta = connection.getMetaData();

			// 1. Create the upload schema, if it does not already exist:
			if (!isSchemaExisting(tableDef.getDBSchemaName(), dbMeta)){
				stmt.executeUpdate("CREATE SCHEMA " + translator.getQualifiedSchemaName(tableDef) + ";");
				if (logger != null)
					logger.logDB(LogLevel.INFO, this, "SCHEMA_CREATED", "Schema \"" + tableDef.getADQLSchemaName() + "\" (in DB: " + translator.getQualifiedSchemaName(tableDef) + ") created.", null);
			}
			// 1bis. Ensure the table does not already exist and if it is the case, throw an understandable exception:
			else if (isTableExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), dbMeta)){
				DBException de = new DBException("Impossible to create the user uploaded table in the database: " + translator.getTableName(tableDef, supportsSchema) + "! This table already exists.");
				if (logger != null)
					logger.logDB(LogLevel.ERROR, this, "ADD_UPLOAD_TABLE", de.getMessage(), de);
				throw de;
			}

			// 2. Create the table:
			// ...build the SQL query:
			StringBuffer sqlBuf = new StringBuffer("CREATE TABLE ");
			sqlBuf.append(translator.getTableName(tableDef, supportsSchema)).append(" (");
			Iterator<TAPColumn> it = tableDef.getColumns();
			while(it.hasNext()){
				TAPColumn col = it.next();
				// column name:
				sqlBuf.append(translator.getColumnName(col));
				// column type:
				sqlBuf.append(' ').append(convertTypeToDB(col.getDatatype()));
				// last column ?
				if (it.hasNext())
					sqlBuf.append(',');
			}
			sqlBuf.append(");");
			// ...execute the update query:
			stmt.executeUpdate(sqlBuf.toString());

			// 3. Fill the table:
			int nbUploadedRows = fillUploadedTable(tableDef, data);

			// Commit the transaction:
			commit();

			// Log the end:
			if (logger != null)
				logger.logDB(LogLevel.INFO, this, "TABLE_CREATED", "Table \"" + tableDef.getADQLName() + "\" (in DB: " + translator.getTableName(tableDef, supportsSchema) + ") created (" + nbUploadedRows + " rows).", null);

			return true;

		}catch(SQLException se){
			rollback();
			if (!isCancelled() && logger != null)
				logger.logDB(LogLevel.WARNING, this, "ADD_UPLOAD_TABLE", "Impossible to create the uploaded table: " + translator.getTableName(tableDef, supportsSchema) + "!", se);
			throw new DBException("Impossible to create the uploaded table: " + translator.getTableName(tableDef, supportsSchema) + "!", se);
		}catch(DBException de){
			rollback();
			throw de;
		}catch(DataReadException dre){
			rollback();
			throw dre;
		}finally{
			closeStatement();
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
	 * <p><i>Note:
	 * 	This function proceeds to a formatting of TIMESTAMP and GEOMETRY (point, circle, box, polygon) values.
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
		sql.append(translator.getTableName(metaTable, supportsSchema)).append(" (");
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
				while(data.hasNextCol()){
					Object val = data.nextCol();
					if (val != null && cols[c - 1] != null){
						/* TIMESTAMP FORMATTING */
						if (cols[c - 1].getDatatype().type == DBDatatype.TIMESTAMP){
							try{
								val = new Timestamp(ISO8601Format.parse(val.toString()));
							}catch(ParseException pe){
								if (logger != null)
									logger.logDB(LogLevel.ERROR, this, "UPLOAD", "[l. " + nbRows + ", c. " + c + "] Unexpected date format for the value: \"" + val + "\"! A date formatted in ISO8601 was expected.", pe);
								throw new DBException("[l. " + nbRows + ", c. " + c + "] Unexpected date format for the value: \"" + val + "\"! A date formatted in ISO8601 was expected.", pe);
							}
						}
						/* GEOMETRY FORMATTING */
						else if (cols[c - 1].getDatatype().type == DBDatatype.POINT || cols[c - 1].getDatatype().type == DBDatatype.REGION){
							Region region;
							// parse the region as an STC-S expression:
							try{
								region = STCS.parseRegion(val.toString());
							}catch(adql.parser.ParseException e){
								if (logger != null)
									logger.logDB(LogLevel.ERROR, this, "UPLOAD", "[l. " + nbRows + ", c. " + c + "] Incorrect STC-S syntax for the geometrical value \"" + val + "\"! " + e.getMessage(), e);
								throw new DataReadException("[l. " + nbRows + ", c. " + c + "] Incorrect STC-S syntax for the geometrical value \"" + val + "\"! " + e.getMessage(), e);
							}
							// translate this STC region into the corresponding column value:
							try{
								val = translator.translateGeometryToDB(region);
							}catch(adql.parser.ParseException e){
								if (logger != null)
									logger.logDB(LogLevel.ERROR, this, "UPLOAD", "[l. " + nbRows + ", c. " + c + "] Impossible to import the ADQL geometry \"" + val + "\" into the database! " + e.getMessage(), e);
								throw new DataReadException("[l. " + nbRows + ", c. " + c + "] Impossible to import the ADQL geometry \"" + val + "\" into the database! " + e.getMessage(), e);
							}
						}
						/* BOOLEAN CASE (more generally, type incompatibility) */
						else if (val != null && cols[c - 1].getDatatype().type == DBDatatype.SMALLINT && val instanceof Boolean)
							val = ((Boolean)val) ? (short)1 : (short)0;
						/* NULL CHARACTER CASE (JUST FOR POSTGRESQL) */
						else if ((dbms == null || dbms.equalsIgnoreCase(DBMS_POSTGRES)) && val instanceof Character && (Character)val == 0x00)
							val = null;
					}
					stmt.setObject(c++, val);
				}
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
	 * 	In this case, this function will prefix the table's DB name by the schema's DB name directly inside the given
	 * 	{@link TAPTable} object. Then the DB name of the schema will be set to NULL.
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
	public synchronized boolean dropUploadedTable(final TAPTable tableDef) throws DBException{
		// If no table to upload, consider it has been dropped and return TRUE:
		if (tableDef == null)
			return true;

		// Starting of new query execution => disable the cancel flag:
		resetCancel();

		// Check the table is well defined (and particularly the schema is well set with an ADQL name = TAP_UPLOAD):
		checkUploadedTableDef(tableDef);

		try{

			// Check the existence of the table to drop:
			if (!isTableExisting(tableDef.getDBSchemaName(), tableDef.getDBName(), connection.getMetaData()))
				return true;

			// Execute the update:
			int cnt = getStatement().executeUpdate("DROP TABLE " + translator.getTableName(tableDef, supportsSchema) + ";");

			// Log the end:
			if (logger != null){
				if (cnt >= 0)
					logger.logDB(LogLevel.INFO, this, "TABLE_DROPPED", "Table \"" + tableDef.getADQLName() + "\" (in DB: " + translator.getTableName(tableDef, supportsSchema) + ") dropped.", null);
				else
					logger.logDB(LogLevel.ERROR, this, "TABLE_DROPPED", "Table \"" + tableDef.getADQLName() + "\" (in DB: " + translator.getTableName(tableDef, supportsSchema) + ") NOT dropped.", null);
			}

			// Ensure the update is successful:
			return (cnt >= 0);

		}catch(SQLException se){
			if (!isCancelled() && logger != null)
				logger.logDB(LogLevel.WARNING, this, "DROP_UPLOAD_TABLE", "Impossible to drop the uploaded table: " + translator.getTableName(tableDef, supportsSchema) + "!", se);
			throw new DBException("Impossible to drop the uploaded table: " + translator.getTableName(tableDef, supportsSchema) + "!", se);
		}finally{
			cancel(true);
			closeStatement();
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
	 * 		inside the given {@link TAPTable} object. Then the DB name of the schema will be set to NULL.
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

		if (!supportsSchema){
			if (tableDef.getADQLSchemaName() != null && tableDef.getADQLSchemaName().trim().length() > 0 && !tableDef.getDBName().startsWith(tableDef.getADQLSchemaName() + "_"))
				tableDef.setDBName(tableDef.getADQLSchemaName() + "_" + tableDef.getDBName());
			if (tableDef.getSchema() != null)
				tableDef.getSchema().setDBName(null);
		}
	}

	/* ************** */
	/* TOOL FUNCTIONS */
	/* ************** */

	/**
	 * <p>Convert the given TAP type into the corresponding DBMS column type.</p>
	 * 
	 * <p>
	 * 	This function tries first the type conversion using the translator ({@link JDBCTranslator#convertTypeToDB(DBType)}).
	 * 	If it fails, a default conversion is done considering all the known types of the following DBMS:
	 * 	PostgreSQL, SQLite, MySQL, Oracle and JavaDB/Derby.
	 * </p>
	 * 
	 * @param type	TAP type to convert.
	 * 
	 * @return	The corresponding DBMS type.
	 * 
	 * @see JDBCTranslator#convertTypeToDB(DBType)
	 * @see #defaultTypeConversion(DBType)
	 */
	protected String convertTypeToDB(final DBType type){
		String dbmsType = translator.convertTypeToDB(type);
		return (dbmsType == null) ? defaultTypeConversion(type) : dbmsType;
	}

	/**
	 * <p>Get the DBMS compatible datatype corresponding to the given column {@link DBType}.</p>
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
	 * @return	The corresponding DB type, or VARCHAR if the given type is not managed or is NULL.
	 */
	protected String defaultTypeConversion(DBType datatype){
		if (datatype == null)
			datatype = new DBType(DBDatatype.VARCHAR);

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
				if (logger != null)
					logger.logDB(LogLevel.INFO, this, "START_TRANSACTION", "Transaction STARTED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			if (logger != null)
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
				if (logger != null)
					logger.logDB(LogLevel.INFO, this, "COMMIT", "Transaction COMMITED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			if (logger != null)
				logger.logDB(LogLevel.ERROR, this, "COMMIT", "Transaction COMMIT impossible!", se);
			throw new DBException("Transaction COMMIT impossible!", se);
		}
	}

	/**
	 * <p>Rollback the current transaction.
	 * The success or the failure of the rollback operation is always logged (except if no logger is available).</p>
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
	 * 
	 * @see #rollback(boolean)
	 */
	protected final void rollback(){
		rollback(true);
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
	 * @param log	<code>true</code> to log the success/failure of the rollback operation,
	 *           	<code>false</code> to be quiet whatever happens.
	 * 
	 * @throws DBException	If it is impossible to rollback a transaction though transactions are supported by this connection..
	 *                    	If these are not supported, this error can never be thrown.
	 * 
	 * @since 2.1
	 */
	protected void rollback(final boolean log){
		try{
			if (supportsTransaction && !connection.getAutoCommit()){
				connection.rollback();
				if (log && logger != null)
					logger.logDB(LogLevel.INFO, this, "ROLLBACK", "Transaction ROLLBACKED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			if (log && logger != null)
				logger.logDB(LogLevel.ERROR, this, "ROLLBACK", "Transaction ROLLBACK impossible!", se);
		}
	}

	/**
	 * <p>End the current transaction.
	 * The success or the failure of the transaction ending operation is always logged (except if no logger is available).</p>
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
	 * 
	 * @see #endTransaction(boolean)
	 */
	protected final void endTransaction(){
		endTransaction(true);
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
	 * @param log	<code>true</code> to log the success/failure of the transaction ending operation,
	 *           	<code>false</code> to be quiet whatever happens.
	 * 
	 * @throws DBException	If it is impossible to end a transaction though transactions are supported by this connection.
	 *                    	If these are not supported, this error can never be thrown.
	 * 
	 * @since 2.1
	 */
	protected void endTransaction(final boolean log){
		try{
			if (supportsTransaction){
				connection.setAutoCommit(true);
				if (log && logger != null)
					logger.logDB(LogLevel.INFO, this, "END_TRANSACTION", "Transaction ENDED.", null);
			}
		}catch(SQLException se){
			supportsTransaction = false;
			if (log && logger != null)
				logger.logDB(LogLevel.ERROR, this, "END_TRANSACTION", "Transaction ENDing impossible!", se);
		}
	}

	/**
	 * <p>Close silently the given {@link ResultSet}.</p>
	 * 
	 * <p>If the given {@link ResultSet} is NULL, nothing (even exception/error) happens.</p>
	 * 
	 * <p>
	 * 	If any {@link SQLException} occurs during this operation, it is caught and just logged
	 * 	(see {@link TAPLog#logDB(uws.service.log.UWSLog.LogLevel, DBConnection, String, String, Throwable)}).
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
			if (logger != null)
				logger.logDB(LogLevel.WARNING, this, "CLOSE", "Can not close a ResultSet!", null);
		}
	}

	/**
	 * <p>Close silently the given {@link Statement}.</p>
	 * 
	 * <p>If the given {@link Statement} is NULL, nothing (even exception/error) happens.</p>
	 * 
	 * <p>
	 * 	The given statement is explicitly canceled by this function before being closed.
	 * 	Thus the corresponding DBMS process is ensured to be stopped. Of course, this
	 * 	cancellation is effective only if this operation is supported by the JDBC driver
	 * 	and the DBMS.
	 * </p>
	 * 
	 * <p><b>Important note:</b>
	 * 	In case of cancellation, <b>NO</b> rollback is performed.
	 * </p>
	 * 
	 * <p>
	 * 	If any {@link SQLException} occurs during this operation, it is caught and just logged
	 * 	(see {@link TAPLog#logDB(uws.service.log.UWSLog.LogLevel, DBConnection, String, String, Throwable)}).
	 * 	No error is thrown and nothing else is done.
	 * </p>
	 * 
	 * @param stmt	{@link Statement} to close.
	 * 
	 * @see #cancel(Statement, boolean)
	 */
	protected final void close(final Statement stmt){
		try{
			if (stmt != null){
				cancel(stmt, false);
				stmt.close();
			}
		}catch(SQLException se){
			if (logger != null)
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
	 * Otherwise the given string is returned as provided.
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
	 * 	(see {@link JDBCTranslator#isCaseSensitive(IdentifierField)}).
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
		if (!supportsSchema || schemaName == null || schemaName.length() == 0)
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
	 * 	the table name will be prefixed by the schema name.
	 * 	The research will then be done with NULL as schema name and this prefixed table name.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	Test on the schema name is done considering the case sensitivity indicated by the translator
	 * 	(see {@link JDBCTranslator#isCaseSensitive(IdentifierField)}).
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
				if (logger != null)
					logger.logDB(LogLevel.ERROR, this, "TABLE_EXIST", "More than one table match to these criteria (schema=" + schemaName + " (case sensitive?" + schemaCaseSensitive + ") && table=" + tableName + " (case sensitive?" + tableCaseSensitive + "))!", null);
				throw new DBException("More than one table match to these criteria (schema=" + schemaName + " (case sensitive?" + schemaCaseSensitive + ") && table=" + tableName + " (case sensitive?" + tableCaseSensitive + "))!");
			}

			return cnt == 1;

		}finally{
			close(rs);
		}
	}

	/**
	 * <p>Tell whether the specified column exists in the specified table of the database.
	 * 	To do so, it is using the given {@link DatabaseMetaData} object to query the database and list all existing columns.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	If schemas are not supported by this connection but a schema name is even though provided in parameter,
	 * 	the table name will be prefixed by the schema name.
	 * 	The research will then be done with NULL as schema name and this prefixed table name.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	Test on the schema name is done considering the case sensitivity indicated by the translator
	 * 	(see {@link JDBCTranslator#isCaseSensitive(IdentifierField)}).
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This function is used by {@link #loadSchemas(TAPTable, TAPMetadata, Statement)}, {@link #loadTables(TAPTable, TAPMetadata, Statement)}
	 * 	and {@link #loadColumns(TAPTable, List, Statement)}.
	 * </i></p>
	 * 
	 * @param schemaName	DB name of the table schema. <i>MAY BE NULL</i>
	 * @param tableName		DB name of the table containing the column to search. <i>MAY BE NULL</i>
	 * @param columnName	DB name of the column to search.
	 * @param dbMeta		Metadata about the database, and mainly the list of all existing tables.
	 * 
	 * @return	<i>true</i> if the specified column exists, <i>false</i> otherwise.
	 * 
	 * @throws SQLException	If any error occurs while interrogating the database about existing columns.
	 */
	protected boolean isColumnExisting(String schemaName, String tableName, String columnName, final DatabaseMetaData dbMeta) throws DBException, SQLException{
		if (columnName == null || columnName.length() == 0)
			return true;

		// Determine the case sensitivity to use for the equality test:
		boolean schemaCaseSensitive = translator.isCaseSensitive(IdentifierField.SCHEMA);
		boolean tableCaseSensitive = translator.isCaseSensitive(IdentifierField.TABLE);
		boolean columnCaseSensitive = translator.isCaseSensitive(IdentifierField.COLUMN);

		ResultSet rsT = null, rsC = null;
		try{
			/* Note:
			 * 
			 *     The DatabaseMetaData.getColumns(....) function does not work properly
			 * with the SQLite driver: when all parameters are set to null, meaning all columns of the database
			 * must be returned, absolutely no rows are selected.
			 * 
			 *     The solution proposed here, is to first search all (matching) tables, and then for each table get
			 * all its columns and find the matching one(s).
			 */

			// List all matching tables:
			if (supportsSchema){
				String schemaPattern = schemaCaseSensitive ? schemaName : null;
				String tablePattern = tableCaseSensitive ? tableName : null;
				rsT = dbMeta.getTables(null, schemaPattern, tablePattern, null);
			}else{
				String tablePattern = tableCaseSensitive ? tableName : null;
				rsT = dbMeta.getTables(null, null, tablePattern, null);
			}

			// For each matching table:
			int cnt = 0;
			String columnPattern = columnCaseSensitive ? columnName : null;
			while(rsT.next()){
				String rsSchema = nullifyIfNeeded(rsT.getString(2));
				String rsTable = rsT.getString(3);
				// test the schema name:
				if (!supportsSchema || schemaName == null || equals(rsSchema, schemaName, schemaCaseSensitive)){
					// test the table name:
					if ((tableName == null || equals(rsTable, tableName, tableCaseSensitive))){
						// list its columns:
						rsC = dbMeta.getColumns(null, rsSchema, rsTable, columnPattern);
						// count all matching columns:
						while(rsC.next()){
							String rsColumn = rsC.getString(4);
							if (equals(rsColumn, columnName, columnCaseSensitive))
								cnt++;
						}
						close(rsC);
					}
				}
			}

			if (cnt > 1){
				if (logger != null)
					logger.logDB(LogLevel.ERROR, this, "COLUMN_EXIST", "More than one column match to these criteria (schema=" + schemaName + " (case sensitive?" + schemaCaseSensitive + ") && table=" + tableName + " (case sensitive?" + tableCaseSensitive + ") && column=" + columnName + " (case sensitive?" + columnCaseSensitive + "))!", null);
				throw new DBException("More than one column match to these criteria (schema=" + schemaName + " (case sensitive?" + schemaCaseSensitive + ") && table=" + tableName + " (case sensitive?" + tableCaseSensitive + ") && column=" + columnName + " (case sensitive?" + columnCaseSensitive + "))!");
			}

			return cnt == 1;

		}finally{
			close(rsT);
			close(rsC);
		}
	}

	/*
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
	 *
	protected String getTablePrefix(final String schemaName){
		if (schemaName != null && schemaName.trim().length() > 0)
			return schemaName + "_";
		else
			return "";
	}*/

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
				if (!isCancelled())
					supportsBatchUpdates = false;
				/*
				 * If the error happens for the first row, it is still possible to insert all rows
				 * with the non-batch function - executeUpdate().
				 * 
				 * Otherwise, it is impossible to insert the previous batched rows ; an exception must be thrown
				 * and must stop the whole TAP_SCHEMA initialization.
				 */
				if (indRow == 1){
					if (!isCancelled() && logger != null)
						logger.logDB(LogLevel.WARNING, this, "EXEC_UPDATE", "BATCH query impossible => TRYING AGAIN IN A NORMAL EXECUTION (executeUpdate())!", se);
				}else{
					if (!isCancelled() && logger != null)
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
				if (logger != null)
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
				if (!isCancelled()){
					supportsBatchUpdates = false;
					if (logger != null)
						logger.logDB(LogLevel.ERROR, this, "EXEC_UPDATE", "BATCH execution impossible!", se);
				}
				throw new DBException("BATCH execution impossible!", se);
			}

			// Remove executed queries from the statement:
			try{
				stmt.clearBatch();
			}catch(SQLException se){
				if (!isCancelled() && logger != null)
					logger.logDB(LogLevel.WARNING, this, "EXEC_UPDATE", "CLEAR BATCH impossible!", se);
			}

			// Count the updated rows:
			int nbRowsUpdated = 0;
			for(int i = 0; i < rows.length; i++)
				nbRowsUpdated += rows[i];

			// Check all given rows have been inserted with success:
			if (nbRowsUpdated != nbRows){
				if (logger != null)
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

	@Override
	public void setFetchSize(final int size){
		supportsFetchSize = true;
		fetchSize = (size > 0) ? size : IGNORE_FETCH_SIZE;
	}
}
