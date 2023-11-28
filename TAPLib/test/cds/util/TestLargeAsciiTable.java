package cds.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.NoSuchElementException;

import org.junit.Test;

import cds.util.LargeAsciiTable.AsciiTableIterator;
import cds.util.LargeAsciiTable.LineProcessor;
import cds.util.LargeAsciiTable.LineProcessorException;

public class TestLargeAsciiTable {

	@Test
	public void testReset(){
		// Empty constructor:
		try(LargeAsciiTable table = new LargeAsciiTable()){

			// Ensure nothing is set:
			assertNull(table.sep);
			assertEquals(LargeAsciiTable.DEFAULT_MEMORY_THRESHOLD, table.memoryThreshold);
			assertEquals(LargeAsciiTable.DEFAULT_MEMORY_THRESHOLD, table.getMemoryThreshold());
			assertNull(table.lines);
			assertNull(table.bufferFile);
			assertNull(table.out);
			assertEquals(0, table.nbLines);
			assertEquals(0, table.nbTotalLines);
			assertNull(table.SPACES);
			assertNull(table.HSEP);
			assertNull(table.sizes);
			assertTrue(table.empty);
			assertEquals(0, table.headerSize);
			assertNull(table.headerPostfix);
			assertFalse(table.complete);
			assertTrue(table.closed);

			// CASE: Reset the table with custom valid arguments => ok
			final int memThreshold = LargeAsciiTable.DEFAULT_MEMORY_THRESHOLD + 10;
			table.reset('%', memThreshold);
			// Check that everything is now ok to add lines:
			assertEquals('%', table.csep);
			assertEquals("%", table.sep);
			assertEquals(memThreshold, table.memoryThreshold);
			assertEquals(memThreshold, table.getMemoryThreshold());
			assertNotNull(table.lines);
			assertEquals(table.memoryThreshold, table.lines.length);
			assertNull(table.bufferFile);
			assertNull(table.out);
			assertEquals(0, table.nbLines);
			assertEquals(0, table.nbTotalLines);
			assertEquals("          ", table.SPACES.toString());
			assertEquals("--------------------", table.HSEP.toString());
			assertNull(table.sizes);
			assertTrue(table.empty);
			assertEquals(0, table.headerSize);
			assertNull(table.headerPostfix);
			assertFalse(table.complete);
			assertFalse(table.closed);

			// CASE: Negative or 0 threshold => Default memory threshold
			table.reset('%', 0);
			assertEquals(LargeAsciiTable.DEFAULT_MEMORY_THRESHOLD, table.memoryThreshold);
			assertEquals(LargeAsciiTable.DEFAULT_MEMORY_THRESHOLD, table.getMemoryThreshold());
			table.reset('%', -10);
			assertEquals(LargeAsciiTable.DEFAULT_MEMORY_THRESHOLD, table.memoryThreshold);
			assertEquals(LargeAsciiTable.DEFAULT_MEMORY_THRESHOLD, table.getMemoryThreshold());

			// CASE: threshold = 1 => ok
			table.reset('%', 1);
			assertEquals(1, table.memoryThreshold);
			assertEquals(1, table.getMemoryThreshold());
		}

	}

	@Test
	public void testClose(){
		// Create a table with the smallest memory threshold as possible:
		try(LargeAsciiTable table = new LargeAsciiTable('|', 1)){
			// And add enough lines so that a buffer file is created:
			table.addLine("val1.1|val1.2");
			table.addLine("val2.2|val2.2");

			/*
			 * NOTE:
			 *   Do not end the table (endTable()) in order to ensure close()
			 *   does it!
			 */

			// Check a buffer file has been created:
			final File bufferFile = table.bufferFile;
			assertNotNull(table.bufferFile);
			assertTrue(table.bufferFile.exists());

			// Check the state before closing the table:
			assertNotNull(table.out);
			assertNotNull(table.lines);
			assertNotNull(table.SPACES);
			assertNotNull(table.HSEP);
			assertNotNull(table.sizes);
			assertNotNull(table.sep);
			assertFalse(table.closed);

			// Close the table:
			table.close();

			// Ensure all resources are released:
			assertNull(table.out);
			assertNull(table.bufferFile);
			assertFalse(bufferFile.exists());
			assertNull(table.lines);
			assertNull(table.SPACES);
			assertNull(table.HSEP);
			assertNull(table.sizes);
			assertNull(table.sep);
			assertTrue(table.closed);
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test close()! (see console for more details)");
		}
	}

