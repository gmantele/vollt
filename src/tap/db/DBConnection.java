package tap.db;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institute (ARI)
 */

import tap.data.DataReadException;
import tap.data.TableIterator;
import tap.metadata.TAPDM;
import tap.metadata.TAPTable;
import uws.service.log.UWSLogType;
import adql.query.ADQLQuery;

/**
 * <p>Connection to the "database" (whatever is the type or whether it is linked to a true DBMS connection).</p>
 * 
 * <p>It lets executing ADQL queries and updating the TAP datamodel (with the list of schemas, tables and columns published in TAP,
 * or with uploaded tables).</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2014)
 */
public interface DBConnection {

	/** Log type specific to the database activity.
	 * @see UWSLogType#createCustomLogType(String) */
	public final static UWSLogType LOG_TYPE_DB_ACTIVITY = UWSLogType.createCustomLogType("DBActivity");

	/**
	 * <p>Get any identifier for this connection.</p>
	 * 
	 * <p><i>note: it is used only for logging purpose.</i></p>
	 * 
	 * @return	ID of this connection.
	 */
	public String getID();

	/**
	 * <p>Let executing the given ADQL query.</p>
	 * 
	 * <p>The result of this query must be formatted as a table, and so must be iterable using a {@link TableIterator}.</p>
	 * 
	 * <p><i>note: the interpretation of the ADQL query is up to the implementation. In most of the case, it is just needed
	 * to translate this ADQL query into an SQL query (understandable by the chosen DBMS).</i></p>
	 * 
	 * @param adqlQuery	ADQL query to execute.
	 * 
	 * @return	The table result.
	 * 
	 * @throws DBException	If any errors occurs while executing the query.
	 */
	public TableIterator executeQuery(final ADQLQuery adqlQuery) throws DBException;

	/**
	 * <p>Add or update the specified TAP_SCHEMA table with the given data.</p>
	 * 
	 * <p><i><b>Warning:</b> It is expected that the given data SHOULD be the only ones inside the specified table.
	 * So, the table SHOULD probably be cleared before the insertion of the given data. However, this behavior MAY depend of the
	 * implementation and more particularly of the way the TAP_SCHEMA is updated.</i></p>
	 * 
	 * @param tapTableName	Name of the TAP_SCHEMA table to add/update.
	 * @param data			Data to use in order to fill the specified table.
	 * 
	 * @return				<i>true</i> if the specified table has been successfully added/updated, <i>false</i> otherwise.
	 * 
	 * @throws DBException			If any error occurs while updating the database.
	 * @throws DataReadException	If any error occurs while reading the given data.
	 */
	public boolean updateTAPTable(final TAPDM tapTableName, final TableIterator data) throws DBException, DataReadException;

	/**
	 * Add the defined and given table inside the TAP_UPLOAD schema.
	 * 
	 * <p><i>note: A table of TAP_UPLOAD MUST be transient and persistent only for the lifetime of the query.
	 * So, this function should always be used with {@link #dropUploadedTable(String)}, which is called at
	 * the end of each query execution.</i></p> 
	 * 
	 * @param tableDef	Definition of the table to upload (list of all columns and of their type).
	 * @param data		Rows and columns of the table to upload.
	 * @param maxNbRows	Maximum number of rows allowed to be inserted. Beyond this limit, a
	 *                  {@link DataReadException} MUST be sent. <i>A negative or a NULL value means "no limit".</i>
	 * 
	 * @return			<i>true</i> if the given table has been successfully added, <i>false</i> otherwise.
	 * 
	 * @throws DBException			If any error occurs while adding the table.
	 * @throws DataReadException	If any error occurs while reading the given data.
	 */
	public boolean addUploadedTable(final TAPTable tableDef, final TableIterator data, final int maxNbRows) throws DBException, DataReadException;

	/**
	 * <p>Drop the specified uploaded table from the database.
	 * More precisely, it means dropping a table from the TAP_UPLOAD schema.</p>
	 * 
	 * @param tableName	Name (in the database) of the uploaded table to drop.
	 * 
	 * @return	<i>true</i> if the specified table has been successfully dropped, <i>false</i> otherwise.
	 * 
	 * @throws DBException	If any error occurs while dropping the specified uploaded table.
	 */
	public boolean dropUploadedTable(final String tableName) throws DBException;

	/**
	 * <p>Close the connection (if needed).</p>
	 * 
	 * <p><i>note: This function is called at the end of a query/job execution, after the result
	 * has been successfully (or not) fetched. When called, it means the connection is no longer needed
	 * for the job and so, can be freed (or given back to a pool, for instance).</i></p>
	 * 
	 * @throws DBException	If any error occurs while closing the connection.
	 */
	public void close() throws DBException;

}
