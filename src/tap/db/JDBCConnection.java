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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Iterator;

import cds.savot.model.SavotTR;
import cds.savot.model.TDSet;

import tap.log.TAPLog;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;

import adql.query.ADQLQuery;

/**
 * Simple implementation of the {@link DBConnection} interface.
 * It creates and manages a JDBC connection to a specified database.
 * Thus results of any executed SQL query will be a {@link ResultSet}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public class JDBCConnection implements DBConnection<ResultSet> {

	/** JDBC prefix of any database URL (for instance: jdbc:postgresql://127.0.0.1/myDB). */
	public final static String JDBC_PREFIX = "jdbc";

	/** Connection ID (typically, the job ID). */
	protected final String ID;

	/** JDBC connection (created and initialized at the creation of this {@link JDBCConnection} instance). */
	protected final Connection connection;

	/** Logger to use if any message needs to be printed to the server manager. */
	protected final TAPLog logger;

	/**
	 * <p>
	 * 	Creates a JDBC connection to the specified database and with the specified JDBC driver.
	 * 	This connection is established using the given user name and password.
	 * <p>
	 * <p><i><u>note:</u> the JDBC driver is loaded using <pre>Class.forName(driverPath)</pre>.</i></p>
	 * 
	 * @param driverPath	Full class name of the JDBC driver.
	 * @param dbUrl			URL to the database. <i><u>note</u> This URL may not be prefixed by "jdbc:". If not, the prefix will be automatically added.</i>
	 * @param dbUser		Name of the database user (supposed to be the database owner).
	 * @param dbPassword	Password of the given database user.
	 * @param logger		Logger to use if any message needs to be printed to the server admin.
	 * 
	 * @throws DBException	If the specified driver can not be found, or if the database URL or user is incorrect.
	 */
	public JDBCConnection(final String ID, final String driverPath, final String dbUrl, final String dbUser, final String dbPassword, final TAPLog logger) throws DBException {
		this.logger = logger;
		this.ID = ID;

		// Load the specified JDBC driver:
		try {
			Class.forName(driverPath);
		} catch (ClassNotFoundException cnfe) {
			logger.dbError("Impossible to find the JDBC driver \""+driverPath+"\" !", cnfe);
			throw new DBException("Impossible to find the JDBC driver \""+driverPath+"\" !", cnfe);
		}

		// Build a connection to the specified database:
		String url = dbUrl.startsWith(JDBC_PREFIX) ? dbUrl : (JDBC_PREFIX+dbUrl);
		try {
			connection = DriverManager.getConnection(url, dbUser, dbPassword);
			logger.connectionOpened(this, dbUrl.substring(dbUrl.lastIndexOf('/')));
		} catch (SQLException se) {
			logger.dbError("Impossible to establish a connection to the database \""+url+"\" !", se);
			throw new DBException("Impossible to establish a connection to the database \""+url+"\" !", se);
		}
	}

	public final String getID() {
		return ID;
	}

	public void startTransaction() throws DBException {
		try{
			Statement st = connection.createStatement();
			st.execute("begin");
			logger.transactionStarted(this);
		}catch(SQLException se){
			logger.dbError("Impossible to begin a transaction !", se);
			throw new DBException("Impossible to begin a transaction !", se);
		}
	}

	public void cancelTransaction() throws DBException {
		try {
			connection.rollback();
			logger.transactionCancelled(this);
		} catch (SQLException se) {
			logger.dbError("Impossible to cancel/rollback a transaction !", se);
			throw new DBException("Impossible to cancel (rollback) the transaction !", se);
		}
	}

	public void endTransaction() throws DBException {
		try {
			connection.commit();
			logger.transactionEnded(this);
		} catch (SQLException se) {
			logger.dbError("Impossible to end/commit a transaction !", se);
			throw new DBException("Impossible to end/commit the transaction !", se);
		}
	}

	public void close() throws DBException {
		try {
			connection.close();
			logger.connectionClosed(this);
		} catch (SQLException se) {
			logger.dbError("Impossible to close a database transaction !", se);
			throw new DBException("Impossible to close the database transaction !", se);
		}
	}

	/* ********************* */
	/* INTERROGATION METHODS */
	/* ********************* */
	public ResultSet executeQuery(final String sqlQuery, final ADQLQuery adqlQuery) throws DBException {
		try{
			Statement stmt = connection.createStatement();
			logger.sqlQueryExecuting(this, sqlQuery);
			ResultSet result = stmt.executeQuery(sqlQuery);
			logger.sqlQueryExecuted(this, sqlQuery);
			return result;
		}catch(SQLException se){
			logger.sqlQueryError(this, sqlQuery, se);
			throw new DBException("Unexpected error while executing a SQL query: "+se.getMessage(), se);
		}
	}

	/* ************** */
	/* UPLOAD METHODS */
	/* ************** */
	public void createSchema(final String schemaName) throws DBException {
		String sql = "CREATE SCHEMA "+schemaName+";";
		try{
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(sql);
			logger.schemaCreated(this, schemaName);
		}catch(SQLException se){
			logger.dbError("Impossible to create the schema \""+schemaName+"\" !", se);
			throw new DBException("Impossible to create the schema \""+schemaName+"\" !", se);
		}
	}

	public void dropSchema(final String schemaName) throws DBException {
		String sql = "DROP SCHEMA IF EXISTS "+schemaName+" CASCADE;";
		try{
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(sql);
			logger.schemaDropped(this, schemaName);
		}catch(SQLException se){
			logger.dbError("Impossible to drop the schema \""+schemaName+"\" !", se);
			throw new DBException("Impossible to drop the schema \""+schemaName+"\" !", se);
		}
	}

	public void createTable(final TAPTable table) throws DBException {
		// Build the SQL query:
		StringBuffer sqlBuf = new StringBuffer();
		sqlBuf.append("CREATE TABLE ").append(table.getDBSchemaName()).append('.').append(table.getDBName()).append("(");
		Iterator<TAPColumn> it = table.getColumns();
		while(it.hasNext()){
			TAPColumn col = it.next();
			sqlBuf.append('"').append(col.getDBName()).append("\" ").append(' ').append(getDBType(col.getDatatype(), col.getArraySize(), logger));
			if (it.hasNext())
				sqlBuf.append(',');
		}
		sqlBuf.append(");");

		// Execute the creation query:
		String sql = sqlBuf.toString();
		try{
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(sql);
			logger.tableCreated(this, table);
		}catch(SQLException se){
			logger.dbError("Impossible to create the table \""+table.getFullName()+"\" !", se);
			throw new DBException("Impossible to create the table \""+table.getFullName()+"\" !", se);
		}
	}

	/**
	 * Gets the database type corresponding to the given {@link TAPColumn} type.
	 * 
	 * @param datatype		Column datatype (short, int, long, float, double, boolea, char or unsignedByte).
	 * @param arraysize		Size of the array type (1 if not an array, a value &gt; 1 for an array).
	 * @param logger		Object to use to print warnings (for instance, if a given datatype is unknown).
	 * 
	 * @return				The corresponding database type or the given datatype if unknown.
	 */
	public static String getDBType(String datatype, final int arraysize, final TAPLog logger) {
		datatype = (datatype == null)?null:datatype.trim().toLowerCase();

		if (datatype == null || datatype.isEmpty()){
			if (logger != null)
				logger.warning("undefined datatype => considered as VARCHAR !");
			return "VARCHAR";
		}

		if (datatype.equals("short"))
			return (arraysize==1)?"INT2":"BYTEA";
		else if (datatype.equals("int"))
			return (arraysize==1)?"INT4":"BYTEA";
		else if (datatype.equals("long"))
			return (arraysize==1)?"INT8":"BYTEA";
		else if (datatype.equals("float"))
			return (arraysize==1)?"FLOAT4":"BYTEA";
		else if (datatype.equals("double"))
			return (arraysize==1)?"FLOAT8":"BYTEA";
		else if (datatype.equals("boolean"))
			return (arraysize==1)?"BOOL":"BYTEA";
		else if (datatype.equals("char"))
			return (arraysize==1)?"CHAR(1)":((arraysize<=0)?"VARCHAR":("VARCHAR("+arraysize+")"));
		else if (datatype.equals("unsignedbyte"))
			return "BYTEA";
		else{
			if (logger != null)
				logger.dbInfo("Warning: unknown datatype: \""+datatype+"\" => considered as \""+datatype+"\" !");
			return datatype;
		}
	}

	public void dropTable(final TAPTable table) throws DBException {
		String sql = "DROP TABLE "+table.getDBSchemaName()+"."+table.getDBName()+";";
		try{
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(sql);
			logger.tableDropped(this, table);
		}catch(SQLException se){
			logger.dbError("Impossible to drop the table \""+table.getFullName()+"\" !", se);
			throw new DBException("Impossible to drop the table \""+table.getFullName()+"\" !", se);
		}
	}

	public void insertRow(final SavotTR row, final TAPTable table) throws DBException {
		StringBuffer sql = new StringBuffer("INSERT INTO ");
		sql.append(table.getDBSchemaName()).append('.').append(table.getDBName()).append(" VALUES (");

		TDSet cells = row.getTDs();
		Iterator<TAPColumn> it = table.getColumns();
		String datatype, value;
		TAPColumn col;
		int i=0;
		while(it.hasNext()){
			col = it.next();
			if (i>0) sql.append(',');
			datatype = col.getDatatype();
			value = cells.getContent(i);
			if (value == null || value.isEmpty())
				sql.append("NULL");
			else if (datatype.equalsIgnoreCase("char") || datatype.equalsIgnoreCase("varchar") || datatype.equalsIgnoreCase("unsignedByte"))
				sql.append('\'').append(value.replaceAll("'", "''").replaceAll("\0", "")).append('\'');
			else{
				if (value.equalsIgnoreCase("nan"))
					sql.append("'NaN'");
				else
					sql.append(value.replaceAll("\0", ""));
			}
			i++;
		}
		sql.append(");");

		try{
			Statement stmt = connection.createStatement();
			int nbInsertedRows = stmt.executeUpdate(sql.toString());
			logger.rowsInserted(this, table, nbInsertedRows);
		}catch(SQLException se){
			logger.dbError("Impossible to insert a row into the table \""+table.getFullName()+"\" !", se);
			throw new DBException("Impossible to insert a row in the table \""+table.getFullName()+"\" !", se);
		}
	}
}
