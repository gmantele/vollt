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
 * Copyright 2012-2016 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import adql.parser.ADQLParser;
import adql.parser.ADQLQueryFactory;
import adql.parser.ParseException;
import adql.query.ADQLQuery;
import tap.data.DataReadException;
import tap.data.TableIterator;
import tap.db.DBConnection;
import tap.db.DBException;
import tap.formatter.OutputFormat;
import tap.log.TAPLog;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.parameters.DALIUpload;
import tap.parameters.TAPParameters;
import tap.upload.Uploader;
import uws.UWSException;
import uws.UWSToolBox;
import uws.job.JobThread;
import uws.job.Result;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>Let process completely an ADQL query.</p>
 * 
 * <p>Thus, this class aims to apply the following actions (in the given order):</p>
 * <ol>
 * 	<li>Upload the user tables, if any</li>
 * 	<li>Parse the ADQL query (and so, transform it in an object tree)</li>
 * 	<li>Execute it in the "database"</li>
 * 	<li>Format and write the result</li>
 * 	<li>Drop all uploaded tables from the "database"</li>
 * </ol>
 * 
 * <h3>Job execution mode</h3>
 * 
 * <p>
 * 	This executor is able to process queries coming from a synchronous job (the result must be written directly in the HTTP response)
 * 	and from an asynchronous job (the result must be written, generally, in a file). Two start(...) functions let deal with
 * 	the differences between the two job execution modes: {@link #start(AsyncThread)} for asynchronous jobs
 * 	and {@link #start(Thread, String, TAPParameters, HttpServletResponse)} for synchronous jobs.
 * </p>
 * 
 * <h3>Input/Output formats</h3>
 * 
 * <p>Uploaded tables must be provided in VOTable format.</p>
 * 
 * <p>
 * 	Query results must be formatted in the format specified by the user in the job parameters. A corresponding formatter ({@link OutputFormat})
 * 	is asked to the description of the TAP service ({@link ServiceConnection}). If none can be found, VOTable will be chosen by default.
 * </p>
 * 
 * <h3>Executor customization</h3>
 * 
 * <p>It is totally possible to customize some parts of the ADQL query processing. However, the main algorithm must remain the same and is implemented
 * 	by {@link #start()}. This function is final, like {@link #start(AsyncThread)} and {@link #start(Thread, String, TAPParameters, HttpServletResponse)},
 * 	which are just preparing the execution for {@link #start()} in function of the job execution mode (asynchronous or synchronous).
 * </p>
 * 
 * <p><i>Note:
 * 	{@link #start()} is using the Template Method Design Pattern: it defines the skeleton/algorithm of the processing, and defers some steps
 * 	to other functions.
 * </i></p>
 * 
 * <p>
 * 	So, you are able to customize almost all individual steps of the ADQL query processing: {@link #parseADQL()}, {@link #executeADQL(ADQLQuery)} and
 * 	{@link #writeResult(TableIterator, OutputFormat, OutputStream)}.
 * </p>
 * 
 * <p><i>Note:
 * 	Note that the formatting of the result is done by an OutputFormat and that the executor is just calling the appropriate function of the formatter.
 * </i></p>
 * 
 * <p>
 * 	There is no way in this executor to customize the upload. However, it does not mean it can not be customized.
 * 	Indeed you can do it easily by extending {@link Uploader} and by providing the new class inside your {@link TAPFactory} implementation
 * (see {@link TAPFactory#createUploader(DBConnection)}).
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.1 (04/2016)
 */
public class ADQLExecutor {

	/** Description of the current TAP service. */
	protected final ServiceConnection service;
	/** The logger to use. */
	protected final TAPLog logger;

	/** The thread which is using this executor. */
	protected Thread thread;
	/** List of all TAP parameters needed for the query execution (and particularly the ADQL query itself). */
	protected TAPParameters tapParams;
	/** Description of the ADQL schema containing all the tables uploaded by the user for this specific query execution.
	 * <i>Note: This attribute is NULL before calling one of the start(...) function. It MAY be NULL also after if no table has been uploaded.</i> */
	protected TAPSchema uploadSchema = null;

	/** The HTTP response in which the query execution must be written. This attribute is NULL if the execution is asynchronous. */
	protected HttpServletResponse response;
	/** The execution report to fill gradually while the processing of the query.
	 * <i>Note: This attribute is NULL before calling one of the start(...) function, but it will never be after this call.</i> */
	protected TAPExecutionReport report;

	/** Connection to the "database".
	 * <i>Note: This attribute is NULL before and after the query processing (= call of a start(...) function).</i> */
	private DBConnection dbConn = null;
	/** ID of the current query processing step (uploading, parsing, execution, writing result, ...).
	 * <i>Note: This attribute is NULL before and after the query processing (= call of a start(...) function).</i> */
	private ExecutionProgression progression = null;
	/** Date/Time at which the current query processing step has started. */
	private long startStep = -1;

	/**
	 * Build an {@link ADQLExecutor}.
	 * 
	 * @param service	The description of the TAP service.
	 */
	public ADQLExecutor(final ServiceConnection service){
		this.service = service;
		this.logger = service.getLogger();
	}

	/**
	 * Get the logger used by this executor.
	 * 
	 * @return	The used logger.
	 */
	public final TAPLog getLogger(){
		return logger;
	}

	/**
	 * <p>Get the report of the query execution. It helps indicating the execution progression and the duration of each step.</p>
	 * 
	 * <p><i>Note:
	 * 	Before starting the execution (= before the call of a "start(...)" function), this function will return NULL.
	 * 	It is set when the query processing starts and remains not NULL after that (even when the execution is finished).
	 * </i></p>
	 * 
	 * @return	The execution report.
	 */
	public final TAPExecutionReport getExecReport(){
		return report;
	}

	/**
	 * <p>Get the object to use in order to write the query result in the appropriate format
	 * (either the asked one, or else VOTable).</p>
	 * 
	 * @return	The appropriate result formatter to use. <i>Can not be NULL!</i>
	 * 
	 * @throws TAPException	If no format corresponds to the asked one and if no default format (for VOTable) can be found.
	 * 
	 * @see ServiceConnection#getOutputFormat(String)
	 */
	protected OutputFormat getFormatter() throws TAPException{
		// Search for the corresponding formatter:
		String format = tapParams.getFormat();
		OutputFormat formatter = service.getOutputFormat((format == null) ? "votable" : format);
		if (format != null && formatter == null)
			formatter = service.getOutputFormat("votable");

		// Format the result:
		if (formatter == null)
			throw new TAPException("Impossible to format the query result: no formatter has been found for the given MIME type \"" + format + "\" and for the default MIME type \"votable\" (short form) !");

		return formatter;
	}

	/**
	 * <p>Start the asynchronous processing of the ADQL query.</p>
	 * 
	 * <p>
	 * 	This function initialize the execution report, get the execution parameters (including the query to process)
	 * 	and then call {@link #start()}.
	 * </p>
	 * 
	 * @param thread	The asynchronous thread which asks the query processing.
	 * 
	 * @return	The resulting execution report.
	 * 
	 * @throws UWSException			If any error occurs while executing the ADQL query.
	 * @throws InterruptedException	If the job has been interrupted (by the user or a time-out).
	 * 
	 * @see #start()
	 */
	public final TAPExecutionReport start(final AsyncThread thread) throws UWSException, InterruptedException{
		if (this.thread != null || this.report != null)
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "This ADQLExecutor has already been executed!");

		this.thread = thread;

		TAPJob tapJob = thread.getTAPJob();
		this.tapParams = tapJob.getTapParams();
		this.report = new TAPExecutionReport(tapJob.getJobId(), false, tapParams);
		this.response = null;

		try{
			return start();
		}catch(IOException ioe){
			if (thread.isInterrupted())
				return report;
			else
				throw new UWSException(ioe);
		}catch(TAPException te){
			if (thread.isInterrupted())
				return report;
			else
				throw new UWSException(te.getHttpErrorCode(), te);
		}catch(UWSException ue){
			if (thread.isInterrupted())
				return report;
			else
				throw ue;
		}
	}

	/**
	 * <p>Create the database connection required for the ADQL execution.</p>
	 * 
	 * <p><i>Note: This function has no effect if the DB connection already exists.</i></p>
	 * 
	 * @param jobID	ID of the job which will be executed by this {@link ADQLExecutor}.
	 *             	This ID will be the database connection ID.
	 * 
	 * @throws TAPException	If the DB connection creation fails.
	 * 
	 * @see TAPFactory#getConnection(String)
	 * 
	 * @since 2.0
	 */
	public final void initDBConnection(final String jobID) throws TAPException{
		if (dbConn == null)
			dbConn = service.getFactory().getConnection(jobID);
	}

	/**
	 * Cancel the current SQL query execution or result set fetching if any is currently running.
	 * If no such process is on going, this function has no effect.
	 * 
	 * @since 2.1
	 */
	public final void cancelQuery(){
		if (dbConn != null && progression == ExecutionProgression.EXECUTING_ADQL)
			dbConn.cancel(true);
	}

	/**
	 * <p>Start the synchronous processing of the ADQL query.</p>
	 * 
	 * <p>This function initialize the execution report and then call {@link #start()}.</p>
	 * 
	 * @param thread	The synchronous thread which asks the query processing.
	 * @param jobId		ID of the corresponding job.
	 * @param params	All execution parameters (including the query to process).
	 * @param response	Object in which the result or the error must be written.
	 * 
	 * @return	The resulting execution report.
	 * 
	 * @throws TAPException			If any error occurs while executing the ADQL query.
	 * @throws IOException			If any error occurs while writing the result in the given {@link HttpServletResponse}.
	 * @throws InterruptedException	If the job has been interrupted (by the user or a time-out).
	 * 
	 * @see #start()
	 */
	public final TAPExecutionReport start(final Thread thread, final String jobId, final TAPParameters params, final HttpServletResponse response) throws TAPException, IOException, InterruptedException{
		if (this.thread != null || this.report != null)
			throw new TAPException("This ADQLExecutor has already been executed!");

		this.thread = thread;
		this.tapParams = params;
		this.report = new TAPExecutionReport(jobId, true, tapParams);
		this.response = response;

		try{
			return start();
		}catch(UWSException ue){
			throw new TAPException(ue, ue.getHttpErrorCode());
		}
	}

	/**
	 * <p>Process the ADQL query.</p>
	 * 
	 * <p>This function calls the following function (in the same order):</p>
	 * <ol>
	 * 	<li>{@link TAPFactory#getConnection(String)}</li>
	 * 	<li>{@link #uploadTables()}</li>
	 * 	<li>{@link #parseADQL()}</li>
	 * 	<li>{@link #executeADQL(ADQLQuery)}</li>
	 * 	<li>{@link #writeResult(TableIterator)}</li>
	 * 	<li>{@link #dropUploadedTables()}</li>
	 * 	<li>{@link TAPFactory#freeConnection(DBConnection)}</li>
	 * </ol>
	 * 
	 * <p>
	 * 	The execution report is updated gradually. Besides a job parameter - progression - is set at each step of the process in order to
	 * 	notify the user of the progression of the query execution. This parameter is removed at the end of the execution if it is successful.
	 * </p>
	 * 
	 * <p>The "interrupted" flag of the associated thread is often tested so that stopping the execution as soon as possible.</p>
	 * 
	 * @return	The updated execution report.
	 * 
	 * @throws TAPException			If any error occurs while executing the ADQL query.
	 * @throws UWSException			If any error occurs while executing the ADQL query.
	 * @throws IOException			If an error happens while writing the result in the specified {@link HttpServletResponse}.
	 *                    			<i>That kind of error can be thrown only in synchronous mode.
	 *                    			In asynchronous, the error is stored as job error report and is never propagated.</i>
	 * @throws InterruptedException	If the job has been interrupted (by the user or a time-out).
	 */
	protected final TAPExecutionReport start() throws TAPException, UWSException, IOException, InterruptedException{
		logger.logTAP(LogLevel.INFO, report, "START_EXEC", (report.synchronous ? "Synchronous" : "Asynchronous") + " execution of an ADQL query STARTED.", null);

		// Save the start time (for reporting usage):
		long start = System.currentTimeMillis();

		TableIterator queryResult = null;

		try{
			// Get a "database" connection:
			initDBConnection(report.jobID);

			// 1. UPLOAD TABLES, if there is any:
			if (tapParams.getUploadedTables() != null && tapParams.getUploadedTables().length > 0){
				startStep(ExecutionProgression.UPLOADING);
				uploadTables();
				endStep();
			}

			if (thread.isInterrupted())
				throw new InterruptedException();

			// 2. PARSE THE ADQL QUERY:
			startStep(ExecutionProgression.PARSING);
			// Parse the query:
			ADQLQuery adqlQuery = null;
			try{
				adqlQuery = parseADQL();
			}catch(ParseException pe){
				if (report.synchronous)
					throw new TAPException("Incorrect ADQL query: " + pe.getMessage(), pe, UWSException.BAD_REQUEST, tapParams.getQuery(), progression);
				else
					throw new UWSException(UWSException.BAD_REQUEST, pe, "Incorrect ADQL query: " + pe.getMessage());
			}
			// List all resulting columns (it will be useful later to format the result):
			report.resultingColumns = adqlQuery.getResultingColumns();
			endStep();

			if (thread.isInterrupted())
				throw new InterruptedException();

			// 3. EXECUTE THE ADQL QUERY:
			startStep(ExecutionProgression.EXECUTING_ADQL);
			queryResult = executeADQL(adqlQuery);
			endStep();

			if (thread.isInterrupted())
				throw new InterruptedException();

			// 4. WRITE RESULT:
			startStep(ExecutionProgression.WRITING_RESULT);
			writeResult(queryResult);
			endStep();

			// Report the COMPLETED status:
			tapParams.remove(TAPJob.PARAM_PROGRESSION);
			report.success = true;

			// Set the total duration in the report:
			report.setTotalDuration(System.currentTimeMillis() - start);

			// Log and report the end of this execution:
			logger.logTAP(LogLevel.INFO, report, "END_EXEC", "ADQL query execution finished.", null);

			return report;
		}finally{
			// Close the result if any:
			if (queryResult != null){
				try{
					queryResult.close();
				}catch(DataReadException dre){
					logger.logTAP(LogLevel.WARNING, report, "END_EXEC", "Can not close the database query result!", dre);
				}
			}

			// Drop all the uploaded tables (they are not supposed to exist after the query execution):
			try{
				dropUploadedTables();
			}catch(TAPException e){
				logger.logTAP(LogLevel.WARNING, report, "END_EXEC", "Can not drop the uploaded tables from the database!", e);
			}

			// Free the connection (so that giving it back to a pool if any, otherwise just free resources):
			if (dbConn != null){
				service.getFactory().freeConnection(dbConn);
				dbConn = null;
			}
		}
	}

	/**
	 * <p>Memorize the time at which the step starts, the step ID and update the job parameter "progression"
	 * (to notify the user about the progression of the query processing).</p>
	 * 
	 * <p><i>Note:
	 * 	If for some reason the job parameter "progression" can not be updated, no error will be thrown. A WARNING message
	 * 	will be just written in the log.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This function is designed to work with {@link #endStep()}, which must be called after it, when the step is finished (successfully or not).
	 * </i></p>
	 * 
	 * @param progression	ID of the starting step.
	 * 
	 * @see #endStep()
	 */
	private void startStep(final ExecutionProgression progression){
		// Save the start time (for report usage):
		startStep = System.currentTimeMillis();
		// Memorize the current step:
		this.progression = progression;
		// Update the job parameter "progression", to notify the user about the progression of the query processing:
		try{
			tapParams.set(TAPJob.PARAM_PROGRESSION, this.progression);
		}catch(UWSException ue){
			// should not happen, but just in case...
			logger.logTAP(LogLevel.WARNING, report, "START_STEP", "Can not set/update the informative job parameter \"" + TAPJob.PARAM_PROGRESSION + "\" (this parameter would be just for notification purpose about the execution progression)!", ue);
		}
	}

	/**
	 * <p>Set the duration of the current step in the execution report.</p>
	 * 
	 * <p><i>Note:
	 * 	The start time and the ID of the step are then forgotten.
	 * </i></p>
	 * 
	 * <p><i>Note:
	 * 	This function is designed to work with {@link #startStep(ExecutionProgression)}, which must be called before it, when the step is starting.
	 * 	It marks the end of a step.
	 * </i></p>
	 * 
	 * @see #startStep(ExecutionProgression)
	 */
	private void endStep(){
		if (progression != null){
			// Set the duration of this step in the execution report:
			report.setDuration(progression, System.currentTimeMillis() - startStep);
			// No start time:
			startStep = -1;
			// No step for the moment:
			progression = null;
		}
	}

	/**
	 * <p>Create in the "database" all tables uploaded by the user (only for this specific query execution).</p>
	 * 
	 * <p><i>Note:
	 * 	Obviously, nothing is done if no table has been uploaded.
	 * </i></p>
	 * 
	 * @throws TAPException	If any error occurs while reading the uploaded table
	 *                     	or while importing them in the database.
	 */
	private final void uploadTables() throws TAPException{
		// Fetch the tables to upload:
		DALIUpload[] tables = tapParams.getUploadedTables();

		// Upload them, if needed:
		if (tables.length > 0){
			logger.logTAP(LogLevel.INFO, report, "UPLOADING", "Loading uploaded tables (" + tables.length + ")", null);
			uploadSchema = service.getFactory().createUploader(dbConn).upload(tables);
		}
	}

	/**
	 * <p>Parse the ADQL query provided in the parameters by the user.</p>
	 * 
	 * <p>The query factory and the query checker are got from the TAP factory.</p>
	 * 
	 * <p>
	 * 	The configuration of this TAP service list all allowed coordinate systems. These are got here and provided to the query checker
	 * 	in order to ensure the coordinate systems used in the query are in this list.
	 * </p>
	 * 
	 * <p>
	 * 	The row limit specified in the ADQL query (with TOP) is checked and adjusted (if needed). Indeed, this limit
	 * 	can not exceed MAXREC given in parameter and the maximum value specified in the configuration of this TAP service.
	 * 	In the case no row limit is specified in the query or the given value is greater than MAXREC, (MAXREC+1) is used by default.
	 * 	The "+1" aims to detect overflows.
	 * </p>
	 * 
	 * @return	The object representation of the ADQL query.
	 * 
	 * @throws ParseException			If the given ADQL query can not be parsed or if the construction of the object representation has failed.
	 * @throws InterruptedException		If the thread has been interrupted.
	 * @throws TAPException				If the TAP factory is unable to create the ADQL factory or the query checker.
	 */
	protected ADQLQuery parseADQL() throws ParseException, InterruptedException, TAPException{
		// Log the start of the parsing:
		logger.logTAP(LogLevel.INFO, report, "PARSING", "Parsing ADQL: " + tapParams.getQuery().replaceAll("(\t|\r?\n)+", " "), null);

		// Create the ADQL parser:
		ADQLParser parser = service.getFactory().createADQLParser();
		if (parser == null){
			logger.logTAP(LogLevel.WARNING, null, "PARSING", "No ADQL parser returned by the TAPFactory! The default implementation is used instead.", null);
			parser = new ADQLParser();
		}

		// Set the ADQL factory:
		if (parser.getQueryFactory() == null || parser.getQueryFactory().getClass() == ADQLQueryFactory.class)
			parser.setQueryFactory(service.getFactory().createQueryFactory());

		// Set the query checker:
		if (parser.getQueryChecker() == null)
			parser.setQueryChecker(service.getFactory().createQueryChecker(uploadSchema));

		// Parse the ADQL query:
		ADQLQuery query = parser.parseQuery(tapParams.getQuery());

		// Set or check the row limit:
		final int limit = query.getSelect().getLimit();
		final Integer maxRec = tapParams.getMaxRec();
		if (maxRec != null && maxRec > -1){
			if (limit <= -1 || limit > maxRec)
				query.getSelect().setLimit(maxRec + 1);
		}

		return query;
	}

	/**
	 * <p>Execute in "database" the given object representation of an ADQL query.</p>
	 * 
	 * <p>By default, this function is just calling {@link DBConnection#executeQuery(ADQLQuery)} and then it returns the value returned by this call.</p>
	 * 
	 * <p><i>Note:
	 * 	An INFO message is logged at the end of the query execution in order to report the result status (success or error)
	 * 	and the execution duration.
	 * </i></p>
	 * 
	 * @param adql	The object representation of the ADQL query to execute.
	 * 
	 * @return	The result of the query,
	 *        	or NULL if the query execution has failed.
	 * 
	 * @throws InterruptedException	If the thread has been interrupted.
	 * @throws TAPException			If the {@link DBConnection} has failed to deal with the given ADQL query.
	 * 
	 * @see DBConnection#executeQuery(ADQLQuery)
	 */
	protected TableIterator executeADQL(final ADQLQuery adql) throws InterruptedException, TAPException{
		// Log the start of execution:
		logger.logTAP(LogLevel.INFO, report, "START_DB_EXECUTION", "ADQL query: " + adql.toADQL().replaceAll("(\t|\r?\n)+", " "), null);

		// Set the fetch size, if any:
		if (service.getFetchSize() != null && service.getFetchSize().length >= 1){
			if (report.synchronous && service.getFetchSize().length >= 2)
				dbConn.setFetchSize(service.getFetchSize()[1]);
			else
				dbConn.setFetchSize(service.getFetchSize()[0]);
		}

		// Execute the ADQL query:
		TableIterator result = dbConn.executeQuery(adql);

		// Log the success or failure:
		if (result == null)
			logger.logTAP(LogLevel.INFO, report, "END_DB_EXECUTION", "Query execution aborted after " + (System.currentTimeMillis() - startStep) + "ms!", null);
		else
			logger.logTAP(LogLevel.INFO, report, "END_DB_EXECUTION", "Query successfully executed in " + (System.currentTimeMillis() - startStep) + "ms!", null);

		return result;
	}

	/**
	 * <p>Write the given query result into the appropriate format in the appropriate output
	 * (HTTP response for a synchronous execution, otherwise a file or any output provided by UWS).</p>
	 * 
	 * <p>This function prepare the output in function of the execution type (synchronous or asynchronous).
	 * 	Once prepared, the result, the output and the formatter to use are given to {@link #writeResult(TableIterator, OutputFormat, OutputStream)}
	 * 	which will really process the result formatting and writing.
	 * </p>
	 * 
	 * @param queryResult	The result of the query execution in database.
	 * 
	 * @throws InterruptedException	If the thread has been interrupted.
	 * @throws IOException			If an error happens while writing the result in the {@link HttpServletResponse}.
	 *                    			<i>That kind of error can be thrown only in synchronous mode.
	 *                    			In asynchronous, the error is stored as job error report and is never propagated.</i>
	 * @throws TAPException			If an error occurs while getting the appropriate formatter or while formatting or writing (synchronous execution) the result.
	 * @throws UWSException			If an error occurs while getting the output stream or while writing (asynchronous execution) the result.
	 * 
	 * @see #writeResult(TableIterator, OutputFormat, OutputStream)
	 */
	protected final void writeResult(final TableIterator queryResult) throws InterruptedException, IOException, TAPException, UWSException{
		// Log the start of the writing:
		logger.logTAP(LogLevel.INFO, report, "WRITING_RESULT", "Writing the query result", null);

		// Get the appropriate result formatter:
		OutputFormat formatter = getFormatter();

		// CASE SYNCHRONOUS:
		if (response != null){
			long start = -1;

			// Set the HTTP content type to the MIME type of the result format:
			response.setContentType(formatter.getMimeType());

			// Set the character encoding:
			response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

			// Write the formatted result in the HTTP response output:
			start = System.currentTimeMillis();
			writeResult(queryResult, formatter, response.getOutputStream());

			logger.logTAP(LogLevel.INFO, report, "RESULT_WRITTEN", "Result formatted (in " + formatter.getMimeType() + " ; " + (report.nbRows < 0 ? "?" : report.nbRows) + " rows ; " + ((report.resultingColumns == null) ? "?" : report.resultingColumns.length) + " columns) in " + ((start <= 0) ? "?" : (System.currentTimeMillis() - start)) + "ms!", null);
		}
		// CASE ASYNCHRONOUS:
		else{
			boolean completed = false;
			long start = -1, end = -1;
			Result result = null;
			JobThread jobThread = (JobThread)thread;
			try{
				// Create a UWS Result object to store the result
				// (the result will be stored in a file and this object is the association between the job and the result file):
				result = jobThread.createResult();

				// Set the MIME type of the result format in the result description:
				result.setMimeType(formatter.getMimeType());

				// Write the formatted result in the file output:
				start = System.currentTimeMillis();
				writeResult(queryResult, formatter, jobThread.getResultOutput(result));
				end = System.currentTimeMillis();

				// Set the size (in bytes) of the result in the result description:
				result.setSize(jobThread.getResultSize(result));

				// Add the result description and link in the job description:
				jobThread.publishResult(result);

				completed = true;

				logger.logTAP(LogLevel.INFO, report, "RESULT_WRITTEN", "Result formatted (in " + formatter.getMimeType() + " ; " + (report.nbRows < 0 ? "?" : report.nbRows) + " rows ; " + ((report.resultingColumns == null) ? "?" : report.resultingColumns.length) + " columns) in " + ((start <= 0 || end <= 0) ? "?" : (end - start)) + "ms!", null);

			}catch(IOException ioe){
				// Propagate the exception:
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Impossible to write in the file into which the result of the job " + report.jobID + " must be written!");
			}finally{
				if (!completed){
					// Delete the result file (it is either incomplete or incorrect ;
					// it is then not reliable and is anyway not associated with the job and so could not be later deleted when the job will be):
					if (result != null){
						try{
							service.getFileManager().deleteResult(result, jobThread.getJob());
						}catch(IOException ioe){
							logger.logTAP(LogLevel.ERROR, report, "WRITING_RESULT", "The result writting has failed and the produced partial result must be deleted, but this deletion also failed! (job: " + report.jobID + ")", ioe);
						}
					}
				}
			}
		}
	}

	/**
	 * <p>Format and write the given result in the given output with the given formatter.</p>
	 * 
	 * <p>By default, this function is just calling {@link OutputFormat#writeResult(TableIterator, OutputStream, TAPExecutionReport, Thread)}.</p>
	 * 
	 * <p><i>Note:
	 * 	{@link OutputFormat#writeResult(TableIterator, OutputStream, TAPExecutionReport, Thread)} is often testing the "interrupted" flag of the
	 * 	thread in order to stop as fast as possible if the user has cancelled the job or if the thread has been interrupted for another reason.
	 * </i></p>
	 * 
	 * @param queryResult	Query result to format and to output.
	 * @param formatter		The object able to write the result in the appropriate format.
	 * @param output		The stream in which the result must be written.
	 * 
	 * @throws InterruptedException	If the thread has been interrupted.
	 * @throws IOException			If there is an error while writing the result in the given stream.
	 * @throws TAPException			If there is an error while formatting the result.
	 */
	protected void writeResult(TableIterator queryResult, OutputFormat formatter, OutputStream output) throws InterruptedException, IOException, TAPException{
		formatter.writeResult(queryResult, output, report, thread);
	}

	/**
	 * <p>Drop all tables uploaded by the user from the database.</p>
	 * 
	 * <p><i>Note:
	 * 	By default, if an error occurs while dropping a table from the database, the error will just be logged ; it won't be thrown/propagated.
	 * </i></p>
	 * 
	 * @throws TAPException	If a grave error occurs. <i>By default, no exception is thrown ; they are just logged.</i>
	 */
	protected void dropUploadedTables() throws TAPException{
		if (uploadSchema != null){
			// Drop all uploaded tables:
			for(TAPTable t : uploadSchema){
				try{
					dbConn.dropUploadedTable(t);
				}catch(DBException dbe){
					logger.logTAP(LogLevel.ERROR, report, "DROP_UPLOAD", "Can not drop the uploaded table \"" + t.getDBName() + "\" (in adql \"" + t.getADQLName() + "\") from the database!", dbe);
				}
			}
		}
	}

}
