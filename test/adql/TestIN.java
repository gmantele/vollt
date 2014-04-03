package adql;

import java.util.Iterator;

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

import adql.translator.PostgreSQLTranslator;

public class TestIN {

	public static void main(String[] args) throws Exception {
		In myIn = new In(new ADQLColumn("typeObj"), new ADQLOperand[]{new StringConstant("galaxy"), new StringConstant("star"), new StringConstant("planet"), new StringConstant("nebula")}, true);
		System.out.println(myIn.getName()+": "+myIn.toADQL());

		ADQLQuery subQuery = new ADQLQuery();

		ClauseSelect select = subQuery.getSelect();
		select.setDistinctColumns(true);
		select.setLimit(10);
		select.add(new ADQLColumn("typeObj"));

		subQuery.setFrom(new ADQLTable("Objects"));

		ADQLList<ADQLOrder> orderBy = subQuery.getOrderBy();
		orderBy.add(new ADQLOrder(1));

		myIn.setSubQuery(subQuery);
		System.out.println("\n*** "+myIn.getName().toUpperCase()+" ***\n"+myIn.toADQL());
		PostgreSQLTranslator translator = new PostgreSQLTranslator();
		System.out.println("\n*** SQL TRANSLATION ***\n"+translator.translate(myIn));

		IReplaceHandler sHandler = new SimpleReplaceHandler(true) {

			@Override
			public boolean match(ADQLObject obj) {
				return (obj instanceof ADQLColumn) && ((ADQLColumn)obj).getColumnName().equals("typeObj");
			}

			@Override
			public ADQLObject getReplacer(ADQLObject objToReplace) {
				return new ADQLColumn("type");
			}
		};
		sHandler.searchAndReplace(myIn);
		System.out.println("INFO: "+sHandler.getNbReplacement()+"/"+sHandler.getNbMatch()+" replaced objects !");
		Iterator<ADQLObject> results = sHandler.iterator();
		System.out.println("\n*** SEARCH RESULTS ***");
		while(results.hasNext())
			System.out.println("\t- "+results.next());

		System.out.println("\n*** AFTER REPLACEMENT ***\n"+myIn.toADQL());
	}

}
