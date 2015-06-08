package testtools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public final class DBTools {

	public static int count = 0;

	public final static void main(final String[] args) throws Throwable{
		for(int i = 0; i < 3; i++){
			Thread t = new Thread(new Runnable(){
				@Override
				public void run(){
					count++;
					try{
						Connection conn = DBTools.createConnection("postgresql", "127.0.0.1", null, "gmantele", "gmantele", "pwd");
						System.out.println("Start - " + count + ": ");
						String query = "SELECT * FROM gums.smc WHERE magg BETWEEN " + (15 + count) + " AND " + (20 + count) + ";";
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

	public final static HashMap<String,String> VALUE_JDBC_DRIVERS = new HashMap<String,String>(4);
	static{
		VALUE_JDBC_DRIVERS.put("oracle", "oracle.jdbc.OracleDriver");
		VALUE_JDBC_DRIVERS.put("postgresql", "org.postgresql.Driver");
		VALUE_JDBC_DRIVERS.put("mysql", "com.mysql.jdbc.Driver");
		VALUE_JDBC_DRIVERS.put("sqlite", "org.sqlite.JDBC");
	}

	private DBTools(){}

	public final static Connection createConnection(String dbms, final String server, final String port, final String dbName, final String user, final String passwd) throws DBToolsException{
		// 1. Resolve the DBMS and get its JDBC driver:
		if (dbms == null)
			throw new DBToolsException("Missing DBMS (expected: oracle, postgresql, mysql or sqlite)!");
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
