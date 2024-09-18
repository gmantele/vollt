package tap.data;

/*
 * This file is part of TAPLibrary.
 *
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2024 - Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.db.DBType;
import tap.TAPException;
import tap.metadata.TAPColumn;
import tap.metadata.VotType;
import tap.metadata.VotType.VotDatatype;
import tap.upload.UploadDataSource;
import uk.ac.starlink.ecsv.EcsvTableBuilder;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.parquet.ParquetTableBuilder;
import uk.ac.starlink.table.*;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.table.formats.MrtTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * {@link TableIterator} which lets iterate over a table using STIL.
 *
 * <p>
 * 	{@link #getColType()} will return TAP type based on the type declared in the
 * 	table metadata part.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.4 (09/2024)
 * @since 2.4
 */
public class STILTableIterator implements TableIterator {

	/** Data source containing the table on which this {@link TableIterator} is
	 * iterating over. */
	protected final DataSource dataSource;
	/** The StarTable on which to iterate. */
	protected final StarTable table;
	/** Metadata about all columns of the target table. */
	protected final TAPColumn[] colMetadata;
	/** Rows to iterate to. */
	protected final RowSequence rows;

	/** Indicate whether the row iteration has already started. */
	protected boolean iterationStarted = false;
	/** Indicate whether the last row has already been reached. */
	protected boolean endReached = false;

	/** The last read row. Column iteration is done on this array. */
	protected Object[] row;
	/** Index of the last read column (=0 just after {@link #nextRow()} and
	 * before {@link #nextCol()}, =length of {@link #colMetadata} after the last
	 * column has been read). */
	protected int indCol = -1;

	protected static final TableBuilder[] autoBuilders = new TableBuilder[]{
			new VOTableBuilder(),
			new FitsTableBuilder(),
			new MrtTableBuilder(),
			new EcsvTableBuilder()
			/*
			 * Implementation note:
			 *
			 *   CSV is not present in this list because it has no magic number.
			 *   Thus, any text format is likely to be read as a CSV file, but
			 *   the resulting table will not match the real table (e.g. columns
			 *   may be merged, ...).
			 */
	};

	/**
	 * Build a {@link TableIterator} able to read rows and columns inside the
	 * given table input stream.
	 *
	 * @param dataSource	Input data source toward the table to iterate to.
	 *
	 * @throws NullPointerException	If NULL is given in parameter.
	 * @throws DataReadException	If the given table can not be parsed.
	 */
	public STILTableIterator(final DataSource dataSource) throws DataReadException {
		this.dataSource = Objects.requireNonNull(dataSource, "Missing VOTable document input stream over which to iterate!");
		table           = openTable();
		colMetadata     = extractColMeta();
		rows            = getRowSequence();
	}

	@Override
	public void close() throws DataReadException {
		endReached = true;
	}


	/* ********************************************************************** */
	/* * TABLE CREATION */
	/* ********************************************************************** */

	protected StarTable openTable() throws DataReadException {
		// If a MIME type is provided, open the table with it:
		if (hasMimeType(dataSource))
			return openTableWithMimeType(getMimeType(dataSource));
		else
		{
			/* Otherwise, try all file extensions (first, from the filename,
			 * then from the parameter name): */
			final String[] fileExtensions = new String[]{
					getFileExtensionFromFileName(dataSource).orElse(null),
					getFileExtensionFromNameParam(dataSource).orElse(null)
			};

			// Try with each file extension ; stop when one works:
			for(String fileExt : fileExtensions) {
				if (fileExt != null) {
					final Optional<StarTable> optTable = tryOpenTableWithFileExtension(fileExt);
					if (optTable.isPresent())
						return optTable.get();
				}
			}

			// If no file extension is useful, try to guess:
			return tryToOpenTableWithMultipleFormats();
		}
	}

