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

import java.io.Serializable;

import uws.UWSException;

/**
 * Any object which wants to follow the progression of any instance of {@link UWSJob}.
 * It will be notified at each execution phase transition of the observer job.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 * 
 * @see UWSJob
 * @see UWSJob#addObserver(JobObserver)
 */
public interface JobObserver extends Serializable {

	/**
	 * Used when one of the observed jobs notifies its observers of a modification of its execution phase.
	 * 
	 * @param job			The observed jobs which notifies the observer.
	 * @param oldPhase		The job phase before its modification.
	 * @param newPhase		The job phase after its modification (the same value as {@link UWSJob#getPhase()}).
	 * 
	 * @throws UWSException	If there is an error during the call of this method.
	 */
	public void update(UWSJob job, ExecutionPhase oldPhase, ExecutionPhase newPhase) throws UWSException;

}
