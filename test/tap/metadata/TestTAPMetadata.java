package tap.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import tap.metadata.TAPTable.TableType;

public class TestTAPMetadata {

	@Test
	public void testGetObsCoreTable(){
		TAPMetadata metadata = new TAPMetadata();

		// CASE: no IVOA schema:
		assertNull(metadata.getObsCoreTable());

		// CASE: empty IVOA schema:
		TAPSchema ivoaSchema = new TAPSchema("ivoa");
		metadata.addSchema(ivoaSchema);
		assertNull(metadata.getObsCoreTable());

		// CASE: with ObsCore table as defined in the ObsCore's IVOA standard:
		TAPTable obscore = new TAPTable("ObsCore");
		ivoaSchema.addTable(obscore);
		assertNotNull(metadata.getObsCoreTable());
		assertEquals("ivoa.ObsCore", metadata.getObsCoreTable().getFullName());

		// CASE: with "obscore" (all lower-case):
		obscore = new TAPTable("obscore", TableType.view);
		ivoaSchema.removeAllTables();
		ivoaSchema.addTable(obscore);
		assertNotNull(metadata.getObsCoreTable());
		assertEquals("ivoa.obscore", metadata.getObsCoreTable().getFullName());

		// CASE: ObsCore table BUT in a different schema:
		metadata.removeAllSchemas();
		TAPSchema differentSchema = new TAPSchema("different");
		metadata.addSchema(differentSchema);
		differentSchema.addTable("ObsCore");
		assertNull(metadata.getObsCoreTable());
	}

}
