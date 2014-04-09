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
 * Copyright 2012-2013 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institute (ARI)
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import adql.db.DBColumn;

/**
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI) - gmantele@ari.uni-heidelberg.de
 * @version 1.1 (12/2013)
 */
public class ResultSet2SVFormatter extends SVFormat<ResultSet> implements ResultSetFormatter {

	/**
	 * @since 1.1
	 */
	public ResultSet2SVFormatter(final ServiceConnection<ResultSet> service, char colSeparator){
		super(service, colSeparator);
	}

	public ResultSet2SVFormatter(final ServiceConnection<ResultSet> service, char colSeparator, boolean delimitStrings){
		super(service, colSeparator, delimitStrings);
	}

	/**
	 * @since 1.1
	 */
	public ResultSet2SVFormatter(final ServiceConnection<ResultSet> service, char colSeparator, boolean delimitStrings, String mimeType, String typeAlias){
		super(service, colSeparator, delimitStrings, mimeType, typeAlias);
	}

	public ResultSet2SVFormatter(final ServiceConnection<ResultSet> service, String colSeparator){
		super(service, colSeparator);
	}

	public ResultSet2SVFormatter(final ServiceConnection<ResultSet> service, String colSeparator, boolean delimitStrings){
		super(service, colSeparator, delimitStrings);
	}

	/**
	 * @since 1.1
	 */
	public ResultSet2SVFormatter(final ServiceConnection<ResultSet> service, String colSeparator, boolean delimitStrings, String mimeType, String typeAlias){
		super(service, colSeparator, delimitStrings, mimeType, typeAlias);
	}

	@Override
	protected DBColumn[] writeMetadata(ResultSet queryResult, PrintWriter writer, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException{
		DBColumn[] selectedColumns = execReport.resultingColumns;
		int nbColumns = (selectedColumns == null) ? -1 : selectedColumns.length;
		if (nbColumns > 0){
			for(int i = 0; i < nbColumns - 1; i++){
				writer.print(selectedColumns[i].getADQLName());
				writer.print(separator);
			}
			writer.print(selectedColumns[nbColumns - 1].getADQLName());
			writer.println();
			writer.flush();
		}
		return selectedColumns;
	}

	@Override
	protected int writeData(ResultSet queryResult, DBColumn[] selectedColumns, PrintWriter writer, TAPExecutionReport execReport, Thread thread) throws IOException, TAPException, InterruptedException{
		int nbRows = 0;
		try{
			int nbColumns = queryResult.getMetaData().getColumnCount();
			while(queryResult.next()){
				if (execReport.parameters.getMaxRec() > 0 && nbRows >= execReport.parameters.getMaxRec()) // that's to say: OVERFLOW !
					break;

				Object value;
				for(int i = 1; i <= nbColumns; i++){
					value = formatValue(queryResult.getObject(i), selectedColumns[i - 1]);
					writeFieldValue(value, selectedColumns[i - 1], writer);
					if (i != nbColumns)
						writer.print(separator);
					if (thread.isInterrupted())
						throw new InterruptedException();
				}
				writer.println();
				nbRows++;

				if (thread.isInterrupted())
					throw new InterruptedException();
			}
			writer.flush();
		}catch(SQLException se){
			throw new TAPException("Job N°" + execReport.jobID + " - Impossible to get the " + (nbRows + 1) + "-th rows from the given ResultSet !", se);
		}

		return nbRows;
	}

	@Override
	public Object formatValue(Object value, DBColumn colMeta){
		return value;
	}

}
