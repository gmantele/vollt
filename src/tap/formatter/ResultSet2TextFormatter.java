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

import java.sql.ResultSet;
import java.sql.SQLException;

import adql.db.DBColumn;

import cds.util.AsciiTable;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;

public class ResultSet2TextFormatter extends TextFormat<ResultSet> implements ResultSetFormatter {

	public ResultSet2TextFormatter(ServiceConnection<ResultSet> service) {
		super(service);
	}

	@Override
	protected String getHeader(ResultSet queryResult, TAPExecutionReport execReport, Thread thread) throws TAPException {
		DBColumn[] selectedColumns = execReport.resultingColumns;
		StringBuffer line = new StringBuffer();
		int nbColumns = (selectedColumns == null) ? -1 : selectedColumns.length;
		if (nbColumns > 0){
			for(int i=0; i<nbColumns-1; i++)
				line.append(selectedColumns[i].getADQLName()).append('|');
			line.append(selectedColumns[nbColumns-1].getADQLName());
		}
		return line.toString();
	}

	@Override
	protected int writeData(ResultSet queryResult, AsciiTable asciiTable, TAPExecutionReport execReport, Thread thread) throws TAPException {
		int nbRows = 0;
		try{
			DBColumn[] selectedColumns = execReport.resultingColumns;
			int nbColumns = selectedColumns.length;
			StringBuffer line = new StringBuffer();
			while(queryResult.next()){
				if (execReport.parameters.getMaxRec() > 0 && nbRows >= execReport.parameters.getMaxRec()) // that's to say: OVERFLOW !
					break;

				line.delete(0, line.length());
				Object value;
				for(int i=1; i<=nbColumns; i++){
					value = formatValue(queryResult.getObject(i), selectedColumns[i-1]);
					writeFieldValue(value, selectedColumns[i-1], line);
					if (i != nbColumns)
						line.append('|');
				}
				asciiTable.addLine(line.toString());
				nbRows++;
			}
		}catch(SQLException se){
			throw new TAPException("Job N°"+execReport.jobID+" - Impossible to get the "+(nbRows+1)+"-th rows from the given ResultSet !", se);
		}
		return nbRows;
	}

	@Override
	public Object formatValue(Object value, DBColumn colMeta) {
		return value;
	}

}
