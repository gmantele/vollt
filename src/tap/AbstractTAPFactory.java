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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import tap.db.DBConnection;
import tap.metadata.TAPTable;
import tap.parameters.TAPParameters;
import tap.upload.Uploader;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.Result;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;
import adql.db.DBChecker;
import adql.parser.ADQLQueryFactory;
import adql.parser.QueryChecker;

public abstract class AbstractTAPFactory extends TAPFactory {

	protected AbstractTAPFactory(ServiceConnection service) throws NullPointerException{
		super(service);
	}

	@Override
	public UWSBackupManager createUWSBackupManager(final UWSService uws) throws TAPException{
		return null;
	}

	@Override
	protected TAPJob createTAPJob(final HttpServletRequest request, final JobOwner owner) throws UWSException{
		try{
			TAPParameters tapParams = createTAPParameters(request);
			return new TAPJob(owner, tapParams);
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Can not create a TAP asynchronous job !");
		}
	}

	@Override
	protected TAPJob createTAPJob(final String jobId, final JobOwner owner, final TAPParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws UWSException{
		try{
			return new TAPJob(jobId, owner, params, quote, startTime, endTime, results, error);
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Can not create a TAP asynchronous job !");
		}
	}

	@Override
	public ADQLExecutor createADQLExecutor() throws TAPException{
		return new ADQLExecutor(service);
	}

	@Override
	protected TAPParameters createTAPParameters(final HttpServletRequest request) throws UWSException{
		try{
			return new TAPParameters(request, service, getExpectedAdditionalParameters(), getInputParamControllers());
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te);
		}
	}

	@Override
	protected TAPParameters createTAPParameters(final Map<String,Object> params) throws UWSException{
		try{
			return new TAPParameters(service, params, getExpectedAdditionalParameters(), getInputParamControllers());
		}catch(TAPException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te);
		}
	}

	@Override
	public ADQLQueryFactory createQueryFactory() throws TAPException{
		return new ADQLQueryFactory();
	}

	@Override
	protected QueryChecker createQueryChecker(final Collection<TAPTable> tables) throws TAPException{
		return new DBChecker(tables);
	}

	@Override
	public Uploader createUploader(final DBConnection dbConn) throws TAPException{
		return new Uploader(service, dbConn);
	}

}
