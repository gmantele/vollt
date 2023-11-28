package uws.service.file.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestRotateFileAction {

	final String TMP_DIR = System.getProperty("java.io.tmpdir");

	File srcFile;
	File destFile;

	@Before
	public void setUp() throws Exception{
		final String SUFFIX = "" + System.currentTimeMillis();
		srcFile = new File(TMP_DIR, "taptest_rotate_src_" + SUFFIX);
		destFile = new File(TMP_DIR, "taptest_rotate_dest_" + SUFFIX);
	}

	@After
	public void tearDown() throws Exception{
		if (srcFile != null){
			srcFile.delete();
			srcFile = null;
		}
		if (destFile != null){
			destFile.delete();
			destFile = null;
		}
	}

	@Test
	public void testConstructor(){
		/* CASE: No source file => ERROR */
		try{
			new RotateFileAction(null, null);
			fail("This construction should have failed because no source file has been provided!");
		}catch(Exception ex){
			assertEquals(NullPointerException.class, ex.getClass());
			assertEquals("Missing source file!", ex.getMessage());
		}
		/* CASE: The source file does not exist => ERROR */
		try{
			assertFalse(srcFile.exists());
			new RotateFileAction(srcFile, null);
			fail("This construction should have failed because the source file does not exist.");
		}catch(Exception ex){
			assertEquals(IllegalArgumentException.class, ex.getClass());
			assertEquals("The source file \"" + srcFile.getAbsolutePath() + "\" does not exist!", ex.getMessage());
		}
		/* CASE: The source file is a directory => ERROR */
		try{
			assertTrue(srcFile.mkdir());
			new RotateFileAction(srcFile, null);
			fail("This construction should have failed because the source file is a directory!");
		}catch(Exception ex){
			assertEquals(IllegalArgumentException.class, ex.getClass());
			assertEquals("The source file \"" + srcFile.getAbsolutePath() + "\" is a directory instead of a regular file!", ex.getMessage());
		}finally{
			srcFile.delete();
		}
		/* CASE: No dest file => ERROR */
		try{
			assertTrue(srcFile.createNewFile());
			new RotateFileAction(srcFile, null);
			fail("This construction should have failed because the dest file is missing!");
		}catch(Exception ex){
			assertEquals(NullPointerException.class, ex.getClass());
			assertEquals("Missing target file!", ex.getMessage());
		}finally{
			srcFile.delete();
		}
		/* CASE: the dest file exists and is a directory => ERROR */
		try{
			assertTrue(srcFile.createNewFile());
			assertTrue(destFile.mkdir());
			new RotateFileAction(srcFile, destFile);
			fail("This construction should have failed because the dest file is a directory!");
		}catch(Exception ex){
			assertEquals(IllegalArgumentException.class, ex.getClass());
			assertEquals("The target file \"" + destFile.getAbsolutePath() + "\" is a directory instead of a regular file!", ex.getMessage());
		}finally{
			srcFile.delete();
			destFile.delete();
		}
		/* CASE: Everything is OK! */
		try{
			assertTrue(srcFile.createNewFile());
			assertFalse(destFile.exists());
			new RotateFileAction(srcFile, destFile);
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unexpected error while construction! (see console for more details)");
		}
	}

	@Test
	public void testRun(){
		/* CASE: Destination does not exist yet */
		try{
			// Create a source file:
			assertTrue(srcFile.createNewFile());

			// Ensure the dest file does exist yet:
			assertFalse(destFile.exists());

			// Run the action:
			CloseAction action = new RotateFileAction(srcFile, destFile);
			action.run();

			/* Check the source file does not exist anymore
			 * BUT the dest one yes: */
			assertFalse(srcFile.exists());
			assertTrue(destFile.exists());
		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unexpected IO error! (see console for more details)");
		}finally{
			srcFile.delete();
			destFile.delete();
		}

		/* CASE: Destination already exists */
		try{
			// Create a source file:
			assertTrue(srcFile.createNewFile());

			// Sleep for 1 second (so that the last modified date can be different):
			Thread.sleep(1000);

			// Already create the dest:
			assertTrue(destFile.createNewFile());
			long lastModified = destFile.lastModified();

			// Run the action:
			CloseAction action = new RotateFileAction(srcFile, destFile);
			action.run();

			/* Check the source file does not exist anymore
			 * AND the destination file does still exist
			 * BUT with a different lastModified date: */
			assertFalse(srcFile.exists());
			assertTrue(destFile.exists());
			assertNotEquals(lastModified, destFile.lastModified());

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unexpected IO error! (see console for more details)");
		}
	}

}
