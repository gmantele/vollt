package uws.service;

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

import uws.UWSException;
import uws.job.JobList;
import uws.job.serializer.UWSSerializer;
import uws.service.backup.UWSBackupManager;
import uws.service.file.UWSFileManager;
import uws.service.log.UWSLog;

/**
 * <p>
 * 	Minimal API of a UWS service.
 * 	Basically, an instance of this interface is supposed to manage one or several jobs lists.
 * </p>
 * 
 * <p><i><u>note:</u>
 * 	All the functions of this interface are required by {@link JobList}, {@link uws.job.UWSJob}
 * 	and all the other classes available in this library.
 * </i></p>
 * 
 * <p>Two default implementations of this interface are provided in this library:</p>
 * <ul>
 * 	<li>{@link UWSService}: this class represents an object which is able to receive, to interpret
 * 		and to answer to any HTTP request as a UWS service must do (see {@link UWSService#executeRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
 * 		Thus, an instance of this class must be used inside a servlet: all HTTP requests dedicated
 * 		to UWS will be then forwarded to it.
 *	</li>
 * 	<li>{@link UWSServlet}: this class represents directly a servlet. All standard UWS actions are managed as new HTTP methods.
 * 		Indeed, for each HTTP method, a servlet has one function (i.e. doGet(...), doPost(...)). So, for each UWS action, a {@link UWSServlet}
 * 		has one function: doAddJob(...), doDestroyJob(...), doGetJob(...), ...</li>
 * </ul>
 * 
 * <p>
 * 	These two classes already implement all standard actions and behaviors of UWS 1.0. Nothing really change between them except the
 * 	way they lets creating and managing a UWS. The second implementation is the most simple to use because it gathers
 * 	the UWS and the servlet.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (12/2014)
 */
public interface UWS extends Iterable<JobList> {

	/** Attribute of the HttpServletRequest to set and to get in order to access the request ID set by the UWS library.
	 * @since 4.1 */
	public static final String REQ_ATTRIBUTE_ID = "UWS_REQUEST_ID";

	/** Attribute of the HttpServletRequest to set and to get in order to access the parameters extracted by the UWS library (using a RequestParser).
	 * @since 4.1 */
	public static final String REQ_ATTRIBUTE_PARAMETERS = "UWS_PARAMETERS";

	/**
	 * Gets the name of this UWS.
	 * 
	 * @return	Its name (MAY BE NULL).
	 */
	public String getName();

	/**
	 * Gets the description of this UWS.
	 * 
	 * @return	Its description (MAY BE NULL).
	 */
	public String getDescription();

	/* ******************* */
	/* JOB LIST MANAGEMENT */
	/* ******************* */

	/**
	 * Adds a jobs list to this UWS.
	 * 
	 * @param jl	The jobs list to add.
	 * 
	 * @return		<i>true</i> if the jobs list has been successfully added,
	 * 				<i>false</i> if the given jobs list is <i>null</i> or if a jobs list with this name already exists
	 * 				or if a UWS is already associated with another UWS.
	 */
	public boolean addJobList(final JobList newJL);

	/**
	 * Gets the jobs list whose the name matches exactly the given one.
	 * 
	 * @param name	Name of the jobs list to get.
	 * @return		The corresponding jobs list.
	 * 
	 * @throws UWSException		If the given name is <i>null</i> or empty, or if no jobs list matches.
	 */
	public JobList getJobList(final String name) throws UWSException;

	/**
	 * Gets the number of managed jobs lists.
	 * 
	 * @return	The number of jobs lists.
	 */
	public int getNbJobList();

	/**
	 * <p>Destroys the specified jobs list.</p>
	 * <p><i><u>note:</u> After the call of this function, the UWS reference of the given jobs list should be removed (see {@link JobList#setUWS(UWS)}).</i></p>
	 * 
	 * @param name	Name of the jobs list to destroy.
	 * 
	 * @return	<i>true</i> if the given jobs list has been destroyed, <i>false</i> otherwise.
	 */
	public boolean destroyJobList(final String name) throws UWSException;

