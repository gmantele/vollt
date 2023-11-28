package tap.resource;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPException;

/**
 * <p>List the common functions that a TAP resource must have.
 * Basically, any TAP resource may be initialized, may be destroyed, must have a name and must execute a request provided by its TAP service.</p>
 * 
 * <p><i><b>Important note:</b>
 * 	It is strongly recommended that the name of the TAP resource is also provided through a public static attribute named "RESOURCE_NAME".
 * 	If this attribute exists, its value must be the same as the one returned by {@link #getName()}.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public interface TAPResource {

	/**
	 * Let initialize this TAP resource.
	 * 
	 * @param config	Servlet configuration. (may be useful for the configuration of this resource)
	 * 
	 * @throws ServletException	If any error prevent the initialization of this TAP resource. In case a such exception is thrown, the service should stop immediately.
	 */
	public void init(ServletConfig config) throws ServletException;

	/**
	 * Let free properly all system/file/DB resources kept opened by this TAP resource.
	 */
	public void destroy();

	/**
	 * <p>Let diffuse the base URL of the TAP service to all its TAP resources.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This function should be called just once: either at the creation of the service or when the first request is sent to the TAP service
	 * 	(in this case, the request is also used to finish the initialization of the TAP service, and of all its resources).
	 * </i></p>
	 * 
	 * @param baseURL	Common URL/URI used in all requests sent by any user to the TAP service.  
	 */
	public void setTAPBaseURL(String baseURL);

	/**
	 * <p>Get the name of this TAP resource.</p>
	 * 
	 * <p><i><b>Important note:</b>
	 * 	This name MUST NOT be NULL and SHOULD NEVER change.
	 * </i></p>
	 * 
	 * @return	Name of this TAP resource.
	 */
	public String getName();

	/**
	 * <p>Interpret the given request, execute the appropriate action and finally return a result or display information to the user.</p>
	 * 
	 * <p><b>IMPORTANT:
	 * 	"TAP resources can not take the law in their own hands!"</b> :-)
	 * 	Errors that could occur inside this function should not be written directly in the given {@link HttpServletResponse}.
	 * 	They should be thrown to the resources executor: an instance of {@link TAP}, which
	 * 	will fill the {@link HttpServletResponse} with the error in the format described by the IVOA standard - VOTable. Besides, {@link TAP} may also
	 * 	add more information and may log the error (in function of this type).
	 * </p>
	 * 
	 * @param request	Request sent by the user and which should be interpreted/executed here.
	 * @param response	Response in which the result of the request must be written.
	 * 
	 * @return	<i>true</i> if the request has been successfully executed, <i>false</i> otherwise (but generally an exception will be sent if the request can't be executed).
	 * 
	 * @throws IOException		If any error occurs while writing the result of the given request.
	 * @throws TAPException		If any other error occurs while interpreting and executing the request or by formating and writing its result.
	 */
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws IOException, TAPException;

}
