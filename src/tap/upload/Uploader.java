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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institute (ARI)
 */

import java.io.IOException;
import java.io.InputStream;

import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPException;
import tap.data.DataReadException;
import tap.data.VOTableIterator;
import tap.db.DBConnection;
import tap.metadata.TAPColumn;
import tap.metadata.TAPDM;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;

import com.oreilly.servlet.multipart.ExceededSizeException;

/**
 * <p>Let upload properly given VOTable inputs.</p>
 * 
 * <p>This class manages particularly the upload limit in rows
 * (thanks to {@link VOTableIterator}) and in bytes (thanks to a {@link LimitedSizeInputStream}).</p>
 * 
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2014)
 */
public class Uploader {

	/** Specification of the TAP service. */
	protected final ServiceConnection service;
	/** Connection to the "database" (which lets upload the content of any given VOTable). */
	protected final DBConnection dbConn;
	/** Limit on the number of rows allowed to be uploaded in once (whatever is the number of tables). */
	protected final int nbRowsLimit;
	/** Limit on the number of bytes allowed to be uploaded in once (whatever is the number of tables). */
	protected final int nbBytesLimit;

	/** Number of rows already loaded. */
	protected int nbRows = 0;

	/**
	 * Build an {@link Uploader} object.
	 * 
	 * @param service	Specification of the TAP service using this uploader.
	 * @param dbConn	A valid (open) connection to the "database".
	 * 
	 * @throws TAPException	If any error occurs while building this {@link Uploader}.
	 */
	public Uploader(final ServiceConnection service, final DBConnection dbConn) throws TAPException{
		// NULL tests:
		if (service == null)
			throw new NullPointerException("The given ServiceConnection is NULL !");
		if (dbConn == null)
			throw new NullPointerException("The given DBConnection is NULL !");

		// Set the service and database connections:
		this.service = service;
		this.dbConn = dbConn;

		// Ensure UPLOAD is allowed by the TAP service specification...
		if (this.service.uploadEnabled()){
			// ...and set the rows or bytes limit:
			if (this.service.getUploadLimitType()[1] == LimitUnit.rows){
				nbRowsLimit = ((this.service.getUploadLimit()[1] > 0) ? this.service.getUploadLimit()[1] : -1);
				nbBytesLimit = -1;
			}else{
				nbBytesLimit = ((this.service.getUploadLimit()[1] > 0) ? this.service.getUploadLimit()[1] : -1);
				nbRowsLimit = -1;
			}
		}else
			throw new TAPException("Upload aborted: this functionality is disabled in this TAP service!");
	}

	/**
	 * Upload all the given VOTable inputs.
	 * 
	 * @param loaders	Array of tables to upload.
	 * 
	 * @return	A {@link TAPSchema} containing the list and the description of all uploaded tables.
	 * 
	 * @throws TAPException	If any error occurs while reading the VOTable inputs or while uploading the table into the "database".
	 * 
	 * @see DBConnection#addUploadedTable(TAPTable, tap.data.TableIterator)
	 */
	public TAPSchema upload(final TableLoader[] loaders) throws TAPException{
		TAPSchema uploadSchema = new TAPSchema(TAPDM.UPLOADSCHEMA.getLabel());
		InputStream votable = null;
		String tableName = null;
		try{
			for(TableLoader loader : loaders){
				tableName = loader.tableName;

				// Open a stream toward the VOTable:
				votable = loader.openStream();

				// Set a byte limit if one is required:
				if (nbBytesLimit > 0)
					votable = new LimitedSizeInputStream(votable, nbBytesLimit);

				// Start reading the VOTable:
				VOTableIterator dataIt = new VOTableIterator(votable);

				// Define the table to upload:
				TAPColumn[] columns = dataIt.getMetadata();
				TAPTable table = new TAPTable(tableName);
				table.setDBName(tableName + "_" + System.currentTimeMillis());
				for(TAPColumn col : columns)
					table.addColumn(col);

				// Add the table to the TAP_UPLOAD schema:
				uploadSchema.addTable(table);

				// Create and fill the corresponding table in the database:
				dbConn.addUploadedTable(table, dataIt, nbRowsLimit);

				// Close the VOTable stream:
				votable.close();
			}
		}catch(DataReadException dre){
			if (dre.getCause() instanceof ExceededSizeException)
				throw new TAPException("Upload limit exceeded ! You can upload at most " + ((nbBytesLimit > 0) ? (nbBytesLimit + " bytes.") : (nbRowsLimit + " rows.")));
			else
				throw new TAPException("Error while reading the VOTable \"" + tableName + "\": " + dre.getMessage(), dre);
		}catch(IOException ioe){
			throw new TAPException("Error while reading the VOTable of \"" + tableName + "\"!", ioe);
		}catch(NullPointerException npe){
			if (votable != null && votable instanceof LimitedSizeInputStream)
				throw new TAPException("Upload limit exceeded ! You can upload at most " + ((nbBytesLimit > 0) ? (nbBytesLimit + " bytes.") : (nbRowsLimit + " rows.")));
			else
				throw new TAPException(npe);
		}finally{
			try{
				if (votable != null)
					votable.close();
			}catch(IOException ioe){
				;
			}
		}

		// Return the TAP_UPLOAD schema (containing just the uploaded tables):
		return uploadSchema;
	}

}
