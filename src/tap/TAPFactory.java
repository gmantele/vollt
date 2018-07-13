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
 */

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import adql.parser.ADQLParser;
import adql.parser.ADQLQueryFactory;
import adql.parser.QueryChecker;
import adql.query.ADQLQuery;
import tap.db.DBConnection;
import tap.metadata.TAPSchema;
import tap.parameters.TAPParameters;
import tap.upload.Uploader;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.UWSFactory;
import uws.service.UWSService;
import uws.service.backup.UWSBackupManager;
import uws.service.error.ServiceErrorWriter;
import uws.service.file.UWSFileManager;
import uws.service.request.RequestParser;

/**
 * <p>Let build essential objects of the TAP service.</p>
 *
 * <p>Basically, it means answering to the following questions:</p>
 * <ul>
 * 	<li>how to connect to the database? <i>({@link DBConnection})</i></li>
 * 	<li>which UWS implementation (default implementation provided by default) to use? <i>({@link UWSService})</i></li>
 * 	<li>whether and how UWS/asynchronous jobs must be backuped and restored? <i>({@link UWSBackupManager})</i></li>
 * 	<li>how to create asynchronous jobs? <i>({@link TAPJob})</i></li>
 * 	<li>whether and how tables must be updated? <i>({@link Uploader})</i></li>
 * 	<li>how to execute an ADQL query? <i>({@link ADQLExecutor})</i>
 * 	<li>how to parser an ADQL query? <i>({@link ADQLParser})</i></li>
 * 	<li>how to check ADQL queries? <i>({@link QueryChecker})</i></li>
 * </ul>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.2 (09/2017)
 */
public abstract class TAPFactory implements UWSFactory {

	/** Connection to the TAP service ; it provides all important service configuration information. */
	protected final ServiceConnection service;

	/**
	 * Build a basic {@link TAPFactory}.
	 * Nothing is done except setting the service connection.
	 *
	 * @param service	Configuration of the TAP service. <i>MUST NOT be NULL</i>
	 *
	 * @throws NullPointerException	If the given {@link ServiceConnection} is NULL.
	 */
	protected TAPFactory(final ServiceConnection service) throws NullPointerException{
		if (service == null)
			throw new NullPointerException("Can not create a TAPFactory without a ServiceConnection instance !");

		this.service = service;
	}

	/**
	 * <p>Get the object to use when an error must be formatted and written to the user.</p>
	 *
	 * <p>This formatted error will be either written in an HTTP response or in a job error summary.</p>
	 *
	 * @return	The error writer to use.
	 *
	 * @since 2.0
	 */
	public abstract ServiceErrorWriter getErrorWriter();

	/* ******************* */
	/* DATABASE CONNECTION */
	/* ******************* */

	/**
	 * <p>Get a free database connection.</p>
	 *
	 * <p>
	 * 	<i>Free</i> means this connection is not currently in use and will be exclusively dedicated to the function/process/thread
	 * 	which has asked for it by calling this function.
	 * </p>
	 *
	 * <p><i>Note:
	 * 	This function can create on the fly a new connection OR get a free one from a connection pool. Considering the
	 * 	creation time of a database connection, the second way is recommended.
	 * </i></p>
	 *
	 * <p><b>IMPORTANT:</b>
	 * 	The returned connection MUST be freed after having used it.
	 * </p>
	 *
	 * <p><i><b>WARNING:</b>
	 * 	Some implementation may free the connection automatically when not used for a specific time.
	 * 	So, do not forget to free the connection after use!
	 * </i></p>
	 *
	 * @param jobID	ID of the job/thread/process which has asked for this connection. <i>note: The returned connection must then be identified thanks to this ID.</i>
	 *
	 * @return	A new and free connection to the database. <b>MUST BE NOT NULL, or otherwise a TAPException should be returned.</b>
	 *
	 * @throws TAPException	If there is any error while getting a free connection.
	 *
	 * @since 2.0
	 */
	public abstract DBConnection getConnection(final String jobID) throws TAPException;

