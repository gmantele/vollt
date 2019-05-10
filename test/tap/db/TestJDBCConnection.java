package tap.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.parser.ADQLParser;
import adql.parser.ADQLParserFactory;
import adql.parser.ParseException;
import adql.query.ADQLQuery;
import adql.query.IdentifierField;
import adql.translator.AstroH2Translator;
import adql.translator.PostgreSQLTranslator;
import tap.data.DataReadException;
import tap.data.TableIterator;
import tap.data.VOTableIterator;
import tap.db_testtools.DBTools;
import tap.metadata.TAPColumn;
import tap.metadata.TAPForeignKey;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPMetadata.STDSchema;
import tap.metadata.TAPMetadata.STDTable;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;

public class TestJDBCConnection {

	private static Connection h2Connection;
	private static JDBCConnection h2JDBCConnection;
	private static JDBCConnection sensH2JDBCConnection;

	private static Connection sqliteConnection;
	private static JDBCConnection sqliteJDBCConnection;
	private static JDBCConnection sensSqliteJDBCConnection;

	private static String uploadExamplePath;

	private final static String sqliteDbFile = "./test/tap/db_testtools/db-test/sqlite_testDB.db";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		uploadExamplePath = "./test/tap/db/upload_example.vot";

		DBTools.createTestDB();
		h2Connection = DBTools.createConnection("h2", null, null, DBTools.DB_TEST_PATH, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD);
		h2JDBCConnection = new JDBCConnection(h2Connection, new AstroH2Translator(false), "H2", null);
		sensH2JDBCConnection = new JDBCConnection(h2Connection, new AstroH2Translator(true, true, true, true), "SensitiveH2", null);

		sqliteConnection = DBTools.createConnection("sqlite", null, null, sqliteDbFile, null, null);
		sqliteJDBCConnection = new JDBCConnection(sqliteConnection, new PostgreSQLTranslator(false), "SQLITE", null);
		sensSqliteJDBCConnection = new JDBCConnection(sqliteConnection, new PostgreSQLTranslator(true), "SensitiveSQLite", null);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Drop the H2 database:
		DBTools.dropTestDB();

