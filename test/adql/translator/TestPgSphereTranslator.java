package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Types;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.util.PGobject;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.STCS.Region;
import adql.parser.ParseException;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.GeometryFunction.GeometryValue;

public class TestPgSphereTranslator {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{}

	@AfterClass
	public static void tearDownAfterClass() throws Exception{}

	@Before
	public void setUp() throws Exception{}

	@After
	public void tearDown() throws Exception{}

	@Test
	public void testTranslateCentroidFunction(){
		try{
			PgSphereTranslator translator = new PgSphereTranslator();
			CentroidFunction centfc = new CentroidFunction(new GeometryValue<GeometryFunction>(new CircleFunction(new StringConstant("ICRS"), new NumericConstant(128.23), new NumericConstant(0.53), new NumericConstant(2))));
			assertEquals("center(scircle(spoint(radians(128.23),radians(0.53)),radians(2)))", translator.translate(centfc));
		}catch(Throwable t){
			t.printStackTrace(System.err);
			fail("An error occured while building a simple CentroidFunction! (see the console for more details)");
		}
	}

	@Test
	public void testConvertTypeFromDB(){
		PgSphereTranslator translator = new PgSphereTranslator();

		// POINT
		DBType type = translator.convertTypeFromDB(Types.OTHER, "spoint", "spoint", null);
		assertNotNull(type);
		assertEquals(DBDatatype.POINT, type.type);
		assertEquals(DBType.NO_LENGTH, type.length);

		// CIRCLE
		type = translator.convertTypeFromDB(Types.OTHER, "scircle", "scircle", null);
		assertNotNull(type);
		assertEquals(DBDatatype.REGION, type.type);
		assertEquals(DBType.NO_LENGTH, type.length);

		// BOX
		type = translator.convertTypeFromDB(Types.OTHER, "sbox", "sbox", null);
		assertNotNull(type);
		assertEquals(DBDatatype.REGION, type.type);
		assertEquals(DBType.NO_LENGTH, type.length);

		// POLYGON
		type = translator.convertTypeFromDB(Types.OTHER, "spoly", "spoly", null);
		assertNotNull(type);
		assertEquals(DBDatatype.REGION, type.type);
		assertEquals(DBType.NO_LENGTH, type.length);
	}

	@Test
	public void testConvertTypeToDB(){
		PgSphereTranslator translator = new PgSphereTranslator();

		// NULL
		assertEquals("VARCHAR", translator.convertTypeToDB(null));

		// POINT
		assertEquals("spoint", translator.convertTypeToDB(new DBType(DBDatatype.POINT)));

		// REGION (any other region is transformed into a polygon)
		assertEquals("spoly", translator.convertTypeToDB(new DBType(DBDatatype.REGION)));
	}

