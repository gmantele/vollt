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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * <p>A job is treated as a state machine where the Execution Phase is the job state. The phases are:</p>
 * <ul>
 *	<li>{@link #PENDING}: 	the job is accepted by the service but not yet committed for
 *          				execution by the client. In this state, the job quote can be read
 *          				and evaluated. This is the state into which a job enters when it
 *          				is first created.</li>
 *
 *	<li>{@link #QUEUED}: 	the job is committed for execution by the client but the service
 *         					has not yet assigned it to a processor. No Results are produced in
 *         					this phase.</li>
 *
 *	<li>{@link #EXECUTING}: the job has been assigned to a processor. Results may be produced
 *            				at any time during this phase.</li>
 *
 *	<li>{@link #COMPLETED}: the execution of the job is over. The Results may be collected.</li>
 *
 *	<li>{@link #ERROR}: 	the job failed to complete. No further work will be done nor Results
 *        					produced. Results may be unavailable or available but invalid; either
 *        					way the Results should not be trusted.</li>
 *
 *	<li>{@link #ABORTED}: 	the job has been manually aborted by the user, or the system has
 *          				aborted the job due to lack of or overuse of resources.</li>
 *
 *	<li>{@link #UNKNOWN}: 	the job is in an unknown state.</li>
 *
 *	<li>{@link #HELD}: 		The job is HELD pending execution and will not automatically be
 *      	 				executed (cf pending).</li>
 * 
 *	<li>{@link #SUSPENDED}:	The job has been suspended by the system during execution. This might
 *							be because of temporary lack of resource. The UWS will automatically
 *							resume the job into the EXECUTING phase without any intervention
 *							when resource becomes available.</li>
 * </ul>
 * 
 * @see UWSJob
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 02/2011
 */
public enum ExecutionPhase {
	PENDING, QUEUED, EXECUTING, COMPLETED, ERROR, ABORTED, UNKNOWN, HELD, SUSPENDED;

	public static final String getStr(ExecutionPhase ph){
		return (ph==null)?ExecutionPhase.UNKNOWN.name():ph.name();
	}

	public static final ExecutionPhase getPhase(String phStr){
		try{
			return valueOf(phStr);
		}catch(Exception ex){
			return ExecutionPhase.UNKNOWN;
		}

	}
}

