package tap.formatter;

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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONWriter;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTypes;
import adql.db.DBColumn;

public class ResultSet2JsonFormatter extends JSONFormat<ResultSet> implements ResultSetFormatter {

	public ResultSet2JsonFormatter(ServiceConnection<ResultSet> service, boolean logFormatReport){
		super(service, logFormatReport);
	}

	public ResultSet2JsonFormatter(ServiceConnection<ResultSet> service){
		super(service);
	}

	@Override
	protected DBColumn[] writeMetadata(ResultSet queryResult, JSONWriter out, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException, JSONException{
		out.array();
		DBColumn[] selectedColumns = execReport.resultingColumns;

		try{
			ResultSetMetaData meta = queryResult.getMetaData();
			int indField = 1;
			if (selectedColumns != null){
				for(DBColumn field : selectedColumns){
					TAPColumn tapCol = null;
					try{
						tapCol = (TAPColumn)field;
					}catch(ClassCastException ex){
						tapCol = new TAPColumn(field.getADQLName());
						tapCol.setDatatype(meta.getColumnTypeName(indField), TAPTypes.NO_SIZE);
						service.getLogger().warning("Unknown DB datatype for the field \"" + tapCol.getName() + "\" ! It is supposed to be \"" + tapCol.getDatatype() + "\" (original value: \"" + meta.getColumnTypeName(indField) + "\").");
						selectedColumns[indField - 1] = tapCol;
					}
					writeFieldMeta(tapCol, out);
					indField++;

					if (thread.isInterrupted())
						throw new InterruptedException();
				}
			}
		}catch(SQLException e){
			service.getLogger().error("Job N°" + execReport.jobID + " - Impossible to get the metadata of the given ResultSet !", e);
		}

		out.endArray();
		return selectedColumns;
	}

	@Override
	protected int writeData(ResultSet queryResult, DBColumn[] selectedColumns, JSONWriter out, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException, JSONException{
		out.array();
		int nbRows = 0;
		try{
			int nbColumns = queryResult.getMetaData().getColumnCount();
			while(queryResult.next()){
				if (execReport.parameters.getMaxRec() > 0 && nbRows >= execReport.parameters.getMaxRec()) // that's to say: OVERFLOW !
					break;

				out.array();
				Object value;
				for(int i = 1; i <= nbColumns; i++){
					value = formatValue(queryResult.getObject(i), selectedColumns[i - 1]);
					writeFieldValue(value, selectedColumns[i - 1], out);
					if (thread.isInterrupted())
						throw new InterruptedException();
				}
				out.endArray();
				nbRows++;

				if (thread.isInterrupted())
					throw new InterruptedException();
			}
		}catch(SQLException se){
			throw new TAPException("Job N°" + execReport.jobID + " - Impossible to get the " + (nbRows + 1) + "-th rows from the given ResultSet !", se);
		}

		out.endArray();
		return nbRows;
	}

	@Override
	public Object formatValue(Object value, DBColumn colMeta){
		return value;
	}

}
