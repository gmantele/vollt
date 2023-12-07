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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 *
 * TODO:  needs merge (Updated by G.Landais for VizieR)
 *     - createQueryChecker is not final!
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import adql.db.DBChecker;
import adql.parser.ADQLParser;
import adql.parser.ADQLQueryFactory;
import adql.parser.ParseException;
import adql.parser.QueryChecker;
import adql.query.ADQLQuery;
import tap.db.DBConnection;
import tap.error.DefaultTAPErrorWriter;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.parameters.TAPParameters;
import tap.upload.Uploader;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.Result;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;
import uws.service.error.ServiceErrorWriter;

/**
 * Default implementation of most of the {@link TAPFactory} function.
 * Only the functions related with the database connection stay abstract.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.2 (09/2017)
 */
public abstract class AbstractTAPFactory extends TAPFactory {

	/** The error writer to use when any error occurs while executing a resource or to format an error occurring while executing an asynchronous job. */
	protected final ServiceErrorWriter errorWriter;

	/**
	 * Build a basic TAPFactory.
	 * Nothing is done except setting the service connection.
	 *
	 * @param service	Configuration of the TAP service. <i>MUST NOT be NULL</i>
	 *
	 * @throws NullPointerException	If the given {@link ServiceConnection} is NULL.
	 *
	 * @see AbstractTAPFactory#AbstractTAPFactory(ServiceConnection, ServiceErrorWriter)
	 */
	protected AbstractTAPFactory(ServiceConnection service) throws NullPointerException{
		this(service, new DefaultTAPErrorWriter(service));
	}

	/**
	 * <p>Build a basic TAPFactory.
	 * Nothing is done except setting the service connection and the given error writer.</p>
	 *
	 * <p>Then the error writer will be used when creating a UWS service and a job thread.</p>
	 *
	 * @param service		Configuration of the TAP service. <i>MUST NOT be NULL</i>
	 * @param errorWriter	Object to use to format and write the errors for the user.
	 *
	 * @throws NullPointerException	If the given {@link ServiceConnection} is NULL.
	 *
	 * @see TAPFactory#TAPFactory(ServiceConnection)
	 */
	protected AbstractTAPFactory(final ServiceConnection service, final ServiceErrorWriter errorWriter) throws NullPointerException{
		super(service);
		this.errorWriter = errorWriter;
	}

	@Override
	public final ServiceErrorWriter getErrorWriter(){
		return errorWriter;
	}

	/* *************** */
	/* ADQL MANAGEMENT */
	/* *************** */

	/**
	 * <p><i>Note:
	 * 	Unless the standard implementation - {@link ADQLExecutor} - does not fit exactly your needs,
	 * 	it should not be necessary to extend this class and to extend this function (implemented here by default).
	 * </i></p>
	 */
	@Override
	public ADQLExecutor createADQLExecutor() throws TAPException{
		return new ADQLExecutor(service);
	}

	/**
	 * <p><i>Note:
	 * 	This function should be extended if you want to customize the ADQL grammar.
	 * </i></p>
	 */
	@Override
	public ADQLParser createADQLParser() throws TAPException{
		return new ADQLParser();
	}

	/**
	 * <p><i>Note:
	 * 	This function should be extended if you have customized the creation of any
	 * 	{@link ADQLQuery} part ; it could be the addition of one or several user defined function
	 * 	or the modification of any ADQL function or clause specific to your implementation.
	 * </i></p>
	 */
	@Override
	public ADQLQueryFactory createQueryFactory() throws TAPException{
		return new ADQLQueryFactory();
	}

	/**
	 * <p>This implementation gathers all tables published in this TAP service and those uploaded
	 * by the user. Then it calls {@link #createQueryChecker(Collection)} with this list in order
	 * to create a query checked.
	 * </p>
	 *
	 * <p><i>Note:
	 * 	This function can not be overridded, but {@link #createQueryChecker(Collection)} can be.
	 * </i></p>
	 */
	
	//TODO: G.Landais - remove final because it is used by TAPVizieR
	@Override
	public QueryChecker createQueryChecker(final TAPSchema uploadSchema) throws TAPException{
		// Get all tables published in this TAP service:
		TAPMetadata meta = service.getTAPMetadata();

		// Build a list in order to gather all these with the uploaded ones:
		ArrayList<TAPTable> tables = new ArrayList<TAPTable>(meta.getNbTables());

		// Add all tables published in TAP:
		Iterator<TAPTable> it = meta.getTables();
		while(it.hasNext())
			tables.add(it.next());

		// Add all tables uploaded by the user:
		if (uploadSchema != null){
			for(TAPTable table : uploadSchema)
				tables.add(table);
		}

		// Finally, create the query checker:
		return createQueryChecker(tables);
	}

	/**
	 * <p>Create an object able to check the consistency between the ADQL query and the database.
	 * That's to say, it checks whether the tables and columns used in the query really exist
	 * in the database.</p>
	 *
	 * <p><i>Note:
	 * 	This implementation just create a {@link DBChecker} instance with the list given in parameter.
	 * </i></p>
	 *
	 * @param tables	List of all available tables (and indirectly, columns).
	 *
	 * @return	A new ADQL query checker.
	 *
	 * @throws TAPException	If any error occurs while creating the query checker.
	 */
	protected QueryChecker createQueryChecker(final Collection<TAPTable> tables) throws TAPException{
		try{
			return new DBChecker(tables, service.getUDFs(), service.getGeometries(), service.getCoordinateSystems());
		}catch(ParseException e){
			throw new TAPException("Unable to build a DBChecker instance! " + e.getMessage(), e, UWSException.INTERNAL_SERVER_ERROR);
		}
	}

