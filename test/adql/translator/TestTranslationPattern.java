package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import adql.query.operand.ADQLOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.ADQLFunction;
import adql.query.operand.function.DefaultUDF;

public class TestTranslationPattern {

	private ADQLTranslator translator;
	private ADQLFunction fct;

	@Before
	public void initTest() throws Exception {
		translator = new PostgreSQLTranslator();
		fct = new DefaultUDF("anyFunction", new ADQLOperand[]{ new StringConstant("Blabla"), new NumericConstant(123), new NumericConstant(1.23) });
	}

	/* *************************************************************************
	 * Empty/NULL pattern => NullPointerException
	 */

	@Test(expected = NullPointerException.class)
	public void testApplyNullPattern() throws Exception {
		TranslationPattern.apply(null, fct, translator);
	}

	@Test(expected = NullPointerException.class)
	public void testCheckNullPattern() throws Exception {
		TranslationPattern.check(null, fct.getNbParameters());
	}

	@Test(expected = NullPointerException.class)
	public void testApplyEmptyPattern() throws Exception {
		TranslationPattern.apply("", fct, translator);
	}

	@Test(expected = NullPointerException.class)
	public void testCheckEmptyPattern() throws Exception {
		TranslationPattern.check("", fct.getNbParameters());
	}

	@Test(expected = NullPointerException.class)
	public void testApplyLongEmptyPattern() throws Exception {
		TranslationPattern.apply(" 	 ", fct, translator);
	}

	@Test(expected = NullPointerException.class)
	public void testCheckLongEmptyPattern() throws Exception {
		TranslationPattern.check(" 	 ", fct.getNbParameters());
	}

	/* *************************************************************************
	 * No function => NullPointerException
	 */

	@Test(expected = NullPointerException.class)
	public void testApplyPattern_WithNoFunction() throws Exception {
		TranslationPattern.apply("foo", null, translator);
	}

	/* *************************************************************************
	 * No translator => NullPointerException
	 */

	@Test(expected = NullPointerException.class)
	public void testApplyPattern_WithNoTranslator() throws Exception {
		TranslationPattern.apply("foo", fct, null);
	}

	/* *************************************************************************
	 * No $ expression => returned as such
	 */

	@Test
	public void testApplyPattern_WithNothingToResolve() throws Exception {
		assertEquals("foo", TranslationPattern.apply("foo", fct, translator));
	}

	@Test
	public void testCheckPattern_WithNothingToResolve() throws Exception {
		assertEquals("foo", TranslationPattern.check("foo", fct.getNbParameters()));
	}

	/* *************************************************************************
	 * Ending with '}' => returned as such
	 */

	@Test
	public void testApplyPattern_EndingWithRightCurlyBrace() throws Exception {
		assertEquals("foo}", TranslationPattern.apply("foo}", fct, translator));
	}

	@Test
	public void testCheckPattern_EndingWithRightCurlyBrace() throws Exception {
		assertEquals("foo}", TranslationPattern.check("foo}", fct.getNbParameters()));
	}

	/* *************************************************************************
	 * '$' followed with neither '$' nor an integer => TranslationException
	 */

