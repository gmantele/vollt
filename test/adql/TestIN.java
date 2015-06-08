package adql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import adql.query.ADQLList;
import adql.query.ADQLObject;
import adql.query.ADQLOrder;
import adql.query.ADQLQuery;
import adql.query.ClauseSelect;
import adql.query.constraint.In;
import adql.query.from.ADQLTable;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.search.IReplaceHandler;
import adql.search.SimpleReplaceHandler;
import adql.translator.ADQLTranslator;
import adql.translator.PostgreSQLTranslator;

public class TestIN {

	private static ADQLTranslator translator = null;

	@BeforeClass
	public static void setUpBeforeClass(){
		translator = new PostgreSQLTranslator();
	}

	@Test
	public void testIN(){
		// Test with a simple list of values (here, string constants):
		In myIn = new In(new ADQLColumn("typeObj"), new ADQLOperand[]{new StringConstant("galaxy"),new StringConstant("star"),new StringConstant("planet"),new StringConstant("nebula")}, true);
		// check the ADQL:
		assertEquals("typeObj NOT IN ('galaxy' , 'star' , 'planet' , 'nebula')", myIn.toADQL());
		// check the SQL translation:
		try{
			assertEquals(myIn.toADQL(), translator.translate(myIn));
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded because the IN statement is correct and theoretically well supported by the POSTGRESQL translator!");
		}

		// Test with a sub-query:
		ADQLQuery subQuery = new ADQLQuery();

		ClauseSelect select = subQuery.getSelect();
		select.setDistinctColumns(true);
		select.setLimit(10);
		select.add(new ADQLColumn("typeObj"));

		subQuery.setFrom(new ADQLTable("Objects"));

		ADQLList<ADQLOrder> orderBy = subQuery.getOrderBy();
		orderBy.add(new ADQLOrder(1));

		myIn.setSubQuery(subQuery);
		// check the ADQL:
		assertEquals("typeObj NOT IN (SELECT DISTINCT TOP 10 typeObj\nFROM Objects\nORDER BY 1 ASC)", myIn.toADQL());
		// check the SQL translation:
		try{
			assertEquals("typeObj NOT IN (SELECT DISTINCT typeObj AS \"typeObj\"\nFROM Objects\nORDER BY 1 ASC\nLimit 10)", translator.translate(myIn));
		}catch(Exception ex){
			ex.printStackTrace();
			fail("This test should have succeeded because the IN statement is correct and theoretically well supported by the POSTGRESQL translator!");
		}

		// Test after replacement inside this IN statement:
		IReplaceHandler sHandler = new SimpleReplaceHandler(true){

			@Override
			public boolean match(ADQLObject obj){
				return (obj instanceof ADQLColumn) && ((ADQLColumn)obj).getColumnName().equals("typeObj");
			}

			@Override
			public ADQLObject getReplacer(ADQLObject objToReplace){
				return new ADQLColumn("type");
			}
		};
		sHandler.searchAndReplace(myIn);
		assertEquals(2, sHandler.getNbMatch());
		assertEquals(sHandler.getNbMatch(), sHandler.getNbReplacement());
		Iterator<ADQLObject> results = sHandler.iterator();
		while(results.hasNext())
			assertEquals("typeObj", results.next().toADQL());
		assertEquals("type NOT IN (SELECT DISTINCT TOP 10 type\nFROM Objects\nORDER BY 1 ASC)", myIn.toADQL());
	}

}