	@Test
	public void testAddHeaderLine(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){

			// CASE: NULL or empty line => false
			assertFalse(table.addHeaderLine(null));
			assertFalse(table.addHeaderLine(""));
			assertFalse(table.addHeaderLine("  "));
			assertFalse(table.addHeaderLine(" 	 "));
			assertEquals(0, table.headerSize);
			assertEquals(table.headerSize, table.headerSize());
			assertEquals(0, table.nbLines);
			assertEquals(table.nbLines, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertNull(table.bufferFile);

			// CASE: add a valid header line => ok
			assertTrue(table.addHeaderLine("col1|col2"));
			assertTrue(table.addHeaderLine("another header blabla"));
			assertEquals(2, table.headerSize);
			assertEquals(table.headerSize, table.headerSize());
			assertEquals(2, table.nbLines);
			assertEquals(table.nbLines, table.nbTotalLines);
			assertEquals(table.nbTotalLines + 1, table.size()); // +1 for the header/data separation line
			assertNull(table.bufferFile);

			// CASE: add header line after some data added => IllegalStateException
			table.addLine("val1|val2");
			try{
				table.addHeaderLine("additional header line");
				fail("This function call should have failed with an error!");
			}catch(Exception ex){
				assertEquals(IllegalStateException.class, ex.getClass());
				assertEquals(LargeAsciiTable.ERROR_HEADER_CLOSED, ex.getMessage());
			}

			// CASE: add header line after the table completed => IllegalStateException
			table.endTable();
			try{
				table.addHeaderLine("additional header line");
				fail("This function call should have failed with an error!");
			}catch(Exception ex){
				assertEquals(IllegalStateException.class, ex.getClass());
				assertEquals(LargeAsciiTable.ERROR_TABLE_COMPLETE, ex.getMessage());
			}

			// CASE: add header line in a closed table => IllegalStateException
			table.close();
			try{
				table.addHeaderLine("additional header line");
				fail("This function call should have failed with an error!");
			}catch(Exception ex){
				assertEquals(IllegalStateException.class, ex.getClass());
				assertEquals(LargeAsciiTable.ERROR_TABLE_CLOSED, ex.getMessage());
			}

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test close()! (see console for more details)");
		}
	}

