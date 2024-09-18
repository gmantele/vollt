package tap.data;

import adql.db.DBType;
import org.junit.BeforeClass;
import org.junit.Test;
import tap.metadata.TAPColumn;
import tap.metadata.VotType;
import tap.upload.UploadDataSource;
import uws.service.file.LocalUWSFileManager;
import uws.service.file.UWSFileManager;
import uws.service.request.UploadFile;

import static org.junit.Assert.*;

import java.io.File;

public class TestSTILTableIterator {

    private static final String uploadExampleDir = "./test/tap/data";

    private static UWSFileManager fileManager;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        fileManager = new LocalUWSFileManager(new File(uploadExampleDir));
    }

    private static UploadDataSource makeDataSource(final String filePath, final String mimeType, final String paramName){
        final UploadFile file = new UploadFile((paramName == null ? "test" : paramName), filePath, fileManager);
        if (mimeType != null)
            file.setMimeType(mimeType);
        return new UploadDataSource(file);
    }

    private static UploadDataSource makeDataSource(final String filePath, final String mimeType){
        return makeDataSource(filePath, mimeType, null);
    }

    private static UploadDataSource makeDataSource(final String filePath){
        return makeDataSource(filePath, null, null);
    }

    @Test
    public void hasFileExtension(){
        assertTrue(STILTableIterator.hasFileExtension("hello.txt"));
        assertTrue(STILTableIterator.hasFileExtension("blabla/bidule/hello.txt"));

        assertFalse(STILTableIterator.hasFileExtension("blabla.txt/bidule/hello"));
        assertFalse(STILTableIterator.hasFileExtension("hello."));
        assertFalse(STILTableIterator.hasFileExtension("hello_txt"));
    }

    @Test
    public void constructor_ShouldFail_WhenNoFileFound(){
        // Given
        final UploadDataSource dataSource = makeDataSource("foo");

        // When
        try(TableIterator ignored = new STILTableIterator(dataSource)) {
            fail("No file exists. This test should fail without testing all formats.");
        }

        // Then
        catch(Exception ex){
            assertEquals(DataReadException.class, ex.getClass());
            assertEquals("Failed to read the table '"+dataSource.getName()+"'! Cause: no file found.", ex.getMessage());
            assertNotNull(ex.getCause());
            assertEquals("The uploaded file submitted with the parameter \""+dataSource.getName()+"\" can not be found any more on the server!", ex.getCause().getMessage());
        }
    }

    @Test
    public void constructor_ShouldFail_WhenFormatCannotBeDetectedAutomatically(){
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/unsupportedTableFormat.png");

        // When
        try(TableIterator ignored = new STILTableIterator(dataSource)) {
            fail("Images cannot be uploaded as tables. This format is not supported by VOLLT. This test should fail.");
        }

        // Then
        catch(Exception ex){
            assertEquals(DataReadException.class, ex.getClass());
            assertEquals("Failed to read the table 'test'! Cause: failed to automatically detect the table format of the table to upload ("+dataSource.getName()+")!", ex.getMessage().substring(0, 118));
        }
    }

    @Test
    public void constructor_ShouldFail_WhenFileIsJustEmpty(){
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/emptyFile.txt");

        // When
        try(TableIterator ignored = new STILTableIterator(dataSource)) {
            fail("File is empty. Consequently, its format cannot be detected. This test should fail.");
        }

        // Then
        catch(Exception ex){
            assertEquals(DataReadException.class, ex.getClass());
            assertEquals("Failed to read the table 'test'! Cause: failed to automatically detect the table format of the table to upload ("+dataSource.getName()+")!", ex.getMessage().substring(0, 118));
        }
    }

    @Test
    public void constructor_ShouldSucceed_WhenSupportedTableWithNoExtensionAndMimeType() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata_vot_no_extension");

        // When
        try(TableIterator valid = new STILTableIterator(dataSource)) {

            // Then
            assertNotNull(valid);
        }
    }

    @Test
    public void constructor_ShouldFail_WhenUnsupportedTableWithNoExtensionAndMimeType() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata_ascii");

        // When
        try(TableIterator ignored = new STILTableIterator(dataSource)) {
            fail("File is ASCII which is a format that can not be detected automatically. This test should fail.");
        }

        // Then
        catch(Exception ex){
            assertEquals(DataReadException.class, ex.getClass());
            assertEquals("Failed to read the table 'test'! Cause: failed to automatically detect the table format of the table to upload ("+dataSource.getName()+")!", ex.getMessage().substring(0, 118));
        }
    }

    @Test
    public void constructor_ShouldSucceed_WhenTableWithMimeType() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata_ascii", "ascii");

        // When
        try(TableIterator valid = new STILTableIterator(dataSource)) {

            // Then
            assertNotNull(valid);
        }
    }

    @Test
    public void constructor_ShouldSucceed_WhenTableWithGenericMimeType() throws Exception {
        /*
         * Why this test?
         *   This use-case often happen with cURL using the
         *   '-F "table=@myFilePath"' parameter with no type specified (which
         *   happens most of the time).
         */

        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata.vot", "application/octet-stream");

        // When
        try(TableIterator valid = new STILTableIterator(dataSource)) {

            // Then
            assertNotNull(valid);
        }
    }

    @Test
    public void constructor_ShouldSucceed_WhenTableWithFileExtension() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata.fits");

        // When
        try(TableIterator valid = new STILTableIterator(dataSource)) {

            // Then
            assertNotNull(valid);
        }
    }

    @Test
    public void constructor_ShouldSucceed_WhenParamNameWithFileExtension() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata_ascii", null, "test.txt");

        // When
        try(TableIterator valid = new STILTableIterator(dataSource)) {

            // Then
            assertNotNull(valid);
        }
    }

    @Test
    public void constructor_ShouldFail_WhenFileNameExtensionIsIncorrect_AndParamNameWithoutFileExtension() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata_csv.lis", null, "test");

        // When
        try(TableIterator ignored = new STILTableIterator(dataSource)) {
            fail("File is CSV which is a format that can not be detected automatically and there is no other file extension as 'lis' which is incorrect. This test should fail.");
        }

        // Then
        catch(Exception ex){
            assertEquals(DataReadException.class, ex.getClass());
            assertEquals("Failed to read the table 'test'! Cause: failed to automatically detect the table format of the table to upload ("+dataSource.getName()+")!", ex.getMessage().substring(0, 118));
        }

    }

    @Test
    public void constructor_ShouldSucceed_WhenFileNameExtensionIsIncorrect_ButParamNameWithFileExtensionIsCorrect() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata_csv.lis", null, "test.csv");

        // When
        try(TableIterator valid = new STILTableIterator(dataSource)) {

            // Then
            assertNotNull(valid);
        }
    }

    @Test
    public void getMetadata_ShouldSucceed_WhenAskingForThem() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata.vot");

        // When
        try(TableIterator table = new STILTableIterator(dataSource)) {

            // Then
            final TAPColumn[] columns = table.getMetadata();
            assertEquals(4, columns.length);
        }
    }

    @Test
    public void nextRow_ShouldSucceed_WhenAskingForAllRows() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata.vot");

        // When
        try(TableIterator table = new STILTableIterator(dataSource)) {
            int cntLines = 0;
            while(table.nextRow())
                cntLines++;

            // Then
            assertEquals(100, cntLines);
        }
    }

    @Test
    public void nextCol_ShouldSucceed_WhenAskingForAllColumns() throws Exception {
        // Given
        final UploadDataSource dataSource = makeDataSource(uploadExampleDir+"/testdata.vot");

        // When
        try(TableIterator table = new STILTableIterator(dataSource)) {
            table.nextRow();

            int cntColumns = 0;
            while(table.hasNextCol()) {
                cntColumns++;
                table.nextCol();
            }

            // Then
            assertEquals(4, cntColumns);
        }
    }

}
