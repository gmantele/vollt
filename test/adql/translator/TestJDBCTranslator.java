package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBType;
import adql.db.STCS.Region;
import adql.parser.ParseException;
import adql.query.IdentifierField;
import adql.query.operand.StringConstant;
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
	public void setUp() throws Exception{}

	@Test
	public void testTranslateStringConstant(){
		JDBCTranslator tr = new AJDBCTranslator();

		/* Ensure the translation from ADQL to SQL of strings is correct ;
		 * particularly, ' should be escaped otherwise it would mean the end of a string in SQL
		 *(the way to escape a such character is by doubling the character '): */
		try{
			assertEquals("'SQL''s translation'", tr.translate(new StringConstant("SQL's translation")));
		}catch(TranslationException e){
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate a StringConstant object into SQL.");
		}
	}

	public final static class AJDBCTranslator extends JDBCTranslator {

		@Override
		public String translate(ExtractCoord extractCoord) throws TranslationException{
			return null;
		}

		@Override
		public String translate(ExtractCoordSys extractCoordSys) throws TranslationException{
			return null;
		}

		@Override
		public String translate(AreaFunction areaFunction) throws TranslationException{
			return null;
		}

		@Override
		public String translate(CentroidFunction centroidFunction) throws TranslationException{
			return null;
		}

		@Override
		public String translate(DistanceFunction fct) throws TranslationException{
			return null;
		}

		@Override
		public String translate(ContainsFunction fct) throws TranslationException{
			return null;
		}

		@Override
		public String translate(IntersectsFunction fct) throws TranslationException{
			return null;
		}

		@Override
		public String translate(PointFunction point) throws TranslationException{
			return null;
		}

		@Override
		public String translate(CircleFunction circle) throws TranslationException{
			return null;
		}

		@Override
		public String translate(BoxFunction box) throws TranslationException{
			return null;
		}

		@Override
		public String translate(PolygonFunction polygon) throws TranslationException{
			return null;
		}

		@Override
		public String translate(RegionFunction region) throws TranslationException{
			return null;
		}

		@Override
		public boolean isCaseSensitive(IdentifierField field){
			return false;
		}

		@Override
		public DBType convertTypeFromDB(int dbmsType, String rawDbmsTypeName, String dbmsTypeName, String[] typeParams){
			return null;
		}

		@Override
		public String convertTypeToDB(DBType type){
			return null;
		}

		@Override
		public Region translateGeometryFromDB(Object jdbcColValue) throws ParseException{
			return null;
		}

		@Override
		public Object translateGeometryToDB(Region region) throws ParseException{
			return null;
		}

	}

}
