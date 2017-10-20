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

import uws.job.UWSJob;

/**
 * Definition of a filter aiming to reduce a list of jobs.
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (10/2017)
 * @since 4.3
 *
 * @see JobFilters
 */
public interface JobFilter {

	/**
	 * Tell whether the given job match this filter.
	 *
	 * <p><i>Note:
	 * 	In case of error while evaluating this filter on the given job,
	 * 	<code>false</code> will be returned.
	 * </i></p>
	 *
	 * @param job	A job to filter.
	 *
	 * @return	<code>true</code> if the job matches this filter,
	 *        	<code>false</code> otherwise.
	 */
	public boolean match(final UWSJob job);

}