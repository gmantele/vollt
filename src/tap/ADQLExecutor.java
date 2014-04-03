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

import java.io.IOException;
import java.io.OutputStream;

import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import adql.parser.ADQLParser;
import adql.parser.ADQLQueryFactory;
import adql.parser.ParseException;
import adql.parser.QueryChecker;

import adql.query.ADQLQuery;

import adql.translator.ADQLTranslator;
import adql.translator.TranslationException;

import tap.db.DBConnection;
import tap.db.DBException;

import tap.formatter.OutputFormat;

import tap.log.TAPLog;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.parameters.TAPParameters;

import tap.upload.TableLoader;

import uws.UWSException;

import uws.job.JobThread;
import uws.job.Result;

public class ADQLExecutor<R> {

	protected final ServiceConnection<R> service;
	protected final TAPLog logger;

	protected Thread thread;
	protected TAPParameters tapParams;
	protected HttpServletResponse response;
	protected TAPExecutionReport report;

	private DBConnection<R> dbConn = null;
	protected TAPSchema uploadSchema = null;


	public ADQLExecutor(final ServiceConnection<R> service){
		this.service = service;
		this.logger = service.getLogger();
	}

	public final TAPLog getLogger() {
		return logger;
	}

	public final TAPExecutionReport getExecReport(){
		return report;
	}

	public boolean hasUploadedTables(){
		return (uploadSchema != null) && (uploadSchema.getNbTables() > 0);
	}

	protected final DBConnection<R> getDBConnection() throws TAPException {
		return (dbConn != null) ? dbConn : (dbConn=service.getFactory().createDBConnection((report!=null)?report.jobID:null));
	}

	public final void closeDBConnection() throws TAPException {
		if (dbConn != null){
			dbConn.close();
			dbConn = null;
		}
	}

	private final void uploadTables() throws TAPException {
		TableLoader[] tables = tapParams.getTableLoaders();
		if (tables.length > 0){
			logger.info("JOB "+report.jobID+"\tLoading uploaded tables ("+tables.length+")...");
			long start = System.currentTimeMillis();
			try{
				uploadSchema = service.getFactory().createUploader(getDBConnection()).upload(tables);
			}finally{
				TAPParameters.deleteUploadedTables(tables);
				report.setDuration(ExecutionProgression.UPLOADING, System.currentTimeMillis()-start);
			}
		}

	}

	private final R executeADQL() throws ParseException, InterruptedException, TranslationException, SQLException, TAPException, UWSException {
		long start;

		tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.PARSING);
		start = System.currentTimeMillis();
		ADQLQuery adql = parseADQL();
		report.setDuration(ExecutionProgression.PARSING, System.currentTimeMillis()-start);

		if (thread.isInterrupted())
			throw new InterruptedException();

		report.resultingColumns = adql.getResultingColumns();

		final int limit = adql.getSelect().getLimit();
		final int maxRec = tapParams.getMaxRec();
		if (maxRec > -1){
			if (limit <= -1 || limit > maxRec)
				adql.getSelect().setLimit(maxRec+1);
		}

		tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.TRANSLATING);
		start = System.currentTimeMillis();
		String sqlQuery = translateADQL(adql);
		report.setDuration(ExecutionProgression.TRANSLATING, System.currentTimeMillis()-start);
		report.sqlTranslation = sqlQuery;

		if (thread.isInterrupted())
			throw new InterruptedException();

		tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.EXECUTING_SQL);
		start = System.currentTimeMillis();
		R result = executeQuery(sqlQuery, adql);
		report.setDuration(ExecutionProgression.EXECUTING_SQL, System.currentTimeMillis()-start);

		return result;
	}

	public final TAPExecutionReport start(final AsyncThread<R> thread) throws TAPException, UWSException, InterruptedException, ParseException, TranslationException, SQLException {
		if (this.thread != null || this.report != null)
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "This ADQLExecutor has already been executed !");

		this.thread = thread;

		TAPJob tapJob = thread.getTAPJob();
		this.tapParams = tapJob.getTapParams();
		this.report = new TAPExecutionReport(tapJob.getJobId(), false, tapParams);
		this.response = null;

		return start();
	}

	public final TAPExecutionReport start(final Thread thread, final String jobId, final TAPParameters params, final HttpServletResponse response) throws TAPException, UWSException, InterruptedException, ParseException, TranslationException, SQLException {
		if (this.thread != null || this.report != null)
			throw new TAPException("This ADQLExecutor has already been executed !");

		this.thread = thread;
		this.tapParams = params;
		this.report = new TAPExecutionReport(jobId, true, tapParams);
		this.response = response;

		return start();
	}

	protected final TAPExecutionReport start() throws TAPException, UWSException, InterruptedException, ParseException, TranslationException, SQLException {
		long start = System.currentTimeMillis();
		try{
			// Upload tables if needed:
			if (tapParams != null && tapParams.getTableLoaders() != null && tapParams.getTableLoaders().length > 0){
				tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.UPLOADING);
				uploadTables();
			}

			if (thread.isInterrupted())
				throw new InterruptedException();

			// Parse, translate in SQL and execute the ADQL query:
			R queryResult = executeADQL();
			if (queryResult == null || thread.isInterrupted())
				throw new InterruptedException();

			// Write the result:
			tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.WRITING_RESULT);
			writeResult(queryResult);

			logger.info("JOB "+report.jobID+" COMPLETED");
			tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.FINISHED);

			report.success = true;

			return report;
		}catch(NullPointerException npe){
			npe.printStackTrace();
			throw npe;
		} finally {
			try {
				dropUploadedTables();
			} catch (TAPException e) { logger.error("JOB "+report.jobID+"\tCan not drop uploaded tables !", e); }
			try {
				closeDBConnection();
			} catch (TAPException e) { logger.error("JOB "+report.jobID+"\tCan not close the DB connection !", e); }
			report.setTotalDuration(System.currentTimeMillis()-start);
			logger.queryFinished(report);
		}
	}

	protected ADQLQuery parseADQL() throws ParseException, InterruptedException, TAPException {
		ADQLQueryFactory queryFactory = service.getFactory().createQueryFactory();
		QueryChecker queryChecker = service.getFactory().createQueryChecker(uploadSchema);
		ADQLParser parser;
		if (queryFactory == null)
			parser = new ADQLParser(queryChecker);
		else
			parser = new ADQLParser(queryChecker, queryFactory);
		parser.setCoordinateSystems(service.getCoordinateSystems());
		parser.setDebug(false);
		//logger.info("Job "+report.jobID+" - 1/5 Parsing ADQL....");
		return parser.parseQuery(tapParams.getQuery());
	}

	protected String translateADQL(ADQLQuery query) throws TranslationException, InterruptedException, TAPException {
		ADQLTranslator translator = service.getFactory().createADQLTranslator();
		//logger.info("Job "+report.jobID+" - 2/5 Translating ADQL...");
		return translator.translate(query);
	}

	protected R executeQuery(String sql, ADQLQuery adql) throws SQLException, InterruptedException, TAPException {
		//logger.info("Job "+report.jobID+" - 3/5 Creating DBConnection....");
		DBConnection<R> dbConn = getDBConnection();
		//logger.info("Job "+report.jobID+" - 4/5 Executing query...\n"+sql);
		final long startTime = System.currentTimeMillis();
		R result = dbConn.executeQuery(sql, adql);
		if (result == null)
			logger.info("JOB "+report.jobID+" - QUERY ABORTED AFTER "+(System.currentTimeMillis()-startTime)+" MS !");
		else
			logger.info("JOB "+report.jobID+" - QUERY SUCCESFULLY EXECUTED IN "+(System.currentTimeMillis()-startTime)+" MS !");
		return result;
	}

	protected OutputFormat<R> getFormatter() throws TAPException {
		// Search for the corresponding formatter:
		String format = tapParams.getFormat();
		OutputFormat<R> formatter = service.getOutputFormat((format == null)?"votable":format);
		if (format != null && formatter == null)
			formatter = service.getOutputFormat("votable");

		// Format the result:
		if (formatter == null)
			throw new TAPException("Impossible to format the query result: no formatter has been found for the given MIME type \""+format+"\" and for the default MIME type \"votable\" (short form) !");

		return formatter;
	}

	protected final void writeResult(R queryResult) throws InterruptedException, TAPException, UWSException {
		OutputFormat<R> formatter = getFormatter();

		// Synchronous case:
		if (response != null){
			long start = System.currentTimeMillis();
			try{
				response.setContentType(formatter.getMimeType());
				writeResult(queryResult, formatter, response.getOutputStream());
			}catch(IOException ioe){
				throw new TAPException("Impossible to get the output stream of the HTTP request to write the result of the job "+report.jobID+" !", ioe);
			}finally{
				report.setDuration(ExecutionProgression.WRITING_RESULT, System.currentTimeMillis()-start);
			}

		}// Asynchronous case:
		else{
			long start = System.currentTimeMillis();
			try{
				JobThread jobThread = (JobThread)thread;
				Result result = jobThread.createResult();
				result.setMimeType(formatter.getMimeType());
				writeResult(queryResult, formatter, jobThread.getResultOutput(result));
				result.setSize(jobThread.getResultSize(result));
				jobThread.publishResult(result);
			}catch(IOException ioe){
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Impossible to get the output stream of the result file to write the result of the job "+report.jobID+" !");
			}finally{
				report.setDuration(ExecutionProgression.WRITING_RESULT, System.currentTimeMillis()-start);
			}
		}
	}

	protected void writeResult(R queryResult, OutputFormat<R> formatter, OutputStream output) throws InterruptedException, TAPException {
		//logger.info("Job "+report.jobID+" - 5/5 Writing result file...");
		formatter.writeResult(queryResult, output, report, thread);
	}

	protected void dropUploadedTables() throws TAPException {
		if (uploadSchema != null){
			// Drop all uploaded tables:
			DBConnection<R> dbConn = getDBConnection();
			for(TAPTable t : uploadSchema){
				try{
					dbConn.dropTable(t);
				}catch(DBException dbe){
					logger.error("JOB "+report.jobID+"\tCan not drop the table \""+t.getDBName()+"\" (in adql \""+t.getADQLName()+"\") from the database !", dbe);
				}
			}
			closeDBConnection();
		}
	}

}
