package vollt_examples.adql.parse;

import adql.db.FunctionDef;
import adql.parser.ADQLParser;
import adql.parser.feature.FeatureSet;
import adql.parser.grammar.ParseException;
import adql.query.ADQLQuery;

/**
 * Examples and explanations about how to declare UDF.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2019
 */
public class E_DeclareUDF {

	public static void main(final String[] args) {

		// Input query:
		final String QUERY = "Select MY_UDF(name)\nFrom data";

		////////////////////////////////////////////////////////////////////////
		//           CASE 1/3: Default = any undeclared UDF allowed           //
		////////////////////////////////////////////////////////////////////////

		System.out.println("\n##### DEFAULT = ANY UNDECLARED UDF ALLOWED #####\n\n    " + QUERY.replaceAll("\n", "\n    "));

		try {

			// Create the parser:
			ADQLParser parser = new ADQLParser();

			// Parse the query:
			ADQLQuery query = parser.parseQuery(QUERY);

			System.out.println("\n((i)) Correct ADQL query ((i))");

			System.out.println("\n((i)) As interpreted: ((i))\n    " + query.toADQL().replaceAll("\n", "\n    "));

		}
		// 3. EVENTUALLY DEAL WITH ERRORS:
		catch(ParseException ex) {
			System.out.println("\n((X)) INCORRECT QUERY! " + ex.getClass().getSimpleName() + " ((X))\n" + ex.getMessage());
		}

		////////////////////////////////////////////////////////////////////////
		//                CASE 2/3: No undeclared UDF allowed                 //
		////////////////////////////////////////////////////////////////////////

		System.out.println("\n##### NO UNDECLARED UDF ALLOWED #####\n\n    " + QUERY.replaceAll("\n", "\n    "));

		try {

			// Create the parser:
			ADQLParser parser = new ADQLParser();

			// FORBID ALL UNDECLARED UDF:
			parser.getSupportedFeatures().allowAnyUdf(false);

			// Parse the query:
			ADQLQuery query = parser.parseQuery(QUERY);

			System.out.println("\n((i)) Correct ADQL query ((i))");

			System.out.println("\n((i)) As interpreted: ((i))\n    " + query.toADQL().replaceAll("\n", "\n    "));

		}
		// 3. EVENTUALLY DEAL WITH ERRORS:
		catch(ParseException ex) {
			System.out.println("\n((X)) INCORRECT QUERY! " + ex.getClass().getSimpleName() + " ((X))\n" + ex.getMessage());
		}

		////////////////////////////////////////////////////////////////////////
		//                      CASE 3/3: Declare a UDF                       //
		////////////////////////////////////////////////////////////////////////

		System.out.println("\n##### DECLARE A UDF #####\n\n    " + QUERY.replaceAll("\n", "\n    "));

		try {

			// Create the parser:
			ADQLParser parser = new ADQLParser();

			// FORBID ALL UNDECLARED UDF:
			FeatureSet features = parser.getSupportedFeatures();
			features.allowAnyUdf(false);

			// DECLARE A UDF:
			// ...define this function:
			FunctionDef myUdf = FunctionDef.parse("my_udf(param1 VARCHAR) -> VARCHAR", parser.getADQLVersion());
			// ...now add it to the supported features:
			if (!features.support(myUdf.toLanguageFeature()))
				throw new Error("Impossible to support the UDF `" + myUdf + "`! This is the important point of this example file.");

			// Parse the query:
			ADQLQuery query = parser.parseQuery(QUERY);

			System.out.println("\n((i)) Correct ADQL query ((i))");

			System.out.println("\n((i)) As interpreted: ((i))\n    " + query.toADQL().replaceAll("\n", "\n    "));

		}
		// 3. EVENTUALLY DEAL WITH ERRORS:
		catch(ParseException ex) {
			System.out.println("\n((X)) INCORRECT QUERY! " + ex.getClass().getSimpleName() + " ((X))\n" + ex.getMessage());
		}

	}

}
