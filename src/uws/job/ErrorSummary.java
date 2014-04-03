package uws.job;

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

import uws.UWSException;

import uws.job.serializer.UWSSerializer;

import uws.job.user.JobOwner;

/**
 * This class gives a short description of the occurred error (if any) during a job execution.
 * A fuller representation of the error may be retrieved from <i>{jobs}/(job-id)/error</i>.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 02/2011
 */
public class ErrorSummary extends SerializableUWSObject {
	private static final long serialVersionUID = 1L;

	/** <b>[Required]</b> A short description of the error. */
	protected String message;

	/** <b>[Required]</b> The type of the error. */
	protected ErrorType type;

	/** <i>[Optional]</i> The URI/URL toward the file which contains a more detailed description of the error (i.e. an Exception stack trace). */
	protected String details = null;


	/* CONSTRUCTORS */
	/**
	 * <p>Builds an error summary from an Exception.</p>
	 * 
	 * <p><b><u>WARNING:</u> No file is written: that is the responsibility of the creator of this error summary !</b></p>
	 * 
	 * @param ex				The Exception which describes the error. Only the message is used ({@link Exception#getMessage()}).
	 * @param errorType			The type of this error. (if <i>null</i> the error type is by default set to {@link ErrorType#FATAL} )
	 * @param detailedMsgURI	<i>null</i> or the URI/URL at which a detailed error message is given (different from {jobs}/(job-id)/error).<br />
	 * 
	 * @see ErrorSummary#ErrorSummary(String, ErrorType, String)
	 */
	public ErrorSummary(Exception ex, ErrorType errorType, String detailedMsgURI){
		this(((ex==null)?null:ex.getMessage()), errorType, detailedMsgURI);
	}

	/**
	 * Builds an error summary with the given short description.
	 * 
	 * @param msg			A short description of the error.
	 * @param errorType		The type of the error. (if <i>null</i> the error type is by default set to {@link ErrorType#FATAL} )
	 * 
	 * @see #ErrorSummary(String, ErrorType, String)
	 */
	public ErrorSummary(String msg, ErrorType errorType){
		this(msg, errorType, (String)null);
	}

	/**
	 * <p>Builds an error summary with the given short description and with the URL to access to a detailed description.</p>
	 * <p><b><u>Warning:</u> No file is written: that is the responsibility of the creator of this error summary !</b></p>
	 * 
	 * @param msg				A short description of the error.
	 * @param errorType			The type of the error. (if <i>null</i> the error type is by default set to {@link ErrorType#FATAL} )
	 * @param detailedMsgURI	<i>null</i> or the URI/URL at which a detailed error message is given (different from {jobs}/(job-id)/error).
	 */
	public ErrorSummary(String msg, ErrorType errorType, String detailedMsgURI){
		message = (msg==null)?"{No error message}":msg;
		type = (errorType==null)?ErrorType.FATAL:errorType;
		details = (detailedMsgURI == null || detailedMsgURI.trim().length() == 0)?null:detailedMsgURI.trim();
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	/**
	 * Gets a short description of the occurred error.
	 * 
	 * @return A short error message.
	 */
	public final String getMessage() {
		return message;
	}

	/**
	 * Gets the type of the occurred error <i>({@link ErrorType#FATAL} by default)</i>.
	 * 
	 * @return The error type.
	 * 
	 * @see ErrorType
	 */
	public final ErrorType getType() {
		return type;
	}

	/**
	 * Indicates whether there are more details about the occurred error.<br />
	 * If <i>true</i> these details can be found at {jobs}/(job-id)/error.
	 * 
	 * @return <i>true</i> if there are more details, <i>false</i> otherwise.
	 */
	public final boolean hasDetail() {
		return details != null;
	}

	/**
	 * Gets the URI/URL where the details about the occurred error can be found.
	 * 
	 * @return The error details.
	 */
	public final String getDetails() {
		return details;
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	@Override
	public String serialize(UWSSerializer serializer, JobOwner owner) throws UWSException {
		return serializer.getErrorSummary(this, true);
	}

	@Override
	public String toString(){
		return "ERROR_SUMMARY {type: "+type.name()+"; message: \""+message+"\"; details: "+(hasDetail()?details:"none")+"}";
	}
}
