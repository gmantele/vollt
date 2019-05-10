package adql.parser;

import java.io.IOException;
import java.io.InputStream;

import adql.db.exception.UnresolvedIdentifiersException;
import adql.query.ADQLQuery;
import adql.translator.PostgreSQLTranslator;
import adql.translator.TranslationException;

/**
 * Factory of ADQL parsers.
 *
 * <h3>ADQL versions</h3>
 *
 * <p>
 * 	It is able to deal with all versions of the ADQL grammar supported by this
 * 	library. All these versions are listed in the enumeration
 * 	{@link ADQLVersion}.
 * </p>
 *
 * <p>
 * 	To create a such factory, an ADQL version must be provided. If none is
 * 	given, the default one will be used (<i>see {@link #DEFAULT_VERSION}</i>).
 * </p>
 *
 * <h3>Runnable class</h3>
 *
 * <p>
 * 	This class includes a main function and thus, it can be executed directly.
 * 	Its execution allows to parse an ADQL query. Then, in function of the passed
 * 	parameters, it is possible to just check its syntax, translate it into SQL
 * 	or try to fix the query.
 * </p>
 * <p><i>
 * 	To get help about this program, just run it with the argument
 * 	<code>-h</code> or <code>--help</code>.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (04/2019)
 * @since 2.0
 */
public class ADQLParserFactory {

	/* **********************************************************************
	 *                         VERSION MANAGEMENT
	 * ********************************************************************** */

	/**
	 * Enumeration of all supported versions of the ADQL grammar.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.0 (04/2019)
	 * @since 2.0
	 */
	public static enum ADQLVersion {
		/** Version REC-2.0 - <a href="http://www.ivoa.net/documents/cover/ADQL-20081030.html">http://www.ivoa.net/documents/cover/ADQL-20081030.html</a>. */
		V2_0,
		/** Version PR-2.1 - <a href="http://www.ivoa.net/documents/ADQL/20180112/index.html">http://www.ivoa.net/documents/ADQL/20180112/index.html</a>. */
		V2_1; // TODO Move 2.1 as default when it becomes REC

		@Override
		public String toString() {
			return name().toLowerCase().replace('_', '.');
		}

		/** TODO JUnit for ADQLVersion.parse(String)
		 * Parse the given string as an ADQL version number.
		 *
		 * <p>This function should work with the following syntaxes:</p>
		 * <ul>
		 * 	<li><code>2.0</code></li>
		 * 	<li><code>2_0</code></li>
		 * 	<li><code>v2.0</code> or <code>V2.0</code></li>
		 * 	<li><code>v2_0</code> or <code>V2_0</code></li>
		 * </ul>
		 *
		 * @param str	String to parse as an ADQL version specification.
		 *
		 * @return	The identified ADQL version.
		 */
		public static ADQLVersion parse(String str) {
			if (str == null)
				return null;

			str = str.trim().toUpperCase();

			if (str.isEmpty())
				return null;

			if (str.charAt(0) != 'V')
				str = 'V' + str;

			try {
				return ADQLVersion.valueOf(str.replaceAll("\\.", "_"));
			} catch(IllegalArgumentException iae) {
				return null;
			}
		}
	}

	/** Version of the ADQL grammar to use when none is specified:
	 * {@link ADQLVersion#V2_0 2.0}. */
	public final static ADQLVersion DEFAULT_VERSION = ADQLVersion.V2_0; // TODO Move 2.1 as default when it becomes REC

	/**
	 * Get the list of all supported ADQL grammar versions.
	 *
	 * @return	List of all supported ADQL versions.
	 *
	 * @see ADQLVersion#values()
	 */
	public static ADQLVersion[] getSupportedVersions() {
		return ADQLVersion.values();
	}

	/**
	 * Build on the fly a human list of all supported ADQL grammar versions.
	 *
	 * <p><i><b>Example:</b> <code>v2.0</code>, <code>v2.1</code>.</i></p>
	 *
	 * @return	List of all supported ADQL versions.
	 */
	public static String getSupportedVersionsAsString() {
		StringBuilder buf = new StringBuilder();
		for(ADQLVersion v : ADQLVersion.values()) {
			if (buf.length() > 0)
				buf.append(", ");
			buf.append(v.toString());
			if (v == DEFAULT_VERSION)
				buf.append(" (default)");
		}
		return buf.toString();
	}

	/* **********************************************************************
	 *                          PARSER CREATION
	 * ********************************************************************** */

