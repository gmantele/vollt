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

import uws.UWSException;
import uws.job.ExecutionPhase;
import uws.job.JobObserver;
import uws.job.UWSJob;

/**
 * Job observer that unblock (here: notify) the given thread when a change of
 * the execution phase is detected.
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (11/2017)
 * @since 4.3
 */
public class WaitObserver implements JobObserver {
	private static final long serialVersionUID = 1L;

	/** Thread to notify in case an execution phase occurs. */
	private final Thread waitingThread;

	/**
	 * Build a {@link JobObserver} which will wake up the given thread when the
	 * execution phase of watched jobs changes.
	 *
	 * @param thread	Thread to notify.
	 */
	public WaitObserver(final Thread thread){
		waitingThread = thread;
	}

	@Override
	public void update(final UWSJob job, final ExecutionPhase oldPhase, final ExecutionPhase newPhase) throws UWSException{
		if (oldPhase != null && newPhase != null && oldPhase != newPhase){
			synchronized(waitingThread){
				waitingThread.notify();
			}
		}
	}

}