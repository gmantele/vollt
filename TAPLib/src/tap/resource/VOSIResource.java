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

/**
 * <p>VOSI - VO Support Interfaces - lets describe a minimal interface that VO web services should provide.</p>
 * 
 * <p>
 * 	This interface aims to give information about the capabilities, the availability and the reliability of the service.
 * 	To reach this goal the 3 following endpoints (resources) should be provided:
 * </p>
 * <ol>
 * 	<li><b>Capability metadata:</b> list all available resources, give their access URL and a standard ID (helping to identify the type of resource).
 * 	                                More information related to the service itself (or about the VO standard it is implementing) may be provided.</li>
 *                                  
 * 	<li><b>Availability metadata:</b> indicate whether the service is available or not. It may also provide a note and some other information about
 * 	                                  its reliability, such as the date at which it is up, or since when it is down and when it will be back.</li>
 * 
 * 	<li><b>Tables metadata:</b> since some VO services deal with tabular data (in output, in input or queriable by a language like ADQL),
 * 	                            a VOSI-compliant service shall provide a list and a description of them.</li>
 * </ol>
 * 
 * <p>
 * 	Implementing the VOSI interface means that each service endpoint/resource must be described in the capability endpoint with an access URL and a standard VO ID.
 * </p>
 * 
 * <p>The standard IDs of the VOSI endpoints are the following:</p>
 * <ul>
 * 	<li><b>Capabilities:</b> ivo://ivoa.net/std/VOSI#capabilities</li>
 * 	<li><b>Availability:</b> ivo://ivoa.net/std/VOSI#availability</li>
 * 	<li><b>Tables:</b> ivo://ivoa.net/std/VOSI#tables</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public interface VOSIResource {

	/**
	 * Get the capabilities of this resource.
	 * 
	 * @return	Resource capabilities.
	 */
	public String getCapability();

	/**
	 * Get the URL which lets access this resource.
	 * 
	 * @return	Access URL.
	 */
	public String getAccessURL();

	/**
	 * <p>Get the standardID of this endpoint of the VOSI interface.</p>
	 * 
	 * <p>The standard IDs of the VOSI endpoints are the following:</p>
	 * <ul>
	 * 	<li><b>Capabilities:</b> ivo://ivoa.net/std/VOSI#capabilities</li>
	 * 	<li><b>Availability:</b> ivo://ivoa.net/std/VOSI#availability</li>
	 * 	<li><b>Tables:</b> ivo://ivoa.net/std/VOSI#tables</li>
	 * </ul>
	 * 
	 * @return	Standard ID of this VOSI endpoint.
	 */
	public String getStandardID();

}