		// Drop the SQLite database:
		(new File(sqliteDbFile)).delete();
	}

	/* ***** */
	/* TESTS */
	/* ***** */

	@Test
	public void testGetTAPSchemaTablesDef() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {
			TAPMetadata meta = createCustomSchema();
			TAPTable customColumns = meta.getTable(STDSchema.TAPSCHEMA.toString(), STDTable.COLUMNS.toString());
			TAPTable[] tapTables = conn.mergeTAPSchemaDefs(meta);
			TAPSchema stdSchema = TAPMetadata.getStdSchema(conn.supportsSchema);
			assertEquals(5, tapTables.length);
			assertTrue(equals(tapTables[0], stdSchema.getTable(STDTable.SCHEMAS.label)));
			assertEquals(customColumns.getSchema(), tapTables[0].getSchema());
			assertTrue(equals(tapTables[1], stdSchema.getTable(STDTable.TABLES.label)));
			assertEquals(customColumns.getSchema(), tapTables[1].getSchema());
			assertTrue(equals(tapTables[2], customColumns));
			assertTrue(equals(tapTables[3], stdSchema.getTable(STDTable.KEYS.label)));
			assertEquals(customColumns.getSchema(), tapTables[3].getSchema());
			assertTrue(equals(tapTables[4], stdSchema.getTable(STDTable.KEY_COLUMNS.label)));
			assertEquals(customColumns.getSchema(), tapTables[4].getSchema());
		}
	}

	@Test
	public void testSetTAPSchema() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {
			short cnt = -1;
			while(cnt < 1) {
				/* NO CUSTOM DEFINITION */
				// Prepare the test:
				if (cnt == -1)
					dropSchema(STDSchema.TAPSCHEMA.label, conn);
				else
					createTAPSchema(conn);
				// Do the test:
				try {
					TAPMetadata meta = new TAPMetadata();
					int[] expectedCounts = getStats(meta);
					conn.setTAPSchema(meta);
					int[] effectiveCounts = getStats(conn, meta);
					for(int i = 0; i < expectedCounts.length; i++)
						assertEquals(expectedCounts[i], effectiveCounts[i]);
				} catch(DBException dbe) {
					dbe.printStackTrace(System.err);
					fail("[" + conn.getID() + ";no def] No error should happen here ; when an empty list of metadata is given, at least the TAP_SCHEMA should be created and filled with a description of itself.");
				}

				/* CUSTOM DEFINITION */
				// Prepare the test:
				if (cnt == -1)
					dropSchema(STDSchema.TAPSCHEMA.label, conn);
				// Do the test:
				try {
					TAPMetadata meta = createCustomSchema();
					int[] expectedCounts = getStats(meta);
					conn.setTAPSchema(meta);
					int[] effectiveCounts = getStats(conn, meta);
					for(int i = 0; i < expectedCounts.length; i++)
						assertEquals(expectedCounts[i], effectiveCounts[i]);
				} catch(DBException dbe) {
					dbe.printStackTrace(System.err);
					fail("[" + conn.getID() + ";custom def] No error should happen here!");
				}

				cnt++;
			}
		}
	}

	@Test
	public void testGetCreationOrder() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {
			assertEquals(-1, conn.getCreationOrder(null));
			assertEquals(0, conn.getCreationOrder(STDTable.SCHEMAS));
			assertEquals(1, conn.getCreationOrder(STDTable.TABLES));
			assertEquals(2, conn.getCreationOrder(STDTable.COLUMNS));
			assertEquals(3, conn.getCreationOrder(STDTable.KEYS));
			assertEquals(4, conn.getCreationOrder(STDTable.KEY_COLUMNS));
		}
	}

	@Test
	public void testGetDBMSDatatype() {
		assertEquals("VARCHAR", h2JDBCConnection.defaultTypeConversion(null));
		assertEquals("TEXT", sqliteJDBCConnection.defaultTypeConversion(null));

		assertEquals("VARBINARY", h2JDBCConnection.defaultTypeConversion(new DBType(DBDatatype.VARBINARY)));
		assertEquals("BLOB", sqliteJDBCConnection.defaultTypeConversion(new DBType(DBDatatype.VARBINARY)));
	}

	@Test
	public void testMergeTAPSchemaDefs() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {

			// TEST WITH NO METADATA OBJECT:
			// -> expected: throws a NULL exception.
			try {
				conn.mergeTAPSchemaDefs(null);
			} catch(Exception e) {
				assertEquals(NullPointerException.class, e.getClass());
			}

			// TEST WITH EMPTY METADATA OBJECT:
			// -> expected: returns at least the 5 tables of the TAP_SCHEMA.
			TAPTable[] stdTables = conn.mergeTAPSchemaDefs(new TAPMetadata());

			assertEquals(5, stdTables.length);

			for(TAPTable t : stdTables)
				assertEquals(STDSchema.TAPSCHEMA.toString(), t.getADQLSchemaName());

			assertEquals(STDTable.SCHEMAS.toString(), stdTables[0].getADQLName());
			assertEquals(STDTable.TABLES.toString(), stdTables[1].getADQLName());
			assertEquals(STDTable.COLUMNS.toString(), stdTables[2].getADQLName());
			assertEquals(STDTable.KEYS.toString(), stdTables[3].getADQLName());
			assertEquals(STDTable.KEY_COLUMNS.toString(), stdTables[4].getADQLName());

			// TEST WITH INCOMPLETE TAP_SCHEMA TABLES LIST + 1 CUSTOM TAP_SCHEMA TABLE (here: TAP_SCHEMA.columns):
			// -> expected: the 5 tables of the TAP_SCHEMA including the modification of the standard tables & ignore the additional table(s) if any (which is the case here).
			TAPMetadata customMeta = createCustomSchema();
			stdTables = conn.mergeTAPSchemaDefs(customMeta);

			assertEquals(5, stdTables.length);

			for(TAPTable t : stdTables)
				assertEquals(STDSchema.TAPSCHEMA.toString(), t.getADQLSchemaName());

			assertEquals(STDTable.SCHEMAS.toString(), stdTables[0].getADQLName());
			assertEquals(STDTable.TABLES.toString(), stdTables[1].getADQLName());
			assertEquals(STDTable.COLUMNS.toString(), stdTables[2].getADQLName());
			assertEquals("Columns", stdTables[2].getDBName());
			assertNotNull(stdTables[2].getColumn("TestNewColumn"));
			assertEquals(STDTable.KEYS.toString(), stdTables[3].getADQLName());
			assertEquals(STDTable.KEY_COLUMNS.toString(), stdTables[4].getADQLName());
		}
	}

	@Test
	public void testEquals() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {
			// NULL tests:
			assertFalse(conn.equals("tap_schema", null, false));
			assertFalse(conn.equals("tap_schema", null, true));
			assertFalse(conn.equals(null, "tap_schema", false));
			assertFalse(conn.equals(null, "tap_schema", true));
			assertFalse(conn.equals(null, null, false));
			assertFalse(conn.equals(null, null, true));

			// CASE SENSITIVE tests:
			if (conn.supportsMixedCaseQuotedIdentifier || conn.mixedCaseQuoted) {
				assertFalse(conn.equals("tap_schema", "TAP_SCHEMA", true));
				assertTrue(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", true));
				assertFalse(conn.equals("TAP_SCHEMA", "tap_schema", true));
				assertFalse(conn.equals("Columns", "columns", true));
				assertFalse(conn.equals("columns", "Columns", true));
			} else if (conn.lowerCaseQuoted) {
				assertTrue(conn.equals("tap_schema", "TAP_SCHEMA", true));
				assertFalse(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", true));
				assertFalse(conn.equals("TAP_SCHEMA", "tap_schema", true));
				assertFalse(conn.equals("Columns", "columns", true));
				assertTrue(conn.equals("columns", "Columns", true));
			} else if (conn.upperCaseQuoted) {
				assertFalse(conn.equals("tap_schema", "TAP_SCHEMA", true));
				assertTrue(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", true));
				assertTrue(conn.equals("TAP_SCHEMA", "tap_schema", true));
				assertFalse(conn.equals("Columns", "columns", true));
				assertFalse(conn.equals("columns", "Columns", true));
			} else {
				assertTrue(conn.equals("tap_schema", "TAP_SCHEMA", true));
				assertTrue(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", true));
				assertTrue(conn.equals("TAP_SCHEMA", "tap_schema", true));
				assertTrue(conn.equals("Columns", "columns", true));
				assertTrue(conn.equals("columns", "Columns", true));
			}

			// CASE INSENSITIVE tests:
			if (conn.supportsMixedCaseUnquotedIdentifier) {
				assertTrue(conn.equals("tap_schema", "TAP_SCHEMA", false));
				assertTrue(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", false));
				assertTrue(conn.equals("TAP_SCHEMA", "tap_schema", false));
				assertTrue(conn.equals("Columns", "columns", false));
				assertTrue(conn.equals("columns", "Columns", false));
			} else if (conn.lowerCaseUnquoted) {
				assertTrue(conn.equals("tap_schema", "TAP_SCHEMA", false));
				assertFalse(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", false));
				assertFalse(conn.equals("TAP_SCHEMA", "tap_schema", false));
				assertFalse(conn.equals("Columns", "columns", false));
				assertTrue(conn.equals("columns", "Columns", false));
			} else if (conn.upperCaseUnquoted) {
				assertFalse(conn.equals("tap_schema", "TAP_SCHEMA", false));
				assertTrue(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", false));
				assertTrue(conn.equals("TAP_SCHEMA", "tap_schema", false));
				assertFalse(conn.equals("Columns", "columns", false));
				assertFalse(conn.equals("columns", "Columns", false));
			} else {
				assertTrue(conn.equals("tap_schema", "TAP_SCHEMA", false));
				assertTrue(conn.equals("TAP_SCHEMA", "TAP_SCHEMA", false));
				assertTrue(conn.equals("TAP_SCHEMA", "tap_schema", false));
				assertTrue(conn.equals("Columns", "columns", false));
				assertTrue(conn.equals("columns", "Columns", false));
			}
		}
	}

	@Test
	public void testGetTAPSchema() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {
			try {
				// Prepare the test:
				createTAPSchema(conn);
				// Try to get it (which should work without any problem here):
				conn.getTAPSchema();
			} catch(DBException de) {
				de.printStackTrace(System.err);
				fail("No pbm should happen here (either for the creation of a std TAP_SCHEMA or for its reading)! CAUSE: " + de.getMessage());
			}

			try {
				// Prepare the test:
				dropSchema(STDSchema.TAPSCHEMA.label, conn);
				// Try to get it (which should work without any problem here):
				conn.getTAPSchema();
				fail("DBException expected, because none of the TAP_SCHEMA tables exist.");
			} catch(DBException de) {
				assertTrue(de.getMessage().equals("Impossible to load schemas from TAP_SCHEMA.schemas!"));
			}
		}
	}

	@Test
	public void testIsTableExisting() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {
			try {
				// Get the database metadata:
				DatabaseMetaData dbMeta = conn.connection.getMetaData();

				// Prepare the test:
				createTAPSchema(conn);
				// Test the existence of all TAP_SCHEMA tables:
				assertTrue(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.SCHEMAS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.SCHEMAS.label), dbMeta));
				assertTrue(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.TABLES.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.TABLES.label), dbMeta));
				assertTrue(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.COLUMNS.label), dbMeta));
				assertTrue(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEYS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEYS.label), dbMeta));
				assertTrue(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEY_COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEY_COLUMNS.label), dbMeta));
				// Test the non-existence of any other table:
				assertFalse(conn.isTableExisting(null, "foo", dbMeta));

				// Prepare the test:
				dropSchema(STDSchema.TAPSCHEMA.label, conn);
				// Test the non-existence of all TAP_SCHEMA tables:
				assertFalse(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.SCHEMAS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.SCHEMAS.label), dbMeta));
				assertFalse(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.TABLES.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.TABLES.label), dbMeta));
				assertFalse(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.COLUMNS.label), dbMeta));
				assertFalse(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEYS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEYS.label), dbMeta));
				assertFalse(conn.isTableExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEY_COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEY_COLUMNS.label), dbMeta));
			} catch(Exception ex) {
				ex.printStackTrace(System.err);
				fail("{" + conn.getID() + "} Testing the existence of a table should not throw an error!");
			}
		}
	}

	@Test
	public void testIsColumnExisting() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		int i = -1;
		for(JDBCConnection conn : connections) {
			i++;
			try {
				// Get the database metadata:
				DatabaseMetaData dbMeta = conn.connection.getMetaData();

				// Prepare the test:
				createTAPSchema(conn);
				// Test the existence of one column for all TAP_SCHEMA tables:
				assertTrue(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.SCHEMAS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.SCHEMAS.label), "schema_name", dbMeta));
				assertTrue(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.TABLES.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.TABLES.label), "table_name", dbMeta));
				assertTrue(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.COLUMNS.label), "column_name", dbMeta));
				assertTrue(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEYS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEYS.label), "key_id", dbMeta));
				assertTrue(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEY_COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEY_COLUMNS.label), "key_id", dbMeta));
				// Test the non-existence of any column:
				assertFalse(conn.isColumnExisting(null, null, "foo", dbMeta));

				// Prepare the test:
				dropSchema(STDSchema.TAPSCHEMA.label, conn);
				// Test the non-existence of the same column for all TAP_SCHEMA tables:
				assertFalse(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.SCHEMAS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.SCHEMAS.label), "schema_name", dbMeta));
				assertFalse(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.TABLES.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.TABLES.label), "table_name", dbMeta));
				assertFalse(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.COLUMNS.label), "column_name", dbMeta));
				assertFalse(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEYS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEYS.label), "key_id", dbMeta));
				assertFalse(conn.isColumnExisting(STDSchema.TAPSCHEMA.label, (conn.supportsSchema ? STDTable.KEY_COLUMNS.label : STDSchema.TAPSCHEMA.label + "_" + STDTable.KEY_COLUMNS.label), "key_id", dbMeta));
			} catch(Exception ex) {
				ex.printStackTrace(System.err);
				fail("{" + conn.getID() + "} Testing the existence of a column should not throw an error!");
			}
		}
	}

	@Test
	public void testAddUploadedTable() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		TAPTable tableDef = null;
		for(JDBCConnection conn : connections) {
			InputStream io = null;
			try {
				io = new FileInputStream(uploadExamplePath);
				TableIterator it = new VOTableIterator(io);

				TAPColumn[] cols = it.getMetadata();
				tableDef = new TAPTable("UploadExample");
				for(TAPColumn c : cols)
					tableDef.addColumn(c);

				// Test with no schema set:
				try {
					conn.addUploadedTable(tableDef, it);
					fail("The table is not inside a TAPSchema, so this test should have failed!");
				} catch(Exception ex) {
					assertTrue(ex instanceof DBException);
					assertEquals("Missing upload schema! An uploaded table must be inside a schema whose the ADQL name is strictly equals to \"" + STDSchema.UPLOADSCHEMA.label + "\" (but the DB name may be different).", ex.getMessage());
				}

				// Specify the UPLOAD schema for the table to upload:
				TAPSchema schema = new TAPSchema(STDSchema.UPLOADSCHEMA.label);
				schema.addTable(tableDef);

				// Prepare the test: no TAP_UPLOAD schema and no table TAP_UPLOAD.UploadExample:
				dropSchema(STDSchema.UPLOADSCHEMA.label, conn);
				// Test:
				try {
					assertTrue(conn.addUploadedTable(tableDef, it));
				} catch(Exception ex) {
					ex.printStackTrace(System.err);
					fail("{" + conn.ID + "} This error should not happen: no TAP_UPLOAD schema.");
				}

				close(io);
				io = new FileInputStream(uploadExamplePath);
				it = new VOTableIterator(io);

				// Prepare the test: the TAP_UPLOAD schema exist but not the table TAP_UPLOAD.UploadExample:
				dropTable(tableDef.getDBSchemaName(), tableDef.getDBName(), conn);
				// Test:
				try {
					assertTrue(conn.addUploadedTable(tableDef, it));
				} catch(Exception ex) {
					ex.printStackTrace(System.err);
					fail("{" + conn.ID + "} This error should not happen: no TAP_UPLOAD schema.");
				}

				close(io);
				io = new FileInputStream(uploadExamplePath);
				it = new VOTableIterator(io);

				// Prepare the test: the TAP_UPLOAD schema and the table TAP_UPLOAD.UploadExample BOTH exist:
				;
				// Test:
				try {
					assertFalse(conn.addUploadedTable(tableDef, it));
				} catch(Exception ex) {
					if (ex instanceof DBException)
						assertEquals("Impossible to create the user uploaded table in the database: " + conn.translator.getTableName(tableDef, conn.supportsSchema) + "! This table already exists.", ex.getMessage());
					else {
						ex.printStackTrace(System.err);
						fail("{" + conn.ID + "} DBException was the expected exception!");
					}
				}

			} catch(Exception ex) {
				ex.printStackTrace(System.err);
				fail("{" + conn.ID + "} This error should never happen except there is a problem with the file (" + uploadExamplePath + ").");
			} finally {
				close(io);
			}
		}
	}

	@Test
	public void testDropUploadedTable() {
		TAPTable tableDef = new TAPTable("TableToDrop");
		TAPSchema uploadSchema = new TAPSchema(STDSchema.UPLOADSCHEMA.label);
		uploadSchema.addTable(tableDef);

		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {
			try {
				// 1st TEST CASE: the schema TAP_UPLOAD does not exist -> no error should be raised!
				// drop the TAP_UPLOAD schema:
				dropSchema(uploadSchema.getDBName(), conn);
				// try to drop the table:
				assertTrue(conn.dropUploadedTable(tableDef));

				// 2nd TEST CASE: the table does not exists -> no error should be raised!
				// create the TAP_UPLOAD schema, but not the table:
				createSchema(uploadSchema.getDBName(), conn);
				// try to drop the table:
				assertTrue(conn.dropUploadedTable(tableDef));

				// 3rd TEST CASE: the table and the schema exist -> the table should be created without any error!
				// create the fake uploaded table:
				createFooTable(tableDef.getDBSchemaName(), tableDef.getDBName(), conn);
				// try to drop the table:
				assertTrue(conn.dropUploadedTable(tableDef));

			} catch(Exception ex) {
				ex.printStackTrace(System.err);
				fail("{" + conn.ID + "} This error should not happen. The table should be dropped and even if it does not exist, no error should be thrown.");
			}
		}
	}

	@Test
	public void testExecuteQuery() {
		// There should be no difference between a H2 connection and a SQLITE one!
		JDBCConnection[] connections = new JDBCConnection[]{ h2JDBCConnection, sensH2JDBCConnection, sqliteJDBCConnection, sensSqliteJDBCConnection };
		for(JDBCConnection conn : connections) {

			TAPSchema schema = TAPMetadata.getStdSchema(conn.supportsSchema);
			ArrayList<DBTable> tables = new ArrayList<DBTable>(schema.getNbTables());
			for(TAPTable t : schema)
				tables.add(t);

			ADQLParser parser = (new ADQLParserFactory()).createParser();
			parser.setQueryChecker(new DBChecker(tables));
			parser.setDebug(false);

			/*if (conn.ID.equalsIgnoreCase("SQLITE")){
				for(DBTable t : tables){
					TAPTable tapT = (TAPTable)t;
					tapT.getSchema().setDBName(null);
					tapT.setDBName(tapT.getSchema().getADQLName() + "_" + tapT.getDBName());
				}
			}*/

			TableIterator result = null;
			try {
				// Prepare the test: create the TAP_SCHEMA:
				dropSchema(STDSchema.TAPSCHEMA.label, conn);
				// Build the ADQLQuery object:
				ADQLQuery query = parser.parseQuery("SELECT table_name FROM TAP_SCHEMA.tables;");
				// Execute the query:
				result = conn.executeQuery(query);
				fail("{" + conn.ID + "} This test should have failed because TAP_SCHEMA was supposed to not exist!");
			} catch(DBException de) {
				assertTrue(de.getMessage().startsWith("Unexpected error while executing a SQL query: "));
				assertTrue(de.getMessage().indexOf("tap_schema") > 0 || de.getMessage().indexOf("TAP_SCHEMA") > 0);
			} catch(ParseException pe) {
				pe.printStackTrace(System.err);
				fail("There should be no pbm to parse the ADQL expression!");
			} finally {
				if (result != null) {
					try {
						result.close();
					} catch(DataReadException de) {
					}
					result = null;
				}
			}

			try {
				// Prepare the test: create the TAP_SCHEMA:
				createTAPSchema(conn);
				// Build the ADQLQuery object:
				ADQLQuery query = parser.parseQuery("SELECT table_name FROM TAP_SCHEMA.tables;");
				// Execute the query:
				result = conn.executeQuery(query);
				assertEquals(1, result.getMetadata().length);
				int cntRow = 0;
				while(result.nextRow()) {
					cntRow++;
					assertTrue(result.hasNextCol());
					assertNotNull(TAPMetadata.resolveStdTable((String)result.nextCol()));
					assertFalse(result.hasNextCol());
				}
				assertEquals(5, cntRow);
			} catch(DBException de) {
				de.printStackTrace(System.err);
				fail("No ADQL/SQL query error was expected here!");
			} catch(ParseException pe) {
				fail("There should be no pbm to parse the ADQL expression!");
			} catch(DataReadException e) {
				e.printStackTrace(System.err);
				fail("There should be no pbm when accessing rows and the first (and only) columns of the result!");
			} catch(Exception ex) {
				ex.printStackTrace(System.err);
				fail("There should be no pbm when reading the query result!");
			} finally {
				if (result != null) {
					try {
						result.close();
					} catch(DataReadException de) {
					}
					result = null;
				}
			}
		}
	}

	/* ************** */
	/* TOOL FUNCTIONS */
	/* ************** */

	public final static void main(final String[] args) throws Throwable {
		JDBCConnection conn = new JDBCConnection(DBTools.createConnection("h2", null, null, DBTools.DB_TEST_PATH, DBTools.DB_TEST_USER, DBTools.DB_TEST_PWD), new AstroH2Translator(), "TEST_H2", null);
		TestJDBCConnection.createTAPSchema(conn);
		TestJDBCConnection.dropSchema(STDSchema.TAPSCHEMA.label, conn);
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
	protected static String getTablePrefix(final String schemaName) {
		if (schemaName != null && schemaName.trim().length() > 0)
			return schemaName + "_";
		else
			return "";
	}

	private static String getOfficialSchemaName(final String schemaName, final JDBCConnection conn) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.connection.createStatement();
			rs = conn.connection.getMetaData().getSchemas();
			while(rs.next()) {
				if (schemaName.equalsIgnoreCase(rs.getString(1)))
					return rs.getString(1);
			}
			close(rs);
			rs = null;
		} catch(Exception ex) {
			rollback(conn);
			ex.printStackTrace(System.err);
			fail("{" + conn.ID + "} Impossible to fetch the official DB schema name of " + schemaName + "!");
		} finally {
			close(rs);
			close(stmt);
		}
		return null;
	}

	private static void dropSchema(String schemaName, final JDBCConnection conn) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.connection.createStatement();

			final boolean caseSensitive = conn.translator.isCaseSensitive(IdentifierField.SCHEMA);
			if (conn.supportsSchema) {
				// search the official case sensitive schema name:
				schemaName = getOfficialSchemaName(schemaName, conn);
				// do nothing if the schema does not exist:
				if (schemaName == null)
					return;
				// then, list all its tables:
				startTransaction(conn);
				rs = conn.connection.getMetaData().getTables(null, schemaName, null, null);
				ArrayList<String> tablesToDrop = new ArrayList<String>();
				while(rs.next())
					tablesToDrop.add(rs.getString(3));
				close(rs);
				rs = null;
				// drop them:
				for(String t : tablesToDrop)
					stmt.executeUpdate("DROP TABLE IF EXISTS \"" + t + "\";");
				// finally drop the schema itself:
				stmt.executeUpdate("DROP SCHEMA IF EXISTS " + formatIdentifier(schemaName, true) + ";");
				commit(conn);
			} else {
				startTransaction(conn);
				final String tablePrefix = getTablePrefix(schemaName);
				final int prefixLen = tablePrefix.length();
				if (prefixLen <= 0)
					return;
				rs = conn.connection.getMetaData().getTables(null, null, null, null);
				ArrayList<String> tablesToDrop = new ArrayList<String>();
				while(rs.next()) {
					String table = rs.getString(3);
					if (table.length() > prefixLen) {
						if (equals(schemaName, table.substring(0, prefixLen - 1), caseSensitive))
							tablesToDrop.add(table);
					}
				}
				close(rs);
				rs = null;
				for(String t : tablesToDrop)
					stmt.executeUpdate("DROP TABLE IF EXISTS \"" + t + "\";");
				commit(conn);
			}
		} catch(Exception ex) {
			rollback(conn);
			ex.printStackTrace(System.err);
			fail("{" + conn.ID + "} Impossible to prepare a test by: dropping the schema " + schemaName + "!");
		} finally {
			close(rs);
			close(stmt);
		}
	}

	private static void dropTable(final String schemaName, final String tableName, final JDBCConnection conn) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			final boolean sCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.SCHEMA);
			final boolean tCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.TABLE);
			stmt = conn.connection.createStatement();
			if (conn.supportsSchema)
				stmt.executeUpdate("DROP TABLE IF EXISTS " + formatIdentifier(schemaName, sCaseSensitive) + "." + formatIdentifier(tableName, tCaseSensitive) + ";");
			else {
				rs = conn.connection.getMetaData().getTables(null, null, null, null);
				String tableToDrop = null;
				while(rs.next()) {
					String table = rs.getString(3);
					if (equals(tableName, table, tCaseSensitive)) {
						tableToDrop = table;
						break;
					}
				}
				close(rs);
				if (tableToDrop != null)
					stmt.executeUpdate("DROP TABLE IF EXISTS \"" + tableToDrop + "\";");
			}
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("{" + conn.ID + "} Impossible to prepare a test by: dropping the table " + schemaName + "." + tableName + "!");
		} finally {
			close(rs);
			close(stmt);
		}
	}

	private static void createSchema(final String schemaName, final JDBCConnection conn) {
		if (!conn.supportsSchema)
			return;

		dropSchema(schemaName, conn);

		Statement stmt = null;
		ResultSet rs = null;
		try {
			final boolean sCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.SCHEMA);
			stmt = conn.connection.createStatement();
			stmt.executeUpdate("CREATE SCHEMA " + formatIdentifier(schemaName, sCaseSensitive) + ";");
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("{" + conn.ID + "} Impossible to prepare a test by: creating the schema " + schemaName + "!");
		} finally {
			close(rs);
			close(stmt);
		}
	}

	private static void createFooTable(final String schemaName, final String tableName, final JDBCConnection conn) {
		dropTable(schemaName, tableName, conn);

		Statement stmt = null;
		ResultSet rs = null;
		try {
			final boolean sCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.SCHEMA);
			final boolean tCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.TABLE);
			String tablePrefix = formatIdentifier(schemaName, sCaseSensitive);
			if (!conn.supportsSchema || tablePrefix == null)
				tablePrefix = "";
			else
				tablePrefix += ".";
			stmt = conn.connection.createStatement();
			stmt.executeUpdate("CREATE TABLE " + tablePrefix + formatIdentifier(tableName, tCaseSensitive) + " (ID integer);");
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("{" + conn.ID + "} Impossible to prepare a test by: creating the table " + schemaName + "." + tableName + "!");
		} finally {
			close(rs);
			close(stmt);
		}
	}

	private static TAPMetadata createTAPSchema(final JDBCConnection conn) {
		dropSchema(STDSchema.TAPSCHEMA.label, conn);

		TAPMetadata metadata = new TAPMetadata();
		Statement stmt = null;
		try {
			final boolean sCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.SCHEMA);
			final boolean tCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.TABLE);
			final boolean cCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.COLUMN);
			String[] tableNames = new String[]{ STDTable.SCHEMAS.label, STDTable.TABLES.label, STDTable.COLUMNS.label, STDTable.KEYS.label, STDTable.KEY_COLUMNS.label };
			if (conn.supportsSchema) {
				for(int i = 0; i < tableNames.length; i++)
					tableNames[i] = formatIdentifier(STDSchema.TAPSCHEMA.label, sCaseSensitive) + "." + formatIdentifier(tableNames[i], tCaseSensitive);
			} else {
				for(int i = 0; i < tableNames.length; i++)
					tableNames[i] = formatIdentifier(getTablePrefix(STDSchema.TAPSCHEMA.label) + tableNames[i], tCaseSensitive);
			}

			startTransaction(conn);

			stmt = conn.connection.createStatement();

			if (conn.supportsSchema)
				stmt.executeUpdate("CREATE SCHEMA " + formatIdentifier(STDSchema.TAPSCHEMA.label, sCaseSensitive) + ";");

			stmt.executeUpdate("CREATE TABLE " + tableNames[0] + "(" + formatIdentifier("schema_name", cCaseSensitive) + " VARCHAR," + formatIdentifier("description", cCaseSensitive) + " VARCHAR," + formatIdentifier("utype", cCaseSensitive) + " VARCHAR," + formatIdentifier("dbname", cCaseSensitive) + " VARCHAR, PRIMARY KEY(" + formatIdentifier("schema_name", cCaseSensitive) + "));");
			stmt.executeUpdate("DELETE FROM " + tableNames[0] + ";");

			stmt.executeUpdate("CREATE TABLE " + tableNames[1] + "(" + formatIdentifier("schema_name", cCaseSensitive) + " VARCHAR," + formatIdentifier("table_name", cCaseSensitive) + " VARCHAR," + formatIdentifier("table_type", cCaseSensitive) + " VARCHAR," + formatIdentifier("description", cCaseSensitive) + " VARCHAR," + formatIdentifier("utype", cCaseSensitive) + " VARCHAR," + formatIdentifier("dbname", cCaseSensitive) + " VARCHAR, PRIMARY KEY(" + formatIdentifier("schema_name", cCaseSensitive) + ", " + formatIdentifier("table_name", cCaseSensitive) + "));");
			stmt.executeUpdate("DELETE FROM " + tableNames[1] + ";");

			stmt.executeUpdate("CREATE TABLE " + tableNames[2] + "(" + formatIdentifier("table_name", cCaseSensitive) + " VARCHAR," + formatIdentifier("column_name", cCaseSensitive) + " VARCHAR," + formatIdentifier("description", cCaseSensitive) + " VARCHAR," + formatIdentifier("unit", cCaseSensitive) + " VARCHAR," + formatIdentifier("ucd", cCaseSensitive) + " VARCHAR," + formatIdentifier("utype", cCaseSensitive) + " VARCHAR," + formatIdentifier("datatype", cCaseSensitive) + " VARCHAR," + formatIdentifier("arraysize", cCaseSensitive) + " INTEGER," + formatIdentifier("size", cCaseSensitive) + " INTEGER," + formatIdentifier("principal", cCaseSensitive) + " INTEGER," + formatIdentifier("indexed", cCaseSensitive) + " INTEGER," + formatIdentifier("std", cCaseSensitive) + " INTEGER," + formatIdentifier("dbname", cCaseSensitive) + " VARCHAR, PRIMARY KEY(" + formatIdentifier("table_name", cCaseSensitive) + ", " + formatIdentifier("column_name", cCaseSensitive) + "));");
			stmt.executeUpdate("DELETE FROM " + tableNames[2] + ";");

			stmt.executeUpdate("CREATE TABLE " + tableNames[3] + "(" + formatIdentifier("key_id", cCaseSensitive) + " VARCHAR," + formatIdentifier("from_table", cCaseSensitive) + " VARCHAR," + formatIdentifier("target_table", cCaseSensitive) + " VARCHAR," + formatIdentifier("description", cCaseSensitive) + " VARCHAR," + formatIdentifier("utype", cCaseSensitive) + " VARCHAR, PRIMARY KEY(" + formatIdentifier("key_id", cCaseSensitive) + "));");
			stmt.executeUpdate("DELETE FROM " + tableNames[3] + ";");

			stmt.executeUpdate("CREATE TABLE " + tableNames[4] + "(" + formatIdentifier("key_id", cCaseSensitive) + " VARCHAR," + formatIdentifier("from_column", cCaseSensitive) + " VARCHAR," + formatIdentifier("target_column", cCaseSensitive) + " VARCHAR, PRIMARY KEY(" + formatIdentifier("key_id", cCaseSensitive) + "));");
			stmt.executeUpdate("DELETE FROM " + tableNames[4] + ";");

			/*if (!conn.supportsSchema){
				TAPSchema stdSchema = TAPMetadata.getStdSchema();
				for(TAPTable t : stdSchema)
					t.setDBName(getTablePrefix(STDSchema.TAPSCHEMA.label) + t.getADQLName());
				metadata.addSchema(stdSchema);
			}else*/
			metadata.addSchema(TAPMetadata.getStdSchema(conn.supportsSchema));

			ArrayList<TAPTable> lstTables = new ArrayList<TAPTable>();
			for(TAPSchema schema : metadata) {
				stmt.executeUpdate("INSERT INTO " + tableNames[0] + " VALUES('" + schema.getADQLName() + "','" + schema.getDescription() + "','" + schema.getUtype() + "','" + schema.getDBName() + "')");
				for(TAPTable t : schema)
					lstTables.add(t);
			}

			ArrayList<DBColumn> lstCols = new ArrayList<DBColumn>();
			for(TAPTable table : lstTables) {
				stmt.executeUpdate("INSERT INTO " + tableNames[1] + " VALUES('" + table.getADQLSchemaName() + "','" + table.getADQLName() + "','" + table.getType() + "','" + table.getDescription() + "','" + table.getUtype() + "','" + table.getDBName() + "')");
				for(DBColumn c : table)
					lstCols.add(c);

			}
			lstTables = null;

			for(DBColumn c : lstCols) {
				TAPColumn col = (TAPColumn)c;
				stmt.executeUpdate("INSERT INTO " + tableNames[2] + " VALUES('" + col.getTable().getADQLName() + "','" + col.getADQLName() + "','" + col.getDescription() + "','" + col.getUnit() + "','" + col.getUcd() + "','" + col.getUtype() + "','" + col.getDatatype().type + "'," + col.getDatatype().length + "," + col.getDatatype().length + "," + (col.isPrincipal() ? 1 : 0) + "," + (col.isIndexed() ? 1 : 0) + "," + (col.isStd() ? 1 : 0) + ",'" + col.getDBName() + "')");
			}

			commit(conn);

		} catch(Exception ex) {
			rollback(conn);
			ex.printStackTrace(System.err);
			fail("{" + conn.ID + "} Impossible to prepare a test by: creating TAP_SCHEMA!");
		} finally {
			close(stmt);
		}

		return metadata;
	}

	private static void startTransaction(final JDBCConnection conn) {
		try {
			conn.connection.setAutoCommit(false);
		} catch(SQLException se) {
		}
	}

	private static void commit(final JDBCConnection conn) {
		try {
			conn.connection.commit();
			conn.connection.setAutoCommit(true);
		} catch(SQLException se) {
		}

	}

	private static void rollback(final JDBCConnection conn) {
		try {
			conn.connection.rollback();
			conn.connection.setAutoCommit(true);
		} catch(SQLException se) {
		}

	}

	private static String formatIdentifier(final String identifier, final boolean caseSensitive) {
		if (identifier == null)
			return null;
		else if (identifier.charAt(0) == '"')
			return identifier;
		else if (caseSensitive)
			return "\"" + identifier + "\"";
		else
			return identifier;
	}

	private static boolean equals(final String name1, final String name2, final boolean caseSensitive) {
		return (name1 != null && name2 != null && (caseSensitive ? name1.equals(name2) : name1.equalsIgnoreCase(name2)));
	}

	private static boolean equals(final TAPTable table1, final TAPTable table2) {
		if (table1 == null || table2 == null) {
			//System.out.println("[EQUALS] tables null!");
			return false;
		}

		if (!table1.getFullName().equals(table2.getFullName())) {
			//System.out.println("[EQUALS] tables name different: " + table1.getFullName() + " != " + table2.getFullName() + "!");
			return false;
		}

		if (table1.getType() != table2.getType()) {
			//System.out.println("[EQUALS] tables type different: " + table1.getType() + " != " + table2.getType() + "!");
			return false;
		}

		if (table1.getNbColumns() != table2.getNbColumns()) {
			//System.out.println("[EQUALS] tables length different: " + table1.getNbColumns() + " columns != " + table2.getNbColumns() + " columns!");
			return false;
		}

		Iterator<TAPColumn> it = table1.getColumns();
		while(it.hasNext()) {
			TAPColumn col1 = it.next();
			if (!equals(col1, table2.getColumn(col1.getADQLName()))) {
				//System.out.println("[EQUALS] tables columns different!");
				return false;
			}
		}

		return true;
	}

	private static boolean equals(final TAPColumn col1, final TAPColumn col2) {
		if (col1 == null || col2 == null) {
			//System.out.println("[EQUALS] columns null!");
			return false;
		}

		if (!col1.getADQLName().equals(col2.getADQLName())) {
			//System.out.println("[EQUALS] columns name different: " + col1.getADQLName() + " != " + col2.getADQLName() + "!");
			return false;
		}

		if (!equals(col1.getDatatype(), col2.getDatatype())) {
			//System.out.println("[EQUALS] columns type different: " + col1.getDatatype() + " != " + col2.getDatatype() + "!");
			return false;
		}

		if (col1.getUnit() != col2.getUnit()) {
			//System.out.println("[EQUALS] columns unit different: " + col1.getUnit() + " != " + col2.getUnit() + "!");
			return false;
		}

		if (col1.getUcd() != col2.getUcd()) {
			//System.out.println("[EQUALS] columns ucd different: " + col1.getUcd() + " != " + col2.getUcd() + "!");
			return false;
		}

		return true;
	}

	private static boolean equals(final DBType type1, final DBType type2) {
		return type1 != null && type2 != null && type1.type == type2.type && type1.length == type2.length;
	}

	private static TAPMetadata createCustomSchema() {
		TAPMetadata tapMeta = new TAPMetadata();
		TAPSchema tapSchema = new TAPSchema(STDSchema.TAPSCHEMA.toString());
		TAPTable customColumns = (TAPTable)TAPMetadata.getStdTable(STDTable.COLUMNS).copy("Columns", STDTable.COLUMNS.label);
		customColumns.addColumn("TestNewColumn", new DBType(DBDatatype.VARCHAR), "This is a fake column, just for test purpose.", null, null, null);
		tapSchema.addTable(customColumns);
		TAPTable addTable = new TAPTable("AdditionalTable");
		addTable.addColumn("Blabla");
		addTable.addColumn("Foo");
		tapSchema.addTable(addTable);
		tapMeta.addSchema(tapSchema);
		return tapMeta;
	}

	/**
	 * <p>Get the expected counts after a call of {@link JDBCConnection#setTAPSchema(TAPMetadata)}.</p>
	 *
	 * <p>Counts are computed from the given metadata ; the same metadata that will be given to {@link JDBCConnection#setTAPSchema(TAPMetadata)}.</p>
	 *
	 * @param meta
	 *
	 * @return	An integer array with the following values: [0]=nbSchemas, [1]=nbTables, [2]=nbColumns, [3]=nbKeys and [4]=nbKeyColumns.
	 */
	private static int[] getStats(final TAPMetadata meta) {
		int[] counts = new int[]{ 1, 5, 0, 0, 0 };

		int[] stdColCounts = new int[]{ 4, 6, 13, 5, 3 }; // 4,6 because of the addition of `dbname` ; 13 because of the addition of `dbname` and `arraysize` (tap-1.1)
		for(int c = 0; c < stdColCounts.length; c++)
			counts[2] += stdColCounts[c];

		Iterator<TAPSchema> itSchemas = meta.iterator();
		while(itSchemas.hasNext()) {
			TAPSchema schema = itSchemas.next();

			boolean isTapSchema = (schema.getADQLName().equalsIgnoreCase(STDSchema.TAPSCHEMA.toString()));
			if (!isTapSchema)
				counts[0]++;

			Iterator<TAPTable> itTables = schema.iterator();
			while(itTables.hasNext()) {
				TAPTable table = itTables.next();
				if (isTapSchema && TAPMetadata.resolveStdTable(table.getADQLName()) != null) {
					int ind = h2JDBCConnection.getCreationOrder(TAPMetadata.resolveStdTable(table.getADQLName()));
					counts[2] -= stdColCounts[ind];
				} else
					counts[1]++;

				Iterator<DBColumn> itColumns = table.iterator();
				while(itColumns.hasNext()) {
					itColumns.next();
					counts[2]++;
				}

				Iterator<TAPForeignKey> itKeys = table.getForeignKeys();
				while(itKeys.hasNext()) {
					TAPForeignKey fk = itKeys.next();
					counts[3]++;
					counts[4] += fk.getNbRelations();
				}
			}
		}

		return counts;
	}

	/**
	 * <p>Get the effective counts after a call of {@link JDBCConnection#setTAPSchema(TAPMetadata)}.</p>
	 *
	 * <p>Counts are computed directly from the DB using the given connection; the same connection used to set the TAP schema in {@link JDBCConnection#setTAPSchema(TAPMetadata)}.</p>
	 *
	 * @param conn
	 * @param meta	Metadata, in order to get the standard TAP tables' name.
	 *
	 * @return	An integer array with the following values: [0]=nbSchemas, [1]=nbTables, [2]=nbColumns, [3]=nbKeys and [4]=nbKeyColumns.
	 */
	private static int[] getStats(final JDBCConnection conn, final TAPMetadata meta) {
		int[] counts = new int[5];

		Statement stmt = null;
		try {
			stmt = conn.connection.createStatement();

			TAPSchema tapSchema = meta.getSchema(STDSchema.TAPSCHEMA.toString());

			String schemaPrefix = formatIdentifier(tapSchema.getDBName(), conn.translator.isCaseSensitive(IdentifierField.SCHEMA));
			if (!conn.supportsSchema || schemaPrefix == null)
				schemaPrefix = "";
			else
				schemaPrefix += ".";

			boolean tCaseSensitive = conn.translator.isCaseSensitive(IdentifierField.TABLE);
			TAPTable tapTable = tapSchema.getTable(STDTable.SCHEMAS.toString());
			counts[0] = count(stmt, schemaPrefix + formatIdentifier(tapTable.getDBName(), tCaseSensitive), tapSchema.getADQLName() + "." + tapTable.getADQLName());

			tapTable = tapSchema.getTable(STDTable.TABLES.toString());
			counts[1] = count(stmt, schemaPrefix + formatIdentifier(tapTable.getDBName(), tCaseSensitive), tapSchema.getADQLName() + "." + tapTable.getADQLName());

			tapTable = tapSchema.getTable(STDTable.COLUMNS.toString());
			counts[2] = count(stmt, schemaPrefix + formatIdentifier(tapTable.getDBName(), tCaseSensitive), tapSchema.getADQLName() + "." + tapTable.getADQLName());

			tapTable = tapSchema.getTable(STDTable.KEYS.toString());
			counts[3] = count(stmt, schemaPrefix + formatIdentifier(tapTable.getDBName(), tCaseSensitive), tapSchema.getADQLName() + "." + tapTable.getADQLName());

			tapTable = tapSchema.getTable(STDTable.KEY_COLUMNS.toString());
			counts[4] = count(stmt, schemaPrefix + formatIdentifier(tapTable.getDBName(), tCaseSensitive), tapSchema.getADQLName() + "." + tapTable.getADQLName());

		} catch(SQLException se) {
			fail("Can not create a statement!");
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch(SQLException ex) {
			}
		}
		return counts;
	}

	private static int count(final Statement stmt, final String qualifiedTableName, final String adqlTableName) {
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery("SELECT COUNT(*) FROM " + qualifiedTableName + ";");
			rs.next();
			return rs.getInt(1);
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("Can not count! Maybe " + qualifiedTableName + " (in ADQL: " + adqlTableName + ") does not exist.");
			return -1;
		} finally {
			close(rs);
		}
	}

	private static void close(final ResultSet rs) {
		if (rs == null)
			return;
		try {
			rs.close();
		} catch(SQLException se) {
		}
	}

	private static void close(final Statement stmt) {
		try {
			if (stmt != null)
				stmt.close();
		} catch(SQLException se) {
		}
	}

	private static void close(final InputStream io) {
		try {
			if (io != null)
				io.close();
		} catch(IOException ioe) {
		}
	}

}
