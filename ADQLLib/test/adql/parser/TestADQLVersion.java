package adql.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import adql.parser.ADQLParser.ADQLVersion;

public class TestADQLVersion {

	@Test
	public void testToString() {
		assertEquals("v2.0", ADQLVersion.V2_0.toString());
		assertEquals("v2.1", ADQLVersion.V2_1.toString());
	}

	@Test
	public void testParse() {
		// TEST: NULL or empty strings => NULL
		String[] strings = new String[]{ null, "", "  ", " 	 " };
		for(String str : strings)
			assertNull(ADQLVersion.parse(str));

		// TEST: unknown version => NULL
		strings = new String[]{ "foo", "v0.1", "V1.0" };
		for(String str : strings)
			assertNull(ADQLVersion.parse(str));

		// TEST: Expected version strings => OK
		assertEquals(ADQLVersion.V2_0, ADQLVersion.parse("2.0"));
		assertEquals(ADQLVersion.V2_0, ADQLVersion.parse("2_0"));
		assertEquals(ADQLVersion.V2_0, ADQLVersion.parse("v2.0"));
		assertEquals(ADQLVersion.V2_0, ADQLVersion.parse("V2.0"));
		assertEquals(ADQLVersion.V2_0, ADQLVersion.parse("v2_0"));
		assertEquals(ADQLVersion.V2_0, ADQLVersion.parse("V2_0"));

		// TEST: it MUST work with all available versions
		for(ADQLVersion v : ADQLVersion.values()) {
			assertEquals(v, ADQLVersion.parse(v.name()));
			assertEquals(v, ADQLVersion.parse(v.toString()));
		}
	}

}
