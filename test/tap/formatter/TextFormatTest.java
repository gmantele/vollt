package tap.formatter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import tap.ServiceConnection;
import tap.TAPExecutionReport;
import tap.TAPJob;
import tap.data.ResultSetTableIterator;
import tap.data.TableIterator;
import tap.metadata.TAPColumn;
import tap.parameters.TAPParameters;
import testtools.CommandExecute;
import testtools.DBTools;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;

/**
 * <p>Test the TestFormat function {@link TestFormat#writeResult(TableIterator, OutputStream, TAPExecutionReport, Thread)}.</p>
 * 
 * <p>2 test ares done: 1 with an overflow and another without.</p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (09/2014)
 */
public class TextFormatTest {

	private static Connection conn;
	private static ServiceConnection serviceConn;
	private static TAPColumn[] resultingColumns;
	private static File textFile = new File("/home/gmantele/Desktop/text_test.txt");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		conn = DBTools.createConnection("postgresql", "127.0.0.1", null, "gmantele", "gmantele", "pwd");
		serviceConn = new ServiceConnection4Test();

		resultingColumns = new TAPColumn[4];
		resultingColumns[0] = new TAPColumn("ID", new DBType(DBDatatype.VARCHAR));
		resultingColumns[1] = new TAPColumn("ra", new DBType(DBDatatype.DOUBLE), "Right ascension", "deg", "pos.eq.ra", null);
		resultingColumns[2] = new TAPColumn("deg", new DBType(DBDatatype.DOUBLE), "Declination", "deg", "pos.eq.dec", null);
		resultingColumns[3] = new TAPColumn("gmag", new DBType(DBDatatype.DOUBLE), "G magnitude", "mag", "phot.mag;em.opt.B", null);

		if (!textFile.exists())
			textFile.createNewFile();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{
		DBTools.closeConnection(conn);
		textFile.delete();
	}

	@Test
	public void testWriteResult(){
		ResultSet rs = null;
		try{
			rs = DBTools.select(conn, "SELECT id, ra, deg, gmag FROM gums LIMIT 10;");

			HashMap<String,Object> tapParams = new HashMap<String,Object>(1);
			tapParams.put(TAPJob.PARAM_MAX_REC, "100");
			TAPParameters params = new TAPParameters(serviceConn, tapParams);
			TAPExecutionReport report = new TAPExecutionReport("123456A", true, params);
			report.resultingColumns = resultingColumns;

			TableIterator it = new ResultSetTableIterator(rs);

			TextFormat formatter = new TextFormat(serviceConn);
			OutputStream output = new BufferedOutputStream(new FileOutputStream(textFile));
			formatter.writeResult(it, output, report, Thread.currentThread());
			output.close();

			assertTrue(CommandExecute.execute("wc -l < \"" + textFile.getAbsolutePath() + "\"").trim().equals("12"));

		}catch(Exception t){
			t.printStackTrace();
			fail("Unexpected exception!");
		}finally{
			if (rs != null){
				try{
					rs.close();
				}catch(SQLException se){}
			}
		}
	}

	@Test
	public void testWriteResultWithOverflow(){
		ResultSet rs = null;
		try{
			rs = DBTools.select(conn, "SELECT id, ra, deg, gmag FROM gums LIMIT 10;");

			HashMap<String,Object> tapParams = new HashMap<String,Object>(1);
			tapParams.put(TAPJob.PARAM_MAX_REC, "5");
			TAPParameters params = new TAPParameters(serviceConn, tapParams);
			TAPExecutionReport report = new TAPExecutionReport("123456A", true, params);
			report.resultingColumns = resultingColumns;

			TableIterator it = new ResultSetTableIterator(rs);

			TextFormat formatter = new TextFormat(serviceConn);
			OutputStream output = new BufferedOutputStream(new FileOutputStream(textFile));
			formatter.writeResult(it, output, report, Thread.currentThread());
			output.close();

			assertTrue(CommandExecute.execute("wc -l < \"" + textFile.getAbsolutePath() + "\"").trim().equals("7"));

		}catch(Exception t){
			t.printStackTrace();
			fail("Unexpected exception!");
		}finally{
			if (rs != null){
				try{
					rs.close();
				}catch(SQLException e){
					System.err.println("Can not close the RESULTSET!");
					e.printStackTrace();
				}
			}
		}
	}

}
