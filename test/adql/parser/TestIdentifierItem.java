package adql.parser;

import static adql.parser.ADQLParserConstants.DELIMITED_IDENTIFIER;
import static adql.parser.ADQLParserConstants.REGULAR_IDENTIFIER_CANDIDATE;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import adql.parser.IdentifierItems.IdentifierItem;

public class TestIdentifierItem {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
	}

	@Before
	public void setUp() throws Exception{
	}

	@Test
	public void testIdentifierItem(){
		/* A regular identifier (with no special characters) should be returned
		 * as provided: */
		IdentifierItem identifier = new IdentifierItem(new Token(REGULAR_IDENTIFIER_CANDIDATE, "m50"), false);
		assertEquals("m50", identifier.toString());

		/* Ensure doubled double quotes are escaped
		 * (i.e. considered as a single double quote): */
		identifier = new IdentifierItem(new Token(DELIMITED_IDENTIFIER, "m50\"\""), true);
		assertEquals("m50\"", identifier.toString());
	}

}