	private Optional<StarTable> tryOpenTableWithFileExtension(final String fileExtension) {
		try{
			return Optional.of(openTableWithFileExtension(fileExtension));
		}
		catch(Exception ignored){
			return Optional.empty();
		}
	}

	private boolean hasMimeType(final DataSource dataSource){
		return (dataSource instanceof UploadDataSource)
				&& ((UploadDataSource) dataSource).getMimeType().isPresent()
				&& !"application/octet-stream".equalsIgnoreCase(((UploadDataSource) dataSource).getMimeType().orElse(""));
		/*
		 * Implementation note:
		 *   By default, cURL submits file in Multipart using the MIME-Type
		 *   'application/octet-stream', which of course, never match precisely
		 *   the format of the given file. With this last test we ignore this
		 *   very generic MIME-Type.
		 */
	}

	private String getMimeType(final DataSource dataSource){
		return ((UploadDataSource) dataSource).getMimeType().orElse(null);
	}

	private Optional<String> getFileExtensionFromFileName(final DataSource dataSource){
		if (hasFileNameWithExtension(dataSource))
		{
			final String fileName = getFileName((UploadDataSource) dataSource);
			return Optional.of(getFileExtension(fileName));
		}
		else
			return Optional.empty();
	}

	private Optional<String> getFileExtensionFromNameParam(final DataSource dataSource){
		if (hasFileExtension(dataSource.getName()))
			return Optional.of(getFileExtension(dataSource.getName()));
		else
			return Optional.empty();
	}

	private boolean hasFileNameWithExtension(final DataSource dataSource){
		return (dataSource instanceof UploadDataSource)
				&& getFileName((UploadDataSource)dataSource) != null
				&& hasFileExtension(getFileName((UploadDataSource)dataSource));
	}

	private String getFileName(final UploadDataSource dataSource) {
		return dataSource.getFileName();
	}

	/* IMPLEMENTATION NOTE:
	 *   Protected and Static function only to be able to JUnit test it. */
	protected static boolean hasFileExtension(final String fileName){
		return fileName.matches(".*[.][A-Za-z0-9]+");
	}

	private String getFileExtension(final String fileName){
		return fileName.substring(fileName.lastIndexOf('.')+1);
	}

	protected StarTable openTableWithMimeType(final String mimeType) throws DataReadException {
		try {
			final Optional<TableBuilder> builder = getTableBuilderForMimeType(mimeType);
			if (builder.isPresent())
				return openTableUsing(builder.get());
			else
				throw failedReadException("unsupported MIME type ("+mimeType+")");
		}
		catch(FileNotFoundException ex){
			throw failedReadException("no file found", ex);
		}
		catch(IOException ioe){
			throw failedReadException("the specified table format ("+mimeType+") might not match the real table format, or the table is not formatted in a supported way", ioe);
		}
	}

	protected Optional<TableBuilder> getTableBuilderForMimeType(final String mimeType){
		final int indexOfSeparator = mimeType.indexOf(';');
		final String mainMimeType  = (indexOfSeparator > 0 ? mimeType.substring(0, indexOfSeparator) : mimeType).trim().toLowerCase();

		final String[] rawOptions = (indexOfSeparator > 0 ? mimeType.substring(indexOfSeparator).split(" *; *") : new String[0]);
		final Set<String> options = new HashSet<>();
		for(String opt : rawOptions){
			options.add(opt.replaceAll(" +", "").toLowerCase());
		}

		switch (mainMimeType) {
			// VOTable:
			case "votable":
			case "application/x-votable+xml":
			case "votable/b2":
			case "application/x-votable+xml;serialization=binary2":
			case "votable/td":
			case "application/x-votable+xml;serialization=tabledata":
			case "votable/fits":
			case "application/x-votable+xml;serialization=fits":
			case "application/xml":
			case "text/xml":
				return Optional.of(new VOTableBuilder());
			// FITS:
			case "fits":
			case "application/fits":
			case "image/fits": /* Note: more permissive on purpose. Web-browsers generally interpret a .fits file as an `image/fits` by default with an `<input type="file">`, even if it is a tabular FITS. */
				return Optional.of(new FitsTableBuilder());
			// Parquet:
			case "parquet":
				if (supportsParquet())
					return Optional.of(new ParquetTableBuilder());
				else
					return Optional.empty();
			// CSV:
			case "csv":
			case "text/csv":
			case "application/csv":
				final CsvTableBuilder builder = new CsvTableBuilder();
				builder.setHasHeader(options.contains("header=present"));
				return Optional.of(builder);
			// ECSV:
			case "ecsv":
				return Optional.of(new EcsvTableBuilder());
			// MRT (Machine Readable Table):
			case "mrt":
				return Optional.of(new MrtTableBuilder());
			// ASCII:
			case "ascii":
			case "text":
			case "text/plain":
				return Optional.of(new AsciiTableBuilder());
			default:
				return Optional.empty();
		}
	}