	/**
	 * <p>Free the given connection.</p>
	 *
	 * <p>
	 * 	This function is called by the TAP library when a job/thread does not need this connection any more. It aims
	 * 	to free resources associated to the given database connection.
	 * </p>
	 *
	 * <p><i>Note:
	 * 	This function can just close definitely the connection OR give it back to a connection pool. The implementation is
	 * 	here totally free!
	 * </i></p>
	 *
	 * @param conn	The connection to close.
	 *
	 * @since 2.0
	 */
	public abstract void freeConnection(final DBConnection conn);

	/**
	 * <p>Destroy all resources (and particularly DB connections and JDBC driver) allocated in this factory.</p>
	 *
	 * <p><i>Note:
	 * 	This function is called when the TAP service is shutting down.
	 * 	After this call, the factory may not be able to provide any closed resources ; its behavior may be unpredictable.
	 * </i></p>
	 *
	 * @since 2.0
	 */
	public abstract void destroy();

	/* *************** */
	/* ADQL MANAGEMENT */
	/* *************** */

	/**
	 * <p>Create the object able to execute an ADQL query and to write and to format its result.</p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}
	 * </i></p>
	 *
	 * @return	An ADQL executor.
	 *
	 * @throws TAPException	If any error occurs while creating an ADQL executor.
	 */
	public abstract ADQLExecutor createADQLExecutor() throws TAPException;

	/**
	 * <p>Create a parser of ADQL query.</p>
	 *
	 * <p><i>Warning:
	 * 	This parser can be created with a query factory and/or a query checker.
	 * 	{@link #createQueryFactory()} will be used only if the default query factory (or none) is set
	 * 	in the ADQL parser returned by this function.
	 * 	Idem for {@link #createQueryChecker(TAPSchema)}: it will used only if no query checker is set
	 * 	in the returned ADQL parser.
	 * </i></p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @return	An ADQL query parser.
	 *
	 * @throws TAPException	If any error occurs while creating an ADQL parser.
	 *
	 * @since 2.0
	 */
	public abstract ADQLParser createADQLParser() throws TAPException;

	/**
	 * <p>Create a factory able to build every part of an {@link ADQLQuery} object.</p>
	 *
	 * <p><i>Warning:
	 * 	This function is used only if the default query factory (or none) is set in the ADQL parser
	 * 	returned by {@link #createADQLParser()}.
	 * </i></p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}
	 * </i></p>
	 *
	 * @return	An {@link ADQLQuery} factory.
	 *
	 * @throws TAPException	If any error occurs while creating the factory.
	 */
	public abstract ADQLQueryFactory createQueryFactory() throws TAPException;

	/**
	 * <p>Create an object able to check the consistency between the ADQL query and the database.
	 * That's to say, it checks whether the tables and columns used in the query really exist
	 * in the database.</p>
	 *
	 * <p><i>Warning:
	 * 	This function is used only if no query checker is set in the ADQL parser
	 * 	returned by {@link #createADQLParser()}.
	 * </i></p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}
	 * </i></p>
	 *
	 * @param uploadSchema	ADQL schema containing the description of all uploaded tables.
	 *
	 * @return	A query checker.
	 *
	 * @throws TAPException	If any error occurs while creating a query checker.
	 */
	public abstract QueryChecker createQueryChecker(final TAPSchema uploadSchema) throws TAPException;

	/* ****** */
	/* UPLOAD */
	/* ****** */

	/**
	 * <p>Create an object able to manage the creation of submitted user tables (in VOTable) into the database.</p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @param dbConn	The database connection which has requested an {@link Uploader}.
	 *
	 * @return	An {@link Uploader}.
	 *
	 * @throws TAPException	If any error occurs while creating an {@link Uploader} instance.
	 */
	public abstract Uploader createUploader(final DBConnection dbConn) throws TAPException;

	/* ************** */
	/* UWS MANAGEMENT */
	/* ************** */

