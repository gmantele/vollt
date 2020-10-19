package uws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TestUWSToolBox {

	@Test
	public void testGetMimeType(){
		// TEST: NULL => NULL
		assertNull(UWSToolBox.getMimeType(null));

		// TEST: empty string => binary
		assertEquals("application/octet-stream", UWSToolBox.getMimeType(""));
		assertEquals("application/octet-stream", UWSToolBox.getMimeType(" 	 "));

		/*
		 * Note: each test is also performed with the file name and file
		 *       extension separator(.).
		 */

		// TEST: unknown file extension => NULL
		assertNull(UWSToolBox.getMimeType("foo"));
		assertNull(UWSToolBox.getMimeType(".foo"));

		/* Ensure length of known MIME types and file extensions have the same
		 * number of items: */
		assertEquals(UWSToolBox.fileExts.length, UWSToolBox.mimeTypes.length);

		// Roughly check the MIME types and file extensions mapping:
		String ext, mime;
		/* ...1st item (i.e. binary) */
		ext = UWSToolBox.fileExts[0];
		mime = UWSToolBox.mimeTypes[0];
		assertEquals("", ext);
		assertEquals("application/octet-stream", mime);
		assertEquals(mime, UWSToolBox.getMimeType(ext));
		assertEquals(mime, UWSToolBox.getMimeType("." + ext));
		/* ...last item (i.e. bmp) */
		ext = UWSToolBox.fileExts[UWSToolBox.fileExts.length - 1];
		mime = UWSToolBox.mimeTypes[UWSToolBox.mimeTypes.length - 1];
		assertEquals("bmp", ext);
		assertEquals("image/x-portable-bitmap", mime);
		assertEquals(mime, UWSToolBox.getMimeType(ext));
		assertEquals(mime, UWSToolBox.getMimeType("." + ext));

		// TEST: VOTable mapping => ok
		assertEquals("application/x-votable+xml", UWSToolBox.getMimeType("vot"));
		assertEquals("application/x-votable+xml", UWSToolBox.getMimeType(".vot"));

		// Ensure it is not not case sensitive:
		assertEquals("application/x-votable+xml", UWSToolBox.getMimeType(".VOt"));
	}

	@Test
	public void testGetFileExtension(){
		// TEST: NULL => NULL
		assertNull(UWSToolBox.getFileExtension(null));

		// TEST: empty string => NULL
		assertNull(UWSToolBox.getFileExtension(""));
		assertNull(UWSToolBox.getFileExtension(" 	 "));

		// TEST: unknown MIME type => NULL
		assertNull(UWSToolBox.getMimeType("foo"));

		/* Ensure length of known MIME types and file extensions have the same
		 * number of items: */
		assertEquals(UWSToolBox.mimeTypes.length, UWSToolBox.fileExts.length);

		// Roughly check the MIME types and file extensions mapping:
		String ext, mime;
		/* ...1st item (i.e. binary) */
		mime = UWSToolBox.mimeTypes[0];
		ext = UWSToolBox.fileExts[0];
		assertEquals("application/octet-stream", mime);
		assertEquals("", ext);
		assertEquals(ext, UWSToolBox.getFileExtension(mime));
		/* ...last item (i.e. bmp) */
		mime = UWSToolBox.mimeTypes[UWSToolBox.mimeTypes.length - 1];
		ext = UWSToolBox.fileExts[UWSToolBox.fileExts.length - 1];
		assertEquals("image/x-portable-bitmap", mime);
		assertEquals("bmp", ext);
		assertEquals(ext, UWSToolBox.getFileExtension(mime));

		// TEST: VOTable mapping => ok
		assertEquals("vot", UWSToolBox.getFileExtension("application/x-votable+xml"));

		// Ensure it is not not case sensitive:
		assertEquals("vot", UWSToolBox.getFileExtension("application/x-VOTable+XML"));
	}

}