	protected StarTable openTableWithFileExtension(final String fileExtension) throws DataReadException {
		try {
			final Optional<TableBuilder> builder = getTableBuilderForFileExtension(fileExtension);
			if (builder.isPresent())
				return openTableUsing(builder.get());
			else
				throw failedReadException("unsupported file extension ("+fileExtension+")");
		}
		catch(FileNotFoundException ex){
			throw failedReadException("no file found.", ex);
		}
		catch(IOException ioe){
			throw failedReadException("The specified file extension ("+fileExtension+") might not match the real table format, or the table is not formatted in a supported way", ioe);
		}
	}

	protected Optional<TableBuilder> getTableBuilderForFileExtension(final String fileExtension){
		switch(fileExtension.trim().toLowerCase()){
			// VOTable:
			case "xml":
			case "vot":
			case "votable":
				return Optional.of(new VOTableBuilder());
			// FITS:
			case "fits":
				return Optional.of(new FitsTableBuilder());
			// Parquet:
			case "parquet":
				if (supportsParquet())
					return Optional.of(new ParquetTableBuilder());
				else
					return Optional.empty();
			// CSV:
			case "csv":
				return Optional.of(new CsvTableBuilder());
			// ECSV:
			case "ecsv":
				return Optional.of(new EcsvTableBuilder());
			// MRT (Machine Readable Table):
			case "mrt":
				return Optional.of(new MrtTableBuilder());
			// ASCII:
			case "ascii":
			case "txt":
				return Optional.of(new AsciiTableBuilder());
			default:
				return Optional.empty();
		}
	}

	private static boolean supportsParquet(){
		try {
			Class.forName("org.apache.parquet.io.InputFile");
			return true;
		}catch(ClassNotFoundException ex){
			return false;
		}
	}

	protected StarTable tryToOpenTableWithMultipleFormats() throws DataReadException {
		int indFormat = 0;
		final String[] testedFormats = new String[autoBuilders.length];

		for(TableBuilder builder : autoBuilders) {
			try {
				return openTableUsing(builder);
			} catch (FileNotFoundException ex) {
				throw failedReadException("no file found", ex);
			} catch (Exception ignored) {
				testedFormats[indFormat++] = builder.getFormatName();
				/* Try with the next format. */
			}
		}

		// If no format is suitable, reject the table:
		throw failedReadException("failed to automatically detect the table format of the table to upload ("+dataSource.getName()+")! The following formats have been tested: "+String.join(", ", testedFormats)+". HINT: try to specify the table format using the HTTP header 'Content-Type' with the appropriate MIME type.");
	}

	/**
	 * Simply make/open a StarTable with the dataSource using the given
	 * {@link TableBuilder} (dedicated to a particular table format).
	 *
	 * @param builder	The table builder to use.
	 *
	 * @return	The opened table.
	 *
	 * @throws IOException	     If the given table builder failed to open the
	 *                           data source. An interesting sub-exception to
	 *                           look at is: {@link TableFormatException}.
	 */
	private StarTable openTableUsing(final TableBuilder builder) throws IOException {
		return builder.makeStarTable(dataSource, false, StoragePolicy.PREFER_DISK);
	}