	/**
	 * Builds a parser whose the query to parse will have to be given as a
	 * String in parameter of
	 * {@link ADQLParser#parseQuery(java.lang.String) parseQuery(String)}.
	 */
	public final ADQLParser createParser() {
		return createParser(DEFAULT_VERSION);
	}

	/**
	 * Builds a parser whose the query to parse will have to be given as a
	 * String in parameter of
	 * {@link ADQLParser#parseQuery(java.lang.String) parseQuery(String)}.
	 *
	 * @param version	Version of the ADQL grammar that the parser must
	 *               	implement.
	 *               	<i>If NULL, the {@link #DEFAULT_VERSION} will be used.</i>
	 */
	public ADQLParser createParser(ADQLVersion version) {
		// Prevent the NULL value by setting the default version if necessary:
		if (version == null)
			version = DEFAULT_VERSION;

		// Create the appropriate parser in function of the specified version:
		switch(version) {
			case V2_0:
				return new ADQLParser200();
			case V2_1:
			default:
				return new ADQLParser201();
		}
	}

	/* **********************************************************************
	 *                       STATIC PARSER CREATION
	 * ********************************************************************** */

	/** Factory to use only when a default parser is asked without any
	 * {@link ADQLParserFactory} instance.
	 * @see #createDefaultParser() */
	private static volatile ADQLParserFactory defaultFactory = null;

	/**
	 * Create an ADQL parser with the default ADQL grammar version (see
	 * {@link #DEFAULT_VERSION}).
	 *
	 * @return	A new parser implementing the default version supported by this
	 *        	library.
	 */
	public final static ADQLParser createDefaultParser() {
		// Create the default factory, if not already done:
		if (defaultFactory == null) {
			synchronized (ADQLParserFactory.class) {
				if (defaultFactory == null)
					defaultFactory = new ADQLParserFactory();
			}
		}

		// Create a parser implementing the default version of the ADQL grammar:
		return defaultFactory.createParser(DEFAULT_VERSION);
	}

	/* **********************************************************************
	 *                           MAIN FUNCTION
	 * ********************************************************************** */

