package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.SearchColumnList;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.query.ADQLQuery;
import adql.query.from.ADQLJoin;
import adql.query.operand.ADQLColumn;

public class TestSQLServerTranslator {

	private List<DBTable> tables = null;

	@Before
	public void setUp() throws Exception{
		tables = new ArrayList<DBTable>(2);
		DefaultDBTable t = new DefaultDBTable("aTable");
		t.addColumn(new DefaultDBColumn("id", t));
		t.addColumn(new DefaultDBColumn("name", t));
		t.addColumn(new DefaultDBColumn("aColumn", t));
		tables.add(t);
		t = new DefaultDBTable("anotherTable");
		t.addColumn(new DefaultDBColumn("id", t));
		t.addColumn(new DefaultDBColumn("name", t));
		t.addColumn(new DefaultDBColumn("anotherColumn", t));
		tables.add(t);
	}

	@Test
	public void testNaturalJoin(){
		final String adqlquery = "SELECT id, name, aColumn, anotherColumn FROM aTable A NATURAL JOIN anotherTable B;";

		try{
			ADQLQuery query = (new ADQLParser(new DBChecker(tables), new SQLServer_ADQLQueryFactory())).parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			// Test the FROM part:
			assertEquals("\"aTable\" AS A INNER JOIN \"anotherTable\" AS B ON \"aTable\".\"id\"=\"anotherTable\".\"id\" AND \"aTable\".\"name\"=\"anotherTable\".\"name\"", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to NATURAL) are actually translated as columns of the first joined table):
			assertEquals("SELECT A.\"id\" AS \"id\" , A.\"name\" AS \"name\" , A.\"aColumn\" AS \"aColumn\" , B.\"anotherColumn\" AS \"anotherColumn\"", translator.translate(query.getSelect()));

		}catch(ParseException pe){
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		}catch(TranslationException te){
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

	@Test
	public void testNaturalJoin2(){
		final String adqlquery = "SELECT id, name, aColumn, anotherColumn FROM aTable \"A\" NATURAL JOIN anotherTable B;";

		try{
			ADQLQuery query = (new ADQLParser(new DBChecker(tables), new SQLServer_ADQLQueryFactory())).parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			ADQLJoin join = (ADQLJoin)query.getFrom();

			try{
				StringBuffer buf = new StringBuffer();

				// Find duplicated items between the two lists and translate them as ON conditions:
				DBColumn rightCol;
				SearchColumnList leftList = join.getLeftTable().getDBColumns();
				SearchColumnList rightList = join.getRightTable().getDBColumns();
				for(DBColumn leftCol : leftList){
					// search for at most one column with the same name in the RIGHT list
					// and throw an exception is there are several matches:
					rightCol = ADQLJoin.findAtMostOneColumn(leftCol.getADQLName(), (byte)0, rightList, false);
					// if there is one...
					if (rightCol != null){
						// ...check there is only one column with this name in the LEFT list,
						// and throw an exception if it is not the case:
						ADQLJoin.findExactlyOneColumn(leftCol.getADQLName(), (byte)0, leftList, true);
						// ...append the corresponding join condition:
						if (buf.length() > 0)
							buf.append(" AND ");
						ADQLColumn col = new ADQLColumn(leftCol.getADQLName());
						col.setDBLink(leftCol);
						// TODO col.setAdqlTable(adqlTable);
						buf.append(translator.translate(col));
						buf.append("=");
						col = new ADQLColumn(rightCol.getADQLName());
						col.setDBLink(rightCol);
						buf.append(translator.translate(col));
					}
				}

				System.out.println("ON " + buf.toString());
			}catch(Exception uje){
				System.err.println("Impossible to resolve the NATURAL JOIN between " + join.getLeftTable().toADQL() + " and " + join.getRightTable().toADQL() + "!");
				uje.printStackTrace();
			}

		}catch(ParseException pe){
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		}
	}

	@Test
	public void testJoinWithUSING(){
		final String adqlquery = "SELECT B.id, name, aColumn, anotherColumn FROM aTable A JOIN anotherTable B USING(name);";

		try{
			ADQLQuery query = (new ADQLParser(new DBChecker(tables), new SQLServer_ADQLQueryFactory())).parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			// Test the FROM part:
			assertEquals("\"aTable\" AS A INNER JOIN \"anotherTable\" AS B ON \"aTable\".\"name\"=\"anotherTable\".\"name\"", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to USING) are actually translated as columns of the first joined table):
			assertEquals("SELECT B.\"id\" AS \"id\" , A.\"name\" AS \"name\" , A.\"aColumn\" AS \"aColumn\" , B.\"anotherColumn\" AS \"anotherColumn\"", translator.translate(query.getSelect()));

		}catch(ParseException pe){
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		}catch(TranslationException te){
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

}