	/* ****** */
	/* UPLOAD */
	/* ****** */

	/**
	 * <p>This implementation just create an {@link Uploader} instance with the given database connection.</p>
	 *
	 * <p><i>Note:
	 * 	This function should be overrided if you need to change the DB name of the TAP_UPLOAD schema.
	 * 	Indeed, by overriding this function you can specify a given TAPSchema to use as TAP_UPLOAD schema
	 * 	in the constructor of {@link Uploader}. But do not forget that this {@link TAPSchema} instance MUST have
	 * 	an ADQL name equals to "TAP_UPLOAD", otherwise, a TAPException will be thrown.
	 * </i></p>
	 */
	@Override
	public Uploader createUploader(final DBConnection dbConn) throws TAPException{
		return new Uploader(service, dbConn);
	}

	/* ************** */
	/* UWS MANAGEMENT */
	/* ************** */

	/**
	 * <p>This implementation just create a {@link UWSService} instance.</p>
	 *
	 * <p><i>Note:
	 * 	This implementation is largely enough for a TAP service. It is not recommended to override
	 * 	this function.
	 * </i></p>
	 */
	@Override
	public UWSService createUWS() throws TAPException{
		try{
			UWSService uws = new UWSService(this, this.service.getFileManager(), this.service.getLogger());
			uws.setName("TAP/async");
			uws.setErrorWriter(errorWriter);
			return uws;
		}catch(UWSException ue){
			throw new TAPException("Can not create a UWS service (asynchronous resource of TAP)!", ue, UWSException.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * <p>This implementation does not provided a backup manager.
	 * It means that no asynchronous job will be restored and backuped.</p>
	 *
	 * <p>You must override this function if you want enable the backup feature.</p>
	 */
	@Override
	public UWSBackupManager createUWSBackupManager(final UWSService uws) throws TAPException{
		return null;
	}

	/**
	 * <p>This implementation provides a basic {@link TAPJob} instance.</p>
	 *
	 * <p>
	 * 	If you need to add or modify the behavior of some functions of a {@link TAPJob},
	 * 	you must override this function and return your own extension of {@link TAPJob}.
	 * </p>
	 */
	@Override
	protected TAPJob createTAPJob(final HttpServletRequest request, final JobOwner owner) throws UWSException{
		try{
			// Extract the HTTP request ID (the job ID should be the same, if not already used by another job):
			String requestID = null;
			if (request.getAttribute(UWS.REQ_ATTRIBUTE_ID) != null && request.getAttribute(UWS.REQ_ATTRIBUTE_ID) instanceof String)
				requestID = request.getAttribute(UWS.REQ_ATTRIBUTE_ID).toString();

			// Extract the TAP parameters from the HTTP request:
			TAPParameters tapParams = createTAPParameters(request);

			// Create the job:
			return new TAPJob(owner, tapParams, requestID);
		}catch(TAPException te){
			if (te.getCause() != null && te.getCause() instanceof UWSException)
				throw (UWSException)te.getCause();
			else
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Can not create a TAP asynchronous job!");
		}
	}

	/**
	 * <p>This implementation provides a basic {@link TAPJob} instance.</p>
	 *
	 * <p>
	 * 	If you need to add or modify the behavior of some functions of a
	 * 	{@link TAPJob}, you must override this function and return your own
	 * 	extension of {@link TAPJob}.
	 * </p>
	 */
	@Override
	protected TAPJob createTAPJob(final String jobId, final long creationTime, final JobOwner owner, final TAPParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws UWSException{
		try{
			return new TAPJob(jobId, creationTime, owner, params, quote, startTime, endTime, results, error);
		}catch(TAPException te){
			if (te.getCause() != null && te.getCause() instanceof UWSException)
				throw (UWSException)te.getCause();
			else
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Can not create a TAP asynchronous job !");
		}
	}

	/**
	 * <p>This implementation extracts standard TAP parameters from the given request.</p>
	 *
	 * <p>
	 * 	Non-standard TAP parameters are added in a map inside the returned {@link TAPParameters} object
	 * 	and are accessible with {@link TAPParameters#get(String)} and {@link TAPParameters#getAdditionalParameters()}.
	 * 	However, if you want to manage them in another way, you must extend {@link TAPParameters} and override
	 * 	this function in order to return an instance of your extension.
	 * </p>
	 */
	@Override
	public TAPParameters createTAPParameters(final HttpServletRequest request) throws TAPException{
		return new TAPParameters(request, service);
	}

	/**
	 * <p>This implementation extracts standard TAP parameters from the given request.</p>
	 *
	 * <p>
	 * 	Non-standard TAP parameters are added in a map inside the returned {@link TAPParameters} object
	 * 	and are accessible with {@link TAPParameters#get(String)} and {@link TAPParameters#getAdditionalParameters()}.
	 * 	However, if you want to manage them in another way, you must extend {@link TAPParameters} and override
	 * 	this function in order to return an instance of your extension.
	 * </p>
	 */
	@Override
	public TAPParameters createTAPParameters(final Map<String,Object> params) throws TAPException{
		return new TAPParameters(service, params);
	}

}
