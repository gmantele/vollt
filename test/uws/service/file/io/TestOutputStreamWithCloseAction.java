package uws.service.file.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOutputStreamWithCloseAction {
	final String TMP_DIR = System.getProperty("java.io.tmpdir");

	File srcFile;
	File destFile;

	@Before
	public void setUp() throws Exception{
		final String SUFFIX = "" + System.currentTimeMillis();
		srcFile = new File(TMP_DIR, "taptest_stream_src_" + SUFFIX);
		destFile = new File(TMP_DIR, "taptest_stream_dest_" + SUFFIX);
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
	public void testOutputStreamWithCloseAction(){
		/* CASE: Missing OutputStream to wrap => ERROR */
		OutputStreamWithCloseAction out = null;
		try{
			out = new OutputStreamWithCloseAction(null, null);
			fail("This construction should have failed because no OutputStream has been provided!");
		}catch(Exception ex){
			assertEquals(NullPointerException.class, ex.getClass());
			assertEquals("Missing OutputStream to wrap!", ex.getMessage());
		}finally{
			if (out != null){
				try{
					out.close();
				}catch(IOException ioe){
				}
			}
		}
	}

	@Test
	public void testClose(){
		OutputStreamWithCloseAction out = null;
		try{
			// Open the stream toward the temp file:
			out = new OutputStreamWithCloseAction(new FileOutputStream(srcFile), new RotateFileAction(srcFile, destFile));

			out.write("File\n".getBytes());
			out.flush();
			out.write("successfully written!".getBytes());

			// Close the stream, and so, rotate the files:
			out.close();

			// Check the files have been rotated:
			assertFalse(srcFile.exists());
			assertTrue(destFile.exists());

			// Check the content of the dest file:
			try(InputStream input = new FileInputStream(destFile)){
				byte[] buffer = new byte[256];
				int nbRead = 0;
				nbRead = input.read(buffer);
				assertEquals(26, nbRead);
				assertEquals("File\nsuccessfully written!", new String(buffer, 0, nbRead));
			}

		}catch(Exception ex){
			ex.printStackTrace(System.err);
			fail("Unexpected error! (see the console for more details)");
		}finally{
			if (out != null){
				try{
					out.close();
				}catch(IOException ioe){
				}
			}
		}
	}

}
