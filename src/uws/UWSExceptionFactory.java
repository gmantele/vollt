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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import uws.job.ExecutionPhase;

import uws.job.user.JobOwner;

/**
 * Let's creating the common exceptions of a UWS service.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
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

	public final static UWSException missingJobListName(){
		return missingJobListName(null);
	}

	public final static UWSException missingJobListName(final String consequence){
		return new UWSException(UWSException.BAD_REQUEST, "Missing job list name !" + appendMessage(consequence));
	}

	public final static UWSException incorrectJobListName(final String jlName){
		return incorrectJobListName(jlName, null);
	}

	public final static UWSException incorrectJobListName(final String jlName, final String consequence){
		return new UWSException(UWSException.NOT_FOUND, "Incorrect job list name ! The jobs list " + jlName + " does not exist." + appendMessage(consequence));
	}

	public final static UWSException missingJobID(){
		return missingJobID(null);
	}

	public final static UWSException missingJobID(final String consequence){
		return new UWSException(UWSException.BAD_REQUEST, "Missing job ID !" + appendMessage(consequence));
	}

	public final static UWSException incorrectJobID(String jobListName, String jobID){
		return incorrectJobID(jobListName, jobID, null);
	}

	public final static UWSException incorrectJobID(final String jobListName, final String jobID, final String consequence){
		return new UWSException(UWSException.NOT_FOUND, "Incorrect job ID ! The job " + jobID + " does not exist in the jobs list " + jobListName + appendMessage(consequence));
	}

	public final static UWSException missingSerializer(final String mimeTypes){
		return missingSerializer(null);
	}

	public final static UWSException missingSerializer(final String mimeTypes, final String consequence){
		return new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Missing UWS serializer for the MIME types: " + mimeTypes + " !" + appendMessage(consequence));
	}

	public final static UWSException incorrectJobParameter(final String jobID, final String paramName){
		return incorrectJobParameter(jobID, paramName, null);
	}

	public final static UWSException incorrectJobParameter(final String jobID, final String paramName, final String consequence){
		return new UWSException(UWSException.NOT_FOUND, "Incorrect job parameter ! The parameter " + paramName + " does not exist in the job " + jobID + "." + appendMessage(consequence));
	}

	public final static UWSException incorrectJobResult(final String jobID, final String resultID){
		return incorrectJobResult(jobID, resultID, null);
	}

	public final static UWSException incorrectJobResult(final String jobID, final String resultID, final String consequence){
		return new UWSException(UWSException.NOT_FOUND, "Incorrect result ID ! There is no result " + resultID + " in the job " + jobID + "." + appendMessage(consequence));
	}

	public final static UWSException noErrorSummary(final String jobID){
		return noErrorSummary(jobID, null);
	}

	public final static UWSException noErrorSummary(final String jobID, final String consequence){
		return new UWSException(UWSException.NOT_FOUND, "There is no error summary in the job " + jobID + " !" + appendMessage(consequence));
	}

	public final static UWSException incorrectPhaseTransition(final String jobID, final ExecutionPhase fromPhase, final ExecutionPhase toPhase){
		return incorrectPhaseTransition(jobID, fromPhase, toPhase, null);
	}

	public final static UWSException incorrectPhaseTransition(final String jobID, final ExecutionPhase fromPhase, final ExecutionPhase toPhase, final String consequence){
		return new UWSException(UWSException.BAD_REQUEST, "Incorrect phase transition ! => the job " + jobID + " is in the phase " + fromPhase + ". It can not go to " + toPhase + "." + appendMessage(consequence));
	}

	public final static UWSException missingOutputStream(){
		return missingOutputStream(null);
	}

	public final static UWSException missingOutputStream(final String consequence){
		return new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Missing output stream !" + appendMessage(consequence));
	}

	public final static UWSException incorrectSerialization(final String serializationValue, final String serializationTarget){
		return incorrectSerialization(serializationValue, serializationTarget, null);
	}

	public final static UWSException incorrectSerialization(final String serializationValue, final String serializationTarget, final String consequence){
		return new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Incorrect serialization value (=" + serializationValue + ") ! => impossible to serialize " + serializationTarget + "." + appendMessage(consequence));
	}

	public final static UWSException readPermissionDenied(final JobOwner user, final boolean jobList, final String containerName){
		return readPermissionDenied(user, jobList, containerName, null);
	}

	public final static UWSException readPermissionDenied(final JobOwner user, final boolean jobList, final String containerName, final String consequence){
		return new UWSException(UWSException.PERMISSION_DENIED, user.getID() + ((user.getPseudo() == null) ? "" : (" (alias " + user.getPseudo() + ")")) + " is not allowed to read the content of the " + (jobList ? "jobs list" : "job") + " \"" + containerName + "\" !" + appendMessage(consequence));
	}

	public final static UWSException writePermissionDenied(final JobOwner user, final boolean jobList, final String containerName){
		return writePermissionDenied(user, jobList, containerName, null);
	}

	public final static UWSException writePermissionDenied(final JobOwner user, final boolean jobList, final String containerName, final String consequence){
		return new UWSException(UWSException.PERMISSION_DENIED, user.getID() + ((user.getPseudo() == null) ? "" : (" (alias " + user.getPseudo() + ")")) + " is not allowed to update the content of the " + (jobList ? "jobs list" : "job") + " \"" + containerName + "\" !" + appendMessage(consequence));
	}

	public final static UWSException executePermissionDenied(final JobOwner user, final String jobID){
		return executePermissionDenied(user, jobID, null);
	}

	public final static UWSException executePermissionDenied(final JobOwner user, final String jobID, final String consequence){
		return new UWSException(UWSException.PERMISSION_DENIED, user.getID() + ((user.getPseudo() == null) ? "" : (" (alias " + user.getPseudo() + ")")) + " is not allowed to execute/abort the job \"" + jobID + "\" !" + appendMessage(consequence));
	}

	public final static UWSException restoreJobImpossible(final Throwable t, final String cause){
		return restoreJobImpossible(t, cause, null);
	}

	public final static UWSException restoreJobImpossible(final Throwable t, final String cause, final String consequence){
		return new UWSException(UWSException.INTERNAL_SERVER_ERROR, t, ((cause == null) ? "" : cause) + " Impossible to restore a job from the backup file(s)." + appendMessage(consequence));
	}

	public final static UWSException restoreUserImpossible(final String cause){
		return restoreUserImpossible(null, cause, null);
	}

	public final static UWSException restoreUserImpossible(final Throwable t, final String cause){
		return restoreUserImpossible(t, cause, null);
	}

	public final static UWSException restoreUserImpossible(final Throwable t, final String cause, final String consequence){
		return new UWSException(UWSException.INTERNAL_SERVER_ERROR, t, ((cause == null) ? "" : cause) + " Impossible to restore a user from the backup file(s)." + appendMessage(consequence));
	}

	public final static UWSException jobModificationForbidden(final String jobId, final ExecutionPhase phase, final String parameter){
		return jobModificationForbidden(jobId, phase, parameter, null);
	}

	public final static UWSException jobModificationForbidden(final String jobId, final ExecutionPhase phase, final String parameter, final String consequence){
		if (parameter != null && !parameter.trim().isEmpty())
			return new UWSException(UWSException.NOT_ALLOWED, "Impossible to change the parameter \"" + parameter + "\" of the job " + jobId + ((phase != null) ? (" (phase: " + phase + ")") : "") + " !" + appendMessage(consequence));
		else
			return new UWSException(UWSException.NOT_ALLOWED, "Impossible to change the parameters of the job " + jobId + ((phase != null) ? (" (phase: " + phase + ")") : "") + " !" + appendMessage(consequence));
	}

	public final static UWSException badFormat(final String jobId, final String paramName, final String paramValue, final String valueClass, final String expectedFormat){
		return badFormat(jobId, paramName, paramValue, valueClass, expectedFormat, null);
	}

	public final static UWSException badFormat(final String jobId, final String paramName, final String paramValue, final String valueClass, final String expectedFormat, final String consequence){
		String strExpected = ((expectedFormat != null && !expectedFormat.trim().isEmpty()) ? (" Expected: " + expectedFormat) : "");
		String strClass = ((valueClass != null && !valueClass.trim().isEmpty()) ? (" {an instance of " + valueClass + "}") : "");

		if (paramName != null && !paramName.trim().isEmpty()){
			if (jobId != null && !jobId.trim().isEmpty())
				return new UWSException(UWSException.BAD_REQUEST, "Bad format for the parameter " + paramName.toUpperCase() + " of the job " + jobId + ": \"" + paramValue + "\"" + strClass + "." + strExpected + appendMessage(consequence));
			else
				return new UWSException(UWSException.BAD_REQUEST, "Bad format for " + paramName + ": \"" + paramValue + "\"" + strClass + "." + strExpected + appendMessage(consequence));
		}else
			return new UWSException(UWSException.BAD_REQUEST, "Bad format: \"" + paramValue + "\"" + strClass + "." + strExpected + appendMessage(consequence));
	}

}
