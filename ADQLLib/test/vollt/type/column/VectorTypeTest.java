package vollt.type.column;

import org.junit.Test;

import static org.junit.Assert.*;

public class VectorTypeTest {

    @Test
    public void constructor_ShouldFail_WhenNoSubType(){
        assertThrows(NullPointerException.class, () -> new VectorType(null, "2"));
    }

    @Test
    public void constructor_ShouldFail_WhenNoArraysize(){
        final ScalarType typeChar = new TypeChar();
        assertThrows(NullPointerException.class, () -> new VectorType(typeChar, null));
    }

    @Test
    public void constructor_ShouldFail_WhenEmptyArraysize(){
        final ScalarType typeChar = new TypeChar();
        assertThrows(IllegalArgumentException.class, () -> new VectorType(typeChar, "  "));
    }


    @Test
    public void isArray() {
        // Given:
        final VectorType vector = new VectorType(new TypeInteger(), "*");

        // When + Then:
        assertTrue(vector.isArray());
    }

    @Test
    public void canMergeWith_ShouldSucceed_WhenExactlySameVectorType() {
        // Given:
        final VectorType vector1 = new VectorType(new TypeInteger(), "2");
        final VectorType vector2 = new VectorType(new TypeInteger(), "2");

        // When + Then:
        assertTrue(vector1.canMergeWith(vector2));
    }

    @Test
    public void canMergeWith_ShouldSucceed_WhenSimilarVectorType() {
        // Given:
        final VectorType vector1 = new VectorType(new TypeInteger(), "2");
        final VectorType vector2 = new VectorType(new TypeShort(), "2");

        // When + Then:
        assertTrue(vector1.canMergeWith(vector2));
    }

    @Test
    public void canMergeWith_ShouldFail_WhenOneIsVectorButNotTheSecond() {
        // Given:
        final VectorType vector1 = new VectorType(new TypeInteger(), "1");
        final TypeInteger typeInteger = new TypeInteger();

        // When + Then:
        assertFalse(vector1.canMergeWith(typeInteger));
    }

    @Test
    public void canMergeWith_ShouldFail_WhenSubTypesCanNotBeMerged() {
        // Given:
        final VectorType vector1 = new VectorType(new TypeInteger(), "2");
        final VectorType vector2 = new VectorType(new TypeChar(), "2");

        // When + Then:
        assertFalse(vector1.canMergeWith(vector2));
    }
}