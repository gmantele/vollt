package adql.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.grammar.ParseException;
import adql.query.constraint.Comparison;
import adql.query.from.ADQLJoin;
import adql.query.from.ADQLTable;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.function.ADQLFunction;
import adql.search.SimpleSearchHandler;

public class TestADQLObjectPosition {

	@Before
	public void setUp() {

	}

	@Test
	public void testPositionInAllClauses() {
		for(ADQLVersion version : ADQLVersion.values()) {
			try {
				ADQLQuery query = new ADQLParser(version).parseQuery((version != ADQLVersion.V2_0 ? "WITH bar AS (SELECT * FROM superbar) " : "") + "SELECT truc, bidule.machin, toto(truc, chose) AS \"super\" FROM foo JOIN bidule USING(id) WHERE truc > 12.5 AND bidule.machin < 5 GROUP BY chose HAVING try > 0 ORDER BY chouetteAlors, 2 DESC");

				Iterator<ADQLObject> results = query.search(new SimpleSearchHandler(true) {
					@Override
					protected boolean match(ADQLObject obj) {
						if (obj instanceof ADQLList<?> && ((ADQLList<?>)obj).isEmpty())
							return false;
						else
							return obj.getPosition() == null;
					}
				});
				if (results.hasNext()) {
					System.err.println("OBJECT WITH NO DEFINED POSITION in ADQL-" + version + ":");
					while(results.hasNext()) {
						ADQLObject r = results.next();
						System.err.println("    * " + r.toADQL() + " {" + r.getClass().getSimpleName() + "}");
					}
					fail("At least one item of the generated ADQL tree does not have a position information! (see System.err for more details)");
				}
			} catch(ParseException pe) {
				pe.printStackTrace();
				fail("No error should have occured here: the ADQL query is syntactically correct!");
			}
		}
	}

	private void assertEquality(final TextPosition expected, final TextPosition realPos) {
		assertEquals(expected.beginLine, realPos.beginLine);
		assertEquals(expected.beginColumn, realPos.beginColumn);
		assertEquals(expected.endLine, realPos.endLine);
		assertEquals(expected.endColumn, realPos.endColumn);
	}

	@Test
	public void testPositionAccuracy() {
		for(ADQLVersion version : ADQLVersion.values()) {
			try {
				ADQLQuery query = new ADQLParser(version).parseQuery("SELECT TOP 1000 oid FROM foo JOIN bar USING(oid)\nWHERE foo || toto = 'truc'\n      AND 2 > 1+0 GROUP BY oid HAVING COUNT(oid) > 10\n\tORDER BY 1 DESC");
				// Test SELECT
				assertEquality(new TextPosition(1, 1, 1, 20), query.getSelect().getPosition());
				// Test ADQLColumn (here: "oid")
				assertEquality(new TextPosition(1, 17, 1, 20), query.getSelect().get(0).getPosition());
				// Test FROM & ADQLJoin
				/* NB: The clause FROM is the only one which is not a list but a single item of type FromContent (JOIN or table).
				 *     That's why, it is not possible to get its exact starting position ('FROM') ; the starting position is
				 *     the one of the first table of the clause FROM. */
				assertEquality(new TextPosition(1, 26, 1, 49), query.getFrom().getPosition());
				// Test ADQLTable
				List<ADQLTable> tables = query.getFrom().getTables();
				assertEquality(new TextPosition(1, 26, 1, 29), tables.get(0).getPosition());
				assertEquality(new TextPosition(1, 35, 1, 38), tables.get(1).getPosition());
				// Test the join condition:
				Iterator<ADQLColumn> itCol = ((ADQLJoin)query.getFrom()).getJoinedColumns();
				assertEquality(new TextPosition(1, 45, 1, 48), itCol.next().getPosition());
				// Test WHERE
				assertEquality(new TextPosition(2, 1, 3, 18), query.getWhere().getPosition());
				// Test COMPARISON = CONSTRAINT
				Comparison comp = (Comparison)(query.getWhere().get(0));
				assertEquality(new TextPosition(2, 7, 2, 27), comp.getPosition());
				// Test left operand = concatenation:
				ADQLOperand operand = comp.getLeftOperand();
				assertEquality(new TextPosition(2, 7, 2, 18), operand.getPosition());
				Iterator<ADQLObject> itObj = operand.adqlIterator();
				// foo
				assertEquality(new TextPosition(2, 7, 2, 10), itObj.next().getPosition());
				// toto
				assertEquality(new TextPosition(2, 14, 2, 18), itObj.next().getPosition());
				// Test right operand = string:
				operand = comp.getRightOperand();
				assertEquality(new TextPosition(2, 21, 2, 27), operand.getPosition());
				// Test COMPARISON > CONSTRAINT:
				comp = (Comparison)(query.getWhere().get(1));
				assertEquality(new TextPosition(3, 11, 3, 18), comp.getPosition());
				// Test left operand = numeric:
				operand = comp.getLeftOperand();
				assertEquality(new TextPosition(3, 11, 3, 12), operand.getPosition());
				// Test right operand = operation:
				operand = comp.getRightOperand();
				assertEquality(new TextPosition(3, 15, 3, 18), operand.getPosition());
				itObj = operand.adqlIterator();
				// 1
				assertEquality(new TextPosition(3, 15, 3, 16), itObj.next().getPosition());
				// 0
				assertEquality(new TextPosition(3, 17, 3, 18), itObj.next().getPosition());
				// Test GROUP BY
				assertEquality(new TextPosition(3, 19, 3, 31), query.getGroupBy().getPosition());
				// oid
				assertEquality(new TextPosition(3, 28, 3, 31), query.getGroupBy().get(0).getPosition());
				// Test HAVING
				assertEquality(new TextPosition(3, 32, 3, 54), query.getHaving().getPosition());
				// Test COMPARISON > CONSTRAINT:
				comp = (Comparison)(query.getHaving().get(0));
				assertEquality(new TextPosition(3, 39, 3, 54), comp.getPosition());
				// Test left operand = COUNT function:
				operand = comp.getLeftOperand();
				assertEquality(new TextPosition(3, 39, 3, 49), operand.getPosition());
				// Test parameter = ADQLColumn oid:
				assertEquality(new TextPosition(3, 45, 3, 48), ((ADQLFunction)operand).getParameter(0).getPosition());
				// Test right operand = operation:
				operand = comp.getRightOperand();
				assertEquality(new TextPosition(3, 52, 3, 54), operand.getPosition());
				// Test ORDER BY
				assertEquality(new TextPosition(4, 9, 4, 24), query.getOrderBy().getPosition());
				// Test ORDER BY item:
				assertEquality(new TextPosition(4, 18, 4, 24), query.getOrderBy().get(0).getPosition());
				// Test column index:
				assertEquality(new TextPosition(4, 18, 4, 19), query.getOrderBy().get(0).getColumnReference().getPosition());

			} catch(ParseException pe) {
				System.err.println("ERROR IN THE ADQL-" + version + " QUERY AT " + pe.getPosition());
				pe.printStackTrace();
				fail("No error should have occured here: the ADQL query is syntactically correct!");
			}
		}
	}

}
