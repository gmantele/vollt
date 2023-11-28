package tap.db_testtools;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.h2.tools.RunScript;

public final class DBTools {

	public static int count = 0;

	public final static void main(final String[] args) throws Throwable{
		try{
			createTestDB();
			for(int i = 0; i < 3; i++){
				Thread t = new Thread(new Runnable(){
					@Override
					public void run(){
						count++;
						try{
							Connection conn = DBTools.createConnection("h2", null, null, DBTools.DB_TEST_PATH, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD);
							System.out.println("Start - " + count + ": ");
							String query = "SELECT * FROM hipparcos WHERE vmag BETWEEN " + (5 + count) + " AND " + (10 + count) + ";";
							System.out.println(query);
							ResultSet rs = DBTools.select(conn, query);
							try{
								rs.last();
								System.out.println("Nb rows: " + rs.getRow());
							}catch(SQLException e){
								e.printStackTrace();
							}
							if (DBTools.closeConnection(conn))
								System.out.println("[DEBUG] Connection closed!");
						}catch(DBToolsException e){
							e.printStackTrace();
						}
						System.out.println("End - " + count);
						count--;
					}
				});
				t.start();
			}
		}finally{
			dropTestDB();
		}
	}

	public static class DBToolsException extends Exception {

		private static final long serialVersionUID = 1L;

		public DBToolsException(){
			super();
		}

		public DBToolsException(String message, Throwable cause){
			super(message, cause);
		}

		public DBToolsException(String message){
			super(message);
		}

		public DBToolsException(Throwable cause){
			super(cause);
		}

	}

	/* ********************************************************************* */
	/* H2 TEST DATABASE ATTRIBUTES AND FUNCTIONS                             */

	public static String DB_TEST_JDBC_DRIVER = "org.h2.Driver";
	public static String DB_TEST_PATH = "./test/tap/db_testtools/db-test/db-test";
	public static String DB_TEST_URL = "jdbc:h2:" + DB_TEST_PATH;
	public static String DB_TEST_USER = "junit";
	public static String DB_TEST_PWD = "super-pwd";
	public static String DB_TEST_TRANSLATOR = "adql.translator.H2Translator";
	public static String DB_TEST_SCRIPTS_DIR = "./test/tap/db_testtools/db-test/";

	public static void createTestDB() throws SQLException{
		createTestDB(true);
	}

	public static void createTestDB(final boolean dropFirstIfExists) throws SQLException{
		if (dropFirstIfExists)
			dropTestDB();
		RunScript.execute(DBTools.DB_TEST_URL, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD, DBTools.DB_TEST_SCRIPTS_DIR + "create-db.sql", null, false);
	}

	public static void createAddTAPSchema() throws SQLException{
		RunScript.execute(DBTools.DB_TEST_URL, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD, DBTools.DB_TEST_SCRIPTS_DIR + "create-tap_schema.sql", null, false);
		RunScript.execute(DBTools.DB_TEST_URL, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD, DBTools.DB_TEST_SCRIPTS_DIR + "fill-tap_schema.sql", null, false);
	}

	public static void dropTestDB(){
		// Delete the Database:
		File f = new File(DB_TEST_PATH + ".mv.db");
		if (f.exists())
			f.delete();

		// Delete its corresponding H2 error log:
		f = new File(DB_TEST_PATH + ".trace.db");
		if (f.exists())
			f.delete();
	}

	/* ********************************************************************* */
	/* DATABASE CONNECTION FUNCTIONS                                         */

	public final static HashMap<String,String> VALUE_JDBC_DRIVERS = new HashMap<String,String>(4);
	static{
		VALUE_JDBC_DRIVERS.put("oracle", "oracle.jdbc.OracleDriver");
		VALUE_JDBC_DRIVERS.put("postgresql", "org.postgresql.Driver");
		VALUE_JDBC_DRIVERS.put("mysql", "com.mysql.jdbc.Driver");
		VALUE_JDBC_DRIVERS.put("sqlite", "org.sqlite.JDBC");
		VALUE_JDBC_DRIVERS.put("h2", "org.h2.Driver");
	}

	private DBTools(){}

	public final static Connection createConnection(String dbms, final String server, final String port, final String dbName, final String user, final String passwd) throws DBToolsException{
		// 1. Resolve the DBMS and get its JDBC driver:
		if (dbms == null)
			throw new DBToolsException("Missing DBMS (expected: oracle, postgresql, mysql, sqlite or h2)!");
		dbms = dbms.toLowerCase();
		String jdbcDriver = VALUE_JDBC_DRIVERS.get(dbms);
		if (jdbcDriver == null)
			throw new DBToolsException("Unknown DBMS (\"" + dbms + "\")!");

		// 2. Load the JDBC driver:
		try{
			Class.forName(jdbcDriver);
		}catch(ClassNotFoundException e){
			throw new DBToolsException("Impossible to load the JDBC driver: " + e.getMessage(), e);
		}

		// 3. Establish the connection:
		Connection connection = null;
		try{
			connection = DriverManager.getConnection("jdbc:" + dbms + ":" + ((server != null && server.trim().length() > 0) ? "//" + server + ((port != null && port.trim().length() > 0) ? (":" + port) : "") + "/" : "") + dbName, user, passwd);
		}catch(SQLException e){
			throw new DBToolsException("Connection failed: " + e.getMessage(), e);
		}

		if (connection == null)
			throw new DBToolsException("Failed to make connection!");

		return connection;
	}

	public final static boolean closeConnection(final Connection conn) throws DBToolsException{
		try{
			if (conn != null && !conn.isClosed()){
				conn.close();
				try{
					Thread.sleep(200);
				}catch(InterruptedException e){
					System.err.println("WARNING: can't wait/sleep before testing the connection close status! [" + e.getMessage() + "]");
				}
				return conn.isClosed();
			}else
				return true;
		}catch(SQLException e){
			throw new DBToolsException("Closing connection failed: " + e.getMessage(), e);
		}
	}

	public final static ResultSet select(final Connection conn, final String selectQuery) throws DBToolsException{
		if (conn == null || selectQuery == null || selectQuery.trim().length() == 0)
			throw new DBToolsException("One parameter is missing!");

		try{
			Statement stmt = conn.createStatement();
			return stmt.executeQuery(selectQuery);
		}catch(SQLException e){
			throw new DBToolsException("Can't execute the given SQL query: " + e.getMessage(), e);
		}
	}

}
