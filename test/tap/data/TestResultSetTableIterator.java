package tap.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.GregorianCalendar;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.DBType;
import adql.parser.ADQLParser;
import adql.query.ADQLSet;
import adql.translator.AstroH2Translator;
import tap.db_testtools.DBTools;
import tap.metadata.TAPColumn;

public class TestResultSetTableIterator {

	private static Connection conn;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DBTools.createTestDB();
		conn = DBTools.createConnection("h2", null, null, DBTools.DB_TEST_PATH, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DBTools.closeConnection(conn);
		DBTools.dropTestDB();
	}

	@Test
	public void testWithRSNULL() {
		try {
			new ResultSetTableIterator(null);
			fail("The constructor should have failed, because: the given ResultSet is NULL.");
		} catch(Exception ex) {
			assertEquals("java.lang.NullPointerException", ex.getClass().getName());
			assertEquals("Missing ResultSet object over which to iterate!", ex.getMessage());
		}
	}

	@Test
	public void testWithData() {
		TableIterator it = null;
		try {
			ResultSet rs = DBTools.select(conn, "SELECT hip, ra, dec, vmag FROM hipparcos LIMIT 10;");

			it = new ResultSetTableIterator(rs);
			// TEST there is column metadata before starting the iteration:
			assertTrue(it.getMetadata() != null);
			final int expectedNbLines = 10, expectedNbColumns = 4;
			int countLines = 0, countColumns = 0;
			while(it.nextRow()) {
				// count lines:
				countLines++;
				// reset columns count:
				countColumns = 0;
				while(it.hasNextCol()) {
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

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct ResultSet (containing some valid rows).");
		} finally {
			if (it != null) {
				try {
					it.close();
				} catch(DataReadException dre) {
				}
			}
		}
	}

	@Test
	public void testWithEmptySet() {
		TableIterator it = null;
		try {
			ResultSet rs = DBTools.select(conn, "SELECT * FROM hipparcos WHERE hip = 1056;");

			it = new ResultSetTableIterator(rs);
			// TEST there is column metadata before starting the iteration:
			assertTrue(it.getMetadata() != null);
			int countLines = 0;
			// count lines:
			while(it.nextRow())
				countLines++;
			// TEST that no line has been read:
			assertEquals(countLines, 0);

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct ResultSet (containing some valid rows).");
		} finally {
			if (it != null) {
				try {
					it.close();
				} catch(DataReadException dre) {
				}
			}
		}
	}

	@Test
	public void testWithClosedSet() {
		try {
			// create a valid ResultSet:
			ResultSet rs = DBTools.select(conn, "SELECT * FROM hipparcos WHERE hip = 1056;");

			// close the ResultSet:
			rs.close();

			// TRY to create a TableIterator with a closed ResultSet:
			new ResultSetTableIterator(rs);

			fail("The constructor should have failed, because: the given ResultSet is closed.");
		} catch(Exception ex) {
			assertEquals(ex.getClass().getName(), "tap.data.DataReadException");
		}
	}

	@Test
	public void testDateFormat() {
		ResultSet rs = null;
		try {
			// create a valid ResultSet:
			rs = DBTools.select(conn, "SELECT * FROM hipparcos LIMIT 1;");

			// Create the iterator:
			ResultSetTableIterator rsit = new ResultSetTableIterator(rs);
			assertTrue(rsit.nextRow());
			assertTrue(rsit.hasNextCol());
			rsit.nextCol();

			// Set a date-time:
			GregorianCalendar cal = new GregorianCalendar();
			cal.set(2017, GregorianCalendar.FEBRUARY, 1, 15, 13, 56); // 1st Feb. 2017 - 15:13:56 CET

			// Try to format it from a java.SQL.Timestamp into a ISO8601 date-time:
			assertEquals("2017-02-01T14:13:56Z", rsit.formatColValue(new java.sql.Timestamp(cal.getTimeInMillis())));
			// Try to format it from a java.UTIL.Date into an ISO8601 date-time:
			assertEquals("2017-02-01T14:13:56Z", rsit.formatColValue(cal.getTime()));

			// Try to format it from a java.SQL.Date into a simple date (no time indication):
			assertEquals("2017-02-01", rsit.formatColValue(new java.sql.Date(cal.getTimeInMillis())));

			// Try to format it into a simple time (no date indication):
			assertEquals("15:13:56", rsit.formatColValue(new java.sql.Time(cal.getTimeInMillis())));

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("An exception occurs while formatting dates/times.");
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch(Exception ex) {
				}
			}
		}
	}

	@Test
	public void testGeometryColumns() {
		ResultSet rs = null;
		try {
			ADQLSet query = (new ADQLParser()).parseQuery("SELECT TOP 1 POINT('', ra, dec), CENTROID(CIRCLE('', ra, dec, 2)), BOX('', ra-1, dec-2, ra+1, dec+2), CIRCLE('', ra, dec, 2) FROM hipparcos;");

			// create a valid ResultSet:
			rs = DBTools.select(conn, (new AstroH2Translator()).translate(query));

			// Create the iterator:
			ResultSetTableIterator rsit = new ResultSetTableIterator(rs, query.getResultingColumns());
			assertTrue(rsit.nextRow());

			// Fetch the metadata:
			TAPColumn[] cols = rsit.getMetadata();
			assertEquals(4, cols.length);

			// Check that the two first columns are POINTs:
			for(int i = 0; i < 2; i++)
				assertEquals(DBType.DBDatatype.POINT, cols[i].getDatatype().type);

			// Check that the next columns are REGIONs:
			for(int i = 2; i < 3; i++)
				assertEquals(DBType.DBDatatype.REGION, cols[i].getDatatype().type);

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("An exception occurs while checking geometrical functions datatypes.");
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch(Exception ex) {
				}
			}
		}
	}

	@Test
	public void testSQLFunctions() {
		ResultSet rs = null;
		try {
			ADQLSet query = (new ADQLParser()).parseQuery("SELECT COUNT(*), MIN(vmag), AVG(plx) FROM hipparcos;");

			// create a valid ResultSet:
			rs = DBTools.select(conn, (new AstroH2Translator()).translate(query));

			// Create the iterator:
			ResultSetTableIterator rsit = new ResultSetTableIterator(rs, query.getResultingColumns());
			assertTrue(rsit.nextRow());

			// Fetch the metadata:
			TAPColumn[] cols = rsit.getMetadata();
			assertEquals(3, cols.length);

			// Check that the first column is a BIGINT:
			assertEquals(DBType.DBDatatype.BIGINT, cols[0].getDatatype().type);

			// Check that the two next columns are REAL:
			for(int i = 1; i < 3; i++)
				assertEquals(DBType.DBDatatype.REAL, cols[i].getDatatype().type);

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("An exception occurs while checking SQL functions datatypes");
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch(Exception ex) {
				}
			}
		}
	}
}
