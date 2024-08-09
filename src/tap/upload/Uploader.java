package tap.upload;

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
 * Copyright 2012-2024 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.data.DataReadException;
import tap.data.LimitedTableIterator;
import tap.data.STILTableIterator;
import tap.data.TableIterator;
import tap.db.DBConnection;
import tap.db.DBException;
import tap.metadata.TAPColumn;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPMetadata.STDSchema;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.parameters.DALIUpload;
import uk.ac.starlink.util.DataSource;
import uws.UWSException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Let create properly given VOTable inputs in the "database".
 *
 * <p>
 * 	This class manages particularly the upload limit in rows and in bytes by
 * 	creating a {@link LimitedTableIterator} with a {@link STILTableIterator}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.4 (08/2024)
 *
 * @see LimitedTableIterator
 * @see STILTableIterator
 */
public class Uploader {
	/** Specification of the TAP service. */
	protected final ServiceConnection service;
	/** Connection to the "database" (which lets upload the content of any given
	 * VOTable). */
	protected final DBConnection dbConn;
	/** Description of the TAP_UPLOAD schema to use.
	 * @since 2.0 */
	protected final TAPSchema uploadSchema;
	/** Type of limit to set: ROWS or BYTES. <i>MAY be NULL ; if NULL, no limit
	 * will be set.</i> */
	protected final LimitUnit limitUnit;
	/** Limit on the number of rows or bytes (depending on {@link #limitUnit})
	 * allowed to be uploaded in once (whatever is the number of tables). */
	protected final int limit;

	/**
	 * Build an {@link Uploader} object.
	 *
	 * @param service	Specification of the TAP service using this uploader.
	 * @param dbConn	A valid (open) connection to the "database".
	 *
	 * @throws TAPException	If any error occurs while building this
	 *                     	{@link Uploader}.
	 */
	public Uploader(final ServiceConnection service, final DBConnection dbConn) throws TAPException{
		this(service, dbConn, null);
	}

	/**
	 * Build an {@link Uploader} object.
	 *
	 * @param service	Specification of the TAP service using this uploader.
	 * @param dbConn	A valid (open) connection to the "database".
	 *
	 * @throws TAPException	If any error occurs while building this
	 *                     	{@link Uploader}.
	 *
	 * @since 2.0
	 */
	public Uploader(final ServiceConnection service, final DBConnection dbConn, final TAPSchema uplSchema) throws TAPException{
		// Set the service and database connections:
		this.service = Objects.requireNonNull(service, "The given ServiceConnection is NULL !");
		this.dbConn  = Objects.requireNonNull(dbConn, "The given DBConnection is NULL !");

		// No use for this class if no upload enabled:
		if (!this.service.uploadEnabled())
			throw new TAPException("Upload aborted: this functionality is disabled in this TAP service!");

		// Set the given upload schema:
		if (uplSchema != null){
			checkUploadSchemaName(uplSchema);
			this.uploadSchema = uplSchema;
		}
		// ...or the default one:
		else
			this.uploadSchema = buildDefaultTAPSchema();

		// Get the upload limit:
		final Object[] extractedLimit = extractLimitFromService();
		this.limit     = (int)extractedLimit[0];
		this.limitUnit = (LimitUnit)extractedLimit[1];
	}

	protected void checkUploadSchemaName(final TAPSchema uplSchema) throws TAPException {
		if (!uplSchema.getADQLName().equalsIgnoreCase(TAPMetadata.STDSchema.UPLOADSCHEMA.label))
			throw new TAPException("Incorrect upload schema! Its ADQL name MUST be \"" + TAPMetadata.STDSchema.UPLOADSCHEMA.label + "\" ; here is is \"" + uplSchema.getADQLName() + "\".", UWSException.INTERNAL_SERVER_ERROR);
	}

	protected TAPSchema buildDefaultTAPSchema() {
		return new TAPSchema(TAPMetadata.STDSchema.UPLOADSCHEMA.label, "Schema for tables uploaded by users.");
	}

	/**
	 * Extract the limit from the set service.
	 *
	 * @return An array with two elements:
	 *         [0] = integer value of the limit(-1, if none),
	 *         [1] = the corresponding unit (LimitUnit instance).
	 */
	protected Object[] extractLimitFromService() {
		if (this.service.getUploadLimitType()[1] != null && this.service.getUploadLimit()[1] >= 0)
		{
			final int limitValue           = (int)(this.service.getUploadLimitType()[1].bytesFactor() * this.service.getUploadLimit()[1]);
			final LimitUnit limitValueUnit = (this.service.getUploadLimitType()[1] == LimitUnit.rows) ? LimitUnit.rows : LimitUnit.bytes;
			return new Object[]{limitValue, limitValueUnit};
		}
		else
			return new Object[]{-1, null};
	}

