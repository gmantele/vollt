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
 * Copyright 2012,2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import uws.UWSException;

/**
 * <p>Any exception that occurred while a TAP service activity.</p>
 * 
 * <p>Most of the time this exception wraps another exception (e.g. {@link UWSException}).</p>
 * 
 * <p>It contains an HTTP status code, set by default to HTTP-500 (Internal Server Error).</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (04/2015)
 */
public class TAPException extends Exception {

	private static final long serialVersionUID = 1L;

	/** An ADQL query which were executed when the error occurred. */
	private String adqlQuery = null;

	/** The ADQL query execution status (e.g. uploading, parsing, executing) just when the error occurred.  */
	private ExecutionProgression executionStatus = null;

	/** The HTTP status code to set in the HTTP servlet response if the exception reaches the servlet. */
	private int httpErrorCode = UWSException.INTERNAL_SERVER_ERROR;

	/**
	 * Standard TAP exception: no ADQL query or execution status specified.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param message	Message explaining the error.
	 */
	public TAPException(String message){
		super(message);
	}

	/**
	 * Standard TAP exception: no ADQL query or execution status specified.
	 * The corresponding HTTP status code is set by the second parameter.
	 * 
	 * @param message		Message explaining the error.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 */
	public TAPException(String message, int httpErrorCode){
		super(message);
		this.httpErrorCode = httpErrorCode;
	}

	/**
	 * TAP exception with the ADQL query which were executed when the error occurred.
	 * No execution status specified.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param message	Message explaining the error.
	 * @param query		The ADQL query which were executed when the error occurred.
	 */
	public TAPException(String message, String query){
		super(message);
		adqlQuery = query;
	}

	/**
	 * TAP exception with the ADQL query which were executed when the error occurred.
	 * No execution status specified.
	 * The corresponding HTTP status code is set by the second parameter.
	 * 
	 * @param message		Message explaining the error.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 * @param query			The ADQL query which were executed when the error occurred.
	 */
	public TAPException(String message, int httpErrorCode, String query){
		this(message, httpErrorCode);
		adqlQuery = query;
	}

	/**
	 * TAP exception with the ADQL query which were executed when the error occurred,
	 * AND with its execution status (e.g. uploading, parsing, executing, ...).
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param message	Message explaining the error.
	 * @param query		The ADQL query which were executed when the error occurred.
	 * @param status	Execution status/phase of the given ADQL query when the error occurred. 
	 */
	public TAPException(String message, String query, ExecutionProgression status){
		this(message, query);
		executionStatus = status;
	}

	/**
	 * TAP exception with the ADQL query which were executed when the error occurred,
	 * AND with its execution status (e.g. uploading, parsing, executing, ...).
	 * The corresponding HTTP status code is set by the second parameter.
	 * 
	 * @param message		Message explaining the error.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 * @param query			The ADQL query which were executed when the error occurred.
	 * @param status		Execution status/phase of the given ADQL query when the error occurred. 
	 */
	public TAPException(String message, int httpErrorCode, String query, ExecutionProgression status){
		this(message, httpErrorCode, query);
		executionStatus = status;
	}

	/**
	 * <p>TAP exception wrapping the given {@link UWSException}.</p>
	 * 
	 * <p>The message of this TAP exception will be exactly the same as the one of the given exception.</p>
	 * 
	 * <p>
	 * 	Besides, the cause of this TAP exception will be the cause of the given exception ONLY if it has one ;
	 * 	otherwise it will the given exception.
	 * </p>
	 * 
	 * <p>The HTTP status code will be the same as the one of the given {@link UWSException}.</p>
	 * 
	 * @param ue	The exception to wrap.
	 */
	public TAPException(UWSException ue){
		this(ue.getMessage(), (ue.getCause() == null ? ue : ue.getCause()), ue.getHttpErrorCode());
	}

