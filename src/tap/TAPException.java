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

import uws.UWSException;

public class TAPException extends Exception {

	private static final long serialVersionUID = 1L;

	private String adqlQuery = null;
	private ExecutionProgression executionStatus = null;

	private int httpErrorCode = UWSException.INTERNAL_SERVER_ERROR;

	public TAPException(String message) {
		super(message);
	}

	public TAPException(String message, int httpErrorCode) {
		super(message);
		this.httpErrorCode = httpErrorCode;
	}

	public TAPException(String message, String query) {
		super(message);
		adqlQuery = query;
	}

	public TAPException(String message, int httpErrorCode, String query) {
		this(message, httpErrorCode);
		adqlQuery = query;
	}

	public TAPException(String message, String query, ExecutionProgression status) {
		this(message, query);
		executionStatus = status;
	}

	public TAPException(String message, int httpErrorCode, String query, ExecutionProgression status) {
		this(message, httpErrorCode, query);
		executionStatus = status;
	}

	public TAPException(UWSException ue){
		this(ue.getMessage(), ue.getCause(), ue.getHttpErrorCode());
	}

	public TAPException(UWSException cause, int httpErrorCode) {
		this(cause);
		this.httpErrorCode = httpErrorCode;
	}

	public TAPException(UWSException cause, int httpErrorCode, ExecutionProgression status) {
		this(cause, httpErrorCode);
		this.executionStatus = status;
	}

	public TAPException(Throwable cause) {
		super(cause);
	}

	public TAPException(Throwable cause, int httpErrorCode) {
		super(cause);
		this.httpErrorCode = httpErrorCode;
	}

	public TAPException(Throwable cause, String query) {
		super(cause);
		adqlQuery = query;
	}

	public TAPException(Throwable cause, int httpErrorCode, String query) {
		this(cause, httpErrorCode);
		adqlQuery = query;
	}

	public TAPException(Throwable cause, String query, ExecutionProgression status) {
		this(cause, query);
		executionStatus = status;
	}

	public TAPException(Throwable cause, int httpErrorCode, String query, ExecutionProgression status) {
		this(cause, httpErrorCode, query);
		executionStatus = status;
	}

	public TAPException(String message, Throwable cause) {
		super(message, cause);
	}

	public TAPException(String message, Throwable cause, int httpErrorCode) {
		super(message, cause);
		this.httpErrorCode = httpErrorCode;
	}

	public TAPException(String message, Throwable cause, String query) {
		super(message, cause);
		adqlQuery = query;
	}

	public TAPException(String message, Throwable cause, int httpErrorCode, String query) {
		this(message, cause, httpErrorCode);
		adqlQuery = query;
	}

	public TAPException(String message, Throwable cause, String query, ExecutionProgression status) {
		this(message, cause, query);
		executionStatus = status;
	}

	public TAPException(String message, Throwable cause, int httpErrorCode, String query, ExecutionProgression status) {
		this(message, cause, httpErrorCode, query);
		executionStatus = status;
	}

	public int getHttpErrorCode(){
		return httpErrorCode;
	}

	public String getQuery(){
		return adqlQuery;
	}

	public ExecutionProgression getExecutionStatus(){
		return executionStatus;
	}

}