	/**
	 * <p>Create the object which will manage the asynchronous resource of the TAP service.
	 * This resource is a UWS service.</p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @return	A UWS service which will be the asynchronous resource of this TAP service.
	 *
	 * @throws TAPException	If any error occurs while creating this UWS service.
	 */
	public abstract UWSService createUWS() throws TAPException;

	/**
	 * <p>Create the object which will manage the backup and restoration of all asynchronous jobs.</p>
	 *
	 * <p><i>Note:
	 * 	This function may return NULL. If it does, asynchronous jobs won't be backuped.
	 * </i></p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @param uws	The UWS service which has to be backuped and restored.
	 *
	 * @return	The backup manager to use. <i>MAY be NULL</i>
	 *
	 * @throws TAPException	If any error occurs while creating this backup manager.
	 */
	public abstract UWSBackupManager createUWSBackupManager(final UWSService uws) throws TAPException;

	/**
	 * <p>Creates a (PENDING) UWS job from the given HTTP request.</p>
	 *
	 * <p>
	 * 	This implementation just call {@link #createTAPJob(HttpServletRequest, JobOwner)}
	 * 	with the given request, in order to ensure that the returned object is always a {@link TAPJob}.
	 * </p>
	 *
	 * @see uws.service.AbstractUWSFactory#createJob(javax.servlet.http.HttpServletRequest, uws.job.user.JobOwner)
	 * @see #createTAPJob(HttpServletRequest, JobOwner)
	 */
	@Override
	public final UWSJob createJob(HttpServletRequest request, JobOwner owner) throws UWSException{
		return createTAPJob(request, owner);
	}

	/**
	 * <p>Create a PENDING asynchronous job from the given HTTP request.</p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @param request	Request which contains all parameters needed to set correctly the asynchronous job to create.
	 * @param owner		The user which has requested the job creation.
	 *
	 * @return	A new PENDING asynchronous job.
	 *
	 * @throws UWSException	If any error occurs while reading the parameters in the request or while creating the job.
	 */
	protected abstract TAPJob createTAPJob(final HttpServletRequest request, final JobOwner owner) throws UWSException;

	/**
	 * <p>Creates a UWS job with the following attributes.</p>
	 *
	 * <p>
	 * 	This implementation just call {@link #createTAPJob(String, long, JobOwner, TAPParameters, long, long, long, List, ErrorSummary)}
	 * 	with the given parameters, in order to ensure that the returned object
	 * 	is always a {@link TAPJob}.
	 * </p>
	 *
	 * <p><i>Note 1:
	 * 	This function is mainly used to restore a UWS job at the UWS
	 * 	initialization.
	 * </i></p>
	 *
	 * <p><i>Note 2:
	 * 	The job phase is chosen automatically from the given job attributes
	 * 	(i.e. no endTime => PENDING, no result and no error => ABORTED, ...).
	 * </i></p>
	 *
	 * @see uws.service.AbstractUWSFactory#createJob(String, long, JobOwner, UWSParameters, long, long, long, List, ErrorSummary)
	 * @see #createTAPJob(String, long, JobOwner, TAPParameters, long, long, long, List, ErrorSummary)
	 */
	@Override
	public final UWSJob createJob(String jobId, long creationTime, JobOwner owner, final UWSParameters params, long quote, long startTime, long endTime, List<Result> results, ErrorSummary error) throws UWSException{
		return createTAPJob(jobId, creationTime, owner, (TAPParameters)params, quote, startTime, endTime, results, error);
	}

	/**
	 * <p>Create a PENDING asynchronous job with the given parameters.</p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided in {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @param jobId			ID of the job (NOT NULL).
	 * @param creationTime	Creation date/time of the job (SHOULD NOT BE
	 *                    	NEGATIVE OR NULL).
	 * @param owner			Owner of the job.
	 * @param params		List of all input job parameters.
	 * @param quote			Its quote (in seconds).
	 * @param startTime		Date/Time of the start of this job.
	 * @param endTime		Date/Time of the end of this job.
	 * @param results		All results of this job.
	 * @param error			The error which ended the job to create.
	 *
	 * @return	A new PENDING asynchronous job.
	 *
	 * @throws UWSException	If there is an error while creating the job.
	 *
	 * @since 2.2
	 */
	protected abstract TAPJob createTAPJob(final String jobId, final long creationTime, final JobOwner owner, final TAPParameters params, final long quote, final long startTime, final long endTime, final List<Result> results, final ErrorSummary error) throws UWSException;

