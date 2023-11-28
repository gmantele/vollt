package adql.db;

import adql.parser.ADQLParser;
import adql.query.ADQLSet;
import adql.translator.PostgreSQLTranslator;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestSubQueries {

	private final static String SCHEMA_PUBLIC = "public";
	private ADQLParser adqlParser;

	@Before
	public void setUp() throws Exception {
		// Build the list of available tables:
		final ArrayList<DBTable> TABLES = new ArrayList<>();
		TABLES.add(createTable1());
		TABLES.add(createTable2());
		TABLES.add(createTable3());

		// Create a new ADQLParser:
		adqlParser = new ADQLParser();

		// Associate it with the available tables:
		adqlParser.setQueryChecker(new DBChecker(TABLES));
	}

	private DBTable createTable1(){
		DefaultDBTable table = new DefaultDBTable("table1");
		table.setADQLSchemaName(SCHEMA_PUBLIC);
		table.addColumn(new DefaultDBColumn("oid", new DBType(DBType.DBDatatype.BIGINT), table));
		table.addColumn(new DefaultDBColumn("ra", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("dec", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("h_m", new DBType(DBType.DBDatatype.REAL), table));
		table.addColumn(new DefaultDBColumn("j_m", new DBType(DBType.DBDatatype.REAL), table));
		table.addColumn(new DefaultDBColumn("k_m", new DBType(DBType.DBDatatype.REAL), table));
		return table;
	}

	private DBTable createTable2(){
		DefaultDBTable table = new DefaultDBTable("table2");
		table.setADQLSchemaName(SCHEMA_PUBLIC);
		table.addColumn(new DefaultDBColumn("oid2", new DBType(DBType.DBDatatype.BIGINT), table));
		table.addColumn(new DefaultDBColumn("ra", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("ra_error", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("dec", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("dec_error", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("parallax", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("parallax_error", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("magnitude", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("pmra", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("pmra_error", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("pmdec", new DBType(DBType.DBDatatype.DOUBLE), table));
		table.addColumn(new DefaultDBColumn("pmdec_error", new DBType(DBType.DBDatatype.DOUBLE), table));
		return table;
	}

	private DBTable createTable3(){
		DefaultDBTable table = new DefaultDBTable("table3");
		table.setADQLSchemaName(SCHEMA_PUBLIC);
		table.addColumn(new DefaultDBColumn("oid", new DBType(DBType.DBDatatype.BIGINT), table));
		table.addColumn(new DefaultDBColumn("oid2", new DBType(DBType.DBDatatype.BIGINT), table));
		return table;
	}

	@Test
	public void testSeveralSubqueries() {
		try {
			ADQLSet query = adqlParser.parseQuery("SELECT sel2.*,t1.h_m, t1.j_m, t1.k_m\nFROM (\n  SELECT sel1.*, t3.*\n  FROM (\n  	SELECT *\n    FROM table2 AS t2\n	WHERE 1=CONTAINS(POINT('ICRS', t2.ra, t2.dec), CIRCLE('ICRS', 56.75, 24.1167, 15.))\n  ) AS sel1 JOIN table3 AS t3 ON t3.oid2=sel1.oid2\n) AS sel2 JOIN table1 AS t1 ON sel2.oid=t1.oid");
			assertEquals("SELECT sel2.* , t1.h_m , t1.j_m , t1.k_m\nFROM (SELECT sel1.* , t3.*\nFROM (SELECT *\nFROM table2 AS t2\nWHERE 1 = CONTAINS(POINT('ICRS', t2.ra, t2.dec), CIRCLE('ICRS', 56.75, 24.1167, 15.))) AS sel1 INNER JOIN table3 AS t3 ON ON t3.oid2 = sel1.oid2) AS sel2 INNER JOIN table1 AS t1 ON ON sel2.oid = t1.oid", query.toADQL());

		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("No error expected! (see console for more details)");
		}
	}

	@Test
	public void testFatherTableAliasIntoSubqueries() {
		try {
			ADQLSet query = adqlParser.parseQuery("SELECT oid FROM table1 as MyAlias WHERE oid IN (SELECT oid2 FROM table2 WHERE oid2 = myAlias.oid)");
			assertEquals("SELECT oid\nFROM table1 AS MyAlias\nWHERE oid IN (SELECT oid2\nFROM table2\nWHERE oid2 = myAlias.oid)", query.toADQL());
			assertEquals("SELECT \"myalias\".\"oid\" AS \"oid\"\nFROM \"public\".\"table1\" AS \"myalias\"\nWHERE \"myalias\".\"oid\" IN (SELECT \"public\".\"table2\".\"oid2\" AS \"oid2\"\nFROM \"public\".\"table2\"\nWHERE \"public\".\"table2\".\"oid2\" = \"myalias\".\"oid\")", (new PostgreSQLTranslator()).translate(query));
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("No error expected! (see console for more details)");
		}
	}

	@Test
	public void testParentRefToMixedCaseColumnAliasInsideSubQueries() {
		try {
			ADQLSet query = adqlParser.parseQuery("SELECT t.* FROM (SELECT (ra+ra_error) AS x, (dec+dec_error) AS Y, pmra AS \"ProperMotion\" FROM table2) AS t");
			assertEquals("SELECT t.*\nFROM (SELECT (ra+ra_error) AS x , (dec+dec_error) AS Y , pmra AS \"ProperMotion\"\nFROM table2) AS t", query.toADQL());
			assertEquals("SELECT \"t\".\"x\" AS \"x\" , \"t\".\"y\" AS \"y\" , \"t\".\"ProperMotion\" AS \"ProperMotion\"\nFROM (SELECT ((\"public\".\"table2\".\"ra\"+\"public\".\"table2\".\"ra_error\")) AS \"x\" , ((\"public\".\"table2\".\"dec\"+\"public\".\"table2\".\"dec_error\")) AS \"y\" , \"public\".\"table2\".\"pmra\" AS \"ProperMotion\"\nFROM \"public\".\"table2\") AS \"t\"", (new PostgreSQLTranslator()).translate(query));
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			fail("No error expected! (see console for more details)");
		}
	}

}
