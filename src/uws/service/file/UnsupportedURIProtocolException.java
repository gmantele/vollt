package uws.service.file;

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
 * Copyright 2014 - Astronomisches Rechen Institut (ARI)
 */

import java.net.URI;

import uws.UWSException;

/**
 * Error sent when trying to read a remote file using a URI whose the scheme/protocol is not supported.
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.1 (11/2014)
 * @since 4.1
 */
public class UnsupportedURIProtocolException extends UWSException {
	private static final long serialVersionUID = 1L;

	/**
	 * Build an {@link UnsupportedURIProtocolException}.
	 * 
	 * @param uri	The URI whose the scheme/protocol is incorrect.
	 */
	public UnsupportedURIProtocolException(final URI uri){
		super(UWSException.BAD_REQUEST, "Unsupported protocol: \"" + (uri != null ? uri.getScheme() : "") + "\"! => can not open the resource \"" + uri + "\".");
	}

}
