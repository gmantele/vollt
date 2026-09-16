package vollt.type.column.jdbc;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (02/2025)
 */
public class TestJDBCColumnType {

    @Test
    public void constructor_ShouldExtractNoParameter_WhenNoneIsProvided(){
        // Given:
        final String typeName = "varchar";

        // When:
        final JDBCColumnType colType = new JDBCColumnType(typeName);

        // Then:
        assertEquals(typeName, colType.getFullName());
        assertEquals(typeName, colType.getSimpleName());
    }

    @Test
    public void constructor_ShouldExtractNoParameter_WhenNothingBetweenBrackets(){
        // Given:
        final String typeName = "varchar";
        final String fullName = typeName+"()";

        // When:
        final JDBCColumnType colType = new JDBCColumnType(fullName);

        // Then:
        assertEquals(fullName, colType.getFullName());
        assertEquals(typeName, colType.getSimpleName());
    }

    @Test
    public void constructor_ShouldExtractParameters_WhenSomeAreProvided(){
        // Given:
        final String typeName = "varchar";
        final String[] parameters = new String[]{"12", "34"};
        final String fullName = typeName+"("+String.join(", ", parameters)+")";

        // When:
        final JDBCColumnType colType = new JDBCColumnType(fullName);

        // Then:
        assertEquals(fullName, colType.getFullName());
        assertEquals(typeName, colType.getSimpleName());
        assertEquals(parameters.length, colType.getParameters().length);
        for(int i=0; i< parameters.length; i++)
            assertEquals(parameters[i], colType.getParameters()[i]);
    }

}