	@Override
	public TAPColumn[] getMetadata() throws DataReadException {
		return colMetadata;
	}


	/* ********************************************************************** */
	/* * ITERATION MANAGEMENT */
	/* ********************************************************************** */

	protected final RowSequence getRowSequence() throws DataReadException {
		try{
			return table.getRowSequence();
		}catch(IOException ioe){
			throw new DataReadException("Unable to parse/read the given table ("+dataSource.getName()+")!", ioe);
		}
	}

	@Override
	public boolean nextRow() throws DataReadException {
		if (endReached)
			return false;

		try {
			if (rows.next()) {
				row = rows.getRow();
				endReached = false;
				iterationStarted = true;
				indCol = 0;
			} else
				endReached = true;
		}catch(IOException ioe){
			throw failedReadException("unexpected error while reading the next line of the table to upload ("+dataSource.getName()+")!", ioe);
		}

		return !endReached;
	}

	@Override
	public boolean hasNextCol() throws IllegalStateException, DataReadException {
		// Check the read state:
		checkReadState();

		// Determine whether the last column has been reached or not:
		return (indCol < colMetadata.length);
	}

	@Override
	public Object nextCol() throws NoSuchElementException, IllegalStateException, DataReadException {
		// Check the read state and ensure there is still at least one column to read:
		if (!hasNextCol())
			throw new NoSuchElementException("No more field to read!");

		// Get the column value:
		return row[indCol++];
	}

	@Override
	public DBType getColType() throws IllegalStateException, DataReadException {
		// Basically check the read state (for rows iteration):
		checkReadState();

		// Check deeper the read state (for columns iteration):
		if (indCol <= 0)
			throw new IllegalStateException("No field has yet been read!");
		else if (indCol > colMetadata.length)
			throw new IllegalStateException("All fields have already been read!");

		// Return the column type:
		return colMetadata[indCol - 1].getDatatype();
	}

	/**
	 * <p>Check the row iteration state. That's to say whether:</p>
	 * <ul>
	 * 	<li>the row iteration has started = the first row has been read = a first call of {@link #nextRow()} has been done</li>
	 * 	<li>AND the row iteration is not finished = the last row has been read.</li>
	 * </ul>
	 * @throws IllegalStateException If this iterator is in an unexpected state.
	 */
	protected void checkReadState() throws IllegalStateException {
		if (!iterationStarted)
			throw new IllegalStateException("No row has yet been read!");
		else if (endReached)
			throw new IllegalStateException("End of VOTable file already reached!");
	}


	/* ********************************************************************** */
	/* * DATA TYPE MANAGEMENT */
	/* ********************************************************************** */

	protected TAPColumn[] extractColMeta() throws DataReadException {
		final int countColumns    = table.getColumnCount();
		final TAPColumn[] columns = new TAPColumn[countColumns];

		for (int i = 0; i < columns.length; i++)
		{
			final ColumnInfo colInfo = table.getColumnInfo(i);

			try {
				final DBType type = resolveVotType(colInfo).toTAPType();
				columns[i] = createTAPColumn(colInfo, type);
			}
			catch(DataReadException dre){
				throw dre;
			}
			catch (TAPException te) {
				throw new DataReadException(te.getMessage(), te);
			}
		}

		return columns;
	}

