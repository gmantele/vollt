package tap.db;

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

import tap.TAPException;

/**
 * This kind of exception is thrown by instances of {@link DBConnection}
 * if an error occurs while a DB operation (interrogation, update, transaction management).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 09/2011
 * 
 * @see DBConnection
 */
public class DBException extends TAPException {
	private static final long serialVersionUID = 1L;

	/**
	 * Builds a DBException.
	 * 
	 * @param message	Error message.
	 */
	public DBException(String message) {
		super(message);
	}

	/**
	 * <p>Builds a DBException.</p>
	 * 
	 * <p>
	 * 	<b>WARNING:</b> The query parameter is supposed to correspond to the ADQL query.
	 * 	You can set it to the SQL query but you must be aware that it may be displayed to the user.
	 * </p>
	 * 
	 * @param message	Error message.
	 * @param query		ADQL query (this string may be displayed to the user).
	 */
	public DBException(String message, String query) {
		super(message, query);
	}

	/**
	 * Builds a DBException.
	 * 
	 * @param cause	Cause of this error.
	 */
	public DBException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>Builds a DBException.</p>
	 * 
	 * <p>
	 * 	<b>WARNING:</b> The query parameter is supposed to correspond to the ADQL query.
	 * 	You can set it to the SQL query but you must be aware that it may be displayed to the user.
	 * </p>
	 * 
	 * @param cause	Cause of this error.
	 * @param query	ADQL query (this string may be displayed to the user).
	 */
	public DBException(Throwable cause, String query) {
		super(cause, query);
	}

	/**
	 * Builds a DBException
	 * 
	 * @param message	Error message.
	 * @param cause		Cause of this error.
	 */
	public DBException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <p>Builds a DBException.</p>
	 * 
	 * <p>
	 * 	<b>WARNING:</b> The query parameter is supposed to correspond to the ADQL query.
	 * 	You can set it to the SQL query but you must be aware that it may be displayed to the user.
	 * </p>
	 * 
	 * @param message	Error message.
	 * @param cause		Cause of this error.
	 * @param query		ADQL query (this string may be displayed to the user).
	 */
	public DBException(String message, Throwable cause, String query) {
		super(message, cause, query);
	}

}
