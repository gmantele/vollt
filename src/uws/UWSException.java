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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import uws.job.ErrorType;

/**
 * Any exception returned by a class of the UWS pattern may be associated with
 * an HTTP error code (like: 404, 303, 500) and a UWS error type.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 12/2010
 */
public class UWSException extends Exception {
	private static final long serialVersionUID = 1L;

	// SUCCESS codes:
	public final static int OK = 200;
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
	public UWSException(String msg){
		this(msg, ErrorType.FATAL);
	}

	public UWSException(String msg, ErrorType type){
		super(msg);
		if (type != null)
			errorType = type;
	}

	public UWSException(Throwable t){
		this(t, ErrorType.FATAL);
	}

	public UWSException(Throwable t, ErrorType type){
		super(t);
		if (type != null)
			errorType = type;
	}

	public UWSException(int httpError, String msg){
		this(msg);
		if (httpError >= 0)
			httpErrorCode = httpError;
	}

	public UWSException(int httpError, String msg, ErrorType type){
		this(msg, type);
		if (httpError >= 0)
			httpErrorCode = httpError;
	}

	public UWSException(int httpError, Throwable t){
		this(t);
		if (httpError >= 0)
			httpErrorCode = httpError;
	}

	public UWSException(int httpError, Throwable t, ErrorType type){
		this(t, type);
		if (httpError >= 0)
			httpErrorCode = httpError;
	}

	public UWSException(int httpError, Throwable t, String msg){
		this(httpError, t, msg, ErrorType.FATAL);
	}

	public UWSException(int httpError, Throwable t, String msg, ErrorType type){
		super(msg, t);
		if (httpError >= 0)
			httpErrorCode = httpError;
		if (type != null)
			errorType = type;
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	public int getHttpErrorCode(){
		return httpErrorCode;
	}

	public ErrorType getUWSErrorType(){
		return errorType;
	}

}
