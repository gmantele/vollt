package adql.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.FunctionDef;
import adql.parser.ParseException;
import adql.query.constraint.Comparison;
import adql.query.constraint.ComparisonOperator;
import adql.query.constraint.ConstraintsGroup;
import adql.query.from.ADQLTable;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.Concatenation;
import adql.query.operand.NumericConstant;
import adql.query.operand.Operation;
import adql.query.operand.OperationType;
import adql.query.operand.StringConstant;
import adql.query.operand.WrappedOperand;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.MathFunction;
import adql.query.operand.function.MathFunctionType;
import adql.query.operand.function.SQLFunction;
import adql.query.operand.function.SQLFunctionType;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;
import adql.search.IReplaceHandler;
import adql.search.ISearchHandler;
import adql.search.SearchColumnHandler;
import adql.search.SimpleReplaceHandler;

public class TestADQLQuery {
	private ADQLQuery query = null;
	private List<ADQLColumn> columns = new ArrayList<ADQLColumn>(8);
	private List<ADQLColumn> typeObjColumns = new ArrayList<ADQLColumn>(3);

	@Before
	public void setUp() {
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
	public void testADQLQuery() {
		assertEquals("SELECT (O.nameObj || ' (' || O.typeObj || ')') AS Nom objet , O.ra , O.dec\nFROM truc.ObsCore AS O\nWHERE ra/dec > 1 AND (typeObj = 'Star' OR typeObj LIKE 'Galaxy*')\nORDER BY 1 DESC", query.toADQL());
	}

	@Test
	public void testSearch() {
		ISearchHandler sHandler = new SearchColumnHandler(false);
		Iterator<ADQLObject> results = query.search(sHandler);
		assertEquals(columns.size(), sHandler.getNbMatch());
		for(ADQLColumn expectedCol : columns)
			assertEquals(expectedCol, results.next());
	}

	@Test
	public void testReplace() {
		IReplaceHandler sHandler = new SimpleReplaceHandler(false, false) {
			@Override
			protected boolean match(ADQLObject obj) {
				return (obj instanceof ADQLColumn) && (((ADQLColumn)obj).getColumnName().equalsIgnoreCase("typeObj"));
			}

			@Override
			public ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException {
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

	@Test
	public void testTypeResultingColumns() {
		ADQLQuery query = new ADQLQuery();
		query.setFrom(new ADQLTable("foo"));
		ClauseSelect select = new ClauseSelect();
		query.setSelect(select);

		// Test with a numeric constant:
		select.add(new NumericConstant(2.3));
		assertEquals(1, query.getResultingColumns().length);
		assertEquals(DBDatatype.UNKNOWN_NUMERIC, query.getResultingColumns()[0].getDatatype().type);

		// Test with a math operation:
		select.clear();
		select.add(new Operation(new Operation(new NumericConstant(2), OperationType.MULT, new NumericConstant(3.14)), OperationType.DIV, new NumericConstant(5)));
		assertEquals(1, query.getResultingColumns().length);
		assertEquals(DBDatatype.UNKNOWN_NUMERIC, query.getResultingColumns()[0].getDatatype().type);

		// Test with a math function:
		try {
			select.clear();
			select.add(new MathFunction(MathFunctionType.SQRT, new ADQLColumn("col1")));
			assertEquals(1, query.getResultingColumns().length);
			assertEquals(DBDatatype.UNKNOWN_NUMERIC, query.getResultingColumns()[0].getDatatype().type);
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("The mathematical function SQRT is well defined. This error should have occurred.");
		}

		// Test with an aggregation function:
		select.clear();
		select.add(new SQLFunction(SQLFunctionType.SUM, new ADQLColumn("col1")));
		assertEquals(1, query.getResultingColumns().length);
		assertEquals(DBDatatype.UNKNOWN_NUMERIC, query.getResultingColumns()[0].getDatatype().type);

		// Test with a string constant:
		select.clear();
		select.add(new StringConstant("blabla"));
		assertEquals(1, query.getResultingColumns().length);
		assertEquals(DBDatatype.VARCHAR, query.getResultingColumns()[0].getDatatype().type);

		// Test with a concatenation:
		select.clear();
		Concatenation concat = new Concatenation();
		concat.add(new StringConstant("super "));
		concat.add(new ADQLColumn("foo", "col"));
		select.add(concat);
		assertEquals(1, query.getResultingColumns().length);
		assertEquals(DBDatatype.VARCHAR, query.getResultingColumns()[0].getDatatype().type);

		// Test with a POINT:
		try {
			select.clear();
			select.add(new PointFunction(new StringConstant(""), new ADQLColumn("ra"), new ADQLColumn("dec")));
			select.add(new CentroidFunction(new GeometryValue<GeometryFunction>(new ADQLColumn("aRegion"))));
			assertEquals(2, query.getResultingColumns().length);
			for(int i = 0; i < 2; i++)
				assertEquals(DBDatatype.POINT, query.getResultingColumns()[i].getDatatype().type);
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("The POINT function is well defined. This error should have occurred.");
		}

		// Test with a REGION (CIRCLE, BOX, POLYGON and REGION functions):
		try {
			select.clear();
			select.add(new CircleFunction(new StringConstant(""), new ADQLColumn("ra"), new ADQLColumn("dec"), new NumericConstant(1)));
			select.add(new BoxFunction(new StringConstant(""), new ADQLColumn("ra"), new ADQLColumn("dec"), new NumericConstant(10), new NumericConstant(20)));
			ADQLOperand[] points = new ADQLOperand[6];
			points[0] = new ADQLColumn("point1");
			points[1] = new ADQLColumn("point2");
			points[2] = new ADQLColumn("point3");
			points[3] = new ADQLColumn("point4");
			points[4] = new ADQLColumn("point5");
			points[5] = new ADQLColumn("point6");
			select.add(new PolygonFunction(new StringConstant(""), points));
			select.add(new RegionFunction(new StringConstant("CIRCLE '' ra dec 2.3")));
			assertEquals(4, query.getResultingColumns().length);
			for(int i = 0; i < 4; i++)
				assertEquals(DBDatatype.REGION, query.getResultingColumns()[i].getDatatype().type);
		} catch(Exception ex) {
			ex.printStackTrace();
			fail("The geometrical functions are well defined. This error should have occurred.");
		}

		// Test with a UDF having no definition:
		select.clear();
		select.add(new DefaultUDF("foo", new ADQLOperand[0]));
		assertEquals(1, query.getResultingColumns().length);
		assertNull(query.getResultingColumns()[0].getDatatype());

		// Test with a UDF having a definition:
		try {
			select.clear();
			DefaultUDF udf = new DefaultUDF("foo", new ADQLOperand[0]);
			udf.setDefinition(new FunctionDef("foo", new DBType(DBDatatype.INTEGER)));
			select.add(udf);
			assertEquals(1, query.getResultingColumns().length);
			assertEquals(DBDatatype.INTEGER, query.getResultingColumns()[0].getDatatype().type);
		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Failed initialization because of an invalid UDF declaration! Cause: (cf console)");
		}
	}
}
