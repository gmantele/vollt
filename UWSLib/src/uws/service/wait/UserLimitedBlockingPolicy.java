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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.servlet.http.HttpServletRequest;

import uws.job.UWSJob;
import uws.job.user.JobOwner;

/**
 * This {@link BlockingPolicy} extends the {@link LimitedBlockingPolicy}.
 * It proposes to limit the blocking duration, but it also limits the
 * number of blocked threads for a given job and user.
 *
 * <h3>Blocked per Job AND User</h3>
 *
 * <p>
 * 	The limit on the number of threads is valid ONLY for a given job AND
 * 	a given user. For example, let's assume there is a limit
 * 	of N blocking requests per job and user. The user U1 can start maximum N
 * 	blocking requests to access the job J1 but not more. During this
 * 	time he can also start up to N blocking access requests to any other job.
 * 	And since this limit is valid only per user, another user U2 can also
 * 	start up to N blocking requests on the job J1 without being affected by the
 * 	fact that the limit is reached by the user U1 on this same job.
 * </p>
 *
 * <p><i>Note:
 * 	If no user is identified, the IP address will be used instead.
 * </i></p>
 *
 * <h3>What happens when the limit is reached?</h3>
 *
 * <p>In a such case, 2 strategies are proposed:</p>
 * <ul>
 * 	<li>unblock the oldest blocked thread and accept the new blocking</li>
 * 	<li>do not block for the new asked blocking (then
 * 		{@link #block(Thread, long, UWSJob, JobOwner, HttpServletRequest)} will
 * 		return 0)</li>
 * </ul>
 *
 * <p>
 * 	The strategy to use MUST be specified at creation using
 * 	{@link #UserLimitedBlockingPolicy(long, int, boolean)} with a third
 * 	parameter set to <code>true</code> to unblock the oldest thread if needed,
 * 	or <code>false</code> to prevent blocking if the limit is reached.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (11/2017)
 * @since 4.3
 */
public class UserLimitedBlockingPolicy extends LimitedBlockingPolicy {

	/** Default number of allowed blocked threads. */
	public final static int DEFAULT_NB_MAX_BLOCKED = 3;

	/** The maximum number of blocked threads for a given job and user. */
	protected final int maxBlockedThreadsByUser;

	/** List of all blocked threads.
	 * <p>
	 * 	Keys are an ID identifying a given job AND a given user
	 * 	(basically: <code>jobId+";"+userId</code> ;  see
	 * 	{@link #buildKey(UWSJob, JobOwner, HttpServletRequest)} for more
	 * 	details).
	 * </p>
	 * <p>
	 * 	Values are fixed-length queues of blocked threads.
	 * </p> */
	protected final Map<String,BlockingQueue<Thread>> blockedThreads;

	/** Indicate what should happen when the maximum number of threads for a
	 * given job and user is reached.
	 * <p>
	 * 	<code>true</code> to unblock the oldest blocked thread in order to allow
	 * 	                  the new blocking.
	 * 	<code>false</code> to forbid new blocking.
	 * </p> */
	protected final boolean unblockOld;

	/**
	 * Build a default {@link UserLimitedBlockingPolicy}.
	 *
	 * <p>
	 * 	This instance will limit the number of blocked threads per user and job
	 * 	to the default value (i.e. {@value #DEFAULT_NB_MAX_BLOCKED}) and will
	 * 	limit the blocking duration to the default timeout
	 * 	(see {@link LimitedBlockingPolicy#DEFAULT_TIMEOUT}).
	 * </p>
	 *
	 * <p>
	 * 	When the limit of threads is reached, the oldest thread is unblocked
	 * 	in order to allow the new incoming blocking.
	 * </p>
	 */
	public UserLimitedBlockingPolicy(){
		this(DEFAULT_TIMEOUT, DEFAULT_NB_MAX_BLOCKED);
	}

	/**
	 * Build a {@link UserLimitedBlockingPolicy} which will limit the blocking
	 * duration to the given value and will limit the number of blocked threads
	 * per job and user to the default value (i.e.
	 * {@value #DEFAULT_NB_MAX_BLOCKED}).
	 *
	 * <p>
	 * 	When the limit of threads is reached, the oldest thread is unblocked
	 * 	in order to allow the new incoming blocking.
	 * </p>
	 *
	 * @param timeout	Maximum blocking duration (in seconds).
	 *               	<i>If &lt; 0, the default timeout (see
	 *               	{@link LimitedBlockingPolicy#DEFAULT_TIMEOUT}) will be
	 *               	set.</i>
	 *
	 * @see LimitedBlockingPolicy#LimitedBlockingPolicy(long)
	 */
	public UserLimitedBlockingPolicy(final long timeout){
		this(timeout, DEFAULT_NB_MAX_BLOCKED);
	}

	/**
	 * Build a {@link UserLimitedBlockingPolicy} which will limit the blocking
	 * duration to the given value and will limit the number of blocked threads
	 * per job and user to the given value.
	 *
	 * <p>
	 * 	When the limit of threads is reached, the oldest thread is unblocked in
	 * 	order to allow the new incoming blocking.
	 * </p>
	 *
	 * @param timeout		Maximum blocking duration (in seconds).
	 *               		<i>If &lt; 0, the default timeout (see
	 *               		{@link LimitedBlockingPolicy#DEFAULT_TIMEOUT}) will
	 *               		be set.</i>
	 * @param maxNbBlocked	Maximum number of blocked threads allowed for a
	 *                    	given job and a given user.
	 *                    	<i>If &le; 0, this parameter will be ignored and the
	 *                    	default value (i.e. {@value #DEFAULT_NB_MAX_BLOCKED})
	 *                    	will be set instead.</i>
	 */
	public UserLimitedBlockingPolicy(final long timeout, final int maxNbBlocked){
		this(timeout, maxNbBlocked, true);
	}

