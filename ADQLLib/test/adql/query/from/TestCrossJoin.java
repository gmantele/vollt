package adql.query.from;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import adql.db.DBColumn;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.SearchColumnList;
import adql.query.IdentifierField;

public class TestCrossJoin {

	private ADQLTable tableA, tableB;

	@AfterClass
	public static void tearDownAfterClass() throws Exception{}

	@Before
	public void setUp() throws Exception{
		/* SET THE TABLES AND COLUMNS NEEDED FOR THE TEST */
		// Describe the available table:
		DefaultDBTable metaTableA = new DefaultDBTable("A");
		metaTableA.setADQLSchemaName("public");
		DefaultDBTable metaTableB = new DefaultDBTable("B");
		metaTableB.setADQLSchemaName("public");

		// Describe its columns:
		metaTableA.addColumn(new DefaultDBColumn("id", new DBType(DBDatatype.VARCHAR), metaTableA));
		metaTableA.addColumn(new DefaultDBColumn("txta", new DBType(DBDatatype.VARCHAR), metaTableA));
		metaTableB.addColumn(new DefaultDBColumn("id", new DBType(DBDatatype.VARCHAR), metaTableB));
		metaTableB.addColumn(new DefaultDBColumn("txtb", new DBType(DBDatatype.VARCHAR), metaTableB));

		// Build the ADQL tables:
		tableA = new ADQLTable("A");
		tableA.setDBLink(metaTableA);
		tableB = new ADQLTable("B");
		tableB.setDBLink(metaTableB);
	}

	@Test
	public void testGetDBColumns(){
		try{
			ADQLJoin join = new CrossJoin(tableA, tableB);
			SearchColumnList joinColumns = join.getDBColumns();
			assertEquals(4, joinColumns.size());

			// check column A.id and B.id
			List<DBColumn> lstFound = joinColumns.search(null, null, null, "id", IdentifierField.getFullCaseSensitive(true));
			assertEquals(2, lstFound.size());
			// A.id
			assertNotNull(lstFound.get(0).getTable());
			assertEquals("A", lstFound.get(0).getTable().getADQLName());
			assertEquals("public", lstFound.get(0).getTable().getADQLSchemaName());
			assertEquals(1, joinColumns.search(null, "public", "A", "id", IdentifierField.getFullCaseSensitive(true)).size());
			// B.id
			assertNotNull(lstFound.get(1).getTable());
			assertEquals("B", lstFound.get(1).getTable().getADQLName());
			assertEquals("public", lstFound.get(1).getTable().getADQLSchemaName());
			assertEquals(1, joinColumns.search(null, "public", "B", "id", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "C", "id", IdentifierField.getFullCaseSensitive(true)).size());

			// check column A.txta
			lstFound = joinColumns.search(null, null, null, "txta", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertNotNull(lstFound.get(0).getTable());
			assertEquals("A", lstFound.get(0).getTable().getADQLName());
			assertEquals("public", lstFound.get(0).getTable().getADQLSchemaName());
			assertEquals(1, joinColumns.search(null, "public", "A", "txta", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "B", "txta", IdentifierField.getFullCaseSensitive(true)).size());

			// check column B.txtb
			lstFound = joinColumns.search(null, null, null, "txtb", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertNotNull(lstFound.get(0).getTable());
			assertEquals("B", lstFound.get(0).getTable().getADQLName());
			assertEquals("public", lstFound.get(0).getTable().getADQLSchemaName());
			assertEquals(1, joinColumns.search(null, "public", "B", "txtb", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "A", "txtb", IdentifierField.getFullCaseSensitive(true)).size());

		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded!");
		}
	}
}
