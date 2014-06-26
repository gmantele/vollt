package tap.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import testtools.DBTools;

public class ResultSetTableIteratorTest {

	private static Connection conn;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		conn = DBTools.createConnection("postgresql", "127.0.0.1", null, "gmantele", "gmantele", "pwd");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{
		DBTools.closeConnection(conn);
	}

	@Test
	public void testWithRSNULL(){
		try{
			new ResultSetTableIterator(null);
			fail("The constructor should have failed, because: the given ResultSet is NULL.");
		}catch(Exception ex){
			assertEquals(ex.getClass().getName(), "java.lang.NullPointerException");
		}
	}

	@Test
	public void testWithData(){
		try{
			ResultSet rs = DBTools.select(conn, "SELECT id, ra, deg, gmag FROM gums LIMIT 10;");

			TableIterator it = new ResultSetTableIterator(rs);
			final int expectedNbLines = 10, expectedNbColumns = 4;
			int countLines = 0, countColumns = 0;
			while(it.nextRow()){
				// count lines:
				countLines++;
				// reset columns count:
				countColumns = 0;
				while(it.hasNextCol()){
					it.nextCol();
					// count columns
					countColumns++;
					// TEST the column type is set (not null):
					assertTrue(it.getColType() != null);
				}
				// TEST that all columns have been read:
				assertEquals(expectedNbColumns, countColumns);
			}
			// TEST that all lines have been read: 
			assertEquals(expectedNbLines, countLines);

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct ResultSet (containing some valid rows).");
		}
	}

	@Test
	public void testWithEmptySet(){
		try{
			ResultSet rs = DBTools.select(conn, "SELECT * FROM gums WHERE id = 'foo';");

			TableIterator it = new ResultSetTableIterator(rs);
			int countLines = 0;
			// count lines:
			while(it.nextRow())
				countLines++;
			// TEST that no line has been read: 
			assertEquals(countLines, 0);

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct ResultSet (containing some valid rows).");
		}
	}

	@Test
	public void testWithClosedSet(){
		try{
			// create a valid ResultSet:
			ResultSet rs = DBTools.select(conn, "SELECT * FROM gums WHERE id = 'foo';");

			// close the ResultSet:
			rs.close();

			// TRY to create a TableIterator with a closed ResultSet:
			new ResultSetTableIterator(rs);

			fail("The constructor should have failed, because: the given ResultSet is closed.");
		}catch(Exception ex){
			assertEquals(ex.getClass().getName(), "tap.data.DataReadException");
		}
	}
}