	@Test
	public void testTranslateGeometryFromDB(){
		PgSphereTranslator translator = new PgSphereTranslator();
		PGobject pgo = new PGobject();

		// NULL
		try{
			assertNull(translator.translateGeometryFromDB(null));
		}catch(Throwable t){
			t.printStackTrace();
			fail(t.getMessage());
		}

		// SPOINT
		try{
			pgo.setType("spoint");
			pgo.setValue("(0.1 , 0.2)");
			Region r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(11.45915, r.coordinates[0][1], 1e-5);

			pgo.setValue("(5.72957d , 11.45915d)");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(11.45915, r.coordinates[0][1], 1e-5);

			pgo.setValue("(  5d 43m 46.480625s , +11d 27m 32.961249s)");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(11.45915, r.coordinates[0][1], 1e-5);

			pgo.setValue("(  0h 22m 55.098708s , +11d 27m 32.961249s)");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(11.45915, r.coordinates[0][1], 1e-5);
		}catch(Throwable t){
			t.printStackTrace();
			fail(t.getMessage());
		}

		// SCIRCLE
		try{
			pgo.setType("scircle");
			pgo.setValue("<(0.1,-0.2),1>");
			Region r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(-11.45915, r.coordinates[0][1], 1e-5);
			assertEquals(57.29577, r.radius, 1e-5);

			pgo.setValue("<(5.72957d , -11.45915d) , 57.29577d>");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(-11.45915, r.coordinates[0][1], 1e-5);
			assertEquals(57.29577, r.radius, 1e-5);

			pgo.setValue("<(  5d 43m 46.452s , -11d 27m 32.94s) , 57d 17m 44.772s>");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(-11.45915, r.coordinates[0][1], 1e-5);
			assertEquals(57.29577, r.radius, 1e-5);

			pgo.setValue("<(  0h 22m 55.0968s , -11d 27m 32.94s) , 57d 17m 44.772s>");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(5.72957, r.coordinates[0][0], 1e-5);
			assertEquals(-11.45915, r.coordinates[0][1], 1e-5);
			assertEquals(57.29577, r.radius, 1e-5);
		}catch(Throwable t){
			t.printStackTrace();
			fail(t.getMessage());
		}

		// SBOX
		try{
			pgo.setType("sbox");
			pgo.setValue("((0.1,0.2),(0.5,0.5))");
			Region r = translator.translateGeometryFromDB(pgo);
			assertEquals(17.18873, r.coordinates[0][0], 1e-5);
			assertEquals(20.05352, r.coordinates[0][1], 1e-5);
			assertEquals(22.91831, r.width, 1e-5);
			assertEquals(17.18873, r.height, 1e-5);

			pgo.setValue("((5.72957795130823d , 11.4591559026165d), (28.6478897565412d , 28.6478897565412d))");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(17.18873, r.coordinates[0][0], 1e-5);
			assertEquals(20.05352, r.coordinates[0][1], 1e-5);
			assertEquals(22.91831, r.width, 1e-5);
			assertEquals(17.18873, r.height, 1e-5);

			pgo.setValue("((  5d 43m 46.480625s , +11d 27m 32.961249s), ( 28d 38m 52.403124s , +28d 38m 52.403124s))");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(17.18873, r.coordinates[0][0], 1e-5);
			assertEquals(20.05352, r.coordinates[0][1], 1e-5);
			assertEquals(22.91831, r.width, 1e-5);
			assertEquals(17.18873, r.height, 1e-5);

			pgo.setValue("((  0h 22m 55.098708s , +11d 27m 32.961249s), (  1h 54m 35.493542s , +28d 38m 52.403124s))");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(17.18873, r.coordinates[0][0], 1e-5);
			assertEquals(20.05352, r.coordinates[0][1], 1e-5);
			assertEquals(22.91831, r.width, 1e-5);
			assertEquals(17.18873, r.height, 1e-5);
		}catch(Throwable t){
			t.printStackTrace();
			fail(t.getMessage());
		}

		// SPOLY
		try{
			pgo.setType("spoly");
			pgo.setValue("{(0.789761486527434 , 0.00436332312998582),(0.789761486527434 , 0.00872664625997165),(0.785398163397448 , 0.00872664625997165),(0.785398163397448 , 0.00436332312998582),(0.781034840267463 , 0.00436332312998582),(0.781034840267463 , 0),(0.785398163397448 , 0)}");
			Region r = translator.translateGeometryFromDB(pgo);
			assertEquals(45.25, r.coordinates[0][0], 1e-2);
			assertEquals(0.25, r.coordinates[0][1], 1e-2);
			assertEquals(45.25, r.coordinates[1][0], 1e-2);
			assertEquals(0.5, r.coordinates[1][1], 1e-2);
			assertEquals(45, r.coordinates[2][0], 1e-2);
			assertEquals(0.5, r.coordinates[2][1], 1e-2);
			assertEquals(45, r.coordinates[3][0], 1e-2);
			assertEquals(0.25, r.coordinates[3][1], 1e-2);
			assertEquals(44.75, r.coordinates[4][0], 1e-2);
			assertEquals(0.25, r.coordinates[4][1], 1e-2);
			assertEquals(44.75, r.coordinates[5][0], 1e-2);
			assertEquals(0, r.coordinates[5][1], 1e-2);
			assertEquals(45, r.coordinates[6][0], 1e-2);
			assertEquals(0, r.coordinates[6][1], 1e-2);

			pgo.setValue("{(45.25d , 0.25d), (45.25d , 0.5d), (45d , 0.5d), (45d , 0.25d), (44.75d , 0.25d), (44.75d , 0d), (45d , 0d)}");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(45.25, r.coordinates[0][0], 1e-2);
			assertEquals(0.25, r.coordinates[0][1], 1e-2);
			assertEquals(45.25, r.coordinates[1][0], 1e-2);
			assertEquals(0.5, r.coordinates[1][1], 1e-2);
			assertEquals(45, r.coordinates[2][0], 1e-2);
			assertEquals(0.5, r.coordinates[2][1], 1e-2);
			assertEquals(45, r.coordinates[3][0], 1e-2);
			assertEquals(0.25, r.coordinates[3][1], 1e-2);
			assertEquals(44.75, r.coordinates[4][0], 1e-2);
			assertEquals(0.25, r.coordinates[4][1], 1e-2);
			assertEquals(44.75, r.coordinates[5][0], 1e-2);
			assertEquals(0, r.coordinates[5][1], 1e-2);
			assertEquals(45, r.coordinates[6][0], 1e-2);
			assertEquals(0, r.coordinates[6][1], 1e-2);

			pgo.setValue("{( 45d 15m 0s , + 0d 15m 0s),( 45d 15m 0s , + 0d 30m 0s),( 45d  0m 0s , + 0d 30m 0s),( 45d  0m 0s , + 0d 15m 0s),( 44d 45m 0s , + 0d 15m 0s),( 44d 45m 0s , + 0d  0m 0s),( 45d  0m 0s , + 0d  0m 0s)}");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(45.25, r.coordinates[0][0], 1e-2);
			assertEquals(0.25, r.coordinates[0][1], 1e-2);
			assertEquals(45.25, r.coordinates[1][0], 1e-2);
			assertEquals(0.5, r.coordinates[1][1], 1e-2);
			assertEquals(45, r.coordinates[2][0], 1e-2);
			assertEquals(0.5, r.coordinates[2][1], 1e-2);
			assertEquals(45, r.coordinates[3][0], 1e-2);
			assertEquals(0.25, r.coordinates[3][1], 1e-2);
			assertEquals(44.75, r.coordinates[4][0], 1e-2);
			assertEquals(0.25, r.coordinates[4][1], 1e-2);
			assertEquals(44.75, r.coordinates[5][0], 1e-2);
			assertEquals(0, r.coordinates[5][1], 1e-2);
			assertEquals(45, r.coordinates[6][0], 1e-2);
			assertEquals(0, r.coordinates[6][1], 1e-2);

			pgo.setValue("{(  3h  1m 0s , + 0d 15m 0s),(  3h  1m 0s , + 0d 30m 0s),(  3h  0m 0s , + 0d 30m 0s),(  3h  0m 0s , + 0d 15m 0s),(  2h 59m 0s , + 0d 15m 0s),(  2h 59m 0s , + 0d  0m 0s),(  3h  0m 0s , + 0d  0m 0s)}");
			r = translator.translateGeometryFromDB(pgo);
			assertEquals(45.25, r.coordinates[0][0], 1e-2);
			assertEquals(0.25, r.coordinates[0][1], 1e-2);
			assertEquals(45.25, r.coordinates[1][0], 1e-2);
			assertEquals(0.5, r.coordinates[1][1], 1e-2);
			assertEquals(45, r.coordinates[2][0], 1e-2);
			assertEquals(0.5, r.coordinates[2][1], 1e-2);
			assertEquals(45, r.coordinates[3][0], 1e-2);
			assertEquals(0.25, r.coordinates[3][1], 1e-2);
			assertEquals(44.75, r.coordinates[4][0], 1e-2);
			assertEquals(0.25, r.coordinates[4][1], 1e-2);
			assertEquals(44.75, r.coordinates[5][0], 1e-2);
			assertEquals(0, r.coordinates[5][1], 1e-2);
			assertEquals(45, r.coordinates[6][0], 1e-2);
			assertEquals(0, r.coordinates[6][1], 1e-2);
		}catch(Throwable t){
			t.printStackTrace();
			fail(t.getMessage());
		}

		// OTHER
		try{
			translator.translateGeometryFromDB(new Double(12.3));
			fail("The translation of a Double as a geometry is not supported!");
		}catch(Throwable t){
			assertTrue(t instanceof ParseException);
			assertEquals("Incompatible type! The column value \"12.3\" was supposed to be a geometrical object.", t.getMessage());
		}
		try{
			pgo.setType("sline");
			pgo.setValue("( -90d, -20d, 200d, XYZ ), 30d ");
			translator.translateGeometryFromDB(pgo);
			fail("The translation of a sline is not supported!");
		}catch(Throwable t){
			assertTrue(t instanceof ParseException);
			assertEquals("Unsupported PgSphere type: \"sline\"! Impossible to convert the column value \"( -90d, -20d, 200d, XYZ ), 30d \" into a Region.", t.getMessage());
		}
	}

