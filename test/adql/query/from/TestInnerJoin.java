package adql.query.from;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBColumn;
import adql.db.DBCommonColumn;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.SearchColumnList;
import adql.query.IdentifierField;
import adql.query.operand.ADQLColumn;

public class TestInnerJoin {

	private ADQLTable tableA, tableB, tableC;

	@Before
	public void setUp() throws Exception{
		/* SET THE TABLES AND COLUMNS NEEDED FOR THE TEST */
		// Describe the available table:
		DefaultDBTable metaTableA = new DefaultDBTable("A");
		metaTableA.setADQLSchemaName("public");
		DefaultDBTable metaTableB = new DefaultDBTable("B");
		metaTableB.setADQLSchemaName("public");
		DefaultDBTable metaTableC = new DefaultDBTable("C");
		metaTableC.setADQLSchemaName("public");

		// Describe its columns:
		metaTableA.addColumn(new DefaultDBColumn("id", new DBType(DBDatatype.VARCHAR), metaTableA));
		metaTableA.addColumn(new DefaultDBColumn("txta", new DBType(DBDatatype.VARCHAR), metaTableA));
		metaTableB.addColumn(new DefaultDBColumn("id", new DBType(DBDatatype.VARCHAR), metaTableB));
		metaTableB.addColumn(new DefaultDBColumn("txtb", new DBType(DBDatatype.VARCHAR), metaTableB));
		metaTableC.addColumn(new DefaultDBColumn("Id", new DBType(DBDatatype.VARCHAR), metaTableC));
		metaTableC.addColumn(new DefaultDBColumn("txta", new DBType(DBDatatype.VARCHAR), metaTableC));
		metaTableC.addColumn(new DefaultDBColumn("txtc", new DBType(DBDatatype.VARCHAR), metaTableC));

		// Build the ADQL tables:
		tableA = new ADQLTable("A");
		tableA.setDBLink(metaTableA);
		tableB = new ADQLTable("B");
		tableB.setDBLink(metaTableB);
		tableC = new ADQLTable("C");
		tableC.setDBLink(metaTableC);
	}

	@Test
	public void testGetDBColumns(){
		// Test NATURAL JOIN 1:
		try{
			ADQLJoin join = new InnerJoin(tableA, tableB);
			SearchColumnList joinColumns = join.getDBColumns();
			assertEquals(3, joinColumns.size());
			List<DBColumn> lstFound = joinColumns.search(null, null, null, "id", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertEquals(DBCommonColumn.class, lstFound.get(0).getClass());
			assertEquals(1, joinColumns.search(null, "public", "A", "id", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(1, joinColumns.search(null, "public", "B", "id", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "C", "id", IdentifierField.getFullCaseSensitive(true)).size());
			lstFound = joinColumns.search(null, "public", "A", "txta", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			lstFound = joinColumns.search(null, "public", "B", "txtb", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded!");
		}

		// Test NATURAL JOIN 2:
		try{
			ADQLJoin join = new InnerJoin(tableA, tableC);
			SearchColumnList joinColumns = join.getDBColumns();
			assertEquals(3, joinColumns.size());

			// check id (column common to table A and C only):
			List<DBColumn> lstFound = joinColumns.search(null, null, null, "id", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertEquals(DBCommonColumn.class, lstFound.get(0).getClass());
			assertEquals(1, joinColumns.search(null, "public", "A", "id", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(1, joinColumns.search(null, "public", "C", "id", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "B", "id", IdentifierField.getFullCaseSensitive(true)).size());

			// check txta (column common to table A and C only):
			lstFound = joinColumns.search(null, null, null, "txta", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertEquals(DBCommonColumn.class, lstFound.get(0).getClass());
			assertEquals(1, joinColumns.search(null, "public", "A", "txta", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(1, joinColumns.search(null, "public", "C", "txta", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "B", "id", IdentifierField.getFullCaseSensitive(true)).size());

			// check txtc (only for table C)
			lstFound = joinColumns.search(null, null, null, "txtc", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertNotNull(lstFound.get(0).getTable());
			assertEquals("C", lstFound.get(0).getTable().getADQLName());
			assertEquals("public", lstFound.get(0).getTable().getADQLSchemaName());

		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded!");
		}

		// Test with a USING("id"):
		try{
			List<ADQLColumn> usingList = new ArrayList<ADQLColumn>(1);
			usingList.add(new ADQLColumn("id"));
			ADQLJoin join = new InnerJoin(tableA, tableC, usingList);
			SearchColumnList joinColumns = join.getDBColumns();
			assertEquals(4, joinColumns.size());

			// check id (column common to table A and C only):
			List<DBColumn> lstFound = joinColumns.search(null, null, null, "id", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertEquals(DBCommonColumn.class, lstFound.get(0).getClass());
			assertEquals(1, joinColumns.search(null, "public", "A", "id", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(1, joinColumns.search(null, "public", "C", "id", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "B", "id", IdentifierField.getFullCaseSensitive(true)).size());

			// check A.txta and C.txta:
			lstFound = joinColumns.search(null, null, null, "txta", IdentifierField.getFullCaseSensitive(true));
			assertEquals(2, lstFound.size());
			// A.txta
			assertNotNull(lstFound.get(0).getTable());
			assertEquals("A", lstFound.get(0).getTable().getADQLName());
			assertEquals("public", lstFound.get(0).getTable().getADQLSchemaName());
			assertEquals(1, joinColumns.search(null, "public", "A", "txta", IdentifierField.getFullCaseSensitive(true)).size());
			// C.txta
			assertNotNull(lstFound.get(1).getTable());
			assertEquals("C", lstFound.get(1).getTable().getADQLName());
			assertEquals("public", lstFound.get(1).getTable().getADQLSchemaName());
			assertEquals(1, joinColumns.search(null, "public", "C", "txta", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "B", "txta", IdentifierField.getFullCaseSensitive(true)).size());

			// check txtc (only for table C):
			lstFound = joinColumns.search(null, null, null, "txtc", IdentifierField.getFullCaseSensitive(true));
			assertEquals(1, lstFound.size());
			assertNotNull(lstFound.get(0).getTable());
			assertEquals("C", lstFound.get(0).getTable().getADQLName());
			assertEquals("public", lstFound.get(0).getTable().getADQLSchemaName());
			assertEquals(1, joinColumns.search(null, "public", "C", "txtc", IdentifierField.getFullCaseSensitive(true)).size());
			assertEquals(0, joinColumns.search(null, "public", "A", "txtc", IdentifierField.getFullCaseSensitive(true)).size());

		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded!");
		}
	}

}