	/* **************************** */
	/* JOB SERIALIZATION MANAGEMENT */
	/* **************************** */

	/**
	 * <p>Gets the serializer whose the MIME type is the same as the given one.</p>
	 * <p><i><u>Note:</u> If this UWS has no corresponding serializer, a default one should be returned !</i></p>
	 * 
	 * @param mimeTypes		The MIME type of the searched serializer (may be more than one MIME types
	 * 						- comma separated ; see the format of the Accept header of a HTTP-Request).
	 * @return				The corresponding serializer
	 * 						or the default serializer of this UWS if no corresponding serializer has been found.
	 * @throws UWSException	If there is no corresponding serializer AND if the default serializer of this UWS can not be found.
	 * 
	 * @see uws.AcceptHeader#AcceptHeader(String)
	 * @see uws.AcceptHeader#getOrderedMimeTypes()
	 */
	public UWSSerializer getSerializer(final String mimeTypes) throws UWSException;

	/* ******************* */
	/* UWS URL INTERPRETER */
	/* ******************* */

	/**
	 * <p>Gets the object which is able to interpret and to build any UWS URL.
	 * It MUST be loaded with the root URL of this UWS: see {@link UWSUrl#load(javax.servlet.http.HttpServletRequest)} and {@link UWSUrl#load(java.net.URL)}.</p>
	 * 
	 * <p><i><u>note:</u> This getter is particularly used to serialize the jobs lists and their jobs.</i></p>
	 * 
	 * @return	Its UWS URL interpreter (SHOULD BE NOT NULL).
	 */
	public UWSUrl getUrlInterpreter();

	/* ************** */
	/* LOG MANAGEMENT */
	/* ************** */

	/**
	 * <p>Gets the logger of this UWS.</p>
	 * <p><i><u>note:</u>A UWS logger is used to watch the HTTP requests received by the UWS and their responses.
	 * The activity of the UWS is also logged and particularly the life of the different jobs and their threads.
	 * A default implementation is available: {@link uws.service.log.DefaultUWSLog}.</i></p>
	 * 
	 * @return	Its logger <u><b>(MUST BE NOT NULL)</b></u>.
	 */
	public UWSLog getLogger();

	/* ******************* */
	/* USER IDENTIFICATION */
	/* ******************* */

	/**
	 * Gets the object which is able to identify a user from an HTTP request.
	 * 
	 * @return	Its user identifier.
	 */
	public UserIdentifier getUserIdentifier();

	/* *********** */
	/* JOB FACTORY */
	/* *********** */

	/**
	 * <p>Gets its job factory.</p>
	 * <p><i><u>note:</u> This objects is the only one to know how to extract job parameters from an HTTP request,
	 * how to create a job and how to create its respective thread. A partial implementation which answers to
	 * the 2 first questions is available: {@link AbstractUWSFactory}</i></p>
	 * 
	 * @return Its job factory.
	 */
	public UWSFactory getFactory();

	/* *************** */
	/* FILE MANAGEMENT */
	/* *************** */

	/**
	 * <p>Gets its file manager.</p>
	 * <p><i><u>note:</u> A file manager tells to a UWS how to create, read and write the different managed files
	 * (i.e. log, result, errors, backup). A default implementation is available: {@link uws.service.file.LocalUWSFileManager}.</i></p>
	 * 
	 * @return Its file manager.
	 */
	public UWSFileManager getFileManager();

	/* ***************** */
	/* BACKUP MANAGEMENT */
	/* ***************** */

	/**
	 * <p>Gets its backup manager.</p>
	 * <p><i><u>note:</u> This object should be used at the initialization of the UWS to restore a previous "session" (see {@link UWSBackupManager#restoreAll()})
	 * and must be used each time the list of jobs of a user (see {@link UWSBackupManager#saveOwner(uws.job.user.JobOwner)}) or all the jobs of this UWS must be saved (see {@link UWSBackupManager#saveAll()}).</i></p>
	 * 
	 * @return Its backup manager.
	 */
	public UWSBackupManager getBackupManager();

}
