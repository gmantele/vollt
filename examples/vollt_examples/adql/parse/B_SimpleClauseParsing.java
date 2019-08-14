package vollt_examples.adql.parse;

import adql.parser.ADQLParser;
import adql.parser.grammar.ParseException;
import adql.query.ClauseConstraints;

/**
 * Minimal lines required to parse an ADQL clause (here: <code>WHERE</code>).
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2019
 */
public class B_SimpleClauseParsing {

	public static void main(final String[] args) {

		// Input ADQL expression:
		final String CLAUSE = "Where Contains(Point('ICRS', ra, dec), Circle('ICRS', 10, 5, 1)) = 1";
		System.out.println("\n    " + CLAUSE);

		try {

			// 1. CREATE A PARSER:
			ADQLParser parser = new ADQLParser();
			/*
			 * To create a parser of a specific version of the ADQL grammar:
			 *
			 *   import adql.parser.ADQLParser.ADQLVersion;
			 *   [...]
			 *   ADQLParser parser = new ADQLParser(ADQLVersion.V2_0);
			 *
			 */

			// 2. PARSE A SPECIFIC ADQL CLAUSE:
			ClauseConstraints constraints = parser.parseWhere(CLAUSE);

			System.out.println("\n((i)) Correct WHERE clause ((i))");

			System.out.println("\n((i)) As interpreted: `" + constraints.toADQL() + "` ((i))");

		}
		// 3. EVENTUALLY DEAL WITH ERRORS:
		catch(ParseException ex) {
			System.out.println("\n((X)) INCORRECT CLAUSE! " + ex.getClass().getSimpleName() + " ((X))\n" + ex.getPosition() + " " + ex.getMessage());
		}

	}

}