	@Test
	public void testApplyPattern_WithIncorrectDollarSuffix() throws Exception {
		try {
			TranslationPattern.apply("foo($bar)", fct, translator);
			fail("Incorrect $ expression ('$bar') => TranslationException");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.5] Unexpected character after '$': 'b'! Expected: '$' or an argument index (i.e. an integer).", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_WithIncorrectDollarSuffix() throws Exception {
		try {
			TranslationPattern.check("foo($bar)", fct.getNbParameters());
			fail("Incorrect $ expression ('$bar') => ParseException");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.5] Unexpected character after '$': 'b'! Expected: '$' or an argument index (i.e. an integer).", ex.getMessage());
		}
	}

	@Test
	public void testApplyPattern_EndingWithDollar() throws Exception {
		try {
			TranslationPattern.apply("foo$", fct, translator);
			fail("Missing integer after $ => TranslationException");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.4] Missing character after '$'! Expected: '$' or an argument index (i.e. an integer).", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_EndingWithDollar() throws Exception {
		try {
			TranslationPattern.check("foo$", fct.getNbParameters());
			fail("Missing integer after $ => ParseException");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.4] Missing character after '$'! Expected: '$' or an argument index (i.e. an integer).", ex.getMessage());
		}
	}

	/* *************************************************************************
	 * Simple argument references
	 */

	@Test
	public void testApplyPattern_WithSimpleArgumentRefs() throws Exception {
		assertEquals("anyFunction(123, 'Blabla', 1.23)", TranslationPattern.apply("anyFunction($2, $1, $3)", fct, translator));
	}

	@Test
	public void testCheckPattern_WithSimpleArgumentRefs() throws Exception {
		assertEquals("anyFunction($2, $1, $3)", TranslationPattern.check("anyFunction($2, $1, $3)", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_WithConcatenatedArgumentRefs() throws Exception {
		assertEquals("123'Blabla'1.23", TranslationPattern.apply("$2$1$3", fct, translator));
	}

	@Test
	public void testCheckPattern_WithConcatenatedArgumentRefs() throws Exception {
		assertEquals("$2$1$3", TranslationPattern.check("$2$1$3", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_WithZeroPrefixedIndex() throws Exception {
		assertEquals("'Blabla'", TranslationPattern.apply("$001", fct, translator));
	}

	@Test
	public void testCheckPattern_WithZeroPrefixedIndex() throws Exception {
		assertEquals("$1", TranslationPattern.check("$001", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_EndingWithArgumentRef() throws Exception {
		assertEquals("'Blabla'", TranslationPattern.apply("$1", fct, translator));
	}

	@Test
	public void testCheckPattern_EndingWithArgumentRef() throws Exception {
		assertEquals("$1", TranslationPattern.check("$1", fct.getNbParameters()));
	}

	@Test()
	public void testApplyPattern_WithSimpleArgumentRefs_ButIndexTooSmall() {
		try {
			TranslationPattern.apply("anyFunction($0)", fct, translator);
			fail("No 0-th argument => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.14] Incorrect argument index: '$0'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	@Test()
	public void testCheckPattern_WithSimpleArgumentRefs_ButIndexTooSmall() {
		try {
			TranslationPattern.check("anyFunction($0)", fct.getNbParameters());
			fail("No 0-th argument => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.14] Incorrect argument index: '$0'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	@Test()
	public void testApplyPattern_WithSimpleArgumentRefs_ButIndexTooBig() {
		try {
			TranslationPattern.apply("anyFunction($9)", fct, translator);
			fail("No 9-th argument => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.14] Incorrect argument index: '$9'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	@Test()
	public void testCheckPattern_WithSimpleArgumentRefs_ButIndexTooBig() {
		try {
			TranslationPattern.check("anyFunction($9)", fct.getNbParameters());
			fail("No 9-th argument => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.14] Incorrect argument index: '$9'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	/* *************************************************************************
	 * Escaped $ (i.e. '$$')
	 */

	@Test
	public void testApplyPattern_WithEscapedDollar() throws Exception {
		assertEquals("foo$2('Blabla', '$123')", TranslationPattern.apply("foo$$2($1, '$$$2')", fct, translator));
	}

	@Test
	public void testCheckPattern_WithEscapedDollar() throws Exception {
		assertEquals("foo$2($1, '$$2')", TranslationPattern.check("foo$$2($1, '$$$2')", fct.getNbParameters()));
	}

	/* *************************************************************************
	 * Argument list reference
	 */

	@Test
	public void testApplyPattern_WithArgumentListRef() throws Exception {
		assertEquals("anyFunction('Blabla', 123, 1.23)", TranslationPattern.apply("anyFunction($1, $2..)", fct, translator));
	}

	@Test
	public void testCheckPattern_WithArgumentListRef() throws Exception {
		assertEquals("anyFunction($1, $2, $3)", TranslationPattern.check("anyFunction($1, $2..)", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_EndingWithArgumentListRef() throws Exception {
		assertEquals("'Blabla', 123, 1.23", TranslationPattern.apply("$1..", fct, translator));
	}

	@Test
	public void testCheckPattern_EndingWithArgumentListRef() throws Exception {
		assertEquals("$1, $2, $3", TranslationPattern.check("$1..", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_IncompleteArgumentListRef() throws Exception {
		assertEquals("'Blabla'.foo", TranslationPattern.apply("$1.foo", fct, translator));
	}

	@Test
	public void testCheckPattern_IncompleteArgumentListRef() throws Exception {
		assertEquals("$1.foo", TranslationPattern.check("$1.foo", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_EndingWithIncompleteArgumentListRef() throws Exception {
		assertEquals("'Blabla'.", TranslationPattern.apply("$1.", fct, translator));
	}

	@Test
	public void testCheckPattern_EndingWithIncompleteArgumentListRef() throws Exception {
		assertEquals("$1.", TranslationPattern.check("$1.", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_WithArgumentListRef_ButIndexTooSmall() {
		try {
			TranslationPattern.apply("anyFunction($1, $0..)", fct, translator);
			fail("No 0-th argument => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.19] Incorrect argument index: '$0'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_WithArgumentListRef_ButIndexTooSmall() {
		try {
			TranslationPattern.check("anyFunction($1, $0..)", fct.getNbParameters());
			fail("No 0-th argument => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.19] Incorrect argument index: '$0'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	@Test
	public void testApplyPattern_WithArgumentListRef_ButIndexTooBig() {
		try {
			TranslationPattern.apply("anyFunction($1, $9..)", fct, translator);
			fail("No 9-th argument => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.19] Incorrect argument index: '$9'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_WithArgumentListRef_ButIndexTooBig() {
		try {
			TranslationPattern.check("anyFunction($1, $9..)", fct.getNbParameters());
			fail("No 9-th argument => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.19] Incorrect argument index: '$9'. Expected: an integer between [1;3].", ex.getMessage());
		}
	}

	/* *************************************************************************
	 * Ternary conditional expression
	 */

	@Test
	public void testApplyPattern_WithTernaryExpression() throws Exception {
		assertEquals("anyFunction('Blabla', 123*10, 0)", TranslationPattern.apply("anyFunction($1$2?{, $2*10}{}$4?{, $4+10}{, 0})", fct, translator));
	}

	@Test
	public void testCheckPattern_WithTernaryExpression() throws Exception {
		assertEquals("anyFunction($1, $2*10, 0)", TranslationPattern.check("anyFunction($1$2?{, $2*10}{}$4?{, $4+10}{, 0})", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_WithRecursiveTernaryExpression() throws Exception {
		assertEquals("anyFunction('Blabla', sum(123, 1.23))", TranslationPattern.apply("anyFunction($1, $2? {$3?{sum($2..)}   {$2}}  {0})", fct, translator));
	}

	@Test
	public void testCheckPattern_WithRecursiveTernaryExpression() throws Exception {
		assertEquals("anyFunction($1, sum($2, $3))", TranslationPattern.check("anyFunction($1, $2? {$3?{sum($2..)}   {$2}}  {0})", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_WithRecursiveTernaryExpression2() throws Exception {
		assertEquals("anyFunction('Blabla', 0)", TranslationPattern.apply("anyFunction($1, $4? {$2?{sum($2..)}   {$2}}  {0})", fct, translator));
	}

	@Test
	public void testCheckPattern_WithRecursiveTernaryExpression2() throws Exception {
		assertEquals("anyFunction($1, 0)", TranslationPattern.check("anyFunction($1, $4? {$2?{sum($2..)}   {$2}}  {0})", fct.getNbParameters()));
	}

	@Test
	public void testApplyPattern_WithUnstartedTernaryThen() throws Exception {
		try {
			TranslationPattern.apply("$1?foo", fct, translator);
			fail("Unstarted ternary THEN block => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.3] Unexpected character: 'f'! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_WithUnstartedTernaryThen() throws Exception {
		try {
			TranslationPattern.check("$1?foo", fct.getNbParameters());
			fail("Unstarted ternary THEN block => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.3] Unexpected character: 'f'! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testApplyPattern_WithUnstartedTernaryElse() throws Exception {
		try {
			TranslationPattern.apply("$1?{$1}foo", fct, translator);
			fail("Unstarted ternary ELSE block => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.7] Unexpected character: 'f'! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_WithUnstartedTernaryElse() throws Exception {
		try {
			TranslationPattern.check("$1?{$1}foo", fct.getNbParameters());
			fail("Unstarted ternary ELSE block => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.7] Unexpected character: 'f'! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testApplyPattern_EndingWithUnstartedTernaryThen() throws Exception {
		try {
			TranslationPattern.apply("$1?", fct, translator);
			fail("Unstarted ternary expression => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.3] Missing start of the THEN block of a ternary expression! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_EndingWithUnstartedTernaryThen() throws Exception {
		try {
			TranslationPattern.check("$1?", fct.getNbParameters());
			fail("Unstarted ternary expression => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.3] Missing start of the THEN block of a ternary expression! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testApplyPattern_EndingWithUnstartedTernaryElse() throws Exception {
		try {
			TranslationPattern.apply("$1?{foo}", fct, translator);
			fail("Unstarted ternary expression => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.8] Missing start of the ELSE block of a ternary expression! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_EndingWithUnstartedTernaryElse() throws Exception {
		try {
			TranslationPattern.check("$1?{foo}", fct.getNbParameters());
			fail("Unstarted ternary expression => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.8] Missing start of the ELSE block of a ternary expression! Expected: '{'.", ex.getMessage());
		}
	}

	@Test
	public void testApplyPattern_WithUnclosedTernaryThen() throws Exception {
		try {
			TranslationPattern.apply("$1?{$1", fct, translator);
			fail("Unclosed ternary expression => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.6] Missing end of the THEN block of a ternary expression! Expected: '}'.", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_WithUnclosedTernaryThen() throws Exception {
		try {
			TranslationPattern.check("$1?{$1", fct.getNbParameters());
			fail("Unclosed ternary expression => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.6] Missing end of the THEN block of a ternary expression! Expected: '}'.", ex.getMessage());
		}
	}

	@Test
	public void testApplyPattern_WithUnclosedTernaryElse() throws Exception {
		try {
			TranslationPattern.apply("$1? {$1} {", fct, translator);
			fail("Unclosed ternary expression => TranslationException expected");
		} catch(Exception ex) {
			assertEquals(TranslationException.class, ex.getClass());
			assertEquals(ParseException.class, ex.getCause().getClass());
			assertEquals("[c.10] Missing end of the ELSE block of a ternary expression! Expected: '}'.", ex.getMessage());
		}
	}

	@Test
	public void testCheckPattern_WithUnclosedTernaryElse() throws Exception {
		try {
			TranslationPattern.check("$1? {$1} {", fct.getNbParameters());
			fail("Unclosed ternary expression => ParseException expected");
		} catch(Exception ex) {
			assertEquals(ParseException.class, ex.getClass());
			assertEquals("[c.10] Missing end of the ELSE block of a ternary expression! Expected: '}'.", ex.getMessage());
		}
	}

}