	/**
	 * Build a {@link UserLimitedBlockingPolicy} which will limit the blocking
	 * duration to the given value and will limit the number of blocked threads
	 * per job and user to the given value.
	 *
	 * <p>
	 * 	When the limit of threads is reached, the oldest thread is unblocked if
	 * 	the 3rd parameter is <code>true</code>, or new incoming blocking will
	 * 	be forbidden if this parameter is <code>false</code>.
	 * </p>
	 *
	 * @param timeout		Maximum blocking duration (in seconds).
	 *               		<i>If &lt; 0, the default timeout (see
	 *               		{@link LimitedBlockingPolicy#DEFAULT_TIMEOUT}) will
	 *               		be set.</i>
	 * @param maxNbBlocked	Maximum number of blocked threads allowed for a
	 *                    	given job and a given user.
	 *                    	<i>If &le; 0, this parameter will be ignored and the
	 *                    	default value (i.e. {@value #DEFAULT_NB_MAX_BLOCKED})
	 *                    	will be set instead.</i>
	 * @param unblockOld	Set the behavior to adopt when the maximum number of
	 *                  	threads is reached for a given job and user.
	 *                  	<code>true</code> to unblock the oldest thread in
	 *                  	order to allow the new incoming blocking,
	 *                  	<code>false</code> to forbid the new incoming
	 *                  	blocking.
	 */
	public UserLimitedBlockingPolicy(final long timeout, final int maxNbBlocked, final boolean unblockOld){
		super(timeout);
		maxBlockedThreadsByUser = (maxNbBlocked <= 0) ? DEFAULT_NB_MAX_BLOCKED : maxNbBlocked;
		blockedThreads = Collections.synchronizedMap(new HashMap<String,BlockingQueue<Thread>>());
		this.unblockOld = unblockOld;
	}

	/**
	 * Build the key for the map {@link #blockedThreads}.
	 *
	 * <p>The built key is: <code>jobId + ";" + userId</code>.</p>
	 *
	 * <p><i>Note:
	 * 	If no user is logged in or if the user is not specified here or if it
	 * 	does not have any ID, the IP address of the HTTP client will be used
	 * 	instead.
	 * </i></p>
	 *
	 * @param job		Job associated with the request to block.
	 *           		<b>MUST NOT be NULL.</b>
	 * @param user		User who asked the blocking behavior.
	 *            		<i>If NULL (or it has a NULL ID), the IP address of the
	 *            		HTTP client will be used.</i>
	 * @param request	HTTP request which should be blocked.
	 *               	<i>SHOULD NOT be NULL.</i>
	 *
	 * @return	The corresponding map key.
	 *        	<i>NEVER NULL.</i>
	 */
	protected final String buildKey(final UWSJob job, final JobOwner user, final HttpServletRequest request){
		if (user == null || user.getID() == null){
			if (request == null)
				return job.getJobId() + ";???";
			else
				return job.getJobId() + ";" + request.getRemoteAddr();
		}else
			return job.getJobId() + ";" + user.getID();
	}

	@Override
	public long block(final Thread thread, final long userDuration, final UWSJob job, final JobOwner user, final HttpServletRequest request){
		// Nothing should happen if no thread and/or no job is provided:
		if (job == null || thread == null)
			return 0;

		// Get the ID of the blocking (job+user):
		String id = buildKey(job, user, request);

		// Get the corresponding queue (if any):
		BlockingQueue<Thread> queue = blockedThreads.get(id);
		if (queue == null)
			queue = new ArrayBlockingQueue<Thread>(maxBlockedThreadsByUser);

		// Try to add the recently blocked thread:
		if (!queue.offer(thread)){
			/* If it fails, 2 strategies are possible: */
			/* 1/ Unblock the oldest blocked thread and add the given thread
			 *    into the queue: */
			if (unblockOld){
				// Get the oldest blocked thread:
				Thread old = queue.poll();
				// Wake it up // Unblock it:
				if (old != null){
					synchronized(old){
						old.notifyAll();
					}
				}
				// Add the thread into the queue:
				queue.offer(thread);
			}
			/* 2/ The given thread CAN NOT be blocked because too many threads
			 *    for this job and user are already blocked => unblock it! */
			else
				return 0;
		}

		// Add the queue into the map:
		blockedThreads.put(id, queue);

		// Return the eventually limited duration to wait:
		return super.block(thread, userDuration, job, user, request);

	}

	@Override
	public void unblocked(final Thread unblockedThread, final UWSJob job, final JobOwner user, final HttpServletRequest request){
		// Nothing should happen if no thread and/or no job is provided:
		if (job == null || unblockedThread == null)
			return;

		// Get the ID of the blocking (job+user):
		String id = buildKey(job, user, request);

		// Get the corresponding queue (if any):
		BlockingQueue<Thread> queue = blockedThreads.get(id);

		if (queue != null){
			Iterator<Thread> it = queue.iterator();
			// Search for the corresponding item inside the queue:
			while(it.hasNext()){
				// When found...
				if (it.next().equals(unblockedThread)){
					// ...remove it from the queue:
					it.remove();
					// If the queue is now empty, remove the queue from the map:
					if (queue.isEmpty())
						blockedThreads.remove(id);
					return;
				}
			}
		}
	}

}