	/**
	 * Resolve a VOTable field type by using the column information extracted
	 * by the STIL library. This information may come from different table
	 * formats, not only VOTable.
	 *
	 * <p><b><u>IMPORTANT:</u>
	 *     This code, {@link #resolveSTILType(ColumnInfo)} and
	 *     {@link #serializeStringArrayArraysize(ColumnInfo)} are an adaptation
	 *     of the function
	 *     <a href="https://github.com/Starlink/starjava/blob/04497f1518c1cc35ca18254f8d16d700bf99ed9a/votable/src/main/uk/ac/starlink/votable/Encoder.java#L258">
	 *         uk.ac.starlink.votable.Encoder#getEncoder(ValueInfo, boolean, boolean)
	 *     </a> from the STIL library of Mark Taylor. This class has a package
	 *     only visibility. Hence this rewriting in VOLLT.
	 * </b></p>
	 *
	 * @param colInfo	Column metadata (especially the datatype info).
	 *
	 * @return	The corresponding VOTable field type.
	 *
	 * @throws DataReadException	If a field datatype is unknown or missing.
	 */
	public static VotType resolveVotType(final ColumnInfo colInfo) throws DataReadException {
		final String xtype = getXtype(colInfo);

		final Object[] stilType = resolveSTILType(colInfo);

		return new VotType((VotDatatype)stilType[0], (String)stilType[1], xtype);
	}

	/**
	 * @see #resolveVotType(ColumnInfo)
	 */
	private static Object[] resolveSTILType(final ColumnInfo colInfo) throws DataReadException {
		VotDatatype datatype = null;
		String arraysize     = getArraysize(colInfo);

		final Class<?> clazz = colInfo.getContentClass();

		if (clazz == Boolean.class || clazz == boolean[].class)
			datatype = VotDatatype.BOOLEAN;

		else if ((clazz == short[].class || clazz == Short.class)
		         && Boolean.TRUE.equals(colInfo.getAuxDatumValue(Tables.UBYTE_FLAG_INFO, Boolean.class)))
		{
			datatype = VotDatatype.UNSIGNEDBYTE;
		}

		else if (clazz == Byte.class || clazz == byte[].class
			  || clazz == Short.class || clazz == short[].class)
		{
			datatype = VotDatatype.SHORT;
		}

		else if (clazz == Integer.class || clazz == int[].class)
			datatype = VotDatatype.INT;

		else if (clazz == Long.class || clazz == long[].class)
			datatype = VotDatatype.LONG;

		else if (clazz == Float.class || clazz == float[].class)
			datatype = VotDatatype.FLOAT;

		else if (clazz == Double.class || clazz == double[].class)
			datatype = VotDatatype.DOUBLE;

		else if (clazz == Character.class) {
			datatype = getCharDatatype(colInfo);
			/* For a single character take care to add the attribute
			 * arraysize="1" - although this is implicit according to
			 * the standard, it's often left off and assumed to be
			 * equivalent to arraysize="*".  This makes sure there is
			 * no ambiguity. */
			arraysize = "1";
		}

		else if (clazz == String.class) {
			final int nChar = colInfo.getElementSize();
			datatype  = getCharDatatype(colInfo);
			arraysize = (nChar > 0) ? Integer.toString(nChar) : "*";
		}

		else if (clazz == String[].class) {
			final Optional<String> resolvedArraysize = serializeStringArrayArraysize(colInfo);
			if (resolvedArraysize.isPresent()) {
				datatype  = getCharDatatype(colInfo);
				arraysize = resolvedArraysize.get();
			}
		}

		if (datatype == null)
			/* Not a type we can do anything with. */
			throw new DataReadException("unknown field datatype: " + colInfo);

		return new Object[]{datatype, arraysize};
	}

	/**
	 * @see #resolveVotType(ColumnInfo)
	 */
	private static Optional<String> serializeStringArrayArraysize(final ColumnInfo colInfo) {
		final int[] dims = colInfo.getShape();
		final int elementSize = colInfo.getElementSize();

		if (elementSize < 0) {
			System.out.println("WARNING: Oh dear - can't serialize array of variable-length " + "strings to VOTable - sorry");
			return Optional.empty();
		}
		else if (dims != null) {
			/* Add an extra dimension since writing treats a string as an
			 * array of chars. */
			int[] charDims = new int[dims.length + 1];
			charDims[0] = elementSize;
			System.arraycopy(dims, 0, charDims, 1, dims.length);

			/* Work out the arraysize attribute. */
			final StringBuilder sbuf = new StringBuilder();
			for (int i = 0; i < charDims.length; i++) {
				if (i > 0) {
					sbuf.append('x');
				}
				if (i == charDims.length - 1 && charDims[i] < 0) {
					sbuf.append('*');
				} else {
					sbuf.append(charDims[i]);
				}
			}
			return Optional.of(sbuf.toString());
		}
		else
			return Optional.empty();
	}

