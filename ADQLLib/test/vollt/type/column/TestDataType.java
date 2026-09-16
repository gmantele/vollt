package vollt.type.column;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestDataType {

    @Test
    public void isUnknown_ShouldSucceed_WhenNULL() {
        assertTrue(ColumnType.isUnknown(null));
    }

    @Test
    public void isUnknown_ShouldSucceed_WhenUnknown(){
        assertTrue(ColumnType.isUnknown(new UnknownType()));
    }

    @Test
    public void isUnknown_ShouldSucceed_WhenUnknownNumeric(){
        assertTrue(ColumnType.isUnknown(new UnknownNumericType()));
    }

    @Test
    public void isUnknown_ShouldFail_WhenAResolvedType(){
        assertFalse(ColumnType.isUnknown(new TypeChar()));
    }

}