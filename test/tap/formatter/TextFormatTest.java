package tap.formatter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import tap.ServiceConnection;
import tap.TAPExecutionReport;
import tap.TAPFactory;
import tap.TAPJob;
import tap.data.ResultSetTableIterator;
import tap.data.TableIterator;
import tap.file.TAPFileManager;
import tap.log.TAPLog;
import tap.metadata.TAPColumn;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPType;
import tap.metadata.TAPType.TAPDatatype;
import tap.parameters.TAPParameters;
import testtools.DBTools;
import uws.service.UserIdentifier;

/**
 * <p>Test the TestFormat function {@link TestFormat#writeResult(TableIterator, OutputStream, TAPExecutionReport, Thread)}.</p>
 * 
 * <p>2 test ares done: 1 with an overflow and another without.</p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (07/2014)
 */
public class TextFormatTest {

	private static Connection conn;
	private static ServiceConnection serviceConn;
	private static TAPColumn[] resultingColumns;
	private static File textFile = new File("/home/gmantele/Desktop/text_test.txt");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		conn = DBTools.createConnection("postgresql", "127.0.0.1", null, "gmantele", "gmantele", "pwd");
		serviceConn = new ServiceConnectionTest();

		resultingColumns = new TAPColumn[4];
		resultingColumns[0] = new TAPColumn("ID", new TAPType(TAPDatatype.VARCHAR));
		resultingColumns[1] = new TAPColumn("ra", new TAPType(TAPDatatype.DOUBLE), "Right ascension", "deg", "pos.eq.ra", null);
		resultingColumns[2] = new TAPColumn("deg", new TAPType(TAPDatatype.DOUBLE), "Declination", "deg", "pos.eq.dec", null);
		resultingColumns[3] = new TAPColumn("gmag", new TAPType(TAPDatatype.DOUBLE), "G magnitude", "mag", "phot.mag;em.opt.B", null);

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

			String[] cmd = new String[]{"/bin/sh","-c","wc -l < \"" + textFile.getAbsolutePath() + "\""};
			assertTrue(executeCommand(cmd).trim().equals("12"));

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

			String[] cmd = new String[]{"/bin/sh","-c","wc -l < \"" + textFile.getAbsolutePath() + "\""};
			assertTrue(executeCommand(cmd).trim().equals("7"));

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

	private String executeCommand(String[] command){

		StringBuffer output = new StringBuffer();

		Process p;
		try{
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while((line = reader.readLine()) != null){
				output.append(line + "\n");
			}

		}catch(Exception e){
			e.printStackTrace();
		}

		return output.toString();

	}

	private static class ServiceConnectionTest implements ServiceConnection {

		@Override
		public int[] getOutputLimit(){
			return new int[]{1000000,1000000};
		}

		@Override
		public LimitUnit[] getOutputLimitType(){
			return new LimitUnit[]{LimitUnit.bytes,LimitUnit.bytes};
		}

		@Override
		public String getProviderName(){
			return null;
		}

		@Override
		public String getProviderDescription(){
			return null;
		}

		@Override
		public boolean isAvailable(){
			return true;
		}

		@Override
		public String getAvailability(){
			return "AVAILABLE";
		}

		@Override
		public int[] getRetentionPeriod(){
			return null;
		}

		@Override
		public int[] getExecutionDuration(){
			return null;
		}

		@Override
		public UserIdentifier getUserIdentifier(){
			return null;
		}

		@Override
		public boolean uploadEnabled(){
			return false;
		}

		@Override
		public int[] getUploadLimit(){
			return null;
		}

		@Override
		public LimitUnit[] getUploadLimitType(){
			return null;
		}

		@Override
		public int getMaxUploadSize(){
			return 0;
		}

		@Override
		public TAPMetadata getTAPMetadata(){
			return null;
		}

		@Override
		public Collection<String> getCoordinateSystems(){
			return null;
		}

		@Override
		public TAPLog getLogger(){
			return null;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public TAPFactory getFactory(){
			return null;
		}

		@Override
		public TAPFileManager getFileManager(){
			return null;
		}

		@Override
		public Iterator<OutputFormat> getOutputFormats(){
			return null;
		}

		@Override
		public OutputFormat getOutputFormat(String mimeOrAlias){
			return null;
		}

	}

}
