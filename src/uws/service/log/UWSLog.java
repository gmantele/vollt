package uws.service.log;

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

import javax.servlet.http.HttpServletRequest;

import uws.job.JobList;
import uws.job.UWSJob;

import uws.job.user.JobOwner;

import uws.service.UWS;

/**
 * Lets logging any kind of message about a UWS.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public interface UWSLog {

	/* *********************** */
	/* GENERAL LOGGING METHODS */
	/* *********************** */

	/**
	 * Logs a debug message.
	 * @param msg	A DEBUG message.
	 */
	public void debug(final String msg);

	/**
	 * Logs an exception as a debug message.
	 * @param t	An exception.
	 */
	public void debug(final Throwable t);

	/**
	 * Logs a full (message+exception) debug message.
	 * @param msg	A DEBUG message.
	 * @param t		An exception.
	 */
	public void debug(final String msg, final Throwable t);

	/**
	 * Logs the given information.
	 * @param msg	An INFO message.
	 */
	public void info(final String msg);

	/**
	 * Logs the given warning.
	 * @param msg	A WARNING message.
	 */
	public void warning(final String msg);

	/**
	 * Logs the given error.
	 * @param msg	An ERROR message.
	 */
	public void error(final String msg);

	/**
	 * Logs the given exception as an error.
	 * @param t	An exception.
	 */
	public void error(final Throwable t);

	/**
	 * Logs a full (message+exception) error message.
	 * @param msg	An ERROR message.
	 * @param t		An exception.
	 */
	public void error(final String msg, final Throwable t);

	/* *************************************** */
	/* LOGGING METHODS TO WATCH THE UWS STATUS */
	/* *************************************** */

	/**
	 * <p>Logs the fact that the given UWS has just been initialized.</p>
	 * <p><i><u>note:</u> Theoretically, no restoration has been done when this method is called.</i></p>
	 * @param uws	The UWS which has just been initialized.
	 */
	public void uwsInitialized(final UWS uws);

	/**
	 * Logs the fact that the given UWS has just been restored.
	 * @param uws		The restored UWS.
	 * @param report	Report of the restoration (in the order: nb restored jobs, nb jobs, nb restored users, nb users).
	 */
	public void uwsRestored(final UWS uws, final int[] report);

	/**
	 * Logs the fact that the given UWS has just been saved.
	 * @param uws		The saved UWS.
	 * @param report	Report of the save (in the order: nb saved jobs, nb jobs, nb saved users, nb users).
	 */
	public void uwsSaved(final UWS uws, final int[] report);

	/**
	 * Logs the fact that all the jobs of the given user have just been saved.
	 * @param owner		The owner whose all the jobs have just been saved.
	 * @param report	Report of the save (in the order: nb saved jobs, nb jobs).
	 */
	public void ownerJobsSaved(final JobOwner owner, final int[] report);

	/**
	 * Logs the fact that the given job has just been created.
	 * @param job	The created job.
	 */
	public void jobCreated(final UWSJob job);

	/**
	 * Logs the fact that the given job has just started.
	 * @param job	The started job.
	 */
	public void jobStarted(final UWSJob job);

	/**
	 * Logs the fact that the given job has just finished.
	 * @param job	The finished job.
	 */
	public void jobFinished(final UWSJob job);

	/**
	 * Logs the fact that the given job has just been destroyed.
	 * @param job	The destroyed job.
	 * @param jl	The job list from which the given job has just been removed.
	 */
	public void jobDestroyed(final UWSJob job, final JobList jl);

	/* ************* */
	/* HTTP ACTIVITY */
	/* ************* */

	/**
	 * Logs any HTTP request received by the UWS and also the send response.
	 * @param request				The HTTP request received by the UWS.
	 * @param user					The identified user which sends this request. (MAY BE NULL)
	 * @param uwsAction				The identified UWS action. (MAY BE NULL)
	 * @param responseStatusCode	The HTTP status code of the response given by the UWS.
	 * @param responseMsg			The message (or a summary of the message) returned by the UWS. (MAY BE NULL)
	 * @param responseError			The error sent by the UWS. (MAY BE NULL)
	 */
	public void httpRequest(final HttpServletRequest request, final JobOwner user, final String uwsAction, final int responseStatusCode, final String responseMsg, final Throwable responseError);

	/* ********************** */
	/* THREAD STATUS MESSAGES */
	/* ********************** */
	/**
	 * Logs the fact that the given thread has just started.
	 * @param t		The started thread.
	 * @param task	Name/Description of the task that the given thread is executing.
	 */
	public void threadStarted(final Thread t, final String task);

	/**
	 * Logs the fact that the given thread has just been interrupted.
	 * @param t		The interrupted thread.
	 * @param task	Name/Description of the task that the given thread was trying to execute.
	 * @param error	Exception that has interrupted the given thread.
	 */
	public void threadInterrupted(final Thread t, final String task, final Throwable error);

	/**
	 * Logs the fact that the given thread has just finished.
	 * @param t		The finished thread.
	 * @param task	Name/Description of the task that the given thread was executing.
	 */
	public void threadFinished(final Thread t, final String task);
}