	@Test
	public void testTranslateGeometryToDB(){
		PgSphereTranslator translator = new PgSphereTranslator();

		try{
			// NULL
			assertNull(translator.translateGeometryToDB(null));

			// POSITION
			Region r = new Region(null, new double[]{45,0});
			PGobject pgo = (PGobject)translator.translateGeometryToDB(r);
			assertNotNull(pgo);
			assertEquals("spoint", pgo.getType());
			assertEquals("(45.0d,0.0d)", pgo.getValue());

			// CIRCLE
			r = new Region(null, new double[]{45,0}, 1.2);
			pgo = (PGobject)translator.translateGeometryToDB(r);
			assertNotNull(pgo);
			assertEquals("spoly", pgo.getType());
			assertEquals("{(46.2d,0.0d),(46.176942336483876d,0.2341083864193539d),(46.108655439013546d,0.4592201188381077d),(45.99776353476305d,0.6666842796235226d),(45.848528137423855d,0.8485281374238569d),(45.666684279623524d,0.9977635347630542d),(45.45922011883811d,1.1086554390135441d),(45.23410838641935d,1.1769423364838765d),(45.0d,1.2d),(44.76589161358065d,1.1769423364838765d),(44.54077988116189d,1.1086554390135441d),(44.333315720376476d,0.9977635347630543d),(44.151471862576145d,0.848528137423857d),(44.00223646523695d,0.6666842796235226d),(43.891344560986454d,0.4592201188381073d),(43.823057663516124d,0.23410838641935325d),(43.8d,-9.188564877424678E-16d),(43.823057663516124d,-0.23410838641935505d),(43.891344560986454d,-0.45922011883810904d),(44.00223646523695d,-0.6666842796235241d),(44.151471862576145d,-0.8485281374238584d),(44.333315720376476d,-0.9977635347630555d),(44.540779881161896d,-1.108655439013545d),(44.76589161358065d,-1.176942336483877d),(45.0d,-1.2d),(45.23410838641936d,-1.1769423364838758d),(45.45922011883811d,-1.1086554390135428d),(45.666684279623524d,-0.9977635347630521d),(45.84852813742386d,-0.8485281374238541d),(45.99776353476306d,-0.6666842796235192d),(46.108655439013546d,-0.45922011883810354d),(46.176942336483876d,-0.23410838641934922d)}", pgo.getValue());

			// BOX
			r = new Region(null, new double[]{45,0}, 1.2, 5);
			pgo = (PGobject)translator.translateGeometryToDB(r);
			assertNotNull(pgo);
			assertEquals("spoly", pgo.getType());
			assertEquals("{(44.4d,-2.5d),(44.4d,2.5d),(45.6d,2.5d),(45.6d,-2.5d)}", pgo.getValue());

			// POLYGON
			r = new Region(null, new double[][]{new double[]{45.25,0.25},new double[]{45.25,0.5},new double[]{45,0.5},new double[]{45,0.25},new double[]{44.75,0.25},new double[]{44.75,0},new double[]{45,0}});
			pgo = (PGobject)translator.translateGeometryToDB(r);
			assertNotNull(pgo);
			assertEquals("spoly", pgo.getType());
			assertEquals("{(45.25d,0.25d),(45.25d,0.5d),(45.0d,0.5d),(45.0d,0.25d),(44.75d,0.25d),(44.75d,0.0d),(45.0d,0.0d)}", pgo.getValue());

			// OTHER
			try{
				r = new Region(new Region(null, new double[]{45,0}));
				translator.translateGeometryToDB(r);
				fail("The translation of a STC Not region is not supported!");
			}catch(Throwable ex){
				assertTrue(ex instanceof ParseException);
				assertEquals("Unsupported geometrical region: \"" + r.type + "\"!", ex.getMessage());
			}

		}catch(ParseException t){
			t.printStackTrace();
			fail(t.getMessage());
		}
	}

}
