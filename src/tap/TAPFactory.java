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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.db.DBConnection;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.parameters.TAPParameters;
import tap.upload.Uploader;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.AbstractUWSFactory;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;
import adql.parser.ADQLQueryFactory;
import adql.parser.QueryChecker;
import adql.translator.ADQLTranslator;

public abstract class TAPFactory extends AbstractUWSFactory {

	protected final ServiceConnection service;

	protected TAPFactory(final ServiceConnection service) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("Can not create a TAPFactory without a ServiceConnection instance !");

		this.service = service;
	}

	public abstract ADQLTranslator createADQLTranslator() throws TAPException;

	protected abstract DBConnection createDBConnection() throws TAPException;

	public abstract DBConnection getConnection(final String jobID) throws TAPException;

	public abstract void freeConnection(final DBConnection conn);

	/**
	 * @return	<=0 in case of problem, >0 otherwise.
	 */
	public abstract int countFreeConnections();

	public UWSService createUWS() throws TAPException{
		return new UWSService(this, this.service.getFileManager(), this.service.getLogger());
	}

	public abstract UWSBackupManager createUWSBackupManager(final UWSService uws) throws TAPException;

	@Override
	public final UWSJob createJob(HttpServletRequest request, JobOwner owner) throws UWSException{
		if (!service.isAvailable())
			throw new UWSException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, service.getAvailability());

		return createTAPJob(request, owner);
	}

	protected abstract TAPJob createTAPJob(final HttpServletRequest request, final JobOwner owner) throws UWSException;

	@Override
	public final UWSJob createJob(String jobId, JobOwner owner, final UWSParameters params, long quote, long startTime, long endTime, List<Result> results, ErrorSummary error) throws UWSException{
		if (!service.isAvailable())
			throw new UWSException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, service.getAvailability());

		return createTAPJob(jobId, owner, (TAPParameters)params, quote, startTime, endTime, results, error);
	}

	protected abstract TAPJob createTAPJob(final String jobId, final JobOwner owner, final TAPParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws UWSException;

	@Override
	public final JobThread createJobThread(final UWSJob job) throws UWSException{
		try{
			return new AsyncThread((TAPJob)job, createADQLExecutor());
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Impossible to create an AsyncThread !");
		}
	}

	public abstract ADQLExecutor createADQLExecutor() throws TAPException;

	/**
	 * Extracts the parameters from the given request (multipart or not).
	 * This function is used only to set UWS parameters, not to create a TAP query (for that, see {@link TAPParameters}).
	 * 
	 * @see uws.service.AbstractUWSFactory#extractParameters(javax.servlet.http.HttpServletRequest, uws.service.UWS)
	 */
	@Override
	public final UWSParameters createUWSParameters(HttpServletRequest request) throws UWSException{
		return createTAPParameters(request);
	}

	protected abstract TAPParameters createTAPParameters(final HttpServletRequest request) throws UWSException;

	@Override
	public final UWSParameters createUWSParameters(Map<String,Object> params) throws UWSException{
		return createTAPParameters(params);
	}

	protected abstract TAPParameters createTAPParameters(final Map<String,Object> params) throws UWSException;

	public abstract ADQLQueryFactory createQueryFactory() throws TAPException;

	public final QueryChecker createQueryChecker(final TAPSchema uploadSchema) throws TAPException{
		TAPMetadata meta = service.getTAPMetadata();
		ArrayList<TAPTable> tables = new ArrayList<TAPTable>(meta.getNbTables());
		Iterator<TAPTable> it = meta.getTables();
		while(it.hasNext())
			tables.add(it.next());
		if (uploadSchema != null){
			for(TAPTable table : uploadSchema)
				tables.add(table);
		}
		return createQueryChecker(tables);
	}

	protected abstract QueryChecker createQueryChecker(final Collection<TAPTable> tables) throws TAPException;

	public abstract Uploader createUploader(final DBConnection dbConn) throws TAPException;

}
