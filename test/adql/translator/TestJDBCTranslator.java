package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBType;
import adql.db.FunctionDef;
import adql.db.STCS.Region;
import adql.parser.grammar.ParseException;
import adql.query.IdentifierField;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.ExtractCoordSys;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

public class TestJDBCTranslator {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testTranslateStringConstant() {
		JDBCTranslator tr = new AJDBCTranslator();

		/* Ensure the translation from ADQL to SQL of strings is correct ;
		 * particularly, ' should be escaped otherwise it would mean the end of a string in SQL
		 *(the way to escape a such character is by doubling the character '): */
		try{
==== BASE ====
			assertEquals("'SQL''s translation'", tr.translate(new StringConstant("SQL's translation")));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate a StringConstant object into SQL.");
		}
	}

	@Test
	public void testTranslateUserDefinedFunction() {
		JDBCTranslator tr = new AJDBCTranslator();

		DefaultUDF udf = new DefaultUDF("split", new ADQLOperand[]{ new ADQLColumn("values"), new StringConstant(";") });

		// TEST: no FunctionDef, so no translation pattern => just return the ADQL
		try {
			assertEquals("split(values, ';')", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		}

		// TEST: a FunctionDef with no translation pattern => just return the ADQL
		try {
			udf.setDefinition(FunctionDef.parse("split(str VARCHAR, sep VARCHAR) -> VARCHAR"));
			assertEquals("split(values, ';')", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem preparing this test.");
		}

		// TEST: a FunctionDef with a translation pattern but no argument reference => the pattern should be returned as such
		try {
			udf.getDefinition().setTranslationPattern("foobar");
			assertEquals("foobar", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem preparing this test.");
		}

		// TEST: a FunctionDef with a translation pattern and with argument reference => the pattern should be applied
		try {
			udf.getDefinition().setTranslationPattern("splitWith($2, $1)");
			assertEquals("splitWith(" + tr.translate(udf.getParameter(1)) + ", " + tr.translate(udf.getParameter(0)) + ")", tr.translate(udf));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate this UDF as in ADQL.");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem preparing this test.");
		}
	}

	public final static class AJDBCTranslator extends JDBCTranslator {

		@Override
		public String translate(ExtractCoord extractCoord) throws TranslationException {
			return null;
		}

		@Override
		public String translate(ExtractCoordSys extractCoordSys) throws TranslationException {
			return null;
		}

		@Override
		public String translate(AreaFunction areaFunction) throws TranslationException {
			return null;
		}

		@Override
		public String translate(CentroidFunction centroidFunction) throws TranslationException {
			return null;
		}

		@Override
		public String translate(DistanceFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(ContainsFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(IntersectsFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(PointFunction point) throws TranslationException {
			return null;
		}

		@Override
		public String translate(CircleFunction circle) throws TranslationException {
			return null;
		}

		@Override
		public String translate(BoxFunction box) throws TranslationException {
			return null;
		}

		@Override
		public String translate(PolygonFunction polygon) throws TranslationException {
			return null;
		}

		@Override
		public String translate(RegionFunction region) throws TranslationException {
			return null;
		}

		@Override
		public boolean isCaseSensitive(IdentifierField field) {
			return false;
		}

		@Override
		public DBType convertTypeFromDB(int dbmsType, String rawDbmsTypeName, String dbmsTypeName, String[] typeParams) {
			return null;
		}

		@Override
		public String convertTypeToDB(DBType type) {
			return null;
		}

		@Override
		public Region translateGeometryFromDB(Object jdbcColValue) throws ParseException {
			return null;
		}

		@Override
		public Object translateGeometryToDB(Region region) throws ParseException {
			return null;
		}

	}

}
