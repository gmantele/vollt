package adql.parser.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.FunctionDef;
import adql.db.FunctionDef.FunctionParam;

public class TestLanguageFeature {

	@Test
	public void testLanguageFeatureStringStringBooleanString() {

		// CASE: No form/name => Error!
		final String[] incorrectNames = new String[]{ null, "", "  " };
		for(String n : incorrectNames) {
			try {
				new LanguageFeature(null, n, false, null);
				fail("A language feature should not be created without a name!");
			} catch(Exception ex) {
				assertEquals(NullPointerException.class, ex.getClass());
				assertEquals("Missing form/name of the language feature to create!", ex.getMessage());
			}
		}

		// CASE: with a name only
		LanguageFeature feat = new LanguageFeature(null, "FOO", false, null);
		assertEquals("!FOO", feat.id);
		assertNull(feat.type);
		assertEquals("FOO", feat.form);
		assertFalse(feat.optional);
		assertNull(feat.description);

		// CASE: name with spaces before and after => should be removed
		feat = new LanguageFeature(null, "	FOO  ", false, "");
		assertEquals("!FOO", feat.id);
		assertNull(feat.type);
		assertEquals("FOO", feat.form);
		assertFalse(feat.optional);
		assertNull(feat.description);

		// CASE: optional feature
		feat = new LanguageFeature(null, "FOO(VARCHAR) -> BOOLEAN", true, "  ");
		assertEquals("!FOO(VARCHAR) -> BOOLEAN", feat.id);
		assertNull(feat.type);
		assertNull(feat.description);
		assertEquals("FOO(VARCHAR) -> BOOLEAN", feat.form);
		assertTrue(feat.optional);

		// CASE: with a description (trimmed)
		feat = new LanguageFeature(null, "FOO", true, "	Bla bla  ");
		assertEquals("!FOO", feat.id);
		assertNull(feat.type);
		assertEquals("FOO", feat.form);
		assertTrue(feat.optional);
		assertEquals("Bla bla", feat.description);

		// CASE: with a type (trimmed)
		feat = new LanguageFeature("	ivo://stuff  ", "FOO", true, "	Bla bla  ");
		assertEquals("ivo://stuff!FOO", feat.id);
		assertEquals("ivo://stuff", feat.type);
		assertEquals("FOO", feat.form);
		assertTrue(feat.optional);
		assertEquals("Bla bla", feat.description);
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void testEquals() {
		// CASE: equality test with NULL => false
		LanguageFeature feat1 = new LanguageFeature(null, "FOO", false, null);
		assertFalse(feat1.equals(null));

		// CASE: equality test with a different type of object returning the same toString() => false
		assertFalse(feat1.equals(new String("!FOO")));

		// CASE: equality test with a different case => false
		LanguageFeature feat2 = new LanguageFeature(null, "Foo", false, null);
		assertFalse(feat1.equals(feat2));

		// CASE: equality test with a different type => false
		feat2 = new LanguageFeature("ivo://stuff", "FOO", false, null);
		assertFalse(feat1.equals(feat2));

		// CASE: equality test with itself => true
		assertTrue(feat1.equals(feat1));
		assertTrue(feat2.equals(feat2));

		// CASE: equality test with a different instance having the same name and type => true
		feat2 = new LanguageFeature(null, "FOO", false, null);
		assertTrue(feat1.equals(feat2));
		// even with a different optional flag or description
		feat1 = new LanguageFeature("ivo://stuff", "FOO(VARCHAR) -> BOOLEAN", false, "Blab bla");
		feat2 = new LanguageFeature("ivo://stuff", "FOO(VARCHAR) -> BOOLEAN", true, null);
		assertTrue(feat1.equals(feat2));
		// the test order does not matter:
		assertTrue(feat2.equals(feat1));

		try {

			// CASE: equality test between 2 UDFs with a different name => false
			LanguageFeature udfFeat1 = new LanguageFeature(new FunctionDef("toto", new DBType(DBDatatype.CHAR)));
			LanguageFeature udfFeat2 = new LanguageFeature(new FunctionDef("tata", new DBType(DBDatatype.CHAR)));
			assertFalse(udfFeat1.equals(udfFeat2));

			// CASE: equality test between 2 UDFs with same name and no parameter => true
			udfFeat2 = new LanguageFeature(new FunctionDef(udfFeat1.udfDefinition.name, new DBType(DBDatatype.CHAR)));
			assertTrue(udfFeat1.equals(udfFeat2));

			// CASE: equality test between 2 UDFs with same name but different number of parameters => false
			udfFeat1 = new LanguageFeature(new FunctionDef("toto", new DBType(DBDatatype.CHAR)));
			udfFeat2 = new LanguageFeature(new FunctionDef(udfFeat1.udfDefinition.name, new DBType(DBDatatype.CHAR), new FunctionParam[]{ new FunctionParam("param1", new DBType(DBDatatype.CHAR)) }));
			assertFalse(udfFeat1.equals(udfFeat2));

			// CASE: equality test between 2 UDFs with same name and same number of parameters but of different types => true
			udfFeat1 = new LanguageFeature(new FunctionDef("toto", new DBType(DBDatatype.CHAR), new FunctionParam[]{ new FunctionParam("param1", new DBType(DBDatatype.BIGINT)) }));
			udfFeat2 = new LanguageFeature(new FunctionDef(udfFeat1.udfDefinition.name, new DBType(DBDatatype.CHAR), new FunctionParam[]{ new FunctionParam("param1", new DBType(DBDatatype.CHAR)) }));
			assertTrue(udfFeat1.equals(udfFeat2));

			// CASE: equality test between 2 UDFs with same name and same number of parameters of same types => true
			udfFeat1 = new LanguageFeature(new FunctionDef("toto", new DBType(DBDatatype.CHAR), new FunctionParam[]{ new FunctionParam("param1", new DBType(DBDatatype.CHAR)) }));
			udfFeat2 = new LanguageFeature(new FunctionDef(udfFeat1.udfDefinition.name, new DBType(DBDatatype.CHAR), new FunctionParam[]{ new FunctionParam("param1", new DBType(DBDatatype.CHAR)) }));
			assertTrue(udfFeat1.equals(udfFeat2));

		} catch(Exception ex) {
			ex.printStackTrace();
			fail("Unexpected error! There is apparently a problem with the FunctionDef declaration....it should not be.");
		}
	}
}