	/**
	* Parses the given ADQL query.
	*
	* <p>The result of the parsing depends of the parameters:</p>
	*
	* <p>
	*     <b>ONLY the syntax is checked: the query is NOT EXECUTED !</b>
	* </p>
	*/
	public static final void main(String[] args) throws Exception {
		final String USAGE = "Usage:\n    adqlParser.jar [-version=...] [-h] [-d] [-v] [-e] [-a|-s] [-f] [<FILE>|<URL>]\n\nNOTE: If no file or URL is given, the ADQL query is expected in the standard\n      input. This query must end with a ';' or <Ctrl+D>!\n\nParameters:\n    -version=...    : Set the version of the ADQL grammar to follow.\n                      It must be one among: " + getSupportedVersionsAsString() + "\n    -h or --help    : Display this help.\n    -v or --verbose : Print the main steps of the parsing\n    -d or --debug   : Print stack traces when a grave error occurs\n    -e or --explain : Explain the ADQL parsing (or Expand the parsing tree)\n    -a or --adql    : Display the understood ADQL query\n    -s or --sql     : Ask the SQL translation of the given ADQL query\n                      (SQL compatible with PostgreSQL)\n    -f or --try-fix : Try fixing the most common ADQL query issues before\n                      attempting to parse the query.\n\nReturn:\n    By default: nothing if the query is correct. Otherwise a message explaining\n                why the query is not correct is displayed.\n    With the -s option, the SQL translation of the given ADQL query will be\n    returned.\n    With the -a option, the ADQL query is returned as it has been understood.\n\nExit status:\n    0  OK !\n    1  Parameter error (missing or incorrect parameter)\n    2  File error (incorrect file/url, reading error, ...)\n    3  Parsing error (syntactic or semantic error)\n    4  Translation error (a problem has occurred during the translation of the\n       given ADQL query in SQL).";
		final String NEED_HELP_MSG = "Try -h or --help to get more help about the usage of this program.";
		final String urlRegex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

		ADQLParser parser;

		short mode = -1;
		String file = null;
		ADQLVersion version = DEFAULT_VERSION;
		boolean verbose = false, debug = false, explain = false, tryFix = false;

		// Parameters reading:
		for(int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-version=")) {
				String[] parts = args[i].split("=");
				if (parts.length <= 1) {
					System.err.println("((!)) Missing ADQL version! It must be one among: " + getSupportedVersionsAsString() + ". ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				}
				String strVersion = parts[1].replaceAll("\\.", "_");
				version = ADQLVersion.parse(strVersion);
				if (version == null) {
					System.err.println("((!)) Incorrect ADQL version: \"" + args[i].split("=")[1] + "\"! It must be one among: " + getSupportedVersionsAsString() + ". ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				}
			} else if (args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--debug"))
				debug = true;
			else if (args[i].equalsIgnoreCase("-v") || args[i].equalsIgnoreCase("--verbose"))
				verbose = true;
			else if (args[i].equalsIgnoreCase("-e") || args[i].equalsIgnoreCase("--explain"))
				explain = true;
			else if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--adql")) {
				if (mode != -1) {
					System.err.println("((!)) Too much parameter: you must choose between -s, -c, -a or nothing ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				} else
					mode = 1;
			} else if (args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--sql")) {
				if (mode != -1) {
					System.err.println("((!)) Too much parameter: you must choose between -s, -c, -a or nothing ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				} else
					mode = 2;
			} else if (args[i].equalsIgnoreCase("-f") || args[i].equalsIgnoreCase("--try-fix"))
				tryFix = true;
			else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help")) {
				System.out.println(USAGE);
				System.exit(0);
			} else if (args[i].startsWith("-")) {
				System.err.println("((!)) Unknown parameter: \"" + args[i] + "\" ((!))\u005cn" + NEED_HELP_MSG);
				System.exit(1);
			} else
				file = args[i].trim();
		}

		try {

			// Get the parser for the specified ADQL version:
			parser = (new ADQLParserFactory()).createParser(version);

			// Try fixing the query, if asked:
			InputStream in = null;
			if (tryFix) {
				if (verbose)
					System.out.println("((i)) Trying to automatically fix the query...");

				String query;
				try {
					// get the input stream...
					if (file == null || file.length() == 0)
						in = System.in;
					else if (file.matches(urlRegex))
						in = (new java.net.URL(file)).openStream();
					else
						in = new java.io.FileInputStream(file);

					// ...and try fixing the query:
					query = parser.tryQuickFix(in);
				} finally {
					// close the stream (if opened):
					if (in != null)
						in.close();
					in = null;
				}

				if (verbose)
					System.out.println("((i)) SUGGESTED QUERY:\n" + query);

				// Initialise the parser with this fixed query:
				in = new java.io.ByteArrayInputStream(query.getBytes());
			}
			// Otherwise, take the query as provided:
			else {
				// Initialise the parser with the specified input:
				if (file == null || file.length() == 0)
					in = System.in;
				else if (file.matches(urlRegex))
					in = (new java.net.URL(file)).openStream();
				else
					in = new java.io.FileInputStream(file);
			}

			// Enable/Disable the debugging in function of the parameters:
			parser.setDebug(explain);

			// Query parsing:
			try {
				if (verbose)
					System.out.print("((i)) Parsing ADQL query...");
				ADQLQuery q = parser.parseQuery(in);
				if (verbose)
					System.out.println("((i)) CORRECT ADQL QUERY ((i))");
				if (mode == 2) {
					PostgreSQLTranslator translator = new PostgreSQLTranslator();
					if (verbose)
						System.out.print("((i)) Translating in SQL...");
					String sql = translator.translate(q);
					if (verbose)
						System.out.println("ok");
					System.out.println(sql);
				} else if (mode == 1) {
					System.out.println(q.toADQL());
				}
			} catch(UnresolvedIdentifiersException uie) {
				System.err.println("((X)) " + uie.getNbErrors() + " unresolved identifiers:");
				for(ParseException pe : uie)
					System.err.println("\t - at " + pe.getPosition() + ": " + uie.getMessage());
				if (debug)
					uie.printStackTrace(System.err);
				System.exit(3);
			} catch(ParseException pe) {
				System.err.println("((X)) Syntax error: " + pe.getMessage() + " ((X))");
				if (debug)
					pe.printStackTrace(System.err);
				System.exit(3);
			} catch(TranslationException te) {
				if (verbose)
					System.out.println("error");
				System.err.println("((X)) Translation error: " + te.getMessage() + " ((X))");
				if (debug)
					te.printStackTrace(System.err);
				System.exit(4);
			}

		} catch(IOException ioe) {
			System.err.println("\n((X)) Error while reading the file \"" + file + "\": " + ioe.getMessage() + " ((X))");
			if (debug)
				ioe.printStackTrace(System.err);
			System.exit(2);
		}

	}

}