	@Test
	public void testAddLine(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){

			// CASE: NULL or empty line => false
			assertFalse(table.addLine(null));
			assertFalse(table.addLine(""));
			assertFalse(table.addLine("  "));
			assertFalse(table.addLine(" 	 "));
			assertEquals(0, table.headerSize);
			assertEquals(table.headerSize, table.headerSize());
			assertEquals(0, table.nbLines);
			assertEquals(table.nbLines, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertNull(table.bufferFile);

			// CASE: first line
			/* ...SUB-CASE: only one column
			 *              => sizes.length=0 and empty=false */
			assertTrue(table.addLine("col1"));
			assertEquals(0, table.headerSize);
			assertEquals(table.headerSize, table.headerSize());
			assertEquals(1, table.nbLines);
			assertEquals(1, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertEquals("col1", table.lines[0]);
			assertNotNull(table.sizes);
			assertEquals(1, table.sizes.length);
			assertEquals(4, table.sizes[0]);
			assertFalse(table.empty);
			assertNull(table.bufferFile);
			/* ...SUB-CASE: more than one column
			 *              => sizes.length=N and empty=false */
			table.reset('|');
			assertTrue(table.addLine(" col1|  column2 |COLUMN three_3 "));
			assertEquals(0, table.headerSize);
			assertEquals(table.headerSize, table.headerSize());
			assertEquals(1, table.nbLines);
			assertEquals(table.nbLines, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertEquals(" col1|  column2 |COLUMN three_3 ", table.lines[0]);
			assertNotNull(table.sizes);
			assertEquals(3, table.sizes.length);
			assertEquals(5, table.sizes[0]);
			assertEquals(10, table.sizes[1]);
			assertEquals(15, table.sizes[2]);
			assertFalse(table.empty);
			assertNull(table.bufferFile);

			// CASE: now, add a line with the wrong number of columns
			/* ...SUB-CASE: less columns => only the stat about the first one is
			 *                              updated */
			assertTrue(table.addLine("value #1"));
			assertEquals(2, table.nbLines);
			assertEquals(table.nbLines, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertEquals("value #1", table.lines[1]);
			assertEquals(3, table.sizes.length);
			assertEquals(8, table.sizes[0]);
			assertEquals(10, table.sizes[1]);
			assertEquals(15, table.sizes[2]);
			assertNull(table.bufferFile);
			/* ...SUB-CASE: more columns => the additional columns are appended
			 *                              in the last expected column */
			assertTrue(table.addLine("val1|val2|val3|val4|val5|val6"));
			assertEquals(3, table.nbLines);
			assertEquals(table.nbLines, table.nbTotalLines);
			assertEquals("val1|val2|val3|val4|val5|val6", table.lines[2]);
			assertEquals(3, table.sizes.length);
			assertEquals(8, table.sizes[0]);
			assertEquals(10, table.sizes[1]);
			assertEquals(19, table.sizes[2]);
			assertNull(table.bufferFile);

			// CASE: add a line in a complete table => IllegalStateException
			table.endTable();
			try{
				table.addLine("additional line");
				fail("This function call should have failed with an error!");
			}catch(Exception ex){
				assertEquals(IllegalStateException.class, ex.getClass());
				assertEquals(LargeAsciiTable.ERROR_TABLE_COMPLETE, ex.getMessage());
			}

			// CASE: add a line in a closed table => IllegalStateException
			table.close();
			try{
				table.addLine("additional header line");
				fail("This function call should have failed with an error!");
			}catch(Exception ex){
				assertEquals(IllegalStateException.class, ex.getClass());
				assertEquals(LargeAsciiTable.ERROR_TABLE_CLOSED, ex.getMessage());
			}

			// CASE: more lines than the memory threshold => use a buffer file
			table.reset('|', 1);
			// first line => ok (still in memory)
			table.addLine("line 1");
			assertEquals(1, table.nbLines);
			assertEquals(table.nbLines, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertEquals("line 1", table.lines[0]);
			assertNull(table.bufferFile);
			assertNull(table.out);
			// second line => now, on disk
			table.addLine("line 2");
			assertEquals(1, table.nbLines);
			assertEquals(2, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertEquals("line 2", table.lines[0]);
			assertNotNull(table.bufferFile);
			assertNotNull(table.out);
			// third line => on disk, again
			table.addLine("line 3");
			assertEquals(1, table.nbLines);
			assertEquals(3, table.nbTotalLines);
			assertEquals(table.nbTotalLines, table.size());
			assertEquals("line 3", table.lines[0]);
			assertNotNull(table.bufferFile);
			assertNotNull(table.out);

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test addLine(...)! (see console for more details)");
		}
	}

	@Test
	public void testEndTable(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){

			assertTrue(table.empty);
			assertEquals(0, table.nbTotalLines);
			assertFalse(table.complete);
			assertFalse(table.closed);

			// CASE: end an empty table => ok
			table.endTable();
			assertTrue(table.empty);
			assertEquals(0, table.nbTotalLines);
			assertTrue(table.complete);
			assertFalse(table.closed);

			// CASE: end an already complete table => ok
			table.endTable();
			assertTrue(table.empty);
			assertEquals(0, table.nbTotalLines);
			assertTrue(table.complete);
			assertFalse(table.closed);

			// CASE: end a closed table => ok
			table.close();
			assertTrue(table.closed);
			table.endTable();
			assertTrue(table.empty);
			assertEquals(0, table.nbTotalLines);
			assertTrue(table.complete);
			assertTrue(table.closed);

			// CASE: end a non empty table stored in memory => ok
			table.reset('|');
			table.addLine("col1|col2");
			assertNull(table.bufferFile);
			assertNull(table.out);
			table.endTable();
			assertFalse(table.empty);
			assertEquals(1, table.nbTotalLines);
			assertTrue(table.complete);
			assertFalse(table.closed);
			assertNull(table.bufferFile);
			assertNull(table.out);

			/* CASE: end a non empty table stored on disk
			 *       => ok (stream toward the buffer file closed, but the buffer
			 *              file still exists) */
			table.reset('|', 1);
			assertEquals(1, table.getMemoryThreshold());
			table.addLine("val 1|val 2");
			table.addLine("val 3|val 4");
			assertNotNull(table.out);
			assertNotNull(table.bufferFile);
			assertTrue(table.bufferFile.exists());
			table.endTable();
			assertFalse(table.empty);
			assertEquals(2, table.nbTotalLines);
			assertTrue(table.complete);
			assertFalse(table.closed);
			assertNull(table.out);
			assertNotNull(table.bufferFile);
			assertTrue(table.bufferFile.exists());

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test endTable()! (see console for more details)");
		}
	}

	@Test
	public void testGetIterator(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){

			// CASE: non-complete table => IllegalStateException
			assertFalse(table.complete);
			assertFalse(table.closed);
			try{
				table.getIterator();
				fail("It should be impossible to create an iterator over a non-complete table!");
			}catch(Exception ex){
				assertEquals(IllegalStateException.class, ex.getClass());
				assertEquals(LargeAsciiTable.ERROR_TABLE_NOT_COMPLETE, ex.getMessage());
			}

			// CASE: closed table => IllegalStateException
			table.close();
			assertTrue(table.closed);
			try{
				table.getIterator();
				fail("It should be impossible to create an iterator over a closed table!");
			}catch(Exception ex){
				assertEquals(IllegalStateException.class, ex.getClass());
				assertEquals(LargeAsciiTable.ERROR_TABLE_CLOSED, ex.getMessage());
			}

			// CASE: memory (complete but not closed) table
			// ...SUB-CASE: empty table => ok
			table.reset('|');
			table.endTable();
			assertTrue(table.empty);
			assertTrue(table.complete);
			assertFalse(table.closed);
			try(AsciiTableIterator it = table.getIterator()){
				assertNotNull(it);
				assertFalse(it.hasNext());
			}
			// ...SUB-CASE: non-empty table => ok
			table.reset('|');
			table.addHeaderLine("col1|col2");
			table.addLine("val1|value #2");
			table.endTable();
			assertNull(table.bufferFile);
			assertFalse(table.empty);
			assertTrue(table.complete);
			assertFalse(table.closed);
			try(AsciiTableIterator it = table.getIterator()){
				assertNotNull(it);
				String[] expectedLines = new String[]{ "col1|col2", "val1|value #2" };
				for(String l : expectedLines){
					assertTrue(it.hasNext());
					assertEquals(l, it.next());
				}
				assertFalse(it.hasNext());
				try{
					it.next();
					fail("No more line available in this iterator => an error was expected!");
				}catch(Exception ex){
					assertEquals(NoSuchElementException.class, ex.getClass());
					assertEquals("No more line available!", ex.getMessage());
				}
			}

			// CASE: non-empty disk (complete but not closed) table => ok
			table.reset('|', 2);
			table.addHeaderLine("col1|col2");
			table.addLine("val1|value #2");
			table.addLine("val3|value #4");
			table.endTable();
			assertNotNull(table.bufferFile);
			assertFalse(table.empty);
			assertEquals(1, table.headerSize());
			assertEquals(3, table.nbTotalLines);
			assertEquals(table.nbTotalLines + 1, table.size());
			assertTrue(table.complete);
			assertFalse(table.closed);
			try(AsciiTableIterator it = table.getIterator()){
				assertNotNull(it);
				String[] expectedLines = new String[]{ "col1|col2", "val1|value #2", "val3|value #4" };
				for(String l : expectedLines){
					assertTrue(it.hasNext());
					assertEquals(l, it.next());
				}
				assertFalse(it.hasNext());
				try{
					it.next();
					fail("No more line available in this iterator => an error was expected!");
				}catch(Exception ex){
					assertEquals(NoSuchElementException.class, ex.getClass());
					assertEquals("No more line available!", ex.getMessage());
				}
			}

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test getIterator()! (see console for more details)");
		}
	}

	@Test
	public void testStreamRawLineProcessorChar(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){

			/*
			 * NOTE:
			 * 	No need to test for non-complete and closed tables because
			 * 	this is done by the internal call to getIterator(). So these
			 * 	tests are already checked above by testGetIterator().
			 */

			// CASE: no LineProcessor => -1 (nothing has been done).
			assertEquals(-1, table.streamRaw(null));

			// CASE: empty (complete) table => 0
			table.endTable();
			AssertLineProcessor lineProc = new AssertLineProcessor();
			assertEquals(table.nbTotalLines, table.streamRaw(lineProc));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: non-empty (complete) table without header => ok
			table.reset('|');
			table.addLine("val1|col val 2");
			table.addLine("value #3|column's value 4");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "val1|col val 2", "value #3|column's value 4" });
			assertEquals(table.nbTotalLines, table.streamRaw(lineProc));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: idem with a different column separator => ok
			lineProc = new AssertLineProcessor(new String[]{ "val1%col val 2", "value #3%column's value 4" });
			assertEquals(table.nbTotalLines, table.streamRaw(lineProc, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: idem but with a header line => ok
			table.reset('|');
			table.addHeaderLine("col1|column 2");
			table.addLine("val1|col val 2");
			table.addLine("value #3|column's value 4");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "col1%column 2", "val1%col val 2", "value #3%column's value 4" });
			assertEquals(table.nbTotalLines, table.streamRaw(lineProc, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: a line with less columns than the first one => ok
			table.reset('|');
			table.addHeaderLine("col1|column 2|col3");
			table.addLine("val1|col val 2");
			table.addLine("value #3||value 4");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "col1%column 2%col3", "val1%col val 2", "value #3%%value 4" });
			assertEquals(table.nbTotalLines, table.streamRaw(lineProc, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: a line with more columns than the first one => ok
			table.reset('|');
			table.addHeaderLine("col1|column 2");
			table.addLine("val1|col val 2|additional col value");
			table.addLine("value #3|column's value 4");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "col1%column 2", "val1%col val 2%additional col value", "value #3%column's value 4" });
			assertEquals(table.nbTotalLines, table.streamRaw(lineProc, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: line proc failed => failed processed lines not counted
			assertEquals(table.nbTotalLines - 1, table.streamRaw(new LineProcessor() {
				private int numLine = 0;

				@Override
				public boolean process(String line) throws LineProcessorException{
					if (numLine++ == 1)
						return false;
					else
						return true;
				}
			}));

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test streamRaw()! (see console for more details)");
		}
	}

	@Test
	public void testAddspaces(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){
			StringBuffer buf = new StringBuffer();
			int originalSpacesLength;

			// CASE: negative or 0 number of spaces => nothing changed
			originalSpacesLength = table.SPACES.length();
			for(int nb : new int[]{ -10, 0 }){
				table.addspaces(buf, nb);
				assertEquals(0, buf.length());
				assertEquals(originalSpacesLength, table.SPACES.length());
			}

			// CASE: ask for less spaces than available => no change of SPACES
			table.addspaces(buf, originalSpacesLength - 2);
			assertEquals(originalSpacesLength - 2, buf.length());
			assertEquals(originalSpacesLength, table.SPACES.length());

			// CASE: ask for less spaces than available => no change of SPACES
			buf.delete(0, buf.length());
			table.addspaces(buf, originalSpacesLength * 2 + 2);
			assertEquals(originalSpacesLength * 2 + 2, buf.length());
			assertEquals(originalSpacesLength * 4, table.SPACES.length());

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test addspaces()! (see console for more details)");
		}
	}

	@Test
	public void testAddHsep(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){
			StringBuffer buf = new StringBuffer();
			int originalSepLength;

			// CASE: negative or 0 number of separators => nothing changed
			originalSepLength = table.HSEP.length();
			for(int nb : new int[]{ -10, 0 }){
				table.addHsep(buf, nb);
				assertEquals(0, buf.length());
				assertEquals(originalSepLength, table.HSEP.length());
			}

			// CASE: ask for less sep. than available => no change of HSEP
			table.addHsep(buf, originalSepLength - 2);
			assertEquals(originalSepLength - 2, buf.length());
			assertEquals(originalSepLength, table.HSEP.length());

			// CASE: ask for less sep. than available => no change of HSEP
			buf.delete(0, buf.length());
			table.addHsep(buf, originalSepLength * 2 + 2);
			assertEquals(originalSepLength * 2 + 2, buf.length());
			assertEquals(originalSepLength * 4, table.HSEP.length());

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test addHsep()! (see console for more details)");
		}
	}

