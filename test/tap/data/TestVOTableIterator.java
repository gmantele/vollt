package tap.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import uk.ac.starlink.table.TableFormatException;

public class TestVOTableIterator {

	public final static String directory = "./test/tap/data/";

	public final static File dataVOTable = new File(directory + "testdata.vot");
	public final static File binaryVOTable = new File(directory + "testdata_binary.vot");

	public final static File emptyVOTable = new File(directory + "emptyset.vot");
	public final static File emptyBinaryVOTable = new File(directory + "emptyset_binary.vot");

	@Test
	public void testWithNULL(){
		try{
			new VOTableIterator(null);
			fail("The constructor should have failed, because: the given VOTable is NULL.");
		}catch(Exception ex){
			assertEquals(ex.getClass().getName(), "java.lang.NullPointerException");
		}
	}

	@Test
	public void testWithData(){
		InputStream input = null;
		TableIterator it = null;
		try{
			input = new BufferedInputStream(new FileInputStream(dataVOTable));
			it = new VOTableIterator(input);
			// TEST there is column metadata before starting the iteration:
			assertTrue(it.getMetadata() != null);
			final int expectedNbLines = 100, expectedNbColumns = 4;
			int countLines = 0, countColumns = 0;
			while(it.nextRow()){
				// count lines:
				countLines++;
				// reset columns count:
				countColumns = 0;
				while(it.hasNextCol()){
					it.nextCol();
					// count columns
					countColumns++;
					// TEST the column type is set (not null):
					assertTrue(it.getColType() != null);
				}
				// TEST that all columns have been read:
				assertEquals(expectedNbColumns, countColumns);
			}
			// TEST that all lines have been read:
			assertEquals(expectedNbLines, countLines);

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct VOTable (containing some valid rows).");
		}finally{
			try{
				if (input != null)
					input.close();
			}catch(IOException e){
				e.printStackTrace();
			}
			if (it != null){
				try{
					it.close();
				}catch(DataReadException dre){}
			}
		}
	}

	@Test
	public void testWithBinary(){
		InputStream input = null;
		TableIterator it = null;
		try{
			input = new BufferedInputStream(new FileInputStream(binaryVOTable));
			it = new VOTableIterator(input);
			// TEST there is column metadata before starting the iteration:
			assertTrue(it.getMetadata() != null);
			final int expectedNbLines = 100, expectedNbColumns = 4;
			int countLines = 0, countColumns = 0;
			while(it.nextRow()){
				// count lines:
				countLines++;
				// reset columns count:
				countColumns = 0;
				while(it.hasNextCol()){
					it.nextCol();
					// count columns
					countColumns++;
					// TEST the column type is set (not null):
					assertTrue(it.getColType() != null);
				}
				// TEST that all columns have been read:
				assertEquals(expectedNbColumns, countColumns);
			}
			// TEST that all lines have been read:
			assertEquals(expectedNbLines, countLines);

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct VOTable (containing some valid rows).");
		}finally{
			try{
				if (input != null)
					input.close();
			}catch(IOException e){
				e.printStackTrace();
			}
			if (it != null){
				try{
					it.close();
				}catch(DataReadException dre){}
			}
		}
	}

	@Test
	public void testWithEmptySet(){
		InputStream input = null;
		TableIterator it = null;
		try{
			input = new BufferedInputStream(new FileInputStream(emptyVOTable));
			it = new VOTableIterator(input);
			// TEST there is column metadata before starting the iteration:
			assertTrue(it.getMetadata() != null);
			int countLines = 0;
			// count lines:
			while(it.nextRow())
				countLines++;
			// TEST that no line has been read:
			assertEquals(countLines, 0);

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct VOTable (even if empty).");
		}finally{
			try{
				if (input != null)
					input.close();
			}catch(IOException e){
				e.printStackTrace();
			}
			if (it != null){
				try{
					it.close();
				}catch(DataReadException dre){}
			}
		}
	}

