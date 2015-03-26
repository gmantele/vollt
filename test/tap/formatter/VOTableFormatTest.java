package tap.formatter;

import static org.junit.Assert.assertEquals;
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
import uk.ac.starlink.votable.DataFormat;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;

/**
 * <p>Test the VOTableFormat function {@link VOTableFormat#writeResult(TableIterator, OutputStream, TAPExecutionReport, Thread)}.</p>
 * 
 * <p>2 test ares done: 1 with an overflow and another without.</p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (09/2014)
 */
public class VOTableFormatTest {

	private static Connection conn;
	private static ServiceConnection serviceConn;
	private static TAPColumn[] resultingColumns;
	private static File votableFile = new File("/home/gmantele/Desktop/votable_test.xml");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		conn = DBTools.createConnection("postgresql", "127.0.0.1", null, "gmantele", "gmantele", "pwd");
		serviceConn = new ServiceConnection4Test();

		resultingColumns = new TAPColumn[4];
		resultingColumns[0] = new TAPColumn("ID", new DBType(DBDatatype.VARCHAR));
		resultingColumns[1] = new TAPColumn("ra", new DBType(DBDatatype.DOUBLE), "Right ascension", "deg", "pos.eq.ra", null);
		resultingColumns[2] = new TAPColumn("deg", new DBType(DBDatatype.DOUBLE), "Declination", "deg", "pos.eq.dec", null);
		resultingColumns[3] = new TAPColumn("gmag", new DBType(DBDatatype.DOUBLE), "G magnitude", "mag", "phot.mag;em.opt.B", null);

		if (!votableFile.exists())
			votableFile.createNewFile();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{
		DBTools.closeConnection(conn);
		votableFile.delete();
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

			VOTableFormat formatter = new VOTableFormat(serviceConn, DataFormat.TABLEDATA);
			OutputStream output = new BufferedOutputStream(new FileOutputStream(votableFile));
			formatter.writeResult(it, output, report, Thread.currentThread());
			output.close();

			// note: due to the pipe (|), we must call /bin/sh as a command whose the command to execute in is the "grep ... | wc -l":
			assertEquals("10", CommandExecute.execute("grep \"<TR>\" \"" + votableFile.getAbsolutePath() + "\" | wc -l").trim());
			assertEquals("0", CommandExecute.execute("grep \"<INFO name=\\\"QUERY_STATUS\\\" value=\\\"OVERFLOW\\\"/>\" \"" + votableFile.getAbsolutePath() + "\" | wc -l").trim());

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

			VOTableFormat formatter = new VOTableFormat(serviceConn, DataFormat.TABLEDATA);
			OutputStream output = new BufferedOutputStream(new FileOutputStream(votableFile));
			formatter.writeResult(it, output, report, Thread.currentThread());
			output.close();

			// note: due to the pipe (|), we must call /bin/sh as a command whose the command to execute in is the "grep ... | wc -l":
			assertEquals("5", CommandExecute.execute("grep \"<TR>\" \"" + votableFile.getAbsolutePath() + "\" | wc -l").trim());
			assertEquals("1", CommandExecute.execute("grep \"<INFO name=\\\"QUERY_STATUS\\\" value=\\\"OVERFLOW\\\"/>\" \"" + votableFile.getAbsolutePath() + "\" | wc -l").trim());

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
