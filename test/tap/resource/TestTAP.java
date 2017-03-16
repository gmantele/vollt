package tap.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import tap.AbstractTAPFactory;
import tap.ServiceConnection;
import tap.TAPException;
import tap.db.DBConnection;
import tap.formatter.ServiceConnection4Test;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import uws.service.file.LocalUWSFileManager;

public class TestTAP {

	@Test
	public void testAppendObsCoreDM(){
		// Create the TAPMetadata:
		TAPMetadata metadata = new TAPMetadata();
		TAPSchema ivoaSchema = new TAPSchema("ivoa");
		TAPTable obscore = new TAPTable("ObsCore");

		// Create a TAP instance:
		try{
			ServiceConnection4Test serviceConn = new ServiceConnection4Test(metadata, new LocalUWSFileManager(new File(System.getProperty("java.io.tmpdir"))));
			serviceConn.setFactory(new TAPFactory4Test(serviceConn));
			TAP tap = new TAP(serviceConn);
			StringBuffer xml = new StringBuffer();

			// CASE: no Obscore table:
			tap.appendObsCoreDM(xml, "");
			assertEquals(0, xml.length());

			// CASE: with an IVOA schema:
			metadata.addSchema(ivoaSchema);
			tap.appendObsCoreDM(xml, "");
			assertEquals(0, xml.length());

			// CASE: with an Obscore table (with no *_xel columns) - ObsCore 1.0:
			ivoaSchema.addTable(obscore);
			tap.appendObsCoreDM(xml, "\t");
			assertEquals("\t<dataModel ivo-id=\"ivo://ivoa.net/std/ObsCore/v1.0\">ObsCore-1.0</dataModel>\n", xml.toString());

			// CASE: with an Obscore 1.1 table but not with all *_xel columns:
			obscore.addColumn("s_xel1", new DBType(DBDatatype.BIGINT), null, null, null, null);
			obscore.addColumn("s_xel2", new DBType(DBDatatype.BIGINT), null, null, null, null);
			obscore.addColumn("t_xel", new DBType(DBDatatype.BIGINT), null, null, null, null);
			obscore.addColumn("em_xel", new DBType(DBDatatype.BIGINT), null, null, null, null);
			xml.delete(0, xml.length());
			tap.appendObsCoreDM(xml, "\t");
			assertEquals("\t<dataModel ivo-id=\"ivo://ivoa.net/std/ObsCore/v1.0\">ObsCore-1.0</dataModel>\n", xml.toString());

			// CASE: correct Obscore 1.1 table:
			obscore.addColumn("pol_xel", new DBType(DBDatatype.BIGINT), null, null, null, null);
			xml.delete(0, xml.length());
			tap.appendObsCoreDM(xml, "\t");
			assertEquals("\t<dataModel ivo-id=\"ivo://ivoa.net/std/ObsCore/v1.1\">ObsCore-1.1</dataModel>\n", xml.toString());

		}catch(Exception e){
			e.printStackTrace();
			fail("Unexpected error while creating a TAP instance! (see console for more details)");
		}
	}

	private static class TAPFactory4Test extends AbstractTAPFactory {

		protected TAPFactory4Test(ServiceConnection service) throws NullPointerException{
			super(service);
		}

		@Override
		public DBConnection getConnection(String jobID) throws TAPException{
			return null;
		}

		@Override
		public void freeConnection(DBConnection conn){}

		@Override
		public void destroy(){}
	}

}