	@Test
	public void testWithEmptyBinarySet(){
		InputStream input = null;
		TableIterator it = null;
		try{
			input = new BufferedInputStream(new FileInputStream(emptyBinaryVOTable));
			it = new VOTableIterator(input);
			// TEST there is column metadata before starting the iteration:
			assertTrue(it.getMetadata() != null);
			int countLines = 0;
			// count lines:
			while(it.nextRow())
				countLines++;
			// TEST that no line has been read:
			assertEquals(countLines, 0);

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("An exception occurs while reading a correct binary VOTable (even if empty).");
		}finally{
			try{
				if (input != null)
					input.close();
			}catch(IOException e){
				e.printStackTrace();
			}
			if (it != null){
				try{
					it.close();
				}catch(DataReadException dre){}
			}
		}
	}

	@Test
	public void testWithNotAVotable(){
		// CASE: Empty file!
		File emptyFile = new File("test/tap/data/emptyFile");
		InputStream input = null;
		VOTableIterator it = null;
		try{
			// Create an empty file:
			emptyFile.createNewFile();

			// Prepare reading it:
			input = new FileInputStream(emptyFile);

			// Start the iteration:
			it = new VOTableIterator(input);

			// Try to read the data:
			it.getMetadata();
			fail("An exception was expected because the file is empty!");

		}catch(IOException ioe){
			ioe.printStackTrace();
			fail("Another exception was expected here! (see the console for more details)");
		}catch(DataReadException dre){
			assertNotNull(dre.getCause());
			assertEquals(TableFormatException.class, dre.getCause().getClass());
			assertEquals("The input file is not a valid VOTable document! Cause: [l.1, c.1] Premature end of file.", dre.getMessage());
		}finally{
			// Close the input stream (if open):
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe){
					ioe.printStackTrace();
				}
			}
			// Close the iterator (if any):
			if (it != null){
				try{
					it.close();
				}catch(DataReadException dre){
					dre.printStackTrace();
				}
			}
			// Delete the temporary empty file (if exists):
			if (emptyFile.exists())
				emptyFile.delete();
		}

		// CASE: Neither empty nor a VOTable file!
		File notAVotFile = new File("test/tap/db_testtools/db-test/create-db.sql");
		input = null;
		it = null;
		try{
			// Prepare reading it:
			input = new FileInputStream(notAVotFile);

			// Start the iteration:
			it = new VOTableIterator(input);

			// Try to read the data:
			it.getMetadata();
			fail("An exception was expected because the file is not a VOTable!");

		}catch(IOException ioe){
			ioe.printStackTrace();
			fail("Another exception was expected here! (see the console for more details)");
		}catch(DataReadException dre){
			assertNotNull(dre.getCause());
			assertEquals(TableFormatException.class, dre.getCause().getClass());
			assertEquals("The input file is not a valid VOTable document! Cause: [l.2, c.1] Content is not allowed in prolog.", dre.getMessage());
		}finally{
			// Close the input stream (if open):
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe){
					ioe.printStackTrace();
				}
			}
			// Close the iterator (if any):
			if (it != null){
				try{
					it.close();
				}catch(DataReadException dre){
					dre.printStackTrace();
				}
			}
		}

		// CASE: A VOTable with no field declared!
		File notAValidVotableFile = new File("test/tap/data/testdata_no-field.vot");
		input = null;
		it = null;
		try{
			// Prepare reading it:
			input = new FileInputStream(notAValidVotableFile);

			// Start the iteration:
			it = new VOTableIterator(input);

			// Try to read the data:
			it.getMetadata();
			fail("An exception was expected because no FIELD is declared in this VOTable!");

		}catch(IOException ioe){
			ioe.printStackTrace();
			fail("Another exception was expected here! (see the console for more details)");
		}catch(DataReadException dre){
			assertEquals("Unexpected VOTable document: no FIELD can be found!", dre.getMessage());
		}finally{
			// Close the input stream (if open):
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe){
					ioe.printStackTrace();
				}
			}
			// Close the iterator (if any):
			if (it != null){
				try{
					it.close();
				}catch(DataReadException dre){
					dre.printStackTrace();
				}
			}
		}
	}
}
