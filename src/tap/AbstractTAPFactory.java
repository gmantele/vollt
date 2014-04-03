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

import java.util.ArrayList;
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
import adql.db.DBChecker;
import adql.db.DBTable;

import adql.parser.ADQLQueryFactory;
import adql.parser.QueryChecker;

public abstract class AbstractTAPFactory<R> extends AbstractUWSFactory implements TAPFactory<R> {

	protected final ServiceConnection<R> service;

	protected AbstractTAPFactory(ServiceConnection<R> service) throws NullPointerException {
		if (service == null)
			throw new NullPointerException("Can not create a TAPFactory without a ServiceConnection instance !");

		this.service = service;
	}

	@Override
	public UWSService createUWS() throws TAPException, UWSException {
		return new UWSService(this.service.getFactory(), this.service.getFileManager(), this.service.getLogger());
	}

	@Override
	public UWSBackupManager createUWSBackupManager(final UWSService uws) throws TAPException, UWSException {
		return null;
	}

	@Override
	public UWSJob createJob(HttpServletRequest request, JobOwner owner) throws UWSException {
		if (!service.isAvailable())
			throw new UWSException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, service.getAvailability());

		try{
			TAPParameters tapParams = (TAPParameters)createUWSParameters(request);
			return new TAPJob(owner, tapParams);
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Can not create a TAP asynchronous job !");
		}
	}

	@Override
	public UWSJob createJob(String jobId, JobOwner owner, final UWSParameters params, long quote, long startTime, long endTime, List<Result> results, ErrorSummary error) throws UWSException {
		if (!service.isAvailable())
			throw new UWSException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, service.getAvailability());
		try{
			return new TAPJob(jobId, owner, (TAPParameters)params, quote, startTime, endTime, results, error);
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Can not create a TAP asynchronous job !");
		}
	}

	@Override
	public final JobThread createJobThread(final UWSJob job) throws UWSException {
		try{
			return new AsyncThread<R>((TAPJob)job, createADQLExecutor());
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Impossible to create an AsyncThread !");
		}
	}

	public ADQLExecutor<R> createADQLExecutor() throws TAPException {
		return new ADQLExecutor<R>(service);
	}

	/**
	 * Extracts the parameters from the given request (multipart or not).
	 * This function is used only to set UWS parameters, not to create a TAP query (for that, see {@link TAPParameters}).
	 * 
	 * @see uws.service.AbstractUWSFactory#extractParameters(javax.servlet.http.HttpServletRequest, uws.service.UWS)
	 */
	@Override
	public UWSParameters createUWSParameters(HttpServletRequest request) throws UWSException {
		try{
			return new TAPParameters(request, service, getExpectedAdditionalParameters(), getInputParamControllers());
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te);
		}
	}

	@Override
	public UWSParameters createUWSParameters(Map<String, Object> params) throws UWSException {
		try {
			return new TAPParameters(service, params, getExpectedAdditionalParameters(), getInputParamControllers());
		} catch (TAPException te) {
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te);
		}
	}

	@Override
	public ADQLQueryFactory createQueryFactory() throws TAPException{
		return new ADQLQueryFactory();
	}

	@Override
	public QueryChecker createQueryChecker(TAPSchema uploadSchema) throws TAPException {
		TAPMetadata meta = service.getTAPMetadata();
		ArrayList<DBTable> tables = new ArrayList<DBTable>(meta.getNbTables());
		Iterator<TAPTable> it = meta.getTables();
		while(it.hasNext())
			tables.add(it.next());
		if (uploadSchema != null){
			for(TAPTable table : uploadSchema)
				tables.add(table);
		}
		return new DBChecker(tables);
	}

	public Uploader createUploader(final DBConnection<R> dbConn) throws TAPException {
		return new Uploader(service, dbConn);
	}

}
