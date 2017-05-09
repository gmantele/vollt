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
 * Copyright 2017 - Astronomisches Rechen Institut (ARI)
 */

/**
 * Exception thrown when a processing of a {@link DBConnection} is cancelled.
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.1 (04/2017)
 */
public class DBCancelledException extends DBException {
	private static final long serialVersionUID = 1L;

	public DBCancelledException(){
		super("DB interaction cancelled!");
	}

	public DBCancelledException(String message){
		super(message);
	}

	public DBCancelledException(String message, String query){
		super(message, query);
	}

	public DBCancelledException(Throwable cause){
		super(cause);
	}

	public DBCancelledException(Throwable cause, String query){
		super(cause, query);
	}

	public DBCancelledException(String message, Throwable cause){
		super(message, cause);
	}

	public DBCancelledException(String message, Throwable cause, String query){
		super(message, cause, query);
	}

}