	/**
	 * Resolve a VOTable field type by using the datatype, arraysize and xtype
	 * strings as specified in a VOTable document.
	 *
	 * @param datatype		Attribute value of VOTable corresponding to the
	 *                      datatype.
	 * @param arraysize		Attribute value of VOTable corresponding to the
	 *                      arraysize.
	 * @param xtype			Attribute value of VOTable corresponding to the
	 *                      xtype.
	 *
	 * @return	The resolved VOTable field type.
	 *
	 * @throws DataReadException	If a field datatype is unknown or missing.
	 */
	public static VotType resolveVotType(final String datatype, final String arraysize, final String xtype) throws DataReadException {
		try{
			if (datatype == null || datatype.trim().isEmpty())
				throw new DataReadException("missing datatype to resolve!");

			final VotDatatype votdatatype = VotDatatype.valueOf(datatype.toUpperCase());

			return new VotType(votdatatype, arraysize, xtype);
		}
		catch(IllegalArgumentException iae){
			throw new DataReadException("unknown field datatype: \"" + datatype + "\"");
		}
	}

	protected static String getDatatype(final ColumnInfo colInfo) throws DataReadException {
		final Optional<String> dataType = getAuxDatumValue(colInfo, "Datatype");
		if (dataType.isPresent())
			return dataType.get().toUpperCase();
		else
			throw new DataReadException("missing VOTable required field: \"datatype\"!");
	}

	protected static VotDatatype getCharDatatype(final ColumnInfo colInfo){
		if ("unicodeChar".equals(colInfo.getAuxDatumValue(VOStarTable.DATATYPE_INFO, String.class)))
			return VotDatatype.UNICODECHAR;
		else
			return VotDatatype.CHAR;
	}

	protected static String getArraysize(final ColumnInfo colInfo) {
		int[] dims = colInfo.getShape();

		final boolean isVariable = (dims != null) && dims.length > 0 && dims[ dims.length - 1 ] < 0;

		if (isVariable)
			return DefaultValueInfo.formatShape(dims).replace(",", "x");
		else
			return null;
	}

	protected static String getXtype(final ColumnInfo colInfo) {
		return getAuxDatumValue(colInfo, "xtype").orElse(null);
	}

	protected static Optional<String> getAuxDatumValue(final ColumnInfo colInfo, final String auxDatumName) {
		final DescribedValue value = colInfo.getAuxDatumByName(auxDatumName);
		if (value == null || value.toString().trim().isEmpty())
			return Optional.empty();
		else
			return Optional.of(value.getValue().toString());
	}

	protected TAPColumn createTAPColumn(final ColumnInfo colInfo, final DBType type) {
		final TAPColumn col = new TAPColumn(colInfo.getName(), type, colInfo.getDescription(), colInfo.getUnitString(), colInfo.getUCD(), colInfo.getUtype());

		col.setPrincipal(false);
		col.setIndexed(false);
		col.setStd(false);

		return col;
	}


	/* ********************************************************************** */
	/* * ERROR MANAGEMENT */
	/* ********************************************************************** */

	protected DataReadException failedReadException(final String cause) {
		return failedReadException(cause, null);
	}

	protected DataReadException failedReadException(final String cause, final Exception caughtException) {
		return new DataReadException("Failed to read the table '"+dataSource.getName()+"'! Cause: "+cause+".", caughtException);
	}

}
