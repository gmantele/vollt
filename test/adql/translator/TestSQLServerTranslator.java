package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBChecker;
import adql.db.DBTable;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.parser.SQLServer_ADQLQueryFactory;
import adql.query.ADQLQuery;

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
			assertEquals("\"aTable\" AS \"a\" INNER JOIN \"anotherTable\" AS \"b\" ON \"a\".\"id\"=\"b\".\"id\" AND \"a\".\"name\"=\"b\".\"name\"", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to NATURAL) are actually translated as columns of the first joined table):
			assertEquals("SELECT \"a\".\"id\" AS \"id\" , \"a\".\"name\" AS \"name\" , \"a\".\"aColumn\" AS \"aColumn\" , \"b\".\"anotherColumn\" AS \"anotherColumn\"", translator.translate(query.getSelect()));

		}catch(ParseException pe){
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		}catch(TranslationException te){
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

	@Test
	public void testJoinWithUSING(){
		final String adqlquery = "SELECT B.id, name, aColumn, anotherColumn FROM aTable A JOIN anotherTable B USING(name);";

		try{
			ADQLQuery query = (new ADQLParser(new DBChecker(tables), new SQLServer_ADQLQueryFactory())).parseQuery(adqlquery);
			SQLServerTranslator translator = new SQLServerTranslator();

			// Test the FROM part:
			assertEquals("\"aTable\" AS \"a\" INNER JOIN \"anotherTable\" AS \"b\" ON \"a\".\"name\"=\"b\".\"name\"", translator.translate(query.getFrom()));

			// Test the SELECT part (in order to ensure the usual common columns (due to USING) are actually translated as columns of the first joined table):
			assertEquals("SELECT \"b\".\"id\" AS \"id\" , \"a\".\"name\" AS \"name\" , \"a\".\"aColumn\" AS \"aColumn\" , \"b\".\"anotherColumn\" AS \"anotherColumn\"", translator.translate(query.getSelect()));

		}catch(ParseException pe){
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		}catch(TranslationException te){
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

}
