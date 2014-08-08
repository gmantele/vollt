package tap.log;

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

import java.io.OutputStream;
import java.io.PrintWriter;

import tap.TAPExecutionReport;
import tap.db.DBConnection;
import tap.file.TAPFileManager;
import tap.metadata.TAPMetadata;
import tap.metadata.TAPTable;
import uws.service.log.DefaultUWSLog;

/**
 * Default implementation of the {@link TAPLog} interface which lets logging any message about a TAP service.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (08/2014)
 * 
 * @see DefaultUWSLog
 */
public class DefaultTAPLog extends DefaultUWSLog implements TAPLog {

	public DefaultTAPLog(TAPFileManager fm){
		super(fm);
	}

	public DefaultTAPLog(OutputStream output){
		super(output);
	}

	public DefaultTAPLog(PrintWriter writer){
		super(writer);
	}

	@Override
	public void queryFinished(final TAPExecutionReport report){
		StringBuffer buffer = new StringBuffer("QUERY END FOR " + report.jobID + "");
		buffer.append(" - success ? ").append(report.success);
		buffer.append(" - synchronous ? ").append(report.synchronous);
		buffer.append(" - total duration = ").append(report.getTotalDuration()).append("ms");
		buffer.append(" => upload=").append(report.getUploadDuration()).append("ms");
		buffer.append(", parsing=").append(report.getParsingDuration()).append("ms");
		buffer.append(", execution=").append(report.getExecutionDuration()).append("ms");
		buffer.append(", formatting[").append(report.parameters.getFormat()).append("]=").append(report.getFormattingDuration()).append("ms");
		info(buffer.toString());
	}

	public void dbActivity(final String message){
		dbActivity(message, null);
	}

	public void dbActivity(final String message, final Throwable t){
		String msgType = (t == null) ? "[INFO] " : "[ERROR] ";
		log(DBConnection.LOG_TYPE_DB_ACTIVITY, ((message == null) ? null : (msgType + message)), t);
	}

	@Override
	public void dbInfo(final String message){
		dbActivity(message);
	}

	@Override
	public void dbError(final String message, final Throwable t){
		dbActivity(message, t);
	}

	@Override
	public void tapMetadataFetched(TAPMetadata metadata){
		dbActivity("TAP metadata fetched from the database !");
	}

	@Override
	public void tapMetadataLoaded(TAPMetadata metadata){
		dbActivity("TAP metadata loaded into the database !");
	}

	@Override
	public void connectionOpened(DBConnection connection, String dbName){
		dbActivity("A connection has been opened to the database \"" + dbName + "\" !");
	}

	@Override
	public void connectionClosed(DBConnection connection){
		dbActivity("A database connection has been closed !");
	}

	@Override
	public void transactionStarted(final DBConnection connection){
		dbActivity("A transaction has been started !");
	}

	@Override
	public void transactionCancelled(final DBConnection connection){
		dbActivity("A transaction has been cancelled !");
	}

	@Override
	public void transactionEnded(final DBConnection connection){
		dbActivity("A transaction has been ended/commited !");
	}

	@Override
	public void schemaCreated(final DBConnection connection, String schema){
		dbActivity("CREATE SCHEMA \"" + schema + "\"\t" + connection.getID());
	}

	@Override
	public void schemaDropped(final DBConnection connection, String schema){
		dbActivity("DROP SCHEMA \"" + schema + "\"\t" + connection.getID());
	}

	protected final String getFullDBName(final TAPTable table){
		return ((table.getSchema() != null) ? (table.getSchema().getDBName() + ".") : "") + table.getDBName();
	}

	@Override
	public void tableCreated(final DBConnection connection, TAPTable table){
		dbActivity("CREATE TABLE \"" + getFullDBName(table) + "\" (ADQL name: \"" + table.getFullName() + "\")\t" + connection.getID());
	}

	@Override
	public void tableDropped(final DBConnection connection, TAPTable table){
		dbActivity("DROP TABLE \"" + getFullDBName(table) + "\" (ADQL name: \"" + table.getFullName() + "\")\t" + connection.getID());
	}

	@Override
	public void rowsInserted(final DBConnection connection, TAPTable table, int nbInsertedRows){
		dbActivity("INSERT ROWS (" + ((nbInsertedRows > 0) ? nbInsertedRows : "???") + ") into \"" + getFullDBName(table) + "\" (ADQL name: \"" + table.getFullName() + "\")\t" + connection.getID());
	}

	@Override
	public void sqlQueryExecuting(final DBConnection connection, String sql){
		dbActivity("EXECUTING SQL QUERY \t" + connection.getID() + "\n" + ((sql == null) ? "???" : sql.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll("\r", "")));
	}

	@Override
	public void sqlQueryError(final DBConnection connection, String sql, Throwable t){
		dbActivity("EXECUTION ERROR\t" + connection.getID(), t);
	}

	@Override
	public void sqlQueryExecuted(final DBConnection connection, String sql){
		dbActivity("SUCCESSFULL END OF EXECUTION\t" + connection.getID());
	}

}
