package uws.service.request;

import org.junit.Test;
import uws.UWSException;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;

import java.io.File;

import static org.junit.Assert.*;

public class TestUploadFile {

    @Test
    public void constructor_ShouldGenerateFileName_WhenNotProvided() throws UWSException {
        // Pre-requisite:
        final UWSFileManager fileManager = new LocalUWSFileManager(new File("/tmp"));

        // Given:
        final String fileName      = "file.txt";
        final String[] directories = new String[]{"somewhere", "somewhere/else", "file:///somewhere", "https://test.com/somewhere/else"};

        for(String dir : directories)
        {
            // When:
            final UploadFile uplFile = new UploadFile("test", dir+"/"+fileName, fileManager);

            // Then:
            assertEquals(fileName, uplFile.getFileName());
        }
    }

    @Test
    public void constructor_ShouldUseGivenFileName_WhenOneProvided() throws UWSException {
        // Pre-requisite:
        final UWSFileManager fileManager = new LocalUWSFileManager(new File("/tmp"));

        // Given:
        final String fileName = "file.txt";
        final String location = "somewhere/else/foo.text";

        // When:
        final UploadFile uplFile = new UploadFile("test", fileName, location, fileManager);

        // Then:
        assertEquals(fileName, uplFile.getFileName());
    }

}
