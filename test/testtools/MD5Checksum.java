package testtools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class MD5Checksum {

	public static byte[] createChecksum(InputStream input) throws Exception{
		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
		int numRead;

		do{
			numRead = input.read(buffer);
			if (numRead > 0){
				complete.update(buffer, 0, numRead);
			}
		}while(numRead != -1);
		return complete.digest();
	}

	// see this How-to for a faster way to convert
	// a byte array to a HEX string
	public static String getMD5Checksum(InputStream input) throws Exception{
		byte[] b = createChecksum(input);
		String result = "";

		for(int i = 0; i < b.length; i++){
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	public static String getMD5Checksum(final String content) throws Exception{
		return getMD5Checksum(new ByteArrayInputStream(content.getBytes()));
	}

	public static void main(String args[]){
		try{
			System.out.println(getMD5Checksum("Blabla et Super blabla"));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
