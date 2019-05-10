package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.parser.ADQLParser;
import adql.parser.ADQLParserFactory;
import adql.parser.ParseException;
import adql.query.ADQLQuery;

public class TestMySQLTranslator {

	@Test
	public void testConcat() {
		try {
			ADQLParser parser = ADQLParserFactory.createDefaultParser();
			MySQLTranslator translator = new MySQLTranslator();

			// Test with an easy translation:
			ADQLQuery query = parser.parseQuery("SELECT 'abc' || ' ' || 'def' FROM aTable");
			assertEquals("SELECT CONCAT('abc', ' ', 'def') AS `concat`", translator.translate(query.getSelect()));

			// Test with an easy translation:
			query = parser.parseQuery("SELECT 'a||b||c' || ' ' || 'd+e|f' FROM aTable");
			assertEquals("SELECT CONCAT('a||b||c', ' ', 'd+e|f') AS `concat`", translator.translate(query.getSelect()));

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("The given ADQL query is completely correct. No error should have occurred while parsing it. (see the console for more details)");
		} catch(TranslationException te) {
			te.printStackTrace();
			fail("No error was expected from this translation. (see the console for more details)");
		}
	}

}
