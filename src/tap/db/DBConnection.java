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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import cds.savot.model.SavotTR;
import tap.metadata.TAPTable;
import uws.service.log.UWSLogType;

import adql.query.ADQLQuery;

/**
 * TODO
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 * 
 * @param <R>	Result type of the execution of a query (see {@link #executeQuery(String, ADQLQuery)}.
 */
public interface DBConnection< R > {

	public final static UWSLogType LOG_TYPE_DB_ACTIVITY = UWSLogType.createCustomLogType("DBActivity");

	public String getID();

	public void startTransaction() throws DBException;

	public void cancelTransaction() throws DBException;

	public void endTransaction() throws DBException;

	public R executeQuery(final String sqlQuery, final ADQLQuery adqlQuery) throws DBException;

	public void createSchema(final String schemaName) throws DBException;

	public void dropSchema(final String schemaName) throws DBException;

	public void createTable(final TAPTable table) throws DBException;

	public void insertRow(final SavotTR row, final TAPTable table) throws DBException;

	public void dropTable(final TAPTable table) throws DBException;

	public void close() throws DBException;

}
