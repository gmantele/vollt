package adql.query.constraint;

import adql.query.operand.ADQLColumn;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.geometry.PointFunction;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestComparison {

    @Test
    public void testSetRightOperand_Null(){
        try {
            new Comparison(new PointFunction(null, new NumericConstant(1), new NumericConstant(2)), ComparisonOperator.EQUAL, null);
            fail("NullPointerException expected: there is no right operand to this equality comparison.");
        }catch(Exception t){
            assertEquals(NullPointerException.class, t.getClass());
            assertEquals("Impossible to update the right operand of the comparison (POINT('', 1, 2) NULL NULL) with a NULL operand!", t.getMessage());
        }
    }

    @Test
    public void testSetRightOperand_Ok(){
        try {
            new Comparison(new PointFunction(null, new NumericConstant(1), new NumericConstant(2)), ComparisonOperator.EQUAL, new PointFunction(null, new NumericConstant(3), new NumericConstant(4)));
        }catch(Exception t){
            t.printStackTrace();
            fail("Success expected: it is perfectly right to compare a POINT to another POINT.");
        }
    }

    @Test
    public void testSetRightOperand_OkWithUnknownType(){
        try {
            new Comparison(new PointFunction(null, new NumericConstant(1), new NumericConstant(2)), ComparisonOperator.EQUAL, new ADQLColumn("ra"));
        }catch(Exception t){
            t.printStackTrace();
            fail("Success expected: it is perfectly right to compare a POINT to a column.");
        }
    }

    @Test
    public void testSetRightOperand_TypeIncompatibility(){
        try {
            new Comparison(new PointFunction(null, new NumericConstant(1), new NumericConstant(2)), ComparisonOperator.EQUAL, new NumericConstant(3));
            fail("UnsupportedOperationException expected: impossible to compare a POINT with a numeric.");
        }catch(Exception t){
            assertEquals(UnsupportedOperationException.class, t.getClass());
            assertEquals("Impossible to update the right operand of the comparison (POINT('', 1, 2) NULL NULL) with \"3\" because its type is not compatible with the type of the left operand!", t.getMessage());
        }
    }

    @Test
    public void testSetRightOperand_TypeIncompatibilityWithLIKE(){
        try {
            new Comparison(new StringConstant("Hello"), ComparisonOperator.LIKE, new NumericConstant(3));
            fail("UnsupportedOperationException expected: impossible to use LIKE between a String and a numeric.");
        }catch(Exception t){
            assertEquals(UnsupportedOperationException.class, t.getClass());
            assertEquals("Impossible to update the right operand of the comparison ('Hello' NULL NULL) with \"3\" because its type is not compatible with the type of the left operand!", t.getMessage());
        }
    }

}
