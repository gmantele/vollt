package adql;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

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
import adql.search.IReplaceHandler;
import adql.search.ISearchHandler;
import adql.search.SearchColumnHandler;
import adql.search.SimpleReplaceHandler;

public class TestADQLQuery {
	private ADQLQuery query = null;
	private List<ADQLColumn> columns = new ArrayList<ADQLColumn>(8);
	private List<ADQLColumn> typeObjColumns = new ArrayList<ADQLColumn>(3);

	@Before
	public void setUp(){
		query = new ADQLQuery();
		columns.clear();
		typeObjColumns.clear();

		columns.add(new ADQLColumn("O", "nameObj")); // 0 = O.nameObj
		columns.add(new ADQLColumn("O", "typeObj")); // 1 = O.typeObj
		columns.add(new ADQLColumn("O", "ra"));      // 2 = O.ra
		columns.add(new ADQLColumn("O", "dec"));     // 3 = O.dec
		columns.add(new ADQLColumn("ra"));           // 4 = ra
		columns.add(new ADQLColumn("dec"));          // 5 = dec
		columns.add(new ADQLColumn("typeObj"));      // 6 = typeObj
		columns.add(new ADQLColumn("typeObj"));      // 7 = typeObj

		typeObjColumns.add(columns.get(1));
		typeObjColumns.add(columns.get(6));
		typeObjColumns.add(columns.get(7));

		// SELECT:
		ClauseSelect select = query.getSelect();
		Concatenation concatObj = new Concatenation();
		concatObj.add(columns.get(0)); // O.nameObj
		concatObj.add(new StringConstant(" ("));
		concatObj.add(columns.get(1)); // O.typeObj
		concatObj.add(new StringConstant(")"));
		select.add(new SelectItem(new WrappedOperand(concatObj), "Nom objet"));
		select.add(columns.get(2)); // O.ra
		select.add(columns.get(3)); // O.dec

		// FROM:
		ADQLTable table = new ADQLTable("truc.ObsCore");
		table.setAlias("O");
		//		table.setJoin(new ADQLJoin(JoinType.INNER, new ADQLTable("VO")));
		query.setFrom(table);

		// WHERE:
		ClauseConstraints where = query.getWhere();
		// ra/dec > 1
		where.add(new Comparison(new Operation(columns.get(4), OperationType.DIV, columns.get(5)), ComparisonOperator.GREATER_THAN, new NumericConstant("1")));
		ConstraintsGroup constOr = new ConstraintsGroup();
		// AND (typeObj == 'Star'
		constOr.add(new Comparison(columns.get(6), ComparisonOperator.EQUAL, new StringConstant("Star")));
		// OR typeObj LIKE 'Galaxy*')
		constOr.add("OR", new Comparison(columns.get(7), ComparisonOperator.LIKE, new StringConstant("Galaxy*")));
		where.add("AND", constOr);

		// ORDER BY:
		ClauseADQL<ADQLOrder> orderBy = query.getOrderBy();
		orderBy.add(new ADQLOrder(1, true));
	}

	@Test
	public void testADQLQuery(){
		assertEquals("SELECT (O.nameObj || ' (' || O.typeObj || ')') AS Nom objet , O.ra , O.dec\nFROM truc.ObsCore AS O\nWHERE ra/dec > 1 AND (typeObj = 'Star' OR typeObj LIKE 'Galaxy*')\nORDER BY 1 DESC", query.toADQL());
	}

	@Test
	public void testSearch(){
		ISearchHandler sHandler = new SearchColumnHandler(false);
		Iterator<ADQLObject> results = query.search(sHandler);
		assertEquals(columns.size(), sHandler.getNbMatch());
		for(ADQLColumn expectedCol : columns)
			assertEquals(expectedCol, results.next());
	}

	@Test
	public void testReplace(){
		IReplaceHandler sHandler = new SimpleReplaceHandler(false, false){
			@Override
			protected boolean match(ADQLObject obj){
				return (obj instanceof ADQLColumn) && (((ADQLColumn)obj).getColumnName().equalsIgnoreCase("typeObj"));
			}

			@Override
			public ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException{
				return new ADQLColumn("NewTypeObj");
			}
		};
		sHandler.searchAndReplace(query);
		assertEquals(typeObjColumns.size(), sHandler.getNbMatch());
		assertEquals(sHandler.getNbMatch(), sHandler.getNbReplacement());
		Iterator<ADQLObject> results = sHandler.iterator();
		for(ADQLColumn expectedCol : typeObjColumns)
			assertEquals(expectedCol, results.next());
		assertEquals("SELECT (O.nameObj || ' (' || NewTypeObj || ')') AS Nom objet , O.ra , O.dec\nFROM truc.ObsCore AS O\nWHERE ra/dec > 1 AND (NewTypeObj = 'Star' OR NewTypeObj LIKE 'Galaxy*')\nORDER BY 1 DESC", query.toADQL());
	}
}
