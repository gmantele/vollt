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
 * Copyright 2015 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

/**
 * <p>Exception which occurs when the connection between the HTTP client and a servlet has been unexpectedly closed.</p>
 * 
 * <p>
 * 	In such situation Tomcat and JBoss throw a class extending {@link IOException} and also named ClientAbortException.
 * 	Jetty just throw a simple {@link IOException} with an appropriate message. And so, other servlet
 * 	containers may throw a similar exception when a client-server connection is closed. This implementation
 * 	of ClientAbortException provided by the library aims to signal this error in a unified way, with a single
 * 	{@link IOException}, whatever is the underlying servlet container.
 * </p>
 * 
 * <p><i>Note:
 * 	Instead of this exception any IOException thrown by an {@link java.io.OutputStream} or a {@link java.io.PrintWriter}
 * 	which has been provided by an {@link javax.servlet.http.HttpServletResponse} should be considered as an abortion of
 * 	the HTTP client.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.1 (04/2015)
 * @since 4.1
 */
public class ClientAbortException extends IOException {
	private static final long serialVersionUID = 1L;

	public ClientAbortException(){
		super();
	}

	public ClientAbortException(final IOException ioe){
		super(ioe);
	}

}
