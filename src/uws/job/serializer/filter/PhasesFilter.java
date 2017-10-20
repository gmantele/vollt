package uws.job.serializer.filter;

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

import java.util.ArrayList;
import java.util.List;

import uws.job.ExecutionPhase;
import uws.job.UWSJob;

/**
 * Job filter based on the execution phase.
 *
 * <p>
 * 	Only jobs that are in one of the execution phases listed in this filter are
 * 	kept.
 * </p>
 *
 * <p>
 * 	The constructor of this filter requires exactly one execution phase.
 * 	But obviously more phases can be added to the list of accepted execution
 * 	phases by using the function {@link #add(ExecutionPhase)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (10/2017)
 * @since 4.3
 */
public final class PhasesFilter implements JobFilter {

	/** List of execution phases in which jobs to keep must be. */
	protected final List<ExecutionPhase> phases = new ArrayList<ExecutionPhase>();

	/**
	 * Build a {@link PhasesFilter} which will retain only jobs in the given
	 * execution phase.
	 *
	 * <p>
	 * 	More phases can be added into the list of accepted phases thanks to the
	 * 	function {@link #add(ExecutionPhase)}.
	 * </p>
	 *
	 * @param phase	An execution phase on which jobs to retain must be.
	 *
	 * @throws NullPointerException	If the given phase is NULL.
	 */
	public PhasesFilter(final ExecutionPhase phase) throws NullPointerException{
		if (phase == null)
			throw new NullPointerException("Missing execution phase! Can not ceate a PhasesFilter without at least one execution phase.");

		phases.add(phase);
	}

	/**
	 * Add the given phase in the list of accepted phases.
	 *
	 * <p><i>Note:
	 * 	The given phase is not added into the list if it is already inside.
	 * </i></p>
	 *
	 * @param phase	An execution phase to add inside the list of accepted
	 *             	phases.
	 */
	public void add(final ExecutionPhase phase){
		if (phase != null && !phases.contains(phase))
			phases.add(phase);
	}

	@Override
	public boolean match(final UWSJob job){
		if (job == null)
			return false;

		for(ExecutionPhase p : phases){
			if (job.getPhase() == p)
				return true;
		}

		return false;
	}

}