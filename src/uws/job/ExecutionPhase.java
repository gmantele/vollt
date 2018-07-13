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

/**
 * A job is treated as a state machine where the Execution Phase is the job
 * state. This enum class gathers all Execution Phases declared by the IVOA
 * since UWS-1.1.
 *
 * <p>
 * 	The transitions of the state machine described by the IVOA are implemented
 * 	in a different class: {@link JobPhase}.
 * </p>
 *
 * @see JobPhase
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (09/2017)
 */
public enum ExecutionPhase{
	/**
	 * The job is accepted by the service but not yet committed for execution by
	 * the client. In this state, the job quote can be read and evaluated. This
	 * is the state into which a job enters when it is first created.
	 *
	 * <p><b>Allowed previous phases:</b> <i>none</i>.</p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #HELD}, {@link #QUEUED}, {@link #ABORTED} or {@link #ERROR}.
	 * </p>
	 */
	PENDING,
	/**
	 * The job is committed for execution by the client but the service has not
	 * yet assigned it to a processor. No Results are produced in this phase.
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #PENDING} or {@link #HELD}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #EXECUTING}, {@link #ABORTED} or {@link #ERROR}.
	 * </p>
	 */
	QUEUED,
	/**
	 * The job has been assigned to a processor. Results may be produced at any
	 * time during this phase.
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #QUEUED}, {@link #HELD} or {@link #SUSPENDED}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #HELD}, {@link #SUSPENDED}, {@link #COMPLETED}, {@link #ABORTED}
	 * 	or {@link #ERROR}.
	 * </p>
	 */
	EXECUTING,
	/**
	 * The execution of the job is over. The Results may be collected.
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #EXECUTING}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #ARCHIVED}.
	 * </p>
	 */
	COMPLETED,
	/**
	 * The job failed to complete. No further work will be done nor Results
	 * produced. Results may be unavailable or available but invalid; either
	 * way the Results should not be trusted.
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #EXECUTING}, {@link #QUEUED} or {@link #PENDING}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #ARCHIVED}.
	 * </p>
	 */
	ERROR,
	/**
	 * The job has been manually aborted by the user, or the system has aborted
	 * the job due to lack of or overuse of resources.
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #PENDING}, {@link #QUEUED}, {@link #EXECUTING}, {@link #HELD} or
	 * 	{@link #SUSPENDED}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #ARCHIVED}.
	 * </p>
	 */
	ABORTED,
	/**
	 * The job is in an unknown state.
	 *
	 * <p><i>Note:
	 * 	If the UWS reports an UNKNOWN phase, then all the client can do is
	 * 	re-query the phase until a known phase is reported.
	 * </i></p>
	 *
	 * <p><b>Allowed previous phases:</b> <i>any</i>.</p>
	 *
	 * <p><b>Possible next phases:</b> <i>any</i>.</p>
	 */
	UNKNOWN,
	/**
	 * The job is HELD pending execution and will not automatically be executed
	 * (cf {@link #PENDING}).
	 *
	 * <p><i>Note:
	 * 	A UWS may place a job in a HELD phase on receipt of a PHASE=RUN request
	 * 	if for some reason the job cannot be immediately queued - in this case
	 * 	it is the responsibility of the client to request PHASE=RUN again at
	 * 	some later time.
	 * </i></p>
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #PENDING} or {@link #EXECUTING}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #QUEUED}, {@link #EXECUTING}, {@link #ABORTED} or {@link #ERROR}.
	 * </p>
	 */
	HELD,
	/**
	 * The job has been suspended by the system during execution. This might be
	 * because of temporary lack of resource. The UWS will automatically resume
	 * the job into the EXECUTING phase without any intervention when resource
	 * becomes available.
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #EXECUTING}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b>
	 * 	{@link #EXECUTING}, {@link #ABORTED} or {@link #ERROR}.
	 * </p>
	 */
	SUSPENDED,
	/**
	 * At destruction time the results associated with a job have been deleted
	 * to free up resource, but the metadata associated with the job have been
	 * retained. This is an alternative that the server may choose in contrast
	 * to completely destroying all record of the job to allow a longer
	 * historical record of the existence of the job to be kept that would
	 * otherwise be the case if limited result storage resources forces
	 * destruction.
	 *
	 * <p><b>Allowed previous phases:</b>
	 * 	{@link #ABORTED}, {@link #COMPLETED} or {@link #ERROR}.
	 * </p>
	 *
	 * <p><b>Possible next phases:</b> <i>none</i>.</p>
	 *
	 * @since 4.3 */
	ARCHIVED;

	/**
	 * Get the label of the given Execution Phase.
	 *
	 * <p><i>Note:
	 * 	The reverse operation is {@link #getPhase(String)}.
	 * </i></p>
	 *
	 * @param ph	An Execution Phase.
	 *
	 * @return	The label of the given phase,
	 *        	or {@link #UNKNOWN} if NULL is given.
	 */
	public static final String getStr(ExecutionPhase ph){
		return (ph == null) ? ExecutionPhase.UNKNOWN.name() : ph.name();
	}

	/**
	 * Get the Execution Phase corresponding to the given label.
	 *
	 * <p><i>Note:
	 * 	The reverse operation is {@link #getStr(ExecutionPhase)}.
	 * </i></p>
	 *
	 * @param phStr	Label of the Execution Phase to resolve.
	 *
	 * @return	The corresponding {@link ExecutionPhase},
	 *        	or {@link #UNKNOWN} if the given String is NULL
	 *        	                    or does not match any legal Execution Phase.
	 */
	public static final ExecutionPhase getPhase(String phStr){
		try{
			return valueOf(phStr);
		}catch(Exception ex){
			return ExecutionPhase.UNKNOWN;
		}

	}
}
