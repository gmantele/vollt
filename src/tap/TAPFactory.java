package tap;

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

import tap.db.DBConnection;
import tap.metadata.TAPSchema;
import tap.upload.Uploader;
import uws.UWSException;
import uws.service.UWSFactory;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;
import adql.parser.ADQLQueryFactory;
import adql.parser.QueryChecker;
import adql.translator.ADQLTranslator;

public interface TAPFactory extends UWSFactory {

	public UWSService createUWS() throws TAPException, UWSException;

	public UWSBackupManager createUWSBackupManager(final UWSService uws) throws TAPException, UWSException;

	public ADQLExecutor createADQLExecutor() throws TAPException;

	public ADQLQueryFactory createQueryFactory() throws TAPException;

	public QueryChecker createQueryChecker(TAPSchema uploadSchema) throws TAPException;

	public ADQLTranslator createADQLTranslator() throws TAPException;

	public DBConnection createDBConnection(final String jobID) throws TAPException;

	public Uploader createUploader(final DBConnection dbConn) throws TAPException;

}
