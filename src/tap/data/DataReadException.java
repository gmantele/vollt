package tap.data;

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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import tap.TAPException;

/**
 * Exception that occurs when reading a data input (can be an InputStream, a ResultSet, a SavotTable, ...).
 * 
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 2.0 (06/2014)
 * @since 2.0
 * 
 * @see TableIterator
 */
public class DataReadException extends TAPException {
	private static final long serialVersionUID = 1L;

	public DataReadException(final String message){
		super(message);
	}

	public DataReadException(Throwable cause){
		super(cause);
	}

	public DataReadException(String message, Throwable cause){
		super(message, cause);
	}

}
