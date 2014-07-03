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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institute (ARI)
 */

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import tap.data.TableIterator;
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
import adql.parser.ADQLParser;
import adql.parser.ADQLQueryFactory;
import adql.parser.ParseException;
import adql.parser.QueryChecker;
import adql.query.ADQLQuery;
import adql.translator.TranslationException;

/**
 * 
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2014)
 */
public class ADQLExecutor {

	protected final ServiceConnection service;
	protected final TAPLog logger;

	protected Thread thread;
	protected TAPParameters tapParams;
	protected HttpServletResponse response;
	protected TAPExecutionReport report;

	private DBConnection dbConn = null;
	protected TAPSchema uploadSchema = null;

	public ADQLExecutor(final ServiceConnection service){
		this.service = service;
		this.logger = service.getLogger();
	}

	public final TAPLog getLogger(){
		return logger;
	}

	public final TAPExecutionReport getExecReport(){
		return report;
	}

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

	public final TAPExecutionReport start(final AsyncThread thread) throws UWSException, InterruptedException{
		if (this.thread != null || this.report != null)
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "This ADQLExecutor has already been executed !");

		this.thread = thread;

		TAPJob tapJob = thread.getTAPJob();
		this.tapParams = tapJob.getTapParams();
		this.report = new TAPExecutionReport(tapJob.getJobId(), false, tapParams);
		this.response = null;

