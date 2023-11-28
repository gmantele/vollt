package uws.service.wait;

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
 * Copyright 2017 - Astronomisches Rechen Institut (ARI)
 */

import javax.servlet.http.HttpServletRequest;

import uws.job.UWSJob;
import uws.job.user.JobOwner;

/**
 * Implementations of this interface define the policy to apply when a blocking
 * of a request is asked by a UWS client.
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (11/2017)
 * @since 4.3
 */
public interface BlockingPolicy {

	/**
	 * Notify this {@link BlockingPolicy} that the given thread is going to
	 * be blocked for the specified duration. This function then decides how
	 * long the given thread must really wait before resuming.
	 *
	 * <p>
	 * 	The parameter "userDuration" and the returned value are durations
	 * 	expressed in seconds. Both follow the same rules:
	 * </p>
	 * <ul>
	 * 	<li><u>If &lt; 0</u>, the request will wait theoretically
	 * 		                  indefinitely.</li>
	 * 	<li><u>If 0</u>, the request will return immediately ; no wait.</li>
	 * 	<li><u>If &gt; 0</u>, the request will wait for the specified amount of
	 * 		                  seconds.</li>
	 * </ul>
	 *
	 * <p>
	 * 	Since a timeout or another special behavior may be chosen by this
	 * 	{@link BlockingPolicy}, the returned value may be different from the
	 * 	user's asked duration. The value that should be taken into account is
	 * 	obviously the returned one.
	 * </p>
	 *
	 * <p><i><b>IMPORTANT:</b>
	 * 	This function may <b>UN</b>block an older request/thread, in function of
	 * 	the strategy chosen/implemented by this {@link BlockingPolicy}.
	 * </i></p>
	 *
	 * @param thread		Thread that is going to be blocked.
	 *                      <i>MUST NOT be NULL. If NULL this function will
	 *                      either do nothing and return 0 (no blocking)
	 *                      or throw a {@link NullPointerException}.</i>
	 * @param userDuration	Waiting duration (in seconds) asked by the user.
	 *                    	<i>&lt; 0 means indefinite, 0 means no wait and
	 *                    	&gt; 0 means waiting for the specified amount of
	 *                    	seconds.</i>
	 * @param job			The job associated with the thread.
	 *           			<i>Should not be NULL.</i>
	 * @param user			The user who asked for the blocking behavior.
	 *            			<i>If NULL, the request will be concerned as
	 *            			anonymous and a decision to identify the user
	 *            			(e.g. use the IP address) may be chosen by the
	 *            			{@link BlockingPolicy} implementation if
	 *            			required.</i>
	 * @param request		The request which is going to be blocked.
	 *               		<i>Should not be NULL.</i>
	 *
	 * @return	The real duration (in seconds) that the UWS service must wait
	 *        	before returning a response to the given HTTP request.
	 *        	<i>&lt; 0 means indefinite, 0 means no wait and &gt; 0 means
	 *        	waiting for the specified amount of seconds.</i>
	 *
	 * @throws NullPointerException	If the given thread is NULL.
	 */
	public long block(final Thread thread, final long userDuration, final UWSJob job, final JobOwner user, final HttpServletRequest request) throws NullPointerException;

	/**
	 * Notify this {@link BlockingPolicy} that the given thread is not blocked
	 * anymore.
	 *
	 * @param unblockedThread	Thread that is not blocked any more.
	 *                       	<b>MUST be NOT NULL.</b>
	 * @param job				The job associated with the unblocked Thread.
	 *           				<i>Should not be NULL.</i>
	 * @param user				The user who originally asked for the blocking
	 *            				behavior.
	 *            				<i>If NULL, the request will be concerned as
	 *            				anonymous and a decision to identify the user
	 *            				(e.g. use the IP address) may be chosen by the
	 *            				{@link BlockingPolicy} implementation if
	 *            				required.</i>
	 * @param request			The request which has been unblocked.
	 *               			<i>Should not be NULL.</i>
	 */
	public void unblocked(final Thread unblockedThread, final UWSJob job, final JobOwner user, final HttpServletRequest request);

}