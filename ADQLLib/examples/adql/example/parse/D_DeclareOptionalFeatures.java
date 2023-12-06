package adql.example.parse;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.operand.function.string.LowerFunction;

/**
 * Example on how to support/unsupport some optional language features while
 * parsing an ADQL query.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 12/2023
 */
public class D_DeclareOptionalFeatures {

	public static void main(final String[] args) throws Throwable {

		// Input query:
		final String QUERY = "Select LOWER(name), ra || ' - ' || dec as \"Position\"\nFrom data;";
		System.out.println("\n    " + QUERY.replaceAll("\n", "\n    "));

		try {

			// Create a parser (supporting the notion of optional features):
			ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

			/*
			 * NOTES:
			 *   If none is specified in parameter of the constructor,
			 *   ADQLParser internally creates a default set of supported
			 *   features.
			 *
			 *   This default may change in function of the ADQL grammar
			 *   version. But generally, all features described in the version
			 *   of an ADQL grammar are by default all supported by ADQLParser.
			 *
			 *   See ADQLParser.setDefaultFeatures() to know the exact content
			 *   of the default features set created by ADQLParser.
			 *
			 *   To customize the list of supported features, you have 3
			 *   possibilities:
			 *
			 *     1. Create an instance of FeatureSet and its support(...)
			 *        and unsupport(...) functions to specify which features
			 *        are supported or not.
			 *        Then, give this FeatureSet in parameter when creating
			 *        an ADQLParser.
			 *
			 *     2. As in 1., create a custom FeatureSet and set it to an
			 *        existing ADQLParser instance using the function
			 *        ADQLParser.setSupportedFeatures().
			 *
			 *     3. Get the FeatureSet of an already created ADQLParser with
			 *        ADQLParser.getSupportedFeatures().
			 *        Then, customize it as in 1. and 2. (with support(...) and
			 *        unsupport(...) functions).
			 *
			 *   The functions FeatureSet.support(...) and
			 *   FeatureSet.unsupport(...) take an instance of LanguageFeature
			 *   in parameter. You DO NOT NEED to create a new one for each
			 *   feature you want to support/unsupport. All ADQLObject
			 *   extensions have a public static attribute named `FEATURE` of
			 *   type LanguageFeature. So if you know the name of the class
			 *   corresponding to your feature, you just have to use its
			 *   attribute FEATURE (as illustrated below).
			 */

			// Create an empty set of language features:
			FeatureSet features = new FeatureSet(false);

			// Forbid any undeclared UDF:
			parser.allowAnyUdf(false);

			// Support all available features:
			features.supportAll();

			// Just get the LanguageFeature for LOWER (DO NOT CREATE IT):
			final LanguageFeature lowerFeature = LowerFunction.FEATURE;

			// Ensures LOWER is now supported:
			if (!parser.getSupportedFeatures().isSupporting(lowerFeature))
				throw new Error("This example can not work properly. LOWER seems to be NOT supported in the default FeatureSet of ADQLParser. To fix this example, pick a different optional features.");

			// BUT for our example, now un-support it:
			parser.getSupportedFeatures().unsupport(lowerFeature);

			// Parse the query:
			parser.parseQuery(QUERY);

			System.err.println("\n((X)) This message should never be visible! ((X)");

		} catch(UnresolvedIdentifiersException ex) {

			System.out.println("\n((X))) INCORRECT QUERY! Cause: " + ex.getNbErrors() + " unsupported features! ((X))");
			for(ParseException pe : ex) {
				System.out.println("  - " + pe.getPosition() + " " + pe.getMessage());
			}

		} catch(ParseException ex) {

			System.out.println("\n((X)) INCORRECT QUERY! " + ex.getClass().getSimpleName() + " ((X))\n" + ex.getPosition() + " " + ex.getMessage());

		}

	}

}