		return start();
	}

	public final TAPExecutionReport start(final Thread thread, final String jobId, final TAPParameters params, final HttpServletResponse response) throws TAPException, InterruptedException{
		if (this.thread != null || this.report != null)
			throw new TAPException("This ADQLExecutor has already been executed !");

		this.thread = thread;
		this.tapParams = params;
		this.report = new TAPExecutionReport(jobId, true, tapParams);
		this.response = response;

		return start();
	}

	protected final TAPExecutionReport start() throws TAPException, UWSException, InterruptedException, ParseException, TranslationException, SQLException{
		long start = System.currentTimeMillis();
		try{
			// Get a "database" connection:
			dbConn = service.getFactory().createDBConnection(report.jobID);

			// 1. UPLOAD TABLES, if needed:
			if (tapParams != null && tapParams.getTableLoaders() != null && tapParams.getTableLoaders().length > 0){
				tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.UPLOADING);
				uploadTables();
			}

			if (thread.isInterrupted())
				throw new InterruptedException();

			// 2. PARSE THE ADQL QUERY:
			tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.PARSING);
			ADQLQuery adqlQuery = parseADQL();

			if (adqlQuery == null || thread.isInterrupted())
				throw new InterruptedException();

			// 3. EXECUTE THE ADQL QUERY:
			tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.EXECUTING_ADQL);
			TableIterator queryResult = executeADQL(adqlQuery);

			// 4. WRITE RESULT:
			tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.WRITING_RESULT);
			writeResult(queryResult);

			// Report the COMPLETED status:
			logger.info("JOB " + report.jobID + " COMPLETED");
			tapParams.set(TAPJob.PARAM_PROGRESSION, ExecutionProgression.FINISHED);
			report.success = true;

			return report;
		}catch(NullPointerException npe){
			npe.printStackTrace();
			throw npe;
		}finally{
			try{
				dropUploadedTables();
			}catch(TAPException e){
				logger.error("JOB " + report.jobID + "\tCan not drop uploaded tables !", e);
			}
			try{
				if (dbConn != null){
					dbConn.close();
					dbConn = null;
				}
			}catch(TAPException e){
				logger.error("JOB " + report.jobID + "\tCan not close the DB connection !", e);
			}
			report.setTotalDuration(System.currentTimeMillis() - start);
			logger.queryFinished(report);
		}
	}

	private final void uploadTables() throws TAPException{
		// Fetch the tables to upload:
		TableLoader[] tables = tapParams.getTableLoaders();

		// Upload them, if needed:
		if (tables.length > 0){
			logger.info("JOB " + report.jobID + "\tLoading uploaded tables (" + tables.length + ")...");
			long start = System.currentTimeMillis();
			try{
				/* TODO Problem with the DBConnection! One is created here for the Uploader (and dbConn is set) and closed by its uploadTables function (but dbConn is not set to null).
				 * Ideally, the connection should not be close, or at least dbConn should be set to null just after. */
				uploadSchema = service.getFactory().createUploader(dbConn).upload(tables);
			}finally{
				TAPParameters.deleteUploadedTables(tables);
				report.setDuration(ExecutionProgression.UPLOADING, System.currentTimeMillis() - start);
			}
		}
	}

	protected ADQLQuery parseADQL() throws ParseException, InterruptedException, TAPException{
		long start = System.currentTimeMillis();
		ADQLQueryFactory queryFactory = service.getFactory().createQueryFactory();
		QueryChecker queryChecker = service.getFactory().createQueryChecker(uploadSchema);
		ADQLParser parser;
		if (queryFactory == null)
			parser = new ADQLParser(queryChecker);
		else
			parser = new ADQLParser(queryChecker, queryFactory);
		parser.setCoordinateSystems(service.getCoordinateSystems());
		parser.setDebug(false);
		ADQLQuery query = parser.parseQuery(tapParams.getQuery());
		final int limit = query.getSelect().getLimit();
		final Integer maxRec = tapParams.getMaxRec();
		if (maxRec != null && maxRec > -1){
			if (limit <= -1 || limit > maxRec)
				query.getSelect().setLimit(maxRec + 1);
		}
		report.setDuration(ExecutionProgression.PARSING, System.currentTimeMillis() - start);
		report.resultingColumns = query.getResultingColumns();
		return query;
	}

	protected TableIterator executeADQL(ADQLQuery adql) throws SQLException, InterruptedException, TAPException{
		final long startTime = System.currentTimeMillis();
		TableIterator result = dbConn.executeQuery(adql);
		if (result == null)
			logger.info("JOB " + report.jobID + " - QUERY ABORTED AFTER " + (System.currentTimeMillis() - startTime) + " MS !");
		else
			logger.info("JOB " + report.jobID + " - QUERY SUCCESFULLY EXECUTED IN " + (System.currentTimeMillis() - startTime) + " MS !");
		return result;
	}

	protected final void writeResult(TableIterator queryResult) throws InterruptedException, TAPException, UWSException{
		OutputFormat formatter = getFormatter();

		// Synchronous case:
		if (response != null){
			long start = System.currentTimeMillis();
			try{
				response.setContentType(formatter.getMimeType());
				writeResult(queryResult, formatter, response.getOutputStream());
			}catch(IOException ioe){
				throw new TAPException("Impossible to get the output stream of the HTTP request to write the result of the job " + report.jobID + " !", ioe);
			}finally{
				report.setDuration(ExecutionProgression.WRITING_RESULT, System.currentTimeMillis() - start);
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
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Impossible to get the output stream of the result file to write the result of the job " + report.jobID + " !");
			}finally{
				report.setDuration(ExecutionProgression.WRITING_RESULT, System.currentTimeMillis() - start);
			}
		}
	}

	protected void writeResult(TableIterator queryResult, OutputFormat formatter, OutputStream output) throws InterruptedException, TAPException{
		//logger.info("Job "+report.jobID+" - 5/5 Writing result file...");
		formatter.writeResult(queryResult, output, report, thread);
	}

	protected void dropUploadedTables() throws TAPException{
		if (uploadSchema != null){
			// Drop all uploaded tables:
			for(TAPTable t : uploadSchema){
				try{
					dbConn.dropUploadedTable(t.getDBName());
				}catch(DBException dbe){
					logger.error("JOB " + report.jobID + "\tCan not drop the table \"" + t.getDBName() + "\" (in adql \"" + t.getADQLName() + "\") from the database !", dbe);
				}
			}
		}
	}

}
