package adql.example.parse;

import adql.parser.ADQLParser;
import adql.parser.grammar.ParseException;
import adql.query.ADQLSet;

/**
 * Minimal lines required to parse an ADQL query.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2019
 */
public class A_SimpleQueryParsing {

	public static void main(final String[] args) {

		// Input query:
		final String QUERY = "Select name, ra || ' - ' || dec as \"Position\"\nFrom data\nWhere Contains(Point('ICRS', ra, dec), Circle('ICRS', 10, 5, 1)) = 1\nOrder By name;";
		System.out.println("\n    " + QUERY.replaceAll("\n", "\n    "));

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

			// 2. PARSE AN ADQL QUERY:
			ADQLSet query = parser.parseQuery(QUERY);

			System.out.println("\n((i)) Correct ADQL query ((i))");

			System.out.println("\n((i)) As interpreted: ((i))\n    " + query.toADQL().replaceAll("\n", "\n    "));

		}
		// 3. EVENTUALLY DEAL WITH ERRORS:
		catch(ParseException ex) {
			System.out.println("\n((X)) INCORRECT QUERY! " + ex.getClass().getSimpleName() + " ((X))\n" + ex.getMessage());
		}

	}

}