	@Test
	public void testStreamAlignedLineProcessorIntArrayCharThread(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){

			final int[] pos = new int[]{ LargeAsciiTable.LEFT, LargeAsciiTable.CENTER, LargeAsciiTable.RIGHT };

			/*
			 * NOTE:
			 * 	No need to test for non-complete and closed tables because
			 * 	this is done by the internal call to getIterator(). So these
			 * 	tests are already checked above by testGetIterator().
			 */

			// CASE: no LineProcessor => -1 (nothing has been done).
			assertEquals(-1, table.streamAligned(null, pos));

			// CASE: empty (complete) table => 0
			table.endTable();
			AssertLineProcessor lineProc = new AssertLineProcessor();
			assertEquals(table.size(), table.streamAligned(lineProc, pos));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: non-empty (complete) table without header => ok
			table.reset('|');
			table.addLine("val1|col val 2|value 3");
			table.addLine("value #4|column's value 5|another value");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "val1    |   col val 2    |      value 3", "value #4|column's value 5|another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: idem with a different column separator => ok
			lineProc = new AssertLineProcessor(new String[]{ "val1    %   col val 2    %      value 3", "value #4%column's value 5%another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			/* CASE: idem with a postfix => ok (no postfix displayed because no
			 *                                  header) */
			table.setHeaderPostfix("ABC");
			assertEquals("ABC", table.getHeaderPostfix());
			lineProc = new AssertLineProcessor(new String[]{ "val1    %   col val 2    %      value 3", "value #4%column's value 5%another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: idem but with a header line => ok
			table.reset('|');
			table.addHeaderLine("col1|column 2|Third column");
			table.addLine("val1|col val 2|value 3");
			table.addLine("value #3|column's value 4|another value");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "  col1  |    column 2    |Third column ", "--------|----------------|-------------", "val1    |   col val 2    |      value 3", "value #3|column's value 4|another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: idem the same column separator => ok
			lineProc = new AssertLineProcessor(new String[]{ "  col1  |    column 2    |Third column ", "--------|----------------|-------------", "val1    |   col val 2    |      value 3", "value #3|column's value 4|another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos, table.csep));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: idem with a postfix and different column separator => ok
			table.setHeaderPostfix("ABC");
			assertEquals("ABC", table.getHeaderPostfix());
			lineProc = new AssertLineProcessor(new String[]{ "  col1  %    column 2    %Third column ", "--------%----------------%-------------ABC", "val1    %   col val 2    %      value 3", "value #3%column's value 4%another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			/* CASE: with less alignment rules (but not 1)
			 *       => ok (use the last specified alignment rule):*/
			table.reset('|');
			table.addHeaderLine("col1|column 2|Third column");
			table.addLine("val1|col val 2");
			table.addLine("value #3||another value");
			table.addLine("value #5|foo|another longer value");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "  col1  |column 2 |    Third column    ", "--------|---------|--------------------", "val1    |col val 2|", "value #3|         |   another value    ", "value #5|   foo   |another longer value" });
			assertEquals(table.size(), table.streamAligned(lineProc, new int[]{ pos[0], pos[1] }));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			/* CASE: with only one alignment rule => ok (use this single rule
			 *                                           for all columns): */
			table.reset('|');
			table.addHeaderLine("col1|column 2|Third column");
			table.addLine("val1|col val 2|value 3");
			table.addLine("value #3|column's value 4|another value");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "  col1  %    column 2    %Third column ", "--------%----------------%-------------", "    val1%       col val 2%      value 3", "value #3%column's value 4%another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, new int[]{ LargeAsciiTable.RIGHT }, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: with no alignment rule => ok (everything on the left)
			lineProc = new AssertLineProcessor(new String[]{ "  col1  %    column 2    %Third column ", "--------%----------------%-------------", "val1    %col val 2       %value 3      ", "value #3%column's value 4%another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, new int[0], '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);
			lineProc = new AssertLineProcessor(new String[]{ "  col1  %    column 2    %Third column ", "--------%----------------%-------------", "val1    %col val 2       %value 3      ", "value #3%column's value 4%another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, null, '%'));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: lines with less columns than the first one => ok
			table.reset('|');
			table.addHeaderLine("col1|column 2|Third column");
			table.addLine("val1|col val 2");
			table.addLine("value #3||another value");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "  col1  |column 2 |Third column ", "--------|---------|-------------", "val1    |col val 2|", "value #3|         |another value" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: a line with more columns than the first one => ok
			table.reset('|');
			table.addHeaderLine("col1|column 2");
			table.addLine("val1|col val 2|additional|column|value");
			table.addLine("value #3|columns' value 4");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "  col1  |            column 2             ", "--------|---------------------------------", "val1    |col val 2|additional|column|value", "value #3|        columns' value 4         " });
			assertEquals(table.size(), table.streamAligned(lineProc, pos));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: just a header line => ok
			table.reset('|');
			table.addHeaderLine("col1|column 2|Third column");
			table.endTable();
			lineProc = new AssertLineProcessor(new String[]{ "col1|column 2|Third column", "----|--------|------------" });
			assertEquals(table.size(), table.streamAligned(lineProc, pos));
			assertEquals(lineProc.numLine, lineProc.expectedLines.length);

			// CASE: line proc failed => failed processed lines not counted
			// ...SUB-CASE: no special column separator
			assertEquals(table.size() - 1, table.streamAligned(new LineProcessor() {
				private int numLine = 0;

				@Override
				public boolean process(String line) throws LineProcessorException{
					if (numLine++ == 1)
						return false;
					else
						return true;
				}
			}, pos));
			// ...SUB-CASE: with a different column separator
			assertEquals(table.size() - 1, table.streamAligned(new LineProcessor() {
				private int numLine = 0;

				@Override
				public boolean process(String line) throws LineProcessorException{
					if (numLine++ == 1)
						return false;
					else
						return true;
				}
			}, pos, '%'));

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test streamRaw()! (see console for more details)");
		}
	}

	@Test
	public void testToString(){
		try(LargeAsciiTable table = new LargeAsciiTable('|')){

			/*
			 * NOTE:
			 * 	No need to test for non-complete and closed tables because
			 * 	this is done by the internal call to getIterator(). So these
			 * 	tests are already checked above by testGetIterator().
			 */

			// CASE: empty table => empty string
			table.endTable();
			assertTrue(table.empty);
			assertTrue(table.complete);
			assertFalse(table.closed);
			assertEquals(0, table.toString().length());

			// CASE: only a header => the header + line separator
			table.reset('|');
			assertTrue(table.addHeaderLine("col1|column 2"));
			table.endTable();
			assertFalse(table.empty);
			assertTrue(table.complete);
			assertFalse(table.closed);
			assertEquals("col1|column 2\n----|--------", table.toString());

			// CASE: only data => ok
			table.reset('|');
			assertTrue(table.addLine("value 1|column's value 2"));
			assertTrue(table.addLine("value #3|value #4"));
			table.endTable();
			assertFalse(table.empty);
			assertTrue(table.complete);
			assertFalse(table.closed);
			assertEquals("value 1 |column's value 2\nvalue #3|value #4        ", table.toString());

			// CASE: header + data => ok
			table.reset('|');
			assertTrue(table.addHeaderLine("col1|column 2"));
			assertTrue(table.addLine("value 1|column's value 2"));
			assertTrue(table.addLine("value #3|value #4"));
			table.endTable();
			assertFalse(table.empty);
			assertTrue(table.complete);
			assertFalse(table.closed);
			assertEquals("  col1  |    column 2    \n--------|----------------\nvalue 1 |column's value 2\nvalue #3|value #4        ", table.toString());

		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected error prevented to test streamRaw()! (see console for more details)");
		}
	}

	private static class AssertLineProcessor implements LineProcessor {

		public final String[] expectedLines;
		public int numLine = 0;

		public AssertLineProcessor(){
			this(null);
		}

		public AssertLineProcessor(final String[] expected){
			this.expectedLines = (expected == null) ? new String[0] : expected;
		}

		@Override
		public boolean process(final String line) throws LineProcessorException{
			assertTrue(numLine < expectedLines.length);
			assertEquals(expectedLines[numLine++], line);
			return true;
		}
	}

}