	/**
	 * <p>Create the thread which will execute the task described by the given UWSJob instance.</p>
	 *
	 * <p>
	 * 	This function is definitely implemented here and can not be overridden. The processing of
	 * 	an ADQL query must always be the same in a TAP service ; it is completely done by {@link AsyncThread}.
	 * </p>
	 *
	 * @see uws.service.UWSFactory#createJobThread(uws.job.UWSJob)
	 * @see AsyncThread
	 */
	@Override
	public final JobThread createJobThread(final UWSJob job) throws UWSException{
		try{
			return new AsyncThread((TAPJob)job, createADQLExecutor(), getErrorWriter());
		}catch(TAPException te){
			if (te.getCause() != null && te.getCause() instanceof UWSException)
				throw (UWSException)te.getCause();
			else
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, "Impossible to create an AsyncThread !");
		}
	}

	/**
	 * <p>Extract the parameters from the given request (multipart or not).</p>
	 *
	 * <p>
	 * 	This function is used only to create the set of parameters for a TAP job (synchronous or asynchronous).
	 * 	Thus, it just call {@link #createTAPParameters(HttpServletRequest)} with the given request, in order to ensure
	 * 	that the returned object is always a {@link TAPParameters}.
	 * </p>
	 *
	 * @see #createTAPParameters(HttpServletRequest)
	 */
	@Override
	public final UWSParameters createUWSParameters(HttpServletRequest request) throws UWSException{
		try{
			return createTAPParameters(request);
		}catch(TAPException te){
			if (te.getCause() != null && te.getCause() instanceof UWSException)
				throw (UWSException)te.getCause();
			else
				throw new UWSException(te.getHttpErrorCode(), te);
		}
	}

	/**
	 * <p>Extract all the TAP parameters from the given HTTP request (multipart or not) and return them.</p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @param request	The HTTP request containing the TAP parameters to extract.
	 *
	 * @return	An object gathering all successfully extracted TAP parameters.
	 *
	 * @throws TAPException	If any error occurs while extracting the parameters.
	 */
	public abstract TAPParameters createTAPParameters(final HttpServletRequest request) throws TAPException;

	/**
	 * <p>Identify and gather all identified parameters of the given map inside a {@link TAPParameters} object.</p>
	 *
	 * <p>
	 * 	This implementation just call {@link #createTAPParameters(Map)} with the given map, in order to ensure
	 * 	that the returned object is always a {@link TAPParameters}.
	 * </p>
	 *
	 * @see uws.service.AbstractUWSFactory#createUWSParameters(java.util.Map)
	 * @see #createTAPParameters(Map)
	 */
	@Override
	public final UWSParameters createUWSParameters(Map<String,Object> params) throws UWSException{
		try{
			return createTAPParameters(params);
		}catch(TAPException te){
			if (te.getCause() != null && te.getCause() instanceof UWSException)
				throw (UWSException)te.getCause();
			else
				throw new UWSException(te.getHttpErrorCode(), te);
		}
	}

	/**
	 * <p>Identify all TAP parameters and gather them inside a {@link TAPParameters} object.</p>
	 *
	 * <p><i>Note:
	 * 	A default implementation is provided by {@link AbstractTAPFactory}.
	 * </i></p>
	 *
	 * @param params	Map containing all parameters.
	 *
	 * @return	An object gathering all successfully identified TAP parameters.
	 *
	 * @throws TAPException	If any error occurs while creating the {@link TAPParameters} object.
	 */
	public abstract TAPParameters createTAPParameters(final Map<String,Object> params) throws TAPException;

	@Override
	public RequestParser createRequestParser(final UWSFileManager fileManager) throws UWSException{
		return new TAPRequestParser(fileManager);
	}

}