	/**
	 * <p>TAP exception wrapping the given {@link UWSException}.</p>
	 * 
	 * <p>The message of this TAP exception will be exactly the same as the one of the given exception.</p>
	 * 
	 * <p>
	 * 	Besides, the cause of this TAP exception will be the cause of the given exception ONLY if it has one ;
	 * 	otherwise it will the given exception.
	 * </p>
	 * 
	 * <p>The HTTP status code will be the one given in second parameter.</p>
	 * 
	 * @param cause			The exception to wrap.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 */
	public TAPException(UWSException cause, int httpErrorCode){
		this(cause);
		this.httpErrorCode = httpErrorCode;
	}

	/**
	 * <p>TAP exception wrapping the given {@link UWSException} and storing the current ADQL query execution status.</p>
	 * 
	 * <p>The message of this TAP exception will be exactly the same as the one of the given exception.</p>
	 * 
	 * <p>
	 * 	Besides, the cause of this TAP exception will be the cause of the given exception ONLY if it has one ;
	 * 	otherwise it will the given exception.
	 * </p>
	 * 
	 * <p>The HTTP status code will be the one given in second parameter.</p>
	 * 
	 * @param cause			The exception to wrap.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 * @param status		Execution status/phase of the given ADQL query when the error occurred. 
	 */
	public TAPException(UWSException cause, int httpErrorCode, ExecutionProgression status){
		this(cause, httpErrorCode);
		this.executionStatus = status;
	}

	/**
	 * Build a {@link TAPException} with the given cause. The built exception will have NO MESSAGE.
	 * No execution status specified.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param cause	The cause of this exception.
	 */
	public TAPException(Throwable cause){
		super(cause);
	}

	/**
	 * Build a {@link TAPException} with the given cause. The built exception will have NO MESSAGE.
	 * No execution status specified.
	 * The corresponding HTTP status code is set by the second parameter.
	 * 
	 * @param cause			The cause of this exception.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 */
	public TAPException(Throwable cause, int httpErrorCode){
		super(cause);
		this.httpErrorCode = httpErrorCode;
	}

	/**
	 * Build a {@link TAPException} with the given cause AND with the ADQL query which were executed when the error occurred.
	 * The built exception will have NO MESSAGE.
	 * No execution status specified.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param cause		The cause of this exception.
	 * @param query		The ADQL query which were executed when the error occurred.
	 */
	public TAPException(Throwable cause, String query){
		super(cause);
		adqlQuery = query;
	}

	/**
	 * Build a {@link TAPException} with the given cause AND with the ADQL query which were executed when the error occurred.
	 * The built exception will have NO MESSAGE.
	 * No execution status specified.
	 * The corresponding HTTP status code is set by the second parameter.
	 * 
	 * @param cause			The cause of this exception.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 * @param query			The ADQL query which were executed when the error occurred.
	 */
	public TAPException(Throwable cause, int httpErrorCode, String query){
		this(cause, httpErrorCode);
		adqlQuery = query;
	}

	/**
	 * Build a {@link TAPException} with the given cause AND with the ADQL query which were executed when the error occurred
	 * AND with its execution status (e.g. uploading, parsing, executing, ...).
	 * The built exception will have NO MESSAGE.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param cause		The cause of this exception.
	 * @param query		The ADQL query which were executed when the error occurred.
	 * @param status	Execution status/phase of the given ADQL query when the error occurred. 
	 */
	public TAPException(Throwable cause, String query, ExecutionProgression status){
		this(cause, query);
		executionStatus = status;
	}

	/**
	 * Build a {@link TAPException} with the given cause AND with the ADQL query which were executed when the error occurred
	 * AND with its execution status (e.g. uploading, parsing, executing, ...).
	 * The built exception will have NO MESSAGE.
	 * The corresponding HTTP status code is set by the second parameter.
	 * 
	 * @param cause			The cause of this exception.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 * @param query			The ADQL query which were executed when the error occurred.
	 * @param status		Execution status/phase of the given ADQL query when the error occurred. 
	 */
	public TAPException(Throwable cause, int httpErrorCode, String query, ExecutionProgression status){
		this(cause, httpErrorCode, query);
		executionStatus = status;
	}

