package adql.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import adql.parser.grammar.ParseException;

public class TestQuickFixer {

	@Test
	public void testFix() {
		final ADQLParser parser = new ADQLParser();
		QueryFixer fixer = parser.getQuickFixer();

		try {
			/* CASE: Nothing to fix => exactly the same as provided */
			// raw ASCII query with perfectly regular ADQL identifiers:
			assertEquals("SELECT foo, bar FROM aTable", fixer.fix("SELECT foo, bar FROM aTable"));
			// same with \n, \r and \t (replaced by 4 spaces):
            String fixed = fixer.fix("SELECT foo,\r\n\tbar\nFROM aTable");
            String expected = "SELECT foo," + System.lineSeparator() + "    bar" + System.lineSeparator() + "FROM aTable";
            assertEquals(expected.replace("\r\n", "\n").trim(), fixed.replace("\r\n", "\n").trim());
			// still ASCII query with delimited identifiers and ADQL functions:
            fixed = fixer.fix("SELECT \"foo\",\r\n\t\"_bar\", AVG(col1)\nFROM \"public\".aTable");
            expected = "SELECT \"foo\"," + System.lineSeparator() + "    \"_bar\", AVG(col1)" + System.lineSeparator() + "FROM \"public\".aTable";
            assertEquals(expected.replace("\r\n", "\n").trim(), fixed.replace("\r\n", "\n").trim());

			/* CASE: Unicode confusable characters => replace by their ASCII alternative */
			assertEquals("SELECT \"_bar\" FROM aTable", fixer.fix("SELECT \"\uFE4Dbar\" FROM aTable"));

			/* CASE: incorrect regular identifier */
			assertEquals("SELECT \"_bar\" FROM aTable", fixer.fix("SELECT _bar FROM aTable"));
			assertEquals("SELECT \"_bar\" FROM aTable", fixer.fix("SELECT \uFE4Dbar FROM aTable"));
			assertEquals("SELECT \"2mass_id\" FROM aTable", fixer.fix("SELECT 2mass_id FROM aTable"));
			assertEquals("SELECT \"col?\" FROM aTable", fixer.fix("SELECT col? FROM aTable"));
			assertEquals("SELECT \"col[2]\" FROM aTable", fixer.fix("SELECT col[2] FROM aTable"));

			/* CASE: SQL reserved keyword */
			assertEquals("SELECT \"date\", \"year\", \"user\" FROM \"public\".aTable", fixer.fix("SELECT date, year, user FROM public.aTable"));

			/* CASE: ADQL function name without parameters list */
			assertEquals("SELECT \"count\", \"distance\" FROM \"schema\".aTable", fixer.fix("SELECT count, distance FROM schema.aTable"));

			/* CASE: a nice combination of everything (with comments at beginning, middle and end) */
            fixed = fixer.fix("-- begin comment\r\nSELECT id, \uFE4Draj2000, distance, (date),\r\tmin,min(mag), \"_dej2000\" -- in-between commment\nFROM public.mytable -- end comment");
            expected = "-- begin comment" + System.getProperty("line.separator") + "SELECT id, \"_raj2000\", \"distance\", (\"date\")," + System.getProperty("line.separator") + "    \"min\",min(mag), \"_dej2000\" -- in-between commment" + System.getProperty("line.separator") + "FROM \"public\".mytable -- end comment";
            assertEquals(expected.replace("\r\n", "\n").trim(), fixed.replace("\r\n", "\n").trim());

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected parsing error! This query should have passed. (see console for more details)");
		}
	}

}
