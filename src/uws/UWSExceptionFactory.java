package uws;

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

import uws.job.ExecutionPhase;
import uws.job.user.JobOwner;

/**
 * Let's creating the common exceptions of a UWS service.
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.3 (09/2017)
 *
 * @see UWSException
 */
public final class UWSExceptionFactory {

	/**
	 * NO INSTANCE OF THIS CLASS MUST BE CREATED !
	 */
	private UWSExceptionFactory(){
		;
	}

	/**
	 * If the given message is returned ONLY IF it is not NULL AND not empty.
	 * @param consequence	A message to test.
	 * @return	The given message if it is not NULL and not an empty string.
	 */
	private final static String appendMessage(final String consequence){
		return ((consequence == null || consequence.trim().length() > 0) ? "" : " => " + consequence);
	}

	public final static String incorrectPhaseTransition(final String jobID, final ExecutionPhase fromPhase, final ExecutionPhase toPhase){
		return incorrectPhaseTransition(jobID, fromPhase, toPhase, null);
	}

	public final static String incorrectPhaseTransition(final String jobID, final ExecutionPhase fromPhase, final ExecutionPhase toPhase, final String consequence){
		return "Incorrect phase transition! => the job " + jobID + " is in the phase " + fromPhase + ". It can not go to " + toPhase + "." + appendMessage(consequence);
	}

	public final static String readPermissionDenied(final JobOwner user, final boolean jobList, final String containerName){
		return readPermissionDenied(user, jobList, containerName, null);
	}

	public final static String readPermissionDenied(final JobOwner user, final boolean jobList, final String containerName, final String consequence){
		return user.getID() + ((user.getPseudo() == null) ? "" : (" (alias " + user.getPseudo() + ")")) + " is not allowed to read the content of the " + (jobList ? "jobs list" : "job") + " \"" + containerName + "\"!" + appendMessage(consequence);
	}

	public final static String writePermissionDenied(final JobOwner user, final boolean jobList, final String containerName){
		return writePermissionDenied(user, jobList, containerName, null);
	}

	public final static String writePermissionDenied(final JobOwner user, final boolean jobList, final String containerName, final String consequence){
		return user.getID() + ((user.getPseudo() == null) ? "" : (" (alias " + user.getPseudo() + ")")) + " is not allowed to update the content of the " + (jobList ? "jobs list" : "job") + " \"" + containerName + "\"!" + appendMessage(consequence);
	}

	public final static String executePermissionDenied(final JobOwner user, final String jobID){
		return executePermissionDenied(user, jobID, null);
	}

	public final static String executePermissionDenied(final JobOwner user, final String jobID, final String consequence){
		return user.getID() + ((user.getPseudo() == null) ? "" : (" (alias " + user.getPseudo() + ")")) + " is not allowed to execute/abort the job \"" + jobID + "\"!" + appendMessage(consequence);
	}

	public final static String jobModificationForbidden(final String jobId, final ExecutionPhase phase, final String parameter){
		return jobModificationForbidden(jobId, phase, parameter, null);
	}

	public final static String jobModificationForbidden(final String jobId, final ExecutionPhase phase, final String parameter, final String consequence){
		if (parameter != null && !parameter.trim().isEmpty())
			return "Impossible to change the parameter \"" + parameter + "\" of the job " + jobId + ((phase != null) ? (" (phase: " + phase + ")") : "") + "!" + appendMessage(consequence);
		else
			return "Impossible to change the parameters of the job " + jobId + ((phase != null) ? (" (phase: " + phase + ")") : "") + "!" + appendMessage(consequence);
	}

}