	/**
	 * Upload all the given VOTable inputs.
	 *
	 * <p><b>Note 1:</b>
	 * 	The {@link TAPTable} objects representing the uploaded tables will be
	 *  associated with the TAP_UPLOAD schema specified at the creation of this
	 *  {@link Uploader}. If no such schema was specified, a default one (whose
	 *  DB name will be equals to the ADQL name, that's to say
	 *  {@link STDSchema#UPLOADSCHEMA}) is created, will be associated with the
	 *  uploaded tables and will be returned by this function.
	 * </p>
	 *
	 * <p><b>Note 2:</b>
	 * 	In case of error while ingesting one or all of the uploaded tables,
	 * 	all tables created in the database before the error occurs are dropped
	 *  <i>(see {@link #dropUploadedTables()})</i>.
	 * </p>
	 *
	 * @param uploads	Array of tables to upload.
	 *
	 * @return	A {@link TAPSchema} containing the list and the description of
	 *        	all uploaded tables.
	 *
	 * @throws TAPException	If any error occurs while reading the VOTable inputs
	 *                     	or while uploading the table into the "database".
	 *
	 * @see DBConnection#addUploadedTable(TAPTable, tap.data.TableIterator)
	 */
	public TAPSchema upload(final DALIUpload[] uploads) throws TAPException{
		final HashSet<String> uploadedTables = new HashSet<>(uploads.length);
		String tableName = null;

		try{
			for(DALIUpload upl : uploads)
			{
				tableName = upl.label;

				checkForTableNameUniqueness(tableName, uploadedTables);

				final boolean isLimitInBytes = LimitUnit.bytes.isCompatibleWith(limitUnit) && limit > 0;
				final DataSource tableSource = isLimitInBytes ? new UploadDataSource(upl.file, limit * limitUnit.bytesFactor()) : new UploadDataSource(upl.file);

				try(TableIterator dataIt = new LimitedTableIterator(new STILTableIterator(tableSource), (limitUnit == LimitUnit.rows ? limit : -1)))
				{
					final TAPColumn[] columns = dataIt.getMetadata();
					final TAPTable table = buildTAPTable(uploadSchema, tableName, columns);

					dbConn.addUploadedTable(table, dataIt);
				}
			}
		}
		catch(DataReadException dre){
			reportFailedUpload("Error while reading the VOTable \"" + tableName + "\": " + dre.getMessage(), dre, UWSException.BAD_REQUEST);
		}
		catch(TAPException te){
			reportFailedUpload(te);
		}

		return uploadSchema;
	}

	protected void reportFailedUpload(final Exception ex) throws TAPException {
		reportFailedUpload(null, ex, -1);
	}

	protected void reportFailedUpload(final String message, final Exception ex, final int httpErrorStatus) throws TAPException {
		dropUploadedTables();

		if (ex instanceof DataReadException && ex.getCause() instanceof ExceededSizeException)
			throw (DataReadException) ex;

		else if (httpErrorStatus > 0) {
			if (message == null || message.trim().isEmpty())
				throw new TAPException(ex, httpErrorStatus);
			else
				throw new TAPException(message, ex, httpErrorStatus);
		}

		else if (message != null && !message.trim().isEmpty())
			throw new TAPException(message, ex);

		else if (ex instanceof TAPException)
			throw (TAPException) ex;

		else
			throw new TAPException(ex);
	}

	protected void checkForTableNameUniqueness(final String tableName, final Set<String> uploadedTables) throws TAPException {
		final boolean uniqueTableName = uploadedTables.add(tableName.toLowerCase());

		if (!uniqueTableName)
			throw new TAPException("Non unique table name (case insensitive) among all tables to upload: \"" + tableName + "\"!", UWSException.BAD_REQUEST);
	}

	protected TAPTable buildTAPTable(final TAPSchema schema, final String tableName, final TAPColumn[] columns) throws TAPException {
		checkForColumnNameUniqueness(columns, tableName);

		final TAPTable table = new TAPTable(tableName);
		table.setDBName(tableName + "_" + System.currentTimeMillis());

		for(TAPColumn col : columns)
			table.addColumn(col);

		schema.addTable(table);

		return table;
	}

	protected void checkForColumnNameUniqueness(final TAPColumn[] columns, final String tableName) throws TAPException {
		HashSet<String> columnNames = new HashSet<>(columns.length);
		for(TAPColumn col : columns){
			boolean uniqueColumnName = columnNames.add(col.getADQLName().toLowerCase());
			if (!uniqueColumnName)
				throw new TAPException("Non unique column name (case insensitive) among all columns of the table \"" + tableName + "\": \"" + col.getADQLName() + "\"!", UWSException.BAD_REQUEST);
		}
	}

	/**
	 * Drop all tables already uploaded in the database.
	 *
	 * @since 2.3
	 */
	protected void dropUploadedTables(){
		if (uploadSchema == null || uploadSchema.getNbTables() == 0)
			return;

		for(TAPTable table : uploadSchema){
			try{
				dbConn.dropUploadedTable(table);
			}catch(DBException e){
				service.getLogger().error("Unable to drop the uploaded table " + table.getFullName() + "!", e);
			}
		}
	}

}
