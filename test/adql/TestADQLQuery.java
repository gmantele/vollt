package adql;

import java.util.Iterator;

import adql.query.ADQLObject;
import adql.query.ADQLOrder;
import adql.query.ADQLQuery;
import adql.query.ClauseADQL;
import adql.query.ClauseConstraints;
import adql.query.ClauseSelect;
import adql.query.SelectItem;

import adql.query.constraint.Comparison;
import adql.query.constraint.ComparisonOperator;
import adql.query.constraint.ConstraintsGroup;

import adql.query.from.ADQLTable;

import adql.query.operand.ADQLColumn;
import adql.query.operand.Concatenation;
import adql.query.operand.NumericConstant;
import adql.query.operand.Operation;
import adql.query.operand.OperationType;
import adql.query.operand.StringConstant;
import adql.query.operand.WrappedOperand;

import adql.search.ISearchHandler;
import adql.search.SearchColumnHandler;

public class TestADQLQuery {
	public static final void main(String[] args) throws Exception{
		ADQLQuery query = new ADQLQuery();

		// SELECT:
		ClauseSelect select = query.getSelect();
		Concatenation concatObj = new Concatenation();
		concatObj.add(new ADQLColumn("O", "nameObj"));
		concatObj.add(new StringConstant(" ("));
		concatObj.add(new ADQLColumn("O", "typeObj"));
		concatObj.add(new StringConstant(")"));
		select.add(new SelectItem(new WrappedOperand(concatObj), "Nom objet"));
		select.add(new ADQLColumn("O", "ra"));
		select.add(new ADQLColumn("O", "dec"));

		// FROM:
		ADQLTable table = new ADQLTable("truc.ObsCore");
		table.setAlias("O");
		//		table.setJoin(new ADQLJoin(JoinType.INNER, new ADQLTable("VO")));
		query.setFrom(table);

		// WHERE:
		ClauseConstraints where = query.getWhere();
		where.add(new Comparison(new Operation(new ADQLColumn("ra"), OperationType.DIV, new ADQLColumn("dec")), ComparisonOperator.GREATER_THAN, new NumericConstant("1")));
		ConstraintsGroup constOr = new ConstraintsGroup();
		constOr.add(new Comparison(new ADQLColumn("typeObj"), ComparisonOperator.EQUAL, new StringConstant("Star")));
		constOr.add("OR", new Comparison(new ADQLColumn("typeObj"), ComparisonOperator.LIKE, new StringConstant("Galaxy*")));
		where.add("AND", constOr);

		// ORDER BY:
		ClauseADQL<ADQLOrder> orderBy = query.getOrderBy();
		orderBy.add(new ADQLOrder(1, true));

		System.out.println("*** QUERY ***\n" + query.toADQL());

		ISearchHandler sHandler = new SearchColumnHandler(false);
		Iterator<ADQLObject> results = query.search(sHandler);
		//		IReplaceHandler sHandler = new SimpleReplaceHandler(false, false) {
		//
		//			@Override
		//			protected boolean match(ADQLObject obj) {
		//				return (obj instanceof ADQLColumn) && (((ADQLColumn)obj).getColumnName().equalsIgnoreCase("typeObj"));
		//			}
		//
		//			@Override
		//			public ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException {
		//				return new ADQLColumn("NewTypeObj");
		//			}
		//
		//		};
		//		sHandler.searchAndReplace(query);
		//		System.out.println("INFO: "+sHandler.getNbReplacement()+"/"+sHandler.getNbMatch()+" replaced objects !");
		//		Iterator<ADQLObject> results = sHandler.iterator();
		System.out.println("\n*** SEARCH ALL COLUMNS ***");
		while(results.hasNext())
			System.out.println("\t- " + results.next().toADQL());

		System.out.println("\n*** QUERY ***\n" + query.toADQL());
	}
}
