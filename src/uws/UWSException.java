package uws;

/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import uws.job.ErrorType;

/**
 * <p>Any exception returned by a class of the UWS pattern may be associated with
 * an HTTP error code (like: 404, 303, 500) and a UWS error type.</p>
 * 
 * <p>
 * 	Any error reported with this kind of exception will (in the most of cases) interrupt a UWS action,
 * 	by reporting an error related with the UWS usage.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (09/2014)
 */
public class UWSException extends Exception {
	private static final long serialVersionUID = 1L;

	// SUCCESS codes:
	public final static int OK = 200;
	public final static int ACCEPTED_BUT_NOT_COMPLETE = 202;
	public final static int NO_CONTENT = 204;

	// REDIRECTION codes:
	public final static int SEE_OTHER = 303;

	// CLIENT ERROR codes:
	public final static int BAD_REQUEST = 400;
	public final static int FORBIDDEN = 403;
	public final static int NOT_FOUND = 404;
	public final static int NOT_ALLOWED = 405;
	public final static int REQUEST_ENTITY_TOO_LARGE = 413;

	// SERVER ERROR codes:
	public final static int INTERNAL_SERVER_ERROR = 500;
	public final static int NOT_IMPLEMENTED = 501;
	public final static int SERVICE_UNAVAILABLE = 503;
	public final static int USER_ACCESS_DENIED = 530;
	public final static int PERMISSION_DENIED = 550;

	/** The HTTP error code <i>(by default {@link UWSException#NOT_FOUND NOT_FOUND})</i>. It MUST BE greater than 0. */
	protected int httpErrorCode = NOT_FOUND;

	/** The UWS error type <i>(by default {@link ErrorType#FATAL FATAL})</i>. It MUST BE non <i>null</i>. */
	protected ErrorType errorType = ErrorType.FATAL;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Exception in the general context of UWS.
	 * 
	 * @param msg	Error message to display.
	 */
	public UWSException(String msg){
		this(msg, null);
	}

	/**
	 * Exception that occurs in the general context of UWS, and with the specified error type (FATAL or TRANSIENT). 
	 * 
	 * @param msg		Error message to display.
	 * @param type		Type of the error (FATAL or TRANSIENT). <i>Note: If NULL, it will be considered as FATAL.</i>
	 */
	public UWSException(String msg, ErrorType type){
		super(msg);
		this.errorType = (type == null) ? ErrorType.FATAL : type;
	}

	/**
	 * Exception that occurs in the general context of UWS because the given exception has been thrown.
	 * 
	 * @param t	The thrown (and so caught) exception.
	 */
	public UWSException(Throwable t){
		this(t, null);
	}

	/**
	 * Exception with the given type that occurs in the general context of UWS
	 * because the given exception has been thrown.
	 * 
	 * @param t			The thrown (and so caught) exception.
	 * @param type		Type of the error (FATAL or TRANSIENT). <i>Note: If NULL, it will be considered as FATAL.</i>
	 */
	public UWSException(Throwable t, ErrorType type){
		super(t);
		this.errorType = (type == null) ? ErrorType.FATAL : type;
	}

	/**
	 * Exception that occurs in the general context of UWS and which should return the given HTTP error code.
	 * 
	 * @param httpError	HTTP error code to return.
	 * @param msg		Error message to display.
	 */
	public UWSException(int httpError, String msg){
		this(httpError, msg, null);
	}

	/**
	 * Exception that occurs in the general context of UWS, with the given type and which should return the given HTTP error code.
	 * 
	 * @param httpError	HTTP error code to return.
	 * @param msg		Error message to display.
	 * @param type		Type of the error (FATAL or TRANSIENT). <i>Note: If NULL, it will be considered as FATAL.</i>
	 */
	public UWSException(int httpError, String msg, ErrorType type){
		this(msg, type);
		this.httpErrorCode = (httpError < 0) ? NOT_FOUND : httpError;
	}

	/**
	 * Exception that occurs in the general context of UWS,
	 * because the given exception has been thrown and that which should return the given HTTP error status.
	 * 
	 * @param httpError	HTTP error code to return.
	 * @param t			The thrown (and so caught) exception.
	 */
	public UWSException(int httpError, Throwable t){
		this(httpError, t, null, null);
	}

	/**
	 * Exception that occurs in the general context of UWS with the given error type,
	 * because the given exception has been thrown and that which should return the given HTTP error status.
	 * 
	 * @param httpError	HTTP error code to return.
	 * @param t			The thrown (and so caught) exception.
	 * @param type		Type of the error (FATAL or TRANSIENT). <i>Note: If NULL, it will be considered as FATAL.</i>
	 */
	public UWSException(int httpError, Throwable t, ErrorType type){
		this(httpError, t, null, type);
	}

	/**
	 * Exception that occurs in the general context of UWS,
	 * because the given exception has been thrown and that which should return the given HTTP error status.
	 * 
	 * @param httpError	HTTP error code to return.
	 * @param t			The thrown (and so caught) exception.
	 * @param msg		Error message to display.
	 */
	public UWSException(int httpError, Throwable t, String msg){
		this(httpError, t, msg, null);
	}

	/**
	 * Exception that occurs in the general context of UWS,
	 * because the given exception has been thrown and that which should return the given HTTP error status.
	 * 
	 * @param httpError	HTTP error code to return.
	 * @param t			The thrown (and so caught) exception.
	 * @param msg		Error message to display.
	 * @param type		Type of the error (FATAL or TRANSIENT). <i>Note: If NULL, it will be considered as FATAL.</i>
	 */
	public UWSException(int httpError, Throwable t, String msg, ErrorType type){
		super(msg, t);
		this.httpErrorCode = (httpError < 0) ? NOT_FOUND : httpError;
		this.errorType = (type == null) ? ErrorType.FATAL : type;
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	/**
	 * Get the HTTP error code that should be returned.
	 * 
	 * @return	The corresponding HTTP error code.
	 */
	public int getHttpErrorCode(){
		return httpErrorCode;
	}

	/**
	 * Get the type of this error (from the UWS point of view ; FATAL or TRANSIENT).
	 * 
	 * @return	Type of this error.
	 */
	public ErrorType getUWSErrorType(){
		return errorType;
	}

}
