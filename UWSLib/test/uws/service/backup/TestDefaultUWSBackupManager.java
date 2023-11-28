package uws.service.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import uws.job.jobInfo.JobInfo;
import uws.job.jobInfo.SingleValueJobInfo;

public class TestDefaultUWSBackupManager {

	@Test
	public void testBase64(){
		final DefaultUWSBackupManager backupManager = new DefaultUWSBackupManager(null);

		// Encoding an empty bytes array => Empty string!
		assertEquals(0, backupManager.toBase64(new byte[0]).length());

		// Decoding an empty string => Empty bytes array!
		assertEquals(0, backupManager.fromBase64("").length);

		// Test encoding a simple string:
		final String strToEncode = "Blabla to encore!!!";
		final String expectedEncoding = "QmxhYmxhIHRvIGVuY29yZSEhIQ==";
		final String encodedStr = backupManager.toBase64(strToEncode.getBytes());
		assertEquals(expectedEncoding, encodedStr);

		// Test decoding this string:
		assertEquals(strToEncode, new String(backupManager.fromBase64(encodedStr)));

	}

	@Test
	public void testBackupAndRestoreJobInfo(){
		final DefaultUWSBackupManager backupManager = new DefaultUWSBackupManager(null);

		// Test encoding a simple JobInfo:
		final SingleValueJobInfo jobInfo = new SingleValueJobInfo("ASuperName", "A super blabla to encode...");
		final String expectedJSON = "rO0ABXNyACJ1d3Muam9iLmpvYkluZm8uU2luZ2xlVmFsdWVKb2JJbmZvAAAAAAAAAAECAANMAARuYW1ldAASTGphdmEvbGFuZy9TdHJpbmc7TAAFdmFsdWVxAH4AAUwAEXhtbFJlcHJlc2VudGF0aW9ucQB+AAF4cHQACkFTdXBlck5hbWV0ABtBIHN1cGVyIGJsYWJsYSB0byBlbmNvZGUuLi50ADQ8QVN1cGVyTmFtZT5BIHN1cGVyIGJsYWJsYSB0byBlbmNvZGUuLi48L0FTdXBlck5hbWU+";
		String encodedJobInfo = null;
		try{
			encodedJobInfo = (String)backupManager.getJSONJobInfo(jobInfo);
			assertEquals(expectedJSON, encodedJobInfo);
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected exception while encoding a JobInfo into a base-64 string! (see console for more details)");
		}

		// Test decoding this JobInfo:
		JobInfo decodedJobInfo;
		try{
			decodedJobInfo = backupManager.restoreJobInfo(encodedJobInfo);
			assertNotNull(decodedJobInfo);
			assertEquals(SingleValueJobInfo.class, decodedJobInfo.getClass());
			assertEquals(jobInfo.getName(), ((SingleValueJobInfo)decodedJobInfo).getName());
			assertEquals(jobInfo.getValue(), ((SingleValueJobInfo)decodedJobInfo).getValue());
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Unexpected exception while decoding a JobInfo from a base-64 string! (see console for more details)");
		}
	}

}
