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

import uws.job.ExecutionPhase;
import uws.job.UWSJob;

/**
 * Job filter which allows all jobs except those in ARCHIVED phase.
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (10/2017)
 * @since 4.3
 */
public class NoArchivedFilter implements JobFilter {

	@Override
	public boolean match(final UWSJob job){
		return (job != null) && (job.getPhase() != ExecutionPhase.ARCHIVED);
	}

}