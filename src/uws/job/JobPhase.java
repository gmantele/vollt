package uws.job;

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
 * Copyright 2012-2017 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.Serializable;

import uws.UWSException;
import uws.UWSExceptionFactory;

/**
 * An instance of this class represents the current execution phase of a given
 * job, and it describes the transitions between the different phases.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (09/2017)
 *
 * @see ExecutionPhase
 * @see UWSJob
 */
public class JobPhase implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Current phase of the associated job. */
	protected ExecutionPhase phase = ExecutionPhase.PENDING;

	/** The job whose the current phase is represented by this class. */
	protected final UWSJob job;

	/**
	 * Builds the phase manager of the given job.
	 *
	 * @param j	The job whose the execution phase must be represented by the
	 *         	built JobPhase instance.
	 *
	 * @throws NullPointerException	If the given job is <i>null</i>.
	 */
	public JobPhase(UWSJob j) throws NullPointerException{
		if (j == null)
			throw new NullPointerException("Missing job instance ! => impossible to build a JobPhase instance.");
		job = j;
	}

	/**
	 * Gets the job whose the execution phase is represented by this object.
	 *
	 * @return	The associated job.
	 */
	public final UWSJob getJob(){
		return job;
	}

	/**
	 * Gets the current phase of the job.
	 *
	 * @return	The current job phase.
	 */
	public final ExecutionPhase getPhase(){
		return phase;
	}

	/**
	 * Lets changing the current phase of the associated job considering the
	 * order of execution phases.
	 *
	 * @param p	The new execution phase.
	 *
	 * @throws UWSException	If the given phase is <i>null</i> or if the phase
	 *                     	transition is forbidden.
	 *
	 * @see #setPhase(ExecutionPhase, boolean)
	 */
	public final void setPhase(ExecutionPhase p) throws UWSException{
		setPhase(p, false);
	}

	/**
	 * Lets changing the current phase of the associated job considering or
	 * not the order of execution phases.
	 *
	 * <p><i>Note:
	 * 	If the given phase is <i>null</i>, nothing is done.
	 * </i></p>
	 *
	 * @param p		The new phase.
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If the phase transition is forbidden.
	 *
	 * @see #setPendingPhase(boolean)
	 * @see #setQueuedPhase(boolean)
	 * @see #setExecutingPhase(boolean)
	 * @see #setCompletedPhase(boolean)
	 * @see #setAbortedPhase(boolean)
	 * @see #setErrorPhase(boolean)
	 * @see #setHeldPhase(boolean)
	 * @see #setSuspendedPhase(boolean)
	 * @see #setArchivedPhase(boolean)
	 * @see #setUnknownPhase(boolean)
	 */
	public void setPhase(ExecutionPhase p, boolean force) throws UWSException{
		if (p == null)
			return;

		// Check that the given phase follows the imposed phases order:
		switch(p){
			case PENDING:
				setPendingPhase(force);
				break;
			case QUEUED:
				setQueuedPhase(force);
				break;
			case EXECUTING:
				setExecutingPhase(force);
				break;
			case COMPLETED:
				setCompletedPhase(force);
				break;
			case ABORTED:
				setAbortedPhase(force);
				break;
			case ERROR:
				setErrorPhase(force);
				break;
			case HELD:
				setHeldPhase(force);
				break;
			case SUSPENDED:
				setSuspendedPhase(force);
				break;
			case ARCHIVED:
				setArchivedPhase(force);
				break;
			case UNKNOWN:
			default:
				setUnknownPhase(force);
				break;
		}
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#PENDING PENDING}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase != PENDING and UNKNOWN)</i>.
	 */
	protected void setPendingPhase(boolean force) throws UWSException{
		if (!force && phase != ExecutionPhase.PENDING && phase != ExecutionPhase.UNKNOWN)
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.PENDING));
		phase = ExecutionPhase.PENDING;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#QUEUED QUEUED}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase != QUEUED, HELD, PENDING
	 *                     	                    and UNKNOWN)</i>.
	 */
	protected void setQueuedPhase(boolean force) throws UWSException{
		if (!force && phase != ExecutionPhase.QUEUED && phase != ExecutionPhase.HELD && phase != ExecutionPhase.PENDING && phase != ExecutionPhase.UNKNOWN)
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.QUEUED));
		phase = ExecutionPhase.QUEUED;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#EXECUTING EXECUTING}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase != EXECUTING, HELD, SUSPENDED,
	 *                     	                    QUEUED and UNKNOWN)</i>.
	 */
	protected void setExecutingPhase(boolean force) throws UWSException{
		if (!force && phase != ExecutionPhase.EXECUTING && phase != ExecutionPhase.HELD && phase != ExecutionPhase.SUSPENDED && phase != ExecutionPhase.QUEUED && phase != ExecutionPhase.UNKNOWN)
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.EXECUTING));
		phase = ExecutionPhase.EXECUTING;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#COMPLETED COMPLETED}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase != COMPLETED, EXECUTING
	 *                     	                    and UNKNOWN)</i>.
	 */
	protected void setCompletedPhase(boolean force) throws UWSException{
		if (!force && phase != ExecutionPhase.COMPLETED && phase != ExecutionPhase.EXECUTING && phase != ExecutionPhase.UNKNOWN)
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.COMPLETED));
		phase = ExecutionPhase.COMPLETED;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#ABORTED ABORTED}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase = COMPLETED, ERROR
	 *                     	                   or ARCHIVED)</i>.
	 */
	protected void setAbortedPhase(boolean force) throws UWSException{
		if (!force && (phase == ExecutionPhase.COMPLETED || phase == ExecutionPhase.ERROR || phase == ExecutionPhase.ARCHIVED))
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.ABORTED));
		phase = ExecutionPhase.ABORTED;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#ERROR ERROR}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase = COMPLETED, ABORTED
	 *                     	                   or ARCHIVED)</i>.
	 */
	protected void setErrorPhase(boolean force) throws UWSException{
		if (!force && (phase == ExecutionPhase.COMPLETED || phase == ExecutionPhase.ABORTED || phase == ExecutionPhase.ARCHIVED))
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.ERROR));
		phase = ExecutionPhase.ERROR;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#HELD HELD}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase != HELD, PENDING, EXECUTING
	 *                     	                    and UNKNOWN)</i>.
	 */
	protected void setHeldPhase(boolean force) throws UWSException{
		if (!force && phase != ExecutionPhase.HELD && phase != ExecutionPhase.PENDING && phase != ExecutionPhase.EXECUTING && phase != ExecutionPhase.UNKNOWN)
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.HELD));
		phase = ExecutionPhase.HELD;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#SUSPENDED SUSPENDED}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase != SUSPENDED, EXECUTING
	 *                     	                    and UNKNOWN)</i>.
	 */
	protected void setSuspendedPhase(boolean force) throws UWSException{
		if (!force && phase != ExecutionPhase.SUSPENDED && phase != ExecutionPhase.EXECUTING && phase != ExecutionPhase.UNKNOWN)
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.SUSPENDED));
		phase = ExecutionPhase.SUSPENDED;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#ARCHIVED ARCHIVED}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	If this phase transition is forbidden
	 *                     	<i>(by default: IF force=false
	 *                     	AND currentPhase != ARCHIVED, COMPLETED, ABORTED,
	 *                     	                    and UNKNOWN)</i>.
	 *
	 * @since 4.3
	 */
	protected void setArchivedPhase(boolean force) throws UWSException{
		if (!force && phase != ExecutionPhase.ARCHIVED && phase != ExecutionPhase.COMPLETED && phase != ExecutionPhase.ABORTED && phase != ExecutionPhase.ERROR && phase != ExecutionPhase.UNKNOWN)
			throw new UWSException(UWSException.BAD_REQUEST, UWSExceptionFactory.incorrectPhaseTransition(job.getJobId(), phase, ExecutionPhase.ARCHIVED));
		phase = ExecutionPhase.ARCHIVED;
	}

	/**
	 * Changes the current phase to {@link ExecutionPhase#UNKNOWN UNKNOWN}.
	 *
	 * @param force	<i>true</i> to ignore the phases order,
	 *             	<i>false</i> otherwise.
	 *
	 * @throws UWSException	By default, never!
	 */
	protected void setUnknownPhase(boolean force) throws UWSException{
		phase = ExecutionPhase.UNKNOWN;
	}

	/**
	 * Indicates whether the job attributes (except errors and results) can be
	 * updated, considering its current phase.
	 *
	 * <p><i>Note:
	 * 	By default, it returns TRUE only if the current phase is equals to
	 * 	{@link ExecutionPhase#PENDING PENDING}!
	 * </i></p>
	 *
	 * @return	<i>true</i> if the job can be updated,
	 *        	<i>false</i> otherwise.
	 */
	public boolean isJobUpdatable(){
		return phase == ExecutionPhase.PENDING;
	}

	/**
	 * Indicates whether the job is finished or not, considering its current
	 * phase.
	 *
	 * <p><i>Note:
	 * 	By default, it returns TRUE only if the current phase is either
	 * 	{@link ExecutionPhase#COMPLETED COMPLETED},
	 * 	{@link ExecutionPhase#ABORTED ABORTED},
	 * 	{@link ExecutionPhase#ERROR ERROR}
	 * 	or {@link ExecutionPhase#ARCHIVED ARCHIVED}!
	 * </i></p>
	 *
	 * @return	<i>true</i> if the job is finished,
	 *        	<i>false</i> otherwise.
	 */
	public boolean isFinished(){
		return phase == ExecutionPhase.COMPLETED || phase == ExecutionPhase.ABORTED || phase == ExecutionPhase.ERROR || phase == ExecutionPhase.ARCHIVED;
	}

	/**
	 * Indicates whether the job is executing, considering its current phase.
	 *
	 * <p><i>Note:
	 * 	By default, it returns TRUE only if the current phase is
	 * 	{@link ExecutionPhase#EXECUTING EXECUTING}!
	 * </i></p>
	 *
	 * @return	<i>true</i> if the job is executing,
	 *        	<i>false</i> otherwise.
	 */
	public boolean isExecuting(){
		return phase == ExecutionPhase.EXECUTING;
	}

	@Override
	public String toString(){
		return ExecutionPhase.getStr(phase);
	}
}
