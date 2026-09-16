package vollt.type.column;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestColumnTypeFactory {

    @Test
    public void setVOTableTypeConverter_ShouldFail_WhenNull(){
        assertThrows(NullPointerException.class, ()->ColumnTypeFactory.setVOTableTypeConverter(null));
    }

}