	/**
	 * Build a {@link TAPException} with the given message and cause.
	 * No execution status specified.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param message	Message of this exception.
	 * @param cause		The cause of this exception.
	 */
	public TAPException(String message, Throwable cause){
		super(message, cause);
	}

	/**
	 * Build a {@link TAPException} with the given message and cause.
	 * No execution status specified.
	 * The corresponding HTTP status code is set by the third parameter.
	 * 
	 * @param message		Message of this exception.
	 * @param cause			The cause of this exception.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 */
	public TAPException(String message, Throwable cause, int httpErrorCode){
		super(message, cause);
		this.httpErrorCode = httpErrorCode;
	}

	/**
	 * Build a {@link TAPException} with the given message and cause,
	 * AND with the ADQL query which were executed when the error occurred.
	 * No execution status specified.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param message		Message of this exception.
	 * @param cause			The cause of this exception.
	 * @param query			The ADQL query which were executed when the error occurred.
	 */
	public TAPException(String message, Throwable cause, String query){
		super(message, cause);
		adqlQuery = query;
	}

	/**
	 * Build a {@link TAPException} with the given message and cause,
	 * AND with the ADQL query which were executed when the error occurred.
	 * No execution status specified.
	 * The corresponding HTTP status code is set by the third parameter.
	 * 
	 * @param message		Message of this exception.
	 * @param cause			The cause of this exception.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 * @param query			The ADQL query which were executed when the error occurred.
	 */
	public TAPException(String message, Throwable cause, int httpErrorCode, String query){
		this(message, cause, httpErrorCode);
		adqlQuery = query;
	}

	/**
	 * Build a {@link TAPException} with the given message and cause,
	 * AND with the ADQL query which were executed when the error occurred
	 * AND with its execution status (e.g. uploading, parsing, executing, ...).
	 * No execution status specified.
	 * The corresponding HTTP status code will be HTTP-500 (Internal Server Error).
	 * 
	 * @param message		Message of this exception.
	 * @param cause			The cause of this exception.
	 * @param query			The ADQL query which were executed when the error occurred.
	 * @param status		Execution status/phase of the given ADQL query when the error occurred. 
	 */
	public TAPException(String message, Throwable cause, String query, ExecutionProgression status){
		this(message, cause, query);
		executionStatus = status;
	}

	/**
	 * Build a {@link TAPException} with the given message and cause,
	 * AND with the ADQL query which were executed when the error occurred
	 * AND with its execution status (e.g. uploading, parsing, executing, ...).
	 * No execution status specified.
	 * The corresponding HTTP status code is set by the third parameter.
	 * 
	 * @param message		Message of this exception.
	 * @param cause			The cause of this exception.
	 * @param httpErrorCode	HTTP response status code. <i>(if &le; 0, 500 will be set by default)</i>
	 * @param query			The ADQL query which were executed when the error occurred.
	 * @param status		Execution status/phase of the given ADQL query when the error occurred. 
	 */
	public TAPException(String message, Throwable cause, int httpErrorCode, String query, ExecutionProgression status){
		this(message, cause, httpErrorCode, query);
		executionStatus = status;
	}

	/**
	 * <p>Get the HTTP status code to set in the HTTP response.</p>
	 * 
	 * <p><i>If the set value is &le; 0, 500 will be returned instead.</i></p>
	 * 
	 * @return	The HTTP response status code.
	 */
	public int getHttpErrorCode(){
		return (httpErrorCode <= 0) ? UWSException.INTERNAL_SERVER_ERROR : httpErrorCode;
	}

	/**
	 * Get the ADQL query which were executed when the error occurred.
	 * 
	 * @return	Executed ADQL query.
	 */
	public String getQuery(){
		return adqlQuery;
	}

	/**
	 * Get the execution status/phase of an ADQL query when the error occurred.
	 * 
	 * @return	ADQL query execution status.
	 */
	public ExecutionProgression getExecutionStatus(){
		return executionStatus;
	}

}
